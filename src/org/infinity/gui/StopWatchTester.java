// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingConstants;

import org.infinity.NearInfinity;
import org.infinity.icon.Icons;
import org.infinity.util.StopWatch;

/**
 * A test dialog for the {@link StopWatch} class.
 */
public class StopWatchTester extends ChildFrame implements ActionListener, ItemListener {
  // Display for hours, minutes, seconds, and fractional seconds (one digit)
  private static final String DISPLAY_HMSF_FMT = "%02d:%02d:%02d.%d";

  private final StopWatch timer = new StopWatch(false);

  private final JLabel displayLabel = new JLabel("00:00:00.0");
  private final JButton resetButton = new JButton("Reset");
  private final JButton startButton = new JButton("Start");
  private final JButton pauseButton = new JButton("Pause/Resume");

  private final JRadioButtonMenuItem rbmiInterval2000 = new JRadioButtonMenuItem("2 seconds");
  private final JRadioButtonMenuItem rbmiInterval1000 = new JRadioButtonMenuItem("1 second");
  private final JRadioButtonMenuItem rbmiInterval500 = new JRadioButtonMenuItem("500 ms");
  private final JRadioButtonMenuItem rbmiInterval100 = new JRadioButtonMenuItem("100 ms");

  public StopWatchTester() {
    super("Stop Watch Test Dialog");
    init();
  }

  public void resetTimer() {
    timer.pause();
    timer.reset();
    startButton.setEnabled(true);
    pauseButton.setEnabled(false);
    updateDisplay();
  }

  public void startTimer() {
    timer.resume();
    startButton.setEnabled(false);
    pauseButton.setEnabled(true);
  }

  public void pauseTimer() {
    timer.pause();
  }

  public void resumeTimer() {
    timer.resume();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == timer) {
      updateDisplay();
    } else if (e.getSource() == resetButton) {
      resetTimer();
    } else if (e.getSource() == startButton) {
      startTimer();
    } else if (e.getSource() == pauseButton) {
      if (timer.isPaused()) {
        resumeTimer();
      } else {
        pauseTimer();
      }
    }
  }

  @Override
  public void itemStateChanged(ItemEvent e) {
    if (e.getStateChange() == ItemEvent.SELECTED) {
      if (e.getSource() == rbmiInterval2000) {
        timer.setDelay(2000L);
      } else if (e.getSource() == rbmiInterval1000) {
        timer.setDelay(1000L);
      } else if (e.getSource() == rbmiInterval500) {
        timer.setDelay(500L);
      } else if (e.getSource() == rbmiInterval100) {
        timer.setDelay(100L);
      }
    }
  }

  private void updateDisplay() {
    long elapsed = timer.elapsed();
    elapsed /= 100L;
    final long fraction = elapsed % 10L;
    elapsed /= 10L;
    final long seconds = elapsed % 60L;
    elapsed /= 60L;
    final long minutes = elapsed % 60L;
    elapsed /= 60L;
    final long hours = elapsed % 60L;

    displayLabel.setText(String.format(DISPLAY_HMSF_FMT, hours, minutes, seconds, fraction));
  }

  private void init() {
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentHidden(ComponentEvent e) {
        resetTimer();
      }
    });

    final Font font = new Font(Font.MONOSPACED, Font.BOLD, 32);
    displayLabel.setFont(font);
    displayLabel.setHorizontalAlignment(SwingConstants.CENTER);

    resetButton.addActionListener(this);
    startButton.addActionListener(this);
    pauseButton.addActionListener(this);

    final ButtonGroup bg = new ButtonGroup();
    bg.add(rbmiInterval2000);
    bg.add(rbmiInterval1000);
    bg.add(rbmiInterval500);
    bg.add(rbmiInterval100);

    final ButtonPopupMenu bpmInterval = new ButtonPopupMenu("Display Interval...");
    bpmInterval.setIcon(Icons.ICON_ARROW_DOWN_15.getIcon());
    bpmInterval.addItem(rbmiInterval2000);
    bpmInterval.addItem(rbmiInterval1000);
    bpmInterval.addItem(rbmiInterval500);
    bpmInterval.addItem(rbmiInterval100);

    rbmiInterval2000.addItemListener(this);
    rbmiInterval1000.addItemListener(this);
    rbmiInterval500.addItemListener(this);
    rbmiInterval100.addItemListener(this);
    rbmiInterval1000.setSelected(true);

    resetButton.setEnabled(true);
    startButton.setEnabled(true);
    pauseButton.setEnabled(false);

    timer.addActionListener(this);

    final GridBagConstraints gbc = new GridBagConstraints();

    // button panel
    final JPanel buttonPanel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    buttonPanel.add(bpmInterval, gbc);
    ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 24, 0, 0), 0, 0);
    buttonPanel.add(resetButton, gbc);
    ViewerUtil.setGBC(gbc, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 8, 0, 0), 0, 0);
    buttonPanel.add(startButton, gbc);
    ViewerUtil.setGBC(gbc, 3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 8, 0, 0), 0, 0);
    buttonPanel.add(pauseButton, gbc);

    // main panel
    final JPanel mainPanel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0);
    mainPanel.add(displayLabel, gbc);
    ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
        new Insets(8, 0, 0, 0), 0, 0);
    mainPanel.add(buttonPanel, gbc);

    // dialog layout
    final Container pane = getContentPane();
    pane.setLayout(new GridBagLayout());
    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(8, 8, 8, 8), 0, 0);
    pane.add(mainPanel, gbc);

    pack();
    setMinimumSize(getSize());
    setMaximumSize(getSize());
    Center.center(this, NearInfinity.getInstance().getBounds());
  }
}
