// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;

import org.infinity.gui.menu.BrowserMenuBar;

public final class StatusBar extends JPanel implements CaretListener, ActionListener {
  private final JLabel messageLabel = new JLabel();
  private final JLabel cursorLabel = new JLabel();
  private final JProgressBar memoryProgress = new JProgressBar(JProgressBar.HORIZONTAL);
  private final Timer updateTimer = new Timer(2000, this);

  public StatusBar() {
    super(new GridBagLayout());
    setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

    messageLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
        BorderFactory.createLineBorder(UIManager.getColor("controlShadow"))));
    cursorLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
        BorderFactory.createLineBorder(UIManager.getColor("controlShadow"))));
    cursorLabel.setPreferredSize(new Dimension(120, cursorLabel.getPreferredSize().height));
    cursorLabel.setMinimumSize(cursorLabel.getPreferredSize());

    if (BrowserMenuBar.getInstance().getOptions().showMemStatus()) {
      memoryProgress.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
          BorderFactory.createLineBorder(UIManager.getColor("controlShadow"))));
      memoryProgress.setStringPainted(true);
      memoryProgress.setMinimumSize(memoryProgress.getPreferredSize());
    }

    final GridBagConstraints c = new GridBagConstraints();
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
        new Insets(0, 0, 0, 0), 0, 0);
    add(messageLabel, c);
    ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.VERTICAL,
        new Insets(0, 3, 0, 0), 0, 0);
    add(cursorLabel, c);

    if (BrowserMenuBar.getInstance().getOptions().showMemStatus()) {
      ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.VERTICAL,
          new Insets(0, 3, 0, 0), 0, 0);
      add(memoryProgress, c);

      updateMemoryProgress();
      updateTimer.setInitialDelay(updateTimer.getDelay());
      updateTimer.setRepeats(true);
      updateTimer.start();
    }
  }

  // --------------------- Begin Interface CaretListener ---------------------

  @Override
  public void caretUpdate(CaretEvent event) {
    if (event.getSource() instanceof JTextArea) {
      JTextArea source = (JTextArea) event.getSource();
      int position = event.getDot();
      try {
        int linenr = source.getLineOfOffset(position);
        cursorLabel.setText(String.format(" %d:%d", linenr + 1, 1 + position - source.getLineStartOffset(linenr)));
      } catch (BadLocationException e) {
        cursorLabel.setText("");
      }
    }
  }

  // --------------------- End Interface CaretListener ---------------------

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == updateTimer) {
      updateMemoryProgress();
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  public void setCursorText(String text) {
    cursorLabel.setText(' ' + text);
  }

  public String getCursorText() {
    String text = cursorLabel.getText();
    if (!text.isEmpty()) {
      return text.substring(1);
    } else {
      return "";
    }
  }

  public void setMessage(String msg) {
    messageLabel.setText(' ' + msg);
    cursorLabel.setText("");
  }

  public String getMessage() {
    String text = messageLabel.getText();
    if (!text.isEmpty()) {
      return text.substring(1);
    } else {
      return "";
    }
  }

  /**
   * Calculates currently used memory and updates the {@link JProgressBar} element.
   */
  private void updateMemoryProgress() {
    // using non-linear scaling for better resolution in lower memory ranges
    final Function<Long, Integer> logScaled = value -> (int) Math.pow(Math.log(value / 1024.0), 6.0);

    final Runtime rt = Runtime.getRuntime();

    // total memory available to the VM
    final long maxMemory = rt.maxMemory();
    memoryProgress.setMaximum(logScaled.apply(maxMemory));

    // used memory
    final long usedMemory = rt.totalMemory() - rt.freeMemory();
    final int value = Math.min(Math.max(logScaled.apply(usedMemory), 0), memoryProgress.getMaximum());
    memoryProgress.setValue(value);

    final long usedMemoryMB = usedMemory / (1024L * 1024L);
    if (usedMemoryMB >= 1024L) {
      final double usedMemoryGB = usedMemoryMB / 1024.0;
      memoryProgress.setString(String.format("%.2f GB", usedMemoryGB));
    } else {
      memoryProgress.setString(usedMemoryMB + " MB");
    }

    // updating tooltip
    final long maxMemoryMB = maxMemory / (1024L * 1024L);
    final double usedMemoryPercent = (double) usedMemory / (double) maxMemory * 100.0;
    final String msg = String.format("Memory used by Near Infinity: %d MB of %d MB (%.1f %%)",
        usedMemoryMB, maxMemoryMB, usedMemoryPercent);
    memoryProgress.setToolTipText(msg);
  }
}
