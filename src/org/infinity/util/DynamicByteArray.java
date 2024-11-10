// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Implementation of a mutable sequence of bytes. The class provides methods for manipulating byte content and
 * modifying the buffer structure, such as adding or removing data.
 */
public class DynamicByteArray implements Iterable<Byte> {
  private static final int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;

  /** Internal byte buffer */
  private byte[] buffer;

  /** The current number of bytes used. */
  private int size;

  /**
   * Constructs an empty byte array with a capacity of 32 bytes.
   */
  public DynamicByteArray() {
    this(32);
  }

  /**
   * Constructs an empty byte array with the specified capacity.
   *
   * @param capacity the initial capacity in bytes.
   * @throws NegativeArraySizeException if {@code capacity} is less than {@code 0}.
   */
  public DynamicByteArray(int capacity) {
    this.buffer = new byte[capacity];
  }

  /**
   * Constructs a byte array initialized with the content of the specified buffer. The initial capacity is {@code 16}
   * plus the buffer length.
   *
   * @param buffer initial content of the byte buffer.
   * @throws NullPointerException if {@code buffer} argument is {@code null}.
   */
  public DynamicByteArray(byte[] buffer) {
    this(buffer, 0, buffer.length);
  }

  /**
   * Constructs a byte array initialized with the content of the specified buffer ranger. The initial capacity is
   * {@code 16} plus the length of the buffer range. @param buffer initial content of the byte buffer.
   *
   * @param offset index of the first byte to append.
   * @param len    number of bytes to append.
   * @return a reference to this object.
   * @throws NullPointerException           if {@code buffer} argument is {@code null}.
   * @throws ArrayIndexOutOfBoundsException if {@code offset < 0} or {@code len < 0} or
   *                                          {@code offset+len > buffer.length}.
   */
  public DynamicByteArray(byte[] buffer, int offset, int len) {
    this(Math.max(0, len) + 16);
    append(buffer, offset, len);
  }

  /** Returns the length of the buffer in bytes. */
  public int length() {
    return size;
  }

  /**
   * Sets the length of the buffer in bytes.
   * <p>
   * If the new length is less than the current length then excess bytes are discarded. If the new length is greater
   * than the current length then sufficient 0 byte values are appended so that length becomes the {@code newLength}
   * argument.
   * </p>
   * <p>
   * The {@code newLength} argument must be greater than or equal to {@code 0}.
   * </p>
   *
   * @param newLength the new length.
   * @throws ArrayIndexOutOfBoundsException if the {@code newLength} argument is negative.
   */
  public void setLength(int newLength) {
    if (newLength < 0) {
      throw new ArrayIndexOutOfBoundsException("newLength " + newLength);
    }
    if (newLength > size) {
      ensureCapacityInternal(newLength);
      Arrays.fill(buffer, size, newLength, (byte)0);
    }
    size = newLength;
  }

  /**
   * Returns {@code true} if, and only if, {@link length} is {@code null}.
   *
   * @return {@code true} if {@link length} is {@code 0}, otherwise {@code false}.
   */
  public boolean isEmpty() {
    return (length() == 0);
  }

  /**
   * Returns the capacity of the internal buffer in bytes. This is the maximum number of bytes the internal buffer
   * can hold without having to resize the buffer.
   */
  public int capacity() {
    return buffer.length;
  }

  /**
   * A maintenance method that truncates the internal buffer to the currently used buffer length.
   */
  public void compact() {
    if (buffer.length > size) {
      final byte[] newBuffer = Arrays.copyOf(buffer, size);
      buffer = newBuffer;
    }
  }

  public DynamicByteArray append(byte b) {
    ensureCapacityInternal(size + 1);
    this.buffer[size] = b;
    size += 1;
    return this;
  }

  public DynamicByteArray append(byte[] buf) {
    return append(buf, 0, buf.length);
  }

  public DynamicByteArray append(byte[] buf, int offset, int len) {
    if (len > 0) {
      ensureCapacityInternal(size + len);
    }
    System.arraycopy(buf, offset, this.buffer, size, len);
    size += len;
    return this;
  }

  public DynamicByteArray insert(int dstOffset, byte b) {
    ensureOffsetInclusive(dstOffset, 1);
    ensureCapacityInternal(size + 1);
    System.arraycopy(buffer, dstOffset, buffer, dstOffset + 1, size - dstOffset);
    buffer[dstOffset] = b;
    size += 1;
    return this;
  }

  public DynamicByteArray insert(int dstOffset, byte[] buf) {
    return insert(dstOffset, buf, 0, buf.length);
  }

  public DynamicByteArray insert(int dstOffset, byte[] buf, int srcOffset, int len) {
    ensureOffsetInclusive(dstOffset, len);
    ensureSrcBuffer(buf.length, srcOffset, len);
    ensureCapacityInternal(size + len);
    System.arraycopy(buffer, dstOffset, buffer, dstOffset + len, size - dstOffset);
    System.arraycopy(buf, srcOffset, buffer, dstOffset, len);
    size += len;
    return this;
  }

  public DynamicByteArray delete(int offset, int len) {
    ensureOffsetInclusive(offset, len);

    if (offset + len > size) {
      len = size - offset;
    }
    if (len > 0) {
      System.arraycopy(buffer, offset + len, buffer, offset, size - offset - len);
      size -= len;
    }
    return this;
  }

  public DynamicByteArray replace(int dstOffset, int len, byte[] buf) {
    return replace(dstOffset, len, buf, 0, buf.length);
  }

  public DynamicByteArray replace(int dstOffset, int dstLen, byte[] buf, int srcOffset, int srcLen) {
    ensureOffsetInclusive(dstOffset, dstLen);
    ensureSrcBuffer(buf.length, srcOffset, srcLen);
    delete(dstOffset, dstLen);
    insert(dstOffset, buf, srcOffset, srcLen);
    return this;
  }

  public DynamicByteArray fill(byte value) {
    return fill(value, 0, size);
  }

  public DynamicByteArray fill(byte value, int offset, int len) {
    ensureOffsetInclusive(offset, len);
    if (offset + len > size) {
      len = size - offset;
    }
    Arrays.fill(buffer, offset, offset + len, value);
    return this;
  }

  /**
   * Returns a copy of the byte array content.
   *
   * @return A new {@code byte[]} object with the content of this byte array.
   */
  public byte[] getArray() {
    return Arrays.copyOf(buffer, size);
  }

  public byte byteAt(int offset) {
    ensureOffsetExclusive(offset, 0);
    if (offset >= size) {
      throw new ArrayIndexOutOfBoundsException("offset " + offset + ", length() " + size);
    }
    return buffer[offset];
  }

  public byte[] subrange(int offset) {
    ensureOffsetInclusive(offset, 0);
    return subrange(offset, size - offset);
  }

  public byte[] subrange(int offset, int len) {
    ensureOffsetInclusive(offset, len);
    if (offset + len > size) {
      throw new ArrayIndexOutOfBoundsException("offset " + offset + ", len " + len + ", buffer.length() " + size);
    }
    return Arrays.copyOfRange(buffer, offset, offset + len);
  }

  public int indexOf(byte b) {
    return indexOf(b, 0);
  }

  public int indexOf(byte b, int startIndex) {
    if (startIndex < 0) {
      startIndex = 0;
    }
    for (int i = size - 1; i >= startIndex; i--) {
      if (buffer[i] == b) {
        return i;
      }
    }
    return -1;
  }

  public int indexOf(byte[] buf) {
    return indexOf(buf, 0, buf.length);
  }

  public int indexOf(byte[] buf, int offset, int len) {
    return indexOf(buf, offset, len, 0);
  }

  public int indexOf(byte[] buf, int offset, int len, int startIndex) {
    ensureSrcBuffer(buf.length, offset, len);
    return indexOf(buffer, 0, size, buf, offset, len, startIndex);
  }

  public int lastIndexOf(byte b) {
    for (int i = size - 1; i >= 0; i--) {
      if (buffer[i] == b) {
        return i;
      }
    }
    return -1;
  }

  public int lastIndexOf(byte b, int startIndex) {
    if (startIndex < 0) {
      return -1;
    }
    if (startIndex >= size) {
      startIndex = size - 1;
    }
    for (int i = startIndex; i >= 0; i--) {
      if (buffer[i] == b) {
        return i;
      }
    }
    return -1;
  }

  public int lastIndexOf(byte[] buf) {
    return lastIndexOf(buf, 0, buf.length);
  }

  public int lastIndexOf(byte[] buf, int offset, int len) {
    return indexOf(buf, offset, len, 0);
  }

  public int lastIndexOf(byte[] buf, int offset, int len, int startIndex) {
    ensureSrcBuffer(buf.length, offset, len);
    return lastIndexOf(buffer, 0, size, buf, offset, len, startIndex);
  }

  /**
   * Causes the byte sequence to be replaced by the reverse of the sequence.
   *
   * @return a reference to this object.
   */
  public DynamicByteArray reverse() {
    for (int left = 0, right = size - 1; left < right; left++, right--) {
      final byte b = buffer[left];
      buffer[left] = buffer[right];
      buffer[right] = b;
    }
    return this;
  }


  @Override
  public Iterator<Byte> iterator() {
    return new ByteIterator(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    for (int i = 0; i < size; i++) {
      result = prime * result + buffer[i];
    }
    result = prime * result + Objects.hash(size);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    DynamicByteArray other = (DynamicByteArray)obj;

    boolean result = (size == other.size);
    for (int i = 0; i < size && result; i++) {
      result = (buffer[i] == other.buffer[i]);
    }
    return result;
  }

  @Override
  public String toString() {
    return "DynamicByteArray [size=" + size + ", capacity=" + buffer.length + "]";
  }

  /**
   * Searches a source buffer for the content of the dest buffer.
   *
   * @param src        the source buffer being searched.
   * @param srcOffset  offset of the source buffer.
   * @param srcLen     length of the source buffer in bytes.
   * @param dst        the bytes being searched for.
   * @param dstOffset  offset of the search buffer.
   * @param dstLen     length of the search buffer in bytes.
   * @param startIndex index to begin the search in the source buffer from.
   * @return Index of the first match in the source buffer. Returns -1 if a match was not found.
   */
  private static int indexOf(byte[] src, int srcOffset, int srcLen, byte[] dst, int dstOffset, int dstLen,
      int startIndex) {
    if (startIndex >= srcLen) {
      return (dstLen == 0) ? srcLen : -1;
    }
    if (startIndex < 0) {
      startIndex = 0;
    }
    if (dstLen == 0) {
      return startIndex;
    }

    final byte first = dst[dstOffset];
    final int max = srcOffset + (srcLen - dstLen);

    for (int i = srcOffset + startIndex; i <= max; i++) {
      // looking for first byte
      if (src[i] != first) {
        while (++i <= max && src[i] != first)
          ;
      }

      // found first character, now looking at the rest of the array
      if (i <= max) {
        int j = i + 1;
        final int end = j + dstLen - 1;
        for (int k = dstOffset + 1; j < end && src[j] == dst[k]; j++, k++)
          ;
        if (j == end) {
          // found whole array
          return i - srcOffset;
        }
      }
    }
    return -1;
  }

  /**
   * Searches a source buffer for the content of the dest buffer.
   *
   * @param src        the source buffer being searched.
   * @param srcOffset  offset of the source buffer.
   * @param srcLen     length of the source buffer in bytes.
   * @param dst        the bytes being searched for.
   * @param dstOffset  offset of the search buffer.
   * @param dstLen     length of the search buffer in bytes.
   * @param startIndex index to begin the search in the source buffer from.
   * @return Index of the last match in the source buffer. Returns -1 if a match was not found.
   */
  private static int lastIndexOf(byte[] src, int srcOffset, int srcLen, byte[] dst, int dstOffset, int dstLen,
      int startIndex) {
    int rightIndex = srcLen - dstLen;
    if (startIndex < 0) {
      return -1;
    }
    if (startIndex > rightIndex) {
      startIndex = rightIndex;
    }
    if (dstLen == 0) {
      return startIndex;
    }

    final int lastIndex = dstOffset + dstLen - 1;
    final byte lastByte = dst[lastIndex];
    final int min = srcOffset + dstLen - 1;
    int i = min + startIndex;

    startSearchForLastByte: while (true) {
      while (i >= min && src[i] != lastByte) {
        i--;
      }
      if (i < min) {
        return -1;
      }
      int j = i - 1;
      final int start = j - (dstLen - 1);
      int k = lastIndex - 1;

      while (j > start) {
        if (src[j--] != dst[k--]) {
          i--;
          continue startSearchForLastByte;
        }
      }
      return start - srcOffset + 1;
    }
  }

  /**
   * Ensures that the capacity is at least equal to the specified minimum. If the current capacity is less than the
   * argument, then a new internal buffer is allocated with greater capacity.
   * <p>
   * If the {@code capacity} is nonpositive (e.g. due to numeric overflow), then a {@link OutOfMemoryError} is thrown.
   * </p>
   *
   * @param capacity the minimum desired capacity.
   */
  private void ensureCapacityInternal(int capacity) {
    if (capacity - buffer.length > 0) {
      buffer = Arrays.copyOf(buffer, newCapacity(capacity));
    }
  }

  /**
   * Throws a {@link ArrayIndexOutOfBoundsException} if {@code dstOffset} or {@code len} contain invalid values.
   *
   * @param dstOffset Offset in this byte buffer.
   * @param len       number of bytes to operate on.
   */
  private void ensureOffsetExclusive(int dstOffset, int len) {
    if (dstOffset < 0 || dstOffset >= length()) {
      throw new ArrayIndexOutOfBoundsException("dstOffset " + dstOffset);
    }
    if (len < 0) {
      throw new ArrayIndexOutOfBoundsException("len " + len);
    }
  }

  /**
   * Throws a {@link ArrayIndexOutOfBoundsException} if {@code dstOffset} or {@code len} contain invalid values.
   *
   * @param dstOffset Offset in this byte buffer.
   * @param len       number of bytes to operate on.
   */
  private void ensureOffsetInclusive(int dstOffset, int len) {
    if (dstOffset < 0 || dstOffset > length()) {
      throw new ArrayIndexOutOfBoundsException("dstOffset " + dstOffset);
    }
    if (len < 0) {
      throw new ArrayIndexOutOfBoundsException("len " + len);
    }
  }

  /**
   * Throws a {@link ArrayIndexOutOfBoundsException} if {@code srcOffset} or {@code len} contain invalid values.
   *
   * @param srcSize   Size of the reference buffer.
   * @param srcOffset Offset in the reference buffer.
   * @param len       number of bytes to operate on.
   */
  private void ensureSrcBuffer(int srcSize, int srcOffset, int len) {
    if (srcOffset < 0 || srcOffset >= srcSize) {
      throw new ArrayIndexOutOfBoundsException("srcOffset " + srcOffset);
    }
    if (len < 0 || srcOffset + len > srcSize) {
      throw new ArrayIndexOutOfBoundsException("srcOffset " + srcOffset + ", len " + len + ", buf.length " + srcSize);
    }
  }

  /**
   * Returns the capacity at least as large as the given minimum capacity. Returns the current capacity increased by the
   * same amount + 2 if that suffices. Will not return a capacity greater than {@code MAX_BUFFER_SIZE} unless the given
   * minimum capacity is greater than that.
   *
   * @param minCapacity the desirec minimum capacity.
   * @return the calculated capacity.
   * @throws OutOfMemoryError if {@code minCapacity} is less than zero or greater than {@link Integer#MAX_VALUE}.
   */
  private int newCapacity(int minCapacity) {
    int newCapacity = (buffer.length << 1) + 2;
    if (newCapacity - minCapacity < 0) {
      newCapacity = minCapacity;
    }
    return (newCapacity >= 0 || MAX_BUFFER_SIZE - newCapacity < 0) ? hugeCapacity(minCapacity) : newCapacity;
  }

  private int hugeCapacity(int minCapacity) {
    if (Integer.MAX_VALUE - minCapacity < 0) {
      // overflow
      throw new OutOfMemoryError();
    }
    return (minCapacity > MAX_BUFFER_SIZE) ? minCapacity : MAX_BUFFER_SIZE;
  }

  // -------------------------- INNER CLASSES --------------------------

  private static class ByteIterator implements Iterator<Byte> {
    private final DynamicByteArray array;

    private int index;

    public ByteIterator(DynamicByteArray array) {
      this.array = array;
      this.index = 0;
    }

    @Override
    public boolean hasNext() {
      return index < array.length();
    }

    @Override
    public Byte next() {
      if (index < array.length()) {
        return array.byteAt(index++);
      } else {
        throw new NoSuchElementException("index " + index + ", length() " + array.length());
      }
    }

    @Override
    public void remove() {
      if (index > 0) {
        array.delete(--index, 1);
      }
    }
  }
}
