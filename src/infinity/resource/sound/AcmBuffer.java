// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.sound;

import infinity.resource.key.ResourceEntry;
import infinity.util.DynamicArray;

/**
 * Decodes ACM encoded audio data into uncompressed PCM WAV audio data.
 * @author argent77
 */
public class AcmBuffer extends AudioBuffer
{

  public AcmBuffer(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  public AcmBuffer(ResourceEntry entry, AudioOverride override) throws Exception
  {
    super(entry, override);
  }

  public AcmBuffer(byte[] buffer, int offset) throws Exception
  {
    super(buffer, offset);
  }

  public AcmBuffer(byte[] buffer, int offset, AudioOverride override) throws Exception
  {
    super(buffer, offset, override);
  }

//--------------------- Begin Class AudioBuffer ---------------------

  protected void convert(byte[] buffer, int offset, AudioOverride override) throws Exception
  {
    AcmReader acm = new AcmReader(buffer, offset, override);
    int numSamples = acm.getSampleCount();
    int numChannels = acm.getChannels();
    int sampleRate = acm.getSampleRate();
    int bitsPerSample = acm.getBitsPerSample();
    byte[] header = createWAVHeader(numSamples / numChannels, numChannels, sampleRate, bitsPerSample);
    data = new byte[header.length + numSamples * bitsPerSample / 8];
    System.arraycopy(header, 0, data, 0, header.length);
    acm.readSamples(data, header.length, numSamples);
  }

//--------------------- End Class AudioBuffer ---------------------


//-------------------------- INNER CLASSES --------------------------

  // ACM decoder class
  private static class AcmReader
  {
    private static final int ID_ACM = 0x01032897;

    private final DynamicArray bufferB; // ACM input buffer and offset

    private final AudioOverride override;
    private int signature = 0, levels = 0, subBlocks = 0, blockSize = 0, samplesReady = 0;
    private int numChannels = 0, sampleRate = 0;
    private int numSamples = 0;         // total count of sound samples
    private int samplesLeft = 0;        // count of unprocessed samples
    private DynamicArray blockI, valuesI;
    private ValueUnpacker unpacker;     // ACM-stream unpacker
    private SubbandDecoder decoder;     // Interplay's subband decoder


    private AcmReader(byte[] buffer, int offset) throws Exception
    {
      this(buffer, offset, null);
    }

    private AcmReader(byte[] buffer, int offset, AudioOverride override) throws Exception
    {
      if (buffer == null)
        throw new NullPointerException();
      if (offset < 0 || offset + 14 > buffer.length)
        throw new Exception("Input buffer too small");

      bufferB = DynamicArray.wrap(buffer, offset, DynamicArray.ElementType.BYTE);
      if (override == null)
        override = AudioOverride.override(0, 0, 0);
      this.override = override;

      init();
    }

    private void init() throws Exception
    {
      signature = bufferB.getInt(0);
      numSamples = bufferB.getInt(4);
      numChannels = bufferB.getShort(8);
      if (override.numChannels > 0)
        numChannels = override.numChannels;
      sampleRate = bufferB.getShort(10);
      if (override.sampleRate > 0)
        numChannels = override.sampleRate;
      short val = bufferB.getShort(12);
      levels = val & 0x0f;
      subBlocks = (val >>> 4) & 0x0fff;
      bufferB.addToBaseOffset(14);

      if (signature != ID_ACM)
        throw new Exception("Invalid ACM header signature");
      if (numSamples < 0)
        throw new Exception("Invalid number of sample: " + numSamples);
      if (numChannels < 1 || numChannels > 2)
        throw new Exception("Unsupported number of channels: " + numChannels);
      if (sampleRate < 4096 || sampleRate > 192000)
        throw new Exception("Unsupported sample rate: " + sampleRate);

      samplesLeft = numSamples;
      blockSize = (1 << levels) * subBlocks;
      blockI = DynamicArray.allocate(blockSize, DynamicArray.ElementType.INTEGER);
      unpacker = new ValueUnpacker(levels, subBlocks, bufferB);
      decoder = new SubbandDecoder(levels);
    }

    private void makeNewSamples()
    {
      unpacker.getOneBlock(blockI);
      decoder.decode(blockI, subBlocks);
      valuesI = blockI.clone();
      samplesReady = (blockSize > samplesLeft) ? samplesLeft : blockSize;
      samplesLeft -= samplesReady;
    }

    private int readSamples(byte[] outBuffer, int offset, int sampleCount)
    {
      DynamicArray bufS = DynamicArray.wrap(outBuffer, offset, DynamicArray.ElementType.SHORT);

      int res = 0;
      while (res < sampleCount) {
        if (samplesReady == 0) {
          if (samplesLeft == 0)
            break;
          makeNewSamples();
        }
        bufS.putShort(0, (short)(valuesI.getInt(0) >> levels));
        valuesI.addToBaseOffset(1);
        bufS.addToBaseOffset(1);
        res++;
        samplesReady--;
      }

      // fill remaining buffer space with silence
      for (int i = res; i < sampleCount; i++) {
        bufS.putShort(0, (short)0).addToBaseOffset(1);
      }

      return res;
    }

    private int getChannels()
    {
      return numChannels;
    }

    private int getSampleRate()
    {
      return sampleRate;
    }

    private int getSampleCount()
    {
      return numSamples;
    }

    private int getBitsPerSample()
    {
      if (override.bitsPerSample > 0)
        return override.bitsPerSample;
      else
        return 16;    // always 16 bit (unless overridden)
    }
  }


  private static class ValueUnpacker
  {
    private static final byte[] TABLE1 = new byte[] {
       0, 1, 2,  4, 5, 6,  8, 9,10,
      16,17,18, 20,21,22, 24,25,26,
      32,33,34, 36,37,38, 40,41,42,
       0, 1, 2,  4, 5   // added for type-safety reasons
    };
    private static final short[] TABLE2 = new short[] {
       0,  1,  2,  3,  4,   8,  9, 10, 11, 12,  16, 17, 18, 19, 20,  24, 25, 26, 27, 28,  32, 33, 34, 35, 36,
      64, 65, 66, 67, 68,  72, 73, 74, 75, 76,  80, 81, 82, 83, 84,  88, 89, 90, 91, 92,  96, 97, 98, 99,100,
     128,129,130,131,132, 136,137,138,139,140, 144,145,146,147,148, 152,153,154,155,156, 160,161,162,163,164,
     192,193,194,195,196, 200,201,202,203,204, 208,209,210,211,212, 216,217,218,219,220, 224,225,226,227,228,
     256,257,258,259,260, 264,265,266,267,268, 272,273,274,275,276, 280,281,282,283,284, 288,289,290,291,292,
       0,  1,  2    // added for type-safety reasons
    };
    private static final short[] TABLE3 = new short[] {
      0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A,
      0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A,
      0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A,
      0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A,
      0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A,
      0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5A,
      0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6A,
      0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7A,
      0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x8A,
      0x90, 0x91, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98, 0x99, 0x9A,
      0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5, 0xA6, 0xA7, 0xA8, 0xA9, 0xAA,
      0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06  // added for type-safety reasons
    };

    private int levels, subBlocks;
    private DynamicArray bufferB;
    private int nextBits, availBits;
    private int sbSize;
    private DynamicArray ampBufS, bufMiddleS;
    private DynamicArray blockI;

    private ValueUnpacker(int levels, int sbCount, DynamicArray bufB)
    {
      if (bufB == null)
        throw new NullPointerException();

      this.levels = levels;
      this.subBlocks = sbCount;
      this.bufferB = bufB.clone();
      this.nextBits = this.availBits = 0;
      this.sbSize = 1 << this.levels;
      this.ampBufS = bufMiddleS = this.blockI = null;
      init();
    }

    private void init()
    {
      ampBufS = DynamicArray.allocate(0x10000, DynamicArray.ElementType.SHORT);
      bufMiddleS = ampBufS.clone().addToBaseOffset(0x8000);
    }

    private void getOneBlock(DynamicArray blockI)
    {
      this.blockI = blockI.clone();
      int pwr = getBits(4) & 0x0f;
      int val = getBits(16) & 0xffff;
      int count = 1 << pwr;
      int v = 0;

      for (int i = 0; i < count; i++) {
        bufMiddleS.putShort(i, (short)v);
        v += val;
      }
      v = -val;
      for (int i = 0; i < count; i++) {
        bufMiddleS.putShort(-i-1, (short)v);
        v -= val;
      }

      for (int pass = 0; pass < sbSize; pass++) {
        int idx = getBits(5) & 0x1f;
        if (fillerProc(idx, pass, idx) == 0)
          return;
      }
    }

    // *** These functions are used to fill the buffer with the amplitude values ***
    private int fillerProc(int fn, int pass, int idx)
    {
      switch (fn & 31) {
        case 0:
          return zeroFill(pass, idx);
        case 3: case 4: case 5: case 6: case 7: case 8: case 9:
        case 10: case 11: case 12: case 13: case 14: case 15: case 16:
          return linearFill(pass, idx);
        case 17:
          return k1Bits3(pass, idx);
        case 18:
          return k1Bits2(pass, idx);
        case 19:
          return t1Bits5(pass, idx);
        case 20:
          return k2Bits4(pass, idx);
        case 21:
          return k2Bits3(pass, idx);
        case 22:
          return t2Bits7(pass, idx);
        case 23:
          return k3Bits5(pass, idx);
        case 24:
          return k3Bits4(pass, idx);
        case 26:
          return k4Bits5(pass, idx);
        case 27:
          return k4Bits4(pass, idx);
        case 29:
          return t3Bits7(pass, idx);
//        case 1: case 2: case 25: case 28: case 30: case 31:
        default:
          return return0(pass, idx);
      }
    }

    private int return0(int pass, int idx)
    {
      return 0;
    }

    // used when the whole column pass is zero-filled
    private int zeroFill(int pass, int idx)
    {
      DynamicArray sbI = blockI.clone().addToBaseOffset(pass);
      int step = sbSize;
      int i = subBlocks;
      do {
        sbI.putInt(0, 0).addToBaseOffset(step);
      } while ((--i) != 0);
      return 1;
    }

    private int linearFill(int pass, int idx)
    {
      int mask = (1 << idx) - 1;
      DynamicArray lbS = bufMiddleS.clone().addToBaseOffset(-1 << (idx-1));
      for (int i = 0; i < subBlocks; i++) {
        blockI.putInt(i*sbSize+pass, lbS.getShort((getBits(idx) & mask)));
      }
      return 1;
    }

    // column with number pass is filled with zeros, and also +/-1, zeros are repeated frequently
    private int k1Bits3(int pass, int idx)
    {
      // Efficiency (bits per value): 3-p0-2.5*p00, p00 - cnt of paired zeros, p0 - cnt of single zeros.
      // it makes sense to use, when the frequency of paired zeros (p00) is greater than 2/3
      for (int i = 0; i < subBlocks; i++) {
        prepareBits(3);
        if ((nextBits & 1) == 0) {
          availBits--;
          nextBits >>= 1;
          blockI.putInt(i*sbSize+pass, 0);
          if ((++i) == subBlocks)
            break;
          blockI.putInt(i*sbSize+pass, 0);
        } else if ((nextBits & 2) == 0) {
          availBits -= 2;
          nextBits >>= 2;
          blockI.putInt(i*sbSize+pass, 0);
        } else {
          blockI.putInt(i*sbSize+pass, bufMiddleS.getShort(((nextBits & 4) != 0) ? 1 : -1));
          availBits -= 3;
          nextBits >>= 3;
        }
      }
      return 1;
    }

    // column is filled with zero and +/-1
    private int k1Bits2(int pass, int idx)
    {
      // Efficiency: 2-P0. P0 - cnt of any zero (P0 = p0 + p00)
      // use it when P0 > 1/3
      for (int i = 0; i < subBlocks; i++) {
        prepareBits(2);
        if ((nextBits & 1) == 0) {
          availBits--;
          nextBits >>= 1;
          blockI.putInt(i*sbSize+pass, 0);
        } else {
          blockI.putInt(i*sbSize+pass, bufMiddleS.getShort(((nextBits & 2) != 0) ? 1 : -1));
          availBits -= 2;
          nextBits >>= 2;
        }
      }
      return 1;
    }

    // all the -1, 0, +1 triplets
    private int t1Bits5(int pass, int idx)
    {
      // Efficiency: always 5/3 bits per value
      // use it when P0 <= 1/3
      for (int i = 0; i < subBlocks; i++) {
        byte val = (byte)(getBits(5) & 0x1f);
        val = TABLE1[val];

        blockI.putInt(i*sbSize+pass, bufMiddleS.getShort((val & 3)-1));
        if ((++i) == subBlocks)
          break;
        val >>= 2;
        blockI.putInt(i*sbSize+pass, bufMiddleS.getShort((val & 3)-1));
        if ((++i) == subBlocks)
          break;
        val >>= 2;
        blockI.putInt(i*sbSize+pass, bufMiddleS.getShort(val-1));
      }
      return 1;
    }

    // -2, -1, 0, 1, 2, and repeating zeros
    private int k2Bits4(int pass, int idx)
    {
      // Efficiency: 4-2*p0-3.5*p00, p00 - cnt of paired zeros, p0 - cnt of single zeros.
      // makes sense to use when p00>2/3
      for (int i = 0; i < subBlocks; i++) {
        prepareBits(4);
        if ((nextBits & 1) == 0) {
          availBits--;
          nextBits >>= 1;
          blockI.putInt(i*sbSize+pass, 0);
          if ((++i) == subBlocks)
            break;
          blockI.putInt(i*sbSize+pass, 0);
        } else if ((nextBits & 2) == 0) {
          availBits -= 2;
          nextBits >>= 2;
          blockI.putInt(i*sbSize+pass, 0);
        } else {
          blockI.putInt(i*sbSize+pass,
                        ((nextBits & 8) != 0) ? bufMiddleS.getShort(((nextBits & 4) != 0) ? 2 : 1) :
                                                bufMiddleS.getShort(((nextBits & 4) != 0) ? -1 : -2));
          availBits -= 4;
          nextBits >>= 4;
        }
      }
      return 1;
    }

    // -2, -1, 0, 1, 2
    private int k2Bits3(int pass, int idx)
    {
      // Efficiency: 3-2*P0, P0 - cnt of any zero (P0 = p0 + p00)
      // use when P0>1/3
      for (int i = 0; i < subBlocks; i++) {
        prepareBits(3);
        if ((nextBits & 1) == 0) {
          availBits--;
          nextBits >>= 1;
          blockI.putInt(i*sbSize+pass, 0);
        } else {
          blockI.putInt(i*sbSize+pass,
                        ((nextBits & 4) != 0) ? bufMiddleS.getShort(((nextBits & 2) != 0) ? 2 : 1) :
                                                bufMiddleS.getShort(((nextBits & 2) != 0) ? -1 : -2));
          availBits -= 3;
          nextBits >>= 3;
        }
      }
      return 1;
    }

    // all the +/-2, +/-1, 0  triplets
    private int t2Bits7(int pass, int idx)
    {
      // Efficiency: always 7/3 bits per value
      // use it when p0 <= 1/3
      for (int i = 0; i < subBlocks; i++) {
        short val = (short)(getBits(7) & 0x7f);
        val = TABLE2[val];

        blockI.putInt(i*sbSize+pass, bufMiddleS.getShort((val & 7)-2));
        if ((++i) == subBlocks)
          break;
        val >>= 3;
        blockI.putInt(i*sbSize+pass, bufMiddleS.getShort((val & 7)-2));
        if ((++i) == subBlocks)
          break;
        val >>= 3;
        blockI.putInt(i*sbSize+pass, bufMiddleS.getShort(val-2));
      }
      return 1;
    }

    // fills with values: -3, -2, -1, 0, 1, 2, 3, and double zeros
    private int k3Bits5(int pass, int idx)
    {
      // Efficiency: 5-3*p0-4.5*p00-p1, p00 - cnt of paired zeros, p0 - cnt of single zeros, p1 - cnt of +/- 1.
      // can be used when frequency of paired zeros (p00) is greater than 2/3
      for (int i = 0; i < subBlocks; i++) {
        prepareBits(5);
        if ((nextBits & 1) == 0) {
          availBits--;
          nextBits >>= 1;
          blockI.putInt(i*sbSize+pass, 0);
          if ((++i) == subBlocks)
            break;
          blockI.putInt(i*sbSize+pass, 0);
        } else if ((nextBits & 2) == 0) {
          availBits -= 2;
          nextBits >>= 2;
          blockI.putInt(i*sbSize+pass, 0);
        } else if ((nextBits & 4) == 0) {
          blockI.putInt(i*sbSize+pass, bufMiddleS.getShort(((nextBits & 8) != 0) ? 1 : -1));
          availBits -= 4;
          nextBits >>= 4;
        } else {
          availBits -= 5;
          int val = (nextBits & 0x18) >> 3;
          nextBits >>= 5;
          if (val >= 2)
            val += 3;
          blockI.putInt(i*sbSize+pass, bufMiddleS.getShort(val-3));
        }
      }
      return 1;
    }

    // fills with values: -3, -2, -1, 0, 1, 2, 3.
    private int k3Bits4(int pass, int idx)
    {
      // Efficiency: 4-3*P0-p1, P0 - cnt of all zeros (P0 = p0 + p00), p1 - cnt of +/- 1.
      for (int i = 0; i < subBlocks; i++) {
        prepareBits(4);
        if ((nextBits & 1) == 0) {
          availBits--;
          nextBits >>= 1;
          blockI.putInt(i*sbSize+pass, 0);
        } else if ((nextBits & 2) == 0) {
          availBits -= 3;
          blockI.putInt(i*sbSize+pass, bufMiddleS.getShort(((nextBits & 4) != 0) ? 1 : -1));
          nextBits >>= 3;
        } else {
          int val = (nextBits & 0x0c) >> 2;
          availBits -= 4;
          nextBits >>= 4;
          if (val >= 2)
            val += 3;
          blockI.putInt(i*sbSize+pass, bufMiddleS.getShort(val-3));
        }
      }
      return 1;
    }

    // fills with values: +/-4, +/-3, +/-2, +/-1, 0, and double zeros
    private int k4Bits5(int pass, int idx)
    {
      // Efficiency: 5-3*p0-4.5*p00, p00 - cnt of paired zeros, p0 - cnt of single zeros.
      // makes sense to use when p00>2/3
      for (int i = 0; i < subBlocks; i++) {
        prepareBits(5);
        if ((nextBits & 1) == 0) {
          availBits--;
          nextBits >>= 1;
          blockI.putInt(i*sbSize+pass, 0);
          if ((++i) == subBlocks)
            break;
          blockI.putInt(i*sbSize+pass, 0);
        } else if ((nextBits & 2) == 0) {
          availBits -= 2;
          nextBits >>= 2;
          blockI.putInt(i*sbSize+pass, 0);
        } else {
          int val = (nextBits & 0x1c) >> 2;
          if (val >= 4)
            val++;
          blockI.putInt(i*sbSize+pass, bufMiddleS.getShort(val-4));
          availBits -= 5;
          nextBits >>= 5;
        }
      }
      return 1;
    }

    // fills with values: +/-4, +/-3, +/-2, +/-1, 0, and double zeros
    private int k4Bits4(int pass, int idx)
    {
      // Efficiency: 4-3*P0, P0 - cnt of all zeros (both single and paired).
      for (int i = 0; i < subBlocks; i++) {
        prepareBits(4);
        if ((nextBits & 1) == 0) {
          availBits--;
          nextBits >>= 1;
          blockI.putInt(i*sbSize+pass, 0);
        } else {
          int val = (nextBits & 0x0e) >> 1;
          availBits -= 4;
          nextBits >>= 4;
          if (val >= 4)
            val++;
          blockI.putInt(i*sbSize+pass, bufMiddleS.getShort(val-4));
        }
      }
      return 1;
    }

    // all the pairs of values from -5 to +5
    private int t3Bits7(int pass, int idx)
    {
      // Efficiency: 7/2 bits per value
      for (int i = 0; i < subBlocks; i++) {
        short val = (short)(getBits(7) & 0x7f);
        val = TABLE3[val];

        blockI.putInt(i*sbSize+pass, bufMiddleS.getShort((val & 0x0f) - 5));
        if ((++i) == subBlocks)
          break;
        val >>= 4;
        blockI.putInt(i*sbSize+pass, bufMiddleS.getShort(val-5));
      }
      return 1;
    }


    // request bits
    private void prepareBits(int bits)
    {
      while (bits > availBits) {
        int oneByte;
        if (bufferB.getBaseOffset() < bufferB.arraySize()) {
          oneByte = bufferB.getByte(0) & 0xff;
          bufferB.addToBaseOffset(1);
        } else
          oneByte = 0;
        nextBits |= oneByte << availBits;
        availBits += 8;
      }
    }

    // request and return next bits
    private int getBits(int bits)
    {
      prepareBits(bits);
      int res = nextBits;
      availBits -= bits;
      nextBits >>= bits;
      return res;
    }
  }


  private static class SubbandDecoder
  {
    private final int levels, blockSize;
    private DynamicArray memBufI;

    private SubbandDecoder(int levels)
    {
      this.levels = levels;
      blockSize = 1 << this.levels;
      init();
    }

    private void decode(DynamicArray bufferI, int blocks)
    {
      if (levels == 0)
        return;

      DynamicArray bufI = bufferI.clone();
      DynamicArray memI = memBufI.clone();
      int sbSize = blockSize >> 1;    // current subband size

      blocks <<= 1;
      sub_4d3fcc(memI.asShortArray(), bufI, sbSize, blocks);
      memI.addToBaseOffset(sbSize);

      for (int i = 0; i < blocks; i++) {
        bufI.putInt(i*sbSize, bufI.getInt(i*sbSize) + 1);
      }

      sbSize >>= 1;
      blocks <<= 1;

      while (sbSize != 0) {
        sub_4d420c(memI, bufI, sbSize, blocks);
        memI.addToBaseOffset(sbSize << 1);
        sbSize >>= 1;
        blocks <<= 1;
      }
    }

    private void init()
    {
      int memSize = (levels == 0) ? 0 : (3*(blockSize >> 1) - 2);
      if (memSize > 0) {
        memBufI = DynamicArray.allocate(memSize, DynamicArray.ElementType.INTEGER);
      }
    }

    private void sub_4d3fcc(DynamicArray memoryS, DynamicArray bufferI, int sbSize, int blocks)
    {
      DynamicArray memS = memoryS.clone();
      DynamicArray bufI = bufferI.clone();
      int row0 = 0, row1 = 0, row2 = 0, row3 = 0, db0 = 0, db1 = 0;
      int sbSize2 = sbSize * 2;
      int sbSize3 = sbSize * 3;
      if (blocks == 2) {
        for (int i = 0; i < sbSize; i++) {
          row0 = bufI.getInt(0);
          row1 = bufI.getInt(sbSize);
          bufI.putInt(0, bufI.getInt(0) + memS.getShort(0) + (memS.getShort(1) << 1));
          bufI.putInt(sbSize, (row0 << 1) - memS.getShort(1) - bufI.getInt(sbSize));
          memS.putShort(0, (short)row0);
          memS.putShort(1, (short)row1);

          memS.addToBaseOffset(2);
          bufI.addToBaseOffset(1);
        }
      } else if (blocks == 4) {
        for (int i = 0; i < sbSize; i++) {
          row0 = bufI.getInt(0);
          row1 = bufI.getInt(sbSize);
          row2 = bufI.getInt(sbSize2);
          row3 = bufI.getInt(sbSize3);

          bufI.putInt(0,        memS.getShort(0) + (memS.getShort(1) << 1) + row0);
          bufI.putInt(sbSize,  -memS.getShort(1) + (row0 << 1) - row1);
          bufI.putInt(sbSize2,  row0 + (row1 << 1) + row2);
          bufI.putInt(sbSize3, -row1 + (row2 << 1) - row3);

          memS.putShort(0, (short)row2);
          memS.putShort(1, (short)row3);

          memS.addToBaseOffset(2);
          bufI.addToBaseOffset(1);
        }
      } else {
        DynamicArray buf2I = bufI.clone();
        for (int i = 0; i < sbSize; i++) {
          buf2I.setBaseOffset(bufI.getBaseOffset());
          if ((blocks & 2) != 0) {
            row0 = buf2I.getInt(0);
            row1 = buf2I.getInt(sbSize);

            buf2I.putInt(0,       memS.getShort(0) + (memS.getShort(1) << 1) + row0);
            buf2I.putInt(sbSize, -memS.getShort(1) + (row0 << 1) - row1);
            buf2I.addToBaseOffset(sbSize2);

            db0 = row0;
            db1 = row1;
          } else {
            db0 = memS.getShort(0);
            db1 = memS.getShort(1);
          }

          for (int j = 0; j < (blocks >> 2); j++) {
            row0 = buf2I.getInt(0);
            buf2I.putInt(0, db0 + (db1 << 1) + row0);
            buf2I.addToBaseOffset(sbSize);

            row1 = buf2I.getInt(0);
            buf2I.putInt(0, -db1 + (row0 << 1) - row1);
            buf2I.addToBaseOffset(sbSize);

            row2 = buf2I.getInt(0);
            buf2I.putInt(0, row0 + (row1 << 1) + row2);
            buf2I.addToBaseOffset(sbSize);

            row3 = buf2I.getInt(0);
            buf2I.putInt(0, -row1 + (row2 << 1) - row3);
            buf2I.addToBaseOffset(sbSize);

            db0 = row2;
            db1 = row3;
          }
          memS.putShort(0, (short)row2);
          memS.putShort(1, (short)row3);
          memS.addToBaseOffset(2);
          bufI.addToBaseOffset(1);
        }
      }
    }

    private void sub_4d420c(DynamicArray memoryI, DynamicArray bufferI, int sbSize, int blocks)
    {
      DynamicArray memI = memoryI.clone();
      DynamicArray bufI = bufferI.clone();
      int row0 = 0, row1 = 0, row2 = 0, row3 = 0, db0 = 0, db1 = 0;
      int sbSize2 = sbSize * 2;
      int sbSize3 = sbSize * 3;
      if (blocks == 4) {
        for (int i = 0; i < sbSize; i++) {
          row0 = bufI.getInt(0);
          row1 = bufI.getInt(sbSize);
          row2 = bufI.getInt(sbSize2);
          row3 = bufI.getInt(sbSize3);

          bufI.putInt(0,        memI.getInt(0) + (memI.getInt(1) << 1) + row0);
          bufI.putInt(sbSize,  -memI.getInt(1) + (row0 << 1) - row1);
          bufI.putInt(sbSize2,  row0 + (row1 << 1) + row2);
          bufI.putInt(sbSize3, -row1 + (row2 << 1) - row3);

          memI.putInt(0, row2);
          memI.putInt(1, row3);

          memI.addToBaseOffset(2);
          bufI.addToBaseOffset(1);
        }
      } else {
        DynamicArray buf2I = bufI.clone();
        for (int i = 0; i < sbSize; i++) {
          buf2I.setBaseOffset(bufI.getBaseOffset());
          db0 = memI.getInt(0);
          db1 = memI.getInt(1);
          for (int j = 0; j < (blocks >> 2); j++) {
            row0 = buf2I.getInt(0);
            buf2I.putInt(0, db0 + (db1 << 1) + row0);
            buf2I.addToBaseOffset(sbSize);

            row1 = buf2I.getInt(0);
            buf2I.putInt(0, -db1 + (row0 << 1) - row1);
            buf2I.addToBaseOffset(sbSize);

            row2 = buf2I.getInt(0);
            buf2I.putInt(0, row0 + (row1 << 1) + row2);
            buf2I.addToBaseOffset(sbSize);

            row3 = buf2I.getInt(0);
            buf2I.putInt(0, -row1 + (row2 << 1) - row3);
            buf2I.addToBaseOffset(sbSize);

            db0 = row2;
            db1 = row3;
          }
          memI.putInt(0, row2);
          memI.putInt(1, row3);

          memI.addToBaseOffset(2);
          bufI.addToBaseOffset(1);
        }
      }
    }
  }
}
