// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ProgressMonitor;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

import org.infinity.NearInfinity;
import org.infinity.datatype.IdsBitmap;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.KitIdsBitmap;
import org.infinity.datatype.PriTypeBitmap;
import org.infinity.datatype.ProRef;
import org.infinity.datatype.ResourceBitmap;
import org.infinity.datatype.SecTypeBitmap;
import org.infinity.datatype.TextBitmap;
import org.infinity.gui.ButtonPopupWindow;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.ViewFrame;
import org.infinity.gui.ViewerUtil;
import org.infinity.gui.WindowBlocker;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractAbility;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.bcs.BcsResource;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.effects.BaseOpcode;
import org.infinity.resource.itm.Ability;
import org.infinity.resource.itm.ItmResource;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.other.VvcResource;
import org.infinity.resource.pro.ProAreaType;
import org.infinity.resource.pro.ProResource;
import org.infinity.resource.pro.ProSingleType;
import org.infinity.resource.spl.SplResource;
import org.infinity.resource.sto.StoResource;
import org.infinity.resource.ui.ResourceCellRenderer;
import org.infinity.resource.ui.ResourceListModel;
import org.infinity.util.Debugging;
import org.infinity.util.IdsMapEntry;
import org.infinity.util.Misc;
import org.infinity.util.SimpleListModel;
import org.infinity.util.Threading;
import org.infinity.util.io.StreamUtils;
import org.infinity.util.tuples.Couple;

public class SearchResource extends ChildFrame implements ActionListener, PropertyChangeListener, Runnable {
  private static final String[] OPTION_PANELS = { "ARE", "CRE", "EFF", "ITM", "PRO", "SPL", "STO", "VVC" };
  private static final String SET_OPTIONS_TEXT = "Set options...";
  private static final String[] SET_SHOW_HIDE_TEXT = { "Show options >>>", "Hide options <<<" };
  private static final String PROPERTY_OPTIONS = "NearInfinity.Options.IsEmpty";

  private final HashMap<String, OptionsBasePanel> mapOptionsPanel = new HashMap<>();
  private JPanel pFindOptions, pBottomBar;
  private JList<ResourceEntry> listResults;
  private JLabel lResults;
  private JButton bSearch;
  private JButton bInsertRef;
  private JButton bOpen;
  private JButton bOpenNew;
  private JComboBox<ObjectString> cbResourceType;
  private JToggleButton bShowHideOptions;
  private CardLayout clOptions;
  private CardLayout clBottomBar;
  private JProgressBar pbProgress;

  public SearchResource() {
    super("Extended search (deprecated)");
    (new SwingWorker<Void, Void>() {
      @Override
      public Void doInBackground() {
        try {
          init();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return null;
      }
    }).execute();
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == cbResourceType) {
      String ext = getCurrentResourceType();
      if (!ext.isEmpty()) {
        clOptions.show(pFindOptions, ext);
        pFindOptions.firePropertyChange(PROPERTY_OPTIONS, !bSearch.isEnabled(), mapOptionsPanel.get(ext).isEmpty());
      }
    } else if (event.getSource() == bShowHideOptions) {
      if (pFindOptions.isVisible()) {
        bShowHideOptions.setText(SET_SHOW_HIDE_TEXT[0]);
        bShowHideOptions.setSelected(false);
        pFindOptions.setVisible(false);
      } else {
        bShowHideOptions.setText(SET_SHOW_HIDE_TEXT[1]);
        bShowHideOptions.setSelected(true);
        pFindOptions.setVisible(true);
      }
    } else if (event.getSource() == bSearch) {
      if (!isOptionsEmpty()) {
        (new Thread(this)).start();
      } else {
        JOptionPane.showMessageDialog(this,
            String.format("No search parameters specified for \"%s\".", getCurrentResourceType()));
      }
    } else if (event.getSource() == bInsertRef) {
      Viewable viewable = NearInfinity.getInstance().getViewable();
      if (viewable == null || !(viewable instanceof BcsResource)) {
        JOptionPane.showMessageDialog(this, "No script displayed in the main window", "Error",
            JOptionPane.ERROR_MESSAGE);
        return;
      } else {
        final ResourceEntry entry = listResults.getSelectedValue();
        if (entry != null) {
          // TODO: Need method to get resource name without extension
          final String resname = entry.getResourceName().substring(0, entry.getResourceName().indexOf('.'));
          ((BcsResource) viewable).insertString('"' + resname + '"');
        }
      }
    } else if (event.getSource() == bOpen) {
      final ResourceEntry entry = listResults.getSelectedValue();
      if (entry != null) {
        NearInfinity.getInstance().showResourceEntry(entry);
      }
    } else if (event.getSource() == bOpenNew) {
      final ResourceEntry entry = listResults.getSelectedValue();
      if (entry != null) {
        new ViewFrame(this, ResourceFactory.getResource(entry));
      }
    }
  }

  @Override
  public void propertyChange(PropertyChangeEvent event) {
    if (event.getSource() == pFindOptions) {
      if (PROPERTY_OPTIONS.equals(event.getPropertyName())) {
        if (event.getNewValue() instanceof Boolean) {
          bSearch.setEnabled(!(Boolean) event.getNewValue());
        }
      }
    }
  }

  @Override
  public void run() {
    listResults.setListData(new ResourceEntry[0]);
    listResults.setEnabled(false);
    bInsertRef.setEnabled(false);
    bOpen.setEnabled(false);
    bOpenNew.setEnabled(false);
    lResults.setText("");

    String type = getCurrentResourceType();
    if (!type.isEmpty()) {
      // initializations
      List<ResourceEntry> resources = ResourceFactory.getResources(type);
      final Vector<ResourceEntry> found = new Vector<>();
      bSearch.setEnabled(false);
      pbProgress.setMinimum(0);
      pbProgress.setMaximum(resources.size());
      pbProgress.setValue(0);
      clBottomBar.show(pBottomBar, "progress");
      WindowBlocker blocker = new WindowBlocker(this);
      blocker.setBlocked(true);

      // executing search
      try {
        Debugging.timerReset();
        OptionsBasePanel panel = mapOptionsPanel.get(type);
        if (panel != null) {
          SearchOptions so = panel.getOptions();

          // using parallel jobs to speed up search
          try (final Threading threadPool = new Threading()) {
            for (ResourceEntry element : resources) {
              threadPool.submit(new SearchWorker(found, so, element));
            }

            // waiting for threads to finish
            threadPool.shutdown();
            try {
              threadPool.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          } catch (Exception e) {
            // ignored
          }

          // preparing results for output
          listResults.ensureIndexIsVisible(0);
          if (!found.isEmpty()) {
            Collections.sort(found);
            listResults.setListData(found);
            listResults.setEnabled(true);
            bInsertRef.setEnabled(true);
            bOpen.setEnabled(true);
            bOpenNew.setEnabled(true);
          }
          if (found.size() == 1) {
            lResults.setText(String.format("(%d match found)", found.size()));
          } else {
            lResults.setText(String.format("(%d matches found)", found.size()));
          }
        }
      } finally {
        Debugging.timerShow("Extended Search", Debugging.TimeFormat.MILLISECONDS);
        blocker.setBlocked(false);
        bSearch.setEnabled(true);
        clBottomBar.show(pBottomBar, "buttons");
      }
    }
  }

  /** Initialize dialog. */
  private void init() throws Exception {
    int progress = 1;
    ProgressMonitor pm = new ProgressMonitor(NearInfinity.getInstance(), "Initializing extended search...",
        "(This may take a while...)", 0, 10);
    pm.setMillisToDecideToPopup(0);
    pm.setMillisToPopup(0);
    pm.setProgress(progress++);
    try {
      setIconImage(Icons.ICON_FIND_16.getIcon().getImage());
      GridBagConstraints c = new GridBagConstraints();

      boolean effSupported = Profile.getProperty(Profile.Key.IS_SUPPORTED_EFF);
      boolean proSupported = Profile.getProperty(Profile.Key.IS_SUPPORTED_PRO);
      boolean vvcSupported = Profile.getProperty(Profile.Key.IS_SUPPORTED_VVC);
      int resTypeCount = 5;
      if (effSupported) {
        resTypeCount++;
      }
      if (proSupported) {
        resTypeCount++;
      }
      if (vvcSupported) {
        resTypeCount++;
      }

      // creating resource-specific options panels
      pm.setNote("Processing ARE...");
      mapOptionsPanel.put(OPTION_PANELS[0], new OptionsAREPanel(this));
      pm.setProgress(progress++);
      pm.setNote("Processing CRE...");
      mapOptionsPanel.put(OPTION_PANELS[1], new OptionsCREPanel(this));
      pm.setProgress(progress++);
      if (effSupported) {
        pm.setNote("Processing EFF...");
        mapOptionsPanel.put(OPTION_PANELS[2], new OptionsEFFPanel(this));
      }
      pm.setProgress(progress++);
      pm.setNote("Processing ITM...");
      mapOptionsPanel.put(OPTION_PANELS[3], new OptionsITMPanel(this));
      pm.setProgress(progress++);
      if (proSupported) {
        pm.setNote("Processing PRO...");
        mapOptionsPanel.put(OPTION_PANELS[4], new OptionsPROPanel(this));
      }
      pm.setProgress(progress++);
      pm.setNote("Processing SPL...");
      mapOptionsPanel.put(OPTION_PANELS[5], new OptionsSPLPanel(this));
      pm.setProgress(progress++);
      pm.setNote("Processing STO...");
      mapOptionsPanel.put(OPTION_PANELS[6], new OptionsSTOPanel(this));
      pm.setProgress(progress++);
      if (vvcSupported) {
        pm.setNote("Processing VVC...");
        mapOptionsPanel.put(OPTION_PANELS[7], new OptionsVVCPanel(this));
      }
      pm.setProgress(progress++);
      pm.setNote("Processing controls...");

      // creating Find section
      JLabel lResType = new JLabel("Resource type:");
      ObjectString[] resTypeList = new ObjectString[resTypeCount];
      resTypeCount = 0;
      resTypeList[resTypeCount++] = new ObjectString("Areas", OPTION_PANELS[0]);
      resTypeList[resTypeCount++] = new ObjectString("Creatures", OPTION_PANELS[1]);
      if (effSupported) {
        resTypeList[resTypeCount++] = new ObjectString("Effects", OPTION_PANELS[2]);
      }
      resTypeList[resTypeCount++] = new ObjectString("Items", OPTION_PANELS[3]);
      if (proSupported) {
        resTypeList[resTypeCount++] = new ObjectString("Projectiles", OPTION_PANELS[4]);
      }
      resTypeList[resTypeCount++] = new ObjectString("Spells", OPTION_PANELS[5]);
      resTypeList[resTypeCount++] = new ObjectString("Stores", OPTION_PANELS[6]);
      if (vvcSupported) {
        resTypeList[resTypeCount++] = new ObjectString("Visual Effects", OPTION_PANELS[7]);
      }

      cbResourceType = new JComboBox<>(resTypeList);
      cbResourceType.setSelectedIndex(0);
      cbResourceType.addActionListener(this);
      bSearch = new JButton("Search", Icons.ICON_FIND_16.getIcon());
      bSearch.setEnabled(false);
      bSearch.addActionListener(this);
      bShowHideOptions = new JToggleButton(SET_SHOW_HIDE_TEXT[1], true);
      bShowHideOptions.addActionListener(this);
      clOptions = new CardLayout();
      pFindOptions = new JPanel(clOptions);
      pFindOptions.setBorder(BorderFactory.createTitledBorder("Options: "));
      pFindOptions.add(mapOptionsPanel.get(OPTION_PANELS[0]), OPTION_PANELS[0]); // ARE
      pFindOptions.add(mapOptionsPanel.get(OPTION_PANELS[1]), OPTION_PANELS[1]); // CRE
      if (effSupported) {
        pFindOptions.add(mapOptionsPanel.get(OPTION_PANELS[2]), OPTION_PANELS[2]); // EFF
      }
      pFindOptions.add(mapOptionsPanel.get(OPTION_PANELS[3]), OPTION_PANELS[3]); // ITM
      if (proSupported) {
        pFindOptions.add(mapOptionsPanel.get(OPTION_PANELS[4]), OPTION_PANELS[4]); // PRO
      }
      pFindOptions.add(mapOptionsPanel.get(OPTION_PANELS[5]), OPTION_PANELS[5]); // SPL
      pFindOptions.add(mapOptionsPanel.get(OPTION_PANELS[6]), OPTION_PANELS[6]); // STO
      if (vvcSupported) {
        pFindOptions.add(mapOptionsPanel.get(OPTION_PANELS[7]), OPTION_PANELS[7]); // VVC
      }
      clOptions.show(pFindOptions, OPTION_PANELS[0]);
      pFindOptions.addPropertyChangeListener(PROPERTY_OPTIONS, this);
      JPanel pFindPanel = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 0, 0, 0), 0, 0);
      pFindPanel.add(lResType, c);
      c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 0), 0, 0);
      pFindPanel.add(cbResourceType, c);
      c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 8, 0, 0), 16, 0);
      pFindPanel.add(bSearch, c);
      c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 0, 0, 0), 0, 0);
      pFindPanel.add(new JPanel(), c);
      c = ViewerUtil.setGBC(c, 4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 8, 0, 0), 0, 0);
      pFindPanel.add(bShowHideOptions, c);
      c = ViewerUtil.setGBC(c, 0, 1, 5, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(8, 0, 0, 0), 0, 0);
      pFindPanel.add(pFindOptions, c);

      JPanel pFindMain = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
          new Insets(4, 4, 0, 4), 0, 0);
      pFindMain.add(pFindPanel, c);

      // creating Results section
      JLabel lResult = new JLabel("Result:");
      lResults = new JLabel("");
      listResults = new JList<>(new SimpleListModel<>());
      listResults.setCellRenderer(new ResourceCellRenderer());
      listResults.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      listResults.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent event) {
          // a double click causes the currently selected resource entry to be displayed
          if (event.getClickCount() == 2) {
            Rectangle cellRect = listResults.getCellBounds(listResults.getSelectedIndex(),
                listResults.getSelectedIndex());
            if (cellRect != null && event.getPoint() != null) {
              if (cellRect.contains(event.getPoint())) {
                actionPerformed(new ActionEvent(bOpen, 0, null));
              }
            }
          }
        }
      });
      JScrollPane scroll = new JScrollPane(listResults);
      JPanel pResult = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 0, 0, 0), 0, 0);
      pResult.add(lResult, c);
      c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 8, 0, 0), 0, 0);
      pResult.add(lResults, c);
      c = ViewerUtil.setGBC(c, 0, 1, 2, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
          new Insets(4, 0, 0, 0), 0, 0);
      pResult.add(scroll, c);

      JPanel pResultMain = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
          new Insets(4, 4, 4, 4), 0, 0);
      pResultMain.add(pResult, c);

      // creating bottom bar for buttons and progress
      bInsertRef = new JButton("Insert reference", Icons.ICON_PASTE_16.getIcon());
      bInsertRef.setMnemonic('r');
      bInsertRef.setEnabled(false);
      bInsertRef.addActionListener(this);
      bOpen = new JButton("Open", Icons.ICON_OPEN_16.getIcon());
      bOpen.setMnemonic('o');
      bOpen.setEnabled(false);
      bOpen.addActionListener(this);
      bOpenNew = new JButton("Open in new window", Icons.ICON_OPEN_16.getIcon());
      bOpenNew.setMnemonic('n');
      bOpenNew.setEnabled(false);
      bOpenNew.addActionListener(this);
      JPanel pBottomButtons = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 0, 0, 0), 0, 0);
      pBottomButtons.add(new JPanel(), c);
      c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 0), 0, 0);
      pBottomButtons.add(bInsertRef, c);
      c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 0), 0, 0);
      pBottomButtons.add(bOpen, c);
      c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 0), 0, 0);
      pBottomButtons.add(bOpenNew, c);
      c = ViewerUtil.setGBC(c, 4, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 0, 0, 4), 0, 0);
      pBottomButtons.add(new JPanel(), c);

      JPanel pBottomProgress = new JPanel(new GridBagLayout());
      pbProgress = new JProgressBar();
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
          new Insets(0, 0, 0, 0), 0, 0);
      pBottomProgress.add(pbProgress, c);

      clBottomBar = new CardLayout(0, 0);
      pBottomBar = new JPanel(clBottomBar);
      pBottomBar.add(pBottomButtons, "buttons");
      pBottomBar.add(pBottomProgress, "progress");
      clBottomBar.show(pBottomBar, "buttons");

      JPanel pBottomMain = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
          new Insets(4, 4, 4, 4), 0, 0);
      pBottomMain.add(pBottomBar, c);

      // putting all together
      JPanel pMain = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
          new Insets(4, 4, 0, 4), 0, 0);
      pMain.add(pFindMain, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
          new Insets(4, 4, 0, 4), 0, 0);
      pMain.add(pResultMain, c);
      c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
          new Insets(4, 4, 4, 4), 0, 0);
      pMain.add(pBottomMain, c);

      getContentPane().setLayout(new BorderLayout());
      getContentPane().add(pMain, BorderLayout.CENTER);
      getRootPane().setDefaultButton(bSearch);

      pm.setProgress(progress++);
      pack();
      setMinimumSize(new Dimension(getPreferredSize().width, getPreferredSize().height - pFindOptions.getHeight()));
      setLocationRelativeTo(getParent());
      setVisible(true);
    } finally {
      pm.close();
    }
  }

  /** Returns whether any option has been selected in the current options panel. */
  private boolean isOptionsEmpty() {
    String type = getCurrentResourceType();
    OptionsBasePanel panel = mapOptionsPanel.get(type);
    if (panel != null) {
      return panel.isEmpty();
    }
    return true;
  }

  /** Returns the resource type (as file extension) of the current selection. */
  private String getCurrentResourceType() {
    ObjectString os = (ObjectString) cbResourceType.getSelectedItem();
    if (os != null) {
      return (String) os.getObject();
    }
    return "";
  }

  // -------------------------- INNER CLASSES --------------------------

  /** Worker class for threaded searching resources. */
  private final class SearchWorker implements Runnable {
    /** List with result of operation. */
    private final List<ResourceEntry> matched;
    private final SearchOptions so;
    private final ResourceEntry entry;

    /**
     * @param matched List containing matching resources
     * @param so      SearchOptions instance
     * @param entry   The resource to search
     */
    public SearchWorker(List<ResourceEntry> matched, SearchOptions so, ResourceEntry entry) {
      this.matched = matched;
      this.so = so;
      this.entry = entry;
    }

    @Override
    public void run() {
      if (entry.matchSearchOptions(so)) {
        matched.add(entry);
      }
      synchronized (pbProgress) {
        pbProgress.setValue(pbProgress.getValue() + 1);
      }
    }
  }

  private static abstract class OptionsBasePanel extends JPanel implements ActionListener {
    private final SearchResource searchResource;
    protected final JCheckBox[] cbOptions;

    public OptionsBasePanel(SearchResource searchResource, int optionsCount) {
      super(new GridBagLayout());
      this.searchResource = searchResource;
      cbOptions = new JCheckBox[optionsCount];
    }

    /** Returns whether any options have been activated. */
    public boolean isEmpty() {
      for (JCheckBox cbOption : cbOptions) {
        if (cbOption.isSelected()) {
          return false;
        }
      }
      return true;
    }

    /** Returns whether the specified option is active. */
    public boolean isOptionEnabled(int index) {
      if (cbOptions != null && index >= 0 && index < cbOptions.length) {
        if (cbOptions[index] != null) {
          return cbOptions[index].isSelected();
        }
      }
      return false;
    }

    /**
     * Returns a structure containing all defined options to be considered by the search.
     *
     * @return A SearchOptions instance containing all relevant search criteria.
     */
    public abstract SearchOptions getOptions();

    /** Triggers action events for the array of specified objects */
    protected void triggerActions(Object[] sources) {
      if (sources != null) {
        for (Object element : sources) {
          actionPerformed(new ActionEvent(element, 0, null));
        }
      }
    }

    protected void fireOptionsPropertyChanged() {
      if (searchResource.pFindOptions != null) {
        searchResource.pFindOptions.firePropertyChange(PROPERTY_OPTIONS, !searchResource.bSearch.isEnabled(),
            isEmpty());
      }
    }
  }

  private static class OptionsAREPanel extends OptionsBasePanel {
    // indices of the options checkboxes
    public static final int ID_ACTOR = 0;
    public static final int ID_AREA_SCRIPT = 1;
    public static final int ID_ANIMATION = 2;
    public static final int ID_ITEM = 3;
    public static final int ID_AREA_TYPE = 4;
    public static final int ID_LOCATION = 5;
    public static final int ID_CUSTOM = 6;

    public static final int OPTIONS_COUNT = 7;

    private FlagsPanel pType, pLocation;
    private CustomFilterPanel pCustomFilter;
    private ButtonPopupWindow bpwType;
    private ButtonPopupWindow bpwLocation;
    private ButtonPopupWindow bpwCustomFilter;
    private JComboBox<ResourceEntry> cbActor;
    private JComboBox<ResourceEntry> cbAnimation;
    private JComboBox<ResourceEntry> cbScript;
    private JComboBox<ResourceEntry> cbItem;

    public OptionsAREPanel(SearchResource searchResource) {
      super(searchResource, OPTIONS_COUNT);
      init();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      if (event.getSource() instanceof JCheckBox) {
        int idx = Utils.getOptionIndex(cbOptions, event.getSource());
        if (idx >= 0) {
          switch (idx) {
            case ID_ACTOR:
              cbActor.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                cbActor.requestFocusInWindow();
              }
              break;
            case ID_AREA_SCRIPT:
              cbScript.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                cbScript.requestFocusInWindow();
              }
              break;
            case ID_ANIMATION:
              cbAnimation.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                cbAnimation.requestFocusInWindow();
              }
              break;
            case ID_ITEM:
              cbItem.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                cbItem.requestFocusInWindow();
              }
              break;
            case ID_AREA_TYPE:
              bpwType.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwType.requestFocusInWindow();
              }
              break;
            case ID_LOCATION:
              bpwLocation.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwLocation.requestFocusInWindow();
              }
              break;
            case ID_CUSTOM:
              bpwCustomFilter.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwCustomFilter.requestFocusInWindow();
              }
              break;
          }
          fireOptionsPropertyChanged();
        }
      }
    }

    @Override
    public SearchOptions getOptions() {
      SearchOptions retVal = new SearchOptions(SearchOptions.getResourceName(SearchOptions.ARE));
      for (int idx = 0; idx < cbOptions.length; idx++) {
        if (isOptionEnabled(idx)) {
          switch (idx) {
            case ID_ACTOR:
              retVal.setOption(SearchOptions.ARE_Actor_Character, getOptionActor());
              break;
            case ID_AREA_SCRIPT:
              retVal.setOption(SearchOptions.ARE_AreaScript, getOptionAreaScript());
              break;
            case ID_ANIMATION:
              retVal.setOption(SearchOptions.ARE_Animation_Animation, getOptionAnimation());
              break;
            case ID_ITEM:
              retVal.setOption(SearchOptions.ARE_Container_Item_Item, getOptionItem());
              break;
            case ID_AREA_TYPE:
              retVal.setOption(SearchOptions.ARE_AreaType, pType.getOptionFlags());
              break;
            case ID_LOCATION:
              retVal.setOption(SearchOptions.ARE_Location, pLocation.getOptionFlags());
              break;
            case ID_CUSTOM:
              if (pCustomFilter.isActive(0)) {
                retVal.setOption(SearchOptions.ARE_Custom1, pCustomFilter.getOptionFilter(0));
              }
              if (pCustomFilter.isActive(1)) {
                retVal.setOption(SearchOptions.ARE_Custom2, pCustomFilter.getOptionFilter(1));
              }
              if (pCustomFilter.isActive(2)) {
                retVal.setOption(SearchOptions.ARE_Custom3, pCustomFilter.getOptionFilter(2));
              }
              if (pCustomFilter.isActive(3)) {
                retVal.setOption(SearchOptions.ARE_Custom4, pCustomFilter.getOptionFilter(3));
              }
              break;
          }
        }
      }
      return retVal;
    }

    public String getOptionActor() {
      return Utils.getResourceName(cbOptions[ID_ACTOR], cbActor);
    }

    public String getOptionAreaScript() {
      return Utils.getResourceName(cbOptions[ID_AREA_SCRIPT], cbScript);
    }

    public String getOptionAnimation() {
      return Utils.getResourceName(cbOptions[ID_ANIMATION], cbAnimation);
    }

    public String getOptionItem() {
      return Utils.getResourceName(cbOptions[ID_ITEM], cbItem);
    }

    private void init() {
      JCheckBox cb;
      GridBagConstraints c = new GridBagConstraints();

      cb = new JCheckBox("Actor:");
      cb.addActionListener(this);
      cbOptions[ID_ACTOR] = cb;
      cb = new JCheckBox("Area script:");
      cb.addActionListener(this);
      cbOptions[ID_AREA_SCRIPT] = cb;
      cb = new JCheckBox("Animation:");
      cb.addActionListener(this);
      cbOptions[ID_ANIMATION] = cb;
      cb = new JCheckBox("Item:");
      cb.setToolTipText("Checks for items in containers only.");
      cb.addActionListener(this);
      cbOptions[ID_ITEM] = cb;
      cb = new JCheckBox("Area type:");
      cb.addActionListener(this);
      cbOptions[ID_AREA_TYPE] = cb;
      cb = new JCheckBox("Location:");
      cb.addActionListener(this);
      cbOptions[ID_LOCATION] = cb;
      cb = new JCheckBox("Custom filters:");
      cb.addActionListener(this);
      cbOptions[ID_CUSTOM] = cb;

      cbActor = Utils.createNamedResourceComboBox(new String[] { "CRE" }, true);

      cbScript = Utils.createNamedResourceComboBox(new String[] { "BCS" }, false);

      if (Profile.isEnhancedEdition()) {
        cbAnimation = Utils.createNamedResourceComboBox(new String[] { "BAM", "PVRZ", "WBM" }, false);
      } else {
        cbAnimation = Utils.createNamedResourceComboBox(new String[] { "BAM" }, false);
      }

      cbItem = Utils.createNamedResourceComboBox(new String[] { "ITM" }, true);

      pCustomFilter = new CustomFilterPanel(4, null);
      bpwCustomFilter = new ButtonPopupWindow(SET_OPTIONS_TEXT, pCustomFilter);

      String[] areType;
      if (Profile.getEngine() == Profile.Engine.PST) {
        areType = AreResource.ATYPE_TORMENT_ARRAY;
      } else if (Profile.getEngine() == Profile.Engine.IWD2) {
        areType = AreResource.ATYPE_IWD2_ARRAY;
      } else if (Profile.getGame() == Profile.Game.PSTEE) {
        areType = AreResource.ATYPE_PSTEE_ARRAY;
      } else if (Profile.getEngine() == Profile.Engine.EE) {
        areType = AreResource.ATYPE_EE_ARRAY;
      } else {
        areType = AreResource.ATYPE_ARRAY;
      }
      String[] areFlags = (Profile.getEngine() == Profile.Engine.PST || Profile.getGame() == Profile.Game.PSTEE)
          ? AreResource.FLAGS_TORMENT_ARRAY
          : AreResource.FLAGS_ARRAY;
      pType = new FlagsPanel(4, areType);
      bpwType = new ButtonPopupWindow(SET_OPTIONS_TEXT, pType);
      bpwType.addActionListener(this);
      pLocation = new FlagsPanel(2, areFlags);
      bpwLocation = new ButtonPopupWindow(SET_OPTIONS_TEXT, pLocation);
      bpwLocation.addActionListener(this);

      JPanel pOptions = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_AREA_SCRIPT], c);
      c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 0), 0, 0);
      pOptions.add(cbScript, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_ACTOR], c);
      c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(cbActor, c);
      c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_ANIMATION], c);
      c = ViewerUtil.setGBC(c, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(cbAnimation, c);
      c = ViewerUtil.setGBC(c, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_ITEM], c);
      c = ViewerUtil.setGBC(c, 1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(cbItem, c);

      c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_AREA_TYPE], c);
      c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 0), 0, 0);
      pOptions.add(bpwType, c);
      c = ViewerUtil.setGBC(c, 2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_LOCATION], c);
      c = ViewerUtil.setGBC(c, 3, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwLocation, c);
      c = ViewerUtil.setGBC(c, 2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_CUSTOM], c);
      c = ViewerUtil.setGBC(c, 3, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwCustomFilter, c);

      triggerActions(cbOptions);

      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 8), 0, 0);
      add(pOptions, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
          new Insets(0, 0, 0, 0), 0, 0);
      add(new JPanel(), c);
    }
  }

  private static class OptionsCREPanel extends OptionsBasePanel {
    // indices of the options checkboxes
    public static final int ID_NAME = 0;
    public static final int ID_SCRIPT_NAME = 1;
    public static final int ID_FLAGS = 2;
    public static final int ID_LEVEL = 3;
    public static final int ID_TYPE = 4;
    public static final int ID_ANIMATION = 5;
    public static final int ID_SCRIPTS = 6;
    public static final int ID_GAME_SPECIFIC = 7;
    public static final int ID_SPELLS = 8;
    public static final int ID_ITEMS = 9;
    public static final int ID_EFFECTS = 10;
    public static final int ID_CUSTOM = 11;

    public static final int OPTIONS_COUNT = 12;

    private FlagsPanel pFlags;
    private CreLevelPanel pLevel;
    private CreLevelIWD2Panel pLevelIWD2;
    private CreTypePanel pType;
    /** Allows to specify filter for scripts. */
    private ResourcesFilterPanel pScripts;
    private CreGameSpecificPanel pGameSpecific;
    private EffectsPanel pEffects;
    /** Allows to specify filter for inventory items. */
    private ResourcesFilterPanel pItems;
    /** Allows to specify filter for spells. */
    private ResourcesFilterPanel pSpells;
    private CustomFilterPanel pCustomFilter;
    private JTextField tfName, tfScriptName;
    private ButtonPopupWindow bpwFlags;
    private ButtonPopupWindow bpwTypes;
    private ButtonPopupWindow bpwLevel;
    private ButtonPopupWindow bpwScripts;
    private ButtonPopupWindow bpwGameSpecific;
    private ButtonPopupWindow bpwEffects;
    private ButtonPopupWindow bpwItems;
    private ButtonPopupWindow bpwSpells;
    private ButtonPopupWindow bpwCustomFilter;
    private JComboBox<IdsMapEntry> cbAnimation;

    public OptionsCREPanel(SearchResource searchResource) {
      super(searchResource, OPTIONS_COUNT);
      init();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      if (event.getSource() instanceof JCheckBox) {
        int idx = Utils.getOptionIndex(cbOptions, event.getSource());
        if (idx >= 0) {
          switch (idx) {
            case ID_NAME:
              tfName.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                tfName.requestFocusInWindow();
              }
              break;
            case ID_SCRIPT_NAME:
              tfScriptName.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                tfScriptName.requestFocusInWindow();
              }
              break;
            case ID_FLAGS:
              bpwFlags.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwFlags.requestFocusInWindow();
              }
              break;
            case ID_LEVEL:
              bpwLevel.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwLevel.requestFocusInWindow();
              }
              break;
            case ID_TYPE:
              bpwTypes.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwTypes.requestFocusInWindow();
              }
              break;
            case ID_SCRIPTS:
              bpwScripts.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwScripts.requestFocusInWindow();
              }
              break;
            case ID_ANIMATION:
              cbAnimation.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                cbAnimation.requestFocusInWindow();
              }
              break;
            case ID_GAME_SPECIFIC:
              bpwGameSpecific.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwGameSpecific.requestFocusInWindow();
              }
              break;
            case ID_EFFECTS:
              bpwEffects.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwEffects.requestFocusInWindow();
              }
              break;
            case ID_ITEMS:
              bpwItems.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwItems.requestFocusInWindow();
              }
              break;
            case ID_SPELLS:
              bpwSpells.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwSpells.requestFocusInWindow();
              }
              break;
            case ID_CUSTOM:
              bpwCustomFilter.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwCustomFilter.requestFocusInWindow();
              }
              break;
          }
          fireOptionsPropertyChanged();
        }
      }
    }

    @Override
    public SearchOptions getOptions() {
      SearchOptions retVal = new SearchOptions(SearchOptions.getResourceName(SearchOptions.CRE));
      for (int idx = 0; idx < cbOptions.length; idx++) {
        if (isOptionEnabled(idx)) {
          switch (idx) {
            case ID_NAME:
              retVal.setOption(SearchOptions.CRE_Name, getOptionName());
              break;
            case ID_SCRIPT_NAME:
              retVal.setOption(SearchOptions.CRE_ScriptName, getOptionScriptName());
              break;
            case ID_FLAGS:
              retVal.setOption(SearchOptions.CRE_Flags, pFlags.getOptionFlags());
              break;
            case ID_LEVEL: {
              if ((Boolean) Profile.getProperty(Profile.Key.IS_SUPPORTED_CRE_V22)) {
                if (pLevelIWD2.isActive(CreLevelIWD2Panel.LEVEL_TOTAL)) {
                  retVal.setOption(SearchOptions.CRE_IWD2LevelTotal,
                      pLevelIWD2.getOptionLevel(CreLevelIWD2Panel.LEVEL_TOTAL));
                }
                if (pLevelIWD2.isActive(CreLevelIWD2Panel.LEVEL_BARBARIAN)) {
                  retVal.setOption(SearchOptions.CRE_IWD2LevelBarbarian,
                      pLevelIWD2.getOptionLevel(CreLevelIWD2Panel.LEVEL_BARBARIAN));
                }
                if (pLevelIWD2.isActive(CreLevelIWD2Panel.LEVEL_BARD)) {
                  retVal.setOption(SearchOptions.CRE_IWD2LevelBard,
                      pLevelIWD2.getOptionLevel(CreLevelIWD2Panel.LEVEL_BARD));
                }
                if (pLevelIWD2.isActive(CreLevelIWD2Panel.LEVEL_CLERIC)) {
                  retVal.setOption(SearchOptions.CRE_IWD2LevelCleric,
                      pLevelIWD2.getOptionLevel(CreLevelIWD2Panel.LEVEL_CLERIC));
                }
                if (pLevelIWD2.isActive(CreLevelIWD2Panel.LEVEL_DRUID)) {
                  retVal.setOption(SearchOptions.CRE_IWD2LevelDruid,
                      pLevelIWD2.getOptionLevel(CreLevelIWD2Panel.LEVEL_DRUID));
                }
                if (pLevelIWD2.isActive(CreLevelIWD2Panel.LEVEL_FIGHTER)) {
                  retVal.setOption(SearchOptions.CRE_IWD2LevelFighter,
                      pLevelIWD2.getOptionLevel(CreLevelIWD2Panel.LEVEL_FIGHTER));
                }
                if (pLevelIWD2.isActive(CreLevelIWD2Panel.LEVEL_MONK)) {
                  retVal.setOption(SearchOptions.CRE_IWD2LevelMonk,
                      pLevelIWD2.getOptionLevel(CreLevelIWD2Panel.LEVEL_MONK));
                }
                if (pLevelIWD2.isActive(CreLevelIWD2Panel.LEVEL_PALADIN)) {
                  retVal.setOption(SearchOptions.CRE_IWD2LevelPaladin,
                      pLevelIWD2.getOptionLevel(CreLevelIWD2Panel.LEVEL_PALADIN));
                }
                if (pLevelIWD2.isActive(CreLevelIWD2Panel.LEVEL_RANGER)) {
                  retVal.setOption(SearchOptions.CRE_IWD2LevelRanger,
                      pLevelIWD2.getOptionLevel(CreLevelIWD2Panel.LEVEL_RANGER));
                }
                if (pLevelIWD2.isActive(CreLevelIWD2Panel.LEVEL_ROGUE)) {
                  retVal.setOption(SearchOptions.CRE_IWD2LevelRogue,
                      pLevelIWD2.getOptionLevel(CreLevelIWD2Panel.LEVEL_ROGUE));
                }
                if (pLevelIWD2.isActive(CreLevelIWD2Panel.LEVEL_SORCERER)) {
                  retVal.setOption(SearchOptions.CRE_IWD2LevelSorcerer,
                      pLevelIWD2.getOptionLevel(CreLevelIWD2Panel.LEVEL_SORCERER));
                }
                if (pLevelIWD2.isActive(CreLevelIWD2Panel.LEVEL_WIZARD)) {
                  retVal.setOption(SearchOptions.CRE_IWD2LevelWizard,
                      pLevelIWD2.getOptionLevel(CreLevelIWD2Panel.LEVEL_WIZARD));
                }
              } else {
                if (pLevel.isActive(0)) {
                  retVal.setOption(SearchOptions.CRE_Level1, pLevel.getOptionLevel(0));
                }
                if (pLevel.isActive(1)) {
                  retVal.setOption(SearchOptions.CRE_Level2, pLevel.getOptionLevel(1));
                }
                if (pLevel.isActive(2)) {
                  retVal.setOption(SearchOptions.CRE_Level3, pLevel.getOptionLevel(2));
                }
              }
              break;
            }
            case ID_TYPE:
              if (pType.isActive(CreTypePanel.TYPE_GENERAL)) {
                retVal.setOption(SearchOptions.CRE_General, pType.getOptionType(CreTypePanel.TYPE_GENERAL));
              }
              if (pType.isActive(CreTypePanel.TYPE_CLASS)) {
                retVal.setOption(SearchOptions.CRE_Class, pType.getOptionType(CreTypePanel.TYPE_CLASS));
              }
              if (pType.isActive(CreTypePanel.TYPE_SPECIFICS)) {
                retVal.setOption(SearchOptions.CRE_Specifics, pType.getOptionType(CreTypePanel.TYPE_SPECIFICS));
              }
              if (pType.isActive(CreTypePanel.TYPE_ALIGNMENT)) {
                retVal.setOption(SearchOptions.CRE_Alignment, pType.getOptionType(CreTypePanel.TYPE_ALIGNMENT));
              }
              if (pType.isActive(CreTypePanel.TYPE_GENDER)) {
                if ((Boolean) Profile.getProperty(Profile.Key.IS_SUPPORTED_CRE_V22)) {
                  retVal.setOption(SearchOptions.CRE_Sex, pType.getOptionType(CreTypePanel.TYPE_GENDER));
                } else {
                  retVal.setOption(SearchOptions.CRE_Gender, pType.getOptionType(CreTypePanel.TYPE_GENDER));
                }
              }
              if (pType.isActive(CreTypePanel.TYPE_RACE)) {
                retVal.setOption(SearchOptions.CRE_Race, pType.getOptionType(CreTypePanel.TYPE_RACE));
              }
              if (pType.isActive(CreTypePanel.TYPE_ALLEGIANCE)) {
                retVal.setOption(SearchOptions.CRE_Allegiance, pType.getOptionType(CreTypePanel.TYPE_ALLEGIANCE));
              }
              if (pType.isActive(CreTypePanel.TYPE_KIT)) {
                retVal.setOption(SearchOptions.CRE_Kit, pType.getOptionType(CreTypePanel.TYPE_KIT));
              }
              break;
            case ID_SCRIPTS:
              if (pScripts.isActive(0)) {
                retVal.setOption(SearchOptions.CRE_Script1, pScripts.getResourceName(0));
              }
              if (pScripts.isActive(1)) {
                retVal.setOption(SearchOptions.CRE_Script2, pScripts.getResourceName(1));
              }
              if (pScripts.isActive(2)) {
                retVal.setOption(SearchOptions.CRE_Script3, pScripts.getResourceName(2));
              }
              break;
            case ID_ANIMATION:
              retVal.setOption(SearchOptions.CRE_Animation, getOptionAnimation());
              break;
            case ID_GAME_SPECIFIC:
              if (pGameSpecific.isActive(CreGameSpecificPanel.TYPE_FEATS1)) {
                retVal.setOption(SearchOptions.CRE_Feats1,
                    pGameSpecific.getOptionFlags(CreGameSpecificPanel.TYPE_FEATS1));
              }
              if (pGameSpecific.isActive(CreGameSpecificPanel.TYPE_FEATS2)) {
                retVal.setOption(SearchOptions.CRE_Feats2,
                    pGameSpecific.getOptionFlags(CreGameSpecificPanel.TYPE_FEATS2));
              }
              if (pGameSpecific.isActive(CreGameSpecificPanel.TYPE_FEATS3)) {
                retVal.setOption(SearchOptions.CRE_Feats3,
                    pGameSpecific.getOptionFlags(CreGameSpecificPanel.TYPE_FEATS3));
              }
              if (pGameSpecific.isActive(CreGameSpecificPanel.TYPE_ATTRIBUTES)) {
                retVal.setOption(SearchOptions.CRE_Attributes,
                    pGameSpecific.getOptionFlags(CreGameSpecificPanel.TYPE_ATTRIBUTES));
              }
              break;
            case ID_EFFECTS:
              if (pEffects.isActive(0)) {
                retVal.setOption(SearchOptions.CRE_Effect_Type1, pEffects.getOptionEffect(0));
              }
              if (pEffects.isActive(1)) {
                retVal.setOption(SearchOptions.CRE_Effect_Type2, pEffects.getOptionEffect(1));
              }
              if (pEffects.isActive(2)) {
                retVal.setOption(SearchOptions.CRE_Effect_Type3, pEffects.getOptionEffect(2));
              }
              if (pEffects.isActive(3)) {
                retVal.setOption(SearchOptions.CRE_Effect_Type4, pEffects.getOptionEffect(3));
              }
              break;
            case ID_ITEMS:
              if (pItems.isActive(0)) {
                retVal.setOption(SearchOptions.CRE_Item_Item1, pItems.getResourceName(0));
              }
              if (pItems.isActive(1)) {
                retVal.setOption(SearchOptions.CRE_Item_Item2, pItems.getResourceName(1));
              }
              if (pItems.isActive(2)) {
                retVal.setOption(SearchOptions.CRE_Item_Item3, pItems.getResourceName(2));
              }
              if (pItems.isActive(3)) {
                retVal.setOption(SearchOptions.CRE_Item_Item4, pItems.getResourceName(3));
              }
              break;
            case ID_SPELLS:
              if (pSpells.isActive(0)) {
                retVal.setOption(SearchOptions.CRE_Spell_Spell1, pSpells.getResourceName(0));
              }
              if (pSpells.isActive(1)) {
                retVal.setOption(SearchOptions.CRE_Spell_Spell2, pSpells.getResourceName(1));
              }
              if (pSpells.isActive(2)) {
                retVal.setOption(SearchOptions.CRE_Spell_Spell3, pSpells.getResourceName(2));
              }
              if (pSpells.isActive(3)) {
                retVal.setOption(SearchOptions.CRE_Spell_Spell4, pSpells.getResourceName(3));
              }
              break;
            case ID_CUSTOM:
              if (pCustomFilter.isActive(0)) {
                retVal.setOption(SearchOptions.CRE_Custom1, pCustomFilter.getOptionFilter(0));
              }
              if (pCustomFilter.isActive(1)) {
                retVal.setOption(SearchOptions.CRE_Custom2, pCustomFilter.getOptionFilter(1));
              }
              if (pCustomFilter.isActive(2)) {
                retVal.setOption(SearchOptions.CRE_Custom3, pCustomFilter.getOptionFilter(2));
              }
              if (pCustomFilter.isActive(3)) {
                retVal.setOption(SearchOptions.CRE_Custom4, pCustomFilter.getOptionFilter(3));
              }
              break;
          }
        }
      }
      return retVal;
    }

    public String getOptionName() {
      return cbOptions[ID_NAME].isSelected() ? tfName.getText() : "";
    }

    public String getOptionScriptName() {
      return cbOptions[ID_SCRIPT_NAME].isSelected() ? tfScriptName.getText() : "";
    }

    public int getOptionAnimation() {
      return Utils.getIdsValue(cbOptions[ID_ANIMATION], cbAnimation);
    }

    private void init() {
      JCheckBox cb;
      GridBagConstraints c = new GridBagConstraints();

      cb = new JCheckBox("Name:");
      cb.addActionListener(this);
      cbOptions[ID_NAME] = cb;
      cb = new JCheckBox("Script name:");
      cb.addActionListener(this);
      cbOptions[ID_SCRIPT_NAME] = cb;
      cb = new JCheckBox("Flags:");
      cb.addActionListener(this);
      cbOptions[ID_FLAGS] = cb;
      cb = new JCheckBox("Creature levels:");
      cb.addActionListener(this);
      cbOptions[ID_LEVEL] = cb;
      cb = new JCheckBox("Creature types:");
      cb.addActionListener(this);
      cbOptions[ID_TYPE] = cb;
      cb = new JCheckBox("Animation ID:");
      cb.addActionListener(this);
      cbOptions[ID_ANIMATION] = cb;
      cb = new JCheckBox("Scripts:");
      cb.addActionListener(this);
      cbOptions[ID_SCRIPTS] = cb;
      cb = new JCheckBox("Game-specific:");
      cb.setEnabled(CreGameSpecificPanel.isGameSpecificEnabled());
      cb.setToolTipText("Icewind Dale 2 and Planescape: Torment only");
      cb.addActionListener(this);
      cbOptions[ID_GAME_SPECIFIC] = cb;
      cb = new JCheckBox("Spells:");
      cb.addActionListener(this);
      cbOptions[ID_SPELLS] = cb;
      cb = new JCheckBox("Items:");
      cb.addActionListener(this);
      cbOptions[ID_ITEMS] = cb;
      cb = new JCheckBox("Effects:");
      cb.addActionListener(this);
      cbOptions[ID_EFFECTS] = cb;
      cb = new JCheckBox("Custom filters:");
      cb.addActionListener(this);
      cbOptions[ID_CUSTOM] = cb;

      tfName = Utils.defaultWidth(new JTextField());
      tfScriptName = Utils.defaultWidth(new JTextField(new FormattedDocument(32, false), "", 0));
      pFlags = new FlagsPanel(4, CreResource.FLAGS_ARRAY);
      bpwFlags = new ButtonPopupWindow(SET_OPTIONS_TEXT, pFlags);
      pType = new CreTypePanel();
      bpwTypes = new ButtonPopupWindow(SET_OPTIONS_TEXT, pType);
      if ((Boolean) Profile.getProperty(Profile.Key.IS_SUPPORTED_CRE_V22)) {
        pLevelIWD2 = new CreLevelIWD2Panel();
        bpwLevel = new ButtonPopupWindow(SET_OPTIONS_TEXT, pLevelIWD2);
      } else {
        pLevel = new CreLevelPanel();
        bpwLevel = new ButtonPopupWindow(SET_OPTIONS_TEXT, pLevel);
      }

      pScripts = new ResourcesFilterPanel(3, "Script %d:", "BCS", false);
      bpwScripts = new ButtonPopupWindow(SET_OPTIONS_TEXT, pScripts);

      cbAnimation = Utils.getIdsMapEntryList(4, "Animation", "ANIMATE.IDS");
      cbAnimation.setPreferredSize(Utils.getPrototypeSize(cbAnimation));

      pGameSpecific = new CreGameSpecificPanel();
      bpwGameSpecific = new ButtonPopupWindow(SET_OPTIONS_TEXT, pGameSpecific);

      pSpells = new ResourcesFilterPanel(4, "Spell resource %d:", "SPL", true);
      bpwSpells = new ButtonPopupWindow(SET_OPTIONS_TEXT, pSpells);

      pItems = new ResourcesFilterPanel(4, "Item resource %d:", "ITM", true);
      bpwItems = new ButtonPopupWindow(SET_OPTIONS_TEXT, pItems);

      pEffects = new EffectsPanel(4, "Effect");
      bpwEffects = new ButtonPopupWindow(SET_OPTIONS_TEXT, pEffects);

      pCustomFilter = new CustomFilterPanel(4, null);
      bpwCustomFilter = new ButtonPopupWindow(SET_OPTIONS_TEXT, pCustomFilter);

      JPanel pOptions = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_NAME], c);
      c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 0), 0, 0);
      pOptions.add(tfName, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_SCRIPT_NAME], c);
      c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(tfScriptName, c);
      c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_FLAGS], c);
      c = ViewerUtil.setGBC(c, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwFlags, c);
      c = ViewerUtil.setGBC(c, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_LEVEL], c);
      c = ViewerUtil.setGBC(c, 1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwLevel, c);
      c = ViewerUtil.setGBC(c, 0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_TYPE], c);
      c = ViewerUtil.setGBC(c, 1, 4, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwTypes, c);
      c = ViewerUtil.setGBC(c, 0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_ANIMATION], c);
      c = ViewerUtil.setGBC(c, 1, 5, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(cbAnimation, c);

      c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_SCRIPTS], c);
      c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 0), 0, 0);
      pOptions.add(bpwScripts, c);
      c = ViewerUtil.setGBC(c, 2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_GAME_SPECIFIC], c);
      c = ViewerUtil.setGBC(c, 3, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwGameSpecific, c);
      c = ViewerUtil.setGBC(c, 2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_SPELLS], c);
      c = ViewerUtil.setGBC(c, 3, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwSpells, c);
      c = ViewerUtil.setGBC(c, 2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_ITEMS], c);
      c = ViewerUtil.setGBC(c, 3, 3, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwItems, c);
      c = ViewerUtil.setGBC(c, 2, 4, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_EFFECTS], c);
      c = ViewerUtil.setGBC(c, 3, 4, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwEffects, c);
      c = ViewerUtil.setGBC(c, 2, 5, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_CUSTOM], c);
      c = ViewerUtil.setGBC(c, 3, 5, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwCustomFilter, c);

      triggerActions(cbOptions);

      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 8), 0, 0);
      add(pOptions, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
          new Insets(0, 0, 0, 0), 0, 0);
      add(new JPanel(), c);
    }
  }

  private static class OptionsEFFPanel extends OptionsBasePanel {
    // indices of the options checkboxes
    public static final int ID_EFFECT = 0;
    public static final int ID_PARAM_1 = 1;
    public static final int ID_PARAM_2 = 2;
    public static final int ID_TIMING_MODE = 3;
    public static final int ID_RESOURCE_1 = 4;
    public static final int ID_RESOURCE_2 = 5;
    public static final int ID_RESOURCE_3 = 6;
    public static final int ID_SAVE_TYPE = 7;
    public static final int ID_CUSTOM = 8;

    public static final int OPTIONS_COUNT = 9;

    private final JSpinner[] sOpcode = new JSpinner[2];
    private final JSpinner[] sParam1 = new JSpinner[2];
    private final JSpinner[] sParam2 = new JSpinner[2];

    private FlagsPanel pSaveType;
    private TimingModePanel pTiming;
    private CustomFilterPanel pCustomFilter;
    private JTextField tfResource1;
    private JTextField tfResource2;
    private JTextField tfResource3;
    private ButtonPopupWindow bpwTiming;
    private ButtonPopupWindow bpwSaveType;
    private ButtonPopupWindow bpwCustomFilter;

    public OptionsEFFPanel(SearchResource searchResource) {
      super(searchResource, OPTIONS_COUNT);
      init();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      if (event.getSource() instanceof JCheckBox) {
        int idx = Utils.getOptionIndex(cbOptions, event.getSource());
        if (idx >= 0) {
          switch (idx) {
            case ID_EFFECT:
              sOpcode[0].setEnabled(cbOptions[idx].isSelected());
              sOpcode[1].setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                sOpcode[0].requestFocusInWindow();
              }
              break;
            case ID_PARAM_1:
              sParam1[0].setEnabled(cbOptions[idx].isSelected());
              sParam1[1].setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                sParam1[0].requestFocusInWindow();
              }
              break;
            case ID_PARAM_2:
              sParam2[0].setEnabled(cbOptions[idx].isSelected());
              sParam2[1].setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                sParam2[0].requestFocusInWindow();
              }
              break;
            case ID_TIMING_MODE:
              bpwTiming.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwTiming.requestFocusInWindow();
              }
              break;
            case ID_RESOURCE_1:
              tfResource1.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                tfResource1.requestFocusInWindow();
              }
              break;
            case ID_RESOURCE_2:
              tfResource2.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                tfResource2.requestFocusInWindow();
              }
              break;
            case ID_RESOURCE_3:
              tfResource3.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                tfResource3.requestFocusInWindow();
              }
              break;
            case ID_SAVE_TYPE:
              bpwSaveType.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwSaveType.requestFocusInWindow();
              }
              break;
            case ID_CUSTOM:
              bpwCustomFilter.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwCustomFilter.requestFocusInWindow();
              }
              break;
          }
          fireOptionsPropertyChanged();
        }
      }
    }

    @Override
    public SearchOptions getOptions() {
      SearchOptions retVal = new SearchOptions(SearchOptions.getResourceName(SearchOptions.EFF));
      for (int idx = 0; idx < cbOptions.length; idx++) {
        if (isOptionEnabled(idx)) {
          switch (idx) {
            case ID_EFFECT:
              retVal.setOption(SearchOptions.EFF_Effect, getOptionEffect());
              break;
            case ID_PARAM_1:
              retVal.setOption(SearchOptions.EFF_Param1, getOptionParam1());
              break;
            case ID_PARAM_2:
              retVal.setOption(SearchOptions.EFF_Param2, getOptionParam2());
              break;
            case ID_TIMING_MODE: {
              if (pTiming.isActive(TimingModePanel.TIMING_MODE)) {
                retVal.setOption(SearchOptions.EFF_TimingMode, pTiming.getOptionMode());
              }
              if (pTiming.isActive(TimingModePanel.TIMING_DURATION)) {
                retVal.setOption(SearchOptions.EFF_Duration, pTiming.getOptionDuration());
              }
              break;
            }
            case ID_RESOURCE_1:
              retVal.setOption(SearchOptions.EFF_Resource1, getOptionResource1());
              break;
            case ID_RESOURCE_2:
              retVal.setOption(SearchOptions.EFF_Resource2, getOptionResource2());
              break;
            case ID_RESOURCE_3:
              retVal.setOption(SearchOptions.EFF_Resource3, getOptionResource3());
              break;
            case ID_SAVE_TYPE:
              retVal.setOption(SearchOptions.EFF_SaveType, pSaveType.getOptionFlags());
              break;
            case ID_CUSTOM:
              if (pCustomFilter.isActive(0)) {
                retVal.setOption(SearchOptions.EFF_Custom1, pCustomFilter.getOptionFilter(0));
              }
              if (pCustomFilter.isActive(1)) {
                retVal.setOption(SearchOptions.EFF_Custom2, pCustomFilter.getOptionFilter(1));
              }
              if (pCustomFilter.isActive(2)) {
                retVal.setOption(SearchOptions.EFF_Custom3, pCustomFilter.getOptionFilter(2));
              }
              if (pCustomFilter.isActive(3)) {
                retVal.setOption(SearchOptions.EFF_Custom4, pCustomFilter.getOptionFilter(3));
              }
              break;
          }
        }
      }
      return retVal;
    }

    public Couple<Integer, Integer> getOptionEffect() {
      return Utils.getRangeValues(cbOptions[ID_EFFECT], sOpcode);
    }

    public Couple<Integer, Integer> getOptionParam1() {
      return Utils.getRangeValues(cbOptions[ID_PARAM_1], sParam1);
    }

    public Couple<Integer, Integer> getOptionParam2() {
      return Utils.getRangeValues(cbOptions[ID_PARAM_2], sParam2);
    }

    public String getOptionResource1() {
      return cbOptions[ID_RESOURCE_1].isSelected() ? tfResource1.getText() : "";
    }

    public String getOptionResource2() {
      return cbOptions[ID_RESOURCE_2].isSelected() ? tfResource2.getText() : "";
    }

    public String getOptionResource3() {
      return cbOptions[ID_RESOURCE_3].isSelected() ? tfResource3.getText() : "";
    }

    private void init() {
      JCheckBox cb;
      GridBagConstraints c = new GridBagConstraints();

      cb = new JCheckBox("Effect opcode:");
      cb.addActionListener(this);
      cbOptions[ID_EFFECT] = cb;
      cb = new JCheckBox("Timing mode:");
      cb.addActionListener(this);
      cbOptions[ID_TIMING_MODE] = cb;
      cb = new JCheckBox("Parameter 1:");
      cb.addActionListener(this);
      cbOptions[ID_PARAM_1] = cb;
      cb = new JCheckBox("Parameter 2:");
      cb.addActionListener(this);
      cbOptions[ID_PARAM_2] = cb;
      cb = new JCheckBox("Resource 1:");
      cb.addActionListener(this);
      cbOptions[ID_RESOURCE_1] = cb;
      cb = new JCheckBox("Resource 2:");
      cb.addActionListener(this);
      cbOptions[ID_RESOURCE_2] = cb;
      cb = new JCheckBox("Resource 3:");
      cb.addActionListener(this);
      cbOptions[ID_RESOURCE_3] = cb;
      cb = new JCheckBox("Save type:");
      cb.addActionListener(this);
      cbOptions[ID_SAVE_TYPE] = cb;
      cb = new JCheckBox("Custom filters:");
      cb.addActionListener(this);
      cbOptions[ID_CUSTOM] = cb;

      sOpcode[0] = Utils.createNumberSpinner(Short.MIN_VALUE, Short.MAX_VALUE, -32768, 32767, 0, 1);
      sOpcode[1] = Utils.createNumberSpinner(Short.MIN_VALUE, Short.MAX_VALUE, -32768, 32767, 999, 1);

      sParam1[0] = Utils.createNumberSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, -32768, 32767, 0, 1);
      sParam1[1] = Utils.createNumberSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, -32768, 32767, 32767, 1);

      sParam2[0] = Utils.createNumberSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, -32768, 32767, 0, 1);
      sParam2[1] = Utils.createNumberSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, -32768, 32767, 32767, 1);

      tfResource1 = Utils.defaultWidth(new JTextField(new FormattedDocument(8, true), "", 0));
      tfResource2 = Utils.defaultWidth(new JTextField(new FormattedDocument(8, true), "", 0));
      tfResource3 = Utils.defaultWidth(new JTextField(new FormattedDocument(8, true), "", 0));

      pTiming = new TimingModePanel();
      bpwTiming = new ButtonPopupWindow(SET_OPTIONS_TEXT, pTiming);

      String[] saveType = BaseOpcode.getSaveType();
      pSaveType = new FlagsPanel(4, saveType);
      bpwSaveType = new ButtonPopupWindow(SET_OPTIONS_TEXT, pSaveType);

      pCustomFilter = new CustomFilterPanel(4, null);
      bpwCustomFilter = new ButtonPopupWindow(SET_OPTIONS_TEXT, pCustomFilter);

      JPanel pOptions = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_EFFECT], c);
      c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 0), 0, 0);
      pOptions.add(Utils.createNumberRangePanel(sOpcode[0], sOpcode[1]), c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_PARAM_1], c);
      c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(Utils.createNumberRangePanel(sParam1[0], sParam1[1]), c);
      c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_PARAM_2], c);
      c = ViewerUtil.setGBC(c, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(Utils.createNumberRangePanel(sParam2[0], sParam2[1]), c);
      c = ViewerUtil.setGBC(c, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_TIMING_MODE], c);
      c = ViewerUtil.setGBC(c, 1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwTiming, c);
      c = ViewerUtil.setGBC(c, 0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_SAVE_TYPE], c);
      c = ViewerUtil.setGBC(c, 1, 4, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwSaveType, c);

      c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_RESOURCE_1], c);
      c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 0), 0, 0);
      pOptions.add(tfResource1, c);
      c = ViewerUtil.setGBC(c, 2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_RESOURCE_2], c);
      c = ViewerUtil.setGBC(c, 3, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(tfResource2, c);
      c = ViewerUtil.setGBC(c, 2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_RESOURCE_3], c);
      c = ViewerUtil.setGBC(c, 3, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(tfResource3, c);
      c = ViewerUtil.setGBC(c, 2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_CUSTOM], c);
      c = ViewerUtil.setGBC(c, 3, 3, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwCustomFilter, c);

      triggerActions(cbOptions);

      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 8), 0, 0);
      add(pOptions, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
          new Insets(0, 0, 0, 0), 0, 0);
      add(new JPanel(), c);
    }
  }

  private static class OptionsITMPanel extends OptionsBasePanel {
    // indices of the options checkboxes
    public static final int ID_NAME = 0;
    public static final int ID_FLAGS = 1;
    public static final int ID_CATEGORY = 2;
    public static final int ID_USABILITY = 3;
    public static final int ID_APPEARANCE = 4;
    public static final int ID_STATS = 5;
    public static final int ID_PRICE = 6;
    public static final int ID_ENCHANTMENT = 7;
    public static final int ID_ABILITY = 8;
    public static final int ID_EFFECTS = 9;
    public static final int ID_CUSTOM = 10;

    public static final int OPTIONS_COUNT = 11;

    private final JSpinner[] sPrice = new JSpinner[2];
    private final JSpinner[] sEnchantment = new JSpinner[2];

    private FlagsPanel pFlags;
    private ItmStatsPanel pStats;
    private ItmUsabilityPanel pUsability;
    private ItmAbilityPanel pAbility;
    private EffectsPanel pEffects;
    private CustomFilterPanel pCustomFilter;
    private JTextField tfName;
    private ButtonPopupWindow bpwFlags;
    private ButtonPopupWindow bpwUsability;
    private ButtonPopupWindow bpwStats;
    private ButtonPopupWindow bpwAbility;
    private ButtonPopupWindow bpwEffects;
    private ButtonPopupWindow bpwCustomFilter;
    private JComboBox<ObjectString> cbAppearance;
    private JComboBox<IndexedString> cbCategory;

    public OptionsITMPanel(SearchResource searchResource) {
      super(searchResource, OPTIONS_COUNT);
      init();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      if (event.getSource() instanceof JCheckBox) {
        int idx = Utils.getOptionIndex(cbOptions, event.getSource());
        if (idx >= 0) {
          switch (idx) {
            case ID_NAME:
              tfName.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                tfName.requestFocusInWindow();
              }
              break;
            case ID_FLAGS:
              bpwFlags.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwFlags.requestFocusInWindow();
              }
              break;
            case ID_CATEGORY:
              cbCategory.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                cbCategory.requestFocusInWindow();
              }
              break;
            case ID_USABILITY:
              bpwUsability.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwUsability.requestFocusInWindow();
              }
              break;
            case ID_APPEARANCE:
              cbAppearance.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                cbAppearance.requestFocusInWindow();
              }
              break;
            case ID_STATS:
              bpwStats.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwStats.requestFocusInWindow();
              }
              break;
            case ID_PRICE:
              sPrice[0].setEnabled(cbOptions[idx].isSelected());
              sPrice[1].setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                sPrice[0].requestFocusInWindow();
              }
              break;
            case ID_ENCHANTMENT:
              sEnchantment[0].setEnabled(cbOptions[idx].isSelected());
              sEnchantment[1].setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                sEnchantment[0].requestFocusInWindow();
              }
              break;
            case ID_ABILITY:
              bpwAbility.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwAbility.requestFocusInWindow();
              }
              break;
            case ID_EFFECTS:
              bpwEffects.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwEffects.requestFocusInWindow();
              }
              break;
            case ID_CUSTOM:
              bpwCustomFilter.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwCustomFilter.requestFocusInWindow();
              }
              break;
          }
          fireOptionsPropertyChanged();
        }
      }
    }

    @Override
    public SearchOptions getOptions() {
      SearchOptions retVal = new SearchOptions(SearchOptions.getResourceName(SearchOptions.ITM));

      for (int idx = 0; idx < cbOptions.length; idx++) {
        if (isOptionEnabled(idx)) {
          switch (idx) {
            case ID_NAME:
              retVal.setOption(SearchOptions.ITM_Name, getOptionName());
              break;
            case ID_FLAGS:
              retVal.setOption(SearchOptions.ITM_Flags, pFlags.getOptionFlags());
              break;
            case ID_CATEGORY:
              retVal.setOption(SearchOptions.ITM_Category, getOptionCategory());
              break;
            case ID_USABILITY:
              if (pUsability.isActive(ItmUsabilityPanel.ITEM_UNUSABLE)) {
                retVal.setOption(SearchOptions.ITM_Unusable,
                    pUsability.getOptionFlags(ItmUsabilityPanel.ITEM_UNUSABLE));
              }
              if (pUsability.isActive(ItmUsabilityPanel.ITEM_KITSUNUSABLE1)) {
                retVal.setOption(SearchOptions.ITM_KitsUnusable1,
                    pUsability.getOptionFlags(ItmUsabilityPanel.ITEM_KITSUNUSABLE1));
              }
              if (pUsability.isActive(ItmUsabilityPanel.ITEM_KITSUNUSABLE2)) {
                retVal.setOption(SearchOptions.ITM_KitsUnusable2,
                    pUsability.getOptionFlags(ItmUsabilityPanel.ITEM_KITSUNUSABLE2));
              }
              if (pUsability.isActive(ItmUsabilityPanel.ITEM_KITSUNUSABLE3)) {
                retVal.setOption(SearchOptions.ITM_KitsUnusable3,
                    pUsability.getOptionFlags(ItmUsabilityPanel.ITEM_KITSUNUSABLE3));
              }
              if (pUsability.isActive(ItmUsabilityPanel.ITEM_KITSUNUSABLE4)) {
                retVal.setOption(SearchOptions.ITM_KitsUnusable4,
                    pUsability.getOptionFlags(ItmUsabilityPanel.ITEM_KITSUNUSABLE4));
              }
              break;
            case ID_APPEARANCE:
              retVal.setOption(SearchOptions.ITM_Appearance, getOptionAppearance());
              break;
            case ID_STATS: {
              if (pStats.isActive(ItmStatsPanel.STAT_LEVEL)) {
                retVal.setOption(SearchOptions.ITM_MinLevel, pStats.getOptionValue(ItmStatsPanel.STAT_LEVEL));
              }
              if (pStats.isActive(ItmStatsPanel.STAT_STR)) {
                retVal.setOption(SearchOptions.ITM_MinSTR, pStats.getOptionValue(ItmStatsPanel.STAT_STR));
              }
              if (pStats.isActive(ItmStatsPanel.STAT_STR_EXTRA)) {
                retVal.setOption(SearchOptions.ITM_MinSTRExtra, pStats.getOptionValue(ItmStatsPanel.STAT_STR_EXTRA));
              }
              if (pStats.isActive(ItmStatsPanel.STAT_CON)) {
                retVal.setOption(SearchOptions.ITM_MinCON, pStats.getOptionValue(ItmStatsPanel.STAT_CON));
              }
              if (pStats.isActive(ItmStatsPanel.STAT_DEX)) {
                retVal.setOption(SearchOptions.ITM_MinDEX, pStats.getOptionValue(ItmStatsPanel.STAT_DEX));
              }
              if (pStats.isActive(ItmStatsPanel.STAT_INT)) {
                retVal.setOption(SearchOptions.ITM_MinINT, pStats.getOptionValue(ItmStatsPanel.STAT_INT));
              }
              if (pStats.isActive(ItmStatsPanel.STAT_WIS)) {
                retVal.setOption(SearchOptions.ITM_MinWIS, pStats.getOptionValue(ItmStatsPanel.STAT_WIS));
              }
              if (pStats.isActive(ItmStatsPanel.STAT_CHA)) {
                retVal.setOption(SearchOptions.ITM_MinCHA, pStats.getOptionValue(ItmStatsPanel.STAT_CHA));
              }
              break;
            }
            case ID_PRICE:
              retVal.setOption(SearchOptions.ITM_Price, getOptionPrice());
              break;
            case ID_ENCHANTMENT:
              retVal.setOption(SearchOptions.ITM_Enchantment, getOptionEnchantment());
              break;
            case ID_ABILITY: {
              SearchOptions ability = new SearchOptions(SearchOptions.getResourceName(SearchOptions.ITM_Ability));
              if (pAbility.isActive(ItmAbilityPanel.ITEM_TYPE)) {
                ability.setOption(SearchOptions.ITM_Ability_Type, pAbility.getOptionType());
              }
              if (pAbility.isActive(ItmAbilityPanel.ITEM_TARGET)) {
                ability.setOption(SearchOptions.ITM_Ability_Target, pAbility.getOptionTarget());
              }
              if (pAbility.isActive(ItmAbilityPanel.ITEM_LAUNCHER)) {
                ability.setOption(SearchOptions.ITM_Ability_Launcher, pAbility.getOptionLauncher());
              }
              if (pAbility.isActive(ItmAbilityPanel.ITEM_DAMAGETYPE)) {
                ability.setOption(SearchOptions.ITM_Ability_DamageType, pAbility.getOptionDamageType());
              }
              if (pAbility.isActive(ItmAbilityPanel.ITEM_PROJECTILE)) {
                ability.setOption(SearchOptions.ITM_Ability_Projectile, pAbility.getOptionProjectile());
              }
              if (pAbility.isActive(ItmAbilityPanel.ITEM_RANGE)) {
                ability.setOption(SearchOptions.ITM_Ability_Range, pAbility.getOptionRange());
              }
              if (pAbility.isActive(ItmAbilityPanel.ITEM_SPEED)) {
                ability.setOption(SearchOptions.ITM_Ability_Speed, pAbility.getOptionSpeed());
              }
              if (pAbility.isActive(ItmAbilityPanel.ITEM_DICECOUNT)) {
                ability.setOption(SearchOptions.ITM_Ability_DiceCount, pAbility.getOptionDiceCount());
              }
              if (pAbility.isActive(ItmAbilityPanel.ITEM_DICESIZE)) {
                ability.setOption(SearchOptions.ITM_Ability_DiceSize, pAbility.getOptionDiceSize());
              }
              if (pAbility.isActive(ItmAbilityPanel.ITEM_CHARGES)) {
                ability.setOption(SearchOptions.ITM_Ability_Charges, pAbility.getOptionCharges());
              }
              if (pAbility.isActive(ItmAbilityPanel.ITEM_EFFECTS)) {
                if (pAbility.pEffects.isActive(0)) {
                  ability.setOption(SearchOptions.ITM_Ability_Effect_Type1, pAbility.getOptionEffects(0));
                }
                if (pAbility.pEffects.isActive(1)) {
                  ability.setOption(SearchOptions.ITM_Ability_Effect_Type2, pAbility.getOptionEffects(1));
                }
                if (pAbility.pEffects.isActive(2)) {
                  ability.setOption(SearchOptions.ITM_Ability_Effect_Type3, pAbility.getOptionEffects(2));
                }
              }
              if (pAbility.isActive(ItmAbilityPanel.ITEM_FLAGS)) {
                ability.setOption(SearchOptions.ITM_Ability_Flags, pAbility.getOptionFlags());
              }
              if (!ability.isEmpty()) {
                ability.setOption(SearchOptions.ITM_Ability_MatchSingle, pAbility.isOptionOneAbilityExclusive());
                retVal.setOption(SearchOptions.ITM_Ability, ability);
              }
              break;
            }
            case ID_EFFECTS:
              if (pEffects.isActive(0)) {
                retVal.setOption(SearchOptions.ITM_Effect_Type1, pEffects.getOptionEffect(0));
              }
              if (pEffects.isActive(1)) {
                retVal.setOption(SearchOptions.ITM_Effect_Type2, pEffects.getOptionEffect(1));
              }
              if (pEffects.isActive(2)) {
                retVal.setOption(SearchOptions.ITM_Effect_Type3, pEffects.getOptionEffect(2));
              }
              break;
            case ID_CUSTOM:
              if (pCustomFilter.isActive(0)) {
                retVal.setOption(SearchOptions.ITM_Custom1, pCustomFilter.getOptionFilter(0));
              }
              if (pCustomFilter.isActive(1)) {
                retVal.setOption(SearchOptions.ITM_Custom2, pCustomFilter.getOptionFilter(1));
              }
              if (pCustomFilter.isActive(2)) {
                retVal.setOption(SearchOptions.ITM_Custom3, pCustomFilter.getOptionFilter(2));
              }
              if (pCustomFilter.isActive(3)) {
                retVal.setOption(SearchOptions.ITM_Custom4, pCustomFilter.getOptionFilter(3));
              }
              break;
          }
        }
      }
      return retVal;
    }

    public String getOptionName() {
      return cbOptions[ID_NAME].isSelected() ? tfName.getText() : "";
    }

    public int getOptionCategory() {
      return cbOptions[ID_CATEGORY].isSelected() ? ((IndexedString) cbCategory.getSelectedItem()).index : 0;
    }

    public String getOptionAppearance() {
      return Utils.getObjectFromString(cbOptions[ID_APPEARANCE], cbAppearance).toString();
    }

    public Couple<Integer, Integer> getOptionPrice() {
      return Utils.getRangeValues(cbOptions[ID_PRICE], sPrice);
    }

    public Couple<Integer, Integer> getOptionEnchantment() {
      return Utils.getRangeValues(cbOptions[ID_ENCHANTMENT], sEnchantment);
    }

    private void init() {
      JCheckBox cb;
      GridBagConstraints c = new GridBagConstraints();

      cb = new JCheckBox("Name:");
      cb.addActionListener(this);
      cbOptions[ID_NAME] = cb;
      cb = new JCheckBox("Flags:");
      cb.addActionListener(this);
      cbOptions[ID_FLAGS] = cb;
      cb = new JCheckBox("Category:");
      cb.addActionListener(this);
      cbOptions[ID_CATEGORY] = cb;
      cb = new JCheckBox("Usability:");
      cb.addActionListener(this);
      cbOptions[ID_USABILITY] = cb;
      cb = new JCheckBox("Appearance:");
      cb.addActionListener(this);
      cbOptions[ID_APPEARANCE] = cb;
      cb = new JCheckBox("Minimum stats:");
      cb.addActionListener(this);
      cbOptions[ID_STATS] = cb;
      cb = new JCheckBox("Price:");
      cb.addActionListener(this);
      cbOptions[ID_PRICE] = cb;
      cb = new JCheckBox("Enchantment:");
      cb.addActionListener(this);
      cbOptions[ID_ENCHANTMENT] = cb;
      cb = new JCheckBox("Item ability:");
      cb.addActionListener(this);
      cbOptions[ID_ABILITY] = cb;
      cb = new JCheckBox("Equipped effects:");
      cb.addActionListener(this);
      cbOptions[ID_EFFECTS] = cb;
      cb = new JCheckBox("Custom filters:");
      cb.addActionListener(this);
      cbOptions[ID_CUSTOM] = cb;

      tfName = Utils.defaultWidth(new JTextField());

      String[] sFlags;
      String[] sCat;
      if ((Boolean) Profile.getProperty(Profile.Key.IS_SUPPORTED_ITM_V11)) {
        sFlags = ItmResource.FLAGS11_ARRAY;
        sCat = ItmResource.CATEGORIES11_ARRAY;
      } else if ((Boolean) Profile.getProperty(Profile.Key.IS_SUPPORTED_ITM_V20)) {
        sFlags = ItmResource.FLAGS_ARRAY;
        sCat = ItmResource.CATEGORIES_ARRAY;
      } else {
        if (Profile.getGame() == Profile.Game.PSTEE) {
          sFlags = ItmResource.FLAGS_PSTEE_ARRAY;
        } else {
          sFlags = ItmResource.FLAGS_ARRAY;
        }
        sCat = ItmResource.CATEGORIES_ARRAY;
      }

      pFlags = new FlagsPanel(4, sFlags);
      bpwFlags = new ButtonPopupWindow(SET_OPTIONS_TEXT, pFlags);

      pUsability = new ItmUsabilityPanel();
      bpwUsability = new ButtonPopupWindow(SET_OPTIONS_TEXT, pUsability);

      cbAppearance = new AutoComboBox<>(ObjectString.from(Profile.getEquippedAppearanceMap()));

      cbCategory = new AutoComboBox<>(IndexedString.createArray(sCat, 0, 0));

      pStats = new ItmStatsPanel();
      bpwStats = new ButtonPopupWindow(SET_OPTIONS_TEXT, pStats);

      pAbility = new ItmAbilityPanel();
      bpwAbility = new ButtonPopupWindow(SET_OPTIONS_TEXT, pAbility);

      sPrice[0] = Utils.createNumberSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 999999, 0, 1);
      sPrice[1] = Utils.createNumberSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 999999, 999999, 1);

      sEnchantment[0] = Utils.createNumberSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 255, 0, 1);
      sEnchantment[1] = Utils.createNumberSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 255, 255, 1);

      pEffects = new EffectsPanel(3, "Equipped effect");
      bpwEffects = new ButtonPopupWindow(SET_OPTIONS_TEXT, pEffects);

      pCustomFilter = new CustomFilterPanel(4, null);
      bpwCustomFilter = new ButtonPopupWindow(SET_OPTIONS_TEXT, pCustomFilter);

      JPanel pOptions = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_NAME], c);
      c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 0), 0, 0);
      pOptions.add(tfName, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_FLAGS], c);
      c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwFlags, c);
      c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_CATEGORY], c);
      c = ViewerUtil.setGBC(c, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(cbCategory, c);
      c = ViewerUtil.setGBC(c, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_USABILITY], c);
      c = ViewerUtil.setGBC(c, 1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwUsability, c);
      c = ViewerUtil.setGBC(c, 0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_APPEARANCE], c);
      c = ViewerUtil.setGBC(c, 1, 4, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(cbAppearance, c);
      c = ViewerUtil.setGBC(c, 0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_STATS], c);
      c = ViewerUtil.setGBC(c, 1, 5, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwStats, c);

      c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_PRICE], c);
      c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 0), 0, 0);
      pOptions.add(Utils.createNumberRangePanel(sPrice[0], sPrice[1]), c);
      c = ViewerUtil.setGBC(c, 2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_ENCHANTMENT], c);
      c = ViewerUtil.setGBC(c, 3, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(Utils.createNumberRangePanel(sEnchantment[0], sEnchantment[1]), c);
      c = ViewerUtil.setGBC(c, 2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_ABILITY], c);
      c = ViewerUtil.setGBC(c, 3, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwAbility, c);
      c = ViewerUtil.setGBC(c, 2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_EFFECTS], c);
      c = ViewerUtil.setGBC(c, 3, 3, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwEffects, c);
      c = ViewerUtil.setGBC(c, 2, 4, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_CUSTOM], c);
      c = ViewerUtil.setGBC(c, 3, 4, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwCustomFilter, c);

      triggerActions(cbOptions);

      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 8), 0, 0);
      add(pOptions, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
          new Insets(0, 0, 0, 0), 0, 0);
      add(new JPanel(), c);
    }
  }

  private static class OptionsPROPanel extends OptionsBasePanel {
    // indices of the options checkboxes
    public static final int ID_ANIMATION = 0;
    public static final int ID_TYPE = 1;
    public static final int ID_SPEED = 2;
    public static final int ID_BEHAVIOR = 3;
    public static final int ID_FLAGS = 4;
    public static final int ID_AREA_FLAGS = 5;
    public static final int ID_TRAP_SIZE = 6;
    public static final int ID_EXPLOSION_SIZE = 7;
    public static final int ID_EXPLOSION_EFFECT = 8;
    public static final int ID_CUSTOM = 9;

    public static final int OPTIONS_COUNT = 10;

    private final JSpinner[] sSpeed = new JSpinner[2];
    private final JSpinner[] sTrapSize = new JSpinner[2];
    private final JSpinner[] sExplosionSize = new JSpinner[2];

    private FlagsPanel pBehavior;
    private FlagsPanel pFlags;
    private FlagsPanel pAreaFlags;
    private CustomFilterPanel pCustomFilter;
    private ButtonPopupWindow bpwFlags;
    private ButtonPopupWindow bpwBehavior;
    private ButtonPopupWindow bpwAreaFlags;
    private ButtonPopupWindow bpwCustomFilter;
    private JComboBox<ResourceEntry> cbAnimation;
    private JComboBox<IndexedString> cbType;
    private JComboBox<IndexedString> cbExplosionEffect;

    public OptionsPROPanel(SearchResource searchResource) {
      super(searchResource, OPTIONS_COUNT);
      init();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      if (event.getSource() instanceof JCheckBox) {
        int idx = Utils.getOptionIndex(cbOptions, event.getSource());
        if (idx >= 0) {
          switch (idx) {
            case ID_ANIMATION:
              cbAnimation.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                cbAnimation.requestFocusInWindow();
              }
              break;
            case ID_TYPE:
              cbType.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                cbType.requestFocusInWindow();
              }
              break;
            case ID_SPEED:
              sSpeed[0].setEnabled(cbOptions[idx].isSelected());
              sSpeed[1].setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                sSpeed[0].requestFocusInWindow();
              }
              break;
            case ID_BEHAVIOR:
              bpwBehavior.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwBehavior.requestFocusInWindow();
              }
              break;
            case ID_FLAGS:
              bpwFlags.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwFlags.requestFocusInWindow();
              }
              break;
            case ID_AREA_FLAGS:
              bpwAreaFlags.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwAreaFlags.requestFocusInWindow();
              }
              break;
            case ID_TRAP_SIZE:
              sTrapSize[0].setEnabled(cbOptions[idx].isSelected());
              sTrapSize[1].setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                sTrapSize[0].requestFocusInWindow();
              }
              break;
            case ID_EXPLOSION_SIZE:
              sExplosionSize[0].setEnabled(cbOptions[idx].isSelected());
              sExplosionSize[1].setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                sExplosionSize[0].requestFocusInWindow();
              }
              break;
            case ID_EXPLOSION_EFFECT:
              cbExplosionEffect.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                cbExplosionEffect.requestFocusInWindow();
              }
              break;
            case ID_CUSTOM:
              bpwCustomFilter.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwCustomFilter.requestFocusInWindow();
              }
              break;
          }
          fireOptionsPropertyChanged();
        }
      }
    }

    @Override
    public SearchOptions getOptions() {
      SearchOptions retVal = new SearchOptions(SearchOptions.getResourceName(SearchOptions.PRO));
      for (int idx = 0; idx < cbOptions.length; idx++) {
        if (isOptionEnabled(idx)) {
          switch (idx) {
            case ID_ANIMATION:
              retVal.setOption(SearchOptions.PRO_Animation, getOptionAnimation());
              break;
            case ID_TYPE:
              retVal.setOption(SearchOptions.PRO_Type, getOptionType());
              break;
            case ID_SPEED:
              retVal.setOption(SearchOptions.PRO_Speed, getOptionSpeed());
              break;
            case ID_BEHAVIOR:
              retVal.setOption(SearchOptions.PRO_Behavior, pBehavior.getOptionFlags());
              break;
            case ID_FLAGS:
              retVal.setOption(SearchOptions.PRO_Flags, pFlags.getOptionFlags());
              break;
            case ID_AREA_FLAGS:
              retVal.setOption(SearchOptions.PRO_AreaFlags, pAreaFlags.getOptionFlags());
              break;
            case ID_TRAP_SIZE:
              retVal.setOption(SearchOptions.PRO_TrapSize, getOptionTrapSize());
              break;
            case ID_EXPLOSION_SIZE:
              retVal.setOption(SearchOptions.PRO_ExplosionSize, getOptionExplosionSize());
              break;
            case ID_EXPLOSION_EFFECT:
              retVal.setOption(SearchOptions.PRO_ExplosionEffect, getOptionExplosionEffect());
              break;
            case ID_CUSTOM:
              if (pCustomFilter.isActive(0)) {
                retVal.setOption(SearchOptions.PRO_Custom1, pCustomFilter.getOptionFilter(0));
              }
              if (pCustomFilter.isActive(1)) {
                retVal.setOption(SearchOptions.PRO_Custom2, pCustomFilter.getOptionFilter(1));
              }
              if (pCustomFilter.isActive(2)) {
                retVal.setOption(SearchOptions.PRO_Custom3, pCustomFilter.getOptionFilter(2));
              }
              if (pCustomFilter.isActive(3)) {
                retVal.setOption(SearchOptions.PRO_Custom4, pCustomFilter.getOptionFilter(3));
              }
              break;
          }
        }
      }
      return retVal;
    }

    public String getOptionAnimation() {
      return Utils.getResourceName(cbOptions[ID_ANIMATION], cbAnimation);
    }

    public int getOptionType() {
      return (Integer) Utils.getObjectFromString(cbOptions[ID_TYPE], cbType);
    }

    public Couple<Integer, Integer> getOptionSpeed() {
      return Utils.getRangeValues(cbOptions[ID_SPEED], sSpeed);
    }

    public Couple<Integer, Integer> getOptionTrapSize() {
      return Utils.getRangeValues(cbOptions[ID_TRAP_SIZE], sTrapSize);
    }

    public Couple<Integer, Integer> getOptionExplosionSize() {
      return Utils.getRangeValues(cbOptions[ID_EXPLOSION_SIZE], sExplosionSize);
    }

    public int getOptionExplosionEffect() {
      return (Integer) Utils.getObjectFromString(cbOptions[ID_EXPLOSION_EFFECT], cbExplosionEffect);
    }

    private void init() {
      JCheckBox cb;
      GridBagConstraints c = new GridBagConstraints();

      cb = new JCheckBox("Proj. animation:");
      cb.addActionListener(this);
      cbOptions[ID_ANIMATION] = cb;
      cb = new JCheckBox("Type:");
      cb.addActionListener(this);
      cbOptions[ID_TYPE] = cb;
      cb = new JCheckBox("Speed:");
      cb.addActionListener(this);
      cbOptions[ID_SPEED] = cb;
      cb = new JCheckBox("Behavior:");
      cb.addActionListener(this);
      cbOptions[ID_BEHAVIOR] = cb;
      cb = new JCheckBox("Projectile flags:");
      cb.addActionListener(this);
      cbOptions[ID_FLAGS] = cb;
      cb = new JCheckBox("Area flags:");
      cb.addActionListener(this);
      cbOptions[ID_AREA_FLAGS] = cb;
      cb = new JCheckBox("Trap size:");
      cb.addActionListener(this);
      cbOptions[ID_TRAP_SIZE] = cb;
      cb = new JCheckBox("Explosion size:");
      cb.addActionListener(this);
      cbOptions[ID_EXPLOSION_SIZE] = cb;
      cb = new JCheckBox("Explosion effect:");
      cb.addActionListener(this);
      cbOptions[ID_EXPLOSION_EFFECT] = cb;
      cb = new JCheckBox("Custom filters:");
      cb.addActionListener(this);
      cbOptions[ID_CUSTOM] = cb;

      cbAnimation = Utils.createNamedResourceComboBox(new String[] { "BAM" }, false);
      cbAnimation.setPreferredSize(Utils.getPrototypeSize(cbAnimation));

      cbType = new AutoComboBox<>(
          IndexedString.createArray(new String[] { "No BAM", "Single target", "Area of effect" }, 0, 1));

      cbExplosionEffect = new AutoComboBox<>(IndexedString.createArray(ProAreaType.PROJECTILES_MAP));

      pBehavior = new FlagsPanel(4, ProResource.BEHAVE_ARRAY);
      bpwBehavior = new ButtonPopupWindow(SET_OPTIONS_TEXT, pBehavior);

      pFlags = new FlagsPanel(4, ProSingleType.FLAGS_ARRAY);
      bpwFlags = new ButtonPopupWindow(SET_OPTIONS_TEXT, pFlags);

      pAreaFlags = new FlagsPanel(4, ProAreaType.AREA_FLAGS_ARRAY);
      bpwAreaFlags = new ButtonPopupWindow(SET_OPTIONS_TEXT, pAreaFlags);

      sSpeed[0] = Utils.createNumberSpinner(Short.MIN_VALUE, Short.MAX_VALUE, 1, 255, 1, 1);
      sSpeed[1] = Utils.createNumberSpinner(Short.MIN_VALUE, Short.MAX_VALUE, 1, 255, 255, 1);

      sTrapSize[0] = Utils.createNumberSpinner(Short.MIN_VALUE, Short.MAX_VALUE, -32768, 32767, 0, 1);
      sTrapSize[1] = Utils.createNumberSpinner(Short.MIN_VALUE, Short.MAX_VALUE, -32768, 32767, 32767, 1);

      sExplosionSize[0] = Utils.createNumberSpinner(Short.MIN_VALUE, Short.MAX_VALUE, -32768, 32767, 0, 1);
      sExplosionSize[1] = Utils.createNumberSpinner(Short.MIN_VALUE, Short.MAX_VALUE, -32768, 32767, 32767, 1);

      pCustomFilter = new CustomFilterPanel(4, null);
      bpwCustomFilter = new ButtonPopupWindow(SET_OPTIONS_TEXT, pCustomFilter);

      JPanel pOptions = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_ANIMATION], c);
      c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 0), 0, 0);
      pOptions.add(cbAnimation, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_TYPE], c);
      c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(cbType, c);
      c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_SPEED], c);
      c = ViewerUtil.setGBC(c, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(Utils.createNumberRangePanel(sSpeed[0], sSpeed[1]), c);
      c = ViewerUtil.setGBC(c, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_BEHAVIOR], c);
      c = ViewerUtil.setGBC(c, 1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwBehavior, c);
      c = ViewerUtil.setGBC(c, 0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_FLAGS], c);
      c = ViewerUtil.setGBC(c, 1, 4, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwFlags, c);

      c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_AREA_FLAGS], c);
      c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 0), 0, 0);
      pOptions.add(bpwAreaFlags, c);
      c = ViewerUtil.setGBC(c, 2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_TRAP_SIZE], c);
      c = ViewerUtil.setGBC(c, 3, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(Utils.createNumberRangePanel(sTrapSize[0], sTrapSize[1]), c);
      c = ViewerUtil.setGBC(c, 2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_EXPLOSION_SIZE], c);
      c = ViewerUtil.setGBC(c, 3, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(Utils.createNumberRangePanel(sExplosionSize[0], sExplosionSize[1]), c);
      c = ViewerUtil.setGBC(c, 2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_EXPLOSION_EFFECT], c);
      c = ViewerUtil.setGBC(c, 3, 3, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(cbExplosionEffect, c);
      c = ViewerUtil.setGBC(c, 2, 4, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_CUSTOM], c);
      c = ViewerUtil.setGBC(c, 3, 4, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwCustomFilter, c);

      triggerActions(cbOptions);

      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 8), 0, 0);
      add(pOptions, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
          new Insets(0, 0, 0, 0), 0, 0);
      add(new JPanel(), c);
    }
  }

  private static class OptionsSPLPanel extends OptionsBasePanel {
    // indices of the options checkboxes
    public static final int ID_NAME = 0;
    public static final int ID_FLAGS = 1;
    public static final int ID_SPELL_TYPE = 2;
    public static final int ID_EXCLUSION = 3;
    public static final int ID_CASTING_ANIMATION = 4;
    public static final int ID_PRIMARY_TYPE = 5;
    public static final int ID_SECONDARY_TYPE = 6;
    public static final int ID_LEVEL = 7;
    public static final int ID_ABILITY = 8;
    public static final int ID_EFFECTS = 9;
    public static final int ID_CUSTOM = 10;

    public static final int OPTIONS_COUNT = 11;

    private final JSpinner[] sLevel = new JSpinner[2];

    private FlagsPanel pFlags;
    private FlagsPanel pExclusion;
    private SplAbilityPanel pAbility;
    private EffectsPanel pEffects;
    private CustomFilterPanel pCustomFilter;
    private JTextField tfName;
    private ButtonPopupWindow bpwFlags;
    private ButtonPopupWindow bpwExclusion;
    private ButtonPopupWindow bpwAbility;
    private ButtonPopupWindow bpwEffects;
    private ButtonPopupWindow bpwCustomFilter;
    private JComboBox<IndexedString> cbSpellType;
    private JComboBox<IndexedString> cbCastingAnim;
    private JComboBox<IndexedString> cbSecondary;
    private JComboBox<IndexedString> cbPrimary;

    public OptionsSPLPanel(SearchResource searchResource) {
      super(searchResource, OPTIONS_COUNT);
      init();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      if (event.getSource() instanceof JCheckBox) {
        int idx = Utils.getOptionIndex(cbOptions, event.getSource());
        if (idx >= 0) {
          switch (idx) {
            case ID_NAME:
              tfName.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                tfName.requestFocusInWindow();
              }
              break;
            case ID_FLAGS:
              bpwFlags.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwFlags.requestFocusInWindow();
              }
              break;
            case ID_SPELL_TYPE:
              cbSpellType.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                cbSpellType.requestFocusInWindow();
              }
              break;
            case ID_EXCLUSION:
              bpwExclusion.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwExclusion.requestFocusInWindow();
              }
              break;
            case ID_CASTING_ANIMATION:
              cbCastingAnim.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                cbCastingAnim.requestFocusInWindow();
              }
              break;
            case ID_PRIMARY_TYPE:
              cbPrimary.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                cbPrimary.requestFocusInWindow();
              }
              break;
            case ID_SECONDARY_TYPE:
              cbSecondary.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                cbSecondary.requestFocusInWindow();
              }
              break;
            case ID_LEVEL:
              sLevel[0].setEnabled(cbOptions[idx].isSelected());
              sLevel[1].setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                sLevel[0].requestFocusInWindow();
              }
              break;
            case ID_ABILITY:
              bpwAbility.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwAbility.requestFocusInWindow();
              }
              break;
            case ID_EFFECTS:
              bpwEffects.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwEffects.requestFocusInWindow();
              }
              break;
            case ID_CUSTOM:
              bpwCustomFilter.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwCustomFilter.requestFocusInWindow();
              }
              break;
          }
          fireOptionsPropertyChanged();
        }
      }
    }

    @Override
    public SearchOptions getOptions() {
      SearchOptions retVal = new SearchOptions(SearchOptions.getResourceName(SearchOptions.SPL));

      for (int idx = 0; idx < cbOptions.length; idx++) {
        if (isOptionEnabled(idx)) {
          switch (idx) {
            case ID_NAME:
              retVal.setOption(SearchOptions.SPL_Name, getOptionName());
              break;
            case ID_FLAGS:
              retVal.setOption(SearchOptions.SPL_Flags, pFlags.getOptionFlags());
              break;
            case ID_SPELL_TYPE:
              retVal.setOption(SearchOptions.SPL_SpellType, getOptionSpellType());
              break;
            case ID_EXCLUSION:
              retVal.setOption(SearchOptions.SPL_Exclusion, pExclusion.getOptionFlags());
              break;
            case ID_CASTING_ANIMATION:
              retVal.setOption(SearchOptions.SPL_CastingAnimation, getOptionCastingAnimation());
              break;
            case ID_PRIMARY_TYPE:
              retVal.setOption(SearchOptions.SPL_PrimaryType, getOptionPrimaryType());
              break;
            case ID_SECONDARY_TYPE:
              retVal.setOption(SearchOptions.SPL_SecondaryType, getOptionSecondaryType());
              break;
            case ID_LEVEL:
              retVal.setOption(SearchOptions.SPL_Level, getOptionLevel());
              break;
            case ID_ABILITY: {
              SearchOptions ability = new SearchOptions(SearchOptions.getResourceName(SearchOptions.SPL_Ability));
              if (pAbility.isActive(SplAbilityPanel.SPELL_TYPE)) {
                ability.setOption(SearchOptions.SPL_Ability_Type, pAbility.getOptionType());
              }
              if (pAbility.isActive(SplAbilityPanel.SPELL_LOCATION)) {
                ability.setOption(SearchOptions.SPL_Ability_Location, pAbility.getOptionLocation());
              }
              if (pAbility.isActive(SplAbilityPanel.SPELL_TARGET)) {
                ability.setOption(SearchOptions.SPL_Ability_Target, pAbility.getOptionTarget());
              }
              if (pAbility.isActive(SplAbilityPanel.SPELL_RANGE)) {
                ability.setOption(SearchOptions.SPL_Ability_Range, pAbility.getOptionRange());
              }
              if (pAbility.isActive(SplAbilityPanel.SPELL_LEVEL)) {
                ability.setOption(SearchOptions.SPL_Ability_Level, pAbility.getOptionLevel());
              }
              if (pAbility.isActive(SplAbilityPanel.SPELL_SPEED)) {
                ability.setOption(SearchOptions.SPL_Ability_Speed, pAbility.getOptionSpeed());
              }
              if (pAbility.isActive(SplAbilityPanel.SPELL_PROJECTILE)) {
                ability.setOption(SearchOptions.SPL_Ability_Projectile, pAbility.getOptionProjectile());
              }
              if (pAbility.isActive(SplAbilityPanel.SPELL_EFFECTS)) {
                if (pAbility.pEffects.isActive(0)) {
                  ability.setOption(SearchOptions.SPL_Ability_Effect_Type1, pAbility.getOptionEffects(0));
                }
                if (pAbility.pEffects.isActive(1)) {
                  ability.setOption(SearchOptions.SPL_Ability_Effect_Type2, pAbility.getOptionEffects(1));
                }
                if (pAbility.pEffects.isActive(2)) {
                  ability.setOption(SearchOptions.SPL_Ability_Effect_Type3, pAbility.getOptionEffects(2));
                }
              }
              if (!ability.isEmpty()) {
                ability.setOption(SearchOptions.SPL_Ability_MatchSingle, pAbility.isOptionOneAbilityExclusive());
                retVal.setOption(SearchOptions.SPL_Ability, ability);
              }
              break;
            }
            case ID_EFFECTS:
              if (pEffects.isActive(0)) {
                retVal.setOption(SearchOptions.SPL_Effect_Type1, pEffects.getOptionEffect(0));
              }
              if (pEffects.isActive(1)) {
                retVal.setOption(SearchOptions.SPL_Effect_Type2, pEffects.getOptionEffect(1));
              }
              if (pEffects.isActive(2)) {
                retVal.setOption(SearchOptions.SPL_Effect_Type3, pEffects.getOptionEffect(2));
              }
              break;
            case ID_CUSTOM:
              if (pCustomFilter.isActive(0)) {
                retVal.setOption(SearchOptions.SPL_Custom1, pCustomFilter.getOptionFilter(0));
              }
              if (pCustomFilter.isActive(1)) {
                retVal.setOption(SearchOptions.SPL_Custom2, pCustomFilter.getOptionFilter(1));
              }
              if (pCustomFilter.isActive(2)) {
                retVal.setOption(SearchOptions.SPL_Custom3, pCustomFilter.getOptionFilter(2));
              }
              if (pCustomFilter.isActive(3)) {
                retVal.setOption(SearchOptions.SPL_Custom4, pCustomFilter.getOptionFilter(3));
              }
              break;
          }
        }
      }
      return retVal;
    }

    public String getOptionName() {
      return cbOptions[ID_NAME].isSelected() ? tfName.getText() : "";
    }

    public int getOptionSpellType() {
      return (Integer) Utils.getObjectFromString(cbOptions[ID_SPELL_TYPE], cbSpellType);
    }

    public int getOptionCastingAnimation() {
      return (Integer) Utils.getObjectFromString(cbOptions[ID_CASTING_ANIMATION], cbCastingAnim);
    }

    public int getOptionPrimaryType() {
      return (Integer) Utils.getObjectFromString(cbOptions[ID_PRIMARY_TYPE], cbPrimary);
    }

    public int getOptionSecondaryType() {
      return (Integer) Utils.getObjectFromString(cbOptions[ID_SECONDARY_TYPE], cbSecondary);
    }

    public Couple<Integer, Integer> getOptionLevel() {
      return Utils.getRangeValues(cbOptions[ID_LEVEL], sLevel);
    }

    private void init() {
      JCheckBox cb;
      GridBagConstraints c = new GridBagConstraints();

      cb = new JCheckBox("Name:");
      cb.addActionListener(this);
      cbOptions[ID_NAME] = cb;
      cb = new JCheckBox("Flags:");
      cb.addActionListener(this);
      cbOptions[ID_FLAGS] = cb;
      cb = new JCheckBox("Spell type:");
      cb.addActionListener(this);
      cbOptions[ID_SPELL_TYPE] = cb;
      cb = new JCheckBox("Exclusion flags:");
      cb.addActionListener(this);
      cbOptions[ID_EXCLUSION] = cb;
      cb = new JCheckBox("Casting animation:");
      cb.addActionListener(this);
      cbOptions[ID_CASTING_ANIMATION] = cb;
      cb = new JCheckBox("Primary type:");
      cb.addActionListener(this);
      cbOptions[ID_PRIMARY_TYPE] = cb;
      cb = new JCheckBox("Secondary type:");
      cb.addActionListener(this);
      cbOptions[ID_SECONDARY_TYPE] = cb;
      cb = new JCheckBox("Spell level:");
      cb.addActionListener(this);
      cbOptions[ID_LEVEL] = cb;
      cb = new JCheckBox("Spell ability:");
      cb.addActionListener(this);
      cbOptions[ID_ABILITY] = cb;
      cb = new JCheckBox("Effect opcodes:");
      cb.addActionListener(this);
      cbOptions[ID_EFFECTS] = cb;
      cb = new JCheckBox("Custom filters:");
      cb.addActionListener(this);
      cbOptions[ID_CUSTOM] = cb;

      tfName = Utils.defaultWidth(new JTextField());

      pFlags = new FlagsPanel(4, SplResource.SPELL_FLAGS_ARRAY);
      bpwFlags = new ButtonPopupWindow(SET_OPTIONS_TEXT, pFlags);

      cbSpellType = new AutoComboBox<>(IndexedString.createArray(SplResource.SPELL_TYPE_ARRAY, 0, 0));

      pExclusion = new FlagsPanel(4, SplResource.EXCLUDE_COMBINED_ARRAY);
      bpwExclusion = new ButtonPopupWindow(SET_OPTIONS_TEXT, pExclusion);

      if (Profile.getGame() == Profile.Game.PST || Profile.getGame() == Profile.Game.PSTEE) {
        cbCastingAnim = new AutoComboBox<>(IndexedString.createArray(SplResource.ANIM_PST_ARRAY, 0, 0));
      } else {
        cbCastingAnim = new AutoComboBox<>(IndexedString.createArray(SplResource.ANIM_ARRAY, 0, 0));
      }

      cbPrimary = new AutoComboBox<>(IndexedString.createArray(PriTypeBitmap.getTypeArray(), 0, 0));

      cbSecondary = new AutoComboBox<>(IndexedString.createArray(SecTypeBitmap.getTypeArray(), 0, 0));

      pAbility = new SplAbilityPanel();
      bpwAbility = new ButtonPopupWindow(SET_OPTIONS_TEXT, pAbility);

      sLevel[0] = Utils.createNumberSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, -32768, 32767, 0, 1);
      sLevel[1] = Utils.createNumberSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, -32768, 32767, 10, 1);

      pEffects = new EffectsPanel(3, "Effect opcode");
      bpwEffects = new ButtonPopupWindow(SET_OPTIONS_TEXT, pEffects);

      pCustomFilter = new CustomFilterPanel(4, null);
      bpwCustomFilter = new ButtonPopupWindow(SET_OPTIONS_TEXT, pCustomFilter);

      JPanel pOptions = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_NAME], c);
      c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 0), 0, 0);
      pOptions.add(tfName, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_FLAGS], c);
      c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwFlags, c);
      c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_SPELL_TYPE], c);
      c = ViewerUtil.setGBC(c, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(cbSpellType, c);
      c = ViewerUtil.setGBC(c, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_EXCLUSION], c);
      c = ViewerUtil.setGBC(c, 1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwExclusion, c);
      c = ViewerUtil.setGBC(c, 0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_CASTING_ANIMATION], c);
      c = ViewerUtil.setGBC(c, 1, 4, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(cbCastingAnim, c);
      c = ViewerUtil.setGBC(c, 0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_LEVEL], c);
      c = ViewerUtil.setGBC(c, 1, 5, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(Utils.createNumberRangePanel(sLevel[0], sLevel[1]), c);

      c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_PRIMARY_TYPE], c);
      c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 0), 0, 0);
      pOptions.add(cbPrimary, c);
      c = ViewerUtil.setGBC(c, 2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_SECONDARY_TYPE], c);
      c = ViewerUtil.setGBC(c, 3, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(cbSecondary, c);
      c = ViewerUtil.setGBC(c, 2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_ABILITY], c);
      c = ViewerUtil.setGBC(c, 3, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwAbility, c);
      c = ViewerUtil.setGBC(c, 2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_EFFECTS], c);
      c = ViewerUtil.setGBC(c, 3, 3, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwEffects, c);
      c = ViewerUtil.setGBC(c, 2, 4, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_CUSTOM], c);
      c = ViewerUtil.setGBC(c, 3, 4, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwCustomFilter, c);

      triggerActions(cbOptions);

      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 8), 0, 0);
      add(pOptions, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
          new Insets(0, 0, 0, 0), 0, 0);
      add(new JPanel(), c);
    }
  }

  private static class OptionsSTOPanel extends OptionsBasePanel {
    // indices of the options checkboxes
    public static final int ID_NAME = 0;
    public static final int ID_TYPE = 1;
    public static final int ID_FLAGS = 2;
    public static final int ID_PURCHASED = 3;
    public static final int ID_ROOMS_AVAILABLE = 4;
    public static final int ID_DEPRECIATION = 5;
    public static final int ID_SELL_MARKUP = 6;
    public static final int ID_BUY_MARKUP = 7;
    public static final int ID_STEALING = 8;
    public static final int ID_CAPACITY = 9;
    public static final int ID_ITEMS = 10;
    public static final int ID_CUSTOM = 11;

    public static final int OPTIONS_COUNT = 12;

    private final JSpinner[] sSellMarkup = new JSpinner[2];
    private final JSpinner[] sBuyMarkup = new JSpinner[2];
    private final JSpinner[] sStealing = new JSpinner[2];
    private final JSpinner[] sCapacity = new JSpinner[2];
    private final JSpinner[] sDepreciation = new JSpinner[2];

    private FlagsPanel pFlags;
    private FlagsPanel pRoomsAvailable;
    private StoCategoriesPanel pPurchased;
    /** Allows to specify filter for items provided by stores. */
    private ResourcesFilterPanel pItemsForSale;
    private CustomFilterPanel pCustomFilter;
    private JTextField tfName;
    private ButtonPopupWindow bpwFlags;
    private ButtonPopupWindow bpwPurchased;
    private ButtonPopupWindow bpwRoomsAvailable;
    private ButtonPopupWindow bpwItemsForSale;
    private ButtonPopupWindow bpwCustomFilter;
    private JComboBox<IndexedString> cbType;

    public OptionsSTOPanel(SearchResource searchResource) {
      super(searchResource, OPTIONS_COUNT);
      init();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      if (event.getSource() instanceof JCheckBox) {
        int idx = Utils.getOptionIndex(cbOptions, event.getSource());
        if (idx >= 0) {
          switch (idx) {
            case ID_NAME:
              tfName.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                tfName.requestFocusInWindow();
              }
              break;
            case ID_TYPE:
              cbType.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                cbType.requestFocusInWindow();
              }
              break;
            case ID_FLAGS:
              bpwFlags.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwFlags.requestFocusInWindow();
              }
              break;
            case ID_PURCHASED:
              bpwPurchased.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwPurchased.requestFocusInWindow();
              }
              break;
            case ID_ROOMS_AVAILABLE:
              bpwRoomsAvailable.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwRoomsAvailable.requestFocusInWindow();
              }
              break;
            case ID_DEPRECIATION:
              sDepreciation[0].setEnabled(cbOptions[idx].isSelected());
              sDepreciation[1].setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                sDepreciation[0].requestFocusInWindow();
              }
              break;
            case ID_SELL_MARKUP:
              sSellMarkup[0].setEnabled(cbOptions[idx].isSelected());
              sSellMarkup[1].setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                sSellMarkup[0].requestFocusInWindow();
              }
              break;
            case ID_BUY_MARKUP:
              sBuyMarkup[0].setEnabled(cbOptions[idx].isSelected());
              sBuyMarkup[1].setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                sBuyMarkup[0].requestFocusInWindow();
              }
              break;
            case ID_STEALING:
              sStealing[0].setEnabled(cbOptions[idx].isSelected());
              sStealing[1].setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                sStealing[0].requestFocusInWindow();
              }
              break;
            case ID_CAPACITY:
              sCapacity[0].setEnabled(cbOptions[idx].isSelected());
              sCapacity[1].setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                sCapacity[0].requestFocusInWindow();
              }
              break;
            case ID_ITEMS:
              bpwItemsForSale.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwItemsForSale.requestFocusInWindow();
              }
              break;
            case ID_CUSTOM:
              bpwCustomFilter.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwCustomFilter.requestFocusInWindow();
              }
              break;
          }
          fireOptionsPropertyChanged();
        }
      }
    }

    @Override
    public SearchOptions getOptions() {
      SearchOptions retVal = new SearchOptions(SearchOptions.getResourceName(SearchOptions.STO));
      for (int idx = 0; idx < cbOptions.length; idx++) {
        if (isOptionEnabled(idx)) {
          switch (idx) {
            case ID_NAME:
              retVal.setOption(SearchOptions.STO_Name, getOptionName());
              break;
            case ID_TYPE:
              retVal.setOption(SearchOptions.STO_Type, getOptionType());
              break;
            case ID_FLAGS:
              retVal.setOption(SearchOptions.STO_Flags, pFlags.getOptionFlags());
              break;
            case ID_PURCHASED:
              if (pPurchased.isActive(0)) {
                retVal.setOption(SearchOptions.STO_Purchased1, pPurchased.getOptionPurchased(0));
              }
              if (pPurchased.isActive(1)) {
                retVal.setOption(SearchOptions.STO_Purchased2, pPurchased.getOptionPurchased(1));
              }
              if (pPurchased.isActive(2)) {
                retVal.setOption(SearchOptions.STO_Purchased3, pPurchased.getOptionPurchased(2));
              }
              if (pPurchased.isActive(3)) {
                retVal.setOption(SearchOptions.STO_Purchased4, pPurchased.getOptionPurchased(3));
              }
              if (pPurchased.isActive(4)) {
                retVal.setOption(SearchOptions.STO_Purchased5, pPurchased.getOptionPurchased(4));
              }
              break;
            case ID_ROOMS_AVAILABLE:
              retVal.setOption(SearchOptions.STO_RoomsAvailable, pRoomsAvailable.getOptionFlags());
              break;
            case ID_DEPRECIATION:
              retVal.setOption(SearchOptions.STO_Depreciation, getOptionDepreciationRate());
              break;
            case ID_SELL_MARKUP:
              retVal.setOption(SearchOptions.STO_SellMarkup, getOptionSellMarkup());
              break;
            case ID_BUY_MARKUP:
              retVal.setOption(SearchOptions.STO_BuyMarkup, getOptionBuyMarkup());
              break;
            case ID_STEALING:
              retVal.setOption(SearchOptions.STO_Stealing, getOptionStealing());
              break;
            case ID_CAPACITY:
              retVal.setOption(SearchOptions.STO_Capacity, getOptionCapacity());
              break;
            case ID_ITEMS:
              if (pItemsForSale.isActive(0)) {
                retVal.setOption(SearchOptions.STO_Item_Item1, pItemsForSale.getResourceName(0));
              }
              if (pItemsForSale.isActive(1)) {
                retVal.setOption(SearchOptions.STO_Item_Item2, pItemsForSale.getResourceName(1));
              }
              if (pItemsForSale.isActive(2)) {
                retVal.setOption(SearchOptions.STO_Item_Item3, pItemsForSale.getResourceName(2));
              }
              if (pItemsForSale.isActive(3)) {
                retVal.setOption(SearchOptions.STO_Item_Item4, pItemsForSale.getResourceName(3));
              }
              if (pItemsForSale.isActive(4)) {
                retVal.setOption(SearchOptions.STO_Item_Item5, pItemsForSale.getResourceName(4));
              }
              break;
            case ID_CUSTOM:
              if (pCustomFilter.isActive(0)) {
                retVal.setOption(SearchOptions.STO_Custom1, pCustomFilter.getOptionFilter(0));
              }
              if (pCustomFilter.isActive(1)) {
                retVal.setOption(SearchOptions.STO_Custom2, pCustomFilter.getOptionFilter(1));
              }
              if (pCustomFilter.isActive(2)) {
                retVal.setOption(SearchOptions.STO_Custom3, pCustomFilter.getOptionFilter(2));
              }
              if (pCustomFilter.isActive(3)) {
                retVal.setOption(SearchOptions.STO_Custom4, pCustomFilter.getOptionFilter(3));
              }
              break;
          }
        }
      }
      return retVal;
    }

    public String getOptionName() {
      return cbOptions[ID_NAME].isSelected() ? tfName.getText() : "";
    }

    public int getOptionType() {
      return (Integer) Utils.getObjectFromString(cbOptions[ID_TYPE], cbType);
    }

    public Couple<Integer, Integer> getOptionDepreciationRate() {
      return Utils.getRangeValues(cbOptions[ID_DEPRECIATION], sDepreciation);
    }

    public Couple<Integer, Integer> getOptionSellMarkup() {
      return Utils.getRangeValues(cbOptions[ID_SELL_MARKUP], sSellMarkup);
    }

    public Couple<Integer, Integer> getOptionBuyMarkup() {
      return Utils.getRangeValues(cbOptions[ID_BUY_MARKUP], sBuyMarkup);
    }

    public Couple<Integer, Integer> getOptionStealing() {
      return Utils.getRangeValues(cbOptions[ID_STEALING], sStealing);
    }

    public Couple<Integer, Integer> getOptionCapacity() {
      return Utils.getRangeValues(cbOptions[ID_CAPACITY], sCapacity);
    }

    private void init() {
      JCheckBox cb;
      GridBagConstraints c = new GridBagConstraints();

      cb = new JCheckBox("Name:");
      cb.addActionListener(this);
      cbOptions[ID_NAME] = cb;
      cb = new JCheckBox("Type:");
      cb.addActionListener(this);
      cbOptions[ID_TYPE] = cb;
      cb = new JCheckBox("Flags:");
      cb.addActionListener(this);
      cbOptions[ID_FLAGS] = cb;
      cb = new JCheckBox("Rooms available:");
      cb.addActionListener(this);
      cbOptions[ID_ROOMS_AVAILABLE] = cb;
      cb = new JCheckBox("Depreciation rate:");
      cb.addActionListener(this);
      cbOptions[ID_DEPRECIATION] = cb;
      cb = new JCheckBox("Sell markup:");
      cb.addActionListener(this);
      cbOptions[ID_SELL_MARKUP] = cb;
      cb = new JCheckBox("Buy markup:");
      cb.addActionListener(this);
      cbOptions[ID_BUY_MARKUP] = cb;
      cb = new JCheckBox("Stealing difficulty:");
      cb.addActionListener(this);
      cbOptions[ID_STEALING] = cb;
      cb = new JCheckBox("Storage capacity:");
      cb.addActionListener(this);
      cbOptions[ID_CAPACITY] = cb;
      cb = new JCheckBox("Types purchased:");
      cb.addActionListener(this);
      cbOptions[ID_PURCHASED] = cb;
      cb = new JCheckBox("Items for sale:");
      cb.addActionListener(this);
      cbOptions[ID_ITEMS] = cb;
      cb = new JCheckBox("Custom filters:");
      cb.addActionListener(this);
      cbOptions[ID_CUSTOM] = cb;

      tfName = Utils.defaultWidth(new JTextField());

      final IndexedString[] types;
      if (Profile.getEngine() == Profile.Engine.BG2 || Profile.isEnhancedEdition()) {
        types = IndexedString.createArray(StoResource.TYPE_BG2_ARRAY, 0, 0);
      } else {
        types = IndexedString.createArray(StoResource.TYPE9_ARRAY, 0, 0);
      }
      cbType = new AutoComboBox<>(types);

      pFlags = new FlagsPanel(4, StoResource.FLAGS_BG2_ARRAY);
      bpwFlags = new ButtonPopupWindow(SET_OPTIONS_TEXT, pFlags);

      pRoomsAvailable = new FlagsPanel(4, StoResource.ROOMS_ARRAY);
      bpwRoomsAvailable = new ButtonPopupWindow(SET_OPTIONS_TEXT, pRoomsAvailable);

      pPurchased = new StoCategoriesPanel(5);
      bpwPurchased = new ButtonPopupWindow(SET_OPTIONS_TEXT, pPurchased);

      pItemsForSale = new ResourcesFilterPanel(5, "Item for sale %d:", "ITM", true);
      bpwItemsForSale = new ButtonPopupWindow(SET_OPTIONS_TEXT, pItemsForSale);

      sDepreciation[0] = Utils.createNumberSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, -32768, 32767, 0, 1);
      sDepreciation[1] = Utils.createNumberSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, -32768, 32767, 32767, 1);

      sSellMarkup[0] = Utils.createNumberSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, -32768, 32767, 0, 1);
      sSellMarkup[1] = Utils.createNumberSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, -32768, 32767, 200, 1);

      sBuyMarkup[0] = Utils.createNumberSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, -32768, 32767, 0, 1);
      sBuyMarkup[1] = Utils.createNumberSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, -32768, 32767, 100, 1);

      sStealing[0] = Utils.createNumberSpinner(Short.MIN_VALUE, Short.MAX_VALUE, -32768, 32767, 0, 1);
      sStealing[1] = Utils.createNumberSpinner(Short.MIN_VALUE, Short.MAX_VALUE, -32768, 32767, 100, 1);

      sCapacity[0] = Utils.createNumberSpinner(0, 65535, 0, 65535, 0, 1);
      sCapacity[1] = Utils.createNumberSpinner(0, 65535, 0, 65535, 65535, 1);

      pCustomFilter = new CustomFilterPanel(4, null);
      bpwCustomFilter = new ButtonPopupWindow(SET_OPTIONS_TEXT, pCustomFilter);

      JPanel pOptions = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_NAME], c);
      c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 0), 0, 0);
      pOptions.add(tfName, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_TYPE], c);
      c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(cbType, c);
      c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_FLAGS], c);
      c = ViewerUtil.setGBC(c, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwFlags, c);
      c = ViewerUtil.setGBC(c, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_ROOMS_AVAILABLE], c);
      c = ViewerUtil.setGBC(c, 1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwRoomsAvailable, c);
      c = ViewerUtil.setGBC(c, 0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_SELL_MARKUP], c);
      c = ViewerUtil.setGBC(c, 1, 4, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(Utils.createNumberRangePanel(sSellMarkup[0], sSellMarkup[1]), c);
      c = ViewerUtil.setGBC(c, 0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_BUY_MARKUP], c);
      c = ViewerUtil.setGBC(c, 1, 5, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(Utils.createNumberRangePanel(sBuyMarkup[0], sBuyMarkup[1]), c);

      c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_DEPRECIATION], c);
      c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 0), 0, 0);
      pOptions.add(Utils.createNumberRangePanel(sDepreciation[0], sDepreciation[1]), c);
      c = ViewerUtil.setGBC(c, 2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_STEALING], c);
      c = ViewerUtil.setGBC(c, 3, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(Utils.createNumberRangePanel(sStealing[0], sStealing[1]), c);
      c = ViewerUtil.setGBC(c, 2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_CAPACITY], c);
      c = ViewerUtil.setGBC(c, 3, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(Utils.createNumberRangePanel(sCapacity[0], sCapacity[1]), c);
      c = ViewerUtil.setGBC(c, 2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_PURCHASED], c);
      c = ViewerUtil.setGBC(c, 3, 3, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwPurchased, c);
      c = ViewerUtil.setGBC(c, 2, 4, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_ITEMS], c);
      c = ViewerUtil.setGBC(c, 3, 4, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwItemsForSale, c);
      c = ViewerUtil.setGBC(c, 2, 5, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_CUSTOM], c);
      c = ViewerUtil.setGBC(c, 3, 5, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwCustomFilter, c);

      triggerActions(cbOptions);

      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 8), 0, 0);
      add(pOptions, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
          new Insets(0, 0, 0, 0), 0, 0);
      add(new JPanel(), c);
    }
  }

  private static class OptionsVVCPanel extends OptionsBasePanel {
    // indices of the options checkboxes
    public static final int ID_ANIMATION = 0;
    public static final int ID_FLAGS = 1;
    public static final int ID_COLOR = 2;
    public static final int ID_SEQUENCING = 3;
    public static final int ID_ORIENTATION = 4;
    public static final int ID_CUSTOM = 5;

    public static final int OPTIONS_COUNT = 6;

    private FlagsPanel pFlags;
    private FlagsPanel pColor;
    private FlagsPanel pSequencing;
    private FlagsPanel pOrientation;
    private CustomFilterPanel pCustomFilter;
    private ButtonPopupWindow bpwFlags;
    private ButtonPopupWindow bpwColor;
    private ButtonPopupWindow bpwSequencing;
    private ButtonPopupWindow bpwOrientation;
    private ButtonPopupWindow bpwCustomFilter;
    private JComboBox<ResourceEntry> cbAnimation;

    public OptionsVVCPanel(SearchResource searchResource) {
      super(searchResource, OPTIONS_COUNT);
      init();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      if (event.getSource() instanceof JCheckBox) {
        int idx = Utils.getOptionIndex(cbOptions, event.getSource());
        if (idx >= 0) {
          switch (idx) {
            case ID_ANIMATION:
              cbAnimation.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                cbAnimation.requestFocusInWindow();
              }
              break;
            case ID_FLAGS:
              bpwFlags.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwFlags.requestFocusInWindow();
              }
              break;
            case ID_COLOR:
              bpwColor.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwColor.requestFocusInWindow();
              }
              break;
            case ID_SEQUENCING:
              bpwSequencing.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwSequencing.requestFocusInWindow();
              }
              break;
            case ID_ORIENTATION:
              bpwOrientation.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwOrientation.requestFocusInWindow();
              }
              break;
            case ID_CUSTOM:
              bpwCustomFilter.setEnabled(cbOptions[idx].isSelected());
              if (cbOptions[idx].isSelected()) {
                bpwCustomFilter.requestFocusInWindow();
              }
              break;
          }
          fireOptionsPropertyChanged();
        }
      }
    }

    @Override
    public SearchOptions getOptions() {
      SearchOptions retVal = new SearchOptions(SearchOptions.getResourceName(SearchOptions.VVC));
      for (int idx = 0; idx < cbOptions.length; idx++) {
        if (isOptionEnabled(idx)) {
          switch (idx) {
            case ID_ANIMATION:
              retVal.setOption(SearchOptions.VVC_Animation, getOptionAnimation());
              break;
            case ID_FLAGS:
              retVal.setOption(SearchOptions.VVC_Flags, pFlags.getOptionFlags());
              break;
            case ID_COLOR:
              retVal.setOption(SearchOptions.VVC_ColorAdjustment, pColor.getOptionFlags());
              break;
            case ID_SEQUENCING:
              retVal.setOption(SearchOptions.VVC_Sequencing, pSequencing.getOptionFlags());
              break;
            case ID_ORIENTATION:
              retVal.setOption(SearchOptions.VVC_Orientation, pOrientation.getOptionFlags());
              break;
            case ID_CUSTOM:
              if (pCustomFilter.isActive(0)) {
                retVal.setOption(SearchOptions.VVC_Custom1, pCustomFilter.getOptionFilter(0));
              }
              if (pCustomFilter.isActive(1)) {
                retVal.setOption(SearchOptions.VVC_Custom2, pCustomFilter.getOptionFilter(1));
              }
              if (pCustomFilter.isActive(2)) {
                retVal.setOption(SearchOptions.VVC_Custom3, pCustomFilter.getOptionFilter(2));
              }
              if (pCustomFilter.isActive(3)) {
                retVal.setOption(SearchOptions.VVC_Custom4, pCustomFilter.getOptionFilter(3));
              }
              break;
          }
        }
      }
      return retVal;
    }

    public String getOptionAnimation() {
      return Utils.getResourceName(cbOptions[ID_ANIMATION], cbAnimation);
    }

    private void init() {
      JCheckBox cb;
      GridBagConstraints c = new GridBagConstraints();

      cb = new JCheckBox("Animation:");
      cb.addActionListener(this);
      cbOptions[ID_ANIMATION] = cb;
      cb = new JCheckBox("Drawing flags:");
      cb.addActionListener(this);
      cbOptions[ID_FLAGS] = cb;
      cb = new JCheckBox("Color adjustment:");
      cb.addActionListener(this);
      cbOptions[ID_COLOR] = cb;
      cb = new JCheckBox("Sequencing:");
      cb.addActionListener(this);
      cbOptions[ID_SEQUENCING] = cb;
      cb = new JCheckBox("Travel orientation:");
      cb.addActionListener(this);
      cbOptions[ID_ORIENTATION] = cb;
      cb = new JCheckBox("Custom filters:");
      cb.addActionListener(this);
      cbOptions[ID_CUSTOM] = cb;

      cbAnimation = Utils.createNamedResourceComboBox(new String[] { "BAM" }, false);

      pFlags = new FlagsPanel(2, VvcResource.TRANSPARENCY_ARRAY);
      bpwFlags = new ButtonPopupWindow(SET_OPTIONS_TEXT, pFlags);

      pColor = new FlagsPanel(2, VvcResource.TINT_ARRAY);
      bpwColor = new ButtonPopupWindow(SET_OPTIONS_TEXT, pColor);

      pSequencing = new FlagsPanel(4, VvcResource.SEQ_ARRAY);
      bpwSequencing = new ButtonPopupWindow(SET_OPTIONS_TEXT, pSequencing);

      pOrientation = new FlagsPanel(4, VvcResource.FACE_ARRAY);
      bpwOrientation = new ButtonPopupWindow(SET_OPTIONS_TEXT, pOrientation);

      pCustomFilter = new CustomFilterPanel(4, null);
      bpwCustomFilter = new ButtonPopupWindow(SET_OPTIONS_TEXT, pCustomFilter);

      JPanel pOptions = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_ANIMATION], c);
      c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 0), 0, 0);
      pOptions.add(cbAnimation, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_FLAGS], c);
      c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwFlags, c);
      c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_COLOR], c);
      c = ViewerUtil.setGBC(c, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwColor, c);

      c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_SEQUENCING], c);
      c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 0), 0, 0);
      pOptions.add(bpwSequencing, c);
      c = ViewerUtil.setGBC(c, 2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_ORIENTATION], c);
      c = ViewerUtil.setGBC(c, 3, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwOrientation, c);
      c = ViewerUtil.setGBC(c, 2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      pOptions.add(cbOptions[ID_CUSTOM], c);
      c = ViewerUtil.setGBC(c, 3, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 4, 0, 0), 0, 0);
      pOptions.add(bpwCustomFilter, c);

      triggerActions(cbOptions);

      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 4, 0, 8), 0, 0);
      add(pOptions, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
          new Insets(0, 0, 0, 0), 0, 0);
      add(new JPanel(), c);
    }
  }

  /** Common base class for popup windows. */
  private static abstract class BasePanel extends JPanel implements ActionListener {
    protected BasePanel() {
      super(new BorderLayout());
    }

    /** Triggers action events for the array of specified objects */
    protected void triggerActions(Object[] sources) {
      if (sources != null) {
        for (Object element : sources) {
          actionPerformed(new ActionEvent(element, 0, null));
        }
      }
    }
  }

  /** Creates a dialog that allows to specify flags. */
  private static final class FlagsPanel extends BasePanel {
    private final int size;
    private final JCheckBox[] cbFlags;

    private JButton bAll;
    private JButton bNone;
    private JButton bInvert;
    private JCheckBox cbExact;

    public FlagsPanel(int size, String[] table) {
      super();
      if (size < 1) {
        size = 1;
      } else if (size == 3 || size > 4) {
        size = 4;
      }
      this.size = size;
      cbFlags = new JCheckBox[this.size << 3];
      init(table);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      if (event.getSource() == bAll) {
        for (JCheckBox cbFlag : cbFlags) {
          cbFlag.setSelected(true);
        }
      } else if (event.getSource() == bNone) {
        for (JCheckBox cbFlag : cbFlags) {
          cbFlag.setSelected(false);
        }
      } else if (event.getSource() == bInvert) {
        for (JCheckBox cbFlag : cbFlags) {
          cbFlag.setSelected(!cbFlag.isSelected());
        }
      }
    }

    public Couple<Object, Object> getOptionFlags() {
      return Couple.with(getFlagData(), isExact());
    }

    private boolean isExact() {
      return cbExact.isSelected();
    }

    private int getFlagData() {
      int retVal = 0;
      for (int mask = 1, i = 0; i < cbFlags.length; i++, mask <<= 1) {
        if (cbFlags[i].isSelected()) {
          retVal |= mask;
        }
      }
      return retVal;
    }

    private void init(String[] table) {
      if (table == null || table.length == 0) {
        table = new String[] { "Normal" };
      }

      GridBagConstraints c = new GridBagConstraints();

      // flags section
      int bits = cbFlags.length;
      int rowCount = bits >>> 2;
      JPanel pBits = new JPanel(new GridBagLayout());
      for (int row = 0, col = 0, i = 0; i < bits; i++, row++) {
        String label = null, desc = null;
        if (i + 1 >= table.length || table[i + 1] == null || table[i + 1].trim().isEmpty()) {
          label = String.format("%s (%d)", "Unknown", i);
        } else {
          String[] s = table[i + 1].split(";");
          label = String.format("%s (%d)", s[0], i);
          if (s.length > 1) {
            desc = s[1];
          }
        }
        cbFlags[i] = new JCheckBox(label, false);
        if (desc != null) {
          cbFlags[i].setToolTipText(desc);
        }

        if (row == rowCount) {
          row = 0;
          col++;
        }

        c = ViewerUtil.setGBC(c, col, row, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
            new Insets((row == 0) ? 0 : 4, (col == 0) ? 0 : 16, 0, 0), 0, 0);
        pBits.add(cbFlags[i], c);
      }

      // bottom section
      bAll = new JButton("Select all");
      bAll.setMargin(new Insets(1, bAll.getMargin().left, 1, bAll.getMargin().right));
      bAll.addActionListener(this);
      bNone = new JButton("Select none");
      bNone.setMargin(new Insets(1, bNone.getMargin().left, 1, bNone.getMargin().right));
      bNone.addActionListener(this);
      bInvert = new JButton("Invert selection");
      bInvert.setMargin(new Insets(1, bInvert.getMargin().left, 1, bInvert.getMargin().right));
      bInvert.addActionListener(this);
      cbExact = new JCheckBox("Exact match");
      cbExact.setToolTipText("Unchecked: Matching set bits only. Checked: Matching the exact state of the flags.");
      JPanel pBottom = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
          new Insets(0, 0, 0, 0), 0, 0);
      pBottom.add(bAll, c);
      c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
          new Insets(0, 8, 0, 0), 0, 0);
      pBottom.add(bNone, c);
      c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
          new Insets(0, 8, 0, 0), 0, 0);
      pBottom.add(bInvert, c);
      c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_END, GridBagConstraints.NONE,
          new Insets(0, 8, 0, 0), 0, 0);
      pBottom.add(cbExact, c);
      c = ViewerUtil.setGBC(c, 4, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_END, GridBagConstraints.NONE,
          new Insets(0, 0, 0, 0), 0, 0);
      pBottom.add(new JPanel(), c);

      // putting all together
      JPanel pMain = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH,
          new Insets(8, 8, 0, 8), 0, 0);
      pMain.add(pBits, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(8, 8, 8, 8), 0, 0);
      pMain.add(pBottom, c);

      add(pMain, BorderLayout.CENTER);
    }
  }

  /** Creates a dialog that allows to specify effect opcodes for CRE resources. */
  private static final class EffectsPanel extends BasePanel {
    private static final int MAX_ENTRY_COUNT = 16;

    private final int entryCount;
    private final String label;
    private final JCheckBox[] cbLabel;
    private final JSpinner[][] sEffects;

    public EffectsPanel(int effectCount, String label) {
      super();
      if (effectCount < 1) {
        effectCount = 1;
      } else if (effectCount > MAX_ENTRY_COUNT) {
        effectCount = MAX_ENTRY_COUNT;
      }
      entryCount = effectCount;
      this.label = (label != null && !label.isEmpty()) ? label : "Effect";
      cbLabel = new JCheckBox[entryCount];
      sEffects = new JSpinner[entryCount][2];
      init();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      if (event.getSource() instanceof JCheckBox) {
        for (int i = 0; i < entryCount; i++) {
          if (event.getSource() == cbLabel[i]) {
            sEffects[i][0].setEnabled(cbLabel[i].isSelected());
            sEffects[i][1].setEnabled(cbLabel[i].isSelected());
            if (cbLabel[i].isSelected()) {
              sEffects[i][0].requestFocusInWindow();
            }
            break;
          }
        }
      }
    }

    public boolean isActive(int id) {
      if (id < 0) {
        id = 0;
      } else if (id >= entryCount) {
        id = entryCount - 1;
      }
      return cbLabel[id].isSelected();
    }

    public Couple<Integer, Integer> getOptionEffect(int id) {
      return Utils.getRangeValues(cbLabel[id], sEffects[id]);
    }

    private void init() {
      // initializing components
      for (int i = 0; i < entryCount; i++) {
        cbLabel[i] = new JCheckBox(String.format("%s %d:", label, i + 1));
        cbLabel[i].addActionListener(this);

        sEffects[i][0] = Utils.createNumberSpinner(Short.MIN_VALUE, Short.MAX_VALUE, 0, 999, 0, 1);
        sEffects[i][1] = Utils.createNumberSpinner(Short.MIN_VALUE, Short.MAX_VALUE, 0, 999, 999, 1);
      }

      // placing components
      GridBagConstraints c = new GridBagConstraints();
      JPanel panel = new JPanel(new GridBagLayout());
      for (int i = 0; i < entryCount; i++) {
        int row, col;
        if (entryCount > 4) {
          row = i % ((entryCount + 1) / 2);
          col = (i < ((entryCount + 1) / 2)) ? 0 : 2;
        } else {
          row = i;
          col = 0;
        }
        c = ViewerUtil.setGBC(c, col, row, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
            new Insets((i > 0) ? 4 : 0, (col == 2) ? 16 : 0, 0, 0), 0, 0);
        panel.add(cbLabel[i], c);
        c = ViewerUtil.setGBC(c, col + 1, row, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, new Insets((i > 0) ? 4 : 0, 8, 0, 0), 0, 0);
        panel.add(Utils.createNumberRangePanel(sEffects[i][0], sEffects[i][1]), c);
      }

      triggerActions(cbLabel);

      JPanel pMain = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH,
          new Insets(8, 8, 8, 8), 0, 0);
      pMain.add(panel, c);
      add(pMain, BorderLayout.CENTER);
    }
  }

  /** Creates a dialog that allows to specify custom filters for the selected resource type. */
  private static final class CustomFilterPanel extends BasePanel {
    private static final int FILTER_STRING = 0;
    private static final int FILTER_NUMBER = 1;
    private static final int FILTER_RESOURCE = 2;
    private static final int FILTER_FLAGS = 3;
    private static final String[] FILTER_PAGES = { "STRING", "NUMBER", "RESOURCE", "FLAGS" };
    private static final String[] FILTER_TEXT = { "as string", "as number", "as resource", "as flags" };
    private static final String[] RESOURCE_TYPES = { "2DA", "ACM", "ARE", "BAF", "BAM", "BCS", "BIO", "BMP", "BS",
        "CHR", "CHU", "CRE", "DLG", "EFF", "FNT", "GAM", "GLSL", "GUI", "IDS", "INI", "ITM", "MOS", "MUS", "MVE", "PLT",
        "PRO", "PVRZ", "RES", "SAV", "SPL", "SQL", "SRC", "STO", "TIS", "TOH", "TOT", "TXT", "VEF", "VVC", "WAV", "WBM",
        "WED", "WFX", "WMP", };

    private static final int MAX_ENTRY_COUNT = 8;

    private final int entryCount;
    private final String label;
    private final JCheckBox[] cbLabel;
    private final JComboBox<?>[] cbFilterType;
    private final JTextField[] tfFieldName;
    private final JTextField[] tfFieldValueString;
    private final JSpinner[][] sFieldValueNumber;
    private final JComboBox<ResourceEntry>[] cbFieldValueResource;
    private final JComboBox<String>[] cbFieldValueResourceType;
    private final FlagsPanel[] pFieldValueFlags;
    private final ButtonPopupWindow[] bpwFieldValueFlags;
    private final CardLayout[] clFilter;
    private final JPanel[] pEntry;
    private final JPanel[] pFilterValue;

    @SuppressWarnings("unchecked")
    public CustomFilterPanel(int filterCount, String label) {
      super();
      if (filterCount < 1) {
        filterCount = 1;
      } else if (filterCount > MAX_ENTRY_COUNT) {
        filterCount = MAX_ENTRY_COUNT;
      }
      entryCount = filterCount;
      this.label = (label != null && !label.isEmpty()) ? label : "Filter";
      cbLabel = new JCheckBox[entryCount];
      cbFilterType = new JComboBox[entryCount];
      tfFieldName = new JTextField[entryCount];
      tfFieldValueString = new JTextField[entryCount];
      sFieldValueNumber = new JSpinner[entryCount][2];
      cbFieldValueResource = new JComboBox[entryCount];
      cbFieldValueResourceType = new JComboBox[entryCount];
      pFieldValueFlags = new FlagsPanel[entryCount];
      bpwFieldValueFlags = new ButtonPopupWindow[entryCount];
      clFilter = new CardLayout[entryCount];
      pEntry = new JPanel[entryCount];
      pFilterValue = new JPanel[entryCount];
      init();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      if (event.getSource() instanceof JCheckBox) {
        for (int i = 0; i < entryCount; i++) {
          if (event.getSource() == cbLabel[i]) {
            cbFilterType[i].setEnabled(cbLabel[i].isSelected());
            tfFieldName[i].setEnabled(cbLabel[i].isSelected());
            tfFieldValueString[i].setEnabled(cbLabel[i].isSelected());
            sFieldValueNumber[i][0].setEnabled(cbLabel[i].isSelected());
            sFieldValueNumber[i][1].setEnabled(cbLabel[i].isSelected());
            cbFieldValueResource[i].setEnabled(cbLabel[i].isSelected());
            cbFieldValueResourceType[i].setEnabled(cbLabel[i].isSelected());
            if (cbLabel[i].isSelected()) {
              tfFieldName[i].requestFocusInWindow();
            }
            break;
          }
        }
      } else if (event.getSource() instanceof JComboBox) {
        for (int i = 0; i < entryCount; i++) {
          if (event.getSource() == cbFilterType[i]) {
            switch (cbFilterType[i].getSelectedIndex()) {
              case FILTER_STRING:
              case FILTER_NUMBER:
              case FILTER_RESOURCE:
              case FILTER_FLAGS:
                clFilter[i].show(pFilterValue[i], FILTER_PAGES[cbFilterType[i].getSelectedIndex()]);
                break;
            }
          } else if (event.getSource() == cbFieldValueResourceType[i]) {
            updateResourceList(i);
            break;
          }
        }
      }
    }

    public boolean isActive(int id) {
      if (id < 0) {
        id = 0;
      } else if (id >= entryCount) {
        id = entryCount - 1;
      }
      return (cbLabel[id].isSelected() && !tfFieldName[id].getText().isEmpty());
    }

    public Couple<Object, Object> getOptionFilter(int id) {
      if (id < 0) {
        id = 0;
      } else if (id >= entryCount) {
        id = entryCount - 1;
      }
      final String name = tfFieldName[id].getText();
      if (cbLabel[id].isSelected() && !name.isEmpty()) {
        switch (cbFilterType[id].getSelectedIndex()) {
          case FILTER_STRING:
            return Couple.with(name, tfFieldValueString[id].getText());
          case FILTER_NUMBER:
            final Integer min = (Integer) sFieldValueNumber[id][0].getValue();
            final Integer max = (Integer) sFieldValueNumber[id][1].getValue();
            return Couple.with(name, Couple.with(min, max));
          case FILTER_RESOURCE:
            final ResourceEntry entry = (ResourceEntry) cbFieldValueResource[id].getSelectedItem();
            return Couple.with(name, entry.getResourceName());
          case FILTER_FLAGS:
            return Couple.with(name, pFieldValueFlags[id].getOptionFlags());
        }
      }
      return null;
    }

    private JPanel createFilterPanel(int entry, int type) {
      if (entry < 0) {
        entry = 0;
      } else if (entry >= entryCount) {
        entry = entryCount - 1;
      }
      if (type < 0) {
        type = 0;
      } else if (type >= FILTER_PAGES.length) {
        type = FILTER_PAGES.length - 1;
      }

      GridBagConstraints c = new GridBagConstraints();
      JPanel panel = new JPanel(new GridBagLayout());
      switch (type) {
        case FILTER_STRING: {
          tfFieldValueString[entry] = Utils.defaultWidth(new JTextField());
          c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
              new Insets(0, 0, 0, 0), 0, 0);
          panel.add(tfFieldValueString[entry], c);
          break;
        }
        case FILTER_NUMBER: {
          sFieldValueNumber[entry][0] = Utils.createNumberSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, Short.MIN_VALUE,
              Short.MAX_VALUE, 0, 1);
          sFieldValueNumber[entry][1] = Utils.createNumberSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, Short.MIN_VALUE,
              Short.MAX_VALUE, Short.MAX_VALUE, 1);
          c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
              new Insets(0, 0, 0, 0), 0, 0);
          panel.add(Utils.createNumberRangePanel(sFieldValueNumber[entry][0], sFieldValueNumber[entry][1]), c);
          break;
        }
        case FILTER_RESOURCE: {
          cbFieldValueResourceType[entry] = new JComboBox<>(RESOURCE_TYPES);
          cbFieldValueResourceType[entry].addActionListener(this);
          cbFieldValueResource[entry] = new JComboBox<>();
          cbFieldValueResource[entry].setPreferredSize(Utils.getPrototypeSize(cbFieldValueResource[entry]));
          updateResourceList(entry);
          c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
              new Insets(0, 0, 0, 0), 0, 0);
          panel.add(cbFieldValueResource[entry], c);
          c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
              new Insets(0, 4, 0, 0), 0, 0);
          panel.add(cbFieldValueResourceType[entry], c);
          break;
        }
        case FILTER_FLAGS: {
          final String[] flagsDesc = { "Select flags", "Bit 0", "Bit 1", "Bit 2", "Bit 3", "Bit 4", "Bit 5", "Bit 6",
              "Bit 7", "Bit 8", "Bit 9", "Bit 10", "Bit 11", "Bit 12", "Bit 13", "Bit 14", "Bit 15", "Bit 16", "Bit 17",
              "Bit 18", "Bit 19", "Bit 20", "Bit 21", "Bit 22", "Bit 23", "Bit 24", "Bit 25", "Bit 26", "Bit 27",
              "Bit 28", "Bit 29", "Bit 30", "Bit 31" };
          pFieldValueFlags[entry] = new FlagsPanel(4, flagsDesc);
          bpwFieldValueFlags[entry] = new ButtonPopupWindow(SET_OPTIONS_TEXT, pFieldValueFlags[entry]);
          c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
              new Insets(0, 0, 0, 0), 0, 0);
          panel.add(bpwFieldValueFlags[entry], c);
          break;
        }
      }
      return panel;
    }

    private void updateResourceList(int entry) {
      final String type = (String) cbFieldValueResourceType[entry].getSelectedItem();
      if (type != null) {
        cbFieldValueResource[entry].setModel(new ResourceListModel(type));
      }
    }

    private void init() {
      GridBagConstraints c = new GridBagConstraints();

      // initializing components
      for (int i = 0; i < entryCount; i++) {
        cbLabel[i] = new JCheckBox(String.format("%s %d:", label, i + 1));
        cbLabel[i].addActionListener(this);

        cbFilterType[i] = new JComboBox<>(FILTER_TEXT);
        cbFilterType[i].addActionListener(this);

        tfFieldName[i] = Utils.defaultWidth(new JTextField());

        clFilter[i] = new CardLayout();
        pFilterValue[i] = new JPanel(clFilter[i]);
        for (int j = 0; j < FILTER_PAGES.length; j++) {
          pFilterValue[i].add(createFilterPanel(i, j), FILTER_PAGES[j]);
        }

        JPanel panel = new JPanel(new GridBagLayout());
        c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
            new Insets(0, 0, 0, 0), 0, 0);
        panel.add(cbLabel[i], c);
        c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
            new Insets(0, 8, 0, 0), 0, 0);
        panel.add(cbFilterType[i], c);
        c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
            new Insets(0, 0, 0, 0), 0, 0);
        panel.add(new JPanel(), c);
        c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
            new Insets(4, 0, 0, 0), 0, 0);
        panel.add(new JLabel("Field name:"), c);
        c = ViewerUtil.setGBC(c, 1, 1, 2, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
            new Insets(4, 8, 0, 0), 0, 0);
        panel.add(tfFieldName[i], c);
        c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
            new Insets(4, 0, 0, 0), 0, 0);
        panel.add(new JLabel("Field value:"), c);
        c = ViewerUtil.setGBC(c, 1, 2, 2, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
            new Insets(4, 8, 0, 0), 0, 0);
        panel.add(pFilterValue[i], c);

        pEntry[i] = new JPanel(new GridBagLayout());
        pEntry[i].setBorder(BorderFactory.createEtchedBorder());
        c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
            new Insets(4, 4, 4, 4), 0, 0);
        pEntry[i].add(panel, c);

      }

      // placing components
      JPanel panel = new JPanel(new GridBagLayout());
      for (int i = 0; i < entryCount; i++) {
        int row, col;
        if (entryCount > 2) {
          row = i % ((entryCount + 1) / 2);
          col = (i < ((entryCount + 1) / 2)) ? 0 : 2;
        } else {
          row = i;
          col = 0;
        }
        c = ViewerUtil.setGBC(c, col, row, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
            new Insets((row > 0) ? 8 : 0, (col == 2) ? 16 : 0, 0, 0), 0, 0);
        panel.add(pEntry[i], c);
      }

      triggerActions(cbLabel);

      JPanel pMain = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH,
          new Insets(8, 8, 8, 8), 0, 0);
      pMain.add(panel, c);
      add(pMain, BorderLayout.CENTER);
    }
  }

  /** Creates a dialog that allows to specify spell effect timing modes. */
  private static final class TimingModePanel extends BasePanel {
    public static final int TIMING_MODE = 0;
    public static final int TIMING_DURATION = 1;

    private static final String[] LABELS = { "Mode:", "Duration:", };

    private final JCheckBox[] cbTiming = new JCheckBox[LABELS.length];
    private final JSpinner[] sDuration = new JSpinner[LABELS.length];
    private JComboBox<IndexedString> cbMode;

    public TimingModePanel() {
      super();
      init();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      if (event.getSource() instanceof JCheckBox) {
        JCheckBox cb = (JCheckBox) event.getSource();
        for (int i = 0; i < cbTiming.length; i++) {
          if (cb == cbTiming[i]) {
            switch (i) {
              case TIMING_MODE:
                cbMode.setEnabled(cb.isSelected());
                if (cb.isSelected()) {
                  cbMode.requestFocusInWindow();
                }
                break;
              case TIMING_DURATION:
                sDuration[0].setEnabled(cb.isSelected());
                sDuration[1].setEnabled(cb.isSelected());
                if (cb.isSelected()) {
                  sDuration[0].requestFocusInWindow();
                }
                break;
            }
            break;
          }
        }
      }
    }

    public boolean isActive(int id) {
      if (id < 0) {
        id = 0;
      } else if (id >= cbTiming.length) {
        id = cbTiming.length - 1;
      }
      return cbTiming[id].isSelected();
    }

    public int getOptionMode() {
      return cbTiming[TIMING_MODE].isSelected() ? ((IndexedString) cbMode.getSelectedItem()).index : 0;
    }

    public Couple<Integer, Integer> getOptionDuration() {
      return Utils.getRangeValues(cbTiming[TIMING_DURATION], sDuration);
    }

    private void init() {
      for (int i = 0; i < LABELS.length; i++) {
        cbTiming[i] = new JCheckBox(LABELS[i]);
        cbTiming[i].addActionListener(this);
      }

      cbMode = Utils.defaultWidth(new AutoComboBox<>(IndexedString.createArray(BaseOpcode.DURATIONS_V2_MAP)), 130);
      sDuration[0] = Utils.createNumberSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, -32768, 32767, 0, 1);
      sDuration[1] = Utils.createNumberSpinner(Integer.MIN_VALUE, Integer.MAX_VALUE, -32768, 32767, 3600, 1);

      // placing components
      GridBagConstraints c = new GridBagConstraints();
      JPanel panel = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 0, 0, 0), 0, 0);
      panel.add(cbTiming[TIMING_MODE], c);
      c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 8, 0, 0), 0, 0);
      panel.add(cbMode, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(8, 0, 0, 0), 0, 0);
      panel.add(cbTiming[TIMING_DURATION], c);
      c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(8, 8, 0, 0), 0, 0);
      panel.add(Utils.createNumberRangePanel(sDuration[0], sDuration[1]), c);

      triggerActions(cbTiming);

      JPanel pMain = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH,
          new Insets(8, 8, 8, 8), 0, 0);
      pMain.add(panel, c);
      add(pMain, BorderLayout.CENTER);
    }
  }

  /** Creates a dialog that allows to specify creature level ranges. */
  private static final class CreLevelPanel extends BasePanel {
    private static final String[] LABELS = { "First class level:", "Second class level:", "Third class level:", };

    private final JSpinner[][] sLevel = new JSpinner[LABELS.length][2];
    private final JCheckBox[] cbLevel = new JCheckBox[LABELS.length];

    public CreLevelPanel() {
      super();
      init();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      if (event.getSource() instanceof JCheckBox) {
        for (int i = 0; i < cbLevel.length; i++) {
          if (event.getSource() == cbLevel[i]) {
            sLevel[i][0].setEnabled(cbLevel[i].isSelected());
            sLevel[i][1].setEnabled(cbLevel[i].isSelected());
            if (cbLevel[i].isSelected()) {
              sLevel[i][0].requestFocusInWindow();
            }
            break;
          }
        }
      }
    }

    public boolean isActive(int classIdx) {
      if (classIdx < 0) {
        classIdx = 0;
      } else if (classIdx >= sLevel.length) {
        classIdx = sLevel.length - 1;
      }
      return cbLevel[classIdx].isSelected();
    }

    public Couple<Integer, Integer> getOptionLevel(int classIdx) {
      return Utils.getRangeValues(cbLevel[classIdx], sLevel[classIdx]);
    }

    private void init() {
      // initializing components
      for (int i = 0; i < sLevel.length; i++) {
        cbLevel[i] = new JCheckBox(LABELS[i]);
        cbLevel[i].addActionListener(this);
        sLevel[i][0] = Utils.createNumberSpinner(Byte.MIN_VALUE, Byte.MAX_VALUE, 0, 100, 0, 1);
        sLevel[i][1] = Utils.createNumberSpinner(Byte.MIN_VALUE, Byte.MAX_VALUE, 0, 100, 100, 1);
      }

      // placing components
      GridBagConstraints c = new GridBagConstraints();
      JPanel panel = new JPanel(new GridBagLayout());
      for (int i = 0; i < LABELS.length; i++) {
        c = ViewerUtil.setGBC(c, 0, i, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
            new Insets((i > 0) ? 4 : 0, 0, 0, 0), 0, 0);
        panel.add(cbLevel[i], c);
        c = ViewerUtil.setGBC(c, 1, i, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
            new Insets((i > 0) ? 4 : 0, 8, 0, 0), 0, 0);
        panel.add(sLevel[i][0], c);
        c = ViewerUtil.setGBC(c, 2, i, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
            new Insets((i > 0) ? 4 : 0, 4, 0, 0), 0, 0);
        panel.add(new JLabel("to"), c);
        c = ViewerUtil.setGBC(c, 3, i, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
            new Insets((i > 0) ? 4 : 0, 4, 0, 0), 0, 0);
        panel.add(sLevel[i][1], c);
      }

      triggerActions(cbLevel);

      JPanel pMain = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH,
          new Insets(8, 8, 8, 8), 0, 0);
      pMain.add(panel, c);
      add(pMain, BorderLayout.CENTER);
    }
  }

  /** Creates a dialog that allows to specify creature level ranges (IWD2-specific). */
  private static final class CreLevelIWD2Panel extends BasePanel {
    public static final int LEVEL_TOTAL = 0;
    public static final int LEVEL_BARBARIAN = 1;
    public static final int LEVEL_BARD = 2;
    public static final int LEVEL_CLERIC = 3;
    public static final int LEVEL_DRUID = 4;
    public static final int LEVEL_FIGHTER = 5;
    public static final int LEVEL_MONK = 6;
    public static final int LEVEL_PALADIN = 7;
    public static final int LEVEL_RANGER = 8;
    public static final int LEVEL_ROGUE = 9;
    public static final int LEVEL_SORCERER = 10;
    public static final int LEVEL_WIZARD = 11;

    private static final String[] LABELS = { "Total level:", "Barbarian level:", "Bard level:", "Cleric level:",
        "Druid level:", "Fighter level:", "Monk level:", "Paladin level:", "Ranger level:", "Rogue level:",
        "Sorcerer level:", "Wizard level:", };

    private final JSpinner[][] sLevel = new JSpinner[LABELS.length][2];
    private final JCheckBox[] cbLevel = new JCheckBox[LABELS.length];

    public CreLevelIWD2Panel() {
      super();
      init();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      if (event.getSource() instanceof JCheckBox) {
        for (int i = 0; i < cbLevel.length; i++) {
          if (event.getSource() == cbLevel[i]) {
            sLevel[i][0].setEnabled(cbLevel[i].isSelected());
            sLevel[i][1].setEnabled(cbLevel[i].isSelected());
            if (cbLevel[i].isSelected()) {
              sLevel[i][0].requestFocusInWindow();
            }
            break;
          }
        }
      }
    }

    public boolean isActive(int id) {
      if (id < 0) {
        id = 0;
      } else if (id >= cbLevel.length) {
        id = cbLevel.length - 1;
      }
      return cbLevel[id].isSelected();
    }

    public Couple<Integer, Integer> getOptionLevel(int id) {
      return Utils.getRangeValues(cbLevel[id], sLevel[id]);
    }

    private void init() {
      // initializing components
      for (int i = 0; i < LABELS.length; i++) {
        cbLevel[i] = new JCheckBox(LABELS[i]);
        cbLevel[i].addActionListener(this);
        sLevel[i][0] = Utils.createNumberSpinner(Byte.MIN_VALUE, Byte.MAX_VALUE, 0, 100, 0, 1);
        sLevel[i][1] = Utils.createNumberSpinner(Byte.MIN_VALUE, Byte.MAX_VALUE, 0, 100, 100, 1);
      }

      // placing components
      GridBagConstraints c = new GridBagConstraints();
      JPanel panel = new JPanel(new GridBagLayout());
      for (int i = 0; i < LABELS.length; i++) {
        int row = i % (LABELS.length / 2);
        int col = (i < LABELS.length / 2) ? 0 : 2;

        c = ViewerUtil.setGBC(c, col, row, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
            new Insets((i > 0) ? 4 : 0, (col == 2) ? 16 : 0, 0, 0), 0, 0);
        panel.add(cbLevel[i], c);
        c = ViewerUtil.setGBC(c, col + 1, row, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, new Insets((i > 0) ? 4 : 0, 8, 0, 0), 0, 0);
        panel.add(Utils.createNumberRangePanel(sLevel[i][0], sLevel[i][1]), c);
      }

      triggerActions(cbLevel);

      JPanel pMain = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH,
          new Insets(8, 8, 8, 8), 0, 0);
      pMain.add(panel, c);
      add(pMain, BorderLayout.CENTER);
    }
  }

  /** Creates a dialog that allows to specify creature types. */
  private static final class CreTypePanel extends BasePanel {
    public static final int TYPE_GENERAL = 0;
    public static final int TYPE_CLASS = 1;
    public static final int TYPE_SPECIFICS = 2;
    public static final int TYPE_ALIGNMENT = 3;
    public static final int TYPE_GENDER = 4;
    public static final int TYPE_RACE = 5;
    public static final int TYPE_ALLEGIANCE = 6;
    public static final int TYPE_KIT = 7;
    private static final int TYPE_SEX = 8; // special: IWD2 only

    private static final String[] LABELS = { "General:", "Class:", "Specifics:", "Alignment:", "Gender:", "Race:",
        "Allegiance:", "Kit:",

        "Sex:",// replacement for "Gender" in IWD2
    };

    private final JCheckBox[] cbLabel = new JCheckBox[LABELS.length - 1];
    /** Each combobox contains list of {@link IdsMapEntry}. */
    @SuppressWarnings("unchecked")
    private final JComboBox<IdsMapEntry>[] cbType = new JComboBox[cbLabel.length];

    public CreTypePanel() {
      super();
      init();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      if (event.getSource() instanceof JCheckBox) {
        for (int i = 0; i < cbLabel.length; i++) {
          if (event.getSource() == cbLabel[i]) {
            cbType[i].setEnabled(cbLabel[i].isSelected());
            if (cbLabel[i].isSelected()) {
              cbType[i].requestFocusInWindow();
            }
            break;
          }
        }
      }
    }

    public boolean isActive(int id) {
      if (id < 0) {
        id = 0;
      } else if (id >= LABELS.length) {
        id = LABELS.length - 1;
      }
      return cbLabel[id].isSelected();
    }

    public int getOptionType(int id) {
      if (id < 0) {
        id = 0;
      } else if (id >= LABELS.length) {
        id = LABELS.length - 1;
      }
      return Utils.getIdsValue(cbLabel[id], cbType[id]);
    }

    private void init() {
      boolean hasKit = Profile.getProperty(Profile.Key.IS_SUPPORTED_KITS);

      // initializing components
      for (int i = 0; i < cbLabel.length; i++) {
        if (i == TYPE_GENDER && Profile.getEngine() == Profile.Engine.IWD2) {
          cbLabel[i] = new JCheckBox(LABELS[TYPE_SEX]);
        } else {
          cbLabel[i] = new JCheckBox(LABELS[i]);
        }
        cbLabel[i].addActionListener(this);
      }
      cbLabel[TYPE_KIT].setEnabled(hasKit);

      final String alignmentIDS = Profile.getProperty(Profile.Key.GET_IDS_ALIGNMENT);
      cbType[TYPE_GENERAL] = Utils.getIdsMapEntryList(1, "General", "GENERAL.IDS");
      cbType[TYPE_CLASS] = Utils.getIdsMapEntryList(1, "Class", "CLASS.IDS");
      cbType[TYPE_SPECIFICS] = Utils.getIdsMapEntryList(1, "Specifics", "SPECIFIC.IDS");
      cbType[TYPE_ALIGNMENT] = Utils.getIdsMapEntryList(1, "Alignment", alignmentIDS);
      cbType[TYPE_GENDER] = Utils.getIdsMapEntryList(1, "Gender", "GENDER.IDS");
      cbType[TYPE_RACE] = Utils.getIdsMapEntryList(1, "Race", "RACE.IDS");
      cbType[TYPE_ALLEGIANCE] = Utils.getIdsMapEntryList(1, "Allegiance", "EA.IDS");

      if (Profile.getEngine() == Profile.Engine.IWD2) {
        cbType[TYPE_KIT] = Utils.getIdsMapEntryList(1, "Kit", "KIT.IDS");
      } else if (hasKit) {
        final KitIdsBitmap kit = new KitIdsBitmap(StreamUtils.getByteBuffer(4), 0, "Kit");
        cbType[TYPE_KIT] = Utils.getIdsMapEntryList(kit);
      } else {
        cbType[TYPE_KIT] = new AutoComboBox<>(new IdsMapEntry[0]);
      }
      cbType[TYPE_KIT].setEnabled(hasKit);

      // placing components
      GridBagConstraints c = new GridBagConstraints();
      JPanel panel = new JPanel(new GridBagLayout());
      for (int i = 0; i < cbLabel.length; i++) {
        int row = i % ((cbLabel.length + 1) / 2);
        int col = (i < (cbLabel.length + 1) / 2) ? 0 : 2;

        c = ViewerUtil.setGBC(c, col, row, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
            new Insets((i > 0) ? 4 : 0, (col == 2) ? 16 : 0, 0, 0), 0, 0);
        panel.add(cbLabel[i], c);
        c = ViewerUtil.setGBC(c, col + 1, row, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, new Insets((i > 0) ? 4 : 0, 8, 0, 0), 0, 0);
        panel.add(cbType[i], c);
      }

      triggerActions(cbLabel);

      JPanel pMain = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH,
          new Insets(8, 8, 8, 8), 0, 0);
      pMain.add(panel, c);
      add(pMain, BorderLayout.CENTER);
    }
  }

  /** Creates a dialog that allows to specify game-specific settings for CRE resources. */
  private static final class CreGameSpecificPanel extends BasePanel {
    public static final int TYPE_FEATS1 = 0;
    public static final int TYPE_FEATS2 = 1;
    public static final int TYPE_FEATS3 = 2;
    public static final int TYPE_ATTRIBUTES = 3;

    private static final String[] LABELS = { "Feats 1:", "Feats 2:", "Feats 3:", "Attributes:", };

    private final JCheckBox[] cbLabel = new JCheckBox[LABELS.length];
    private final FlagsPanel[] pFlags = new FlagsPanel[LABELS.length];
    private final ButtonPopupWindow[] bpwFlags = new ButtonPopupWindow[LABELS.length];

    public static boolean isGameSpecificEnabled() {
      return (Profile.getEngine() == Profile.Engine.IWD2 || Profile.getEngine() == Profile.Engine.PST);
    }

    public CreGameSpecificPanel() {
      super();
      init();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      if (event.getSource() instanceof JCheckBox) {
        for (int i = 0; i < cbLabel.length; i++) {
          if (event.getSource() == cbLabel[i]) {
            bpwFlags[i].setEnabled(cbLabel[i].isSelected());
            if (cbLabel[i].isSelected()) {
              bpwFlags[i].requestFocusInWindow();
            }
            break;
          }
        }
      }
    }

    public boolean isActive(int id) {
      if (id < 0) {
        id = 0;
      } else if (id >= cbLabel.length) {
        id = cbLabel.length - 1;
      }
      return cbLabel[id].isSelected();
    }

    public Couple<Object, Object> getOptionFlags(int id) {
      if (id < 0) {
        id = 0;
      } else if (id >= LABELS.length) {
        id = LABELS.length - 1;
      }
      return cbLabel[id].isSelected() ? pFlags[id].getOptionFlags() : Couple.with(0, false);
    }

    private void init() {
      boolean isIWD2 = (Profile.getEngine() == Profile.Engine.IWD2);
      boolean isBoth = isIWD2 || (Profile.getEngine() == Profile.Engine.PST);

      // initializing components
      for (int i = 0; i < LABELS.length; i++) {
        cbLabel[i] = new JCheckBox(LABELS[i]);
        cbLabel[i].setEnabled((i == TYPE_ATTRIBUTES) ? isBoth : isIWD2);
        cbLabel[i].addActionListener(this);
      }

      // IWD2
      pFlags[TYPE_FEATS1] = new FlagsPanel(4, CreResource.FEATS1_ARRAY);
      bpwFlags[TYPE_FEATS1] = Utils.defaultWidth(new ButtonPopupWindow(SET_OPTIONS_TEXT, pFlags[TYPE_FEATS1]));
      bpwFlags[TYPE_FEATS1].setEnabled(isIWD2);
      pFlags[TYPE_FEATS2] = new FlagsPanel(4, CreResource.FEATS2_ARRAY);
      bpwFlags[TYPE_FEATS2] = Utils.defaultWidth(new ButtonPopupWindow(SET_OPTIONS_TEXT, pFlags[TYPE_FEATS2]));
      bpwFlags[TYPE_FEATS2].setEnabled(isIWD2);
      pFlags[TYPE_FEATS3] = new FlagsPanel(4, CreResource.FEATS3_ARRAY);
      bpwFlags[TYPE_FEATS3] = Utils.defaultWidth(new ButtonPopupWindow(SET_OPTIONS_TEXT, pFlags[TYPE_FEATS3]));
      bpwFlags[TYPE_FEATS3].setEnabled(isIWD2);
      // IWD2 and PST
      if (isIWD2) {
        pFlags[TYPE_ATTRIBUTES] = new FlagsPanel(1, CreResource.ATTRIBUTES_IWD2_ARRAY);
      } else {
        pFlags[TYPE_ATTRIBUTES] = new FlagsPanel(4, CreResource.ATTRIBUTES_PST_ARRAY);
      }
      bpwFlags[TYPE_ATTRIBUTES] = Utils.defaultWidth(new ButtonPopupWindow(SET_OPTIONS_TEXT, pFlags[TYPE_ATTRIBUTES]));
      bpwFlags[TYPE_ATTRIBUTES].setEnabled(isBoth);

      // placing components
      GridBagConstraints c = new GridBagConstraints();
      JPanel panel = new JPanel(new GridBagLayout());
      for (int i = 0; i < LABELS.length; i++) {
        c = ViewerUtil.setGBC(c, 0, i, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
            new Insets((i > 0) ? 4 : 0, 0, 0, 0), 0, 0);
        panel.add(cbLabel[i], c);
        c = ViewerUtil.setGBC(c, 1, i, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
            new Insets((i > 0) ? 4 : 0, 8, 0, 0), 0, 0);
        panel.add(bpwFlags[i], c);
      }

      triggerActions(cbLabel);

      JPanel pMain = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH,
          new Insets(8, 8, 8, 8), 0, 0);
      pMain.add(panel, c);
      add(pMain, BorderLayout.CENTER);
    }
  }

  /** Panel with several filters by resource reference. */
  private static class ResourcesFilterPanel extends BasePanel {
    private final JCheckBox[] cbLabel;
    private final JComboBox<ResourceEntry>[] cbItems;

    /**
     *
     * @param filterCount  Count of filters on panel
     * @param nameTemplate Template of the filter names in {@link String#format} format. Supplied with one integer
     *                     argument
     * @param ext          Extension, used to fill comboboxes with resources
     * @param usePrototype Use prototype string to determine size of combobox
     */
    @SuppressWarnings("unchecked")
    public ResourcesFilterPanel(int filterCount, String nameTemplate, String ext, boolean usePrototype) {
      cbLabel = new JCheckBox[filterCount];
      cbItems = new JComboBox[filterCount];

      final GridBagConstraints c = new GridBagConstraints();
      c.anchor = GridBagConstraints.LINE_START;
      final JPanel panel = new JPanel(new GridBagLayout());
      for (int i = 0; i < filterCount; i++) {
        cbLabel[i] = new JCheckBox(String.format(nameTemplate, i + 1));
        cbLabel[i].addActionListener(this);

        cbItems[i] = Utils.createNamedResourceComboBox(new String[] { ext }, usePrototype);

        final int top = i > 0 ? 4 : 8;
        final int bottom = i == filterCount - 1 ? 8 : 0;
        // Each pair of label and selector on single line
        c.gridy = i;

        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(top, 8, bottom, 0);
        panel.add(cbLabel[i], c);

        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(top, 0, bottom, 8);
        panel.add(cbItems[i], c);
      }

      triggerActions(cbLabel);
      add(panel, BorderLayout.CENTER);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      if (event.getSource() instanceof JCheckBox) {
        for (int i = 0; i < cbLabel.length; i++) {
          if (event.getSource() == cbLabel[i]) {
            cbItems[i].setEnabled(cbLabel[i].isSelected());
            if (cbLabel[i].isSelected()) {
              cbItems[i].requestFocusInWindow();
            }
            break;
          }
        }
      }
    }

    public boolean isActive(int id) {
      return cbLabel[id].isSelected();
    }

    public String getResourceName(int id) {
      return Utils.getResourceName(cbLabel[id], cbItems[id]);
    }
  }

  /** Creates a dialog that allows to specify usability flags. */
  private static final class ItmUsabilityPanel extends BasePanel {
    public static final int ITEM_UNUSABLE = 0;
    public static final int ITEM_KITSUNUSABLE1 = 1;
    public static final int ITEM_KITSUNUSABLE2 = 2;
    public static final int ITEM_KITSUNUSABLE3 = 3;
    public static final int ITEM_KITSUNUSABLE4 = 4;

    private static final String[] LABELS = { "Unusable by:", "Unusable kits 1:", "Unusable kits 2:", "Unusable kits 3:",
        "Unusable kits 4:", };

    private final JCheckBox[] cbLabel = new JCheckBox[LABELS.length];
    private final FlagsPanel[] pFlags = new FlagsPanel[LABELS.length];
    private final ButtonPopupWindow[] bpwFlags = new ButtonPopupWindow[LABELS.length];

    public ItmUsabilityPanel() {
      super();
      init();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      if (event.getSource() instanceof JCheckBox) {
        for (int i = 0; i < LABELS.length; i++) {
          if (event.getSource() == cbLabel[i]) {
            bpwFlags[i].setEnabled(cbLabel[i].isSelected());
            if (cbLabel[i].isSelected()) {
              bpwFlags[i].requestFocusInWindow();
            }
            break;
          }
        }
      }
    }

    public boolean isActive(int id) {
      if (id < 0) {
        id = 0;
      } else if (id >= cbLabel.length) {
        id = cbLabel.length - 1;
      }
      return cbLabel[id].isSelected();
    }

    public Couple<Object, Object> getOptionFlags(int id) {
      return cbLabel[id].isSelected() ? pFlags[id].getOptionFlags() : Couple.with(0, false);
    }

    private void init() {
      boolean kitsSupported = Profile.getProperty(Profile.Key.IS_SUPPORTED_KITS);

      // initializing components
      for (int i = 0; i < LABELS.length; i++) {
        cbLabel[i] = new JCheckBox(LABELS[i]);
        if (i >= ITEM_KITSUNUSABLE1) {
          cbLabel[i].setEnabled(kitsSupported);
        }
        cbLabel[i].addActionListener(this);
      }

      String[] sUnusable;
      if ((Boolean) Profile.getProperty(Profile.Key.IS_SUPPORTED_ITM_V11) || Profile.getGame() == Profile.Game.PSTEE) {
        sUnusable = ItmResource.USABILITY11_ARRAY;
      } else if ((Boolean) Profile.getProperty(Profile.Key.IS_SUPPORTED_ITM_V20)) {
        sUnusable = ItmResource.USABILITY20_ARRAY;
      } else {
        sUnusable = ItmResource.USABILITY_ARRAY;
      }

      pFlags[ITEM_UNUSABLE] = new FlagsPanel(4, sUnusable);
      bpwFlags[ITEM_UNUSABLE] = new ButtonPopupWindow(SET_OPTIONS_TEXT, pFlags[ITEM_UNUSABLE]);

      if (kitsSupported) {
        pFlags[ITEM_KITSUNUSABLE1] = new FlagsPanel(1, ItmResource.KIT_USE_1_ARRAY);
        pFlags[ITEM_KITSUNUSABLE2] = new FlagsPanel(1, ItmResource.KIT_USE_2_ARRAY);
        pFlags[ITEM_KITSUNUSABLE3] = new FlagsPanel(1, ItmResource.KIT_USE_3_ARRAY);
        pFlags[ITEM_KITSUNUSABLE4] = new FlagsPanel(1, ItmResource.KIT_USE_4_ARRAY);
      }
      bpwFlags[ITEM_KITSUNUSABLE1] = new ButtonPopupWindow(SET_OPTIONS_TEXT, pFlags[ITEM_KITSUNUSABLE1]);
      bpwFlags[ITEM_KITSUNUSABLE1].setEnabled(kitsSupported);
      bpwFlags[ITEM_KITSUNUSABLE2] = new ButtonPopupWindow(SET_OPTIONS_TEXT, pFlags[ITEM_KITSUNUSABLE2]);
      bpwFlags[ITEM_KITSUNUSABLE2].setEnabled(kitsSupported);
      bpwFlags[ITEM_KITSUNUSABLE3] = new ButtonPopupWindow(SET_OPTIONS_TEXT, pFlags[ITEM_KITSUNUSABLE3]);
      bpwFlags[ITEM_KITSUNUSABLE3].setEnabled(kitsSupported);
      bpwFlags[ITEM_KITSUNUSABLE4] = new ButtonPopupWindow(SET_OPTIONS_TEXT, pFlags[ITEM_KITSUNUSABLE4]);
      bpwFlags[ITEM_KITSUNUSABLE4].setEnabled(kitsSupported);

      // placing components
      GridBagConstraints c = new GridBagConstraints();
      JPanel panel = new JPanel(new GridBagLayout());
      for (int i = 0; i < LABELS.length; i++) {
        c = ViewerUtil.setGBC(c, 0, i, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
            new Insets((i > 0) ? 4 : 0, 0, 0, 0), 0, 0);
        panel.add(cbLabel[i], c);
        c = ViewerUtil.setGBC(c, 1, i, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
            new Insets((i > 0) ? 4 : 0, 8, 0, 0), 0, 0);
        panel.add(bpwFlags[i], c);
      }

      triggerActions(cbLabel);

      JPanel pMain = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH,
          new Insets(8, 8, 8, 8), 0, 0);
      pMain.add(panel, c);
      add(pMain, BorderLayout.CENTER);
    }
  }

  /** Creates a dialog that allows to specify minimum stats ranges. */
  private static final class ItmStatsPanel extends BasePanel {
    // supported stats
    public static final int STAT_LEVEL = 0;
    public static final int STAT_STR = 1;
    public static final int STAT_STR_EXTRA = 2;
    public static final int STAT_DEX = 3;
    public static final int STAT_CON = 4;
    public static final int STAT_INT = 5;
    public static final int STAT_WIS = 6;
    public static final int STAT_CHA = 7;

    private static final String[] LABELS = { "Min. Level:", "Min. STR:", "Min. STR bonus:", "Min. DEX:", "Min. CON:",
        "Min. INT:", "Min. WIS:", "Min. CHA:", };

    private final JCheckBox[] cbStats = new JCheckBox[LABELS.length];
    private final JSpinner[][] sStats = new JSpinner[LABELS.length][2];

    public ItmStatsPanel() {
      super();
      init();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      if (event.getSource() instanceof JCheckBox) {
        for (int i = 0; i < cbStats.length; i++) {
          if (event.getSource() == cbStats[i]) {
            sStats[i][0].setEnabled(cbStats[i].isSelected());
            sStats[i][1].setEnabled(cbStats[i].isSelected());
            if (cbStats[i].isSelected()) {
              sStats[i][0].requestFocusInWindow();
            }
            break;
          }
        }
      }
    }

    public boolean isActive(int statID) {
      if (statID >= 0 && statID < cbStats.length) {
        return cbStats[statID].isSelected();
      }
      return false;
    }

    public Couple<Integer, Integer> getOptionValue(int statID) {
      return Utils.getRangeValues(cbStats[statID], sStats[statID]);
    }

    private void init() {
      for (int i = 0; i < LABELS.length; i++) {
        cbStats[i] = new JCheckBox(LABELS[i]);
        cbStats[i].addActionListener(this);

        int min = 0;
        int max = 25;
        if (i == STAT_LEVEL || i == STAT_STR_EXTRA) {
          max = 100;
        }
        sStats[i][0] = Utils.createNumberSpinner(Byte.MIN_VALUE, Byte.MAX_VALUE, min, max, min, 1);
        sStats[i][1] = Utils.createNumberSpinner(Byte.MIN_VALUE, Byte.MAX_VALUE, min, max, max, 1);
      }

      // placing components
      GridBagConstraints c = new GridBagConstraints();
      JPanel panel = new JPanel(new GridBagLayout());
      for (int i = 0, row = 0, col = 0; i < LABELS.length; i++, row++) {
        if (i == 4) {
          col = 4;
          row = 0;
        }
        c = ViewerUtil.setGBC(c, col + 0, row, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, new Insets((row == 0) ? 0 : 4, (col == 0) ? 0 : 16, 0, 0), 0, 0);
        panel.add(cbStats[i], c);
        c = ViewerUtil.setGBC(c, col + 1, row, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, new Insets((row == 0) ? 0 : 4, 8, 0, 0), 0, 0);
        panel.add(sStats[i][0], c);
        c = ViewerUtil.setGBC(c, col + 2, row, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, new Insets((row == 0) ? 0 : 4, 4, 0, 0), 0, 0);
        panel.add(new JLabel("to"), c);
        c = ViewerUtil.setGBC(c, col + 3, row, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, new Insets((row == 0) ? 0 : 4, 4, 0, 0), 0, 0);
        panel.add(sStats[i][1], c);
      }

      triggerActions(cbStats);

      JPanel pMain = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH,
          new Insets(8, 8, 8, 8), 0, 0);
      pMain.add(panel, c);
      add(pMain, BorderLayout.CENTER);
    }
  }

  /** Creates a dialog that allows to specify item ability properties. */
  private static final class ItmAbilityPanel extends BasePanel {
    private static final int ITEM_TYPE = 0;
    private static final int ITEM_TARGET = 1;
    private static final int ITEM_RANGE = 2;
    private static final int ITEM_LAUNCHER = 3;
    private static final int ITEM_SPEED = 4;
    private static final int ITEM_DICECOUNT = 5;
    private static final int ITEM_DICESIZE = 6;
    private static final int ITEM_CHARGES = 7;
    private static final int ITEM_FLAGS = 8;
    private static final int ITEM_DAMAGETYPE = 9;
    private static final int ITEM_PROJECTILE = 10;
    private static final int ITEM_EFFECTS = 11;

    private static final String[] LABELS = { "Type:", "Target:", "Range (feet):", "Launcher:", "Speed:", "Dice count:",
        "Dice size:", "Charges:", "Flags:", "Damage type:", "Projectile:", "Effect opcodes:", };

    private final JCheckBox[] cbItems = new JCheckBox[LABELS.length];
    private final JSpinner[] sRange = new JSpinner[2];
    private final JSpinner[] sSpeed = new JSpinner[2];
    private final JSpinner[] sDiceCount = new JSpinner[2];
    private final JSpinner[] sDiceSize = new JSpinner[2];
    private final JSpinner[] sCharges = new JSpinner[2];
    private final FlagsPanel flagsPanel = new FlagsPanel(4, org.infinity.resource.itm.Ability.RECHARGE_ARRAY);

    private EffectsPanel pEffects;
    private JComboBox<IndexedString> cbType;
    private JComboBox<IndexedString> cbTarget;
    private JComboBox<IndexedString> cbLauncher;
    private JComboBox<IndexedString> cbDamageType;
    private JComboBox<IndexedString> cbProjectile;
    private ButtonPopupWindow bpwFlags;
    private ButtonPopupWindow bpwEffects;
    private JCheckBox cbOneAbilityExclusive;

    public ItmAbilityPanel() {
      super();
      init();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      if (event.getSource() instanceof JCheckBox) {
        JCheckBox cb = (JCheckBox) event.getSource();
        for (int i = 0; i < cbItems.length; i++) {
          if (cb == cbItems[i]) {
            switch (i) {
              case ITEM_TYPE:
                cbType.setEnabled(cb.isSelected());
                if (cb.isSelected()) {
                  cbType.requestFocusInWindow();
                }
                break;
              case ITEM_TARGET:
                cbTarget.setEnabled(cb.isSelected());
                if (cb.isSelected()) {
                  cbTarget.requestFocusInWindow();
                }
                break;
              case ITEM_RANGE:
                sRange[0].setEnabled(cb.isSelected());
                sRange[1].setEnabled(cb.isSelected());
                if (cb.isSelected()) {
                  sRange[0].requestFocusInWindow();
                }
                break;
              case ITEM_LAUNCHER:
                cbLauncher.setEnabled(cb.isSelected());
                if (cb.isSelected()) {
                  cbLauncher.requestFocusInWindow();
                }
                break;
              case ITEM_SPEED:
                sSpeed[0].setEnabled(cb.isSelected());
                sSpeed[1].setEnabled(cb.isSelected());
                if (cb.isSelected()) {
                  sSpeed[0].requestFocusInWindow();
                }
                break;
              case ITEM_DICECOUNT:
                sDiceCount[0].setEnabled(cb.isSelected());
                sDiceCount[1].setEnabled(cb.isSelected());
                if (cb.isSelected()) {
                  sDiceCount[0].requestFocusInWindow();
                }
                break;
              case ITEM_DICESIZE:
                sDiceSize[0].setEnabled(cb.isSelected());
                sDiceSize[1].setEnabled(cb.isSelected());
                if (cb.isSelected()) {
                  sDiceSize[0].requestFocusInWindow();
                }
                break;
              case ITEM_CHARGES:
                sCharges[0].setEnabled(cb.isSelected());
                sCharges[1].setEnabled(cb.isSelected());
                if (cb.isSelected()) {
                  sCharges[0].requestFocusInWindow();
                }
                break;
              case ITEM_FLAGS:
                bpwFlags.setEnabled(cb.isSelected());
                if (cb.isSelected()) {
                  bpwFlags.requestFocusInWindow();
                }
                break;
              case ITEM_DAMAGETYPE:
                cbDamageType.setEnabled(cb.isSelected());
                if (cb.isSelected()) {
                  cbDamageType.requestFocusInWindow();
                }
                break;
              case ITEM_PROJECTILE:
                cbProjectile.setEnabled(cb.isSelected());
                if (cb.isSelected()) {
                  cbProjectile.requestFocusInWindow();
                }
                break;
              case ITEM_EFFECTS:
                bpwEffects.setEnabled(cb.isSelected());
                if (cb.isSelected()) {
                  bpwEffects.requestFocusInWindow();
                }
                break;
            }
            break;
          }
        }
      }
    }

    public boolean isActive(int itemID) {
      if (itemID >= 0 && itemID < cbItems.length) {
        return cbItems[itemID].isSelected();
      } else {
        return false;
      }
    }

    public boolean isOptionOneAbilityExclusive() {
      return cbOneAbilityExclusive.isSelected();
    }

    public int getOptionType() {
      return cbItems[ITEM_TYPE].isSelected() ? ((IndexedString) cbType.getSelectedItem()).index : 0;
    }

    public int getOptionTarget() {
      return cbItems[ITEM_TARGET].isSelected() ? ((IndexedString) cbTarget.getSelectedItem()).index : 0;
    }

    public int getOptionLauncher() {
      return cbItems[ITEM_LAUNCHER].isSelected() ? ((IndexedString) cbLauncher.getSelectedItem()).index : 0;
    }

    public int getOptionProjectile() {
      return cbItems[ITEM_PROJECTILE].isSelected() ? ((IndexedString) cbProjectile.getSelectedItem()).index : 0;
    }

    public int getOptionDamageType() {
      return cbItems[ITEM_DAMAGETYPE].isSelected() ? ((IndexedString) cbDamageType.getSelectedItem()).index : 0;
    }

    public Couple<Integer, Integer> getOptionRange() {
      return Utils.getRangeValues(cbItems[ITEM_RANGE], sRange);
    }

    public Couple<Integer, Integer> getOptionSpeed() {
      return Utils.getRangeValues(cbItems[ITEM_SPEED], sSpeed);
    }

    public Couple<Integer, Integer> getOptionDiceCount() {
      return Utils.getRangeValues(cbItems[ITEM_DICECOUNT], sDiceCount);
    }

    public Couple<Integer, Integer> getOptionDiceSize() {
      return Utils.getRangeValues(cbItems[ITEM_DICESIZE], sDiceSize);
    }

    public Couple<Integer, Integer> getOptionCharges() {
      return Utils.getRangeValues(cbItems[ITEM_CHARGES], sCharges);
    }

    public Couple<Integer, Integer> getOptionEffects(int idx) {
      return cbItems[ITEM_EFFECTS].isSelected() ? pEffects.getOptionEffect(idx) : Couple.with(0, 0);
    }

    public Couple<Object, Object> getOptionFlags() {
      return cbItems[ITEM_FLAGS].isSelected() ? flagsPanel.getOptionFlags() : Couple.with(0, false);
    }

    private void init() {
      for (int i = 0; i < LABELS.length; i++) {
        cbItems[i] = new JCheckBox(LABELS[i]);
        cbItems[i].addActionListener(this);
      }

      cbType = Utils.defaultWidth(new AutoComboBox<>(IndexedString.createArray(AbstractAbility.TYPE_ARRAY, 0, 0)));
      cbTarget = Utils.defaultWidth(new AutoComboBox<>(IndexedString.createArray(AbstractAbility.TARGET_TYPE_ARRAY, 0, 0)));
      cbLauncher = Utils.defaultWidth(new AutoComboBox<>(IndexedString.createArray(Ability.LAUNCHER_ARRAY, 0, 0)));
      cbDamageType = Utils.defaultWidth(new AutoComboBox<>(IndexedString.createArray(AbstractAbility.DMG_TYPE_ARRAY, 0, 0)));

      final IndexedString[] pro;
      if (ResourceFactory.resourceExists("PROJECTL.IDS")) {
        ProRef proRef = new ProRef(StreamUtils.getByteBuffer(2), 0, "Projectile");
        pro = new IndexedString[proRef.getBitmap().size()];
        int i = 0;
        for (final ResourceBitmap.RefEntry entry : proRef.getBitmap().values()) {
          long id = entry.getValue();
          pro[i] = new IndexedString(entry.getResourceName(), (int) id);
          i++;
        }
      } else if (Profile.getEngine() == Profile.Engine.PST) {
        pro = IndexedString.createArray(AbstractAbility.PROJ_PST_ARRAY, 0, 0);
      } else if (Profile.getEngine() == Profile.Engine.IWD || Profile.getEngine() == Profile.Engine.IWD2) {
        pro = IndexedString.createArray(AbstractAbility.PROJ_IWD_ARRAY, 0, 0);
      } else {
        pro = IndexedString.createArray(AbstractAbility.PROJECTILE_ARRAY, 0, 0);
      }
      cbProjectile = Utils.defaultWidth(new AutoComboBox<>(pro));

      bpwFlags = Utils.defaultWidth(new ButtonPopupWindow(SET_OPTIONS_TEXT, flagsPanel));

      sRange[0] = Utils.createNumberSpinner(Short.MIN_VALUE, Short.MAX_VALUE, -32768, 32767, 0, 1);
      sRange[1] = Utils.createNumberSpinner(Short.MIN_VALUE, Short.MAX_VALUE, -32768, 32767, 32767, 1);

      sSpeed[0] = Utils.createNumberSpinner(Byte.MIN_VALUE, Byte.MAX_VALUE, -128, 127, 0, 1);
      sSpeed[1] = Utils.createNumberSpinner(Byte.MIN_VALUE, Byte.MAX_VALUE, -128, 127, 127, 1);

      sDiceCount[0] = Utils.createNumberSpinner(Byte.MIN_VALUE, Byte.MAX_VALUE, -128, 127, 0, 1);
      sDiceCount[1] = Utils.createNumberSpinner(Byte.MIN_VALUE, Byte.MAX_VALUE, -128, 127, 127, 1);

      sDiceSize[0] = Utils.createNumberSpinner(Byte.MIN_VALUE, Byte.MAX_VALUE, -128, 127, 0, 1);
      sDiceSize[1] = Utils.createNumberSpinner(Byte.MIN_VALUE, Byte.MAX_VALUE, -128, 127, 127, 1);

      sCharges[0] = Utils.createNumberSpinner(Short.MIN_VALUE, Short.MAX_VALUE, -32768, 32767, 0, 1);
      sCharges[1] = Utils.createNumberSpinner(Short.MIN_VALUE, Short.MAX_VALUE, -32768, 32767, 32767, 1);

      pEffects = new EffectsPanel(3, "Effect opcode");
      bpwEffects = new ButtonPopupWindow(SET_OPTIONS_TEXT, pEffects);

      cbOneAbilityExclusive = new JCheckBox("Apply filters on a per ability basis", true);

      // placing components
      GridBagConstraints c = new GridBagConstraints();
      JPanel panel = new JPanel(new GridBagLayout());
      // first column
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 0, 0, 0), 0, 0);
      panel.add(cbItems[ITEM_TYPE], c);
      c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 8, 0, 0), 0, 0);
      panel.add(cbType, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      panel.add(cbItems[ITEM_TARGET], c);
      c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 8, 0, 0), 0, 0);
      panel.add(cbTarget, c);
      c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      panel.add(cbItems[ITEM_LAUNCHER], c);
      c = ViewerUtil.setGBC(c, 1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 8, 0, 0), 0, 0);
      panel.add(cbLauncher, c);
      c = ViewerUtil.setGBC(c, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      panel.add(cbItems[ITEM_RANGE], c);
      c = ViewerUtil.setGBC(c, 1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 8, 0, 0), 0, 0);
      panel.add(Utils.createNumberRangePanel(sRange[0], sRange[1]), c);
      c = ViewerUtil.setGBC(c, 0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      panel.add(cbItems[ITEM_DICECOUNT], c);
      c = ViewerUtil.setGBC(c, 1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 8, 0, 0), 0, 0);
      panel.add(Utils.createNumberRangePanel(sDiceCount[0], sDiceCount[1]), c);
      c = ViewerUtil.setGBC(c, 0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      panel.add(cbItems[ITEM_DICESIZE], c);
      c = ViewerUtil.setGBC(c, 1, 5, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 8, 0, 0), 0, 0);
      panel.add(Utils.createNumberRangePanel(sDiceSize[0], sDiceSize[1]), c);

      c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 16, 0, 0), 0, 0);
      panel.add(cbItems[ITEM_SPEED], c);
      c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 8, 0, 0), 0, 0);
      panel.add(Utils.createNumberRangePanel(sSpeed[0], sSpeed[1]), c);
      c = ViewerUtil.setGBC(c, 2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      panel.add(cbItems[ITEM_CHARGES], c);
      c = ViewerUtil.setGBC(c, 3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 8, 0, 0), 0, 0);
      panel.add(Utils.createNumberRangePanel(sCharges[0], sCharges[1]), c);
      c = ViewerUtil.setGBC(c, 2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      panel.add(cbItems[ITEM_FLAGS], c);
      c = ViewerUtil.setGBC(c, 3, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 8, 0, 0), 0, 0);
      panel.add(bpwFlags, c);
      c = ViewerUtil.setGBC(c, 2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      panel.add(cbItems[ITEM_DAMAGETYPE], c);
      c = ViewerUtil.setGBC(c, 3, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 8, 0, 0), 0, 0);
      panel.add(cbDamageType, c);
      c = ViewerUtil.setGBC(c, 2, 4, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      panel.add(cbItems[ITEM_PROJECTILE], c);
      c = ViewerUtil.setGBC(c, 3, 4, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 8, 0, 0), 0, 0);
      panel.add(cbProjectile, c);
      c = ViewerUtil.setGBC(c, 2, 5, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      panel.add(cbItems[ITEM_EFFECTS], c);
      c = ViewerUtil.setGBC(c, 3, 5, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 8, 0, 0), 0, 0);
      panel.add(bpwEffects, c);

      triggerActions(cbItems);

      JPanel pBottom = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 0, 0, 0), 0, 0);
      pBottom.add(cbOneAbilityExclusive, c);

      JPanel pMain = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH,
          new Insets(8, 8, 0, 8), 0, 0);
      pMain.add(panel, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH,
          new Insets(8, 8, 8, 8), 0, 0);
      pMain.add(pBottom, c);
      add(pMain, BorderLayout.CENTER);
    }
  }

  /** Creates a dialog that allows to specify spell ability properties. */
  private static final class SplAbilityPanel extends BasePanel {
    private static final int SPELL_TYPE = 0;
    private static final int SPELL_LOCATION = 1;
    private static final int SPELL_TARGET = 2;
    private static final int SPELL_RANGE = 3;
    private static final int SPELL_LEVEL = 4;
    private static final int SPELL_SPEED = 5;
    private static final int SPELL_PROJECTILE = 6;
    private static final int SPELL_EFFECTS = 7;

    private static final String[] LABELS = { "Type:", "Location:", "Target:", "Range (feet):", "Min. level:",
        "Casting speed:", "Projectile:", "Effect opcodes:", };

    private final JCheckBox[] cbSpells = new JCheckBox[LABELS.length];
    private final JSpinner[] sRange = new JSpinner[2];
    private final JSpinner[] sLevel = new JSpinner[2];
    private final JSpinner[] sSpeed = new JSpinner[2];
    private EffectsPanel pEffects;
    private JComboBox<IndexedString> cbType;
    private JComboBox<IndexedString> cbLocation;
    private JComboBox<IndexedString> cbTarget;
    private JComboBox<IndexedString> cbProjectile;
    private JCheckBox cbOneAbilityExclusive;
    private ButtonPopupWindow bpwEffects;

    public SplAbilityPanel() {
      super();
      init();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      if (event.getSource() instanceof JCheckBox) {
        JCheckBox cb = (JCheckBox) event.getSource();
        for (int i = 0; i < cbSpells.length; i++) {
          if (cb == cbSpells[i]) {
            switch (i) {
              case SPELL_TYPE:
                cbType.setEnabled(cb.isSelected());
                if (cb.isSelected()) {
                  cbType.requestFocusInWindow();
                }
                break;
              case SPELL_LOCATION:
                cbLocation.setEnabled(cb.isSelected());
                if (cb.isSelected()) {
                  cbLocation.requestFocusInWindow();
                }
                break;
              case SPELL_TARGET:
                cbTarget.setEnabled(cb.isSelected());
                if (cb.isSelected()) {
                  cbTarget.requestFocusInWindow();
                }
                break;
              case SPELL_RANGE:
                sRange[0].setEnabled(cb.isSelected());
                sRange[1].setEnabled(cb.isSelected());
                if (cb.isSelected()) {
                  sRange[0].requestFocusInWindow();
                }
                break;
              case SPELL_LEVEL:
                sLevel[0].setEnabled(cb.isSelected());
                sLevel[1].setEnabled(cb.isSelected());
                if (cb.isSelected()) {
                  sLevel[0].requestFocusInWindow();
                }
                break;
              case SPELL_SPEED:
                sSpeed[0].setEnabled(cb.isSelected());
                sSpeed[1].setEnabled(cb.isSelected());
                if (cb.isSelected()) {
                  sSpeed[0].requestFocusInWindow();
                }
                break;
              case SPELL_PROJECTILE:
                cbProjectile.setEnabled(cb.isSelected());
                if (cb.isSelected()) {
                  cbProjectile.requestFocusInWindow();
                }
                break;
              case SPELL_EFFECTS:
                bpwEffects.setEnabled(cb.isSelected());
                if (cb.isSelected()) {
                  bpwEffects.requestFocusInWindow();
                }
                break;
            }
            break;
          }
        }
      }
    }

    public boolean isActive(int spellID) {
      if (spellID >= 0 && spellID < cbSpells.length) {
        return cbSpells[spellID].isSelected();
      } else {
        return false;
      }
    }

    public boolean isOptionOneAbilityExclusive() {
      return cbOneAbilityExclusive.isSelected();
    }

    public int getOptionType() {
      return cbSpells[SPELL_TYPE].isSelected() ? ((IndexedString) cbType.getSelectedItem()).index : 0;
    }

    public int getOptionLocation() {
      return cbSpells[SPELL_LOCATION].isSelected() ? ((IndexedString) cbLocation.getSelectedItem()).index : 0;
    }

    public int getOptionTarget() {
      return cbSpells[SPELL_TARGET].isSelected() ? ((IndexedString) cbTarget.getSelectedItem()).index : 0;
    }

    public Couple<Integer, Integer> getOptionRange() {
      return Utils.getRangeValues(cbSpells[SPELL_RANGE], sRange);
    }

    public Couple<Integer, Integer> getOptionLevel() {
      return Utils.getRangeValues(cbSpells[SPELL_LEVEL], sLevel);
    }

    public Couple<Integer, Integer> getOptionSpeed() {
      return Utils.getRangeValues(cbSpells[SPELL_SPEED], sSpeed);
    }

    public int getOptionProjectile() {
      return cbSpells[SPELL_PROJECTILE].isSelected() ? ((IndexedString) cbProjectile.getSelectedItem()).index : 0;
    }

    public Couple<Integer, Integer> getOptionEffects(int effectIdx) {
      return cbSpells[SPELL_EFFECTS].isSelected() ? pEffects.getOptionEffect(effectIdx) : Couple.with(0, 0);
    }

    private void init() {
      for (int i = 0; i < LABELS.length; i++) {
        cbSpells[i] = new JCheckBox(LABELS[i]);
        cbSpells[i].addActionListener(this);
      }

      cbType = Utils.defaultWidth(new AutoComboBox<>(IndexedString.createArray(AbstractAbility.TYPE_ARRAY, 0, 0)));
      cbLocation = Utils.defaultWidth(new AutoComboBox<>(IndexedString.createArray(Ability.ABILITY_USE_ARRAY, 0, 0)));
      cbTarget = Utils.defaultWidth(new AutoComboBox<>(IndexedString.createArray(AbstractAbility.TARGET_TYPE_ARRAY, 0, 0)));

      final IndexedString[] pro;
      if (ResourceFactory.resourceExists("PROJECTL.IDS")) {
        ProRef proRef = new ProRef(StreamUtils.getByteBuffer(2), 0, "Projectile");
        pro = new IndexedString[proRef.getBitmap().size()];
        int i = 0;
        for (final ResourceBitmap.RefEntry entry : proRef.getBitmap().values()) {
          long id = entry.getValue();
          pro[i] = new IndexedString(entry.getResourceName(), (int) id);
          i++;
        }
      } else if (Profile.getEngine() == Profile.Engine.PST) {
        pro = IndexedString.createArray(AbstractAbility.PROJ_PST_ARRAY, 1, 0);
      } else if (Profile.getEngine() == Profile.Engine.IWD || Profile.getEngine() == Profile.Engine.IWD2) {
        pro = IndexedString.createArray(AbstractAbility.PROJ_IWD_ARRAY, 1, 0);
      } else {
        pro = IndexedString.createArray(AbstractAbility.PROJECTILE_ARRAY, 1, 0);
      }
      cbProjectile = Utils.defaultWidth(new AutoComboBox<>(pro));

      sRange[0] = Utils.createNumberSpinner(Short.MIN_VALUE, Short.MAX_VALUE, -32768, 32767, 0, 1);
      sRange[1] = Utils.createNumberSpinner(Short.MIN_VALUE, Short.MAX_VALUE, -32768, 32767, 32767, 1);

      sLevel[0] = Utils.createNumberSpinner(Short.MIN_VALUE, Short.MAX_VALUE, -32768, 32767, 0, 1);
      sLevel[1] = Utils.createNumberSpinner(Short.MIN_VALUE, Short.MAX_VALUE, -32768, 32767, 100, 1);

      sSpeed[0] = Utils.createNumberSpinner(Short.MIN_VALUE, Short.MAX_VALUE, -32768, 32767, 0, 1);
      sSpeed[1] = Utils.createNumberSpinner(Short.MIN_VALUE, Short.MAX_VALUE, -32768, 32767, 99, 1);

      pEffects = new EffectsPanel(3, "Effect opcode");
      bpwEffects = new ButtonPopupWindow(SET_OPTIONS_TEXT, pEffects);

      cbOneAbilityExclusive = new JCheckBox("Apply filters on a per ability basis", true);

      // placing components
      GridBagConstraints c = new GridBagConstraints();
      JPanel panel = new JPanel(new GridBagLayout());
      // first column
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 0, 0, 0), 0, 0);
      panel.add(cbSpells[SPELL_TYPE], c);
      c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 8, 0, 0), 0, 0);
      panel.add(cbType, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      panel.add(cbSpells[SPELL_LOCATION], c);
      c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 8, 0, 0), 0, 0);
      panel.add(cbLocation, c);
      c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      panel.add(cbSpells[SPELL_TARGET], c);
      c = ViewerUtil.setGBC(c, 1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 8, 0, 0), 0, 0);
      panel.add(cbTarget, c);
      c = ViewerUtil.setGBC(c, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 0, 0, 0), 0, 0);
      panel.add(cbSpells[SPELL_RANGE], c);
      c = ViewerUtil.setGBC(c, 1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 8, 0, 0), 0, 0);
      panel.add(Utils.createNumberRangePanel(sRange[0], sRange[1]), c);

      c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 16, 0, 0), 0, 0);
      panel.add(cbSpells[SPELL_LEVEL], c);
      c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 8, 0, 0), 0, 0);
      panel.add(Utils.createNumberRangePanel(sLevel[0], sLevel[1]), c);
      c = ViewerUtil.setGBC(c, 2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      panel.add(cbSpells[SPELL_SPEED], c);
      c = ViewerUtil.setGBC(c, 3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 8, 0, 0), 0, 0);
      panel.add(Utils.createNumberRangePanel(sSpeed[0], sSpeed[1]), c);
      c = ViewerUtil.setGBC(c, 2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      panel.add(cbSpells[SPELL_PROJECTILE], c);
      c = ViewerUtil.setGBC(c, 3, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 8, 0, 0), 0, 0);
      panel.add(cbProjectile, c);
      c = ViewerUtil.setGBC(c, 2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(4, 16, 0, 0), 0, 0);
      panel.add(cbSpells[SPELL_EFFECTS], c);
      c = ViewerUtil.setGBC(c, 3, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(4, 8, 0, 0), 0, 0);
      panel.add(bpwEffects, c);

      triggerActions(cbSpells);

      JPanel pBottom = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(0, 0, 0, 0), 0, 0);
      pBottom.add(cbOneAbilityExclusive, c);

      JPanel pMain = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH,
          new Insets(8, 8, 0, 8), 0, 0);
      pMain.add(panel, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH,
          new Insets(8, 8, 8, 8), 0, 0);
      pMain.add(pBottom, c);
      add(pMain, BorderLayout.CENTER);
    }
  }

  /** Creates a dialog that allows to specify item categories allowed in STO resources. */
  private static final class StoCategoriesPanel extends BasePanel {
    private static final int MAX_ENTRY_COUNT = 16;

    private final int entryCount;
    private final JCheckBox[] cbLabel;
    private final JComboBox<IndexedString>[] cbCategory;

    @SuppressWarnings("unchecked")
    public StoCategoriesPanel(int purchasedCount) {
      super();
      if (purchasedCount < 1) {
        purchasedCount = 1;
      } else if (purchasedCount > MAX_ENTRY_COUNT) {
        purchasedCount = MAX_ENTRY_COUNT;
      }
      entryCount = purchasedCount;
      cbLabel = new JCheckBox[entryCount];
      cbCategory = new JComboBox[entryCount];
      init();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      if (event.getSource() instanceof JCheckBox) {
        for (int i = 0; i < entryCount; i++) {
          if (event.getSource() == cbLabel[i]) {
            cbCategory[i].setEnabled(cbLabel[i].isSelected());
            if (cbLabel[i].isSelected()) {
              cbCategory[i].requestFocusInWindow();
            }
            break;
          }
        }
      }
    }

    public boolean isActive(int index) {
      if (index >= 0 && index < cbLabel.length) {
        return cbLabel[index].isSelected();
      } else {
        return false;
      }
    }

    public int getOptionPurchased(int index) {
      return cbLabel[index].isSelected() ? ((IndexedString) cbCategory[index].getSelectedItem()).index : 0;
    }

    private void init() {
      for (int i = 0; i < entryCount; i++) {
        cbLabel[i] = new JCheckBox(String.format("Category %d:", i + 1));
        cbLabel[i].addActionListener(this);

        String[] cat = ((Boolean) Profile.getProperty(Profile.Key.IS_SUPPORTED_STO_V11)) ? ItmResource.CATEGORIES11_ARRAY
            : ItmResource.CATEGORIES_ARRAY;
        cbCategory[i] = new AutoComboBox<>(IndexedString.createArray(cat, 0, 0));
      }

      // placing components
      GridBagConstraints c = new GridBagConstraints();
      JPanel panel = new JPanel(new GridBagLayout());
      for (int i = 0; i < entryCount; i++) {
        int row, col;
        if (entryCount >= (MAX_ENTRY_COUNT / 2)) {
          row = i % ((entryCount + 1) / 2);
          col = (i < (entryCount + 1) / 2) ? 0 : 2;
        } else {
          row = i;
          col = 0;
        }
        c = ViewerUtil.setGBC(c, col, row, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
            new Insets((i == 0) ? 0 : 4, (col == 2) ? 16 : 0, 0, 0), 0, 0);
        panel.add(cbLabel[i], c);
        c = ViewerUtil.setGBC(c, col + 1, row, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, new Insets((i == 0) ? 0 : 4, 8, 0, 0), 0, 0);
        panel.add(cbCategory[i], c);
      }

      triggerActions(cbLabel);

      JPanel pMain = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH,
          new Insets(8, 8, 8, 8), 0, 0);
      pMain.add(panel, c);
      add(pMain, BorderLayout.CENTER);
    }
  }

  /** Common base for {@link IndexedString} and {@link ObjectString}. */
  private interface StorageString {
    public Object getObject();
  }

  /** Associates strings with a unique integer number. */
  // TODO: Can be removed and replaced by org.infinity.util.ObjectString
  private static final class IndexedString implements StorageString {
    private final String s;
    final int index;

    /** Automatically create string/index pairs from string array. */
    public static IndexedString[] createArray(String[] strings, int startIndex, int ofsIndex) {
      if (strings == null || startIndex >= strings.length) {
        return new IndexedString[0];
      }

      final IndexedString[] retVal = new IndexedString[strings.length - startIndex];
      for (int i = 0; i < retVal.length; ++i) {
        retVal[i] = new IndexedString(strings[startIndex + i], i + ofsIndex);
      }
      return retVal;
    }

    /** Automatically create string/index pairs from HashBitmap source. */
    public static IndexedString[] createArray(Map<? extends Number, String> map) {
      if (map == null) {
        return new IndexedString[0];
      }

      final IndexedString[] retVal = new IndexedString[map.size()];
      int i = 0;
      for (Map.Entry<? extends Number, String> e : map.entrySet()) {
        retVal[i] = new IndexedString(e.getValue(), e.getKey().intValue());
        ++i;
      }
      return retVal;
    }

    public IndexedString(String s, int index) {
      this.s = (s == null || s.isEmpty()) ? "Unknown" : s;
      this.index = index;
    }

    @Override
    public Integer getObject() {
      return index;
    }

    @Override
    public String toString() {
      return String.format("%s (%d)", s, index);
    }
  }

  /** Associates strings with parameterized objects. */
  // TODO: Can be replaced by org.infinity.util.ObjectString after some tweaking
  private static final class ObjectString implements StorageString {
    private final String s;
    private final Object o;

    public static ObjectString[] from(Map<?, String> map) {
      final ObjectString[] items = new ObjectString[map.size()];
      int i = 0;
      for (Map.Entry<?, String> e : map.entrySet()) {
        items[i] = new ObjectString(e.getValue(), e.getKey());
        ++i;
      }
      return items;
    }

    public ObjectString(String s, Object o) {
      this.s = (s == null || s.isEmpty() ? "Unknown" : s);
      this.o = o;
    }

    @Override
    public Object getObject() {
      return o;
    }

    @Override
    public String toString() {
      return String.format("%s (%s)", s, (o != null) ? o.toString() : "(null)");
    }
  }

  /** Controls the maximum string length of text input components. */
  private static final class FormattedDocument extends PlainDocument {
    private final int maxLength;
    private final boolean upperCase;

    public FormattedDocument(int maxLength, boolean upperCase) {
      super();
      this.maxLength = (maxLength > 0) ? maxLength : Integer.MAX_VALUE;
      this.upperCase = upperCase;
    }

    @Override
    public void insertString(int off, String str, AttributeSet a) throws BadLocationException {
      if (str == null || str.isEmpty()) {
        return;
      }

      int newLen = maxLength - getLength();
      if (newLen > 0) {
        String s = (str.length() <= newLen) ? str : str.substring(0, newLen);
        super.insertString(off, upperCase ? s.toUpperCase(Locale.ENGLISH) : s, a);
      }
    }
  }

  private static final class Utils {
    /** Can be used to determine the preferred size of a combobox. */
    public static final String PROTOTYPE_STRING = "XXXXXXXX.XXXX (XXXXXXXXXXXX)";

    /** Returns a combobox containing all available resource of specified extensions. */
    public static JComboBox<ResourceEntry> createNamedResourceComboBox(String[] extensions, boolean usePrototype) {
      final JComboBox<ResourceEntry> cb = new JComboBox<>(new ResourceListModel(extensions));
      cb.setRenderer(new ResourceCellRenderer());
      if (usePrototype) {
        cb.setPreferredSize(Utils.getPrototypeSize(cb));
      }

      return cb;
    }

    public static <T extends JComponent> T defaultWidth(T component) {
      final int defaultWidth = 130;
      if (component != null) {
        component.setPreferredSize(new Dimension(defaultWidth, component.getPreferredSize().height));
      }
      return component;
    }

    private static <T extends JComponent> T defaultWidth(T component, int defaultSize) {
      if (defaultSize <= 0) {
        defaultSize = 130;
      }
      if (component != null) {
        component.setPreferredSize(new Dimension(defaultSize, component.getPreferredSize().height));
      }
      return component;
    }

    /** Enable or disable whether to automatically update the spinner value while typing. */
    private static void setSpinnerAutoUpdate(JSpinner spinner, boolean enable) {
      if (spinner != null) {
        JFormattedTextField ftf = (JFormattedTextField) spinner.getEditor().getComponent(0);
        if (ftf != null) {
          ((DefaultFormatter) ftf.getFormatter()).setCommitsOnValidEdit(enable);
        }
      }
    }

    /** Сreates a "min" to "max" panel. */
    public static JPanel createNumberRangePanel(JSpinner min, JSpinner max) {
      GridBagConstraints c = new GridBagConstraints();
      JPanel panel = new JPanel(new GridBagLayout());
      if (min != null && max != null) {
        setSpinnerAutoUpdate(min, true);
        setSpinnerAutoUpdate(max, true);
        c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
            new Insets(0, 0, 0, 0), 0, 0);
        panel.add(min, c);
        c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
            new Insets(0, 4, 0, 0), 0, 0);
        panel.add(new JLabel("to"), c);
        c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
            new Insets(0, 4, 0, 0), 0, 0);
        panel.add(max, c);
      }

      return panel;
    }

    public static JSpinner createNumberSpinner(int min, int max, int showMin, int showMax, int value, int step) {
      if (min > max) {
        int tmp = min;
        min = max;
        max = tmp;
      }
      if (showMin > showMax) {
        int tmp = showMin;
        showMin = showMax;
        showMax = tmp;
      }
      if (value < showMin) {
        value = showMin;
      } else if (value > showMax) {
        value = showMax;
      }
      JSpinner spinner = new JSpinner(new SpinnerNumberModel(showMin, showMin, showMax, step));
      // XXX: circumventing PreferredSize issues
      ((SpinnerNumberModel) spinner.getModel()).setMinimum(min);
      ((SpinnerNumberModel) spinner.getModel()).setMaximum(max);
      ((SpinnerNumberModel) spinner.getModel()).setValue(value);

      setSpinnerAutoUpdate(spinner, true);

      return spinner;
    }

    /**
     * Returns the ID of the IDS value specified by current selected item in {@code value}.
     *
     * @param enabled If not checked, method returns 0
     * @param value   Combobox with selectable values. If none selected, method returns 0
     */
    public static int getIdsValue(JCheckBox enabled, JComboBox<IdsMapEntry> value) {
      if (enabled.isSelected()) {
        final IdsMapEntry selected = (IdsMapEntry) value.getSelectedItem();
        return (int) selected.getID();
      }
      return 0;
    }

    public static JComboBox<IdsMapEntry> getIdsMapEntryList(int bufSize, String name, String idsResourceName) {
      final IdsBitmap ids = new IdsBitmap(StreamUtils.getByteBuffer(bufSize), 0, bufSize, name, idsResourceName);
      return getIdsMapEntryList(ids);
    }

    public static JComboBox<IdsMapEntry> getIdsMapEntryList(IdsBitmap ids) {
      final SortedMap<Long, IdsMapEntry> map = ids.getBitmap();
      final IdsMapEntry[] list = map.values().toArray(new IdsMapEntry[map.size()]);
      Arrays.sort(list);
      return defaultWidth(new AutoComboBox<>(list), 160);
    }

    /** Returns list's index of o */
    public static int getOptionIndex(Object[] list, Object o) {
      if (o instanceof JCheckBox) {
        for (int i = 0; i < list.length; i++) {
          if (list[i] == o) {
            return i;
          }
        }
      }
      return -1;
    }

    /** Returns the resource name of the specified entry. Handles "NONE" correctly. */
    public static String getResourceName(JCheckBox enabled, JComboBox<ResourceEntry> selector) {
      final ResourceEntry entry = (ResourceEntry) selector.getSelectedItem();
      if (enabled.isSelected() && entry != null && !"NONE".equalsIgnoreCase(entry.getResourceName())) {
        return entry.getResourceName();
      }
      return "";
    }

    /** Returns the object if value is an {@link StorageString}, or value otherwise. */
    public static Object getObjectFromString(JCheckBox enabled, JComboBox<? extends StorageString> selector) {
      final Object value = selector.getSelectedItem();
      if (enabled.isSelected() && value != null) {
        if (value instanceof StorageString) {
          return ((StorageString) value).getObject();
        } else {
          return value;
        }
      }
      return "";
    }

    /** Returns the min/max values of the specified spinner objects. */
    public static Couple<Integer, Integer> getRangeValues(JCheckBox enabled, JSpinner[] spinner) {
      return enabled.isSelected() ? Couple.with((Integer) spinner[0].getValue(), (Integer) spinner[1].getValue())
          : Couple.with(0, 0);
    }

    /** Returns a protype dimension object based on the height of @(code c} and the width of (@code prototype}. */
    public static Dimension getPrototypeSize(JComponent c) {
      return Misc.getPrototypeSize(c, PROTOTYPE_STRING);
    }
  }

  /** Adds "auto-select item" feature to JComboBox. */
  public static class AutoComboBox<E> extends JComboBox<E> {
    public AutoComboBox(E[] items) {
      super(items);
      setEditable(true);
      new AutoDocument<>(this);
    }
  }

  /** Implements the auto-selection of items for {@link AutoComboBox}. */
  private static class AutoDocument<E> extends PlainDocument {
    private final FocusListener editorFocusListener;
    private final JComboBox<E> comboBox;

    private ComboBoxModel<E> model;
    private JTextComponent editor;
    private boolean selecting = false;

    public AutoDocument(final JComboBox<E> comboBox) {
      this.comboBox = comboBox;
      this.model = this.comboBox.getModel();

      this.comboBox.addPropertyChangeListener(new PropertyChangeListener() {
        @SuppressWarnings("unchecked")
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          if (evt.getPropertyName().equals("editor")) {
            configureEditor((ComboBoxEditor) evt.getNewValue());
          }
          if (evt.getPropertyName().equals("model")) {
            model = (ComboBoxModel<E>) evt.getNewValue();
          }
        }
      });

      this.comboBox.addPopupMenuListener(new PopupMenuListener() {
        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
          try {
            Object item = lookupItem(getText(0, getLength()));
            if (item != null) {
              setSelectedItem(item);
            }
          } catch (BadLocationException ble) {
            ble.printStackTrace();
          }
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {
        }
      });

      editorFocusListener = new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
          highlightCompletedText(0);
        }

        @Override
        public void focusLost(FocusEvent e) {
        }
      };

      configureEditor(comboBox.getEditor());

      // handle initially selected object
      Object selected = comboBox.getSelectedItem();
      if (selected != null) {
        setText(selected.toString());
      }
      highlightCompletedText(0);
    }

    @Override
    public void remove(int offs, int len) throws BadLocationException {
      // process only when not selecting an item
      if (!selecting) {
        super.remove(offs, len);
      }
    }

    @Override
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
      // process only when not selecting an item
      if (!selecting) {
        // insert the string into the document
        super.insertString(offs, str, a);

        if (comboBox.isPopupVisible()) {
          // get the resulting string
          String content = getText(0, getLength());
          // lookup a matching item
          Object item = lookupItem(content);
          // select the item (or deselect if null)
          if (item != model.getSelectedItem()) {
            setSelectedItem(item);
          }
          updateCursor();
        }
      }
    }

    private void configureEditor(ComboBoxEditor newEditor) {
      if (editor != null) {
        editor.removeFocusListener(editorFocusListener);
      }

      if (newEditor != null) {
        editor = (JTextComponent) newEditor.getEditorComponent();
        editor.addFocusListener(editorFocusListener);
        editor.setDocument(this);
      }
    }

    /** Attempts to find a matching item from the list. */
    private Object lookupItem(String pattern) {
      if (pattern != null && !pattern.isEmpty()) {
        Object curItem = model.getSelectedItem();

        if (compareItem(curItem, pattern)) {
          return curItem;
        } else {
          for (int i = 0, max = model.getSize(); i < max; i++) {
            curItem = model.getElementAt(i);
            if (compareItem(curItem, pattern)) {
              return curItem;
            }
          }
        }
      }
      return null;
    }

    /** Compares the item with the pattern, returns true if a (partial) match has been found. */
    private boolean compareItem(Object item, String pattern) {
      if (item != null && pattern != null && !pattern.isEmpty()) {
        pattern = pattern.toUpperCase(Locale.ENGLISH);
        String name = item.toString().toUpperCase(Locale.ENGLISH);
        String id;

        Object curItem = item;
        if (curItem instanceof StorageString) { // actual data might be wrapped
          curItem = ((StorageString) curItem).getObject();
        }

        // Check against specific datatypes for more accurate results
        if (curItem instanceof IdsMapEntry) {
          id = Long.toString(((IdsMapEntry) curItem).getID());
        } else if (curItem instanceof IsNumeric) {
          id = Long.toString(((IsNumeric) curItem).getValue());
        } else if (curItem instanceof TextBitmap) {
          id = ((TextBitmap) curItem).getDescription();
        } else {
          id = curItem.toString().toUpperCase(Locale.ENGLISH);
        }

        if (name.startsWith(pattern) || id.startsWith(pattern)) {
          return true;
        }
      }
      return false;
    }

    /** Sets the editor text to the given string. */
    private void setText(String text) {
      try {
        // remove all text and insert the completed string
        super.remove(0, getLength());
        super.insertString(0, text, null);
      } catch (BadLocationException e) {
        e.printStackTrace();
      }
    }

    /** Highlights the editor text, starting at the given offset. */
    private void highlightCompletedText(int start) {
      editor.setCaretPosition(getLength());
      editor.moveCaretPosition(start);
    }

    /** Selects the specified item without triggering certain events. */
    private void setSelectedItem(Object item) {
      selecting = true;
      try {
        model.setSelectedItem(item);
      } finally {
        selecting = false;
      }
    }

    /** Prevent certain GUI themes to select the whole text automatically. */
    private void updateCursor() {
      int offs = editor.getCaretPosition();
      editor.setSelectionStart(offs);
      editor.setSelectionEnd(offs);
    }
  }
}
