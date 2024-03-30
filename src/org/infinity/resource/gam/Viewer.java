// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.gam;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.infinity.datatype.Flag;
import org.infinity.datatype.ResourceRef;
import org.infinity.gui.ViewFrame;
import org.infinity.gui.ViewerUtil;
import org.infinity.gui.ViewerUtil.ListValueRenderer;
import org.infinity.gui.ViewerUtil.StructListPanel;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AbstractVariable;
import org.infinity.resource.StructEntry;
import org.infinity.resource.cre.CreResource;

final class Viewer extends JPanel {
  /** A function that determines the name of (non-)player characters in GAM resources. */
  private static final ViewerUtil.AttributeEntry NPC_ENTRY = (struct) -> {
    if (struct instanceof PartyNPC) {
      final PartyNPC npc = (PartyNPC) struct;

      StructEntry se = npc.getAttribute(PartyNPC.GAM_NPC_NAME);
      if (se != null && !se.toString().isEmpty()) {
        // Display character name from PartyNPC structure
        return se;
      }

      se = npc.getAttribute(PartyNPC.GAM_NPC_CRE_RESOURCE);
      if (se instanceof CreResource) {
        se = ((CreResource) se).getAttribute(CreResource.CRE_NAME);
        if (se != null) {
          // Display character name from embedded CRE resource
          return se;
        }
      } else if (se instanceof ResourceRef) {
        // Display character info from CRE resref
        return se;
      }
    }

    // Fall-back option: Display original structure
    return struct;
  };

  private static JPanel makeMiscPanel(GamResource gam) {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);

    gbc.insets = new Insets(2, 3, 3, 3);
    ViewerUtil.addLabelFieldPair(panel, gam.getAttribute(GamResource.GAM_WORLD_AREA), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, gam.getAttribute(GamResource.GAM_GAME_TIME), gbl, gbc, true);
    StructEntry se = gam.getAttribute(GamResource.GAM_REAL_TIME);
    if (se != null) {
      ViewerUtil.addLabelFieldPair(panel, se, gbl, gbc, true);
    }
    ViewerUtil.addLabelFieldPair(panel, gam.getAttribute(GamResource.GAM_PARTY_GOLD), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, gam.getAttribute(GamResource.GAM_MASTER_AREA), gbl, gbc, true);
    se = gam.getAttribute(GamResource.GAM_WORLDMAP);
    if (se != null) {
      ViewerUtil.addLabelFieldPair(panel, se, gbl, gbc, true);
    }
    se = gam.getAttribute(GamResource.GAM_ZOOM_LEVEL);
    if (se != null) {
      ViewerUtil.addLabelFieldPair(panel, se, gbl, gbc, true);
    }

    gbc.insets.top = 10;
    gbc.gridwidth = 2;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    se = gam.getAttribute(GamResource.GAM_WEATHER);
    if (se != null) {
      JPanel weatherPanel = ViewerUtil.makeCheckPanel((Flag) se, 2);
      panel.add(weatherPanel, gbc);
    }

    se = gam.getAttribute(GamResource.GAM_CONFIGURATION);
    if (se != null) {
      JPanel configPanel = ViewerUtil.makeCheckPanel((Flag) se, 2);
      panel.add(configPanel, gbc);
    }

    return panel;
  }

  Viewer(GamResource gam) {
    final StructListPanel stats1Panel = ViewerUtil.makeListPanel("Non-player characters", gam, NonPartyNPC.class, NPC_ENTRY);
    new NpcContextMenu(stats1Panel);
    final StructListPanel stats2Panel = ViewerUtil.makeListPanel("Player characters", gam, PartyNPC.class, NPC_ENTRY);
    new NpcContextMenu(stats2Panel);

    StructListPanel var1Panel = ViewerUtil.makeListPanel("Variables", gam, Variable.class, AbstractVariable.VAR_NAME,
        new VariableListRenderer());

    setLayout(new GridLayout(2, 3, 3, 3));
    add(makeMiscPanel(gam));
    add(stats2Panel);
    add(var1Panel);
    add(stats1Panel);
    setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
  }

  // -------------------------- INNER CLASSES --------------------------

  private static final class VariableListRenderer extends DefaultListCellRenderer implements ListValueRenderer {
    private VariableListRenderer() {
      super();
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
        boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      setText(getListValue(value));
      return this;
    }

    @Override
    public String getListValue(Object value) {
      if (value instanceof AbstractStruct) {
        AbstractStruct effect = (AbstractStruct) value;
        StructEntry entry1 = effect.getAttribute(effect.getOffset(), false);
        StructEntry entry2 = effect.getAttribute(effect.getOffset() + 40, false);
        return entry1 + " = " + entry2;
      } else if (value != null) {
        return value.toString();
      }
      return "";
    }
  }

  /**
   * Handles a context menu associated with a {@code PartyNPC} list panel.
   */
  private static class NpcContextMenu extends MouseAdapter implements ActionListener {
    private final JPopupMenu popup = new JPopupMenu();
    private final JMenuItem miOpenChr = new JMenuItem("View/Edit CHR", Icons.ICON_ZOOM_16.getIcon());
    private final JMenuItem miOpenCre = new JMenuItem("View/Edit CRE", Icons.ICON_ZOOM_16.getIcon());

    private final StructListPanel panel;
    private final JList<StructEntry> npcList;

    public NpcContextMenu(StructListPanel npcPanel) {
      this.panel = Objects.requireNonNull(npcPanel);
      this.npcList = Objects.requireNonNull(this.panel.getList());
      init();
    }

//    /** Returns the associated {@code StructListPanel} instance. */
//    public StructListPanel getPanel() {
//      return panel;
//    }

//    /** Returns the {@code JPopupMenu} associated with the list panel. */
//    public JPopupMenu getPopupMenu() {
//      return popup;
//    }

    @Override
    public void mousePressed(MouseEvent e) {
      showPopup(e, true);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      showPopup(e, true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (miOpenChr.equals(e.getSource())) {
        final PartyNPC npc = getSelectedNpc(null, false);
        if (npc != null) {
          new ViewFrame(panel.getTopLevelAncestor(), npc);
        }
      } else if (miOpenCre.equals(e.getSource())) {
        final PartyNPC npc = getSelectedNpc(null, false);
        if (npc != null) {
          final StructEntry se = npc.getAttribute(PartyNPC.GAM_NPC_CRE_RESOURCE);
          if (se instanceof CreResource) {
            new ViewFrame(panel.getTopLevelAncestor(), (CreResource) se);
          }
        }
      }
    }

    /**
     * Triggers the context menu addressed by the specified {@code MouseEvent}.
     *
     * @param e {@code MouseEvent} to handle.
     * @param autoSelect Indicates whether the list item at the current mouse coordinate should be selected.
     */
    private void showPopup(MouseEvent e, boolean autoSelect) {
      if (e.isPopupTrigger()) {
        final PartyNPC npc = getSelectedNpc(new Point(e.getX(), e.getY()), autoSelect);
        if (npc != null) {
          miOpenCre.setEnabled(hasCreData(npc));
          popup.show(e.getComponent(), e.getX(), e.getY());
        }
      }
    }

    /**
     * Returns whether the specified {@code PartyNPC} structure contains a valid CRE resource substructure.
     */
    private boolean hasCreData(PartyNPC npc) {
      return npc != null && npc.getAttribute(PartyNPC.GAM_NPC_CRE_RESOURCE) instanceof CreResource;
    }

    /**
     * Returns the NPC list element at the specified coordinate.
     *
     * @param p A location relative to the {@code npcList} component. Can be {@code null}.
     * @param autoSelect Indicates whether the list item at the specified location should be selected.
     * @return Returns the {@code PartyNPC} list item closest to the specified location. Returns the selected list item
     *         if location is {@code null}.
     */
    private PartyNPC getSelectedNpc(Point p, boolean autoSelect) {
      if (p != null && autoSelect) {
        int index = npcList.locationToIndex(p);
        if (index >= 0) {
          npcList.setSelectedIndex(index);
        }
      }

      int index = (p != null && !autoSelect) ? npcList.locationToIndex(p) : npcList.getSelectedIndex();
      if (index >= 0) {
        final Object listItem = npcList.getModel().getElementAt(index);
        if (listItem instanceof PartyNPC) {
          return (PartyNPC) listItem;
        }
      }
      return null;
    }

    private void init() {
      miOpenChr.addActionListener(this);
      miOpenCre.addActionListener(this);

      popup.add(miOpenChr);
      popup.add(miOpenCre);

      npcList.addMouseListener(this);
    }
  }
}
