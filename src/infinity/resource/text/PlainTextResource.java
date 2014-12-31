// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.text;

import infinity.gui.BrowserMenuBar;
import infinity.gui.ButtonPanel;
import infinity.gui.ButtonPopupMenu;
import infinity.gui.InfinityScrollPane;
import infinity.gui.InfinityTextArea;
import infinity.resource.Closeable;
import infinity.resource.ResourceFactory;
import infinity.resource.TextResource;
import infinity.resource.ViewableContainer;
import infinity.resource.Writeable;
import infinity.resource.key.BIFFResourceEntry;
import infinity.resource.key.ResourceEntry;
import infinity.search.TextResourceSearcher;
import infinity.util.Decryptor;
import infinity.util.io.FileNI;
import infinity.util.io.FileWriterNI;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

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

  public PlainTextResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    byte data[] = entry.getResourceData();
    if (data != null && data.length > 1 && data[0] == -1)
      text = Decryptor.decrypt(data, 2, data.length);
    else
      text = new String(data);
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (buttonPanel.getControlByType(ButtonPanel.Control.Save) == event.getSource()) {
      if (ResourceFactory.getInstance().saveResource(this, panel.getTopLevelAncestor()))
        resourceChanged = false;
    } else if (buttonPanel.getControlByType(ButtonPanel.Control.ExportButton) == event.getSource()) {
      ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
    } else if (buttonPanel.getControlByType(ButtonPanel.Control.TrimSpaces) == event.getSource()) {
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
      File output;
      if (entry instanceof BIFFResourceEntry)
        output =
            FileNI.getFile(ResourceFactory.getRootDirs(),
                 ResourceFactory.OVERRIDEFOLDER + File.separatorChar + entry.toString());
      else
        output = entry.getActualFile();
      String options[] = {"Save changes", "Discard changes", "Cancel"};
      int result = JOptionPane.showOptionDialog(panel, "Save changes to " + output + '?', "Resource changed",
                                                JOptionPane.YES_NO_CANCEL_OPTION,
                                                JOptionPane.WARNING_MESSAGE, null, options, options[0]);
      if (result == 0)
        ResourceFactory.getInstance().saveResource(this, panel.getTopLevelAncestor());
      else if (result != 1)
        throw new Exception("Save aborted");
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
    ButtonPopupMenu bpmFind = (ButtonPopupMenu)buttonPanel.getControlByType(ButtonPanel.Control.FindMenu);
    if (event.getSource() == bpmFind) {
      if (bpmFind.getSelectedItem() == ifindall) {
        String type = entry.toString().substring(entry.toString().indexOf(".") + 1);
        List<ResourceEntry> files = ResourceFactory.getInstance().getResources(type);
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
    for (int i = 1; i < linenr; i++)
      startpos = s.indexOf("\n", startpos + 1);
    if (startpos == -1) return;
    int wordpos = s.toUpperCase().indexOf(text.toUpperCase(), startpos);
    if (wordpos != -1)
      editor.select(wordpos, wordpos + text.length());
    else
      editor.select(startpos, s.indexOf("\n", startpos + 1));
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
    if (entry.toString().toUpperCase().endsWith(".BIO") || entry.toString().toUpperCase().endsWith(".RES")) {
      editor.setLineWrap(true);
      editor.setWrapStyleWord(true);
    }

    ifindall =
        new JMenuItem("in all " + entry.toString().substring(entry.toString().indexOf(".") + 1) + " files");
    ifindthis = new JMenuItem("in this file only");
    ButtonPopupMenu bpmFind = (ButtonPopupMenu)buttonPanel.addControl(ButtonPanel.Control.FindMenu);
    bpmFind.setMenuItems(new JMenuItem[]{ifindall, ifindthis});
    bpmFind.addItemListener(this);
    ((JButton)buttonPanel.addControl(ButtonPanel.Control.TrimSpaces)).addActionListener(this);
    ((JButton)buttonPanel.addControl(ButtonPanel.Control.ExportButton)).addActionListener(this);
    ((JButton)buttonPanel.addControl(ButtonPanel.Control.Save)).addActionListener(this);

    panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(pane, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.SOUTH);

    return panel;
  }

// --------------------- End Interface Viewable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    if (editor == null) {
      FileWriterNI.writeString(os, text, text.length());
    } else {
      editor.write(new OutputStreamWriter(os));
    }
  }

// --------------------- End Interface Writeable ---------------------

  public List<String> extract2DAHeaders()
  {
    StringTokenizer st = new StringTokenizer(getText(), "\n");
    st.nextToken();
    st.nextToken();
    String header = st.nextToken();
    st = new StringTokenizer(header);
    List<String> strings = new ArrayList<String>();
    while (st.hasMoreTokens())
      strings.add(st.nextToken().toUpperCase());
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

