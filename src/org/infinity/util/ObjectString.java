// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

/**
 * Associates a string with an object. Both string and object definitions are immutable.
 */
public class ObjectString implements CharSequence, Comparable<ObjectString>
{
  /** Print String together with associated object in parentheses. */
  public static final String FMT_OBJECT_BRACKETS  = "%1$s (%2$s)";
  /** Print String together with associated object after hyphen. */
  public static final String FMT_OBJECT_HYPHEN    = "%1$s - %2$s";
  /** Print only Object part of the ObjectString instance. */
  public static final String FMT_STRING_ONLY      = "%1$s";
  /** Print only String part of the ObjectString instance. */
  public static final String FMT_OBJECT_ONLY      = "%2$s";

  private final String string;
  private final Object object;

  private String displayFormat;

  /**
   * Helper routine: Associate strings with objects.
   */
  public static ObjectString[] createObjectStrings(String[] strings, Object[] objects, String fmt)
  {
    ObjectString[] retVal = null;
    if (strings != null && objects != null) {
      int size = Math.max(strings.length, objects.length);
      retVal = new ObjectString[size];
      for (int i = 0; i < size; i++) {
        String s = (i < strings.length) ? strings[i] : new String();
        Object o = (i < objects.length) ? objects[i] : null;
        retVal[i] = new ObjectString(s, o, fmt);
      }
    } else {
      retVal = new ObjectString[0];
    }
    return retVal;
  }

  /**
   * Helper routine: Automatically create string/index pairs from string array.
   */
  public static ObjectString[] createIndexedStrings(String[] strings, int startIndex, int ofsIndex,
                                                    String fmt)
  {
    ObjectString[] retVal = null;
    if (strings != null && startIndex < strings.length) {
      retVal = new ObjectString[strings.length - startIndex];
      for (int i = startIndex; i < strings.length; i++) {
        retVal[i - startIndex] = new ObjectString(strings[i], Integer.valueOf(i - startIndex + ofsIndex), fmt);
      }
    } else {
      retVal = new ObjectString[0];
    }
    return retVal;
  }

  /**
   * Helper routine: Automatically create string/index pairs from HashBitmap source.
   */
  public static ObjectString[] createIndexedStrings(LongIntegerHashMap<? extends Object>map, String fmt)
  {
    ObjectString[] retVal = null;
    if (map != null) {
      long[] keys = map.keys();
      retVal = new ObjectString[keys.length];
      for (int i = 0; i < keys.length; i++) {
        retVal[i] = new ObjectString(map.get(keys[i]).toString(), Integer.valueOf((int)keys[i]), fmt);
      }
    } else {
      retVal = new ObjectString[0];
    }
    return retVal;
  }


  /** Constructs an ObjectString with empty String object fields. */
  public ObjectString()
  {
    this(null, null, null);
  }

  /**
   * Constructs an ObjectString with the specified String and empty object field.
   */
  public ObjectString(String s)
  {
    this(s, null, null);
  }

  /**
   * Constructs an ObjectString with the specified String and object fields.
   */
  public ObjectString(String s, Object object)
  {
    this(s, object, null);
  }

  /**
   * Constructs an ObjectString with the specified String and object fields.
   * @param fmt Indicates how {@code toString()} should display the formatted string.
   */
  public ObjectString(String s, Object object, String fmt)
  {
    this.string = (s != null) ? s : "";
    this.object = object;
    this.displayFormat = (fmt != null) ? fmt : FMT_OBJECT_BRACKETS;
  }

  /**
   * Copy constructor for ObjectString.
   */
  public ObjectString(ObjectString os)
  {
    if (os != null) {
      this.string = os.getString();
      this.object = os.getObject();
      this.displayFormat = os.getDisplayFormat();
    } else {
      this.string = "";
      this.object = null;
      this.displayFormat = FMT_OBJECT_BRACKETS;
    }
  }

  @Override
  public boolean equals(Object o)
  {
    boolean eq = false;
    if (o instanceof ObjectString) {
      ObjectString os = (ObjectString)o;
      eq = string.equals(os.getString());
      if (eq) {
        eq = (object == null && os.getObject() == null) ||
             (object != null && object.equals(os.getObject()));
      }
    }
    return eq;
  }

//--------------------- Begin Interface CharSequence ---------------------

  @Override
  public int length()
  {
    return string.length();
  }

  public char charAt(int index)
  {
    return string.charAt(index);
  }

  public CharSequence subSequence(int start, int end)
  {
    return string.subSequence(start, end);
  }

  @Override
  public String toString()
  {
    return String.format(displayFormat, string, (object != null) ? object.toString() : "null");
  }

//--------------------- End Interface CharSequence ---------------------

//--------------------- Begin Interface Comparable ---------------------

  @Override
  public int compareTo(ObjectString o)
  {
    if (o != null) {
      return getString().compareTo(o.getString());
    } else {
      throw new NullPointerException();
    }
  }

//--------------------- End Interface Comparable ---------------------

  /** Returns associated string. */
  public String getString()
  {
    return string;
  }

  /** Returns associated object. */
  public Object getObject()
  {
    return object;
  }

  /**
   * Specify how to display textual output when using {@code toString()}.
   * @param fmt Indicates how {@code toString()} should display the formatted string.
   */
  public void setDisplayFormat(String fmt)
  {
    this.displayFormat = (fmt != null) ? fmt : FMT_OBJECT_BRACKETS;
  }

  /**
   * Returns the current display format for {@code toString()}.
   */
  public String getDisplayFormat()
  {
    return displayFormat;
  }
}
