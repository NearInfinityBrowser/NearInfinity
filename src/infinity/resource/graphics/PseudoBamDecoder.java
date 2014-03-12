// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A decoder that takes individual images as input and simulates a BAM structure.
 * All images are put into a single cycle.
 * @author argent77
 */
public class PseudoBamDecoder extends BamDecoder
{
  private final List<PseudoBamFrameEntry> listFrames = new ArrayList<PseudoBamFrameEntry>();

  private int currentFrame;
  private BufferedImage sharedCanvas;   // temporary buffer for drawing on a shared canvas

  public PseudoBamDecoder(Image image)
  {
    this(new Image[]{image});
  }

  public PseudoBamDecoder(Image[] images)
  {
    super(null);
    init(images);
  }

  /**
   * Adds a new frame to the end of the frame list.
   * @param image The image to add.
   */
  public void add(Image image)
  {
    add(new Image[]{image});
  }

  /**
   * Adds the list of frames to the end of the frame list.
   * @param images An array containing the images to add.
   */
  public void add(Image[] images)
  {
    insert(listFrames.size(), images);
  }

  /**
   * Inserts a frame at the specified position.
   * @param frameIdx The position for the frame to insert.
   * @param image The image to insert.
   */
  public void insert(int frameIdx, Image image)
  {
    insert(frameIdx, new Image[]{image});
  }

  /**
   * Inserts an array of frames at the specified position.
   * @param frameIdx The position for the frames to insert.
   * @param images An array containing the images to insert.
   */
  public void insert(int frameIdx, Image[] images)
  {
    if (frameIdx >= 0 && frameIdx <= listFrames.size() && images != null) {
      for (int i = 0; i < images.length; i++) {
        listFrames.add(frameIdx+i, new PseudoBamFrameEntry(images[i]));
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
      if (currentFrame >= listFrames.size()) {
        currentFrame = listFrames.size() - 1;
      }
    }
  }

  /**
   * Removes all frames from the BAM structure.
   */
  public void clear()
  {
    listFrames.clear();
    currentFrame = -1;
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
    setType(Type.INVALID);
    listFrames.clear();
    currentFrame = -1;
    if (sharedCanvas != null) {
      sharedCanvas.flush();
      sharedCanvas = null;
    }
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
  public int frameImageWidth(int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
      if (frameWidth(frameIdx) > 0) {
        if (getMode() == Mode.Shared) {
          return getSharedRectangle().width;
        } else {
          return frameWidth(frameIdx);
        }
      }
    }
    return 0;
  }

  @Override
  public int frameImageHeight(int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
      if (frameHeight(frameIdx) > 0) {
        if (getMode() == Mode.Shared) {
          return getSharedRectangle().height;
        } else {
          return frameHeight(frameIdx);
        }
      }
    }
    return 0;
  }

  @Override
  public Image frameGet(int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
      int w = frameImageWidth(frameIdx);
      int h = frameImageHeight(frameIdx);
      if (w > 0 && h > 0) {
        BufferedImage image = ColorConvert.createCompatibleImage(w, h, true);
        renderFrame(frameIdx, image);
        return image;
      }
    }
    return ColorConvert.createCompatibleImage(1, 1, true);
  }

  @Override
  public void frameGet(int frameIdx, Image canvas)
  {
    if (canvas != null && frameIdx >= 0 && frameIdx < listFrames.size()) {
      int w = frameImageWidth(frameIdx);
      int h = frameImageHeight(frameIdx);
      if (w > 0 && h > 0 && canvas.getWidth(null) >= w && canvas.getHeight(null) >= h) {
        renderFrame(frameIdx, canvas);
      }
    }
  }

  @Override
  public int[] frameGetData(int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
      int w = frameImageWidth(frameIdx);
      int h = frameImageHeight(frameIdx);
      if (w > 0 && h > 0) {
        int[] buffer = new int[w*h];
        renderFrame(frameIdx, buffer, w, h);
        return buffer;
      }
    }
    return new int[0];
  }

  @Override
  public int frameWidth(int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
      return listFrames.get(frameIdx).getWidth();
    } else {
      return 0;
    }
  }

  @Override
  public int frameHeight(int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
      return listFrames.get(frameIdx).getHeight();
    } else {
      return 0;
    }
  }

  @Override
  public int frameCenterX(int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
      return listFrames.get(frameIdx).getCenterX();
    } else {
      return 0;
    }
  }

  @Override
  public int frameCenterY(int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
      return listFrames.get(frameIdx).getCenterY();
    } else {
      return 0;
    }
  }

  @Override
  public int cycleCount()
  {
    return 1;
  }

  @Override
  public int cycleFrameCount()
  {
    return listFrames.size();
  }

  @Override
  public int cycleFrameCount(int cycleIdx)
  {
    if (cycleIdx == 0) {
      return listFrames.size();
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
    return (currentFrame < listFrames.size() - 1);
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
    return frameGet(frameIdx);
  }

  @Override
  public void cycleGetFrame(Image canvas)
  {
    int frameIdx = cycleGetFrameIndexAbsolute();
    frameGet(frameIdx, canvas);
  }

  @Override
  public Image cycleGetFrame(int frameIdx)
  {
    frameIdx = cycleGetFrameIndexAbsolute(frameIdx);
    return frameGet(frameIdx);
  }

  @Override
  public void cycleGetFrame(int frameIdx, Image canvas)
  {
    frameIdx = cycleGetFrameIndexAbsolute(frameIdx);
    frameGet(frameIdx, canvas);
  }

  @Override
  public int[] cycleGetFrameData()
  {
    int frameIdx = cycleGetFrameIndexAbsolute();
    return frameGetData(frameIdx);
  }

  @Override
  public int[] cycleGetFrameData(int frameIdx)
  {
    frameIdx = cycleGetFrameIndexAbsolute(frameIdx);
    return frameGetData(frameIdx);
  }

  @Override
  public int cycleGetFrameIndex()
  {
    return currentFrame;
  }

  @Override
  public boolean cycleSetFrameIndex(int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
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
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
      return frameIdx;
    }
    return 0;
  }

  @Override
  public int cycleGetFrameIndexAbsolute(int cycleIdx, int frameIdx)
  {
    if (cycleIdx == 0 && frameIdx >= 0 && frameIdx < listFrames.size()) {
      return frameIdx;
    }
    return 0;
  }


  private void init(Image[] images)
  {
    // resetting data
    close();

    if (images != null) {
      for (int i = 0; i < images.length; i++) {
        listFrames.add(new PseudoBamFrameEntry(images[i]));
      }

      updateSharedBamSize();
      sharedCanvas = new BufferedImage(getSharedRectangle().width, getSharedRectangle().height,
                                       BufferedImage.TYPE_INT_ARGB);
    }
  }

  // Draws the absolute frame onto the canvas. Takes BAM mode into account.
  private void renderFrame(int frameIdx, Image canvas)
  {
    BufferedImage image = null;
    if (canvas instanceof BufferedImage) {
      image = (BufferedImage)canvas;
    } else {
      image = sharedCanvas;
    }
    int[] buffer = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
    renderFrame(frameIdx, buffer, image.getWidth(), image.getHeight());
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
  private void renderFrame(int frameIdx, int[] buffer, int width, int height)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size() &&
        buffer != null && buffer.length >= width*height) {
      int srcWidth = listFrames.get(frameIdx).width;
      int srcHeight = listFrames.get(frameIdx).height;
      int[] srcData = ((DataBufferInt)listFrames.get(frameIdx).frame.getRaster().getDataBuffer()).getData();

      Arrays.fill(buffer, 0);

      if (getMode() == Mode.Shared) {
        // drawing on shared canvas
        int left = -getSharedRectangle().x;
        int top = -getSharedRectangle().y;
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
    private int width, height;
    private BufferedImage frame;

    private PseudoBamFrameEntry(Image image)
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
    public int getCenterX() { return 0; }
    @Override
    public int getCenterY() { return 0; }
  }
}
