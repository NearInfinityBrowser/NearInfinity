// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JOptionPane;

import org.infinity.NearInfinity;
import org.infinity.util.LauncherUtils;
import org.tinylog.Logger;

/**
 * Browses for the provided URI on mouse clicks
 *
 * @author Fredrik Lindgren
 */
public class UrlBrowser implements MouseListener {
  private final URI url;

  /** Opens the specified URL in the system's default browser. */
  public static boolean openUrl(String url) {
    boolean retVal = false;
    try {
      LauncherUtils.browse(url);
      retVal = true;
    } catch (IOException | URISyntaxException e) {
      Logger.error(e);
      showErrorMessage(e.getMessage());
    }
    return retVal;
  }

  public UrlBrowser(String urlText) {
    url = URI.create(urlText);
  }

  private static void showErrorMessage(String details) {
    String errorMessage;
    if (details == null || details.trim().isEmpty()) {
      errorMessage = "URL cannot be opened on this system.";
    } else {
      errorMessage = "URL cannot be opened:\n" + details;
    }
    final String errorTitle = "Attention";
    JOptionPane.showMessageDialog(NearInfinity.getInstance(), errorMessage, errorTitle, JOptionPane.PLAIN_MESSAGE);
  }

  // --------------------- Begin Interface MouseListener ---------------------

  @Override
  public void mouseClicked(MouseEvent event) {
    try {
      LauncherUtils.browse(url);
    } catch (IOException e) {
      Logger.error(e);
      showErrorMessage(e.getMessage());
    }
  }

  @Override
  public void mousePressed(MouseEvent e) {
  }

  @Override
  public void mouseReleased(MouseEvent e) {
  }

  @Override
  public void mouseEntered(MouseEvent e) {
  }

  @Override
  public void mouseExited(MouseEvent e) {
  }

  // --------------------- End Interface MouseListener ---------------------

}
