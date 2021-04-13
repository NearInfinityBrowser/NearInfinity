// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.browser;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.JTextComponent;

import org.infinity.gui.ViewerUtil;
import org.infinity.icon.Icons;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.cre.browser.ColorSelectionModel.ColorEntry;
import org.infinity.resource.cre.browser.CreatureSelectionModel.CreatureItem;
import org.infinity.resource.cre.decoder.MonsterPlanescapeDecoder;
import org.infinity.resource.cre.decoder.SpriteDecoder;
import org.infinity.resource.cre.decoder.util.ItemInfo;
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

  private final CreatureBrowser browser;

  private CreatureControlModel model;
  private JComboBox<CreatureSelectionModel.CreatureItem> cbCreSelection;
  private JComboBox<CreatureAnimationModel.AnimateEntry> cbCreAnimation;
  private JComboBox<CreatureStatusModel.StatusEntry> cbCreAllegiance;
  private JComboBox<ItemInfo> cbItemHelmet;
  private JComboBox<ItemInfo> cbItemArmor;
  private JComboBox<ItemInfo> cbItemShield;
  private JComboBox<ItemInfo> cbItemWeapon;
  private JButton bReset, bApply, bHidePanel, bShowPanel;
  private JPanel panelMain, panelHidden;
  private JScrollPane scrollShown;
  private CardLayout layoutMain;

  public CreatureControlPanel(CreatureBrowser browser)
  {
    super();
    this.browser = browser;
    init();
  }

  /** Returns the associated {@code CreatureBrowser} instance. */
  public CreatureBrowser getBrowser() { return browser; }

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
    CreUtils.setStatusPanic(cre, getControlModel().getSelectedAllegiance().getStatus() == CreatureStatusModel.Status.PANICKED);
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
      getBrowser().showErrorMessage(e.getMessage(), "Loading creature");
    }

    getBrowser().getSettingsPanel().reset();
    getBrowser().getMediaPanel().reset(true);
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

    // first column: creature and item selection
    JLabel l1 = new JLabel("Select CRE resource:");
    cbCreSelection = new JComboBox<>(model.getModelCreature());
    // this is a good default width for all selection controls in this panel
    cbCreSelection.setPrototypeDisplayValue(CreatureItem.getDefault());
    int defWidth = cbCreSelection.getPreferredSize().width * 5 / 4;
//    setPreferredWidth(cbCreSelection, defWidth);
    cbCreSelection.addActionListener(listeners);
    updateToolTip(cbCreSelection);

    JLabel l2 = new JLabel("Creature animation:");
    l2.setToolTipText("Supports manually entered numbers. Add \"0x\" prefix or \"h\" suffix to specify a hexadecimal number.");
    cbCreAnimation = new JComboBox<>(model.getModelAnimation());
    cbCreAnimation.getEditor().getEditorComponent().addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e)
      {
        if (e.getSource() instanceof JTextComponent) {
          ((JTextComponent)e.getSource()).selectAll();
        }
      }
    });
//    setPreferredWidth(cbCreAnimation, defWidth);
    cbCreAnimation.setEditable(true);
    cbCreAnimation.addActionListener(listeners);
    updateToolTip(cbCreAnimation);

    JLabel l3 = new JLabel("Status:");
    cbCreAllegiance = new JComboBox<>(model.getModelAllegiance());
//    setPreferredWidth(cbCreAllegiance, defWidth);
    cbCreAllegiance.addActionListener(listeners);
    updateToolTip(cbCreAllegiance);

    JLabel l4 = new JLabel("Helmet slot:");
    cbItemHelmet = new JComboBox<>(model.getModelHelmet());
    setPreferredWidth(cbItemHelmet, defWidth);
    cbItemHelmet.addActionListener(listeners);
    updateToolTip(cbItemHelmet);

    JLabel l5 = new JLabel("Armor slot:");
    cbItemArmor = new JComboBox<>(model.getModelArmor());
    setPreferredWidth(cbItemArmor, defWidth);
    cbItemArmor.addActionListener(listeners);
    updateToolTip(cbItemArmor);

    JLabel l6 = new JLabel("Shield slot:");
    cbItemShield = new JComboBox<>(model.getModelShield());
    setPreferredWidth(cbItemShield, defWidth);
    cbItemShield.addActionListener(listeners);
    updateToolTip(cbItemShield);

    JLabel l7 = new JLabel("Weapon slot:");
    cbItemWeapon = new JComboBox<>(model.getModelWeapon());
    setPreferredWidth(cbItemWeapon, defWidth);
    cbItemWeapon.addActionListener(listeners);
    updateToolTip(cbItemWeapon);

    JPanel pColumn1 = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pColumn1.add(l1, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    pColumn1.add(cbCreSelection, c);

    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pColumn1.add(l2, c);
    c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 0), 0, 0);
    pColumn1.add(cbCreAnimation, c);

    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pColumn1.add(l3, c);
    c = ViewerUtil.setGBC(c, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 0), 0, 0);
    pColumn1.add(cbCreAllegiance, c);

    c = ViewerUtil.setGBC(c, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.NONE, new Insets(24, 0, 0, 0), 0, 0);
    pColumn1.add(l4, c);
    c = ViewerUtil.setGBC(c, 1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(24, 8, 0, 0), 0, 0);
    pColumn1.add(cbItemHelmet, c);

    c = ViewerUtil.setGBC(c, 0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pColumn1.add(l5, c);
    c = ViewerUtil.setGBC(c, 1, 4, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 0), 0, 0);
    pColumn1.add(cbItemArmor, c);

    c = ViewerUtil.setGBC(c, 0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pColumn1.add(l6, c);
    c = ViewerUtil.setGBC(c, 1, 5, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 0), 0, 0);
    pColumn1.add(cbItemShield, c);

    c = ViewerUtil.setGBC(c, 0, 6, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pColumn1.add(l7, c);
    c = ViewerUtil.setGBC(c, 1, 6, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 0), 0, 0);
    pColumn1.add(cbItemWeapon, c);


    // second column: color selection
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

    final String[] labels = {"Metal color:", "Minor color:", "Major color:", "Skin color:",
                             "Leather color:", "Armor color:", "Hair color:"};
    for (int i = 0; i < labels.length; i++) {
      l1 = new JLabel(labels[i]);
      colorLabels.add(l1);
      cm = model.getModelColor(i);
      cb = new JComboBox<>(cm);
      cb.setRenderer(cm.getRenderer());
      if (i == 0 && ce != null) {
        cb.setPrototypeDisplayValue(ce);
        defWidth = cb.getPreferredSize().width;
      } else {
        cb.setPrototypeDisplayValue((ColorSelectionModel.ColorEntry)cm.getSelectedItem());
      }
      setPreferredWidth(cb, defWidth);
      cb.addActionListener(listeners);
      cm.addListDataListener(listeners);
      updateToolTip(cb);
      colorControls.add(cb);
    }

    JPanel pColumn2 = new JPanel(new GridBagLayout());
    for (int i = 0; i < colorControls.size(); i++) {
      int top = (i == 0) ? 0 : 8;
      c = ViewerUtil.setGBC(c, 0, i, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                            GridBagConstraints.NONE, new Insets(top, 0, 0, 0), 0, 0);
      pColumn2.add(colorLabels.get(i), c);
      c = ViewerUtil.setGBC(c, 1, i, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(top, 8, 0, 0), 0, 0);
      pColumn2.add(colorControls.get(i), c);
    }


    // third column: buttons
    Insets margin;
    bApply = new JButton("Apply", Icons.getIcon(Icons.ICON_REFRESH_16));
    Font fnt = bApply.getFont().deriveFont(bApply.getFont().getSize2D() * 1.25f);
    bApply.setFont(fnt);
    margin = bApply.getMargin();
    margin.top += 4;  margin.bottom += 4;
    bApply.setMargin(margin);
    bApply.addActionListener(listeners);
    bReset = new JButton("Reset", Icons.getIcon(Icons.ICON_UNDO_16));
    margin = bReset.getMargin();
    margin.top += 4; margin.bottom += 4;
    bReset.setMargin(margin);
    bReset.setToolTipText("Revert to creature defaults");
    bReset.addActionListener(listeners);

    JPanel pButtons = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pButtons.add(bApply, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    pButtons.add(bReset, c);
    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.VERTICAL, new Insets(8, 0, 0, 0), 0, 0);
    pButtons.add(new JPanel(), c);


    // fourth column: show/hide panel button
    bHidePanel = new JButton("Hide panel");
    margin = bHidePanel.getMargin();
    margin.top += 4; margin.bottom += 4;
    bHidePanel.setMargin(margin);
    defWidth = bHidePanel.getPreferredSize().width;
    bHidePanel.addActionListener(listeners);

    JPanel pColumn4 = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_END,
                          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pColumn4.add(bHidePanel, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_END,
                          GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    pColumn4.add(new JPanel(), c);

    // combining columns
    JPanel panelShown = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    panelShown.add(new JPanel(), c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(8, 8, 8, 0), 0, 0);
    panelShown.add(pColumn1, c);
    c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(8, 8, 8, 0), 0, 0);
    panelShown.add(pButtons, c);
    c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(8, 32, 8, 0), 0, 0);
    panelShown.add(pColumn2, c);
    c = ViewerUtil.setGBC(c, 4, 0, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.BOTH, new Insets(8, 16, 8, 8), 0, 0);
    panelShown.add(pColumn4, c);

    scrollShown = new JScrollPane(panelShown, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollShown.getHorizontalScrollBar().setUnitIncrement(32);
    scrollShown.setBorder(panelShown.getBorder());

    // hidden panel
    bShowPanel = new JButton("Show panel");
    margin = bShowPanel.getMargin();
    margin.top += 4; margin.bottom += 4;
    bShowPanel.setMargin(margin);
    defWidth = Math.max(defWidth, bShowPanel.getPreferredSize().width);
    bShowPanel.setPreferredSize(new Dimension(defWidth, bShowPanel.getPreferredSize().height));
    bShowPanel.addActionListener(listeners);

    bHidePanel.setPreferredSize(new Dimension(defWidth, bHidePanel.getPreferredSize().height));

    panelHidden = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_END,
                          GridBagConstraints.NONE, new Insets(8, 8, 8, 8), 0, 0);
    panelHidden.add(bShowPanel, c);

    layoutMain = new CardLayout();
    panelMain = new JPanel(layoutMain);
    panelMain.add(scrollShown);
    panelMain.add(panelHidden);
    layoutMain.first(panelMain);

    setLayout(new BorderLayout());
    add(panelMain, BorderLayout.CENTER);

    fireSettingsChanged();
  }

  /**
   * Returns {@code true} if the pending reset can be performed. Shows a confirmation dialog if changes were made.
   */
  private boolean confirmReset()
  {
    boolean retVal = true;
    if (getControlModel().canReset()) {
      retVal = (JOptionPane.showConfirmDialog(getBrowser(),
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
          ex.printStackTrace();
          getBrowser().showErrorMessage(ex.getMessage(), "Creature selection");
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
      else if (e.getSource() == bHidePanel) {
        layoutMain.last(panelMain);
        panelMain.setPreferredSize(panelHidden.getPreferredSize());
      }
      else if (e.getSource() == bShowPanel) {
        layoutMain.first(panelMain);
        panelMain.setPreferredSize(scrollShown.getPreferredSize());
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
