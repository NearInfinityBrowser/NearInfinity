// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.hexview;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.infinity.gui.ViewerUtil;

/**
 * Searches for either text or byte values in the current resource data.
 */
public class FindDataDialog extends JDialog implements ActionListener, ItemListener, DocumentListener
{
  /** The data type of the search string. */
  public enum Type { TEXT, BYTES }

  private String text;
  private byte[] bytes;
  private boolean retVal;

  private JCheckBox cbCaseSensitive;
  private JButton bOk, bCancel;
  private JTextField tfSearch;
  private JComboBox<String> cbType;

  public FindDataDialog(Window parent)
  {
    super(parent, "Find", Dialog.ModalityType.APPLICATION_MODAL);
    init(parent);
    text = "";
    bytes = new byte[0];
    retVal = false;
  }

  //--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == bOk || e.getSource() == bCancel) {
      retVal = (e.getSource() == bOk);
      if(retVal) {
        text = tfSearch.getText();
        bytes = parseBytes(getText());
      }
      setVisible(false);
    }
  }

  //--------------------- End Interface ActionListener ---------------------

  //--------------------- Begin Interface ItemListener ---------------------

  @Override
  public void itemStateChanged(ItemEvent e)
  {
    if (e.getSource() == cbType) {
      cbCaseSensitive.setEnabled(cbType.getSelectedIndex() == 0);
    }
  }

  //--------------------- End Interface ItemListener ---------------------

  //--------------------- Begin Interface DocumentListener ---------------------

  @Override
  public void insertUpdate(DocumentEvent e)
  {
    bOk.setEnabled(!tfSearch.getText().isEmpty());
  }

  @Override
  public void removeUpdate(DocumentEvent e)
  {
    bOk.setEnabled(!tfSearch.getText().isEmpty());
  }

  @Override
  public void changedUpdate(DocumentEvent e)
  {
  }

  //--------------------- End Interface DocumentListener ---------------------

  /** Displays a Find dialog and returns whether the Find action has been initiated. */
  public boolean find()
  {
    tfSearch.requestFocusInWindow();
    tfSearch.selectAll();
    setVisible(true);
    return retVal;
  }

  /** Returns whether to consider cases when searching text. */
  public boolean isCaseSensitive()
  {
    return cbCaseSensitive.isSelected();
  }

  /** Returns the selected data type. */
  public Type getDataType()
  {
    return (cbType.getSelectedIndex() == 0) ? Type.TEXT : Type.BYTES;
  }

  /**
   * Returns the unprocessed text string. Can be used as is for text type.
   * Use {@link #getBytes()} to return the parsed byte data.
   */
  public String getText()
  {
    return text;
  }

  /**
   * Returns the interpreted byte data from the search string if data type "Byte Values" is selected.
   * Returns an empty byte array otherwise.
   */
  public byte[] getBytes()
  {
    return bytes;
  }

  private void init(Window parent)
  {
    setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    // Options
    JPanel pOptions = new JPanel(new GridBagLayout());
    cbCaseSensitive = new JCheckBox("Case sensitive", false);
    cbCaseSensitive.setMnemonic('c');
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pOptions.add(cbCaseSensitive, gbc);

    // Buttons
    JPanel pButtons = new JPanel(new GridBagLayout());
    bOk = new JButton("OK");
    bOk.addActionListener(this);
    bOk.setEnabled(false);
    bCancel = new JButton("Cancel");
    bCancel.addActionListener(this);
    bOk.setPreferredSize(bCancel.getPreferredSize());
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pButtons.add(new JPanel(), gbc);
    gbc = ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0);
    pButtons.add(bOk, gbc);
    gbc = ViewerUtil.setGBC(gbc, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0);
    pButtons.add(bCancel, gbc);

    // putting all together
    JPanel pMain = new JPanel(new GridBagLayout());
    JLabel lSearch = new JLabel("Search for:", SwingConstants.RIGHT);
    JLabel lType = new JLabel("Datatype:", SwingConstants.RIGHT);
    tfSearch = new JTextField();
    tfSearch.getDocument().addDocumentListener(this);
    cbType = new JComboBox<>(new String[]{"Text String", "Hex Values"});
    cbType.addItemListener(this);
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pMain.add(lSearch, gbc);
    gbc = ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    pMain.add(tfSearch, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pMain.add(lType, gbc);
    gbc = ViewerUtil.setGBC(gbc, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(4, 8, 0, 0), 0, 0);
    pMain.add(cbType, gbc);

    gbc = ViewerUtil.setGBC(gbc, 0, 2, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    pMain.add(new JPanel(), gbc);
    gbc = ViewerUtil.setGBC(gbc, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0);
    pMain.add(pOptions, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 3, 3, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_END,
                            GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    pMain.add(pButtons, gbc);

    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_END,
                            GridBagConstraints.BOTH, new Insets(8, 8, 8, 8), 0, 0);
    add(pMain, gbc);

    // finalizing dialog content
    pack();
    Dimension d = getPreferredSize();
    setMinimumSize(new Dimension(d.width, d.height));
    setPreferredSize(new Dimension(d.width * 3 / 2, d.height));
    pack();
    setLocationRelativeTo(parent);

    // setting up shortcuts
    final InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    final ActionMap actionMap = getRootPane().getActionMap();
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Enter");
    actionMap.put("Enter", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        bOk.doClick();
      }
    });
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Escape");
    actionMap.put("Escape", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        bCancel.doClick();
      }
    });
  }

  // Attempts to parse useful byte values from the specified text string
  private byte[] parseBytes(String text)
  {
    List<Byte> list = new ArrayList<>();

    // parsing text string
    StringBuilder sb = new StringBuilder();
    for (int idx = 0; idx < text.length(); idx++) {
      char ch = text.charAt(idx);
      if (Character.digit(ch, 16) >= 0) {
        sb.append(ch);
      } else if (!Character.isWhitespace(ch)) {
        idx = text.length() - 1;  // skip to end
      }
      if (sb.length() == 2 || (idx+1 == text.length() && sb.length() > 0)) {
        try {
          int value = Integer.parseInt(sb.toString(), 16);
          list.add(Byte.valueOf((byte)value));
          sb.delete(0, sb.length());
        } catch (NumberFormatException e) {
          break;
        }
      }
    }

    // putting values into byte array
    byte[] retVal = new byte[list.size()];
    for (int i = 0; i < retVal.length; i++) {
      retVal[i] = list.get(i).byteValue();
    }

    return retVal;
  }
}
