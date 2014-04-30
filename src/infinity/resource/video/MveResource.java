// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.video;

import infinity.NearInfinity;
import infinity.gui.ButtonPanel;
import infinity.icon.Icons;
import infinity.resource.Closeable;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.ViewableContainer;
import infinity.resource.key.ResourceEntry;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

public class MveResource implements Resource, ActionListener, ItemListener, Closeable, Runnable
{
  private static final int VIDEO_BUFFERS = 3;

  private static final ButtonPanel.Control CtrlPlay   = ButtonPanel.Control.Custom1;
  private static final ButtonPanel.Control CtrlPause  = ButtonPanel.Control.Custom2;
  private static final ButtonPanel.Control CtrlStop   = ButtonPanel.Control.Custom3;

  private static boolean isZoom = true;
  private static boolean isFilter = true;

  private final ResourceEntry entry;
  private final ButtonPanel buttonPanel = new ButtonPanel();

  private MveDecoder decoder;
  private ImageRenderer renderer;
  private MvePlayer player;
  private JPanel panel;
  private JCheckBox cbZoom, cbFilter;

  public MveResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    player = new MvePlayer();
    try {
      decoder = new MveDecoder(entry);
      if (!decoder.isOpen()) {
        decoder.close();
        throw new Exception("");
      }
    } catch (Exception e) {
      decoder = null;
      e.printStackTrace();
      JOptionPane.showMessageDialog(NearInfinity.getInstance(),
                                    "Error opening " + entry, "Error",
                                    JOptionPane.ERROR_MESSAGE);
    }
  }

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (buttonPanel.getControlByType(ButtonPanel.Control.ExportButton) == event.getSource()) {
      ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
    } else if (buttonPanel.getControlByType(CtrlPlay) == event.getSource()) {
      if (player.isStopped()) {
        new Thread(this).start();
      } else {
        if (player.isPaused()) {
          player.continuePlay();
          buttonPanel.getControlByType(CtrlPlay).setEnabled(player.isPaused());
          buttonPanel.getControlByType(CtrlPause).setEnabled(!player.isPaused());
        }
      }
    } else if (buttonPanel.getControlByType(CtrlPause) == event.getSource()) {
      if (!player.isStopped()) {
        if (!player.isPaused()) {
          player.pausePlay();
          buttonPanel.getControlByType(CtrlPlay).setEnabled(player.isPaused());
          buttonPanel.getControlByType(CtrlPause).setEnabled(!player.isPaused());
        }
      }
    } else if (buttonPanel.getControlByType(CtrlStop) == event.getSource()) {
      player.stopPlay();
      buttonPanel.getControlByType(CtrlStop).setEnabled(false);
      buttonPanel.getControlByType(CtrlPause).setEnabled(false);
      buttonPanel.getControlByType(CtrlPlay).setEnabled(true);
    }
  }

//--------------------- End Interface ActionListener ---------------------

//--------------------- Begin Interface ItemListener ---------------------

  @Override
  public void itemStateChanged(ItemEvent event)
  {
    if (event.getSource() == cbZoom) {
      if (renderer != null) {
        isZoom = cbZoom.isSelected();
        renderer.setScalingEnabled(isZoom);
      }
    } else if (event.getSource() == cbFilter) {
      if (renderer != null) {
        isFilter = cbFilter.isSelected();
        Object filter = isFilter ? ImageRenderer.TYPE_BILINEAR : ImageRenderer.TYPE_NEAREST_NEIGHBOR;
        renderer.setInterpolationType(filter);
      }
    }
  }

//--------------------- End Interface ItemListener ---------------------

//--------------------- Begin Interface Resource ---------------------

  @Override
  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

//--------------------- End Interface Resource ---------------------

//--------------------- Begin Interface Closeable ---------------------

  @Override
  public void close() throws Exception
  {
    if (player != null) {
      player.stopPlay();
    }
    if (decoder != null) {
      decoder.close();
      decoder = null;
    }
  }

//--------------------- End Interface Closeable ---------------------

//--------------------- Begin Interface Runnable ---------------------

  @Override
  public void run()
  {
    if (!decoder.isOpen()) {
      try {
        decoder.open(entry);
      } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(panel, "Error starting video playback", "Error",
                                      JOptionPane.ERROR_MESSAGE);
        return;
      }
    }

    buttonPanel.getControlByType(CtrlPlay).setEnabled(false);
    buttonPanel.getControlByType(CtrlPause).setEnabled(true);
    buttonPanel.getControlByType(CtrlStop).setEnabled(true);
    try {
      renderer.clearBuffers();
      player.play(renderer, decoder);
    } catch (Exception e) {
      player.stopPlay();
      e.printStackTrace();
      JOptionPane.showMessageDialog(panel, "Error during playback", "Error", JOptionPane.ERROR_MESSAGE);
    }
    decoder.close();
    buttonPanel.getControlByType(CtrlPlay).setEnabled(true);
    buttonPanel.getControlByType(CtrlPause).setEnabled(false);
    buttonPanel.getControlByType(CtrlStop).setEnabled(false);
  }

//--------------------- End Interface Runable ---------------------

//--------------------- Begin Interface Viewable ---------------------

  @Override
  public JComponent makeViewer(ViewableContainer container)
  {
    if (decoder != null) {
      renderer = new ImageRenderer(VIDEO_BUFFERS, decoder.getVideoWidth(), decoder.getVideoHeight());
      renderer.setHorizontalAlignment(SwingConstants.CENTER);
      renderer.setVerticalAlignment(SwingConstants.CENTER);
      if (isFilter) {
        renderer.setInterpolationType(ImageRenderer.TYPE_BILINEAR);
      } else {
        renderer.setInterpolationType(ImageRenderer.TYPE_NEAREST_NEIGHBOR);
      }
      renderer.setAspectRatioEnabled(true);
      renderer.setScalingEnabled(isZoom);
      decoder.setVideoOutput(renderer);
    } else {
      renderer = new ImageRenderer();
    }
    JScrollPane scroll = new JScrollPane(renderer);
    scroll.setPreferredSize(new Dimension(renderer.getBufferWidth(), renderer.getBufferHeight()));
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.getHorizontalScrollBar().setUnitIncrement(16);
    scroll.setBorder(BorderFactory.createLoweredBevelBorder());

    cbZoom = new JCheckBox("Zoom video", isZoom);
    cbZoom.addItemListener(this);
    cbFilter = new JCheckBox("Enable video filtering", isFilter);
    cbFilter.addItemListener(this);
    cbFilter.setToolTipText("Uncheck for better video performance");
    JPanel optionsPanel = new JPanel();
    BoxLayout bl = new BoxLayout(optionsPanel, BoxLayout.Y_AXIS);
    optionsPanel.setLayout(bl);
    optionsPanel.add(cbZoom);
    optionsPanel.add(cbFilter);

    JButton bPlay = new JButton("Play", Icons.getIcon("Play16.gif"));
    bPlay.addActionListener(this);
    bPlay.setEnabled(decoder != null);
    JButton bPause = new JButton("Pause", Icons.getIcon("Pause16.gif"));
    bPause.addActionListener(this);
    bPause.setEnabled(false);
    JButton bStop = new JButton("Stop", Icons.getIcon("Stop16.gif"));
    bStop.addActionListener(this);
    bStop.setEnabled(false);

    buttonPanel.addControl(bPlay, CtrlPlay);
    buttonPanel.addControl(bPause, CtrlPause);
    buttonPanel.addControl(bStop, CtrlStop);
    ((JButton)buttonPanel.addControl(ButtonPanel.Control.ExportButton)).addActionListener(this);
    buttonPanel.addControl(optionsPanel);

    panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(scroll, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.SOUTH);

    return panel;
  }

//--------------------- End Interface Viewable ---------------------
}
