// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.AlphaComposite;
import java.awt.DisplayMode;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.EventListenerList;

import org.infinity.resource.Closeable;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.cre.decoder.SpriteDecoder;
import org.infinity.resource.cre.decoder.SpriteDecoder.SpriteBamControl;
import org.infinity.resource.cre.decoder.util.AnimationInfo;
import org.infinity.resource.cre.decoder.util.Direction;
import org.infinity.resource.cre.decoder.util.Sequence;
import org.infinity.resource.cre.decoder.util.SpriteUtils;
import org.infinity.resource.graphics.BamDecoder.BamControl;
import org.infinity.resource.graphics.ColorConvert;
import org.infinity.resource.graphics.PseudoBamDecoder.PseudoBamFrameEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.IdsMapCache;
import org.infinity.util.IniMap;
import org.infinity.util.IniMapSection;
import org.infinity.util.Logger;
import org.infinity.util.StringTable;
import org.infinity.util.io.StreamUtils;

/**
 * Expand the {@link JPanel} class to allow creature sprites of the currently opened game moving around the panel and
 * performing random actions.
 */
public class SpriteAnimationPanel extends JPanel
    implements Runnable, Closeable, ActionListener, PropertyChangeListener, WindowStateListener, MouseMotionListener {
  /** Limit sprite updates to 15 fps. */
  private static final long FRAME_DELAY = 1000L / 15L;

  /** Max. number of sprites that will be present at the same time. */
  private static final int MAX_SPRITES = calculateMaxSprites();

  /** Default delay for triggering an event to create a new sprite (lower bound, in ms). */
  private static final int DELAY_CREATE_SPRITE_MIN = 10_000;
  /** Default delay for triggering an event to create a new sprite (upper bound, in ms). */
  private static final int DELAY_CREATE_SPRITE_MAX = 60_000;

  /**
   * Default delay for triggering an event to release an existing sprite (lower bound, in ms).
   * Boundary is opened to allow it to vanish from existence eventually.
   */
  private static final int DELAY_RELEASE_SPRITE_MIN = 30_000;
  /**
   * Default delay for triggering an event to release an existing sprite (upper bound, in ms).
   * Boundary is opened to allow it to vanish from existence eventually.
   */
  private static final int DELAY_RELEASE_SPRITE_MAX = 90_000;

  /** Used for sorting sprites for rendering. */
  private static final Comparator<SpriteInfo> SPRITE_COMPARATOR = Comparator.comparingDouble(a -> a.y);

  /** Set of non-pst action sequences for use. */
  private static final Sequence[] DEF_SEQUENCES = {
      Sequence.ATTACK,
      Sequence.ATTACK_2,
      Sequence.ATTACK_3,
      Sequence.ATTACK_4,
      Sequence.ATTACK_5,
      Sequence.SPELL,
      Sequence.SPELL1,
      Sequence.SPELL2,
      Sequence.SPELL3,
      Sequence.SPELL4,
      Sequence.STANCE,
      Sequence.STANCE2,
      Sequence.STAND,
      Sequence.STAND2,
      Sequence.STAND3,
  };
  /** Set of pst-specific action sequences for use. */
  private static final Sequence[] DEF_SEQUENCES_PST = {
      Sequence.PST_ATTACK1,
      Sequence.PST_ATTACK2,
      Sequence.PST_ATTACK3,
      Sequence.PST_DIE_BACKWARD,
      Sequence.PST_DIE_FORWARD,
      Sequence.PST_DIE_COLLAPSE,
      Sequence.PST_SPELL1,
      Sequence.PST_SPELL2,
      Sequence.PST_SPELL3,
      Sequence.PST_STANCE,
      Sequence.PST_STANCE_FIDGET1,
      Sequence.PST_STANCE_FIDGET2,
      Sequence.PST_STAND,
      Sequence.PST_STAND_FIDGET1,
      Sequence.PST_STAND_FIDGET2,
      Sequence.PST_TALK1,
      Sequence.PST_TALK2,
      Sequence.PST_TALK3,
  };

  /** Collection of active creature sprites, sorted by vertical position to simplify correct drawing order. */
  private final List<SpriteInfo> sprites = Collections.synchronizedList(new ArrayList<>(MAX_SPRITES + 1));
  private final List<SpriteInfo> spritesWorking = Collections.synchronizedList(new ArrayList<>(MAX_SPRITES + 1));

  /** Cached list of CRE resources available for the current game. */
  private final List<ResourceEntry> creResources = new ArrayList<>();

  /** Random numbers provider */
  private final Random random = new Random();

  /** Controls the delay between creating new sprites. */
  private final Timer creationTimer = new Timer(createDelay(random, 0, DELAY_CREATE_SPRITE_MIN), this);

  /** Controls the delay between releasing existing sprites. */
  private final Timer releaseTimer =
      new Timer(createDelay(random, DELAY_RELEASE_SPRITE_MIN, DELAY_RELEASE_SPRITE_MAX), this);

  /** Thread for periodically updating the sprite animations. */
  private Thread runner;

  /** Indicates whether the running thread is actively updating. */
  private boolean running;

  /** Indicates whether the running thread is currently in the paused state. */
  private boolean paused;

  /** Indicates whether the SpriteAnimator instance has been terminated. */
  private boolean closed;

  /** Max. number of sprites that will be present at the same time. */
  private int maxSprites;

  /** Delay for triggering an event to create a new sprite (lower bound, in ms). */
  private int createSpriteDelayMin;
  /** Delay for triggering an event to create a new sprite (upper bound, in ms). */
  private int createSpriteDelayMax;

  /**
   * Default delay for triggering an event to release an existing sprite (lower bound, in ms).
   * Boundary is opened to allow it to vanish from existence eventually.
   */
  private int releaseSpriteDelayMin;
  /**
   * Default delay for triggering an event to release an existing sprite (upper bound, in ms).
   * Boundary is opened to allow it to vanish from existence eventually.
   */
  private int releaseSpriteDelayMax;

  /**
   * Creates a new sprite animation panel with a double buffer and a flow layout. The runner is initially set to the
   * stopped state.
   */
  public SpriteAnimationPanel() {
    this(true);
  }

  /**
   * Creates a new sprite animation panel with a flow layout and the specified buffer strategy. The runner is initially
   * set to the stopped state.
   *
   * @param isDoubleBuffered a boolean, true for double-buffering, whichuses additional memory space to achieve fast,
   *                           flicker-freeupdates.
   */
  public SpriteAnimationPanel(boolean isDoubleBuffered) {
    this(new FlowLayout(), isDoubleBuffered);
  }

  /**
   * Creates a new sprite animation panel with a double buffer and the specified layout manager. The runner is initially
   * set to the stopped state.
   *
   * @param layout the LayoutManager to use.
   */
  public SpriteAnimationPanel(LayoutManager layout) {
    this(layout, true);
  }

  /**
   * Creates a new sprite animation panel with the specified layout manager and buffer strategy. The runner is initially
   * set to the stopped state.
   *
   * @param layout           the LayoutManager to use.
   * @param isDoubleBuffered a boolean, true for double-buffering, whichuses additional memory space to achieve fast,
   *                           flicker-freeupdates.
   */
  public SpriteAnimationPanel(LayoutManager layout, boolean isDoubleBuffered) {
    super(layout, isDoubleBuffered);
    init();
  }

  /** Returns the max. number of sprites that will be visible on the panel at the same time. */
  public int getMaxSprites() {
    return maxSprites;
  }

  /**
   * Specifies the max. number of sprites that should be visible on the panel at the same time. Existing sprites are
   * automatically removed if the new value is smaller than the current number of visible sprites.
   *
   * @param value A positive number.
   */
  public void setMaxSprites(int value) {
    maxSprites = Math.max(1, value);
    while (sprites.size() > maxSprites) {
      final SpriteInfo sprite;
      synchronized (sprites) {
        sprite = sprites.get(0);
        Logger.trace("Removing sprite: {}", sprite.getDecoder().getCreatureInfo().getCreResource().getResourceEntry());
        sprites.remove(0);
      }
      try {
        sprite.close();
      } catch (Exception e) {
        Logger.debug(e);
      }
    }
  }

  /** Specifies an appropriate max. number of sprites that will be visible on the panel at the same time. */
  public void setMaxSpritesAuto() {
    setMaxSprites(calculateMaxSprites());
  }

  /** Returns the lower bound of the sprite creation delay, in ms. */
  public int getSpriteCreationDelayMin() {
    return createSpriteDelayMin;
  }

  /** Returns the upper bound of the sprite creation delay, in ms. */
  public int getSpriteCreationDelayMax() {
    return createSpriteDelayMax;
  }

  /**
   * Specifies a new delay for sprite creation.
   *
   * @param minDelay The lower bound of the delay, in ms.
   * @param maxDelay The upper bound of the delay, in ms.
   */
  public void setSpriteCreationDelay(int minDelay, int maxDelay) {
    minDelay = Math.max(100, minDelay);
    maxDelay = Math.max(100, maxDelay);
    if (minDelay > maxDelay) {
      int v = minDelay;
      minDelay = maxDelay;
      maxDelay = v;
    }
    if (maxDelay == minDelay) {
      maxDelay++;
    }
    createSpriteDelayMin = minDelay;
    createSpriteDelayMax = maxDelay;
  }

  /** Returns the lower bound of the sprite release delay, in ms. */
  public int getSpriteReleaseDelayMin() {
    return releaseSpriteDelayMin;
  }

  /** Returns the upper bound of the sprite release delay, in ms. */
  public int getSpriteReleaseDelayMax() {
    return releaseSpriteDelayMax;
  }

  /**
   * Specifies a new delay for sprite release.
   *
   * @param minDelay The lower bound of the delay, in ms.
   * @param maxDelay The upper bound of the delay, in ms.
   */
  public void setSpriteReleaseDelay(int minDelay, int maxDelay) {
    minDelay = Math.max(100, minDelay);
    maxDelay = Math.max(100, maxDelay);
    if (minDelay > maxDelay) {
      int v = minDelay;
      minDelay = maxDelay;
      maxDelay = v;
    }
    if (maxDelay == minDelay) {
      maxDelay++;
    }
    releaseSpriteDelayMin = minDelay;
    releaseSpriteDelayMax = maxDelay;
  }

  /** Returns the list of sprites */
  public List<SpriteInfo> getSprites() {
    return Collections.unmodifiableList(sprites);
  }

  /** Stops the runner (if running), cleans up current data and initializes new data. */
  public void reset() {
    if (isClosed()) {
      return;
    }

    boolean wasRunning = isRunning();
    boolean wasPaused = isPaused();
    stop();

    cacheCreResources();

    if (wasRunning) {
      start();
      if (wasPaused) {
        pause();
      }
    }
  }

  /** Starts the sprite runner. Does nothing if the runner has already started. */
  public void start() {
    if (isClosed()) {
      return;
    }

    if (!isRunning()) {
      initTimers();
      creationTimer.start();
      releaseTimer.start();
      running = true;
      paused = false;
      runner.interrupt();
    }
  }

  /** Similar to {@link #stop()} this method will stop the sprite runner. However, it won't clean up existing sprites. */
  public void pause() {
    if (isClosed()) {
      return;
    }

    if (isRunning()) {
      releaseTimer.stop();
      creationTimer.stop();
      paused = true;
      running = false;
    }
  }

  /** Stops the sprite runner. Does nothing if the runner has already been stopped. */
  public void stop() {
    if (isClosed()) {
      return;
    }

    if (isRunning()) {
      releaseTimer.stop();
      creationTimer.stop();
      running = false;
      paused = false;
      cleanup();
      SwingUtilities.invokeLater(this::repaint);
    }
  }

  /**
   * Returns {@code true} if the sprite runner is in paused state. The paused state is only set if the runner was
   * explicitly halted by the {@link #pause()} method.
   */
  public boolean isPaused() {
    return !isClosed() && paused;
  }

  /** Returns {@code true} if the sprite runner is currently running. Returns {@code false} otherwise. */
  public boolean isRunning() {
    return !isClosed() && running && !paused;
  }

  /**
   * Returns {@code true} if the {@link SpriteAnimationPanel} instance has been closed.
   * A closed instance cannot be used further anymore.
   */
  public boolean isClosed() {
    return closed;
  }

  /**
   * Terminates the current runner.
   * Calling {@link #start()} or {@link #stop()} won't have any effect after calling this method.
   */
  @Override
  public void close() throws Exception {
    closed = true;
    paused = false;
    if (isRunning()) {
      running = false;
    } else {
      runner.interrupt();
    }
    cleanup();
  }

  @Override
  public void run() {
    while (!isClosed()) {
      if (isRunning()) {
        // playing
        final long startTime = System.currentTimeMillis();
        advance();
        final long endTime = System.currentTimeMillis();
        final long elapsed = Math.max(0L, endTime - startTime);
        final long remainingDelay = Math.max(1L, FRAME_DELAY - elapsed);
        try {
          Thread.sleep(remainingDelay);
        } catch (InterruptedException e) {
          // just bad timing
        }
      } else {
        // paused or stopped
        try {
          Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
          // called to wake up from sleep
        }
      }
    }
  }

  @Override
  public void paint(Graphics g) {
    super.paint(g);
    final Graphics g2 = (g == null) ? null : g.create();
    try {
      render(g2);
    } finally {
      g2.dispose();
    }
  }

  @Override
  public void windowStateChanged(WindowEvent e) {
    if (isRunning() && !isPaused() && (e.getNewState() & Frame.ICONIFIED) != 0) {
      Logger.trace("Window iconified: Stopping sprite actions");
      pause();
    } else if (isPaused()) {
      Logger.trace("Window restored: Resuming sprite actions");
      start();
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == creationTimer) {
      if (sprites.size() < maxSprites) {
        // creating a new sprite instance
        final SpriteInfo sprite = createRandomSprite();
        if (sprite != null) {
          Logger.trace("Adding new sprite: {}", sprite.getDecoder().getCreatureInfo().getCreResource().getResourceEntry());
          synchronized (sprites) {
            sprites.add(sprite);
          }
        }
      }
      creationTimer.setDelay(createDelay(random, createSpriteDelayMin, createSpriteDelayMax));
    } else if (e.getSource() == releaseTimer) {
      // (maybe) release an existing sprite instance
      synchronized (sprites) {
        for (final SpriteInfo si : sprites) {
          if (si.isBounded() && random.nextBoolean()) {
            Logger.trace("Removing sprite bounds: {}", si.getDecoder().getCreatureInfo().getCreResource().getResourceEntry());
            si.setBounded(false);
            break;
          }
        }
      }
      releaseTimer.setDelay(createDelay(random, releaseSpriteDelayMin, releaseSpriteDelayMax));
    }
  }

  @Override
  public void propertyChange(PropertyChangeEvent e) {
    if (e.getSource() instanceof SpriteInfo) {
      final SpriteInfo si = (SpriteInfo) e.getSource();
      switch (e.getPropertyName()) {
        case SpriteInfo.PROPERTY_SEQUENCE_ENDED:
          onSpriteSequenceEnded(si, (Sequence)e.getNewValue());
          break;
        case SpriteInfo.PROPERTY_BOUNDS_HIT:
          onSpriteBoundsHit(si, (int)e.getNewValue());
          break;
        case SpriteInfo.PROPERTY_COLLISION:
          onSpriteCollision(si, (SpriteInfo)e.getNewValue());
          break;
        case SpriteInfo.PROPERTY_VANISHED:
          onSpriteVanished(si);
          break;
      }
    }
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    // nothing to do
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    if (e.getSource() == this) {
      final int x = e.getX();
      final int y = e.getY();
      synchronized (sprites) {
        spritesWorking.clear();
        spritesWorking.addAll(sprites);
        final Rectangle rect = new Rectangle();
        for (final SpriteInfo si : spritesWorking) {
          si.getSpriteBounds((int)si.getX(), (int)si.getY(), rect);
          if (rect.contains(x, y)) {
            si.onSpriteBoundsEntered();
          } else {
            si.onSpriteBoundsExited();
          }
        }
      }
    }
  }

  /** Handles a sprite when their animation cycle of the action sequence ended. */
  private void onSpriteSequenceEnded(SpriteInfo sprite, Sequence seq) {
    updateSprite(sprite, seq);
  }

  /** Handles a sprite when it collides with the canvas boundary. */
  private void onSpriteBoundsHit(SpriteInfo sprite, int boundsMask) {
    // already handled by the sprite object itself
  }

  /** Handles a sprite when it collides with another sprite. */
  private void onSpriteCollision(SpriteInfo sprite, SpriteInfo target) {
    // sprite should move to opposite direction
    final Direction dir = SpriteInfo.findDirection(sprite.getX(), sprite.getY(), target.getX(), target.getY());
    final int shift = random.nextInt(5) - 2;  // adds a slight variation to the direction
    final int newDirValue = (dir.getValue() + 8 + shift) % Direction.values().length;
    final Direction newDir = Direction.from(newDirValue);
    sprite.setDirection(newDir);
  }

  /** Handles a sprite when it vanished from the canvas. */
  private void onSpriteVanished(SpriteInfo sprite) {
    try {
      Logger.trace("Removing sprite: {}", sprite.getDecoder().getCreatureInfo().getCreResource().getResourceEntry());
      synchronized (sprites) {
        sprites.remove(sprite);
      }
      sprite.close();
    } catch (Exception e) {
      Logger.debug(e);
    }
  }

  /** Advances the global sprite setup by one step. */
  private void advance() {
    if (!isRunning() || sprites.isEmpty()) {
      return;
    }

    synchronized (sprites) {
      for (final SpriteInfo si : sprites) {
        si.advance();
      }
    }

    SwingUtilities.invokeLater(this::repaint);
  }

  /** Renders the current sprite setup to the specified graphics context. */
  private void render(Graphics g) {
    if (!isRunning()) {
      return;
    }

    if (g instanceof Graphics2D) {
      final Graphics2D g2 = (Graphics2D)g;
      synchronized (sprites) {
        spritesWorking.clear();
        spritesWorking.addAll(sprites);
        spritesWorking.sort(SPRITE_COMPARATOR);
        for (final SpriteInfo si : spritesWorking) {
          render(g2, si);
        }
      }
    }
  }

  /** Draws the sprite onto the specified graphics context. */
  private void render(Graphics2D g, SpriteInfo sprite) {
    if (sprite == null) {
      return;
    }

    final int x = (int) sprite.getX();
    final int y = (int) sprite.getY();
    PseudoBamFrameEntry frameEntry = sprite.getDecoder().getFrameInfo(sprite.getControl().cycleGetFrameIndexAbsolute());
    final int centerX = frameEntry.getCenterX();
    final int centerY = frameEntry.getCenterY();

    if (sprite.getDecoder().isSelectionCircleEnabled()) {
      sprite.getControl().getVisualMarkers(g, new Point(x - centerX, y - centerY), sprite.getControl().cycleGetFrameIndex());
    }

    g.setComposite(AlphaComposite.SrcOver);
    g.setColor(ColorConvert.TRANSPARENT_COLOR);
    final Image image = sprite.getControl().cycleGetFrame();
    final int dx = x - centerX;
    final int dy = y - centerY;
    g.drawImage(image, dx, dy, null);
    image.flush();
  }

  /** Resets the timer delays. */
  private void initTimers() {
    creationTimer.setDelay(createDelay(random, createSpriteDelayMin, createSpriteDelayMax));
    releaseTimer.setDelay(createDelay(random, releaseSpriteDelayMin, releaseSpriteDelayMax));
  }

  /** Various first-time initializations. */
  private void init() {
    // just needed to detect when we can register the WindowState listener
    addHierarchyListener(new HierarchyListener() {
      @Override
      public void hierarchyChanged(HierarchyEvent e) {
        if (e.getChangedParent() != null) {
          if (SpriteAnimationPanel.this.getTopLevelAncestor() instanceof Window) {
            SpriteAnimationPanel.this.removeHierarchyListener(this);
            final Window wnd = (Window)SpriteAnimationPanel.this.getTopLevelAncestor();
            wnd.addWindowStateListener(SpriteAnimationPanel.this);
          }
        }
      }
    });

    addMouseMotionListener(this);

    cacheCreResources();
    maxSprites = MAX_SPRITES;
    createSpriteDelayMin = DELAY_CREATE_SPRITE_MIN;
    createSpriteDelayMax = DELAY_CREATE_SPRITE_MAX;
    releaseSpriteDelayMin = DELAY_RELEASE_SPRITE_MIN;
    releaseSpriteDelayMax = DELAY_RELEASE_SPRITE_MAX;
    closed = false;
    running = false;
    runner = new Thread(this);
    runner.start();
  }

  /** Cleans up resources and timers. */
  private void cleanup() {
    releaseTimer.stop();
    creationTimer.stop();
    Logger.trace("Clearing all sprites: {}", sprites.size());
    spritesWorking.clear();
    synchronized (sprites) {
      sprites.clear();
    }
  }

  /** Refreshes the CRE resource cache. */
  private void cacheCreResources() {
    creResources.clear();
    creResources.addAll(ResourceFactory.getResources("CRE"));
  }

  /**
   * Creates a new {@link SpriteInfo} instance from a randomly chosen CRE resource.
   *
   * @return A fully initialized {@link SpriteInfo} instance.
   */
  private SpriteInfo createRandomSprite() {
    SpriteInfo retVal = null;

    if (!creResources.isEmpty()) {
      while (true) {
        final ResourceEntry creEntry = creResources.get(random.nextInt(creResources.size()));
        try {
          ByteBuffer buffer = creEntry.getResourceBuffer();
          final String signature = StreamUtils.readString(buffer, 8);
          boolean isValid = false;
          switch (signature) {
            case "CRE V1.0":
              isValid = isValidCreV10(buffer);
              break;
            case "CRE V1.2":
              isValid = isValidCreV12(buffer);
              break;
            case "CRE V2.2":
              isValid = isValidCreV22(buffer);
              break;
            case "CRE V9.0":
              isValid = isValidCreV90(buffer);
              break;
            default:
          }

          if (isValid) {
            final CreResource cre = new CreResource(creEntry);
            final int x = random.nextInt(getWidth());
            final int y = random.nextInt(getHeight());
            final Direction dir = Direction.from(random.nextInt(Direction.values().length));

            final Sequence seq;
            switch (Profile.getGame()) {
              case PST:
                seq = Sequence.PST_WALK;
                break;
              case PSTEE: {
                final int animType = buffer.getInt(0x28) & 0xf000;
                if (animType == 0xf000) {
                  seq = Sequence.PST_WALK;
                } else {
                  seq = Sequence.WALK;
                }
                break;
              }
              default:
                seq = Sequence.WALK;
                break;
            }

            retVal = new SpriteInfo(this, cre, true, x, y, seq, dir);
            retVal.addPropertyChangeListener(this);
            break;
          }
        } catch (Exception e) {
          Logger.debug(e);
        }
      }
    }

    return retVal;
  }

  /** Checks if the specified CRE V1.0 data can be safely created. */
  private boolean isValidCreV10(ByteBuffer buffer) {
    if (buffer != null) {
      final int nameStrref = buffer.getInt(0x08);
      final int tooltipStrref = buffer.getInt(0x0c);
      if (nameStrref <= 0 || nameStrref >= StringTable.getNumEntries() ||
          tooltipStrref <= 0 || tooltipStrref >= StringTable.getNumEntries()) {
        // possibly a meta creature
        return false;
      }

      final int xp = buffer.getInt(0x014);
      final int xp2 = buffer.getInt(0x018);
      if (xp <= 0 && xp2 <= 0) {
        // possibly a meta creature
        return false;
      }

      final int status = buffer.getInt(0x20);
      if ((status & 0xff0) != 0) {
        // skip invisible or dead
        return false;
      }

      final int animType = buffer.getInt(0x28);
      if ((animType & 0xf000) == 0x1000 || ((animType & 0xf000) == 0x3000)) {
        // skip special animations
        return false;
      }

      final String animLabel = IdsMapCache.getIdsSymbol("animate", animType);
      if (animLabel == null) {
        // skip invalid creature animation
        return false;
      }
      if (animLabel.toUpperCase().contains("DOOM_GUARD")) {
        // skip invisible creature animation
        return false;
      }

      final IniMap ini = SpriteUtils.getAnimationInfo(animType);
      if (ini == null) {
        return false;
      }
      final IniMapSection generalSection = ini.getSection("general");
      if (generalSection != null) {
        if (generalSection.getAsDouble("move_scale", -1.0) <= 0.0) {
          // skip non-moving creature
          return false;
        }
      }

      if (Profile.getGame() == Profile.Game.PSTEE) {
        final IniMapSection animSection = ini.getSection("monster_planescape");
        if (animSection != null) {
          final String resref = animSection.getAsString("walk");
          if (resref != null && resref.length() >= 4) {
            if (!resref.substring(1, 4).equalsIgnoreCase("wlk")) {
              // skip animation without dedicated walking sequence
              return false;
            }
          } else {
            // skip animation without walking sequence
            return false;
          }
        }
      }

      final int allegiance = buffer.get(0x270) & 0xff;
      // possibly a meta creature
      return allegiance > 1;
    }

    return false;
  }

  /** Checks if the specified CRE V1.2 data can be safely created. */
  private boolean isValidCreV12(ByteBuffer buffer) {
    if (buffer != null) {
      final int nameStrref = buffer.getInt(0x08);
      final int tooltipStrref = buffer.getInt(0x0c);
      if (nameStrref <= 0 || nameStrref >= StringTable.getNumEntries() ||
          tooltipStrref <= 0 || tooltipStrref >= StringTable.getNumEntries()) {
        // possibly a meta creature
        return false;
      }

      final int xp = buffer.getInt(0x014);
      if (xp <= 0) {
        // possibly a meta creature
        return false;
      }

      final int status = buffer.getInt(0x20);
      if ((status & 0xff0) != 0) {
        // invisible or dead
        return false;
      }

      final int animType = buffer.getInt(0x28);
      if (animType < 0x1000) {
        // special effect animation
        return false;
      }

      final String animLabel = IdsMapCache.getIdsSymbol("animate", animType);
      if (animLabel == null) {
        // invalid creature animation
        return false;
      }
      if (animLabel.indexOf('*') >= 0 || animLabel.indexOf('!') >= 0) {
        // illegal creature animation
        return false;
      }

      final IniMap ini = SpriteUtils.getAnimationInfo(animType);
      if (ini == null) {
        return false;
      }
      final IniMapSection iniSection = ini.getSection("general");
      if (iniSection != null) {
        if (iniSection.getAsDouble("move_scale", -1.0) <= 0.0) {
          // non-moving creature
          return false;
        }
      }

      final IniMapSection animSection = ini.getSection("monster_planescape");
      if (animSection != null) {
        final String resref = animSection.getAsString("walk");
        if (resref != null && resref.length() >= 4) {
          if (!resref.substring(1, 4).equalsIgnoreCase("wlk")) {
            // skip animation without dedicated walking sequence
            return false;
          }
        } else {
          // skip animation without walking sequence
          return false;
        }
      }

      final int allegiance = buffer.get(0x314) & 0xff;
      // possibly a meta creature
      return allegiance > 1;
    }

    return false;
  }

  /** Checks if the specified CRE V2.2 data can be safely created. */
  private boolean isValidCreV22(ByteBuffer buffer) {
    if (buffer != null) {
      final int nameStrref = buffer.getInt(0x08);
      final int tooltipStrref = buffer.getInt(0x0c);
      if (nameStrref <= 0 || nameStrref >= StringTable.getNumEntries() ||
          tooltipStrref <= 0 || tooltipStrref >= StringTable.getNumEntries()) {
        // possibly a meta creature
        return false;
      }

      final int xp = buffer.getInt(0x014);
      if (xp <= 0) {
        // possibly a meta creature
        return false;
      }

      final int status = buffer.getInt(0x20);
      if ((status & 0xff0) != 0) {
        // invisible or dead
        return false;
      }

      final int animType = buffer.getInt(0x28);
      if (animType < 0x1000) {
        // special effect animation
        return false;
      }

      final String animLabel = IdsMapCache.getIdsSymbol("animate", animType);
      if (animLabel == null) {
        // invalid creature animation
        return false;
      }
      if (animLabel.toUpperCase().contains("INVISIBLE") || animLabel.toUpperCase().contains("KEG")) {
        // invisible creature animation
        return false;
      }

      final IniMap ini = SpriteUtils.getAnimationInfo(animType);
      if (ini == null) {
        return false;
      }
      final IniMapSection iniSection = ini.getSection("general");
      if (iniSection != null) {
        // possibly non-moving creature
        return !(iniSection.getAsDouble("move_scale", -1.0) <= 0.0);
      }

      return true;
    }

    return false;
  }

  /** Checks if the specified CRE V9.0 data can be safely created. */
  private boolean isValidCreV90(ByteBuffer buffer) {
    if (buffer != null) {
      final int nameStrref = buffer.getInt(0x08);
      final int tooltipStrref = buffer.getInt(0x0c);
      if (nameStrref <= 0 || nameStrref >= StringTable.getNumEntries() ||
          tooltipStrref <= 0 || tooltipStrref >= StringTable.getNumEntries()) {
        // possibly a meta creature
        return false;
      }

      final int xp = buffer.getInt(0x014);
      if (xp <= 0) {
        // possibly a meta creature
        return false;
      }

      final int status = buffer.getInt(0x20);
      if ((status & 0xff0) != 0) {
        // invisible or dead
        return false;
      }

      final int animType = buffer.getInt(0x28);
      if (animType < 0x1000) {
        // special effect animation
        return false;
      }

      final String animLabel = IdsMapCache.getIdsSymbol("animate", animType);
      if (animLabel == null) {
        // invalid creature animation
        return false;
      }
      if (animLabel.toUpperCase().contains("DOOM_GUARD")) {
        // invisible creature animation
        return false;
      }

      final IniMap ini = SpriteUtils.getAnimationInfo(animType);
      if (ini == null) {
        return false;
      }
      final IniMapSection iniSection = ini.getSection("general");
      if (iniSection != null) {
        // possibly non-moving creature
        return !(iniSection.getAsDouble("move_scale", -1.0) <= 0.0);
      }

      return true;
    }

    return false;
  }

  /** Updates the action and/or direction of the specified sprite. */
  private void updateSprite(SpriteInfo sprite, Sequence seq) {
    final Sequence[] sequences = sprite.isPstAnimation() ? DEF_SEQUENCES_PST: DEF_SEQUENCES;
    final Function<SpriteInfo, Sequence> walkAction = si -> {
        if (si.isPstAnimation()) {
          if (random.nextInt(100) < 80) {
            return si.getDefaultWalkingSequence();
          } else {
            return si.getDefaultRunningSequence();
          }
        } else {
          return si.getDefaultWalkingSequence();
        }
    };
    final boolean isMoving = (sprite.getMoveFactor() > 0.0);

    // determine next sequence
    Sequence newSequence = null;

    if (newSequence == null) {
      switch (seq) {
        case SPELL:
          newSequence = Sequence.CAST;
          break;
        case SPELL1:
          newSequence = Sequence.CAST1;
          break;
        case SPELL2:
          newSequence = Sequence.CAST2;
          break;
        case SPELL3:
          newSequence = Sequence.CAST3;
          break;
        case SPELL4:
          newSequence = Sequence.CAST4;
          break;
        default:
      }
    }

    if (newSequence == null) {
      switch (seq) {
        case PST_DIE_BACKWARD:
        case PST_DIE_FORWARD:
        case PST_DIE_COLLAPSE:
          newSequence = Sequence.PST_GET_UP;
          break;
        case PST_STAND:
        case PST_STAND_FIDGET1:
        case PST_STAND_FIDGET2:
          switch (random.nextInt(8)) {
            case 0:
              newSequence = Sequence.PST_STAND_TO_STANCE;
              break;
            case 1:
              newSequence = Sequence.PST_STAND_FIDGET1;
              break;
            case 2:
              newSequence = Sequence.PST_STAND_FIDGET2;
              break;
            case 3:
            case 4:
            case 5:
              newSequence = Sequence.PST_STAND;
              break;
          }
          break;
        case PST_STANCE:
        case PST_STANCE_FIDGET1:
        case PST_STANCE_FIDGET2:
          switch (random.nextInt(8)) {
            case 0:
              newSequence = Sequence.PST_STANCE_TO_STAND;
              break;
            case 1:
              newSequence = Sequence.PST_STANCE_FIDGET1;
              break;
            case 2:
              newSequence = Sequence.PST_STANCE_FIDGET2;
              break;
            case 3:
            case 4:
            case 5:
              newSequence = Sequence.PST_STANCE;
              break;
          }
          break;
        case PST_STANCE_TO_STAND:
          newSequence = Sequence.PST_STAND;
          break;
        case PST_STAND_TO_STANCE:
          newSequence = Sequence.PST_STANCE;
          break;
        default:
      }
    }

    if (newSequence == null) {
      if (isMoving && random.nextInt(100) < 5) {
        newSequence = walkAction.apply(sprite);
      } else if (!isMoving && random.nextBoolean()) {
        newSequence = walkAction.apply(sprite);
      }
    }

    if (newSequence == null) {
      if (random.nextInt(100) < 10) {
        newSequence = sequences[random.nextInt(sequences.length)];
      }
    }

    // validation check
    if (newSequence != null) {
      for (int i = 0; i < 3 && !sprite.getDecoder().isSequenceAvailable(newSequence); i++) {
        switch (newSequence) {
          case CAST:
          case CAST1:
          case CAST2:
          case CAST3:
          case CAST4:
            newSequence = random.nextBoolean() ? Sequence.STAND : sprite.getDefaultWalkingSequence();
            break;
          case PST_GET_UP:
            newSequence = walkAction.apply(sprite);
            break;
          default:
            newSequence = seq;
        }
      }
      if (!sprite.getDecoder().isSequenceAvailable(newSequence)) {
        newSequence = sprite.getDefaultWalkingSequence();
      }
    }

    // applying sequence
    if (newSequence != null) {
      Logger.trace("Applying new sequence to {}: {}",
          sprite.getDecoder().getCreatureInfo().getCreResource().getResourceEntry(), newSequence.name());
      try {
        sprite.setSequence(newSequence);
      } catch (Throwable t) {
        Logger.debug("Sequence {} not available for {}: Falling back to walking",
            newSequence.name(), sprite.getDecoder().getCreatureInfo().getCreResource().getResourceEntry());
        sprite.setSequence(sprite.getDefaultWalkingSequence());
      }
    }

    // applying direction change
    if (newSequence == null && random.nextInt(100) < 25) {
      sprite.setDirection(Direction.from(random.nextInt(Direction.values().length)));
    }
  }

  /**
   * Returns a randomized delay, based on the specified parameters.
   *
   * @param rnd The {@link Random} instance to generate a randomized value.
   * @param min Lower bounds of the delay.
   * @param max Upper bounds of the delay.
   * @return Delay within the specified parameters.
   */
  private static int createDelay(Random rnd, int min, int max) {
    return min + rnd.nextInt(Math.abs(max - min));
  }

  /** Calculates an appropriate max. sprite count for the current system. */
  private static int calculateMaxSprites() {
    int factor = 4;
    try {
      final DisplayMode dm = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
      final int w = dm.getWidth();
      if (w <= 1280) {
        factor = 2;
      } else if (w <= 1920) {
        factor = 4;
      } else if (w <= 2560) {
        factor = 6;
      } else if (w <= 3840) {
        factor = 8;
      } else {
        factor = 10;
      }
    } catch (Throwable t) {
      Logger.debug(t);
    }
    return 5 * factor;
  }

  // -------------------------- INNER CLASSES --------------------------

  /** Stores the state of a single creature sprite. */
  public static class SpriteInfo implements Closeable {
    /**
     * Name of a {@link PropertyChangeEvent} when the animation cycle of an action sequence ended.
     * <p>
     * Value type: current action {@link Sequence}
     * </p>
     */
    public static final String PROPERTY_SEQUENCE_ENDED = "sequenceEnded";

    /**
     * Name of a {@link PropertyChangeEvent} when the canvas boundary is about to be hit by the next advancement.
     * <p>
     * Value type: Bits of the boundary sides as {@link Integer} bitfield (see {@link #BOUNDS_TOP},
     * {@link #BOUNDS_LEFT}, {@link #BOUNDS_BOTTOM}, {@link #BOUNDS_RIGHT}).
     * </p>
     */
    public static final String PROPERTY_BOUNDS_HIT = "boundsHit";

    /**
     * Name of a {@link PropertyChangeEvent} when a collision with another {@link SpriteInfo} instance is about to
     * happen. This event will be triggered for all involved sprites.
     * <p>
     * Value type: {@link SpriteInfo} object of the collision target.
     * </p>
     */
    public static final String PROPERTY_COLLISION = "collision";

    /**
     * Name of a {@link PropertyChangeEvent} when an unbounded sprite has completely vanished from the canvas.
     * <p>
     * Value type: null (no argument)
     * </p>
     */
    public static final String PROPERTY_VANISHED = "vanished";

    /** Bit index for top boundary. */
    private static final int BOUNDS_TOP     = 0;
    /** Bit index for left boundary. */
    private static final int BOUNDS_LEFT    = 1;
    /** Bit index for bottom boundary. */
    private static final int BOUNDS_BOTTOM  = 2;
    /** Bit index for right boundary. */
    private static final int BOUNDS_RIGHT   = 3;

    /** Defines a unit vector for all available {@link Direction}s. */
    private static final EnumMap<Direction, Point2D.Double> DIRECTION = new EnumMap<>(Direction.class);

    /** Maps mirrored directions when hitting the top boundary from any direction. */
    private static final Map<Direction, Direction> MIRRORED_DIR_TOP = new HashMap<>();
    /** Maps mirrored directions when hitting the left boundary from any direction. */
    private static final Map<Direction, Direction> MIRRORED_DIR_LEFT = new HashMap<>();
    /** Maps mirrored directions when hitting the bottom boundary from any direction. */
    private static final Map<Direction, Direction> MIRRORED_DIR_BOTTOM = new HashMap<>();
    /** Maps mirrored directions when hitting the right boundary from any direction. */
    private static final Map<Direction, Direction> MIRRORED_DIR_RIGHT = new HashMap<>();

    /**
     * Maps a factor to individual action sequences which determines the effective move speed of the sprite.
     * <p>
     * Walking has a move factor 1 (unaltered speed). Running has a move factor 2. Everything else has a move factor 0
     * (no move).
     * </p>
     */
    private static final EnumMap<Sequence, Double> SEQUENCE_MOVE_FACTOR = new EnumMap<>(Sequence.class);

    /** A global threadpool with reusable threads for executing general purpose tasks. */
    private static final ExecutorService CACHED_THREADPOOL = Executors.newCachedThreadPool();

    /** Duration for displaying the selection circle of a sprite. */
    private static final long DISPLAY_CIRCLE_DURATION = 3000L;

    static {
      // Normalized vectors for directions
      DIRECTION.put(Direction.S, getUnitVector(0.0, 2.0, true));
      DIRECTION.put(Direction.SSW, getUnitVector(-1.0, 2.0, true));
      DIRECTION.put(Direction.SW, getUnitVector(-2.0, 2.0, true));
      DIRECTION.put(Direction.WSW, getUnitVector(-2.0, 1.0, true));
      DIRECTION.put(Direction.W, getUnitVector(-2.0, 0.0, true));
      DIRECTION.put(Direction.WNW, getUnitVector(-2.0, -1.0, true));
      DIRECTION.put(Direction.NW, getUnitVector(-2.0, -2.0, true));
      DIRECTION.put(Direction.NNW, getUnitVector(-1.0, -2.0, true));
      DIRECTION.put(Direction.N, getUnitVector(0.0, -2.0, true));
      DIRECTION.put(Direction.NNE, getUnitVector(1.0, -2.0, true));
      DIRECTION.put(Direction.NE, getUnitVector(2.0, -2.0, true));
      DIRECTION.put(Direction.ENE, getUnitVector(2.0, -1.0, true));
      DIRECTION.put(Direction.E, getUnitVector(2.0, 0.0, true));
      DIRECTION.put(Direction.ESE, getUnitVector(2.0, 1.0, true));
      DIRECTION.put(Direction.SE, getUnitVector(2.0, 2.0, true));
      DIRECTION.put(Direction.SSE, getUnitVector(1.0, 2.0, true));

      for (final Direction dir : Direction.values()) {
        MIRRORED_DIR_TOP.put(dir, dir);
        MIRRORED_DIR_LEFT.put(dir, dir);
        MIRRORED_DIR_BOTTOM.put(dir, dir);
        MIRRORED_DIR_RIGHT.put(dir, dir);
        switch (dir) {
          case S:
            MIRRORED_DIR_BOTTOM.put(dir, Direction.N);
            break;
          case SSW:
            MIRRORED_DIR_LEFT.put(dir, Direction.SSE);
            MIRRORED_DIR_BOTTOM.put(dir, Direction.NNW);
            break;
          case SW:
            MIRRORED_DIR_LEFT.put(dir, Direction.SE);
            MIRRORED_DIR_BOTTOM.put(dir, Direction.NW);
            break;
          case WSW:
            MIRRORED_DIR_LEFT.put(dir, Direction.ESE);
            MIRRORED_DIR_BOTTOM.put(dir, Direction.WNW);
            break;
          case W:
            MIRRORED_DIR_LEFT.put(dir, Direction.E);
            break;
          case WNW:
            MIRRORED_DIR_TOP.put(dir, Direction.WSW);
            MIRRORED_DIR_LEFT.put(dir, Direction.ENE);
            break;
          case NW:
            MIRRORED_DIR_TOP.put(dir, Direction.SW);
            MIRRORED_DIR_LEFT.put(dir, Direction.NE);
            break;
          case NNW:
            MIRRORED_DIR_TOP.put(dir, Direction.SSW);
            MIRRORED_DIR_LEFT.put(dir, Direction.NNE);
            break;
          case N:
            MIRRORED_DIR_TOP.put(dir, Direction.S);
            break;
          case NNE:
            MIRRORED_DIR_TOP.put(dir, Direction.SSE);
            MIRRORED_DIR_RIGHT.put(dir, Direction.NNW);
            break;
          case NE:
            MIRRORED_DIR_TOP.put(dir, Direction.SE);
            MIRRORED_DIR_RIGHT.put(dir, Direction.NW);
            break;
          case ENE:
            MIRRORED_DIR_TOP.put(dir, Direction.ESE);
            MIRRORED_DIR_RIGHT.put(dir, Direction.WNW);
            break;
          case E:
            MIRRORED_DIR_RIGHT.put(dir, Direction.W);
            break;
          case ESE:
            MIRRORED_DIR_BOTTOM.put(dir, Direction.ENE);
            MIRRORED_DIR_RIGHT.put(dir, Direction.WSW);
            break;
          case SE:
            MIRRORED_DIR_BOTTOM.put(dir, Direction.NE);
            MIRRORED_DIR_RIGHT.put(dir, Direction.SW);
            break;
          case SSE:
            MIRRORED_DIR_BOTTOM.put(dir, Direction.NNE);
            MIRRORED_DIR_RIGHT.put(dir, Direction.SSW);
            break;
        }
      }

      for (final Sequence seq : Sequence.values()) {
        switch (seq) {
          case WALK:
          case PST_WALK:
            SEQUENCE_MOVE_FACTOR.put(seq, 1.0);
            break;
          case PST_RUN:
            SEQUENCE_MOVE_FACTOR.put(seq, 2.0);
            break;
          default:
            SEQUENCE_MOVE_FACTOR.put(seq, 0.0);
            break;
        }
      }
    }

    /** Task is fired after a set amount of time to disable the selection circle display. */
    private final Callable<Boolean> circleEndedTask = () -> {
      try {
        Thread.sleep(DISPLAY_CIRCLE_DURATION);
      } catch (InterruptedException e) {
        // cancelled prematurely
        return false;
      }

      if (!isClosed()) {
        getDecoder().setSelectionCircleEnabled(false);
        return true;
      }
      return false;
    };

    /** List for storing event listener objects. */
    private final EventListenerList listenerList = new EventListenerList();

    /** Sprite decoder instance */
    private final SpriteDecoder decoder;

    /** Sprite controller instance */
    private final SpriteBamControl control;

    /** Moving speed, in unit vectors per tick */
    private final double speed;

    /** Creature space, radius in pixels */
    private final double space;

    /** The parent {@link SpriteAnimationPanel} instance. */
    private final SpriteAnimationPanel panel;

    /** A reusable {@link BitSet} object for storing the result of {@link #boundsHit()}. */
    private final BitSet boundsHit = new BitSet(4);

    /** x coordinate of cre location */
    private double x;
    /** y coordinate of cre location */
    private double y;

    /** Indicates whether the sprite is bound to the canvas. */
    private boolean bounded;

    /** Currently used animation sequence */
    private Sequence sequence;

    /** Current moving direction */
    private Direction direction;

    /** Frame index of the current animation cycle */
    private int frameIndex;

    /** Indicates that the SpriteInfo object has been released. */
    private boolean closed;

    /** A future that provides access to the background task for delayed deactivation of selection circle display. */
    private Future<Boolean> circleEndedTaskResult;
    /** Tracks whether the mouse cursor has entered or left the bounds of the sprite. */
    private boolean spriteBoundsEntered;

    /**
     * Initializes a new object for tracking the current state of a sprite.
     *
     * @param animator The associated {@link SpriteAnimationPanel} instance.
     * @param cre      {@link CreResource} to create the sprite of.
     * @param bounded  Whether the sprite should be confined within the boundaries of the animator panel.
     * @param x        Initial x coordinate on the panel.
     * @param y        Initial y coordinate on the panel.
     * @param seq      The action sequence to load.
     * @param dir      The direction to move or face.
     * @throws Exception if the sprite could not be loaded.
     */
    public SpriteInfo(SpriteAnimationPanel animator, CreResource cre, boolean bounded, int x, int y, Sequence seq,
        Direction dir) throws Exception {
      this.panel = Objects.requireNonNull(animator);
      this.decoder = SpriteDecoder.importSprite(cre);
      this.decoder.setSelectionCircleBitmap(isPstAnimation());
      this.control = this.decoder.createControl();
      this.control.setMode(BamControl.Mode.INDIVIDUAL);
      this.speed = (int) this.decoder.getMoveScale();
      this.space = this.decoder.getPersonalSpace() * 8;
      this.x = getAdjustedX(x);
      this.y = getAdjustedY(y);
      this.bounded = bounded;
      this.direction = Direction.S;
      setSequence(Objects.requireNonNull(seq));
      setDirection(Objects.requireNonNull(dir));
      this.closed = false;
    }

    /** Returns whether the {@link SpriteInfo} instance has been released. A released instance cannot be used again. */
    public boolean isClosed() {
      return closed;
    }

    @Override
    public void close() throws Exception {
      closed = true;
      direction = null;
      sequence = null;
      boundsHit.clear();
      decoder.close();
    }

    /**
     * Advances the sprite by one frame. Updates frame and position. Sprite is automatically reflected from boundaries
     * if bounded mode is enabled.
     */
    public void advance() {
      if (isClosed()) {
        return;
      }

      final boolean sequenceEnded = isSequenceEnded();
      if (getControl().cycleFrameCount() > 0) {
        frameIndex = (getFrameIndex() + 1) % getControl().cycleFrameCount();
      }
      getControl().cycleSetFrameIndex(getFrameIndex());

      // advancing position: take boundary collision into account
      for (int i = 0; isBounded() && i < 4; i++) {
        final BitSet bounds = boundsHit();
        if (!bounds.isEmpty()) {
          final Direction newDirection = getMirroredDirection(direction, bounds.get(BOUNDS_TOP), bounds.get(BOUNDS_LEFT),
              bounds.get(BOUNDS_BOTTOM), bounds.get(BOUNDS_RIGHT));
          setDirection(newDirection);
          break;
        }
      }
      advancePosition();

      // onSequenceEnded event
      if (sequenceEnded) {
        fireSequenceEnded();
      }
    }

    /** Returns the associated {@link SpriteAnimationPanel} instance. */
    public SpriteAnimationPanel getPanel() {
      return panel;
    }

    /** Returns the {@link SpriteDecoder} instance of the sprite. */
    public SpriteDecoder getDecoder() {
      return decoder;
    }

    /** Returns the {@link SpriteBamControl} associated with the sprite decoder. */
    public SpriteBamControl getControl() {
      return control;
    }

    /** Returns the default movement speed of the sprite. */
    public double getSpeed() {
      return speed;
    }

    /** Returns the space that is occupied by the sprite, as radius in pixels. */
    public double getSpace() {
      return space;
    }

    /** Returns the current x coordinate of the sprite on the canvas. */
    public double getX() {
      return x;
    }

    /** Returns the current y coordinate of the sprite on the canvas. */
    public double getY() {
      return y;
    }

    /** Returns the current action {@link Sequence} of the sprite. */
    public Sequence getSequence() {
      return sequence;
    }

    /**
     * Assigns a new sequence and direction to the sprite.
     *
     * @param newSequence  The new {@link Sequence}.
     * @return {@code true} if sprite data could be loaded successfully, {@code false} otherwise.
     */
    public boolean setSequence(Sequence newSequence) {
      boolean retVal = false;
      if (newSequence != null && newSequence != getSequence()) {
        try {
          retVal = getDecoder().loadSequence(newSequence);
          if (retVal) {
            sequence = newSequence;
            frameIndex = 0;
            setDirection(getDirection());
          }
        } catch (Exception e) {
          Logger.debug(e);
        }
      }
      return retVal;
    }

    /** Returns {@code true} if the animation cycle for the current sequence has ended. */
    public boolean isSequenceEnded() {
      return getFrameIndex() + 1 >= getControl().cycleFrameCount();
    }

    /** Returns the move factor for the current action sequence of the sprite. */
    public double getMoveFactor() {
      return getMoveFactor(getSequence());
    }

    /** Returns the move factor for the specified action sequence. */
    public double getMoveFactor(Sequence seq) {
      return SEQUENCE_MOVE_FACTOR.getOrDefault(seq, 0.0);
    }

    /** Returns the current {@link Direction} of the sprite. */
    public Direction getDirection() {
      return direction;
    }

    /** Assigns the specified direction to the current sprite sequence. Frame advancement is preserved when possible. */
    public void setDirection(Direction newDirection) {
      if (newDirection != null) {
        newDirection = getDecoder().getExistingDirection(newDirection);
        final int cycleIdx = getDecoder().getDirectionMap().getOrDefault(newDirection, -1);
        if (cycleIdx >= 0) {
          getControl().cycleSet(cycleIdx);
          direction = newDirection;
          if (control.cycleFrameCount() > 0) {
            frameIndex = getFrameIndex() % getControl().cycleFrameCount();
          }
        }
      }
    }

    /** Returns the frame index of the animation cycle used for the current action sequence. */
    public int getFrameIndex() {
      return frameIndex;
    }

    /** Returns whether the sprite should be confined to the canvas. */
    public boolean isBounded() {
      return bounded;
    }

    /** Specifies whether the sprite should be confined to the canvas. */
    public void setBounded(boolean b) {
      this.bounded = b;
    }

    /** Returns the horizontal distance for advancing the sprite by one step in the current direction. */
    public double getAdvanceX() {
      return DIRECTION.get(getDirection()).x * getSpeed() * getMoveFactor();
    }

    /** Returns the vertical distance for advancing the sprite by one step in the current direction. */
    public double getAdvanceY() {
      return DIRECTION.get(getDirection()).y * getSpeed() * getMoveFactor();
    }

    /**
     * Returns the vector for advancing the sprite by one step in the current direction.
     * <p>
     * Note that a call of this method involves allocation of a {@link Point2D} object.
     * </p>
     */
    public Point2D.Double getAdvance() {
      final Point2D.Double retVal = new Point2D.Double();
      final Point2D.Double unit = DIRECTION.get(getDirection());
      retVal.x = unit.x * getSpeed() * getMoveFactor();
      retVal.y = unit.y * getSpeed() * getMoveFactor();
      return retVal;
    }

    /** Advances the sprite by one step. */
    public void advancePosition() {
      x = getAdjustedX(x + getAdvanceX());
      y = getAdjustedY(y + getAdvanceY());

      // onVanished event
      if (isVanished()) {
        fireVanished();
      }

      // onBoundsHit event
      final BitSet bits = boundsHit();
      if (!bits.isEmpty()) {
        fireBoundsHit(bits);
      }

      // onCollision event
      findCollisions(true, null);
    }

    /**
     * Returns the bounds of the sprite relative to the given coordinates.
     *
     * @param x    x coordinate of the sprite center position.
     * @param y    y coordinate of the sprite center position.
     * @param rect A {@link Rectangle} object for reuse. Specify {@code null} to create a new {@code Rectangle}
     *               instance.
     * @return {@link Rectangle} with the bounds of the sprite.
     */
    public Rectangle getSpriteBounds(int x, int y, Rectangle rect) {
      if (rect == null) {
        rect = new Rectangle();
      }

      final PseudoBamFrameEntry info = getDecoder().getFrameInfo(getControl().cycleGetFrameIndexAbsolute());
      rect.x = x - info.getCenterX();
      rect.y = y - info.getCenterY();
      rect.width = info.getWidth();
      rect.height = info.getHeight();
      return rect;
    }

    /**
     * Returns a {@link BitSet} with all boundaries that are hit by the next sprite advancement.
     * <p>
     * Note: {@link BitSet} instance of the return value is reused for every call of this method.
     * </p>
     *
     * @return A {@link BitSet} containing all boundaries that are hit by the next sprite advancement.
     * @see #BOUNDS_TOP
     * @see #BOUNDS_LEFT
     * @see #BOUNDS_BOTTOM
     * @see #BOUNDS_RIGHT
     */
    public BitSet boundsHit() {
      boundsHit.clear();
      if (isBounded()) {
        final Point2D.Double vec = getAdvance();
        boundsHit.set(BOUNDS_TOP, y + vec.y - getSpace() < 0);
        boundsHit.set(BOUNDS_LEFT, x + vec.x - getSpace() < 0);
        boundsHit.set(BOUNDS_BOTTOM, y + vec.y + getSpace() > getPanel().getHeight());
        boundsHit.set(BOUNDS_RIGHT, x + vec.x + getSpace() > getPanel().getWidth());
      }
      return boundsHit;
    }

    /** Returns whether the sprite has fully disappeared behind the canvas boundaries. */
    public boolean isVanished() {
      if (isBounded()) {
        return false;
      }

      final PseudoBamFrameEntry info = getDecoder().getFrameInfo(getControl().cycleGetFrameIndexAbsolute());
      int left = -info.getCenterX();
      int top = -info.getCenterY();
      int right = info.getWidth() - info.getCenterX();
      int bottom = info.getHeight() - info.getCenterY();

      boolean retVal = (getX() + right < 0);
      if (!retVal) {
        retVal = (getY() + bottom < 0);
      }
      if (!retVal) {
        retVal = (getX() + left > getPanel().getWidth());
      }
      if (!retVal) {
        retVal = (getY() + top > getPanel().getHeight());
      }
      return retVal;
    }

    /**
     * Returns the mirrored direction based on the specified parameters.
     *
     * @param dir    The {@link Direction} to mirror.
     * @param top    Whether to mirror on the top border.
     * @param left   Whether to mirror on the left border.
     * @param bottom Whether to mirror on the bottom border.
     * @param right  Whether to mirror on the right border.
     * @return The mirrored {@link Direction}.
     */
    public Direction getMirroredDirection(Direction dir, boolean top, boolean left, boolean bottom, boolean right) {
      Direction retVal = dir;
      if (retVal != null) {
        if (top) {
          retVal = MIRRORED_DIR_TOP.get(retVal);
        }
        if (left) {
          retVal = MIRRORED_DIR_LEFT.get(retVal);
        }
        if (bottom) {
          retVal = MIRRORED_DIR_BOTTOM.get(retVal);
        }
        if (right) {
          retVal = MIRRORED_DIR_RIGHT.get(retVal);
        }
      }
      return retVal;
    }

    /** Adds a {@link PropertyChangeListener} to the SpriteInfo object. */
    public void addPropertyChangeListener(PropertyChangeListener l) {
      if (l != null) {
        listenerList.add(PropertyChangeListener.class, l);
      }
    }

    /** Returns all registered {@link PropertyChangeListener}s for this SpriteInfo object. */
    public PropertyChangeListener[] getPropertyChangeListeners() {
      return listenerList.getListeners(PropertyChangeListener.class);
    }

    /** Removes a {@link PropertyChangeListener} from the SpriteInfo object. */
    public void removePropertyChangeListener(PropertyChangeListener l) {
      if (l != null) {
        listenerList.remove(PropertyChangeListener.class, l);
      }
    }

    /** Generalized method for firing a {@link PropertyChangeEvent} to all registered listeners. */
    private void firePropertyChangePerformed(String name, Object oldValue, Object newValue) {
      if (name != null) {
        Object[] listeners = listenerList.getListenerList();
        PropertyChangeEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
          if (listeners[i] == PropertyChangeListener.class) {
            // Event object is lazily created
            if (e == null) {
              e = new PropertyChangeEvent(this, name, oldValue, newValue);
            }
            final PropertyChangeListener listener = (PropertyChangeListener)listeners[i + 1];
            final PropertyChangeEvent event = e;
            SwingUtilities.invokeLater(() -> listener.propertyChange(event));
          }
        }
      }
    }

    /** Fired when the animation cycle of the current action sequence ended. */
    private void fireSequenceEnded() {
      firePropertyChangePerformed(PROPERTY_SEQUENCE_ENDED, null, sequence);
    }

    /** Fired if the sprite will collide with the canvas boundary on the next frame. */
    private void fireBoundsHit(BitSet hitBounds) {
      final int bounds = hitBounds.toByteArray()[0];
      firePropertyChangePerformed(PROPERTY_BOUNDS_HIT, 0, bounds);
    }

    /** Fires if the sprite collide with another sprite at the next frame. */
    private void fireCollision(SpriteInfo target) {
      firePropertyChangePerformed(PROPERTY_COLLISION, null, target);
    }

    /** Fired when the sprite vanished completely from the canvas. */
    private void fireVanished() {
      firePropertyChangePerformed(PROPERTY_VANISHED, null, null);
    }

    /**
     * Determines if any of the available sprites will collide with this sprite in the next update.
     *
     * @param fireEvent Indicates whether a {@link PropertyChangeEvent} is fired for any detected collisions.
     * @param output    An optional {@link List} that stores the collision targets. Specify {@code null} to skip.
     */
    private void findCollisions(boolean fireEvent, List<SpriteInfo> output) {
      final double sx = getX() + getAdvanceX();
      final double sy = getY() + getAdvanceY();
      for (final SpriteInfo si : getPanel().getSprites()) {
        if (si != this) {
          final double dx = si.x + si.getAdvanceX();
          final double dy = si.y + si.getAdvanceY();
          final double spaceDist2 = (getSpace() + si.getSpace())*(getSpace() + si.getSpace());
          final double dist2 = (sx - dx)*(sx - dx) + (sy - dy)*(sy - dy);
          if (dist2 < spaceDist2) {
            if (fireEvent) {
              fireCollision(si);
            }
            if (output != null) {
              output.add(si);
            }
          }
        }
      }
    }

    /** Adjust the specified x coordinate to a legal value. */
    private double getAdjustedX(double x) {
      if (isBounded()) {
        if (x - getSpace() < 0.0 ) {
          x = getSpace();
        }
        if (x + getSpace() > getPanel().getWidth()) {
          x = getPanel().getWidth() - getSpace();
        }
      }
      return x;
    }

    /** Adjust the specified y coordinate to a legal value. */
    private double getAdjustedY(double y) {
      if (isBounded()) {
        if (y - getSpace() < 0.0 ) {
          y = getSpace();
        }
        if (y + getSpace() > getPanel().getHeight()) {
          y = getPanel().getHeight() - getSpace();
        }
      }
      return y;
    }

    /**
     * Enables the selection circle of the sprite for a set amount of time.
     * <p>
     * This method should be called when the mouse cursor is inside the sprite bounds. Together with
     * {@link #onSpriteBoundsExited()} it prevents redundant calls to enable the sprite selection circles while the
     * mouse cursor is inside the sprite bounds.
     * </p>
     */
    public void onSpriteBoundsEntered() {
      if (!spriteBoundsEntered) {
        spriteBoundsEntered = true;
        fireCircleTimer();
      }
    }

    /**
     * Should be called when the mouse cursor is outside of the sprite bounds. This method is used together with
     * {@link #onSpriteBoundsEntered()}.
     */
    public void onSpriteBoundsExited() {
      if (spriteBoundsEntered) {
        spriteBoundsEntered = false;
      }
    }

    /**
     * Enables display of the selection circle for the specified amount of time before it is disabled. It is called
     * when the mouse cursor hovers over a creature sprite.
     *
     * @param millis Delay in milliseconds.
     */
    private void fireCircleTimer() {
      if (circleEndedTaskResult != null) {
        circleEndedTaskResult.cancel(true);
        circleEndedTaskResult = null;
      }
      getDecoder().setSelectionCircleEnabled(true);
      circleEndedTaskResult = CACHED_THREADPOOL.submit(circleEndedTask);
    }

    /** Returns whether the sprite animation is of the Planescape sprite type (Type F000). */
    public boolean isPstAnimation() {
      return (getDecoder().getAnimationType() == AnimationInfo.Type.MONSTER_PLANESCAPE);
    }

    /** Returns the default "walking" action sequence for this sprite type. */
    public Sequence getDefaultWalkingSequence() {
      if (isPstAnimation()) {
        return Sequence.PST_WALK;
      }
      return Sequence.WALK;
    }

    /** Returns the default "running" action sequence for this sprite type. */
    public Sequence getDefaultRunningSequence() {
      if (isPstAnimation()) {
        return Sequence.PST_RUN;
      }
      return Sequence.WALK;
    }

    /** Returns a list of available action sequences for the sprite. */
    public List<Sequence> getAvailableSequences() {
      final List<Sequence> retVal = new ArrayList<>();

      for (final Sequence seq : Sequence.values()) {
        if (getDecoder().isSequenceAvailable(seq)) {
          boolean skip = isPstAnimation() && seq.name().contains("_MISC");
          switch (seq) {
            case HIDE:
            case EMERGE:
              skip = true;
              break;
            default:
              break;
          }

          if (!skip) {
            retVal.add(seq);
          }
        }
      }

      return retVal;
    }

    /** Determines the most suitable {@link Direction} that describes the vector {@code (x1, y1) -> (x2, y2)}. */
    public static Direction findDirection(double x1, double y1, double x2, double y2) {
      Direction retVal = Direction.S;

      final Point2D.Double vec = getUnitVector(x2 - x1, y2 - y1, false);
      if (vec.x == 0.0 && vec.y == 0.0) {
        return retVal;
      }

      double dist = Double.MAX_VALUE;
      for (final Direction dir : Direction.values()) {
        final Point2D.Double base = DIRECTION.get(dir);
        double nom = (vec.x * base.x + vec.y * base.y);
        double den = Math.sqrt(vec.x*vec.x + vec.y*vec.y) * Math.sqrt(base.x*base.x + base.y*base.y);
        double theta = Math.acos(nom / den);
        double thetaAbs = Math.abs(theta);
        if (!Double.isNaN(theta) && thetaAbs < dist) {
          retVal = dir;
          dist = thetaAbs;
          if (dist == 0.0) {
            break;
          }
        }
      }

      return retVal;
    }

    /**
     * Used internally to produce a unit vector.
     *
     * @param x           X coordinate of the vector.
     * @param y           Y coordinate of the vector.
     * @param perspective Whether to apply perspective correction to the vertical direction.
     */
    private static Point2D.Double getUnitVector(double x, double y, boolean perspective) {
      if (perspective) {
        y *= 0.75;
      }

      final double modulus = Math.sqrt(x*x + y*y);
      if (modulus != 0.0) {
        return new Point2D.Double(x / modulus, y / modulus);
      } else {
        return new Point2D.Double();
      }
    }
  }
}
