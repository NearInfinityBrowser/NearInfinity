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
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ConvertToTis extends ChildFrame
    implements ActionListener, PropertyChangeListener, ChangeListener, FocusListener, KeyListener
{
  private static String currentDir = ResourceFactory.getRootDir().toString();

  private String inFileName;
  private JSlider sTileNum;
  private JTextField tfInput, tfOutput, tfTileNum;
  private JButton bConvert, bCancel;
  private JButton bInput, bOutput, bVersionHelp;
  private JComboBox cbVersion;
  private JCheckBox cbCloseOnExit;
  private SwingWorker<List<String>, Void> workerConvert;
  private WindowBlocker blocker;


  /**
   * Converts an image into a TIS V1 resource.
   * @param parent This parameter is needed for the progress monitor only.
   * @param img The source image to convert into a TIS resource.
   * @param tisFileName The name of the resulting TIS file.
   * @param tileCount The number of tiles to convert.
   * @param result Returns more specific information about the conversion process. Data placed in the
   *               first item indicates success, data in the second item indicates failure.
   * @param showProgress Specify whether to show a progress monitor (needs a valid 'parent' parameter).
   * @return <code>true</code> if the conversion finished successfully, <code>false</code> otherwise.
   */
  public static boolean convertV1(Component parent, BufferedImage img, String tisFileName, int tileCount,
                                  List<String> result, boolean showProgress)
  {
    // checking parameters
    if (result == null) {
      return false;
    }
    if (img == null) {
      result.add(null);
      result.add("No source image specified");
      return false;
    }
    if (img.getWidth() <= 0 || ((img.getWidth() % 64) != 0) ||
        img.getHeight() <= 0 || ((img.getHeight() % 64) != 0)) {
      result.add(null);
      result.add("The dimensions of the source image have to be a multiple of 64 pixels.\n" +
                 String.format("Current dimensions are %1$dx%2$d", img.getWidth(), img.getHeight()));
      return false;
    }
    if (tisFileName == null || tisFileName.isEmpty()) {
      result.add(null);
      result.add("No output filename specified.");
      return false;
    }
    if (tileCount < 1 || tileCount > (img.getWidth()*img.getHeight()/4096)) {
      result.add(null);
      result.add("Invalid number of tiles specified.");
      return false;
    }

    ProgressMonitor progress = null;
    int[] src = ((DataBufferInt)img.getRaster().getDataBuffer()).getData();
    byte[] dst = new byte[24 + tileCount*5120];   // header + tiles
    int dstOfs = 0;   // current start offset for write operations

    // writing header data
    System.arraycopy("TIS V1  ".getBytes(Charset.forName("US-ASCII")), 0, dst, 0, 8);
    DynamicArray.putInt(dst, 8, tileCount);
    DynamicArray.putInt(dst, 12, 0x1400);
    DynamicArray.putInt(dst, 16, 0x18);
    DynamicArray.putInt(dst, 20, 0x40);
    dstOfs += 24;

    int[] srcBlock = new int[64*64];      // temp. storage for a single tile
    int[] palette = new int[255];         // temp. storage for generated palette
    int[] hslPalette = new int[255];      // needed for finding nearest color
    byte[] tilePalette = new byte[1024];  // final palette for output
    byte[] tileData = new byte[64*64];    // final tile data for output
    int tw = img.getWidth() / 64;         // tiles per row

    try {
      String note = "Converting tile %1$d / %2$d";
      int progressIndex = 0, progressMax = tileCount;
      if (showProgress) {
        progress = new ProgressMonitor(parent, "Converting TIS...", String.format(note, 0, tileCount),
                                       0, progressMax);
        progress.setMillisToDecideToPopup(0);
        progress.setMillisToPopup(0);
      }

      IntegerHashMap<Byte> colorCache = new IntegerHashMap<Byte>(2048);   // caching RGBColor -> index
      for (int tileIdx = 0; tileIdx < tileCount; tileIdx++) {
        if (showProgress) {
          if (progress.isCanceled()) {
            result.add(null);
            result.add("Conversion has been cancelled.");
            return false;
          }
          progressIndex++;
          if ((progressIndex % 100) == 0) {
            progress.setProgress(progressIndex);
            progress.setNote(String.format(note, progressIndex, progressMax));
          }
        }

        int tx = tileIdx % tw;
        int ty = tileIdx / tw;

        // resetting color cache
        colorCache.clear();

        // initializing source tile
        int inOfs = ty*64*img.getWidth() + tx*64;
        for (int i = 0, outOfs = 0; i < 64; i++, inOfs += img.getWidth(), outOfs += 64) {
          System.arraycopy(src, inOfs, srcBlock, outOfs, 64);
        }

        // reducing colors
        if (ColorConvert.medianCut(srcBlock, 255, palette, false)) {
          ColorConvert.toHslPalette(palette, hslPalette);

          // filling palette and color cache, index 0 denotes transparency
          tilePalette[0] = tilePalette[2] = tilePalette[3] = 0; tilePalette[1] = (byte)255;
          for (int i = 1; i < 256; i++) {
            tilePalette[(i << 2) + 0] = (byte)(palette[i - 1] & 0xff);
            tilePalette[(i << 2) + 1] = (byte)((palette[i - 1] >>> 8) & 0xff);
            tilePalette[(i << 2) + 2] = (byte)((palette[i - 1] >>> 16) & 0xff);
            tilePalette[(i << 2) + 3] = 0;
            colorCache.put(palette[i - 1], (byte)(i - 1));
          }

          // processing pixel data
          for (int i = 0; i < tileData.length; i++) {
            if ((srcBlock[i] & 0xff000000) == 0) {
              tileData[i] = 0;
            } else {
              Byte palIndex = colorCache.get(srcBlock[i]);
              if (palIndex != null) {
                tileData[i] = (byte)(palIndex + 1);
              } else {
                byte color = (byte)ColorConvert.nearestColor(srcBlock[i], hslPalette);
                tileData[i] = (byte)(color + 1);
                colorCache.put(srcBlock[i], color);
              }
            }
          }
        } else {
          // error handling
          result.add(null);
          result.add(String.format("Error processing tile #%1$d. Conversion cancelled.", tileIdx));
          return false;
        }

        // writing final palette and pixel data to output
        System.arraycopy(tilePalette, 0, dst, dstOfs, 1024);
        dstOfs += 1024;
        System.arraycopy(tileData, 0, dst, dstOfs, 4096);
        dstOfs += 4096;
      }

      // writing TIS file to disk
      BufferedOutputStream bos = null;
      try {
        try {
          bos = new BufferedOutputStream(new FileOutputStream(new File(tisFileName)));
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
      src = null; dst = null;
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

  /**
   * Converts an image into a TIS V2 resource.
   * @param parent This parameter is needed for the progress monitor only.
   * @param img The source image to convert into a TIS resource.
   * @param tisFileName The name of the resulting TIS file.
   * @param tileCount The number of tiles to convert.
   * @param result Returns more specific information about the conversion process. Data placed in the
   *               first item indicates success, data in the second item indicates failure.
   * @param showProgress Specify whether to show a progress monitor (needs a valid 'parent' parameter).
   * @return <code>true</code> if the conversion finished successfully, <code>false</code> otherwise.
   */
  public static boolean convertV2(Component parent, BufferedImage img, String tisFileName,
                                  int tileCount, List<String> result, boolean showProgress)
  {
    // checking parameters
    if (result == null) {
      return false;
    }
    if (img == null) {
      result.add(null);
      result.add("No source image specified");
      return false;
    }
    if (img.getWidth() <= 0 || ((img.getWidth() % 64) != 0) ||
        img.getHeight() <= 0 || ((img.getHeight() % 64) != 0)) {
      result.add(null);
      result.add("The dimensions of the source image have to be a multiple of 64 pixels.\n" +
                 String.format("Current dimensions are %1$dx%2$d", img.getWidth(), img.getHeight()));
      return false;
    }
    if (tisFileName == null || tisFileName.isEmpty()) {
      result.add(null);
      result.add("No output filename specified.");
      return false;
    }
    if (!tisFileName.equalsIgnoreCase(createValidTisName(tisFileName, 2))) {
      result.add(null);
      result.add("Incompatible output file specified.\n\n" +
              "Note:\nPVRZ-based TIS files are restricted to exactly 6 characters\n" +
              "plus optional 'N' for night tilesets.)");
      return false;
    }
    if (tileCount < 1 || tileCount > (img.getWidth()*img.getHeight()/4096)) {
      result.add(null);
      result.add("Invalid number of tiles specified.");
      return false;
    }

    // preparing variables
    ProgressMonitor progress = null;
    List<GridManager> pageListComplete = new ArrayList<GridManager>();
    List<GridManager> pageListIncomplete = new ArrayList<GridManager>();
    List<TileEntry> entryList = new ArrayList<TileEntry>(tileCount);

    byte[] dst = new byte[24 + tileCount*12];   // header + tiles
    int dstOfs = 0;   // current start offset for write operations

    try {
      if (showProgress) {
        // preparing progress meter
        progress = new ProgressMonitor(parent, "Converting TIS...", "Preparing TIS", 0, 5);
        progress.setMillisToDecideToPopup(0);
        progress.setMillisToPopup(0);
        progress.setProgress(0);
      }

      // writing header data
      System.arraycopy("TIS V1  ".getBytes(Charset.forName("US-ASCII")), 0, dst, 0, 8);
      DynamicArray.putInt(dst, 8, tileCount);
      DynamicArray.putInt(dst, 12, 0x0c);
      DynamicArray.putInt(dst, 16, 0x18);
      DynamicArray.putInt(dst, 20, 0x40);
      dstOfs += 24;

      // processing tiles
      final int pageDim = 1024;
      final int tileDim = 64;
      final int tilesPerDim = pageDim / tileDim;
      int pw = img.getWidth() / pageDim + (((img.getWidth() % pageDim) != 0) ? 1 : 0);
      int ph = img.getHeight() / pageDim + (((img.getHeight() % pageDim) != 0) ? 1 : 0);

      for (int py = 0; py < ph; py++) {
        for (int px = 0; px < pw; px++) {
          int x = px * pageDim, y = py * pageDim;
          int w = Math.min(pageDim, img.getWidth() - x);
          int h = Math.min(pageDim, img.getHeight() - y);
          if (w == pageDim && h == pageDim) {
            // add page to complete pages list
            GridManager gm = new GridManager(tilesPerDim, tilesPerDim);
            gm.add(new Rectangle(0, 0, tilesPerDim, tilesPerDim));
            pageListComplete.add(gm);
            // register each tile entry in the page
            int pageIdx = pageListComplete.size() - 1;
            int tileIdx = (y*img.getWidth())/(tileDim*tileDim) + x/tileDim;
            for (int ty = 0; ty < tilesPerDim; ty++, tileIdx += img.getWidth()/tileDim) {
              for (int tx = 0; tx < tilesPerDim; tx++) {
                if (tileIdx + tx < tileCount) {
                  TileEntry entry = new TileEntry(tileIdx + tx, pageIdx, tileDim*tx, tileDim*ty);
                  entryList.add(entry);
                }
              }
            }
          } else {
            // find first available page containing sufficient space for the current region
            Dimension space = new Dimension(w/tileDim, h/tileDim);
            int pageIdx = -1;
            Rectangle rectMatch = new Rectangle(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
            for (int i = 0; i < pageListIncomplete.size(); i++) {
              GridManager gm = pageListIncomplete.get(i);
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
              GridManager gm = new GridManager(tilesPerDim, tilesPerDim);
              pageListIncomplete.add(gm);
              pageIdx = pageListIncomplete.size() - 1;
              rectMatch.x = rectMatch.y = 0;
              rectMatch.width = gm.getWidth(); rectMatch.height = gm.getHeight();
            }

            // add region to the page
            GridManager gm = pageListIncomplete.get(pageIdx);
            gm.add(new Rectangle(rectMatch.x, rectMatch.y, space.width, space.height));
            // registering tile entries
            int tileIdx = (y*img.getWidth())/(tileDim*tileDim) + x/tileDim;
            for (int ty = 0; ty < space.height; ty++, tileIdx += img.getWidth()/tileDim) {
              for (int tx = 0; tx < space.width; tx++) {
                // marking page index as incomplete
                if (tileIdx + tx < tileCount) {
                  TileEntry entry = new TileEntry(tileIdx + tx, pageIdx | 0x80000000,
                                                  (rectMatch.x + tx)*tileDim, (rectMatch.y + ty)*tileDim);
                  entryList.add(entry);
                }
              }
            }
          }
        }
      }

      // adjusting page indices of the marked tiles
      int incompleteBase = pageListComplete.size();
      for (int i = 0; i < pageListIncomplete.size(); i++) {
        GridManager gm = pageListIncomplete.get(i);
        gm.shrink();
        pageListComplete.add(gm);
      }
      for (int i = 0; i < entryList.size(); i++) {
        TileEntry entry = entryList.get(i);
        if ((entry.page & 0x80000000) != 0) {
          entry.page = (entry.page & 0xff) + incompleteBase;
        }
      }

      // writing TIS entries
      Collections.sort(entryList, TileEntry.CompareByIndex);
      for (int i = 0; i < entryList.size(); i++, dstOfs += 12) {
        TileEntry entry = entryList.get(i);
        DynamicArray.putInt(dst, dstOfs, entry.page);
        DynamicArray.putInt(dst, dstOfs + 4, entry.x);
        DynamicArray.putInt(dst, dstOfs + 8, entry.y);
      }

      // writing TIS file to disk
      BufferedOutputStream bos = null;
      try {
        try {
          bos = new BufferedOutputStream(new FileOutputStream(new File(tisFileName)));
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

      // generating PVRZ files
      if (!createPvrzPages(tisFileName, img, pageListComplete, DxtEncoder.DxtType.DXT1, entryList,
                           result, progress)) {
        return false;
      }
    } finally {
      // some cleaning up
      dst = null; img.flush();
      if (progress != null) {
        progress.close();
        progress = null;
      }
    }

    // generating conversion summary
    result.add("Conversion finished successfully.");
    return true;
  }

  /**
   * Returns a valid TIS filename based on the parameters.
   * @param tisFilename The TIS filename the return value is based on.
   * @param tisVersion The TIS version to consider (1=V1, 2=V2).
   * @return A valid TIS filename.
   */
  public static String createValidTisName(String tisFilename, int tisVersion)
  {
    // extracting file path and filename without extension
    File outFile = new File(tisFilename);
    String outPath = outFile.getParent();
    if (outPath == null) {
      outPath = "";
    } else if (outPath.charAt(outPath.length() - 1) != File.separatorChar) {
      outPath = outPath + File.separator;
    }
    String outNameBase = outFile.getName();
    if (outNameBase == null || outNameBase.isEmpty() || outNameBase.charAt(0) == '.') {
      outNameBase = "OUTPUT";
    }
    if (outNameBase.lastIndexOf('.') > 0) {
      outNameBase = outNameBase.substring(0, outNameBase.lastIndexOf('.'));
    }

    // limit output filename to 6/7 or 8 characters (based on the TIS version)
    if (tisVersion == 2) {
      if (!Pattern.matches(".{6}[nN]?", outNameBase)) {
        if (outNameBase.length() > 6) {
          outNameBase = outNameBase.substring(0, 6);
        } else {
          final String fill = "000000";
          outNameBase = outNameBase + fill.substring(0, 6 - outNameBase.length());
        }
      }
    } else {
      if (outNameBase.length() > 8) {
        outNameBase = outNameBase.substring(0, 8);
      }
    }

    return outPath + outNameBase + ".TIS";
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

  // generates a PVRZ filename based on the specified parameters
  private static String generatePvrzName(String tisFileName, int page)
  {
    File tisFile = new File(tisFileName);
    String path = tisFile.getParent();
    if (path == null) {
      path = "";
    } else if (path.charAt(path.length() - 1) != File.separatorChar) {
      path = path + File.separator;
    }
    String tisNameBase = tisFile.getName();
    if (tisNameBase.lastIndexOf('.') > 0) {
      tisNameBase = tisNameBase.substring(0, tisNameBase.lastIndexOf('.'));
    }
    if (Pattern.matches(".{6}[nN]?", tisNameBase)) {
      String pvrzName = String.format("%1$s%2$s%3$02d.PVRZ", tisNameBase.substring(0, 1),
                                      tisNameBase.substring(2, tisNameBase.length()), page);
      return path + pvrzName;
    }
    return "";
  }

  // generates PVRZ textures
  private static boolean createPvrzPages(String tisFileName, BufferedImage srcImg,
                                         List<GridManager> pages, DxtEncoder.DxtType dxtType,
                                         List<TileEntry> entryList, List<String> result,
                                         ProgressMonitor progress)
  {
    int dxtCode = (dxtType == DxtEncoder.DxtType.DXT5) ? 11 : 7;
    byte[] output = new byte[DxtEncoder.calcImageSize(1024, 1024, dxtType)];
    String note = "Generating PVRZ file %1$s / %2$s";
    if (progress != null) {
      progress.setMinimum(0);
      progress.setMaximum(pages.size() + 1);
      progress.setProgress(1);
    }
    for (int pageIdx = 0; pageIdx < pages.size(); pageIdx++) {
      if (progress != null) {
        if (progress.isCanceled()) {
          result.add(null);
          result.add("Conversion has been cancelled.");
          return false;
        }
        progress.setProgress(pageIdx + 1);
        progress.setNote(String.format(note, pageIdx+1, pages.size()));
      }
      String pvrzName = generatePvrzName(tisFileName, pageIdx);
      GridManager gm = pages.get(pageIdx);

      // generating texture image
      int w = ConvertToPvrz.nextPowerOfTwo(gm.getWidth()*64);
      int h = ConvertToPvrz.nextPowerOfTwo(gm.getHeight()*64);
      BufferedImage texture = ColorConvert.createCompatibleImage(w, h, true);
      Graphics2D g = (Graphics2D)texture.getGraphics();
      g.setBackground(new Color(0, true));
      g.setColor(Color.BLACK);
      g.fillRect(0, 0, texture.getWidth(), texture.getHeight());
      int tw = srcImg.getWidth() / 64;
      for (final TileEntry entry: entryList) {
        if (entry.page == pageIdx) {
          int sx = (entry.tileIndex % tw) * 64, sy = (entry.tileIndex / tw) * 64;
          int dx = entry.x, dy = entry.y;
          g.clearRect(dx, dy, 64, 64);
          g.drawImage(srcImg, dx, dy, dx+64, dy+64, sx, sy, sx+64, sy+64, null);
        }
      }
      g.dispose();
      int[] textureData = ((DataBufferInt)texture.getRaster().getDataBuffer()).getData();
      try {
        // compressing PVRZ
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


  public ConvertToTis()
  {
    super("Convert to TIS", true);
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
    } else if (event.getSource() == bInput) {
      JFileChooser fc = new JFileChooser(currentDir);
      fc.setDialogTitle("Select input graphics file");
      fc.setDialogType(JFileChooser.OPEN_DIALOG);
      fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
      FileNameExtensionFilter[] filters = getInputFilters();
      for (final FileNameExtensionFilter filter: filters) {
        fc.addChoosableFileFilter(filter);
      }
      fc.setFileFilter(filters[0]);
      if (!tfInput.getText().isEmpty()) {
        fc.setSelectedFile(new File(tfInput.getText()));
      }
      int ret = fc.showOpenDialog(this);
      if (ret == JFileChooser.APPROVE_OPTION) {
        File file = fc.getSelectedFile();
        currentDir = file.getParent();
        inFileName = file.toString();
        tfInput.setText(inFileName);
        validateInput(tfInput.getText());
        if (tfOutput.getText().isEmpty()) {
          tfOutput.setText(createValidTisName(tfInput.getText(), getTisVersion()));
        }
      }
    } else if (event.getSource() == bOutput) {
      JFileChooser fc = new JFileChooser(ResourceFactory.getRootDir());
      fc.setDialogTitle("Specify output filename");
      fc.setDialogType(JFileChooser.SAVE_DIALOG);
      fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
      FileNameExtensionFilter filter = new FileNameExtensionFilter("TIS files (*.tis)", "tis");
      fc.addChoosableFileFilter(filter);
      fc.setFileFilter(filter);
      String fileName = tfOutput.getText();
      if (fileName.isEmpty() && !tfInput.getText().isEmpty()) {
        File f = new File(tfInput.getText());
        if (f.exists() && f.isFile()) {
          fileName = createValidTisName(tfInput.getText(), getTisVersion());
        }
      }
      fc.setSelectedFile(new File(fileName));
      int ret = fc.showSaveDialog(this);
      while (ret == JFileChooser.APPROVE_OPTION) {
        currentDir = fc.getSelectedFile().getParent();
        String orig = setFileExtension(fc.getSelectedFile().toString(), "TIS");
        String fixed = createValidTisName(orig, getTisVersion());
        if (!orig.equalsIgnoreCase(fixed)) {
          ret = JOptionPane.showConfirmDialog(this,
              "The chosen output file is not compatible with the current TIS settings.\n" +
                  "Do you want me to fix it?\n",
              "Invalid output filename", JOptionPane.YES_NO_CANCEL_OPTION);
          if (ret == JOptionPane.YES_OPTION) {
            tfOutput.setText(fixed);
            break;
          } else if (ret == JOptionPane.NO_OPTION) {
            ret = fc.showSaveDialog(this);
          } else {
            break;
          }
        } else {
          tfOutput.setText(orig);
          break;
        }
      }
    } else if (event.getSource() == bVersionHelp) {
      final String helpMsg =
          "\"Legacy\" is the old and proven TIS format supported by all available\n" +
          "Infinity Engine games. Graphics data is stored in the TIS file directly.\n" +
          "Each tile (64x64 pixel block) is limited to a 256 color table.\n\n" +
          "\"PVRZ-based\" uses a new TIS format introduced by BG:EE. Graphics data\n" +
          "is stored separately in PVRZ files and is not limited to a 256 color table.\n" +
          "It is only supported by the Enhanced Editions of the Baldur's Gate games.";
      JOptionPane.showMessageDialog(this, helpMsg, "About TIS versions",
          JOptionPane.INFORMATION_MESSAGE);
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
    if (event.getSource() == sTileNum) {
      tileNumSliderUpdated();
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
    if (event.getSource() == tfInput) {
      // validating input file (if it has changed)
      if (inFileName == null || !inFileName.equals(tfInput.getText())) {
        try {
          WindowBlocker.blockWindow(this, true);
          validateInput(tfInput.getText());
        } finally {
          WindowBlocker.blockWindow(this, false);
        }
      }
    } else if (event.getSource() == tfTileNum) {
      tileNumEditUpdated(true);
    }
  }

//--------------------- End Interface FocusListener ---------------------

//--------------------- Begin Interface KeyListener ---------------------

  @Override
  public void keyTyped(KeyEvent event)
  {
    // nothing to do
  }

  @Override
  public void keyPressed(KeyEvent event)
  {
    if (event.getSource() == tfTileNum) {
      if (event.getKeyCode() == KeyEvent.VK_ENTER) {
        tileNumEditUpdated(true);
        sTileNum.requestFocus();
      }
    }
  }

  @Override
  public void keyReleased(KeyEvent event)
  {
    // nothing to do
  }

//--------------------- End Interface KeyListener ---------------------

  private void init()
  {
    // setting up files section
    JPanel pFiles = new JPanel(new GridBagLayout());
    pFiles.setBorder(BorderFactory.createTitledBorder("Input & Output "));
    GridBagConstraints c = new GridBagConstraints();
    JLabel lInput = new JLabel("Input file:");
    JLabel lOutput = new JLabel("Output file:");
    tfInput = new JTextField();
    tfInput.addFocusListener(this);
    tfOutput = new JTextField();
    tfOutput.addFocusListener(this);
    bInput = new JButton("...");
    bInput.addActionListener(this);
    bOutput = new JButton("...");
    bOutput.addActionListener(this);
    JLabel lInputNote =
        new JLabel("Note: Width and height of the source image have to be a multiple of 64 pixels.");

    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pFiles.add(lInput, c);
    c = setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    pFiles.add(tfInput, c);
    c = setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 4), 0, 0);
    pFiles.add(bInput, c);

    c = setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pFiles.add(lOutput, c);
    c = setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    pFiles.add(tfOutput, c);
    c = setGBC(c, 2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 4), 0, 0);
    pFiles.add(bOutput, c);

    c = setGBC(c, 0, 2, 3, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(8, 4, 4, 0), 0, 0);
    pFiles.add(lInputNote, c);

    // setting up options section
    JPanel pSubOptions = new JPanel(new GridBagLayout());
    JLabel lVersion = new JLabel("TIS version:");
    cbVersion = new JComboBox(new Object[]{"Legacy", "PVRZ-based"});
    cbVersion.setSelectedIndex(0);
    bVersionHelp = new JButton("?");
    bVersionHelp.setToolTipText("About TIS versions");
    bVersionHelp.addActionListener(this);
    bVersionHelp.setMargin(new Insets(bVersionHelp.getInsets().top, 4,
                                      bVersionHelp.getInsets().bottom, 4));

    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pSubOptions.add(lVersion, c);
    c = setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 8, 0);
    pSubOptions.add(cbVersion, c);
    c = setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pSubOptions.add(bVersionHelp, c);
    c = setGBC(c, 3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pSubOptions.add(new JPanel(), c);

    JPanel pOptions = new JPanel(new GridBagLayout());
    pOptions.setBorder(BorderFactory.createTitledBorder("Options "));
    JLabel lTileNum = new JLabel("Number of tiles to convert:");
    lTileNum.setToolTipText("Counting from left to right, top to bottom.");
    sTileNum = new JSlider(JSlider.HORIZONTAL);
    sTileNum.addChangeListener(this);
    tfTileNum = new JTextField(6);
    tfTileNum.addKeyListener(this);
    tfTileNum.addFocusListener(this);

    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pOptions.add(lTileNum, c);
    c = setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    pOptions.add(sTileNum, c);
    c = setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 8, 0, 4), 0, 0);
    pOptions.add(tfTileNum, c);
    c = setGBC(c, 0, 1, 3, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pOptions.add(pSubOptions, c);

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
    add(pFiles, c);
    c = setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 8, 8, 8), 0, 0);
    add(pOptions, c);
    c = setGBC(c, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 8, 0, 8), 0, 0);
    add(pButtons, c);
    c = setGBC(c, 0, 3, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START,
               GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    add(new JPanel(), c);

    // finalizing dialog initialization
    validateInput(inFileName);
    pack();
    setMinimumSize(getPreferredSize());
    setLocationRelativeTo(getParent());
    bInput.requestFocus();
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
    tfInput.setText("");
    tfOutput.setText("");
    sTileNum.setMinimum(0);
    sTileNum.setMaximum(0);
    sTileNum.setValue(0);
    tfTileNum.setText("0");
    bConvert.setEnabled(isReady());
  }

  // got enough data to start conversion?
  private boolean isReady()
  {
    boolean ret = false;
    if (!getInputFile().isEmpty()) {
      File f = new File(getInputFile());
      ret = f.exists() && f.isFile();
    }
    return ret;
  }

  private void tileNumEditUpdated(boolean updateText)
  {
    try {
      int tiles = Integer.parseInt(tfTileNum.getText());
      if (tiles < sTileNum.getMinimum())
        tiles = sTileNum.getMinimum();
      if (tiles > sTileNum.getMaximum())
        tiles = sTileNum.getMaximum();
      if (sTileNum.getValue() != tiles) {
        sTileNum.setValue(tiles);
      }
      tileNumSliderUpdated();
    } catch (NumberFormatException e) {
      if (updateText)
        tfTileNum.setText(Integer.toString(sTileNum.getValue()));
    }
  }

  private void tileNumSliderUpdated()
  {
    String s = Integer.toString(sTileNum.getValue());
    if (!tfTileNum.getText().equals(s)) {
      tfTileNum.setText(s);
    }
  }

  // returns 1=TIS V1 or 2=TIS V2
  private int getTisVersion()
  {
    return (cbVersion.getSelectedIndex() == 1) ? 2 : 1;
  }

  // returns number of tiles to convert
  private int getTileCount()
  {
    return (sTileNum.getValue() > 0) ? sTileNum.getValue() : 0;
  }

  private String getInputFile()
  {
    return (inFileName != null) ? inFileName : "";
  }

  // checking image dimensions and calculating max. possible number of tiles
  private boolean validateInput(String inputFile)
  {
    int tileCount = 0;
    boolean isValid = false;
    inFileName = inputFile;
    if (inFileName != null && !inFileName.isEmpty()) {
      File f = new File(inFileName);
      if (f.exists() && f.isFile()) {
        Dimension dimImage = ColorConvert.getImageDimension(inFileName);
        if (dimImage.width >= 0 && (dimImage.width % 64) == 0 &&
            dimImage.height >= 0 && (dimImage.height % 64) == 0) {
          tileCount = (dimImage.width * dimImage.height) / 4096;
          isValid = true;
        }
      }
    }
    if (isValid) {
      // enable and initialize relevant components
      sTileNum.setMinimum(1);
      sTileNum.setMaximum(tileCount);
      sTileNum.setValue(sTileNum.getMaximum());
      sTileNum.setEnabled(true);
      tfTileNum.setEnabled(true);
      tileNumSliderUpdated();
      bConvert.setEnabled(isReady());
    } else {
      // disable relevant components
      sTileNum.setMinimum(0);
      sTileNum.setMaximum(0);
      sTileNum.setValue(0);
      sTileNum.setEnabled(false);
      tfTileNum.setText("0");
      tfTileNum.setEnabled(false);
      bConvert.setEnabled(false);
    }

    return isValid;
  }

  // Converts source image into a TIS file and optional PVRZ file(s). Returns a summary of the result.
  // Return value: First list element is used for success message, second element for error message.
  private List<String> convert()
  {
    List<String> ret = new Vector<String>(2);

    // validating input file
    File inFile = new File(inFileName);
    if (!inFile.exists() || !inFile.isFile()) {
      ret.add(null);
      ret.add(String.format("Input file \"%1$s\" does not exist.", inFileName));
      return ret;
    }

    // loading source image
    BufferedImage srcImage = null;
    try {
      srcImage = ColorConvert.toBufferedImage(ImageIO.read(inFile), true);
    } catch (Exception e) {
    }
    if (srcImage == null) {
      ret.add(null);
      ret.add("Unable to load source image.");
      return ret;
    }

    // fetching remaining settings
    String outFileName = setFileExtension(tfOutput.getText(), "TIS");
    int maxTileCount = (srcImage.getWidth()*srcImage.getHeight()) / 4096;
    int tileCount = Math.max(1, Math.min(getTileCount(), maxTileCount));
    int tisVersion = getTisVersion();

    if (tisVersion == 2) {
      // TIS V2 conversion
      convertV2(this, srcImage, outFileName, tileCount, ret, true);
    } else {
      // TIS V1 conversion
      convertV1(this, srcImage, outFileName, tileCount, ret, true);
    }

    return ret;
  }


// -------------------------- INNER CLASSES --------------------------

  private static class TileEntry
  {
    public int tileIndex;
    public int page;
    public int x, y;

    public static Comparator<TileEntry> CompareByIndex = new Comparator<TileEntry>() {
      @Override
      public int compare(TileEntry te1, TileEntry te2)
      {
        return te1.tileIndex - te2.tileIndex;
      }
    };

    public TileEntry(int index, int page, int x, int y)
    {
      this.tileIndex = index;
      this.page = page;
      this.x = x;
      this.y = y;
    }
  }
}
