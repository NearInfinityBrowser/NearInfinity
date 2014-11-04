// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.other;

import javax.swing.JComponent;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.ResourceRef;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.gui.StructViewer;
import infinity.gui.hexview.BasicColorMap;
import infinity.gui.hexview.HexViewer;
import infinity.resource.AbstractStruct;
import infinity.resource.HasViewerTabs;
import infinity.resource.Resource;
import infinity.resource.StructEntry;
import infinity.resource.key.ResourceEntry;
import infinity.search.SearchOptions;

public final class VvcResource extends AbstractStruct implements Resource, HasViewerTabs
{
  public static final String s_transparency[] = {"No flags set", "Transparent", "Translucent", "Translucent shadow", "Blended",
                                                 "Mirror X axis", "Mirror Y axis", "Clipped", "Copy from back", "Clear fill",
                                                 "3D blend", "Not covered by wall", "Persist through time stop", "Ignore dream palette",
                                                 "2D blend"};
  public static final String s_tint[] = {"No flags set", "Not light source", "Light source", "Internal brightness", "Time stopped", "",
                                         "Internal gamma", "Non-reserved palette", "Full palette", "", "Dream palette"};
  public static final String s_seq[] = {"No flags set", "Looping", "Special lighting", "Modify for height", "Draw animation", "Custom palette",
                                        "Purgeable", "Not covered by wall", "Mid-level brighten", "High-level brighten"};
  public static final String s_face[] = {"Use current", "Face target", "Follow target", "Follow path", "Lock orientation"};
  public static final String s_noyes[] = {"No", "Yes"};

  private HexViewer hexViewer;

  public VvcResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 4, "Signature"));
    addField(new TextString(buffer, offset + 4, 4, "Version"));
    addField(new ResourceRef(buffer, offset + 8, "Animation", "BAM"));
    addField(new ResourceRef(buffer, offset + 16, "Shadow", "BAM"));
    addField(new Flag(buffer, offset + 24, 2, "Drawing", s_transparency));
    addField(new Flag(buffer, offset + 26, 2, "Color adjustment", s_tint));
    addField(new Unknown(buffer, offset + 28, 4));
    addField(new Flag(buffer, offset + 32, 4, "Sequencing", s_seq));
    addField(new Unknown(buffer, offset + 36, 4));
    addField(new DecNumber(buffer, offset + 40, 4, "Position: X"));
    addField(new DecNumber(buffer, offset + 44, 4, "Position: Y"));
    addField(new Bitmap(buffer, offset + 48, 4, "Draw oriented", s_noyes));
    addField(new DecNumber(buffer, offset + 52, 4, "Frame rate"));
    addField(new DecNumber(buffer, offset + 56, 4, "# orientations"));
    addField(new DecNumber(buffer, offset + 60, 4, "Primary orientation"));
    addField(new Flag(buffer, offset + 64, 4, "Travel orientation", s_face));
    addField(new ResourceRef(buffer, offset + 68, "Palette", "BMP"));
//    addField(new Unknown(buffer, offset + 72, 4));
    addField(new DecNumber(buffer, offset + 76, 4, "Position: Z"));
    addField(new DecNumber(buffer, offset + 80, 4, "Light spot width"));
    addField(new DecNumber(buffer, offset + 84, 4, "Light spot height"));
    addField(new DecNumber(buffer, offset + 88, 4, "Light spot brightness"));
    addField(new DecNumber(buffer, offset + 92, 4, "Duration (frames)"));
    addField(new ResourceRef(buffer, offset + 96, "Resource", "VVC"));
//    addField(new Unknown(buffer, offset + 100, 4));
    addField(new DecNumber(buffer, offset + 104, 4, "First animation number"));
    addField(new DecNumber(buffer, offset + 108, 4, "Second animation number"));
    addField(new DecNumber(buffer, offset + 112, 4, "Current animation number"));
    addField(new Bitmap(buffer, offset + 116, 4, "Continuous playback", s_noyes));
    addField(new ResourceRef(buffer, offset + 120, "Starting sound", "WAV"));
    addField(new ResourceRef(buffer, offset + 128, "Duration sound", "WAV"));
    addField(new ResourceRef(buffer, offset + 136, "Alpha mask", "BAM"));
//    addField(new Unknown(buffer, offset + 136, 4));
    addField(new DecNumber(buffer, offset + 144, 4, "Third animation number"));
    addField(new ResourceRef(buffer, offset + 148, "Ending sound", "WAV"));
    addField(new Unknown(buffer, offset + 156, 336));
    return offset + 492;
  }

//--------------------- Begin Interface HasViewerTabs ---------------------

  @Override
  public int getViewerTabCount()
  {
    return 1;
  }

  @Override
  public String getViewerTabName(int index)
  {
    return StructViewer.TAB_RAW;
  }

  @Override
  public JComponent getViewerTab(int index)
  {
    if (hexViewer == null) {
      hexViewer = new HexViewer(this, new BasicColorMap(this, true));
    }
    return hexViewer;
  }

  @Override
  public boolean viewerTabAddedBefore(int index)
  {
    return false;
  }

//--------------------- End Interface HasViewerTabs ---------------------

  @Override
  protected void viewerInitialized(StructViewer viewer)
  {
    viewer.addTabChangeListener(hexViewer);
  }

  // Called by "Extended Search"
  // Checks whether the specified resource entry matches all available search options.
  public static boolean matchSearchOptions(ResourceEntry entry, SearchOptions searchOptions)
  {
    if (entry != null && searchOptions != null) {
      try {
        VvcResource vvc = new VvcResource(entry);
        boolean retVal = true;
        String key;
        Object o;

        if (retVal) {
          key = SearchOptions.VVC_Animation;
          o = searchOptions.getOption(key);
          StructEntry struct = vvc.getAttribute(SearchOptions.getResourceName(key));
          retVal &= SearchOptions.Utils.matchResourceRef(struct, o, false);
        }

        String[] keyList = new String[]{SearchOptions.VVC_Flags, SearchOptions.VVC_ColorAdjustment,
                                        SearchOptions.VVC_Sequencing, SearchOptions.VVC_Orientation};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            StructEntry struct = vvc.getAttribute(SearchOptions.getResourceName(key));
            retVal &= SearchOptions.Utils.matchFlags(struct, o);
          } else {
            break;
          }
        }

        keyList = new String[]{SearchOptions.VVC_Custom1, SearchOptions.VVC_Custom2,
                               SearchOptions.VVC_Custom3, SearchOptions.VVC_Custom4};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            retVal &= SearchOptions.Utils.matchCustomFilter(vvc, o);
          } else {
            break;
          }
        }

        return retVal;
      } catch (Exception e) {
      }
    }
    return false;
  }
}

