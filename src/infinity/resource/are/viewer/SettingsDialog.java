// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import infinity.icon.Icons;
import infinity.resource.are.viewer.Settings;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Hashtable;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * A settings dialog for the area viewer.
 * @author argent77
 */
public class SettingsDialog extends JDialog
  implements ActionListener, ListSelectionListener
{
  private static final String[] AnimationFrames = new String[]{"Never",
                                                               "On mouse-over only",
                                                               "Always"};
  private static final String[] QualityItems = new String[]{"Choose optimal filtering automatically",
                                                            "Always use nearest neighbor filtering",
                                                            "Always use bilinear filtering"};
  private static final String[] LayerDesc = new String[]{"Actors", "Regions", "Entrances", "Containers",
                                                         "Ambient Sounds", "Ambient Sound Ranges",
                                                         "Doors", "Background Animations", "Automap Notes",
                                                         "Spawn Points", "Map Transitions", "Projectile Traps",
                                                         "Door Polygons", "Wall Polygons" };

  private DefaultListModel modelLayers;
  private JList listLayers;
  private JButton bUp, bDown, bDefaultOrder;
  private JComboBox cbFrames, cbQualityMap, cbQualityAnim;
  private JCheckBox cbStoreSettings;
  private JButton bDefaultSettings, bCancel, bOK;
  private JSpinner sOverlaysFps, sAnimationsFps;
  private JSlider sMiniMapAlpha;
  private boolean settingsChanged;

  public SettingsDialog(Window owner)
  {
    super(owner, "Area viewer settings", Dialog.ModalityType.DOCUMENT_MODAL);
    init();
  }

  public boolean settingsChanged()
  {
    return settingsChanged;
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bUp) {
      layerUp();
    } else if (event.getSource() == bDown) {
      layerDown();
    } else if (event.getSource() == bDefaultOrder) {
      resetLayerOrder();
    } else if (event.getSource() == bDefaultSettings) {
      resetDialogSettings();
    } else if (event.getSource() == bCancel) {
      setVisible(false);
    } else if (event.getSource() == bOK) {
      updateSettings();
      setVisible(false);
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent event)
  {
    if (event.getSource() == listLayers) {
      updateLayerButtons();
    }
  }

  // --------------------- End Interface ListSelectionListener ---------------------

  // Applies dialog settings to the global Settings entries
  private void updateSettings()
  {
    for (int i = 0; i < modelLayers.size(); i++) {
      LayerEntry lt = (LayerEntry)modelLayers.get(i);
      Settings.ListLayerOrder.set(i, lt.getLayer());
    }

    Settings.ShowFrame = cbFrames.getSelectedIndex();

    Settings.InterpolationMap = cbQualityMap.getSelectedIndex();
    Settings.InterpolationAnim = cbQualityAnim.getSelectedIndex();

    Settings.FrameRateOverlays = (Double)sOverlaysFps.getValue();
    Settings.FrameRateAnimations = (Double)sAnimationsFps.getValue();

    Settings.MiniMapAlpha = (double)sMiniMapAlpha.getValue() / 100.0;

    Settings.StoreVisualSettings = cbStoreSettings.isSelected();

    settingsChanged = true;
  }

  // Resets layer order
  private void resetLayerOrder()
  {
    // filling list with layer entries
    int curIndex = listLayers.getSelectedIndex();
    modelLayers.clear();
    List<ViewerConstants.LayerStackingType> list = Settings.getDefaultLayerOrder();
    for (int i = 0; i < list.size(); i++) {
      ViewerConstants.LayerStackingType layer = list.get(i);
      String desc = LayerDesc[Settings.getLayerStackingTypeIndex(layer)];
      modelLayers.addElement(new LayerEntry(layer, desc));
    }
    list.clear();
    list = null;
    listLayers.setSelectedIndex(curIndex);
    updateLayerButtons();
  }

  // Re-initializes dialog controls with global settings
  private void resetDialogSettings()
  {
    resetLayerOrder();

    cbFrames.setSelectedIndex(Settings.getDefaultShowFrame());

    cbQualityMap.setSelectedIndex(Settings.getDefaultInterpolationMap());
    cbQualityAnim.setSelectedIndex(Settings.getDefaultInterpolationAnim());

    sOverlaysFps.setValue(Double.valueOf(Settings.getDefaultFrameRateOverlays()));
    sAnimationsFps.setValue(Double.valueOf(Settings.getDefaultFrameRateAnimations()));

    sMiniMapAlpha.setValue((int)(Settings.getDefaultMiniMapAlpha()*100.0));

    cbStoreSettings.setSelected(Settings.getDefaultStoreVisualSettings());
  }

  private void layerUp()
  {
    int idx = listLayers.getSelectedIndex();
    if (idx > 0) {
      LayerEntry lt = (LayerEntry)modelLayers.get(idx);
      modelLayers.set(idx, modelLayers.get(idx-1));
      modelLayers.set(idx-1, lt);
      listLayers.setSelectedIndex(idx - 1);
      updateLayerButtons();
    }
  }

  private void layerDown()
  {
    int idx = listLayers.getSelectedIndex();
    if (idx < modelLayers.size() - 1) {
      LayerEntry lt = (LayerEntry)modelLayers.get(idx);
      modelLayers.set(idx, modelLayers.get(idx+1));
      modelLayers.set(idx+1, lt);
      listLayers.setSelectedIndex(idx+1);
      updateLayerButtons();
    }
  }

  private void updateLayerButtons()
  {
    bUp.setEnabled(listLayers.getSelectedIndex() > 0);
    bDown.setEnabled(listLayers.getSelectedIndex() < modelLayers.size() - 1);
  }

  private void init()
  {
    setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

    GridBagConstraints c = new GridBagConstraints();

    settingsChanged = false;

    // Initializing layer items order
    modelLayers = new DefaultListModel();
    listLayers = new JList(modelLayers);
    listLayers.setCellRenderer(new IndexedCellRenderer(1));
    listLayers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    listLayers.setBorder(BorderFactory.createLineBorder(getForeground()));
    // filling list with layer entries
    for (int i = 0; i < Settings.ListLayerOrder.size(); i++) {
      ViewerConstants.LayerStackingType layer = Settings.ListLayerOrder.get(i);
      String desc = LayerDesc[Settings.getLayerStackingTypeIndex(layer)];
      modelLayers.addElement(new LayerEntry(layer, desc));
    }
    Dimension d = listLayers.getPreferredSize();
    listLayers.setPreferredSize(new Dimension(d.width + 40, d.height + 4));
    listLayers.setSelectedIndex(0);
    listLayers.addListSelectionListener(this);

    bDefaultOrder = new JButton("Default stacking order");
    bDefaultOrder.addActionListener(this);

    JLabel lDummy = new JLabel();
    JTextArea taOrderNote = new JTextArea();
    taOrderNote.setEditable(false);
    taOrderNote.setWrapStyleWord(true);
    taOrderNote.setLineWrap(true);
    taOrderNote.setFont(lDummy.getFont());
    taOrderNote.setBackground(lDummy.getBackground());
    taOrderNote.setSelectionColor(lDummy.getBackground());
    taOrderNote.setSelectedTextColor(lDummy.getForeground());
    taOrderNote.setText("Note: Layers of higher priority are drawn on top of layers of lower priority.");

    JPanel pLayersArrows = new JPanel(new GridBagLayout());
    bUp = new JButton(Icons.getIcon("Up16.gif"));
    bUp.setMargin(new Insets(16, 2, 16, 2));
    bUp.addActionListener(this);
    bUp.setEnabled(listLayers.getSelectedIndex() > 0);
    bDown = new JButton(Icons.getIcon("Down16.gif"));
    bDown.setMargin(new Insets(16, 2, 16, 2));
    bDown.addActionListener(this);
    updateLayerButtons();
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pLayersArrows.add(bUp, c);
    c = setGBC(c, 0, 1, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pLayersArrows.add(bDown, c);

    JPanel pLayers = new JPanel(new GridBagLayout());
    pLayers.setBorder(BorderFactory.createTitledBorder("Stacking order of layered items: "));
    c = setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.BOTH, new Insets(4, 4, 0, 0), 0, 0);
    pLayers.add(listLayers, c);
    c = setGBC(c, 1, 0, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.VERTICAL, new Insets(4, 4, 0, 4), 0, 0);
    pLayers.add(pLayersArrows, c);
    c = setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0);
    pLayers.add(bDefaultOrder, c);
    c = setGBC(c, 1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.NONE, new Insets(4, 4, 0, 0), 0, 0);
    pLayers.add(new JPanel(), c);
    c = setGBC(c, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.BOTH, new Insets(4, 4, 4, 0), 0, 0);
    pLayers.add(taOrderNote, c);
    c = setGBC(c, 1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.NONE, new Insets(4, 4, 4, 0), 0, 0);
    pLayers.add(new JPanel(), c);

    // Initializing options
    // Background animation frame
    JPanel pShowFrame = new JPanel(new GridBagLayout());
    pShowFrame.setBorder(BorderFactory.createTitledBorder("Background animations: "));
    JLabel lFrames = new JLabel("Show frame:");
    cbFrames = new JComboBox(AnimationFrames);
    cbFrames.setSelectedIndex(Settings.ShowFrame);
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(4, 4, 4, 0), 0, 0);
    pShowFrame.add(lFrames, c);
    c = setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 8, 4, 4), 0, 0);
    pShowFrame.add(cbFrames, c);

    // Interpolation type
    JPanel pQuality = new JPanel(new GridBagLayout());
    pQuality.setBorder(BorderFactory.createTitledBorder("Visual quality: "));
    JLabel lQualityMap = new JLabel("Map tilesets:");
    cbQualityMap = new JComboBox(QualityItems);
    cbQualityMap.setSelectedIndex(Settings.InterpolationMap);
    JLabel lQualityAnim = new JLabel("Animations:");
    cbQualityAnim = new JComboBox(QualityItems);
    cbQualityAnim.setSelectedIndex(Settings.InterpolationAnim);
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(4, 4, 0, 0), 0, 0);
    pQuality.add(lQualityMap, c);
    c = setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 4), 0, 0);
    pQuality.add(cbQualityMap, c);
    c = setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(4, 4, 4, 0), 0, 0);
    pQuality.add(lQualityAnim, c);
    c = setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0);
    pQuality.add(cbQualityAnim, c);

    // Frame rates
    JPanel pFrameRates = new JPanel(new GridBagLayout());
    pFrameRates.setBorder(BorderFactory.createTitledBorder("Frame rates: "));
    JTextArea taFrameRatesNote = new JTextArea();
    taFrameRatesNote.setEditable(false);
    taFrameRatesNote.setWrapStyleWord(true);
    taFrameRatesNote.setLineWrap(true);
    taFrameRatesNote.setFont(lDummy.getFont());
    taFrameRatesNote.setBackground(lDummy.getBackground());
    taFrameRatesNote.setSelectionColor(lDummy.getBackground());
    taFrameRatesNote.setSelectedTextColor(lDummy.getForeground());
    taFrameRatesNote.setText("Caution: The area viewer may become less responsive on higher frame rates.");

    sOverlaysFps = new JSpinner(new SpinnerNumberModel(Settings.FrameRateOverlays, 1.0, 30.0, 0.5));
    d = sOverlaysFps.getPreferredSize();
    sOverlaysFps.setPreferredSize(new Dimension(d.width + 16, d.height));
    sAnimationsFps = new JSpinner(new SpinnerNumberModel(Settings.FrameRateAnimations, 1.0, 30.0, 0.5));
    d = sAnimationsFps.getPreferredSize();
    sAnimationsFps.setPreferredSize(new Dimension(d.width + 16, d.height));
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 4, 0), 0, 0);
    pFrameRates.add(new JLabel("Overlays:"), c);
    c = setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 8, 4, 0), 0, 0);
    pFrameRates.add(sOverlaysFps, c);
    c = setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 4, 0), 0, 0);
    pFrameRates.add(new JLabel("fps"), c);
    c = setGBC(c, 3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 16, 4, 0), 0, 0);
    pFrameRates.add(new JLabel("Animations:"), c);
    c = setGBC(c, 4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 8, 4, 0), 0, 0);
    pFrameRates.add(sAnimationsFps, c);
    c = setGBC(c, 5, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 4, 4), 0, 0);
    pFrameRates.add(new JLabel("fps"), c);
    c = setGBC(c, 0, 1, 6, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0);
    pFrameRates.add(taFrameRatesNote, c);

    // Minimap transparency
    JPanel pMiniMap = new JPanel(new GridBagLayout());
    pMiniMap.setBorder(BorderFactory.createTitledBorder("Mini map opacity: "));

    Hashtable<Integer, JLabel> table = new Hashtable<Integer, JLabel>();
    for (int i = 0; i <= 100; i+=25) {
      table.put(Integer.valueOf(i), new JLabel(String.format("%1$d%%", i)));
    }
    sMiniMapAlpha = new JSlider(0, 100, (int)(Settings.MiniMapAlpha*100.0));
    sMiniMapAlpha.setSnapToTicks(false);
    sMiniMapAlpha.setLabelTable(table);
    sMiniMapAlpha.setPaintLabels(true);
    sMiniMapAlpha.setMinorTickSpacing(5);
    sMiniMapAlpha.setMajorTickSpacing(25);
    sMiniMapAlpha.setPaintTicks(true);
    sMiniMapAlpha.setPaintTrack(true);
    c = setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 4, 4, 4), 0, 0);
    pMiniMap.add(sMiniMapAlpha, c);

    // Misc. settings
    JPanel pMisc = new JPanel(new GridBagLayout());
    pMisc.setBorder(BorderFactory.createTitledBorder("Misc. settings: "));
    cbStoreSettings = new JCheckBox("Remember all visual settings");
    cbStoreSettings.setSelected(Settings.StoreVisualSettings);
    c = setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0);
    pMisc.add(cbStoreSettings, c);

    // initializing dialog buttons
    JPanel pButtons = new JPanel(new GridBagLayout());
    bDefaultSettings = new JButton("Set defaults");
    bDefaultSettings.addActionListener(this);
    bCancel = new JButton("Cancel");
    bCancel.addActionListener(this);
    bOK = new JButton("OK");
    bOK.setPreferredSize(bCancel.getPreferredSize());
    bOK.addActionListener(this);
    c = setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pButtons.add(bDefaultSettings, c);
    c = setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pButtons.add(bOK, c);
    c = setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pButtons.add(bCancel, c);

    // putting options together
    JPanel pOptions = new JPanel(new GridBagLayout());
    c = setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pOptions.add(pShowFrame, c);
    c = setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pOptions.add(pQuality, c);
    c = setGBC(c, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pOptions.add(pFrameRates, c);
    c = setGBC(c, 0, 3, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pOptions.add(pMiniMap, c);
    c = setGBC(c, 0, 4, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pOptions.add(pMisc, c);
    c = setGBC(c, 0, 5, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START,
               GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    pOptions.add(new JPanel(), c);
    c = setGBC(c, 0, 6, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pOptions.add(pButtons, c);

    // putting all together
    JPanel pMain = new JPanel(new GridBagLayout());
    c = setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START,
               GridBagConstraints.BOTH, new Insets(8, 8, 8, 0), 0, 0);
    pMain.add(pLayers, c);
    c = setGBC(c, 1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START,
               GridBagConstraints.BOTH, new Insets(8, 8, 8, 8), 0, 0);
    pMain.add(pOptions, c);

    getContentPane().add(pMain, BorderLayout.CENTER);
    pack();
    setMinimumSize(getPreferredSize());
    setLocationRelativeTo(getOwner());

    // Registering close on ESC key
    getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), getRootPane());
    getRootPane().getActionMap().put(getRootPane(), new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent event)
      {
        setVisible(false);
      }
    });

    getRootPane().setDefaultButton(bOK);
    setVisible(true);
  }

  private static GridBagConstraints setGBC(GridBagConstraints gbc, int gridX, int gridY,
                                           int gridWidth, int gridHeight, double weightX, double weightY,
                                           int anchor, int fill, Insets insets, int iPadX, int iPadY)
  {
    if (gbc == null) gbc = new GridBagConstraints();

    gbc.gridx = gridX;
    gbc.gridy = gridY;
    gbc.gridwidth = gridWidth;
    gbc.gridheight = gridHeight;
    gbc.weightx = weightX;
    gbc.weighty = weightY;
    gbc.anchor = anchor;
    gbc.fill = fill;
    gbc.insets = (insets == null) ? new Insets(0, 0, 0, 0) : insets;
    gbc.ipadx = iPadX;
    gbc.ipady = iPadY;

    return gbc;
  }


//----------------------------- INNER CLASSES -----------------------------

  private static class IndexedCellRenderer extends DefaultListCellRenderer
  {
    private int startIndex;

    public IndexedCellRenderer(int startIndex)
    {
      super();
      this.startIndex = startIndex;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus)
    {
      String template = "%1$0" +
                        String.format("%1$d", Integer.toString(list.getModel().getSize()).length()) +
                        "d - %2$s";
      return super.getListCellRendererComponent(list, String.format(template, index + startIndex, value),
                                                index, isSelected, cellHasFocus);
    }
  }


  // A simple wrapper for the list control to link layers with their respective description
  private static class LayerEntry
  {
    private ViewerConstants.LayerStackingType layer;
    private String desc;

    public LayerEntry(ViewerConstants.LayerStackingType layer, String description)
    {
      this.layer = layer;
      this.desc = description;
    }

    public ViewerConstants.LayerStackingType getLayer()
    {
      return layer;
    }

    public String getLayerDesc()
    {
      return desc;
    }

    @Override
    public String toString()
    {
      return desc;
    }
  }
}
