// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.check;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.NearInfinity;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.IsReference;
import org.infinity.datatype.IsTextual;
import org.infinity.datatype.SectionOffset;
import org.infinity.datatype.StringRef;
import org.infinity.datatype.TextString;
import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.SortableTable;
import org.infinity.gui.TableItem;
import org.infinity.gui.ViewFrame;
import org.infinity.gui.menu.BrowserMenuBar;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.Profile;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.Viewable;
import org.infinity.resource.bcs.Compiler;
import org.infinity.resource.bcs.ScriptMessage;
import org.infinity.resource.bcs.ScriptType;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.sto.ItemSale11;
import org.infinity.resource.wed.Overlay;
import org.infinity.resource.wed.Tilemap;
import org.infinity.util.Misc;
import org.infinity.util.StringTable;
import org.tinylog.Logger;

public final class StructChecker extends AbstractChecker implements ListSelectionListener {
  private static final String[] FILETYPES = { "ARE", "CHR", "CHU", "CRE", "DLG", "EFF", "GAM", "ITM", "PRO", "SPL",
                                              "STO", "VEF", "VVC", "WED", "WMP" };

  private static final HashMap<String, StructInfo> FILE_INFO = new HashMap<>();

  static {
    FILE_INFO.put("ARE", new StructInfo("AREA", new String[] { "V1.0", "V9.1" }));
    FILE_INFO.put("CHR", new StructInfo("CHR ", new String[] { "V1.0", "V1.2", "V2.0", "V2.1", "V2.2", "V9.0" }));
    FILE_INFO.put("CHU", new StructInfo("CHUI", new String[] { "V1  " }));
    FILE_INFO.put("CRE", new StructInfo("CRE ", new String[] { "V1.0", "V1.1", "V1.2", "V2.2", "V9.0" }));
    FILE_INFO.put("DLG", new StructInfo("DLG ", new String[] { "V1.0" }));
    FILE_INFO.put("EFF", new StructInfo("EFF ", new String[] { "V2.0" }));
    FILE_INFO.put("GAM", new StructInfo("GAME", new String[] { "V1.1", "V2.0", "V2.1", "V2.2" }));
    FILE_INFO.put("ITM", new StructInfo("ITM ", new String[] { "V1  ", "V1.1", "V2.0" }));
    FILE_INFO.put("PRO", new StructInfo("PRO ", new String[] { "V1.0" }));
    FILE_INFO.put("SPL", new StructInfo("SPL ", new String[] { "V1  ", "V2.0" }));
    FILE_INFO.put("STO", new StructInfo("STOR", new String[] { "V1.0", "V1.1", "V9.0" }));
    FILE_INFO.put("VEF", new StructInfo("VEF ", new String[] { "V1.0", "" }));
    FILE_INFO.put("VVC", new StructInfo("VVC ", new String[] { "V1.0" }));
    FILE_INFO.put("WED", new StructInfo("WED ", new String[] { "V1.3" }));
    FILE_INFO.put("WMP", new StructInfo("WMAP", new String[] { "V1.0" }));
  }

  private final ChildFrame resultFrame = new ChildFrame("Corrupted files found", true);
  private final JButton bopen = new JButton("Open", Icons.ICON_OPEN_16.getIcon());
  private final JButton bopennew = new JButton("Open in new window", Icons.ICON_OPEN_16.getIcon());
  private final JButton bsave = new JButton("Save...", Icons.ICON_SAVE_16.getIcon());

  /** List of the {@link Corruption} objects. */
  private final SortableTable table;

  public StructChecker() {
    super("Find Corrupted Files", FILETYPES);

    table = new SortableTable(new String[] { "File", "Offset", "Error message" },
        new Class<?>[] { ResourceEntry.class, String.class, String.class }, // TODO: replace "Offset" by Integer
        new Integer[] { 50, 50, 400 });
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == bopen) {
      int row = table.getSelectedRow();
      if (row != -1) {
        showInViewer((Corruption) table.getTableItemAt(row), false);
      }
    } else if (event.getSource() == bopennew) {
      int row = table.getSelectedRow();
      if (row != -1) {
        showInViewer((Corruption) table.getTableItemAt(row), true);
      }
    } else if (event.getSource() == bsave) {
      table.saveCheckResult(resultFrame, "Corrupted files");
    } else {
      super.actionPerformed(event);
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent event) {
    bopen.setEnabled(true);
    bopennew.setEnabled(true);
  }

  // --------------------- End Interface ListSelectionListener ---------------------

  // --------------------- Begin Interface Runnable ---------------------

  @Override
  public void run() {
    if (runCheck(getFiles())) {
      resultFrame.close();
      return;
    }

    if (table.getRowCount() == 0) {
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No errors found", "Info",
          JOptionPane.INFORMATION_MESSAGE);
    } else {
      table.tableComplete();
      resultFrame.setIconImage(Icons.ICON_REFRESH_16.getIcon().getImage());
      JLabel count = new JLabel(table.getRowCount() + " error(s) found", SwingConstants.CENTER);
      count.setFont(count.getFont().deriveFont(count.getFont().getSize() + 2.0f));
      bopen.setMnemonic('o');
      bopennew.setMnemonic('n');
      bsave.setMnemonic('s');
      resultFrame.getRootPane().setDefaultButton(bopennew);
      JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      panel.add(bopen);
      panel.add(bopennew);
      panel.add(bsave);
      JScrollPane scrollTable = new JScrollPane(table);
      scrollTable.getViewport().setBackground(table.getBackground());
      JPanel pane = (JPanel) resultFrame.getContentPane();
      pane.setLayout(new BorderLayout(0, 3));
      pane.add(count, BorderLayout.NORTH);
      pane.add(scrollTable, BorderLayout.CENTER);
      pane.add(panel, BorderLayout.SOUTH);
      bopen.setEnabled(false);
      bopennew.setEnabled(false);
      table.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getOptions().getScriptFont()));
      table.setRowHeight(table.getFontMetrics(table.getFont()).getHeight() + 1);
      table.getSelectionModel().addListSelectionListener(this);
      table.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseReleased(MouseEvent event) {
          if (event.getClickCount() == 2) {
            final int row = table.getSelectedRow();
            if (row != -1) {
              showInViewer((Corruption) table.getTableItemAt(row), true);
            }
          }
        }
      });
      bopen.addActionListener(this);
      bopennew.addActionListener(this);
      bsave.addActionListener(this);
      pane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      resultFrame.setSize(700, 600);
      Center.center(resultFrame, NearInfinity.getInstance().getBounds());
      resultFrame.setVisible(true);
    }
  }

  // --------------------- End Interface Runnable ---------------------

  @Override
  protected Runnable newWorker(ResourceEntry entry) {
    return () -> {
      final Resource resource = ResourceFactory.getResource(entry);
      if (resource instanceof AbstractStruct) {
        search(entry, (AbstractStruct) resource);
      }
      advanceProgress();
    };
  }

  private void search(ResourceEntry entry, AbstractStruct struct) {
    final List<StructEntry> flatList = struct.getFlatFields();
    if (flatList.size() < 2) {
      return;
    }

    StructEntry entry1 = flatList.get(0);
    int offset = entry1.getOffset() + entry1.getSize();
    for (int i = 1; i < flatList.size(); i++) {
      StructEntry entry2 = flatList.get(i);
      if (!entry2.getName().equals(AbstractStruct.COMMON_UNUSED_BYTES)) {
        int delta = entry2.getOffset() - offset;
        if (entry2.getSize() > 0 && delta < 0) {
          synchronized (table) {
            table.addTableItem(new Corruption(entry, entry1.getOffset(),
                entry1.getName() + '(' + Integer.toHexString(entry1.getOffset()) + "h)" + " overlaps "
                    + entry2.getName() + '(' + Integer.toHexString(entry2.getOffset()) + "h)" + " by " + -delta
                    + " bytes"));
          }
        } else if (delta > 0) {
          synchronized (table) {
            table.addTableItem(new Corruption(entry, entry1.getOffset(),
                delta + " unused bytes between " + entry1.getName() + '(' + Integer.toHexString(entry1.getOffset())
                    + "h)" + " and " + entry2.getName() + '(' + Integer.toHexString(entry2.getOffset()) + "h)"));
          }
        }
        // Using max() as shared data regions may confuse the consistency check algorithm
        offset = Math.max(offset, entry2.getOffset() + entry2.getSize());
        entry1 = entry2;
      }
    }
    StructEntry last = flatList.get(flatList.size() - 1);
    if (last.getName().equals(AbstractStruct.COMMON_UNUSED_BYTES)) {
      synchronized (table) {
        table.addTableItem(new Corruption(entry, last.getOffset(), last.getSize() + " unused bytes after "
            + entry1.getName() + '(' + Integer.toHexString(entry1.getOffset()) + "h)"));
      }
    }

    // Checking signature and version fields
    StructInfo info = FILE_INFO.get(entry.getExtension());
    if (info != null) {
      String sig = ((TextString) struct.getAttribute(AbstractStruct.COMMON_SIGNATURE)).toString();
      if (info.isSignature(sig)) {
        String ver = ((TextString) struct.getAttribute(AbstractStruct.COMMON_VERSION)).toString();
        if (!info.isVersion(ver)) {
          // invalid version?
          synchronized (table) {
            table.addTableItem(new Corruption(entry, 4, "Unsupported or invalid version: \"" + ver + "\""));
          }
        }
      } else {
        // invalid signature?
        synchronized (table) {
          table.addTableItem(new Corruption(entry, 0, "Invalid signature: \"" + sig + "\""));
        }
      }
    }

    // Checking for valid substructure offsets and counts
    // calculating size of static portion of the resource data
    final HashSet<Class<? extends StructEntry>> removableSet = new HashSet<>();
    int headerSize = 0;
    if (Profile.hasProperty(Profile.Key.IS_SUPPORTED_CRE_V22) && entry.getExtension().equalsIgnoreCase("CRE")) {
      // special: CRE V2.2 static size cannot be determined dynamically
      final String version = ((TextString) struct.getAttribute(AbstractStruct.COMMON_VERSION)).getText();
      if ("V2.2".equalsIgnoreCase(version)) {
        headerSize = 0x62e;
      }
    }
    if (headerSize == 0) {
      for (final StructEntry field : struct.getFields()) {
        if (field instanceof SectionOffset) {
          final Class<? extends StructEntry> cls = ((SectionOffset) field).getSection();
          removableSet.add(cls);
        }
        if (field instanceof AddRemovable || removableSet.contains(field.getClass())) {
          headerSize = field.getOffset();
          break;
        } else {
          headerSize = field.getOffset() + field.getSize();
        }
      }
    }
    removableSet.clear();

    // CHR offset correction for embedded CRE data
    int ofsOffset = 0;
    if (entry.getExtension().equalsIgnoreCase("CHR")) {
      final StructEntry se = struct.getAttribute(CreResource.CHR_SIGNATURE_2);
      if (se != null) {
        ofsOffset = se.getOffset();
      }
    }

    // checking offsets
    for (final StructEntry field : struct.getFields()) {
      if (field.getOffset() >= headerSize) {
        break;
      }
      if (field instanceof SectionOffset) {
        final SectionOffset so = (SectionOffset) field;
        if (so.getValue() + ofsOffset < headerSize) {
          synchronized (table) {
            table.addTableItem(new Corruption(entry, so.getOffset(),
                "Offset field points to header data (field name: \"" + so.getName() + "\", offset: "
                    + Integer.toHexString(so.getValue()) + "h, header size: "
                    + Integer.toHexString(headerSize - ofsOffset) + "h)"));
          }
        } else if (so.getValue() + ofsOffset > struct.getSize()) {
          synchronized (table) {
            table.addTableItem(new Corruption(entry, so.getOffset(),
                "Offset field value is out of range (field name: \"" + so.getName() + "\", offset: "
                    + Integer.toHexString(so.getValue()) + "h, resource size: "
                    + Integer.toHexString(struct.getSize() - ofsOffset) + "h)"));
          }
        }
      }
    }

    // Type-specific checks
    if (entry.getExtension().equalsIgnoreCase("WED")) {
      List<Corruption> list = getWedCorruption(entry, struct);
      synchronized (table) {
        for (Iterator<Corruption> iter = list.iterator(); iter.hasNext();) {
          table.addTableItem(iter.next());
        }
      }
    } else if (entry.getExtension().equalsIgnoreCase("STO")) {
      List<Corruption> list = getStoCorruption(entry, struct);
      synchronized (table) {
        for (Iterator<Corruption> iter = list.iterator(); iter.hasNext();) {
            table.addTableItem(iter.next());
        }
      }
    }
  }

  // Checking for invalid trigger strings in STO V1.1 resources
  private List<Corruption> getStoCorruption(ResourceEntry entry, AbstractStruct struct) {
    final List<Corruption> list = new ArrayList<>();
    if (entry.getExtension().equalsIgnoreCase("STO")) {
      String version = ((IsTextual) struct.getAttribute(AbstractStruct.COMMON_VERSION)).getText();
      if (version.equalsIgnoreCase("V1.1")) {
        List<StructEntry> itemList = struct.getFields(ItemSale11.class);
        for (int itemIndex = 0, itemCount = itemList.size(); itemIndex < itemCount; itemIndex++) {
          final ItemSale11 item = (ItemSale11) itemList.get(itemIndex);
          final StringRef triggerEntry = (StringRef) item.getAttribute(ItemSale11.STO_SALE_TRIGGER);
          if (triggerEntry.getValue() > 0 && StringTable.isValidStringRef(triggerEntry.getValue())) {
            final String trigger = StringTable.getStringRef(triggerEntry.getValue()).trim();
            if (!trigger.isEmpty()) {
              final Compiler compiler = new Compiler(trigger, ScriptType.TRIGGER);
              compiler.compile();
              for (final ScriptMessage sm : compiler.getErrors()) {
                list.add(new Corruption(entry, triggerEntry.getOffset(), String.format("[ERROR] %s %d - %s: %s",
                    ItemSale11.STO_SALE, itemIndex, ItemSale11.STO_SALE_TRIGGER, sm.getMessage())));
              }
              for (final ScriptMessage sm : compiler.getWarnings()) {
                list.add(new Corruption(entry, triggerEntry.getOffset(), String.format("[WARNING] %s %d - %s: %s",
                    ItemSale11.STO_SALE, itemIndex, ItemSale11.STO_SALE_TRIGGER, sm.getMessage())));
              }
            }
          }
        }
      }
    }
    return list;
  }

  // Checking for WED-specific corruptions
  private List<Corruption> getWedCorruption(ResourceEntry entry, AbstractStruct struct) {
    final List<Corruption> list = new ArrayList<>();
    if (entry.getExtension().equalsIgnoreCase("WED")) {
      final int ovlSize = 0x18; // size of an Overlay structure
      int ovlCount = ((IsNumeric) struct.getAttribute(8, false)).getValue(); // # overlays
      int ovlStartOfs = ((IsNumeric) struct.getAttribute(16, false)).getValue(); // Overlays offset

      for (int ovlIdx = 0; ovlIdx < ovlCount; ovlIdx++) {
        int ovlOfs = ovlStartOfs + ovlIdx * ovlSize;
        Overlay overlay = (Overlay) struct.getAttribute(ovlOfs, false); // Overlay
        if (overlay == null) {
          continue;
        }
        int width = ((IsNumeric) overlay.getAttribute(ovlOfs + 0, false)).getValue();
        int height = ((IsNumeric) overlay.getAttribute(ovlOfs + 2, false)).getValue();
        String tisName = ((IsReference) overlay.getAttribute(ovlOfs + 4, false)).getResourceName();
        int tileStartOfs = ((IsNumeric) overlay.getAttribute(ovlOfs + 16, false)).getValue();
        int indexStartOfs = ((IsNumeric) overlay.getAttribute(ovlOfs + 20, false)).getValue();
        if (tisName == null || tisName.isEmpty() || !ResourceFactory.resourceExists(tisName)) {
          continue;
        }

        // checking Overlay fields
        boolean skip = false;
        if (width <= 0) {
          list.add(new Corruption(entry, ovlOfs + 0, String.format("Overlay %d: Tileset width is <= 0", ovlIdx)));
          skip = true;
        }
        if (height <= 0) {
          list.add(new Corruption(entry, ovlOfs + 2, String.format("Overlay %d: Tileset height is <= 0", ovlIdx)));
          skip = true;
        }
        if ((tileStartOfs <= ovlOfs + ovlCount * ovlSize) || (tileStartOfs >= struct.getSize())) {
          list.add(new Corruption(entry, ovlOfs + 16, String.format("Overlay %d: Tilemap offset is invalid", ovlIdx)));
          skip = true;
        }
        if ((indexStartOfs < ovlOfs + ovlCount * ovlSize) || (indexStartOfs >= struct.getSize())) {
          list.add(new Corruption(entry, ovlOfs + 16,
              String.format("Overlay %d: Tilemap lookup offset is invalid", ovlIdx)));
          skip = true;
        }
        if (skip) {
          continue;
        }

        // Checking Tilemap fields
        ResourceEntry tisResource = ResourceFactory.getResourceEntry(tisName);
        int[] tisInfo; // = {tileCount, tileSize}
        try {
          tisInfo = tisResource.getResourceInfo();
        } catch (Exception e) {
          tisInfo = null;
        }
        if (tisInfo == null || tisInfo.length < 2) {
          continue;
        }
        final int tileSize = 0x0a; // size of a Tilemap structure
        int numTiles = width * height;
        int tileEndOfs = tileStartOfs + numTiles * tileSize;
        int indexEndOfs = indexStartOfs + 2 * numTiles;
        // caching tile maps and tile lookup indices
        final HashMap<Integer, Tilemap> mapTiles = new HashMap<>(numTiles * 3 / 2, 0.8f);
        final HashMap<Integer, Integer> mapIndices = new HashMap<>(numTiles * 3 / 2, 0.8f);
        for (final StructEntry item : overlay.getFields()) {
          int curOfs = item.getOffset();
          if (curOfs >= tileStartOfs && curOfs < tileEndOfs && item instanceof Tilemap) {
            int index = (curOfs - tileStartOfs) / item.getSize();
            mapTiles.put(index, (Tilemap) item);
          } else if (item.getOffset() > indexStartOfs && curOfs < indexEndOfs && item instanceof DecNumber) {
            int index = (curOfs - indexStartOfs) / 2;
            mapIndices.put(index, ((IsNumeric) item).getValue());
          }
        }
        // checking indices
        for (int i = 0; i < numTiles; i++) {
          Tilemap tile = mapTiles.get(Integer.valueOf(i));
          if (tile != null) {
            int tileOfs = tile.getOffset();
            int tileIdx = (tileOfs - tileStartOfs) / tileSize;
            int tileIdxPri = ((IsNumeric) tile.getAttribute(tileOfs + 0, false)).getValue();
            int tileCountPri = ((IsNumeric) tile.getAttribute(tileOfs + 2, false)).getValue();
            int tileIdxSec = ((IsNumeric) tile.getAttribute(tileOfs + 4, false)).getValue();
            IsNumeric tileFlag = (IsNumeric) tile.getAttribute(tileOfs + 6, false);
            int tileFlagValue = tileFlag.getValue();
            for (int j = tileIdxPri, count = tileIdxPri + tileCountPri; j < count; j++) {
              Integer tileLookupIndex = mapIndices.get(Integer.valueOf(j));
              if (tileLookupIndex != null) {
                if (tileLookupIndex >= tisInfo[0]) {
                  list.add(new Corruption(entry, tileOfs + 0,
                      String.format("Overlay %d/Tilemap %d: Primary tile index %d " + "out of range [0..%d]", ovlIdx,
                          tileIdx, j, tisInfo[0] - 1)));
                }
              }
            }
            if (tileFlagValue > 0 && tileIdxSec >= tisInfo[0]) {
              list.add(new Corruption(entry, tileOfs + 4,
                  String.format("Overlay %d/Tilemap %d: Secondary tile index %d " + "out of range [0..%d]", ovlIdx,
                      tileIdx, tileIdxSec, tisInfo[0] - 1)));
            }
          }
        }
      }
    }
    return list;
  }

  /**
   * Opens a view of the referenced resources and selects the field at the offset in question.
   *
   * @param corruption {@link Corruption} instance with error information.
   * @param newWindow  Whether to open the resource in a new window.
   */
  private void showInViewer(Corruption corruption, boolean newWindow) {
    if (corruption == null) {
      return;
    }

    if (newWindow) {
      final ResourceEntry entry = corruption.getResourceEntry();
      final Resource res = ResourceFactory.getResource(entry);
      final int offset = corruption.getOffset();
      new ViewFrame(resultFrame, res);
      if (res instanceof AbstractStruct) {
        try {
          ((AbstractStruct) res).getViewer().selectEntry(offset);
        } catch (Exception e) {
          Logger.error(e);
        }
      }
    } else {
      final ResourceEntry entry = corruption.getResourceEntry();
      final int offset = corruption.getOffset();
      NearInfinity.getInstance().showResourceEntry(entry);
      if (parent instanceof ViewFrame && parent.isVisible()) {
        final Resource res = ResourceFactory.getResource(entry);
        ((ViewFrame) parent).setViewable(res);
        if (res instanceof AbstractStruct) {
          try {
            ((AbstractStruct) res).getViewer().selectEntry(offset);
          } catch (Exception e) {
            Logger.error(e);
          }
        }
      } else {
        NearInfinity.getInstance().showResourceEntry(entry, () -> {
          final Viewable viewable = NearInfinity.getInstance().getViewable();
          if (viewable instanceof AbstractStruct) {
            try {
              ((AbstractStruct) viewable).getViewer().selectEntry(offset);
            } catch (Exception e) {
              Logger.error(e);
            }
          }
        });
      }
    }
  }

  // -------------------------- INNER CLASSES --------------------------

  private static final class Corruption implements TableItem {
    private final ResourceEntry resourceEntry;
    private final int offset;
    private final String offsetString;
    private final String errorMsg;

    private Corruption(ResourceEntry resourceEntry, int offset, String errorMsg) {
      this.resourceEntry = resourceEntry;
      this.offset= offset;
      this.offsetString = Integer.toHexString(offset) + 'h';
      this.errorMsg = errorMsg;
    }

    @Override
    public Object getObjectAt(int columnIndex) {
      if (columnIndex == 0) {
        return resourceEntry;
      } else if (columnIndex == 1) {
        return offsetString;
      } else {
        return errorMsg;
      }
    }

    public ResourceEntry getResourceEntry() {
      return resourceEntry;
    }

    public int getOffset() {
      return offset;
    }

    @SuppressWarnings("unused")
    public String getMessage() {
      return errorMsg;
    }

    @Override
    public String toString() {
      return "File: " + resourceEntry.getResourceName() + ", Offset: " + offsetString + ", Error: " + errorMsg;
    }
  }

  /** Stores supported signature and versions for a single structured resource format. */
  private static final class StructInfo {
    public final String signature;
    public final String[] version;

    public StructInfo(String sig, String[] ver) {
      signature = (sig != null) ? sig : "";
      if (ver != null) {
        version = new String[ver.length];
        for (int i = 0; i < version.length; i++) {
          version[i] = (ver[i] != null) ? ver[i] : "";
        }
      } else {
        version = new String[0];
      }
    }

    /** Returns whether the signatures matches the signature of the current structure definition. */
    public boolean isSignature(String sig) {
      return (sig != null) ? signature.equals(sig) : false;
    }

    /** Returns whether the specified version is supported by the current structure definition. */
    public boolean isVersion(String ver) {
      if (ver != null) {
        for (final String v : version) {
          if (ver.equals(v)) {
            return true;
          }
        }
      }
      return false;
    }
  }
}
