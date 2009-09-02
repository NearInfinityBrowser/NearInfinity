// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.bcs;

import infinity.resource.*;
import infinity.resource.Closeable;
import infinity.resource.key.ResourceEntry;
import infinity.resource.key.BIFFResourceEntry;
import infinity.util.Decryptor;
import infinity.util.Filewriter;
import infinity.gui.*;
import infinity.icon.Icons;
import infinity.search.TextResourceSearcher;

import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class BafResource implements TextResource, Writeable, Closeable, ItemListener, ActionListener,
                                    DocumentListener
{
  private static JFileChooser chooser;
  private final ResourceEntry entry;
  private JTabbedPane tabbedPane;
  private ButtonPopupMenu bfind, buses, berrors, bwarnings;
  private JButton bcompile, bdecompile, bsave, bsaveScript, bExport;
  private JMenuItem ifindall, ifindthis;
  private JPanel panel;
  private JTextArea codeText, sourceText;
  private String text;
  private boolean sourceChanged = false;

  public BafResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    byte data[] = entry.getResourceData();
    if (data.length == 0)
      text = "";
    else if (data[0] == -1)
      text = Decryptor.decrypt(data, 2, data.length);
    else
      text = new String(data);
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bcompile) {
      codeText.setText(Compiler.getInstance().compile(sourceText.getText()));
      codeText.setCaretPosition(0);
      bcompile.setEnabled(false);
      bdecompile.setEnabled(false);
      bsaveScript.setEnabled(Compiler.getInstance().getErrors().size() == 0);
      SortedMap<Integer, String> errorMap = Compiler.getInstance().getErrors();
      SortedMap<Integer, String> warningMap = Compiler.getInstance().getWarnings();
      berrors.setText("Errors (" + errorMap.size() + ")...");
      bwarnings.setText("Warnings (" + warningMap.size() + ")...");
      if (errorMap.size() == 0)
        berrors.setEnabled(false);
      else {
        JMenuItem errorItems[] = new JMenuItem[errorMap.size()];
        int counter = 0;
        for (final Integer lineNr : errorMap.keySet()) {
          String error = errorMap.get(lineNr);
          errorItems[counter++] = new JMenuItem(lineNr.toString() + ": " + error);
        }
        berrors.setMenuItems(errorItems);
        berrors.setEnabled(true);
      }
      if (warningMap.size() == 0)
        bwarnings.setEnabled(false);
      else {
        JMenuItem warningItems[] = new JMenuItem[warningMap.size()];
        int counter = 0;
        for (final Integer lineNr : warningMap.keySet()) {
          String warning = warningMap.get(lineNr);
          warningItems[counter++] = new JMenuItem(lineNr.toString() + ": " + warning);
        }
        bwarnings.setMenuItems(warningItems);
        bwarnings.setEnabled(true);
      }
      Decompiler.decompile(codeText.getText(), true);
      Set<ResourceEntry> uses = Decompiler.getResourcesUsed();
      JMenuItem usesItems[] = new JMenuItem[uses.size()];
      int usesIndex = 0;
      for (final ResourceEntry usesEntry : uses) {
        if (usesEntry.getSearchString() != null)
          usesItems[usesIndex++] =
          new JMenuItem(usesEntry.toString() + " (" + usesEntry.getSearchString() + ')');
        else
          usesItems[usesIndex++] = new JMenuItem(usesEntry.toString());
      }
      buses.setMenuItems(usesItems);
      buses.setEnabled(usesItems.length > 0);
    }
    else if (event.getSource() == bdecompile) {
      sourceText.setText(Decompiler.decompile(codeText.getText(), true));
      sourceText.setCaretPosition(0);
      Set<ResourceEntry> uses = Decompiler.getResourcesUsed();
      JMenuItem usesItems[] = new JMenuItem[uses.size()];
      int usesIndex = 0;
      for (final ResourceEntry usesEntry : uses) {
        if (usesEntry.getSearchString() != null)
          usesItems[usesIndex++] =
          new JMenuItem(usesEntry.toString() + " (" + usesEntry.getSearchString() + ')');
        else
          usesItems[usesIndex++] = new JMenuItem(usesEntry.toString());
      }
      buses.setMenuItems(usesItems);
      buses.setEnabled(usesItems.length > 0);
      bcompile.setEnabled(false);
      bdecompile.setEnabled(false);
      tabbedPane.setSelectedIndex(0);
    }
    else if (event.getSource() == bsave) {
      if (berrors.isEnabled()) {
        String options[] = {"Save", "Cancel"};
        int result = JOptionPane.showOptionDialog(panel, "Script contains errors. Save anyway?", "Errors found",
                                                  JOptionPane.YES_NO_OPTION,
                                                  JOptionPane.WARNING_MESSAGE, null, options, options[0]);
        if (result == 1)
          return;
      }
      if (ResourceFactory.getInstance().saveResource(this, panel.getTopLevelAncestor())) {
        bsave.setEnabled(false);
        sourceChanged = false;
      }
    }
    else if (event.getSource() == bsaveScript) {
      if (chooser == null) {
        chooser = new JFileChooser(ResourceFactory.getRootDir());
        chooser.setDialogTitle("Save source code");
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter()
        {
          public boolean accept(File pathname)
          {
            return pathname.isDirectory() || pathname.getName().toLowerCase().endsWith(".bcs");
          }

          public String getDescription()
          {
            return "Infinity script (.BCS)";
          }
        });
      }
      chooser.setSelectedFile(
              new File(entry.toString().substring(0, entry.toString().indexOf((int)'.')) + ".BCS"));
      int returnval = chooser.showSaveDialog(panel.getTopLevelAncestor());
      if (returnval == JFileChooser.APPROVE_OPTION) {
        try {
          PrintWriter pw = new PrintWriter(new FileOutputStream(chooser.getSelectedFile()));
          pw.println(codeText.getText());
          pw.close();
          JOptionPane.showMessageDialog(panel, "File saved to \"" + chooser.getSelectedFile().toString() +
                                               '\"', "Save completed", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
          JOptionPane.showMessageDialog(panel, "Error saving " + chooser.getSelectedFile().toString(),
                                        "Error", JOptionPane.ERROR_MESSAGE);
          e.printStackTrace();
        }
      }
    }
    else if (event.getSource() == bExport)
      ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Closeable ---------------------

  public void close() throws Exception
  {
    if (sourceChanged) {
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
  }

// --------------------- End Interface Closeable ---------------------


// --------------------- Begin Interface DocumentListener ---------------------

  public void insertUpdate(DocumentEvent event)
  {
    if (event.getDocument() == codeText.getDocument()) {
      bdecompile.setEnabled(true);
    }
    else if (event.getDocument() == sourceText.getDocument()) {
      bsave.setEnabled(true);
      bcompile.setEnabled(true);
      sourceChanged = true;
    }
  }

  public void removeUpdate(DocumentEvent event)
  {
    if (event.getDocument() == codeText.getDocument()) {
      bdecompile.setEnabled(true);
    }
    else if (event.getDocument() == sourceText.getDocument()) {
      bsave.setEnabled(true);
      bcompile.setEnabled(true);
      sourceChanged = true;
    }
  }

  public void changedUpdate(DocumentEvent event)
  {
    if (event.getDocument() == codeText.getDocument()) {
      bdecompile.setEnabled(true);
    }
    else if (event.getDocument() == sourceText.getDocument()) {
      bsave.setEnabled(true);
      bcompile.setEnabled(true);
      sourceChanged = true;
    }
  }

// --------------------- End Interface DocumentListener ---------------------


// --------------------- Begin Interface ItemListener ---------------------

  public void itemStateChanged(ItemEvent event)
  {
    if (event.getSource() == bfind) {
      if (bfind.getSelectedItem() == ifindall) {
        java.util.List<ResourceEntry> files = ResourceFactory.getInstance().getResources("BAF");
        new TextResourceSearcher(files, panel.getTopLevelAncestor());
      }
      else if (bfind.getSelectedItem() == ifindthis) {
        java.util.List<ResourceEntry> files = new ArrayList<ResourceEntry>(1);
        files.add(entry);
        new TextResourceSearcher(files, panel.getTopLevelAncestor());
      }
    }
    else if (event.getSource() == buses) {
      JMenuItem item = buses.getSelectedItem();
      String name = item.getText();
      int index = name.indexOf(" (");
      if (index != -1)
        name = name.substring(0, index);
      ResourceEntry resEntry = ResourceFactory.getInstance().getResourceEntry(name);
      new ViewFrame(panel.getTopLevelAncestor(), ResourceFactory.getResource(resEntry));
    }
    else if (event.getSource() == berrors) {
      String selected = berrors.getSelectedItem().getText();
      int linenr = Integer.parseInt(selected.substring(0, selected.indexOf(": ")));
      highlightText(linenr, null);
    }
    else if (event.getSource() == bwarnings) {
      String selected = bwarnings.getSelectedItem().getText();
      int linenr = Integer.parseInt(selected.substring(0, selected.indexOf(": ")));
      highlightText(linenr, null);
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
    return text;
  }

  public void highlightText(int linenr, String highlightText)
  {
    String s = sourceText.getText();
    int startpos = 0;
    for (int i = 1; i < linenr; i++)
      startpos = s.indexOf("\n", startpos + 1);
    if (startpos == -1) return;
    int wordpos = -1;
    if (highlightText != null)
      wordpos = s.toUpperCase().indexOf(highlightText.toUpperCase(), startpos);
    if (wordpos != -1)
      sourceText.select(wordpos, wordpos + highlightText.length());
    else
      sourceText.select(startpos, s.indexOf("\n", startpos + 1));
    sourceText.getCaret().setSelectionVisible(true);
  }

// --------------------- End Interface TextResource ---------------------


// --------------------- Begin Interface Viewable ---------------------

  public JComponent makeViewer(ViewableContainer container)
  {
    sourceText = new JTextArea(text);
    sourceText.addCaretListener(container.getStatusBar());
    sourceText.setFont(BrowserMenuBar.getInstance().getScriptFont());
    sourceText.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    sourceText.setLineWrap(false);
    sourceText.setTabSize(4);
    sourceText.getDocument().addDocumentListener(this);
    JScrollPane scrollSource = new JScrollPane(sourceText);
    scrollSource.setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlDkShadow")));

    bcompile = new JButton("Compile", Icons.getIcon("Redo16.gif"));
    bcompile.setMnemonic('c');
    bcompile.addActionListener(this);
    berrors = new ButtonPopupMenu("Errors (0)...", new JMenuItem[0]);
    berrors.setIcon(Icons.getIcon("Up16.gif"));
    berrors.addItemListener(this);
    bwarnings = new ButtonPopupMenu("Warnings (0)...", new JMenuItem[0]);
    bwarnings.setIcon(Icons.getIcon("Up16.gif"));
    bwarnings.addItemListener(this);
    JPanel sourcePanelButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
    sourcePanelButtons.add(bcompile);
    sourcePanelButtons.add(berrors);
    sourcePanelButtons.add(bwarnings);

    JPanel sourcePanel = new JPanel(new BorderLayout());
    sourcePanel.add(scrollSource, BorderLayout.CENTER);
    sourcePanel.add(sourcePanelButtons, BorderLayout.SOUTH);

    codeText = new JTextArea();
    codeText.setFont(BrowserMenuBar.getInstance().getScriptFont());
    codeText.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    codeText.setCaretPosition(0);
    codeText.setLineWrap(false);
    codeText.getDocument().addDocumentListener(this);
    JScrollPane scrollCode = new JScrollPane(codeText);
    scrollCode.setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlDkShadow")));

    bdecompile = new JButton("Decompile", Icons.getIcon("Undo16.gif"));
    bdecompile.setMnemonic('d');
    bdecompile.addActionListener(this);
    JPanel codePanelButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
    codePanelButtons.add(bdecompile);

    JPanel codePanel = new JPanel(new BorderLayout());
    codePanel.add(scrollCode, BorderLayout.CENTER);
    codePanel.add(codePanelButtons, BorderLayout.SOUTH);

    ifindall = new JMenuItem("in all scripts");
    ifindthis = new JMenuItem("in this script only");
    bsave = new JButton("Save", Icons.getIcon("Save16.gif"));
    bsave.setMnemonic('a');
    bsaveScript = new JButton("Save code", Icons.getIcon("Save16.gif"));
    bExport = new JButton("Export", Icons.getIcon("Export16.gif"));
    bExport.setToolTipText("NB! Will export last *saved* version");
    bfind = new ButtonPopupMenu("Find...", new JMenuItem[]{ifindall, ifindthis});
    bfind.setIcon(Icons.getIcon("Find16.gif"));
    buses = new ButtonPopupMenu("Uses...", new JMenuItem[0]);
    buses.setIcon(Icons.getIcon("Find16.gif"));
    bfind.addItemListener(this);
    buses.addItemListener(this);
    bsave.addActionListener(this);
    bsaveScript.addActionListener(this);

    tabbedPane = new JTabbedPane();
    tabbedPane.addTab("Script source", sourcePanel);
    tabbedPane.addTab("Script code (compiled)", codePanel);

    JPanel lowerpanel = new JPanel();
    lowerpanel.setLayout(new FlowLayout(FlowLayout.CENTER, 6, 6));
    lowerpanel.add(bfind);
    lowerpanel.add(buses);
    lowerpanel.add(bExport);
    lowerpanel.add(bsave);
    lowerpanel.add(bsaveScript);

    panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(tabbedPane, BorderLayout.CENTER);
    panel.add(lowerpanel, BorderLayout.SOUTH);

    bcompile.setEnabled(true);
    berrors.setEnabled(false);
    bwarnings.setEnabled(false);
    bdecompile.setEnabled(false);
    bsave.setEnabled(false);
    buses.setEnabled(false);
    bsaveScript.setEnabled(false);

    return panel;
  }

// --------------------- End Interface Viewable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    if (sourceText == null)
      Filewriter.writeString(os, text, text.length());
    else
      Filewriter.writeString(os, sourceText.getText(), sourceText.getText().length());
  }

// --------------------- End Interface Writeable ---------------------
}

