// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.check;

import infinity.NearInfinity;
import infinity.datatype.DecNumber;
import infinity.datatype.SectionCount;
import infinity.gui.Center;
import infinity.gui.ChildFrame;
import infinity.icon.Icons;
import infinity.resource.AbstractAbility;
import infinity.resource.AbstractStruct;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.key.ResourceEntry;
import infinity.search.ReferenceHitFrame;
import infinity.util.Debugging;
import infinity.util.Misc;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ProgressMonitor;

public class EffectsIndexChecker extends ChildFrame implements ActionListener, Runnable
{
  private static final String FMT_PROGRESS = "Checking %ss...";

  private static final String[] FILETYPES = {"ITM", "SPL"};
  private final JButton bstart = new JButton("Check", Icons.getIcon("Find16.gif"));
  private final JButton bcancel = new JButton("Cancel", Icons.getIcon("Delete16.gif"));
  private final JButton binvert = new JButton("Invert", Icons.getIcon("Refresh16.gif"));
  private final JCheckBox[] boxes = new JCheckBox[FILETYPES.length];
  private final ReferenceHitFrame hitFrame;
  private List<ResourceEntry> files;
  private ProgressMonitor progress;
  private int progressIndex;

  public EffectsIndexChecker()
  {
    super("Effects Index Checker");
    setIconImage(Icons.getIcon("Refresh16.gif").getImage());
    hitFrame = new ReferenceHitFrame("Mis-indexed Effects", NearInfinity.getInstance());

    bstart.setMnemonic('s');
    bcancel.setMnemonic('c');
    binvert.setMnemonic('i');
    bstart.addActionListener(this);
    bcancel.addActionListener(this);
    binvert.addActionListener(this);
    getRootPane().setDefaultButton(bstart);

    JPanel boxpanel = new JPanel(new GridLayout(0, 2, 3, 3));
    for (int i = 0; i < boxes.length; i++) {
      boxes[i] = new JCheckBox(FILETYPES[i], true);
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
//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bstart) {
      setVisible(false);
      files = new ArrayList<ResourceEntry>();
      for (int i = 0; i < FILETYPES.length; i++) {
        if (boxes[i].isSelected())
          files.addAll(ResourceFactory.getResources(FILETYPES[i]));
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

  @Override
  public void run()
  {
    try {
      String type = "WWWW";
      progressIndex = 0;
      progress = new ProgressMonitor(NearInfinity.getInstance(), "Checking...",
                                     String.format(FMT_PROGRESS, type),
                                     0, files.size());
      progress.setMillisToDecideToPopup(100);
      ThreadPoolExecutor executor = Misc.createThreadPool();
      boolean isCancelled = false;
      Debugging.timerReset();
      for (int i = 0; i < files.size(); i++) {
        ResourceEntry entry = files.get(i);
        if (i % 10 == 0) {
          String ext = entry.getExtension();
          if (ext != null && !type.equalsIgnoreCase(ext)) {
            type = ext;
            progress.setNote(String.format(FMT_PROGRESS, type));
          }
        }
        Misc.isQueueReady(executor, true, -1);
        executor.execute(new Worker(entry));
        if (progress.isCanceled()) {
          isCancelled = true;
          break;
        }
      }

      // enforcing thread termination if process has been cancelled
      if (isCancelled) {
        executor.shutdownNow();
      } else {
        executor.shutdown();
      }

      // waiting for pending threads to terminate
      while (!executor.isTerminated()) {
        if (!isCancelled && progress.isCanceled()) {
          executor.shutdownNow();
          isCancelled = true;
        }
        try { Thread.sleep(1); } catch (InterruptedException e) {}
      }

      if (isCancelled) {
        hitFrame.close();
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Check cancelled", "Info",
                                      JOptionPane.INFORMATION_MESSAGE);
      } else {
        hitFrame.setVisible(true);
      }
    } finally {
      advanceProgress(true);
    }
    Debugging.timerShow("Check completed", Debugging.TimeFormat.MILLISECONDS);
  }

  private void search(ResourceEntry entry, AbstractStruct struct)
  {
    int numGlobalEffects = ((SectionCount) struct.getAttribute("# global effects")).getValue();
    int expectedEffectsIndex = numGlobalEffects;
    List<StructEntry> structList = struct.getList();
    for (int i = 0; i < structList.size(); i++) {
      Object o = structList.get(i);
      if (o instanceof AbstractAbility) {
        AbstractAbility abil = (AbstractAbility) o;
        int effectsIndex = ((DecNumber) abil.getAttribute("First effect index")).getValue();
        if (effectsIndex != expectedEffectsIndex) {
          synchronized (hitFrame) {
            hitFrame.addHit(entry, entry.getSearchString(), abil);
          }
        }
        expectedEffectsIndex += abil.getEffectsCount();
      }
    }
  }

  private synchronized void advanceProgress(boolean finished)
  {
    if (progress != null) {
      if (finished) {
        progressIndex = 0;
        progress.close();
        progress = null;
      } else {
        progressIndex++;
        progress.setProgress(progressIndex);
      }
    }
  }

//-------------------------- INNER CLASSES --------------------------

  private class Worker implements Runnable
  {
    private final ResourceEntry entry;

    public Worker(ResourceEntry entry)
    {
      this.entry = entry;
    }

    @Override
    public void run()
    {
      if (entry != null) {
        Resource resource = ResourceFactory.getResource(entry);
        if (resource != null) {
          search(entry, (AbstractStruct)resource);
        }
      }
      advanceProgress(false);
    }
  }
}
