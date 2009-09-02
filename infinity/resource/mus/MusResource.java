// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.mus;

import infinity.gui.BrowserMenuBar;
import infinity.gui.ButtonPopupMenu;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.Closeable;
import infinity.resource.key.BIFFResourceEntry;
import infinity.resource.key.ResourceEntry;
import infinity.search.TextResourceSearcher;
import infinity.util.Filewriter;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public final class MusResource implements Closeable, TextResource, ActionListener, Writeable, ItemListener,
                                          DocumentListener
{
  private static int lastIndex = -1;
  private final JTabbedPane tabbedPane = new JTabbedPane();
  private final ResourceEntry entry;
  private final String text;
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
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bsave) {
      if (ResourceFactory.getInstance().saveResource(this, panel.getTopLevelAncestor()))
        resourceChanged = false;
      viewer.parseMusfile(this);
    }
    else if (event.getSource() == bexport)
      ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Closeable ---------------------

  public void close() throws Exception
  {
    lastIndex = tabbedPane.getSelectedIndex();
    if (resourceChanged) {
      File output;
      if (entry instanceof BIFFResourceEntry)
        output =
        new File(ResourceFactory.getRootDir(),
                 ResourceFactory.OVERRIDEFOLDER + File.separatorChar + entry.toString());
      else
        output = entry.getActualFile();
      String options[] = {"Save changes", "Discard changes", "Cancel"};
      int result = JOptionPane.showOptionDialog(panel, "Save changes to " + output + '?', "Resource changed",
                                                JOptionPane.YES_NO_CANCEL_OPTION,
                                                JOptionPane.WARNING_MESSAGE, null, options, options[0]);
      if (result == 0)
        ResourceFactory.getInstance().saveResource(this, panel.getTopLevelAncestor());
      else if (result == 2)
        throw new Exception("Save aborted");
    }
    if (viewer != null)
      viewer.close();
  }

// --------------------- End Interface Closeable ---------------------


// --------------------- Begin Interface DocumentListener ---------------------

  public void insertUpdate(DocumentEvent event)
  {
    resourceChanged = true;
  }

  public void removeUpdate(DocumentEvent event)
  {
    resourceChanged = true;
  }

  public void changedUpdate(DocumentEvent event)
  {
    resourceChanged = true;
  }

// --------------------- End Interface DocumentListener ---------------------


// --------------------- Begin Interface ItemListener ---------------------

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

  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

// --------------------- End Interface Resource ---------------------


// --------------------- Begin Interface TextResource ---------------------

  public String getText()
  {
    if (editor == null)
      return text;
    else
      return editor.getText();
  }

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

  public JComponent makeViewer(ViewableContainer container)
  {
    viewer = new Viewer(this);
    panel = new JPanel();
    tabbedPane.addTab("View", viewer);
    tabbedPane.addTab("Edit", getEditor(container.getStatusBar()));
    panel.setLayout(new BorderLayout());
    panel.add(tabbedPane, BorderLayout.CENTER);
    if (lastIndex != -1)
      tabbedPane.setSelectedIndex(lastIndex);
    else if (BrowserMenuBar.getInstance().getDefaultStructView() == BrowserMenuBar.DEFAULT_EDIT)
      tabbedPane.setSelectedIndex(1);
    return panel;
  }

// --------------------- End Interface Viewable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    if (editor == null)
      Filewriter.writeString(os, text, text.length());
    else
      Filewriter.writeString(os, editor.getText(), editor.getText().length());
  }

// --------------------- End Interface Writeable ---------------------

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
}

