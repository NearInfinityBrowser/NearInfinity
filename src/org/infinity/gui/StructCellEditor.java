// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.infinity.datatype.InlineEditable;

/**
 * A specialized cell renderer for the {@code StructViewer} table.
 */
public class StructCellEditor extends AbstractCellEditor implements TableCellEditor
{
  /** The delegate class which handles all methods sent from the {@code CellEditor} */
  protected final StructEditorDelegate delegate = new StructEditorDelegate();

  /** The Swing component being edited. */
  protected InlineEditable editorComponent;

  /**
   * An integer specifying the number of clicks needed to start editing.
   * Even if {@codeclickCountToStart} is defined as zero, it will not initiate until a click occurs.
   */
  protected int clickCountToStart;

  public StructCellEditor()
  {
    // use double-click to enable editor
    clickCountToStart = 2;
  }

  /**
   * Returns a reference to the editor component.
   * @return the editor <code>Component</code>
   */
  public Component getComponent()
  {
    return (editorComponent != null) ? editorComponent.getEditor() : InlineEditable.DEFAULT_EDITOR;
  }

  /**
   * Specifies the number of clicks needed to start editing.
   * @param count  an int specifying the number of clicks needed to start editing
   * @see #getClickCountToStart
   */
  public void setClickCountToStart(int count)
  {
    clickCountToStart = count;
  }

  /**
   * Returns the number of clicks needed to start editing.
   * @return the number of clicks needed to start editing
   */
  public int getClickCountToStart()
  {
    return clickCountToStart;
  }

  /**
   * Forwards the message from the {@code CellEditor} to the {@code delegate}.
   * @see StructEditorDelegate#getCellEditorValue
   */
  @Override
  public Object getCellEditorValue()
  {
    return delegate.getCellEditorValue();
  }

  /**
   * Forwards the message from the {@code CellEditor} to the {@code delegate}.
   * @see StructEditorDelegate#isCellEditable(EventObject)
   */
  @Override
  public boolean isCellEditable(EventObject anEvent)
  {
    return delegate.isCellEditable(anEvent);
  }

  /**
   * Forwards the message from the {@code CellEditor} to the {@code delegate}.
   * @see StructEditorDelegate#shouldSelectCell(EventObject)
   */
  @Override
  public boolean shouldSelectCell(EventObject anEvent)
  {
    return delegate.shouldSelectCell(anEvent);
  }

  /**
   * Forwards the message from the {@code CellEditor} to the {@code delegate}.
   * @see StructEditorDelegate#stopCellEditing
   */
  @Override
  public boolean stopCellEditing()
  {
    return delegate.stopCellEditing();
  }

  /**
   * Forwards the message from the {@code CellEditor} to the {@code delegate}.
   * @see StructEditorDelegate#cancelCellEditing
   */
  @Override
  public void cancelCellEditing()
  {
    delegate.cancelCellEditing();
  }

  @Override
  public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
  {
    if (value instanceof InlineEditable) {
      editorComponent = (InlineEditable)value;
    }
    JComponent comp = (editorComponent != null) ? editorComponent.getEditor() : InlineEditable.DEFAULT_EDITOR;

    delegate.setValue(value);

    if (!comp.isOpaque()) {
//    if (comp instanceof JCheckBox) {
      //in order to avoid a "flashing" effect when clicking a checkbox
      //in a table, it is important for the editor to have as a border
      //the same border that the renderer has, and have as the background
      //the same color as the renderer has. This is primarily only
      //needed for JCheckBox since this editor doesn't fill all the
      //visual space of the table cell, unlike a text field.
      TableCellRenderer renderer = table.getCellRenderer(row, column);
      Component c = renderer.getTableCellRendererComponent(table, value, isSelected, true, row, column);
      if (c != null) {
        comp.setOpaque(true);
        comp.setBackground(c.getBackground());
        if (c instanceof JComponent) {
          comp.setBorder(((JComponent)c).getBorder());
        }
      } else {
        comp.setOpaque(false);
      }
    }

    return comp;
  }

//-------------------------- INNER CLASSES --------------------------

  protected class StructEditorDelegate implements ActionListener, ItemListener
  {
    /**
     * Returns the value of this cell.
     * @return the value of this cell
     */
    public Object getCellEditorValue()
    {
      return editorComponent.getEditorValue();
    }

    /**
     * Sets the value of this cell.
     * @param value the new value of this cell
     */
    public void setValue(Object value)
    {
      editorComponent.setEditorValue(value);
    }

    /**
     * Returns {@code true} if {@code anEvent} is <b>not</b> a {@code MouseEvent}.
     * Otherwise, it returns {@code true} if the necessary number of clicks have occurred,
     * and returns {@code false} otherwise.
     * @param anEvent the event
     * @return {@code true} if cell is ready for editing, {@code false} otherwise
     * @see #setClickCountToStart
     * @see #shouldSelectCell
     */
    public boolean isCellEditable(EventObject anEvent)
    {
      if (anEvent instanceof MouseEvent) {
        return ((MouseEvent)anEvent).getClickCount() >= clickCountToStart;
      }
      return true;
    }

    /**
     * Returns true to indicate that the editing cell may be selected.
     * @param anEvent the event
     * @return true
     * @see #isCellEditable
     */
    public boolean shouldSelectCell(EventObject anEvent)
    {
      return true;
    }

    /**
     * Returns true to indicate that editing has begun.
     * @param anEvent the event
     */
    public boolean startCellEditing(EventObject anEvent)
    {
      return true;
    }

    /**
     * Stops editing and returns true to indicate that editing has stopped.
     * This method calls <code>fireEditingStopped</code>.
     * @return  true
     */
    public boolean stopCellEditing()
    {
      fireEditingStopped();
      return true;
    }

    /**
     * Cancels editing. This method calls <code>fireEditingCanceled</code>.
     */
    public void cancelCellEditing()
    {
      fireEditingCanceled();
    }

    /**
     * When an action is performed, editing is ended.
     * @param e the action event
     * @see #stopCellEditing
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
      StructCellEditor.this.stopCellEditing();
    }

    /**
     * When an item's state changes, editing is ended.
     * @param e the action event
     * @see #stopCellEditing
     */
    @Override
    public void itemStateChanged(ItemEvent e)
    {
      StructCellEditor.this.stopCellEditing();
    }
  }
}
