// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.check;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.NearInfinity;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.StringRef;
import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.SortableTable;
import org.infinity.gui.TableItem;
import org.infinity.gui.ViewFrame;
import org.infinity.gui.WindowBlocker;
import org.infinity.gui.menu.BrowserMenuBar;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.bcs.BcsResource;
import org.infinity.resource.bcs.Compiler;
import org.infinity.resource.bcs.Decompiler;
import org.infinity.resource.bcs.ScriptType;
import org.infinity.resource.dlg.AbstractCode;
import org.infinity.resource.dlg.Action;
import org.infinity.resource.dlg.DlgResource;
import org.infinity.resource.dlg.State;
import org.infinity.resource.dlg.Transition;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.text.PlainTextResource;
import org.infinity.util.Misc;
import org.infinity.util.StringTable;

public final class ResourceUseChecker extends AbstractChecker
    implements Runnable, ListSelectionListener, ActionListener {
  private static final Pattern RESREF_PATTERN = Pattern.compile("\\w{3,8}");

  private static final String[] FILE_TYPES = { "2DA", "ARE", "BCS", "BS", "CHR", "CHU", "CRE", "DLG", "EFF", "INI",
                                               "ITM", "PRO", "SPL", "STO", "VEF", "VVC", "WED", "WMP" };

  private static final String[] CHECK_TYPES = { "ARE", "BAM", "BCS", "CRE", "DLG", "EFF", "ITM", "PRO", "SPL", "STO",
                                                "TIS", "VEF", "VVC", "WAV", "WED" };

  private final TreeMap<String, ResourceEntry> unusedResources = new TreeMap<>(String::compareToIgnoreCase);
  private final HashSet<String> checkTypes = new HashSet<>(CHECK_TYPES.length * 2);

  private ChildFrame resultFrame;
  private JButton bopen;
  private JButton bopennew;
  private JButton bsave;

  /** List of the {@link UnusedFileTableItem} objects. */
  private SortableTable table;

  public ResourceUseChecker(Component parent) {
    super(CHECK_MULTI_TYPE_FORMAT, CHECK_TYPES);
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == bopen) {
      int row = table.getSelectedRow();
      if (row != -1) {
        ResourceEntry resourceEntry = (ResourceEntry) table.getValueAt(row, 0);
        NearInfinity.getInstance().showResourceEntry(resourceEntry);
      }
    } else if (event.getSource() == bopennew) {
      int row = table.getSelectedRow();
      if (row != -1) {
        ResourceEntry resourceEntry = (ResourceEntry) table.getValueAt(row, 0);
        Resource resource = ResourceFactory.getResource(resourceEntry);
        new ViewFrame(resultFrame, resource);
      }
    } else if (event.getSource() == bsave) {
      table.saveCheckResult(resultFrame, "Unused resources");
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
    final WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    blocker.setBlocked(true);
    try {
      final ArrayList<ResourceEntry> files = new ArrayList<>();
      for (final String fileType : FILE_TYPES) {
        files.addAll(ResourceFactory.getResources(fileType));
      }

      checkTypes.clear();
      final List<JCheckBox> typeList = getSelector().getFileTypes();
      for (final JCheckBox cb : typeList) {
        if (cb.isSelected()) {
          checkTypes.add(cb.getText());
        }
      }

      for (final ResourceEntry e : getFiles()) {
        unusedResources.put(e.getResourceName(), e);
      }

      if (runSearch("Searching", files)) {
        return;
      }

      if (unusedResources.isEmpty()) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No unused resources found", "Info",
            JOptionPane.INFORMATION_MESSAGE);
      } else {
        table = new SortableTable(new String[] { "File", "Name" }, new Class<?>[] { ResourceEntry.class, String.class },
            new Integer[] { 200, 200 });
        for (final Entry<String, ResourceEntry> entry : unusedResources.entrySet()) {
          table.addTableItem(new UnusedFileTableItem(entry.getValue()));
        }
        table.tableComplete();
        resultFrame = new ChildFrame("Result", true);
        resultFrame.setIconImage(Icons.ICON_FIND_16.getIcon().getImage());
        bopen = new JButton("Open", Icons.ICON_OPEN_16.getIcon());
        bopennew = new JButton("Open in new window", Icons.ICON_OPEN_16.getIcon());
        bsave = new JButton("Save...", Icons.ICON_SAVE_16.getIcon());
        bopen.setMnemonic('o');
        bopennew.setMnemonic('n');
        bsave.setMnemonic('s');
        resultFrame.getRootPane().setDefaultButton(bopennew);
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.add(bopen);
        panel.add(bopennew);
        panel.add(bsave);
        JLabel count = new JLabel(table.getRowCount() + " unused resources found", SwingConstants.CENTER);
        count.setFont(count.getFont().deriveFont(count.getFont().getSize() + 2.0f));
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
        table.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseReleased(MouseEvent event) {
            if (event.getClickCount() == 2) {
              int row = table.getSelectedRow();
              if (row != -1) {
                ResourceEntry resourceEntry = (ResourceEntry) table.getValueAt(row, 0);
                Resource resource = ResourceFactory.getResource(resourceEntry);
                new ViewFrame(resultFrame, resource);
                if (resource instanceof AbstractStruct) {
                  ((AbstractStruct) resource).getViewer().selectEntry((String) table.getValueAt(row, 1));
                }
              }
            }
          }
        });
        bopen.addActionListener(this);
        bopennew.addActionListener(this);
        bsave.addActionListener(this);
        pane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        table.getSelectionModel().addListSelectionListener(this);
        resultFrame.pack();
        Center.center(resultFrame, NearInfinity.getInstance().getBounds());
        resultFrame.setVisible(true);
      }
    } finally {
      blocker.setBlocked(false);
    }
  }

  // --------------------- End Interface Runnable ---------------------

  @Override
  protected Runnable newWorker(ResourceEntry entry) {
    return () -> {
      final Resource resource = ResourceFactory.getResource(entry);
      if (resource instanceof DlgResource) {
        checkDialog((DlgResource) resource);
      } else if (resource instanceof BcsResource) {
        checkScript((BcsResource) resource);
      } else if (resource instanceof PlainTextResource) {
        checkTextfile((PlainTextResource) resource);
      } else if (resource instanceof AbstractStruct) {
        checkStruct((AbstractStruct) resource);
      }
      advanceProgress();
    };
  }

  private void checkDialog(DlgResource dialog) {
    for (final StructEntry entry : dialog.getFields()) {
      if (entry instanceof ResourceRef) {
        checkResourceRef((ResourceRef) entry);
      } else if (entry instanceof AbstractCode) {
        try {
          final AbstractCode code = (AbstractCode) entry;
          final ScriptType type = code instanceof Action ? ScriptType.ACTION : ScriptType.TRIGGER;
          final Compiler compiler = new Compiler(code.getText(), type);

          checkCode(compiler.getCode(), type);
        } catch (Exception e) {
          synchronized (System.err) {
            e.printStackTrace();
          }
        }
      } else if (checkTypes.contains("WAV") && (entry instanceof State || entry instanceof Transition)) {
        for (final StructEntry e : ((AbstractStruct) entry).getFlatFields()) {
          if (e instanceof StringRef) {
            checkSound((StringRef) e);
          }
        }
      }
    }
  }

  private void checkScript(BcsResource script) {
    try {
      checkCode(script.getCode(), ScriptType.BCS);
    } catch (Exception e) {
      synchronized (System.err) {
        e.printStackTrace();
      }
    }
  }

  private void checkStruct(AbstractStruct struct) {
    for (final StructEntry entry : struct.getFlatFields()) {
      if (entry instanceof ResourceRef) {
        checkResourceRef((ResourceRef) entry);
      } else if (checkTypes.contains("WAV") && entry instanceof StringRef) {
        checkSound((StringRef) entry);
      }
    }
  }

  private void checkTextfile(PlainTextResource text) {
    final Matcher m = RESREF_PATTERN.matcher(text.getText());
    while (m.find()) {
      for (final String checkType : checkTypes) {
        removeEntries(m.group() + '.' + checkType);
      }
    }
  }

  /**
   * Removes from {@link #unusedResources} all resources to which the script code refers.
   * <p>
   * This method can be called from several threads
   *
   * @param compiledCode Compiled code from BCS, dialog action or trigger. Must not be {@code null}
   *
   * @throws Exception If {@code compiledCode} contains invalid code
   */
  private void checkCode(String compiledCode, ScriptType type) throws Exception {
    final Decompiler decompiler = new Decompiler(compiledCode, type, true);
    decompiler.setGenerateComments(false);
    decompiler.setGenerateResourcesUsed(true);
    decompiler.decompile();

    synchronized (unusedResources) {
      for (final ResourceEntry entry : decompiler.getResourcesUsed()) {
        unusedResources.remove(entry.getResourceName());
      }
    }
  }

  /**
   * If type (a.k.a. extension) of the resource equals to the {@link #checkType type of checking resources}, removes
   * specified resource name from {@link #unusedResources}, otherwise do nothing.
   * <p>
   * This method can be called from several threads
   *
   * @param ref Reference to the resource for checking
   */
  private void checkResourceRef(ResourceRef ref) {
    final String type = ref.getType().toUpperCase();
    if (checkTypes.contains(type)) {
      removeEntries(ref.getResourceName());
    }
  }

  /**
   * If string reference has the associated sound, removes this sound from {@link #unusedResources}, otherwise do
   * nothing.
   * <p>
   * This method can be called from several threads
   *
   * @param ref Reference to entry in string table that contains sound file name
   */
  private void checkSound(StringRef ref) {
    final int index = ref.getValue();
    if (index >= 0) {
      final String wav = StringTable.getSoundResource(index);
      if (!wav.isEmpty()) {
        removeEntries(wav + ".WAV");
      }
    }
  }

  /**
   * Removes from {@link #unusedResources} all entries that name equals {@code name}.
   * <p>
   * This method can be called from several threads
   *
   * @param name Resource name that must be erased from unused list
   */
  private void removeEntries(String name) {
    synchronized (unusedResources) {
      unusedResources.remove(name);
    }
  }

  // -------------------------- INNER CLASSES --------------------------

  private static final class UnusedFileTableItem implements TableItem {
    private final ResourceEntry file;

    private UnusedFileTableItem(ResourceEntry file) {
      this.file = file;
    }

    @Override
    public Object getObjectAt(int columnIndex) {
      if (columnIndex == 0) {
        return file;
      }
      return file.getSearchString();
    }

    @Override
    public String toString() {
      return String.format("File: %s, Name: %s", file.getResourceName(), file.getSearchString());
    }
  }
}
