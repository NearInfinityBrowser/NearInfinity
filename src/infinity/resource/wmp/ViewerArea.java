// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wmp;

import infinity.datatype.*;
import infinity.gui.ViewFrame;
import infinity.gui.ViewerUtil;
import infinity.icon.Icons;
import infinity.resource.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

final class ViewerArea extends JPanel implements ActionListener
{
  private final DefaultListModel listModel = new DefaultListModel();
  private final JButton bOpen = new JButton("View/Edit", Icons.getIcon("Zoom16.gif"));
  private JList list;

  private static JPanel makeInfoPanel(AreaEntry areaEntry)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);
    gbc.insets = new Insets(3, 3, 3, 3);
    ViewerUtil.addLabelFieldPair(panel, areaEntry.getAttribute("Name"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, areaEntry.getAttribute("Current area"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, areaEntry.getAttribute("Loading image"), gbl, gbc, true);
    return panel;
  }

  ViewerArea(AreaEntry areaEntry)
  {
    JPanel flagPanel = ViewerUtil.makeCheckPanel((Flag)areaEntry.getAttribute("Flags"), 1);
    JPanel infoPane = makeInfoPanel(areaEntry);
    JComponent icon = ViewerUtil.makeBamPanel(
            (ResourceRef)areaEntry.getSuperStruct().getAttribute("Map icons"),
            ((DecNumber)areaEntry.getAttribute("Icon number")).getValue(),
            0);
    JPanel linkPanelN = ViewerUtil.makeListPanel("North links", areaEntry, AreaLinkNorth.class, "Target entrance");
    JPanel linkPanelS = ViewerUtil.makeListPanel("South links", areaEntry, AreaLinkSouth.class, "Target entrance");
    JPanel linkPanelW = ViewerUtil.makeListPanel("West links", areaEntry, AreaLinkWest.class, "Target entrance");
    JPanel linkPanelE = ViewerUtil.makeListPanel("East links", areaEntry, AreaLinkEast.class, "Target entrance");

    JPanel linkPanel = new JPanel(new GridLayout(2,2,6,6));
    linkPanel.add(linkPanelN);
    linkPanel.add(linkPanelE);
    linkPanel.add(linkPanelS);
    linkPanel.add(linkPanelW);

    JPanel leftPanel = new JPanel(new BorderLayout(0, 3));
    leftPanel.add(icon, BorderLayout.NORTH);
    leftPanel.add(infoPane, BorderLayout.CENTER);

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    setLayout(gbl);

    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.insets.left = 3;
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.CENTER;
    gbl.setConstraints(leftPanel, gbc);
    add(leftPanel);

    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(flagPanel, gbc);
    add(flagPanel);

    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbl.setConstraints(linkPanel, gbc);
    add(linkPanel);
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bOpen)
      new ViewFrame(getTopLevelAncestor(), (Viewable)list.getSelectedValue());
  }

// --------------------- End Interface ActionListener ---------------------

  private JPanel makeLinkPanel(AreaEntry areaEntry)
  {
    DecNumber firstNorth = (DecNumber)areaEntry.getAttribute("First link (north)");
    DecNumber countNorth = (DecNumber)areaEntry.getAttribute("# links (north)");
    DecNumber firstWest = (DecNumber)areaEntry.getAttribute("First link (west)");
    DecNumber countWest = (DecNumber)areaEntry.getAttribute("# links (west)");
    DecNumber firstSouth = (DecNumber)areaEntry.getAttribute("First link (south)");
    DecNumber countSouth = (DecNumber)areaEntry.getAttribute("# links (south)");
    DecNumber firstEast = (DecNumber)areaEntry.getAttribute("First link (east)");
    DecNumber countEast = (DecNumber)areaEntry.getAttribute("# links (east)");

//    for (int i = firstNorth.getValue(); i < firstNorth.getValue() + countNorth.getValue(); i++)
//      listModel.addElement(areaEntry.getSuperStruct().getAttribute("Area link " + i));
//    for (int i = firstWest.getValue(); i < firstWest.getValue() + countWest.getValue(); i++)
//      listModel.addElement(areaEntry.getSuperStruct().getAttribute("Area link " + i));
//    for (int i = firstSouth.getValue(); i < firstSouth.getValue() + countSouth.getValue(); i++)
//      listModel.addElement(areaEntry.getSuperStruct().getAttribute("Area link " + i));
//    for (int i = firstEast.getValue(); i < firstEast.getValue() + countEast.getValue(); i++)
//      listModel.addElement(areaEntry.getSuperStruct().getAttribute("Area link " + i));

    list = new JList(listModel);
    list.setCellRenderer(new AreaLinkEntryRenderer());
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    final JComponent parent = this;
    list.addMouseListener(new MouseAdapter()
    {
      public void mouseClicked(MouseEvent e)
      {
        if (e.getClickCount() == 2 && list.getSelectedValue() instanceof Viewable) {
          new ViewFrame(parent.getTopLevelAncestor(), (Viewable)list.getSelectedValue());
        }
      }
    });
    if (listModel.size() > 0)
      list.setSelectedIndex(0);
    bOpen.addActionListener(this);
    bOpen.setEnabled(listModel.size() > 0 && listModel.get(0) instanceof Viewable);

    JPanel panel = new JPanel(new BorderLayout(0, 3));
    panel.add(new JLabel("Area links"), BorderLayout.NORTH);
    panel.add(new JScrollPane(list), BorderLayout.CENTER);
    panel.add(bOpen, BorderLayout.SOUTH);
    return panel;
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class AreaLinkEntryRenderer extends DefaultListCellRenderer
  {
    private AreaLinkEntryRenderer()
    {
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                  boolean cellHasFocus)
    {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      AbstractStruct areaLink = (AbstractStruct)value;
      StructEntry entry = areaLink.getAttribute("Target entrance");
      DecNumber targetIndex = (DecNumber)areaLink.getAttribute("Target area");
      AreaEntry targetEntry = (AreaEntry)areaLink.getSuperStruct().getAttribute("Area " + targetIndex);

      StringBuffer name = new StringBuffer();
      if (targetEntry != null)
        name.append(targetEntry.getAttribute("Name"));
      if (entry != null && entry.toString().length() > 0)
        name.append(" (").append(entry.toString()).append(')');
      if (name.length() == 0)
        setText(areaLink.toString());
      else
        setText(name.toString());
      return this;
    }
  }
}

