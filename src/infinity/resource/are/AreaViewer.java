// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
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
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ProgressMonitor;
import javax.swing.SpringLayout;

import infinity.NearInfinity;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.HexNumber;
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
import infinity.datatype.Bitmap;
import infinity.resource.cre.CreResource;
import infinity.resource.graphics.ColorConvert;
import infinity.resource.graphics.TisDecoder;
import infinity.resource.graphics.TisResource2;
import infinity.resource.key.FileResourceEntry;
import infinity.resource.key.ResourceEntry;
import infinity.resource.toh.StrRefEntry;
import infinity.resource.toh.TohResource;
import infinity.resource.tot.StringEntry;
import infinity.resource.tot.TotResource;
import infinity.resource.vertex.Vertex;
import infinity.resource.wed.Overlay;
import infinity.resource.wed.Tilemap;
import infinity.resource.wed.WedResource;

/**
 * The Area Viewer shows a selected map with its associated items, such as actors, triggers or
 * animations.
 * @author argent77
 */
public final class AreaViewer extends ChildFrame
  implements Runnable, ActionListener, ItemListener, LayerItemListener, ComponentListener, MouseMotionListener
{
  // Identifies the respective layers
  private static enum Layers { ACTOR, TRIGGER, ENTRANCE, CONTAINER, AMBIENT, DOOR,
//                               AMBIENTRANGE,
                               ANIMATION, AUTOMAP, SPAWNPOINT, PROTRAP, TRANSITION,
                               DOORPOLY, WALLPOLY }

  // Identifies the respective WED resources
  private static enum DayNight { DAY, NIGHT }

  // Identifies locations of the area transitions
  private static enum AreaEdge { NORTH, EAST, SOUTH, WEST }

  // Tracks the current layer item state
  private static EnumMap<Layers, Boolean> LayerButtonState =
      new EnumMap<Layers, Boolean>(Layers.class);
  static {
    LayerButtonState.put(Layers.ACTOR, false);
    LayerButtonState.put(Layers.TRIGGER, false);
    LayerButtonState.put(Layers.ENTRANCE, false);
    LayerButtonState.put(Layers.CONTAINER, false);
    LayerButtonState.put(Layers.AMBIENT, false);
//    LayerButtonState.put(Layers.AMBIENTRANGE, false);
    LayerButtonState.put(Layers.DOOR, false);
    LayerButtonState.put(Layers.ANIMATION, false);
    LayerButtonState.put(Layers.AUTOMAP, false);
    LayerButtonState.put(Layers.SPAWNPOINT, false);
    LayerButtonState.put(Layers.PROTRAP, false);
    LayerButtonState.put(Layers.TRANSITION, false);
    LayerButtonState.put(Layers.DOORPOLY, false);
    LayerButtonState.put(Layers.WALLPOLY, false);
  }

  private final AreResource are;

  private EnumMap<DayNight, JRadioButton> dayNightButton =
      new EnumMap<DayNight, JRadioButton>(DayNight.class);
  private EnumMap<DayNight, WedResource> dayNightWed =
      new EnumMap<DayNight, WedResource>(DayNight.class);
  private EnumMap<DayNight, List<TisResource2.TileInfo>> dayNightTiles =
      new EnumMap<DayNight, List<TisResource2.TileInfo>>(DayNight.class);
  private EnumMap<DayNight, List<Integer>> dayNightDoorIndices =
      new EnumMap<DayNight, List<Integer>>(DayNight.class);
  private DayNight currentMap = null;
  private TisDecoder tisDecoder;

  private EnumMap<Layers, JCheckBox> layerButton =
      new EnumMap<Layers, JCheckBox>(Layers.class);
  private EnumMap<Layers, List<AbstractLayerItem>> layerItems =
      new EnumMap<Layers, List<AbstractLayerItem>>(Layers.class);

  private JPanel pRoot, pView, pSideBar;
  private JScrollPane spView;
  private JLabel lTileset;
  private ImageIcon mapImage;
  private JCheckBox cbDrawClosed;
  private JLabel lPosX, lPosY;
  private JTextArea taInfo;
  private Point mapCoordinate;
  private ProgressMonitor progressMonitor;
  private int pmMax, pmCur;


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


  public AreaViewer(AreResource areaFile)
  {
    this(NearInfinity.getInstance(), areaFile);
  }

  public AreaViewer(Component parent, AreResource areaFile)
  {
    super("Area Viewer: " + areaFile.getName(), true);
    this.are = areaFile;

    if ((NearInfinity.getInstance().getExtendedState() & Frame.MAXIMIZED_BOTH) != 0) {
      setExtendedState(Frame.MAXIMIZED_BOTH);
    } else {
      setExtendedState(Frame.NORMAL);
    }

    initProgressMonitor(parent, "Initializing " + are.getName(), 15, 0, 0);
    new Thread(this).start();
  }

//--------------------- Begin Interface Runnable ---------------------

  public void run()
  {
    initGui();
    releaseProgressMonitor();
  }

//--------------------- End Interface Runnable ---------------------

//--------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == dayNightButton.get(DayNight.DAY)) {
      setCurrentMap(DayNight.DAY);
      initLayerWedPoly();
      enableLayer(Layers.DOORPOLY, layerButton.get(Layers.DOORPOLY).isSelected());
      enableLayer(Layers.WALLPOLY, layerButton.get(Layers.WALLPOLY).isSelected());
    } else if (event.getSource() == dayNightButton.get(DayNight.NIGHT)) {
      setCurrentMap(DayNight.NIGHT);
      initLayerWedPoly();
      enableLayer(Layers.DOORPOLY, layerButton.get(Layers.DOORPOLY).isSelected());
      enableLayer(Layers.WALLPOLY, layerButton.get(Layers.WALLPOLY).isSelected());
    } else if (event.getSource() instanceof AbstractLayerItem) {
      AbstractLayerItem item = (AbstractLayerItem)event.getSource();
      item.showViewable();
    }
  }

//--------------------- End Interface ActionListener ---------------------

//--------------------- Begin Interface ItemListener ---------------------

  public void itemStateChanged(ItemEvent event)
  {
    if (event.getItemSelectable() == layerButton.get(Layers.ACTOR)) {
      enableLayer(Layers.ACTOR, layerButton.get(Layers.ACTOR).isSelected());
    } else if (event.getItemSelectable() == layerButton.get(Layers.TRIGGER)) {
      enableLayer(Layers.TRIGGER, layerButton.get(Layers.TRIGGER).isSelected());
    } else if (event.getItemSelectable() == layerButton.get(Layers.ENTRANCE)) {
      enableLayer(Layers.ENTRANCE, layerButton.get(Layers.ENTRANCE).isSelected());
    } else if (event.getItemSelectable() == layerButton.get(Layers.CONTAINER)) {
      enableLayer(Layers.CONTAINER, layerButton.get(Layers.CONTAINER).isSelected());
    } else if (event.getItemSelectable() == layerButton.get(Layers.AMBIENT)) {
      enableLayer(Layers.AMBIENT, layerButton.get(Layers.AMBIENT).isSelected());
//      layerButton.get(Layers.AMBIENTRANGE).setEnabled(layerButton.get(Layers.AMBIENT).isSelected() &&
//                                                                      !layerItems.get(Layers.AMBIENTRANGE).isEmpty());
//      enableLayer(Layers.AMBIENTRANGE, layerButton.get(Layers.AMBIENT).isSelected() &&
//                                                       layerButton.get(Layers.AMBIENTRANGE).isSelected());
//    } else if (event.getItemSelectable() == layerButton.get(Layers.AMBIENTRANGE)) {
//      enableLayer(Layers.AMBIENTRANGE, layerButton.get(Layers.AMBIENTRANGE).isSelected());
    } else if (event.getItemSelectable() == layerButton.get(Layers.DOOR)) {
      enableLayer(Layers.DOOR, layerButton.get(Layers.DOOR).isSelected());
    } else if (event.getItemSelectable() == layerButton.get(Layers.ANIMATION)) {
      enableLayer(Layers.ANIMATION, layerButton.get(Layers.ANIMATION).isSelected());
    } else if (event.getItemSelectable() == layerButton.get(Layers.AUTOMAP)) {
      enableLayer(Layers.AUTOMAP, layerButton.get(Layers.AUTOMAP).isSelected());
    } else if (event.getItemSelectable() == layerButton.get(Layers.TRANSITION)) {
      enableLayer(Layers.TRANSITION, layerButton.get(Layers.TRANSITION).isSelected());
    } else if (event.getItemSelectable() == layerButton.get(Layers.SPAWNPOINT)) {
      enableLayer(Layers.SPAWNPOINT, layerButton.get(Layers.SPAWNPOINT).isSelected());
    } else if (event.getItemSelectable() == layerButton.get(Layers.PROTRAP)) {
      enableLayer(Layers.PROTRAP, layerButton.get(Layers.PROTRAP).isSelected());
    } else if (event.getItemSelectable() == layerButton.get(Layers.DOORPOLY)) {
      enableLayer(Layers.DOORPOLY, layerButton.get(Layers.DOORPOLY).isSelected());
    } else if (event.getItemSelectable() == layerButton.get(Layers.WALLPOLY)) {
      enableLayer(Layers.WALLPOLY, layerButton.get(Layers.WALLPOLY).isSelected());
    } else if (event.getItemSelectable() == cbDrawClosed) {
      setDoorState(getCurrentMap(), drawDoorsClosed());
      enableLayer(Layers.DOOR, layerButton.get(Layers.DOOR).isSelected());
      enableLayer(Layers.DOORPOLY, layerButton.get(Layers.DOORPOLY).isSelected());
    }
  }

//--------------------- End Interface ItemListener ---------------------

//--------------------- Begin Interface ItemStateListener ---------------------

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

  public void componentHidden(ComponentEvent event)
  {
  }

  public void componentMoved(ComponentEvent event)
  {
  }

  public void componentResized(ComponentEvent event)
  {
    if (event.getSource() == lTileset) {
      // changing viewport size whenever the tileset size changes
      pView.setPreferredSize(lTileset.getSize());
    } else if (event.getSource() == spView) {
      // centering the tileset if it fits into the viewport
      Dimension pDim = lTileset.getPreferredSize();
      Dimension spDim = spView.getSize();
      if (pDim.width < spDim.width || pDim.height < spDim.height) {
        Point pLocation = lTileset.getLocation();
        Point pDistance = new Point();
        if (pDim.width < spDim.width)
          pDistance.x = pLocation.x - (spDim.width - pDim.width) / 2;
        if (pDim.height < spDim.height)
          pDistance.y = pLocation.y - (spDim.height - pDim.height) / 2;
        lTileset.setLocation(pLocation.x - pDistance.x, pLocation.y - pDistance.y);
      }
    }
  }

  public void componentShown(ComponentEvent event)
  {
  }

//--------------------- End Interface ComponentListener ---------------------

//--------------------- Begin Interface MouseMotionListener ---------------------

  public void mouseDragged(MouseEvent event)
  {
  }

  public void mouseMoved(MouseEvent event)
  {
    if (event.getSource() == lTileset) {
      setAreaLocation(event.getPoint());
    } else if (event.getSource() instanceof AbstractLayerItem) {
      // forwarding mouse event to continue displaying cursor position information when hovering over layer items
      AbstractLayerItem item = (AbstractLayerItem)event.getSource();
      MouseEvent newEvent =
          new MouseEvent(lTileset, event.getID(), event.getWhen(), event.getModifiers(),
                         event.getX() + item.getX(), event.getY() + item.getY(),
                         event.getXOnScreen(), event.getYOnScreen(),
                         event.getClickCount(), event.isPopupTrigger(), event.getButton());
      lTileset.dispatchEvent(newEvent);
    }
  }

//--------------------- End Interface MouseMotionListener ---------------------

  private void initGui()
  {
    // assembling main view
    pView = new JPanel(null);
    lTileset = new JLabel();
    lTileset.addComponentListener(this);
    lTileset.addMouseMotionListener(this);
    lTileset.setHorizontalAlignment(JLabel.CENTER);
    lTileset.setVerticalAlignment(JLabel.CENTER);
    mapImage = new ImageIcon();
    lTileset.setIcon(mapImage);
    pView.add(lTileset, BorderLayout.CENTER);
    spView = new JScrollPane(pView);
    spView.addComponentListener(this);
    spView.getVerticalScrollBar().setUnitIncrement(16);
    spView.getHorizontalScrollBar().setUnitIncrement(16);

    // initializing map data
    advanceProgressMonitor("Loading tileset");
    initMap();
    advanceProgressMonitor("Loading actors");
    initLayerActor();
    advanceProgressMonitor("Loading entrances");
    initLayerEntrance();
    advanceProgressMonitor("Loading ambient sounds");
    initLayerAmbient();
    advanceProgressMonitor("Loading animations");
    initLayerAnimation();
    advanceProgressMonitor("Loading automap notes");
    initLayerAutomap();
    advanceProgressMonitor("Loading proj. traps");
    initLayerProTrap();
    advanceProgressMonitor("Loading spawn points");
    initLayerSpawnPoint();
    advanceProgressMonitor("Loading triggers");
    initLayerTrigger();
    advanceProgressMonitor("Loading containers");
    initLayerContainer();
    advanceProgressMonitor("Loading doors");
    initLayerDoor();
    advanceProgressMonitor("Loading map transitions");
    initLayerTransition();
    advanceProgressMonitor("Loading wall/door polygons");
    initLayerWedPoly();
//    advanceProgressMonitor("Loading ambient sound ranges");
//    initLayerAmbientRange();
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

    // first time layer initialization
    layerButton.get(Layers.ACTOR).setSelected(LayerButtonState.get(Layers.ACTOR));
    layerButton.get(Layers.TRIGGER).setSelected(LayerButtonState.get(Layers.TRIGGER));
    layerButton.get(Layers.ENTRANCE).setSelected(LayerButtonState.get(Layers.ENTRANCE));
    layerButton.get(Layers.CONTAINER).setSelected(LayerButtonState.get(Layers.CONTAINER));
//    layerButton.get(Layers.AMBIENTRANGE).setSelected(LayerButtonState.get(Layers.AMBIENTRANGE));
//    layerButton.get(Layers.AMBIENTRANGE).setEnabled(LayerButtonState.get(Layers.AMBIENT) &&
//                                                    !layerItems.get(Layers.AMBIENTRANGE).isEmpty());
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
          case TRIGGER:       cb = new JCheckBox("Triggers"); break;
          case ENTRANCE:      cb = new JCheckBox("Entrances"); break;
          case CONTAINER:     cb = new JCheckBox("Containers"); break;
          case AMBIENT:       cb = new JCheckBox("Ambient Sounds"); break;
  //        case AMBIENTRANGE:  cb = new JCheckBox("Ambient Sound Radius"); break;
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
            lTileset.remove(item);
          }
        }
        oldList.clear();
      }

      if (list != null) {
        for (final AbstractLayerItem item: list) {
          if (item != null) {
            item.setVisible(false);
            lTileset.add(item);
            item.setItemLocation(item.getMapLocation());
          }
        }
        layerItems.put(layer, list);
      }
    }
  }

  // Returns whether the layer is available
  private boolean isLayerEnabled(Layers layer)
  {
    if (layer != null && layerButton.containsKey(layer)) {
      return layerButton.get(layer).isEnabled();
    }
    return false;
  }

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

  // Returns the currently displayed area location (of the mouse cursor)
  private Point getAreaLocation()
  {
    if (mapCoordinate == null) {
      mapCoordinate = new Point();
    }
    return mapCoordinate;
  }

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

  // Returns the information string in the information box
  private String getInfoText()
  {
    if (taInfo != null) {
      return taInfo.getText();
    } else {
      return new String();
    }
  }

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

    // initializing actor objects
    ArrayList<Actor> listActors = new ArrayList<Actor>();
    SectionOffset so = (SectionOffset)are.getAttribute("Actors offset");
    SectionCount sc = (SectionCount)are.getAttribute("# actors");
    if (so != null && sc != null) {
      int baseOfs = so.getValue();
      int count = sc.getValue();
      if (baseOfs > 0 && count > 0) {
        try {
          for (int i = 0; i < count; i++) {
            Actor actor = (Actor)are.getAttribute("Actor " + i);
            if (actor != null) {
              listActors.add(actor);
            }
          }
        } catch (Exception e) {
          System.err.println("Error parsing actors.");
          e.printStackTrace();
        }
      }
    }

    // initializing actor layer items
    ArrayList<AbstractLayerItem> list = new ArrayList<AbstractLayerItem>(listActors.size());
    final Icon[] iconGood = new Icon[]{Icons.getIcon("ActorGreen.png"), Icons.getIcon("ActorGreen_s.png")};
    final Icon[] iconNeutral = new Icon[]{Icons.getIcon("ActorBlue.png"), Icons.getIcon("ActorBlue_s.png")};
    final Icon[] iconEvil = new Icon[]{Icons.getIcon("ActorRed.png"), Icons.getIcon("ActorRed_s.png")};
    Point center = new Point(12, 40);
    for (final Actor actor: listActors) {
      String msg;
      Icon[] icon;
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
      item.setToolTipText(msg);
      item.setIcon(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
      item.addActionListener(this);
      item.addLayerItemListener(this);
      item.addMouseMotionListener(this);
      list.add(item);
    }
    addLayerItems(Layers.ACTOR, list);
    setLayerEnabled(Layers.ACTOR, !list.isEmpty(), list.size() + " actors available");
  }

  private void initLayerTrigger()
  {
    addLayer(Layers.TRIGGER);

    // initializing trigger objects
    ArrayList<ITEPoint> listTriggers = new ArrayList<ITEPoint>();
    SectionOffset so = (SectionOffset)are.getAttribute("Triggers offset");
    SectionCount sc = (SectionCount)are.getAttribute("# triggers");
    if (so != null && sc != null) {
      int baseOfs = so.getValue();
      int count = sc.getValue();
      if (baseOfs > 0 && count > 0) {
        try {
          for (int i = 0; i < count; i++) {
            ITEPoint ite = (ITEPoint)are.getAttribute("Trigger " + i);
            if (ite != null) {
              listTriggers.add(ite);
            }
          }
        } catch (Exception e) {
          System.err.println("Error parsing triggers.");
          return;
        }
      }
    }

    // initializing trigger layer items
    final String[] type = new String[]{" (Proximity trigger)", " (Info trigger)", " (Travel trigger)"};
    final Color[] color = new Color[]{new Color(0xFF400000, true), new Color(0xFF400000, true),
                                      new Color(0xC0800000, true), new Color(0xC0C00000, true)};
    ArrayList<AbstractLayerItem> list = new ArrayList<AbstractLayerItem>(listTriggers.size());
    for (final ITEPoint trigger: listTriggers) {
      String msg;
      Polygon poly = new Polygon();
      try {
        msg = ((TextString)trigger.getAttribute("Name")).toString();
        msg += type[((Bitmap)trigger.getAttribute("Type")).getValue()];
        int vnum = ((DecNumber)trigger.getAttribute("# vertices")).getValue();
        for (int i = 0; i < vnum; i++) {
          Vertex vertex = ((Vertex)trigger.getAttribute("Vertex " + i));
          if (vertex != null) {
            poly.addPoint(((DecNumber)vertex.getAttribute("X")).getValue(),
                          ((DecNumber)vertex.getAttribute("Y")).getValue());
          }
        }
      } catch (Throwable e) {
        msg = new String();
      }
      Rectangle rect = normalizePolygon(poly);
      ShapedLayerItem item = new ShapedLayerItem(new Point(rect.x, rect.y), trigger, msg, poly);
      item.setToolTipText(msg);
      item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, color[0]);
      item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[1]);
      item.setFillColor(AbstractLayerItem.ItemState.NORMAL, color[2]);
      item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[3]);
      item.setStroked(true);
      item.setFilled(true);
      item.addActionListener(this);
      item.addLayerItemListener(this);
      item.addMouseMotionListener(this);
      list.add(item);
    }
    addLayerItems(Layers.TRIGGER, list);
    setLayerEnabled(Layers.TRIGGER, !list.isEmpty(), list.size() + " triggers available");
  }

  private void initLayerEntrance()
  {
    addLayer(Layers.ENTRANCE);

    // initializing entrance objects
    ArrayList<Entrance> listEntrances = new ArrayList<Entrance>();
    SectionOffset so = (SectionOffset)are.getAttribute("Entrances offset");
    SectionCount sc = (SectionCount)are.getAttribute("# entrances");
    if (so != null && sc != null) {
      int baseOfs = so.getValue();
      int count = sc.getValue();
      if (baseOfs > 0 && count > 0) {
        try {
          for (int i = 0; i < count; i++) {
            Entrance entrance = (Entrance)are.getAttribute("Entrance " + i);
            if (entrance != null) {
              listEntrances.add(entrance);
            }
          }
        } catch (Exception e) {
          System.err.println("Error parsing entrances.");
          return;
        }
      }
    }

    // initializing entrance layer items
    ArrayList<AbstractLayerItem> list = new ArrayList<AbstractLayerItem>(listEntrances.size());
    final Icon[] icon = new Icon[]{Icons.getIcon("Entrance.png"), Icons.getIcon("Entrance_s.png")};
    Point center = new Point(11, 18);
    for (final Entrance entrance: listEntrances) {
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
      item.setToolTipText(msg);
      item.setIcon(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
      item.addActionListener(this);
      item.addLayerItemListener(this);
      item.addMouseMotionListener(this);
      list.add(item);
    }
    addLayerItems(Layers.ENTRANCE, list);
    setLayerEnabled(Layers.ENTRANCE, !list.isEmpty(), list.size() + " entrances available");
  }

  private void initLayerContainer()
  {
    addLayer(Layers.CONTAINER);

    // initializing container objects
    ArrayList<infinity.resource.are.Container> listContainers = new ArrayList<infinity.resource.are.Container>();
    SectionOffset so = (SectionOffset)are.getAttribute("Containers offset");
    SectionCount sc = (SectionCount)are.getAttribute("# containers");
    if (so != null && sc != null) {
      int baseOfs = so.getValue();
      int count = sc.getValue();
      if (baseOfs > 0 && count > 0) {
        try {
          for (int i = 0; i < count; i++) {
            infinity.resource.are.Container container =
                (infinity.resource.are.Container)are.getAttribute("Container " + i);
            if (container != null) {
              listContainers.add(container);
            }
          }
        } catch (Exception e) {
          System.err.println("Error parsing containers.");
          e.printStackTrace();
        }
      }
    }

    // initializing container layer items
    final String[] s_type = new String[]{" (Unknown)", " (Bag)", " (Chest)", " (Drawer)", " (Pile)",
                                         " (Table)", " (Shelf)", " (Altar)", " (Invisible)",
                                         " (Spellbook)", " (Body)", " (Barrel)", " (Crate)"};
    final Color[] color = new Color[]{new Color(0xFF004040, true), new Color(0xFF004040, true),
                                      new Color(0xC0008080, true), new Color(0xC000C0C0, true)};
    ArrayList<AbstractLayerItem> list = new ArrayList<AbstractLayerItem>(listContainers.size());
    for (final infinity.resource.are.Container container: listContainers) {
      String msg;
      Polygon poly = new Polygon();
      try {
        msg = ((TextString)container.getAttribute("Name")).toString();
        msg += s_type[((Bitmap)container.getAttribute("Type")).getValue()];
        int vnum = ((DecNumber)container.getAttribute("# vertices")).getValue();
        for (int i = 0; i < vnum; i++) {
          Vertex vertex = ((Vertex)container.getAttribute("Vertex " + i));
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
      item.setToolTipText(msg);
      item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, color[0]);
      item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[1]);
      item.setFillColor(AbstractLayerItem.ItemState.NORMAL, color[2]);
      item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[3]);
      item.setStroked(true);
      item.setFilled(true);
      item.addActionListener(this);
      item.addLayerItemListener(this);
      item.addMouseMotionListener(this);
      list.add(item);
    }
    addLayerItems(Layers.CONTAINER, list);
    setLayerEnabled(Layers.CONTAINER, !list.isEmpty(), list.size() + " containers available");
  }

  private void initLayerAmbient()
  {
    addLayer(Layers.AMBIENT);

    // initializing ambient sound objects
    ArrayList<Ambient> listAmbients = new ArrayList<Ambient>();
    SectionOffset so = (SectionOffset)are.getAttribute("Ambients offset");
    SectionCount sc = (SectionCount)are.getAttribute("# ambients");
    if (so != null && sc != null) {
      int baseOfs = so.getValue();
      int count = sc.getValue();
      if (baseOfs > 0 && count > 0) {
        try {
          for (int i = 0; i < count; i++) {
            Ambient ambient = (Ambient)are.getAttribute("Ambient " + i);
            if (ambient != null) {
              listAmbients.add(ambient);
            }
          }
        } catch (Exception e) {
          System.err.println("Error parsing ambient sounds.");
          e.printStackTrace();
        }
      }
    }

    // initializing ambient sound layer items
    ArrayList<AbstractLayerItem> list = new ArrayList<AbstractLayerItem>(listAmbients.size());
    final Icon[] icon = new Icon[]{Icons.getIcon("Ambient.png"), Icons.getIcon("Ambient_s.png"),
                                   Icons.getIcon("AmbientRanged.png"), Icons.getIcon("AmbientRanged_s.png")};
    Point center = new Point(16, 16);
    for (final Ambient ambient: listAmbients) {
      String msg;
      Point location = new Point(0, 0);
      int iconBase;
      try {
        location.x = ((DecNumber)ambient.getAttribute("Origin: X")).getValue();
        location.y = ((DecNumber)ambient.getAttribute("Origin: Y")).getValue();
        iconBase = ((Flag)ambient.getAttribute("Flags")).isFlagSet(2) ? 0 : 2;
        msg = ((TextString)ambient.getAttribute("Name")).toString();
        if (iconBase == 0) {
          msg += " (Global)";
        } else if (iconBase == 2) {
          msg += " (Ranged)";
        }
      } catch (Throwable e) {
        msg = new String();
        iconBase = 0;
      }
      IconLayerItem item = new IconLayerItem(location, ambient, msg, icon[iconBase + 0], center);
      item.setToolTipText(msg);
      item.setIcon(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[iconBase + 1]);
      item.addActionListener(this);
      item.addLayerItemListener(this);
      item.addMouseMotionListener(this);
      list.add(item);
    }
    addLayerItems(Layers.AMBIENT, list);
    setLayerEnabled(Layers.AMBIENT, !list.isEmpty(), list.size() + " ambient sounds available");
  }

//  private void initLayerAmbientRange()
//  {
//    addLayer(Layers.AMBIENTRANGE);
//
//
//    // initializing ambient sound objects
//    ArrayList<Ambient> listAmbients = new ArrayList<Ambient>();
//    SectionOffset so = (SectionOffset)are.getAttribute("Ambients offset");
//    SectionCount sc = (SectionCount)are.getAttribute("# ambients");
//    if (so != null && sc != null) {
//      int baseOfs = so.getValue();
//      int count = sc.getValue();
//      if (baseOfs > 0 && count > 0) {
//        try {
//          for (int i = 0; i < count; i++) {
//            Ambient ambient = (Ambient)are.getAttribute("Ambient " + i);
//            if (ambient != null) {
//              listAmbients.add(ambient);
//            }
//          }
//        } catch (Exception e) {
//          System.err.println("Error parsing ambient sound ranges.");
//          e.printStackTrace();
//        }
//      }
//    }
//
//    // initializing ambient sound layer items
//    ArrayList<AbstractLayerItem> list = new ArrayList<AbstractLayerItem>(listAmbients.size());
//    final Color[] color = new Color[]{new Color(0xFF000080, true), new Color(0xFF000080, true),
//                                      new Color(0x00204080, true), new Color(0x004060C0, true)};
//    for (final Ambient ambient: listAmbients) {
//      String msg;
//      Point location = new Point(0, 0);
//      Ellipse2D.Float circle = null;
//      int radius = 0;
//      int volume = 0;
//      try {
//        location.x = ((DecNumber)ambient.getAttribute("Origin: X")).getValue();
//        location.y = ((DecNumber)ambient.getAttribute("Origin: Y")).getValue();
//        radius = ((DecNumber)ambient.getAttribute("Radius")).getValue();
//        volume = ((DecNumber)ambient.getAttribute("Volume")).getValue();
//        msg = ((TextString)ambient.getAttribute("Name")).toString();
//        boolean global = ((Flag)ambient.getAttribute("Flags")).isFlagSet(2);
//        if (!global && radius > 0) {
//          circle = new Ellipse2D.Float(0, 0, (float)(2*radius), (float)(2*radius));
//          float alphaF = (float)volume / 100.0f * 255.0f;
//          int alphaNorm = ((int)Math.max(10.0f, alphaF*0.5f) & 0xff);
//          int alphaHigh = ((int)Math.min(255.0f, alphaF*0.5f) & 0xff);
//          color[2] = new Color(color[2].getRGB() | (alphaNorm << 24), true);
//          color[3] = new Color(color[3].getRGB() | (alphaHigh << 24), true);
//        }
//      } catch (Throwable e) {
//        msg = new String();
//      }
//      if (circle != null) {
//        ShapedLayerItem item = new ShapedLayerItem(location, ambient, msg, circle, new Point(radius, radius));
//        item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, color[0]);
//        item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[1]);
//        item.setFillColor(AbstractLayerItem.ItemState.NORMAL, color[2]);
//        item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[3]);
//        item.setStroked(true);
//        item.setFilled(true);
//        item.addActionListener(this);
//        item.addLayerItemListener(this);
//        item.addMouseMotionListener(this);
//        list.add(item);
//        item.setVisible(false);
//        lTileset.add(item);
//        item.setItemLocation(item.getMapLocation());
//      }
//    }
//    addLayerItems(Layers.AMBIENTRANGE, list);
//    setLayerEnabled(Layers.AMBIENTRANGE, !list.isEmpty(),
//                    list.size() + " ambient sounds with local radius available");
//  }

  private void initLayerDoor()
  {
    addLayer(Layers.DOOR);

    // initializing door objects
    ArrayList<Door> listDoors = new ArrayList<Door>();
    SectionOffset so = (SectionOffset)are.getAttribute("Doors offset");
    SectionCount sc = (SectionCount)are.getAttribute("# doors");
    if (so != null && sc != null) {
      int baseOfs = so.getValue();
      int count = sc.getValue();
      if (baseOfs > 0 && count > 0) {
        try {
          for (int i = 0; i < count; i++) {
            Door door = ((Door)are.getAttribute("Door " + i));
            if (door != null) {
              listDoors.add(door);
            }
          }
        } catch (Exception e) {
          System.err.println("Error parsing doors.");
          e.printStackTrace();
        }
      }
    }

    // initializing door layer items
    final Color[] color = new Color[]{new Color(0xFF400040, true), new Color(0xFF400040, true),
                                      new Color(0xC0800080, true), new Color(0xC0C000C0, true)};
    ArrayList<AbstractLayerItem> list = new ArrayList<AbstractLayerItem>(2*listDoors.size());
    for (final Door door: listDoors) {
      Polygon[] poly = new Polygon[]{new Polygon(), new Polygon()};
      String[] msg = {null, null};
      try {
        msg[0] = ((TextString)door.getAttribute("Name")).toString() + " (Open)";
        int vnum1 = ((DecNumber)door.getAttribute("# vertices (open)")).getValue();
        for (int i = 0; i < vnum1; i++) {
          Vertex vertex = (Vertex)door.getAttribute("Open vertex " + i);
          if (vertex != null) {
            poly[0].addPoint(((DecNumber)vertex.getAttribute("X")).getValue(),
                          ((DecNumber)vertex.getAttribute("Y")).getValue());
          }
        }
        msg[1] = ((TextString)door.getAttribute("Name")).toString() + " (Closed)";
        int vnum2 = ((DecNumber)door.getAttribute("# vertices (closed)")).getValue();
        for (int i = 0; i < vnum2; i++) {
          Vertex vertex = (Vertex)door.getAttribute("Closed vertex " + i);
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
        item.setToolTipText(msg[i]);
        item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, color[0]);
        item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[1]);
        item.setFillColor(AbstractLayerItem.ItemState.NORMAL, color[2]);
        item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[3]);
        item.setStroked(true);
        item.setFilled(true);
        item.addActionListener(this);
        item.addLayerItemListener(this);
        item.addMouseMotionListener(this);
        list.add(item);
      }
    }
    addLayerItems(Layers.DOOR, list);
    setLayerEnabled(Layers.DOOR, !listDoors.isEmpty(), listDoors.size() + " doors available");
  }

  private void initLayerAnimation()
  {
    addLayer(Layers.ANIMATION);

    // initializing background animation objects
    ArrayList<Animation> listAnimations = new ArrayList<Animation>();
    SectionOffset so = (SectionOffset)are.getAttribute("Animations offset");
    SectionCount sc = (SectionCount)are.getAttribute("# animations");
    if (so != null && sc != null) {
      int baseOfs = so.getValue();
      int count = sc.getValue();
      if (baseOfs > 0 && count > 0) {
        try {
          for (int i = 0; i < count; i++) {
            Animation anim = (Animation)are.getAttribute("Animation " + i);
            if (anim != null) {
              listAnimations.add(anim);
            }
          }
        } catch (Exception e) {
          System.err.println("Error parsing background animations.");
          e.printStackTrace();
        }
      }
    }

    // initializing background animation layer items
    ArrayList<AbstractLayerItem> list = new ArrayList<AbstractLayerItem>(listAnimations.size());
    final Icon[] icon = new Icon[]{Icons.getIcon("Animation.png"), Icons.getIcon("Animation_s.png")};
    Point center = new Point(16, 17);
    for (final Animation animation: listAnimations) {
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
      item.setToolTipText(msg);
      item.setIcon(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
      item.addActionListener(this);
      item.addLayerItemListener(this);
      item.addMouseMotionListener(this);
      list.add(item);
    }
    addLayerItems(Layers.ANIMATION, list);
    setLayerEnabled(Layers.ANIMATION, !list.isEmpty(), list.size() + " animations available");
  }

  private void initLayerAutomap()
  {
    addLayer(Layers.AUTOMAP);

    // initializing automap notes objects
    ArrayList<AutomapNotePST> listAutomapNotesPST = new ArrayList<AutomapNotePST>();
    ArrayList<AutomapNote> listAutomapNotes = new ArrayList<AutomapNote>();
    SectionOffset so = null;
    SectionCount sc = null;
    if (ResourceFactory.getGameID() != ResourceFactory.ID_BG1 &&
        ResourceFactory.getGameID() != ResourceFactory.ID_BG1TOTSC) {
      so = (SectionOffset)are.getAttribute("Automap notes offset");
      sc = (SectionCount)are.getAttribute("# automap notes");
    }
    if (so != null && sc != null) {
      int baseOfs = so.getValue();
      int count = sc.getValue();
      if (baseOfs > 0 && count > 0) {
        try {
          if (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT) {
            for (int i = 0; i < count; i++) {
              AutomapNotePST automap = (AutomapNotePST)are.getAttribute("Automap note " + i);
              if (automap != null) {
                listAutomapNotesPST.add(automap);
              }
            }
          } else {
            for (int i = 0; i < count; i++) {
              AutomapNote automap = (AutomapNote)are.getAttribute("Automap note " + i);
              if (automap != null) {
                listAutomapNotes.add(automap);
              }
            }
          }
        } catch (Exception e) {
          System.err.println("Error parsing automap notes.");
          e.printStackTrace();
        }
      }
    }

    // initializing automap notes layer items
    ArrayList<AbstractLayerItem> list;
    if (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT) {
      list = new ArrayList<AbstractLayerItem>(listAutomapNotesPST.size());
    } else {
      list = new ArrayList<AbstractLayerItem>(listAutomapNotes.size());
    }
    final Icon[] icon = new Icon[]{Icons.getIcon("Automap.png"), Icons.getIcon("Automap_s.png")};
    Point center = new Point(26, 26);
    if (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT) {
      for (final AutomapNotePST automap: listAutomapNotesPST) {
        String msg;
        Point location = new Point(0, 0);
        try {
          location.x = ((DecNumber)automap.getAttribute("Coordinate: X")).getValue();
          location.y = ((DecNumber)automap.getAttribute("Coordinate: Y")).getValue();
          msg = ((TextString)automap.getAttribute("Text")).toString();
        } catch (Throwable e) {
          msg = new String();
        }
        IconLayerItem item = new IconLayerItem(location, automap, msg, icon[0], center);
        item.setToolTipText(msg);
        item.setIcon(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
        item.addActionListener(this);
        item.addLayerItemListener(this);
        item.addMouseMotionListener(this);
        list.add(item);
      }
    } else {
      for (final AutomapNote automap: listAutomapNotes) {
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
                  sc = (SectionCount)toh.getAttribute("# strref entries");
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
        item.setToolTipText(msg);
        item.setIcon(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
        item.addActionListener(this);
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
    Dimension mapDim = getMapSize(getCurrentMap());
    mapDim.width *= getTisDecoder().info().tileWidth();
    mapDim.height *= getTisDecoder().info().tileHeight();
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
        item.setToolTipText(msg);
        item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, color[0]);
        item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[1]);
        item.setFillColor(AbstractLayerItem.ItemState.NORMAL, color[2]);
        item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[3]);
        item.setStroked(true);
        item.setFilled(true);
        item.addActionListener(this);
        item.addLayerItemListener(this);
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

    // initializing projectile trap objects
    ArrayList<ProTrap> listProTraps = new ArrayList<ProTrap>();
    SectionOffset so = null;
    SectionCount sc = null;
    if (ResourceFactory.getGameID() == ResourceFactory.ID_BG2 ||
        ResourceFactory.getGameID() == ResourceFactory.ID_BG2TOB ||
            ResourceFactory.getGameID() == ResourceFactory.ID_BGEE) {
      so = (SectionOffset)are.getAttribute("Projectile traps offset");
      sc = (SectionCount)are.getAttribute("# projectile traps");
    }
    if (so != null && sc != null) {
      int baseOfs = so.getValue();
      int count = sc.getValue();
      if (baseOfs > 0 && count > 0) {
        try {
          for (int i = 0; i < count; i++) {
            ProTrap trap = ((ProTrap)are.getAttribute("Projectile trap " + i));
            if (trap != null) {
              listProTraps.add(trap);
            }
          }
        } catch (Exception e) {
          System.err.println("Error parsing projectile traps.");
          e.printStackTrace();
        }
      }
    }

    // initializing projectile trap layer items
    ArrayList<AbstractLayerItem> list = new ArrayList<AbstractLayerItem>(listProTraps.size());
    final Icon[] icon = new Icon[]{Icons.getIcon("ProTrap.png"), Icons.getIcon("ProTrap_s.png")};
    Point center = new Point(14, 14);
    for (final ProTrap trap: listProTraps) {
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
      item.setToolTipText(msg);
      item.setIcon(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
      item.addActionListener(this);
      item.addLayerItemListener(this);
      item.addMouseMotionListener(this);
      list.add(item);
    }
    addLayerItems(Layers.PROTRAP, list);
    setLayerEnabled(Layers.PROTRAP, !list.isEmpty(), list.size() + " projectile traps available");
  }

  private void initLayerSpawnPoint()
  {
    addLayer(Layers.SPAWNPOINT);

    // initializing spawn point objects
    ArrayList<SpawnPoint> listSpawnPoints = new ArrayList<SpawnPoint>();
    SectionOffset so = (SectionOffset)are.getAttribute("Spawn points offset");
    SectionCount sc = (SectionCount)are.getAttribute("# spawn points");
    if (so != null && sc != null) {
      int baseOfs = so.getValue();
      int count = sc.getValue();
      if (baseOfs > 0 && count > 0) {
        try {
          for (int i = 0; i < count; i++) {
            SpawnPoint sp = (SpawnPoint)are.getAttribute("Spawn point " + i);
            if (sp != null) {
              listSpawnPoints.add(sp);
            }
          }
        } catch (Exception e) {
          System.err.println("Error parsing spawn points.");
          e.printStackTrace();
        }
      }
    }

    // initializing spawn point layer items
    ArrayList<AbstractLayerItem> list = new ArrayList<AbstractLayerItem>(listSpawnPoints.size());
    final Icon[] icon = new Icon[]{Icons.getIcon("SpawnPoint.png"), Icons.getIcon("SpawnPoint_s.png")};
    Point center = new Point(22, 22);
    for (final SpawnPoint spawn: listSpawnPoints) {
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
      item.setToolTipText(msg);
      item.setIcon(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
      item.addActionListener(this);
      item.addLayerItemListener(this);
      item.addMouseMotionListener(this);
      list.add(item);
    }
    addLayerItems(Layers.SPAWNPOINT, list);
    setLayerEnabled(Layers.SPAWNPOINT, !list.isEmpty(), list.size() + " spawn points available");
  }

  private void initLayerWedPoly()
  {
    addLayer(Layers.DOORPOLY);
    addLayer(Layers.WALLPOLY);

    // common initializations
    String wedName = ((ResourceRef)are.getAttribute("WED resource")).getResourceName();
    ResourceEntry entry = ResourceFactory.getInstance().getResourceEntry(wedName);
    WedResource wed = null;
    try {
      wed = new WedResource(entry);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
    HexNumber ofsDoors = (HexNumber)wed.getAttribute("Doors offset");
    HexNumber ofsPolygons = (HexNumber)wed.getAttribute("Wall polygons offset");
    int numDoors = ((DecNumber)wed.getAttribute("# doors")).getValue();
    int numWallPoly = ((DecNumber)wed.getAttribute("# wall polygons")).getValue();

    // initializing wall polygon objects
    ArrayList<infinity.resource.wed.Polygon> listWallPoly = new ArrayList<infinity.resource.wed.Polygon>();
    if (ofsPolygons != null && ofsPolygons.getValue() > 0) {
      try {
        for (int i = 0; i < numWallPoly; i++) {
          infinity.resource.wed.Polygon poly =
              (infinity.resource.wed.Polygon)wed.getAttribute("Wall polygon " + i);
          if (poly != null) {
            listWallPoly.add(poly);
          }
        }
      } catch (Exception e) {
        System.err.println("Error parsing wall polygons.");
        e.printStackTrace();
      }
    }

    // initializing door polygon objects
    ArrayList<infinity.resource.wed.Door> listDoorPoly = new ArrayList<infinity.resource.wed.Door>();
    if (ofsDoors != null && ofsDoors.getValue() > 0) {
      try {
        for (int i = 0; i < numDoors; i++) {
          infinity.resource.wed.Door door = (infinity.resource.wed.Door)wed.getAttribute("Door " + i);
          if (door != null) {
            listDoorPoly.add(door);
          }
        }
      } catch (Exception e) {
        System.err.println("Error parsing door polygons.");
        e.printStackTrace();
      }
    }


    // initializing door polygon layer items
    Color[] color = new Color[]{new Color(0xFF603080, true), new Color(0xFF603080, true),
                                new Color(0x80A050C0, true), new Color(0xC0C060D0, true)};
    ArrayList<AbstractLayerItem> listDoor = new ArrayList<AbstractLayerItem>(2*listDoorPoly.size());
    for (final infinity.resource.wed.Door door: listDoorPoly) {
      try {
        int numOpen = ((SectionCount)door.getAttribute("# polygons open")).getValue();
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
            dp[0] = (infinity.resource.wed.Polygon)door.getAttribute("Open polygon " + i);
            if (dp[0] != null) {
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
            dp[1] = (infinity.resource.wed.Polygon)door.getAttribute("Closed polygon " + i);
            if (dp[1] != null) {
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
              item.setToolTipText(msg2[j]);
              item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, color[0]);
              item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[1]);
              item.setFillColor(AbstractLayerItem.ItemState.NORMAL, color[2]);
              item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[3]);
              item.setStroked(true);
              item.setFilled(true);
              item.addActionListener(this);
              item.addLayerItemListener(this);
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

    // initializing wall polygon layer items
    color = new Color[]{new Color(0xFF005046, true), new Color(0xFF005046, true),
                        new Color(0x8020A060, true), new Color(0xA030B070, true)};
    ArrayList<AbstractLayerItem> listWall = new ArrayList<AbstractLayerItem>(listWallPoly.size());
    int count = 0;
    for (final infinity.resource.wed.Polygon wp: listWallPoly) {
      String msg;
      Polygon poly = new Polygon();
      try {
        msg = "Wall polygon #" + count + " " + createFlags((Flag)wp.getAttribute("Polygon flags"),
                                                           infinity.resource.wed.Polygon.s_flags);
        int numVertices = ((SectionCount)wp.getAttribute("# vertices")).getValue();
        for (int i = 0; i < numVertices; i++) {
          Vertex vertex = ((Vertex)wp.getAttribute("Vertex " + i));
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
      item.setToolTipText(msg);
      item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, color[0]);
      item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[1]);
      item.setFillColor(AbstractLayerItem.ItemState.NORMAL, color[2]);
      item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[3]);
      item.setStroked(true);
      item.setFilled(true);
      item.addActionListener(this);
      item.addLayerItemListener(this);
      item.addMouseMotionListener(this);
      listWall.add(item);
    }
    addLayerItems(Layers.WALLPOLY, listWall);
    setLayerEnabled(Layers.WALLPOLY, !listWall.isEmpty(), listWall.size() + " wall polygons available");
  }

  private void enableLayer(Layers layer, boolean enable)
  {
    if (layer == Layers.DOOR || layer == Layers.DOORPOLY) {
      List<AbstractLayerItem> list = layerItems.get(layer);
      int ofs = drawDoorsClosed() ? 1 : 0;
      for (int i = 0; i < list.size() / 2; i++) {
        showLayerItem(list.get((i << 1) + ((ofs + 1) & 1)), false);
        showLayerItem(list.get((i << 1) + ofs), enable);
      }
    } else {
      showAllLayerItems(layerItems.get(layer), enable);
    }
    LayerButtonState.put(layer, enable);
  }

  private DayNight getCurrentMap()
  {
    return currentMap;
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
        BufferedImage img = (BufferedImage)mapImage.getImage();
        if (img == null || img.getWidth() != mapDim.width || img.getHeight() != mapDim.height) {
          // creating new image object
          img = ColorConvert.createCompatibleImage(mapDim.width, mapDim.height, false);
          mapImage.setImage(img);
        } else if (img != null) {
          img.flush();
        }

        // drawing map tiles
        TisResource2.drawTiles(img, getTisDecoder(), mapTilesDim.width, mapTilesDim.height,
                               dayNightTiles.get(dn));

        // drawing opened/closed door tiles
        TisResource2.drawDoorTiles(img, getTisDecoder(), mapTilesDim.width, mapTilesDim.height,
                                   dayNightTiles.get(dn), dayNightDoorIndices.get(dn),
                                   drawDoorsClosed());

        lTileset.setSize(mapDim);
        currentMap = dn;
        lTileset.repaint();
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
        BufferedImage img = (BufferedImage)mapImage.getImage();

        if (img != null && img.getWidth() >= mapDim.width && img.getHeight() >= mapDim.height) {
          // drawing opened/closed door tiles
          TisResource2.drawDoorTiles(img, getTisDecoder(), mapTilesDim.width, mapTilesDim.height,
                                     dayNightTiles.get(dn), dayNightDoorIndices.get(dn),
                                     drawDoorsClosed());
          lTileset.repaint();
          return true;
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
        TisDecoder decoder = new TisDecoder(getTisResource(dn));
        tisDecoder = decoder;
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

        ArrayList<TisResource2.TileInfo> listTiles = new ArrayList<TisResource2.TileInfo>(width*height);
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
          TisResource2.TileInfo info = new TisResource2.TileInfo(idx % width, idx / width, tileIdx, tileIdxAlt, mask);
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

  // sets visibility state of the specified layer items list
  private void showAllLayerItems(List<AbstractLayerItem> list, boolean show)
  {
    if (list != null) {
      for (final AbstractLayerItem item: list) {
        item.setVisible(show);
      }
    }
  }

  // sets visibility state of the specific layer items
  private void showLayerItem(AbstractLayerItem item, boolean show)
  {
    if (item != null) {
      item.setVisible(show);
    }
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
}
