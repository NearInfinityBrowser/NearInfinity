// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import infinity.NearInfinity;
import infinity.gui.ButtonPopupMenu;
import infinity.gui.WindowBlocker;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.key.ResourceEntry;
import infinity.search.ReferenceSearcher;
import infinity.util.ArrayUtil;
import infinity.util.Byteconvert;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

public final class BamResource implements Resource, ActionListener, ItemListener
{
  private static final int ANIMDELAY = 100; // 10.0 fps
  private final ResourceEntry entry;
  private Anim anims[];
  private ButtonPopupMenu bexport;
  private Frame frames[];
  private JButton bnextanim, bprevanim, bnextframe, bprevframe, bfindref;
  private JLabel label, lanim, lframe;
  private JMenuItem iExport, iCompress, iDecompress;
  private JPanel panel;
  private JToggleButton bplay;
  private Palette palette;
  private Timer timer;
  private boolean compressed;
  private byte transparent;
  private int selectedFrame, selectedAnim;
  private int lookupTable[];

  public BamResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    byte buffer[] = entry.getResourceData();
    String signature = new String(buffer, 0, 4);
    if (signature.equals("BAMC")) {
      compressed = true;
      buffer = Compressor.decompress(buffer);
    }
    else if (!signature.equals("BAM "))
      throw new Exception("Unsupported BAM file: " + signature);
    WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    blocker.setBlocked(true);
    try {
      int numberframes = (int)Byteconvert.convertShort(buffer, 0x08);
      int numberanims = (int)Byteconvert.convertUnsignedByte(buffer, 0x0a);
      transparent = buffer[0x0b];
      int frameOffset = Byteconvert.convertInt(buffer, 0x0c);
      int paletteOffset = Byteconvert.convertInt(buffer, 0x10);
      int lookupOffset = Byteconvert.convertInt(buffer, 0x14);

      palette = new Palette(buffer, paletteOffset, lookupOffset - paletteOffset);

      frames = new Frame[numberframes];
      for (int i = 0; i < numberframes; i++)
        frames[i] = new Frame(buffer, frameOffset + 12 * i);

      int animOffset = frameOffset + 12 * numberframes;
      int lookupCount = 0;
      anims = new Anim[numberanims];
      for (int i = 0; i < numberanims; i++) {
        anims[i] = new Anim(buffer, animOffset + 4 * i);
        lookupCount = Math.max(lookupCount, anims[i].getMaxLookup());
      }

      lookupTable = new int[lookupCount];
      for (int i = 0; i < lookupCount; i++)
        lookupTable[i] = (int)Byteconvert.convertShort(buffer, lookupOffset + i * 2);
    } catch (Error err) {
      blocker.setBlocked(false);
      err.printStackTrace();
      throw new Exception("Corrupted BAM - read aborted");
    } catch (Exception e) {
      blocker.setBlocked(false);
      e.printStackTrace();
      throw new Exception("Corrupted BAM - read aborted");
    }
    blocker.setBlocked(false);
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bnextframe) {
      selectedFrame++;
      showFrame();
    }
    else if (event.getSource() == bprevframe) {
      selectedFrame--;
      showFrame();
    }
    else if (event.getSource() == bnextanim) {
      selectedAnim++;
      // stop timer if no frames in current cycle
      if (timer != null && timer.isRunning() && anims[selectedAnim].frameCount == 0) {
        timer.stop();
        bplay.setSelected(false);
      }
      selectedFrame = 0;
      showFrame();
    }
    else if (event.getSource() == bprevanim) {
      selectedAnim--;
      if (timer != null && timer.isRunning() && anims[selectedAnim].frameCount == 0) {
        timer.stop();
        bplay.setSelected(false);
      }
      selectedFrame = 0;
      showFrame();
    }
    else if (event.getSource() == bplay) {
      if (bplay.isSelected()) {
        if (timer == null)
          timer = new Timer(ANIMDELAY, this);
        timer.restart();
      }
      else {
        if (timer != null)
          timer.stop();
      }
    }
    else if (event.getSource() == bfindref)
      new ReferenceSearcher(entry, panel.getTopLevelAncestor());
    else {
      // Timer
      selectedFrame++;
      if (selectedFrame == anims[selectedAnim].frameCount)
        selectedFrame = 0;
      showFrame();
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ItemListener ---------------------

  public void itemStateChanged(ItemEvent event)
  {
    if (event.getSource() == bexport) {
      if (bexport.getSelectedItem() == iExport) {
        ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
      }
      else if (bexport.getSelectedItem() == iDecompress) {
        try {
          byte data[] = Compressor.decompress(entry.getResourceData());
          ResourceFactory.getInstance().exportResource(entry, data, entry.toString(),
                                                       panel.getTopLevelAncestor());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      else if (bexport.getSelectedItem() == iCompress) {
        try {
          byte data[] = Compressor.compress(entry.getResourceData(), "BAMC", "V1  ");
          ResourceFactory.getInstance().exportResource(entry, data, entry.toString(),
                                                       panel.getTopLevelAncestor());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

// --------------------- End Interface ItemListener ---------------------


// --------------------- Begin Interface Resource ---------------------

  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

// --------------------- End Interface Resource ---------------------


// --------------------- Begin Interface Viewable ---------------------

  public JComponent makeViewer(ViewableContainer container)
  {
    label = new JLabel("", JLabel.CENTER);
    lanim = new JLabel("", JLabel.CENTER);
    lframe = new JLabel("", JLabel.CENTER);
    bnextanim = new JButton(Icons.getIcon("Forward16.gif"));
    bprevanim = new JButton(Icons.getIcon("Back16.gif"));
    bnextframe = new JButton(Icons.getIcon("Forward16.gif"));
    bprevframe = new JButton(Icons.getIcon("Back16.gif"));
    bplay = new JToggleButton("Play", Icons.getIcon("Play16.gif"));
    bnextanim.setMargin(new Insets(bnextanim.getMargin().top, 2, bnextanim.getMargin().bottom, 2));
    bprevanim.setMargin(bnextanim.getMargin());
    bnextframe.setMargin(bnextanim.getMargin());
    bprevframe.setMargin(bnextanim.getMargin());
    iExport = new JMenuItem("original");
    iDecompress = new JMenuItem("decompressed");
    iCompress = new JMenuItem("compressed");
    bfindref = new JButton("Find references...", Icons.getIcon("Find16.gif"));
    bfindref.setMnemonic('f');
    bfindref.addActionListener(this);
    bprevanim.addActionListener(this);
    bnextanim.addActionListener(this);
    bprevframe.addActionListener(this);
    bnextframe.addActionListener(this);
    bplay.addActionListener(this);
    iDecompress.setEnabled(false);
    iCompress.setEnabled(false);
    if (ResourceFactory.getGameID() == ResourceFactory.ID_BG2 ||
        ResourceFactory.getGameID() == ResourceFactory.ID_BG2TOB ||
        ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2 ||
        compressed) {
      if (compressed)
        iDecompress.setEnabled(true);
      else
        iCompress.setEnabled(true);
    }
    bexport = new ButtonPopupMenu("Export...", new JMenuItem[]{iExport, iDecompress, iCompress});
    bexport.addItemListener(this);
    bexport.setIcon(Icons.getIcon("Export16.gif"));

    JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    bpanel.add(lanim);
    bpanel.add(bprevanim);
    bpanel.add(bnextanim);
    bpanel.add(lframe);
    bpanel.add(bprevframe);
    bpanel.add(bnextframe);
    bpanel.add(bplay);
    bpanel.add(bfindref);
    bpanel.add(bexport);

    panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(label, BorderLayout.CENTER);
    panel.add(bpanel, BorderLayout.SOUTH);
    label.setBorder(BorderFactory.createLoweredBevelBorder());
    showFrame();
    return panel;
  }

// --------------------- End Interface Viewable ---------------------

  public Image getFrame(int frameNr)
  {
    return frames[frameNr].image;
  }

  public int getFrameNr(int animNr, int frameNr)
  {
    return lookupTable[frameNr + anims[animNr].lookupIndex];
  }

  private void showFrame()
  {
    label.setText("");
    ImageIcon lastimage = (ImageIcon)label.getIcon();
    if (lastimage != null)
      lastimage.getImage().flush();
    label.setIcon(null);

    lanim.setText("Cycle: " + (selectedAnim + 1) + '/' + anims.length);
    lframe.setText("Frame: " + (selectedFrame + 1) + '/' + anims[selectedAnim].frameCount);
    if (anims[selectedAnim].getFrame(selectedFrame) == null)
      label.setText("No image");
    else
      label.setIcon(new ImageIcon(anims[selectedAnim].getFrame(selectedFrame)));

    bplay.setEnabled(anims[selectedAnim].frameCount > 1);
    bnextframe.setEnabled(selectedFrame + 1 < anims[selectedAnim].frameCount);
    bprevframe.setEnabled(selectedFrame > 0);
    bnextanim.setEnabled(selectedAnim + 1 < anims.length);
    bprevanim.setEnabled(selectedAnim > 0);
  }

// -------------------------- INNER CLASSES --------------------------

  private final class Frame
  {
    private BufferedImage image;

    private Frame(byte buffer[], int offset)
    {
      int width = (int)Byteconvert.convertShort(buffer, offset);
      int height = (int)Byteconvert.convertShort(buffer, offset + 0x02);
//      int xcoord = Byteconvert.convertShort(buffer, offset + 0x04);
//      int ycoord = Byteconvert.convertShort(buffer, offset + 0x06);
      long frameDataOffset = Byteconvert.convertUnsignedInt(buffer, offset + 0x08);
      boolean rle = true;
      if (frameDataOffset > Math.pow((double)2, (double)31)) {
        rle = false;
        frameDataOffset -= (long)Math.pow((double)2, (double)31);
      }

      if (height < 1 || width < 1)
        return;
      byte imagedata[] = new byte[height * width];
      if (!rle)
        imagedata = ArrayUtil.getSubArray(buffer, (int)frameDataOffset, imagedata.length);
      else {
        int w_idx = 0;
        while (w_idx < imagedata.length) {
          byte b = buffer[(int)frameDataOffset++];
          imagedata[w_idx++] = b;
          if (b == transparent) {
            int toread = (int)buffer[(int)frameDataOffset++];
            if (toread < 0)
              toread += 256;
            for (int i = 0; i < toread; i++)
              if (w_idx < imagedata.length)
                imagedata[w_idx++] = transparent;
          }
        }
      }
      image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      for (int h_idx = 0; h_idx < height; h_idx++)
        for (int w_idx = 0; w_idx < width; w_idx++)
          image.setRGB(w_idx, h_idx, palette.getColor((int)imagedata[h_idx * width + w_idx]));
    }
  }

  private final class Anim
  {
    private final int frameCount;
    private final int lookupIndex;

    private Anim(byte buffer[], int offset)
    {
      frameCount = (int)Byteconvert.convertShort(buffer, offset);
      lookupIndex = (int)Byteconvert.convertShort(buffer, offset + 0x02);
    }

    private int getMaxLookup()
    {
      return frameCount + lookupIndex;
    }

    private Image getFrame(int i)
    {
      try {
        return frames[lookupTable[i + lookupIndex]].image;
      } catch (Exception e) {
      }
      return null;
    }
  }
}

