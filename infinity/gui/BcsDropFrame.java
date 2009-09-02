// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import infinity.NearInfinity;
import infinity.icon.Icons;
import infinity.resource.ResourceFactory;
import infinity.resource.bcs.Compiler;
import infinity.resource.bcs.Decompiler;
import infinity.resource.key.FileResourceEntry;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

final class BcsDropFrame extends ChildFrame implements ActionListener, ListSelectionListener
{
  private final JButton bOpen = new JButton("Open selected", Icons.getIcon("Open16.gif"));
  private final JButton bSelectDir = new JButton(Icons.getIcon("Open16.gif"));
  private final JCheckBox cbIgnoreWarnings = new JCheckBox("Ignore compiler warnings", true);
  private final JFileChooser fc = new JFileChooser(ResourceFactory.getRootDir());
  private final JLabel compZone = new JLabel("Compiler drop zone (BAF)", JLabel.CENTER);
  private final JLabel decompZone = new JLabel("Decompiler drop zone (BCS/BS)", JLabel.CENTER);
  private final JLabel statusMsg = new JLabel(" Drag and drop files or folders into the zones");
  private final JRadioButton rbSaveBS = new JRadioButton("BS", false);
  private final JRadioButton rbSaveBCS = new JRadioButton("BCS", true);
  private final JRadioButton rbOrigDir = new JRadioButton("Same directory as input", true);
  private final JRadioButton rbOtherDir = new JRadioButton("Other ", false);
  private final JTabbedPane tabbedPane = new JTabbedPane();
  private final JTextField tfOtherDir = new JTextField(10);
  private final SortableTable table;
  private final WindowBlocker blocker;

  BcsDropFrame()
  {
    super("Script Drop Zone");
    setIconImage(Icons.getIcon("History16.gif").getImage());

    blocker = new WindowBlocker(this);
    compZone.setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlDkShadow")));
    decompZone.setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlDkShadow")));
    table = new SortableTable(new String[]{"File", "Errors/Warnings", "Line"},
                              new Class[]{Object.class, Object.class, Integer.class},
                              new int[]{200, 400, 100});
    table.getSelectionModel().addListSelectionListener(this);
    table.addMouseListener(new MouseAdapter()
    {
      public void mouseReleased(MouseEvent e)
      {
        if (e.getClickCount() == 2) {
          FileResourceEntry resourceEntry = (FileResourceEntry)table.getValueAt(table.getSelectedRow(), 0);
          if (resourceEntry != null)
            new ViewFrame(table.getTopLevelAncestor(),
                          ResourceFactory.getResource(resourceEntry));
        }
      }
    });
    JPanel centerPanel = new JPanel(new GridLayout(2, 1, 0, 6));
    centerPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    centerPanel.add(compZone);
    centerPanel.add(decompZone);
    JScrollPane scrollTable = new JScrollPane(table);
    scrollTable.getViewport().setBackground(table.getBackground());
    bOpen.addActionListener(this);
    bOpen.setEnabled(false);

    JPanel bPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    bPanel.add(bOpen);

    JPanel errorPanel = new JPanel(new BorderLayout());
    errorPanel.add(scrollTable, BorderLayout.CENTER);
    errorPanel.add(bPanel, BorderLayout.SOUTH);

    ButtonGroup bg = new ButtonGroup();
    bg.add(rbSaveBCS);
    bg.add(rbSaveBS);
    bg = new ButtonGroup();
    bg.add(rbOrigDir);
    bg.add(rbOtherDir);
    rbOrigDir.addActionListener(this);
    rbOtherDir.addActionListener(this);
    tfOtherDir.setEditable(false);
    bSelectDir.setEnabled(false);
    bSelectDir.addActionListener(this);
    fc.setDialogTitle("Select output directory");
    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    JPanel otherDir = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    bSelectDir.setMargin(new Insets(0, 0, 0, 0));
    otherDir.add(rbOtherDir);
    otherDir.add(tfOtherDir);
    otherDir.add(bSelectDir);
    JLabel label1 = new JLabel("Save compiled scripts as:");
    JLabel label2 = new JLabel("Output directory:");
    cbIgnoreWarnings.setToolTipText("Write script files even if they have compile errors");
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel optionsPanel = new JPanel(gbl);
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(3, 3, 3, 3);
    gbl.setConstraints(label1, gbc);
    optionsPanel.add(label1);
    gbl.setConstraints(rbSaveBCS, gbc);
    optionsPanel.add(rbSaveBCS);
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(rbSaveBS, gbc);
    optionsPanel.add(rbSaveBS);
    gbc.gridwidth = 1;
    JPanel dummy2 = new JPanel();
    gbl.setConstraints(dummy2, gbc);
    optionsPanel.add(dummy2);
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(cbIgnoreWarnings, gbc);
    optionsPanel.add(cbIgnoreWarnings);
    gbc.insets.top = 9;
    gbc.gridwidth = 1;
    gbl.setConstraints(label2, gbc);
    optionsPanel.add(label2);
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(rbOrigDir, gbc);
    optionsPanel.add(rbOrigDir);
    gbc.insets.top = 3;
    gbc.gridwidth = 1;
    JPanel dummy = new JPanel();
    gbl.setConstraints(dummy, gbc);
    optionsPanel.add(dummy);
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(otherDir, gbc);
    optionsPanel.add(otherDir);

    tabbedPane.add("Drop zones", centerPanel);
    tabbedPane.add("Compiler errors", errorPanel);
    tabbedPane.add("Options", optionsPanel);

    statusMsg.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
                                                           BorderFactory.createLineBorder(
                                                                   UIManager.getColor("controlShadow"))));
    JPanel pane = (JPanel)getContentPane();
    pane.setLayout(new BorderLayout());
    pane.add(tabbedPane, BorderLayout.CENTER);
    pane.add(statusMsg, BorderLayout.SOUTH);
    pane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

    new DropTarget(compZone, new MyDropTargetListener(compZone));
    new DropTarget(decompZone, new MyDropTargetListener(decompZone));

    setSize(500, 400);
    Center.center(this, NearInfinity.getInstance().getBounds());
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bOpen) {
      FileResourceEntry resourceEntry = (FileResourceEntry)table.getValueAt(table.getSelectedRow(), 0);
      if (resourceEntry != null)
        new ViewFrame(this, ResourceFactory.getResource(resourceEntry));
    }
    else if (event.getSource() == rbOrigDir) {
      bSelectDir.setEnabled(false);
      tfOtherDir.setEnabled(false);
    }
    else if (event.getSource() == rbOtherDir) {
      bSelectDir.setEnabled(true);
      tfOtherDir.setEnabled(true);
    }
    else if (event.getSource() == bSelectDir) {
      if (fc.showDialog(this, "Select") == JFileChooser.APPROVE_OPTION)
        tfOtherDir.setText(fc.getSelectedFile().toString());
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  public void valueChanged(ListSelectionEvent event)
  {
    bOpen.setEnabled(table.getSelectedRowCount() > 0);
  }

// --------------------- End Interface ListSelectionListener ---------------------

  private SortedMap<Integer, String> compileFile(File file)
  {
    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      StringBuffer source = new StringBuffer();
      String line = br.readLine();
      while (line != null) {
        source.append(line).append('\n');
        line = br.readLine();
      }
      br.close();
      String compiled = Compiler.getInstance().compile(source.toString());
      SortedMap<Integer, String> errors = Compiler.getInstance().getErrors();
      SortedMap<Integer, String> warnings = Compiler.getInstance().getWarnings();
      if (!cbIgnoreWarnings.isSelected())
        errors.putAll(warnings);
      if (errors.size() == 0) {
        String filename = file.getName();
        filename = filename.substring(0, filename.lastIndexOf((int)'.'));
        if (rbSaveBCS.isSelected())
          filename += ".BCS";
        else
          filename += ".BS";
        File output;
        if (rbOrigDir.isSelected())
          output = new File(file.getParent(), filename);
        else
          output = new File(tfOtherDir.getText(), filename);
        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(output)));
        pw.print(compiled);
        pw.close();
      }
      return errors;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  private boolean decompileFile(File file)
  {
    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      StringBuffer code = new StringBuffer();
      String line = br.readLine();
      while (line != null) {
        code.append(line).append('\n');
        line = br.readLine();
      }
      br.close();
      String filename = file.getName();
      filename = filename.substring(0, filename.lastIndexOf((int)'.')) + ".BAF";
      File output;
      if (rbOrigDir.isSelected())
        output = new File(file.getParent(), filename);
      else
        output = new File(tfOtherDir.getText(), filename);
      PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(output)));
      pw.println(Decompiler.decompile(code.toString(), true));
      pw.close();
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  private void filesDropped(Component component, List<File> files)
  {
    blocker.setBlocked(true);
    table.clear();
    long startTime = System.currentTimeMillis();
    int ok = 0, failed = 0;
    if (component == compZone) {
      for (int i = 0; i < files.size(); i++) {
        File file = files.get(i);
        if (file.isDirectory()) {
          File f[] = file.listFiles();
          for (final File newVar : f)
            files.add(newVar);
        }
        else if (file.toString().toUpperCase().endsWith("BAF")) {
          SortedMap<Integer, String> errors = compileFile(file);
          if (errors == null)
            failed++;
          else {
            if (errors.size() == 0)
              ok++;
            else {
              for (final Integer lineNr : errors.keySet())
                table.addTableItem(new CompileError(file, lineNr.intValue(), errors.get(lineNr)));
              failed++;
            }
          }
        }
      }
      if (failed > 0)
        tabbedPane.setSelectedIndex(1);
    }
    else if (component == decompZone) {
      for (int i = 0; i < files.size(); i++) {
        File file = files.get(i);
        if (file.isDirectory()) {
          File f[] = file.listFiles();
          for (final File newVar : f)
            files.add(newVar);
        }
        else if (file.toString().toUpperCase().endsWith("BCS") ||
                 file.toString().toUpperCase().endsWith("BS")) {
          if (decompileFile(file))
            ok++;
          else
            failed++;
        }
      }
    }
    long time = System.currentTimeMillis() - startTime;
    table.tableComplete();
    statusMsg.setText(" " + ok + " files (de)compiled ok, " + failed + " failed in " + time + " ms.");
    blocker.setBlocked(false);
  }

// -------------------------- INNER CLASSES --------------------------

  private final class MyDropTargetListener implements DropTargetListener, Runnable
  {
    private final Component component;
    private java.util.List files;

    private MyDropTargetListener(Component component)
    {
      this.component = component;
    }

    public void dragEnter(DropTargetDragEvent event)
    {
    }

    public void dragOver(DropTargetDragEvent event)
    {
    }

    public void dropActionChanged(DropTargetDragEvent event)
    {
    }

    public void dragExit(DropTargetEvent event)
    {
    }

    public void drop(DropTargetDropEvent event)
    {
      if (event.isLocalTransfer() || !event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        event.rejectDrop();
        return;
      }
      try {
        event.acceptDrop(DnDConstants.ACTION_COPY);
        files = (java.util.List)event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
      } catch (Exception e) {
        e.printStackTrace();
        event.dropComplete(false);
        return;
      }
      event.dropComplete(true);
      new Thread(this).start();
    }

    public void run()
    {
      filesDropped(component, new ArrayList<File>(files));
    }
  }

  private static final class CompileError implements TableItem
  {
    private final FileResourceEntry resourceEntry;
    private final Integer linenr;
    private final String error;

    private CompileError(File file, int linenr, String error)
    {
      resourceEntry = new FileResourceEntry(file);
      this.linenr = new Integer(linenr);
      this.error = error;
    }

    public Object getObjectAt(int columnIndex)
    {
      if (columnIndex == 0)
        return resourceEntry;
      else if (columnIndex == 1)
        return error;
      else
        return linenr;
    }
  }
}

