// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.sav;

import infinity.NearInfinity;
import infinity.gui.ButtonPanel;
import infinity.gui.ViewFrame;
import infinity.gui.WindowBlocker;
import infinity.icon.Icons;
import infinity.resource.Closeable;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.ViewableContainer;
import infinity.resource.Writeable;
import infinity.resource.key.ResourceEntry;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

public final class SavResource implements Resource, ActionListener, Closeable, Writeable
{
  private static final JLabel lhelp = new JLabel("<html><b>Instructions:</b><ol>" +
                                                 "<li>Decompress the SAV file." +
                                                 "<li>View/edit the individual files." +
                                                 "<li>If any changes have been made, " +
                                                 "Compress to rebuild SAV file.</ol></html>");

  private static final ButtonPanel.Control CtrlCompress   = ButtonPanel.Control.Custom1;
  private static final ButtonPanel.Control CtrlDecompress = ButtonPanel.Control.Custom2;
  private static final ButtonPanel.Control CtrlEdit       = ButtonPanel.Control.Custom3;
  private static final ButtonPanel.Control CtrlDelete     = ButtonPanel.Control.Custom4;

  private final IOHandler handler;
  private final ResourceEntry entry;
  private final ButtonPanel buttonPanel = new ButtonPanel();

  private DefaultListModel listModel;
  private JList filelist;
  private JPanel panel;
  private List<? extends ResourceEntry> entries;

  public SavResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    handler = new IOHandler(entry);
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (buttonPanel.getControlByType(CtrlCompress) == event.getSource()) {
      compressData(true);
    } else if (buttonPanel.getControlByType(CtrlDecompress) == event.getSource()) {
      decompressData(true);
    } else if (buttonPanel.getControlByType(CtrlEdit) == event.getSource()) {
      ResourceEntry fileentry = (ResourceEntry)entries.get(filelist.getSelectedIndex());
      Resource res = ResourceFactory.getResource(fileentry);
      new ViewFrame(panel.getTopLevelAncestor(), res);
    } else if (buttonPanel.getControlByType(ButtonPanel.Control.ExportButton) == event.getSource()) {
      ResourceFactory.exportResource(entry, panel.getTopLevelAncestor());
    } else if (buttonPanel.getControlByType(CtrlDelete) == event.getSource()) {
      int index = filelist.getSelectedIndex();
      ResourceEntry resourceentry = (ResourceEntry)entries.get(index);
      entries.remove(resourceentry);
      listModel.remove(index);
      if (index == listModel.size()) {
        index--;
      }
      filelist.setSelectedIndex(index);
      filelist.revalidate();
      filelist.repaint();
      if (listModel.size() == 0) {
        buttonPanel.getControlByType(CtrlDelete).setEnabled(false);
        buttonPanel.getControlByType(CtrlEdit).setEnabled(false);
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Closeable ---------------------

  @Override
  public void close()
  {
    if (buttonPanel.getControlByType(CtrlCompress).isEnabled()) {
      final String msg = getResourceEntry().getResourceName() + " is still decompressed. Compress it?";
      if (JOptionPane.showConfirmDialog(panel.getTopLevelAncestor(), msg, "Question", JOptionPane.YES_NO_OPTION,
                                        JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
        compressData(true);
      }
    }
    handler.close();
  }

// --------------------- End Interface Closeable ---------------------


// --------------------- Begin Interface Resource ---------------------

  @Override
  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

// --------------------- End Interface Resource ---------------------


// --------------------- Begin Interface Viewable ---------------------

  @Override
  public JComponent makeViewer(ViewableContainer container)
  {
    listModel = new DefaultListModel();
    for (int i = 0; i < handler.getFileEntries().size(); i++)
      listModel.addElement(handler.getFileEntries().get(i));
    filelist = new JList(listModel);
    filelist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    filelist.addMouseListener(new MouseAdapter()
    {
      @Override
      public void mouseClicked(MouseEvent event)
      {
        if (event.getClickCount() == 2) {
          ResourceEntry fileentry = (ResourceEntry)entries.get(filelist.getSelectedIndex());
          Resource res = ResourceFactory.getResource(fileentry);
          new ViewFrame(panel.getTopLevelAncestor(), res);
        }
      }
    });

    JButton bDecompress = new JButton("Decompress", Icons.getIcon("Export16.gif"));
    bDecompress.setMnemonic('d');
    bDecompress.addActionListener(this);

    JButton bEdit = new JButton("View/Edit", Icons.getIcon("Zoom16.gif"));
    bEdit.setMnemonic('v');
    bEdit.addActionListener(this);
    bEdit.setEnabled(false);

    JButton bDelete = new JButton("Delete file", Icons.getIcon("Delete16.gif"));
    bDelete.addActionListener(this);
    bDelete.setEnabled(false);

    JButton bCompress = new JButton("Compress", Icons.getIcon("Import16.gif"));
    bCompress.setMnemonic('c');
    bCompress.addActionListener(this);
    bCompress.setEnabled(false);

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

    buttonPanel.addControl(bDecompress, CtrlDecompress);
    buttonPanel.addControl(bEdit, CtrlEdit);
    buttonPanel.addControl(bDelete, CtrlDelete);
    buttonPanel.addControl(bCompress, CtrlCompress);
    ((JButton)buttonPanel.addControl(ButtonPanel.Control.ExportButton)).addActionListener(this);

    panel = new JPanel(new BorderLayout());
    panel.add(centerpanel, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.SOUTH);
    centerpanel.setBorder(BorderFactory.createLoweredBevelBorder());

    return panel;
  }

// --------------------- End Interface Viewable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    handler.write(os);
  }

// --------------------- End Interface Writeable ---------------------

  public IOHandler getFileHandler()
  {
    return handler;
  }

  private boolean compressData(boolean showError)
  {
    try {
      WindowBlocker block = new WindowBlocker(NearInfinity.getInstance());
      try {
        block.setBlocked(true);
        handler.compress(entries);
        ResourceFactory.saveResource(this, panel.getTopLevelAncestor());
        buttonPanel.getControlByType(CtrlDecompress).setEnabled(true);
        filelist.setEnabled(false);
        buttonPanel.getControlByType(CtrlEdit).setEnabled(false);
        buttonPanel.getControlByType(CtrlDelete).setEnabled(false);
        buttonPanel.getControlByType(CtrlCompress).setEnabled(false);
      } finally {
        block.setBlocked(false);
        block = null;
      }
    } catch (Exception e) {
      if (showError) {
        JOptionPane.showMessageDialog(panel, "Error compressing file", "Error", JOptionPane.ERROR_MESSAGE);
      }
      e.printStackTrace();
      return false;
    }
    return true;
  }

  private boolean decompressData(boolean showError)
  {
    try {
      WindowBlocker block = new WindowBlocker(NearInfinity.getInstance());
      try {
        block.setBlocked(true);
        entries = handler.decompress();
        buttonPanel.getControlByType(CtrlCompress).setEnabled(true);
        filelist.setEnabled(true);
        buttonPanel.getControlByType(CtrlEdit).setEnabled(true);
        buttonPanel.getControlByType(CtrlDelete).setEnabled(true);
        buttonPanel.getControlByType(CtrlDecompress).setEnabled(false);
        filelist.setSelectedIndex(0);
      } finally {
        block.setBlocked(false);
        block = null;
      }
    } catch (Exception e) {
      if (showError) {
        JOptionPane.showMessageDialog(panel, "Error decompressing file", "Error", JOptionPane.ERROR_MESSAGE);
      }
      e.printStackTrace();
      return false;
    }
    return true;
  }
}

