
package infinity.gui.treetable;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.CellEditor;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;

public class AbstractCellEditor implements CellEditor
{
  private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();

// --------------------- Begin Interface CellEditor ---------------------

  @Override
  public Object getCellEditorValue() { return null; }

  @Override
  public boolean isCellEditable(EventObject e) { return true; }

  @Override
  public boolean shouldSelectCell(EventObject anEvent) { return false; }

  @Override
  public boolean stopCellEditing() { return true; }

  @Override
  public void cancelCellEditing() {}

  @Override
  public void addCellEditorListener(CellEditorListener l)
  {
    listeners.add(l);
  }

  @Override
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
