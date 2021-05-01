// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;

import org.infinity.NearInfinity;
import org.infinity.datatype.Datatype;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Editable;
import org.infinity.datatype.EffectType;
import org.infinity.datatype.Flag;
import org.infinity.datatype.HexNumber;
import org.infinity.datatype.InlineEditable;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.IsReference;
import org.infinity.datatype.IsTextual;
import org.infinity.datatype.Readable;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.datatype.TextBitmap;
import org.infinity.datatype.TextString;
import org.infinity.datatype.Unknown;
import org.infinity.datatype.UnknownBinary;
import org.infinity.datatype.UnknownDecimal;
import org.infinity.gui.BrowserMenuBar.ViewMode;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.Closeable;
import org.infinity.resource.HasChildStructs;
import org.infinity.resource.HasViewerTabs;
import org.infinity.resource.Profile;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.Viewable;
import org.infinity.resource.dlg.AbstractCode;
import org.infinity.resource.dlg.DlgResource;
import org.infinity.resource.dlg.State;
import org.infinity.resource.dlg.Transition;
import org.infinity.resource.dlg.TreeItemEntry;
import org.infinity.search.AttributeSearcher;
import org.infinity.search.DialogItemRefSearcher;
import org.infinity.search.DialogStateReferenceSearcher;
import org.infinity.search.advanced.AdvancedSearch;
import org.infinity.search.advanced.SearchOptions;
import org.infinity.util.Misc;
import org.infinity.util.StructClipboard;
import org.infinity.util.io.ByteBufferOutputStream;
import org.infinity.util.io.StreamUtils;
import org.infinity.util.tuples.Couple;

public final class StructViewer extends JPanel implements ListSelectionListener, ActionListener,
                                                          ItemListener, ChangeListener, TableModelListener,
                                                          ComponentListener
{
  // Commonly used tab names
  public static final String TAB_EDIT           = "Edit";
  public static final String TAB_VIEW           = "View";
  public static final String TAB_RAW            = "Raw";

  // Menu commands
  public static final String CMD_COPYVALUE      = "VCopy";
  public static final String CMD_PASTEVALUE     = "VPaste";
  public static final String CMD_CUT            = "Cut";
  public static final String CMD_COPY           = "Copy";
  public static final String CMD_PASTE          = "Paste";
  public static final String CMD_TOHEX          = "ToHex";
  public static final String CMD_TOSTRING       = "ToStr";
  public static final String CMD_TOBIN          = "ToBin";
  public static final String CMD_TODEC          = "ToDec";
  public static final String CMD_TOINT          = "ToInt";
  public static final String CMD_TOHEXINT       = "ToHexInt";
  public static final String CMD_TOFLAGS        = "ToFlags";
  public static final String CMD_TORESLIST      = "ToResList";
  public static final String CMD_RESET          = "ResetType";
  public static final String CMD_GOTO_OFFSET    = "GotoOffset";
  public static final String CMD_ADD_ADV_SEARCH = "AddAdvSearch";
  public static final String CMD_SHOW_IN_TREE   = "ShowInTree";
  public static final String CMD_SHOWVIEWER     = "ShowView";
  public static final String CMD_SHOWNEWVIEWER  = "ShowNewView";
  public static final String UPDATE_VALUE       = "UpdateValue";

  // Identifiers for card layout elements
  private static final String CARD_EMPTY        = "Empty";
  /** Identifier of the container for {@link Editable} field of the {@code struct}. */
  private static final String CARD_EDIT         = "Edit";
  private static final String CARD_TEXT         = "Text";


  private static Class<? extends StructEntry> lastNameStruct, lastIndexStruct;
  private static String lastName;
  private static int lastIndex;
  private final AbstractStruct struct;
  private final Map<Class<? extends StructEntry>, Color> fieldColors = new HashMap<>();
  private final CardLayout cards = new CardLayout();
  private final JMenuItem miCopyValue = createMenuItem(CMD_COPYVALUE, "Copy value", Icons.getIcon(Icons.ICON_COPY_16), this);
  private final JMenuItem miPasteValue = createMenuItem(CMD_PASTEVALUE, "Replace value", Icons.getIcon(Icons.ICON_PASTE_16), this);
  private final JMenuItem miCut = createMenuItem(CMD_CUT, "Cut", Icons.getIcon(Icons.ICON_CUT_16), this);
  private final JMenuItem miCopy = createMenuItem(CMD_COPY, "Copy", Icons.getIcon(Icons.ICON_COPY_16), this);
  private final JMenuItem miPaste = createMenuItem(CMD_PASTE, "Paste", Icons.getIcon(Icons.ICON_PASTE_16), this);
  private final JMenuItem miToHex = createMenuItem(CMD_TOHEX, "Edit as hex", Icons.getIcon(Icons.ICON_REFRESH_16), this);
  private final JMenuItem miToString = createMenuItem(CMD_TOSTRING, "Edit as string", Icons.getIcon(Icons.ICON_REFRESH_16), this);
  private final JMenuItem miToBin = createMenuItem(CMD_TOBIN, "Edit as binary", Icons.getIcon(Icons.ICON_REFRESH_16), this);
  private final JMenuItem miToDec = createMenuItem(CMD_TODEC, "Edit as decimal", Icons.getIcon(Icons.ICON_REFRESH_16), this);
  private final JMenuItem miToInt = createMenuItem(CMD_TOINT, "Edit as number", Icons.getIcon(Icons.ICON_REFRESH_16), this);
  private final JMenuItem miToHexInt = createMenuItem(CMD_TOHEXINT, "Edit as hex number", Icons.getIcon(Icons.ICON_REFRESH_16), this);
  private final JMenuItem miToFlags = createMenuItem(CMD_TOFLAGS, "Edit as bit field", Icons.getIcon(Icons.ICON_REFRESH_16), this);
  private final JMenuItem miReset = createMenuItem(CMD_RESET, "Reset field type", Icons.getIcon(Icons.ICON_REFRESH_16), this);
  private final JMenuItem miAddToAdvSearch = createMenuItem(CMD_ADD_ADV_SEARCH, "Add to Advanced Search", Icons.getIcon(Icons.ICON_FIND_16), this);
  private final JMenuItem miGotoOffset = createMenuItem(CMD_GOTO_OFFSET, "Go to offset", null, this);
  private final JMenuItem miShowInTree = createMenuItem(CMD_SHOW_IN_TREE, "Show in tree", Icons.getIcon(Icons.ICON_SELECT_IN_TREE_16), this);
  private final JMenuItem miShowViewer = createMenuItem(CMD_SHOWVIEWER, "Show in viewer", Icons.getIcon(Icons.ICON_ROW_INSERT_AFTER_16), this);
  private final JMenuItem miShowNewViewer = createMenuItem(CMD_COPYVALUE, "Show in new viewer", null, this);
  private final JMenu miToResref = createResrefMenu(CMD_TORESLIST, "Edit as resref", Profile.getAvailableResourceTypes(),
                                                      Icons.getIcon(Icons.ICON_REFRESH_16), this);
  private final JPanel lowerpanel = new JPanel(cards);
  private final JPanel editpanel = new JPanel();
  private final ButtonPanel buttonPanel = new ButtonPanel();
  private final JPopupMenu popupmenu = new JPopupMenu();
  private final InfinityTextArea tatext = new InfinityTextArea(true);
  private final HashMap<Integer, StructEntry> entryMap = new HashMap<>();
  private final HashMap<Viewable, ViewFrame> viewMap = new HashMap<>();
  private final StructTable table;
  private JMenuItem miFindAttribute, miFindReferences, miFindStateReferences, miFindRefToItem;
  private Editable editable;
  private JTabbedPane tabbedPane;
  private JSplitPane splitv;
  private boolean splitterSet;
  private int oldSplitterHeight;
  private Couple<Integer, Integer> storedSelection;

  private static JMenuItem createMenuItem(String cmd, String text, Icon icon, ActionListener l)
  {
    JMenuItem m = new JMenuItem(text, icon);
    m.setActionCommand(cmd);
    if (l != null)
      m.addActionListener(l);
    return m;
  }

  private static JMenu createResrefMenu(String cmd, String text, String[] types, Icon icon, ActionListener l)
  {
    JMenu menu = new JMenu(text);
    if (icon != null) {
      menu.setIcon(icon);
    }

    if (types == null) {
      types = Profile.getAvailableResourceTypes();
    }
    for (final String type: types) {
      JMenuItem m = new JMenuItem(type);
      m.setActionCommand(cmd);
      if (l != null) {
        m.addActionListener(l);
      }
      menu.add(m);
    }

    return menu;
  }

  public StructViewer(AbstractStruct struct)
  {
    this.struct = struct;
    this.struct.addTableModelListener(this);
    this.table = new StructTable(this.struct);
    table.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    table.getSelectionModel().addListSelectionListener(this);
    table.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getScriptFont()));
    table.setRowHeight(table.getFontMetrics(table.getFont()).getHeight() + 1);
    table.addMouseListener(new MouseAdapter()
    {
      @Override
      public void mouseClicked(MouseEvent e)
      {
        if (e.getClickCount() == 2 && table.getSelectedRowCount() == 1) {
          Object selected = table.getModel().getValueAt(table.getSelectedRow(), 1);
          if (selected instanceof Viewable) {
            createViewFrame(table.getTopLevelAncestor(), (Viewable)selected);
          }
          else if (selected instanceof SectionOffset) {
            selectFirstEntryOfType(((SectionOffset)selected).getSection());
          }
          else if (selected instanceof SectionCount) {
            selectFirstEntryOfType(((SectionCount)selected).getSection());
          }
        }
      }
    });
    table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
    {
      @Override
      public Component getTableCellRendererComponent(JTable table, Object value,
                                                     boolean isSelected, boolean hasFocus, int row,
                                                     int column)
      {
        final StructEntry field = (StructEntry)table.getModel().getValueAt(row, 1);
        Class<? extends StructEntry> cls = null;
        if (BrowserMenuBar.getInstance().getColoredOffsetsEnabled()) {
          if (field instanceof SectionOffset)
            cls = ((SectionOffset)field).getSection();
          else if (field instanceof SectionCount)
            cls = ((SectionCount)field).getSection();
          else if (field instanceof AbstractStruct)
            cls = field.getClass();
          else if (fieldColors.containsKey(field.getClass())) // consider only referenced simple field types
            cls = field.getClass();
        }
        setBackground(getClassColor(cls));

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (column >= 2)
          setHorizontalAlignment(JLabel.TRAILING);
        else
          setHorizontalAlignment(JLabel.LEADING);
        return this;
      }
    });
    table.setDefaultEditor(Object.class, new StructCellEditor());

    popupmenu.add(miCopyValue);
    popupmenu.add(miPasteValue);
    popupmenu.add(miCut);
    popupmenu.add(miCopy);
    popupmenu.add(miPaste);
    popupmenu.addSeparator();
    popupmenu.add(miToHex);
    popupmenu.add(miToBin);
    popupmenu.add(miToDec);
    popupmenu.add(miToInt);
    popupmenu.add(miToHexInt);
    popupmenu.add(miToFlags);
    popupmenu.add(miToResref);
    popupmenu.add(miToString);
    popupmenu.add(miReset);
    popupmenu.addSeparator();
    popupmenu.add(miAddToAdvSearch);
    popupmenu.add(miGotoOffset);
    if (struct instanceof DlgResource) {
      popupmenu.add(miShowInTree);
      popupmenu.add(miShowViewer);
      popupmenu.add(miShowNewViewer);
    }
    table.addMouseListener(new PopupListener());
    miCopyValue.setEnabled(false);
    miPasteValue.setEnabled(false);
    miCut.setEnabled(false);
    miCopy.setEnabled(false);
    miPaste.setEnabled(
            StructClipboard.getInstance().getContentType(struct) == StructClipboard.CLIPBOARD_ENTRIES);
    miToHex.setEnabled(false);
    miToBin.setEnabled(false);
    miToDec.setEnabled(false);
    miToInt.setEnabled(false);
    miToHexInt.setEnabled(false);
    miToFlags.setEnabled(false);
    miToResref.setEnabled(false);
    miToString.setEnabled(false);
    miReset.setEnabled(false);
    miAddToAdvSearch.setEnabled(false);
    miGotoOffset.setEnabled(false);
    miShowInTree.setEnabled(false);
    miShowViewer.setEnabled(false);
    miShowNewViewer.setEnabled(false);

    tatext.setHighlightCurrentLine(false);
    tatext.setEOLMarkersVisible(false);
    tatext.setEditable(false);
    tatext.setMargin(new Insets(3, 3, 3, 3));
    tatext.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getScriptFont()));
    InfinityScrollPane scroll = new InfinityScrollPane(tatext, true);
    scroll.setLineNumbersEnabled(false);
    table.setModel(struct);
    table.getTableHeader().setReorderingAllowed(false);
    table.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
    table.addComponentListener(this);
    for (int column = 0, colCount = table.getColumnModel().getColumnCount(); column < colCount; column++) {
      switch (table.getColumnName(column)) {
        case AbstractStruct.COLUMN_ATTRIBUTE:
          table.getColumnModel().getColumn(column).setPreferredWidth(NearInfinity.getInstance().getTableColumnWidth(0));
          break;
        case AbstractStruct.COLUMN_VALUE:
          table.getColumnModel().getColumn(column).setPreferredWidth(NearInfinity.getInstance().getTableColumnWidth(1));
          break;
        case AbstractStruct.COLUMN_OFFSET:
          table.getColumnModel().getColumn(column).setPreferredWidth(NearInfinity.getInstance().getTableColumnWidth(2));
          break;
        case AbstractStruct.COLUMN_SIZE:
          table.getColumnModel().getColumn(column).setPreferredWidth(NearInfinity.getInstance().getTableColumnWidth(3));
          break;
      }
    }

    lowerpanel.add(scroll, CARD_TEXT);
    lowerpanel.add(editpanel, CARD_EDIT);
    lowerpanel.add(new JPanel(), CARD_EMPTY);
    lowerpanel.addComponentListener(this);
    cards.show(lowerpanel, CARD_EMPTY);

    if (struct instanceof HasChildStructs && !struct.getFields().isEmpty()) {
      try {
        final AddRemovable[] prototypes = ((HasChildStructs)struct).getPrototypes();
        if (prototypes.length > 0) {
          final JMenuItem menuItems[] = new JMenuItem[prototypes.length];
          for (int i = 0; i < prototypes.length; i++) {
            final AddRemovable proto = prototypes[i];
            final JMenuItem menu = new JMenuItem(proto.getName());
            menu.putClientProperty("prototype", proto);
            menuItems[i] = menu;
          }
          ButtonPopupMenu bpmAdd = (ButtonPopupMenu)buttonPanel.addControl(ButtonPanel.Control.ADD);
          bpmAdd.setMenuItems(menuItems);
          bpmAdd.addItemListener(this);
          JButton bRemove = (JButton)buttonPanel.addControl(ButtonPanel.Control.REMOVE);
          bRemove.setEnabled(false);
          bRemove.addActionListener(this);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    ButtonPopupMenu bpmFind = (ButtonPopupMenu)buttonPanel.addControl(ButtonPanel.Control.FIND_MENU);
    bpmFind.addItemListener(this);
    if (struct instanceof DlgResource) {
      miFindAttribute = new JMenuItem("selected attribute");
      miFindAttribute.setEnabled(false);
      miFindReferences = new JMenuItem("references to this file");
      miFindReferences.setEnabled(struct instanceof Resource && struct.getParent() == null);
      miFindStateReferences = new JMenuItem("references to this state");
      miFindStateReferences.setEnabled(false);
      miFindRefToItem = new JMenuItem("references to selected item in this file");
      miFindRefToItem.setEnabled(false);

      bpmFind.setMenuItems(new JMenuItem[]{miFindAttribute, miFindReferences,
                                           miFindStateReferences, miFindRefToItem});
    } else {
      miFindAttribute = new JMenuItem("selected attribute");
      miFindAttribute.setEnabled(false);
      miFindReferences = new JMenuItem("references to this file");
      miFindReferences.setEnabled(struct instanceof Resource && struct.getParent() == null);
      bpmFind.setMenuItems(new JMenuItem[]{miFindAttribute, miFindReferences});
    }
    JButton bView = (JButton)buttonPanel.addControl(ButtonPanel.Control.VIEW_EDIT);
    bView.setEnabled(false);
    bView.addActionListener(this);
    ((JButton)buttonPanel.addControl(ButtonPanel.Control.PRINT)).addActionListener(this);
    if (struct instanceof Resource && !struct.getFields().isEmpty() && struct.getParent() == null) {
      ((JButton)buttonPanel.addControl(ButtonPanel.Control.EXPORT_BUTTON)).addActionListener(this);
      ((JButton)buttonPanel.addControl(ButtonPanel.Control.SAVE)).addActionListener(this);
    }

    JScrollPane scrollTable = new JScrollPane(table);
    scrollTable.getViewport().setBackground(table.getBackground());
    scrollTable.setBorder(BorderFactory.createEmptyBorder());
    splitv = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollTable, lowerpanel);
    splitv.setDividerLocation(2 * (NearInfinity.getInstance().getHeight() - 100) / 3);

    setLayout(new BorderLayout());
    if (struct instanceof HasViewerTabs) {
      HasViewerTabs tabs = (HasViewerTabs)struct;
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(splitv, BorderLayout.CENTER);
      panel.add(buttonPanel, BorderLayout.SOUTH);
      tabbedPane = new JTabbedPane();

      // adding custom tabs
      int editIndex = -1;
      for (int i = 0; i < tabs.getViewerTabCount(); i++) {
        if (tabs.viewerTabAddedBefore(i)) {
          // adding before "Edit"
          if (editIndex < 0) {
            tabbedPane.addTab(tabs.getViewerTabName(i), tabs.getViewerTab(i));
          } else {
            tabbedPane.insertTab(tabs.getViewerTabName(i), null, tabs.getViewerTab(i), null, editIndex);
            editIndex++;
          }
        } else {
          // adding after "Edit"
          if (editIndex < 0) {
            tabbedPane.addTab(TAB_EDIT, panel);
            editIndex = tabbedPane.getTabCount() - 1;
          }
          tabbedPane.addTab(tabs.getViewerTabName(i), tabs.getViewerTab(i));
        }
      }

      // add "Edit" tab if not yet added
      if (editIndex < 0) {
        tabbedPane.addTab(TAB_EDIT, panel);
        editIndex = tabbedPane.getTabCount() - 1;
      }

      add(tabbedPane, BorderLayout.CENTER);
      if (struct.getParent() instanceof HasViewerTabs) {
        StructViewer sViewer = struct.getParent().getViewer();
        if (sViewer == null) {
          sViewer = struct.getParent().getParent().getViewer();
        }
        if (sViewer != null && sViewer.tabbedPane != null) {
          // make sure tab index is within bounds
          int idx = Math.max(Math.min(sViewer.tabbedPane.getSelectedIndex(), tabbedPane.getTabCount() - 1), 0);
          tabbedPane.setSelectedIndex(idx);
        }
      } else if (lastIndexStruct == struct.getClass()) {
        tabbedPane.setSelectedIndex(lastIndex);
      } else if (BrowserMenuBar.getInstance().getDefaultStructView() == ViewMode.Edit) {
        tabbedPane.setSelectedIndex(getEditTabIndex());
      }
      if (isEditTabSelected()) {
        if (lastNameStruct == struct.getClass()) {
          selectEntry(lastName);
        } else {
          table.getSelectionModel().setSelectionInterval(0, 0);
        }
      }
    } else {
      add(splitv, BorderLayout.CENTER);
      add(buttonPanel, BorderLayout.SOUTH);
      if (lastNameStruct == struct.getClass()) {
        selectEntry(lastName);
      } else {
        table.getSelectionModel().setSelectionInterval(0, 0);
      }
    }

    addComponentListener(this);

    StructClipboard.getInstance().addChangeListener(this);
    table.repaint();
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    final String cmd = event.getActionCommand();
    final ListSelectionModel lsm = table.getSelectionModel();

    final int min = lsm.getMinSelectionIndex();
    final int max = lsm.getMaxSelectionIndex();

    if (event.getSource() instanceof JComponent &&
        buttonPanel.getControlPosition((JComponent)event.getSource()) >= 0) {
      if (buttonPanel.getControlByType(ButtonPanel.Control.VIEW_EDIT) == event.getSource()) {
        Viewable selected = (Viewable)table.getModel().getValueAt(min, 1);
        createViewFrame(getTopLevelAncestor(), selected);
      } else if (buttonPanel.getControlByType(ButtonPanel.Control.REMOVE) == event.getSource()) {
        if (!(struct instanceof HasChildStructs)) {
          return;
        }
        Window wnd = SwingUtilities.getWindowAncestor(this);
        if (wnd == null) {
          wnd = NearInfinity.getInstance();
        }
        WindowBlocker.blockWindow(wnd, true);
        try {
          int[] rows = table.getSelectedRows();
          for (int i = rows.length - 1; i >= 0; i--) {
            Object entry = table.getModel().getValueAt(rows[i], 1);
            if (entry instanceof AddRemovable) {
              try {
                struct.removeDatatype((AddRemovable)entry, true);
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
          }
        } finally {
          WindowBlocker.blockWindow(wnd, false);
        }
      } else if (buttonPanel.getControlByType(ButtonPanel.Control.SAVE) == event.getSource()) {
        if (ResourceFactory.saveResource((Resource)struct, getTopLevelAncestor())) {
          struct.setStructChanged(false);
        }
      } else if (buttonPanel.getControlByType(ButtonPanel.Control.EXPORT_BUTTON) == event.getSource()) {
        ResourceFactory.exportResource(struct.getResourceEntry(), getTopLevelAncestor());
      } else if (buttonPanel.getControlByType(ButtonPanel.Control.PRINT) == event.getSource()) {
        PrinterJob pj = PrinterJob.getPrinterJob();
        pj.setPrintable(table);
        if (pj.printDialog()) {
          try {
            pj.print();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    } else if (UPDATE_VALUE.equals(cmd)) {
      if (editable != null && editable.updateValue(struct)) {
        struct.setStructChanged(true);
        final int index = struct.getFields().indexOf(editable);
        struct.fireTableRowsUpdated(index, index);
        if (editable instanceof EffectType) {
          // don't lose current selection
          if (struct.getViewer() != null) {
            struct.getViewer().storeCurrentSelection();
          }
          // Updates multiple lines - could be done better?
          struct.fireTableDataChanged();
          if (struct.getViewer() != null) {
            struct.getViewer().restoreCurrentSelection();
          }
        }
      }
      else
        JOptionPane.showMessageDialog(this, "Error updating value", "Error", JOptionPane.ERROR_MESSAGE);
    } else if (CMD_COPYVALUE.equals(cmd)) {
      StructClipboard.getInstance().copyValue(struct, min, max);
    } else if (CMD_PASTEVALUE.equals(cmd)) {
      final int changed = StructClipboard.getInstance().pasteValue(struct, min);
      if (changed == 0)
        JOptionPane.showMessageDialog(this, "Attribute doesn't match!", "Error", JOptionPane.ERROR_MESSAGE);
      else {
        struct.fireTableRowsUpdated(min, min + changed);
        struct.setStructChanged(true);
      }
    } else if (CMD_CUT.equals(cmd)) {
      lsm.removeIndexInterval(min, max);
      table.clearSelection();
      StructClipboard.getInstance().cut(struct, min, max);
    } else if (CMD_COPY.equals(cmd)) {
      StructClipboard.getInstance().copy(struct, min, max);
    } else if (CMD_PASTE.equals(cmd)) {
      int row = StructClipboard.getInstance().paste(struct);
      if (row >= 0) {
        lsm.setSelectionInterval(row, row);
        table.scrollRectToVisible(table.getCellRect(row, 1, true));
      }
    } else if (CMD_TOHEX.equals(cmd)) {
      convertAttribute(min, miToHex);
    } else if (CMD_TOBIN.equals(cmd)) {
      convertAttribute(min, miToBin);
    } else if (CMD_TODEC.equals(cmd)) {
      convertAttribute(min, miToDec);
    } else if (CMD_TOINT.equals(cmd)) {
      convertAttribute(min, miToInt);
    } else if (CMD_TOHEXINT.equals(cmd)) {
      convertAttribute(min, miToHexInt);
    } else if (CMD_TOFLAGS.equals(cmd)) {
      convertAttribute(min, miToFlags);
    } else if (CMD_TORESLIST.equals(cmd)) {
      convertAttribute(min, (JMenuItem)event.getSource());
    } else if (CMD_TOSTRING.equals(cmd)) {
      convertAttribute(min, miToString);
    } else if (CMD_RESET.equals(cmd)) {
      convertAttribute(min, miReset);
    } else if (CMD_GOTO_OFFSET.equals(cmd)) {
      final StructEntry se = (StructEntry)table.getValueAt(min, 1);
      Class<? extends StructEntry> cls = null;
      if (se instanceof SectionOffset)
        cls = ((SectionOffset)se).getSection();
      else if (se instanceof SectionCount)
        cls = ((SectionCount)se).getSection();
      if (cls != null)
        selectFirstEntryOfType(cls);
    } else if (CMD_ADD_ADV_SEARCH.equals(cmd)) {
      addToAdvancedSearch((StructEntry)table.getValueAt(min, 1));
    } else if (CMD_SHOW_IN_TREE.equals(cmd)) {
      // this should only be available for DlgResources
      final DlgResource dlgRes = (DlgResource) struct;
      dlgRes.selectInTree((TreeItemEntry)table.getValueAt(min, 1));
    } else if (CMD_SHOWVIEWER.equals(cmd)) {
      // this should only be available for DlgResources
      final DlgResource dlgRes = (DlgResource) struct;
      dlgRes.selectInEdit((StructEntry)table.getValueAt(min, 1));
    } else if (CMD_SHOWNEWVIEWER.equals(cmd)) {
      // get a copy of the resource first
      DlgResource dlgRes = (DlgResource) ResourceFactory.getResource(struct.getResourceEntry());
      createViewFrame(getTopLevelAncestor(), dlgRes);
      dlgRes.selectInEdit((StructEntry)table.getValueAt(min, 1));
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event)
  {
    considerMenuEnabled();
  }

// --------------------- End Interface ChangeListener ---------------------


// --------------------- Begin Interface ItemListener ---------------------

  @Override
  public void itemStateChanged(ItemEvent event)
  {
    final Object src = event.getSource();
    if (src == buttonPanel.getControlByType(ButtonPanel.Control.ADD) && struct instanceof HasChildStructs) {
      final JMenuItem item = ((ButtonPopupMenu)src).getSelectedItem();
      AddRemovable toadd = (AddRemovable)item.getClientProperty("prototype");
      try {
        toadd = ((HasChildStructs)struct).confirmAddEntry(toadd);
        if (toadd != null) {
          toadd = (AddRemovable)toadd.clone();
        }
        int index = struct.addDatatype(toadd);
        table.getSelectionModel().setSelectionInterval(index, index);
        table.scrollRectToVisible(table.getCellRect(index, 1, true));
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else
    if (src == buttonPanel.getControlByType(ButtonPanel.Control.FIND_MENU)) {
      final JMenuItem item = ((ButtonPopupMenu)src).getSelectedItem();
      if (item == miFindAttribute) {
        new AttributeSearcher(struct, (StructEntry)table.getValueAt(table.getSelectedRow(), 1),
                              getTopLevelAncestor());
      } else if (item == miFindReferences) {
        struct.searchReferences(getTopLevelAncestor());
      } else if (item == miFindStateReferences) {
        State state = (State)table.getValueAt(table.getSelectedRow(), 1);
        new DialogStateReferenceSearcher(struct.getResourceEntry(), state, getTopLevelAncestor());
      } else if (item == miFindRefToItem) {
        new DialogItemRefSearcher((DlgResource) struct, table.getValueAt(table.getSelectedRow(), 1),
                                  getTopLevelAncestor());
      }
    }
  }

// --------------------- End Interface ItemListener ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent event)
  {
    if (event.getValueIsAdjusting())
      return;
    considerMenuEnabled();
    ListSelectionModel lsm = (ListSelectionModel)event.getSource();
    if (lsm.isSelectionEmpty() || lsm.getMaxSelectionIndex() != lsm.getMinSelectionIndex()) {
      tatext.setText("");
      // allow removal of multiple AddRemovable entries
      boolean removeEnabled = !lsm.isSelectionEmpty();
      for (int cur = lsm.getMinSelectionIndex(), max = lsm.getMaxSelectionIndex(); cur <= max && removeEnabled; cur++) {
        removeEnabled = table.getModel().getValueAt(cur, 1) instanceof AddRemovable;
      }
      JButton bRemove = (JButton)buttonPanel.getControlByType(ButtonPanel.Control.REMOVE);
      if (bRemove != null) {
        bRemove.setEnabled(removeEnabled);
      }
      JButton bView = (JButton)buttonPanel.getControlByType(ButtonPanel.Control.VIEW_EDIT);
      if (bView != null) {
        bView.setEnabled(false);
      }
      cards.show(lowerpanel, CARD_EMPTY);
      miToHex.setEnabled(false);
      miToBin.setEnabled(false);
      miToDec.setEnabled(false);
      miToInt.setEnabled(false);
      miToHexInt.setEnabled(false);
      miToFlags.setEnabled(false);
      miToResref.setEnabled(false);
      miToString.setEnabled(false);
      miReset.setEnabled(false);
      miAddToAdvSearch.setEnabled(false);
      miGotoOffset.setEnabled(false);
      miShowInTree.setEnabled(false);
      miShowViewer.setEnabled(false);
      miShowNewViewer.setEnabled(false);
      if (miFindAttribute != null) {
        miFindAttribute.setEnabled(false);
      }
      if (miFindStateReferences != null) {
        miFindStateReferences.setEnabled(false);
      }
      if (miFindRefToItem != null) {
        miFindRefToItem.setEnabled(false);
      }
    }
    else {
      table.scrollRectToVisible(table.getCellRect(lsm.getMinSelectionIndex(), 0, true));
      Object selected = table.getModel().getValueAt(lsm.getMinSelectionIndex(), 1);
      String className = selected.getClass().getCanonicalName();
      if (className == null) {
        className = "";
      }
      miPaste.setEnabled(StructClipboard.getInstance().getContentType(struct) == StructClipboard.CLIPBOARD_ENTRIES);
      JButton bRemove = (JButton)buttonPanel.getControlByType(ButtonPanel.Control.REMOVE);
      if (bRemove != null) {
        bRemove.setEnabled(selected instanceof AddRemovable && ((AddRemovable)selected).canRemove());
      }
      JButton bView = (JButton)buttonPanel.getControlByType(ButtonPanel.Control.VIEW_EDIT);
      if (bView != null) {
        bView.setEnabled(selected instanceof Viewable);
      }
      if (miFindAttribute != null) {
        miFindAttribute.setEnabled(!(selected instanceof AbstractStruct));
      }
      if (miFindStateReferences != null) {
        miFindStateReferences.setEnabled(selected instanceof State);
      }
      final boolean isDataType = (selected instanceof Datatype);
      final boolean isReadable = (selected instanceof Readable);
      miToHex.setEnabled(isDataType && isReadable && !(selected instanceof Unknown ||
                                                       selected instanceof SectionCount ||
                                                       selected instanceof AbstractCode));
      if (!miToHex.isEnabled() &&
          (selected instanceof UnknownBinary || selected instanceof UnknownDecimal)) {
        miToHex.setEnabled(true);
      }
      miToBin.setEnabled(isDataType && isReadable &&
                         !(selected instanceof UnknownBinary ||
                           selected instanceof SectionCount ||
                           selected instanceof AbstractCode));
      miToDec.setEnabled(isDataType && isReadable &&
                         !(selected instanceof UnknownDecimal ||
                           selected instanceof SectionCount ||
                           selected instanceof AbstractCode));
      miToInt.setEnabled(isDataType && isReadable &&
                         ((Datatype)selected).getSize() <= 4 &&
                         !(selected instanceof SectionCount ||
                           selected instanceof SectionOffset ||
                           selected instanceof AbstractCode));
      miToHexInt.setEnabled(isDataType && isReadable &&
                            ((Datatype)selected).getSize() <= 4 &&
                            !(selected instanceof HexNumber ||
                              selected instanceof SectionCount ||
                              selected instanceof SectionOffset ||
                              selected instanceof AbstractCode));
      miToFlags.setEnabled(isDataType && isReadable &&
                           ((Datatype)selected).getSize() <= 4 &&
                           !(selected instanceof Flag ||
                             selected instanceof SectionCount ||
                             selected instanceof SectionOffset ||
                             selected instanceof AbstractCode));
      miToResref.setEnabled(isDataType && isReadable &&
                            selected instanceof IsTextual &&
                            ((Datatype)selected).getSize() == 8);
      miToString.setEnabled(isDataType && isReadable &&
                            (selected instanceof Unknown ||
                             selected instanceof ResourceRef ||
                             selected instanceof TextBitmap) &&
                            !(selected instanceof AbstractCode));
      miReset.setEnabled(isDataType && isReadable &&
                         getCachedStructEntry(((Datatype)selected).getOffset()) != null &&
                         !(selected instanceof AbstractCode));
      miAddToAdvSearch.setEnabled(!(selected instanceof AbstractStruct || selected instanceof Unknown));
      miGotoOffset.setEnabled(selected instanceof SectionOffset|| selected instanceof SectionCount);
      final boolean isSpecialDlgTreeItem = (selected instanceof State
                                         || selected instanceof Transition);
      final boolean isSpecialDlgStruct = isSpecialDlgTreeItem
                                      || selected instanceof AbstractCode;

      if (miFindRefToItem != null) {
        miFindRefToItem.setEnabled(isSpecialDlgStruct);
      }
      miShowInTree.setEnabled(isSpecialDlgTreeItem);
      miShowViewer.setEnabled(isSpecialDlgStruct);
      miShowNewViewer.setEnabled(isSpecialDlgStruct);

      if (selected instanceof Editable) {
        edit((Editable)selected);
      }
      else if (selected instanceof InlineEditable) {
        tatext.setText("");
        cards.show(lowerpanel, CARD_EMPTY);
      }
      else {
        editable = null;
        if (selected instanceof AbstractStruct)
          tatext.setText(((AbstractStruct)selected).toMultiLineString());
        else
          tatext.setText(selected.toString());
        tatext.setCaretPosition(0);
        cards.show(lowerpanel, CARD_TEXT);
      }
    }
  }

// --------------------- End Interface ListSelectionListener ---------------------


// --------------------- Begin Interface TableModelListener ---------------------

  @Override
  public void tableChanged(TableModelEvent event)
  {
    if (event.getType() == TableModelEvent.UPDATE) {
      final StructEntry structEntry = struct.getFields().get(event.getFirstRow());
      if (structEntry instanceof Editable && (editable == null || (structEntry.getOffset() == editable.getOffset() &&
                                                                   structEntry != editable))) {
        edit((Editable)structEntry);
      }
    }
  }

// --------------------- End Interface TableModelListener ---------------------

// --------------------- Begin Interface ComponentListener ---------------------

  @Override
  public void componentShown(ComponentEvent e)
  {
  }

  @Override
  public void componentResized(ComponentEvent e)
  {
    if (e.getSource() == this) {
      // ensure fixed lower panel height
      int loc = Math.max(50, splitv.getHeight() - NearInfinity.getInstance().getTablePanelHeight());
      splitv.setDividerLocation(loc);
      splitterSet = true;   // XXX: work-around to prevent storing uninitialized splitter location in prefs
    } else if (e.getSource() == lowerpanel) {
      if (oldSplitterHeight > 0 && splitv.getHeight() == oldSplitterHeight) {
        int v = Math.max(50, splitv.getHeight() - splitv.getDividerLocation());
        NearInfinity.getInstance().updateTablePanelHeight(v);
      } else {
        // don't update splitter location when resizing window
        oldSplitterHeight = splitv.getHeight();
      }
    } else if (e.getSource() == table) {
      // ensure fixed "Attribute" and "Offset" column widths
      int w = table.getWidth();
      int w0 = table.getColumnModel().getColumn(0).getPreferredWidth();
      int w2 = (table.getColumnCount() > 2) ? table.getColumnModel().getColumn(2).getPreferredWidth() : 0;
      int w3 = (table.getColumnCount() > 3) ? table.getColumnModel().getColumn(3).getPreferredWidth() : 0;
      int w1 = w - (w0 + w2 + w3);

      table.getColumnModel().getColumn(0).setPreferredWidth(w0);
      table.getColumnModel().getColumn(1).setPreferredWidth(w1);
      if (table.getColumnCount() > 2) {
        table.getColumnModel().getColumn(2).setPreferredWidth(w2);
        if (table.getColumnCount() > 3)
          table.getColumnModel().getColumn(3).setPreferredWidth(w3);
      }
    }
  }

  @Override
  public void componentMoved(ComponentEvent e)
  {
  }

  @Override
  public void componentHidden(ComponentEvent e)
  {
  }

// --------------------- End Interface ComponentListener ---------------------

  /**
   * Stores the currently selected list items internally.
   * Intended to be used with {@link #restoreCurrentSelection()}.
   */
  public void storeCurrentSelection()
  {
    if (storedSelection == null) {
      int[] selection = table.getSelectedRows();
      if (selection.length > 0) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (final int idx: selection) {
          min = Math.min(min, idx);
          max = Math.max(max, idx);
        }
        storedSelection = Couple.with(min, max);
      }
    }
  }

  /** Restores the selection state saved by the method {@link #storeCurrentSelection()}. */
  public void restoreCurrentSelection()
  {
    if (storedSelection != null) {
      table.setRowSelectionInterval(storedSelection.getValue0(), storedSelection.getValue1());
      storedSelection = null;
    }
  }

  public ButtonPanel getButtonPanel()
  {
    return buttonPanel;
  }

  public void close()
  {
    // storing current table column widths and divider location
    for (int col = 0, colCount = table.getColumnCount(); col < colCount; col++) {
      switch (table.getColumnName(col)) {
        case AbstractStruct.COLUMN_ATTRIBUTE:
          NearInfinity.getInstance().updateTableColumnWidth(0, table.getColumnModel().getColumn(col).getPreferredWidth());
          break;
        case AbstractStruct.COLUMN_VALUE:
          NearInfinity.getInstance().updateTableColumnWidth(1, table.getColumnModel().getColumn(col).getPreferredWidth());
          break;
        case AbstractStruct.COLUMN_OFFSET:
          NearInfinity.getInstance().updateTableColumnWidth(2, table.getColumnModel().getColumn(col).getPreferredWidth());
          break;
        case AbstractStruct.COLUMN_SIZE:
          NearInfinity.getInstance().updateTableColumnWidth(3, table.getColumnModel().getColumn(col).getPreferredWidth());
          break;
      }
    }
    if (splitterSet) {
      NearInfinity.getInstance().updateTablePanelHeight(splitv.getHeight() - splitv.getDividerLocation());
    }

    StructClipboard.getInstance().removeChangeListener(this);
    if (struct instanceof Resource) {
      if (tabbedPane != null && struct instanceof HasViewerTabs) {
        lastIndex = tabbedPane.getSelectedIndex();
        lastIndexStruct = struct.getClass();
      } else {
        lastIndexStruct = null;
      }
      if (table.getSelectionModel().getMinSelectionIndex() != -1) {
        lastName = ((StructEntry)table.getModel().getValueAt(table.getSelectionModel()
                                 .getMinSelectionIndex(), 1)).getName();
        lastNameStruct = struct.getClass();
      } else {
        lastName = null;
        lastNameStruct = null;
      }
    }

    if (tabbedPane != null) {
      for (int i = 0; i < tabbedPane.getTabCount(); i++) {
        Component c = tabbedPane.getComponentAt(i);
        if (c instanceof Closeable) {
          try {
            ((Closeable)c).close();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
      tabbedPane = null;
    }
  }

  public StructEntry getSelectedEntry()
  {
    if (table.getSelectedRow() == -1)
      return null;
    return (StructEntry)table.getModel().getValueAt(table.getSelectedRow(), 1);
  }

  public int getSelectedRow()
  {
    return table.getSelectedRow();
  }

  public void selectEntry(String name)
  {
    for (final StructEntry entry : struct.getFields()) {
      if (entry.getName().equals(name)) {
        selectEntry(entry);
        return;
      }
    }
  }

  public void selectEntry(int offset)
  {
    selectEntry(offset, true);
  }

  public void selectEntry(int offset, boolean recursive)
  {
    for (final StructEntry entry : struct.getFields()) {
      if (entry instanceof AbstractStruct && recursive) {
        selectEntry((AbstractStruct)entry, offset);
      } else if (entry.getOffset() == offset) {
        selectEntry(entry);
      }
    }
  }

  /** Helper method for finding out if a "View" tab is available. */
  public boolean hasViewTab()
  {
    return hasTab(TAB_VIEW);
  }

  /** Helper method for finding out if "View" tab is selected. */
  public boolean isViewTabSelected()
  {
    return isTabSelected(getTabIndex(TAB_VIEW));
  }

  /** Helper method for selecting "View" tab if available. */
  public void selectViewTab()
  {
    selectTab(getTabIndex(TAB_VIEW));
  }

  /** Returns whether "Edit" tab is selected. */
  public boolean isEditTabSelected()
  {
    return isTabSelected(getTabIndex(TAB_EDIT));
  }

  /** Selects the "Edit" tab. */
  public void selectEditTab()
  {
    selectTab(getTabIndex(TAB_EDIT));
  }

  /** Returns tab index of "Edit" tab. */
  public int getEditTabIndex()
  {
    return getTabIndex(TAB_EDIT);
  }

  /** Helper method for finding out if a "Raw" tab is available. */
  public boolean hasRawTab()
  {
    return hasTab(TAB_RAW);
  }

  /** Helper method for finding out if "Raw" tab is selected. */
  public boolean isRawTabSelected()
  {
    return isTabSelected(getTabIndex(TAB_RAW));
  }

  /** Helper method for selecting "Raw" tab if available. */
  public void selectRawTab()
  {
    selectTab(getTabIndex(TAB_RAW));
  }

  /** Returns whether the tab with the given name exists. */
  public boolean hasTab(String name)
  {
    return (getTabIndex(name) >= 0);
  }

  /** Returns whether the specified tab is currently selected. */
  public boolean isTabSelected(int index)
  {
    if (index >= 0 && index < tabbedPane.getTabCount()) {
      return (index == tabbedPane.getSelectedIndex());
    }
    return false;
  }

  /** Selects the specified tab if available. */
  public void selectTab(int index)
  {
    if (tabbedPane != null) {
      if (index >= 0 && index < tabbedPane.getTabCount()) {
        tabbedPane.setSelectedIndex(index);
      }
    }
  }

  /** Returns tab index of specified tab name.  */
  public int getTabIndex(String name)
  {
    if (tabbedPane != null && name != null) {
      for (int i = 0, count = tabbedPane.getTabCount(); i < count; i++) {
        if (name.equals(tabbedPane.getTitleAt(i))) {
          return i;
        }
      }
    }
    return -1;
  }

  /** Adds a ChangeListener to the TabbedPane if available. */
  public void addTabChangeListener(ChangeListener l)
  {
    if (tabbedPane != null && l != null) {
      tabbedPane.addChangeListener(l);
    }
  }

  /** Removes a ChangeListener from the TabbedPane if available. */
  public void removeTabChangeListener(ChangeListener l)
  {
    if (tabbedPane != null && l != null) {
      tabbedPane.removeChangeListener(l);
    }
  }

  /** Returns an array of ChangeListeners added to the TabbedPane if available. */
  public ChangeListener[] getTabChangeListeners()
  {
    if (tabbedPane != null) {
      return tabbedPane.getChangeListeners();
    } else {
      return new ChangeListener[0];
    }
  }

  /**
   * Returns an already existing ViewFrame of the given Viewable object if available.
   * Returns a new ViewFrame object otherwise. Assumes top level ancestor of the given view as parent.
   */
  public ViewFrame getViewFrame(Viewable view)
  {
    return getViewFrame(getTopLevelAncestor(), view);
  }

  /**
   * Returns an already existing ViewFrame of the given Viewable object if available.
   * Returns a new ViewFrame object otherwise.
   */
  public ViewFrame getViewFrame(Component parent, Viewable view)
  {
    return createViewFrame(parent, view);
  }

  private void considerMenuEnabled()
  {
    ListSelectionModel lsm = table.getSelectionModel();
    if (lsm.isSelectionEmpty()) {
      miCopyValue.setEnabled(false);
      miPasteValue.setEnabled(false);
      miCut.setEnabled(false);
      miCopy.setEnabled(false);
      miPaste.setEnabled(
              StructClipboard.getInstance().getContentType(struct) == StructClipboard.CLIPBOARD_ENTRIES);
    }
    else if (lsm.getMaxSelectionIndex() != lsm.getMinSelectionIndex()) {
      boolean isRemovable = true;
      boolean isValue = true;
      for (int i = lsm.getMinSelectionIndex(); i <= lsm.getMaxSelectionIndex(); i++) {
        Object o = table.getModel().getValueAt(i, 1);
        if (!(o instanceof AddRemovable))
          isRemovable = false;
        else
          isRemovable = ((AddRemovable)o).canRemove();
        if (o instanceof AbstractStruct)
          isValue = false;
      }
      miCopyValue.setEnabled(isValue);
      miPasteValue.setEnabled(false);
      miCut.setEnabled(isRemovable);
      miCopy.setEnabled(isRemovable);
      miPaste.setEnabled(
              StructClipboard.getInstance().getContentType(struct) == StructClipboard.CLIPBOARD_ENTRIES);
    }
    else {
      Object selected = table.getModel().getValueAt(lsm.getMinSelectionIndex(), 1);
      miPaste.setEnabled(
              StructClipboard.getInstance().getContentType(struct) == StructClipboard.CLIPBOARD_ENTRIES);
      if (selected instanceof AddRemovable) {
        miCopyValue.setEnabled(false);
        miPasteValue.setEnabled(false);
        miCut.setEnabled(((AddRemovable)selected).canRemove());
        miCopy.setEnabled(((AddRemovable)selected).canRemove());
      }
      else {
        miCopyValue.setEnabled(!(selected instanceof AbstractStruct));
        miPasteValue.setEnabled(miCopyValue.isEnabled() &&
                                StructClipboard.getInstance().getContentType(struct) ==
                                StructClipboard.CLIPBOARD_VALUES);
        miCut.setEnabled(false);
        miCopy.setEnabled(false);
      }
    }
  }

  private void convertAttribute(int index, JMenuItem menuitem)
  {
    final StructEntry entry = struct.getFields().get(index);
    if (!isCachedStructEntry(entry.getOffset())) setCachedStructEntry(entry);
    ByteBuffer bb = StreamUtils.getByteBuffer(entry.getSize());
    try {
      try (ByteBufferOutputStream bbos = new ByteBufferOutputStream(bb)) {
        entry.write(bbos);
      }
      bb.position(0);
      StructEntry newentry;
      if (menuitem == miToHex) {
        newentry = new Unknown(bb, 0, entry.getSize(), entry.getName());
      } else if (menuitem == miToBin) {
        newentry = new UnknownBinary(bb, 0, entry.getSize(), entry.getName());
      } else if (menuitem == miToDec) {
        newentry = new UnknownDecimal(bb, 0, entry.getSize(), entry.getName());
      } else if (menuitem == miToInt) {
        newentry = new DecNumber(bb, 0, entry.getSize(), entry.getName());
      } else if (menuitem == miToHexInt) {
        newentry = new HexNumber(bb, 0, entry.getSize(), entry.getName());
      } else if (menuitem == miToFlags) {
        newentry = new Flag(bb, 0, entry.getSize(), entry.getName(), null);
      } else if (CMD_TORESLIST.equals(menuitem.getActionCommand())) {
        newentry = new ResourceRef(bb, 0, entry.getName(), menuitem.getText());
      } else if (menuitem == miToString) {
        newentry = new TextString(bb, 0, entry.getSize(), entry.getName());
      } else if (menuitem == miReset) {
        newentry = removeCachedStructEntry(entry.getOffset());
        if (newentry == null) {
          newentry = entry;
        } else {
          ((Readable)newentry).read(bb, 0);
        }
      } else {
        throw new NullPointerException();
      }
      newentry.setOffset(entry.getOffset());
      struct.setField(index, newentry);
      table.getSelectionModel().removeSelectionInterval(index, index);
      table.getSelectionModel().addSelectionInterval(index, index);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void selectEntry(StructEntry structEntry)
  {
    int i = 0;
    for (final StructEntry o : struct.getFields()) {
      if (structEntry == o) {
        table.getSelectionModel().setSelectionInterval(i, i);
        selectEditTab();
        return;
      }
      else if (o instanceof AbstractStruct) {
        selectSubEntry((AbstractStruct)o, structEntry);
      }
      ++i;
    }
  }

  private void selectEntry(AbstractStruct subStruct, int offset)
  {
    for (final StructEntry entry : subStruct.getFields()) {
      if (entry instanceof AbstractStruct)
        selectEntry((AbstractStruct)entry, offset);
      else if (entry.getOffset() == offset)
        selectSubEntry(subStruct, entry);
    }
  }

  private void selectSubEntry(AbstractStruct subStruct, StructEntry structEntry)
  {
    int i = 0;
    for (final StructEntry o : subStruct.getFields()) {
      if (structEntry == o) {
        createViewFrame(getTopLevelAncestor(), subStruct);
//        new ViewFrame(getTopLevelAncestor(), subStruct);
        StructViewer viewer = subStruct.getViewer();
        viewer.table.getSelectionModel().setSelectionInterval(i, i);
        table.scrollRectToVisible(table.getCellRect(i, 0, true));
        viewer.selectEditTab();
        return;
      }
      else if (o instanceof AbstractStruct) {
        selectSubEntry((AbstractStruct)o, structEntry);
      }
      ++i;
    }
  }

  /** Caches the given StructEntry object. */
  private void setCachedStructEntry(StructEntry struct)
  {
    if (struct != null) {
      if (!entryMap.containsKey(Integer.valueOf(struct.getOffset()))) {
        entryMap.put(struct.getOffset(), struct);
      }
    }
  }

  private StructEntry getCachedStructEntry(int offset)
  {
    return entryMap.get(Integer.valueOf(offset));
  }

  /** Removes the StructEntry object at the given offset and returns it. */
  private StructEntry removeCachedStructEntry(int offset)
  {
    return entryMap.remove(Integer.valueOf(offset));
  }

  /** Indicates whether the given StructEntry object is equal to the cached object. */
  private boolean isCachedStructEntry(int offset)
  {
    return entryMap.containsKey(Integer.valueOf(offset));
  }


  /** Recycles existing ViewFrame constructs if possible. */
  private ViewFrame createViewFrame(Component parent, Viewable view)
  {
    ViewFrame frame = null;
    if (view != null) {
      if (parent == null) {
        parent = getTopLevelAncestor();
      }
      frame = viewMap.get(view);
      if (frame == null || !frame.isVisible()) {
        frame = new ViewFrame(parent, view);
        viewMap.put(view, frame);
      } else {
        frame.toFront();
      }
    }
    return frame;
  }

  /**
   * Opens editor for specified {@code editable} and appends it to lower pane of
   * this viewer.
   *
   * @param editable Field of the edited {@link #struct}, that will be selected
   * @throws NullPointerException if {@code editable} is {@code null}
   */
  private void edit(Editable editable) {
    // Save for handle UPDATE_VALUE events later
    this.editable = editable;
    editpanel.removeAll();

    final JComponent editor = editable.edit(this);

    final GridBagLayout gbl = new GridBagLayout();
    final GridBagConstraints gbc = new GridBagConstraints();
    editpanel.setLayout(gbl);
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.VERTICAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.insets = new Insets(3, 3, 3, 3);
    gbl.setConstraints(editor, gbc);
    editpanel.add(editor);

    editpanel.revalidate();
    editpanel.repaint();
    cards.show(lowerpanel, CARD_EDIT);
    editable.select();
  }

  /**
   * Move viewport for this table in such way that specified entry becomes visible
   * and selects it.
   *
   * @param field Field for selection. If this field not in this table or {@code null},
   *        do nothing
   */
  private void select(StructEntry field)
  {
    final int index = struct.getFields().indexOf(field);
    if (index >= 0) {
      table.getSelectionModel().setSelectionInterval(index, index);

      SwingUtilities.invokeLater(() -> {
        final JComponent viewport = (JComponent)table.getParent();
        final Rectangle r = table.getCellRect(index, 0, true);
        final Rectangle v = viewport.getVisibleRect();
        table.scrollRectToVisible(new Rectangle(r.x, r.y, (int) v.getWidth(), (int) v.getHeight()));
      });
    }
  }

//  /**
//   * Selects in the table field that corresponds to the specified offset entry
//   * or opens new child structure viewer, if corresponding field not in this table
//   *
//   * @param entry Offset to show
//   */
//  private void selectOffset(SectionOffset entry)
//  {
//    // Select entry at offset
//    final int offset = entry.getValue();
//    final StructEntry field = struct.getAttribute(offset, entry.getSection());
//    if (field != null) {
//      final AbstractStruct parent = field.getParent();
//      if (parent != struct) {
//        new ViewFrame(this, parent);
//      }
//      parent.getViewer().select(field);
//    }
//  }

  /**
   * Selects the first structure of the specified class type.
   * @param cls Class of the structure to search.
   */
  private void selectFirstEntryOfType(Class<? extends StructEntry> cls)
  {
    if (cls != null) {
      final StructEntry field = struct.getField(cls, 0);
      if (field != null) {
        final AbstractStruct parent = field.getParent();
        if (parent != struct ) {
          new ViewFrame(this, parent);
        }
        parent.getViewer().select(field);
      }
    }
  }

  /**
   * Returns the color associated with the specified class type. Returns Color.WHITE if no class type specified.
   * @param cls The class associated with the field value.
   * @return Color corresponding to the specified field class type. {@code Color.WHITE} by default.
   */
  private Color getClassColor(Class<? extends StructEntry> cls)
  {
    if (cls != null) {
      return fieldColors.computeIfAbsent(cls,
          c -> ViewerUtil.BACKGROUND_COLORS[fieldColors.size() % ViewerUtil.BACKGROUND_COLORS.length]);
    }
    return Color.WHITE;
  }

  /**
   * Creates an Advanced Search filter out of the specified {@code StructEntry} instance
   * and adds it to the Advanced Search dialog.
   */
  private void addToAdvancedSearch(StructEntry entry)
  {
    if (entry == null || entry instanceof AbstractStruct)
      return;

    // setting search value
    SearchOptions so = null;
    if (entry instanceof Flag) {
      so = new SearchOptions();
      so.setValueBitfield(((Flag)entry).getValue(), SearchOptions.BitFieldMode.Exact);
    } else if (entry instanceof IsReference) {
      so = new SearchOptions();
      so.setValueResource(((IsReference)entry).getResourceName());
    } else if (entry instanceof IsNumeric) {
      so = new SearchOptions();
      so.setValueNumber(((IsNumeric)entry).getValue());
    } else if (!(entry instanceof Unknown)) {
      so = new SearchOptions();
      so.setValueText(entry.toString(), false, false);
    } else {
      return;
    }

    // setting structure level and field name
    List<String> structure = so.getStructure();
    for (AbstractStruct struct = entry.getParent(); struct != null && struct.getParent() != null; struct = struct.getParent())
      structure.add(0, getStrippedFieldName(struct.getName()));
    so.setSearchName(entry.getName(), true, false);

    // root structure of resource needed for resource name
    AbstractStruct root = struct;
    while (root.getParent() != null)
      root = root.getParent();
    if (root == null || root.getResourceEntry() == null)
      return;

    AdvancedSearch dlg = ChildFrame.show(AdvancedSearch.class, () -> new AdvancedSearch());
    dlg.setResourceType(root.getResourceEntry().getExtension());
    dlg.addFilter(so);
  }

  /** Strips numeric indices from the specified field name. */
  private String getStrippedFieldName(String name)
  {
    Pattern p = Pattern.compile("(.+)\\s+\\d+");
    Matcher m = p.matcher(name);
    if (m.find())
      return m.group(1);

    return name;
  }

// -------------------------- INNER CLASSES --------------------------

  private final class PopupListener extends MouseAdapter
  {
    @Override
    public void mousePressed(MouseEvent e)
    {
      maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
      maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e)
    {
      if (e.isPopupTrigger())
        popupmenu.show(e.getComponent(), e.getX(), e.getY());
    }
  }

  private static final class StructTable extends JTable implements Printable
  {
    private final AbstractStruct struct;

    private StructTable(AbstractStruct struct)
    {
      this.struct = struct;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException
    {
      Graphics2D g2 = (Graphics2D)graphics;
      g2.setColor(Color.BLACK);
      int fontHeight = g2.getFontMetrics().getHeight();
      int fontDesent = g2.getFontMetrics().getDescent();

      //leave room for page number
      double pageHeight = pageFormat.getImageableHeight() - (double)fontHeight;
      double pageWidth = pageFormat.getImageableWidth();
      double tableWidth = (double)getColumnModel().getTotalColumnWidth();
      double scale = (double)1;
      if (tableWidth >= pageWidth)
        scale = pageWidth / tableWidth;

      double headerHeightOnPage = (double)getTableHeader().getHeight() * scale;
      double tableWidthOnPage = tableWidth * scale;

      double oneRowHeight = (double)getRowHeight() * scale;
      int numRowsOnAPage = (int)((pageHeight - headerHeightOnPage) / oneRowHeight);
      double pageHeightForTable = oneRowHeight * (double)numRowsOnAPage;
      int totalNumPages = (int)Math.ceil((double)getRowCount() / (double)numRowsOnAPage);
      if (pageIndex >= totalNumPages)
        return NO_SUCH_PAGE;

      g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
      //bottom center
      g2.drawString(struct.getName() + " - Page " + (pageIndex + 1) + '/' + totalNumPages,
                    ((int)pageWidth >> 1) - 35, (int)(pageHeight + (double)fontHeight - (double)fontDesent));

      g2.translate((double)0.0f, headerHeightOnPage - (double)pageIndex * pageHeightForTable);

      //If this piece of the table is smaller than the size available, clip to the appropriate bounds.
      if (pageIndex + 1 == totalNumPages) {
        int lastRowPrinted = numRowsOnAPage * pageIndex;
        int numRowsLeft = getRowCount() - lastRowPrinted;
        g2.setClip(0, (int)(pageHeightForTable * (double)pageIndex), (int)Math.ceil(tableWidthOnPage),
                   (int)Math.ceil(oneRowHeight * (double)numRowsLeft));
      }
      //else clip to the entire area available.
      else
        g2.setClip(0, (int)(pageHeightForTable * (double)pageIndex), (int)Math.ceil(tableWidthOnPage),
                   (int)Math.ceil(pageHeightForTable));

      g2.scale(scale, scale);
      paint(g2);
      g2.scale((double)1 / scale, (double)1 / scale);
      g2.translate((double)0.0f, (double)pageIndex * pageHeightForTable - headerHeightOnPage);
      g2.setClip(0, 0, (int)Math.ceil(tableWidthOnPage), (int)Math.ceil(headerHeightOnPage));
      g2.scale(scale, scale);
      getTableHeader().paint(g2);
      //paint header at top

      return Printable.PAGE_EXISTS;
    }
  }
}
