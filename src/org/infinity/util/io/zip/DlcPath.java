// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information
//
// ----------------------------------------------------------------------------
//
// Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
//
//   - Redistributions of source code must retain the above copyright
//     notice, this list of conditions and the following disclaimer.
//
//   - Redistributions in binary form must reproduce the above copyright
//     notice, this list of conditions and the following disclaimer in the
//     documentation and/or other materials provided with the distribution.
//
//   - Neither the name of Oracle nor the names of its
//     contributors may be used to endorse or promote products derived
//     from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
// THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
// PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
// CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
// EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package org.infinity.util.io.zip;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Path implementation for DLC archives in zip format.
 */
public class DlcPath implements Path
{
  private final DlcFileSystem dfs;
  private final byte[] path;

  private volatile int[] offsets;

  private int hashcode = 0;  // cached hashcode (created lazily)

  DlcPath(DlcFileSystem dfs, byte[] path)
  {
    this(dfs, path, false);
  }

  DlcPath(DlcFileSystem dfs, byte[] path, boolean normalized)
  {
    this.dfs = dfs;
    if (normalized) {
        this.path = path;
    } else {
        this.path = normalize(path);
    }
  }

  @Override
  public DlcFileSystem getFileSystem()
  {
    return dfs;
  }

  @Override
  public boolean isAbsolute()
  {
    return (this.path.length > 0 && path[0] == '/');
  }

  @Override
  public DlcPath getRoot()
  {
    if (this.isAbsolute()) {
      return new DlcPath(dfs, new byte[] { path[0] });
    } else {
      return null;
    }
  }

  @Override
  public Path getFileName()
  {
    initOffsets();
    int count = offsets.length;
    if (count == 0) {
      return null; // no elements so no name
    }
    if (count == 1 && path[0] != '/') {
      return this;
    }
    int lastOffset = offsets[count - 1];
    int len = path.length - lastOffset;
    byte[] result = new byte[len];
    System.arraycopy(path, lastOffset, result, 0, len);
    return new DlcPath(dfs, result);
  }

  @Override
  public DlcPath getParent()
  {
    initOffsets();
    int count = offsets.length;
    if (count == 0) { // no elements so no parent
      return null;
    }
    int len = offsets[count - 1] - 1;
    if (len <= 0) { // parent is root only (may be null)
      return getRoot();
    }
    byte[] result = new byte[len];
    System.arraycopy(path, 0, result, 0, len);
    return new DlcPath(dfs, result);
  }

  @Override
  public int getNameCount()
  {
    initOffsets();
    return offsets.length;
  }

  @Override
  public DlcPath getName(int index)
  {
    initOffsets();
    if (index < 0 || index >= offsets.length) {
      throw new IllegalArgumentException();
    }
    int begin = offsets[index];
    int len;
    if (index == (offsets.length - 1)) {
      len = path.length - begin;
    } else {
      len = offsets[index + 1] - begin - 1;
    }
    // construct result
    byte[] result = new byte[len];
    System.arraycopy(path, begin, result, 0, len);
    return new DlcPath(dfs, result);
  }

  @Override
  public DlcPath subpath(int beginIndex, int endIndex)
  {
    initOffsets();
    if (beginIndex < 0 ||
        beginIndex >= offsets.length ||
        endIndex > offsets.length ||
        beginIndex >= endIndex) {
      throw new IllegalArgumentException();
    }

    // starting offset and length
    int begin = offsets[beginIndex];
    int len;
    if (endIndex == offsets.length) {
      len = path.length - begin;
    } else {
      len = offsets[endIndex] - begin - 1;
    }
    // construct result
    byte[] result = new byte[len];
    System.arraycopy(path, begin, result, 0, len);
    return new DlcPath(dfs, result);
  }

  @Override
  public boolean startsWith(Path other)
  {
    try {
    final DlcPath o = checkPath(other);
      if (o.isAbsolute() != this.isAbsolute() || o.path.length > this.path.length) {
        return false;
      }
      int olast = o.path.length;
      for (int i = 0; i < olast; i++) {
        if (o.path[i] != this.path[i]) {
          return false;
        }
      }
      olast--;
      return (o.path.length == this.path.length) ||
             (o.path[olast] == '/') ||
             (this.path[olast + 1] == '/');
    } catch (Exception e) {
    }
    return false;
  }

  @Override
  public boolean startsWith(String other)
  {
    return startsWith(getFileSystem().getPath(other));
  }

  @Override
  public boolean endsWith(Path other)
  {
    try {
      final DlcPath o = checkPath(other);
      int olast = o.path.length - 1;
      if (olast > 0 && o.path[olast] == '/') {
        olast--;
      }
      int last = this.path.length - 1;
      if (last > 0 && this.path[last] == '/') {
        last--;
      }
      if (olast == -1) {  // o.path.length == 0
        return last == -1;
      }
      if ((o.isAbsolute() && (!this.isAbsolute() || olast != last)) || (last < olast)) {
        return false;
      }
      for (; olast >= 0; olast--, last--) {
        if (o.path[olast] != this.path[last]) {
          return false;
        }
      }
      return (o.path[olast + 1] == '/') || (last == -1) || (this.path[last] == '/');
    } catch (Exception e) {
    }
    return false;
  }

  @Override
  public boolean endsWith(String other)
  {
    return endsWith(getFileSystem().getPath(other));
  }

  @Override
  public Path normalize()
  {
    byte[] resolved = getResolved();
    if (resolved == path) { // no change
      return this;
    }
    return new DlcPath(dfs, resolved, true);
  }

  @Override
  public DlcPath resolve(Path other)
  {
    final DlcPath o = checkPath(other);
    if (o.isAbsolute()) {
      return o;
    }
    byte[] resolved = null;
    if (this.path[path.length - 1] == '/') {
      resolved = new byte[path.length + o.path.length];
      System.arraycopy(path, 0, resolved, 0, path.length);
      System.arraycopy(o.path, 0, resolved, path.length, o.path.length);
    } else {
      resolved = new byte[path.length + 1 + o.path.length];
      System.arraycopy(path, 0, resolved, 0, path.length);
      resolved[path.length] = '/';
      System.arraycopy(o.path, 0, resolved, path.length + 1, o.path.length);
    }
    return new DlcPath(dfs, resolved);
  }

  @Override
  public DlcPath resolve(String other)
  {
    return resolve(getFileSystem().getPath(other));
  }

  @Override
  public Path resolveSibling(Path other)
  {
    if (other == null) {
      throw new NullPointerException();
    }
    Path parent = getParent();
    return (parent == null) ? other : parent.resolve(other);
  }

  @Override
  public Path resolveSibling(String other)
  {
    return resolveSibling(getFileSystem().getPath(other));
  }

  @Override
  public Path relativize(Path other)
  {
    final DlcPath o = checkPath(other);
    if (o.equals(this)) {
      return new DlcPath(getFileSystem(), new byte[0], true);
    }
    if (/* this.getFileSystem() != o.getFileSystem() || */
        this.isAbsolute() != o.isAbsolute()) {
      throw new IllegalArgumentException();
    }
    int mc = this.getNameCount();
    int oc = o.getNameCount();
    int n = Math.min(mc, oc);
    int i = 0;
    while (i < n) {
      if (!equalsNameAt(o, i)) {
        break;
      }
      i++;
    }
    int dotdots = mc - i;
    int len = dotdots * 3 - 1;
    if (i < oc) {
      len += (o.path.length - o.offsets[i] + 1);
    }
    byte[] result = new byte[len];

    int pos = 0;
    while (dotdots > 0) {
      result[pos++] = (byte) '.';
      result[pos++] = (byte) '.';
      if (pos < len) {  // no tailing slash at the end
        result[pos++] = (byte) '/';
      }
      dotdots--;
    }
    if (i < oc) {
      System.arraycopy(o.path, o.offsets[i], result, pos, o.path.length - o.offsets[i]);
    }
    return new DlcPath(getFileSystem(), result);
  }

  @Override
  public URI toUri()
  {
    try {
      return new URI(DlcFileSystemProvider.SCHEME,
                     dfs.getDlcFile().toUri() + "!" + dfs.getString(toAbsolutePath().path),
                     null);
    } catch (Exception ex) {
      throw new AssertionError(ex);
    }
  }

  @Override
  public DlcPath toAbsolutePath()
  {
    if (isAbsolute()) {
      return this;
    } else {
      // add / bofore the existing path
      byte[] defaultdir = dfs.getDefaultDir().path;
      int defaultlen = defaultdir.length;
      boolean endsWith = (defaultdir[defaultlen - 1] == '/');
      byte[] t = null;
      if (endsWith) {
        t = new byte[defaultlen + path.length];
      } else {
        t = new byte[defaultlen + 1 + path.length];
      }
      System.arraycopy(defaultdir, 0, t, 0, defaultlen);
      if (!endsWith) {
        t[defaultlen++] = '/';
      }
      System.arraycopy(path, 0, t, defaultlen, path.length);
      return new DlcPath(dfs, t, true); // normalized
    }
  }

  @Override
  public DlcPath toRealPath(LinkOption... options) throws IOException
  {
    DlcPath realPath = new DlcPath(dfs, getResolvedPath()).toAbsolutePath();
    realPath.checkAccess();
    return realPath;
  }

  @Override
  public File toFile()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers)
      throws IOException
  {
    if (watcher == null || events == null || modifiers == null) {
      throw new NullPointerException();
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public WatchKey register(WatchService watcher, Kind<?>... events) throws IOException
  {
    return register(watcher, events, new WatchEvent.Modifier[0]);
  }

  @Override
  public Iterator<Path> iterator()
  {
    return new Iterator<Path>() {
      private int i = 0;

      @Override
      public boolean hasNext()
      {
        return (i < getNameCount());
      }

      @Override
      public Path next()
      {
        if (i < getNameCount()) {
          Path result = getName(i);
          i++;
          return result;
        } else {
          throw new NoSuchElementException();
        }
      }

      @Override
      public void remove()
      {
        throw new ReadOnlyFileSystemException();
      }
    };
  }

  @Override
  public int compareTo(Path other)
  {
    final DlcPath o = checkPath(other);
    int len1 = this.path.length;
    int len2 = o.path.length;

    int n = Math.min(len1, len2);
    byte v1[] = this.path;
    byte v2[] = o.path;

    int k = 0;
    while (k < n) {
      int c1 = v1[k] & 0xff;
      int c2 = v2[k] & 0xff;
      if (c1 != c2) {
        return c1 - c2;
      }
      k++;
    }
    return len1 - len2;
  }

  @Override
  public String toString()
  {
    return dfs.getString(path);
  }

  @Override
  public int hashCode()
  {
    int h = hashcode;
    if (h == 0) {
      hashcode = h = Arrays.hashCode(path);
    }
    return h;
  }

  @Override
  public boolean equals(Object obj)
  {
    return (obj != null) &&
           (obj instanceof DlcPath) &&
           (this.dfs == ((DlcPath)obj).dfs) &&
           (compareTo((Path)obj) == 0);
  }

  boolean isHidden()
  {
    return false;
  }

  private DlcPath checkPath(Path path)
  {
    if (path == null) {
      throw new NullPointerException();
    }
    if (!(path instanceof DlcPath)) {
      throw new ProviderMismatchException();
    }
    return (DlcPath)path;
  }

  private boolean equalsNameAt(DlcPath other, int index)
  {
    int mbegin = offsets[index];
    int mlen = 0;
    if (index == (offsets.length - 1)) {
      mlen = path.length - mbegin;
    } else {
      mlen = offsets[index + 1] - mbegin - 1;
    }
    int obegin = other.offsets[index];
    int olen = 0;
    if (index == (other.offsets.length - 1)) {
      olen = other.path.length - obegin;
    } else {
      olen = other.offsets[index + 1] - obegin - 1;
    }
    if (mlen != olen) {
      return false;
    }
    int n = 0;
    while (n < mlen) {
      if (path[mbegin + n] != other.path[obegin + n]) {
        return false;
      }
      n++;
    }
    return true;
  }

  // create offset list if not already created
  private void initOffsets()
  {
    if (offsets == null) {
      int count, index;
      // count names
      count = 0;
      index = 0;
      while (index < path.length) {
        byte c = path[index++];
        if (c != '/') {
          count++;
          while (index < path.length && path[index] != '/') {
            index++;
          }
        }
      }
      // populate offsets
      int[] result = new int[count];
      count = 0;
      index = 0;
      while (index < path.length) {
        byte c = path[index];
        if (c == '/') {
          index++;
        } else {
          result[count++] = index++;
          while (index < path.length && path[index] != '/') {
            index++;
          }
        }
      }
      synchronized (this) {
        if (offsets == null) {
          offsets = result;
        }
      }
    }
  }

  // resolved path for locating zip entry inside the zip file,
  // the result path does not contain ./ and .. components
  private volatile byte[] resolved = null;
  byte[] getResolvedPath()
  {
    byte[] r = resolved;
    if (r == null) {
      if (isAbsolute()) {
        r = getResolved();
      } else {
        r = toAbsolutePath().getResolvedPath();
      }
      if (r[0] == '/') {
        r = Arrays.copyOfRange(r, 1, r.length);
      }
      resolved = r;
    }
    return resolved;
  }

  // removes redundant slashs, replace "\" to zip separator "/"
  // and check for invalid characters
  private byte[] normalize(byte[] path)
  {
    if (path.length == 0) {
      return path;
    }
    byte prevC = 0;
    for (int i = 0; i < path.length; i++) {
      byte c = path[i];
      if (c == '\\') {
        return normalize(path, i);
      }
      if (c == (byte) '/' && prevC == '/') {
        return normalize(path, i - 1);
      }
      if (c == '\u0000') {
        throw new InvalidPathException(dfs.getString(path), "Path: nul character not allowed");
      }
      prevC = c;
    }
    return path;
  }

  private byte[] normalize(byte[] path, int off)
  {
    byte[] to = new byte[path.length];
    int n = 0;
    while (n < off) {
      to[n] = path[n];
      n++;
    }
    int m = n;
    byte prevC = 0;
    while (n < path.length) {
      byte c = path[n++];
      if (c == (byte) '\\') {
        c = (byte) '/';
      }
      if (c == (byte) '/' && prevC == (byte) '/') {
        continue;
      }
      if (c == '\u0000') {
        throw new InvalidPathException(dfs.getString(path), "Path: nul character not allowed");
      }
      to[m++] = c;
      prevC = c;
    }
    if (m > 1 && to[m - 1] == '/') {
      m--;
    }
    return (m == to.length) ? to : Arrays.copyOf(to, m);
  }

  // Remove DotSlash(./) and resolve DotDot (..) components
  private byte[] getResolved()
  {
    if (path.length == 0) {
      return path;
    }
    for (int i = 0; i < path.length; i++) {
      byte c = path[i];
      if (c == (byte) '.') {
        return resolve0();
      }
    }
    return path;
  }

  // TBD: performance, avoid initOffsets
  private byte[] resolve0()
  {
    byte[] to = new byte[path.length];
    int nc = getNameCount();
    int[] lastM = new int[nc];
    int lastMOff = -1;
    int m = 0;
    for (int i = 0; i < nc; i++) {
      int n = offsets[i];
      int len = (i == offsets.length - 1) ? (path.length - n) : (offsets[i + 1] - n - 1);
      if (len == 1 && path[n] == (byte) '.') {
        if (m == 0 && path[0] == '/') { // absolute path
          to[m++] = '/';
        }
        continue;
      }
      if (len == 2 && path[n] == '.' && path[n + 1] == '.') {
        if (lastMOff >= 0) {
          m = lastM[lastMOff--]; // retreat
          continue;
        }
        if (path[0] == '/') { // "/../xyz" skip
          if (m == 0)
            to[m++] = '/';
        } else { // "../xyz" -> "../xyz"
          if (m != 0 && to[m - 1] != '/') {
            to[m++] = '/';
          }
          while (len-- > 0) {
            to[m++] = path[n++];
          }
        }
        continue;
      }
      if ((m == 0 && path[0] == '/') || // absolute path
          (m != 0 && to[m - 1] != '/')) { // not the first name
        to[m++] = '/';
      }
      lastM[++lastMOff] = m;
      while (len-- > 0) {
        to[m++] = path[n++];
      }
    }
    if (m > 1 && to[m - 1] == '/') {
      m--;
    }
    return (m == to.length) ? to : Arrays.copyOf(to, m);
  }


  /////////////////////////////////////////////////////////////////////


  void createDirectory(FileAttribute<?>... attrs)
  {
    throw new UnsupportedOperationException();
  }

  InputStream newInputStream(OpenOption... options) throws IOException
  {
    if (options.length > 0) {
      for (OpenOption opt : options) {
        if (opt != StandardOpenOption.READ) {
          throw new UnsupportedOperationException("'" + opt + "' not allowed");
        }
      }
    }
    return dfs.newInputStream(getResolvedPath());
  }

  DirectoryStream<Path> newDirectoryStream(Filter<? super Path> filter) throws IOException
  {
    return new DlcDirectoryStream(this, filter);
  }

  void delete()
  {
    throw new UnsupportedOperationException();
  }

  void deleteIfExists()
  {
    throw new UnsupportedOperationException();
  }

  DlcFileAttributes getAttributes() throws IOException
  {
    DlcFileAttributes zfas = dfs.getFileAttributes(getResolvedPath());
    if (zfas == null) {
      throw new NoSuchFileException(toString());
    }
    return zfas;
  }

  void setAttribute(String attribute, Object value, LinkOption... options)
  {
    throw new UnsupportedOperationException();
  }

  void setTimes(FileTime mtime, FileTime atime, FileTime ctime)
  {
    throw new UnsupportedOperationException();
  }

  Map<String, Object> readAttributes(String attributes, LinkOption... options) throws IOException
  {
    String view = null;
    String attrs = null;
    int colonPos = attributes.indexOf(':');
    if (colonPos == -1) {
      view = "basic";
      attrs = attributes;
    } else {
      view = attributes.substring(0, colonPos++);
      attrs = attributes.substring(colonPos);
    }
    DlcFileAttributeView dfv = DlcFileAttributeView.get(this, view);
    if (dfv == null) {
      throw new UnsupportedOperationException("view not supported");
    }
    return dfv.readAttributes(attrs);
  }

  FileStore getFileStore() throws IOException
  {
    // each ZipFileSystem only has one root (as requested for now)
    if (exists()) {
      return dfs.getFileStore(this);
    }
    throw new NoSuchFileException(dfs.getString(path));
  }

  boolean isSameFile(Path other) throws IOException
  {
    if (this.equals(other)) {
      return true;
    }
    if (other == null || this.getFileSystem() != other.getFileSystem()) {
      return false;
    }
    this.checkAccess();
    ((DlcPath)other).checkAccess();
    return Arrays.equals(this.getResolvedPath(), ((DlcPath)other).getResolvedPath());
  }

  SeekableByteChannel newByteChannel(Set<? extends OpenOption> options, FileAttribute<?>... attrs)
      throws IOException
  {
    return dfs.newByteChannel(getResolvedPath(), options, attrs);
  }

  FileChannel newFileChannel(Set<? extends OpenOption> options, FileAttribute<?>... attrs)
      throws IOException
  {
    return dfs.newFileChannel(getResolvedPath(), options, attrs);
  }

  void checkAccess(AccessMode... modes) throws IOException
  {
    boolean w = false;
    boolean x = false;
    for (AccessMode mode : modes) {
      switch (mode) {
        case READ:
          break;
        case WRITE:
          w = true;
          break;
        case EXECUTE:
          x = true;
          break;
        default:
          throw new UnsupportedOperationException();
      }
    }
    DlcFileAttributes attrs = dfs.getFileAttributes(getResolvedPath());
    if (attrs == null && (path.length != 1 || path[0] != '/')) {
      throw new NoSuchFileException(toString());
    }
    if (w) {
      if (dfs.isReadOnly()) {
        throw new AccessDeniedException(toString());
      }
    }
    if (x) {
      throw new AccessDeniedException(toString());
    }
  }

  boolean exists()
  {
    if (path.length == 1 && path[0] == '/') {
      return true;
    }
    return dfs.exists(getResolvedPath());
  }

  OutputStream newOutputStream(OpenOption... options)
  {
    throw new UnsupportedOperationException();
  }

  void move(DlcPath target, CopyOption... options)
  {
    throw new UnsupportedOperationException();
  }

  void copy(DlcPath target, CopyOption... options)
  {
    throw new UnsupportedOperationException();
  }
}
