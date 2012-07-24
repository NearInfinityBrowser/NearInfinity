// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.sound;

import infinity.resource.ResourceFactory;
import infinity.util.Byteconvert;
import infinity.util.Filewriter;

import javax.sound.sampled.*;
import java.io.*;

public final class SoundUtilities
{
  private static final AdpcmFmt fmt = new AdpcmFmt();
  private static File acm2wav;
  private static final byte myData[] = new byte[8196]; // old value: 32769
  private static final int stepTable[] = {
    7, 8, 9, 10, 11, 12, 13, 14,
    16, 17, 19, 21, 23, 25, 28, 31,
    34, 37, 41, 45, 50, 55, 60, 66,
    73, 80, 88, 97, 107, 118, 130, 143,
    157, 173, 190, 209, 230, 253, 279, 307,
    337, 371, 408, 449, 494, 544, 598, 658,
    724, 796, 876, 963, 1060, 1166, 1282, 1411,
    1552, 1707, 1878, 2066, 2272, 2499, 2749, 3024,
    3327, 3660, 4026, 4428, 4871, 5358, 5894, 6484,
    7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899,
    15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794,
    32767};
  private static final int indexTable[] = {-1, -1, -1, -1, 2, 4, 6, 8};
  private AudioFormat audioFormat;
  private SourceDataLine dataLine;
  private boolean notstop = true, stopped = true;

  public static File convert(File acmfile, boolean isMono) throws IOException
  {
    File wavfile = new File(acmfile.getName().substring(0, acmfile.getName().lastIndexOf((int)'.')) + ".WAV");
    if (!wavfile.exists()) {
      if (acm2wav == null || !acm2wav.exists())
        acm2wav = new File(ResourceFactory.getRootDir(), "acm2wav.exe");
      if (!acm2wav.exists())
        acm2wav = new File("acm2wav.exe");
      if (!acm2wav.exists())
        throw new IOException("acm2wav.exe not found");
      try {
        if (isMono)
          Runtime.getRuntime().exec('\"' + acm2wav.getAbsolutePath() + "\" \"" + acmfile + "\" -m").waitFor();
        else
          Runtime.getRuntime().exec('\"' + acm2wav.getAbsolutePath() + "\" \"" + acmfile + '\"').waitFor();
      } catch (InterruptedException e) {
      }
    }
    return wavfile;
  }

  public static File convert(byte data[], int offset, String filename, boolean isMono) throws IOException
  {
    File wavfile = new File(filename.substring(0, filename.lastIndexOf((int)'.')) + ".WAV");
    if (!wavfile.exists()) {
      File acmfile = new File(filename.substring(0, filename.lastIndexOf((int)'.')) + ".ACM");
      BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(acmfile));
      bos.write(data, offset, data.length - offset);
      bos.close();
      wavfile = convert(acmfile, isMono);
      acmfile.delete();
    }
    return wavfile;
  }

  public static File convertADPCM(byte data[], int offset, String filename) throws IOException
  {
    File pcmwav = new File(filename);
    OutputStream os = new BufferedOutputStream(new FileOutputStream(pcmwav));
    offset += 0x0c;
    offset = fmt.read(data, offset);
    if (fmt.bits_sample != 4) {
      System.out.println("ADPCM: " + fmt.bits_sample + " bits/sample not supported");
      throw new IOException();
    }
    while (!new String(data, offset, 4).equals("data"))
      offset += 8 + Byteconvert.convertInt(data, offset + 4);
    int data_length = Byteconvert.convertInt(data, offset + 4);
    offset += 8;
    Filewriter.writeString(os, "RIFF", 4);
    Filewriter.writeInt(os,
                        0x24 +
                        2 * (int)fmt.num_channels * fmt.samples_block * (data_length / (int)fmt.block_align));
    Filewriter.writeString(os, "WAVE", 4);
    fmt.writePcm(os);
    Filewriter.writeString(os, "data", 4);
    Filewriter.writeInt(os,
                        2 * (int)fmt.num_channels * fmt.samples_block * (data_length / (int)fmt.block_align));
    byte pcm[] = new byte[fmt.samples_block * (int)fmt.num_channels * 2]; // ->16 bit = 2 bytes/sample
    while (data_length > 0) {
      decodeADPCM(data, pcm, offset, fmt);
      Filewriter.writeBytes(os, pcm);
      offset += (int)fmt.block_align;
      data_length -= (int)fmt.block_align;
    }
    os.close();
    return pcmwav;
  }

  private static void decodeADPCM(byte data[], byte pcm[], int offset, AdpcmFmt fmt)
  {
    for (int channel = 0; channel < fmt.num_channels; channel++) {
      short last_sample = Byteconvert.convertShort(data, 4 * channel + offset);
      byte step_index = Byteconvert.convertByte(data, 4 * channel + offset + 2);
      pcm[2 * channel] = (byte)(last_sample & 0xff);
      pcm[1 + 2 * channel] = (byte)(last_sample >> 8 & 0xff);
      for (int index = 0; index < fmt.samples_block / 2; index++) {
        int index_data = offset + index +
                         4 * ((int)fmt.num_channels + channel + ((int)fmt.num_channels - 1) * (index / 4));
        int pcm_index = 2 * (channel + (int)fmt.num_channels * (1 + 2 * index));
        // Lower 4 bits
        byte nibble = (byte)(data[index_data] & 7);
        int diff = stepTable[step_index] * (int)nibble / 4 + stepTable[step_index] / 8;
        if ((data[index_data] & 0x08) != 0)
          last_sample -= (short)diff;
        else
          last_sample += (short)diff;
//        if (last_sample > 32767)
//          last_sample = 32767;
//        else if (last_sample < -32768)
//          last_sample = -32768;
        step_index += (byte)indexTable[nibble];
        if (step_index > 88)
          step_index = (byte)88;
        else if (step_index < 0)
          step_index = (byte)0;
        pcm[pcm_index] = (byte)(last_sample & 0xff);
        pcm[1 + pcm_index] = (byte)(last_sample >> 8 & 0xff);
        // Upper 4 bits
        nibble = (byte)(data[index_data] >> 4 & 7);
        diff = stepTable[step_index] * (int)nibble / 4 + stepTable[step_index] / 8;
        if ((data[index_data] & 0x80) != 0)
          last_sample -= (short)diff;
        else
          last_sample += (short)diff;
//        if (last_sample > 32767)
//          last_sample = 32767;
//        else if (last_sample < -32768)
//          last_sample = -32768;
        step_index += (byte)indexTable[nibble];
        if (step_index > 88)
          step_index = (byte)88;
        else if (step_index < 0)
          step_index = (byte)0;
        pcm[2 * (int)fmt.num_channels + pcm_index] = (byte)(last_sample & 0xff);
        pcm[2 * (int)fmt.num_channels + 1 + pcm_index] = (byte)(last_sample >> 8 & 0xff);
      }
    }
  }

  public synchronized void play(File wavfile) throws Exception
  {
    notstop = true;
    stopped = false;
    AudioInputStream ais;
    if (wavfile == null)
      return;
    try {
      ais = AudioSystem.getAudioInputStream(wavfile);
    } catch (UnsupportedAudioFileException e) {
      throw new IOException("Unsupported WAV-format");
    }
    if (dataLine == null || !ais.getFormat().matches(audioFormat)) {
      audioFormat = ais.getFormat();
      DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
      if (!AudioSystem.isLineSupported(info))
        throw new IOException("Unsupported WAV-format");
      dataLine = (SourceDataLine)AudioSystem.getLine(info);
      dataLine.open(ais.getFormat(), 16384);
    }
    dataLine.start();

    while (notstop) {
      int numBytesRead = ais.read(myData, 0, myData.length);
      if (numBytesRead == -1) break;
      dataLine.write(myData, 0, numBytesRead);
    }
    ais.close();
    if (!notstop)
      dataLine.drain();
    // dataLine.stop();
    // dataLine.close();
    stopped = true;
  }

  public void stopPlay()
  {
    notstop = false;
    while (!stopped)
      try {
        Thread.sleep((long)100);
      } catch (InterruptedException e) {
      }
    try {
      Thread.sleep((long)300);
    } catch (InterruptedException e) {
    }
//    dataLine.stop();
//    dataLine.close();
    dataLine = null;
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class AdpcmFmt
  {
    private String id;
    private int chunk_size, sample_rate, samples_block; // bytes_sec
    private short num_channels, block_align, bits_sample; // compr_code, extra_bytes

    private AdpcmFmt()
    {
    }

    private int read(byte data[], int offset)
    {
      id = new String(data, offset, 4);
      chunk_size = Byteconvert.convertInt(data, offset + 4);
//      compr_code = Byteconvert.convertShort(data, offset + 8);
      num_channels = Byteconvert.convertShort(data, offset + 10);
      sample_rate = Byteconvert.convertInt(data, offset + 12);
//      bytes_sec = Byteconvert.convertInt(data, offset + 16);
      block_align = Byteconvert.convertShort(data, offset + 20);
      bits_sample = Byteconvert.convertShort(data, offset + 22);
//      extra_bytes = Byteconvert.convertShort(data, offset + 24);
      samples_block = Byteconvert.convertUnsignedShort(data, offset + 26);
      return offset + 8 + chunk_size;
    }

    private void writePcm(OutputStream os) throws IOException
    {
      Filewriter.writeString(os, id, 4);
      Filewriter.writeInt(os, 0x10);
      Filewriter.writeShort(os, (short)0x01);
      Filewriter.writeShort(os, num_channels);
      Filewriter.writeInt(os, sample_rate);
      Filewriter.writeInt(os, sample_rate * (int)num_channels * 2);
      Filewriter.writeShort(os, (short)((int)num_channels * 2));
      Filewriter.writeShort(os, (short)16);
    }
  }
}

