// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.search;

import infinity.gui.*;
import infinity.icon.Icons;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.regex.Pattern;

public final class SearchMaster extends JPanel implements Runnable, ActionListener
{
  private final JButton bnext = new JButton("Find Next", Icons.getIcon("FindAgain16.gif"));
  private final JButton bclear = new JButton("New Search", Icons.getIcon("New16.gif"));
  private final JCheckBox cbwhole = new JCheckBox("Match whole word only");
  private final JCheckBox cbcase = new JCheckBox("Match case");
  private final JFrame container;
  private final JRadioButton rbup = new JRadioButton("Up");
  private final JRadioButton rbdown = new JRadioButton("Down");
  private final JTextField tfinput = new JTextField(15);
  private final SearchClient slave;
  private final WindowBlocker blocker;
  private Thread thread;
  private int index;

  public static void createAsFrame(SearchClient slave, String title, Component parent)
  {
    ChildFrame frame = new ChildFrame("Find: " + title, true);
    frame.setIconImage(Icons.getIcon("Find16.gif").getImage());
    JPanel pane = (JPanel)frame.getContentPane();
    pane.setLayout(new BorderLayout());
    pane.add(new SearchMaster(slave, frame), BorderLayout.CENTER);
    frame.pack();
    Center.center(frame, parent.getBounds());
    frame.setVisible(true);
  }

  public static JPanel createAsPanel(SearchClient slave, JFrame container)
  {
    return new SearchMaster(slave, container);
  }

  private static void syncNotify(Object o)
  {
    synchronized (o) {
      o.notifyAll();
    }
  }

  private SearchMaster(SearchClient slave, JFrame container)
  {
    this.slave = slave;
    this.container = container;
    blocker = new WindowBlocker(container);
    container.getRootPane().setDefaultButton(bnext);
    bclear.setMnemonic('n');
    bnext.addActionListener(this);
    tfinput.addActionListener(this);
    bclear.addActionListener(this);
    ButtonGroup gb = new ButtonGroup();
    gb.add(rbup);
    gb.add(rbdown);
    rbup.setMnemonic('u');
    rbdown.setMnemonic('d');
    rbdown.setSelected(true);

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    setLayout(gbl);
    JLabel label = new JLabel("Find what:");
    label.setLabelFor(tfinput);
    label.setDisplayedMnemonic('f');
    tfinput.setMinimumSize(new Dimension(tfinput.getMinimumSize().width, bnext.getMinimumSize().height));
    tfinput.setPreferredSize(
            new Dimension(tfinput.getPreferredSize().width, bnext.getPreferredSize().height));
    JPanel dirpanel = new JPanel();
    dirpanel.add(new JPanel());
    dirpanel.add(rbup);
    dirpanel.add(rbdown);
    dirpanel.setBorder(BorderFactory.createTitledBorder("Direction"));
    JPanel matchpanel = new JPanel();
    matchpanel.setLayout(new GridLayout(2, 1));
    matchpanel.add(cbwhole);
    matchpanel.add(cbcase);
    cbwhole.setMnemonic('w');
    cbcase.setMnemonic('m');
    JPanel bpanel = new JPanel();
    bpanel.setLayout(new GridLayout(2, 1, 6, 6));
    bpanel.add(bclear);

    gbc.insets = new Insets(6, 6, 3, 3);
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbl.setConstraints(label, gbc);
    add(label);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.gridwidth = 2;
    gbc.insets.left = 3;
    gbl.setConstraints(tfinput, gbc);
    add(tfinput);

    gbc.weightx = 0.0;
    gbc.insets.right = 6;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(bnext, gbc);
    add(bnext);

    gbc.gridwidth = 2;
    gbc.insets = new Insets(3, 6, 6, 3);
    gbl.setConstraints(matchpanel, gbc);
    add(matchpanel);

    gbc.gridwidth = 1;
    gbc.insets.left = 3;
    gbl.setConstraints(dirpanel, gbc);
    add(dirpanel);

    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.anchor = GridBagConstraints.NORTH;
    gbc.insets.right = 6;
    gbl.setConstraints(bpanel, gbc);
    add(bpanel);
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bnext || event.getSource() == tfinput) {
      if (thread == null || !thread.isAlive()) {
        thread = new Thread(this);
        thread.start();
      }
      else
        syncNotify(slave);
    }
    else if (event.getSource() == bclear) {
      rbdown.setSelected(true);
      tfinput.setText("");
      syncNotify(slave);
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Runnable ---------------------

  public void run()
  {
    index = 0;
    String term = tfinput.getText();
    term = term.replaceAll("(\\W)", "\\\\$1");
    if (cbwhole.isSelected())
      term = ".*\\b" + term + "\\b.*";
    else
      term = ".*" + term + ".*";
    Pattern regPattern = Pattern.compile(term, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    if (cbcase.isSelected())
      regPattern = Pattern.compile(term, Pattern.DOTALL);
    blocker.setBlocked(true);
    bnext.setEnabled(false);
    cbwhole.setEnabled(false);
    cbcase.setEnabled(false);
    tfinput.setEnabled(false);
    while (true) {
      String s = slave.getText(index);
      if (s == null)
        break;
      if (regPattern.matcher(s).matches()) {
        slave.hitFound(index);
        blocker.setBlocked(false);
        container.requestFocus();
        bnext.setEnabled(true);
        synchronized (slave) {
          try {
            slave.wait();
          } catch (InterruptedException e) {
          }
        }
        blocker.setBlocked(true);
        bnext.setEnabled(false);
        if (tfinput.getText().equals("")) {
          term = null;
          break;
        }
      }
      if (rbdown.isSelected())
        index++;
      else
        index--;
    }
    blocker.setBlocked(false);
    bnext.setEnabled(true);
    cbwhole.setEnabled(true);
    cbcase.setEnabled(true);
    tfinput.setEnabled(true);
    if (term != null)
      JOptionPane.showMessageDialog(this, "No more matches found", "Search complete",
                                    JOptionPane.INFORMATION_MESSAGE);
  }

// --------------------- End Interface Runnable ---------------------
}

