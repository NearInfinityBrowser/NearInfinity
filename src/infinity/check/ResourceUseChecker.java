// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.check;

import infinity.NearInfinity;
import infinity.datatype.ResourceRef;
import infinity.gui.*;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.bcs.*;
import infinity.resource.dlg.*;
import infinity.resource.dlg.Action;
import infinity.resource.key.ResourceEntry;
import infinity.resource.other.PlainTextResource;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ResourceUseChecker implements Runnable, ListSelectionListener, ActionListener
{
  private static final Pattern RESREFPATTERN = Pattern.compile("\\w{3,8}");
  private static final String FILETYPES[] = {"2DA", "ARE", "BCS", "BS", "CHR", "CHU", "CRE",
                                             "DLG", "EFF", "INI", "ITM", "PRO", "SPL", "STO",
                                             "VEF", "VVC", "WED", "WMP"};
  private static final String CHECKTYPES[] = {"ARE", "BCS", "CRE", "DLG", "EFF", "ITM", "PRO", "SPL", "STO",
                                              "TIS", "VEF", "VVC", "WAV", "WED"};
  private final ChildFrame selectframe = new ChildFrame("Find unused files", true);
  private final JButton bstart = new JButton("Search", Icons.getIcon("Find16.gif"));
  private final JButton bcancel = new JButton("Cancel", Icons.getIcon("Delete16.gif"));
  private final JRadioButton[] typeButtons;
  private final List<ResourceEntry> checkList = new ArrayList<ResourceEntry>();
  private ChildFrame resultFrame;
  private JButton bopen, bopennew;
  private SortableTable table;
  private String checkType;

  public ResourceUseChecker(Component parent)
  {
    typeButtons = new JRadioButton[CHECKTYPES.length];
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
    selectframe.setIconImage(Icons.getIcon("Find16.gif").getImage());
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
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  public void valueChanged(ListSelectionEvent event)
  {
    bopen.setEnabled(true);
    bopennew.setEnabled(true);
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
    table = new SortableTable(new String[]{"File", "Name"},
                              new Class[]{Object.class, Object.class},
                              new int[]{200, 200});
    checkList.addAll(ResourceFactory.getInstance().getResources(checkType));
    long startTime = System.currentTimeMillis();
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
    System.out.println("Search took " + (System.currentTimeMillis() - startTime) + "ms");
    for (int i = 0; i < checkList.size(); i++)
      table.addTableItem(new UnusedFileTableItem(checkList.get(i)));
    if (table.getRowCount() == 0)
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No unused " + checkType + "s found",
                                    "Info", JOptionPane.INFORMATION_MESSAGE);
    else {
      table.tableComplete();
      resultFrame = new ChildFrame("Result", true);
      resultFrame.setIconImage(Icons.getIcon("Find16.gif").getImage());
      bopen = new JButton("Open", Icons.getIcon("Open16.gif"));
      bopennew = new JButton("Open in new window", Icons.getIcon("Open16.gif"));
      bopen.setMnemonic('o');
      bopennew.setMnemonic('n');
      resultFrame.getRootPane().setDefaultButton(bopennew);
      JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      panel.add(bopen);
      panel.add(bopennew);
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
      table.setFont(BrowserMenuBar.getInstance().getScriptFont());
      table.addMouseListener(new MouseAdapter()
      {
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
      pane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      table.getSelectionModel().addListSelectionListener(this);
      resultFrame.pack();
      Center.center(resultFrame, NearInfinity.getInstance().getBounds());
      resultFrame.setVisible(true);
    }
    blocker.setBlocked(false);
  }

// --------------------- End Interface Runnable ---------------------

  private void checkDialog(DlgResource dialog)
  {
    List<StructEntry> flatList = dialog.getFlatList();
    for (int i = 0; i < flatList.size(); i++) {
      if (flatList.get(i) instanceof ResourceRef) {
        ResourceRef ref = (ResourceRef)flatList.get(i);
        if (ref.getType().equalsIgnoreCase(checkType)) {
          for (Iterator<ResourceEntry> j = checkList.iterator(); j.hasNext();)
            if (j.next().toString().equalsIgnoreCase(ref.getResourceName())) {
              j.remove();
              break;
            }
        }
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
          Set<ResourceEntry> resourcesUsed = Decompiler.getResourcesUsed();
          for (final ResourceEntry resourceEntry : resourcesUsed)
            checkList.remove(resourceEntry);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  private void checkScript(BcsResource script)
  {
    Decompiler.decompile(script.getCode(), true);
    Set<ResourceEntry> resourcesUsed = Decompiler.getResourcesUsed();
    for (final ResourceEntry resourceEntry : resourcesUsed)
      checkList.remove(resourceEntry);
  }

  private void checkStruct(AbstractStruct struct)
  {
    List<StructEntry> flatList = struct.getFlatList();
    for (int i = 0; i < flatList.size(); i++) {
      if (flatList.get(i) instanceof ResourceRef) {
        ResourceRef ref = (ResourceRef)flatList.get(i);
        if (ref.getType().equalsIgnoreCase(checkType)) {
          for (Iterator<ResourceEntry> j = checkList.iterator(); j.hasNext();)
            if (j.next().toString().equalsIgnoreCase(ref.getResourceName())) {
              j.remove();
              break;
            }
        }
      }
    }
  }

  private void checkTextfile(PlainTextResource text)
  {
    Matcher m = RESREFPATTERN.matcher(text.getText());
    while (m.find()) {
      String s = text.getText().substring(m.start(), m.end()) + '.' + checkType;
      for (Iterator<ResourceEntry> i = checkList.iterator(); i.hasNext();) {
        if (i.next().toString().equalsIgnoreCase(s)) {
          i.remove();
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

    public Object getObjectAt(int columnIndex)
    {
      if (columnIndex == 0)
        return file;
      return file.getSearchString();
    }
  }
}

