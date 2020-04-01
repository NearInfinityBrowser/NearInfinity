// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.text.modes;

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;

/**
 * A token maker that turns text into a linked list of {@link Token}s
 * for syntax highlighting WeiDU.log content.
 */
public class WeiDULogTokenMaker extends AbstractTokenMaker
{
  /** Style for highlighting TLK text. */
  public static final String SYNTAX_STYLE_WEIDU = "text/WeiDU";

  // available token types
  public static final int TOKEN_STRING      = Token.LITERAL_STRING_DOUBLE_QUOTE;
  public static final int TOKEN_NUMBER      = Token.LITERAL_NUMBER_DECIMAL_INT;
  public static final int TOKEN_COMMENT     = Token.COMMENT_EOL;
  public static final int TOKEN_WHITESPACE  = Token.WHITESPACE;

  private static final String CharDigit       = "-0123456789";


  public WeiDULogTokenMaker()
  {
    super();
  }

  @Override
  public Token getTokenList(Segment text, int initialTokenType, int startOffset)
  {
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

    for (int i = ofs; i < end; i++) {
      char c = array[i];
      switch (currentTokenType) {
        case Token.NULL:
        {
          currentTokenStart = i;    // starting new token here

          if (c == '~') {
            currentTokenType = TOKEN_STRING;
          } else if (c == '#') {
            currentTokenType = TOKEN_NUMBER;
          } else if (c == '/' && i+1 < end && array[i+1] == '/') {
            currentTokenType = TOKEN_COMMENT;
          } else {
            currentTokenType = TOKEN_WHITESPACE;
          }
          break;
        }
        case TOKEN_STRING:
        {
          if (c == '~') {
            addToken(text, currentTokenStart, i, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenType = Token.NULL;
          }
          break;
        }
        case TOKEN_WHITESPACE:
        {
          if (c == '~') {
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_STRING;
          } else if (c == '#') {
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_NUMBER;
          } else if (c == '/' && i+1 < end && array[i+1] == '/') {
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_COMMENT;
          }
          break;
        }
        case TOKEN_NUMBER:
        {
          if (CharDigit.indexOf(c) >= 0) {
            // still number
          } else if (c == '/' && i+1 < end && array[i+1] == '/') {
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_COMMENT;
          } else {
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_WHITESPACE;
          }
          break;
        }
        case TOKEN_COMMENT:
        {
          // still line comment
          break;
        }
        default:  // should not happen
          try {
            throw new Exception("Invalid token " + currentTokenType + " found at position " + (newStartOfs + i));
          } catch (Exception e) {
            e.printStackTrace();
          }
      }
    }

    // adding the current token to the list
    switch (currentTokenType) {
      case Token.NULL:
        addNullToken();
        break;
      default:
        addToken(array, currentTokenStart, end - 1, currentTokenType, newStartOfs + currentTokenStart);
        addNullToken();
    }

    return firstToken;
  }

  @Override
  public TokenMap getWordsToHighlight()
  {
    // no keywords to check
    return new TokenMap();
  }

}
