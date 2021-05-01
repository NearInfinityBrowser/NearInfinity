// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.IndexColorModel;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.ProgressMonitor;

import org.infinity.gui.converter.ConvertToPvrz;
import org.infinity.resource.Profile;
import org.infinity.util.BinPack2D;
import org.infinity.util.DynamicArray;
import org.infinity.util.io.FileManager;
import org.infinity.util.io.StreamUtils;
import org.infinity.util.tuples.Couple;

/**
 * A decoder that takes individual images as input and simulates a BAM structure.
 * Furthermore, this class provides methods for manipulating the frame and cycle structure.
 */
public class PseudoBamDecoder extends BamDecoder
{
  // A list of helpful options that can be applied globally, to frames or to cycles.
  /** A label of some kind for identification purposes. [String] */
  public static final String OPTION_STRING_LABEL    = "Label";
  /** A flag specifying a compression state. (BAM v1 specific) [Boolean] */
  public static final String OPTION_BOOL_COMPRESSED = "Compressed";
  /** A value specifying a compressed pixel value. (BAM v1 specific) [Integer] */
  public static final String OPTION_INT_RLEINDEX    = "RLEIndex";
  /** A value specifying the start index of data blocks (BAM v2 specific) [Integer] */
  public static final String OPTION_INT_BLOCKINDEX  = "BlockIndex";
  /** A value specifying the number of data blocks (BAM v2 specific) [Integer] */
  public static final String OPTION_INT_BLOCKCOUNT  = "BlockCount";


  private static final Color TransparentColor = new Color(0, true);

  private final PseudoBamFrameEntry defaultFrameInfo = new PseudoBamFrameEntry(null, 0, 0);
  private final HashMap<String, Object> mapOptions = new HashMap<>();

  private List<PseudoBamCycleEntry> listCycles = new ArrayList<>();
  private List<PseudoBamFrameEntry> listFrames;
  private PseudoBamControl defaultControl;

  public PseudoBamDecoder()
  {
    this(null, (BufferedImage[])null, (Point[])null);
  }

  public PseudoBamDecoder(List<PseudoBamFrameEntry> framesList)
  {
    this(framesList, (BufferedImage[])null, (Point[])null);
  }

  public PseudoBamDecoder(BufferedImage image)
  {
    this(null, new BufferedImage[]{image}, new Point[0]);
  }

  public PseudoBamDecoder(List<PseudoBamFrameEntry> framesList, BufferedImage image)
  {
    this(framesList, new BufferedImage[]{image}, new Point[0]);
  }

  public PseudoBamDecoder(BufferedImage image, Point center)
  {
    this(null, new BufferedImage[]{image}, new Point[]{center});
  }

  public PseudoBamDecoder(List<PseudoBamFrameEntry> framesList, BufferedImage image, Point center)
  {
    this(framesList, new BufferedImage[]{image}, new Point[]{center});
  }

  public PseudoBamDecoder(BufferedImage[] images)
  {
    this(null, images, new Point[0]);
  }

  public PseudoBamDecoder(List<PseudoBamFrameEntry> framesList, BufferedImage[] images)
  {
    this(framesList, images, new Point[0]);
  }

  public PseudoBamDecoder(BufferedImage[] images, Point[] centers)
  {
    this(null, images, centers);
  }

  public PseudoBamDecoder(List<PseudoBamFrameEntry> framesList, BufferedImage[] images, Point[] centers)
  {
    super(null);
    setFramesList(framesList);
    init(images, centers);
  }

  /** Returns all available options by name. */
  public String[] getOptionNames()
  {
    String[] retVal = new String[mapOptions.keySet().size()];
    Iterator<String> iter = mapOptions.keySet().iterator();
    int idx = 0;
    while (iter.hasNext()) {
      retVal[idx++] = iter.next();
    }

    return retVal;
  }

  /** Returns the value of the specified global BAM option. */
  public Object getOption(String name)
  {
    if (name != null) {
      return mapOptions.get(name);
    }
    return null;
  }

  /** Sets a custom option for the whole BAM. */
  public void setOption(String name, Object value)
  {
    if (name != null) {
      mapOptions.put(name, value);
    }
  }


  /** Returns the currently used frames list. */
  public List<PseudoBamFrameEntry> getFramesList()
  {
    return listFrames;
  }

  /**
   * Attaches a custom list of frame entries to the object.
   * Caution: Methods don't check explicitly for {@code null} entries in the list.
   * @param framesList The new frames list to attach. Specifying {@code null} will create
   *                   a new list automatically.
   */
  public void setFramesList(List<PseudoBamFrameEntry> framesList)
  {
    if (framesList != null) {
      listFrames = framesList;
    } else {
      listFrames = new ArrayList<>();
    }
  }


  /** Returns the currently used cycles list. */
  public List<PseudoBamCycleEntry> getCyclesList()
  {
    return listCycles;
  }

  /**
   * Attaches a custom list of cycle entries to the object.
   * Caution: Methods don't check explicitely for {@code null} entries in the list.
   * @param cyclesList The new cycles list to attach. Specifying {@code null} will create
   *                   a new list automatically.
   */
  public void setCyclesList(List<PseudoBamCycleEntry> cyclesList)
  {
    if (cyclesList != null) {
      listCycles = cyclesList;
    } else {
      listCycles = new ArrayList<>();
    }
  }


  /**
   * Adds a new frame to the end of the frame list. Center position defaults to (0, 0).
   * @param image The image to add.
   * @return The index of the added frame or -1 if frame could not be added.
   */
  public int frameAdd(BufferedImage image)
  {
    return frameInsert(listFrames.size(), new BufferedImage[]{image}, new Point[0]);
  }

  /**
   * Adds a new frame to the end of the frame list.
   * @param image The image to add.
   * @param center The center position of the image.
   * @return The index of the added frame or -1 if frame could not be added.
   */
  public int frameAdd(BufferedImage image, Point center)
  {
    return frameInsert(listFrames.size(), new BufferedImage[]{image}, new Point[]{center});
  }

  /**
   * Adds the list of frames to the end of the frame list. Center positions default to (0, 0).
   * @param images An array containing the images to add.
   * @return The index of the first added frame or -1 if frames could not be added.
   */
  public int frameAdd(BufferedImage[] images)
  {
    return frameInsert(listFrames.size(), images, new Point[0]);
  }

  /**
   * Adds the list of frames to the end of the frame list.
   * @param images An array containing the images to add.
   * @param centers An array of center positions corresponding with the images.
   * @return The index of the first added frame or -1 if frames could not be added.
   */
  public int frameAdd(BufferedImage[] images, Point[] centers)
  {
    return frameInsert(listFrames.size(), images, centers);
  }

  /**
   * Inserts a frame at the specified position. Center position defaults to (0, 0).
   * @param frameIdx The position for the frame to insert.
   * @param image The image to insert.
   * @return The index of the inserted frame or -1 if frame could not be inserted.
   */
  public int frameInsert(int frameIdx, BufferedImage image)
  {
    return frameInsert(frameIdx, new BufferedImage[]{image}, new Point[0]);
  }

  /**
   * Inserts a frame at the specified position.
   * @param frameIdx The position for the frame to insert.
   * @param image The image to insert.
   * @param center The center position of the image.
   * @return The index of the inserted frame or -1 if frame could not be inserted.
   */
  public int frameInsert(int frameIdx, BufferedImage image, Point center)
  {
    return frameInsert(frameIdx, new BufferedImage[]{image}, new Point[]{center});
  }

  /**
   * Inserts an array of frames at the specified position. Center positions default to (0, 0).
   * @param frameIdx The position for the frames to insert.
   * @param images An array containing the images to insert.
   * @return The index of the first inserted frame or -1 if frames could not be inserted.
   */
  public int frameInsert(int frameIdx, BufferedImage[] images)
  {
    return frameInsert(frameIdx, images, new Point[0]);
  }

  /**
   * Inserts an array of frames at the specified position.
   * @param frameIdx The position for the frames to insert.
   * @param images An array containing the images to insert.
   * @param centers An array of center positions corresponding with the images.
   * @return The index of the first inserted frame or -1 if frames could not be inserted.
   */
  public int frameInsert(int frameIdx, BufferedImage[] images, Point[] centers)
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
      return frameIdx;
    }
    return -1;
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
    listCycles.clear();
    listFrames.clear();
  }

  /**
   * Moves the frame by the specified (positive or negative) offset.
   * @return The new frame index, or -1 on error.
   */
  public int frameMove(int frameIdx, int offset)
  {
    if (frameIdx >= 0 && frameIdx < listFrames.size()) {
      int ofsAbs = frameIdx + offset;
      if (ofsAbs < 0) ofsAbs = 0;
      if (ofsAbs >= listFrames.size()) ofsAbs = listFrames.size() - 1;
      if (ofsAbs != frameIdx) {
        PseudoBamFrameEntry entry = listFrames.get(frameIdx);
        listFrames.remove(frameIdx);
        listFrames.add(ofsAbs, entry);
      }
      return ofsAbs;
    }
    return -1;
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
    if (defaultControl != null) {
      defaultControl.cycleSet(0);
    }
    listCycles.clear();
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
  public ByteBuffer getResourceBuffer()
  {
    return StreamUtils.getByteBuffer(0);
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
      if (control.getMode() == BamDecoder.BamControl.Mode.SHARED) {
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
      if (control.getMode() == BamDecoder.BamControl.Mode.SHARED) {
        control.updateSharedBamSize();
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
      listCycles.add(new PseudoBamCycleEntry(indices));
    }

    // creating default bam control instance as a fallback option
    defaultControl = new PseudoBamControl(this);
    defaultControl.setMode(BamControl.Mode.SHARED);
    defaultControl.setSharedPerCycle(false);
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
      int srcPixelStride = srcImage.getRaster().getSampleModel().getNumDataElements();
      int srcBufferType = srcImage.getRaster().getDataBuffer().getDataType();
      int dstBufferType = dstImage.getRaster().getDataBuffer().getDataType();
      byte[] srcBufferB = null, dstBufferB = null;
      int[] srcBufferI = null, dstBufferI = null;
      IndexColorModel cm = null;
      if (srcBufferType == DataBuffer.TYPE_BYTE) {
        srcBufferB = ((DataBufferByte)srcImage.getRaster().getDataBuffer()).getData();
        if (srcImage.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
          cm = (IndexColorModel)srcImage.getColorModel();
        } else if (srcPixelStride == 3 || srcPixelStride == 4) {
          // XXX: a hack to convert non-paletted pixel types on-the-fly
          srcBufferI = new int[srcImage.getWidth()*srcImage.getHeight()];
          int[] shift;
          int mask;
          if (srcPixelStride == 3) {
            shift = new int[]{0, 8, 16};
            mask = 0xff000000;
          } else {
            shift = new int[]{24, 0, 8, 16};
            mask = 0;
          }
          for (int si = 0, di = 0, numPixels = srcBufferI.length; di < numPixels; si += srcPixelStride, di++) {
            int px = 0;
            for (int i = 0, cnt = shift.length; i < cnt; i++) {
              px |= (srcBufferB[si+i] & 0xff) << shift[i];
            }
            px |= mask;
            srcBufferI[di] = px;
          }
          srcBufferB = null;
        } else {
          // not supported
          return;
        }
      } else if (srcBufferType == DataBuffer.TYPE_INT) {
        srcBufferI = ((DataBufferInt)srcImage.getRaster().getDataBuffer()).getData();
      }
      if (dstBufferType == DataBuffer.TYPE_BYTE) {
        dstBufferB = ((DataBufferByte)dstImage.getRaster().getDataBuffer()).getData();
      } else if (dstBufferType == DataBuffer.TYPE_INT) {
        dstBufferI = ((DataBufferInt)dstImage.getRaster().getDataBuffer()).getData();
      }
      if (srcBufferI != null && dstBufferB != null) {
        // incompatible combination
        return;
      }

      int dstWidth = dstImage.getWidth();
      int dstHeight = dstImage.getHeight();
      int srcWidth = listFrames.get(frameIdx).getWidth();
      int srcHeight = listFrames.get(frameIdx).getHeight();
      if (control.getMode() == BamControl.Mode.SHARED) {
        // drawing on shared canvas
        Rectangle shared = control.getSharedRectangle();
        int srcCenterX = listFrames.get(frameIdx).getCenterX();
        int srcCenterY = listFrames.get(frameIdx).getCenterY();
        int left = -shared.x - srcCenterX;
        int top = -shared.y - srcCenterY;
        int maxWidth = (dstWidth < srcWidth + left) ? dstWidth : srcWidth;
        int maxHeight = (dstHeight < srcHeight + top) ? dstHeight : srcHeight;
        int srcOfs = 0, dstOfs = top*dstWidth + left;
        for (int y = 0; y < maxHeight; y++) {
          for (int x = 0; x < maxWidth; x++) {
            if (srcBufferB != null) {
              if (dstBufferB != null) {
                dstBufferB[dstOfs+x] = srcBufferB[srcOfs+x];
              } else {
                dstBufferI[dstOfs+x] = cm.getRGB(srcBufferB[srcOfs+x] & 0xff);
              }
            } else {
              // only one possible combination left
              dstBufferI[dstOfs+x] = srcBufferI[srcOfs+x];
            }
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
            if (srcBufferB != null) {
              if (dstBufferB != null) {
                dstBufferB[dstOfs+x] = srcBufferB[srcOfs+x];
              } else {
                dstBufferI[dstOfs+x] = cm.getRGB(srcBufferB[srcOfs+x] & 0xff);
              }
            } else {
              // only one possible combination left
              dstBufferI[dstOfs+x] = srcBufferI[srcOfs+x];
            }
          }
          srcOfs += srcWidth;
          dstOfs += dstWidth;
        }
      }
      srcBufferB = null; dstBufferB = null;
      srcBufferI = null; dstBufferI = null;

      // rendering resulting image onto the canvas if needed
      if (dstImage != canvas) {
        Graphics2D g = (Graphics2D)canvas.getGraphics();
        try {
          if (getComposite() != null) {
            g.setComposite(getComposite());
          }
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

  /**
   * Determines whether "color" can be classified as RLE-compressed color.
   * @param color The color to check.
   * @param rleColor The RLE color index.
   * @param threshold The amount of alpha allowed for opaque colors.
   * @return {@code true} if the color is determined as the RLE-compressed color.
   */
  public static boolean isRleColor(int color, int rleColor, int threshold)
  {
    final int Green = 0x0000ff00;
    rleColor &= 0x00ffffff;
    if (threshold < 0) threshold = 0; else if (threshold > 255) threshold = 255;
    boolean inThreshold = (((color >>> 24) & 0xff) < (255 - threshold));
    color &= 0x00ffffff;
    return (color == rleColor || (inThreshold && rleColor == Green));
  }

  /**
   * Determines whether "color" is interpreted as "transparent".
   * @param color The color to check.
   * @param threshold The amount of alpha allowed for opaque colors. Specify negative value to skip check.
   * @return {@code true} if the color is determined as "transparent".
   */
  public static boolean isTransparentColor(int color, int threshold)
  {
    if (threshold < 0) return (color & 0xff000000) == 0;
    final int Green = 0x0000ff00;
    if (threshold < 0) threshold = 0; else if (threshold > 255) threshold = 255;
    boolean isAlpha = (((color >>> 24) & 0xff) < (255 - threshold));
    boolean isGreen = ((color & 0x00ffffff) == Green);
    return (isAlpha || isGreen);
  }


  /**
   * Creates a BAM v1 resource from the current BAM structure. Requires paletted source frames
   * using a common palette for all frames.
   * @param fileName The filename of the BAM file to export.
   * @param progress An optional progress monitor to display the state of the export progress.
   * @param curProgress The current progress state of the progress monitor.
   * @return {@code true} if the export was successfull, {@code false} otherwise.
   * @throws Exception If an unrecoverable error occured.
   */
  public boolean exportBamV1(Path fileName, ProgressMonitor progress, int curProgress) throws Exception
  {
    final int FrameEntrySize = 12;
    final int CycleEntrySize = 4;

    if (!listFrames.isEmpty() && !listCycles.isEmpty()) {
      // sanity checks
      if (fileName == null) {
        throw new Exception("Invalid filename specified.");
      }
      if (listFrames.size() > 65535) {
        throw new Exception("No more than 65535 frames supported.");
      }
      if (listCycles.size() > 255) {
        throw new Exception("No more than 255 cycles supported.");
      }
      for (int i = 0; i < listCycles.size(); i++) {
        if (listCycles.get(i).size() > 65535) {
          throw new Exception(String.format("No more than 65535 frames per cycle supported. " +
                                            "Cycle %d contains %d entries.",
                                            i, listCycles.get(i).size()));
        }
      }

      int[] palette = null;
      int transIndex = -1;
      for (int i = 0; i < listFrames.size(); i++) {
        PseudoBamFrameEntry entry = listFrames.get(i);

        // checking source frame type
        if (entry.getFrame().getType() != BufferedImage.TYPE_BYTE_INDEXED) {
          throw new Exception("Unsupported source frame image type.");
        }

        // checking frame properties
        if (entry.getWidth() <= 0 || entry.getWidth() > 65535 || entry.getHeight() <= 0 || entry.getHeight() > 65535 ||
            entry.getCenterX() < Short.MIN_VALUE || entry.getCenterX() > Short.MAX_VALUE ||
            entry.getCenterY() < Short.MIN_VALUE || entry.getCenterY() > Short.MAX_VALUE) {
          throw new Exception("Dimensions are out of range for frame index " + i);
        }

        // rudimentary palette check
        if (palette == null) {
          final int Green = 0x0000ff00;
          IndexColorModel cm = (IndexColorModel)entry.getFrame().getColorModel();
          palette = new int[1 << cm.getPixelSize()];
          cm.getRGBs(palette);
          for (int j = 0; j < palette.length; j++) {
            int c = palette[i] & 0x00ffffff;
            if (transIndex < 0 && c == Green) {
              transIndex = j;
              break;
            }
          }
          if (transIndex < 0) {
            transIndex = 0;
          }
        } else {
          IndexColorModel cm = (IndexColorModel)entry.getFrame().getColorModel();
          if (palette.length != (1 <<cm.getPixelSize())) {
            throw new Exception("Incompatible palette found in source frame " + i);
          }
        }
      }

      // initializing progress monitor
      if (progress != null) {
        if (curProgress < 0) curProgress = 0;
        progress.setMaximum(progress.getMaximum() + 2);
        progress.setProgress(curProgress++);
        progress.setNote("Encoding frames");
      }

      // calculating the max. space required for a single frame
      PseudoBamControl control = createControl();
      control.setMode(BamDecoder.BamControl.Mode.SHARED);
      control.setSharedPerCycle(false);
      Dimension dimFrame = control.calculateSharedCanvas(false).getSize();
      int maxImageSize = (dimFrame.width*dimFrame.height*3) / 2;    // about 1.5x of max. size
      List<byte[]> listFrameData = new ArrayList<>(listFrames.size());

      // encoding frames
      Object o = getOption(OPTION_INT_RLEINDEX);
      byte rleIndex = (byte)(((o != null) ? ((Integer)o).intValue() : 0) & 0xff);
      byte[] dstData = new byte[maxImageSize];

      for (int idx = 0; idx < listFrames.size(); idx++) {
        o = listFrames.get(idx).getOption(OPTION_BOOL_COMPRESSED);
        boolean frameCompressed = (o != null) ? ((Boolean)o).booleanValue() : false;
        PseudoBamFrameEntry entry = listFrames.get(idx);
        byte[] srcBuffer = ((DataBufferByte)entry.frame.getRaster().getDataBuffer()).getData();

        if (frameCompressed) {
          // creating RLE compressed frame
          int srcIdx = 0, dstIdx = 0, srcMax = srcBuffer.length;
          while (srcIdx < srcMax) {
            if (rleIndex == srcBuffer[srcIdx]) {
              // color to compress
              int cnt = 0;
              srcIdx++;
              while (srcIdx < srcMax && cnt < 255 && rleIndex == srcBuffer[srcIdx]) {
                cnt++;
                srcIdx++;
              }
              dstData[dstIdx++] = rleIndex;
              dstData[dstIdx++] = (byte)cnt;
            } else {
              // uncompressed pixels
              dstData[dstIdx++] = srcBuffer[srcIdx++];
            }
          }
          // storing the resulting frame data
          byte[] outData = new byte[dstIdx];
          System.arraycopy(dstData, 0, outData, 0, dstIdx);
          listFrameData.add(outData);
        } else {
          // creating uncompressed frame
          System.arraycopy(srcBuffer, 0, dstData, 0, srcBuffer.length);
          // storing the resulting frame data
          byte[] outData = new byte[srcBuffer.length];
          System.arraycopy(dstData, 0, outData, 0, srcBuffer.length);
          listFrameData.add(outData);
        }
        srcBuffer = null;
      }

      if (progress != null) {
        progress.setProgress(curProgress++);
        progress.setNote("Generating BAM");
      }

      // creating cycles table and frame lookup table
      List<Integer> listFrameLookup = new ArrayList<>();
      int lookupSize = 0;
      for (int i = 0; i < listCycles.size(); i++) {
        listFrameLookup.add(Integer.valueOf(lookupSize));
        lookupSize += listCycles.get(i).size();
      }

      // putting it all together
      int ofsFrameEntries = 0x18;
      int ofsPalette = ofsFrameEntries + listFrames.size()*FrameEntrySize + listCycles.size()*CycleEntrySize;
      int ofsLookup = ofsPalette + 1024;
      int ofsFrameData = ofsLookup + lookupSize*2;
      int bamSize = ofsFrameData;
      // updating frame offsets
      int[] frameDataOffsets = new int[listFrameData.size()];
      for (int i = 0; i < listFrameData.size(); i++) {
        frameDataOffsets[i] = bamSize;
        o = listFrames.get(i).getOption(OPTION_BOOL_COMPRESSED);
        if (o == null || ((Boolean)o).booleanValue() == false) {
          frameDataOffsets[i] |= 0x80000000;
        }
        bamSize += listFrameData.get(i).length;
      }

      byte[] bamData = new byte[bamSize];
      System.arraycopy("BAM V1  ".getBytes(), 0, bamData, 0, 8);
      DynamicArray.putShort(bamData, 0x08, (short)listFrames.size());
      DynamicArray.putByte(bamData, 0x0a, (byte)listCycles.size());
      DynamicArray.putByte(bamData, 0x0b, rleIndex);
      DynamicArray.putInt(bamData, 0x0c, ofsFrameEntries);
      DynamicArray.putInt(bamData, 0x10, ofsPalette);
      DynamicArray.putInt(bamData, 0x14, ofsLookup);

      // adding frame entries
      int curOfs = ofsFrameEntries;
      for (int i = 0; i < listFrames.size(); i++) {
        DynamicArray.putShort(bamData, curOfs, (short)listFrames.get(i).getWidth());
        DynamicArray.putShort(bamData, curOfs + 2, (short)listFrames.get(i).getHeight());
        DynamicArray.putShort(bamData, curOfs + 4, (short)listFrames.get(i).getCenterX());
        DynamicArray.putShort(bamData, curOfs + 6, (short)listFrames.get(i).getCenterY());
        DynamicArray.putInt(bamData, curOfs + 8, frameDataOffsets[i]);
        curOfs += FrameEntrySize;
      }

      // adding cycle entries
      for (int i = 0; i < listCycles.size(); i++) {
        DynamicArray.putShort(bamData, curOfs, (short)listCycles.get(i).size());
        DynamicArray.putShort(bamData, curOfs + 2, listFrameLookup.get(i).shortValue());
        curOfs += CycleEntrySize;
      }

      // adding palette
      for (int i = 0; i < palette.length; i++) {
        DynamicArray.putByte(bamData, curOfs, (byte)(palette[i] & 0xff));               // red
        DynamicArray.putByte(bamData, curOfs + 1, (byte)((palette[i] >>> 8) & 0xff));   // green
        DynamicArray.putByte(bamData, curOfs + 2, (byte)((palette[i] >>> 16) & 0xff));  // blue
        byte a = (byte)((palette[i] >>> 24) & 0xff);
        if (a == (byte)255) a = 0;
        DynamicArray.putByte(bamData, curOfs + 3, a);                                   // alpha
        curOfs += 4;
      }

      // adding frame lookup table
      for (int i = 0; i < listCycles.size(); i++) {
        for (int j = 0; j < listCycles.get(i).frames.size(); j++) {
          DynamicArray.putShort(bamData, curOfs, listCycles.get(i).frames.get(j).shortValue());
          curOfs += 2;
        }
      }

      // adding frame graphics data
      for (int i = 0; i < listFrameData.size(); i++) {
        System.arraycopy(listFrameData.get(i), 0, bamData, curOfs, listFrameData.get(i).length);
        curOfs += listFrameData.get(i).length;
      }

      // compressing BAM (optional)
      o = getOption(OPTION_BOOL_COMPRESSED);
      boolean isCompressed = (o != null) ? ((Boolean)o).booleanValue() : false;
      if (isCompressed) {
        bamData = Compressor.compress(bamData, "BAMC", "V1  ");
      }

      // writing BAM to disk
      try (OutputStream os = StreamUtils.getOutputStream(fileName, true)) {
        os.write(bamData);
      } catch (Exception e) {
        e.printStackTrace();
        throw e;
      }
      bamData = null;
      return true;
    }

    return false;
  }


  /**
   * Creates a BAM v2 resource from the current BAM structure.
   * @param fileName The BAM filename. Path is also used for associated PVRZ files.
   * @param dxtType The desired DXTn compression type to use.
   * @param pvrzIndex The start index of PVRZ files.
   * @param progress An optional progress monitor to display the state of the export progress.
   * @param curProgress The current progress state of the progress monitor.
   * @return {@code true} if the export was successful, {@code false} otherwise.
   * @throws Exception If an unrecoverable error occured.
   */
  public boolean exportBamV2(Path fileName, DxtEncoder.DxtType dxtType, int pvrzIndex,
                              ProgressMonitor progress, int curProgress) throws Exception
  {
    final int FrameEntrySize = 12;
    final int CycleEntrySize = 4;
    final int BlockEntrySize = 28;

    if (!listFrames.isEmpty() && !listCycles.isEmpty()) {
      // sanity checks
      if (fileName == null) {
        throw new Exception("Invalid filename specified.");
      }
      if (dxtType != DxtEncoder.DxtType.DXT1 && dxtType != DxtEncoder.DxtType.DXT5) {
        dxtType = DxtEncoder.DxtType.DXT5;
      }
      if (pvrzIndex < 0 || pvrzIndex > 99999) {
        throw new Exception("PVRZ start index is out of range [0..99999].");
      }

      // preparing output path for PVRZ files
      Path pvrzFilePath = fileName.toAbsolutePath().getParent();
      List<FrameDataV2> listFrameData = new ArrayList<>(listFrames.size());
      List<BinPack2D> listGrid = new ArrayList<>();

      // initializing progress monitor
      if (progress != null) {
        if (curProgress < 0) curProgress = 0;
        progress.setMaximum(progress.getMaximum() + 5);
        progress.setProgress(curProgress++);
        progress.setNote("Calculating PVRZ layout");
      }

      // preparations
      // generating block data list
      if (!buildFrameDataList(listFrameData, listGrid, pvrzIndex)) {
        return false;
      }

      // generating remaining info blocks
      List<FrameDataV2> listFrameDataBlocks = new ArrayList<>();
      List<PseudoBamFrameEntry> listFrameEntries = new ArrayList<>();
      List<Couple<Short, Short>> listCycleData = new ArrayList<>(listCycles.size());
      int frameStartIndex = 0;    // keeps track of current start index of frame entries
      int blockStartIndex = 0;    // keeps track of current start index of frame data blocks
      for (int i = 0; i < listCycles.size(); i++) {
        List<Integer> cycleFrames = listCycles.get(i).frames;

        // generating cycle entries
        Couple<Short, Short> cycle = Couple.with(Short.valueOf((short)cycleFrames.size()),
                                                 Short.valueOf((short)frameStartIndex));
        listCycleData.add(cycle);

        for (int j = 0; j < cycleFrames.size(); j++) {
          int idx = cycleFrames.get(j).intValue();
          try {
            FrameDataV2 frame = listFrameData.get(idx);
            PseudoBamFrameEntry bfe = listFrames.get(idx);

            PseudoBamFrameEntry entry = new PseudoBamFrameEntry(bfe.frame, bfe.getCenterX(), bfe.getCenterY());
            entry.setOption(OPTION_INT_BLOCKINDEX, Integer.valueOf(blockStartIndex));
            entry.setOption(OPTION_INT_BLOCKCOUNT, Integer.valueOf(1));
            listFrameEntries.add(entry);
            blockStartIndex++;
            listFrameDataBlocks.add(frame);
          } catch (IndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException(
                String.format("Invalid frame index %d found in cycle %d", idx, i));
          }
        }
        frameStartIndex += cycleFrames.size();
      }

      // putting it all together
      int ofsFrameEntries = 0x20;
      int ofsCycleEntries = ofsFrameEntries + listFrameEntries.size()*FrameEntrySize;
      int ofsFrameData = ofsCycleEntries + listCycleData.size()*CycleEntrySize;
      int bamSize = ofsFrameData + listFrameDataBlocks.size()*BlockEntrySize;
      byte[] bamData = new byte[bamSize];

      // writing main header
      System.arraycopy("BAM V2  ".getBytes(), 0, bamData, 0, 8);
      DynamicArray.putInt(bamData, 0x08, listFrameEntries.size());
      DynamicArray.putInt(bamData, 0x0c, listCycleData.size());
      DynamicArray.putInt(bamData, 0x10, listFrameDataBlocks.size());
      DynamicArray.putInt(bamData, 0x14, ofsFrameEntries);
      DynamicArray.putInt(bamData, 0x18, ofsCycleEntries);
      DynamicArray.putInt(bamData, 0x1c, ofsFrameData);

      // writing frame entries
      int ofs = ofsFrameEntries;
      Object o;
      short v;
      for (int i = 0; i < listFrameEntries.size(); i++) {
        PseudoBamFrameEntry fe = listFrameEntries.get(i);
        DynamicArray.putShort(bamData, ofs, (short)fe.getWidth());
        DynamicArray.putShort(bamData, ofs + 2, (short)fe.getHeight());
        DynamicArray.putShort(bamData, ofs + 4, (short)fe.getCenterX());
        DynamicArray.putShort(bamData, ofs + 6, (short)fe.getCenterY());
        o = fe.getOption(OPTION_INT_BLOCKINDEX);
        v = (o != null) ? ((Integer)o).shortValue() : 0;
        DynamicArray.putShort(bamData, ofs + 8, v);
        o = fe.getOption(OPTION_INT_BLOCKCOUNT);
        v = (o != null) ? ((Integer)o).shortValue() : 0;
        DynamicArray.putShort(bamData, ofs + 10, v);
        ofs += FrameEntrySize;
      }

      // writing cycle entries
      for (int i = 0; i < listCycleData.size(); i++) {
        Couple<Short, Short> entry = listCycleData.get(i);
        DynamicArray.putShort(bamData, ofs, entry.getValue0().shortValue());
        DynamicArray.putShort(bamData, ofs + 2, entry.getValue1().shortValue());
        ofs += CycleEntrySize;
      }

      // writing frame data blocks
      for (int i = 0; i < listFrameDataBlocks.size(); i++) {
        FrameDataV2 entry = listFrameDataBlocks.get(i);
        DynamicArray.putInt(bamData, ofs, entry.page);
        DynamicArray.putInt(bamData, ofs + 4, entry.sx);
        DynamicArray.putInt(bamData, ofs + 8, entry.sy);
        DynamicArray.putInt(bamData, ofs + 12, entry.width);
        DynamicArray.putInt(bamData, ofs + 16, entry.height);
        DynamicArray.putInt(bamData, ofs + 20, entry.dx);
        DynamicArray.putInt(bamData, ofs + 24, entry.dy);
        ofs += BlockEntrySize;
      }

      // writing BAM to disk
      try (OutputStream os = StreamUtils.getOutputStream(fileName, true)) {
        os.write(bamData);
      } catch (Exception e) {
        e.printStackTrace();
        throw e;
      }
      bamData = null;

      // generating PVRZ files
      return createPvrzPages(pvrzFilePath, dxtType, listGrid, listFrameData, progress, curProgress);
    }
    return false;
  }


  /**
   * Creates an array of max. 255 colors that can be used to create a global palette for all available frames.
   * Makes use of the specified color map if available. Does not consider transparent color.
   * @param colorMap An optional color map that will be used if available. Can be {@code null}.
   * @return An int array containing up to 255 colors without the transparent color entry.
   */
  public int[] createGlobalPalette(HashMap<Integer, Integer> colorMap)
  {
    final Integer Green = Integer.valueOf(0xff00ff00);

    int[] retVal;
    if (!listFrames.isEmpty() && !listCycles.isEmpty()) {
      // adding pixels of all available frames to the hashset
      HashMap<Integer, Integer> newMap;
      if (colorMap == null) {
        newMap = new HashMap<>();
        for (int i = 0; i < listFrames.size(); i++) {
          registerColors(newMap, listFrames.get(i).frame);
        }
      } else {
        newMap = new HashMap<>(colorMap.size());
        colorMap.forEach((k,v) -> {
          if ((k.intValue() & 0xff000000) == 0) {
            k = Integer.valueOf(k.intValue() | 0xff000000);
          }
          newMap.put(k, v);
        });
      }

      // transparent color does not count
      newMap.remove(Green);

      // creating palette
      int numColors = newMap.size();
      int[] colorBuffer = new int[numColors];
      Iterator<Integer> iter = newMap.keySet().iterator();
      int idx = 0;
      while (iter.hasNext()) {
        colorBuffer[idx] = iter.next();
        idx++;
      }
      if (colorBuffer.length > 255) {
        boolean ignoreAlpha = !(Boolean)Profile.getProperty(Profile.Key.IS_SUPPORTED_BAM_V1_ALPHA);
        retVal = ColorConvert.medianCut(colorBuffer, 255, ignoreAlpha);
      } else {
        retVal = colorBuffer;
      }

      // removing duplicate entries from the palette
      HashSet<Integer> colorSet = new HashSet<>();
      for (int i = 0; i < retVal.length; i++) {
        colorSet.add(Integer.valueOf(retVal[i]));
      }
      if (colorSet.size() != retVal.length) {
        retVal = new int[colorSet.size()];
        idx = 0;
        iter = colorSet.iterator();
        while (iter.hasNext()) {
          retVal[idx] = iter.next().intValue();
          idx++;
        }
      }
    } else {
      retVal = new int[0];
    }
    return retVal;
  }


  /** Maps all color values of the specified image. */
  public static void registerColors(HashMap<Integer, Integer> colorMap, BufferedImage image)
  {
    final int Green = 0xff00ff00;

    if (image != null) {
      if (image.getType() == BufferedImage.TYPE_BYTE_INDEXED &&
          image.getColorModel() instanceof IndexColorModel) {
        IndexColorModel cm = (IndexColorModel)image.getColorModel();
        boolean hasAlpha = cm.hasAlpha();
        int numColors = 1 << cm.getPixelSize();
        for (int i = 0; i < numColors; i++) {
          int color = cm.getRGB(i);

          // determining transparency
          if (hasAlpha && ((color & 0xff000000) == 0)) {
            color = Green;
          }

          // registering color in map
          Integer key = Integer.valueOf(color);
          Integer count = colorMap.get(key);
          if (count == null) {
            count = Integer.valueOf(1);
          } else {
            ++count;
          }
          colorMap.put(key, count);
        }
      } else if (image.getRaster().getDataBuffer().getDataType() == DataBuffer.TYPE_INT) {
        int[] buffer = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < buffer.length; i++) {
          int color = buffer[i];

          // determining transparency
          if ((color & 0xff000000) == 0) {
            color = Green;
          }

          // registering color in map
          Integer key = Integer.valueOf(color);
          Integer count = colorMap.get(key);
          if (count == null) {
            count = Integer.valueOf(1);
          } else {
            ++count;
          }
          colorMap.put(key, count);
        }
      }
    }
  }

  /** Unmaps all color values of the specified image. */
  public static void unregisterColors(HashMap<Integer, Integer> colorMap, BufferedImage image)
  {
    final int Green = 0xff00ff00;

    if (image != null) {
      if (image.getType() == BufferedImage.TYPE_BYTE_INDEXED &&
          image.getColorModel() instanceof IndexColorModel) {
        IndexColorModel cm = (IndexColorModel)image.getColorModel();
        byte[] buffer = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
        boolean hasAlpha = cm.hasAlpha();
        for (int i = 0; i < buffer.length; i++) {
          int pixel = buffer[i] & 0xff;
          int color = (cm.getAlpha(pixel) << 24) |
                      (cm.getRed(pixel) << 16) |
                      (cm.getGreen(pixel) << 8) |
                      cm.getBlue(pixel);

          // determining transparency
          if (hasAlpha && cm.getAlpha(pixel) == 0) {
            color = Green;
          }

          // unregistering color in map
          Integer key = Integer.valueOf(color);
          Integer count = colorMap.get(key);
          if (count != null) {
            --count;
            if (count == 0) {
              colorMap.remove(key);
            } else {
              colorMap.put(key, count);
            }
          }
        }
      } else if (image.getRaster().getDataBuffer().getDataType() == DataBuffer.TYPE_INT) {
        int[] buffer = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < buffer.length; i++) {
          int color = buffer[i];

          // determining transparency
          if ((color & 0xff000000) == 0) {
            color = Green;
          }

          // unregistering color in map
          Integer key = Integer.valueOf(color);
          Integer count = colorMap.get(key);
          if (count != null) {
            --count;
            if (count == 0) {
              colorMap.remove(key);
            } else {
              colorMap.put(key, count);
            }
          }
        }
      }
    }
  }


  // Calculates the locations of all frames on PVRZ textures and stores the results in framesList and gridList.
  private boolean buildFrameDataList(List<FrameDataV2> framesList, List<BinPack2D> gridList,
                                     int pvrzPageIndex) throws Exception
  {
    if (framesList != null && gridList != null && pvrzPageIndex >= 0 && pvrzPageIndex < 99999) {
      final int pageDim = 1024;
      final BinPack2D.HeuristicRules binPackRule = BinPack2D.HeuristicRules.BOTTOM_LEFT_RULE;

      for (int frameIdx = 0; frameIdx < listFrames.size(); frameIdx++) {
        int imgWidth = listFrames.get(frameIdx).frame.getWidth() + 2;
        int imgHeight = listFrames.get(frameIdx).frame.getHeight() + 2;

        // use multiple of 4 to take advantage of texture compression algorithm
        Dimension space = new Dimension((imgWidth+3) & ~3, (imgHeight+3) & ~3);
        int pageIdx = -1;
        Rectangle rectMatch = null;
        for (int i = 0; i < gridList.size(); i++) {
          BinPack2D packer = gridList.get(i);
          rectMatch = packer.insert(space.width, space.height, binPackRule);
          if (rectMatch.height > 0) {
            pageIdx = i;
            break;
          }
        }

        // create new page?
        if (pageIdx < 0) {
          BinPack2D packer = new BinPack2D(pageDim, pageDim);
          gridList.add(packer);
          pageIdx = gridList.size() - 1;
          rectMatch = packer.insert(space.width, space.height, binPackRule);
        }

        // registering page entry (centering frame in padded region)
        FrameDataV2 entry = new FrameDataV2(pvrzPageIndex + pageIdx,
                                            rectMatch.x + 1, rectMatch.y + 1,
                                            imgWidth - 2, imgHeight - 2,
                                            0, 0);
        framesList.add(entry);
      }

      if (pvrzPageIndex + gridList.size() > 100000) {
        throw new Exception(String.format("The number of required PVRZ files exceeds the max. index of 99999.\n" +
                                          "Please choose a PVRZ start index smaller than or equal to %d.",
                                          100000 - gridList.size()));
      }
      return true;
    }
    return false;
  }

  // Creates all PVRZ files defined in the method arguments.
  private boolean createPvrzPages(Path path, DxtEncoder.DxtType dxtType, List<BinPack2D> gridList,
                                  List<FrameDataV2> framesList, ProgressMonitor progress,
                                  int curProgress) throws Exception
  {
    if (path == null) {
      path = FileManager.resolve("");
    }
    int dxtCode = (dxtType == DxtEncoder.DxtType.DXT5) ? 11 : 7;
    byte[] output = new byte[DxtEncoder.calcImageSize(1024, 1024, dxtType)];
    int pageMin = Integer.MAX_VALUE;
    int pageMax = -1;
    for (int i = 0; i < framesList.size(); i++) {
      FrameDataV2 entry = framesList.get(i);
      pageMin = Math.min(pageMin, entry.page);
      pageMax = Math.max(pageMax, entry.page);
    }

    String note = "Generating PVRZ file %s / %s";
    if (progress != null) {
      if (curProgress < 0) curProgress = 0;
      progress.setMaximum(curProgress + pageMax - pageMin + 1);
      progress.setProgress(curProgress++);
    }

    // processing each PVRZ page
    for (int i = pageMin; i <= pageMax; i++) {
      if (progress != null) {
        if (progress.isCanceled()) {
          throw new Exception("Conversion has been cancelled by the user.");
        }
        progress.setProgress(curProgress);
        progress.setNote(String.format(note, curProgress, pageMax - pageMin + 1));
        curProgress++;
      }

      Path pvrzName = path.resolve(String.format("MOS%04d.PVRZ", i));
      BinPack2D packer = gridList.get(i - pageMin);
      packer.shrinkBin(true);

      // generating texture image
      int tw = packer.getBinWidth();
      int th = packer.getBinHeight();
      BufferedImage texture = ColorConvert.createCompatibleImage(tw, th, true);
      Graphics2D g = texture.createGraphics();
      try {
        g.setComposite(AlphaComposite.Src);
        g.setColor(TransparentColor);
        g.fillRect(0, 0, texture.getWidth(), texture.getHeight());
        for (int frameIdx = 0; frameIdx < listFrames.size(); frameIdx++) {
          BufferedImage image = listFrames.get(frameIdx).frame;
          FrameDataV2 frame = framesList.get(frameIdx);
          if (frame.page == i) {
            int sx = frame.dx, sy = frame.dy;
            int dx = frame.sx, dy = frame.sy;
            int w = frame.width, h = frame.height;
            g.fillRect(dx - 1, dy - 1, w + 2, h + 2);   // compensating for padding done in buildFrameDataList()
            g.drawImage(image, dx, dy, dx+w, dy+h, sx, sy, sx+w, sy+h, null);
          }
        }
      } finally {
        g.dispose();
        g = null;
      }

      // compressing PVRZ
      String errorMsg = null;
      int[] textureData = ((DataBufferInt)texture.getRaster().getDataBuffer()).getData();
      try {
        int outSize = DxtEncoder.calcImageSize(texture.getWidth(), texture.getHeight(), dxtType);
        DxtEncoder.encodeImage(textureData, texture.getWidth(), texture.getHeight(), output, dxtType);
        byte[] header = ConvertToPvrz.createPVRHeader(texture.getWidth(), texture.getHeight(), dxtCode);
        byte[] pvrz = new byte[header.length + outSize];
        System.arraycopy(header, 0, pvrz, 0, header.length);
        System.arraycopy(output, 0, pvrz, header.length, outSize);
        header = null;
        pvrz = Compressor.compress(pvrz, 0, pvrz.length, true);

        // writing PVRZ to disk
        try (OutputStream os = StreamUtils.getOutputStream(pvrzName, true)) {
          os.write(pvrz);
        } catch (Exception e) {
          errorMsg = String.format("Error writing PVRZ file \"%s\" to disk.", pvrzName);
          e.printStackTrace();
        }
        textureData = null;
        pvrz = null;
      } catch (Exception e) {
        e.printStackTrace();
        errorMsg = String.format("Error generating PVRZ files:\n%s.", e.getMessage());
      }

      if (errorMsg != null) {
        throw new Exception(errorMsg);
      }
    }
    output = null;
    return true;
  }

  @Override
  public int hashCode()
  {
    int hash = super.hashCode();
    hash = 31 * hash + ((mapOptions == null) ? 0 : mapOptions.hashCode());
    hash = 31 * hash + ((listCycles == null) ? 0 : listCycles.hashCode());
    hash = 31 * hash + ((listFrames == null) ? 0 : listFrames.hashCode());
    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (!(o instanceof PseudoBamDecoder)) {
      return false;
    }
    boolean retVal = super.equals(o);
    if (retVal) {
      PseudoBamDecoder other = (PseudoBamDecoder)o;
      retVal &= (this.mapOptions == null && other.mapOptions == null) ||
                (this.mapOptions != null && this.mapOptions.equals(other.mapOptions));
      retVal &= (this.listCycles == null && other.listCycles == null) ||
                (this.listCycles != null && this.listCycles.equals(other.listCycles));
      retVal &= (this.listFrames == null && other.listFrames == null) ||
                (this.listFrames != null && this.listFrames.equals(other.listFrames));
    }
    return retVal;
  }

//-------------------------- INNER CLASSES --------------------------

  /** Provides information for a single frame entry */
  public static class PseudoBamFrameEntry implements FrameEntry
  {
    private final HashMap<String, Object> mapOptions = new HashMap<>();

    private int width, height, centerX, centerY;
    private int overrideCenterX, overrideCenterY;
    private BufferedImage frame;

    public PseudoBamFrameEntry(BufferedImage image, int centerX, int centerY)
    {
      setFrame(image);
      setCenterX(centerX);
      setCenterY(centerY);
      this.centerX = getCenterX();
      this.centerY = getCenterY();
    }

    @Override
    public int getWidth() { return width; }
    @Override
    public int getHeight() { return height; }
    @Override
    public int getCenterX() { return overrideCenterX; }
    @Override
    public int getCenterY() { return overrideCenterY; }

    @Override
    public void setCenterX(int x) { overrideCenterX = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, x)); }
    @Override
    public void setCenterY(int y) { overrideCenterY = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, y)); }
    @Override
    public void resetCenter() { overrideCenterX = centerX; overrideCenterY = centerY; }

    /** Returns the image object of this frame entry. */
    public BufferedImage getFrame()
    {
      return frame;
    }

    /** Assigns a new image object to this frame entry. */
    public void setFrame(BufferedImage image)
    {
      if (image != null) {
        frame = image;
        width = frame.getWidth();
        height = frame.getHeight();
      } else {
        frame = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        width = height = 1;
      }
    }

    @Override
    public Object clone()
    {
      PseudoBamFrameEntry retVal = new PseudoBamFrameEntry(frame, centerX, centerY);
      retVal.overrideCenterX = overrideCenterX;
      retVal.overrideCenterY = overrideCenterY;
      return retVal;
    }

    @Override
    public String toString()
    {
      String s = (String)getOption(PseudoBamDecoder.OPTION_STRING_LABEL);
      if (s != null) {
        return s;
      } else {
        return String.format("Frame@%dx%d", width, height);
      }
    }

    /** Returns all available options by name. */
    public String[] getOptionNames()
    {
      String[] retVal = new String[mapOptions.keySet().size()];
      Iterator<String> iter = mapOptions.keySet().iterator();
      int idx = 0;
      while (iter.hasNext()) {
        retVal[idx++] = iter.next();
      }

      return retVal;
    }

    /** Returns the value of the specified option for this frame. */
    public Object getOption(String name)
    {
      if (name != null) {
        return mapOptions.get(name);
      }
      return null;
    }

    /** Sets a custom option for this frame. */
    public void setOption(String name, Object value)
    {
      if (name != null) {
        mapOptions.put(name, value);
      }
    }
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

    /** Returns all available options by name for the current cycle. */
    public String[] cycleGetOptionNames()
    {
      return cycleGetOptionsNames(currentCycle);
    }

    /** Returns all available options by name for the specified cycle. */
    public String[] cycleGetOptionsNames(int cycleIdx)
    {
      update();
      if (cycleIdx >= 0 && cycleIdx < getDecoder().listCycles.size()) {
        return getDecoder().listCycles.get(cycleIdx).getOptionNames();
      }
      return null;
    }

    /** Returns the specified option associated with the current cycle. */
    public Object cycleGetOption(String name)
    {
      return cycleGetOption(currentCycle, name);
    }

    /** Returns the option associated with the specified cycle. */
    public Object cycleGetOption(int cycleIdx, String name)
    {
      update();
      if (cycleIdx >= 0 && cycleIdx < getDecoder().listCycles.size()) {
        return getDecoder().listCycles.get(cycleIdx).getOption(name);
      }
      return null;
    }

    /** Assigns a custom option to the current cycle. */
    public void cycleSetOption(String name, Object value)
    {
      cycleSetOption(currentCycle, name, value);
    }

    /** Assigns a custom option to the specified cycle. */
    public void cycleSetOption(int cycleIdx, String name, Object value)
    {
      update();
      if (cycleIdx >= 0 && cycleIdx < getDecoder().listCycles.size()) {
        getDecoder().listCycles.get(cycleIdx).setOption(name, value);
      }
    }


    /** Adds a new empty cycle. Returns the index of the added cycle. */
    public int cycleAdd()
    {
      return cycleInsert(getDecoder().listCycles.size(), null);
    }

    /** Adds a new cycle and initializes it with an array of frame indices. Returns the index of the added cycle. */
    public int cycleAdd(int[] indices)
    {
      return cycleInsert(getDecoder().listCycles.size(), indices);
    }

    /** Inserts a new empty cycle at the specified position. */
    public int cycleInsert(int cycleIdx)
    {
      return cycleInsert(cycleIdx, null);
    }

    /**
     * Inserts a new cycle at the specified position and initializes it with an array of frame indices.
     * Returns the index of the inserted cycle. Returns -1 if the cycle could not be inserted.
     */
    public int cycleInsert(int cycleIdx, int[] indices)
    {
      if (cycleIdx >= 0 && cycleIdx <= getDecoder().listCycles.size()) {
        PseudoBamCycleEntry ce = new PseudoBamCycleEntry(indices);
        getDecoder().listCycles.add(cycleIdx, ce);
        update();
        return cycleIdx;
      }
      return -1;
    }

    /** Removes the cycle at the specified position. */
    public void cycleRemove(int cycleIdx)
    {
      cycleRemove(cycleIdx, 1);
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

    /**
     * Moves the current cycle by the specified (positive or negative) offset.
     * @return The new cycle index, or -1 on error.
     */
    public int cycleMove(int offset)
    {
      return cycleMove(currentCycle, offset);
    }

    /**
     * Moves the specified cycle by the specified (positive or negative) offset.
     * @return The new cycle index, or -1 on error.
     */
    public int cycleMove(int cycleIdx, int offset)
    {
      if (cycleIdx >= 0 && cycleIdx < getDecoder().listCycles.size()) {
        int ofsAbs = cycleIdx + offset;
        if (ofsAbs < 0) ofsAbs = 0;
        if (ofsAbs >= getDecoder().listCycles.size()) ofsAbs = getDecoder().listCycles.size() - 1;
        if (ofsAbs != cycleIdx) {
          PseudoBamCycleEntry ce = getDecoder().listCycles.get(cycleIdx);
          getDecoder().listCycles.remove(cycleIdx);
          getDecoder().listCycles.add(ofsAbs, ce);
        }
        return ofsAbs;
      }
      return -1;
    }


    /** Adds frame indices to the specified cycle. */
    public int cycleAddFrames(int cycleIdx, int[] indices)
    {
      if (cycleIdx >= 0 && cycleIdx < getDecoder().listCycles.size()) {
        return cycleInsertFrames(cycleIdx, getDecoder().listCycles.get(cycleIdx).size(), indices);
      }
      return -1;
    }

    /** Inserts frame indices to the cycle at the specified position. */
    public int cycleInsertFrames(int cycleIdx, int pos, int[] indices)
    {
      if (cycleIdx >= 0 && cycleIdx < getDecoder().listCycles.size()) {
        if (getDecoder().listCycles.get(cycleIdx).insert(pos, indices)) {
          update();
          return pos;
        }
      }
      return -1;
    }

    /** Removes one frame index from the cycle at the specified position. */
    public void cycleRemoveFrames(int cycleIdx, int pos)
    {
      cycleRemoveFrames(cycleIdx, pos, 1);
    }

    /** Removes frame indices from the cycle at the specified position. */
    public void cycleRemoveFrames(int cycleIdx, int pos, int count)
    {
      if (cycleIdx >= 0 && cycleIdx < getDecoder().listCycles.size()) {
        getDecoder().listCycles.get(cycleIdx).remove(pos, count);
        update();
      }
    }

    /** Removes all frame indices from the current cycle. */
    public void cycleClearFrames()
    {
      cycleClearFrames(currentCycle);
    }

    /** Removes all frame indices from the specified cycle. */
    public void cycleClearFrames(int cycleIdx)
    {
      if (cycleIdx >= 0 && cycleIdx < getDecoder().listCycles.size()) {
        getDecoder().listCycles.get(cycleIdx).clear();
        update();
      }
    }

    /**
     * Moves the current frame of the current cycle by the specified (positive or negative) offset.
     * Sets the current frame to the new position afterwards.
     * @return The new frame index within the cycle, or -1 on error.
     */
    public int cycleMoveFrame(int offset)
    {
      int pos = cycleMoveFrame(currentCycle, currentFrame, offset);
      if (pos >= 0) {
        currentFrame = pos;
      }
      return pos;
    }

    /**
     * Moves the frame of the current cycle by the specified (positive or negative) offset.
     * @return The new frame index within the cycle, or -1 on error.
     */
    public int cycleMoveFrame(int frameIdx, int offset)
    {
      return cycleMoveFrame(currentCycle, frameIdx, offset);
    }

    /**
     * Moves the frame of the cycle by the specified (positive or negative) offset.
     * @return The new frame index within the cycle, or -1 on error.
     */
    public int cycleMoveFrame(int cycleIdx, int frameIdx, int offset)
    {
      if (cycleIdx >= 0 && cycleIdx < getDecoder().listCycles.size()) {
        PseudoBamCycleEntry ce = getDecoder().listCycles.get(cycleIdx);
        if (frameIdx >= 0 && frameIdx < ce.size()) {
          int ofsAbs = frameIdx + offset;
          if (ofsAbs < 0) ofsAbs = 0;
          if (ofsAbs >= ce.size()) ofsAbs = ce.size() - 1;
          if (ofsAbs != frameIdx) {
            int index = ce.get(frameIdx);
            ce.remove(frameIdx, 1);
            ce.insert(ofsAbs, new int[]{index});
          }
          return ofsAbs;
        }
      }
      return -1;
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


    /** Returns the palette of the current frame in the current cycle. Returns {@code null} if no palette is available. */
    public int[] cycleFrameGetPalette()
    {
      return cycleFrameGetPalette(currentCycle, currentFrame);
    }

    /** Returns the palette of the specified frame in the current cycle. Returns {@code null} if no palette is available. */
    public int[] cycleFrameGetPalette(int frameIdx)
    {
      return cycleFrameGetPalette(currentCycle, frameIdx);
    }

    /** Returns the palette of the frame in the specified cycle. Returns {@code null} if no palette is available. */
    public int[] cycleFrameGetPalette(int cycleIdx, int frameIdx)
    {
      int index = cycleGetFrameIndexAbsolute(cycleIdx, frameIdx);
      if (index >= 0) {
        BufferedImage image = getDecoder().listFrames.get(index).frame;
        if (image != null && image.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
          if (image.getColorModel() instanceof IndexColorModel) {
            IndexColorModel cm = (IndexColorModel)image.getColorModel();
            int[] palette = new int[256];
            int size = Math.min(1 << cm.getPixelSize(), 256);
            for (int i = 0; i < size; i++) {
              palette[i] = (cm.getAlpha(i) << 24) | (cm.getRed(i) << 16) | (cm.getGreen(i) << 8) | cm.getBlue(i);
            }
            return palette;
          }
        }
      }
      return null;
    }

    /**
     * Validates the current cycle configuration. This method should be called whenever changes
     * have been made to the frames and/or cycle structure outside of this control instance.
     */
    public void validate()
    {
      update();
    }

    /** Returns a CycleEntry structure for the specified cycle. */
    public PseudoBamCycleEntry getCycleInfo(int cycleIdx)
    {
      if (cycleIdx >= 0 && cycleIdx < getDecoder().listCycles.size()) {
        return getDecoder().listCycles.get(cycleIdx);
      } else {
        return null;
      }
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
      currentCycle = currentFrame = -1;
      update();
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
      updateSharedBamSize();
    }
  }


  /** Stores information for a single cycle */
  public static class PseudoBamCycleEntry
  {
    private final List<Integer> frames;   // stores abs. frame indices that define this cycle
    private final HashMap<String, Object> mapOptions = new HashMap<>();

    protected PseudoBamCycleEntry(int[] indices)
    {
      frames = new ArrayList<>();
      add(indices);
    }

    /** Returns all available options by name. */
    public String[] getOptionNames()
    {
      String[] retVal = new String[mapOptions.keySet().size()];
      Iterator<String> iter = mapOptions.keySet().iterator();
      int idx = 0;
      while (iter.hasNext()) {
        retVal[idx++] = iter.next();
      }

      return retVal;
    }

    /** Returns the value of the specified option. */
    public Object getOption(String name)
    {
      if (name != null) {
        return mapOptions.get(name);
      }
      return null;
    }

    /** Adds a custom option to this cycle. */
    public void setOption(String name, Object value)
    {
      if (name != null) {
        mapOptions.put(name, value);
      }
    }

    /** Returns the number of stored frame indices. */
    public int size()
    {
      return frames.size();
    }

    /** Returns the frame index at specified position. Returns -1 on error. */
    public int get(int pos)
    {
      if (pos >= 0 && pos < frames.size()) {
        return frames.get(pos).intValue();
      } else {
        return -1;
      }
    }

    /** Replaces the frame index value at the specified position. Note: Does not validate frameIdx! */
    public void set(int pos, int frameIdx)
    {
      if (pos >= 0 && pos < frames.size()) {
        frames.set(pos, frameIdx);
      }
    }

    /** Removes all frame indices. */
    public void clear()
    {
      frames.clear();
    }

    /** Appends specified indices to list. */
    public void add(int[] indices)
    {
      insert(frames.size(), indices);
    }

    /** Inserts indices at specified position. */
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

    /** Removes count indices at specified position. */
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

    @Override
    public String toString()
    {
      StringBuilder sb = new StringBuilder("[");
      for (int i = 0; i < frames.size(); i++) {
        sb.append(Integer.toString(frames.get(i)));
        if (i < frames.size() - 1) {
          sb.append(", ");
        }
      }
      sb.append("]");
      return sb.toString();
    }
  }


  // Storage for BAM v2 frame data blocks
  private static class FrameDataV2
  {
    public int page, sx, sy, width, height, dx, dy;

    public FrameDataV2(int page, int sx, int sy, int width, int height, int dx, int dy)
    {
      this.page = page;
      this.sx = sx;
      this.sy = sy;
      this.width = width;
      this.height = height;
      this.dx = dx;
      this.dy = dy;
    }

    @Override
    public int hashCode()
    {
      int hash = 7;
      hash = 31 * hash + page;
      hash = 31 * hash + sx;
      hash = 31 * hash + sy;
      hash = 31 * hash + width;
      hash = 31 * hash + height;
      hash = 31 * hash + dx;
      hash = 31 * hash + dy;
      return hash;
    }

    @Override
    public boolean equals(Object o)
    {
      if (o instanceof FrameDataV2) {
        FrameDataV2 fd = (FrameDataV2)o;
        return (fd.page == page && fd.sx == sx && fd.sy == sy && fd.width == width &&
                fd.height == height && fd.dx == dx && fd.dy == dy);
      }
      return false;
    }
  }
}
