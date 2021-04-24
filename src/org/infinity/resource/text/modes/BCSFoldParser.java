// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.text.modes;

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.fife.ui.rsyntaxtextarea.folding.FoldParser;
import org.fife.ui.rsyntaxtextarea.folding.FoldType;

/**
 * A fold parser for BCS scripts. It supports folding of multiline comments
 * and IF/END blocks.
 */
public class BCSFoldParser implements FoldParser
{
  private static final char[] BLOCK_START = {'I', 'F'};
  private static final char[] BLOCK_END = {'E', 'N', 'D'};
  private static final char[] MLC_END = {'*', '/'};

  public BCSFoldParser()
  {
  }

  @Override
  public List<Fold> getFolds(RSyntaxTextArea textArea)
  {
    List<Fold> folds = new ArrayList<>();

    Fold curFold = null;
    int lineCount = textArea.getLineCount();
    boolean inMLC = false;
    int mlcStart = 0;

    try {
      for (int line = 0; line < lineCount; line++) {
        Token t = textArea.getTokenListForLine(line);
        while (t != null && t.isPaintable()) {
          if (t.isComment()) {
            // continuing an MLC from a previous line?
            if (inMLC) {
              // found the end of the MLC starting on the previous line...
              if (t.endsWith(MLC_END)) {
                int mlcEnd = t.getEndOffset() - 1;
                if (curFold == null) {
                  curFold = new Fold(FoldType.COMMENT, textArea, mlcStart);
                  curFold.setEndOffset(mlcEnd);
                  folds.add(curFold);
                  curFold = null;
                } else {
                  curFold = curFold.createChild(FoldType.COMMENT, mlcStart);
                  curFold.setEndOffset(mlcEnd);
                  curFold = curFold.getParent();
                }
                inMLC = false;
                mlcStart = 0;
              }
              // otherwise this MLC is continuing on to yet another line
            } else {
              // if we're in an MLC that ends on a later line...
              if (t.getType() == Token.COMMENT_MULTILINE && !t.endsWith(MLC_END)) {
                inMLC = true;
                mlcStart = t.getOffset();
              }
            }
          } else if (t.is(Token.RESERVED_WORD, BLOCK_START)) {
            // a script block starts
            if (curFold == null) {
              curFold = new Fold(FoldType.CODE, textArea, t.getOffset());
            }
          } else if (t.is(Token.RESERVED_WORD, BLOCK_END)) {
            // a script block ends - we don't need to consider nested blocks
            if (curFold != null) {
              curFold.setEndOffset(t.getEndOffset() - 1);
              folds.add(curFold);
              curFold = null;
            }
          }
          t = t.getNextToken();
        }
      }
    } catch (BadLocationException e) {
      e.printStackTrace();
    }

    return folds;
  }
}
