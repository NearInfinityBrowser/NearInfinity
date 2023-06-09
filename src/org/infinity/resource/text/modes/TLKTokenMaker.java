// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.text.modes;

import java.util.regex.Pattern;

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;
import org.infinity.resource.Profile;

/**
 * A token maker that turns text into a linked list of {@link Token}s for syntax highlighting Infinity Engine text
 * strings.
 */
public class TLKTokenMaker extends AbstractTokenMaker {
  /** Style for highlighting TLK text. */
  public static final String SYNTAX_STYLE_TLK = "text/TLK";

  // available token types
  public static final int TOKEN_TEXT    = Token.IDENTIFIER; // for regular text
  public static final int TOKEN_TAG     = Token.MARKUP_TAG_NAME; // for <xyz> tokens
  public static final int TOKEN_COLORED = Token.MARKUP_TAG_ATTRIBUTE; // for colored text (EE-specific)

  private static final Pattern REG_TOKEN = Pattern.compile("<[^<>]+>");
  private static final String CHAR_SEPARATOR = " \t-‒–—―";

  private final boolean isEE;

  private int currentTokenStart;
  private int currentTokenType;
  private int parentTokenType;

  public TLKTokenMaker() {
    super();
    isEE = Profile.isEnhancedEdition();
  }

  @Override
  public Token getTokenList(Segment text, int initialTokenType, int startOffset) {
    resetTokenList();

    char[] array = text.array;
    int ofs = text.offset;
    int cnt = text.count;
    int end = ofs + cnt;

    int newStartOfs = startOffset - ofs;
    currentTokenStart = ofs;
    currentTokenType = initialTokenType;
    parentTokenType = currentTokenType;

    for (int i = ofs; i < end; i++) {
      char c = array[i];

      switch (currentTokenType) {
        case Token.NULL: {
          currentTokenStart = i; // starting new token here

          if (c == '<' && REG_TOKEN.matcher(new String(array, i, end - i)).lookingAt()) {
            currentTokenType = TOKEN_TAG;
          } else if (isEE && c == '^' && i + 1 < end && array[i + 1] != '^') {
            currentTokenType = TOKEN_COLORED;
            parentTokenType = currentTokenType;
          } else {
            currentTokenType = TOKEN_TEXT;
            parentTokenType = currentTokenType;
            if (CHAR_SEPARATOR.indexOf(c) >= 0) {
              // ensure correct text wrapping at word boundaries
              addToken(array, currentTokenStart, i, currentTokenType, newStartOfs + currentTokenStart);
              currentTokenStart = i + 1;
            }
          }
          break;
        }

        case TOKEN_TEXT: {
          if (c == '<' && REG_TOKEN.matcher(new String(array, i, end - i)).lookingAt()) {
            addToken(array, currentTokenStart, i - 1, currentTokenType, newStartOfs + currentTokenStart);
            parentTokenType = currentTokenType;
            currentTokenStart = i;
            currentTokenType = TOKEN_TAG;
          } else if (isEE && c == '^') {
            addToken(array, currentTokenStart, i - 1, currentTokenType, newStartOfs + currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_COLORED;
          } else {
            if (CHAR_SEPARATOR.indexOf(c) >= 0) {
              // ensure correct text wrapping at word boundaries
              addToken(array, currentTokenStart, i, currentTokenType, newStartOfs + currentTokenStart);
              currentTokenStart = i + 1;
            }
          }
          break;
        }

        case TOKEN_TAG: {
          if (c == '>') {
            addToken(array, currentTokenStart, i, currentTokenType, newStartOfs + currentTokenStart);
            currentTokenStart = i + 1;
            currentTokenType = parentTokenType;
          }
          break;
        }

        case TOKEN_COLORED: {
          if (c == '<' && REG_TOKEN.matcher(new String(array, i, end - i)).lookingAt()) {
            addToken(array, currentTokenStart, i - 1, currentTokenType, newStartOfs + currentTokenStart);
            parentTokenType = currentTokenType;
            currentTokenStart = i;
            currentTokenType = TOKEN_TAG;
          } else if (c == '^' && i + 1 < end && array[i + 1] == '-') {
            i++;
            addToken(array, currentTokenStart, i, currentTokenType, newStartOfs + currentTokenStart);
            currentTokenType = Token.NULL;
          } else {
            if (CHAR_SEPARATOR.indexOf(c) >= 0) {
              // ensure correct text wrapping at word boundaries
              addToken(array, currentTokenStart, i, currentTokenType, newStartOfs + currentTokenStart);
              currentTokenStart = i + 1;
            }
          }
          break;
        }

        default: // should not happen
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

      // tokens that can span multiple lines
      case TOKEN_TEXT:
      case TOKEN_COLORED:
        addToken(array, currentTokenStart, end - 1, currentTokenType, newStartOfs + currentTokenStart);
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

}
