// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui.converter;

import infinity.gui.ButtonPopupMenu;
import infinity.gui.ViewerUtil;
import infinity.resource.ResourceFactory;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;

/**
 * An options dialog for the BAM converter.
 * @author argent77
 */
class BamOptionsDialog extends JDialog implements ActionListener, FocusListener
{
  private static final String PREFS_BAMVERSION    = "BCBamVersion";
  private static final String PREFS_PATH          = "BCPath";
  private static final String PREFS_AUTOCLEAR     = "BCAutoClear";
  private static final String PREFS_CLOSEONEXIT   = "BCCloseOnExit";
  private static final String PREFS_TRANSPARENCY  = "BCTransparencyThreshold";
  private static final String PREFS_COMPRESSBAM   = "BCCompressBam";
  private static final String PREFS_COMPRESSTYPE  = "BCCompressionType";
  private static final String PREFS_PVRZINDEX     = "BCPvrzIndex";

  // Default settings
  private static final int DefaultBamVersion            = ConvertToBam.VERSION_BAMV1;
  private static final String DefaultPath               = "";
  private static final boolean DefaultAutoClear         = true;
  private static final boolean DefaultCloseOnExit       = false;
  private static final int DefaultTransparencyThreshold = 5;    // in percent
  private static final boolean DefaultCompressBam       = false;
  private static final int DefaultCompressionType       = ConvertToBam.COMPRESSION_AUTO;
  private static final int DefaultPvrzIndex             = 1000;

  // Current settings
  private static boolean SettingsLoaded     = false;
  private static int BamVersion             = DefaultBamVersion;
  private static String Path                = DefaultPath;
  private static boolean AutoClear          = DefaultAutoClear;
  private static boolean CloseOnExit        = DefaultCloseOnExit;
  private static int TransparencyThreshold  = DefaultTransparencyThreshold;
  private static boolean CompressBam        = DefaultCompressBam;
  private static int CompressionType        = DefaultCompressionType;
  private static int PvrzIndex              = DefaultPvrzIndex;

  private JButton bOK, bCancel, bDefaults, bTransparencyHelp;
  private JComboBox cbBamVersion, cbCompressionType;
  private JCheckBox cbCloseOnExit, cbAutoClear, cbCompressBam;
  private JSpinner sTransparency, sPvrzIndex;
  private JTextField tfPath;
  private JMenuItem miPathSet, miPathClear;
  private ButtonPopupMenu bpmPath;


  /** Attempts to load stored settings from disk. Falls back to default values. */
  public static void loadSettings(boolean force)
  {
    if (!SettingsLoaded || force) {
      Preferences prefs = Preferences.userNodeForPackage(ConvertToBam.class);

      BamVersion = prefs.getInt(PREFS_BAMVERSION, DefaultBamVersion);
      Path = prefs.get(PREFS_PATH, DefaultPath);
      AutoClear = prefs.getBoolean(PREFS_AUTOCLEAR, DefaultAutoClear);
      CloseOnExit = prefs.getBoolean(PREFS_CLOSEONEXIT, DefaultCloseOnExit);
      TransparencyThreshold = prefs.getInt(PREFS_TRANSPARENCY, DefaultTransparencyThreshold);
      CompressBam = prefs.getBoolean(PREFS_COMPRESSBAM, DefaultCompressBam);
      CompressionType = prefs.getInt(PREFS_COMPRESSTYPE, DefaultCompressionType);
      PvrzIndex = prefs.getInt(PREFS_PVRZINDEX, DefaultPvrzIndex);

      validateSettings();
      SettingsLoaded = true;
    }
  }

  /** Stores the current settings on disk. */
  public static void saveSettings()
  {
    validateSettings();
    Preferences prefs = Preferences.userNodeForPackage(ConvertToBam.class);

    prefs.putInt(PREFS_BAMVERSION, BamVersion);
    prefs.put(PREFS_PATH, Path);
    prefs.putBoolean(PREFS_AUTOCLEAR, AutoClear);
    prefs.putBoolean(PREFS_CLOSEONEXIT, CloseOnExit);
    prefs.putInt(PREFS_TRANSPARENCY, TransparencyThreshold);
    prefs.putBoolean(PREFS_COMPRESSBAM, CompressBam);
    prefs.putInt(PREFS_COMPRESSTYPE, CompressionType);
    prefs.putInt(PREFS_PVRZINDEX, PvrzIndex);
  }

  // Makes sure that all settings are valid.
  private static void validateSettings()
  {
    BamVersion = Math.min(Math.max(BamVersion, ConvertToBam.VERSION_BAMV1), ConvertToBam.VERSION_BAMV2);
    if (Path == null) Path = DefaultPath;
    if (!Path.isEmpty() && !(new File(Path)).isDirectory()) Path = DefaultPath;
    TransparencyThreshold = Math.min(Math.max(TransparencyThreshold, 0), 100);
    CompressionType = Math.min(Math.max(CompressionType, ConvertToBam.COMPRESSION_AUTO), ConvertToBam.COMPRESSION_DXT5);
    PvrzIndex = Math.min(Math.max(PvrzIndex, 0), 99999);
  }

  /** Returns the default BAM version index. */
  public static int getBamVersion() { return BamVersion; }
  /** Returns the default path. */
  public static String getPath() { return Path; }
  /** Returns whether to automatically clear the current BAM after a successful conversion. */
  public static boolean getAutoClear() { return AutoClear; }
  /** Returns the default state for the "Close On Exit" checkbox. */
  public static boolean getCloseOnExit() { return CloseOnExit; }
  /** Returns the transparency threshold in percent. */
  public static int getTransparencyThreshold() { return TransparencyThreshold; }
  /** Returns the default state for "Compress BAM" checkbox (BAM v1). */
  public static boolean getCompressBam() { return CompressBam; }
  /** Returns the default compression type (BAM v2). */
  public static int getCompressionType() { return CompressionType; }
  /** Returns the default PVRZ index (BAM v2). */
  public static int getPvrzIndex() { return PvrzIndex; }


  public BamOptionsDialog(ConvertToBam parent)
  {
    super(parent, "Options", Dialog.ModalityType.DOCUMENT_MODAL);
    if (parent == null) {
      throw new NullPointerException();
    }
    init();
  }

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == miPathSet) {
      String path = tfPath.getText();
      if (path.isEmpty()) {
        path = ResourceFactory.getRootDir().toString();
      }
      File rootPath = ConvertToBam.getOpenPathName(this, "Select initial directory", path);
      if (rootPath != null) {
        tfPath.setText(rootPath.toString());
      }
    } else if (event.getSource() == miPathClear) {
      tfPath.setText("");
    } else if (event.getSource() == bTransparencyHelp) {
      String msg = "Defines a threshold that is used to determine whether a color is considered \"transparent\".\n\n" +
                   "Example:\n" +
                   "A value of 5% will treat any pixels with a transparency of 5% or higher as fully transparent.\n" +
                   "Pixels with less than 5% transparency will be treated as fully opaque.\n\n" +
                   "Note: This setting only affects Legacy BAM (v1) conversions.";
      JOptionPane.showMessageDialog(this, msg, "Help", JOptionPane.INFORMATION_MESSAGE);
    } else if (event.getSource() == bDefaults) {
      setDefaults();
    } else if (event.getSource() == bOK) {
      updateSettings();
      saveSettings();
      setVisible(false);
    } else if (event.getSource() == bCancel) {
      setVisible(false);
    }
  }

//--------------------- End Interface ActionListener ---------------------

//--------------------- Begin Interface FocusListener ---------------------

  @Override
  public void focusGained(FocusEvent event)
  {
  }

  @Override
  public void focusLost(FocusEvent event)
  {
    if (event.getSource() == tfPath) {
      String path = tfPath.getText();
      if (!path.isEmpty() && !(new File(path)).isDirectory()) {
        tfPath.setText(Path);
      }
    }
  }

//--------------------- End Interface FocusListener ---------------------

  private void init()
  {
    loadSettings(false);

    GridBagConstraints c = new GridBagConstraints();

    // initializing "General" panel
    JLabel l1 = new JLabel("Default BAM version:");
    JLabel l2 = new JLabel("Default root path:");
    cbBamVersion = new JComboBox(ConvertToBam.BamVersionItems);
    cbBamVersion.setSelectedIndex(getBamVersion());
    tfPath = new JTextField();
    tfPath.setText(getPath());
    tfPath.addFocusListener(this);
    miPathSet = new JMenuItem("Set...");
    miPathSet.addActionListener(this);
    miPathClear = new JMenuItem("Clear");
    miPathClear.addActionListener(this);
    bpmPath = new ButtonPopupMenu("...", new JMenuItem[]{miPathSet, miPathClear});
    cbCloseOnExit = new JCheckBox("Select \"Close dialog after conversion\" by default", getCloseOnExit());
    cbAutoClear = new JCheckBox("Automatically clear frames and cycles after conversion", getAutoClear());

    JPanel pGeneral = new JPanel(new GridBagLayout());
    pGeneral.setBorder(BorderFactory.createTitledBorder("General "));
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pGeneral.add(l1, c);
    c = ViewerUtil.setGBC(c, 1, 0, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 4, 0, 4), 0, 0);
    pGeneral.add(cbBamVersion, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 4, 0, 0), 0, 0);
    pGeneral.add(l2, c);
    c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0);
    pGeneral.add(tfPath, c);
    c = ViewerUtil.setGBC(c, 2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 4, 0, 4), 0, 0);
    pGeneral.add(bpmPath, c);
    c = ViewerUtil.setGBC(c, 0, 2, 3, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 4, 0, 4), 0, 0);
    pGeneral.add(cbAutoClear, c);
    c = ViewerUtil.setGBC(c, 0, 3, 3, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0);
    pGeneral.add(cbCloseOnExit, c);

    // initializing "Legacy BAM" panel
    l1 = new JLabel("Transparency threshold:");
    SpinnerNumberModel model = new SpinnerNumberModel(getTransparencyThreshold(), 0, 100, 1);
    sTransparency = new JSpinner(model);
    sTransparency.setEditor(new JSpinner.NumberEditor(sTransparency, "#'%'"));
    bTransparencyHelp = new JButton("?");
    bTransparencyHelp.setMargin(new Insets(2, 4, 2, 4));
    bTransparencyHelp.setToolTipText("About transparency threshold");
    bTransparencyHelp.addActionListener(this);
    cbCompressBam = new JCheckBox("Select \"Compress BAM\" by default", getCompressBam());
    JPanel pBamV1 = new JPanel(new GridBagLayout());
    pBamV1.setBorder(BorderFactory.createTitledBorder("Legacy BAM "));
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pBamV1.add(l1, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0);
    pBamV1.add(sTransparency, c);
    c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 4, 0, 4), 0, 0);
    pBamV1.add(bTransparencyHelp, c);
    c = ViewerUtil.setGBC(c, 0, 1, 3, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0);
    pBamV1.add(cbCompressBam, c);

    // initializing "PVRZ-based BAM" panel
    l1 = new JLabel("Default compression:");
    l2 = new JLabel("Default PVRZ index:");
    cbCompressionType = new JComboBox(ConvertToBam.CompressionItems);
    cbCompressionType.setSelectedIndex(getCompressionType());
    model = new SpinnerNumberModel(getPvrzIndex(), 0, 99999, 1);
    sPvrzIndex = new JSpinner(model);
    JPanel pBamV2 = new JPanel(new GridBagLayout());
    pBamV2.setBorder(BorderFactory.createTitledBorder("PVRZ-based BAM "));
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pBamV2.add(l1, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0);
    pBamV2.add(cbCompressionType, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 4, 4, 0), 0, 0);
    pBamV2.add(l2, c);
    c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0);
    pBamV2.add(sPvrzIndex, c);

    // initializing bottom button bar
    bDefaults = new JButton("Set defaults");
    bDefaults.addActionListener(this);
    bDefaults.setMargin(new Insets(4, bDefaults.getInsets().left, 4, bDefaults.getInsets().right));
    bCancel = new JButton("Cancel");
    bCancel.addActionListener(this);
    bCancel.setMargin(new Insets(4, bCancel.getInsets().left, 4, bCancel.getInsets().right));
    bOK = new JButton("OK");
    bOK.addActionListener(this);
    bOK.setMargin(new Insets(4, bOK.getInsets().left, 4, bOK.getInsets().right));
    bOK.setPreferredSize(bCancel.getPreferredSize());
    JPanel pButtons = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pButtons.add(bDefaults, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pButtons.add(bOK, c);
    c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pButtons.add(bCancel, c);

    // putting all together
    JPanel pAll = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 2, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.BOTH, new Insets(8, 8, 0, 8), 0, 0);
    pAll.add(pGeneral, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.BOTH, new Insets(8, 8, 4, 0), 0, 0);
    pAll.add(pBamV1, c);
    c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.BOTH, new Insets(8, 4, 4, 8), 0, 0);
    pAll.add(pBamV2, c);
    c = ViewerUtil.setGBC(c, 0, 2, 2, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(8, 8, 8, 8), 0, 0);
    pAll.add(pButtons, c);

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(pAll, BorderLayout.CENTER);
    pack();
    setResizable(false);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), getRootPane());
    getRootPane().getActionMap().put(getRootPane(), new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent event) { setVisible(false); }
    });
    setLocationRelativeTo(getOwner());
    setVisible(true);
  }

  // Applies the default settings to the dialog controls
  private void setDefaults()
  {
    cbBamVersion.setSelectedIndex(DefaultBamVersion);
    tfPath.setText(DefaultPath);
    cbAutoClear.setSelected(DefaultAutoClear);
    cbCloseOnExit.setSelected(DefaultCloseOnExit);
    sTransparency.setValue(Integer.valueOf(DefaultTransparencyThreshold));
    cbCompressBam.setSelected(DefaultCompressBam);
    cbCompressionType.setSelectedIndex(DefaultCompressionType);
    sPvrzIndex.setValue(Integer.valueOf(DefaultPvrzIndex));
  }

  // Fetches the values from the dialog controls
  private void updateSettings()
  {
    BamVersion = cbBamVersion.getSelectedIndex();
    Path = tfPath.getText();
    AutoClear = cbAutoClear.isSelected();
    CloseOnExit = cbCloseOnExit.isSelected();
    TransparencyThreshold = (Integer)sTransparency.getValue();
    CompressBam = cbCompressBam.isSelected();
    CompressionType = cbCompressionType.getSelectedIndex();
    PvrzIndex = (Integer)sPvrzIndex.getValue();
    validateSettings();
  }
}