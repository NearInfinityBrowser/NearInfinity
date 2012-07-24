// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.bcs;

import infinity.gui.*;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.Closeable;
import infinity.resource.key.BIFFResourceEntry;
import infinity.resource.key.ResourceEntry;
import infinity.search.ScriptReferenceSearcher;
import infinity.search.TextResourceSearcher;
import infinity.util.Decryptor;
import infinity.util.Filewriter;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

public final class BcsResource implements TextResource, Writeable, Closeable, ActionListener, ItemListener,
                                          DocumentListener
{
  private static JFileChooser chooser;
  private static final boolean DEBUG = false;
  private final ResourceEntry entry;
  private ButtonPopupMenu bfind, buses, bexport, berrors, bwarnings;
  private JButton bcompile, bdecompile, bsave;
  private JMenuItem ifindall, ifindthis, ifindusage, iexportsource, iexportscript;
  private JPanel panel;
  private JTabbedPane tabbedPane;
  private JTextArea codeText;
  private ScriptTextArea sourceText;
  private String text;
  private boolean sourceChanged = false, codeChanged = false;

  public static void main(String args[]) throws IOException
  {
    new ResourceFactory(new File("CHITIN.KEY"));
    List<ResourceEntry> bcsfiles = ResourceFactory.getInstance().getResources("BCS");
    bcsfiles.addAll(ResourceFactory.getInstance().getResources("BS"));
    PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter("diff.txt")));
    long start = System.currentTimeMillis();
    for (int i = bcsfiles.size() - 1; i >= 0; i--) {
      try {
        BcsResource bcs = new BcsResource(bcsfiles.get(i));
        String recompiled = Compiler.getInstance().compile(Decompiler.decompile(bcs.text, true));
        if (Compiler.getInstance().getErrors().size() > 0) {
          System.out.println("Errors in " + bcs.entry.toString());
          pw.println(bcs.entry.toString());
          for (final String error : Compiler.getInstance().getErrors().values())
            pw.println(error);
        }
        else if (!recompiled.equals(bcs.text)) {
          int index = bcs.text.indexOf("\r\n");
          while (index != -1) {
            bcs.text = bcs.text.substring(0, index) + '\n' + bcs.text.substring(index + 2);
            index = bcs.text.indexOf("\r\n");
          }
          if (!recompiled.equals(bcs.text)) {
            System.out.println("Difference in " + bcs.entry.toString());
            pw.println(bcs.entry.toString());
          }
        }
      } catch (Exception e) {
        System.out.println("Exception in " + bcsfiles.get(i).toString());
        pw.println(bcsfiles.get(i).toString());
        e.printStackTrace(pw);
      }
      if (i == 10 * (i / 10))
        System.out.println(i + " scripts left");
    }
    pw.close();
    System.out.println("Test took " + (System.currentTimeMillis() - start) + " ms");
    System.exit(0);
  }

  public BcsResource(ResourceEntry entry) throws Exception
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
      try {
        if (DEBUG) {
          BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("bcs_org.txt"));
          write(bos);
          bos.close();
        }
        codeText.setText(Compiler.getInstance().compile(sourceText.getText()));
        codeText.setCaretPosition(0);
        bcompile.setEnabled(false);
        bdecompile.setEnabled(false);
        sourceChanged = false;
        codeChanged = true;
        if (DEBUG) {
          BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("bcs_new.txt"));
          write(bos);
          bos.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      iexportscript.setEnabled(Compiler.getInstance().getErrors().size() == 0);
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
      sourceChanged = false;
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
        codeChanged = false;
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Closeable ---------------------

  public void close() throws Exception
  {
    if (sourceChanged) {
      String options[] = {"Compile & save", "Discard changes", "Cancel"};
      int result = JOptionPane.showOptionDialog(panel, "Script contains uncompiled changes", "Uncompiled changes",
                                                JOptionPane.YES_NO_CANCEL_OPTION,
                                                JOptionPane.WARNING_MESSAGE, null, options, options[0]);
      if (result == 0) {
        bcompile.doClick();
        if (berrors.isEnabled())
          throw new Exception("Save aborted");
        ResourceFactory.getInstance().saveResource(this, panel.getTopLevelAncestor());
      }
      else if (result == 2)
        throw new Exception("Save aborted");
    }
    else if (codeChanged) {
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
      bsave.setEnabled(true);
      bdecompile.setEnabled(true);
      sourceChanged = false;
      codeChanged = true;
    }
    else if (event.getDocument() == sourceText.getDocument()) {
      bcompile.setEnabled(true);
      sourceChanged = true;
    }
  }

  public void removeUpdate(DocumentEvent event)
  {
    if (event.getDocument() == codeText.getDocument()) {
      bsave.setEnabled(true);
      bdecompile.setEnabled(true);
      sourceChanged = false;
      codeChanged = true;
    }
    else if (event.getDocument() == sourceText.getDocument()) {
      bcompile.setEnabled(true);
      sourceChanged = true;
    }
  }

  public void changedUpdate(DocumentEvent event)
  {
    if (event.getDocument() == codeText.getDocument()) {
      bsave.setEnabled(true);
      bdecompile.setEnabled(true);
      sourceChanged = false;
      codeChanged = true;
    }
    else if (event.getDocument() == sourceText.getDocument()) {
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
        List<ResourceEntry> files = ResourceFactory.getInstance().getResources("BCS");
        files.addAll(ResourceFactory.getInstance().getResources("BS"));
        new TextResourceSearcher(files, panel.getTopLevelAncestor());
      }
      else if (bfind.getSelectedItem() == ifindthis) {
        List<ResourceEntry> files = new ArrayList<ResourceEntry>(1);
        files.add(entry);
        new TextResourceSearcher(files, panel.getTopLevelAncestor());
      }
      else if (bfind.getSelectedItem() == ifindusage)
        new ScriptReferenceSearcher(entry, panel.getTopLevelAncestor());
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
    else if (event.getSource() == bexport) {
      if (bexport.getSelectedItem() == iexportsource) {
        if (chooser == null) {
          chooser = new JFileChooser(ResourceFactory.getRootDir());
          chooser.setDialogTitle("Export source");
          chooser.setFileFilter(new FileFilter()
          {
            public boolean accept(File pathname)
            {
              return pathname.isDirectory() || pathname.getName().toLowerCase().endsWith(".baf");
            }

            public String getDescription()
            {
              return "Infinity script (.BAF)";
            }
          });
        }
        chooser.setSelectedFile(
                new File(entry.toString().substring(0, entry.toString().indexOf((int)'.')) + ".BAF"));
        int returnval = chooser.showSaveDialog(panel.getTopLevelAncestor());
        if (returnval == JFileChooser.APPROVE_OPTION) {
          try {
            PrintWriter pw = new PrintWriter(new FileOutputStream(chooser.getSelectedFile()));
            pw.println(sourceText.getText());
            pw.close();
            JOptionPane.showMessageDialog(panel, "File saved to \"" + chooser.getSelectedFile().toString() +
                                                 '\"', "Export complete", JOptionPane.INFORMATION_MESSAGE);
          } catch (IOException e) {
            JOptionPane.showMessageDialog(panel, "Error exporting " + chooser.getSelectedFile().toString(),
                                          "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
          }
        }
      }
      else if (bexport.getSelectedItem() == iexportscript)
        ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
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
    if (sourceText != null)
      return sourceText.getText();
    return Decompiler.decompile(text, false);
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
    sourceText = new ScriptTextArea();
    sourceText.addCaretListener(container.getStatusBar());
    sourceText.setFont(BrowserMenuBar.getInstance().getScriptFont());
    sourceText.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    sourceText.setLineWrap(false);
    sourceText.setTabSize(4);
    sourceText.getDocument().addDocumentListener(this);
    JScrollPane scrollDecompiled = new JScrollPane(sourceText);
    scrollDecompiled.setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlDkShadow")));

    bcompile = new JButton("Compile", Icons.getIcon("Redo16.gif"));
    bcompile.setMnemonic('c');
    bcompile.addActionListener(this);
    berrors = new ButtonPopupMenu("Errors (0)...", new JMenuItem[0]);
    berrors.setIcon(Icons.getIcon("Up16.gif"));
    berrors.addItemListener(this);
    bwarnings = new ButtonPopupMenu("Warnings (0)...", new JMenuItem[0]);
    bwarnings.setIcon(Icons.getIcon("Up16.gif"));
    bwarnings.addItemListener(this);
    JPanel decompilePanelButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
    decompilePanelButtons.add(bcompile);
    decompilePanelButtons.add(berrors);
    decompilePanelButtons.add(bwarnings);

    JPanel decompiledPanel = new JPanel(new BorderLayout());
    decompiledPanel.add(scrollDecompiled, BorderLayout.CENTER);
    decompiledPanel.add(decompilePanelButtons, BorderLayout.SOUTH);

    codeText = new JTextArea(text);
    codeText.setFont(BrowserMenuBar.getInstance().getScriptFont());
    codeText.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    codeText.setCaretPosition(0);
    codeText.setLineWrap(false);
    codeText.getDocument().addDocumentListener(this);
    JScrollPane scrollCompiled = new JScrollPane(codeText);
    scrollCompiled.setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlDkShadow")));

    bdecompile = new JButton("Decompile", Icons.getIcon("Undo16.gif"));
    bdecompile.setMnemonic('d');
    bdecompile.addActionListener(this);
    JPanel compiledPanelButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
    compiledPanelButtons.add(bdecompile);

    JPanel compiledPanel = new JPanel(new BorderLayout());
    compiledPanel.add(scrollCompiled, BorderLayout.CENTER);
    compiledPanel.add(compiledPanelButtons, BorderLayout.SOUTH);

    ifindall = new JMenuItem("in all scripts");
    ifindthis = new JMenuItem("in this script only");
    ifindusage = new JMenuItem("references to this script");
    bsave = new JButton("Save", Icons.getIcon("Save16.gif"));
    bsave.setMnemonic('a');
    iexportscript = new JMenuItem("script code");
    iexportsource = new JMenuItem("script source");
    iexportscript.setToolTipText("NB! Will export last *saved* version");
    bexport = new ButtonPopupMenu("Export...", new JMenuItem[]{iexportscript, iexportsource});
    bexport.setIcon(Icons.getIcon("Export16.gif"));
    bfind = new ButtonPopupMenu("Find...", new JMenuItem[]{ifindall, ifindthis, ifindusage});
    bfind.setIcon(Icons.getIcon("Find16.gif"));
    buses = new ButtonPopupMenu("Uses...", new JMenuItem[0]);
    buses.setIcon(Icons.getIcon("Find16.gif"));
    bfind.addItemListener(this);
    buses.addItemListener(this);
    bsave.addActionListener(this);
    bexport.addItemListener(this);

    tabbedPane = new JTabbedPane();
    tabbedPane.addTab("Script source (decompiled)", decompiledPanel);
    tabbedPane.addTab("Script code", compiledPanel);

    JPanel lowerpanel = new JPanel();
    lowerpanel.setLayout(new FlowLayout(FlowLayout.CENTER, 6, 6));
    lowerpanel.add(bfind);
    lowerpanel.add(buses);
    lowerpanel.add(bexport);
    lowerpanel.add(bsave);

    panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(tabbedPane, BorderLayout.CENTER);
    panel.add(lowerpanel, BorderLayout.SOUTH);

    bdecompile.doClick();
    bcompile.setEnabled(true);
    if (BrowserMenuBar.getInstance().autocheckBCS()) {
      bcompile.doClick();
      codeChanged = false;
    }
    else {
      berrors.setEnabled(false);
      bwarnings.setEnabled(false);
    }
    bdecompile.setEnabled(false);
    bsave.setEnabled(false);

    return panel;
  }

// --------------------- End Interface Viewable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    if (codeText == null)
      Filewriter.writeString(os, text, text.length());
    else
      Filewriter.writeString(os, codeText.getText(), codeText.getText().length());
  }

// --------------------- End Interface Writeable ---------------------

  public String getCode()
  {
    return text;
  }

  public void insertString(String s)
  {
    int pos = sourceText.getCaret().getDot();
    sourceText.insert(s, pos);
  }
}

