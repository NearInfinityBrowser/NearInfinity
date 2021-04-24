// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.browser;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.infinity.gui.ViewerUtil;
import org.infinity.gui.WindowBlocker;
import org.infinity.resource.cre.browser.icon.Icons;
import org.infinity.resource.cre.decoder.SpriteDecoder;
import org.infinity.resource.cre.decoder.SpriteDecoder.SpriteBamControl;
import org.infinity.resource.cre.decoder.util.Direction;
import org.infinity.resource.cre.decoder.util.Sequence;

/**
 * This panel provides controls for animation playback and related visual options.
 */
public class MediaPanel extends JPanel
{
  private static boolean isLoop;

  static {
    isLoop = true;
  }

  // mapping of slider value to direction
  private final HashMap<Integer, Direction> directionMap = new HashMap<>();
  private final Listeners listeners = new Listeners();
  private final CreatureBrowser browser;

  private JButton bHome, bEnd, bStepBack, bStepForward, bPlay, bStop;
  private DefaultComboBoxModel<Sequence> modelSequences;
  private JComboBox<Sequence> cbSequences;
  private JCheckBox cbLoop;
  private JSlider slDirection;
  private JLabel lDirection;
  private JLabel lFrameCur, lFrameMax;
  private SpriteBamControl controller;
  private Timer timer;
  private int curFrame, curCycle;

  public MediaPanel(CreatureBrowser browser)
  {
    super();
    this.browser = Objects.requireNonNull(browser);
    init();
  }

  /** Returns the associated {@code CreatureBrowser} instance. */
  public CreatureBrowser getBrowser() { return browser; }

  /**
   * Discards the current animation state and initializes a new animation.
   * @param preserveState whether current media control states (sequence, frame index, ...) should be preserved if possible.
   */
  public void reset(boolean preserveState)
  {
    Sequence oldSequence = preserveState ? getSequence() : null;
    Direction oldDir = preserveState ? getDirection(getCurrentDirection()) : null;
    int oldFrameIdx = preserveState ? getCurrentFrame() : 0;
    boolean isRunning = preserveState ? isRunning() : false;

    stop();
    modelSequences.removeAllElements();
    setCurrentDirection(0);
    slDirection.setMinimum(0);
    slDirection.setMaximum(0);
    directionMap.clear();
    controller = null;
    curFrame = curCycle = -1;
    updateControls();

    SpriteDecoder decoder = getBrowser().getDecoder();
    if (decoder == null) {
      return;
    }

    SettingsPanel settings = getBrowser().getSettingsPanel();
    decoder.setAutoApplyChanges(false);
    decoder.setPaletteReplacementEnabled(settings.isPaletteReplacementEnabled());
    decoder.setTintEnabled(settings.isTintEnabled());
    decoder.setBlurEnabled(settings.isBlurEnabled());
    decoder.setTranslucencyEnabled(settings.isTranslucencyEnabled());
    decoder.setSelectionCircleEnabled(settings.isSelectionCircleEnabled());
    decoder.setSelectionCircleBitmap(settings.isOrnateSelectionCircle());
    decoder.setPersonalSpaceVisible(settings.isPersonalSpaceEnabled());
    decoder.setRenderAvatar(settings.isAvatarVisible());
    decoder.setRenderHelmet(settings.isHelmetVisible());
    decoder.setRenderShield(settings.isShieldVisible());
    decoder.setRenderWeapon(settings.isWeaponVisible());
    decoder.setBoundingBoxVisible(settings.isOverlayBordersVisible());
    decoder.applyAnimationChanges();
    decoder.setAutoApplyChanges(true);

    setController(decoder.createControl());
    getBrowser().getRenderPanel().setComposite(getBrowser().getSettingsPanel().getComposite());
    initSequences(decoder, oldSequence);
    initDirection(decoder, oldDir);
    setCurrentFrame(oldFrameIdx);
    updateControls();
    setRunning(isRunning);
  }

  /** Returns the currently assigned BAM controller for the creature animation. */
  public SpriteBamControl getController() { return controller; }

  /** Sets the BAM controller for the creature animation. */
  protected void setController(SpriteBamControl controller)
  {
    if (this.controller == null && controller != null ||
        this.controller != null && !this.controller.equals(controller)) {
      pause();
      this.controller = controller;
      if (this.controller != null) {
        this.controller.setMode(SpriteBamControl.Mode.SHARED);
        this.controller.setSharedPerCycle(false);
      }
    }
  }

  /** Returns the current frame rate. Rounding errors may occur. */
  public int getFrameRate()
  {
    int retVal = 0;
    if (timer != null && timer.getDelay() > 0) {
      retVal = 1000 / timer.getDelay();
    }
    return retVal;
  }

  /**
   * Sets the frame rate for playback.
   * @param fps the frame rate in frames per seconds.
   * @throws IllegalArgumentException if frame rate lies outside of supported range [1, 60].
   */
  public void setFrameRate(int fps) throws IllegalArgumentException
  {
    if (fps < 1 || fps > 60) {
      throw new IllegalArgumentException("Unsupported frame rate: " + fps + " fps");
    }
    int delay = 1000 / fps;
    if (timer == null) {
      timer = new Timer(delay, listeners);
    } else {
      timer.setDelay(delay);
    }
  }

  /** Returns the currently displayed BAM frame. */
  public int getCurrentFrame()
  {
    if (getController() != null) {
      return getController().cycleGetFrameIndex();
    }
    return -1;
  }

  /**
   * Sets the current BAM frame and updates display accordingly. Does nothing if no BAM controller is available.
   * @param frameIdx the new frame index in the current cycle. Frame index is capped to the available range of cycle frames.
   */
  public void setCurrentFrame(int frameIdx)
  {
    if (getController() != null) {
      if (curFrame != frameIdx || curCycle != getController().cycleGet()) {
        frameIdx = Math.max(0, Math.min(getController().cycleFrameCount() - 1, frameIdx));
        getController().cycleSetFrameIndex(frameIdx);
        curFrame = getController().cycleGetFrameIndex();
        getBrowser().getRenderPanel().setFrame(getController());
        getBrowser().getRenderPanel().updateCanvas();
        updateLabels();
      }
    }
  }

  /** Returns the total number of frames in the current cycle. */
  public int getMaxFrame()
  {
    if (getController() != null) {
      return getController().cycleFrameCount();
    }
    return -1;
  }

  /** Returns the selected cycle index. */
  public int getCurrentCycle()
  {
    if (getController() != null) {
      return getController().cycleGet();
    }
    return -1;
  }

  /**
   * Sets the active cycle and updates display accordingly.
   * @param cycleIdx the new cycle index.
   * @throws IndexOutOfBoundsException if the cycle index does not exist.
   */
  public void setCurrentCycle(int cycleIdx) throws IndexOutOfBoundsException
  {
    if (getController() != null) {
      if (curCycle != cycleIdx) {
        if (!getController().cycleSet(cycleIdx)) {
          throw new IndexOutOfBoundsException("Cycle index is out of bounds: " + cycleIdx);
        }
        setCurrentFrame(curFrame);
        curCycle = getController().cycleGet();
      }
    }
  }

  /** Returns the current direction slider position as numeric value. */
  public int getCurrentDirection() { return slDirection.getValue(); }

  /** Sets the specified direction index if available. Throws an {@code IndexOutOfBoundsException} otherwise. */
  public void setCurrentDirection(int pos) throws IndexOutOfBoundsException
  {
    if (pos >= slDirection.getMinimum() && pos <= slDirection.getMaximum()) {
      slDirection.setValue(pos);
    } else {
      throw new IndexOutOfBoundsException("Direction value out of bounds: " + pos);
    }
  }

  /** Sets the specified direction if available. Throws an {@code IllegalArgumentException} otherwise. */
  public void setCurrentDirection(Direction dir) throws IllegalArgumentException
  {
    if (dir != null) {
      for (int i = 0; i < slDirection.getMaximum(); i++) {
        if (dir.equals(getDirection(i))) {
          slDirection.setValue(i);
          return;
        }
      }
      throw new IllegalArgumentException("Requested direction is not available: " + dir);
    } else {
      slDirection.setValue(0);
    }
  }

  /** Returns the currently selected animation sequence. Returns {@code null} if no sequence is active. */
  public Sequence getSequence()
  {
    return modelSequences.getElementAt(cbSequences.getSelectedIndex());
  }

  /** Sets the specified sequence and loads the associated animation. */
  public void setSequence(Sequence seq) throws IllegalArgumentException
  {
    int oldIdx = cbSequences.getSelectedIndex();
    int idx = modelSequences.getIndexOf(seq);
    if (idx >= 0) {
      if (idx != oldIdx) {
        curCycle = curFrame = -1;
        cbSequences.setSelectedIndex(idx);
      }
    } else {
      throw new IllegalArgumentException("Specified sequence is not available");
    }
  }

  public void loadSequence(Sequence seq) throws IllegalArgumentException
  {
    SpriteDecoder decoder = getBrowser().getDecoder();
    RenderPanel renderer = getBrowser().getRenderPanel();
    Direction oldDir = getDirection(getCurrentDirection());
    boolean isRunning = isRunning();
    pause();
    try {
      if (!getBrowser().getDecoder().loadSequence(seq)) {
        throw new Exception();
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Could not load animation sequence");
    }

    setController(decoder.createControl());
    initDirection(decoder, oldDir);
    if (curCycle >= 0 && curCycle < getController().cycleCount()) {
      getController().cycleSet(curCycle);
      if (curFrame >= 0 && curFrame < getController().cycleFrameCount()) {
        getController().cycleSetFrameIndex(curFrame);
      }
    }
    curCycle = getController().cycleGet();
    curFrame = getController().cycleGetFrameIndex();

    renderer.setFrame(getController());
    renderer.updateCanvas();
    setRunning(isRunning);
  }

  /** Returns whether playback is repeated when the end of the animation is reached. */
  public boolean isLooping() { return cbLoop.isSelected(); }

  /** Returns whether playback is enabled. */
  public boolean isRunning()
  {
    if (timer != null) {
      return timer.isRunning();
    }
    return false;
  }

  /** Enables or disables playback of the currently active animation sequence. */
  public void setRunning(boolean b)
  {
    if (b != isRunning()) {
      if (b) {
        timer.start();
      } else {
        timer.stop();
      }
      String iconName = isRunning() ? Icons.ICON_PAUSE : Icons.ICON_PLAY;
      bPlay.setIcon(Icons.getIcon(iconName));
      updateControls();
    }
  }

  /** Advances current animation by one frame. Takes loop into account. Updates controls. */
  public void advanceFrame()
  {
    if (getMaxFrame() > 0) {
      int frameIdx = getCurrentFrame() + 1;
      if (isLooping()) {
        frameIdx %= getMaxFrame();
      }

      if (frameIdx < getMaxFrame()) {
        setCurrentFrame(frameIdx);
      } else {
        pause();
      }
    }
  }

  /** Starts playback of the active BAM cycle. Does nothing if playback is already enabled. */
  public void play()
  {
    if (!isRunning()) {
      setRunning(true);
      updateControls();
    }
  }

  /**
   * Stops playback of the active BAM cycle without resetting frame position.
   * Does nothing if playback is already disabled.
   */
  public void pause()
  {
    if (isRunning()) {
      setRunning(false);
      updateControls();
    }
  }

  /**
   * Stops playback of the active BAM cycle and resets frame position.
   */
  public void stop()
  {
    setRunning(false);
    setCurrentFrame(0);
    updateControls();
  }

  private void init()
  {
    GridBagConstraints c = new GridBagConstraints();

    // frame info
    JLabel l1 = new JLabel("Frame:");
    JLabel l2 = new JLabel("/");
    lFrameCur = new JLabel("0");
    lFrameMax = new JLabel("0");

    JPanel pRow1 = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pRow1.add(new JPanel(), c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pRow1.add(l1, c);
    c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pRow1.add(lFrameCur, c);
    c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pRow1.add(l2, c);
    c = ViewerUtil.setGBC(c, 4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pRow1.add(lFrameMax, c);
    c = ViewerUtil.setGBC(c, 5, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pRow1.add(new JPanel(), c);


    // playback controls
    bHome = new JButton(Icons.getIcon(Icons.ICON_HOME));
    bHome.addActionListener(listeners);

    bStepBack = new JButton(Icons.getIcon(Icons.ICON_STEP_BACK));
    bStepBack.addActionListener(listeners);

    bPlay = new JButton(Icons.getIcon(Icons.ICON_PLAY));
    bPlay.addActionListener(listeners);

    bStop = new JButton(Icons.getIcon(Icons.ICON_STOP));
    bStop.addActionListener(listeners);

    bStepForward= new JButton(Icons.getIcon(Icons.ICON_STEP_FORWARD));
    bStepForward.addActionListener(listeners);

    bEnd = new JButton(Icons.getIcon(Icons.ICON_END));
    bEnd.addActionListener(listeners);

    JPanel pRow2 = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pRow2.add(bHome, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 0, 0);
    pRow2.add(bStepBack, c);
    c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 0, 0);
    pRow2.add(bPlay, c);
    c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 0, 0);
    pRow2.add(bStop, c);
    c = ViewerUtil.setGBC(c, 4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 0, 0);
    pRow2.add(bStepForward, c);
    c = ViewerUtil.setGBC(c, 5, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 0, 0);
    pRow2.add(bEnd, c);


    // direction controls
    slDirection = new JSlider(SwingConstants.HORIZONTAL);
    slDirection.setSnapToTicks(true);
    slDirection.setPaintLabels(true);
    slDirection.setPaintTicks(true);
    slDirection.setSnapToTicks(true);
    slDirection.addChangeListener(listeners);
    l1 = new JLabel("Direction:");
    lDirection = new JLabel("S");

    JPanel pRow3 = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pRow3.add(new JPanel(), c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 0, 0, 0), 0, 0);
    pRow3.add(l1, c);
    c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 4, 0, 0), 0, 0);
    pRow3.add(lDirection, c);
    c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pRow3.add(new JPanel(), c);
    c = ViewerUtil.setGBC(c, 0, 1, 4, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pRow3.add(slDirection, c);


    // combining all rows
    JPanel pColumn1 = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pColumn1.add(pRow1, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pColumn1.add(pRow2, c);
    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    pColumn1.add(pRow3, c);


    // sequence selection, loop option
    l1 = new JLabel("Sequence:");
    modelSequences = new DefaultComboBoxModel<>();
    cbSequences = new JComboBox<>(modelSequences);
    cbSequences.setPrototypeDisplayValue(Sequence.ATTACK_2WEAPONS1);
    cbSequences.addItemListener(listeners);

    cbLoop = new JCheckBox("Loop", isLoop);

    JPanel pColumn2 = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pColumn2.add(l1, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    pColumn2.add(cbSequences, c);
    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pColumn2.add(cbLoop, c);


    // combining panels
    JPanel panelMain = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    panelMain.add(new JPanel(), c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 0, 8, 0), 0, 0);
    panelMain.add(pColumn1, c);
    c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 32, 8, 0), 0, 0);
    panelMain.add(pColumn2, c);
    c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    panelMain.add(new JPanel(), c);

    JScrollPane scrollMedia = new JScrollPane(panelMain);
    scrollMedia.setBorder(panelMain.getBorder());

    setLayout(new BorderLayout());
    add(scrollMedia, BorderLayout.CENTER);

    // default settings
    curFrame = curCycle = -1;
    setFrameRate(15);
    updateControls();
  }

  /** Updates media controls based on the current animation state. */
  private void updateControls()
  {
    boolean loaded = getSequence() != null;
    boolean running = isRunning();
    bHome.setEnabled(running || loaded && getCurrentFrame() > 0);
    bEnd.setEnabled(running || loaded && getCurrentFrame() < getMaxFrame() - 1);
    bStepBack.setEnabled(running || loaded && getCurrentFrame() > 0);
    bStepForward.setEnabled(running || loaded && getCurrentFrame() < getMaxFrame() - 1);
    bPlay.setEnabled(loaded);
    bStop.setEnabled(running);
    cbSequences.setEnabled(modelSequences.getSize() > 0);
    cbLoop.setEnabled(loaded);
    slDirection.setEnabled(loaded && slDirection.getMaximum() > slDirection.getMinimum());
    updateLabels();
  }

  /** Updates labels bvased on current control and animation state. */
  private void updateLabels()
  {
    lFrameCur.setText(Integer.toString(getCurrentFrame()));
    lFrameMax.setText(Integer.toString(getMaxFrame() - 1));
    Direction dir = getDirection(getCurrentDirection());
    lDirection.setText(dir.toString());
    String text = "";
    if (getController() != null) {
      int cycle = getController().getDecoder().getDirectionMap().getOrDefault(dir, -1);
      text = "Cycle: " + cycle;
    }
    lDirection.setToolTipText(text);
  }

  /** Initializes the sequence list with available animation sequences and preselects a suitable default sequence. */
  private void initSequences(SpriteDecoder decoder, Sequence defSeq)
  {
    modelSequences.removeAllElements();
    if (decoder != null) {
      for (final Sequence seq : Sequence.values()) {
        if (decoder.isSequenceAvailable(seq)) {
          modelSequences.addElement(seq);
        }
      }
    }
    cbSequences.setEnabled(modelSequences.getSize() > 0);

    if (modelSequences.getSize() > 0) {
      int idx = -1;
      if (defSeq != null) {
        // try given default sequence first
        idx = modelSequences.getIndexOf(defSeq);
      }
      if (idx < 0) {
        // try default sequence from list
        for (final Sequence seq : Sequence.getDefaultSequences()) {
          idx = modelSequences.getIndexOf(seq);
          if (idx >= 0) {
            break;
          }
        }
      }
      if (idx < 0) {
        // fall back to first available sequence
        idx = 0;
      }
      cbSequences.setSelectedIndex(idx);
    }
  }

  /** Initializes the direction slider and label with available directions. */
  private void initDirection(SpriteDecoder decoder, Direction defDir)
  {
    // discarding old data
    slDirection.setMinimum(0);
    slDirection.setMaximum(0);
    directionMap.clear();

    if (decoder != null) {
      // collecting directions
      List<Integer> directions = new ArrayList<>(Direction.values().length * 2 + 1);
      for (final Direction dir : decoder.getDirectionMap().keySet()) {
        directions.add(Integer.valueOf(dir.getValue()));
      }

      // sorting in descending order: maps relative slider positions to more natural directions
      Collections.sort(directions, (a, b) -> b - a);

      int min = -directions.size() + 1;
      int max = directions.size();
      // duplicate list entries
      directions.addAll(new ArrayList<>(directions));
      // remove excess entries from left (negative) side
      while (directions.size() > 1 && directions.get(0) > Direction.N.getValue()) {
        directions.remove(0);
        min++;
      }
      // remove excess entries from right (positive) side
      while (directions.size() > 1 && directions.get(directions.size() - 1) < Direction.N.getValue()) {
        directions.remove(directions.size() - 1);
        max--;
      }

      for (int i = min; i <= max; i++) {
        int dirVal = directions.get(i - min);
        Direction dir = Direction.from(dirVal);
        directionMap.put(Integer.valueOf(i), dir);
      }

      // initializing slider
      slDirection.setMinimum(min);
      slDirection.setMaximum(max);
      // defining major ticks distance
      Integer d1 = decoder.getDirectionMap().get(Direction.S);
      if (d1 != null) {
        Integer d2 = decoder.getDirectionMap().get(Direction.W);
        if (d2 != null) {
          slDirection.setMajorTickSpacing(d2 - d1);
        } else {
          d2 = decoder.getDirectionMap().get(Direction.N);
          if (d2 != null) {
            slDirection.setMajorTickSpacing((d2 - d1) / 2);
          }
        }
      }
      slDirection.setMinorTickSpacing(1);

      // defining direction labels
      Hashtable<Integer, JLabel> labels = new Hashtable<>();
      for (int i = min; i <= max; i++) {
        Direction dir = getDirection(i);
        if (dir != null && (dir.getValue() % 4) == 0) {
          labels.put(Integer.valueOf(i), new JLabel(dir.toString()));
        }
      }
      slDirection.setLabelTable(labels);

      // restoring default direction
      int value = 0;
      defDir = decoder.getExistingDirection(defDir);
      if (defDir != null) {
        for (int i = slDirection.getMinimum(); i <= slDirection.getMaximum(); i++) {
          if (defDir.equals(getDirection(i))) {
            value = i;
            break;
          }
        }
      }
      slDirection.setValue(value);
    }
  }

  /** Returns the {@code Direction} of the specified direction slider position. Defaults to {@code Direction.S}. */
  private Direction getDirection(int index)
  {
    return directionMap.getOrDefault(Integer.valueOf(index), Direction.S);
  }

//-------------------------- INNER CLASSES --------------------------

  /**
   * Listeners are outsourced to this class for cleaner code.
   */
  private class Listeners implements ActionListener, ChangeListener, ItemListener
  {
    public Listeners()
    {
    }

    //--------------------- Begin Interface ActionListener ---------------------

    @Override
    public void actionPerformed(ActionEvent e)
    {
      if (e.getSource() == timer) {
        advanceFrame();
      }
      else if (e.getSource() == bHome) {
        setCurrentFrame(0);
        updateControls();
      }
      else if (e.getSource() == bEnd) {
        setCurrentFrame(getMaxFrame() - 1);
        updateControls();
      }
      else if (e.getSource() == bStepBack) {
        if (getCurrentFrame() > 0) {
          setCurrentFrame(getCurrentFrame() - 1);
          updateControls();
        }
      }
      else if (e.getSource() == bStepForward) {
        if (getCurrentFrame() < getMaxFrame() - 1) {
          setCurrentFrame(getCurrentFrame() + 1);
          updateControls();
        }
      }
      else if (e.getSource() == bPlay) {
        setRunning(!isRunning());
      }
      else if (e.getSource() == bStop) {
        stop();
      }
    }

    //--------------------- End Interface ActionListener ---------------------

    //--------------------- Begin Interface ChangeListener ---------------------
    @Override
    public void stateChanged(ChangeEvent e)
    {
      if (e.getSource() == slDirection) {
        Direction dir = getDirection(slDirection.getValue());
        lDirection.setText(dir.toString());
        int cycle = getBrowser().getDecoder().getDirectionMap().getOrDefault(dir, -1).intValue();
        if (cycle >= 0) {
          setCurrentCycle(cycle);
        }
        updateControls();
      }
    }

    //--------------------- End Interface ChangeListener ---------------------

    //--------------------- Begin Interface ItemListener ---------------------
    @Override
    public void itemStateChanged(ItemEvent e)
    {
      if (e.getSource() == cbSequences) {
        if (e.getStateChange() == ItemEvent.SELECTED &&
            e.getItem() instanceof Sequence) {
          final Sequence seq = (Sequence)e.getItem();
          try {
            WindowBlocker.blockWindow(getBrowser(), true);
            loadSequence(seq);
          } catch (Exception ex) {
            ex.printStackTrace();
            getBrowser().showErrorMessage(ex.getMessage(), "Sequence selection");
          } finally {
            WindowBlocker.blockWindow(getBrowser(), false);
          }
        }
      }
    }

    //--------------------- End Interface ItemListener ---------------------
  }
}
