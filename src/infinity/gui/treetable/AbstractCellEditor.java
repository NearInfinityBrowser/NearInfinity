
package infinity.gui.treetable;

import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

public class AbstractCellEditor implements CellEditor
{
  private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();

// --------------------- Begin Interface CellEditor ---------------------

  public Object getCellEditorValue() { return null; }

  public boolean isCellEditable(EventObject e) { return true; }

  public boolean shouldSelectCell(EventObject anEvent) { return false; }

  public boolean stopCellEditing() { return true; }

  public void cancelCellEditing() {}

  public void addCellEditorListener(CellEditorListener l)
  {
    listeners.add(l);
  }

  public void removeCellEditorListener(CellEditorListener l)
  {
    listeners.remove(l);
  }

// --------------------- End Interface CellEditor ---------------------

  protected void fireEditingCanceled()
  {
    ChangeEvent event = new ChangeEvent(this);
    for (int i = 0; i < listeners.size(); i++)
      listeners.get(i).editingCanceled(event);
  }

  protected void fireEditingStopped()
  {
    ChangeEvent event = new ChangeEvent(this);
    for (int i = 0; i < listeners.size(); i++)
      listeners.get(i).editingStopped(event);
  }
}
