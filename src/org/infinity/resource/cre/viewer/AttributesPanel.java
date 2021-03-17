// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.viewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Objects;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EtchedBorder;

import org.infinity.resource.cre.decoder.SpriteDecoder;

/**
 * This panel provides a table view of creature animation attributes.
 */
public class AttributesPanel extends JPanel
{
  private final CreatureViewer viewer;

  private JTable tableAnimation;
  private DecoderAttributesTableModel tableModel;

  public AttributesPanel(CreatureViewer viewer)
  {
    super();
    this.viewer = Objects.requireNonNull(viewer);
    init();
  }

  /** Returns the associated {@code CreatureViewer} instance. */
  public CreatureViewer getViewer() { return viewer; }

  /** Discards and reloads the current settings and attributes list. */
  public void reset()
  {
    SpriteDecoder decoder = getViewer().getDecoder();
    tableModel.setDecoder(decoder);
  }

  private void init()
  {
    JLabel label = new JLabel("Animation attributes:");
    tableModel = new DecoderAttributesTableModel();
    tableAnimation = new JTable(tableModel,
                                new DecoderAttributesTableModel.AttributesColumnModel(),
                                new DecoderAttributesTableModel.AttributesListSelectionModel());
    Dimension dim = new Dimension(0, 50);
    for (int i = 0; i < tableAnimation.getColumnModel().getColumnCount(); i++) {
      dim.width += tableAnimation.getColumnModel().getColumn(i).getPreferredWidth();
    }
    tableAnimation.setPreferredScrollableViewportSize(dim);
    tableAnimation.getTableHeader().setFont(tableAnimation.getTableHeader().getFont().deriveFont(Font.BOLD));
    tableAnimation.getTableHeader().setDefaultRenderer(new DecoderAttributesTableModel.AttributesHeaderRenderer(tableAnimation));
    tableAnimation.getTableHeader().setReorderingAllowed(false);
    tableAnimation.setRowHeight(tableAnimation.getRowHeight() * 3 / 2);
    tableAnimation.setIntercellSpacing(new Dimension(4, 0));
    JScrollPane scrollTable = new JScrollPane(tableAnimation);
    scrollTable.setBorder(new EtchedBorder());

    setLayout(new BorderLayout());
    add(label, BorderLayout.PAGE_START);
    add(scrollTable, BorderLayout.CENTER);
  }
}
