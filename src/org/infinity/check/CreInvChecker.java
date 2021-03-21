// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.check;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.NearInfinity;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.IsNumeric;
import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.SortableTable;
import org.infinity.gui.TableItem;
import org.infinity.gui.ViewFrame;
import org.infinity.gui.WindowBlocker;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.cre.Item;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.AbstractSearcher;
import org.infinity.util.Misc;

/** Performs checking {@link CreResource CRE} & {@code CHR} resources. */
public final class CreInvChecker extends AbstractSearcher implements Runnable, ActionListener, ListSelectionListener
{
  private ChildFrame resultFrame;
  private JButton bopen, bopennew, bsave;
  /** List of the {@link CreInvError} objects. */
  private SortableTable table;

  public CreInvChecker(Component parent)
  {
    super(CHECK_ONE_TYPE_FORMAT, parent);
    new Thread(this).start();
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
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
    } else if (event.getSource() == bsave) {
      table.saveCheckResult(resultFrame, "CRE items not in inventory");
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent event)
  {
    bopen.setEnabled(true);
    bopennew.setEnabled(true);
  }

// --------------------- End Interface ListSelectionListener ---------------------


// --------------------- Begin Interface Runnable ---------------------

  @Override
  public void run()
  {
    final WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    blocker.setBlocked(true);
    try {
      final List<ResourceEntry> creFiles = ResourceFactory.getResources("CRE");
      creFiles.addAll(ResourceFactory.getResources("CHR"));

      table = new SortableTable(new String[]{"File", "Name", "Item"},
                                new Class<?>[]{ResourceEntry.class, String.class, Item.class},
                                new Integer[]{100, 100, 200});

      if (runSearch("Checking inventories", creFiles)) {
        return;
      }

      if (table.getRowCount() == 0) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No hits found",
                                      "Info", JOptionPane.INFORMATION_MESSAGE);
      } else {
        resultFrame = new ChildFrame("Result of CRE inventory check", true);
        resultFrame.setIconImage(Icons.getIcon(Icons.ICON_REFRESH_16).getImage());
        bopen = new JButton("Open", Icons.getIcon(Icons.ICON_OPEN_16));
        bopennew = new JButton("Open in new window", Icons.getIcon(Icons.ICON_OPEN_16));
        bsave = new JButton("Save...", Icons.getIcon(Icons.ICON_SAVE_16));
        JLabel count = new JLabel(table.getRowCount() + " hit(s) found", JLabel.CENTER);
        count.setFont(count.getFont().deriveFont((float)count.getFont().getSize() + 2.0f));
        bopen.setMnemonic('o');
        bopennew.setMnemonic('n');
        bsave.setMnemonic('s');
        resultFrame.getRootPane().setDefaultButton(bopennew);
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.add(bopen);
        panel.add(bopennew);
        panel.add(bsave);
        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.getViewport().setBackground(table.getBackground());
        JPanel pane = (JPanel)resultFrame.getContentPane();
        pane.setLayout(new BorderLayout(0, 3));
        pane.add(count, BorderLayout.NORTH);
        pane.add(scrollTable, BorderLayout.CENTER);
        pane.add(panel, BorderLayout.SOUTH);
        bopen.setEnabled(false);
        bopennew.setEnabled(false);
        table.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getScriptFont()));
        table.setRowHeight(table.getFontMetrics(table.getFont()).getHeight() + 1);
        table.getSelectionModel().addListSelectionListener(this);
        table.addMouseListener(new MouseAdapter()
        {
          @Override
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
        bsave.addActionListener(this);
        pane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        resultFrame.pack();
        Center.center(resultFrame, NearInfinity.getInstance().getBounds());
        resultFrame.setVisible(true);
      }
    } finally {
      blocker.setBlocked(false);
    }
  }

// --------------------- End Interface Runnable ---------------------

  @Override
  protected Runnable newWorker(ResourceEntry entry)
  {
    return () -> {
      try {
        checkCreature(new CreResource(entry));
      } catch (Exception e) {
        synchronized (System.err) {
          e.printStackTrace();
        }
      }
      advanceProgress();
    };
  }

  private void checkCreature(CreResource cre)
  {
    final List<Item> items = new ArrayList<>();
    final List<DecNumber> slots = new ArrayList<>();
    final IsNumeric slots_offset = (IsNumeric)cre.getAttribute(CreResource.CRE_OFFSET_ITEM_SLOTS);
    for (final StructEntry entry : cre.getFields()) {
      if (entry instanceof Item) {
        items.add((Item)entry);
      } else if (entry.getOffset() >= slots_offset.getValue() + cre.getOffset() &&
               entry instanceof DecNumber
               && !entry.getName().equals(CreResource.CRE_SELECTED_WEAPON_SLOT)
               && !entry.getName().equals(CreResource.CRE_SELECTED_WEAPON_ABILITY))
        slots.add((DecNumber)entry);
    }
    //TODO: Investigate ability to changes slots to set and use slots.contains(...) below
    for (DecNumber slot : slots) {
      final int value = slot.getValue();
      if (value >= 0 && value < items.size()) {
        items.set(value, null);
      }
    }
    for (Item item : items) {
      if (item != null) {
        synchronized (table) {
          table.addTableItem(new CreInvError(cre.getResourceEntry(), item));
        }
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

    @Override
    public Object getObjectAt(int columnIndex)
    {
      if (columnIndex == 0)
        return resourceEntry;
      else if (columnIndex == 1)
        return resourceEntry.getSearchString();
      else
        return itemRef;
    }

    @Override
    public String toString()
    {
      return String.format("File: %s, Name: %s, %s",
                           resourceEntry.getResourceName(), resourceEntry.getSearchString(), itemRef.toString());
    }
  }
}
