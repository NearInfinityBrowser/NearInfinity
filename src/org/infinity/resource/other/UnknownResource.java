// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.other;

import tv.porst.jhexview.DataChangedEvent;
import tv.porst.jhexview.IDataChangedListener;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.infinity.NearInfinity;
import org.infinity.gui.ButtonPanel;
import org.infinity.gui.InfinityTextArea;
import org.infinity.gui.ViewerUtil;
import org.infinity.gui.WindowBlocker;
import org.infinity.gui.hexview.GenericHexViewer;
import org.infinity.resource.Closeable;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.ViewableContainer;
import org.infinity.resource.Writeable;
import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Misc;
import org.infinity.util.io.StreamUtils;

public final class UnknownResource implements Resource, Closeable, Writeable, ActionListener,
                                              ChangeListener, DocumentListener, CaretListener,
                                              IDataChangedListener
{
  private static final int TAB_VIEW = 0;
  private static final int TAB_TEXT = 1;
  private static final int TAB_RAW  = 2;

  private static final int MIN_SIZE_WARN        =   4 * 1024 * 1024;  // Show warning for files >= size
  private static final int MIN_SIZE_BLOCK_TEXT  = 128 * 1024 * 1024;  // Block text edit >= size
  private static final int MIN_SIZE_BLOCK_RAW   = 256 * 1024 * 1024;  // Block raw edit >= size

  private final ResourceEntry entry;
  private final long entrySize;
  private final ButtonPanel buttonPanel = new ButtonPanel();

  private JTabbedPane tabbedPane;
  private InfinityTextArea editor;
  private GenericHexViewer hexViewer;
  private JButton bShowEditor;
  private JPanel panelMain, panelRaw;
  private boolean textModified, dataSynced;
  private Charset textCharset;

  public UnknownResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    int[] data = this.entry.getResourceInfo();
    if (data != null && data.length > 0) {
      entrySize = (data.length == 1) ? data[0] : (data[0] * data[1]);
    } else {
      entrySize = 0L;
    }
  }

//--------------------- Begin Interface Closeable ---------------------

  @Override
  public void close() throws Exception
  {
    if (isTextModified() || isRawModified()) {
      ResourceFactory.closeResource(this, entry, panelMain);
    }
  }

//--------------------- End Interface Closeable ---------------------

// --------------------- Begin Interface ChangeListener ---------------------

 @Override
 public void stateChanged(ChangeEvent e)
 {
   if (e.getSource() == tabbedPane) {
     synchronizeData(tabbedPane.getSelectedIndex());
     updateStatusBar();
   }
 }

// --------------------- End Interface ChangeListener ---------------------

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bShowEditor) {
      openTextEditor(true);
    }
    else if (event.getSource() == buttonPanel.getControlByType(ButtonPanel.Control.EXPORT_BUTTON)) {
      ResourceFactory.exportResource(entry, panelMain.getTopLevelAncestor());
    }
    else if (event.getSource() == buttonPanel.getControlByType(ButtonPanel.Control.SAVE)) {
      if (ResourceFactory.saveResource(this, panelMain.getTopLevelAncestor())) {
        setTextModified(false);
        setRawModified(false);
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------

// --------------------- Begin Interface DocumentListener ---------------------

  @Override
  public void insertUpdate(DocumentEvent e)
  {
    setTextModified(true);
  }

  @Override
  public void removeUpdate(DocumentEvent e)
  {
    setTextModified(true);
  }

  @Override
  public void changedUpdate(DocumentEvent e)
  {
    setTextModified(true);
  }

// --------------------- End Interface DocumentListener ---------------------

// --------------------- Begin Interface CaretListener ---------------------

  @Override
  public void caretUpdate(CaretEvent e)
  {
    if (e.getSource() == editor) {
      updateStatusBar();
    }
  }

// --------------------- End Interface CaretListener ---------------------

// --------------------- Begin Interface IDataChangedListener ---------------------

  @Override
  public void dataChanged(DataChangedEvent event)
  {
    setRawModified(true);
  }

// --------------------- End Interface IDataChangedListener ---------------------

// --------------------- Begin Interface Resource ---------------------

  @Override
  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

// --------------------- End Interface Resource ---------------------


// --------------------- Begin Interface Viewable ---------------------

  @Override
  public JComponent makeViewer(ViewableContainer container)
  {
    ((JButton)buttonPanel.addControl(ButtonPanel.Control.EXPORT_BUTTON)).addActionListener(this);
    ((JButton)buttonPanel.addControl(ButtonPanel.Control.SAVE)).addActionListener(this);
    buttonPanel.getControlByType(ButtonPanel.Control.SAVE).setEnabled(false);

    GridBagConstraints gbc = new GridBagConstraints();

    // creating View tab
    JPanel panelView = new JPanel(new GridBagLayout());
    panelView.setBorder(BorderFactory.createLoweredBevelBorder());
    JLabel label = new JLabel("Unsupported file format", JLabel.CENTER);
    bShowEditor = new JButton("Edit as text");
    bShowEditor.addActionListener(this);
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                            GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    panelView.add(new JLabel(), gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                            GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    panelView.add(label, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                            GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    panelView.add(bShowEditor, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 3, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                            GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    panelView.add(new JLabel(), gbc);

    // creating (empty) Edit tab
    editor = new InfinityTextArea(true);
    editor.getDocument().addDocumentListener(this);
    editor.addCaretListener(this);

    // creating Raw tab (stub)
    panelRaw = new JPanel(new BorderLayout());
    if (getEntrySize() >= MIN_SIZE_BLOCK_RAW) {
      label = new JLabel("File is too big for the hex editor (" + getEntrySize() + " bytes).", JLabel.CENTER);
      panelRaw.add(label, BorderLayout.CENTER);
    }

    tabbedPane = new JTabbedPane();
    tabbedPane.addTab("View", panelView);
    tabbedPane.addTab("Edit", new JScrollPane(editor));
    tabbedPane.addTab("Raw", panelRaw);
    tabbedPane.setEnabledAt(TAB_TEXT, false);
    tabbedPane.addChangeListener(this);

    panelMain = new JPanel(new BorderLayout());
    panelMain.add(tabbedPane, BorderLayout.CENTER);
    panelMain.add(buttonPanel, BorderLayout.SOUTH);

    return panelMain;
  }

// --------------------- End Interface Viewable ---------------------

//--------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    if (tabbedPane.getSelectedIndex() == TAB_TEXT) {
      StreamUtils.writeString(os, editor.getText(), editor.getText().length(), textCharset);
    } else {
      StreamUtils.writeBytes(os, hexViewer.getData());
    }
  }

//--------------------- End Interface Writeable ---------------------

  private boolean isEditorActive()
  {
    return tabbedPane.isEnabledAt(TAB_TEXT);
  }

  private void setEditorActive(boolean activate)
  {
    if (tabbedPane.isEnabledAt(TAB_TEXT) != activate) {
      if (!activate && tabbedPane.getSelectedIndex() == TAB_TEXT) {
        tabbedPane.setSelectedIndex(TAB_VIEW);
      }
      tabbedPane.setEnabledAt(TAB_TEXT, activate);
    }
  }

  // Returns file size of ResourceEntry
  private long getEntrySize()
  {
    return entrySize;
  }

  private boolean isRawActive()
  {
    return (hexViewer != null);
  }

  private boolean isTextModified()
  {
    return (isEditorActive() && textModified);
  }

  private void setTextModified(boolean modified)
  {
    if (isEditorActive()) {
      textModified = modified;
      setDataInSync(!modified);
      setSaveButtonEnabled(isTextModified());
    }
  }

  private boolean isRawModified()
  {
    return (isRawActive() && hexViewer.isModified());
  }

  private void setRawModified(boolean modified)
  {
    if (isRawActive()) {
      if (!modified) {
        hexViewer.clearModified();
      }
      setDataInSync(!modified);
      setSaveButtonEnabled(isRawModified());
    }
  }

  // Returns true if content in one of the editors has been modified by the user
  private boolean isDataInSync()
  {
    if (isEditorActive() && isRawActive()) {
      return dataSynced;
    } else {
      return true;
    }
  }

  // Mark data as (not) synchronized
  private void setDataInSync(boolean b)
  {
    dataSynced = b;
  }

  // Synchronizes data in text and raw tabs. Sync target is specified by the tabIndex parameter.
  private void synchronizeData(int tabIndex)
  {
    switch (tabIndex) {
      case TAB_TEXT:
      {
        if (!isDataInSync()) {
          int pos = editor.getCaretPosition();
          editor.setText(hexViewer.getText(textCharset));
          editor.setCaretPosition(Math.min(editor.getDocument().getLength(), pos));
          editor.discardAllEdits();
          setDataInSync(true);
        }
        editor.requestFocusInWindow();
        break;
      }
      case TAB_RAW:
      {
        // lazy initialization
        if (getEntrySize() < MIN_SIZE_BLOCK_RAW) {
          if (!isRawActive()) {
            try {
              WindowBlocker.blockWindow(true);
              hexViewer = new GenericHexViewer(entry);
              hexViewer.addDataChangedListener(this);
              hexViewer.setCurrentOffset(0L);
              panelRaw.add(hexViewer, BorderLayout.CENTER);
            } catch (Exception e) {
              e.printStackTrace();
            } finally {
              WindowBlocker.blockWindow(false);
            }
          }
          if (!isDataInSync()) {
            hexViewer.setText(editor.getText(), textCharset);
            setDataInSync(true);
          }
          hexViewer.requestFocusInWindow();
        }
        break;
      }
    }
  }

  private void setSaveButtonEnabled(boolean enable)
  {
    buttonPanel.getControlByType(ButtonPanel.Control.SAVE).setEnabled(enable);
  }

  private void updateStatusBar()
  {
    if (isEditorActive() && tabbedPane.getSelectedIndex() == TAB_TEXT) {
      int row = editor.getCaretLineNumber() + 1;
      int col = editor.getCaretOffsetFromLineStart() + 1;
      NearInfinity.getInstance().getStatusBar().setCursorText(Integer.toString(row) + ":" + Integer.toString(col));
    } else if (isRawActive() && tabbedPane.getSelectedIndex() == TAB_RAW) {
      hexViewer.updateStatusBar();
    } else {
      NearInfinity.getInstance().getStatusBar().setCursorText("");
    }
  }

  // Opens resource in text editor, optionally trigger safeguard mechanisms if file is big
  private void openTextEditor(boolean confirmSize)
  {
    if (isEditorActive()) {
      tabbedPane.setSelectedIndex(TAB_TEXT);
      return;
    }

    // Confirm loading big files
    if (confirmSize && entry instanceof FileResourceEntry) {
      if (getEntrySize() >= MIN_SIZE_BLOCK_TEXT) {
        JOptionPane.showMessageDialog(panelMain,
                                      "File is too big for the text editor (" + getEntrySize() + " bytes).",
                                      "File size error", JOptionPane.ERROR_MESSAGE);
        return;
      } else if (getEntrySize() >= MIN_SIZE_WARN) {
        if (JOptionPane.showConfirmDialog(panelMain,
                                          "File size is " + getEntrySize() + " bytes. " +
                                              "Do you really want to load the file into the text editor?",
                                          "Show as text?",
                                          JOptionPane.YES_NO_OPTION,
                                          JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
          return;
        }
      }
    }

    WindowBlocker.blockWindow(true);
    new SwingWorker<Void, Void>() {
      @Override
      protected Void doInBackground() throws Exception
      {
        try {
          boolean success = false;
          try {
            // try to determine character encoding format of text data
            final byte[] data = isRawModified() ? hexViewer.getData() : StreamUtils.toArray(entry.getResourceBuffer());
            textCharset = Misc.detectCharset(data);
            editor.setText(new String(data, textCharset));
            editor.setCaretPosition(0);
            editor.discardAllEdits();   // don't undo loading operation
            success = true;
          } catch (Exception e) {
            e.printStackTrace();
          }
          if (success) {
            setEditorActive(true);
            setTextModified(false);
            tabbedPane.setSelectedIndex(TAB_TEXT);
            editor.requestFocusInWindow();
          } else {
            JOptionPane.showMessageDialog(NearInfinity.getInstance(),
                                          "Error reading file data.",
                                          "Error", JOptionPane.ERROR_MESSAGE);
          }
        } finally {
          WindowBlocker.blockWindow(false);
        }
        return null;
      }
    }.execute();
  }
}

