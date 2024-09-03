// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.text;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import org.infinity.gui.ButtonPanel;
import org.infinity.gui.ButtonPopupMenu;
import org.infinity.gui.InfinityScrollPane;
import org.infinity.gui.InfinityTextArea;
import org.infinity.gui.menu.BrowserMenuBar;
import org.infinity.resource.Closeable;
import org.infinity.resource.Profile;
import org.infinity.resource.Referenceable;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.TextResource;
import org.infinity.resource.ViewableContainer;
import org.infinity.resource.Writeable;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.ReferenceSearcher;
import org.infinity.search.TextResourceSearcher;
import org.infinity.util.IdsMap;
import org.infinity.util.IdsMapCache;
import org.infinity.util.IdsMapEntry;
import org.infinity.util.Logger;
import org.infinity.util.Misc;
import org.infinity.util.StaticSimpleXorDecryptor;
import org.infinity.util.io.StreamUtils;

public class PlainTextResource
    implements TextResource, Writeable, ActionListener, ItemListener, DocumentListener, Closeable, Referenceable {
  private final ResourceEntry entry;

  protected final String text;

  private final ButtonPanel buttonPanel = new ButtonPanel();

  private JMenuItem iFindAll;
  private JMenuItem iFindThis;
  private JMenuItem miFormatTrim;
  private JMenuItem miFormatAlignCompact;
  private JMenuItem miFormatAlignUniform;
  private JMenuItem miFormatSort;
  private JPanel panel;

  /** Text editor for editing resource. Created after calling {@link #makeViewer}. */
  protected InfinityTextArea editor;

  private boolean resourceChanged;
  private int highlightedLine = -1;

  /** Returns description for INI resources linked to ARE resources or listed in ANIMATE.IDS. */
  public static String getSearchString(ResourceEntry entry) {
    String retVal = null;
    if (entry != null && "INI".equalsIgnoreCase(entry.getExtension())) {
      try {
        // try animation id first
        int animId = Integer.parseInt(entry.getResourceRef(), 16);
        if (animId >= 0 && animId <= 0xffff) {
          IdsMap idsMap = IdsMapCache.get("ANIMATE.IDS");
          if (idsMap != null) {
            IdsMapEntry idsEntry = idsMap.get(animId);
            if (idsEntry != null) {
              retVal = idsEntry.getSymbol();
            }
          }
        }
      } catch (NumberFormatException e) {
        Logger.trace(e);
      }

      if (retVal == null) {
        ResourceEntry areEntry = ResourceFactory.getResourceEntry(entry.getResourceRef() + ".ARE");
        if (areEntry != null) {
          retVal = AreResource.getSearchString(areEntry);
        }
      }
    }
    return retVal;
  }

  /**
   * Removes whitespace at the beginning and/or end of lines.
   *
   * @param text The text to trim.
   * @param trailing Whether to trim trailing whitespace.
   * @param leading Whether to trim leading whitespace.
   * @return The trimmed string. Returns {@code null} if {@code text} argument is {@code null}.
   */
  public static String trimSpaces(String text, boolean trailing, boolean leading) {
    String retVal = null;
    if (text != null) {
      final String newline = text.contains("\r\n") ? "\r\n" : "\n";
      final String[] lines = text.split("\r?\n");
      final StringBuilder newText = new StringBuilder();
      for (String line : lines) {
        if (trailing && leading) {
          newText.append(line.trim()).append(newline);
        } else if (trailing) {
          newText.append(Misc.trimEnd(line)).append(newline);
        } else if (leading) {
          newText.append(Misc.trimStart(line)).append(newline);
        } else {
          newText.append(line).append(newline);
        }
      }
      retVal = newText.toString();
    }
    return retVal;
  }

  /**
   * Aligns table columns individually.
   *
   * @param text The text content with table columns.
   * @return The aligned text. Returns {@code null} if {@code text} argument is {@code null}.
   */
  public static String alignTableColumnsCompact(String text) {
    return alignTableColumns(text, 2, true, 4);
  }

  /**
   * Aligns all table columns evenly, comparable to WeiDU's PRETTY_PRINT_2DA.
   *
   * @param text The text content with table columns.
   * @return The aligned text. Returns {@code null} if {@code text} argument is {@code null}.
   */
  public static String alignTableColumnsUniform(String text) {
    return alignTableColumns(text, 1, false, 1);
  }

  /**
   * Aligns table columns to improve readability.
   *
   * @param text The text content with table columns.
   * @param spaces         Min. number of spaces between columns.
   * @param alignPerColumn specify {@code true} to calculate max width on a per column basis, or {@code false} to
   *                       calculate for the whole table.
   * @param multipleOf     ensures that column position is always a multiple of the specified value. (e.g. specify 2 to
   *                       have every column start at an even horizontal position.)
   * @return The aligned text. Returns {@code null} if {@code text} argument is {@code null}.
   */
  public static String alignTableColumns(String text, int spaces, boolean alignPerColumn, int multipleOf) {
    String retVal = null;
    if (text != null) {
      spaces = Math.max(1, spaces);
      multipleOf = Math.max(1, multipleOf);

      // splitting text into lines
      final String newline = text.contains("\r\n") ? "\r\n" : "\n";
      final String[] lines = text.split("\r?\n");

      // splitting lines into tokens
      int maxCols = 0;
      int maxTokenLength = 0;
      final List<List<String>> matrix = new ArrayList<>(lines.length);
      int i = 0;
      for (final String item : lines) {
        final String line = item.trim();
        if (line.isEmpty()) {
          continue;
        }

        final String[] tokens = line.split("\\s+");
        if (tokens.length > 0) {
          i++;
          final ArrayList<String> row = new ArrayList<>(tokens.length);
          if (i == 3) {
            row.add("");
          }
          for (final String token : tokens) {
            if (!token.isEmpty()) {
              row.add(token);
            }
          }
          if (i > 2) {
            maxCols = Math.max(maxCols, row.size());
            for (String token : tokens) {
              maxTokenLength = Math.max(maxTokenLength, token.length());
            }
          }
          matrix.add(row);
        }
      }

      // calculating column sizes
      final int[] columns = new int[maxCols];
      for (int col = 0; col < maxCols; col++) {
        int maxLen = 0;
        if (alignPerColumn) {
          for (int row = 2; row < matrix.size(); row++) {
            if (col < matrix.get(row).size()) {
              maxLen = Math.max(maxLen, matrix.get(row).get(col).length());
            }
          }
        } else {
          maxLen = maxTokenLength;
        }
        int len = maxLen + spaces;
        if (len % multipleOf != 0) {
          len += multipleOf - (len % multipleOf);
        }
        columns[col] = len;
      }

      // normalizing data
      final StringBuilder newText = new StringBuilder();
      int blankLen = maxTokenLength + spaces;
      if (blankLen % multipleOf != 0) {
        blankLen += multipleOf - (blankLen % multipleOf);
      }
      String blank = new String(new char[blankLen]).replace('\0', ' ');
      for (int row = 0, rows = matrix.size(); row < rows; row++) {
        StringBuilder sb = new StringBuilder();
        for (int col = 0, cols = matrix.get(row).size(); col < cols; col++) {
          String token = matrix.get(row).get(col);
          sb.append(token);
          if (col < cols - 1) {
            if (row < 2) {
              sb.append(' ');
            } else {
              int end = columns[col] - token.length();
              sb.append((end < blank.length()) ? blank.substring(0, end) : blank);
            }
          }
        }
        newText.append(sb.toString()).append(newline);
      }
      retVal = newText.toString();
    }
    return retVal;
  }

  /**
   * Sorts IDS entries by key values. Special entry at line 1 is excluded.
   *
   * @param text The text content to perform sorting on.
   * @param ascending {@code true} to sort in ascending order, {@code false} to sort in descending order.
   * @param isTrigger {@code true} to ignore bit 14 (0x4000) when comparing is performed.
   * @return Text with sorted IDS entries. Returns {@code null} if {@code text} argument is {@code null}.
   */
  public static String sortTable(String text, boolean ascending, boolean isTrigger) {
    String retVal = null;
    if (text != null) {
      final String newline = text.contains("\r\n") ? "\r\n" : "\n";
      final String[] lines = text.split("\r?\n");

      // dividing lines into fixed entries and (sortable) ids entries
      final List<String> header = new ArrayList<>(); // contains fixed lines to be placed at the top
      final List<String> entries = new ArrayList<>(); // contains ids entries
      for (String line : lines) {
        final String[] items = line.trim().split("\\s+", 2);
        if (items.length < 2 || items[0].equalsIgnoreCase("IDS")) {
          header.add(line);
        } else {
          entries.add(line);
        }
      }
      if (entries.isEmpty()) {
        return text;
      }

      // sorting ids entries
      final Pattern patKey = Pattern.compile("\\s*(\\S+).*");
      entries.sort((c1, c2) -> {
        int radix;
        long v1 = Long.MAX_VALUE, v2 = Long.MAX_VALUE;
        Matcher m;

        m = patKey.matcher(c1);
        if (m.find()) {
          String s = m.groupCount() > 0 ? m.group(1) : "";
          radix = (s.length() > 2 && (s.charAt(1) == 'x' || s.charAt(1) == 'X')) ? 16 : 10;
          if (radix == 16) {
            s = s.substring(2);
          }
          try {
            v1 = Long.parseLong(s, radix);
          } catch (NumberFormatException ex) {
            Logger.trace(ex);
          }
        }

        m = patKey.matcher(c2);
        if (m.find()) {
          String s = m.groupCount() > 0 ? m.group(1) : "";
          radix = (s.length() > 2 && (s.charAt(1) == 'x' || s.charAt(1) == 'X')) ? 16 : 10;
          if (radix == 16) {
            s = s.substring(2);
          }
          try {
            v2 = Long.parseLong(s, radix);
          } catch (NumberFormatException ex) {
            Logger.trace(ex);
          }
        }

        if (isTrigger) {
          v1 &= ~0x4000;
          v2 &= ~0x4000;
        }

        int result = Long.compare(v1, v2);
        if (!ascending) {
          result = -result;
        }
        return result;
      });

      // building output string
      final StringBuilder sb = new StringBuilder();
      for (String s : header) {
        sb.append(s).append(newline);
      }
      for (String s : entries) {
        sb.append(s).append(newline);
      }
      if (text.charAt(text.length() - 1) != '\n') {
        sb.delete(sb.length() - newline.length(), sb.length());
      }

      retVal = sb.toString();
    }
    return retVal;
  }

  public PlainTextResource(ResourceEntry entry) throws Exception {
    this.entry = entry;
    ByteBuffer buffer = entry.getResourceBuffer();
    if (buffer.limit() > 1 && buffer.getShort(0) == -1) {
      buffer = StaticSimpleXorDecryptor.decrypt(buffer, 2);
    }
    final Charset cs = Misc.getCharsetFrom(BrowserMenuBar.getInstance().getOptions().getSelectedCharset());
    text = applyTransformText(StreamUtils.readString(buffer, buffer.limit(), cs));
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (buttonPanel.getControlByType(ButtonPanel.Control.SAVE) == event.getSource()) {
      if (ResourceFactory.saveResource(this, panel.getTopLevelAncestor())) {
        resourceChanged = false;
      }
    } else if (buttonPanel.getControlByType(ButtonPanel.Control.SAVE_AS) == event.getSource()) {
      if (ResourceFactory.saveResourceAs(this, panel.getTopLevelAncestor())) {
        resourceChanged = false;
      }
    } else if (buttonPanel.getControlByType(ButtonPanel.Control.FIND_REFERENCES) == event.getSource()) {
      searchReferences(panel.getTopLevelAncestor());
    } else if (buttonPanel.getControlByType(ButtonPanel.Control.EXPORT_BUTTON) == event.getSource()) {
      ResourceFactory.exportResource(entry, panel.getTopLevelAncestor());
    } else if (buttonPanel.getControlByType(ButtonPanel.Control.TRIM_SPACES) == event.getSource()) {
      setText(trimSpaces(editor.getText(), true, false));
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface Closeable ---------------------

  @Override
  public void close() throws Exception {
    if (resourceChanged) {
      ResourceFactory.closeResource(this, entry, panel);
    }
  }

  // --------------------- End Interface Closeable ---------------------

  // --------------------- Begin Interface Referenceable ---------------------

  @Override
  public boolean isReferenceable() {
    return true;
  }

  @Override
  public void searchReferences(Component parent) {
    new ReferenceSearcher(entry, parent);
  }

  // --------------------- End Interface Referenceable ---------------------

  // --------------------- Begin Interface DocumentListener ---------------------

  @Override
  public void insertUpdate(DocumentEvent event) {
    resourceChanged = true;
  }

  @Override
  public void removeUpdate(DocumentEvent event) {
    resourceChanged = true;
  }

  @Override
  public void changedUpdate(DocumentEvent event) {
    resourceChanged = true;
  }

  // --------------------- End Interface DocumentListener ---------------------

  // --------------------- Begin Interface ItemListener ---------------------

  @Override
  public void itemStateChanged(ItemEvent event) {
    ButtonPopupMenu bpmFind = (ButtonPopupMenu) buttonPanel.getControlByType(ButtonPanel.Control.FIND_MENU);
    ButtonPopupMenu bpmFormat = (ButtonPopupMenu) buttonPanel.getControlByType(ButtonPanel.Control.CUSTOM_1);
    if (event.getSource() == bpmFind) {
      if (bpmFind.getSelectedItem() == iFindAll) {
        final List<ResourceEntry> files = ResourceFactory.getResources(entry.getExtension());
        new TextResourceSearcher(files, panel.getTopLevelAncestor());
      } else if (bpmFind.getSelectedItem() == iFindThis) {
        new TextResourceSearcher(Collections.singletonList(entry), panel.getTopLevelAncestor());
      }
    } else if (event.getSource() == bpmFormat) {
      if (bpmFormat.getSelectedItem() == miFormatTrim) {
        setText(trimSpaces(editor.getText(), true, false));
      } else if (bpmFormat.getSelectedItem() == miFormatAlignCompact) {
        setText(alignTableColumnsCompact(editor.getText()));
      } else if (bpmFormat.getSelectedItem() == miFormatAlignUniform) {
        setText(alignTableColumnsUniform(editor.getText()));
      } else if (bpmFormat.getSelectedItem() == miFormatSort) {
        setText(sortTable(editor.getText(), true, entry.getResourceRef().equalsIgnoreCase("TRIGGER")));
      }
    }
  }

  // --------------------- End Interface ItemListener ---------------------

  // --------------------- Begin Interface Resource ---------------------

  @Override
  public ResourceEntry getResourceEntry() {
    return entry;
  }

  // --------------------- End Interface Resource ---------------------

  // --------------------- Begin Interface TextResource ---------------------

  @Override
  public String getText() {
    return text;
  }

  @Override
  public void highlightText(int linenr, String highlightText) {
    try {
      int startOfs = editor.getLineStartOffset(linenr - 1);
      int endOfs = editor.getLineEndOffset(linenr - 1);
      if (highlightText != null) {
        String text = editor.getText(startOfs, endOfs - startOfs);
        Pattern p = Pattern.compile(highlightText, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
          startOfs += m.start();
          endOfs = startOfs + m.end() - m.start() + 1;
        }
      }
      highlightText(startOfs, endOfs);
    } catch (BadLocationException ble) {
      Logger.trace(ble);
    }
  }

  @Override
  public void highlightText(int startOfs, int endOfs) {
    try {
      editor.setCaretPosition(startOfs);
      editor.moveCaretPosition(endOfs - 1);
      editor.getCaret().setSelectionVisible(true);
    } catch (IllegalArgumentException e) {
      Logger.trace(e);
    }
  }

  // --------------------- End Interface TextResource ---------------------

  // --------------------- Begin Interface Viewable ---------------------

  @Override
  public JComponent makeViewer(ViewableContainer container) {
    editor = new InfinityTextArea(text, true);
    InfinityScrollPane pane = new InfinityScrollPane(editor, true);
    setSyntaxHighlightingEnabled(editor, pane);
    editor.addCaretListener(container.getStatusBar());
    editor.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getOptions().getScriptFont()));
    editor.setMargin(new Insets(3, 3, 3, 3));
    editor.setCaretPosition(0);
    editor.setLineWrap(false);
    editor.getDocument().addDocumentListener(this);

    final String ext = entry.getExtension();
    if ("BIO".equals(ext) || "RES".equals(ext)) {
      editor.setLineWrap(true);
      editor.setWrapStyleWord(true);
    }

    iFindAll = new JMenuItem("in all " + ext + " files");
    iFindThis = new JMenuItem("in this file only");
    ButtonPopupMenu bpmFind = (ButtonPopupMenu) buttonPanel.addControl(ButtonPanel.Control.FIND_MENU);
    bpmFind.setMenuItems(new JMenuItem[] { iFindAll, iFindThis });
    bpmFind.addItemListener(this);
    if ("2DA".equals(ext)) {
      miFormatTrim = new JMenuItem("Trim spaces");
      miFormatAlignCompact = new JMenuItem("Align table (compact)");
      miFormatAlignCompact.setToolTipText("Align table columns to improve readability. Column width is calculated individually.");
      miFormatAlignUniform = new JMenuItem("Align table (uniform)");
      miFormatAlignUniform.setToolTipText(
          "Align table columns to improve readability. Column width is calculated evenly, comparable to WeiDU's PRETTY_PRINT_2DA.");
      ButtonPopupMenu bpmFormat = new ButtonPopupMenu("Format...",
          new JMenuItem[] { miFormatTrim, miFormatAlignCompact, miFormatAlignUniform });
      bpmFormat.addItemListener(this);
      buttonPanel.addControl(bpmFormat, ButtonPanel.Control.CUSTOM_1);
    } else if ("IDS".equals(ext)) {
      miFormatTrim = new JMenuItem("Trim spaces");
      miFormatSort = new JMenuItem("Sort entries");
      miFormatSort.setToolTipText("Sort entries in ascending order. Note: Bit 14 (0x4000) is ignored for TRIGGER.IDS.");
      ButtonPopupMenu bpmFormat = new ButtonPopupMenu("Format...", new JMenuItem[] { miFormatTrim, miFormatSort });
      bpmFormat.addItemListener(this);
      buttonPanel.addControl(bpmFormat, ButtonPanel.Control.CUSTOM_1);
    } else {
      ((JButton) buttonPanel.addControl(ButtonPanel.Control.TRIM_SPACES)).addActionListener(this);
    }
    if ("2DA".equals(ext)) {
      ((JButton) buttonPanel.addControl(ButtonPanel.Control.FIND_REFERENCES)).addActionListener(this);
    }
    ((JButton) buttonPanel.addControl(ButtonPanel.Control.EXPORT_BUTTON)).addActionListener(this);
    ((JButton) buttonPanel.addControl(ButtonPanel.Control.SAVE)).addActionListener(this);
    ((JButton) buttonPanel.addControl(ButtonPanel.Control.SAVE_AS)).addActionListener(this);

    panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(pane, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.SOUTH);

    SwingUtilities.invokeLater(() -> {
      if (highlightedLine >= 0) {
        highlightText(highlightedLine, null);
      }
    });

    return panel;
  }

  // --------------------- End Interface Viewable ---------------------

  // --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException {
    if (editor == null) {
      StreamUtils.writeString(os, text, text.length());
    } else {
      editor.write(new OutputStreamWriter(os));
    }
  }

  // --------------------- End Interface Writeable ---------------------

  public void setHighlightedLine(int highlightedLine) {
    this.highlightedLine = highlightedLine;
    if (panel != null) {
      highlightText(highlightedLine, null);
    }
  }

  public int getHighlightedLine() {
    return highlightedLine;
  }

  public List<String> extract2DAHeaders() {
    StringTokenizer st = new StringTokenizer(getText(), "\n");
    st.nextToken();
    st.nextToken();
    String header = st.nextToken();
    st = new StringTokenizer(header);
    final List<String> strings = new ArrayList<>();
    while (st.hasMoreTokens()) {
      strings.add(st.nextToken().toUpperCase(Locale.ENGLISH));
    }
    return strings;
  }

  /**
   * Sets the text in the editor and places the caret before the first character. Does nothing if input text is equal
   * to the current text content.
   *
   * @param text The text to set.
   */
  public void setText(String text) {
    if (text != null) {
      if (text.compareTo(editor.getText()) != 0) {
        editor.setText(text);
        editor.setCaretPosition(0);
      }
    }
  }

  private void setSyntaxHighlightingEnabled(InfinityTextArea edit, InfinityScrollPane pane) {
    InfinityTextArea.Language language = InfinityTextArea.Language.NONE;
    if (entry != null) {
      if ("SQL".equalsIgnoreCase(entry.getExtension())) {
        if (!BrowserMenuBar.isInstantiated() || BrowserMenuBar.getInstance().getOptions().getSqlSyntaxHighlightingEnabled()) {
          language = InfinityTextArea.Language.SQL;
        }
      } else if ("LUA".equalsIgnoreCase(entry.getExtension())) {
        if (!BrowserMenuBar.isInstantiated() || BrowserMenuBar.getInstance().getOptions().getLuaSyntaxHighlightingEnabled()) {
          language = InfinityTextArea.Language.LUA;
        }
      } else if ("MENU".equalsIgnoreCase(entry.getExtension())) {
        if (!BrowserMenuBar.isInstantiated() || BrowserMenuBar.getInstance().getOptions().getMenuSyntaxHighlightingEnabled()) {
          language = InfinityTextArea.Language.MENU;
        }
      } else if ("INI".equalsIgnoreCase(entry.getExtension())) {
        if (!BrowserMenuBar.isInstantiated() || BrowserMenuBar.getInstance().getOptions().getIniSyntaxHighlightingEnabled()) {
          language = InfinityTextArea.Language.INI;
        }
      } else if (Profile.isEnhancedEdition() && "BALDUR.INI".equalsIgnoreCase(entry.getResourceName())) {
        if (!BrowserMenuBar.isInstantiated() || BrowserMenuBar.getInstance().getOptions().getSqlSyntaxHighlightingEnabled()) {
          language = InfinityTextArea.Language.SQL;
        }
      } else if ("GLSL".equalsIgnoreCase(entry.getExtension())) {
        if (!BrowserMenuBar.isInstantiated() || BrowserMenuBar.getInstance().getOptions().getGlslSyntaxHighlightingEnabled()) {
          language = InfinityTextArea.Language.GLSL;
        }
      } else if ("BCS".equalsIgnoreCase(entry.getExtension()) || "BS".equalsIgnoreCase(entry.getExtension())
          || "BAF".equalsIgnoreCase(entry.getExtension())) {
        if (!BrowserMenuBar.isInstantiated() || BrowserMenuBar.getInstance().getOptions().getBcsSyntaxHighlightingEnabled()) {
          language = InfinityTextArea.Language.BCS;
        }
      } else if ("WeiDU.log".equalsIgnoreCase(entry.getResourceName())
          || "WeiDU-BGEE.log".equalsIgnoreCase(entry.getResourceName())) {
        if (!BrowserMenuBar.isInstantiated() || BrowserMenuBar.getInstance().getOptions().getWeiDUSyntaxHighlightingEnabled()) {
          language = InfinityTextArea.Language.WEIDU;
        }
      }
    }
    if (edit != null) {
      edit.applyExtendedSettings(language, null);
      edit.setFont(Misc.getScaledFont(edit.getFont()));
    }
    if (pane != null) {
      pane.applyExtendedSettings(language);
    }
  }

  private String applyTransformText(String data) {
    if (data == null) {
      return data;
    }

    final String ext = (entry != null) ? entry.getExtension() : "";
    if (ext.equals("2DA")) {
      return applyAutoAlign2da(data);
    }

    return data;
  }

  private String applyAutoAlign2da(String data) {
    switch (BrowserMenuBar.getInstance().getOptions().getAutoAlign2da()) {
      case COMPACT:
        return alignTableColumnsCompact(data);
      case UNIFORM:
        return alignTableColumnsUniform(data);
      default:
    }

    return data;
  }

}
