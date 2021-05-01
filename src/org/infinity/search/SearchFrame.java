// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.NearInfinity;
import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.ViewFrame;
import org.infinity.icon.Icons;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.Viewable;
import org.infinity.resource.bcs.BcsResource;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Misc;

public final class SearchFrame extends ChildFrame implements ActionListener, ListSelectionListener, Runnable
{
  private static final SearchFrame searchframe = null;
  private final CardLayout cards = new CardLayout();
  private final JButton bopen = new JButton("Open", Icons.getIcon(Icons.ICON_OPEN_16));
  private final JButton bopennew = new JButton("Open in new window", Icons.getIcon(Icons.ICON_OPEN_16));
  private final JButton binsert = new JButton("Insert reference", Icons.getIcon(Icons.ICON_PASTE_16));
  private final JButton bsearch = new JButton("Search", Icons.getIcon(Icons.ICON_FIND_16));
  private final JList<String> list = new JList<>();
  private final JPanel bpanel;
  private final JProgressBar progress = new JProgressBar();
  private final JRadioButton rbcre = new JRadioButton("Creatures");
  private final JRadioButton rbitm = new JRadioButton("Items");
  private final JRadioButton rbspl = new JRadioButton("Spells");
  private final JRadioButton rbsto = new JRadioButton("Stores");
  private final JTextField tfield = new JTextField(10);

  public static void clearCache()
  {
    if (searchframe != null) {
      searchframe.list.setListData(new String[0]);
      searchframe.bopen.setEnabled(false);
      searchframe.bopennew.setEnabled(false);
      searchframe.binsert.setEnabled(false);
    }
  }

  public SearchFrame()
  {
    super("Find");
    setIconImage(Icons.getIcon(Icons.ICON_FIND_16).getImage());
    getRootPane().setDefaultButton(bsearch);
    bopen.setMnemonic('o');
    bopennew.setMnemonic('n');
    binsert.setMnemonic('r');
    rbcre.setMnemonic('c');
    rbitm.setMnemonic('i');
    rbspl.setMnemonic('s');
    rbsto.setMnemonic('t');
    ButtonGroup bg = new ButtonGroup();
    bg.add(rbcre);
    bg.add(rbitm);
    bg.add(rbspl);
    bg.add(rbsto);
    rbcre.setSelected(true);

    list.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    JScrollPane scroll = new JScrollPane(list);
    list.setEnabled(false);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.addListSelectionListener(this);
    bopen.setEnabled(false);
    bopennew.setEnabled(false);
    binsert.setEnabled(false);
    binsert.setToolTipText("Inserts reference to selected item into script displayed in main window");
    JLabel label = new JLabel("Name:");
    label.setLabelFor(tfield);
    label.setDisplayedMnemonic('n');
    JLabel resultLabel = new JLabel("Result:");
    bsearch.addActionListener(this);
    bopen.addActionListener(this);
    bopennew.addActionListener(this);
    binsert.addActionListener(this);
    list.addMouseListener(new MouseAdapter()
    {
      @Override
      public void mouseClicked(MouseEvent event)
      {
        if (event.getClickCount() == 2) {
          String selected = list.getSelectedValue();
          String resname = selected.substring(0, selected.indexOf(" - "));
          ResourceEntry entry = ResourceFactory.getResourceEntry(resname);
          NearInfinity.getInstance().showResourceEntry(entry);
        }
      }
    });
    addWindowListener(new WindowAdapter()
    {
      @Override
      public void windowOpened(WindowEvent event)
      {
        tfield.requestFocus();
      }
    });

    JPanel lpanel2 = new JPanel();
    lpanel2.setLayout(new FlowLayout());
    lpanel2.add(binsert);
    lpanel2.add(bopen);
    lpanel2.add(bopennew);

    bpanel = new JPanel();
    bpanel.setLayout(cards);
    bpanel.add(lpanel2, "Button");
    bpanel.add(progress, "Progress");
    cards.show(bpanel, "Button");

    JPanel rbpanel = new JPanel();
    rbpanel.setLayout(new GridLayout(1, 4, 3, 3));
    rbpanel.add(rbcre);
    rbpanel.add(rbitm);
    rbpanel.add(rbspl);
    rbpanel.add(rbsto);
    rbpanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Find:"),
                                                         BorderFactory.createEmptyBorder(3, 6, 3, 3)));

    JPanel pane = (JPanel)getContentPane();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    pane.setLayout(gbl);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(6, 4, 3, 5);
    gbl.setConstraints(rbpanel, gbc);
    pane.add(rbpanel);

    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.insets = new Insets(3, 6, 3, 3);
    gbl.setConstraints(label, gbc);
    pane.add(label);

    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.anchor = GridBagConstraints.SOUTH;
    gbc.insets.left = 3;
    tfield.setPreferredSize(new Dimension((int)tfield.getPreferredSize().getWidth(),
                                          (int)bsearch.getPreferredSize().getHeight()));
    gbl.setConstraints(tfield, gbc);
    pane.add(tfield);

    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.weightx = 0.0;
    gbc.insets = new Insets(6, 3, 3, 7);
    gbl.setConstraints(bsearch, gbc);
    pane.add(bsearch);

    gbc.anchor = GridBagConstraints.CENTER;
    gbc.insets.left = 6;
    gbc.insets.bottom = 0;
    gbl.setConstraints(resultLabel, gbc);
    pane.add(resultLabel);

    gbc.insets = new Insets(3, 4, 3, 5);
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbl.setConstraints(scroll, gbc);
    pane.add(scroll);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.insets = new Insets(3, 6, 6, 7);
    gbl.setConstraints(bpanel, gbc);
    pane.add(bpanel);

    setSize(Misc.getScaledValue(500), Misc.getScaledValue(450));
    Center.center(this, NearInfinity.getInstance().getBounds());
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == tfield || event.getSource() == bsearch) {
      if (tfield.getText() == null || tfield.getText().equals(""))
        return;
      new Thread(this).start();
    }
    else if (event.getSource() == bopen) {
      String selected = list.getSelectedValue();
      String resname = selected.substring(0, selected.indexOf(" - "));
      ResourceEntry entry = ResourceFactory.getResourceEntry(resname);
      NearInfinity.getInstance().showResourceEntry(entry);
    }
    else if (event.getSource() == bopennew) {
      String selected = list.getSelectedValue();
      String resname = selected.substring(0, selected.indexOf(" - "));
      new ViewFrame(this, ResourceFactory.getResource(ResourceFactory.getResourceEntry(resname)));
    }
    else if (event.getSource() == binsert) {
      Viewable viewable = NearInfinity.getInstance().getViewable();
      if (viewable == null || !(viewable instanceof BcsResource)) {
        JOptionPane.showMessageDialog(this, "No script displayed in the main window", "Error",
                                      JOptionPane.ERROR_MESSAGE);
        return;
      }
      String selected = list.getSelectedValue();
      String resname = selected.substring(0, selected.indexOf("."));
      ((BcsResource)viewable).insertString('\"' + resname + '\"');
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent event)
  {
    bopen.setEnabled(true);
    bopennew.setEnabled(true);
    binsert.setEnabled(true);
  }

// --------------------- End Interface ListSelectionListener ---------------------


// --------------------- Begin Interface Runnable ---------------------

  @Override
  public void run()
  {
    list.setEnabled(false);
    tfield.setEnabled(false);
    list.clearSelection();
    bsearch.setEnabled(false);
    bopen.setEnabled(false);
    bopennew.setEnabled(false);
    binsert.setEnabled(false);
    list.setListData(new String[]{});
    rbcre.setEnabled(false);
    rbitm.setEnabled(false);
    rbspl.setEnabled(false);
    rbsto.setEnabled(false);

    String selectedtype = "";
    if (rbcre.isSelected())
      selectedtype = "CRE";
    else if (rbitm.isSelected())
      selectedtype = "ITM";
    else if (rbspl.isSelected())
      selectedtype = "SPL";
    else if (rbsto.isSelected())
      selectedtype = "STO";

    List<ResourceEntry> resources = ResourceFactory.getResources(selectedtype);
    String expr = tfield.getText().toLowerCase(Locale.ENGLISH);
    List<String> found = new ArrayList<>();
    cards.show(bpanel, "Progress");
    progress.setMaximum(resources.size());
    for (int i = 0; i < resources.size(); i++) {
      ResourceEntry entry = resources.get(i);
      String string = entry.getSearchString();
      if (string != null && string.toLowerCase(Locale.ENGLISH).indexOf(expr) != -1)
        found.add(entry.toString() + " - " + string);
      progress.setValue(i + 1);
    }
    cards.show(bpanel, "Button");
    progress.setValue(0);

    list.ensureIndexIsVisible(0);
    if (found.size() > 0) {
      Collections.sort(found);
      list.setListData(found.toArray(new String[found.size()]));
      list.setEnabled(true);
    }

    rbcre.setEnabled(true);
    rbitm.setEnabled(true);
    rbspl.setEnabled(true);
    rbsto.setEnabled(true);
    tfield.setEnabled(true);
    bsearch.setEnabled(true);
  }

// --------------------- End Interface Runnable ---------------------
}

