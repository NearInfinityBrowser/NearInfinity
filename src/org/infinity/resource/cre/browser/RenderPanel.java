// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.browser;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Objects;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.infinity.gui.RenderCanvas;
import org.infinity.gui.ViewerUtil;
import org.infinity.resource.cre.decoder.SpriteDecoder.SpriteBamControl;
import org.infinity.resource.graphics.ColorConvert;

/**
 * This panel handles drawing background and creature animations.
 */
public class RenderPanel extends JPanel
{
  private static final Color COLOR_TRANSPARENT = new Color(0, true);
  private static final float POS_REL_X = 0.5f;
  private static final float POS_REL_Y = 2.0f / 3.0f;

  private final Listeners listeners = new Listeners();
  private final CreatureBrowser browser;
  // storage for scroll pane view size to track view size changes
  private Dimension viewSize = new Dimension();

  private JPanel pCanvas;
  private RenderCanvas rcCanvas;
  private JScrollPane spCanvas;
  private Image backgroundImage;
  private Color backgroundColor;
  private Point backgroundCenter;
  private Image frame;
  private Rectangle frameBounds;
  private Composite composite;
  private float zoom;
  private boolean isFiltering;
  private boolean backgroundChanged;
  // rectangle (position and size) of the BAM frame
  private Rectangle frameRect;
  // storage for background image content that is overwritten by the animation frame
  private Image frameBackground;

  public RenderPanel(CreatureBrowser browser)
  {
    super();
    this.browser = Objects.requireNonNull(browser);
    init();
  }

  /** Returns the associated {@code CreatureBrowser} instance. */
  public CreatureBrowser getBrowser() { return browser; }

  /** Returns the current background color of the panel. */
  public Color getBackgroundColor()
  {
    return (backgroundColor != null) ? backgroundColor : getDefaultBackgroundColor();
  }

  /** Sets the background color for the panel. */
  public void setBackgroundColor(Color c)
  {
    if (backgroundColor == null && c != null ||
        backgroundColor != null && !backgroundColor.equals(c)) {
      this.backgroundColor = c;
      pCanvas.setBackground(getBackgroundColor());
      backgroundChanged = true;
    }
  }

  /** Returns the background image used by the panel. Returns {@code null} if no image is defined. */
  public Image getBackgroundImage()
  {
    return backgroundImage;
  }

  /** Sets the background image used by the panel. Specify {@code null} if no image should be displayed. */
  public void setBackgroundImage(Image image)
  {
    if (backgroundImage == null && image != null ||
        backgroundImage != null && !backgroundImage.equals(image)) {
      if (backgroundImage != null) {
        backgroundImage.flush();
      }
      backgroundImage = image;
      backgroundChanged = true;

      // clear frame background info
      frameRect = null;
      if (frameBackground != null) {
        frameBackground.flush();
      }
      frameBackground = null;
    }
  }

  /**
   * Returns the center position for the creature animation on the background image.
   * Returns a default center position if no explicit center is defined.
   */
  public Point getBackgroundCenter()
  {
    if (backgroundCenter == null) {
      if (backgroundImage != null) {
        return new Point((int)(backgroundImage.getWidth(null) * POS_REL_X),
                         (int)(backgroundImage.getHeight(null) * POS_REL_Y));
      } else {
        return new Point();
      }
    } else {
      return backgroundCenter;
    }
  }

  /** Sets the center position for the creature animation on the background image. */
  public void setBackgroundCenter(Point pt)
  {
    if (backgroundCenter == null && pt != null ||
        backgroundCenter != null && !backgroundCenter.equals(pt)) {
      backgroundCenter = (pt != null) ? new Point(pt) : null;
    }
  }

  /** Returns the current zoom factor. */
  public float getZoom() { return zoom; }

  /** Sets a new zoom factor. Valid range: [0.01, 10.0] */
  public void setZoom(float zoom) throws IllegalArgumentException
  {
    if (zoom < 0.01f || zoom > 10.0f) {
      throw new IllegalArgumentException("Zoom factor is out of range: " + zoom);
    }
    if (zoom != this.zoom) {
      this.zoom = zoom;
    }
  }

  /** Returns whether bilinear filtering is enabled. */
  public boolean isFilterEnabled()
  {
    return isFiltering;
  }

  /** Sets whether bilinear filtering is enabled {@code true} or nearest neighbor filtering is enabled {@code false}. */
  public void setFilterEnabled(boolean b)
  {
    if (b != isFiltering) {
      isFiltering = b;
      Object type = isFiltering ? RenderCanvas.TYPE_BILINEAR : RenderCanvas.TYPE_NEAREST_NEIGHBOR;
      rcCanvas.setInterpolationType(type);
      updateCanvas();
    }
  }

  /** Returns the {@link Composite} object used to render the creature animation. */
  public Composite getComposite()
  {
    return (composite != null) ? composite : AlphaComposite.SrcOver;
  }

  /**
   * Sets the {@link Composite} object used to render the creature animation.
   * Specify {@code null} to use the default {@code Composite}.
   */
  public void setComposite(Composite c)
  {
    if (composite == null && c != null ||
        composite != null && !composite.equals(c)) {
      composite = c;
      updateCanvas();
    }
  }

  /** Stores the active BAM frame and frame center from the specified {@code PseudoBamControl} object internally for display. */
  public void setFrame(SpriteBamControl ctrl)
  {
    if (ctrl != null) {
      if (frameBounds == null) {
        frameBounds = new Rectangle();
      }
      frameBounds.setSize(ctrl.getSharedDimension());
      frameBounds.setLocation(ctrl.getSharedOrigin());
      if (frame == null ||
          frame.getWidth(null) != frameBounds.width ||
          frame.getHeight(null) != frameBounds.height) {
        frame = ColorConvert.createCompatibleImage(frameBounds.width, frameBounds.height, true);
      } else {
        // clear old content
        Graphics2D g = (Graphics2D)frame.getGraphics();
        try {
          g.setComposite(AlphaComposite.Src);
          g.setColor(COLOR_TRANSPARENT);
          g.fillRect(0, 0, frame.getWidth(null), frame.getHeight(null));
        } finally {
          g.dispose();
        }
      }
      ctrl.cycleGetFrame(frame);
    } else {
      frameBounds = null;
      if (frame != null) {
        frame.flush();
      }
      frame = null;
    }
  }

  /**
   * Attempts to center the viewport on the animation sprite.
   * Does nothing if the whole canvas fits into the viewport.
   */
  public void centerOnSprite()
  {
    Dimension dimExtent = spCanvas.getViewport().getExtentSize();
    Dimension dimCanvas = rcCanvas.getSize();
    if (dimCanvas.width > dimExtent.width ||
        dimCanvas.height > dimExtent.height) {
      // calculating center point
      int x = 0;
      int y = 0;
      if (backgroundCenter != null) {
        x = backgroundCenter.x;
        y = backgroundCenter.y;
      } else if (frameBounds != null) {
        x = -frameBounds.x;
        y = -frameBounds.y;
      }
      if (frameBounds != null) {
        // shift viewport center to frame center
        x += frameBounds.x + frameBounds.width / 2;
        y += frameBounds.y + frameBounds.height / 2;
      }

      x = (int)(x * getZoom());
      y = (int)(y * getZoom());

      // adjusting viewport
      int cx = Math.max(0, x - (dimExtent.width / 2));
      int cy = Math.max(0, y - (dimExtent.height / 2));
      spCanvas.getViewport().setViewPosition(new Point(cx, cy));
    }
  }

  /** Calculates the canvas dimension to fit background image and sprite frame. */
  private Dimension calculateImageDimension()
  {
    Dimension retVal = new Dimension(1, 1);
    if (frameBounds != null) {
      retVal.width = Math.max(retVal.width, frameBounds.width);
      retVal.height = Math.max(retVal.height, frameBounds.height);
    }
    if (backgroundImage != null) {
      retVal.width = Math.max(retVal.width, backgroundImage.getWidth(null));
      retVal.height = Math.max(retVal.height, backgroundImage.getHeight(null));
    }
    return retVal;
  }

  /** Ensures that the image associated with the {@code RenderCanvas} exists and is properly initialized. */
  private void ensureCanvasImage()
  {
    Image img;
    Dimension dim = calculateImageDimension();
    if (rcCanvas.getImage() == null ||
        rcCanvas.getImage().getWidth(null) != dim.width ||
        rcCanvas.getImage().getHeight(null) != dim.height) {
      img = ColorConvert.createCompatibleImage(dim.width, dim.height, true);
      backgroundChanged = true;
    } else {
      img = rcCanvas.getImage();
    }

    if (backgroundChanged) {
      Graphics2D g = (Graphics2D)img.getGraphics();
      try {
        g.setComposite(AlphaComposite.Src);
        int x = (backgroundImage != null) ? (dim.width - backgroundImage.getWidth(null)) / 2 : 0;
        int y = (backgroundImage != null) ? (dim.height - backgroundImage.getHeight(null)) / 2 : 0;
        if (backgroundImage == null || x > 0 || y > 0) {
          g.setColor(getBackgroundColor());
          g.fillRect(0, 0, img.getWidth(null), img.getHeight(null));
        }
        if (backgroundImage != null) {
          g.drawImage(backgroundImage, x, y, null);
        }
      } finally {
        g.dispose();
      }
      rcCanvas.setImage(img);
      frameRect = null;
      frameBackground = null;
      backgroundChanged = false;
    }

    rcCanvas.setImage(img);
  }

  /** Restores the background area overwritten by the previous animation frame. */
  private void restoreFrameBackground()
  {
    if (frameRect != null && frameBackground != null) {
      Graphics2D g = (Graphics2D)rcCanvas.getImage().getGraphics();
      try {
        g.setComposite(AlphaComposite.Src);
        g.drawImage(frameBackground, frameRect.x, frameRect.y, null);
      } finally {
        g.dispose();
      }
    }
  }

  /** Stores the background area to be overwritten by the current animation frame. */
  private void storeFrameBackground()
  {
    if (frameBounds != null) {
      if (frameRect == null) {
        frameRect = new Rectangle();
      }
      int x = (backgroundCenter != null) ? backgroundCenter.x : -frameBounds.x;
      int y = (backgroundCenter != null) ? backgroundCenter.y : -frameBounds.y;
      frameRect.x = x + frameBounds.x;
      frameRect.y = y + frameBounds.y;
      frameRect.width = frameBounds.width;
      frameRect.height = frameBounds.height;
      if (frameBackground == null ||
          frameBackground.getWidth(null) != frameRect.width ||
          frameBackground.getHeight(null) != frameRect.height) {
        frameBackground = ColorConvert.createCompatibleImage(frameRect.width, frameRect.height, true);
      }
      Graphics2D g = (Graphics2D)frameBackground.getGraphics();
      try {
        g.setComposite(AlphaComposite.Src);
        g.drawImage(rcCanvas.getImage(), 0, 0, frameRect.width, frameRect.height,
                    frameRect.x, frameRect.y, frameRect.x + frameRect.width, frameRect.y + frameRect.height,
                    null);
      } finally {
        g.dispose();
      }
    } else {
      frameRect = null;
      if (frameBackground != null) {
        frameBackground.flush();
      }
      frameBackground = null;
    }
  }

  /** Draws the current frame if available. */
  private void drawFrame()
  {
    if (frame != null) {
      int x = (backgroundCenter != null) ? backgroundCenter.x : -frameBounds.x;
      int y = (backgroundCenter != null) ? backgroundCenter.y : -frameBounds.y;

      Graphics2D g = (Graphics2D)rcCanvas.getImage().getGraphics();
      Point pos = new Point(x + frameBounds.x, y + frameBounds.y);
      try {
        // drawing markers
        SpriteBamControl ctrl = getBrowser().getMediaPanel().getController();
        ctrl.getVisualMarkers(g, pos);

        // drawing frame
        g.setComposite(getComposite());
        g.drawImage(frame, x + frameBounds.x, y + frameBounds.y, null);
      } finally {
        g.dispose();
      }
    }
  }

  /** Updates the display with the current background and BAM frame. */
  public void updateCanvas()
  {
    updateCanvas(false);

    // redraw canvas
    repaint();
  }

  /** Internally used to update the canvas control. */
  protected void updateCanvas(boolean restore)
  {
    backgroundChanged |= restore;

    // recreate the canvas image if necessary
    ensureCanvasImage();

    // restore frame rect portion of background if necessary
    restoreFrameBackground();

    // store new frame rect portion of background
    storeFrameBackground();

    // draw frame
    drawFrame();

    // apply zoom factor
    if (rcCanvas.getImage() != null) {
      Dimension dim = new Dimension(rcCanvas.getImage().getWidth(null), rcCanvas.getImage().getHeight(null));
      dim.width *= getZoom();
      dim.height *= getZoom();
      rcCanvas.setPreferredSize(dim);
      rcCanvas.setSize(dim);
    }

    revalidate();
  }

  /** Returns the L&F-specific default background color of the {@code JPanel}. */
  private static Color getDefaultBackgroundColor()
  {
    return UIManager.getColor("Panel.background");
  }

  private void init()
  {
    GridBagConstraints c = new GridBagConstraints();

    pCanvas = new JPanel(new GridBagLayout());
    rcCanvas = new RenderCanvas(null, true);
    rcCanvas.addComponentListener(listeners);

    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                          GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    pCanvas.add(rcCanvas, c);

    spCanvas = new JScrollPane(pCanvas);
    spCanvas.setBorder(pCanvas.getBorder());
    spCanvas.getHorizontalScrollBar().setUnitIncrement(16);
    spCanvas.getVerticalScrollBar().setUnitIncrement(16);
    spCanvas.addComponentListener(listeners);
    spCanvas.getViewport().addChangeListener(listeners);

    setLayout(new BorderLayout());
    add(spCanvas, BorderLayout.CENTER);

    // default settings
    backgroundColor = null;
    backgroundImage = null;
    backgroundCenter = null;
    frame = null;
    frameBounds = null;
    composite = null;
    zoom = 1.0f;
    isFiltering = false;
    frameRect = null;
    frameBackground = null;
  }

//-------------------------- INNER CLASSES --------------------------

  private class Listeners implements ComponentListener, ChangeListener
  {
    public Listeners()
    {
    }

    //--------------------- Begin Interface ComponentListener ---------------------

    @Override
    public void componentResized(ComponentEvent e)
    {
      if (e.getSource() == rcCanvas) {
        // recenter view relative to sprite frame position and dimension
      }
    }

    @Override
    public void componentMoved(ComponentEvent e)
    {
    }

    @Override
    public void componentShown(ComponentEvent e)
    {
    }

    @Override
    public void componentHidden(ComponentEvent e)
    {
    }

    //--------------------- End Interface ComponentListener ---------------------

    //--------------------- Begin Interface ChangeListener ---------------------

    @Override
    public void stateChanged(ChangeEvent e)
    {
      if (e.getSource() == spCanvas.getViewport()) {
        Dimension dimView = spCanvas.getViewport().getViewSize();
        if (!dimView.equals(viewSize)) {
          // recenter view
          viewSize.width = dimView.width;
          viewSize.height = dimView.height;
          centerOnSprite();
        } else {
          viewSize.width = dimView.width;
          viewSize.height = dimView.height;
        }
      }
    }

    //--------------------- End Interface ChangeListener ---------------------

  }
}
