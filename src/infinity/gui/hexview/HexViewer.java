// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui.hexview;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import infinity.NearInfinity;
import infinity.datatype.Bitmap;
import infinity.datatype.ColorPicker;
import infinity.datatype.ColorValue;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.HashBitmap;
import infinity.datatype.IDSTargetEffect;
import infinity.datatype.IdsBitmap;
import infinity.datatype.Kit2daBitmap;
import infinity.datatype.MultiNumber;
import infinity.datatype.ProRef;
import infinity.datatype.ResourceRef;
import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.datatype.Song2daBitmap;
import infinity.datatype.StringRef;
import infinity.datatype.TextBitmap;
import infinity.datatype.TextEdit;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.datatype.UnsignDecNumber;
import infinity.gui.BrowserMenuBar;
import infinity.gui.ButtonPanel;
import infinity.gui.StatusBar;
import infinity.gui.ViewerUtil;
import infinity.gui.WindowBlocker;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.Closeable;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.dlg.AbstractCode;
import infinity.resource.key.BIFFResourceEntry;
import infinity.resource.key.ResourceEntry;
import infinity.util.io.FileNI;
import infinity.util.io.FileOutputStreamNI;
import infinity.util.io.FileWriterNI;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import tv.porst.jhexview.DataChangedEvent;
import tv.porst.jhexview.HexViewEvent;
import tv.porst.jhexview.IColormap;
import tv.porst.jhexview.IDataChangedListener;
import tv.porst.jhexview.IDataProvider;
import tv.porst.jhexview.IHexViewListener;
import tv.porst.jhexview.IMenuCreator;
import tv.porst.jhexview.JHexView;

/**
 * A hex viewer and editor component designed to be used as separate tab in resource viewers.
 *
 * Not implemented: proper support for AbstractAction (changes in referenced text blocks are ignored)
 *
 * @author argent77
 */
public class HexViewer extends JPanel implements IHexViewListener, IDataChangedListener,
                                                 ActionListener, ChangeListener, Closeable
{
  private static final ButtonPanel.Control BUTTON_FIND      = ButtonPanel.Control.FindButton;
  private static final ButtonPanel.Control BUTTON_FINDNEXT  = ButtonPanel.Control.Custom1;
  private static final ButtonPanel.Control BUTTON_EXPORT    = ButtonPanel.Control.ExportButton;
  private static final ButtonPanel.Control BUTTON_SAVE      = ButtonPanel.Control.Save;
  private static final ButtonPanel.Control BUTTON_REFRESH   = ButtonPanel.Control.Custom2;

  private static final String FMT_OFFSET = "%1$Xh (%1$d)";

  private final AbstractStruct struct;
  private final JHexView hexView = new JHexView();
  private final InfoPanel pInfo;
  private final ButtonPanel buttonPanel = new ButtonPanel();

  private FindData findData;
  private IDataProvider dataProvider;
  private IColormap colorMap;
  private IMenuCreator menuCreator;
  private JScrollPane spInfo;
  private boolean tabSelected;
  private int cachedSize;

  // Returns a short description of the specified structure type
  public static String getTypeDesc(StructEntry type)
  {
    if (type instanceof AbstractStruct) {
      return "Nested structure";
    } else if (type instanceof AbstractCode) {
      return "Script code";
    } else if (type instanceof Bitmap || type instanceof HashBitmap || type instanceof IdsBitmap ||
        type instanceof Kit2daBitmap || type instanceof Song2daBitmap) {
      return "Numeric type or identifier";
    } else if (type instanceof ColorPicker) {
      return "RGB Color";
    } else if (type instanceof ColorValue) {
      return "Palette index";
    } else if (type instanceof SectionCount) {
      return "Count of a structure type";
    } else if (type instanceof SectionOffset) {
      return "Start offset of a structure type";
    } else if (type instanceof DecNumber || type instanceof MultiNumber ||
               type instanceof UnsignDecNumber) {
      return "Number";
    } else if (type instanceof Flag) {
      return "Flags/Bitfield";
    } else if (type instanceof IDSTargetEffect) {
      return "IDS file/entry";
    } else if (type instanceof ProRef) {
      return "Projectile";
    } else if (type instanceof ResourceRef) {
      return "Resource reference";
    } else if (type instanceof StringRef) {
      return "String reference";
    } else if (type instanceof TextBitmap || type instanceof TextEdit ||
               type instanceof TextString) {
      return "Text field";
    } else if (type instanceof Unknown) {
      return "Unknown or unused data";
    } else {
      return "n/a";
    }
  }


  public HexViewer(AbstractStruct struct)
  {
    this(struct, null, null);
  }

  public HexViewer(AbstractStruct struct, IColormap colorMap)
  {
    this(struct, colorMap, null);
  }

  public HexViewer(AbstractStruct struct, IColormap colorMap, IDataProvider dataProvider)
  {
    super();

    if (struct == null) {
      throw new NullPointerException("struct is null");
    }
    this.struct = struct;
    this.dataProvider = (dataProvider == null) ? new StructuredDataProvider(this.struct) : dataProvider;
    this.dataProvider.addListener(this);
    this.colorMap = colorMap;
    this.menuCreator = new ResourceMenuCreator(getHexView(), this.struct);

    if (this.dataProvider instanceof StructuredDataProvider) {
      this.pInfo = new InfoPanel();
    } else {
      this.pInfo = null;
    }

    initGUI();
  }

//--------------------- Begin Interface IHexViewListener ---------------------

  @Override
  public void stateChanged(HexViewEvent event)
  {
    if (event.getSource() instanceof JHexView &&
        event.getCause() == HexViewEvent.Cause.SelectionChanged &&
        getStruct().isRawTabSelected()) {
      JHexView hv = (JHexView)event.getSource();
      int offset = (int)hv.getCurrentOffset();

      // updating info panel
      updateInfoPanel(offset);

      // updating statusbar
      updateStatusBar(offset);
    }
  }

//--------------------- End Interface IHexViewListener ---------------------

//--------------------- Begin Interface IDataChangedListener ---------------------

  @Override
  public void dataChanged(DataChangedEvent event)
  {
    getStruct().setStructChanged(true);
  }

//--------------------- End Interface IDataChangedListener ---------------------

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == buttonPanel.getControlByType(BUTTON_FIND)) {
      if (getFindData().find()) {
        boolean b;
        String s = null;
        if (getFindData().getDataType() == FindData.Type.Text) {
          b = !getFindData().getText().isEmpty();
          s = getFindData().getText();
        } else {
          b = (getFindData().getBytes().length > 0);
          if (getFindData().getBytes().length > 0) {
            s = byteArrayToString(getFindData().getBytes());
          }
        }

        findPattern((int)getHexView().getCurrentOffset());

        // Setting up "Find next" button
        JComponent c = buttonPanel.getControlByType(BUTTON_FINDNEXT);
        c.setEnabled(b);
        if (s == null) {
          c.setToolTipText(null);
        } else if (s.length() <= 30) {
          c.setToolTipText(String.format("Find \"%1$s\"", s));
        } else {
          c.setToolTipText(String.format("Find \"%1$s...\"", s.substring(0, 30)));
        }
      }
      getHexView().requestFocusInWindow();
    } else if (event.getSource() == buttonPanel.getControlByType(BUTTON_FINDNEXT)) {
      findPattern((int)getHexView().getCurrentOffset());
      getHexView().requestFocusInWindow();
    } else if (event.getSource() == buttonPanel.getControlByType(BUTTON_EXPORT)) {
      ResourceFactory.getInstance().exportResource(getStruct().getResourceEntry(), getTopLevelAncestor());
      getHexView().requestFocusInWindow();
    } else if (event.getSource() == buttonPanel.getControlByType(BUTTON_SAVE)) {
      // XXX: Ugly hack: mimicking ResourceFactory.saveResource()
      IDataProvider dataProvider = getHexView().getData();
      ResourceEntry entry = getStruct().getResourceEntry();
      File output;
      if (entry instanceof BIFFResourceEntry) {
        output = FileNI.getFile(ResourceFactory.getRootDirs(),
                                ResourceFactory.OVERRIDEFOLDER + File.separatorChar + entry.toString());
        File override = FileNI.getFile(ResourceFactory.getRootDirs(), ResourceFactory.OVERRIDEFOLDER);
        if (!override.exists()) {
          override.mkdir();
        }
        ((BIFFResourceEntry)entry).setOverride(true);
      } else {
        output = entry.getActualFile();
      }

      if (output != null && output.exists()) {
        String options[] = {"Overwrite", "Cancel"};
        if (JOptionPane.showOptionDialog(this, output + " exists. Overwrite?", "Save resource",
                                         JOptionPane.YES_NO_OPTION,
                                         JOptionPane.WARNING_MESSAGE, null, options, options[0]) == 0) {
          if (BrowserMenuBar.getInstance().backupOnSave()) {
            try {
              File bakFile = new FileNI(output.getCanonicalPath() + ".bak");
              if (bakFile.isFile()) {
                bakFile.delete();
              }
              if (!bakFile.exists()) {
                output.renameTo(bakFile);
              }
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        } else {
          return;
        }
      }

      try {
        byte[] buffer = dataProvider.getData(0, dataProvider.getDataLength());
        try {
          // make sure that nothing interferes with the writing process
          getHexView().setEnabled(false);
          BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStreamNI(output));
          FileWriterNI.writeBytes(bos, buffer);
          bos.close();
        } finally {
          getHexView().setEnabled(true);
        }
        buffer = null;
        getStruct().setStructChanged(false);
        getHexView().clearModified();
        JOptionPane.showMessageDialog(this, "File saved to \"" + output.getAbsolutePath() + '\"',
                                      "Save complete", JOptionPane.INFORMATION_MESSAGE);
      } catch (IOException e) {
        JOptionPane.showMessageDialog(this, "Error while saving " + getStruct().getResourceEntry().toString(),
                                      "Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
        return;
      }
      getHexView().requestFocusInWindow();
    } else if (event.getSource() == buttonPanel.getControlByType(BUTTON_REFRESH)) {
      dataModified(true);
      getHexView().requestFocusInWindow();
    }
  }

//--------------------- End Interface ActionListener ---------------------

//--------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent e)
  {
    if (e.getSource() instanceof JTabbedPane) {
      if (!tabSelected && getStruct().isRawTabSelected()) {
        // actions when entering Raw tab
        tabSelected = true;

        // performs lazy first-time initializations
        initialize();

        getHexView().requestFocusInWindow();
        updateStatusBar((int)getHexView().getCurrentOffset());
      } else if (tabSelected) {
        // actions when leaving Raw tab
        tabSelected = false;
        getHexView().clearModified();
        getHexView().resetUndo();
        updateStatusBar(-1);
      }
    }
  }

//--------------------- End Interface ChangeListener ---------------------

//--------------------- Begin Interface Closeable ---------------------

  @Override
  public void close() throws Exception
  {
    // setting HexView component invisible will clean up resources
    getHexView().setVisible(false);

    if (dataProvider instanceof StructuredDataProvider) {
      ((StructuredDataProvider)dataProvider).close();
    } else if (dataProvider instanceof ResourceDataProvider) {
      ((ResourceDataProvider)dataProvider).clear();
    }

    if (colorMap instanceof BasicColorMap) {
      ((BasicColorMap)colorMap).close();
    }

    if (findData != null) {
      findData.dispose();
      findData = null;
    }
  }

//--------------------- End Interface Closeable ---------------------

  /** Returns the associated resource structure. */
  public AbstractStruct getStruct()
  {
    return struct;
  }

  /** Returns the HexView component. */
  public JHexView getHexView()
  {
    return hexView;
  }

  /** Notify HexViewer that data has been changed. Executes a forced reset. */
  public void dataModified()
  {
    dataModified(true);
  }

  /** Notify HexViewer that data has been changed. Specify whether to force a reset. */
  public void dataModified(boolean force)
  {
    if (force || cachedSize != getDataProvider().getDataLength()) {
      WindowBlocker.blockWindow(true);
      try {
        if (getDataProvider() instanceof StructuredDataProvider) {
          // notifying data provider that data has changed
          ((StructuredDataProvider)getDataProvider()).reset();
        }
        if (hexView.isColorMapEnabled()) {
          // notifying color map that data has changed
          if (getColorMap() instanceof BasicColorMap) {
            ((BasicColorMap)getColorMap()).reset();
          }
        }

        cachedSize = getDataProvider().getDataLength();
      } finally {
        WindowBlocker.blockWindow(false);
      }
    }
  }

  // initialize controls
  private void initGUI()
  {
    setLayout(new BorderLayout());

    // configuring hexview
    Color textColor = dataProvider.isEditable() ? Color.BLACK: Color.GRAY;
    hexView.setEnabled(false);
    hexView.setDefinitionStatus(JHexView.DefinitionStatus.UNDEFINED);
    hexView.setAddressMode(JHexView.AddressMode.BIT32);
    hexView.setSeparatorsVisible(false);
    hexView.setBytesPerColumn(1);
    hexView.setBytesPerRow(16);
    hexView.setColumnSpacing(8);
    hexView.setMouseOverHighlighted(false);
    hexView.setShowModified(true);
    hexView.setCaretColor(Color.BLACK);
    hexView.setFontSize(13);
    hexView.setHeaderFontStyle(Font.BOLD);
    hexView.setFontColorHeader(new Color(0x0000c0));
    hexView.setBackgroundColorOffsetView(hexView.getBackground());
    hexView.setFontColorOffsetView(new Color(0x0000c0));
    hexView.setBackgroundColorHexView(hexView.getBackground());
    hexView.setFontColorHexView1(textColor);
    hexView.setFontColorHexView2(textColor);
    hexView.setBackgroundColorAsciiView(hexView.getBackground());
    hexView.setFontColorAsciiView(textColor);
    hexView.setFontColorModified(Color.RED);
    hexView.setSelectionColor(new Color(0xc0c0c0));

    // Info panel only available for structured data
    if (pInfo != null) {
      spInfo = new JScrollPane(pInfo);
      spInfo.getVerticalScrollBar().setUnitIncrement(16);

      JSplitPane splitv = new JSplitPane(JSplitPane.VERTICAL_SPLIT, hexView, spInfo);
      splitv.setDividerLocation(2 * NearInfinity.getInstance().getContentPane().getHeight() / 3);
      add(splitv, BorderLayout.CENTER);

    } else {
      add(hexView, BorderLayout.CENTER);
    }

    // setting up button panel
    JButton b = (JButton)buttonPanel.addControl(BUTTON_FIND);
    b.addActionListener(this);
    b = (JButton)buttonPanel.addControl(new JButton("Find next"), BUTTON_FINDNEXT);
    b.setEnabled(false);
    b.addActionListener(this);
    b = (JButton)buttonPanel.addControl(BUTTON_EXPORT);
    b.addActionListener(this);
    b = (JButton)buttonPanel.addControl(BUTTON_SAVE);
    b.addActionListener(this);
    b = (JButton)buttonPanel.addControl(new JButton("Refresh"), BUTTON_REFRESH);
    b.setIcon(Icons.getIcon("Refresh16.gif"));
    b.setToolTipText("Force a refresh of the displayed data");
    b.addActionListener(this);

    add(buttonPanel, BorderLayout.SOUTH);
  }

  private FindData getFindData()
  {
    if (findData == null) {
      Window w = null;
      if (getStruct().getViewer() != null) {
        w = SwingUtilities.getWindowAncestor(getStruct().getViewer());
      }
      if (w == null) {
        w = NearInfinity.getInstance();
      }
      findData = new FindData(w);
    }

    return findData;
  }

  // Attempts to find the next match of the search string as defined in the FindData instance, starting at offset.
  private void findPattern(int offset)
  {
    if (getFindData().getDataType() == FindData.Type.Text) {
      offset = getHexView().findAscii(offset, getFindData().getText(), getFindData().isCaseSensitive());
      if (offset >= 0) {
        getHexView().setCurrentOffset(offset);
        getHexView().setSelectionLength(getFindData().getText().length()*2);
      } else {
        JOptionPane.showMessageDialog(this, "No match found.", "Find", JOptionPane.INFORMATION_MESSAGE);
      }
    } else {
      if (getFindData().getBytes().length > 0) {
        offset = getHexView().findHex(offset, getFindData().getBytes());
        if (offset >= 0) {
          getHexView().setCurrentOffset(offset);
          getHexView().setSelectionLength(getFindData().getBytes().length*2);
        } else {
          JOptionPane.showMessageDialog(this, "No match found.", "Find", JOptionPane.INFORMATION_MESSAGE);
        }
      } else {
        JOptionPane.showMessageDialog(this, "Search string does not contain valid byte values",
                                      "Find", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private String byteArrayToString(byte[] buffer)
  {
    StringBuilder sb = new StringBuilder();
    sb.append('[');
    if (buffer != null) {
      for (int i = 0; i < buffer.length; i++) {
        sb.append(String.format("%1$02X", buffer[i] & 0xff));
        if (i+1 < buffer.length) {
          sb.append(", ");
        }
      }
    }
    sb.append(']');
    return sb.toString();
  }

  // Update statusbar
  private void updateStatusBar(int offset)
  {
    StatusBar sb = NearInfinity.getInstance().getStatusBar();
    if (offset >= 0) {
      sb.setCursorText(String.format(FMT_OFFSET, offset));
    } else {
      sb.setCursorText("");
    }
  }

  // Update info panel
  private void updateInfoPanel(int offset)
  {
    if (pInfo != null) {
      pInfo.setOffset(offset);
    }
  }

  private IDataProvider getDataProvider()
  {
    return dataProvider;
  }

  private IColormap getColorMap()
  {
    return colorMap;
  }

  private boolean isInitialized()
  {
    return (getHexView().getData() == dataProvider && getHexView().getColorMap() == colorMap);
  }

  // Performs final initializations required to display data
  private void initialize()
  {
    if (!isInitialized()) {
      hexView.setEnabled(true);
      hexView.setColormap(colorMap);
      hexView.setColorMapEnabled(BrowserMenuBar.getInstance().getHexColorMapEnabled());
      hexView.setMenuCreator(menuCreator);
      hexView.setEnabled(true);
      hexView.addHexListener(this);
      hexView.setData(dataProvider);
      hexView.setDefinitionStatus(hexView.getData().getDataLength() > 0 ?
          JHexView.DefinitionStatus.DEFINED : JHexView.DefinitionStatus.UNDEFINED);

      if (pInfo != null) {
        pInfo.setOffset(0);
      }

      cachedSize = getDataProvider().getDataLength();
    }
  }

//-------------------------- INNER CLASSES --------------------------

  // Panel component showing information about the currently selected data.
  private final class InfoPanel extends JPanel
  {
    private final List<StructEntryTableModel> listModels = new ArrayList<StructEntryTableModel>();
    private final List<Component> listComponents = new ArrayList<Component>();

    private JPanel mainPanel;
    private int offset;

    public InfoPanel()
    {
      super();

      if (struct == null) {
        throw new NullPointerException("struct is null");
      }

      init();
    }

//    /** Returns current offset. */
//    public int getOffset()
//    {
//      return offset;
//    }

    /** Sets new offset and updates info panel. */
    public void setOffset(int offset)
    {
      if (offset != this.offset) {
        this.offset = offset;
        updatePanel(this.offset);
      }
    }

    // Initialize controls
    private void init()
    {
      setLayout(new GridBagLayout());
      setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      offset = -1;

      mainPanel = new JPanel();
      mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
      GridBagConstraints gbc = new GridBagConstraints();
      gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
      add(mainPanel, gbc);
      gbc = ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
      add(new JPanel(), gbc);
    }

    // Updates tables and table models based on the data at the specified offset
    private void updatePanel(int offset)
    {
      StructuredDataProvider data = (getDataProvider() instanceof StructuredDataProvider) ?
                                    (StructuredDataProvider)getDataProvider() : null;
      if (data != null) {
        // creating list of nested StructEntry objects
        StructEntry newEntry = data.getFieldAt(offset);
        final List<StructEntry> list;
        if (newEntry != null) {
          list = newEntry.getStructChain();
          if (!list.isEmpty() && list.get(0) == getStruct()) {
            list.remove(0);
          }
        } else {
          list = new ArrayList<StructEntry>();
        }

        // removing invalid models and controls
        int lastIdx = listModels.size() - 1;
        for ( ; lastIdx >= 0; lastIdx--) {
          StructEntry curEntry = listModels.get(lastIdx).getStruct();
          if (!list.contains(curEntry)) {
            listModels.remove(lastIdx);
            Component c = listComponents.remove(lastIdx);
            removeComponentFromPanel(c);
          } else {
            break;
          }
        }
        // lastIdx contains the highest index of remaining structures

        // adding updated models and tables to the lists
        for (int i = lastIdx + 1; i < list.size(); i++) {
          StructEntryTableModel model = new StructEntryTableModel(list.get(i));
          listModels.add(model);
          Component c = createInfoTable(model, listComponents.size() + 1);
          listComponents.add(c);
          addComponentToPanel(c);
        }
      } else {
        for (int i = listModels.size() - 1; i >= 0; i--) {
          listModels.remove(i);
          Component c = listComponents.remove(i);
          removeComponentFromPanel(c);
        }
      }

      // notifying panel of the changed layout
      revalidate();
      repaint();
    }

    // Constructs and initializes a new table panel based on the specified model and level information
    private Component createInfoTable(TableModel model, int level)
    {
      final String[] suffix = {"th", "st", "nd", "rd", "th"};

      JPanel retVal = new JPanel(new GridBagLayout());

      // creating title
      JLabel l = new JLabel(String.format("%1$d%2$s level structure:",
                                          level, suffix[Math.max(0, Math.min(level, 4))]));
      l.setFont(l.getFont().deriveFont(Font.BOLD));
      GridBagConstraints gbc = new GridBagConstraints();
      gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
      retVal.add(l, gbc);

      // creating table
      JTable table = new JTable(model);
      table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      table.setFont(BrowserMenuBar.getInstance().getScriptFont());
      table.setBorder(BorderFactory.createLineBorder(Color.GRAY));
      table.getTableHeader().setBorder(BorderFactory.createLineBorder(Color.GRAY));
      table.getTableHeader().setReorderingAllowed(false);
      table.getTableHeader().setResizingAllowed(true);
      table.setFocusable(false);
      table.setEnabled(false);

      final String maxString = String.format("%1$080d", 0);
      Font f = BrowserMenuBar.getInstance().getScriptFont();
      FontMetrics fm = table.getFontMetrics(f);
      Rectangle2D rect = f.getStringBounds(maxString, fm.getFontRenderContext());
      Dimension d = table.getPreferredSize();
      d.width = (int)rect.getWidth();
      table.setPreferredSize(d);

      gbc = ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(2, 0, 16, 0), 0, 0);
      retVal.add(table, gbc);

      return retVal;
    }

    // Removes the specified component from the info panel
    private void removeComponentFromPanel(Component c)
    {
      if (c != null) {
        mainPanel.remove(c);
      }
    }

    // Adds the specified component to the info panel
    private void addComponentToPanel(Component c)
    {
      if (c != null) {
        if (!isAncestorOf(c)) {
          mainPanel.add(c);
        }
      }
    }
  }


  // Manages the representation of a single StructEntry instance
  private class StructEntryTableModel extends AbstractTableModel
  {
    private final String[] names = new String[]{"Name", "Start offset", "Length",
                                                 "Structure type", "Value"};

    private final StructEntry entry;

    public StructEntryTableModel(StructEntry entry)
    {
      super();
      this.entry = entry;
    }

    //--------------------- Begin Class AbstractTableModel ---------------------

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
      return String.class;
    }

    @Override
    public int getRowCount()
    {
      return names.length;
    }

    @Override
    public int getColumnCount()
    {
      return 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
      if (columnIndex == 0) {
        if (rowIndex >= 0 && rowIndex < getRowCount()) {
          return names[rowIndex];
        }
      } else if (columnIndex == 1) {
        if (getStruct() != null) {
          switch (rowIndex) {
            case 0: // Name
              return getStruct().getName();
            case 1: // Start offset
              return String.format("%1$Xh (%1$d)", getStruct().getOffset());
            case 2: // Length
              return String.format("%1$d byte%2$s",
                                   getStruct().getSize(), (getStruct().getSize() != 1) ? "s" : "");
            case 3: // Structure type
              return getTypeDesc(getStruct());
            case 4: // Field value
            {
              String s = getStruct().toString();
              return (s.length() > 30) ? s.substring(0, 30) + "..." : s;
            }
          }
        }
      }
      return "";
    }

    //--------------------- End Class AbstractTableModel ---------------------

    /** Returns the associated StructEntry instance. */
    public StructEntry getStruct()
    {
      return entry;
    }
  }


  // Searches for either text or byte values in the current resource data
  private static final class FindData extends JDialog implements ActionListener, ItemListener,
                                                                 DocumentListener
  {
    /** The data type of the search string. */
    public enum Type { Text, Bytes }

    private String text;
    private byte[] bytes;
    private boolean retVal;

    private JCheckBox cbCaseSensitive;
    private JButton bOk, bCancel;
    private JTextField tfSearch;
    private JComboBox cbType;

    public FindData(Window parent)
    {
      super(parent, "Find", Dialog.ModalityType.APPLICATION_MODAL);
      init(parent);
      text = "";
      bytes = new byte[0];
      retVal = false;
    }

    //--------------------- Begin Interface ActionListener ---------------------

    @Override
    public void actionPerformed(ActionEvent e)
    {
      if (e.getSource() == bOk || e.getSource() == bCancel) {
        retVal = (e.getSource() == bOk);
        if(retVal) {
          text = tfSearch.getText();
          bytes = parseBytes(getText());
        }
        setVisible(false);
      }
    }

    //--------------------- End Interface ActionListener ---------------------

    //--------------------- Begin Interface ItemListener ---------------------

    @Override
    public void itemStateChanged(ItemEvent e)
    {
      if (e.getSource() == cbType) {
        cbCaseSensitive.setEnabled(cbType.getSelectedIndex() == 0);
      }
    }

    //--------------------- End Interface ItemListener ---------------------

    //--------------------- Begin Interface DocumentListener ---------------------

    @Override
    public void insertUpdate(DocumentEvent e)
    {
      bOk.setEnabled(!tfSearch.getText().isEmpty());
    }

    @Override
    public void removeUpdate(DocumentEvent e)
    {
      bOk.setEnabled(!tfSearch.getText().isEmpty());
    }

    @Override
    public void changedUpdate(DocumentEvent e)
    {
    }

    //--------------------- End Interface DocumentListener ---------------------

    /** Displays a Find dialog and returns whether the Find action has been initiated. */
    public boolean find()
    {
      tfSearch.requestFocusInWindow();
      tfSearch.selectAll();
      setVisible(true);
      return retVal;
    }

    /** Returns whether to consider cases when searching text. */
    public boolean isCaseSensitive()
    {
      return cbCaseSensitive.isSelected();
    }

    /** Returns the selected data type. */
    public Type getDataType()
    {
      return (cbType.getSelectedIndex() == 0) ? Type.Text : Type.Bytes;
    }

    /**
     * Returns the unprocessed text string. Can be used as is for text type.
     * Use {@link #getBytes()} to return the parsed byte data.
     */
    public String getText()
    {
      return text;
    }

    /**
     * Returns the interpreted byte data from the search string if data type "Byte Values" is selected.
     * Returns an empty byte array otherwise.
     */
    public byte[] getBytes()
    {
      return bytes;
    }

    private void init(Window parent)
    {
      setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
      setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();

      // Options
      JPanel pOptions = new JPanel(new GridBagLayout());
      cbCaseSensitive = new JCheckBox("Case sensitive", false);
      cbCaseSensitive.setMnemonic('c');
      gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
      pOptions.add(cbCaseSensitive, gbc);

      // Buttons
      JPanel pButtons = new JPanel(new GridBagLayout());
      bOk = new JButton("OK");
      bOk.addActionListener(this);
      bOk.setEnabled(false);
      bCancel = new JButton("Cancel");
      bCancel.addActionListener(this);
      bOk.setPreferredSize(bCancel.getPreferredSize());
      gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
      pButtons.add(new JPanel(), gbc);
      gbc = ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0);
      pButtons.add(bOk, gbc);
      gbc = ViewerUtil.setGBC(gbc, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0);
      pButtons.add(bCancel, gbc);

      // putting all together
      JPanel pMain = new JPanel(new GridBagLayout());
      JLabel lSearch = new JLabel("Search for:", SwingConstants.RIGHT);
      JLabel lType = new JLabel("Datatype:", SwingConstants.RIGHT);
      tfSearch = new JTextField();
      tfSearch.getDocument().addDocumentListener(this);
      cbType = new JComboBox(new String[]{"Text String", "Hex Values"});
      cbType.addItemListener(this);
      gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
      pMain.add(lSearch, gbc);
      gbc = ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
      pMain.add(tfSearch, gbc);
      gbc = ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
      pMain.add(lType, gbc);
      gbc = ViewerUtil.setGBC(gbc, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(4, 8, 0, 0), 0, 0);
      pMain.add(cbType, gbc);

      gbc = ViewerUtil.setGBC(gbc, 0, 2, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
      pMain.add(new JPanel(), gbc);
      gbc = ViewerUtil.setGBC(gbc, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0);
      pMain.add(pOptions, gbc);
      gbc = ViewerUtil.setGBC(gbc, 0, 3, 3, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_END,
                              GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
      pMain.add(pButtons, gbc);

      gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_END,
                              GridBagConstraints.BOTH, new Insets(8, 8, 8, 8), 0, 0);
      add(pMain, gbc);

      // finalizing dialog content
      pack();
      Dimension d = getPreferredSize();
      setMinimumSize(new Dimension(d.width, d.height));
      setPreferredSize(new Dimension(d.width * 3 / 2, d.height));
      pack();
      setLocationRelativeTo(parent);

      // setting up shortcuts
      final InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
      final ActionMap actionMap = getRootPane().getActionMap();
      inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Enter");
      actionMap.put("Enter", new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e)
        {
          bOk.doClick();
        }
      });
      inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Escape");
      actionMap.put("Escape", new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e)
        {
          bCancel.doClick();
        }
      });
    }

    // Attempts to parse useful byte values from the specified text string
    private byte[] parseBytes(String text)
    {
      List<Byte> list = new ArrayList<Byte>();

      // parsing text string
      StringBuilder sb = new StringBuilder();
      for (int idx = 0; idx < text.length(); idx++) {
        char ch = text.charAt(idx);
        if (Character.digit(ch, 16) >= 0) {
          sb.append(ch);
        } else if (!Character.isWhitespace(ch)) {
          idx = text.length() - 1;  // skip to end
        }
        if (sb.length() == 2 || (idx+1 == text.length() && sb.length() > 0)) {
          try {
            int value = Integer.parseInt(sb.toString(), 16);
            list.add(Byte.valueOf((byte)value));
            sb.delete(0, sb.length());
          } catch (NumberFormatException e) {
            break;
          }
        }
      }

      // putting values into byte array
      byte[] retVal = new byte[list.size()];
      for (int i = 0; i < retVal.length; i++) {
        retVal[i] = list.get(i).byteValue();
      }

      return retVal;
    }
  }
}
