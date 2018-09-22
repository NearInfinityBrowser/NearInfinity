// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.mus;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.ButtonPanel;
import org.infinity.gui.ButtonPopupMenu;
import org.infinity.gui.InfinityScrollPane;
import org.infinity.gui.InfinityTextArea;
import org.infinity.gui.WindowBlocker;
import org.infinity.resource.Closeable;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.TextResource;
import org.infinity.resource.ViewableContainer;
import org.infinity.resource.Writeable;
import org.infinity.resource.key.BIFFResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.SongReferenceSearcher;
import org.infinity.search.TextResourceSearcher;
import org.infinity.util.Misc;
import org.infinity.util.io.FileManager;
import org.infinity.util.io.StreamUtils;

public final class MusResource implements Closeable, TextResource, ActionListener, Writeable, ItemListener,
                                          DocumentListener
{
  private static int lastIndex = -1;
  private final ResourceEntry entry;
  private final String text;
  private final ButtonPanel buttonPanel = new ButtonPanel();

  private JTabbedPane tabbedPane;
  private JMenuItem ifindall, ifindthis, ifindreference;
  private JPanel panel;
  private InfinityTextArea editor;
  private Viewer viewer;
  private boolean resourceChanged;

  public MusResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    ByteBuffer buffer = entry.getResourceBuffer();
    text = StreamUtils.readString(buffer, buffer.limit());
    resourceChanged = false;
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (buttonPanel.getControlByType(ButtonPanel.Control.SAVE) == event.getSource()) {
      if (ResourceFactory.saveResource(this, panel.getTopLevelAncestor())) {
        setDocumentModified(false);
      }
      viewer.loadMusResource(this);
    }
    else if (buttonPanel.getControlByType(ButtonPanel.Control.EXPORT_BUTTON) == event.getSource()) {
      ResourceFactory.exportResource(entry, panel.getTopLevelAncestor());
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Closeable ---------------------

  @Override
  public void close() throws Exception
  {
    lastIndex = tabbedPane.getSelectedIndex();
    if (resourceChanged) {
      ResourceFactory.closeResource(this, entry, panel);
    }
    if (viewer != null) {
      viewer.close();
    }
  }

// --------------------- End Interface Closeable ---------------------


// --------------------- Begin Interface DocumentListener ---------------------

  @Override
  public void insertUpdate(DocumentEvent event)
  {
    setDocumentModified(true);
  }

  @Override
  public void removeUpdate(DocumentEvent event)
  {
    setDocumentModified(true);
  }

  @Override
  public void changedUpdate(DocumentEvent event)
  {
    setDocumentModified(true);
  }

// --------------------- End Interface DocumentListener ---------------------


// --------------------- Begin Interface ItemListener ---------------------

  @Override
  public void itemStateChanged(ItemEvent event)
  {
    if (buttonPanel.getControlByType(ButtonPanel.Control.FIND_MENU) == event.getSource()) {
      ButtonPopupMenu bpmFind = (ButtonPopupMenu)event.getSource();
      if (bpmFind.getSelectedItem() == ifindall) {
        String type = entry.toString().substring(entry.toString().indexOf(".") + 1);
        List<ResourceEntry> files = ResourceFactory.getResources(type);
        new TextResourceSearcher(files, panel.getTopLevelAncestor());
      } else if (bpmFind.getSelectedItem() == ifindthis) {
        List<ResourceEntry> files = new ArrayList<ResourceEntry>();
        files.add(entry);
        new TextResourceSearcher(files, panel.getTopLevelAncestor());
      } else if (bpmFind.getSelectedItem() == ifindreference) {
        new SongReferenceSearcher(entry, panel.getTopLevelAncestor());
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
    if (editor == null)
      return text;
    else
      return editor.getText();
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
    panel = new JPanel(new BorderLayout());
    try {
      WindowBlocker.blockWindow(true);
      viewer = new Viewer(this);
      tabbedPane = new JTabbedPane();
      tabbedPane.addTab("View", viewer);
      tabbedPane.addTab("Edit", getEditor(container.getStatusBar()));
      panel.add(tabbedPane, BorderLayout.CENTER);
      if (lastIndex != -1) {
        tabbedPane.setSelectedIndex(lastIndex);
      } else if (BrowserMenuBar.getInstance().getDefaultStructView() == BrowserMenuBar.DEFAULT_EDIT) {
        tabbedPane.setSelectedIndex(1);
      }
      WindowBlocker.blockWindow(false);
    } catch (Exception e) {
      WindowBlocker.blockWindow(false);
    }
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
      StreamUtils.writeString(os, editor.getText(), editor.getText().length());
    }
  }

// --------------------- End Interface Writeable ---------------------

  public Viewer getViewer()
  {
    return viewer;
  }

  private JComponent getEditor(CaretListener caretListener)
  {
    ifindall =
        new JMenuItem("in all " + entry.toString().substring(entry.toString().indexOf(".") + 1) + " files");
    ifindthis = new JMenuItem("in this file only");
    ifindreference = new JMenuItem("references to this file");
    ButtonPopupMenu bpmFind = (ButtonPopupMenu)buttonPanel.addControl(ButtonPanel.Control.FIND_MENU);
    bpmFind.setMenuItems(new JMenuItem[]{ifindall, ifindthis, ifindreference});
    bpmFind.addItemListener(this);
    editor = new InfinityTextArea(text, true);
    editor.discardAllEdits();
    editor.addCaretListener(caretListener);
    editor.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getScriptFont()));
    editor.setMargin(new Insets(3, 3, 3, 3));
    editor.setCaretPosition(0);
    editor.setLineWrap(false);
    editor.getDocument().addDocumentListener(this);
    ((JButton)buttonPanel.addControl(ButtonPanel.Control.EXPORT_BUTTON)).addActionListener(this);
    JButton bSave = (JButton)buttonPanel.addControl(ButtonPanel.Control.SAVE);
    bSave.addActionListener(this);
    bSave.setEnabled(getDocumentModified());

    JPanel lowerpanel = new JPanel();
    lowerpanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    lowerpanel.add(buttonPanel);

    JPanel panel2 = new JPanel();
    panel2.setLayout(new BorderLayout());
    panel2.add(new InfinityScrollPane(editor, true), BorderLayout.CENTER);
    panel2.add(lowerpanel, BorderLayout.SOUTH);

    return panel2;
  }

  private boolean getDocumentModified()
  {
    return resourceChanged;
  }

  private void setDocumentModified(boolean b)
  {
    if (b != resourceChanged) {
      resourceChanged = b;
      buttonPanel.getControlByType(ButtonPanel.Control.SAVE).setEnabled(resourceChanged);
    }
  }
}

