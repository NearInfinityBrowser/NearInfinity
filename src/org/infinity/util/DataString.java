// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

/**
 * Associates a string with a typed object. Both string and object definitions are immutable.
 * @param <T> type of the data object associated with the {@code DataString}.
 */
public class DataString<T> implements CharSequence, Comparable<CharSequence>
{
  /** Print {@code String} together with associated data object in parentheses. */
  public static final String FMT_DATA_BRACKETS  = "%s (%s)";

  /** Print associated data object together with {@code String} in parentheses. */
  public static final String FMT_DATA_BRACKETS_REVERSED = "%2$s (%1$s)";

  /** Print {@code String} together with associated data object after hyphen. */
  public static final String FMT_DATA_HYPHEN    = "%s - %s";

  /** Print associated data object together with {@code String} after hyphen. */
  public static final String FMT_DATA_HYPHEN_REVERSED = "%2$s - %1$s";

  /** Print only the String part of the {@code DataString} instance. */
  public static final String FMT_STRING_ONLY    = "%1$s";

  /** Print only the data object part of the {@code DataString} instance. */
  public static final String FMT_DATA_ONLY      = "%2$s";


  private final String text;
  private final T data;

  private String fmt;

  /**
   * Creates a new {@code DataString} instance with the specified parameters.
   * @param <T> the data object type.
   * @param s the associated {@code String}.
   * @param data the associated data of type {@code T}.
   * @return a new {@code DataString} object.
   */
  public static <T> DataString<T> with(String s, T data)
  {
    return new DataString<>(s, data);
  }

  /**
   * Creates a new {@code DataString} instance with the specified parameters.
   * @param <T> the data object type.
   * @param s the associated {@code String}.
   * @param data the associated data of type {@code T}.
   * @param fmt a formatter string used by the {@link #toString()} method. Specifying {@code null} results in using
   *            the {@link #FMT_STRING_ONLY} formatter string.
   * @return a new {@code DataString} object.
   */
  public static <T> DataString<T> with(String s, T data, String fmt)
  {
    return new DataString<>(s, data, fmt);
  }

  /**
   * Constructs a new {@code DataString} object and initializes it with the specified arguments.
   * @param s the associated {@code String}.
   * @param data the associated data of type {@code T}.
   */
  public DataString(String s, T data)
  {
    this(s, data, null);
  }

  /**
   * Constructs a new {@code DataString} object and initializes it with the specified arguments.
   * @param s the associated {@code String}.
   * @param data the associated data of type {@code T}.
   * @param fmt a formatter string used by the {@link #toString()} method. Specifying {@code null} results in using
   *            the {@link #FMT_STRING_ONLY} formatter string.
   */
  public DataString(String s, T data, String fmt)
  {
    super();
    this.text = (s != null) ? s : "";
    this.data = data;
    setFormatter(fmt);
  }

//--------------------- Begin Interface CharSequence ---------------------

  /** Returns the length of the formatted string returned by {@link #toString()}. */
  @Override
  public int length()
  {
    return toString().length();
  }

  /**
   * Returns the {@code char} value at the specified index. An index ranges from {@code 0} to {@code length() - 1}.
   * The first char value of the sequence is at index {@code 0}, the next at index {@code 1}, and so on,
   * as for array indexing.
   * <p>The method is applied to the formatted string returned by {@link #toString()}.
   * <p>If the {@code char} value specified by the index is a surrogate, the surrogatevalue is returned.
   */
  @Override
  public char charAt(int index)
  {
    return toString().charAt(index);
  }

  /**
   * Returns a {@code CharSequence} that is a subsequence of this sequence. The subsequence starts with the
   * {@code char} value at the specified index and ends with the char value at index {@code end - 1}.
   * The length (in {@code char}s) of there turned sequence is {@code end - start}, so if {@code start == end}
   * then an empty sequence is returned.
   * <p>The method is applied to the formatted string returned by {@link #toString()}.
   * @param start Index the begin index, inclusive.
   * @param end the end index, exclusive.
   * @return the specified subsequence.
   */
  @Override
  public CharSequence subSequence(int start, int end)
  {
    return toString().subSequence(start, end);
  }

  /**
   * Returns a string with the content controlled by the associated formatter string.
   */
  @Override
  public String toString()
  {
    return String.format(getFormatter(), getString(), getData());
  }

//--------------------- End Interface CharSequence ---------------------

//--------------------- Begin Interface Comparable ---------------------

  /**
   * Compares this object with the specified object for order. Returns anegative integer, zero,
   * or a positive integer as this object is lessthan, equal to, or greater than the specified object.
   * <p>The method is applied to the formatted string returned by {@link #toString()}.
   */
  @Override
  public int compareTo(CharSequence o)
  {
    if (o == null) {
      return 1;
    }
    return toString().compareTo(o.toString());
  }

//--------------------- End Interface Comparable ---------------------

  @Override
  public boolean equals(Object anObject)
  {
    if (this == anObject) {
      return true;
    }
    if (!(anObject instanceof DataString)) {
      return false;
    }
    DataString<?> other = (DataString<?>)anObject;
    boolean retVal = text.equals(other.text);
    retVal &= (data == null && other.data == null) ||
              (data != null && data.equals(other.data));
    return retVal;
  }

  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 31 * hash + text.hashCode();
    hash = 31 * hash + ((data == null) ? 0 : data.hashCode());
    return hash;
  }

  /**
   * Returns the raw {@code String} associated with the {@code DataString}.
   * @return {@code String} part of the {@code DataString}
   */
  public String getString()
  {
    return text;
  }

  /**
   * Returns the data object associated with the {@code DataString}.
   * @return data object of type {@link T}.
   */
  public T getData()
  {
    return data;
  }

  /**
   * Returns the formatter string used by {@link #toString()} to generate the return value.
   */
  public String getFormatter()
  {
    return fmt;
  }

  /**
   * <p>Sets the formatter string that is used by {@link #toString()} to generate the return value.
   * <p>The formatter string may have up to two format specifiers. The first specifier is used for the string portion
   * of the {@code DataString}. The second specifier is used for the data portion of the {@code DataString}.
   * <p>The position flag of the specifiers can be used to control order of output.
   * @param fmt the formatter string. Specifying {@code null} results in using the {@link #FMT_STRING_ONLY}
   *            formatter string.
   */
  public void setFormatter(String fmt)
  {
    if (fmt == null) {
      fmt = FMT_STRING_ONLY;
    }
    if (!fmt.equals(this.fmt)) {
      this.fmt = fmt;
    }
  }

  /**
   * Returns {@code true} if, and only if, {@link #length()} is {@code 0}.
   * @return {@code true} if {@link #length()} is {@code null}, otherwise {@code false}.
   */
  public boolean isEmpty()
  {
    return toString().isEmpty();
  }
}
