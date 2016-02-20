// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.other;

import javax.swing.JComponent;

import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.gui.StructViewer;
import infinity.gui.hexview.BasicColorMap;
import infinity.gui.hexview.StructHexViewer;
import infinity.resource.AbstractStruct;
import infinity.resource.HasViewerTabs;
import infinity.resource.Resource;
import infinity.resource.key.ResourceEntry;

public final class WfxResource extends AbstractStruct implements Resource, HasViewerTabs
{
  // WFX-specific field labels
  public static final String WFX_CURVE_RADIUS     = "SR curve radius";
  public static final String WFX_FLAGS            = "Flags";
  public static final String WFX_VARIATION_PITCH  = "Pitch variation";
  public static final String WFX_VARIATION_VOLUME = "Volume variation";

  private static final String[] s_flag = {"No flags set", "Cutscene audio", "Alternate SR curve",
                                          "Pitch variance", "Volume variance", "Disable environmental effects"};

  private StructHexViewer hexViewer;

  public WfxResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 4, COMMON_SIGNATURE));
    addField(new TextString(buffer, offset + 4, 4, COMMON_VERSION));
    addField(new DecNumber(buffer, offset + 8, 4, WFX_CURVE_RADIUS));
    addField(new Flag(buffer, offset + 12, 4, WFX_FLAGS, s_flag));
    addField(new DecNumber(buffer, offset + 16, 4, WFX_VARIATION_PITCH));
    addField(new DecNumber(buffer, offset + 20, 4, WFX_VARIATION_VOLUME));
    addField(new Unknown(buffer, offset + 24, 240));
    return offset + 264;
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
}

