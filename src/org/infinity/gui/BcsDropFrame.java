// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.NearInfinity;
import org.infinity.icon.Icons;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.bcs.Compiler;
import org.infinity.resource.bcs.Decompiler;
import org.infinity.resource.bcs.ScriptMessage;
import org.infinity.resource.bcs.ScriptType;
import org.infinity.resource.key.FileResourceEntry;
import org.infinity.util.Misc;
import org.infinity.util.io.FileEx;
import org.infinity.util.io.FileManager;

final class BcsDropFrame extends ChildFrame implements ActionListener, ListSelectionListener
{
  private final JButton bOpen = new JButton("Open selected", Icons.getIcon(Icons.ICON_OPEN_16));
  private final JButton bSelectDir = new JButton(Icons.getIcon(Icons.ICON_OPEN_16));
  private final JCheckBox cbIgnoreWarnings = new JCheckBox("Ignore compiler warnings", true);
  private final JFileChooser fc = new JFileChooser(Profile.getGameRoot().toFile());
  private final JLabel compZone = new JLabel("Compiler drop zone (BAF)", JLabel.CENTER);
  private final JLabel decompZone = new JLabel("Decompiler drop zone (BCS/BS)", JLabel.CENTER);
  private final JLabel statusMsg = new JLabel(" Drag and drop files or folders into the zones");
  private final JRadioButton rbSaveBS = new JRadioButton("BS", false);
  private final JRadioButton rbSaveBCS = new JRadioButton("BCS", true);
  private final JRadioButton rbOrigDir = new JRadioButton("Same directory as input", true);
  private final JRadioButton rbOtherDir = new JRadioButton("Other ", false);
  private final JTabbedPane tabbedPane = new JTabbedPane();
  private final JTextField tfOtherDir = new JTextField(10);
  /** List of the {@link CompileError} objects. */
  private final SortableTable table;
  private final WindowBlocker blocker;

  BcsDropFrame()
  {
    super("Script Drop Zone");
    setIconImage(Icons.getIcon(Icons.ICON_HISTORY_16).getImage());

    blocker = new WindowBlocker(this);
    compZone.setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlDkShadow")));
    decompZone.setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlDkShadow")));

    table = new SortableTable(new String[]{"File", "Errors/Warnings", "Line"},
                              new Class<?>[]{FileResourceEntry.class, String.class, Integer.class},
                              new Integer[]{200, 400, 100});

    table.getSelectionModel().addListSelectionListener(this);
    table.addMouseListener(new MouseAdapter()
    {
      @Override
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

    setSize(Misc.getScaledValue(500), Misc.getScaledValue(400));
    Center.center(this, NearInfinity.getInstance().getBounds());
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
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

  @Override
  public void valueChanged(ListSelectionEvent event)
  {
    bOpen.setEnabled(table.getSelectedRowCount() > 0);
  }

// --------------------- End Interface ListSelectionListener ---------------------

  private SortedSet<ScriptMessage> compileFile(Path file)
  {
    final StringBuilder source = new StringBuilder();
    try (BufferedReader br = Files.newBufferedReader(file)) {
      String line = br.readLine();
      while (line != null) {
        source.append(line).append('\n');
        line = br.readLine();
      }
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
    Compiler compiler = new Compiler(source.toString());
    String compiled = compiler.getCode();
    SortedSet<ScriptMessage> errors = compiler.getErrors();
    SortedSet<ScriptMessage> warnings = compiler.getWarnings();
    if (!cbIgnoreWarnings.isSelected()) {
      errors.addAll(warnings);
    }
    if (errors.isEmpty()) {
      String filename = file.getFileName().toString();
      filename = filename.substring(0, filename.lastIndexOf((int)'.'));
      if (rbSaveBCS.isSelected()) {
        filename += ".BCS";
      } else {
        filename += ".BS";
      }
      Path output;
      if (rbOrigDir.isSelected()) {
        output = file.getParent().resolve(filename);
      } else {
        output = FileManager.resolve(tfOtherDir.getText(), filename);
      }
      try (BufferedWriter bw = Files.newBufferedWriter(output)) {
        bw.write(compiled);
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
    }
    return errors;
  }

  private boolean decompileFile(Path file)
  {
    final StringBuilder code = new StringBuilder();
    try (BufferedReader br = Files.newBufferedReader(file)) {
      String line = br.readLine();
      while (line != null) {
        code.append(line).append('\n');
        line = br.readLine();
      }
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    String filename = file.getFileName().toString();
    filename = filename.substring(0, filename.lastIndexOf((int)'.')) + ".BAF";
    Path output;
    if (rbOrigDir.isSelected()) {
      output = file.getParent().resolve(filename);
    } else {
      output = FileManager.resolve(tfOtherDir.getText(), filename);
    }
    Decompiler decompiler = new Decompiler(code.toString(), ScriptType.BCS, true);
    decompiler.setGenerateComments(BrowserMenuBar.getInstance().autogenBCSComments());
    try (BufferedWriter bw = Files.newBufferedWriter(output)) {
      bw.write(decompiler.getSource().replaceAll("\r?\n", Misc.LINE_SEPARATOR));
      bw.newLine();
    } catch (Exception e) {
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
      for (File f : files) {
        Path file = f.toPath();
        if (FileEx.create(file).isDirectory()) {
          try (DirectoryStream<Path> dstream = Files.newDirectoryStream(file)) {
            for (final Path p: dstream) {
              files.add(p.toFile());
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        else if (file.getFileName().toString().toUpperCase(Locale.ENGLISH).endsWith(".BAF")) {
          SortedSet<ScriptMessage> errors = compileFile(file);
          if (errors == null) {
            failed++;
          } else {
            if (errors.isEmpty()) {
              ok++;
            } else {
              for (final ScriptMessage sm: errors) {
                table.addTableItem(new CompileError(file, sm.getLine(), sm.getMessage()));
              }
              failed++;
            }
          }
        }
      }
      if (failed > 0) {
        tabbedPane.setSelectedIndex(1);
      }
    }
    else if (component == decompZone) {
      for (File f : files) {
        final Path file = f.toPath();
        if (FileEx.create(file).isDirectory()) {
          try (final DirectoryStream<Path> dstream = Files.newDirectoryStream(file)) {
            for (final Path p: dstream) {
              files.add(p.toFile());
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        else if (file.getFileName().toString().toUpperCase(Locale.ENGLISH).endsWith(".BCS") ||
                 file.getFileName().toString().toUpperCase(Locale.ENGLISH).endsWith(".BS")) {
          if (decompileFile(file)) {
            ok++;
          } else {
            failed++;
          }
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
    private List<File> files;

    private MyDropTargetListener(Component component)
    {
      this.component = component;
    }

    @Override
    public void dragEnter(DropTargetDragEvent event)
    {
    }

    @Override
    public void dragOver(DropTargetDragEvent event)
    {
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent event)
    {
    }

    @Override
    public void dragExit(DropTargetEvent event)
    {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void drop(DropTargetDropEvent event)
    {
      if (event.isLocalTransfer() || !event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        event.rejectDrop();
        return;
      }
      try {
        event.acceptDrop(DnDConstants.ACTION_COPY);
        files = (List<File>)event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
      } catch (Exception e) {
        e.printStackTrace();
        event.dropComplete(false);
        return;
      }
      event.dropComplete(true);
      new Thread(this).start();
    }

    @Override
    public void run()
    {
      filesDropped(component, new ArrayList<>(files));
    }
  }

  private static final class CompileError implements TableItem
  {
    private final FileResourceEntry resourceEntry;
    private final Integer linenr;
    private final String error;

    private CompileError(Path file, int linenr, String error)
    {
      resourceEntry = new FileResourceEntry(file);
      this.linenr = linenr;
      this.error = error;
    }

    @Override
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

