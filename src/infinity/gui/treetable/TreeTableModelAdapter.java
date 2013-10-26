
package infinity.gui.treetable;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.TreePath;

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
      @Override
      public void treeExpanded(TreeExpansionEvent event)
      {
        fireTableDataChanged();
      }

      @Override
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
      @Override
      public void treeNodesChanged(TreeModelEvent e)
      {
        delayedFireTableDataChanged();
      }

      @Override
      public void treeNodesInserted(TreeModelEvent e)
      {
        delayedFireTableDataChanged();
      }

      @Override
      public void treeNodesRemoved(TreeModelEvent e)
      {
        delayedFireTableDataChanged();
      }

      @Override
      public void treeStructureChanged(TreeModelEvent e)
      {
        delayedFireTableDataChanged();
      }
    });
  }

// --------------------- Begin Interface TableModel ---------------------

  @Override
  public int getRowCount()
  {
    return tree.getRowCount();
  }

  // Wrappers, implementing TableModel interface.

  @Override
  public int getColumnCount()
  {
    return treeTableModel.getColumnCount();
  }

  @Override
  public Object getValueAt(int row, int column)
  {
    return treeTableModel.getValueAt(nodeForRow(row), column);
  }

// --------------------- End Interface TableModel ---------------------

  @Override
  public Class<? extends Object> getColumnClass(int column)
  {
    return treeTableModel.getColumnClass(column);
  }

  @Override
  public String getColumnName(int column)
  {
    return treeTableModel.getColumnName(column);
  }

  @Override
  public boolean isCellEditable(int row, int column)
  {
    return treeTableModel.isCellEditable(nodeForRow(row), column);
  }

  @Override
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
      @Override
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

