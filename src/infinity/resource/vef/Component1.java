package infinity.resource.vef;

import infinity.resource.AbstractStruct;

final class Component1 extends CompBase
{
  Component1() throws Exception
  {
    super("Component1");
  }

  Component1(AbstractStruct superStruct, byte[] buffer, int offset) throws Exception
  {
    super(superStruct, buffer, offset, "Component1");
  }
}
