// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.check;

import infinity.NearInfinity;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.ResourceRef;
import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.datatype.TextString;
import infinity.gui.BrowserMenuBar;
import infinity.gui.Center;
import infinity.gui.ChildFrame;
import infinity.gui.SortableTable;
import infinity.gui.TableItem;
import infinity.gui.ViewFrame;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.Profile;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.key.ResourceEntry;
import infinity.resource.wed.Overlay;
import infinity.resource.wed.Tilemap;
import infinity.util.Debugging;
import infinity.util.Misc;
import infinity.util.io.FileNI;
import infinity.util.io.FileWriterNI;
import infinity.util.io.PrintWriterNI;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ProgressMonitor;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public final class StructChecker extends ChildFrame implements ActionListener, Runnable,
                                                               ListSelectionListener
{
  private static final String FMT_PROGRESS = "Checking %ss...";
  private static final String[] FILETYPES = {"ARE", "CHR", "CHU", "CRE", "DLG", "EFF", "GAM", "ITM",
                                             "PRO", "SPL", "STO", "VEF", "VVC", "WED", "WMP"};
  private static final HashMap<String, StructInfo> fileInfo = new HashMap<String, StructInfo>();
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
    fileInfo.put("VEF", new StructInfo("VEF ", new String[]{"V1.0"}));
    fileInfo.put("VVC", new StructInfo("VVC ", new String[]{"V1.0"}));
    fileInfo.put("WED", new StructInfo("WED ", new String[]{"V1.3"}));
    fileInfo.put("WMP", new StructInfo("WMAP", new String[]{"V1.0"}));
  }

  private final ChildFrame resultFrame = new ChildFrame("Corrupted files found", true);
  private final JButton bstart = new JButton("Check", Icons.getIcon("Find16.gif"));
  private final JButton bcancel = new JButton("Cancel", Icons.getIcon("Delete16.gif"));
  private final JButton binvert = new JButton("Invert", Icons.getIcon("Refresh16.gif"));
  private final JButton bopen = new JButton("Open", Icons.getIcon("Open16.gif"));
  private final JButton bopennew = new JButton("Open in new window", Icons.getIcon("Open16.gif"));
  private final JButton bsave = new JButton("Save...", Icons.getIcon("Save16.gif"));
  private final JCheckBox[] boxes = new JCheckBox[FILETYPES.length];
  private final List<ResourceEntry> files = new ArrayList<ResourceEntry>();
  private final SortableTable table;
  private ProgressMonitor progress;
  private int progressIndex;

  public StructChecker()
  {
    super("Find Corrupted Files");
    setIconImage(Icons.getIcon("Refresh16.gif").getImage());

    List<Class<? extends Object>> colClasses = new ArrayList<Class<? extends Object>>(3);
    colClasses.add(Object.class); colClasses.add(Object.class); colClasses.add(Object.class);
    table = new SortableTable(Arrays.asList(new String[]{"File", "Offset", "Error message"}),
                              colClasses, Arrays.asList(new Integer[]{50, 50, 400}));

    bstart.setMnemonic('s');
    bcancel.setMnemonic('c');
    binvert.setMnemonic('i');
    bstart.addActionListener(this);
    bcancel.addActionListener(this);
    binvert.addActionListener(this);
    getRootPane().setDefaultButton(bstart);

    JPanel boxpanel = new JPanel(new GridLayout(0, 2, 3, 3));
    for (int i = 0; i < boxes.length; i++) {
      boxes[i] = new JCheckBox(FILETYPES[i], true);
      boxpanel.add(boxes[i]);
    }
    boxpanel.setBorder(BorderFactory.createEmptyBorder(3, 12, 3, 0));

    JPanel ipanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    ipanel.add(binvert);
    JPanel innerpanel = new JPanel(new BorderLayout());
    innerpanel.add(boxpanel, BorderLayout.CENTER);
    innerpanel.add(ipanel, BorderLayout.SOUTH);
    innerpanel.setBorder(BorderFactory.createTitledBorder("Select files to check:"));

    JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    bpanel.add(bstart);
    bpanel.add(bcancel);

    JPanel mainpanel = new JPanel(new BorderLayout());
    mainpanel.add(innerpanel, BorderLayout.CENTER);
    mainpanel.add(bpanel, BorderLayout.SOUTH);
    mainpanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

    JPanel pane = (JPanel)getContentPane();
    pane.setLayout(new BorderLayout());
    pane.add(mainpanel, BorderLayout.CENTER);

    pack();
    Center.center(this, NearInfinity.getInstance().getBounds());
    setVisible(true);
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bstart) {
      setVisible(false);
      for (int i = 0; i < FILETYPES.length; i++) {
        if (boxes[i].isSelected())
          files.addAll(ResourceFactory.getResources(FILETYPES[i]));
      }
      if (files.size() > 0)
        new Thread(this).start();
    }
    else if (event.getSource() == binvert) {
      for (final JCheckBox box : boxes)
        box.setSelected(!box.isSelected());
    }
    else if (event.getSource() == bcancel)
      setVisible(false);
    else if (event.getSource() == bopen) {
      int row = table.getSelectedRow();
      if (row != -1) {
        ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
        NearInfinity.getInstance().showResourceEntry(resourceEntry);
//        NearInfinity.getInstance().setViewable(ResourceFactory.getResourceresourceEntry));
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
      JFileChooser chooser = new JFileChooser(Profile.getGameRoot());
      chooser.setDialogTitle("Save result");
      chooser.setSelectedFile(new FileNI("result.txt"));
      if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        File output = chooser.getSelectedFile();
        if (output.exists()) {
          String[] options = {"Overwrite", "Cancel"};
          if (JOptionPane.showOptionDialog(this, output + " exists. Overwrite?",
                                           "Save result", JOptionPane.YES_NO_OPTION,
                                           JOptionPane.WARNING_MESSAGE, null, options, options[0]) != 0)
            return;
        }
        try {
          PrintWriter pw = new PrintWriterNI(new BufferedWriter(new FileWriterNI(output)));
          pw.println("File corruption search");
          pw.println("Number of errors: " + table.getRowCount());
          for (int i = 0; i < table.getRowCount(); i++)
            pw.println(table.getTableItemAt(i).toString());
          pw.close();
          JOptionPane.showMessageDialog(this, "Result saved to " + output, "Save complete",
                                        JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
          JOptionPane.showMessageDialog(this, "Error while saving " + output,
                                        "Error", JOptionPane.ERROR_MESSAGE);
          e.printStackTrace();
        }
      }
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
    try {
      String type = "WWWW";
      progressIndex = 0;
      progress = new ProgressMonitor(NearInfinity.getInstance(), "Checking...",
                                     String.format(FMT_PROGRESS, type),
                                     0, files.size());
      progress.setMillisToDecideToPopup(100);
      ThreadPoolExecutor executor = Misc.createThreadPool();
      boolean isCancelled = false;
      Debugging.timerReset();
      for (int i = 0; i < files.size(); i++) {
        ResourceEntry entry = files.get(i);
        if (i % 10 == 0) {
          String ext = entry.getExtension();
          if (ext != null && !type.equalsIgnoreCase(ext)) {
            type = ext;
            progress.setNote(String.format(FMT_PROGRESS, type));
          }
        }
        Misc.isQueueReady(executor, true, -1);
        executor.execute(new Worker(entry));
        if (progress.isCanceled()) {
          isCancelled = true;
          break;
        }
      }

      // enforcing thread termination if process has been cancelled
      if (isCancelled) {
        executor.shutdownNow();
      } else {
        executor.shutdown();
      }

      // waiting for pending threads to terminate
      while (!executor.isTerminated()) {
        if (!isCancelled && progress.isCanceled()) {
          executor.shutdownNow();
          isCancelled = true;
        }
        try { Thread.sleep(1); } catch (InterruptedException e) {}
      }

      if (isCancelled) {
        resultFrame.close();
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Check canceled", "Info",
                                      JOptionPane.INFORMATION_MESSAGE);
        return;
      }

      if (table.getRowCount() == 0) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No errors found",
                                      "Info", JOptionPane.INFORMATION_MESSAGE);
      } else {
        table.tableComplete();
        resultFrame.setIconImage(Icons.getIcon("Refresh16.gif").getImage());
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
        table.setFont(BrowserMenuBar.getInstance().getScriptFont());
        table.getSelectionModel().addListSelectionListener(this);
        table.addMouseListener(new MouseAdapter()
        {
          @Override
          public void mouseReleased(MouseEvent event)
          {
            if (event.getClickCount() == 2) {
              int row = table.getSelectedRow();
              if (row != -1) {
                ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
                Resource resource = ResourceFactory.getResource(resourceEntry);
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
    } finally {
      advanceProgress(true);
    }
    Debugging.timerShow("Check completed", Debugging.TimeFormat.MILLISECONDS);
  }

// --------------------- End Interface Runnable ---------------------

  private void search(ResourceEntry entry, AbstractStruct struct)
  {
    List<StructEntry> flatList = struct.getFlatList();
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
    List<Corruption> list = new ArrayList<Corruption>();
    if (entry.getExtension().equalsIgnoreCase("WED")) {
      final int ovlSize = 0x18; // size of an Overlay structure
      int ovlCount = ((SectionCount)struct.getAttribute(8, false)).getValue(); // # overlays
      int ovlStartOfs = ((SectionOffset)struct.getAttribute(16, false)).getValue();  // Overlays offset

      for (int ovlIdx = 0; ovlIdx < ovlCount; ovlIdx++) {
        int ovlOfs = ovlStartOfs + ovlIdx*ovlSize;
        Overlay overlay = (Overlay)struct.getAttribute(ovlOfs, false);  // Overlay
        if (overlay == null) {
          continue;
        }
        int width = ((DecNumber)overlay.getAttribute(ovlOfs + 0, false)).getValue();
        int height = ((DecNumber)overlay.getAttribute(ovlOfs + 2, false)).getValue();
        String tisName = ((ResourceRef)overlay.getAttribute(ovlOfs + 4, false)).getResourceName();
        int tileStartOfs = ((SectionOffset)overlay.getAttribute(ovlOfs + 16, false)).getValue();
        int indexStartOfs = ((SectionOffset)overlay.getAttribute(ovlOfs + 20, false)).getValue();
        if (tisName == null || tisName.isEmpty() || !ResourceFactory.resourceExists(tisName)) {
          continue;
        }

        // checking Overlay fields
        boolean skip = false;
        if (width <= 0) {
          list.add(new Corruption(entry, ovlOfs + 0,
                                  String.format("Overlay %1$d: Tileset width is <= 0", ovlIdx)));
          skip = true;
        }
        if (height <= 0) {
          list.add(new Corruption(entry, ovlOfs + 2,
                                  String.format("Overlay %1$d: Tileset height is <= 0", ovlIdx)));
          skip = true;
        }
        if ((tileStartOfs <= ovlOfs + ovlCount*ovlSize) || (tileStartOfs >= struct.getSize())) {
          list.add(new Corruption(entry, ovlOfs + 16,
                                  String.format("Overlay %1$d: Tilemap offset is invalid", ovlIdx)));
          skip = true;
        }
        if ((indexStartOfs < ovlOfs + ovlCount*ovlSize) || (indexStartOfs >= struct.getSize())) {
          list.add(new Corruption(entry, ovlOfs + 16,
                                  String.format("Overlay %1$d: Tilemap lookup offset is invalid", ovlIdx)));
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
        HashMap<Integer, Tilemap> mapTiles = new HashMap<Integer, Tilemap>(numTiles*3/2, 0.8f);
        HashMap<Integer, Integer> mapIndices = new HashMap<Integer, Integer>(numTiles*3/2, 0.8f);
        for (Iterator<StructEntry> iter = overlay.getList().iterator(); iter.hasNext();) {
          StructEntry item = iter.next();
          int curOfs = item.getOffset();
          if (curOfs >= tileStartOfs && curOfs < tileEndOfs && item instanceof Tilemap) {
            int index = (curOfs - tileStartOfs) / item.getSize();
            mapTiles.put(Integer.valueOf(index), (Tilemap)item);
          } else if (item.getOffset() > indexStartOfs && curOfs < indexEndOfs && item instanceof DecNumber) {
            int index = (curOfs - indexStartOfs) / 2;
            mapIndices.put(Integer.valueOf(index), Integer.valueOf(((DecNumber)item).getValue()));
          }
        }
        // checking indices
        for (int i = 0; i < numTiles; i++) {
          Tilemap tile = mapTiles.get(Integer.valueOf(i));
          if (tile != null) {
            int tileOfs = tile.getOffset();
            int tileIdx = (tileOfs - tileStartOfs) / tileSize;
            int tileIdxPri = ((DecNumber)tile.getAttribute(tileOfs + 0, false)).getValue();
            int tileCountPri = ((DecNumber)tile.getAttribute(tileOfs + 2, false)).getValue();
            int tileIdxSec = ((DecNumber)tile.getAttribute(tileOfs + 4, false)).getValue();
            Flag tileFlag = (Flag)tile.getAttribute(tileOfs + 6, false);
            int tileFlagValue = (int)tileFlag.getValue();
            for (int j = tileIdxPri, count = tileIdxPri + tileCountPri; j < count; j++) {
              Integer tileLookupIndex = mapIndices.get(Integer.valueOf(j));
              if (tileLookupIndex != null) {
                if (tileLookupIndex >= tisInfo[0]) {
                  list.add(new Corruption(entry, tileOfs + 0,
                                          String.format("Overlay %1$d/Tilemap %2$d: Primary tile index %3$d " +
                                                        "out of range [0..%4$d]",
                                                        ovlIdx, tileIdx, j, tisInfo[0] - 1)));
                }
              }
            }
            if (tileFlagValue > 0 && tileIdxSec >= tisInfo[0]) {
              list.add(new Corruption(entry, tileOfs + 4,
                                      String.format("Overlay %1$d/Tilemap %2$d: Secondary tile index %3$d " +
                                                    "out of range [0..%4$d]",
                                                    ovlIdx, tileIdx, tileIdxSec, tisInfo[0] - 1)));
            }
          }
        }
      }
    }
    return list;
  }

  private synchronized void advanceProgress(boolean finished)
  {
    if (progress != null) {
      if (finished) {
        progressIndex = 0;
        progress.close();
        progress = null;
      } else {
        progressIndex++;
        progress.setProgress(progressIndex);
      }
    }
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
      StringBuffer buf = new StringBuffer("File: ");
      buf.append(resourceEntry.toString());
      buf.append("  Offset: ").append(offset);
      buf.append("  Error message: ").append(errorMsg);
      return buf.toString();
    }
  }

  // Stores supported signature and versions for a single structured resource format
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

  private class Worker implements Runnable
  {
    private final ResourceEntry entry;

    public Worker(ResourceEntry entry)
    {
      this.entry = entry;
    }

    @Override
    public void run()
    {
      if (entry != null) {
        Resource resource = ResourceFactory.getResource(entry);
        if (resource != null) {
          search(entry, (AbstractStruct)resource);
        }
      }
      advanceProgress(false);
    }
  }
}
