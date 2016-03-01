// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;

import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
import org.infinity.icon.Icons;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.TextResource;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Debugging;
import org.infinity.util.Misc;

public final class TextResourceSearcher implements Runnable, ActionListener
{
  private static final String FMT_PROGRESS = "Processing resource %d/%d";

  private final ChildFrame inputFrame;
  private final Component parent;
  private final JButton bsearch = new JButton("Search", Icons.getIcon(Icons.ICON_FIND_AGAIN_16));
  private final JCheckBox cbwhole = new JCheckBox("Match whole word only");
  private final JCheckBox cbcase = new JCheckBox("Match case");
  private final JCheckBox cbregex = new JCheckBox("Use regular expressions");
  private final JTextField tfinput = new JTextField("", 15);
  private final List<ResourceEntry> files;

  private ProgressMonitor progress;
  private int progressIndex;
  private Pattern regPattern;
  private TextHitFrame resultFrame;

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
      term = term.replaceAll("(\\W)", "\\\\$1");
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
      boolean isCancelled = false;
      inputFrame.setVisible(false);
      resultFrame = new TextHitFrame(term, parent);
      ThreadPoolExecutor executor = Misc.createThreadPool();
      progressIndex = 0;
      progress = new ProgressMonitor(parent, "Searching...",
                                     String.format(FMT_PROGRESS, files.size(), files.size()),
                                     0, files.size());
      progress.setNote(String.format(FMT_PROGRESS, progressIndex, files.size()));
      progress.setMillisToDecideToPopup(100);
      Debugging.timerReset();
      for (int i = 0; i < files.size(); i++) {
        Misc.isQueueReady(executor, true, -1);
        executor.execute(new Worker(files.get(i)));
        if (progress.isCanceled()) {
          isCancelled = true;
          break;
        }
      }

      // enforcing thread termination if process has been cancelled
      if (isCancelled) {
        executor.shutdownNow();
      } else {
        executor.shutdown();
      }

      // waiting for pending threads to terminate
      while (!executor.isTerminated()) {
        if (!isCancelled && progress.isCanceled()) {
          executor.shutdownNow();
          isCancelled = true;
        }
        try { Thread.sleep(1); } catch (InterruptedException e) {}
      }

      if (isCancelled) {
        resultFrame.close();
        JOptionPane.showMessageDialog(parent, "Search cancelled", "Info", JOptionPane.INFORMATION_MESSAGE);
      } else {
        resultFrame.setVisible(true);
      }
    } finally {
      advanceProgress(true);
      regPattern = null;
      resultFrame = null;
    }
    Debugging.timerShow("Search completed", Debugging.TimeFormat.MILLISECONDS);
  }

// --------------------- End Interface Runnable ---------------------

  private synchronized void advanceProgress(boolean finished)
  {
    if (progress != null) {
      if (finished) {
        progressIndex = 0;
        progress.close();
        progress = null;
      } else {
        progressIndex++;
        if (progressIndex % 100 == 0) {
          progress.setNote(String.format(FMT_PROGRESS, progressIndex, files.size()));
        }
        progress.setProgress(progressIndex);
      }
    }
  }

  private synchronized void addHit(ResourceEntry entry, String line, int lineNr)
  {
    if (resultFrame != null) {
      resultFrame.addHit(entry, line, lineNr);
    }
  }

  private Pattern getPattern()
  {
    return regPattern;
  }

//-------------------------- INNER CLASSES --------------------------

  private class Worker implements Runnable
  {
    private final ResourceEntry entry;

    public Worker(ResourceEntry entry)
    {
      this.entry = entry;
    }

    @Override
    public void run()
    {
      if (entry != null) {
        TextResource resource = (TextResource)ResourceFactory.getResource(entry);
        if (resource != null) {
          BufferedReader br = new BufferedReader(new StringReader(resource.getText()));
          try {
            String line;
            int linenr = 0;
            while ((line = br.readLine()) != null) {
              linenr++;
              if (getPattern().matcher(line).matches()) {
                addHit(entry, line, linenr);
              }
            }
          } catch (IOException e) {
            e.printStackTrace();
          } finally {
            try {
              br.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
        advanceProgress(false);
      }
    }
  }
}

