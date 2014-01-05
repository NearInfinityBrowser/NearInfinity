// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.util;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;


/**
 * Collection of methods to deal with rectangles in a 2D grid of arbitrary size.
 * @author argent77
 */
public class GridManager
{
  /**
   * Specifies the search order for free regions.
   */
  public static enum Alignment {
    TopLeftHorizontal,
    TopLeftVertical,
    TopRightHorizontal,
    TopRightVertical,
    BottomLeftHorizontal,
    BottomLeftVertical,
    BottomRightHorizontal,
    BottomRightVertical
  }

  private final List<Rectangle> rectList;   // list of blocked regions
  private final Rectangle grid;

  /**
   * Defines an empty grid of specified size.
   * @param width Width of the grid.
   * @param height Height of the grid.
   */
  public GridManager(int width, int height)
  {
    if (width <= 0 || height <= 0)
      throw new IndexOutOfBoundsException();

    rectList = new ArrayList<Rectangle>();
    grid = new Rectangle(0, 0, width, height);
  }

  /**
   * Returns the width of the grid.
   * @return Width of the grid.
   */
  public int getWidth()
  {
    return grid.width;
  }

  /**
   * Returns the height of the grid.
   * @return Height of the grid.
   */
  public int getHeight()
  {
    return grid.height;
  }

  /**
   * Resizes the grid. Removes blocked regions that fall outside of the new dimension.
   * @param newWidth The new width of the grid (has to be > 0).
   * @param newHeight The new height of the grid (has to be > 0).
   */
  public void resize(int newWidth, int newHeight)
  {
    if (newWidth <= 0 || newHeight <= 0)
      throw new IndexOutOfBoundsException();

    if (newWidth != grid.width || newHeight != grid.height) {
      grid.width = newWidth;
      grid.height = newHeight;

      // updating list of rectangles
      for (int i = rectList.size() - 1; i >= 0; i--) {
        Rectangle newRect = rectList.get(i).intersection(grid);
        if (!newRect.equals(rectList.get(i)))
          rectList.set(i, newRect);
      }
    }
  }

  /**
   * Attempts to reduce the dimensions of the grid to the smallest possible size without cutting
   * off blocked regions. (Minimum size: 1x1)
   */
  public void shrink()
  {
    int w = 1;
    int h = 1;
    for (int i = 0; i < rectList.size(); i++) {
      Rectangle r = rectList.get(i);
      w = Math.max(w, r.x + r.width);
      h = Math.max(h, r.y + r.height);
    }
    grid.width = w;
    grid.height = h;
  }

  /**
   * Checks whether or not the specified point lies within a blocked region.
   * @param pt The point to check.
   * @return <code>true</code> if the point lies within a blocked region.
   */
  public boolean isBlocked(Point pt)
  {
    if (pt != null) {
      for (int i = 0; i < rectList.size(); i++) {
        if (rectList.get(i).contains(pt))
          return true;
      }
    }
    return false;
  }

  /**
   * Blocks the specified region in the grid.
   * @param rect The region to block.
   * @return The resulting rectangle after cutting off all parts that lie outside the grid.
   */
  public Rectangle add(Rectangle rect)
  {
    if (rect != null) {
      Rectangle r = rect.intersection(grid);

      if (!r.isEmpty()) {
        // avoid duplicates
        for (int i = 0; i < rectList.size(); i++) {
          if (rectList.get(i).equals(r))
            return r;
        }
        rectList.add(r);
      }
      return r;
    }
    return new Rectangle();
  }

  /**
   * Removes all regions that block the specified location.
   * @param pt The point in the grid to unblock.
   */
  public void remove(Point pt)
  {
    if (pt != null && grid.contains(pt)) {
      for (int i = rectList.size() - 1; i >= 0; i--) {
        if (rectList.get(i).contains(pt))
          rectList.remove(i);
      }
    }
  }

  /**
   * Removes all regions that intersect with the specified rectangle.
   * @param rect The region in the grid to unblock.
   */
  public void remove(Rectangle rect)
  {
    if (rect != null && grid.intersects(rect)) {
      for (int i = rectList.size() - 1; i >= 0; i--) {
        if (rectList.get(i).intersects(rect))
          rectList.remove(i);
      }
    }
  }

  /**
   * Removes all blocked regions.
   */
  public void clear()
  {
    rectList.clear();
  }

  /**
   * Returns the location of the next available free space.
   * @param space The minimum dimension of the free space to locate.
   * @param align The search order for the next free region.
   * @return A rectangle that isn't blocked by any obstacle or <code>null</code> otherwise.
   */
  public Rectangle findNext(Dimension space, Alignment align)
  {
    if (space != null && space.width > 0 && space.height > 0) {
      // preparations
      Rectangle rect = new Rectangle(0, 0, space.width, space.height);
      int stepX, stepY;
      boolean isVertical;
      switch (align) {
        case TopLeftVertical:
          rect.x = 0; rect.y = 0;
          stepX = 1; stepY = 1;
          isVertical = true;
          break;
        case TopRightHorizontal:
          rect.x = grid.width - rect.width; rect.y = 0;
          stepX = -1; stepY = 1;
          isVertical = false;
          break;
        case TopRightVertical:
          rect.x = grid.width - rect.width; rect.y = 0;
          stepX = -1; stepY = 1;
          isVertical = true;
          break;
        case BottomLeftHorizontal:
          rect.x = 0; rect.y = grid.height - rect.height;
          stepX = 1; stepY = -1;
          isVertical = false;
          break;
        case BottomLeftVertical:
          rect.x = 0; rect.y = grid.height - rect.height;
          stepX = 1; stepY = -1;
          isVertical = true;
          break;
        case BottomRightHorizontal:
          rect.x = grid.width - rect.width; rect.y = grid.height - rect.height;
          stepX = -1; stepY = -1;
          isVertical = false;
          break;
        case BottomRightVertical:
          rect.x = grid.width - rect.width; rect.y = grid.height - rect.height;
          stepX = -1; stepY = -1;
          isVertical = true;
          break;
        default:
          rect.x = 0; rect.y = 0;
          stepX = 1; stepY = 1;
          isVertical = false;
      }

      // using a simple brute force algorithm to check for the next free region
      int maxIterations = (grid.width-rect.width+1)*(grid.height-rect.height+1);
      int iteration = 0;
      while (iteration < maxIterations) {
        // check for blocked region
        boolean isBlocked = false;
        for (int i = 0; i < rectList.size(); i++) {
          Rectangle r = rectList.get(i);
          // using inlined intersection check for better performance
          if ((r.x < rect.x+rect.width) && (r.x+r.width > rect.x) &&
              (r.y < rect.y+rect.height) && (r.y+r.height > rect.y)) {
            isBlocked = true;
            break;
          }
        }

        // free region found?
        if (!isBlocked) {
          return rect;
        }

        // advance one step (checking bounds)
        if (isVertical) {
          rect.y += stepY;
          if (rect.y < 0) {
            rect.y = grid.height - rect.height;
            rect.x += stepX;
            if (rect.x < 0) {
              rect.x = grid.width - rect.width;
            } else if (rect.x+rect.width > grid.width) {
              rect.x = 0;
            }
          } else if (rect.y+rect.height > grid.height) {
            rect.y = 0;
            rect.x += stepX;
            if (rect.x < 0) {
              rect.x = grid.width - rect.width;
            } else if (rect.x+rect.width > grid.width) {
              rect.x = 0;
            }
          }
        } else {
          rect.x += stepX;
          if (rect.x < 0) {
            rect.x = grid.width - rect.width;
            rect.y += stepY;
            if (rect.y < 0) {
              rect.y = grid.height - rect.height;
            } else if (rect.y+rect.height > grid.height) {
              rect.y = 0;
            }
          } else if (rect.x+rect.width > grid.width) {
            rect.x = 0;
            rect.y += stepY;
            if (rect.y < 0) {
              rect.y = grid.height - rect.height;
            } else if (rect.y+rect.height > grid.height) {
              rect.y = 0;
            }
          }
        }
        iteration++;
      }
    }
    return null;
  }
}
