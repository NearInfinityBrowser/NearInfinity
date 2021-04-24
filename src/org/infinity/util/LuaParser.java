// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.text.PlainTextResource;

/**
 * Provides methods for converting Lua table structures into LuaEntry objects. Note: Not all available Lua features are
 * supported. Expects valid Lua code (limited code validation).
 */
public class LuaParser
{
  private enum Token {
    None, Key, Index, Value, ValueString, ValueNumber, ValueHexNumber, ValueBoolean, Comment,
  }

  /**
   * Attempts to retrieve a Lua table structure from the specified Lua resource.
   *
   * @param entry
   *          Lua resource entry.
   * @param name
   *          Name of the Lua table structure to retrieve.
   * @param exactMatch
   *          Whether the specified name is matched case-by-case ({@code true}) or as a regular expression
   *          ({@code false}).
   * @return All LuaEntry structures matching the specified structure name, listed inside a root LuaEntry container
   *         labeled as key="0".
   * @throws Exception
   */
  public static LuaEntry Parse(ResourceEntry entry, String name, boolean exactMatch) throws Exception
  {
    return Parse(Arrays.asList(entry), name, exactMatch);
  }

  /**
   * Attempts to retrieve a Lua table structure from the specified Lua resource.
   *
   * @param entries
   *          Array of Lua resource entries.
   * @param name
   *          Name of the Lua table structure to retrieve.
   * @param exactMatch
   *          Whether the specified name is matched case-by-case ({@code true}) or as a regular expression
   *          ({@code false}).
   * @return All LuaEntry structures matching the specified structure name, listed inside a root LuaEntry container
   *         labeled as key="0".
   * @throws Exception
   */
  public static LuaEntry Parse(List<ResourceEntry> entries, String name, boolean exactMatch) throws Exception
  {
    if (entries == null || entries.size() == 0)
      return null;

    StringBuilder sb = new StringBuilder();
    for (ResourceEntry entry : entries)
      sb.append((new PlainTextResource(entry)).getText());

    return Parse(sb.toString(), name, exactMatch);
  }

  /**
   * Attempts to retrieve a Lua table structure from the specified Lua string.
   *
   * @param data
   *          Lua code as string.
   * @param name
   *          Name of the Lua table structure to retrieve.
   * @param exactMatch
   *          Whether the specified name is matched literally (true) or as a regular expression (false).
   * @return All LuaEntry structures matching the specified structure name, listed inside a root LuaEntry container
   *         labeled as key="0".
   * @throws Exception
   */
  public static LuaEntry Parse(String data, String name, boolean exactMatch) throws Exception
  {
    if (data == null || name == null || name.isEmpty())
      return null;

    if (exactMatch)
      name = Pattern.quote(name);

    LuaEntry root = new LuaEntry(0);
    root.children = new ArrayList<>();

    // search for table definitions
    // Example: mytable = {"value1", "value2"}
    String pattern = String.format("^[ \t]*(%s)\\s*=", name);
    Pattern p = Pattern.compile(pattern, Pattern.MULTILINE);
    Matcher m = p.matcher(data);

    while (m.find()) {
      LuaEntry retVal = ParseElement((CharBuffer) CharBuffer.wrap(data).position(m.start(1)), root);
      if (retVal != null)
        root.children.add(retVal);
    }

    // search for table additions
    // Example: mytable[#mytable+1] = {"value1", "value2"}
    pattern = String.format("^[ \t]*(%1$s)[ \t]*\\[#%1$s[ \\t]*\\+[ \\t]*1\\]\\s*=", name);
    p = Pattern.compile(pattern, Pattern.MULTILINE);
    m = p.matcher(data);

    while (m.find()) {
      String exactName = m.group(1);
      LuaEntry child = root.findChild(exactName, false);
      if (child != null) {
        LuaEntry retVal = ParseElement((CharBuffer) CharBuffer.wrap(data).position(m.start(1)), child);
        if (retVal != null) {
          if (child.children == null)
            child.children = new ArrayList<>();
          retVal.key = Integer.toString(child.children.size());
          child.children.add(retVal);
        }
      }
    }

    return root;
  }

  // Recursive token-based parser
  private static LuaEntry ParseElement(CharBuffer buffer, LuaEntry parent) throws Exception
  {
    LuaEntry retVal = null;
    Token prevToken = Token.None;
    Token curToken = Token.None;
    char delimiter = (char) 0;
    StringBuilder key = new StringBuilder();
    StringBuilder value = new StringBuilder();
    while (buffer.position() < buffer.limit()) {
      char ch = buffer.get();
      switch (curToken) {
        case None:
          if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || ch == '_') {
            prevToken = curToken;
            curToken = Token.Key;
            key.append(ch);
          } else if (ch == '-' && peekBuffer(buffer) == '-') {
            buffer.get();
            prevToken = curToken;
            curToken = Token.Comment;
          } else if (ch == ',' || ch == '{') {
            if (retVal == null)
              retVal = (key.length() > 0) ? new LuaEntry(key.toString()) : new LuaEntry(parent);
            if (retVal.children == null)
              retVal.children = new ArrayList<>();
            LuaEntry el = ParseElement(buffer, retVal);
            if (el != null)
              retVal.children.add(el);
          } else if (ch == '}') {
            if (retVal == null)
              buffer.position(buffer.position() - 1);
            return retVal;
          } else if (ch > ' ') {
            prevToken = curToken;
            curToken = Token.Value;
            buffer.position(buffer.position() - 1);
            retVal = new LuaEntry(parent);
          }
          break;
        case Key:
          if (ch >= '0' && ch <= '9' || ch >= 'A' && ch <= 'Z' || ch >= 'a' && ch <= 'z' || ch == '_') {
            key.append(ch);
          } else if (ch == '[') {
            prevToken = curToken;
            curToken = Token.Index;
          } else if (ch == '=') {
            prevToken = curToken;
            curToken = Token.Value;
            retVal = new LuaEntry(key.toString());
          } else if (ch == ',' || ch == '}') {
            // special case: true/false cannot be correctly determined before fully parsed
            if (key.toString().equals("true") || key.toString().equals("false")) {
              value.append(key.toString());
              key.setLength(0);
              curToken = Token.ValueBoolean;
              buffer.position(buffer.position() - 1);
              if (retVal == null)
                retVal = new LuaEntry(parent);
            }
          } else if (ch == '-' && peekBuffer(buffer) == '-') {
            buffer.get();
            prevToken = curToken;
            curToken = Token.Comment;
          }
          break;
        case Index: // simplified index parsing (content is ignored)
          if (ch == ']')
            curToken = prevToken; // restore previous state
          break;
        case Value:
          if (ch == '\'' || ch == '"' || ch == '[') {
            // special case: double square brackets [[...]] as string delimiter
            if (ch == '[' && peekBuffer(buffer) == '[')
              ch = ']';
            delimiter = ch;
            curToken = Token.ValueString;
          } else if (ch >= '0' && ch <= '9') {
            if (ch == 0 && peekBuffer(buffer) == 'x') {
              curToken = Token.ValueHexNumber;
              buffer.get(); // skipping next character
            } else {
              curToken = Token.ValueNumber;
              value.append(ch);
            }
          } else if (ch == 't' && buffer.position() + 3 <= buffer.limit() && peekBuffer(buffer, 3).equals("rue")) {
            curToken = Token.ValueBoolean;
            value.append("true");
            buffer.position(buffer.position() + 3);
          } else if (ch == 'f' && buffer.position() + 4 <= buffer.limit() && peekBuffer(buffer, 4).equals("alse")) {
            curToken = Token.ValueBoolean;
            value.append("false");
            buffer.position(buffer.position() + 4);
          } else if (ch == '{') {
            if (retVal.children == null)
              retVal.children = new ArrayList<>();
            LuaEntry el = ParseElement(buffer, retVal);
            if (el != null)
              retVal.children.add(el);
            prevToken = curToken;
            curToken = Token.None;
          } else if (ch == '-') {
            char ch2 = peekBuffer(buffer);
            if (ch2 == '-') {
              buffer.get();
              prevToken = curToken;
              curToken = Token.Comment;
            } else if (ch2 >= '0' && ch2 <= '9') {
              value.append(ch);
            }
          }
          break;
        case ValueBoolean:
          if (ch == ',' || ch == '}') {
            retVal.value = Boolean.parseBoolean(value.toString());
            buffer.position(buffer.position() - 1);
            return retVal;
          } else if (ch == '-' && peekBuffer(buffer) == '-') {
            buffer.get();
            prevToken = curToken;
            curToken = Token.Comment;
          }
          break;
        case ValueNumber:
          if (ch >= '0' && ch <= '9') {
            value.append(ch);
          } else if (ch == ',' || ch == '}') {
            retVal.value = Integer.parseInt(value.toString());
            buffer.position(buffer.position() - 1);
            return retVal;
          } else if (ch == '-' && peekBuffer(buffer) == '-') {
            buffer.get();
            prevToken = curToken;
            curToken = Token.Comment;
          }
          break;
        case ValueHexNumber:
          if ((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F')) {
            value.append(ch);
          } else if (ch == ',' || ch == '}') {
            retVal.value = Integer.parseInt(value.toString(), 16);
            buffer.position(buffer.position() - 1);
            return retVal;
          } else if (ch == '-' && peekBuffer(buffer) == '-') {
            buffer.get();
            prevToken = curToken;
            curToken = Token.Comment;
          }
          break;
        case ValueString:
          if (ch == delimiter && (delimiter != ']' || peekBuffer(buffer) == ']')) {
            // special case: double square brackets [[...]] as string delimiter
            if (ch == ']')
              ch = buffer.get();
            retVal.value = value.toString();
            delimiter = (char) 0;
          } else if (delimiter == 0 && (ch == ',' || ch == '}')) {
            buffer.position(buffer.position() - 1);
            return retVal;
          } else if (delimiter == 0 && ch == '-' && peekBuffer(buffer) == '-') {
            buffer.get();
            prevToken = curToken;
            curToken = Token.Comment;
          } else {
            if (ch == '\\' && buffer.position() < buffer.limit()) {
              // escape sequence?
              ch = buffer.get();
              switch (ch) {
                case 'a':
                  ch = (char) 0x7;
                  break; // Bell
                case 'b':
                  ch = '\b';
                  break; // backspacce
                case 'f':
                  ch = '\f';
                  break; // form feed
                case 'n':
                  ch = '\n';
                  break; // line feed
                case 'r':
                  ch = '\r';
                  break; // carriage return
                case 't':
                  ch = '\t';
                  break; // horizontal tab
                case 'v':
                  ch = (char) 0xb;
                  break; // vertical tab
                case '\\':
                  ch = '\\';
                  break; // backslash
                case '"':
                  ch = '\"';
                  break; // double quotes
                case '\'':
                  ch = '\'';
                  break; // single quote
              }
              if (ch >= '0' && ch <= '7') {
                // octal value?
                try {
                  int code = Integer.parseInt(peekBuffer(buffer, 2) + ch, 8);
                  ch = (char) code;
                } catch (Exception e) {
                }
              } else if (ch == 'x') {
                // hexadecimal value?
                try {
                  int code = Integer.parseInt(peekBuffer(buffer, 2), 16);
                  ch = (char) code;
                } catch (Exception e) {
                }
              }
            }
            value.append(ch);
          }
          break;
        case Comment:
          if (ch == '\n')
            curToken = prevToken; // restore previous state
          break;
        default:
          throw new Exception(String.format("Invalid character '%c' at position %d", ch, buffer.position() - 1));
      }
    }

    return retVal;
  }

  // Preview next 'count' characters as string (truncated if necessary), empty string if not available.
  private static String peekBuffer(CharBuffer buf, int count)
  {
    if (buf == null)
      return "";
    if (count <= 0)
      return "";
    if (buf.position() + count > buf.limit())
      count = buf.limit() - buf.position();
    try {
      return buf.subSequence(0, count).toString();
    } catch (Exception e) {
    }
    return "";
  }

  // Preview next character, null character if not available.
  private static char peekBuffer(CharBuffer buf)
  {
    if (buf != null && buf.position() < buf.limit())
      return buf.get(buf.position());
    return '\0';
  }

  private LuaParser()
  {
  }
}
