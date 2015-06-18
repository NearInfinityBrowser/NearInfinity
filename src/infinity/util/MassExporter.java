// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.util;

import infinity.NearInfinity;
import infinity.gui.Center;
import infinity.gui.ChildFrame;
import infinity.gui.ViewerUtil;
import infinity.icon.Icons;
import infinity.resource.Profile;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.Writeable;
import infinity.resource.bcs.Decompiler;
import infinity.resource.cre.CreResource;
import infinity.resource.graphics.BamDecoder;
import infinity.resource.graphics.BamResource;
import infinity.resource.graphics.ColorConvert;
import infinity.resource.graphics.Compressor;
import infinity.resource.graphics.MosDecoder;
import infinity.resource.graphics.MosV1Decoder;
import infinity.resource.graphics.PvrDecoder;
import infinity.resource.graphics.TisDecoder;
import infinity.resource.graphics.TisResource;
import infinity.resource.key.BIFFArchive;
import infinity.resource.key.ResourceEntry;
import infinity.resource.sound.AudioFactory;
import infinity.resource.video.MveResource;
import infinity.util.io.FileNI;
import infinity.util.io.FileOutputStreamNI;
import infinity.util.io.FileWriterNI;
import infinity.util.io.PrintWriterNI;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public final class MassExporter extends ChildFrame implements ActionListener, ListSelectionListener,
                                                              Runnable
{
  private static final String TYPES[] = {"2DA", "ARE", "BAM", "BCS", "BS", "BIO", "BMP",
                                         "CHU", "CHR", "CRE", "DLG", "EFF", "FNT", "GAM",
                                         "GLSL", "GUI", "IDS", "INI", "ITM", "MOS", "MVE",
                                         "PLT", "PRO", "PVRZ", "SPL", "SQL", "SRC", "STO",
                                         "TIS", "TOH", "TOT", "VEF", "VVC", "WAV", "WBM",
                                         "WED", "WFX", "WMP"};
  private final JButton bExport = new JButton("Export", Icons.getIcon("Export16.gif"));
  private final JButton bCancel = new JButton("Cancel", Icons.getIcon("Delete16.gif"));
  private final JButton bDirectory = new JButton(Icons.getIcon("Open16.gif"));
  private final JCheckBox cbDecompile = new JCheckBox("Decompile scripts", true);
  private final JCheckBox cbDecrypt = new JCheckBox("Decrypt text files", true);
  private final JCheckBox cbConvertWAV = new JCheckBox("Convert sounds", true);
  private final JCheckBox cbConvertCRE = new JCheckBox("Convert CHR=>CRE", false);
  private final JCheckBox cbDecompress = new JCheckBox("Decompress BAM/MOS", false);
  private final JCheckBox cbConvertToPNG = new JCheckBox("Export MOS/PVRZ/TIS as PNG", false);
  private final JCheckBox cbConvertTisVersion = new JCheckBox("Convert TIS to ", false);
  private final JComboBox cbConvertTisList = new JComboBox(new String[]{"Palette-based", "PVRZ-based"});
  private final JCheckBox cbExtractFramesBAM = new JCheckBox("Export BAM frames as ", false);
  private final JCheckBox cbExportMVEasAVI = new JCheckBox("Export MVE as AVI", false);
  private final JCheckBox cbOverwrite = new JCheckBox("Overwrite existing files", false);
  private final JFileChooser fc = new JFileChooser(Profile.getGameRoot());
  private final JComboBox cbExtractFramesBAMFormat = new JComboBox(new String[]{"PNG", "BMP"});
  private final JList listTypes = new JList(TYPES);
  private final JTextField tfDirectory = new JTextField(20);
  private final byte[] buffer = new byte[65536];
  private File outputDir;
  private Object[] selectedTypes;

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
    cbConvertToPNG.setToolTipText("Caution: Selecting both MOS and TIS may overwrite or skip some files!");
    cbExtractFramesBAM.setToolTipText("Note: Frames of each BAM resource are exported into separate subfolders.");
    cbConvertTisVersion.setToolTipText("Caution: Conversion may take a long time. Files may be renamed to conform to naming scheme for PVRZ-based TIS files.");

    JPanel leftPanel = new JPanel(new BorderLayout());
    leftPanel.add(new JLabel("File types to export:"), BorderLayout.NORTH);
    leftPanel.add(new JScrollPane(listTypes), BorderLayout.CENTER);

    JPanel topRightPanel = new JPanel(new BorderLayout());
    topRightPanel.add(new JLabel("Output directory:"), BorderLayout.NORTH);
    topRightPanel.add(tfDirectory, BorderLayout.CENTER);
    topRightPanel.add(bDirectory, BorderLayout.EAST);

    GridBagConstraints gbc = new GridBagConstraints();
    JPanel bottomRightPanel = new JPanel(new GridBagLayout());

    JPanel pBamFrames = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    pBamFrames.add(cbExtractFramesBAM);
    pBamFrames.add(cbExtractFramesBAMFormat);

    JPanel pTisConvert = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    pTisConvert.add(cbConvertTisVersion);
    pTisConvert.add(cbConvertTisList);

    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    bottomRightPanel.add(new JLabel("Options:"), gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(2, 0, 0, 0), 0, 0);
    bottomRightPanel.add(cbConvertWAV, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(2, 0, 0, 0), 0, 0);
    bottomRightPanel.add(cbConvertCRE, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 3, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(2, 0, 0, 0), 0, 0);
    bottomRightPanel.add(cbDecompile, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 4, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(2, 0, 0, 0), 0, 0);
    bottomRightPanel.add(cbDecrypt, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 5, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(2, 0, 0, 0), 0, 0);
    bottomRightPanel.add(cbDecompress, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 6, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(2, 0, 0, 0), 0, 0);
    bottomRightPanel.add(cbConvertToPNG, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 7, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(2, 0, 0, 0), 0, 0);
    bottomRightPanel.add(pTisConvert, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 8, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(2, 0, 0, 0), 0, 0);
    bottomRightPanel.add(pBamFrames, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 9, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(2, 0, 0, 0), 0, 0);
    bottomRightPanel.add(cbExportMVEasAVI, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 10, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(2, 0, 0, 0), 0, 0);
    bottomRightPanel.add(cbOverwrite, gbc);

    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    bottomPanel.add(bExport);
    bottomPanel.add(bCancel);

    JPanel pane = (JPanel)getContentPane();
    GridBagLayout gbl = new GridBagLayout();
//    GridBagConstraints gbc = new GridBagConstraints();
    gbc = new GridBagConstraints();
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

    pack();
    setMinimumSize(getPreferredSize());
    Center.center(this, NearInfinity.getInstance().getBounds());
    setVisible(true);
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bExport) {
      selectedTypes = listTypes.getSelectedValues();
      outputDir = new FileNI(tfDirectory.getText());
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

  @Override
  public void valueChanged(ListSelectionEvent event)
  {
    bExport.setEnabled(listTypes.getSelectedIndices().length > 0 && tfDirectory.getText().length() > 0);
  }

// --------------------- End Interface ListSelectionListener ---------------------


// --------------------- Begin Interface Runnable ---------------------

  @Override
  public void run()
  {
    final String fmtProgress = "Processing resource %1$d/%2$d";
    java.util.List<ResourceEntry> selectedFiles = new ArrayList<ResourceEntry>(1000);
    for (final Object newVar : selectedTypes) {
      selectedFiles.addAll(ResourceFactory.getResources((String)newVar));
    }
    final int count = selectedFiles.size();
    ProgressMonitor progress = new ProgressMonitor(NearInfinity.getInstance(), "Exporting...",
                                                   String.format(fmtProgress, 0, count),
                                                   0, selectedFiles.size());
    progress.setMillisToDecideToPopup(0);
    progress.setMillisToPopup(0);
    progress.setProgress(0);
    for (int i = 0; i < count; i++) {
      ResourceEntry resourceEntry = selectedFiles.get(i);
      export(resourceEntry);
      progress.setProgress(i);
      if (count < 50 || i % 10 == 0) {
        progress.setNote(String.format(fmtProgress, i, count));
      }
      if (progress.isCanceled()) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Mass export aborted");
        return;
      }
    }
    progress.close();
    JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Mass export completed");
  }

// --------------------- End Interface Runnable ---------------------

  private void exportText(ResourceEntry entry, File output) throws Exception
  {
    byte data[] = entry.getResourceData();
    if (data[0] == -1) {
      data = Decryptor.decrypt(data, 2, data.length).getBytes();
    }
    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStreamNI(output));
    FileWriterNI.writeBytes(bos, data);
    bos.close();
  }

  private void exportDecompiledScript(ResourceEntry entry, File output) throws Exception
  {
    output = new FileNI(outputDir, Misc.replaceFileExtension(entry.toString(), "BAF"));
    if (output.exists() && !cbOverwrite.isSelected()) {
      return;
    }
    byte data[] = entry.getResourceData();
    if (data.length > 0) {
      if (data[0] == -1) {
        data = Decryptor.decrypt(data, 2, data.length).getBytes();
      }
      String script = Decompiler.decompile(new String(data), false);
      PrintWriter pw = new PrintWriterNI(new BufferedWriter(new FileWriterNI(output)));
      pw.println(script);
      pw.close();
    }
  }

  private void decompressBamMos(ResourceEntry entry, File output) throws Exception
  {
    byte data[] = entry.getResourceData();
    String signature = new String(data, 0, 4);
    if (signature.equalsIgnoreCase("BAMC") || signature.equalsIgnoreCase("MOSC")) {
      data = Compressor.decompress(data);
    }
    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStreamNI(output));
    FileWriterNI.writeBytes(bos, data);
    bos.close();
  }

  private void decompressWav(ResourceEntry entry, File output) throws Exception
  {
    byte[] buffer = AudioFactory.convertAudio(entry);
    if (buffer != null) {
      BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStreamNI(output));
      FileWriterNI.writeBytes(bos, buffer);
      bos.close();
      buffer = null;
    }
  }

  private void mosToPng(ResourceEntry entry, File output) throws Exception
  {
    if (entry != null && entry.getExtension().equalsIgnoreCase("MOS") && output != null) {
      output = new FileNI(outputDir, Misc.replaceFileExtension(entry.toString(), "PNG"));
      if (output.exists() && !cbOverwrite.isSelected()) {
        return;
      }

      MosDecoder decoder = MosDecoder.loadMos(entry);
      if (decoder != null) {
        if (decoder instanceof MosV1Decoder) {
          ((MosV1Decoder)decoder).setTransparencyEnabled(true);
        }
        RenderedImage image = ColorConvert.toBufferedImage(decoder.getImage(), true);
        try {
          ImageIO.write(image, "png", output);
        } finally {
          image = null;
        }
      } else {
        throw new Exception(String.format("Error loading resource: %1$s", entry.getResourceName()));
      }
    }
  }

  private void pvrzToPng(ResourceEntry entry, File output) throws Exception
  {
    if (entry != null && entry.getExtension().equalsIgnoreCase("PVRZ") && output != null) {
      output = new FileNI(outputDir, Misc.replaceFileExtension(entry.toString(), "PNG"));
      if (output.exists() && !cbOverwrite.isSelected()) {
        return;
      }

      PvrDecoder decoder = PvrDecoder.loadPvr(entry);
      if (decoder != null) {
        RenderedImage image = decoder.decode();
        try {
          ImageIO.write(image, "png", output);
        } finally {
          image = null;
        }
      } else {
        throw new Exception(String.format("Error loading resource: %1$s", entry.getResourceName()));
      }
    }
  }

  private void tisToPng(ResourceEntry entry, File output) throws Exception
  {
    if (entry != null && entry.getExtension().equalsIgnoreCase("TIS") && output != null) {
      output = new FileNI(outputDir, Misc.replaceFileExtension(entry.toString(), "PNG"));
      if (output.exists() && !cbOverwrite.isSelected()) {
        return;
      }

      TisDecoder decoder = TisDecoder.loadTis(entry);
      if (decoder != null) {
        int tileCount = decoder.getTileCount();
        int columns = TisResource.calcTileWidth(entry, 1);
        int rows = tileCount / columns;
        if ((tileCount % columns) != 0) {
          rows++;
        }

        BufferedImage tile = ColorConvert.createCompatibleImage(64, 64, Transparency.BITMASK);
        BufferedImage image = ColorConvert.createCompatibleImage(64*columns, 64*rows, Transparency.BITMASK);
        try {
          Graphics2D g = (Graphics2D)image.getGraphics();
          try {
            for (int i = 0; i < tileCount; i++) {
              int x = 64*(i % columns);
              int y = 64*(i / columns);
              decoder.getTile(i, tile);
              g.drawImage(tile, x, y, null);
            }
          } finally {
            g.dispose();
            g = null;
          }
          ImageIO.write(image, "png", output);
        } finally {
          tile = null;
          image = null;
        }
      } else {
        throw new Exception(String.format("Error loading resource: %1$s", entry.getResourceName()));
      }
    }
  }

  private void extractBamFrames(ResourceEntry entry, File output) throws Exception
  {
    String format = (cbExtractFramesBAMFormat.getSelectedIndex() == 0) ? "png" : "bmp";
    String filePath = output.getParent();
    String fileName = output.getName();
    int extIdx = fileName.lastIndexOf('.');
    String fileBase = (extIdx >= 0) ? fileName.substring(0, extIdx) : fileName;
    String fileExt = "." + format;

    // creating subfolder for frames
    File dir = new FileNI(filePath, fileBase);
    if (!dir.exists()) {
      if (!dir.mkdir()) {
        String msg = String.format("Error creating folder \"%1$s\". Skipping file \"%2$s\".",
                                   fileBase, fileName);
        System.err.println(msg);
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
        return;
      }
    } else if (!dir.isDirectory()) {
      String msg = String.format("Folder \"%1$s\" can not be created. Skipping file \"%2$s\".",
                                 fileBase, fileName);
      System.err.println(msg);
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }
    filePath = dir.getPath();

    BamDecoder decoder = BamDecoder.loadBam(entry);
    BamResource.exportFrames(decoder, filePath, fileBase, fileExt, format, true);
  }

  private void chrToCre(ResourceEntry entry, File output) throws Exception
  {
    output = new FileNI(outputDir, Misc.replaceFileExtension(entry.toString(), "CRE"));
    if (output.exists() && !cbOverwrite.isSelected()) {
      return;
    }
    CreResource crefile = new CreResource(entry);
    java.util.List<StructEntry> flatList = crefile.getFlatList();
    while (!flatList.get(0).toString().equals("CRE ")) {
      flatList.remove(0);
    }
    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStreamNI(output));
    for (int i = 0; i < flatList.size(); i++) {
      ((Writeable)flatList.get(i)).write(bos);
    }
    bos.close();
  }

  private void exportResource(ResourceEntry entry, File output) throws Exception
  {
    if (entry != null && output != null) {
      InputStream is = entry.getResourceDataAsStream();
      int[] info = entry.getResourceInfo();
      int size = info[0];
      byte[] tileheader = null;
      boolean isTis = false, isTisV2 = false;
      if (entry.getExtension().equalsIgnoreCase("TIS")) {
        isTis = true;
        size *= info[1];
        if (!entry.hasOverride()) {
          tileheader = BIFFArchive.getTisHeader(info[0], info[1]);
        } else {
          tileheader = new byte[24];
          is.read(tileheader);
        }
        isTisV2 = (DynamicArray.getInt(tileheader, 12) == 0x0c);
      }

      if (isTis && cbConvertTisVersion.isSelected() &&
          isTisV2 == false && cbConvertTisList.getSelectedIndex() == 1) {
        TisResource tis = new TisResource(entry);
        tis.convertToPvrzTis(TisResource.makeTisFileNameValid(output), false);
      } else if (isTis && cbConvertTisVersion.isSelected() &&
                 isTisV2 == true && cbConvertTisList.getSelectedIndex() == 0) {
        TisResource tis = new TisResource(entry);
        tis.convertToPaletteTis(output, false);
      } else if (size >= 0) {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStreamNI(output));
        if (tileheader != null)
          bos.write(tileheader);
        while (size > 0) {
          int bytesRead = is.read(buffer, 0, Math.min(size, buffer.length));
          bos.write(buffer, 0, bytesRead);
          size -= bytesRead;
        }
        is.close();
        bos.close();
      }
    }
  }


  private void export(ResourceEntry entry)
  {
    try {
      File output = new FileNI(outputDir, entry.toString());
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
           (Profile.isEnhancedEdition() &&
               (entry.getExtension().equalsIgnoreCase("GLSL") ||
                entry.getExtension().equalsIgnoreCase("GUI") ||
                entry.getExtension().equalsIgnoreCase("SQL"))) ||
           (entry.getExtension().equalsIgnoreCase("SRC") &&
               Profile.getEngine() == Profile.Engine.IWD2)) &&
          cbDecrypt.isSelected()) {
        exportText(entry, output);
      }
      else if ((entry.getExtension().equalsIgnoreCase("BCS") ||
                entry.getExtension().equalsIgnoreCase("BS")) && cbDecompile.isSelected()) {
        exportDecompiledScript(entry, output);
      }
      else if (entry.getExtension().equalsIgnoreCase("MOS") && cbConvertToPNG.isSelected()) {
        mosToPng(entry, output);
      }
      else if (entry.getExtension().equalsIgnoreCase("PVRZ") && cbConvertToPNG.isSelected()) {
        pvrzToPng(entry, output);
      }
      else if (entry.getExtension().equalsIgnoreCase("TIS") && cbConvertToPNG.isSelected()) {
        tisToPng(entry, output);
      }
      else if (entry.getExtension().equalsIgnoreCase("BAM") && cbExtractFramesBAM.isSelected()) {
        extractBamFrames(entry, output);
      }
      else if ((entry.getExtension().equalsIgnoreCase("BAM") ||
                entry.getExtension().equalsIgnoreCase("MOS")) && cbDecompress.isSelected()) {
        decompressBamMos(entry, output);
      }
      else if (entry.getExtension().equalsIgnoreCase("CHR") && cbConvertCRE.isSelected()) {
        chrToCre(entry, output);
      }
      else if (entry.getExtension().equalsIgnoreCase("WAV") && cbConvertWAV.isSelected()) {
        decompressWav(entry, output);
      }
      else if (entry.getExtension().equalsIgnoreCase("MVE") && cbExportMVEasAVI.isSelected()) {
        output = new FileNI(outputDir, Misc.replaceFileExtension(entry.toString(), "avi"));
        if (output.exists() && !cbOverwrite.isSelected()) {
          return;
        }
        MveResource.convertAvi(entry, output, null, true);
      }
      else {
        exportResource(entry, output);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}

