// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.gui.StructViewer;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.util.ArrayUtil;
import infinity.util.DynamicArray;
import infinity.util.Filewriter;

import javax.swing.*;
import javax.swing.text.*;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumMap;

public final class TextEdit extends Datatype implements Editable
{
  public static enum EOLType {
    UNIX, WINDOWS
  }

  private static final EnumMap<EOLType, String> EOL = new EnumMap<EOLType, String>(EOLType.class);
  static {
    EOL.put(EOLType.UNIX, "\n");
    EOL.put(EOLType.WINDOWS, "\r\n");
  }

  JTextArea textArea;
  private byte[] bytes;
  private String text;
  private EOLType eolType;

  public TextEdit(byte buffer[], int offset, int length, String name)
  {
    this(buffer, offset, length, name, EOLType.UNIX);
  }

  public TextEdit(byte buffer[], int offset, int length, String name, EOLType eolType)
  {
    super(offset, length, name);
    bytes = ArrayUtil.getSubArray(buffer, offset, length);
    this.eolType = (eolType != null) ? eolType : EOLType.UNIX;
  }

  // --------------------- Begin Interface Editable ---------------------

  public JComponent edit(ActionListener container)
  {
    JButton bUpdate;
    if (textArea == null) {
      textArea = new JTextArea(1, 200);
      textArea.setWrapStyleWord(true);
      textArea.setLineWrap(true);
      textArea.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      textArea.setDocument(new FixedDocument(textArea, bytes.length));
    }
    textArea.setText(toString());

    bUpdate = new JButton("Update value", Icons.getIcon("Refresh16.gif"));
    bUpdate.addActionListener(container);
    bUpdate.setActionCommand(StructViewer.UPDATE_VALUE);
    JScrollPane scroll = new JScrollPane(textArea);

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);

    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbl.setConstraints(scroll, gbc);
    panel.add(scroll);

    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.insets.left = 6;
    gbl.setConstraints(bUpdate, gbc);
    panel.add(bUpdate);

    panel.setMinimumSize(DIM_BROAD);
    panel.setPreferredSize(DIM_BROAD);
    return panel;
  }

  public void select()
  {
  }

  public boolean updateValue(AbstractStruct struct)
  {
    text = textArea.getText();
    return true;
  }

  // --------------------- End Interface Editable ---------------------


  // --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    Filewriter.writeBytes(os, toArray());
  }

  // --------------------- End Interface Writeable ---------------------

  public String toString()
  {
    if (text == null)
      text = eolConvert(DynamicArray.getString(bytes, 0, bytes.length), System.getProperty("line.separator"));
    return text;
  }

  public byte[] toArray()
  {
    if (text != null) {
      byte[] buf = eolConvert(text).getBytes();
      int imax = buf.length < bytes.length ? buf.length : bytes.length;
      for (int i = 0; i < imax; i++)
        bytes[i] = buf[i];
      for (int i = imax; i < bytes.length; i++)
        bytes[i] = 0;
    }
    return bytes;
  }

  private String eolConvert(String s)
  {
    if (s != null && s.length() > 0)
      return s.replaceAll("(\r\n|\n)", EOL.get(eolType));
    else
      return s;
  }

  private String eolConvert(String s, String eol)
  {
    if (s != null && s.length() > 0 && eol != null && eol.length() > 0)
      return s.replaceAll("(\r\n|\n)", eol);
    else
      return s;
  }


//-------------------------- INNER CLASSES --------------------------

  // Ensures a size limit on byte level
  private class FixedDocument extends PlainDocument
  {
    private int maxLength;
    private JTextArea textArea;

    FixedDocument(JTextArea text, int length)
    {
      super();
      textArea = text;
      maxLength = length >= 0 ? length : 0;
    }

    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException
    {
      if (str == null || textArea == null ||
          eolConvert(textArea.getText()).getBytes().length + eolConvert(str).getBytes().length > maxLength)
        return;
      super.insertString(offs, str, a);
    }
  }
}
