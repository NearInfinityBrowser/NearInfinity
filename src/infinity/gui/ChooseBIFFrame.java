// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import infinity.NearInfinity;
import infinity.icon.Icons;
import infinity.resource.ResourceFactory;
import infinity.resource.key.BIFFArchive;
import infinity.resource.key.BIFFEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

final class ChooseBIFFrame extends ChildFrame implements ActionListener
{
  private final BIFFEditor editor;
  private final JButton bok = new JButton("OK");
  private final JButton bcancel = new JButton("Cancel");
  private final JComboBox cbbifname;
  private final JRadioButton rbbiff = new JRadioButton("BIFF - All games");
  private final JRadioButton rbbif = new JRadioButton("BIF - Only Icewind Dale");
  private final JRadioButton rbbifc = new JRadioButton("BIFC - Only Baldur's Gate 2");
  private final JRadioButton rbedit = new JRadioButton("Edit existing");
  private final JRadioButton rbcreate = new JRadioButton("Create new");
  private final JTextField tfbifname = new JTextField(10);

  ChooseBIFFrame(BIFFEditor editor)
  {
    super("Edit BIFF: Select file", true);
    setIconImage(Icons.getIcon("Edit16.gif").getImage());
    this.editor = editor;
    bok.setMnemonic('o');
    bcancel.setMnemonic('c');
    rbcreate.setMnemonic('n');
    rbedit.setMnemonic('e');
    getRootPane().setDefaultButton(bok);

    JPanel format = new JPanel();
    format.setLayout(new GridLayout(3, 1));
    format.add(rbbiff);
    format.add(rbbif);
    format.add(rbbifc);
    format.setBorder(BorderFactory.createTitledBorder("Select format:"));
    ButtonGroup bg1 = new ButtonGroup();
    bg1.add(rbbiff);
    bg1.add(rbbif);
    bg1.add(rbbifc);
    rbbiff.setSelected(true);

    JPanel bpanel = new JPanel(new GridLayout(1, 2, 6, 6));
    bpanel.add(bok);
    bpanel.add(bcancel);
    bok.addActionListener(this);
    bcancel.addActionListener(this);

    JPanel lowerpanel = new JPanel();
    lowerpanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
    lowerpanel.add(bpanel);

    tfbifname.setEnabled(false);
    rbbiff.setEnabled(false);
    rbbif.setEnabled(false);
    rbbifc.setEnabled(false);

    ButtonGroup bg2 = new ButtonGroup();
    bg2.add(rbedit);
    bg2.add(rbcreate);
    rbedit.setSelected(true);
    rbedit.addActionListener(this);
    rbcreate.addActionListener(this);

    cbbifname = new JComboBox(ResourceFactory.getKeyfile().getBIFFEntriesSorted());
    cbbifname.setSelectedIndex(0);
    cbbifname.setEditable(false);

    JPanel pane = (JPanel)getContentPane();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    pane.setLayout(gbl);

    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.insets = new Insets(6, 6, 3, 6);
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(rbedit, gbc);
    pane.add(rbedit);

    JLabel label1 = new JLabel("Name:");
    gbc.insets = new Insets(3, 6, 6, 3);
    gbc.anchor = GridBagConstraints.EAST;
    gbc.gridwidth = 1;
    gbl.setConstraints(label1, gbc);
    pane.add(label1);

    gbc.insets = new Insets(3, 3, 6, 6);
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(cbbifname, gbc);
    pane.add(cbbifname);

    gbc.insets = new Insets(6, 6, 3, 6);
    gbc.anchor = GridBagConstraints.WEST;
    gbl.setConstraints(rbcreate, gbc);
    pane.add(rbcreate);

    JLabel label2 = new JLabel("Name:");
    gbc.insets = new Insets(3, 6, 3, 3);
    gbc.anchor = GridBagConstraints.EAST;
    gbc.gridwidth = 1;
    gbl.setConstraints(label2, gbc);
    pane.add(label2);

    gbc.insets = new Insets(3, 3, 3, 6);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(tfbifname, gbc);
    pane.add(tfbifname);
    tfbifname.addActionListener(this);

    JLabel label3 = new JLabel("(File will be saved as data\\<filename>)");
    gbl.setConstraints(label3, gbc);
    pane.add(label3);

//    gbc.insets = new Insets(3, 6, 6, 6);
//    gbc.fill = GridBagConstraints.NONE;
//    gbc.weighty = 1.0;
//    gbc.weightx = 0.0;
//    gbl.setConstraints(format, gbc);
//    pane.add(format);

    gbc.weighty = 0.0;
    gbl.setConstraints(lowerpanel, gbc);
    pane.add(lowerpanel);

    pack();
    Center.center(this, NearInfinity.getInstance().getBounds());
    setVisible(true);
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bcancel)
      close();
    else if (event.getSource() == rbedit) {
      cbbifname.setEnabled(true);
      tfbifname.setEnabled(false);
      rbbiff.setEnabled(false);
      rbbif.setEnabled(false);
      rbbifc.setEnabled(false);
    }
    else if (event.getSource() == rbcreate) {
      cbbifname.setEnabled(false);
      tfbifname.setEnabled(true);
      rbbiff.setEnabled(true);
      int gameid = ResourceFactory.getGameID();
      rbbif.setEnabled(gameid == ResourceFactory.ID_ICEWIND || gameid == ResourceFactory.ID_ICEWINDHOW ||
                       gameid == ResourceFactory.ID_ICEWINDHOWTOT);
      rbbifc.setEnabled(gameid == ResourceFactory.ID_BG2 || gameid == ResourceFactory.ID_BG2TOB);
    }
    else if (event.getSource() == bok || event.getSource() == tfbifname) {
      if (rbcreate.isSelected()) {
        // Check if name exists
        String name = tfbifname.getText().toLowerCase();
        if (name.equals("") || name.indexOf("\\") != -1 || name.indexOf("/") != -1) {
          JOptionPane.showMessageDialog(this, "Illegal BIFF name", "Error", JOptionPane.ERROR_MESSAGE);
          return;
        }
        name = "data\\" + name;
        int form = BIFFEditor.BIFF;
        if (rbbif.isSelected())
          form = BIFFEditor.BIF;
        else if (rbbifc.isSelected())
          form = BIFFEditor.BIFC;
        if (!name.endsWith(".bif"))
          name += ".bif";
        for (int i = 0; i < cbbifname.getItemCount(); i++)
          if (name.equalsIgnoreCase(cbbifname.getItemAt(i).toString())) {
            JOptionPane.showMessageDialog(this, "This BIFF already exists!", "Error",
                                          JOptionPane.ERROR_MESSAGE);
            return;
          }
        close();
        editor.makeEditor(new BIFFEntry(name), form);
      }
      else {
        // Edit existing
        BIFFEntry entry = (BIFFEntry)cbbifname.getSelectedItem();
        JOptionPane.showMessageDialog(this, "Make sure you have a backup of " + entry.getFile(),
                                      "Warning", JOptionPane.WARNING_MESSAGE);
        try {
          BIFFArchive file = ResourceFactory.getKeyfile().getBIFFFile(entry);
          int form = BIFFEditor.BIFF;
          if (file.getSignature().equals("BIF "))
            form = BIFFEditor.BIF;
          else if (file.getSignature().equals("BIFC"))
            form = BIFFEditor.BIFC;
          close();
          editor.makeEditor(entry, form);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------
}

