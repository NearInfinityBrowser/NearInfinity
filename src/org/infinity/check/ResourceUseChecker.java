// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.check;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.NearInfinity;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.StringRef;
import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.SortableTable;
import org.infinity.gui.TableItem;
import org.infinity.gui.ViewFrame;
import org.infinity.gui.WindowBlocker;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
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
import org.infinity.search.AbstractSearcher;
import org.infinity.util.Misc;
import org.infinity.util.StringTable;

public final class ResourceUseChecker extends AbstractSearcher implements Runnable, ListSelectionListener, ActionListener
{
  private static final Pattern RESREFPATTERN = Pattern.compile("\\w{3,8}");
  private static final String[] FILETYPES = {"2DA", "ARE", "BCS", "BS", "CHR", "CHU", "CRE",
                                             "DLG", "EFF", "INI", "ITM", "PRO", "SPL", "STO",
                                             "VEF", "VVC", "WED", "WMP"};
  private static final String[] CHECKTYPES = {"ARE", "BAM", "BCS", "CRE", "DLG", "EFF", "ITM", "PRO",
                                              "SPL", "STO", "TIS", "VEF", "VVC", "WAV", "WED"};
  private final ChildFrame selectframe = new ChildFrame("Find unused files", true);
  private final JButton bstart = new JButton("Search", Icons.getIcon(Icons.ICON_FIND_16));
  private final JButton bcancel = new JButton("Cancel", Icons.getIcon(Icons.ICON_DELETE_16));
  private final JRadioButton[] typeButtons = new JRadioButton[CHECKTYPES.length];
  private final List<ResourceEntry> unusedResources = new ArrayList<>();
  private ChildFrame resultFrame;
  private JButton bopen, bopennew, bsave;
  /** List of the {@link UnusedFileTableItem} objects. */
  private SortableTable table;
  private String checkType;

  public ResourceUseChecker(Component parent)
  {
    super(CHECK_MULTI_TYPE_FORMAT, parent);
    ButtonGroup bg = new ButtonGroup();
    JPanel radioPanel = new JPanel(new GridLayout(0, 1));
    for (int i = 0; i < typeButtons.length; i++) {
      typeButtons[i] = new JRadioButton(CHECKTYPES[i]);
      bg.add(typeButtons[i]);
      radioPanel.add(typeButtons[i]);
    }
    typeButtons[0].setSelected(true);
    bstart.setMnemonic('s');
    bcancel.setMnemonic('c');
    bstart.addActionListener(this);
    bcancel.addActionListener(this);
    selectframe.getRootPane().setDefaultButton(bstart);
    selectframe.setIconImage(Icons.getIcon(Icons.ICON_FIND_16).getImage());
    radioPanel.setBorder(BorderFactory.createTitledBorder("Select type to search:"));

    JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    bpanel.add(bstart);
    bpanel.add(bcancel);

    JPanel mainpanel = new JPanel(new BorderLayout());
    mainpanel.add(radioPanel, BorderLayout.CENTER);
    mainpanel.add(bpanel, BorderLayout.SOUTH);
    mainpanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

    JPanel pane = (JPanel)selectframe.getContentPane();
    pane.setLayout(new BorderLayout());
    pane.add(mainpanel, BorderLayout.CENTER);

    selectframe.pack();
    Center.center(selectframe, parent.getBounds());
    selectframe.setVisible(true);
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bstart) {
      selectframe.setVisible(false);
      for (int i = 0; i < typeButtons.length; i++)
        if (typeButtons[i].isSelected()) {
          checkType = CHECKTYPES[i];
          new Thread(this).start();
          return;
        }
    }
    else if (event.getSource() == bcancel)
      selectframe.setVisible(false);
    else if (event.getSource() == bopen) {
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
        Resource resource = ResourceFactory.getResource(resourceEntry);
        new ViewFrame(resultFrame, resource);
      }
    }
    else if (event.getSource() == bsave) {
      JFileChooser fc = new JFileChooser(Profile.getGameRoot().toFile());
      fc.setDialogTitle("Save search result");
      fc.setSelectedFile(new File(fc.getCurrentDirectory(), "result.txt"));
      if (fc.showSaveDialog(resultFrame) == JFileChooser.APPROVE_OPTION) {
        Path output = fc.getSelectedFile().toPath();
        if (Files.exists(output)) {
          String options[] = {"Overwrite", "Cancel"};
          if (JOptionPane.showOptionDialog(resultFrame, output + " exists. Overwrite?",
                                           "Save result", JOptionPane.YES_NO_OPTION,
                                           JOptionPane.WARNING_MESSAGE, null, options, options[0]) != 0)
            return;
        }
        try (BufferedWriter bw = Files.newBufferedWriter(output)) {
          bw.write("Result of CRE inventory check"); bw.newLine();
          bw.write("Number of hits: " + table.getRowCount()); bw.newLine();
          for (int i = 0; i < table.getRowCount(); i++) {
            bw.write(table.getTableItemAt(i).toString()); bw.newLine();
          }
          JOptionPane.showMessageDialog(resultFrame, "Result saved to " + output, "Save complete",
                                        JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
          JOptionPane.showMessageDialog(resultFrame, "Error while saving " + output,
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
    final WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    blocker.setBlocked(true);
    try {
      final ArrayList<ResourceEntry> files = new ArrayList<>();
      for (final String fileType : FILETYPES) {
        files.addAll(ResourceFactory.getResources(fileType));
      }

      unusedResources.addAll(ResourceFactory.getResources(checkType));
      if (runSearch("Searching", files)) {
        return;
      }

      if (unusedResources.isEmpty()) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No unused " + checkType + "s found",
                                      "Info", JOptionPane.INFORMATION_MESSAGE);
      } else {
        table = new SortableTable(new String[]{"File", "Name"},
                                  new Class<?>[]{ResourceEntry.class, String.class},
                                  new Integer[]{200, 200});
        for (ResourceEntry entry : unusedResources) {
          table.addTableItem(new UnusedFileTableItem(entry));
        }
        table.tableComplete();
        resultFrame = new ChildFrame("Result", true);
        resultFrame.setIconImage(Icons.getIcon(Icons.ICON_FIND_16).getImage());
        bopen = new JButton("Open", Icons.getIcon(Icons.ICON_OPEN_16));
        bopennew = new JButton("Open in new window", Icons.getIcon(Icons.ICON_OPEN_16));
        bsave = new JButton("Save...", Icons.getIcon(Icons.ICON_SAVE_16));
        bopen.setMnemonic('o');
        bopennew.setMnemonic('n');
        bsave.setMnemonic('s');
        resultFrame.getRootPane().setDefaultButton(bopennew);
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.add(bopen);
        panel.add(bopennew);
        panel.add(bsave);
        JLabel count = new JLabel(table.getRowCount() + " unused " + checkType + "s found", JLabel.CENTER);
        count.setFont(count.getFont().deriveFont((float)count.getFont().getSize() + 2.0f));
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
                if (resource instanceof AbstractStruct) {
                  ((AbstractStruct)resource).getViewer().selectEntry((String)table.getValueAt(row, 1));
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
        checkDialog((DlgResource)resource);
      } else if (resource instanceof BcsResource) {
        checkScript((BcsResource)resource);
      } else if (resource instanceof PlainTextResource) {
        checkTextfile((PlainTextResource)resource);
      } else if (resource != null) {
        checkStruct((AbstractStruct)resource);
      }
      advanceProgress();
    };
  }

  private void checkDialog(DlgResource dialog)
  {
    for (StructEntry entry : dialog.getList()) {
      if (entry instanceof ResourceRef) {
        checkResourceRef((ResourceRef)entry);
      }
      else if (entry instanceof AbstractCode) {
        try {
          checkCode((AbstractCode)entry);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      else if (checkType.equalsIgnoreCase("WAV") &&
               (entry instanceof State || entry instanceof Transition)) {
        for (StructEntry e : ((AbstractStruct)entry).getFlatList()) {
          if (e instanceof StringRef) {
            checkSound((StringRef)e);
          }
        }
      }
    }
  }

  private void checkScript(BcsResource script)
  {
    final Decompiler decompiler = new Decompiler(script.getCode(), true);
    decompiler.setGenerateComments(false);
    decompiler.setGenerateResourcesUsed(true);
    try {
      decompiler.decompile();
      for (final ResourceEntry entry : decompiler.getResourcesUsed()) {
        synchronized (unusedResources) {
          unusedResources.remove(entry);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void checkStruct(AbstractStruct struct)
  {
    for (StructEntry entry : struct.getFlatList()) {
      if (entry instanceof ResourceRef) {
        checkResourceRef((ResourceRef)entry);
      }
      else if (checkType.equalsIgnoreCase("WAV") && entry instanceof StringRef) {
        checkSound((StringRef)entry);
      }
    }
  }

  private void checkTextfile(PlainTextResource text)
  {
    final Matcher m = RESREFPATTERN.matcher(text.getText());
    while (m.find()) {
      removeEntries(m.group() + '.' + checkType);
    }
  }

  /**
   * Performs code checking. This method can be called from several threads
   *
   * @param code Code to action or trigger in dialog. Never {@code null}
   *
   * @throws Exception If {@code script} contains invalid code
   */
  private void checkCode(AbstractCode code) throws Exception {
    final ScriptType type = code instanceof Action ? ScriptType.ACTION : ScriptType.TRIGGER;
    final Compiler compiler = new Compiler(code.toString(), type);
    final String compiled = compiler.getCode();
    final Decompiler decompiler = new Decompiler(compiled, ScriptType.BCS, true);

    decompiler.setGenerateComments(false);
    decompiler.setGenerateResourcesUsed(true);
    decompiler.setScriptType(type);
    decompiler.decompile();

    for (final ResourceEntry entry : decompiler.getResourcesUsed()) {
      synchronized (unusedResources) {
        unusedResources.remove(entry);
      }
    }
  }

  private void checkResourceRef(ResourceRef ref) {
    if (checkType.equalsIgnoreCase(ref.getType())) {
      removeEntries(ref.getResourceName());
    }
  }

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
   *
   * @param name Resource name that must be erased from unused list
   */
  private void removeEntries(String name) {
    synchronized (unusedResources) {
      for (final Iterator<ResourceEntry> it = unusedResources.iterator(); it.hasNext();) {
        if (it.next().toString().equalsIgnoreCase(name)) {
          it.remove();
          break;
        }
      }
    }
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class UnusedFileTableItem implements TableItem
  {
    private final ResourceEntry file;

    private UnusedFileTableItem(ResourceEntry file)
    {
      this.file = file;
    }

    @Override
    public Object getObjectAt(int columnIndex)
    {
      if (columnIndex == 0)
        return file;
      return file.getSearchString();
    }

    @Override
    public String toString()
    {
      return String.format("File: %s  Name: %s", file.toString(), file.getSearchString());
    }
  }
}
