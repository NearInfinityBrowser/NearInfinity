// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.bcs;

import infinity.gui.BrowserMenuBar;
import infinity.gui.ButtonPanel;
import infinity.gui.ButtonPopupMenu;
import infinity.gui.InfinityScrollPane;
import infinity.gui.InfinityTextArea;
import infinity.gui.ScriptTextArea;
import infinity.gui.ViewFrame;
import infinity.icon.Icons;
import infinity.resource.Closeable;
import infinity.resource.ResourceFactory;
import infinity.resource.TextResource;
import infinity.resource.ViewableContainer;
import infinity.resource.Writeable;
import infinity.resource.key.BIFFResourceEntry;
import infinity.resource.key.ResourceEntry;
import infinity.search.ScriptReferenceSearcher;
import infinity.search.TextResourceSearcher;
import infinity.util.Decryptor;
import infinity.util.io.FileNI;
import infinity.util.io.FileOutputStreamNI;
import infinity.util.io.FileWriterNI;
import infinity.util.io.PrintWriterNI;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

public final class BcsResource implements TextResource, Writeable, Closeable, ActionListener, ItemListener,
                                          DocumentListener
{
  private static final boolean DEBUG = false;

  // for decompile panel
  private static final ButtonPanel.Control CtrlCompile    = ButtonPanel.Control.Custom1;
  private static final ButtonPanel.Control CtrlErrors     = ButtonPanel.Control.Custom2;
  private static final ButtonPanel.Control CtrlWarnings   = ButtonPanel.Control.Custom3;
  // for compiled panel
  private static final ButtonPanel.Control CtrlDecompile  = ButtonPanel.Control.Custom1;
  // for button panel
  private static final ButtonPanel.Control CtrlUses       = ButtonPanel.Control.Custom1;

  private static JFileChooser chooser;
  private final ResourceEntry entry;
  private final ButtonPanel buttonPanel = new ButtonPanel();
  private final ButtonPanel bpDecompile = new ButtonPanel();
  private final ButtonPanel bpCompiled = new ButtonPanel();

  private JMenuItem ifindall, ifindthis, ifindusage, iexportsource, iexportscript;
  private JPanel panel;
  private JTabbedPane tabbedPane;
  private InfinityTextArea codeText;
  private ScriptTextArea sourceText;
  private String text;
  private boolean sourceChanged = false, codeChanged = false;

//  public static void main(String args[]) throws IOException
//  {
//    new ResourceFactory(new File("CHITIN.KEY"));
//    List<ResourceEntry> bcsfiles = ResourceFactory.getInstance().getResources("BCS");
//    bcsfiles.addAll(ResourceFactory.getInstance().getResources("BS"));
//    PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter("diff.txt")));
//    long start = System.currentTimeMillis();
//    for (int i = bcsfiles.size() - 1; i >= 0; i--) {
//      try {
//        BcsResource bcs = new BcsResource(bcsfiles.get(i));
//        String recompiled = Compiler.getInstance().compile(Decompiler.decompile(bcs.text, true));
//        if (Compiler.getInstance().getErrors().size() > 0) {
//          System.out.println("Errors in " + bcs.entry.toString());
//          pw.println(bcs.entry.toString());
//          for (final String error : Compiler.getInstance().getErrors().values())
//            pw.println(error);
//        }
//        else if (!recompiled.equals(bcs.text)) {
//          int index = bcs.text.indexOf("\r\n");
//          while (index != -1) {
//            bcs.text = bcs.text.substring(0, index) + '\n' + bcs.text.substring(index + 2);
//            index = bcs.text.indexOf("\r\n");
//          }
//          if (!recompiled.equals(bcs.text)) {
//            System.out.println("Difference in " + bcs.entry.toString());
//            pw.println(bcs.entry.toString());
//          }
//        }
//      } catch (Exception e) {
//        System.out.println("Exception in " + bcsfiles.get(i).toString());
//        pw.println(bcsfiles.get(i).toString());
//        e.printStackTrace(pw);
//      }
//      if (i == 10 * (i / 10))
//        System.out.println(i + " scripts left");
//    }
//    pw.close();
//    System.out.println("Test took " + (System.currentTimeMillis() - start) + " ms");
//    System.exit(0);
//  }

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

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (bpDecompile.getControlByType(CtrlCompile) == event.getSource()) {
      JButton bCompile = (JButton)event.getSource();
      JButton bDecompile = (JButton)bpCompiled.getControlByType(CtrlDecompile);
      ButtonPopupMenu bpmErrors = (ButtonPopupMenu)bpDecompile.getControlByType(CtrlErrors);
      ButtonPopupMenu bpmWarnings = (ButtonPopupMenu)bpDecompile.getControlByType(CtrlWarnings);
      try {
        if (DEBUG) {
          BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStreamNI("bcs_org.txt"));
          write(bos);
          bos.close();
        }
        codeText.setText(Compiler.getInstance().compile(sourceText.getText()));
        codeText.setCaretPosition(0);
        bCompile.setEnabled(false);
        bDecompile.setEnabled(false);
        sourceChanged = false;
        codeChanged = true;
        if (DEBUG) {
          BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStreamNI("bcs_new.txt"));
          write(bos);
          bos.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      iexportscript.setEnabled(Compiler.getInstance().getErrors().size() == 0);
      SortedMap<Integer, String> errorMap = Compiler.getInstance().getErrors();
      SortedMap<Integer, String> warningMap = Compiler.getInstance().getWarnings();
      bpmErrors.setText("Errors (" + errorMap.size() + ")...");
      bpmWarnings.setText("Warnings (" + warningMap.size() + ")...");
      if (errorMap.size() == 0) {
        bpmErrors.setEnabled(false);
      } else {
        JMenuItem errorItems[] = new JMenuItem[errorMap.size()];
        int counter = 0;
        for (final Integer lineNr : errorMap.keySet()) {
          String error = errorMap.get(lineNr);
          errorItems[counter++] = new JMenuItem(lineNr.toString() + ": " + error);
        }
        bpmErrors.setMenuItems(errorItems);
        bpmErrors.setEnabled(true);
      }
      if (warningMap.size() == 0) {
        bpmWarnings.setEnabled(false);
      } else {
        JMenuItem warningItems[] = new JMenuItem[warningMap.size()];
        int counter = 0;
        for (final Integer lineNr : warningMap.keySet()) {
          String warning = warningMap.get(lineNr);
          warningItems[counter++] = new JMenuItem(lineNr.toString() + ": " + warning);
        }
        bpmWarnings.setMenuItems(warningItems);
        bpmWarnings.setEnabled(true);
      }
    } else if (bpCompiled.getControlByType(CtrlDecompile) == event.getSource()) {
      JButton bDecompile = (JButton)event.getSource();
      JButton bCompile = (JButton)bpDecompile.getControlByType(CtrlCompile);
      ButtonPopupMenu bpmUses = (ButtonPopupMenu)buttonPanel.getControlByType(CtrlUses);

      sourceText.setText(Decompiler.decompile(codeText.getText(), true));
      sourceText.setCaretPosition(0);
      Set<ResourceEntry> uses = Decompiler.getResourcesUsed();
      JMenuItem usesItems[] = new JMenuItem[uses.size()];
      int usesIndex = 0;
      for (final ResourceEntry usesEntry : uses) {
        if (usesEntry.getSearchString() != null) {
          usesItems[usesIndex++] =
          new JMenuItem(usesEntry.toString() + " (" + usesEntry.getSearchString() + ')');
        } else {
          usesItems[usesIndex++] = new JMenuItem(usesEntry.toString());
        }
      }
      bpmUses.setMenuItems(usesItems);
      bpmUses.setEnabled(usesItems.length > 0);
      bCompile.setEnabled(false);
      bDecompile.setEnabled(false);
      sourceChanged = false;
      tabbedPane.setSelectedIndex(0);
    } else if (buttonPanel.getControlByType(ButtonPanel.Control.Save) == event.getSource()) {
      JButton bSave = (JButton)event.getSource();
      ButtonPopupMenu bpmErrors = (ButtonPopupMenu)bpDecompile.getControlByType(CtrlErrors);
      if (bpmErrors.isEnabled()) {
        String options[] = {"Save", "Cancel"};
        int result = JOptionPane.showOptionDialog(panel, "Script contains errors. Save anyway?", "Errors found",
                                                  JOptionPane.YES_NO_OPTION,
                                                  JOptionPane.WARNING_MESSAGE, null, options, options[0]);
        if (result != 0) {
          return;
        }
      }
      if (ResourceFactory.getInstance().saveResource(this, panel.getTopLevelAncestor())) {
        bSave.setEnabled(false);
        sourceChanged = false;
        codeChanged = false;
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Closeable ---------------------

  @Override
  public void close() throws Exception
  {
    if (sourceChanged) {
      String options[] = {"Compile & save", "Discard changes", "Cancel"};
      int result = JOptionPane.showOptionDialog(panel, "Script contains uncompiled changes", "Uncompiled changes",
                                                JOptionPane.YES_NO_CANCEL_OPTION,
                                                JOptionPane.WARNING_MESSAGE, null, options, options[0]);
      if (result == 0) {
        ((JButton)bpDecompile.getControlByType(CtrlCompile)).doClick();
        if (bpDecompile.getControlByType(CtrlErrors).isEnabled()) {
          throw new Exception("Save aborted");
        }
        ResourceFactory.getInstance().saveResource(this, panel.getTopLevelAncestor());
      } else if (result == 2 || result == JOptionPane.CLOSED_OPTION)
        throw new Exception("Save aborted");
    } else if (codeChanged) {
      File output;
      if (entry instanceof BIFFResourceEntry) {
        output =
            FileNI.getFile(ResourceFactory.getRootDirs(),
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
    if (event.getDocument() == codeText.getDocument()) {
      buttonPanel.getControlByType(ButtonPanel.Control.Save).setEnabled(true);
      bpCompiled.getControlByType(CtrlDecompile).setEnabled(true);
      sourceChanged = false;
      codeChanged = true;
    }
    else if (event.getDocument() == sourceText.getDocument()) {
      bpDecompile.getControlByType(CtrlCompile).setEnabled(true);
      sourceChanged = true;
    }
  }

  @Override
  public void removeUpdate(DocumentEvent event)
  {
    if (event.getDocument() == codeText.getDocument()) {
      buttonPanel.getControlByType(ButtonPanel.Control.Save).setEnabled(true);
      bpCompiled.getControlByType(CtrlDecompile).setEnabled(true);
      sourceChanged = false;
      codeChanged = true;
    }
    else if (event.getDocument() == sourceText.getDocument()) {
      bpDecompile.getControlByType(CtrlCompile).setEnabled(true);
      sourceChanged = true;
    }
  }

  @Override
  public void changedUpdate(DocumentEvent event)
  {
    if (event.getDocument() == codeText.getDocument()) {
      buttonPanel.getControlByType(ButtonPanel.Control.Save).setEnabled(true);
      bpCompiled.getControlByType(CtrlDecompile).setEnabled(true);
      sourceChanged = false;
      codeChanged = true;
    }
    else if (event.getDocument() == sourceText.getDocument()) {
      bpDecompile.getControlByType(CtrlCompile).setEnabled(true);
      sourceChanged = true;
    }
  }

// --------------------- End Interface DocumentListener ---------------------


// --------------------- Begin Interface ItemListener ---------------------

  @Override
  public void itemStateChanged(ItemEvent event)
  {
    if (buttonPanel.getControlByType(ButtonPanel.Control.FindMenu) == event.getSource()) {
      ButtonPopupMenu bpmFind = (ButtonPopupMenu)event.getSource();
      if (bpmFind.getSelectedItem() == ifindall) {
        List<ResourceEntry> files = ResourceFactory.getInstance().getResources("BCS");
        files.addAll(ResourceFactory.getInstance().getResources("BS"));
        new TextResourceSearcher(files, panel.getTopLevelAncestor());
      } else if (bpmFind.getSelectedItem() == ifindthis) {
        List<ResourceEntry> files = new ArrayList<ResourceEntry>(1);
        files.add(entry);
        new TextResourceSearcher(files, panel.getTopLevelAncestor());
      } else if (bpmFind.getSelectedItem() == ifindusage)
        new ScriptReferenceSearcher(entry, panel.getTopLevelAncestor());
    } else if (buttonPanel.getControlByType(CtrlUses) == event.getSource()) {
      ButtonPopupMenu bpmUses = (ButtonPopupMenu)event.getSource();
      JMenuItem item = bpmUses.getSelectedItem();
      String name = item.getText();
      int index = name.indexOf(" (");
      if (index != -1) {
        name = name.substring(0, index);
      }
      ResourceEntry resEntry = ResourceFactory.getInstance().getResourceEntry(name);
      new ViewFrame(panel.getTopLevelAncestor(), ResourceFactory.getResource(resEntry));
    } else if (buttonPanel.getControlByType(ButtonPanel.Control.ExportMenu) == event.getSource()) {
      ButtonPopupMenu bpmExport = (ButtonPopupMenu)event.getSource();
      if (bpmExport.getSelectedItem() == iexportsource) {
        if (chooser == null) {
          chooser = new JFileChooser(ResourceFactory.getRootDir());
          chooser.setDialogTitle("Export source");
          chooser.setFileFilter(new FileFilter()
          {
            @Override
            public boolean accept(File pathname)
            {
              return pathname.isDirectory() || pathname.getName().toLowerCase().endsWith(".baf");
            }

            @Override
            public String getDescription()
            {
              return "Infinity script (.BAF)";
            }
          });
        }
        chooser.setSelectedFile(
                new FileNI(entry.toString().substring(0, entry.toString().indexOf((int)'.')) + ".BAF"));
        int returnval = chooser.showSaveDialog(panel.getTopLevelAncestor());
        if (returnval == JFileChooser.APPROVE_OPTION) {
          try {
            PrintWriter pw = new PrintWriterNI(new FileOutputStreamNI(chooser.getSelectedFile()));
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
      } else if (bpmExport.getSelectedItem() == iexportscript) {
        ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
      }
    } else if (bpCompiled.getControlByType(CtrlErrors) == event.getSource()) {
      ButtonPopupMenu bpmErrors = (ButtonPopupMenu)event.getSource();
      String selected = bpmErrors.getSelectedItem().getText();
      int linenr = Integer.parseInt(selected.substring(0, selected.indexOf(": ")));
      highlightText(linenr, null);
    } else if (bpCompiled.getControlByType(CtrlWarnings) == event.getSource()) {
      ButtonPopupMenu bpmWarnings = (ButtonPopupMenu)event.getSource();
      String selected = bpmWarnings.getSelectedItem().getText();
      int linenr = Integer.parseInt(selected.substring(0, selected.indexOf(": ")));
      highlightText(linenr, null);
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
    if (sourceText != null)
      return sourceText.getText();
    return Decompiler.decompile(text, false);
  }

  @Override
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

  @Override
  public JComponent makeViewer(ViewableContainer container)
  {
    sourceText = new ScriptTextArea();
    sourceText.addCaretListener(container.getStatusBar());
    sourceText.setFont(BrowserMenuBar.getInstance().getScriptFont());
    sourceText.setMargin(new Insets(3, 3, 3, 3));
    sourceText.setLineWrap(false);
    sourceText.getDocument().addDocumentListener(this);
    InfinityScrollPane scrollDecompiled = new InfinityScrollPane(sourceText, true);
    scrollDecompiled.setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlDkShadow")));

    JButton bCompile = new JButton("Compile", Icons.getIcon("Redo16.gif"));
    bCompile.setMnemonic('c');
    bCompile.addActionListener(this);
    ButtonPopupMenu bpmErrors = new ButtonPopupMenu("Errors (0)...", new JMenuItem[0]);
    bpmErrors.setIcon(Icons.getIcon("Up16.gif"));
    bpmErrors.addItemListener(this);
    ButtonPopupMenu bpmWarnings = new ButtonPopupMenu("Warnings (0)...", new JMenuItem[0]);
    bpmWarnings.setIcon(Icons.getIcon("Up16.gif"));
    bpmWarnings.addItemListener(this);
    bpDecompile.addControl(bCompile, CtrlCompile);
    bpDecompile.addControl(bpmErrors, CtrlErrors);
    bpDecompile.addControl(bpmWarnings, CtrlWarnings);

    JPanel decompiledPanel = new JPanel(new BorderLayout());
    decompiledPanel.add(scrollDecompiled, BorderLayout.CENTER);
    decompiledPanel.add(bpDecompile, BorderLayout.SOUTH);

    codeText = new InfinityTextArea(text, true);
    codeText.setFont(BrowserMenuBar.getInstance().getScriptFont());
    codeText.setMargin(new Insets(3, 3, 3, 3));
    codeText.setCaretPosition(0);
    codeText.setLineWrap(false);
    codeText.getDocument().addDocumentListener(this);
    InfinityScrollPane scrollCompiled = new InfinityScrollPane(codeText, true);
    scrollCompiled.setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlDkShadow")));

    JButton bDecompile = new JButton("Decompile", Icons.getIcon("Undo16.gif"));
    bDecompile.setMnemonic('d');
    bDecompile.addActionListener(this);
    bpCompiled.addControl(bDecompile, CtrlDecompile);

    JPanel compiledPanel = new JPanel(new BorderLayout());
    compiledPanel.add(scrollCompiled, BorderLayout.CENTER);
    compiledPanel.add(bpCompiled, BorderLayout.SOUTH);

    ifindall = new JMenuItem("in all scripts");
    ifindthis = new JMenuItem("in this script only");
    ifindusage = new JMenuItem("references to this script");
    ButtonPopupMenu bpmFind = (ButtonPopupMenu)buttonPanel.addControl(ButtonPanel.Control.FindMenu);
    bpmFind.setMenuItems(new JMenuItem[]{ifindall, ifindthis, ifindusage});
    bpmFind.addItemListener(this);
    ButtonPopupMenu bpmUses = new ButtonPopupMenu("Uses...", new JMenuItem[]{});
    bpmUses.setIcon(Icons.getIcon("Find16.gif"));
    bpmUses.addItemListener(this);
    buttonPanel.addControl(bpmUses, CtrlUses);
    iexportscript = new JMenuItem("script code");
    iexportsource = new JMenuItem("script source");
    iexportscript.setToolTipText("NB! Will export last *saved* version");
    ButtonPopupMenu bpmExport = (ButtonPopupMenu)buttonPanel.addControl(ButtonPanel.Control.ExportMenu);
    bpmExport.setMenuItems(new JMenuItem[]{iexportscript, iexportsource});
    bpmExport.addItemListener(this);
    JButton bSave = (JButton)buttonPanel.addControl(ButtonPanel.Control.Save);
    bSave.addActionListener(this);

    tabbedPane = new JTabbedPane();
    tabbedPane.addTab("Script source (decompiled)", decompiledPanel);
    tabbedPane.addTab("Script code", compiledPanel);

    panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(tabbedPane, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.SOUTH);

    bDecompile.doClick();
    bCompile.setEnabled(true);
    if (BrowserMenuBar.getInstance().autocheckBCS()) {
      bCompile.doClick();
      codeChanged = false;
    }
    else {
      bpmErrors.setEnabled(false);
      bpmWarnings.setEnabled(false);
    }
    bDecompile.setEnabled(false);
    bSave.setEnabled(false);

    return panel;
  }

// --------------------- End Interface Viewable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    if (codeText == null)
      FileWriterNI.writeString(os, text, text.length());
    else
      FileWriterNI.writeString(os, codeText.getText(), codeText.getText().length());
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

