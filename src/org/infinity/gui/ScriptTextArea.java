// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Set;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.infinity.NearInfinity;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.bcs.Compiler;
import org.infinity.resource.bcs.Decompiler;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.text.modes.BCSTokenMaker;
import org.infinity.util.IdsMapCache;
import org.infinity.util.IdsMapEntry;

public class ScriptTextArea extends InfinityTextArea
{
  private ScriptPopupMenu menu = new ScriptPopupMenu();

  public ScriptTextArea() {
    super(true);

    Language lang;
    if (BrowserMenuBar.getInstance() != null &&
        BrowserMenuBar.getInstance().getBcsSyntaxHighlightingEnabled()) {
        lang = Language.BCS;
    } else {
      lang = Language.NONE;
    }
    applyExtendedSettings(lang, null);

    addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent ev) {
        handlePopup(ev);
      }

      @Override
      public void mouseReleased(MouseEvent ev) {
        handlePopup(ev);
      }
    });
  }

  @Override
  public void setText(String text)
  {
    // prevent undo to remove the text
    super.setText(text);
    discardAllEdits();
  }

  // try to paint an indicator below "crosslinks"
  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Rectangle rect = g.getClipBounds();

    // try to get the "lines" which need to be painted
    int upperLine = 0;
    int lowerLine = 0;
    try {
      upperLine = getLineOfOffset(viewToModel(new Point(rect.x, rect.y)));
      lowerLine = getLineOfOffset(viewToModel(new Point(rect.x + rect.width,
                                                        rect.y + rect.height)));
    } catch (BadLocationException e) { }
    //System.err.println("would consider drawing from lines " + upperLine + " to " + lowerLine);
    for (int line = upperLine; line <= lowerLine; line++) {
      try {
        int start = getLineStartOffset(line);
        int end = getLineEndOffset(line) - 1; // newline

        int[][] linkOffsets = findLinksInSection(start, end);
        if (linkOffsets.length == 0) {
          continue;
        }

        Graphics2D g2d = (Graphics2D) g;
        // clear that line before doing anything
        Color oldColor = g2d.getColor();
        g2d.setColor(getBackground());
        Rectangle rectStart = modelToView(start);
        Rectangle rectEnd = modelToView(end);
        g2d.drawLine(rectStart.x, rectStart.y + rectStart.height,
                     rectEnd.x, rectEnd.y + rectEnd.height);
        g2d.setColor(oldColor);

        Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                                      1, new float[] { 1, 2 }, 0));
        // now underline the crosslinks
        for (int[] pair : linkOffsets) {
          // convert into view coordinates
          rectStart = modelToView(pair[0]);
          rectEnd = modelToView(pair[1]);
          g2d.drawLine(rectStart.x, rectStart.y + rectStart.height - 1,
                     rectEnd.x, rectEnd.y + rectEnd.height - 1);
        }
        g2d.setStroke(oldStroke);
      } catch (BadLocationException e) { }
    }
  }

  // looks for "crosslinks" in script lines
  private int[][] findLinksInSection(int start, int end) throws BadLocationException {
    ArrayList<int[]> links = new ArrayList<int[]>();

    int startLine = getLineOfOffset(start);
    int endLine = getLineOfOffset(end);
    for (int i = startLine; i <= endLine; i++) {
      // skipping folded lines
      boolean folded = false;
      for (int j = 0; j < getFoldManager().getFoldCount(); j++) {
        Fold fold = getFoldManager().getFold(j);
        if (fold.isCollapsed() && i >= fold.getStartLine() && i <= fold.getEndLine()) {
          folded = true;
          break;
        }
      }
      if (folded) {
        continue;
      }

      // looking for crosslinks
      String lineText = getText().substring(getLineStartOffset(i), getLineEndOffset(i));
      Token token = getTokenListForLine(i);
      while (token != null && token.getType() != Token.NULL) {
        if (token.getOffset() >= start && token.getOffset() + token.length() <= end) {
          if (token.getType() == BCSTokenMaker.TOKEN_STRING && token.length() > 2) {
            int ofsTokenFromLineStart = token.getOffset() - getLineStartOffset(i);
            String text = token.getLexeme().substring(1, token.length() - 1);
            if (findResEntry(lineText, ofsTokenFromLineStart + 1, text) != null) {
              // add it to our list of crosslinks
              links.add(new int[]{start + ofsTokenFromLineStart + 1,
                                  start + ofsTokenFromLineStart + token.length() - 1});
            }
          } else if (token.getType() == BCSTokenMaker.TOKEN_SYMBOL_SPELL) {
            int ofsTokenFromLineStart = token.getOffset() - getLineStartOffset(i);
            String text = token.getLexeme();
            if (findResEntry(lineText, ofsTokenFromLineStart + 1, text) != null) {
              // add it to our list of crosslinks
              links.add(new int[]{start + ofsTokenFromLineStart,
                                  start + ofsTokenFromLineStart + token.length()});
            }
          }
        }
        token = token.getNextToken();
      }
    }
    return links.toArray(new int[0][0]);
  }

  private void handlePopup(MouseEvent ev) {
    if (ev.isPopupTrigger()) {
      try {
        // get "word" under click
        Document doc = getDocument();
        int offset = viewToModel(ev.getPoint());
        final int lineNr = getLineOfOffset(offset);
        final int lineStart = getLineStartOffset(lineNr);
        String line = doc.getText(lineStart, getLineEndOffset(lineNr) - lineStart);
        offset = offset - lineStart;

        String token = getToken(line, offset, "\"", "(), "); // quoted
        if (token == null) {
          // fall back to ids token parsing
          token = getToken(line, offset, ",()", "[].\" "); // IDS
          if (token == null) {
            return;
          }
        }

        ResourceEntry resEntry = findResEntry(line, offset, token);
        menu.setResEntry(resEntry);

        if (resEntry != null) {
          menu.show(this, ev.getX(), ev.getY());
        }
      }
      catch (BadLocationException ble) {}
    }
  }

  private String getToken(String line, int offset, String delims, String invalidChars) {
    int tokenStart, tokenEnd;
    for (tokenStart = offset; tokenStart > 0; tokenStart--) {
      char current = line.charAt(tokenStart);
      if (delims.indexOf(current) != -1) {
        tokenStart++;
        break;
      }
      else if (invalidChars.indexOf(current) != -1) {
        return null;
      }
    }
    for (tokenEnd = offset + 1; tokenEnd < line.length(); tokenEnd++) {
      char current = line.charAt(tokenEnd);
      if (delims.indexOf(current) != -1) {
        break;
      }
      else if (invalidChars.indexOf(current) != -1) {
        return null;
      }
    }
    return line.substring(tokenStart, tokenEnd);
  }

  private ResourceEntry findResEntry(String line, int offset, String token) {
    // determine function name and param position
    int parenLevel = 0;
    int paramPos = 0;
    int idx;
    for (idx = offset; idx > 0; idx--) {
      char current = line.charAt(idx);
      if (current == ')') {
        parenLevel++;
      }
      else if ((current == ',') && (parenLevel == 0)) {
        paramPos++;
      }
      else if (current == '(') {
        if (parenLevel == 0) {
          // found end of corresponding function name
          break;
        }
        parenLevel--;
      }
    }

    int endPos = idx;
    while ((idx > 0) && (Character.isLetter(line.charAt(idx - 1)))) {
      idx--;
    }
    String function = line.substring(idx, endPos);

    // lookup function name in trigger.ids / action.ids
    String[] idsFiles = new String[] { "trigger.ids", "action.ids" };
    for (final String idsFile : idsFiles) {
      IdsMapEntry idsEntry = IdsMapCache.get(idsFile).lookup(function + "(");
      if (idsEntry != null) {
        String[] paramDefs = idsEntry.getParameters().split(",");
        String definition;
        if (paramPos >= 0 && paramPos < paramDefs.length) {
          definition = paramDefs[paramPos];
        } else {
          definition = "";
        }

        // check script names (death var)
        if (definition.equalsIgnoreCase("O:Object*")
         || definition.equalsIgnoreCase("O:Target*")
         || definition.equalsIgnoreCase("O:Actor*")
         || (definition.equalsIgnoreCase("S:Name*")
          && (function.equalsIgnoreCase("Dead")
           || function.equalsIgnoreCase("Name")
           || function.equalsIgnoreCase("NumDead")
           || function.equalsIgnoreCase("NumDeadGT")
           || function.equalsIgnoreCase("NumDeadLT")))) {
          Compiler bcscomp = new Compiler();
          if (bcscomp.hasScriptName(token)) {
            Set<ResourceEntry> entries = bcscomp.getResForScriptName(token);
            for (ResourceEntry entry : entries) {
              // for now, just return the first entry
              return entry;
            }
          }
          else {
            return null;
          }
        }

        // spell.ids
        if (definition.equalsIgnoreCase("I:Spell*Spell")) {
          // retrieving spell resource specified by symbolic spell name
          String resName = org.infinity.resource.spl.Viewer.getResourceName(token, true);
          if (resName != null && !resName.isEmpty() &&
              ResourceFactory.resourceExists(resName, true)) {
            return ResourceFactory.getResourceEntry(resName, true);
          } else {
            return null;
          }
        }

        // guessing
        String[] possibleExtensions = guessExtension(function, definition);
        for (final String ext : possibleExtensions) {
          if (ResourceFactory.resourceExists(token + ext, true)) {
            return ResourceFactory.getResourceEntry(token + ext, true);
          }
        }

        break;
      }
    }
    return null;
  }

  // most parts stolen from Compiler.java
  private String[] guessExtension(String function, String definition) {
    definition = definition.trim();
    // first the unique values
    if (definition.equalsIgnoreCase("S:Area*")
     || definition.equalsIgnoreCase("S:Area1*")
     || definition.equalsIgnoreCase("S:Area2*")
     || definition.equalsIgnoreCase("S:ToArea*")
     || definition.equalsIgnoreCase("S:Areaname*")
     || definition.equalsIgnoreCase("S:FromArea*")) {
      return new String[] { ".ARE" };
    }
    else if (definition.equalsIgnoreCase("S:BamResRef*")) {
      return new String[] { ".BAM" };
    }
    else if (definition.equals("S:CutScene*")
          || definition.equalsIgnoreCase("S:ScriptFile*")
          || definition.equalsIgnoreCase("S:Script*")) {
      return new String[] { ".BCS" };
    }
    else if (definition.equalsIgnoreCase("S:Palette*")) {
      return new String[] { ".BMP" };
    }
    else if (definition.equalsIgnoreCase("S:Item*")
          || definition.equalsIgnoreCase("S:Take*")
          || definition.equalsIgnoreCase("S:Give*")
          || definition.equalsIgnoreCase("S:Item")
          || definition.equalsIgnoreCase("S:OldObject*")) {
      return new String[] { ".ITM" };
    }
    else if (definition.equalsIgnoreCase("S:Parchment*")) {
      return new String[] { ".MOS" };
    }
    else if (definition.equalsIgnoreCase("S:Spell*")
          || definition.equalsIgnoreCase("S:Res*")) {
      return new String[] { ".SPL" };
    }
    else if (definition.equalsIgnoreCase("S:Pool*")) {
      return new String[] { ".SRC" };
    }
    else if (definition.startsWith("S:Store*")) {
      return new String[] { ".STO" };
    }
    else if (definition.equalsIgnoreCase("S:Sound*")
          || definition.equalsIgnoreCase("S:Voice*")) {
      return new String[] { ".WAV" };
    }
    else if (definition.equalsIgnoreCase("S:TextList*")) {
      return new String[] { ".2DA" };
    }
    // and now the ambiguous
    else if (definition.equalsIgnoreCase("S:Effect*")) {
      return new String[] { ".VEF", ".VVC", ".BAM" };
    }
    else if (definition.equalsIgnoreCase("S:DialogFile*")) {
      return new String[] { ".DLG", ".VEF", ".VVC", ".BAM" };
    }
    else if (definition.equalsIgnoreCase("S:Object*")) {
      return Decompiler.getResRefType(function);
    }
    else if (definition.equalsIgnoreCase("S:NewObject*")) {
      return Decompiler.getResRefType(function);
    }
    else if (definition.equalsIgnoreCase("S:ResRef*")) {
      return Decompiler.getResRefType(function);
    }

    return new String[] {};
  }


//-------------------------- INNER CLASSES --------------------------

  private class ScriptPopupMenu extends JPopupMenu implements ActionListener {
    private ResourceEntry resourceEntry = null;
    private final JMenuItem mi_open = new JMenuItem("Open");
    private final JMenuItem mi_opennew = new JMenuItem("Open in new window");

    ScriptPopupMenu() {
      add(mi_open);
      add(mi_opennew);

      mi_open.addActionListener(this);
      mi_opennew.addActionListener(this);
    }

    public void setResEntry(ResourceEntry resEntry) {
      this.resourceEntry = resEntry;
    }
    @Override
    public void actionPerformed(ActionEvent ev) {
      if (ev.getSource() == mi_open) {
        NearInfinity.getInstance().showResourceEntry(resourceEntry);
      }
      else if (ev.getSource() == mi_opennew) {
        Resource res = ResourceFactory.getResource(resourceEntry);
        new ViewFrame(NearInfinity.getInstance(), res);
      }
    }
  }
}
