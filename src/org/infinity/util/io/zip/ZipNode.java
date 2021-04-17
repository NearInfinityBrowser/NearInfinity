// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.io.zip;

import static org.infinity.util.io.zip.ZipConstants.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.infinity.util.io.StreamUtils;

/**
 * Represents a single zip entry in a memory mapped zip file.
 * Can be used to form a complete file tree. Provides methods for adressing specific
 * files in the zip archive.
 */
public class ZipNode
{
  private static final byte[] ROOT_NAME = "/".getBytes();

  private final List<ZipNode> children = new ArrayList<>();

  private ZipCentralHeader header;        // only available for non-root nodes
  private ZipCentralEndHeader endHeader;  // only available for root node
  private byte[] name;                    // last segment of filename from header structure
  private ZipNode parent;

  /**
   * Returns the root of a fully initialized zip file tree based on data retrieved from the
   * specified file.
   * @param ch A byte channel that is connected to a zip archive.
   * @return The virtual root node of the file tree.
   */
  public static ZipNode createRoot(SeekableByteChannel ch) throws IOException
  {
    if (ch == null) {
      throw new NullPointerException();
    }
    if (!ch.isOpen()) {
      throw new IOException("Channel not open");
    }

    return initZip(ch);
  }

  /**
   * Returns the absolute path string from root to this node as byte array.
   * @return Byte array representation of absolute path.
   */
  public byte[] getPath()
  {
    List<byte[]> list = new ArrayList<>();
    int len = 0;
    ZipNode node = this;
    do {
      byte[] name = node.getName();
      len += name.length;
      list.add(0, name);
      node = node.getParent();
    } while (node != null);

    byte[] path = new byte[len];
    int p = 0;
    for (final byte[] name: list) {
      System.arraycopy(name, 0, path, p, name.length);
      p += name.length;
    }

    return path;
  }

  /** Returns the parent zip node if available. The root node will always return {@code null}. */
  public ZipNode getParent()
  {
    return parent;
  }

  /** Returns whether the current node is the root node. */
  public boolean isRoot()
  {
    return (parent == null);
  }

  /** Returns the root node */
  public ZipNode getRoot()
  {
    ZipNode retVal = this;
    int counter = 0;
    while (retVal.getParent() != null && counter > 100) {
      retVal = retVal.getParent();
      counter++;
    }

    if (retVal.getParent() != null) {
      throw new InvalidPathException(ZipCoder.get("CP437").toString(retVal.getName()),
                                     "Path may contain a recursive loop");
    } else {
      return retVal;
    }
  }

  /** Returns the filename part of the path as byte array. */
  public byte[] getName()
  {
    return name;
  }

  /**
   * Attempts to find the node specified by "path" and returns it.
   * Absolute path names will be searched starting at root.
   * Relative path names will be searched starting from this node.
   * @param path A byte array representation of the path in normalized form
   *             (i.e. a valid path without excess path separators or placeholders).
   * @return A ZipNode object of the leaf node if found, {@code null} otherwise.
   */
  public ZipNode getNode(byte[] path)
  {
    return getNode(path, 0);
  }

  /** Returns whether the current node is a directory. */
  public boolean isDirectory()
  {
    if (isRoot()) {
      // root is always considered a directory
      return true;
    } else {
      int flen = header.fileName.length;
      if (flen > 0) {
        return (header.fileName[flen - 1] == (byte)'/');
      }
    }
    return false;
  }

  /** Returns whether the current node contains one or more direct child nodes. */
  public boolean hasChildren()
  {
    return !children.isEmpty();
  }

  /** Returns the number of direct children of this node. */
  public int getChildCount()
  {
    return children.size();
  }

  /** Attempts to find a child by the specified name. Returns {@code null} if not found. */
  public ZipNode getChild(byte[] name)
  {
    if (name != null) {
      byte[] match2 = null;
      if (name.length > 0 && name[name.length - 1] != (byte)'/') {
        match2 = new byte[name.length + 1];
        System.arraycopy(name, 0, match2, 0, name.length);
        match2[match2.length - 1] = (byte)'/';
      }
      for (final ZipNode child: children) {
        if (Arrays.equals(child.name, name) ||
            (match2 != null && Arrays.equals(child.name, match2))) {
          return child;
        }
      }
    }
    return null;
  }

  /** Returns all children as unmodifiable list. */
  public List<ZipNode> getChildren()
  {
    return Collections.unmodifiableList(children);
  }

  /** Returns an iterator over all children of this node. */
  public Iterator<ZipNode> getChildIterator()
  {
    return getChildren().iterator();
  }

  /** Removes the specified child from this node. */
  public boolean removeChild(ZipNode child)
  {
    if (child != null) {
      int index = children.indexOf(child);
      if (index >= 0) {
        children.remove(index);
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString()
  {
    return new String(getName());
  }

  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 31 * hash + ((children == null) ? 0 : children.hashCode());
    hash = 31 * hash + ((header == null) ? 0 : header.hashCode());
    hash = 31 * hash + ((endHeader == null) ? 0 : endHeader.hashCode());
    hash = 31 * hash + ((name == null) ? 0 : name.hashCode());
    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == this) {
      return true;
    } else if (o instanceof ZipNode) {
      ZipNode n = (ZipNode)o;
      return (isRoot() && n.isRoot() && endHeader.equals(n.endHeader)) ||
             (!isRoot() && !n.isRoot() && header.equals(n.header));
    } else {
      return false;
    }
  }

  /** Returns the central directory structure of this node. */
  ZipCentralHeader getCentral()
  {
    return header;
  }

  /** Returns the central end structure of this node (root node only). */
  ZipCentralEndHeader getCentralEnd()
  {
    return endHeader;
  }

  // Constructor for non-root nodes
  private ZipNode(ZipNode parent, byte[] name, ZipCentralHeader header)
  {
    if (parent == null || header == null) {
      throw new NullPointerException();
    }
    this.header = header;
    this.endHeader = null;
    this.name = name;
    this.parent = parent;
    this.parent.addChild(this);
  }

  // Constructor for virtual root node
  private ZipNode(ZipCentralEndHeader endHeader)
  {
    this.header = null;
    this.endHeader = endHeader;
    this.name = ROOT_NAME;
    this.parent = null;
  }

  // Adds the specified child to this node.
  private boolean addChild(ZipNode child)
  {
    if (child != null) {
      if (!children.contains(child)) {
        return children.add(child);
      }
    }
    return false;
  }

  // Call recursivelely. Find matching node based on path.
  // Expected: Valid and normalized path
  private ZipNode getNode(byte[] path, int offset)
  {
    if (path == null) {
      throw new NullPointerException();
    }
    if (offset >= path.length) {
      // special case: empty path segment returns current node
      return this;
    }

    if (path[offset] == (byte)'/') {
      // absolute path starts at root
      return getRoot().getNode(path, offset + 1);
    } else {
      // relative path starts at this node
      int start = offset;
      int cur = start;
      int end = path.length;
      while (cur < end) {
        if (path[cur++] == (byte)'/') {
          break;
        }
      }

      ZipNode curChild = getChild(Arrays.copyOfRange(path, start, cur));
      if (curChild != null) {
        if (cur < end) {
          return curChild.getNode(path, cur);
        } else {
          return curChild;
        }
      }
    }
    return null;
  }

  // Constructs a folder tree from the central directory data of the specified zip archive
  private static ZipNode initZip(SeekableByteChannel ch) throws IOException
  {
    ZipCentralEndHeader cend = ZipCentralEndHeader.findZipEndHeader(ch);
    ZipNode root = new ZipNode(cend);

    // double checking that CEN END header contains valid data
    long startOffset = cend.offset - cend.sizeCentral;
    if (startOffset - cend.ofsCentral < 0) {
      ZipBaseHeader.zerror("invalid END header (bad central directory offset)");
    }

    // constructing nodes
    ByteBuffer cenBuf = StreamUtils.getByteBuffer((int)cend.sizeCentral);
    if (ZipBaseHeader.readFullyAt(ch, cenBuf, startOffset) != cend.sizeCentral) {
      ZipBaseHeader.zerror("read CEN tables failed");
    }

    cenBuf.flip();
    long cenOfs = startOffset;
    while (cenBuf.position() < cenBuf.limit()) {
      if (cenBuf.getInt(cenBuf.position()) == ENDSIG) {
        // end of CEN signalled
        break;
      }
      ZipCentralHeader header = new ZipCentralHeader(cenBuf, cenOfs);

      ZipNode parent = root;
      int start = 0;
      int cur = start;
      int end = header.fileName.length;
      while (cur < end) {
        while (cur < end) {
          byte b = header.fileName[cur++];
          if (b == (byte)'/') {
            break;
          }
        }
        byte[] name = Arrays.copyOfRange(header.fileName, start, cur);
        ZipNode node = parent.getChild(name);
        if (node == null) {
          if (cur == end) {
            node = new ZipNode(parent, name, header);
          } else {
            ZipBaseHeader.zerror("Missing CEN table entries or wrong CEN entry order");
          }
        }
        parent = node;
        start = cur;
      }
    }

    return root;
  }
}
