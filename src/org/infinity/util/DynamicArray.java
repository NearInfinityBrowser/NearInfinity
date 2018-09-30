// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.nio.ByteOrder;
import java.nio.charset.Charset;


/**
 * An array object which mimics to a certain degree the type casting behavior of pointers from
 * other programming languages. The byte order is initially set to little endian.
 * Features include:
 * <ul>
 * <li>Converting between integer types of different sizes</li>
 * <li>Setting and modifying a base offset</li>
 * <li>Reading/writing data in big endian or little endian byte order</li>
 * <li>Static methods for quick array/value conversions</li>
 * </ul>
 */
public class DynamicArray
{
  /**
   * Supported data type sizes.
   */
  public static enum ElementType { BYTE, SHORT, INTEGER24, INTEGER, LONG }

  private byte[] buffer;
  private ElementType elementType;
  private int elementSize;
  private ByteOrder order;
  private int baseOfs;

  /**
   * Creates a new Array with a buffer of specified size.
   * @param size Number of logical elements in the buffer.
   * @param type The logical data type of the array.
   * @return An array object of the specified logical element type.
   */
  public static DynamicArray allocate(int size, ElementType type)
  {
    return new DynamicArray(size, type);
  }

  /**
   * Creates a new Array by using an existing array.
   * @param b The array that will back this object.
   * @param type The logical data type of the array.
   * @return An array object of the specified logical element type.
   */
  public static DynamicArray wrap(byte[] b, ElementType type)
  {
    return new DynamicArray(b, 0, type);
  }

  /**
   * Creates a new Array by using an existing array.
   * @param b The array that will back this object.
   * @param ofs The start ofs of the array that backs this object.
   * @param type The logical data type of the array.
   * @return An array object of the specified logical element type.
   */
  public static DynamicArray wrap(byte[] b, int ofs, ElementType type)
  {
    return new DynamicArray(b, ofs, type);
  }

  /**
   * Reads a byte from the specified buffer
   * @param buffer The buffer to read the value from.
   * @param offset Buffer offset.
   * @return Byte value from buffer or 0 on error.
   */
  public static byte getByte(byte[] buffer, int offset)
  {
    if (buffer != null && offset >= 0 && offset < buffer.length) {
      return buffer[offset];
    } else
      return 0;
  }

  /**
   * Writes a byte into the specified buffer.
   * @param buffer The buffer to write the value to.
   * @param offset Buffer offset.
   * @param value Value to write.
   * @return true if successfull, false otherwise.
   */
  public static boolean putByte(byte[] buffer, int offset, byte value)
  {
    if (buffer != null && offset >= 0 && offset < buffer.length) {
      buffer[offset] = value;
      return true;
    } else
      return false;
  }

  /**
   * Returns a byte array representation of the specified byte value.
   * @param value The value to convert into a byte array.
   * @return The byte array of the specified value.
   */
  public static byte[] convertByte(byte value)
  {
    return new byte[]{value};
  }

  /**
   * Reads a short from the specified buffer in little endian byte order.
   * @param buffer The buffer to read the value from.
   * @param offset Buffer offset.
   * @return Short value from buffer or 0 on error.
   */
  public static short getShort(byte[] buffer, int offset)
  {
    if (buffer != null && offset >= 0 && offset + 1 < buffer.length) {
      short v = (short)(buffer[offset] & 0xff);
      v |= (buffer[offset+1] & 0xff) << 8;
      return v;
    } else
      return 0;
  }

  /**
   * Writes a short into the specified buffer, using little endian byte order.
   * @param buffer The buffer to write the value to.
   * @param offset Buffer offset.
   * @param value Value to write.
   * @return true if successfull, false otherwise.
   */
  public static boolean putShort(byte[] buffer, int offset, short value)
  {
    if (buffer != null && offset >= 0 && offset + 1 < buffer.length) {
      buffer[offset] = (byte)(value & 0xff);
      buffer[offset+1] = (byte)((value >>> 8) & 0xff);
      return true;
    } else
      return false;
  }

  /**
   * Returns a byte array representation of the specified short value in little endian byte order.
   * @param value The value to convert into a byte array.
   * @return The byte array of the specified value.
   */
  public static byte[] convertShort(short value)
  {
    return new byte[]{(byte)(value & 0xff),
        (byte)((value >>> 8) & 0xff)};
  }

  /**
   * Reads a 24-bit integer from the specified buffer in little endian byte order.
   * @param buffer The buffer to read the value from.
   * @param offset Buffer offset.
   * @return 24-bit integer value from buffer or 0 on error.
   */
  public static int getInt24(byte[] buffer, int offset)
  {
    if (buffer != null && offset >= 0 && offset + 2 < buffer.length) {
      int v = buffer[offset] & 0xff;
      v |= (buffer[offset+1] & 0xff) << 8;
      v |= (buffer[offset+2] & 0xff) << 16;
      if ((v & 0x800000) != 0)    // sign-extending value
        v |= 0xff000000;
      return v;
    } else
      return 0;
  }

  /**
   * Writes a 24-bit integer into the specified buffer, using little endian byte order.
   * @param buffer The buffer to write the value to.
   * @param offset Buffer offset.
   * @param value Value to write.
   * @return true if successfull, false otherwise.
   */
  public static boolean putInt24(byte[] buffer, int offset, int value)
  {
    if (buffer != null && offset >= 0 && offset + 2 < buffer.length) {
      buffer[offset] = (byte)(value & 0xff);
      buffer[offset+1] = (byte)((value >>> 8) & 0xff);
      buffer[offset+2] = (byte)((value >>> 16) & 0xff);
      return true;
    } else
      return false;
  }

  /**
   * Returns a byte array representation of the specified 24-bit integer value in little endian byte order.
   * @param value The value to convert into a byte array.
   * @return The byte array of the specified value.
   */
  public static byte[] convertInt24(int value)
  {
    return new byte[]{(byte)(value & 0xff),
                      (byte)((value >>> 8) & 0xff),
                      (byte)((value >>> 16) & 0xff)};
  }

  /**
   * Reads an integer from the specified buffer in little endian byte order.
   * @param buffer The buffer to read the value from.
   * @param offset Buffer offset.
   * @return Integer value from buffer or 0 on error.
   */
  public static int getInt(byte[] buffer, int offset)
  {
    if (buffer != null && offset >= 0 && offset + 3 < buffer.length) {
      int v = buffer[offset] & 0xff;
      v |= (buffer[offset+1] & 0xff) << 8;
      v |= (buffer[offset+2] & 0xff) << 16;
      v |= (buffer[offset+3] & 0xff) << 24;
      return v;
    } else
      return 0;
  }

  /**
   * Writes an integer into the specified buffer, using little endian byte order.
   * @param buffer The buffer to write the value to.
   * @param offset Buffer offset.
   * @param value Value to write.
   * @return true if successfull, false otherwise.
   */
  public static boolean putInt(byte[] buffer, int offset, int value)
  {
    if (buffer != null && offset >= 0 && offset + 3 < buffer.length) {
      buffer[offset] = (byte)(value & 0xff);
      buffer[offset+1] = (byte)((value >>> 8) & 0xff);
      buffer[offset+2] = (byte)((value >>> 16) & 0xff);
      buffer[offset+3] = (byte)((value >>> 24) & 0xff);
      return true;
    } else
      return false;
  }

  /**
   * Returns a byte array representation of the specified integer value in little endian byte order.
   * @param value The value to convert into a byte array.
   * @return The byte array of the specified value.
   */
  public static byte[] convertInt(int value)
  {
    return new byte[]{(byte)(value & 0xff),
        (byte)((value >>> 8) & 0xff),
        (byte)((value >>> 16) & 0xff),
        (byte)((value >>> 24) & 0xff)};
  }

  /**
   * Reads a long from the specified buffer in little endian byte order.
   * @param buffer The buffer to read the value from.
   * @param offset Buffer offset.
   * @return Long value from buffer or 0 on error.
   */
  public static long getLong(byte[] buffer, int offset)
  {
    if (buffer != null && offset >= 0 && offset + 7 < buffer.length) {
      long v = buffer[offset] & 0xffL;
      v |= (buffer[offset+1] & 0xffL) << 8;
      v |= (buffer[offset+2] & 0xffL) << 16;
      v |= (buffer[offset+3] & 0xffL) << 24;
      v |= (buffer[offset+4] & 0xffL) << 32;
      v |= (buffer[offset+5] & 0xffL) << 40;
      v |= (buffer[offset+6] & 0xffL) << 48;
      v |= (buffer[offset+7] & 0xffL) << 56;
      return v;
    } else
      return 0;
  }

  /**
   * Writes a long into the specified buffer, using little endian byte order.
   * @param buffer The buffer to write the value to.
   * @param offset Buffer offset.
   * @param value Value to write.
   * @return true if successfull, false otherwise.
   */
  public static boolean putLong(byte[] buffer, int offset, long value)
  {
    if (buffer != null && offset >= 0 && offset + 7 < buffer.length) {
      buffer[offset] = (byte)(value & 0xffL);
      buffer[offset+1] = (byte)((value >>> 8) & 0xffL);
      buffer[offset+2] = (byte)((value >>> 16) & 0xffL);
      buffer[offset+3] = (byte)((value >>> 24) & 0xffL);
      buffer[offset+4] = (byte)((value >>> 32) & 0xffL);
      buffer[offset+5] = (byte)((value >>> 40) & 0xffL);
      buffer[offset+6] = (byte)((value >>> 48) & 0xffL);
      buffer[offset+7] = (byte)((value >>> 56) & 0xffL);
      return true;
    } else
      return false;
  }

  /**
   * Returns a byte array representation of the specified long value in little endian byte order.
   * @param value The value to convert into a byte array.
   * @return The byte array of the specified value.
   */
  public static byte[] convertLong(long value)
  {
    return new byte[]{(byte)(value & 0xffL),
                      (byte)((value >>> 8) & 0xffL),
                      (byte)((value >>> 16) & 0xffL),
                      (byte)((value >>> 24) & 0xffL),
                      (byte)((value >>> 32) & 0xffL),
                      (byte)((value >>> 40) & 0xffL),
                      (byte)((value >>> 48) & 0xffL),
                      (byte)((value >>> 56) & 0xffL)};
  }

  /**
  * Converts a byte sequence of a buffer into a string using the "windows-1252" charset.
  * @param buffer The buffer to read the byte sequence from.
  * @param offset Buffer offset.
  * @param length The number of bytes to convert.
  * @return A string representation of the byte sequence or an empty string on error.
  */
  public static String getString(byte[] buffer, int offset, int length)
  {
    return getString(buffer, offset, length, Misc.CHARSET_DEFAULT);
  }

  /**
   * Converts a byte sequence of a buffer into a string.
   * @param buffer The buffer to read the byte sequence from.
   * @param offset Buffer offset.
   * @param length The number of bytes to convert.
   * @param charset The charset to be used to decode the bytes.
   *                Specify {@code null} to use "windows-1252" charset.
   * @return A string representation of the byte sequence or an empty string on error.
   */
  public static String getString(byte[] buffer, int offset, int length, Charset charset)
  {
    if (buffer != null && offset >= 0 && offset < buffer.length && length >= 0) {
      if (charset == null) {
        charset = Misc.CHARSET_DEFAULT;
      }
      if (offset + length > buffer.length)
        length = buffer.length - offset;
      for (int i = 0; i < length; i++) {
        if (buffer[offset+i] == 0x00) {
          length = i;
          break;
        }
      }
      // XXX: Work around a bug in Java 6: String(buffer, ofs, len, charset) creates an internal copy of the whole buffer before proceeding
      byte[] temp = new byte[length];
      System.arraycopy(buffer, offset, temp, 0, length);
      return new String(temp, charset);
    }
    return "";
  }

  /**
   * Writes a text string into the specified buffer using the "windows-1252" charset for conversion.
   * @param buffer The buffer to write the value to.
   * @param offset Buffer offset.
   * @param length The max. length of the string to write.
   * @param s The (null-terminated) text string to write.
   * @return true if successfull, false otherwise.
   */
  public static boolean putString(byte[] buffer, int offset, int length, String s)
  {
    return putString(buffer, offset, length, s, Misc.CHARSET_DEFAULT);
  }

  /**
   * Writes a text string into the specified buffer, using the specified charset for conversion.
   * @param buffer The buffer to write the value to.
   * @param offset Buffer offset.
   * @param length The max. length of the string to write.
   * @param s The (null-terminated) text string to write.
   * @param cs The charset used to convert characters into bytes.
   * @return true if successfull, false otherwise.
   */
  public static boolean putString(byte[] buffer, int offset, int length, String s, Charset cs)
  {
    if (buffer != null && offset >= 0 && length >= 0 && offset+length <= buffer.length && s != null) {
      if (cs == null) {
        cs = Misc.CHARSET_DEFAULT;
      }
      byte[] buf = s.getBytes(cs);
      int len = Math.min(buffer.length - offset, buf.length);
      System.arraycopy(buf, 0, buffer, offset, len);
      if (offset+len < buffer.length) {
        buffer[offset+len] = 0; // writing string termination byte
      }
      return true;
    }
    return false;
  }

  /**
   * Convenience method to read an unsigned byte value from the specified buffer.
   * @param buffer The buffer to read the value from.
   * @param offset Buffer offset
   * @return An unsigned byte value.
   */
  public static short getUnsignedByte(byte[] buffer, int offset)
  {
    return (short)(getByte(buffer, offset) & 0xff);
  }

  /**
   * Convenience method to read an unsigned short value from the specified buffer
   * in little endian byte order.
   * @param buffer The buffer to read the value from.
   * @param offset Buffer offset
   * @return An unsigned short value.
   */
  public static int getUnsignedShort(byte[] buffer, int offset)
  {
    return ((int)getShort(buffer, offset) & 0xffff);
  }

  /**
   * Convenience method to read an unsigned 24-bit integer value from the specified buffer
   * in little endian byte order.
   * @param buffer The buffer to read the value from.
   * @param offset Buffer offset
   * @return An unsigned 24-bit integer value.
   */
  public static int getUnsignedInt24(byte[] buffer, int offset)
  {
    return (getInt24(buffer, offset) & 0xffffff);
  }

  /**
   * Convenience method to read an unsigned integer value from the specified buffer
   * in little endian byte order.
   * @param buffer The buffer to read the value from.
   * @param offset Buffer offset
   * @return An unsigned integer value.
   */
  public static long getUnsignedInt(byte[] buffer, int offset)
  {
    return ((long)getInt(buffer, offset) & 0xffffffffL);
  }

  /**
   * Creates and returns a shallow copy of the current object.
   */
  @Override
  public DynamicArray clone()
  {
    return new DynamicArray(buffer, baseOfs, elementType, order);
  }

  /**
   * Takes only the underlying array and the current base offset into account.
   */
  @Override
  public int hashCode()
  {
    int hash = 0;
    if (buffer != null)
      hash += buffer.hashCode();
    hash += Integer.hashCode(baseOfs);

    return hash;
  }

  /**
   * Indicates whether this object is equal to another. The objects are equal if both point to
   * the same array and are set to the same base offset.
   * @return true if both objects refer to the same array, starting at the same base offset.
   *         false otherwise.
   */
  @Override
  public boolean equals(Object obj)
  {
    if (obj != null && obj instanceof DynamicArray) {
      DynamicArray o = (DynamicArray)obj;
      if (buffer == o.buffer && baseOfs == o.baseOfs)
        return true;
    }
    return false;
  }

  /**
   * Returns the number of logical array elements.
   * @return Number of logical array elements.
   */
  public int size()
  {
    return buffer.length / elementSize;
  }

  /**
   * Returns the size of the underlying array in bytes.
   * @return Array size in bytes.
   */
  public int arraySize()
  {
    return buffer.length;
  }

  /**
   * Returns the size of a single logical array element.
   * @return Size of a single array element.
   */
  public int elementSize()
  {
    return elementSize;
  }

  public ElementType elementType()
  {
    return elementType;
  }

  /**
   * Modifies the array's byte order.
   * @param order The new byte order
   * @return This object
   */
  public DynamicArray setOrder(ByteOrder order)
  {
    if (order != null)
      this.order = order;

    return this;
  }

  /**
   * Returns the currently used byte order.
   * @return The current byte order
   */
  public ByteOrder getOrder()
  {
    return order;
  }

  /**
   * Returns the current base offset of the underlying array.
   * @return The current base offset.
   */
  public int getBaseOffset()
  {
    return baseOfs;
  }

  /**
   * Sets a new base offset for the underlying array. All read/write operations take this
   * offset into account automatically.
   * @param baseOffset The new base offset.
   * @return This object
   */
  public DynamicArray setBaseOffset(int baseOffset)
  {
    if (baseOffset >= 0 && baseOffset < buffer.length)
      this.baseOfs = baseOffset;

    return this;
  }

  /**
   * Adds the specified index to the current base offset of the underlying array.
   * @param index Adds an offset of {@code index} elements to the current base offset. Can be negative.
   * @return This object
   */
  public DynamicArray addToBaseOffset(int index)
  {
    int ofs = index * elementSize;
    if (baseOfs + ofs < 0)
      ofs = -baseOfs;
    if (baseOfs + ofs > buffer.length)
      ofs = buffer.length - baseOfs;

    baseOfs += ofs;

    return this;
  }

  /**
   * Returns the complete underlying array without regard to the current base offset.
   * @return The complete underlying array.
   */
  public byte[] getArray()
  {
    return buffer;
  }

  /**
   * Returns the array with a logical element type of byte.
   * @return A dynamic array of the current buffer with a byte element size.
   */
  public DynamicArray asByteArray()
  {
    return asByteArray(this.baseOfs);
  }

  /**
   * Returns the array with a logical element type of byte.
   * @param baseOfs The start offset of the underlying array (offset in bytes).
   * @return A dynamic array of the current buffer with a byte element size.
   */
  public DynamicArray asByteArray(int baseOfs)
  {
    return new DynamicArray(buffer, baseOfs, ElementType.BYTE, order);
  }

  /**
   * Returns the array with a logical element type of short.
   * @return A dynamic array of the current buffer with a short element size.
   */
  public DynamicArray asShortArray()
  {
    return asShortArray(this.baseOfs);
  }

  /**
   * Returns the array with a logical element type of short.
   * @param baseOfs The start offset of the underlying array (offset in bytes).
   * @return A dynamic array of the current buffer with a short element size.
   */
  public DynamicArray asShortArray(int baseOfs)
  {
    return new DynamicArray(buffer, baseOfs, ElementType.SHORT, order);
  }

  /**
   * Returns the array with a logical element type of 24-bit integer.
   * @return A dynamic array of the current buffer with a 24-bit integer element size.
   */
  public DynamicArray asInt24Array()
  {
    return asInt24Array(this.baseOfs);
  }

  /**
   * Returns the array with a logical element type of 24-bit integer.
   * @param baseOfs The start offset of the underlying array (offset in bytes).
   * @return A dynamic array of the current buffer with a 24-bit integer element size.
   */
  public DynamicArray asInt24Array(int baseOfs)
  {
    return new DynamicArray(buffer, baseOfs, ElementType.INTEGER24, order);
  }

  /**
   * Returns the array with a logical element type of integer.
   * @return A dynamic array of the current buffer with a integer element size.
   */
  public DynamicArray asIntArray()
  {
    return asIntArray(this.baseOfs);
  }

  /**
   * Returns the array with a logical element type of integer.
   * @param baseOfs The start offset of the underlying array (offset in bytes).
   * @return A dynamic array of the current buffer with a integer element size.
   */
  public DynamicArray asIntArray(int baseOfs)
  {
    return new DynamicArray(buffer, baseOfs, ElementType.INTEGER, order);
  }

  /**
   * Returns the array with a logical element type of long.
   * @return A dynamic array of the current buffer with a long element size.
   */
  public DynamicArray asLongArray()
  {
    return asLongArray(this.baseOfs);
  }

  /**
   * Returns the array with a logical element type of long.
   * @param baseOfs The start offset of the underlying array (offset in bytes).
   * @return A dynamic array of the current buffer with a long element size.
   */
  public DynamicArray asLongArray(int baseOfs)
  {
    return new DynamicArray(buffer, baseOfs, ElementType.LONG, order);
  }


  /**
   * Returns the byte value at the specified index.
   * @param index The logical index of the the value.
   * @return The byte value at the specified position.
   */
  public byte getByte(int index)
  {
    int ofs = baseOfs + index * elementSize;
    if (ofs < 0)
      throw new NullPointerException();

    byte v = 0;
    if (ofs < buffer.length)
      v = buffer[ofs];

    return v;
  }

  /**
   * Writes the specified byte value to the specified logical position.
   * @param index The logical index within the array to write the value to.
   * @param v The value to write
   * @return This object
   */
  public DynamicArray putByte(int index, byte v)
  {
    int ofs = baseOfs + index * elementSize;
    if (ofs < 0)
      throw new NullPointerException();

    if (ofs < buffer.length)
      buffer[ofs] = v;

    return this;
  }

  /**
   * Returns the short value at the specified index.
   * @param index The logical index of the the value.
   * @return The short value at the specified position.
   */
  public short getShort(int index)
  {
    int ofs = baseOfs + index * elementSize;
    if (ofs < 0)
      throw new NullPointerException();

    short v = 0;
    if (ofs < buffer.length)
      v |= buffer[ofs++] & 0xff;
    if (ofs < buffer.length)
      v |= (buffer[ofs++] & 0xff) << 8;

    return fixShortOrder(v);
  }

  /**
   * Writes the specified short value to the specified logical position.
   * @param index The logical index within the array to write the value to.
   * @param v The value to write
   * @return This object
   */
  public DynamicArray putShort(int index, short v)
  {
    int ofs = baseOfs + index * elementSize;
    if (ofs < 0)
      throw new NullPointerException();

    v = fixShortOrder(v);
    if (ofs < buffer.length)
      buffer[ofs++] = (byte)(v & 0xff);
    if (ofs < buffer.length)
      buffer[ofs++] = (byte)((v >>> 8) & 0xff);

    return this;
  }

  /**
   * Returns the 24-bit integer value at the specified index.
   * @param index The logical index of the the value.
   * @return The 24-bit integer value at the specified position.
   */
  public int getInt24(int index)
  {
    int ofs = baseOfs + index * elementSize;
    if (ofs < 0)
      throw new NullPointerException();

    int v = 0;
    if (ofs < buffer.length)
      v |= buffer[ofs++] & 0xff;
    if (ofs < buffer.length)
      v |= (buffer[ofs++] & 0xff) << 8;
    if (ofs < buffer.length)
      v |= (buffer[ofs++] & 0xff) << 16;
    v = fixInt24Order(v);
    if ((v & 0x800000) != 0)
      v |= 0xff000000;

    return v;
  }

  /**
   * Writes the specified 24-bit integer value to the specified logical position.
   * @param index The logical index within the array to write the value to.
   * @param v The value to write
   * @return This object
   */
  public DynamicArray putInt24(int index, int v)
  {
    int ofs = baseOfs + index * elementSize;
    if (ofs < 0)
      throw new NullPointerException();

    v = fixInt24Order(v);
    if (ofs < buffer.length)
      buffer[ofs++] = (byte)(v & 0xff);
    if (ofs < buffer.length)
      buffer[ofs++] = (byte)((v >>> 8) & 0xff);
    if (ofs < buffer.length)
      buffer[ofs++] = (byte)((v >>> 16) & 0xff);

    return this;
  }

  /**
   * Returns the integer value at the specified index.
   * @param index The logical index of the the value.
   * @return The integer value at the specified position.
   */
  public int getInt(int index)
  {
    int ofs = baseOfs + index * elementSize;
    if (ofs < 0)
      throw new NullPointerException();

    int v = 0;
    if (ofs < buffer.length)
      v |= buffer[ofs++] & 0xff;
    if (ofs < buffer.length)
      v |= (buffer[ofs++] & 0xff) << 8;
    if (ofs < buffer.length)
      v |= (buffer[ofs++] & 0xff) << 16;
    if (ofs < buffer.length)
      v |= (buffer[ofs++] & 0xff) << 24;

    return fixIntOrder(v);
  }

  /**
   * Writes the specified integer value to the specified logical position.
   * @param index The logical index within the array to write the value to.
   * @param v The value to write
   * @return This object
   */
  public DynamicArray putInt(int index, int v)
  {
    int ofs = baseOfs + index * elementSize;
    if (ofs < 0)
      throw new NullPointerException();

    v = fixIntOrder(v);
    if (ofs < buffer.length)
      buffer[ofs++] = (byte)(v & 0xff);
    if (ofs < buffer.length)
      buffer[ofs++] = (byte)((v >>> 8) & 0xff);
    if (ofs < buffer.length)
      buffer[ofs++] = (byte)((v >>> 16) & 0xff);
    if (ofs < buffer.length)
      buffer[ofs++] = (byte)((v >>> 24) & 0xff);

    return this;
  }

  /**
   * Returns the long value at the specified index.
   * @param index The logical index of the the value.
   * @return The long value at the specified position.
   */
  public long getLong(int index)
  {
    int ofs = baseOfs + index * elementSize;
    if (ofs < 0)
      throw new NullPointerException();

    long v = 0L;
    if (ofs < buffer.length)
      v |= (long)buffer[ofs++] & 0xffL;
    if (ofs < buffer.length)
      v |= ((long)buffer[ofs++] & 0xffL) << 8;
    if (ofs < buffer.length)
      v |= ((long)buffer[ofs++] & 0xffL) << 16;
    if (ofs < buffer.length)
      v |= ((long)buffer[ofs++] & 0xffL) << 24;
    if (ofs < buffer.length)
      v |= ((long)buffer[ofs++] & 0xffL) << 32;
    if (ofs < buffer.length)
      v |= ((long)buffer[ofs++] & 0xffL) << 40;
    if (ofs < buffer.length)
      v |= ((long)buffer[ofs++] & 0xffL) << 48;
    if (ofs < buffer.length)
      v |= ((long)buffer[ofs++] & 0xffL) << 56;

    return fixLongOrder(v);
  }

  /**
   * Writes the specified long value to the specified logical position.
   * @param index The logical index within the array to write the value to.
   * @param v The value to write
   * @return This object
   */
  public DynamicArray putLong(int index, long v)
  {
    int ofs = baseOfs + index * elementSize;
    if (ofs < 0)
      throw new NullPointerException();

    v = fixLongOrder(v);
    if (ofs < buffer.length)
      buffer[ofs++] = (byte)(v & 0xffL);
    if (ofs < buffer.length)
      buffer[ofs++] = (byte)((v >>> 8) & 0xffL);
    if (ofs < buffer.length)
      buffer[ofs++] = (byte)((v >>> 16) & 0xffL);
    if (ofs < buffer.length)
      buffer[ofs++] = (byte)((v >>> 24) & 0xffL);
    if (ofs < buffer.length)
      buffer[ofs++] = (byte)((v >>> 32) & 0xffL);
    if (ofs < buffer.length)
      buffer[ofs++] = (byte)((v >>> 40) & 0xffL);
    if (ofs < buffer.length)
      buffer[ofs++] = (byte)((v >>> 48) & 0xffL);
    if (ofs < buffer.length)
      buffer[ofs++] = (byte)((v >>> 56) & 0xffL);

    return this;
  }

  /**
   * Returns a subarray of specified size starting at the specified index.
   * @param index The logical starting index of the the subarray.
   * @param size The size of the the subarray in bytes.
   * @return The extracted subarray.
   */
  public byte[] get(int index, int size)
  {
    int ofs = baseOfs + index * elementSize;
    if (ofs < 0 || size < 0)
      throw new NullPointerException();

    int remaining = (ofs + size > buffer.length) ? ofs + size - buffer.length : 0;
    if (ofs + size > buffer.length)
      size = buffer.length - ofs;

    final byte[] b = new byte[size];
    System.arraycopy(buffer, ofs, b, 0, size);
    for (int i = 0; i < remaining; i++)
      b[ofs + size + i] = 0;

    return b;
  }

  /**
   * Writes the specified subarray into the array, starting at the specified logical array position.
   * @param index The logical starting index to write the subarray to.
   * @param b The subarray to write
   * @return This object
   */
  public DynamicArray put(int index, byte[] b)
  {
    int ofs = baseOfs + index * elementSize;
    if (ofs < 0 || b == null)
      throw new NullPointerException();

    int size = (ofs + b.length > buffer.length) ? buffer.length - ofs : b.length;
    System.arraycopy(b, 0, buffer, ofs, size);

    return this;
  }

  /**
   * Writes the specified subarray into the array, starting at the specified logical array position.
   * @param index The logical starting index to write the subarray to.
   * @param b The subarray to write
   * @param ofs The start offset of the subarray
   * @param len The number of bytes to write
   * @return This object
   */
  public DynamicArray put(int index, byte[] b, int ofs, int len)
  {
    int bufferOfs = baseOfs + index * elementSize;
    if (bufferOfs < 0 || b == null || ofs < 0 || ofs >= b.length || len < 0)
      throw new NullPointerException();
    if (ofs + len > b.length)
      len = b.length - ofs;

    if (len > 0) {
      int size = (bufferOfs + len > buffer.length) ? buffer.length - bufferOfs : len;
      System.arraycopy(b, ofs, buffer, bufferOfs, size);
    }

    return this;
  }

  /**
   * Convenience method: Returns the unsigned byte value at the specified index.
   * @param index The logical index of the the value.
   * @return The unsigned byte value at the specified position.
   */
  public short getUnsignedByte(int index)
  {
    return (short)(getByte(index) & 0xff);
  }

  /**
   * Convenience method: Returns the unsigned short value at the specified index.
   * @param index The logical index of the the value.
   * @return The unsigned short value at the specified position.
   */
  public int getUnsignedShort(int index)
  {
    return ((int)getShort(index) & 0xffff);
  }

  /**
   * Convenience method: Returns the unsigned 24-bit integer value at the specified index.
   * @param index The logical index of the the value.
   * @return The unsigned 24-byte integer value at the specified position.
   */
  public int getUnsignedInt24(int index)
  {
    return getInt24(index) & 0xffffff;
  }

  /**
   * Convenience method: Returns the unsigned integer value at the specified index.
   * @param index The logical index of the the value.
   * @return The unsigned integer value at the specified position.
   */
  public long getUnsignedInt(int index)
  {
    return ((long)getInt(index) & 0xffffffffL);
  }

  // Allocate a new buffer
  private DynamicArray(int size, ElementType elemType)
  {
    this(size, elemType, ByteOrder.LITTLE_ENDIAN);
  }

  // Allocate a new buffer
  private DynamicArray(int size, ElementType elemType, ByteOrder order)
  {
    createBuffer(size, elemType, order);
  }

  // Wraps an existing buffer
  private DynamicArray(byte[] buf, int ofs, ElementType elemType)
  {
    this(buf, ofs, elemType, ByteOrder.LITTLE_ENDIAN);
  }

  // Wraps an existing buffer
  private DynamicArray(byte[] buf, int ofs, ElementType elemType, ByteOrder order)
  {
    if (buf == null || elemType == null)
      throw new NullPointerException();
    if (ofs < 0)
      ofs = 0;
    if (ofs > buf.length)
      ofs = buf.length;

    setElementType(elemType);
    this.buffer = buf;
    this.baseOfs = ofs;
    this.order = (order != null) ? order : ByteOrder.LITTLE_ENDIAN;
  }

  // Sets new element size
  private void setElementType(ElementType type)
  {
    if (type != null) {
      switch (type) {
        case SHORT:
        elementType = type;
        elementSize = 2;
        break;
        case INTEGER24:
          elementType = type;
          elementSize = 3;
          break;
        case INTEGER:
        elementType = type;
        elementSize = 4;
        break;
        case LONG:
        elementType = type;
        elementSize = 8;
        break;
        case BYTE:
        default:
          elementType = ElementType.BYTE;
          elementSize = 1;
          break;
      }
    } else {
      elementType = ElementType.BYTE;
      elementSize = 1;
    }
  }

  // Creates a new buffer of specified size multiplied with elemSize
  private void createBuffer(int size, ElementType elemType, ByteOrder order)
  {
    if (size < 0 || elemType == null)
      throw new NullPointerException();

    setElementType(elemType);
    buffer = new byte[size*elementSize()];

    order = (order != null) ? order : ByteOrder.LITTLE_ENDIAN;
  }

  private short fixShortOrder(short v)
  {
    if (order == ByteOrder.BIG_ENDIAN) {
      short tmp = (short)((v <<  8) & 0xff00);
      tmp |=      (short)((v >>> 8) & 0xff);
      v = tmp;
    }
    return v;
  }

  private int fixInt24Order(int v)
  {
    if (order == ByteOrder.BIG_ENDIAN) {
      int tmp = (v <<  16) & 0xff0000;
      tmp |=     v         & 0xff00;
      tmp |=    (v >>> 16) & 0xff;
      v = tmp;
    }
    return v;
  }

  private int fixIntOrder(int v)
  {
    if (order == ByteOrder.BIG_ENDIAN) {
      int tmp = (v <<  24) & 0xff000000;
      tmp |=    (v <<   8) & 0xff0000;
      tmp |=    (v >>>  8) & 0xff00;
      tmp |=    (v >>> 24) & 0xff;
      v = tmp;
    }
    return v;
  }

  private long fixLongOrder(long v)
  {
    if (order == ByteOrder.BIG_ENDIAN) {
      long tmp = (v <<  56) & 0xff00000000000000L;
      tmp |=     (v <<  40) & 0xff000000000000L;
      tmp |=     (v <<  24) & 0xff0000000000L;
      tmp |=     (v <<   8) & 0xff00000000L;
      tmp |=     (v >>>  8) & 0xff000000L;
      tmp |=     (v >>> 24) & 0xff0000L;
      tmp |=     (v >>> 40) & 0xff00L;
      tmp |=     (v >>> 56) & 0xffL;
      v = tmp;
    }
    return v;
  }
}
