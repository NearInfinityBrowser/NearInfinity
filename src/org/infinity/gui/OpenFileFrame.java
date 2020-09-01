// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.infinity.NearInfinity;
import org.infinity.icon.Icons;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.io.FileEx;
import org.infinity.util.io.FileManager;

public final class OpenFileFrame extends ChildFrame implements ActionListener
{
  private static final JFileChooser fc = new JFileChooser(".");
  private final JButton bExternalBrowse = new JButton("Browse...");
  private final JButton bOpen = new JButton("Open", Icons.getIcon(Icons.ICON_OPEN_16));
  private final JButton bOpenNew = new JButton("Open in new window", Icons.getIcon(Icons.ICON_OPEN_16));
  private final JCheckBox cbStayOpen = new JCheckBox("Keep this dialog open");
  private final JLabel lExternalDrop = new JLabel("or drop file(s) here", JLabel.CENTER);
  private final JRadioButton rbExternal = new JRadioButton("Open external file");
  private final JRadioButton rbInternal = new JRadioButton("Open internal file");
  private final JTextField tfExternalName = new JTextField(20);
  private final TextListPanel<ResourceEntry> lpInternal;

  OpenFileFrame()
  {
    super("Open File");
    setIconImage(Icons.getIcon(Icons.ICON_OPEN_16).getImage());
    rbExternal.addActionListener(this);
    rbInternal.addActionListener(this);
    rbExternal.setMnemonic('e');
    rbInternal.setMnemonic('i');
    cbStayOpen.setMnemonic('k');
    ButtonGroup gb = new ButtonGroup();
    gb.add(rbExternal);
    gb.add(rbInternal);
    fc.setDialogTitle("Open external file");
    tfExternalName.addActionListener(this);
    bExternalBrowse.setMnemonic('b');
    bExternalBrowse.addActionListener(this);
    tfExternalName.setMinimumSize(
            new Dimension(tfExternalName.getMinimumSize().width, bExternalBrowse.getMinimumSize().height));
    tfExternalName.getDocument().addDocumentListener(new DocumentListener()
    {
      @Override
      public void insertUpdate(DocumentEvent e)
      {
        bOpenNew.setEnabled(rbInternal.isSelected() || tfExternalName.getText().length() > 0);
      }

      @Override
      public void removeUpdate(DocumentEvent e)
      {
        bOpenNew.setEnabled(rbInternal.isSelected() || tfExternalName.getText().length() > 0);
      }

      @Override
      public void changedUpdate(DocumentEvent e)
      {
        bOpenNew.setEnabled(rbInternal.isSelected() || tfExternalName.getText().length() > 0);
      }
    });
    lpInternal = new TextListPanel<>(new ArrayList<>(ResourceFactory.getResourceTreeModel().getResourceEntries()));
    bOpen.addActionListener(this);
    bOpenNew.addActionListener(this);
    bOpenNew.setEnabled(false);
    bOpen.setMnemonic('o');
    bOpenNew.setMnemonic('n');
    lExternalDrop.setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlDkShadow")));
    new DropTarget(lExternalDrop, new MyDropTargetListener());
    rbExternal.setSelected(true);
    bOpen.setEnabled(false);
    getRootPane().setDefaultButton(bOpenNew);
    lpInternal.setEnabled(false);
    lpInternal.addMouseListener(new MouseAdapter()
    {
      @Override
      public void mouseClicked(MouseEvent event)
      {
        if (event.getClickCount() == 2) {
          actionPerformed(new ActionEvent(lpInternal, 0, "View"));
        }
      }
    });

    JPanel pane = (JPanel)getContentPane();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    pane.setLayout(gbl);

    gbc.weightx = 1.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(3, 3, 3, 6);
    gbl.setConstraints(rbExternal, gbc);
    pane.add(rbExternal);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.gridwidth = 1;
    gbc.insets = new Insets(0, 12, 3, 0);
    gbl.setConstraints(tfExternalName, gbc);
    pane.add(tfExternalName);

    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.insets = new Insets(0, 3, 3, 6);
    gbl.setConstraints(bExternalBrowse, gbc);
    pane.add(bExternalBrowse);

    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.insets = new Insets(0, 12, 3, 6);
    gbl.setConstraints(lExternalDrop, gbc);
    pane.add(lExternalDrop);

    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(9, 3, 3, 6);
    gbl.setConstraints(rbInternal, gbc);
    pane.add(rbInternal);

    gbc.weighty = 3.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets = new Insets(0, 12, 3, 6);
    gbl.setConstraints(lpInternal, gbc);
    pane.add(lpInternal);

    JPanel bPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    bPanel.add(bOpen);
    bPanel.add(bOpenNew);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weighty = 0.0;
    gbc.insets = new Insets(3, 6, 0, 6);

    gbl.setConstraints(bPanel, gbc);
    pane.add(bPanel);

    gbc.insets.top = 0;
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.NONE;
    gbl.setConstraints(cbStayOpen, gbc);
    pane.add(cbStayOpen);

    setSize(330, 400);
    Center.center(this, NearInfinity.getInstance().getBounds());
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == rbExternal) {
      bOpen.setEnabled(false);
      bOpenNew.setEnabled(tfExternalName.getText().length() > 0);
      lpInternal.setEnabled(false);
      tfExternalName.setEnabled(true);
      bExternalBrowse.setEnabled(true);
    }
    else if (event.getSource() == rbInternal) {
      bOpen.setEnabled(true);
      bOpenNew.setEnabled(true);
      lpInternal.setEnabled(true);
      tfExternalName.setEnabled(false);
      bExternalBrowse.setEnabled(false);
    }
    else if (event.getSource() == tfExternalName) {
      openExternalFile(this, FileManager.resolve(tfExternalName.getText()));
    }
    else if (event.getSource() == bExternalBrowse) {
      if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        tfExternalName.setText(fc.getSelectedFile().toString());
    }
    else if (event.getSource() == bOpen || event.getSource() == lpInternal) {
      if (!cbStayOpen.isSelected())
        setVisible(false);
      final ResourceEntry entry = lpInternal.getSelectedValue();
      NearInfinity.getInstance().showResourceEntry(entry);
    }
    else if (event.getSource() == bOpenNew) {
      if (!cbStayOpen.isSelected()) {
        setVisible(false);
      }
      if (rbInternal.isSelected()) {
        final ResourceEntry entry = lpInternal.getSelectedValue();
        if (entry != null) {
          new ViewFrame(this, ResourceFactory.getResource(entry));
        }
      } else {
        openExternalFile(this, FileManager.resolve(tfExternalName.getText()));
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------

  /** Attempts to open the specified external game resource. */
  public static void openExternalFile(Component parent, Path file)
  {
    if (!FileEx.create(file).exists()) {
      JOptionPane.showMessageDialog(parent, '\"' + file.toString() + "\" not found",
                                    "Error", JOptionPane.ERROR_MESSAGE);
    } else {
      new ViewFrame(parent, ResourceFactory.getResource(new FileResourceEntry(file)));
    }
  }

// -------------------------- INNER CLASSES --------------------------

  private final class MyDropTargetListener implements DropTargetListener, Runnable
  {
    private List<File> files;

    private MyDropTargetListener()
    {
    }

    @Override
    public void dragEnter(DropTargetDragEvent event)
    {
    }

    @Override
    public void dragOver(DropTargetDragEvent event)
    {
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent event)
    {
    }

    @Override
    public void dragExit(DropTargetEvent event)
    {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void drop(DropTargetDropEvent event)
    {
      if (event.isLocalTransfer() || !event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        event.rejectDrop();
        return;
      }
      try {
        event.acceptDrop(DnDConstants.ACTION_COPY);
        files = (List<File>)event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
      } catch (Exception e) {
        e.printStackTrace();
        event.dropComplete(false);
        return;
      }
      event.dropComplete(true);
      new Thread(this).start();
    }

    @Override
    public void run()
    {
      if (files != null) {
        for (final File file: files) {
          if (file != null && !file.isDirectory()) {
            openExternalFile(OpenFileFrame.this, file.toPath());
          }
        }
      }
    }
  }
}

