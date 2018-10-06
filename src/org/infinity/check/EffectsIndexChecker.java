// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.check;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.infinity.NearInfinity;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.SectionCount;
import org.infinity.gui.Center;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractAbility;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.spl.SplResource;
import org.infinity.search.ReferenceHitFrame;

public class EffectsIndexChecker extends TypeChecker implements ActionListener, Runnable
{
  private static final String[] FILETYPES = {"ITM", "SPL"};
  private final JButton bstart = new JButton("Check", Icons.getIcon(Icons.ICON_FIND_16));
  private final JButton bcancel = new JButton("Cancel", Icons.getIcon(Icons.ICON_DELETE_16));
  private final JButton binvert = new JButton("Invert", Icons.getIcon(Icons.ICON_REFRESH_16));
  private final JCheckBox[] boxes = new JCheckBox[FILETYPES.length];
  private final ReferenceHitFrame hitFrame;
  private List<ResourceEntry> files;

  public EffectsIndexChecker()
  {
    super("Effects Index Checker");
    setIconImage(Icons.getIcon(Icons.ICON_REFRESH_16).getImage());
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
      files = new ArrayList<>();
      for (int i = 0; i < FILETYPES.length; i++) {
        if (boxes[i].isSelected()) {
          files.addAll(ResourceFactory.getResources(FILETYPES[i]));
        }
      }
      if (!files.isEmpty()) {
        new Thread(this).start();
      }
    }
    else if (event.getSource() == binvert) {
      for (final JCheckBox box : boxes) {
        box.setSelected(!box.isSelected());
      }
    }
    else if (event.getSource() == bcancel) {
      setVisible(false);
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Runnable ---------------------

  @Override
  public void run()
  {
    if (runCheck(files)) {
      hitFrame.close();
    } else {
      hitFrame.setVisible(true);
    }
  }

  @Override
  protected Runnable newWorker(ResourceEntry entry) {
    return () -> {
      final Resource resource = ResourceFactory.getResource(entry);
      if (resource != null) {
        search(entry, (AbstractStruct)resource);
      }
      advanceProgress();
    };
  }

  private void search(ResourceEntry entry, AbstractStruct struct)
  {
    final int numGlobalEffects = ((SectionCount) struct.getAttribute(SplResource.SPL_NUM_GLOBAL_EFFECTS)).getValue();
    int expectedEffectsIndex = numGlobalEffects;
    for (StructEntry e : struct.getList()) {
      if (e instanceof AbstractAbility) {
        final AbstractAbility abil = (AbstractAbility) e;
        final int effectsIndex = ((DecNumber) abil.getAttribute(AbstractAbility.ABILITY_FIRST_EFFECT_INDEX)).getValue();
        if (effectsIndex != expectedEffectsIndex) {
          synchronized (hitFrame) {
            hitFrame.addHit(entry, entry.getSearchString(), abil);
          }
        }
        expectedEffectsIndex += abil.getEffectsCount();
      }
    }
  }
}
