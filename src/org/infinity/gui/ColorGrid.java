// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.infinity.util.Platform;

/**
 * Implements a grid of selectable color indices.
 */
public class ColorGrid extends JPanel implements MouseListener, MouseMotionListener
{
  /** Supported frame types for selected color entries. */
  public enum Frame { SINGLE_LINE, DOUBLE_LINE }

  /** Only one color entry can be selected at a time. */
  public static final int SELECTION_SINGLE = 0;
  /** One or more color entries can be selected at a time. */
  public static final int SELECTION_MULTIPLE = 1;

  private static final int DRAG_DISABLED    = 0;  // no drag&drop
  private static final int DRAG_INITIALIZED = 1;  // a potential drag&drop has been initiated
  private static final int DRAG_ENABLED     = 2;  // drag&drop has been enabled

  private static final int MaxColorCount = 65536;
  private static final int DefaultColorCount = 16;
  private static final int DefaultColorsPerRow = 16;
  private static final int DefaultGap = 3;
  private static final int DefaultSelectionMode = SELECTION_SINGLE;
  private static final Dimension DefaultColorSize = new Dimension(16, 16);
  private static final Color DefaultColor = Color.BLACK;
  private static final Frame DefaultFrame = Frame.SINGLE_LINE;
  private static final Color DefaultFrameColor = Color.BLUE;
  private static final boolean DefaultDragDropEnabled = false;
  // Defines the square of the minimum dragging distance before activating drag&drop mode
  private static final int DragDropTriggerDistance2 = 16;
  // Background pattern is visible for semi-transparent color entries
  private static final TexturePaint BackgroundPattern = createBackgroundPattern();

  private final List<Color> listColors = new ArrayList<>();
  private final List<ActionListener> listActionListeners = new ArrayList<>();
  private final List<MouseOverListener> listMouseOverListeners = new ArrayList<>();
  private final List<ChangeListener> listChangeListeners = new ArrayList<>();
  // stores selected color entry indices
  private final List<Integer> listSelection = new ArrayList<>();

  private Frame frame;                // frame type
  private Color frameColor;           // frame color
  private int colorsPerRow;           // number of colors per row
  private boolean readOnly;           // indicates whether the user can interact with the color grid
  private Dimension colorSize;        // dimension of a single color entry
  private int gapX, gapY;             // horizontal and vertical gaps between color entries
  private int currentMouseOverIndex;  // current color index under the mouse cursor
  private int selectionMode;          // current selection mode
  private boolean isDragDropEnabled;
  private int DragDropMode;                 // current drag&drop mode
  private Point pDragDropStart;             // starting mouse position relative to color grid component
  private int dragDropStart, dragDropCur;   // initial and current color indices

  public ColorGrid()
  {
    this(DefaultColorCount, null);
  }

  public ColorGrid(int colorCount)
  {
    this(colorCount, null);
  }

  public ColorGrid(int colorCount, Collection<Color> colors)
  {
    super(true);
    init(colorCount, colors);
  }

  /**
   * Adds an ActionListener to the component.
   * ActionListener events will be triggered whenever a new color entry has been selected.
   */
  public void addActionListener(ActionListener l)
  {
    if (l != null) {
      if (listActionListeners.indexOf(l) < 0) {
        listActionListeners.add(l);
      }
    }
  }

  /** Returns an array of all registered ActionListeners. */
  public ActionListener[] getActionListeners()
  {
    ActionListener[] listeners = new ActionListener[listActionListeners.size()];
    for (int i = 0; i < listActionListeners.size(); i++) {
      listeners[i] = listActionListeners.get(i);
    }
    return listeners;
  }

  /** Removes an ActionListener from the component. */
  public void removeActionListener(ActionListener l)
  {
    if (l != null) {
      int idx = listActionListeners.indexOf(l);
      if (idx >= 0) {
        listActionListeners.remove(idx);
      }
    }
  }


  /**
   * Adds a MouseOverListener to the component.
   * MouseOverListener events will be triggered whenever the mouse cursor is over a new color entry.
   */
  public void addMouseOverListener(MouseOverListener l)
  {
    if (l != null) {
      if (listMouseOverListeners.indexOf(l) < 0) {
        listMouseOverListeners.add(l);
      }
    }
  }

  /** Returns an array of all registered MouseOverListeners. */
  public MouseOverListener[] getMouseOverListeners()
  {
    MouseOverListener[] listeners = new MouseOverListener[listMouseOverListeners.size()];
    for (int i = 0; i < listMouseOverListeners.size(); i++) {
      listeners[i] = listMouseOverListeners.get(i);
    }
    return listeners;
  }

  /** Removes a MouseOverListener from the component. */
  public void removeMouseOverListener(MouseOverListener l)
  {
    if (l != null) {
      int idx = listMouseOverListeners.indexOf(l);
      if (idx >= 0) {
        listMouseOverListeners.remove(idx);
      }
    }
  }


  /**
   * Adds a ChangeListener to the component.
   * ChangeListener events will be triggered whenever the palette definition of the grid has been
   * modified by the user.
   */
  public void addChangeListener(ChangeListener l)
  {
    if (l != null) {
      if (listChangeListeners.indexOf(l) < 0) {
        listChangeListeners.add(l);
      }
    }
  }

  /** Returns an array of all registered ChangeListeners. */
  public ChangeListener[] getChangeListeners()
  {
    ChangeListener[] listeners = new ChangeListener[listChangeListeners.size()];
    for (int i = 0; i < listChangeListeners.size(); i++) {
      listeners[i] = listChangeListeners.get(i);
    }
    return listeners;
  }

  /** Removes a ChangeListener from the component. */
  public void removeChangeListener(ChangeListener l)
  {
    if (l != null) {
      int idx = listChangeListeners.indexOf(l);
      if (idx >= 0) {
        listChangeListeners.remove(idx);
      }
    }
  }

  /** Returns the currently active selection mode (one of SELECTION_SINGLE or SELECTION_MULTIPLE) */
  public int getSelectionMode()
  {
    return selectionMode;
  }

  /**
   * Sets the current color selection mode.
   * @param mode Either one of {@link #SELECTION_SINGLE} or {@link #SELECTION_MULTIPLE}.
   */
  public void setSelectionMode(int mode)
  {
    if (mode == SELECTION_SINGLE || mode == SELECTION_MULTIPLE && selectionMode != mode) {
      selectionMode = mode;
      if (selectionMode == SELECTION_SINGLE && !listSelection.isEmpty()) {
        setSelectedIndex(listSelection.get(listSelection.size() - 1).intValue());
      }
    }
  }

  /** Returns whether the user can interact with the color grid (e.g. selecting color entries). */
  public boolean isReadOnly()
  {
    return readOnly;
  }

  /** Specify whether the user can interact with the color grid (e.g. by selecting a color entry). */
  public void setReadOnly(boolean set)
  {
    if (set != readOnly) {
      readOnly = set;
      if (getSelectedIndex() >= 0) {
        setSelectedIndex(-1);
      }
    }
  }

  /** Returns whether drag&drop capability has been enabled. */
  public boolean isDragDropEnabled()
  {
    return isDragDropEnabled;
  }

  /** Enables or disables the drag&drop capability of this color grid. */
  public void setDragDropEnabled(boolean enable)
  {
    if (enable != isDragDropEnabled) {
      isDragDropEnabled = enable;
    }
  }

  /**
   * Returns the number of colors displayed per row.
   * @return the number of colors per row.
   */
  public int getGridWidth()
  {
    return colorsPerRow;
  }

  /**
   * Resizes the component to allow the specified number of color entries to be placed in a single row.
   * @param width The number of color entries per row to display.
   * @throws IllegalArgumentException if width is out of bounds.
   */
  public void setGridWidth(int width)
  {
    if (width > 0 && width < getColorCount() && width != colorsPerRow) {
      colorsPerRow = width;
      updateSize();
    } else {
      throw new IllegalArgumentException("Invalid grid width: " + width);
    }
  }


  /** Returns the dimension of a single color entry. */
  public Dimension getColorEntrySize()
  {
    return new Dimension(colorSize);
  }

  /**
   * Specify the size for each individual color entry displayed by the component.
   * Specifying {@code null} restores the default size.
   */
  public void setColorEntrySize(Dimension size)
  {
    if (size != null) {
      if (size.width > 0 && size.height > 0) {
        colorSize = new Dimension(size);
      }
    } else {
      colorSize = new Dimension(DefaultColorSize);
    }
    updateSize();
  }


  /** Returns the horizontal gap between color entries. */
  public int getColorEntryHorizontalGap()
  {
    return gapX;
  }

  /** Returns the vertical gap between color entries. */
  public int getColorEntryVerticalGap()
  {
    return gapY;
  }

  /** Defines the horizintal gap between color entries. Default is 3 pixels. */
  public void setColorEntryHorizontalGap(int gap)
  {
    if (gap >= 0 && gap != gapX) {
      this.gapX = gap;
      updateSize();
    }
  }

  /** Defines the vertical gap between color entries. Default is 3 pixels. */
  public void setColorEntryVerticalGap(int gap)
  {
    if (gap >= 0 && gap != gapY) {
      this.gapY = gap;
      updateSize();
    }
  }


  /** Returns the number of defined colors. */
  public int getColorCount()
  {
    return listColors.size();
  }

  /**
   * Specify a new number of colors to be displayed by this component. New color entries are always
   * filled with the default color "black".
   * @param newCount The new number of colors to be displayed.
   */
  public void setColorCount(int newCount)
  {
    if (newCount > 0 && newCount < MaxColorCount && newCount != getColorCount()) {
      if (newCount < getColorCount()) {
        // removing entries
        while (listColors.size() > newCount) {
          listColors.remove(listColors.size() - 1);
        }
      } else {
        // adding entries
        while (listColors.size() < newCount) {
          listColors.add(DefaultColor);
        }
      }
      updateSize();
    }
  }


  /**
   * Returns the color at the specified palette entry.
   * @param index The index of the desired color entry.
   * @return A Color object of the specified color entry.
   * @throws IndexOutOfBoundsException if index is out of bounds.
   */
  public Color getColor(int index)
  {
    if (index >= 0 && index < getColorCount()) {
      return listColors.get(index);
    } else {
      throw new IndexOutOfBoundsException(String.format("%d out of bounds [0, %d]", index, getColorCount()));
    }
  }

  /**
   * Sets the value of a single color. (Note: alpha component will be ignored.)
   * @param index The color entry to set.
   * @param color The new color value.
   */
  public void setColor(int index, Color color)
  {
    if (index >= 0 && index < getColorCount() && color != null) {
      listColors.set(index, new Color(color.getRGB(), true));
      repaint();
    }
  }

  /**
   * Sets the values of a number of colors, starting at a specified color index.
   * @param index The start index for the color values to set
   * @param colors An array of color values to set.
   */
  public void setColor(int index, Color[] colors)
  {
    if (index >= 0 && index < getColorCount() && colors != null) {
      int cnt = colors.length;
      if (index + cnt > getColorCount()) {
        cnt = getColorCount() - index;
      }
      for (int i = 0; i < cnt; i++) {
        if (colors[i] != null) {
          listColors.set(index, new Color(colors[i].getRGB(), true));
        }
      }
      repaint();
    }
  }


  /** Returns the frame type used for selected color entries. */
  public Frame getSelectionFrame()
  {
    return frame;
  }

  /** Sets a new Frame type to show for selected color entries. */
  public void setSelectionFrame(Frame frameType)
  {
    if (frameType != null && frameType != frame) {
      this.frame = frameType;
      repaint();
    }
  }

  /** Returns the frame color */
  public Color getSelectionFrameColor()
  {
    return frameColor;
  }

  /** Sets a new frame color. Specifying {@code null} sets the default frame color. */
  public void setSelectionFrameColor(Color color)
  {
    if (color != null) {
      frameColor = new Color(color.getRGB());
    } else {
      frameColor = DefaultFrameColor;
    }
    repaint();
  }

  /**
   * Returns the color value at the selected color entry. In multiple selection mode only the last
   * selected color value will be returned.
   * Returns {@code null} if no color has been selected.
   */
  public Color getSelectedColor()
  {
    if (!listSelection.isEmpty()) {
      return listColors.get(listSelection.get(listSelection.size() - 1).intValue());
    } else {
      return null;
    }
  }

  /**
   * Returns a list of all selected color entries. Returns an empty array if no color has been
   * selected.
   */
  public Color[] getSelectedColors()
  {
    Color[] retVal = new Color[listSelection.size()];
    for (int i = 0; i < listSelection.size(); i++) {
      retVal[i] = listColors.get(listSelection.get(i).intValue());
    }
    return retVal;
  }

  /** Returns whether the specified color entry index is currently selected. */
  public boolean isSelectedIndex(int index)
  {
    if (index >= 0 && index < getColorCount()) {
      int idx = listSelection.indexOf(Integer.valueOf(index));
      return (idx >= 0);
    }
    return false;
  }

  /** Returns {@code true} if nothing is selected, {@code false} otherwise. */
  public boolean isSelectionEmpty()
  {
    return listSelection.isEmpty();
  }

  /**
   * Returns the currently selected color in single selection mode or the last selected value
   * in multiple selections mode..
   * Returns -1 if no color has been selected.
   */
  public int getSelectedIndex()
  {
    if (!listSelection.isEmpty()) {
      return listSelection.get(listSelection.size() - 1).intValue();
    } else {
      return -1;
    }
  }

  /**
   * Returns the selected color entry indices as an array of integers.
   * @return An array of all selected color entry indices.
   */
  public int[] getSelectedIndices()
  {
    int[] retVal = new int[listSelection.size()];
    for (int i = 0; i < listSelection.size(); i++) {
      retVal[i] = listSelection.get(i).intValue();
    }
    return retVal;
  }

  /**
   * Selects the color entry at the specified index. Previously selected entries will be
   * unselected automatically.
   */
  public void setSelectedIndex(int newIndex)
  {
    setSelectedIndices(new int[]{newIndex});
  }

  /** Selects the specified indices. Previously selected entries will be unselected automatically. */
  public void setSelectedIndices(int[] indices)
  {
    listSelection.clear();
    if (indices != null) {
      for (int i = 0; i < indices.length; i++) {
        if (indices[i] >= 0 && indices[i] < getColorCount()) {
          int idx = listSelection.indexOf(Integer.valueOf(indices[i]));
          if (idx < 0) {
            listSelection.add(Integer.valueOf(indices[i]));
          }
        }
      }
    }
    repaint();
  }

  /**
   * Adds the specified index to the current selection in multiple selection mode.
   * Behaves like {@link #setSelectedIndex(int)} in single selection.
   */
  public void addSelectedIndex(int index)
  {
    addSelectedIndices(new int[]{index});
  }

  /**
   * Adds the specified indices to the current selection in multiple selection mode.
   * Behaves like {@link #setSelectedIndex(int)} in single selection, but selects only the last
   * entry in the specified index array.
   * @param indices Array of indices to select.
   */
  public void addSelectedIndices(int[] indices)
  {
    if (getSelectionMode() == SELECTION_SINGLE) {
      setSelectedIndex((indices != null && indices.length > 0) ? indices[indices.length - 1] : -1);
    } else {
      if (indices != null) {
        for (int i = 0; i < indices.length; i++) {
          if (indices[i] >= 0 && indices[i] < getColorCount()) {
            int idx = listSelection.indexOf(Integer.valueOf(indices[i]));
            if (idx < 0) {
              listSelection.add(Integer.valueOf(indices[i]));
            }
          }
        }
        repaint();
      }
    }
  }

  /** Removes the specified index from the selection. */
  public void removeSelectedIndex(int index)
  {
    removeSelectedIndices(new int[]{index});
  }

  /**
   * Removes the specified list of indices from the selection.
   * @param indices Array of indices to unselect.
   */
  public void removeSelectedIndices(int[] indices)
  {
    if (indices != null) {
      for (int i = 0; i < indices.length; i++) {
        if (indices[i] >= 0 && indices[i] < getColorCount()) {
          int idx = listSelection.indexOf(Integer.valueOf(indices[i]));
          if (idx >= 0) {
            listSelection.remove(idx);
          }
        }
      }
      repaint();
    }
  }

  /** Clears all selected color entries. */
  public void clearSelection()
  {
    if (!listSelection.isEmpty()) {
      listSelection.clear();
      repaint();
    }
  }

  // Fires ActionListener events for all registered listeners
  protected void fireActionListener()
  {
    ActionEvent event = new ActionEvent(this, 0, "", System.currentTimeMillis(), 0);
    for (int i = 0; i < listActionListeners.size(); i++) {
      listActionListeners.get(i).actionPerformed(event);
    }
  }

  // Fires MouseOverListener events for all registered listeners
  protected void fireMouseOverListener(int index)
  {
    MouseOverEvent event = new MouseOverEvent(this, index);
    for (int i = 0; i < listMouseOverListeners.size(); i++) {
      listMouseOverListeners.get(i).mouseOver(event);
    }
  }

  // Fires ChangeListener events for all registered listeners
  protected void fireChangeListener()
  {
    ChangeEvent event = new ChangeEvent(this);
    for (int i = 0; i < listChangeListeners.size(); i++) {
      listChangeListeners.get(i).stateChanged(event);
    }
  }


  @Override
  public void paintComponent(Graphics g)
  {
    super.paintComponent(g);
    updateDisplay(g);
  }

  // Creates a simple checkerboard pattern as texture object.
  private static TexturePaint createBackgroundPattern()
  {
    // A simple checkerboard pattern
    int[] checker = {
        0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffc0c0c0, 0xffc0c0c0, 0xffc0c0c0, 0xffc0c0c0,
        0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffc0c0c0, 0xffc0c0c0, 0xffc0c0c0, 0xffc0c0c0,
        0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffc0c0c0, 0xffc0c0c0, 0xffc0c0c0, 0xffc0c0c0,
        0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffc0c0c0, 0xffc0c0c0, 0xffc0c0c0, 0xffc0c0c0,
        0xffc0c0c0, 0xffc0c0c0, 0xffc0c0c0, 0xffc0c0c0, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff,
        0xffc0c0c0, 0xffc0c0c0, 0xffc0c0c0, 0xffc0c0c0, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff,
        0xffc0c0c0, 0xffc0c0c0, 0xffc0c0c0, 0xffc0c0c0, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff,
        0xffc0c0c0, 0xffc0c0c0, 0xffc0c0c0, 0xffc0c0c0, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff,
      };
    BufferedImage buf = new BufferedImage(8,  8, BufferedImage.TYPE_INT_ARGB);
    int[] raster = ((DataBufferInt)buf.getRaster().getDataBuffer()).getData();
    System.arraycopy(checker, 0, raster, 0, checker.length);
    TexturePaint tp = new TexturePaint(buf, new Rectangle(0, 0, 8, 8));
    return tp;
  }

  // First-time initializations
  private void init(int count, Collection<Color> colors)
  {
    // setting default properties
    frame = DefaultFrame;
    frameColor = DefaultFrameColor;
    colorsPerRow = DefaultColorsPerRow;
    selectionMode = DefaultSelectionMode;
    colorSize = (Dimension)DefaultColorSize.clone();
    gapX = gapY = DefaultGap;
    currentMouseOverIndex = -1;
    isDragDropEnabled = DefaultDragDropEnabled;

    // initializing color list
    if (count < 1) count = 1; else if (count > MaxColorCount) count = MaxColorCount;
    listColors.clear();
    int idx = 0;
    Iterator<Color> iter = null;
    if (colors != null) {
      iter = colors.iterator();
    }
    while (idx < count) {
      if (iter != null && iter.hasNext()) {
        Color c = iter.next();
        if (c != null) {
          listColors.add(new Color(c.getRGB(), true));
        } else {
          listColors.add(DefaultColor);
        }
      } else {
        listColors.add(DefaultColor);
      }
      idx++;
    }
    updateSize();

    addMouseListener(this);
    addMouseMotionListener(this);
  }

  // Updates the dimension of the component
  private void updateSize()
  {
    int colCount = (listColors.size()+colorsPerRow-1) / colorsPerRow;
    int w = gapX + colorsPerRow*(gapX + colorSize.width);
    int h = gapY + colCount*(gapY + colorSize.height);
    setPreferredSize(new Dimension(w, h));
    setMinimumSize(getPreferredSize());
    validate();
    repaint();
  }

  // Paints the component's content
  private void updateDisplay(Graphics g)
  {
    if (g != null) {
      Graphics2D g2d = (Graphics2D)g;
      // painting color boxes
      for (int i = 0; i < getColorCount(); i++) {
        int col = i % colorsPerRow;
        int row = i / colorsPerRow;
        int x = gapX + col*(gapX + colorSize.width);
        int y = gapY + row*(gapY + colorSize.height);

        if (Platform.IS_UNIX) {
          // XXX: Workaround for issue: java.lang.InternalError "Surface not cachable"
          g2d.setColor(new Color(0xffffff));
          g2d.fillRect(x, y, colorSize.width, colorSize.height);
          g2d.setColor(new Color(0xc0c0c0));
          for (int ofsY = 0; ofsY < colorSize.height; ofsY += 4) {
            for (int ofsX = 0; ofsX < colorSize.width; ofsX += 4) {
              if ((ofsY & 0x4) != (ofsX & 0x4)) {
                int boxH = Math.min(4, colorSize.height - ofsY);
                int boxW = Math.min(4, colorSize.width - ofsX);
                g2d.fillRect(x + ofsX, y + ofsY, boxW, boxH);
              }
            }
          }
        } else {
          g2d.setPaint(BackgroundPattern);
          g2d.fillRect(x, y, colorSize.width, colorSize.height);
          g2d.setPaint(null);
        }

        boolean alpha = (i != 0); // Color at index 0 is implicitly treated as transparent
        g.setColor(new Color(listColors.get(i).getRGB(), alpha));
        g.fillRect(x, y, colorSize.width, colorSize.height);
      }

      // painting frame around selected color entry
      for (int i = 0; i < listSelection.size(); i++) {
        int curIdx = listSelection.get(i).intValue();
        int col = curIdx % colorsPerRow;
        int row = curIdx / colorsPerRow;
        int x = gapX + col*(gapX + colorSize.width);
        int y = gapY + row*(gapY + colorSize.height);
        g.setColor(frameColor);
        switch (frame) {
          case SINGLE_LINE:
          {
            g.drawRect(x - 2, y - 2, colorSize.width + 3, colorSize.height + 3);
            break;
          }
          case DOUBLE_LINE:
          {
            g.drawRect(x - 1, y - 1, colorSize.width + 1, colorSize.height + 1);
            g.drawRect(x - 3, y - 3, colorSize.width + 5, colorSize.height + 5);
            break;
          }
        }
      }
    }
  }

  // Returns the color index at the specified coordinate
  private int getColorIndexAt(Point coord)
  {
    if (coord != null && (coord.x - gapX) >= 0 && (coord.y - gapY) >= 0 &&
        (coord.x - gapX) < getWidth() && (coord.y - gapY) < getHeight()) {
      int col = (coord.x - gapX) / (gapX + colorSize.width);
      int row = (coord.y - gapY) / (gapY + colorSize.height);
      int x = gapX + col*(gapX + colorSize.width);
      int y = gapY + row*(gapY + colorSize.height);
      if (coord.x >= x && coord.x < x + colorSize.width && coord.y >= y && coord.y < y + colorSize.height) {
        int index = row*colorsPerRow + col;
        if (index >= 0 && index < listColors.size()) {
          return index;
        }
      }
    }
    return -1;
  }


  // Returns whether a color drag is active
  private boolean isColorDragEnabled()
  {
    return (DragDropMode != DRAG_DISABLED);
  }

  // Enables/disables the drag&drop mode
  private void setColorDragEnabled(boolean enable, Point coords)
  {
    if (coords != null) {
      if (isDragDropEnabled && DragDropMode == DRAG_DISABLED && enable) {
        int idx = getColorIndexAt(coords);
        if (idx >= 0) {
          // enabling drag mode
          DragDropMode = DRAG_INITIALIZED;
          pDragDropStart = (Point)coords.clone();
          dragDropStart = dragDropCur = idx;
        }
      } else if (!enable) {
        // disabling drag mode and updating results
        setCursor(Cursor.getDefaultCursor());
        if (DragDropMode == DRAG_ENABLED) {
          if (dragDropStart != dragDropCur) {
            fireChangeListener();
          }
        }
        DragDropMode = DRAG_DISABLED;
      }
    }
  }

  // Call whenever the mouse cursor moves
  private void updateColorDrag(Point coords)
  {
    if (coords != null) {
      if (DragDropMode == DRAG_INITIALIZED) {
        int x = Math.abs(pDragDropStart.x - coords.x);
        int y = Math.abs(pDragDropStart.y - coords.y);
        if (x*x + y*y >= DragDropTriggerDistance2) {
          DragDropMode = DRAG_ENABLED;
          setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
      }

      if (DragDropMode == DRAG_ENABLED) {
        if (coords.x >= 0 && coords.x < getWidth() &&
            coords.y >= 0 && coords.y < getHeight()) {
          updateColorLocation(getColorIndexAt(coords));
        }
      }
    }
  }

  // Updates the locations of the colors in the grid
  private void updateColorLocation(int newIndex)
  {
    if (DragDropMode == DRAG_ENABLED) {
      if (newIndex >= 0 && dragDropCur != newIndex) {
        // updating color grid
        Color c = listColors.get(dragDropCur);
        listColors.remove(dragDropCur);
        listColors.add(newIndex, c);
        boolean isSelected = isSelectedIndex(dragDropCur);

        // updating selection list
        for (int i = 0; i < listSelection.size(); i++) {
          int idx = listSelection.get(i).intValue();
          if (idx < dragDropCur && idx >= newIndex) {
            listSelection.set(i, Integer.valueOf(idx+1));
          } else if (idx > dragDropCur && idx <= newIndex) {
            listSelection.set(i, Integer.valueOf(idx-1));
          }
        }
        repaint();

        if (isSelected) {
          removeSelectedIndex(dragDropCur);
          addSelectedIndex(newIndex);
        }

        dragDropCur = newIndex;
      }
    }
  }

//--------------------- Begin Interface MouseListener ---------------------

  @Override
  public void mouseClicked(MouseEvent event)
  {
  }

  @Override
  public void mousePressed(MouseEvent event)
  {
    // selecting color entry
    if (event.getSource() == this && event.getButton() == MouseEvent.BUTTON1) {
      if (!isReadOnly()) {
        int index = getColorIndexAt(event.getPoint());
        if (getSelectionMode() == SELECTION_SINGLE) {
          // single selection mode
          if (!isSelectedIndex(index)) {
            setSelectedIndex(index);
            fireActionListener();
          }
        } else {
          // multiple selection mode
          if (isSelectedIndex(index)) {
            removeSelectedIndex(index);
          } else {
            addSelectedIndex(index);
          }
          fireActionListener();
        }
        setColorDragEnabled(true, event.getPoint());
      }
    }
  }

  @Override
  public void mouseReleased(MouseEvent event)
  {
    if (event.getSource() == this && event.getButton() == MouseEvent.BUTTON1) {
      if (!isReadOnly()) {
        setColorDragEnabled(false, event.getPoint());
      }
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

//--------------------- Begin Interface MouseMotionListener ---------------------

  @Override
  public void mouseDragged(MouseEvent event)
  {
    if (event.getSource() == this) {
      if (!isReadOnly() && isColorDragEnabled()) {
        updateColorDrag(event.getPoint());
      }
    }
  }

  @Override
  public void mouseMoved(MouseEvent event)
  {
    // detecting color entry under mouse cursor
    if (event.getSource() == this) {
      int index = getColorIndexAt(event.getPoint());
      if (index != currentMouseOverIndex) {
        currentMouseOverIndex = index;
        fireMouseOverListener(currentMouseOverIndex);
      }
    }
  }

//--------------------- End Interface MouseMotionListener ---------------------


//-------------------------- INNER CLASSES --------------------------

  /** Defines an object which listens to MouseOverEvents. */
  public interface MouseOverListener extends EventListener
  {
    public void mouseOver(MouseOverEvent event);
  }


  /** MouseOverEvent is used to notify listeners that the mouse has been placed over a specific color entry. */
  public class MouseOverEvent extends EventObject
  {
    private int index;

    /**
     * Constructs a MouseOverEvent.
     * @param source The source of the event.
     * @param index The color index where the mouse cursor is currently located.
     *              Specify a negative value to indicate an invalid color entry.
     */
    public MouseOverEvent(Object source, int index)
    {
      super(source);
      if (index < 0) index = -1;
      this.index = index;
    }

    /** Returns the color index. Or -1 when there is no color entry is under the mouse cursor. */
    public int getColorIndex()
    {
      return index;
    }
  }
}
