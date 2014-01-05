// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.gui.StructViewer;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.util.Filewriter;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.EnumMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

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
  private String charsetName;
  private boolean terminateString, editable;

  public TextEdit(byte buffer[], int offset, int length, String name)
  {
    super(offset, length, name);
    bytes = Arrays.copyOfRange(buffer, offset, offset + length);
    this.eolType = EOLType.UNIX;
    this.charsetName = Charset.defaultCharset().name();
    this.terminateString = false;
    this.editable = true;
  }

  // --------------------- Begin Interface Editable ---------------------

  @Override
  public JComponent edit(ActionListener container)
  {
    JButton bUpdate;
    if (textArea == null) {
      textArea = new JTextArea(1, 200);
      textArea.setWrapStyleWord(true);
      textArea.setLineWrap(true);
      textArea.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      textArea.setDocument(new FixedDocument(textArea, bytes.length));
      textArea.setEditable(editable);
    }
    textArea.setText(toString());

    bUpdate = new JButton("Update value", Icons.getIcon("Refresh16.gif"));
    bUpdate.setEnabled(editable);
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

  @Override
  public void select()
  {
  }

  @Override
  public boolean updateValue(AbstractStruct struct)
  {
    text = textArea.getText();
    return true;
  }

  // --------------------- End Interface Editable ---------------------


  // --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    Filewriter.writeBytes(os, toArray());
  }

  // --------------------- End Interface Writeable ---------------------

  @Override
  public String toString()
  {
    if (text == null)
      try {
        int len = 0;
        while (len < bytes.length && bytes[len] != 0) {
          len++;
        }
        text = eolConvert(new String(bytes, 0, len, charsetName), System.getProperty("line.separator"));
      } catch (UnsupportedEncodingException e) {
        text = eolConvert(new String(bytes, 0, bytes.length), System.getProperty("line.separator"));
        e.printStackTrace();
      }
    return text;
  }

  public byte[] toArray()
  {
    if (text != null) {
      byte[] buf = null;
      try {
        buf = eolConvert(text).getBytes(charsetName);
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
        buf = eolConvert(text).getBytes();
      }
      if (buf != null) {
        // XXX: multibyte encodings may cause issues
        int imax = buf.length < bytes.length ? buf.length : bytes.length;
        for (int i = 0; i < imax; i++)
          bytes[i] = buf[i];
        for (int i = imax; i < bytes.length; i++)
          bytes[i] = 0;
        if (terminateString)
          bytes[bytes.length - 1] = 0;    // ensure null-termination
      }
    }
    return bytes;
  }

  public EOLType getEolType()
  {
    return eolType;
  }

  public void setEolType(EOLType type)
  {
    if (type != null)
      eolType = type;
  }

  public boolean getStringTerminated()
  {
    return terminateString;
  }

  public void setStringTerminated(boolean terminated)
  {
    terminateString = terminated;
  }

  public String getCharset()
  {
    return charsetName;
  }

  public boolean setCharset(String charsetName)
  {
    if (Charset.isSupported(charsetName)) {
      this.charsetName = charsetName;
      return true;
    } else {
      return false;
    }
  }

  public boolean getEditable()
  {
    return editable;
  }

  public void setEditable(boolean edit)
  {
    editable = edit;
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

    @Override
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException
    {
      if (str == null || textArea == null ||
          eolConvert(textArea.getText()).getBytes().length + eolConvert(str).getBytes().length > maxLength)
        return;
      super.insertString(offs, str, a);
    }
  }
}
