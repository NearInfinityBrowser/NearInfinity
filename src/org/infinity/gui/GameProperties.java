// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.infinity.NearInfinity;
import org.infinity.icon.Icons;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.util.io.FileEx;
import org.infinity.util.tuples.Couple;

/**
 * Display verbose information about the currently selected game.
 */
public final class GameProperties extends ChildFrame implements ActionListener
{
  private static final EnumMap<Profile.Key, String> RES_TYPES = new EnumMap<>(Profile.Key.class);

  static {
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_2DA, "2DA V1.0");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_ACM, "ACM");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_ARE_V10, "ARE V1.0");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_ARE_V91, "ARE V9.1");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_BAM_V1, "BAM V1");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_BAM_V1_ALPHA, "BAM V1+Alpha");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_BAMC_V1, "BAMC V1");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_BAM_V2, "BAM V2");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_BCS, "BCS");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_BIFF, "BIFF V1");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_BIFC, "BIFC V1.0");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_BIF, "BIF V1.0");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_BIK, "BIK");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_BIO, "BIO");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_BMP_PAL, "BMP (8-bit)");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_BMP_ALPHA, "BMP (32-bit)");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_CHR_V10, "CHR V1.0");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_CHR_V20, "CHR V2.0");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_CHR_V21, "CHR V2.1");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_CHR_V22, "CHR V2.2");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_CHU, "CHU V1");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_CRE_V10, "CRE V1.0");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_CRE_V12, "CRE V1.2");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_CRE_V22, "CRE V2.2");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_CRE_V90, "CRE V9.0");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_DLG, "DLG V1.0");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_EFF, "EFF V2.0");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_FNT, "FNT");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_GAM_V11, "GAM V1.1");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_GAM_V20, "GAM V2.0");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_GAM_V21, "GAM V2.1");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_GAM_V22, "GAM V2.2");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_GLSL, "GLSL");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_GUI, "GUI");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_IDS, "IDS V1.0");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_INI, "INI");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_ITM_V10, "ITM V1.0");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_ITM_V11, "ITM V1.1");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_ITM_V20, "ITM V2.0");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_KEY, "KEY V1");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_LUA, "LUA");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_MAZE, "MAZE");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_MENU, "MENU");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_MOS_V1, "MOS V1");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_MOSC_V1, "MOSC V1");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_MOS_V2, "MOS V2");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_MUS, "MUS");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_MVE, "MVE");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_OGG, "OGG");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_PLT, "PLT V1");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_PNG, "PNG");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_PRO, "PRO V1.0");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_PVRZ, "PVRZ");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_RES, "RES");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_SAV, "SAV V1.0");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_SPL_V1, "SPL V1.0");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_SPL_V2, "SPL V2.0");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_SQL, "SQL");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_SRC_PST, "SRC (PS:T)");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_SRC_IWD2, "SRC (IWD2)");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_STO_V10, "STO V1.0");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_STO_V11, "STO V1.1");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_STO_V90, "STO V9.0");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_TIS_V1, "TIS (Tiled)");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_TIS_V2, "TIS (PVRZ)");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_TLK, "TLK V1");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_TO_V1, "TOH/TOT");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_TO_V2, "TOH V2");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_TTF, "TTF");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_VAR, "VAR");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_VEF, "VEF");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_VVC, "VVC V1.0");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_WAV, "WAV");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_WAVC, "WAVC V1.0");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_WBM, "WBM");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_WED, "WED V1.3");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_WFX, "WFX V1.0");
    RES_TYPES.put(Profile.Key.IS_SUPPORTED_WMP, "WMP V1.0");
  }

  private final JButton bClose = new JButton("Close");
  private final JButton bEdit = new JButton("Edit...");

  public GameProperties(Window owner)
  {
    super("Game Properties", true);
    init();
  }

  //--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bClose) {
      setVisible(false);
    } else if (event.getSource() == bEdit) {
      NearInfinity.getInstance().editGameIni(this);
    }
  }

  //--------------------- End Interface ActionListener ---------------------

  private void init()
  {
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setLayout(new BorderLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    List<Couple<JComponent, JComponent>> listControls = new ArrayList<>();
    JLabel l;
    JTextField tf;
    String s;

    // Entry: game type
    s = Profile.getProperty(Profile.Key.GET_GAME_TITLE);
    if ((Boolean)Profile.getProperty(Profile.Key.IS_FORCED_GAME)) {
      s = s + " (enforced)";
    }
    l = new JLabel("Game type:");
    tf = createReadOnlyField(s, true);
    listControls.add(Couple.with(l, tf));

    // Entry: profile name
    s = Profile.getProperty(Profile.Key.GET_GAME_DESC);
    l = new JLabel("Profile name:");
    tf = createReadOnlyField((s != null) ? s : "n/a", true);
    listControls.add(Couple.with(l, tf));

    // Entry: game folder
    s = (Profile.getGameRoot()).toString();
    l = new JLabel("Game folder:");
    tf = createReadOnlyField(s, true);
    listControls.add(Couple.with(l, tf));
    if (Profile.isEnhancedEdition()) {
      // Entry: home folder
      s = Profile.getHomeRoot().toString();
      l = new JLabel("Home folder:");
      tf = createReadOnlyField(s, true);
      listControls.add(Couple.with(l, tf));

      // Entry: detected DLC
      List<Path> dlcPaths = Profile.getProperty(Profile.Key.GET_GAME_DLC_FOLDERS_AVAILABLE);
      if (dlcPaths != null && !dlcPaths.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        for (final Path dlcPath: dlcPaths) {
          if (sb.length() > 0) {
            sb.append("; ");
          }
          sb.append(dlcPath.getFileSystem().toString());
        }
        l = new JLabel("DLC archives:");
        tf = createReadOnlyField(sb.toString(), true);
        listControls.add(Couple.with(l, tf));
      }

      // Entry: available languages
      List<String> languages = Profile.getProperty(Profile.Key.GET_GAME_LANG_FOLDER_NAMES_AVAILABLE);
      StringBuilder sb = new StringBuilder();
      s = ResourceFactory.autodetectGameLanguage(Profile.getProperty(Profile.Key.GET_GAME_INI_FILE));
      if (s != null) {
        sb.append(String.format("Autodetect (%s)", getLanguageName(s)));
      }
      if (languages != null) {
        for (final String lang: languages) {
          if (lang != null && !lang.isEmpty()) {
            sb.append(String.format(", %s", getLanguageName(lang)));
          }
        }
      }
      l = new JLabel("Available languages:");
      tf = createReadOnlyField(sb.toString(), true);
      listControls.add(Couple.with(l, tf));

      // Entry: language
      s = getLanguageName(Profile.getProperty(Profile.Key.GET_GAME_LANG_FOLDER_NAME));
      l = new JLabel("Current language:");
      tf = createReadOnlyField(s, true);
      listControls.add(Couple.with(l, tf));
    }

    // Entry: Use female TLK file
    l = new JLabel("Uses female TLK file:");
    tf = createReadOnlyField(Boolean.toString((Profile.getProperty(Profile.Key.GET_GAME_DIALOGF_FILE) != null)), true);
    listControls.add(Couple.with(l, tf));

    // Entry: game's ini file
    l = new JLabel("Game's INI file:");
    JPanel pIni = new JPanel(new GridBagLayout());
    Path iniFile = Profile.getProperty(Profile.Key.GET_GAME_INI_FILE);
    s = (iniFile != null) ? iniFile.toString() : "n/a";
    tf = createReadOnlyField(s, true);
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pIni.add(tf, gbc);
    bEdit.setMargin(new Insets(2, 4, 2, 4));
    bEdit.addActionListener(this);
    bEdit.setEnabled(iniFile != null && FileEx.create(iniFile).isFile());
    gbc = ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pIni.add(bEdit, gbc);
    listControls.add(Couple.with(l, pIni));

    // adding controls from listControls to dialog
    JPanel pFixed = new JPanel(new GridBagLayout());
    int row = 0;
    for (final Iterator<Couple<JComponent, JComponent>> iter = listControls.iterator(); iter.hasNext();) {
      Couple<JComponent, JComponent> pair = iter.next();
      gbc = ViewerUtil.setGBC(gbc, 0, row, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
      pFixed.add(pair.getValue0(), gbc);
      gbc = ViewerUtil.setGBC(gbc, 1, row, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 0), 0, 0);
      pFixed.add(pair.getValue1(), gbc);
      row++;
    }

    // setting max. preferred width
    DisplayMode dm = NearInfinity.getInstance().getGraphicsConfiguration().getDevice().getDisplayMode();
    if (pFixed.getPreferredSize().width > (dm.getWidth() / 2)) {
      pFixed.setPreferredSize(new Dimension(dm.getWidth() / 2, pFixed.getPreferredSize().height));
    }

    // adding list of supported file formats
    FlowLayout flow = new FlowLayout(FlowLayout.LEFT, 8, 4);
    JPanel pSupportList = new JPanel(flow);
    pSupportList.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    List<JLabel> listTypes = new ArrayList<>();
    int maxWidth = 0, maxHeight = 0;
    // preparing entries
    for (Iterator<Profile.Key> iter = RES_TYPES.keySet().iterator(); iter.hasNext();) {
      Profile.Key key = iter.next();
      if (key != null) {
        JLabel label = createCheckLabel(key, RES_TYPES.get(key));
        maxWidth = Math.max(maxWidth, label.getPreferredSize().width);
        maxHeight = Math.max(maxHeight, label.getPreferredSize().height);
        listTypes.add(label);
      }
    }

    Collections.sort(listTypes, new Comparator<JLabel>() {
      @Override
      public int compare(JLabel o1, JLabel o2)
      {
        return o1.getText().compareToIgnoreCase(o2.getText());
      }
    });

    // setting preferred size to fit all entries
    int itemsPerRow = pFixed.getPreferredSize().width / (maxWidth + flow.getHgap());
    if (pFixed.getPreferredSize().width % (maxWidth + flow.getHgap()) > (maxWidth / 2)) {
      itemsPerRow++;  // prevent bigger gaps on the right side
    }
    itemsPerRow = Math.max(itemsPerRow, 4);
    int numCols = ((listTypes.size() + itemsPerRow - 1) / itemsPerRow);
    int panelWidth = itemsPerRow * (maxWidth + flow.getHgap()) + flow.getHgap() +
                     pSupportList.getInsets().left + pSupportList.getInsets().right;
    int panelHeight = numCols*(maxHeight + flow.getVgap()) + flow.getVgap() +
                      pSupportList.getInsets().top + pSupportList.getInsets().bottom;
    pSupportList.setPreferredSize(new Dimension(panelWidth, panelHeight));
    // adding entries to GUI
    for (Iterator<JLabel> iter = listTypes.iterator(); iter.hasNext();) {
      JLabel label = iter.next();
      Dimension d = label.getPreferredSize();
      d.width = maxWidth;
      label.setPreferredSize(d);
      pSupportList.add(label);
    }
    // creating actual panel for supported resource types
    JPanel pSupport = new JPanel(new GridBagLayout());
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pSupport.add(new JLabel("Supported resource types:"), gbc);
    gbc = ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pSupport.add(new JPanel(), gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 1, 2, 1, 1.0, 1.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.BOTH, new Insets(4, 0, 0, 0), 0, 0);
    pSupport.add(pSupportList, gbc);

    // adding close button
    JPanel pButton = new JPanel(new GridBagLayout());
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pButton.add(new JPanel(), gbc);
    bClose.addActionListener(this);
    gbc = ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pButton.add(bClose, gbc);
    gbc = ViewerUtil.setGBC(gbc, 2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pButton.add(new JPanel(), gbc);

    // putting everything together
    JPanel pMain = new JPanel(new GridBagLayout());
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 8), 0, 0);
    pMain.add(pFixed, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.BOTH, new Insets(8, 8, 0, 8), 0, 0);
    pMain.add(pSupport, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(8, 8, 8, 8), 0, 0);
    pMain.add(pButton, gbc);

    add(pMain, BorderLayout.CENTER);

    pack();
    setLocationRelativeTo(getOwner());
    bClose.requestFocusInWindow();
    getRootPane().setDefaultButton(bClose);
    setVisible(true);
  }

  // Returns the name of the language specified by the given language code
  private static String getLanguageName(String langCode)
  {
    if (langCode != null && langCode.matches("[a-z]{2}_[A-Z]{2}")) {
      String lang[] = langCode.split("_");
      if (lang.length >= 2) {
        String name = (new Locale(lang[0], lang[1])).getDisplayLanguage();
        if (name != null && !name.isEmpty()) {
          return name;
        }
      }
    }
    return langCode;
  }

  // Creates a read-only text field, optionally with visible caret
  private static JTextField createReadOnlyField(String text, boolean showCaret)
  {
    JTextField tf = new JTextField();
    if (showCaret) {
      tf.addFocusListener(new FocusListener() {
        @Override
        public void focusLost(FocusEvent e)
        {
          JTextField tf = (JTextField)e.getSource();
          tf.getCaret().setVisible(false);
        }
        @Override
        public void focusGained(FocusEvent e)
        {
          JTextField tf = (JTextField)e.getSource();
          tf.getCaret().setVisible(true);
        }
      });
    }
    tf.setEditable(false);
    tf.setFont(UIManager.getFont("Label.font"));
    if (text != null) {
      tf.setText(text);
      tf.setCaretPosition(0);
    }
    return tf;
  }

  // Creates a label with a graphical icon specifying checked or unchecked state
  private static JLabel createCheckLabel(Profile.Key key, String desc)
  {
    if (key != null && desc != null && Profile.getProperty(key) instanceof Boolean) {
      ImageIcon icon;
      if ((Boolean)Profile.getProperty(key)) {
        icon = Icons.getIcon(Icons.ICON_CHECK_16);
      } else {
        icon = Icons.getIcon(Icons.ICON_CHECK_NOT_16);
      }
      JLabel label = new JLabel(desc, icon, SwingConstants.LEFT);
      label.setFont(new Font(Font.MONOSPACED, label.getFont().getStyle(), label.getFont().getSize()));
      return label;
    } else {
      return null;
    }
  }
}
