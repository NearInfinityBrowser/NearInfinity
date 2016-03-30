// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.text;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.ButtonPanel;
import org.infinity.gui.ButtonPopupMenu;
import org.infinity.gui.InfinityScrollPane;
import org.infinity.gui.InfinityTextArea;
import org.infinity.resource.Closeable;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.TextResource;
import org.infinity.resource.ViewableContainer;
import org.infinity.resource.Writeable;
import org.infinity.resource.key.BIFFResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.TextResourceSearcher;
import org.infinity.util.Decryptor;
import org.infinity.util.io.FileManager;
import org.infinity.util.io.StreamUtils;

public final class PlainTextResource implements TextResource, Writeable, ActionListener, ItemListener,
                                                DocumentListener, Closeable
{
  private final ResourceEntry entry;
  private final String text;
  private final ButtonPanel buttonPanel = new ButtonPanel();

  private JMenuItem ifindall, ifindthis;
  private JPanel panel;
  private InfinityTextArea editor;
  private boolean resourceChanged;
  private int highlightedLine;

  public PlainTextResource(ResourceEntry entry) throws Exception
  {
    this(entry, -1);
  }

  public PlainTextResource(ResourceEntry entry, int highlightedLine) throws Exception
  {
    this.entry = entry;
    ByteBuffer buffer = entry.getResourceBuffer();
    if (buffer.limit() > 1 && buffer.getShort(0) == -1) {
      buffer = Decryptor.decrypt(buffer, 2);
    }
    text = StreamUtils.readString(buffer, buffer.limit());
    this.highlightedLine = highlightedLine;
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (buttonPanel.getControlByType(ButtonPanel.Control.SAVE) == event.getSource()) {
      if (ResourceFactory.saveResource(this, panel.getTopLevelAncestor()))
        resourceChanged = false;
    } else if (buttonPanel.getControlByType(ButtonPanel.Control.EXPORT_BUTTON) == event.getSource()) {
      ResourceFactory.exportResource(entry, panel.getTopLevelAncestor());
    } else if (buttonPanel.getControlByType(ButtonPanel.Control.TRIM_SPACES) == event.getSource()) {
      StringBuffer newText = new StringBuffer(editor.getText().length());
      StringTokenizer st = new StringTokenizer(editor.getText(), "\n");
      while (st.hasMoreTokens()) {
        newText.append(st.nextToken().trim()).append('\n');
      }
      editor.setText(newText.toString());
      editor.setCaretPosition(0);
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Closeable ---------------------

  @Override
  public void close() throws Exception
  {
    if (resourceChanged) {
      Path output;
      if (entry instanceof BIFFResourceEntry) {
        output = FileManager.query(Profile.getGameRoot(), Profile.getOverrideFolderName(), entry.toString());
      } else {
        output = entry.getActualPath();
      }
      String options[] = {"Save changes", "Discard changes", "Cancel"};
      int result = JOptionPane.showOptionDialog(panel, "Save changes to " + output + '?', "Resource changed",
                                                JOptionPane.YES_NO_CANCEL_OPTION,
                                                JOptionPane.WARNING_MESSAGE, null, options, options[0]);
      if (result == 0) {
        ResourceFactory.saveResource(this, panel.getTopLevelAncestor());
      } else if (result != 1) {
        throw new Exception("Save aborted");
      }
    }
  }

// --------------------- End Interface Closeable ---------------------


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
    if (event.getSource() == bpmFind) {
      if (bpmFind.getSelectedItem() == ifindall) {
        String type = entry.toString().substring(entry.toString().indexOf(".") + 1);
        List<ResourceEntry> files = ResourceFactory.getResources(type);
        new TextResourceSearcher(files, panel.getTopLevelAncestor());
      } else if (bpmFind.getSelectedItem() == ifindthis) {
        List<ResourceEntry> files = new ArrayList<ResourceEntry>();
        files.add(entry);
        new TextResourceSearcher(files, panel.getTopLevelAncestor());
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
  public void highlightText(int linenr, String text)
  {
    String s = editor.getText();
    int startpos = 0;
    int i = (s.charAt(0) == '\n') ? 2 : 1;
    for (; i < linenr; i++) {
      startpos = s.indexOf("\n", startpos + 1);
    }
    if (startpos == -1) return;
    if (text != null) {
      // try to select specified text string
      int wordpos = s.toUpperCase(Locale.ENGLISH).indexOf(text.toUpperCase(Locale.ENGLISH), startpos);
      if (wordpos != -1) {
        editor.select(wordpos, wordpos + text.length());
      } else {
        editor.select(startpos, s.indexOf("\n", startpos + 1));
      }
    } else {
      // select whole line
      int endpos = s.indexOf("\n", startpos + 1);
      if (endpos < 0) {
        endpos = s.length();
      }
      editor.select(startpos, endpos);
    }
    editor.getCaret().setSelectionVisible(true);
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
    editor.setFont(BrowserMenuBar.getInstance().getScriptFont());
    editor.setMargin(new Insets(3, 3, 3, 3));
    editor.setCaretPosition(0);
    editor.setLineWrap(false);
    editor.getDocument().addDocumentListener(this);
    if (entry.toString().toUpperCase(Locale.ENGLISH).endsWith(".BIO") ||
        entry.toString().toUpperCase(Locale.ENGLISH).endsWith(".RES")) {
      editor.setLineWrap(true);
      editor.setWrapStyleWord(true);
    }

    ifindall =
        new JMenuItem("in all " + entry.toString().substring(entry.toString().indexOf(".") + 1) + " files");
    ifindthis = new JMenuItem("in this file only");
    ButtonPopupMenu bpmFind = (ButtonPopupMenu)buttonPanel.addControl(ButtonPanel.Control.FIND_MENU);
    bpmFind.setMenuItems(new JMenuItem[]{ifindall, ifindthis});
    bpmFind.addItemListener(this);
    ((JButton)buttonPanel.addControl(ButtonPanel.Control.TRIM_SPACES)).addActionListener(this);
    ((JButton)buttonPanel.addControl(ButtonPanel.Control.EXPORT_BUTTON)).addActionListener(this);
    ((JButton)buttonPanel.addControl(ButtonPanel.Control.SAVE)).addActionListener(this);

    panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(pane, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.SOUTH);

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run()
      {
        if (highlightedLine >= 0) {
          highlightText(highlightedLine, null);
        }
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
    List<String> strings = new ArrayList<String>();
    while (st.hasMoreTokens())
      strings.add(st.nextToken().toUpperCase(Locale.ENGLISH));
    return strings;
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
      }
    }
    if (edit != null) {
      edit.applyExtendedSettings(language, null);
    }
    if (pane != null) {
      pane.applyExtendedSettings(language);
    }
  }
}

