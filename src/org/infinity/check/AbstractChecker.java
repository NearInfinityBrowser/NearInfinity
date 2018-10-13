// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.check;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import org.infinity.NearInfinity;
import org.infinity.gui.Center;
import org.infinity.icon.Icons;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.FileTypeSelector;

/**
 * Base class for selectiong files for check and runs checks in multithreaded mode.
 *
 * @author Mingun
 */
public abstract class AbstractChecker extends TypeChecker implements ActionListener, Runnable {
  /** Selector of file types in which search must be performed. */
  private final FileTypeSelector selector;
  /** Button that begins check with specified settings. */
  private final JButton bStart  = new JButton("Check", Icons.getIcon(Icons.ICON_FIND_16));
  /** Button that closes start check dialog. */
  private final JButton bCancel = new JButton("Cancel", Icons.getIcon(Icons.ICON_DELETE_16));

  /** Key used to save and restore checkbox selection. */
  private final String key;
  /** Resources, selected for check. */
  protected List<ResourceEntry> files;

  public AbstractChecker(String title, String key, String[] filetypes)
  {
    super(title);
    setIconImage(Icons.getIcon(Icons.ICON_REFRESH_16).getImage());
    this.key = key;
    selector = new FileTypeSelector("Select files to check:", key, filetypes, null);

    final JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    bpanel.add(bStart);
    bpanel.add(bCancel);

    final JPanel pane = (JPanel)getContentPane();
    pane.setLayout(new BorderLayout());
    pane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    pane.add(selector, BorderLayout.CENTER);
    pane.add(bpanel, BorderLayout.SOUTH);

    bStart.setMnemonic('s');
    bCancel.setMnemonic('c');
    bStart.addActionListener(this);
    bCancel.addActionListener(this);
    getRootPane().setDefaultButton(bStart);

    pack();
    Center.center(this, NearInfinity.getInstance().getBounds());
    setVisible(true);
  }

  //<editor-fold defaultstate="collapsed" desc="ActionListeneer">
  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bStart) {
      setVisible(false);
      files = selector.getResources(key);
      if (!files.isEmpty()) {
        new Thread(this).start();
      }
    }
    else if (event.getSource() == bCancel) {
      setVisible(false);
    }
  }
  //</editor-fold>
}
