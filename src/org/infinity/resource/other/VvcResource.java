// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.other;

import java.nio.ByteBuffer;

import javax.swing.JComponent;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.TextString;
import org.infinity.datatype.Unknown;
import org.infinity.gui.StructViewer;
import org.infinity.gui.hexview.BasicColorMap;
import org.infinity.gui.hexview.StructHexViewer;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.HasViewerTabs;
import org.infinity.resource.Resource;
import org.infinity.resource.StructEntry;
import org.infinity.resource.graphics.BamResource;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.sound.SoundResource;
import org.infinity.search.SearchOptions;

/**
 * This resource describes visual "spell casting" effects ({@link BamResource BAM}
 * files) with optional sounds ({@link SoundResource WAVC} files). VVCs can be
 * invoked by some script actions (e.g. {@code CreateVisualEffect},
 * {@code CreateVisualEffectObject}) and by some effects (e.g. "Play 3D effect").
 *
 * VVC Files use a 3D co-ordinates system when playing the exact location of VVC
 * animations. Infinity Engine Games are rendered from an isometric angle; this
 * means that the X-Y-Z axis need to be percieved within this isometric frame to
 * properly understand how VVC files will play:
 * <code><pre>
 *     Z   Y
 *  O  |  /
 * /|\ | /
 * _+_ |/
 *      \
 *       \X
 * </pre></code>
 * <ul>
 * <li>X is up or down</li>
 * <li>Y is the distance between the feet of the character and the animation</li>
 * <li>Z is the distance between the head of the character and the animation</li>
 * </ul>
 *
 * @see <a href="https://gibberlings3.github.io/iesdp/file_formats/ie_formats/vvc_v1.htm">
 * https://gibberlings3.github.io/iesdp/file_formats/ie_formats/vvc_v1.htm</a>
 */
public final class VvcResource extends AbstractStruct implements Resource, HasViewerTabs
{
  // VVC-specific field labels
  public static final String VVC_ANIMATION                = "Animation";
  public static final String VVC_SHADOW                   = "Shadow";
  public static final String VVC_DRAWING                  = "Drawing";
  public static final String VVC_COLOR_ADJUSTMENT         = "Color adjustment";
  public static final String VVC_SEQUENCING               = "Sequencing";
  public static final String VVC_POSITION_X               = "Position: X";
  public static final String VVC_POSITION_Y               = "Position: Y";
  public static final String VVC_POSITION_Z               = "Position: Z";
  public static final String VVC_DRAW_ORIENTED            = "Draw oriented";
  public static final String VVC_FRAME_RATE               = "Frame rate";
  public static final String VVC_NUM_ORIENTATIONS         = "# orientations";
  public static final String VVC_PRIMARY_ORIENTATION      = "Primary orientation";
  public static final String VVC_TRAVEL_ORIENTATION       = "Travel orientation";
  public static final String VVC_PALETTE                  = "Palette";
  public static final String VVC_LIGHT_SPOT_WIDTH         = "Light spot width";
  public static final String VVC_LIGHT_SPOT_HEIGHT        = "Light spot height";
  public static final String VVC_LIGHT_SPOT_BRIGHTNESS    = "Light spot brightness";
  public static final String VVC_DURATION                 = "Duration (frames)";
  public static final String VVC_RESOURCE                 = "Resource";
  public static final String VVC_FIRST_ANIMATION_INDEX    = "First animation number";
  public static final String VVC_SECOND_ANIMATION_INDEX   = "Second animation number";
  public static final String VVC_CURRENT_ANIMATION_INDEX  = "Current animation number";
  public static final String VVC_CONTINUOUS_PLAYBACK      = "Continuous playback";
  public static final String VVC_SOUND_STARTING           = "Starting sound";
  public static final String VVC_SOUND_DURATION           = "Duration sound";
  public static final String VVC_ALPHA_MASK               = "Alpha mask";
  public static final String VVC_THIRD_ANIMATION_INDEX    = "Third animation number";
  public static final String VVC_SOUND_ENDING             = "Ending sound";

  public static final String[] s_transparency = {
      "No flags set", "Transparent", "Translucent", "Translucent shadow", "Blended",
      "Mirror X axis", "Mirror Y axis", "Clipped", "Copy from back", "Clear fill",
      "3D blend", "Not covered by wall", "Persist through time stop", "Ignore dream palette",
      "2D blend"};
  public static final String[] s_tint = {
      "No flags set", "Not light source", "Light source", "Internal brightness", "Time stopped", null,
      "Internal gamma", "Non-reserved palette", "Full palette", null, "Dream palette"};
  public static final String[] s_seq = {
      "No flags set", "Looping", "Special lighting", "Modify for height", "Draw animation", "Custom palette",
      "Purgeable", "Not covered by wall", "Mid-level brighten", "High-level brighten"};
  public static final String[] s_face = {"Use current", "Face target", "Follow target", "Follow path",
                                         "Lock orientation"};

  private StructHexViewer hexViewer;

  public VvcResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 4, COMMON_SIGNATURE));
    addField(new TextString(buffer, offset + 4, 4, COMMON_VERSION));
    addField(new ResourceRef(buffer, offset + 8, VVC_ANIMATION, "BAM"));
    addField(new ResourceRef(buffer, offset + 16, VVC_SHADOW, "BAM"));
    addField(new Flag(buffer, offset + 24, 2, VVC_DRAWING, s_transparency));
    addField(new Flag(buffer, offset + 26, 2, VVC_COLOR_ADJUSTMENT, s_tint));
    addField(new Unknown(buffer, offset + 28, 4));
    addField(new Flag(buffer, offset + 32, 4, VVC_SEQUENCING, s_seq));
    addField(new Unknown(buffer, offset + 36, 4));
    addField(new DecNumber(buffer, offset + 40, 4, VVC_POSITION_X));
    addField(new DecNumber(buffer, offset + 44, 4, VVC_POSITION_Y));
    addField(new Bitmap(buffer, offset + 48, 4, VVC_DRAW_ORIENTED, OPTION_NOYES));
    addField(new DecNumber(buffer, offset + 52, 4, VVC_FRAME_RATE));
    addField(new DecNumber(buffer, offset + 56, 4, VVC_NUM_ORIENTATIONS));
    addField(new DecNumber(buffer, offset + 60, 4, VVC_PRIMARY_ORIENTATION));
    addField(new Flag(buffer, offset + 64, 4, VVC_TRAVEL_ORIENTATION, s_face));
    addField(new ResourceRef(buffer, offset + 68, VVC_PALETTE, "BMP"));
    addField(new DecNumber(buffer, offset + 76, 4, VVC_POSITION_Z));
    addField(new DecNumber(buffer, offset + 80, 4, VVC_LIGHT_SPOT_WIDTH));
    addField(new DecNumber(buffer, offset + 84, 4, VVC_LIGHT_SPOT_HEIGHT));
    addField(new DecNumber(buffer, offset + 88, 4, VVC_LIGHT_SPOT_BRIGHTNESS));
    addField(new DecNumber(buffer, offset + 92, 4, VVC_DURATION));
    addField(new ResourceRef(buffer, offset + 96, VVC_RESOURCE, "VVC"));
    addField(new DecNumber(buffer, offset + 104, 4, VVC_FIRST_ANIMATION_INDEX));
    addField(new DecNumber(buffer, offset + 108, 4, VVC_SECOND_ANIMATION_INDEX));
    addField(new DecNumber(buffer, offset + 112, 4, VVC_CURRENT_ANIMATION_INDEX));
    addField(new Bitmap(buffer, offset + 116, 4, VVC_CONTINUOUS_PLAYBACK, OPTION_NOYES));
    addField(new ResourceRef(buffer, offset + 120, VVC_SOUND_STARTING, "WAV"));
    addField(new ResourceRef(buffer, offset + 128, VVC_SOUND_DURATION, "WAV"));
    addField(new ResourceRef(buffer, offset + 136, VVC_ALPHA_MASK, "BAM"));
    addField(new DecNumber(buffer, offset + 144, 4, VVC_THIRD_ANIMATION_INDEX));
    addField(new ResourceRef(buffer, offset + 148, VVC_SOUND_ENDING, "WAV"));
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
      hexViewer = new StructHexViewer(this, new BasicColorMap(this, true));
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

  /**
   * Called by "Extended Search"
   * Checks whether the specified resource entry matches all available search options.
   */
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
          StructEntry struct = vvc.getAttribute(SearchOptions.getResourceName(key), false);
          retVal &= SearchOptions.Utils.matchResourceRef(struct, o, false);
        }

        String[] keyList = new String[]{SearchOptions.VVC_Flags, SearchOptions.VVC_ColorAdjustment,
                                        SearchOptions.VVC_Sequencing, SearchOptions.VVC_Orientation};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            StructEntry struct = vvc.getAttribute(SearchOptions.getResourceName(key), false);
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
