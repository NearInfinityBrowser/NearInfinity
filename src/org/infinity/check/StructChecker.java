// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.check;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.NearInfinity;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.IsReference;
import org.infinity.datatype.TextString;
import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.SortableTable;
import org.infinity.gui.TableItem;
import org.infinity.gui.ViewFrame;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.wed.Overlay;
import org.infinity.resource.wed.Tilemap;
import org.infinity.util.Misc;

public final class StructChecker extends AbstractChecker implements ListSelectionListener
{
  private static final String[] FILETYPES = {"ARE", "CHR", "CHU", "CRE", "DLG", "EFF", "GAM", "ITM",
                                             "PRO", "SPL", "STO", "VEF", "VVC", "WED", "WMP"};
  private static final HashMap<String, StructInfo> fileInfo = new HashMap<>();
  static {
    fileInfo.put("ARE", new StructInfo("AREA", new String[]{"V1.0", "V9.1"}));
    fileInfo.put("CHR", new StructInfo("CHR ", new String[]{"V1.0", "V1.2", "V2.0", "V2.1", "V2.2", "V9.0"}));
    fileInfo.put("CHU", new StructInfo("CHUI", new String[]{"V1  "}));
    fileInfo.put("CRE", new StructInfo("CRE ", new String[]{"V1.0", "V1.1", "V1.2", "V2.2", "V9.0"}));
    fileInfo.put("DLG", new StructInfo("DLG ", new String[]{"V1.0"}));
    fileInfo.put("EFF", new StructInfo("EFF ", new String[]{"V2.0"}));
    fileInfo.put("GAM", new StructInfo("GAME", new String[]{"V1.1", "V2.0", "V2.1", "V2.2"}));
    fileInfo.put("ITM", new StructInfo("ITM ", new String[]{"V1  ", "V1.1", "V2.0"}));
    fileInfo.put("PRO", new StructInfo("PRO ", new String[]{"V1.0"}));
    fileInfo.put("SPL", new StructInfo("SPL ", new String[]{"V1  ", "V2.0"}));
    fileInfo.put("STO", new StructInfo("STOR", new String[]{"V1.0", "V1.1", "V9.0"}));
    fileInfo.put("VEF", new StructInfo("VEF ", new String[]{"V1.0", ""}));
    fileInfo.put("VVC", new StructInfo("VVC ", new String[]{"V1.0"}));
    fileInfo.put("WED", new StructInfo("WED ", new String[]{"V1.3"}));
    fileInfo.put("WMP", new StructInfo("WMAP", new String[]{"V1.0"}));
  }

  private final ChildFrame resultFrame = new ChildFrame("Corrupted files found", true);
  private final JButton bopen = new JButton("Open", Icons.getIcon(Icons.ICON_OPEN_16));
  private final JButton bopennew = new JButton("Open in new window", Icons.getIcon(Icons.ICON_OPEN_16));
  private final JButton bsave = new JButton("Save...", Icons.getIcon(Icons.ICON_SAVE_16));
  /** List of the {@link Corruption} objects. */
  private final SortableTable table;

  public StructChecker()
  {
    super("Find Corrupted Files", "StructChecker", FILETYPES);

    table = new SortableTable(new String[]{"File", "Offset", "Error message"},
                              new Class<?>[]{ResourceEntry.class, String.class, String.class},//TODO: replace "Offset" by Integer
                              new Integer[]{50, 50, 400});
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bopen) {
      int row = table.getSelectedRow();
      if (row != -1) {
        ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
        NearInfinity.getInstance().showResourceEntry(resourceEntry);
      }
    }
    else if (event.getSource() == bopennew) {
      int row = table.getSelectedRow();
      if (row != -1) {
        ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
        new ViewFrame(resultFrame, ResourceFactory.getResource(resourceEntry));
      }
    }
    else if (event.getSource() == bsave) {
      table.saveCheckResult(resultFrame, "Corrupted files");
    } else {
      super.actionPerformed(event);
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent event)
  {
    bopen.setEnabled(true);
    bopennew.setEnabled(true);
  }

// --------------------- End Interface ListSelectionListener ---------------------


// --------------------- Begin Interface Runnable ---------------------

  @Override
  public void run()
  {
    if (runCheck(files)) {
      resultFrame.close();
      return;
    }

    if (table.getRowCount() == 0) {
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No errors found",
                                    "Info", JOptionPane.INFORMATION_MESSAGE);
    } else {
      table.tableComplete();
      resultFrame.setIconImage(Icons.getIcon(Icons.ICON_REFRESH_16).getImage());
      JLabel count = new JLabel(table.getRowCount() + " error(s) found", JLabel.CENTER);
      count.setFont(count.getFont().deriveFont((float)count.getFont().getSize() + 2.0f));
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
      JPanel pane = (JPanel)resultFrame.getContentPane();
      pane.setLayout(new BorderLayout(0, 3));
      pane.add(count, BorderLayout.NORTH);
      pane.add(scrollTable, BorderLayout.CENTER);
      pane.add(panel, BorderLayout.SOUTH);
      bopen.setEnabled(false);
      bopennew.setEnabled(false);
      table.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getScriptFont()));
      table.setRowHeight(table.getFontMetrics(table.getFont()).getHeight() + 1);
      table.getSelectionModel().addListSelectionListener(this);
      table.addMouseListener(new MouseAdapter()
      {
        @Override
        public void mouseReleased(MouseEvent event)
        {
          if (event.getClickCount() == 2) {
            final int row = table.getSelectedRow();
            if (row != -1) {
              final ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
              final Resource resource = ResourceFactory.getResource(resourceEntry);
              new ViewFrame(resultFrame, resource);
              ((AbstractStruct)resource).getViewer().selectEntry((String)table.getValueAt(row, 1));
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
  protected Runnable newWorker(ResourceEntry entry)
  {
    return () -> {
      final Resource resource = ResourceFactory.getResource(entry);
      if (resource instanceof AbstractStruct) {
        search(entry, (AbstractStruct)resource);
      }
      advanceProgress();
    };
  }

  private void search(ResourceEntry entry, AbstractStruct struct)
  {
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
                                              entry1.getName() + '(' + Integer.toHexString(entry1.getOffset()) +
                                              "h)" +
                                              " overlaps " +
                                              entry2.getName() + '(' + Integer.toHexString(entry2.getOffset()) +
                                              "h)" +
                                              " by " + -delta + " bytes"));
          }
        } else if (delta > 0) {
          synchronized (table) {
            table.addTableItem(new Corruption(entry, entry1.getOffset(),
                                              delta + " unused bytes between " +
                                              entry1.getName() + '(' + Integer.toHexString(entry1.getOffset()) +
                                              "h)" +
                                              " and " +
                                              entry2.getName() + '(' + Integer.toHexString(entry2.getOffset()) +
                                              "h)"));
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
        table.addTableItem(new Corruption(entry, last.getOffset(),
                                          last.getSize() + " unused bytes after " +
                                          entry1.getName() + '(' + Integer.toHexString(entry1.getOffset()) +
                                          "h)"));
      }
    }

    // Checking signature and version fields
    StructInfo info = fileInfo.get(entry.getExtension());
    if (info != null) {
      String sig = ((TextString)struct.getAttribute(AbstractStruct.COMMON_SIGNATURE)).toString();
      if (info.isSignature(sig)) {
        String ver = ((TextString)struct.getAttribute(AbstractStruct.COMMON_VERSION)).toString();
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

    // Type-specific checks
    if (entry.getExtension().equalsIgnoreCase("WED")) {
      List<Corruption> list = getWedCorruption(entry, struct);
      for (Iterator<Corruption> iter = list.iterator(); iter.hasNext();) {
        synchronized (table) {
          table.addTableItem(iter.next());
        }
      }
    }
  }

  // Checking for WED-specific corruptions
  private List<Corruption> getWedCorruption(ResourceEntry entry, AbstractStruct struct)
  {
    final List<Corruption> list = new ArrayList<>();
    if (entry.getExtension().equalsIgnoreCase("WED")) {
      final int ovlSize = 0x18; // size of an Overlay structure
      int ovlCount = ((IsNumeric)struct.getAttribute(8, false)).getValue(); // # overlays
      int ovlStartOfs = ((IsNumeric)struct.getAttribute(16, false)).getValue();  // Overlays offset

      for (int ovlIdx = 0; ovlIdx < ovlCount; ovlIdx++) {
        int ovlOfs = ovlStartOfs + ovlIdx*ovlSize;
        Overlay overlay = (Overlay)struct.getAttribute(ovlOfs, false);  // Overlay
        if (overlay == null) {
          continue;
        }
        int width = ((IsNumeric)overlay.getAttribute(ovlOfs + 0, false)).getValue();
        int height = ((IsNumeric)overlay.getAttribute(ovlOfs + 2, false)).getValue();
        String tisName = ((IsReference)overlay.getAttribute(ovlOfs + 4, false)).getResourceName();
        int tileStartOfs = ((IsNumeric)overlay.getAttribute(ovlOfs + 16, false)).getValue();
        int indexStartOfs = ((IsNumeric)overlay.getAttribute(ovlOfs + 20, false)).getValue();
        if (tisName == null || tisName.isEmpty() || !ResourceFactory.resourceExists(tisName)) {
          continue;
        }

        // checking Overlay fields
        boolean skip = false;
        if (width <= 0) {
          list.add(new Corruption(entry, ovlOfs + 0,
                                  String.format("Overlay %d: Tileset width is <= 0", ovlIdx)));
          skip = true;
        }
        if (height <= 0) {
          list.add(new Corruption(entry, ovlOfs + 2,
                                  String.format("Overlay %d: Tileset height is <= 0", ovlIdx)));
          skip = true;
        }
        if ((tileStartOfs <= ovlOfs + ovlCount*ovlSize) || (tileStartOfs >= struct.getSize())) {
          list.add(new Corruption(entry, ovlOfs + 16,
                                  String.format("Overlay %d: Tilemap offset is invalid", ovlIdx)));
          skip = true;
        }
        if ((indexStartOfs < ovlOfs + ovlCount*ovlSize) || (indexStartOfs >= struct.getSize())) {
          list.add(new Corruption(entry, ovlOfs + 16,
                                  String.format("Overlay %d: Tilemap lookup offset is invalid", ovlIdx)));
          skip = true;
        }
        if (skip) {
          continue;
        }

        // Checking Tilemap fields
        ResourceEntry tisResource = ResourceFactory.getResourceEntry(tisName);
        int[] tisInfo;  // = {tileCount, tileSize}
        try {
          tisInfo = tisResource.getResourceInfo();
        } catch (Exception e) {
          tisInfo = null;
        }
        if (tisInfo == null || tisInfo.length < 2) {
          continue;
        }
        final int tileSize = 0x0a;  // size of a Tilemap structure
        int numTiles = width*height;
        int tileEndOfs = tileStartOfs + numTiles*tileSize;
        int indexEndOfs = indexStartOfs + 2*numTiles;
        // caching tile maps and tile lookup indices
        final HashMap<Integer, Tilemap> mapTiles = new HashMap<>(numTiles*3/2, 0.8f);
        final HashMap<Integer, Integer> mapIndices = new HashMap<>(numTiles*3/2, 0.8f);
        for (final StructEntry item : overlay.getFields()) {
          int curOfs = item.getOffset();
          if (curOfs >= tileStartOfs && curOfs < tileEndOfs && item instanceof Tilemap) {
            int index = (curOfs - tileStartOfs) / item.getSize();
            mapTiles.put(Integer.valueOf(index), (Tilemap)item);
          } else if (item.getOffset() > indexStartOfs && curOfs < indexEndOfs && item instanceof DecNumber) {
            int index = (curOfs - indexStartOfs) / 2;
            mapIndices.put(Integer.valueOf(index), Integer.valueOf(((IsNumeric)item).getValue()));
          }
        }
        // checking indices
        for (int i = 0; i < numTiles; i++) {
          Tilemap tile = mapTiles.get(Integer.valueOf(i));
          if (tile != null) {
            int tileOfs = tile.getOffset();
            int tileIdx = (tileOfs - tileStartOfs) / tileSize;
            int tileIdxPri = ((IsNumeric)tile.getAttribute(tileOfs + 0, false)).getValue();
            int tileCountPri = ((IsNumeric)tile.getAttribute(tileOfs + 2, false)).getValue();
            int tileIdxSec = ((IsNumeric)tile.getAttribute(tileOfs + 4, false)).getValue();
            IsNumeric tileFlag = (IsNumeric)tile.getAttribute(tileOfs + 6, false);
            int tileFlagValue = tileFlag.getValue();
            for (int j = tileIdxPri, count = tileIdxPri + tileCountPri; j < count; j++) {
              Integer tileLookupIndex = mapIndices.get(Integer.valueOf(j));
              if (tileLookupIndex != null) {
                if (tileLookupIndex >= tisInfo[0]) {
                  list.add(new Corruption(entry, tileOfs + 0,
                                          String.format("Overlay %d/Tilemap %d: Primary tile index %d " +
                                                        "out of range [0..%d]",
                                                        ovlIdx, tileIdx, j, tisInfo[0] - 1)));
                }
              }
            }
            if (tileFlagValue > 0 && tileIdxSec >= tisInfo[0]) {
              list.add(new Corruption(entry, tileOfs + 4,
                                      String.format("Overlay %d/Tilemap %d: Secondary tile index %d " +
                                                    "out of range [0..%d]",
                                                    ovlIdx, tileIdx, tileIdxSec, tisInfo[0] - 1)));
            }
          }
        }
      }
    }
    return list;
  }
// -------------------------- INNER CLASSES --------------------------

  private static final class Corruption implements TableItem
  {
    private final ResourceEntry resourceEntry;
    private final String offset;
    private final String errorMsg;

    private Corruption(ResourceEntry resourceEntry, int offset, String errorMsg)
    {
      this.resourceEntry = resourceEntry;
      this.offset = Integer.toHexString(offset) + 'h';
      this.errorMsg = errorMsg;
    }

    @Override
    public Object getObjectAt(int columnIndex)
    {
      if (columnIndex == 0)
        return resourceEntry;
      else if (columnIndex == 1)
        return offset;
      else
        return errorMsg;
    }

    @Override
    public String toString()
    {
      return "File: " + resourceEntry.getResourceName()
           + ", Offset: " + offset
           + ", Error: " + errorMsg;
    }
  }

  /** Stores supported signature and versions for a single structured resource format. */
  private static final class StructInfo
  {
    public final String signature;
    public final String[] version;

    public StructInfo(String sig, String[] ver)
    {
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
    public boolean isSignature(String sig)
    {
      return (sig != null) ? signature.equals(sig) : false;
    }

    /** Returns whether the specified version is supported by the current structure definition. */
    public boolean isVersion(String ver)
    {
      if (ver != null) {
        for (final String v: version) {
          if (ver.equals(v)) {
            return true;
          }
        }
      }
      return false;
    }
  }
}
