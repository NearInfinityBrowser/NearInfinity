// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import org.infinity.gui.StructViewer;
import org.infinity.gui.ViewerUtil;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
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
 * Field that represents indexed color or color range.
 *
 * <h2>Bean property</h2>
 * When this field is child of {@link AbstractStruct}, then changes of its internal
 * value reported as {@link PropertyChangeEvent}s of the {@link #getParent() parent}
 * struct.
 * <ul>
 * <li>Property name: {@link #getName() name} of this field</li>
 * <li>Property type: {@code int}</li>
 * <li>Value meaning: index of the color in the color table</li>
 * </ul>
 */
public class ColorValue extends Datatype implements Editable, IsNumeric
{
  private static final int DEFAULT_COLOR_WIDTH  = 16;
  private static final int DEFAULT_COLOR_HEIGHT = 24;

  private final HashMap<Integer, String> randomColors = new HashMap<>();

  private int number;
  private JList<Image> colorList;
  private ResourceEntry colorEntry; // the source of color ranges
  private IdsMap colorMap;          // provides an optional symbolic color name

  public ColorValue(ByteBuffer buffer, int offset, int length, String name, boolean allowRandom)
  {
    this(buffer, offset, length, name, allowRandom, null);
  }

  public ColorValue(ByteBuffer buffer, int offset, int length, String name, boolean allowRandom, String bmpFile)
  {
    super(offset, length, name);
    init(bmpFile, allowRandom);
    read(buffer, offset);
  }

  private void init(String bmpFile, boolean allowRandom)
  {
    if (bmpFile != null && ResourceFactory.resourceExists(bmpFile)) {
      this.colorEntry = ResourceFactory.getResourceEntry(bmpFile);
    }
    if (ResourceFactory.resourceExists("CLOWNCLR.IDS")) {
      this.colorMap = IdsMapCache.get("CLOWNCLR.IDS");
    }

    ResourceEntry randomEntry = null;
    if (allowRandom && ResourceFactory.resourceExists("RANDCOLR.2DA")) {
      randomEntry = ResourceFactory.getResourceEntry("RANDCOLR.2DA");
    }

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

//--------------------- Begin Interface Editable ---------------------

  @Override
  public JComponent edit(ActionListener container)
  {
    int defaultColorWidth = (colorEntry == null) ? DEFAULT_COLOR_WIDTH : 0;
    ColorListModel colorModel = new ColorListModel(this, defaultColorWidth, DEFAULT_COLOR_HEIGHT);
    colorList = new JList<>(colorModel);
    colorList.setCellRenderer(new ColorCellRenderer(this));
    colorList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    colorList.setBorder(BorderFactory.createLineBorder(Color.GRAY));
    colorList.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent event)
      {
        if (event.getClickCount() == 2) {
          container.actionPerformed(new ActionEvent(this, 0, StructViewer.UPDATE_VALUE));
        }
      }
    });
    colorList.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent event)
      {
        if (event.getKeyCode() == KeyEvent.VK_ENTER) {
          container.actionPerformed(new ActionEvent(this, 0, StructViewer.UPDATE_VALUE));
        }
      }
    });
    JScrollPane scroll = new JScrollPane(colorList);
    scroll.setBorder(BorderFactory.createEmptyBorder());

    int selection = Math.min(colorList.getModel().getSize(), Math.max(0, getValue()));
    colorList.setSelectedIndex(selection);

    JButton bUpdate = new JButton("Update value", Icons.getIcon(Icons.ICON_REFRESH_16));
    bUpdate.addActionListener(container);
    bUpdate.setActionCommand(StructViewer.UPDATE_VALUE);

    JPanel panel = new JPanel(new GridBagLayout());

    GridBagConstraints gbc = new GridBagConstraints();
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                            GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    panel.add(scroll, gbc);

    gbc = ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                            GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 0, 0);
    panel.add(bUpdate, gbc);

    Dimension dim = (colorMap != null) ? new Dimension(DIM_MEDIUM.width + 100, DIM_MEDIUM.height) : DIM_MEDIUM;
    panel.setMinimumSize(Misc.getScaledDimension(dim));
    panel.setPreferredSize(Misc.getScaledDimension(dim));

    return panel;
  }

  @Override
  public void select()
  {
    colorList.ensureIndexIsVisible(colorList.getSelectedIndex());
  }

  @Override
  public boolean updateValue(AbstractStruct struct)
  {
    if (colorList.getSelectedIndex() >= 0) {
      if (number != colorList.getSelectedIndex()) {
        long oldValue = getLongValue();
        setValue(colorList.getSelectedIndex());

        // notifying listeners
        if (getLongValue() != oldValue) {
          fireValueUpdated(new UpdateEvent(this, struct));
        }
      }

      return true;
    }

    return false;
  }

//--------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    writeInt(os, number);
  }

//--------------------- End Interface Writeable ---------------------

//--------------------- Begin Interface Readable ---------------------

  @Override
  public int read(ByteBuffer buffer, int offset)
  {
    buffer.position(offset);
    switch (getSize()) {
      case 1:
        number = buffer.get() & 0xff;
        break;
      case 2:
        number = buffer.getShort() & 0xffff;
        break;
      case 4:
        number = buffer.getInt() & 0x7fffffff;
        break;
      default:
        throw new IllegalArgumentException();
    }

    return offset + getSize();
  }

//--------------------- End Interface Readable ---------------------

//--------------------- Begin Interface IsNumeric ---------------------

  @Override
  public long getLongValue()
  {
    return (long)number & 0x7fffffffL;
  }

  @Override
  public int getValue()
  {
    return number;
  }

//--------------------- End Interface IsNumeric ---------------------

  @Override
  public String toString()
  {
    String retVal = "Color index " + Integer.toString(number);
    String name = getColorName(number);
    if (name != null) {
      retVal += " (" + name + ")";
    }
    return retVal;
  }

  @Override
  public int hashCode()
  {
    int hash = super.hashCode();
    hash = 31 * hash + Integer.hashCode(number);
    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (!super.equals(o) || !(o instanceof ColorValue)) {
      return false;
    }
    ColorValue other = (ColorValue)o;
    boolean retVal = (number == other.number);
    return retVal;
  }

  /**
   * Returns the name associated with the specified color entry.
   * Returns {@code null} if no name is available.
   */
  public String getColorName(int index)
  {
    String retVal = randomColors.get(Integer.valueOf(index));
    if (retVal == null) {
      retVal = lookupColorName(colorMap, index, true);
    }
    return retVal;
  }

  /**
   * Returns a symbolic color name based on the specified IDS lookup.
   * Returns {@code null} if no lookup table is available.
   */
  public static String lookupColorName(IdsMap colorMap, int index, boolean prettify)
  {
    String retVal = null;
    if (colorMap != null) {
      IdsMapEntry e = colorMap.get(index);
      if (e != null) {
        retVal = prettify ? Misc.prettifySymbol(e.getSymbol()) : e.getSymbol();
      } else {
        retVal = "Unknown";
      }
    }
    return retVal;
  }

  private void setValue(int newValue)
  {
    final int oldValue = number;
    number = newValue;
    if (oldValue != newValue) {
      firePropertyChange(oldValue, newValue);
    }
  }

//-------------------------- INNER CLASSES --------------------------

  private static final class ColorCellRenderer extends DefaultListCellRenderer
  {
    private static final Border DEFAULT_NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);

    private final ColorValue colorValue;

    public ColorCellRenderer(ColorValue cv)
    {
      super();
      this.colorValue = Objects.requireNonNull(cv);
      setVerticalAlignment(SwingConstants.CENTER);
      setHorizontalTextPosition(SwingConstants.RIGHT);
      setVerticalTextPosition(SwingConstants.CENTER);
      setIconTextGap(8);
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus)
    {
      if (isSelected) {
        setBackground(list.getSelectionBackground());
        setForeground(list.getSelectionForeground());
      } else {
        setBackground(list.getBackground());
        setForeground(list.getForeground());
      }

      String label = "Index " + index;
      String name = colorValue.getColorName(index);
      if (name != null) {
        label += " (" + name + ")";
      }
      setText(label);
      if (value instanceof Image) {
        setIcon(new ImageIcon((Image)value));
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

  private static final class ColorListModel extends AbstractListModel<Image>
  {
    private final List<Image> colors = new ArrayList<>(256);
    private final ColorValue colorValue;

    public ColorListModel(ColorValue cv, int defaultWidth, int defaultHeight)
    {
      this.colorValue = Objects.requireNonNull(cv);
      initEntries(defaultWidth, defaultHeight);
    }

    @Override
    public int getSize()
    {
      return colors.size();
    }

    @Override
    public Image getElementAt(int index)
    {
      if (index >= 0 && index < getSize()) {
        return colors.get(index);
      }
      return null;
    }

    private void initEntries(int defaultWidth, int defaultHeight)
    {
      if (colorValue.colorEntry == null) {
        if (ResourceFactory.resourceExists("RANGES12.BMP")) {
          colorValue.colorEntry = ResourceFactory.getResourceEntry("RANGES12.BMP");
        } else if (ResourceFactory.resourceExists("MPALETTE.BMP")) {
          colorValue.colorEntry = ResourceFactory.getResourceEntry("MPALETTE.BMP");
        }
      }


      // scanning range of colors
      int maxValue = 255; // default size
      if (colorValue.colorEntry != null) {
        BufferedImage image = null;
        try {
          image = new GraphicsResource(colorValue.colorEntry).getImage();
          maxValue = Math.max(maxValue, image.getHeight() - 1);
          if (defaultWidth <= 0) {
            // auto-calculate color width
            defaultWidth = 192 / image.getWidth();
          }

          for (int idx = 0; idx <= maxValue; idx++) {
            BufferedImage range;
            if (!colorValue.randomColors.containsKey(Integer.valueOf(idx)) && idx < image.getHeight()) {
              // fixed color
              range = getFixedColor(image, idx, defaultWidth, defaultHeight);
            } else {
              // random color or invalid entry
              range = getVirtualColor(idx, defaultWidth, defaultHeight);
            }
            colors.add(range);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    // Returns an image derived from the specified color range bitmap
    private BufferedImage getFixedColor(BufferedImage colorRanges, int index, int width, int height)
    {
      BufferedImage retVal = null;

      if (colorRanges != null && index >= 0 && index < colorRanges.getHeight()) {
        retVal = new BufferedImage(colorRanges.getWidth() * width, height, colorRanges.getType());
        Graphics2D g = retVal.createGraphics();
        try {
          g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
          g.drawImage(colorRanges, 0, 0, retVal.getWidth(), retVal.getHeight(), 0, index, colorRanges.getWidth(), index+1, null);
          g.setColor(Color.BLACK);
          g.setStroke(new BasicStroke(1.0f));
          g.drawRect(0, 0, retVal.getWidth() - 1, retVal.getHeight() - 1);
        } finally {
          g.dispose();
        }
      }

      return retVal;
    }

    // Returns an image describing a random color or invalid color entry
    private BufferedImage getVirtualColor(int index, int width, int height)
    {
      BufferedImage retVal = null;

      Color invalidColor = new Color(0xe0e0e0);
      boolean isRandom = colorValue.randomColors.containsKey(Integer.valueOf(index));
      retVal = new BufferedImage(12 * width, height, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = retVal.createGraphics();
      try {
        g.setColor(isRandom ? Color.LIGHT_GRAY : invalidColor);
        g.fillRect(0, 0, retVal.getWidth(), retVal.getHeight());
        g.setFont(new JLabel().getFont());
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.0f));
        g.drawRect(0, 0, retVal.getWidth() - 1, retVal.getHeight() - 1);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        String msg = isRandom ? "(Random)" : "(Invalid)";
        FontMetrics fm = g.getFontMetrics();
        Rectangle2D rect = fm.getStringBounds(msg, g);
        g.drawString(msg,
                    (float)(retVal.getWidth() - rect.getWidth()) / 2.0f,
                    (float)(retVal.getHeight() - rect.getY()) / 2.0f);
      } finally {
        g.dispose();
      }

      return retVal;
    }
  }
}
