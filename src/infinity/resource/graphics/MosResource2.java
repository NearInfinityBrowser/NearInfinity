// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import infinity.NearInfinity;
import infinity.gui.ButtonPopupMenu;
import infinity.gui.WindowBlocker;
import infinity.icon.Icons;
import infinity.resource.Closeable;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.ViewableContainer;
import infinity.resource.key.ResourceEntry;
import infinity.search.ReferenceSearcher;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class MosResource2 implements Resource, ActionListener, Closeable
{
  // max. number of recommended active threads
  private static final int THREADS_MAX = (int)(Math.ceil(Runtime.getRuntime().availableProcessors() * 1.25));

  private final ResourceEntry entry;
  private BufferedImage image;
  private MosDecoder decoder;
  private ButtonPopupMenu mnuExport;
  private JMenuItem miExport, miExport2, miExportBMP;
  private JButton btnFind;
  private JPanel panel;
  private boolean compressed;

  public MosResource2(ResourceEntry entry)
  {
    this.entry = entry;

    WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    try {
      blocker.setBlocked(true);
      decoder = new MosDecoder(entry);
      setImage();
      blocker.setBlocked(false);
    } catch (Exception e) {
      blocker.setBlocked(false);
      e.printStackTrace();
    }
  }

//--------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == btnFind) {
      new ReferenceSearcher(entry, panel.getTopLevelAncestor());
    } else if (event.getSource() == miExport) {
      ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
    } else if (event.getSource() == miExport2) {
      try {
        byte[] data = entry.getResourceData();
        if (compressed) {
          data = Compressor.decompress(data);
        } else {
          data = Compressor.compress(data, "MOSC", "V1  ");
        }
        ResourceFactory.getInstance().exportResource(entry, data, entry.toString(),
                                                     panel.getTopLevelAncestor());
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else if (event.getSource() == miExportBMP) {
      try {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        String fileName = entry.toString().replace(".MOS", ".BMP");
        if (ImageIO.write(image, "bmp", os)) {
          ResourceFactory.getInstance().exportResource(entry,
              os.toByteArray(),
              fileName, panel.getTopLevelAncestor());
        } else {
          JOptionPane.showMessageDialog(panel.getTopLevelAncestor(),
                                        "Error while exporting " + entry, "Error",
                                        JOptionPane.ERROR_MESSAGE);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

//--------------------- End Interface ActionListener ---------------------

//--------------------- Begin Interface Resource ---------------------

  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

//--------------------- End Interface Resource ---------------------

//--------------------- Begin Interface Closeable ---------------------

  public void close() throws Exception
  {
    image = null;
    decoder = null;
    System.gc();    // XXX: There have to be better ways to free resources from memory
  }

//--------------------- End Interface Closeable ---------------------

//--------------------- Begin Interface Viewable ---------------------

  public JComponent makeViewer(ViewableContainer container)
  {
    btnFind = new JButton("Find references...", Icons.getIcon("Find16.gif"));
    btnFind.setMnemonic('f');
    btnFind.addActionListener(this);

    miExport = new JMenuItem("original");
    miExport.setMnemonic('o');
    miExport.addActionListener(this);
    miExport2 = new JMenuItem("compress");
    if (compressed) {
      miExport2.setText("decompress");
      miExport2.setMnemonic('d');
    } else {
      miExport2.setText("compress");
      miExport2.setMnemonic('c');
    }
    miExport2.addActionListener(this);
    miExportBMP = new JMenuItem("as BMP");
    miExportBMP.setMnemonic('b');
    miExportBMP.addActionListener(this);

    mnuExport = new ButtonPopupMenu("Export...", new JMenuItem[]{miExport, miExport2, miExportBMP});
    mnuExport.setIcon(Icons.getIcon("Export16.gif"));
    mnuExport.setMnemonic('e');

    if (ResourceFactory.getGameID() == ResourceFactory.ID_BG2 ||
        ResourceFactory.getGameID() == ResourceFactory.ID_BG2TOB ||
        ResourceFactory.getGameID() == ResourceFactory.ID_BGEE) {
      miExport2.setEnabled(decoder.info().type() == MosDecoder.MosInfo.MosType.PALETTE);
    } else {
      miExport2.setEnabled(false);
    }

    JScrollPane scroll = new JScrollPane(new JLabel(new ImageIcon(image)));
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.getHorizontalScrollBar().setUnitIncrement(16);

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    buttonPanel.add(btnFind);
    buttonPanel.add(mnuExport);

    panel = new JPanel(new BorderLayout());
    panel.add(scroll, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.PAGE_END);
    scroll.setBorder(BorderFactory.createLoweredBevelBorder());

    return panel;
  }

//--------------------- End Interface Viewable ---------------------

  public BufferedImage getImage()
  {
    return image;
  }

  public MosDecoder getDecoder()
  {
    return decoder;
  }

  private void setImage() throws Exception
  {
    if (decoder != null) {
      compressed = decoder.info().isCompressed();

      if (decoder.info().blockCount() > 0) {
        ColorConvert.ColorFormat outputFormat = ColorConvert.ColorFormat.R8G8B8;
        image = new BufferedImage(decoder.info().width(), decoder.info().height(), BufferedImage.TYPE_INT_RGB);
        int threadsRunning = 0;
        int blocksStarted = 0;
        int blocksFinished = 0;
        LinkedBlockingQueue<BlockInfo> queue = new LinkedBlockingQueue<BlockInfo>();
        while (blocksFinished < decoder.info().blockCount()) {
          // starting a couple of threads
          while (blocksStarted < decoder.info().blockCount() && threadsRunning < THREADS_MAX) {
            (new Thread(new BlockDecoder(queue, decoder, new BlockInfo(blocksStarted), outputFormat))).start();
            blocksStarted++;
            threadsRunning++;
          }

          // drawing decoded blocks if available
          if (queue.peek() != null) {
            BlockInfo info = queue.poll();
            MosDecoder.BlockInfo bi = decoder.info().blockInfo(info.index);
            for (int y = 0; y < bi.height(); y++) {
              for (int x = 0; x < bi.width(); x++) {
                image.setRGB(bi.x() + x, bi.y() + y, info.output[y*bi.width()+x]);
              }
            }
            info.output = null;
            threadsRunning--;
            blocksFinished++;
          }
        }
      } else
        throw new Exception("No image data available");

    } else
      throw new Exception("MOS decoder not initialized");
  }


//-------------------------- INNER CLASSES --------------------------

  // stores information about a single MOS data block
  private static final class BlockInfo
  {
    private final int index;
    private int[] output;

    private BlockInfo(int blockIndex)
    {
      this.index = blockIndex;
      this.output = null;
    }
  }

  // decodes a single data block asynchronously
  private static class BlockDecoder implements Runnable
  {
    private LinkedBlockingQueue<BlockInfo> queue;
    private MosDecoder decoder;
    private BlockInfo blockInfo;
    private ColorConvert.ColorFormat outFormat;

    public BlockDecoder(LinkedBlockingQueue<BlockInfo> queue, MosDecoder decoder, BlockInfo info,
                        ColorConvert.ColorFormat fmt) throws Exception
    {
      if (queue == null || decoder == null || info == null)
        throw new NullPointerException();
      this.queue = queue;
      this.decoder = decoder;
      this.blockInfo = info;
      this.outFormat = fmt;
    }

    public void run()
    {
      try {
        MosDecoder.BlockInfo bi = decoder.info().blockInfo(blockInfo.index);
        blockInfo.output = new int[bi.width()*bi.height()];
        ColorConvert.BufferToColor(outFormat, decoder.decodeBlock(blockInfo.index, outFormat),
                                  0, blockInfo.output, 0, blockInfo.output.length);

        int counter = 50;
        while (counter > 0) {
          try {
            queue.offer(blockInfo, 10, TimeUnit.MILLISECONDS);
            counter = 0;
          } catch (InterruptedException e) {
            counter--;
            if (counter == 0) {
              System.err.println("Error putting MOS data block into queue");
              e.printStackTrace();
            }
          }
        }

      } catch (Exception e) {
        System.err.println("Error decoding MOS data block #" + blockInfo.index);
        e.printStackTrace();
      }
    }

  }
}
