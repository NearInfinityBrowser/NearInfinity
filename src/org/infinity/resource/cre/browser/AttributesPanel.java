// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.browser;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EtchedBorder;

import org.infinity.gui.ViewFrame;
import org.infinity.gui.ViewerUtil;
import org.infinity.icon.Icons;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.cre.decoder.SpriteDecoder;
import org.infinity.resource.key.ResourceEntry;

/**
 * This panel provides a table view of creature animation attributes.
 */
public class AttributesPanel extends JPanel implements ActionListener
{
  private final CreatureBrowser browser;

  private JTable tableAnimation;
  private DecoderAttributesTableModel tableModel;
  private JButton bIni;

  public AttributesPanel(CreatureBrowser viewer)
  {
    super();
    this.browser = Objects.requireNonNull(viewer);
    init();
  }

  /** Returns the associated {@code CreatureViewer} instance. */
  public CreatureBrowser getBrowser() { return browser; }

  /** Discards and reloads the current settings and attributes list. */
  public void reset()
  {
    SpriteDecoder decoder = getBrowser().getDecoder();
    tableModel.setDecoder(decoder);
    bIni.setEnabled(getIniResource() != null);
  }

  /**
   * Returns the INI file associated with the currently selected creature animation if available,
   * returns {@code null} otherwise.
   */
  private ResourceEntry getIniResource()
  {
    ResourceEntry retVal = null;
    if (getBrowser().getDecoder() != null) {
      String iniResref = String.format("%04X", getBrowser().getDecoder().getAnimationId());
      if (iniResref.length() == 4) {
        retVal = ResourceFactory.getResourceEntry(iniResref + ".INI");
      }
    }
    return retVal;
  }

  private void init()
  {
    JLabel label = new JLabel("Animation attributes:");

    bIni = new JButton("View/Edit INI", Icons.getIcon(Icons.ICON_ZOOM_16));
    bIni.addActionListener(this);

    GridBagConstraints c = new GridBagConstraints();
    JPanel panel = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    panel.add(label, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 0, 0);
    panel.add(bIni);

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

    setLayout(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    add(panel, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
        GridBagConstraints.BOTH, new Insets(4, 0, 0, 0), 0, 0);
    add(scrollTable, c);
  }

  //--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == bIni) {
      ResourceEntry entry = getIniResource();
      if (entry != null) {
        new ViewFrame(getBrowser(), ResourceFactory.getResource(entry));
      } else {
        JOptionPane.showMessageDialog(getBrowser(), "Unable to open INI resource.", "View/Edit INI",
                                      JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  //--------------------- End Interface ActionListener ---------------------
}
