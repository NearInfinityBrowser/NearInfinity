// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import infinity.NearInfinity;
import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.RemovableDecNumber;
import infinity.datatype.ResourceRef;
import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.gui.Center;
import infinity.gui.ChildFrame;
import infinity.gui.RenderCanvas;
import infinity.gui.WindowBlocker;
import infinity.gui.layeritem.AbstractLayerItem;
import infinity.gui.layeritem.IconLayerItem;
import infinity.gui.layeritem.LayerItemEvent;
import infinity.gui.layeritem.LayerItemListener;
import infinity.icon.Icons;
import infinity.resource.ResourceFactory;
import infinity.resource.are.Ambient;
import infinity.resource.are.AreResource;
import infinity.resource.graphics.ColorConvert;
import infinity.resource.graphics.TisDecoder;
import infinity.resource.key.ResourceEntry;
import infinity.resource.wed.Overlay;
import infinity.resource.wed.Tilemap;
import infinity.resource.wed.WedResource;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
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
  // identifies the respective WED resources
  private static enum DayNight { DAY, NIGHT }

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
  private final EnumMap<ItemLayer.Type, JCheckBox> layerButtons =
      new EnumMap<ItemLayer.Type, JCheckBox>(ItemLayer.Type.class);

  private final Component parent;
  private final AreResource are;
  private final AreaStructures structure;   // provides access to preprocessed map structures
  private final LayerManager layers;        // provides access to the map item layers
  private final AbstractLayerItem areItem;

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
    this.parent = parent;
    this.are = areaFile;
    this.structure = new AreaStructures(this);
    this.layers = new LayerManager(this);
    this.areItem = new IconLayerItem(new Point(), this.are, areaFile.getName());
    areItem.setVisible(false);

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

  /**
   * Returns the canvas for the map graphics
   * @return
   */
  public RenderCanvas getCanvas()
  {
    return mapCanvas;
  }

  /**
   * Returns the data structures object of the current map.
   * @return The data structures object of the current map.
   */
  public AreaStructures getAreaStructures()
  {
    return structure;
  }

  /**
   * Returns the layers manager object of the viewer.
   * @return The layers manager object of the viewer
   */
  public LayerManager getAreaLayers()
  {
    return layers;
  }

  // Returns whether closed door are currently shown
  public boolean isDoorStateClosed()
  {
    if (cbDrawClosed != null) {
      return cbDrawClosed.isSelected();
    } else
      return false;
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
      for (ItemLayer.Type type: new ItemLayer.Type[]{ItemLayer.Type.DOORPOLY, ItemLayer.Type.WALLPOLY}) {
        layers.initData(type, layerButtons.get(type), mapCanvas);
        layers.get(type).setEnabled(layerButtons.get(type).isSelected());
      }
//      layers.initData(ItemLayer.Type.DOORPOLY, mapCanvas);
//      layers.initData(ItemLayer.Type.WALLPOLY, mapCanvas);
//      ItemLayer layer = layers.get(ItemLayer.Type.DOORPOLY);
//      layer.setEnabled(layer.getCheckBox().isSelected());
//      layer = layers.get(ItemLayer.Type.WALLPOLY);
//      layer.setEnabled(layer.getCheckBox().isSelected());
    } else if (event.getSource() == dayNightButton.get(DayNight.NIGHT)) {
      setCurrentMap(DayNight.NIGHT);
      structure.initWed();
      for (ItemLayer.Type type: new ItemLayer.Type[]{ItemLayer.Type.DOORPOLY, ItemLayer.Type.WALLPOLY}) {
        layers.initData(type, layerButtons.get(type), mapCanvas);
        layers.get(type).setEnabled(layerButtons.get(type).isSelected());
      }
//      layers.initData(ItemLayer.Type.DOORPOLY, mapCanvas);
//      layers.initData(ItemLayer.Type.WALLPOLY, mapCanvas);
//      ItemLayer layer = layers.get(ItemLayer.Type.DOORPOLY);
//      layer.setEnabled(layer.getCheckBox().isSelected());
//      layer = layers.get(ItemLayer.Type.WALLPOLY);
//      layer.setEnabled(layer.getCheckBox().isSelected());
    } else if (event.getSource() instanceof AbstractLayerItem) {
      AbstractLayerItem item = (AbstractLayerItem)event.getSource();
      item.showViewable();
    } else if (event.getSource() instanceof JMenuItem) {
      if (event.getSource() instanceof LayerMenuItem) {
        LayerMenuItem lmi = (LayerMenuItem)event.getSource();
        AbstractLayerItem item = lmi.getLayerItem();
        if (item != null) {
          if (item == areItem) {
            // Global structure: showing Edit tab of are structure
            are.selectEditTab();
            getParentWindow().toFront();
          } else {
            // Sub structure: creating and showing new Viewable
            item.showViewable();
          }
        }
      }
    }
  }

//--------------------- End Interface ActionListener ---------------------

//--------------------- Begin Interface ItemListener ---------------------

  @Override
  public void itemStateChanged(ItemEvent event)
  {
    if (event.getItemSelectable() == cbDrawClosed) {
      setDoorState(getCurrentMap(), isDoorStateClosed());
      for (ItemLayer.Type type: new ItemLayer.Type[]{ItemLayer.Type.DOOR, ItemLayer.Type.DOORPOLY}) {
        layers.get(type).setEnabled(layerButtons.get(type).isSelected());
      }
    } else if (event.getItemSelectable() instanceof JCheckBox) {
      for (ItemLayer.Type type: ItemLayer.Type.values()) {
        if (event.getItemSelectable() == layerButtons.get(type)) {
          ItemLayer layer = layers.get(type);
          JCheckBox cb = layerButtons.get(type);
          layer.setEnabled(cb.isSelected());
          if (type == ItemLayer.Type.AMBIENT) {
            ItemLayer layerAR = layers.get(ItemLayer.Type.AMBIENTRANGE);
            JCheckBox cbAR = layerButtons.get(ItemLayer.Type.AMBIENTRANGE);
            cbAR.setEnabled(cb.isSelected() && !layerAR.isEmpty());
            layerAR.setEnabled(cb.isSelected() && cbAR.isSelected());
            break;
          }
        }
      }
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
  protected void windowClosing() throws Exception
  {
    mapCanvas.setImage(null);
    mapCanvas.removeAll();
    layers.clearData();
    tisDecoder.close();
    structure.clear();
    dayNightWed.clear();
    dispose();
    System.gc();
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
    ItemLayer.Type[] typeCreation = new ItemLayer.Type[]{
        ItemLayer.Type.ACTOR, ItemLayer.Type.ENTRANCE, ItemLayer.Type.AMBIENT,
        ItemLayer.Type.ANIMATION, ItemLayer.Type.PROTRAP, ItemLayer.Type.SPAWNPOINT,
        ItemLayer.Type.AUTOMAP, ItemLayer.Type.CONTAINER, ItemLayer.Type.DOOR, ItemLayer.Type.REGION,
        ItemLayer.Type.DOORPOLY, ItemLayer.Type.WALLPOLY, ItemLayer.Type.AMBIENTRANGE};
    for (final ItemLayer.Type type: typeCreation) {
      JCheckBox cb = layers.get(type).createCheckBox(this);
      layerButtons.put(type, cb);
      layers.initData(type, cb, mapCanvas);
    }
    advanceProgressMonitor("Creating GUI");
    mapCanvas.add(areItem);

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
    JPanel pLayers = createGroupBox("Layers: ", new GridLayout(layers.size(), 1));
    for (final ItemLayer.Type type: ItemLayer.Type.values()) {
      pLayers.add(layerButtons.get(type));
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
    ItemLayer.Type[] typeSelection = new ItemLayer.Type[]{
        ItemLayer.Type.ACTOR, ItemLayer.Type.REGION, ItemLayer.Type.ENTRANCE,
        ItemLayer.Type.CONTAINER, ItemLayer.Type.AMBIENTRANGE, ItemLayer.Type.AMBIENT,
        ItemLayer.Type.DOOR, ItemLayer.Type.ANIMATION, ItemLayer.Type.AUTOMAP, ItemLayer.Type.PROTRAP,
        ItemLayer.Type.SPAWNPOINT, ItemLayer.Type.DOORPOLY, ItemLayer.Type.WALLPOLY};
    for (final ItemLayer.Type type: typeSelection) {
      layerButtons.get(type).setSelected(layers.get(type).isSelected());
      layers.get(type).setEnabled(layers.get(type).isSelected());
      if (type == ItemLayer.Type.AMBIENTRANGE) {
        layerButtons.get(type).setEnabled(layers.get(ItemLayer.Type.AMBIENT).isSelected() &&
                                                     !layers.get(type).isEmpty());
      }
    }

    setVisible(true);
  }

  private void createDayNight(DayNight dn, String label)
  {
    JRadioButton rb = new JRadioButton(label);
    rb.addActionListener(this);
    rb.setEnabled(false);
    dayNightButton.put(dn, rb);
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
                      isDoorStateClosed());

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
                          isDoorStateClosed());

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
    List<LayerMenuItem> menuItems = new ArrayList<LayerMenuItem>();
    Point itemLocation = new Point();

    // adding global menu item
    LayerMenuItem lmi = new LayerMenuItem("Global: " + areItem.getMessage(), areItem);
    lmi.setIcon(Icons.getIcon("Edit16.gif"));
    lmi.addActionListener(this);
    menuItems.add(lmi);

    // for each active layer...
    for (final ItemLayer.Type type: ItemLayer.Type.values()) {
      ItemLayer layer = layers.get(type);
      if (layerButtons.get(type).isSelected()) {
        List<AbstractLayerItem> itemList = layer.getItemList();
        if (itemList != null) {
          // for each visible layer item...
          for (int i = 0; i < itemList.size(); i++) {
            if (layer.isExtendedItemActive(i)) {
              final AbstractLayerItem item = itemList.get(i);
              if (item != null) {
                if (type == ItemLayer.Type.AMBIENT &&
                    layerButtons.get(ItemLayer.Type.AMBIENTRANGE).isSelected()) {
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
                  lmi = new LayerMenuItem(sb.toString(), item);
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
      for (final LayerMenuItem mi: menuItems) {
        pmItems.add(mi);
        if (mi.getLayerItem() == areItem && menuItems.size() > 1) {
          pmItems.addSeparator();
        }
      }
      return true;
    }
    return false;
  }

  // Shows a popup menu containing layer items located at the current position when needed
  private void showItemPopup(MouseEvent event)
  {
    if (event != null && event.isPopupTrigger()) {
      Component parent = null;
      Point location = null;
      if (event.getSource() instanceof AbstractLayerItem) {
        parent = (AbstractLayerItem)event.getSource();
        location = parent.getLocation();
        location.translate(event.getX(), event.getY());
      } else if (event.getSource() == mapCanvas) {
        parent = mapCanvas;
        location = event.getPoint();
      }
      if (parent != null && location != null) {
        if (updateItemPopup(location)) {
          pmItems.show(parent, event.getX(), event.getY());
        }
      }
    }
  }

  // attempts to find the next available instance of a Window in the list of this component's parent
  private Window getParentWindow()
  {
    Component c = this.parent;
    while (c != null) {
      if (c instanceof Window)
        return (Window)c;
      c = c.getParent();
    }
    return NearInfinity.getInstance();
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
