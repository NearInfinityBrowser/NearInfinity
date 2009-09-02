// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.search;

import infinity.datatype.DecNumber;
import infinity.gui.Center;
import infinity.gui.ChildFrame;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.dlg.AbstractCode;
import infinity.resource.key.ResourceEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.regex.Pattern;

public final class AttributeSearcher implements Runnable, ActionListener
{
  private final ChildFrame inputFrame;
  private final Component parent;
  private final JButton bsearch = new JButton("Search", Icons.getIcon("FindAgain16.gif"));
  private final JCheckBox cbwhole = new JCheckBox("Match whole word only");
  private final JCheckBox cbcase = new JCheckBox("Match case");
  private final JCheckBox cbnot = new JCheckBox("Negate result");
  private final JRadioButton rbexact = new JRadioButton("Exact match");
  private final JRadioButton rbless = new JRadioButton("Less than");
  private final JRadioButton rbgreater = new JRadioButton("Greater than");
  private final JTextField tfinput = new JTextField("", 15);
  private final List files;
  private final StructEntry structEntry;

  public AttributeSearcher(AbstractStruct struct, StructEntry structEntry, Component parent)
  {
    this.structEntry = structEntry;
    this.parent = parent;
    while (struct.getSuperStruct() != null)
      struct = struct.getSuperStruct();
    String filename = struct.getResourceEntry().toString();
    files =
    ResourceFactory.getInstance().getResources(
            filename.substring(filename.lastIndexOf(".") + 1).toUpperCase());
    inputFrame = new ChildFrame("Find: " + structEntry.getName(), true);
    inputFrame.setIconImage(Icons.getIcon("Find16.gif").getImage());
    inputFrame.getRootPane().setDefaultButton(bsearch);
    tfinput.setText(structEntry.toString());
    bsearch.addActionListener(this);
    tfinput.addActionListener(this);
    rbexact.addActionListener(this);
    rbless.addActionListener(this);
    rbgreater.addActionListener(this);
    ButtonGroup gb = new ButtonGroup();
    gb.add(rbexact);
    gb.add(rbless);
    gb.add(rbgreater);
    rbexact.setMnemonic('e');
    rbless.setMnemonic('l');
    rbgreater.setMnemonic('g');
    rbexact.setSelected(true);
    rbexact.setEnabled(structEntry instanceof DecNumber);
    rbless.setEnabled(structEntry instanceof DecNumber);
    rbgreater.setEnabled(structEntry instanceof DecNumber);
    cbcase.setEnabled(!rbexact.isEnabled());

    Container pane = inputFrame.getContentPane();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    pane.setLayout(gbl);
    JLabel label = new JLabel("Find what:");
    label.setLabelFor(tfinput);
    label.setDisplayedMnemonic('f');
    JPanel dirpanel = new JPanel();
    dirpanel.add(new JPanel());
    dirpanel.add(rbexact);
    dirpanel.add(rbless);
    dirpanel.add(rbgreater);
    dirpanel.setBorder(BorderFactory.createTitledBorder("Only valid for numbers"));
    JPanel matchpanel = new JPanel();
    matchpanel.setLayout(new GridLayout(3, 1));
    matchpanel.add(cbnot);
    matchpanel.add(cbwhole);
    matchpanel.add(cbcase);
    cbnot.setMnemonic('n');
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

    gbc.gridwidth = 2;
    gbc.insets = new Insets(3, 6, 6, 3);
    gbl.setConstraints(matchpanel, gbc);
    pane.add(matchpanel);

    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.insets.left = 3;
    gbc.insets.right = 6;
    gbl.setConstraints(dirpanel, gbc);
    pane.add(dirpanel);

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
    else if (event.getSource() == rbexact)
      cbwhole.setEnabled(true);
    else if (event.getSource() == rbless || event.getSource() == rbgreater)
      cbwhole.setEnabled(false);
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Runnable ---------------------

  public void run()
  {
    String title = structEntry.getName() + " - " + tfinput.getText();
    if (cbnot.isSelected())
      title = structEntry.getName() + " - not " + tfinput.getText();
    ReferenceHitFrame resultFrame = new ReferenceHitFrame(title, parent);
    String term = tfinput.getText();
    term = term.replaceAll("(\\W)", "\\\\$1");
    if (cbwhole.isSelected())
      term = ".*\\b" + term + "\\b.*";
    else
      term = ".*" + term + ".*";
    Pattern regPattern = Pattern.compile(term, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    if (cbcase.isSelected())
      regPattern = Pattern.compile(term, Pattern.DOTALL);
    int searchNumber = 0;
    if (structEntry instanceof DecNumber) {
      try {
        searchNumber = Integer.parseInt(tfinput.getText());
      } catch (NumberFormatException e) {
        inputFrame.setVisible(true);
        JOptionPane.showMessageDialog(inputFrame, "Not a number", "Error", JOptionPane.ERROR_MESSAGE);
        inputFrame.toFront();
        return;
      }
    }
    inputFrame.setVisible(false);
    ProgressMonitor progress = new ProgressMonitor(parent, "Searching...", null, 0, files.size());
    progress.setMillisToDecideToPopup(100);
    for (int i = 0; i < files.size(); i++) {
      ResourceEntry entry = (ResourceEntry)files.get(i);
      AbstractStruct resource = (AbstractStruct)ResourceFactory.getResource(entry);
      if (resource != null) {
        List flatList = resource.getFlatList();
        for (int j = 0; j < flatList.size(); j++) {
          StructEntry searchEntry = (StructEntry)flatList.get(j);
          if (structEntry instanceof AbstractCode && structEntry.getClass() == searchEntry.getClass() ||
              searchEntry.getName().equalsIgnoreCase(structEntry.getName())) {
            boolean hit = false;
            if (rbexact.isSelected())
              hit = regPattern.matcher(searchEntry.toString()).matches();
            else if (rbless.isSelected())
              hit = searchNumber > ((DecNumber)searchEntry).getValue();
            else if (rbgreater.isSelected())
              hit = searchNumber < ((DecNumber)searchEntry).getValue();
            if (cbnot.isSelected())
              hit = !hit;
            if (hit) {
              AbstractStruct superStruct = resource.getSuperStruct(searchEntry);
              if (superStruct instanceof Resource || superStruct == null)
                resultFrame.addHit(entry, entry.getSearchString(), searchEntry);
              else
                resultFrame.addHit(entry, superStruct.getName(), searchEntry);
            }
          }
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
}

