// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sound;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.infinity.resource.key.ResourceEntry;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;

/**
 * Decodes Ogg Vorbis encoded audio data into uncompressed PCM WAV audio data.
 */

public class OggBuffer extends AudioBuffer {
  private Info oggInfo;

  public OggBuffer(ResourceEntry entry) throws Exception {
    super(entry);
  }

  public OggBuffer(ResourceEntry entry, AudioOverride override) throws Exception {
    super(entry, override);
  }

  public OggBuffer(byte[] buffer, int offset) throws Exception {
    super(buffer, offset);
  }

  public OggBuffer(byte[] buffer, int offset, AudioOverride override) throws Exception {
    super(buffer, offset, override);
  }

  public Info getInfo() {
    return oggInfo;
  }

  // --------------------- Begin Class AudioBuffer ---------------------

  @Override
  protected void convert(byte[] buffer, int offset, AudioOverride override) throws Exception {
    if (buffer == null) {
      throw new NullPointerException();
    }
    if (offset < 0 || offset >= buffer.length) {
      throw new Exception("Invalid buffer offset");
    }

    // override object will be ignored
    data = decodeOgg(buffer, offset);
  }

  // --------------------- Begin Class AudioBuffer ---------------------

  // Ogg decode routine closely based on JOrbis' DecodeExample.java
  private byte[] decodeOgg(byte[] inBuf, int offset) throws Exception {
    if (inBuf == null) {
      throw new NullPointerException();
    }
    if (offset < 0) {
      offset = 0;
    }
    if (offset >= inBuf.length) {
      throw new Exception("Input buffer too small");
    }

    int convSize = 4096 * 2;
    final byte[] convBuffer = new byte[convSize]; // take 8k out of the data segment, not the stack

    ByteArrayInputStream bis = new ByteArrayInputStream(inBuf, offset, inBuf.length - offset);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();

    SyncState oy = new SyncState(); // sync and verify incoming physical bitstream
    StreamState os = new StreamState(); // take physical pages, weld into a logical stream of packets
    Page og = new Page(); // one Ogg bitstream page. Vorbis packets are inside
    Packet op = new Packet(); // one raw packet of data for decode

    oggInfo = new Info(); // struct that stores all the static vorbis bitstream settings
    Comment vc = new Comment(); // struct that stores all the bitstream user comments
    DspState vd = new DspState(); // central working state for the packet->PCM decoder
    Block vb = new Block(vd); // local working space for packet->PCM decode

    byte[] buffer;
    int bytes = 0;

    // Decode setup
    oy.init(); // Now we can read pages

    while (true) { // we repeat if the bitstream is chained
      int eos = 0;

      // grab some data at the head of the stream. We want the first page
      // (which is guaranteed to be small and only contain the Vorbis
      // stream initial header) We need the first page to get the stream
      // serialno.

      // submit a 4k block to libvorbis' Ogg layer
      int index = oy.buffer(4096);
      buffer = oy.data;
      try {
        bytes = bis.read(buffer, index, 4096);
      } catch (Exception e) {
        throw new Exception(e.getMessage());
      }
      oy.wrote(bytes);

      // Get the first page.
      if (oy.pageout(og) != 1) {
        // have we simply run out of data? If so, we're done.
        if (bytes < 4096) {
          break;
        }

        // error case. Must not be Vorbis data
        throw new Exception("Input does not appear to be an Ogg bitstream.");
      }

      // Get the serial number and set up the rest of decode.
      // serialno first; use it to set up a logical stream
      os.init(og.serialno());

      // extract the initial header from the first page and verify that the
      // Ogg bitstream is in fact Vorbis data

      // I handle the initial header first instead of just having the code
      // read all three Vorbis headers at once because reading the initial
      // header is an easy way to identify a Vorbis bitstream and it's
      // useful to see that functionality seperated out.

      oggInfo.init();
      vc.init();
      if (os.pagein(og) < 0) {
        // error; stream version mismatch perhaps
        throw new Exception("Error reading first page of Ogg bitstream data.");
      }

      if (os.packetout(op) != 1) {
        // no page? must not be vorbis
        throw new Exception("Error reading initial header packet.");
      }

      if (oggInfo.synthesis_headerin(vc, op) < 0) {
        // error case; not a vorbis header
        throw new Exception("This Ogg bitstream does not contain Vorbis audio data.");
      }

      // At this point, we're sure we're Vorbis. We've set up the logical
      // (Ogg) bitstream decoder. Get the comment and codebook headers and
      // set up the Vorbis decoder

      // The next two packets in order are the comment and codebook headers.
      // They're likely large and may span multiple pages. Thus we reead
      // and submit data until we get our two pacakets, watching that no
      // pages are missing. If a page is missing, error out; losing a
      // header page is the only place where missing data is fatal. */

      int i = 0;
      while (i < 2) {
        while (i < 2) {
          int result = oy.pageout(og);
          if (result == 0) {
            break;
          }

          // Don't complain about missing or corrupt data yet.
          // We'll catch it at the packet output phase.

          if (result == 1) {
            os.pagein(og); // we can ignore any errors here

            // as they'll also become apparent
            // at packetout
            while (i < 2) {
              result = os.packetout(op);
              if (result == 0) {
                break;
              }
              if (result == -1) {
                // Uh oh; data at some point was corrupted or missing!
                // We can't tolerate that in a header. Die.
                throw new Exception("Corrupt secondary header.");
              }
              oggInfo.synthesis_headerin(vc, op);
              i++;
            }
          }
        }

        // no harm in not checking before adding more
        index = oy.buffer(4096);
        buffer = oy.data;
        try {
          bytes = bis.read(buffer, index, 4096);
        } catch (Exception e) {
          throw new Exception(e.getMessage());
        }

        if (bytes == 0 && i < 2) {
          throw new Exception("End of file before finding all Vorbis headers!");
        }
        oy.wrote(bytes);
      }

      // Throw the comments plus a few lines about the bitstream we're decoding
      // {
      // byte[][] ptr = vc.user_comments;
      // for (int j = 0; j < ptr.length; j++) {
      // if (ptr[j] == null)
      // break;
      // System.err.println(new String(ptr[j], 0, ptr[j].length-1));
      // }
      // System.err.println("\nBitstream is "+vi.channels+" channel, "+vi.rate+"Hz");
      // System.err.println("Encoded by: "+new String(vc.vendor, 0, vc.vendor.length-1)+"\n");
      // }

      convSize = 4096 / oggInfo.channels;

      // OK, got and parsed all three headers. Initialize the Vorbis packet->PCM decoder.
      vd.synthesis_init(oggInfo); // central decode state
      vb.init(vd); // local state for most of the decode, so multiple block decodes can proceed
      // in parallel. We could init multiple vorbis_block structures for vd here

      float[][][] _pcm = new float[1][][];
      int[] _index = new int[oggInfo.channels];

      // The rest is just a straight decode loop until end of stream
      while (eos == 0) {
        while (eos == 0) {

          int result = oy.pageout(og);
          if (result == 0) {
            break;
          }

          if (result == -1) { // missing or corrupt data at this page position
            System.err.println("Corrupt or missing data in bitstream; continuing...");
          } else {
            os.pagein(og); // can safely ignore errors at this point

            while (true) {
              result = os.packetout(op);

              if (result == 0) {
                break;
              }

              if (result == -1) { // missing or corrupt data at this page position
                // no reason to complain; already complained above
              } else {
                // we have a packet. Decode it
                int samples;
                if (vb.synthesis(op) == 0) { // test for success!
                  vd.synthesis_blockin(vb);
                }

                // **pcm is a multichannel float vector. In stereo, for
                // example, pcm[0] is left, and pcm[1] is right. samples is
                // the size of each channel. Convert the float values
                // (-1.<=range<=1.) to whatever PCM format and write it out

                while ((samples = vd.synthesis_pcmout(_pcm, _index)) > 0) {
                  float[][] pcm = _pcm[0];
                  int bout = (samples < convSize) ? samples : convSize;

                  // convert floats to 16 bit signed ints (host order) and interleave
                  for (i = 0; i < oggInfo.channels; i++) {
                    int ptr = i * 2;
                    // int ptr = i;
                    int mono = _index[i];
                    for (int j = 0; j < bout; j++) {
                      int val = (int) (pcm[i][mono + j] * 32767.0f);
                      // short val = (short)(pcm[i][mono + j] * 32767.0f);
                      // int val = (int)Math.round(pcm[i][mono + j] * 32767.0f);
                      // might as well guard against clipping
                      if (val > 32767) {
                        val = 32767;
                      }
                      if (val < -32768) {
                        val = -32768;
                      }
                      if (val < 0) {
                        val = val | 0x8000;
                      }
                      convBuffer[ptr] = (byte) (val);
                      convBuffer[ptr + 1] = (byte) (val >>> 8);
                      ptr += 2 * oggInfo.channels;
                    }
                  }

                  bos.write(convBuffer, 0, 2 * oggInfo.channels * bout);

                  // tell libvorbis how
                  // many samples we
                  // actually consumed
                  vd.synthesis_read(bout);
                }
              }
            }
            if (og.eos() != 0) {
              eos = 1;
            }
          }
        }

        if (eos == 0) {
          index = oy.buffer(4096);
          buffer = oy.data;
          try {
            bytes = bis.read(buffer, index, 4096);
          } catch (Exception e) {
            throw e;
          }
          oy.wrote(bytes);
          if (bytes == 0) {
            eos = 1;
          }
        }
      }

      // clean up this logical bitstream; before exit we see if we're followed by another [chained]
      os.clear();

      // ogg_page and ogg_packet structs always point to storage in libvorbis.
      // They're never freed or manipulated directly
      vb.clear();
      vd.clear();
      oggInfo.clear(); // must be called last
    }

    // OK, clean up the framer
    oy.clear();

    // create final output buffer
    int samplesPerChannel = bos.size() / (oggInfo.channels * 2);
    byte[] header = createWAVHeader(samplesPerChannel, oggInfo.channels, oggInfo.rate, 16);
    byte[] output = new byte[header.length + bos.size()];
    System.arraycopy(header, 0, output, 0, header.length);
    System.arraycopy(bos.toByteArray(), 0, output, header.length, bos.size());

    return output;
  }
}
