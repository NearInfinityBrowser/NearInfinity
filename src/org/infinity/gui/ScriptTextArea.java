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
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenImpl;
import org.infinity.NearInfinity;
import org.infinity.icon.Icons;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.bcs.ScriptInfo;
import org.infinity.resource.bcs.Signatures;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.text.modes.BCSTokenMaker;
import org.infinity.util.CreMapCache;
import org.infinity.util.IdsMapCache;
import org.infinity.util.Misc;

/**
 * Extends {@link InfinityTextArea} by script-specific features.
 */
public class ScriptTextArea extends InfinityTextArea implements DocumentListener
{
  private enum IconType {
    INFORMATION,
    WARNING,
    ERROR
  }

  private static final Stroke STROKE_LINK  = new BasicStroke(0.75f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                                                             1.0f, new float[]{1.0f, 2.0f}, 0.0f);

  private static final EnumMap<IconType, Icon> icons = new EnumMap<>(IconType.class);
  static {
    icons.put(IconType.INFORMATION, Icons.getIcon(Icons.ICON_INFORMATION_16));
    icons.put(IconType.WARNING, Icons.getIcon(Icons.ICON_WARNING_16));
    icons.put(IconType.ERROR, Icons.getIcon(Icons.ICON_ERROR_16));
  }

  // Contains tokens that require special attention
  private final TreeMap<Integer, InteractiveToken> tokenMap = new TreeMap<>();
  // Controls concurrent access to tokenMap instance
  private final ReentrantLock tokenMapLock = new ReentrantLock();
  // Special popup menu for interactive resource references
  private final ScriptPopupMenu menu = new ScriptPopupMenu();

  private Signatures triggers;
  private Signatures actions;

  public ScriptTextArea()
  {
    super(true);

    Language lang;
    if (BrowserMenuBar.getInstance() != null &&
        BrowserMenuBar.getInstance().getBcsSyntaxHighlightingEnabled()) {
        lang = Language.BCS;
    } else {
      lang = Language.NONE;
    }
    applyExtendedSettings(lang, null);
    setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getScriptFont()));

    triggers = Signatures.getTriggers();
    actions = Signatures.getActions();

    if (triggers != null && actions != null) {
      getDocument().addDocumentListener(this);

      addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) { handlePopup(e); }

        @Override
        public void mouseReleased(MouseEvent e) { handlePopup(e); }
      });
    }
  }

  @Override
  public void setText(String text)
  {
    super.setText(text);
    discardAllEdits();
  }

  @Override
  protected void paintComponent(Graphics g)
  {
    super.paintComponent(g);

    // getting range of affected lines
    Rectangle rect = g.getClipBounds();
    int offsetMin;
    int offsetMax;
    try {
      int lineMin = getLineOfOffset(viewToModel(rect.getLocation()));
      int lineMax = getLineOfOffset(viewToModel(new Point(rect.x + rect.width, rect.y + rect.height)));
      offsetMin = getLineStartOffset(lineMin);
      offsetMax = getLineEndOffset(lineMax);
    } catch (BadLocationException ble) {
      offsetMin = offsetMax = -1;
    }

    if (offsetMin >= 0 && offsetMax >= offsetMin && tokenMapLock.tryLock()) {
      try {
        SortedMap<Integer, InteractiveToken> map =
            tokenMap.subMap(Integer.valueOf(offsetMin), Integer.valueOf(offsetMax));

        // processing interactive tokens of visible area
        Iterator<Integer> iter = map.keySet().iterator();
        while (iter.hasNext()) {
          Integer key = iter.next();
          InteractiveToken itoken = map.get(key);

          if (itoken.isLink() && !itoken.isSilent()) {
            try {
              Rectangle rectStart = modelToView(itoken.position);
              Rectangle rectEnd = modelToView(itoken.position + itoken.length);

              // 1. clearing line
              Graphics2D g2d = (Graphics2D) g;
              Stroke oldStroke = g2d.getStroke();
              Color oldColor = g2d.getColor();
              g2d.setStroke(STROKE_LINK);
              g2d.setColor(getBackground());
              g2d.drawLine(rectStart.x, rectStart.y + rectStart.height - 1, rectEnd.x, rectEnd.y + rectEnd.height - 1);

              // 2. drawing decorations
              g2d.setColor(itoken.color);
              g2d.drawLine(rectStart.x, rectStart.y + rectStart.height - 1, rectEnd.x, rectEnd.y + rectEnd.height - 1);
              g2d.setStroke(oldStroke);
              g2d.setColor(oldColor);
            } catch (BadLocationException ble) {
            }
          }
        }
      } finally {
        tokenMapLock.unlock();
      }
    }
  }

  @Override
  public String getToolTipText(MouseEvent e)
  {
    String retVal = super.getToolTipText(e);

    tokenMapLock.lock();
    try {
      int offset = viewToModel(e.getPoint());
      int line = getLineOfOffset(offset);
      int ofsMin = getLineStartOffset(line);
      int ofsMax = getLineEndOffset(line);
      SortedMap<Integer, InteractiveToken> map =
          tokenMap.subMap(Integer.valueOf(ofsMin), Integer.valueOf(ofsMax));
      Iterator<InteractiveToken> iter = map.values().iterator();

      while (iter.hasNext()) {
        InteractiveToken itoken = iter.next();
        if (itoken.isTooltip()) {
          if (offset >= itoken.position && offset < itoken.position + itoken.length) {
            retVal = itoken.tooltip;
            break;
          }
        }
      }
    } catch (BadLocationException ble) {
    } finally {
      tokenMapLock.unlock();
    }

    return retVal;
  }

//--------------------- Begin Interface DocumentListener ---------------------

  @Override
  public void insertUpdate(DocumentEvent e)
  {
    updateInteractiveTokens(true);
    repaint();
  }

  @Override
  public void removeUpdate(DocumentEvent e)
  {
    updateInteractiveTokens(true);
    repaint();
  }

  @Override
  public void changedUpdate(DocumentEvent e)
  {
    // ignore?
  }

//--------------------- End Interface DocumentListener ---------------------

//--------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent e)
  {
    // important: stateChanged() is registered by super class
    super.stateChanged(e);

    updateInteractiveTokens(false);
    repaint();
  }

//--------------------- End Interface ChangeListener ---------------------

  /**
   * Adds a new error notification to the gutter at the left edge.
   * @param line The (one-based) line where to add the icon.
   * @param message A tooltip message
   * @param overwrite Set to {@code true} if this message should always replace older messages.
   *                  Set to {@code false} to skip this message if a message of higher or equal
   *                  priority has already been added to this line.
   */
  public void setLineError(int line, String message, boolean overwrite)
  {
    if (!overwrite) {
      GutterIcon item = getGutterIconInfo(line);
      if (item != null) {
        if (item.icon == icons.get(IconType.ERROR)) {
          return;
        }
      }
    }
    line--; // 1-based to 0-based index
    removeGutterIcon(line);
    addGutterIcon(line, icons.get(IconType.ERROR), message);
  }

  /**
   * Adds a new warning notification to the gutter at the left edge.
   * @param line The (one-based) line where to add the icon.
   * @param message A tooltip message
   * @param overwrite Set to {@code true} if this message should always replace older messages.
   *                  Set to {@code false} to skip this message if a message of higher or equal
   *                  priority has already been added to this line.
   */
  public void setLineWarning(int line, String message, boolean overwrite)
  {
    if (!overwrite) {
      GutterIcon item = getGutterIconInfo(line);
      if (item != null) {
        if (item.icon == icons.get(IconType.ERROR) ||
            item.icon == icons.get(IconType.WARNING)) {
          return;
        }
      }
    }
    line--; // 1-based to 0-based index
    removeGutterIcon(line);
    addGutterIcon(line, icons.get(IconType.WARNING), message);
  }

  /**
   * Adds a new information notification to the gutter at the left edge.
   * @param line The (one-based) line where to add the icon.
   * @param message A tooltip message
   * @param overwrite Set to {@code true} if this message should always replace older messages.
   *                  Set to {@code false} to skip this message if a message of higher or equal
   *                  priority has already been added to this line.
   */
  public void setLineInformation(int line, String message, boolean overwrite)
  {
    if (!overwrite) {
      GutterIcon item = getGutterIconInfo(line);
      if (item != null) {
        if (item.icon == icons.get(IconType.ERROR) ||
            item.icon == icons.get(IconType.WARNING) ||
            item.icon == icons.get(IconType.INFORMATION)) {
          return;
        }
      }
    }
    line--; // 1-based to 0-based index
    removeGutterIcon(line);
    addGutterIcon(line, icons.get(IconType.INFORMATION), message);
  }

  private void handlePopup(MouseEvent e)
  {
    if (e.isPopupTrigger()) {
      tokenMapLock.lock();
      try {
        int offset = viewToModel(e.getPoint());
        int line = getLineOfOffset(offset);
        int minOffset = getLineStartOffset(line);
        int maxOffset = getLineEndOffset(line);

        SortedMap<Integer, InteractiveToken> map =
            tokenMap.subMap(Integer.valueOf(minOffset), Integer.valueOf(maxOffset));

        Iterator<InteractiveToken> iter = map.values().iterator();
        while (iter.hasNext()) {
          InteractiveToken token = iter.next();
          // generate list of resource links
          menu.clearResEntries();
          if (token.isLink()) {
            for (final ResourceEntry entry: token.resourceEntries) {
              menu.addResEntry(entry);
            }
            if (offset >= token.position && offset < token.position + token.length) {
              menu.show(this, e.getX(), e.getY());
              break;
            }
          }
        }
      } catch (BadLocationException ble) {
      } finally {
        tokenMapLock.unlock();
      }
    }
  }

  // Processes a change in the document regarding interactive tokens
  private void updateInteractiveTokens(boolean reset)
  {
    tokenMapLock.lock();
    try {
      Point range = getVisibleLineRange(null);
      if (range.x < 0 || range.y < 0) {
        return;
      }

      if (reset) {
        tokenMap.clear();
      } else {
        SortedMap<Integer, InteractiveToken> submap;
        if (range.x > 0) {
          try {
            int offset = getLineStartOffset(range.x);
            submap = tokenMap.subMap(Integer.valueOf(0), Integer.valueOf(offset));
            Iterator<Integer> iter = submap.keySet().iterator();
            while (iter.hasNext()) {
              iter.next();
              iter.remove();
            }
          } catch (BadLocationException ble) {
          }
        }

        try {
          int offset = getLineEndOffset(range.y);
          submap = tokenMap.subMap(Integer.valueOf(offset), Integer.valueOf(Integer.MAX_VALUE));
          Iterator<Integer> iter = submap.keySet().iterator();
          while (iter.hasNext()) {
            iter.next();
            iter.remove();
          }
        } catch (BadLocationException ble) {
        }
      }

      // processing new content
      try {
        int offset = getLineStartOffset(range.x);
        int endOffset = getLineEndOffset(range.y);
        while (offset < endOffset) {
          Token token = null;
          while (offset <= endOffset && token == null) {
            token = modelToToken(offset++);
          }

          if (token != null) {
            offset = token.getEndOffset();
            InteractiveToken result = null;
            Integer key = Integer.valueOf(token.getOffset());
            if (!tokenMap.containsKey(key)) {
              Token curToken = new TokenImpl(token);  // make sure content of current token doesn't change
              switch (curToken.getType()) {
                case BCSTokenMaker.TOKEN_ACTION:
                  result = updateFunctionToken(curToken, actions);
                  break;
                case BCSTokenMaker.TOKEN_TRIGGER:
                  result = updateFunctionToken(curToken, triggers);
                  break;
                case BCSTokenMaker.TOKEN_SYMBOL:
                case BCSTokenMaker.TOKEN_SYMBOL_SPELL:
                  result = updateSymbolToken(curToken);
                  break;
                case BCSTokenMaker.TOKEN_STRING:
                  result = updateStringToken(curToken);
                  break;
              }

              if (result != null) {
                tokenMap.put(key, result);
              }
            }
          }
        }
      } catch (BadLocationException ble) {
      }
    } finally {
      tokenMapLock.unlock();
    }
  }

  // Process action or trigger name
  private InteractiveToken updateFunctionToken(Token token, Signatures sig)
  {
    InteractiveToken retVal = null;
    String name = token.getLexeme();
    Signatures.Function function = sig.getFunction(name);
    if (function != null) {
      retVal = new InteractiveToken(token.getOffset(), token.length(), function.toString(),
                                    null, getForegroundForToken(token));
    }
    return retVal;
  }

  // Process symbol or symbolic spell name
  private InteractiveToken updateSymbolToken(Token token)
  {
    InteractiveToken retVal = null;
    Signatures.Function.Parameter param = getFunctionParameter(token);
    if (param != null) {
      if (param.getType() == Signatures.Function.Parameter.TYPE_INTEGER) {
        String idsRef = param.getIdsRef().toUpperCase(Locale.ENGLISH);
        if (!idsRef.isEmpty()) {
          if (idsRef.equals("SPELL")) {
            // resolving symbolic spell name
            String resRef = org.infinity.resource.spl.Viewer.getResourceName(token.getLexeme(), true);
            if (resRef != null) {
              ResourceEntry entry = ResourceFactory.getResourceEntry(resRef);
              String name = null;
              if (entry != null) {
                name = entry.getSearchString();
              }
              String text = (name != null) ? resRef + " (" + name + ")" : resRef;
              retVal = new InteractiveToken(token.getOffset(), token.length(), text, entry,
                                            getForegroundForToken(token));
              retVal.resourceEntries.add(ResourceFactory.getResourceEntry("SPELL.IDS"));
            }
          } else {
            // resolving regular symbol
            idsRef = idsRef + ".IDS";
            Long value = IdsMapCache.getIdsValue(idsRef, token.getLexeme(), null);
            if (value != null) {
              retVal = new InteractiveToken(token.getOffset(), token.length(),
                                            idsRef + ": " + value.toString() + " (0x" + Long.toHexString(value.longValue()) + ")",
                                            ResourceFactory.getResourceEntry(idsRef), getForegroundForToken(token));
            }
          }
        }
      } else if (param.getType() == Signatures.Function.Parameter.TYPE_OBJECT) {
        String idsRef = getIdsTargetResRef(token);
        if (idsRef != null) {
          idsRef = IdsMapCache.getValidIdsRef(idsRef);
          Long value = IdsMapCache.getIdsValue(idsRef, token.getLexeme(), null);
          if (value != null) {
            retVal = new InteractiveToken(token.getOffset(), token.length(),
                                          idsRef + ": " + value.longValue() + " (0x" + Long.toHexString(value.longValue()) + ")",
                                          ResourceFactory.getResourceEntry(idsRef), getForegroundForToken(token));
          }
        }
      }
    }
    return retVal;
  }

  // Process string
  private InteractiveToken updateStringToken(Token token)
  {
    InteractiveToken retVal = null;
    Signatures.Function.Parameter param = getFunctionParameter(token);
    if (param != null &&
        (param.getType() == Signatures.Function.Parameter.TYPE_STRING ||
         param.getType() == Signatures.Function.Parameter.TYPE_OBJECT)) {
      // getting string content
      String value = token.getLexeme();
      if (value != null && value.length() >= 2) {
        String delim = "\"~%";
        int v1 = delim.indexOf(value.charAt(0));
        int v2 = delim.indexOf(value.charAt(value.length() - 1));
        if (v1 > -1 && v2 > -1 && v1 == v2) {
          value = value.substring(1, value.length() - 1);
        }
      }

      String[] types;
      if (param.getType() == Signatures.Function.Parameter.TYPE_OBJECT) {
        types = new String[]{Signatures.Function.Parameter.RESTYPE_SCRIPT};
      } else {
        types = param.getResourceType();
      }

      for (String type: types) {
        if (type.equals(Signatures.Function.Parameter.RESTYPE_SCRIPT)) {
          // script name
          Set<ResourceEntry> set = CreMapCache.getCreForScriptName(value);
          if (set != null && !set.isEmpty()) {
            ResourceEntry entry = set.iterator().next();
            String name = entry.getSearchString();
            String text = (name != null) ? entry.getResourceName() + " (" + name + ")" : entry.getResourceName();
            retVal = new InteractiveToken(token.getOffset() + 1, token.length() - 2, text, entry,
                                          getForegroundForToken(token));
            break;
          }
        } else if (type.equals(Signatures.Function.Parameter.RESTYPE_SPELL_LIST)) {
          // list of spell codes
          String text = "";
          ArrayList<ResourceEntry> resList = new ArrayList<>();
          for (int i = 0, cnt = value.length() / 4; i < cnt; i++) {
            String snum = value.substring(i*4, i*4 + 4);
            String res = null;
            try {
              long num = Long.parseLong(snum);
              res = IdsMapCache.getIdsSymbol("SPELL.IDS", num);
              String resRef = org.infinity.resource.spl.Viewer.getResourceName((int)num, true);
              if (ResourceFactory.resourceExists(resRef)) {
                resList.add(ResourceFactory.getResourceEntry(resRef));
              }
            } catch (NumberFormatException e) {
            }
            if (res != null) {
              if (i > 0) { text += ", "; }
              text += res;
            }
          }
          retVal = new InteractiveToken(token.getOffset() + 1, token.length() - 2, text, null,
                                        getForegroundForToken(token));
          retVal.resourceEntries.addAll(resList);
          retVal.resourceEntries.add(ResourceFactory.getResourceEntry("SPELL.IDS"));
          break;
        } else if (Character.isUpperCase(type.charAt(0)) || Character.isDigit(type.charAt(0))) {
          // regular resource
          if (!"ARE".equals(type) || !ScriptInfo.getInfo().isGlobalScope(token.getLexeme())) {
            String resRef = value + '.' + type;
            ResourceEntry entry = ResourceFactory.getResourceEntry(resRef, true);
            if (entry != null) {
              String name = entry.getSearchString();
              String text = (name != null) ? entry.getResourceName() + " (" + name + ")" : entry.getResourceName();
              retVal = new InteractiveToken(token.getOffset() + 1, token.length() - 2, text, entry,
                                            getForegroundForToken(token));
              break;
            }
          }
        }
      }
    }
    return retVal;
  }

  private Signatures.Function.Parameter getFunctionParameter(Token token)
  {
    Signatures.Function.Parameter retVal = null;

    if (token != null) {
      int offset = token.getOffset();
      Token resultToken = null;
      int index = 0;
      while (offset > 0) {
        token = null;
        while (offset > 0 && token == null) {
          token = modelToToken(--offset);
        }

        if (token != null) {
          offset = token.getOffset();
          if (token.getType() == BCSTokenMaker.TOKEN_ACTION ||
              token.getType() == BCSTokenMaker.TOKEN_TRIGGER) {
            resultToken = token;
            break;
          } else if (token.getType() == BCSTokenMaker.TOKEN_KEYWORD) {
            break;
          } else if (token.getType() == BCSTokenMaker.TOKEN_OPERATOR &&
                     token.getLexeme().indexOf(',') >= 0) {
            index++;
          }
        }
      }

      if (resultToken != null) {
        Signatures.Function function = null;
        if (resultToken.getType() == BCSTokenMaker.TOKEN_ACTION) {
          function = actions.getFunction(resultToken.getLexeme());
        } else if (resultToken.getType() == BCSTokenMaker.TOKEN_TRIGGER) {
          function = triggers.getFunction(resultToken.getLexeme());
        }
        if (function != null) {
          if (index >= 0 && index < function.getNumParameters()) {
            retVal = function.getParameter(index);
          }
        }
      }
    }

    return retVal;
  }

  private String getIdsTargetResRef(Token token)
  {
    String retVal = null;

    if (token != null && token.getType() == BCSTokenMaker.TOKEN_SYMBOL) {
      int offset = token.getOffset();
      Token resultToken = null;
      int index = 0;
      while (offset > 0) {
        token = null;
        while (offset > 0 && token == null) {
          token = modelToToken(--offset);
        }

        if (token != null) {
          offset = token.getOffset();
          if (token.getType() == BCSTokenMaker.TOKEN_OPERATOR && token.getLexeme().indexOf('[') >= 0) {
            resultToken = token;
            break;
          } else if (token.getType() == BCSTokenMaker.TOKEN_KEYWORD ||
                     token.getType() == BCSTokenMaker.TOKEN_ACTION ||
                     token.getType() == BCSTokenMaker.TOKEN_TRIGGER) {
            break;
          } else if (token.getType() == BCSTokenMaker.TOKEN_OPERATOR &&
                     (token.getLexeme().indexOf(',') >= 0 || token.getLexeme().indexOf('(') >= 0)) {
            break;
          } else if (token.getType() == BCSTokenMaker.TOKEN_OPERATOR &&
                     token.getLexeme().indexOf('.') >= 0) {
            index++;
          }
        }
      }

      if (resultToken != null) {
        String[] idsFiles = ScriptInfo.getInfo().getObjectIdsList();
        if (index >= 0 && index < idsFiles.length) {
          retVal = idsFiles[index];
        }
      }
    }

    return retVal;
  }

//-------------------------- INNER CLASSES --------------------------

  private static class InteractiveToken
  {
    public final ArrayList<ResourceEntry> resourceEntries = new ArrayList<>();
    public final int position;    // position within the text
    public final int length;      // length of token
    public final String tooltip;
    public final Color color;

    public boolean silent;

    public InteractiveToken(int position, int length, String tooltip, ResourceEntry resourceEntry, Color color)
    {
      this.position = position;
      this.length = length;
      this.tooltip = tooltip;
      this.color = color;
      if (resourceEntry != null) {
        resourceEntries.add(resourceEntry);
      }
    }

    public boolean isTooltip() { return (tooltip != null) && !tooltip.isEmpty(); }

    public boolean isLink() { return !resourceEntries.isEmpty(); }

    public boolean isSilent() { return silent || !isTooltip(); }
  }

  private static class ScriptPopupMenu extends JPopupMenu implements ActionListener
  {
    private final ArrayList<DataMenuItem> itemsOpen = new ArrayList<>();
    private final ArrayList<DataMenuItem> itemsOpenNew = new ArrayList<>();

    public ScriptPopupMenu()
    {
    }

    public void addResEntry(ResourceEntry resEntry)
    {
      if (!itemsOpen.isEmpty()) {
        addSeparator();
      }

      DataMenuItem item = new DataMenuItem("Open \"" + resEntry.getResourceName() + "\"", null, resEntry);
      item.addActionListener(this);
      itemsOpen.add(item);
      add(item);
      item = new DataMenuItem("Open \"" + resEntry.getResourceName() + "\" in new window", null, resEntry);
      item.addActionListener(this);
      itemsOpenNew.add(item);
      add(item);
    }

    public void clearResEntries()
    {
      itemsOpen.forEach((item) -> { item.removeActionListener(this); });
      itemsOpen.clear();
      itemsOpenNew.forEach((item) -> { item.removeActionListener(this); });
      itemsOpenNew.clear();
      removeAll();
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
      if (e.getSource() instanceof DataMenuItem) {
        DataMenuItem item = (DataMenuItem)e.getSource();
        ResourceEntry entry = (item.getData() instanceof ResourceEntry) ? (ResourceEntry)item.getData() : null;
        if (entry != null) {
          if (itemsOpen.contains(e.getSource())) {
            NearInfinity.getInstance().showResourceEntry(entry);
          } else if (itemsOpenNew.contains(e.getSource())) {
            new ViewFrame(NearInfinity.getInstance(), ResourceFactory.getResource(entry));
          }
        }
      }
    }
  }
}
