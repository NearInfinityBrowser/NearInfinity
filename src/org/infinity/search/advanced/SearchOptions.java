// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search.advanced;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.infinity.resource.Profile;

/**
 * Storage for parameters of a single search entry for "Advanced Search".
 */
public class SearchOptions implements Cloneable {
  /** How to search for fields. */
  public enum FieldMode {
    /** Search for field name (exact or as regular expression). */
    BY_NAME,
    /** Search for field offset relative to child element of specified search structure. */
    BY_RELATIVE_OFFSET,
    /** Search for field offset relative to resource start. */
    BY_ABSOLUTE_OFFSET,
  }

  /** How to match field values. */
  public enum ValueType {
    /** Match textually (exact or as regular expression). All simple field types are supported. */
    TEXT,
    /** Same as {@code ValueType.Text}, but additionally restrict text length and take resource type into account. */
    RESOURCE,
    /** Match by numeric value or range. All numeric field types are supported. */
    NUMBER,
    /** Same as {@code ValueType.Number}, but does not support ranges and allows binary matches (and/or/xor). */
    BITFIELD,
  }

  /** How to check numeric bitfield values. */
  public enum BitFieldMode {
    /** All marked and unmarked bits must match. */
    EXACT,
    /** All marked bits must match. */
    AND,
    /** One or more marked bits must match. */
    OR,
    /** Only one marked bit must match. */
    XOR,
  }

  /** Default value for numeric lower bound if not specified. */
  private static final int DEFAULT_NUMBER_MIN = 0;
  /** Default value for numeric upper bound if not specified. */
  private static final int DEFAULT_NUMBER_MAX = 32767;

  /**
   * Path to search structure. Root level is set implicitly. emptyStructure is returned for absolute field offset mode.
   */
  private final List<String> structure;
  private final List<String> emptyStructure;

  /** Whether to include substructures in search. */
  private boolean structureRecursive;
  /** Whether structure names are evaluated as regular expression. */
  private boolean structureRegex;
  /** Whether filter matches are restricted to the same structure if structure levels are equal. */
  private boolean structureGroup;

  /** How to find resource fields: e.g. by name, by offset (relative or absolute). */
  private FieldMode searchType;
  /** Field name if SearchType.byName is selected. */
  private String searchName;
  /** Whether field name is evaluated in a case-sensitive manner. */
  private boolean searchNameCase;
  /** Whether field name is evaluated as regular expression. */
  private boolean searchNameRegex;
  /** Field name if any of the offset-related SearchTypes are selected. */
  private int searchOffset;

  /** Datatype of value to search. */
  private ValueType valueType;
  /** Store search string used by {@code ValueType.Text} and {@code ValueType.Resource}. */
  private String valueText;
  /** Whether search string is evaluated in a case-sensitive manner. */
  private boolean valueTextCase;
  /** Whether search string is evaluated as regular expression. */
  private boolean valueTextRegex;
  /** Store numeric search value (lower bounds) used by {@code ValueType.Number} or {@code ValueType.Bitfield}. */
  private int valueNumberMin;
  /** Optionally store numeric search value (upper bounds) used by {@code ValueType.Number}. */
  private int valueNumberMax;
  /** Resource type (extension) to filter serach string of type {@code ValueType.Resource}. */
  private String valueResourceType;
  /** How to match bitfield bits. */
  private BitFieldMode bitFieldMode;

  /** Set to {@code true} to consider only entries where search values do NOT match. */
  private boolean invertMatch;

  /** Initializes a SearchOptions instance with default values. */
  public SearchOptions() {
    this(null);
  }

  /** Initializes a SearchOptions instance with settings from the specified SearchOption argument. */
  public SearchOptions(SearchOptions so) {
    structure = new ArrayList<>();
    emptyStructure = Collections.unmodifiableList(new ArrayList<String>(0));
    init(so);
  }

  /** Getter for resource structure list. Root level is implicitly set. */
  public List<String> getStructure() {
    return (getSearchType() == FieldMode.BY_ABSOLUTE_OFFSET) ? emptyStructure : structure;
  }

  /**
   * Returns whether to include substructures in search. Returns always {@code true} for search type "ByAbsoluteOffset"
   * and {@code false} for search type "ByRelativeOffset".
   */
  public boolean isStructureRecursive() {
    return (getSearchType() == FieldMode.BY_ABSOLUTE_OFFSET)
        || (structureRecursive && getSearchType() == FieldMode.BY_NAME);
  }

  /** Sets whether to include substructures in search. */
  public void setStructureRecursive(boolean set) {
    structureRecursive = set;
  }

  /** Returns whether structure names are evaluated as regular expression. */
  public boolean isStructureRegex() {
    return structureRegex;
  }

  /** Sets whether structure names are evaluated as regular expression. */
  public void setStructureRegex(boolean set) {
    structureRegex = set;
  }

  /**
   * Returns whether filter matches are restricted to the same structure if structure levels are equal. Returns always
   * {@code false} for search type "ByAbsoluteOffset".
   */
  public boolean isStructureGroup() {
    return (getSearchType() != FieldMode.BY_ABSOLUTE_OFFSET) && structureGroup;
  }

  /** Set whether filter matches are restricted to the same structure if structure levels are equal. */
  public void setStructureGroup(boolean set) {
    structureGroup = set;
  }

  /** Returns the current field mode. */
  public FieldMode getSearchType() {
    return searchType;
  }

  /** Returns the field name (only if search type is "ByName"). */
  public String getSearchName() {
    return (searchType == FieldMode.BY_NAME) ? searchName : "";
  }

  /** Returns whether text value is matched case-sensitive (only if search type is "ByName"). */
  public boolean isSearchNameCaseSensitive() {
    return searchType == FieldMode.BY_NAME && searchNameCase;
  }

  /** Returns whether text value is a regular expression (only if search type is "ByName"). */
  public boolean isSearchNameRegex() {
    return searchType == FieldMode.BY_NAME && searchNameRegex;
  }

  /** Sets field name and search type to "ByName". */
  public void setSearchName(String name, boolean caseSensitive, boolean regex) {
    searchType = FieldMode.BY_NAME;
    searchName = (name != null) ? name : "";
    searchNameCase = caseSensitive;
    searchNameRegex = regex;
  }

  /** Returns the field offset (only if search type is "ByRelativeOffset" or "ByAbsoluteOffset"). */
  public int getSearchOffset() {
    return searchOffset;
  }

  /** Sets field offset and search type to "ByRelativeOffset". */
  public void setSearchOffsetRelative(int offset) {
    setSearchOffset(FieldMode.BY_RELATIVE_OFFSET, offset);
  }

  /** Sets field offset and search type to "ByAbsoluteOffset". */
  public void setSearchOffsetAbsolute(int offset) {
    setSearchOffset(FieldMode.BY_ABSOLUTE_OFFSET, offset);
  }

  /** Sets field offset and specified offset-related field type. */
  public void setSearchOffset(FieldMode mode, int offset) {
    if (mode == FieldMode.BY_RELATIVE_OFFSET || mode == FieldMode.BY_ABSOLUTE_OFFSET) {
      searchType = mode;
      searchOffset = Math.max(offset, 0);
    }
  }

  /** Returns the current value type. */
  public ValueType getValueType() {
    return valueType;
  }

  /**
   * Returns text value (only if value type is "Text").
   */
  public String getValueText() {
    return (valueType == ValueType.TEXT) ? valueText : "";
  }

  /** Returns whether text value is matched case-sensitive (only if value type is "Text"). */
  public boolean isValueTextCaseSensitive() {
    return valueType == ValueType.TEXT && valueTextCase;
  }

  /** Returns whether text value is a regular expression (only if value type is "Text"). */
  public boolean isValueTextRegex() {
    return valueType == ValueType.TEXT && valueTextRegex;
  }

  /** Sets text value, whether it's case sensitive or a regular expression, and value type to "Text". */
  public void setValueText(String text, boolean caseSensitive, boolean regex) {
    valueType = ValueType.TEXT;
    valueText = (text != null) ? text : "";
    valueTextCase = caseSensitive;
    valueTextRegex = regex;
  }

  /** Returns lower bound of numeric value (only if value type is "Number"). */
  public int getValueNumberMin() {
    return (valueType == ValueType.NUMBER) ? valueNumberMin : DEFAULT_NUMBER_MIN;
  }

  /** Returns upper bound of numeric value (only if value type is "Number"). */
  public int getValueNumberMax() {
    return (valueType == ValueType.NUMBER) ? valueNumberMax : DEFAULT_NUMBER_MAX;
  }

  /** Returns whether numeric value is a range (only if value type is "Number"). */
  public boolean isValueNumberRange() {
    return (valueType == ValueType.NUMBER) && (valueNumberMin != valueNumberMax);
  }

  /** Sets lower and upper bound of numeric value to specified value, and value type to "Number". */
  public void setValueNumber(int value) {
    setValueNumber(value, value);
  }

  /** Sets lower and upper bound of numeric value, and value type to "Number". */
  public void setValueNumber(int valueMin, int valueMax) {
    valueType = ValueType.NUMBER;
    valueNumberMin = Math.min(valueMin, valueMax);
    valueNumberMax = Math.max(valueMax, valueMin);
  }

  /** Returns resource type (only if value type is "Resource"). */
  public String getValueResourceType() {
    return (valueType == ValueType.RESOURCE) ? valueResourceType : "";
  }

  /** Returns resource resref without extension (only if value type is "Resource"). */
  public String getValueResourceRef() {
    return (valueType == ValueType.RESOURCE) ? valueText : "";
  }

  /** Returns resource name with extension (only if value type is "Resource"). */
  public String getValueResource() {
    if (valueType == ValueType.RESOURCE)
      return valueText + (valueResourceType.isEmpty() ? "" : "." + valueResourceType);
    return "";
  }

  /** Sets resource resref and type, and value type to "Resource". */
  public void setValueResource(String name, String extension) {
    if (name == null)
      name = "";
    if (extension == null)
      extension = "";
    else if (extension.indexOf('.') < 0)
      extension = "." + extension;
    setValueResource(name + extension);
  }

  /** Sets full resource name, and value type to "Resource". */
  public void setValueResource(String resourceName) {
    valueType = ValueType.RESOURCE;
    if (resourceName == null)
      resourceName = "";
    int pos = resourceName.lastIndexOf('.');
    if (pos >= 0) {
      valueResourceType = resourceName.substring(pos + 1);
      valueText = resourceName.substring(0, pos);
    } else {
      valueText = resourceName;
    }
  }

  /** Returns bitfield value (only if value type is "Bitfield"). */
  public int getValueBitfield() {
    return (valueType == ValueType.BITFIELD) ? valueNumberMin : 0;
  }

  /** Returns bitfield mode (only if value type is "Bitfield"). */
  public BitFieldMode getBitfieldMode() {
    if (valueType == ValueType.BITFIELD)
      return bitFieldMode;
    return BitFieldMode.AND;
  }

  /** Sets bitfield value and mode, and value type to "Bitfield". */
  public void setValueBitfield(int value, BitFieldMode mode) {
    valueType = ValueType.BITFIELD;
    valueNumberMin = value;
    bitFieldMode = (mode != null) ? mode : BitFieldMode.AND;
  }

  /** Returns whether match is inverted. */
  public boolean isInvertMatch() {
    return invertMatch;
  }

  /** Sets whether match is inverted. */
  public void setInvertMatch(boolean set) {
    invertMatch = set;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    int cnt = 0;
    sb.append("structure");
    if (isStructureGroup()) {
      sb.append("(grouped");
      cnt++;
    }
    if (isStructureRecursive()) {
      sb.append(cnt > 0 ? "," : "(").append("recursive");
      cnt++;
    }
    if (isStructureRegex()) {
      sb.append(cnt > 0 ? "," : "(").append("regex");
      cnt++;
    }
    if (cnt > 0)
      sb.append(')');
    sb.append("=[root]");
    for (String item : getStructure())
      sb.append(">").append(item);

    switch (getSearchType()) {
      case BY_NAME:
        sb.append("; field name(").append(isSearchNameCaseSensitive() ? "case" : "nocase").append(',')
            .append(isSearchNameRegex() ? "regex" : "exact").append(")=").append(getSearchName());
        break;
      case BY_RELATIVE_OFFSET:
        sb.append("; field offset(rel)=0x").append(Integer.toHexString(getSearchOffset()));
        break;
      case BY_ABSOLUTE_OFFSET:
        sb.append("; field offset(abs)=0x").append(Integer.toHexString(getSearchOffset()));
        break;
      default:
    }

    switch (getValueType()) {
      case TEXT:
        sb.append("; search(text,").append(isValueTextCaseSensitive() ? "case" : "nocase").append(',')
            .append(isValueTextRegex() ? "regex" : "exact").append(")=").append(getValueText());
        break;
      case RESOURCE:
        sb.append("; search(resref,").append(getValueResourceType()).append(")=").append(getValueResourceRef());
        break;
      case NUMBER:
        sb.append("; search(numeric)=").append(getValueNumberMin());
        if (isValueNumberRange())
          sb.append("-").append(getValueNumberMax());
        break;
      case BITFIELD:
        sb.append("; search(bitfield,");
        switch (getBitfieldMode()) {
          case EXACT:
            sb.append("exact");
            break;
          case AND:
            sb.append("and");
            break;
          case OR:
            sb.append("or");
            break;
          case XOR:
            sb.append("xor");
            break;
          default:
        }
        sb.append(")=0x").append(Long.toHexString(getValueBitfield() & 0xffffffffL));
        break;
      default:
    }

    if (isInvertMatch())
      sb.append("; invert");

    return sb.toString();
  }

  @Override
  public Object clone() {
    return new SearchOptions(this);
  }

  /**
   * Attempts to fix invalid or inplausible options in the specified SearchOptions instance. Returns a SearchOptions
   * instance with default values if {@code null} is specified.
   */
  public static SearchOptions validate(SearchOptions so) {
    if (so == null)
      so = new SearchOptions();

    for (int i = so.structure.size() - 1; i >= 0; i--) {
      String item = so.structure.get(i);
      if (item == null) {
        so.structure.remove(i);
      } else {
        so.structure.set(i, item.trim());
      }
    }

    if (so.searchType == null)
      so.searchType = FieldMode.BY_NAME;

    if (so.searchName == null)
      so.searchName = "";
    else
      so.searchName = so.searchName.trim();

    so.searchOffset = Math.max(0, so.searchOffset);

    if (so.valueType == null)
      so.valueType = ValueType.TEXT;

    if (so.valueText == null)
      so.valueText = "";
    else
      so.valueText = so.valueText.trim();

    if (so.valueNumberMax < so.valueNumberMin)
      so.valueNumberMax = so.valueNumberMin;

    if (so.valueResourceType == null)
      so.valueResourceType = "";
    else {
      so.valueResourceType = so.valueResourceType.trim().toUpperCase();
      if (so.valueResourceType.length() > 4)
        so.valueResourceType = so.valueResourceType.substring(0, 4);
      if (!so.valueResourceType.isEmpty() && !Profile.isResourceTypeSupported(so.valueResourceType))
        so.valueResourceType = "";
      if (ValueType.RESOURCE.equals(so.valueType)) {
        int pos = so.valueText.lastIndexOf('.');
        if (pos >= 0)
          so.valueText = so.valueText.substring(0, pos);
        so.valueText = so.valueText.substring(0, Math.min(so.valueText.length(), 8));
      }
    }

    if (so.bitFieldMode == null)
      so.bitFieldMode = BitFieldMode.AND;
    if (ValueType.BITFIELD.equals(so.valueType))
      so.valueNumberMax = so.valueNumberMin;

    return so;
  }

  /**
   * Initialize instance with settings from the specified SearchOptions object, or use default values if not available.
   */
  private void init(SearchOptions so) {
    structure.clear();
    if (so != null)
      structure.addAll(so.structure);
    structureRecursive = so != null && so.structureRecursive;
    structureRegex = so != null && so.structureRegex;
    structureGroup = so == null || so.structureGroup;
    searchType = (so != null) ? so.searchType : FieldMode.BY_NAME;
    searchName = (so != null) ? so.searchName : "";
    searchNameCase = so != null && so.searchNameCase;
    searchNameRegex = so != null && so.searchNameRegex;
    searchOffset = (so != null) ? so.searchOffset : 0;
    valueType = (so != null) ? so.valueType : ValueType.TEXT;
    valueText = (so != null) ? so.valueText : "";
    valueTextCase = so != null && so.valueTextCase;
    valueTextRegex = so != null && so.valueTextRegex;
    valueNumberMin = (so != null) ? so.valueNumberMin : 0;
    valueNumberMax = (so != null) ? so.valueNumberMax : 32767;
    valueResourceType = (so != null) ? so.valueResourceType : "";
    bitFieldMode = (so != null) ? so.bitFieldMode : BitFieldMode.AND;
    invertMatch = so != null && so.invertMatch;
  }
}
