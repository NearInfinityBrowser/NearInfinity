// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ProgressMonitor;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.infinity.NearInfinity;
import org.infinity.datatype.Flag;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.SectionOffset;
import org.infinity.gui.ButtonPopupWindow;
import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.DataMenuItem;
import org.infinity.gui.RenderCanvas;
import org.infinity.gui.StructViewer;
import org.infinity.gui.ViewFrame;
import org.infinity.gui.ViewerUtil;
import org.infinity.gui.WindowBlocker;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.IconLayerItem;
import org.infinity.gui.layeritem.LayerItemEvent;
import org.infinity.gui.layeritem.LayerItemListener;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.RestSpawn;
import org.infinity.resource.are.Song;
import org.infinity.resource.are.viewer.ViewerConstants.LayerStackingType;
import org.infinity.resource.are.viewer.ViewerConstants.LayerType;
import org.infinity.resource.are.viewer.icon.ViewerIcons;
import org.infinity.resource.graphics.GraphicsResource;
import org.infinity.resource.graphics.ColorConvert;
import org.infinity.resource.key.BIFFResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.wed.Overlay;
import org.infinity.resource.wed.WedResource;
import org.infinity.util.io.FileManager;
import org.infinity.util.io.StreamUtils;

/**
 * The Area Viewer shows a selected map with its associated structures, such as actors, regions or
 * animations.
 */
public class AreaViewer extends ChildFrame
{
  private static final String LabelInfoX = "Position X:";
  private static final String LabelInfoY = "Position Y:";
  private static final String LabelEnableSchedule = "Enable time schedules";
  private static final String LabelDrawClosed = "Draw closed";
  private static final String LabelDrawOverlays = "Enable overlays";
  private static final String LabelAnimateOverlays = "Animate overlays";
  private static final String LabelDrawGrid = "Show grid";

  private final Listeners listeners;
  private final Map map;
  private final Point mapCoordinates = new Point();
  private final String windowTitle;
  private final JCheckBox[] cbLayers = new JCheckBox[LayerManager.getLayerTypeCount()];
  private final JCheckBox[] cbLayerRealActor = new JCheckBox[2];
  private final JCheckBox[] cbLayerRealAnimation = new JCheckBox[2];
  private final JCheckBox[] cbMiniMaps = new JCheckBox[3];
//  private final JToggleButton[] tbAddLayerItem = new JToggleButton[LayerManager.getLayerTypeCount()];

  private LayerManager layerManager;
  private TilesetRenderer rcCanvas;
  private JPanel pCanvas;
  private JScrollPane spCanvas;
  private Point vpMapCenter;    // contains map coordinate at the viewport center
  private JToolBar toolBar;
  private JToggleButton tbView, tbEdit;
  private JButton tbAre, tbWed, tbSongs, tbRest, tbSettings, tbRefresh, tbExportPNG;
  private JTree treeControls;
  private ButtonPopupWindow bpwDayTime;
  private DayTimePanel pDayTime;
  private JCheckBox cbDrawClosed, cbDrawOverlays, cbAnimateOverlays, cbDrawGrid, cbEnableSchedules;
  private JComboBox<String> cbZoomLevel;
  private JCheckBox cbLayerAmbientRange;
  private JLabel lPosX, lPosY;
  private JTextArea taInfo;
  private boolean bMapDragging;
  private Point mapDraggingPosStart, mapDraggingScrollStart, mapDraggingPos;
  private Timer timerOverlays;
  private JPopupMenu pmItems;
  private SwingWorker<Void, Void> workerInitGui, workerLoadMap, workerOverlays;
  private ProgressMonitor progress;
  private int pmCur, pmMax;
  private WindowBlocker blocker;
  private boolean initialized;


  /**
   * Checks whether the specified ARE resource can be displayed with the area viewer.
   * @param are The ARE resource to check
   * @return {@code true} if area is viewable, {@code false} otherwise.
   */
  public static boolean isValid(AreResource are)
  {
    if (are != null) {
      ResourceRef wedRef = (ResourceRef)are.getAttribute(AreResource.ARE_WED_RESOURCE);
      ResourceEntry wedEntry = ResourceFactory.getResourceEntry(wedRef.getResourceName());
      if (wedEntry != null) {
        try {
          WedResource wedFile = new WedResource(wedEntry);
          int ofs = ((SectionOffset)wedFile.getAttribute(WedResource.WED_OFFSET_OVERLAYS)).getValue();
          Overlay overlay = (Overlay)wedFile.getAttribute(ofs, false);
          ResourceRef tisRef = (ResourceRef)overlay.getAttribute(Overlay.WED_OVERLAY_TILESET);
          ResourceEntry tisEntry = ResourceFactory.getResourceEntry(tisRef.getResourceName());
          if (tisEntry != null)
            return true;
        } catch (Exception e) {
        }
      }
    }
    return false;
  }

  /** Returns the general day time (day/twilight/night). */
  private static int getDayTime()
  {
    return ViewerConstants.getDayTime(Settings.TimeOfDay);
  }


  /** Returns the currently selected day time in hours. */
  private static int getHour()
  {
    return Settings.TimeOfDay;
  }

  public AreaViewer(Component parent, AreResource are)
  {
    super("");
    windowTitle = String.format("Area Viewer: %s", are.getName());
    initProgressMonitor(parent, "Initializing " + are.getName(), "Loading ARE resource...", 3, 0, 0);
    listeners = new Listeners();
    map = new Map(this, are);
    // loading map in dedicated thread
    workerInitGui = new SwingWorker<Void, Void>() {
      @Override
      protected Void doInBackground() throws Exception {
        try {
          init();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return null;
      }
    };
    workerInitGui.addPropertyChangeListener(listeners);
    workerInitGui.execute();
  }

  @Override
  public void close()
  {
    Settings.storeSettings(false);

    if (!map.closeWed(ViewerConstants.AREA_DAY, true)) {
      return;
    }
    if (!map.closeWed(ViewerConstants.AREA_NIGHT, true)) {
      return;
    }
    map.clear();
    if (rcCanvas != null) {
      removeLayerItems();
      rcCanvas.dispose();
    }
    if (layerManager != null) {
      layerManager.close();
      layerManager = null;
    }
    SharedResourceCache.clearCache();
    dispose();
    System.gc();
    super.close();
  }

  /**
   * Returns the tileset renderer for this viewer instance.
   * @return The currently used TilesetRenderer instance.
   */
  public TilesetRenderer getRenderer()
  {
    return rcCanvas;
  }

  /** Returns the instance which handles all listeners of the area viewer. */
  Listeners getListeners()
  {
    return listeners;
  }


  /** initialize GUI and structures. */
  private void init()
  {
    initialized = false;
    advanceProgressMonitor("Initializing GUI...");

    GridBagConstraints c = new GridBagConstraints();
    JPanel p;

    // initialize misc. features
    pmItems = new JPopupMenu("Select item:");
    bMapDragging = false;
    mapDraggingPosStart = new Point();
    mapDraggingPos = new Point();
    mapDraggingScrollStart = new Point();

    // Creating main view area
    pCanvas = new JPanel(new GridBagLayout());
    rcCanvas = new TilesetRenderer(map.getOverlayTransparency());
    rcCanvas.addComponentListener(getListeners());
    rcCanvas.addMouseListener(getListeners());
    rcCanvas.addMouseMotionListener(getListeners());
    if (Settings.MouseWheelZoom) {
      rcCanvas.addMouseWheelListener(getListeners());
    }
    rcCanvas.addChangeListener(getListeners());
    rcCanvas.setHorizontalAlignment(RenderCanvas.CENTER);
    rcCanvas.setVerticalAlignment(RenderCanvas.CENTER);
    rcCanvas.setLocation(0, 0);
    rcCanvas.setLayout(null);
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                          GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    pCanvas.add(rcCanvas, c);
    spCanvas = new JScrollPane(pCanvas);
    spCanvas.addComponentListener(getListeners());
    spCanvas.getViewport().addChangeListener(getListeners());
    spCanvas.getVerticalScrollBar().setUnitIncrement(16);
    spCanvas.getHorizontalScrollBar().setUnitIncrement(16);
    JPanel pView = new JPanel(new BorderLayout());
    pView.add(spCanvas, BorderLayout.CENTER);

    // Creating right side bar
    JPanel pTree = new JPanel(new GridBagLayout());
    pTree.setBorder(BorderFactory.createTitledBorder("Area Viewer Controls: "));
    DefaultMutableTreeNode t, t2, t3;
    DefaultMutableTreeNode top = new DefaultMutableTreeNode("");

    // Adding Visual State elements
    // Note: the string is required for setting the correct size of the button
    bpwDayTime = new ButtonPopupWindow(String.format("  %s  ", DayTimePanel.getButtonText(21)),
                                       Icons.getIcon(Icons.ICON_ARROW_DOWN_15));
    Dimension d = bpwDayTime.getPreferredSize();
    bpwDayTime.setIconTextGap(8);
    pDayTime = new DayTimePanel(bpwDayTime, getHour());
    pDayTime.addChangeListener(getListeners());
    bpwDayTime.setContent(pDayTime);
    bpwDayTime.setPreferredSize(d);
    bpwDayTime.setMargin(new Insets(2, bpwDayTime.getMargin().left, 2, bpwDayTime.getMargin().right));

    cbEnableSchedules = new JCheckBox(LabelEnableSchedule);
    cbEnableSchedules.setToolTipText("Enable activity schedules on layer structures that support them (e.g. actors, ambient sounds or background animations.");
    cbEnableSchedules.addActionListener(getListeners());

    cbDrawClosed = new JCheckBox(LabelDrawClosed);
    cbDrawClosed.setToolTipText("Draw opened or closed states of doors");
    cbDrawClosed.addActionListener(getListeners());

    cbDrawGrid = new JCheckBox(LabelDrawGrid);
    cbDrawGrid.addActionListener(getListeners());

    cbDrawOverlays = new JCheckBox(LabelDrawOverlays);
    cbDrawOverlays.addActionListener(getListeners());

    cbAnimateOverlays = new JCheckBox(LabelAnimateOverlays);
    cbAnimateOverlays.addActionListener(getListeners());

    JLabel lZoomLevel = new JLabel("Zoom map:");
    cbZoomLevel = new JComboBox<>(Settings.LabelZoomFactor);
    cbZoomLevel.setSelectedIndex(Settings.getZoomLevelIndex(Settings.ZoomFactor));
    cbZoomLevel.addActionListener(getListeners());
    JPanel pZoom = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pZoom.add(lZoomLevel, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    pZoom.add(cbZoomLevel, c);

    JLabel l = new JLabel("Visual State");
    l.setFont(new Font(l.getFont().getFontName(), Font.BOLD, l.getFont().getSize()+1));
    t = new DefaultMutableTreeNode(l);
    top.add(t);
    t.add(new DefaultMutableTreeNode(bpwDayTime));
    t.add(new DefaultMutableTreeNode(cbEnableSchedules));
    t.add(new DefaultMutableTreeNode(cbDrawClosed));
    t.add(new DefaultMutableTreeNode(cbDrawGrid));
    t2 = new DefaultMutableTreeNode(cbDrawOverlays);
    t2.add(new DefaultMutableTreeNode(cbAnimateOverlays));
    t.add(t2);
    t.add(new DefaultMutableTreeNode(pZoom));

    // Adding Layer elements
    l = new JLabel("Layers");
    l.setFont(new Font(l.getFont().getFontName(), Font.BOLD, l.getFont().getSize()+1));
    t = new DefaultMutableTreeNode(l);
    top.add(t);
    for (int i = 0, ltCount = LayerManager.getLayerTypeCount(); i < ltCount; i++) {
      LayerType layer = LayerManager.getLayerType(i);
      cbLayers[i] = new JCheckBox(LayerManager.getLayerTypeLabel(layer));
      cbLayers[i].addActionListener(getListeners());
      t2 = new DefaultMutableTreeNode(cbLayers[i]);
      t.add(t2);
      if (i == LayerManager.getLayerTypeIndex(LayerType.ACTOR)) {
        // Initializing real creature animation checkboxes
        cbLayerRealActor[0] = new JCheckBox("Show actor animation");
        cbLayerRealActor[0].addActionListener(getListeners());
        t3 = new DefaultMutableTreeNode(cbLayerRealActor[0]);
        t2.add(t3);
        cbLayerRealActor[1] = new JCheckBox("Animate actor animation");
        cbLayerRealActor[1].addActionListener(getListeners());
        t3 = new DefaultMutableTreeNode(cbLayerRealActor[1]);
        t2.add(t3);
      } else if (i == LayerManager.getLayerTypeIndex(LayerType.AMBIENT)) {
        // Initializing ambient sound range checkbox
        cbLayerAmbientRange = new JCheckBox("Show local sound ranges");
        cbLayerAmbientRange.addActionListener(getListeners());
        t3 = new DefaultMutableTreeNode(cbLayerAmbientRange);
        t2.add(t3);
      } else if (i == LayerManager.getLayerTypeIndex(LayerType.ANIMATION)) {
        // Initializing real animation checkboxes
        cbLayerRealAnimation[0] = new JCheckBox("Show actual animations");
        cbLayerRealAnimation[0].addActionListener(getListeners());
        t3 = new DefaultMutableTreeNode(cbLayerRealAnimation[0]);
        t2.add(t3);
        cbLayerRealAnimation[1] = new JCheckBox("Animate actual animations");
        cbLayerRealAnimation[1].addActionListener(getListeners());
        t3 = new DefaultMutableTreeNode(cbLayerRealAnimation[1]);
        t2.add(t3);
      }
    }

    // Adding mini map entries
    cbMiniMaps[ViewerConstants.MAP_SEARCH] = new JCheckBox("Display search map");
    cbMiniMaps[ViewerConstants.MAP_SEARCH].addActionListener(getListeners());
    cbMiniMaps[ViewerConstants.MAP_LIGHT] = new JCheckBox("Display light map");
    cbMiniMaps[ViewerConstants.MAP_LIGHT].addActionListener(getListeners());
    cbMiniMaps[ViewerConstants.MAP_HEIGHT] = new JCheckBox("Display height map");
    cbMiniMaps[ViewerConstants.MAP_HEIGHT].addActionListener(getListeners());

    l = new JLabel("Mini maps");
    l.setFont(new Font(l.getFont().getFontName(), Font.BOLD, l.getFont().getSize()+1));
    t = new DefaultMutableTreeNode(l);
    top.add(t);
    t.add(new DefaultMutableTreeNode(cbMiniMaps[0]));
    t.add(new DefaultMutableTreeNode(cbMiniMaps[1]));
    t.add(new DefaultMutableTreeNode(cbMiniMaps[2]));

    treeControls = new JTree(new DefaultTreeModel(top));
    treeControls.addTreeExpansionListener(getListeners());
    treeControls.setBackground(getBackground());
    treeControls.setRootVisible(false);
    treeControls.setShowsRootHandles(true);
    treeControls.setRowHeight(bpwDayTime.getPreferredSize().height);
    treeControls.setEditable(true);
    ComponentTreeCellRenderer renderer = new ComponentTreeCellRenderer();
    treeControls.setCellRenderer(renderer);
    treeControls.setCellEditor(new ComponentTreeCellEditor(treeControls, renderer));
    ToolTipManager.sharedInstance().registerComponent(treeControls);
    for (int i = 0; i < treeControls.getRowCount(); i++) {
      treeControls.expandRow(i);
    }
    treeControls.setMinimumSize(treeControls.getPreferredSize());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.BOTH, new Insets(0, 4, 4, 4), 0, 0);
    pTree.add(treeControls, c);

    // Creating Info Box area
    JLabel lPosXLabel = new JLabel(LabelInfoX);
    JLabel lPosYLabel = new JLabel(LabelInfoY);
    lPosX = new JLabel("0");
    lPosY = new JLabel("0");
    taInfo = new JTextArea(4, 15);
    taInfo.setEditable(false);
    taInfo.setFont(lPosX.getFont());
    taInfo.setBackground(lPosX.getBackground());
    taInfo.setSelectionColor(lPosX.getBackground());
    taInfo.setSelectedTextColor(lPosX.getBackground());
    taInfo.setWrapStyleWord(true);
    taInfo.setLineWrap(true);

    p = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    p.add(lPosXLabel, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    p.add(lPosX, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 0, 0, 0), 0, 0);
    p.add(lPosYLabel, c);
    c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(4, 8, 0, 0), 0, 0);
    p.add(lPosY, c);
    c = ViewerUtil.setGBC(c, 0, 2, 2, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.BOTH, new Insets(4, 0, 0, 0), 0, 0);
    p.add(taInfo, c);

    JPanel pInfoBox = new JPanel(new GridBagLayout());
    pInfoBox.setBorder(BorderFactory.createTitledBorder("Information: "));
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.BOTH, new Insets(0, 4, 0, 4), 0, 0);
    pInfoBox.add(p, c);

    // Assembling right side bar
    JPanel pSideBar = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 4), 0, 0);
    pSideBar.add(pTree, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 4), 0, 0);
    pSideBar.add(pInfoBox, c);
    p = new JPanel();
    p.setPreferredSize(new Dimension(pTree.getPreferredSize().width, p.getMinimumSize().height));
    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 0, 0, 0), 0, 0);
    pSideBar.add(p, c);

    // Creating toolbar
    Dimension dimSeparator = new Dimension(24, 40);
    toolBar = new JToolBar("Area Viewer Controls", SwingConstants.HORIZONTAL);
    toolBar.setRollover(true);
    toolBar.setFloatable(false);
    tbView = new JToggleButton(Icons.getIcon(ViewerIcons.class, ViewerIcons.ICON_BTN_VIEW_MODE), true);
    tbView.setToolTipText("Enter view mode");
    tbView.addActionListener(getListeners());
    tbView.setEnabled(false);
//    toolBar.add(tbView);
    tbEdit = new JToggleButton(Icons.getIcon(ViewerIcons.class, ViewerIcons.ICON_BTN_EDIT_MODE), false);
    tbEdit.setToolTipText("Enter edit mode");
    tbEdit.addActionListener(getListeners());
    tbEdit.setEnabled(false);
//    toolBar.add(tbEdit);

//    toolBar.addSeparator(dimSeparator);

    JToggleButton tb;
    tb = new JToggleButton(Icons.getIcon(ViewerIcons.class, ViewerIcons.ICON_BTN_ADD_ACTOR), false);
    tb.setToolTipText("Add a new actor to the map");
    tb.addActionListener(getListeners());
    tb.setEnabled(false);
//    toolBar.add(tb);
//    tbAddLayerItem[LayerManager.getLayerTypeIndex(LayerType.ACTOR)] = tb;
    tb = new JToggleButton(Icons.getIcon(ViewerIcons.class, ViewerIcons.ICON_BTN_ADD_REGION), false);
    tb.setToolTipText("Add a new region to the map");
    tb.addActionListener(getListeners());
    tb.setEnabled(false);
//    toolBar.add(tb);
//    tbAddLayerItem[LayerManager.getLayerTypeIndex(LayerType.REGION)] = tb;
    tb = new JToggleButton(Icons.getIcon(ViewerIcons.class, ViewerIcons.ICON_BTN_ADD_ENTRANCE), false);
    tb.setToolTipText("Add a new entrance to the map");
    tb.addActionListener(getListeners());
    tb.setEnabled(false);
//    toolBar.add(tb);
//    tbAddLayerItem[LayerManager.getLayerTypeIndex(LayerType.ENTRANCE)] = tb;
    tb = new JToggleButton(Icons.getIcon(ViewerIcons.class, ViewerIcons.ICON_BTN_ADD_CONTAINER), false);
    tb.setToolTipText("Add a new container to the map");
    tb.addActionListener(getListeners());
    tb.setEnabled(false);
//    toolBar.add(tb);
//    tbAddLayerItem[LayerManager.getLayerTypeIndex(LayerType.CONTAINER)] = tb;
    tb = new JToggleButton(Icons.getIcon(ViewerIcons.class, ViewerIcons.ICON_BTN_ADD_AMBIENT), false);
    tb.setToolTipText("Add a new global ambient sound to the map");
    tb.addActionListener(getListeners());
    tb.setEnabled(false);
//    toolBar.add(tb);
//    tbAddLayerItem[LayerManager.getLayerTypeIndex(LayerType.AMBIENT)] = tb;
    tb = new JToggleButton(Icons.getIcon(ViewerIcons.class, ViewerIcons.ICON_BTN_ADD_DOOR), false);
    tb.setToolTipText("Add a new door to the map");
    tb.addActionListener(getListeners());
    tb.setEnabled(false);
//    toolBar.add(tb);
//    tbAddLayerItem[LayerManager.getLayerTypeIndex(LayerType.DOOR)] = tb;
    tb = new JToggleButton(Icons.getIcon(ViewerIcons.class, ViewerIcons.ICON_BTN_ADD_ANIM), false);
    tb.setToolTipText("Add a new background animation to the map");
    tb.addActionListener(getListeners());
    tb.setEnabled(false);
//    toolBar.add(tb);
//    tbAddLayerItem[LayerManager.getLayerTypeIndex(LayerType.ANIMATION)] = tb;
    tb = new JToggleButton(Icons.getIcon(ViewerIcons.class, ViewerIcons.ICON_BTN_ADD_AUTOMAP), false);
    tb.setToolTipText("Add a new automap note to the map");
    tb.addActionListener(getListeners());
    tb.setEnabled(false);
//    toolBar.add(tb);
//    tbAddLayerItem[LayerManager.getLayerTypeIndex(LayerType.AUTOMAP)] = tb;
    tb = new JToggleButton(Icons.getIcon(ViewerIcons.class, ViewerIcons.ICON_BTN_ADD_SPAWN_POINT), false);
    tb.setToolTipText("Add a new spawn point to the map");
    tb.addActionListener(getListeners());
    tb.setEnabled(false);
//    toolBar.add(tb);
//    tbAddLayerItem[LayerManager.getLayerTypeIndex(LayerType.SPAWN_POINT)] = tb;
    tb = new JToggleButton(Icons.getIcon(ViewerIcons.class, ViewerIcons.ICON_BTN_ADD_PRO_TRAP), false);
    tb.setToolTipText("Add a new projectile trap to the map");
    tb.addActionListener(getListeners());
    tb.setEnabled(false);
//    toolBar.add(tb);
//    tbAddLayerItem[LayerManager.getLayerTypeIndex(LayerType.PRO_TRAP)] = tb;
    tb = new JToggleButton(Icons.getIcon(ViewerIcons.class, ViewerIcons.ICON_BTN_ADD_DOOR_POLY), false);
    tb.setToolTipText("Add a new door polygon to the map");
    tb.addActionListener(getListeners());
    tb.setEnabled(false);
//    toolBar.add(tb);
//    tbAddLayerItem[LayerManager.getLayerTypeIndex(LayerType.DOOR_POLY)] = tb;
    tb = new JToggleButton(Icons.getIcon(ViewerIcons.class, ViewerIcons.ICON_BTN_ADD_WALL_POLY), false);
    tb.setToolTipText("Add a new wall polygon to the map");
    tb.addActionListener(getListeners());
    tb.setEnabled(false);
//    toolBar.add(tb);
//    tbAddLayerItem[LayerManager.getLayerTypeIndex(LayerType.WALL_POLY)] = tb;

//    toolBar.addSeparator(dimSeparator);

    tbAre = new JButton(Icons.getIcon(ViewerIcons.class, ViewerIcons.ICON_BTN_MAP_ARE));
    tbAre.setToolTipText(String.format("Edit ARE structure (%s)", map.getAre().getName()));
    tbAre.addActionListener(getListeners());
    toolBar.add(tbAre);
    tbWed = new JButton(Icons.getIcon(ViewerIcons.class, ViewerIcons.ICON_BTN_MAP_WED));
    tbWed.addActionListener(getListeners());
    toolBar.add(tbWed);
    tbSongs = new JButton(Icons.getIcon(ViewerIcons.class, ViewerIcons.ICON_BTN_SONGS));
    tbSongs.setToolTipText("Edit song entries");
    tbSongs.addActionListener(getListeners());
    toolBar.add(tbSongs);
    tbRest = new JButton(Icons.getIcon(ViewerIcons.class, ViewerIcons.ICON_BTN_REST));
    tbRest.setToolTipText("Edit rest encounters");
    tbRest.addActionListener(getListeners());
    toolBar.add(tbRest);

    toolBar.addSeparator(dimSeparator);

    tbSettings = new JButton(Icons.getIcon(ViewerIcons.class, ViewerIcons.ICON_BTN_SETTINGS));
    tbSettings.setToolTipText("Area viewer settings");
    tbSettings.addActionListener(getListeners());
    toolBar.add(tbSettings);
    tbRefresh = new JButton(Icons.getIcon(ViewerIcons.class, ViewerIcons.ICON_BTN_REFRESH));
    tbRefresh.setToolTipText("Update map");
    tbRefresh.addActionListener(getListeners());
    toolBar.add(tbRefresh);

    toolBar.addSeparator(dimSeparator);

    tbExportPNG = new JButton(Icons.getIcon(ViewerIcons.class, ViewerIcons.ICON_BTN_EXPORT));
    tbExportPNG.setToolTipText("Export current map state as PNG");
    tbExportPNG.addActionListener(getListeners());
    toolBar.add(tbExportPNG);

    pView.add(toolBar, BorderLayout.NORTH);

    updateToolBarButtons();

    // Putting all together
    JPanel pMain = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    pMain.add(pView, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0);
    pMain.add(pSideBar, c);

    // setting frame rate for overlay animations to 5 fps (in-game frame rate: 7.5 fps)
    timerOverlays = new Timer(1000/5, getListeners());

    advanceProgressMonitor("Initializing map...");
    Container pane = getContentPane();
    pane.setLayout(new BorderLayout());
    pane.add(pMain, BorderLayout.CENTER);
    pack();

    // setting window size and state
    setSize(NearInfinity.getInstance().getSize());
    Center.center(this, NearInfinity.getInstance().getBounds());
    setExtendedState(NearInfinity.getInstance().getExtendedState());

    try {
      initGuiSettings();
    } catch (OutOfMemoryError e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(this, "Not enough memory to load area!", "Error", JOptionPane.ERROR_MESSAGE);
      throw e;
    }
    rcCanvas.requestFocusInWindow();    // put focus on a safe component
    advanceProgressMonitor("Ready!");

    // adding context menu key support for calling the popup menu on map items
    rcCanvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0), rcCanvas);
    rcCanvas.getActionMap().put(rcCanvas, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent event)
      {
        Point pt = rcCanvas.getMousePosition();
        if (pt != null) {
          Point pts = rcCanvas.getLocationOnScreen();
          pts.x += pt.x;
          pts.y += pt.y;
          showItemPopup(new MouseEvent(rcCanvas, MouseEvent.MOUSE_PRESSED, 0, 0, pt.x, pt.y, pts.x, pts.y, 1, true, MouseEvent.BUTTON2));
        }
      }
    });

    updateWindowTitle();
    setVisible(true);
    initialized = true;
  }

  /** Returns whether area viewer is still being initialized. */
  private boolean isInitialized()
  {
    return initialized;
  }


  /** Sets the state of all GUI components and their associated actions. */
  private void initGuiSettings()
  {
    Settings.loadSettings(false);

    // expanding main sections in sidebar based on current settings
    DefaultTreeModel model = (DefaultTreeModel)treeControls.getModel();
    DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();
    for (int i = 0, cCount = root.getChildCount(); i < cCount; i++) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)root.getChildAt(i);
      int bit = 1 << i;
      if ((Settings.SidebarControls & bit) != 0) {
        treeControls.expandPath(new TreePath(node.getPath()));
      } else {
        treeControls.collapsePath(new TreePath(node.getPath()));
      }
    }

    // initializing minimap state (needs to be set before the first call to setHour)
    cbMiniMaps[ViewerConstants.MAP_SEARCH].setSelected(Settings.MiniMap == ViewerConstants.MAP_SEARCH);
    cbMiniMaps[ViewerConstants.MAP_LIGHT].setSelected(Settings.MiniMap == ViewerConstants.MAP_LIGHT);
    cbMiniMaps[ViewerConstants.MAP_HEIGHT].setSelected(Settings.MiniMap == ViewerConstants.MAP_HEIGHT);

    // initializing visual state of the map
    setHour(Settings.TimeOfDay);

    // initializing time schedules for layer items
    cbEnableSchedules.setSelected(Settings.EnableSchedules);

    // initializing closed state of doors
    cbDrawClosed.setSelected(Settings.DrawClosed);
    cbDrawClosed.setEnabled(rcCanvas.hasDoors());
    if (rcCanvas.hasDoors()) {
      setDoorState(Settings.DrawClosed);
    }

    // initializing grid
    cbDrawGrid.setSelected(Settings.DrawGrid);
    setTileGridEnabled(Settings.DrawGrid);

    // initializing overlays
    cbDrawOverlays.setSelected(Settings.DrawOverlays);
    cbDrawOverlays.setEnabled(rcCanvas.hasOverlays());
    cbAnimateOverlays.setEnabled(rcCanvas.hasOverlays());
    if (rcCanvas.hasOverlays()) {
      setOverlaysEnabled(Settings.DrawOverlays);
      setOverlaysAnimated(cbAnimateOverlays.isSelected());
    }

    // initializing zoom level
    cbZoomLevel.setSelectedIndex(Settings.getZoomLevelIndex(Settings.ZoomFactor));

    // initializing layers
    layerManager = new LayerManager(map.getAre(), getCurrentWed(), this);
    layerManager.setDoorState(Settings.DrawClosed ? ViewerConstants.DOOR_CLOSED : ViewerConstants.DOOR_OPEN);
    layerManager.setScheduleEnabled(Settings.EnableSchedules);
    layerManager.setSchedule(LayerManager.toSchedule(getHour()));
    addLayerItems();
    updateScheduledItems();
    for (int i = 0, ltCount = LayerManager.getLayerTypeCount(); i < ltCount; i++) {
      LayerType layer = LayerManager.getLayerType(i);
      int bit = 1 << i;
      boolean isChecked = (Settings.LayerFlags & bit) != 0;
      int count = layerManager.getLayerObjectCount(layer);
      if (count > 0) {
        cbLayers[i].setToolTipText(layerManager.getLayerAvailability(layer));
      }
      cbLayers[i].setEnabled(count > 0);
      cbLayers[i].setSelected(isChecked);
      updateLayerItems(Settings.layerToStacking(layer));
      showLayer(LayerManager.getLayerType(i), cbLayers[i].isSelected());
    }

    // initializing actor sprites display
    // Disabling animated frames for performance and safety reasons
    if (Settings.ShowActorSprites == ViewerConstants.ANIM_SHOW_ANIMATED) {
      Settings.ShowActorSprites = ViewerConstants.ANIM_SHOW_STILL;
    }
    ((LayerActor)layerManager.getLayer(LayerType.ACTOR)).setRealActorFrameState(Settings.ShowActorFrame);
    cbLayerRealActor[0].setSelected(Settings.ShowActorSprites == ViewerConstants.ANIM_SHOW_STILL);
    cbLayerRealActor[1].setSelected(false);
    updateRealActors();
    updateRealActorsLighting(getVisualState());

    // Setting up ambient sound ranges
    LayerAmbient layerAmbient = (LayerAmbient)layerManager.getLayer(ViewerConstants.LayerType.AMBIENT);
    if (layerAmbient.getLayerObjectCount(ViewerConstants.AMBIENT_TYPE_LOCAL) > 0) {
      cbLayerAmbientRange.setToolTipText(layerAmbient.getAvailability(ViewerConstants.AMBIENT_TYPE_LOCAL));
    }
    cbLayerAmbientRange.setSelected(Settings.ShowAmbientRanges);
    updateAmbientRange();

    // initializing background animation display
    // Disabling animated frames for performance and safety reasons
    if (Settings.ShowRealAnimations == ViewerConstants.ANIM_SHOW_ANIMATED) {
      Settings.ShowRealAnimations = ViewerConstants.ANIM_SHOW_STILL;
    }
    ((LayerAnimation)layerManager.getLayer(LayerType.ANIMATION)).setRealAnimationFrameState(Settings.ShowAnimationFrame);
    cbLayerRealAnimation[0].setSelected(Settings.ShowRealAnimations == ViewerConstants.ANIM_SHOW_STILL);
    cbLayerRealAnimation[1].setSelected(false);
    updateRealAnimation();
    updateRealAnimationsLighting(getVisualState());

    updateWindowTitle();
    applySettings();
  }

  /** Updates the window title. */
  private void updateWindowTitle()
  {
    int zoom = (int)Math.round(getZoomFactor()*100.0);

    String dayNight;
    switch (getVisualState()) {
      case ViewerConstants.LIGHTING_TWILIGHT:
        dayNight = "twilight";
        break;
      case ViewerConstants.LIGHTING_NIGHT:
        dayNight = "night";
        break;
      default:
        dayNight = "day";
    }

    String scheduleState = Settings.EnableSchedules ? "enabled" : "disabled";

    String doorState = isDoorStateClosed() ? "closed" : "open";

    String overlayState;
    if (isOverlaysEnabled() && !isOverlaysAnimated()) {
      overlayState = "enabled";
    } else if (isOverlaysEnabled() && isOverlaysAnimated()) {
      overlayState = "animated";
    } else {
      overlayState = "disabled";
    }

    String gridState = isTileGridEnabled() ? "enabled" : "disabled";

    setTitle(String.format("%s  (Time: %02d:00 (%s), Schedules: %s, Doors: %s, Overlays: %s, Grid: %s, Zoom: %d%%)",
                           windowTitle, getHour(), dayNight, scheduleState, doorState, overlayState, gridState, zoom));
  }

  /** Sets day time to a specific hour (0..23). */
  private void setHour(int hour)
  {
    while (hour < 0) { hour += 24; }
    hour %= 24;
    Settings.TimeOfDay = hour;
    setVisualState(getHour());
    if (layerManager != null) {
      layerManager.setSchedule(LayerManager.toSchedule(getHour()));
    }
    if (pDayTime != null) {
      pDayTime.setHour(Settings.TimeOfDay);
    }
    updateScheduledItems();
  }

  /** Returns the currently selected WED resource (day/night). */
  private WedResource getCurrentWed()
  {
    return map.getWed(getCurrentWedIndex());
  }

  /** Returns the currently selected WED resource (day/night). */
  private int getCurrentWedIndex()
  {
    return getDayTime() == ViewerConstants.LIGHTING_NIGHT
        ? ViewerConstants.AREA_NIGHT
        : ViewerConstants.AREA_DAY;
  }


  /** Returns the currently selected visual state (day/twilight/night) depending on whether the map supports day/night cycles. */
  public int getVisualState()
  {
    if (map.hasDayNight()) {
      return getDayTime();
    } else {
      return ViewerConstants.LIGHTING_DAY;
    }
  }

  /** Set the lighting condition of the current map (day/twilight/night) and real background animations. */
  private synchronized void setVisualState(int hour)
  {
    hour %= 24;
    while (hour < 0) { hour += 24; }
    int index = ViewerConstants.getDayTime(hour);
    if (!map.hasDayNight()) {
      index = ViewerConstants.LIGHTING_DAY;
    }
    switch (index) {
      case ViewerConstants.LIGHTING_DAY:
        if (!isProgressMonitorActive() && map.getWed(ViewerConstants.AREA_DAY) != rcCanvas.getWed()) {
          initProgressMonitor(this, "Loading tileset...", null, 1, 0, 0);
        }
        if (!rcCanvas.isMapLoaded() || rcCanvas.getWed() != map.getWed(ViewerConstants.AREA_DAY)) {
          rcCanvas.loadMap(map.getOverlayTransparency(), map.getWed(ViewerConstants.AREA_DAY));
          reloadWedLayers(true);
        }
        rcCanvas.setLighting(index);
        break;
      case ViewerConstants.LIGHTING_TWILIGHT:
        if (!isProgressMonitorActive() && map.getWed(ViewerConstants.AREA_DAY) != rcCanvas.getWed()) {
          initProgressMonitor(this, "Loading tileset...", null, 1, 0, 0);
        }
        if (!rcCanvas.isMapLoaded() || rcCanvas.getWed() != map.getWed(ViewerConstants.AREA_DAY)) {
          rcCanvas.loadMap(map.getOverlayTransparency(), map.getWed(ViewerConstants.AREA_DAY));
          reloadWedLayers(true);
        }
        rcCanvas.setLighting(index);
        break;
      case ViewerConstants.LIGHTING_NIGHT:
        if (!isProgressMonitorActive() && map.getWed(ViewerConstants.AREA_NIGHT) != rcCanvas.getWed()) {
          initProgressMonitor(this, "Loading tileset...", null, 1, 0, 0);
        }
        if (!rcCanvas.isMapLoaded() || map.hasExtendedNight()) {
          if (rcCanvas.getWed() != map.getWed(ViewerConstants.AREA_NIGHT)) {
            rcCanvas.loadMap(map.getOverlayTransparency(), map.getWed(ViewerConstants.AREA_NIGHT));
            reloadWedLayers(true);
          }
        }
        if (!map.hasExtendedNight()) {
          rcCanvas.setLighting(index);
        }
        break;
    }
    // updating current visual state
    if (hour != getHour()) {
      Settings.TimeOfDay = hour;
    }

    updateMiniMap();
    updateToolBarButtons();
    updateRealActorsLighting(getVisualState());
    updateRealAnimationsLighting(getVisualState());
    updateScheduledItems();
    updateWindowTitle();
  }


  /** Returns whether map dragging is enabled; updates current and previous mouse positions. */
  private boolean isMapDragging(Point mousePos)
  {
    if (bMapDragging && mousePos != null && !mapDraggingPos.equals(mousePos)) {
      mapDraggingPos.x = mousePos.x;
      mapDraggingPos.y = mousePos.y;
    }
    return bMapDragging;
  }

  /** Enables/disables map dragging mode (set mouse cursor, global state and current mouse position). */
  private void setMapDraggingEnabled(boolean enable, Point mousePos)
  {
    if (bMapDragging != enable) {
      bMapDragging = enable;
      setCursor(Cursor.getPredefinedCursor(bMapDragging ? Cursor.MOVE_CURSOR : Cursor.DEFAULT_CURSOR));
      if (bMapDragging && mousePos != null) {
        mapDraggingPosStart.x = mapDraggingPos.x = mousePos.x;
        mapDraggingPosStart.y = mapDraggingPos.y = mousePos.y;
        mapDraggingScrollStart.x = spCanvas.getHorizontalScrollBar().getModel().getValue();
        mapDraggingScrollStart.y = spCanvas.getVerticalScrollBar().getModel().getValue();
      }
    }
  }

  /** Returns the current or previous mouse position. */
  private Point getMapDraggingDistance()
  {
    Point pDelta = new Point();
    if (bMapDragging) {
      pDelta.x = mapDraggingPosStart.x - mapDraggingPos.x;
      pDelta.y = mapDraggingPosStart.y - mapDraggingPos.y;
    }
    return pDelta;
  }

  /** Updates the map portion displayed in the viewport. */
  private void moveMapViewport()
  {
    if (!mapDraggingPosStart.equals(mapDraggingPos)) {
      Point distance = getMapDraggingDistance();
      JViewport vp = spCanvas.getViewport();
      Point curPos = vp.getViewPosition();
      Dimension curDim = vp.getExtentSize();
      Dimension maxDim = new Dimension(spCanvas.getHorizontalScrollBar().getMaximum(),
                                       spCanvas.getVerticalScrollBar().getMaximum());
      if (curDim.width < maxDim.width) {
        curPos.x = mapDraggingScrollStart.x + distance.x;
        if (curPos.x < 0) curPos.x = 0;
        if (curPos.x + curDim.width > maxDim.width) curPos.x = maxDim.width - curDim.width;
      }
      if (curDim.height < maxDim.height) {
        curPos.y = mapDraggingScrollStart.y + distance.y;
        if (curPos.y < 0) curPos.y = 0;
        if (curPos.y + curDim.height > maxDim.height) curPos.y = maxDim.height - curDim.height;
      }
      vp.setViewPosition(curPos);
    }
  }


  /** Returns whether closed door state is active. */
  private boolean isDoorStateClosed()
  {
    return Settings.DrawClosed;
  }

  /** Draw opened/closed state of doors (affects map tiles, door layer and door poly layer). */
  private void setDoorState(boolean closed)
  {
    Settings.DrawClosed = closed;
    setDoorStateMap(closed);
    setDoorStateLayers(closed);
    updateWindowTitle();
  }

  /** Called by setDoorState(): sets door state map tiles. */
  private void setDoorStateMap(boolean closed)
  {
    if (rcCanvas != null) {
      rcCanvas.setDoorsClosed(Settings.DrawClosed);
    }
  }

  /** Called by setDoorState(): sets door state in door layer and door poly layer. */
  private void setDoorStateLayers(boolean closed)
  {
    if (layerManager != null) {
      layerManager.setDoorState(Settings.DrawClosed ? ViewerConstants.DOOR_CLOSED : ViewerConstants.DOOR_OPEN);
    }
  }


  /** Returns whether tile grid on map has been enabled. */
  private boolean isTileGridEnabled()
  {
    return Settings.DrawGrid;
  }

  /** Enable/disable tile grid on map. */
  private void setTileGridEnabled(boolean enable)
  {
    Settings.DrawGrid = enable;
    if (rcCanvas != null) {
      rcCanvas.setGridEnabled(Settings.DrawGrid);
    }
    updateWindowTitle();
  }


  /** Returns whether overlays are enabled (considers both internal overlay flag and whether the map contains overlays). */
  private boolean isOverlaysEnabled()
  {
    return Settings.DrawOverlays;
  }

  /** Enable/disable overlays. */
  private void setOverlaysEnabled(boolean enable)
  {
    Settings.DrawOverlays = enable;
    if (rcCanvas != null) {
      rcCanvas.setOverlaysEnabled(Settings.DrawOverlays);
    }
    updateWindowTitle();
  }


  /** Returns whether overlays are animated. */
  private boolean isOverlaysAnimated()
  {
    if (timerOverlays != null) {
      return (isOverlaysEnabled() && timerOverlays.isRunning());
    } else {
      return false;
    }
  }

  /** Activate/deactivate overlay animations. */
  private void setOverlaysAnimated(boolean animate)
  {
    if (timerOverlays != null) {
      if (animate && !timerOverlays.isRunning()) {
        timerOverlays.start();
      } else if (!animate && timerOverlays.isRunning()) {
        timerOverlays.stop();
      }
      updateWindowTitle();
    }
  }

  /** Advances animated overlays by one frame. */
  private synchronized void advanceOverlayAnimation()
  {
    if (rcCanvas != null) {
      rcCanvas.advanceTileFrame();
    }
  }


  /** Returns whether layer items should be included when exporting map as graphics. */
  private boolean isExportLayersEnabled()
  {
    return Settings.ExportLayers;
  }


  /** Returns the currently used zoom factor of the canvas map. */
  private double getZoomFactor()
  {
    if (rcCanvas != null) {
      return rcCanvas.getZoomFactor();
    } else {
      return Settings.ZoomFactor;
    }
  }

  /** Sets a new zoom level to the map and associated structures. */
  private void setZoomFactor(double zoomFactor, double fallbackZoomFactor)
  {
    updateViewpointCenter();
    if (zoomFactor == Settings.ZoomFactorAuto) {
      // removing scrollbars (not needed in this mode)
      boolean needValidate = false;
      if (spCanvas.getHorizontalScrollBarPolicy() != ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER) {
        spCanvas.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        needValidate = true;
      }
      if (spCanvas.getVerticalScrollBarPolicy() != ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER) {
        spCanvas.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        needValidate = true;
      }
      if (needValidate) {
        spCanvas.validate();    // required for determining the correct viewport size
      }
      // determining zoom factor by preserving correct aspect ratio
      Dimension viewDim = new Dimension(spCanvas.getViewport().getExtentSize());
      Dimension mapDim = new Dimension(rcCanvas.getMapWidth(false), rcCanvas.getMapHeight(false));
      double zoomX = (double)viewDim.width / (double)mapDim.width;
      double zoomY = (double)viewDim.height / (double)mapDim.height;
      zoomFactor = zoomX;
      if ((int)(zoomX*mapDim.height) > viewDim.height) {
        zoomFactor = zoomY;
      }
      if (rcCanvas != null) {
        rcCanvas.setZoomFactor(zoomFactor);
      }
      zoomFactor = 0.0;
    } else {
      if (zoomFactor < 0.0) {
        zoomFactor = isInitialized() ? getCustomZoomFactor(fallbackZoomFactor) : fallbackZoomFactor;
        if (zoomFactor < 0.0) {
          return;
        }
      }
      // (re-)activating scrollbars
      if (spCanvas.getHorizontalScrollBarPolicy() != ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED) {
        spCanvas.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      }
      if (spCanvas.getVerticalScrollBarPolicy() != ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED) {
        spCanvas.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
      }
      if (rcCanvas != null) {
        rcCanvas.setZoomFactor(zoomFactor);
      }
    }
    setViewpointCenter();
    Settings.ZoomFactor = zoomFactor;
    updateWindowTitle();
  }

  /** Handles manual input of zoom factor (in percent). */
  private double getCustomZoomFactor(double defaultZoom)
  {
    String defInput = Integer.toString((int)Math.round(defaultZoom * 100.0));
    Object ret = JOptionPane.showInputDialog(this, "Enter zoom factor (in percent):", "Area Map Zoom Factor",
                                             JOptionPane.QUESTION_MESSAGE, null, null, defInput);
    if (ret != null) {
      try {
        int idx = ret.toString().indexOf('%');
        if (idx >= 0) {
          ret = ret.toString().substring(0, idx);
        }
        ret = ret.toString().trim();
        int value = (int)Math.round(Double.parseDouble(ret.toString()));
        double f = (double)value / 100.0;
        if (f > 0.0 && f <= Settings.ZoomFactorMax) {
          defaultZoom = f;
        } else {
          int max = (int)(Settings.ZoomFactorMax * 100.0);
          JOptionPane.showMessageDialog(this, "Number is outside of valid range (1-" + max + ").", "Error",
                                        JOptionPane.ERROR_MESSAGE);
        }
      } catch (NumberFormatException nfe) {
        JOptionPane.showMessageDialog(this, "Invalid number.", "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
    return defaultZoom;
  }

  /** Returns whether auto-fit has been selected. */
  private boolean isAutoZoom()
  {
    return (Settings.ZoomFactor == Settings.ZoomFactorAuto);
  }


  /** Updates the map coordinate at the center of the current viewport. */
  private void updateViewpointCenter()
  {
    if (vpMapCenter == null) {
      vpMapCenter = new Point();
    }
    int mapWidth = rcCanvas.getMapWidth(true);
    int mapHeight = rcCanvas.getMapHeight(true);
    Rectangle view = spCanvas.getViewport().getViewRect();
    vpMapCenter.x = (view.width > mapWidth) ? mapWidth / 2 : view.x + (view.width / 2);
    vpMapCenter.y = (view.height > mapHeight) ? mapHeight / 2 : view.y + (view.height / 2);
    vpMapCenter.x = (int)(vpMapCenter.x / getZoomFactor());
    vpMapCenter.y = (int)(vpMapCenter.y / getZoomFactor());
  }

  /** Attempts to re-center the last known center coordinate in the current viewport. */
  private void setViewpointCenter()
  {
    if (vpMapCenter != null) {
      int mapWidth = rcCanvas.getMapWidth(true);
      int mapHeight = rcCanvas.getMapHeight(true);
      final int centerX = (int)(vpMapCenter.x * getZoomFactor());
      final int centerY = (int)(vpMapCenter.y * getZoomFactor());
      JViewport vp = spCanvas.getViewport();
      Rectangle view = vp.getViewRect();
      Point newView = new Point(view.getLocation());
      if (view.width < mapWidth) {
        newView.x = centerX - (view.width / 2);
        newView.x = Math.max(newView.x, 0);
        newView.x = Math.min(newView.x, mapWidth - view.width);
      }
      if (view.height < mapHeight) {
        newView.y = centerY - (view.height / 2);
        newView.y = Math.max(newView.y, 0);
        newView.y = Math.min(newView.y, mapHeight - view.height);
      }
      if (!view.getLocation().equals(newView)) {
        vp.setViewPosition(newView);
      }
    }
  }

  /** Converts canvas coordinates into actual map coordinates. */
  private Point canvasToMapCoordinates(Point coords)
  {
    if (coords != null) {
      coords.x = (int)(coords.x / getZoomFactor());
      coords.y = (int)(coords.y / getZoomFactor());
    }
    return coords;
  }

  /** Updates the map coordinates pointed to by the current cursor position. */
  private void showMapCoordinates(Point coords)
  {
    if (coords != null) {
      // Converting canvas coordinates -> map coordinates
      coords = canvasToMapCoordinates(coords);
      if (coords.x != mapCoordinates.x) {
        mapCoordinates.x = coords.x;
        lPosX.setText(Integer.toString(mapCoordinates.x));
      }
      if (coords.y != mapCoordinates.y) {
        mapCoordinates.y = coords.y;
        lPosY.setText(Integer.toString(mapCoordinates.y));
      }
    }
  }

  /** Shows a description in the info box. */
  private void setInfoText(String text)
  {
    if (taInfo != null) {
      if (text != null) {
        taInfo.setText(text);
      } else {
        taInfo.setText("");
      }
    }
  }


  /** Creates and displays a popup menu containing the items located at the specified location. */
  private boolean updateItemPopup(Point canvasCoords)
  {
    final int MaxLen = 32;    // max. length of a menuitem text

    if (layerManager != null) {
      // preparing menu items
      final List<JMenuItem> menuItems = new ArrayList<>();
      Point itemLocation = new Point();
      pmItems.removeAll();

      // for each active layer...
      for (final LayerStackingType stacking : Settings.ListLayerOrder) {
        final LayerType layer = Settings.stackingToLayer(stacking);
        if (!isLayerEnabled(layer)) { continue; }

        List<? extends LayerObject> itemList = layerManager.getLayerObjects(layer);
        if (itemList == null || itemList.isEmpty()) { continue; }

        // for each layer object...
        for (final LayerObject obj : itemList) {
          final AbstractLayerItem[] items = obj.getLayerItems();
          // for each layer item...
          for (int i = 0; i < items.length; i++) {
            // special case: Ambient/Ambient range (avoiding duplicates)
            if (stacking == LayerStackingType.AMBIENT &&
                cbLayerAmbientRange.isSelected() &&
                ((LayerObjectAmbient)obj).isLocal()) {
              // skipped: will be handled in AmbientRange layer
              break;
            }
            if (stacking == LayerStackingType.AMBIENT_RANGE) {
              if (((LayerObjectAmbient)obj).isLocal() &&
                  i == ViewerConstants.AMBIENT_ITEM_ICON) {
                // considering ranged item only
                continue;
              } else if (!((LayerObjectAmbient)obj).isLocal()) {
                // global sounds don't have ambient ranges
                break;
              }
            }

            final AbstractLayerItem item = items[i];
            itemLocation.x = canvasCoords.x - item.getX();
            itemLocation.y = canvasCoords.y - item.getY();
            if (item.isVisible() && item.contains(itemLocation)) {
              // creating a new menu item
              StringBuilder sb = new StringBuilder();
              if (item.getName() != null && !item.getName().isEmpty()) {
                sb.append(item.getName());
              } else {
                sb.append("Item");
              }
              sb.append(": ");
              int lenPrefix = sb.length();
              int lenMsg = item.getToolTipText().length();
              if (lenPrefix + lenMsg > MaxLen) {
                sb.append(item.getToolTipText().substring(0, MaxLen - lenPrefix));
                sb.append("...");
              } else {
                sb.append(item.getToolTipText());
              }
              DataMenuItem dmi = new DataMenuItem(sb.toString(), null, item);
              if (lenPrefix + lenMsg > MaxLen) {
                dmi.setToolTipText(item.getToolTipText());
              }
              dmi.addActionListener(getListeners());
              menuItems.add(dmi);
            }
          }
        }
      }

      // updating context menu with the prepared item list
      if (!menuItems.isEmpty()) {
        for (final JMenuItem item : menuItems) {
          pmItems.add(item);
        }
      }
      return !menuItems.isEmpty();
    }
    return false;
  }

  /** Shows a popup menu containing layer items located at the current position if available. */
  private void showItemPopup(MouseEvent event)
  {
    if (event != null && event.isPopupTrigger()) {
      Component parent = null;
      Point location = null;
      if (event.getSource() instanceof AbstractLayerItem) {
        parent = (AbstractLayerItem)event.getSource();
        location = parent.getLocation();
        location.translate(event.getX(), event.getY());
      } else if (event.getSource() == rcCanvas) {
        parent = rcCanvas;
        location = event.getPoint();
      }

      if (parent != null && location != null) {
        if (updateItemPopup(location)) {
          pmItems.show(parent, event.getX(), event.getY());
        }
      }
    }
  }

  /** Updates all available layer items. */
  private void reloadLayers()
  {
    SharedResourceCache.clearCache();
    rcCanvas.reload(true);
    reloadAreLayers(false);
    reloadWedLayers(false);
    applySettings();
  }

  /** Updates ARE-related layer items. */
  private void reloadAreLayers(boolean order)
  {
    if (layerManager != null) {
      for (int i = 0, ltCount = LayerManager.getLayerTypeCount(); i < ltCount; i++) {
        LayerType layer = LayerManager.getLayerType(i);
        LayerStackingType layer2 = Settings.layerToStacking(layer);
        if (layer != LayerType.DOOR_POLY && layer != LayerType.WALL_POLY) {
          layerManager.reload(layer);
          updateLayerItems(layer2);
          addLayerItems(layer2);
          if (layer == LayerType.AMBIENT) {
            updateLayerItems(LayerStackingType.AMBIENT_RANGE);
            addLayerItems(LayerStackingType.AMBIENT_RANGE);
          }
          showLayer(layer, cbLayers[i].isSelected());
        }
      }
    }
    updateRealActors();
    updateRealActorsLighting(getVisualState());
    updateAmbientRange();
    updateRealAnimation();
    updateRealAnimationsLighting(getVisualState());
    if (order) {
      orderLayerItems();
    }
  }

  /** Updates WED-related layer items. */
  private void reloadWedLayers(boolean order)
  {
    if (layerManager != null) {
      layerManager.close(LayerType.DOOR_POLY);
      layerManager.close(LayerType.WALL_POLY);
      layerManager.setWedResource(getCurrentWed());
      layerManager.reload(LayerType.DOOR_POLY);
      layerManager.reload(LayerType.WALL_POLY);
      updateLayerItems(LayerStackingType.DOOR_POLY);
      updateLayerItems(LayerStackingType.WALL_POLY);
      addLayerItems(LayerStackingType.DOOR_POLY);
      addLayerItems(LayerStackingType.WALL_POLY);
      showLayer(LayerType.DOOR_POLY, cbLayers[LayerManager.getLayerTypeIndex(LayerType.DOOR_POLY)].isSelected());
      showLayer(LayerType.WALL_POLY, cbLayers[LayerManager.getLayerTypeIndex(LayerType.WALL_POLY)].isSelected());
    }
    if (order) {
      orderLayerItems();
    }
  }


  /** Returns the identifier of the specified layer checkbox, or null on error. */
  private LayerType getLayerType(JCheckBox cb)
  {
    if (cb != null) {
      for (int i = 0; i < cbLayers.length; i++) {
        if (cb == cbLayers[i]) {
          return LayerManager.getLayerType(i);
        }
      }
    }
    return null;
  }

  /** Returns whether the specified layer is visible (by layer). */
  private boolean isLayerEnabled(LayerType layer)
  {
    if (layer != null) {
      return ((Settings.LayerFlags & (1 << LayerManager.getLayerTypeIndex(layer))) != 0);
    } else {
      return false;
    }
  }

  /** Opens a viewable instance associated with the specified layer item. */
  private void showTable(AbstractLayerItem item)
  {
    if (item != null) {
      if (item.getViewable() instanceof AbstractStruct) {
        Window wnd = getViewerWindow((AbstractStruct)item.getViewable());
        ((AbstractStruct)item.getViewable()).selectEditTab();
        wnd.setVisible(true);
        wnd.toFront();
      } else {
        item.showViewable();
      }
    }
  }

  /**
   * Attempts to find the Window instance containing the viewer of the specified
   * AbstractStruct object.
   * If it cannot find one, it creates and returns a new one.
   * If all fails, it returns the NearInfinity instance.
   */
  private Window getViewerWindow(AbstractStruct as)
  {
    if (as != null) {
      final StructViewer sv = as.getViewer();
      if (sv != null && sv.getParent() != null) {
        // Determining whether the structure is associated with any open NearInfinity window
        Component[] list = sv.getParent().getComponents();
        if (list != null) {
          for (final Component comp : list) {
            if (comp == sv) {
              Component c = sv.getParent();
              while (c != null) {
                if (c instanceof Window) {
                  // Window found, returning
                  return (Window)c;
                }
                c = c.getParent();
              }
            }
          }
        }
      }
      // Window not found, creating and returning a new one
      List<AbstractStruct> structChain = new ArrayList<>();
      structChain.add(as);
      AbstractStruct as2 = as;
      while (as2.getParent() != null) {
        structChain.add(0, as2.getParent());
        as2 = as2.getParent();
      }

      if (map.getAre() == structChain.get(0)) {
        // no need to create the whole Viewable chain
        return new ViewFrame(NearInfinity.getInstance(), as);
      }

      // recycling existing Viewables if possible
      Window wnd = NearInfinity.getInstance();
      for (final Iterator<ChildFrame> iter = ChildFrame.getFrameIterator(cf -> cf instanceof ViewFrame &&
                                                                               ((ViewFrame)cf).getViewable() instanceof AbstractStruct);
          iter.hasNext(); ) {
        ViewFrame vf = (ViewFrame)iter.next();
        AbstractStruct struct = (AbstractStruct)vf.getViewable();
        if (struct.getResourceEntry() != null &&
            struct.getResourceEntry().equals(structChain.get(0).getResourceEntry())) {
          structChain.remove(0);
          wnd = vf;
          break;
        }
      }

      // creating Viewable chain
      for (int i = 0; i < structChain.size(); i++) {
        wnd = new ViewFrame(wnd, structChain.get(i));
      }
      if (wnd != null && wnd != NearInfinity.getInstance()) {
        return wnd;
      }
    }
    // Last resort: returning NearInfinity instance
    return NearInfinity.getInstance();
  }

  /** Updates the visibility state of minimaps (search/height/light maps). */
  private void updateMiniMap()
  {
    final int type;
    if (cbMiniMaps[ViewerConstants.MAP_SEARCH].isSelected()) {
      type = ViewerConstants.MAP_SEARCH;
    } else if (cbMiniMaps[ViewerConstants.MAP_LIGHT].isSelected()) {
      type = ViewerConstants.MAP_LIGHT;
    } else if (cbMiniMaps[ViewerConstants.MAP_HEIGHT].isSelected()) {
      type = ViewerConstants.MAP_HEIGHT;
    } else {
      type = ViewerConstants.MAP_NONE;
    }
    updateTreeNode(cbMiniMaps[ViewerConstants.MAP_SEARCH]);
    updateTreeNode(cbMiniMaps[ViewerConstants.MAP_LIGHT]);
    updateTreeNode(cbMiniMaps[ViewerConstants.MAP_HEIGHT]);
    Settings.MiniMap = type;
    rcCanvas.setMiniMap(Settings.MiniMap, map.getMiniMap(Settings.MiniMap, getDayTime() == ViewerConstants.LIGHTING_NIGHT));
  }

  /** Sets visibility state of scheduled layer items depending on current day time. */
  private void updateScheduledItems()
  {
    if (layerManager != null) {
      for (int i = 0, ltCount = LayerManager.getLayerTypeCount(); i < ltCount; i++) {
        LayerType layer = LayerManager.getLayerType(i);
        layerManager.setLayerVisible(layer, isLayerEnabled(layer));
      }
    }
  }

  /** Applying time schedule settings to layer items. */
  private void updateTimeSchedules()
  {
    layerManager.setScheduleEnabled(Settings.EnableSchedules);
    updateWindowTitle();
  }

  /** Updates the state of the ambient sound range checkbox and associated functionality. */
  private void updateAmbientRange()
  {
    if (layerManager != null) {
      LayerAmbient layer = (LayerAmbient)layerManager.getLayer(LayerType.AMBIENT);
      if (layer != null) {
        JCheckBox cb = cbLayers[LayerManager.getLayerTypeIndex(LayerType.AMBIENT)];
        cbLayerAmbientRange.setEnabled(cb.isSelected() && layer.getLayerObjectCount(ViewerConstants.AMBIENT_TYPE_LOCAL) > 0);
        boolean state = cbLayerAmbientRange.isEnabled() && cbLayerAmbientRange.isSelected();
        layer.setItemTypeEnabled(ViewerConstants.AMBIENT_ITEM_RANGE, state);
      } else {
        cbLayerAmbientRange.setEnabled(false);
      }
      updateTreeNode(cbLayerAmbientRange);

      // Storing settings
      Settings.ShowAmbientRanges = cbLayerAmbientRange.isSelected();
    }
  }

  /** Applies the specified lighting condition to real actor items. */
  private void updateRealActorsLighting(int visualState)
  {
    if (layerManager != null) {
      List<? extends LayerObject> list = layerManager.getLayerObjects(LayerType.ACTOR);
      if (list != null) {
        for (final LayerObject obj : list) {
          ((LayerObjectActor)obj).setLighting(visualState);
        }
      }
    }
  }

  /** Applies the specified lighting condition to real animation items. */
  private void updateRealAnimationsLighting(int visualState)
  {
    if (layerManager != null) {
      List<? extends LayerObject> list = layerManager.getLayerObjects(LayerType.ANIMATION);
      if (list != null) {
        for (final LayerObject obj : list) {
          ((LayerObjectAnimation)obj).setLighting(visualState);
        }
      }
    }
  }

  /** Updates the state of real actor checkboxes and their associated functionality. */
  private void updateRealActors()
  {
    if (layerManager != null) {
      LayerActor layer = (LayerActor)layerManager.getLayer(LayerType.ACTOR);
      if (layer != null) {
        JCheckBox cb = cbLayers[LayerManager.getLayerTypeIndex(LayerType.ACTOR)];
        boolean enabled = cb.isEnabled() && cb.isSelected();
        cbLayerRealActor[0].setEnabled(enabled);
        cbLayerRealActor[1].setEnabled(enabled);
        boolean animEnabled = false;
        boolean animPlaying = false;
        if (enabled) {
          if (cbLayerRealActor[0].isSelected()) {
            animEnabled = true;
          } else if (cbLayerRealActor[1].isSelected()) {
            animEnabled = true;
            animPlaying = true;
          }
        }
        layer.setRealActorEnabled(animEnabled);
        layer.setRealActorPlaying(animPlaying);
      } else {
        cbLayerRealActor[0].setEnabled(false);
        cbLayerRealActor[1].setEnabled(false);
      }
      updateTreeNode(cbLayerRealActor[0]);
      updateTreeNode(cbLayerRealActor[1]);

      // Storing settings
      if (!cbLayerRealActor[0].isSelected() && !cbLayerRealActor[1].isSelected()) {
        Settings.ShowActorSprites = ViewerConstants.ANIM_SHOW_NONE;
      } else if (cbLayerRealActor[0].isSelected() && !cbLayerRealActor[1].isSelected()) {
        Settings.ShowActorSprites = ViewerConstants.ANIM_SHOW_STILL;
      } else if (!cbLayerRealActor[0].isSelected() && cbLayerRealActor[1].isSelected()) {
        Settings.ShowActorSprites = ViewerConstants.ANIM_SHOW_ANIMATED;
      }
    }
  }

  /** Updates the state of real animation checkboxes and their associated functionality. */
  private void updateRealAnimation()
  {
    if (layerManager != null) {
      LayerAnimation layer = (LayerAnimation)layerManager.getLayer(LayerType.ANIMATION);
      if (layer != null) {
        JCheckBox cb = cbLayers[LayerManager.getLayerTypeIndex(LayerType.ANIMATION)];
        boolean enabled = cb.isEnabled() && cb.isSelected();
        cbLayerRealAnimation[0].setEnabled(enabled);
        cbLayerRealAnimation[1].setEnabled(enabled);
        boolean animEnabled = false;
        boolean animPlaying = false;
        if (enabled) {
          if (cbLayerRealAnimation[0].isSelected()) {
            animEnabled = true;
          } else if (cbLayerRealAnimation[1].isSelected()) {
            animEnabled = true;
            animPlaying = true;
          }
        }
        layer.setRealAnimationEnabled(animEnabled);
        layer.setRealAnimationPlaying(animPlaying);
      } else {
        cbLayerRealAnimation[0].setEnabled(false);
        cbLayerRealAnimation[1].setEnabled(false);
      }
      updateTreeNode(cbLayerRealAnimation[0]);
      updateTreeNode(cbLayerRealAnimation[1]);

      // Storing settings
      if (!cbLayerRealAnimation[0].isSelected() && !cbLayerRealAnimation[1].isSelected()) {
        Settings.ShowRealAnimations = ViewerConstants.ANIM_SHOW_NONE;
      } else if (cbLayerRealAnimation[0].isSelected() && !cbLayerRealAnimation[1].isSelected()) {
        Settings.ShowRealAnimations = ViewerConstants.ANIM_SHOW_STILL;
      } else if (!cbLayerRealAnimation[0].isSelected() && cbLayerRealAnimation[1].isSelected()) {
        Settings.ShowRealAnimations = ViewerConstants.ANIM_SHOW_ANIMATED;
      }
    }
  }

  /** Show/hide items of the specified layer. */
  private void showLayer(LayerType layer, boolean visible)
  {
    if (layer != null && layerManager != null) {
      layerManager.setLayerVisible(layer, visible);
      // updating layer states
      int bit = 1 << LayerManager.getLayerTypeIndex(layer);
      if (visible) {
        Settings.LayerFlags |= bit;
      } else {
        Settings.LayerFlags &= ~bit;
      }
    }
  }


  /** Adds items of all available layers to the map canvas. */
  private void addLayerItems()
  {
    for (int i = 0, lloSize = Settings.ListLayerOrder.size(); i < lloSize; i++) {
      addLayerItems(Settings.ListLayerOrder.get(i));
    }
  }

  /** Adds items of the specified layer to the map canvas. */
  private void addLayerItems(LayerStackingType layer)
  {
    if (layer != null && layerManager != null) {
      List<? extends LayerObject> list = layerManager.getLayerObjects(Settings.stackingToLayer(layer));
      if (list != null) {
        for (int i = 0, size = list.size(); i < size; i++) {
          addLayerItem(layer, list.get(i));
        }
      }
    }
  }

  /** Adds items of a single layer object to the map canvas. */
  private void addLayerItem(LayerStackingType layer, LayerObject object)
  {
    if (object != null) {
      // Dealing with ambient icons and ambient ranges separately
      if (layer == LayerStackingType.AMBIENT) {
        AbstractLayerItem item = object.getLayerItem(ViewerConstants.AMBIENT_ITEM_ICON);
        if (item != null) {
          rcCanvas.add(item);
        }
      } else if (layer == LayerStackingType.AMBIENT_RANGE) {
        AbstractLayerItem item = object.getLayerItem(ViewerConstants.AMBIENT_ITEM_RANGE);
        if (item != null) {
          rcCanvas.add(item);
        }
      } else {
        for (final AbstractLayerItem item : object.getLayerItems()) {
          rcCanvas.add(item);
        }
      }
    }
  }


  /** Removes all items of all available layers. */
  private void removeLayerItems()
  {
    for (int i = 0, lloSize = Settings.ListLayerOrder.size(); i < lloSize; i++) {
      removeLayerItems(Settings.ListLayerOrder.get(i));
    }
  }

  /** Removes all items of the specified layer. */
  private void removeLayerItems(LayerStackingType layer)
  {
    if (layer != null && layerManager != null) {
      List<? extends LayerObject> list = layerManager.getLayerObjects(Settings.stackingToLayer(layer));
      if (list != null) {
        for (final LayerObject obj : list) {
          removeLayerItem(layer, obj);
        }
      }
    }
  }

  /** Removes items of a single layer object from the map canvas. */
  private void removeLayerItem(LayerStackingType layer, LayerObject object)
  {
    if (object != null) {
      if (layer == LayerStackingType.AMBIENT) {
        AbstractLayerItem item = object.getLayerItem(ViewerConstants.AMBIENT_ITEM_ICON);
        rcCanvas.remove(item);
      } else if (layer == LayerStackingType.AMBIENT_RANGE) {
        AbstractLayerItem item = object.getLayerItem(ViewerConstants.AMBIENT_ITEM_RANGE);
        if (item != null) {
          rcCanvas.remove(item);
        }
      } else {
        for (final AbstractLayerItem item : object.getLayerItems()) {
          rcCanvas.remove(item);
        }
      }
    }
  }


  /** Re-orders layer items on the map using listLayer for determining priorities. */
  private void orderLayerItems()
  {
    if (layerManager != null) {
      int index = 0;
      for (final LayerStackingType type : Settings.ListLayerOrder) {
        final List<? extends LayerObject> list = layerManager.getLayerObjects(Settings.stackingToLayer(type));
        if (list == null) { continue; }

        for (final LayerObject obj : list) {
          if (type == LayerStackingType.AMBIENT_RANGE) {
            // Special: process ambient ranges only
            AbstractLayerItem item = obj.getLayerItem(ViewerConstants.AMBIENT_ITEM_RANGE);
            if (item != null) {
              rcCanvas.setComponentZOrder(item, index);
              index++;
            }
          } else if (type == LayerStackingType.AMBIENT) {
            // Special: process ambient icons only
            AbstractLayerItem item = obj.getLayerItem(ViewerConstants.AMBIENT_ITEM_ICON);
            rcCanvas.setComponentZOrder(item, index);
            index++;
          } else {
            for (final AbstractLayerItem item : obj.getLayerItems()) {
              if (item.getParent() != null) {
                rcCanvas.setComponentZOrder(item, index);
                index++;
              }
            }
          }
        }
      }
    }
  }


  /** Updates all items of all available layers. */
  private void updateLayerItems()
  {
    for (final LayerStackingType type : Settings.ListLayerOrder) {
      updateLayerItems(type);
    }
  }

  /** Updates the map locations of the items in the specified layer. */
  private void updateLayerItems(LayerStackingType layer)
  {
    if (layer != null && layerManager != null) {
      List<? extends LayerObject> list = layerManager.getLayerObjects(Settings.stackingToLayer(layer));
      if (list != null) {
        for (final LayerObject obj : list) {
          updateLayerItem(obj);
        }
      }
    }
  }

  /** Updates the map locations of the items in the specified layer object. */
  private void updateLayerItem(LayerObject object)
  {
    if (object != null) {
      object.update(getZoomFactor());
    }
  }

  /** Update toolbar-related stuff. */
  private void updateToolBarButtons()
  {
    tbWed.setToolTipText(String.format("Edit WED structure (%s)", getCurrentWed().getName()));
  }

  /** Initializes a new progress monitor instance. */
  private void initProgressMonitor(Component parent, String msg, String note, int maxProgress,
                                   int msDecide, int msWait)
  {
    if (parent == null) parent = NearInfinity.getInstance();
    if (maxProgress <= 0) maxProgress = 1;

    releaseProgressMonitor();
    pmMax = maxProgress;
    pmCur = 0;
    progress = new ProgressMonitor(parent, msg + "        \t", note, 0, pmMax);
    progress.setMillisToDecideToPopup(msDecide);
    progress.setMillisToPopup(msWait);
    progress.setProgress(pmCur);
  }

  /** Closes the current progress monitor. */
  private void releaseProgressMonitor()
  {
    if (progress != null) {
      progress.close();
      progress = null;
    }
  }

  /** Advances the current progress monitor by one and adds the specified note. */
  private void advanceProgressMonitor(String note)
  {
    if (progress != null) {
      if (pmCur < pmMax) {
        pmCur++;
        if (note != null) {
          progress.setNote(note);
        }
        progress.setProgress(pmCur);
      }
    }
  }

  /** Returns whether a progress monitor is currently active. */
  private boolean isProgressMonitorActive()
  {
    return progress != null;
  }


  /** Updates the tree node containing the specified component. */
  private void updateTreeNode(Component c)
  {
    if (treeControls != null) {
      DefaultTreeModel model = (DefaultTreeModel)treeControls.getModel();
      if (model.getRoot() instanceof TreeNode) {
        TreeNode node = getTreeNodeOf((TreeNode)model.getRoot(), c);
        if (node != null) {
          model.nodeChanged(node);
        }
      }
    }
  }

  /** Recursive function to find the node containing c. */
  private TreeNode getTreeNodeOf(TreeNode node, Component c)
  {
    if (node != null && node instanceof DefaultMutableTreeNode && c != null) {
      if (((DefaultMutableTreeNode)node).getUserObject() == c) {
        return node;
      }
      for (int i = 0, cCount = node.getChildCount(); i < cCount; i++) {
        TreeNode retVal = getTreeNodeOf(node.getChildAt(i), c);
        if (retVal != null) {
          return retVal;
        }
      }
    }
    return null;
  }

  /** Shows settings dialog and updates respective controls if needed. */
  private void viewSettings()
  {
    SettingsDialog vs = new SettingsDialog(this);
    if (vs.settingsChanged()) {
      applySettings();
    }
  }

  /** Applies current global area viewer settings. */
  private void applySettings()
  {
    // applying layer stacking order
    orderLayerItems();
    // applying interpolation settings to map
    switch (Settings.InterpolationMap) {
      case ViewerConstants.FILTERING_AUTO:
        rcCanvas.setForcedInterpolation(false);
        break;
      case ViewerConstants.FILTERING_NEARESTNEIGHBOR:
        rcCanvas.setInterpolationType(ViewerConstants.TYPE_NEAREST_NEIGHBOR);
        rcCanvas.setForcedInterpolation(true);
        break;
      case ViewerConstants.FILTERING_BILINEAR:
        rcCanvas.setInterpolationType(ViewerConstants.TYPE_BILINEAR);
        rcCanvas.setForcedInterpolation(true);
        break;
    }
    // applying minimap alpha
    rcCanvas.setMiniMapTransparency((int)(Settings.MiniMapAlpha*255.0));

    // applying mouse wheel zoom
    if (Settings.MouseWheelZoom) {
      rcCanvas.addMouseWheelListener(getListeners());
    } else {
      rcCanvas.removeMouseWheelListener(getListeners());
    }

    if (layerManager != null) {
      // applying actor frame settings
      ((LayerActor)layerManager.getLayer(LayerType.ACTOR)).setRealActorFrameState(Settings.ShowActorFrame);
      // applying actor selection circle visibility
      ((LayerActor)layerManager.getLayer(LayerType.ACTOR)).setRealActorSelectionCircleEnabled(Settings.ShowActorSelectionCircle);
      // applying actor personal space visibility
      ((LayerActor)layerManager.getLayer(LayerType.ACTOR)).setRealActorPersonalSpaceEnabled(Settings.ShowActorPersonalSpace);
      // applying animation frame settings
      ((LayerAnimation)layerManager.getLayer(LayerType.ANIMATION)).setRealAnimationFrameState(Settings.ShowAnimationFrame);
      // applying animation active override settings
      ((LayerAnimation)layerManager.getLayer(LayerType.ANIMATION)).setRealAnimationActiveIgnored(Settings.OverrideAnimVisibility);
      // applying interpolation settings to animations
      switch (Settings.InterpolationAnim) {
        case ViewerConstants.FILTERING_AUTO:
          layerManager.setRealAnimationForcedInterpolation(false);
          break;
        case ViewerConstants.FILTERING_NEARESTNEIGHBOR:
          layerManager.setRealAnimationInterpolation(ViewerConstants.TYPE_NEAREST_NEIGHBOR);
          layerManager.setRealAnimationForcedInterpolation(true);
          break;
        case ViewerConstants.FILTERING_BILINEAR:
          layerManager.setRealAnimationInterpolation(ViewerConstants.TYPE_BILINEAR);
          layerManager.setRealAnimationForcedInterpolation(true);
          break;
      }
      // applying frame rate to animated overlays
      int interval = (int)(1000.0 / Settings.FrameRateOverlays);
      if (interval != timerOverlays.getDelay()) {
        timerOverlays.setDelay(interval);
      }
      // applying frame rate to actor sprites and background animations
      layerManager.setRealAnimationFrameRate(Settings.FrameRateAnimations);
    }
  }

  /** Exports the current map state to PNG. */
  private void exportMap()
  {
    WindowBlocker.blockWindow(this, true);
    initProgressMonitor(this, "Exporting to PNG...", null, 1, 0, 0);

    // prevent blocking the event queue
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run()
      {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        boolean bRet = false;
        try {
          final BufferedImage dstImage;
          if (isExportLayersEnabled()) {
            double zoom = getZoomFactor();
            setZoomFactor(1.0, 1.0);
            try {
              dstImage = new BufferedImage(rcCanvas.getWidth(), rcCanvas.getHeight(), BufferedImage.TYPE_INT_RGB);
              Graphics2D g = dstImage.createGraphics();
              rcCanvas.paint(g);
              g.dispose();
            } finally {
              setZoomFactor(zoom, Settings.ZoomFactor);
            }
          } else {
            VolatileImage srcImage = (VolatileImage)rcCanvas.getImage();
            dstImage = ColorConvert.createCompatibleImage(srcImage.getWidth(), srcImage.getHeight(),
                                                          srcImage.getTransparency());
            Graphics2D g = dstImage.createGraphics();
            g.drawImage(srcImage, 0, 0, null);
            g.dispose();
          }
          bRet = ImageIO.write(dstImage, "png", os);
          dstImage.flush();
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          releaseProgressMonitor();
          WindowBlocker.blockWindow(AreaViewer.this, false);
        }
        if (bRet) {
          String fileName = map.getAre().getResourceEntry().getResourceName()
                              .toUpperCase(Locale.US).replace(".ARE", ".PNG");
          ResourceFactory.exportResource(map.getAre().getResourceEntry(),
                                         StreamUtils.getByteBuffer(os.toByteArray()),
                                         fileName, AreaViewer.this);
        } else {
          JOptionPane.showMessageDialog(AreaViewer.this, "Error while exporting map as graphics.",
                                        "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    });
  }


//----------------------------- INNER CLASSES -----------------------------

  /** Handles all events of the viewer. */
  private class Listeners implements ActionListener, MouseListener, MouseMotionListener, MouseWheelListener,
                                     ChangeListener, TilesetChangeListener, PropertyChangeListener,
                                     LayerItemListener, ComponentListener, TreeExpansionListener
  {
    public Listeners()
    {
    }

    //--------------------- Begin Interface ActionListener ---------------------

    @Override
    public void actionPerformed(ActionEvent event)
    {
      if (event.getSource() instanceof JCheckBox) {
        JCheckBox cb = (JCheckBox)event.getSource();
        LayerType layer = getLayerType(cb);
        if (layer != null) {
          showLayer(layer, cb.isSelected());
          if (layer == LayerType.ACTOR) {
            // Taking care of real animation display
            updateRealActors();
          } else if (layer == LayerType.AMBIENT) {
            // Taking care of local ambient ranges
            updateAmbientRange();
          } else if (layer == LayerType.ANIMATION) {
            // Taking care of real animation display
            updateRealAnimation();
          }
          updateScheduledItems();
        } else if (cb == cbLayerRealActor[0]) {
          if (cbLayerRealActor[0].isSelected()) {
            cbLayerRealActor[1].setSelected(false);
          }
          updateRealActors();
        } else if (cb == cbLayerRealActor[1]) {
          if (cbLayerRealActor[1].isSelected()) {
            cbLayerRealActor[0].setSelected(false);
          }
          updateRealActors();
        } else if (cb == cbLayerAmbientRange) {
          updateAmbientRange();
        } else if (cb == cbLayerRealAnimation[0]) {
          if (cbLayerRealAnimation[0].isSelected()) {
            cbLayerRealAnimation[1].setSelected(false);
          }
          updateRealAnimation();
        } else if (cb == cbLayerRealAnimation[1]) {
          if (cbLayerRealAnimation[1].isSelected()) {
            cbLayerRealAnimation[0].setSelected(false);
          }
          updateRealAnimation();
        } else if (cb == cbEnableSchedules) {
          WindowBlocker.blockWindow(AreaViewer.this, true);
          try {
            Settings.EnableSchedules = cbEnableSchedules.isSelected();
            updateTimeSchedules();
          } finally {
            WindowBlocker.blockWindow(AreaViewer.this, false);
          }
        } else if (cb == cbDrawClosed) {
          WindowBlocker.blockWindow(AreaViewer.this, true);
          try {
            setDoorState(cb.isSelected());
          } finally {
            WindowBlocker.blockWindow(AreaViewer.this, false);
          }
        } else if (cb == cbDrawGrid) {
          WindowBlocker.blockWindow(AreaViewer.this, true);
          try {
            setTileGridEnabled(cb.isSelected());
          } finally {
            WindowBlocker.blockWindow(AreaViewer.this, false);
          }
        } else if (cb == cbDrawOverlays) {
          WindowBlocker.blockWindow(AreaViewer.this, true);
          try {
            setOverlaysEnabled(cb.isSelected());
            cbAnimateOverlays.setEnabled(cb.isSelected());
            if (!cb.isSelected() && cbAnimateOverlays.isSelected()) {
              cbAnimateOverlays.setSelected(false);
              setOverlaysAnimated(false);
            }
            updateTreeNode(cbAnimateOverlays);
          } finally {
            WindowBlocker.blockWindow(AreaViewer.this, false);
          }
        } else if (cb == cbAnimateOverlays) {
          WindowBlocker.blockWindow(AreaViewer.this, true);
          try {
            setOverlaysAnimated(cb.isSelected());
          } finally {
            WindowBlocker.blockWindow(AreaViewer.this, false);
          }
        } else if (cb == cbMiniMaps[ViewerConstants.MAP_SEARCH]) {
          if (cb.isSelected()) {
            cbMiniMaps[ViewerConstants.MAP_LIGHT].setSelected(false);
            cbMiniMaps[ViewerConstants.MAP_HEIGHT].setSelected(false);
          }
          WindowBlocker.blockWindow(AreaViewer.this, true);
          try {
            updateMiniMap();
          } finally {
            WindowBlocker.blockWindow(AreaViewer.this, false);
          }
        } else if (cb == cbMiniMaps[ViewerConstants.MAP_LIGHT]) {
          if (cb.isSelected()) {
            cbMiniMaps[ViewerConstants.MAP_SEARCH].setSelected(false);
            cbMiniMaps[ViewerConstants.MAP_HEIGHT].setSelected(false);
          }
          WindowBlocker.blockWindow(AreaViewer.this, true);
          try {
            updateMiniMap();
          } finally {
            WindowBlocker.blockWindow(AreaViewer.this, false);
          }
        } else if (cb == cbMiniMaps[ViewerConstants.MAP_HEIGHT]) {
          if (cb.isSelected()) {
            cbMiniMaps[ViewerConstants.MAP_SEARCH].setSelected(false);
            cbMiniMaps[ViewerConstants.MAP_LIGHT].setSelected(false);
          }
          WindowBlocker.blockWindow(AreaViewer.this, true);
          try {
            updateMiniMap();
          } finally {
            WindowBlocker.blockWindow(AreaViewer.this, false);
          }
        }
      } else if (event.getSource() == cbZoomLevel) {
        WindowBlocker.blockWindow(AreaViewer.this, true);
        try {
          double previousZoomFactor = Settings.ZoomFactor;
          try {
            setZoomFactor(Settings.ItemZoomFactor[cbZoomLevel.getSelectedIndex()], previousZoomFactor);
          } catch (OutOfMemoryError e) {
            e.printStackTrace();
            cbZoomLevel.hidePopup();
            WindowBlocker.blockWindow(AreaViewer.this, false);
            String msg = "Not enough memory to set selected zoom level.\n"
                + "(Note: It is highly recommended to close and reopen the area viewer.)";
            JOptionPane.showMessageDialog(AreaViewer.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
            cbZoomLevel.setSelectedIndex(Settings.getZoomLevelIndex(previousZoomFactor));
            setZoomFactor(previousZoomFactor, Settings.ZoomFactorDefault);
          }
        } finally {
          WindowBlocker.blockWindow(AreaViewer.this, false);
        }
      } else if (event.getSource() == timerOverlays) {
        // Important: making sure that only ONE instance is running at a time to avoid GUI freezes
        if (workerOverlays == null) {
          workerOverlays = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
              advanceOverlayAnimation();
              return null;
            }
          };
          workerOverlays.addPropertyChangeListener(this);
          workerOverlays.execute();
        }
      } else if (event.getSource() instanceof AbstractLayerItem) {
        AbstractLayerItem item = (AbstractLayerItem)event.getSource();
        showTable(item);
      } else if (event.getSource() instanceof DataMenuItem) {
        DataMenuItem lmi = (DataMenuItem)event.getSource();
        AbstractLayerItem item = (AbstractLayerItem)lmi.getData();
        showTable(item);
      } else if (event.getSource() == tbAre) {
        showTable(map.getAreItem());
      } else if (event.getSource() == tbWed) {
        showTable(map.getWedItem(getCurrentWedIndex()));
      } else if (event.getSource() == tbSongs) {
        showTable(map.getSongItem());
      } else if (event.getSource() == tbRest) {
        showTable(map.getRestItem());
      } else if (event.getSource() == tbSettings) {
        viewSettings();
      } else if (event.getSource() == tbRefresh) {
        WindowBlocker.blockWindow(AreaViewer.this, true);
        try {
          reloadLayers();
        } finally {
          WindowBlocker.blockWindow(AreaViewer.this, false);
        }
//      } else if (ArrayUtil.indexOf(tbAddLayerItem, event.getSource()) >= 0) {
//        // TODO: include "Add layer item" functionality
//        int index = ArrayUtil.indexOf(tbAddLayerItem, event.getSource());
//        switch (LayerManager.getLayerType(index)) {
//          case Actor:
//          case Ambient:
//          case Animation:
//          case Automap:
//          case Container:
//          case Door:
//          case DoorPoly:
//          case Entrance:
//          case ProTrap:
//          case Region:
//          case SpawnPoint:
//          case Transition:
//          case WallPoly:
//            break;
//        }
      } else if (event.getSource() == tbExportPNG) {
        exportMap();
      }
    }

    //--------------------- End Interface ActionListener ---------------------

    //--------------------- Begin Interface MouseMotionListener ---------------------

    @Override
    public void mouseDragged(MouseEvent event)
    {
      if (event.getSource() == rcCanvas && isMapDragging(event.getLocationOnScreen())) {
        moveMapViewport();
      }
    }

    @Override
    public void mouseMoved(MouseEvent event)
    {
      if (event.getSource() == rcCanvas) {
        showMapCoordinates(event.getPoint());
      } else if (event.getSource() instanceof AbstractLayerItem) {
        AbstractLayerItem item = (AbstractLayerItem)event.getSource();
        MouseEvent newEvent = new MouseEvent(rcCanvas, event.getID(), event.getWhen(), event.getModifiers(),
            event.getX() + item.getX(), event.getY() + item.getY(),
            event.getXOnScreen(), event.getYOnScreen(),
            event.getClickCount(), event.isPopupTrigger(), event.getButton());
        rcCanvas.dispatchEvent(newEvent);
      }
    }

    //--------------------- End Interface MouseMotionListener ---------------------

    //--------------------- Begin Interface MouseListener ---------------------

    @Override
    public void mouseClicked(MouseEvent event)
    {
    }

    @Override
    public void mousePressed(MouseEvent event)
    {
      if (event.getButton() == MouseEvent.BUTTON1 && event.getSource() == rcCanvas) {
        setMapDraggingEnabled(true, event.getLocationOnScreen());
      } else {
        showItemPopup(event);
      }
    }

    @Override
    public void mouseReleased(MouseEvent event)
    {
      if (event.getButton() == MouseEvent.BUTTON1 && event.getSource() == rcCanvas) {
        setMapDraggingEnabled(false, event.getLocationOnScreen());
      } else {
        showItemPopup(event);
      }
    }

    @Override
    public void mouseEntered(MouseEvent event)
    {
    }

    @Override
    public void mouseExited(MouseEvent event)
    {
    }

    //--------------------- End Interface MouseListener ---------------------

    //--------------------- Begin Interface MouseWheelListener ---------------------

    @Override
    public void mouseWheelMoved(MouseWheelEvent event)
    {
      if (event.getSource() == rcCanvas) {
        int notches = event.getWheelRotation();
        double zoom = getZoomFactor();
        if (notches > 0) {
          zoom = Settings.getNextZoomFactor(getZoomFactor(), false);
        } else if (notches < 0) {
          zoom = Settings.getNextZoomFactor(getZoomFactor(), true);
        }
        if (zoom != getZoomFactor()) {
          WindowBlocker.blockWindow(AreaViewer.this, true);
          try {
            double previousZoomFactor = Settings.ZoomFactor;
            try {
              setZoomFactor(zoom, previousZoomFactor);
            } catch (OutOfMemoryError e) {
              e.printStackTrace();
              cbZoomLevel.hidePopup();
              WindowBlocker.blockWindow(AreaViewer.this, false);
              String msg = "Not enough memory to set selected zoom level.\n"
                  + "(Note: It is highly recommended to close and reopen the area viewer.)";
              JOptionPane.showMessageDialog(AreaViewer.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
              cbZoomLevel.setSelectedIndex(Settings.getZoomLevelIndex(previousZoomFactor));
              setZoomFactor(previousZoomFactor, Settings.ZoomFactorDefault);
            }
          } finally {
            ActionListener[] list = cbZoomLevel.getActionListeners();
            try {
              for (final ActionListener l: list) {
                cbZoomLevel.removeActionListener(l);
              }
              cbZoomLevel.setSelectedIndex(Settings.getZoomLevelIndex(getZoomFactor()));
              treeControls.repaint();
              treeControls.revalidate();
            } finally {
              for (final ActionListener l: list) {
                cbZoomLevel.addActionListener(l);
              }
            }
            WindowBlocker.blockWindow(AreaViewer.this, false);
          }
        }
      }
    }

    //--------------------- End Interface MouseWheelListener ---------------------

    //--------------------- Begin Interface ChangeListener ---------------------

    @Override
    public void stateChanged(ChangeEvent event)
    {
      if (event.getSource() == pDayTime) {
        if (workerLoadMap == null) {
          // loading map in a separate thread
          if (workerLoadMap == null) {
            blocker = new WindowBlocker(AreaViewer.this);
            blocker.setBlocked(true);
            workerLoadMap = new SwingWorker<Void, Void>() {
              @Override
              protected Void doInBackground() throws Exception
              {
                setHour(pDayTime.getHour());
                return null;
              }
            };
            workerLoadMap.addPropertyChangeListener(this);
            workerLoadMap.execute();
          }
        }
      }
    }

    //--------------------- End Interface ChangeListener ---------------------

    //--------------------- Begin Interface TilesetChangeListener ---------------------

    @Override
    public void tilesetChanged(TilesetChangeEvent event)
    {
      if (event.getSource() == rcCanvas) {
        if (event.hasChangedMap()) {
          updateLayerItems();
        }
      }
    }

    //--------------------- End Interface TilesetChangeListener ---------------------

    //--------------------- Begin Interface LayerItemListener ---------------------

    @Override
    public void layerItemChanged(LayerItemEvent event)
    {
      if (event.getSource() instanceof AbstractLayerItem) {
        AbstractLayerItem item = (AbstractLayerItem)event.getSource();
        if (event.isHighlighted()) {
          setInfoText(item.getToolTipText());
        } else {
          setInfoText(null);
        }
      }
    }

    //--------------------- End Interface LayerItemListener ---------------------

    //--------------------- Begin Interface PropertyChangeListener ---------------------

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
      if (event.getSource() == workerInitGui) {
        if ("state".equals(event.getPropertyName()) &&
            SwingWorker.StateValue.DONE == event.getNewValue()) {
          releaseProgressMonitor();
          workerInitGui = null;
        }
      } else if (event.getSource() == workerLoadMap) {
        if ("state".equals(event.getPropertyName()) &&
            SwingWorker.StateValue.DONE == event.getNewValue()) {
          if (blocker != null) {
            blocker.setBlocked(false);
            blocker = null;
          }
          releaseProgressMonitor();
          workerLoadMap = null;
        }
      } else if (event.getSource() == workerOverlays) {
        if ("state".equals(event.getPropertyName()) &&
            SwingWorker.StateValue.DONE == event.getNewValue()) {
          // Important: making sure that only ONE instance is running at a time to avoid GUI freezes
          workerOverlays = null;
        }
      }
    }

    //--------------------- End Interface PropertyChangeListener ---------------------

    //--------------------- Begin Interface ComponentListener ---------------------

    @Override
    public void componentResized(ComponentEvent event)
    {
      if (event.getSource() == rcCanvas) {
        // changing panel size whenever the tileset size changes
        pCanvas.setPreferredSize(rcCanvas.getSize());
        pCanvas.setSize(rcCanvas.getSize());
      }
      if (event.getSource() == spCanvas) {
        if (isAutoZoom()) {
          setZoomFactor(Settings.ZoomFactorAuto, Settings.ZoomFactorDefault);
        }
        // centering the tileset if it fits into the viewport
        Dimension pDim = rcCanvas.getPreferredSize();
        Dimension spDim = pCanvas.getSize();
        if (pDim.width < spDim.width || pDim.height < spDim.height) {
          Point pLocation = rcCanvas.getLocation();
          Point pDistance = new Point();
          if (pDim.width < spDim.width) {
            pDistance.x = pLocation.x - (spDim.width - pDim.width) / 2;
          }
          if (pDim.height < spDim.height) {
            pDistance.y = pLocation.y - (spDim.height - pDim.height) / 2;
          }
          rcCanvas.setLocation(pLocation.x - pDistance.x, pLocation.y - pDistance.y);
        } else {
          rcCanvas.setLocation(0, 0);
        }
      }
    }

    @Override
    public void componentMoved(ComponentEvent event)
    {
    }

    @Override
    public void componentShown(ComponentEvent event)
    {
    }

    @Override
    public void componentHidden(ComponentEvent event)
    {
    }

    //--------------------- End Interface ComponentListener ---------------------

    //--------------------- Begin Interface TreeExpansionListener ---------------------

    @Override
    public void treeExpanded(TreeExpansionEvent event)
    {
      if (event.getPath().getLastPathComponent() instanceof DefaultMutableTreeNode) {
        // Storing the expanded state of the node if it marks a sidebar section
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)event.getPath().getLastPathComponent();
        if (node.getLevel() == 1) {
          DefaultMutableTreeNode root = (DefaultMutableTreeNode)node.getParent();
          for (int i = 0, cCount = root.getChildCount(); i < cCount; i++) {
            if (root.getChildAt(i) == node) {
              switch (1 << i) {
                case ViewerConstants.SIDEBAR_VISUALSTATE:
                  Settings.SidebarControls |= ViewerConstants.SIDEBAR_VISUALSTATE;
                  break;
                case ViewerConstants.SIDEBAR_LAYERS:
                  Settings.SidebarControls |= ViewerConstants.SIDEBAR_LAYERS;
                  break;
                case ViewerConstants.SIDEBAR_MINIMAPS:
                  Settings.SidebarControls |= ViewerConstants.SIDEBAR_MINIMAPS;
                  break;
              }
            }
          }
        }
      }
    }

    @Override
    public void treeCollapsed(TreeExpansionEvent event)
    {
      if (event.getPath().getLastPathComponent() instanceof DefaultMutableTreeNode) {
        // Storing the collapsed state of the node if it marks a sidebar section
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)event.getPath().getLastPathComponent();
        if (node.getLevel() == 1) {
          DefaultMutableTreeNode root = (DefaultMutableTreeNode)node.getParent();
          for (int i = 0, cCount = root.getChildCount(); i < cCount; i++) {
            if (root.getChildAt(i) == node) {
              switch (1 << i) {
                case ViewerConstants.SIDEBAR_VISUALSTATE:
                  Settings.SidebarControls &= ~ViewerConstants.SIDEBAR_VISUALSTATE;
                  break;
                case ViewerConstants.SIDEBAR_LAYERS:
                  Settings.SidebarControls &= ~ViewerConstants.SIDEBAR_LAYERS;
                  break;
                case ViewerConstants.SIDEBAR_MINIMAPS:
                  Settings.SidebarControls &= ~ViewerConstants.SIDEBAR_MINIMAPS;
                  break;
              }
            }
          }
        }
      }
    }

    //--------------------- End Interface TreeExpansionListener ---------------------
  }


  /** Handles map-specific properties. */
  private static class Map
  {
    private final Window parent;
    private final WedResource[] wed = new WedResource[2];
    private final AbstractLayerItem[] wedItem = new IconLayerItem[]{null, null};
    private final GraphicsResource[] mapLight = new GraphicsResource[]{null, null};

    private final AreResource are;
    private int overlayTransparency;
    private boolean hasDayNight, hasExtendedNight;
    private AbstractLayerItem areItem, songItem, restItem;
    private GraphicsResource mapSearch, mapHeight;

    public Map(Window parent, AreResource are)
    {
      this.parent = parent;
      this.are = are;
      init();
    }

    /**
     * Removes resources from memory.
     */
    public void clear()
    {
      songItem = null;
      restItem = null;
      areItem = null;
      closeWed(ViewerConstants.AREA_DAY, false);
      wed[ViewerConstants.AREA_DAY] = null;
      closeWed(ViewerConstants.AREA_NIGHT, false);
      wed[ViewerConstants.AREA_NIGHT] = null;
      wedItem[ViewerConstants.AREA_DAY] = null;
      wedItem[ViewerConstants.AREA_NIGHT] = null;
    }

    /**
     * Returns the current AreResource instance.
     * @return The current AreResource instance.
     */
    public AreResource getAre()
    {
      return are;
    }

    /**
     * Returns the WedResource instance of day or night map.
     * @param dayNight Either one of AREA_DAY or AREA_NIGHT.
     * @return The desired WedResource instance.
     */
    public WedResource getWed(int dayNight)
    {
      switch (dayNight) {
        case ViewerConstants.AREA_DAY: return wed[ViewerConstants.AREA_DAY];
        case ViewerConstants.AREA_NIGHT: return wed[ViewerConstants.AREA_NIGHT];
        default: return null;
      }
    }

    /**
     * Returns the specifie minimap.
     * @param mapType One of MAP_SEARCH, MAP_HEIGHT or MAP_LIGHT.
     * @param isNight Specify {@code true} to return the night-specific light map,
     *                or {@code false} to return the day-specific light map.
     * @return The specified BmpResource instance.
     */
    public GraphicsResource getMiniMap(int mapType, boolean isNight)
    {
      switch (mapType) {
        case ViewerConstants.MAP_SEARCH:
          return mapSearch;
        case ViewerConstants.MAP_HEIGHT:
          return mapHeight;
        case ViewerConstants.MAP_LIGHT:
          return isNight ? mapLight[1] : mapLight[0];
        default:
          return null;
      }
    }

    /**
     * Attempts to close the specified WED. If changes have been done, a dialog asks for saving.
     * @param dayNight Either AREA_DAY or AREA_NIGHT.
     * @param allowCancel Indicates whether to allow cancelling the saving process.
     * @return {@code true} if the resource has been closed, {@code false} otherwise (e.g.
     *         if the user chooses to cancel saving changes.)
     */
    public boolean closeWed(int dayNight, boolean allowCancel)
    {
      boolean bRet = false;
      dayNight = (dayNight == ViewerConstants.AREA_NIGHT) ? ViewerConstants.AREA_NIGHT : ViewerConstants.AREA_DAY;
      if (wed[dayNight] != null) {
        if (wed[dayNight].hasStructChanged()) {
          Path output;
          if (wed[dayNight].getResourceEntry() instanceof BIFFResourceEntry) {
            output = FileManager.query(Profile.getRootFolders(), Profile.getOverrideFolderName(),
                                       wed[dayNight].getResourceEntry().getResourceName());
          } else {
            output = wed[dayNight].getResourceEntry().getActualPath();
          }
          int optionIndex = allowCancel ? 1 : 0;
          int optionType = allowCancel ? JOptionPane.YES_NO_CANCEL_OPTION : JOptionPane.YES_NO_OPTION;
          String options[][] = { {"Save changes", "Discard changes"}, {"Save changes", "Discard changes", "Cancel"} };
          int result = JOptionPane.showOptionDialog(parent, "Save changes to " + output + '?', "Resource changed",
                                                    optionType, JOptionPane.WARNING_MESSAGE, null,
                                                    options[optionIndex], options[optionIndex][0]);
          if (result == 0) {
            ResourceFactory.saveResource(wed[dayNight], parent);
          }
          if (result != 2) {
            wed[dayNight].setStructChanged(false);
          }
          bRet = (result != 2);
        } else {
          bRet = true;
        }
        if (bRet && wed[dayNight].getViewer() != null) {
          wed[dayNight].getViewer().close();
        }
      }
      return bRet;
    }

    /**
     * Reloads the specified WED resource.
     * @param dayNight The WED resource to load.
     */
    public void reloadWed(int dayNight)
    {
      dayNight = (dayNight == ViewerConstants.AREA_NIGHT) ? ViewerConstants.AREA_NIGHT : ViewerConstants.AREA_DAY;
      ResourceRef wedRef = (ResourceRef)are.getAttribute(AreResource.ARE_WED_RESOURCE);
      if (wedRef != null) {
        if (dayNight == ViewerConstants.AREA_DAY) {
          wed[dayNight] = null;
          try {
            if (!wedRef.isEmpty()) {
              final WedResource res = new WedResource(ResourceFactory.getResourceEntry(wedRef.getResourceName()));
              wed[dayNight] = res;
              wedItem[dayNight] = new IconLayerItem(res, res.getName());
              wedItem[dayNight].setVisible(false);
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        } else {
          wed[dayNight] = wed[ViewerConstants.AREA_DAY];
          // getting extended night map
          if (hasExtendedNight && !wedRef.isEmpty()) {
            final String wedName = wedRef.getResourceName();
            int pos = wedName.lastIndexOf('.');
            if (pos > 0) {
              String wedNameNight = wedName.substring(0, pos) + "N" + wedName.substring(pos);
              try {
                wed[dayNight] = new WedResource(ResourceFactory.getResourceEntry(wedNameNight));
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
          }

          if (wed[dayNight] != null) {
            wedItem[dayNight] = new IconLayerItem(wed[dayNight], wed[dayNight].getName());
            wedItem[dayNight].setVisible(false);
          }
        }
      }
    }

    /**
     * Reloads the search/light/height maps associated with the area.
     */
    public void reloadMiniMaps()
    {
      if (wed[ViewerConstants.AREA_DAY] != null) {
        String mapName = wed[ViewerConstants.AREA_DAY].getName().toUpperCase(Locale.ENGLISH);
        final int pos = mapName.lastIndexOf('.');
        if (pos >= 0) {
          mapName = mapName.substring(0, pos);
        }

        // loading search map
        mapSearch = loadMap(mapName + "SR.BMP", null);
        // loading height map
        mapHeight = loadMap(mapName + "HT.BMP", null);
        // loading light map(s)
        mapLight[0] = loadMap(mapName + "LM.BMP", null);
        mapLight[1] = hasExtendedNight ? loadMap(mapName + "LN.BMP", mapLight[0]) : mapLight[0];
      }
    }

    /** Returns the pseudo layer item for the AreResource structure. */
    public AbstractLayerItem getAreItem()
    {
      return areItem;
    }

    /** Returns the pseudo layer item for the WedResource structure of the selected day time. */
    public AbstractLayerItem getWedItem(int dayNight)
    {
      if (dayNight == ViewerConstants.AREA_NIGHT) {
        return wedItem[ViewerConstants.AREA_NIGHT];
      } else {
        return wedItem[ViewerConstants.AREA_DAY];
      }
    }

    /** Returns the pseudo layer item for the ARE's song structure. */
    public AbstractLayerItem getSongItem()
    {
      return songItem;
    }

    /** Returns the pseudo layer item for the ARE's rest encounter structure. */
    public AbstractLayerItem getRestItem()
    {
      return restItem;
    }

    /** Returns whether the current map supports day/twilight/night settings. */
    public boolean hasDayNight()
    {
      return hasDayNight;
    }

    /** Returns true if the current map has separate WEDs for day/night. */
    public boolean hasExtendedNight()
    {
      return hasExtendedNight;
    }

    /**
     * Returns the alpha blending strength of overlays in the range [0, 255]
     * where 0 is fully opaque and 255 is fully transparent.
     * Default for classic BG1: 0 (fully opaque); everything else: 128 (50% transparency) */
    public int getOverlayTransparency()
    {
      return overlayTransparency;
    }

    private static GraphicsResource loadMap(String mapName, GraphicsResource def)
    {
      try {
        return new GraphicsResource(ResourceFactory.getResourceEntry(mapName));
      } catch (Exception e) {
        return def;
      }
    }

    private void init()
    {
      // fetching important options
      final Flag flags = (Flag)are.getAttribute(AreResource.ARE_LOCATION);
      if (flags != null) {
        if (Profile.getEngine() == Profile.Engine.PST || Profile.getGame() == Profile.Game.PSTEE) {
          hasDayNight = flags.isFlagSet(10);
          hasExtendedNight = false;
        } else {
          hasDayNight = flags.isFlagSet(1);
          hasExtendedNight = flags.isFlagSet(6);
        }
      }
      if (Profile.isEnhancedEdition()) {
        overlayTransparency = ((IsNumeric)are.getAttribute(AreResource.ARE_OVERLAY_TRANSPARENCY)).getValue();
        if (overlayTransparency == 0)
          overlayTransparency = 128;
      } else {
        overlayTransparency = (Profile.getEngine() == Profile.Engine.BG1) ? 0 : 128;
      }

      // initializing pseudo layer items
      areItem = new IconLayerItem(are, are.getName());
      areItem.setVisible(false);

      final Song song = (Song)are.getAttribute(Song.ARE_SONGS);
      if (song != null) {
        songItem = new IconLayerItem(song, "");
        songItem.setVisible(false);
      }

      final RestSpawn rest = (RestSpawn)are.getAttribute(RestSpawn.ARE_RESTSPAWN);
      if (rest != null) {
        restItem = new IconLayerItem(rest, "");
      }

      // getting associated WED resources
      reloadWed(ViewerConstants.AREA_DAY);
      reloadWed(ViewerConstants.AREA_NIGHT);
      reloadMiniMaps();
    }
  }


  /** Defines a panel providing controls for setting day times (either by hour or by general day time). */
  private static final class DayTimePanel extends JPanel implements ActionListener, ChangeListener
  {
    private final List<ChangeListener> listeners = new ArrayList<>();
    private final JRadioButton[] rbDayTime = new JRadioButton[3];
    private final ButtonPopupWindow bpwDayTime;

    private JSlider sHours;

    /** Creates and returns a string describing the time for display on the parent button. */
    public static String getButtonText(int hour)
    {
      final String[] dayTime = new String[]{"Day", "Twilight", "Night"};
      String desc = dayTime[ViewerConstants.getDayTime(hour)];
      return String.format("Time (%02d:00 - %s)", hour, desc);
    }


    public DayTimePanel(ButtonPopupWindow bpw, int hour)
    {
      super(new BorderLayout());
      bpwDayTime = bpw;
      init(hour);
    }

    public int getHour()
    {
      return sHours.getValue();
    }

    public void setHour(int hour)
    {
      while (hour < 0) { hour += 24; }
      hour %= 24;
      if (hour != sHours.getValue()) {
        sHours.setValue(hour);
        rbDayTime[ViewerConstants.getDayTime(hour)].setSelected(true);
        fireStateChanged();
      }
    }

    /**
     * Adds a ChangeListener to the slider.
     * @param l the ChangeListener to add
     */
    public void addChangeListener(ChangeListener l)
    {
      if (l != null) {
        if (!listeners.contains(l)) {
          listeners.add(l);
        }
      }
    }

    /**
     * Removes a ChangeListener from the slider.
     * @param l the ChangeListener to remove
     */
    @SuppressWarnings("unused")
    public void removeChangeListener(ChangeListener l)
    {
      if (l != null) {
        int index = listeners.indexOf(l);
        if (index >= 0) {
          listeners.remove(index);
        }
      }
    }

    /**
     * Returns an array of all the ChangeListeners added to this JSlider with addChangeListener().
     * @return All of the ChangeListeners added or an empty array if no listeners have been added.
     */
    @SuppressWarnings("unused")
    public ChangeListener[] getChangeListeners()
    {
      return listeners.toArray(new ChangeListener[listeners.size()]);
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
      for (int i = 0; i < rbDayTime.length; i++) {
        if (event.getSource() == rbDayTime[i]) {
          int hour = ViewerConstants.getHourOf(i);
          if (hour != sHours.getValue()) {
            sHours.setValue(hour);
          }
          break;
        }
      }
    }

    @Override
    public void stateChanged(ChangeEvent event)
    {
      if (event.getSource() == sHours) {
        if (!sHours.getValueIsAdjusting()) {
          int dt = ViewerConstants.getDayTime(sHours.getValue());
          if (!rbDayTime[dt].isSelected()) {
            rbDayTime[dt].setSelected(true);
          }
          updateButton();
          fireStateChanged();
        }
      }
    }

    /** Fires a stateChanged event for all registered listeners. */
    private void fireStateChanged()
    {
      ChangeEvent event = new ChangeEvent(this);
      for (final ChangeListener l : listeners) {
        l.stateChanged(event);
      }
    }

    /** Updates the text of the parent button. */
    private void updateButton()
    {
      if (bpwDayTime != null) {
        bpwDayTime.setText(getButtonText(sHours.getValue()));
      }
    }

    private void init(int hour)
    {
      while (hour < 0) { hour += 24; }
      hour %= 24;
      int dayTime = ViewerConstants.getDayTime(hour);

      ButtonGroup bg = new ButtonGroup();
      String s = String.format("Day (%02d:00)", ViewerConstants.getHourOf(ViewerConstants.LIGHTING_DAY));
      rbDayTime[ViewerConstants.LIGHTING_DAY] = new JRadioButton(s, (dayTime == ViewerConstants.LIGHTING_DAY));
      rbDayTime[ViewerConstants.LIGHTING_DAY].addActionListener(this);
      bg.add(rbDayTime[ViewerConstants.LIGHTING_DAY]);
      s = String.format("Twilight (%02d:00)", ViewerConstants.getHourOf(ViewerConstants.LIGHTING_TWILIGHT));
      rbDayTime[ViewerConstants.LIGHTING_TWILIGHT] = new JRadioButton(s, (dayTime == ViewerConstants.LIGHTING_TWILIGHT));
      rbDayTime[ViewerConstants.LIGHTING_TWILIGHT].addActionListener(this);
      bg.add(rbDayTime[ViewerConstants.LIGHTING_TWILIGHT]);
      s = String.format("Night (%02d:00)", ViewerConstants.getHourOf(ViewerConstants.LIGHTING_NIGHT));
      rbDayTime[ViewerConstants.LIGHTING_NIGHT] = new JRadioButton(s, (dayTime == ViewerConstants.LIGHTING_NIGHT));
      rbDayTime[ViewerConstants.LIGHTING_NIGHT].addActionListener(this);
      bg.add(rbDayTime[ViewerConstants.LIGHTING_NIGHT]);

      final Hashtable<Integer, JLabel> table = new Hashtable<>();
      for (int i = 0; i < 24; i += 4) {
        table.put(i, new JLabel(String.format("%02d:00", i)));
      }
      sHours = new JSlider(0, 23, hour);
      sHours.addChangeListener(this);
      sHours.setSnapToTicks(true);
      sHours.setLabelTable(table);
      sHours.setPaintLabels(true);
      sHours.setMinorTickSpacing(1);
      sHours.setMajorTickSpacing(4);
      sHours.setPaintTicks(true);
      sHours.setPaintTrack(true);
      Dimension dim = sHours.getPreferredSize();
      sHours.setPreferredSize(new Dimension((dim.width*3)/2, dim.height));

      GridBagConstraints c = new GridBagConstraints();
      JPanel pHours = new JPanel(new GridBagLayout());
      pHours.setBorder(BorderFactory.createTitledBorder("By hour: "));
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0);
      pHours.add(sHours, c);

      JPanel pTime = new JPanel(new GridBagLayout());
      pTime.setBorder(BorderFactory.createTitledBorder("By lighting condition: "));
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.NONE, new Insets(4, 8, 4, 0), 0, 0);
      pTime.add(rbDayTime[ViewerConstants.LIGHTING_DAY], c);
      c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.NONE, new Insets(4, 8, 4, 0), 0, 0);
      pTime.add(rbDayTime[ViewerConstants.LIGHTING_TWILIGHT], c);
      c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.NONE, new Insets(4, 8, 4, 8), 0, 0);
      pTime.add(rbDayTime[ViewerConstants.LIGHTING_NIGHT], c);

      JPanel pMain = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 4), 0, 0);
      pMain.add(pHours, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0);
      pMain.add(pTime, c);

      add(pMain, BorderLayout.CENTER);

      updateButton();
    }
  }


  /** Adds support for visual components in JTree instances. */
  private static class ComponentTreeCellRenderer extends DefaultTreeCellRenderer
  {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                  boolean expanded, boolean leaf, int row,
                                                  boolean hasFocus) {
      if (value instanceof DefaultMutableTreeNode) {
        value = ((DefaultMutableTreeNode)value).getUserObject();
      }
      if (value instanceof Component) {
        return (Component)value;
      }
      return new JLabel((value != null) ? value.toString() : "");
    }
  }


  /** Adds support for editable visual components in JTree instances. */
  private static class ComponentTreeCellEditor extends DefaultTreeCellEditor
  {
    public ComponentTreeCellEditor(JTree tree, ComponentTreeCellRenderer renderer)
    {
      super(tree, renderer);
    }

    @Override
    public boolean isCellEditable(EventObject event)
    {
      return true;
    }

    @Override
    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected,
                                                boolean expanded, boolean leaf, int row)
    {
      return renderer.getTreeCellRendererComponent(tree, value, isSelected, expanded, leaf, row, true);
    }
  }
}
