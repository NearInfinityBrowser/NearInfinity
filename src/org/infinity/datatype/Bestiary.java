// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import static java.awt.event.ActionEvent.ACTION_PERFORMED;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.infinity.gui.InfinityScrollPane;
import org.infinity.gui.InfinityTextArea;
import org.infinity.gui.RenderCanvas;
import static org.infinity.gui.StructViewer.UPDATE_VALUE;
import org.infinity.gui.hexview.GenericHexViewer;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.gam.GamResource;
import org.infinity.resource.graphics.GraphicsResource;
import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.IniMap;
import org.infinity.util.IniMapCache;
import org.infinity.util.IniMapEntry;
import org.infinity.util.IniMapSection;
import org.infinity.util.StringTable;

import tv.porst.jhexview.DataChangedEvent;
import tv.porst.jhexview.HexViewEvent;
import tv.porst.jhexview.IColormap;
import tv.porst.jhexview.IDataChangedListener;
import tv.porst.jhexview.IHexViewListener;
import tv.porst.jhexview.JHexView;
import tv.porst.jhexview.SimpleDataProvider;

/**
 * Datatype for {@link GamResource#GAM_BESTIARY Bestiary} field of the
 * {@link GamResource GAM} resource.
 *
 * Creature becomes known when any of a party sees it, however is unclear how the
 * {@link CreResource creature} is connected with record in a bestiary.
 *
 * <p>
 * The creature has the {@link CreResource#CRE_CHARACTER_TYPE Character type} field
 * value of which suspiciously remind types of creatures in a bestiary, but, first,
 * not all creatures have this field filled, but it get to a bestiary at least
 * (Lady of Pain, Morte, Nameless One, Lothar, Coaxmetal, Pillar of Skulls,
 * The Transcendent One), secondly, the made experiment showed that changeover of
 * value of this field in a CRE file and in a SAV file does not influence record
 * of a bestiary.
 *
 * @author Mingun
 */
public final class Bestiary extends Datatype implements Editable, TableModel
{
  /**
   * This string reference used as category title when bestiary not contains any
   * NPC creatures (at start of game).
   */
  private static final int EMPTY_BESTIARY_NAME = 3594;
  /**
   * This string reference used as category description when bestiary not contains any
   * NPC creatures (at start of game).
   */
  private static final int EMPTY_BESTIARY_DESC = 3595;
  /** Color, used to highligth background of unused bytes. */
  private static final Color UNUSED_COLOR = new Color(0xffffa0);
  /** Color, used to highligth background of party members. */
  private static final Color PARTY_COLOR = Color.CYAN;

  //<editor-fold defaultstate="collapsed" desc="Creatures">
  /**
   * List of used entries in a bestiary field. Other bytes in the {@link #known}
   * field are unused.
   * <p>
   * String and image references are hardcoded because it seems that they are
   * hardcoded in the game.
   */
  private final List<Creature> creatures;
  //</editor-fold>
  /**
   * If byte contains non zero value, then player knowns creature from {@link #creatures}
   * array. Only first 88 bytes is used ({@code creatures.length}).
   */
  private final byte[] known;

  /** This class contains information about creature: name, description and image. */
  private static final class Creature
  {
    /** String reference to talk table with brief name of the creature. */
    private final int nameStrRef;
    /** String reference to talk table with description of the creature. */
    private final int descStrRef;
    /** Category of creature that determines, at which section in the Bestiary creature will appears. */
    private final Category category;
    /** Resource reference with name like {@code JR...KN.BMP} with image of the creature. */
    private final String imageResRef;
    /** Variable that used to count killed creatures of this type. */
    private final String killVarName;

    enum Category
    {
      /** Creature is playable character that appears in the "Playable Characters" in the game menu.*/
      PARTY("0"),
      /** Creature is Non-playable Character that appears in the "Non-playable Characters" in the game menu.*/
      NPC("1");

      /** Representation of categoty in {@code ini} file. */
      final String type;

      Category(String type) { this.type = type; }

      static Category resolve(String type)
      {
        for (Category cat : values()) {
          if (cat.type.equals(type)) return cat;
        }
        return null;
      }
    }

    public Creature(Integer nameStrRef, Integer descStrRef, Category category,
                    String imageResRef, String killVarName)
    {
      this.nameStrRef  = nameStrRef == null ? -1 : nameStrRef.intValue();
      this.descStrRef  = descStrRef == null ? -1 : descStrRef.intValue();
      this.category    = category;
      this.imageResRef = imageResRef;
      this.killVarName = killVarName;
    }
    public Creature(IniMapSection section)
    {
      this(section == null ? null : section.getEntry("name"),
           section == null ? null : section.getEntry("desc0"),
           section == null ? null : section.getEntry("class"),
           section == null ? null : section.getEntry("imageKnown"),
           section == null ? null : section.getEntry("killvar")
      );
    }
    private Creature(IniMapEntry nameStrRef,
                     IniMapEntry descStrRef,
                     IniMapEntry category,
                     IniMapEntry imageResRef,
                     IniMapEntry killVarName
    )
    {
      this(nameStrRef  == null ? null : nameStrRef.getIntValue(),
           descStrRef  == null ? null : descStrRef.getIntValue(),
           category    == null ? null : Category.resolve(category.getValue()),
           imageResRef == null ? null : imageResRef.getValue(),
           killVarName == null ? null : killVarName.getValue()
      );
    }
    /** Translates string reference of brief name to string with name. */
    public String getName() { return StringTable.getStringRef(nameStrRef); }
    /** Translates string reference of detail description to string with description. */
    public String getDesc() { return StringTable.getStringRef(descStrRef); }
    public boolean isParty() { return category == Category.PARTY; }
    public Image getImage()
    {
      final ResourceEntry entry = ResourceFactory.getResourceEntry(imageResRef + ".bmp");
      if (entry == null) { return null; }

      final Resource resource = ResourceFactory.getResource(entry);
      if (resource instanceof GraphicsResource) {
        return ((GraphicsResource) resource).getImage();
      }
      return null;
    }

    @Override
    public String toString()
    {
      return "Creature{"
              + "nameStrRef=" + nameStrRef
              + ", descStrRef=" + descStrRef
              + ", category=" + category
              + ", imageResRef=" + imageResRef
              + ", killVarName=" + killVarName
              + '}';
    }
  }
  /**
   * Editor for Bestiary field. Contains table with creatures, HEX area for edit
   * not used bytes and information panel with image and description of the selected
   * creature in the table.
   */
  private final class Viewer extends JSplitPane implements ListSelectionListener,
                                                           TableModelListener,
                                                           IDataChangedListener,
                                                           IHexViewListener,
                                                           IColormap
  {
    /** Canvas used to draw creature image in editor. */
    private final RenderCanvas image = new RenderCanvas();
    /** Text area that contains description of the creature. */
    private final InfinityTextArea desc = new InfinityTextArea(true);
    /** Table for editing used creature entries. */
    private final JTable table;
    /** Object which requested the editor and is interested in events about its updating. */
    private final ActionListener container;

    public Viewer(ActionListener container)
    {
      super();
      this.container = container;
      table = new JTable(Bestiary.this);
      final JHexView hex = new JHexView();

      final JTabbedPane pane = new JTabbedPane();
      pane.addTab("Table", new JScrollPane(table));
      pane.addTab("Raw", hex);

      final JSplitPane editor = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
      editor.add(pane, JSplitPane.TOP);
      editor.add(new InfinityScrollPane(desc, true), JSplitPane.BOTTOM);
      editor.setResizeWeight(0.8);// 80% - to table, 20% - to description

      add(editor, JSplitPane.LEFT);
      add(new JScrollPane(image), JSplitPane.RIGHT);
      // PST images for bestiary has 281 x 441 resolution. Reserve 19 pixels for scrollbar
      image.setPreferredSize(new Dimension(300, 441));
      setResizeWeight(1);// 100% - to table, image always has the same size so do not need resize
      resetToPreferredSizes();

      desc.setEditable(false);
      desc.setLineWrap(true);
      desc.setWrapStyleWord(true);

      final TableColumnModel model = table.getColumnModel();
      TableColumn col = model.getColumn(0);// checkbox
      col.setMinWidth(20);
      col.setMaxWidth(20);
      col.setResizable(false);

      col = model.getColumn(1);// number
      col.setMinWidth(25);
      col.setMaxWidth(25);
      col.setResizable(false);

      col = model.getColumn(2);// name
      col.setMinWidth(0);
      col.setPreferredWidth(50);

      col = model.getColumn(4);// kill variable
      col.setMinWidth(0);
      col.setPreferredWidth(50);

      table.getSelectionModel().addListSelectionListener(this);
      table.getModel().addTableModelListener(this);

      final DefaultTableCellRenderer renderer = new DefaultTableCellRenderer()
      {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column)
        {
          final boolean isPC = creatures.get(row).isParty();
          setBackground(isPC ? PARTY_COLOR : table.getBackground());

          super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
          setForeground(isKnown(row) ? null : Color.GRAY);
          return this;
        }
      };
      table.setDefaultRenderer(Object.class, renderer);
      table.setDefaultRenderer(Integer.class, renderer);

      final SimpleDataProvider provider = new SimpleDataProvider(known);
      provider.addListener(this);
      GenericHexViewer.configureHexView(hex, true);
      hex.setDefinitionStatus(JHexView.DefinitionStatus.DEFINED);
      hex.setData(provider);
      hex.setColormap(this);
      hex.addHexListener(this);
      // Because StructViewer not stretch his components, set infinity preferred size
      setPreferredSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    //<editor-fold defaultstate="collapsed" desc="ListSelectionListener">
    @Override
    public void valueChanged(ListSelectionEvent e)
    {
      final ListSelectionModel model = (ListSelectionModel)e.getSource();
      final int index = model.getAnchorSelectionIndex();
      final boolean hasImage = image.getImage() != null;
      if (index < 0 || index >= creatures.size()) {
        image.setImage(null);
        desc.setText(null);
      } else {
        final Creature cre = creatures.get(index);
        image.setImage(cre.getImage());
        final int pos = desc.getCaretPosition();
        final String text = cre.getDesc();
        desc.setText(text);
        desc.setCaretPosition(text.length() < pos ? 0 : pos);
      }
      if (hasImage ^ (image.getImage() != null)) {
        revalidate();
      }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="TableModelListener">
    @Override
    public void tableChanged(TableModelEvent e)
    {
      container.actionPerformed(new ActionEvent(e.getSource(), ACTION_PERFORMED, UPDATE_VALUE));
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="IDataChangedListener">
    @Override
    public void dataChanged(DataChangedEvent e)
    {
      container.actionPerformed(new ActionEvent(e.getSource(), ACTION_PERFORMED, UPDATE_VALUE));
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="IHexViewListener">
    @Override
    public void stateChanged(HexViewEvent e)
    {
      final int offset = (int)(e.getSelectionStart() / 2);
      final int length = (int)(e.getSelectionLength()/ 2);
      table.getSelectionModel().setSelectionInterval(offset, offset + length);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="IColormap">
    @Override
    public boolean colorize(byte value, long currentOffset) { return true; }

    @Override
    public Color getBackgroundColor(byte value, long currentOffset)
    {
      if (currentOffset >= creatures.size()) {
        return UNUSED_COLOR;
      }
      if (creatures.get((int)currentOffset).isParty()) {
        return PARTY_COLOR;
      }
      return null;
    }

    @Override
    public Color getForegroundColor(byte value, long currentOffset)
    {
      return value == 0 ? Color.GRAY : null;
    }
    //</editor-fold>
  }

  public Bestiary(ByteBuffer buffer, int offset, String name)
  {
    super(null, offset, 260, name);
    known = new byte[getSize()];
    read(buffer, offset);
    creatures = readCreatures();
  }

  //<editor-fold defaultstate="collapsed" desc="Editable">
  @Override
  public JComponent edit(ActionListener container) { return new Viewer(container); }

  @Override
  public void select() {}

  @Override
  public boolean updateValue(AbstractStruct struct)
  {
    // notifying listeners
    fireValueUpdated(new UpdateEvent(this, struct));

    return true;
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Writable">
  @Override
  public void write(OutputStream os) throws IOException
  {
    os.write(known);
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Readable">
  @Override
  public int read(ByteBuffer buffer, int offset)
  {
    buffer.position(offset);
    buffer.get(known);
    return offset + known.length;
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="TableModel">
  @Override
  public int getRowCount() { return creatures.size(); }
  @Override
  public int getColumnCount() { return 5; }
  @Override
  public String getColumnName(int columnIndex)
  {
    switch (columnIndex) {
      case 0: return "Known?";
      case 1: return "#";
      case 2: return "Name";
      case 3: return "Description";
      case 4: return "Kill variable";
      default: return null;
    }
  }
  @Override
  public Class<?> getColumnClass(int columnIndex)
  {
    switch (columnIndex) {
      case 0: return Boolean.class;
      case 1: return Integer.class;
      case 2: return String.class;
      case 3: return String.class;
      case 4: return String.class;
      default: return null;
    }
  }
  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) { return columnIndex == 0; }
  @Override
  public Object getValueAt(int rowIndex, int columnIndex)
  {
    final Creature cre = creatures.get(rowIndex);
    switch (columnIndex) {
      case 0: return isKnown(rowIndex);
      case 1: return rowIndex + 1;
      case 2: return cre.getName();
      case 3: return cre.getDesc();
      case 4: return cre.killVarName;
      default: return null;
    }
  }
  @Override
  public void setValueAt(Object aValue, int rowIndex, int columnIndex)
  {
    if (columnIndex == 0) {
      known[rowIndex] = (byte)(Boolean.TRUE.equals(aValue) ? 1 : 0);
      fireTableChanged(rowIndex);
    }
  }

  @Override
  public void addTableModelListener(TableModelListener l)
  {
    listenerList.add(TableModelListener.class, l);
  }

  @Override
  public void removeTableModelListener(TableModelListener l)
  {
    listenerList.remove(TableModelListener.class, l);
  }
  //</editor-fold>

  /**
   * Returns {@code true} if specified creature is included in the bestiary.
   *
   * @param creatureIndex Creature index in the bestiaty field
   * @return {@code true} if player already see this creature in a game, {@code false} otherwise
   */
  public boolean isKnown(int creatureIndex) { return known[creatureIndex] != 0; }

  private void fireTableChanged(int row)
  {
    final TableModelEvent e = new TableModelEvent(this, row, row);
    // Guaranteed to return a non-null array
    final Object[] listeners = listenerList.getListenerList();
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length-2; i >= 0; i -= 2) {
      if (listeners[i] == TableModelListener.class) {
        ((TableModelListener)listeners[i+1]).tableChanged(e);
      }
    }
  }

  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (int i = 0; i < known.length; ++i) {
      if (!isKnown(i)) { continue; }

      if (!first) { sb.append("; "); }
      first = false;

      if (i < creatures.size()) {
        sb.append(creatures.get(i).getName());
      }
      sb.append('(').append(i+1).append(')');
    }
    return first ? "No known creatures" : sb.toString();
  }

  /**
   * Read all creatures from {@code "beast.ini"} file in game root. File can contain
   * creatures with numbers in range {@code [0; 256]}, all creatures with other
   * numbers will be skipped and log message will be writed to the {@link System#err
   * standard error stream}.
   *
   * @return List with creatures found in the file. If file has incorrect format
   *         returns empty list
   */
  private static List<Creature> readCreatures()
  {
    final String filename = "beast.ini";
    final Path beast = Profile.getGameRoot().resolve(filename);
    return readCreatures(filename, IniMapCache.get(new FileResourceEntry(beast)));
  }

  /**
   * Read all creatures from specified ini file in game root. File can contain
   * creatures with numbers in range {@code [0; 256]}, all creatures with other
   * numbers will be skipped and log message will be writed to the {@link System#err
   * standard error stream}.
   *
   * @param filename Filename from which file was read, for logging
   * @param ini File to read. Must not be {@code null}
   *
   * @return List with creatures found in the file. If file has incorrect format
   *         returns empty list
   */
  private static List<Creature> readCreatures(String filename, IniMap ini)
  {
    final IniMapSection init = ini.getSection("init");
    if (init == null) {
      System.err.println(filename + ": [init] section not found in the file. Creatures not loaded");
      return Collections.emptyList();
    }
    final IniMapEntry entry = init.getEntry("beastcount");
    if (entry == null) {
      System.err.println(filename + ": \"beastcount\" key in [init] section not found. Creatures not loaded");
      return Collections.emptyList();
    }
    final Integer count = entry.getIntValue();
    if (count == null) {
      System.err.println(filename + ": \"beastcount\" key in [init] section: expected integer buf found "
              + entry.getValue() + ". Creatures not loaded");
      return Collections.emptyList();
    }

    final ArrayList<Creature> result = new ArrayList<>(count.intValue());
    for (final IniMapSection section : ini) {
      final int i;
      try {
        i = Integer.parseInt(section.getName());
      } catch (NumberFormatException ex) {
        continue;
      }
      if (i < 0 || i >= 256) {
        System.err.println(filename + ": invalid creature number "+i+", expected number in range [0; 256]. Creature skipped");
        continue;
      }
      result.ensureCapacity(i);
      result.add(new Creature(section));
    }
    return result;
  }
}
