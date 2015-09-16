// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.search;

import infinity.datatype.TextString;
import infinity.gui.Center;
import infinity.gui.ChildFrame;
import infinity.gui.ViewerUtil;
import infinity.icon.Icons;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.cre.CreResource;
import infinity.resource.key.ResourceEntry;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ProgressMonitor;

abstract class AbstractReferenceSearcher implements Runnable, ActionListener
{
  protected static final String[] FILE_TYPES = {"2DA", "ARE", "BCS", "CHR", "CHU", "CRE", "DLG",
                                                "EFF", "GAM", "INI", "ITM", "PRO", "SAV", "SPL",
                                                "STO", "VEF", "VVC", "WED", "WMP"};

  private static HashMap<String, Boolean[]> lastSelection = new HashMap<String, Boolean[]>();

  final ResourceEntry targetEntry;

  private final ChildFrame selectframe = new ChildFrame("References", true);
  private final Component parent;
  private final JButton bStart = new JButton("Search", Icons.getIcon("Find16.gif"));
  private final JButton bCancel = new JButton("Cancel", Icons.getIcon("Delete16.gif"));
  private final JButton bInvert = new JButton("Invert");
  private final JButton bClear = new JButton("Clear");
  private final JButton bSet = new JButton("Set");
  private final JButton bDefault = new JButton("Default");
  private final ReferenceHitFrame hitFrame;
  private final String[] filetypes;
  private final boolean[] preselect;
  protected String targetEntryName;   // optional alternate name to search for
  private JCheckBox[] boxes;
  private List<ResourceEntry> files;

  AbstractReferenceSearcher(ResourceEntry targetEntry, String filetypes[], Component parent)
  {
    this(targetEntry, filetypes, setSelectedFileTypes(targetEntry, filetypes), parent);
  }

  AbstractReferenceSearcher(ResourceEntry targetEntry, String filetypes[], boolean[] preselect, Component parent)
  {
    this.targetEntry = targetEntry;
    if (targetEntry.getExtension().equalsIgnoreCase("CRE")) {
      String resName = targetEntry.getResourceName();
      if (resName.lastIndexOf('.') > 0) {
        resName = resName.substring(0, resName.lastIndexOf('.'));
      }
      try {
        CreResource cre = new CreResource(targetEntry);
        StructEntry nameEntry = cre.getAttribute("Script name");
        if (nameEntry instanceof TextString) {
          targetEntryName = ((TextString)nameEntry).toString();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    this.filetypes = filetypes;
    this.preselect = preselect;
    this.parent = parent;
    hitFrame = new ReferenceHitFrame(targetEntry, parent);
    if (filetypes.length == 1) {
      files = new ArrayList<ResourceEntry>();
      files.addAll(ResourceFactory.getResources(filetypes[0]));
      if (files.size() > 0)
        new Thread(this).start();
    }
    else {
      boxes = new JCheckBox[filetypes.length];
      bStart.setMnemonic('s');
      bCancel.setMnemonic('c');
      bInvert.setMnemonic('i');
      bDefault.setMnemonic('d');
      bStart.addActionListener(this);
      bCancel.addActionListener(this);
      bInvert.addActionListener(this);
      bClear.addActionListener(this);
      bSet.addActionListener(this);
      bDefault.addActionListener(this);
      selectframe.getRootPane().setDefaultButton(bStart);
      selectframe.setIconImage(Icons.getIcon("Find16.gif").getImage());

      JPanel boxpanel = new JPanel(new GridLayout(0, 2, 3, 3));
      for (int i = 0; i < boxes.length; i++) {
        boxes[i] = new JCheckBox(filetypes[i], isPreselected(filetypes, preselect, i));
        boxpanel.add(boxes[i]);
      }
      boxpanel.setBorder(BorderFactory.createEmptyBorder(3, 12, 3, 0));
      Boolean[] selection = lastSelection.get(this.targetEntry.getExtension());
      if (selection != null) {
        for (int i = 0; i < selection.length; i++) {
          boxes[i].setSelected(selection[i]);
        }
      }

      GridBagConstraints gbc = new GridBagConstraints();
      JPanel ipanel1 = new JPanel(new GridBagLayout());
      gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
      ipanel1.add(bClear, gbc);
      gbc = ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
      ipanel1.add(bSet, gbc);
      gbc = ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
      ipanel1.add(bDefault, gbc);
      gbc = ViewerUtil.setGBC(gbc, 1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                              GridBagConstraints.HORIZONTAL, new Insets(4, 8, 0, 0), 0, 0);
      ipanel1.add(bInvert, gbc);
      JPanel ipanel = new JPanel(new BorderLayout());
      ipanel.add(ipanel1, BorderLayout.CENTER);

      JPanel innerpanel = new JPanel(new BorderLayout());
      innerpanel.add(boxpanel, BorderLayout.CENTER);
      innerpanel.add(ipanel, BorderLayout.SOUTH);
      innerpanel.setBorder(BorderFactory.createTitledBorder("Select files to search:"));

      JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      bpanel.add(bStart);
      bpanel.add(bCancel);

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

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bStart) {
      selectframe.setVisible(false);
      files = new ArrayList<ResourceEntry>();
      Boolean[] selection = lastSelection.get(targetEntry.getExtension());
      if (selection == null) {
        selection = new Boolean[filetypes.length];
        lastSelection.put(targetEntry.getExtension(), selection);
      }
      for (int i = 0; i < filetypes.length; i++) {
        if (boxes[i].isSelected()) {
          files.addAll(ResourceFactory.getResources(filetypes[i]));
        }
        selection[i] = Boolean.valueOf(boxes[i].isSelected());
      }
      if (files.size() > 0) {
        new Thread(this).start();
      }
    }
    else if (event.getSource() == bCancel) {
      selectframe.setVisible(false);
    }
    else if (event.getSource() == bInvert) {
      for (final JCheckBox box: boxes) {
        box.setSelected(!box.isSelected());
      }
    }
    else if (event.getSource() == bClear) {
      for (final JCheckBox box: boxes) {
        box.setSelected(false);
      }
    }
    else if (event.getSource() == bSet) {
      for (final JCheckBox box: boxes) {
        box.setSelected(true);
      }
    }
    else if (event.getSource() == bDefault) {
      if (preselect != null) {
        for (int i = 0; i < boxes.length; i++) {
          if (preselect != null) {
            boxes[i].setSelected((i < preselect.length) ? preselect[i] : false);
          } else {
            boxes[i].setSelected(true);
          }
        }
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Runnable ---------------------

  @Override
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

  ResourceEntry getTargetEntry()
  {
    return targetEntry;
  }

  private boolean isPreselected(String[] filetypes, boolean[] preselect, int index)
  {
    boolean retVal = true;
    if (index >= 0 && filetypes != null && filetypes.length > index) {
      if (preselect != null && preselect.length > index && !preselect[index]) {
        retVal = false;
      }
    }
    return retVal;
  }

  protected static boolean[] setSelectedFileTypes(ResourceEntry entry, String[] filetypes)
  {
    boolean[] retVal = null;
    if (entry != null && filetypes != null && filetypes.length > 0) {
      retVal = new boolean[filetypes.length];
      for (int i = 0; i < retVal.length; i++) retVal[i] = false;
      String[] selectedExt = null;
      String ext = entry.getExtension();
      if ("ARE".equalsIgnoreCase(ext)) {
        selectedExt = new String[]{"2DA", "BCS", "DLG", "GAM", "WMP"};
      } else if ("BAM".equalsIgnoreCase(ext)) {
        selectedExt = new String[]{"2DA", "ARE", "BCS", "CHU", "CRE", "DLG", "EFF",
                                   "GAM", "INI", "ITM", "PRO", "SPL", "VEF", "VVC", "WMP"};
      } else if ("BMP".equalsIgnoreCase(ext)) {
        selectedExt = new String[]{"2DA", "ARE", "BCS", "CHU", "CRE", "DLG", "EFF", "GAM",
                                   "INI", "ITM", "PRO", "SPL", "VEF", "VVC"};
      } else if ("CRE".equalsIgnoreCase(ext)) {
        selectedExt = new String[]{"2DA", "ARE", "BCS", "DLG", "EFF", "GAM", "INI", "ITM", "SPL"};
      } else if ("DLG".equalsIgnoreCase(ext)) {
        selectedExt = new String[]{"2DA", "BCS", "CRE", "DLG", "GAM"};
      } else if ("EFF".equalsIgnoreCase(ext)) {
        selectedExt = new String[]{"CRE", "EFF", "GAM", "ITM", "SPL"};
      } else if ("FNT".equalsIgnoreCase(ext)) {
        selectedExt = new String[]{"CHU"};
      } else if ("ITM".equalsIgnoreCase(ext)) {
        selectedExt = new String[]{"2DA", "ARE", "BCS", "CRE", "DLG", "EFF", "GAM", "ITM",
                                   "SPL", "STO"};
      } else if ("MOS".equalsIgnoreCase(ext)) {
        selectedExt = new String[]{"2DA", "ARE", "BCS", "CHU", "DLG", "WMP"};
      } else if ("MVE".equalsIgnoreCase(ext) || "WBM".equalsIgnoreCase(ext)) {
        selectedExt = new String[]{"ARE", "BCS", "CRE", "DLG", "EFF", "GAM", "ITM", "SPL"};
      } else if ("PRO".equalsIgnoreCase(ext)) {
        selectedExt = new String[]{"ARE", "CRE", "EFF", "GAM", "ITM", "SPL"};
      } else if ("PVRZ".equalsIgnoreCase(ext)) {
        selectedExt = new String[]{"BAM", "MOS", "TIS"};
      } else if ("SPL".equalsIgnoreCase(ext)) {
        selectedExt = new String[]{"2DA", "ARE", "BCS", "CRE", "DLG", "EFF", "GAM",
                                   "ITM", "SPL"};
      } else if ("STO".equalsIgnoreCase(ext)) {
        selectedExt = new String[]{"BCS", "DLG"};
      } else if ("TIS".equalsIgnoreCase(ext)) {
        selectedExt = new String[]{"WED"};
      } else if ("VVC".equalsIgnoreCase(ext)) {
        selectedExt = new String[]{"ARE", "BCS", "DLG", "EFF", "GAM", "INI", "ITM", "PRO",
                                   "SPL", "VEF", "VVC"};
      } else if ("WAV".equalsIgnoreCase(ext)) {
        selectedExt = new String[]{"2DA", "ARE", "BCS", "CRE", "DLG", "EFF", "GAM", "INI",
                                   "ITM", "PRO", "SPL", "VEF", "VVC"};
      } else if ("WED".equalsIgnoreCase(ext)) {
        selectedExt = new String[]{"ARE"};
      } else if ("WMP".equalsIgnoreCase(ext)) {
        selectedExt = new String[]{"BCS", "DLG"};
      }

      // defining preselection
      if (selectedExt != null && selectedExt.length > 0) {
        for (int i = 0; i < retVal.length; i++) {
          for (int j = 0; j < selectedExt.length; j++) {
            if (filetypes[i] != null && filetypes[i].equalsIgnoreCase(selectedExt[j])) {
              retVal[i] = true;
              break;
            }
          }
        }
      } else {
        // select all by default
        for (int i = 0; i < retVal.length; i++) {
          retVal[i] = true;
        }
      }
    }
    return retVal;
  }
}

