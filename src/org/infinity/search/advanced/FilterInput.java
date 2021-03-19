// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search.advanced;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.infinity.gui.ButtonPopupWindow;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.ViewerUtil;
import org.infinity.gui.WindowBlocker;
import org.infinity.gui.WrapLayout;
import org.infinity.icon.Icons;
import org.infinity.resource.Profile;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.ui.ResourceListModel;
import org.infinity.util.DataString;

/**
 * Input dialog for a advanced search filter definition.
 */
public class FilterInput extends ChildFrame
{
  // CardLayout identifiers for field input types
  private static final String FIELD_TYPE_TEXT   = "text";
  private static final String FIELD_TYPE_OFFSET = "offset";

  // CardLayout identifiers for value input types
  private static final String VALUE_TYPE_TEXT     = "text";
  private static final String VALUE_TYPE_NUMBER   = "number";
  private static final String VALUE_TYPE_RESOURCE = "resource";
  private static final String VALUE_TYPE_BITFIELD = "bitfield";

  // Signal names for synchronized operations
  private static final String SIGNAL_RESOURCE_INPUT = "ResourceInput";

  private final Listeners listeners = new Listeners();
  private final HashMap<String, CountDownLatch> signalMap = new HashMap<>();

  // Stores associations between formatted text fields and popup menus
  private final HashMap<JPopupMenu, JFormattedTextField> menuToTextFieldMap = new HashMap<>();

  private String resourceType;
  private AdvancedSearch advancedSearch;
  private SearchOptions searchOptions;

  private JPanel pStructure;
  private JTree treeStructure;
  private JButton bStructureAdd, bStructureRemove, bStructureEdit;
  private JCheckBox cbStructureRecursive, cbStructureRegex, cbStructureGroup;

  private JPanel pFieldInput;
  private CardLayout clFieldInput, clValueInput;
  private JComboBox<DataString<SearchOptions.FieldMode>> cbFieldType;
  private JTextField tfFieldName, tfValueStringInput;
  private JFormattedTextField ftfFieldOffsetInput;
  private JCheckBox cbFieldNameCase, cbFieldNameRegex;
  private JPopupMenu menuFieldOffset;

  private JPanel pValueInput;
  private JComboBox<DataString<SearchOptions.ValueType>> cbValueType;
  private JComboBox<DataString<SearchOptions.BitFieldMode>> cbValueBitfieldMode;
  private JComboBox<ResourceEntry> cbValueResourceInput;
  private JComboBox<String> cbValueResourceType;
  private JFormattedTextField ftfValueInputMin, ftfValueInputMax;
  private ButtonPopupWindow bpwValueBitfield;
  private JCheckBox cbValueStringCase, cbValueStringRegex, cbValueNumberRange;
  private JPopupMenu menuValueNumberMin, menuValueNumberMax;

  private JCheckBox cbInvertMatch;
  private JButton bInputReset, bInputApply, bInputClose;

  public FilterInput()
  {
    super("Filter");
    init();
  }

  /**
   * Associates a SearchOptions instance with the dialog. Specify {@code null} to create a new SearchOptions entry.
   */
  public void setOptions(AdvancedSearch dlg, String resourceType, SearchOptions so)
  {
    setOptions(dlg, resourceType, so, so == null);
  }

  /**
   * Associates a SearchOptions instance with the dialog. Specify {@code null} to create a new SearchOptions entry.
   * Use parameter {@code isNew} to override the accept button label ("Add" or "Apply").
   */
  public void setOptions(AdvancedSearch dlg, String resourceType, SearchOptions so, boolean isNew)
  {
    advancedSearch = dlg;
    this.resourceType = resourceType;
    searchOptions = so;
    if (bInputApply != null) {
      bInputApply.setText((so == null || isNew) ? "Add" : "Apply");
    }

    // initialize dialog with options
    importOptions(searchOptions);
  }

  /**
   * Returns an initialized SearchOptions instance based on the current dialog input.
   * @return An initialized SearchOptions instance.
   */
  public SearchOptions getOptions()
  {
    SearchOptions so = searchOptions;
    if (so == null)
      so = new SearchOptions();

    // initialize object with dialog input
    exportOptions(so);

    return so;
  }

  /** Updates the structure level group box title by the specified resource type. */
  public void updateResourceType(String resourceType)
  {
    if (resourceType != null && !resourceType.isEmpty()) {
      pStructure.setBorder(BorderFactory.createTitledBorder(String.format("Structure level (%s): ", resourceType)));
    } else {
      pStructure.setBorder(BorderFactory.createTitledBorder("Structure level: "));
    }
  }

  private void init()
  {
    resourceType = "";
    setIconImage(Icons.getIcon(Icons.ICON_FIND_16).getImage());

    // preparing popup menus for formatted text fields
    menuFieldOffset = new JPopupMenu();
    menuValueNumberMin = new JPopupMenu();
    menuValueNumberMax = new JPopupMenu();
    for (final JPopupMenu menu : new JPopupMenu[] {menuFieldOffset, menuValueNumberMin, menuValueNumberMax}) {
      JMenuItem mi = new JMenuItem("Edit as decimal number");
      mi.addActionListener(listeners);
      menu.add(mi);
      mi = new JMenuItem("Edit as hexadecimal number");
      mi.addActionListener(listeners);
      menu.add(mi);
      menu.addPopupMenuListener(listeners);
    }

    // preparing subsections
    pStructure = initStructureLevel();
    JPanel pField = initFieldInput();
    JPanel pValue = initValueInput();

    // Invert match option
    cbInvertMatch = new JCheckBox("Invert match");
    cbInvertMatch.setToolTipText("Add to result on mismatch");

    // dialog button bar
    bInputReset = new JButton("Reset");
    bInputReset.addActionListener(listeners);
    bInputApply = new JButton("Add");
    bInputApply.addActionListener(listeners);
    bInputClose = new JButton("Close");
    bInputClose.addActionListener(listeners);

    JPanel pButtons = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pButtons.add(bInputReset, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pButtons.add(new JPanel(), c);
    c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 0, 0);
    pButtons.add(bInputApply, c);
    c = ViewerUtil.setGBC(c, 3, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 0, 0);
    pButtons.add(bInputClose, c);

    // put everything together
    JPanel pFilterMain = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1, 1, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    pFilterMain.add(pStructure, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1, 0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    pFilterMain.add(pField, c);
    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 1, 0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    pFilterMain.add(pValue, c);
    c = ViewerUtil.setGBC(c, 0, 3, 1, 1, 1, 0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE, new Insets(4, 4, 0, 0), 0, 0);
    pFilterMain.add(cbInvertMatch, c);

    JPanel pMain = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1, 1, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(8, 8, 0, 8), 0, 0);
    pMain.add(pFilterMain, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1, 0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL, new Insets(16, 8, 8, 8), 0, 0);
    pMain.add(pButtons, c);

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(pMain, BorderLayout.CENTER);
    getRootPane().setDefaultButton(bInputApply);

    pack();
    setMinimumSize(new Dimension(getPreferredSize()));
    setLocationRelativeTo(getParent());
    setVisible(true);
  }

  // Initializes the structure level section
  private JPanel initStructureLevel()
  {
    GridBagConstraints c = new GridBagConstraints();
    treeStructure = new JTree(new DefaultMutableTreeNode("[root]"));
    treeStructure.setRootVisible(true);
    treeStructure.setShowsRootHandles(false);
    treeStructure.setEditable(true);
    treeStructure.setToolTipText("<html>Path to the substructure of the resource type to search.<br>" +
                                 "(Example: [root] &gt; Item ability &gt; Effect to search in " +
                                 "&quot;Item ability &gt; Effect&quot; substructures.)</html>");
    treeStructure.addTreeWillExpandListener(listeners);
    treeStructure.addTreeSelectionListener(listeners);
    treeStructure.addKeyListener(listeners);
    DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer)treeStructure.getCellRenderer();
    renderer.setLeafIcon(null);
    renderer.setOpenIcon(null);
    renderer.setClosedIcon(null);
    JScrollPane scrollTree = new JScrollPane(treeStructure);
    scrollTree.setPreferredSize(new Dimension(renderer.getPreferredSize().width, renderer.getPreferredSize().height * 4));

    bStructureAdd = new JButton("+");
    bStructureAdd.setToolTipText("Add substructure");
    bStructureAdd.addActionListener(listeners);
    bStructureRemove = new JButton("-");
    bStructureRemove.setToolTipText("Remove substructure");
    bStructureRemove.setEnabled(false);
    bStructureRemove.addActionListener(listeners);
    bStructureEdit = new JButton("…");
    bStructureEdit.setToolTipText("Rename substructure");
    bStructureEdit.setEnabled(false);
    bStructureEdit.addActionListener(listeners);
    cbStructureRecursive = new JCheckBox("Recursive");
    cbStructureRecursive.setToolTipText("Search in substructures as well (only available if field is searched by name");
    cbStructureRegex = new JCheckBox("Use regular expression");
    cbStructureGroup = new JCheckBox("Group filters");
    cbStructureGroup.setToolTipText("Restrict matches of filters with identical structure levels to the same substructure.");
    cbStructureGroup.setSelected(true);
    JPanel pStructureButtons = new JPanel(new GridLayout(1, 3, 8, 0));
    pStructureButtons.add(bStructureAdd);
    pStructureButtons.add(bStructureEdit);
    pStructureButtons.add(bStructureRemove);

    JPanel pStructureOptionsPanel = new JPanel(new WrapLayout(WrapLayout.LEFT, 0, 4));
    pStructureOptionsPanel.add(cbStructureRecursive);
    pStructureOptionsPanel.add(cbStructureRegex);
    pStructureOptionsPanel.add(cbStructureGroup);

    JPanel pStructureButtonPanel = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pStructureButtonPanel.add(pStructureButtons, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pStructureButtonPanel.add(pStructureOptionsPanel, c);

    JPanel pStructure = new JPanel(new GridBagLayout());
    pStructure.setBorder(BorderFactory.createTitledBorder("Structure level: "));
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1, 1, GridBagConstraints.LINE_START, GridBagConstraints.BOTH, new Insets(4, 8, 0, 8), 0, 0);
    pStructure.add(scrollTree, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(8, 8, 8, 8), 0, 0);
    pStructure.add(pStructureButtonPanel, c);

    return pStructure;
  }

  // Initializes the field input section
  private JPanel initFieldInput()
  {
    GridBagConstraints c = new GridBagConstraints();
    JLabel lFieldType = new JLabel("Search field by:");
    DefaultComboBoxModel<DataString<SearchOptions.FieldMode>> modelFieldType =
        new DefaultComboBoxModel<DataString<SearchOptions.FieldMode>>() {{
          addElement(DataString.with("Name", SearchOptions.FieldMode.ByName, "%s"));
          addElement(DataString.with("Relative Offset", SearchOptions.FieldMode.ByRelativeOffset, "%s"));
          addElement(DataString.with("Absolute Offset", SearchOptions.FieldMode.ByAbsoluteOffset, "%s"));
    }};
    cbFieldType = new JComboBox<>(modelFieldType);
    cbFieldType.setSelectedIndex(0);
    cbFieldType.addActionListener(listeners);
    JPanel pFieldType = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pFieldType.add(lFieldType, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    pFieldType.add(cbFieldType, c);

    JLabel lFieldName = new JLabel("Field name:");
    tfFieldName = new JTextField();
    cbFieldNameCase = new JCheckBox("Match case");
    cbFieldNameRegex = new JCheckBox("Use regular expression");
    JPanel pFieldName = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 2, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pFieldName.add(lFieldName, c);
    c = ViewerUtil.setGBC(c, 0, 1, 2, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pFieldName.add(tfFieldName, c);
    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(4, 0, 0, 0), 0, 0);
    pFieldName.add(cbFieldNameCase, c);
    c = ViewerUtil.setGBC(c, 1, 2, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0);
    pFieldName.add(cbFieldNameRegex, c);

    JLabel lFieldOffset = new JLabel("Field offset:");
    ftfFieldOffsetInput = new JFormattedTextField(new NumberFormatterEx(NumberFormatterEx.NumberFormat.hexadecimal, 0L, Integer.MAX_VALUE, 0L));
    ftfFieldOffsetInput.setToolTipText("Supported notations: decimal, hexadecimal (requires prefix \"0x\" or suffix \"h\")");
    ftfFieldOffsetInput.addMouseListener(listeners);
    menuToTextFieldMap.put(menuFieldOffset, ftfFieldOffsetInput);
    JPanel pFieldOffset = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pFieldOffset.add(lFieldOffset, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pFieldOffset.add(ftfFieldOffsetInput, c);
    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 1, 1, GridBagConstraints.LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    pFieldOffset.add(new JPanel(), c);

    clFieldInput = new CardLayout(0, 0);
    pFieldInput = new JPanel(clFieldInput);
    pFieldInput.add(pFieldName, FIELD_TYPE_TEXT);
    pFieldInput.add(pFieldOffset, FIELD_TYPE_OFFSET);
    clFieldInput.show(pFieldInput, FIELD_TYPE_TEXT);

    JPanel pField = new JPanel(new GridBagLayout());
    pField.setBorder(BorderFactory.createTitledBorder("Field: "));
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 8), 0, 0);
    pField.add(pFieldType, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(8, 8, 8, 8), 0, 0);
    pField.add(pFieldInput, c);

    return pField;
  }

  // Initializes the value input section
  private JPanel initValueInput()
  {
    GridBagConstraints c = new GridBagConstraints();
    JLabel lValueType = new JLabel("Value type:");
    DefaultComboBoxModel<DataString<SearchOptions.ValueType>> modelValueType =
        new DefaultComboBoxModel<DataString<SearchOptions.ValueType>>() {{
          addElement(DataString.with("Text", SearchOptions.ValueType.Text, "%s"));
          addElement(DataString.with("Number", SearchOptions.ValueType.Number, "%s"));
          addElement(DataString.with("Resource", SearchOptions.ValueType.Resource, "%s"));
          addElement(DataString.with("Bitfield", SearchOptions.ValueType.Bitfield, "%s"));
    }};
    cbValueType = new JComboBox<>(modelValueType);
    cbValueType.setSelectedIndex(0);
    cbValueType.addActionListener(listeners);

    JLabel lValue = new JLabel("Field value:");

    // text input
    tfValueStringInput = new JTextField();
    cbValueStringCase = new JCheckBox("Match case");
    cbValueStringRegex = new JCheckBox("Use regular expression");
    JPanel pValueString = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 2, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pValueString.add(tfValueStringInput, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pValueString.add(cbValueStringCase, c);
    c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(8, 4, 0, 0), 0, 0);
    pValueString.add(cbValueStringRegex, c);
    c = ViewerUtil.setGBC(c, 0, 2, 2, 1, 1, 1, GridBagConstraints.LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    pValueString.add(new JPanel(), c);

    // numeric input
    ftfValueInputMin = new JFormattedTextField(new NumberFormatterEx(NumberFormatterEx.NumberFormat.decimal, -0xffffffffL, 0xffffffffL, 0L));
    ftfValueInputMin.addMouseListener(listeners);
    ftfValueInputMin.addKeyListener(listeners);
    menuToTextFieldMap.put(menuValueNumberMin, ftfValueInputMin);
    ftfValueInputMax = new JFormattedTextField(new NumberFormatterEx(NumberFormatterEx.NumberFormat.decimal, -0xffffffffL, 0xffffffffL, 32767));
    ftfValueInputMax.addMouseListener(listeners);
    ftfValueInputMax.addKeyListener(listeners);
    ftfValueInputMax.setEnabled(false);
    menuToTextFieldMap.put(menuValueNumberMax, ftfValueInputMax);
    cbValueNumberRange = new JCheckBox("Numeric range", false);
    cbValueNumberRange.addActionListener(listeners);
    JPanel pValueNumber = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pValueNumber.add(ftfValueInputMin, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pValueNumber.add(new JLabel("to"), c);
    c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0);
    pValueNumber.add(ftfValueInputMax, c);
    c = ViewerUtil.setGBC(c, 0, 1, 3, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    pValueNumber.add(cbValueNumberRange, c);
    c = ViewerUtil.setGBC(c, 0, 2, 3, 1, 1, 1, GridBagConstraints.LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    pValueNumber.add(new JPanel(), c);

    // resource input
    cbValueResourceInput = new JComboBox<>(new ResourceListModel());
    cbValueResourceType = new JComboBox<>(getValueResourceTypes(true));
    cbValueResourceType.addActionListener(listeners);
    JPanel pValueResource = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pValueResource.add(cbValueResourceInput, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 0, 0);
    pValueResource.add(cbValueResourceType, c);
    c = ViewerUtil.setGBC(c, 0, 1, 2, 1, 1, 1, GridBagConstraints.LINE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    pValueResource.add(new JPanel(), c);

    // bitfield input
    final String[] flagsDesc = {
        "Bit 0", "Bit 1", "Bit 2", "Bit 3", "Bit 4", "Bit 5", "Bit 6", "Bit 7",
        "Bit 8", "Bit 9", "Bit 10", "Bit 11", "Bit 12", "Bit 13", "Bit 14", "Bit 15",
        "Bit 16", "Bit 17", "Bit 18", "Bit 19", "Bit 20", "Bit 21", "Bit 22", "Bit 23",
        "Bit 24", "Bit 25", "Bit 26", "Bit 27", "Bit 28", "Bit 29", "Bit 30", "Bit 31" };
    bpwValueBitfield = new ButtonPopupWindow("Set options...", new FlagsPanel(4, flagsDesc));

    JLabel lValueBitfieldMode = new JLabel("Mode:");
    DefaultComboBoxModel<DataString<SearchOptions.BitFieldMode>> modelValueBitfieldMode =
        new DefaultComboBoxModel<DataString<SearchOptions.BitFieldMode>>() {{
          addElement(DataString.with("Exact match", SearchOptions.BitFieldMode.Exact, "%s"));
          addElement(DataString.with("Match all set bits (AND)", SearchOptions.BitFieldMode.And, "%s"));
          addElement(DataString.with("Match any set bits (OR)", SearchOptions.BitFieldMode.Or, "%s"));
          addElement(DataString.with("Match one set bit (XOR)", SearchOptions.BitFieldMode.Xor, "%s"));
    }};
    cbValueBitfieldMode = new JComboBox<>(modelValueBitfieldMode);
    JPanel pValueBitfield = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 2, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pValueBitfield.add(bpwValueBitfield, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pValueBitfield.add(lValueBitfieldMode, c);
    c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 0), 0, 0);
    pValueBitfield.add(cbValueBitfieldMode, c);

    clValueInput = new CardLayout(0, 0);
    pValueInput = new JPanel(clValueInput);
    pValueInput.add(pValueString, VALUE_TYPE_TEXT);
    pValueInput.add(pValueNumber, VALUE_TYPE_NUMBER);
    pValueInput.add(pValueResource, VALUE_TYPE_RESOURCE);
    pValueInput.add(pValueBitfield, VALUE_TYPE_BITFIELD);
    clValueInput.show(pValueInput, VALUE_TYPE_TEXT);

    JPanel pValue = new JPanel(new GridBagLayout());
    pValue .setBorder(BorderFactory.createTitledBorder("Value: "));
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 8), 0, 0);
    pValue.add(lValueType, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 8), 0, 0);
    pValue.add(cbValueType, c);
    c = ViewerUtil.setGBC(c, 0, 1, 2, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 8), 0, 0);
    pValue.add(lValue, c);
    c = ViewerUtil.setGBC(c, 0, 2, 2, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(4, 8, 8, 8), 0, 0);
    pValue.add(pValueInput, c);

    return pValue;
  }

  /** Helper method: Enables or disables all components of the structure subpanel. */
  private void setStructureEnabled(boolean set)
  {
    treeStructure.setEnabled(set);
    bStructureAdd.setEnabled(set);
    bStructureEdit.setEnabled(set);
    bStructureRemove.setEnabled(set);
    cbStructureRecursive.setEnabled(set);
    cbStructureRegex.setEnabled(set);
    cbStructureGroup.setEnabled(set);
  }

  /**
   * Returns a complete list of resource types supported by the current game.
   * {@code addEmpty} indicates whether to add an empty entry at the first list position.
   */
  private static Vector<String> getValueResourceTypes(boolean addEmpty)
  {
    Vector<String> list = new Vector<>();
    String[] types = Profile.getAvailableResourceTypes();
    for (String type : types)
      list.add(type);
    Collections.sort(list);

    if (addEmpty)
      list.add(0, "");

    return list;
  }

  /** Applies options from the specified SearchOptions instance to the dialog controls. */
  private void importOptions(SearchOptions so)
  {
    // Resource structure panel
    updateResourceType(resourceType);

    // make sure options are in a valid state
    so = SearchOptions.validate(so);

    // structure level
    {
      // removing old tree nodes (except root)
      clearTreeNodes(treeStructure);

      // adding new tree nodes
      for (String s : so.getStructure()) {
        addTreeLeaf(treeStructure, s, false);
      }
      // tree may not expand automatically
      expandTreeNodes(treeStructure);

      cbStructureRecursive.setSelected(so.isStructureRecursive());
      cbStructureRegex.setSelected(so.isStructureRegex());
      cbStructureGroup.setSelected(so.isStructureGroup());
    }

    // field input
    {
      DefaultComboBoxModel<DataString<SearchOptions.FieldMode>> model =
          (DefaultComboBoxModel<DataString<SearchOptions.FieldMode>>)cbFieldType.getModel();
      int idx = 0;
      for (int i = 0; i < model.getSize(); i++) {
        DataString<SearchOptions.FieldMode> os = model.getElementAt(i);
        if (so.getSearchType().equals(os.getData())) {
          idx = i;
          break;
        }
      }
      cbFieldType.setSelectedIndex(idx);

      tfFieldName.setText(so.getSearchName());
      cbFieldNameCase.setSelected(so.isSearchNameCaseSensitive());
      cbFieldNameRegex.setSelected(so.isSearchNameRegex());
      ftfFieldOffsetInput.setValue(Long.toString(so.getSearchOffset(), 16) + "h");
    }

    // value input
    {
      DefaultComboBoxModel<DataString<SearchOptions.ValueType>> modelValue =
          (DefaultComboBoxModel<DataString<SearchOptions.ValueType>>)cbValueType.getModel();
      int idx = 0;
      for (int i = 0; i < modelValue.getSize(); i++) {
        DataString<SearchOptions.ValueType> os = modelValue.getElementAt(i);
        if (so.getValueType().equals(os.getData())) {
          idx = i;
          break;
        }
      }
      cbValueType.setSelectedIndex(idx);

      tfValueStringInput.setText(so.getValueText());
      cbValueStringCase.setSelected(so.isValueTextCaseSensitive());
      cbValueStringRegex.setSelected(so.isValueTextRegex());
      ftfValueInputMin.setValue(so.getValueNumberMin());
      ftfValueInputMax.setValue(so.getValueNumberMax());
      ftfValueInputMax.setEnabled(so.isValueNumberRange());
      cbValueNumberRange.setSelected(so.isValueNumberRange());

      if (so.getValueType() == SearchOptions.ValueType.Resource) {
        // we have to wait for the resource type list to be populated before we can select a resource entry
        signalCreate(SIGNAL_RESOURCE_INPUT);
        try {
          DefaultComboBoxModel<String> modelResType = (DefaultComboBoxModel<String>)cbValueResourceType.getModel();
          idx = Math.max(0, modelResType.getIndexOf(so.getValueResourceType()));
          cbValueResourceType.setSelectedIndex(idx);

          signalAwait(SIGNAL_RESOURCE_INPUT, 5000);

          if (!so.getValueResourceType().isEmpty()) {
            ResourceListModel modelResInput = (ResourceListModel)cbValueResourceInput.getModel();
            idx = Math.max(0, modelResInput.getIndexOf(so.getValueResource()));
            cbValueResourceInput.setSelectedIndex(idx);
          }
        } finally {
          signalRelease(SIGNAL_RESOURCE_INPUT);
        }
      } else {
        cbValueResourceType.setSelectedIndex(0);
        cbValueResourceInput.setSelectedIndex(0);
      }

      FlagsPanel fp = (FlagsPanel)bpwValueBitfield.getContent();
      fp.setValue(so.getValueBitfield());

      DefaultComboBoxModel<DataString<SearchOptions.BitFieldMode>> modelMode =
          (DefaultComboBoxModel<DataString<SearchOptions.BitFieldMode>>)cbValueBitfieldMode.getModel();
      idx = 0;
      for (int i = 0; i < modelValue.getSize(); i++) {
        DataString<SearchOptions.BitFieldMode> os = modelMode.getElementAt(i);
        if (so.getBitfieldMode().equals(os.getData())) {
          idx = i;
          break;
        }
      }
      cbValueBitfieldMode.setSelectedIndex(idx);
    }

    // invert match
    cbInvertMatch.setSelected(so.isInvertMatch());
  }

  /** Exports state of current dialog controls to the specified SearchOptions instance. */
  private void exportOptions(SearchOptions so)
  {
    if (so == null)
      return;

    // invert match
    so.setInvertMatch(cbInvertMatch.isSelected());

    // value input
    {
      DataString<SearchOptions.ValueType> os = cbValueType.getModel().getElementAt(cbValueType.getSelectedIndex());
      if (os != null) {
        SearchOptions.ValueType type = os.getData();
        switch (type) {
          case Text:
            so.setValueText(tfValueStringInput.getText(), cbValueStringCase.isSelected(), cbValueStringRegex.isSelected());
            break;
          case Number:
          {
            int min = (int)((NumberFormatterEx)ftfValueInputMin.getFormatter()).getNumericValue();
            int max = cbValueNumberRange.isSelected() ? (int)((NumberFormatterEx)ftfValueInputMax.getFormatter()).getNumericValue() : min;
            so.setValueNumber(min, max);
            break;
          }
          case Resource:
            if (cbValueResourceInput.getSelectedItem() != null)
              so.setValueResource(cbValueResourceInput.getSelectedItem().toString());
            else
              so.setValueResource("");
            break;
          case Bitfield:
          {
            FlagsPanel fp = (FlagsPanel)bpwValueBitfield.getContent();
            DataString<SearchOptions.BitFieldMode> osMode =
                cbValueBitfieldMode.getModel().getElementAt(cbValueBitfieldMode.getSelectedIndex());
            so.setValueBitfield(fp.getValue(), osMode.getData());
            break;
          }
        }
      } else {
        so.setValueText("", false, false);
      }
    }

    // field input
    {
      DataString<SearchOptions.FieldMode> os = cbFieldType.getModel().getElementAt(cbFieldType.getSelectedIndex());
      if (os != null) {
        SearchOptions.FieldMode mode = os.getData();
        switch (mode) {
          case ByName:
            so.setSearchName(tfFieldName.getText(), cbFieldNameCase.isSelected(), cbFieldNameRegex.isSelected());
            break;
          default:
            so.setSearchOffset(mode, (int)((NumberFormatterEx)ftfFieldOffsetInput.getFormatter()).getNumericValue());
        }
      } else {
        so.setSearchName("", false, false);
      }
    }

    // structure level (note: must be processed after setting field type)
    try {
      if (!so.getStructure().isEmpty())
        so.getStructure().clear();
      Object node = treeStructure.getModel().getRoot();
      while (treeStructure.getModel().getChildCount(node) > 0) {
        Object child = treeStructure.getModel().getChild(node, 0);
        if (child != null)
          so.getStructure().add(child.toString());
        node = child;
      }
    } catch (UnsupportedOperationException e) {
      // skip for field type "By Absolute Offset"
    }
    so.setStructureRecursive(cbStructureRecursive.isSelected());
    so.setStructureRegex(cbStructureRegex.isSelected());
    so.setStructureGroup(cbStructureGroup.isSelected());
  }

  /** Returns the first available leaf node of the tree. */
  private DefaultMutableTreeNode getTreeLeaf(JTree tree)
  {
    if (tree != null) {
      if (tree.getModel().getRoot() instanceof DefaultMutableTreeNode) {
        return ((DefaultMutableTreeNode)tree.getModel().getRoot()).getFirstLeaf();
      }
    }
    return null;
  }

  /** Returns the path to the parent of the specified node. */
  private TreePath getParentTreePath(JTree tree, TreeNode node)
  {
    TreePath path = null;
    if (tree != null && node != null) {
      node = node.getParent();
      if (node != null && tree.getModel() instanceof DefaultTreeModel) {
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        path = new TreePath(model.getPathToRoot(node));
      }
    }
    return path;
  }

  /** Expands the whole tree. */
  private void expandTreeNodes(JTree tree)
  {
    if (tree != null) {
      for (int row = 0; row < tree.getRowCount(); row++) {
        tree.expandRow(row);
      }
    }
  }

  /** Removes all nodes from the tree, except root. */
  private void clearTreeNodes(JTree tree)
  {
    if (tree != null) {
      if (tree.getModel() instanceof DefaultTreeModel &&
          tree.getModel().getRoot() instanceof MutableTreeNode) {
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        MutableTreeNode node = (MutableTreeNode)model.getRoot();
        if (node.getChildCount() > 0) {
          node = (MutableTreeNode)node.getChildAt(0);
          model.removeNodeFromParent(node);
        }
      }
    }
  }

  /** Removes the selected element from structure tree. */
  private void removeSelectedTreeNode(JTree tree, boolean prompt)
  {
    if (tree != null) {
      TreePath path = treeStructure.getSelectionPath();
      if (path != null) {
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        Object node = path.getLastPathComponent();
        if (node instanceof MutableTreeNode && ((MutableTreeNode)node).getParent() != null) {
          if (!prompt ||
              JOptionPane.showConfirmDialog(FilterInput.this, String.format("Remove substructure \"%s\"?",
                                            node.toString()), "Confirm removal",
                                            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            TreePath parentPath = getParentTreePath(tree, (TreeNode)node);
            if (parentPath != null)
              tree.setSelectionPath(parentPath);
            model.removeNodeFromParent((MutableTreeNode)node);
          }
        }
      }
    }
  }

  /** Adds a new leaf node to the tree with the specified text and enable edit mode if {@code edit} is {@code true}. */
  private DefaultMutableTreeNode addTreeLeaf(JTree tree, String text, boolean edit)
  {
    DefaultMutableTreeNode newNode = null;
    if (tree != null) {
      DefaultMutableTreeNode leaf = getTreeLeaf(treeStructure);
      if (leaf != null && treeStructure.getModel() instanceof DefaultTreeModel) {
        DefaultTreeModel model = (DefaultTreeModel)treeStructure.getModel();
        newNode = new DefaultMutableTreeNode(text != null ? text : "new node");
        model.insertNodeInto(newNode, leaf, 0);
        if (edit) {
          TreePath path = new TreePath(model.getPathToRoot(newNode));
          treeStructure.scrollPathToVisible(path);
          treeStructure.setSelectionPath(path);
          treeStructure.startEditingAtPath(path);
        }
      }
    }
    return newNode;
  }

  /**
   * A method for synchronization purposes. Creates a new synchronization object with a signal count of 1.
   */
  private boolean signalCreate(String name)
  {
    return signalCreate(name, 1);
  }

  /**
   * A method for synchronization purposes. Creates a new synchronization object.
   * @param name Name of the synchronization object
   * @param count Number of signals to receive before the synchronization object is marked as completed.
   */
  private boolean signalCreate(String name, int count)
  {
    if (name == null || count <= 0 || signalMap.containsKey(name))
      return false;
    signalMap.put(name, new CountDownLatch(count));
    return true;
  }

  /**
   * A method for synchronization purposes. Releases the specified synchronization object.
   */
  private void signalRelease(String name)
  {
    if (name != null && signalMap.containsKey(name))
      signalMap.remove(name);
  }

  /**
   * A method for synchronization purposes. Marks the specified synchronization object as processed.
   */
  private void signalCountdown(String name)
  {
    CountDownLatch signal = signalMap.getOrDefault(name, null);
    if (signal != null)
      signal.countDown();
  }

  /**
   * A method for synchronization purposes. Waits for the synchronization object to be processed.
   */
  private void signalAwait(String name, int timeoutMs)
  {
    CountDownLatch signal = signalMap.getOrDefault(name, null);
    if (signal != null) {
      try {
        if (timeoutMs < 0)
          timeoutMs = Integer.MAX_VALUE;
        signal.await(timeoutMs, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

//-------------------------- INNER CLASSES --------------------------

  private class Listeners
    implements ActionListener, TreeWillExpandListener, TreeSelectionListener, MouseListener, KeyListener, PopupMenuListener
  {
    // --------------------- Interface ActionListener ---------------------

    @Override
    public void actionPerformed(ActionEvent event)
    {
      if (event.getSource() == bStructureAdd) {
        // add new element to structure tree
        addTreeLeaf(treeStructure, "enter name", true);
      } else if (event.getSource() == bStructureEdit) {
        // edit selected element in structure tree
        TreePath path = treeStructure.getSelectionPath();
        if (path != null) {
          treeStructure.scrollPathToVisible(path);
          treeStructure.startEditingAtPath(path);
        }
      } else if (event.getSource() == bStructureRemove) {
        // remove selected element from structure tree
        removeSelectedTreeNode(treeStructure, true);
      } else if (event.getSource() == cbFieldType) {
        // switch field type panel
        if (cbFieldType.getSelectedIndex() >= 0) {
          DataString<SearchOptions.FieldMode> os = cbFieldType.getModel().getElementAt(cbFieldType.getSelectedIndex());
          SearchOptions.FieldMode mode = os.getData();
          switch (mode) {
            case ByName:
              clFieldInput.show(pFieldInput, FIELD_TYPE_TEXT);
              setStructureEnabled(true);
              break;
            case ByRelativeOffset:
              clFieldInput.show(pFieldInput, FIELD_TYPE_OFFSET);
              setStructureEnabled(true);
              cbStructureRecursive.setEnabled(false);
              break;
            case ByAbsoluteOffset:
              clFieldInput.show(pFieldInput, FIELD_TYPE_OFFSET);
              setStructureEnabled(false);
              break;
          }
        }
      } else if (event.getSource() == cbValueType) {
        // switch value type panel
        if (cbValueType.getSelectedIndex() >= 0) {
          DataString<SearchOptions.ValueType> os = cbValueType.getModel().getElementAt(cbValueType.getSelectedIndex());
          SearchOptions.ValueType mode = os.getData();
          switch (mode) {
            case Number:
              clValueInput.show(pValueInput, VALUE_TYPE_NUMBER);
              cbValueType.setToolTipText("Matches against any kind of numeric fields.");
              break;
            case Resource:
              clValueInput.show(pValueInput, VALUE_TYPE_RESOURCE);
              cbValueType.setToolTipText("Matches against resource fields.");
              break;
            case Bitfield:
              clValueInput.show(pValueInput, VALUE_TYPE_BITFIELD);
              cbValueType.setToolTipText("Matches against flags fields.");
              break;
            default:
              clValueInput.show(pValueInput, VALUE_TYPE_TEXT);
              cbValueType.setToolTipText("Matches against all field types (except Unknown).");
          }
        }
      } else if (event.getSource() == cbValueNumberRange) {
        // enable/disable value number range
        ftfValueInputMax.setEnabled(cbValueNumberRange.isSelected());
      } else if (event.getSource() == cbValueResourceType) {
        // re-initialize resource selection list based on selected resource type
        if (cbValueResourceType.getSelectedIndex() >= 0) {
          // do in background to keep dialog responsive
          (new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception
            {
              Cursor cursor = FilterInput.this.getCursor();
              WindowBlocker blocker = new WindowBlocker(FilterInput.this);
              blocker.setBlocked(true);
              try {
                FilterInput.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                if (cbValueResourceInput.getModel().getSize() > 0)
                  cbValueResourceInput.setSelectedIndex(0);
                String ext = (String)cbValueResourceType.getSelectedItem();
                if (ext == null || ext.isEmpty()) {
                  cbValueResourceInput.setModel(new ResourceListModel());
                } else {
                  cbValueResourceInput.setModel(new ResourceListModel(ext));
                }
                cbValueResourceInput.requestFocusInWindow();
              } finally {
                FilterInput.this.setCursor(cursor);
                blocker.setBlocked(false);
                // signal a waiting thread that the operation is complete
                signalCountdown(SIGNAL_RESOURCE_INPUT);
              }
              return null;
            }
          }).execute();
        }
      } else if (event.getSource() == bInputReset) {
        // reset all input to default values
        if (JOptionPane.showConfirmDialog(FilterInput.this, "Revert all settings to default values?", "Confirm reset",
                                          JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
          exportOptions(null);
        }
      } else if (event.getSource() == bInputApply) {
        if (advancedSearch != null) {
          advancedSearch.addFilter(getOptions());
          setVisible(false);
          advancedSearch.toFront();
        } else {
          JOptionPane.showMessageDialog(FilterInput.this, "Could not add or update filter.", "Add filter",
                                        JOptionPane.ERROR_MESSAGE);
          setVisible(false);
        }
      } else if (event.getSource() == bInputClose) {
        // remove window from screen
        setVisible(false);
      } else if (event.getSource() instanceof JMenuItem) {
        // toggle between decimal/hexadecimal display in formatted text fields
        JMenuItem mi = (JMenuItem)event.getSource();
        JPopupMenu menu = (mi.getParent() instanceof JPopupMenu) ? (JPopupMenu)mi.getParent() : null;
        JFormattedTextField ftf = menuToTextFieldMap.get(menu);
        if (menu != null && ftf != null) {
          List<Component> list = Arrays.asList(menu.getComponents()).stream().filter(c -> c instanceof JMenuItem).collect(Collectors.toList());
          switch (list.indexOf(mi)) {
            case 0: // dec
              ((NumberFormatterEx)ftf.getFormatter()).setNumberFormat(NumberFormatterEx.NumberFormat.decimal);
              break;
            case 1: // hex
              ((NumberFormatterEx)ftf.getFormatter()).setNumberFormat(NumberFormatterEx.NumberFormat.hexadecimal);
              break;
          }
        }
      }
    }

    // --------------------- Interface TreeWillExpandListener ---------------------

    @Override
    public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException
    {
    }

    @Override
    public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException
    {
      if (event.getSource() == treeStructure) {
        // nodes should not collapse
        throw new ExpandVetoException(event);
      }
    }

    // --------------------- Interface TreeSelectionListener ---------------------

    @Override
    public void valueChanged(TreeSelectionEvent event)
    {
      if (event.getSource() == treeStructure) {
        // Do not touch root!
        boolean isRoot = treeStructure.getModel().getRoot().equals(event.getPath().getLastPathComponent());
        treeStructure.setEditable(!isRoot);
        bStructureEdit.setEnabled(!isRoot);
        bStructureRemove.setEnabled(!isRoot);
      }
    }

    // --------------------- Interface MouseListener ---------------------

    @Override
    public void mouseClicked(MouseEvent event)
    {
      if (event.getSource() instanceof JTextField) {
        JTextField edit = (JTextField)event.getSource();
        if (event.getClickCount() == 2) {
          // Invoke later to circumvent content validation (may not work correctly on every platform)
          SwingUtilities.invokeLater(() -> edit.selectAll());
        } else if (!event.isPopupTrigger()) {
          edit.setCaretPosition(edit.viewToModel(event.getPoint()));
          //SwingUtilities.invokeLater(() -> edit.setCaretPosition(edit.viewToModel(event.getPoint())));
        }
      }
    }

    @Override
    public void mousePressed(MouseEvent event)
    {
      if (event.getSource() instanceof JFormattedTextField) {
        if (event.isPopupTrigger()) {
          // find popup menu associated with formatted text field and display it
          for (final Map.Entry<JPopupMenu, JFormattedTextField> entry : menuToTextFieldMap.entrySet()) {
            final JPopupMenu menu = entry.getKey();
            final JFormattedTextField ftf = entry.getValue();
            if (ftf == event.getSource()) {
              if (!menu.isVisible())
                menu.show(ftf, event.getX(), event.getY());
              break;
            }
          }
        }
      }
    }

    @Override
    public void mouseReleased(MouseEvent event)
    {
      if (event.getSource() instanceof JFormattedTextField) {
        if (event.isPopupTrigger()) {
          // find popup menu associated with formatted text field and display it
          for (final Map.Entry<JPopupMenu, JFormattedTextField> entry : menuToTextFieldMap.entrySet()) {
            final JPopupMenu menu = entry.getKey();
            final JFormattedTextField ftf = entry.getValue();
            if (ftf == event.getSource()) {
              if (!menu.isVisible())
                menu.show(ftf, event.getX(), event.getY());
              break;
            }
          }
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

    // --------------------- Interface KeyListener ---------------------

    @Override
    public void keyTyped(KeyEvent event)
    {
    }

    @Override
    public void keyPressed(KeyEvent event)
    {
      if (event.getSource() == treeStructure) {
        // remove selected element from structure tree
        removeSelectedTreeNode(treeStructure, true);
      } else if (event.getSource() instanceof JFormattedTextField) {
        // increment/decrement numeric content by constant amount
        JFormattedTextField ftf = (JFormattedTextField)event.getSource();
        NumberFormatterEx formatter = (NumberFormatterEx)ftf.getFormatter();
        NumberFormatterEx.NumberFormat fmt = formatter.getNumberFormat();
        long value = formatter.getNumericValue();
        long inc = (event.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) == 0 ? 1L : 10L;
        boolean updated = false;
        switch (event.getKeyCode()) {
          case KeyEvent.VK_UP:
          case KeyEvent.VK_KP_UP:
            value += inc;
            updated = true;
            break;
          case KeyEvent.VK_DOWN:
          case KeyEvent.VK_KP_DOWN:
            value -= inc;
            updated = true;
            break;
        }
        if (updated) {
          switch (fmt) {
            case decimal:
              ftf.setValue(Long.toString(value));
              break;
            case hexadecimal:
              ftf.setValue(Long.toString(value, 16) + "h");
              break;
          }
        }
      }
    }

    @Override
    public void keyReleased(KeyEvent event)
    {
    }

    // --------------------- Interface PopupMenuListener ---------------------

    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent event)
    {
      if (event.getSource() instanceof JPopupMenu) {
        // update menu item states based on number format of associated formatted text field
        JPopupMenu menu = (JPopupMenu)event.getSource();
        JFormattedTextField ftf = menuToTextFieldMap.get(menu);
        if (ftf != null) {
          NumberFormatterEx.NumberFormat fmt = ((NumberFormatterEx)ftf.getFormatter()).getNumberFormat();
          List<Component> list = Arrays.asList(menu.getComponents()).stream().filter(c -> c instanceof JMenuItem).collect(Collectors.toList());
          if (list.size() >= 2) {
            list.get(0).setEnabled(fmt != NumberFormatterEx.NumberFormat.decimal);
            list.get(1).setEnabled(fmt != NumberFormatterEx.NumberFormat.hexadecimal);
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
