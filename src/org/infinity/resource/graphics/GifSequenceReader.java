// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class GifSequenceReader
{
  /**
   * List of supported frame disposal methods.
   * Unknown or unsupported methods are combined in {@code DoNotDispose}.
   */
  public enum DisposalMethod {
    DoNotDispose,
    Background,
    Previous,
  }

  private static final String metaFormatName = "javax_imageio_gif_image_1.0";
  private static final HashSet<String> metaNodeNames = new HashSet<>();
  static {
    metaNodeNames.add("ImageDescriptor");
    metaNodeNames.add("GraphicControlExtension");
  }

  private final ImageReader gifReader;
  private final List<Frame> frames;
  private final int numFrames;

  /**
   * Creates a new GifSequenceReader.
   *
   * @param inputStream the ImageInputStream to read data from.
   * @throws IOException if no input stream or gif reader is found.
   */
  public GifSequenceReader(ImageInputStream inputStream) throws IOException
  {
    if (inputStream == null) { throw new IOException("No image input stream specified."); }
    frames = new ArrayList<>();
    gifReader = getReader();
    gifReader.setInput(inputStream, false, false);
    numFrames = gifReader.getNumImages(true);
  }

  /**
   * Decodes the next available GIF frame. This and previously decoded frames can be access by
   * {@code getFrame}.
   * @return The Frame object initialized with frame data, or {@code null} if no more frames
   *         are available.
   * @throws Exception When a read or decode error occurred.
   */
  public Frame decode() throws Exception
  {
    Frame retVal = null;
    if (frames.size() < numFrames) {
      int index = frames.size();
      IIOImage ioimg = gifReader.readAll(index, null);
      retVal = initFrame(index, ioimg);
      if (retVal == null) { throw new Exception("Unable to decode GIF frame "+ index); }
      frames.add(retVal);
    }
    return retVal;
  }

  /**
   * Decodes all remaining frames at once. Frames can be accessed afterwards by {@code getFrame}.
   * @return The number of frames decoded by this call.
   * @throws Exception When a read or decode error occurred.
   */
  public int decodeAll() throws Exception
  {
    int count = 0;
    while (decode() != null) {
      count++;
    }
    return count;
  }

  /**
   * Returns the number of decoded GIF frames.
   */
  public int getFrameCount()
  {
    return frames.size();
  }

  /**
   * Returns the decoded GIF frame at the specified index.
   * @param index frame index, starting at 0.
   */
  public Frame getFrame(int index) throws IndexOutOfBoundsException
  {
    return frames.get(index);
  }

  private Frame initFrame(int index, IIOImage ioimg) throws Exception
  {
    if (ioimg == null) { throw new NullPointerException("img is null"); }

    Frame frame = new Frame(index);
    Frame framePrev = (index > 0) ? frames.get(index - 1) : null;
    updateMetadata(frame, ioimg.getMetadata());
    RenderedImage rimg = ioimg.getRenderedImage();
    if (rimg == null) { throw new Exception("No image data found"); }

    // Converting RenderedImage into BufferedImage object
    BufferedImage gifImg = null;
    if (rimg instanceof BufferedImage) {
      gifImg = (BufferedImage)rimg;
    } else {
      ColorModel cm = rimg.getColorModel();
      int width = rimg.getWidth();
      int height = rimg.getHeight();
      WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
      boolean isAlphaPre = cm.isAlphaPremultiplied();
      Hashtable<String, Object> properties = new Hashtable<>();
      String[] keys = rimg.getPropertyNames();
      for (String k: keys) { properties.put(k, rimg.getProperty(k)); }
      gifImg = new BufferedImage(cm, raster, isAlphaPre, properties);
      gifImg.copyData(raster);
    }

    BufferedImage bimg = null;
    Graphics2D g = null;
    if (framePrev != null) {
      bimg = new BufferedImage(framePrev.getRenderedImage().getWidth(),
                               framePrev.getRenderedImage().getHeight(),
                               BufferedImage.TYPE_INT_ARGB);
      DisposalMethod dm = framePrev.getDisposalMethod();

      // Preparing canvas
      g = bimg.createGraphics();
      switch (dm) {
        case DoNotDispose:  // Copy previous frame to current frame
          g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
          g.drawImage(framePrev.getRenderedImage(), null, 0, 0);
          break;
        case Previous:      // Restore content from two frame ago
          if (index > 1) {
            Frame fpp = frames.get(index - 2);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
            g.drawImage(fpp.getRenderedImage(), null, 0, 0);
          }
          break;
        case Background:    // Clear with background color
          Color col = new Color(0, true);
          if (framePrev.getTransparentIndex() >= 0) {
            IndexColorModel cm = (IndexColorModel)framePrev.getImage().getColorModel();
            col = new Color(cm.getRGB(framePrev.getTransparentIndex()), true);
          }
          g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
          g.setColor(col);
          g.fillRect(0, 0, bimg.getWidth(), bimg.getHeight());
          break;
      }
    } else {
      int w = frame.getRect().x + frame.getRect().width;
      int h = frame.getRect().y + frame.getRect().height;
      bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
      g = bimg.createGraphics();
    }

    if (g != null) {
      // Rendering current frame
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
      g.drawImage(gifImg, null, frame.getRect().x, frame.getRect().y);
      g.dispose();
      g = null;
    }

    frame.image = gifImg;
    frame.renderedImage = bimg;
    return frame;
  }

  // Updates the specified Frame object with metadata
  private Frame updateMetadata(Frame frame, IIOMetadata meta)
  {
    if (frame != null && meta != null) {
      Node tree = meta.getAsTree(metaFormatName);
      NodeList children = tree.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        Node child = children.item(i);
        NamedNodeMap attr = child.getAttributes();
        switch (child.getNodeName()) {
          case "ImageDescriptor":
            frame.rect.x = getIntValue(attr.getNamedItem("imageLeftPosition"));
            frame.rect.y = getIntValue(attr.getNamedItem("imageTopPosition"));
            frame.rect.width = getIntValue(attr.getNamedItem("imageWidth"));
            frame.rect.height = getIntValue(attr.getNamedItem("imageHeight"));
            frame.interlaced = getBoolValue(attr.getNamedItem("interlaceFlag"));
            break;
          case "GraphicControlExtension":
            Node n = attr.getNamedItem("disposalMethod");
            if (n != null) {
              switch (n.getNodeValue()) {
                case "restoreToBackgroundColor":
                  frame.disposal = DisposalMethod.Background;
                  break;
                case "restoreToPrevious":
                  frame.disposal = DisposalMethod.Previous;
                  break;
                default:
                  frame.disposal = DisposalMethod.DoNotDispose;
                  break;
              }
            }
            frame.userInput = getBoolValue(attr.getNamedItem("userInputFlag"));
            if (getBoolValue(attr.getNamedItem("transparentColorFlag"))) {
              frame.transIndex = getIntValue(attr.getNamedItem("transparentColorIndex"));
            } else {
              frame.transIndex = -1;
            }
            frame.delay = getIntValue(attr.getNamedItem("transparentColorIndex"));
        }
      }
    }
    return frame;
  }

  private int getIntValue(Node node)
  {
    int retVal = 0;
    if (node != null) {
      try {
        retVal = Integer.parseInt(node.getNodeValue());
      } catch (NumberFormatException e) {
      }
    }
    return retVal;
  }

  private boolean getBoolValue(Node node)
  {
    boolean retVal = false;
    if (node != null) {
      retVal = Boolean.parseBoolean(node.getNodeValue());
    }
    return retVal;
  }

  private static ImageReader getReader() throws IOException
  {
    Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix("gif");
    if (!iter.hasNext()) {
      throw new IOException("No GIF image readers exist");
    } else {
      return iter.next();
    }
  }

//-------------------------- INNER CLASSES --------------------------

  public static class Frame
  {
    private int index;
    private BufferedImage image, renderedImage;
    private Rectangle rect;
    private int delay;
    private DisposalMethod disposal;
    private int transIndex;
    private boolean userInput, interlaced;

    Frame(int index)
    {
      this.index = index;
      this.rect = new Rectangle();
      this.disposal = DisposalMethod.DoNotDispose;
      this.transIndex = -1;
    }

    /** Returns the frame index within the GIF animation sequence. */
    public int getIndex() { return index; }

    /** Returns the unprocessed image of the frame. */
    public BufferedImage getImage() { return image; }

    /** Returns the correctly rendered image relative to neighboring frames. */
    public BufferedImage getRenderedImage() { return renderedImage; }

    /** Returns position and size of the frame relative to global animation size. */
    public Rectangle getRect() { return rect; }

    /** Returns the time to display this frame. */
    public int getDelay() { return delay; }

    /** Returns the disposal method used to prepare for the next frame. */
    public DisposalMethod getDisposalMethod() { return disposal; }

    /** Returns the transparent palette index, or -1 if not present. */
    public int getTransparentIndex() { return transIndex; }

    /** Returns the user input flag. */
    public boolean isUserInput() { return userInput; }

    /** Returns whether frame data is encoded in interlaced mode. */
    public boolean isInterlaced() { return interlaced; }
  }
}
