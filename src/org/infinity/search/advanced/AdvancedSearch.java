// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search.advanced;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.infinity.NearInfinity;
import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.SortableTable;
import org.infinity.gui.ViewFrame;
import org.infinity.gui.ViewerUtil;
import org.infinity.gui.WindowBlocker;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.Viewable;
import org.infinity.resource.dlg.DlgResource;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.ReferenceHitFrame;
import org.infinity.util.Debugging;
import org.infinity.util.Misc;
import org.infinity.util.SimpleListModel;

public class AdvancedSearch extends ChildFrame implements Runnable
{
  /** Indicates how to evaluate filter matches against a resource. */
  public enum FilterMode {
    /** All filter must match. */
    MatchAll,
    /** At least one filter must match. */
    MatchAny,
    /** Only one filter must match. */
    MatchOne,
  }

  // Wiki link to "Advanced Search" documentation
  private static final String URL_WIKI_HELP = "https://github.com/NearInfinityBrowser/NearInfinity/wiki/Documentation-Advanced-Search";

  // Cardlayout identifiers for status bar components
  private static final String STATUS_BUTTONS  = "buttons";
  private static final String STATUS_PROGRESS = "progress";

  // Menu item indices in the filter popup menu
  private static final int MENU_FILTER_ADD        = 0;
  private static final int MENU_FILTER_CLONE      = 1;
  private static final int MENU_FILTER_EDIT       = 2;
  private static final int MENU_FILTER_REMOVE     = 3;
  private static final int MENU_FILTER_REMOVE_ALL = 4;

  // Not all resource types are available for all games
  private static final HashMap<String, Profile.Key[]> SupportedResourceTypes = new HashMap<>(25);
  static {
    SupportedResourceTypes.put("ARE", new Profile.Key[] {Profile.Key.IS_SUPPORTED_ARE_V10, Profile.Key.IS_SUPPORTED_ARE_V91});
    SupportedResourceTypes.put("CHR", new Profile.Key[] {Profile.Key.IS_SUPPORTED_CHR_V10, Profile.Key.IS_SUPPORTED_CHR_V20,
                                                         Profile.Key.IS_SUPPORTED_CHR_V21, Profile.Key.IS_SUPPORTED_CHR_V22});
    SupportedResourceTypes.put("CHU", new Profile.Key[] {Profile.Key.IS_SUPPORTED_CHU});
    SupportedResourceTypes.put("CRE", new Profile.Key[] {Profile.Key.IS_SUPPORTED_CRE_V10, Profile.Key.IS_SUPPORTED_CRE_V12,
                                                         Profile.Key.IS_SUPPORTED_CRE_V22, Profile.Key.IS_SUPPORTED_CRE_V90});
    SupportedResourceTypes.put("DLG", new Profile.Key[] {Profile.Key.IS_SUPPORTED_DLG});
    SupportedResourceTypes.put("EFF", new Profile.Key[] {Profile.Key.IS_SUPPORTED_EFF});
    SupportedResourceTypes.put("GAM", new Profile.Key[] {Profile.Key.IS_SUPPORTED_GAM_V11, Profile.Key.IS_SUPPORTED_GAM_V20,
                                                         Profile.Key.IS_SUPPORTED_GAM_V21, Profile.Key.IS_SUPPORTED_GAM_V22});
    SupportedResourceTypes.put("ITM", new Profile.Key[] {Profile.Key.IS_SUPPORTED_ITM_V10, Profile.Key.IS_SUPPORTED_ITM_V11,
                                                         Profile.Key.IS_SUPPORTED_ITM_V20});
    SupportedResourceTypes.put("PRO", new Profile.Key[] {Profile.Key.IS_SUPPORTED_PRO});
    SupportedResourceTypes.put("SPL", new Profile.Key[] {Profile.Key.IS_SUPPORTED_SPL_V1, Profile.Key.IS_SUPPORTED_SPL_V2});
    SupportedResourceTypes.put("SRC", new Profile.Key[] {Profile.Key.IS_SUPPORTED_SRC_PST});
    SupportedResourceTypes.put("STO", new Profile.Key[] {Profile.Key.IS_SUPPORTED_STO_V10, Profile.Key.IS_SUPPORTED_STO_V11,
                                                         Profile.Key.IS_SUPPORTED_STO_V90});
    SupportedResourceTypes.put("VAR", new Profile.Key[] {Profile.Key.IS_SUPPORTED_VAR});
    SupportedResourceTypes.put("VEF", new Profile.Key[] {Profile.Key.IS_SUPPORTED_VEF});
    SupportedResourceTypes.put("VVC", new Profile.Key[] {Profile.Key.IS_SUPPORTED_VVC});
    SupportedResourceTypes.put("WED", new Profile.Key[] {Profile.Key.IS_SUPPORTED_WED});
    SupportedResourceTypes.put("WFX", new Profile.Key[] {Profile.Key.IS_SUPPORTED_WFX});
    SupportedResourceTypes.put("WMP", new Profile.Key[] {Profile.Key.IS_SUPPORTED_WMP});
  }

  private final Listeners listeners = new Listeners();
  private final JFileChooser chooser;

  private JPopupMenu menuFilters;
  private JComboBox<String> cbResourceTypes;
  private ButtonGroup bgFilterMode;
  private JList<SearchOptions> filterList;
  private JButton bFilterSave, bFilterLoad;
  private JButton bFilterAdd, bFilterClone, bFilterEdit, bFilterRemove, bFilterRemoveAll;
  private JButton bSearch, bOpen, bOpenNew, bSave;
  private SortableTable listResults;
  private JLabel lResultsStatus;
  private CardLayout clBottomBar;
  private JPanel pBottomBar;
  private JProgressBar pbProgress;


  public AdvancedSearch()
  {
    super("Advanced search");
    (new SwingWorker<Void, Void>() {
      @Override
      public Void doInBackground()
      {
        try {
          init();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return null;
      }
    }).execute();

    // setting up file dialog for filter import/export
    chooser = new JFileChooser(Profile.getGameRoot().toFile());
    chooser.setFileFilter(new FileNameExtensionFilter("XML Configuration (*.xml)", "xml"));
  }

  /** Returns the currently selected resource type. */
  public String getResourceType()
  {
    return cbResourceTypes.getSelectedItem().toString();
  }

  /** Sets the specified resource type if available. Returns success state. */
  public boolean setResourceType(String type)
  {
    if (type == null)
      return false;

    type = type.toUpperCase();
    for (int i = 0, cnt = cbResourceTypes.getModel().getSize(); i < cnt; i++) {
      if (cbResourceTypes.getModel().getElementAt(i).equals(type)) {
        cbResourceTypes.setSelectedIndex(i);
        return true;
      }
    }
    return false;
  }

  /**
   * Adds the specified SearchOptions instance to the filter list.
   * If the specified filter already exists it will be updated instead.
   */
  public void addFilter(SearchOptions filter)
  {
    if (filter != null) {
      SimpleListModel<SearchOptions> model = (SimpleListModel<SearchOptions>)filterList.getModel();
      int idx = model.indexOf(filter);
      if (idx >= 0) {
        model.set(idx, filter);
      } else {
        idx = model.getSize();
        model.add(filter);
      }
      filterList.setSelectedIndex(idx);
    }
  }

  /** Returns the number of defined filters. */
  public int getFilterCount()
  {
    return filterList.getModel().getSize();
  }

  /** Returns the filter instance at the specified index. */
  public SearchOptions getFilter(int index)
  {
    if (index >= 0 && index < filterList.getModel().getSize())
      return filterList.getModel().getElementAt(index);
    return null;
  }

  /**
   * Removes the filter at the specified index from the filter list.
   * Optionally selects the next available entry in the list.
   */
  public boolean removeFilter(int idx, boolean autoSelect)
  {
    SimpleListModel<SearchOptions> model = (SimpleListModel<SearchOptions>)filterList.getModel();
    if (idx >= 0 && idx < model.getSize()) {
      model.remove(idx);
      if (autoSelect) {
        if (idx >= model.getSize())
          idx--;
        if (idx >= 0)
          filterList.setSelectedIndex(idx);
      }
      return true;
    }

    return false;
  }

  /** Removes all filters from the filter list. */
  private void removeAllFilters()
  {
    SimpleListModel<SearchOptions> model = (SimpleListModel<SearchOptions>)filterList.getModel();
    model.clear();
  }

  private void init() throws Exception
  {
    setIconImage(Icons.getIcon(Icons.ICON_FIND_16).getImage());

    GridBagConstraints c = new GridBagConstraints();

    // preparing popup menu for the filter list
    menuFilters = new JPopupMenu();
    menuFilters.addPopupMenuListener(listeners);
    for (String s : new String[] {"Add filter...", "Clone filter...", "Edit filter...", "Remove filter", "Remove all filters"}) {
      JMenuItem mi = new JMenuItem(s);
      mi.addActionListener(listeners);
      menuFilters.add(mi);
    }

    JLabel lResourceTypes = new JLabel("Resource type:");
    cbResourceTypes = new JComboBox<>(getAvailableResourceTypes());
    cbResourceTypes.addActionListener(listeners);

    JPanel pResourceTypes = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pResourceTypes.add(lResourceTypes, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 0, 0);
    pResourceTypes.add(cbResourceTypes, c);

    JRadioButton rb = new JRadioButton("Match all (AND)");
    bgFilterMode = new ButtonGroup();
    rb.setModel(new ToggleButtonDataModel(FilterMode.MatchAll));
    rb.setToolTipText("All filters must match");
    bgFilterMode.add(rb);
    rb = new JRadioButton("Match any (OR)");
    rb.setModel(new ToggleButtonDataModel(FilterMode.MatchAny));
    rb.setToolTipText("One or more filters must match");
    bgFilterMode.add(rb);
    rb = new JRadioButton("Match one (XOR)");
    rb.setModel(new ToggleButtonDataModel(FilterMode.MatchOne));
    rb.setToolTipText("Only one filter must match");
    bgFilterMode.add(rb);

    JPanel pFilterMode = new JPanel(new GridLayout(1, bgFilterMode.getButtonCount(), 8, 0));
    pFilterMode.setBorder(BorderFactory.createTitledBorder("Filter mode:"));
    for (Enumeration<AbstractButton> list = bgFilterMode.getElements(); list.hasMoreElements();) {
      AbstractButton b = list.nextElement();
      b.setSelected(FilterMode.MatchAll.equals(((ToggleButtonDataModel)b.getModel()).getData()));
      pFilterMode.add(b);
    }

    JPanel pFilterSettings = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pFilterSettings.add(pResourceTypes, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 16, 0, 0), 0, 0);
    pFilterSettings.add(pFilterMode, c);

    // clickable help link
    JLabel lDocLink = ViewerUtil.createUrlLabel("Help", URL_WIKI_HELP);
    lDocLink.setToolTipText(URL_WIKI_HELP);

    JLabel lFilterList = new JLabel("Filter list:");
    JPanel pFilterListTitle = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pFilterListTitle.add(lFilterList, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 16, 0, 4), 0, 0);
    pFilterListTitle.add(lDocLink, c);

    filterList = new JList<>(new SimpleListModel<>());
    filterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    filterList.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getScriptFont()));
    filterList.setPrototypeCellValue(new SearchOptions());
    filterList.addListSelectionListener(listeners);
    filterList.getModel().addListDataListener(listeners);
    filterList.addMouseListener(listeners);
    JScrollPane scrollFilterList = new JScrollPane(filterList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scrollFilterList.setMinimumSize(new Dimension(filterList.getFixedCellWidth() + 8, filterList.getFixedCellHeight() * 6));
    scrollFilterList.setPreferredSize(new Dimension(8, scrollFilterList.getMinimumSize().height));

    bFilterSave = new JButton("Save filters...", Icons.getIcon(Icons.ICON_IMPORT_16));
    bFilterSave.setEnabled(false);
    bFilterSave.addActionListener(listeners);
    bFilterLoad = new JButton("Load filters...", Icons.getIcon(Icons.ICON_EXPORT_16));
    bFilterLoad.addActionListener(listeners);

    bFilterAdd = new JButton("Add...");
    bFilterAdd.addActionListener(listeners);
    bFilterAdd.setToolTipText("Add a new filter");
    bFilterClone = new JButton("Clone...");
    bFilterClone.addActionListener(listeners);
    bFilterClone.setEnabled(false);
    bFilterClone.setToolTipText("Add a copy of the selected filter");
    bFilterEdit = new JButton("Edit...");
    bFilterEdit.setEnabled(false);
    bFilterEdit.addActionListener(listeners);
    bFilterEdit.setToolTipText("Edit the selected filter");
    bFilterRemove = new JButton("Remove");
    bFilterRemove.setEnabled(false);
    bFilterRemove.addActionListener(listeners);
    bFilterRemove.setToolTipText("Remove the selected filter");
    bFilterRemoveAll = new JButton("Clear");
    bFilterRemoveAll.setEnabled(false);
    bFilterRemoveAll.addActionListener(listeners);
    bFilterRemoveAll.setToolTipText("Remove all filters");

    bSearch = new JButton("Search", Icons.getIcon(Icons.ICON_FIND_16));
    bSearch.setEnabled(false);
    bSearch.addActionListener(listeners);

    bOpen = new JButton("Open", Icons.getIcon(Icons.ICON_OPEN_16));
    bOpen.setMnemonic('o');
    bOpen.setEnabled(false);
    bOpen.addActionListener(listeners);
    bOpenNew = new JButton("Open in new window", Icons.getIcon(Icons.ICON_OPEN_16));
    bOpenNew.setMnemonic('n');
    bOpenNew.setEnabled(false);
    bOpenNew.addActionListener(listeners);
    bSave = new JButton("Save...", Icons.getIcon(Icons.ICON_SAVE_16));
    bSave.setMnemonic('s');
    bSave.setEnabled(false);
    bSave.addActionListener(listeners);

    listResults = new SortableTable(new String[]{"File", "Name", "Attribute"},
                                    new Class<?>[]{ResourceEntry.class, String.class, String.class},
                                    new Integer[]{100, 150, 300});
    listResults.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getScriptFont()));
    listResults.setRowHeight(listResults.getFontMetrics(listResults.getFont()).getHeight() + 1);
    listResults.setPreferredScrollableViewportSize(new Dimension(100, listResults.getRowHeight() * 10));
    listResults.getSelectionModel().addListSelectionListener(listeners);
    listResults.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
          int row = listResults.getSelectedRow();
          if (row != -1) {
            listeners.actionPerformed(new ActionEvent(bOpen, 0, null));
          }
        }
      }
    });
    JScrollPane scrollListResults = new JScrollPane(listResults);

    JLabel lResult = new JLabel("Result:");
    lResultsStatus = new JLabel("");
    pbProgress = new JProgressBar();

    // filter buttons
    JPanel pFilterButtons = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pFilterButtons.add(bFilterSave, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pFilterButtons.add(bFilterLoad, c);
    c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pFilterButtons.add(new JPanel(), c);
    c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pFilterButtons.add(bFilterAdd, c);
    c = ViewerUtil.setGBC(c, 4, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pFilterButtons.add(bFilterClone, c);
    c = ViewerUtil.setGBC(c, 5, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pFilterButtons.add(bFilterEdit, c);
    c = ViewerUtil.setGBC(c, 6, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pFilterButtons.add(bFilterRemove, c);
    c = ViewerUtil.setGBC(c, 7, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pFilterButtons.add(bFilterRemoveAll, c);
    c = ViewerUtil.setGBC(c, 8, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 16, 0, 0), 0, 0);
    pFilterButtons.add(bSearch, c);

    // whole filter section
    JPanel pFilters = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pFilters.add(pFilterSettings, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pFilters.add(pFilterListTitle, c);
    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 1, 1, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(2, 0, 0, 0), 0, 0);
    pFilters.add(scrollFilterList, c);
    c = ViewerUtil.setGBC(c, 0, 3, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pFilters.add(pFilterButtons, c);

    // search results
    JPanel pResultList = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pResultList.add(lResult, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    pResultList.add(lResultsStatus, c);
    c = ViewerUtil.setGBC(c, 0, 1, 2, 1, 1, 1, GridBagConstraints.LINE_START, GridBagConstraints.BOTH, new Insets(2, 0, 0, 0), 0, 0);
    pResultList.add(scrollListResults, c);

    // button bar below search results
    JPanel pResultButtons = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pResultButtons.add(new JPanel(), c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pResultButtons.add(bOpen, c);
    c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pResultButtons.add(bOpenNew, c);
    c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pResultButtons.add(bSave, c);
    c = ViewerUtil.setGBC(c, 4, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pResultButtons.add(new JPanel(), c);

    // progress bar
    JPanel pProgress = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1, 1, GridBagConstraints.LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    pProgress.add(pbProgress, c);

    // button bar shares space with progress bar
    clBottomBar = new CardLayout(0, 0);
    pBottomBar = new JPanel(clBottomBar);
    pBottomBar.add(pResultButtons, STATUS_BUTTONS);
    pBottomBar.add(pProgress, STATUS_PROGRESS);
    clBottomBar.show(pBottomBar, STATUS_BUTTONS);

    // whole bottom bar
    JPanel pResultsMain = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0);
    pResultsMain.add(pBottomBar, c);

    // combine everything
    JPanel pMain = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH, new Insets(8, 8, 0, 8), 0, 0);
    pMain.add(pFilters, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1, 1, GridBagConstraints.LINE_START, GridBagConstraints.BOTH, new Insets(8, 8, 0, 8), 0, 0);
    pMain.add(pResultList, c);
    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH, new Insets(8, 8, 8, 8), 0, 0);
    pMain.add(pResultsMain, c);

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(pMain, BorderLayout.CENTER);
    getRootPane().setDefaultButton(bSearch);

    pack();
    setMinimumSize(new Dimension(getPreferredSize()));
    setLocationRelativeTo(getParent());
    setVisible(true);
  }


  @Override
  public void run()
  {
    listResults.clear();
    listResults.setEnabled(false);
    bOpen.setEnabled(false);
    bOpenNew.setEnabled(false);
    bSave.setEnabled(false);
    lResultsStatus.setText("");

    FilterMode filterOp = getCurrentFilterMode();
    if (filterOp != null) {
      // initializations
      String resType = cbResourceTypes.getSelectedItem().toString();
      List<ResourceEntry> resources = ResourceFactory.getResources(resType);
      final Vector<ReferenceHitFrame.ReferenceHit> found = new Vector<>();
      bSearch.setEnabled(false);
      pbProgress.setMinimum(0);
      pbProgress.setMaximum(resources.size());
      pbProgress.setValue(0);
      clBottomBar.show(pBottomBar, STATUS_PROGRESS);
      WindowBlocker blocker = new WindowBlocker(this);
      blocker.setBlocked(true);

      // executing search
      try {
        Debugging.timerReset();
        List<SearchOptions> searchOptions = getSearchOptions();

        // using parallel jobs to speed up search
        ThreadPoolExecutor executor = Misc.createThreadPool();
        for (final ResourceEntry entry : resources) {
          Misc.isQueueReady(executor, true, -1);
          executor.execute(new AdvancedSearchWorker(found, filterOp, searchOptions, entry, pbProgress));
        }

        // waiting for threads to finish
        executor.shutdown();
        try {
          executor.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }

        // preparing results for output
        int resourceCount = 0;
        if (!found.isEmpty()) {
          Collections.sort(found);
          for (ReferenceHitFrame.ReferenceHit hit : found) {
            listResults.addTableItem(hit);
          }
          listResults.tableComplete();
          listResults.setEnabled(true);
          listResults.getSelectionModel().setSelectionInterval(0, 0);
          listResults.ensureIndexIsVisible(listResults.getSelectedRow());

          // determine number of unique resource entries in the results list
          HashSet<ResourceEntry> entrySet = new HashSet<>();
          resourceCount = found.stream().filter(e -> entrySet.add(e.getResource())).collect(Collectors.toList()).size();
        }

        lResultsStatus.setText(String.format("(%d match%s in %d resource%s found)",
                                             found.size(), found.size() == 1 ? "" : "es",
                                             resourceCount, resourceCount == 1 ? "" : "s"));
      } finally {
        Debugging.timerShow("Advanced Search", Debugging.TimeFormat.MILLISECONDS);
        blocker.setBlocked(false);
        bSearch.setEnabled(true);
        clBottomBar.show(pBottomBar, STATUS_BUTTONS);
      }
    }
  }

  /** Called whenever the content of a list control changes. */
  public void listDataChanged(Object source)
  {
    if (source == filterList.getModel()) {
      bSearch.setEnabled(filterList.getModel().getSize() > 0);
      bFilterSave.setEnabled(filterList.getModel().getSize() > 0);
      bFilterRemoveAll.setEnabled(filterList.getModel().getSize() > 0);
    }
  }

  /** Attempts to show the requested structure or substructure of a resource in a viewer. */
  private void showEntryInViewer(int row, Viewable viewable)
  {
    if (viewable instanceof DlgResource) {
      DlgResource dlgRes = (DlgResource) viewable;
      JComponent detailViewer = dlgRes.getViewerTab(0);
      JTabbedPane parent = (JTabbedPane) detailViewer.getParent();
      dlgRes.selectInEdit(((ReferenceHitFrame.ReferenceHit)listResults.getTableItemAt(row)).getStructEntry());
      // make sure we see the detail viewer
      parent.getModel().setSelectedIndex(parent.indexOfComponent(detailViewer));
    } else if (viewable instanceof AbstractStruct) {
      ((AbstractStruct)viewable).getViewer().selectEntry(((ReferenceHitFrame.ReferenceHit)listResults.getTableItemAt(row)).getStructEntry().getOffset());
    }
  }

  /** Returns the currently selected filter mode. Returns {@code null} on error. */
  private FilterMode getCurrentFilterMode()
  {
    try {
      return (FilterMode)((ToggleButtonDataModel)bgFilterMode.getSelection()).getData();
    } catch (Exception e) {
    }
    return null;
  }

  /** Returns a list of currently defined search options. */
  private List<SearchOptions> getSearchOptions()
  {
    SimpleListModel<SearchOptions> model = (SimpleListModel<SearchOptions>)filterList.getModel();
    return Collections.list(model.elements());
  }

  /** Returns the currently selected filter mode. */
  private FilterMode getSelectedFilterMode()
  {
    Object o = ((ToggleButtonDataModel)bgFilterMode.getSelection()).getData();
    if (o instanceof FilterMode)
      return (FilterMode)o;
    return FilterMode.MatchAll;
  }

  /** Sets filter mode according to specified parameter. */
  private void setFilterMode(FilterMode mode)
  {
    if (mode != null) {
      for (Enumeration<AbstractButton> list = bgFilterMode.getElements(); list.hasMoreElements();) {
        AbstractButton b = list.nextElement();
        b.setSelected(mode.equals(((ToggleButtonDataModel)b.getModel()).getData()));
      }
    }
  }

  /** Imports a new search configuration from the specified xml file. */
  private boolean importConfig(File xmlFile)
  {
    try {
      XmlConfig cfg = XmlConfig.Import(xmlFile);
      cbResourceTypes.setSelectedItem(cfg.getResourceType());
      setFilterMode(cfg.getFilterMode());
      removeAllFilters();
      for (final SearchOptions so : cfg.getFilters()) {
        addFilter(so);
      }
      if (filterList.getModel().getSize() > 0)
        filterList.setSelectedIndex(0);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  /** Exports the current serach configuration to the specified xml file. */
  private boolean exportConfig(File xmlFile)
  {
    try {
      return XmlConfig.Export(xmlFile, cbResourceTypes.getSelectedItem().toString(), getSelectedFilterMode(), getSearchOptions());
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  /** Returns a list of supported structured resource types by the game, based on list of available resource types. */
  public static Vector<String> getAvailableResourceTypes()
  {
    Vector<String> list = new Vector<>();
    for (Map.Entry<String, Profile.Key[]> e : SupportedResourceTypes.entrySet()) {
      boolean supported = false;
      for (Profile.Key key : e.getValue()) {
        boolean b = Profile.getProperty(key);
        supported |= b;
      }
      if (supported)
        list.add(e.getKey());
    }
    Collections.sort(list);
    return list;
  }


//-------------------------- INNER CLASSES --------------------------

  private class Listeners
    implements ActionListener, MouseListener, ListSelectionListener, ListDataListener, PopupMenuListener
  {
    // --------------------- Interface ActionListener ---------------------

    @Override
    public void actionPerformed(ActionEvent event)
    {
      if (event.getSource() == cbResourceTypes) {
        // update border label in Filter input dialog when resource type changes
        FilterInput dlg = ChildFrame.getFirstFrame(FilterInput.class);
        if (dlg != null) {
          dlg.updateResourceType(cbResourceTypes.getSelectedItem().toString());
        }
      } else if (event.getSource() == bFilterSave) {
        // save current search settings to file
        chooser.setDialogTitle("Save advanced search settings");
        if (chooser.showSaveDialog(AdvancedSearch.this) == JFileChooser.APPROVE_OPTION) {
          // autocomplete file extension of none present
          File selectedFile = chooser.getSelectedFile();
          if (chooser.getFileFilter().getDescription().contains("*.xml") &&
              selectedFile.getName().indexOf('.') < 0)
            selectedFile = new File(selectedFile.getAbsolutePath() + ".xml");
          if (!selectedFile.exists() ||
              JOptionPane.showOptionDialog(AdvancedSearch.this, String.format("%s exists. Overwrite?", selectedFile), "Save configuration",
                                           JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                                           new String[] {"Overwrite", "Cancel"}, "Overwrite") == JOptionPane.YES_OPTION) {
            if (exportConfig(selectedFile)) {
              JOptionPane.showMessageDialog(AdvancedSearch.this, String.format("Search configuration saved to\n%s.", selectedFile),
                                            "Save configuration", JOptionPane.INFORMATION_MESSAGE);
            } else {
              JOptionPane.showMessageDialog(AdvancedSearch.this, String.format("Could not save configuration to\n%s.", selectedFile),
                                            "Save configuration", JOptionPane.ERROR_MESSAGE);
            }
          }
        }
      } else if (event.getSource() == bFilterLoad) {
        // load search settings from file
        chooser.setDialogTitle("Load advanced search settings");
        if (chooser.showOpenDialog(AdvancedSearch.this) == JFileChooser.APPROVE_OPTION) {
          if (!chooser.getSelectedFile().exists()) {
            JOptionPane.showMessageDialog(AdvancedSearch.this, String.format("Could not find file\n%s.", chooser.getSelectedFile()),
                                          "Load configuration", JOptionPane.ERROR_MESSAGE);
          } else if (importConfig(chooser.getSelectedFile())) {
            JOptionPane.showMessageDialog(AdvancedSearch.this, String.format("Search configuration loaded from\n%s.", chooser.getSelectedFile()),
                                          "Load configuration", JOptionPane.INFORMATION_MESSAGE);
          } else {
            JOptionPane.showMessageDialog(AdvancedSearch.this, String.format("Could not load configuration data from\n%s.", chooser.getSelectedFile()),
                                          "Load configuration", JOptionPane.ERROR_MESSAGE);
          }
        }
      } else if (event.getSource() == bFilterAdd) {
        // add new filter entry
        FilterInput dlg = ChildFrame.show(FilterInput.class, () -> new FilterInput());
        if (dlg != null)
          dlg.setOptions(AdvancedSearch.this, cbResourceTypes.getSelectedItem().toString(), null);
      } else if (event.getSource() == bFilterClone) {
        // create duplicate of existing filter entry
        FilterInput dlg = ChildFrame.show(FilterInput.class, () -> new FilterInput());
        if (dlg != null)
          dlg.setOptions(AdvancedSearch.this, cbResourceTypes.getSelectedItem().toString(),
                         new SearchOptions(filterList.getSelectedValue()), true);
      } else if (event.getSource() == bFilterEdit) {
        // edit existing filter entry
        FilterInput dlg = ChildFrame.show(FilterInput.class, () -> new FilterInput());
        if (dlg != null)
          dlg.setOptions(AdvancedSearch.this, cbResourceTypes.getSelectedItem().toString(), filterList.getSelectedValue());
      } else if (event.getSource() == bFilterRemove) {
        // remove single filter entry
        int idx = filterList.getSelectedIndex();
        if (idx >= 0) {
          if (JOptionPane.showConfirmDialog(AdvancedSearch.this, "Remove selected filter?", "Confirm removal",
                                            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            removeFilter(idx, true);
          }
        }
      } else if (event.getSource() == bFilterRemoveAll) {
        // remove all filter entries
        if (filterList.getModel().getSize() > 0) {
          if (JOptionPane.showConfirmDialog(AdvancedSearch.this, "Remove all filters?", "Confirm removal",
                                            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            removeAllFilters();
          }
        }
      } else if (event.getSource() == bSearch) {
        // perform search
        if (filterList.getModel().getSize() > 0) {
          (new Thread(AdvancedSearch.this)).start();
        } else {
          JOptionPane.showMessageDialog(AdvancedSearch.this,
                                        String.format("No filter definitions specified for \"%s\".", cbResourceTypes.getSelectedItem()),
                                        "Advanced Search", JOptionPane.ERROR_MESSAGE);
        }
      } else if (event.getSource() == bSave) {
        // save current result list to file
        listResults.saveSearchResult(AdvancedSearch.this, "results.txt");
      } else if (event.getSource() == bOpen) {
        // open search result
        int row = listResults.getSelectedRow();
        if (row != -1) {
          ResourceEntry entry = (ResourceEntry)listResults.getValueAt(row, 0);
          if (entry != null) {
            NearInfinity.getInstance().showResourceEntry(entry);
            Viewable viewable = NearInfinity.getInstance().getViewable();
            showEntryInViewer(row, viewable);
            if (viewable instanceof DlgResource)
              NearInfinity.getInstance().toFront();
          }
        }
      } else if (event.getSource() == bOpenNew) {
        // open search result in new window
        int row = listResults.getSelectedRow();
        if (row != -1) {
          Resource res = ResourceFactory.getResource((ResourceEntry)listResults.getValueAt(row, 0));
          new ViewFrame(AdvancedSearch.this, res);
          showEntryInViewer(row, res);
        }
      } else if (event.getSource() instanceof JMenuItem) {
        // process menu items from filter popup menu
        JMenuItem mi = (JMenuItem)event.getSource();
        List<Component> list = Arrays.asList(menuFilters.getComponents()).stream().filter(c -> c instanceof JMenuItem).collect(Collectors.toList());
        switch (list.indexOf(mi)) {
          case MENU_FILTER_ADD:
            actionPerformed(new ActionEvent(bFilterAdd, 0, null));
            break;
          case MENU_FILTER_CLONE:
            actionPerformed(new ActionEvent(bFilterClone, 0, null));
            break;
          case MENU_FILTER_EDIT:
            actionPerformed(new ActionEvent(bFilterEdit, 0, null));
            break;
          case MENU_FILTER_REMOVE:
            actionPerformed(new ActionEvent(bFilterRemove, 0, null));
            break;
          case MENU_FILTER_REMOVE_ALL:
            actionPerformed(new ActionEvent(bFilterRemoveAll, 0, null));
            break;
        }
      }
    }

    // --------------------- Interface MouseListener ---------------------

    @Override
    public void mouseClicked(MouseEvent event)
    {
      if (event.getSource() == filterList) {
        if (!event.isPopupTrigger() && event.getClickCount() == 2) {
          // double click triggers filter edit
          Rectangle cellRect = filterList.getCellBounds(filterList.getSelectedIndex(), filterList.getSelectedIndex());
          if (cellRect != null && event.getPoint() != null && cellRect.contains(event.getPoint())) {
            listeners.actionPerformed(new ActionEvent(bFilterEdit, 0, null));
          }
        }
      }
    }

    @Override
    public void mousePressed(MouseEvent event)
    {
      if (event.getSource() == filterList) {
        if (event.isPopupTrigger()) {
          if (!menuFilters.isVisible())
            menuFilters.show(filterList, event.getX(), event.getY());
        }
      }
    }

    @Override
    public void mouseReleased(MouseEvent event)
    {
      if (event.getSource() == filterList) {
        if (event.isPopupTrigger()) {
          if (!menuFilters.isVisible())
            menuFilters.show(filterList, event.getX(), event.getY());
        }
      }
    }

    @Override
    public void mouseEntered(MouseEvent event)
    {
    }

    @Override
    public void mouseExited(MouseEvent event)
    {
    }

    // --------------------- Interface ListSelectionListener ---------------------

    @Override
    public void valueChanged(ListSelectionEvent event)
    {
      if (event.getSource() == filterList) {
        bFilterClone.setEnabled(filterList.getSelectedIndex() != -1);
        bFilterEdit.setEnabled(filterList.getSelectedIndex() != -1);
        bFilterRemove.setEnabled(filterList.getSelectedIndex() != -1);
      } else if (listResults.getSelectionModel() == event.getSource()) {
        bOpen.setEnabled(listResults.getSelectedRow() != -1);
        bOpenNew.setEnabled(listResults.getSelectedRow() != -1);
        bSave.setEnabled(listResults.getSelectedRow() != -1);
      }
    }

    // --------------------- Interface ListDataListener ---------------------

    @Override
    public void intervalAdded(ListDataEvent event)
    {
      listDataChanged(event.getSource());
    }

    @Override
    public void intervalRemoved(ListDataEvent event)
    {
      listDataChanged(event.getSource());
    }

    @Override
    public void contentsChanged(ListDataEvent event)
    {
      listDataChanged(event.getSource());
    }

    // --------------------- Interface PopupMenuListener ---------------------

    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent event)
    {
      if (event.getSource() == menuFilters) {
        // update menu item states based on current filter list state
        List<Component> items = Arrays.asList(menuFilters.getComponents()).stream().filter(c -> c instanceof JMenuItem).collect(Collectors.toList());
        for (int i = 0, cnt = items.size(); i < cnt; i++) {
          JMenuItem mi = (JMenuItem)items.get(i);
          switch (i) {
            case MENU_FILTER_ADD:
              // always enabled
              break;
            case MENU_FILTER_CLONE:
            case MENU_FILTER_EDIT:
            case MENU_FILTER_REMOVE:
              mi.setEnabled(filterList.getSelectedIndex() != -1);
              break;
            case MENU_FILTER_REMOVE_ALL:
              mi.setEnabled(filterList.getModel().getSize() > 0);
              break;
          }
        }
      }
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent event)
    {
    }

    @Override
    public void popupMenuCanceled(PopupMenuEvent event)
    {
    }
  }
}
