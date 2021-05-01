// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.Border;

/**
 * Implements a 2D grid of tiles (image blocks of fixed size) featuring resizable rows and columns
 * and several visual options.
 */
public class TileGrid extends JComponent
{
  private static final int DEFAULT_WIDTH    = 64;
  private static final int DEFAULT_HEIGHT   = 64;
  private static final int DEFAULT_ROWS     = 1;
  private static final int DEFAULT_COLUMNS  = 1;
  private static final Color DEFAULT_COLOR  = Color.LIGHT_GRAY;

  private JPanel root;
  private GridLayout gridLayout;
  private int rowCount, colCount;
  private int tileWidth, tileHeight;
  private Color bgColor, gridColor;
  private List<Image> imageList;
  private List<TileLabel> tileList;
  private Border tileBorder;
  private boolean bShowGrid, bShowIcons;

  /**
   * Creates an empty 1x1 tile grid.
   */
  public TileGrid()
  {
    super();

    init(DEFAULT_ROWS, DEFAULT_COLUMNS, DEFAULT_WIDTH, DEFAULT_HEIGHT);
  }

  /**
   * Creates an empty tile grid of the specified dimensions. Each tile has a default size of 64x64 pixels.
   * @param rows Number of rows
   * @param columns Number of columns
   */
  public TileGrid(int rows, int columns)
  {
    super();

    init(rows, columns, DEFAULT_WIDTH, DEFAULT_HEIGHT);
  }

  /**
   * Creates an empty tile grid of the specified dimensions.
   * @param rows Number of rows
   * @param columns Number of columns
   * @param tileWidth Width of each tile in pixels.
   * @param tileHeight Height of each tile in pixels.
   */
  public TileGrid(int rows, int columns, int tileWidth, int tileHeight)
  {
    super();

    init(rows, columns, tileWidth, tileHeight);
  }

  /**
   * Gets the number of tile columns.
   * @return Number of tile columns
   */
  public int getTileColumns()
  {
    return colCount;
  }

  /**
   * Sets the number of tile columns.
   * @param columns The new number of tiles for each row.
   */
  public void setColumns(int columns)
  {
    setGridSize(new Dimension(columns, rowCount));
  }

  /**
   * Gets the number of rows.
   * @return Number of rows
   */
  public int getTileRows()
  {
    return rowCount;
  }

  /**
   * Sets the number of tile rows.
   * @param rows The new number of rows
   */
  public void setRows(int rows)
  {
    setGridSize(new Dimension(colCount, rows));
  }

  /**
   * Sets both rows and columns.
   * @param rows The new number of rows
   * @param columns The new number of columns
   */
  public void setGridSize(int rows, int columns)
  {
    setGridSize(new Dimension(columns, rows));
  }

  /**
   * Sets both rows and columns.
   * @param dim Rows and columns specified as Dimension structure.
   */
  public void setGridSize(Dimension dim)
  {
    if (dim != null) {
      updateGridSize(dim.height, dim.width);
      validate();
    }
  }

  /**
   * Gets the width of each tile in pixels.
   * @return Width of each tile in pixels
   */
  public int getTileWidth()
  {
    return tileWidth;
  }

  /**
   * Sets the width of each tile in pixels.
   * @param width New width of each tile in pixels
   */
  public void setTileWidth(int width)
  {
    if (width > 0 && width != tileWidth) {
      tileWidth = width;
      updateSize();
      validate();
    }
  }

  /**
   * Gets the height of each tile in pixels.
   * @return Height of each tile in pixels
   */
  public int getTileHeight()
  {
    return tileHeight;
  }

  /**
   * Sets the height of each tile in pixels.
   * @param height The new height of each tile in pixels
   */
  public void setTileHeight(int height)
  {
    if (height > 0 && height != tileHeight) {
      tileHeight = height;
      updateSize();
      validate();
    }
  }

  /**
   * Returns the number of tiles displayed in the component (NOT necessarily equal to the number
   * of images assigned to this component!).
   * @return The number of tiles displayed within the grid of the component.
   */
  public int getTileCount()
  {
    return rowCount*colCount;
  }

  /**
   * Returns the background color for unused tiles (i.e. for tiles without graphics).
   * @return Color of unused tiles
   */
  public Color getBackgroundColor()
  {
    return bgColor;
  }

  /**
   * Sets the background color of unused tiles.
   * @param color The new background color for unused tiles
   */
  public void setTileColor(Color color)
  {
    updateColor(color);
    validate();
  }

  /**
   * Returns the image object located at the specified index in the internal image list.
   * @param index The index of the image object within the internal image list
   * @return An image object
   * @throws IndexOutOfBoundsException If no image found at the specified index
   */
  public Image getImage(int index) throws IndexOutOfBoundsException
  {
    if (index < 0 || index >= imageList.size())
        throw new IndexOutOfBoundsException("Index out of bounds: " + index);

    return imageList.get(index);
  }

  /**
   * Returns a copy of the internal image list.
   * @return A copy of the internal image list
   */
  public List<Image> getImageList()
  {
    return new ArrayList<>(imageList);
  }

  /**
   * Adds a new image to the internal image list.
   * @param image The image to add
   */
  public void addImage(Image image)
  {
    if (image == null)
      throw new NullPointerException();

    imageList.add(image);
    updateTileList(imageList.size() - 1, 1);
    validate();
  }

  /**
   * Adds an array of images to the internal image list.
   * @param images The array of images to add
   */
  public void addImage(Image[] images)
  {
    if (images == null)
      throw new NullPointerException();

    addImage(Arrays.asList(images));
  }

  /**
   * Adds a collection of images to the internal image list.
   * @param images The collection of images to add
   */
  public void addImage(List<Image> images)
  {
    if (images == null)
      throw new NullPointerException();

    if (images.size() > 0) {
      int startIndex = imageList.size();
      imageList.addAll(images);
      updateTileList(startIndex, images.size());
      validate();
    }
  }

  /**
   * Adds an image into the internal image list at a specified index position.
   * @param index The index position to add the image
   * @param image The image to insert
   */
  public void insertImage(int index, Image image)
  {
    if (image == null)
      throw new NullPointerException();
    if (index < 0 || index >= imageList.size())
      throw new IndexOutOfBoundsException("Index out of bounds: " + index);

    imageList.add(index, image);
    updateTileList(index, imageList.size() - index);
    validate();
  }

  /**
   * Adds an array of images into the internal image list, starting at the specified index position.
   * @param index The start index to add the images into
   * @param images The array of images to insert
   */
  public void insertImage(int index, Image[] images)
  {
    if (images == null)
      throw new NullPointerException();
    if (index < 0 || index >= imageList.size())
      throw new IndexOutOfBoundsException("Index out of bounds: " + index);

    insertImage(index, Arrays.asList(images));
  }

  /**
   * Adds a collection of images into the internal image list, starting at the specified index position.
   * @param index The start index to add the images into
   * @param images The collection of images to insert
   */
  public void insertImage(int index, List<Image> images)
  {
    if (images == null)
      throw new NullPointerException();
    if (index < 0 || index >= imageList.size())
      throw new IndexOutOfBoundsException("Index out of bounds: " + index);

    if (images.size() > 0) {
      imageList.addAll(index, images);
      updateTileList(index, imageList.size() - index);
      validate();
    }
  }

  /**
   * Replaces an existing image in the internal image list with a new one.
   * @param index The index position of the image to replace
   * @param image The replacement image
   */
  public void replaceImage(int index, Image image)
  {
    if (image == null)
      throw new NullPointerException();
    if (index < 0 || index >= imageList.size())
      throw new IndexOutOfBoundsException("Index out of bounds: " + index);

    imageList.set(index, image);
    updateTileList(index, 1);
    validate();
  }

  /**
   * Replaces an array of existing images with new ones.
   * @param index The start index of the array of images to replace
   * @param images The array of replacement images
   */
  public void replaceImage(int index, Image[] images)
  {
    if (images == null)
      throw new NullPointerException();
    if (index < 0 || index >= imageList.size())
      throw new IndexOutOfBoundsException("Index out of bounds: " + index);

    replaceImage(index, Arrays.asList(images));
  }

  /**
   * Replaces a collection of existing images with new ones.
   * @param index The start index of the collection of images to replace
   * @param images The collection of replacement images
   */
  public void replaceImage(int index, List<Image> images)
  {
    if (images == null)
      throw new NullPointerException();
    if (index < 0 || index >= imageList.size())
      throw new IndexOutOfBoundsException("Index out of bounds: " + index);

    if (images.size() > 0) {
      int count = 0;
      ListIterator<Image> iterSrc = images.listIterator(index);
      ListIterator<Image> iterDst = images.listIterator(index);

      // replacing elements
      while (iterSrc.hasNext() && iterDst.hasNext()) {
        Image image = iterSrc.next();
        iterDst.next();
        iterDst.set(image);
        count++;
      }

      // adding remaining elements (if any)
      if (!iterDst.hasNext()) {
        while (iterSrc.hasNext()) {
          Image image = iterSrc.next();
          iterDst.add(image);
          count++;
        }
      }

      updateTileList(index, count);
      validate();
    }
  }

  /**
   * Removes one image from the internal image list at the specified index position.
   * @param index The index of the image to remove
   */
  public void removeImage(int index)
  {
    if (index < 0 || index >= imageList.size())
      throw new IndexOutOfBoundsException("Index out of bounds: " + index);

    removeImage(index, 1);
  }

  /**
   * Removes a specific number of images from the internal image list.
   * @param index The start position of the images to remove
   * @param count The number of images to remove
   */
  public void removeImage(int index, int count)
  {
    if (index < 0 || index >= imageList.size())
      throw new IndexOutOfBoundsException("Index out of bounds: " + index);

    while (count > 0 && imageList.size() > index) {
      imageList.remove(index);
      count--;
    }
    updateTileList(index, 0);
    validate();
  }

  /**
   * Removes all images from the internal image list.
   */
  public void clearImages()
  {
    imageList.clear();
    updateTileList(0, 0);
    validate();
  }

  /**
   * Returns the index position of the specified image within the internal image list.
   * @param image The image to look for
   * @return The index position of the matching image or -1 otherwise
   */
  public int findImage(Image image)
  {
    if (image != null) {
      int idx = 0;
      ListIterator<Image> iter = imageList.listIterator();
      while (iter.hasNext()) {
        Image listImage = iter.next();
        if (listImage.equals(image))
          return idx;
        idx++;
      }
    }
    return -1;
  }

  /**
   * Returns the tile position of the specified image.
   * @param image The image to look for
   * @return A point structure, containing the row (height) and column (width) of the matching image
   *         or null otherwise.
   */
  public Point findImageLocation(Image image)
  {
    return indexToLocation(findImage(image));
  }

  // get/set image from/to specified tile location
  /**
   * Returns the image found at the specified tile position.
   * @param row The zero-based row of the tile
   * @param column The zero-based column of the tile
   * @return The image object or null if no image available at the specified location
   */
  public Image getImage(int row, int column)
  {
    int index = locationToIndex(new Point(row, column));
    if (index >= 0 && index < imageList.size())
      return imageList.get(index);
    else
      return null;
  }

  /**
   * Replace or set a new image at the specified grid location.
   * @param row The zero-based row of the tile
   * @param column The zero-based column of the tile
   * @param image The replacement image
   */
  public void setImage(int row, int column, Image image)
  {
    int index = locationToIndex(new Point(row, column));
    if (index >= 0 && index < imageList.size()) {
      replaceImage(index, image);
    } else
      throw new IndexOutOfBoundsException();
  }

  /**
   * Returns the number of images in the internal image list.
   * @return The number of images in the internal image list
   */
  public int getImageCount()
  {
    return imageList.size();
  }

  /**
   * Returns the currently active grid color.
   * @return The grid color as Color object.
   */
  public Color getGridColor()
  {
    return gridColor;
  }

  /**
   * Sets a new grid color.
   * @param gridColor The new grid color.
   */
  public void setGridColor(Color gridColor)
  {
    if (gridColor != this.gridColor) {
      this.gridColor = gridColor;
      updateTileGrid();
      validate();
    }
  }

  /**
   * Returns whether the grid is currently shown.
   * @return The visibility state of the grid
   */
  public boolean getShowGrid()
  {
    return bShowGrid;
  }

  /**
   * Sets the visibility state of the grid
   * @param showGrid True to show the grid, false to hide it.
   */
  public void setShowGrid(boolean showGrid)
  {
    if (showGrid != bShowGrid) {
      bShowGrid = showGrid;
      updateTileGrid();
      validate();
    }
  }

  /**
   * Returns the current border around each tile.
   * @return The border around each tile.
   */
  public Border getTileBorder()
  {
    return tileBorder;
  }

  /**
   * Sets a new border around each tile.
   * @param border The new border for each tile
   */
  public void setTileBorder(Border border)
  {
    if (border != this.tileBorder) {
      this.tileBorder = border;
      invalidate();
      updateTileBorders();
      validate();
    }
  }

  /**
   * Returns the visibility state of the tiles
   * @param showIcons
   * @return
   */
  public boolean getShowIcons(boolean showIcons)
  {
    return bShowIcons;
  }

  /**
   * Sets the visibility state of the tiles.
   * @param showIcons True to show the tiles, false to hide them
   */
  public void setShowIcons(boolean showIcons)
  {
    if (showIcons != bShowIcons) {
      bShowIcons = showIcons;
      updateTileIcons(bShowIcons);
      validate();
    }
  }


// -------------------------- PRIVATE METHODS --------------------------

  private void init(int rows, int cols, int tw, int th)
  {
    this.rowCount = 0;
    this.colCount = 0;
    this.tileWidth = Math.max(tw, 1);
    this.tileHeight = Math.max(th, 1);
    this.bgColor = null;
    this.imageList = new ArrayList<>(1);
    this.tileList = new ArrayList<>();
    this.tileBorder = null;
    this.bShowGrid = false;
    this.bShowIcons = true;

    setLayout(null);
    gridLayout = new GridLayout(1, 1, 0, 0);
    root = new JPanel(gridLayout);
    add(root);
    root.setLocation(0, 0);
    root.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

    updateColor(DEFAULT_COLOR);
    updateGridSize(Math.max(rows, DEFAULT_ROWS), Math.max(cols, DEFAULT_COLUMNS));
  }

  // Call whenever the grid size (rows or columns) changes
  private boolean updateGridSize(int newRows, int newCols)
  {
    if (newRows < 1 || newCols < 1)
      throw new IllegalArgumentException("Invalid grid size: " + newRows + "x" + newCols);

    if (newRows != rowCount || newCols != colCount) {
      if (newRows != rowCount)
        gridLayout.setRows(newRows);
      if (newCols != colCount)
        gridLayout.setColumns(newCols);
      rowCount = newRows;
      colCount = newCols;

      // synchronizing tileList with new number of tiles
      syncTileList(rowCount*colCount);
      updateTileLayout();
      updateSize();

      return true;
    } else
      return false;
  }

  // tileList must always contain as many elements as there are tiles on the grid
  private void syncTileList(int newSize)
  {
    if (newSize > 0) {
      // synchronizing tileList with new number of tiles
      if (newSize > tileList.size()  && imageList.size() > tileList.size()) {
        ListIterator<Image> iterImage = imageList.listIterator(tileList.size());
        while (iterImage.hasNext() && tileList.size() < newSize)
          tileList.add(createLabel(iterImage.next()));
      }
      while (newSize < tileList.size())
        tileList.remove(tileList.size() - 1);
      while (newSize > tileList.size())
        tileList.add(createLabel(null));
    }
  }

  // Calls whenever the size of the main component changes
  private void updateSize()
  {
    Dimension dim = new Dimension(colCount*tileWidth, rowCount*tileHeight);
    root.setMinimumSize(dim);
    root.setPreferredSize(dim);
    root.setMaximumSize(dim);
    root.setSize(dim);
    setMinimumSize(dim);
    setPreferredSize(dim);
    setMaximumSize(dim);
    setSize(dim);
  }

  // Call whenever (a portion of) the tile list has to be re-created (tileCount=0: all tiles, starting at startIndex)
  private void updateTileList(int startIndex, int tileCount)
  {
    startIndex = Math.min(Math.max(startIndex, 0), tileList.size());
    if (tileCount <= 0)
      tileCount = tileList.size() - startIndex;

    ListIterator<Image> iterImage = imageList.listIterator(startIndex);
    ListIterator<TileLabel> iterTile = tileList.listIterator(startIndex);
    // updating icons on labels
    while (tileCount > 0 && iterImage.hasNext() && iterTile.hasNext()) {
      iterTile.next().setImage(iterImage.next());
      tileCount--;
    }
    // removing icons from remaining labels
    while (tileCount > 0 && iterTile.hasNext()) {
      iterTile.next().setImage(null);
      tileCount--;
    }
  }

  // Calls whenever all tiles have to be re-added to the main component
  private void updateTileLayout()
  {
    // 1. remove all tiles from main component
    root.removeAll();

    // 2. re-add tiles, depending on ordering
    ListIterator<TileLabel> iter = tileList.listIterator();
    while (iter.hasNext())
      root.add(iter.next());
  }

  // Call whenever the frame around the tiles has to be drawn/undrawn
  private void updateTileGrid()
  {
    root.setEnabled(false);
    for (final TileLabel label: tileList) {
      label.setGridColor(gridColor);
      label.setShowGrid(bShowGrid);
    }
    root.setEnabled(true);
  }

  // Call whenever a border has to be added/removed
  private void updateTileBorders()
  {
    root.setEnabled(false);
    for (final TileLabel label: tileList) {
      label.setBorder(tileBorder);
      root.setEnabled(true);
    }
  }

  // Call whenever the visibility state of the icons changes
  private void updateTileIcons(boolean visible)
  {
    // for each tile: add/remove icon
    if (visible != bShowIcons) {
      bShowIcons = visible;
      if (bShowIcons) {
        ListIterator<Image> iterSrc = imageList.listIterator();
        ListIterator<TileLabel> iterDst = tileList.listIterator();
        while (iterDst.hasNext()) {
          Image image = iterSrc.hasNext() ? iterSrc.next() : null;
          TileLabel label = iterDst.next();
          label.setImage(image);
        }
      } else {
        ListIterator<TileLabel> iter = tileList.listIterator();
        while (iter.hasNext()) {
          TileLabel label = iter.next();
          label.setImage(null);
        }
      }
    }
  }

  // Call whenever the background color of the tiles and main component changes
  private void updateColor(Color color)
  {
    if (bgColor != color) {
      bgColor = color;
      root.setBackground(bgColor);    // needed???
      for (final TileLabel label: tileList)
        label.setBackground(bgColor);
    }
  }

  // Create a new label (use this in place of new TileLabel())
  private TileLabel createLabel(Image img)
  {
    TileLabel label = new TileLabel();
    label.setBackground(bgColor);
    label.setBorder(tileBorder);
    if (img != null)
      label.setImage(img);
    return label;
  }

  // Translate tile index to grid location
  private Point indexToLocation(int index)
  {
    if (index >= 0) {
      Point pt = new Point(index % colCount, index / colCount);
      return (pt.y < rowCount) ? pt : null;
    }
    return null;
  }

  // Translate grid location to tile index
  private int locationToIndex(Point location)
  {
    if (location != null) {
      if (location.x >= 0 && location.x < colCount &&
          location.y >= 0 && location.y < rowCount) {
        return location.y*colCount + location.x;
      }
    }
    return -1;
  }


//-------------------------- INNER CLASSES --------------------------

  // Adding grid support to RenderCanvas
  private class TileLabel extends RenderCanvas
  {
    private Color gridColor;
    private boolean showGrid;

    public TileLabel()
    {
      super();
      init();
    }

    public void setShowGrid(boolean bShow)
    {
      if (bShow != showGrid) {
        showGrid = bShow;
        repaint();
      }
    }

    public void setGridColor(Color newColor)
    {
      if (newColor != null && !gridColor.equals(newColor)) {
        gridColor = newColor;
        if (showGrid)
          repaint();
      }
    }

    @Override
    protected void paintComponent(Graphics g)
    {
      super.paintComponent(g);
      if (showGrid) {
        g.setColor(gridColor);
        g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
        g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
      }
    }

    private void init()
    {
      showGrid = false;
      gridColor = Color.GRAY;
    }
  }
}
