// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.util;

import infinity.NearInfinity;
import infinity.gui.Center;
import infinity.gui.ChildFrame;
import infinity.icon.Icons;
import infinity.resource.ResourceFactory;
import infinity.resource.Writeable;
import infinity.resource.bcs.Decompiler;
import infinity.resource.cre.CreResource;
import infinity.resource.graphics.Compressor;
import infinity.resource.graphics.MveResource;
import infinity.resource.key.ResourceEntry;
import infinity.resource.sound.SoundUtilities;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public final class MassExporter extends ChildFrame implements ActionListener, ListSelectionListener,
                                                              Runnable
{
  private static final String TYPES[] = {"2DA", "ARE", "BAM", "BCS", "BIO", "BMP", "BS",
                                         "CHU", "CHR", "CRE", "DLG", "EFF", "GAM", "IDS",
                                         "INI", "ITM", "MOS", "MVE", "PLT", "PRO", "SRC",
                                         "SPL", "STO", "TIS", "VVC", "WAV", "WED", "WFX", "WMP"};
  private final JButton bExport = new JButton("Export", Icons.getIcon("Export16.gif"));
  private final JButton bCancel = new JButton("Cancel", Icons.getIcon("Delete16.gif"));
  private final JButton bDirectory = new JButton(Icons.getIcon("Open16.gif"));
  private final JCheckBox cbDecompile = new JCheckBox("Decompile scripts", true);
  private final JCheckBox cbDecrypt = new JCheckBox("Decrypt text files", true);
  private final JCheckBox cbConvertWAV = new JCheckBox("Convert sounds", true);
  private final JCheckBox cbConvertCRE = new JCheckBox("Convert CHR=>CRE", false);
  private final JCheckBox cbDecompress = new JCheckBox("Decompress BAM/MOS", false);
  private final JCheckBox cbExecutableMVE = new JCheckBox("Make movies executable", false);
  private final JCheckBox cbOverwrite = new JCheckBox("Overwrite existing files", false);
  private final JFileChooser fc = new JFileChooser(ResourceFactory.getRootDir());
  private final JList listTypes = new JList(TYPES);
  private final JTextField tfDirectory = new JTextField(20);
  private final byte[] buffer = new byte[65536];
  private File outputDir;
  private Object selectedTypes[];

  public MassExporter()
  {
    super("Mass Exporter", true);

    bExport.addActionListener(this);
    bCancel.addActionListener(this);
    bDirectory.addActionListener(this);
    bExport.setEnabled(false);
    tfDirectory.setEditable(false);
    listTypes.addListSelectionListener(this);
    fc.setDialogTitle("Mass export: Select directory");
    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    getRootPane().setDefaultButton(bExport);
    bExport.setMnemonic('e');
    bCancel.setMnemonic('d');
    if (SoundUtilities.converterExists())
      ;
    else {
      cbConvertWAV.setSelected(false);
      cbConvertWAV.setEnabled(false);
      cbConvertWAV.setToolTipText("Sound converter not found");
    }

    JPanel leftPanel = new JPanel(new BorderLayout());
    leftPanel.add(new JLabel("File types to export:"), BorderLayout.NORTH);
    leftPanel.add(new JScrollPane(listTypes), BorderLayout.CENTER);

    JPanel topRightPanel = new JPanel(new BorderLayout());
    topRightPanel.add(new JLabel("Output directory:"), BorderLayout.NORTH);
    topRightPanel.add(tfDirectory, BorderLayout.CENTER);
    topRightPanel.add(bDirectory, BorderLayout.EAST);

    JPanel bottomRightPanel = new JPanel(new GridLayout(0, 1));
    bottomRightPanel.add(new JLabel("Options:"));
    bottomRightPanel.add(cbConvertWAV);
    bottomRightPanel.add(cbConvertCRE);
    bottomRightPanel.add(cbDecompile);
    bottomRightPanel.add(cbDecrypt);
    bottomRightPanel.add(cbDecompress);
    bottomRightPanel.add(cbExecutableMVE);
    bottomRightPanel.add(cbOverwrite);

    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    bottomPanel.add(bExport);
    bottomPanel.add(bCancel);

    JPanel pane = (JPanel)getContentPane();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    pane.setLayout(gbl);

    gbc.weightx = 0.0;
    gbc.weighty = 1.0;
    gbc.gridheight = 2;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets = new Insets(6, 6, 6, 6);
    gbl.setConstraints(leftPanel, gbc);
    pane.add(leftPanel);

    gbc.gridheight = 1;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.weighty = 0.0;
    gbc.weightx = 1.0;
    gbl.setConstraints(topRightPanel, gbc);
    pane.add(topRightPanel);

    gbc.weighty = 1.0;
    gbl.setConstraints(bottomRightPanel, gbc);
    pane.add(bottomRightPanel);

    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.weighty = 0.0;
    gbc.weightx = 1.0;
    gbl.setConstraints(bottomPanel, gbc);
    pane.add(bottomPanel);

    setSize(400, 300);
    Center.center(this, NearInfinity.getInstance().getBounds());
    setVisible(true);
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bExport) {
      selectedTypes = listTypes.getSelectedValues();
      outputDir = new File(tfDirectory.getText());
      outputDir.mkdirs();
      setVisible(false);
      new Thread(this).start();
    }
    else if (event.getSource() == bCancel)
      setVisible(false);
    else if (event.getSource() == bDirectory) {
      if (fc.showDialog(this, "Select") == JFileChooser.APPROVE_OPTION)
        tfDirectory.setText(fc.getSelectedFile().toString());
      bExport.setEnabled(listTypes.getSelectedIndices().length > 0 && tfDirectory.getText().length() > 0);
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  public void valueChanged(ListSelectionEvent event)
  {
    bExport.setEnabled(listTypes.getSelectedIndices().length > 0 && tfDirectory.getText().length() > 0);
  }

// --------------------- End Interface ListSelectionListener ---------------------


// --------------------- Begin Interface Runnable ---------------------

  public void run()
  {
    java.util.List<ResourceEntry> selectedFiles = new ArrayList<ResourceEntry>(1000);
    for (final Object newVar : selectedTypes) 
      selectedFiles.addAll(ResourceFactory.getInstance().getResources((String)newVar));
    ProgressMonitor progress = new ProgressMonitor(NearInfinity.getInstance(), "Exporting...", null,
                                                   0, selectedFiles.size());
    progress.setMillisToDecideToPopup(100);
    progress.setMillisToPopup(100);
    for (int i = 0; i < selectedFiles.size(); i++) {
      ResourceEntry resourceEntry = selectedFiles.get(i);
      export(resourceEntry);
      progress.setProgress(i);
      if (progress.isCanceled()) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Mass export aborted");
        return;
      }
    }
    progress.close();
    JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Mass export completed");
  }

// --------------------- End Interface Runnable ---------------------

  private void export(ResourceEntry entry)
  {
    try {
      File output = new File(outputDir, entry.toString());
      if (output.exists() && !cbOverwrite.isSelected())
        return;
      if ((entry.getExtension().equalsIgnoreCase("IDS") ||
           entry.getExtension().equalsIgnoreCase("2DA") ||
           entry.getExtension().equalsIgnoreCase("BIO") ||
           entry.getExtension().equalsIgnoreCase("RES") ||
           entry.getExtension().equalsIgnoreCase("INI") ||
           entry.getExtension().equalsIgnoreCase("SET") ||
           entry.getExtension().equalsIgnoreCase("WOK") ||
           entry.getExtension().equalsIgnoreCase("TXI") ||
           entry.getExtension().equalsIgnoreCase("DWK") ||
           entry.getExtension().equalsIgnoreCase("PWK") ||
           entry.getExtension().equalsIgnoreCase("NSS") ||
           entry.getExtension().equalsIgnoreCase("TXT") ||
           (entry.getExtension().equalsIgnoreCase("SRC") &&
           ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2)) &&
          cbDecrypt.isSelected()) {
        byte data[] = entry.getResourceData();
        if (data[0] == -1)
          data = Decryptor.decrypt(data, 2, data.length).getBytes();
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(output));
        Filewriter.writeBytes(bos, data);
        bos.close();
      }
      else if ((entry.getExtension().equalsIgnoreCase("BCS") ||
                entry.getExtension().equalsIgnoreCase("BS")) && cbDecompile.isSelected()) {
        output = new File(outputDir, entry.toString().substring(0, entry.toString().lastIndexOf(".")) +
                                     ".BAF");
        if (output.exists() && !cbOverwrite.isSelected())
          return;
        byte data[] = entry.getResourceData();
        if (data.length > 0) {
          if (data[0] == -1)
            data = Decryptor.decrypt(data, 2, data.length).getBytes();
          String script = Decompiler.decompile(new String(data), false);
          PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(output)));
          pw.println(script);
          pw.close();
        }
      }
      else if ((entry.getExtension().equalsIgnoreCase("BAM") ||
                entry.getExtension().equalsIgnoreCase("MOS")) && cbDecompress.isSelected()) {
        byte data[] = entry.getResourceData();
        String signature = new String(data, 0, 4);
        if (signature.equalsIgnoreCase("BAMC") || signature.equalsIgnoreCase("MOSC"))
          data = Compressor.decompress(data);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(output));
        Filewriter.writeBytes(bos, data);
        bos.close();
      }
      else if (entry.getExtension().equalsIgnoreCase("CHR") && cbConvertCRE.isSelected()) {
        output = new File(outputDir, entry.toString().substring(0, entry.toString().lastIndexOf(".")) +
                                     ".CRE");
        if (output.exists() && !cbOverwrite.isSelected())
          return;
        CreResource crefile = new CreResource(entry);
        java.util.List flatList = crefile.getFlatList();
        while (!flatList.get(0).toString().equals("CRE "))
          flatList.remove(0);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(output));
        for (int i = 0; i < flatList.size(); i++)
          ((Writeable)flatList.get(i)).write(bos);
        bos.close();
      }
      else if (entry.getExtension().equalsIgnoreCase("WAV") && cbConvertWAV.isSelected()) {
        byte data[] = entry.getResourceData();
        String signature = new String(data, 0, 4);
        if (signature.equalsIgnoreCase("WAVC")) {
          int channels = (int)Byteconvert.convertShort(data, 20);
          File acmfile = new File(outputDir,
                                  entry.toString().substring(0, entry.toString().lastIndexOf((int)'.')) +
                                  ".ACM");
          BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(acmfile));
          bos.write(data, 28, data.length - 28);
          bos.close();
          if (!SoundUtilities.converterExists())
            return;
          else
            SoundUtilities.convert(acmfile, channels == 1);
          acmfile.delete();
        }
        else {
          BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(output));
          Filewriter.writeBytes(bos, data);
          bos.close();
        }
      }
      else if (entry.getExtension().equalsIgnoreCase("MVE") && cbExecutableMVE.isSelected()) {
        output = new File(outputDir, entry.toString().substring(0, entry.toString().lastIndexOf(".")) +
                                     ".exe");
        if (output.exists() && !cbOverwrite.isSelected())
          return;
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(output));
        BufferedInputStream stub = new BufferedInputStream(MveResource.class.getResourceAsStream("mve.stub"));
        Filewriter.writeBytes(bos, Filereader.readBytes(stub, 77312));
        stub.close();
        InputStream is = entry.getResourceDataAsStream();
        int size = entry.getResourceInfo()[0];
        int bytesRead = is.read(buffer);
        while (size > 0) {
          bos.write(buffer, 0, bytesRead);
          size -= bytesRead;
          bytesRead = is.read(buffer, 0, Math.min(size, buffer.length));
        }
        bos.close();
        is.close();
      }
      else {
        InputStream is = entry.getResourceDataAsStream();
        int size = entry.getResourceInfo()[0];
        if (entry.getExtension().equalsIgnoreCase("TIS"))
          size *= entry.getResourceInfo()[1];
        if (size >= 0) {
          BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(output));
          while (size > 0) {
            int bytesRead = is.read(buffer, 0, Math.min(size, buffer.length));
            bos.write(buffer, 0, bytesRead);
            size -= bytesRead;
          }
          is.close();
          bos.close();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

