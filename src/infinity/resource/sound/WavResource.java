// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.sound;

import infinity.icon.Icons;
import infinity.resource.Closeable;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.ViewableContainer;
import infinity.resource.key.FileResourceEntry;
import infinity.resource.key.ResourceEntry;
import infinity.search.WavReferenceSearcher;
import infinity.util.DynamicArray;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

@Deprecated
public final class WavResource implements Resource, ActionListener, Closeable, Runnable
{
  private final ResourceEntry entry;
  private final SoundUtilities a2w = new SoundUtilities();
  private final String signature;
  private File wavfile;
  private JButton bplay, bstop, bfind, bconvert, bexport, bexportmp3, bexportConvert;
  private JPanel panel;
  private boolean fileCreated;
  private byte mp3data[];
  private int channels;

  public WavResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    byte data[] = entry.getResourceData();
    signature = new String(data, 0, 4);
    if (signature.equals("RIFF")) {
      if (DynamicArray.getShort(data, 20) == 0x11) { // IMA ADPCM
        wavfile = SoundUtilities.convertADPCM(data, 0, entry.toString());
        fileCreated = true;
      }
//      else if (ResourceFactory.getGameID() == ResourceFactory.ID_KOTOR &&
//               data[20] == 0x01) {
//        data[20] = 0x11;
//        wavfile = SoundUtilities.convertADPCM(data, 0, entry.toString());
//        fileCreated = true;
//      }
      else if (data.length > 60 && new String(data, 58, 3).equalsIgnoreCase("ID3"))
        mp3data = Arrays.copyOfRange(data, 58, data.length);
      else if (entry instanceof FileResourceEntry)
        wavfile = entry.getActualFile();
      else {
        // In BIF
        wavfile = new File(entry.toString());
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(wavfile));
        bos.write(data, 0, data.length);
        bos.close();
        fileCreated = true;
      }
    }
    else if (signature.equals("WAVC")) {
      new String(data, 4, 4); // Version
      DynamicArray.getInt(data, 8); // Unc_length
      DynamicArray.getInt(data, 12); // Com_length
      // 4 unknown bytes
      channels = (int)DynamicArray.getShort(data, 20);
      // 6 unknown bytes

      if (SoundUtilities.converterExists())
        wavfile = SoundUtilities.convert(data, 28, '_' + entry.toString(), channels == 1);
      fileCreated = true;
    }
    else if (signature.equals("BMU ")) {
      mp3data = Arrays.copyOfRange(data, 8, data.length);
    }
    else if (data.length > 480 && new String(data, 470, 4).equals("RIFF")) {
      if (DynamicArray.getShort(data, 470 + 20) == 0x11) { // IMA ADPCM
        wavfile = SoundUtilities.convertADPCM(data, 470, entry.toString());
        fileCreated = true;
      }
      else {
        wavfile = new File(entry.toString());
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(wavfile));
        bos.write(data, 470, data.length - 470);
        bos.close();
        fileCreated = true;
      }
    }
    else
      throw new Exception("Unsupported WAV file: " + signature);
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bplay)
      new Thread(this).start();
    else if (event.getSource() == bstop) {
      bstop.setEnabled(false);
      a2w.stopPlay();
      bplay.setEnabled(true);
    }
    else if (event.getSource() == bfind)
      new WavReferenceSearcher(entry, panel.getTopLevelAncestor());
    else if (event.getSource() == bconvert) {
      try {
        wavfile = SoundUtilities.convert(entry.getResourceData(), 28, '_' + entry.toString(), channels == 1);
        bplay.setEnabled(true);
        bplay.setToolTipText(null);
        bconvert.setEnabled(false);
      } catch (Exception e) {
        JOptionPane.showMessageDialog(panel, "Error during conversion", "Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
      }
    }
    else if (event.getSource() == bexport)
      ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
    else if (event.getSource() == bexportmp3)
      ResourceFactory.getInstance().exportResource(entry, mp3data,
                                                   entry.toString().substring(0,
                                                                              entry.toString().lastIndexOf(
                                                                                      (int)'.')) + ".mp3",
                                                   panel.getTopLevelAncestor());
    else if (event.getSource() == bexportConvert) {
      JFileChooser chooser = new JFileChooser(ResourceFactory.getRootDir());
      chooser.setDialogTitle("Export & Convert");
      chooser.setSelectedFile(new File(entry.toString()));
      if (chooser.showDialog(panel, "Export") == JFileChooser.APPROVE_OPTION) {
        String filename = chooser.getSelectedFile().toString();
        try {
          SoundUtilities.convert(entry.getResourceData(), 28, filename, channels == 1);
        } catch (Exception e) {
          JOptionPane.showMessageDialog(panel, "Error during conversion", "Error", JOptionPane.ERROR_MESSAGE);
          e.printStackTrace();
        }
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Closeable ---------------------

  @Override
  public void close()
  {
    a2w.stopPlay();
    if (wavfile == null || !fileCreated)
      return;
    if (!wavfile.delete())
      wavfile.deleteOnExit();
  }

// --------------------- End Interface Closeable ---------------------


// --------------------- Begin Interface Resource ---------------------

  @Override
  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

// --------------------- End Interface Resource ---------------------


// --------------------- Begin Interface Runnable ---------------------

  @Override
  public void run()
  {
    bplay.setEnabled(false);
    bstop.setEnabled(true);
    try {
      a2w.play(wavfile);
    } catch (Exception e) {
      JOptionPane.showMessageDialog(panel, "Error during playback", "Error", JOptionPane.ERROR_MESSAGE);
      e.printStackTrace();
    }
    bstop.setEnabled(false);
    bplay.setEnabled(true);
  }

// --------------------- End Interface Runnable ---------------------


// --------------------- Begin Interface Viewable ---------------------

  @Override
  public JComponent makeViewer(ViewableContainer container)
  {
    JPanel buttonpanel = new JPanel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    buttonpanel.setLayout(gbl);
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.fill = GridBagConstraints.HORIZONTAL;

    bfind = new JButton("Find references...", Icons.getIcon("Find16.gif"));
    bexport = new JButton("Export...", Icons.getIcon("Export16.gif"));
    bexport.addActionListener(this);
    bfind.addActionListener(this);

    if (mp3data == null) {
      bplay = new JButton(Icons.getIcon("Play16.gif"));
      bstop = new JButton(Icons.getIcon("Stop16.gif"));
      bplay.addActionListener(this);
      bstop.addActionListener(this);
      bplay.setEnabled(wavfile != null);
      bstop.setEnabled(false);
//      if (!BrowserMenuBar.getInstance().autoConvertWAV() && wavfile == null) {
//        bconvert = new JButton("Convert", Icons.getIcon("Refresh16.gif"));
//        bconvert.addActionListener(this);
//        bplay.setEnabled(false);
//        bplay.setToolTipText("You must convert file before playback is possible");
//      }

      gbl.setConstraints(bplay, gbc);
      buttonpanel.add(bplay);

      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbl.setConstraints(bstop, gbc);
      buttonpanel.add(bstop);

      if (bconvert != null) {
        gbl.setConstraints(bconvert, gbc);
        buttonpanel.add(bconvert);
      }
    }
    else {
      bexportmp3 = new JButton("Export as MP3...", Icons.getIcon("Export16.gif"));
      bexportmp3.addActionListener(this);

      gbl.setConstraints(bexportmp3, gbc);
      buttonpanel.add(bexportmp3);
    }
    JPanel centerpanel = new JPanel(new BorderLayout());
    centerpanel.add(buttonpanel, BorderLayout.CENTER);

    JPanel lowerpanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    lowerpanel.add(bfind);
    lowerpanel.add(bexport);
    if (signature.equalsIgnoreCase("WAVC")) {
      bexportConvert = new JButton("Export & Convert...", Icons.getIcon("Export16.gif"));
      bexportConvert.addActionListener(this);
      if (SoundUtilities.converterExists())
        ;
      else {
        bexportConvert.setSelected(false);
        bexportConvert.setToolTipText("Sound converter not found");
      }
      lowerpanel.add(bexportConvert);
    }

    panel = new JPanel(new BorderLayout());
    panel.add(centerpanel, BorderLayout.CENTER);
    panel.add(lowerpanel, BorderLayout.SOUTH);
    centerpanel.setBorder(BorderFactory.createLoweredBevelBorder());

    return panel;
  }

// --------------------- End Interface Viewable ---------------------
}

