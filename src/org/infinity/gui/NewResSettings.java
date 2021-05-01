// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

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
import java.util.EnumMap;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.infinity.icon.Icons;
import org.infinity.resource.Profile;
import org.infinity.util.StringTable;

public final class NewResSettings extends NewAbstractSettings implements KeyListener
{
  private enum GameType { UNKNOWN, BG2, IWD, IWD2, IWDEE }

  private static final EnumMap<GameType, Vector<StrrefItem>> STRREF_ITEM  =
      new EnumMap<>(GameType.class);
  static {
    Vector<StrrefItem> list;
    // creating maps for unknown, BG2, IWD and IWD2
    // initializing 'unknown' items
    STRREF_ITEM.put(GameType.UNKNOWN, (list = new Vector<>()));
    list.add(new StrrefItem(-1,    "User-defined biography"));
    // initializing BG2 items
    STRREF_ITEM.put(GameType.BG2, (list = new Vector<>()));
    list.add(new StrrefItem(-1,    "User-defined biography"));
    list.add(new StrrefItem(33347, "Biography of the protagonist"));
    list.add(new StrrefItem(15882, "Biography of a generic NPC"));
    // initializing IWD items
    STRREF_ITEM.put(GameType.IWD, (list = new Vector<>()));
    list.add(new StrrefItem(-1,    "User-defined biography"));
    list.add(new StrrefItem(19423, "Biography of a fighter"));
    list.add(new StrrefItem(19429, "Biography of a ranger"));
    list.add(new StrrefItem(19427, "Biography of a paladin"));
    list.add(new StrrefItem(19422, "Biography of a cleric"));
    list.add(new StrrefItem(19421, "Biography of a druid"));
    list.add(new StrrefItem(19430, "Biography of a mage"));
    list.add(new StrrefItem(19428, "Biography of a thief"));
    list.add(new StrrefItem(19425, "Biography of a bard"));
    // initializing IWD2 items
    STRREF_ITEM.put(GameType.IWD2, (list = new Vector<>()));
    list.add(new StrrefItem(-1,    "User-defined biography"));
    list.add(new StrrefItem(27862, "Biography of a barbarian"));
    list.add(new StrrefItem(19425, "Biography of a bard"));
    list.add(new StrrefItem(19422, "Biography of a cleric"));
    list.add(new StrrefItem(19421, "Biography of a druid"));
    list.add(new StrrefItem(19423, "Biography of a fighter"));
    list.add(new StrrefItem(27860, "Biography of a monk"));
    list.add(new StrrefItem(19427, "Biography of a paladin"));
    list.add(new StrrefItem(19429, "Biography of a ranger"));
    list.add(new StrrefItem(19428, "Biography of a rogue"));
    list.add(new StrrefItem(27863, "Biography of a sorcerer"));
    list.add(new StrrefItem(19430, "Biography of a wizard"));
    // initializing IWDEE items
    STRREF_ITEM.put(GameType.IWDEE, (list = new Vector<>()));
    list.add(new StrrefItem(-1,    "User-defined biography"));
    list.add(new StrrefItem(19423, "Biography of a fighter"));
    list.add(new StrrefItem(19429, "Biography of a ranger"));
    list.add(new StrrefItem(19427, "Biography of a paladin"));
    list.add(new StrrefItem(19422, "Biography of a cleric"));
    list.add(new StrrefItem(19421, "Biography of a druid"));
    list.add(new StrrefItem(19430, "Biography of a mage"));
    list.add(new StrrefItem(19428, "Biography of a thief"));
    list.add(new StrrefItem(19425, "Biography of a bard"));
    list.add(new StrrefItem(40284, "Biography of a sorcerer"));
    list.add(new StrrefItem(40273, "Biography of a monk"));
    list.add(new StrrefItem(40276, "Biography of a barbarian"));
  }

  private JComboBox<StrrefItem> cbStrref;
  private JButton updateButton;
  private GameType gameType;   // 0=unknown, 1=BG2, 2=IWD, 3=IWD2
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

    cbStrref = new JComboBox<>(STRREF_ITEM.get(gameType));
    cbStrref.addKeyListener(this);
    lastStrref = -1;

    JLabel strrefLabel = new JLabel("Select template:");
    strrefLabel.setLabelFor(cbStrref);
    strrefLabel.setDisplayedMnemonic(KeyEvent.VK_S);

    updateButton = new JButton("Update text", Icons.getIcon(Icons.ICON_REFRESH_16));
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
    switch (Profile.getGame()) {
      case BG2SoA:
      case BG2ToB:
      case BG1EE:
      case BG1SoD:
      case BG2EE:
      case PSTEE:
      case EET:
        gameType = GameType.BG2;
        break;
      case IWD:
      case IWDHoW:
      case IWDHowTotLM:
        gameType = GameType.IWD;
        break;
      case IWD2:
        gameType = GameType.IWD2;
        break;
      case IWDEE:
        gameType = GameType.IWDEE;
        break;
      default:
        gameType = GameType.UNKNOWN;
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
        desc = newText.replace("\r", "");    // not sure if CR is supported
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
      if (stringId >= 0 && stringId < StringTable.getNumEntries())
        return StringTable.getStringRef(stringId);
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
