// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.util;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Manages rectangular blocks of the same binary state in a 2D grid of arbitrary size.
 * @author argent77
 */
public class GridManager
{
  /**
   * Specifies how to sort a list of rectangles (if at all).
   */
  public static enum SortOrder { Unsorted, ByWidth, ByHeight }

  private static enum Alignment { Horizontal, Vertical, Both }

  private static final Comparator<Rectangle> CompareByWidth = new Comparator<Rectangle>() {
    @Override
    public int compare(Rectangle rc1, Rectangle rc2)
    {
      return rc1.width - rc2.width;
    }
  };

  private static final Comparator<Rectangle> CompareByHeight = new Comparator<Rectangle>() {
    @Override
    public int compare(Rectangle rc1, Rectangle rc2)
    {
      return rc1.height - rc2.height;
    }
  };

  private final List<Rectangle> rectHList, rectVList, rectList;
  private BitSet grid;
  private int rows, cols;
  private boolean lastState, dirty;


  /**
   * Creates a 2D grid and sets each tile to the state <code>false</code>.
   * @param rows A positive number that defines the number of rows in the grid.
   * @param columns A positive number that defines the number of columns in the grid.
   */
  public GridManager(int rows, int columns)
  {
    this(rows, columns, false);
  }

  /**
   * Creates a 2D grid and initializes each tile to a given state.
   * @param rows A positive number that defines the number of rows in the grid.
   * @param columns A positive number that defines the number of columns in the grid.
   * @param state The initial state of each tile in the grid.
   */
  public GridManager(int rows, int columns, boolean state)
  {
    if (rows <= 0 || columns <= 0)
      throw new IndexOutOfBoundsException();

    this.rows = rows;
    this.cols = columns;
    grid = new BitSet(this.rows*this.cols);
    grid.set(0, grid.size(), state);
    rectHList = new ArrayList<Rectangle>();
    rectVList = new ArrayList<Rectangle>();
    rectList = new ArrayList<Rectangle>();
    setModified();
  }

  /**
   * Returns the number of rows available in the grid.
   * @return Number of rows in the grid.
   */
  public int getRows()
  {
    return rows;
  }

  /**
   * Returns the number of columns in the grid.
   * @return Number of columns in the grid.
   */
  public int getCols()
  {
    return cols;
  }

  /**
   * Resizes the grid to the specified new rows and columns and sets the state of each tile
   * to <code>false</code>.
   * @param rows A positive number that defines the number of rows in the grid.
   * @param columns A positive number that defines the number of columns in the grid.
   */
  public void resize(int rows, int columns)
  {
    resize(rows, columns, false);
  }

  /**
   * Resizes the grid to the specified new rows and columns.
   * @param rows A positive number that defines the number of rows in the grid.
   * @param columns A positive number that defines the number of columns in the grid.
   * @param state The initial state of each tile in the grid.
   */
  public void resize(int rows, int columns, boolean state)
  {
    if (rows <= 0 || columns <= 0)
      throw new IndexOutOfBoundsException();

    this.rows = rows;
    this.cols = columns;
    setModified();
    grid.set(0, this.rows*this.cols, state);
  }

  /**
   * Returns the state of the specified tile.
   * @param x Column of the tile.
   * @param y Row of the tile.
   * @return Current state of the tile.
   */
  public boolean get(int x, int y)
  {
    if (x < 0 || x >= cols || y < 0 || y >= rows)
      throw new IndexOutOfBoundsException();

    return grid.get(y*cols+x);
  }

  /**
   * Sets the specified tile to a specific state.
   * @param x Column of the tile.
   * @param y Row of the tile.
   * @param state The state to set.
   */
  public void set(int x, int y, boolean state)
  {
    if (x < 0 || x >= cols || y < 0 || y >= rows)
      throw new IndexOutOfBoundsException();

    setModified();
    grid.set(y*cols+x, state);
  }

  /**
   * Sets all tiles in the grid to a specific state.
   * @param state The state to set.
   */
  public void setAll(boolean state)
  {
    setModified();
    grid.set(0, grid.size(), state);
  }

  /**
   * Sets the specified region of tiles to a specific state.
   * @param rect The region of tiles to set.
   * @param state The state to set.
   */
  public void setRectangle(Rectangle rect, boolean state)
  {
    // ensure rectangle to be valid
    rect = getValidRectangle(rect);
    if (rect == null || rect.isEmpty())
      return;

    setModified();
    int ofs = rect.y*cols + rect.x;
    for (int i = rect.y; i < rect.y + rect.height; i++, ofs+=cols) {
      grid.set(ofs, ofs+rect.width, state);
    }
  }

  /**
   * Flips the state of the specified tile to the complement of its current value.
   * @param x Column of the tile.
   * @param y Row of the tile.
   * @return Returns the new state of the modified tile.
   */
  public boolean flip(int x, int y)
  {
    if (x < 0 || x >= cols || y < 0 || y >= rows)
      throw new IndexOutOfBoundsException();

    int bit = y*cols+x;
    grid.flip(bit);

    setModified();
    return grid.get(bit);
  }

  /**
   * Flips the states of all tiles to the complement of their current values.
   */
  public void flipAll()
  {
    setModified();
    grid.flip(0, grid.size());
  }

  /**
   * Flips the state of the tiles in the specified region.
   * @param rect The region of tiles to flip.
   */
  public void flipRectangle(Rectangle rect)
  {
    // ensure rectangle to be valid
    rect = getValidRectangle(rect);
    if (rect == null || rect.isEmpty())
      return;

    setModified();
    if (rect.width > 0 && rect.height > 0) {
      int ofs = rect.y*cols + rect.x;
      for (int i = rect.y; i < rect.y + rect.height; i++, ofs+=cols) {
        grid.flip(ofs, ofs+rect.width);
      }
    }
  }

  /**
   * Returns a list of available rectangle of the specified state.
   * @param state The state of the rectangle.
   * @param order How to sort the list of rectangles.
   * @return A list of rectangles satisfying the specified parameters.
   */
  public List<Rectangle> getRegions(boolean state, SortOrder order)
  {
    if (order == null)
      order = SortOrder.Unsorted;

    // update list of regions if needed
    if (isModified(state)) {
      updateListOfRectangles(state);
    }

    // returned (sorted) list of regions
    List<Rectangle> retList = new ArrayList<Rectangle>(rectList.size());
    retList.addAll(rectList);
    if (order == SortOrder.ByWidth) {
      Collections.sort(retList, CompareByWidth);
    } else if (order == SortOrder.ByHeight) {
      Collections.sort(retList, CompareByHeight);
    }
    return retList;
  }

  /**
   * Returns a list of available rectangle of the specified state that are equal or greater than
   * the specified space.
   * @param space The minimum dimensions of available rectangles.
   * @param state The state of the rectangles.
   * @param order How to sort the list of rectangles.
   * @return A list of rectangles satisfying the specified parameters.
   */
  public List<Rectangle> getMatchingRegions(Dimension space, boolean state, SortOrder order)
  {
    if (space == null || space.width <= 0 || space.height <= 0 ||
        space.width*space.height > getTileCount(state)) {
      return new ArrayList<Rectangle>();
    }

    // create list of matching rectangles
    List<Rectangle> retList = getRegions(state, order);
    int idx = 0;
    while (idx < retList.size()) {
      Rectangle rect = rectList.get(idx);
      if (rect.width < space.width || rect.height < space.height) {
        retList.remove(idx);
      } else {
        idx++;
      }
    }

    return retList;
  }

  /**
   * Returns total number of tiles in the grid.
   * @return Total number of tiles in the grid.
   */
  public int getTileCount()
  {
    return cols*rows;
  }

  /**
   * Returns the number of tiles of specified state in the grid.
   * @param state The state of the tiles to count.
   * @return Number of tiles of specified state in the grid.
   */
  public int getTileCount(boolean state)
  {
    int count = 0;
    for (int i = 0; i < grid.size(); i++) {
      if (grid.get(i) == state) {
        count++;
      }
    }
    return count;
  }

  /**
   * Attempts to reduce the dimensions of the grid to the smallest possible size without cutting
   * off marked regions. (Minimum size: 1x1)
   */
  public void shrink()
  {
    int maxX = 0, maxY = 0;
    for (int y = 0; y < rows; y++) {
      for (int x = 0; x < cols; x++) {
        int bit = y*cols + x;
        if (grid.get(bit)) {
          if (x > maxX) maxX = x;
          if (y > maxY) maxY = y;
        }
      }
    }
    cols = maxX+1;
    rows = maxY+1;
  }

  /**
   * Expands the grid dimensions by the specified values. No data will be lost. The state of the
   * new area will be initially set to <code>false</code>.
   * @param addToCols The amount of horizontal space to add.
   * @param addToRows The amount of vertical space to add.
   */
  public void expand(int addToCols, int addToRows)
  {
    expand(addToCols, addToRows, false);
  }

  /**
   * Expands the grid dimensions by the specified values. No data will be lost.
   * @param addToCols The amount of horizontal space to add.
   * @param addToRows The amount of vertical space to add.
   * @param state The initial state of the added area.
   */
  public void expand(int addToCols, int addToRows, boolean state)
  {
    if (addToCols <= 0)
      addToCols = 0;
    if (addToRows <= 0)
      addToRows = 0;

    if (addToCols > 0 || addToRows > 0) {
      int newCols = cols+addToCols;
      int newRows = rows+addToRows;
      BitSet bs = new BitSet(newCols*newRows);
      bs.set(0, bs.size(), state);
      for (int y = 0; y < rows; y++) {
        int src = y*cols;
        int dst = y*newCols;
        for (int x = 0; x < cols; x++) {
          bs.set(dst+x, grid.get(src+x));
        }
      }
      grid = bs;
      cols = newCols;
      rows = newRows;
    }
  }

  private boolean isModified(boolean state)
  {
    return dirty || (lastState != state);
  }

  private void setModified()
  {
    dirty = true;
  }

  private void clearModified()
  {
    dirty = false;
  }

  // Generates a list of all possible rectangles of the specified state.
  private void updateListOfRectangles(boolean state)
  {
    if (isModified(state)) {
      rectHList.clear();
      rectVList.clear();
      Rectangle rect = new Rectangle();

      // scan horizontally
      for (int y = 0; y < rows; y++) {
        for (int x = 0; x < cols; x++) {
          if (get(x, y) == state) {
            int x0 = x, y0 = y, y1 = y;
            int x1 = lastOf(state, x, y, 0, Alignment.Horizontal);
            rect.x = x0; rect.y = y0;
            rect.width = x1 - x0 + 1; rect.height = y1 - y0 + 1;
            if (!matchRectangle(rect, Alignment.Horizontal)) {
              // scan lines above
              while (y0 > 0) {
                if (get(x0, y0 - 1) == state) {
                  int tmp = lastOf(state, x0, y0-1, x1-x0+1, Alignment.Horizontal);
                  if (tmp >= x1) {
                    y0--;
                  } else {
                    break;
                  }
                } else {
                  break;
                }
              }
              // scan lines below
              while (y1 < rows - 1) {
                if (get(x0, y1 + 1) == state) {
                  int tmp = lastOf(state, x0, y1+1, x1-x0+1, Alignment.Horizontal);
                  if (tmp >= x1) {
                    y1++;
                  }
                } else {
                  break;
                }
              }
              // add rectangle to the horizontal list
              addRectangle(new Rectangle(x0, y0, x1-x0+1, y1-y0+1), Alignment.Horizontal);
            }
            x = x1;   // skip tiles of the same state
          }
        }
      }

      // scan vertically
      for (int x = 0; x < cols; x++) {
        for (int y = 0; y < rows; y++) {
          if (get(x, y) == state) {
            int y0 = y, x0 = x, x1 = x;
            int y1 = lastOf(state, x, y, 0, Alignment.Vertical);
            rect.x = x0; rect.y = y0;
            rect.width = x1 - x0 + 1; rect.height = y1 - y0 + 1;
            if (!matchRectangle(rect, Alignment.Vertical)) {
              // scan lines left
              while (x0 > 0) {
                if (get(x0 - 1, y0) == state) {
                  int tmp = lastOf(state, x0-1, y0, y1-y0+1, Alignment.Vertical);
                  if (tmp >= y1) {
                    x0--;
                  } else {
                    break;
                  }
                } else {
                  break;
                }
              }
              // scan lines right
              while (x1 < cols - 1) {
                if (get(x1 + 1, y0) == state) {
                  int tmp = lastOf(state, x1+1, y0, y1-y0+1, Alignment.Vertical);
                  if (tmp >= y1) {
                    x1++;
                  } else {
                    break;
                  }
                } else {
                  break;
                }
              }
              // add rectangle to the vertical list
              addRectangle(new Rectangle(x0, y0, x1-x0+1, y1-y0+1), Alignment.Vertical);
            }
            y = y1;   // skip tiles of the same state
          }
        }
      }

      // merging horizontal and vertical rectangle list, eliminating duplicates
      mergeLists();

      lastState = state;
      clearModified();
    }
  }

  // Returns the x or y position of the last tile of specified state within the specified distance
  private int lastOf(boolean state, int x, int y, int length, Alignment align)
  {
    int cur = 0;
    int ofs = y*cols + x;
    if (align == Alignment.Horizontal) {
      cur = x;
      if (length <= 0 || x + length > cols)
        length = cols - x;
      for (int i = x; i < x + length; i++, ofs++) {
        if (grid.get(ofs) == state) {
          cur = i;
        } else {
          break;
        }
      }
    } else if (align == Alignment.Vertical) {
      cur = y;
      if (length <= 0 || y + length > rows)
        length = rows - y;
      for (int i = y; i < y + length; i++, ofs += cols) {
        if (grid.get(ofs) == state) {
          cur = i;
        } else {
          break;
        }
      }
    }
    return cur;
  }

  // Returns true if either width or height of the rectangle fits perfectly into one of the already
  // listed rectangles in the specified list
  private boolean matchRectangle(Rectangle rect, Alignment align)
  {
    if (rect != null) {
      if (align == Alignment.Horizontal) {
        for (final Rectangle orig: rectHList) {
          if (rect.y >= orig.y && rect.y < orig.y + orig.height &&
              rect.x == orig.x && rect.width == orig.width) {
            return true;
          }
        }
      } else if (align == Alignment.Vertical) {
        for (final Rectangle orig: rectVList) {
          if (rect.x >= orig.x && rect.x < orig.x + orig.width &&
              rect.y == orig.y && rect.height == orig.height) {
            return true;
          }
        }
      }
    }
    return false;
  }

  // adds the specified rectangle to the respective list
  private void addRectangle(Rectangle rect, Alignment align)
  {
    if (rect != null) {
      if (align == Alignment.Horizontal) {
        rectHList.add(rect);
      } else if (align == Alignment.Vertical) {
        rectVList.add(rect);
      }
    }
  }

  // merging both lists, skipping duplicates
  private void mergeLists()
  {
    rectList.clear();
    rectList.addAll(rectHList);
    for (int i = 0; i < rectVList.size(); i++) {
      boolean exists = false;
      for (int j = 0; j < rectHList.size(); j++) {
        if (rectVList.get(i).equals(rectHList.get(j))) {
          exists = true;
          break;
        }
      }
      if (!exists) {
        rectList.add(rectVList.get(i));
      }
    }
  }

  private Rectangle getValidRectangle(Rectangle rect)
  {
    if (rect == null)
      return null;

    if (rect.x < 0) {
      rect.width -= rect.x;
      rect.x = 0;
    }
    if (rect.y < 0) {
      rect.height -= rect.y;
      rect.y = 0;
    }
    if (rect.width > 0 && rect.x + rect.width > cols) {
      rect.width = cols - rect.x;
    }
    if (rect.height > 0 && rect.y + rect.height > rows) {
      rect.height = rows - rect.y;
    }
    if (rect.width <= 0 || rect.height <= 0) {
      rect.x = rect.y = rect.width = rect.height = 0;
    }

    return rect;
  }
}
