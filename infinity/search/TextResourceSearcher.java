// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.search;

import infinity.gui.Center;
import infinity.gui.ChildFrame;
import infinity.icon.Icons;
import infinity.resource.ResourceFactory;
import infinity.resource.TextResource;
import infinity.resource.key.ResourceEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public final class TextResourceSearcher implements Runnable, ActionListener
{
  private final ChildFrame inputFrame;
  private final Component parent;
  private final JButton bsearch = new JButton("Search", Icons.getIcon("FindAgain16.gif"));
  private final JCheckBox cbwhole = new JCheckBox("Match whole word only");
  private final JCheckBox cbcase = new JCheckBox("Match case");
  private final JTextField tfinput = new JTextField("", 15);
  private final List<ResourceEntry> files;

  public TextResourceSearcher(List<ResourceEntry> files, Container parent)
  {
    this.files = files;
    this.parent = parent;

    String title = "";
    if (files.size() == 1)
      title = (files.get(0)).toString();
    else if (files.size() > 1) {
      ResourceEntry entry = files.get(0);
      title = "all " + entry.getExtension() + " files";
    }
    inputFrame = new ChildFrame("Find in " + title, true);
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
    matchpanel.setLayout(new GridLayout(0, 1));
    matchpanel.add(cbwhole);
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
    gbc.insets = new Insets(3, 6, 6, 3);
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
    TextHitFrame resultFrame = new TextHitFrame(term, parent);
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
      TextResource resource = (TextResource)ResourceFactory.getResource(entry);
      if (resource != null) {
        int linenr = 0;
        StringTokenizer tokenizer = new StringTokenizer(resource.getText(), "\n", true);
        while (tokenizer.hasMoreTokens()) {
          linenr++;
          String token = tokenizer.nextToken();
          if (token.equals("\n") && tokenizer.hasMoreTokens())
            token = tokenizer.nextToken();
          if (regPattern.matcher(token).matches())
            resultFrame.addHit(entry, token, linenr);
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

