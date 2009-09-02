// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.sav;

import infinity.gui.ViewFrame;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.key.ResourceEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public final class SavResource implements Resource, ActionListener, Closeable, Writeable
{
  private static final JLabel lhelp = new JLabel("<html><b>Instructions:</b><ol>" +
                                                 "<li>Decompress the SAV file." +
                                                 "<li>View/edit the individual files." +
                                                 "<li>If any changes have been made, " +
                                                 "Compress to rebuild SAV file.</ol></html>");
  private final IOHandler handler;
  private final ResourceEntry entry;
  private DefaultListModel listModel;
  private JButton bcompress, bdecompress, bedit, bdelete, bexport;
  private JList filelist;
  private JPanel panel;
  private List entries;

  public SavResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    handler = new IOHandler(entry);
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bcompress) {
      try {
        handler.compress(entries);
        ResourceFactory.getInstance().saveResource(this, panel.getTopLevelAncestor());
        bdecompress.setEnabled(true);
        filelist.setEnabled(false);
        bedit.setEnabled(false);
        bdelete.setEnabled(false);
        bcompress.setEnabled(false);
      } catch (Exception e) {
        JOptionPane.showMessageDialog(panel, "Error compressing file", "Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
      }
    }
    else if (event.getSource() == bdecompress) {
      try {
        entries = handler.decompress();
        bcompress.setEnabled(true);
        filelist.setEnabled(true);
        bedit.setEnabled(true);
        bdelete.setEnabled(true);
        bdecompress.setEnabled(false);
        filelist.setSelectedIndex(0);
      } catch (Exception e) {
        JOptionPane.showMessageDialog(panel, "Error decompressing file", "Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
      }
    }
    else if (event.getSource() == bedit) {
      ResourceEntry fileentry = (ResourceEntry)entries.get(filelist.getSelectedIndex());
      Resource res = ResourceFactory.getResource(fileentry);
      new ViewFrame(panel.getTopLevelAncestor(), res);
    }
    else if (event.getSource() == bexport)
      ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
    else if (event.getSource() == bdelete) {
      int index = filelist.getSelectedIndex();
      ResourceEntry resourceentry = (ResourceEntry)entries.get(index);
      entries.remove(resourceentry);
      listModel.remove(index);
      if (index == listModel.size())
        index--;
      filelist.setSelectedIndex(index);
      filelist.revalidate();
      filelist.repaint();
      if (listModel.size() == 0) {
        bdelete.setEnabled(false);
        bedit.setEnabled(false);
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Closeable ---------------------

  public void close()
  {
    handler.close();
  }

// --------------------- End Interface Closeable ---------------------


// --------------------- Begin Interface Resource ---------------------

  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

// --------------------- End Interface Resource ---------------------


// --------------------- Begin Interface Viewable ---------------------

  public JComponent makeViewer(ViewableContainer container)
  {
    listModel = new DefaultListModel();
    for (int i = 0; i < handler.getFileEntries().size(); i++)
      listModel.addElement(handler.getFileEntries().get(i));
    filelist = new JList(listModel);
    filelist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    filelist.addMouseListener(new MouseAdapter()
    {
      public void mouseClicked(MouseEvent event)
      {
        if (event.getClickCount() == 2) {
          ResourceEntry fileentry = (ResourceEntry)entries.get(filelist.getSelectedIndex());
          Resource res = ResourceFactory.getResource(fileentry);
          new ViewFrame(panel.getTopLevelAncestor(), res);
        }
      }
    });
    bcompress = new JButton("Compress", Icons.getIcon("Import16.gif"));
    bdecompress = new JButton("Decompress", Icons.getIcon("Export16.gif"));
    bedit = new JButton("View/Edit", Icons.getIcon("Zoom16.gif"));
    bdelete = new JButton("Delete file", Icons.getIcon("Delete16.gif"));
    bcompress.setMnemonic('c');
    bdecompress.setMnemonic('d');
    bedit.setMnemonic('v');
    bcompress.addActionListener(this);
    bdecompress.addActionListener(this);
    bedit.addActionListener(this);
    bdelete.addActionListener(this);
    bcompress.setEnabled(false);
    bedit.setEnabled(false);
    bdelete.setEnabled(false);
    filelist.setEnabled(false);

    JPanel centerpanel = new JPanel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    centerpanel.setLayout(gbl);

    JLabel label = new JLabel("Contents of " + entry.toString());
    JScrollPane scroll = new JScrollPane(filelist);
    Dimension size = scroll.getPreferredSize();
    scroll.setPreferredSize(new Dimension(2 * (int)size.getWidth(), 2 * (int)size.getHeight()));

    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.insets = new Insets(6, 0, 0, 0);
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(label, gbc);
    centerpanel.add(label);

    gbc.insets.top = 3;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.VERTICAL;
    gbl.setConstraints(scroll, gbc);
    centerpanel.add(scroll);

    gbc.weighty = 0.0;
    gbl.setConstraints(lhelp, gbc);
    centerpanel.add(lhelp);

    bexport = new JButton("Export...", Icons.getIcon("Export16.gif"));
    bexport.setMnemonic('e');
    bexport.addActionListener(this);
    JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    bpanel.add(bdecompress);
    bpanel.add(bedit);
    bpanel.add(bdelete);
    bpanel.add(bcompress);
    bpanel.add(bexport);

    panel = new JPanel(new BorderLayout());
    panel.add(centerpanel, BorderLayout.CENTER);
    panel.add(bpanel, BorderLayout.SOUTH);
    centerpanel.setBorder(BorderFactory.createLoweredBevelBorder());

    return panel;
  }

// --------------------- End Interface Viewable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    handler.write(os);
  }

// --------------------- End Interface Writeable ---------------------

  public IOHandler getFileHandler()
  {
    return handler;
  }
}

