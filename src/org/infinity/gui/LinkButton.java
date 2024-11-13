// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.infinity.NearInfinity;
import org.infinity.datatype.ResourceRef;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.updater.Utils;
import org.infinity.util.Logger;

/**
 * A JLabel-based control which supports either internal game resources or external URLs.
 */
public class LinkButton extends JLabel implements MouseListener, ActionListener {
  private static final String CMD_OPEN      = "OPEN"; // open resource in same window
  private static final String CMD_OPEN_NEW  = "OPEN_NEW"; // open resource in new window
  private static final String CMD_BROWSE    = "BROWSE"; // open URL in system-default browser

  /** Tooltip text if an alternate resource reference is used for the link button. */
  private static final String TOOLTIP_ALTERNATE_RESREF = "No resource assigned: Showing default resource.";

  /** Color of the linked text if an alternate resource reference is used (for light and dark L&F themes). */
  private static final Color LINK_COLOR_ALTERNATE_RESREF_LIGHT = new Color(0x800000);
  private static final Color LINK_COLOR_ALTERNATE_RESREF_DARK = new Color(0xC04000);

  private final List<ActionListener> listeners = new ArrayList<>();

  private ResourceEntry entry;
  private String url;
  private boolean isResource;

  /**
   * Creates a link button which points to an internal game resource as specified by the argument.
   *
   * @param resourceRef The game resource as ResourceRef object.
   */
  public LinkButton(ResourceRef resourceRef) {
    this(resourceRef, 0);
  }

  /**
   * Creates a link button which points to an internal game resource as specified by the argument.
   *
   * @param resourceRef The game resource as ResourceRef object.
   * @param maxLength   Max. number of characters displayed in the label text. Full string is displayed as tooltip
   *                    instead.
   */
  public LinkButton(ResourceRef resourceRef, int maxLength) {
    super();
    setHorizontalAlignment(SwingConstants.LEFT);
    setResource(resourceRef, maxLength);
  }

  /**
   * Creates a link button which points to an internal game resource as specified by the argument.
   *
   * @param resourceRef The game resource as ResourceRef object.
   * @param maxLength   Max. number of characters displayed in the label text. Full string is displayed as tooltip
   *                      instead.
   * @param isAlternate Specify {@code true} to change link color and tooltip to indicate that a fallback resource is
   *                      used.
   */
  public LinkButton(ResourceRef resourceRef, int maxLength, boolean isAlternate) {
    setHorizontalAlignment(SwingConstants.LEFT);
    setResource(resourceRef, maxLength, isAlternate);
  }

  /**
   * Creates a link button which points to an internal game resource as specified by the argument.
   *
   * @param resourceName The game resource as string.
   */
  public LinkButton(String resourceName) {
    this(resourceName, 0);
  }

  /**
   * Creates a link button which points to an internal game resource as specified by the argument.
   *
   * @param resourceName The game resource as string.
   * @param maxLength    Max. number of characters displayed in the label text. Full string is displayed as tooltip
   *                     instead.
   */
  public LinkButton(String resourceName, int maxLength) {
    this(resourceName, maxLength, false);
  }

  /**
   * Creates a link button which points to an internal game resource as specified by the argument.
   *
   * @param resourceName The game resource as string.
   * @param maxLength    Max. number of characters displayed in the label text. Full string is displayed as tooltip
   *                       instead.
   * @param isAlternate  Specify {@code true} to change link color and tooltip to indicate that a fallback resource is
   *                       used.
   */
  public LinkButton(String resourceName, int maxLength, boolean isAlternate) {
    super();
    setHorizontalAlignment(SwingConstants.LEFT);
    setResource(resourceName, maxLength, isAlternate);
  }

  /**
   * Creates a link button which points to an external URL.
   *
   * @param text The display name of the link.
   * @param url  The actual URL of the link.
   */
  public LinkButton(String text, String url) {
    this(text, url, 0);
  }

  /**
   * Creates a link button which points to an external URL.
   *
   * @param text      The display name of the link.
   * @param url       The actual URL of the link.
   * @param maxLength Max. number of characters displayed in the label text. Full string is displayed as tooltip
   *                  instead.
   */
  public LinkButton(String text, String url, int maxLength) {
    super();
    setHorizontalAlignment(SwingConstants.LEFT);
    setUrl(text, url, maxLength);
  }

  /** Creates a link from the specified resource reference. */
  public void setResource(ResourceRef resourceRef) {
    setResource(resourceRef, 0);
  }

  /** Creates a link from the specified resource reference. */
  public void setResource(ResourceRef resourceRef, int maxLength) {
    setResource(resourceRef, maxLength, false);
  }

  /** Creates a link from the specified resource reference. */
  public void setResource(ResourceRef resourceRef, int maxLength, boolean isAlternate) {
    if (resourceRef != null) {
      setResource(ResourceFactory.getResourceEntry(resourceRef.getResourceName()), resourceRef.toString(), maxLength,
          isAlternate);
    } else {
      setResource(null, null, maxLength);
    }
  }

  /** Attempts to create a link from the specified resource name. */
  public void setResource(String resourceName) {
    setResource(ResourceFactory.getResourceEntry(resourceName), resourceName, 0);
  }

  /** Attempts to create a link from the specified resource name. */
  public void setResource(String resourceName, int maxLength) {
    setResource(resourceName, maxLength, false);
  }

  /**
   * Attempts to create a link from the specified resource name. Link color and tooltip are changed if
   * {@code isAlternate} is {@code true}.
   */
  public void setResource(String resourceName, int maxLength, boolean isAlternate) {
    setResource(ResourceFactory.getResourceEntry(resourceName), resourceName, maxLength, isAlternate);
  }

  private void setResource(ResourceEntry entry, String resourceName, int maxLength) {
    setResource(entry, resourceName, maxLength, false);
  }

  private void setResource(ResourceEntry entry, String resourceName, int maxLength, boolean isAlternate) {
    isResource = true;
    removeActionListener(this);
    this.entry = entry;
    if (entry != null) {
      addActionListener(this);
      setLink(resourceName, entry.getResourceName(), true, maxLength, isAlternate);
      setEnabled(true);
      // setToolTipText(null);
    } else {
      setLink(resourceName, null, false, maxLength);
      setEnabled(false);
      setToolTipText("Resource not found");
    }
  }

  /** Sets link or label text, depending on arguments. */
  private void setLink(String text, String resource, boolean asLink, int maxLength) {
    setLink(text, resource, asLink, maxLength, false);
  }

  /** Sets link or label text, depending on arguments. */
  private void setLink(String text, String resource, boolean asLink, int maxLength, boolean isAlternate) {
    removeMouseListener(this);
    setCursor(null);

    if (text == null) {
      text = resource;
    }

    String toolTip = isAlternate ? TOOLTIP_ALTERNATE_RESREF : null;
    Color color = isAlternate ? getAlternateLinkColor() : null;

    if (maxLength > 0 && text != null && text.length() > maxLength) {
      if (toolTip == null || toolTip.isEmpty()) {
        toolTip = text;
      } else {
        toolTip = text + " - " + toolTip;
      }
      text = text.substring(0, maxLength) + "...";
    }

    if (!asLink) {
      setText(text);
    } else if (resource != null && !resource.isEmpty()) {
      final String colorAttr;
      if (color != null) {
        colorAttr = String.format(" style=\"color: #%02X%02X%02X\"", color.getRed(), color.getGreen(), color.getBlue());
      } else {
        colorAttr = "";
      }
      setText("<html><a" + colorAttr + " href=\"" + resource + "\">" + text + "</a></html");
      addMouseListener(this);
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    } else {
      setText("");
    }

    if (toolTip != null) {
      setToolTipText(toolTip);
    }
  }

  /** Returns a color value that is suitable for the current L&F UI scheme. */
  private static Color getAlternateLinkColor() {
    final Color color = UIManager.getColor("Panel.background");
    final int brightness = Math.max(Math.max(color.getRed(), color.getGreen()), color.getBlue());
    if (brightness >= 0xa0) {
      return LINK_COLOR_ALTERNATE_RESREF_LIGHT;
    } else {
      return LINK_COLOR_ALTERNATE_RESREF_DARK;
    }
  }

  /** Creates a link to an external URL. */
  public void setUrl(String text, String url) {
    setUrl(text, url, 0);
  }

  /** Creates a link to an external URL. */
  public void setUrl(String text, String url, int maxLength) {
    isResource = false;
    if (url == null || url.isEmpty()) {
      url = "about:blank";
    }
    if (text == null || text.isEmpty()) {
      text = url;
    }
    this.url = url;
    addActionListener(this);
    setLink(text, url, true, maxLength);
    setEnabled(true);
  }

  /** Returns the external link or internal resource entry as string. */
  public String getUrl() {
    if (isResource) {
      return entry.getResourceName();
    } else {
      return url;
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();
    if ((cmd == null) || cmd.equals(CMD_OPEN_NEW)) {
      new ViewFrame(((LinkButton) e.getSource()).getTopLevelAncestor(), ResourceFactory.getResource(entry));
    } else if (cmd.equals(CMD_OPEN)) {
      NearInfinity.getInstance().showResourceEntry(entry);
    } else if (cmd.equals(CMD_BROWSE)) {
      try {
        Utils.openWebPage(new URL(getUrl()));
      } catch (Exception ex) {
        Logger.error(ex);
        JOptionPane.showMessageDialog(((LinkButton) e.getSource()).getTopLevelAncestor(),
            "Error opening link in browser.", "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    String cmd;
    if (isResource) {
      if ((e.getButton() == MouseEvent.BUTTON2) || (e.getButton() == MouseEvent.BUTTON3)) {
        cmd = CMD_OPEN;
      } else {
        cmd = CMD_OPEN_NEW;
      }
    } else {
      cmd = CMD_BROWSE;
    }

    ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, cmd);
    for (final ActionListener l : listeners) {
      l.actionPerformed(event);
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

  public void removeActionListener(ActionListener listener) {
    listeners.remove(listener);
  }

  private void addActionListener(ActionListener listener) {
    listeners.add(listener);
  }
}
