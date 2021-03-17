// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.viewer;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Composite;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EtchedBorder;

import org.infinity.gui.ViewerUtil;
import org.infinity.resource.Profile;
import org.infinity.resource.cre.viewer.bg.Backgrounds;
import org.infinity.resource.cre.viewer.icon.Icons;

/**
 * This panel provides controls for visual settings and includes a table view of creature animation attributes.
 */
public class SettingsPanel extends JPanel
{
  // Available items for zoom selection list
  private static final Vector<ItemString<Integer>> zoomList = new Vector<ItemString<Integer>>() {{
    add(ItemString.with("50 %", 50));
    add(ItemString.with("100 % (original)", 100));
    add(ItemString.with("200 %", 200));
    add(ItemString.with("300 %", 300));
    add(ItemString.with("400 %", 400));
  }};

  // Available items for frame rate selection list
  private static final Vector<ItemString<Integer>> frameRateList = new Vector<ItemString<Integer>>() {{
    add(ItemString.with("1 frames/sec.", 1));
    add(ItemString.with("5 frames/sec.", 5));
    add(ItemString.with("10 frames/sec.", 10));
    add(ItemString.with("15 frames/sec. (original)", 15));
    add(ItemString.with("20 frames/sec.", 20));
    add(ItemString.with("25 frames/sec.", 25));
    add(ItemString.with("30 frames/sec.", 30));
  }};

  private static int indexZoom, indexFrameRate, indexBackground;
  private static boolean isFiltering, isBlending, isSelectionCircle, isPersonalSpace, isPaletteReplacementEnabled,
                         isShowAvatar, isShowHelmet, isShowShield, isShowWeapon, isShowBorders;

  static {
    indexZoom = 1;        // 100 % (original)
    indexFrameRate = 3;   // 15 fps (original)
    indexBackground = 0;  // System color
    isFiltering = false;
    isBlending = true;
    isSelectionCircle = false;
    isPersonalSpace = false;
    isShowAvatar = true;
    isShowHelmet = true;
    isShowShield = true;
    isShowWeapon = true;
    isShowBorders = false;
    isPaletteReplacementEnabled = true;
  }

  private final Listeners listeners = new Listeners();
  private final CreatureViewer viewer;

  private JComboBox<ItemString<Integer>> cbZoom;
  private JComboBox<ItemString<Integer>> cbFrameRate;
  private JComboBox<Backgrounds.BackgroundInfo> cbBackground;
  private JButton bCenter;
  private JCheckBox cbFiltering, cbBlending, cbSelectionCircle, cbPersonalSpace, cbPaletteReplacementEnabled,
                    cbShowAvatar, cbShowHelmet, cbShowShield, cbShowWeapon, cbShowBorders;
  private AttributesPanel panelAttributes;

  public SettingsPanel(CreatureViewer viewer)
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
    applyBackgroundInfo();
    getAttributesPanel().reset();
  }

  /** Returns the currently selected zoom (in percent). */
  public int getZoom()
  {
    int retVal = 100;
    ItemString<Integer> is = cbZoom.getModel().getElementAt(indexZoom);
    if (is != null) {
      retVal = is.getData().intValue();
    }
    return retVal;
  }

  private void setZoomIndex(int index)
  {
    if (index != indexZoom) {
      if (index >= 0 && index < cbZoom.getModel().getSize()) {
        indexZoom = index;
        cbZoom.setSelectedIndex(indexZoom);
        getViewer().getRenderPanel().setZoom((float)getZoom() / 100.0f);
        getViewer().getRenderPanel().updateCanvas();
      } else {
        throw new IndexOutOfBoundsException();
      }
    }
  }

  /** Returns the currently selected frame rate. */
  public int getFrameRate()
  {
    int retVal = 15;
    ItemString<Integer> is = cbFrameRate.getModel().getElementAt(indexFrameRate);
    if (is != null) {
      retVal = is.getData().intValue();
    }
    return retVal;
  }

  private void setFrameRateIndex(int index)
  {
    if (index != indexFrameRate) {
      if (index >= 0 && index < cbFrameRate.getModel().getSize()) {
        indexFrameRate = index;
        cbFrameRate.setSelectedIndex(indexFrameRate);
        getViewer().getMediaPanel().setFrameRate(getFrameRate());
      } else {
        throw new IndexOutOfBoundsException();
      }
    }
  }

  /** Returns the selected {@code BackgroundInfo} object. */
  public Backgrounds.BackgroundInfo getBackgroundInfo() { return cbBackground.getModel().getElementAt(indexBackground); }

  private void setBackgroundInfoIndex(int index)
  {
    if (index != indexBackground) {
      if (index >= 0 && index < cbBackground.getModel().getSize()) {
        indexBackground = index;
        cbBackground.setSelectedIndex(indexBackground);
        applyBackgroundInfo();
      } else {
        throw new IndexOutOfBoundsException();
      }
    }
  }

  private void applyBackgroundInfo()
  {
    Backgrounds.BackgroundInfo info = getBackgroundInfo();
    if (info != null) {
      getViewer().getRenderPanel().setBackgroundColor(info.getColor());
      getViewer().getRenderPanel().setBackgroundImage(info.getImage());
      getViewer().getRenderPanel().setBackgroundCenter(info.getCenter());
      getViewer().getRenderPanel().updateCanvas();
    }
  }

  /** Returns whether bilinear filtering is enabled for sprite display. */
  public boolean isFilteringEnabled() { return isFiltering; }

  private void setFilteringEnabled(boolean b)
  {
    if (isFiltering != b) {
      isFiltering = b;
      cbFiltering.setSelected(isFiltering);
      getViewer().getRenderPanel().setFilterEnabled(isFiltering);
    }
  }

  /** Returns whether special blending effects are enabled for selected creature animations. */
  public boolean isBlendingEnabled() { return isBlending; }

  private void setBlendingEnabled(boolean b)
  {
    if (isBlending != b) {
      isBlending = b;
      cbBlending.setSelected(isBlending);
      getViewer().getRenderPanel().setComposite(getComposite());
    }
  }

  /** Returns whether the selection circle is visible. */
  public boolean isSelectionCircleEnabled() { return isSelectionCircle; }

  private void setSelectionCircleEnabled(boolean b)
  {
    if (isSelectionCircle != b) {
      isSelectionCircle = b;
      cbSelectionCircle.setSelected(isSelectionCircle);
      getViewer().getMediaPanel().reset(true);
    }
  }

  /** Returns whether personal space is visible. */
  public boolean isPersonalSpaceEnabled() { return isPersonalSpace; }

  private void setPersonalSpaceEnabled(boolean b)
  {
    if (isPersonalSpace != b) {
      isPersonalSpace = b;
      cbPersonalSpace.setSelected(isPersonalSpace);
      getViewer().getMediaPanel().reset(true);
    }
  }

  /** Returns whether palette replacement (full palette or false colors) is enabled. */
  public boolean isPaletteReplacementEnabled() { return isPaletteReplacementEnabled; }

  private void setPaletteReplacementEnabled(boolean b)
  {
    if (isPaletteReplacementEnabled != b) {
      isPaletteReplacementEnabled = b;
      cbPaletteReplacementEnabled.setSelected(isPaletteReplacementEnabled);
      getViewer().getMediaPanel().reset(true);
    }
  }

  /** Returns whether the creature animation avatar is drawn. */
  public boolean isAvatarVisible() { return isShowAvatar; }

  private void setAvatarVisible(boolean b)
  {
    if (isShowAvatar != b) {
      isShowAvatar = b;
      cbShowAvatar.setSelected(isShowAvatar);
      getViewer().getMediaPanel().reset(true);
    }
  }

  /** Returns whether the helmet overlay is drawn. */
  public boolean isHelmetVisible() { return isShowHelmet; }

  private void setHelmetVisible(boolean b)
  {
    if (isShowHelmet != b) {
      isShowHelmet = b;
      cbShowHelmet.setSelected(isShowHelmet);
      getViewer().getMediaPanel().reset(true);
    }
  }

  /** Returns whether the shield overlay is drawn. */
  public boolean isShieldVisible() { return isShowShield; }

  private void setShieldVisible(boolean b)
  {
    if (isShowShield != b) {
      isShowShield = b;
      cbShowShield.setSelected(isShowShield);
      getViewer().getMediaPanel().reset(true);
    }
  }

  /** Returns whether the weapon overlay is drawn. */
  public boolean isWeaponVisible() { return isShowWeapon; }

  private void setWeaponVisible(boolean b)
  {
    if (isShowWeapon != b) {
      isShowWeapon = b;
      cbShowWeapon.setSelected(isShowWeapon);
      getViewer().getMediaPanel().reset(true);
    }
  }

  /** Returns whether colorered borders are drawn around sprite avatar and overlays. */
  public boolean isOverlayBordersVisible() { return isShowBorders; }

  private void setOverlayBordersVisible(boolean b)
  {
    if (isShowBorders != b) {
      isShowBorders = b;
      cbShowBorders.setSelected(isShowBorders);
      getViewer().getMediaPanel().reset(true);
    }
  }

  /** Returns the {@link Composite} object valid for the currently loaded {@code SpriteDecoder} instance. */
  public Composite getComposite()
  {
    Composite retVal = AlphaComposite.SrcOver;
    if (getViewer().getDecoder() != null && isBlendingEnabled()) {
      retVal = getViewer().getDecoder().getComposite();
    }
    return retVal;
  }

  /** Provides access to the creature animation attributes panel. */
  public AttributesPanel getAttributesPanel() { return panelAttributes; }

  private void init()
  {
    GridBagConstraints c = new GridBagConstraints();

    // selection controls
    JLabel l1 = new JLabel("Zoom:");
    cbZoom = new JComboBox<SettingsPanel.ItemString<Integer>>(zoomList);
    cbZoom.setPrototypeDisplayValue(zoomList.get(1));
    cbZoom.setSelectedIndex(indexZoom);
    cbZoom.addActionListener(listeners);

    bCenter = new JButton(Icons.getIcon(Icons.ICON_CENTER));
    bCenter.setToolTipText("Center display on creature animation.");
    bCenter.addActionListener(listeners);

    JLabel l2 = new JLabel("Frame rate:");
    cbFrameRate = new JComboBox<SettingsPanel.ItemString<Integer>>(frameRateList);
    cbFrameRate.setPrototypeDisplayValue(frameRateList.get(2));
    cbFrameRate.setSelectedIndex(indexFrameRate);
    cbFrameRate.addActionListener(listeners);

    JLabel l3 = new JLabel("Background:");
    List<Backgrounds.BackgroundInfo> bgList = Backgrounds.getBackgrounds(Profile.getGame());
    cbBackground = new JComboBox<Backgrounds.BackgroundInfo>(bgList.toArray(new Backgrounds.BackgroundInfo[bgList.size()]));
    cbBackground.setPrototypeDisplayValue(Backgrounds.BackgroundList.get(1));
    cbBackground.setSelectedIndex(indexBackground);
    cbBackground.addActionListener(listeners);

    JPanel panel1 = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_END,
                          GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0);
    panel1.add(l1, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0);
    panel1.add(cbZoom, c);
    c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.VERTICAL, new Insets(0, 8, 0, 0), 0, 0);
    panel1.add(bCenter, c);

    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_END,
                          GridBagConstraints.VERTICAL, new Insets(8, 0, 0, 0), 0, 0);
    panel1.add(l2, c);
    c = ViewerUtil.setGBC(c, 1, 1, 2, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 4, 0, 0), 0, 0);
    panel1.add(cbFrameRate, c);

    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_END,
                          GridBagConstraints.VERTICAL, new Insets(8, 0, 0, 0), 0, 0);
    panel1.add(l3, c);
    c = ViewerUtil.setGBC(c, 1, 2, 2, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 4, 0, 0), 0, 0);
    panel1.add(cbBackground, c);


    // checkbox controls
    cbFiltering = new JCheckBox("Enable filtering", isFiltering);
    cbFiltering.addActionListener(listeners);

    cbBlending= new JCheckBox("Enable blending", isBlending);
    cbBlending.setToolTipText("Affects only creature animations with special blending attributes (e.g. nishruus or wisps)");
    cbBlending.addActionListener(listeners);

    cbSelectionCircle = new JCheckBox("Show selection circle", isSelectionCircle);
    cbSelectionCircle.addActionListener(listeners);

    cbPersonalSpace = new JCheckBox("Show personal space", isPersonalSpace);
    cbPersonalSpace.addActionListener(listeners);

    cbShowAvatar = new JCheckBox("Show avatar overlay", isShowAvatar);
    cbShowAvatar.addActionListener(listeners);

    cbShowHelmet = new JCheckBox("Show helmet overlay", isShowHelmet);
    cbShowHelmet.addActionListener(listeners);

    cbShowShield = new JCheckBox("Show shield overlay", isShowShield);
    cbShowShield.addActionListener(listeners);

    cbShowWeapon = new JCheckBox("Show weapon overlay", isShowWeapon);
    cbShowWeapon.addActionListener(listeners);

    cbShowBorders = new JCheckBox("Show overlay borders", isShowBorders);
    cbShowBorders.setToolTipText("Draw borders around individual segments of the creature animation.");
    cbShowBorders.addActionListener(listeners);

    cbPaletteReplacementEnabled = new JCheckBox("Palette replacement", isPaletteReplacementEnabled);
    cbPaletteReplacementEnabled.setToolTipText("Enable full palette or false color palette replacement.");
    cbPaletteReplacementEnabled.addActionListener(listeners);

    JPanel panel2 = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    panel2.add(cbFiltering, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 16, 0, 0), 0, 0);
    panel2.add(cbBlending, c);

    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    panel2.add(cbSelectionCircle, c);
    c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 16, 0, 0), 0, 0);
    panel2.add(cbPersonalSpace, c);

    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    panel2.add(cbShowAvatar, c);
    c = ViewerUtil.setGBC(c, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 16, 0, 0), 0, 0);
    panel2.add(cbShowHelmet, c);

    c = ViewerUtil.setGBC(c, 0, 3, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    panel2.add(cbShowWeapon, c);
    c = ViewerUtil.setGBC(c, 1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 16, 0, 0), 0, 0);
    panel2.add(cbShowShield, c);

    c = ViewerUtil.setGBC(c, 0, 4, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    panel2.add(cbShowBorders, c);
    c = ViewerUtil.setGBC(c, 1, 4, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 16, 0, 0), 0, 0);
    panel2.add(cbPaletteReplacementEnabled, c);


    // attributes table panel
    panelAttributes = new AttributesPanel(getViewer());


    // combining panels
    JPanel panelMain = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 8), 0, 0);
    panelMain.add(panel1, c);

    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(16, 8, 0, 8), 0, 0);
    panelMain.add(panel2, c);

    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.BOTH, new Insets(16, 8, 8, 8), 0, 0);
    panelMain.add(panelAttributes, c);

    JScrollPane scroll = new JScrollPane(panelMain);
    scroll.setBorder(new EtchedBorder());

    setLayout(new BorderLayout());
    add(scroll, BorderLayout.CENTER);
  }

//-------------------------- INNER CLASSES --------------------------

  private class Listeners implements ActionListener
  {
    public Listeners()
    {
    }

    //--------------------- Begin Interface ActionListener ---------------------

    @Override
    public void actionPerformed(ActionEvent e)
    {
      if (e.getSource() == bCenter) {
        getViewer().getRenderPanel().centerOnSprite();
      }
      else if (e.getSource() == cbZoom) {
        setZoomIndex(cbZoom.getSelectedIndex());
      }
      else if (e.getSource() == cbFrameRate) {
        setFrameRateIndex(cbFrameRate.getSelectedIndex());
      }
      else if (e.getSource() == cbBackground) {
        setBackgroundInfoIndex(cbBackground.getSelectedIndex());
      }
      else if (e.getSource() == cbFiltering) {
        setFilteringEnabled(cbFiltering.isSelected());
      }
      else if (e.getSource() == cbBlending) {
        setBlendingEnabled(cbBlending.isSelected());
      }
      else if (e.getSource() == cbSelectionCircle) {
        setSelectionCircleEnabled(cbSelectionCircle.isSelected());
      }
      else if (e.getSource() == cbPersonalSpace) {
        setPersonalSpaceEnabled(cbPersonalSpace.isSelected());
      }
      else if (e.getSource() == cbShowAvatar) {
        setAvatarVisible(cbShowAvatar.isSelected());
      }
      else if (e.getSource() == cbShowHelmet) {
        setHelmetVisible(cbShowHelmet.isSelected());
      }
      else if (e.getSource() == cbShowShield) {
        setShieldVisible(cbShowShield.isSelected());
      }
      else if (e.getSource() == cbShowWeapon) {
        setWeaponVisible(cbShowWeapon.isSelected());
      }
      else if (e.getSource() == cbShowBorders) {
        setOverlayBordersVisible(cbShowBorders.isSelected());
      }
      else if (e.getSource() == cbPaletteReplacementEnabled) {
        setPaletteReplacementEnabled(cbPaletteReplacementEnabled.isSelected());
      }
    }

    //--------------------- Begin Interface ActionListener ---------------------
  }

  /**
   * Helper class: String with attached user-defined data.
   *
   * @param <T> type of the user-defined data
   */
  private static class ItemString<T>
  {
    private final T data;
    private final String text;

    public static <T> ItemString<T> with(String text, T data)
    {
      return new ItemString<T>(text, data);
    }

    public ItemString(String text, T data)
    {
      this.text = (text != null) ? text : "";
      this.data = data;
    }

    public T getData() { return data; }

    @Override
    public String toString()
    {
      return text;
    }
  }
}
