// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.text.modes;

import java.util.Locale;
import java.util.TreeSet;

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.bcs.ScriptInfo;
import org.infinity.resource.bcs.Signatures;
import org.infinity.util.IdsMap;
import org.infinity.util.IdsMapCache;
import org.infinity.util.IdsMapEntry;

/**
 * A token maker that turns text into a linked list of {@code Token}s
 * for syntax highlighting Infinity Engine BCS scripts.
 */
public class BCSTokenMaker extends AbstractTokenMaker
{
  /** Style for highlighting BCS scripts. */
  public static final String SYNTAX_STYLE_BCS = "text/BCS";

  // available token types
  public static final int TOKEN_IDENTIFIER    = Token.IDENTIFIER;   // used for unrecognized literals
  public static final int TOKEN_KEYWORD       = Token.RESERVED_WORD;
  public static final int TOKEN_ACTION        = Token.FUNCTION;
  public static final int TOKEN_TRIGGER       = Token.DATA_TYPE;
  public static final int TOKEN_OBJECT        = Token.VARIABLE;
  public static final int TOKEN_NUMBER        = Token.LITERAL_NUMBER_DECIMAL_INT;
  public static final int TOKEN_HEXNUMBER     = Token.LITERAL_NUMBER_HEXADECIMAL;
  public static final int TOKEN_BINNUMBER     = Token.LITERAL_NUMBER_FLOAT;
  public static final int TOKEN_STRING        = Token.LITERAL_STRING_DOUBLE_QUOTE;
  public static final int TOKEN_COMMENT_LINE  = Token.COMMENT_EOL;
  public static final int TOKEN_COMMENT_BLOCK = Token.COMMENT_MULTILINE;
  public static final int TOKEN_SYMBOL        = Token.MARKUP_TAG_NAME;
  public static final int TOKEN_SYMBOL_SPELL  = Token.MARKUP_TAG_ATTRIBUTE;
  public static final int TOKEN_OPERATOR      = Token.OPERATOR;
  public static final int TOKEN_WHITESPACE    = Token.WHITESPACE;

  private static final String CharWhiteSpace    = " \t";
  private static final String CharOperator      = "!|,.()[]";
  private static final String CharHexPrefix     = "xX";
  private static final String CharBinPrefix     = "bB";
  private static final String CharStringDelim   = "\"~%";

  private int currentTokenStart;
  private int currentTokenType;
  private char stringDelimiter;

  public BCSTokenMaker()
  {
    super();
  }

  @Override
  public TokenMap getWordsToHighlight()
  {
    TokenMap tokenMap = new TokenMap();

    // symbolic names
    for (String ids : usedIds()) {
      int type = ("SPELL.IDS".equalsIgnoreCase(ids)) ? TOKEN_SYMBOL_SPELL : TOKEN_SYMBOL;
      fillFunctions(tokenMap, ids, type);
    }
    tokenMap.put("ANYONE", TOKEN_SYMBOL);

    // objects
    fillFunctions(tokenMap, "OBJECT.IDS", TOKEN_OBJECT);

    // actions
    fillFunctions(tokenMap, "ACTION.IDS", TOKEN_ACTION);

    // triggers
    fillFunctions(tokenMap, "TRIGGER.IDS", TOKEN_TRIGGER);

    // keywords
    tokenMap.put("IF", TOKEN_KEYWORD);
    tokenMap.put("THEN", TOKEN_KEYWORD);
    tokenMap.put("RESPONSE", TOKEN_KEYWORD);
    tokenMap.put("END", TOKEN_KEYWORD);

    return tokenMap;
  }

  @Override
  public void addToken(Segment segment, int start, int end, int tokenType, int startOffset)
  {
    if (tokenType == TOKEN_IDENTIFIER) {
      int value = wordsToHighlight.get(segment, start, end);
      if (value != -1) {
        tokenType = value;
      }
    }
    super.addToken(segment, start, end, tokenType, startOffset);
  }

  @Override
  public String[] getLineCommentStartAndEnd(int languageIndex)
  {
    return new String[]{"// ", null};
  }

  @Override
  public boolean getMarkOccurrencesOfTokenType(int type)
  {
    return type == TOKEN_ACTION || type == TOKEN_TRIGGER ||
           type == TOKEN_OBJECT || type == TOKEN_SYMBOL || type == TOKEN_SYMBOL_SPELL;
  }

  @Override
  public boolean getShouldIndentNextLineAfter(Token token)
  {
//    if (token != null) {
//      if (token.getType() == TOKEN_KEYWORD) {
//        String s = String.valueOf(token.getTextArray(), token.getTextOffset(),
//                                  token.getEndOffset() - token.getTextOffset());
//        if ("IF".equals(s) || "THEN".equals(s) || s.startsWith("#")) {
//          return true;
//        }
//      }
//    }
    return false;
  }

  /**
   * Returns a list of tokens representing the given text.
   *
   * @param text The text to break into tokens.
   * @param initialTokenType The token with which to start tokenizing.
   * @param startOffset The offset at which the line of tokens begins.
   * @return A linked list of tokens representing {@code text}.
   */
  @Override
  public Token getTokenList(Segment text, int initialTokenType, int startOffset)
  {
    resetTokenList();

    char[] array = text.array;
    int ofs = text.offset;
    int cnt = text.count;
    int end = text.offset + cnt;

    int newStartOfs = startOffset - ofs;
    currentTokenStart = ofs;
    currentTokenType = initialTokenType;
    stringDelimiter = 0;

    boolean tokenCheckComment = false;  // indicates whether character is part of the comment prefix/suffix

    for (int i = ofs; i < end; i++) {
      char c = array[i];

      switch (currentTokenType) {
        case Token.NULL:
        {
          currentTokenStart = i;    // starting new token here

          if (c == '#') {
            // keyword: action block probability
            if (i+1 < end && RSyntaxUtilities.isDigit(array[i+1])) {
              currentTokenType = TOKEN_KEYWORD;
            } else {
              currentTokenType = TOKEN_IDENTIFIER;
            }
          } else if (c == '/') {
            // comment
            currentTokenType = TOKEN_IDENTIFIER;
            if (i+1 < end) {
              if (array[i+1] == '/') {
                tokenCheckComment = true;
                currentTokenType = TOKEN_COMMENT_LINE;
              } else if (array[i+1] == '*') {
                tokenCheckComment = true;
                currentTokenType = TOKEN_COMMENT_BLOCK;
              }
            }
          } else if (CharStringDelim.indexOf(c) > -1) {
            // string
            stringDelimiter = c;
            currentTokenType = TOKEN_STRING;
          } else if (CharWhiteSpace.indexOf(c) > -1) {
            // whitespace
            currentTokenType = TOKEN_WHITESPACE;
          } else if (CharOperator.indexOf(c) > -1) {
            // operator
            currentTokenType = TOKEN_OPERATOR;
          } else {
            if (c == '-') {
              if (i+1 < end && RSyntaxUtilities.isDigit(array[i+1])) {
                // negative number
                currentTokenType = TOKEN_NUMBER;
                if (array[i+1] == '0' && i+2 < end && CharHexPrefix.indexOf(array[i+2]) > -1) {
                  // hex number
                  currentTokenType = TOKEN_HEXNUMBER;
                } else if (array[i+1] == '0' && i+2 < end && CharBinPrefix.indexOf(array[i+2]) > -1) {
                  // bin number
                  currentTokenType = TOKEN_BINNUMBER;
                }
              }
            } else if (RSyntaxUtilities.isDigit(c)) {
              currentTokenType = TOKEN_NUMBER;
              if (c == '0' && i+1 < end && CharHexPrefix.indexOf(array[i+1]) > -1) {
                // hex number
                currentTokenType = TOKEN_HEXNUMBER;
              } else if (c == '0' && i+1 < end && CharBinPrefix.indexOf(array[i+1]) > -1) {
                // bin number
                currentTokenType = TOKEN_BINNUMBER;
              }
            } else {
              // a potential identifier
              currentTokenType = TOKEN_IDENTIFIER;
            }
          }
        }   // end of case Token.NULL:
        break;

        case TOKEN_KEYWORD:
        {
          if (RSyntaxUtilities.isDigit(c)) {
            // still keyword
          } else if (c == '/') {
            // comment
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_IDENTIFIER;
            if (i+1 < end) {
              if (array[i+1] == '/') {
                tokenCheckComment = true;
                currentTokenType = TOKEN_COMMENT_LINE;
              } else if (array[i+1] == '*') {
                tokenCheckComment = true;
                currentTokenType = TOKEN_COMMENT_BLOCK;
              }
            }
          } else if (CharStringDelim.indexOf(c) > -1) {
            // string
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            stringDelimiter = c;
            currentTokenStart = i;
            currentTokenType = TOKEN_STRING;
          } else if (CharWhiteSpace.indexOf(c) > -1) {
            // whitespace
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_WHITESPACE;
          } else if (CharOperator.indexOf(c) > -1) {
            // operator
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_OPERATOR;
          } else {
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenStart = i;
            if (c == '-') {
              if (i+1 < end && RSyntaxUtilities.isDigit(array[i+1])) {
                // negative number
                currentTokenType = TOKEN_NUMBER;
                if (array[i+1] == '0' && i+2 < end && CharHexPrefix.indexOf(array[i+2]) > -1) {
                  // hex number
                  currentTokenType = TOKEN_HEXNUMBER;
                } else if (array[i+1] == '0' && i+2 < end && CharBinPrefix.indexOf(array[i+2]) > -1) {
                  // bin number
                  currentTokenType = TOKEN_BINNUMBER;
                }
              }
            } else {
              // a potential identifier
              currentTokenType = TOKEN_IDENTIFIER;
            }
          }
        }   // end of case TOKEN_KEYWORD:
        break;

        case TOKEN_OPERATOR:
        {
          if (c == '#') {
            // keyword: start of action block probability
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenStart = i;
            if (i+1 < end && RSyntaxUtilities.isDigit(array[i+1])) {
              currentTokenType = TOKEN_KEYWORD;
            } else {
              currentTokenType = TOKEN_IDENTIFIER;
            }
          } else if (c == '/') {
            // comment
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_IDENTIFIER;
            if (i+1 < end) {
              if (array[i+1] == '/') {
                tokenCheckComment = true;
                currentTokenType = TOKEN_COMMENT_LINE;
              } else if (array[i+1] == '*') {
                tokenCheckComment = true;
                currentTokenType = TOKEN_COMMENT_BLOCK;
              }
            }
          } else if (CharStringDelim.indexOf(c) > -1) {
            // string
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            stringDelimiter = c;
            currentTokenStart = i;
            currentTokenType = TOKEN_STRING;
          } else if (CharWhiteSpace.indexOf(c) > -1) {
            // whitespace
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_WHITESPACE;
          } else if (CharOperator.indexOf(c) > -1) {
            // still operator
          } else {
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenStart = i;
            if (c == '-') {
              if (i+1 < end && RSyntaxUtilities.isDigit(array[i+1])) {
                // negative number
                currentTokenType = TOKEN_NUMBER;
                if (array[i+1] == '0' && i+2 < end && CharHexPrefix.indexOf(array[i+2]) > -1) {
                  // hex number
                  currentTokenType = TOKEN_HEXNUMBER;
                } else if (array[i+1] == '0' && i+2 < end && CharBinPrefix.indexOf(array[i+2]) > -1) {
                  // bin number
                  currentTokenType = TOKEN_BINNUMBER;
                }
              }
            } else if (RSyntaxUtilities.isDigit(c)) {
              currentTokenType = TOKEN_NUMBER;
              if (c == '0' && i+1 < end && CharHexPrefix.indexOf(array[i+1]) > -1) {
                // hex number
                currentTokenType = TOKEN_HEXNUMBER;
              } else if (c == '0' && i+1 < end && CharBinPrefix.indexOf(array[i+1]) > -1) {
                // hex number
                currentTokenType = TOKEN_BINNUMBER;
              }
            } else {
              // a potential identifier
              currentTokenType = TOKEN_IDENTIFIER;
            }
          }
        }   // end of case TOKEN_OPERATOR:
        break;

        case TOKEN_WHITESPACE:
        {
          if (c == '#') {
            // keyword: start of action block probability
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenStart = i;
            if (i+1 < end && RSyntaxUtilities.isDigit(array[i+1])) {
              currentTokenType = TOKEN_KEYWORD;
            } else {
              currentTokenType = TOKEN_IDENTIFIER;
            }
          } else if (c == '/') {
            // comment
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_IDENTIFIER;
            if (i+1 < end) {
              if (array[i+1] == '/') {
                tokenCheckComment = true;
                currentTokenType = TOKEN_COMMENT_LINE;
              } else if (array[i+1] == '*') {
                tokenCheckComment = true;
                currentTokenType = TOKEN_COMMENT_BLOCK;
              }
            }
          } else if (CharStringDelim.indexOf(c) > -1) {
            // string
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            stringDelimiter = c;
            currentTokenStart = i;
            currentTokenType = TOKEN_STRING;
          } else if (CharWhiteSpace.indexOf(c) > -1) {
            // still whitespace
          } else if (CharOperator.indexOf(c) > -1) {
            // operator
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_OPERATOR;
          } else {
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenStart = i;
            if (c == '-') {
              if (i+1 < end && RSyntaxUtilities.isDigit(array[i+1])) {
                // negative number
                currentTokenType = TOKEN_NUMBER;
                if (array[i+1] == '0' && i+2 < end && CharHexPrefix.indexOf(array[i+2]) > -1) {
                  // hex number
                  currentTokenType = TOKEN_HEXNUMBER;
                } else if (array[i+1] == '0' && i+2 < end && CharBinPrefix.indexOf(array[i+2]) > -1) {
                  // bin number
                  currentTokenType = TOKEN_BINNUMBER;
                }
              }
            } else if (RSyntaxUtilities.isDigit(c)) {
              currentTokenType = TOKEN_NUMBER;
              if (c == '0' && i+1 < end && CharHexPrefix.indexOf(array[i+1]) > -1) {
                // hex number
                currentTokenType = TOKEN_HEXNUMBER;
              } else if (c == '0' && i+1 < end && CharBinPrefix.indexOf(array[i+1]) > -1) {
                // bin number
                currentTokenType = TOKEN_BINNUMBER;
              }
            } else {
              // a potential identifier
              currentTokenType = TOKEN_IDENTIFIER;
            }
          }
        }   // end of case TOKEN_WHITESPACE:
        break;

        case TOKEN_COMMENT_LINE:
          // still line comment
          break;

        case TOKEN_COMMENT_BLOCK:
        {
          if (tokenCheckComment) {
            if (c == '/') {
              addToken(text, currentTokenStart, i, currentTokenType, newStartOfs+currentTokenStart);
              currentTokenType = Token.NULL;
            }
            tokenCheckComment = false;
          } else if (c == '*') {
            if (i+1 < end && array[i+1] == '/') {
              tokenCheckComment = true;
            }
          }
        }   // end of case TOKEN_COMMENT_BLOCK:
        break;

        case TOKEN_STRING:
        {
          if (c == stringDelimiter) {
            addToken(text, currentTokenStart, i, currentTokenType, newStartOfs+currentTokenStart);
            stringDelimiter = 0;
            currentTokenType = Token.NULL;
          }
        }   // end of case TOKEN_STRING:
        break;

        case TOKEN_BINNUMBER:
        case TOKEN_HEXNUMBER:
        case TOKEN_NUMBER:
        {
          if (c == '#') {
            // keyword: start of action block probability
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenStart = i;
            if (i+1 < end && RSyntaxUtilities.isDigit(array[i+1])) {
              currentTokenType = TOKEN_KEYWORD;
            } else {
              currentTokenType = TOKEN_IDENTIFIER;
            }
          } else if (c == '/') {
            // comment
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_IDENTIFIER;
            if (i+1 < end) {
              if (array[i+1] == '/') {
                tokenCheckComment = true;
                currentTokenType = TOKEN_COMMENT_LINE;
              } else if (array[i+1] == '*') {
                tokenCheckComment = true;
                currentTokenType = TOKEN_COMMENT_BLOCK;
              }
            }
          } else if (CharStringDelim.indexOf(c) > -1) {
            // string
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            stringDelimiter = c;
            currentTokenStart = i;
            currentTokenType = TOKEN_STRING;
          } else if (CharWhiteSpace.indexOf(c) > -1) {
            // whitespace
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_WHITESPACE;
          } else if (CharOperator.indexOf(c) > -1) {
            // operator
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_OPERATOR;
          } else if (currentTokenType == TOKEN_HEXNUMBER && (RSyntaxUtilities.isHexCharacter(c) || CharHexPrefix.indexOf(c) > -1)) {
            // still a hex number?
            if (CharHexPrefix.indexOf(c) > -1 && (i == currentTokenStart || array[i-1] != '0')) {
              currentTokenType = TOKEN_IDENTIFIER;
            }
          } else if (currentTokenType == TOKEN_BINNUMBER && (("01".indexOf(c) > -1) || CharBinPrefix.indexOf(c) > -1)) {
            // still a bin number?
            if (CharBinPrefix.indexOf(c) > -1 && (i == currentTokenStart || array[i-1] != '0')) {
              currentTokenType = TOKEN_IDENTIFIER;
            }
          } else if (currentTokenType == TOKEN_NUMBER && RSyntaxUtilities.isDigit(c)) {
            // still a number
          } else {
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenStart = i;
            // a potential identifier
            currentTokenType = TOKEN_IDENTIFIER;
          }
        }   // end of case TOKEN_NUMBER:
        break;

        case TOKEN_IDENTIFIER:
        {
          if (c == '/') {
            // comment
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_IDENTIFIER;
            if (i+1 < end) {
              if (array[i+1] == '/') {
                tokenCheckComment = true;
                currentTokenType = TOKEN_COMMENT_LINE;
              } else if (array[i+1] == '*') {
                tokenCheckComment = true;
                currentTokenType = TOKEN_COMMENT_BLOCK;
              }
            }
          } else if (CharStringDelim.indexOf(c) > -1) {
            // string
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            stringDelimiter = c;
            currentTokenStart = i;
            currentTokenType = TOKEN_STRING;
          } else if (CharWhiteSpace.indexOf(c) > -1) {
            // whitespace
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenStart = i;
            currentTokenType = Token.WHITESPACE;
          } else if (CharOperator.indexOf(c) > -1) {
            // operator
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenStart = i;
            currentTokenType = TOKEN_OPERATOR;
          } else {
            // still identifier
          }
        }   // end of case TOKEN_IDENTIFIER:
        break;

        default:    // should never happen
        {
          try {
            throw new Exception(String.format("Invalid token %d found at position %d",
                                              currentTokenType, newStartOfs+i));
          } catch (Exception e) {
            e.printStackTrace();
          }
        }   // end of default:
      }   // end of switch (currentTokenType)
    }   // end of for (int i = ofs; i < end; i++)

    // adding the current token to the list
    switch (currentTokenType) {
      case Token.NULL:    // do nothing if no token is active
        addNullToken();
        break;

      case TOKEN_COMMENT_BLOCK:   // block comments can span multiple lines
        addToken(text, currentTokenStart, end-1, currentTokenType, newStartOfs+currentTokenStart);
        break;

      default:    // everything else doesn't continue to the next line
        addToken(text, currentTokenStart, end-1, currentTokenType, newStartOfs+currentTokenStart);
        addNullToken();
    }

    return firstToken;
  }

  /**
   * Fills map of tokens from the IDS resource.
   *
   * @param tokenMap Object to which tokens are added
   * @param resource Name of the IDS resourse that used for extract tokens
   * @param tokenType Type of all tokens that method puts to the {@code tokenMap}
   */
  private void fillFunctions(TokenMap tokenMap, String resource, int tokenType)
  {
    final IdsMap map = IdsMapCache.get(resource);
    if (map == null) return;

    for (final IdsMapEntry e : map.getAllValues()) {
      for (String fullName : e) {
        String name = extractFunctionName(fullName);
        if (name != null && !name.isEmpty()) {
          tokenMap.put(name, tokenType);
        }
      }
    }
  }
  /** Extracts the function name of the action/trigger definition. */
  private String extractFunctionName(String function)
  {
    if (function != null && !function.isEmpty()) {
      int idx = function.indexOf('(');
      if (idx < 0) {
        idx = function.length();
      }
      if (idx > 0) {
        return function.substring(0, idx).trim();
      }
    }
    return null;
  }

  /** Scans action and trigger definitions for referenced IDS files and returns them as a sorted set. */
  private TreeSet<String> usedIds()
  {
    // adding IDS files referenced in function signatures
    Signatures actions = Signatures.getActions();
    Signatures triggers = Signatures.getTriggers();
    Signatures[] signatures = new Signatures[]{actions, triggers};
    TreeSet<String> idsSet = new TreeSet<>();
    for (final Signatures sig: signatures) {
      if (sig != null) {
        for (final Integer code: sig.getFunctionIds()) {
          Signatures.Function[] functions = sig.getFunction(code.intValue());
          for (final Signatures.Function f: functions) {
            for (int idx = 0, cnt = f.getNumParameters(); idx < cnt; idx++) {
              String ids = f.getParameter(idx).getIdsRef();
              if (!ids.isEmpty()) {
                ids = ids.toUpperCase(Locale.ENGLISH) + ".IDS";
                if (ResourceFactory.resourceExists(ids)) {
                  idsSet.add(ids);
                }
              }
            }
          }
        }
      }
    }

    // adding IDS targets
    String[] idsTargets = ScriptInfo.getInfo().getObjectIdsList();
    for (String ids: idsTargets) {
      ids = ids.toUpperCase(Locale.ENGLISH) + ".IDS";
      if (ResourceFactory.resourceExists(ids)) {
        idsSet.add(ids);
      }
    }

    return idsSet;
  }
}
