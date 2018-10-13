// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.check;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.NearInfinity;
import org.infinity.datatype.StringRef;
import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.SortableTable;
import org.infinity.gui.TableItem;
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
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.text.PlainTextResource;
import org.infinity.search.SearchClient;
import org.infinity.search.SearchMaster;
import org.infinity.util.Misc;
import org.infinity.util.StringTable;

public final class StringUseChecker extends AbstractChecker implements Runnable, ListSelectionListener, SearchClient, ActionListener
{
  private static final Pattern NUMBERPATTERN = Pattern.compile("\\d+", Pattern.DOTALL);
  private static final String[] FILETYPES = {"2DA", "ARE", "BCS", "BS", "CHR", "CHU", "CRE", "DLG", "EFF",
                                             "INI", "ITM", "SPL", "SRC", "STO", "WMP"};
  private ChildFrame resultFrame;
  private JTextArea textArea;
  /** List of the {@link UnusedStringTableItem} objects. */
  private SortableTable table;
  private boolean[] strUsed;
  private JMenuItem save;

  public StringUseChecker()
  {
    super(MULTI_TYPE_FORMAT);
    new Thread(this).start();
  }

// --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent event)
  {
    if (table.getSelectedRow() == -1)
      textArea.setText(null);
    else {
      TableItem item = table.getTableItemAt(table.getSelectedRow());
      textArea.setText(item.toString());
    }
    textArea.setCaretPosition(0);
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

      strUsed = new boolean[StringTable.getNumEntries() + 1];
      if (runCheck("Searching...", files)) {
        return;
      }

      table = new SortableTable(new String[]{"String", "StrRef"},
                                new Class<?>[]{String.class, Integer.class},
                                new Integer[]{450, 20});
      for (int i = 0; i < strUsed.length; i++) {
        if (!strUsed[i]) {
          table.addTableItem(new UnusedStringTableItem(i));
        }
      }
      if (table.getRowCount() == 0) {
        resultFrame.close();
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No unused strings found",
                                      "Info", JOptionPane.INFORMATION_MESSAGE);
      } else {
        table.tableComplete(1);
        textArea = new JTextArea(10, 40);
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        JScrollPane scrollText = new JScrollPane(textArea);
        resultFrame = new ChildFrame("Result", true);
        save = new JMenuItem("Save");
        save.addActionListener(this);
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(save);
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        resultFrame.setJMenuBar(menuBar);
        resultFrame.setIconImage(Icons.getIcon(Icons.ICON_FIND_16).getImage());
        JLabel count = new JLabel(table.getRowCount() + " unused string(s) found", JLabel.CENTER);
        count.setFont(count.getFont().deriveFont((float)count.getFont().getSize() + 2.0f));
        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.getViewport().setBackground(table.getBackground());
        JPanel pane = (JPanel)resultFrame.getContentPane();
        pane.setLayout(new BorderLayout(0, 3));
        pane.add(count, BorderLayout.NORTH);
        pane.add(scrollTable, BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel searchPanel = SearchMaster.createAsPanel(this, resultFrame);
        bottomPanel.add(scrollText, BorderLayout.CENTER);
        bottomPanel.add(searchPanel, BorderLayout.EAST);
        pane.add(bottomPanel, BorderLayout.SOUTH);
        table.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getScriptFont()));
        table.setRowHeight(table.getFontMetrics(table.getFont()).getHeight() + 1);
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

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == save) {
      JFileChooser c = new JFileChooser(Profile.getGameRoot().toFile());
      c.setDialogTitle("Save result");
      if (c.showSaveDialog(resultFrame) == JFileChooser.APPROVE_OPTION) {
        Path output = c.getSelectedFile().toPath();
        if (Files.exists(output)) {
          String[] options = {"Overwrite", "Cancel"};
          if (JOptionPane.showOptionDialog(resultFrame, output + "exists. Overwrite?",
                                           "Save result",JOptionPane.YES_NO_OPTION,
                                           JOptionPane.WARNING_MESSAGE, null, options, options[0]) != 0)
            return;
        }
        try (BufferedWriter bw = Files.newBufferedWriter(output)) {
          bw.write("Searched for unused strings"); bw.newLine();
          bw.write("Number of hits: "  + table.getRowCount()); bw.newLine();
          bw.newLine();
          for (int i = 0; i < table.getRowCount(); i++) {
            bw.write("StringRef: " + table.getTableItemAt(i).getObjectAt(1) + " /* " +
                     table.getTableItemAt(i).toString().replaceAll("\r\n", Misc.LINE_SEPARATOR) +
                " */");
            bw.newLine();
          }
          JOptionPane.showMessageDialog(resultFrame, "Result saved to " + output, "Save complete",
                                        JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
          JOptionPane.showMessageDialog(resultFrame, "Error while saving " + output,
                                        "Error", JOptionPane.ERROR_MESSAGE);
          ex.printStackTrace();
        }
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------

// --------------------- Begin Interface SearchClient ---------------------

  @Override
  public String getText(int nr)
  {
    if (nr < 0 || nr >= table.getRowCount())
      return null;
    return table.getTableItemAt(nr).toString();
  }

  @Override
  public void hitFound(int nr)
  {
    table.getSelectionModel().addSelectionInterval(nr, nr);
    table.scrollRectToVisible(table.getCellRect(table.getSelectionModel().getMinSelectionIndex(), 0, true));
  }

// --------------------- End Interface SearchClient ---------------------

  private void checkDialog(DlgResource dialog)
  {
    for (StructEntry entry : dialog.getFlatList()) {
      if (entry instanceof StringRef) {
        checkStringRef((StringRef)entry);
      }
      else if (entry instanceof AbstractCode) {
        final AbstractCode code = (AbstractCode)entry;
        try {
          final ScriptType type = code instanceof Action ? ScriptType.ACTION : ScriptType.TRIGGER;
          final Compiler compiler = new Compiler(code.getText(), type);

          checkCode(compiler.getCode(), type);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  private void checkScript(BcsResource script)
  {
    try {
      checkCode(script.getCode(), null);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void checkStruct(AbstractStruct struct)
  {
    for (StructEntry entry : struct.getFlatList()) {
      if (entry instanceof StringRef) {
        checkStringRef((StringRef)entry);
      }
    }
  }

  private void checkTextfile(PlainTextResource text)
  {
    final Matcher m = NUMBERPATTERN.matcher(text.getText());
    while (m.find()) {
      final long nr = Long.parseLong(m.group());
      if (nr >= 0 && nr < strUsed.length) {
        synchronized (strUsed) {
          strUsed[(int)nr] = true;
        }
      }
    }
  }

  private void checkCode(String compiledCode, ScriptType type) throws Exception
  {
    final Decompiler decompiler = new Decompiler(compiledCode, true);
    decompiler.setGenerateComments(false);
    decompiler.setGenerateResourcesUsed(true);
    if (type != null) {
      decompiler.setScriptType(type);
    }
    decompiler.decompile();

    for (final Integer stringRef : decompiler.getStringRefsUsed()) {
      final int u = stringRef.intValue();
      if (u >= 0 && u < strUsed.length) {
        synchronized (strUsed) {
          strUsed[u] = true;
        }
      }
    }
  }

  private void checkStringRef(StringRef ref)
  {
    final int index = ref.getValue();
    if (index >= 0 && index < strUsed.length) {
      synchronized (strUsed) {
        strUsed[index] = true;
      }
    }
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class UnusedStringTableItem implements TableItem
  {
    private final Integer strRef;
    private final String string;

    private UnusedStringTableItem(Integer strRef)
    {
      this.strRef = strRef;
      string = StringTable.getStringRef(strRef.intValue(), StringTable.Format.NONE);
    }

    @Override
    public Object getObjectAt(int columnIndex)
    {
      if (columnIndex == 1)
        return strRef;
      return string;
    }

    @Override
    public String toString()
    {
      return string;
    }
  }
}
