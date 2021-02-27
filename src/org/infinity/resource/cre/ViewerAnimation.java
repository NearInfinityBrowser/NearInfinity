// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.infinity.NearInfinity;
import org.infinity.gui.ButtonPanel;
import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.RenderCanvas;
import org.infinity.gui.ViewerUtil;
import org.infinity.gui.WindowBlocker;
import org.infinity.icon.Icons;
import org.infinity.resource.cre.decoder.SpriteDecoder;
import org.infinity.resource.cre.decoder.SpriteUtils;
import org.infinity.resource.graphics.PseudoBamDecoder.PseudoBamControl;

/**
 * A basic creature animation viewer.
 */
public class ViewerAnimation extends ChildFrame implements ActionListener
{
  private static final Color TransparentColor = new Color(0, true);
  private static final int ANIM_DELAY = 1000 / 15;    // 15 fps in milliseconds

  private static final ButtonPanel.Control CtrlNextCycle      = ButtonPanel.Control.CUSTOM_1;
  private static final ButtonPanel.Control CtrlPrevCycle      = ButtonPanel.Control.CUSTOM_2;
  private static final ButtonPanel.Control CtrlNextFrame      = ButtonPanel.Control.CUSTOM_3;
  private static final ButtonPanel.Control CtrlPrevFrame      = ButtonPanel.Control.CUSTOM_4;
  private static final ButtonPanel.Control CtrlPlay           = ButtonPanel.Control.CUSTOM_5;
  private static final ButtonPanel.Control CtrlCycleLabel     = ButtonPanel.Control.CUSTOM_6;
  private static final ButtonPanel.Control CtrlFrameLabel     = ButtonPanel.Control.CUSTOM_7;
  private static final ButtonPanel.Control CtrlSequenceLabel  = ButtonPanel.Control.CUSTOM_8;
  private static final ButtonPanel.Control CtrlSequenceList   = ButtonPanel.Control.CUSTOM_9;
  private static final ButtonPanel.Control CtrlShowCircle     = ButtonPanel.Control.CUSTOM_10;
  private static final ButtonPanel.Control CtrlShowSpace      = ButtonPanel.Control.CUSTOM_11;
  private static final ButtonPanel.Control CtrlZoom           = ButtonPanel.Control.CUSTOM_12;

  // List of potential sequences to display when loading a new creature
  private static final List<SpriteDecoder.Sequence> InitialSequences = new ArrayList<SpriteDecoder.Sequence>() {{
    add(SpriteDecoder.Sequence.STAND);
    add(SpriteDecoder.Sequence.STAND2);
    add(SpriteDecoder.Sequence.STAND3);
    add(SpriteDecoder.Sequence.STAND_EMERGED);
    add(SpriteDecoder.Sequence.PST_STAND);
    add(SpriteDecoder.Sequence.STANCE);
    add(SpriteDecoder.Sequence.STANCE2);
    add(SpriteDecoder.Sequence.PST_STANCE);
    add(SpriteDecoder.Sequence.WALK);
    add(SpriteDecoder.Sequence.PST_WALK);
  }};

  private static boolean zoom = false;
  private static boolean showSelectionCircle = false;
  private static boolean showPersonalSpace = false;

  private final ButtonPanel buttonControlPanel = new ButtonPanel();
  private final CreResource cre;

  private SpriteDecoder decoder;
  private PseudoBamControl bamControl;
  private RenderCanvas rcDisplay;
  private int curCycle, curFrame;
  private Timer timer;
  private SpriteDecoder.Sequence sequence;

  public ViewerAnimation(CreResource cre)
  {
    super("", true);
    this.cre = Objects.requireNonNull(cre);
    try {
      this.decoder = SpriteDecoder.importSprite(getCre());

      init();
    } catch (Exception e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(this, "Creature animation could not be loaded.\nError message: " + e.getMessage(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
      this.bamControl = null;
      this.decoder = null;
      close();
      return;
    }
  }

  public CreResource getCre()
  {
    return cre;
  }

  /** Returns the associated {@code SpriteDecoder} instance. */
  public SpriteDecoder getDecoder()
  {
    return decoder;
  }

  /** Returns the {@code BamControl} instance linked to the {@code SpriteDecoder}. */
  public PseudoBamControl getController()
  {
    return bamControl;
  }

  private void setController(PseudoBamControl ctrl)
  {
    this.bamControl = Objects.requireNonNull(ctrl, "BamControl cannot be null");
  }

  /** Returns the selected animation sequence. */
  public SpriteDecoder.Sequence getAnimationSequence()
  {
    return sequence;
  }

  /** Loads a new animation sequence. */
  private void setAnimationSequence(SpriteDecoder.Sequence seq) throws Exception
  {
    if (seq != null && seq != getAnimationSequence()) {
      sequence = seq;
      curFrame = 0;
      getDecoder().loadSequence(seq);
      resetAnimationSequence();
      showFrame();
    }
  }

  private void resetAnimationSequence() throws Exception
  {
    setController(getDecoder().createControl());
    getController().setMode(PseudoBamControl.Mode.SHARED);
    getController().setSharedPerCycle(false);
    if (curCycle < getController().cycleCount()) {
      getController().cycleSet(curCycle);
      if (curFrame < getController().cycleFrameCount()) {
        getController().cycleSetFrameIndex(curFrame);
      }
    }
    curCycle = getController().cycleGet();
    curFrame = getController().cycleGetFrameIndex();
    updateCanvasSize();
  }

  public void updateCanvasSize()
  {
    int zoom = isZoomed() ? 2 : 1;
    Dimension dim = getController().getSharedDimension();
    Dimension dimDisplay = new Dimension(dim.width * zoom, dim.height * zoom);
    boolean imageChanged = !dim.equals(new Dimension(rcDisplay.getImage().getWidth(null), rcDisplay.getImage().getHeight(null)));
    boolean sizeChanged = !dimDisplay.equals(rcDisplay.getPreferredSize());
    if (imageChanged || sizeChanged) {
      rcDisplay.setImage(new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB));
      if (sizeChanged) {
        rcDisplay.setPreferredSize(dimDisplay);
        Container c = SwingUtilities.getAncestorOfClass(JScrollPane.class, rcDisplay);
        if (c != null) {
          c.setMinimumSize(rcDisplay.getPreferredSize());
          c.invalidate();
          c.getParent().validate();
        }
      }
    }
    updateCanvas();
  }

  public void updateCanvas()
  {
    BufferedImage image = (BufferedImage)rcDisplay.getImage();
    Graphics2D g = image.createGraphics();
    try {
      g.setComposite(AlphaComposite.Src);
      g.setColor(TransparentColor);
      g.fillRect(0, 0, image.getWidth(), image.getHeight());
    } finally {
      g.dispose();
      g = null;
    }

    // rendering new frame
    getController().cycleGetFrame(image);
    rcDisplay.repaint();
  }

  /** Returns whether animation is zoomed. */
  public boolean isZoomed()
  {
    return ((JCheckBox)buttonControlPanel.getControlByType(CtrlZoom)).isSelected();
  }

  /** Returns whether the animation is played back. */
  public boolean isPlaying()
  {
    if (timer == null) {
      timer = new Timer(ANIM_DELAY, this);
    }
    return timer.isRunning();
  }

  /** Toggles playback between "play" and "pause". */
  public void togglePlay()
  {
    if (isPlaying()) {
      pause();
    } else {
      play();
    }
  }

  /** Starts playback. Does nothing if animation is already played back. */
  public void play()
  {
    if (!isPlaying()) {
      timer.restart();
    }
  }

  /** Stops playback. Does nothing if animation is already stopped. */
  public void pause()
  {
    if (isPlaying()) {
      timer.stop();
    }
  }

  /** Rewinds animation of current cycle to first frame. */
  public void rewind()
  {
    curFrame = 0;
    showFrame();
  }

//--------------------- Begin Class ChildFrame ---------------------

  @Override
  protected boolean windowClosing(boolean forced) throws Exception
  {
    SpriteUtils.clearCache();
    return true;
  }

//--------------------- End Class ChildFrame ---------------------

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (buttonControlPanel.getControlByType(CtrlSequenceList) == event.getSource()) {
      JComboBox<?> cb = (JComboBox<?>)buttonControlPanel.getControlByType(CtrlSequenceList);
      SpriteDecoder.Sequence seq = (SpriteDecoder.Sequence)(cb).getSelectedItem();
      try {
        WindowBlocker.blockWindow(this, true);
        setAnimationSequence(seq);
        updateControls();
      } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        cb.setSelectedItem(getAnimationSequence());
      } finally {
        WindowBlocker.blockWindow(this, false);
      }
    }
    else if (buttonControlPanel.getControlByType(CtrlZoom) == event.getSource()) {
      try {
        WindowBlocker.blockWindow(this, true);
        zoom = ((JCheckBox)buttonControlPanel.getControlByType(CtrlZoom)).isSelected();
        updateCanvasSize();
      } finally {
        WindowBlocker.blockWindow(this, false);
      }
    }
    else if (buttonControlPanel.getControlByType(CtrlShowCircle) == event.getSource()) {
      try {
        WindowBlocker.blockWindow(this, true);
        showSelectionCircle = ((JCheckBox)buttonControlPanel.getControlByType(CtrlShowCircle)).isSelected();
        getDecoder().setSelectionCircleEnabled(showSelectionCircle);
        resetAnimationSequence();
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        WindowBlocker.blockWindow(this, false);
      }
    }
    else if (buttonControlPanel.getControlByType(CtrlShowSpace) == event.getSource()) {
      try {
        WindowBlocker.blockWindow(this, true);
        showPersonalSpace = ((JCheckBox)buttonControlPanel.getControlByType(CtrlShowSpace)).isSelected();
        getDecoder().setPersonalSpaceVisible(showPersonalSpace);
        resetAnimationSequence();
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        WindowBlocker.blockWindow(this, false);
      }
    }
    else if (buttonControlPanel.getControlByType(CtrlPrevCycle) == event.getSource()) {
      if (curCycle > 0) {
        curCycle--;
        getController().cycleSet(curCycle);
        if (isPlaying() && getController().cycleFrameCount() == 0) {
          pause();
          ((JToggleButton)buttonControlPanel.getControlByType(CtrlPlay)).setSelected(false);
        }
        rewind();
        showFrame();
      }
    }
    else if (buttonControlPanel.getControlByType(CtrlNextCycle) == event.getSource()) {
      if (curCycle < getController().cycleCount() - 1) {
        curCycle++;
        getController().cycleSet(curCycle);
        if (isPlaying() && getController().cycleFrameCount() == 0) {
          pause();
          ((JToggleButton)buttonControlPanel.getControlByType(CtrlPlay)).setSelected(false);
        }
        rewind();
        showFrame();
      }
    }
    else if (buttonControlPanel.getControlByType(CtrlPrevFrame) == event.getSource()) {
      if (curFrame > 0) {
        curFrame--;
        showFrame();
      }
    }
    else if (buttonControlPanel.getControlByType(CtrlNextFrame) == event.getSource()) {
      if (curFrame < getController().cycleFrameCount() - 1) {
        curFrame++;
        showFrame();
      }
    }
    else if (buttonControlPanel.getControlByType(CtrlPlay) == event.getSource()) {
      if (((JToggleButton)buttonControlPanel.getControlByType(CtrlPlay)).isSelected()) {
        play();
      } else {
        pause();
      }
      updateControls();
    }
    else if (timer == event.getSource()) {
      curFrame += 1;
      curFrame %= getController().cycleFrameCount();
      showFrame();
    }
  }

//--------------------- End Interface ActionListener ---------------------

  private void init() throws Exception
  {
    Dimension dim = new Dimension(1, 1);
    rcDisplay = new RenderCanvas(new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB));
    rcDisplay.setHorizontalAlignment(SwingConstants.CENTER);
    rcDisplay.setVerticalAlignment(SwingConstants.CENTER);
    rcDisplay.setInterpolationType(RenderCanvas.TYPE_NEAREST_NEIGHBOR);
    rcDisplay.setScalingEnabled(true);
    JScrollPane scrollDisplay = new JScrollPane(rcDisplay, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollDisplay.setBorder(BorderFactory.createEmptyBorder());

    JToggleButton bPlay = new JToggleButton("Play", Icons.getIcon(Icons.ICON_PLAY_16));
    bPlay.addActionListener(this);

    JLabel lCycle = new JLabel("", JLabel.CENTER);
    JButton bPrevCycle = new JButton(Icons.getIcon(Icons.ICON_BACK_16));
    bPrevCycle.setMargin(new Insets(bPrevCycle.getMargin().top, 2, bPrevCycle.getMargin().bottom, 2));
    bPrevCycle.addActionListener(this);
    JButton bNextCycle = new JButton(Icons.getIcon(Icons.ICON_FORWARD_16));
    bNextCycle.setMargin(bPrevCycle.getMargin());
    bNextCycle.addActionListener(this);

    JLabel lFrame = new JLabel("", JLabel.CENTER);
    lFrame.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    JButton bPrevFrame = new JButton(Icons.getIcon(Icons.ICON_BACK_16));
    bPrevFrame.setMargin(new Insets(bPrevFrame.getMargin().top, 2, bPrevFrame.getMargin().bottom, 2));
    bPrevFrame.addActionListener(this);
    JButton bNextFrame = new JButton(Icons.getIcon(Icons.ICON_FORWARD_16));
    bNextFrame.setMargin(bPrevFrame.getMargin());
    bNextFrame.addActionListener(this);

    JLabel lSequence = new JLabel("Sequence:");
    lSequence.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    DefaultComboBoxModel<SpriteDecoder.Sequence> modelSequences = new DefaultComboBoxModel<>();
    JComboBox<SpriteDecoder.Sequence> cbSequences = new JComboBox<>(modelSequences);
    cbSequences.addActionListener(this);
    for (final SpriteDecoder.Sequence seq : SpriteDecoder.Sequence.values()) {
      if (getDecoder().isSequenceAvailable(seq)) {
        modelSequences.addElement(seq);
      }
    }
    cbSequences.setEnabled(cbSequences.getItemCount() > 0);

    JCheckBox cbZoom = new JCheckBox("Zoom", zoom);
    cbZoom.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    cbZoom.addActionListener(this);
    getDecoder().setSelectionCircleEnabled(showSelectionCircle);
    JCheckBox cbShowCircle = new JCheckBox("Show selection circle", getDecoder().isSelectionCircleEnabled());
    cbShowCircle.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    cbShowCircle.addActionListener(this);
    getDecoder().setPersonalSpaceVisible(showPersonalSpace);
    JCheckBox cbShowSpace = new JCheckBox("Show personal space", getDecoder().isPersonalSpaceVisible());
    cbShowSpace.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    cbShowSpace.addActionListener(this);

    buttonControlPanel.addControl(lCycle, CtrlCycleLabel);
    buttonControlPanel.addControl(bPrevCycle, CtrlPrevCycle);
    buttonControlPanel.addControl(bNextCycle, CtrlNextCycle);
    buttonControlPanel.addControl(lFrame, CtrlFrameLabel);
    buttonControlPanel.addControl(bPrevFrame, CtrlPrevFrame);
    buttonControlPanel.addControl(bNextFrame, CtrlNextFrame);
    buttonControlPanel.addControl(bPlay, CtrlPlay);
    buttonControlPanel.addControl(lSequence, CtrlSequenceLabel);
    buttonControlPanel.addControl(cbSequences, CtrlSequenceList);
    buttonControlPanel.addControl(cbZoom, CtrlZoom);
    buttonControlPanel.addControl(cbShowCircle, CtrlShowCircle);
    buttonControlPanel.addControl(cbShowSpace, CtrlShowSpace);

    setLayout(new GridBagLayout());
    GridBagConstraints c;
    c = ViewerUtil.setGBC(null, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    add(scrollDisplay, c);
    c = ViewerUtil.setGBC(null, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 0, 8, 0), 0, 0);
    add(buttonControlPanel, c);

    String name = getCre().getName();
    if (!name.isEmpty()) {
      setTitle(String.format("%s (%s)", getCre().getName(), getCre().getAttribute(CreResource.CRE_NAME)));
    } else {
      setTitle(getCre().getName());
    }
    setSize(NearInfinity.getInstance().getPreferredSize());
    Center.center(this, NearInfinity.getInstance().getBounds());
    setExtendedState(NearInfinity.getInstance().getExtendedState() & ~ICONIFIED);
    setVisible(true);

    // loading animation sequence
    if (cbSequences.isEnabled()) {
      int seqIdx = 0;
      for (final SpriteDecoder.Sequence sequence : InitialSequences) {
        int idx = ((DefaultComboBoxModel<?>)cbSequences.getModel()).getIndexOf(sequence);
        if (idx >= 0) {
          seqIdx = idx;
          break;
        }
      }
      SpriteDecoder.Sequence seq = cbSequences.getModel().getElementAt(seqIdx);
      cbSequences.setSelectedItem(seq);
      setAnimationSequence(seq);
    }
  }

  private void showFrame()
  {
    if (getController() == null) {
      return;
    }

    if (!getController().cycleSetFrameIndex(curFrame)) {
      getController().cycleReset();
      curFrame = 0;
    }

    updateCanvas();

    ((JLabel)buttonControlPanel.getControlByType(CtrlCycleLabel))
      .setText("Cycle: " + curCycle + "/" + (getController().cycleCount() - 1));
    ((JLabel)buttonControlPanel.getControlByType(CtrlFrameLabel))
      .setText("Frame: " + curFrame + "/" + (getController().cycleFrameCount() - 1));
    updateControls();
  }

  private void updateControls()
  {
    if (getController() != null) {
      buttonControlPanel.getControlByType(CtrlPrevFrame).setEnabled(curFrame > 0);
      buttonControlPanel.getControlByType(CtrlPrevCycle).setEnabled(curCycle > 0);
      buttonControlPanel.getControlByType(CtrlNextFrame).setEnabled(curFrame < getController().cycleFrameCount() - 1);
      buttonControlPanel.getControlByType(CtrlNextCycle).setEnabled(curCycle < getController().cycleCount() - 1);
      buttonControlPanel.getControlByType(CtrlPlay).setEnabled(getController().cycleFrameCount() > 0);
    } else {
      buttonControlPanel.getControlByType(CtrlPrevFrame).setEnabled(false);
      buttonControlPanel.getControlByType(CtrlPrevCycle).setEnabled(false);
      buttonControlPanel.getControlByType(CtrlNextFrame).setEnabled(false);
      buttonControlPanel.getControlByType(CtrlNextCycle).setEnabled(false);
      buttonControlPanel.getControlByType(CtrlPlay).setEnabled(false);
    }
  }
}
