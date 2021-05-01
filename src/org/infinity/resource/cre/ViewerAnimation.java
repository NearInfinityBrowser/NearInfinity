// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.infinity.NearInfinity;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.RenderCanvas;
import org.infinity.gui.ViewerUtil;
import org.infinity.gui.WindowBlocker;
import org.infinity.icon.Icons;
import org.infinity.resource.cre.browser.CreatureBrowser;
import org.infinity.resource.cre.decoder.SpriteDecoder;
import org.infinity.resource.cre.decoder.SpriteDecoder.SpriteBamControl;
import org.infinity.resource.cre.decoder.util.Sequence;
import org.infinity.resource.cre.decoder.util.SpriteUtils;

/**
 * A basic creature animation viewer.
 */
public class ViewerAnimation extends JComponent implements ActionListener
{
  private static final Color COLOR_TRANSPARENT = new Color(0, true);
  private static final int ANIM_DELAY = 1000 / 15;    // 15 fps in milliseconds

  private static boolean zoom = false;
  private static boolean showSelectionCircle = false;
  private static boolean showPersonalSpace = false;
  private static boolean showOverlayBorders = false;

  private final CreResource cre;

  private boolean initialized;
  private SpriteDecoder decoder;
  private SpriteBamControl bamControl;
  private BufferedImage tmpImage;
  private RenderCanvas rcDisplay;
  private int curCycle, curFrame;
  private Timer timer;
  private Sequence sequence;
  private JButton bNextCycle, bPrevCycle, bNextFrame, bPrevFrame, bOpenBrowser;
  private JToggleButton bPlay;
  private JLabel lCurCycle, lCurFrame;
  private JComboBox<Sequence> cbSequences;
  private JCheckBox cbShowCircle, cbShowSpace, cbShowBorders, cbZoom;

  public ViewerAnimation(CreResource cre)
  {
    this.cre = Objects.requireNonNull(cre);
    init();
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
  public SpriteBamControl getController()
  {
    return bamControl;
  }

  private void setController(SpriteBamControl ctrl)
  {
    this.bamControl = Objects.requireNonNull(ctrl, "BamControl cannot be null");
  }

  /** Returns the selected animation sequence. */
  public Sequence getAnimationSequence()
  {
    return sequence;
  }

  /** Loads a new animation sequence. */
  private void setAnimationSequence(Sequence seq) throws Exception
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
    getController().setMode(SpriteBamControl.Mode.SHARED);
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

  /** Ensures that the canvas is big enough to contain the current creature animation sequence. */
  public void updateCanvasSize()
  {
    int zoom = isZoomed() ? 2 : 1;
    Dimension dim = getController().getSharedDimension();
    Dimension dimDisplay = new Dimension(dim.width * zoom, dim.height * zoom);
    boolean imageChanged = !dim.equals(new Dimension(rcDisplay.getImage().getWidth(null), rcDisplay.getImage().getHeight(null)));
    boolean sizeChanged = !dimDisplay.equals(rcDisplay.getPreferredSize());
    if (imageChanged || sizeChanged) {
      tmpImage = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
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

  /** Updates display with content of the current animation frame. */
  public void updateCanvas()
  {
    // pre-rendering new frame
    Graphics2D g = tmpImage.createGraphics();
    try {
      g.setComposite(AlphaComposite.Src);
      g.setColor(COLOR_TRANSPARENT);
      g.fillRect(0, 0, tmpImage.getWidth(), tmpImage.getHeight());
    } finally {
      g.dispose();
      g = null;
    }
    getController().cycleGetFrame(tmpImage);

    g = (Graphics2D)rcDisplay.getImage().getGraphics();
    try {
      // clearing old content
      g.setComposite(AlphaComposite.Src);
      g.setColor(COLOR_TRANSPARENT);
      g.fillRect(0, 0, rcDisplay.getImage().getWidth(null), rcDisplay.getImage().getHeight(null));

      // drawing markers
      getController().getVisualMarkers(g, null);

      // drawing animation frame
      g.setComposite(AlphaComposite.SrcOver);
      g.drawImage(tmpImage, 0, 0, null);
    } finally {
      g.dispose();
      g = null;
    }

    rcDisplay.repaint();
  }

  /** Returns whether animation is zoomed. */
  public boolean isZoomed()
  {
    return cbZoom.isSelected();
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

  /** Loads the creature animation associated with the current CRE resource. */
  public void open()
  {
    // loading animation on demand
    if (!isInitialized()) {
      try {
        WindowBlocker.blockWindow(true);
        initAnimation();
      } catch (Exception ex) {
        ex.printStackTrace();
        WindowBlocker.blockWindow(false);
        JOptionPane.showMessageDialog(NearInfinity.getInstance(),
                                      "Creature animation could not be loaded.\nError message: " + ex.getMessage(),
                                      "Error", JOptionPane.ERROR_MESSAGE);
      } finally {
        WindowBlocker.blockWindow(false);
      }
    }
  }

  /** Cleans up resources. */
  public void close()
  {
    pause();
    setInitialized(false);
    if (getDecoder() != null) {
      getDecoder().close();
    }
    SpriteUtils.clearCache();
    this.bamControl = null;
    this.decoder = null;
  }

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (timer == event.getSource()) {
      if (getController().cycleFrameCount() > 0) {
        curFrame = (curFrame + 1) % getController().cycleFrameCount();
      }
      showFrame();
    }
    else if (cbSequences == event.getSource()) {
      Sequence seq = cbSequences.getModel().getElementAt(cbSequences.getSelectedIndex());
      try {
        WindowBlocker.blockWindow(true);
        setAnimationSequence(seq);
        updateControls();
      } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        cbSequences.setSelectedItem(getAnimationSequence());
      } finally {
        WindowBlocker.blockWindow(false);
      }
    }
    else if (cbZoom == event.getSource()) {
      try {
        WindowBlocker.blockWindow(true);
        zoom = cbZoom.isSelected();
        updateCanvasSize();
      } finally {
        WindowBlocker.blockWindow(false);
      }
    }
    else if (cbShowCircle == event.getSource()) {
      showSelectionCircle = cbShowCircle.isSelected();
      getDecoder().setSelectionCircleEnabled(showSelectionCircle);
      updateCanvas();
    }
    else if (cbShowSpace == event.getSource()) {
      showPersonalSpace = cbShowSpace.isSelected();
      getDecoder().setPersonalSpaceVisible(showPersonalSpace);
      updateCanvas();
    }
    else if (cbShowBorders == event.getSource()) {
      try {
        WindowBlocker.blockWindow(true);
        showOverlayBorders = cbShowBorders.isSelected();
        getDecoder().setBoundingBoxVisible(showOverlayBorders);
        resetAnimationSequence();
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        WindowBlocker.blockWindow(false);
      }
    }
    else if (bPrevCycle == event.getSource()) {
      if (curCycle > 0) {
        curCycle--;
        getController().cycleSet(curCycle);
        if (isPlaying() && getController().cycleFrameCount() == 0) {
          pause();
          bPlay.setSelected(false);
        }
        rewind();
        showFrame();
      }
    }
    else if (bNextCycle == event.getSource()) {
      if (curCycle < getController().cycleCount() - 1) {
        curCycle++;
        getController().cycleSet(curCycle);
        if (isPlaying() && getController().cycleFrameCount() == 0) {
          pause();
          bPlay.setSelected(false);
        }
        rewind();
        showFrame();
      }
    }
    else if (bPrevFrame == event.getSource()) {
      if (curFrame > 0) {
        curFrame--;
        showFrame();
      }
    }
    else if (bNextFrame == event.getSource()) {
      if (curFrame < getController().cycleFrameCount() - 1) {
        curFrame++;
        showFrame();
      }
    }
    else if (bPlay == event.getSource()) {
      if (bPlay.isSelected()) {
        play();
      } else {
        pause();
      }
      updateControls();
    }
    else if (bOpenBrowser == event.getSource()) {
      CreatureBrowser cv = ChildFrame.show(CreatureBrowser.class, () -> new CreatureBrowser(getCre()));
      if (cv != null) {
        pause();
        if (getCre() != cv.getCreResource()) {
          cv.setCreResource(getCre());
        }
        cv.toFront();
      }
    }
  }

//--------------------- End Interface ActionListener ---------------------

  private void init()
  {
    tmpImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    rcDisplay = new RenderCanvas(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
    rcDisplay.setHorizontalAlignment(SwingConstants.CENTER);
    rcDisplay.setVerticalAlignment(SwingConstants.CENTER);
    rcDisplay.setInterpolationType(RenderCanvas.TYPE_NEAREST_NEIGHBOR);
    rcDisplay.setScalingEnabled(true);
    JScrollPane scrollDisplay = new JScrollPane(rcDisplay, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollDisplay.setBorder(BorderFactory.createEmptyBorder());

    bPlay = new JToggleButton("Play", Icons.getIcon(Icons.ICON_PLAY_16));
    bPlay.addActionListener(this);

    lCurCycle = new JLabel("", JLabel.CENTER);
    bPrevCycle = new JButton(Icons.getIcon(Icons.ICON_BACK_16));
    bPrevCycle.setMargin(new Insets(bPrevCycle.getMargin().top, 2, bPrevCycle.getMargin().bottom, 2));
    bPrevCycle.addActionListener(this);
    bNextCycle = new JButton(Icons.getIcon(Icons.ICON_FORWARD_16));
    bNextCycle.setMargin(bPrevCycle.getMargin());
    bNextCycle.addActionListener(this);

    lCurFrame = new JLabel("", JLabel.CENTER);
    bPrevFrame = new JButton(Icons.getIcon(Icons.ICON_BACK_16));
    bPrevFrame.setMargin(new Insets(bPrevFrame.getMargin().top, 2, bPrevFrame.getMargin().bottom, 2));
    bPrevFrame.addActionListener(this);
    bNextFrame = new JButton(Icons.getIcon(Icons.ICON_FORWARD_16));
    bNextFrame.setMargin(bPrevFrame.getMargin());
    bNextFrame.addActionListener(this);

    JLabel lSequence = new JLabel("Sequence:");
    DefaultComboBoxModel<Sequence> modelSequences = new DefaultComboBoxModel<>();
    cbSequences = new JComboBox<>(modelSequences);

    cbZoom = new JCheckBox("Zoom", zoom);
    cbZoom.addActionListener(this);
    cbShowCircle = new JCheckBox("Show selection circle", showSelectionCircle);
    cbShowCircle.addActionListener(this);
    cbShowSpace = new JCheckBox("Show personal space", showPersonalSpace);
    cbShowSpace.addActionListener(this);
    cbShowBorders = new JCheckBox("Show overlay borders", showOverlayBorders);
    cbShowBorders.addActionListener(this);

    bOpenBrowser = new JButton("Open in browser", Icons.getIcon(Icons.ICON_CRE_VIEWER_24));
    bOpenBrowser.setToolTipText("Open in Creature Animation Browser");
    bOpenBrowser.addActionListener(this);

    GridBagConstraints c = new GridBagConstraints();
    // first row of controls: animation controls, sequence selection and browser button
    JPanel pRow1 = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pRow1.add(new JPanel(), c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pRow1.add(lCurCycle, c);
    c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 0, 0);
    pRow1.add(bPrevCycle, c);
    c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pRow1.add(bNextCycle, c);

    c = ViewerUtil.setGBC(c, 4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 16, 0, 0), 0, 0);
    pRow1.add(lCurFrame, c);
    c = ViewerUtil.setGBC(c, 5, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 0, 0);
    pRow1.add(bPrevFrame, c);
    c = ViewerUtil.setGBC(c, 6, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pRow1.add(bNextFrame, c);

    c = ViewerUtil.setGBC(c, 7, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 16, 0, 0), 0, 0);
    pRow1.add(bPlay, c);

    c = ViewerUtil.setGBC(c, 8, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 16, 0, 0), 0, 0);
    pRow1.add(lSequence, c);
    c = ViewerUtil.setGBC(c, 9, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pRow1.add(cbSequences, c);

    c = ViewerUtil.setGBC(c, 10, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 16, 0, 0), 0, 0);
    pRow1.add(bOpenBrowser, c);

    c = ViewerUtil.setGBC(c, 11, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pRow1.add(new JPanel(), c);

    // second row of controls: various checkboxes
    JPanel pRow2 = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pRow2.add(new JPanel(), c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pRow2.add(cbZoom, c);
    c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 0, 0);
    pRow2.add(cbShowCircle, c);
    c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 0, 0);
    pRow2.add(cbShowSpace, c);
    c = ViewerUtil.setGBC(c, 4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 0, 0);
    pRow2.add(cbShowBorders, c);
    c = ViewerUtil.setGBC(c, 5, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pRow2.add(new JPanel(), c);


    JPanel pMain = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pMain.add(scrollDisplay, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    pMain.add(pRow1, c);
    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
        GridBagConstraints.HORIZONTAL, new Insets(8, 0, 8, 0), 0, 0);
    pMain.add(pRow2, c);

    setLayout(new BorderLayout());
    add(pMain, BorderLayout.CENTER);

    setInitialized(false);

    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentShown(ComponentEvent e)
      {
        // loading animation on demand
        open();
      }
    });
  }

  private void initAnimation() throws Exception
  {
    this.decoder = SpriteDecoder.importSprite(getCre());
    getDecoder().setBoundingBoxVisible(showOverlayBorders);
    getDecoder().setSelectionCircleEnabled(showSelectionCircle);
    getDecoder().setPersonalSpaceVisible(showPersonalSpace);

    // preparing sequence list
    DefaultComboBoxModel<Sequence> modelSequences = (DefaultComboBoxModel<Sequence>)cbSequences.getModel();
    for (final Sequence seq : Sequence.values()) {
      if (getDecoder().isSequenceAvailable(seq)) {
        modelSequences.addElement(seq);
      }
    }
    cbSequences.setEnabled(cbSequences.getItemCount() > 0);
    cbSequences.addActionListener(this);

    // loading animation sequence
    if (cbSequences.isEnabled()) {
      int seqIdx = 0;
      for (final Sequence sequence : Sequence.getDefaultSequences()) {
        int idx = ((DefaultComboBoxModel<?>)cbSequences.getModel()).getIndexOf(sequence);
        if (idx >= 0) {
          seqIdx = idx;
          break;
        }
      }
      Sequence seq = cbSequences.getModel().getElementAt(seqIdx);
      cbSequences.setSelectedItem(seq);
      setAnimationSequence(seq);
    }

    setInitialized(true);
  }

  private boolean isInitialized()
  {
    return initialized;
  }

  private void setInitialized(boolean b)
  {
    initialized = b;
    JComponent[] controls = new JComponent[] {
        bNextCycle, bPrevCycle, bNextFrame, bPrevFrame, bOpenBrowser, bPlay,
        lCurCycle, lCurFrame, cbSequences, cbShowCircle, cbShowSpace, cbShowBorders, cbZoom
    };
    for (final JComponent c : controls) {
      if (c != null) {
        c.setEnabled(initialized);
      }
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

    lCurCycle.setText("Cycle: " + curCycle + "/" + (getController().cycleCount() - 1));
    lCurFrame.setText("Frame: " + curFrame + "/" + (getController().cycleFrameCount() - 1));
    updateControls();
  }

  private void updateControls()
  {
    if (getController() != null) {
      bPrevFrame.setEnabled(curFrame > 0);
      bPrevCycle.setEnabled(curCycle > 0);
      bNextFrame.setEnabled(curFrame < getController().cycleFrameCount() - 1);
      bNextCycle.setEnabled(curCycle < getController().cycleCount() - 1);
      bPlay.setEnabled(getController().cycleFrameCount() > 0);
    } else {
      bPrevFrame.setEnabled(false);
      bPrevCycle.setEnabled(false);
      bNextFrame.setEnabled(false);
      bNextCycle.setEnabled(false);
      bPlay.setEnabled(false);
    }
  }
}
