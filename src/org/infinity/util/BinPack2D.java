// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.infinity.util.tuples.Couple;

/**
 * Implementation of a two-dimensional rectangle bin packing algorithm.
 * (Port of Jukka Jylänki's C++ implementation of RectangleBinPack-&gt;MaxRectsBinPack.)
 * <br><br>
 * Can be used to pack multiple rectangles of arbitrary size into a "bin" of rectangular shape
 * with the goal to add as many rectangles as possible into the bin.
 */
public class BinPack2D
{
  /** Specifies the different heuristic rules that can be used when deciding where to place a new rectangle. */
  public enum HeuristicRules {
    /** BSSF: Positions the rectangle against the short side of a free rectangle into which it fits the best. */
    BEST_SHORT_SIDE_FIT,
    /** BLSF: Positions the rectangle against the long side of a free rectangle into which it fits the best. */
    BEST_LONG_SIDE_FIT,
    /** BAF: Positions the rectangle into the smallest free rect into which it fits. */
    BEST_AREA_FIT,
    /** BL: Does the Tetris placement. */
    BOTTOM_LEFT_RULE,
    /** CP: Chooses the placement where the rectangle touches other rects as much as possible. */
    CONTACT_POINT_RULE,
  }

  private final List<Rectangle> usedRectangles = new ArrayList<>();
  private final List<Rectangle> freeRectangles = new ArrayList<>();

  private int binWidth, binHeight;

  /**
   * Instantiates a bin of size (0,0). Call Init to create a new bin.
   */
  public BinPack2D()
  {
    binWidth = binHeight = 0;
  }

  /**
   * Instantiates a bin of the given size.
   * @param width Width of the bin.
   * @param height Height of the bin.
   */
  public BinPack2D(int width, int height)
  {
    init(width, height);
  }

  /**
   * (Re)initializes the packer to an empty bin of width x height units. Call whenever
   * you need to restart with a new bin.
   * @param width Width of the bin.
   * @param height Height of the bin.
   */
  public void init(int width, int height)
  {
    binWidth = width;
    binHeight = height;
    usedRectangles.clear();
    freeRectangles.clear();
    freeRectangles.add(new Rectangle(0, 0, width, height));
  }

  /** Returns the width of the current bin. */
  public int getBinWidth() { return binWidth; }

  /** Returns the height of the current bin. */
  public int getBinHeight() { return binHeight; }

  /**
   * Attempts to shrink the current bin as much as possible.
   * @param binary If {@code true}, the shrinking process will always try to reduce dimensions
   *               by 50% for each iteration.
   */
  public void shrinkBin(boolean binary)
  {
    if (usedRectangles.isEmpty()) {
      return;
    }

    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int maxY = Integer.MIN_VALUE;

    // finding borders
    for (int i = 0, size = usedRectangles.size(); i < size; i++) {
      Rectangle r = usedRectangles.get(i);
      minX = Math.min(minX, r.x);
      minY = Math.min(minY, r.y);
      maxX = Math.max(maxX, r.x + r.width);
      maxY = Math.max(maxY, r.y + r.height);
    }

    int newWidth = maxX - minX;
    int newHeight = maxY - minY;

    if (binary) {
      // attempt to shrink to the next lower power of two
      int curWidth = binWidth;
      int curHeight = binHeight;
      while (newWidth <= (curWidth >>> 1)) {
        curWidth >>>= 1;
      }
      newWidth = curWidth;
      while (newHeight <= (curHeight >>> 1)) {
        curHeight >>>= 1;
      }
      newHeight = curHeight;
    }

    // adjusting rectangle positions
    if ((newWidth != binWidth || newHeight != binHeight) && (minX > 0 || minY > 0)) {
      Iterator<Rectangle> iterFree = freeRectangles.iterator();
      Iterator<Rectangle> iterUsed = usedRectangles.iterator();
      while (iterFree.hasNext() && iterUsed.hasNext()) {
        if (iterFree.hasNext()) {
          Rectangle r = iterFree.next();
          r.x -= minX;
          r.y -= minY;
        }
        if (iterUsed.hasNext()) {
          Rectangle r = iterUsed.next();
          r.x -= minX;
          r.y -= minY;
        }
      }
    }

    binWidth = newWidth;
    binHeight = newHeight;
  }

  /**
   * Inserts the given list of rectangles in an offline/batch mode.
   * @param rects The list of rectangles to insert. This list will be destroyed in the process.
   * @param dst (out) This list will be filled with the packed rectangles. The indices will not
   *            correspond to that of rects.
   * @param rule The rectangle placement rule to use when packing.
   */
  public void insert(List<Dimension> rects, List<Rectangle> dst, HeuristicRules rule) throws NullPointerException
  {
    if (rects != null && dst != null) {
      dst.clear();

      while (rects.size() > 0) {
        int bestScore1 = Integer.MAX_VALUE;
        int bestScore2 = Integer.MAX_VALUE;
        int bestRectIndex = -1;
        Rectangle bestNode = null;

        Couple<Integer, Integer> score = Couple.with(0, 0);
        for (int i = 0, size = rects.size(); i < size; i++) {
          Dimension d = rects.get(i);
          Rectangle newNode = scoreRect(d.width, d.height, rule, score);
          if (score.getValue0() < bestScore1 ||
              (score.getValue0() == bestScore1 && score.getValue1() < bestScore2)) {
            bestScore1 = score.getValue0();
            bestScore2 = score.getValue1();
            bestNode = newNode;
            bestRectIndex = i;
          }
        }

        if (bestRectIndex == -1) {
          return;
        }

        placeRect(bestNode);
        rects.remove(bestRectIndex);
      }
    } else {
      throw new NullPointerException();
    }
  }

  /**
   * Inserts a single rectangle into the bin.
   * @param width Width of the rectangle to insert.
   * @param height Height of the rectangle to insert.
   * @param rule The rectangle placement rule to use when packing.
   * @return Returns the resulting packed rectangle. Returns empty rectangle if no fit found.
   */
  public Rectangle insert(int width, int height, HeuristicRules rule)
  {
    Rectangle newNode = null;
    switch (rule) {
      case BEST_SHORT_SIDE_FIT:
        newNode = findPositionForNewNodeBestShortSideFit(width, height, null);
        break;
      case BOTTOM_LEFT_RULE:
        newNode = findPositionForNewNodeBottomLeft(width, height, null);
        break;
      case CONTACT_POINT_RULE:
        newNode = findPositionForNewNodeContactPoint(width, height, null);
        break;
      case BEST_LONG_SIDE_FIT:
        newNode = findPositionForNewNodeBestLongSideFit(width, height, null);
        break;
      case BEST_AREA_FIT:
        newNode = findPositionForNewNodeBestAreaFit(width, height, null);
        break;
    }

    if (newNode.height == 0) {
      return newNode;
    }

    for (int i = 0, size = freeRectangles.size(); i < size; i++) {
      if (splitFreeNode(freeRectangles.get(i), newNode)) {
        freeRectangles.remove(i);
        i--;
        size--;
      }
    }

    pruneFreeList();

    usedRectangles.add(newNode);
    return newNode;
  }

  /**
   * Computes the ratio of used surface area to the total bin area.
   * @return The ratio of used surface area to the total bin area.
   */
  public float getOccupancy()
  {
    long usedSurfaceArea = 0L;
    for (int i = 0, size = usedRectangles.size(); i < size; i++) {
      Rectangle r = usedRectangles.get(i);
      usedSurfaceArea += r.width * r.height;
    }

    return (float)(usedSurfaceArea) / (float)(binWidth*binHeight);
  }

  /**
   * Computes the placement score for placing the given rectangle with the given method.
   * @param width Width of the rectangle.
   * @param height Height of the rectangle.
   * @param rule The selected rectangle placement rule.
   * @param score (out) Returns the primary and secondary placement score.
   * @return This struct identifies where the rectangle would be placed if it were placed.
   */
  private Rectangle scoreRect(int width, int height, HeuristicRules rule, Couple<Integer, Integer> score)
  {
    if (score == null) { score = Couple.with(0, 0); }

    Rectangle newNode = null;
    score.setValue0(Integer.MAX_VALUE);
    score.setValue1(Integer.MAX_VALUE);
    switch (rule) {
      case BEST_SHORT_SIDE_FIT:
        newNode = findPositionForNewNodeBestShortSideFit(width, height, score);
        break;
      case BOTTOM_LEFT_RULE:
        newNode = findPositionForNewNodeBottomLeft(width, height, score);
        break;
      case CONTACT_POINT_RULE:
        newNode = findPositionForNewNodeContactPoint(width, height, score);
        score.setValue0(-score.getValue0());
        break;
      case BEST_LONG_SIDE_FIT:
        newNode = findPositionForNewNodeBestLongSideFit(width, height, score);
        break;
      case BEST_AREA_FIT:
        newNode = findPositionForNewNodeBestAreaFit(width, height, score);
        break;
    }

    // cannot fit the current rectangle
    if (newNode.height == 0) {
      score.setValue0(Integer.MAX_VALUE);
      score.setValue1(Integer.MAX_VALUE);
    }

    return newNode;
  }

  /**
   * Places the given rectangle into the bin.
   * @param node The rectangle to place.
   */
  private void placeRect(Rectangle node)
  {
    for (int i = 0, size = freeRectangles.size(); i < size; i++) {
      if (splitFreeNode(freeRectangles.get(i), node)) {
        freeRectangles.remove(i);
        i--;
        size--;
      }
    }

    pruneFreeList();

    usedRectangles.add(node);
  }

  /**
   * Computes the placement score for the "CP" variant.
   */
  private int contactPointScoreNode(int x, int y, int width, int height)
  {
    int score = 0;

    if (x == 0 || x + width == binWidth) {
      score += height;
    }
    if (y == 0 || y + height == binHeight) {
      score += width;
    }

    for (int i = 0, size = usedRectangles.size(); i < size; i++) {
      Rectangle r = usedRectangles.get(i);
      if (r.x == x + width || r.x + r.width == x) {
        score += commonIntervalLength(r.y, r.y + r.height, y, y + height);
      }
      if (r.y == y + height || r.y + r.height == y) {
        score += commonIntervalLength(r.x, r.x + r.width, x, x + width);
      }
    }

    return score;
  }

  // bestPos.getValue0(): bestY, bestPos.getValue1(): bestX
  private Rectangle findPositionForNewNodeBottomLeft(int width, int height, Couple<Integer, Integer> bestPos)
  {
    if (bestPos == null) { bestPos = Couple.with(0, 0); }
    Rectangle bestNode = new Rectangle();

    bestPos.setValue0(Integer.MAX_VALUE);
    for (int i = 0, size = freeRectangles.size(); i < size; i++) {
      // Try to place the rectangle in upright (non-flipped) orientation.
      Rectangle r = freeRectangles.get(i);
      if (r.width >= width && r.height >= height) {
        int topSideY = r.y + height;
        if (topSideY < bestPos.getValue0() ||
            (topSideY == bestPos.getValue0() && r.x < bestPos.getValue1())) {
          bestNode.x = r.x;
          bestNode.y = r.y;
          bestNode.width = width;
          bestNode.height = height;
          bestPos.setValue1(r.x);
          bestPos.setValue0(topSideY);
        }
      }
    }

    return bestNode;
  }

  // bestFit.getValue0(): short side, bestFit.getValue1(): long side
  private Rectangle findPositionForNewNodeBestShortSideFit(int width, int height, Couple<Integer, Integer> bestFit)
  {
    if (bestFit == null) { bestFit = Couple.with(0, 0); }
    Rectangle bestNode = new Rectangle();

    bestFit.setValue0(Integer.MAX_VALUE);
    for (int i = 0, size = freeRectangles.size(); i < size; i++) {
      // Try to place the rectangle in upright (non-flipped) orientation.
      Rectangle r = freeRectangles.get(i);
      if (r.width >= width && r.height >= height) {
        int leftoverHoriz = Math.abs(r.width - width);
        int leftoverVert = Math.abs(r.height - height);
        int shortSideFit = Math.min(leftoverHoriz, leftoverVert);
        int longSideFit = Math.max(leftoverHoriz, leftoverVert);

        if (shortSideFit < bestFit.getValue0() ||
            (shortSideFit == bestFit.getValue0() && longSideFit < bestFit.getValue1())) {
          bestNode.x = r.x;
          bestNode.y = r.y;
          bestNode.width = width;
          bestNode.height = height;
          bestFit.setValue0(shortSideFit);
          bestFit.setValue1(longSideFit);
        }
      }
    }

    return bestNode;
  }

  // bestFit.getValue0(): short side, bestFit.getValue1(): long side
  private Rectangle findPositionForNewNodeBestLongSideFit(int width, int height, Couple<Integer, Integer> bestFit)
  {
    if (bestFit == null) { bestFit = Couple.with(0, 0); }
    Rectangle bestNode = new Rectangle();

    bestFit.setValue1(Integer.MAX_VALUE);
    for (int i = 0, size = freeRectangles.size(); i < size; i++) {
      // Try to place the rectangle in upright (non-flipped) orientation.
      Rectangle r = freeRectangles.get(i);
      if (r.width >= width && r.height >= height) {
        int leftoverHoriz = Math.abs(r.width - width);
        int leftoverVert = Math.abs(r.height - height);
        int shortSideFit = Math.min(leftoverHoriz, leftoverVert);
        int longSideFit = Math.max(leftoverHoriz, leftoverVert);

        if (longSideFit < bestFit.getValue1() ||
            (longSideFit == bestFit.getValue1() && shortSideFit < bestFit.getValue0())) {
          bestNode.x = r.x;
          bestNode.y = r.y;
          bestNode.width = width;
          bestNode.height = height;
          bestFit.setValue0(shortSideFit);
          bestFit.setValue1(longSideFit);
        }
      }
    }

    return bestNode;
  }

  // bestFit.getValue0(): area, bestFit.getValue1(): short side
  private Rectangle findPositionForNewNodeBestAreaFit(int width, int height, Couple<Integer, Integer> bestFit)
  {
    if (bestFit == null) { bestFit = Couple.with(0, 0); }
    Rectangle bestNode = new Rectangle();

    bestFit.setValue0(Integer.MAX_VALUE);
    for (int i = 0, size = freeRectangles.size(); i < size; i++) {
      Rectangle r = freeRectangles.get(i);
      int areaFit = r.width*r.height - width*height;

      // Try to place the rectangle in upright (non-flipped) orientation.
      if (r.width >= width && r.height >= height) {
        int leftoverHoriz = Math.abs(r.width - width);
        int leftoverVert = Math.abs(r.height - height);
        int shortSideFit = Math.min(leftoverHoriz, leftoverVert);

        if (areaFit < bestFit.getValue0() ||
            (areaFit == bestFit.getValue0() && shortSideFit < bestFit.getValue1())) {
          bestNode.x = r.x;
          bestNode.y = r.y;
          bestNode.width = width;
          bestNode.height = height;
          bestFit.setValue0(areaFit);
          bestFit.setValue1(shortSideFit);
        }
      }
    }

    return bestNode;
  }

  // bestScore.getValue0(): contact, bestScore.getValue1(): unused
  private Rectangle findPositionForNewNodeContactPoint(int width, int height, Couple<Integer, Integer> bestScore)
  {
    if (bestScore == null) { bestScore = Couple.with(0, 0); }
    Rectangle bestNode = new Rectangle();

    bestScore.setValue0(-1);
    for (int i = 0, size = freeRectangles.size(); i < size; i++) {
      // Try to place the rectangle in upright (non-flipped) orientation.
      Rectangle r = freeRectangles.get(i);
      if (r.width >= width && r.height >= height) {
        int score = contactPointScoreNode(r.x, r.y, width, height);
        if (score > bestScore.getValue0())
        {
          bestNode.x = r.x;
          bestNode.y = r.y;
          bestNode.width = width;
          bestNode.height = height;
          bestScore.setValue0(score);
        }
      }
    }

    return bestNode;
  }

  /** Returns {@code true} if the free node was split. */
  private boolean splitFreeNode(Rectangle freeNode, Rectangle usedNode)
  {
    // Test with SAT if the rectangles even intersect.
    if (usedNode.x >= freeNode.x + freeNode.width || usedNode.x + usedNode.width <= freeNode.x ||
        usedNode.y >= freeNode.y + freeNode.height || usedNode.y + usedNode.height <= freeNode.y) {
        return false;
    }

    if (usedNode.x < freeNode.x + freeNode.width && usedNode.x + usedNode.width > freeNode.x) {
      // New node at the top side of the used node.
      if (usedNode.y > freeNode.y && usedNode.y < freeNode.y + freeNode.height) {
        Rectangle newNode = (Rectangle)freeNode.clone();
        newNode.height = usedNode.y - newNode.y;
        freeRectangles.add(newNode);
      }

      // New node at the bottom side of the used node.
      if (usedNode.y + usedNode.height < freeNode.y + freeNode.height) {
        Rectangle newNode = (Rectangle)freeNode.clone();
        newNode.y = usedNode.y + usedNode.height;
        newNode.height = freeNode.y + freeNode.height - (usedNode.y + usedNode.height);
        freeRectangles.add(newNode);
      }
    }

    if (usedNode.y < freeNode.y + freeNode.height && usedNode.y + usedNode.height > freeNode.y) {
      // New node at the left side of the used node.
      if (usedNode.x > freeNode.x && usedNode.x < freeNode.x + freeNode.width) {
        Rectangle newNode = (Rectangle)freeNode.clone();
        newNode.width = usedNode.x - newNode.x;
        freeRectangles.add(newNode);
      }

      // New node at the right side of the used node.
      if (usedNode.x + usedNode.width < freeNode.x + freeNode.width) {
        Rectangle newNode = (Rectangle)freeNode.clone();
        newNode.x = usedNode.x + usedNode.width;
        newNode.width = freeNode.x + freeNode.width - (usedNode.x + usedNode.width);
        freeRectangles.add(newNode);
      }
    }

    return true;
  }

  /** Goes through the free rectangle list and removes any redundant entries. */
  private void pruneFreeList()
  {
    // Go through each pair and remove any rectangle that is redundant.
    for(int i = 0, size1 = freeRectangles.size(); i < size1; i++) {
      Rectangle r1 = freeRectangles.get(i);
      for(int j = i+1, size2 = freeRectangles.size(); j < size2; j++) {
        Rectangle r2 = freeRectangles.get(j);
        if (isContainedIn(r1, r2)) {
          freeRectangles.remove(i);
          i--;
          size1--;
          size2--;
          break;
        }
        if (isContainedIn(r2, r1)) {
          freeRectangles.remove(j);
          j--;
          size1--;
          size2--;
        }
      }
    }
  }

  // Returns 0 if the two intervals i1 and i2 are disjoint, or the length of their overlap otherwise.
  private int commonIntervalLength(int i1start, int i1end, int i2start, int i2end)
  {
    if (i1end < i2start || i2end < i1start) {
      return 0;
    }
    return Math.min(i1end, i2end) - Math.max(i1start, i2start);
  }

  // Returns true if a is contained in b.
  private boolean isContainedIn(Rectangle a, Rectangle b)
  {
    if (a != null && b != null) {
      return a.x >= b.x &&
             a.y >= b.y &&
             a.x+a.width <= b.x+b.width &&
             a.y+a.height <= b.y+b.height;
    }
    return false;
  }
}
