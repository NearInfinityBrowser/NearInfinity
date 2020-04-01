// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
import org.infinity.icon.Icons;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.TextResource;
import org.infinity.resource.key.ResourceEntry;

public final class TextResourceSearcher extends AbstractSearcher implements Runnable, ActionListener
{
  private final ChildFrame inputFrame;
  private final JButton bsearch = new JButton("Search", Icons.getIcon(Icons.ICON_FIND_AGAIN_16));
  private final JCheckBox cbwhole = new JCheckBox("Match whole word only");
  private final JCheckBox cbcase = new JCheckBox("Match case");
  private final JCheckBox cbregex = new JCheckBox("Use regular expressions");
  private final JTextField tfinput = new JTextField("", 15);
  private final List<ResourceEntry> files;

  private Pattern regPattern;
  private TextHitFrame resultFrame;

  public TextResourceSearcher(List<ResourceEntry> files, Container parent)
  {
    super(SEARCH_ONE_TYPE_FORMAT, parent);
    this.files = files;

    String title = "";
    if (files.size() == 1)
      title = (files.get(0)).toString();
    else if (files.size() > 1) {
      ResourceEntry entry = files.get(0);
      title = "all " + entry.getExtension() + " files";
    }
    inputFrame = new ChildFrame("Find in " + title, true);
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
    matchpanel.setLayout(new GridLayout(0, 2));
    matchpanel.add(cbwhole);
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
    gbc.insets = new Insets(3, 6, 6, 3);
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
      resultFrame = new TextHitFrame(term, parent);

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
      if (resource instanceof TextResource) {
        try (final BufferedReader br = new BufferedReader(new StringReader(((TextResource)resource).getText()))) {
          String line;
          int linenr = 0;
          while ((line = br.readLine()) != null) {
            linenr++;
            if (regPattern.matcher(line).matches()) {
              addHit(entry, line, linenr);
            }
          }
        } catch (IOException e) {
          synchronized (System.err) {
            e.printStackTrace();
          }
        }
      }
      advanceProgress();
    };
  }

  private synchronized void addHit(ResourceEntry entry, String line, int lineNr)
  {
    if (resultFrame != null) {
      resultFrame.addHit(entry, line, lineNr);
    }
  }
}
