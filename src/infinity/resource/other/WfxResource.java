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
import infinity.gui.hexview.HexViewer;
import infinity.resource.AbstractStruct;
import infinity.resource.HasViewerTabs;
import infinity.resource.Resource;
import infinity.resource.key.ResourceEntry;

public final class WfxResource extends AbstractStruct implements Resource, HasViewerTabs
{
  private static final String s_flag[] = {"No flags set", "Cutscene audio", "Alternate SR curve",
                                          "Pitch variance", "Volume variance", "Disable environmental effects"};

  private HexViewer hexViewer;

  public WfxResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 4, "Signature"));
    list.add(new TextString(buffer, offset + 4, 4, "Version"));
    list.add(new DecNumber(buffer, offset + 8, 4, "SR curve radius"));
    list.add(new Flag(buffer, offset + 12, 4, "Flags", s_flag));
    list.add(new DecNumber(buffer, offset + 16, 4, "Pitch variation"));
    list.add(new DecNumber(buffer, offset + 20, 4, "Volume variation"));
    list.add(new Unknown(buffer, offset + 24, 240));
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
}

