package infinity.resource.vef;

import infinity.resource.AbstractStruct;

final class Component2 extends CompBase
{
  Component2() throws Exception
  {
    super("Component2");
  }

  Component2(AbstractStruct superStruct, byte[] buffer, int offset) throws Exception
  {
    super(superStruct, buffer, offset, "Component2");
  }
}
