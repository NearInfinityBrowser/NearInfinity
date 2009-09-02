// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.search;

import infinity.datatype.StringRef;
import infinity.gui.Center;
import infinity.gui.ChildFrame;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.bcs.Compiler;
import infinity.resource.bcs.Decompiler;
import infinity.resource.dlg.AbstractCode;
import infinity.resource.dlg.Action;
import infinity.resource.key.ResourceEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DialogSearcher implements Runnable, ActionListener
{
  private final ChildFrame inputFrame;
  private final Component parent;
  private final JButton bsearch = new JButton("Search", Icons.getIcon("FindAgain16.gif"));
  private final JCheckBox cbwhole = new JCheckBox("Match whole word only");
  private final JCheckBox cbcase = new JCheckBox("Match case");
  private final JCheckBox cbsearchcode = new JCheckBox("Include trigger & action scripts", true);
  private final JTextField tfinput = new JTextField("", 15);
  private final List<ResourceEntry> files;

  public DialogSearcher(List<ResourceEntry> files, Component parent)
  {
    this.files = files;
    this.parent = parent;
    String title = "Find: DLG files";
    if (files.size() == 1)
      title = "Find: " + files.get(0).toString();
    inputFrame = new ChildFrame(title, true);
    inputFrame.setIconImage(Icons.getIcon("Find16.gif").getImage());
    inputFrame.getRootPane().setDefaultButton(bsearch);
    bsearch.addActionListener(this);
    tfinput.addActionListener(this);

    Container pane = inputFrame.getContentPane();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    pane.setLayout(gbl);
    JLabel label = new JLabel("Find what:");
    label.setLabelFor(tfinput);
    label.setDisplayedMnemonic('f');
    JPanel matchpanel = new JPanel();
    matchpanel.setLayout(new GridLayout(2, 2));
    matchpanel.add(cbwhole);
    matchpanel.add(cbsearchcode);
    matchpanel.add(cbcase);
    cbwhole.setMnemonic('w');
    cbcase.setMnemonic('m');

    gbc.insets = new Insets(6, 6, 3, 3);
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbl.setConstraints(label, gbc);
    pane.add(label);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.gridwidth = 2;
    gbc.insets.left = 3;
    gbl.setConstraints(tfinput, gbc);
    pane.add(tfinput);

    gbc.weightx = 0.0;
    gbc.insets.right = 6;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(bsearch, gbc);
    pane.add(bsearch);

    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.insets = new Insets(3, 6, 6, 6);
    gbl.setConstraints(matchpanel, gbc);
    pane.add(matchpanel);

    inputFrame.pack();
    Center.center(inputFrame, parent.getBounds());
    inputFrame.setVisible(true);
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bsearch || event.getSource() == tfinput) {
      inputFrame.setVisible(false);
      new Thread(this).start();
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Runnable ---------------------

  public void run()
  {
    String term = tfinput.getText();
    ReferenceHitFrame resultFrame = new ReferenceHitFrame(term, parent);
    term = term.replaceAll("(\\W)", "\\\\$1");
    if (cbwhole.isSelected())
      term = ".*\\b" + term + "\\b.*";
    else
      term = ".*" + term + ".*";
    Pattern regPattern = Pattern.compile(term, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    if (cbcase.isSelected())
      regPattern = Pattern.compile(term, Pattern.DOTALL);
    inputFrame.setVisible(false);
    ProgressMonitor progress = new ProgressMonitor(parent, "Searching...", null, 0, files.size());
    progress.setMillisToDecideToPopup(100);
    for (int i = 0; i < files.size(); i++) {
      ResourceEntry entry = files.get(i);
      Resource resource = ResourceFactory.getResource(entry);
      if (resource != null) {
        Map<StructEntry, StructEntry> searchMap = makeSearchMap((AbstractStruct)resource);
        for (final StructEntry searchEntry : searchMap.keySet()) {
          String s = null;
          if (searchEntry instanceof StringRef)
            s = searchEntry.toString();
          else if (searchEntry instanceof AbstractCode) {
            try {
              String code = Compiler.getInstance().compileDialogCode(searchEntry.toString(),
                                                                     searchEntry instanceof Action);
              if (Compiler.getInstance().getErrors().size() == 0) {
                if (searchEntry instanceof Action)
                  s = Decompiler.decompileDialogAction(code, false);
                else
                  s = Decompiler.decompileDialogTrigger(code, false);
              }
              else
                System.out.println("Error(s) compiling " + entry.toString() + " - " + searchEntry.getName());
            } catch (Exception e) {
              System.out.println("Exception (de)compiling " + entry.toString() + " - " + searchEntry.getName());
              e.printStackTrace();
            }
            if (s == null)
              s = "";
          }
          Matcher matcher = regPattern.matcher(s);
          if (matcher.matches())
            resultFrame.addHit(entry, searchMap.get(searchEntry).getName(), searchEntry);
        }
      }
      progress.setProgress(i + 1);
      if (progress.isCanceled()) {
        JOptionPane.showMessageDialog(parent, "Search canceled", "Info", JOptionPane.INFORMATION_MESSAGE);
        return;
      }
    }
    resultFrame.setVisible(true);
  }

// --------------------- End Interface Runnable ---------------------

  private Map<StructEntry, StructEntry> makeSearchMap(AbstractStruct struct)
  {
    SortedMap<StructEntry, StructEntry> map = new TreeMap<StructEntry, StructEntry>();
    for (int i = 0; i < struct.getRowCount(); i++) {
      StructEntry entry = struct.getStructEntryAt(i);
      if (entry instanceof AbstractStruct)
        map.putAll(makeSearchMap((AbstractStruct)entry));
      else if (cbsearchcode.isSelected() && entry instanceof AbstractCode)
        map.put(entry, entry);
      else if (entry instanceof StringRef)
        map.put(entry, struct);
    }
    return map;
  }
}

