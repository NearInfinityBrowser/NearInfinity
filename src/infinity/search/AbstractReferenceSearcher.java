// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.search;

import infinity.gui.Center;
import infinity.gui.ChildFrame;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.key.ResourceEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

abstract class AbstractReferenceSearcher implements Runnable, ActionListener
{
  private static boolean lastSelected[] = {true};
  final ResourceEntry targetEntry;
  private final ChildFrame selectframe = new ChildFrame("References", true);
  private final Component parent;
  private final JButton bstart = new JButton("Search", Icons.getIcon("Find16.gif"));
  private final JButton bcancel = new JButton("Cancel", Icons.getIcon("Delete16.gif"));
  private final JButton binvert = new JButton("Invert", Icons.getIcon("Refresh16.gif"));
  private final ReferenceHitFrame hitFrame;
  private final String[] filetypes;
  private JCheckBox[] boxes;
  private List<ResourceEntry> files;

  AbstractReferenceSearcher(ResourceEntry targetEntry, String filetypes[], Component parent)
  {
    this.targetEntry = targetEntry;
    this.filetypes = filetypes;
    this.parent = parent;
    hitFrame = new ReferenceHitFrame(targetEntry, parent);
    if (filetypes.length == 1) {
      files = new ArrayList<ResourceEntry>();
      files.addAll(ResourceFactory.getInstance().getResources(filetypes[0]));
      if (files.size() > 0)
        new Thread(this).start();
    }
    else {
      boxes = new JCheckBox[filetypes.length];
      bstart.setMnemonic('s');
      bcancel.setMnemonic('c');
      binvert.setMnemonic('i');
      bstart.addActionListener(this);
      bcancel.addActionListener(this);
      binvert.addActionListener(this);
      selectframe.getRootPane().setDefaultButton(bstart);
      selectframe.setIconImage(Icons.getIcon("Find16.gif").getImage());

      JPanel boxpanel = new JPanel(new GridLayout(0, 2, 3, 3));
      for (int i = 0; i < boxes.length; i++) {
        boxes[i] = new JCheckBox(filetypes[i], true);
        boxpanel.add(boxes[i]);
      }
      boxpanel.setBorder(BorderFactory.createEmptyBorder(3, 12, 3, 0));
      if (lastSelected.length == boxes.length)
        for (int i = 0; i < lastSelected.length; i++)
          boxes[i].setSelected(lastSelected[i]);

      JPanel ipanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      ipanel.add(binvert);
      JPanel innerpanel = new JPanel(new BorderLayout());
      innerpanel.add(boxpanel, BorderLayout.CENTER);
      innerpanel.add(ipanel, BorderLayout.SOUTH);
      innerpanel.setBorder(BorderFactory.createTitledBorder("Select files to search:"));

      JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      bpanel.add(bstart);
      bpanel.add(bcancel);

      JPanel mainpanel = new JPanel(new BorderLayout());
      mainpanel.add(innerpanel, BorderLayout.CENTER);
      mainpanel.add(bpanel, BorderLayout.SOUTH);
      mainpanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

      JPanel pane = (JPanel)selectframe.getContentPane();
      pane.setLayout(new BorderLayout());
      pane.add(mainpanel, BorderLayout.CENTER);

      selectframe.pack();
      Center.center(selectframe, parent.getBounds());
      selectframe.setVisible(true);
    }
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bstart) {
      selectframe.setVisible(false);
      files = new ArrayList<ResourceEntry>();
      lastSelected = new boolean[filetypes.length];
      for (int i = 0; i < filetypes.length; i++) {
        if (boxes[i].isSelected())
          files.addAll(ResourceFactory.getInstance().getResources(filetypes[i]));
        lastSelected[i] = boxes[i].isSelected();
      }
      if (files.size() > 0)
        new Thread(this).start();
    }
    else if (event.getSource() == bcancel)
      selectframe.setVisible(false);
    else if (event.getSource() == binvert) {
      for (final JCheckBox box : boxes) 
        box.setSelected(!box.isSelected());
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Runnable ---------------------

  public void run()
  {
    ProgressMonitor progress = new ProgressMonitor(parent, "Searching...", null, 0, files.size());
    progress.setMillisToDecideToPopup(100);
    String type = null;
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < files.size(); i++) {
      ResourceEntry entry = files.get(i);
      Resource resource = ResourceFactory.getResource(entry);
      if (resource != null) {
        if (!entry.getExtension().equalsIgnoreCase(type)) {
          type = entry.getExtension();
          progress.setNote(type + 's');
        }
        search(entry, resource);
      }
      progress.setProgress(i + 1);
      if (progress.isCanceled()) {
        JOptionPane.showMessageDialog(parent, "Search canceled", "Info", JOptionPane.INFORMATION_MESSAGE);
        return;
      }
    }
    System.out.println("Search completed: " + (System.currentTimeMillis() - startTime) + "ms.");
    hitFrame.setVisible(true);
  }

// --------------------- End Interface Runnable ---------------------

  void addHit(ResourceEntry entry, String name, StructEntry ref)
  {
    hitFrame.addHit(entry, name, ref);
  }

  abstract void search(ResourceEntry entry, Resource resource);
}

