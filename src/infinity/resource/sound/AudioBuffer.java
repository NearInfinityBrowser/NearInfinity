// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.sound;

import infinity.resource.key.ResourceEntry;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Abstract base class provides access to uncompressed PCM WAV audio data.
 * @author argent77
 */
public abstract class AudioBuffer
{
  /**
   * Contains uncompressed PCM WAV audio data (including WAV header)
   */
  protected byte[] data = null;

  public AudioBuffer(ResourceEntry entry) throws Exception
  {
    this(entry, null);
  }

  public AudioBuffer(ResourceEntry entry, AudioOverride override) throws Exception
  {
    if (entry != null) {
      convert(entry.getResourceData(), 0, override);
    } else
      throw new NullPointerException();
  }

  public AudioBuffer(byte[] buffer, int offset) throws Exception
  {
    this(buffer, offset, null);
  }

  public AudioBuffer(byte[] buffer, int offset, AudioOverride override) throws Exception
  {
    convert(buffer, offset, override);
  }


  /**
   * Returns the buffer of uncompressed PCM data including WAV header.
   * @return Buffer containing uncompressed PCM WAV data.
   */
  public byte[] getAudioData()
  {
    return data;
  }

  /**
   * Converts the source audio data into uncompressed PCM WAV data.
   * @param buffer Buffer containing source audio data.
   * @param offset Start offset into buffer.
   * @param override An optional override object to force certain properties of the audio format.
   * @throws Exception
   */
  protected abstract void convert(byte[] buffer, int offset, AudioOverride override) throws Exception;


  /**
   * Creates and returns a valid PCM WAV header structure.
   * @param samplesPerChannel Total number of samples per channel.
   * @param channels Number of sound channels in audio clip.
   * @param sampleRate Sample rate in Hz of audio clip.
   * @param bitsPerSample Bits per sample (8..32 bit supported)
   * @return A complete PCM WAV header
   * @throws Exception
   */
  protected static byte[] createWAVHeader(int samplesPerChannel, int channels, int sampleRate,
                                       int bitsPerSample) throws Exception
  {
    final int ID_CHUNK      = 0x46464952;     // 'RIFF'
    final int ID_FORMAT     = 0x45564157;     // 'WAVE'
    final int ID_SUBCHUNK1  = 0x20746d66;     // 'fmt '
    final int ID_SUBCHUNK2  = 0x61746164;     // 'data'

    // sanity checks
    if (samplesPerChannel < 1)
      throw new Exception("Invalid number of samples: " + samplesPerChannel);
    if (channels < 1 || channels > 2)
      throw new Exception("Unsupported number of channels: " + channels);
    if (sampleRate < 4096 || sampleRate > 192000)
      throw new Exception("Unsupported sample rate: " + sampleRate);
    switch (bitsPerSample) {
      case 8: case 16: case 24: case 32: break;
      default: throw new Exception("Unsupported bits per sample: " + bitsPerSample);
    }

    // setting required WAVE fields
    short blockAlign = (short)(channels * bitsPerSample / 8);
    int totalSize = samplesPerChannel * blockAlign;
    int byteRate = sampleRate * blockAlign;
    int chunkSize = 36 + totalSize;

    // writing header data
    ByteBuffer bb = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
    bb.putInt(ID_CHUNK);
    bb.putInt(chunkSize);
    bb.putInt(ID_FORMAT);
    bb.putInt(ID_SUBCHUNK1);
    bb.putInt(16);          // SubChunk1 size
    bb.putShort((short)1);  // PCM type
    bb.putShort((short)channels);
    bb.putInt(sampleRate);
    bb.putInt(byteRate);
    bb.putShort(blockAlign);
    bb.putShort((short)bitsPerSample);
    bb.putInt(ID_SUBCHUNK2);
    bb.putInt(totalSize);

    return bb.array();
  }


//-------------------------- INNER CLASSES --------------------------

  /**
   * Use to override autodetected properties of the source audio format.
   * Audio buffers are not required to honor the properties defined in the AudioOverride object.
   * @author argent77
   */
  public static class AudioOverride
  {
    protected int numChannels;
    protected int sampleRate;
    protected int bitsPerSample;

    /**
     * Creates an audio channel override object.
     * (A value of zero signals the audio decoder to ignore that parameter.
     *  A negative value lets the AudioBuffer object decide what to do with that parameter.)
     * @param numChannels Force to use <code>numChannels</code> audio channels.
     * @return An AudioOverride object initialized with the specified parameters.
     */
    public static AudioOverride overrideChannels(int numChannels)
    {
      return new AudioOverride(numChannels, 0, 0);
    }

    /**
     * Creates a sample rate override object.
     * (A value of zero signals the audio decoder to ignore that parameter.
     *  A negative value lets the AudioBuffer object decide what to do with that parameter.)
     * @param sampleRate Force a sample rate of <code>sampleRate</code> Hz.
     * @return An AudioOverride object initialized with the specified parameters.
     */
    public static AudioOverride overrideSampleRate(int sampleRate)
    {
      return new AudioOverride(0, sampleRate, 0);
    }

    /**
     * Creates a bits per sample override object.
     * (A value of zero signals the audio decoder to ignore that parameter.
     *  A negative value lets the AudioBuffer object decide what to do with that parameter.)
     * @param bitsPerSample Force <code>bitsPerSample</code> bits per sample.
     * @return An AudioOverride object initialized with the specified parameters.
     */
    public static AudioOverride overrideBitsPerSample(int bitsPerSample)
    {
      return new AudioOverride(0, 0, bitsPerSample);
    }

    /**
     * Creates an override object with the specified forced properties.
     * (A value of zero signals the audio decoder to ignore that parameter.
     *  A negative value lets the AudioBuffer object decide what to do with that parameter.)
     * @param numChannels Force to use <code>numChannels</code> audio channels.
     * @param sampleRate Force a sample rate of <code>sampleRate</code> Hz.
     * @param bitsPerSample Force <code>bitsPerSample</code> bits per sample.
     * @return An AudioOverride object initialized with the specified parameters.
     */
    public static AudioOverride override(int numChannels, int sampleRate, int bitsPerSample)
    {
      return new AudioOverride(numChannels, sampleRate, bitsPerSample);
    }


    private AudioOverride(int numChannels, int sampleRate, int bitsPerSample)
    {
      this.numChannels = numChannels;
      this.sampleRate = sampleRate;
      this.bitsPerSample = bitsPerSample;
    }
  }

}
