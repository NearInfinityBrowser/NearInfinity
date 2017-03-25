// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.InfinityScrollPane;
import org.infinity.gui.InfinityTextArea;
import org.infinity.gui.StringEditor;
import org.infinity.gui.StructViewer;
import org.infinity.gui.ViewFrame;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.StringReferenceSearcher;
import org.infinity.util.StringTable;

public final class StringRef extends Datatype implements Editable, IsNumeric, IsTextual, ActionListener
{
  private JButton bPlay, bEdit, bUpdate, bSearch;
  private InfinityTextArea taRefText;
  private JTextField tfRefNr;
  private int value;

  public StringRef(String name, int value)
  {
    this(null, name, value);
  }

  public StringRef(StructEntry parent, String name, int value)
  {
    super(parent, 0, 4, name);
    this.value = value;
  }

  public StringRef(ByteBuffer buffer, int offset, String name)
  {
    this(null, buffer, offset, name);
  }

  public StringRef(StructEntry parent, ByteBuffer buffer, int offset, String name)
  {
    super(parent, offset, 4, name);
    read(buffer, offset);
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == tfRefNr || event.getSource() == bUpdate) {
      taRefText.setText(StringTable.getStringRef(Integer.parseInt(tfRefNr.getText())));
      String resname = StringTable.getSoundResource(Integer.parseInt(tfRefNr.getText()));
      bPlay.setEnabled(!resname.isEmpty() && ResourceFactory.resourceExists(resname + ".WAV"));
    }
    else if (event.getSource() == bEdit) {
      StringEditor editor = null;
      List<ChildFrame> frames = ChildFrame.getFrames(StringEditor.class);
      if (!frames.isEmpty()) {
        editor = (StringEditor)frames.get(0);
      }
      if (editor == null) {
        new StringEditor(Integer.parseInt(tfRefNr.getText()));
      } else {
        editor.setVisible(true);
        editor.showEntry(StringTable.Type.MALE, Integer.parseInt(tfRefNr.getText()));
      }
    }
    else if (event.getSource() == bPlay) {
      int newvalue = Integer.parseInt(tfRefNr.getText());
      ResourceEntry entry = ResourceFactory.getResourceEntry(StringTable.getSoundResource(newvalue) + ".WAV");
      new ViewFrame(bPlay.getTopLevelAncestor(), ResourceFactory.getResource(entry));
    }
    else if (event.getSource() == bSearch) {
      new StringReferenceSearcher(Integer.parseInt(tfRefNr.getText()), bSearch.getTopLevelAncestor());
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Editable ---------------------

  @Override
  public JComponent edit(ActionListener container)
  {
    if (tfRefNr == null) {
      tfRefNr = new JTextField(8);
      tfRefNr.addActionListener(this);
      taRefText = new InfinityTextArea(1, 200, true);
      if (BrowserMenuBar.getInstance().getTlkSyntaxHighlightingEnabled()) {
        taRefText.applyExtendedSettings(InfinityTextArea.Language.TLK, null);
      }
      taRefText.setHighlightCurrentLine(false);
      taRefText.setEditable(false);
      taRefText.setLineWrap(true);
      taRefText.setWrapStyleWord(true);
      taRefText.setMargin(new Insets(3, 3, 3, 3));
      bPlay = new JButton("Sound", Icons.getIcon(Icons.ICON_VOLUME_16));
      bPlay.setToolTipText("Opens associated sound");
      bPlay.addActionListener(this);
      bEdit = new JButton("Edit", Icons.getIcon(Icons.ICON_EDIT_16));
      bEdit.setToolTipText("Opens string editor");
      bEdit.setMnemonic('e');
      bEdit.addActionListener(this);
      bSearch = new JButton("Find references...", Icons.getIcon(Icons.ICON_FIND_16));
      bSearch.addActionListener(this);
      bSearch.setMnemonic('f');
    }
    String resname = StringTable.getSoundResource(value);
    bPlay.setEnabled(!resname.isEmpty() && ResourceFactory.resourceExists(resname + ".WAV"));
    taRefText.setText(StringTable.getStringRef(value));
    taRefText.setCaretPosition(0);
    InfinityScrollPane scroll = new InfinityScrollPane(taRefText, true);
    scroll.setLineNumbersEnabled(false);
    tfRefNr.setText(String.valueOf(value));
    tfRefNr.setMinimumSize(tfRefNr.getPreferredSize());
    JLabel label = new JLabel("StringRef: ");
    label.setLabelFor(tfRefNr);
    label.setDisplayedMnemonic('s');
    bPlay.setMargin(new Insets(1, 3, 1, 3));
    bEdit.setMargin(bPlay.getMargin());
    bSearch.setMargin(bPlay.getMargin());
    tfRefNr.setMinimumSize(new Dimension(tfRefNr.getPreferredSize().width, bPlay.getPreferredSize().height));

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
    if (container != null) {
      bUpdate = new JButton("Update value", Icons.getIcon(Icons.ICON_REFRESH_16));
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

  @Override
  public void select()
  {
  }

  @Override
  public boolean updateValue(AbstractStruct struct)
  {
    int newvalue = Integer.parseInt(tfRefNr.getText());
    String newstring = StringTable.getStringRef(newvalue);
    if (newstring.equalsIgnoreCase("Error"))
      return false;
    value = newvalue;

    // notifying listeners
    fireValueUpdated(new UpdateEvent(this, struct));

    return true;
  }

// --------------------- End Interface Editable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    writeInt(os, value);
  }

// --------------------- End Interface Writeable ---------------------

//--------------------- Begin Interface Readable ---------------------

  @Override
  public int read(ByteBuffer buffer, int offset)
  {
    buffer.position(offset);
    value = buffer.getInt();

    return offset + getSize();
  }

//--------------------- End Interface Readable ---------------------

  @Override
  public String toString()
  {
    return toString(StringTable.Format.NONE);
  }

  public String toString(StringTable.Format fmt)
  {
    if (fmt == null) {
      fmt = StringTable.Format.NONE;
    }
    return StringTable.getStringRef(value, fmt);
  }

//--------------------- Begin Interface IsNumeric ---------------------

  @Override
  public long getLongValue()
  {
    return (long)value & 0xffffffffL;
  }

  @Override
  public int getValue()
  {
    return value;
  }

//--------------------- End Interface IsNumeric ---------------------

//--------------------- Begin Interface IsTextual ---------------------

  @Override
  public String getText()
  {
    return StringTable.getStringRef(value);
  }

//--------------------- End Interface IsTextual ---------------------

  public void setValue(int newvalue)
  {
    value = newvalue;
    taRefText.setText(StringTable.getStringRef(value));
    tfRefNr.setText(String.valueOf(value));
    String resname = StringTable.getSoundResource(value);
    bPlay.setEnabled(!resname.isEmpty() && ResourceFactory.resourceExists(resname + ".WAV"));
  }
}

