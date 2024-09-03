// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sound;

import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.sound.AudioBuffer.AudioOverride;
import org.infinity.util.DynamicArray;
import org.infinity.util.io.StreamUtils;
import org.tinylog.Logger;

public class AudioFactory {
  // supported audio formats
  private static enum AudioFormat {
    FMT_UNKNOWN, FMT_WAV, FMT_ACM, FMT_WAVC, FMT_OGG
  }

  /**
   * Attempts to detect the resource format of the specified resource and returns a matching AudioBuffer object.
   *
   * @param entry The audio resource to decode.
   * @return An AudioBuffer object, or null if resource format could not be determined.
   */
  public static AudioBuffer getAudioBuffer(ResourceEntry entry) {
    return getAudioBuffer(entry, null);
  }

  /**
   * Attempts to detect the resource format of the specified resource and returns a matching AudioBuffer object.
   *
   * @param entry    The audio resource to decode.
   * @param override An optional AudioOverride object containing audio properties to override.
   * @return An AudioBuffer object, or null if resource format could not be determined.
   */
  public static AudioBuffer getAudioBuffer(ResourceEntry entry, AudioOverride override) {
    if (entry != null) {
      try {
        return getAudioBuffer(StreamUtils.toArray(entry.getResourceBuffer()), 0, override);
      } catch (Exception e) {
        Logger.error(e);
        return null;
      }
    }
    return null;
  }

  /**
   * Attempts to detect the resource format of the specified data and returns a matching AudioBuffer object.
   *
   * @param buffer A buffer containing unprocessed audio data.
   * @param offset Start offset of the audio data.
   * @return An AudioBuffer object, or null on error or if audio format could not be determined.
   */
  public static AudioBuffer getAudioBuffer(byte[] buffer, int offset) {
    return getAudioBuffer(buffer, offset, null);
  }

  /**
   * Attempts to detect the resource format of the specified data and returns a matching AudioBuffer object.
   *
   * @param buffer   A buffer containing unprocessed audio data.
   * @param offset   Start offset of the audio data.
   * @param override An optional AudioOverride object containing audio properties to override.
   * @return An AudioBuffer object, or null on error or if audio format could not be determined.
   */
  public static AudioBuffer getAudioBuffer(byte[] buffer, int offset, AudioOverride override) {
    try {
      switch (detectFormat(buffer, offset)) {
        case FMT_WAV:
          return new WavBuffer(buffer, offset, override);
        case FMT_ACM:
          return new AcmBuffer(buffer, offset, override);
        case FMT_WAVC:
          return new WavcBuffer(buffer, offset, override);
        case FMT_OGG:
          return new OggBuffer(buffer, offset, override);
        default:
          return null;
      }
    } catch (Exception e) {
      Logger.error(e);
      return null;
    }
  }

  /**
   * Convenience method: Returns a buffer containing decoded audio data including WAV header.
   *
   * @param entry The audio resource to decode.
   * @return A buffer containing decoded audio data including WAV header or null on error.
   */
  public static byte[] convertAudio(ResourceEntry entry) {
    return convertAudio(entry, null);
  }

  /**
   * Convenience method: Returns a buffer containing decoded audio data including WAV header.
   *
   * @param entry    The audio resource to decode.
   * @param override An optional AudioOverride object containing audio properties to override.
   * @return A buffer containing decoded audio data including WAV header or null on error.
   */
  public static byte[] convertAudio(ResourceEntry entry, AudioOverride override) {
    if (entry != null) {
      try {
        return convertAudio(StreamUtils.toArray(entry.getResourceBuffer()), 0, override);
      } catch (Exception e) {
        Logger.error(e);
        return null;
      }
    }
    return null;
  }

  /**
   * Convenience method: Returns a buffer containing decoded audio data including WAV header.
   *
   * @param buffer A buffer containing unprocessed audio data.
   * @param offset Start offset of the audio data.
   * @return A buffer containing decoded audio data including WAV header or null on error.
   */
  public static byte[] convertAudio(byte[] buffer, int offset) {
    return convertAudio(buffer, offset, null);
  }

  /**
   * Convenience method: Returns a buffer containing decoded audio data including WAV header.
   *
   * @param buffer   A buffer containing unprocessed audio data.
   * @param offset   Start offset of the audio data.
   * @param override An optional AudioOverride object containing audio properties to override.
   * @return A buffer containing decoded audio data including WAV header or null on error.
   */
  public static byte[] convertAudio(byte[] buffer, int offset, AudioOverride override) {
    try {
      AudioBuffer ab = getAudioBuffer(buffer, offset, override);
      if (ab != null) {
        return ab.getAudioData();
      }
    } catch (Exception e) {
      Logger.error(e);
      return null;
    }
    return null;
  }

  // Detects supported audio formats
  private static AudioFormat detectFormat(byte[] buffer, int offset) {
    AudioFormat af = AudioFormat.FMT_UNKNOWN;
    try {
      if (buffer != null && offset >= 0 && offset + 4 < buffer.length) {
        String s = new String(buffer, offset, 4);
        if (s.equals("RIFF")) {
          if (offset + 44 < buffer.length && new String(buffer, offset + 8, 4).equals("WAVE")) {
            af = AudioFormat.FMT_WAV;
          }
        } else if (s.equals("WAVC")) {
          if (offset + 32 < buffer.length && new String(buffer, offset + 4, 4).equals("V1.0")) {
            af = AudioFormat.FMT_WAVC;
          }
        } else if (s.equals("OggS")) {
          af = AudioFormat.FMT_OGG;
        } else if (DynamicArray.getInt(buffer, offset) == 0x01032897) {
          af = AudioFormat.FMT_ACM;
        }
      }
    } catch (Throwable t) {
      Logger.trace(t);
    }
    return af;
  }
}
