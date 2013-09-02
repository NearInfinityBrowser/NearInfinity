
package infinity.resource.graphics;

import infinity.resource.Resource;
import infinity.resource.Viewable;
import infinity.resource.ViewableContainer;
import infinity.resource.key.ResourceEntry;

import javax.swing.*;

public final class WbmResource implements Resource
{
  private final ResourceEntry entry;

  public WbmResource(ResourceEntry entry)
  {
    this.entry = entry;
  }

// --------------------- Begin Interface Resource ---------------------

  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

// --------------------- End Interface Resource ---------------------

// --------------------- Begin Interface Viewable ---------------------

  public JComponent makeViewer(ViewableContainer container)
  {
    return new JPanel();
  }

// --------------------- End Interface Viewable ---------------------
}
