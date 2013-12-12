// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import infinity.NearInfinity;
import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.IdsBitmap;
import infinity.datatype.RemovableDecNumber;
import infinity.datatype.ResourceRef;
import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.datatype.StringRef;
import infinity.datatype.TextEdit;
import infinity.datatype.TextString;
import infinity.gui.Center;
import infinity.gui.ChildFrame;
import infinity.gui.RenderCanvas;
import infinity.gui.WindowBlocker;
import infinity.gui.layeritem.AbstractLayerItem;
import infinity.gui.layeritem.IconLayerItem;
import infinity.gui.layeritem.LayerItemEvent;
import infinity.gui.layeritem.LayerItemListener;
import infinity.gui.layeritem.ShapedLayerItem;
import infinity.icon.Icons;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.are.Actor;
import infinity.resource.are.Ambient;
import infinity.resource.are.Animation;
import infinity.resource.are.AreResource;
import infinity.resource.are.AutomapNote;
import infinity.resource.are.AutomapNotePST;
import infinity.resource.are.Door;
import infinity.resource.are.Entrance;
import infinity.resource.are.ITEPoint;
import infinity.resource.are.ProTrap;
import infinity.resource.are.SpawnPoint;
import infinity.resource.are.viewer.AreaStructure.Structure;
import infinity.resource.cre.CreResource;
import infinity.resource.graphics.ColorConvert;
import infinity.resource.graphics.TisDecoder;
import infinity.resource.key.FileResourceEntry;
import infinity.resource.key.ResourceEntry;
import infinity.resource.to.StrRefEntry;
import infinity.resource.to.StringEntry;
import infinity.resource.to.TohResource;
import infinity.resource.to.TotResource;
import infinity.resource.vertex.Vertex;
import infinity.resource.wed.Overlay;
import infinity.resource.wed.Tilemap;
import infinity.resource.wed.WedResource;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.ProgressMonitor;
import javax.swing.SpringLayout;

/**
 * The Area Viewer shows a selected map with its associated items, such as actors, regions or
 * animations.
 * @author argent77
 */
public final class AreaViewer extends ChildFrame
  implements Runnable, ActionListener, ItemListener, LayerItemListener, ComponentListener,
             MouseMotionListener, MouseListener
{
  // identifies the respective layers of map structures
  private static enum Layers { ACTOR, REGION, ENTRANCE, CONTAINER, AMBIENT, AMBIENTRANGE, DOOR,
                               ANIMATION, AUTOMAP, SPAWNPOINT, PROTRAP, TRANSITION, DOORPOLY,
                               WALLPOLY }

  // identifies the respective WED resources
  private static enum DayNight { DAY, NIGHT }

  // identifies locations of the map transitions
  private static enum AreaEdge { NORTH, EAST, SOUTH, WEST }

  // tracks the current layer item state
  private static final EnumMap<Layers, Boolean> LayerButtonState =
      new EnumMap<Layers, Boolean>(Layers.class);
  private static final EnumMap<Layers, String> layerItemDesc =
      new EnumMap<Layers, String>(Layers.class);
  static {
    for (final Layers layer: Layers.values()) {
      LayerButtonState.put(layer, false);
    }
    layerItemDesc.put(Layers.ACTOR, "Actor");
    layerItemDesc.put(Layers.REGION, "Region");
    layerItemDesc.put(Layers.ENTRANCE, "Entrance");
    layerItemDesc.put(Layers.CONTAINER, "Container");
    layerItemDesc.put(Layers.AMBIENT, "Sound");
    layerItemDesc.put(Layers.AMBIENTRANGE, "Sound");
    layerItemDesc.put(Layers.DOOR, "Door");
    layerItemDesc.put(Layers.ANIMATION, "Animation");
    layerItemDesc.put(Layers.AUTOMAP, "Automap");
    layerItemDesc.put(Layers.SPAWNPOINT, "Spawn Point");
    layerItemDesc.put(Layers.PROTRAP, "Trap");
    layerItemDesc.put(Layers.TRANSITION, "Transition");
    layerItemDesc.put(Layers.DOORPOLY, "Door Poly");
    layerItemDesc.put(Layers.WALLPOLY, "Wall Poly");
  }

  // RadioButtons to switch between day/night WEDs
  private final EnumMap<DayNight, JRadioButton> dayNightButton =
      new EnumMap<DayNight, JRadioButton>(DayNight.class);
  // stores day and night WED resources
  private final EnumMap<DayNight, WedResource> dayNightWed =
      new EnumMap<DayNight, WedResource>(DayNight.class);
  // stores lists of tiles for both day and night maps
  private final EnumMap<DayNight, List<TileInfo>> dayNightTiles =
      new EnumMap<DayNight, List<TileInfo>>(DayNight.class);
  // stores door tile indices for both day and night maps
  private final EnumMap<DayNight, List<Integer>> dayNightDoorIndices =
      new EnumMap<DayNight, List<Integer>>(DayNight.class);
  // CheckBoxes to show/hide specific layers of map structures
  private final EnumMap<Layers, JCheckBox> layerButton =
      new EnumMap<Layers, JCheckBox>(Layers.class);
  // stores the visual representations of the actual map structures
  private final EnumMap<Layers, List<AbstractLayerItem>> layerItems =
      new EnumMap<Layers, List<AbstractLayerItem>>(Layers.class);

  private final AreResource are;
  private final AreaStructure structure;    // provides access to preprocessed map structures

  private DayNight currentMap;
  private TisDecoder tisDecoder;
  private JPanel pRoot, pView, pSideBar;
  private JScrollPane spView;
  private RenderCanvas mapCanvas;
  private JCheckBox cbDrawClosed;
  private JLabel lPosX, lPosY;
  private JTextArea taInfo;

  private Point mapCoordinate;      // map location of current mouse cursor position
  private JPopupMenu pmItems;       // displays a list of layer items at a specific map position
  boolean bMapDragging;             // Is map dragging active
  private Point mapDraggingPosStart;          // starting mouse position during map dragging
  private Point mapDraggingPosCurrent;        // current mouse positiuon during map dragging
  private Point mapDraggingScrollStart;       // starting Viewport location during dragging
  private ProgressMonitor progressMonitor;    // progress dialog shown during GUI initialization
  private int pmMax, pmCur;                   // tracks GUI initialization progress

  /**
   * Checks whether the specified ARE resource can be displayed with the area viewer.
   * @param are The ARE resource to check
   * @return true if area is viewable, false otherwise.
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


  /**
   * Constructs a new Area Viewer window.
   * @param areaFile The ARE resource to process.
   */
  public AreaViewer(AreResource areaFile)
  {
    this(NearInfinity.getInstance(), areaFile);
  }

  /**
   * Constructs a new Area Viewer window.
   * @param parent Determines the position of the progress dialog during initialization.
   * @param areaFile The ARE resource to process.
   */
  public AreaViewer(Component parent, AreResource areaFile)
  {
    super("Area Viewer: " + areaFile.getName(), true);
    this.are = areaFile;
    this.structure = new AreaStructure(this);

    if ((NearInfinity.getInstance().getExtendedState() & Frame.MAXIMIZED_BOTH) != 0) {
      setExtendedState(Frame.MAXIMIZED_BOTH);
    } else {
      setExtendedState(Frame.NORMAL);
    }

    initProgressMonitor(parent, "Initializing " + are.getName(), 3, 0, 0);
    new Thread(this).start();
  }

  /**
   * Returns the ARE resource structure attached to the viewer.
   * @return The current ARE resource structure.
   */
  public AreResource getAre()
  {
    return are;
  }

  /**
   * Returns the currently selected WED resource structure that is linked to the ARE.
   * @return The currently selected WED resource structure.
   */
  public WedResource getCurrentWed()
  {
    return dayNightWed.get(getCurrentMap());
  }


//--------------------- Begin Interface Runnable ---------------------

  @Override
  public void run()
  {
    initGui();
    releaseProgressMonitor();
  }

//--------------------- End Interface Runnable ---------------------

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == dayNightButton.get(DayNight.DAY)) {
      setCurrentMap(DayNight.DAY);
      structure.initWed();
      initLayerDoorPoly();
      initLayerWallPoly();
      enableLayer(Layers.DOORPOLY, layerButton.get(Layers.DOORPOLY).isSelected());
      enableLayer(Layers.WALLPOLY, layerButton.get(Layers.WALLPOLY).isSelected());
    } else if (event.getSource() == dayNightButton.get(DayNight.NIGHT)) {
      setCurrentMap(DayNight.NIGHT);
      structure.initWed();
      initLayerDoorPoly();
      initLayerWallPoly();
      enableLayer(Layers.DOORPOLY, layerButton.get(Layers.DOORPOLY).isSelected());
      enableLayer(Layers.WALLPOLY, layerButton.get(Layers.WALLPOLY).isSelected());
    } else if (event.getSource() instanceof AbstractLayerItem) {
      AbstractLayerItem item = (AbstractLayerItem)event.getSource();
      item.showViewable();
    } else if (event.getSource() instanceof LayerMenuItem) {
      LayerMenuItem lmi = (LayerMenuItem)event.getSource();
      AbstractLayerItem item = lmi.getLayerItem();
      if (item != null) {
        item.showViewable();
      }
    }
  }

//--------------------- End Interface ActionListener ---------------------

//--------------------- Begin Interface ItemListener ---------------------

  @Override
  public void itemStateChanged(ItemEvent event)
  {
    for (final Layers layer: Layers.values()) {
      JCheckBox cb;
      if (event.getItemSelectable() != null &&
          event.getItemSelectable() == (cb = layerButton.get(layer))) {
        enableLayer(layer, cb.isSelected());
        if (layer == Layers.AMBIENT) {
          layerButton.get(Layers.AMBIENTRANGE).setEnabled(
              cb.isSelected() && !layerItems.get(Layers.AMBIENTRANGE).isEmpty());
          enableLayer(Layers.AMBIENTRANGE,
                      cb.isSelected() && layerButton.get(Layers.AMBIENTRANGE).isSelected());
        }
        return;
      }
    }

    if (event.getItemSelectable() == cbDrawClosed) {
      setDoorState(getCurrentMap(), drawDoorsClosed());
      enableLayer(Layers.DOOR, layerButton.get(Layers.DOOR).isSelected());
      enableLayer(Layers.DOORPOLY, layerButton.get(Layers.DOORPOLY).isSelected());
    }
  }

//--------------------- End Interface ItemListener ---------------------

//--------------------- Begin Interface ItemStateListener ---------------------

  @Override
  public void layerItemChanged(LayerItemEvent event)
  {
    if (event.getSource() instanceof AbstractLayerItem) {
      AbstractLayerItem item = (AbstractLayerItem)event.getSource();
      if (event.isHighlighted()) {
        setInfoText(item.getMessage());
        setAreaLocation(item.getMapLocation());
      } else {
        setInfoText(null);
      }
    }
  }

//--------------------- End Interface ItemStateListener ---------------------

//--------------------- Begin Interface ComponentListener ---------------------

  @Override
  public void componentHidden(ComponentEvent event)
  {
  }

  @Override
  public void componentMoved(ComponentEvent event)
  {
  }

  @Override
  public void componentResized(ComponentEvent event)
  {
    if (event.getSource() == mapCanvas) {
      // changing viewport size whenever the tileset size changes
      pView.setPreferredSize(mapCanvas.getSize());
    } else if (event.getSource() == spView) {
      // centering the tileset if it fits into the viewport
      Dimension pDim = mapCanvas.getPreferredSize();
      Dimension spDim = spView.getSize();
      if (pDim.width < spDim.width || pDim.height < spDim.height) {
        Point pLocation = mapCanvas.getLocation();
        Point pDistance = new Point();
        if (pDim.width < spDim.width)
          pDistance.x = pLocation.x - (spDim.width - pDim.width) / 2;
        if (pDim.height < spDim.height)
          pDistance.y = pLocation.y - (spDim.height - pDim.height) / 2;
        mapCanvas.setLocation(pLocation.x - pDistance.x, pLocation.y - pDistance.y);
      }
    }
  }

  @Override
  public void componentShown(ComponentEvent event)
  {
  }

//--------------------- End Interface ComponentListener ---------------------

//--------------------- Begin Interface MouseMotionListener ---------------------

  @Override
  public void mouseDragged(MouseEvent event)
  {
    if (event.getSource() == mapCanvas && isMapDragging(event.getLocationOnScreen())) {
      moveMapViewport();
    }
  }

  @Override
  public void mouseMoved(MouseEvent event)
  {
    if (event.getSource() == mapCanvas) {
      setAreaLocation(event.getPoint());
    } else if (event.getSource() instanceof AbstractLayerItem) {
      // forwarding mouse event to continue displaying cursor position information
      // when hovering over layer items
      AbstractLayerItem item = (AbstractLayerItem)event.getSource();
      MouseEvent newEvent =
          new MouseEvent(mapCanvas, event.getID(), event.getWhen(), event.getModifiers(),
                         event.getX() + item.getX(), event.getY() + item.getY(),
                         event.getXOnScreen(), event.getYOnScreen(),
                         event.getClickCount(), event.isPopupTrigger(), event.getButton());
      mapCanvas.dispatchEvent(newEvent);
    }
  }

//--------------------- End Interface MouseMotionListener ---------------------

//--------------------- Begin Interface MouseListener ---------------------

  @Override
  public void mouseClicked(MouseEvent event)
  {
  }

  @Override
  public void mouseEntered(MouseEvent event)
  {
  }

  @Override
  public void mouseExited(MouseEvent event)
  {
  }

  @Override
  public void mousePressed(MouseEvent event)
  {
    if (event.getButton() == MouseEvent.BUTTON1 && event.getSource() == mapCanvas) {
      setMapDraggingEnabled(true, event.getLocationOnScreen());
    } else {
      showItemPopup(event);
    }
  }

  @Override
  public void mouseReleased(MouseEvent event)
  {
    if (event.getButton() == MouseEvent.BUTTON1 && event.getSource() == mapCanvas) {
      setMapDraggingEnabled(false, event.getLocationOnScreen());
    } else {
      showItemPopup(event);
    }
  }

//--------------------- End Interface MouseListener ---------------------

//--------------------- Begin Class ChildFrame ---------------------

  @Override
  protected boolean windowClosing(boolean forced) throws Exception
  {
    mapCanvas.setImage(null);
    mapCanvas.removeAll();
    layerItems.clear();
    tisDecoder.close();
    structure.clear();
    dayNightWed.clear();
    dispose();
    System.gc();
    return super.windowClosing(forced);
  }

//--------------------- End Class ChildFrame ---------------------

  private void initGui()
  {
    // assembling main view
    pView = new JPanel(null);
    mapCanvas = new RenderCanvas();
    mapCanvas.addComponentListener(this);
    mapCanvas.addMouseListener(this);
    mapCanvas.addMouseMotionListener(this);
    mapCanvas.setHorizontalAlignment(RenderCanvas.CENTER);
    mapCanvas.setVerticalAlignment(RenderCanvas.CENTER);
    pView.add(mapCanvas, BorderLayout.CENTER);
    spView = new JScrollPane(pView);
    spView.addComponentListener(this);
    spView.getVerticalScrollBar().setUnitIncrement(16);
    spView.getHorizontalScrollBar().setUnitIncrement(16);

    // initializing map data
    advanceProgressMonitor("Loading tileset");
    initMap();
    advanceProgressMonitor("Loading map entries");
    // initializing layer items (order is important for z-order (front to back)!)
    structure.init();
    initLayerActor();
    initLayerEntrance();
    initLayerAmbient();
    initLayerAnimation();
    initLayerProTrap();
    initLayerSpawnPoint();
    initLayerAutomap();
    initLayerContainer();
    initLayerDoor();
    initLayerRegion();
    initLayerTransition();
    initLayerDoorPoly();
    initLayerWallPoly();
    initLayerAmbientRange();
    advanceProgressMonitor("Creating GUI");

    // assembling Visual State group box
    ButtonGroup bg = new ButtonGroup();
    JPanel pVisual = createGroupBox("Visual State: ", new GridLayout(dayNightButton.size() + 1, 1));
    for (final JRadioButton rb: dayNightButton.values()) {
      bg.add(rb);
      pVisual.add(rb);
    }
    cbDrawClosed = new JCheckBox("Draw closed");
    cbDrawClosed.addItemListener(this);
    pVisual.add(cbDrawClosed);

    // Assembling Layers group box
    JPanel pLayers = createGroupBox("Layers: ", new GridLayout(layerButton.size(), 1));
    for (final JCheckBox cb: layerButton.values()) {
      pLayers.add(cb);
    }

    // Assembling Information box
    SpringLayout sl = new SpringLayout();
    JPanel pInfo = createGroupBox("Information: ", sl);
    JLabel lPosXText = new JLabel("Position X:  ");
    JLabel lPosYText = new JLabel("Position Y:  ");
    lPosX = new JLabel("0");
    lPosY = new JLabel("0");
    taInfo = new JTextArea(4, 15);
    taInfo.setEditable(false);
    taInfo.setFont(new JLabel().getFont());
    taInfo.setBackground(new JLabel().getBackground());
    taInfo.setSelectionColor(taInfo.getBackground());
    taInfo.setSelectedTextColor(taInfo.getForeground());
    taInfo.setWrapStyleWord(true);
    taInfo.setLineWrap(true);
    taInfo.setPreferredSize(taInfo.getMinimumSize());
    pInfo.add(lPosXText);
    pInfo.add(lPosX);
    pInfo.add(lPosYText);
    pInfo.add(lPosY);
    pInfo.add(taInfo);
    sl.putConstraint(SpringLayout.WEST, lPosXText, 3, SpringLayout.WEST, pInfo);
    sl.putConstraint(SpringLayout.NORTH, lPosXText, 3, SpringLayout.NORTH, pInfo);
    sl.putConstraint(SpringLayout.WEST, lPosX, 3, SpringLayout.EAST, lPosXText);
    sl.putConstraint(SpringLayout.NORTH, lPosX, 3, SpringLayout.NORTH, pInfo);
    sl.putConstraint(SpringLayout.WEST, lPosYText, 3, SpringLayout.WEST, pInfo);
    sl.putConstraint(SpringLayout.NORTH, lPosYText, 3, SpringLayout.SOUTH, lPosXText);
    sl.putConstraint(SpringLayout.WEST, lPosY, 3, SpringLayout.EAST, lPosYText);
    sl.putConstraint(SpringLayout.NORTH, lPosY, 3, SpringLayout.SOUTH, lPosX);
    sl.putConstraint(SpringLayout.WEST, taInfo, 3, SpringLayout.WEST, pInfo);
    sl.putConstraint(SpringLayout.NORTH, taInfo, 3, SpringLayout.SOUTH, lPosYText);
    sl.putConstraint(SpringLayout.EAST, pInfo, 3, SpringLayout.EAST, taInfo);
    sl.putConstraint(SpringLayout.SOUTH, pInfo, 3, SpringLayout.SOUTH, taInfo);

    // Streamlining groupboxes
    JPanel[] list = new JPanel[]{ pVisual, pLayers, pInfo };
    int maxWidth = 0;
    for (int i = 0; i < list.length; i++) {
      maxWidth = Math.max(list[i].getPreferredSize().width, maxWidth);
    }
    for (int i = 0; i < list.length; i++) {
      list[i].setPreferredSize(new Dimension(maxWidth, list[i].getPreferredSize().height));
    }

    // Assembling sidebar
    sl = new SpringLayout();
    pSideBar = new JPanel(sl);
    pSideBar.add(pVisual);
    pSideBar.add(pLayers);
    pSideBar.add(pInfo);
    JPanel panel = new JPanel();
    pSideBar.add(panel);
    pVisual.setMaximumSize(pVisual.getPreferredSize());
    sl.putConstraint(SpringLayout.WEST, pVisual, 5, SpringLayout.WEST, pSideBar);
    sl.putConstraint(SpringLayout.NORTH, pVisual, 5, SpringLayout.NORTH, pSideBar);
    pLayers.setMaximumSize(pLayers.getPreferredSize());
    sl.putConstraint(SpringLayout.WEST, pLayers, 5, SpringLayout.WEST, pSideBar);
    sl.putConstraint(SpringLayout.NORTH, pLayers, 5, SpringLayout.SOUTH, pVisual);
    pInfo.setMaximumSize(pInfo.getPreferredSize());
    sl.putConstraint(SpringLayout.WEST, pInfo, 5, SpringLayout.WEST, pSideBar);
    sl.putConstraint(SpringLayout.NORTH, pInfo, 5, SpringLayout.SOUTH, pLayers);
    sl.putConstraint(SpringLayout.WEST, panel, 5, SpringLayout.WEST, pSideBar);
    sl.putConstraint(SpringLayout.NORTH, panel, 5, SpringLayout.SOUTH, pInfo);
    sl.putConstraint(SpringLayout.EAST, pSideBar, 5, SpringLayout.EAST, pVisual);
    sl.putConstraint(SpringLayout.SOUTH, pSideBar, 5, SpringLayout.SOUTH, panel);

    // putting all together
    pRoot = new JPanel(new BorderLayout());
    pRoot.add(spView, BorderLayout.CENTER);
    pRoot.add(pSideBar, BorderLayout.LINE_END);

    Container pane = getContentPane();
    pane.setLayout(new BorderLayout());
    pane.add(pRoot, BorderLayout.CENTER);
    setSize(NearInfinity.getInstance().getSize());
    Center.center(this, NearInfinity.getInstance().getBounds());

    // misc. initializations
    pmItems = new JPopupMenu("Select item:");
    bMapDragging = false;
    mapDraggingPosStart = new Point();
    mapDraggingPosCurrent = new Point();
    mapDraggingScrollStart = new Point();

    // first time layer initialization (order of ambient/ambientrange is important!)
    layerButton.get(Layers.ACTOR).setSelected(LayerButtonState.get(Layers.ACTOR));
    layerButton.get(Layers.REGION).setSelected(LayerButtonState.get(Layers.REGION));
    layerButton.get(Layers.ENTRANCE).setSelected(LayerButtonState.get(Layers.ENTRANCE));
    layerButton.get(Layers.CONTAINER).setSelected(LayerButtonState.get(Layers.CONTAINER));
    layerButton.get(Layers.AMBIENTRANGE).setSelected(LayerButtonState.get(Layers.AMBIENTRANGE));
    layerButton.get(Layers.AMBIENTRANGE).setEnabled(LayerButtonState.get(Layers.AMBIENT) &&
                                                    !layerItems.get(Layers.AMBIENTRANGE).isEmpty());
    layerButton.get(Layers.AMBIENT).setSelected(LayerButtonState.get(Layers.AMBIENT));
    layerButton.get(Layers.DOOR).setSelected(LayerButtonState.get(Layers.DOOR));
    layerButton.get(Layers.ANIMATION).setSelected(LayerButtonState.get(Layers.ANIMATION));
    layerButton.get(Layers.AUTOMAP).setSelected(LayerButtonState.get(Layers.AUTOMAP));
    layerButton.get(Layers.TRANSITION).setSelected(LayerButtonState.get(Layers.TRANSITION));
    layerButton.get(Layers.PROTRAP).setSelected(LayerButtonState.get(Layers.PROTRAP));
    layerButton.get(Layers.SPAWNPOINT).setSelected(LayerButtonState.get(Layers.SPAWNPOINT));
    layerButton.get(Layers.DOORPOLY).setSelected(LayerButtonState.get(Layers.DOORPOLY));
    layerButton.get(Layers.WALLPOLY).setSelected(LayerButtonState.get(Layers.WALLPOLY));

    setVisible(true);
  }

  private void createDayNight(DayNight dn, String label)
  {
    JRadioButton rb = new JRadioButton(label);
    rb.addActionListener(this);
    rb.setEnabled(false);
    dayNightButton.put(dn, rb);
  }

  // adds layer-specific checkboxes to the GUI
  private void addLayer(Layers layer)
  {
    if (layer != null) {
      JCheckBox cb = layerButton.get(layer);
      // add only if not yet created
      if (cb == null) {
        switch (layer) {
          case ACTOR:         cb = new JCheckBox("Actors"); break;
          case REGION:        cb = new JCheckBox("Regions"); break;
          case ENTRANCE:      cb = new JCheckBox("Entrances"); break;
          case CONTAINER:     cb = new JCheckBox("Containers"); break;
          case AMBIENT:       cb = new JCheckBox("Ambient Sounds"); break;
          case AMBIENTRANGE:  cb = new JCheckBox("Ambient Sound Ranges"); break;
          case DOOR:          cb = new JCheckBox("Doors"); break;
          case ANIMATION:     cb = new JCheckBox("Background Animations"); break;
          case AUTOMAP:       cb = new JCheckBox("Automap Notes"); break;
          case SPAWNPOINT:    cb = new JCheckBox("Spawn Points"); break;
          case PROTRAP:       cb = new JCheckBox("Projectile Traps"); break;
          case TRANSITION:    cb = new JCheckBox("Map Transitions"); break;
          case DOORPOLY:      cb = new JCheckBox("Door Polygons"); break;
          case WALLPOLY:      cb = new JCheckBox("Wall Polygons"); break;
        }
        if (cb != null) {
          layerButton.put(layer, cb);
          cb.addItemListener(this);
          cb.setEnabled(false);
        }
      }
    }
  }

  // adds a list of layer items to the view, removes old items if necessary
  private void addLayerItems(Layers layer, List<AbstractLayerItem> list)
  {
    if (layer != null) {
      if (layerItems.containsKey(layer)) {
        List<AbstractLayerItem> oldList = layerItems.get(layer);
        for (final AbstractLayerItem item: oldList) {
          if (item != null) {
            mapCanvas.remove(item);
          }
        }
        oldList.clear();
      }

      if (list != null) {
        for (final AbstractLayerItem item: list) {
          if (item != null) {
            item.setVisible(false);
            mapCanvas.add(item);
            item.setItemLocation(item.getMapLocation());
          }
        }
        layerItems.put(layer, list);
      }
    }
  }

  // Returns whether the layer is visible
  private boolean isLayerSelected(Layers layer)
  {
    if (layer != null && layerButton.containsKey(layer)) {
      return layerButton.get(layer).isSelected();
    }
    return false;
  }

//  // Returns whether the layer is available
//  private boolean isLayerEnabled(Layers layer)
//  {
//    if (layer != null && layerButton.containsKey(layer)) {
//      return layerButton.get(layer).isEnabled();
//    }
//    return false;
//  }

  // Enables/disables the checkbox associated with the specified layer
  private void setLayerEnabled(Layers layer, boolean enable, String toolTipText)
  {
    if (layer != null && layerButton.containsKey(layer)) {
      JCheckBox cb = layerButton.get(layer);
      if (!enable && cb.isSelected()) {
        cb.setSelected(false);
      }
      cb.setEnabled(enable);
      if (enable && toolTipText != null && !toolTipText.isEmpty()) {
        cb.setToolTipText(toolTipText);
      }
    }
  }

  // creates a simple titled frame
  private JPanel createGroupBox(String label, LayoutManager mgr)
  {
    JPanel panel = new JPanel(mgr);
    panel.setBorder(BorderFactory.createTitledBorder(label));
    return panel;
  }

//  // Returns the currently displayed area location (of the mouse cursor)
//  private Point getAreaLocation()
//  {
//    if (mapCoordinate == null) {
//      mapCoordinate = new Point();
//    }
//    return mapCoordinate;
//  }

  // Updates the area location in the information box
  private void setAreaLocation(Point loc)
  {
    if (mapCoordinate == null)
      mapCoordinate = new Point();

    if (loc != null) {
      if (lPosX != null && mapCoordinate.x != loc.x) {
        lPosX.setText(Integer.toString(loc.x));
        mapCoordinate.x = loc.x;
      }
      if (lPosY != null && mapCoordinate.y != loc.y) {
        lPosY.setText(Integer.toString(loc.y));
        mapCoordinate.y = loc.y;
      }
    }
  }

//  // Returns the information string in the information box
//  private String getInfoText()
//  {
//    if (taInfo != null) {
//      return taInfo.getText();
//    } else {
//      return new String();
//    }
//  }

  // Updates the information string in the information box
  private void setInfoText(String msg)
  {
    if (taInfo != null) {
      if (msg != null) {
        taInfo.setText(msg);
      } else {
        taInfo.setText("");
      }
    }
  }

  private void initMap()
  {
    createDayNight(DayNight.DAY, "Day");
    createDayNight(DayNight.NIGHT, "Night");

    // check for "Extended Night" flag
    Flag flags = (Flag)are.getAttribute("Location");
    boolean hasNight = flags.isFlagSet(6);

    try {
      // get "Day" WED resource
      ResourceRef wed = (ResourceRef)are.getAttribute("WED resource");
      ResourceEntry entry = ResourceFactory.getInstance().getResourceEntry(wed.getResourceName());
      if (!initWedData(DayNight.DAY, entry)) {
        return;
      }

      // get "Night WED resource
      if (hasNight) {
        String resName = wed.getResourceName();
        if (resName.lastIndexOf('.') > 0) {
          String resExt = resName.substring(resName.lastIndexOf('.'));
          resName = resName.substring(0, resName.lastIndexOf('.')) + "N" + resExt;
        } else {
          resName = resName + "N.WED";
        }
        entry = ResourceFactory.getInstance().getResourceEntry(resName);
        initWedData(DayNight.NIGHT, entry);
      }
    } catch (Exception e) {
      return;
    }

    dayNightButton.get(DayNight.DAY).setSelected(true);
    dayNightButton.get(DayNight.NIGHT).setEnabled(hasNight);
    setCurrentMap(DayNight.DAY);
  }


  private void initLayerActor()
  {
    addLayer(Layers.ACTOR);

    // initializing actor layer items
    List<StructEntry> listEntries = structure.getStructureList(Structure.ARE, Structure.ACTOR);
    if (listEntries == null)
      return;
    ArrayList<AbstractLayerItem> list = new ArrayList<AbstractLayerItem>(listEntries.size());
    final Image[] iconGood = new Image[]{Icons.getImage("ActorGreen.png"), Icons.getImage("ActorGreen_s.png")};
    final Image[] iconNeutral = new Image[]{Icons.getImage("ActorBlue.png"), Icons.getImage("ActorBlue_s.png")};
    final Image[] iconEvil = new Image[]{Icons.getImage("ActorRed.png"), Icons.getImage("ActorRed_s.png")};
    Point center = new Point(12, 40);
    for (int idx = 0; idx < listEntries.size(); idx++) {
      final Actor actor = (Actor)listEntries.get(idx);
      String msg;
      Image[] icon;
      Point location = new Point(0, 0);
      long ea;
      try {
        location.x = ((DecNumber)actor.getAttribute("Position: X")).getValue();
        location.y = ((DecNumber)actor.getAttribute("Position: Y")).getValue();
        StructEntry obj = actor.getAttribute("Character");
        CreResource cre = null;
        if (obj instanceof TextString) {
          // ARE in savegame
          cre = (CreResource)actor.getAttribute("CRE file");
        } else if (obj instanceof ResourceRef) {
          String creName = ((ResourceRef)obj).getResourceName();
          cre = new CreResource(ResourceFactory.getInstance().getResourceEntry(creName));
        }
        if (cre != null) {
          msg = ((StringRef)cre.getAttribute("Name")).toString();
          ea = ((IdsBitmap)cre.getAttribute("Allegiance")).getValue();
        } else
          throw new Exception();
        if (ea >= 2L && ea <= 30L) {
          icon = iconGood;
        } else if (ea >= 200) {
          icon = iconEvil;
        } else {
          icon = iconNeutral;
        }
      } catch (Throwable e) {
        msg = new String();
        icon = iconNeutral;
      }
      IconLayerItem item = new IconLayerItem(location, actor, msg, icon[0], center);
      item.setName(layerItemDesc.get(Layers.ACTOR));
      item.setToolTipText(msg);
      item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
      item.addActionListener(this);
      item.addLayerItemListener(this);
      item.addMouseListener(this);
      item.addMouseMotionListener(this);
      list.add(item);
    }
    addLayerItems(Layers.ACTOR, list);
    setLayerEnabled(Layers.ACTOR, !list.isEmpty(), list.size() + " actors available");
  }

  private void initLayerRegion()
  {
    addLayer(Layers.REGION);

    // initializing region layer items
    List<StructEntry> listEntries = structure.getStructureList(Structure.ARE, Structure.REGION);
    if (listEntries == null)
      return;
    final String[] type = new String[]{" (Proximity trigger)", " (Info point)", " (Travel region)"};
    final Color[] color = new Color[]{new Color(0xFF400000, true), new Color(0xFF400000, true),
                                      new Color(0xC0800000, true), new Color(0xC0C00000, true)};
    ArrayList<AbstractLayerItem> list = new ArrayList<AbstractLayerItem>(listEntries.size());
    for (int idx = 0; idx < listEntries.size(); idx++) {
      final ITEPoint region = (ITEPoint)listEntries.get(idx);
      String msg;
      Polygon poly = new Polygon();
      try {
        msg = ((TextString)region.getAttribute("Name")).toString();
        msg += type[((Bitmap)region.getAttribute("Type")).getValue()];
        int vertexIndex = ((DecNumber)region.getAttribute("First vertex index")).getValue();
        int vnum = ((DecNumber)region.getAttribute("# vertices")).getValue();
        for (int i = 0; i < vnum; i++) {
          Vertex vertex = (Vertex)structure.getStructureByIndex(Structure.ARE, Structure.VERTEX,
                                                                vertexIndex+i);
          if (vertex != null) {
            poly.addPoint(((DecNumber)vertex.getAttribute("X")).getValue(),
                          ((DecNumber)vertex.getAttribute("Y")).getValue());
          }
        }
      } catch (Throwable e) {
        msg = new String();
      }
      Rectangle rect = normalizePolygon(poly);
      ShapedLayerItem item = new ShapedLayerItem(new Point(rect.x, rect.y), region, msg, poly);
      item.setName(layerItemDesc.get(Layers.REGION));
      item.setToolTipText(msg);
      item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, color[0]);
      item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[1]);
      item.setFillColor(AbstractLayerItem.ItemState.NORMAL, color[2]);
      item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[3]);
      item.setStroked(true);
      item.setFilled(true);
      item.addActionListener(this);
      item.addLayerItemListener(this);
      item.addMouseListener(this);
      item.addMouseMotionListener(this);
      list.add(item);
    }
    addLayerItems(Layers.REGION, list);
    setLayerEnabled(Layers.REGION, !list.isEmpty(), list.size() + " regions available");
  }

  private void initLayerEntrance()
  {
    addLayer(Layers.ENTRANCE);

    // initializing entrance layer items
    List<StructEntry> listEntries = structure.getStructureList(Structure.ARE, Structure.ENTRANCE);
    if (listEntries == null)
      return;
    ArrayList<AbstractLayerItem> list = new ArrayList<AbstractLayerItem>(listEntries.size());
    final Image[] icon = new Image[]{Icons.getImage("Entrance.png"), Icons.getImage("Entrance_s.png")};
    Point center = new Point(11, 18);
    for (int idx = 0; idx < listEntries.size(); idx++) {
      final Entrance entrance = (Entrance)listEntries.get(idx);
      String msg;
      Point location = new Point(0, 0);
      try {
        location.x = ((DecNumber)entrance.getAttribute("Location: X")).getValue();
        location.y = ((DecNumber)entrance.getAttribute("Location: Y")).getValue();
        int o = ((Bitmap)entrance.getAttribute("Orientation")).getValue();
        msg = ((TextString)entrance.getAttribute("Name")).toString() +
              " (" + Actor.s_orientation[o] + ")";
      } catch (Throwable e) {
        msg = new String();
      }
      IconLayerItem item = new IconLayerItem(location, entrance, msg, icon[0], center);
      item.setName(layerItemDesc.get(Layers.ENTRANCE));
      item.setToolTipText(msg);
      item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
      item.addActionListener(this);
      item.addLayerItemListener(this);
      item.addMouseListener(this);
      item.addMouseMotionListener(this);
      list.add(item);
    }
    addLayerItems(Layers.ENTRANCE, list);
    setLayerEnabled(Layers.ENTRANCE, !list.isEmpty(), list.size() + " entrances available");
  }

  private void initLayerContainer()
  {
    addLayer(Layers.CONTAINER);

    // initializing container layer items
    List<StructEntry> listEntries = structure.getStructureList(Structure.ARE, Structure.CONTAINER);
    if (listEntries == null)
      return;
    final String[] s_type = new String[]{" (Unknown)", " (Bag)", " (Chest)", " (Drawer)", " (Pile)",
                                         " (Table)", " (Shelf)", " (Altar)", " (Invisible)",
                                         " (Spellbook)", " (Body)", " (Barrel)", " (Crate)"};
    final Color[] color = new Color[]{new Color(0xFF004040, true), new Color(0xFF004040, true),
                                      new Color(0xC0008080, true), new Color(0xC000C0C0, true)};
    ArrayList<AbstractLayerItem> list = new ArrayList<AbstractLayerItem>(listEntries.size());
    for (int idx = 0; idx < listEntries.size(); idx++) {
      final infinity.resource.are.Container container =  (infinity.resource.are.Container)listEntries.get(idx);
      String msg;
      Polygon poly = new Polygon();
      try {
        msg = ((TextString)container.getAttribute("Name")).toString();
        msg += s_type[((Bitmap)container.getAttribute("Type")).getValue()];
        int vertexIndex = ((DecNumber)container.getAttribute("First vertex index")).getValue();
        int vnum = ((DecNumber)container.getAttribute("# vertices")).getValue();
        for (int i = 0; i < vnum; i++) {
          Vertex vertex = (Vertex)structure.getStructureByIndex(Structure.ARE, Structure.VERTEX,
                                                                vertexIndex+i);
          if (vertex != null) {
            poly.addPoint(((DecNumber)vertex.getAttribute("X")).getValue(),
                          ((DecNumber)vertex.getAttribute("Y")).getValue());
          }
        }
      } catch (Throwable e) {
        msg = new String();
      }
      Rectangle rect = normalizePolygon(poly);
      ShapedLayerItem item = new ShapedLayerItem(new Point(rect.x, rect.y), container, msg, poly);
      item.setName(layerItemDesc.get(Layers.CONTAINER));
      item.setToolTipText(msg);
      item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, color[0]);
      item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[1]);
      item.setFillColor(AbstractLayerItem.ItemState.NORMAL, color[2]);
      item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[3]);
      item.setStroked(true);
      item.setFilled(true);
      item.addActionListener(this);
      item.addLayerItemListener(this);
      item.addMouseListener(this);
      item.addMouseMotionListener(this);
      list.add(item);
    }
    addLayerItems(Layers.CONTAINER, list);
    setLayerEnabled(Layers.CONTAINER, !list.isEmpty(), list.size() + " containers available");
  }

  private void initLayerAmbient()
  {
    addLayer(Layers.AMBIENT);

    // initializing ambient sound layer items
    List<StructEntry> listEntries = structure.getStructureList(Structure.ARE, Structure.AMBIENT);
    if (listEntries == null)
      return;
    ArrayList<AbstractLayerItem> list = new ArrayList<AbstractLayerItem>(listEntries.size());
    final Image[] icon = new Image[]{Icons.getImage("Ambient.png"), Icons.getImage("Ambient_s.png"),
                                      Icons.getImage("AmbientRanged.png"), Icons.getImage("AmbientRanged_s.png")};
    Point center = new Point(16, 16);
    for (int idx = 0; idx < listEntries.size(); idx++) {
      final Ambient ambient = (Ambient)listEntries.get(idx);
      String msg;
      Point location = new Point(0, 0);
      int iconBase;
      try {
        location.x = ((DecNumber)ambient.getAttribute("Origin: X")).getValue();
        location.y = ((DecNumber)ambient.getAttribute("Origin: Y")).getValue();
        iconBase = ((Flag)ambient.getAttribute("Flags")).isFlagSet(2) ? 0 : 2;
        msg = ((TextString)ambient.getAttribute("Name")).toString();
      } catch (Throwable e) {
        msg = new String();
        iconBase = 0;
      }
      IconLayerItem item = new IconLayerItem(location, ambient, msg, icon[iconBase + 0], center);
      item.setName(layerItemDesc.get(Layers.AMBIENT));
      item.setToolTipText(msg);
      item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[iconBase + 1]);
      item.addActionListener(this);
      item.addLayerItemListener(this);
      item.addMouseListener(this);
      item.addMouseMotionListener(this);
      list.add(item);
    }
    addLayerItems(Layers.AMBIENT, list);
    setLayerEnabled(Layers.AMBIENT, !list.isEmpty(), list.size() + " ambient sounds available");
  }

  private void initLayerAmbientRange()
  {
    addLayer(Layers.AMBIENTRANGE);

    // initializing ambient sound layer items
    List<StructEntry> listEntries = structure.getStructureList(Structure.ARE, Structure.AMBIENT);
    if (listEntries == null)
      return;
    ArrayList<AbstractLayerItem> list = new ArrayList<AbstractLayerItem>(listEntries.size());
    final Color[] color = new Color[]{new Color(0xA0000080, true), new Color(0xA0000080, true),
                                      new Color(0x00204080, true), new Color(0x004060C0, true)};
    for (int idx = 0; idx < listEntries.size(); idx++) {
      final Ambient ambient = (Ambient)listEntries.get(idx);
      String msg;
      Point location = new Point(0, 0);
      Ellipse2D.Float circle = null;
      int radius = 0;
      int volume = 0;
      try {
        location.x = ((DecNumber)ambient.getAttribute("Origin: X")).getValue();
        location.y = ((DecNumber)ambient.getAttribute("Origin: Y")).getValue();
        radius = ((DecNumber)ambient.getAttribute("Radius")).getValue();
        volume = ((DecNumber)ambient.getAttribute("Volume")).getValue();
        msg = ((TextString)ambient.getAttribute("Name")).toString();
        boolean global = ((Flag)ambient.getAttribute("Flags")).isFlagSet(2);
        if (!global && radius > 0) {
          circle = new Ellipse2D.Float(0, 0, (float)(2*radius), (float)(2*radius));
          double minAlpha = 0.0, maxAlpha = 64.0;
          double alphaF = minAlpha + Math.sqrt((double)volume) / 10.0 * (maxAlpha - minAlpha);
          int alphaNorm = (int)alphaF & 0xff;
          int alphaHigh = (int)alphaF & 0xff;
          color[2] = new Color(color[2].getRGB() | (alphaNorm << 24), true);
          color[3] = new Color(color[3].getRGB() | (alphaHigh << 24), true);
        }
      } catch (Throwable e) {
        msg = new String();
      }
      if (circle != null) {
        ShapedLayerItem item = new ShapedLayerItem(location, ambient, msg, circle, new Point(radius, radius));
        item.setName(layerItemDesc.get(Layers.AMBIENTRANGE));
        item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, color[0]);
        item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[1]);
        item.setFillColor(AbstractLayerItem.ItemState.NORMAL, color[2]);
        item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[3]);
        item.setStrokeWidth(AbstractLayerItem.ItemState.NORMAL, 2);
        item.setStrokeWidth(AbstractLayerItem.ItemState.HIGHLIGHTED, 2);
        item.setStroked(true);
        item.setFilled(true);
        item.addActionListener(this);
        item.addLayerItemListener(this);
        item.addMouseListener(this);
        item.addMouseMotionListener(this);
        list.add(item);
        item.setVisible(false);
        mapCanvas.add(item);
        item.setItemLocation(item.getMapLocation());
      }
    }
    addLayerItems(Layers.AMBIENTRANGE, list);
    setLayerEnabled(Layers.AMBIENTRANGE, !list.isEmpty(),
                    list.size() + " ambient sounds with local radius available");
  }

  private void initLayerDoor()
  {
    addLayer(Layers.DOOR);

    // initializing door layer items
    List<StructEntry> listEntries = structure.getStructureList(Structure.ARE, Structure.DOOR);
    if (listEntries == null)
      return;
    final Color[] color = new Color[]{new Color(0xFF400040, true), new Color(0xFF400040, true),
                                      new Color(0xC0800080, true), new Color(0xC0C000C0, true)};
    ArrayList<AbstractLayerItem> list = new ArrayList<AbstractLayerItem>(2*listEntries.size());
    for (int idx = 0; idx < listEntries.size(); idx++) {
      final Door door = (Door)listEntries.get(idx);
      Polygon[] poly = new Polygon[]{new Polygon(), new Polygon()};
      String[] msg = {null, null};
      try {
        msg[0] = ((TextString)door.getAttribute("Name")).toString() + " (Open)";
        int vertexIndex = ((DecNumber)door.getAttribute("First vertex index (open)")).getValue();
        int vnum = ((DecNumber)door.getAttribute("# vertices (open)")).getValue();
        for (int i = 0; i < vnum; i++) {
          Vertex vertex = (Vertex)structure.getStructureByIndex(Structure.ARE, Structure.VERTEX,
                                                                vertexIndex+i);
          if (vertex != null) {
            poly[0].addPoint(((DecNumber)vertex.getAttribute("X")).getValue(),
                          ((DecNumber)vertex.getAttribute("Y")).getValue());
          }
        }
        msg[1] = ((TextString)door.getAttribute("Name")).toString() + " (Closed)";
        vertexIndex = ((DecNumber)door.getAttribute("First vertex index (closed)")).getValue();
        vnum = ((DecNumber)door.getAttribute("# vertices (closed)")).getValue();
        for (int i = 0; i < vnum; i++) {
          Vertex vertex = (Vertex)structure.getStructureByIndex(Structure.ARE, Structure.VERTEX,
                                                                vertexIndex+i);
          if (vertex != null) {
            poly[1].addPoint(((DecNumber)vertex.getAttribute("X")).getValue(),
                          ((DecNumber)vertex.getAttribute("Y")).getValue());
          }
        }
      } catch (Throwable e) {
        msg[0] = new String();
        msg[1] = new String();
      }

      // adding open/closed door items
      for (int i = 0; i < poly.length; i++) {
        Rectangle rect = normalizePolygon(poly[i]);
        ShapedLayerItem item = new ShapedLayerItem(new Point(rect.x, rect.y), door, msg[i], poly[i]);
        item.setName(layerItemDesc.get(Layers.DOOR));
        item.setToolTipText(msg[i]);
        item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, color[0]);
        item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[1]);
        item.setFillColor(AbstractLayerItem.ItemState.NORMAL, color[2]);
        item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[3]);
        item.setStroked(true);
        item.setFilled(true);
        item.addActionListener(this);
        item.addLayerItemListener(this);
        item.addMouseListener(this);
        item.addMouseMotionListener(this);
        list.add(item);
      }
    }
    addLayerItems(Layers.DOOR, list);
    setLayerEnabled(Layers.DOOR, !listEntries.isEmpty(), listEntries.size() + " doors available");
  }

  private void initLayerAnimation()
  {
    addLayer(Layers.ANIMATION);

    // initializing background animation layer items
    List<StructEntry> listEntries = structure.getStructureList(Structure.ARE, Structure.ANIMATION);
    if (listEntries == null)
      return;
    ArrayList<AbstractLayerItem> list = new ArrayList<AbstractLayerItem>(listEntries.size());
    final Image[] icon = new Image[]{Icons.getImage("Animation.png"), Icons.getImage("Animation_s.png")};
    Point center = new Point(16, 17);
    for (int idx = 0; idx < listEntries.size(); idx++) {
      final Animation animation = (Animation)listEntries.get(idx);
      String msg;
      Point location = new Point(0, 0);
      try {
        location.x = ((DecNumber)animation.getAttribute("Location: X")).getValue();
        location.y = ((DecNumber)animation.getAttribute("Location: Y")).getValue();
        msg = ((TextString)animation.getAttribute("Name")).toString();
      } catch (Throwable e) {
        msg = new String();
      }
      IconLayerItem item = new IconLayerItem(location, animation, msg, icon[0], center);
      item.setName(layerItemDesc.get(Layers.ANIMATION));
      item.setToolTipText(msg);
      item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
      item.addActionListener(this);
      item.addLayerItemListener(this);
      item.addMouseListener(this);
      item.addMouseMotionListener(this);
      list.add(item);
    }
    addLayerItems(Layers.ANIMATION, list);
    setLayerEnabled(Layers.ANIMATION, !list.isEmpty(), list.size() + " animations available");
  }

  private void initLayerAutomap()
  {
    addLayer(Layers.AUTOMAP);

    // initializing automap notes layer items
    List<StructEntry> listEntries = structure.getStructureList(Structure.ARE, Structure.AUTOMAP);
    if (listEntries == null)
      return;
    ArrayList<AbstractLayerItem> list = new ArrayList<AbstractLayerItem>(listEntries.size());
    final Image[] icon = new Image[]{Icons.getImage("Automap.png"), Icons.getImage("Automap_s.png")};
    Point center = new Point(26, 26);
    if (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT) {
      final double mapScale = 32.0 / 3.0;   // scaling factor for MOS to TIS coordinates
      for (int idx = 0; idx < listEntries.size(); idx++) {
        final AutomapNotePST automap = (AutomapNotePST)listEntries.get(idx);
        String msg;
        Point location = new Point(0, 0);
        try {
          int v = ((DecNumber)automap.getAttribute("Coordinate: X")).getValue();
          location.x = (int)((double)v * mapScale);
          v = ((DecNumber)automap.getAttribute("Coordinate: Y")).getValue();
          location.y = (int)((double)v * mapScale);
          msg = ((TextString)automap.getAttribute("Text")).toString();
        } catch (Throwable e) {
          msg = new String();
        }
        IconLayerItem item = new IconLayerItem(location, automap, msg, icon[0], center);
        item.setName(layerItemDesc.get(Layers.AUTOMAP));
        item.setToolTipText(msg);
        item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
        item.addActionListener(this);
        item.addLayerItemListener(this);
        item.addMouseListener(this);
        item.addMouseMotionListener(this);
        list.add(item);
      }
    } else {
      for (int idx = 0; idx < listEntries.size(); idx++) {
        final AutomapNote automap = (AutomapNote)listEntries.get(idx);
        String msg;
        Point location = new Point(0, 0);
        try {
          // fetching automap note string from dialog.tlk
          location.x = ((DecNumber)automap.getAttribute("Coordinate: X")).getValue();
          location.y = ((DecNumber)automap.getAttribute("Coordinate: Y")).getValue();
          if (((Bitmap)automap.getAttribute("Text location")).getValue() == 1)
            msg = ((StringRef)automap.getAttribute("Text")).toString();
          else {
            // fetching automap note string from Talk Override
            msg = "[user-defined note]";
            try {
              int srcStrref = ((StringRef)automap.getAttribute("Text")).getValue();
              if (srcStrref > 0) {
                String filePath = are.getResourceEntry().getActualFile().toString();
                filePath = filePath.replace(are.getResourceEntry().getResourceName(), "");
                File tohFile = new File(filePath + "DEFAULT.TOH");
                File totFile = new File(filePath + "DEFAULT.TOT");
                if (tohFile.exists() && totFile.exists()) {
                  FileResourceEntry tohEntry = new FileResourceEntry(tohFile);
                  FileResourceEntry totEntry = new FileResourceEntry(totFile);
                  TohResource toh = new TohResource(tohEntry);
                  TotResource tot = new TotResource(totEntry);
                  SectionCount sc = (SectionCount)toh.getAttribute("# strref entries");
                  int totIndex = -1;
                  if (sc != null && sc.getValue() > 0) {
                    for (int i = 0; i < sc.getValue(); i++) {
                      StrRefEntry strref = (StrRefEntry)toh.getAttribute("StrRef entry " + i);
                      int v = ((StringRef)strref.getAttribute("Overridden strref")).getValue();
                      if (v == srcStrref) {
                        totIndex = i;
                        break;
                      }
                    }
                    if (totIndex >= 0) {
                      StringEntry se = (StringEntry)tot.getAttribute("String entry " + totIndex);
                      if (se != null) {
                        TextEdit te = (TextEdit)se.getAttribute("String data");
                        if (te != null) {
                          msg = te.toString();
                        }
                      }
                    }
                  }
                }
              }
            } catch (Exception e) {
            }
          }
        } catch (Throwable e) {
          msg = new String();
        }
        IconLayerItem item = new IconLayerItem(location, automap, msg, icon[0], center);
        item.setName(layerItemDesc.get(Layers.AUTOMAP));
        item.setToolTipText(msg);
        item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
        item.addActionListener(this);
        item.addMouseListener(this);
        item.addMouseMotionListener(this);
        item.addLayerItemListener(this);
        list.add(item);
      }
    }
    addLayerItems(Layers.AUTOMAP, list);
    setLayerEnabled(Layers.AUTOMAP, !list.isEmpty(), list.size() + " automap notes available");
  }

  private void initLayerTransition()
  {
    addLayer(Layers.TRANSITION);

    // initializing transition objects
    EnumMap<AreaEdge, Resource> listTransitions = new EnumMap<AreaEdge, Resource>(AreaEdge.class);
    EnumMap<AreaEdge, String> attrMap = new EnumMap<AreaEdge, String>(AreaEdge.class);
    attrMap.put(AreaEdge.NORTH, "Area north");
    attrMap.put(AreaEdge.EAST, "Area east");
    attrMap.put(AreaEdge.SOUTH, "Area south");
    attrMap.put(AreaEdge.WEST, "Area west");
    for (final AreaEdge edge: AreaEdge.values()) {
      ResourceRef res = (ResourceRef)are.getAttribute(attrMap.get(edge));
      if (res != null && !res.getResourceName().equalsIgnoreCase("NONE.ARE")) {
        ResourceEntry entry = ResourceFactory.getInstance().getResourceEntry(res.getResourceName());
        if (entry != null) {
          listTransitions.put(edge, ResourceFactory.getResource(entry));
        }
      }
    }

    // initializing transition layer items
    ArrayList<AbstractLayerItem> list = new ArrayList<AbstractLayerItem>(listTransitions.size());
    final Color[] color = new Color[]{new Color(0xFF404000, true), new Color(0xFF404000, true),
                                      new Color(0xC0808000, true), new Color(0xC0C0C000, true)};
    Dimension mapTilesDim = getMapSize(getCurrentMap());
    Dimension mapDim = new Dimension(mapTilesDim.width*getTisDecoder().info().tileWidth(),
                                     mapTilesDim.height*getTisDecoder().info().tileHeight());
    EnumMap<AreaEdge, Rectangle> rectMap = new EnumMap<AreaEdge, Rectangle>(AreaEdge.class);
    rectMap.put(AreaEdge.NORTH, new Rectangle(0, 0, mapDim.width, 16));
    rectMap.put(AreaEdge.EAST, new Rectangle(mapDim.width - 16, 0, 16, mapDim.height));
    rectMap.put(AreaEdge.SOUTH, new Rectangle(0, mapDim.height - 16, mapDim.width, 16));
    rectMap.put(AreaEdge.WEST, new Rectangle(0, 0, 16, mapDim.height));
    for (final AreaEdge edge: AreaEdge.values()) {
      if (listTransitions.containsKey(edge)) {
        Resource resource = listTransitions.get(edge);
        String msg = "Transition to " + resource.getResourceEntry().getResourceName();
        Polygon poly = new Polygon();
        poly.addPoint(0, 0);
        poly.addPoint(rectMap.get(edge).width, 0);
        poly.addPoint(rectMap.get(edge).width, rectMap.get(edge).height);
        poly.addPoint(0, rectMap.get(edge).height);
        ShapedLayerItem item = new ShapedLayerItem(new Point(rectMap.get(edge).x, rectMap.get(edge).y),
                                                   resource, msg, poly);
        item.setName(layerItemDesc.get(Layers.TRANSITION));
        item.setToolTipText(msg);
        item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, color[0]);
        item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[1]);
        item.setFillColor(AbstractLayerItem.ItemState.NORMAL, color[2]);
        item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[3]);
        item.setStroked(true);
        item.setFilled(true);
        item.addActionListener(this);
        item.addLayerItemListener(this);
        item.addMouseListener(this);
        item.addMouseMotionListener(this);
        list.add(item);
      }
    }
    addLayerItems(Layers.TRANSITION, list);
    setLayerEnabled(Layers.TRANSITION, !list.isEmpty(), list.size() + " map transitions available");
  }

  private void initLayerProTrap()
  {
    addLayer(Layers.PROTRAP);

    // initializing projectile trap layer items
    List<StructEntry> listEntries = structure.getStructureList(Structure.ARE, Structure.PROTRAP);
    if (listEntries == null)
      return;
    ArrayList<AbstractLayerItem> list = new ArrayList<AbstractLayerItem>(listEntries.size());
    final Image[] icon = new Image[]{Icons.getImage("ProTrap.png"), Icons.getImage("ProTrap_s.png")};
    Point center = new Point(14, 14);
    for (int idx = 0; idx < listEntries.size(); idx++) {
      final ProTrap trap = (ProTrap)listEntries.get(idx);
      String msg;
      Point location = new Point(0, 0);
      try {
        location.x = ((DecNumber)trap.getAttribute("Location: X")).getValue();
        location.y = ((DecNumber)trap.getAttribute("Location: Y")).getValue();
        msg = ((ResourceRef)trap.getAttribute("Trap")).toString();
        int target = ((DecNumber)trap.getAttribute("Target")).getValue() & 0xff;
        if (target >= 2 && target <= 30) {
          msg += " (hostile)";
        } else if (target >= 200) {
          msg += " (friendly)";
        }
      } catch (Throwable e) {
        msg = new String();
      }
      IconLayerItem item = new IconLayerItem(location, trap, msg, icon[0], center);
      item.setName(layerItemDesc.get(Layers.PROTRAP));
      item.setToolTipText(msg);
      item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
      item.addActionListener(this);
      item.addLayerItemListener(this);
      item.addMouseListener(this);
      item.addMouseMotionListener(this);
      list.add(item);
    }
    addLayerItems(Layers.PROTRAP, list);
    setLayerEnabled(Layers.PROTRAP, !list.isEmpty(), list.size() + " projectile traps available");
  }

  private void initLayerSpawnPoint()
  {
    addLayer(Layers.SPAWNPOINT);

    // initializing spawn point layer items
    List<StructEntry> listEntries = structure.getStructureList(Structure.ARE, Structure.SPAWNPOINT);
    if (listEntries == null)
      return;
    ArrayList<AbstractLayerItem> list = new ArrayList<AbstractLayerItem>(listEntries.size());
    final Image[] icon = new Image[]{Icons.getImage("SpawnPoint.png"), Icons.getImage("SpawnPoint_s.png")};
    Point center = new Point(22, 22);
    for (int idx = 0; idx < listEntries.size(); idx++) {
      final SpawnPoint spawn = (SpawnPoint)listEntries.get(idx);
      String msg;
      Point location = new Point(0, 0);
      try {
        location.x = ((DecNumber)spawn.getAttribute("Location: X")).getValue();
        location.y = ((DecNumber)spawn.getAttribute("Location: Y")).getValue();
        msg = ((TextString)spawn.getAttribute("Name")).toString();
      } catch (Throwable e) {
        msg = new String();
      }
      IconLayerItem item = new IconLayerItem(location, spawn, msg, icon[0], center);
      item.setName(layerItemDesc.get(Layers.SPAWNPOINT));
      item.setToolTipText(msg);
      item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
      item.addActionListener(this);
      item.addLayerItemListener(this);
      item.addMouseListener(this);
      item.addMouseMotionListener(this);
      list.add(item);
    }
    addLayerItems(Layers.SPAWNPOINT, list);
    setLayerEnabled(Layers.SPAWNPOINT, !list.isEmpty(), list.size() + " spawn points available");
  }

  private void initLayerDoorPoly()
  {
    addLayer(Layers.DOORPOLY);

    // initializing door polygon layer items
    List<StructEntry> listEntries = structure.getStructureList(Structure.WED, Structure.DOOR);
    if (listEntries == null)
      return;
    Color[] color = new Color[]{new Color(0xFF603080, true), new Color(0xFF603080, true),
                                new Color(0x80A050C0, true), new Color(0xC0C060D0, true)};
    ArrayList<AbstractLayerItem> listDoor = new ArrayList<AbstractLayerItem>(2*listEntries.size());
    for (int idx = 0; idx < listEntries.size(); idx++) {
      final infinity.resource.wed.Door door = (infinity.resource.wed.Door)listEntries.get(idx);
      try {
        int ofsOpen = ((SectionOffset)door.getAttribute("Polygons open offset")).getValue();
        int numOpen = ((SectionCount)door.getAttribute("# polygons open")).getValue();
        int ofsClosed = ((SectionOffset)door.getAttribute("Polygons closed offset")).getValue();
        int numClosed = ((SectionCount)door.getAttribute("# polygons closed")).getValue();
        int numDoorPairs = (numOpen > numClosed) ? numOpen : numClosed;
        for (int i = 0; i < numDoorPairs; i++) {
          String msg = ((TextString)door.getAttribute("Name")).toString();
          if (numDoorPairs > 1) {
            msg += " #" + i;
          }
          String[] msg2 = {null, null};
          Polygon[] poly = new Polygon[]{new Polygon(), new Polygon()};
          infinity.resource.wed.Polygon[] dp = {null, null};

          // open polygon
          if (numOpen > i) {
            dp[0] = (infinity.resource.wed.Polygon)structure.getStructureByOffset(Structure.WED,
                                                                                  Structure.DOORPOLY,
                                                                                  ofsOpen);
            if (dp[0] != null) {
              ofsOpen += dp[0].getSize();
              msg2[0] = msg + " " + createFlags((Flag)dp[0].getAttribute("Polygon flags"),
                                                 infinity.resource.wed.Polygon.s_flags) +
                        " (Open)";
              int numVertices = ((SectionCount)dp[0].getAttribute("# vertices")).getValue();
              for (int j = 0; j < numVertices; j++) {
                Vertex vertex = ((Vertex)dp[0].getAttribute("Vertex " + j));
                if (vertex != null) {
                  poly[0].addPoint(((DecNumber)vertex.getAttribute("X")).getValue(),
                                   ((DecNumber)vertex.getAttribute("Y")).getValue());
                }
              }
            }
          }

          // closed polygon
          if (numClosed > i) {
            dp[1] = (infinity.resource.wed.Polygon)structure.getStructureByOffset(Structure.WED,
                                                                                  Structure.DOORPOLY,
                                                                                  ofsClosed);
            if (dp[1] != null) {
              ofsClosed += dp[1].getSize();
              msg2[1] = msg + " " + createFlags((Flag)dp[1].getAttribute("Polygon flags"),
                                                infinity.resource.wed.Polygon.s_flags) +
                        " (Closed)";
              int numVertices = ((SectionCount)dp[1].getAttribute("# vertices")).getValue();
              for (int j = 0; j < numVertices; j++) {
                Vertex vertex = ((Vertex)dp[1].getAttribute("Vertex " + j));
                if (vertex != null) {
                  poly[1].addPoint(((DecNumber)vertex.getAttribute("X")).getValue(),
                                   ((DecNumber)vertex.getAttribute("Y")).getValue());
                }
              }
            }
          }

          // adding to item list
          for (int j = 0; j < dp.length; j++) {
            if (dp[j] != null) {
              Rectangle rect = normalizePolygon(poly[j]);
              ShapedLayerItem item = new ShapedLayerItem(new Point(rect.x, rect.y), door, msg2[j], poly[j]);
              item.setName(layerItemDesc.get(Layers.DOORPOLY));
              item.setToolTipText(msg2[j]);
              item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, color[0]);
              item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[1]);
              item.setFillColor(AbstractLayerItem.ItemState.NORMAL, color[2]);
              item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[3]);
              item.setStroked(true);
              item.setFilled(true);
              item.addActionListener(this);
              item.addLayerItemListener(this);
              item.addMouseListener(this);
              item.addMouseMotionListener(this);
              listDoor.add(item);
            } else {
              listDoor.add(null);
            }
          }
        }
      } catch (Exception e) {
      }
    }
    addLayerItems(Layers.DOORPOLY, listDoor);
    setLayerEnabled(Layers.DOORPOLY, !listDoor.isEmpty(), (listDoor.size() / 2) + " door polygons available");
  }

  private void initLayerWallPoly()
  {
    addLayer(Layers.WALLPOLY);

    // initializing wall polygon layer items
    List<StructEntry> listEntries = structure.getStructureList(Structure.WED, Structure.WALLPOLY);
    if (listEntries == null)
      return;
    Color[] color = new Color[]{new Color(0xFF005046, true), new Color(0xFF005046, true),
                                new Color(0x8020A060, true), new Color(0xA030B070, true)};
    ArrayList<AbstractLayerItem> listWall = new ArrayList<AbstractLayerItem>(listEntries.size());
    int count = 0;
    for (int idx = 0; idx < listEntries.size(); idx++) {
      final infinity.resource.wed.Polygon wp = (infinity.resource.wed.Polygon)listEntries.get(idx);
      String msg;
      Polygon poly = new Polygon();
      try {
        msg = "Wall polygon #" + count + " " + createFlags((Flag)wp.getAttribute("Polygon flags"),
                                                           infinity.resource.wed.Polygon.s_flags);
        int vertexIndex = ((DecNumber)wp.getAttribute("Vertex index")).getValue();
        int numVertices = ((SectionCount)wp.getAttribute("# vertices")).getValue();
        for (int i = 0; i < numVertices; i++) {
          Vertex vertex = (Vertex)structure.getStructureByIndex(Structure.WED, Structure.VERTEX,
                                                                vertexIndex + i);
          if (vertex != null) {
            poly.addPoint(((DecNumber)vertex.getAttribute("X")).getValue(),
                          ((DecNumber)vertex.getAttribute("Y")).getValue());
          }
        }
      } catch (Exception e) {
        msg = new String();
      }
      count++;
      Rectangle rect = normalizePolygon(poly);
      ShapedLayerItem item = new ShapedLayerItem(new Point(rect.x, rect.y), wp, msg, poly);
      item.setName(layerItemDesc.get(Layers.WALLPOLY));
      item.setToolTipText(msg);
      item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, color[0]);
      item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[1]);
      item.setFillColor(AbstractLayerItem.ItemState.NORMAL, color[2]);
      item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[3]);
      item.setStroked(true);
      item.setFilled(true);
      item.addActionListener(this);
      item.addLayerItemListener(this);
      item.addMouseListener(this);
      item.addMouseMotionListener(this);
      listWall.add(item);
    }
    addLayerItems(Layers.WALLPOLY, listWall);
    setLayerEnabled(Layers.WALLPOLY, !listWall.isEmpty(), listWall.size() + " wall polygons available");
  }

  private void enableLayer(Layers layer, boolean enable)
  {
    if (layer != null && layerItems.containsKey(layer)) {
      List<AbstractLayerItem> list = layerItems.get(layer);
      for (int i = 0; i < list.size(); i++) {
        AbstractLayerItem item = list.get(i);
        if (item != null) {
          item.setVisible(isExtendedLayerItemActive(layer, i) && enable);
        }
      }
      LayerButtonState.put(layer, enable);
    }
  }

  // Returns whether the items of the specified layer consist of more than one logical item
  private boolean isExtendedLayerItem(Layers layer)
  {
    return (layer == Layers.DOOR || layer == Layers.DOORPOLY);
  }

  // Returns whether the layer item of the specified list index is currently active
  private boolean isExtendedLayerItemActive(Layers layer, int itemIndex)
  {
    if (isExtendedLayerItem(layer)) {
      return ((itemIndex & 1) == 1) == drawDoorsClosed();
    } else {
      return true;
    }
  }

  // Translates polygon to top-left corner and returns original bounding box
  private Rectangle normalizePolygon(Polygon poly)
  {
    if (poly != null) {
      Rectangle r = poly.getBounds();
      poly.translate(-r.x, -r.y);
      return r;
    }
    return new Rectangle();
  }

  // Returns the BufferedImage object containing the pixel data of the currently shown tileset
  private BufferedImage getCurrentMapImage()
  {
    if (mapCanvas != null) {
      return ColorConvert.toBufferedImage(mapCanvas.getImage(), false);
    }
    return null;
  }

  private DayNight getCurrentMap()
  {
    return currentMap;
  }

  // Draws the complete map
  private boolean setCurrentMap(DayNight dn)
  {
    if (dn != null &&
        dn != getCurrentMap()  &&
        dayNightWed.containsKey(dn) &&
        dayNightTiles.containsKey(dn) &&
        dayNightDoorIndices.containsKey(dn)) {
      try {
        WindowBlocker.blockWindow(this, true);
        setTisDecoder(dn);
        Dimension mapTilesDim = getMapSize(dn);
        Dimension mapDim = new Dimension(mapTilesDim.width*getTisDecoder().info().tileWidth(),
                                         mapTilesDim.height*getTisDecoder().info().tileHeight());

        BufferedImage img = getCurrentMapImage();
        if (img == null || img.getWidth() != mapDim.width || img.getHeight() != mapDim.height) {
          mapCanvas.setImage(null);
          mapCanvas.setSize(mapDim);
          // creating new image object
          img = ColorConvert.createCompatibleImage(mapDim.width, mapDim.height, false);
          mapCanvas.setImage(img);
        } else {
          img.flush();
        }

        // drawing map tiles
        drawTiles(img, getTisDecoder(), mapTilesDim.width, mapTilesDim.height,
                  dayNightTiles.get(dn));

        // drawing opened/closed door tiles
        drawDoorTiles(img, getTisDecoder(), mapTilesDim.width, mapTilesDim.height,
                      dayNightTiles.get(dn), dayNightDoorIndices.get(dn),
                      drawDoorsClosed());

        img = null;
        currentMap = dn;
        mapCanvas.repaint();
        WindowBlocker.blockWindow(this, false);

        return true;
      } catch (Exception e) {
        WindowBlocker.blockWindow(this, false);
        e.printStackTrace();
      }
    }
    return false;
  }

  // Draws the doors onto the current map image in the specified opened/closed state
  private boolean setDoorState(DayNight dn, boolean drawClosed)
  {
    if (dn != null) {
      try {
        Dimension mapTilesDim = getMapSize(dn);
        Dimension mapDim = new Dimension(mapTilesDim.width*getTisDecoder().info().tileWidth(),
            mapTilesDim.height*getTisDecoder().info().tileHeight());

        BufferedImage img = getCurrentMapImage();
        if (img != null) {
          if (img.getWidth() >= mapDim.width && img.getHeight() >= mapDim.height) {
            // drawing opened/closed door tiles
            drawDoorTiles(img, getTisDecoder(), mapTilesDim.width, mapTilesDim.height,
                          dayNightTiles.get(dn), dayNightDoorIndices.get(dn),
                          drawDoorsClosed());

            img = null;
            mapCanvas.repaint();
            return true;
          }
          img = null;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return false;
  }

  // Returns the width and height of the specified map in tiles
  private Dimension getMapSize(DayNight dn)
  {
    Dimension d = new Dimension();
    if (dn != null) {
      WedResource wed = dayNightWed.get(dn);
      Overlay ovl = (Overlay)wed.getAttribute("Overlay 0");
      if (ovl != null) {
        d.width = ((DecNumber)ovl.getAttribute("Width")).getValue();
        d.height = ((DecNumber)ovl.getAttribute("Height")).getValue();
      }
    }
    return d;
  }

  private ResourceEntry getTisResource(DayNight dn)
  {
    if (dn != null && dayNightWed.containsKey(dn)) {
      WedResource wed = dayNightWed.get(dn);
      Overlay ovl = (Overlay)wed.getAttribute("Overlay 0");
      if (ovl != null) {
        ResourceRef tisRef = (ResourceRef)ovl.getAttribute("Tileset");
        if (tisRef != null) {
          return ResourceFactory.getInstance().getResourceEntry(tisRef.getResourceName());
        }
      }
    }
    return null;
  }

  private TisDecoder getTisDecoder()
  {
    return tisDecoder;
  }

  // re-initializes the TIS decoder object with the TIS associated with the Day/Night WED
  private void setTisDecoder(DayNight dn)
  {
    if (dn != getCurrentMap()) {
      try {
        if (tisDecoder == null) {
          tisDecoder = new TisDecoder();
        }
        tisDecoder.open(getTisResource(dn));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  // Initialize all relevant data to draw a map
  private boolean initWedData(DayNight dn, ResourceEntry entry)
  {
    if (dn != null && entry != null) {
      try {
        WedResource wed = new WedResource(entry);
        byte[] buffer = entry.getResourceData();

        // getting tile indices
        Overlay ovl = (Overlay)wed.getAttribute("Overlay 0");
        ResourceRef tisRef = (ResourceRef)ovl.getAttribute("Tileset");
        ResourceEntry tisEntry = ResourceFactory.getInstance().getResourceEntry(tisRef.getResourceName());
        if (tisEntry == null)
          throw new Exception("TIS resource not found: " + tisRef.getResourceName());
        int width = ((DecNumber)ovl.getAttribute("Width")).getValue();
        int height = ((DecNumber)ovl.getAttribute("Height")).getValue();

        ArrayList<TileInfo> listTiles = new ArrayList<TileInfo>(width*height);
        int tileNum = width*height;
        for (int idx = 0; idx < tileNum; idx++) {
          int mask, tileIdx, tileIdxAlt;
          try {
            Tilemap tm = ((Tilemap)ovl.getAttribute("Tilemap " + idx));
            int pti = ((DecNumber)tm.getAttribute("Primary tile index")).getValue();
            int sti = ((DecNumber)tm.getAttribute("Secondary tile index")).getValue();
            Flag f = (Flag)tm.getAttribute("Draw Overlays");
            mask = 0;
            for (int i = 0; i < 8; i++) {
              if (f.isFlagSet(i))
                mask |= (1 << i);
            }
            tileIdx = ((DecNumber)ovl.getAttribute("Tilemap index " + pti)).getValue();
            tileIdxAlt = sti;
          } catch (Exception e) {
            tileIdx = idx;
            tileIdxAlt = -1;
            mask = 0;
          }
          TileInfo info = new TileInfo(idx % width, idx / width, tileIdx, tileIdxAlt, mask);
          listTiles.add(info);
        }
        dayNightTiles.put(dn, listTiles);

        // getting door tile indices
        int doorOfs = ((SectionOffset)wed.getAttribute("Doors offset")).getValue();
        int doorNum = ((SectionCount)wed.getAttribute("# doors")).getValue();
        ArrayList<Integer> listDoorTiles = new ArrayList<Integer>();
        int curDoorOfs = doorOfs;
        int doorSize = 0x1a;
        for (int i = 0; i < doorNum; i++, curDoorOfs += doorSize) {
          infinity.resource.wed.Door door = new infinity.resource.wed.Door(wed, buffer, curDoorOfs, i);
          boolean isClosed = ((Bitmap)door.getAttribute("Is door?")).getValue() == 1;
          if (isClosed) {
            int idxCount = ((SectionCount)door.getAttribute("# tilemap indexes")).getValue();
            for (int idx = 0; idx < idxCount; idx++) {
              int tile = ((RemovableDecNumber)door.getAttribute("Tilemap index " + idx)).getValue();
              if (tile > 0)
                listDoorTiles.add(new Integer(tile));
            }
          }
        }
        dayNightDoorIndices.put(dn, listDoorTiles);

        dayNightWed.put(dn, wed);
        dayNightButton.get(dn).setEnabled(true);
        return true;
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
    }
    return false;
  }

  // Returns whether closed door are currently shown
  private boolean drawDoorsClosed()
  {
    if (cbDrawClosed != null) {
      return cbDrawClosed.isSelected();
    } else
      return false;
  }

  private void initProgressMonitor(Component parent, String msg, int maxProgress, int decide, int wait)
  {
    if (progressMonitor != null)
      progressMonitor.close();
    if (parent == null)
      parent = NearInfinity.getInstance();

    if (maxProgress <= 0)
      maxProgress = 1;
    pmMax = maxProgress;
    pmCur = 0;
    progressMonitor = new ProgressMonitor(parent, msg, "", 0, pmMax);
    progressMonitor.setMillisToDecideToPopup(decide);
    progressMonitor.setMillisToPopup(wait);
    progressMonitor.setProgress(0);
  }

  private void releaseProgressMonitor()
  {
    if (progressMonitor != null) {
      progressMonitor.close();
      progressMonitor = null;
    }
  }

  private void advanceProgressMonitor(String note)
  {
    if (progressMonitor != null) {
      if (pmCur < pmMax) {
        pmCur++;
        if (note != null)
          progressMonitor.setNote(note);
        progressMonitor.setProgress(pmCur);
      }
    }
  }

  // Returns whether map dragging is enabled; updates current and previous mouse positions
  private boolean isMapDragging(Point mousePos)
  {
    if (bMapDragging && mousePos != null && !mapDraggingPosCurrent.equals(mousePos)) {
      mapDraggingPosCurrent.x = mousePos.x;
      mapDraggingPosCurrent.y = mousePos.y;
    }
    return bMapDragging;
  }

  // Enables/Disables map dragging mode (set mouse cursor, global state and current mouse position)
  private void setMapDraggingEnabled(boolean enable, Point mousePos)
  {
    if (bMapDragging != enable) {
      bMapDragging = enable;
      setCursor(Cursor.getPredefinedCursor(bMapDragging ? Cursor.MOVE_CURSOR : Cursor.DEFAULT_CURSOR));
      if (bMapDragging == true) {
        if (mousePos == null) {
          mousePos = new Point();
        }
        mapDraggingPosStart.x = mapDraggingPosCurrent.x = mousePos.x;
        mapDraggingPosStart.y = mapDraggingPosCurrent.y = mousePos.y;
        mapDraggingScrollStart.x = spView.getHorizontalScrollBar().getModel().getValue();
        mapDraggingScrollStart.y = spView.getVerticalScrollBar().getModel().getValue();
      }
    }
  }

  // Returns the current or previous mouse position
  private Point getMapDraggingDistance()
  {
    Point pDelta = new Point();
    if (bMapDragging) {
      pDelta.x = mapDraggingPosStart.x - mapDraggingPosCurrent.x;
      pDelta.y = mapDraggingPosStart.y - mapDraggingPosCurrent.y;
    }
    return pDelta;
  }

  // Updates the map portion displayed in the viewport
  private void moveMapViewport()
  {
    if (!mapDraggingPosStart.equals(mapDraggingPosCurrent)) {
      Point distance = getMapDraggingDistance();
      JViewport vp = spView.getViewport();
      Point curPos = vp.getViewPosition();
      Dimension curDim = vp.getExtentSize();
      Dimension maxDim = new Dimension(spView.getHorizontalScrollBar().getMaximum(),
                                       spView.getVerticalScrollBar().getMaximum());
      if (curDim.width < maxDim.width) {
        curPos.x = mapDraggingScrollStart.x + distance.x;
        if (curPos.x < 0)
          curPos.x = 0;
        if (curPos.x + curDim.width > maxDim.width)
          curPos.x = maxDim.width - curDim.width;
      }
      if (curDim.height < maxDim.height) {
        curPos.y = mapDraggingScrollStart.y + distance.y;
        if (curPos.y < 0)
          curPos.y = 0;
        if (curPos.y + curDim.height > maxDim.height)
          curPos.y = maxDim.height - curDim.height;
      }
      vp.setViewPosition(curPos);
    }
  }

  // Creates and displays a popup menu containing the items located at the specified position
  private boolean updateItemPopup(Point mapLocation)
  {
    final int MAX_LEN = 32;

    // preparing menu items
    List<JMenuItem> menuItems = new ArrayList<JMenuItem>();
    Point itemLocation = new Point();
    // for each active layer...
    for (final Layers layer: Layers.values()) {
      if (isLayerSelected(layer)) {
        List<AbstractLayerItem> itemList = layerItems.get(layer);
        if (itemList != null) {
          // for each visible layer item...
          for (int i = 0; i < itemList.size(); i++) {
            if (isExtendedLayerItemActive(layer, i)) {
              final AbstractLayerItem item = itemList.get(i);
              if (item != null) {
                if (layer == Layers.AMBIENT && isLayerSelected(Layers.AMBIENTRANGE)) {
                  // skip duplicate (AMBIENT and AMBIENTRANGE) entries
                  if (item.getViewable() instanceof Ambient) {
                    Flag flag = (Flag)((Ambient)item.getViewable()).getAttribute("Flags");
                    if (flag != null && flag.isFlagSet(2)) {
                      break;
                    }
                  }
                }
                itemLocation.x = mapLocation.x - item.getX();
                itemLocation.y = mapLocation.y - item.getY();
                if (item.contains(itemLocation)) {
                  // creating a new menu item to be added to the context menu
                  StringBuilder sb = new StringBuilder();
                  sb.append(item.getName() == null || item.getName().isEmpty() ?
                            "Item" : item.getName());
                  sb.append(": ");
                  int lenPrefix = sb.length();
                  int lenMsg = item.getMessage().length();
                  sb.append((lenPrefix + lenMsg > MAX_LEN) ?
                            (item.getMessage().substring(0, MAX_LEN - lenPrefix) + "...") :
                            item.getMessage());
                  LayerMenuItem lmi = new LayerMenuItem(sb.toString(), item);
                  if (lenPrefix + lenMsg > MAX_LEN) {
                    lmi.setToolTipText(item.getMessage());
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
    pmItems.removeAll();
    if (!menuItems.isEmpty()) {
      for (final JMenuItem mi: menuItems) {
        pmItems.add(mi);
      }
      return true;
    }
    return false;
  }

  // Shows a popup menu containing layer items located at the current position when needed
  private void showItemPopup(MouseEvent event)
  {
    if (event != null && event.isPopupTrigger() && event.getSource() instanceof AbstractLayerItem) {
      AbstractLayerItem item = (AbstractLayerItem)event.getSource();
      Point location = item.getLocation();
      location.translate(event.getX(), event.getY());
      if (updateItemPopup(location)) {
        pmItems.show(item, event.getX(), event.getY());
      }
    }
  }


  // Helps to create a string representation of flags (index 0=no flags set)
  private static String createFlags(Flag flags, String[] flagsDesc)
  {
    if (flags != null) {
      int numFlags = 0;
      for (int i = 0; i < flags.getSize() * 8; i++) {
        if (flags.isFlagSet(i)) {
          numFlags++;
        }
      }
      if (numFlags > 0) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (int i = 0; i < flags.getSize() * 8; i++) {
          if (flags.isFlagSet(i)) {
            numFlags--;
            if (flagsDesc != null && i+1 < flagsDesc.length) {
              sb.append(flagsDesc[i+1]);
            } else {
              sb.append("Bit " + i);
            }
            if (numFlags > 0) {
              sb.append(", ");
            }
          }
        }
        sb.append("]");
        return sb.toString();
      } else if (flagsDesc != null && flagsDesc.length > 0) {
        return "[" + flagsDesc[0] + "]";
      }
    }
    return "[No flags]";
  }

  /**
   * Draws a list of map tiles into the specified image object.
   * @param image The image to draw the tiles into
   * @param decoder The TIS decoder needed to decode the tiles
   * @param tilesX Number of tiles per row
   * @param tilesY Number of tile rows
   * @param tileInfo A list of info objects needed to draw the right tiles
   * @return true if successful, false otherwise
   */
  private static boolean drawTiles(BufferedImage image, TisDecoder decoder,
                                  int tilesX, int tilesY, List<TileInfo> tileInfo)
  {
    if (image != null && decoder != null && tileInfo != null) {
      int tileWidth = decoder.info().tileWidth();
      int tileHeight = decoder.info().tileHeight();
      int width = tilesX * tileWidth;
      int height = tilesY * tileHeight;
      if (image.getWidth() >= width && image.getHeight() >= height) {
        final BufferedImage imgTile = ColorConvert.createCompatibleImage(tileWidth, tileHeight, false);
        final Graphics2D g = (Graphics2D)image.getGraphics();
        for (final TileInfo tile: tileInfo) {
          try {
            if (decoder.decodeTile(imgTile, tile.tilenum)) {
              g.drawImage(imgTile, tile.xpos*tileWidth, tile.ypos*tileHeight, null);
            }
          } catch (Exception e) {
            System.err.println("Error drawing tile #" + tile.tilenum);
          }
        }
        g.dispose();
        return true;
      }
    }
    return false;
  }

  /**
   * Draws a specific list of primary or secondary tiles, depending on the specified opened/closed state.
   * @param image The image to draw the tiles into
   * @param decoder The TIS decoder needed to decode the tiles
   * @param tilesX Number of tiles per row
   * @param tilesY Number of tile rows
   * @param tileInfo List of info objects needed to draw the right tiles
   * @param doorIndices List of info objects of specific door tiles
   * @param drawClosed Indicates whether the primary or secondary tile has to be drawn
   * @return true if successful, false otherwise
   */
  private static boolean drawDoorTiles(BufferedImage image, TisDecoder decoder,
                                      int tilesX, int tilesY, List<TileInfo> tileInfo,
                                      List<Integer> doorIndices, boolean drawClosed)
  {
    if (image != null && decoder != null && tileInfo != null && doorIndices != null) {
      int tileWidth = decoder.info().tileWidth();
      int tileHeight = decoder.info().tileHeight();
      int width = tilesX * tileWidth;
      int height = tilesY * tileHeight;
      if (image.getWidth() >= width && image.getHeight() >= height) {
        final BufferedImage imgTile = ColorConvert.createCompatibleImage(tileWidth, tileHeight, false);
        final Graphics2D g = (Graphics2D)image.getGraphics();
        for (final int index: doorIndices) {
          // searching for correct tileinfo object
          TileInfo tile = tileInfo.get(index);
          if (tile.tilenum != index) {
            for (TileInfo ti: tileInfo) {
              if (ti.tilenum == index) {
                tile = ti;
                break;
              }
            }
          }

          // decoding tile
          int tileIdx = (drawClosed && tile.tilenumAlt != -1) ? tile.tilenumAlt : tile.tilenum;
          try {
            if (decoder.decodeTile(imgTile, tileIdx)) {
              g.drawImage(imgTile, tile.xpos*tileWidth, tile.ypos*tileHeight, null);
            }
          } catch (Exception e) {
            System.err.println("Error drawing tile #" + tileIdx);
          }
        }
        g.dispose();
        return true;
      }
    }
    return false;
  }



//----------------------------- INNER CLASSES -----------------------------

  // Associates a menu item with a layer item object
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


  // Stores information about a single TIS tile
  public static final class TileInfo
  {
    private final int xpos, ypos;         // coordinate in tile grid
    private final int tilenum;            // primary tile index from WED
    private final int tilenumAlt;         // secondary tile index from WED
    private final int[] overlayIndices;   // index of additional overlays to address

    public TileInfo(int xpos, int ypos, int tilenum)
    {
      this.xpos = xpos;
      this.ypos = ypos;
      this.tilenum = tilenum;
      this.tilenumAlt = -1;
      this.overlayIndices = null;
    }

    public TileInfo(int xpos, int ypos, int tilenum, int tilenumAlt)
    {
      this.xpos = xpos;
      this.ypos = ypos;
      this.tilenum = tilenum;
      this.tilenumAlt = tilenumAlt;
      this.overlayIndices = null;
    }

    public TileInfo(int xpos, int ypos, int tilenum, int tilenumAlt, int overlayMask)
    {
      this.xpos = xpos;
      this.ypos = ypos;
      this.tilenum = tilenum;
      this.tilenumAlt = tilenumAlt;

      // calculating additional overlays
      int mcount = 0;
      int[] mindices = new int[8];
      for (int i = 0; i < 8; i++) {
        if ((overlayMask & (1 << i)) != 0) {
          mindices[mcount] = i;
          mcount++;
          break;
        }
      }
      if (mcount > 0) {
        this.overlayIndices = new int[mcount];
        System.arraycopy(mindices, 0, this.overlayIndices, 0, mcount);
      } else
        this.overlayIndices = null;
    }
  }
}
