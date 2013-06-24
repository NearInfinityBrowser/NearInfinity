// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.vef;

import infinity.datatype.*;
import infinity.resource.*;
import infinity.resource.key.ResourceEntry;

public final class VefResource extends AbstractStruct implements Resource, HasAddRemovable
{
  public VefResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  public AddRemovable[] getAddRemovables() throws Exception

  {
    return new AddRemovable[]{new Component1(), new Component2()};
  }

// --------------------- End Interface HasAddRemovable ---------------------

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 4, "Signature"));
    list.add(new TextString(buffer, offset + 4, 4, "Version"));
    SectionOffset offset_component1 = new SectionOffset(buffer, offset + 8, "Component1 offset", Component1.class);
    list.add(offset_component1);
    SectionCount count_component1 = new SectionCount(buffer, offset + 12, 4, "Component1 count", Component1.class);
    list.add(count_component1);
    SectionOffset offset_component2 = new SectionOffset(buffer, offset + 16, "Component2 offset", Component2.class);
    list.add(offset_component2);
    SectionCount count_component2 = new SectionCount(buffer, offset + 20, 4, "Component2 count", Component2.class);
    list.add(count_component2);

    offset = offset_component1.getValue();
    for (int i = 0; i < count_component1.getValue(); i++) {
      Component1 comp1 = new Component1(this, buffer, offset);
      offset = comp1.getEndOffset();
      list.add(comp1);
    }

    offset = offset_component2.getValue();
    for (int i = 0; i < count_component2.getValue(); i++) {
      Component2 comp2 = new Component2(this, buffer, offset);
      offset = comp2.getEndOffset();
      list.add(comp2);
    }

    int endoffset = offset;
    for (int i = 0; i < list.size(); i++) {
      StructEntry entry = list.get(i);
      if (entry.getOffset() + entry.getSize() > endoffset)
        endoffset = entry.getOffset() + entry.getSize();
    }
    return endoffset;
  }
}
