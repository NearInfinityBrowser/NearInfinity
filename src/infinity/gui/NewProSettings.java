// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

public final class NewProSettings extends NewAbstractSettings
{
  private static final String PRODESC[] = {"1 - No BAM", "2 - Single target", "3 - Area of effect"};

  private JComboBox<String> cbType;
  private ProConfig config;

  public NewProSettings(Window parent)
  {
    super(parent, "PRO settings");
    config = new ProConfig();
    initDialog(parent);
  }

  public NewProSettings(Window parent, int proType)
  {
    super(parent, "PRO settings");
    config = new ProConfig(proType);
    initDialog(parent);
  }

  public ProConfig getConfig()
  {
    return config;
  }

  protected void accept()
  {
    config.setProjectileType(cbType.getSelectedIndex() + 1);
    super.accept();
  }

  private void initDialog(Window parent)
  {
    cbType = new JComboBox<String>(PRODESC);
    cbType.setSelectedIndex(config.getProjectileType() - 1);

    JLabel label = new JLabel("Select projectile type:");
    label.setLabelFor(cbType);
    label.setDisplayedMnemonic(KeyEvent.VK_S);

    JPanel panel = new JPanel(new GridBagLayout());
    Container pane = getContentPane();
    pane.add(panel);
    GridBagConstraints gbc = new GridBagConstraints();

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
    buttonPanel.add(acceptButton());
    buttonPanel.add(rejectButton());

    gbc.insets = new Insets(10, 10, 3, 10);
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.fill = GridBagConstraints.NONE;
    panel.add(label, gbc);

    gbc.insets = new Insets(0, 10, 10, 10);
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(cbType, gbc);

    gbc.insets = new Insets(0, 5, 5, 5);
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.anchor = GridBagConstraints.SOUTHEAST;
    gbc.fill = GridBagConstraints.NONE;
    panel.add(buttonPanel, gbc);

    pack();
    setMinimumSize(getPreferredSize());
    setLocationRelativeTo(parent);
    setVisible(true);
  }

//-------------------------- INNER CLASSES --------------------------

  public class ProConfig
  {
    private int proType;    // field at offset 0x08

    public ProConfig()
    {
      super();
      setProjectileType(2);   // defaults to 'single target'
    }

    public ProConfig(int type)
    {
      super();
      setProjectileType(type);
    }

    public int getProjectileType()
    {
      return proType;
    }

    private void setProjectileType(int newType)
    {
      proType = newType;
      if (proType < 1)
        proType = 1;
      if (proType > 3)
        proType = 3;
    }
  }
}
