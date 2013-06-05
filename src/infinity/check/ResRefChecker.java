// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.check;

import infinity.NearInfinity;
import infinity.datatype.ResourceRef;
import infinity.datatype.SpawnResourceRef;
import infinity.gui.Center;
import infinity.gui.ChildFrame;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.other.PlainTextResource;
import infinity.resource.cre.CreResource;
import infinity.resource.key.ResourceEntry;
import infinity.search.ReferenceHitFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public final class ResRefChecker extends ChildFrame implements ActionListener, Runnable
{
  private static final String filetypes[] = {"ARE", "CHR", "CHU", "CRE", "DLG", "EFF", "GAM", "ITM", "PRO",
                                             "SPL", "STO", "VEF", "VVC", "WED", "WMP"};
  private final JButton bstart = new JButton("Check", Icons.getIcon("Find16.gif"));
  private final JButton bcancel = new JButton("Cancel", Icons.getIcon("Delete16.gif"));
  private final JButton binvert = new JButton("Invert", Icons.getIcon("Refresh16.gif"));
  private final JCheckBox[] boxes;
  private final ReferenceHitFrame hitFrame;
  private List<ResourceEntry> files;
  private List<String> extraValues;

  public ResRefChecker()
  {
    super("ResRef Checker");
    setIconImage(Icons.getIcon("Refresh16.gif").getImage());
    hitFrame = new ReferenceHitFrame("Illegal ResourceRefs", NearInfinity.getInstance());

    ResourceEntry spawnRef = ResourceFactory.getInstance().getResourceEntry("SPAWNGRP.2DA");
    if (spawnRef != null) {
      PlainTextResource spawn = (PlainTextResource)ResourceFactory.getResource(spawnRef);
      extraValues = spawn.extract2DAHeaders();
    }

    boxes = new JCheckBox[filetypes.length];
    bstart.setMnemonic('s');
    bcancel.setMnemonic('c');
    binvert.setMnemonic('i');
    bstart.addActionListener(this);
    bcancel.addActionListener(this);
    binvert.addActionListener(this);
    getRootPane().setDefaultButton(bstart);

    JPanel boxpanel = new JPanel(new GridLayout(0, 2, 3, 3));
    for (int i = 0; i < boxes.length; i++) {
      boxes[i] = new JCheckBox(filetypes[i], true);
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
    Center.center(this, NearInfinity.getInstance().getBounds());
    setVisible(true);
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bstart) {
      setVisible(false);
      files = new ArrayList<ResourceEntry>();
      for (int i = 0; i < filetypes.length; i++) {
        if (boxes[i].isSelected())
          files.addAll(ResourceFactory.getInstance().getResources(filetypes[i]));
      }
      if (files.size() > 0)
        new Thread(this).start();
    }
    else if (event.getSource() == binvert) {
      for (final JCheckBox box : boxes)
        box.setSelected(!box.isSelected());
    }
    else if (event.getSource() == bcancel)
      setVisible(false);
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Runnable ---------------------

  public void run()
  {
    ProgressMonitor progress = new ProgressMonitor(NearInfinity.getInstance(), "Checking...", null, 0,
                                                   files.size());
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
        search(entry, (AbstractStruct)resource);
      }
      progress.setProgress(i + 1);
      if (progress.isCanceled()) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Check canceled", "Info",
                                      JOptionPane.INFORMATION_MESSAGE);
        return;
      }
    }
    System.out.println("Check completed: " + (System.currentTimeMillis() - startTime) + "ms.");
    hitFrame.setVisible(true);
  }

// --------------------- End Interface Runnable ---------------------

  private void search(ResourceEntry entry, AbstractStruct struct)
  {
    List<StructEntry> structList = struct.getFlatList();
    for (int i = 0; i < structList.size(); i++) {
      Object o = structList.get(i);
      if (o instanceof SpawnResourceRef) {
        SpawnResourceRef ref = (SpawnResourceRef)o;
        String resourceName = ref.getResourceName();
        if (resourceName.length() > 5 && resourceName.substring(0, 5).equalsIgnoreCase("None."))
          ;
        else if (extraValues != null && extraValues.contains(ref.getResName()))
          ;
        else if (!ResourceFactory.getInstance().resourceExists(resourceName))
          hitFrame.addHit(entry, entry.getSearchString(), ref);
        else if (!ref.isLegalEntry(ResourceFactory.getInstance().getResourceEntry(resourceName))) {
          hitFrame.addHit(entry, entry.getSearchString(), ref);
        }
      }
      else if (o instanceof ResourceRef) {
        ResourceRef ref = (ResourceRef)o;
        String resourceName = ref.getResourceName();
        if (resourceName.length() > 5 && resourceName.substring(0, 5).equalsIgnoreCase("None."))
          ;
        else if (struct instanceof CreResource && resourceName.substring(0, 3).equalsIgnoreCase("rnd"))
          ;
        else if (!ResourceFactory.getInstance().resourceExists(resourceName))
          hitFrame.addHit(entry, entry.getSearchString(), ref);
        else if (!ref.isLegalEntry(ResourceFactory.getInstance().getResourceEntry(resourceName)))
          hitFrame.addHit(entry, entry.getSearchString(), ref);
      }
    }
  }
}

