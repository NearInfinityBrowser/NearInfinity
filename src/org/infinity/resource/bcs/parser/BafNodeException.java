// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.bcs.parser;

/**
 * Extends {@link ParseException} by another constructor.
 */
public class BafNodeException extends ParseException
{
  public BafNodeException(Token token)
  {
    super();
    this.currentToken = token;
  }

  public BafNodeException(String message, Token token)
  {
    super(message);
    this.currentToken = token;
  }
}
