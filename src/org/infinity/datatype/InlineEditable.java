// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;

import org.infinity.gui.BrowserMenuBar;
import org.infinity.resource.StructEntry;
import org.infinity.util.Misc;

public interface InlineEditable extends StructEntry
{
  /** The background color used for table cells. */
  static final Color GRID_BACKGROUND =
      (UIManager.getColor("Table.focusCellBackground") != null) ? UIManager.getColor("Table.focusCellBackground") : Color.WHITE;
  /** The border color used for table cells. */
  static final Color GRID_BORDER =
      (UIManager.getColor("Table.gridColor") != null) ? UIManager.getColor("Table.gridColor") : Color.BLACK;

  /** The default component used for the inline editor. */
  static final JTextField DEFAULT_EDITOR = new JTextField() {{
      setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getScriptFont()));
      setBorder(new LineBorder(GRID_BORDER, 1));
      setBackground(GRID_BACKGROUND);
  }};

  /** Called when the specified editor value should be applied to the {@code InlineEditable} object. */
  boolean update(Object value);

  /**
   * Returns the editing component that is used for editing a value within the table cell.
   * This method is called whenever the editor is activated.
   * <p><b>Note:</b> For performance reasons the returned component should only be created once and reused afterwards.
   */
  default JComponent getEditor() { return DEFAULT_EDITOR; }

  /** This method is called to return the edited data in a format supported by the {@code InlineEditable} object. */
  default Object getEditorValue() { return DEFAULT_EDITOR.getText(); }

  /** This method is called to initialize the editor with the specified data. */
  default void setEditorValue(Object data) { DEFAULT_EDITOR.setText((data != null) ? data.toString() : ""); }
}

