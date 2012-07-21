// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.resource.AbstractStruct;
import infinity.resource.StructEntry;

import javax.swing.*;
import java.awt.event.*;

public interface Editable extends StructEntry
{
  JComponent edit(ActionListener container);

  void select();

  boolean updateValue(AbstractStruct struct);
}

