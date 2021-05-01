// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
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
import java.util.Arrays;
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

import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.ButtonPanel;
import org.infinity.gui.ButtonPopupMenu;
import org.infinity.gui.InfinityScrollPane;
import org.infinity.gui.InfinityTextArea;
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
import org.infinity.util.StaticSimpleXorDecryptor;
import org.infinity.util.IdsMap;
import org.infinity.util.IdsMapCache;
import org.infinity.util.IdsMapEntry;
import org.infinity.util.Misc;
import org.infinity.util.io.StreamUtils;

public class PlainTextResource implements TextResource, Writeable, ActionListener, ItemListener,
                                          DocumentListener, Closeable, Referenceable
{
  private final ResourceEntry entry;
  protected final String text;
  private final ButtonPanel buttonPanel = new ButtonPanel();

  private JMenuItem ifindall, ifindthis;
  private JMenuItem miFormatTrim, miFormatAlign, miFormatSort;
  private JPanel panel;
  /** Text editor for editing resource. Created after calling {@link #makeViewer}. */
  protected InfinityTextArea editor;
  private boolean resourceChanged;
  private int highlightedLine = -1;

  /** Returns description for INI resources linked to ARE resources or listed in ANIMATE.IDS. */
  public static String getSearchString(ResourceEntry entry)
  {
    String retVal = null;
    if (entry != null && "INI".equalsIgnoreCase(entry.getExtension())) {
      try {
        // try animation id first
        int animId = Integer.parseInt(entry.getResourceRef(), 16);
        if (animId >= 0 && animId <= 0xffff) {
          IdsMap idsMap = IdsMapCache.get("ANIMATE.IDS");
          if (idsMap != null) {
            IdsMapEntry idsEntry = idsMap.get(animId);
            if (idsEntry != null)
              retVal = idsEntry.getSymbol();
          }
        }
      } catch (NumberFormatException e) {
      }

      if (retVal == null) {
        ResourceEntry areEntry = ResourceFactory.getResourceEntry(entry.getResourceRef() + ".ARE");
        if (areEntry != null)
          retVal = AreResource.getSearchString(areEntry);
      }
    }
    return retVal;
  }

  public PlainTextResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    ByteBuffer buffer = entry.getResourceBuffer();
    if (buffer.limit() > 1 && buffer.getShort(0) == -1) {
      buffer = StaticSimpleXorDecryptor.decrypt(buffer, 2);
    }
    final Charset cs;
    if (BrowserMenuBar.getInstance() != null) {
      cs = Charset.forName(BrowserMenuBar.getInstance().getSelectedCharset());
    } else {
      cs = Misc.CHARSET_DEFAULT;
    }
    text = StreamUtils.readString(buffer, buffer.limit(), cs);
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (buttonPanel.getControlByType(ButtonPanel.Control.SAVE) == event.getSource()) {
      if (ResourceFactory.saveResource(this, panel.getTopLevelAncestor()))
        resourceChanged = false;
    } else if (buttonPanel.getControlByType(ButtonPanel.Control.FIND_REFERENCES) == event.getSource()) {
      searchReferences(panel.getTopLevelAncestor());
    } else if (buttonPanel.getControlByType(ButtonPanel.Control.EXPORT_BUTTON) == event.getSource()) {
      ResourceFactory.exportResource(entry, panel.getTopLevelAncestor());
    } else if (buttonPanel.getControlByType(ButtonPanel.Control.TRIM_SPACES) == event.getSource()) {
      trimSpaces();
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Closeable ---------------------

  @Override
  public void close() throws Exception
  {
    if (resourceChanged) {
      ResourceFactory.closeResource(this, entry, panel);
    }
  }

// --------------------- End Interface Closeable ---------------------

//--------------------- Begin Interface Referenceable ---------------------

  @Override
  public boolean isReferenceable()
  {
    return true;
  }

  @Override
  public void searchReferences(Component parent)
  {
    new ReferenceSearcher(entry, parent);
  }

//--------------------- End Interface Referenceable ---------------------

// --------------------- Begin Interface DocumentListener ---------------------

  @Override
  public void insertUpdate(DocumentEvent event)
  {
    resourceChanged = true;
  }

  @Override
  public void removeUpdate(DocumentEvent event)
  {
    resourceChanged = true;
  }

  @Override
  public void changedUpdate(DocumentEvent event)
  {
    resourceChanged = true;
  }

// --------------------- End Interface DocumentListener ---------------------


// --------------------- Begin Interface ItemListener ---------------------

  @Override
  public void itemStateChanged(ItemEvent event)
  {
    ButtonPopupMenu bpmFind = (ButtonPopupMenu)buttonPanel.getControlByType(ButtonPanel.Control.FIND_MENU);
    ButtonPopupMenu bpmFormat = (ButtonPopupMenu)buttonPanel.getControlByType(ButtonPanel.Control.CUSTOM_1);
    if (event.getSource() == bpmFind) {
      if (bpmFind.getSelectedItem() == ifindall) {
        final List<ResourceEntry> files = ResourceFactory.getResources(entry.getExtension());
        new TextResourceSearcher(files, panel.getTopLevelAncestor());
      } else if (bpmFind.getSelectedItem() == ifindthis) {
        new TextResourceSearcher(Arrays.asList(entry), panel.getTopLevelAncestor());
      }
    } else if (event.getSource() == bpmFormat) {
      if (bpmFormat.getSelectedItem() == miFormatTrim) {
        trimSpaces();
      } else if (bpmFormat.getSelectedItem() == miFormatAlign) {
        alignTableColumns(2, true, 4);
      } else if (bpmFormat.getSelectedItem() == miFormatSort) {
        sortTable(true);
      }
    }
  }

// --------------------- End Interface ItemListener ---------------------


// --------------------- Begin Interface Resource ---------------------

  @Override
  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

// --------------------- End Interface Resource ---------------------


// --------------------- Begin Interface TextResource ---------------------

  @Override
  public String getText()
  {
    return text;
  }

  @Override
  public void highlightText(int linenr, String highlightText)
  {
    try {
      int startOfs = editor.getLineStartOffset(linenr - 1);
      int endOfs = editor.getLineEndOffset(linenr - 1);
      if (highlightText != null) {
        String text = editor.getText(startOfs, endOfs - startOfs);
        Pattern p = Pattern.compile(highlightText, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
          startOfs += m.start();
          endOfs = startOfs + m.end() + 1;
        }
      }
      highlightText(startOfs, endOfs);
    } catch (BadLocationException ble) {
    }
  }

  @Override
  public void highlightText(int startOfs, int endOfs)
  {
    try {
      editor.setCaretPosition(startOfs);
      editor.moveCaretPosition(endOfs - 1);
      editor.getCaret().setSelectionVisible(true);
    } catch (IllegalArgumentException e) {
    }
  }

// --------------------- End Interface TextResource ---------------------


// --------------------- Begin Interface Viewable ---------------------

  @Override
  public JComponent makeViewer(ViewableContainer container)
  {
    editor = new InfinityTextArea(text, true);
    InfinityScrollPane pane = new InfinityScrollPane(editor, true);
    setSyntaxHighlightingEnabled(editor, pane);
    editor.addCaretListener(container.getStatusBar());
    editor.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getScriptFont()));
    editor.setMargin(new Insets(3, 3, 3, 3));
    editor.setCaretPosition(0);
    editor.setLineWrap(false);
    editor.getDocument().addDocumentListener(this);

    final String ext = entry.getExtension();
    if ("BIO".equals(ext) || "RES".equals(ext)) {
      editor.setLineWrap(true);
      editor.setWrapStyleWord(true);
    }

    ifindall  = new JMenuItem("in all " + ext + " files");
    ifindthis = new JMenuItem("in this file only");
    ButtonPopupMenu bpmFind = (ButtonPopupMenu)buttonPanel.addControl(ButtonPanel.Control.FIND_MENU);
    bpmFind.setMenuItems(new JMenuItem[]{ifindall, ifindthis});
    bpmFind.addItemListener(this);
    if ("2DA".equals(ext)) {
      miFormatTrim = new JMenuItem("Trim spaces");
      miFormatAlign = new JMenuItem("Align table");
      miFormatAlign.setToolTipText("Align table columns to improve readability.");
      ButtonPopupMenu bpmFormat = new ButtonPopupMenu("Format...", new JMenuItem[]{miFormatTrim, miFormatAlign});
      bpmFormat.addItemListener(this);
      buttonPanel.addControl(bpmFormat, ButtonPanel.Control.CUSTOM_1);
    } else if ("IDS".equals(ext)) {
      miFormatTrim = new JMenuItem("Trim spaces");
      miFormatSort = new JMenuItem("Sort entries");
      miFormatSort.setToolTipText("Sort entries in ascending order.");
      ButtonPopupMenu bpmFormat = new ButtonPopupMenu("Format...", new JMenuItem[]{miFormatTrim, miFormatSort});
      bpmFormat.addItemListener(this);
      buttonPanel.addControl(bpmFormat, ButtonPanel.Control.CUSTOM_1);
    } else {
      ((JButton)buttonPanel.addControl(ButtonPanel.Control.TRIM_SPACES)).addActionListener(this);
    }
    if ("2DA".equals(ext)) {
      ((JButton)buttonPanel.addControl(ButtonPanel.Control.FIND_REFERENCES)).addActionListener(this);
    }
    ((JButton)buttonPanel.addControl(ButtonPanel.Control.EXPORT_BUTTON)).addActionListener(this);
    ((JButton)buttonPanel.addControl(ButtonPanel.Control.SAVE)).addActionListener(this);

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
  public void write(OutputStream os) throws IOException
  {
    if (editor == null) {
      StreamUtils.writeString(os, text, text.length());
    } else {
      editor.write(new OutputStreamWriter(os));
    }
  }

// --------------------- End Interface Writeable ---------------------

  public void setHighlightedLine(int highlightedLine)
  {
    this.highlightedLine = highlightedLine;
    if (panel != null) {
      highlightText(highlightedLine, null);
    }
  }

  public int getHighlightedLine()
  {
    return highlightedLine;
  }

  public List<String> extract2DAHeaders()
  {
    StringTokenizer st = new StringTokenizer(getText(), "\n");
    st.nextToken();
    st.nextToken();
    String header = st.nextToken();
    st = new StringTokenizer(header);
    final List<String> strings = new ArrayList<>();
    while (st.hasMoreTokens())
      strings.add(st.nextToken().toUpperCase(Locale.ENGLISH));
    return strings;
  }

  /**
   * Removes trailing whitespace from every line of the text. Ensures that text ends with a newline.
   */
  public void trimSpaces()
  {
    String input = editor.getText();
    String[] lines = input.split("\n");
    StringBuilder newText = new StringBuilder();
    for (int i = 0; i < lines.length; i++)
      newText.append(Misc.trimEnd(lines[i])).append('\n');
    String output = newText.toString();

    if (input.compareTo(output) != 0) {
      editor.setText(output);
      editor.setCaretPosition(0);
    }
  }

  /**
   * Aligns table columns to improve readability.
   * @param spaces Min. number of spaces between columns.
   * @param alignPerColumn specify {@code true} to calculate max width on a per column basis,
   *                       or {@code false} to calculate for the whole table.
   * @param multipleOf ensures that column position is always a multiple of the specified value.
   *                   (e.g. specify 2 to have every column start at an even horizontal position.)
   */
  public void alignTableColumns(int spaces, boolean alignPerColumn, int multipleOf)
  {
    spaces = Math.max(1, spaces);
    multipleOf = Math.max(1, multipleOf);

    // splitting text into lines
    String input = editor.getText();
    String[] lines = input.split("\n");

    // splitting lines into tokens
    int maxCols = 0;
    int maxTokenLength = 0;
    List<List<String>> matrix = new ArrayList<>(lines.length);
    for (int i = 0; i < lines.length; i++) {
      String[] tokens = lines[i].split("\\s+");
      if (tokens.length > 0) {
        matrix.add(new ArrayList<>(tokens.length));
        if (matrix.size() == 3) matrix.get(matrix.size() - 1).add("");
        for (int j = 0; j < tokens.length; j++) {
          if (!tokens[j].isEmpty())
            matrix.get(i).add(tokens[j]);
        }
        if (matrix.size() > 2) {
          maxCols = Math.max(maxCols, matrix.get(matrix.size() - 1).size());
          for (int j = 0; j < tokens.length; j++)
            maxTokenLength = Math.max(maxTokenLength, tokens[j].length());
        }
      }
    }

    // calculating column sizes
    int[] columns = new int[maxCols];
    for (int col = 0; col < maxCols; col++) {
      int maxLen = 0;
      if (alignPerColumn) {
        for (int row = 2; row < matrix.size(); row++) {
          if (col < matrix.get(row).size())
            maxLen = Math.max(maxLen, matrix.get(row).get(col).length());
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
    StringBuilder newText = new StringBuilder();
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
      newText.append(sb.toString()).append('\n');
    }
    String output = newText.toString();

    if (input.compareTo(output) != 0) {
      editor.setText(newText.toString());
      editor.setCaretPosition(0);
    }
  }

  /**
   * Sorts IDS entries by key values. Special entry at line 1 is excluded.
   * @param ascending {@code true} to sort in ascending order, {@code false} to sort in descending order.
   */
  public void sortTable(boolean ascending)
  {
    String input = editor.getText();
    String[] lines = input.split("\n");

    // dividing lines into fixed entries and (sortable) ids entries
    List<String> header = new ArrayList<>();  // contains fixed lines to be placed at the top
    List<String> entries = new ArrayList<>(); // contains ids entries
    for (int i = 0, cnt = lines.length; i < cnt; i++) {
      String[] items = lines[i].trim().split("\\s+", 2);
      if (items.length < 2 || items[0].equalsIgnoreCase("IDS")) {
        header.add(lines[i]);
      } else {
        entries.add(lines[i]);
      }
    }
    if (entries.isEmpty())
      return;

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
        if (radix == 16)
          s = s.substring(2);
        try { v1 = Long.parseLong(s, radix); } catch (NumberFormatException ex) { }
      }

      m = patKey.matcher(c2);
      if (m.find()) {
        String s = m.groupCount() > 0 ? m.group(1) : "";
        radix = (s.length() > 2 && (s.charAt(1) == 'x' || s.charAt(1) == 'X')) ? 16 : 10;
        if (radix == 16)
          s = s.substring(2);
        try { v2 = Long.parseLong(s, radix); } catch (NumberFormatException ex) { }
      }

      int retVal = (v1 < v2) ? -1 : ((v1 > v2) ? 1 : 0);
      if (!ascending)
        retVal = -retVal;
      return retVal;
    });

    // building output string
    StringBuilder sb = new StringBuilder();
    for (String s : header)
      sb.append(s).append('\n');
    for (String s : entries)
      sb.append(s).append('\n');
    if (input.charAt(input.length() - 1) != '\n')
      sb.deleteCharAt(sb.length() - 1);

    String output = sb.toString();
    if (!input.equals(output)) {
      editor.setText(output);
      editor.setCaretPosition(0);
    }
  }

  private void setSyntaxHighlightingEnabled(InfinityTextArea edit, InfinityScrollPane pane)
  {
    InfinityTextArea.Language language = InfinityTextArea.Language.NONE;
    if (entry != null) {
      if ("SQL".equalsIgnoreCase(entry.getExtension())) {
        if (BrowserMenuBar.getInstance() == null ||
            BrowserMenuBar.getInstance().getSqlSyntaxHighlightingEnabled()) {
          language = InfinityTextArea.Language.SQL;
        }
      } else if ("LUA".equalsIgnoreCase(entry.getExtension())) {
        if (BrowserMenuBar.getInstance() == null ||
            BrowserMenuBar.getInstance().getLuaSyntaxHighlightingEnabled()) {
          language = InfinityTextArea.Language.LUA;
        }
      } else if (Profile.isEnhancedEdition() && "BALDUR.INI".equalsIgnoreCase(entry.getResourceName())) {
        if (BrowserMenuBar.getInstance() == null ||
            BrowserMenuBar.getInstance().getSqlSyntaxHighlightingEnabled()) {
          language = InfinityTextArea.Language.SQL;
        }
      } else if ("GLSL".equalsIgnoreCase(entry.getExtension())) {
        if (BrowserMenuBar.getInstance() == null ||
            BrowserMenuBar.getInstance().getGlslSyntaxHighlightingEnabled()) {
          language = InfinityTextArea.Language.GLSL;
        }
      } else if ("BCS".equalsIgnoreCase(entry.getExtension()) ||
                 "BS".equalsIgnoreCase(entry.getExtension()) ||
                 "BAF".equalsIgnoreCase(entry.getExtension())) {
        if (BrowserMenuBar.getInstance() == null ||
            BrowserMenuBar.getInstance().getBcsSyntaxHighlightingEnabled()) {
          language = InfinityTextArea.Language.BCS;
        }
      } else if ("WeiDU.log".equalsIgnoreCase(entry.getResourceName()) ||
                 "WeiDU-BGEE.log".equalsIgnoreCase(entry.getResourceName())) {
        if (BrowserMenuBar.getInstance() == null ||
            BrowserMenuBar.getInstance().getWeiDUSyntaxHighlightingEnabled()) {
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
}
