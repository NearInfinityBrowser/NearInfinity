// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.mus;

import infinity.gui.BrowserMenuBar;
import infinity.icon.Icons;
import infinity.resource.sound.AudioPlayer;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

public class Viewer2 extends JPanel implements Runnable, ActionListener
{
  private final DefaultListModel listModel = new DefaultListModel();
  private final JList list = new JList(listModel);
  private final AudioPlayer player = new AudioPlayer();
  private final List<Entry2> entryList = new ArrayList<Entry2>();

  private JLabel playList;
  private JButton bPlay, bEnd, bStop;
  private boolean play, end, closed;


  public Viewer2(MusResource mus)
  {
    closed = false;
    initGUI();
    loadMusResource(mus);
  }

//--------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bPlay) {
      new Thread(this).start();
    } else if (event.getSource() == bStop) {
      bStop.setEnabled(false);
      bEnd.setEnabled(false);
      play = false;
      player.stopPlay();
    } else if (event.getSource() == bEnd) {
      bEnd.setEnabled(false);
      end = true;
    }
  }

//--------------------- End Interface ActionListener ---------------------

//--------------------- Begin Interface Runnable ---------------------

  public void run()
  {
    bPlay.setEnabled(false);
    bStop.setEnabled(true);
    bEnd.setEnabled(true);
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
          player.play(entryList.get(nextnr).getAudioBuffer());
        } else if (entryList.get(nextnr).getEndBuffer() != null) {
          player.play(entryList.get(nextnr).getEndBuffer());
          play = false;
        }
        if (!end) {
          nextnr = entryList.get(nextnr).getNextNr();
          if (nextnr == -1 || nextnr == entryList.size()) {
            play = false;
          }
        }
      }
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this, "Error during playback", "Error", JOptionPane.ERROR_MESSAGE);
      e.printStackTrace();
    }
    bPlay.setEnabled(true);
    bStop.setEnabled(false);
    bEnd.setEnabled(false);
    list.setEnabled(true);
    list.setSelectedIndex(0);
    list.ensureIndexIsVisible(0);
  }

//--------------------- End Interface Runnable ---------------------

  public void close()
  {
    setClosed(true);
    stopPlay();
//    Entry2.clearCache();
  }

  // Creates a new music list and loads all associated soundtracks
  public void loadMusResource(MusResource mus)
  {
    new Thread(new MusLoader(mus)).start();
  }

  private synchronized void parseMusFile(MusResource mus)
  {
    if (!isClosed()) {
      stopPlay();
      bPlay.setEnabled(false);
      list.setEnabled(false);
      StringTokenizer tokenizer = new StringTokenizer(mus.getText(), "\n");
      String dir = tokenizer.nextToken().trim();
      listModel.clear();
      entryList.clear();
      int count = Integer.valueOf(tokenizer.nextToken().trim()).intValue();
      for (int i = 0; i < count; i++) {
        if (isClosed()) return;
        Entry2 entry = new Entry2(mus.getResourceEntry(), dir, entryList, tokenizer.nextToken().trim(), i);
        entryList.add(entry);
        listModel.addElement(entry);
      }

      for (final Entry2 entry: entryList) {
        if (isClosed()) return;
        try {
          entry.init();
        } catch (Exception e) {
          e.printStackTrace();
          JOptionPane.showMessageDialog(getTopLevelAncestor(), "Error loading " + entry.toString() + '\n' +
                                        e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
      }

      boolean enable = (!entryList.isEmpty() && entryList.get(0).getAudioBuffer() != null);
      bPlay.setEnabled(enable);
      list.setEnabled(enable);
      list.repaint();
      list.setSelectedIndex(0);
    }
  }


  private void initGUI()
  {
    bPlay = new JButton("Play", Icons.getIcon("Play16.gif"));
    bPlay.addActionListener(this);
    bEnd = new JButton("Finish", Icons.getIcon("End16.gif"));
    bEnd.setEnabled(false);
    bEnd.addActionListener(this);
    bStop = new JButton("Stop", Icons.getIcon("Stop16.gif"));
    bStop.setEnabled(false);
    bStop.addActionListener(this);

    JPanel buttonPanel = new JPanel(new GridLayout(1, 0, 6, 0));
    buttonPanel.add(bPlay);
    buttonPanel.add(bEnd);
    buttonPanel.add(bStop);

    list.setEnabled(false);
    list.setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlShadow")));
    list.setFont(BrowserMenuBar.getInstance().getScriptFont());
    playList = new JLabel("Playlist:");

    JScrollPane scroll = new JScrollPane(list);
    JPanel centerPanel = new JPanel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    centerPanel.setLayout(gbl);
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(playList, gbc);
    centerPanel.add(playList);
    gbl.setConstraints(scroll, gbc);
    centerPanel.add(scroll);
    gbl.setConstraints(buttonPanel, gbc);
    centerPanel.add(buttonPanel);

    setLayout(new BorderLayout());
    add(centerPanel, BorderLayout.CENTER);
  }

  public void stopPlay()
  {
    if (player != null) {
      play = false;
      player.stopPlay();
    }
  }

  private synchronized void setClosed(boolean b)
  {
    if (b != closed) {
      closed = b;
    }
  }

  private boolean isClosed()
  {
    return closed;
  }

//-------------------------- INNER CLASSES --------------------------

  private class MusLoader implements Runnable
  {
    private final MusResource mus;

    private MusLoader(MusResource mus)
    {
      this.mus = mus;
    }

    public void run()
    {
      try {
        if (mus != null && mus.getViewer() != null ) {
          mus.getViewer().parseMusFile(mus);
        }
      } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(mus.getViewer().getTopLevelAncestor(), e.getMessage(), "Error",
                                      JOptionPane.ERROR_MESSAGE);
      }
    }
  }
}
