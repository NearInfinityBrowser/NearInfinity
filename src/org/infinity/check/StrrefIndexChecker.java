// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.check;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.TextResource;
import org.infinity.resource.are.AutomapNote;
import org.infinity.resource.bcs.BcsResource;
import org.infinity.resource.bcs.Compiler;
import org.infinity.resource.bcs.Decompiler;
import org.infinity.resource.bcs.ScriptType;
import org.infinity.resource.dlg.AbstractCode;
import org.infinity.resource.dlg.Action;
import org.infinity.resource.dlg.DlgResource;
import org.infinity.resource.gam.JournalEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.text.PlainTextResource;
import org.infinity.search.StringReferenceSearcher;
import org.infinity.util.Misc;
import org.infinity.util.StringTable;

public class StrrefIndexChecker extends AbstractChecker implements ListSelectionListener
{
  private final ChildFrame resultFrame = new ChildFrame("Illegal strrefs found", true);
  private final JButton bopen = new JButton("Open", Icons.getIcon(Icons.ICON_OPEN_16));
  private final JButton bopennew = new JButton("Open in new window", Icons.getIcon(Icons.ICON_OPEN_16));
  private final JButton bsave = new JButton("Save...", Icons.getIcon(Icons.ICON_SAVE_16));

  /** List of the {@link StrrefEntry} objects. */
  private SortableTable table;
  /** Count of strings in the {@link StringTable talk table}. */
  private int strrefCount;

  public StrrefIndexChecker()
  {
    super("Find illegal strrefs", "StrrefIndexChecker", StringReferenceSearcher.FILE_TYPES);

    table = new SortableTable(new String[]{"File", "Offset / Line:Pos", "Strref"},
                              new Class<?>[]{StrrefEntry.class, String.class, Integer.class},
                              new Integer[]{200, 100, 100});
  }

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bopen) {
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
      table.saveCheckResult(resultFrame, "Unknown string references (maximum " + strrefCount + ")");
    } else {
      super.actionPerformed(event);
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
    strrefCount = StringTable.getNumEntries();
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
              final StrrefEntry item = (StrrefEntry)table.getTableItemAt(row);
              if (item.isText && resource instanceof TextResource) {
                ((TextResource)resource).highlightText(item.line, Integer.toString(item.strref));
              } else if (resource instanceof AbstractStruct) {
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
  }

//--------------------- End Interface Runnable ---------------------

  @Override
  protected Runnable newWorker(ResourceEntry entry)
  {
    return () -> {
      final Resource resource = ResourceFactory.getResource(entry);
      if (resource instanceof DlgResource) {
        checkDialog((DlgResource)resource);
      } else if (resource instanceof BcsResource) {
        checkScript((BcsResource)resource);
      } else if (resource instanceof PlainTextResource) {
        checkText((PlainTextResource)resource);
      } else if (resource instanceof AbstractStruct) {
        checkStruct((AbstractStruct)resource);
      }
      advanceProgress();
    };
  }

  private void checkDialog(DlgResource dialog)
  {
    for (final StructEntry entry : dialog.getFlatFields()) {
      if (entry instanceof StringRef) {
        final int strref = ((StringRef)entry).getValue();
        if (strref < -1 || strref >= strrefCount) {
          synchronized (table) {
            table.addTableItem(new StrrefEntry(dialog.getResourceEntry(), entry.getOffset(), strref));
          }
        }
      } else if (entry instanceof AbstractCode) {
        final AbstractCode code = (AbstractCode)entry;
        try {
          final ScriptType type = code instanceof Action ? ScriptType.ACTION : ScriptType.TRIGGER;
          final Compiler compiler = new Compiler(code.getText(), type);

          final Decompiler decompiler = new Decompiler(compiler.getCode(), type, true);
          decompiler.setGenerateComments(false);
          decompiler.setGenerateResourcesUsed(true);
          decompiler.decompile();
          for (final Integer stringRef : decompiler.getStringRefsUsed()) {
            final int strref = stringRef.intValue();
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

  private void checkScript(BcsResource script)
  {
    final Decompiler decompiler = new Decompiler(script.getCode(), true);
    decompiler.setGenerateComments(false);
    decompiler.setGenerateResourcesUsed(true);
    try {
      decompiler.decompile();
      for (final Integer stringRef : decompiler.getStringRefsUsed()) {
        final int strref = stringRef.intValue();
        if (strref < -1 || strref >= strrefCount) {
          // XXX: search routine may produce false positives
          final String strrefString = stringRef.toString();
          final String source = decompiler.getSource();
          final String[] lines = source.split("\r?\n");
          int line = -1, pos = -1;
          final Pattern pattern = Pattern.compile("\\b" + strrefString + "\\b", Pattern.DOTALL);
          for (int i = 0; i < lines.length; i++) {
            final Matcher matcher = pattern.matcher(lines[i]);
            if (matcher.find()) {
              line = i;
              pos = matcher.start();
              break;
            }
          }
          synchronized (table) {
            table.addTableItem(new StrrefEntry(script.getResourceEntry(), line + 1, pos + 1, strref));
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void checkStruct(AbstractStruct struct)
  {
    for (final StructEntry entry : struct.getFlatFields()) {
      if (entry instanceof StringRef) {
        final int strref = ((StringRef)entry).getValue();
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

  private void checkText(PlainTextResource text)
  {
    final String[] lines = text.getText().split("\r?\n");
    for (int i = 0; i < lines.length; i++) {
      final Matcher matcher = StringReferenceSearcher.NUMBER_PATTERN.matcher(lines[i]);
      while (matcher.find()) {
        final int line = i;
        final int pos = matcher.start();
        final int len = matcher.end() - pos;
        try {
          final long strref = Long.parseLong(lines[line].substring(pos, pos + len));
          // skip values out of integer range
          if (strref >= Integer.MIN_VALUE && strref <= Integer.MAX_VALUE) {
            if (strref < -1 || strref > strrefCount) {
              synchronized (table) {
                table.addTableItem(new StrrefEntry(text.getResourceEntry(), line + 1, pos + 1, (int)strref));
              }
            }
          }
        } catch (NumberFormatException e) {
          e.printStackTrace();
        }
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
    public StrrefEntry(ResourceEntry entry, int line, int pos, int strref)
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
        case 1: return isText ? (line + ":" + pos) : Integer.toHexString(offset) + 'h';
        default: return Integer.toString(strref);
      }
    }

    @Override
    public String toString()
    {
      StringBuilder sb = new StringBuilder("File: ");
      sb.append(entry.getResourceName());
      if (isText) {
        sb.append(", Line: ").append(line);
        sb.append(", Position: ").append(pos);
      } else {
        sb.append(", Offset: ").append(Integer.toHexString(offset)).append('h');
      }
      sb.append(", Strref: ").append(strref).append(" (").append(Integer.toHexString(strref)).append("h)");
      return sb.toString();
    }
  }
}
