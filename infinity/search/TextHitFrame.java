// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.search;

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

final class TextHitFrame extends ChildFrame implements ActionListener, ListSelectionListener
{
  private final Component parent;
  private final JButton bopen = new JButton("Open", Icons.getIcon("Open16.gif"));
  private final JButton bopennew = new JButton("Open in new window", Icons.getIcon("Open16.gif"));
  private final JButton bsave = new JButton("Save...", Icons.getIcon("Save16.gif"));
  private final JLabel count;
  private final SortableTable table;
  private final String query;

  TextHitFrame(final String query, Component parent)
  {
    super("Search Result", true);
    this.query = query;
    this.parent = parent;
    setIconImage(Icons.getIcon("History16.gif").getImage());
    table = new SortableTable(new String[]{"File", "Text", "Line"},
                              new Class[]{Object.class, Object.class, Integer.class},
                              new int[]{100, 300, 50});
    bopen.setMnemonic('o');
    bopennew.setMnemonic('n');
    bsave.setMnemonic('s');
    getRootPane().setDefaultButton(bopennew);
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    panel.add(bopen);
    panel.add(bopennew);
    panel.add(bsave);
    count = new JLabel(table.getRowCount() + " hits found", JLabel.CENTER);
    count.setFont(count.getFont().deriveFont((float)count.getFont().getSize() + 2.0f));
    JPanel pane = (JPanel)getContentPane();
    pane.setLayout(new BorderLayout(0, 3));
    JScrollPane scrollTable = new JScrollPane(table);
    scrollTable.getViewport().setBackground(table.getBackground());
    pane.add(count, BorderLayout.NORTH);
    pane.add(scrollTable, BorderLayout.CENTER);
    pane.add(panel, BorderLayout.SOUTH);
    pane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    bopen.setEnabled(false);
    bopennew.setEnabled(false);
    table.setFont(BrowserMenuBar.getInstance().getScriptFont());
    table.getSelectionModel().addListSelectionListener(this);
    final ChildFrame frame = this;
    table.addMouseListener(new MouseAdapter()
    {
      public void mouseReleased(MouseEvent event)
      {
        if (event.getClickCount() == 2) {
          int row = table.getSelectedRow();
          if (row != -1) {
            Resource res = ResourceFactory.getResource((ResourceEntry)table.getValueAt(row, 0));
            new ViewFrame(frame, res);
            if (res instanceof TextResource)
              ((TextResource)res).highlightText(((Integer)table.getValueAt(row, 2)).intValue(), query);
          }
        }
      }
    });
    bopen.addActionListener(this);
    bopennew.addActionListener(this);
    bsave.addActionListener(this);
    pack();
    Center.center(this, parent.getBounds());
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bopen) {
      int row = table.getSelectedRow();
      if (row != -1) {
        ResourceEntry entry = (ResourceEntry)table.getValueAt(row, 0);
        if (parent instanceof ViewFrame && parent.isVisible()) {
          Resource res = ResourceFactory.getResource(entry);
          ((ViewFrame)parent).setViewable(res);
          if (res instanceof TextResource)
            ((TextResource)res).highlightText(((Integer)table.getValueAt(row, 2)).intValue(), query);
        }
        else {
          NearInfinity.getInstance().showResourceEntry(entry);
          Viewable viewable = NearInfinity.getInstance().getViewable();
          if (viewable instanceof TextResource)
            ((TextResource)viewable).highlightText(((Integer)table.getValueAt(row, 2)).intValue(), query);
        }
      }
    }
    else if (event.getSource() == bopennew) {
      int row = table.getSelectedRow();
      if (row != -1) {
        Resource res = ResourceFactory.getResource((ResourceEntry)table.getValueAt(row, 0));
        new ViewFrame(this, res);
        if (res instanceof TextResource)
          ((TextResource)res).highlightText(((Integer)table.getValueAt(row, 2)).intValue(), query);
      }
    }
    else if (event.getSource() == bsave) {
      JFileChooser chooser = new JFileChooser(ResourceFactory.getRootDir());
      chooser.setDialogTitle("Save search result");
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
          pw.println("Searched for: " + query);
          pw.println("Number of hits: " + table.getRowCount());
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

  public void setVisible(boolean b)
  {
    table.tableComplete();
    count.setText(table.getRowCount() + " hit(s) found");
    if (b && table.getRowCount() == 0)
      JOptionPane.showMessageDialog(parent, "No hits found", "Info", JOptionPane.INFORMATION_MESSAGE);
    else
      super.setVisible(b);
  }

  public void addHit(ResourceEntry entry, String line, int lineNr)
  {
    table.addTableItem(new TextHit(entry, line.trim(), lineNr));
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class TextHit implements TableItem
  {
    private final ResourceEntry entry;
    private final String line;
    private final Integer linenr;

    private TextHit(ResourceEntry entry, String name, int linenr)
    {
      this.entry = entry;
      line = name;
      this.linenr = new Integer(linenr);
    }

    public Object getObjectAt(int columnIndex)
    {
      if (columnIndex == 0)
        return entry;
      else if (columnIndex == 1)
        return line;
      return linenr;
    }

    public String toString()
    {
      StringBuffer buf = new StringBuffer("File: ");
      buf.append(entry.toString()).append("  Text: ").append(line).append("  Line: ").append(linenr);
      return buf.toString();
    }
  }
}

