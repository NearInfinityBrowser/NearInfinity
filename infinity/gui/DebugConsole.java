// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import infinity.NearInfinity;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.Closeable;
import infinity.resource.key.ResourceEntry;
import infinity.resource.key.ResourceTreeModel;
import infinity.util.StructClipboard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

final class DebugConsole extends ChildFrame implements ActionListener, ItemListener
{
  private final ButtonPopupMenu testPopup;
  private final JButton bclearconsole = new JButton("Clear", Icons.getIcon("New16.gif"));
  private final JButton bsaveconsole = new JButton("Save...", Icons.getIcon("Save16.gif"));
  private final JMenuItem miReadTest = new JMenuItem("Read test...");
  private final JMenuItem miWriteTest = new JMenuItem("Write test...");
  private final JMenuItem miCutPasteTest = new JMenuItem("Cut/Paste test...");

  private static void cutPasteTest(ResourceEntry entry)
  {
    StructClipboard.getInstance().clear();
    try {
      Resource resource = ResourceFactory.getResource(entry);
      if (resource instanceof HasAddRemovable) {
        AbstractStruct struct = (AbstractStruct)resource;
        int firstIndex = 0;
        for (int i = 0; i < struct.getRowCount(); i++) {
          StructEntry structEntry = struct.getStructEntryAt(i);
          if (firstIndex == 0 && structEntry instanceof AddRemovable)
            firstIndex = i;
          if (firstIndex > 0 && !(structEntry instanceof AddRemovable)) {
            StructClipboard.getInstance().cut(struct, firstIndex, i - 1);
            firstIndex = 0;
            i = 0;
          }
        }
        if (firstIndex != 0)
          StructClipboard.getInstance().cut(struct, firstIndex, struct.getRowCount() - 1);
        if (StructClipboard.getInstance().getContentType(struct) != StructClipboard.CLIPBOARD_EMPTY) {
          StructClipboard.getInstance().paste(struct);
          StructClipboard.getInstance().clear();
          List<StructEntry> flatList = struct.getFlatList();
          StructEntry firstEntry = flatList.get(0);
          for (int i = 1; i < flatList.size(); i++) {
            StructEntry secondEntry = flatList.get(i);
            if (firstEntry.getOffset() + firstEntry.getSize() != secondEntry.getOffset())
              System.err.println(
                      struct.getName() + " (1) " + firstEntry.getName() + ' ' +
                      Integer.toHexString(firstEntry.getOffset()) + "h + " +
                      Integer.toHexString(firstEntry.getSize()) + "h = " +
                      Integer.toHexString(firstEntry.getOffset() + firstEntry.getSize()) +
                      "h  !=  (2) " +
                      secondEntry.getName() + ' ' +
                      Integer.toHexString(secondEntry.getOffset()) +
                      'h');
            firstEntry = secondEntry;
          }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        struct.write(baos);
        byte writtenData[] = baos.toByteArray();
        ResourceEntry entryCopy = new ByteArrayResourceEntry(entry, writtenData);
        ResourceFactory.getResource(entryCopy);
        baos.close();
      }
      if (resource instanceof Closeable)
        ((Closeable)resource).close();
    } catch (Exception e) {
      System.err.println("Error during cut/paste test of " + entry);
      e.printStackTrace();
    }
  }

  DebugConsole()
  {
    super("Debug Console");
    setIconImage(Icons.getIcon("Properties16.gif").getImage());
    testPopup =
    new ButtonPopupMenu("Debug tests..", new JMenuItem[]{miReadTest, miWriteTest, miCutPasteTest});
    testPopup.addItemListener(this);
    testPopup.setIcon(Icons.getIcon("Refresh16.gif"));

    bclearconsole.setMnemonic('c');
    bclearconsole.addActionListener(this);
    bsaveconsole.setMnemonic('s');
    bsaveconsole.addActionListener(this);

    JTextArea taconsole = NearInfinity.getConsoleText();
    taconsole.setEditable(false);
    taconsole.setFont(BrowserMenuBar.getInstance().getScriptFont());

    JPanel lowerpanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    lowerpanel.add(testPopup);
    lowerpanel.add(bclearconsole);
    lowerpanel.add(bsaveconsole);

    JPanel pane = (JPanel)getContentPane();
    pane.setLayout(new BorderLayout());
    pane.add(new JScrollPane(taconsole), BorderLayout.CENTER);
    pane.add(lowerpanel, BorderLayout.SOUTH);

    setSize(450, 450);
    Center.center(this, NearInfinity.getInstance().getBounds());
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bclearconsole)
      NearInfinity.getConsoleText().setText("");
    else if (event.getSource() == bsaveconsole) {
      JFileChooser chooser = new JFileChooser(ResourceFactory.getRootDir());
      chooser.setDialogTitle("Save console");
      chooser.setSelectedFile(new File("nidebuglog.txt"));
      if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        File output = chooser.getSelectedFile();
        if (output.exists()) {
          String options[] = {"Overwrite", "Cancel"};
          if (JOptionPane.showOptionDialog(this, output + " exists. Overwrite?", "Save debug log", JOptionPane.YES_NO_OPTION,
                                           JOptionPane.WARNING_MESSAGE, null, options, options[0]) == 1)
            return;
        }
        try {
          PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(output)));
          pw.println("Near Infinity Debug Log");
          pw.println(BrowserMenuBar.VERSION);
          pw.println(ResourceFactory.getGameName(ResourceFactory.getGameID()));
          pw.println();
          pw.println(NearInfinity.getConsoleText().getText());
          pw.println();
          Properties props = System.getProperties();
          for (Object key : props.keySet())
            pw.println(key + "=" + props.get(key));
          pw.close();
          JOptionPane.showMessageDialog(this, "Console saved to " + output, "Save complete",
                                        JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
          JOptionPane.showMessageDialog(this, "Error while saving " + output, "Error",
                                        JOptionPane.ERROR_MESSAGE);
          e.printStackTrace();
        }
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ItemListener ---------------------

  public void itemStateChanged(ItemEvent event)
  {
    if (event.getSource() == testPopup) {
      if (testPopup.getSelectedItem() == miReadTest)
        readTest();
      else if (testPopup.getSelectedItem() == miWriteTest)
        writeTest();
      else if (testPopup.getSelectedItem() == miCutPasteTest)
        cutPasteTest();
    }
  }

// --------------------- End Interface ItemListener ---------------------

  private void cutPasteTest()
  {
    final DebugConsole console = this;
    (new Thread()
    {
      public void run()
      {
        String filetype = JOptionPane.showInputDialog(console, "Enter file type (*=all files)", "Cut/Paste test",
                                                      JOptionPane.QUESTION_MESSAGE);
        if (filetype == null || filetype.equals(""))
          return;
        WindowBlocker blocker = new WindowBlocker(console);
        blocker.setBlocked(true);
        if (filetype.equals("*")) {
          ResourceTreeModel treeModel = ResourceFactory.getInstance().getResources();
          ProgressMonitor progress = new ProgressMonitor(console, "Simulating cut/paste...", null, 0,
                                                         treeModel.size());
          progress.setMillisToDecideToPopup(100);
          int counter = 0;
          Stack stack = new Stack();
          stack.push(treeModel.getRoot());
          while (!stack.empty()) {
            Object node = stack.pop();
            if (treeModel.isLeaf(node)) {
              cutPasteTest((ResourceEntry)node);
              progress.setProgress(++counter);
            }
            else {
              for (int i = 0; i < treeModel.getChildCount(node); i++)
                stack.push(treeModel.getChild(node, i));
            }
            if (progress.isCanceled())
              break;
          }
          progress.close();
        }
        else {
          List<ResourceEntry> resources = ResourceFactory.getInstance().getResources(filetype.toUpperCase());
          ProgressMonitor progress = new ProgressMonitor(console, "Simulating cut/paste...", null, 0,
                                                         resources.size());
          progress.setMillisToDecideToPopup(100);
          for (int i = 0; i < resources.size(); i++) {
            cutPasteTest(resources.get(i));
            progress.setProgress(i);
            if (progress.isCanceled())
              break;
          }
          progress.close();
        }
        blocker.setBlocked(false);
        System.out.println("Test completed");
      }
    }).start();
  }

  private void readTest()
  {
    final DebugConsole console = this;
    (new Thread()
    {
      public void run()
      {
        String filetype = JOptionPane.showInputDialog(console, "Enter file type (*=all files)", "Read test",
                                                      JOptionPane.QUESTION_MESSAGE);
        if (filetype == null || filetype.equals(""))
          return;
        WindowBlocker blocker = new WindowBlocker(console);
        blocker.setBlocked(true);
        if (filetype.equals("*")) {
          ResourceTreeModel treeModel = ResourceFactory.getInstance().getResources();
          ProgressMonitor progress = new ProgressMonitor(console, "Reading files...", null, 0,
                                                         treeModel.size());
          progress.setMillisToDecideToPopup(100);
          int counter = 0;
          Stack stack = new Stack();
          stack.push(treeModel.getRoot());
          while (!stack.empty()) {
            Object node = stack.pop();
            if (treeModel.isLeaf(node)) {
              Resource res = ResourceFactory.getResource((ResourceEntry)node);
              if (res instanceof Closeable) {
                try {
                  ((Closeable)res).close();
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
              progress.setProgress(++counter);
            }
            else {
              for (int i = 0; i < treeModel.getChildCount(node); i++)
                stack.push(treeModel.getChild(node, i));
            }
            if (progress.isCanceled())
              break;
          }
          progress.close();
        }
        else {
          List<ResourceEntry> resources = ResourceFactory.getInstance().getResources(filetype.toUpperCase());
          ProgressMonitor progress = new ProgressMonitor(console, "Reading files...", null, 0,
                                                         resources.size());
          progress.setMillisToDecideToPopup(100);
          for (int i = 0; i < resources.size(); i++) {
            Resource res = ResourceFactory.getResource(resources.get(i));
            if (res instanceof Closeable) {
              try {
                ((Closeable)res).close();
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
            progress.setProgress(i);
            if (progress.isCanceled())
              break;
          }
          progress.close();
        }
        blocker.setBlocked(false);
        System.out.println("Test completed");
      }
    }).start();
  }

  private void writeTest()
  {
    final DebugConsole console = this;
    (new Thread()
    {
      public void run()
      {
        String filetype = JOptionPane.showInputDialog(console, "Enter file type (*=all files)", "Write test",
                                                      JOptionPane.QUESTION_MESSAGE);
        if (filetype == null || filetype.equals(""))
          return;
        WindowBlocker blocker = new WindowBlocker(console);
        blocker.setBlocked(true);
        if (filetype.equals("*")) {
          ResourceTreeModel treeModel = ResourceFactory.getInstance().getResources();
          ProgressMonitor progress = new ProgressMonitor(console, "Simulating writeField...", null, 0,
                                                         treeModel.size());
          progress.setMillisToDecideToPopup(100);
          int counter = 0;
          Stack stack = new Stack();
          stack.push(treeModel.getRoot());
          while (!stack.empty()) {
            Object node = stack.pop();
            if (treeModel.isLeaf(node)) {
              writeTest((ResourceEntry)node);
              progress.setProgress(++counter);
            }
            else {
              for (int i = 0; i < treeModel.getChildCount(node); i++)
                stack.push(treeModel.getChild(node, i));
            }
            if (progress.isCanceled())
              break;
          }
          progress.close();
        }
        else {
          List<ResourceEntry> resources = ResourceFactory.getInstance().getResources(filetype.toUpperCase());
          ProgressMonitor progress = new ProgressMonitor(console, "Simulating writeField...", null, 0,
                                                         resources.size());
          progress.setMillisToDecideToPopup(100);
          for (int i = 0; i < resources.size(); i++) {
            writeTest(resources.get(i));
            progress.setProgress(i);
            if (progress.isCanceled())
              break;
          }
          progress.close();
        }
        blocker.setBlocked(false);
        System.out.println("Test completed");
      }
    }).start();
  }

  private void writeTest(ResourceEntry entry)
  {
    try {
      Resource resource = ResourceFactory.getResource(entry);
      if (resource instanceof Writeable) {
        byte readData[] = entry.getResourceData();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(readData.length);
        ((Writeable)resource).write(baos);
        byte writtenData[] = baos.toByteArray();
        if (readData.length != writtenData.length)
          System.err.println(entry.toString() + " - read: " + readData.length + " bytes, written: " +
                             writtenData.length + " bytes.");
        for (int i = 0; i < Math.min(readData.length, writtenData.length); i++) {
          if (readData[i] != writtenData[i]) {
            String readString = new String(readData, i, 1);
            String writtenString = new String(writtenData, i, 1);
            if (!readString.equalsIgnoreCase(writtenString)) {
              System.err.println(
                      entry.toString() + " - offset:" + Integer.toHexString(i) +
                      "h read:" + Integer.toHexString((int)readData[i]) + "h ('" +
                      readString +
                      "') written: " + Integer.toHexString((int)writtenData[i]) +
                      "h ('" +
                      writtenString +
                      "')");
              if (resource instanceof AbstractStruct) {
                StructEntry attribute = ((AbstractStruct)resource).getAttribute(i);
                if (attribute == null)
                  System.err.println(
                          entry.toString() + " - no attribute found at offset:" + Integer.toHexString(i) +
                          'h');
                else
                  System.err.println(entry.toString() + " - byte part of: " + attribute.getName() +
                                     " (" + attribute.getClass().toString() + ')');
              }
            }
          }
        }
      }
      if (resource instanceof Closeable)
        ((Closeable)resource).close();
    } catch (Exception e) {
      System.err.println("Error during writeField test of " + entry);
      e.printStackTrace();
    }
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class ByteArrayResourceEntry extends ResourceEntry
  {
    private final ResourceEntry copyOf;
    private final byte data[];

    private ByteArrayResourceEntry(ResourceEntry copyOf, byte data[])
    {
      this.copyOf = copyOf;
      this.data = data;
    }

    public String getExtension()
    {
      return copyOf.getExtension();
    }

    public String getTreeFolder()
    {
      return copyOf.getTreeFolder();
    }

    public int[] getResourceInfo(boolean ignoreoverride) throws Exception
    {
      throw new IllegalAccessError();
    }

    public byte[] getResourceData(boolean ignoreoverride) throws Exception
    {
      return data;
    }

    protected InputStream getResourceDataAsStream(boolean ignoreoverride) throws Exception
    {
      throw new IllegalAccessError();
    }

    protected File getActualFile(boolean ignoreoverride)
    {
      return null;
    }

    public String toString()
    {
      return copyOf.toString();
    }

    public String getResourceName()
    {
      return copyOf.getResourceName();
    }

    public boolean hasOverride()
    {
      return false;
    }
  }
}

