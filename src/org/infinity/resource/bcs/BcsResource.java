// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.bcs;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.ButtonPanel;
import org.infinity.gui.ButtonPopupMenu;
import org.infinity.gui.InfinityScrollPane;
import org.infinity.gui.InfinityTextArea;
import org.infinity.gui.ScriptTextArea;
import org.infinity.gui.ViewFrame;
import org.infinity.icon.Icons;
import org.infinity.resource.Closeable;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.TextResource;
import org.infinity.resource.ViewableContainer;
import org.infinity.resource.Writeable;
import org.infinity.resource.key.BIFFResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.ScriptReferenceSearcher;
import org.infinity.search.TextResourceSearcher;
import org.infinity.util.Decryptor;
import org.infinity.util.Misc;
import org.infinity.util.io.FileManager;
import org.infinity.util.io.StreamUtils;

public final class BcsResource implements TextResource, Writeable, Closeable, ActionListener, ItemListener,
                                          DocumentListener
{
  // for decompile panel
  private static final ButtonPanel.Control CtrlCompile    = ButtonPanel.Control.CUSTOM_1;
  private static final ButtonPanel.Control CtrlErrors     = ButtonPanel.Control.CUSTOM_2;
  private static final ButtonPanel.Control CtrlWarnings   = ButtonPanel.Control.CUSTOM_3;
  // for compiled panel
  private static final ButtonPanel.Control CtrlDecompile  = ButtonPanel.Control.CUSTOM_1;
  // for button panel
  private static final ButtonPanel.Control CtrlUses       = ButtonPanel.Control.CUSTOM_1;

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

  public BcsResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    ByteBuffer buffer = entry.getResourceBuffer();
    if (buffer.limit() > 1 && buffer.getShort(0) == -1) {
      buffer = Decryptor.decrypt(buffer, 2);
    }
    text = StreamUtils.readString(buffer, buffer.limit());
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
      Compiler compiler = new Compiler(sourceText.getText());
      codeText.setText(compiler.getCode());
      codeText.setCaretPosition(0);
      bCompile.setEnabled(false);
      bDecompile.setEnabled(false);
      sourceChanged = false;
      codeChanged = true;
      iexportscript.setEnabled(compiler.getErrors().size() == 0);
      SortedMap<Integer, String> errorMap = compiler.getErrors();
      SortedMap<Integer, String> warningMap = compiler.getWarnings();
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

      Decompiler decompiler = new Decompiler(codeText.getText(), true);
      sourceText.setText(decompiler.getSource());
      sourceText.setCaretPosition(0);
      Set<ResourceEntry> uses = decompiler.getResourcesUsed();
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
    } else if (buttonPanel.getControlByType(ButtonPanel.Control.SAVE) == event.getSource()) {
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
      if (ResourceFactory.saveResource(this, panel.getTopLevelAncestor())) {
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
        ResourceFactory.saveResource(this, panel.getTopLevelAncestor());
      } else if (result == 2 || result == JOptionPane.CLOSED_OPTION)
        throw new Exception("Save aborted");
    } else if (codeChanged) {
      Path output;
      if (entry instanceof BIFFResourceEntry) {
        output = FileManager.query(Profile.getRootFolders(), Profile.getOverrideFolderName(), entry.toString());
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
    if (event.getDocument() == codeText.getDocument()) {
      buttonPanel.getControlByType(ButtonPanel.Control.SAVE).setEnabled(true);
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
      buttonPanel.getControlByType(ButtonPanel.Control.SAVE).setEnabled(true);
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
      buttonPanel.getControlByType(ButtonPanel.Control.SAVE).setEnabled(true);
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
    if (buttonPanel.getControlByType(ButtonPanel.Control.FIND_MENU) == event.getSource()) {
      ButtonPopupMenu bpmFind = (ButtonPopupMenu)event.getSource();
      if (bpmFind.getSelectedItem() == ifindall) {
        List<ResourceEntry> files = ResourceFactory.getResources("BCS");
        files.addAll(ResourceFactory.getResources("BS"));
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
      ResourceEntry resEntry = ResourceFactory.getResourceEntry(name);
      new ViewFrame(panel.getTopLevelAncestor(), ResourceFactory.getResource(resEntry));
    } else if (buttonPanel.getControlByType(ButtonPanel.Control.EXPORT_MENU) == event.getSource()) {
      ButtonPopupMenu bpmExport = (ButtonPopupMenu)event.getSource();
      if (bpmExport.getSelectedItem() == iexportsource) {
        if (chooser == null) {
          chooser = new JFileChooser(Profile.getGameRoot().toFile());
          chooser.setDialogTitle("Export source");
          chooser.setFileFilter(new FileFilter()
          {
            @Override
            public boolean accept(File pathname)
            {
              return pathname.isDirectory() || pathname.getName().toLowerCase(Locale.ENGLISH).endsWith(".baf");
            }

            @Override
            public String getDescription()
            {
              return "Infinity script (.BAF)";
            }
          });
        }
        chooser.setSelectedFile(new File(StreamUtils.replaceFileExtension(entry.toString(), "BAF")));
        int returnval = chooser.showSaveDialog(panel.getTopLevelAncestor());
        if (returnval == JFileChooser.APPROVE_OPTION) {
          try (BufferedWriter bw = Files.newBufferedWriter(chooser.getSelectedFile().toPath())) {
            bw.write(sourceText.getText().replaceAll("\r?\n", Misc.LINE_SEPARATOR));
            bw.newLine();
            JOptionPane.showMessageDialog(panel, "File saved to \"" + chooser.getSelectedFile().toString() +
                                                 '\"', "Export complete", JOptionPane.INFORMATION_MESSAGE);
          } catch (IOException e) {
            JOptionPane.showMessageDialog(panel, "Error exporting " + chooser.getSelectedFile().toString(),
                                          "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
          }
        }
      } else if (bpmExport.getSelectedItem() == iexportscript) {
        ResourceFactory.exportResource(entry, panel.getTopLevelAncestor());
      }
    } else if (bpDecompile.getControlByType(CtrlErrors) == event.getSource()) {
      ButtonPopupMenu bpmErrors = (ButtonPopupMenu)event.getSource();
      String selected = bpmErrors.getSelectedItem().getText();
      int linenr = Integer.parseInt(selected.substring(0, selected.indexOf(": ")));
      highlightText(linenr, null);
    } else if (bpDecompile.getControlByType(CtrlWarnings) == event.getSource()) {
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
    if (sourceText != null) {
      return sourceText.getText();
    }
    Decompiler decompiler = new Decompiler(text, false);
    return decompiler.getSource();
  }

  @Override
  public void highlightText(int linenr, String highlightText)
  {
    String s = sourceText.getText();
    int startpos = 0;
    int i = (s.charAt(0) == '\n') ? 2 : 1;
    for (; i < linenr; i++) {
      startpos = s.indexOf("\n", startpos + 1);
    }
    if (startpos == -1) return;
    if (highlightText != null) {
      // try to select specified text string
      int wordpos = -1;
      if (highlightText != null) {
        wordpos = s.toUpperCase(Locale.ENGLISH).indexOf(highlightText.toUpperCase(Locale.ENGLISH), startpos);
      }
      if (wordpos != -1) {
        sourceText.select(wordpos, wordpos + highlightText.length());
      } else {
        sourceText.select(startpos, s.indexOf("\n", startpos + 1));
      }
    } else {
      // select whole line
      int endpos = s.indexOf("\n", startpos + 1);
      if (endpos < 0) {
        endpos = s.length();
      }
      sourceText.select(startpos, endpos);
    }
    sourceText.getCaret().setSelectionVisible(true);
  }

// --------------------- End Interface TextResource ---------------------


// --------------------- Begin Interface Viewable ---------------------

  @Override
  public JComponent makeViewer(ViewableContainer container)
  {
    sourceText = new ScriptTextArea();
    sourceText.setAutoIndentEnabled(BrowserMenuBar.getInstance().getBcsAutoIndentEnabled());
    sourceText.addCaretListener(container.getStatusBar());
    sourceText.setFont(BrowserMenuBar.getInstance().getScriptFont());
    sourceText.setMargin(new Insets(3, 3, 3, 3));
    sourceText.setLineWrap(false);
    sourceText.getDocument().addDocumentListener(this);
    InfinityScrollPane scrollDecompiled = new InfinityScrollPane(sourceText, true);
    scrollDecompiled.setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlDkShadow")));

    JButton bCompile = new JButton("Compile", Icons.getIcon(Icons.ICON_REDO_16));
    bCompile.setMnemonic('c');
    bCompile.addActionListener(this);
    ButtonPopupMenu bpmErrors = new ButtonPopupMenu("Errors (0)...", new JMenuItem[0]);
    bpmErrors.setIcon(Icons.getIcon(Icons.ICON_UP_16));
    bpmErrors.addItemListener(this);
    ButtonPopupMenu bpmWarnings = new ButtonPopupMenu("Warnings (0)...", new JMenuItem[0]);
    bpmWarnings.setIcon(Icons.getIcon(Icons.ICON_UP_16));
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

    JButton bDecompile = new JButton("Decompile", Icons.getIcon(Icons.ICON_UNDO_16));
    bDecompile.setMnemonic('d');
    bDecompile.addActionListener(this);
    bpCompiled.addControl(bDecompile, CtrlDecompile);

    JPanel compiledPanel = new JPanel(new BorderLayout());
    compiledPanel.add(scrollCompiled, BorderLayout.CENTER);
    compiledPanel.add(bpCompiled, BorderLayout.SOUTH);

    ifindall = new JMenuItem("in all scripts");
    ifindthis = new JMenuItem("in this script only");
    ifindusage = new JMenuItem("references to this script");
    ButtonPopupMenu bpmFind = (ButtonPopupMenu)buttonPanel.addControl(ButtonPanel.Control.FIND_MENU);
    bpmFind.setMenuItems(new JMenuItem[]{ifindall, ifindthis, ifindusage});
    bpmFind.addItemListener(this);
    ButtonPopupMenu bpmUses = new ButtonPopupMenu("Uses...", new JMenuItem[]{});
    bpmUses.setIcon(Icons.getIcon(Icons.ICON_FIND_16));
    bpmUses.addItemListener(this);
    buttonPanel.addControl(bpmUses, CtrlUses);
    iexportscript = new JMenuItem("script code");
    iexportsource = new JMenuItem("script source");
    iexportscript.setToolTipText("NB! Will export last *saved* version");
    ButtonPopupMenu bpmExport = (ButtonPopupMenu)buttonPanel.addControl(ButtonPanel.Control.EXPORT_MENU);
    bpmExport.setMenuItems(new JMenuItem[]{iexportscript, iexportsource});
    bpmExport.addItemListener(this);
    JButton bSave = (JButton)buttonPanel.addControl(ButtonPanel.Control.SAVE);
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
    if (codeText == null) {
      StreamUtils.writeString(os, text, text.length());
    } else {
      StreamUtils.writeString(os, codeText.getText(), codeText.getText().length());
    }
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

