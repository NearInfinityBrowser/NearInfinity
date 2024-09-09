// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.converter;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.IndexColorModel;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.BiFunction;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.infinity.gui.ButtonPopupMenu;
import org.infinity.gui.NewAbstractSettings;
import org.infinity.gui.OpenResourceDialog;
import org.infinity.gui.ViewerUtil;
import org.infinity.icon.Icons;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.graphics.BamDecoder;
import org.infinity.resource.graphics.BamDecoder.BamControl;
import org.infinity.resource.graphics.BamDecoder.FrameEntry;
import org.infinity.resource.graphics.ColorConvert;
import org.infinity.resource.graphics.PseudoBamDecoder;
import org.infinity.resource.graphics.PseudoBamDecoder.PseudoBamControl;
import org.infinity.resource.graphics.PseudoBamDecoder.PseudoBamCycleEntry;
import org.infinity.resource.graphics.PseudoBamDecoder.PseudoBamFrameEntry;
import org.infinity.resource.key.BIFFResourceEntry;
import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Misc;
import org.infinity.util.Platform;
import org.infinity.util.tuples.Couple;
import org.tinylog.Logger;

/**
 * Output filter: Overlay the current animation with multiple BAM files.
 */
public class BamFilterOutputOverlay extends BamFilterBaseOutput implements ActionListener, ListSelectionListener {
  private static final String FILTER_NAME = "Overlay BAM output";
  private static final String FILTER_DESC = "This filter renders all listed BAM files on top of the current "
                                            + "animation on a per frame basis. It can be used to combine BAM animations "
                                            + "that were split into segments because of hardware limitations "
                                            + "(e.g. creature animations.)\n"
                                            + "Overlay BAM files should contain at least as many cycles and cycle frame "
                                            + "entries as the active BAM animation.\n"
                                            + "Note: Output filters will always be processed last.";

  /**
   * Controls how BAM animations should be rendered over the source BAM animation.
   */
  public enum OverlayMode {
    /** Source pixels are only overwritten by non-transparent overlay BAM pixels. */
    NORMAL("Normal",
        "Source pixels are only overwritten by non-transparent overlay BAM pixels. Semi-transparent pixels are blended.",
        BamFilterOutputOverlay::pixelOpNormal),
    /** Source pixels are always overwritten by overlay BAM pixels. */
    FORCED("Forced",
        "Source pixels are always overwritten by overlay BAM pixels.",
        BamFilterOutputOverlay::pixelOpForced),
    /** Only non-transparent source pixels are overwritten by overlay BAM pixels. */
    INCLUSIVE("Inclusive",
        "Only non-transparent source pixels are overwritten by overlay BAM pixels. Semi-transparent pixels are blended.",
        BamFilterOutputOverlay::pixelOpInclusive),
    /** Only transparent source pixels are overwritten by overlay BAM pixels. */
    EXCLUSIVE("Exclusive",
        "Only transparent source pixels are overwritten by overlay BAM pixels. Semi-transparent pixels are blended.",
        BamFilterOutputOverlay::pixelOpExclusive),
    ;

    /** Returns the specified {@link OverlayMode}, or {@link #NORMAL} if {@code mode} is {@code null}. */
    public static OverlayMode getOrDefault(OverlayMode mode) {
      return (mode != null) ? mode : NORMAL;
    }

    /**
     * Returns the {@link OverlayMode} with the specified ordinal value.
     * Returns {@link #NORMAL} if the value is out of bounds.
     */
    public static OverlayMode getOrDefault(int ordinal) {
      if (ordinal >= 0 && ordinal < values().length) {
        return values()[ordinal];
      } else {
        return NORMAL;
      }
    }

    private final String name;
    private final String desc;
    private final OverlayFunc fn;

    OverlayMode(String name, String desc, OverlayFunc fn) {
      this.name = name;
      this.desc = desc;
      this.fn = fn;
    }

    /** Returns a short name for the OverlayMode enum. */
    public String getName() {
      return name;
    }

    /** Returns a more verbose description of the effect the OverlayMode enum produces. */
    public String getDesc() {
      return desc;
    }

    /** Returns a function that can be used to achieve the desired overlay effect. It works on a per pixel basis. */
    public OverlayFunc getFunction() {
      return fn;
    }

    @Override
    public String toString() {
      return getName();
    }
  }

  // Cache for overlay BAM resources
  private final HashMap<ResourceEntry, BamDecoder> bamCache = new HashMap<>();

  private OverlayTableModel model;
  private JTable table;
  private JButton bUp;
  private JButton bDown;
  private JButton bEdit;
  private JButton bRemove;
  private JMenuItem miAddResource;
  private JMenuItem miAddFile;

  public static String getFilterName() {
    return FILTER_NAME;
  }

  public static String getFilterDesc() {
    return FILTER_DESC;
  }

  public BamFilterOutputOverlay(ConvertToBam parent) {
    super(parent, FILTER_NAME, FILTER_DESC);
  }

  @Override
  public boolean process(PseudoBamDecoder decoder) throws Exception {
    return applyEffect(decoder);
  }

  @Override
  public String getConfiguration() {
    // Format: A concatenation of pairs of BAM resource or file paths and overlay modes
    // Note: URI-encode semicolon in path strings: %3B
    // Example: res:/bam_resource;0;file:///c:/path/to/bam_file;1;rel:/chitin.key;2;...
    final StringBuilder sb = new StringBuilder();
    for (final Couple<ResourceEntry, OverlayMode> entry : model) {
      try {
        final ResourcePath rp = new ResourcePath(entry.getValue0());
        final OverlayMode mode = entry.getValue1();
        if (sb.length() > 0) {
          sb.append(';');
        }
        sb.append(rp.toString()).append(';').append(mode.ordinal());
      } catch (Exception e) {
        Logger.warn(e, "Invalid resource: " + entry);
      }
    }
    return sb.toString();
  }

  @Override
  public boolean setConfiguration(String config) {
    if (config != null) {
      model.clear();
      config = config.trim();
      if (!config.isEmpty()) {
        final String[] params = config.split(";");
        for (int i = 0; i < params.length; i+= 2) {
          // parsing BAM resource
          final ResourceEntry resource;
          try {
            resource = new ResourcePath(new URI(params[i])).getResourceEntry();
          } catch (URISyntaxException e) {
            Logger.error(e, "Invalid filter parameter: " + params[i]);
            continue;
          }

          // parsing overlay mode
          final OverlayMode mode;
          if (i + 1 < params.length) {
            int modeIdx = Misc.toNumber(params[i + 1], 0);
            mode = OverlayMode.getOrDefault(modeIdx);
          } else {
            mode = OverlayMode.NORMAL;
            Logger.warn("No overlay mode specified. Assuming default mode.");
          }

          addResource(resource, mode, false);
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public PseudoBamFrameEntry updatePreview(int frameIndex, PseudoBamFrameEntry frame) {
    // shortcut
    if (model.isEmpty()) {
      return frame;
    }

    // cycle index and cycle entry index are needed to render the correct overlay BAM frames
    int cycleIdx = -1;
    int cycleEntryIdx = -1;
    final PseudoBamDecoder decoder = getConverter().getBamDecoder(ConvertToBam.BAM_FINAL);
    final List<PseudoBamCycleEntry> cycleList = decoder.getCyclesList();
    for (int i = 0, numCycles = cycleList.size(); i < numCycles; i++) {
      final PseudoBamCycleEntry cycle = cycleList.get(i);
      for (int j = 0, numCycleEntries = cycle.size(); j < numCycleEntries; j++) {
        final int frameIdx = cycle.get(j);
        if (frameIdx == frameIndex) {
          cycleIdx = i;
          cycleEntryIdx = j;
          break;
        }
      }
      if (cycleIdx >= 0) {
        break;
      }
    }

//    Logger.debug("Cycle: {}, cycle entry: {}", cycleIdx, cycleEntryIdx);
    if (cycleIdx >= 0) {
      // perform preview operation
      final int[] frameIndices = new int[model.getRowCount()];
      for (int i = 0; i < frameIndices.length; i++) {
        final ResourceEntry re = model.get(i).getValue0();
        final BamDecoder ovlDecoder = bamCache.get(re);
        if (ovlDecoder != null) {
          final BamControl control = ovlDecoder.createControl();
          int ovlFrameIdx = control.cycleGetFrameIndexAbsolute(cycleIdx, cycleEntryIdx);
          frameIndices[i] = ovlFrameIdx;
        } else {
          Logger.warn("No BAM decoder available for overlay resource: " + re);
          return frame;
        }
      }

      try {
        return createOverlayFrame(frame, frameIndices);
      } catch (Exception e) {
        Logger.error(e);
      }
    }

    return frame;
  }

  @Override
  protected JPanel loadControls() {
    // getting optimal width for "overlay mode" table column
    int maxWidth = 0;
    final JLabel l = new JLabel();
    for (final OverlayMode mode : OverlayMode.values()) {
      final Dimension dim = Misc.getPrototypeSize(l, mode.toString());
      maxWidth = Math.max(maxWidth, dim.width);
    }

    model = new OverlayTableModel();
    table = new JTable(model);
    table.getSelectionModel().addListSelectionListener(this);
    table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    table.setDragEnabled(false);

    final DefaultTableCellRenderer tcr = new DefaultTableCellRenderer();
    tcr.setHorizontalAlignment(SwingConstants.CENTER);
    table.getColumnModel().getColumn(1).setCellRenderer(tcr);
    table.getColumnModel().getColumn(1).setPreferredWidth(maxWidth + 8);
    table.getColumnModel().getColumn(1).setMaxWidth(maxWidth + 8);

    table.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && bEdit.isEnabled()) {
          // invoke "Edit" button click
          actionPerformed(new ActionEvent(bEdit, ActionEvent.ACTION_PERFORMED, null));
        }
        super.mouseClicked(e);
      }
    });

    table.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if ((e.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) != 0 && e.getKeyCode() == KeyEvent.VK_A) {
          // Ctrl+A
          table.selectAll();
        }
      }
    });

    final JScrollPane scroll = new JScrollPane(table);
    scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

    bUp = new JButton(Icons.ICON_UP_16.getIcon());
    bUp.setMargin(new Insets(bUp.getInsets().top + 4, 2, bUp.getInsets().bottom + 4, 2));
    bUp.setToolTipText("Move selected list entry up.");
    bUp.setEnabled(false);
    bUp.addActionListener(this);

    bDown = new JButton(Icons.ICON_DOWN_16.getIcon());
    bDown.setMargin(new Insets(bDown.getInsets().top + 4, 2, bDown.getInsets().bottom + 4, 2));
    bDown.setToolTipText("Move selected list entry down.");
    bDown.setEnabled(false);
    bDown.addActionListener(this);

    bEdit = new JButton("Edit...");
    bEdit.setToolTipText("Edit overlay mode of the selected list entry.");
    bEdit.setEnabled(false);
    bEdit.addActionListener(this);

    bRemove = new JButton("Remove");
    bRemove.setToolTipText("Remove the selected BAM resource entry from the list.");
    bRemove.setEnabled(false);
    bRemove.addActionListener(this);

    miAddResource = new JMenuItem("Add BAM resource...");
    miAddResource.addActionListener(this);
    miAddFile = new JMenuItem("Add BAM file...");
    miAddFile.addActionListener(this);
    final ButtonPopupMenu bpmAdd = new ButtonPopupMenu("Add", new JMenuItem[]{miAddResource, miAddFile});
    bpmAdd.setIcon(Icons.ICON_ARROW_UP_15.getIcon());
    bpmAdd.setToolTipText("Add a new game resource or external file to the list.");

    final GridBagConstraints c = new GridBagConstraints();
    final JPanel panelRight = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    panelRight.add(bUp, c);
    ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(8, 0, 0, 0), 0, 0);
    panelRight.add(bDown, c);

    final JPanel panelBottom = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    panelBottom.add(bRemove, c);
    ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_END, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0);
    panelBottom.add(new JPanel(), c);
    ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    panelBottom.add(bEdit, c);
    ViewerUtil.setGBC(c, 3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END, GridBagConstraints.NONE,
        new Insets(0, 8, 0, 0), 0, 0);
    panelBottom.add(bpmAdd, c);

    final JPanel panelMain = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH,
        new Insets(0, 0, 0, 0), 0, 0);
    panelMain.add(scroll, c);
    ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.VERTICAL,
        new Insets(0, 4, 0, 0), 0, 0);
    panelMain.add(panelRight, c);
    ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_END, GridBagConstraints.HORIZONTAL,
        new Insets(8, 0, 0, 0), 0, 0);
    panelMain.add(panelBottom, c);
    ViewerUtil.setGBC(c, 1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
        new Insets(0, 0, 0, 0), 0, 0);
    panelMain.add(new JPanel(), c);

    return panelMain;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == bUp) {
      // moving resource entry one position up
      if (onMoveUpClicked()) {
        fireChangeListener();
      }
    } else if (e.getSource() == bDown) {
      // moving resource entry one position down
      if (onMoveDownClicked()) {
        fireChangeListener();
      }
    } else if (e.getSource() == bEdit) {
      // editing overlay mode of selected entries
      if (onEditClicked()) {
        fireChangeListener();
      }
    } else if (e.getSource() == bRemove) {
      // removing overlay resource entry
      if (onRemoveClicked()) {
        fireChangeListener();
      }
    } else if (e.getSource() == miAddFile) {
      // adding new external file
      if (onAddFileClicked()) {
        fireChangeListener();
      }
    } else if (e.getSource() == miAddResource) {
      // adding new game resource
      if (onAddResourceClicked()) {
        fireChangeListener();
      }
    }
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    if (!e.getValueIsAdjusting()) {
      // getting selected row(s)
      int minIdx = model.getRowCount();
      int maxIdx = -1;
      for (final int i : table.getSelectedRows()) {
        minIdx = Math.min(minIdx, i);
        maxIdx = Math.max(maxIdx, i);
      }
      final boolean selected = (minIdx != model.getRowCount() && maxIdx != -1);
      bEdit.setEnabled(selected);
      bRemove.setEnabled(selected);
      bUp.setEnabled(selected && minIdx > 0);
      bDown.setEnabled(selected && maxIdx < model.getRowCount() - 1);
    }
  }

  /** Moves all selected table entries one position up (towards start of list). Returns {@code true} if table data has changed. */
  private boolean onMoveUpClicked() {
    final int minIndex = Arrays.stream(table.getSelectedRows()).min().orElse(-1);
    if (minIndex > 0) {
      final int[] indices = table.getSelectedRows();
      for (int index : indices) {
        final Couple<ResourceEntry, OverlayMode> item = model.remove(index);
        model.add(index - 1, item);
      }
      table.getSelectionModel().clearSelection();
      for (final int index : indices) {
        table.getSelectionModel().addSelectionInterval(index - 1, index - 1);
      }
      return true;
    }
    return false;
  }

  /** Moves all selected table entries one position down (towards end of list). Returns {@code true} if table data has changed. */
  private boolean onMoveDownClicked() {
    final int maxIndex = Arrays.stream(table.getSelectedRows()).max().orElse(model.getRowCount());
    if (maxIndex < model.getRowCount() - 1) {
      final int[] indices = table.getSelectedRows();
      for (int i = indices.length - 1; i >= 0; i--) {
        final Couple<ResourceEntry, OverlayMode> item = model.remove(indices[i]);
        model.add(indices[i] + 1, item);
      }
      table.getSelectionModel().clearSelection();
      for (final int index : indices) {
        table.getSelectionModel().addSelectionInterval(index + 1, index + 1);
      }
      return true;
    }
    return false;
  }

  /** Allows the user to adjust the overlay mode for selected table entries. Returns {@code true} if table data has changed. */
  private boolean onEditClicked() {
    final EnumSet<OverlayMode> modes = EnumSet.noneOf(OverlayMode.class);
    for (final int rowIdx : table.getSelectedRows()) {
      final OverlayMode mode = (OverlayMode) model.getValueAt(rowIdx, 1);
      if (mode != null) {
        modes.add(mode);
      }
    }
    if (!modes.isEmpty()) {
      final OverlayMode mode = (modes.size() == 1) ? modes.iterator().next() : null;
      final OverlayModeSelection dlg = new OverlayModeSelection(getConverter(), mode);
      if (dlg.isAccepted()) {
        final OverlayMode newMode = dlg.getConfig();
        if (newMode != null) {
          for (final int rowIdx : table.getSelectedRows()) {
            model.set(rowIdx, newMode);
          }
          return true;
        }
      }
    }
    return false;
  }

  /** Removes all selected entries from the table. Returns {@code true} if table data has changed. */
  private boolean onRemoveClicked() {
    final int[] selectedRows = table.getSelectedRows();
    if (selectedRows.length > 0) {
      int maxIdx = -1;
      for (int i = selectedRows.length - 1; i >= 0; i--) {
        removeResource(selectedRows[i]);
        maxIdx = Math.max(maxIdx, selectedRows[i]);
      }
      if (!model.isEmpty()) {
        maxIdx = Math.max(maxIdx, model.getRowCount() - 1);
        table.setRowSelectionInterval(maxIdx, maxIdx);
      }
      return true;
    }
    return false;
  }

  /** Allows the user to add one or more external files to the table. Returns {@code true} if table data has changed. */
  private boolean onAddFileClicked() {
    final Path[] files = ConvertToBam.getOpenFileName(getConverter(), "BAM files", null, true,
        new FileNameExtensionFilter[] { ConvertToBam.getBamFilter() }, 0);
    if (files != null) {
      for (final Path file : files) {
        final ResourceEntry entry = new FileResourceEntry(file);
        try {
          if (addResource(entry, null, true)) {
            final int rowIdx = model.getRowCount() - 1;
            table.setRowSelectionInterval(rowIdx, rowIdx);
          }
        } catch (NullPointerException npe) {
          Logger.warn(npe);
          JOptionPane.showMessageDialog(getConverter(), npe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
      return true;
    }
    return false;
  }

  /** Allows the user to add one or more game resources to the table. Returns {@code true} if table data has changed. */
  private boolean onAddResourceClicked() {
    final ResourceEntry[] resources = OpenResourceDialog.showOpenDialog(getConverter(), "BAM resources",
        new String[] { "BAM" }, true);
    if (resources != null) {
      for (final ResourceEntry resource : resources) {
        try {
          if (addResource(resource, null, true)) {
            final int rowIdx = model.getRowCount() - 1;
            table.setRowSelectionInterval(rowIdx, rowIdx);
          }
        } catch (NullPointerException npe) {
          Logger.warn(npe);
          JOptionPane.showMessageDialog(getConverter(), npe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Adds the specified BAM resource to the overlay table.
   *
   * @param entry {@link ResourceEntry} of the BAM resource to add.
   * @param mode {@link OverlayMode} to associate with the BAM resource. Specify {@code null} to use {@link OverlayMode#NORMAL}.
   * @param warn Indicates whether a warning dialog should be shown if the BAM resource has an incompatible cycle structure.
   * @return {@code true} if the BAM resource was added successfully, {@code false} otherwise.
   * @throws NullPointerException if the load operation was cancelled.
   */
  private boolean addResource(ResourceEntry entry, OverlayMode mode, boolean warn) {
    Objects.requireNonNull(entry, "BAM resource is null");
    if (!bamCache.containsKey(entry)) {
      final BamDecoder decoder = BamDecoder.loadBam(entry);
      if (decoder == null) {
        throw new NullPointerException("Could not load BAM: " + entry);
      }

      if (warn) {
        if (!isBamCompatible(decoder)) {
          final int retVal = JOptionPane.showConfirmDialog(getConverter(),
              "Incompatible cycle structure detected. Load anyway?", entry.getResourceName(), JOptionPane.YES_NO_OPTION);
          if (retVal != JOptionPane.YES_OPTION) {
            return false;
          }
        }
      }

      bamCache.put(entry, decoder);
    }
    model.add(entry, OverlayMode.getOrDefault(mode));
    return true;
  }

  /**
   * Removes the BAM resource entry at the specified table row index.
   *
   * @param rowIndex Table row index of the BAM resource.
   * @throws IndexOutOfBoundsException if {@code rowIndex} is negative or greater than or equal to the table size.
   */
  private void removeResource(int rowIndex) {
    if (rowIndex >= 0) {
      final Couple<ResourceEntry, OverlayMode> item = model.remove(rowIndex);
      bamCache.remove(item.getValue0());
    }
  }

  /**
   * Determines whether the specified BAM is compatible with the active BAM.
   * A BAM is considered compatible if it contains at least as many cycles and every cycle contains at least
   * as many frame indices as the active BAM.
   *
   * @param decoder {@link BamDecoder} instance of a overlay BAM resource.
   * @return {@code true} if the BAM resource is compatible, {@code false} otherwise.
   */
  private boolean isBamCompatible(BamDecoder decoder) {
    final BamControl dstControl = Objects.requireNonNull(decoder).createControl();
    final BamDecoder srcDecoder = getConverter().getBamDecoder(ConvertToBam.BAM_FINAL);
    final BamControl srcControl = srcDecoder.createControl();

    boolean isCompatible = dstControl.cycleCount() >= srcControl.cycleCount();
    for (int i = 0, count = srcControl.cycleCount(); isCompatible && i < count; i++) {
      isCompatible = dstControl.cycleFrameCount(i) >= srcControl.cycleFrameCount(i);
    }
    return isCompatible;
  }

  private boolean applyEffect(PseudoBamDecoder decoder) throws Exception {
    if (decoder == null) {
      return false;
    }

    if (model.isEmpty()) {
      return BamFilterBaseOutput.convertBam(getConverter(), getConverter().getBamOutput(), decoder);
    }

    // mapping: array of source frame indices -> new frame index
    final HashMap<int[], Integer> frameIndexCache =
        new HashMap<>(decoder.frameCount() * 4 / 3);

    // initializing BAM controls; controls[0] always refers to the source BAM control
    final BamControl[] controls = new BamControl[model.getRowCount() + 1];
    controls[0] = decoder.createControl();
    for (int i = 0; i < model.getRowCount(); i++) {
      final ResourceEntry re = model.get(i).getValue0();
      controls[i + 1] = bamCache.get(re).createControl();
    }
    // stores new frame indices for each cycle/frame index
    final int[][] cycleFrameIndices = new int[controls[0].cycleCount()][];

    // generating frame index mappings
    for (int cycleIdx = 0, numCycles = controls[0].cycleCount(); cycleIdx < numCycles; cycleIdx++) {
      cycleFrameIndices[cycleIdx] = new int[controls[0].cycleFrameCount(cycleIdx)];
      for (int frameIdx = 0, numFrames = controls[0].cycleFrameCount(cycleIdx); frameIdx < numFrames; frameIdx++) {
        final int[] frames = new int[controls.length];
        for (int i = 0; i < frames.length; i++) {
          frames[i] = controls[i].cycleGetFrameIndexAbsolute(cycleIdx, frameIdx);
        }
        final int newIndex = frameIndexCache.size();
        frameIndexCache.computeIfAbsent(frames, key -> newIndex);
        if (frameIndexCache.containsKey(frames)) {
          cycleFrameIndices[cycleIdx][frameIdx] = frameIndexCache.get(frames);
        } else {
          throw new Exception("Unexpected cycle frame index generated for cycle=" + cycleIdx + ", frame index=" + frameIdx);
        }
      }
    }

    // reversing mapping direction to get a sorted set of "frame index -> array of source frame indices" pairs
    // this set should contain no gaps between the frame indices
    final TreeSet<Couple<Integer, int[]>> frameSet = new TreeSet<>((c1, c2) -> c1.getValue0() - c2.getValue0());
    for (final Map.Entry<int[], Integer> entry : frameIndexCache.entrySet()) {
      frameSet.add(Couple.with(entry.getValue(), entry.getKey()));
    }

    // generating output BAM
    PseudoBamDecoder decoderOut = new PseudoBamDecoder();
    final PseudoBamControl controlOut = decoderOut.createControl();
    // adding global custom options
    String[] options = decoder.getOptionNames();
    for (String option : options) {
      decoderOut.setOption(option, decoder.getOption(option));
    }

    // generating output frames (outsource actual frame image generation to separate method to reuse it for preview generation)
    int prevIndex = -1;
    for (final Couple<Integer, int[]> entry : frameSet) {
      final int frameIdx = entry.getValue0();
      final int[] srcFrameIndices = entry.getValue1();

      // adding dummy frames if needed (should not happen)
      while (prevIndex + 1 < frameIdx) {
        decoderOut.frameAdd(ColorConvert.createCompatibleImage(1, 1, true), new Point());
        prevIndex++;
        Logger.warn("Filling frame list gap with dummy entry at index " + prevIndex);
      }

      // generating output image and center information
      final PseudoBamFrameEntry frameEntry = createOverlayFrame(decoder.getFrameInfo(srcFrameIndices[0]),
          Arrays.copyOfRange(srcFrameIndices, 1, srcFrameIndices.length));
      decoderOut.frameAdd(frameEntry.getFrame(), new Point(frameEntry.getCenterX(), frameEntry.getCenterY()));

      prevIndex = frameIdx;
    }

    // recreating cycle information
    for (int[] cycleFrameIndex : cycleFrameIndices) {
      controlOut.cycleAdd(cycleFrameIndex);
    }

    if (getConverter().isBamV1Selected()) {
      // converting output BAM frames to paletted format
      // palette index 1 may have a special meaning: ensure that it doesn't change in the output BAM
      final int[] palette = getConverter().getPaletteDialog().getPalette(BamPaletteDialog.TYPE_GENERATED);
      final int[] reservedColors;
      if (palette != null && palette.length > 1) {
        reservedColors = new int[] { palette[1] };
      } else {
        reservedColors = new int[0];
      }
      decoderOut = convertToPalettedBam(decoderOut, ConvertToBam.getUseAlpha(), ConvertToBam.getTransparencyThreshold(),
          reservedColors);
    }

    // saving BAM to disk
    return BamFilterBaseOutput.convertBam(getConverter(), getConverter().getBamOutput(), decoderOut);
  }

  /**
   * Composes a new BAM frame from the specified source frame indices.
   *
   * @param srcFrameEntry {@link PseudoBamFrameEntry} instance of the active BAM animation frame.
   * @param overlayFrameIndices Array of BAM frame indices from the overlaid BAM resources.
   * @return Fully initialized {@link PseudoBamFrameEntry} of the composed frame.
   * @throws Exception if the output frame could not be generated.
   */
  private PseudoBamFrameEntry createOverlayFrame(PseudoBamFrameEntry srcFrameEntry, int[] overlayFrameIndices)
      throws Exception {
    if (srcFrameEntry == null) {
      throw new Exception("Source frame entry structure is null");
    }
    if (overlayFrameIndices == null) {
      throw new Exception("No overlay frame indices specified");
    }
    if (overlayFrameIndices.length < model.getRowCount()) {
      throw new Exception("Incomplete number of overlay frame indices specified (expected: " + model.getRowCount()
          + ", found: " + overlayFrameIndices.length + ")");
    }

    // shortcut
    if (model.isEmpty()) {
      return srcFrameEntry;
    }

    PseudoBamFrameEntry retVal = null;
    // preparations
    final BamDecoder[] decoders = new BamDecoder[model.getRowCount()];
    final BamControl[] controls = new BamControl[decoders.length];
    for (int i = 0; i < decoders.length; i++) {
      final BamDecoder decoder = bamCache.get(model.get(i).getValue0());
      if (decoder == null) {
        throw new Exception("Decoder not available for BAM resource at index " + i);
      }
      decoders[i] = decoder;
      controls[i] = decoders[i].createControl();
    }

    // calculating output frame dimension
    int left = Integer.MAX_VALUE, top = Integer.MAX_VALUE, right = Integer.MIN_VALUE, bottom = Integer.MIN_VALUE;
    int cx = srcFrameEntry.getCenterX();
    int cy = srcFrameEntry.getCenterY();
    left = Math.min(left, (cx < 0) ? cx : -cx);
    top = Math.min(top,  (cy < 0) ? cy : -cy);
    right = Math.max(right, srcFrameEntry.getWidth() - cx);
    bottom = Math.max(bottom, srcFrameEntry.getHeight() - cy);

    for (int i = 0; i < decoders.length; i++) {
      if (i < overlayFrameIndices.length) {
        final FrameEntry frameInfo = decoders[i].getFrameInfo(overlayFrameIndices[i]);
        cx = frameInfo.getCenterX();
        cy = frameInfo.getCenterY();
        left = Math.min(left, (cx < 0) ? cx : -cx);
        top = Math.min(top,  (cy < 0) ? cy : -cy);
        right = Math.max(right, frameInfo.getWidth() - cx);
        bottom = Math.max(bottom, frameInfo.getHeight() - cy);
      }
    }

    final int frameWidth = right - left;
    final int frameHeight = bottom - top;
    final Point center = new Point(-left, -top);
    final BufferedImage dstImage = ColorConvert.createCompatibleImage(frameWidth, frameHeight, true);

    // composing frame data
    // source BAM frame (simple copy)
    Graphics2D g = dstImage.createGraphics();
    try {
      g.drawImage(srcFrameEntry.getFrame(), -left - srcFrameEntry.getCenterX(), -top - srcFrameEntry.getCenterY(), null);
    } finally {
      g.dispose();
      g = null;
    }

    // overlay BAM frames
    if (decoders.length > 0) {
      final BufferedImage srcImage = ColorConvert.createCompatibleImage(frameWidth, frameHeight, true);
      final int[] dstBuffer = ((DataBufferInt) dstImage.getRaster().getDataBuffer()).getData();
      for (int i = 0; i < decoders.length; i++) {
        final BamDecoder decoder = decoders[i];
        final BamControl control = controls[i];
        final OverlayMode mode = model.get(i).getValue1();
        final FrameEntry info = decoder.getFrameInfo(overlayFrameIndices[i]);
        decoder.frameGet(control, overlayFrameIndices[i], srcImage);
        final int[] srcBuffer = ((DataBufferInt) srcImage.getRaster().getDataBuffer()).getData();

        int x1 = -left -info.getCenterX();
        int y1 = -top - info.getCenterY();
        boolean skip = false;
        for (int y = 0, h = info.getHeight(); y < h && !skip; y++) {
          for (int x = 0, w = info.getWidth(); x < w && !skip; x++) {
            // get dest buffer pixel -> src
            final int ofsDestBuffer = (y1 + y) * frameWidth + (x1 + x);
            // get overlay frame pixel -> dst
            final int ofsSrcBuffer = y * frameWidth + x;
            if (ofsDestBuffer >= 0 && ofsDestBuffer < dstBuffer.length && ofsSrcBuffer >= 0 && ofsSrcBuffer < srcBuffer.length) {
              // calculate resulting pixel and set it in dest buffer
              final int pixel = mode.getFunction().apply(dstBuffer[ofsDestBuffer], srcBuffer[ofsSrcBuffer]);
              dstBuffer[ofsDestBuffer] = pixel;
            } else {
              Logger.debug("decoder={}, frame={}, dstBuffer.length={}, ofsDestBuffer={}, srcBuffer.length={}, ofsSrcBuffer={}",
                  i, overlayFrameIndices[i], dstBuffer.length, ofsDestBuffer, srcBuffer.length, ofsSrcBuffer);
              if (ofsDestBuffer >= 0 && ofsDestBuffer < dstBuffer.length) {
                throw new IndexOutOfBoundsException("Destination buffer: offset out of bounds (offset=" + ofsDestBuffer
                    + ", buffer size=" + dstBuffer.length);
              } else {
                throw new IndexOutOfBoundsException("Source buffer: offset out of bounds (offset=" + ofsSrcBuffer
                    + ", buffer size=" + srcBuffer.length);
              }
            }
          }
        }
      }
    }

    retVal = new PseudoBamFrameEntry(dstImage, center.x, center.y);

    return retVal;
  }

  /**
   * Returns the non-transparent destination pixel. Otherwise the source pixel is returned.
   *
   * @param src Pixel value of the current animation frame.
   * @param dst Pixel value of the overlaid animation frame.
   * @return Pixel value of the blend operation.
   */
  private static int pixelOpNormal(int src, int dst) {
    final int a2 = (dst >> 24) & 0xff;
    if (a2 > 0 && a2 < 0xff) {
      // performing alpha blending
      final int a1 = (src >> 24) & 0xff;
      final int r1 = (src >> 16) & 0xff;
      final int g1 = (src >> 8) & 0xff;
      final int b1 = src & 0xff;

      final int r2 = (dst >> 16) & 0xff;
      final int g2 = (dst >> 8) & 0xff;
      final int b2 = dst & 0xff;

      final int r = (((r1 * a1) >> 8) + ((r2 * a2 * (256 - a1)) >> 16)) & 0xff;
      final int g = (((g1 * a1) >> 8) + ((g2 * a2 * (256 - a1)) >> 16)) & 0xff;
      final int b = (((b1 * a1) >> 8) + ((b2 * a2 * (256 - a1)) >> 16)) & 0xff;
      final int a = (a1 + ((a2 * (256 - a1)) >> 8)) & 0xff;
      return (a << 24) | (r << 16) | (g << 8) | b;
    } else {
      return ((dst & 0xff000000) != 0) ? dst : src;
    }
  }

  /**
   * Always returns the destination pixel.
   *
   * @param src Pixel value of the current animation frame.
   * @param dst Pixel value of the overlaid animation frame.
   * @return Pixel value of the blend operation.
   */
  private static int pixelOpForced(int src, int dst) {
    return dst;
  }

  /**
   * Returns the destination pixel if the source pixel is non-transparent. Otherwise the source pixel is returned.
   *
   * @param src Pixel value of the current animation frame.
   * @param dst Pixel value of the overlaid animation frame.
   * @return Pixel value of the blend operation.
   */
  private static int pixelOpInclusive(int src, int dst) {
    final int a1 = (src >> 24) & 0xff;
    if (a1 > 0 && a1 < 0xff) {
      // performing alpha blending
      final int r1 = (src >> 16) & 0xff;
      final int g1 = (src >> 8) & 0xff;
      final int b1 = src & 0xff;

      final int a2 = (dst >> 24) & 0xff;
      final int r2 = (dst >> 16) & 0xff;
      final int g2 = (dst >> 8) & 0xff;
      final int b2 = dst & 0xff;

      final int r = (((r1 * a1) >> 8) + ((r2 * a2 * (256 - a1)) >> 16)) & 0xff;
      final int g = (((g1 * a1) >> 8) + ((g2 * a2 * (256 - a1)) >> 16)) & 0xff;
      final int b = (((b1 * a1) >> 8) + ((b2 * a2 * (256 - a1)) >> 16)) & 0xff;
      final int a = (a1 + ((a2 * (256 - a1)) >> 8)) & 0xff;
      return (a << 24) | (r << 16) | (g << 8) | b;
    } else {
      return ((src & 0xff000000) != 0) ? dst : src;
    }
  }

  /**
   * Returns the destination pixel if the source pixel is transparent. Otherwise the source pixel is returned.
   *
   * @param src Pixel value of the current animation frame.
   * @param dst Pixel value of the overlaid animation frame.
   * @return Pixel value of the blend operation.
   */
  private static int pixelOpExclusive(int src, int dst) {
    final int a1 = (src >> 24) & 0xff;
    if (a1 > 0 && a1 < 0xff) {
      // performing alpha blending
      final int r1 = (src >> 16) & 0xff;
      final int g1 = (src >> 8) & 0xff;
      final int b1 = src & 0xff;

      final int a2 = (dst >> 24) & 0xff;
      final int r2 = (dst >> 16) & 0xff;
      final int g2 = (dst >> 8) & 0xff;
      final int b2 = dst & 0xff;

      final int r = (((r1 * a1) >> 8) + ((r2 * a2 * (256 - a1)) >> 16)) & 0xff;
      final int g = (((g1 * a1) >> 8) + ((g2 * a2 * (256 - a1)) >> 16)) & 0xff;
      final int b = (((b1 * a1) >> 8) + ((b2 * a2 * (256 - a1)) >> 16)) & 0xff;
      final int a = (a1 + ((a2 * (256 - a1)) >> 8)) & 0xff;
      return (a << 24) | (r << 16) | (g << 8) | b;
    } else {
      return ((src & 0xff000000) == 0) ? dst : src;
    }
  }

  // TODO: move method to a more suitable class (e.g. ColorConvert) and make it public
  /**
   * Returns a {@link PseudoBamDecoder} instance where all frames are fully BAM V1 compatible. Does nothing if the
   * decoder is already BAM V1 compatible.
   *
   * @param decoder               {@link PseudoBamDecoder} instance to convert.
   * @param useAlpha              Specifies whether the generated palette supports alpha values.
   * @param transparencyThreshold Threshold to determine whether a color is considered fully transparent. This value is
   *                                only considered if {@code useAlpha} is {@code false}.
   * @param reservedColors        Optional array of colors that should be assigned to fixed palette indices, starting at
   *                                palette index 1. Magic color "green" is ignored.
   * @return {@link PseudoBamDecoder} instance with palette-based frames and the same cycle configuration as the source
   *         decoder.
   * @throws Exception if an unrecoverable error occurs.
   */
  private static PseudoBamDecoder convertToPalettedBam(PseudoBamDecoder decoder, boolean useAlpha,
      int transparencyThreshold, int... reservedColors) throws Exception {
    boolean isPalette = true;
    final List<PseudoBamFrameEntry> framesList = decoder.getFramesList();
    for (int i = 0, size = framesList.size(); isPalette && i < size; i++) {
      isPalette = (framesList.get(i).getFrame().getType() == BufferedImage.TYPE_BYTE_INDEXED);
    }

    if (isPalette) {
      return decoder;
    }

    // preparing palette
    final LinkedHashMap<Integer, Integer> colorMap = new LinkedHashMap<>();
    for (int frameIdx = 0, frameCount = decoder.frameCount(); frameIdx < frameCount; frameIdx++) {
      final PseudoBamFrameEntry info = decoder.getFrameInfo(frameIdx);
      PseudoBamDecoder.registerColors(colorMap, info.getFrame(), true);
    }

    final int[] subPalette = decoder.createGlobalPalette(colorMap);
    final int[] newPalette = new int[256];
    newPalette[0] = 0x0000ff00;
    System.arraycopy(subPalette, 0, newPalette, 1, subPalette.length);

    // configuring reserved colors
    final double alphaWeight = useAlpha ? 1.0 : 0.0;
    if (reservedColors.length > 0) {
      int curIndex = 1;
      for (final int color : reservedColors) {
        if ((color & 0xffffff) != 0x00ff00) {
          int idx = ColorConvert.getNearestColor(color, newPalette, alphaWeight, ColorConvert.COLOR_DISTANCE_CIE94, true);
          if (idx != curIndex) {
            final int tmp = newPalette[idx];
            newPalette[idx] = newPalette[curIndex];
            newPalette[curIndex] = tmp;
            curIndex++;
          }
        }
      }
    }

    final PseudoBamDecoder newDecoder = new PseudoBamDecoder();

    // performing color quantization
    final HashMap<Integer, Byte> colorCache = new HashMap<>(4096);
    for (int i = 1; i < newPalette.length; i++) {
      colorCache.put(newPalette[i], (byte) i);
    }
    final IndexColorModel cm = new IndexColorModel(8, 256, newPalette, 0, useAlpha, 0, DataBuffer.TYPE_BYTE);
    for (final PseudoBamFrameEntry frameInfo : framesList) {
      final BufferedImage dstImage = new BufferedImage(frameInfo.getWidth(), frameInfo.getHeight(), BufferedImage.TYPE_BYTE_INDEXED, cm);
      final byte[] dstBuf = ((DataBufferByte) dstImage.getRaster().getDataBuffer()).getData();

      final BufferedImage srcImage = frameInfo.getFrame();
      if (srcImage.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
        // processing palette-based source image
        final byte[] srcBuf = ((DataBufferByte) srcImage.getRaster().getDataBuffer()).getData();
        final IndexColorModel srcColorModel = (IndexColorModel) srcImage.getColorModel();
        final int[] srcColors = new int[srcColorModel.getMapSize()];
        srcColorModel.getRGBs(srcColors);
        for (int ofs = 0; ofs < srcBuf.length; ofs++) {
          final int srcColorIdx = srcBuf[ofs] & 0xff;
          final int color = srcColors[(srcColorIdx < srcColors.length) ? srcColorIdx : 0];
          if (!PseudoBamDecoder.isTransparentColor(color, transparencyThreshold)) {
            final byte colorIdx = colorCache.computeIfAbsent(color, c -> {
              return (byte) ColorConvert.getNearestColor(color, newPalette, alphaWeight, ColorConvert.COLOR_DISTANCE_CIE94);
            });
            dstBuf[ofs] = colorIdx;
          }
        }
      } else {
        // processing truecolor source image
        final int[] srcBuf = ((DataBufferInt) srcImage.getRaster().getDataBuffer()).getData();
        for (int ofs = 0; ofs < srcBuf.length; ofs++) {
          final int color = srcBuf[ofs];
          if (!PseudoBamDecoder.isTransparentColor(color, transparencyThreshold)) {
            final byte colorIdx = colorCache.computeIfAbsent(color, c -> {
              return (byte) ColorConvert.getNearestColor(color, newPalette, alphaWeight, ColorConvert.COLOR_DISTANCE_CIE94);
            });
            dstBuf[ofs] = colorIdx;
          }
        }
      }

      // adding frame
      final int dstFrameIdx = newDecoder.frameAdd(dstImage, new Point(frameInfo.getCenterX(), frameInfo.getCenterY()));
      final PseudoBamFrameEntry dstFrameInfo = newDecoder.getFrameInfo(dstFrameIdx);
      dstFrameInfo.setOption(PseudoBamDecoder.OPTION_BOOL_TRANSPARENTGREENFORCED, true);
      for (final String option : frameInfo.getOptionNames()) {
        dstFrameInfo.setOption(option, frameInfo.getOption(option));
      }
    }

    // transfering bam cycles
    newDecoder.getCyclesList().addAll(decoder.getCyclesList());

    return newDecoder;
  }

  /**
   * Specialization of the {@link BiFunction} interface for combining two source pixels to a destination pixel.
   * <p>The first parameter specifies the pixel of the current animation frame.</p>
   * <p>The second parameter specifies the pixel of the overlaid animation frame.</p>
   * <p>The resulting pixel value is stored in the output animation frame.</p>
   * <p>Pixel format is {@code 0xAARRGGBB}.</p>
   */
  @FunctionalInterface
  public static interface OverlayFunc extends BiFunction<Integer, Integer, Integer> {
  }

  /**
   * Helper class that encodes a BAM resource path into a universal (URI) format.
   */
  public static class ResourcePath {
    /**
     * URI scheme for biffed game resources. The resource path is expected to contain the resource name as root element.
     */
    public static final String URI_SCHEME_RESOURCE = "res";
    /**
     * URI scheme for relative file path definitions. The path is expected to be relative to the game's root folder.
     */
    public static final String URI_SCHEME_RELPATH = "rel";

    private final URI path;

    /**
     * Initializes the {@code ResourcePath} object with the specified resource.
     *
     * @param entry {@link ResourceEntry} containing the game resource or file path of the BAM resource.
     * @throws Exception If the resource location could not be determined.
     */
    public ResourcePath(ResourceEntry entry) throws Exception {
      Objects.requireNonNull(entry);
      if (entry instanceof BIFFResourceEntry) {
        this.path = new URI(URI_SCHEME_RESOURCE, "/" + entry.getResourceName(), null);
      } else {
        final Path resPath = entry.getActualPath();
        if (resPath.startsWith(Profile.getGameRoot())) {
          // try to store relative path if possible
          final String relPath;
          if ("\\".equals(Platform.FILE_SEPARATOR)) {
            relPath = Profile.getGameRoot().relativize(resPath).toString().replace('\\', '/');
          } else {
            relPath = Profile.getGameRoot().relativize(resPath).toString();
          }
          this.path = new URI(URI_SCHEME_RELPATH, "/" + relPath, null);
        } else {
          this.path = resPath.toUri();
        }
      }
    }

    /**
     * Initializes the {@code ResourcePath} object with the specified {@link URI}.
     *
     * @param uri {@link URI} instance that defines a resource location.
     */
    public ResourcePath(URI uri) {
      this.path = Objects.requireNonNull(uri);
    }

    /** Returns the {@link URI} of the resource stored inside this object. */
    public URI getURI() {
      return path;
    }

    /** Returns {@code true} if this object points to a (biffed) game resource. */
    public boolean isResource() {
      return URI_SCHEME_RESOURCE.equals(path.getScheme());
    }

    /** Returns {@code true} if this object points to a relative file path. */
    public boolean isRelativePath() {
      return URI_SCHEME_RELPATH.equals(path.getScheme());
    }

    /**
     * Decodes the ResourceString URI and returns it as a {@link ResourceEntry} object.
     *
     * @return A {@link ResourceEntry} object with the resource path definition. Returns {@code null} if the resource
     * could not be resolved.
     * @throws IllegalArgumentException if the object contains an illegal path definition.
     * @throws FileSystemNotFoundException if the filesystem does not exist (e.g. a virtual DLC filesystem).
     */
    public ResourceEntry getResourceEntry() {
      if (isResource()) {
        // remove leading slash and decode semicolon
        final String resourceName = path.getPath().substring(1).replace("%3B", ";");
        return ResourceFactory.getResourceEntry(resourceName);
      } else if (isRelativePath()) {
        // remove leading slash and decode semicolon
        final String resourceName = path.getPath().substring(1).replace("%3B", ";");
        return new FileResourceEntry(Profile.getGameRoot().resolve(resourceName).normalize());
      } else {
        return new FileResourceEntry(Paths.get(path).normalize());
      }
    }

    @Override
    public int hashCode() {
      return Objects.hash(path);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      ResourcePath other = (ResourcePath)obj;
      return Objects.equals(path, other.path);
    }

    /**
     * Returns the content of the resource path in URI format. Semicolons inside the URI string are encoded
     * to prevent conflicts with the BAM Converter configuration format.
     */
    @Override
    public String toString() {
      return path.toString().replace(";", "%3B");
    }
  }

  /**
   * Table model for the JTable component of the preview UI with a subset of {@link List} methods.
   */
  public static class OverlayTableModel extends AbstractTableModel
      implements Iterable<Couple<ResourceEntry, OverlayMode>> {
    private final List<Couple<ResourceEntry, OverlayMode>> entries = new ArrayList<>();

    public OverlayTableModel() {
    }

    @Override
    public int getRowCount() {
      return entries.size();
    }

    @Override
    public int getColumnCount() {
      return 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      if (rowIndex < 0 || rowIndex >= entries.size()) {
        throw new IndexOutOfBoundsException("Row out of bounds: " + rowIndex);
      }
      return (columnIndex == 0) ? entries.get(rowIndex).getValue0() : entries.get(rowIndex).getValue1();
    }

    @Override
    public String getColumnName(int column) {
      return (column == 0) ? "Resource" : "Mode";
    }

    /** Adds a new entry with the specified arguments to the end of the table model. */
    public void add(ResourceEntry resource, OverlayMode mode) {
      add(entries.size(), resource, mode);
    }

    /** Inserts a new entry with the specified argument at the specified position in the table model. */
    public void add(int index, ResourceEntry resource, OverlayMode mode) {
      Objects.requireNonNull(resource);
      Objects.requireNonNull(mode);
      add(index, Couple.with(resource, mode));
    }

    /** Inserts a new entry at the specified position in the table model. */
    public void add(int index, Couple<ResourceEntry, OverlayMode> item) {
      Objects.requireNonNull(item);
      entries.add(index, item);
      fireTableRowsInserted(index, index);
    }

    /** Removes all entries from the table model. */
    public void clear() {
      final int size = entries.size();
      if (size > 0) {
        entries.clear();
        fireTableDataChanged();
      }
    }

    /** Returns the content of the entry at the specified position in the table model. */
    public Couple<ResourceEntry, OverlayMode> get(int index) {
      final Couple<ResourceEntry, OverlayMode> entry = entries.get(index);
      return Couple.with(entry.getValue0(), entry.getValue1());
    }

    /** Returns {@code true} if the table model contains no entries. */
    public boolean isEmpty() {
      return entries.isEmpty();
    }

    /** Returns a read-only iterator over the entries in the table model. */
    @Override
    public Iterator<Couple<ResourceEntry, OverlayMode>> iterator() {
      return Collections.unmodifiableList(entries).iterator();
    }

    /** Removes the entry at the specified position in the table model. */
    public Couple<ResourceEntry, OverlayMode> remove(int index) {
      final Couple<ResourceEntry, OverlayMode> retVal = entries.remove(index);
      fireTableRowsDeleted(index, index);
      return retVal;
    }

    /** Replaces the {@link ResourceEntry} of the entry at the specified position in the table model. */
    public ResourceEntry set(int index, ResourceEntry resource) {
      Objects.requireNonNull(resource);
      final ResourceEntry retVal = entries.get(index).setValue0(resource);
      fireTableCellUpdated(index, 0);
      return retVal;
    }

    /** Replaces the {@link OverlayMode} of the entry at the specified position in the table model. */
    public OverlayMode set(int index, OverlayMode mode) {
      Objects.requireNonNull(mode);
      final OverlayMode retVal = entries.get(index).setValue1(mode);
      fireTableCellUpdated(index, 1);
      return retVal;
    }

    /** Replaces the content of the entry at the specified position in the table model. */
    public Couple<ResourceEntry, OverlayMode> set(int index, ResourceEntry resource, OverlayMode mode) {
      Objects.requireNonNull(resource);
      Objects.requireNonNull(mode);
      final Couple<ResourceEntry, OverlayMode> entry = entries.get(index);
      final Couple<ResourceEntry, OverlayMode> retVal = Couple.with(entry.getValue0(), entry.getValue1());
      entry.setValue0(resource);
      entry.setValue1(mode);
      fireTableRowsUpdated(index, index);
      return retVal;
    }
  }

  /**
   * Interactive dialog for selecting an {@link OverlayMode}.
   */
  private static class OverlayModeSelection extends NewAbstractSettings implements ItemListener {
    private static final String NO_CHANGE_NAME = "(No change)";
    private static final String NO_CHANGE_DESC = "Keep the current overlay modes for all selected entries.";

    private JComboBox<OverlayMode> cbMode;
    private JTextArea descArea;

    public OverlayModeSelection(Window parent, OverlayMode mode) {
      super(parent, "Select Overlay Mode");
      init(mode);
    }

    @Override
    public OverlayMode getConfig() {
      return cbMode.getItemAt(cbMode.getSelectedIndex());
    }

    private void init(OverlayMode mode) {
      acceptButton().setText("Select");
      acceptButton().setIcon(Icons.ICON_CHECK_16.getIcon());

      final JLabel label = new JLabel("Select overlay mode:");
      label.setLabelFor(cbMode);
      label.setDisplayedMnemonic(KeyEvent.VK_S);

      final OverlayMode[] items;
      if (mode == null) {
        items = new OverlayMode[OverlayMode.values().length + 1];
        items[0] = null;
        System.arraycopy(OverlayMode.values(), 0, items, 1, OverlayMode.values().length);
      } else {
        items = OverlayMode.values();
      }
      cbMode = new JComboBox<>(items);
      cbMode.setRenderer(new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
            boolean cellHasFocus) {
          final String item = (value != null) ? value.toString() : NO_CHANGE_NAME;
          return super.getListCellRendererComponent(list, item, index, isSelected, cellHasFocus);
        }
      });
      if (mode != null) {
        cbMode.setSelectedItem(OverlayMode.getOrDefault(mode));
      } else {
        cbMode.setSelectedIndex(0);
      }
      cbMode.addItemListener(this);

      descArea = new JTextArea();
      descArea.setLineWrap(true);
      descArea.setWrapStyleWord(true);
      descArea.setEditable(false);
      descArea.setBackground(label.getBackground());
      final JScrollPane scroll = new JScrollPane(descArea);
      scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
      scroll.getVerticalScrollBar().setUnitIncrement(16);
      scroll.setPreferredSize(new Dimension(label.getPreferredSize().width * 7 / 4, 6 * label.getPreferredSize().height));
      scroll.setMinimumSize(scroll.getPreferredSize());
      updateDesc(cbMode.getItemAt(cbMode.getSelectedIndex()));

      final JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 5));
      panelButtons.add(acceptButton());
      panelButtons.add(rejectButton());

      final GridBagConstraints c = new GridBagConstraints();
      final JPanel panel = new JPanel(new GridBagLayout());
      ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
          new Insets(10, 10, 3, 10), 0, 0);
      panel.add(label, c);
      ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 10, 10, 10), 0, 0);
      panel.add(cbMode, c);
      ViewerUtil.setGBC(c, 0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH,
          new Insets(0, 10, 10, 10), 0, 0);
      panel.add(scroll, c);
      ViewerUtil.setGBC(c, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LAST_LINE_END, GridBagConstraints.NONE,
          new Insets(0, 5, 5, 5), 0, 0);
      panel.add(panelButtons, c);

      final Container contentPane = getContentPane();
      contentPane.add(panel);

      pack();
      setMinimumSize(getPreferredSize());
      setLocationRelativeTo(getParent());
      setCancelOnEscape(true);
      setVisible(true);
    }

    private void updateDesc(OverlayMode mode) {
      final String desc = (mode != null) ? mode.getDesc() : NO_CHANGE_DESC;
      descArea.setText(desc);
      descArea.setCaretPosition(0);
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
      if (e.getItem() instanceof OverlayMode) {
        final OverlayMode mode = (OverlayMode) e.getItem();
        updateDesc(mode);
      }
    }
  }
}
