// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.text.modes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.util.IdsMap;
import org.infinity.util.IdsMapCache;
import org.infinity.util.IdsMapEntry;

/**
 * A token maker that turns text into a linked list of {@code Token}s
 * for syntax highlighting Infinity Engine BCS scripts.
 * @author argent77
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

  private int currentTokenStart;
  private int currentTokenType;

  public BCSTokenMaker()
  {
    super();
  }

  @Override
  public TokenMap getWordsToHighlight()
  {
    TokenMap tokenMap = new TokenMap();
    IdsMap map;

    // symbolic names
    List<String> idsFile = new ArrayList<String>();
    if (Profile.getEngine() == Profile.Engine.BG1) {
      idsFile.add("ALIGN.IDS");     idsFile.add("ALIGNMEN.IDS");  idsFile.add("ANIMATE.IDS");
      idsFile.add("ASTYLES.IDS");   idsFile.add("BOOLEAN.IDS");   idsFile.add("CLASS.IDS");
      idsFile.add("DAMAGES.IDS");   idsFile.add("EA.IDS");        idsFile.add("GENDER.IDS");
      idsFile.add("GENERAL.IDS");   idsFile.add("GTIMES.IDS");    idsFile.add("HAPPY.IDS");
      idsFile.add("HOTKEY.IDS");    idsFile.add("MFLAGS.IDS");    idsFile.add("RACE.IDS");
      idsFile.add("REACTION.IDS");  idsFile.add("SCRLEV.IDS");    idsFile.add("SCROLL.IDS");
      idsFile.add("SLOTS.IDS");     idsFile.add("SOUNDOFF.IDS");  idsFile.add("SPECIFIC.IDS");
      idsFile.add("SPELL.IDS");     idsFile.add("STATE.IDS");     idsFile.add("STATS.IDS");
      idsFile.add("TIME.IDS");      idsFile.add("TIMEODAY.IDS");  idsFile.add("WEATHER.IDS");
    } else if (Profile.getEngine() == Profile.Engine.IWD) {
      idsFile.add("ALIGN.IDS");     idsFile.add("ALIGNMEN.IDS");  idsFile.add("ANIMATE.IDS");
      idsFile.add("ASTYLES.IDS");   idsFile.add("BITMODE.IDS");   idsFile.add("BOOLEAN.IDS");
      idsFile.add("CLASS.IDS");     idsFile.add("DAMAGES.IDS");   idsFile.add("DIR.IDS");
      idsFile.add("EA.IDS");        idsFile.add("EXTSTATE.IDS");  idsFile.add("GENDER.IDS");
      idsFile.add("GENERAL.IDS");   idsFile.add("GTIMES.IDS");    idsFile.add("HAPPY.IDS");
      idsFile.add("HELPTYPE.IDS");  idsFile.add("HOTKEY.IDS");    idsFile.add("MFLAGS.IDS");
      idsFile.add("MUSICS.IDS");    idsFile.add("RACE.IDS");      idsFile.add("REACTION.IDS");
      idsFile.add("SCHOOL.IDS");    idsFile.add("SCRLEV.IDS");    idsFile.add("SCROLL.IDS");
      idsFile.add("SEQUENCE.IDS");  idsFile.add("SLOTS.IDS");     idsFile.add("SOUNDOFF.IDS");
      idsFile.add("SPECIFIC.IDS");  idsFile.add("SPELL.IDS");     idsFile.add("SPLSTATE.IDS");
      idsFile.add("STATE.IDS");     idsFile.add("STATMOD.IDS");   idsFile.add("STATS.IDS");
      idsFile.add("TIME.IDS");      idsFile.add("TIMEODAY.IDS");  idsFile.add("WEATHER.IDS");
    } else if (Profile.getEngine() == Profile.Engine.IWD2) {
      idsFile.add("ALIGNMNT.IDS");  idsFile.add("ANIMATE.IDS");   idsFile.add("AREADIFF.IDS");
      idsFile.add("AREAFLAG.IDS");  idsFile.add("ATTSTYL.IDS");   idsFile.add("BARDSONG.IDS");
      idsFile.add("BITMODE.IDS");   idsFile.add("BOOLEAN.IDS");   idsFile.add("CLASS.IDS");
      idsFile.add("CLASSMSK.IDS");  idsFile.add("CREAREFL.IDS");  idsFile.add("DAMAGES.IDS");
      idsFile.add("DIFFLEVL.IDS");  idsFile.add("DIFFMODE.IDS");  idsFile.add("DIR.IDS");
      idsFile.add("DOORFLAG.IDS");  idsFile.add("EA.IDS");        idsFile.add("FEATS.IDS");
      idsFile.add("GENDER.IDS");    idsFile.add("GENERAL.IDS");   idsFile.add("GTIMES.IDS");
      idsFile.add("HAPPY.IDS");     idsFile.add("HELPTYPE.IDS");  idsFile.add("HOTKEY.IDS");
      idsFile.add("HPFLAGS.IDS");   idsFile.add("KIT.IDS");       idsFile.add("MFLAGS.IDS");
      idsFile.add("MODAL.IDS");     idsFile.add("MUSIC.IDS");     idsFile.add("MUSICS.IDS");
      idsFile.add("RACE.IDS");      idsFile.add("REACTION.IDS");  idsFile.add("SCEFFECT.IDS");
      idsFile.add("SCHOOL.IDS");    idsFile.add("SCRLEV.IDS");    idsFile.add("SCROLL.IDS");
      idsFile.add("SEQUENCE.IDS");  idsFile.add("SHEFFECT.IDS");  idsFile.add("SKILLS.IDS");
      idsFile.add("SLOTS.IDS");     idsFile.add("SOUNDOFF.IDS");  idsFile.add("SPECIFIC.IDS");
      idsFile.add("SPELL.IDS");     idsFile.add("SPLCAST.IDS");   idsFile.add("SPLSTATE.IDS");
      idsFile.add("STATE.IDS");     idsFile.add("STATMOD.IDS");   idsFile.add("STATS.IDS");
      idsFile.add("SUBRACE.IDS");   idsFile.add("TEAMBIT.IDS");   idsFile.add("TIME.IDS");
      idsFile.add("TIMEODAY.IDS");  idsFile.add("WEATHER.IDS");
    } else if (Profile.getEngine() == Profile.Engine.PST) {
      idsFile.add("AITIME.IDS");    idsFile.add("ALIGN.IDS");     idsFile.add("ALIGNMEN.IDS");
      idsFile.add("ANIMSTAT.IDS");  idsFile.add("ASTYLES.IDS");   idsFile.add("BITS.IDS");
      idsFile.add("BONES.IDS");     idsFile.add("BOOLEAN.IDS");   idsFile.add("CLASS.IDS");
      idsFile.add("CLOWNCLR.IDS");  idsFile.add("CLOWNRGE.IDS");  idsFile.add("DAMAGES.IDS");
      idsFile.add("DELTA.IDS");     idsFile.add("DISGUISE.IDS");  idsFile.add("EA.IDS");
      idsFile.add("EYESIGHT.IDS");  idsFile.add("FACTION.IDS");   idsFile.add("GENDER.IDS");
      idsFile.add("GENERAL.IDS");   idsFile.add("GTIMES.IDS");    idsFile.add("HOTKEY.IDS");
      idsFile.add("INTERNAL.IDS");  idsFile.add("KILLSTAT.IDS");  idsFile.add("MOVVAL.IDS");
      idsFile.add("ORDER.IDS");     idsFile.add("RACE.IDS");      idsFile.add("REACTION.IDS");
      idsFile.add("SCRLEV.IDS");    idsFile.add("SCROLL.IDS");    idsFile.add("SONGFLAG.IDS");
      idsFile.add("SONGS.IDS");     idsFile.add("SOUNDOFF.IDS");  idsFile.add("SPECIFIC.IDS");
      idsFile.add("SPELL.IDS");     idsFile.add("STATE.IDS");     idsFile.add("STATS.IDS");
      idsFile.add("TEAM.IDS");      idsFile.add("TIME.IDS");      idsFile.add("TIMEODAY.IDS");
      idsFile.add("WEATHER.IDS");   idsFile.add("WPROF.IDS");
    } else {    // BG2, BGEE, BG2EE and unknown
      idsFile.add("ALIGN.IDS");     idsFile.add("ALIGNMEN.IDS");  idsFile.add("ANIMATE.IDS");
      idsFile.add("AREAFLAG.IDS");  idsFile.add("AREATYPE.IDS");  idsFile.add("ASTYLES.IDS");
      idsFile.add("BOOLEAN.IDS");   idsFile.add("CLASS.IDS");     idsFile.add("DAMAGES.IDS");
      idsFile.add("DIFFLEV.IDS");   idsFile.add("DMGTYPE.IDS");   idsFile.add("EA.IDS");
      idsFile.add("GENDER.IDS");    idsFile.add("GENERAL.IDS");   idsFile.add("GTIMES.IDS");
      idsFile.add("HAPPY.IDS");     idsFile.add("HOTKEY.IDS");    idsFile.add("JOURTYPE.IDS");
      idsFile.add("KIT.IDS");       idsFile.add("MFLAGS.IDS");    idsFile.add("MODAL.IDS");
      idsFile.add("RACE.IDS");      idsFile.add("REACTION.IDS");  idsFile.add("SCRLEV.IDS");
      idsFile.add("SCROLL.IDS");    idsFile.add("SEQ.IDS");       idsFile.add("SHOUTIDS.IDS");
      idsFile.add("SLOTS.IDS");     idsFile.add("SNDSLOT.IDS");   idsFile.add("SOUNDOFF.IDS");
      idsFile.add("SPECIFIC.IDS");  idsFile.add("SPELL.IDS");     idsFile.add("STATE.IDS");
      idsFile.add("STATS.IDS");     idsFile.add("TIME.IDS");      idsFile.add("TIMEODAY.IDS");
      idsFile.add("WEATHER.IDS");
      if (Profile.isEnhancedEdition()) {
        idsFile.add("BUTTON.IDS");    idsFile.add("DIR.IDS");       idsFile.add("EXTSTATE.IDS");
        idsFile.add("ITEMFLAG.IDS");  idsFile.add("MAPNOTES.IDS");  idsFile.add("MUSIC.IDS");
        idsFile.add("SONGLIST.IDS");  idsFile.add("SPLSTATE.IDS");  idsFile.add("STATMOD.IDS");
        idsFile.add("WMPFLAG.IDS");
      }
    }
    for (Iterator<String> iterIDS = idsFile.iterator(); iterIDS.hasNext();) {
      String ids = iterIDS.next();
      int type = ("SPELL.IDS".equalsIgnoreCase(ids)) ? TOKEN_SYMBOL_SPELL : TOKEN_SYMBOL;
      if (ResourceFactory.resourceExists(ids)) {
        map = IdsMapCache.get(ids);
        if (map != null) {
          for (Iterator<IdsMapEntry> iterEntry = map.getAllValues().iterator(); iterEntry.hasNext();) {
            String name = iterEntry.next().getString();
            if (name != null && !name.isEmpty()) {
              tokenMap.put(name, type);
            }
          }
        }
      }
    }
    tokenMap.put("ANYONE", TOKEN_SYMBOL);

    // objects
    map = IdsMapCache.get("OBJECT.IDS");
    for (Iterator<IdsMapEntry> iter = map.getAllValues().iterator(); iter.hasNext();) {
      String name = extractFunctionName(iter.next().getString());
      if (name != null && !name.isEmpty()) {
        tokenMap.put(name, TOKEN_OBJECT);
      }
    }

    // actions
    map = IdsMapCache.get("ACTION.IDS");
    for (Iterator<IdsMapEntry> iter = map.getAllValues().iterator(); iter.hasNext();) {
      String name = extractFunctionName(iter.next().getString());
      if (name != null && !name.isEmpty()) {
        tokenMap.put(name, TOKEN_ACTION);
      }
    }

    // triggers
    map = IdsMapCache.get("TRIGGER.IDS");
    for (Iterator<IdsMapEntry> iter = map.getAllValues().iterator(); iter.hasNext();) {
      String name = extractFunctionName(iter.next().getString());
      if (name != null && !name.isEmpty()) {
        tokenMap.put(name, TOKEN_TRIGGER);
      }
    }

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
          } else if (c == '"') {
            // string
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
                }
              }
            } else if (RSyntaxUtilities.isDigit(c)) {
              currentTokenType = TOKEN_NUMBER;
              if (c == '0' && i+1 < end && CharHexPrefix.indexOf(array[i+1]) > -1) {
                // hex number
                currentTokenType = TOKEN_HEXNUMBER;
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
          } else if (c == '"') {
            // string
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
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
          } else if (c == '"') {
            // string
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
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
                }
              }
            } else if (RSyntaxUtilities.isDigit(c)) {
              currentTokenType = TOKEN_NUMBER;
              if (c == '0' && i+1 < end && CharHexPrefix.indexOf(array[i+1]) > -1) {
                // hex number
                currentTokenType = TOKEN_HEXNUMBER;
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
          } else if (c == '"') {
            // string
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
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
                }
              }
            } else if (RSyntaxUtilities.isDigit(c)) {
              currentTokenType = TOKEN_NUMBER;
              if (c == '0' && i+1 < end && CharHexPrefix.indexOf(array[i+1]) > -1) {
                // hex number
                currentTokenType = TOKEN_HEXNUMBER;
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
          if (c == '"') {
            addToken(text, currentTokenStart, i, currentTokenType, newStartOfs+currentTokenStart);
            currentTokenType = Token.NULL;
          }
        }   // end of case TOKEN_STRING:
        break;

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
          } else if (c == '"') {
            // string
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
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
          } else if (c == '"') {
            // string
            addToken(text, currentTokenStart, i-1, currentTokenType, newStartOfs+currentTokenStart);
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
            throw new Exception(String.format("Invalid token %1$d found at position %2$d",
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


  // Extracts the function name of the action/trigger definition
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
}
