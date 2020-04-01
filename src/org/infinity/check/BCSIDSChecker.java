// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.check;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.NearInfinity;
import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.SortableTable;
import org.infinity.gui.TableItem;
import org.infinity.gui.ViewFrame;
import org.infinity.gui.WindowBlocker;
import org.infinity.icon.Icons;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.bcs.BcsResource;
import org.infinity.resource.bcs.Decompiler;
import org.infinity.resource.bcs.ScriptType;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.AbstractSearcher;
import org.infinity.util.Misc;

/** Performs checking {@link BcsResource BCS} & {@code BS} resources. */
public final class BCSIDSChecker extends AbstractSearcher implements Runnable, ActionListener, ListSelectionListener
{
  private ChildFrame resultFrame;
  private JButton bopen, bopennew, bsave;
  /** List of the {@link BCSIDSErrorTableLine} objects. */
  private SortableTable table;

  public BCSIDSChecker(Component parent)
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
        BcsResource bcsfile = (BcsResource)NearInfinity.getInstance().getViewable();
        bcsfile.highlightText(((Integer)table.getValueAt(row, 2)).intValue(), null);
      }
    }
    else if (event.getSource() == bopennew) {
      int row = table.getSelectedRow();
      if (row != -1) {
        ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
        Resource resource = ResourceFactory.getResource(resourceEntry);
        ViewFrame viewFrame = new ViewFrame(resultFrame, resource);
        BcsResource bcsfile = (BcsResource)viewFrame.getViewable();
        bcsfile.highlightText(((Integer)table.getValueAt(row, 2)).intValue(), null);
      }
    }
    else if (event.getSource() == bsave) {
      table.saveCheckResult(resultFrame, "Unknown IDS references in BCS & BS files");
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
      final List<ResourceEntry> bcsFiles = ResourceFactory.getResources("BCS");
      bcsFiles.addAll(ResourceFactory.getResources("BS"));

      table = new SortableTable(new String[]{"File", "Error message", "Line"},
                                new Class<?>[]{ResourceEntry.class, String.class, Integer.class},
                                new Integer[]{100, 300, 50});

      if (runSearch("Checking", bcsFiles)) {
        return;
      }

      if (table.getRowCount() == 0) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No unknown references found",
                                      "Info", JOptionPane.INFORMATION_MESSAGE);
      } else {
        table.tableComplete();
        resultFrame = new ChildFrame("Result", true);
        resultFrame.setIconImage(Icons.getIcon(Icons.ICON_REFRESH_16).getImage());
        bopen = new JButton("Open", Icons.getIcon(Icons.ICON_OPEN_16));
        bopennew = new JButton("Open in new window", Icons.getIcon(Icons.ICON_OPEN_16));
        bsave = new JButton("Save...", Icons.getIcon(Icons.ICON_SAVE_16));
        JLabel count = new JLabel(table.getRowCount() + " hits(s) found", JLabel.CENTER);
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
        checkScript(new BcsResource(entry));
      } catch (Exception e) {
        synchronized (System.err) {
          e.printStackTrace();
        }
      }
      advanceProgress();
    };
  }

  /**
   * Performs script checking. This method can be called from several threads
   *
   * @param script Script resource for check. Never {@code null}
   *
   * @throws Exception If {@code script} contains invalid code
   */
  private void checkScript(BcsResource script) throws Exception
  {
    final Decompiler decompiler = new Decompiler(script.getCode(), ScriptType.BCS, true);
    decompiler.setGenerateComments(false);
    decompiler.setGenerateResourcesUsed(true);
    decompiler.decompile();
    for (final Map.Entry<Integer, String> e : decompiler.getIdsErrors().entrySet()) {
      final Integer lineNr = e.getKey();
      final String error = e.getValue();
      if (!error.contains("GTIMES.IDS") &&
          !error.contains("SCROLL.IDS") &&
          !error.contains("SHOUTIDS.IDS") &&
          !error.contains("SPECIFIC.IDS") &&
          !error.contains("TIME.IDS")) {
        synchronized (table) {
          table.addTableItem(new BCSIDSErrorTableLine(script.getResourceEntry(), error, lineNr));
        }
      }
    }
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class BCSIDSErrorTableLine implements TableItem
  {
    private final ResourceEntry resourceEntry;
    private final String error;
    private final Integer lineNr;

    private BCSIDSErrorTableLine(ResourceEntry resourceEntry, String error, Integer lineNr)
    {
      this.resourceEntry = resourceEntry;
      this.error = error;
      this.lineNr = lineNr;
    }

    @Override
    public Object getObjectAt(int columnIndex)
    {
      if (columnIndex == 0)
        return resourceEntry;
      else if (columnIndex == 1)
        return error;
      return lineNr;
    }

    @Override
    public String toString()
    {
      return String.format("File: %s, Line: %d, Error: %s",
                           resourceEntry.getResourceName(), lineNr, error);
    }
  }
}
