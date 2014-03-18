// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.List;

/**
 * A decoder that takes individual images as input and simulates a BAM structure.
 * Furthermore, this class provides methods for manipulating the frame and cycle structure.
 * @author argent77
 */
public class PseudoBamDecoder extends BamDecoder
{
  private final List<PseudoBamFrameEntry> listFrames = new ArrayList<PseudoBamFrameEntry>();
  private final List<CycleEntry> listCycles = new ArrayList<CycleEntry>();
  private final PseudoBamFrameEntry defaultFrameInfo = new PseudoBamFrameEntry(null, 0, 0);

  private PseudoBamControl defaultControl;

  public PseudoBamDecoder(BufferedImage image)
  {
    this(new BufferedImage[]{image}, new Point[0]);
  }

  public PseudoBamDecoder(BufferedImage image, Point center)
  {
    this(new BufferedImage[]{image}, new Point[]{center});
  }

  public PseudoBamDecoder(BufferedImage[] images)
  {
    this(images, new Point[0]);
  }

  public PseudoBamDecoder(BufferedImage[] images, Point[] centers)
  {
    super(null);
    init(images, centers);
  }

  /**
   * Adds a new frame to the end of the frame list. Center position defaults to (0, 0).
   * @param image The image to add.
   */
  public void frameAdd(BufferedImage image)
  {
    frameInsert(listFrames.size(), new BufferedImage[]{image}, new Point[0]);
  }

  /**
   * Adds a new frame to the end of the frame list.
   * @param image The image to add.
   * @param center The center position of the image.
   */
  public void frameAdd(BufferedImage image, Point center)
  {
    frameInsert(listFrames.size(), new BufferedImage[]{image}, new Point[]{center});
  }

  /**
   * Adds the list of frames to the end of the frame list. Center positions default to (0, 0).
   * @param images An array containing the images to add.
   */
  public void frameAdd(BufferedImage[] images)
  {
    frameInsert(listFrames.size(), images, new Point[0]);
  }

  /**
   * Adds the list of frames to the end of the frame list.
   * @param images An array containing the images to add.
   * @param centers An array of center positions corresponding with the images.
   */
  public void frameAdd(BufferedImage[] images, Point[] centers)
  {
    frameInsert(listFrames.size(), images, centers);
  }

  /**
   * Inserts a frame at the specified position. Center position defaults to (0, 0).
   * @param frameIdx The position for the frame to insert.
   * @param image The image to insert.
   */
  public void frameInsert(int frameIdx, BufferedImage image)
  {
    frameInsert(frameIdx, new BufferedImage[]{image}, new Point[0]);
  }

  /**
   * Inserts a frame at the specified position.
   * @param frameIdx The position for the frame to insert.
   * @param image The image to insert.
   * @param center The center position of the image.
   */
  public void frameInsert(int frameIdx, BufferedImage image, Point center)
  {
    frameInsert(frameIdx, new BufferedImage[]{image}, new Point[]{center});
  }

  /**
   * Inserts an array of frames at the specified position. Center positions default to (0, 0).
   * @param frameIdx The position for the frames to insert.
   * @param images An array containing the images to insert.
   */
  public void frameInsert(int frameIdx, BufferedImage[] images)
  {
    frameInsert(frameIdx, images, new Point[0]);
  }

  /**
   * Inserts an array of frames at the specified position.
   * @param frameIdx The position for the frames to insert.
   * @param images An array containing the images to insert.
   * @param centers An array of center positions corresponding with the images.
   */
  public void frameInsert(int frameIdx, BufferedImage[] images, Point[] centers)
  {
    if (frameIdx >= 0 && frameIdx <= listFrames.size() && images != null) {
      for (int i = 0; i < images.length; i++) {
        int x = 0, y = 0;
        if (centers != null && centers.length > i && centers[i] != null) {
          x = centers[i].x;
          y = centers[i].y;
        }
        listFrames.add(frameIdx+i, new PseudoBamFrameEntry(images[i], x, y));
      }
    }
  }

  /**
   * Removes the frame at the specified position.
   * @param frameIdx The frame position.
   */
  public void frameRemove(int frameIdx)
  {
    frameRemove(frameIdx, 1);
  }

  /**
   * Removes a number of frames, start at the specified position.
   * @param frameIdx The frame position.
   * @param count The number of frames to remove.
   */
  public void frameRemove(int frameIdx, int count)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size() && count > 0) {
      if (frameIdx + count > listFrames.size()) {
        count = listFrames.size() - frameIdx;
      }
      for (int i = 0; i < count; i++) {
        listFrames.remove(frameIdx);
      }
    }
  }

  /**
   * Removes all frames from the BAM structure.
   */
  public void frameClear()
  {
    listFrames.clear();
  }


  @Override
  public PseudoBamControl createControl()
  {
    return new PseudoBamControl(this);
  }

  @Override
  public PseudoBamFrameEntry getFrameInfo(int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
      return listFrames.get(frameIdx);
    } else {
      return defaultFrameInfo;
    }
  }

  @Override
  public void close()
  {
    listFrames.clear();
  }

  @Override
  public boolean isOpen()
  {
    return !listFrames.isEmpty();
  }

  @Override
  public void reload()
  {
    // does nothing
  }

  @Override
  public byte[] getResourceData()
  {
    return new byte[0];
  }

  @Override
  public int frameCount()
  {
    return listFrames.size();
  }

  @Override
  public Image frameGet(BamControl control, int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
      if (control == null) {
        control = defaultControl;
      }
      int w, h;
      if (control.getMode() == BamDecoder.BamControl.Mode.Shared) {
        Dimension d = control.getSharedDimension();
        w = d.width;
        h = d.height;
      } else {
        w = getFrameInfo(frameIdx).getWidth();
        h = getFrameInfo(frameIdx).getHeight();
      }
      if (w > 0 && h > 0) {
        BufferedImage image = ColorConvert.createCompatibleImage(w, h, true);
        frameGet(control, frameIdx, image);
        return image;
      }
    }
    return ColorConvert.createCompatibleImage(1, 1, true);
  }

  @Override
  public void frameGet(BamControl control, int frameIdx, Image canvas)
  {
    if (canvas != null && frameIdx >= 0 && frameIdx < listFrames.size()) {
      if(control == null) {
        control = defaultControl;
      }
      int w, h;
      if (control.getMode() == BamDecoder.BamControl.Mode.Shared) {
        Dimension d = control.getSharedDimension();
        w = d.width;
        h = d.height;
      } else {
        w = getFrameInfo(frameIdx).getWidth();
        h = getFrameInfo(frameIdx).getHeight();
      }
      if (w > 0 && h > 0 && canvas.getWidth(null) >= w && canvas.getHeight(null) >= h) {
        renderFrame(control, frameIdx, canvas);
      }
    }
  }


  private void init(BufferedImage[] images, Point[] centers)
  {
    // resetting data
    close();

    if (images != null) {
      for (int i = 0; i < images.length; i++) {
        int x = 0, y = 0;
        if (centers != null && centers.length > i && centers[i] != null) {
          x = centers[i].x;
          y = centers[i].y;
        }
        listFrames.add(new PseudoBamFrameEntry(images[i], x, y));
      }

      // creating a default cycle
      int[] indices = new int[listFrames.size()];
      for (int i = 0; i < indices.length; i++) {
        indices[i] = i;
      }
      listCycles.add(new CycleEntry(indices));

      // creating default bam control instance as a fallback option
      defaultControl = new PseudoBamControl(this);
      defaultControl.setMode(BamControl.Mode.Shared);
      defaultControl.setSharedPerCycle(false);
    }
  }

  // Draws the absolute frame onto the canvas. Takes BAM mode into account.
  private void renderFrame(BamControl control, int frameIdx, Image canvas)
  {
    if (canvas != null && frameIdx >= 0 && frameIdx < listFrames.size()) {
      if (control == null) {
        control = defaultControl;
      }

      // decoding frame data
      BufferedImage srcImage = listFrames.get(frameIdx).frame;
      BufferedImage dstImage = ColorConvert.toBufferedImage(canvas, true, false);
      byte[] srcBufferB = null, dstBufferB = null;
      int[] srcBufferI = null, dstBufferI = null;
      if (srcImage.getType() == BufferedImage.TYPE_BYTE_INDEXED &&
          dstImage.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
        srcBufferB = ((DataBufferByte)srcImage.getRaster().getDataBuffer()).getData();
        dstBufferB = ((DataBufferByte)dstImage.getRaster().getDataBuffer()).getData();
      } else {
        srcBufferI = ((DataBufferInt)srcImage.getRaster().getDataBuffer()).getData();
        dstBufferI = ((DataBufferInt)dstImage.getRaster().getDataBuffer()).getData();
      }
      int dstWidth = dstImage.getWidth();
      int dstHeight = dstImage.getHeight();
      int srcWidth = listFrames.get(frameIdx).width;
      int srcHeight = listFrames.get(frameIdx).height;
      if (control.getMode() == BamControl.Mode.Shared) {
        // drawing on shared canvas
        int left = -control.getSharedRectangle().x;
        int top = -control.getSharedRectangle().y;
        int maxWidth = (dstWidth < srcWidth + left) ? dstWidth : srcWidth;
        int maxHeight = (dstHeight < srcHeight + top) ? dstHeight : srcHeight;
        int srcOfs = 0, dstOfs = top*dstWidth + left;
        for (int y = 0; y < maxHeight; y++) {
          for (int x = 0; x < maxWidth; x++) {
            if (dstBufferB != null) dstBufferB[dstOfs+x] = srcBufferB[srcOfs+x];
            if (dstBufferI != null) dstBufferI[dstOfs+x] = srcBufferI[srcOfs+x];
          }
          srcOfs += srcWidth;
          dstOfs += dstWidth;
        }
      } else {
        // drawing on individual canvas
        int srcOfs = 0, dstOfs = 0;
        int maxWidth = (dstWidth < srcWidth) ? dstWidth : srcWidth;
        int maxHeight = (dstHeight < srcHeight) ? dstHeight : srcHeight;
        for (int y = 0; y < maxHeight; y++) {
          for (int x = 0; x < maxWidth; x++) {
            if (dstBufferB != null) dstBufferB[dstOfs+x] = srcBufferB[srcOfs+x];
            if (dstBufferI != null) dstBufferI[dstOfs+x] = srcBufferI[srcOfs+x];
          }
          srcOfs += srcWidth;
          dstOfs += dstWidth;
        }
      }
      srcBufferB = null; dstBufferB = null;
      srcBufferI = null; dstBufferI = null;

      // rendering resulting image onto the canvas if needed
      if (dstImage != canvas) {
        Graphics g = canvas.getGraphics();
        try {
          g.drawImage(dstImage, 0, 0, null);
        } finally {
          g.dispose();
          g = null;
        }
        dstImage.flush();
        dstImage = null;
      }
    }
  }

  // Draws the absolute frame into the buffer. Takes BAM mode into account.
//  private void renderFrame(BamControl control, int frameIdx, int[] buffer, int width, int height)
//  {
//    if (control == null) {
//      control = defaultControl;
//    }
//
//    if (frameIdx >= 0 && frameIdx < listFrames.size() &&
//        buffer != null && buffer.length >= width*height) {
//      int srcWidth = listFrames.get(frameIdx).width;
//      int srcHeight = listFrames.get(frameIdx).height;
//      int[] srcData = ((DataBufferInt)listFrames.get(frameIdx).frame.getRaster().getDataBuffer()).getData();
//
//      if (control.getMode() == BamControl.Mode.Shared) {
//        // drawing on shared canvas
//        int left = -control.getSharedRectangle().x;
//        int top = -control.getSharedRectangle().y;
//        int maxWidth = (width < srcWidth + left) ? width : srcWidth;
//        int maxHeight = (height < srcHeight + top) ? height : srcHeight;
//        int srcOfs = 0, dstOfs = top*width + left;
//        for (int y = 0; y < maxHeight; y++) {
//          for (int x = 0; x < maxWidth; x++) {
//            buffer[dstOfs+x] = srcData[srcOfs+x];
//          }
//          srcOfs += srcWidth;
//          dstOfs += width;
//        }
//      } else {
//        // drawing on individual canvas
//        int srcOfs = 0, dstOfs = 0;
//        int maxWidth = (width < srcWidth) ? width : srcWidth;
//        int maxHeight = (height < srcHeight) ? height : srcHeight;
//        for (int y = 0; y < maxHeight; y++) {
//          for (int x = 0; x < maxWidth; x++) {
//            buffer[dstOfs+x] = srcData[srcOfs+x];
//          }
//          srcOfs += srcWidth;
//          dstOfs += width;
//        }
//      }
//    }
//  }

//-------------------------- INNER CLASSES --------------------------

  /** Provides information for a single frame entry */
  public class PseudoBamFrameEntry implements FrameEntry
  {
    private int width, height, centerX, centerY;
    private BufferedImage frame;

    private PseudoBamFrameEntry(BufferedImage image, int centerX, int centerY)
    {
      if (image != null) {
        frame = ColorConvert.toBufferedImage(image, true);
        width = frame.getWidth(null);
        height = frame.getHeight(null);
        this.centerX = centerX;
        this.centerY = centerY;
      } else {
        frame = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        width = height = 1;
      }
    }

    @Override
    public int getWidth() { return width; }
    @Override
    public int getHeight() { return height; }
    @Override
    public int getCenterX() { return centerX; }
    @Override
    public int getCenterY() { return centerY; }
  }

  /** Provides access to cycle-specific functionality. */
  public static class PseudoBamControl extends BamControl
  {
    private int currentCycle, currentFrame;

    protected PseudoBamControl(PseudoBamDecoder decoder)
    {
      super(decoder);
      init();
    }


    /** Adds a new empty cycle. */
    public void cycleAdd()
    {
      cycleInsert(getDecoder().listCycles.size(), null);
    }

    /** Adds a new cycle and initializes it with an array of frame indices. */
    public void cycleAdd(int[] indices)
    {
      cycleInsert(getDecoder().listCycles.size(), indices);
    }

    /** Inserts a new empty cycle at the specified position. */
    public void cycleInsert(int cycleIdx)
    {
      cycleInsert(cycleIdx, null);
    }

    /** Inserts a new cycle at the specified position and initializes it with an array of frame indices. */
    public void cycleInsert(int cycleIdx, int[] indices)
    {
      if (cycleIdx >= 0 && cycleIdx <= getDecoder().listCycles.size()) {
        CycleEntry ce = new CycleEntry(indices);
        getDecoder().listCycles.add(cycleIdx, ce);
        update();
      }
    }

    /** Removes a number of cycles at the specified position. */
    public void cycleRemove(int cycleIdx, int count)
    {
      if (cycleIdx >= 0 && cycleIdx < getDecoder().listCycles.size() && count > 0) {
        if (cycleIdx + count > getDecoder().listCycles.size()) {
          count = getDecoder().listCycles.size() - cycleIdx;
        }
        for (int i = 0; i < count; i++) {
          getDecoder().listCycles.remove(cycleIdx);
        }
        update();
      }
    }

    /** Removes all available cycles. */
    public void cycleClear()
    {
      getDecoder().listCycles.clear();
      update();
    }

    /** Adds frame indices to the specified cycle. */
    public void cycleAddFrames(int cycleIdx, int[] indices)
    {
      if (cycleIdx >= 0 && cycleIdx < getDecoder().listCycles.size()) {
        cycleInsertFrames(cycleIdx, getDecoder().listCycles.get(cycleIdx).size(), indices);
      }
    }

    /** Inserts frame indices to the cycle at the specified position. */
    public void cycleInsertFrames(int cycleIdx, int pos, int[] indices)
    {
      if (cycleIdx >= 0 && cycleIdx < getDecoder().listCycles.size()) {
        getDecoder().listCycles.get(cycleIdx).insert(pos, indices);
        update();
      }
    }

    /** Removes one frame index from the cycle at the specified position. */
    public void cycleRemoveFrames(int cycleIdx, int pos)
    {
      if (cycleIdx >= 0 && cycleIdx < getDecoder().listCycles.size()) {
        cycleRemoveFrames(cycleIdx, getDecoder().listCycles.get(cycleIdx).size(), 1);
      }
    }

    /** Removes frame indices from the cycle at the specified position. */
    public void cycleRemoveFrames(int cycleIdx, int pos, int count)
    {
      if (cycleIdx >= 0 && cycleIdx < getDecoder().listCycles.size()) {
        getDecoder().listCycles.get(cycleIdx).remove(pos, count);
        update();
      }
    }

    /** Removes all frame indices from the specified cycle. */
    public void cycleClearFrames(int cycleIdx)
    {
      if (cycleIdx >= 0 && cycleIdx < getDecoder().listCycles.size()) {
        getDecoder().listCycles.get(cycleIdx).clear();
        update();
      }
    }


    /** Returns whether the current frame in the current cycle includes a palette. */
    public boolean cycleFrameHasPalette()
    {
      return cycleFrameHasPalette(currentCycle, currentFrame);
    }

    /** Returns whether the specified frame in the current cycle includes a palette. */
    public boolean cycleFrameHasPalette(int frameIdx)
    {
      return cycleFrameHasPalette(currentCycle, frameIdx);
    }

    /** Returns whether the frame in the specified cycles includes a palette. */
    public boolean cycleFrameHasPalette(int cycleIdx, int frameIdx)
    {
      int index = cycleGetFrameIndexAbsolute(cycleIdx, frameIdx);
      if (index >= 0) {
        BufferedImage image = getDecoder().listFrames.get(index).frame;
        if (image != null && image.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
          return true;
        }
      }
      return false;
    }


    /** Returns the palette of the current frame in the current cycle. Returns <code>null</code> if no palette is available. */
    public int[] cycleFrameGetPalette()
    {
      return cycleFrameGetPalette(currentCycle, currentFrame);
    }

    /** Returns the palette of the specified frame in the current cycle. Returns <code>null</code> if no palette is available. */
    public int[] cycleFrameGetPalette(int frameIdx)
    {
      return cycleFrameGetPalette(currentCycle, frameIdx);
    }

    /** Returns the palette of the frame in the specified cycle. Returns <code>null</code> if no palette is available. */
    public int[] cycleFrameGetPalette(int cycleIdx, int frameIdx)
    {
      int index = cycleGetFrameIndexAbsolute(cycleIdx, frameIdx);
      if (index >= 0) {
        BufferedImage image = getDecoder().listFrames.get(index).frame;
        if (image != null && image.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
          if (image.getColorModel() instanceof IndexColorModel) {
            IndexColorModel cm = (IndexColorModel)image.getColorModel();
            int[] palette = new int[256];
            int size = 1 << cm.getPixelSize();
            if (size > 256) size = 256;
            for (int i = 0; i < size; i++) {
              palette[i] = (cm.getAlpha(i) << 24) | (cm.getRed(i) << 16) | (cm.getGreen(i) << 8) | cm.getBlue(i);
            }
            return palette;
          }
        }
      }
      return null;
    }


    @Override
    public PseudoBamDecoder getDecoder()
    {
      return (PseudoBamDecoder)super.getDecoder();
    }

    @Override
    public int cycleCount()
    {
      return getDecoder().listCycles.size();
    }

    @Override
    public int cycleFrameCount()
    {
      return cycleFrameCount(currentCycle);
    }

    @Override
    public int cycleFrameCount(int cycleIdx)
    {
      if (cycleIdx >= 0 && cycleIdx < getDecoder().listCycles.size()) {
        return getDecoder().listCycles.get(cycleIdx).size();
      }
      return 0;
    }

    @Override
    public int cycleGet()
    {
      return currentCycle;
    }

    @Override
    public boolean cycleSet(int cycleIdx)
    {
      if (cycleIdx >= 0 && cycleIdx < getDecoder().listCycles.size()) {
        currentCycle = cycleIdx;
        if (isSharedPerCycle()) {
          updateSharedBamSize();
        }
        return true;
      }
      return false;
    }

    @Override
    public boolean cycleHasNextFrame()
    {
      if (currentCycle >= 0 && currentCycle < getDecoder().listCycles.size()) {
        return (currentFrame >= 0 && currentFrame < getDecoder().listCycles.get(currentCycle).size() - 1);
      }
      return false;
    }

    @Override
    public boolean cycleNextFrame()
    {
      if (cycleHasNextFrame()) {
        currentFrame++;
        return true;
      } else {
        return false;
      }
    }

    @Override
    public void cycleReset()
    {
      currentFrame = 0;
    }

    @Override
    public Image cycleGetFrame()
    {
      int frameIdx = cycleGetFrameIndexAbsolute();
      return getDecoder().frameGet(this, frameIdx);
    }

    @Override
    public void cycleGetFrame(Image canvas)
    {
      int frameIdx = cycleGetFrameIndexAbsolute();
      getDecoder().frameGet(this, frameIdx, canvas);
    }

    @Override
    public Image cycleGetFrame(int frameIdx)
    {
      frameIdx = cycleGetFrameIndexAbsolute(frameIdx);
      return getDecoder().frameGet(this, frameIdx);
    }

    @Override
    public void cycleGetFrame(int frameIdx, Image canvas)
    {
      frameIdx = cycleGetFrameIndexAbsolute(frameIdx);
      getDecoder().frameGet(this, frameIdx, canvas);
    }

    @Override
    public int cycleGetFrameIndex()
    {
      return currentFrame;
    }

    @Override
    public boolean cycleSetFrameIndex(int frameIdx)
    {
      if (currentCycle >= 0 && currentCycle < getDecoder().listCycles.size() &&
          frameIdx >= 0 && frameIdx < getDecoder().listCycles.get(currentCycle).size()) {
        currentFrame = frameIdx;
        return true;
      } else {
        return false;
      }
    }

    @Override
    public int cycleGetFrameIndexAbsolute()
    {
      return cycleGetFrameIndexAbsolute(currentCycle, currentFrame);
    }

    @Override
    public int cycleGetFrameIndexAbsolute(int frameIdx)
    {
      return cycleGetFrameIndexAbsolute(currentCycle, frameIdx);
    }

    @Override
    public int cycleGetFrameIndexAbsolute(int cycleIdx, int frameIdx)
    {
      if (cycleIdx >= 0 && cycleIdx < getDecoder().listCycles.size() &&
          frameIdx >= 0 && frameIdx < getDecoder().listCycles.get(cycleIdx).size()) {
        return getDecoder().listCycles.get(cycleIdx).get(frameIdx);
      } else {
        return -1;
      }
    }


    private void init()
    {
      currentCycle = currentFrame = 0;
      update();
      updateSharedBamSize();
    }

    // Updates current cycle and frame pointers
    private void update()
    {
      if (getDecoder().listCycles.isEmpty()) {
        currentCycle = currentFrame = -1;
      } else {
        if (currentCycle < 0) {
          currentCycle = 0;
          if (getDecoder().listCycles.get(currentCycle).size() == 0) {
            currentFrame = -1;
          } else {
            currentFrame = 0;
          }
        } else if (currentCycle >= getDecoder().listCycles.size()) {
          currentCycle = getDecoder().listCycles.size() - 1;
          if (getDecoder().listCycles.get(currentCycle).size() == 0) {
            currentFrame = -1;
          } else {
            currentFrame = 0;
          }
        }
      }
    }
  }


  // Stores information for a single cycle
  private static class CycleEntry
  {
    private final List<Integer> frames;   // stores abs. frame indices that define this cycle

    public CycleEntry(int[] indices)
    {
      frames = new ArrayList<Integer>();
      add(indices);
    }

    // Returns the number of stored frame indices
    public int size()
    {
      return frames.size();
    }

    // Returns the frame index at specified position. Returns -1 on error.
    public int get(int pos)
    {
      if (pos >= 0 && pos < frames.size()) {
        return frames.get(pos).intValue();
      } else {
        return -1;
      }
    }

    // Removes all frame indices.
    public void clear()
    {
      frames.clear();
    }

    // Appends specified indices to list.
    public void add(int[] indices)
    {
      insert(frames.size(), indices);
    }

    // Inserts indices at specified position.
    public boolean insert(int pos, int[] indices)
    {
      if (indices != null && pos >= 0 && pos <= frames.size()) {
        for (int i = 0; i < indices.length; i++) {
          frames.add(pos + i, indices[i]);
        }
        return true;
      }
      return false;
    }

    // Removes count indices at specified position.
    public boolean remove(int pos, int count)
    {
      if (pos >= 0 && pos < frames.size()) {
        if (pos + count > frames.size()) {
          count = frames.size() - pos;
        }
        for (int i = 0; i < count; i++) {
          frames.remove(pos);
        }
        return count > 0;
      }
      return false;
    }
  }
}
