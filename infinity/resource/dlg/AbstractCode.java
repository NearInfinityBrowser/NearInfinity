// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.dlg;

import infinity.datatype.*;
import infinity.gui.BrowserMenuBar;
import infinity.gui.ButtonPopupMenu;
import infinity.gui.ScriptTextArea;
import infinity.gui.StructViewer;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.bcs.Compiler;
import infinity.util.Filewriter;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.List;

public abstract class AbstractCode extends Datatype implements Editable, AddRemovable, ActionListener,
                                                               DocumentListener, ItemListener
{
  private final DecNumber len;
  private final DecNumber off;
  private ButtonPopupMenu bError, bWarning;
  private JButton bUpdate, bCheck;
  private ScriptTextArea textArea;
  private SortedMap<Integer, String> errors, warnings;
  private String text;

  AbstractCode(String name)
  {
    this(new byte[8], 0, name);
    text = "";
  }

  AbstractCode(byte buffer[], int offset, String nane)
  {
    super(offset, 8, nane);
    off = new DecNumber(buffer, offset, 4, "Offset");
    len = new DecNumber(buffer, offset + 4, 4, "Length");
    text = new String(buffer, off.getValue(), len.getValue());
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bCheck) {
      Compiler.getInstance().compileDialogCode(textArea.getText(), this instanceof Action);
      errors = Compiler.getInstance().getErrors();
      warnings = Compiler.getInstance().getWarnings();
      if (errors.size() > 0) {
        JMenuItem errorItems[] = new JMenuItem[errors.size()];
        int count = 0;
        for (Integer lineNr : errors.keySet()) {
          String error = errors.get(lineNr);
          errorItems[count++] = new JMenuItem(lineNr.toString() + ": " + error);
        }
        bError.setMenuItems(errorItems);
      }
      if (warnings.size() > 0) {
        JMenuItem warningItems[] = new JMenuItem[warnings.size()];
        int count = 0;
        for (Integer lineNr : warnings.keySet()) {
          String warning = warnings.get(lineNr);
          warningItems[count++] = new JMenuItem(lineNr.toString() + ": " + warning);
        }
        bWarning.setMenuItems(warningItems);
      }
      bError.setEnabled(errors.size() > 0);
      bWarning.setEnabled(warnings.size() > 0);
      bError.setText("Errors (" + errors.size() + ")...");
      bWarning.setText("Warnings (" + warnings.size() + ")...");
      bCheck.setEnabled(false);
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface DocumentListener ---------------------

  public void insertUpdate(DocumentEvent event)
  {
    bUpdate.setEnabled(true);
    bCheck.setEnabled(true);
  }

  public void removeUpdate(DocumentEvent event)
  {
    bUpdate.setEnabled(true);
    bCheck.setEnabled(true);
  }

  public void changedUpdate(DocumentEvent event)
  {
    bUpdate.setEnabled(true);
    bCheck.setEnabled(true);
  }

// --------------------- End Interface DocumentListener ---------------------


// --------------------- Begin Interface Editable ---------------------

  public JComponent edit(ActionListener container)
  {
    textArea = new ScriptTextArea();
    textArea.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    String convertedText = text;
    int index = convertedText.indexOf((int)'\r');
    while (index != -1) {
      convertedText = convertedText.substring(0, index) + convertedText.substring(index + 1);
      index = convertedText.indexOf((int)'\r');
    }
    textArea.setText(convertedText);
    textArea.setCaretPosition(0);
    textArea.getDocument().addDocumentListener(this);

    bUpdate = new JButton("Update", Icons.getIcon("Refresh16.gif"));
    bUpdate.addActionListener(container);
    bUpdate.setActionCommand(StructViewer.UPDATE_VALUE);
    bUpdate.setEnabled(false);
    bCheck = new JButton("Compile Check", Icons.getIcon("Redo16.gif"));
    bCheck.addActionListener(this);
    bError = new ButtonPopupMenu("Errors (0)...", new JMenuItem[]{});
    bError.setEnabled(false);
    bWarning = new ButtonPopupMenu("Warnings (0)...", new JMenuItem[]{});
    bWarning.setEnabled(false);
    bError.addItemListener(this);
    bWarning.addItemListener(this);
    bError.setIcon(Icons.getIcon("Up16.gif"));
    bWarning.setIcon(Icons.getIcon("Up16.gif"));
    JScrollPane scroll = new JScrollPane(textArea);

    JPanel bPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    bPanel.add(bUpdate);
    bPanel.add(bCheck);
    bPanel.add(bError);
    bPanel.add(bWarning);

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);

    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets.left = 5;
    gbc.insets.right = 5;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(scroll, gbc);
    panel.add(scroll);

    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets.top = 3;
    gbc.insets.left = 0;
    gbl.setConstraints(bPanel, gbc);
    panel.add(bPanel);

    panel.setMinimumSize(DIM_BROAD);
    panel.setPreferredSize(DIM_BROAD);
    return panel;
  }

  public void select()
  {
    if (BrowserMenuBar.getInstance().autocheckBCS())
      bCheck.doClick();
  }

  public boolean updateValue(AbstractStruct struct)
  {
    if (bCheck.isEnabled())
      bCheck.doClick();
    if (errors.size() > 0) {
      String options[] = {"Update", "Cancel"};
      if (JOptionPane.showOptionDialog(textArea.getTopLevelAncestor(), "Errors exist. Update anyway?", "Update value",
                                       JOptionPane.YES_NO_OPTION,
                                       JOptionPane.WARNING_MESSAGE, null, options, options[0]) == 1)
        return true;
    }
    text = textArea.getText();
    int index = text.indexOf((int)'\n');
    while (index != -1) {
      text = text.substring(0, index) + '\r' + text.substring(index);
      index = text.indexOf((int)'\n', index + 2);
    }
    bUpdate.setEnabled(false);
    return true;
  }

// --------------------- End Interface Editable ---------------------


// --------------------- Begin Interface ItemListener ---------------------

  public void itemStateChanged(ItemEvent event)
  {
    String selected = "";
    if (event.getSource() == bError)
      selected = bError.getSelectedItem().getText();
    else if (event.getSource() == bWarning)
      selected = bWarning.getSelectedItem().getText();
    int index = selected.indexOf(": ");
    int line = Integer.parseInt(selected.substring(0, index));
    highlightLine(line);
  }

// --------------------- End Interface ItemListener ---------------------


// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    off.write(os);
    len.write(os);
  }

// --------------------- End Interface Writeable ---------------------

  public String toString()
  {
    return text;
  }

  public void addFlatList(List<StructEntry> flatList)
  {
    flatList.add(off);
    flatList.add(len);
    try {
      TextString ts = new TextString(text.getBytes(), 0, len.getValue(), "Text");
      ts.setOffset(off.getValue());
      flatList.add(ts);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public int getTextLength()
  {
    return len.getValue();
  }

  public int updateOffset(int offs)
  {
    off.setValue(offs);
    len.setValue(text.length());
    return len.getValue();
  }

  public void writeString(OutputStream os) throws IOException
  {
    Filewriter.writeString(os, text, len.getValue());
  }

  private void highlightLine(int linenr)
  {
    String s = textArea.getText() + '\n';
    int startpos = 0;
    for (int i = 1; i < linenr; i++)
      startpos = s.indexOf("\n", startpos + 1);
    if (startpos == -1) return;
    int endpos = s.indexOf("\n", startpos + 1);
    textArea.select(startpos, endpos);
    textArea.getCaret().setSelectionVisible(true);
  }
}

