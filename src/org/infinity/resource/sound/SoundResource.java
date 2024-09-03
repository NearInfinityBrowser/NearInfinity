// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sound;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import org.infinity.NearInfinity;
import org.infinity.gui.ButtonPanel;
import org.infinity.gui.ButtonPopupMenu;
import org.infinity.gui.ViewerUtil;
import org.infinity.icon.Icons;
import org.infinity.resource.Closeable;
import org.infinity.resource.Referenceable;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.ViewableContainer;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.WavReferenceSearcher;
import org.infinity.util.Logger;
import org.infinity.util.io.StreamUtils;

/**
 * Handles all kinds of supported single track audio files.
 */
public class SoundResource implements Resource, ActionListener, ItemListener, Closeable, Referenceable, Runnable {
  /** Formatted string with 4 placeholders: elapsed minute, elapsed second, total minutes, total seconds */
  private static final String FMT_PLAY_TIME         = "%02d:%02d / %02d:%02d";

  private static final ButtonPanel.Control PROPERTIES = ButtonPanel.Control.CUSTOM_1;

  /** Provides quick access to the "play" and "pause" image icon. */
  private static final HashMap<Boolean, Icon> PLAY_ICONS = new HashMap<>();

  static {
    PLAY_ICONS.put(true, Icons.ICON_PLAY_16.getIcon());
    PLAY_ICONS.put(false, Icons.ICON_PAUSE_16.getIcon());
  }

  private final ResourceEntry entry;
  private final ButtonPanel buttonPanel = new ButtonPanel();

  private AudioPlayer player;
  private AudioBuffer audioBuffer = null;
  private JButton bPlay;
  private JButton bStop;
  private JLabel lTime;
  private JMenuItem miExport;
  private JMenuItem miConvert;
  private JPanel panel;
  private boolean isWAV;
  private boolean isReference;
  private boolean isClosed;

  public SoundResource(ResourceEntry entry) throws Exception {
    this.entry = entry;
    player = new AudioPlayer();
    isWAV = false;
    isReference = (entry.getExtension().equalsIgnoreCase("WAV"));
    isClosed = false;
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == bPlay) {
      if (player == null || !player.isRunning()) {
        new Thread(this).start();
      } else if (player.isRunning()) {
        bPlay.setIcon(PLAY_ICONS.get(!player.isPaused()));
        player.setPaused(!player.isPaused());
      }
    } else if (event.getSource() == bStop) {
      bStop.setEnabled(false);
      player.stopPlay();
      bPlay.setIcon(PLAY_ICONS.get(true));
    } else if (buttonPanel.getControlByType(ButtonPanel.Control.FIND_REFERENCES) == event.getSource()) {
      searchReferences(panel.getTopLevelAncestor());
    } else if (buttonPanel.getControlByType(PROPERTIES) == event.getSource()) {
      showProperties();
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface ItemListener ---------------------

  @Override
  public void itemStateChanged(ItemEvent event) {
    if (buttonPanel.getControlByType(ButtonPanel.Control.EXPORT_MENU) == event.getSource()) {
      ButtonPopupMenu bpmExport = (ButtonPopupMenu) event.getSource();
      if (bpmExport.getSelectedItem() == miExport) {
        ResourceFactory.exportResource(entry, panel.getTopLevelAncestor());
      } else if (bpmExport.getSelectedItem() == miConvert) {
        final String fileName = StreamUtils.replaceFileExtension(entry.getResourceName(), "WAV");
        ByteBuffer buffer = StreamUtils.getByteBuffer(audioBuffer.getAudioData());
        ResourceFactory.exportResource(entry, buffer, fileName, panel.getTopLevelAncestor());
      }
    }
  }

  // --------------------- End Interface ItemListener ---------------------

  // --------------------- Begin Interface Closeable ---------------------

  @Override
  public void close() throws Exception {
    setClosed(true);
    if (player != null) {
      player.stopPlay();
      player = null;
    }
    audioBuffer = null;
    panel = null;
  }

  // --------------------- End Interface Closeable ---------------------

  // --------------------- Begin Interface Resource ---------------------

  @Override
  public ResourceEntry getResourceEntry() {
    return entry;
  }

  // --------------------- End Interface Resource ---------------------

  // --------------------- Begin Interface Referenceable ---------------------

  @Override
  public boolean isReferenceable() {
    return isReference;
  }

  @Override
  public void searchReferences(Component parent) {
    new WavReferenceSearcher(entry, parent);
  }

  // --------------------- End Interface Referenceable ---------------------

  // --------------------- Begin Interface Runnable ---------------------

  @Override
  public void run() {
    if (bPlay != null) {
      bPlay.setIcon(PLAY_ICONS.get(false));
      bStop.setEnabled(true);
    }
    if (audioBuffer != null) {
      final TimerElapsedTask timerTask = new TimerElapsedTask(250L);
      try {
        timerTask.start();
        player.play(audioBuffer);
      } catch (Exception e) {
        JOptionPane.showMessageDialog(panel, "Error during playback", "Error", JOptionPane.ERROR_MESSAGE);
        Logger.error(e);
      }
      player.stopPlay();
      timerTask.stop();
    }
    if (bPlay != null) {
      bStop.setEnabled(false);
      bPlay.setIcon(PLAY_ICONS.get(true));
    }
  }

  // --------------------- End Interface Runnable ---------------------

  // --------------------- Begin Interface Viewable ---------------------

  @Override
  public JComponent makeViewer(ViewableContainer container) {
    JPanel controlPanel = new JPanel(new GridBagLayout());

    bPlay = new JButton(PLAY_ICONS.get(true));
    bPlay.addActionListener(this);
    GridBagConstraints c = new GridBagConstraints();
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(4, 8, 4, 0), 0, 0);
    controlPanel.add(bPlay, c);

    bStop = new JButton(Icons.ICON_STOP_16.getIcon());
    bStop.addActionListener(this);
    bStop.setEnabled(false);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.REMAINDER,
        new Insets(4, 8, 4, 8), 0, 0);
    controlPanel.add(bStop, c);

    lTime = new JLabel(String.format(FMT_PLAY_TIME, 0, 0, 0, 0));
    c = ViewerUtil.setGBC(c, 0, 1, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.REMAINDER,
        new Insets(4, 8, 4, 8), 0, 0);
    controlPanel.add(lTime, c);

    JPanel centerPanel = new JPanel(new BorderLayout());
    centerPanel.add(controlPanel, BorderLayout.CENTER);

    if (isReference) {
      // only available for WAV resource types
      ((JButton) buttonPanel.addControl(ButtonPanel.Control.FIND_REFERENCES)).addActionListener(this);
    }

    miExport = new JMenuItem("original");
    miConvert = new JMenuItem("as WAV");
    miConvert.setEnabled(!isWAV);
    ButtonPopupMenu bpmExport = (ButtonPopupMenu) buttonPanel.addControl(ButtonPanel.Control.EXPORT_MENU);
    bpmExport.setMenuItems(new JMenuItem[] { miExport, miConvert });
    bpmExport.addItemListener(this);

    JButton bProperties = new JButton("Properties...", Icons.ICON_EDIT_16.getIcon());
    bProperties.setEnabled(false);
    bProperties.addActionListener(this);
    buttonPanel.addControl(bProperties, PROPERTIES);

    panel = new JPanel(new BorderLayout());
    panel.add(centerPanel, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.SOUTH);
    centerPanel.setBorder(BorderFactory.createLoweredBevelBorder());

    loadSoundResource();

    return panel;
  }

  // Updates the time label with total duration and specified elapsed time (in milliseconds).
  private synchronized void updateTimeLabel(long elapsed) {
    long duration = (audioBuffer != null) ? audioBuffer.getDuration() : 0L;
    long em = elapsed / 1000 / 60;
    long es = (elapsed / 1000) - (em * 60);
    long dm = duration / 1000 / 60;
    long ds = (duration / 1000) - (dm * 60);
    lTime.setText(String.format(FMT_PLAY_TIME, em, es, dm, ds));
  }

  /**
   * Returns a formatted representation of the total duration of the sound clip.
   *
   * @param exact Whether the seconds part should contain the fractional amount.
   * @return A formatted string representing the sound clip duration.
   */
  private String getTotalDurationString(boolean exact) {
    long duration = (audioBuffer != null) ? audioBuffer.getDuration() : 0L;
    long m = duration / 1000 / 60;
    if (exact) {
      double s = (duration / 1000.0) - (m * 60);
      return String.format("%02d:%06.3f", m, s);
    } else {
      long s = (duration / 1000) - (m * 60);
      return String.format("%02d:%02d", m, s);
    }
  }

  // Returns the top level container associated with this viewer
  private Container getContainer() {
    if (panel != null) {
      return panel.getTopLevelAncestor();
    } else {
      return NearInfinity.getInstance();
    }
  }

  private void loadSoundResource() {
    setLoaded(false);
    (new SwingWorker<Boolean, Void>() {
      @Override
      public Boolean doInBackground() {
        return loadAudio();
      }
    }).execute();
  }

  private synchronized void setLoaded(boolean b) {
    if (bPlay != null) {
      bPlay.setEnabled(b);
      bPlay.setIcon(PLAY_ICONS.get(true));

      updateTimeLabel(0);
      miConvert.setEnabled(b);
      buttonPanel.getControlByType(PROPERTIES).setEnabled(true);
    }
  }

  private synchronized void setClosed(boolean b) {
    if (b != isClosed) {
      isClosed = b;
    }
  }

  private synchronized boolean isClosed() {
    return isClosed;
  }

  private boolean loadAudio() {
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
      Logger.error(e);
      JOptionPane.showMessageDialog(getContainer(), e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
    return false;
  }

  /** Shows a message dialog with basic properties of the current sound resource. */
  private void showProperties() {
    if (audioBuffer == null) {
      return;
    }

    final String resName = entry.getResourceName().toUpperCase(Locale.ENGLISH);
    String format;
    int rate;
    int channels;
    String channelsDesc;
    String duration = getTotalDurationString(true);
    final String extra;
    if (audioBuffer instanceof OggBuffer) {
      format = "Ogg Vorbis";
      final OggBuffer buf = (OggBuffer) audioBuffer;
      rate = buf.getInfo().rate;
      channels = buf.getInfo().channels;
      double bitrate = entry.getResourceSize();
      bitrate = bitrate * 8.0 / 1000.0; // in kbit
      bitrate = bitrate * 1000.0 / buf.getDuration();  // per second
      extra = "Bitrate:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + ((long) bitrate) + " kbps";
    } else if (audioBuffer instanceof AcmBuffer) {
      if (audioBuffer instanceof WavcBuffer) {
        format = "WAVC/ACM";
      } else {
        format = "ACM";
      }
      final AcmBuffer buf = (AcmBuffer) audioBuffer;
      rate = buf.getSampleRate();
      channels = buf.getChannels();
      extra = "Bits/Sample:&nbsp;" + buf.getBitsPerSample();
    } else {
      format = "PCM";
      rate = audioBuffer.getSampleRate();
      channels = audioBuffer.getChannels();
      extra = "Bits/Sample:&nbsp;" + audioBuffer.getBitsPerSample();
    }

    switch (channels) {
      case 1:
        channelsDesc = " (Mono)";
        break;
      case 2:
        channelsDesc = " (Stereo)";
        break;
      default:
        channelsDesc = "";
        break;
    }

    final String br = "<br />";
    final StringBuilder sb = new StringBuilder("<html><div style='font-family:monospace'>");
    sb.append("Format:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;").append(format).append(br);
    sb.append("Duration:&nbsp;&nbsp;&nbsp;&nbsp;").append(duration).append(br);
    sb.append(extra).append(br);
    sb.append("Sample Rate:&nbsp;").append(rate).append(" Hz").append(br);
    sb.append("Channels:&nbsp;&nbsp;&nbsp;&nbsp;").append(channels).append(channelsDesc).append(br);
    sb.append("</div></html>");
    JOptionPane.showMessageDialog(panel, sb.toString(), "Properties of " + resName, JOptionPane.INFORMATION_MESSAGE);
  }

  // --------------------- End Interface Viewable ---------------------

  // -------------------------- INNER CLASSES --------------------------

  private class TimerElapsedTask extends TimerTask {
    private final long delay;

    private Timer timer;
    private boolean paused;

    /** Initializes a new timer task with the given delay, in milliseconds. */
    public TimerElapsedTask(long delay) {
      this.delay = Math.max(1L, delay);
      this.timer = null;
      this.paused = false;
    }

    /**
     * Starts a new scheduled run.
     */
    public void start() {
      if (timer == null) {
        timer = new Timer();
        timer.schedule(this, 0L, delay);
      }
    }

//    /** Returns whether a task has been initialized via {@link #start()}. */
//    public boolean isRunning() {
//      return (timer != null);
//    }

//    /** Pauses or unpauses a scheduled run. */
//    public void setPaused(boolean paused) {
//      this.paused = paused;
//    }

//    /** Returns whether a scheduled run is in paused state. */
//    public boolean isPaused() {
//      return paused;
//    }

    /** Stops a scheduled run. */
    public void stop() {
      if (timer != null) {
        timer.cancel();
        timer = null;
        paused = false;
        if (bPlay != null) {
          updateTimeLabel(0L);
        }
      }
    }

    @Override
    public void run() {
      if (!paused && timer != null && player != null && player.getDataLine() != null && bPlay != null) {
        updateTimeLabel(player.getDataLine().getMicrosecondPosition() / 1000L);
      }
    }
  }

  public void playSound() {
    loadAudio();
    new Thread(this).start();
  }
}
