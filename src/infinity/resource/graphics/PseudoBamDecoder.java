// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.List;

/**
 * A decoder that takes individual images as input and simulates a BAM structure.
 * All images are put into a single cycle.
 * @author argent77
 */
public class PseudoBamDecoder extends BamDecoder
{
  private final List<PseudoBamFrameEntry> listFrames = new ArrayList<PseudoBamFrameEntry>();

  private PseudoBamControl defaultControl;

  public PseudoBamDecoder(Image image)
  {
    this(new Image[]{image}, new Point[0]);
  }

  public PseudoBamDecoder(Image image, Point center)
  {
    this(new Image[]{image}, new Point[]{center});
  }

  public PseudoBamDecoder(Image[] images)
  {
    this(images, new Point[0]);
  }

  public PseudoBamDecoder(Image[] images, Point[] centers)
  {
    super(null);
    init(images, centers);
  }

  /**
   * Adds a new frame to the end of the frame list. Center position defaults to (0, 0).
   * @param image The image to add.
   */
  public void add(Image image)
  {
    add(new Image[]{image}, new Point[0]);
  }

  /**
   * Adds a new frame to the end of the frame list.
   * @param image The image to add.
   * @param center The center position of the image.
   */
  public void add(Image image, Point center)
  {
    add(new Image[]{image}, new Point[0]);
  }

  /**
   * Adds the list of frames to the end of the frame list. Center positions default to (0, 0).
   * @param images An array containing the images to add.
   */
  public void add(Image[] images)
  {
    insert(listFrames.size(), images);
  }

  /**
   * Adds the list of frames to the end of the frame list.
   * @param images An array containing the images to add.
   * @param centers An array of center positions corresponding with the images.
   */
  public void add(Image[] images, Point[] centers)
  {
    insert(listFrames.size(), images, centers);
  }

  /**
   * Inserts a frame at the specified position. Center position defaults to (0, 0).
   * @param frameIdx The position for the frame to insert.
   * @param image The image to insert.
   */
  public void insert(int frameIdx, Image image)
  {
    insert(frameIdx, new Image[]{image});
  }

  /**
   * Inserts a frame at the specified position.
   * @param frameIdx The position for the frame to insert.
   * @param image The image to insert.
   * @param center The center position of the image.
   */
  public void insert(int frameIdx, Image image, Point center)
  {
    insert(frameIdx, new Image[]{image});
  }

  /**
   * Inserts an array of frames at the specified position. Center positions default to (0, 0).
   * @param frameIdx The position for the frames to insert.
   * @param images An array containing the images to insert.
   */
  public void insert(int frameIdx, Image[] images)
  {
    insert(frameIdx, images, new Point[0]);
  }

  /**
   * Inserts an array of frames at the specified position.
   * @param frameIdx The position for the frames to insert.
   * @param images An array containing the images to insert.
   * @param centers An array of center positions corresponding with the images.
   */
  public void insert(int frameIdx, Image[] images, Point[] centers)
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
  public void remove(int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
      listFrames.remove(frameIdx);
    }
  }

  /**
   * Removes all frames from the BAM structure.
   */
  public void clear()
  {
    listFrames.clear();
//    currentFrame = -1;
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
      return null;
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

  @Override
  public int[] frameGetData(BamControl control, int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
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
      if (w > 0 && h > 0) {
        int[] buffer = new int[w*h];
        renderFrame(control, frameIdx, buffer, w, h);
        return buffer;
      }
    }
    return new int[0];
  }


  private void init(Image[] images, Point[] centers)
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

        // creating default bam control instance as a fallback option
        defaultControl = new PseudoBamControl(this);
        defaultControl.setMode(BamControl.Mode.Shared);
        defaultControl.setSharedPerCycle(false);
      }
    }
  }

  // Draws the absolute frame onto the canvas. Takes BAM mode into account.
  private void renderFrame(BamControl control, int frameIdx, Image canvas)
  {
    BufferedImage image = ColorConvert.toBufferedImage(canvas, true);
    int[] buffer = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
    renderFrame(control, frameIdx, buffer, image.getWidth(), image.getHeight());
    buffer = null;
    if (image != canvas) {
      Graphics g = canvas.getGraphics();
      g.drawImage(image, 0, 0, null);
      g.dispose();
      image.flush();
      image = null;
    }
  }

  // Draws the absolute frame into the buffer. Takes BAM mode into account.
  private void renderFrame(BamControl control, int frameIdx, int[] buffer, int width, int height)
  {
    if (control == null) {
      control = defaultControl;
    }

    if (frameIdx >= 0 && frameIdx < listFrames.size() &&
        buffer != null && buffer.length >= width*height) {
      int srcWidth = listFrames.get(frameIdx).width;
      int srcHeight = listFrames.get(frameIdx).height;
      int[] srcData = ((DataBufferInt)listFrames.get(frameIdx).frame.getRaster().getDataBuffer()).getData();

      if (control.getMode() == BamControl.Mode.Shared) {
        // drawing on shared canvas
        int left = -control.getSharedRectangle().x;
        int top = -control.getSharedRectangle().y;
        int maxWidth = (width < srcWidth + left) ? width : srcWidth;
        int maxHeight = (height < srcHeight + top) ? height : srcHeight;
        int srcOfs = 0, dstOfs = top*width + left;
        for (int y = 0; y < maxHeight; y++) {
          for (int x = 0; x < maxWidth; x++) {
            buffer[dstOfs+x] = srcData[srcOfs+x];
          }
          srcOfs += srcWidth;
          dstOfs += width;
        }
      } else {
        // drawing on individual canvas
        int srcOfs = 0, dstOfs = 0;
        int maxWidth = (width < srcWidth) ? width : srcWidth;
        int maxHeight = (height < srcHeight) ? height : srcHeight;
        for (int y = 0; y < maxHeight; y++) {
          for (int x = 0; x < maxWidth; x++) {
            buffer[dstOfs+x] = srcData[srcOfs+x];
          }
          srcOfs += srcWidth;
          dstOfs += width;
        }
      }
    }
  }

//-------------------------- INNER CLASSES --------------------------

  public class PseudoBamFrameEntry implements FrameEntry
  {
    private int width, height, centerX, centerY;
    private BufferedImage frame;

    private PseudoBamFrameEntry(Image image, int centerX, int centerY)
    {
      if (image != null) {
        frame = ColorConvert.toBufferedImage(image, true);
        width = frame.getWidth(null);
        height = frame.getHeight(null);
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
    private int currentFrame;

    protected PseudoBamControl(PseudoBamDecoder decoder)
    {
      super(decoder);
      init();
    }

    @Override
    public PseudoBamDecoder getDecoder()
    {
      return (PseudoBamDecoder)super.getDecoder();
    }

    @Override
    public int cycleCount()
    {
      return 1;
    }

    @Override
    public int cycleFrameCount()
    {
      return getDecoder().listFrames.size();
    }

    @Override
    public int cycleFrameCount(int cycleIdx)
    {
      if (cycleIdx == 0) {
        return getDecoder().listFrames.size();
      }
      return 0;
    }

    @Override
    public int cycleGet()
    {
      return 1;
    }

    @Override
    public boolean cycleSet(int cycleIdx)
    {
      return (cycleIdx == 0);
    }

    @Override
    public boolean cycleHasNextFrame()
    {
      return (currentFrame < getDecoder().listFrames.size() - 1);
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
    public int[] cycleGetFrameData()
    {
      int frameIdx = cycleGetFrameIndexAbsolute();
      return getDecoder().frameGetData(this, frameIdx);
    }

    @Override
    public int[] cycleGetFrameData(int frameIdx)
    {
      frameIdx = cycleGetFrameIndexAbsolute(frameIdx);
      return getDecoder().frameGetData(this, frameIdx);
    }

    @Override
    public int cycleGetFrameIndex()
    {
      return currentFrame;
    }

    @Override
    public boolean cycleSetFrameIndex(int frameIdx)
    {
      if (frameIdx >= 0 && frameIdx < getDecoder().listFrames.size()) {
        currentFrame = frameIdx;
        return true;
      } else {
        return false;
      }
    }

    @Override
    public int cycleGetFrameIndexAbsolute()
    {
      return currentFrame;
    }

    @Override
    public int cycleGetFrameIndexAbsolute(int frameIdx)
    {
      if (frameIdx >= 0 && frameIdx < getDecoder().listFrames.size()) {
        return frameIdx;
      }
      return -1;
    }

    @Override
    public int cycleGetFrameIndexAbsolute(int cycleIdx, int frameIdx)
    {
      if (cycleIdx == 0 && frameIdx >= 0 && frameIdx < getDecoder().listFrames.size()) {
        return frameIdx;
      }
      return -1;
    }


    private void init()
    {
      currentFrame = 0;
      updateSharedBamSize();
    }
  }
}
