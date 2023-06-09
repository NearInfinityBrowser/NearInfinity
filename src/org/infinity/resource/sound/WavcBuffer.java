// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sound;

import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.DynamicArray;

/**
 * Decodes ACM encoded audio data in WAVC format into uncompressed PCM WAV audio data.
 */
public class WavcBuffer extends AcmBuffer {
  public WavcBuffer(ResourceEntry entry) throws Exception {
    super(entry);
  }

  public WavcBuffer(ResourceEntry entry, AudioOverride override) throws Exception {
    super(entry, override);
  }

  public WavcBuffer(byte[] buffer, int offset) throws Exception {
    super(buffer, offset);
  }

  public WavcBuffer(byte[] buffer, int offset, AudioOverride override) throws Exception {
    super(buffer, offset, override);
  }

  // --------------------- Begin Class AudioBuffer ---------------------

  @Override
  protected void convert(byte[] buffer, int offset, AudioOverride override) throws Exception {
    // parsing WAVC buffer
    if (!new String(buffer, 0, 4).equals("WAVC")) {
      throw new Exception("Invalid WAVC header");
    }
    if (!new String(buffer, 4, 4).equals("V1.0")) {
      throw new Exception("Unsupported WAVC version");
    }
    // int dsize = DynamicArray.getInt(buffer, 8);
    int csize = DynamicArray.getInt(buffer, 12);
    int acmOfs = DynamicArray.getInt(buffer, 16);
    if (acmOfs < 0x1c || acmOfs + csize > buffer.length - offset) {
      throw new Exception("Invalid WAVC header data");
    }
    int numChannels = DynamicArray.getUnsignedShort(buffer, offset + 20);
    if (numChannels < 1 || numChannels > 2) {
      throw new Exception("Unsupported number of audio channels: " + numChannels);
    }
    int bitsPerSample = DynamicArray.getUnsignedShort(buffer, offset + 22);
    if (bitsPerSample < 8 || bitsPerSample > 32) {
      throw new Exception("Unsupported bits/sample: " + bitsPerSample);
    }
    int sampleRate = DynamicArray.getUnsignedShort(buffer, offset + 24);
    if (sampleRate < 4096 || sampleRate > 192000) {
      throw new Exception("Unsupported sample rate: " + sampleRate);
    }

    if (override != null) {
      if (override.numChannels < 0) {
        override.numChannels = numChannels;
      }
      if (override.sampleRate < 0) {
        override.sampleRate = sampleRate;
      }
      if (override.bitsPerSample < 0) {
        override.bitsPerSample = bitsPerSample;
      }
    }

    // let AcmBuffer handle the remaining data
    super.convert(buffer, offset + acmOfs, override);
  }

  // --------------------- Begin Class AudioBuffer ---------------------
}
