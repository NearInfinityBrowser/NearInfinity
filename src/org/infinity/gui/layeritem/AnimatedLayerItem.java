// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.layeritem;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import org.infinity.gui.RenderCanvas;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.viewer.AbstractAnimationProvider;
import org.infinity.resource.are.viewer.ViewerConstants;
import org.infinity.resource.graphics.ColorConvert;

/**
 * Represents a game resource structure visually as a bitmap animation.
 */
public class AnimatedLayerItem extends AbstractLayerItem
    implements LayerItemListener, ActionListener, PropertyChangeListener
{
  private static final Color TRANSPARENT_COLOR = new Color(0, true);

  private final FrameInfo[] frameInfos = {new FrameInfo(), new FrameInfo()};

  private BasicAnimationProvider animation;
  private boolean isAutoPlay;
  private Timer timer;
  private Object interpolationType;
  private boolean forcedInterpolation;
  private double zoomFactor;
  private Rectangle frameBounds;    // Point(x,y) defines the point of origin for the animation graphics
  private RenderCanvas rcCanvas;    // Renders both the animation graphics and an optional frame
  private SwingWorker<Void, Void> workerAnimate;

  /**
   * Initialize object with an associated Viewable, an additional text message
   * and an array of Frame object containing graphics data and frame centers.
   *
   * @param viewable Associated Viewable object
   * @param tooltip A short text message shown as tooltip or menu item text
   * @param anim An array of Frame objects defining the animation for this layer item
   */
  public AnimatedLayerItem(Viewable viewable, String tooltip, BasicAnimationProvider anim)
  {
    super(viewable, tooltip);
    init();
    initAnimation(anim);
  }

  /**
   * Returns the currently assigned animation provider.
   */
  public BasicAnimationProvider getAnimation()
  {
    return animation;
  }

  /**
   * Assigns a new animation to the layer item.
   */
  public void setAnimation(BasicAnimationProvider anim)
  {
    initAnimation(anim);
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
  public Object getInterpolationType()
  {
    return interpolationType;
  }

  /**
   * Specifies the interpolation type used for scaled items.
   * @param type One of the TYPE_xxx constants.
   */
  public void setInterpolationType(Object type)
  {
    if (this.interpolationType != type) {
      if (type == ViewerConstants.TYPE_NEAREST_NEIGHBOR ||
          type == ViewerConstants.TYPE_BILINEAR ||
          type == ViewerConstants.TYPE_BICUBIC) {
        this.interpolationType = type;
        if (forcedInterpolation) {
          rcCanvas.setInterpolationType(interpolationType);
        }
        updateFrame();
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
   * @param set If {@code true}, uses a predefined interpolation type only.
   *            If {@code false}, chooses an interpolation type automatically.
   */
  public void setForcedInterpolation(boolean set)
  {
    if (forcedInterpolation != set) {
      forcedInterpolation = set;
      updateFrame();
    }
  }

  /**
   * Returns the {@link Composite} object assigned to the canvas.
   */
  public Composite getComposite()
  {
    return rcCanvas.getComposite();
  }

  /**
   * Sets the {@link Composite} object for the canvas.
   */
  public void setComposite(Composite comp)
  {
    rcCanvas.setComposite(comp);
  }

  /**
   * Returns whether the animation will automatically restart after playing the last frame.
   * (Note: Merely returns the value provided by the attached BasicAnimationProvider object.)
   */
  public boolean isLooping()
  {
    return animation.isLooping();
  }

  public boolean isAutoPlay()
  {
    return isAutoPlay;
  }

  /**
   * Sets whether the layer item will start playing automatically when the item becomes visible.
   */
  public void setAutoPlay(boolean set)
  {
    if (set != isAutoPlay) {
      isAutoPlay = set;
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
      animation.resetFrame();
      updateDisplay(false);
    }
  }

  /**
   * Returns whether a frame is drawn around the item in the specified state.
   */
  public boolean isFrameEnabled(ItemState state)
  {
    return frameInfos[state.ordinal()].isEnabled();
  }

  /**
   * Enables/disables the frame around the item in the specified state.
   */
  public void setFrameEnabled(ItemState state, boolean enabled)
  {
    frameInfos[state.ordinal()].setEnabled(enabled);
    updateFrame();
  }

  /**
   * Returns the frame width in pixels in the specified state.
   */
  public int getFrameWidth(ItemState state)
  {
    return (int)frameInfos[state.ordinal()].getStroke().getLineWidth();
  }

  /**
   * Defines the frame width in pixels in the specified state.
   */
  public void setFrameWidth(ItemState state, int width)
  {
    frameInfos[state.ordinal()].setStroke(new BasicStroke(width < 1 ? 1 : width));
    updateFrame();
  }

  /**
   * Returns the color used for the frame around the item in the specified state.
   */
  public Color getFrameColor(ItemState state)
  {
    return frameInfos[state.ordinal()].getColor();
  }

  /**
   * Defines the color of the frame around the item in the specified state.
   */
  public void setFrameColor(ItemState state, Color color)
  {
    frameInfos[state.ordinal()].setColor(color);
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

  @Override
  public void layerItemChanged(LayerItemEvent event)
  {
    if (event.getSource() == this) {
      updateDisplay(false);
    }
  }

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == timer) {
      // advancing frame by one
      if (!animation.advanceFrame()) {
        if (animation.isLooping()) {
          animation.resetFrame();
        } else {
          stop();
          return;
        }
      }

      // Important: making sure that only ONE instance is running at a time to avoid GUI freezes
      if (workerAnimate == null) {
        workerAnimate = new SwingWorker<Void, Void>() {
          @Override
          protected Void doInBackground() throws Exception {
            updateFrame();
            return null;
          }
        };
        workerAnimate.addPropertyChangeListener(this);
        workerAnimate.execute();
      }
    }
  }

  @Override
  public void propertyChange(PropertyChangeEvent event)
  {
    if (event.getSource() == workerAnimate) {
      if ("state".equals(event.getPropertyName()) &&
              SwingWorker.StateValue.DONE == event.getNewValue()) {
        // Important: making sure that only ONE instance is running at a time to avoid GUI freezes
        workerAnimate = null;
      }
    }
  }

  @Override
  public void repaint()
  {
    updateCanvas();
    super.repaint();
  }

  @Override
  protected boolean isMouseOver(Point pt)
  {
    Rectangle r = new Rectangle(getCanvasBounds(true));
    r.translate(-r.x, -r.y);
    return r.contains(pt);
  }

  /**
   * Calculates a rectangle big enough to fit the current frame image and border into.
   * Returns whether the canvas size changed.
   */
  private void updateCanvasSize()
  {
    int strokeWidth = (int)Math.max(getFrameInfo(false).getStroke().getLineWidth(),
        getFrameInfo(true).getStroke().getLineWidth());

    if (frameBounds == null) {
      frameBounds = new Rectangle(-strokeWidth, -strokeWidth, 2*strokeWidth, 2*strokeWidth);
    } else {
      frameBounds.x = frameBounds.y = -strokeWidth;
      frameBounds.width = frameBounds.height = 2*strokeWidth;
    }

    frameBounds.width += animation.getImage().getWidth(null);
    frameBounds.height += animation.getImage().getHeight(null);

    if (rcCanvas.getImage() == null ||
        rcCanvas.getImage().getWidth(null) != frameBounds.width ||
        rcCanvas.getImage().getHeight(null) != frameBounds.height) {
      rcCanvas.setImage(ColorConvert.createCompatibleImage(frameBounds.width, frameBounds.height, true));
    }
  }

  private Rectangle getCanvasBounds(boolean scaled)
  {
    if (frameBounds == null) {
      updateCanvasSize();
    }
    if (scaled) {
      return new Rectangle((int)((double)frameBounds.x*zoomFactor),
                           (int)((double)frameBounds.y*zoomFactor),
                           (int)((double)frameBounds.width*zoomFactor),
                           (int)((double)frameBounds.height*zoomFactor));
    } else {
      return frameBounds;
    }
  }

  /** Returns the FrameInfo object of the specified state. */
  private FrameInfo getFrameInfo(boolean highlighted)
  {
    return highlighted ? frameInfos[1] : frameInfos[0];
  }

  /** First-time initializations. */
  private void init()
  {
    setLayout(new BorderLayout());
    isAutoPlay = false;
    zoomFactor = 1.0;
    interpolationType = ViewerConstants.TYPE_NEAREST_NEIGHBOR;
    forcedInterpolation = false;

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

  /** Animation-related initializations (requires this.frame to be initialized). */
  private void initAnimation(BasicAnimationProvider anim)
  {
    boolean isPlaying = isPlaying();
    stop();

    if (anim != null) {
      animation = anim;
    } else {
      if (!(animation instanceof AbstractAnimationProvider.DefaultAnimationProvider)) {
        animation = AbstractAnimationProvider.DEFAULT_ANIMATION_PROVIDER;
      }
    }

    updateAnimation();

    if (isPlaying) {
      play();
    }
  }

  /** Call whenever the behavior of the current animation changes. */
  private void updateAnimation()
  {
    boolean isPlaying = isPlaying();
    pause();

    updateCanvasSize();

    if (isPlaying) {
      play();
    } else {
      updateFrame();
    }
  }

  /** Updates the display if needed. */
  private void updateDisplay(boolean force)
  {
    if (!isPlaying() || force) {
      repaint();
    }
  }

  /** Updates both frame content and position. */
  private void updateFrame()
  {
    updateSize();
    updatePosition();
    repaint();
  }

  /** Draws the current frame onto the canvas. */
  private synchronized void updateCanvas()
  {
    boolean isHighlighted = (getItemState() == ItemState.HIGHLIGHTED);

    Graphics2D g2 = (Graphics2D)rcCanvas.getImage().getGraphics();
    try {
      g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
      g2.setColor(TRANSPARENT_COLOR);
      g2.fillRect(0, 0, rcCanvas.getImage().getWidth(null), rcCanvas.getImage().getHeight(null));

      // drawing animation graphics
      g2.drawImage(animation.getImage(), -frameBounds.x, -frameBounds.y, null);

      // drawing frame
      FrameInfo fi = getFrameInfo(isHighlighted);
      if (fi.isEnabled()) {
        g2.setColor(fi.getColor());
        g2.setStroke(fi.getStroke());
        int penWidth2 = (int)fi.getStroke().getLineWidth() >>> 1;
        int penWidthExtra = (int)fi.getStroke().getLineWidth() & 1;
        g2.drawRect(penWidth2, penWidth2,
                    frameBounds.width - penWidth2 - penWidthExtra - 1,
                    frameBounds.height - penWidth2 - penWidthExtra- 1);
      }
    } finally {
      g2.dispose();
      g2 = null;
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

  /** Updates the component position based on the current frame's center. Takes zoom factor into account. */
  private void updatePosition()
  {
    Rectangle bounds = getCanvasBounds(true);
    Point curOfs = new Point(-bounds.x, -bounds.y);
    // applying animation offsets
    curOfs.x -= (int)((double)animation.getLocationOffset().x*getZoomFactor());
    curOfs.y -= (int)((double)animation.getLocationOffset().y*getZoomFactor());
    if (!getLocationOffset().equals(curOfs)) {
      Point distance = new Point(getLocationOffset().x - curOfs.x - 1, getLocationOffset().y - curOfs.y - 1);
      setLocationOffset(curOfs);
      Point loc = super.getLocation();
      setLocation(loc.x + distance.x, loc.y + distance.y);
    }
  }

//----------------------------- INNER CLASSES -----------------------------

  /** Stores information about frames around the item. */
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
