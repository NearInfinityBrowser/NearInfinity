// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import infinity.resource.ResourceFactory;
import infinity.resource.graphics.ColorConvert;
import infinity.resource.graphics.Compressor;
import infinity.resource.graphics.DxtEncoder;
import infinity.util.DynamicArray;
import infinity.util.GridManager;
import infinity.util.IntegerHashMap;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ConvertToMos extends ChildFrame
    implements ActionListener, PropertyChangeListener, ChangeListener, FocusListener
{
  private JTabbedPane tabPane;
  private JTextField tfInputV1, tfOutputV1, tfInputV2, tfOutputV2;
  private JButton bInputV1, bOutputV1, bInputV2, bOutputV2, bCompressionHelp;
  private JButton bConvert, bCancel;
  private JSpinner sPvrzIndex;
  private JLabel lPvrzInfo;
  private JComboBox cbCompression;
  private JCheckBox cbCompress, cbCloseOnExit;
  private SwingWorker<List<String>, Void> workerConvert;
  private WindowBlocker blocker;

  /**
   * Converts an image into a MOS V1 resource.
   * @param parent This parameter is needed for the progress monitor only.
   * @param img The source image to convert into a MOS resource.
   * @param mosFileName The name of the resulting MOS file.
   * @param compressed If <code>true</code>, converts into a compressed BAMC file.
   * @param result Returns more specific information about the conversion process. Data placed in the
   *               first item indicates success, data in the second item indicates failure.
   * @param showProgress Specify whether to show a progress monitor (needs a valid 'parent' parameter).
   * @return <code>true</code> if the conversion finished successfully, <code>false</code> otherwise.
   */
  public static boolean convertV1(Component parent, BufferedImage img, String mosFileName,
                                  boolean compressed, List<String> result, boolean showProgress)
  {
    // checking parameters
    if (result == null) {
      return false;
    }
    if (img == null) {
      result.add(null);
      result.add("No source image specified.");
      return false;
    }
    if (mosFileName == null || mosFileName.isEmpty()) {
      result.add(null);
      result.add("No output filename specified.");
      return false;
    }

    // preparing MOS V1 header
    int width = img.getWidth();
    int height = img.getHeight();
    int cols = (width + 63) / 64;
    int rows = (height + 63) / 64;
    int tileCount = cols * rows;
    int palOfs = 24;
    int tableOfs = palOfs + tileCount*1024;
    int dataOfs = tableOfs + tileCount*4;
    byte[] dst = new byte[dataOfs + width*height];
    System.arraycopy("MOS V1  ".getBytes(Charset.forName("US-ASCII")), 0, dst, 0, 8);
    DynamicArray.putShort(dst, 8, (short)width);
    DynamicArray.putShort(dst, 10, (short)height);
    DynamicArray.putShort(dst, 12, (short)cols);
    DynamicArray.putShort(dst, 14, (short)rows);
    DynamicArray.putInt(dst, 16, 64);
    DynamicArray.putInt(dst, 20, palOfs);

    ProgressMonitor progress = null;
    try {
      String note = "Converting tile %1$d / %2$d";
      int progressIndex = 0, progressMax = tileCount;
      if (showProgress) {
        progress = new ProgressMonitor(parent, "Converting MOS...",
                                       String.format(note, progressIndex, progressMax), 0, progressMax);
        progress.setMillisToDecideToPopup(250);
        progress.setMillisToPopup(500);
      }

      // creating list of tiles as int[] arrays
      List<int[]> tileList = new ArrayList<int[]>(cols*rows);
      for (int y = 0; y < rows; y++) {
        for (int x = 0; x < cols; x++) {
          int tileX = x * 64;
          int tileY = y * 64;
          int tileW = (tileX + 64 < width) ? 64 : (width - tileX);
          int tileH = (tileY + 64 < height) ? 64 : (height - tileY);
          int[] rgbArray = new int[tileW*tileH];
          img.getRGB(tileX, tileY, tileW, tileH, rgbArray, 0, tileW);
          tileList.add(rgbArray);
        }
      }

      // applying color reduction to each tile
      int[] palette = new int[255];
      int[] hslPalette = new int[255];
      byte[] tilePalette = new byte[1024];
      byte[] tileData = new byte[64*64];
      int curPalOfs = palOfs, curTableOfs = tableOfs, curDataOfs = dataOfs;
      IntegerHashMap<Byte> colorCache = new IntegerHashMap<Byte>(1536);   // caching RGBColor -> index
      for (int tileIdx = 0; tileIdx < tileList.size(); tileIdx++) {
        colorCache.clear();
        if (showProgress) {
          if (progress.isCanceled()) {
            dst = null;
            result.add(null);
            result.add("Conversion has been cancelled.");
            return false;
          }
          progressIndex++;
          if ((progressIndex % 10) == 0) {
            progress.setProgress(progressIndex);
            progress.setNote(String.format(note, progressIndex, progressMax));
          }
        }

        int[] pixels = tileList.get(tileIdx);
        if (ColorConvert.medianCut(pixels, 255, palette, false)) {
          ColorConvert.toHslPalette(palette, hslPalette);
          // filling palette
          // first palette entry denotes transparency
          tilePalette[0] = tilePalette[2] = tilePalette[3] = 0; tilePalette[1] = (byte)255;
          for (int i = 1; i < 256; i++) {
            tilePalette[(i << 2) + 0] = (byte)(palette[i - 1] & 0xff);
            tilePalette[(i << 2) + 1] = (byte)((palette[i - 1] >>> 8) & 0xff);
            tilePalette[(i << 2) + 2] = (byte)((palette[i - 1] >>> 16) & 0xff);
            tilePalette[(i << 2) + 3] = 0;
            colorCache.put(palette[i - 1], (byte)(i - 1));
          }
          // filling pixel data
          for (int i = 0; i < pixels.length; i++) {
            if ((pixels[i] & 0xff000000) == 0) {
              tileData[i] = 0;
            } else {
              Byte palIndex = colorCache.get(pixels[i]);
              if (palIndex != null) {
                tileData[i] = (byte)(palIndex + 1);
              } else {
                byte color = (byte)ColorConvert.nearestColor(pixels[i], hslPalette);
                tileData[i] = (byte)(color + 1);
                colorCache.put(pixels[i], color);
              }
            }
          }
        } else {
          // error handling
          dst = null;
          result.add(null);
          result.add(String.format("Error processing tile #%1$d. Conversion cancelled.", tileIdx));
          return false;
        }

        System.arraycopy(tilePalette, 0, dst, curPalOfs, 1024);
        curPalOfs += 1024;
        DynamicArray.putInt(dst, curTableOfs, curDataOfs - dataOfs);
        curTableOfs += 4;
        System.arraycopy(tileData, 0, dst, curDataOfs, pixels.length);
        curDataOfs += pixels.length;
      }
      tileList.clear(); tileList = null;
      tileData = null; tilePalette = null; hslPalette = null; palette = null;

      // optionally compressing to MOSC V1
      if (compressed) {
        dst = Compressor.compress(dst, "MOSC", "V1  ");
      }

      // writing MOS file to disk
      BufferedOutputStream bos = null;
      try {
        try {
          bos = new BufferedOutputStream(new FileOutputStream(new File(mosFileName)));
          bos.write(dst);
          bos.close();
          bos = null;
        } catch (IOException e) {
          // error handling
          if (bos != null) {
            bos.close();
            bos = null;
          }
          e.printStackTrace();
          result.add(null);
          result.add("Error writing TIS file to disk.");
          return false;
        }
      } catch (Exception e) {
        // non-critical error
        e.printStackTrace();
      }
    } finally {
      // some cleaning up
      if (showProgress) {
        progress.close();
        progress = null;
      }
    }

    // generating conversion summary
    result.add("Conversion finished successfully.");
    return true;
  }

  /**
   * Converts an image into a MOS V2 resource.
   * @param parent This parameter is needed for the progress monitor only.
   * @param img The source image to convert into a MOS resource.
   * @param mosFileName The name of the resulting MOS file.
   * @param dxtType The desired compression type.
   * @param pvrzIndex The starting index for PVRZ files.
   * @param result Returns more specific information about the conversion process. Data placed in the
   *               first item indicates success, data in the second item indicates failure.
   * @param showProgress Specify whether to show a progress monitor (needs a valid 'parent' parameter).
   * @return <code>true</code> if the conversion finished successfully, <code>false</code> otherwise.
   */
  public static boolean convertV2(Component parent, BufferedImage img, String mosFileName,
                                  DxtEncoder.DxtType dxtType, int pvrzIndex,
                                  List<String> result, boolean showProgress)
  {
    // checking parameters
    if (result == null) {
      return false;
    }
    if (img == null) {
      result.add(null);
      result.add("No source image specified.");
      return false;
    }
    if (mosFileName == null || mosFileName.isEmpty()) {
      result.add(null);
      result.add("No output filename specified.");
      return false;
    }
    if (pvrzIndex < 0 || pvrzIndex > 99999) {
      result.add(null);
      result.add("PVRZ index is out of range [0..99999].");
      return false;
    }

    // preparing variables
    ProgressMonitor progress = null;
    int width = img.getWidth();
    int height = img.getHeight();
    List<GridManager> pageList = new ArrayList<GridManager>();
    List<MosEntry> entryList = new ArrayList<MosEntry>();

    try {
      if (showProgress) {
        // preparing progress meter
        progress = new ProgressMonitor(parent, "Converting MOS...", "Preparing data", 0, 5);
        progress.setMillisToDecideToPopup(0);
        progress.setMillisToPopup(0);
        progress.setProgress(0);
      }

      // processing tiles
      final int pageDim = 1024;
      int x = 0, y = 0, pOfs = 0;
      while (pOfs < width*height) {
        int w = Math.min(pageDim, width - x);
        int h = Math.min(pageDim, height - y);
        if (w == pageDim && h == pageDim) {
          // add page to complete pages list
          GridManager gm = new GridManager(pageDim >>> 2, pageDim >>> 2);
          gm.add(new Rectangle(0, 0, pageDim >>> 2, pageDim >>> 2));
          pageList.add(gm);
          // register page entry
          int pageIdx = pageList.size() - 1;
          MosEntry entry = new MosEntry(pvrzIndex + pageIdx, new Point(0, 0), w, h, new Point(x, y));
          entryList.add(entry);
        } else {
          // find first available page containing sufficient space for the current region
          // (forcing 4 pixels alignment for better DXT compression)
          Dimension space = new Dimension((w + 3) >>> 2, (h + 3) >>> 2);
          int pageIdx = -1;
          Rectangle rectMatch = new Rectangle(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
          for (int i = 0; i < pageList.size(); i++) {
            GridManager gm = pageList.get(i);
            Rectangle rect = gm.findNext(space, GridManager.Alignment.TopLeftHorizontal);
            if (rect != null) {
              pageIdx = i;
              rectMatch = (Rectangle)rect.clone();
              break;
            }
            if (pageIdx >= 0) {
              break;
            }
          }

          // create new page if no match found
          if (pageIdx == -1) {
            GridManager gm = new GridManager(pageDim >>> 2, pageDim >>> 2);
            pageList.add(gm);
            pageIdx = pageList.size() - 1;
            rectMatch.x = rectMatch.y = 0;
            rectMatch.width = gm.getWidth(); rectMatch.height = gm.getHeight();
          }

          // add region to the page
          GridManager gm = pageList.get(pageIdx);
          gm.add(new Rectangle(rectMatch.x, rectMatch.y, space.width, space.height));
          // register page entry
          MosEntry entry = new MosEntry(pvrzIndex + pageIdx, new Point(rectMatch.x << 2, rectMatch.y << 2),
                                        w, h, new Point(x, y));
          entryList.add(entry);
        }

        // advance scanning
        if (x + pageDim >= width) {
          x = 0;
          y += pageDim;
        } else {
          x += pageDim;
        }
        pOfs = y*width + x;
      }

      // check PVRZ index again
      if (pvrzIndex + pageList.size() > 100000) {
        result.add(null);
        result.add(String.format("One or more PVRZ indices exceed the max. possible value of 99999.\n" +
                                 "Please choose a start index smaller or equal to %1$d.",
                                 100000 - pageList.size()));
        return false;
      }

      byte[] dst = new byte[24 + entryList.size()*28];   // header + tiles
      int dstOfs = 0;

      // writing MOS header and data
      System.arraycopy("MOS V2  ".getBytes(Charset.forName("US-ASCII")), 0, dst, 0, 8);
      DynamicArray.putInt(dst, 8, width);
      DynamicArray.putInt(dst, 12, height);
      DynamicArray.putInt(dst, 16, entryList.size());
      DynamicArray.putInt(dst, 20, 24);
      dstOfs += 24;
      for (int i = 0; i < entryList.size(); i++, dstOfs += 28) {
        MosEntry entry = entryList.get(i);
        DynamicArray.putInt(dst, dstOfs, entry.page);
        DynamicArray.putInt(dst, dstOfs + 4, entry.srcLocation.x);
        DynamicArray.putInt(dst, dstOfs + 8, entry.srcLocation.y);
        DynamicArray.putInt(dst, dstOfs + 12, entry.width);
        DynamicArray.putInt(dst, dstOfs + 16, entry.height);
        DynamicArray.putInt(dst, dstOfs + 20, entry.dstLocation.x);
        DynamicArray.putInt(dst, dstOfs + 24, entry.dstLocation.y);
      }

      // writing MOS file to disk
      BufferedOutputStream bos = null;
      try {
        try {
          bos = new BufferedOutputStream(new FileOutputStream(new File(mosFileName)));
          bos.write(dst);
          bos.close();
          bos = null;
        } catch (IOException e) {
          // error handling
          if (bos != null) {
            bos.close();
            bos = null;
          }
          e.printStackTrace();
          result.add(null);
          result.add("Error writing MOS file to disk.");
          return false;
        }
      } catch (Exception e) {
        // non-critical error
        e.printStackTrace();
      }
      dst = null;

      // generating PVRZ files
      if (!createPvrzPages(new File(mosFileName).getParent(), img, dxtType, pageList, entryList,
                           result, progress)) {
        return false;
      }
    } finally {
      // some cleaning up
      img.flush();
      if (progress != null) {
        progress.close();
        progress = null;
      }
    }

    // generating conversion summary
    result.add("Conversion finished successfully.");
    return true;
  }

  // Returns a list of supported graphics file formats
  private static FileNameExtensionFilter[] getInputFilters()
  {
    FileNameExtensionFilter[] filters = new FileNameExtensionFilter[] {
        new FileNameExtensionFilter("Graphics files (*.bmp, *.png, *,jpg, *.jpeg)",
                                    "bam", "bmp", "png", "jpg", "jpeg"),
        new FileNameExtensionFilter("BMP files (*.bmp)", "bmp"),
        new FileNameExtensionFilter("PNG files (*.png)", "png"),
        new FileNameExtensionFilter("JPEG files (*.jpg, *.jpeg)", "jpg", "jpeg")
    };
    return filters;
  }

  // sets a new file extension to the specified filename string
  private static String setFileExtension(String fileName, String extension)
  {
    if (fileName != null && !fileName.isEmpty()) {
      int pos = fileName.lastIndexOf('.');
      if (pos > 0) {
        // make sure our 'dot' belongs to the file's extension
        if (pos > fileName.lastIndexOf(File.separatorChar)) {
          fileName = fileName.substring(0, pos);
        }
      }
      if (extension != null && !extension.isEmpty()) {
        fileName = fileName + "." + extension;
      }
    }
    return fileName;
  }

  // generates PVRZ textures
  private static boolean createPvrzPages(String path, BufferedImage img, DxtEncoder.DxtType dxtType,
                                         List<GridManager> gridList, List<MosEntry> entryList,
                                         List<String> result, ProgressMonitor progress)
  {
    // preparing variables
    if (path == null)
      path = "";
    if (!path.isEmpty()) {
      if (path.charAt(path.length() - 1) != File.separatorChar)
        path = path + File.separator;
    }
    int dxtCode = (dxtType == DxtEncoder.DxtType.DXT5) ? 11 : 7;
    byte[] output = new byte[DxtEncoder.calcImageSize(1024, 1024, dxtType)];
    int pageMin = Integer.MAX_VALUE;
    int pageMax = -1;
    for (final MosEntry e: entryList) {
      pageMin = Math.min(pageMin, e.page);
      pageMax = Math.max(pageMax, e.page);
    }

    String note = "Generating PVRZ file %1$s / %2$s";
    int curProgress = 1;
    if (progress != null) {
      progress.setMinimum(0);
      progress.setMaximum(pageMax - pageMin + 2);
      progress.setProgress(curProgress);
    }

    // processing each PVRZ page
    for (int i = pageMin; i <= pageMax; i++) {
      if (progress != null) {
        if (progress.isCanceled()) {
          result.add(null);
          result.add("Conversion has been cancelled.");
          return false;
        }
        progress.setProgress(curProgress);
        progress.setNote(String.format(note, curProgress, pageMax - pageMin + 1));
        curProgress++;
      }
      String pvrzName = path + String.format("MOS%1$04d.PVRZ", i);
      GridManager gm = gridList.get(i - pageMin);
      gm.shrink();

      // generating texture image
      int tw = ConvertToPvrz.nextPowerOfTwo(gm.getWidth() << 2);
      int th = ConvertToPvrz.nextPowerOfTwo(gm.getHeight() << 2);
      BufferedImage texture = ColorConvert.createCompatibleImage(tw, th, true);
      Graphics2D g = (Graphics2D)texture.getGraphics();
      g.setBackground(new Color(0, true));
      g.setColor(Color.BLACK);
      g.fillRect(0, 0, texture.getWidth(), texture.getHeight());
      for (final MosEntry entry: entryList) {
        if (entry.page == i) {
          int sx = entry.dstLocation.x, sy = entry.dstLocation.y;
          int dx = entry.srcLocation.x, dy = entry.srcLocation.y;
          int w = entry.width, h = entry.height;
          g.clearRect(dx, dy, w, h);
          g.drawImage(img, dx, dy, dx+w, dy+h, sx, sy, sx+w, sy+h, null);
        }
      }
      g.dispose();

      // compressing PVRZ
      int[] textureData = ((DataBufferInt)texture.getRaster().getDataBuffer()).getData();
      try {
        int outSize = DxtEncoder.calcImageSize(texture.getWidth(), texture.getHeight(), dxtType);
        DxtEncoder.encodeImage(textureData, texture.getWidth(), texture.getHeight(), output, dxtType);
        byte[] header = ConvertToPvrz.createPVRHeader(texture.getWidth(), texture.getHeight(), dxtCode);
        byte[] pvrz = new byte[header.length + outSize];
        System.arraycopy(header, 0, pvrz, 0, header.length);
        System.arraycopy(output, 0, pvrz, header.length, outSize);
        header = null;
        pvrz = Compressor.compress(pvrz, 0, pvrz.length, true);

        // writing PVRZ to disk
        BufferedOutputStream bos = null;
        try {
          try {
            bos = new BufferedOutputStream(new FileOutputStream(new File(pvrzName)));
            bos.write(pvrz);
            bos.close();
            bos = null;
          } catch (IOException e) {
            // critical error
            if (bos != null) {
              bos.close();
              bos = null;
            }
            e.printStackTrace();
            result.add(null);
            result.add(String.format("Error writing PVRZ file \"%1$s\" to disk.", pvrzName));
            return false;
          }
        } catch (Exception e) {
          // non-critical error
          e.printStackTrace();
        }
        pvrz = null;
      } catch (Exception e) {
        e.printStackTrace();
        result.add(null);
        result.add(String.format("Error while generating PVRZ files:\n%1$s", e.getMessage()));
        return false;
      }
    }
    output = null;
    return true;
  }


  public ConvertToMos()
  {
    super("Convert to MOS", true);
    init();
  }

//--------------------- Begin Class ChildFrame ---------------------

  @Override
  protected boolean windowClosing(boolean forced) throws Exception
  {
    clear();
    return super.windowClosing(forced);
  }

//--------------------- End Class ChildFrame ---------------------

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bConvert) {
      workerConvert = new SwingWorker<List<String>, Void>() {
        @Override
        public List<String> doInBackground()
        {
          return convert();
        }
      };
      workerConvert.addPropertyChangeListener(this);
      blocker = new WindowBlocker(this);
      blocker.setBlocked(true);
      workerConvert.execute();
    } else if (event.getSource() == bCancel) {
      hideWindow();
    } else if (event.getSource() == bInputV1 || event.getSource() == bInputV2) {
      String fileName =
          tfInputV1.getText().isEmpty() ? ResourceFactory.getRootDir().toString() : tfInputV1.getText();
      if ((fileName = getImageFileName(new File(fileName))) != null) {
        tfInputV1.setText(fileName);
        tfInputV2.setText(fileName);
        if (tfOutputV1.getText().isEmpty()) {
          fileName = setFileExtension(fileName, "MOS");
          tfOutputV1.setText(fileName);
          tfOutputV2.setText(fileName);
        }
        bConvert.setEnabled(isReady());
      }
    } else if (event.getSource() == bOutputV1 || event.getSource() == bOutputV2) {
      String fileName =
          tfOutputV1.getText().isEmpty() ? ResourceFactory.getRootDir().toString() : tfOutputV1.getText();
      if ((fileName = getMosFileName(new File(fileName))) != null) {
        tfOutputV1.setText(fileName);
        tfOutputV2.setText(fileName);
      }
      bConvert.setEnabled(isReady());
    } else if (event.getSource() == bCompressionHelp) {
      final String helpMsg =
          "\"DXT1\" provides the highest compression ratio. It supports only 1 bit alpha\n" +
          "(i.e. either no or full transparency) and is the preferred type for TIS or MOS resources.\n\n" +
          "\"DXT5\" provides an average compression ratio. It features interpolated\n" +
          "alpha transitions and is the preferred type for BAM resources.\n\n" +
          "\"Auto\" selects the most appropriate compression type based on the input data.";
      JOptionPane.showMessageDialog(this, helpMsg, "About Compression Types", JOptionPane.INFORMATION_MESSAGE);
    }
  }

//--------------------- End Interface ActionListener ---------------------

//--------------------- Begin Interface PropertyChangeListener ---------------------

  @Override
  public void propertyChange(PropertyChangeEvent event)
  {
    if (event.getSource() == workerConvert) {
      if ("state".equals(event.getPropertyName()) &&
          SwingWorker.StateValue.DONE == event.getNewValue()) {
        if (blocker != null) {
          blocker.setBlocked(false);
          blocker = null;
        }
        List<String> sl = null;
        try {
          sl = workerConvert.get();
        } catch (Exception e) {
          e.printStackTrace();
        }
        workerConvert = null;

        boolean isError = false;
        String s = null;
        if (sl != null && !sl.isEmpty()) {
          if (sl.get(0) != null) {
            s = sl.get(0);
          } else if (sl.size() > 1 && sl.get(1) != null) {
            s = sl.get(1);
            isError = true;
          }
        }
        if (s != null) {
          if (isError) {
            JOptionPane.showMessageDialog(this, s, "Error", JOptionPane.ERROR_MESSAGE);
          } else {
            JOptionPane.showMessageDialog(this, s, "Information", JOptionPane.INFORMATION_MESSAGE);
            if (cbCloseOnExit.isSelected()) {
              hideWindow();
            } else {
              clear();
            }
          }
        } else {
          JOptionPane.showMessageDialog(this, "Unknown error!", "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  }

//--------------------- End Interface PropertyChangeListener ---------------------

//--------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event)
  {
    if (event.getSource() == sPvrzIndex) {
      lPvrzInfo.setText(pvrzInfoString(sPvrzIndex.getValue()));
    }
  }

//--------------------- End Interface ChangeListener ---------------------

//--------------------- Begin Interface FocusListener ---------------------

  @Override
  public void focusGained(FocusEvent event)
  {
    // nothing to do
  }

  @Override
  public void focusLost(FocusEvent event)
  {
    if (event.getSource() == tfInputV1) {
      tfInputV2.setText(tfInputV1.getText());
      bConvert.setEnabled(isReady());
    } else if (event.getSource() == tfInputV2) {
      tfInputV1.setText(tfInputV2.getText());
      bConvert.setEnabled(isReady());
    } else if (event.getSource() == tfOutputV1) {
      tfOutputV2.setText(tfOutputV1.getText());
      bConvert.setEnabled(isReady());
    } else if (event.getSource() == tfOutputV2) {
      tfOutputV1.setText(tfOutputV2.getText());
      bConvert.setEnabled(isReady());
    }
  }

//--------------------- End Interface FocusListener ---------------------

  private void init()
  {
    GridBagConstraints c = new GridBagConstraints();

    // setting up input/output section (Legacy V1)
    JPanel pFilesV1 = new JPanel(new GridBagLayout());
    pFilesV1.setBorder(BorderFactory.createTitledBorder("Input & Output "));
    JLabel lInputV1 = new JLabel("Input file:");
    JLabel lOutputV1 = new JLabel("Output file:");
    tfInputV1 = new JTextField();
    tfInputV1.addFocusListener(this);
    tfOutputV1 = new JTextField();
    tfOutputV1.addFocusListener(this);
    bInputV1 = new JButton("...");
    bInputV1.addActionListener(this);
    bOutputV1 = new JButton("...");
    bOutputV1.addActionListener(this);
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0);
    pFilesV1.add(lInputV1, c);
    c = setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    pFilesV1.add(tfInputV1, c);
    c = setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 4), 0, 0);
    pFilesV1.add(bInputV1, c);
    c = setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 4, 4, 0), 0, 0);
    pFilesV1.add(lOutputV1, c);
    c = setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 8, 4, 0), 0, 0);
    pFilesV1.add(tfOutputV1, c);
    c = setGBC(c, 2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 4, 4, 4), 0, 0);
    pFilesV1.add(bOutputV1, c);

    // setting up input/output section (PVRZ-based V2)
    JPanel pFilesV2 = new JPanel(new GridBagLayout());
    pFilesV2.setBorder(BorderFactory.createTitledBorder("Input & Output "));
    JLabel lInputV2 = new JLabel("Input file:");
    JLabel lOutputV2 = new JLabel("Output file:");
    tfInputV2 = new JTextField();
    tfInputV2.addFocusListener(this);
    tfOutputV2 = new JTextField();
    tfOutputV2.addFocusListener(this);
    bInputV2 = new JButton("...");
    bInputV2.addActionListener(this);
    bOutputV2 = new JButton("...");
    bOutputV2.addActionListener(this);
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pFilesV2.add(lInputV2, c);
    c = setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    pFilesV2.add(tfInputV2, c);
    c = setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 4), 0, 0);
    pFilesV2.add(bInputV2, c);
    c = setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 4, 0), 0, 0);
    pFilesV2.add(lOutputV2, c);
    c = setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 8, 4, 0), 0, 0);
    pFilesV2.add(tfOutputV2, c);
    c = setGBC(c, 2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 4, 4), 0, 0);
    pFilesV2.add(bOutputV2, c);

    // setting up options section (legacy V1)
    JPanel pOptionsV1 = new JPanel(new GridBagLayout());
    pOptionsV1.setBorder(BorderFactory.createTitledBorder("Options "));
    cbCompress = new JCheckBox("Compressed (MOSC)", false);
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(4, 0, 0, 0), 0, 0);
    pOptionsV1.add(cbCompress, c);
    c = setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pOptionsV1.add(new JPanel(), c);
    c = setGBC(c, 0, 1, 2, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    pOptionsV1.add(new JPanel(), c);

    // setting up options section (PVRZ-based V2)
    JPanel pOptionsV2 = new JPanel(new GridBagLayout());
    pOptionsV2.setBorder(BorderFactory.createTitledBorder("Options "));
    JLabel lPvrzIndex = new JLabel("PVRZ index starts at:");
    JLabel lCompression = new JLabel("Compression type:");
    sPvrzIndex = new JSpinner(new SpinnerNumberModel(0, 0, 99999, 1));
    sPvrzIndex.setToolTipText("Enter a number from 0 to 99999");
    sPvrzIndex.addChangeListener(this);
    cbCompression = new JComboBox(new Object[]{"Auto", "DXT1", "DXT5"});
    cbCompression.setSelectedIndex(0);
    bCompressionHelp = new JButton("?");
    bCompressionHelp.setToolTipText("About compression types");
    bCompressionHelp.addActionListener(this);
    bCompressionHelp.setMargin(new Insets(bCompressionHelp.getInsets().top, 4,
                                          bCompressionHelp.getInsets().bottom, 4));
    lPvrzInfo = new JLabel(pvrzInfoString(sPvrzIndex.getValue()));

    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pOptionsV2.add(lPvrzIndex, c);
    c = setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 0, 0);
    pOptionsV2.add(sPvrzIndex, c);
    c = setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 16, 0, 0), 0, 0);
    pOptionsV2.add(lCompression, c);
    c = setGBC(c, 3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 0, 0);
    pOptionsV2.add(cbCompression, c);
    c = setGBC(c, 4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 4), 0, 0);
    pOptionsV2.add(bCompressionHelp, c);
    c = setGBC(c, 0, 1, 5, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0);
    pOptionsV2.add(lPvrzInfo, c);

    // setting up tabbed pane
    tabPane = new JTabbedPane(JTabbedPane.TOP);

    JPanel pTabV1 = new JPanel(new GridBagLayout());
    c = setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 4, 2, 4), 0, 0);
    pTabV1.add(pFilesV1, c);
    c = setGBC(c, 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START,
               GridBagConstraints.BOTH, new Insets(2, 4, 4, 4), 0, 0);
    pTabV1.add(pOptionsV1, c);
    tabPane.addTab("Legacy (V1)", pTabV1);
    tabPane.setMnemonicAt(0, KeyEvent.VK_1);

    JPanel pTabV2 = new JPanel(new GridBagLayout());
    c = setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 4, 2, 4), 0, 0);
    pTabV2.add(pFilesV2, c);
    c = setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(2, 4, 4, 4), 0, 0);
    pTabV2.add(pOptionsV2, c);
    tabPane.addTab("PVRZ-based (V2)", pTabV2);
    tabPane.setMnemonicAt(1, KeyEvent.VK_2);
    tabPane.setSelectedIndex(0);

    // setting up bottom button bar
    cbCloseOnExit = new JCheckBox("Close dialog after conversion", true);
    bConvert = new JButton("Start Conversion");
    bConvert.addActionListener(this);
    bConvert.setEnabled(isReady());
    Insets i = bConvert.getInsets();
    bConvert.setMargin(new Insets(i.top + 2, i.left, i.bottom + 2, i.right));
    bCancel = new JButton("Cancel");
    bCancel.addActionListener(this);
    i = bCancel.getInsets();
    bCancel.setMargin(new Insets(i.top + 2, i.left, i.bottom + 2, i.right));

    JPanel pButtons = new JPanel(new GridBagLayout());
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pButtons.add(cbCloseOnExit, c);
    c = setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pButtons.add(new JPanel(), c);
    c = setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
               GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pButtons.add(bConvert, c);
    c = setGBC(c, 3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pButtons.add(bCancel, c);

    // putting all together
    setLayout(new GridBagLayout());
    c = setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 8), 0, 0);
    add(tabPane, c);
    c = setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 8), 0, 0);
    add(pButtons, c);
    c = setGBC(c, 0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    add(new JPanel(), c);

    // finalizing dialog initialization
    pack();
    setMinimumSize(getPreferredSize());
    setLocationRelativeTo(getParent());
    setVisible(true);
  }

  private GridBagConstraints setGBC(GridBagConstraints gbc, int gridX, int gridY,
                                    int gridWidth, int gridHeight, double weightX, double weightY,
                                    int anchor, int fill, Insets insets, int iPadX, int iPadY)
  {
    if (gbc == null)
      gbc = new GridBagConstraints();

    gbc.gridx = gridX;
    gbc.gridy = gridY;
    gbc.gridwidth = gridWidth;
    gbc.gridheight = gridHeight;
    gbc.weightx = weightX;
    gbc.weighty = weightY;
    gbc.anchor = anchor;
    gbc.fill = fill;
    gbc.insets = (insets == null) ? new Insets(0, 0, 0, 0) : insets;
    gbc.ipadx = iPadX;
    gbc.ipady = iPadY;

    return gbc;
  }


  private void hideWindow()
  {
    clear();
    setVisible(false);
  }

  // resetting dialog state
  private void clear()
  {
    tfInputV1.setText("");
    tfInputV2.setText("");
    tfOutputV1.setText("");
    tfOutputV2.setText("");
    bConvert.setEnabled(isReady());
  }

  // got enough data to start conversion?
  private boolean isReady()
  {
    boolean ret = false;
    if (!tfInputV1.getText().isEmpty() && !tfOutputV1.getText().isEmpty()) {
      File f = new File(tfInputV1.getText());
      ret = f.exists() && f.isFile();
    }
    return ret;
  }

  private int getPvrzIndex(Object o)
  {
    int index = 0;
    if (o != null) {
      try {
        if (o instanceof Integer) {
          index = ((Integer)o).intValue();
        } else {
          index = Integer.parseInt(o.toString());
        }
      } catch (Exception e) {
      }
    }
    return index;
  }

  private String pvrzInfoString(Object o)
  {
    int index = getPvrzIndex(o);
    return String.format("Resulting in MOS%1$04d.PVRZ, MOS%2$04d.PVRZ, ...", index, index+1);
  }

  private String getImageFileName(File path)
  {
    JFileChooser fc = new JFileChooser();
    fc.setDialogTitle("Select input graphics file");
    fc.setDialogType(JFileChooser.OPEN_DIALOG);
    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    FileNameExtensionFilter[] filters = getInputFilters();
    for (final FileNameExtensionFilter filter: filters) {
      fc.addChoosableFileFilter(filter);
    }
    fc.setFileFilter(filters[0]);
    if (path != null) {
      fc.setSelectedFile(path);
    }
    int ret = fc.showOpenDialog(this);
    if (ret == JFileChooser.APPROVE_OPTION) {
      return fc.getSelectedFile().toString();
    } else {
      return null;
    }
  }

  private String getMosFileName(File path)
  {
    JFileChooser fc = new JFileChooser();
    fc.setDialogTitle("Specify output filename");
    fc.setDialogType(JFileChooser.SAVE_DIALOG);
    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    FileNameExtensionFilter filter = new FileNameExtensionFilter("MOS files (*.mos)", "mos");
    fc.addChoosableFileFilter(filter);
    fc.setFileFilter(filter);
    if (path != null) {
      fc.setSelectedFile(path);
    }
    int ret = fc.showSaveDialog(this);
    if (ret == JFileChooser.APPROVE_OPTION) {
      return fc.getSelectedFile().toString();
    } else {
      return null;
    }
  }

  private List<String> convert()
  {
    List<String> result = new Vector<String>(2);

    // validating input file
    File inFile = new File(tfInputV1.getText());
    if (!inFile.exists() || !inFile.isFile()) {
      result.add(null);
      result.add(String.format("Input file \"%1$s\" does not exist.", tfInputV1.getText()));
      return result;
    }

    // loading source image
    BufferedImage srcImage = null;
    try {
      srcImage = ColorConvert.toBufferedImage(ImageIO.read(inFile), true);
    } catch (Exception e) {
    }
    if (srcImage == null) {
      result.add(null);
      result.add("Unable to load source image.");
      return result;
    }

    // handling "auto" compression
    DxtEncoder.DxtType dxtType = DxtEncoder.DxtType.DXT1;
    if (tabPane.getSelectedIndex() == 1 && cbCompression.getSelectedIndex() == 0) {
      int[] pixels = ((DataBufferInt)srcImage.getRaster().getDataBuffer()).getData();
      for (int i = 0; i < pixels.length; i++) {
        int alpha = pixels[i] >>> 24;
        if (alpha > 0x20 && alpha < 0xe0) {
          dxtType = DxtEncoder.DxtType.DXT5;
          break;
        }
      }
    }

    // fetching remaining settings
    int pvrzIndex = getPvrzIndex(sPvrzIndex.getValue());
    boolean isMOSC = cbCompress.isSelected();

    // converting
    if (tabPane.getSelectedIndex() == 0) {
      convertV1(this, srcImage, tfOutputV1.getText(), isMOSC, result, true);
    } else if (tabPane.getSelectedIndex() == 1) {
      convertV2(this, srcImage, tfOutputV2.getText(), dxtType, pvrzIndex, result, true);
    } else {
      result.add(null);
      result.add("No MOS type specified!");
    }

    return result;
  }


//-------------------------- INNER CLASSES --------------------------

 private static class MosEntry
 {
   public int page;
   public int width, height;
   public Point srcLocation;
   public Point dstLocation;

   public MosEntry(int page, Point srcLocation, int width, int height, Point dstLocation)
   {
     this.page = page;
     this.srcLocation = srcLocation;
     this.width = width;
     this.height = height;
     this.dstLocation = dstLocation;
   }
 }
}
