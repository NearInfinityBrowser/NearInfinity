// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.text.modes;

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.infinity.util.Logger;

/**
 * A token maker that turns text into a linked list of {@code Token}s for syntax highlighting Infinity Engine INI
 * resources.
 */
public class INITokenMaker extends AbstractTokenMaker {
  /** Style for highlighting INI text. */
  public static final String SYNTAX_STYLE_INI = "text/INI";

  // available token types
  public static final int TOKEN_SECTION       = TokenTypes.PREPROCESSOR;
  public static final int TOKEN_KEY           = TokenTypes.RESERVED_WORD;
  public static final int TOKEN_EQUALS        = TokenTypes.OPERATOR;
  public static final int TOKEN_VALUE         = TokenTypes.LITERAL_STRING_DOUBLE_QUOTE;
  public static final int TOKEN_COMMENT       = TokenTypes.COMMENT_EOL;
  public static final int TOKEN_WHITESPACE    = TokenTypes.WHITESPACE;

  private static final String WHITESPACE = " \t";
  private static final String NOT_IDENTIFIER = " \t\r\n#;/[]=";

  private enum Type {
    /** Default value. */
    None,
    /** Previous token was TOKEN_SECTION. */
    Section,
    /** Previous token was TOKEN_KEY. */
    Key,
    /** Previous token was TOKEN_EQUALS. */
    Equals,
    /** Previous token was TOKEN_VALUE. */
    Value,
  }

  public INITokenMaker() {
    super();
  }

  @Override
  public String[] getLineCommentStartAndEnd(int languageIndex) {
    return new String[] { "// ", null };
  }

  @Override
  public Token getTokenList(Segment text, int initialTokenType, int startOffset) {
    int currentTokenStart;
    int currentTokenType;

    resetTokenList();

    char[] array = text.array;
    int ofs = text.offset;
    int cnt = text.count;
    int end = ofs + cnt;

    int newStartOfs = startOffset - ofs;
    currentTokenStart = ofs;
    currentTokenType = initialTokenType;

    Type curType = Type.None;

    for (int i = ofs; i < end; i++) {
      char c = array[i];
      switch (currentTokenType) {
        case TokenTypes.NULL: {
          currentTokenStart = i; // starting new token here

          if (isComment(c, i, end, array)) {
            currentTokenType = TOKEN_COMMENT;
          } else if (c == '[') {
            currentTokenType = TOKEN_SECTION;
          } else if (isIdentifier(c)) {
            currentTokenType = TOKEN_KEY;
          } else {
            currentTokenType = TOKEN_WHITESPACE;
          }
          curType = Type.None;
          break;
        }
        case TOKEN_SECTION: {
          if (isComment(c, i, end, array)) {
            addToken(text, currentTokenStart, i - 1, currentTokenType, newStartOfs + currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_COMMENT;
          } else if (curType == Type.Equals) {
            // fixing current toke type
            currentTokenType = TOKEN_VALUE;
          } else if (c == ']') {
            addToken(text, currentTokenStart, i, currentTokenType, newStartOfs + currentTokenStart);
            currentTokenType = TokenTypes.NULL;
          }
          break;
        }
        case TOKEN_KEY: {
          if (isComment(c, i, end, array)) {
            addToken(text, currentTokenStart, i - 1, currentTokenType, newStartOfs + currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_COMMENT;
          } else if (c == '=') {
            addToken(text, currentTokenStart, i - 1, currentTokenType, newStartOfs + currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_EQUALS;
          } else if (isWhiteSpace(c)) {
            addToken(text, currentTokenStart, i - 1, currentTokenType, newStartOfs + currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_WHITESPACE;
            curType = Type.Key;
          }
          break;
        }
        case TOKEN_VALUE: {
          if (isComment(c, i, end, array)) {
            addToken(text, currentTokenStart, i - 1, currentTokenType, newStartOfs + currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_COMMENT;
          }
          break;
        }
        case TOKEN_EQUALS: {
          if (isComment(c, i, end, array)) {
            addToken(text, currentTokenStart, i - 1, currentTokenType, newStartOfs + currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_COMMENT;
          } else if (isWhiteSpace(c)) {
            addToken(text, currentTokenStart, i - 1, currentTokenType, newStartOfs + currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_WHITESPACE;
            curType = Type.Equals;
          } else {
            addToken(text, currentTokenStart, i - 1, currentTokenType, newStartOfs + currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_VALUE;
          }
          break;
        }
        case TOKEN_WHITESPACE: {
          if (isComment(c, i, end, array)) {
            addToken(text, currentTokenStart, i - 1, currentTokenType, newStartOfs + currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_COMMENT;
          } else if (curType == Type.Key && c == '=') {
            addToken(text, currentTokenStart, i - 1, currentTokenType, newStartOfs + currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_EQUALS;
          } else if (curType == Type.Key && isIdentifier(c)) {
            addToken(text, currentTokenStart, i - 1, currentTokenType, newStartOfs + currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_KEY;
          } else if (curType == Type.Equals && !isWhiteSpace(c)) {
            addToken(text, currentTokenStart, i - 1, currentTokenType, newStartOfs + currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_VALUE;
          } else if (!isWhiteSpace(c)) {
            addToken(text, currentTokenStart, i - 1, currentTokenType, newStartOfs + currentTokenStart);
            i--;
            currentTokenType = TokenTypes.NULL;
          }
          break;
        }
        case TOKEN_COMMENT: {
          // still line comment
          break;
        }
        default:  // should not happen
          try {
            throw new Exception("Invalid token " + currentTokenType + " found at position " + (newStartOfs + i));
          } catch (Exception e) {
            Logger.error(e);
          }
      }
    }

    // adding the current token to the list
    switch (currentTokenType) {
      case TokenTypes.NULL:
        addNullToken();
        break;
      default:
        addToken(array, currentTokenStart, end - 1, currentTokenType, newStartOfs + currentTokenStart);
        addNullToken();
    }

    return firstToken;
  }

  @Override
  public TokenMap getWordsToHighlight() {
    // no keywords to check
    return new TokenMap();
  }

  /** Returns {@code true} for all supported single line comment prefixes. */
  private boolean isComment(char c, int index, int lineEnd, char[] array) {
    return (c == ';' || c == '#' || (c == '/' && index + 1 < lineEnd && array[index + 1] == '/'));
  }

  /** Returns {@code true} if the specified character is a whitespace character. */
  private boolean isWhiteSpace(char c) {
    return WHITESPACE.indexOf(c) >= 0;
  }

  /** Returns {@code true} if the specified character is a valid identifier character. */
  private boolean isIdentifier(char c) {
    return NOT_IDENTIFIER.indexOf(c) < 0;
  }

}
