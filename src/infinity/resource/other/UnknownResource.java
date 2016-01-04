// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.other;

import infinity.NearInfinity;
import infinity.gui.ButtonPanel;
import infinity.gui.InfinityTextArea;
import infinity.gui.ViewerUtil;
import infinity.gui.WindowBlocker;
import infinity.resource.Closeable;
import infinity.resource.Profile;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.ViewableContainer;
import infinity.resource.Writeable;
import infinity.resource.key.BIFFResourceEntry;
import infinity.resource.key.FileResourceEntry;
import infinity.resource.key.ResourceEntry;
import infinity.util.io.FileNI;
import infinity.util.io.FileWriterNI;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public final class UnknownResource implements Resource, Closeable, Writeable, ActionListener,
                                              ChangeListener, DocumentListener
{
  private static final String CARD_BUTTON = "button";
  private static final String CARD_EDITOR = "editor";

  private final ResourceEntry entry;
  private final ButtonPanel buttonPanel = new ButtonPanel();

  private JTabbedPane tabbedPane;
  private InfinityTextArea editor;
  private JButton bShowEditor;
  private JPanel panelMain, panelEdit;
  private boolean editorActive, textModified;
  private Charset textCharset;

  public UnknownResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
  }

//--------------------- Begin Interface Closeable ---------------------

 @Override
 public void close() throws Exception
 {
   if (isTextModified()) {
     File output = null;
     if (entry instanceof BIFFResourceEntry) {
       output = FileNI.getFile(Profile.getRootFolders(),
                               Profile.getOverrideFolderName() + File.separatorChar + entry.toString());
     } else if (entry instanceof FileResourceEntry) {
       output = entry.getActualFile();
     }

     if (output != null) {
       final String options[] = {"Save changes", "Discard changes", "Cancel"};
       int result = JOptionPane.showOptionDialog(panelMain, "Save changes to " + output.toString(),
                                                 "Resource changed", JOptionPane.YES_NO_CANCEL_OPTION,
                                                 JOptionPane.WARNING_MESSAGE, null, options, options[0]);
       if (result == 0) {
         ResourceFactory.saveResource(this, panelMain.getTopLevelAncestor());
       } else if (result != 1) {
         throw new Exception("Save aborted");
       }
     }
   }
 }

//--------------------- End Interface Closeable ---------------------

// --------------------- Begin Interface ChangeListener ---------------------

 @Override
 public void stateChanged(ChangeEvent e)
 {
   if (e.getSource() == tabbedPane) {
     setSaveButtonEnabled(isTextModified());
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
    else if (event.getSource() == buttonPanel.getControlByType(ButtonPanel.Control.ExportButton)) {
      ResourceFactory.exportResource(entry, panelMain.getTopLevelAncestor());
    }
    else if (event.getSource() == buttonPanel.getControlByType(ButtonPanel.Control.Save)) {
      if (ResourceFactory.saveResource(this, panelMain.getTopLevelAncestor())) {
        setTextModified(false);
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------

// --------------------- Begin Interface DocumentListener ---------------------

  @Override
  public void insertUpdate(DocumentEvent e)
  {
    textModified = true;
    setSaveButtonEnabled(isTextModified());
  }

  @Override
  public void removeUpdate(DocumentEvent e)
  {
    textModified = true;
    setSaveButtonEnabled(isTextModified());
  }

  @Override
  public void changedUpdate(DocumentEvent e)
  {
    textModified = true;
    setSaveButtonEnabled(isTextModified());
  }

// --------------------- End Interface DocumentListener ---------------------


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
    ((JButton)buttonPanel.addControl(ButtonPanel.Control.ExportButton)).addActionListener(this);
    ((JButton)buttonPanel.addControl(ButtonPanel.Control.Save)).addActionListener(this);
    buttonPanel.getControlByType(ButtonPanel.Control.Save).setEnabled(false);

    JPanel panelView = new JPanel(new BorderLayout());
    JLabel label = new JLabel("Unsupported file format", JLabel.CENTER);
    panelView.add(label, BorderLayout.CENTER);
    panelView.add(buttonPanel, BorderLayout.SOUTH);
    label.setBorder(BorderFactory.createLoweredBevelBorder());

    CardLayout cl = new CardLayout();
    panelEdit = new JPanel(cl);

    JPanel panelEditButton = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                            GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    bShowEditor = new JButton("Edit as text");
    bShowEditor.addActionListener(this);
    panelEditButton.add(bShowEditor, gbc);
    panelEdit.add(panelEditButton, CARD_BUTTON);

    editor = new InfinityTextArea(true);
    editor.getDocument().addDocumentListener(this);
    panelEdit.add(new JScrollPane(editor), CARD_EDITOR);
    cl.show(panelEdit, "BUTTON");

    tabbedPane = new JTabbedPane();
    tabbedPane.addTab("View", panelView);
    tabbedPane.addTab("Edit", panelEdit);
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
    if (isTextModified()) {
      FileWriterNI.writeString(os, editor.getText(), editor.getText().length(), textCharset);
    }
  }

//--------------------- End Interface Writeable ---------------------

  private boolean isEditorActive()
  {
    return editorActive;
  }

  private void setEditorActive(boolean activate)
  {
    editorActive = activate;
  }

  private boolean isTextModified()
  {
    return (isEditorActive() && textModified);
  }

  private void setTextModified(boolean modified)
  {
    if (modified != textModified) {
      textModified = modified;
      setSaveButtonEnabled(isTextModified());
    }
  }

  private void setSaveButtonEnabled(boolean enable)
  {
    buttonPanel.getControlByType(ButtonPanel.Control.Save).setEnabled(enable);
  }

  // Opens resource in text editor, optionally ask for confirmation if file is big
  private void openTextEditor(boolean confirmSize)
  {
    if (isEditorActive()) {
      return;
    }

    // Confirm loading big files
    if (confirmSize && entry instanceof FileResourceEntry) {
      File file = ((FileResourceEntry)entry).getActualFile();
      if (file != null) {
        long fileSize = file.length();
        if (fileSize > 512 * 1024) {
          if (JOptionPane.showConfirmDialog(panelMain,
                                            "File size is " + fileSize + " bytes. " +
                                                "Do you really want to load the file into the text editor?",
                                            "Show as text?",
                                            JOptionPane.YES_NO_OPTION,
                                            JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
          }
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
            final byte[] data = entry.getResourceData();
            textCharset = null;
            if (data != null) {
              if (data.length >= 3 &&
                  data[0] == -17 && data[1] == -69 && data[2] == -65) { // UTF-8 BOM (0xef, 0xbb, 0xbf)
                textCharset = Charset.forName("utf-8");
              } else if (data.length >= 2 &&
                         data[0] == -2 && data[1] == -1) {  // UTF-16 BOM (0xfeff) in big-endian order
                textCharset = Charset.forName("utf-16be");
              } else if (data.length >= 2 &&
                         data[0] == -1 && data[1] == -2) {  // UTF-16 BOM (0xfeff) in little-endian order
                textCharset = Charset.forName("utf-16le");
              }
            }
            if (textCharset == null) {
              // fall back to ANSI charset
              textCharset = Charset.forName("iso-8859-1");
            }
            editor.setText(new String(data, textCharset));
            editor.setCaretPosition(0);
            editor.discardAllEdits();   // don't undo loading operation
            success = true;
          } catch (Exception e) {
            e.printStackTrace();
          }
          if (success) {
            CardLayout cl = (CardLayout)panelEdit.getLayout();
            cl.show(panelEdit, CARD_EDITOR);
            editor.requestFocusInWindow();
            setEditorActive(true);
            setTextModified(false);
            setSaveButtonEnabled(isTextModified());
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

