// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sound;

import java.util.Arrays;
import java.util.HashSet;

import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.DynamicArray;

/**
 * Loads and provides access to uncompressed PCM WAV audio data.
 */
public class WavBuffer extends AudioBuffer
{
  public WavBuffer(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  public WavBuffer(ResourceEntry entry, AudioOverride override) throws Exception
  {
    super(entry, override);
  }

  public WavBuffer(byte[] buffer, int offset) throws Exception
  {
    super(buffer, offset);
  }

  public WavBuffer(byte[] buffer, int offset, AudioOverride override) throws Exception
  {
    super(buffer, offset, override);
  }

//--------------------- Begin Class AudioBuffer ---------------------

  @Override
  protected void convert(byte[] buffer, int offset, AudioOverride override) throws Exception
  {
    WaveFmt fmt = new WaveFmt(override);
    offset = fmt.read(buffer, offset);

    if (fmt.pcmType == WaveFmt.ID_TYPE_ADPCM)
      data = convertADPCM(buffer, offset, fmt);
    else if (fmt.pcmType == WaveFmt.ID_TYPE_PCM) {
      byte[] header = createWAVHeader(fmt.samplesPerChannel, fmt.numChannels,
                                      fmt.sampleRate, fmt.bitsPerSample);
      int dstDataSize = fmt.samplesPerChannel * fmt.numChannels * (fmt.bitsPerSample / 8);
      data = new byte[header.length + dstDataSize];
      System.arraycopy(header, 0, data, 0, header.length);
      System.arraycopy(buffer, offset, data, header.length, dstDataSize);
    }
  }

//--------------------- End Class AudioBuffer ---------------------

  // Decodes IMA ADPCM encoded audio data and returns a buffer containing
  // signed 16-bit PCM audio data, including WAV header
  private static byte[] convertADPCM(byte[] buffer, int offset, WaveFmt fmt) throws Exception
  {
    final int stepTable[] = {
            7,     8,     9,    10,    11,    12,    13,    14,
           16,    17,    19,    21,    23,    25,    28,    31,
           34,    37,    41,    45,    50,    55,    60,    66,
           73,    80,    88,    97,   107,   118,   130,   143,
          157,   173,   190,   209,   230,   253,   279,   307,
          337,   371,   408,   449,   494,   544,   598,   658,
          724,   796,   876,   963,  1060,  1166,  1282,  1411,
         1552,  1707,  1878,  2066,  2272,  2499,  2749,  3024,
         3327,  3660,  4026,  4428,  4871,  5358,  5894,  6484,
         7132,  7845,  8630,  9493, 10442, 11487, 12635, 13899,
        15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794,
        32767};
    final int indexTable[] = {-1, -1, -1, -1, 2, 4, 6, 8};

    // sanity checks
    if (buffer == null || fmt == null)
      throw new NullPointerException();
    if (fmt.pcmType != WaveFmt.ID_TYPE_ADPCM)
      throw new Exception("No ADPCM data found");
    if (offset + fmt.dataSize > buffer.length)
      throw new Exception("Input buffer too small");

    // creating output buffer and header
    int pcmBitsPerSample = 16;
    int pcmDataSize = fmt.samplesPerChannel * fmt.numChannels * pcmBitsPerSample >> 3;
    byte[] header = createWAVHeader(fmt.samplesPerChannel, fmt.numChannels, fmt.sampleRate, pcmBitsPerSample);

    DynamicArray pcm = DynamicArray.allocate((header.length + pcmDataSize) >> 1, DynamicArray.ElementType.SHORT);
    System.arraycopy(header, 0, pcm.getArray(), 0, header.length);
    int pcmOfs = header.length >> 1;

    // decoding data
    int adpcmSize = fmt.dataSize;
    int pcmBlockSize = (fmt.samplesPerBlock * fmt.numChannels);
    while (adpcmSize > 0) {
      for (int channel = 0; channel < fmt.numChannels; channel++) {
        short lastSample = DynamicArray.getShort(buffer, offset + (channel << 2));
        byte stepIndex = DynamicArray.getByte(buffer, offset + (channel << 2) + 2);
        pcm.putShort(pcmOfs + channel, lastSample);
        for (int idx = 0; idx < (fmt.samplesPerBlock >> 1); idx++) {
          int idxData = offset + idx + ((fmt.numChannels + channel + (fmt.numChannels - 1) * (idx >> 2)) << 2);
          int pcmIdx = channel + fmt.numChannels * (1 + (idx << 1));
          // Lower 4 bits
          byte nibble = (byte)(buffer[idxData] & 0x07);
          int diff = (stepTable[stepIndex] * nibble >> 2) + (stepTable[stepIndex] >> 3);
          if ((buffer[idxData] & 0x08) != 0)
            lastSample -= (short)diff;
          else
            lastSample += (short)diff;
          stepIndex += (byte)indexTable[nibble];
          if (stepIndex > stepTable.length - 1)
            stepIndex = (byte)(stepTable.length - 1);
          else if (stepIndex < 0)
            stepIndex = (byte)0;
          pcm.putShort(pcmOfs + pcmIdx, lastSample);
          // Upper 4 bits
          nibble = (byte)((buffer[idxData] >> 4) & 0x07);
          diff = (stepTable[stepIndex] * nibble >> 2) + (stepTable[stepIndex] >> 3);
          if ((buffer[idxData] & 0x80) != 0)
            lastSample -= (short)diff;
          else
            lastSample += (short)diff;
          stepIndex += (byte)indexTable[nibble];
          if (stepIndex > stepTable.length - 1)
            stepIndex = (byte)(stepTable.length - 1);
          else if (stepIndex < 0)
            stepIndex = (byte)0;
          pcm.putShort(pcmOfs + fmt.numChannels + pcmIdx, lastSample);
        }
      }
      adpcmSize -= fmt.blockAlign;
      offset += fmt.blockAlign;
      pcmOfs += pcmBlockSize;
    }

    return pcm.getArray();
  }


//-------------------------- INNER CLASSES --------------------------

  private static class WaveFmt
  {
    private static final int ID_CHUNK         = 0x46464952;     // 'RIFF'
    private static final int ID_FORMAT        = 0x45564157;     // 'WAVE'
    private static final int ID_SUBCHUNK1     = 0x20746d66;     // 'fmt '
    private static final int ID_SUBCHUNK2     = 0x61746164;     // 'data'
    private static final short ID_TYPE_PCM    = 0x01;           // PCM
    private static final short ID_TYPE_ADPCM  = 0x11;           // IMA ADPCM
    private static final HashSet<Short> s_audioTypes =
        new HashSet<>(Arrays.asList(new Short[]{ID_TYPE_PCM, ID_TYPE_ADPCM}));

    private final AudioOverride override;
    private int sampleRate, samplesPerBlock, samplesPerChannel, dataSize;
    private short pcmType, numChannels, blockAlign, bitsPerSample;

    private WaveFmt(AudioOverride override)
    {
      if (override == null)
        override = AudioOverride.override(0, 0, 0);
      this.override = override;
    }

    // Read wave header and return start offset of audio data
    private int read(byte[] buffer, int offset) throws Exception
    {
      if (buffer == null)
        throw new NullPointerException();
      if (offset < 0 || offset + 44 >= buffer.length)
        throw new Exception("Input buffer too small");

      if (DynamicArray.getInt(buffer, offset) != ID_CHUNK)
        throw new Exception("Invalid RIFF header");
      int i = DynamicArray.getInt(buffer, offset + 4);
      if (i <= 36 || offset + i + 8 > buffer.length)
        throw new Exception("Input buffer too small");
      if (DynamicArray.getInt(buffer, offset + 8) != ID_FORMAT)
        throw new Exception("Unsupported RIFF format");
      offset += 12;

      if (DynamicArray.getInt(buffer, offset) != ID_SUBCHUNK1)
        throw new Exception("Invalid fmt header block");
      int subChunk1Size = DynamicArray.getInt(buffer, offset + 4);
      if (subChunk1Size < 16)
        throw new Exception("Invalid fmt header size");
      pcmType = DynamicArray.getShort(buffer, offset + 8);
      if (!s_audioTypes.contains(pcmType))
        throw new Exception("Unsupported audio compression format: " + pcmType);
      numChannels = DynamicArray.getShort(buffer, offset + 10);
      sampleRate = DynamicArray.getInt(buffer, offset + 12);
      DynamicArray.getInt(buffer, offset + 16);   // byte rate
      blockAlign = DynamicArray.getShort(buffer, offset + 20);
      bitsPerSample = DynamicArray.getShort(buffer, offset + 22);
      offset += subChunk1Size + 8;

      if (pcmType == ID_TYPE_ADPCM) {    // ADPCM compression
        if (override.numChannels > 0)
          numChannels = (short)override.numChannels;
        if (override.sampleRate > 0)
          sampleRate = override.sampleRate;
        if (bitsPerSample != 4)
          throw new Exception("ADPCM: " + bitsPerSample + " bits/sample not supported");
        short extraSize = DynamicArray.getShort(buffer, offset);
        if (extraSize < 2)
          throw new Exception("ADPCM: Extra header size too small");
        if (extraSize < 4)
          samplesPerBlock = DynamicArray.getShort(buffer, 38);
        else
          samplesPerBlock = DynamicArray.getInt(buffer, 38);
        offset += extraSize + 2;
      } else {                  // PCM compression
        if (override.numChannels > 0)
          numChannels = (short)override.numChannels;
        if (override.sampleRate > 0)
          sampleRate = override.sampleRate;
        if (override.bitsPerSample > 0)
          bitsPerSample = (short)override.bitsPerSample;
        blockAlign = (short)(numChannels * bitsPerSample / 8);
        samplesPerBlock = 1;
      }

      // skip additional info headers
      while (DynamicArray.getInt(buffer, offset) != ID_SUBCHUNK2) {
        int skip = DynamicArray.getInt(buffer, offset + 4);
        offset += 8 + skip;
        if (offset >= buffer.length)
          throw new Exception("Unexpected end of data");
      }

      if (DynamicArray.getInt(buffer, offset) != ID_SUBCHUNK2)
        throw new Exception("Invalid data header block");
      dataSize = DynamicArray.getInt(buffer, offset + 4);
      samplesPerChannel = (dataSize / blockAlign) * samplesPerBlock;
      offset += 8;

      return offset;
    }
  }
}
