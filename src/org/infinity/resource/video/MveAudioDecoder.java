// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.video;

import java.util.Arrays;

import javax.sound.sampled.AudioFormat;

import org.infinity.resource.video.MveDecoder.MveInfo;
import org.infinity.resource.video.MveDecoder.MveSegment;

/**
 * Decodes a single frame of audio data. (Internally used by MveDecoder)
 */
public class MveAudioDecoder
{
  // 256 delta entries for compressed audio
  private static short[] DELTA = {
      0,      1,      2,      3,      4,      5,      6,      7,      8,      9,     10,     11,     12,     13,     14,     15,
     16,     17,     18,     19,     20,     21,     22,     23,     24,     25,     26,     27,     28,     29,     30,     31,
     32,     33,     34,     35,     36,     37,     38,     39,     40,     41,     42,     43,     47,     51,     56,     61,
     66,     72,     79,     86,     94,    102,    112,    122,    133,    145,    158,    173,    189,    206,    225,    245,
    267,    292,    318,    348,    379,    414,    452,    493,    538,    587,    640,    699,    763,    832,    908,    991,
   1081,   1180,   1288,   1405,   1534,   1673,   1826,   1993,   2175,   2373,   2590,   2826,   3084,   3365,   3672,   4008,
   4373,   4772,   5208,   5683,   6202,   6767,   7385,   8059,   8794,   9597,  10472,  11428,  12471,  13609,  14851,  16206,
  17685,  19298,  21060,  22981,  25078,  27367,  29864,  32589, -29973, -26728, -23186, -19322, -15105, -10503,  -5481,     -1,
      1,      1,   5481,  10503,  15105,  19322,  23186,  26728,  29973, -32589, -29864, -27367, -25078, -22981, -21060, -19298,
 -17685, -16206, -14851, -13609, -12471, -11428, -10472,  -9597,  -8794,  -8059,  -7385,  -6767,  -6202,  -5683,  -5208,  -4772,
  -4373,  -4008,  -3672,  -3365,  -3084,  -2826,  -2590,  -2373,  -2175,  -1993,  -1826,  -1673,  -1534,  -1405,  -1288,  -1180,
  -1081,   -991,   -908,   -832,   -763,   -699,   -640,   -587,   -538,   -493,   -452,   -414,   -379,   -348,   -318,   -292,
   -267,   -245,   -225,   -206,   -189,   -173,   -158,   -145,   -133,   -122,   -112,   -102,    -94,    -86,    -79,    -72,
    -66,    -61,    -56,    -51,    -47,    -43,    -42,    -41,    -40,    -39,    -38,    -37,    -36,    -35,    -34,    -33,
    -32,    -31,    -30,    -29,    -28,    -27,    -26,    -25,    -24,    -23,    -22,    -21,    -20,    -19,    -18,    -17,
    -16,    -15,    -14,    -13,    -12,    -11,    -10,     -9,     -8,     -7,     -6,     -5,     -4,     -3,     -2,     -1
  };

  // the currently used MVE info structure
  private final MveInfo info;

  private boolean isAudioInit;    // set when audio initialization occured in the current chunk

  /**
   * Creates a new MveAudioDecoder object. Constructor is not publicly accessible.
   * @param info Info structure containing all MVE related data.
   * @return An MveAudioDecoder object on success, {@code null} otherwise.
   */
  public static MveAudioDecoder createDecoder(MveInfo info)
  {
    if (info != null) {
      return new MveAudioDecoder(info);
    } else {
      return null;
    }
  }

  /**
   * Processes audio specific segments.
   * @param segment The current segment to process.
   * @return {@code true} if the segment has been processed successfully,
   *         {@code false} if the segment did not fit into the audio category.
   * @throws Exception On error.
   */
  public boolean processAudio(MveSegment segment) throws Exception
  {
    if (info == null || segment == null)
      throw new NullPointerException();

    switch (segment.getOpcode()) {
      case MveDecoder.MVE_OC_END_OF_STREAM:     // do final clean up
        shutDown();
        break;
      case MveDecoder.MVE_OC_END_OF_CHUNK:      // do some temporary clean up
        cleanUp();
        break;
      case MveDecoder.MVE_OC_AUDIO_BUFFERS:     // initializes audio properties
        processInitAudio(segment);
        break;
      case MveDecoder.MVE_OC_PLAY_AUDIO:        // ignored
        break;
      case MveDecoder.MVE_OC_AUDIO_DATA:        // decodes a frame worth of audio data
        if (info.audioCompressed) {
          processCompressedAudio(segment);
        } else {
          processUncompressedAudio(segment);
        }
        break;
      case MveDecoder.MVE_OC_AUDIO_SILENCE:     // generates a frame worth of silence
        processSilence(segment);
        break;
      default:
        return false;
    }
    return true;
  }

  /**
   * Properly releases temporary resources.
   */
  public void close()
  {
    shutDown();
  }

  private MveAudioDecoder(MveInfo info)
  {
    if (info == null)
      throw new NullPointerException();

    this.info = info;
  }

  // cleans up temporary data
  private void cleanUp()
  {
    info.audioInitialized = isAudioInit;
    isAudioInit = false;
  }

  // cleans up all MVE specific
  private void shutDown()
  {
    // nothing to do (yet)
  }

  // initializes audio properties
  private void processInitAudio(MveSegment segment) throws Exception
  {
    isAudioInit = true;
    if (segment != null && segment.getOpcode() == MveDecoder.MVE_OC_AUDIO_BUFFERS) {
      int flags, channels, bitsPerSample, sampleRate;
      switch (segment.getVersion()) {
        case 0:
          segment.getBits(16);    // bogus data
          flags = segment.getBits(16);
          channels = ((flags & MveDecoder.MVE_AUDIO_STEREO) != 0) ? 2 : 1;
          bitsPerSample = ((flags & MveDecoder.MVE_AUDIO_16BIT) != 0) ? 16 : 8;
          sampleRate = segment.getBits(16);
          info.audioCompressed = false;
          segment.getBits(16);    // buffer length (not needed)
          break;
        case 1:
          segment.getBits(16);    // bogus data
          flags = segment.getBits(16);
          channels = ((flags & MveDecoder.MVE_AUDIO_STEREO) != 0) ? 2 : 1;
          bitsPerSample = ((flags & MveDecoder.MVE_AUDIO_16BIT) != 0) ? 16 : 8;
          info.audioCompressed = ((flags & MveDecoder.MVE_AUDIO_COMPRESSED) != 0);
          sampleRate = segment.getBits(16);
          segment.getBits(32);    // buffer length (not needed)
          break;
        default:
          throw new Exception("Unsupported version: " + segment.getVersion());
      }
      info.audioFormat = new AudioFormat((float)sampleRate, bitsPerSample, channels, (bitsPerSample != 8), false);
    }
  }

  // writes an audio frame worth of silent data
  private void processSilence(MveSegment segment) throws Exception
  {
    int index = segment.getBits(16);    // sequential index of the audio frame
    int mask = segment.getBits(16);     // channel mask
    int len = segment.getBits(16);      // total size of the uncompressed audio block in bytes
    if (len < 0)
      len = 0;
    updateTimer(len);

    byte[] block = null;
    AudioQueue queue;
    for (int i = 0; i < MveInfo.AUDIOSTREAMS_MAX; i++) {
      if ((mask & (1 << i)) != 0) {
        queue = info.audioOutput.get(i);
        if (queue != null) {
          if (block == null) {
            block = new byte[len];
            Arrays.fill(block, (byte)0);
          }
          queue.addAudioBlock(index, block);
        }
      }
    }
  }

  // writes an audio frame worth of uncompressed audio data
  private void processUncompressedAudio(MveSegment segment) throws Exception
  {
    int index = segment.getBits(16);    // sequential index of the audio frame
    int mask = segment.getBits(16);     // channel mask
    int len = segment.getBits(16);      // total size of the uncompressed audio block in bytes
    if (len < 0)
      len = 0;
    updateTimer(len);

    byte[] block = null;
    AudioQueue queue;
    for (int i = 0; i < MveInfo.AUDIOSTREAMS_MAX; i++) {
      if ((mask & (1 << i)) != 0) {
        queue = info.audioOutput.get(i);
        if (queue != null) {
          if (block == null) {
            block = new byte[len];
            System.arraycopy(segment.getData(), 6, block, 0, len);
          }
          queue.addAudioBlock(index, block);
        }
      }
    }
  }

  // decodes and writes an audio frame worth of compressed audio data
  private void processCompressedAudio(MveSegment segment) throws Exception
  {
    if (info.audioFormat.getSampleSizeInBits() != 16)
      throw new Exception("Unsupported bits per samples: " + info.audioFormat.getSampleSizeInBits());

    int index = segment.getBits(16);    // sequential index of the audio frame
    int mask = segment.getBits(16);     // channel mask
    int len = segment.getBits(16);      // total size of the uncompressed audio block in bytes
    if (len < 0)
      len = 0;
    updateTimer(len);

    short[] predictor = null;
    byte[] block = null;
    AudioQueue queue;
    for (int i = 0; i < MveInfo.AUDIOSTREAMS_MAX; i++) {
      if ((mask & (1 << i)) != 0) {
        queue = info.audioOutput.get(i);
        if (queue != null) {
          if (block == null) {
            int channelMask = info.audioFormat.getChannels() >>> 1;
            int outOfs = 0;
            block = new byte[len];
            predictor = new short[2];

            // initializing start value
            for (int j = 0; j < info.audioFormat.getChannels(); j++) {
              predictor[j] = (short)segment.getBits(16);
              block[outOfs++] = (byte)(predictor[j] & 0xff);
              block[outOfs++] = (byte)(predictor[j] >>> 8);
            }

            // decoding deltas
            int channel = 0;
            while (outOfs < len) {
              predictor[channel] += DELTA[segment.getBits(8)];
              block[outOfs++] = (byte)(predictor[channel] & 0xff);
              block[outOfs++] = (byte)(predictor[channel] >>> 8);
              channel = (channel + 1) & channelMask;
            }
          }
          queue.addAudioBlock(index, block);
        }
      }
    }
  }

  private void updateTimer(int frameSize)
  {
    if (!info.isFrameDelayStable) {
      info.frameDelay = (int)((1000000L * frameSize) / ((long)info.audioFormat.getSampleRate() *
                                                       info.audioFormat.getChannels() *
                                                       info.audioFormat.getSampleSizeInBits() / 8L));
    }
  }
}
