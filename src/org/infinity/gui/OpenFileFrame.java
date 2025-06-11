// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
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
import java.io.IOException;
import java.nio.file.Files;
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
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.infinity.NearInfinity;
import org.infinity.icon.Icons;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Logger;
import org.infinity.util.io.FileEx;
import org.infinity.util.io.FileManager;

public final class OpenFileFrame extends ChildFrame implements ActionListener {
  private static final JFileChooser FC = new JFileChooser(".");

  private final JButton bExternalBrowse = new JButton("Browse...");
  private final JButton bOpen = new JButton("Open", Icons.ICON_OPEN_16.getIcon());
  private final JButton bOpenNew = new JButton("Open in new window", Icons.ICON_OPEN_16.getIcon());
  private final JCheckBox cbStayOpen = new JCheckBox("Keep this dialog open");
  private final JCheckBox cbAlwaysOnTop = new JCheckBox("Keep dialog always on top");
  private final JLabel lExternalDrop = new JLabel("or drop file(s) here", SwingConstants.CENTER);
  private final JRadioButton rbExternal = new JRadioButton("Open external file");
  private final JRadioButton rbInternal = new JRadioButton("Open internal file");
  private final JTextField tfExternalName = new JTextField(20);
  private final TextListPanel<ResourceEntry> lpInternal;

  public OpenFileFrame() {
    super("Open File");
    setIconImage(Icons.ICON_OPEN_16.getIcon().getImage());
    rbExternal.addActionListener(this);
    rbInternal.addActionListener(this);
    rbExternal.setMnemonic('e');
    rbInternal.setMnemonic('i');
    cbStayOpen.setMnemonic('k');
    cbAlwaysOnTop.setMnemonic('t');
    cbAlwaysOnTop.addActionListener(this);
    ButtonGroup gb = new ButtonGroup();
    gb.add(rbExternal);
    gb.add(rbInternal);
    FC.setDialogTitle("Open external file");
    tfExternalName.addActionListener(this);
    bExternalBrowse.setMnemonic('b');
    bExternalBrowse.addActionListener(this);
    tfExternalName
        .setMinimumSize(new Dimension(tfExternalName.getMinimumSize().width, bExternalBrowse.getMinimumSize().height));
    tfExternalName.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        bOpenNew.setEnabled(rbInternal.isSelected() || !tfExternalName.getText().isEmpty());
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        bOpenNew.setEnabled(rbInternal.isSelected() || !tfExternalName.getText().isEmpty());
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        bOpenNew.setEnabled(rbInternal.isSelected() || !tfExternalName.getText().isEmpty());
      }
    });
    lpInternal = new TextListPanel<>(new ArrayList<>(ResourceFactory.getResourceTreeModel().getResourceEntries()));
    bOpen.addActionListener(this);
    bOpenNew.addActionListener(this);
    bOpenNew.setEnabled(false);
    bOpen.setMnemonic('o');
    bOpenNew.setMnemonic('n');
    final Dimension dim = lExternalDrop.getPreferredSize();
    lExternalDrop.setPreferredSize(new Dimension(dim.width, dim.height * 4));
    lExternalDrop.setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlDkShadow")));
    new DropTarget(lExternalDrop, new MyDropTargetListener());
    rbExternal.setSelected(true);
    bOpen.setEnabled(false);
    getRootPane().setDefaultButton(bOpenNew);
    lpInternal.setEnabled(false);
    lpInternal.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
          actionPerformed(new ActionEvent(lpInternal, 0, "View"));
        }
      }
    });

    final Container pane = getContentPane();
    pane.setLayout(new GridBagLayout());

    final JPanel mainPanel = new JPanel(new GridBagLayout());
    final GridBagConstraints gbc = new GridBagConstraints();

    final JPanel externalFilePanel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0);
    externalFilePanel.add(tfExternalName, gbc);
    ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 8, 0, 0), 0, 0);
    externalFilePanel.add(bExternalBrowse, gbc);

    final JPanel buttonPanel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_END, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 4), 0, 0);
    buttonPanel.add(bOpen, gbc);
    ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 4, 0, 0), 0, 0);
    buttonPanel.add(bOpenNew, gbc);

    final JPanel optionsPanel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    optionsPanel.add(cbStayOpen, gbc);
    ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 8, 0, 0), 0, 0);
    optionsPanel.add(cbAlwaysOnTop, gbc);
    ViewerUtil.setGBC(gbc, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0);
    optionsPanel.add(new JPanel(), gbc);

    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0);
    mainPanel.add(rbExternal, gbc);
    ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(8, 0, 0, 0), 0, 0);
    mainPanel.add(externalFilePanel, gbc);
    ViewerUtil.setGBC(gbc, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(8, 0, 0, 0), 0, 0);
    mainPanel.add(lExternalDrop, gbc);
    ViewerUtil.setGBC(gbc, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(16, 0, 0, 0), 0, 0);
    mainPanel.add(rbInternal, gbc);
    ViewerUtil.setGBC(gbc, 0, 4, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
        new Insets(8, 0, 0, 0), 0, 0);
    mainPanel.add(lpInternal, gbc);
    ViewerUtil.setGBC(gbc, 0, 5, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(8, 0, 0, 0), 0, 0);
    mainPanel.add(buttonPanel, gbc);
    ViewerUtil.setGBC(gbc, 0, 6, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(8, 0, 0, 0), 0, 0);
    mainPanel.add(optionsPanel, gbc);

    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
        new Insets(8, 8, 8, 8), 0, 0);
    pane.add(mainPanel, gbc);

    pack();
    setMinimumSize(getSize());
    Center.center(this, NearInfinity.getInstance().getBounds());
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == rbExternal) {
      bOpen.setEnabled(false);
      bOpenNew.setEnabled(!tfExternalName.getText().isEmpty());
      lpInternal.setEnabled(false);
      tfExternalName.setEnabled(true);
      bExternalBrowse.setEnabled(true);
    } else if (event.getSource() == rbInternal) {
      bOpen.setEnabled(true);
      bOpenNew.setEnabled(true);
      lpInternal.setEnabled(true);
      tfExternalName.setEnabled(false);
      bExternalBrowse.setEnabled(false);
    } else if (event.getSource() == tfExternalName) {
      openExternalFile(this, FileManager.resolve(tfExternalName.getText()));
    } else if (event.getSource() == bExternalBrowse) {
      if (FC.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        tfExternalName.setText(FC.getSelectedFile().toString());
      }
    } else if (event.getSource() == bOpen || event.getSource() == lpInternal) {
      if (!cbStayOpen.isSelected()) {
        setVisible(false);
      }
      final ResourceEntry entry = lpInternal.getSelectedValue();
      NearInfinity.getInstance().showResourceEntry(entry);
    } else if (event.getSource() == bOpenNew) {
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
    } else if (event.getSource() == cbAlwaysOnTop) {
      setAlwaysOnTop(cbAlwaysOnTop.isSelected());
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  /** Attempts to open the specified external game resource. */
  public static void openExternalFile(Component parent, Path file) {
    if (!FileEx.create(file).exists()) {
      JOptionPane.showMessageDialog(parent, '\"' + file.toString() + "\" not found", "Error",
          JOptionPane.ERROR_MESSAGE);
    } else {
      try {
        final long sizeLimit = 1_000_000_000L;  // 1 GB
        final long fileSize = Files.size(file);
        if (fileSize > sizeLimit) {
          final int result = JOptionPane.showConfirmDialog(parent,
              String.format("Selected file is %.1f GB big. Open anyway?", (double)fileSize / sizeLimit),
              "Question", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
          if (result != JOptionPane.YES_OPTION) {
            return;
          }
        }
      } catch (IOException e) {
        Logger.error(e);
      }
      new ViewFrame(parent, ResourceFactory.getResource(new FileResourceEntry(file)));
    }
  }

  // -------------------------- INNER CLASSES --------------------------

  private final class MyDropTargetListener implements DropTargetListener, Runnable {
    private List<File> files;

    private MyDropTargetListener() {
    }

    @Override
    public void dragEnter(DropTargetDragEvent event) {
    }

    @Override
    public void dragOver(DropTargetDragEvent event) {
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent event) {
    }

    @Override
    public void dragExit(DropTargetEvent event) {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void drop(DropTargetDropEvent event) {
      if (event.isLocalTransfer() || !event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        event.rejectDrop();
        return;
      }
      try {
        event.acceptDrop(DnDConstants.ACTION_COPY);
        files = (List<File>) event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
      } catch (Exception e) {
        Logger.error(e);
        event.dropComplete(false);
        return;
      }
      event.dropComplete(true);
      new Thread(this).start();
    }

    @Override
    public void run() {
      if (files != null) {
        for (final File file : files) {
          if (file != null && !file.isDirectory()) {
            openExternalFile(OpenFileFrame.this, file.toPath());
          }
        }
      }
    }
  }
}
