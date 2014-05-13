// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import infinity.icon.Icons;
import infinity.resource.ResourceFactory;
import infinity.util.StringResource;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

public final class NewResSettings extends NewAbstractSettings implements KeyListener
{
  private static final Vector<Vector<StrrefItem>> STRREF_ITEM = new Vector<Vector<StrrefItem>>();
  static {
    // creating maps for unknown, BG2, IWD and IWD2
    STRREF_ITEM.add(new Vector<StrrefItem>());
    STRREF_ITEM.add(new Vector<StrrefItem>());
    STRREF_ITEM.add(new Vector<StrrefItem>());
    STRREF_ITEM.add(new Vector<StrrefItem>());
    // initializing 'unknown' items
    STRREF_ITEM.get(0).add(new StrrefItem(-1,    "User-defined biography"));
    // initializing BG2 items
    STRREF_ITEM.get(1).add(new StrrefItem(-1,    "User-defined biography"));
    STRREF_ITEM.get(1).add(new StrrefItem(33347, "Biography of the protagonist"));
    STRREF_ITEM.get(1).add(new StrrefItem(15882, "Biography of a generic NPC"));
    // initializing IWD items
    STRREF_ITEM.get(2).add(new StrrefItem(-1,    "User-defined biography"));
    STRREF_ITEM.get(2).add(new StrrefItem(19423, "Biography of a fighter"));
    STRREF_ITEM.get(2).add(new StrrefItem(19429, "Biography of a ranger"));
    STRREF_ITEM.get(2).add(new StrrefItem(19427, "Biography of a paladin"));
    STRREF_ITEM.get(2).add(new StrrefItem(19422, "Biography of a cleric"));
    STRREF_ITEM.get(2).add(new StrrefItem(19421, "Biography of a druid"));
    STRREF_ITEM.get(2).add(new StrrefItem(19430, "Biography of a mage"));
    STRREF_ITEM.get(2).add(new StrrefItem(19428, "Biography of a thief"));
    STRREF_ITEM.get(2).add(new StrrefItem(19425, "Biography of a bard"));
    // initializing IWD2 items
    STRREF_ITEM.get(3).add(new StrrefItem(-1,    "User-defined biography"));
    STRREF_ITEM.get(3).add(new StrrefItem(27862, "Biography of a barbarian"));
    STRREF_ITEM.get(3).add(new StrrefItem(19425, "Biography of a bard"));
    STRREF_ITEM.get(3).add(new StrrefItem(19422, "Biography of a cleric"));
    STRREF_ITEM.get(3).add(new StrrefItem(19421, "Biography of a druid"));
    STRREF_ITEM.get(3).add(new StrrefItem(19423, "Biography of a fighter"));
    STRREF_ITEM.get(3).add(new StrrefItem(27860, "Biography of a monk"));
    STRREF_ITEM.get(3).add(new StrrefItem(19427, "Biography of a paladin"));
    STRREF_ITEM.get(3).add(new StrrefItem(19429, "Biography of a ranger"));
    STRREF_ITEM.get(3).add(new StrrefItem(19428, "Biography of a rogue"));
    STRREF_ITEM.get(3).add(new StrrefItem(27863, "Biography of a sorcerer"));
    STRREF_ITEM.get(3).add(new StrrefItem(19430, "Biography of a wizard"));
  }

  private JComboBox cbStrref;
  private JButton updateButton;
  private int gameId;   // 0=unknown, 1=BG2, 2=IWD, 3=IWD2
  private int lastStrref;

  private InfinityTextArea taText;
  private ResConfig config;

  public NewResSettings(Window parent)
  {
    super(parent, "Biography settings");
    initGame();
    config = new ResConfig();
    initDialog(parent);
  }

  public NewResSettings(Window parent, String bio)
  {
    super(parent, "Biography settings");
    initGame();
    config = new ResConfig(bio);
    initDialog(parent);
  }

  @Override
  public ResConfig getConfig()
  {
    return config;
  }

  @Override
  protected void accept()
  {
    config.setText(taText.getText());
    super.accept();
  }

  private void initDialog(Window parent)
  {
    getRootPane().setDefaultButton(null);   // prevent accidental file creation

    cbStrref = new JComboBox(STRREF_ITEM.get(gameId));
    cbStrref.addKeyListener(this);
    lastStrref = -1;

    JLabel strrefLabel = new JLabel("Select template:");
    strrefLabel.setLabelFor(cbStrref);
    strrefLabel.setDisplayedMnemonic(KeyEvent.VK_S);

    updateButton = new JButton("Update text", Icons.getIcon("Refresh16.gif"));
    updateButton.setMnemonic(KeyEvent.VK_U);
    updateButton.addActionListener(this);

    taText = new InfinityTextArea(20, 80, true);
    taText.setWrapStyleWord(true);
    taText.setLineWrap(true);
    if (cbStrref.getSelectedItem() instanceof StrrefItem) {
      taText.setText(((StrrefItem)cbStrref.getSelectedItem()).getString());
      taText.setCaretPosition(0);
    }

    JPanel panel = new JPanel(new GridBagLayout());
    Container pane = getContentPane();
    pane.add(panel);
    GridBagConstraints gbc = new GridBagConstraints();

    JPanel strrefPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
    strrefPanel.add(strrefLabel);
    strrefPanel.add(cbStrref);
    strrefPanel.add(updateButton);

    JPanel textPanel = new JPanel(new BorderLayout());
    textPanel.add(new InfinityScrollPane(taText, true), BorderLayout.CENTER);

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
    buttonPanel.add(acceptButton());
    buttonPanel.add(rejectButton());

    gbc.insets = new Insets(5, 5, 0, 5);
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.anchor = GridBagConstraints.NORTHEAST;
    gbc.fill = GridBagConstraints.NONE;
    panel.add(strrefPanel, gbc);

    gbc.insets = new Insets(0, 10, 0, 10);
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.BOTH;
    panel.add(textPanel, gbc);

    gbc.insets = new Insets(0, 5, 5, 5);
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.anchor = GridBagConstraints.SOUTHEAST;
    gbc.fill = GridBagConstraints.NONE;
    panel.add(buttonPanel, gbc);

    pack();
    setMinimumSize(new Dimension(200, 100));
    setLocationRelativeTo(parent);
    taText.requestFocusInWindow();    // text area receives initial focus
    setVisible(true);
  }

  private void initGame()
  {
    switch (ResourceFactory.getGameID()) {
      case ResourceFactory.ID_BG2:
      case ResourceFactory.ID_BG2TOB:
      case ResourceFactory.ID_BGEE:
      case ResourceFactory.ID_BG2EE:
        gameId = 1;
        break;
      case ResourceFactory.ID_ICEWIND:
      case ResourceFactory.ID_ICEWINDHOW:
      case ResourceFactory.ID_ICEWINDHOWTOT:
        gameId = 2;
        break;
      case ResourceFactory.ID_ICEWIND2:
        gameId = 3;
        break;
      default:
        gameId = 0;
        break;
    }
  }

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == updateButton) {
      if (lastStrref == -1)
        config.setText(taText.getText());
      if (cbStrref.getSelectedItem() instanceof StrrefItem) {
        StrrefItem obj = (StrrefItem)cbStrref.getSelectedItem();
        taText.setText((obj.getStringId() == -1) ? config.getText() : obj.getString());
        taText.requestFocusInWindow();
        taText.setCaretPosition(0);
        lastStrref = obj.getStringId();
      } else
        taText.setText("");
    }
    super.actionPerformed(event);
  }

//--------------------- End Interface ActionListener ---------------------

//--------------------- Begin Interface KeyListener ---------------------

  @Override
  public void keyPressed(KeyEvent event)
  {
    if (event.getSource() == cbStrref && event.getKeyCode() == KeyEvent.VK_ENTER)
      updateButton.doClick();
  }

  @Override
  public void keyReleased(KeyEvent event)
  {
  }

  @Override
  public void keyTyped(KeyEvent event)
  {
  }

//---------------------- End Interface KeyListener ----------------------

//-------------------------- INNER CLASSES --------------------------

  public class ResConfig
  {
    private String desc;    // field at offset 0x08

    public ResConfig()
    {
      setText("");
    }

    public ResConfig(String newText)
    {
      setText(newText);
    }

    public String getText()
    {
      return desc;
    }

    private void setText(String newText)
    {
      if (newText != null)
        desc = newText.replaceAll("\r", "");    // not sure if CR is supported
      else
        desc = "";
    }
  }

  private static class StrrefItem
  {
    private final int stringId;
    private final String desc;
    private final String defaultString;

    public StrrefItem(int strref, String description)
    {
      this.stringId = strref;
      this.desc = description;
      this.defaultString = "";
    }

    public int getStringId()
    {
      return stringId;
    }

    public String getString()
    {
      if (StringResource.getMaxIndex() <= 0)    // required?
        StringResource.getStringRef(0);
      if (stringId >= 0 && stringId < StringResource.getMaxIndex())
        return StringResource.getStringRef(stringId);
      else
        return defaultString;
    }

    @Override
    public String toString()
    {
      return desc;
    }
  }
}
