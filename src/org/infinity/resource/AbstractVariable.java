// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource;

import java.nio.ByteBuffer;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.FloatNumber;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.IsTextual;
import org.infinity.datatype.StringRef;
import org.infinity.datatype.TextString;
import org.infinity.util.io.StreamUtils;

public class AbstractVariable extends AbstractStruct implements AddRemovable
{
  // Variable-specific field labels
  public static final String VAR              = "Variable";
  public static final String VAR_NAME         = "Name";
  public static final String VAR_TYPE         = "Type";
  /** Attribute name for field with type {@link DecNumber}, representing reference value of this variable. */
  public static final String VAR_REFERENCE    = "Reference value (unused)";
  public static final String VAR_DWORD        = "Dword value (unused)";
  /** Attribute name for field with type {@link DecNumber}, representing integer value of this variable. */
  public static final String VAR_INT          = "Integer value";
  public static final String VAR_DOUBLE       = "Double value (unused)";
  public static final String VAR_SCRIPT_NAME  = "Script name (unused)";

  public static final String s_type[] = {"Integer", "Float", "Script name", "Resource reference",
                                         "String reference", "Double word"};

  /** Type of the variable. */
  public enum Type
  {
    /** {@link #getValue} return value as {@link Integer}. */
    Integer,
    /** {@link #getValue} return value as {@link Double}. */
    Double,
    /** {@link #getValue} return value as {@link String}. */
    ScriptName,
    /** {@link #getValue} return value as {@link Integer}. */
    ResRef,
    /** {@link #getValue} return value as {@link StringRef} with name of variable name. */
    StrRef,
    /** {@link #getValue} return value as {@link Long}. */
    Dword;

    /**
     * Returns value of specified variable according to the type.
     *
     * @param var Variable which value need to extract
     *
     * @return Value. See enum elements documentation for the returning type
     */
    public Object getValue(AbstractVariable var)
    {
      switch (this) {
        case Integer:    return ((IsNumeric)var.getAttribute(VAR_INT, false)).getValue();
        case Double:     return ((FloatNumber)var.getAttribute(VAR_DOUBLE, false)).getValue();
        case ScriptName: return ((IsTextual)var.getAttribute(VAR_SCRIPT_NAME, false)).getText();
        case ResRef:     return ((IsNumeric)var.getAttribute(VAR_REFERENCE, false)).getValue();
        case StrRef: {
          final IsTextual name  = (IsTextual)var.getAttribute(VAR_NAME, false);
          final IsNumeric value = (IsNumeric)var.getAttribute(VAR_REFERENCE, false);
          return new StringRef(name.getText(), value.getValue());
        }
        case Dword:      return ((IsNumeric)var.getAttribute(VAR_DWORD, false)).getLongValue();
      }
      throw new InternalError("Unknown enum variant: " + this);
    }
  }

  protected AbstractVariable() throws Exception
  {
    super(null, VAR, StreamUtils.getByteBuffer(84), 0);
  }

  protected AbstractVariable(AbstractStruct superStruct, ByteBuffer buffer, int offset, int nr)
      throws Exception
  {
    super(superStruct, VAR + " " + nr, buffer, offset);
  }

  protected AbstractVariable(AbstractStruct superStruct, String name, ByteBuffer buffer, int offset)
      throws Exception
  {
    super(superStruct, name, buffer, offset);
  }

  @Override
  public boolean canRemove()
  {
    return true;
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 32, VAR_NAME));
    addField(new Bitmap(buffer, offset + 32, 2, VAR_TYPE, s_type));
    addField(new DecNumber(buffer, offset + 34, 2, VAR_REFERENCE));
    addField(new DecNumber(buffer, offset + 36, 4, VAR_DWORD));
    addField(new DecNumber(buffer, offset + 40, 4, VAR_INT));
    addField(new FloatNumber(buffer, offset + 44, 8, VAR_DOUBLE));
    addField(new TextString(buffer, offset + 52, 32, VAR_SCRIPT_NAME));
    return offset + 84;
  }
  /**
   * Returns type of this variable. Currently for all games it is {@link Type#Integer}.
   *
   * @return Type of variable
   */
  public Type getType()
  {
    final IsNumeric type = (IsNumeric)getAttribute(VAR_TYPE, false);
    return Type.values()[type.getValue()];
  }
  /**
   * Returns value of this variable. Currently for all games it is {@link Integer}.
   *
   * @return Value of variable according to it
   */
  public Object getValue() { return getType().getValue(this); }
  /**
   * Returns value of the specified type of this variable. If variable have different
   * type, {@code null} is returned.
   *
   * @param type Type of value to get
   * @return Variable value or {@code null}. See documentation for {@link Type}
   *         enum values for the concrete returning type
   */
  public Object getValue(Type type)
  {
    final IsNumeric t = (IsNumeric)getAttribute(VAR_TYPE, false);
    return type.ordinal() == t.getValue() ? type.getValue(this) : null;
  }
}
