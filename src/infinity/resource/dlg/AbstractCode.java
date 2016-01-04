// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.dlg;

import infinity.datatype.Datatype;
import infinity.datatype.DecNumber;
import infinity.datatype.Editable;
import infinity.datatype.IsTextual;
import infinity.datatype.TextString;
import infinity.datatype.UpdateEvent;
import infinity.gui.BrowserMenuBar;
import infinity.gui.ButtonPanel;
import infinity.gui.ButtonPopupMenu;
import infinity.gui.InfinityScrollPane;
import infinity.gui.ScriptTextArea;
import infinity.gui.StructViewer;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.StructEntry;
import infinity.resource.bcs.Compiler;
import infinity.util.io.FileWriterNI;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.SortedMap;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public abstract class AbstractCode extends Datatype
    implements Editable, IsTextual, AddRemovable, ActionListener, DocumentListener, ItemListener
{
  // DLG/AbstractCode-specific field labels
  public static final String DLG_CODE_OFFSET = "Offset";
  public static final String DLG_CODE_LENGTH = "Length";
  public static final String DLG_CODE_TEXT    = "Text";

  private static final ButtonPanel.Control CtrlUpdate   = ButtonPanel.Control.Custom1;
  private static final ButtonPanel.Control CtrlCheck    = ButtonPanel.Control.Custom2;
  private static final ButtonPanel.Control CtrlErrors   = ButtonPanel.Control.Custom3;
  private static final ButtonPanel.Control CtrlWarnings = ButtonPanel.Control.Custom4;

  private final ButtonPanel buttonPanel = new ButtonPanel();

  private DecNumber len;
  private DecNumber off;
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
    read(buffer, offset);
    text = new String(buffer, off.getValue(), len.getValue());
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (buttonPanel.getControlByType(CtrlCheck) == event.getSource()) {
      JButton bCheck = (JButton)event.getSource();
      ButtonPopupMenu bpmErrors = (ButtonPopupMenu)buttonPanel.getControlByType(CtrlErrors);
      ButtonPopupMenu bpmWarnings = (ButtonPopupMenu)buttonPanel.getControlByType(CtrlWarnings);
      Compiler compiler = new Compiler(textArea.getText(),
                                       (this instanceof Action) ? Compiler.ScriptType.Action :
                                                                  Compiler.ScriptType.Trigger);
      errors = compiler.getErrors();
      warnings = compiler.getWarnings();
      if (errors.size() > 0) {
        JMenuItem errorItems[] = new JMenuItem[errors.size()];
        int count = 0;
        for (Integer lineNr : errors.keySet()) {
          String error = errors.get(lineNr);
          errorItems[count++] = new JMenuItem(lineNr.toString() + ": " + error);
        }
        bpmErrors.setMenuItems(errorItems);
      }
      if (warnings.size() > 0) {
        JMenuItem warningItems[] = new JMenuItem[warnings.size()];
        int count = 0;
        for (Integer lineNr : warnings.keySet()) {
          String warning = warnings.get(lineNr);
          warningItems[count++] = new JMenuItem(lineNr.toString() + ": " + warning);
        }
        bpmWarnings.setMenuItems(warningItems);
      }
      bpmErrors.setEnabled(errors.size() > 0);
      bpmWarnings.setEnabled(warnings.size() > 0);
      bpmErrors.setText("Errors (" + errors.size() + ")...");
      bpmWarnings.setText("Warnings (" + warnings.size() + ")...");
      bCheck.setEnabled(false);
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface DocumentListener ---------------------

  @Override
  public void insertUpdate(DocumentEvent event)
  {
    buttonPanel.getControlByType(CtrlUpdate).setEnabled(true);
    buttonPanel.getControlByType(CtrlCheck).setEnabled(true);
  }

  @Override
  public void removeUpdate(DocumentEvent event)
  {
    buttonPanel.getControlByType(CtrlUpdate).setEnabled(true);
    buttonPanel.getControlByType(CtrlCheck).setEnabled(true);
  }

  @Override
  public void changedUpdate(DocumentEvent event)
  {
    buttonPanel.getControlByType(CtrlUpdate).setEnabled(true);
    buttonPanel.getControlByType(CtrlCheck).setEnabled(true);
  }

// --------------------- End Interface DocumentListener ---------------------


// --------------------- Begin Interface Editable ---------------------

  @Override
  public JComponent edit(ActionListener container)
  {
    textArea = new ScriptTextArea();
    textArea.setMargin(new Insets(3, 3, 3, 3));
    String convertedText = text;
    int index = convertedText.indexOf((int)'\r');
    while (index != -1) {
      convertedText = convertedText.substring(0, index) + convertedText.substring(index + 1);
      index = convertedText.indexOf((int)'\r');
    }
    textArea.setText(convertedText);
    textArea.setCaretPosition(0);
    textArea.getDocument().addDocumentListener(this);

    JButton bUpdate = new JButton("Update", Icons.getIcon("Refresh16.gif"));
    bUpdate.addActionListener(container);
    bUpdate.setActionCommand(StructViewer.UPDATE_VALUE);
    bUpdate.setEnabled(false);
    buttonPanel.addControl(bUpdate, CtrlUpdate);

    JButton bCheck = new JButton("Compile Check", Icons.getIcon("Redo16.gif"));
    bCheck.addActionListener(this);
    buttonPanel.addControl(bCheck, CtrlCheck);

    ButtonPopupMenu bpmErrors = new ButtonPopupMenu("Errors (0)...", new JMenuItem[]{});
    bpmErrors.setIcon(Icons.getIcon("Up16.gif"));
    bpmErrors.addItemListener(this);
    bpmErrors.setEnabled(false);
    buttonPanel.addControl(bpmErrors, CtrlErrors);

    ButtonPopupMenu bpmWarnings = new ButtonPopupMenu("Warnings (0)...", new JMenuItem[]{});
    bpmWarnings.setIcon(Icons.getIcon("Up16.gif"));
    bpmWarnings.addItemListener(this);
    bpmWarnings.setEnabled(false);
    buttonPanel.addControl(bpmWarnings, CtrlWarnings);

    InfinityScrollPane scroll = new InfinityScrollPane(textArea, true);

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
    gbl.setConstraints(buttonPanel, gbc);
    panel.add(buttonPanel);

    panel.setMinimumSize(DIM_BROAD);
    panel.setPreferredSize(DIM_BROAD);
    return panel;
  }

  @Override
  public void select()
  {
    if (BrowserMenuBar.getInstance().autocheckBCS()) {
      ((JButton)buttonPanel.getControlByType(CtrlCheck)).doClick();
    }
  }

  @Override
  public boolean updateValue(AbstractStruct struct)
  {
    JButton bCheck = (JButton)buttonPanel.getControlByType(CtrlCheck);
    JButton bUpdate = (JButton)buttonPanel.getControlByType(CtrlUpdate);
    if (bCheck.isEnabled())
      bCheck.doClick();
    if (errors.size() > 0) {
      String options[] = {"Update", "Cancel"};
      if (JOptionPane.showOptionDialog(textArea.getTopLevelAncestor(), "Errors exist. Update anyway?", "Update value",
                                       JOptionPane.YES_NO_OPTION,
                                       JOptionPane.WARNING_MESSAGE, null, options, options[0]) != 0) {

        // notifying listeners
        fireValueUpdated(new UpdateEvent(this, struct));

        return true;
      }
    }
    text = textArea.getText();
    int index = text.indexOf((int)'\n');
    while (index != -1) {
      text = text.substring(0, index) + '\r' + text.substring(index);
      index = text.indexOf((int)'\n', index + 2);
    }
    bUpdate.setEnabled(false);

    // notifying listeners
    fireValueUpdated(new UpdateEvent(this, struct));

    return true;
  }

// --------------------- End Interface Editable ---------------------


//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------


// --------------------- Begin Interface ItemListener ---------------------

  @Override
  public void itemStateChanged(ItemEvent event)
  {
    ButtonPopupMenu bpmErrors = (ButtonPopupMenu)buttonPanel.getControlByType(CtrlErrors);
    ButtonPopupMenu bpmWarnings = (ButtonPopupMenu)buttonPanel.getControlByType(CtrlWarnings);
    String selected = "";
    if (event.getSource() == bpmErrors)
      selected = bpmErrors.getSelectedItem().getText();
    else if (event.getSource() == bpmWarnings)
      selected = bpmWarnings.getSelectedItem().getText();
    int index = selected.indexOf(": ");
    int line = Integer.parseInt(selected.substring(0, index));
    highlightLine(line);
  }

// --------------------- End Interface ItemListener ---------------------


// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    off.write(os);
    len.write(os);
  }

// --------------------- End Interface Writeable ---------------------

// --------------------- Begin Interface Readable ---------------------

  @Override
  public int read(byte[] buffer, int offset)
  {
    off = new DecNumber(buffer, offset, 4, DLG_CODE_OFFSET);
    len = new DecNumber(buffer, offset + 4, 4, DLG_CODE_LENGTH);
    return offset + getSize();
  }

// --------------------- End Interface Readable ---------------------

// --------------------- Begin Interface IsTextual ---------------------

  @Override
  public String getText()
  {
    return text;
  }

// --------------------- End Interface IsTextual ---------------------

  @Override
  public String toString()
  {
    return getText();
  }

  public void addFlatList(List<StructEntry> flatList)
  {
    flatList.add(off);
    flatList.add(len);
    try {
      TextString ts = new TextString(text.getBytes(), 0, len.getValue(), DLG_CODE_TEXT);
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

  public int getTextOffset()
  {
    return off.getValue();
  }

  public int updateOffset(int offs)
  {
    off.setValue(offs);
    len.setValue(text.length());
    return len.getValue();
  }

  public void writeString(OutputStream os) throws IOException
  {
    FileWriterNI.writeString(os, text, len.getValue());
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

