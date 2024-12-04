// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.bcs.parser;

/**
 * <b>Note:</b> Setting option NODE_CLASS in .jjt file tells the compiler to use a custom Node class in favor of the
 * default class {@link SimpleNode}.
 */
public class BafNode extends SimpleNode {
  public BafNode(int id) {
    super(id);
  }

  public BafNode(BafParser p, int id) {
    super(p, id);
  }

  /**
   * Returns the content of all tokens associated with the node.
   */
  public String getTokenString() {
    StringBuilder sb = new StringBuilder();
    Token token1 = jjtGetFirstToken();
    Token token2 = jjtGetLastToken();
    while (token1 != null) {
      sb.append(token1);
      if (token1 != token2) {
        token1 = token1.next;
      } else {
        token1 = null;
      }
    }
    return sb.toString();
  }
}
