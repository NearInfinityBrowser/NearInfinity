package org.infinity.util;

import java.nio.ByteBuffer;
import javax.xml.bind.DatatypeConverter;

import org.junit.Assert;
import org.junit.Test;

public class StaticSimpleXorDecryptorTest {
  //public static ByteBuffer decrypt(ByteBuffer buffer, int offset)
  @Test
  public void testWithoutOffset() {
    final String initialInputAsString = "0123456789ABCDEF";
    final String expectedOutputAsString = "898BCADD0378741A";//TODO: Blind assumption. Confirm.
    final String expectedOutputOfOutput = initialInputAsString;
    int offset = 0;

    ByteBuffer input = ByteBuffer.wrap(DatatypeConverter.parseHexBinary(initialInputAsString));
    ByteBuffer output = StaticSimpleXorDecryptor.decrypt(input, offset);
    byte[] outputAsArray = new byte[output.limit()];
    output.get(outputAsArray);
    Assert.assertEquals(expectedOutputAsString, DatatypeConverter.printHexBinary(outputAsArray));

    output.rewind();
    ByteBuffer outputOfOutput = StaticSimpleXorDecryptor.decrypt(output, offset);
    byte[] outputOfOutputAsArray = new byte[output.limit()];
    outputOfOutput.get(outputOfOutputAsArray);
    Assert.assertEquals(expectedOutputOfOutput, DatatypeConverter.printHexBinary(outputOfOutputAsArray));
  }

  @Test
  public void testWithOffset() {
    final String initialInputAsString = "0123456789ABCDEF";
    final String expectedOutputAsString = "CDCF0611473C0000";//TODO: Blind assumption. Confirm.
    final String expectedOutputOfOutput = "8EB9C8868AD30000";//TODO: Blind assumption. Confirm.
    int offset = 2;

    ByteBuffer input = ByteBuffer.wrap(DatatypeConverter.parseHexBinary(initialInputAsString));
    ByteBuffer output = StaticSimpleXorDecryptor.decrypt(input, offset);
    byte[] outputAsArray = new byte[output.limit()];
    output.get(outputAsArray);
    Assert.assertEquals(expectedOutputAsString, DatatypeConverter.printHexBinary(outputAsArray));

    output.rewind();
    ByteBuffer outputOfOutput = StaticSimpleXorDecryptor.decrypt(output, offset);
    byte[] outputOfOutputAsArray = new byte[output.limit()];
    outputOfOutput.get(outputOfOutputAsArray);
    Assert.assertEquals(expectedOutputOfOutput, DatatypeConverter.printHexBinary(outputOfOutputAsArray));
  }

  @Test(expected=IndexOutOfBoundsException.class)
  public void testWithOffsetSameAsLength() {
    final String initialInputAsString = "0123456789ABCDEF";

    ByteBuffer input = ByteBuffer.wrap(DatatypeConverter.parseHexBinary(initialInputAsString));
    StaticSimpleXorDecryptor.decrypt(input, input.limit());
  }

  @Test(expected=IndexOutOfBoundsException.class)
  public void testWithOffsetGreaterThanLength() {
    final String initialInputAsString = "0123456789ABCDEF";

    ByteBuffer input = ByteBuffer.wrap(DatatypeConverter.parseHexBinary(initialInputAsString));
    StaticSimpleXorDecryptor.decrypt(input, input.limit() + 2);
  }

  @Test(expected=IndexOutOfBoundsException.class)
  public void testWithNegativeOffset() {
    final String initialInputAsString = "0123456789ABCDEF";
    int offset = -1;

    ByteBuffer input = ByteBuffer.wrap(DatatypeConverter.parseHexBinary(initialInputAsString));
    StaticSimpleXorDecryptor.decrypt(input, offset);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testWithZeroLengthByteBuffer() {
    StaticSimpleXorDecryptor.decrypt(ByteBuffer.allocate(0), 0);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testWithNull() {
    StaticSimpleXorDecryptor.decrypt(null, 0);
  }
}
