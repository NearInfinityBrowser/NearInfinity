// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Vector;

import org.infinity.util.io.FileWriterNI;

// Create a new IE game resource from scratch.
public class ResourceStructure implements Cloneable
{
  public static final int ID_BYTE    = 0x01;    // integer, fixed size=1
  public static final int ID_WORD    = 0x02;    // integer, fixed size=2
  public static final int ID_DWORD   = 0x04;    // integer, fixed size=4
  public static final int ID_STRREF  = 0x04;    // integer, fixed size=4 (alias for ID_DWORD)
  public static final int ID_RESREF  = 0x08;    // string, default size=8
  public static final int ID_STRING  = 0x10;    // string, variable size
  public static final int ID_ARRAY   = 0x11;    // byte array, variable size

  private Vector<Item> list;
  private int cursize;

  public ResourceStructure()
  {
    super();
    list = new Vector<Item>();
    cursize = 0;
  }

  // Specialized method: for fixed-sized entries only, value=0
  public int add(int type)
  {
    return insert(cursize, type);
  }

  // Specialized method: value argument interpreted as size for ID_STRING and ID_ARRAY
  public int add(int type, int value)
  {
    return insert(cursize, type, value);
  }

  // Specialized method: for strings only, string length determines size for ID_STRING
  public int add(int type, String value)
  {
    return insert(cursize, type, value);
  }

  public int add(int type, int size, Object value)
  {
    return insert(cursize, type, size, value);
  }


  // Specialized method: for fixed-sized entries only, value=0
  public int insert(int offset, int type)
  {
    switch (type) {
      case ID_BYTE:   return insert(offset, type, type & 0xf, new Byte((byte)0));
      case ID_WORD:   return insert(offset, type, type & 0xf, new Short((short)0));
      case ID_DWORD:  return insert(offset, type, type & 0xf, new Integer(0));
      case ID_RESREF: return insert(offset, type, type & 0xf, "");
      default:        throw new IllegalArgumentException();
    }
  }

  // Specialized method: value argument interpreted as size for ID_STRING and ID_ARRAY
  public int insert(int offset, int type, int value)
  {
    switch (type) {
      case ID_BYTE:   return insert(offset, type, type & 0xf, new Byte((byte)value));
      case ID_WORD:   return insert(offset, type, type & 0xf, new Short((short)value));
      case ID_DWORD:  return insert(offset, type, type & 0xf, new Integer(value));
      case ID_STRING: return insert(offset, type, value, null);
      case ID_ARRAY:  return insert(offset, type, value, null);
      default:        throw new IllegalArgumentException();
    }
  }

  // Specialized method: for strings only, string length determines size for ID_STRING
  public int insert(int offset, int type, String value)
  {
    if (type == ID_RESREF)
      return insert(offset, type, type & 0xf, value);
    else if (type == ID_STRING && value != null)
      return insert(offset, ID_ARRAY, value.getBytes().length, value.getBytes());
    else
      throw new IllegalArgumentException();
  }

  // Catch-all method: returns size of the whole structure after insertion
  public int insert(int offset, int type, int size, Object value)
  {
    if (type == ID_BYTE || type == ID_WORD || type == ID_DWORD)
      size = type & 0xf;
    return insertItem(offset, type, size, value);
  }


  // Remove item at offset, returns new size of the structure
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

  // returns whole structure as sequence of bytes
  public byte[] getBytes()
  {
    if (list.size() == 0)
      return null;
    ByteBuffer buf = ByteBuffer.allocate(cursize);
    for (final Item e : list)
      buf.put(e.toBuffer());
    return buf.array();
  }

  // returns item at specified offset as sequence of bytes
  public byte[] getBytes(int offset)
  {
    return getItem(offset).toBuffer();
  }

  public void write(OutputStream os) throws IOException
  {
    byte[] data = getBytes();
    if (data != null)
      FileWriterNI.writeBytes(os, data);
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
      case ID_ARRAY:
        return (value == null || ((byte[])value).length <= size);
      default:
        return false;
    }
  }

  // returns the index of the item located at offset
  private int indexOf(int offset)
  {
    if (offset < 0 || offset >= cursize)
      throw new IndexOutOfBoundsException();

    int curofs = 0;
    for (int i = 0; i < list.size(); i++) {
      int size = list.get(i).getSize();
      if (offset >= curofs && offset < curofs + size)
        return i;
      curofs += size;
    }

    throw new IndexOutOfBoundsException();
  }

  private int insertItem(int offset, int type, int size, Object value)
  {
    if (offset >= 0 && offset <= cursize) {
      if (!isValidItem(type, size, value))
        throw new IllegalArgumentException();
      if (offset == cursize)
        list.add(new Item(type, size, value));
      else
        list.insertElementAt(new Item(type, size, value), indexOf(offset));
      cursize += size;
      return cursize;
    } else
      throw new IndexOutOfBoundsException();
  }

  private int removeItem(int offset)
  {
    if (offset >= 0 && offset < cursize) {
      int index = indexOf(offset);
      int size = list.get(index).getSize();
      list.remove(index);
      cursize -= size;
    } else
      throw new IndexOutOfBoundsException();

    return cursize;
  }

  private Item getItem(int offset)
  {
    return list.get(indexOf(offset));
  }


// --------------------- Begin Interface Cloneable ---------------------

  @Override
  public Object clone()
  {
    ResourceStructure o = new ResourceStructure();
    o.list = new Vector<Item>(list);
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
    public Object clone()
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
      if (type == ID_RESREF || type == ID_STRING)
        this.value = (value == null || !(value instanceof String)) ? "" : value;
      else if (type == ID_ARRAY)
        this.value = (value == null) ? new byte[]{0} : value;
        else
          this.value = value;
    }


    // returns the current item as a sequence of bytes
    private byte[] toBuffer()
    {
      if (size <= 0)
        throw new IndexOutOfBoundsException();

      ByteBuffer buf = ByteBuffer.allocate(size);
      buf.order(ByteOrder.LITTLE_ENDIAN);
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
          if (value != null && value instanceof String) {
            byte[] b = ((String)value).getBytes();
            buf.put(Arrays.copyOf(b, b.length <= size ? b.length : size));
          }
          break;
        case ID_ARRAY:
          if (value != null && value instanceof byte[]) {
            byte[] b = (byte[])value;
            buf.put(Arrays.copyOf(b, b.length <= size ? b.length : size));
          }
          break;
      }
      return buf.array();
    }
  }
}
