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
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.IsTextual;
import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.RenderCanvas;
import org.infinity.gui.ViewerUtil;
import org.infinity.gui.WindowBlocker;
import org.infinity.icon.Icons;
import org.infinity.resource.cre.decoder.SpriteDecoder;
import org.infinity.resource.cre.decoder.util.Sequence;
import org.infinity.resource.cre.decoder.util.SpriteUtils;
import org.infinity.resource.cre.viewer.CreatureViewer;
import org.infinity.resource.gam.PartyNPC;
import org.infinity.resource.graphics.PseudoBamDecoder.PseudoBamControl;
import org.infinity.util.StringTable;

/**
 * A basic creature animation viewer.
 */
public class ViewerAnimation extends ChildFrame implements ActionListener
{
  private static final Color COLOR_TRANSPARENT = new Color(0, true);
  private static final int ANIM_DELAY = 1000 / 15;    // 15 fps in milliseconds

  private static boolean zoom = false;
  private static boolean showSelectionCircle = false;
  private static boolean showPersonalSpace = false;

  private final CreResource cre;

  private SpriteDecoder decoder;
  private PseudoBamControl bamControl;
  private RenderCanvas rcDisplay;
  private int curCycle, curFrame;
  private Timer timer;
  private Sequence sequence;
  private JButton bNextCycle, bPrevCycle, bNextFrame, bPrevFrame, bOpenBrowser;
  private JToggleButton bPlay;
  private JLabel lCurCycle, lCurFrame;
  private JComboBox<Sequence> cbSequences;
  private JCheckBox cbShowCircle, cbShowSpace, cbZoom;

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

  /** Ensures that the canvas is big enough to contain the current creature animation sequence. */
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

  /** Updates display with content of the current animation frame. */
  public void updateCanvas()
  {
    BufferedImage image = (BufferedImage)rcDisplay.getImage();
    Graphics2D g = image.createGraphics();
    try {
      g.setComposite(AlphaComposite.Src);
      g.setColor(COLOR_TRANSPARENT);
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

//--------------------- Begin Class ChildFrame ---------------------

  @Override
  protected boolean windowClosing(boolean forced) throws Exception
  {
    pause();
    if (getDecoder() != null) {
      getDecoder().close();
    }
    SpriteUtils.clearCache();
    return true;
  }

//--------------------- End Class ChildFrame ---------------------

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
        WindowBlocker.blockWindow(this, true);
        setAnimationSequence(seq);
        updateControls();
      } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        cbSequences.setSelectedItem(getAnimationSequence());
      } finally {
        WindowBlocker.blockWindow(this, false);
      }
    }
    else if (cbZoom == event.getSource()) {
      try {
        WindowBlocker.blockWindow(this, true);
        zoom = cbZoom.isSelected();
        updateCanvasSize();
      } finally {
        WindowBlocker.blockWindow(this, false);
      }
    }
    else if (cbShowCircle == event.getSource()) {
      try {
        WindowBlocker.blockWindow(this, true);
        showSelectionCircle = cbShowCircle.isSelected();
        getDecoder().setSelectionCircleEnabled(showSelectionCircle);
        resetAnimationSequence();
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        WindowBlocker.blockWindow(this, false);
      }
    }
    else if (cbShowSpace == event.getSource()) {
      try {
        WindowBlocker.blockWindow(this, true);
        showPersonalSpace = cbShowSpace.isSelected();
        getDecoder().setPersonalSpaceVisible(showPersonalSpace);
        resetAnimationSequence();
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        WindowBlocker.blockWindow(this, false);
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
      CreatureViewer cv = ChildFrame.show(CreatureViewer.class, () -> new CreatureViewer(getCre()));
      if (cv != null) {
        if (getCre() != cv.getCreResource()) {
          cv.setCreResource(getCre());
        }
        cv.toFront();
      }
      close();
    }
  }

//--------------------- End Interface ActionListener ---------------------

  private void init() throws Exception
  {
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
    for (final Sequence seq : Sequence.values()) {
      if (getDecoder().isSequenceAvailable(seq)) {
        modelSequences.addElement(seq);
      }
    }
    cbSequences.setEnabled(cbSequences.getItemCount() > 0);
    cbSequences.addActionListener(this);

    cbZoom = new JCheckBox("Zoom", zoom);
    cbZoom.addActionListener(this);
    getDecoder().setSelectionCircleEnabled(showSelectionCircle);
    cbShowCircle = new JCheckBox("Show selection circle", getDecoder().isSelectionCircleEnabled());
    cbShowCircle.addActionListener(this);
    getDecoder().setPersonalSpaceVisible(showPersonalSpace);
    cbShowSpace = new JCheckBox("Show personal space", getDecoder().isPersonalSpaceVisible());
    cbShowSpace.addActionListener(this);

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
    c = ViewerUtil.setGBC(c, 4, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pRow2.add(new JPanel(), c);


    setLayout(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    add(scrollDisplay, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    add(pRow1, c);
    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
        GridBagConstraints.HORIZONTAL, new Insets(8, 0, 8, 0), 0, 0);
    add(pRow2, c);

    // determining creature resource and name
    String resName, name;
    if (getCre().getResourceEntry() != null) {
      resName = getCre().getResourceEntry().getResourceName();
    } else if (getCre().getParent() != null) {
      resName = getCre().getParent().getName();
    } else {
      resName = getCre().getName();
    }

    int strref = ((IsNumeric)getCre().getAttribute(CreResource.CRE_NAME)).getValue();
    if (!StringTable.isValidStringRef(strref)) {
      strref = ((IsNumeric)getCre().getAttribute(CreResource.CRE_NAME)).getValue();
    }
    if (StringTable.isValidStringRef(strref)) {
      name = StringTable.getStringRef(strref);
    } else if (getCre().getParent() instanceof PartyNPC) {
      name = ((IsTextual)getCre().getParent().getAttribute(PartyNPC.GAM_NPC_NAME)).getText();
    } else {
      name = "";
    }

    if (!name.isEmpty()) {
      setTitle(String.format("%s (%s)", resName, name));
    } else {
      setTitle(resName);
    }

    Dimension dim = NearInfinity.getInstance().getSize();
    setSize(dim.width - 200, dim.height - 45);
    Center.center(this, NearInfinity.getInstance().getBounds());
    setVisible(true);

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
