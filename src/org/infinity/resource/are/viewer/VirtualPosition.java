// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.nio.ByteBuffer;

import javax.swing.JButton;
import javax.swing.JOptionPane;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.IsNumeric;
import org.infinity.gui.ButtonPanel;
import org.infinity.gui.StructViewer;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.util.Logger;
import org.infinity.util.StructClipboard;
import org.infinity.util.io.StreamUtils;

/**
 * Creates an {@link AbstractStruct} instance and initializes it with the specified x and y coordinates.
 * <p>
 * Instances of this class can be used as child structures by the {@link VirtualMap} class.
 * </p>
 */
public class VirtualPosition extends AbstractStruct implements AddRemovable {
  public static final String POSITION   = "Map Position";
  public static final String POSITION_X = "X";
  public static final String POSITION_Y = "Y";

  private static VirtualPosition instance;

  /** Returns the singleton {@code MapPosition} instance. */
  public static VirtualPosition getInstance() {
    if (instance == null) {
      try {
        instance = new VirtualPosition(null, 0, 0);
      } catch (Exception e) {
        Logger.error(e);
      }
    }
    return instance;
  }

  /** Required for prototyping instances of this class. */
  VirtualPosition() throws Exception {
    super(null, POSITION, StreamUtils.getByteBuffer(4), 0, 2);
  }

  /** Creates a new {@code MapPosition} structure and initializes it with the specified coordinates. */
  public VirtualPosition(AbstractStruct superStruct, int x, int y) throws Exception {
    super(null, POSITION, getBufferData(x, y), 0, 2);
  }

  /** Creates a new indexed {@code MapPosition} structure and initializes it with the specified coordinates. */
  public VirtualPosition(AbstractStruct superStruct, int x, int y, int nr) throws Exception {
    super(null, POSITION + " " + nr, getBufferData(x, y), 0, 2);
  }

  /** Creates a new indexed {@code MapPosition} structure and initializes it with data from the specified buffer. */
  public VirtualPosition(AbstractStruct superStruct, ByteBuffer buffer, int offset, int nr) throws Exception {
    super(superStruct, POSITION + " " + nr, buffer, offset, 2);
  }

  /** Creates a new {@code MapPosition} structure and initializes it with data from the specified buffer. */
  public VirtualPosition(AbstractStruct superStruct, String name, ByteBuffer buffer, int offset) throws Exception {
    super(superStruct, name, buffer, offset, 2);
  }

  // --------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove() {
    return true;
  }

  // --------------------- End Interface AddRemovable ---------------------

  @Override
  protected void viewerInitialized(StructViewer viewer) {
    final ButtonPanel panel = viewer.getButtonPanel();
    final JButton copyButton = new JButton("Copy position", Icons.ICON_COPY_16.getIcon());
    copyButton.setToolTipText("Copies x and y coordinates of the map position to the clipboard.");
    copyButton.addActionListener(e -> {
      if (!copyToClipboard(true, true)) {
        JOptionPane.showMessageDialog(viewer, "Could not copy map coordinates to the clipboard.", "Error",
            JOptionPane.ERROR_MESSAGE);
      }
    });
    panel.addControl(copyButton, ButtonPanel.Control.CUSTOM_1);
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception {
    addField(new DecNumber(buffer, offset, 2, POSITION_X));
    addField(new DecNumber(buffer, offset + 2, 2, POSITION_Y));
    return offset + 4;
  }

  /** Returns the current value of the x coordinate. */
  public int getX() {
    return ((IsNumeric)getAttribute(POSITION_X)).getValue();
  }

  /** Sets a new x coordinate and returns the current {@code MapPosition} instance. */
  public VirtualPosition setX(int value) {
    ((DecNumber)getAttribute(POSITION_X)).setValue(value);
    return this;
  }

  /** Returns the current value of the y coordinate. */
  public int getY() {
    return ((IsNumeric)getAttribute(POSITION_Y)).getValue();
  }

  /** Sets a new y coordinate and returns the current {@code MapPosition} instance. */
  public VirtualPosition setY(int value) {
    ((DecNumber)getAttribute(POSITION_Y)).setValue(value);
    return this;
  }

  /** Sets the new x and y coordinates to the structure and returns the current {@code MapPosition} instance. */
  public VirtualPosition setPosition(int x, int y) {
    ((DecNumber)getAttribute(POSITION_X)).setValue(x);
    ((DecNumber)getAttribute(POSITION_Y)).setValue(y);
    return this;
  }

  /**
   * Copies the current map position to the clipboard.
   *
   * @param internal Indicates whether to copy the position to the internal {@link StructClipboard} instance.
   * @param global   Indicates whether to copy the position to the clipboard of the system as a formatted string.
   * @return {@code true} if the operation succeeded, {@code false} otherwise.
   */
  public boolean copyToClipboard(boolean internal, boolean global) {
    try {
      if (global) {
        final String s = getX() + "," + getY();
        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(s), null);
      }
      if (internal) {
        StructClipboard.getInstance().copyValue(this, 0, 1, false);
      }
      return true;
    } catch (Exception e) {
      Logger.warn(e);
    }
    return false;
  }

  /** Initializes the internal byte buffer with the specified coordinates. */
  private static ByteBuffer getBufferData(int x, int y) {
    final ByteBuffer bb = StreamUtils.getByteBuffer(4);
    bb.putShort(0, (short)x);
    bb.putShort(2, (short)y);
    return bb;
  }
}