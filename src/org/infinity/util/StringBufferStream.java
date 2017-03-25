// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.nio.InvalidMarkException;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Wrapper for Strings which provides a pointer for traversing the string and methods for
 * reading or searching for characters or substrings.
 * <b>Note:</b> This class is not thread-safe.
 */
public class StringBufferStream
{
  private static final String WHITE_SPACE = " \t\r\n\f";

  private final String data;
  private final Stack<Integer> marker;
  private final int start;    // absolute start position inside the string
  private final int length;   // length of the (sub)string
  private int position;
  private boolean autoSkipWS;

  /**
   * Creates a StringBufferStream from the specified String object.
   * @param s The string to wrap.
   * @throws NullPointerException if {@code s} is {@code null}.
   */
  public StringBufferStream(String s)
  {
    if (s == null) {
      throw new NullPointerException();
    }

    this.data = s;
    this.start = 0;
    this.length = this.data.length();
    this.position = 0;
    this.autoSkipWS = false;
    this.marker = new Stack<>();
  }

  /**
   * Creates a StringBufferStream over a substring.
   * @param s The string to wrap.
   * @param start Start position for the stream.
   * @param length Length of the substring.
   * @throws NullPointerException if {@code s} is {@code null}.
   * @throws IllegalArgumentException if {@code start} is negative or greater as the string length,
   *                                  or {@code length} is negative or exceeds remaining string length.
   */
  public StringBufferStream(String s, int start, int length)
  {
    if (s == null) {
      throw new NullPointerException();
    }
    if (start < 0 || start > s.length()) {
      throw new IllegalArgumentException();
    }
    if (length < 0 || start + length > s.length()) {
      throw new IllegalArgumentException();
    }

    this.data = s;
    this.start = start;
    this.length = length;
    this.position = 0;
    this.autoSkipWS = false;
    this.marker = new Stack<>();
  }


  /** Returns the underlying String object. */
  public String buffer()
  {
    return data;
  }

  /** Returns the start offset inside the buffer. */
  public int bufferStart()
  {
    return start;
  }

  /** RReturns the defined length of the buffer. */
  public int bufferLength()
  {
    return length;
  }

  /** Returns the length of the underlying string. */
  public int size() { return length; }

  /** Returns the current string pointer position. */
  public int position() { return position; }

  /**
   * Sets string pointer to new position.
   * @param pos The new position within the string. Must be non-negative and no larger
   *            than the underlying string.
   * @return This stream.
   * @throws IllegalArgumentException if the resulting position is negative or larger than the
   *         underlying string.
   */
  public StringBufferStream position(int pos)
  {
    if (pos != position) {
      if (pos < 0 || pos > length) {
        throw new IllegalArgumentException();
      }
      position = pos;
    }
    return this;
  }

  /**
   * Moves string pointer by the specified amount.
   * @param offset Relative amount to move the current string pointer. Can be positive or negative.
   *
   * @return This stream.
   * @throws IllegalArgumentException if the resulting position is negative or larger than the
   *         underlying string.
   */
  public StringBufferStream move(int offset)
  {
    if (offset != 0) {
      int pos = position + offset;
      if (pos < 0 || pos > length) {
        throw new IllegalArgumentException();
      }
      position = pos;
    }
    return this;
  }

  /**
   * Returns whether whitespace character are automatically skipped when implicitly advancing the
   * string pointer by one of the {@code skip()} or {@code get()} routines.
   * @return
   */
  public boolean isAutoSkipWhitespace()
  {
    return autoSkipWS;
  }

  /**
   * Sets whether the string pointer should automatically skip whitespace when implicitly advancing
   * the string pointer by one of the {@code skip()} or {@code get()} routines.
   * @param set
   */
  public void setAutoSkipWhitespace(boolean set)
  {
    autoSkipWS = set;
  }

  /**
   * Moves the string pointer forward by one. Does nothing if end of string has been reached.
   * <b>Note:</b> Automatically skips subsequent whitespace characters if AutoSkip is enabled.
   * @return This stream.
   */
  public StringBufferStream skip()
  {
    if (position < length) {
      position++;
    }
    skipAutoWhitespace();
    return this;
  }

  /**
   * Moves the string pointer to the next occurrence of a non-whitespace character.
   * @return This stream.
   */
  public StringBufferStream skipWhitespace()
  {
    int pos = position;
    for (; pos < length; pos++) {
      if (WHITE_SPACE.indexOf(data.charAt(start + pos), 0) < 0) {
        break;
      }
    }
    position = pos;

    return this;
  }

  /**
   * Moves the string pointer behind the substring only if it matches the specified search string.
   * Does nothing if the search string does not match the substring starting at the current
   * string position.
   * <b>Note:</b> Automatically skips subsequent whitespace characters if AutoSkip is enabled and
   *              a match has been found.
   * @param s The search string.
   * @return {@code true} if the search string matches the text starting at the current position.
   *         In this case the string pointer is placed behind the matching substring.
   *         {@code false} otherwise. String pointer is not updated in this case.
   */
  public boolean skip(String s)
  {
    if (s == null) {
      throw new NullPointerException();
    }
    boolean retVal = false;
    if (s.isEmpty()) {
      retVal = true;
    } else {
      if (peek(s)) {
        move(s.length());
        retVal = true;
      }
    }
    if (retVal) skipAutoWhitespace();
    return retVal;
  }

  /**
   * Moves the string pointer behind the substring only if it matches the specified regular expression.
   * Does nothing if the regular expression does not match the substring starting at the current
   * string position.
   * <b>Note:</b> Automatically skips subsequent whitespace characters if AutoSkip is enabled and
   *              a match has been found.
   * @param regex The regular expression.
   * @return {@code true} if the search string matches the regular expression starting at the current
   *         position. In this case the string pointer is placed behind the matching substring.
   *         {@code false} otherwise. String pointer is not updated in this case.
   */
  public boolean skipMatch(String regex)
  {
    if (regex == null) {
      throw new NullPointerException();
    }

    boolean retVal = false;
    if (regex.isEmpty()) {
      retVal = true;
    } else {
      Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
      Matcher matcher = pattern.matcher(data);
      if (matcher.find(start + position) &&
          matcher.start() == start + position &&
          matcher.end() <= start + length) {
        position(matcher.end());
        retVal = true;
      }
    }
    if (retVal) skipAutoWhitespace();
    return retVal;
  }

  /**
   * Returns {@code true} if string pointer has reached the end of the string.
   * Returns {@code false} otherwise.
   */
  public boolean eos()
  {
      return (position >= length);
  }

  /**
   * Adds the current string position to the stack. Each call adds a new mark to the stack
   * which will be restored by a subsequent {@link reset()} call.
   * @return This stream.
   */
  public StringBufferStream mark()
  {
    marker.push(Integer.valueOf(position));
    return this;
  }

  /**
   * Removes the previously set mark from the stack. Use {@link hasMark()} if a mark is available.
   * @throws InvalidMarkException if no mark has been set.
   */
  public StringBufferStream unmark()
  {
    if (marker.empty()) {
      throw new InvalidMarkException();
    }

    marker.pop();
    return this;
  }

  /** Returns whether one or more markers have been defined. */
  public boolean hasMark()
  {
    return !marker.empty();
  }

  /**
   * Resets the string position to the previously marked position and removes the mark.
   * @return This stream.
   * @throws InvalidMarkException If no mark has been set yet.
   */
  public StringBufferStream reset()
  {
    if (marker.empty()) {
      throw new InvalidMarkException();
    }

    position = marker.pop().intValue();
    return this;
  }

  /**
   * Returns the character at the current position without updating the string pointer.
   * @return Character at current string position. Returns '\0' if no more characters are available.
   */
  public char peek()
  {
    return (position < length) ? data.charAt(start + position) : '\0';
  }

  /**
   * Returns the specified number of characters as string without updating the string pointer.
   * @param size Number of characters to return. Must be non-negative.
   * @return String with specified number of characters. Can be less if not enough characters
   *         are available.
   * @throws IllegalArgumentException if size is negative.
   */
  public String peek(int size)
  {
    if (size < 0) {
      throw new IllegalArgumentException();
    }

    size = Math.min(size, length - position);
    return data.substring(start + position, start + position + size);
  }

  /**
   * Tests if the substring of this string beginning at the current position starts with the
   * specified search string.
   * @param s The search string.
   * @return {@code true} if the string at the current position starts with the specified search string.
   * @return {@code true} if the search string matches the substring of this string beginning
   *         at the current position, {@code false} otherwise.
   * @throws NullPointerException if s is {@code null}.
   */
  public boolean peek(String s)
  {
    if (s == null) {
      throw new NullPointerException();
    }

    if (s.isEmpty()) {
      return true;
    } else if (position + s.length() <= length) {
      return data.startsWith(s, start + position);
    } else {
      return false;
    }
  }

  /**
   * Returns a string that matches specified regular expression, starting at the current string
   * position without updating it. The match must start at current string position to succeed.
   * @param regex A regular expression.
   * @return String containing matching characters starting at current string position.
   *         Returns {@code null} if no match is available.
   * @throws NullPointerException if regex is {@code null}.
   */
  public String peekMatch(String regex)
  {
    if (regex == null) {
      throw new NullPointerException();
    }

    if (regex.isEmpty()) {
      return "";
    } else {
      Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
      Matcher matcher = pattern.matcher(data);
      if (matcher.find(start + position) &&
          matcher.start() == start + position &&
          matcher.end() <= start + length) {
        return matcher.group();
      }
    }
    return null;
  }

  /**
   * Returns the character at the current position. String pointer will be updated.
   * <b>Note:</b> Automatically skips subsequent whitespace characters if AutoSkip is enabled.
   * @return Character at current string position. Returns '\0' if no more characters are available.
   */
  public char get()
  {
    char ch = peek();
    safeMovePosition(1);
    skipAutoWhitespace();
    return ch;
  }

  /**
   * Returns the specified number of characters as string. String pointer will be updated.
   * <b>Note:</b> Automatically skips subsequent whitespace characters if AutoSkip is enabled.
   * @param size Number of characters to return. Must be non-negative
   * @return String with specified number of characters. Can be less if not enough characters
   *         are available.
   */
  public String get(int size)
  {
    String s = peek(size);
    safeMovePosition(s.length());
    skipAutoWhitespace();
    return s;
  }

  /**
   * Returns a string that matches specified regular expression, starting at the current
   * string position. The match must start at current string position to succeed.
   * String pointer will be updated.
   * <b>Note:</b> Automatically skips subsequent whitespace characters if AutoSkip is enabled and
   *              a match has been found.
   * @param regex The regular expression.
   * @return String containing matching characters starting at current string position.
   *         Returns {@code null} if no match is available.
   */
  public String getMatch(String regex)
  {
    String s = peekMatch(regex);
    if (s != null) {
      safeMovePosition(s.length());
      skipAutoWhitespace();
    }
    return s;
  }

  /**
   * Returns the position of the first occurrence of the specified character starting at the
   * current string position.
   * @param ch A character.
   * @return the position of the first occurrence of the character in the underlying string that is
   *         greater than or equal to the current string position, or -1 if the character does not occur.
   */
  public int find(char ch)
  {
    int retVal = data.indexOf(ch, start + position);
    if (retVal >= start + length) {
      retVal = -1;
    }
    return retVal;
  }

  /**
   * Returns the position of the first occurrence of the specified substring starting at the
   * current string position.
   * @param s The substring to search for in a case-sensitive manner.
   * @return the position of the first occurrence of the specified substring, starting at the
   *         current string position, or -1 if there is no such occurrence.
   * @throws NullPointerException if s is {@code null}.
   */
  public int find(String s)
  {
    if (s == null) {
      throw new NullPointerException();
    }

    if (s.isEmpty()) {
      return position;
    } else {
      int retVal = data.indexOf(s, start + position);
      if (retVal + s.length() > start + length) {
        retVal = -1;
      }
      return retVal;
    }
  }

  /**
   * Returns the position of the first occurrence of the substring matching the specified
   * regular expression, starting at the current string position.
   * @param regex The regular expression to use.
   * @return the position of the first occurrence of the substring matching the specified
   *         regular expression, starting at the current string position.
   *         Returns -1 if no match found.
   * @throws NullPointerException if argument is {@code null}.
   * @throws PatternSyntaxException if specified argument is not a valid regular expression.
   */
  public int findMatch(String regex)
  {
    if (regex == null) {
      throw new NullPointerException();
    }

    if (regex.isEmpty()) {
      return position;
    } else {
      Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
      Matcher matcher = pattern.matcher(data);
      if (matcher.find(start + position) && matcher.end() <= start + length) {
        return matcher.start();
      }
    }
    return -1;
  }

  private StringBufferStream skipAutoWhitespace()
  {
    if (autoSkipWS) {
      return skipWhitespace();
    } else {
      return this;
    }
  }

  // Moves current pointer by specified amount. Stops at end of string. Returns whether pointer has been advanced.
  private boolean safeMovePosition(int offset)
  {
    if (offset > 0) {
      offset = Math.min(offset, length - position);
      position += offset;
      return true;
    }
    return false;
  }
}
