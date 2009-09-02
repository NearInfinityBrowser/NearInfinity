// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.check;

import infinity.NearInfinity;
import infinity.gui.*;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.key.ResourceEntry;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public final class StructChecker extends ChildFrame implements ActionListener, Runnable,
                                                               ListSelectionListener
{
  private static final String filetypes[] = {"ARE", "CHR", "CHU", "CRE", "DLG", "GAM", "ITM",
                                             "SPL", "STO", "WED", "WMP"};
  private final ChildFrame resultFrame = new ChildFrame("Corrupted files found", true);
  private final JButton bstart = new JButton("Check", Icons.getIcon("Find16.gif"));
  private final JButton bcancel = new JButton("Cancel", Icons.getIcon("Delete16.gif"));
  private final JButton binvert = new JButton("Invert", Icons.getIcon("Refresh16.gif"));
  private final JButton bopen = new JButton("Open", Icons.getIcon("Open16.gif"));
  private final JButton bopennew = new JButton("Open in new window", Icons.getIcon("Open16.gif"));
  private final JButton bsave = new JButton("Save...", Icons.getIcon("Save16.gif"));
  private final JCheckBox[] boxes;
  private final List<ResourceEntry> files = new ArrayList<ResourceEntry>();
  private final SortableTable table;

  public StructChecker()
  {
    super("Find Corrupted Files");
    setIconImage(Icons.getIcon("Refresh16.gif").getImage());

    table = new SortableTable(new String[]{"File", "Offset", "Error message"},
                              new Class[]{Object.class, Object.class, Object.class},
                              new int[]{50, 50, 400});

    boxes = new JCheckBox[filetypes.length];
    bstart.setMnemonic('s');
    bcancel.setMnemonic('c');
    binvert.setMnemonic('i');
    bstart.addActionListener(this);
    bcancel.addActionListener(this);
    binvert.addActionListener(this);
    getRootPane().setDefaultButton(bstart);

    JPanel boxpanel = new JPanel(new GridLayout(0, 2, 3, 3));
    for (int i = 0; i < boxes.length; i++) {
      boxes[i] = new JCheckBox(filetypes[i], true);
      boxpanel.add(boxes[i]);
    }
    boxpanel.setBorder(BorderFactory.createEmptyBorder(3, 12, 3, 0));

    JPanel ipanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    ipanel.add(binvert);
    JPanel innerpanel = new JPanel(new BorderLayout());
    innerpanel.add(boxpanel, BorderLayout.CENTER);
    innerpanel.add(ipanel, BorderLayout.SOUTH);
    innerpanel.setBorder(BorderFactory.createTitledBorder("Select files to check:"));

    JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    bpanel.add(bstart);
    bpanel.add(bcancel);

    JPanel mainpanel = new JPanel(new BorderLayout());
    mainpanel.add(innerpanel, BorderLayout.CENTER);
    mainpanel.add(bpanel, BorderLayout.SOUTH);
    mainpanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

    JPanel pane = (JPanel)getContentPane();
    pane.setLayout(new BorderLayout());
    pane.add(mainpanel, BorderLayout.CENTER);

    pack();
    Center.center(this, NearInfinity.getInstance().getBounds());
    setVisible(true);
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bstart) {
      setVisible(false);
      for (int i = 0; i < filetypes.length; i++) {
        if (boxes[i].isSelected())
          files.addAll(ResourceFactory.getInstance().getResources(filetypes[i]));
      }
      if (files.size() > 0)
        new Thread(this).start();
    }
    else if (event.getSource() == binvert) {
      for (final JCheckBox box : boxes)
        box.setSelected(!box.isSelected());
    }
    else if (event.getSource() == bcancel)
      setVisible(false);
    else if (event.getSource() == bopen) {
      int row = table.getSelectedRow();
      if (row != -1) {
        ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
        NearInfinity.getInstance().showResourceEntry(resourceEntry);
//        NearInfinity.getInstance().setViewable(ResourceFactory.getResourceresourceEntry));
      }
    }
    else if (event.getSource() == bopennew) {
      int row = table.getSelectedRow();
      if (row != -1) {
        ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
        new ViewFrame(resultFrame, ResourceFactory.getResource(resourceEntry));
      }
    }
    else if (event.getSource() == bsave) {
      JFileChooser chooser = new JFileChooser(ResourceFactory.getRootDir());
      chooser.setDialogTitle("Save result");
      chooser.setSelectedFile(new File("result.txt"));
      if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        File output = chooser.getSelectedFile();
        if (output.exists()) {
          String options[] = {"Overwrite", "Cancel"};
          if (JOptionPane.showOptionDialog(this, output + " exists. Overwrite?",
                                           "Save result", JOptionPane.YES_NO_OPTION,
                                           JOptionPane.WARNING_MESSAGE, null, options, options[0]) == 1)
            return;
        }
        try {
          PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(output)));
          pw.println("File corruption search");
          pw.println("Number of errors: " + table.getRowCount());
          for (int i = 0; i < table.getRowCount(); i++)
            pw.println(table.getTableItemAt(i).toString());
          pw.close();
          JOptionPane.showMessageDialog(this, "Result saved to " + output, "Save complete",
                                        JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
          JOptionPane.showMessageDialog(this, "Error while saving " + output,
                                        "Error", JOptionPane.ERROR_MESSAGE);
          e.printStackTrace();
        }
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
    ProgressMonitor progress = new ProgressMonitor(NearInfinity.getInstance(), "Checking...", null, 0,
                                                   files.size());
    progress.setMillisToDecideToPopup(100);
    String type = null;
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < files.size(); i++) {
      ResourceEntry entry = files.get(i);
      Resource resource = ResourceFactory.getResource(entry);
      if (resource != null) {
        if (!entry.getExtension().equalsIgnoreCase(type)) {
          type = entry.getExtension();
          progress.setNote(type + 's');
        }
        search(entry, (AbstractStruct)resource);
      }
      progress.setProgress(i + 1);
      if (progress.isCanceled()) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Check canceled", "Info",
                                      JOptionPane.INFORMATION_MESSAGE);
        return;
      }
    }
    System.out.println("Check completed: " + (System.currentTimeMillis() - startTime) + "ms.");
    if (table.getRowCount() == 0)
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No errors found",
                                    "Info", JOptionPane.INFORMATION_MESSAGE);
    else {
      table.tableComplete();
      resultFrame.setIconImage(Icons.getIcon("Refresh16.gif").getImage());
      JLabel count = new JLabel(table.getRowCount() + " error(s) found", JLabel.CENTER);
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
      bsave.addActionListener(this);
      pane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      resultFrame.setSize(700, 600);
      Center.center(resultFrame, NearInfinity.getInstance().getBounds());
      resultFrame.setVisible(true);
    }
  }

// --------------------- End Interface Runnable ---------------------

  private void search(ResourceEntry entry, AbstractStruct struct)
  {
    List<StructEntry> flatList = struct.getFlatList();
    if (flatList.size() < 2)
      return;
    StructEntry entry1 = flatList.get(0);
    for (int i = 1; i < flatList.size(); i++) {
      StructEntry entry2 = flatList.get(i);
      if (!entry2.getName().equals("Unused bytes?")) {
        int delta = entry2.getOffset() - entry1.getOffset() - entry1.getSize();
        if (delta < 0)
          table.addTableItem(new Corruption(entry, entry1.getOffset(),
                                            entry1.getName() + '(' + Integer.toHexString(entry1.getOffset()) +
                                            "h)" +
                                            " overlaps " +
                                            entry2.getName() + '(' + Integer.toHexString(entry2.getOffset()) +
                                            "h)" +
                                            " by " + -delta + " bytes"));
        else if (delta > 0)
          table.addTableItem(new Corruption(entry, entry1.getOffset(),
                                            delta + " unused bytes between " +
                                            entry1.getName() + '(' + Integer.toHexString(entry1.getOffset()) +
                                            "h)" +
                                            " and " +
                                            entry2.getName() + '(' + Integer.toHexString(entry2.getOffset()) +
                                            "h)"));
        entry1 = entry2;
      }
    }
    StructEntry last = flatList.get(flatList.size() - 1);
    if (last.getName().equals("Unused bytes?"))
      table.addTableItem(new Corruption(entry, last.getOffset(),
                                        last.getSize() + " unused bytes after " +
                                        entry1.getName() + '(' + Integer.toHexString(entry1.getOffset()) +
                                        "h)"));
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class Corruption implements TableItem
  {
    private final ResourceEntry resourceEntry;
    private final String offset;
    private final String errorMsg;

    private Corruption(ResourceEntry resourceEntry, int offset, String errorMsg)
    {
      this.resourceEntry = resourceEntry;
      this.offset = Integer.toHexString(offset) + 'h';
      this.errorMsg = errorMsg;
    }

    public Object getObjectAt(int columnIndex)
    {
      if (columnIndex == 0)
        return resourceEntry;
      else if (columnIndex == 1)
        return offset;
      else
        return errorMsg;
    }

    public String toString()
    {
      StringBuffer buf = new StringBuffer("File: ");
      buf.append(resourceEntry.toString());
      buf.append("  Offset: ").append(offset);
      buf.append("  Error message: ").append(errorMsg);
      return buf.toString();
    }
  }
}