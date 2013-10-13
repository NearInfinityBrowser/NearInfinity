// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.mus;

import infinity.gui.BrowserMenuBar;
import infinity.gui.ButtonPopupMenu;
import infinity.gui.WindowBlocker;
import infinity.icon.Icons;
import infinity.resource.Closeable;
import infinity.resource.ResourceFactory;
import infinity.resource.TextResource;
import infinity.resource.ViewableContainer;
import infinity.resource.Writeable;
import infinity.resource.key.BIFFResourceEntry;
import infinity.resource.key.ResourceEntry;
import infinity.search.TextResourceSearcher;
import infinity.util.Filewriter;
import infinity.util.NIFile;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public final class MusResource implements Closeable, TextResource, ActionListener, Writeable, ItemListener,
                                          DocumentListener
{
  private static int lastIndex = -1;
  private final ResourceEntry entry;
  private final String text;
  private JTabbedPane tabbedPane;
  private ButtonPopupMenu bfind;
  private JButton bsave, bexport;
  private JMenuItem ifindall, ifindthis;
  private JPanel panel;
  private JTextArea editor;
  private Viewer viewer;
  private boolean resourceChanged;

  public MusResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    text = new String(entry.getResourceData());
    resourceChanged = false;
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bsave) {
      if (ResourceFactory.getInstance().saveResource(this, panel.getTopLevelAncestor())) {
        setDocumentModified(false);
      }
      viewer.loadMusResource(this);
    }
    else if (event.getSource() == bexport)
      ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Closeable ---------------------

  @Override
  public void close() throws Exception
  {
    lastIndex = tabbedPane.getSelectedIndex();
    if (resourceChanged) {
      File output;
      if (entry instanceof BIFFResourceEntry) {
        output = NIFile.getFile(ResourceFactory.getRootDirs(),
                 ResourceFactory.OVERRIDEFOLDER + File.separatorChar + entry.toString());
      } else {
        output = entry.getActualFile();
      }
      String options[] = {"Save changes", "Discard changes", "Cancel"};
      int result = JOptionPane.showOptionDialog(panel, "Save changes to " + output + '?', "Resource changed",
                                                JOptionPane.YES_NO_CANCEL_OPTION,
                                                JOptionPane.WARNING_MESSAGE, null, options, options[0]);
      if (result == 0) {
        ResourceFactory.getInstance().saveResource(this, panel.getTopLevelAncestor());
      } else if (result == 2) {
        throw new Exception("Save aborted");
      }
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
    if (event.getSource() == bfind) {
      if (bfind.getSelectedItem() == ifindall) {
        String type = entry.toString().substring(entry.toString().indexOf(".") + 1);
        List<ResourceEntry> files = ResourceFactory.getInstance().getResources(type);
        new TextResourceSearcher(files, panel.getTopLevelAncestor());
      }
      else if (bfind.getSelectedItem() == ifindthis) {
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
    if (editor == null)
      return text;
    else
      return editor.getText();
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
    if (editor == null)
      Filewriter.writeString(os, text, text.length());
    else
      Filewriter.writeString(os, editor.getText(), editor.getText().length());
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
    bfind = new ButtonPopupMenu("Find...", new JMenuItem[]{ifindall, ifindthis});
    bfind.addItemListener(this);
    editor = new JTextArea();
    editor.addCaretListener(caretListener);
    editor.setText(text);
    editor.setFont(BrowserMenuBar.getInstance().getScriptFont());
    editor.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    editor.setCaretPosition(0);
    editor.setLineWrap(false);
    editor.getDocument().addDocumentListener(this);
    bexport = new JButton("Export");
    bexport.setMnemonic('e');
    bexport.setToolTipText("NB! Will export last *saved* version");
    bexport.addActionListener(this);
    bsave = new JButton("Save");
    bsave.setMnemonic('a');
    bsave.addActionListener(this);
    bfind.setIcon(Icons.getIcon("Find16.gif"));
    bexport.setIcon(Icons.getIcon("Export16.gif"));
    bsave.setIcon(Icons.getIcon("Save16.gif"));
    bsave.setEnabled(getDocumentModified());

    JPanel bpanel = new JPanel();
    bpanel.setLayout(new GridLayout(1, 3, 6, 0));
    bpanel.add(bfind);
    bpanel.add(bexport);
    bpanel.add(bsave);

    JPanel lowerpanel = new JPanel();
    lowerpanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    lowerpanel.add(bpanel);

    JPanel panel2 = new JPanel();
    panel2.setLayout(new BorderLayout());
    panel2.add(new JScrollPane(editor), BorderLayout.CENTER);
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
      bsave.setEnabled(resourceChanged);
    }
  }
}

