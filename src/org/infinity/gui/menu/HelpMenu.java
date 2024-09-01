// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.menu;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;

import org.infinity.NearInfinity;
import org.infinity.gui.UrlBrowser;
import org.infinity.gui.ViewerUtil;
import org.infinity.gui.WindowBlocker;
import org.infinity.icon.Icons;
import org.infinity.updater.UpdateCheck;
import org.infinity.updater.UpdateInfo;
import org.infinity.updater.Updater;
import org.infinity.updater.UpdaterSettings;
import org.infinity.util.DataString;
import org.infinity.util.Misc;
import org.infinity.util.tuples.Couple;
import org.tinylog.Logger;

/**
 * Handles Help menu items for the {@link BrowserMenuBar}.
 */
public class HelpMenu extends JMenu implements BrowserSubMenu, ActionListener {
  private static final String WIKI_URL = "https://github.com/NearInfinityBrowser/NearInfinity/wiki";

  private final BrowserMenuBar menuBar;

  private final JMenuItem helpAbout;
  private final JMenuItem helpWiki;
  private final JMenuItem helpLicense;
  private final JMenuItem helpJOrbisLicense;
  private final JMenuItem helpFifeLicense;
  private final JMenuItem helpJHexViewLicense;
  private final JMenuItem helpMonteMediaLicense;
  private final JMenuItem helpJFontChooserLicense;
  private final JMenuItem helpApngWriterLicense;
  private final JMenuItem helpCommonMarkLicense;
  private final JMenuItem helpFlatLafLicense;
  private final JMenuItem helpTinyLogLicense;
  private final JMenuItem helpOracleLicense;
  private final JMenuItem helpUpdateSettings;
  private final JMenuItem helpUpdateCheck;

  public HelpMenu(BrowserMenuBar parent) {
    super("Help");
    setMnemonic(KeyEvent.VK_H);

    menuBar = parent;

    helpAbout = BrowserMenuBar.makeMenuItem("About Near Infinity", KeyEvent.VK_A, Icons.ICON_ABOUT_16.getIcon(), -1, this);
    add(helpAbout);

    helpWiki = BrowserMenuBar.makeMenuItem("Near Infinity Wiki", KeyEvent.VK_W, Icons.ICON_HELP_16.getIcon(), -1, this);
    helpWiki.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
    add(helpWiki);

    helpLicense = BrowserMenuBar.makeMenuItem("Near Infinity License", KeyEvent.VK_N, Icons.ICON_EDIT_16.getIcon(), -1, this);
    add(helpLicense);

    JMenu miscLicenses = new JMenu("Third-party licenses");
    miscLicenses.setMnemonic(KeyEvent.VK_T);
    add(miscLicenses);

    helpFlatLafLicense = BrowserMenuBar.makeMenuItem("FlatLaf License", KeyEvent.VK_A, Icons.ICON_EDIT_16.getIcon(), -1, this);
    miscLicenses.add(helpFlatLafLicense);

    helpCommonMarkLicense = BrowserMenuBar.makeMenuItem("CommonMark-Java License", KeyEvent.VK_A, Icons.ICON_EDIT_16.getIcon(), -1, this);
    miscLicenses.add(helpCommonMarkLicense);

    helpApngWriterLicense = BrowserMenuBar.makeMenuItem("APNG Writer License", KeyEvent.VK_A, Icons.ICON_EDIT_16.getIcon(), -1, this);
    miscLicenses.add(helpApngWriterLicense);

    helpFifeLicense = BrowserMenuBar.makeMenuItem("Fifesoft License", KeyEvent.VK_F, Icons.ICON_EDIT_16.getIcon(), -1, this);
    miscLicenses.add(helpFifeLicense);

    helpJFontChooserLicense = BrowserMenuBar.makeMenuItem("JFontChooser License", KeyEvent.VK_C, Icons.ICON_EDIT_16.getIcon(), -1,
        this);
    miscLicenses.add(helpJFontChooserLicense);

    helpJHexViewLicense = BrowserMenuBar.makeMenuItem("JHexView License", KeyEvent.VK_H, Icons.ICON_EDIT_16.getIcon(), -1, this);
    miscLicenses.add(helpJHexViewLicense);

    helpJOrbisLicense = BrowserMenuBar.makeMenuItem("JOrbis License", KeyEvent.VK_J, Icons.ICON_EDIT_16.getIcon(), -1, this);
    miscLicenses.add(helpJOrbisLicense);

    helpMonteMediaLicense = BrowserMenuBar.makeMenuItem("Monte Media License", KeyEvent.VK_M, Icons.ICON_EDIT_16.getIcon(), -1,
        this);
    miscLicenses.add(helpMonteMediaLicense);

    helpOracleLicense = BrowserMenuBar.makeMenuItem("Oracle License", KeyEvent.VK_O, Icons.ICON_EDIT_16.getIcon(), -1, this);
    miscLicenses.add(helpOracleLicense);

    helpTinyLogLicense = BrowserMenuBar.makeMenuItem("tinylog License", KeyEvent.VK_T, Icons.ICON_EDIT_16.getIcon(), -1, this);
    miscLicenses.add(helpTinyLogLicense);

    addSeparator();

    helpUpdateSettings = BrowserMenuBar.makeMenuItem("Update settings...", KeyEvent.VK_S, null, -1, this);
    add(helpUpdateSettings);

    helpUpdateCheck = BrowserMenuBar.makeMenuItem("Check for updates", KeyEvent.VK_U, Icons.ICON_FIND_16.getIcon(), -1, this);
    add(helpUpdateCheck);
  }

  public boolean isUpdateMenuEnabled() {
    return helpUpdateCheck.isEnabled();
  }

  /**
   * Disables and hides, or enables and shows all Update-related menu entries from the Help menu,
   * depending on the specified parameter.
   */
  public void setUpdateMenuEnabled(boolean enable) {
    boolean checkSeparator = false;
    // We have to iterate through the whole menu, since JSeparator instances cannot be accessed directly
    for (int i = getMenuComponentCount() - 1; i >= 0; i--) {
      final Component c = getMenuComponent(i);
      if (c instanceof JMenuItem) {
        if (c == helpUpdateCheck) {
          c.setEnabled(enable);
          c.setVisible(enable);
        } else if (c == helpUpdateSettings) {
          c.setEnabled(enable);
          c.setVisible(enable);
          checkSeparator = true;
        }
      } else if (checkSeparator && c instanceof JSeparator) {
        c.setVisible(enable);
        break;
      }
    }
  }

  @Override
  public BrowserMenuBar getMenuBar() {
    return menuBar;
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == helpAbout) {
      displayAbout();
    } else if (event.getSource() == helpWiki) {
      UrlBrowser.openUrl(WIKI_URL);
    } else if (event.getSource() == helpLicense) {
      displayLicense("org/infinity/LICENSE.txt", "LGPL License");
    } else if (event.getSource() == helpJOrbisLicense) {
      displayLicense("org/infinity/JOrbis.License.txt", "LGPL License");
    } else if (event.getSource() == helpFifeLicense) {
      displayLicense("org/infinity/RSyntaxTextArea.License.txt", "BSD License");
    } else if (event.getSource() == helpJHexViewLicense) {
      displayLicense("org/infinity/JHexView.License.txt", "GPL License");
    } else if (event.getSource() == helpMonteMediaLicense) {
      displayLicense("org/infinity/MonteMedia.License.txt", "Creative Commons / LGPL License");
    } else if (event.getSource() == helpJFontChooserLicense) {
      displayLicense("org/infinity/JFontChooser.License.txt", "MIT License");
    } else if (event.getSource() == helpFlatLafLicense) {
      displayLicense("org/infinity/FlatLaf.License.txt", "Apache License");
    } else if (event.getSource() == helpApngWriterLicense) {
      displayLicense("org/infinity/apng-writer.License.txt", "BSD License");
    } else if (event.getSource() == helpCommonMarkLicense) {
      displayLicense("org/infinity/commonmark.License.txt", "BSD License");
    } else if (event.getSource() == helpOracleLicense) {
      displayLicense("org/infinity/Oracle.License.txt", "BSD License");
    } else if (event.getSource() == helpTinyLogLicense) {
      displayLicense("org/infinity/tinylog.License.txt", "Apache License");
    } else if (event.getSource() == helpUpdateSettings) {
      UpdaterSettings.showDialog(NearInfinity.getInstance());
    } else if (event.getSource() == helpUpdateCheck) {
      UpdateInfo info = null;
      try {
        WindowBlocker.blockWindow(NearInfinity.getInstance(), true);
        info = Updater.getInstance().loadUpdateInfo();
        if (info == null) {
          final String msg = "Unable to find update information.\n"
              + "Please make sure that your Update Settings have been configured correctly.";
          JOptionPane.showMessageDialog(NearInfinity.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
          return;
        }
        if (!Updater.isNewRelease(info.getRelease(), false)) {
          info = null;
        }
      } catch (Exception e) {
        Logger.error(e);
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Check for updates: " + e.getMessage(), "Error",
            JOptionPane.ERROR_MESSAGE);
        return;
      } finally {
        WindowBlocker.blockWindow(NearInfinity.getInstance(), false);
      }
      UpdateCheck.showDialog(NearInfinity.getInstance(), info);
    }
  }

  private void displayAbout() {
    // title string
    final String versionString = "Near Infinity " + NearInfinity.getVersion();
    // list of current links
    final List<DataString<String>> currentLinks = new ArrayList<DataString<String>>() {
      {
        add(DataString.with("Active branch", "https://github.com/Argent77/NearInfinity/"));
        add(DataString.with("Main branch", "https://github.com/NearInfinityBrowser/NearInfinity/"));
        add(DataString.with("Wiki page", WIKI_URL));
      }
    };
    // original author
    final String originalVersion = "From Near Infinity 1.32.1 beta 24";
    final String originalCopyright = "Copyright (\u00A9) 2001-2005 - Jon Olav Hauglid";
    // List of various contributors (sorted alphabetically)
    final List<Couple<String, String[]>> contributors2 = new ArrayList<Couple<String, String[]>>() {
      {
        add(new Couple<>("Maintainers and contributors",
            new String[] { "Argent77", "Bubb", "devSin", "Fredrik Lindgren (aka Wisp)", "FredSRichardson", "Mingun",
                "nbauma109", "Taimon", "Valerio Bigiani (aka The Bigg)", "VileRik", "winterheart" }));
        add(new Couple<>("Near Infinity logo/icon", new String[] { "Cuv", "Troodon80" }));
        add(new Couple<>("Many thanks to", new String[] { "Avenger", "CamDawg", "Galactygon", "Gwendolyne", "K4thos",
            "kjeron", "Luke", "lynx", "Sam.", "everyone else who helped to improve Near Infinity" }));
      }
    };
    // copyright message
    final List<String> copyNearInfinityText = new ArrayList<String>() {
      {
        add("This program is free and may be distributed according to the terms of ");
        add("the GNU Lesser General Public License.");
      }
    };
    // Third-party copyright messages
    final List<String> copyThirdPartyText = new ArrayList<String>() {
      {
        add("Most icons (\u00A9) eclipse.org - Common Public License.");
        add("RSyntaxTextArea (\u00A9) Fifesoft - Berkeley Software Distribution License.");
        add("Monte Media Library by Werner Randelshofer - GNU Lesser General Public License.");
        add("JOrbis (\u00A9) JCraft Inc. - GNU Lesser General Public License.");
        add("JHexView by Sebastian Porst - GNU General Public License.");
        add("CommonMark-Java (\u00A9) Atlassian Pty. Ltd. - BSD License.");
        add("FlatLaf (\u00A9) FormDev Software GmbH - Apache License.");
        add("tinylog 2 by Martin Winandy - Apache License.");
        add("APNG Writer by Weoulren - BSD License.");
      }
    };

    // Fixed elements
    final Font defaultfont = UIManager.getFont("Label.font");
    final Font font = defaultfont.deriveFont(Misc.getScaledValue(13.0f));
    final Font bigFont = defaultfont.deriveFont(Font.BOLD, Misc.getScaledValue(20.0f));
    final Font smallFont = defaultfont.deriveFont(Misc.getScaledValue(11.0f));

    GridBagConstraints gbc = new GridBagConstraints();

    // version
    JLabel lVersion = new JLabel(versionString);
    lVersion.setFont(bigFont);

    JPanel pLinks = new JPanel(new GridBagLayout());
    {
      int row = 0;
      // current links
      for (int i = 0; i < currentLinks.size(); i++, row++) {
        int top = (i > 0) ? 4 : 0;
        JLabel lTitle = new JLabel(currentLinks.get(i).getString() + ":");
        lTitle.setFont(font);
        String link = currentLinks.get(i).getData();
        JLabel lLink = ViewerUtil.createUrlLabel(link);
        lLink.setFont(font);
        gbc = ViewerUtil.setGBC(gbc, 0, row, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, new Insets(top, 0, 0, 0), 0, 0);
        pLinks.add(lTitle, gbc);
        gbc = ViewerUtil.setGBC(gbc, 1, row, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, new Insets(top, 4, 0, 0), 0, 0);
        pLinks.add(lLink, gbc);
      }

      // original author block
      JLabel label = new JLabel(originalVersion);
      label.setFont(font);
      gbc = ViewerUtil.setGBC(gbc, 0, row, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(16, 0, 0, 0), 0, 0);
      pLinks.add(label, gbc);
      row++;
      label = new JLabel(originalCopyright);
      label.setFont(font);
      gbc = ViewerUtil.setGBC(gbc, 0, row, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
      pLinks.add(label, gbc);
      row++;
    }

    // contributors
    JPanel pContrib = new JPanel(new GridBagLayout());
    {
      JLabel label = new JLabel("Credits:");
      label.setFont(font);
      gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 0, 0, 0), 0, 0);
      pContrib.add(label, gbc);

      StringBuilder sb = new StringBuilder();
      for (int i = 0, count = contributors2.size(); i < count; i++) {
        final Couple<String, String[]> entry = contributors2.get(i);
        // for (final Couple<String, String[]> entry: contributors2) {
        final String header = entry.getValue0() + ":\n";
        sb.append(header);
        final String[] sequence = entry.getValue1();
        for (int j = 0, len = sequence.length; j < len; j++) {
          if (j > 0) {
            if (j == len - 1) {
              if (len > 2) {
                sb.append(',');
              }
              sb.append(" and ");
            } else {
              sb.append(", ");
            }
          }
          sb.append(sequence[j]);
        }
        if (i < count - 1) {
          sb.append("\n\n");
        }
      }

      JTextArea editor = new JTextArea(6, 0);
      editor.setBackground(label.getBackground());
      editor.setBorder(BorderFactory.createEmptyBorder());
      editor.setWrapStyleWord(true);
      editor.setLineWrap(true);
      editor.setEditable(false);
      editor.setFocusable(false);
      editor.setText(sb.toString());
      editor.setCaretPosition(0);
      JScrollPane scroll = new JScrollPane(editor, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      scroll.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
      gbc = ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
          new Insets(2, 0, 0, 0), 0, 0);
      pContrib.add(scroll, gbc);
    }

    // Near Infinity license
    JPanel pLicense = new JPanel(new GridBagLayout());
    {
      int row = 0;
      JLabel label = new JLabel("Near Infinity license:");
      label.setFont(smallFont.deriveFont(Misc.getScaledValue(12.0f)));
      gbc = ViewerUtil.setGBC(gbc, 0, row, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 2, 0), 0, 0);
      pLicense.add(label, gbc);
      row++;

      for (String element : copyNearInfinityText) {
        label = new JLabel(element);
        label.setFont(smallFont);
        gbc = ViewerUtil.setGBC(gbc, 0, row, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
        pLicense.add(label, gbc);
        row++;
      }
    }

    // Additional licenses
    JPanel pMiscLicenses = new JPanel(new GridBagLayout());
    {
      int row = 0;
      JLabel label = new JLabel("Additional licenses:");
      label.setFont(smallFont.deriveFont(Misc.getScaledValue(12.0f)));
      gbc = ViewerUtil.setGBC(gbc, 0, row, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 2, 0), 0, 0);
      pMiscLicenses.add(label, gbc);
      row++;

      for (String element : copyThirdPartyText) {
        label = new JLabel(element);
        label.setFont(smallFont);
        gbc = ViewerUtil.setGBC(gbc, 0, row, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
        pMiscLicenses.add(label, gbc);
        row++;
      }
    }

    // putting all together
    JPanel panel = new JPanel(new GridBagLayout());
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    panel.add(lVersion, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
        GridBagConstraints.HORIZONTAL, new Insets(16, 0, 0, 0), 0, 0);
    panel.add(pLinks, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 2, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
        GridBagConstraints.HORIZONTAL, new Insets(16, 0, 0, 0), 0, 0);
    panel.add(pContrib, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 3, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
        GridBagConstraints.HORIZONTAL, new Insets(16, 0, 0, 0), 0, 0);
    panel.add(pLicense, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 4, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
        GridBagConstraints.HORIZONTAL, new Insets(16, 0, 0, 0), 0, 0);
    panel.add(pMiscLicenses, gbc);

    JOptionPane.showMessageDialog(NearInfinity.getInstance(), panel, "About Near Infinity",
        JOptionPane.INFORMATION_MESSAGE, Icons.ICON_APP_128.getIcon());
  }

  private void displayLicense(String classPath, String title) {
    JPanel panel = new JPanel(new BorderLayout());
    JTextPane tphelp = new JTextPane();
    tphelp.setFont(new Font("Monospaced", Font.PLAIN, Misc.getScaledValue(12)));
    tphelp.setEditable(false);
    tphelp.setMargin(new Insets(3, 3, 3, 3));
    panel.add(new JScrollPane(tphelp), BorderLayout.CENTER);
    panel.setPreferredSize(Misc.getScaledDimension(new Dimension(640, 480)));

    try {
      tphelp.setPage(ClassLoader.getSystemResource(classPath));
    } catch (IOException e) {
      Logger.error(e);
    }

    JOptionPane.showMessageDialog(NearInfinity.getInstance(), panel, title, JOptionPane.PLAIN_MESSAGE);
  }
}
