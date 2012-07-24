// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.check;

import infinity.NearInfinity;
import infinity.datatype.DecNumber;
import infinity.datatype.HexNumber;
import infinity.gui.*;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.cre.CreResource;
import infinity.resource.cre.Item;
import infinity.resource.key.ResourceEntry;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public final class CreInvChecker implements Runnable, ActionListener, ListSelectionListener
{
  private final List<StructEntry> items = new ArrayList<StructEntry>();
  private final List<StructEntry> slots = new ArrayList<StructEntry>();
  private ChildFrame resultFrame;
  private JButton bopen, bopennew;
  private SortableTable table;

  public CreInvChecker()
  {
    new Thread(this).start();
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bopen) {
      int row = table.getSelectedRow();
      if (row != -1) {
        ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
        NearInfinity.getInstance().showResourceEntry(resourceEntry);
        ((AbstractStruct)NearInfinity.getInstance().getViewable()).getViewer().selectEntry(
                ((Item)table.getValueAt(row, 2)).getName());
      }
    }
    else if (event.getSource() == bopennew) {
      int row = table.getSelectedRow();
      if (row != -1) {
        ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
        Resource resource = ResourceFactory.getResource(resourceEntry);
        new ViewFrame(resultFrame, resource);
        ((AbstractStruct)resource).getViewer().selectEntry(((Item)table.getValueAt(row, 2)).getName());
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  public void valueChanged(ListSelectionEvent event)
  {
    bopen.setEnabled(true);
    bopennew.setEnabled(true);
  }

// --------------------- End Interface ListSelectionListener ---------------------


// --------------------- Begin Interface Runnable ---------------------

  public void run()
  {
    WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    blocker.setBlocked(true);
    List<ResourceEntry> creFiles = ResourceFactory.getInstance().getResources("CRE");
    creFiles.addAll(ResourceFactory.getInstance().getResources("CHR"));
    ProgressMonitor progress = new ProgressMonitor(NearInfinity.getInstance(),
                                                   "Checking inventories...", null, 0, creFiles.size());
    table = new SortableTable(new String[]{"File", "Name", "Item"},
                              new Class[]{Object.class, Object.class, Object.class},
                              new int[]{100, 100, 200});
    for (int i = 0; i < creFiles.size(); i++) {
      ResourceEntry entry = creFiles.get(i);
      try {
        checkCreature(new CreResource(entry));
      } catch (Exception e) {
        e.printStackTrace();
      }
      progress.setProgress(i + 1);
      if (progress.isCanceled()) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Operation canceled",
                                      "Info", JOptionPane.INFORMATION_MESSAGE);
        blocker.setBlocked(false);
        return;
      }
    }

    if (table.getRowCount() == 0)
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No hits found",
                                    "Info", JOptionPane.INFORMATION_MESSAGE);
    else {
      resultFrame = new ChildFrame("Result of CRE inventory check", true);
      resultFrame.setIconImage(Icons.getIcon("Refresh16.gif").getImage());
      bopen = new JButton("Open", Icons.getIcon("Open16.gif"));
      bopennew = new JButton("Open in new window", Icons.getIcon("Open16.gif"));
      JLabel count = new JLabel(table.getRowCount() + " hit(s) found", JLabel.CENTER);
      count.setFont(count.getFont().deriveFont((float)count.getFont().getSize() + 2.0f));
      bopen.setMnemonic('o');
      bopennew.setMnemonic('n');
      resultFrame.getRootPane().setDefaultButton(bopennew);
      JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      panel.add(bopen);
      panel.add(bopennew);
      JScrollPane scrollTable = new JScrollPane(table);
      scrollTable.getViewport().setBackground(table.getBackground());
      JPanel pane = (JPanel)resultFrame.getContentPane();
      pane.setLayout(new BorderLayout(0, 3));
      pane.add(count, BorderLayout.NORTH);
      pane.add(scrollTable, BorderLayout.CENTER);
      pane.add(panel, BorderLayout.SOUTH);
      bopen.setEnabled(false);
      bopennew.setEnabled(false);
      table.setFont(BrowserMenuBar.getInstance().getScriptFont());
      table.getSelectionModel().addListSelectionListener(this);
      table.addMouseListener(new MouseAdapter()
      {
        public void mouseReleased(MouseEvent event)
        {
          if (event.getClickCount() == 2) {
            int row = table.getSelectedRow();
            if (row != -1) {
              ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
              Resource resource = ResourceFactory.getResource(resourceEntry);
              new ViewFrame(resultFrame, resource);
              ((AbstractStruct)resource).getViewer().selectEntry((String)table.getValueAt(row, 1));
            }
          }
        }
      });
      bopen.addActionListener(this);
      bopennew.addActionListener(this);
      pane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      resultFrame.pack();
      Center.center(resultFrame, NearInfinity.getInstance().getBounds());
      resultFrame.setVisible(true);
    }
    blocker.setBlocked(false);
//    for (int i = 0; i < table.getRowCount(); i++) {
//      CreInvError error = (CreInvError)table.getTableItemAt(i);
//      System.out.println(error.resourceEntry + " (" + error.resourceEntry.getSearchString() + ") -> " + error.itemRef.getAttribute("Item"));
//    }
  }

// --------------------- End Interface Runnable ---------------------

  private void checkCreature(CreResource cre)
  {
    HexNumber slots_offset = (HexNumber)cre.getAttribute("Item slots offset");
    items.clear();
    slots.clear();
    for (int i = 0; i < cre.getRowCount(); i++) {
      StructEntry entry = cre.getStructEntryAt(i);
      if (entry instanceof Item)
        items.add(entry);
      else if (entry.getOffset() >= slots_offset.getValue() + cre.getOffset() &&
               entry instanceof DecNumber
               && !entry.getName().equals("Weapon slot selected")
               && !entry.getName().equals("Weapon ability selected"))
        slots.add(entry);
    }
    for (int i = 0; i < slots.size(); i++) {
      DecNumber slot = (DecNumber)slots.get(i);
      if (slot.getValue() >= 0 && slot.getValue() < items.size())
        items.set(slot.getValue(), slots_offset); // Dummy object
    }
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i) != slots_offset) {
        Item item = (Item)items.get(i);
        table.addTableItem(new CreInvError(cre.getResourceEntry(), item));
      }
    }
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class CreInvError implements TableItem
  {
    private final ResourceEntry resourceEntry;
    private final Item itemRef;

    private CreInvError(ResourceEntry resourceEntry, Item itemRef)
    {
      this.resourceEntry = resourceEntry;
      this.itemRef = itemRef;
    }

    public Object getObjectAt(int columnIndex)
    {
      if (columnIndex == 0)
        return resourceEntry;
      else if (columnIndex == 1)
        return resourceEntry.getSearchString();
      else
        return itemRef;
    }
  }
}

