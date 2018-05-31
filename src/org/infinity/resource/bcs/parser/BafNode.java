// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.bcs.parser;

//import java.util.Deque;
//import java.util.Iterator;
//import java.util.LinkedList;
//import java.util.NoSuchElementException;

/**
 * <b>Note:</b> Setting option NODE_CLASS in .jjt file tells the compiler to use a custom
 * Node class in favor of the default class {@link SimpleNode}.
 */
public class BafNode extends SimpleNode
{
  public BafNode(int id)
  {
    super(id);
  }

  public BafNode(BafParser p, int id)
  {
    super(p, id);
  }

//  /**
//   * Returns an iterator over all elements starting with this node. The nodes are traversed in
//   * pre-order.
//   * @return an iterator over this node and all child elements in pre-order sequence.
//   */
//  public Iterator<BafNode> iterator()
//  {
//    return iterator(null);
//  }

//  /**
//   * Returns an iterator over all elements matching the specified string, starting with this node.
//   * The nodes are traversed in pre-order.
//   * @param match The filter string. Specify {@code null} to include all available nodes.
//   * @return an iterator over this node and all child elements in pre-order sequence
//   *         matching the specified string.
//   */
//  public Iterator<BafNode> iterator(String match)
//  {
//    return new NodeIterator(this, match);
//  }

//  /**
//   * Returns the first available node with the specified name, starting from this node.
//   * Returns {@code null} otherwise.
//   */
//  public BafNode findNextNode(String name)
//  {
//    BafNode retVal = null;
//    if (toString().equals(name)) {
//      retVal = this;
//    } else {
//      for (int i = 0, cnt = jjtGetNumChildren(); i < cnt && retVal == null; i++) {
//        retVal = ((BafNode)jjtGetChild(i)).findNextNode(name);
//      }
//    }
//    return retVal;
//  }

  /**
   * Returns the content of all tokens associated with the node.
   */
  public String getTokenString()
  {
    StringBuilder sb = new StringBuilder();
    Token token1 = jjtGetFirstToken();
    Token token2 = jjtGetLastToken();
    while (token1 != null) {
      sb.append(token1.toString());
      if (token1 != token2) {
        token1 = token1.next;
      } else {
        token1 = null;
      }
    }
    return sb.toString();
  }

//-------------------------- INNER CLASSES --------------------------

//  public static class NodeIterator implements Iterator<BafNode>
//  {
//    private final Deque<BafNode> sequence;
//
//    private NodeIterator(BafNode rootNode)
//    {
//      this(rootNode, null);
//    }
//
//    private NodeIterator(BafNode rootNode, String name)
//    {
//      if (rootNode == null) {
//        throw new NullPointerException();
//      }
//      this.sequence = new LinkedList<>();
//      createNodeSequence(this.sequence, rootNode, name);
//    }
//
//    // Traverses the whole tree starting from the specified node and adds every matching node
//    // into the given list.
//    private static void createNodeSequence(Deque<BafNode> list, BafNode node, String match)
//    {
//      if (match == null || match.equals(node.toString())) {
//        list.add(node);
//      }
//
//      for (int i = 0, cnt = node.jjtGetNumChildren(); i < cnt; i++) {
//        createNodeSequence(list, (BafNode)node.jjtGetChild(i), match);
//      }
//    }
//
//    @Override
//    public boolean hasNext()
//    {
//      return (sequence.peek() != null);
//    }
//
//    @Override
//    public BafNode next()
//    {
//      BafNode retVal = sequence.poll();
//      if (retVal != null) {
//        return retVal;
//      } else {
//        throw new NoSuchElementException();
//      }
//    }
//  }
}
