// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.check;

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
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import org.infinity.NearInfinity;
import org.infinity.datatype.StringRef;
import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.SortableTable;
import org.infinity.gui.TableItem;
import org.infinity.gui.ViewFrame;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.TextResource;
import org.infinity.resource.are.AutomapNote;
import org.infinity.resource.bcs.BcsResource;
import org.infinity.resource.bcs.Compiler;
import org.infinity.resource.bcs.Decompiler;
import org.infinity.resource.dlg.AbstractCode;
import org.infinity.resource.dlg.Action;
import org.infinity.resource.dlg.DlgResource;
import org.infinity.resource.gam.JournalEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.text.PlainTextResource;
import org.infinity.util.Debugging;
import org.infinity.util.Misc;
import org.infinity.util.StringResource;
import org.infinity.util.io.FileNI;
import org.infinity.util.io.FileWriterNI;
import org.infinity.util.io.PrintWriterNI;

public class StrrefIndexChecker extends ChildFrame implements ActionListener, ListSelectionListener,
                                                              Runnable
{
  private static final String FMT_PROGRESS = "Checking %ss...";
  private static final String[] FILETYPES = {"2DA", "ARE", "BCS", "BS", "CHR", "CHU", "CRE", "DLG",
                                             "EFF", "GAM", "INI", "ITM", "SPL", "SRC", "STO", "WMP"};
  private final ChildFrame resultFrame = new ChildFrame("Illegal strrefs found", true);
  private final JButton bstart = new JButton("Check", Icons.getIcon(Icons.ICON_FIND_16));
  private final JButton bcancel = new JButton("Cancel", Icons.getIcon(Icons.ICON_DELETE_16));
  private final JButton binvert = new JButton("Invert", Icons.getIcon(Icons.ICON_REFRESH_16));
  private final JButton bopen = new JButton("Open", Icons.getIcon(Icons.ICON_OPEN_16));
  private final JButton bopennew = new JButton("Open in new window", Icons.getIcon(Icons.ICON_OPEN_16));
  private final JButton bsave = new JButton("Save...", Icons.getIcon(Icons.ICON_SAVE_16));
  private final JCheckBox[] boxes = new JCheckBox[FILETYPES.length];
  private final List<ResourceEntry> files = new ArrayList<ResourceEntry>();

  private SortableTable table;
  private int strrefCount;
  private ProgressMonitor progress;
  private int progressIndex;

  public StrrefIndexChecker()
  {
    super("Find illegal strrefs");
    setIconImage(Icons.getIcon(Icons.ICON_REFRESH_16).getImage());

    List<Class<? extends Object>> colClasses = new ArrayList<Class<? extends Object>>(3);
    colClasses.add(Object.class); colClasses.add(Object.class); colClasses.add(Object.class);
    table = new SortableTable(Arrays.asList(new String[]{"File", "Offset / Line:Pos", "Strref"}),
                              colClasses, Arrays.asList(new Integer[]{200, 100, 100}));

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
    setMinimumSize(getPreferredSize());
    Center.center(this, NearInfinity.getInstance().getBounds());
    setVisible(true);
  }

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bstart) {
      setVisible(false);
      for (int i = 0; i < FILETYPES.length; i++) {
        if (boxes[i].isSelected()) {
          files.addAll(ResourceFactory.getResources(FILETYPES[i]));
        }
      }
      if (files.size() > 0) {
        new Thread(this).start();
      }
    } else if (event.getSource() == binvert) {
      for (final JCheckBox cb: boxes) {
        cb.setSelected(!cb.isSelected());
      }
    } else if (event.getSource() == bcancel) {
      setVisible(false);
    } else if (event.getSource() == bopen) {
      int row = table.getSelectedRow();
      if (row >= 0) {
        ResourceEntry entry = (ResourceEntry)table.getValueAt(row, 0);
        NearInfinity.getInstance().showResourceEntry(entry);
      }
    } else if (event.getSource() == bopennew) {
      int row = table.getSelectedRow();
      if (row >= 0) {
        ResourceEntry entry = (ResourceEntry)table.getValueAt(row, 0);
        new ViewFrame(resultFrame, ResourceFactory.getResource(entry));
      }
    } else if (event.getSource() == bsave) {
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
          pw.println("Illegal strref search");
          pw.println("Number of errors: " + table.getRowCount());
          for (int i = 0; i < table.getRowCount(); i++) {
            pw.println(table.getTableItemAt(i).toString());
          }
          pw.close();
          JOptionPane.showMessageDialog(this, "Result saved to " + output, "Save complete",
                                        JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
          JOptionPane.showMessageDialog(this, "Error while saving " + output,"Error",
                                        JOptionPane.ERROR_MESSAGE);
          e.printStackTrace();
        }
      }
    }
  }

//--------------------- End Interface ActionListener ---------------------

//--------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent event)
  {
    bopen.setEnabled(true);
    bopennew.setEnabled(true);
  }

//--------------------- End Interface ListSelectionListener ---------------------

//--------------------- Begin Interface Runnable ---------------------

  @Override
  public void run()
  {
    try {
      StringResource.getStringRef(0);
      strrefCount = StringResource.getMaxIndex();
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
                StrrefEntry item = (StrrefEntry)table.getTableItemAt(row);
                if (item.isText) {
                  ((TextResource)resource).highlightText(item.line, Integer.toString(item.strref));
                } else {
                  ((AbstractStruct)resource).getViewer().selectEntry(item.offset);
                }
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

//--------------------- End Interface Runnable ---------------------

  private void checkDialog(DlgResource dialog)
  {
    if (dialog != null) {
      List<StructEntry> flatList = dialog.getFlatList();
      for (final StructEntry entry: flatList) {
        if (entry instanceof StringRef) {
          int strref = ((StringRef)entry).getValue();
          if (strref < -1 || strref >= strrefCount) {
            synchronized (table) {
              table.addTableItem(new StrrefEntry(dialog.getResourceEntry(), entry.getOffset(), strref));
            }
          }
        } else if (entry instanceof AbstractCode) {
          AbstractCode code = (AbstractCode)entry;
          try {
            Compiler compiler = new Compiler(code.toString(),
                                             (code instanceof Action) ? Compiler.ScriptType.ACTION
                                                                      : Compiler.ScriptType.TRIGGER);
            String compiled = compiler.getCode();
            Decompiler decompiler = new Decompiler(compiled, true);
            if (code instanceof Action) {
              decompiler.setScriptType(Decompiler.ScriptType.ACTION);
            } else {
              decompiler.setScriptType(Decompiler.ScriptType.TRIGGER);
            }
            decompiler.decompile();
            Set<Integer> used = decompiler.getStringRefsUsed();
            for (final Integer stringRef : used) {
              int strref = stringRef.intValue();
              if (strref < -1 || strref >= strrefCount) {
                synchronized (table) {
                  table.addTableItem(new StrrefEntry(dialog.getResourceEntry(), entry.getOffset(), strref));
                }
              }
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    }
  }

  private void checkScript(BcsResource script)
  {
    if (script != null) {
      Decompiler decompiler = new Decompiler(script.getCode(), true);
      decompiler.decompile();
      Set<Integer> used = decompiler.getStringRefsUsed();
      for (final Integer stringRef : used) {
        int strref = stringRef.intValue();
        if (strref < -1 || strref >= strrefCount) {
          // XXX: search routine may produce false positives
          String strrefString = stringRef.toString();
          String source = decompiler.getSource();
          String[] lines = source.split("\r?\n");
          int line = -1, pos = -1, len = -1;
          Pattern pattern = Pattern.compile("\\b" + strrefString + "\\b", Pattern.DOTALL);
          for (int i = 0; i < lines.length; i++) {
            Matcher matcher = pattern.matcher(lines[i]);
            if (matcher.find()) {
              line = i;
              pos = matcher.start();
              len = matcher.end() - pos;
              break;
            }
          }
          synchronized (table) {
            table.addTableItem(new StrrefEntry(script.getResourceEntry(), line + 1, pos + 1, len, strref));
          }
        }
      }
    }
  }

  private void checkStruct(AbstractStruct struct)
  {
    if (struct != null) {
      List<StructEntry> flatList = struct.getFlatList();
      for (final StructEntry entry: flatList) {
        if (entry instanceof StringRef) {
          int strref = ((StringRef)entry).getValue();
          if (strref < -1 || strref >= strrefCount) {
            if (strref >= 3000000 &&
                (entry.getParent() instanceof AutomapNote || entry.getParent() instanceof JournalEntry)) {
              // skip talk override entries
              continue;
            }
            synchronized (table) {
              table.addTableItem(new StrrefEntry(struct.getResourceEntry(), entry.getOffset(), strref));
            }
          }
        }
      }
    }
  }

  private void checkText(PlainTextResource text)
  {
    if (text != null) {
      Pattern pattern = Pattern.compile("\\b\\d+\\b", Pattern.DOTALL);
      String[] lines = text.getText().split("\r?\n");
      for (int i = 0; i < lines.length; i++) {
        Matcher matcher = pattern.matcher(lines[i]);
        while (matcher.find()) {
          int line = i;
          int pos = matcher.start();
          int len = matcher.end() - pos;
          try {
            long strref = Long.parseLong(lines[line].substring(pos, pos + len));
            // skip values out of integer range
            if (strref >= Integer.MIN_VALUE && strref <= Integer.MAX_VALUE) {
              if (strref < -1 || strref > strrefCount) {
                synchronized (table) {
                  table.addTableItem(new StrrefEntry(text.getResourceEntry(), line + 1, pos + 1, len, (int)strref));
                }
              }
            }
          } catch (NumberFormatException e) {
            e.printStackTrace();
          }
        }
      }
    }
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

//-------------------------- INNER CLASSES --------------------------

  private static final class StrrefEntry implements TableItem
  {
    private final boolean isText;
    private final ResourceEntry entry;
    private final int offset;
    private final int line, pos;
    private final int strref;

    /** Constructor for text resources (2DA, BCS, ...). */
    public StrrefEntry(ResourceEntry entry, int line, int pos, int len, int strref)
    {
      this.isText = true;
      this.entry = entry;
      this.line = line;
      this.pos = pos;
      this.strref = strref;
      this.offset = -1;
    }

    /** Constructor for structured resources. */
    public StrrefEntry(ResourceEntry entry, int offset, int strref)
    {
      this.isText = false;
      this.entry = entry;
      this.offset = offset;
      this.strref = strref;
      this.line = this.pos = -1;
    }

    @Override
    public Object getObjectAt(int columnIndex)
    {
      switch (columnIndex) {
        case 0: return entry;
        case 1: return isText ? (Integer.toString(line) + ":" + Integer.toString(pos)) : Integer.toHexString(offset) + 'h';
        default: return Integer.toString(strref);
      }
    }

    @Override
    public String toString()
    {
      StringBuilder sb = new StringBuilder("File: ");
      sb.append(entry.toString());
      if (isText) {
        sb.append("  Line: ").append(line);
        sb.append("  Position: ").append(pos);
      } else {
        sb.append("  Offset: ").append(Integer.toHexString(offset)).append('h');
      }
      sb.append("  Strref: ").append(strref);
      return sb.toString();
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
        if (resource instanceof DlgResource) {
          checkDialog((DlgResource)resource);
        } else if (resource instanceof BcsResource) {
          checkScript((BcsResource)resource);
        } else if (resource instanceof PlainTextResource) {
          checkText((PlainTextResource)resource);
        } else if (resource instanceof AbstractStruct) {
          checkStruct((AbstractStruct)resource);
        }
      }
      advanceProgress(false);
    }
  }
}
