// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.check;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ProgressMonitor;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.NearInfinity;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.HexNumber;
import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.SortableTable;
import org.infinity.gui.TableItem;
import org.infinity.gui.ViewFrame;
import org.infinity.gui.WindowBlocker;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.cre.Item;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Debugging;
import org.infinity.util.Misc;

public final class CreInvChecker implements Runnable, ActionListener, ListSelectionListener
{
  private static final String FMT_PROGRESS = "Checking resource %d/%d";

  private ChildFrame resultFrame;
  private JButton bopen, bopennew, bsave;
  private SortableTable table;
  private ProgressMonitor progress;
  private int progressIndex;
  private List<ResourceEntry> creFiles;

  public CreInvChecker()
  {
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
      JFileChooser fc = new JFileChooser(Profile.getGameRoot().toFile());
      fc.setDialogTitle("Save search result");
      fc.setSelectedFile(new File(fc.getCurrentDirectory(), "result.txt"));
      if (fc.showSaveDialog(resultFrame) == JFileChooser.APPROVE_OPTION) {
        Path output = fc.getSelectedFile().toPath();
        if (Files.exists(output)) {
          String[] options = {"Overwrite", "Cancel"};
          if (JOptionPane.showOptionDialog(resultFrame, output + " exists. Overwrite?",
                                           "Save result", JOptionPane.YES_NO_OPTION,
                                           JOptionPane.WARNING_MESSAGE, null, options, options[0]) != 0)
            return;
        }
        try (BufferedWriter bw = Files.newBufferedWriter(output)) {
          bw.write("Result of CRE inventory check"); bw.newLine();
          bw.write("Number of hits: " + table.getRowCount()); bw.newLine();
          for (int i = 0; i < table.getRowCount(); i++) {
            bw.write(table.getTableItemAt(i).toString()); bw.newLine();
          }
          JOptionPane.showMessageDialog(resultFrame, "Result saved to " + output, "Save complete",
                                        JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
          JOptionPane.showMessageDialog(resultFrame, "Error while saving " + output,
                                        "Error", JOptionPane.ERROR_MESSAGE);
          e.printStackTrace();
        }
      }
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
    WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    blocker.setBlocked(true);
    try {
      ThreadPoolExecutor executor = Misc.createThreadPool();
      creFiles = ResourceFactory.getResources("CRE");
      creFiles.addAll(ResourceFactory.getResources("CHR"));
      progressIndex = 0;
      progress = new ProgressMonitor(NearInfinity.getInstance(), "Checking inventories...",
                                     String.format(FMT_PROGRESS, creFiles.size(), creFiles.size()),
                                     0, creFiles.size());
      progress.setNote(String.format(FMT_PROGRESS, 0, creFiles.size()));

      List<Class<? extends Object>> colClasses = new ArrayList<Class<? extends Object>>(3);
      colClasses.add(Object.class); colClasses.add(Object.class); colClasses.add(Object.class);
      table = new SortableTable(Arrays.asList(new String[]{"File", "Name", "Item"}),
                                colClasses, Arrays.asList(new Integer[]{100, 100, 200}));

      boolean isCancelled = false;
      Debugging.timerReset();
      for (int i = 0; i < creFiles.size(); i++) {
        Misc.isQueueReady(executor, true, -1);
        executor.execute(new Worker(creFiles.get(i)));
        if (progress.isCanceled()) {
          isCancelled = true;
          break;
        }
      }

      // enforcing thread termination if process has been cancelled
      if (isCancelled) {
        executor.shutdownNow();
      } else {
        executor.shutdown();
      }

      // waiting for pending threads to terminate
      while (!executor.isTerminated()) {
        if (!isCancelled && progress.isCanceled()) {
          executor.shutdownNow();
          isCancelled = true;
        }
        try { Thread.sleep(1); } catch (InterruptedException e) {}
      }

      if (isCancelled) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Operation cancelled",
                                      "Info", JOptionPane.INFORMATION_MESSAGE);
        return;
      }

      if (table.getRowCount() == 0)
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No hits found",
                                      "Info", JOptionPane.INFORMATION_MESSAGE);
      else {
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
      advanceProgress(true);
      blocker.setBlocked(false);
      if (creFiles != null) {
        creFiles.clear();
        creFiles = null;
      }
    }
    Debugging.timerShow("Check completed", Debugging.TimeFormat.MILLISECONDS);
  }

// --------------------- End Interface Runnable ---------------------

  private void checkCreature(CreResource cre)
  {
    final List<StructEntry> items = new ArrayList<StructEntry>();
    final List<StructEntry> slots = new ArrayList<StructEntry>();
    HexNumber slots_offset = (HexNumber)cre.getAttribute(CreResource.CRE_OFFSET_ITEM_SLOTS);
    for (int i = 0; i < cre.getFieldCount(); i++) {
      StructEntry entry = cre.getField(i);
      if (entry instanceof Item)
        items.add(entry);
      else if (entry.getOffset() >= slots_offset.getValue() + cre.getOffset() &&
               entry instanceof DecNumber
               && !entry.getName().equals(CreResource.CRE_SELECTED_WEAPON_SLOT)
               && !entry.getName().equals(CreResource.CRE_SELECTED_WEAPON_ABILITY))
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
        synchronized (table) {
          table.addTableItem(new CreInvError(cre.getResourceEntry(), item));
        }
      }
    }
  }

  private synchronized void advanceProgress(boolean finished)
  {
    if (progress != null) {
      if (finished) {
        progressIndex = 0;
        progress.close();
        progress = null;
      } else {
        progressIndex++;
        if (progressIndex % 100 == 0) {
          progress.setNote(String.format(FMT_PROGRESS, progressIndex, creFiles.size()));
        }
        progress.setProgress(progressIndex);
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
      return String.format("File: %1$s  Name: %2$s  %3$s",
                           resourceEntry.toString(), resourceEntry.getSearchString(), itemRef.toString());
    }
  }

  private class Worker implements Runnable
  {
    private final ResourceEntry entry;

    public Worker(ResourceEntry entry)
    {
      this.entry = entry;
    }

    @Override
    public void run()
    {
      if (entry != null) {
        try {
          checkCreature(new CreResource(entry));
        } catch (Exception e) {
          synchronized (System.err) {
            e.printStackTrace();
          }
        }
      }
      advanceProgress(false);
    }
  }
}

