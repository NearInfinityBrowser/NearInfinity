package infinity.resource.are.viewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.ProgressMonitor;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import infinity.NearInfinity;
import infinity.datatype.Flag;
import infinity.datatype.ResourceRef;
import infinity.gui.Center;
import infinity.gui.ChildFrame;
import infinity.gui.RenderCanvas;
import infinity.gui.ViewFrame;
import infinity.gui.WindowBlocker;
import infinity.gui.layeritem.AbstractLayerItem;
import infinity.gui.layeritem.AnimatedLayerItem;
import infinity.gui.layeritem.IconLayerItem;
import infinity.gui.layeritem.LayerItemEvent;
import infinity.gui.layeritem.LayerItemListener;
import infinity.icon.Icons;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.are.AreResource;
import infinity.resource.key.BIFFResourceEntry;
import infinity.resource.key.ResourceEntry;
import infinity.resource.wed.Overlay;
import infinity.resource.wed.WedResource;
import infinity.util.NIFile;

/**
 * The Area Viewer shows a selected map with its associated structures, such as actors, regions or
 * animations.
 * @author argent77
 */
public class AreaViewer extends ChildFrame
    implements ActionListener, MouseListener, MouseMotionListener, TilesetChangeListener,
               PropertyChangeListener, LayerItemListener, ComponentListener
{
  private static final String[] LabelZoomFactor = new String[]{"Auto-fit", "25%", "33%", "50%", "100%", "200%", "300%", "400%"};
  private static final double[] ItemZoomFactor = new double[]{0.0, 0.25, 1.0/3.0, 0.5, 1.0, 2.0, 3.0, 4.0};
  private static final int ZoomFactorIndexAuto = 0;       // points to the auto-fit zoom factor
  private static final int ZoomFactorIndexDefault = 4;    // points to the default zoom factor (1x)
  private static final String LabelInfoX = "Position X:";
  private static final String LabelInfoY = "Position Y:";
  private static final String LabelDrawClosed = "Draw closed";
  private static final String LabelDrawOverlays = "Enable overlays";
  private static final String LabelAnimateOverlays = "Animate overlays";
  private static final String LabelDrawGrid = "Show grid";

  private static boolean DrawClosed = false;
  private static boolean DrawOverlays = true;
  private static boolean DrawGrid = false;
  private static int LayerFlags = 0;    // bitmask of selected layers
  private static int showRealAnimations = ViewerConstants.ANIM_SHOW_NONE;
  private static int VisualState = ViewerConstants.LIGHTING_DAY;
  private static int ZoomLevel = ZoomFactorIndexDefault;

  private final Component parent;
  private final Map map;
  private final Point mapCoordinate = new Point();
  private final String windowTitle;
  private final JCheckBox[] cbLayerRealAnimation = new JCheckBox[2];
  private LayerManager layerManager;
  private TilesetRenderer rcCanvas;
  private JPanel pCanvas;
  private JScrollPane spCanvas;
//  private Rectangle vpCenterExtent;   // combines map center and viewport extent in one structure
  private JRadioButton[] rbVisualState;
  private JCheckBox cbDrawClosed, cbDrawOverlays, cbAnimateOverlays, cbDrawGrid;
  private JComboBox cbZoomLevel;
  private JCheckBox[] cbLayers;
  private JLabel lPosX, lPosY;
  private JTextArea taInfo;
  private boolean bMapDragging;
  private Point mapDraggingPosStart, mapDraggingScrollStart, mapDraggingPos;
  private Timer timerOverlays;
  private boolean bTimerActive;
  private JPopupMenu pmItems;
  private SwingWorker<Void, Void> workerInitGui, workerLoadMap;
  private ProgressMonitor progress;
  private int pmCur, pmMax;
  private WindowBlocker blocker;


  /**
   * Checks whether the specified ARE resource can be displayed with the area viewer.
   * @param are The ARE resource to check
   * @return <code>true</code> if area is viewable, <code>false</code> otherwise.
   */
  public static boolean IsValid(AreResource are)
  {
    if (are != null) {
      ResourceRef wedRef = (ResourceRef)are.getAttribute("WED resource");
      ResourceEntry wedEntry = ResourceFactory.getInstance().getResourceEntry(wedRef.getResourceName());
      if (wedEntry != null) {
        try {
          WedResource wedFile = new WedResource(wedEntry);
          Overlay overlay = (Overlay)wedFile.getAttribute("Overlay 0");
          ResourceRef tisRef = (ResourceRef)overlay.getAttribute("Tileset");
          ResourceEntry tisEntry = ResourceFactory.getInstance().getResourceEntry(tisRef.getResourceName());
          if (tisEntry != null)
            return true;
        } catch (Exception e) {
          return false;
        }
      }
    }
    return false;
  }


  public AreaViewer(AreResource are)
  {
    this(NearInfinity.getInstance(), are);
  }

  public AreaViewer(Component parent, AreResource are)
  {
    super("", true);
    windowTitle = String.format("Area Viewer: %1$s", (are != null) ? are.getName() : "(Unknown)");
    initProgressMonitor(parent, "Initializing " + are.getName(), "Loading ARE resource...", 3, 0, 0);
    this.parent = parent;
    this.map = new Map(this, are);
    workerInitGui = new SwingWorker<Void, Void>() {
      @Override
      protected Void doInBackground() throws Exception {
        init();
        return null;
      }
    };
    workerInitGui.addPropertyChangeListener(this);
    workerInitGui.execute();
  }

  /**
   * Returns the tileset renderer for this viewer instance.
   * @return The currently used TilesetRenderer instance.
   */
  public TilesetRenderer getRenderer()
  {
    return rcCanvas;
  }

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() instanceof JRadioButton) {
      JRadioButton rb = (JRadioButton)event.getSource();
      int vsIndex = getVisualState(rb);
      if (vsIndex >= 0) {
        if (vsIndex != VisualState) {
          // loading map in a separate thread
          VisualState = vsIndex;
          blocker = new WindowBlocker(this);
          blocker.setBlocked(true);
          workerLoadMap = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception
            {
              setVisualState(VisualState);
              return null;
            }
          };
          workerLoadMap.addPropertyChangeListener(this);
          workerLoadMap.execute();
        }
      }
    } else if (event.getSource() instanceof JCheckBox) {
      JCheckBox cb = (JCheckBox)event.getSource();
      LayerManager.Layer layer = getLayerType(cb);
      if (layer != null) {
        showLayer(layer, cb.isSelected());
        // Special: "Ambient Range" depends on the state of "Ambient Sound"
        if (layer == LayerManager.Layer.Ambient) {
          JCheckBox cbRange = cbLayers[LayerManager.getLayerIndex(LayerManager.Layer.AmbientRange)];
          cbRange.setEnabled(cb.isSelected());
          showLayer(LayerManager.Layer.AmbientRange, cbRange.isEnabled() && cbRange.isSelected());
        } else if (layer == LayerManager.Layer.Animation) {
          cbLayerRealAnimation[0].setEnabled(cb.isEnabled() && cb.isSelected());
          cbLayerRealAnimation[1].setEnabled(cb.isEnabled() && cb.isSelected());
          int state = ViewerConstants.ANIM_SHOW_NONE;
          if (cbLayerRealAnimation[0].isSelected()) {
            state = ViewerConstants.ANIM_SHOW_STILL;
          } else if (cbLayerRealAnimation[1].isSelected()) {
            state = ViewerConstants.ANIM_SHOW_ANIMATED;
          }
          showRealAnimations(state);
        }
      } else if (cb == cbLayerRealAnimation[0]) {
        if (cbLayerRealAnimation[0].isSelected()) {
          cbLayerRealAnimation[1].setSelected(false);
        }
        showRealAnimations(cbLayerRealAnimation[0].isSelected() ? ViewerConstants.ANIM_SHOW_STILL : ViewerConstants.ANIM_SHOW_NONE);
      } else if (cb == cbLayerRealAnimation[1]) {
        if (cbLayerRealAnimation[1].isSelected()) {
          cbLayerRealAnimation[0].setSelected(false);
        }
        showRealAnimations(cbLayerRealAnimation[1].isSelected() ? ViewerConstants.ANIM_SHOW_ANIMATED : ViewerConstants.ANIM_SHOW_NONE);
      } else if (cb == cbDrawClosed) {
        WindowBlocker.blockWindow(this, true);
        try {
          setDrawClosed(cb.isSelected());
        } finally {
          WindowBlocker.blockWindow(this, false);
        }
      } else if (cb == cbDrawGrid) {
        WindowBlocker.blockWindow(this, true);
        try {
          setDrawGrid(cb.isSelected());
        } finally {
          WindowBlocker.blockWindow(this, false);
        }
      } else if (cb == cbDrawOverlays) {
        WindowBlocker.blockWindow(this, true);
        try {
          setDrawOverlays(cb.isSelected());
        } finally {
          WindowBlocker.blockWindow(this, false);
        }
      } else if (cb == cbAnimateOverlays) {
        WindowBlocker.blockWindow(this, true);
        try {
          setAnimateOverlays(cb.isSelected());
        } finally {
          WindowBlocker.blockWindow(this, false);
        }
      }
    } else if (event.getSource() == cbZoomLevel) {
      WindowBlocker.blockWindow(this, true);
      try {
        try {
          setZoomLevel(cbZoomLevel.getSelectedIndex());
        } catch (OutOfMemoryError e) {
          cbZoomLevel.hidePopup();
          WindowBlocker.blockWindow(this, false);
          String msg = "Not enough memory to set selected zoom level.\n"
                       + "(Note: It is highly recommended to close and reopen the area viewer.)";
          JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
          cbZoomLevel.setSelectedIndex(ZoomLevel);
          setZoomLevel(ZoomLevel);
        }
      } finally {
        WindowBlocker.blockWindow(this, false);
      }
    } else if (event.getSource() == timerOverlays) {
      advanceOverlayAnimation();
    } else if (event.getSource() instanceof AbstractLayerItem) {
      AbstractLayerItem item = (AbstractLayerItem)event.getSource();
      item.showViewable();
    } else if (event.getSource() instanceof LayerMenuItem) {
      LayerMenuItem lmi = (LayerMenuItem)event.getSource();
      AbstractLayerItem item = lmi.getLayerItem();
      showTable(item);
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

//--------------------- Begin Interface TilesetChangeListener ---------------------

  @Override
  public void tilesetChanged(TilesetChangeEvent event)
  {
    if (event.getSource() == rcCanvas) {
      if (event.hasChangedMap()) {
        updateLayerItems();
        showRealAnimations(showRealAnimations);
      } else if (event.hasChangedDoorState()) {
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
        setInfoText(item.getMessage());
        showMapCoordinates(item.getMapLocation());
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
    if (event.getSource() == spCanvas || event.getSource() == rcCanvas) {
      if (isAutoZoom()) {
        setZoomLevel(ZoomFactorIndexAuto);
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

//--------------------- Begin Class ChildFrame ---------------------

  @Override
  protected boolean windowClosing(boolean forced) throws Exception
  {
    if (!map.closeWed(Map.MAP_DAY, true)) {
      return false;
    }
    if (!map.closeWed(Map.MAP_NIGHT, true)) {
      return false;
    }
    if (map != null) {
      map.clear();
    }
    if (rcCanvas != null) {
      removeLayerItems();
      rcCanvas.clear();
      rcCanvas.setImage(null);
    }
    if (layerManager != null) {
      layerManager.clear();
      layerManager = null;
    }
    dispose();
    System.gc();
    return super.windowClosing(forced);
  }

//--------------------- End Class ChildFrame ---------------------


  private static GridBagConstraints setGBC(GridBagConstraints gbc, int gridX, int gridY,
                                           int gridWidth, int gridHeight, double weightX, double weightY,
                                           int anchor, int fill, Insets insets, int iPadX, int iPadY)
  {
    if (gbc == null) gbc = new GridBagConstraints();

    gbc.gridx = gridX;
    gbc.gridy = gridY;
    gbc.gridwidth = gridWidth;
    gbc.gridheight = gridHeight;
    gbc.weightx = weightX;
    gbc.weighty = weightY;
    gbc.anchor = anchor;
    gbc.fill = fill;
    gbc.insets = (insets == null) ? new Insets(0, 0, 0, 0) : insets;
    gbc.ipadx = iPadX;
    gbc.ipady = iPadY;

    return gbc;
  }


  // initialize GUI and structures
  private void init()
  {
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
    rcCanvas = new TilesetRenderer();
    rcCanvas.addComponentListener(this);
    rcCanvas.addMouseListener(this);
    rcCanvas.addMouseMotionListener(this);
    rcCanvas.addChangeListener(this);
    rcCanvas.setHorizontalAlignment(RenderCanvas.CENTER);
    rcCanvas.setVerticalAlignment(RenderCanvas.CENTER);
    rcCanvas.setLocation(0, 0);
    rcCanvas.setLayout(null);
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
               GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    pCanvas.add(rcCanvas, c);
    spCanvas = new JScrollPane(pCanvas);
    spCanvas.addComponentListener(this);
    spCanvas.getVerticalScrollBar().setUnitIncrement(16);
    spCanvas.getHorizontalScrollBar().setUnitIncrement(16);
    JPanel pView = new JPanel(new GridBagLayout());
    c = setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
               GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    pView.add(spCanvas, c);

    // Creating right side bar
    // Creating Visual State area
    int lightingModes = TilesetRenderer.getLightingModesCount();
    ButtonGroup bgVisualState = new ButtonGroup();
    rbVisualState = new JRadioButton[lightingModes];
    for (int i = 0; i < lightingModes; i++) {
      rbVisualState[i] = new JRadioButton(TilesetRenderer.LabelVisualStates[i]);
      rbVisualState[i].addActionListener(this);
      bgVisualState.add(rbVisualState[i]);
    }
    cbDrawClosed = new JCheckBox(LabelDrawClosed);
    cbDrawClosed.setToolTipText("Draw opened or closed states of doors");
    cbDrawClosed.addActionListener(this);

    cbDrawGrid = new JCheckBox(LabelDrawGrid);
    cbDrawGrid.addActionListener(this);

    cbDrawOverlays = new JCheckBox(LabelDrawOverlays);
    cbDrawOverlays.addActionListener(this);

    String msgAnimate = "Warning: The area viewer may become less responsive when activating this feature.";
    cbAnimateOverlays = new JCheckBox(LabelAnimateOverlays);
    cbAnimateOverlays.setToolTipText(msgAnimate);
    cbAnimateOverlays.addActionListener(this);

    JLabel lZoomLevel = new JLabel("Zoom map:");
    cbZoomLevel = new JComboBox(LabelZoomFactor);
    cbZoomLevel.setSelectedIndex(ZoomLevel);
    cbZoomLevel.addActionListener(this);
    JPanel pZoom = new JPanel(new GridBagLayout());
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pZoom.add(lZoomLevel, c);
    c = setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    pZoom.add(cbZoomLevel, c);

    p = new JPanel(new GridBagLayout());
    for (int i = 0; i < lightingModes; i++) {
      c = setGBC(c, 0, i, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                 GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
      p.add(rbVisualState[i], c);
    }
    c = setGBC(c, 0, lightingModes, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    p.add(cbDrawClosed, c);
    c = setGBC(c, 0, lightingModes + 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.NONE, new Insets(4, 0, 0, 0), 0, 0);
    p.add(cbDrawGrid, c);
    c = setGBC(c, 0, lightingModes + 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.NONE, new Insets(4, 0, 0, 0), 0, 0);
    p.add(cbDrawOverlays, c);
    c = setGBC(c, 0, lightingModes + 3, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.NONE, new Insets(0, 12, 0, 0), 0, 0);
    p.add(cbAnimateOverlays, c);
    c = setGBC(c, 0, lightingModes + 4, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.NONE, new Insets(4, 4, 0, 0), 0, 0);
    p.add(pZoom, c);

    JPanel pVisualState = new JPanel(new GridBagLayout());
    pVisualState.setBorder(BorderFactory.createTitledBorder("Visual State: "));
    c = setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.BOTH, new Insets(0, 4, 4, 4), 0, 0);
    pVisualState.add(p, c);


    // Creating Layers area
    cbLayers = new JCheckBox[LayerManager.getLayerTypeCount()];
    p = new JPanel(new GridBagLayout());
    for (int idx = 0, i = 0; i < LayerManager.getLayerTypeCount(); i++, idx++) {
      LayerManager.Layer layer = LayerManager.getLayerType(i);
      cbLayers[i] = new JCheckBox(LayerManager.getLayerLabel(layer));
      cbLayers[i].setEnabled(false);
      cbLayers[i].addActionListener(this);
      c = setGBC(c, 0, idx, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                 GridBagConstraints.NONE, new Insets(0, (layer != LayerManager.Layer.AmbientRange) ? 0 : 12, 0, 0), 0, 0);
      p.add(cbLayers[i], c);
      if (i == LayerManager.getLayerIndex(LayerManager.Layer.Animation)) {
        cbLayerRealAnimation[0] = new JCheckBox("Show actual animations");
        cbLayerRealAnimation[0].setEnabled(false);
        cbLayerRealAnimation[0].addActionListener(this);
        cbLayerRealAnimation[1] = new JCheckBox("Animate actual animations");
        cbLayerRealAnimation[1].setEnabled(false);
        cbLayerRealAnimation[1].setToolTipText(msgAnimate);
        cbLayerRealAnimation[1].addActionListener(this);
        idx++;
        c = setGBC(c, 0, idx, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
            GridBagConstraints.NONE, new Insets(0, 12, 0, 0), 0, 0);
        p.add(cbLayerRealAnimation[0], c);
        idx++;
        c = setGBC(c, 0, idx, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                   GridBagConstraints.NONE, new Insets(0, 12, 0, 0), 0, 0);
        p.add(cbLayerRealAnimation[1], c);
      }
    }

    JPanel pLayers = new JPanel(new GridBagLayout());
    pLayers.setBorder(BorderFactory.createTitledBorder("Layers: "));
    c = setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.BOTH, new Insets(0, 4, 0, 4), 0, 0);
    pLayers.add(p, c);


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
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    p.add(lPosXLabel, c);
    c = setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    p.add(lPosX, c);
    c = setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.NONE, new Insets(4, 0, 0, 0), 0, 0);
    p.add(lPosYLabel, c);
    c = setGBC(c, 1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 8, 0, 0), 0, 0);
    p.add(lPosY, c);
    c = setGBC(c, 0, 2, 2, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.BOTH, new Insets(4, 0, 0, 0), 0, 0);
    p.add(taInfo, c);

    JPanel pInfoBox = new JPanel(new GridBagLayout());
    pInfoBox.setBorder(BorderFactory.createTitledBorder("Information: "));
    c = setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.BOTH, new Insets(0, 4, 0, 4), 0, 0);
    pInfoBox.add(p, c);

    // Assembling right side bar
    JPanel pSideBar = new JPanel(new GridBagLayout());
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 4), 0, 0);
    pSideBar.add(pVisualState, c);
    c = setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(8, 4, 0, 4), 0, 0);
    pSideBar.add(pLayers, c);
    c = setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(8, 4, 0, 4), 0, 0);
    pSideBar.add(pInfoBox, c);
    c = setGBC(c, 0, 3, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.BOTH, new Insets(4, 0, 0, 0), 0, 0);
    pSideBar.add(new JPanel(), c);

    // Putting all together
    JPanel pMain = new JPanel(new GridBagLayout());
    c = setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    pMain.add(pView, c);
    c = setGBC(c, 1, 0, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0);
    pMain.add(pSideBar, c);

    // setting frame rate for overlay animations to 5 fps (in-game frame rate: 7.5 fps)
    timerOverlays = new Timer(1000/5, this);

    advanceProgressMonitor("Initializing map...");
    setLayout(new BorderLayout());
    add(pMain, BorderLayout.CENTER);
    pack();

    // setting window size and state
    setSize(NearInfinity.getInstance().getSize());
    Center.center(this, NearInfinity.getInstance().getBounds());
    setExtendedState(NearInfinity.getInstance().getExtendedState());

    try {
      initGuiSettings();
    } catch (OutOfMemoryError e) {
      JOptionPane.showMessageDialog(this, "Not enough memory to load area!", "Error", JOptionPane.ERROR_MESSAGE);
      throw e;
    }
    rcCanvas.requestFocusInWindow();    // put focus on a safe component
    advanceProgressMonitor("Ready!");

    updateWindowTitle();
    setVisible(true);
  }


  // Updates the map coordinates pointed to by the current cursor position
  private void showMapCoordinates(Point coord)
  {
    if (coord != null) {
      coord.x = (int)((double)coord.x / getZoomLevel());
      coord.y = (int)((double)coord.y / getZoomLevel());
      if (coord.x != mapCoordinate.x) {
        mapCoordinate.x = coord.x;
        lPosX.setText(Integer.toString(mapCoordinate.x));
      }
      if (coord.y != mapCoordinate.y) {
        mapCoordinate.y = coord.y;
        lPosY.setText(Integer.toString(mapCoordinate.y));
      }
    }
  }

  // Shows a description in the info box
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

  // Sets the state of all GUI components and their associated actions
  private void initGuiSettings()
  {
    // initializing visual state of the map
    if (!map.hasDayNight()) {
      VisualState = ViewerConstants.LIGHTING_DAY;
    }
    for (int i = 0; i < rbVisualState.length; i++) {
      if (i != ViewerConstants.LIGHTING_DAY) {
        rbVisualState[i].setEnabled(map.hasDayNight());
      }
    }
    rbVisualState[VisualState].setSelected(true);
    setVisualState(VisualState);

    // initializing closed state of doors
    cbDrawClosed.setSelected(DrawClosed);
    cbDrawClosed.setEnabled(rcCanvas.hasDoors());
    if (rcCanvas.hasDoors()) {
      setDrawClosed(DrawClosed);
    }

    // initializing grid
    cbDrawGrid.setSelected(DrawGrid);
    setDrawGrid(DrawGrid);

    // initializing overlays
    cbDrawOverlays.setSelected(DrawOverlays);
    cbDrawOverlays.setEnabled(rcCanvas.hasOverlays());
    cbAnimateOverlays.setEnabled(rcCanvas.hasOverlays());
    if (rcCanvas.hasOverlays()) {
      setDrawOverlays(DrawOverlays);
      setAnimateOverlays(cbAnimateOverlays.isSelected());
    }

    // initializing zoom level
    cbZoomLevel.setSelectedIndex(ZoomLevel);

    // initializing layers
    layerManager = new LayerManager(getCurrentAre(), getCurrentWed(), this);
    layerManager.setDoorState(DrawClosed ? ViewerConstants.DOOR_CLOSED : ViewerConstants.DOOR_OPEN);
    addLayerItems();
    for (int i = 0; i < LayerManager.getLayerTypeCount(); i++) {
      LayerManager.Layer layer = LayerManager.getLayerType(i);
      int bit = 1 << i;
      boolean isChecked = (LayerFlags & bit) != 0;
      int count = layerManager.getLayerObjectCount(layer);
      if (count > 0) {
        cbLayers[i].setToolTipText(layerManager.getLayerAvailability(layer));
      }
      if (layer == LayerManager.Layer.AmbientRange) {
        // state of ambient sound range depends on ambient sound layer
        int bit2 = 1 << LayerManager.getLayerIndex(LayerManager.Layer.Ambient);
        cbLayers[i].setEnabled((LayerFlags & bit2) != 0 && count > 0);
      } else {
        cbLayers[i].setEnabled(count > 0);
      }
      cbLayers[i].setSelected(isChecked);
      updateLayerItems(layer);
      showLayer(LayerManager.getLayerType(i), cbLayers[i].isSelected());
    }

    // initializing background animation display
    // Disabling animated frames for performance and safety reasons
    if (showRealAnimations == ViewerConstants.ANIM_SHOW_ANIMATED) {
      showRealAnimations = ViewerConstants.ANIM_SHOW_STILL;
    }
    int idx = LayerManager.getLayerIndex(LayerManager.Layer.Animation);
    cbLayerRealAnimation[0].setEnabled(cbLayers[idx].isEnabled() && cbLayers[idx].isSelected());
    cbLayerRealAnimation[0].setSelected(showRealAnimations == ViewerConstants.ANIM_SHOW_STILL);
    cbLayerRealAnimation[1].setEnabled(cbLayers[idx].isEnabled() && cbLayers[idx].isSelected());
    cbLayerRealAnimation[1].setSelected(false);
    showRealAnimations(showRealAnimations);
  }

  // Updates the window title
  private void updateWindowTitle()
  {
    int zoom = (int)(getZoomLevel()*100.0);

    String dayNight;
    switch (getCurrentVisualState()) {
      case ViewerConstants.LIGHTING_TWILIGHT:
        dayNight = "twilight";
        break;
      case ViewerConstants.LIGHTING_NIGHT:
        dayNight = "night";
        break;
      default:
        dayNight = "day";
    }

    String doorState = isDrawingClosed() ? "Doors: closed" : "Doors: open";

    String overlayState;
    if (isDrawingOverlays() && !isAnimatedOverlays()) {
      overlayState = "Overlays: enabled";
    } else if (isDrawingOverlays() && isAnimatedOverlays()) {
      overlayState = "Overlays: animated";
    } else {
      overlayState = "Overlays: disabled";
    }

    String gridState = isDrawingGrid() ? "Grid: enabled" : "Grid: disabled";

    setTitle(String.format("%1$s  (Time: %2$s, %3$s, %4$s, %5$s, Zoom: %6$d%%)",
                           windowTitle, dayNight, doorState, overlayState, gridState, zoom));
  }

  // Returns the currently selected ARE resource
  private AreResource getCurrentAre()
  {
    if (map != null) {
      return map.getAre();
    } else {
      return null;
    }
  }

  // Returns the currently selected WED resource (day/night)
  private WedResource getCurrentWed()
  {
    if (map != null) {
      return map.getWed(getCurrentWedIndex());
    }
    return null;
  }

  // Returns the currently selected WED resource (day/night)
  private int getCurrentWedIndex()
  {
    if (map != null) {
      for (int i = 0; i < rbVisualState.length; i++) {
        if (rbVisualState[i].isSelected()) {
          int type = (i == ViewerConstants.LIGHTING_NIGHT) ? Map.MAP_NIGHT : Map.MAP_DAY;
          return type;
        }
      }
    }
    return Map.MAP_DAY;
  }

  // Returns the identifier of the specified radio button, or -1 on error
  private int getVisualState(JRadioButton button)
  {
    if (button != null) {
      for (int i = 0; i < rbVisualState.length; i++) {
        if (button == rbVisualState[i]) {
          return i;
        }
      }
    }
    return -1;
  }

  // Returns the currently selected visual state (day/twilight/night)
  private int getCurrentVisualState()
  {
    return VisualState;
  }

  // Set the lighting condition of the current map (day/twilight/night)
  private void setVisualState(int index)
  {
    if (index >= 0 && index < TilesetRenderer.getLightingModesCount()) {
      switch (index) {
        case ViewerConstants.LIGHTING_DAY:
          if (!isProgressMonitorActive() && map.getWed(Map.MAP_DAY) != rcCanvas.getWed()) {
            initProgressMonitor(this, "Loading tileset...", null, 1, 0, 0);
          }
          if (rcCanvas.getWed() != map.getWed(Map.MAP_DAY)) {
            rcCanvas.loadMap(map.getWed(Map.MAP_DAY));
            reloadWedLayers();
          }
          rcCanvas.setLighting(index);
          break;
        case ViewerConstants.LIGHTING_TWILIGHT:
          if (!isProgressMonitorActive() && map.getWed(Map.MAP_DAY) != rcCanvas.getWed()) {
            initProgressMonitor(this, "Loading tileset...", null, 1, 0, 0);
          }
          if (rcCanvas.getWed() != map.getWed(Map.MAP_DAY)) {
            rcCanvas.loadMap(map.getWed(Map.MAP_DAY));
            reloadWedLayers();
          }
          rcCanvas.setLighting(index);
          break;
        case ViewerConstants.LIGHTING_NIGHT:
          if (!isProgressMonitorActive() && map.getWed(Map.MAP_NIGHT) != rcCanvas.getWed()) {
            initProgressMonitor(this, "Loading tileset...", null, 1, 0, 0);
          }
          if (map.hasExtendedNight()) {
            if (rcCanvas.getWed() != map.getWed(Map.MAP_NIGHT)) {
              rcCanvas.loadMap(map.getWed(Map.MAP_NIGHT));
              reloadWedLayers();
            }
          } else {
            rcCanvas.setLighting(index);
          }
          break;
      }
      // updating current visual state
      if (index >= 0) {
        VisualState = index;
      }
      updateRealAnimations(index);
      updateWindowTitle();
    }
  }

  // Updates all available layer items
  private void reloadLayers()
  {
    reloadAreLayers();
    reloadWedLayers();
  }

  // Updates ARE-related layer items
  private void reloadAreLayers()
  {
    if (layerManager != null) {
      for (int i = 0; i < LayerManager.getLayerTypeCount(); i++) {
        LayerManager.Layer layer = LayerManager.getLayerType(i);
        if (layer != LayerManager.Layer.DoorPoly && layer != LayerManager.Layer.WallPoly) {
          removeLayerItems(layer);
          layerManager.setAreResource(getCurrentAre());
          updateLayerItems(layer);
          addLayerItems(layer);
          showLayer(layer, cbLayers[i].isSelected());
        }
      }
    }
  }

  // Updates WED-related layer items
  private void reloadWedLayers()
  {
    if (layerManager != null) {
      removeLayerItems(LayerManager.Layer.DoorPoly);
      removeLayerItems(LayerManager.Layer.WallPoly);
      layerManager.setWedResource(getCurrentWed());
      updateLayerItems(LayerManager.Layer.DoorPoly);
      addLayerItems(LayerManager.Layer.DoorPoly);
      showLayer(LayerManager.Layer.DoorPoly,
                cbLayers[LayerManager.getLayerIndex(LayerManager.Layer.DoorPoly)].isSelected());
      updateLayerItems(LayerManager.Layer.WallPoly);
      addLayerItems(LayerManager.Layer.WallPoly);
      showLayer(LayerManager.Layer.WallPoly,
                cbLayers[LayerManager.getLayerIndex(LayerManager.Layer.WallPoly)].isSelected());
    }
  }


  // Returns the identifier of the specified layer checkbox, or null on error
  private LayerManager.Layer getLayerType(JCheckBox cb)
  {
    if (cb != null) {
      for (int i = 0; i < cbLayers.length; i++) {
        if (cb == cbLayers[i]) {
          return LayerManager.Layer.values()[i];
        }
      }
    }
    return null;
  }

  // Sets the door state to layer items
  private void setLayerDoorState(boolean isClosed)
  {
    if (layerManager != null) {
      layerManager.setDoorState(isClosed ? ViewerConstants.DOOR_CLOSED : ViewerConstants.DOOR_OPEN);
    }
  }

  // Toggles between static animation icon and real animations
  private void showRealAnimations(int animState)
  {
    if (layerManager != null) {
      if (layerManager.isLayerVisible(LayerManager.Layer.Animation)) {
        if (animState < ViewerConstants.ANIM_SHOW_NONE) {
          animState = ViewerConstants.ANIM_SHOW_NONE;
        } else if (animState > ViewerConstants.ANIM_SHOW_ANIMATED) {
          animState = ViewerConstants.ANIM_SHOW_ANIMATED;
        }
        showRealAnimations = animState;
        List<LayerObject> list = layerManager.getLayerObjects(LayerManager.Layer.Animation);
        if (list != null) {
          for (int i = 0; i < list.size(); i++) {
            boolean isActive = ((LayerObjectAnimation)list.get(i)).isActiveAt(getCurrentVisualState());
            LayerObjectAnimation obj = (LayerObjectAnimation)list.get(i);
            obj.getLayerItem(ViewerConstants.ANIM_ICON).setVisible((showRealAnimations == ViewerConstants.ANIM_SHOW_NONE) && isActive);
            AnimatedLayerItem item = (AnimatedLayerItem)obj.getLayerItem(ViewerConstants.ANIM_REAL);
            item.setVisible((showRealAnimations != ViewerConstants.ANIM_SHOW_NONE) && isActive);
            if (showRealAnimations != ViewerConstants.ANIM_SHOW_NONE) {
              if (showRealAnimations == ViewerConstants.ANIM_SHOW_ANIMATED) {
                item.setAutoPlay(true);
                item.play();
              } else {
                item.stop();
              }
            }
          }
        }
      }
    }
  }

  private void updateRealAnimations(int visualState)
  {
    if (layerManager != null) {
      if (layerManager.isLayerVisible(LayerManager.Layer.Animation)) {
        List<LayerObject> list = layerManager.getLayerObjects(LayerManager.Layer.Animation);
        if (list != null) {
          for (int i = 0; i < list.size(); i++) {
            ((LayerObjectAnimation)list.get(i)).setLighting(visualState);
          }
        }
      }
    }
  }

  // Show/hide the specified layer
  private void showLayer(LayerManager.Layer layer, boolean visible)
  {
    if (layer != null && layerManager != null) {
      layerManager.setLayerVisible(layer, visible);
      // updating layer states
      int bit = 1 << LayerManager.getLayerIndex(layer);
      if (visible) {
        LayerFlags |= bit;
      } else {
        LayerFlags &= ~bit;
      }
    }
  }

  // Adds items of all available layers to the map canvas.
  private void addLayerItems()
  {
    for (int i = 0; i < LayerManager.LayerOrdered.length; i++) {
      addLayerItems(LayerManager.LayerOrdered[i]);
    }
  }

  // Adds items of the specified layer to the map canvas.
  private void addLayerItems(LayerManager.Layer layer)
  {
    if (layer != null && layerManager != null) {
      List<LayerObject> list = layerManager.getLayerObjects(layer);
      if (list != null) {
        for (int i = 0; i < list.size(); i++) {
          addLayerItem(layer, list.get(i));
        }
      }
    }
  }

  // Adds items of a single layer object to the map canvas.
  private void addLayerItem(LayerManager.Layer layer, LayerObject object)
  {
    if (object != null) {
      // Dealing with ambient icons and ambient ranges separately
      if (layer == LayerManager.Layer.Ambient) {
        AbstractLayerItem item = ((LayerObjectAmbient)object).getLayerItem(ViewerConstants.AMBIENT_ICON);
        if (item != null) {
          rcCanvas.add(item);
        }
      } else if (layer == LayerManager.Layer.AmbientRange) {
        AbstractLayerItem item = ((LayerObjectAmbient)object).getLayerItem(ViewerConstants.AMBIENT_RANGE);
        if (item != null) {
          rcCanvas.add(item);
        }
      } else {
        AbstractLayerItem[] items = object.getLayerItems();
        if (items != null) {
          for (int i = 0; i < items.length; i++) {
            rcCanvas.add(items[i]);
          }
        }
      }
    }
  }

  // Removes all items of all available layers.
  private void removeLayerItems()
  {
    for (int i = 0; i < LayerManager.Layer.values().length; i++) {
      removeLayerItems(LayerManager.Layer.values()[i]);
    }
  }

  // Removes all items of the specified layer.
  private void removeLayerItems(LayerManager.Layer layer)
  {
    if (layer != null && layerManager != null) {
      List<LayerObject> list = layerManager.getLayerObjects(layer);
      if (list != null) {
        for (int i = 0; i < list.size(); i++) {
          removeLayerItem(list.get(i));
        }
      }
    }
  }

  // Removes items of a single layer object from the map canvas.
  private void removeLayerItem(LayerObject object)
  {
    if (object != null) {
      AbstractLayerItem[] items = object.getLayerItems();
      if (items != null) {
        for (int i = 0; i < items.length; i++) {
          rcCanvas.remove(items[i]);
        }
      }
    }
  }

  // Updates all items of all available layers.
  private void updateLayerItems()
  {
    for (int i = 0; i < LayerManager.Layer.values().length; i++) {
      updateLayerItems(LayerManager.Layer.values()[i]);
    }
  }

  // Updates the map locations of the items in the specified layer.
  private void updateLayerItems(LayerManager.Layer layer)
  {
    if (layer != null && layerManager != null) {
      List<LayerObject> list = layerManager.getLayerObjects(layer);
      if (list != null) {
        for (int i = 0; i < list.size(); i++) {
          updateLayerItem(list.get(i));
        }
      }
    }
  }

  // Updates the map locations of the items in the specified layer object.
  private void updateLayerItem(LayerObject object)
  {
    if (object != null) {
      object.update(new Point(), getZoomLevel());
    }
  }

  // Returns the current door state
  private boolean isDrawingClosed()
  {
    return DrawClosed;
  }

  // Draw opened/closed state of doors
  private void setDrawClosed(boolean selected)
  {
    DrawClosed = selected;
    rcCanvas.setDoorsClosed(DrawClosed);
    setLayerDoorState(DrawClosed);
    if (layerManager != null) {
      layerManager.setDoorState(DrawClosed ? ViewerConstants.DOOR_CLOSED : ViewerConstants.DOOR_OPEN);
    }
    updateWindowTitle();
  }

  // Returns the visibility state of the tile grid
  private boolean isDrawingGrid()
  {
    return DrawGrid;
  }

  // Controls draw tile grid
  private void setDrawGrid(boolean selected)
  {
    DrawGrid = selected;
    rcCanvas.setGridEnabled(DrawGrid);
    updateWindowTitle();
  }

  // Returns whether overlays are visible
  private boolean isDrawingOverlays()
  {
    return cbDrawOverlays.isEnabled() && DrawOverlays;
  }

  // Show/hide overlays
  private void setDrawOverlays(boolean selected)
  {
    DrawOverlays = selected;
    rcCanvas.setOverlaysEnabled(DrawOverlays);
    if (!DrawOverlays && cbAnimateOverlays.isSelected()) {
      cbAnimateOverlays.doClick();
    }
    cbAnimateOverlays.setEnabled(DrawOverlays);
    updateWindowTitle();
  }

  // Returns whether overlays are animated
  private boolean isAnimatedOverlays()
  {
    return (cbAnimateOverlays.isEnabled() && DrawOverlays && timerOverlays.isRunning());
  }

  // Activate/deactivate overlay animations
  private void setAnimateOverlays(boolean selected)
  {
    if (selected && !timerOverlays.isRunning()) {
      timerOverlays.start();
    } else if (!selected && timerOverlays.isRunning()) {
      timerOverlays.stop();
    }
    updateWindowTitle();
  }

  // Returns the currently used zoom factor of the canvas map
  private double getZoomLevel()
  {
    return rcCanvas.getZoomFactor();
  }

  private void setZoomLevel(int zoomIndex)
  {
    if (zoomIndex >= 0) {
//      updateViewpointCenter();

      double zoom = 1.0;
      if (zoomIndex == ZoomFactorIndexAuto) {
        // removing scrollbars (not needed in this mode)
        spCanvas.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        spCanvas.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        Dimension viewDim = new Dimension(spCanvas.getViewport().getExtentSize());
        Dimension mapDim = new Dimension(rcCanvas.getMapWidth(false), rcCanvas.getMapHeight(false));
        double zoomX = (double)viewDim.width / (double)mapDim.width;
        double zoomY = (double)viewDim.height / (double)mapDim.height;
        zoom = zoomX;
        if ((int)(zoomX*mapDim.height) > viewDim.height) {
          zoom = zoomY;
        }
      } else {
        // (re-)activating scrollbars
        spCanvas.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        spCanvas.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        zoom = ItemZoomFactor[zoomIndex];
      }
      rcCanvas.setZoomFactor(zoom);
      ZoomLevel = zoomIndex;
      updateWindowTitle();
    }
  }

  // Returns whether auto-fit has been selected
  private boolean isAutoZoom()
  {
    return cbZoomLevel.getSelectedIndex() == ZoomFactorIndexAuto;
  }

  // Updates the map coordinate at the center of the current viewport
//  private void updateViewpointCenter()
//  {
//    if (vpCenterExtent == null) {
//      vpCenterExtent = new Rectangle();
//    }
//    Dimension mapDim = new Dimension(rcCanvas.getMapWidth(true), rcCanvas.getMapHeight(true));
//    JViewport vp = spCanvas.getViewport();
//    Rectangle view = vp.getViewRect();
//    vpCenterExtent.x = view.x + (view.width / 2);
//    vpCenterExtent.y = view.y + (view.height / 2);
//    if (view.width > mapDim.width) {
//      vpCenterExtent.x = mapDim.width / 2;
//    }
//    if (view.height > mapDim.height) {
//      vpCenterExtent.y = mapDim.height / 2;
//    }
//    // canvas coordinate -> map coordinate
//    vpCenterExtent.x = (int)((double)vpCenterExtent.x / getZoomLevel());
//    vpCenterExtent.y = (int)((double)vpCenterExtent.y / getZoomLevel());
//
//    vpCenterExtent.width = spCanvas.getHorizontalScrollBar().getMaximum();
//    vpCenterExtent.height = spCanvas.getVerticalScrollBar().getMaximum();
//  }

  // Attempts to re-center the last known center coordinate in the current viewport
//  private void setViewpointCenter()
//  {
//    if (vpCenterExtent != null) {
//      if (vpCenterExtent.width != spCanvas.getHorizontalScrollBar().getMaximum() &&
//          vpCenterExtent.height != spCanvas.getVerticalScrollBar().getMaximum()) {
//
//        Dimension mapDim = new Dimension(rcCanvas.getMapWidth(true), rcCanvas.getMapHeight(true));
//
//        // map coordinate -> canvas coordinate
//        vpCenterExtent.x = (int)((double)vpCenterExtent.x * getZoomLevel());
//        vpCenterExtent.y = (int)((double)vpCenterExtent.y * getZoomLevel());
//
//        JViewport vp = spCanvas.getViewport();
//        Rectangle view = vp.getViewRect();
//        Point newViewPos = new Point(vpCenterExtent.x - (view.width / 2), vpCenterExtent.y - (view.height / 2));
//        if (newViewPos.x < 0) {
//          newViewPos.x = 0;
//        } else if (newViewPos.x + view.width > mapDim.width) {
//          newViewPos.x = mapDim.width - view.width;
//        }
//        if (newViewPos.y < 0) {
//          newViewPos.y = 0;
//        } else if (newViewPos.y + view.height > mapDim.height) {
//          newViewPos.y = mapDim.height - view.height;
//        }
//
//        vpCenterExtent = null;
//        vp.setViewPosition(newViewPos);
//      }
//    }
//  }

  private void advanceOverlayAnimation()
  {
    if (!bTimerActive) {
      bTimerActive = true;
      try {
        rcCanvas.advanceTileFrame();
      } finally {
        bTimerActive = false;
      }
    }
  }

  // Returns whether map dragging is enabled; updates current and previous mouse positions
  private boolean isMapDragging(Point mousePos)
  {
    if (bMapDragging && mousePos != null && !mapDraggingPos.equals(mousePos)) {
      mapDraggingPos.x = mousePos.x;
      mapDraggingPos.y = mousePos.y;
    }
    return bMapDragging;
  }

  // Enables/disables map dragging mode (set mouse cursor, global state and current mouse position)
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

  // Returns the current or previous mouse position
  private Point getMapDraggingDistance()
  {
    Point pDelta = new Point();
    if (bMapDragging) {
      pDelta.x = mapDraggingPosStart.x - mapDraggingPos.x;
      pDelta.y = mapDraggingPosStart.y - mapDraggingPos.y;
    }
    return pDelta;
  }

  // Updates the map portion displayed in the viewport
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

  // Creates and displays a popup menu containing the items located at the specified location
  private boolean updateItemPopup(Point canvasCoords)
  {
    final int MaxLen = 32;

    if (layerManager != null) {
      // preparing menu items
      List<JMenuItem> menuItems = new ArrayList<JMenuItem>();
      Point itemLocation = new Point();

      // adding global menu item
      pmItems.removeAll();
      LayerMenuItem lmi = new LayerMenuItem("Global: " + map.getAreItem().getMessage(), map.getAreItem());
      lmi.setIcon(Icons.getIcon("Edit16.gif"));
      lmi.addActionListener(this);
      pmItems.add(lmi);
      lmi = new LayerMenuItem("Global: " + map.getWedItem(getCurrentWedIndex()).getMessage(),
                              map.getWedItem(getCurrentWedIndex()));
      lmi.setIcon(Icons.getIcon("Edit16.gif"));
      lmi.addActionListener(this);
      pmItems.add(lmi);

      // for each active layer...
      for (int i = 0; i < cbLayers.length; i++) {
        if (cbLayers[i].isSelected()) {
          LayerManager.Layer layer = LayerManager.getLayerType(i);
          if (layer != null) {
            List<LayerObject> itemList = layerManager.getLayerObjects(layer);
            if (itemList != null) {
              // for each layer object...
              for (int j = 0; j < itemList.size(); j++) {
                AbstractLayerItem[] items = itemList.get(j).getLayerItems();
                // for each layer item...
                for (int k = 0; k < items.length; k++) {
                  // special case: Ambient/Ambient range (avoid duplicates)
                  if (layer == LayerManager.Layer.Ambient &&
                      layerManager.isLayerVisible(LayerManager.Layer.AmbientRange) &&
                      ((LayerObjectAmbient)itemList.get(j)).isLocal()) {
                    // skipped: will be handled in AmbientRange layer
                    break;
                  }
                  if (layer == LayerManager.Layer.AmbientRange &&
                             ((LayerObjectAmbient)itemList.get(j)).isLocal() &&
                             k == 0) {
                    // considering ranged item only
                    continue;
                  }
                  itemLocation.x = canvasCoords.x - items[k].getX();
                  itemLocation.y = canvasCoords.y - items[k].getY();
                  if (items[k].isVisible() && items[k].contains(itemLocation)) {
                    // create a new menu entry
                    StringBuilder sb = new StringBuilder();
                    if (items[k].getName() != null && !items[k].getName().isEmpty()) {
                      sb.append(items[k].getName());
                    } else {
                      sb.append("Item");
                    }
                    sb.append(": ");
                    int lenPrefix = sb.length();
                    int lenMsg = items[k].getMessage().length();
                    if (lenPrefix + lenMsg > MaxLen) {
                      sb.append(items[k].getMessage().substring(0, MaxLen - lenPrefix));
                      sb.append("...");
                    } else {
                      sb.append(items[k].getMessage());
                    }
                    lmi = new LayerMenuItem(sb.toString(), items[k]);
                    if (lenPrefix + lenMsg > MaxLen) {
                      lmi.setToolTipText(items[k].getMessage());
                    }
                    lmi.addActionListener(this);
                    menuItems.add(lmi);
                  }
                }
              }
            }
          }
        }
      }

      // updating context menu with the prepared item list
      if (!menuItems.isEmpty()) {
        pmItems.addSeparator();
        for (int i = 0; i < menuItems.size(); i++) {
          pmItems.add(menuItems.get(i));
        }
      }
      return true;
    }
    return false;
  }

  // Shows a popup menu containing layer items located at the current position if available
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

  // Opens a viewable instance associated with the specified layer item
  private void showTable(AbstractLayerItem item)
  {
    if (item != null) {
      if (item.getViewable() == map.getAre()) {
        // special: global ARE structure
        map.getAre().selectEditTab();
        getParentWindow().toFront();
      } else if (item.getViewable() instanceof WedResource) {
        // special: global WED structure
        new ViewFrame(NearInfinity.getInstance(), item.getViewable());
      } else {
        item.showViewable();
      }
    }
  }

  // Attempts to find the first parent window of this viewer
  private Window getParentWindow()
  {
    Component c = parent;
    while (c != null) {
      if (c instanceof Window) {
        return (Window)c;
      }
      c = c.getParent();
    }
    return NearInfinity.getInstance();
  }

  private void initProgressMonitor(Component parent, String msg, String note, int maxProgress,
                                   int msDecide, int msWait)
  {
    if (parent == null) parent = NearInfinity.getInstance();
    if (maxProgress <= 0) maxProgress = 1;

    releaseProgressMonitor();
    pmMax = maxProgress;
    pmCur = 0;
    progress = new ProgressMonitor(parent, msg, note, 0, pmMax);
    progress.setMillisToDecideToPopup(msDecide);
    progress.setMillisToPopup(msWait);
    progress.setProgress(pmCur);
  }

  private void releaseProgressMonitor()
  {
    if (progress != null) {
      progress.close();
      progress = null;
    }
  }

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

  private boolean isProgressMonitorActive()
  {
    return progress != null;
  }

//----------------------------- INNER CLASSES -----------------------------

  // Associates a menu item with a layer item
  private class LayerMenuItem extends JMenuItem
  {
    private AbstractLayerItem layerItem;

    public LayerMenuItem(String text, AbstractLayerItem item)
    {
      super(text);
      layerItem = item;
    }

    public AbstractLayerItem getLayerItem()
    {
      return layerItem;
    }
  }


  // Handles map-specific properties
  private static class Map
  {
    private static final int MAP_DAY    = 0;
    private static final int MAP_NIGHT  = 1;

    private final Window parent;
    private final WedResource[] wed = new WedResource[2];
    private final AbstractLayerItem[] wedItem = new IconLayerItem[]{null, null};

    private AreResource are;
    private boolean hasDayNight, hasExtendedNight;
    private AbstractLayerItem areItem;

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
      are = null;
      areItem = null;
      closeWed(MAP_DAY, false);
      wed[MAP_DAY] = null;
      closeWed(MAP_NIGHT, false);
      wed[MAP_NIGHT] = null;
      wedItem[MAP_DAY] = null;
      wedItem[MAP_NIGHT] = null;
    }

    // Returns the current AreResource instance
    public AreResource getAre()
    {
      return are;
    }

    // Returns the WedResource instance of day or night map
    public WedResource getWed(int dayNight)
    {
      switch (dayNight) {
        case MAP_DAY: return wed[MAP_DAY];
        case MAP_NIGHT: return wed[MAP_NIGHT];
        default: return null;
      }
    }

    /**
     * Attempts to close the specified WED. If changes have been done, a dialog asks for saving.
     * @param dayNight Either MAP_DAY or MAP_NIGHT.
     * @param allowCancel Indicates whether to allow cancelling the saving process.
     * @return <code>true</code> if the resource has been closed, <code>false</code> otherwise (e.g.
     *         if the user chooses to cancel saving changes.)
     */
    public boolean closeWed(int dayNight, boolean allowCancel)
    {
      boolean bRet = false;
      dayNight = (dayNight == MAP_NIGHT) ? MAP_NIGHT : MAP_DAY;
      if (wed[dayNight] != null) {
        if (wed[dayNight].hasStructChanged()) {
          File output;
          if (wed[dayNight].getResourceEntry() instanceof BIFFResourceEntry) {
            output = NIFile.getFile(ResourceFactory.getRootDir(),
                                    ResourceFactory.OVERRIDEFOLDER + File.separatorChar +
                                    wed[dayNight].getResourceEntry().toString());
          } else {
            output = wed[dayNight].getResourceEntry().getActualFile();
          }
          int optionIndex = allowCancel ? 1 : 0;
          int optionType = allowCancel ? JOptionPane.YES_NO_CANCEL_OPTION : JOptionPane.YES_NO_OPTION;
          String options[][] = { {"Save changes", "Discard changes"}, {"Save changes", "Discard changes", "Cancel"} };
          int result = JOptionPane.showOptionDialog(parent, "Save changes to " + output + '?', "Resource changed",
                                                    optionType, JOptionPane.WARNING_MESSAGE, null,
                                                    options[optionIndex], options[optionIndex][0]);
          if (result == 0) {
            ResourceFactory.getInstance().saveResource((Resource)wed[dayNight], parent);
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
      if (are != null) {
        dayNight = (dayNight == MAP_NIGHT) ? MAP_NIGHT : MAP_DAY;
        String wedName = "";
        ResourceRef wedRef = (ResourceRef)are.getAttribute("WED resource");
        if (wedRef != null) {
          wedName = wedRef.getResourceName();
          if ("None".equalsIgnoreCase(wedName)) {
            wedName = "";
          }

          if (dayNight == MAP_DAY) {
            if (!wedName.isEmpty()) {
              try {
                wed[MAP_DAY] = new WedResource(ResourceFactory.getInstance().getResourceEntry(wedName));
              } catch (Exception e) {
                wed[MAP_DAY] = null;
              }
            } else {
              wed[MAP_DAY] = null;
            }

            if (wed[MAP_DAY] != null) {
              wedItem[MAP_DAY] = new IconLayerItem(new Point(), wed[MAP_DAY], wed[MAP_DAY].getName());
              wedItem[MAP_DAY].setVisible(false);
            }
          } else {
            // getting extended night map
            if (hasExtendedNight && !wedName.isEmpty()) {
              int pos = wedName.lastIndexOf('.');
              if (pos > 0) {
                String wedNameNight = wedName.substring(0, pos) + "N" + wedName.substring(pos);
                try {
                  wed[MAP_NIGHT] = new WedResource(ResourceFactory.getInstance().getResourceEntry(wedNameNight));
                } catch (Exception e) {
                  wed[MAP_NIGHT] = wed[MAP_DAY];
                }
              } else {
                wed[MAP_NIGHT] = wed[MAP_DAY];
              }
            } else {
              wed[MAP_NIGHT] = wed[MAP_DAY];
            }

            if (wed[MAP_NIGHT] != null) {
              wedItem[MAP_NIGHT] = new IconLayerItem(new Point(), wed[MAP_NIGHT], wed[MAP_NIGHT].getName());
              wedItem[MAP_NIGHT].setVisible(false);
            }
          }
        }
      }
    }

    // Returns the pseudo layer item for the AreResource structure
    public AbstractLayerItem getAreItem()
    {
      return areItem;
    }

    // Returns the pseudo layer item for the WedResource structure of the selected day time
    public AbstractLayerItem getWedItem(int dayNight)
    {
      if (dayNight == MAP_NIGHT) {
        return wedItem[MAP_NIGHT];
      } else {
        return wedItem[MAP_DAY];
      }
    }

    // Returns whether the current map supports day/twilight/night settings
    public boolean hasDayNight()
    {
      return hasDayNight;
    }

    // Returns true if the current map has separate WEDs for day/night
    public boolean hasExtendedNight()
    {
      return hasExtendedNight;
    }


    private void init()
    {
      if (are != null) {
        // fetching important flags
        Flag flags = (Flag)are.getAttribute("Location");
        if (flags != null) {
          if (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT) {
            hasDayNight = flags.isFlagSet(10);
            hasExtendedNight = false;
          } else {
            hasDayNight = flags.isFlagSet(1);
            hasExtendedNight = flags.isFlagSet(6);
          }
        }

        // getting associated WED resources
        reloadWed(MAP_DAY);
        reloadWed(MAP_NIGHT);

        // initializing pseudo layer items to easily access the main structures
        areItem = new IconLayerItem(new Point(), are, are.getName());
        areItem.setVisible(false);
      }
    }
  }
}
