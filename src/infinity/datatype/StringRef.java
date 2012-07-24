// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.gui.*;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.ResourceFactory;
import infinity.resource.key.ResourceEntry;
import infinity.search.StringReferenceSearcher;
import infinity.util.Byteconvert;
import infinity.util.StringResource;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public final class StringRef extends Datatype implements Editable, ActionListener
{
  private JButton bPlay, bEdit, bUpdate, bSearch;
  private JTextArea taRefText;
  private JTextField tfRefNr;
  private int value;

  public StringRef(String name, int value)
  {
    super(0, 8, name); // OK?
    this.value = value;
  }

  public StringRef(byte buffer[], int offset, String name)
  {
    super(offset, 4, name);
    value = Byteconvert.convertInt(buffer, offset);
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == tfRefNr || event.getSource() == bUpdate) {
      taRefText.setText(StringResource.getStringRef(Integer.parseInt(tfRefNr.getText())));
      String resname = StringResource.getResource(Integer.parseInt(tfRefNr.getText()));
      bPlay.setEnabled(resname != null && ResourceFactory.getInstance().resourceExists(resname + ".WAV"));
    }
    else if (event.getSource() == bEdit) {
      StringEditor editor = null;
      List<ChildFrame> frames = ChildFrame.getFrames(StringEditor.class);
      for (int i = 0; i < frames.size(); i++) {
        StringEditor e = (StringEditor)frames.get(i);
        if (e.getFile().equals(StringResource.getFile()))
          editor = e;
      }
      if (editor == null)
        new StringEditor(StringResource.getFile(), Integer.parseInt(tfRefNr.getText()));
      else {
        editor.setVisible(true);
        editor.showEntry(Integer.parseInt(tfRefNr.getText()));
      }
    }
    else if (event.getSource() == bPlay) {
      int newvalue = Integer.parseInt(tfRefNr.getText());
      ResourceEntry entry = ResourceFactory.getInstance().getResourceEntry(
              StringResource.getResource(newvalue) + ".WAV");
      new ViewFrame(bPlay.getTopLevelAncestor(), ResourceFactory.getResource(entry));
    }
    else if (event.getSource() == bSearch)
      new StringReferenceSearcher(Integer.parseInt(tfRefNr.getText()), bSearch.getTopLevelAncestor());
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Editable ---------------------

  public JComponent edit(ActionListener container)
  {
    if (tfRefNr == null) {
      tfRefNr = new JTextField(8);
      tfRefNr.addActionListener(this);
      taRefText = new JTextArea(1, 200);
      taRefText.setEditable(false);
      taRefText.setLineWrap(true);
      taRefText.setWrapStyleWord(true);
      taRefText.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      bPlay = new JButton("Sound", Icons.getIcon("Volume16.gif"));
      bPlay.setToolTipText("Opens associated sound");
      bPlay.addActionListener(this);
      bEdit = new JButton("Edit", Icons.getIcon("Edit16.gif"));
      bEdit.setToolTipText("Opens string editor");
      bEdit.setMnemonic('e');
      bEdit.addActionListener(this);
      bSearch = new JButton("Find references...", Icons.getIcon("Find16.gif"));
      bSearch.addActionListener(this);
      bSearch.setMnemonic('f');
    }
    String resname = StringResource.getResource(value);
    bPlay.setEnabled(resname != null && ResourceFactory.getInstance().resourceExists(resname + ".WAV"));
    taRefText.setText(StringResource.getStringRef(value));
    taRefText.setCaretPosition(0);
    tfRefNr.setText(String.valueOf(value));
    tfRefNr.setMinimumSize(tfRefNr.getPreferredSize());
    JLabel label = new JLabel("StringRef: ");
    label.setLabelFor(tfRefNr);
    label.setDisplayedMnemonic('s');
    JScrollPane scroll = new JScrollPane(taRefText);
    bPlay.setMargin(new Insets(1, 3, 1, 3));
    bEdit.setMargin(bPlay.getMargin());
    bSearch.setMargin(bPlay.getMargin());
    tfRefNr.setMinimumSize(new Dimension(tfRefNr.getPreferredSize().width, bPlay.getPreferredSize().height));

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
    if (container != null) {
      bUpdate = new JButton("Update value", Icons.getIcon("Refresh16.gif"));
      bUpdate.setMargin(bPlay.getMargin());
      bUpdate.setActionCommand(StructViewer.UPDATE_VALUE);
      bUpdate.addActionListener(this);
      bUpdate.addActionListener(container);
      buttonPanel.add(bUpdate);
    }
    buttonPanel.add(bPlay);
    buttonPanel.add(bEdit);
    buttonPanel.add(bSearch);

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);

    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.insets = new Insets(0, 0, 3, 3);
    gbc.fill = GridBagConstraints.NONE;
    gbl.setConstraints(label, gbc);
    panel.add(label);

    gbc.anchor = GridBagConstraints.WEST;
    gbl.setConstraints(tfRefNr, gbc);
    panel.add(tfRefNr);

    gbc.insets.right = 0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(buttonPanel, gbc);
    panel.add(buttonPanel);

    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbl.setConstraints(scroll, gbc);
    panel.add(scroll);

    panel.setMinimumSize(DIM_BROAD);
    panel.setPreferredSize(DIM_BROAD);
    return panel;
  }

  public void select()
  {
  }

  public boolean updateValue(AbstractStruct struct)
  {
    int newvalue = Integer.parseInt(tfRefNr.getText());
    String newstring = StringResource.getStringRef(newvalue);
    if (newstring.equalsIgnoreCase("Error"))
      return false;
    value = newvalue;
    return true;
  }

// --------------------- End Interface Editable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    super.writeInt(os, value);
  }

// --------------------- End Interface Writeable ---------------------

  public String toString()
  {
    return StringResource.getStringRef(value);
  }

  public int getValue()
  {
    return value;
  }

  public void setValue(int newvalue)
  {
    value = newvalue;
    taRefText.setText(StringResource.getStringRef(value));
    tfRefNr.setText(String.valueOf(value));
    String resname = StringResource.getResource(value);
    bPlay.setEnabled(resname != null && ResourceFactory.getInstance().resourceExists(resname + ".WAV"));
  }
}

