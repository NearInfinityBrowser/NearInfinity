// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui.layeritem;

import infinity.gui.RenderCanvas;
import infinity.resource.Viewable;
import infinity.resource.are.viewer.TilesetRenderer;
import infinity.resource.are.viewer.ViewerConstants;
import infinity.resource.graphics.ColorConvert;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 * Represents a game resource structure visually as a bitmap animation.
 * @author argent77
 */
/*
 * TODO: Add proper support for mirrored animations (mirrored on Y axis)
 * - sprites are already mirrored correctly
 * - center position has yet to be mirrored
 * Example map using mirrored animationsL AR3016
 */
public class AnimatedLayerItem extends AbstractLayerItem implements LayerItemListener, ActionListener
{
  // lookup table for alpha transparency on blended animations
  private static final int[] tableAlpha = new int[256];
  static {
    for (int i = 0; i < tableAlpha.length; i++) {
      tableAlpha[i] = (int)(Math.pow((double)i / 255.0, 0.5) * 256.0);
    }
  }

  private final FrameInfo[] frameInfos = new FrameInfo[]{new FrameInfo(), new FrameInfo()};

  private Frame[] frames;
  private boolean isBlended, isMirrored, isLooping, isAuto, forcedInterpolation, isSelfIlluminated;
  private Timer timer;
  private int curFrame;
  private int interpolationType;
  private double zoomFactor;
  private int lighting;
  private Rectangle canvasBounds;   // Rectangle.[x,y] points to frame center [0,0]
  private RenderCanvas rcCanvas;

  /**
   * Initialize object with default settings.
   */
  public AnimatedLayerItem()
  {
    this(null, null, null, null);
  }

  /**
   * Initialize object with the specified map location.
   * @param location Map location
   */
  public AnimatedLayerItem(Point location)
  {
    this(location, null, null, null);
  }

  /**
   * Initialize object with a specific map location and an associated viewable object.
   * @param location Map location
   * @param viewable Associated Viewable object
   */
  public AnimatedLayerItem(Point location, Viewable viewable)
  {
    this(location, viewable, null, null);
  }

  /**
   * Initialize object with a specific map location, associated Viewable and an additional text message.
   * @param location Map location
   * @param viewable Associated Viewable object
   * @param msg An arbitrary text message
   */
  public AnimatedLayerItem(Point location, Viewable viewable, String msg)
  {
    this(location, viewable, msg, null);
  }

  /**
   * Initialize object with a specific map location, associated Viewable, an additional text message
   * and an array of Frame object containing graphics data and frame centers.
   * @param location Map location
   * @param viewable Associated Viewable object
   * @param msg An arbitrary text message
   * @param frames An array of Frame objects defining the animation for this layer item
   */
  public AnimatedLayerItem(Point location, Viewable viewable, String msg, Frame[] frames)
  {
    super(location, viewable, msg);
    init();
    initAnimation(frames);
  }

  /**
   * Returns the currently assigned animation.
   * @return The animation as an array of Frame objects.
   */
  public Frame[] getAnimation()
  {
    return frames;
  }

  /**
   * Assigns a new animation to the layer item.
   * @param frames An array of Frame objects defining the animation for this layer item.
   */
  public void setAnimation(Frame[] frames)
  {
    initAnimation(frames);
  }

  /**
   * Returns the currently defined frame rate for the animation. The returned value is only an
   * approximation of the frame rate defined in {@link #setFrameRate(double)}, as the timer
   * resolution is limited to 1 ms.
   * @return Frame rate in frames/second.
   */
  public double getFrameRate()
  {
    double delay = (double)timer.getDelay();
    if (delay > 0.0) {
      return 1000.0 / delay;
    } else {
      return 0.0;
    }
  }

  /**
   * Sets the frame rate of the animation in frames/second.
   * @param framesPerSecond The desired frame rate in the range [1.0, 60.0].
   */
  public void setFrameRate(double framesPerSecond)
  {
    if (framesPerSecond < 1.0) framesPerSecond = 1.0; else if (framesPerSecond > 60.0) framesPerSecond = 60.0;
    int delay = (int)(1000.0 / framesPerSecond);
    timer.setDelay(delay);
  }

  /**
   * Returns a single Frame object of the currently assigned animation.
   * @param index Index of the frame.
   * @return A Frame object of a single animation frame, or <code>null</code> on error.
   */
  public Frame getFrame(int index)
  {
    if (index >= 0 && index < frames.length) {
      return frames[index];
    } else {
      return null;
    }
  }

  /**
   * Returns the total number of frames of the animation sequence.
   * @return
   */
  public int getFrameCount()
  {
    return frames.length;
  }

  public int getCurrentFrame()
  {
    return curFrame;
  }

  /**
   * Manually sets the current frame.
   */
  public void setCurrentFrame(int frameIdx)
  {
    if (frames.length > 0) {
      frameIdx %= frames.length;
      if (frameIdx != curFrame) {
        curFrame = frameIdx;
        updateCanvas();
        updateDisplay(false);
      }
    }
  }

  /**
   * Returns the currently used zoom factor for this layer item.
   * @return Zoom factor
   */
  public double getZoomFactor()
  {
    return zoomFactor;
  }

  /**
   * Defines a new zoom factor for this layer item.
   * @param zoomFactor The new zoom factor.
   */
  public void setZoomFactor(double zoomFactor)
  {
    if (this.zoomFactor != zoomFactor) {
      this.zoomFactor = zoomFactor;
      updateSize();
      updatePosition();
    }
  }

  /**
   * Returns the currently used interpolation type.
   */
  public int getInterpolationType()
  {
    return interpolationType;
  }

  /**
   * Specifies the interpolation type used for scaled items.
   * @param type One of the TYPE_xxx constants.
   */
  public void setInterpolationType(int type)
  {
    if (this.interpolationType != type) {
      switch (type) {
        case ViewerConstants.TYPE_NEAREST_NEIGHBOR:
        case ViewerConstants.TYPE_BILINEAR:
        case ViewerConstants.TYPE_BICUBIC:
          this.interpolationType = type;
          break;
        default:
          return;
      }
      if (forcedInterpolation) {
        rcCanvas.setInterpolationType(interpolationType);
      }
    }
  }

  /**
   * Returns whether the renderer is forced to use the predefined interpolation type on scaling.
   * @return
   */
  public boolean isForcedInterpolation()
  {
    return forcedInterpolation;
  }

  /**
   * Specifies whether the renderer uses the best interpolation type based on the current zoom factor
   * or uses a predefined interpolation type only.
   * @param set If <code>true</code>, uses a predefined interpolation type only.
   *            If <code>false</code>, chooses an interpolation type automatically.
   */
  public void setForcedInterpolation(boolean set)
  {
    if (forcedInterpolation != set) {
      forcedInterpolation = set;
      updateCanvas();
      updateDisplay(false);
    }
  }

  /**
   * Returns whether the animation uses brightness as alpha transparency.
   */
  public boolean isBlended()
  {
    return isBlended;
  }

  /**
   * Sets whether the animation uses its brightness as alpha transparency.
   */
  public void setBlended(boolean set)
  {
    if (set != isBlended) {
      isBlended = set;
      updateCanvas();
      updateDisplay(false);
    }
  }

  /**
   * Returns whether the animation is drawn mirrored on the x axis.
   */
  public boolean isMirrored()
  {
    return isMirrored;
  }

  /**
   * Sets whether the animation is mirrored on the x axis.
   */
  public void setMirrored(boolean set)
  {
    if (set != isMirrored) {
      isMirrored = set;

      // updating center positions of all frames
      for (int i = 0; i < frames.length; i++) {
        Frame frame = frames[i];
        for (int j = 0; j < frame.getCount(); j++) {
          frame.getCenter(j).x = frame.getImage(j).getWidth() - frame.getCenter(j).x - 1;
        }
      }

      updateAnimation();
    }
  }

  public boolean isSelfIlluminated()
  {
    return isSelfIlluminated;
  }

  public void setSelfIlluminated(boolean set)
  {
    if (set != isSelfIlluminated) {
      isSelfIlluminated = set;
      updateCanvas();
      updateDisplay(false);
    }
  }

  /**
   * Returns the lighting condition of the animation.
   */
  public int getLighting()
  {
    return lighting;
  }

  /**
   * Defines a new lighting condition for the animation. Does nothing if the animation is
   * self-illuminated.
   */
  public void setLighting(int state)
  {
    switch (state) {
      case ViewerConstants.LIGHTING_DAY:
      case ViewerConstants.LIGHTING_TWILIGHT:
      case ViewerConstants.LIGHTING_NIGHT:
        if (state != lighting) {
          lighting = state;
          updateDisplay(false);
        }
        break;
    }
  }

  /**
   * Returns whether the animation will automatically restart after playing the last frame.
   */
  public boolean isLooping()
  {
    return isLooping;
  }

  /**
   * Sets whether the animation automatically restarts after playing the last frame.
   */
  public void setLooping(boolean set)
  {
    if (set != isLooping) {
      isLooping = set;
    }
  }

  public boolean isAutoPlay()
  {
    return isAuto;
  }

  /**
   * Sets whether the layer item will start playing automatically when the item becomes visible.
   */
  public void setAutoPlay(boolean set)
  {
    if (set != isAuto) {
      isAuto = set;
      if (isAutoPlay() && isVisible()) {
        play();
      }
    }
  }

  /**
   * Returns whether the animation is playing.
   */
  public boolean isPlaying()
  {
    return timer.isRunning();
  }

  /**
   * Starts playback of the animation.
   */
  public void play()
  {
    if (!isPlaying()) {
      timer.start();
    }
  }

  /**
   * Stops playback of the animation without resetting current frame.
   */
  public void pause()
  {
    if (isPlaying()) {
      timer.stop();
    }
  }

  /**
   * Stops playback of the animation and sets current frame to 0.
   */
  public void stop()
  {
    if (isPlaying()) {
      timer.stop();
      curFrame = 0;
      updateCanvas();
      updateDisplay(false);
    }
  }

  /**
   * Returns whether a frame is drawn around the item in the specified state.
   */
  public boolean isFrameEnabled(ItemState state)
  {
    switch (state) {
      case NORMAL:
        return frameInfos[0].isEnabled();
      case HIGHLIGHTED:
        return frameInfos[0].isEnabled();
      default:
        return false;
    }
  }

  /**
   * Enables/disables the frame around the item in the specified state.
   */
  public void setFrameEnabled(ItemState state, boolean enabled)
  {
    switch (state) {
      case NORMAL:
        frameInfos[0].setEnabled(enabled);
        break;
      case HIGHLIGHTED:
        frameInfos[1].setEnabled(enabled);
        break;
    }
    updateFrame();
  }

  /**
   * Returns the frame width in pixels in the specified state.
   */
  public int getFrameWidth(ItemState state)
  {
    switch (state) {
      case NORMAL:
        return (int)frameInfos[0].getStroke().getLineWidth();
      case HIGHLIGHTED:
        return (int)frameInfos[1].getStroke().getLineWidth();
      default:
        return 0;
    }
  }

  /**
   * Defines the frame width in pixels in the specified state.
   */
  public void setFrameWidth(ItemState state, int width)
  {
    if (width < 1) width = 1;
    switch (state) {
      case NORMAL:
        frameInfos[0].setStroke(new BasicStroke((float)width));
        break;
      case HIGHLIGHTED:
        frameInfos[1].setStroke(new BasicStroke((float)width));
        break;
    }
    updateFrame();
  }

  /**
   * Returns the color used for the frame around the item in the specified state.
   */
  public Color getFrameColor(ItemState state)
  {
    switch (state) {
      case NORMAL:
        return frameInfos[0].getColor();
      case HIGHLIGHTED:
        return frameInfos[1].getColor();
      default:
        return FrameInfo.DefaultColor;
    }
  }

  /**
   * Defines the color of the frame around the item in the specified state.
   */
  public void setFrameColor(ItemState state, Color color)
  {
    switch (state) {
      case NORMAL:
        frameInfos[0].setColor(color);
        break;
      case HIGHLIGHTED:
        frameInfos[1].setColor(color);
        break;
    }
    updateFrame();
  }


  @Override
  public void setVisible(boolean aFlag)
  {
    if (aFlag != isVisible()) {
      if (isAutoPlay()) {
        if (aFlag) { play(); } else { pause(); }
      }
    }
    super.setVisible(aFlag);
  }

//--------------------- Begin Interface LayerItemListener ---------------------

  @Override
  public void layerItemChanged(LayerItemEvent event)
  {
    if (event.getSource() == this) {
      updateCanvas();
      updateDisplay(false);
    }
  }

//--------------------- End Interface LayerItemListener ---------------------

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == timer) {
      if (frames.length > 0) {
        curFrame = (curFrame + 1) % frames.length;
        if (!isLooping && curFrame == 0) {
          timer.stop();
        } else {
          updateFrame();
        }
      }
    }
  }

//--------------------- End Interface ActionListener ---------------------

  @Override
  protected boolean isMouseOver(Point pt)
  {
    Rectangle r = new Rectangle(getCanvasBounds(true));
    r.translate(-r.x, -r.y);
    return r.contains(pt);
  }

  // Calculates a rectangle big enough to fit each frame into. Takes center positions into account.
  // Returns true if the canvas bounds have been changed.
  // To get the top-left corner of the selected frame:
  // left = -canvasBounds.x - frame.center.x
  // top  = -canvasBounds.y - frame.center.y
  private boolean updateCanvasBounds()
  {
    int strokeWidth = (int)Math.max(getFrameInfo(false).getStroke().getLineWidth(),
        getFrameInfo(true).getStroke().getLineWidth());

    if (canvasBounds == null) {
      canvasBounds = new Rectangle(-strokeWidth, -strokeWidth, 2*strokeWidth, 2*strokeWidth);
    }

    Rectangle oldRect = new Rectangle(canvasBounds);
    if (frames != null && frames.length > 0) {
      int x1 = Integer.MAX_VALUE, x2 = Integer.MIN_VALUE;
      int y1 = Integer.MAX_VALUE, y2 = Integer.MIN_VALUE;
      for (int i = 0; i < frames.length; i++) {
        for (int j = 0; j < frames[i].getCount(); j++) {
          // taking zoom factor into account
          x1 = Math.min(x1, -frames[i].getCenter(j).x);
          y1 = Math.min(y1, -frames[i].getCenter(j).y);
          x2 = Math.max(x2, frames[i].getImage(j).getWidth() - frames[i].getCenter(j).x);
          y2 = Math.max(y2, frames[i].getImage(j).getHeight() - frames[i].getCenter(j).y);
        }
      }
      // creating bounding box
      canvasBounds.x = x1 - strokeWidth;
      canvasBounds.y = y1 - strokeWidth;
      canvasBounds.width = x2 - x1 + 1 + 2*strokeWidth;
      canvasBounds.height = y2 - y1 + 1 + 2*strokeWidth;
    } else {
      canvasBounds.x = -strokeWidth;
      canvasBounds.y = -strokeWidth;
      canvasBounds.width = 2*strokeWidth;
      canvasBounds.height = 2*strokeWidth;
    }

    return !oldRect.equals(canvasBounds);
  }

  private Rectangle getCanvasBounds(boolean scaled)
  {
    if (scaled) {
      return new Rectangle((int)((double)canvasBounds.x*zoomFactor),
                           (int)((double)canvasBounds.y*zoomFactor),
                           (int)((double)canvasBounds.width*zoomFactor),
                           (int)((double)canvasBounds.height*zoomFactor));
    } else {
      return canvasBounds;
    }
  }

  // Returns the center position of the specified frame(s). Takes zoom factor into account.
  private Point getFrameImageCenter(int frameIdx, int imageIdx, boolean scaled)
  {
    if (frameIdx >= 0 && frameIdx < frames.length &&
        imageIdx >= 0 &&  imageIdx < frames[frameIdx].getCount()) {
      if (scaled) {
        int x = (int)(frames[frameIdx].getCenter(imageIdx).x * zoomFactor);
        int y = (int)(frames[frameIdx].getCenter(imageIdx).y * zoomFactor);
        return new Point(x, y);
      } else {
        return frames[frameIdx].getCenter(imageIdx);
      }
    } else {
      return new Point();
    }
  }

  // Returns the FrameInfo object of the specified state
  private FrameInfo getFrameInfo(boolean highlighted)
  {
    return highlighted ? frameInfos[1] : frameInfos[0];
  }

  // First-time initializations
  private void init()
  {
    setLayout(new BorderLayout());
    isBlended = false;
    isMirrored = false;
    isSelfIlluminated = false;
    isLooping = false;
    isAuto = false;
    zoomFactor = 1.0;
    lighting = ViewerConstants.LIGHTING_DAY;
    interpolationType = ViewerConstants.TYPE_NEAREST_NEIGHBOR;
    forcedInterpolation = false;
    curFrame = 0;

    if (timer == null) {
      timer = new Timer(1000 / 15, this);
    }

    if (rcCanvas == null) {
      rcCanvas = new RenderCanvas();
      rcCanvas.setHorizontalAlignment(SwingConstants.CENTER);
      rcCanvas.setVerticalAlignment(SwingConstants.CENTER);
      rcCanvas.setScalingEnabled(true);
      add(rcCanvas, BorderLayout.CENTER);
    }

    addLayerItemListener(this);
  }

  // Animation-related initializations (requires this.frame to be initialized)
  private void initAnimation(Frame[] frames)
  {
    boolean isPlaying = isPlaying();
    stop();

    // initializing new list of frames
    this.frames = (frames != null) ? frames : new Frame[0];
    for (int i = 0; i < this.frames.length; i++) {
      if (this.frames[i] == null) {
        this.frames[i] = new Frame();
      }
    }

    updateAnimation();

    if (isPlaying) {
      play();
    }
  }

  // Call whenever the behavior of the current animation changes
  private void updateAnimation()
  {
    boolean isPlaying = isPlaying();
    pause();

    if (updateCanvasBounds()) {
      BufferedImage image = new BufferedImage(getCanvasBounds(false).width,
                                              getCanvasBounds(false).height,
                                              BufferedImage.TYPE_INT_ARGB);
      rcCanvas.setImage(image);
    }

    if (isPlaying) {
      play();
    } else {
      updateFrame();
    }
  }

  // Updates both frame content and position. Use this to display the current frame.
  private void updateFrame()
  {
    updateCanvas();
    updateSize();
    updatePosition();
    repaint();
  }

  // Draws the current frame onto the canvas
  private synchronized void updateCanvas()
  {
    boolean isHighlighted = (getItemState() == ItemState.HIGHLIGHTED);

    // used throughout calculations for optimization purposes
    final int shiftFactor24 = 24;
    final int shiftFactor16 = 16;
    final int shiftFactor8  = 8;
    final int alphaScale = 65793;

    // For converting color into luma value
    // Weighting: 0.299*r + 0.587*g + 0.114*b, using factor 655.36 for faster calculations
    final int lumaR = 19595, lumaG = 38470, lumaB = 7471;

    if (curFrame >= 0 && curFrame < frames.length && canvasBounds.width > 0 && canvasBounds.height > 0) {
      BufferedImage dstImage = ColorConvert.toBufferedImage(rcCanvas.getImage(), true);
      int[] dest = ((DataBufferInt)dstImage.getRaster().getDataBuffer()).getData();
      Arrays.fill(dest, 0);
      int dstWidth = dstImage.getWidth();

      int baseAlpha = frames[curFrame].getBaseAlpha()*alphaScale;   // scaled up for faster calculation

      for (int imageIdx = 0; imageIdx < frames[curFrame].getCount(); imageIdx++) {
        BufferedImage srcImage = frames[curFrame].getImage(imageIdx);
        int[] source = ((DataBufferInt)srcImage.getRaster().getDataBuffer()).getData();
        int srcWidth = srcImage.getWidth();
        int srcHeight = srcImage.getHeight();

        Point center = getFrameImageCenter(curFrame, imageIdx, false);
        Rectangle bounds = getCanvasBounds(false);
        int left = -bounds.x - center.x;  // left-most pixel of the sprite on the canvas
        int top = -bounds.y - center.y;   // top-most pixel of the sprite on the canvas
        int srcOfs = 0;
        int dstOfs = top*dstWidth + left;
        int sa, da, r, g, b;
        for (int y = 0; y < srcHeight; y++, srcOfs += srcWidth, dstOfs += dstWidth) {
          for (int x = 0; x < srcWidth; x++) {
            int srcPixel = isMirrored ? source[srcOfs + srcWidth - x - 1] : source[srcOfs+x];
            sa = (srcPixel >>> 24) & 0xff;
            r = (srcPixel >>> 16) & 0xff;
            g = (srcPixel >>> 8) & 0xff;
            b = srcPixel & 0xff;
            if (isBlended) {
              // calculating alpha value (combo of luma and lookup table)
              if (sa > 0) {
                da = tableAlpha[((r*lumaR) + (g*lumaG) + (b*lumaB)) >>> shiftFactor16];
                da = (sa*da) >>> shiftFactor8;
              } else {
                da = 0;
              }
            } else {
              da = sa;
            }
            da = (da*baseAlpha) >>> shiftFactor24;

            if (!isSelfIlluminated) {
              // applying lighting conditions
              r = (r * TilesetRenderer.LightingAdjustment[lighting][0]) >>> TilesetRenderer.LightingAdjustmentShift;
              g = (g * TilesetRenderer.LightingAdjustment[lighting][1]) >>> TilesetRenderer.LightingAdjustmentShift;
              b = (b * TilesetRenderer.LightingAdjustment[lighting][2]) >>> TilesetRenderer.LightingAdjustmentShift;
            }

            if (da > 255) da = 255;
            if (r > 255) r = 255;
            if (g > 255) g = 255;
            if (b > 255) b = 255;

            dest[dstOfs+x] = (da << 24) | (r << 16) | (g << 8) | b;
          }
        }
        source = null;
      }
      dest = null;
      dstImage.flush();

      FrameInfo fi = getFrameInfo(isHighlighted);
      if (fi.isEnabled()) {
        Graphics2D g2d = (Graphics2D)dstImage.getGraphics();
        g2d.setColor(fi.getColor());
        g2d.setStroke(fi.getStroke());
        int penWidth2 = (int)fi.getStroke().getLineWidth() >>> 1;
        int penWidthExtra = (int)fi.getStroke().getLineWidth() & 1;
        g2d.drawRect(penWidth2, penWidth2,
                     dstImage.getWidth() - penWidth2 - penWidthExtra - 1,
                     dstImage.getHeight() - penWidth2 - penWidthExtra- 1);
        g2d.dispose();
      }
    }
  }

  private void updateSize()
  {
    Rectangle r = getBounds();
    Image img = rcCanvas.getImage();
    if (img != null) {
      r.width = (int)((double)img.getWidth(null)*getZoomFactor());
      r.height = (int)((double)img.getHeight(null)*getZoomFactor());
    } else {
      r.width = r.height = 1;
    }
    setPreferredSize(r.getSize());
    setMinimumSize(r.getSize());
    setSize(r.getSize());

    if (forcedInterpolation) {
      rcCanvas.setInterpolationType(interpolationType);
    } else {
      rcCanvas.setInterpolationType((zoomFactor < 1.0) ? RenderCanvas.TYPE_BILINEAR : RenderCanvas.TYPE_NEAREST_NEIGHBOR);
    }
  }

  // Updates the component position based on the current frame's center. Takes zoom factor into account.
  private void updatePosition()
  {
    Rectangle bounds = getCanvasBounds(true);
    Point curOfs = new Point(-bounds.x, -bounds.y);
    if (!getLocationOffset().equals(curOfs)) {
      Point distance = new Point(getLocationOffset().x - curOfs.x - 1, getLocationOffset().y - curOfs.y - 1);
      setLocationOffset(curOfs);
      Point loc = super.getLocation();
      setLocation(loc.x + distance.x, loc.y + distance.y);
    }
  }

  // Updates the display if needed
  private void updateDisplay(boolean force)
  {
    if (!isPlaying() || force) {
      repaint();
    }
  }

//----------------------------- INNER CLASSES -----------------------------

  /**
   * Defines a graphics/center pair for a single animation frame.
   * @author argent77
   */
  public static class Frame
  {
    private final List<BufferedImage> listImage = new ArrayList<BufferedImage>();
    private final List<Point> listCenter = new ArrayList<Point>();
    private int alpha;

    /**
     * Creates a new Frame object with an empty 1x1 pixel image, centered at [0, 0] and
     * using base alpha transparency = 255.
     */
    public Frame()
    {
      this(new Image[0], new Point[0], 255);
    }

    /**
     * Creates a new Frame object with the specified image (default: empty 1x1 pixel image),
     * center position (default: [0, 0]) and base alpha transparency (default: 255).
     */
    public Frame(Image image, Point center, int baseAlpha)
    {
      this(new Image[]{image}, new Point[]{center}, baseAlpha);
    }

    /**
     * Creates a new Frame object with the specified images (default: empty 1x1 pixel image),
     * center positions (default: [0, 0]) and base alpha transparency (default: 255).
     */
    public Frame(Image[] images, Point[] centers, int baseAlpha)
    {
      add(images, centers);
      if (baseAlpha < 0) baseAlpha = 0; else if (baseAlpha > 255) baseAlpha = 255;
      this.alpha = baseAlpha;
    }

    // Returns number of images associated with this frame
    public int getCount()
    {
      return listImage.size();
    }

    public BufferedImage getImage(int idx)
    {
      if (idx >= 0 && idx < listImage.size()) {
        return listImage.get(idx);
      } else {
        return null;
      }
    }

    public Point getCenter(int idx)
    {
      if (idx >= 0 && idx < listCenter.size()) {
        return listCenter.get(idx);
      } else {
        return null;
      }
    }

    public int getBaseAlpha()
    {
      return alpha;
    }

    // Removes all images from the frame
    public void clear()
    {
      listImage.clear();
      listCenter.clear();
    }

    public boolean remove(int idx)
    {
      if (idx >= 0 && idx < listImage.size()) {
        listImage.remove(idx);
        listCenter.remove(idx);
        return true;
      } else {
        return false;
      }
    }

    // adds a new image/center pair to the frame
    public void add(Image image, Point center)
    {
      if (image != null) {
        listImage.add(ColorConvert.toBufferedImage(image, true));
      } else {
        listImage.add(ColorConvert.createCompatibleImage(1, 1, true));
      }

      if (center != null) {
        listCenter.add(center);
      } else {
        listCenter.add(new Point());
      }
    }

    // adds new image/center pairs to the frame
    public void add(Image[] images, Point[] centers)
    {
      int max = Math.min((images != null) ? images.length : 0, (centers != null) ? centers.length : 0);
      for (int i = 0; i < max; i++) {
        add((images != null && images.length > i) ? images[i] : null,
            (centers != null && centers.length > i) ? centers[i] : null);
      }
    }
  }


  // Stores information about frames around the item
  private static class FrameInfo
  {
    private static Color DefaultColor = new Color(0, true);
    private static BasicStroke DefaultStroke = new BasicStroke(1.0f);

    private boolean enabled;
    private Color color;
    private BasicStroke stroke;

    public FrameInfo()
    {
      this(DefaultStroke, DefaultColor, false);
    }

    public FrameInfo(BasicStroke stroke, Color color, boolean enabled)
    {
      setStroke(stroke);
      setColor(color);
      this.enabled = enabled;
    }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled)
    {
      this.enabled = enabled;
    }

    public BasicStroke getStroke() { return stroke; }

    public void setStroke(BasicStroke stroke)
    {
      if (stroke != null) {
        this.stroke = stroke;
      } else {
        this.stroke = DefaultStroke;
      }
    }

    public Color getColor() { return color; }

    public void setColor(Color color)
    {
      if (color == null) {
        color = DefaultColor;
      }
      this.color = color;
    }
  }
}
