// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.viewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.infinity.gui.ViewerUtil;
import org.infinity.icon.Icons;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.cre.decoder.MonsterPlanescapeDecoder;
import org.infinity.resource.cre.decoder.SpriteDecoder;
import org.infinity.resource.cre.decoder.internal.ItemInfo;
import org.infinity.resource.cre.viewer.ColorSelectionModel.ColorEntry;
import org.infinity.resource.cre.viewer.CreatureSelectionModel.CreatureItem;
import org.infinity.util.IdsMap;
import org.infinity.util.IdsMapCache;
import org.infinity.util.IdsMapEntry;
import org.infinity.util.Misc;

/**
 * This panel provides controls for customizing various aspects of a CRE resource.
 */
public class CreatureControlPanel extends JPanel
{
  private static final String[] defaultColorLabels = { "Metal color", "Minor color", "Major color", "Skin color",
                                                       "Leather color", "Armor color", "Hair color" };

  // Labels for color selection lists
  private final List<JLabel> colorLabels = new ArrayList<>();
  // Color selection list controls
  private final List<JComboBox<ColorSelectionModel.ColorEntry>> colorControls = new ArrayList<>();
  private final Listeners listeners = new Listeners();

  private final CreatureViewer viewer;

  private CreatureControlModel model;
  private JComboBox<CreatureSelectionModel.CreatureItem> cbCreSelection;
  private JComboBox<CreatureAnimationModel.AnimateEntry> cbCreAnimation;
  private JComboBox<CreatureAllegianceModel.AllegianceEntry> cbCreAllegiance;
  private JComboBox<ItemInfo> cbItemHelmet;
  private JComboBox<ItemInfo> cbItemArmor;
  private JComboBox<ItemInfo> cbItemShield;
  private JComboBox<ItemInfo> cbItemWeapon;
  private JCheckBox cbPanic;
  private JButton bReset;
  private JButton bApply;

  public CreatureControlPanel(CreatureViewer viewer)
  {
    super();
    this.viewer = viewer;
    init();
  }

  /** Returns the associated {@code CreatureViewer} instance. */
  public CreatureViewer getViewer() { return viewer; }

  public CreatureControlModel getControlModel() { return model; }

  /**
   * Sets color labels to the specified values. Empty strings are considered as "unused".
   * Specify empty array to set default labels.
   */
  public void setColorLabels(String[] labels)
  {
    if (labels == null) {
      labels = defaultColorLabels;
    }
    for (int i = 0; i < colorLabels.size(); i++) {
      JLabel l = colorLabels.get(i);
      if (i < labels.length && labels[i] != null && !labels[i].isEmpty()) {
        l.setText(labels[i] + ":");
      } else {
        l.setText("Unused color:");
      }
      l.getParent().invalidate();
    }
    validate();
  }

  /** Resets all CRE-related settings to the values provided by the selected CRE resource. */
  public void resetSettings()
  {
    getControlModel().reset();
  }

  /** Applies the current CRE-related settings to the creature animation. */
  public void applySettings()
  {
    CreResource cre = getControlModel().getDecoder().getCreResource();
    CreUtils.setAnimation(cre, getControlModel().getSelectedAnimation().getValue());
    CreUtils.setAllegiance(cre, getControlModel().getSelectedAllegiance().getValue());
    CreUtils.setStatusPanic(cre, getControlModel().getModelPanic().isSelected());
    CreUtils.setEquipmentHelmet(cre, getControlModel().getSelectedHelmet(getControlModel().getModelHelmet()));
    CreUtils.setEquipmentArmor(cre, getControlModel().getSelectedArmor(getControlModel().getModelArmor()));
    CreUtils.setEquipmentWeapon(cre, getControlModel().getSelectedWeapon(getControlModel().getModelWeapon()));
    CreUtils.setEquipmentShield(cre, getControlModel().getSelectedShield(getControlModel().getModelShield()));
    int idx = 0;
    for (final Iterator<ColorSelectionModel> iter = getControlModel().getColorModelIterator(); iter.hasNext(); ) {
      ColorSelectionModel cm = iter.next();
      if (cm.getSelectedItem() instanceof ColorSelectionModel.ColorEntry) {
        ColorSelectionModel.ColorEntry ce = (ColorSelectionModel.ColorEntry)cm.getSelectedItem();
        CreUtils.setColor(cre, idx, ce.getIndex());
      }
      idx++;
    }
    try {
      getControlModel().resetDecoder(cre);
    } catch (Exception e) {
      e.printStackTrace();
      getViewer().showErrorMessage("Could not load creature animation.\nError: " + e.getMessage());
    }

    getViewer().getSettingsPanel().reset();
    getViewer().getMediaPanel().reset(true);
    getControlModel().resetModified();
  }

  /** Called whenever a setting has been changed by the user. */
  public void fireSettingsChanged()
  {
    bReset.setEnabled(getControlModel().canReset());
    bApply.setEnabled(getControlModel().canApply());
  }

  private void init()
  {
    model = new CreatureControlModel(this);

    GridBagConstraints c = new GridBagConstraints();

    // first column
    JLabel l1 = new JLabel("Select CRE resource:");
    cbCreSelection = new JComboBox<>(model.getModelCreature());
    // this is a good default width for all selection controls in this panel
    cbCreSelection.setPrototypeDisplayValue(CreatureItem.getDefault());
    int defWidth = cbCreSelection.getPreferredSize().width * 5 / 4;
    setPreferredWidth(cbCreSelection, defWidth);
    cbCreSelection.addActionListener(listeners);
//    model.getModelCreature().addListDataListener(listeners);
    updateToolTip(cbCreSelection);

    JLabel l2 = new JLabel("Creature animation:");
    l2.setToolTipText("Supports manually entered numbers. Add \"0x\" prefix or \"h\" suffix to specify a hexadecimal number.");
    cbCreAnimation = new JComboBox<>(model.getModelAnimation());
    setPreferredWidth(cbCreAnimation, defWidth);
    cbCreAnimation.setEditable(true);
    cbCreAnimation.addActionListener(listeners);
    updateToolTip(cbCreAnimation);

    JLabel l3 = new JLabel("Allegiance:");
    cbCreAllegiance = new JComboBox<>(model.getModelAllegiance());
    setPreferredWidth(cbCreAllegiance, defWidth);
    cbCreAllegiance.addActionListener(listeners);
    updateToolTip(cbCreAllegiance);

    cbPanic = new JCheckBox("Panicked");
    cbPanic.setModel(model.getModelPanic());
    cbPanic.addActionListener(listeners);

    JPanel pColumn1 = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pColumn1.add(l1, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0);
    pColumn1.add(cbCreSelection, c);

    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pColumn1.add(l2, c);
    c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 4, 0, 0), 0, 0);
    pColumn1.add(cbCreAnimation, c);

    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pColumn1.add(l3, c);
    c = ViewerUtil.setGBC(c, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 4, 0, 0), 0, 0);
    pColumn1.add(cbCreAllegiance, c);

    c = ViewerUtil.setGBC(c, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pColumn1.add(new JPanel(), c);
    c = ViewerUtil.setGBC(c, 1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 4, 0, 0), 0, 0);
    pColumn1.add(cbPanic, c);


    // second column
    l1 = new JLabel("Helmet:");
    cbItemHelmet = new JComboBox<>(model.getModelHelmet());
    setPreferredWidth(cbItemHelmet, defWidth);
    cbItemHelmet.addActionListener(listeners);
    updateToolTip(cbItemHelmet);

    l2 = new JLabel("Armor:");
    cbItemArmor = new JComboBox<>(model.getModelArmor());
    setPreferredWidth(cbItemArmor, defWidth);
    cbItemArmor.addActionListener(listeners);
    updateToolTip(cbItemArmor);

    l3 = new JLabel("Shield:");
    cbItemShield = new JComboBox<>(model.getModelShield());
    setPreferredWidth(cbItemShield, defWidth);
    cbItemShield.addActionListener(listeners);
    updateToolTip(cbItemShield);

    JLabel l4 = new JLabel("Weapon:");
    cbItemWeapon = new JComboBox<>(model.getModelWeapon());
    setPreferredWidth(cbItemWeapon, defWidth);
    cbItemWeapon.addActionListener(listeners);
    updateToolTip(cbItemWeapon);

    JPanel pColumn2 = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pColumn2.add(l1, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0);
    pColumn2.add(cbItemHelmet, c);

    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pColumn2.add(l2, c);
    c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 4, 0, 0), 0, 0);
    pColumn2.add(cbItemArmor, c);

    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pColumn2.add(l3, c);
    c = ViewerUtil.setGBC(c, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 4, 0, 0), 0, 0);
    pColumn2.add(cbItemShield, c);

    c = ViewerUtil.setGBC(c, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pColumn2.add(l4, c);
    c = ViewerUtil.setGBC(c, 1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 4, 0, 0), 0, 0);
    pColumn2.add(cbItemWeapon, c);


    // third column
    JComboBox<ColorSelectionModel.ColorEntry> cb;
    ColorSelectionModel cm = model.getModelColor(0);

    // determine suitable color entry for preferred combobox width
    ColorSelectionModel.ColorEntry ce = cm.getElementAt(0);
    for (int i = 0; i < cm.getSize(); i++) {
      ColorSelectionModel.ColorEntry item = cm.getElementAt(i);
      if (item != null && (ce == null || item.toString().length() > ce.toString().length())) {
        ce = item;
      }
    }

    l1 = new JLabel("Metal color:");
    colorLabels.add(l1);
    cm = model.getModelColor(0);
    cb = new JComboBox<>(cm);
    cb.setRenderer(cm.getRenderer());
    if (ce != null) {
      cb.setPrototypeDisplayValue(ce);
      defWidth = cb.getPreferredSize().width;
    }
    setPreferredWidth(cb, defWidth);
    cb.addActionListener(listeners);
    cm.addListDataListener(listeners);
    updateToolTip(cb);
    colorControls.add(cb);

    l1 = new JLabel("Minor color:");
    colorLabels.add(l1);
    cm = model.getModelColor(1);
    cb = new JComboBox<>(cm);
    cb.setRenderer(cm.getRenderer());
    cb.setPrototypeDisplayValue((ColorSelectionModel.ColorEntry)cm.getSelectedItem());
    setPreferredWidth(cb, defWidth);
    cb.addActionListener(listeners);
    cm.addListDataListener(listeners);
    updateToolTip(cb);
    colorControls.add(cb);

    l1 = new JLabel("Major color:");
    colorLabels.add(l1);
    cm = model.getModelColor(2);
    cb = new JComboBox<>(cm);
    cb.setRenderer(cm.getRenderer());
    cb.setPrototypeDisplayValue((ColorSelectionModel.ColorEntry)cm.getSelectedItem());
    setPreferredWidth(cb, defWidth);
    cb.addActionListener(listeners);
    cm.addListDataListener(listeners);
    updateToolTip(cb);
    colorControls.add(cb);

    l1 = new JLabel("Skin color:");
    colorLabels.add(l1);
    cm = model.getModelColor(3);
    cb = new JComboBox<>(cm);
    cb.setRenderer(cm.getRenderer());
    cb.setPrototypeDisplayValue((ColorSelectionModel.ColorEntry)cm.getSelectedItem());
    setPreferredWidth(cb, defWidth);
    cb.addActionListener(listeners);
    cm.addListDataListener(listeners);
    updateToolTip(cb);
    colorControls.add(cb);

    JPanel pColumn3 = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pColumn3.add(colorLabels.get(0), c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0);
    pColumn3.add(colorControls.get(0), c);

    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pColumn3.add(colorLabels.get(1), c);
    c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 4, 0, 0), 0, 0);
    pColumn3.add(colorControls.get(1), c);

    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pColumn3.add(colorLabels.get(2), c);
    c = ViewerUtil.setGBC(c, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 4, 0, 0), 0, 0);
    pColumn3.add(colorControls.get(2), c);

    c = ViewerUtil.setGBC(c, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pColumn3.add(colorLabels.get(3), c);
    c = ViewerUtil.setGBC(c, 1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 4, 0, 0), 0, 0);
    pColumn3.add(colorControls.get(3), c);


    // fourth column
    l1 = new JLabel("Leather color:");
    colorLabels.add(l1);
    cm = model.getModelColor(4);
    cb = new JComboBox<>(cm);
    cb.setRenderer(cm.getRenderer());
    cb.setPrototypeDisplayValue((ColorSelectionModel.ColorEntry)cm.getSelectedItem());
    setPreferredWidth(cb, defWidth);
    cb.addActionListener(listeners);
    cm.addListDataListener(listeners);
    updateToolTip(cb);
    colorControls.add(cb);

    l1 = new JLabel("Armor color:");
    colorLabels.add(l1);
    cm = model.getModelColor(5);
    cb = new JComboBox<>(cm);
    cb.setRenderer(cm.getRenderer());
    cb.setPrototypeDisplayValue((ColorSelectionModel.ColorEntry)cm.getSelectedItem());
    setPreferredWidth(cb, defWidth);
    cb.addActionListener(listeners);
    cm.addListDataListener(listeners);
    updateToolTip(cb);
    colorControls.add(cb);

    l1 = new JLabel("Hair color:");
    colorLabels.add(l1);
    cm = model.getModelColor(6);
    cb = new JComboBox<>(cm);
    cb.setRenderer(cm.getRenderer());
    cb.setPrototypeDisplayValue((ColorSelectionModel.ColorEntry)cm.getSelectedItem());
    setPreferredWidth(cb, defWidth);
    cb.addActionListener(listeners);
    cm.addListDataListener(listeners);
    updateToolTip(cb);
    colorControls.add(cb);

    Insets margin;
    bReset = new JButton("Reset", Icons.getIcon(Icons.ICON_UNDO_16));
    margin = bReset.getMargin();
    margin.top += 4; margin.bottom += 4;
    bReset.setMargin(margin);
    bReset.setToolTipText("Revert to creature defaults");
    bReset.addActionListener(listeners);
    bApply = new JButton("Apply", Icons.getIcon(Icons.ICON_REFRESH_16));
    margin = bApply.getMargin();
    margin.top += 4;  margin.bottom += 4;
    bApply.setMargin(margin);
    bApply.addActionListener(listeners);

    JPanel pColumn4 = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pColumn4.add(colorLabels.get(4), c);
    c = ViewerUtil.setGBC(c, 1, 0, 2, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0);
    pColumn4.add(colorControls.get(4), c);

    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pColumn4.add(colorLabels.get(5), c);
    c = ViewerUtil.setGBC(c, 1, 1, 2, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 4, 0, 0), 0, 0);
    pColumn4.add(colorControls.get(5), c);

    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pColumn4.add(colorLabels.get(6), c);
    c = ViewerUtil.setGBC(c, 1, 2, 2, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 4, 0, 0), 0, 0);
    pColumn4.add(colorControls.get(6), c);

    c = ViewerUtil.setGBC(c, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pColumn4.add(new JPanel(), c);
    c = ViewerUtil.setGBC(c, 1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(8, 4, 0, 0), 0, 0);
    pColumn4.add(bReset, c);
    c = ViewerUtil.setGBC(c, 2, 3, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 0), 0, 0);
    pColumn4.add(bApply, c);

    // combining columns
    JPanel panelMain = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(8, 8, 8, 0), 0, 0);
    panelMain.add(pColumn1, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(8, 16, 8, 0), 0, 0);
    panelMain.add(pColumn2, c);
    c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(8, 16, 8, 0), 0, 0);
    panelMain.add(pColumn3, c);
    c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(8, 16, 8, 8), 0, 0);
    panelMain.add(pColumn4, c);

    JScrollPane scroll = new JScrollPane(panelMain);
    scroll.setBorder(new EtchedBorder());

    setLayout(new BorderLayout());
    add(scroll, BorderLayout.CENTER);

    fireSettingsChanged();
  }

  /**
   * Returns {@code true} if the pending reset can be performed. Shows a confirmation dialog if changes were made.
   */
  private boolean confirmReset()
  {
    boolean retVal = true;
    if (getControlModel().canReset()) {
      retVal = (JOptionPane.showConfirmDialog(getViewer(),
                                              "Creature settings have been modified. Do you want to revert these changes?",
                                              "Revert changes",
                                              JOptionPane.YES_NO_OPTION,
                                              JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION);
    }
    return retVal;
  }

  /**
   * Generates an array of color location labels based on the creature animation provided
   * by the specified {@code SpriteDecoder}.
   */
  public static String[] createColorLabels(SpriteDecoder decoder)
  {
    String[] retVal = null;
    if (decoder != null && decoder instanceof MonsterPlanescapeDecoder) {
      MonsterPlanescapeDecoder mpd = (MonsterPlanescapeDecoder)decoder;
      IdsMap map = IdsMapCache.get("CLOWNRGE.IDS");
      if (map != null) {
        retVal = new String[mpd.getColorLocationCount()];
        for (int i = 0; i < retVal.length; i++) {
          int location = mpd.getColorLocation(i) >>> 4;
          if (location > 0) {
            IdsMapEntry entry = map.get(location);
            if (entry != null) {
              retVal[i] = Misc.prettifySymbol(entry.getSymbol()) + " Color";
            }
          }
        }
      }
    }

    if (retVal == null) {
      retVal = Arrays.copyOf(defaultColorLabels, defaultColorLabels.length);
    }

    return retVal;
  }

  /** Helper method: Updates preferred width of the specified {@code JComponent} instance. */
  private static void setPreferredWidth(JComponent c, int width)
  {
    if (c != null) {
      Dimension dim = c.getPreferredSize();
      dim.width = width;
      c.setPreferredSize(dim);
    }
  }

  /** Helper method: Updates the tooltip of the specified {@code JComboBox} object with the label of the selected item. */
  private static void updateToolTip(Object o)
  {
    if (o instanceof JComboBox<?>) {
      JComboBox<?> cb = (JComboBox<?>)o;
      Object s = cb.getSelectedItem();
      if (s != null) {
        cb.setToolTipText(s.toString());
      } else {
        cb.setToolTipText("");
      }
    }
  }

  //-------------------------- INNER CLASSES --------------------------

  /**
   * Listeners are outsourced to this class for cleaner code.
   */
  private class Listeners implements ActionListener, ListDataListener
  {
    private Listeners()
    {
    }

    //--------------------- Begin Interface ActionListener ---------------------

    @Override
    public void actionPerformed(ActionEvent e)
    {
      if (e.getSource() == bApply) {
        applySettings();
      }
      else if (e.getSource() == bReset) {
        if (confirmReset()) {
          resetSettings();
        }
      }
      else if (e.getSource() == cbCreSelection) {
        try {
          getControlModel().creSelectionChanged();
          updateToolTip(cbCreSelection);
        } catch (Exception ex) {
          getViewer().showErrorMessage("Could not load the creature resource.\nError: " + ex.getMessage());
        }
      }
      else if (e.getSource() == cbCreAnimation) {
        if (cbCreAnimation.getSelectedItem() != null) {
          // find matching list entry
          int idx = getControlModel().getModelAnimation().getIndexOf(cbCreAnimation.getSelectedItem());
          if (idx != -1) {
            getControlModel().getModelAnimation().setSelectedItem(getControlModel().getModelAnimation().getElementAt(idx));
          }
        }
        getControlModel().creAnimationChanged();
        updateToolTip(cbCreAnimation);
      }
      else if (e.getSource() == cbCreAllegiance) {
        getControlModel().creAllegianceChanged();
        updateToolTip(cbCreAllegiance);
      }
      else if (e.getSource() == cbItemHelmet) {
        getControlModel().itemHelmetChanged();
        updateToolTip(cbItemHelmet);
      }
      else if (e.getSource() == cbItemArmor) {
        getControlModel().itemArmorChanged();
        updateToolTip(cbItemArmor);
      }
      else if (e.getSource() == cbItemShield) {
        getControlModel().itemShieldChanged();
        updateToolTip(cbItemShield);
      }
      else if (e.getSource() == cbItemWeapon) {
        getControlModel().itemWeaponChanged();
        updateToolTip(cbItemWeapon);
      }
      else if (e.getSource() == cbPanic) {
        getControlModel().crePanicChanged();
      }
      else {
        // color selection
        int idx = colorControls.indexOf(e.getSource());
        if (idx >= 0) {
          getControlModel().colorChanged(idx);
          updateToolTip(colorControls.get(idx));
        }
      }
    }

    //--------------------- End Interface ActionListener ---------------------

    //--------------------- Begin Interface ListDataListener ---------------------

    @Override
    public void intervalAdded(ListDataEvent e)
    {
    }

    @Override
    public void intervalRemoved(ListDataEvent e)
    {
    }

    @Override
    public void contentsChanged(ListDataEvent e)
    {
//      if (e.getSource() == getControlModel().getModelCreature()) {
//      }
//      else if (e.getSource() == getControlModel().getModelAnimation()) {
//      }
//      else if (e.getSource() == getControlModel().getModelAllegiance()) {
//      }
//      else if (e.getSource() == getControlModel().getModelHelmet()) {
//      }
//      else if (e.getSource() == getControlModel().getModelArmor()) {
//      }
//      else if (e.getSource() == getControlModel().getModelShield()) {
//      }
//      else if (e.getSource() == getControlModel().getModelWeapon()) {
//      }
//      else {
        // color selections may be unused in PST/PSTEE
        for (int idx = 0, cnt = colorControls.size(); idx < cnt; idx++) {
          final JComboBox<ColorEntry> cb = colorControls.get(idx);
          if (e.getSource() == colorControls.get(idx).getModel()) {
            boolean enabled = (colorControls.get(idx).getSelectedItem() != null);
            cb.setEnabled(enabled);
            colorLabels.get(idx).setEnabled(enabled);
            break;
          }
        }
//      }
    }

    //--------------------- End Interface ListDataListener ---------------------
  }
}
