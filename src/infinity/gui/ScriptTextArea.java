package infinity.gui;

import infinity.NearInfinity;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.bcs.Compiler;
import infinity.resource.bcs.Decompiler;
import infinity.resource.key.ResourceEntry;
import infinity.util.IdsMapCache;
import infinity.util.IdsMapEntry;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
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
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

public class ScriptTextArea extends JTextArea {
  ScriptPopupMenu menu = new ScriptPopupMenu();

  public ScriptTextArea() {
    super();
    addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent ev) {
        handlePopup(ev);
      }

      public void mouseReleased(MouseEvent ev) {
        handlePopup(ev);
      }
    });
  }

  // try to paint an indicator below "crosslinks"
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
          g2d.drawLine(rectStart.x, rectStart.y + rectStart.height,
                     rectEnd.x, rectEnd.y + rectEnd.height);
        }
        g2d.setStroke(oldStroke);
      } catch (BadLocationException e) { }
    }
  }

  // looks for "crosslinks" in script lines
  private int[][] findLinksInSection(int start, int end) throws BadLocationException {
    ArrayList<int[]> links = new ArrayList<int[]>();
    String linetext = getText(start, end - start);

    int posStartToken = -1;
    for (int i = 0; i < linetext.length(); i++) {
      if (linetext.charAt(i) == '"') {
        if (posStartToken != -1) {
          // found a "word", so check for possible crosslink
          String token = linetext.substring(posStartToken + 1, i);

          if (findResEntry(linetext, posStartToken + 1, token) != null) {
            // add it to our list of crosslinks
            links.add(new int[] { start + posStartToken + 1, start + i });
          }
          posStartToken = -1;
        }
        else {
          posStartToken = i;
        }
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
        String definition = paramDefs[paramPos];

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
          Compiler bcscomp = Compiler.getInstance();
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
          IdsMapEntry idsSpell = IdsMapCache.get("spell.ids").lookup(token);
          if (idsSpell != null) {
            String spellID = String.valueOf(idsSpell.getID());
            int type = Character.digit(spellID.charAt(0), 10);
            String splfile;
            switch (type) {
              case 1:
                splfile = "SPPR";
                break;

              case 2:
                splfile = "SPWI";
                break;

              case 3:
                splfile = "SPIN";
                break;

              case 4:
                splfile = "SPCL";
                break;

              default:
                return null;
            }
            splfile += spellID.substring(1) + ".SPL";
            if (ResourceFactory.getInstance().resourceExists(splfile)) {
              return ResourceFactory.getInstance().getResourceEntry(splfile);
            }

          }

          // found nothing, if this line is reached
          return null;
        }

        // guessing
        String[] possibleExtensions = guessExtension(function, definition);
        for (final String ext : possibleExtensions) {
          if (ResourceFactory.getInstance().resourceExists(token + ext)) {
            return ResourceFactory.getInstance().getResourceEntry(token + ext);
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
      return new String[] { ".BAM", ".VVC" };
    }
    else if (definition.equalsIgnoreCase("S:DialogFile*")) {
      return new String[] { ".DLG", ".VVC" };
    }
    else if (definition.equalsIgnoreCase("S:Object*")) {
      return new String[] { ".ITM", ".VVC", ".BAM" };
    }
    else if (definition.equalsIgnoreCase("S:NewObject*")) {
      return new String[] { ".CRE", ".DLG", ".BCS", ".ITM" };
    }
    else if (definition.equalsIgnoreCase("S:ResRef*")) {
      return Decompiler.getResRefType(function);
    }

    return new String[] {};
  }

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
