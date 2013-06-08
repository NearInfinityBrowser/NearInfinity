// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.gui.StructViewer;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.util.ArrayUtil;
import infinity.util.Byteconvert;
import infinity.util.Filewriter;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.OutputStream;

public final class TextEdit extends Datatype implements Editable
{
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
					textArea.getText().getBytes().length + str.getBytes().length > maxLength)
				return;
			super.insertString(offs, str, a);
		}
	}

	JTextArea textArea;
	private byte[] bytes;
	private String text;

	public TextEdit(byte buffer[], int offset, int length, String name)
	{
		super(offset, length, name);
		bytes = ArrayUtil.getSubArray(buffer, offset, length);
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
			text = Byteconvert.convertString(bytes, 0, bytes.length);
		return text;
	}
	
	public byte[] toArray()
	{
		if (text != null) {
			byte[] buf = text.getBytes();
			int imax = buf.length < bytes.length ? buf.length : bytes.length;
			for (int i = 0; i < imax; i++)
				bytes[i] = buf[i];
			for (int i = imax; i < bytes.length; i++)
				bytes[i] = 0;
		}
		return bytes;
	}
}
