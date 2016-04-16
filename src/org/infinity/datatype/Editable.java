// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.awt.event.ActionListener;

import javax.swing.JComponent;

import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;

public interface Editable extends StructEntry
{
  JComponent edit(ActionListener container);

  void select();

  boolean updateValue(AbstractStruct struct);
}

