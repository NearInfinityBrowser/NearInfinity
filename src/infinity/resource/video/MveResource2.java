// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.video;

import infinity.NearInfinity;
import infinity.icon.Icons;
import infinity.resource.Closeable;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.ViewableContainer;
import infinity.resource.key.ResourceEntry;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class MveResource2 implements Resource, ActionListener, ChangeListener, Closeable, Runnable
{
  private static final int VIDEO_BUFFERS = 3;

  private static boolean isZoom = true;
  private static boolean isFilter = true;

  private final ResourceEntry entry;

  private MveDecoder decoder;
  private ImageRenderer renderer;
  private MvePlayer player;
  private JPanel panel;
  private JButton bPlay, bPause, bStop, bExport;
  private JCheckBox cbZoom, cbFilter;

  public MveResource2(ResourceEntry entry) throws Exception
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
    if (event.getSource() == bExport) {
      ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
    } else if (event.getSource() == bPlay) {
      if (player.isStopped()) {
        new Thread(this).start();
      } else {
        if (player.isPaused()) {
          player.continuePlay();
          bPlay.setEnabled(player.isPaused());
          bPause.setEnabled(!player.isPaused());
        }
      }
    } else if (event.getSource() == bPause) {
      if (!player.isStopped()) {
        if (!player.isPaused()) {
          player.pausePlay();
          bPlay.setEnabled(player.isPaused());
          bPause.setEnabled(!player.isPaused());
        }
      }
    } else if (event.getSource() == bStop) {
      player.stopPlay();
      bStop.setEnabled(false);
      bPause.setEnabled(false);
      bPlay.setEnabled(true);
    }
  }

//--------------------- End Interface ActionListener ---------------------

//--------------------- Begin Interface ItemChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event)
  {
    if (event.getSource() == cbZoom) {
      if (renderer != null) {
        isZoom = cbZoom.isSelected();
        renderer.setScalingEnabled(isZoom);
      }
    } else if (event.getSource() == cbFilter) {
      if (renderer != null) {
        isFilter = cbFilter.isSelected();
        int filter = isFilter ? ImageRenderer.TYPE_BILINEAR : ImageRenderer.TYPE_NEAREST_NEIGHBOR;
        renderer.setInterpolationType(filter);
      }
    }
  }

//--------------------- End Interface ItemChangeListener ---------------------

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

    bPlay.setEnabled(false);
    bPause.setEnabled(true);
    bStop.setEnabled(true);
    try {
      renderer.clearBuffers();
      player.play(renderer, decoder);
    } catch (Exception e) {
      player.stopPlay();
      e.printStackTrace();
      JOptionPane.showMessageDialog(panel, "Error during playback", "Error", JOptionPane.ERROR_MESSAGE);
    }
    decoder.close();
    bPlay.setEnabled(true);
    bPause.setEnabled(false);
    bStop.setEnabled(false);
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
    cbZoom.addChangeListener(this);
    cbFilter = new JCheckBox("Enable video filtering", isFilter);
    cbFilter.addChangeListener(this);
    cbFilter.setToolTipText("Uncheck for better video performance");
    JPanel optionsPanel = new JPanel();
    BoxLayout bl = new BoxLayout(optionsPanel, BoxLayout.Y_AXIS);
    optionsPanel.setLayout(bl);
    optionsPanel.add(cbZoom);
    optionsPanel.add(cbFilter);

    bPlay = new JButton("Play", Icons.getIcon("Play16.gif"));
    bPlay.addActionListener(this);
    bPlay.setEnabled(decoder != null);
    bPause = new JButton("Pause", Icons.getIcon("Pause16.gif"));
    bPause.addActionListener(this);
    bPause.setEnabled(false);
    bStop = new JButton("Stop", Icons.getIcon("Stop16.gif"));
    bStop.addActionListener(this);
    bStop.setEnabled(false);

    bExport = new JButton("Export...", Icons.getIcon("Export16.gif"));
    bExport.setMnemonic('e');
    bExport.addActionListener(this);

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    buttonPanel.add(bPlay);
    buttonPanel.add(bPause);
    buttonPanel.add(bStop);
    buttonPanel.add(bExport);
    buttonPanel.add(optionsPanel);

    panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(scroll, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.PAGE_END);

    return panel;
  }

//--------------------- End Interface Viewable ---------------------
}
