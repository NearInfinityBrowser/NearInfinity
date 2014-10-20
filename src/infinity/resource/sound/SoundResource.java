// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.sound;

import infinity.NearInfinity;
import infinity.gui.ButtonPanel;
import infinity.gui.ButtonPopupMenu;
import infinity.icon.Icons;
import infinity.resource.Closeable;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.ViewableContainer;
import infinity.resource.key.ResourceEntry;
import infinity.search.WavReferenceSearcher;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

/**
 * Handles all kinds of supported single track audio files.
 * @author argent77
 */
public class SoundResource implements Resource, ActionListener, ItemListener, Closeable, Runnable
{
  private final ResourceEntry entry;
  private final ButtonPanel buttonPanel = new ButtonPanel();

  private AudioPlayer player;
  private AudioBuffer audioBuffer = null;
  private JButton bPlay, bStop;
  private JMenuItem miExport, miConvert;
  private JPanel panel;
  private boolean isWAV, isReference, isClosed;

  public SoundResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    player = new AudioPlayer();
    isWAV = false;
    isReference = (entry.getExtension().equalsIgnoreCase("WAV"));
    isClosed = false;
  }

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bPlay) {
      new Thread(this).start();
    } else if (event.getSource() == bStop) {
      bStop.setEnabled(false);
      player.stopPlay();
      bPlay.setEnabled(true);
    } else if (buttonPanel.getControlByType(ButtonPanel.Control.FindReferences) == event.getSource()) {
      new WavReferenceSearcher(entry, panel.getTopLevelAncestor());
    }
  }

//--------------------- End Interface ActionListener ---------------------

//--------------------- Begin Interface ItemListener ---------------------

  @Override
  public void itemStateChanged(ItemEvent event)
  {
    if (buttonPanel.getControlByType(ButtonPanel.Control.ExportMenu) == event.getSource()) {
      ButtonPopupMenu bpmExport = (ButtonPopupMenu)event.getSource();
      if (bpmExport.getSelectedItem() == miExport) {
        ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
      } else if (bpmExport.getSelectedItem() == miConvert) {
        String fileName = entry.toString();
        if (fileName.lastIndexOf('.') > 0)
          fileName = fileName.substring(0, fileName.lastIndexOf('.')) + ".WAV";
        ResourceFactory.getInstance().exportResource(entry, audioBuffer.getAudioData(),
                                                     fileName, panel.getTopLevelAncestor());
      }
    }
  }

//--------------------- End Interface ItemListener ---------------------

//--------------------- Begin Interface Closeable ---------------------

  @Override
  public void close() throws Exception
  {
    setClosed(true);
    if (player != null) {
      player.stopPlay();
      player = null;
    }
    audioBuffer = null;
    panel = null;
  }

//--------------------- End Interface Closeable ---------------------

//--------------------- Begin Interface Resource ---------------------

  @Override
  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

//--------------------- End Interface Resource ---------------------

//--------------------- Begin Interface Runnable ---------------------

  @Override
  public void run()
  {
    bPlay.setEnabled(false);
    bStop.setEnabled(true);
    if (audioBuffer != null) {
      try {
        player.play(audioBuffer);
      } catch (Exception e) {
        JOptionPane.showMessageDialog(panel, "Error during playback", "Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
        player.stopPlay();
      }
    }
    bStop.setEnabled(false);
    bPlay.setEnabled(true);
  }

//--------------------- End Interface Runnable ---------------------

//--------------------- Begin Interface Viewable ---------------------

  @Override
  public JComponent makeViewer(ViewableContainer container)
  {
    JPanel controlPanel = new JPanel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    controlPanel.setLayout(gbl);
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.fill = GridBagConstraints.HORIZONTAL;

    bPlay = new JButton(Icons.getIcon("Play16.gif"));
    bPlay.addActionListener(this);
    gbl.setConstraints(bPlay, gbc);
    controlPanel.add(bPlay);
    bStop = new JButton(Icons.getIcon("Stop16.gif"));
    bStop.addActionListener(this);
    bStop.setEnabled(false);
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(bStop, gbc);
    controlPanel.add(bStop);

    JPanel centerPanel = new JPanel(new BorderLayout());
    centerPanel.add(controlPanel, BorderLayout.CENTER);

    if (isReference) {
      // only available for WAV resource types
      ((JButton)buttonPanel.addControl(ButtonPanel.Control.FindReferences)).addActionListener(this);
    }

    miExport = new JMenuItem("original");
    miConvert = new JMenuItem("as WAV");
    miConvert.setEnabled(!isWAV);
    ButtonPopupMenu bpmExport = (ButtonPopupMenu)buttonPanel.addControl(ButtonPanel.Control.ExportMenu);
    bpmExport.setMenuItems(new JMenuItem[]{miExport, miConvert});
    bpmExport.addItemListener(this);

    panel = new JPanel(new BorderLayout());
    panel.add(centerPanel, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.SOUTH);
    centerPanel.setBorder(BorderFactory.createLoweredBevelBorder());

    loadSoundResource();

    return panel;
  }

  // Returns the top level container associated with this viewer
  private Container getContainer()
  {
    if (panel != null) {
      return panel.getTopLevelAncestor();
    } else
      return NearInfinity.getInstance();
  }

  private void loadSoundResource()
  {
    setLoaded(false);
    (new SwingWorker<Boolean, Void>() {
      @Override
      public Boolean doInBackground()
      {
        return loadAudio();
      }
    }).execute();
  }

  private synchronized void setLoaded(boolean b)
  {
    if (bPlay != null) {
      bPlay.setEnabled(b);
    }
    JButton bFind = (JButton)buttonPanel.getControlByType(ButtonPanel.Control.FindReferences);
    if (bFind != null) {
      bFind.setEnabled(b);
    }
    ButtonPopupMenu bpmExport = (ButtonPopupMenu)buttonPanel.getControlByType(ButtonPanel.Control.ExportMenu);
    if (bpmExport != null) {
      bpmExport.setEnabled(b);
    }
  }

  private synchronized void setClosed(boolean b)
  {
    if (b != isClosed) {
      isClosed = b;
    }
  }

  private synchronized boolean isClosed()
  {
    return isClosed;
  }

  private boolean loadAudio()
  {
    try {
      AudioBuffer.AudioOverride override = null;
      AudioBuffer buffer = null;
      synchronized (entry) {
        // ignore # channels in ACM headers
        if (entry.getExtension().equalsIgnoreCase("ACM")) {
          override = AudioBuffer.AudioOverride.overrideChannels(2);
        }
        buffer = AudioFactory.getAudioBuffer(entry, override);
      }
      if (buffer != null && !isClosed()) {
        synchronized (this) {
          audioBuffer = buffer;
          isWAV = (audioBuffer instanceof WavBuffer);
          isReference = (entry.getExtension().compareToIgnoreCase("WAV") == 0);
        }
        setLoaded(true);
        return true;
      }
    } catch (Exception e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(getContainer(), e.getMessage(), "Error",
                                    JOptionPane.ERROR_MESSAGE);
    }
    return false;
  }

//--------------------- End Interface Viewable ---------------------
}
