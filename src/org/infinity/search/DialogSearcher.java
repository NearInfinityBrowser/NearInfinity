// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.infinity.datatype.StringRef;
import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.bcs.Compiler;
import org.infinity.resource.bcs.Decompiler;
import org.infinity.resource.bcs.ScriptType;
import org.infinity.resource.dlg.AbstractCode;
import org.infinity.resource.dlg.Action;
import org.infinity.resource.key.ResourceEntry;

public final class DialogSearcher extends AbstractSearcher implements Runnable, ActionListener
{
  private final ChildFrame inputFrame;
  private final JButton bsearch = new JButton("Search", Icons.getIcon(Icons.ICON_FIND_AGAIN_16));
  private final JCheckBox cbwhole = new JCheckBox("Match whole word only");
  private final JCheckBox cbcase = new JCheckBox("Match case");
  private final JCheckBox cbsearchcode = new JCheckBox("Include trigger & action scripts", true);
  private final JCheckBox cbregex = new JCheckBox("Use regular expressions");
  private final JTextField tfinput = new JTextField("", 15);
  private final List<ResourceEntry> files;

  private Pattern regPattern;
  private ReferenceHitFrame resultFrame;

  public DialogSearcher(List<ResourceEntry> files, Component parent)
  {
    super(SEARCH_ONE_TYPE_FORMAT, parent);
    this.files = files;
    String title = "Find: DLG files";
    if (files.size() == 1)
      title = "Find: " + files.get(0).toString();
    inputFrame = new ChildFrame(title, true);
    inputFrame.setIconImage(Icons.getIcon(Icons.ICON_FIND_16).getImage());
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
    matchpanel.add(cbregex);
    cbwhole.setMnemonic('w');
    cbcase.setMnemonic('m');
    cbregex.setMnemonic('r');

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

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bsearch || event.getSource() == tfinput) {
      inputFrame.setVisible(false);
      new Thread(this).start();
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Runnable ---------------------

  @Override
  public void run()
  {
    String term = tfinput.getText();
    if (!cbregex.isSelected()) {
      term = Pattern.quote(term);
    }
    if (cbwhole.isSelected()) {
      term = ".*\\b" + term + "\\b.*";
    } else {
      term = ".*" + term + ".*";
    }

    try {
      if (cbcase.isSelected()) {
        regPattern = Pattern.compile(term, Pattern.DOTALL);
      } else {
        regPattern = Pattern.compile(term, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
      }
    } catch (PatternSyntaxException e) {
      JOptionPane.showMessageDialog(parent, "Syntax error in search string.", "Error", JOptionPane.ERROR_MESSAGE);
      regPattern = null;
      return;
    }

    try {
      // executing multithreaded search
      inputFrame.setVisible(false);
      resultFrame = new ReferenceHitFrame(term, parent);
      if (runSearch("Searching", files)) {
        resultFrame.close();
      } else {
        resultFrame.setVisible(true);
      }
    } finally {
      regPattern = null;
      resultFrame = null;
    }
  }

// --------------------- End Interface Runnable ---------------------

  @Override
  protected Runnable newWorker(ResourceEntry entry)
  {
    return () -> {
      final Resource resource = ResourceFactory.getResource(entry);
      if (resource instanceof AbstractStruct) {
        final Map<StructEntry, StructEntry> searchMap = makeSearchMap((AbstractStruct)resource);
        for (final Map.Entry<StructEntry, StructEntry> e : searchMap.entrySet()) {
          final StructEntry searchEntry = e.getKey();
          String s = null;
          if (searchEntry instanceof StringRef) {
            s = searchEntry.toString();
          } else if (searchEntry instanceof AbstractCode) {
            try {
              final AbstractCode code = (AbstractCode)searchEntry;
              final ScriptType type = searchEntry instanceof Action ? ScriptType.ACTION : ScriptType.TRIGGER;
              final Compiler compiler = new Compiler(code.getText(), type);

              if (compiler.getErrors().isEmpty()) {
                final Decompiler decompiler = new Decompiler(compiler.getCode(), type, false);
                decompiler.setGenerateComments(false);
                decompiler.setGenerateResourcesUsed(false);
                s = decompiler.getSource();
              } else {
                synchronized (System.err) {
                  System.err.println("Error(s) compiling " + entry.toString() + " - " + searchEntry.getName());
                }
              }
            } catch (Exception ex) {
              synchronized (System.err) {
                System.err.println("Exception (de)compiling " + entry.toString() + " - " + searchEntry.getName());
                ex.printStackTrace();
              }
            }
            if (s == null) {
              s = "";
            }
          }
          final Matcher matcher = regPattern.matcher(s);
          if (matcher.matches()) {
            addResult(entry, e.getValue().getName(), searchEntry);
          }
        }
      }
      advanceProgress();
    };
  }

  private Map<StructEntry, StructEntry> makeSearchMap(AbstractStruct struct)
  {
    final SortedMap<StructEntry, StructEntry> map = new TreeMap<>();
    for (final StructEntry entry : struct.getFields()) {
      if (entry instanceof AbstractStruct)
        map.putAll(makeSearchMap((AbstractStruct)entry));
      else if (cbsearchcode.isSelected() && entry instanceof AbstractCode)
        map.put(entry, entry);
      else if (entry instanceof StringRef)
        map.put(entry, struct);
    }
    return map;
  }

  private synchronized void addResult(ResourceEntry entry, String line, StructEntry ref)
  {
    if (resultFrame != null) {
      resultFrame.addHit(entry, line, ref);
    }
  }
}
