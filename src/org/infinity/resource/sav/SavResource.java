// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sav;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.infinity.NearInfinity;
import org.infinity.gui.ButtonPanel;
import org.infinity.gui.ButtonPopupMenu;
import org.infinity.gui.ResourceChooser;
import org.infinity.gui.ViewFrame;
import org.infinity.gui.WindowBlocker;
import org.infinity.icon.Icons;
import org.infinity.resource.Closeable;
import org.infinity.resource.Profile;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.ViewableContainer;
import org.infinity.resource.Writeable;
import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.SimpleListModel;
import org.infinity.util.io.FileEx;

/**
 * This resource acts as a standalone compressed archive. The file is zlib compressed,
 * and allows incremental updates to be carried out quickly and easily.
 *
 * @see <a href="https://gibberlings3.github.io/iesdp/file_formats/ie_formats/sav_v1.htm">
 * https://gibberlings3.github.io/iesdp/file_formats/ie_formats/sav_v1.htm</a>
 */
public final class SavResource implements Resource, Closeable, Writeable,
                                          ActionListener, ListSelectionListener
{
  private static final JLabel lhelp = new JLabel("<html><b>Instructions:</b><ol>" +
                                                 "<li>Decompress the SAV file." +
                                                 "<li>View/edit the individual files." +
                                                 "<li>If any changes have been made, " +
                                                 "Compress to rebuild SAV file.</ol></html>");

  private static final ButtonPanel.Control CtrlCompress   = ButtonPanel.Control.CUSTOM_1;
  private static final ButtonPanel.Control CtrlDecompress = ButtonPanel.Control.CUSTOM_2;
  private static final ButtonPanel.Control CtrlEdit       = ButtonPanel.Control.CUSTOM_3;
  private static final ButtonPanel.Control CtrlDelete     = ButtonPanel.Control.CUSTOM_4;
  private static final ButtonPanel.Control CtrlAdd        = ButtonPanel.Control.ADD;

  private final IOHandler handler;
  private final ResourceEntry entry;
  private final ButtonPanel buttonPanel = new ButtonPanel();

  private SimpleListModel<ResourceEntry> listModel;
  private JList<ResourceEntry> filelist;
  private JPanel panel;
  private List<ResourceEntry> entries;
  private JMenuItem miAddExternal;
  private JMenuItem miAddInternal;

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
      ResourceEntry fileentry = entries.get(filelist.getSelectedIndex());
      Resource res = ResourceFactory.getResource(fileentry);
      new ViewFrame(panel.getTopLevelAncestor(), res);
    } else if (buttonPanel.getControlByType(ButtonPanel.Control.EXPORT_BUTTON) == event.getSource()) {
      ResourceFactory.exportResource(entry, panel.getTopLevelAncestor());
    } else if (buttonPanel.getControlByType(CtrlDelete) == event.getSource()) {
      if (!filelist.isSelectionEmpty()) {
        String fileName = filelist.getSelectedValue().toString();
        int ret = JOptionPane.showConfirmDialog(panel.getTopLevelAncestor(),
                                                "Delete file " + fileName + "?",
                                                "Delete file", JOptionPane.YES_NO_OPTION,
                                                JOptionPane.QUESTION_MESSAGE);
        if (ret == JOptionPane.YES_OPTION) {
          removeResource(filelist.getSelectedIndex());
        }
      }
    } else if (miAddExternal == event.getSource()) {
      JFileChooser fc = new JFileChooser(Profile.getGameRoot().toFile());
      fc.setDialogTitle("Open external file");
      fc.setDialogType(JFileChooser.OPEN_DIALOG);
      fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
      fc.setMultiSelectionEnabled(false);
      FileNameExtensionFilter filter =
          new FileNameExtensionFilter("Supported file types", Profile.getAvailableResourceTypes());
      fc.addChoosableFileFilter(filter);
      fc.setFileFilter(filter);
      if (fc.showOpenDialog(panel.getTopLevelAncestor()) == JFileChooser.APPROVE_OPTION) {
        addResource(new FileResourceEntry(fc.getSelectedFile().toPath()));
      }
    } else if (miAddInternal == event.getSource()) {
      ResourceChooser rc = new ResourceChooser();
      if (rc.showDialog(panel.getTopLevelAncestor()) == ResourceChooser.APPROVE_OPTION) {
        addResource(rc.getSelectedItem());
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------

// --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent e)
  {
    if (filelist.isEnabled()) {
      buttonPanel.getControlByType(CtrlDelete).setEnabled(!filelist.isSelectionEmpty());
      buttonPanel.getControlByType(CtrlEdit).setEnabled(!filelist.isSelectionEmpty());
    }
  }

// --------------------- End Interface ListSelectionListener ---------------------

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
    listModel = new SimpleListModel<>();
    for (int i = 0; i < handler.getFileEntries().size(); i++) {
      listModel.addElement(handler.getFileEntries().get(i));
    }
    filelist = new JList<>(listModel);
    filelist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    filelist.addListSelectionListener(this);
    filelist.addMouseListener(new MouseAdapter()
    {
      @Override
      public void mouseClicked(MouseEvent event)
      {
        if (event.getClickCount() == 2) {
          ResourceEntry fileentry = entries.get(filelist.getSelectedIndex());
          Resource res = ResourceFactory.getResource(fileentry);
          new ViewFrame(panel.getTopLevelAncestor(), res);
        }
      }
    });

    JButton bDecompress = new JButton("Decompress", Icons.getIcon(Icons.ICON_EXPORT_16));
    bDecompress.setMnemonic('d');
    bDecompress.addActionListener(this);

    JButton bEdit = new JButton("View/Edit", Icons.getIcon(Icons.ICON_ZOOM_16));
    bEdit.setMnemonic('v');
    bEdit.addActionListener(this);
    bEdit.setEnabled(false);

    JButton bDelete = new JButton("Delete file", Icons.getIcon(Icons.ICON_DELETE_16));
    bDelete.addActionListener(this);
    bDelete.setEnabled(false);

    miAddExternal = new JMenuItem("External file");
    miAddExternal.addActionListener(this);
    miAddInternal = new JMenuItem("Game resource");
    miAddInternal.addActionListener(this);
    ButtonPopupMenu bpmAdd = new ButtonPopupMenu("Add...", new JMenuItem[]{miAddExternal, miAddInternal});
    bpmAdd.setIcon(Icons.getIcon(Icons.ICON_ADD_16));
    bpmAdd.setEnabled(false);

    JButton bCompress = new JButton("Compress", Icons.getIcon(Icons.ICON_IMPORT_16));
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
    buttonPanel.addControl(bpmAdd, CtrlAdd);
    buttonPanel.addControl(bDelete, CtrlDelete);
    buttonPanel.addControl(bCompress, CtrlCompress);
    ((JButton)buttonPanel.addControl(ButtonPanel.Control.EXPORT_BUTTON)).addActionListener(this);

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
        buttonPanel.getControlByType(CtrlAdd).setEnabled(false);
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
        buttonPanel.getControlByType(CtrlAdd).setEnabled(true);
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

  private void addResource(String resourceName)
  {
    addResource(ResourceFactory.getResourceEntry(resourceName));
  }

  private void addResource(ResourceEntry resourceEntry)
  {
    if (resourceEntry != null) {
      Path output = handler.getTempFolder().resolve(resourceEntry.getResourceName());
      try {
        if (FileEx.create(output).exists()) {
          String msg = "File " + resourceEntry.getResourceName() + " already exists. Overwrite?";
          int ret = JOptionPane.showConfirmDialog(panel.getTopLevelAncestor(),
                                                  msg, "Overwrite file?", JOptionPane.YES_NO_OPTION,
                                                  JOptionPane.QUESTION_MESSAGE);
          if (ret != JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(panel.getTopLevelAncestor(), "Operation cancelled.");
            return;
          }
        }
        ResourceFactory.exportResource(resourceEntry, output);

        ResourceEntry newEntry = new FileResourceEntry(output);
        int idx = 0;
        for (int count = entries.size(); idx < count; idx++) {
          if (newEntry.compareTo(entries.get(idx)) == 0) {
            filelist.setSelectedIndex(idx);
            idx = -1;
            break;
          } else if (newEntry.compareTo(entries.get(idx)) < 0) {
            break;
          }
        }

        if (idx >= 0) {
          entries.add(idx, newEntry);
          listModel.add(idx, newEntry);
          filelist.setSelectedIndex(idx);
          filelist.revalidate();
          filelist.repaint();
          filelist.ensureIndexIsVisible(idx);
        }

        buttonPanel.getControlByType(CtrlDelete).setEnabled(filelist.getSelectedIndex() >= 0);
        buttonPanel.getControlByType(CtrlEdit).setEnabled(filelist.getSelectedIndex() >= 0);
      } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(panel.getTopLevelAncestor(),
                                      e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private void removeResource(int entryIndex)
  {
    if (entryIndex >= 0 && entryIndex < entries.size()) {
      ResourceEntry resourceEntry = entries.get(entryIndex);
      Path file = resourceEntry.getActualPath();
      if (FileEx.create(file).exists()) {
        try {
          Files.delete(file);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      entries.remove(resourceEntry);
      listModel.remove(entryIndex);
      if (entryIndex == listModel.size()) {
        entryIndex--;
      }
      filelist.setSelectedIndex(entryIndex);
      filelist.revalidate();
      filelist.repaint();
      if (listModel.size() == 0) {
        buttonPanel.getControlByType(CtrlDelete).setEnabled(false);
        buttonPanel.getControlByType(CtrlEdit).setEnabled(false);
      }
    } else {
      JOptionPane.showMessageDialog(panel.getTopLevelAncestor(),
                                    "Error removing selected resource from the list.",
                                    "Error", JOptionPane.ERROR_MESSAGE);
    }
  }
}
