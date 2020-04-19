// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search.advanced;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.infinity.gui.ViewerUtil;

/**
 *
 */
public class FlagsPanel extends JPanel implements ActionListener
{
  private final int size;
  private final JCheckBox[] cbFlags;

  private JButton bAll, bNone, bInvert;

  public FlagsPanel(int size, String[] labels)
  {
    super(new BorderLayout());
    if (size < 1)
      size = 1;
    if (size >= 3)
      size = 4;
    this.size = size;
    cbFlags = new JCheckBox[this.size << 3];
    init(labels);
  }

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bAll || event.getSource() == bNone) {
      boolean op = (event.getSource() == bAll);
      for (JCheckBox cb : cbFlags)
        cb.setSelected(op);
    } else if (event.getSource() == bInvert) {
      for (JCheckBox cb: cbFlags)
        cb.setSelected(!cb.isSelected());
    }
  }

  /** Returns the numeric representation of selected flags. */
  public int getValue() {
    int value = 0;
    for (int i = 0; i < cbFlags.length; i++)
      if (cbFlags[i].isSelected())
        value |= 1 << i;
    return value;
  }

  /** Sets the bits according to the specified value. */
  public void setValue(int value)
  {
    for (int i = 0; i < cbFlags.length; i++) {
      cbFlags[i].setSelected((value & 1) != 0);
      value >>>= 1;
    }
  }

  private void init(String[] labels)
  {
    if (labels == null || labels.length == 0)
      labels = new String[] {"Normal"};

    GridBagConstraints c = new GridBagConstraints();

    // flags
    int bits = cbFlags.length;
    int cols = 4;
    int rows = bits / cols;
    JPanel pBits = new JPanel(new GridBagLayout());
    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols; col++) {
        int idx = col * rows + row;
        String label = (idx < labels.length) ? labels[idx] : "Unknown";
        cbFlags[idx] = new JCheckBox(label, false);

        c = ViewerUtil.setGBC(c, col, row, 1, 1, 1, 0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
        pBits.add(cbFlags[idx], c);
      }
    }

    // buttons
    bAll = new JButton("Select all");
    bAll.setMargin(new Insets(1, bAll.getMargin().left, 1, bAll.getMargin().right));
    bAll.addActionListener(this);
    bNone = new JButton("Select none");
    bNone.setMargin(new Insets(1, bNone.getMargin().left, 1, bNone.getMargin().right));
    bNone.addActionListener(this);
    bInvert = new JButton("Invert selection");
    bInvert.setMargin(new Insets(1, bInvert.getMargin().left, 1, bInvert.getMargin().right));
    bInvert.addActionListener(this);
    JPanel pButtons = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0, 0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pButtons.add(bAll, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0, 0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 0, 0);
    pButtons.add(bNone, c);
    c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0, 0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 0, 0);
    pButtons.add(bInvert, c);
    c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 1, 0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 0, 0);
    pButtons.add(new JPanel(), c);

    JPanel pMain = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0, 0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(8, 8, 0, 8), 0, 0);
    pMain.add(pBits, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0, 0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL, new Insets(8, 8, 8, 8), 0, 0);
    pMain.add(pButtons, c);

    add(pMain, BorderLayout.CENTER);
  }
}
