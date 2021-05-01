// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.browser;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import org.infinity.gui.BrowserMenuBar;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.graphics.GraphicsResource;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.IdsMap;
import org.infinity.util.IdsMapCache;
import org.infinity.util.IdsMapEntry;
import org.infinity.util.Misc;
import org.infinity.util.Table2da;
import org.infinity.util.Table2daCache;

/**
 * {@code ComboBoxModel} for the color selection combo box used in the Creature Animation Browser.
 */
public class ColorSelectionModel extends AbstractListModel<ColorSelectionModel.ColorEntry>
implements ComboBoxModel<ColorSelectionModel.ColorEntry>
{
  private static final HashMap<Integer, String> randomColors = new HashMap<>();

  private final List<ColorEntry> colorList = new ArrayList<>(256);
  private final ColorCellRenderer renderer = new ColorCellRenderer();

  private Object selectedItem;
  private ResourceEntry colorEntry;
  private IdsMap colorMap;

  public ColorSelectionModel()
  {
    this(null);
  }

  public ColorSelectionModel(ResourceEntry bmpResource)
  {
    super();
    this.colorMap = ResourceFactory.resourceExists("CLOWNCLR.IDS") ? IdsMapCache.get("CLOWNCLR.IDS") : null;
    setColorRangesEntry(bmpResource);
  }

  /** Returns the {@code ListCellRenderer} instance associated with the list model. */
  public ColorCellRenderer getRenderer() { return renderer; }

  /**
   * Returns the {@code ResourceEntry} instance of the BMP resource containing color ranges.
   * Returns {@code null} if no BMP resource is available.
   */
  public ResourceEntry getColorRangesEntry() { return colorEntry; }

  public void setColorRangesEntry(ResourceEntry bmpResource)
  {
    if (bmpResource == null) {
      if (Profile.getGame() == Profile.Game.PST || Profile.getGame() == Profile.Game.PSTEE) {
        bmpResource = ResourceFactory.getResourceEntry("PAL32.BMP");
      } else {
        bmpResource = ResourceFactory.getResourceEntry("RANGES12.BMP");
        if (bmpResource == null) {
          bmpResource = ResourceFactory.getResourceEntry("MPALETTE.BMP");
        }
      }
    }

    if ((colorEntry != null && !colorEntry.equals(bmpResource)) ||
        (colorEntry == null && bmpResource != null)) {
      colorEntry = bmpResource;
      init();
    }
  }

  public void reload()
  {
    initRandomColors(true);
    init();
  }

  /**
   * Returns the index-position of the specified object in the list.
   * @param anItem a {@code ColorEntry} object or {@code Number} object.
   * @return an int representing the index position, where 0 is the first position. Returns -1
   *         if the item could not be found in the list.
   */
  public int getIndexOf(Object anItem)
  {
    if (anItem instanceof ColorEntry) {
      return colorList.indexOf(anItem);
    } else if (anItem instanceof Number) {
      final int colIdx = ((Number)anItem).intValue();
      return IntStream
          .range(0, colorList.size())
          .filter(i -> colorList.get(i).getIndex() == colIdx)
          .findAny()
          .orElse(-1);
    }
    return -1;
  }

  /** Empties the list. */
  public void removeAllElements()
  {
    if (!colorList.isEmpty()) {
      int oldSize = colorList.size();
      colorList.clear();
      selectedItem = null;
      if (oldSize > 0) {
        fireIntervalRemoved(this, 0, oldSize - 1);
      }
    } else {
      selectedItem = null;
    }
  }

//--------------------- Begin Interface ListModel ---------------------

  @Override
  public int getSize()
  {
    return colorList.size();
  }

  @Override
  public ColorEntry getElementAt(int index)
  {
    if (index >= 0 && index < colorList.size()) {
      return colorList.get(index);
    } else {
      return null;
    }
  }

//--------------------- End Interface ListModel ---------------------

//--------------------- Begin Interface ComboBoxModel ---------------------

  @Override
  public void setSelectedItem(Object anItem)
  {
    if ((selectedItem != null && !selectedItem.equals(anItem)) ||
        selectedItem == null && anItem != null) {
      selectedItem = anItem;
      fireContentsChanged(this, -1, -1);
    }
  }

  @Override
  public Object getSelectedItem()
  {
    return selectedItem;
  }

//--------------------- End Interface ComboBoxModel ---------------------

  private void init()
  {
    removeAllElements();

    initRandomColors(false);

    BufferedImage image = null;
    try {image = new GraphicsResource(getColorRangesEntry()).getImage(); } catch (Exception e) {}

    int max = (image != null) ? image.getHeight() - 1 : 0;
    if (!randomColors.isEmpty()) {
      max = Math.max(max, Collections.max(randomColors.keySet()));
    }

    for (int i = 0; i <= max; i++) {
      String name = randomColors.get(Integer.valueOf(i));
      if (name != null) {
        colorList.add(new ColorEntry(i, name, true));
      } else {
        name = "";
        if (colorMap != null) {
          IdsMapEntry idsEntry = colorMap.get(i);
          name = (idsEntry != null) ? Misc.prettifySymbol(idsEntry.getSymbol()) : "";
        }
        colorList.add(new ColorEntry(image, i, name));
      }
    }

    if (!colorList.isEmpty()) {
      fireIntervalAdded(this, 0, colorList.size() - 1);
    }

    setSelectedItem(getElementAt(0));
  }

  private static synchronized void initRandomColors(boolean forced)
  {
    if (forced) {
      randomColors.clear();
    }

    if (randomColors.isEmpty()) {
      ResourceEntry randomEntry = ResourceFactory.getResourceEntry("RANDCOLR.2DA");

      // collecting valid random color indices
      if (randomEntry != null) {
        Table2da table = Table2daCache.get(randomEntry);
        for (int col = 1, count = table.getColCount(); col < count; col++) {
          int index = Misc.toNumber(table.get(0, col), -1);
          String name = Misc.prettifySymbol(table.getHeader(col));
          if (index >= 0 && index < 256) {
            randomColors.put(index, name);
          }
        }
      }
    }
  }

//-------------------------- INNER CLASSES --------------------------

  public static class ColorCellRenderer extends DefaultListCellRenderer
  {
    private static final Border DEFAULT_NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);

    public ColorCellRenderer()
    {
      super();
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus)
    {
      if (value == null || !(value instanceof ColorEntry)) {
        return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      }

      ColorEntry entry = (ColorEntry)value;
      if (isSelected) {
        setBackground(list.getSelectionBackground());
        setForeground(list.getSelectionForeground());
      } else {
        setBackground(list.getBackground());
        setForeground(list.getForeground());
      }

      setText(entry.toString());

      if (entry.getImage() != null) {
        setIcon(new ImageIcon(entry.getImage()));
        setIconTextGap(8);
      } else {
        setIcon(null);
      }

      setEnabled(list.isEnabled());
      setFont(list.getFont());

      Border border = null;
      if (cellHasFocus) {
        if (isSelected) {
          border = UIManager.getBorder("List.focusSelectedCellHighlightBorder");
        }
        if (border == null) {
          border = UIManager.getBorder("List.focusCellHighlightBorder");
        }
      } else {
        border = UIManager.getBorder("List.cellNoFocusBorder");
        if (border == null) {
          border = DEFAULT_NO_FOCUS_BORDER;
        }
      }
      setBorder(border);

      return this;
    }
  }

  public static class ColorEntry implements Comparable<ColorEntry>
  {
    public enum State {
      /** Indicates a fixed color entry. */
      FIXED,
      /** Indicates a randomized color entry. */
      RANDOM,
      /** Indicates a non-existing color entry. */
      INVALID,
    }

    private static final int DEFAULT_IMAGE_WIDTH = 128; // total width of color range
    // take global font scaling into account
    private static final int DEFAULT_IMAGE_HEIGHT = 20 * BrowserMenuBar.getInstance().getGlobalFontSize() / 100;

    private static final Color COLOR_INVALID      = new Color(0xe0e0e0);
    private static final Color COLOR_RANDOM       = Color.LIGHT_GRAY;

    private static final String LABEL_INVALID     = "(Invalid)";
    private static final String LABEL_RANDOM      = "(Random)";

    private final int index;
    private final Image image;
    private String name;
    private State state;

    /** Creates a fixed color entry. */
    public ColorEntry(BufferedImage ranges, int index, String name)
    {
      this.index = Math.max(0, Math.min(255, index));
      this.name = (name != null) ? name.trim() : "";
      this.image = createFixedColor(ranges, index);
    }

    /**
     * Creates a random color or invalid color entry.
     * @param name
     * @param isRandom
     */
    public ColorEntry(int index, String name, boolean isRandom)
    {
      this.index = Math.max(0, Math.min(255, index));
      this.name = (name != null) ? name.trim() : "";
      this.image = isRandom ? createRandomColor() : createInvalidColor();
    }

    public int getIndex() { return index; }
    public Image getImage() { return image; }
    public String getName() { return name; }
    public State getState() { return state; }

    @Override
    public int compareTo(ColorEntry o)
    {
      return getIndex() - o.getIndex();
    }

    @Override
    public String toString()
    {
      if (getName().isEmpty()) {
        return Integer.toString(getIndex());
      } else {
        return getIndex() + " (" + getName() + ")";
      }
    }

    private Image createFixedColor(BufferedImage image, int index)
    {
      if (image == null || index < 0 || index >= image.getHeight()) {
        return createInvalidColor();
      }

      state = State.FIXED;
      BufferedImage range = new BufferedImage(DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT, image.getType());
      Graphics2D g = range.createGraphics();
      try {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(image, 0, 0, range.getWidth(), range.getHeight(), 0, index, image.getWidth(), index + 1, null);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.0f));
        g.drawRect(0, 0, range.getWidth() - 1, range.getHeight() - 1);
      } finally {
        g.dispose();
      }
      return range;
    }

    private Image createRandomColor() { return createCustomColor(COLOR_RANDOM, LABEL_RANDOM); }

    private Image createInvalidColor() { return createCustomColor(COLOR_INVALID, LABEL_INVALID); }

    private Image createCustomColor(Color color, String label)
    {
      state = (color != null) ? State.RANDOM : State.INVALID;
      Color col = (color != null) ? color : COLOR_INVALID;
      String text = (label != null) ? label : LABEL_INVALID;
      BufferedImage range = new BufferedImage(DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = range.createGraphics();
      try {
        g.setColor(col);
        g.fillRect(0, 0, range.getWidth(), range.getHeight());
        g.setFont(new JLabel().getFont().deriveFont(12.0f));
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.0f));
        g.drawRect(0, 0, range.getWidth() - 1, range.getHeight() - 1);
        if (!text.isEmpty()) {
          g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
          FontMetrics fm = g.getFontMetrics();
          Rectangle2D rect = fm.getStringBounds(label, g);
          g.drawString(text,
                       (float)(range.getWidth() - rect.getWidth()) / 2.0f,
                       (float)(range.getHeight() - rect.getY()) / 2.0f);
        }
      } finally {
        g.dispose();
      }
      return range;
    }
  }

}
