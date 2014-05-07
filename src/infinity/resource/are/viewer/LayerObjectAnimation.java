// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.io.File;

import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.ResourceRef;
import infinity.datatype.TextString;
import infinity.gui.layeritem.AbstractLayerItem;
import infinity.gui.layeritem.AnimatedLayerItem;
import infinity.gui.layeritem.BasicAnimationProvider;
import infinity.gui.layeritem.IconLayerItem;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.ResourceFactory;
import infinity.resource.are.Animation;
import infinity.resource.are.AreResource;
import infinity.resource.are.viewer.icon.ViewerIcons;
import infinity.resource.graphics.BamDecoder;
import infinity.resource.graphics.ColorConvert;
import infinity.resource.graphics.PseudoBamDecoder;
import infinity.resource.key.FileResourceEntry;
import infinity.resource.key.ResourceEntry;
import infinity.util.DynamicArray;

/**
 * Handles specific layer type: ARE/Background Animation
 * @author argent77
 */
public class LayerObjectAnimation extends LayerObject
{
  private static final Image[][] Icon = new Image[][]{
    // active versions
    {Icons.getImage(ViewerIcons.class, "itm_Anim1.png"), Icons.getImage(ViewerIcons.class, "itm_Anim2.png")},
    {Icons.getImage(ViewerIcons.class, "itm_AnimWBM1.png"), Icons.getImage(ViewerIcons.class, "itm_AnimWBM2.png")},
    {Icons.getImage(ViewerIcons.class, "itm_AnimPVRZ1.png"), Icons.getImage(ViewerIcons.class, "itm_AnimPVRZ2.png")},
    {Icons.getImage(ViewerIcons.class, "itm_AnimBAM1.png"), Icons.getImage(ViewerIcons.class, "itm_AnimBAM2.png")},
    // inactive versions
    {Icons.getImage(ViewerIcons.class, "itm_Anim1_bw.png"), Icons.getImage(ViewerIcons.class, "itm_Anim2_bw.png")},
    {Icons.getImage(ViewerIcons.class, "itm_AnimWBM1_bw.png"), Icons.getImage(ViewerIcons.class, "itm_AnimWBM2_bw.png")},
    {Icons.getImage(ViewerIcons.class, "itm_AnimPVRZ1_bw.png"), Icons.getImage(ViewerIcons.class, "itm_AnimPVRZ2_bw.png")},
    {Icons.getImage(ViewerIcons.class, "itm_AnimBAM1_bw.png"), Icons.getImage(ViewerIcons.class, "itm_AnimBAM2_bw.png")}
  };
  private static Point Center = new Point(16, 17);

  private final Animation anim;
  private final Point location = new Point();
  private final AbstractLayerItem[] items = new AbstractLayerItem[2];

  private Flag scheduleFlags;


  public LayerObjectAnimation(AreResource parent, Animation anim)
  {
    super(ViewerConstants.RESOURCE_ARE, "Animation", Animation.class, parent);
    this.anim = anim;
    init();
  }

  @Override
  public void close()
  {
    super.close();
    // removing cached references
    for (int i = 0; i < items.length; i++) {
      if (items[i] != null) {
        Object key = items[i].getData();
        if (key != null) {
          switch (i) {
            case ViewerConstants.ANIM_ITEM_ICON:
              SharedResourceCache.remove(SharedResourceCache.Type.Icon, key);
              break;
            case ViewerConstants.ANIM_ITEM_REAL:
              SharedResourceCache.remove(SharedResourceCache.Type.Animation, key);
              break;
          }
        }
      }
    }
  }

  @Override
  public AbstractStruct getStructure()
  {
    return anim;
  }

  @Override
  public AbstractStruct[] getStructures()
  {
    return new AbstractStruct[]{anim};
  }

  @Override
  public AbstractLayerItem getLayerItem()
  {
    return items[0];
  }

  /**
   * Returns the layer item of the specific state. (either ANIM_ITEM_ICON or ANIM_ITEM_REAL).
   * @param type The state of the item to be returned.
   * @return The desired layer item, or <code>null</code> if not available.
   */
  @Override
  public AbstractLayerItem getLayerItem(int type)
  {
    type = (type == ViewerConstants.ANIM_ITEM_REAL) ? ViewerConstants.ANIM_ITEM_REAL : ViewerConstants.ANIM_ITEM_ICON;
    return items[type];
  }

  @Override
  public AbstractLayerItem[] getLayerItems()
  {
    return items;
  }

  @Override
  public void reload()
  {
    init();
  }

  @Override
  public void update(double zoomFactor)
  {
    for (int i = 0; i < items.length; i++) {
      if (items[i] != null) {
        items[i].setItemLocation((int)(location.x*zoomFactor + (zoomFactor / 2.0)),
                                 (int)(location.y*zoomFactor + (zoomFactor / 2.0)));
        if (i == ViewerConstants.ANIM_ITEM_REAL) {
          ((AnimatedLayerItem)items[i]).setZoomFactor(zoomFactor);
        }
      }
    }
  }

  @Override
  public Point getMapLocation()
  {
    return location;
  }

  @Override
  public Point[] getMapLocations()
  {
    return new Point[]{location, location};
  }

  @Override
  public boolean isScheduled(int schedule)
  {
    if (schedule >= ViewerConstants.TIME_0 && schedule <= ViewerConstants.TIME_23) {
      return (scheduleFlags.isFlagSet(schedule));
    } else {
      return false;
    }
  }

  /**
   * Sets the lighting condition of the animation. Does nothing if the animation is flagged as
   * self-illuminating.
   * @param dayTime One of the constants: <code>TilesetRenderer.LIGHTING_DAY</code>,
   *                <code>TilesetRenderer.LIGHTING_TWILIGHT</code>, <code>TilesetRenderer.LIGHTING_NIGHT</code>.
   */
  public void setLighting(int dayTime)
  {
    if (items[ViewerConstants.ANIM_ITEM_REAL] != null) {
      AnimatedLayerItem item = (AnimatedLayerItem)items[ViewerConstants.ANIM_ITEM_REAL];
      if (item != null) {
        BasicAnimationProvider provider = item.getAnimation();
        if (provider instanceof BackgroundAnimationProvider) {
          BackgroundAnimationProvider anim = (BackgroundAnimationProvider)provider;
          anim.setLighting(dayTime);
        }
        item.repaint();
      }
    }
  }


  private void init()
  {
    if (anim != null) {
      String keyAnim = "";
      String msg = "";
      int iconIdx = 0;
      BackgroundAnimationProvider animation = null;
      int skippedFrames = 0;
      boolean isActive = true, isBlended = false, isMirrored = false, isSelfIlluminated = false;
      try {
        // PST seems to ignore a couple of animation settings
        boolean isTorment = (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT);
        boolean isEE = (ResourceFactory.getGameID() == ResourceFactory.ID_BGEE ||
                        ResourceFactory.getGameID() == ResourceFactory.ID_BG2EE);

        location.x = ((DecNumber)anim.getAttribute("Location: X")).getValue();
        location.y = ((DecNumber)anim.getAttribute("Location: Y")).getValue();
        Flag flags = (Flag)anim.getAttribute("Appearance");
        isActive = flags.isFlagSet(0);
        isBlended = flags.isFlagSet(1) || isTorment;
        isMirrored = flags.isFlagSet(11);
        isSelfIlluminated = !flags.isFlagSet(2);
        boolean isSynchronized = flags.isFlagSet(4);
        boolean isWBM = false;
        boolean isPVRZ = false;
        if (isEE) {
          isWBM = flags.isFlagSet(13);
          isPVRZ = flags.isFlagSet(15);
          if (flags.isFlagSet(13)) {
            iconIdx = 1;
          } else if (flags.isFlagSet(15)) {
            iconIdx = 2;
          } else {
            iconIdx = 3;
          }
        }
        if (!isActive) {
          iconIdx += 4;   // adjusting to display inactive versions of the icons
        }

        msg = ((TextString)anim.getAttribute("Name")).toString();
        scheduleFlags = ((Flag)anim.getAttribute("Active at"));

        int baseAlpha = ((DecNumber)anim.getAttribute("Translucency")).getValue();
        if (baseAlpha < 0) baseAlpha = 0; else if (baseAlpha > 255) baseAlpha = 255;
        baseAlpha = 255 - baseAlpha;

        // initializing frames
        if (isWBM) {
          // using icon as placeholder
          // generating key from icon hashcode
          keyAnim = String.format(String.format("%1$08x", Icon[iconIdx][0].hashCode()));
          BamDecoder bam = null;
          if (!SharedResourceCache.contains(SharedResourceCache.Type.Animation, keyAnim)) {
            bam = new PseudoBamDecoder(ColorConvert.toBufferedImage(Icon[iconIdx][0], true, false), Center);
            SharedResourceCache.add(SharedResourceCache.Type.Animation, keyAnim, new ResourceAnimation(keyAnim, bam));
          } else {
            SharedResourceCache.add(SharedResourceCache.Type.Animation, keyAnim);
            bam = ((ResourceAnimation)SharedResourceCache.get(SharedResourceCache.Type.Animation, keyAnim)).getData();
          }
          animation = new BackgroundAnimationProvider(bam);
          animation.setBaseAlpha(baseAlpha);
          animation.setActive(isActive);
          animation.setActiveIgnored(Settings.OverrideAnimVisibility);
        } else if (isPVRZ) {
          // using icon as placeholder
          // generating key from icon hashcode
          keyAnim = String.format(String.format("%1$08x", Icon[iconIdx][0].hashCode()));
          BamDecoder bam = null;
          if (!SharedResourceCache.contains(SharedResourceCache.Type.Animation, keyAnim)) {
            bam = new PseudoBamDecoder(ColorConvert.toBufferedImage(Icon[iconIdx][0], true, false), Center);
            SharedResourceCache.add(SharedResourceCache.Type.Animation, keyAnim, new ResourceAnimation(keyAnim, bam));
          } else {
            SharedResourceCache.add(SharedResourceCache.Type.Animation, keyAnim);
            bam = ((ResourceAnimation)SharedResourceCache.get(SharedResourceCache.Type.Animation, keyAnim)).getData();
          }
          animation = new BackgroundAnimationProvider(bam);
          animation.setBaseAlpha(baseAlpha);
          animation.setActive(isActive);
          animation.setActiveIgnored(Settings.OverrideAnimVisibility);
        } else {
          // setting up BAM frames
          String animFile = ((ResourceRef)anim.getAttribute("Animation")).getResourceName();
          if (animFile == null || animFile.isEmpty() || "None".equalsIgnoreCase(animFile)) {
            animFile = "";
          }
          boolean isPartial = flags.isFlagSet(3) && !isTorment;
//          boolean isRandom = flags.isFlagSet(5);
          boolean playAllFrames = flags.isFlagSet(9);   // play all cycles simultaneously
          boolean hasExternalPalette = flags.isFlagSet(10);
          int cycle = ((DecNumber)anim.getAttribute("Animation number")).getValue();
          int frameCount = ((DecNumber)anim.getAttribute("Frame number")).getValue();
          skippedFrames = ((DecNumber)anim.getAttribute("Start delay (frames)")).getValue();
          if (isSynchronized || isTorment) {
            skippedFrames = 0;
          }

          // retrieving external palette (if available)
          int[] palette = null;
          String paletteFile = null;
          if (hasExternalPalette) {
            ResourceRef ref = (ResourceRef)anim.getAttribute("Palette");
            if (ref != null) {
              paletteFile = ref.getResourceName();
              if (paletteFile == null || paletteFile.isEmpty() || "None".equalsIgnoreCase(paletteFile)) {
                paletteFile = "";
              }
              palette = getExternalPalette(paletteFile);
            }
          }

          // generating unique key from BAM filename and optional palette hashcode
          keyAnim = String.format("%1$s", animFile);
          BamDecoder bam = null;
          if (!SharedResourceCache.contains(SharedResourceCache.Type.Animation, keyAnim)) {
            ResourceEntry bamEntry = ResourceFactory.getInstance().getResourceEntry(animFile);
            bam = BamDecoder.loadBam(bamEntry);
            SharedResourceCache.add(SharedResourceCache.Type.Animation, keyAnim, new ResourceAnimation(keyAnim, bam));
          } else {
            SharedResourceCache.add(SharedResourceCache.Type.Animation, keyAnim);
            bam = ((ResourceAnimation)SharedResourceCache.get(SharedResourceCache.Type.Animation, keyAnim)).getData();
          }
          animation = new BackgroundAnimationProvider(bam);
          animation.setPalette(palette);
          animation.setPaletteEnabled(palette != null);
          animation.setActive(isActive);
          animation.setActiveIgnored(Settings.OverrideAnimVisibility);
          animation.setBaseAlpha(baseAlpha);
          animation.setBlended(isBlended);
          BamDecoder.BamControl control = bam.createControl();
          if (cycle < 0) cycle = 0; else if (cycle >= control.cycleCount()) cycle = control.cycleCount() - 1;
          animation.setCycle(cycle);
          animation.setLooping(!isPartial);
          animation.setMirrored(isMirrored);
          animation.setMultiPart(playAllFrames);
          animation.setSelfIlluminated(isSelfIlluminated);
          animation.setFrameCap(isPartial ? frameCount : -1);
          animation.setStartFrame(skippedFrames);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }

      // Using cached icons
      Image[] icon;
      String keyIcon = String.format("%1$s%2$s", SharedResourceCache.createKey(Icon[iconIdx][0]),
                                                 SharedResourceCache.createKey(Icon[iconIdx][1]));
      if (SharedResourceCache.contains(SharedResourceCache.Type.Icon, keyIcon)) {
        icon = ((ResourceIcon)SharedResourceCache.get(SharedResourceCache.Type.Icon, keyIcon)).getData();
        SharedResourceCache.add(SharedResourceCache.Type.Icon, keyIcon);
      } else {
        icon = Icon[iconIdx];
        SharedResourceCache.add(SharedResourceCache.Type.Icon, keyIcon, new ResourceIcon(keyIcon, icon));
      }

      IconLayerItem item1 = new IconLayerItem(location, anim, msg, icon[0], Center);
      item1.setData(keyIcon);
      item1.setName(getCategory());
      item1.setToolTipText(msg);
      item1.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
      item1.setVisible(isVisible());
      items[0] = item1;

      AnimatedLayerItem item2 = new AnimatedLayerItem(location, anim, msg, animation);
      item2.setData(keyAnim);
      item2.setName(getCategory());
      item2.setToolTipText(msg);
      item2.setVisible(false);
      item2.setFrameRate(10.0);
      item2.setAutoPlay(false);
      item2.setFrameColor(AbstractLayerItem.ItemState.NORMAL, new Color(0xA0FF0000, true));
      item2.setFrameWidth(AbstractLayerItem.ItemState.NORMAL, 2);
      item2.setFrameEnabled(AbstractLayerItem.ItemState.NORMAL, false);
      item2.setFrameColor(AbstractLayerItem.ItemState.HIGHLIGHTED, Color.RED);
      item2.setFrameWidth(AbstractLayerItem.ItemState.HIGHLIGHTED, 2);
      item2.setFrameEnabled(AbstractLayerItem.ItemState.HIGHLIGHTED, true);
      items[1] = item2;
    }
  }

  // Loads a palette from the specified BMP resource (only 8-bit standard BMPs supported)
  private int[] getExternalPalette(String bmpFile)
  {
    int[] retVal = null;
    if (bmpFile != null && !bmpFile.isEmpty()) {
      ResourceEntry entry = ResourceFactory.getInstance().getResourceEntry(bmpFile);
      if (entry == null) {
        entry = new FileResourceEntry(new File(bmpFile));
      }
      if (entry != null) {
        try {
          byte[] data = entry.getResourceData();
          if (data != null && data.length > 1078) {
            boolean isBMP = (DynamicArray.getUnsignedShort(data, 0) == 0x4D42);   // 'BM'
            int palOfs = DynamicArray.getInt(data, 0x0e);
            int bpp = DynamicArray.getShort(data, 0x1c);
            if (isBMP && palOfs >= 0x28 && bpp == 8) {
              int ofs = 0x0e + palOfs;
              retVal = new int[256];
              for (int i = 0; i < 256; i++) {
                retVal[i] = DynamicArray.getInt(data, ofs + i*4);
              }
            }
          }
          data = null;
        } catch (Exception e) {
        }
      }
      entry = null;
    }

    if (retVal == null) {
      retVal = new int[0];
    }

    return retVal;
  }

}
