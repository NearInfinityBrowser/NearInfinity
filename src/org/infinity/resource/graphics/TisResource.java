// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.RootPaneContainer;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.infinity.NearInfinity;
import org.infinity.datatype.IsNumeric;
import org.infinity.gui.ButtonPanel;
import org.infinity.gui.ButtonPopupMenu;
import org.infinity.gui.TileGrid;
import org.infinity.gui.ViewFrame;
import org.infinity.gui.WindowBlocker;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Closeable;
import org.infinity.resource.Profile;
import org.infinity.resource.Referenceable;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.ViewableContainer;
import org.infinity.resource.graphics.TisConvert.Status;
import org.infinity.resource.key.BIFFResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.wed.IndexNumber;
import org.infinity.resource.wed.Overlay;
import org.infinity.resource.wed.Tilemap;
import org.infinity.resource.wed.WedResource;
import org.infinity.search.ReferenceSearcher;
import org.infinity.util.io.FileEx;

/**
 * This resource describes a tileset. There are currently two variants available:
 * <ol>
 * <li><b>Palette-based TIS</b>
 * <p>
 * TIS files are generally comprised of a large number of tiles, each of which consists of a palette and a rectangular
 * block of pixels. Each pixel is an index into the associated palette. Each tile has its own palette and a block of
 * pixels. The pixel data is not compressed.
 * <p>
 * Each tile consists of a 256 colour palette, with each entry being an RGBA value stored in BGRA order (note that the
 * Alpha value is unused), followed by 8-bit pixel values, which are indices into the palette. The pixel values are row
 * by row, from left to right and top to bottom. Index 0 is hardcoded to be the transparent index.</li>
 * <li><b>PVRZ-based TIS</b>
 * <p>
 * This variant is only supported by {@link Profile.Engine#EE Enhanced Edition} games. Each tile definition refers to a
 * block of pixels within an associated {@link PvrzResource PVRZ} file.
 * <p>
 * Each tile consists of a block of pixels that is defined in an associated PVRZ file.</li>
 * </ol>
 * TIS files contain only the graphics for an area - the location information is stored in a {@link WedResource WED}
 * file.
 * <p>
 * Engine specific notes:
 * <ul>
 * <li>PST can only load TIS files when they are stored in a {@link BIFFResourceEntry BIFF} file.</li>
 * <li>Palette-based TIS induces a noticeable performance hit and occasional visual glitches when used in
 * {@link Profile.Engine#EE Enhanced Edition} games. It is highly recommended to use PVRZ-based TIS instead.</li>
 * </ul>
 *
 * @see <a href="https://gibberlings3.github.io/iesdp/file_formats/ie_formats/tis_v1.htm">
 *      https://gibberlings3.github.io/iesdp/file_formats/ie_formats/tis_v1.htm</a>
 */
public class TisResource implements Resource, Closeable, Referenceable, ActionListener, ChangeListener, ItemListener,
    KeyListener, PropertyChangeListener {
  private static final int DEFAULT_COLUMNS = 5;

  private static final String FMT_TILEINFO_SHOW = "Tile %d: Show PVRZ information...";
  private static final String FMT_TILEINFO_PVRZ = "Tile %d: Open PVRZ resource...";
  private static final String FMT_TILEINFO_WED = "Tile %d: Open WED overlay tilemap... ";

  private static boolean showGrid = false;

  private final ResourceEntry entry;
  private final ButtonPanel buttonPanel = new ButtonPanel();
  private final JPopupMenu menuTileInfo = new JPopupMenu();
  private final JMenuItem miTileInfoShow = new JMenuItem();
  private final JMenuItem miTileInfoPvrz = new JMenuItem();
  private final JMenuItem miTileInfoWed = new JMenuItem();
  private final List<SwingWorker<Status, Void>> workers = new ArrayList<>();

  private WedResource wedResource;
  private HashMap<Integer, Tilemap> wedTileMap;
  private TisDecoder decoder;
  private List<Image> tileImages; // stores one tile per image
  private TileGrid tileGrid;      // the main component for displaying the tileset
  private JSlider slCols;         // changes the tiles per row
  private JTextField tfCols;      // input/output tiles per row
  private JCheckBox cbGrid;       // show/hide frame around each tile
  private JMenuItem miExport;
  private JMenuItem miExportPaletteTis;
  private JMenuItem miExportPvrzTis;
  private JMenuItem miExportPNG;
  private JPanel panel; // top-level panel of the viewer
  private RootPaneContainer rpc;
  private WindowBlocker blocker;
  private int defaultWidth;
  private int lastTileInfoIndex = -1;

  public TisResource(ResourceEntry entry) throws Exception {
    this.entry = entry;
    initTileset();
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == buttonPanel.getControlByType(ButtonPanel.Control.FIND_REFERENCES)) {
      searchReferences(panel.getTopLevelAncestor());
    } else if (event.getSource() == miExport) {
      ResourceFactory.exportResource(entry, panel.getTopLevelAncestor());
    } else if (event.getSource() == miExportPaletteTis) {
      final Path tisFile = getTisFileName(panel.getTopLevelAncestor(), false);
      if (tisFile != null) {
        performBackgroundTask(() -> TisConvert.convertToPaletteTis(tileImages, decoder, tisFile, true,
            panel.getTopLevelAncestor()));
      }
    } else if (event.getSource() == miExportPvrzTis) {
      final Path tisFile = getTisFileName(panel.getTopLevelAncestor(), true);
      if (tisFile != null) {
        performBackgroundTask(() -> TisConvert.convertToPvrzTis(decoder, tisFile, true, panel.getTopLevelAncestor()));
      }
    } else if (event.getSource() == miExportPNG) {
      final Path pngFile = getPngFileName(panel.getTopLevelAncestor());
      if (pngFile != null) {
        performBackgroundTask(() -> TisConvert.exportPNG(tileImages, tileGrid.getTileColumns(), pngFile, true,
            panel.getTopLevelAncestor()));
      }
    } else if (event.getSource() == miTileInfoShow) {
      if (!showPvrzInfo(lastTileInfoIndex)) {
        JOptionPane.showMessageDialog(panel,
            String.format("Could not retrieve PVRZ information for tile %d.", lastTileInfoIndex), "Error",
            JOptionPane.ERROR_MESSAGE);
      }
    } else if (event.getSource() == miTileInfoPvrz) {
      if (!openPvrzResource(lastTileInfoIndex)) {
        JOptionPane.showMessageDialog(panel,
            String.format("Could not open PVRZ resource for tile %d.", lastTileInfoIndex), "Error",
            JOptionPane.ERROR_MESSAGE);
      }
    } else if (event.getSource() == miTileInfoWed) {
      final Tilemap tm = wedTileMap.get(lastTileInfoIndex);
      if (!openStructEntry(tm)) {
        JOptionPane.showMessageDialog(panel,
            String.format("Could not open overlay structure for tile %d.", lastTileInfoIndex), "Error",
            JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event) {
    if (event.getSource() == slCols) {
      int cols = slCols.getValue();
      tfCols.setText(Integer.toString(cols));
      tileGrid.setGridSize(calcGridSize(tileGrid.getImageCount(), cols));
    }
  }

  // --------------------- End Interface ChangeListener ---------------------

  // --------------------- Begin Interface ItemListener ---------------------

  @Override
  public void itemStateChanged(ItemEvent event) {
    if (event.getSource() == cbGrid) {
      showGrid = cbGrid.isSelected();
      tileGrid.setShowGrid(showGrid);
    }
  }

  // --------------------- End Interface ChangeListener ---------------------

  // --------------------- Begin Interface KeyListener ---------------------

  @Override
  public void keyPressed(KeyEvent event) {
    if (event.getSource() == tfCols) {
      if (event.getKeyCode() == KeyEvent.VK_ENTER) {
        int cols;
        try {
          cols = Integer.parseInt(tfCols.getText());
        } catch (NumberFormatException e) {
          cols = slCols.getValue();
          tfCols.setText(Integer.toString(slCols.getValue()));
        }
        if (cols != slCols.getValue()) {
          if (cols <= 0) {
            cols = 1;
          }
          if (cols >= decoder.getTileCount()) {
            cols = decoder.getTileCount();
          }
          slCols.setValue(cols);
          tfCols.setText(Integer.toString(slCols.getValue()));
          tileGrid.setGridSize(calcGridSize(tileGrid.getImageCount(), cols));
        }
        slCols.requestFocus(); // remove focus from textfield
      }
    }
  }

  @Override
  public void keyReleased(KeyEvent event) {
    // nothing to do
  }

  @Override
  public void keyTyped(KeyEvent event) {
    // nothing to do
  }

  // --------------------- End Interface KeyListener ---------------------

  // --------------------- Begin Interface PropertyChangeListener ---------------------

  @Override
  public void propertyChange(PropertyChangeEvent event) {
    if (event.getSource() instanceof SwingWorker<?, ?>) {
      @SuppressWarnings("unchecked")
      SwingWorker<Status, Void> worker = (SwingWorker<Status, Void>) event.getSource();
      workers.remove(worker);
      if ("state".equals(event.getPropertyName()) && SwingWorker.StateValue.DONE == event.getNewValue()) {
        if (blocker != null) {
          blocker.setBlocked(false);
          blocker = null;
        }
        Status retVal = Status.ERROR;
        try {
          retVal = worker.get();
          if (retVal == null) {
            retVal = Status.ERROR;
          }
        } catch (Exception e) {
          e.printStackTrace();
        }

        if (retVal == Status.SUCCESS) {
          JOptionPane.showMessageDialog(panel.getTopLevelAncestor(), "File exported successfully.", "Export complete",
              JOptionPane.INFORMATION_MESSAGE);
        } else if (retVal == Status.CANCELLED) {
          JOptionPane.showMessageDialog(panel.getTopLevelAncestor(), "Export has been cancelled.", "Information",
              JOptionPane.INFORMATION_MESSAGE);
        } else if (retVal == Status.UNSUPPORTED) {
          JOptionPane.showMessageDialog(panel.getTopLevelAncestor(), "Operation not (yet) supported.", "Information",
              JOptionPane.INFORMATION_MESSAGE);
        } else {
          JOptionPane.showMessageDialog(panel.getTopLevelAncestor(), "Error while exporting " + entry, "Error",
              JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  }

  // --------------------- End Interface PropertyChangeListener ---------------------

  // --------------------- Begin Interface Closeable ---------------------

  @Override
  public void close() throws Exception {
    while (!workers.isEmpty()) {
      SwingWorker<Status, Void> worker = workers.remove(0);
      if (worker != null && !worker.isDone()) {
        worker.cancel(true);
      }
      worker = null;
    }

    if (tileImages != null) {
      tileImages.clear();
      tileImages = null;
    }

    if (tileGrid != null) {
      tileGrid.clearImages();
      tileGrid = null;
    }

    if (decoder != null) {
      decoder.close();
      decoder = null;
    }

    System.gc();
  }

  // --------------------- End Interface Closeable ---------------------

  // --------------------- Begin Interface Resource ---------------------

  @Override
  public ResourceEntry getResourceEntry() {
    return entry;
  }

  // --------------------- End Interface Resource ---------------------

  // --------------------- Begin Interface Referenceable ---------------------

  @Override
  public boolean isReferenceable() {
    return true;
  }

  @Override
  public void searchReferences(Component parent) {
    new ReferenceSearcher(entry, parent);
  }

  // --------------------- End Interface Referenceable ---------------------

  // --------------------- Begin Interface Viewable ---------------------

  @Override
  public JComponent makeViewer(ViewableContainer container) {
    if (container instanceof RootPaneContainer) {
      rpc = (RootPaneContainer) container;
    } else {
      rpc = NearInfinity.getInstance();
    }

    if (decoder == null) {
      return new JPanel(new BorderLayout());
    }

    int tileCount = decoder.getTileCount();
    int defaultColumns = Math.min(tileCount, DEFAULT_COLUMNS);

    // 1. creating top panel
    // 1.1. creating label with text field
    JLabel lblTPR = new JLabel("Tiles per row:");
    tfCols = new JTextField(Integer.toString(defaultColumns), 5);
    tfCols.addKeyListener(this);
    JPanel tPanel1 = new JPanel(new FlowLayout(FlowLayout.CENTER));
    tPanel1.add(lblTPR);
    tPanel1.add(tfCols);

    // 1.2. creating slider
    slCols = new JSlider(SwingConstants.HORIZONTAL, 1, tileCount, defaultColumns);
    if (tileCount > 1000) {
      slCols.setMinorTickSpacing(100);
      slCols.setMajorTickSpacing(1000);
    } else if (tileCount > 100) {
      slCols.setMinorTickSpacing(10);
      slCols.setMajorTickSpacing(100);
    } else {
      slCols.setMinorTickSpacing(1);
      slCols.setMajorTickSpacing(10);
    }
    slCols.setPaintTicks(true);
    slCols.addChangeListener(this);

    // 1.3. adding left side of the top panel together
    JPanel tlPanel = new JPanel(new GridLayout(2, 1));
    tlPanel.add(tPanel1);
    tlPanel.add(slCols);

    // 1.4. configuring checkbox
    cbGrid = new JCheckBox("Show Grid", showGrid);
    cbGrid.addItemListener(this);
    JPanel trPanel = new JPanel(new GridLayout());
    trPanel.add(cbGrid);

    // 1.5. putting top panel together
    BorderLayout bl = new BorderLayout();
    JPanel topPanel = new JPanel(bl);
    topPanel.add(tlPanel, BorderLayout.CENTER);
    topPanel.add(trPanel, BorderLayout.LINE_END);

    // 2. creating main panel
    // 2.1. creating tiles table and scroll pane
    tileGrid = new TileGrid(1, defaultColumns, decoder.getTileWidth(), decoder.getTileHeight());
    tileGrid.addImage(tileImages);
    tileGrid.setGridSize(calcGridSize(tileGrid.getImageCount(), getDefaultTilesPerRow()));
    tileGrid.setShowGrid(showGrid);

    menuTileInfo.add(miTileInfoShow);
    menuTileInfo.add(miTileInfoPvrz);
    menuTileInfo.add(miTileInfoWed);
    miTileInfoShow.addActionListener(this);
    miTileInfoPvrz.addActionListener(this);
    miTileInfoWed.addActionListener(this);
    tileGrid.addMouseListener(new PopupListener());

    slCols.setValue(tileGrid.getTileColumns());
    tfCols.setText(Integer.toString(tileGrid.getTileColumns()));
    JScrollPane scroll = new JScrollPane(tileGrid);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.getHorizontalScrollBar().setUnitIncrement(16);

    // 2.2. putting main panel together
    JPanel centerPanel = new JPanel(new BorderLayout());
    centerPanel.add(scroll, BorderLayout.CENTER);

    // 3. creating bottom panel
    // 3.1. creating export button
    miExport = new JMenuItem("original");
    miExport.addActionListener(this);
    if (decoder.getType() == TisDecoder.Type.PVRZ) {
      miExportPaletteTis = new JMenuItem("as palette-based TIS");
      miExportPaletteTis.addActionListener(this);
    } else if (decoder.getType() == TisDecoder.Type.PALETTE) {
      miExportPvrzTis = new JMenuItem("as PVRZ-based TIS");
      miExportPvrzTis.addActionListener(this);
    }
    miExportPNG = new JMenuItem("as PNG");
    miExportPNG.addActionListener(this);

    List<JMenuItem> list = new ArrayList<>();
    if (miExport != null) {
      list.add(miExport);
    }
    if (miExportPaletteTis != null) {
      list.add(miExportPaletteTis);
    }
    if (miExportPvrzTis != null) {
      list.add(miExportPvrzTis);
    }
    if (miExportPNG != null) {
      list.add(miExportPNG);
    }
    JMenuItem[] mi = new JMenuItem[list.size()];
    for (int i = 0; i < mi.length; i++) {
      mi[i] = list.get(i);
    }
    ((JButton) buttonPanel.addControl(ButtonPanel.Control.FIND_REFERENCES)).addActionListener(this);
    ButtonPopupMenu bpmExport = (ButtonPopupMenu) buttonPanel.addControl(ButtonPanel.Control.EXPORT_MENU);
    bpmExport.setMenuItems(mi);

    // 4. packing all together
    panel = new JPanel(new BorderLayout());
    panel.add(topPanel, BorderLayout.NORTH);
    panel.add(centerPanel, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.SOUTH);
    centerPanel.setBorder(BorderFactory.createLoweredBevelBorder());
    return panel;
  }

  // --------------------- End Interface Viewable ---------------------

  /**
   * Returns a read-only list of decoded tiles.
   *
   * @return {@link List} of tiles as {@link Image} objects.
   */
  public List<Image> getTileList() {
    return Collections.unmodifiableList(tileImages);
  }

  /**
   * Returns the {@link TisDecoder} instance for this tileset.
   *
   * @return {@link TisDecoder} instance.
   */
  public TisDecoder getDecoder() {
    return decoder;
  }

  /** Returns whether the specified PVRZ index can be found in the current TIS resource. */
  public boolean containsPvrzReference(int index) {
    boolean retVal = false;
    if (index >= 0 && index <= 99) {
      if (decoder instanceof TisV2Decoder) {
        TisV2Decoder tisv2 = (TisV2Decoder) decoder;
        for (int i = 0, count = tisv2.getTileCount(); i < count && !retVal; i++) {
          retVal = (tisv2.getPvrzPage(i) == index);
        }
      }
    }
    return retVal;
  }

  // Returns detected or guessed number of tiles per row of the current TIS
  private int getDefaultTilesPerRow() {
    return defaultWidth;
  }

  // Returns an output filename for a TIS file
  private Path getTisFileName(Component parent, boolean enforceValidName) {
    Path retVal = null;
    JFileChooser fc = new JFileChooser(ResourceFactory.getExportFilePath().toFile());
    fc.setDialogTitle("Export resource");
    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    FileNameExtensionFilter filter = new FileNameExtensionFilter("TIS files (*.tis)", "tis");
    fc.addChoosableFileFilter(filter);
    fc.setFileFilter(filter);
    fc.setSelectedFile(new File(fc.getCurrentDirectory(), getResourceEntry().getResourceName()));
    boolean repeat = enforceValidName;
    do {
      retVal = null;
      if (fc.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
        retVal = fc.getSelectedFile().toPath();
        if (enforceValidName && !TisConvert.isTisFileNameValid(retVal)) {
          JOptionPane.showMessageDialog(parent, "PVRZ-based TIS filenames have to be 2 up to 7 characters long.",
              "Error", JOptionPane.ERROR_MESSAGE);
        } else {
          repeat = false;
        }
        if (FileEx.create(retVal).exists()) {
          final String options[] = { "Overwrite", "Cancel" };
          if (JOptionPane.showOptionDialog(parent, retVal + " exists. Overwrite?", "Export resource",
              JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]) != 0) {
            retVal = null;
            repeat = false;
          }
        }
      } else {
        repeat = false;
      }
    } while (repeat);
    return retVal;
  }

  // Returns output filename for a PNG file
  private Path getPngFileName(Component parent) {
    Path retVal = null;
    JFileChooser fc = new JFileChooser(ResourceFactory.getExportFilePath().toFile());
    fc.setDialogTitle("Export resource");
    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG files (*.png)", "png");
    fc.addChoosableFileFilter(filter);
    fc.setFileFilter(filter);
    fc.setSelectedFile(new File(fc.getCurrentDirectory(),
        getResourceEntry().getResourceName().toUpperCase(Locale.ENGLISH).replace(".TIS", ".PNG")));
    if (fc.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
      retVal = fc.getSelectedFile().toPath();
      if (FileEx.create(retVal).exists()) {
        final String options[] = { "Overwrite", "Cancel" };
        if (JOptionPane.showOptionDialog(parent, retVal + " exists. Overwrite?", "Export resource",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]) != 0) {
          retVal = null;
        }
      }
    }
    return retVal;
  }

  private void initTileset() {
    try {
      WindowBlocker.blockWindow(true);

      decoder = TisDecoder.loadTis(entry);
      if (decoder != null) {
        wedResource = TisConvert.loadWedForTis(entry, false);
        initOverlayMap(wedResource);
        int tileCount = decoder.getTileCount();
        defaultWidth = calcTileWidth(wedResource, tileCount);
        tileImages = new ArrayList<>(tileCount);
        for (int tileIdx = 0; tileIdx < tileCount; tileIdx++) {
          BufferedImage image = ColorConvert.createCompatibleImage(64, 64, Transparency.BITMASK);
          decoder.getTile(tileIdx, image);
          tileImages.add(image);
        }
      } else {
        throw new Exception("No TIS resource loaded");
      }
      WindowBlocker.blockWindow(false);
    } catch (Exception e) {
      e.printStackTrace();
      WindowBlocker.blockWindow(false);
      if (tileImages == null) {
        tileImages = new ArrayList<>();
      }
      if (tileImages.isEmpty()) {
        tileImages.add(ColorConvert.createCompatibleImage(1, 1, Transparency.BITMASK));
      }
      JOptionPane.showMessageDialog(NearInfinity.getInstance(),
          "Error while loading TIS resource: " + entry.getResourceName(), "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  // Initializes the WED overlay lookup table for tile information, if WED resource is available.
  private void initOverlayMap(WedResource wed) {
    if (wed != null) {
      try {
        final Overlay ovl = (Overlay) wed.getAttribute(Overlay.WED_OVERLAY + " 0");
        int width = ((IsNumeric) ovl.getAttribute(Overlay.WED_OVERLAY_WIDTH)).getValue();
        int height = ((IsNumeric) ovl.getAttribute(Overlay.WED_OVERLAY_HEIGHT)).getValue();
        wedTileMap = new HashMap<>(width * height * 4 / 3);

        // mapping primary tiles
        final List<StructEntry> tileMapList = ovl.getFields(Tilemap.class);
        final List<StructEntry> tileIndexList = ovl.getFields(IndexNumber.class);
        for (final StructEntry e : tileMapList) {
          final Tilemap tm = (Tilemap) e;
          final int idx = ((IsNumeric) tm.getAttribute(Tilemap.WED_TILEMAP_TILE_INDEX_PRI)).getValue();
          final int tileIdx = ((IsNumeric) tileIndexList.get(idx)).getValue();
          wedTileMap.put(tileIdx, tm);
        }

        // mapping secondary tiles
        wedTileMap.entrySet().stream().filter(e -> {
          final int idx = ((IsNumeric) e.getValue().getAttribute(Tilemap.WED_TILEMAP_TILE_INDEX_SEC)).getValue();
          return (idx >= 0 && !wedTileMap.containsKey(idx));
        }).forEach((e) -> {
          final Tilemap tm = e.getValue();
          final int idx = ((IsNumeric) tm.getAttribute(Tilemap.WED_TILEMAP_TILE_INDEX_SEC)).getValue();
          wedTileMap.putIfAbsent(idx, tm);
        });
      } catch (Exception e) {
      }
    }
  }

  /** Opens the specified {@code StructEntry} in a new {@code ViewFrame} window. */
  private boolean openStructEntry(StructEntry e) {
    if (e != null) {
      for (StructEntry se = e; se != null; se = se.getParent()) {
        if (se instanceof Resource) {
          final Resource viewable = (Resource) se;
          new ViewFrame(panel, viewable);
          ((AbstractStruct) viewable).getViewer().selectEntry(e.getOffset());
          return true;
        }
      }
    }
    return false;
  }

  /** Opens the PVRZ resource containing the specified tile index. */
  private boolean openPvrzResource(int tileIndex) {
    if (decoder instanceof TisV2Decoder && tileIndex >= 0 && tileIndex < decoder.getTileCount()) {
      final TisV2Decoder d = (TisV2Decoder) decoder;
      final ResourceEntry resEntry = ResourceFactory.getResourceEntry(d.getPvrzFileName(tileIndex));
      if (resEntry != null) {
        final Resource res = ResourceFactory.getResource(resEntry);
        if (res != null) {
          new ViewFrame(panel, res);
          return true;
        }
      }
    }
    return false;
  }

  /** Opens a message dialog with PVRZ-related information about the specified tile. */
  private boolean showPvrzInfo(int tileIndex) {
    if (decoder instanceof TisV2Decoder && tileIndex >= 0 && tileIndex < decoder.getTileCount()) {
      final TisV2Decoder d = (TisV2Decoder) decoder;
      final ByteBuffer buf = d.getResourceBuffer();
      final int tileOfs = buf.getInt(0x10) + tileIndex * 0x0c;
      final int tilePage = buf.getInt(tileOfs);

      final String info;
      if (tilePage < 0) {
        info = "Tile type: Solid black color<br>No PVRZ reference available.";
      } else {
        final String pvrzName = d.getPvrzFileName(tileIndex);
        final int tileX = buf.getInt(tileOfs + 4);
        final int tileY = buf.getInt(tileOfs + 8);
        info = String.format(
            "PVRZ resource: %s<br>" +
            "PVRZ page: %d<br>" +
            "PVRZ coordinates: x=%d, y=%d",
            pvrzName, tilePage, tileX, tileY);
      }
      JOptionPane.showMessageDialog(panel,
          String.format("<html><div style=\"font-family:monospace;padding-right:8px\">%s<br>&nbsp;</div></html>", info),
          "PVRZ information: tile " + tileIndex, JOptionPane.INFORMATION_MESSAGE);
      return true;
    }
    return false;
  }

  /**
   * Performs the given operation in a background task.
   *
   * @param operation Operation to perform as {@link Supplier} object.
   * @return {@link SwingWorker} instance that is used to perform the background operation.
   */
  private SwingWorker<Status, Void> performBackgroundTask(Supplier<Status> operation) {
    if (operation != null) {
      blocker = new WindowBlocker(rpc);
      blocker.setBlocked(true);
      final SwingWorker<Status, Void> worker = new SwingWorker<Status, Void>() {
        protected Status doInBackground() throws Exception {
          Status retVal = Status.ERROR;
          try {
            retVal = operation.get();
          } catch (Exception e) {
            e.printStackTrace();
          }
          return retVal;
        }
      };
      workers.add(worker);
      worker.addPropertyChangeListener(this);
      worker.execute();
      return worker;
    }
    return null;
  }

  /**
   * Attempts to retrieve the tileset width, in tiles, from the specified WED resource. Falls back to a value
   * based on {@code defTileCount}, if WED information is not available.
   *
   * @param wed           WED resource for this TIS.
   * @param defTileCount  A an optional tile count that will be used to "guess" the correct number of tiles per row
   *                      if WED information is no available.
   * @return Number of tiles per row for the current TIS resource.
   */
  private static int calcTileWidth(WedResource wed, int defTileCount) {
    int retVal = (defTileCount < 9) ? defTileCount : (int) (Math.sqrt(defTileCount) * 1.18);

    if (wed != null) {
      final Overlay ovl = (Overlay) wed.getAttribute(Overlay.WED_OVERLAY + " 0");
      if (ovl != null) {
        final int width = ((IsNumeric) ovl.getAttribute(Overlay.WED_OVERLAY_WIDTH)).getValue();
        if (width > 0) {
          retVal = width;
        }
      }
    }

    return retVal;
  }

  // Calculates a Dimension structure with the correct number of columns and rows from the specified arguments
  private static Dimension calcGridSize(int imageCount, int colSize) {
    if (imageCount >= 0 && colSize > 0) {
      int rowSize = imageCount / colSize;
      if (imageCount % colSize > 0) {
        rowSize++;
      }
      return new Dimension(colSize, Math.max(1, rowSize));
    }
    return null;
  }

  // -------------------------- INNER CLASSES --------------------------

  private final class PopupListener extends MouseAdapter {
    @Override
    public void mousePressed(MouseEvent e) {
      maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
      if (e.isPopupTrigger()) {
        int index = tileGrid.getTileIndexAt(new Point(e.getX(), e.getY()));
        if (index >= 0 && index < decoder.getTileCount()) {
          lastTileInfoIndex = index;

          miTileInfoShow.setText(String.format(FMT_TILEINFO_SHOW, lastTileInfoIndex));
          miTileInfoShow.setVisible(decoder.getType() == TisDecoder.Type.PVRZ);

          miTileInfoPvrz.setText(String.format(FMT_TILEINFO_PVRZ, lastTileInfoIndex));
          miTileInfoPvrz.setVisible(decoder.getType() == TisDecoder.Type.PVRZ);

          miTileInfoWed.setText(String.format(FMT_TILEINFO_WED, lastTileInfoIndex));
          miTileInfoWed.setVisible(wedTileMap != null && wedTileMap.containsKey(lastTileInfoIndex));

          if (miTileInfoShow.isVisible() || miTileInfoPvrz.isVisible() || miTileInfoWed.isVisible()) {
            menuTileInfo.show(e.getComponent(), e.getX(), e.getY());
          }
        }
      }
    }
  }
}
