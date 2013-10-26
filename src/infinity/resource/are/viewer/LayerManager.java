// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.IdsBitmap;
import infinity.datatype.ResourceRef;
import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.datatype.StringRef;
import infinity.datatype.TextEdit;
import infinity.datatype.TextString;
import infinity.gui.layeritem.AbstractLayerItem;
import infinity.gui.layeritem.IconLayerItem;
import infinity.gui.layeritem.ShapedLayerItem;
import infinity.icon.Icons;
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
import infinity.resource.are.viewer.AreaStructures.Structure;
import infinity.resource.are.viewer.ItemLayer.Type;
import infinity.resource.cre.CreResource;
import infinity.resource.key.FileResourceEntry;
import infinity.resource.to.StrRefEntry;
import infinity.resource.to.StringEntry;
import infinity.resource.to.TohResource;
import infinity.resource.to.TotResource;
import infinity.resource.vertex.Vertex;
import infinity.util.FileCI;

import java.awt.Color;
import java.awt.Container;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import javax.swing.JCheckBox;

/**
 * Manages layers of map structures as a whole.
 * @author argent77
 */
class LayerManager
{
  private static final EnumMap<Type, Boolean> LayerSelectedState = new EnumMap<Type, Boolean>(Type.class);
  private static final EnumMap<Type, String> DefaultLayerName = new EnumMap<Type, String>(Type.class);
  private static final EnumMap<Type, String> DefaultLayerText = new EnumMap<Type, String>(Type.class);
  static {
    for (final ItemLayer.Type type: ItemLayer.Type.values()) {
      LayerSelectedState.put(type, false);
    }

    DefaultLayerName.put(Type.ACTOR,        "Actor");
    DefaultLayerName.put(Type.REGION,       "Region");
    DefaultLayerName.put(Type.ENTRANCE,     "Entrance");
    DefaultLayerName.put(Type.CONTAINER,    "Container");
    DefaultLayerName.put(Type.AMBIENT,      "Ambient");
    DefaultLayerName.put(Type.AMBIENTRANGE, "Ambient");
    DefaultLayerName.put(Type.DOOR,         "Door");
    DefaultLayerName.put(Type.ANIMATION,    "Animation");
    DefaultLayerName.put(Type.AUTOMAP,      "Automap");
    DefaultLayerName.put(Type.SPAWNPOINT,   "Spawn Point");
    DefaultLayerName.put(Type.PROTRAP,      "Trap");
    DefaultLayerName.put(Type.DOORPOLY,     "Door Poly");
    DefaultLayerName.put(Type.WALLPOLY,     "Wall Poly");

    DefaultLayerText.put(Type.ACTOR,        "Actors");
    DefaultLayerText.put(Type.REGION,       "Regions");
    DefaultLayerText.put(Type.ENTRANCE,     "Entrances");
    DefaultLayerText.put(Type.CONTAINER,    "Containers");
    DefaultLayerText.put(Type.AMBIENT,      "Ambient Sounds");
    DefaultLayerText.put(Type.AMBIENTRANGE, "Ambient Sound Ranges");
    DefaultLayerText.put(Type.DOOR,         "Doors");
    DefaultLayerText.put(Type.ANIMATION,    "Background Animations");
    DefaultLayerText.put(Type.AUTOMAP,      "Automap Notes");
    DefaultLayerText.put(Type.SPAWNPOINT,   "Spawn Points");
    DefaultLayerText.put(Type.PROTRAP,      "Projectile Traps");
    DefaultLayerText.put(Type.DOORPOLY,     "Door Polygons");
    DefaultLayerText.put(Type.WALLPOLY,     "Wall Polygons");
  }

  private final EnumMap<Type, ItemLayer> layers =
      new EnumMap<Type, ItemLayer>(Type.class);

  private final AreaViewer viewer;

  /**
   * Returns the global selected state of the specified layer.
   * @param type The layer type.
   * @return <code>true</code> if the layer is selected, <code>false</code> otherwise.
   */
  static boolean getSelectedState(Type type)
  {
    if (type != null && LayerSelectedState.containsKey(type)) {
      return LayerSelectedState.get(type);
    } else {
      return false;
    }
  }

  /**
   * Sets a new globally selected state of the specified layer.
   * @param type The layer type.
   * @param state The new selected state.
   */
  static void setSelectedState(Type type, boolean state)
  {
    if (type != null && LayerSelectedState.containsKey(type)) {
      LayerSelectedState.put(type, state);
    }
  }


  LayerManager(AreaViewer viewer)
  {
    this.viewer = viewer;
    for (final Type type: Type.values()) {
      layers.put(type, new ItemLayer(viewer, type, DefaultLayerName.get(type),
                                     DefaultLayerText.get(type), LayerSelectedState.get(type)));
    }
  }

  /**
   * Returns the number of layers available.
   * @return Number of available layers.
   */
  int size()
  {
    return layers.size();
  }

  /**
   * Returns the layer object of the specified type.
   * @param type The requested layer type.
   * @return The layer object of the specified type, or <code>null</code> if not available.
   */
  ItemLayer get(Type type)
  {
    if (type != null) {
      return layers.get(type);
    }
    return null;
  }

  /**
   * Returns the first ItemLayer object that contains the specified name.
   * @param name The name associated with the ItemLayer object in question.
   * @return The matching ItemLayer object, or <code>null</code> if not match found.
   */
  ItemLayer findByName(String name)
  {
    if (name != null) {
      for (final ItemLayer layer: layers.values()) {
        if (name.equals(layer.getName()))
          return layer;
      }
    }
    return null;
  }

  /**
   * A convenience method to set basic properties of the specified layer altogether.
   * @param type The type of the layer to set.
   * @param name The name to set.
   * @param longName The long name to set.
   * @return <code>true</code> if the properties have been set, <code>false</code> otherwise.
   */
  boolean setProperties(Type type, String name, String longName)
  {
    if (type != null) {
      ItemLayer layer = layers.get(type);
      layer.setName(name);
      layer.setLongName(longName);
      return true;
    }
    return false;
  }

  /**
   * Initializes the specified layer and fills it with data. Old data will be removed automatically.
   * @param type The type of the layer to initialize.
   * @parem id A short and unique keyword that describes the layer type (e.g. "Ambient" or "Actor").
   * @parem text The text that is associated with the layer's checkbox.
   * @parem listener A listener that handles the layer's checkbox state changes.
   * @return <code>true</code> on success, <code>false</code> otherwise.
   */
  boolean initData(Type type, JCheckBox checkBox, Container target)
  {
    switch (type) {
      case ACTOR:         return initActor(checkBox, target);
      case REGION:        return initRegion(checkBox, target);
      case ENTRANCE:      return initEntrance(checkBox, target);
      case CONTAINER:     return initContainer(checkBox, target);
      case AMBIENT:       return initAmbient(checkBox, target);
      case AMBIENTRANGE:  return initAmbientRange(checkBox, target);
      case DOOR:          return initDoor(checkBox, target);
      case ANIMATION:     return initAnimation(checkBox, target);
      case AUTOMAP:       return initAutomap(checkBox, target);
      case SPAWNPOINT:    return initSpawnPoint(checkBox, target);
      case PROTRAP:       return initProTrap(checkBox, target);
      case DOORPOLY:      return initDoorPoly(checkBox, target);
      case WALLPOLY:      return initWallPoly(checkBox, target);
      default:            return false;
    }
  }

  /**
   * Removes data from all available ItemLayer objects.
   */
  void clearData()
  {
    for (final ItemLayer layer: layers.values()) {
      layer.clear();
    }
  }


  private boolean initActor(JCheckBox checkBox, Container target)
  {
    boolean res = false;

    // initializing actor layer items
    ItemLayer layer = get(Type.ACTOR);
    AreaStructures structure = viewer.getAreaStructures();
    List<StructEntry> listEntries = structure.getStructureList(Structure.ARE, Structure.ACTOR);
    if (listEntries != null) {
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
        item.setName(layer.getName());
        item.setToolTipText(msg);
        item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
        item.addActionListener(viewer);
        item.addLayerItemListener(viewer);
        item.addMouseListener(viewer);
        item.addMouseMotionListener(viewer);
        list.add(item);
      }

      layer.setExtended(false);
      layer.removeAllFromContainer(target);
      layer.clear();
      layer.add(list);
      layer.addAllToContainer(target);
      res = true;
    }
    checkBox.setEnabled(res && !layer.isEmpty());
    checkBox.setToolTipText(layer.itemCount() + " actors available");
    return res;
  }

  private boolean initRegion(JCheckBox checkBox, Container target)
  {
    boolean res = false;

    // initializing region layer items
    ItemLayer layer = get(Type.REGION);
    AreaStructures structure = viewer.getAreaStructures();
    List<StructEntry> listEntries = structure.getStructureList(Structure.ARE, Structure.REGION);
    if (listEntries != null) {
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
            Vertex vertex = (Vertex)structure.getEntryByIndex(Structure.ARE, Structure.VERTEX,
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
        item.setName(layer.getName());
        item.setToolTipText(msg);
        item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, color[0]);
        item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[1]);
        item.setFillColor(AbstractLayerItem.ItemState.NORMAL, color[2]);
        item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[3]);
        item.setStroked(true);
        item.setFilled(true);
        item.addActionListener(viewer);
        item.addLayerItemListener(viewer);
        item.addMouseListener(viewer);
        item.addMouseMotionListener(viewer);
        list.add(item);
      }

      layer.setExtended(false);
      layer.removeAllFromContainer(target);
      layer.clear();
      layer.add(list);
      layer.addAllToContainer(target);
      res = true;
    }
    checkBox.setEnabled(res && !layer.isEmpty());
    checkBox.setToolTipText(layer.itemCount() + " regions available");
    return res;
  }

  private boolean initEntrance(JCheckBox checkBox, Container target)
  {
    boolean res = false;

    // initializing entrance layer items
    ItemLayer layer = get(Type.ENTRANCE);
    AreaStructures structure = viewer.getAreaStructures();
    List<StructEntry> listEntries = structure.getStructureList(Structure.ARE, Structure.ENTRANCE);
    if (listEntries != null) {
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
        item.setName(layer.getName());
        item.setToolTipText(msg);
        item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
        item.addActionListener(viewer);
        item.addLayerItemListener(viewer);
        item.addMouseListener(viewer);
        item.addMouseMotionListener(viewer);
        list.add(item);
      }

      layer.setExtended(false);
      layer.removeAllFromContainer(target);
      layer.clear();
      layer.add(list);
      layer.addAllToContainer(target);
      res = true;
    }
    checkBox.setEnabled(res && !layer.isEmpty());
    checkBox.setToolTipText(layer.itemCount() + " entrances available");
    return res;
  }

  private boolean initContainer(JCheckBox checkBox, Container target)
  {
    boolean res = false;

    // initializing container layer items
    ItemLayer layer = get(Type.CONTAINER);
    AreaStructures structure = viewer.getAreaStructures();
    List<StructEntry> listEntries = structure.getStructureList(Structure.ARE, Structure.CONTAINER);
    if (listEntries != null) {
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
            Vertex vertex = (Vertex)structure.getEntryByIndex(Structure.ARE, Structure.VERTEX,
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
        item.setName(layer.getName());
        item.setToolTipText(msg);
        item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, color[0]);
        item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[1]);
        item.setFillColor(AbstractLayerItem.ItemState.NORMAL, color[2]);
        item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[3]);
        item.setStroked(true);
        item.setFilled(true);
        item.addActionListener(viewer);
        item.addLayerItemListener(viewer);
        item.addMouseListener(viewer);
        item.addMouseMotionListener(viewer);
        list.add(item);
      }

      layer.setExtended(false);
      layer.removeAllFromContainer(target);
      layer.clear();
      layer.add(list);
      layer.addAllToContainer(target);
      res = true;
    }
    checkBox.setEnabled(res && !layer.isEmpty());
    checkBox.setToolTipText(layer.itemCount() + " containers available");
    return res;
  }

  private boolean initAmbient(JCheckBox checkBox, Container target)
  {
    boolean res = false;

    // initializing ambient sound layer items
    ItemLayer layer = get(Type.AMBIENT);
    AreaStructures structure = viewer.getAreaStructures();
    List<StructEntry> listEntries = structure.getStructureList(Structure.ARE, Structure.AMBIENT);
    if (listEntries != null) {
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
        item.setName(layer.getName());
        item.setToolTipText(msg);
        item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[iconBase + 1]);
        item.addActionListener(viewer);
        item.addLayerItemListener(viewer);
        item.addMouseListener(viewer);
        item.addMouseMotionListener(viewer);
        list.add(item);
      }

      layer.setExtended(false);
      layer.removeAllFromContainer(target);
      layer.clear();
      layer.add(list);
      layer.addAllToContainer(target);
      res = true;
    }
    checkBox.setEnabled(res && !layer.isEmpty());
    checkBox.setToolTipText(layer.itemCount() + " ambient sounds available");
    return res;
  }

  private boolean initAmbientRange(JCheckBox checkBox, Container target)
  {
    boolean res = false;

    // initializing ambient sound layer items
    ItemLayer layer = get(Type.AMBIENTRANGE);
    AreaStructures structure = viewer.getAreaStructures();
    List<StructEntry> listEntries = structure.getStructureList(Structure.ARE, Structure.AMBIENT);
    if (listEntries != null) {
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
          item.setName(layer.getName());
          item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, color[0]);
          item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[1]);
          item.setFillColor(AbstractLayerItem.ItemState.NORMAL, color[2]);
          item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[3]);
          item.setStrokeWidth(AbstractLayerItem.ItemState.NORMAL, 2);
          item.setStrokeWidth(AbstractLayerItem.ItemState.HIGHLIGHTED, 2);
          item.setStroked(true);
          item.setFilled(true);
          item.addActionListener(viewer);
          item.addLayerItemListener(viewer);
          item.addMouseListener(viewer);
          item.addMouseMotionListener(viewer);
          list.add(item);
          item.setVisible(false);
          item.setItemLocation(item.getMapLocation());
        }
      }

      layer.setExtended(false);
      layer.removeAllFromContainer(target);
      layer.clear();
      layer.add(list);
      layer.addAllToContainer(target);
      res = true;
    }
    checkBox.setEnabled(res && !layer.isEmpty());
    checkBox.setToolTipText(layer.itemCount() + " ambient sounds with local radius available");
    return res;
  }

  private boolean initDoor(JCheckBox checkBox, Container target)
  {
    boolean res = false;

    // initializing door layer items
    ItemLayer layer = get(Type.DOOR);
    AreaStructures structure = viewer.getAreaStructures();
    List<StructEntry> listEntries = structure.getStructureList(Structure.ARE, Structure.DOOR);
    if (listEntries != null) {
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
            Vertex vertex = (Vertex)structure.getEntryByIndex(Structure.ARE, Structure.VERTEX,
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
            Vertex vertex = (Vertex)structure.getEntryByIndex(Structure.ARE, Structure.VERTEX,
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
          item.setName(layer.getName());
          item.setToolTipText(msg[i]);
          item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, color[0]);
          item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[1]);
          item.setFillColor(AbstractLayerItem.ItemState.NORMAL, color[2]);
          item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[3]);
          item.setStroked(true);
          item.setFilled(true);
          item.addActionListener(viewer);
          item.addLayerItemListener(viewer);
          item.addMouseListener(viewer);
          item.addMouseMotionListener(viewer);
          list.add(item);
        }
      }

      layer.setExtended(true);
      layer.removeAllFromContainer(target);
      layer.clear();
      layer.add(list);
      layer.addAllToContainer(target);
      res = true;
    }
    checkBox.setEnabled(res && !layer.isEmpty());
    checkBox.setToolTipText(listEntries.size() + " doors available");
    return res;
  }

  private boolean initAnimation(JCheckBox checkBox, Container target)
  {
    boolean res = false;

    // initializing background animation layer items
    ItemLayer layer = get(Type.ANIMATION);
    AreaStructures structure = viewer.getAreaStructures();
    List<StructEntry> listEntries = structure.getStructureList(Structure.ARE, Structure.ANIMATION);
    if (listEntries != null) {
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
        item.setName(layer.getName());
        item.setToolTipText(msg);
        item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
        item.addActionListener(viewer);
        item.addLayerItemListener(viewer);
        item.addMouseListener(viewer);
        item.addMouseMotionListener(viewer);
        list.add(item);
      }

      layer.setExtended(false);
      layer.removeAllFromContainer(target);
      layer.clear();
      layer.add(list);
      layer.addAllToContainer(target);
      res = true;
    }
    checkBox.setEnabled(res && !layer.isEmpty());
    checkBox.setToolTipText(layer.itemCount() + " animations available");
    return res;
  }

  private boolean initAutomap(JCheckBox checkBox, Container target)
  {
    boolean res = false;

    // initializing automap notes layer items
    ItemLayer layer = get(Type.AUTOMAP);
    AreaStructures structure = viewer.getAreaStructures();
    List<StructEntry> listEntries = structure.getStructureList(Structure.ARE, Structure.AUTOMAP);
    if (listEntries != null) {
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
          item.setName(layer.getName());
          item.setToolTipText(msg);
          item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
          item.addActionListener(viewer);
          item.addLayerItemListener(viewer);
          item.addMouseListener(viewer);
          item.addMouseMotionListener(viewer);
          list.add(item);
        }
      } else {
        AreResource are = viewer.getAre();
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
                  File tohFile = new FileCI(filePath + "DEFAULT.TOH");
                  File totFile = new FileCI(filePath + "DEFAULT.TOT");
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
          item.setName(layer.getName());
          item.setToolTipText(msg);
          item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
          item.addActionListener(viewer);
          item.addMouseListener(viewer);
          item.addMouseMotionListener(viewer);
          item.addLayerItemListener(viewer);
          list.add(item);
        }
      }

      layer.setExtended(false);
      layer.removeAllFromContainer(target);
      layer.clear();
      layer.add(list);
      layer.addAllToContainer(target);
      res = true;
    }
    checkBox.setEnabled(res && !layer.isEmpty());
    checkBox.setToolTipText(layer.itemCount() + " automap notes available");
    return res;
  }

  private boolean initSpawnPoint(JCheckBox checkBox, Container target)
  {
    boolean res = false;

    // initializing spawn point layer items
    ItemLayer layer = get(Type.SPAWNPOINT);
    AreaStructures structure = viewer.getAreaStructures();
    List<StructEntry> listEntries = structure.getStructureList(Structure.ARE, Structure.SPAWNPOINT);
    if (listEntries != null) {
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
        item.setName(layer.getName());
        item.setToolTipText(msg);
        item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
        item.addActionListener(viewer);
        item.addLayerItemListener(viewer);
        item.addMouseListener(viewer);
        item.addMouseMotionListener(viewer);
        list.add(item);
      }

      layer.setExtended(false);
      layer.removeAllFromContainer(target);
      layer.clear();
      layer.add(list);
      layer.addAllToContainer(target);
      res = true;
    }
    checkBox.setEnabled(res && !layer.isEmpty());
    checkBox.setToolTipText(layer.itemCount() + " spawn points available");
    return res;
  }

  private boolean initProTrap(JCheckBox checkBox, Container target)
  {
    boolean res = false;

    // initializing projectile trap layer items
    ItemLayer layer = get(Type.PROTRAP);
    AreaStructures structure = viewer.getAreaStructures();
    List<StructEntry> listEntries = structure.getStructureList(Structure.ARE, Structure.PROTRAP);
    if (listEntries != null) {
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
          int ea = ((DecNumber)trap.getAttribute("Target")).getValue() & 0xff;
          if (ea >= 2 && ea <= 30) {
            msg += " (hostile)";
          } else if (ea >= 200) {
            msg += " (friendly)";
          }
        } catch (Throwable e) {
          msg = new String();
        }
        IconLayerItem item = new IconLayerItem(location, trap, msg, icon[0], center);
        item.setName(layer.getName());
        item.setToolTipText(msg);
        item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
        item.addActionListener(viewer);
        item.addLayerItemListener(viewer);
        item.addMouseListener(viewer);
        item.addMouseMotionListener(viewer);
        list.add(item);
      }

      layer.setExtended(false);
      layer.removeAllFromContainer(target);
      layer.clear();
      layer.add(list);
      layer.addAllToContainer(target);
      res = true;
    }
    checkBox.setEnabled(res && !layer.isEmpty());
    checkBox.setToolTipText(layer.itemCount() + " projectile traps available");
    return res;
  }

  private boolean initDoorPoly(JCheckBox checkBox, Container target)
  {
    boolean res = false;

    // initializing door polygon layer items
    ItemLayer layer = get(Type.DOORPOLY);
    AreaStructures structure = viewer.getAreaStructures();
    List<StructEntry> listEntries = structure.getStructureList(Structure.WED, Structure.DOOR);
    if (listEntries != null) {
      Color[] color = new Color[]{new Color(0xFF603080, true), new Color(0xFF603080, true),
                                  new Color(0x80A050C0, true), new Color(0xC0C060D0, true)};
      ArrayList<AbstractLayerItem> list = new ArrayList<AbstractLayerItem>(2*listEntries.size());
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
              dp[0] = (infinity.resource.wed.Polygon)structure.getEntryByOffset(Structure.WED,
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
              dp[1] = (infinity.resource.wed.Polygon)structure.getEntryByOffset(Structure.WED,
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
                item.setName(layer.getName());
                item.setToolTipText(msg2[j]);
                item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, color[0]);
                item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[1]);
                item.setFillColor(AbstractLayerItem.ItemState.NORMAL, color[2]);
                item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[3]);
                item.setStroked(true);
                item.setFilled(true);
                item.addActionListener(viewer);
                item.addLayerItemListener(viewer);
                item.addMouseListener(viewer);
                item.addMouseMotionListener(viewer);
                list.add(item);
              } else {
                list.add(null);
              }
            }
          }
        } catch (Exception e) {
        }
      }

      layer.setExtended(true);
      layer.removeAllFromContainer(target);
      layer.clear();
      layer.add(list);
      layer.addAllToContainer(target);
      res = true;
    }
    checkBox.setEnabled(res && !layer.isEmpty());
    checkBox.setToolTipText(listEntries.size() + " door polygons available");
    return res;
  }

  private boolean initWallPoly(JCheckBox checkBox, Container target)
  {
    boolean res = false;

    // initializing wall polygon layer items
    ItemLayer layer = get(Type.WALLPOLY);
    AreaStructures structure = viewer.getAreaStructures();
    List<StructEntry> listEntries = structure.getStructureList(Structure.WED, Structure.WALLPOLY);
    if (listEntries != null) {
      Color[] color = new Color[]{new Color(0xFF005046, true), new Color(0xFF005046, true),
                                  new Color(0x8020A060, true), new Color(0xA030B070, true)};
      ArrayList<AbstractLayerItem> list = new ArrayList<AbstractLayerItem>(listEntries.size());
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
            Vertex vertex = (Vertex)structure.getEntryByIndex(Structure.WED, Structure.VERTEX,
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
        item.setName(layer.getName());
        item.setToolTipText(msg);
        item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, color[0]);
        item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[1]);
        item.setFillColor(AbstractLayerItem.ItemState.NORMAL, color[2]);
        item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[3]);
        item.setStroked(true);
        item.setFilled(true);
        item.addActionListener(viewer);
        item.addLayerItemListener(viewer);
        item.addMouseListener(viewer);
        item.addMouseMotionListener(viewer);
        list.add(item);
      }

      layer.setExtended(false);
      layer.removeAllFromContainer(target);
      layer.clear();
      layer.add(list);
      layer.addAllToContainer(target);
      res = true;
    }
    checkBox.setEnabled(res && !layer.isEmpty());
    checkBox.setToolTipText(listEntries.size() + " wall polygons available");
    return res;
  }


  // Translates polygon to top-left corner and returns original bounding box
  private static Rectangle normalizePolygon(Polygon poly)
  {
    if (poly != null) {
      Rectangle r = poly.getBounds();
      poly.translate(-r.x, -r.y);
      return r;
    }
    return new Rectangle();
  }

  // Helps to create a string representation of flags (flagsDesc[0] = no flags set)
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


}
