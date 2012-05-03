// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import infinity.NearInfinity;
import infinity.icon.Icons;
import infinity.resource.ResourceFactory;
import infinity.resource.key.FileResourceEntry;
import infinity.resource.key.ResourceEntry;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class OpenFileFrame extends ChildFrame implements ActionListener
{
  private static final JFileChooser fc = new JFileChooser(".");
  private final JButton bExternalBrowse = new JButton("Browse...");
  private final JButton bOpen = new JButton("Open", Icons.getIcon("Open16.gif"));
  private final JButton bOpenNew = new JButton("Open in new window", Icons.getIcon("Open16.gif"));
  private final JCheckBox cbStayOpen = new JCheckBox("Keep this dialog open");
  private final JLabel lExternalDrop = new JLabel("or drop file(s) here", JLabel.CENTER);
  private final JRadioButton rbExternal = new JRadioButton("Open external file");
  private final JRadioButton rbInternal = new JRadioButton("Open internal file");
  private final JTextField tfExternalName = new JTextField(20);
  private final TextListPanel lpInternal;

  OpenFileFrame()
  {
    super("Open File");
    setIconImage(Icons.getIcon("Open16.gif").getImage());
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
      public void insertUpdate(DocumentEvent e)
      {
        bOpenNew.setEnabled(rbInternal.isSelected() || tfExternalName.getText().length() > 0);
      }

      public void removeUpdate(DocumentEvent e)
      {
        bOpenNew.setEnabled(rbInternal.isSelected() || tfExternalName.getText().length() > 0);
      }

      public void changedUpdate(DocumentEvent e)
      {
        bOpenNew.setEnabled(rbInternal.isSelected() || tfExternalName.getText().length() > 0);
      }
    });
    lpInternal =
    new TextListPanel(new ArrayList<ResourceEntry>(ResourceFactory.getInstance().getResources().getResourceEntries()));
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
    else if (event.getSource() == tfExternalName)
      openExternalFile(new File(tfExternalName.getText()));
    else if (event.getSource() == bExternalBrowse) {
      if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        tfExternalName.setText(fc.getSelectedFile().toString());
    }
    else if (event.getSource() == bOpen || event.getSource() == lpInternal) {
      if (!cbStayOpen.isSelected())
        setVisible(false);
      ResourceEntry entry = (ResourceEntry)lpInternal.getSelectedValue();
      NearInfinity.getInstance().showResourceEntry(entry);
    }
    else if (event.getSource() == bOpenNew) {
      if (!cbStayOpen.isSelected())
        setVisible(false);
      if (rbInternal.isSelected())
        new ViewFrame(this,
                      ResourceFactory.getResource((ResourceEntry)lpInternal.getSelectedValue()));
      else
        openExternalFile(new File(tfExternalName.getText()));
    }
  }

// --------------------- End Interface ActionListener ---------------------

  private void openExternalFile(File file)
  {
    if (!file.exists())
      JOptionPane.showMessageDialog(this, '\"' + file.toString() + "\" not found",
                                    "Error", JOptionPane.ERROR_MESSAGE);
    else
      new ViewFrame(this, ResourceFactory.getResource(new FileResourceEntry(file)));
  }

// -------------------------- INNER CLASSES --------------------------

  private final class MyDropTargetListener implements DropTargetListener, Runnable
  {
    private List files;

    private MyDropTargetListener()
    {
    }

    public void dragEnter(DropTargetDragEvent event)
    {
    }

    public void dragOver(DropTargetDragEvent event)
    {
    }

    public void dropActionChanged(DropTargetDragEvent event)
    {
    }

    public void dragExit(DropTargetEvent event)
    {
    }

    public void drop(DropTargetDropEvent event)
    {
      if (event.isLocalTransfer() || !event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        event.rejectDrop();
        return;
      }
      try {
        event.acceptDrop(DnDConstants.ACTION_COPY);
        files = (List)event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
      } catch (Exception e) {
        e.printStackTrace();
        event.dropComplete(false);
        return;
      }
      event.dropComplete(true);
      new Thread(this).start();
    }

    public void run()
    {
      for (int i = 0; i < files.size(); i++) {
        File file = (File)files.get(i);
        if (!file.isDirectory())
          openExternalFile(file);
      }
    }
  }
}

