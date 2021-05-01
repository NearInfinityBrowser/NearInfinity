// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Vector;

import org.infinity.util.io.StreamUtils;

// Create a new IE game resource from scratch.
public class ResourceStructure implements Cloneable
{
  public static final int ID_BYTE    = 0x01;    // integer, fixed size=1
  public static final int ID_WORD    = 0x02;    // integer, fixed size=2
  public static final int ID_DWORD   = 0x04;    // integer, fixed size=4
  public static final int ID_STRREF  = 0x04;    // integer, fixed size=4 (alias for ID_DWORD)
  public static final int ID_RESREF  = 0x08;    // string, default size=8
  public static final int ID_STRING  = 0x10;    // string, variable size
  public static final int ID_BUFFER  = 0x11;    // ByteBuffer, variable size

  private Vector<Item> list;
  private int cursize;

  public ResourceStructure()
  {
    super();
    list = new Vector<>();
    cursize = 0;
  }

  /** Specialized method: for fixed-sized entries only, value=0. */
  public int add(int type)
  {
    return insert(cursize, type);
  }

  /**
   * Specialized method: value argument interpreted as size for {@link #ID_STRING}
   * and {@link #ID_ARRAY}.
   */
  public int add(int type, int value)
  {
    return insert(cursize, type, value);
  }

  /** Specialized method: for strings only, string length determines size for {@link #ID_STRING}. */
  public int add(int type, String value)
  {
    return insert(cursize, type, value);
  }

  public int add(int type, int size, Object value)
  {
    return insert(cursize, type, size, value);
  }


  /** Specialized method: for fixed-sized entries only, value=0. */
  public int insert(int offset, int type)
  {
    switch (type) {
      case ID_BYTE:   return insert(offset, type, type & 0xf, (byte)0);
      case ID_WORD:   return insert(offset, type, type & 0xf, (short)0);
      case ID_DWORD:  return insert(offset, type, type & 0xf, 0);
      case ID_RESREF: return insert(offset, type, type & 0xf, "");
      default:        throw new IllegalArgumentException("Invalid type " + type);
    }
  }

  /**
   * Specialized method: value argument interpreted as size for {@link #ID_STRING}
   * and {@link #ID_ARRAY}.
   */
  public int insert(int offset, int type, int value)
  {
    switch (type) {
      case ID_BYTE:   return insert(offset, type, type & 0xf, (byte)value);
      case ID_WORD:   return insert(offset, type, type & 0xf, (short)value);
      case ID_DWORD:  return insert(offset, type, type & 0xf, value);
      case ID_STRING: return insert(offset, type, value, null);
      case ID_BUFFER: return insert(offset, type, value, null);
      default:        throw new IllegalArgumentException("Invalid type " + type);
    }
  }

  /** Specialized method: for strings only, string length determines size for {@link #ID_STRING}. */
  public int insert(int offset, int type, String value)
  {
    if (type == ID_RESREF) {
      return insert(offset, type, type & 0xf, value);
    }
    if (type == ID_STRING && value != null) {
      ByteBuffer buffer = StreamUtils.getByteBuffer(value.getBytes());
      return insert(offset, ID_BUFFER, buffer.limit(), buffer);
    }
    throw new IllegalArgumentException("Invalid type " + type);
  }

  /** Catch-all method: returns size of the whole structure after insertion */
  public int insert(int offset, int type, int size, Object value)
  {
    if (type == ID_BYTE || type == ID_WORD || type == ID_DWORD) {
      size = type & 0xf;
    }
    return insertItem(offset, type, size, value);
  }


  /** Remove item at offset, returns new size of the structure. */
  public int remove(int offset)
  {
    return removeItem(offset);
  }

  public void clear()
  {
    list.clear();
    cursize = 0;
  }

  public int size()
  {
    return cursize;
  }

  public boolean isEmpty()
  {
    return list.isEmpty();
  }

  public Item get(int offset)
  {
    return getItem(offset);
  }

  /** Returns whole structure as sequence of bytes. */
  public ByteBuffer getBuffer()
  {
    if (list.isEmpty()) {
      return StreamUtils.getByteBuffer(0);
    }
    ByteBuffer buf = StreamUtils.getByteBuffer(cursize);
    for (final Item e : list) {
      buf.put(e.toBuffer());
    }
    buf.position(0);
    return buf;
  }

  /** Returns item at specified offset as sequence of bytes. */
  public ByteBuffer getBuffer(int offset)
  {
    return getItem(offset).toBuffer();
  }

  public void write(OutputStream os) throws IOException
  {
    StreamUtils.writeBytes(os, getBuffer());
  }


  private boolean isValidItem(int type, int size, Object value)
  {
    if (size <= 0)
      return false;
    switch (type) {
      case ID_BYTE:
      case ID_WORD:
      case ID_DWORD:
        return (size == (type & 0xf));
      case ID_RESREF:
      case ID_STRING:
        return (value == null || (value instanceof String && ((String)value).getBytes().length <= size));
      case ID_BUFFER:
        return ((value == null) ||
                (value instanceof byte[]) ||
                (value instanceof ByteBuffer && ((ByteBuffer)value).limit() <= size));
      default:
        return false;
    }
  }

  /** Returns the index of the item located at {@code offset}. */
  private int indexOf(int offset)
  {
    if (offset < 0 || offset >= cursize) {
      throw new IndexOutOfBoundsException("Index out of range [0, " + cursize + "]: " + offset);
    }

    int curofs = 0;
    for (int i = 0; i < list.size(); i++) {
      int size = list.get(i).getSize();
      if (offset >= curofs && offset < curofs + size) {
        return i;
      }
      curofs += size;
    }

    throw new IndexOutOfBoundsException("Index out of range: " + offset);
  }

  private int insertItem(int offset, int type, int size, Object value)
  {
    if (offset < 0 || offset > cursize) {
      throw new IndexOutOfBoundsException("Index out of range [0, " + cursize+1 + "]: " + offset);
    }
    if (!isValidItem(type, size, value)) {
      throw new IllegalArgumentException("Try to insert invalid item");
    }
    if (offset == cursize) {
      list.add(new Item(type, size, value));
    } else {
      list.insertElementAt(new Item(type, size, value), indexOf(offset));
    }
    cursize += size;
    return cursize;
  }

  private int removeItem(int offset)
  {
    if (offset < 0 || offset >= cursize) {
      throw new IndexOutOfBoundsException("Index out of range [0, " + cursize + "]: " + offset);
    }
    int index = indexOf(offset);
    int size = list.get(index).getSize();
    list.remove(index);
    cursize -= size;
    return cursize;
  }

  private Item getItem(int offset)
  {
    return list.get(indexOf(offset));
  }


// --------------------- Begin Interface Cloneable ---------------------

  @Override
  public ResourceStructure clone()
  {
    ResourceStructure o = new ResourceStructure();
    o.list = new Vector<>(list);
    o.cursize = cursize;
    return o;
  }

// --------------------- End Interface Cloneable ---------------------

  @Override
  public boolean equals(Object obj)
  {
    if (obj == this)
      return true;
    if (!(obj instanceof ResourceStructure))
      return false;
    return (list.equals(((ResourceStructure)obj).list) && cursize == ((ResourceStructure)obj).cursize);
  }

  @Override
  public int hashCode()
  {
    return super.hashCode() + list.hashCode() + cursize;
  }


// -------------------------- INNER CLASSES --------------------------

  public final class Item implements Cloneable
  {
    private final int type;
    private final int size;
    private final Object value;

    public int getType()
    {
      return type;
    }

    // size always in bytes
    public int getSize()
    {
      return size;
    }

    public Object getValue()
    {
      return value;
    }

// --------------------- Begin Interface Cloneable ---------------------

    @Override
    public Item clone()
    {
      return new Item(type, size, value);
    }

// --------------------- End Interface Cloneable ---------------------

    @Override
    public boolean equals(Object obj)
    {
      if (obj == this)
        return true;
      if (!(obj instanceof Item))
        return false;
      return (type == ((Item)obj).type && size == ((Item)obj).size && value == ((Item)obj).value);
    }

    @Override
    public int hashCode()
    {
      return super.hashCode() + type + size + value.hashCode();
    }

    private Item(int type, int size, Object value)
    {
      this.type = type;
      this.size = (type == ID_BYTE || type == ID_WORD || type == ID_DWORD) ? type & 0xf : size;
      if (type == ID_RESREF || type == ID_STRING) {
        this.value = (value == null || !(value instanceof String)) ? "" : value;
      } else if (type == ID_BUFFER) {
        if (value instanceof byte[]) {
          this.value = StreamUtils.getByteBuffer((byte[])value);
        } else if (value instanceof ByteBuffer) {
          this.value = value;
        } else {
          this.value = StreamUtils.getByteBuffer(size);
        }
      } else {
        this.value = value;
      }
    }


    /** Returns the current item as a ByteBuffer object. */
    private ByteBuffer toBuffer()
    {
      if (size <= 0)
        throw new IndexOutOfBoundsException();

      ByteBuffer buf = StreamUtils.getByteBuffer(size);
      switch (type) {
        case ID_BYTE:
          if (value != null)
            buf.put((Byte)value);
          break;
        case ID_WORD:
          if (value != null)
            buf.putShort((Short)value);
          break;
        case ID_DWORD:
          if (value != null)
            buf.putInt((Integer)value);
          break;
        case ID_RESREF:
        case ID_STRING:
          if (value instanceof String) {
            byte[] b = ((String)value).getBytes();
            buf.put(Arrays.copyOf(b, b.length <= size ? b.length : size));
          }
          break;
        case ID_BUFFER:
          if (value instanceof ByteBuffer) {
            ByteBuffer b = (ByteBuffer)value;
            b.position(0);
            int len = Math.min(b.limit(), size);
            int limit = b.limit();
            if (len < b.limit()) {
              b.limit(len);
            }
            buf.put(b);
            b.limit(limit);
            b.position(0);
          }
          break;
      }
      buf.position(0);
      return buf;
    }
  }
}
