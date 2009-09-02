// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.check;

import infinity.NearInfinity;
import infinity.datatype.StringRef;
import infinity.gui.*;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.bcs.*;
import infinity.resource.dlg.*;
import infinity.resource.dlg.Action;
import infinity.resource.key.ResourceEntry;
import infinity.resource.other.PlainTextResource;
import infinity.search.SearchClient;
import infinity.search.SearchMaster;
import infinity.util.StringResource;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringUseChecker implements Runnable, ListSelectionListener, SearchClient
{
  private static final Pattern NUMBERPATTERN = Pattern.compile("\\d+", Pattern.DOTALL);
  private static final String FILETYPES[] = {"2DA", "ARE", "BCS", "BS", "CHR", "CHU", "CRE", "DLG", "EFF",
                                             "INI", "ITM", "SPL", "SRC", "STO", "WMP"};
  private ChildFrame resultFrame;
  private JTextArea textArea;
  private SortableTable table;
  private boolean strUsed[];

  public StringUseChecker()
  {
    new Thread(this).start();
  }

// --------------------- Begin Interface ListSelectionListener ---------------------

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

  public void run()
  {
    WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    blocker.setBlocked(true);
    List<ResourceEntry> files = new ArrayList<ResourceEntry>();
    for (final String fileType : FILETYPES)
      files.addAll(ResourceFactory.getInstance().getResources(fileType));
    ProgressMonitor progress = new ProgressMonitor(NearInfinity.getInstance(),
                                                   "Searching...", null, 0, files.size());
    table = new SortableTable(new String[]{"String", "StrRef"},
                              new Class[]{Object.class, Integer.class},
                              new int[]{450, 20});
    StringResource.getStringRef(0);
    strUsed = new boolean[StringResource.getMaxIndex() + 1];
    for (int i = 0; i < files.size(); i++) {
      ResourceEntry entry = files.get(i);
      Resource resource = ResourceFactory.getResource(entry);
      if (resource instanceof DlgResource)
        checkDialog((DlgResource)resource);
      else if (resource instanceof BcsResource)
        checkScript((BcsResource)resource);
      else if (resource instanceof PlainTextResource)
        checkTextfile((PlainTextResource)resource);
      else if (resource != null)
        checkStruct((AbstractStruct)resource);
      progress.setProgress(i + 1);
      if (progress.isCanceled()) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Operation canceled",
                                      "Info", JOptionPane.INFORMATION_MESSAGE);
        blocker.setBlocked(false);
        return;
      }
    }
    for (int i = 0; i < strUsed.length; i++)
      if (!strUsed[i])
        table.addTableItem(new UnusedStringTableItem(new Integer(i)));
    if (table.getRowCount() == 0)
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No unused strings found",
                                    "Info", JOptionPane.INFORMATION_MESSAGE);
    else {
      table.tableComplete(1);
      textArea = new JTextArea(10, 40);
      textArea.setEditable(false);
      textArea.setWrapStyleWord(true);
      textArea.setLineWrap(true);
      JScrollPane scrollText = new JScrollPane(textArea);
      resultFrame = new ChildFrame("Result", true);
      resultFrame.setIconImage(Icons.getIcon("Find16.gif").getImage());
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
      table.setFont(BrowserMenuBar.getInstance().getScriptFont());
      pane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      table.getSelectionModel().addListSelectionListener(this);
      resultFrame.pack();
      Center.center(resultFrame, NearInfinity.getInstance().getBounds());
      resultFrame.setVisible(true);
    }
    blocker.setBlocked(false);
  }

// --------------------- End Interface Runnable ---------------------


// --------------------- Begin Interface SearchClient ---------------------

  public String getText(int nr)
  {
    if (nr < 0 || nr >= table.getRowCount())
      return null;
    return table.getTableItemAt(nr).toString();
  }

  public void hitFound(int nr)
  {
    table.getSelectionModel().addSelectionInterval(nr, nr);
    table.scrollRectToVisible(table.getCellRect(table.getSelectionModel().getMinSelectionIndex(), 0, true));
  }

// --------------------- End Interface SearchClient ---------------------

  private void checkDialog(DlgResource dialog)
  {
    List<StructEntry> flatList = dialog.getFlatList();
    for (int i = 0; i < flatList.size(); i++) {
      if (flatList.get(i) instanceof StringRef) {
        StringRef ref = (StringRef)flatList.get(i);
        if (ref.getValue() >= 0 && ref.getValue() < strUsed.length)
          strUsed[ref.getValue()] = true;
      }
      else if (flatList.get(i) instanceof AbstractCode) {
        AbstractCode code = (AbstractCode)flatList.get(i);
        try {
          String compiled = infinity.resource.bcs.Compiler.getInstance().compileDialogCode(code.toString(),
                                                                                           code instanceof Action);
          if (code instanceof Action)
            Decompiler.decompileDialogAction(compiled, true);
          else
            Decompiler.decompileDialogTrigger(compiled, true);
          Set<Integer> used = Decompiler.getStringRefsUsed();
          for (final Integer stringRef : used) {
            int u = stringRef.intValue();
            if (u >= 0 && u < strUsed.length)
              strUsed[u] = true;
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  private void checkScript(BcsResource script)
  {
    Decompiler.decompile(script.getCode(), true);
    Set<Integer> used = Decompiler.getStringRefsUsed();
    for (final Integer stringRef : used) {
      int u = stringRef.intValue();
      if (u >= 0 && u < strUsed.length)
        strUsed[u] = true;
    }
  }

  private void checkStruct(AbstractStruct struct)
  {
    List<StructEntry> flatList = struct.getFlatList();
    for (int i = 0; i < flatList.size(); i++) {
      if (flatList.get(i) instanceof StringRef) {
        StringRef ref = (StringRef)flatList.get(i);
        if (ref.getValue() >= 0 && ref.getValue() < strUsed.length)
          strUsed[ref.getValue()] = true;
      }
    }
  }

  private void checkTextfile(PlainTextResource text)
  {
    Matcher m = NUMBERPATTERN.matcher(text.getText());
    while (m.find()) {
      long nr = Long.parseLong(text.getText().substring(m.start(), m.end()));
      if (nr >= 0 && nr < strUsed.length)
        strUsed[(int)nr] = true;
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
      string = StringResource.getStringRef(strRef.intValue());
    }

    public Object getObjectAt(int columnIndex)
    {
      if (columnIndex == 1)
        return strRef;
      return string;
    }

    public String toString()
    {
      return string;
    }
  }
}

