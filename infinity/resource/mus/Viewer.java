// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.mus;

import infinity.NearInfinity;
import infinity.gui.BrowserMenuBar;
import infinity.gui.WindowBlocker;
import infinity.icon.Icons;
import infinity.resource.sound.SoundUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.StringTokenizer;

final class Viewer extends JPanel implements Runnable, ActionListener
{
  private final JButton bplay = new JButton(Icons.getIcon("Play16.gif"));
  private final JButton bstop = new JButton(Icons.getIcon("Stop16.gif"));
  private final JButton bend = new JButton(Icons.getIcon("End16.gif"));
  private final DefaultListModel listModel = new DefaultListModel();
  private final JList list = new JList(listModel);
  private final SoundUtilities a2w = new SoundUtilities();
  private final JButton bconvert = new JButton("Convert", Icons.getIcon("Refresh16.gif"));
  private Entry[] entries;
  private boolean play = true, end;

  Viewer(MusResource mus)
  {
    bplay.addActionListener(this);
    bstop.addActionListener(this);
    bend.addActionListener(this);
    bstop.setEnabled(false);
    bend.setEnabled(false);
    bplay.setEnabled(false);
    JPanel buttonpanel = new JPanel(new GridLayout(1, 0, 6, 0));
    buttonpanel.add(bplay);
    buttonpanel.add(bend);
    buttonpanel.add(bstop);
    if (!BrowserMenuBar.getInstance().autoConvertMUS()) {
      bplay.setToolTipText("You must convert the files before playback is possible");
      bconvert.addActionListener(this);
      buttonpanel.add(bconvert);
    }

    list.setEnabled(false);
    list.setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlShadow")));
    list.setFont(BrowserMenuBar.getInstance().getScriptFont());
    JLabel label = new JLabel("Playlist:");

    JScrollPane scroll = new JScrollPane(list);
    JPanel centerpanel = new JPanel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    centerpanel.setLayout(gbl);
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(label, gbc);
    centerpanel.add(label);
    gbl.setConstraints(scroll, gbc);
    centerpanel.add(scroll);
    gbl.setConstraints(buttonpanel, gbc);
    centerpanel.add(buttonpanel);

    setLayout(new BorderLayout());
    add(centerpanel, BorderLayout.CENTER);

    parseMusfile(mus);
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bplay)
      new Thread(this).start();
    else if (event.getSource() == bstop) {
      bstop.setEnabled(false);
      bend.setEnabled(false);
      play = false;
      a2w.stopPlay();
    }
    else if (event.getSource() == bend) {
      bend.setEnabled(false);
      end = true;
    }
    else if (event.getSource() == bconvert) {
      bconvert.setEnabled(false);
      new Thread(new Convert(getTopLevelAncestor())).start();
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Runnable ---------------------

  public void run()
  {
    bplay.setEnabled(false);
    bstop.setEnabled(true);
    bend.setEnabled(true);
    list.setEnabled(false);
    int nextnr = list.getSelectedIndex();
    if (nextnr == -1)
      nextnr = 0;
    play = true;
    end = false;
    try {
      while (play) {
        if (!end) {
          list.setSelectedIndex(nextnr);
          list.ensureIndexIsVisible(nextnr);
          list.repaint();
          a2w.play(entries[nextnr].getWavFile());
        }
        else {
          if (entries[nextnr].getEndfile() != null)
            a2w.play(entries[nextnr].getEndfile());
          play = false;
        }
        if (!end) {
          nextnr = entries[nextnr].getNextNr();
          if (nextnr == -1 || nextnr == entries.length)
            play = false;
        }
      }
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this, "Error during playback", "Error", JOptionPane.ERROR_MESSAGE);
      e.printStackTrace();
    }
    bplay.setEnabled(true);
    bstop.setEnabled(false);
    bend.setEnabled(false);
    list.setEnabled(true);
    list.setSelectedIndex(0);
    list.ensureIndexIsVisible(0);
  }

// --------------------- End Interface Runnable ---------------------

  public void close()
  {
    play = false;
    a2w.stopPlay();
    if (entries != null) {
      for (final Entry entry : entries)
        if (entry != null)
          entry.close();
    }
  }

  void parseMusfile(MusResource mus)
  {
    close();
    StringTokenizer tokenizer = new StringTokenizer(mus.getText(), "\n");
    String dir = tokenizer.nextToken().trim();
    listModel.clear();
    entries = new Entry[Integer.valueOf(tokenizer.nextToken().trim()).intValue()];
    for (int i = 0; i < entries.length; i++) {
      entries[i] = new Entry(mus.getResourceEntry(), dir, entries, tokenizer.nextToken().trim(), i);
      listModel.addElement(entries[i]);
    }
    if (BrowserMenuBar.getInstance().autoConvertMUS())
      new Thread(new Convert(getTopLevelAncestor())).start();
  }

// -------------------------- INNER CLASSES --------------------------

  private final class Convert implements Runnable
  {
    private final Component parent;

    private Convert(Component parent)
    {
      this.parent = parent;
    }

    public void run()
    {
      WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
      blocker.setBlocked(true);
      for (final Entry entry : entries)
        try {
          entry.init();
        } catch (IOException e) {
          e.printStackTrace();
          JOptionPane.showMessageDialog(parent, "Error converting " + entry + '\n' + e.getMessage(),
                                        "Error", JOptionPane.ERROR_MESSAGE);
          if (e.getMessage().equals("acm2wav.exe not found"))
            break;
        }
      blocker.setBlocked(false);
      if (bplay != null) {
        bplay.setToolTipText(null);
        bplay.setEnabled(entries[0].getWavFile() != null);
        list.setEnabled(entries[0].getWavFile() != null);
        list.setSelectedIndex(0);
      }
    }
  }
}

