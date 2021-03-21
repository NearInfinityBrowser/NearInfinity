// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder.util;

import java.util.Objects;

/**
 * Class for accessing creature animation attributes.
 */
public class DecoderAttribute implements Comparable<DecoderAttribute>
{
  /** Supported data types for creature animation attributes. */
  public enum DataType {
    /** Specifies an integer attribute. */
    INT(0),
    /** Specifies a floating point attribute (double). */
    DECIMAL(0.0),
    /** Specifies a boolean attribute. */
    BOOLEAN(false),
    /** Specifies a string attribute. */
    STRING(""),
    /** Specifies a custom data type of any kind. */
    USERDEFINED(null);

    public Object getDefaultValue() { return defValue; }

    private final Object defValue;
    private DataType(Object defValue) {
      this.defValue = defValue;
    }
  }

  private final String name;
  private final DataType type;
  private final Object defValue;

  /** Creates a new {@code Attribute} instance with the specified arguments. */
  public static DecoderAttribute with(String name, DataType type)
  {
    return new DecoderAttribute(name, type);
  }

  /** Creates a new {@code Attribute} instance with the specified arguments. */
  public static DecoderAttribute with(String name, DataType type, Object defValue)
  {
    return new DecoderAttribute(name, type, defValue);
  }

  public DecoderAttribute(String name, DataType type)
  {
    this(name, type, Objects.requireNonNull(type, "Attribute type cannot be null").getDefaultValue());
  }

  public DecoderAttribute(String name, DataType type, Object defValue)
  {
    this.name = Objects.requireNonNull(name, "Attribute name cannot be null");
    this.type = Objects.requireNonNull(type, "Attribute type cannot be null");
    this.defValue = defValue;
  }

  /** Returns the attribute key. */
  public String getName()
  {
    return name;
  }

  /** Returns the data type of the attribute value as one of several predefined enums. */
  public DataType getType()
  {
    return type;
  }

  /** Returns a default value that is returned if the actual value is not available. */
  public Object getDefaultValue()
  {
    return defValue;
  }

  @Override
  public String toString()
  {
    return "key=" + name + ", type=" + type.toString();
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == this) {
      return true;
    }
    if (!(o instanceof DecoderAttribute)) {
      return false;
    }
    DecoderAttribute other = (DecoderAttribute)o;
    boolean retVal = (this.name == null && other.name == null) ||
                     (this.name != null && this.name.equals(other.name));
    retVal &= (this.type == null && other.type == null) ||
              (this.type != null && this.type.equals(other.type));
    return retVal;
  }

  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 32 * hash + ((name == null) ? 0 : name.hashCode());
    hash = 32 * hash + ((type == null) ? 0 : type.hashCode());
    return hash;
  }

  @Override
  public int compareTo(DecoderAttribute o)
  {
    return name.compareTo(o.name);
  }
}
