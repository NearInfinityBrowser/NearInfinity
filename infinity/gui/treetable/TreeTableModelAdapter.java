
package infinity.gui.treetable;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.*;

final class TreeTableModelAdapter extends AbstractTableModel
{
  private final JTree tree;
  private final TreeTableModel treeTableModel;

  TreeTableModelAdapter(TreeTableModel treeTableModel, JTree tree)
  {
    this.tree = tree;
    this.treeTableModel = treeTableModel;

    tree.addTreeExpansionListener(new TreeExpansionListener()
    {
      // Don't use fireTableRowsInserted() here; the selection model
      // would get updated twice.
      public void treeExpanded(TreeExpansionEvent event)
      {
        fireTableDataChanged();
      }

      public void treeCollapsed(TreeExpansionEvent event)
      {
        fireTableDataChanged();
      }
    });

    // Install a TreeModelListener that can update the table when
    // tree changes. We use delayedFireTableDataChanged as we can
    // not be guaranteed the tree will have finished processing
    // the event before us.
    treeTableModel.addTreeModelListener(new TreeModelListener()
    {
      public void treeNodesChanged(TreeModelEvent e)
      {
        delayedFireTableDataChanged();
      }

      public void treeNodesInserted(TreeModelEvent e)
      {
        delayedFireTableDataChanged();
      }

      public void treeNodesRemoved(TreeModelEvent e)
      {
        delayedFireTableDataChanged();
      }

      public void treeStructureChanged(TreeModelEvent e)
      {
        delayedFireTableDataChanged();
      }
    });
  }

// --------------------- Begin Interface TableModel ---------------------

  public int getRowCount()
  {
    return tree.getRowCount();
  }

  // Wrappers, implementing TableModel interface.

  public int getColumnCount()
  {
    return treeTableModel.getColumnCount();
  }

  public Object getValueAt(int row, int column)
  {
    return treeTableModel.getValueAt(nodeForRow(row), column);
  }

// --------------------- End Interface TableModel ---------------------

  public Class getColumnClass(int column)
  {
    return treeTableModel.getColumnClass(column);
  }

  public String getColumnName(int column)
  {
    return treeTableModel.getColumnName(column);
  }

  public boolean isCellEditable(int row, int column)
  {
    return treeTableModel.isCellEditable(nodeForRow(row), column);
  }

  public void setValueAt(Object value, int row, int column)
  {
    treeTableModel.setValueAt(value, nodeForRow(row), column);
  }

  /**
   * Invokes fireTableDataChanged after all the pending events have been
   * processed. SwingUtilities.invokeLater is used to handle this.
   */
  private void delayedFireTableDataChanged()
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        fireTableDataChanged();
      }
    });
  }

  private Object nodeForRow(int row)
  {
    TreePath treePath = tree.getPathForRow(row);
    return treePath.getLastPathComponent();
  }
}

