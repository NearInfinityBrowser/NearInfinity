// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.browser;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.colorchooser.AbstractColorChooserPanel;

import org.infinity.gui.ViewerUtil;
import org.infinity.resource.Profile;
import org.infinity.resource.cre.browser.bg.Backgrounds;
import org.infinity.resource.cre.browser.bg.Backgrounds.BackgroundInfo;
import org.infinity.resource.cre.browser.icon.Icons;
import org.infinity.util.tuples.Monuple;

/**
 * This panel provides controls for visual settings and includes a table view of creature animation attributes.
 */
public class SettingsPanel extends JPanel
{
  // Available render canvas backgrounds
  public static final List<Backgrounds.BackgroundInfo> backgroundList = new ArrayList<Backgrounds.BackgroundInfo>() {{
    add(Backgrounds.BG_COLOR_NONE);
    add(Backgrounds.BG_CAVE_BG);
    add(Backgrounds.BG_CITY_NIGHT_SOD);
    add(Backgrounds.BG_WILDERNESS_BG);
    add(Backgrounds.BG_WILDERNESS_IWD);
    add(Backgrounds.BG_CITY_PST);
    add(Backgrounds.BG_DUNGEON_PST);
    add(Backgrounds.BG_COLOR_WHITE);
    add(Backgrounds.BG_COLOR_BLACK);
    add(Backgrounds.BG_COLOR_LIGHT_GRAY);
    add(Backgrounds.BG_COLOR_GRAY);
    // REMEMBER: has to be last entry in list
    add(new Backgrounds.BackgroundInfo("Customize color...", Color.WHITE));
  }};

  // Available items for zoom selection list
  private static final Vector<ItemString<Integer>> zoomList = new Vector<ItemString<Integer>>() {{
    add(ItemString.with("50 %", 50));
    add(ItemString.with("100 % (original)", 100));
    add(ItemString.with("200 %", 200));
    add(ItemString.with("300 %", 300));
    add(ItemString.with("400 %", 400));
    add(ItemString.with("500 %", 500));
  }};

  // Available items for frame rate selection list
  private static final Vector<ItemString<Integer>> frameRateList = new Vector<ItemString<Integer>>() {{
    add(ItemString.with("1 frames/sec.", 1));
    add(ItemString.with("2 frames/sec.", 2));
    add(ItemString.with("5 frames/sec.", 5));
    add(ItemString.with("10 frames/sec.", 10));
    add(ItemString.with("15 frames/sec. (original)", 15));
    add(ItemString.with("20 frames/sec.", 20));
    add(ItemString.with("25 frames/sec.", 25));
    add(ItemString.with("30 frames/sec.", 30));
    add(ItemString.with("50 frames/sec.", 50));
    add(ItemString.with("60 frames/sec.", 60));
  }};

  private static int indexZoom, indexFrameRate, indexBackground;
  private static boolean isFiltering, isBlending, isTranslucent, isSelectionCircle, isOrnateSelectionCircle, isPersonalSpace,
                         isTintEnabled, isBlurEnabled, isPaletteReplacementEnabled, isShowBorders,
                         isShowAvatar, isShowHelmet, isShowShield, isShowWeapon;

  static {
    resetSettings();
  }

  private final Listeners listeners = new Listeners();
  private final CreatureBrowser browser;

  private JComboBox<ItemString<Integer>> cbZoom;
  private JComboBox<ItemString<Integer>> cbFrameRate;
  private JComboBox<Backgrounds.BackgroundInfo> cbBackground;
  private JButton bCenter;
  private JCheckBox cbFiltering, cbBlending, cbTranslucent, cbSelectionCircle, cbOrnateSelectionCircle, cbPersonalSpace,
                    cbTintEnabled, cbBlurEnabled, cbPaletteReplacementEnabled, cbShowBorders,
                    cbShowAvatar, cbShowHelmet, cbShowShield, cbShowWeapon;
  private AttributesPanel panelAttributes;

  /** Returns a list of background info instances available for the specified game. */
  public static List<BackgroundInfo> getBackgrounds(Profile.Game game)
  {
    return backgroundList
        .stream()
        .filter(bi -> bi.getGames().contains((game != null) ? game : Profile.getGame()))
        .collect(Collectors.toList());
  }

  /** Initializes global settings with sane defaults. */
  private static void resetSettings()
  {
    indexZoom = 1;        // 100 % (original)
    indexFrameRate = 4;   // 15 fps (original)
    indexBackground = 0;  // System color
    isFiltering = false;
    isBlending = true;
    isTranslucent = true;
    isSelectionCircle = false;
    isOrnateSelectionCircle = (Profile.getGame() == Profile.Game.PST) || (Profile.getGame() == Profile.Game.PSTEE);
    isPersonalSpace = false;
    isShowAvatar = true;
    isShowHelmet = true;
    isShowShield = true;
    isShowWeapon = true;
    isShowBorders = false;
    isTintEnabled = true;
    isBlurEnabled = true;
    isPaletteReplacementEnabled = true;
  }

  public SettingsPanel(CreatureBrowser browser)
  {
    super();
    this.browser = Objects.requireNonNull(browser);
    resetSettings();
    init();
  }

  /** Returns the associated {@code CreatureBrowser} instance. */
  public CreatureBrowser getBrowser() { return browser; }

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
        getBrowser().getRenderPanel().setZoom((float)getZoom() / 100.0f);
        getBrowser().getRenderPanel().updateCanvas();
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
        getBrowser().getMediaPanel().setFrameRate(getFrameRate());
      } else {
        throw new IndexOutOfBoundsException();
      }
    }
  }

  /** Returns the selected {@code BackgroundInfo} object. */
  public Backgrounds.BackgroundInfo getBackgroundInfo() { return cbBackground.getModel().getElementAt(indexBackground); }

  private void setBackgroundInfoIndex(int index)
  {
    if (index != indexBackground || index == cbBackground.getModel().getSize() - 1) {
      if (index >= 0 && index < cbBackground.getModel().getSize()) {
        if (index == cbBackground.getModel().getSize() - 1) {
          // special: define custom color
          Backgrounds.BackgroundInfo info = cbBackground.getModel().getElementAt(index);
          Color color = getCustomColor(info.getColor());
          if (color != null) {
            info = cbBackground.getModel().getElementAt(index);
            info.setColor(color);
            info.setLabel(String.format("Customize color: RGB(%d,%d,%d)", color.getRed(), color.getGreen(), color.getBlue()));
          } else {
            cbBackground.setSelectedIndex(indexBackground);
            return;
          }
        }

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
      getBrowser().getRenderPanel().setBackgroundColor(info.getColor());
      getBrowser().getRenderPanel().setBackgroundImage(info.getImage());
      getBrowser().getRenderPanel().setBackgroundCenter(info.getCenter());
      getBrowser().getRenderPanel().updateCanvas();
    }
  }

  /** Returns whether bilinear filtering is enabled for sprite display. */
  public boolean isFilteringEnabled() { return isFiltering; }

  private void setFilteringEnabled(boolean b)
  {
    if (isFiltering != b) {
      isFiltering = b;
      cbFiltering.setSelected(isFiltering);
      getBrowser().getRenderPanel().setFilterEnabled(isFiltering);
    }
  }

  /** Returns whether special blending effects are enabled for selected creature animations. */
  public boolean isBlendingEnabled() { return isBlending; }

  private void setBlendingEnabled(boolean b)
  {
    if (isBlending != b) {
      isBlending = b;
      cbBlending.setSelected(isBlending);
      getBrowser().getRenderPanel().setComposite(getComposite());
    }
  }

  /** Returns whether translucency is enabled for selected creature animations. */
  public boolean isTranslucencyEnabled() { return isTranslucent; }

  private void setTranslucencyEnabled(boolean b)
  {
    if (isTranslucent != b) {
      isTranslucent = b;
      cbTranslucent.setSelected(isTranslucent);
      getBrowser().getMediaPanel().reset(true);
    }
  }

  /** Returns whether the selection circle is visible. */
  public boolean isSelectionCircleEnabled() { return isSelectionCircle; }

  private void setSelectionCircleEnabled(boolean b)
  {
    if (isSelectionCircle != b) {
      isSelectionCircle = b;
      cbSelectionCircle.setSelected(isSelectionCircle);
      getBrowser().getDecoder().setSelectionCircleEnabled(isSelectionCircle);
      getBrowser().getRenderPanel().updateCanvas();
    }
  }

  /** Returns whether an ornate graphics is used to draw the selection circle. */
  public boolean isOrnateSelectionCircle() { return isOrnateSelectionCircle; }

  private void setOrnateSelectionCircle(boolean b)
  {
    if (isOrnateSelectionCircle != b) {
      isOrnateSelectionCircle = b;
      cbOrnateSelectionCircle.setSelected(isOrnateSelectionCircle);
      getBrowser().getDecoder().setSelectionCircleBitmap(isOrnateSelectionCircle);
      getBrowser().getRenderPanel().updateCanvas();
    }
  }

  /** Returns whether personal space is visible. */
  public boolean isPersonalSpaceEnabled() { return isPersonalSpace; }

  private void setPersonalSpaceEnabled(boolean b)
  {
    if (isPersonalSpace != b) {
      isPersonalSpace = b;
      cbPersonalSpace.setSelected(isPersonalSpace);
      getBrowser().getDecoder().setPersonalSpaceVisible(isPersonalSpace);
      getBrowser().getRenderPanel().updateCanvas();
    }
  }

  /** Returns whether tint effects (e.g. from opcodes 51/52) are enabled. */
  public boolean isTintEnabled() { return isTintEnabled; }

  private void setTintEnabled(boolean b)
  {
    if (isTintEnabled != b) {
      isTintEnabled = b;
      cbTintEnabled.setSelected(isTintEnabled);
      getBrowser().getMediaPanel().reset(true);
    }
  }

  /** Returns whether blur effect (opcode 66) is enabled. */
  public boolean isBlurEnabled() { return isBlurEnabled; }

  private void setBlurEnabled(boolean b)
  {
    if (isBlurEnabled != b) {
      isBlurEnabled = b;
      cbBlurEnabled.setSelected(isBlurEnabled);
      getBrowser().getMediaPanel().reset(true);
    }
  }

  /** Returns whether palette replacement (full palette or false colors) is enabled. */
  public boolean isPaletteReplacementEnabled() { return isPaletteReplacementEnabled; }

  private void setPaletteReplacementEnabled(boolean b)
  {
    if (isPaletteReplacementEnabled != b) {
      isPaletteReplacementEnabled = b;
      cbPaletteReplacementEnabled.setSelected(isPaletteReplacementEnabled);
      getBrowser().getMediaPanel().reset(true);
    }
  }

  /** Returns whether the creature animation avatar is drawn. */
  public boolean isAvatarVisible() { return isShowAvatar; }

  private void setAvatarVisible(boolean b)
  {
    if (isShowAvatar != b) {
      isShowAvatar = b;
      cbShowAvatar.setSelected(isShowAvatar);
      getBrowser().getMediaPanel().reset(true);
    }
  }

  /** Returns whether the helmet overlay is drawn. */
  public boolean isHelmetVisible() { return isShowHelmet; }

  private void setHelmetVisible(boolean b)
  {
    if (isShowHelmet != b) {
      isShowHelmet = b;
      cbShowHelmet.setSelected(isShowHelmet);
      getBrowser().getMediaPanel().reset(true);
    }
  }

  /** Returns whether the shield overlay is drawn. */
  public boolean isShieldVisible() { return isShowShield; }

  private void setShieldVisible(boolean b)
  {
    if (isShowShield != b) {
      isShowShield = b;
      cbShowShield.setSelected(isShowShield);
      getBrowser().getMediaPanel().reset(true);
    }
  }

  /** Returns whether the weapon overlay is drawn. */
  public boolean isWeaponVisible() { return isShowWeapon; }

  private void setWeaponVisible(boolean b)
  {
    if (isShowWeapon != b) {
      isShowWeapon = b;
      cbShowWeapon.setSelected(isShowWeapon);
      getBrowser().getMediaPanel().reset(true);
    }
  }

  /** Returns whether colorered borders are drawn around sprite avatar and overlays. */
  public boolean isOverlayBordersVisible() { return isShowBorders; }

  private void setOverlayBordersVisible(boolean b)
  {
    if (isShowBorders != b) {
      isShowBorders = b;
      cbShowBorders.setSelected(isShowBorders);
      getBrowser().getMediaPanel().reset(true);
    }
  }

  /** Returns the {@link Composite} object valid for the currently loaded {@code SpriteDecoder} instance. */
  public Composite getComposite()
  {
    Composite retVal = AlphaComposite.SrcOver;
    if (getBrowser().getDecoder() != null && isBlendingEnabled()) {
      retVal = getBrowser().getDecoder().getComposite();
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
    cbZoom = new JComboBox<>(zoomList);
    cbZoom.setPrototypeDisplayValue(zoomList.get(1));
    cbZoom.setSelectedIndex(indexZoom);
    cbZoom.addActionListener(listeners);

    bCenter = new JButton(Icons.getIcon(Icons.ICON_CENTER));
    bCenter.setToolTipText("Center display on creature animation.");
    bCenter.addActionListener(listeners);

    JLabel l2 = new JLabel("Frame rate:");
    cbFrameRate = new JComboBox<>(frameRateList);
    cbFrameRate.setPrototypeDisplayValue(frameRateList.get(2));
    cbFrameRate.setSelectedIndex(indexFrameRate);
    cbFrameRate.addActionListener(listeners);

    JLabel l3 = new JLabel("Background:");
    List<Backgrounds.BackgroundInfo> bgList = getBackgrounds(Profile.getGame());
    cbBackground = new JComboBox<>(bgList.toArray(new Backgrounds.BackgroundInfo[bgList.size()]));
    cbBackground.setPrototypeDisplayValue(backgroundList.get(1));
    cbBackground.setSelectedIndex(indexBackground);
    cbBackground.addActionListener(listeners);

    JPanel panel1 = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0);
    panel1.add(l1, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0);
    panel1.add(cbZoom, c);
    c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 1.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.VERTICAL, new Insets(0, 8, 0, 0), 0, 0);
    panel1.add(bCenter, c);

    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.VERTICAL, new Insets(8, 0, 0, 0), 0, 0);
    panel1.add(l2, c);
    c = ViewerUtil.setGBC(c, 1, 1, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 4, 0, 0), 0, 0);
    panel1.add(cbFrameRate, c);

    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.VERTICAL, new Insets(8, 0, 0, 0), 0, 0);
    panel1.add(l3, c);
    c = ViewerUtil.setGBC(c, 1, 2, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 4, 0, 0), 0, 0);
    panel1.add(cbBackground, c);


    // checkbox controls
    cbFiltering = new JCheckBox("Enable filtering", isFiltering);
    cbFiltering.setToolTipText("On: bilinear filtering, off: nearest neighbor filtering");
    cbFiltering.addActionListener(listeners);

    cbBlending = new JCheckBox("Enable blending", isBlending);
    cbBlending.setToolTipText("Affects only creature animations with special blending attributes (e.g. movanic devas or wisps).");
    cbBlending.addActionListener(listeners);

    cbTranslucent = new JCheckBox("Enable translucency", isTranslucent);
    cbTranslucent.setToolTipText("Affects only creature animations with translucency effect (e.g. ghosts or air elementals).");
    cbTranslucent.addActionListener(listeners);

    cbTintEnabled = new JCheckBox("Enable tint effect", isTintEnabled);
    cbTintEnabled.setToolTipText("Includes color modifications defined by effect opcodes 8, 51 and 52.");
    cbTintEnabled.addActionListener(listeners);

    cbBlurEnabled = new JCheckBox("Enable blur effect", isBlurEnabled);
    cbBlurEnabled.addActionListener(listeners);

    cbPaletteReplacementEnabled = new JCheckBox("Enable palette replacement", isPaletteReplacementEnabled);
    cbPaletteReplacementEnabled.setToolTipText("Enable full palette or false color palette replacement.");
    cbPaletteReplacementEnabled.addActionListener(listeners);

    cbSelectionCircle = new JCheckBox("Show selection circle", isSelectionCircle);
    cbSelectionCircle.addActionListener(listeners);

    cbOrnateSelectionCircle = new JCheckBox("Use ornate selection circle", isOrnateSelectionCircle);
    cbOrnateSelectionCircle.setToolTipText("Enable to use the ornate selection circle graphics from PST.");
    cbOrnateSelectionCircle.addActionListener(listeners);

    cbPersonalSpace = new JCheckBox("Show personal space", isPersonalSpace);
    cbPersonalSpace.setToolTipText("Enable to visualize the search map cells blocked by the creature.");
    cbPersonalSpace.addActionListener(listeners);

    cbShowAvatar = new JCheckBox("Show avatar overlay", isShowAvatar);
    cbShowAvatar.setToolTipText("Includes the avatar, separate shadows and ground layers of buried creatures.");
    cbShowAvatar.addActionListener(listeners);

    cbShowHelmet = new JCheckBox("Show helmet overlay", isShowHelmet);
    cbShowHelmet.addActionListener(listeners);

    cbShowShield = new JCheckBox("Show shield overlay", isShowShield);
    cbShowShield.setToolTipText("Includes shields and offhand weapons.");
    cbShowShield.addActionListener(listeners);

    cbShowWeapon = new JCheckBox("Show weapon overlay", isShowWeapon);
    cbShowWeapon.addActionListener(listeners);

    cbShowBorders = new JCheckBox("Show overlay borders", isShowBorders);
    cbShowBorders.setToolTipText("Draw bounding boxes around individual segments of the creature animation.");
    cbShowBorders.addActionListener(listeners);

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
    panel2.add(cbShowAvatar, c);

    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    panel2.add(cbOrnateSelectionCircle, c);
    c = ViewerUtil.setGBC(c, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 16, 0, 0), 0, 0);
    panel2.add(cbShowHelmet, c);

    c = ViewerUtil.setGBC(c, 0, 3, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    panel2.add(cbPersonalSpace, c);
    c = ViewerUtil.setGBC(c, 1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 16, 0, 0), 0, 0);
    panel2.add(cbShowWeapon, c);

    c = ViewerUtil.setGBC(c, 0, 4, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    panel2.add(cbTranslucent, c);
    c = ViewerUtil.setGBC(c, 1, 4, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 16, 0, 0), 0, 0);
    panel2.add(cbShowShield, c);

    c = ViewerUtil.setGBC(c, 0, 5, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    panel2.add(cbBlurEnabled, c);
    c = ViewerUtil.setGBC(c, 1, 5, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 16, 0, 0), 0, 0);
    panel2.add(cbShowBorders, c);

    c = ViewerUtil.setGBC(c, 0, 6, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    panel2.add(cbTintEnabled, c);
    c = ViewerUtil.setGBC(c, 1, 6, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 16, 0, 0), 0, 0);
    panel2.add(cbPaletteReplacementEnabled, c);


    // attributes table panel
    panelAttributes = new AttributesPanel(getBrowser());


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
    scroll.setBorder(panelMain.getBorder());

    setLayout(new BorderLayout());
    add(scroll, BorderLayout.CENTER);
  }

  /** Returns a color from user input. */
  private Color getCustomColor(Color defColor)
  {
    final JColorChooser cc = new JColorChooser((defColor != null) ? defColor : Color.WHITE);

    // We only need the RGB panel
    AbstractColorChooserPanel rgbPanel = null;
    for (final AbstractColorChooserPanel panel : cc.getChooserPanels()) {
      if (panel.getDisplayName().toUpperCase().contains("RGB")) {
        rgbPanel = panel;
      }
    }
    if (rgbPanel != null) {
      for (final AbstractColorChooserPanel panel : cc.getChooserPanels()) {
        if (panel != rgbPanel) {
          cc.removeChooserPanel(panel);
        }
      }
    }

    final Monuple<Color> retVal = Monuple.with(null);
    JDialog dlg = null;
    try {
      // Returns color value without alpha component
      dlg = JColorChooser.createDialog(getBrowser(), "Choose background color", true, cc,
                                       evt -> retVal.setValue0(new Color(cc.getColor().getRGB(), false)), null);
      dlg.setVisible(true);
    } finally {
      if (dlg != null) {
        dlg.dispose();
        dlg = null;
      }
    }
    return retVal.getValue0();
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
        getBrowser().getRenderPanel().centerOnSprite();
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
      else if (e.getSource() == cbTranslucent) {
        setTranslucencyEnabled(cbTranslucent.isSelected());
      }
      else if (e.getSource() == cbTintEnabled) {
        setTintEnabled(cbTintEnabled.isSelected());
      }
      else if (e.getSource() == cbBlurEnabled) {
        setBlurEnabled(cbBlurEnabled.isSelected());
      }
      else if (e.getSource() == cbPaletteReplacementEnabled) {
        setPaletteReplacementEnabled(cbPaletteReplacementEnabled.isSelected());
      }
      else if (e.getSource() == cbSelectionCircle) {
        setSelectionCircleEnabled(cbSelectionCircle.isSelected());
      }
      else if (e.getSource() == cbOrnateSelectionCircle) {
        setOrnateSelectionCircle(cbOrnateSelectionCircle.isSelected());
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
      return new ItemString<>(text, data);
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
