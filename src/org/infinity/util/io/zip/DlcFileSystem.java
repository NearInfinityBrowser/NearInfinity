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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
import java.util.zip.ZipError;
import java.util.zip.ZipException;

import static org.infinity.util.io.zip.ZipConstants.*;
import static org.infinity.util.io.zip.ZipUtils.*;

/**
 * FileSystem implementation for DLC archives in zip format loosely based on
 * Oracles example code for virtual filesystems.
 *
 * Provides methods for read-only operations on zip archives created with the
 * "store" compression method.
 *
 * Supported filesystem properties:
 * - encoding: Specifies the filename encoding (Default: CP437)
 */
public class DlcFileSystem extends FileSystem
{
  private final DlcFileSystemProvider provider;
  private final DlcPath defaultDir;
  private final boolean readOnly;
  private final Path dfpath;
  private final ZipCoder zc;

  // configurable by env map
  private final String  nameEncoding;  // default encoding for name/comment


  DlcFileSystem(DlcFileSystemProvider provider, Path dfpath, Map<String, ?> env)
      throws IOException
  {
    // configurable env setup
    if (env != null) {
      this.nameEncoding = env.containsKey("encoding") ? (String) env.get("encoding") : "CP437";
    } else {
      this.nameEncoding = "CP437";
    }

    this.readOnly = true;
    this.provider = provider;
    this.dfpath = dfpath;
    if (Files.notExists(this.dfpath)) {
      throw new FileSystemNotFoundException(this.dfpath.toString());
    }
    this.dfpath.getFileSystem().provider().checkAccess(this.dfpath, AccessMode.READ);
    this.zc = ZipCoder.get(nameEncoding);
    this.defaultDir = new DlcPath(this, getBytes("/"));
    this.ch = Files.newByteChannel(this.dfpath, StandardOpenOption.READ);
    this.cen = initCEN();
  }

  @Override
  public FileSystemProvider provider()
  {
    return provider;
  }

  @Override
  public void close() throws IOException
  {
    if (!isOpen) {
      return;
    }
    isOpen = false; // set closed
    if (!streams.isEmpty()) { // unlock and close all remaining streams
      Set<InputStream> copy = new HashSet<>(streams);
      for (InputStream is : copy) {
        is.close();
      }
    }
    ch.close(); // close the ch just in case no update

    provider.removeFileSystem(dfpath, this);
  }

  @Override
  public boolean isOpen()
  {
    return isOpen;
  }

  @Override
  public boolean isReadOnly()
  {
    return readOnly;
  }

  @Override
  public String getSeparator()
  {
    return "/";
  }

  @Override
  public Iterable<Path> getRootDirectories()
  {
    ArrayList<Path> pathArr = new ArrayList<>();
    pathArr.add(new DlcPath(this, new byte[] { '/' }));
    return pathArr;
  }

  @Override
  public Iterable<FileStore> getFileStores()
  {
    ArrayList<FileStore> list = new ArrayList<>(1);
    list.add(new DlcFileStore(new DlcPath(this, new byte[]{ '/' })));
    return list;
  }

  private static final Set<String> supportedFileAttributeViews =
      Collections.unmodifiableSet(new HashSet<String>(
          Arrays.asList(DlcFileAttributeView.VIEW_BASIC, DlcFileAttributeView.VIEW_ZIP)));

  @Override
  public Set<String> supportedFileAttributeViews()
  {
    return supportedFileAttributeViews;
  }

  @Override
  public Path getPath(String first, String... more)
  {
    String path;
    if (more.length == 0) {
      path = first;
    } else {
      StringBuilder sb = new StringBuilder();
      sb.append(first);
      for (String segment : more) {
        if (segment.length() > 0) {
          if (sb.length() > 0) {
            sb.append('/');
          }
          sb.append(segment);
        }
      }
      path = sb.toString();
    }
    return new DlcPath(this, getBytes(path));
  }

  private static final String GLOB_SYNTAX = "glob";
  private static final String REGEX_SYNTAX = "regex";

  @Override
  public PathMatcher getPathMatcher(String syntaxAndPattern)
  {
    int pos = syntaxAndPattern.indexOf(':');
    if (pos <= 0 || pos == syntaxAndPattern.length()) {
      throw new IllegalArgumentException();
    }
    String syntax = syntaxAndPattern.substring(0, pos);
    String input = syntaxAndPattern.substring(pos + 1);
    String expr;
    if (syntax.equals(GLOB_SYNTAX)) {
      expr = toRegexPattern(input);
    } else {
      if (syntax.equals(REGEX_SYNTAX)) {
        expr = input;
      } else {
        throw new UnsupportedOperationException("Syntax '" + syntax + "' not recognized");
      }
    }
    // return matcher
    final Pattern pattern = Pattern.compile(expr);
    return new PathMatcher() {
      @Override
      public boolean matches(Path path)
      {
        return pattern.matcher(path.toString()).matches();
      }
    };
  }

  @Override
  public UserPrincipalLookupService getUserPrincipalLookupService()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public WatchService newWatchService() throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString()
  {
    return dfpath.toString();
  }

  Path getDlcFile()
  {
    return dfpath;
  }

  DlcPath getDefaultDir()
  {
    return defaultDir;
  }

  FileStore getFileStore(DlcPath path)
  {
    return new DlcFileStore(path);
  }

  DlcFileAttributes getFileAttributes(byte[] path) throws IOException
  {
    Entry e;
    beginRead();
    try {
      ensureOpen();
      e = getEntry0(path);
      if (e == null) {
        IndexNode inode = getInode(path);
        if (inode == null) {
          return null;
        }
        e = new Entry(inode.name); // pseudo directory
        e.method = METHOD_STORED; // STORED for dir
        e.mtime = e.atime = e.ctime = -1;// -1 for all times
      }
    } finally {
      endRead();
    }
    return new DlcFileAttributes(e);
  }

  boolean exists(byte[] path) throws IOException
  {
    beginRead();
    try {
      ensureOpen();
      return getInode(path) != null;
    } finally {
      endRead();
    }
  }

  boolean isDirectory(byte[] path) throws IOException
  {
    beginRead();
    try {
      IndexNode n = getInode(path);
      return (n != null) && n.isDir();
    } finally {
      endRead();
    }
  }

  private DlcPath toDlcPath(byte[] path)
  {
    // make it absolute
    byte[] p = new byte[path.length + 1];
    p[0] = '/';
    System.arraycopy(path, 0, p, 1, path.length);
    return new DlcPath(this, p);
  }

  // returns the list of child paths of "path"
  Iterator<Path> iteratorOf(byte[] path, DirectoryStream.Filter<? super Path> filter)
      throws IOException
  {
    beginRead(); // iteration of inodes needs exclusive lock
    try {
      ensureOpen();
      IndexNode inode = getInode(path);
      if (inode == null)
        throw new NotDirectoryException(getString(path));
      List<Path> list = new ArrayList<>();
      IndexNode child = inode.child;
      while (child != null) {
        DlcPath zp = toDlcPath(child.name);
        if (filter == null || filter.accept(zp)) {
          list.add(zp);
        }
        child = child.sibling;
      }
      return list.iterator();
    } finally {
      endRead();
    }
  }

  // Returns an input stream for reading the contents of the specified
  // file entry.
  InputStream newInputStream(byte[] path) throws IOException
  {
    beginRead();
    try {
      ensureOpen();
      Entry e = getEntry0(path);
      if (e == null) {
        throw new NoSuchFileException(getString(path));
      }
      if (e.isDir()) {
        throw new FileSystemException(getString(path), "is a directory", null);
      }
      return getInputStream(e);
    } finally {
      endRead();
    }
  }

  SeekableByteChannel newByteChannel(byte[] path, Set<? extends OpenOption> options,
                                     FileAttribute<?>... attrs) throws IOException
  {
    checkOptions(options);
    if (options.contains(StandardOpenOption.WRITE) || options.contains(StandardOpenOption.APPEND)) {
      checkWritable();
    }
    beginRead();
    try {
      ensureOpen();
      Entry e = getEntry0(path);
      if (e == null || e.isDir()) {
        throw new NoSuchFileException(getString(path));
      }

      final long basePos = getDataPos(e);
      final long baseSize = e.size;
      final SeekableByteChannel sbc = Files.newByteChannel(getDlcFile(), options, attrs);
      sbc.position(basePos);
      return new SeekableByteChannel() {
        @Override
        public boolean isOpen()
        {
          return sbc.isOpen();
        }

        @Override
        public void close() throws IOException
        {
          sbc.close();
        }

        @Override
        public int write(ByteBuffer src) throws IOException
        {
          throw new UnsupportedOperationException();
        }

        @Override
        public SeekableByteChannel truncate(long size) throws IOException
        {
          throw new UnsupportedOperationException();
        }

        @Override
        public long size() throws IOException
        {
          return baseSize;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException
        {
          checkOpen();
          if (sbc.position() >= basePos+baseSize) {
            return -1;
          }
          int remaining = (int)(baseSize - (sbc.position() - basePos));
          remaining = Math.min(dst.remaining(), remaining);
          int processed = 0, nread;
          do {
            nread = sbc.read(dst);
            if (nread > 0) {
              processed += nread;
            }
          } while (nread >= 0 && dst.hasRemaining());
          return processed;
        }

        @Override
        public SeekableByteChannel position(long newPosition) throws IOException
        {
          checkOpen();
          if (newPosition < 0) {
            throw new IOException("Negative position");
          }
          sbc.position(basePos + newPosition);
          return this;
        }

        @Override
        public long position() throws IOException
        {
          checkOpen();
          return sbc.position() - basePos;
        }

        private void checkOpen() throws IOException
        {
          if (sbc == null || !sbc.isOpen()) {
            throw new IOException("Channel not open");
          }
        }
      };

//      final ReadableByteChannel rbc = Channels.newChannel(getInputStream(e));
//      final long size = e.size;
//      return new SeekableByteChannel() {
//        long read = 0;
//
//        public boolean isOpen()
//        {
//          return rbc.isOpen();
//        }
//
//        public long position() throws IOException
//        {
//          return read;
//        }
//
//        public SeekableByteChannel position(long pos) throws IOException
//        {
//          throw new UnsupportedOperationException();
//        }
//
//        public int read(ByteBuffer dst) throws IOException
//        {
//          int n = rbc.read(dst);
//          if (n > 0) {
//            read += n;
//          }
//          return n;
//        }
//
//        public SeekableByteChannel truncate(long size) throws IOException
//        {
//          throw new NonWritableChannelException();
//        }
//
//        public int write(ByteBuffer src) throws IOException
//        {
//          throw new NonWritableChannelException();
//        }
//
//        public long size() throws IOException
//        {
//          return size;
//        }
//
//        public void close() throws IOException
//        {
//          rbc.close();
//        }
//      };
    } finally {
      endRead();
    }
  }

  FileChannel newFileChannel(byte[] path, Set<? extends OpenOption> options,
                             FileAttribute<?>... attrs) throws IOException
  {
    checkOptions(options);
    if (options.contains(StandardOpenOption.WRITE) || options.contains(StandardOpenOption.APPEND)) {
      checkWritable();
    }
    beginRead();
    try {
      ensureOpen();
      Entry e = getEntry0(path);
      if (e == null || e.isDir()) {
        throw new NoSuchFileException(getString(path));
      }

      final long basePos = getDataPos(e);
      final long baseSize = e.size;
      final FileChannel fch = FileChannel.open(getDlcFile(), options, attrs);
      fch.position(basePos);
      return new FileChannel() {
        @Override
        protected void implCloseChannel() throws IOException
        {
          fch.close();
        }

        @Override
        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException
        {
          throw new UnsupportedOperationException();
        }

        @Override
        public int write(ByteBuffer src, long position) throws IOException
        {
          throw new UnsupportedOperationException();
        }

        @Override
        public int write(ByteBuffer src) throws IOException
        {
          throw new UnsupportedOperationException();
        }

        @Override
        public FileLock tryLock(long position, long size, boolean shared) throws IOException
        {
          checkOpen();
          if (position < 0) {
            throw new IOException("Position is negative");
          } else if (size < 0) {
            throw new IOException("Size is negative");
          }
          return fch.tryLock(basePos + position, Math.min(size, baseSize - position), shared);
        }

        @Override
        public FileChannel truncate(long size) throws IOException
        {
          throw new UnsupportedOperationException();
        }

        @Override
        public long transferTo(long position, long count, WritableByteChannel target) throws IOException
        {
          checkOpen();
          return fch.transferTo(basePos + position, Math.min(count, baseSize - position), target);
        }

        @Override
        public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException
        {
          throw new UnsupportedOperationException();
        }

        @Override
        public long size() throws IOException
        {
          checkOpen();
          return baseSize;
        }

        @Override
        public long read(ByteBuffer[] dsts, int offset, int length) throws IOException
        {
          checkOpen();
          if (fch.position() >= basePos+baseSize) {
            return -1L;
          }

          long remaining = baseSize - (fch.position() - basePos);
          long processed = 0;
          int idx = offset;
          int maxIdx = Math.min(dsts.length - offset, idx + length);
          byte[] buf = new byte[65536];
          MappedByteBuffer src = fch.map(MapMode.READ_ONLY, fch.position(), remaining);
          while (idx < maxIdx) {
            long toRead = Math.min(dsts[idx].remaining(), remaining);
            while (toRead > 0) {
              long bufSize = Math.min(buf.length, toRead);
              src.get(buf, 0, (int)bufSize);
              dsts[idx].put(buf, 0, (int)bufSize);
              toRead -= bufSize;
              processed += bufSize;
              remaining -= bufSize;
            }
            idx++;
          }
          return processed;
        }

        @Override
        public int read(ByteBuffer dst, long position) throws IOException
        {
          checkOpen();
          long curPosition = fch.position();
          if (position >= basePos+baseSize) {
            return -1;
          } else if (position < 0) {
            throw new IOException("Negative position");
          }
          fch.position(basePos + position);
          long retVal = read(new ByteBuffer[]{dst}, 0, 1);
          fch.position(curPosition);
          return (int)retVal;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException
        {
          return (int)read(new ByteBuffer[]{dst}, 0, 1);
        }

        @Override
        public FileChannel position(long newPosition) throws IOException
        {
          checkOpen();
          if (newPosition < 0) {
            throw new IOException("Negative position");
          }
          fch.position(basePos + newPosition);
          return this;
        }

        @Override
        public long position() throws IOException
        {
          checkOpen();
          return fch.position() - basePos;
        }

        @Override
        public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException
        {
          checkOpen();
          if (position < 0) {
            throw new IOException("Negative position");
          } else if (size < 0) {
            throw new IOException("Negative size");
          } else if (position > baseSize) {
            throw new IOException("Position exceeds file size");
          }
          long absPos = basePos + position;
          long absSize = Math.min(size, baseSize - position);
          return fch.map(mode, absPos, absSize);
        }

        @Override
        public FileLock lock(long position, long size, boolean shared) throws IOException
        {
          checkOpen();
          if (position < 0) {
            throw new IOException("Position is negative");
          } else if (size < 0) {
            throw new IOException("Size is negative");
          }
          return fch.lock(basePos + position, Math.min(size, baseSize - position), shared);
        }

        @Override
        public void force(boolean metaData) throws IOException
        {
          checkOpen();
          // do nothing
        }

        private void checkOpen() throws IOException
        {
          if (fch == null || !fch.isOpen()) {
            throw new IOException("Channel not open");
          }
        }
      };
    } finally {
      endRead();
    }
  }


  private void checkWritable() throws IOException
  {
    throw new ReadOnlyFileSystemException();
  }

  private void checkOptions(Set<? extends OpenOption> options)
  {
    // check for options of null type and option is an intance of
    // StandardOpenOption
    for (OpenOption option : options) {
      if (option == null) {
        throw new NullPointerException();
      }
      if (!(option instanceof StandardOpenOption)) {
        throw new IllegalArgumentException();
      }
    }
  }

  // the outstanding input streams that need to be closed
  private Set<InputStream> streams = Collections.synchronizedSet(new HashSet<InputStream>());


  private final void beginRead()
  {
    rwlock.readLock().lock();
  }

  private final void endRead()
  {
    rwlock.readLock().unlock();
  }

  ///////////////////////////////////////////////////////////////////

  private volatile boolean isOpen = true;
  private final SeekableByteChannel ch; // channel to the zipfile
  final byte[] cen; // CEN & ENDHDR
  private END end;
  private long locpos; // position of first LOC header (usually 0)

  private final ReadWriteLock rwlock = new ReentrantReadWriteLock();

  // name -> pos (in cen), IndexNode itself can be used as a "key"
  private LinkedHashMap<IndexNode, IndexNode> inodes;

  final byte[] getBytes(String name)
  {
    return zc.getBytes(name);
  }

  final String getString(byte[] name)
  {
    return zc.toString(name);
  }

  protected void finalize() throws IOException
  {
    close();
  }

  private long getDataPos(Entry e) throws IOException
  {
    if (e.locoff == -1) {
      Entry e2 = getEntry0(e.name);
      if (e2 == null) {
        throw new ZipException("invalid loc for entry <" + e.name + ">");
      }
      e.locoff = e2.locoff;
    }
    byte[] buf = new byte[LOCHDR];
    if (readFullyAt(buf, 0, buf.length, e.locoff) != buf.length) {
      throw new ZipException("invalid loc for entry <" + e.name + ">");
    }
    return locpos + e.locoff + LOCHDR + LOCNAM(buf) + LOCEXT(buf);
  }

  // Reads len bytes of data from the specified offset into buf.
  // Returns the total number of bytes read.
  // Each/every byte read from here (except the cen, which is mapped).
  final long readFullyAt(byte[] buf, int off, long len, long pos) throws IOException
  {
    ByteBuffer bb = ByteBuffer.wrap(buf);
    bb.position(off);
    bb.limit((int) (off + len));
    return readFullyAt(bb, pos);
  }

  private final long readFullyAt(ByteBuffer bb, long pos) throws IOException
  {
    synchronized (ch) {
      return ch.position(pos).read(bb);
    }
  }

  // Searches for end of central directory (END) header. Returns the contents
  // of the END header. Throws an exception if the END header was not found or
  // an error occurred.
  private END findEND() throws IOException
  {
    byte[] buf = new byte[READBLOCKSZ];
    long ziplen = ch.size();
    long minHDR = (ziplen - END_MAXLEN) > 0 ? ziplen - END_MAXLEN : 0;
    long minPos = minHDR - (buf.length - ENDHDR);
    END end = null;

    for (long pos = ziplen - buf.length; (pos >= minPos) && (end == null); pos -= (buf.length - ENDHDR)) {
      int off = 0;
      if (pos < 0) {
        // Pretend there are some NUL bytes before start of file
        off = (int) -pos;
        Arrays.fill(buf, 0, off, (byte) 0);
      }
      int len = buf.length - off;
      if (readFullyAt(buf, off, len, pos + off) != len) {
        zerror("zip END header not found");
      }

      // Now scan the block backwards for END header signature
      for (int i = buf.length - ENDHDR; (i >= 0) && (end == null); i--) {
        if (buf[i + 0] == (byte) 'P' && buf[i + 1] == (byte) 'K' && buf[i + 2] == (byte) '\005'
            && buf[i + 3] == (byte) '\006' && (pos + i + ENDHDR + ENDCOM(buf, i) <= ziplen)) {
          // Found END header
          buf = Arrays.copyOfRange(buf, i, i + ENDHDR);
          end = new END();
          end.endsub = ENDSUB(buf);
          end.centot = ENDTOT(buf);
          end.cenlen = ENDSIZ(buf);
          end.cenoff = ENDOFF(buf);
          end.comlen = ENDCOM(buf);
          end.endpos = pos + i;
        }
      }
    }

    // Double check by parsing through CEN and updating END structure if necessary,
    // or search for END structure from front to back as fall back solution.
    if (end != null && end.cenoff >= end.endpos) {
      zerror("invalid END header (bad central directory size)");
    }
    long curpos = (end != null) ? end.cenoff : 0;
    long endpos = (end != null) ? end.endpos : ziplen;
    int bufSize = Math.max(Math.max(Math.max(12, LOCHDR), CENHDR), ENDHDR);
    buf = new byte[bufSize];

    // do a sequential search to find the first instance of a supported header signature
    while (curpos < endpos) {
      readFullyAt(buf, 0, 4, curpos);
      long sig = CENSIG(buf, 0);
      if (sig == LOCSIG || sig == CENSIG || sig == ZIP64_ENDSIG || sig == ZIP64_LOCHDR || sig == ENDSIG) {
        break;
      }
      curpos++;
    }

    while (curpos < endpos) {
      long sig = LOCSIG(buf);
      if (sig == LOCSIG) {
        if (readFullyAt(buf, 0, LOCHDR, curpos) != LOCHDR) {
          zerror("read LOC structure failed");
        }
        long csize = LOCSIZ(buf);
        long size = LOCLEN(buf);
        if (csize == ZIP64_MINVAL || size == ZIP64_MINVAL) {
          zerror("ZIP64 LOC structure not supported");
        }
      } else if (sig == CENSIG) { // central directory record
        if (readFullyAt(buf, 0, CENHDR, curpos) != CENHDR) {
          zerror("read CEN tables failed");
        }
        curpos += CENHDR + CENNAM(buf, 0) + CENEXT(buf, 0) + CENCOM(buf, 0);
      } else if (sig == ZIP64_ENDSIG) { // zip64 end of central directory record
        if (readFullyAt(buf, 0, 12, curpos) != 12) {
          zerror("read ZIP64 end header failed");
        }
        curpos += 12 + LL(buf, 4);
      } else if (sig == ZIP64_LOCSIG) { // zip64 end of central directory locator
        curpos += ZIP64_LOCHDR;
      } else if (sig == ENDSIG) { // END header
        if (readFullyAt(buf, 0, ENDHDR, curpos) != ENDHDR) {
          zerror("zip END header not found");
        }
        if (end == null) {
          end = new END();
        }
        end.endsub = ENDSUB(buf);
        end.centot = ENDTOT(buf);
        end.cenlen = ENDSIZ(buf);
        end.cenoff = ENDOFF(buf);
        end.comlen = ENDCOM(buf);
        end.endpos = curpos;
        break;
      } else {
        zerror("invalid header data found");
      }
      readFullyAt(buf, 0, 4, curpos);
    }

    if (end != null) {
      if (end.cenlen == ZIP64_MINVAL || end.cenoff == ZIP64_MINVAL || end.centot == ZIP64_MINVAL32) {
        // need to find the zip64 end;
        byte[] loc64 = new byte[ZIP64_LOCHDR];
        if (readFullyAt(loc64, 0, loc64.length, end.endpos - ZIP64_LOCHDR) != loc64.length) {
          return end;
        }
        long end64pos = ZIP64_LOCOFF(loc64);
        byte[] end64buf = new byte[ZIP64_ENDHDR];
        if (readFullyAt(end64buf, 0, end64buf.length, end64pos) != end64buf.length) {
          return end;
        }
        // end64 found, re-calcualte everything.
        end.cenlen = ZIP64_ENDSIZ(end64buf);
        end.cenoff = ZIP64_ENDOFF(end64buf);
        end.centot = (int) ZIP64_ENDTOT(end64buf); // assume total < 2g
        end.endpos = end64pos;
      }
      return end;
    }
    zerror("zip END header not found");
    return null; // make compiler happy
  }

  // Reads zip file central directory. Returns the file position of first
  // CEN header, otherwise returns -1 if an error occurred. If zip->msg != NULL
  // then the error was a zip format error and zip->msg has the error text.
  // Always pass in -1 for knownTotal; it's used for a recursive call.
  private byte[] initCEN() throws IOException
  {
    end = findEND();
    if (end.endpos == 0) {
      inodes = new LinkedHashMap<>(10);
      locpos = 0;
      return null; // only END header present
    }
    if (end.cenlen > end.endpos) {
      zerror("invalid END header (bad central directory size)");
    }
    long cenpos = end.endpos - end.cenlen; // position of CEN table

    // Get position of first local file (LOC) header, taking into
    // account that there may be a stub prefixed to the zip file.
    locpos = cenpos - end.cenoff;
    if (locpos < 0) {
      zerror("invalid END header (bad central directory offset)");
    }

    // read in the CEN and END
    byte[] cen = new byte[(int) (end.cenlen + ENDHDR)];
    if (readFullyAt(cen, 0, cen.length, cenpos) != end.cenlen + ENDHDR) {
      zerror("read CEN tables failed");
    }
    // Iterate through the entries in the central directory
    inodes = new LinkedHashMap<>(end.centot + 1);
    int pos = 0;
    int limit = cen.length - ENDHDR;
    while (pos < limit) {
      if (CENSIG(cen, pos) != CENSIG) {
        zerror("invalid CEN header (bad signature)");
      }
      int method = CENHOW(cen, pos);
      int nlen = CENNAM(cen, pos);
      int elen = CENEXT(cen, pos);
      int clen = CENCOM(cen, pos);
      if ((CENFLG(cen, pos) & 1) != 0) {
        zerror("invalid CEN header (encrypted entry)");
      }
      if (method != METHOD_STORED) {
        zerror("invalid CEN header (unsupported compression method: " + method + ")");
      }
      if (pos + CENHDR + nlen > limit) {
        zerror("invalid CEN header (bad header size)");
      }
      byte[] name = Arrays.copyOfRange(cen, pos + CENHDR, pos + CENHDR + nlen);
      IndexNode inode = new IndexNode(name, pos);
      inodes.put(inode, inode);
      // skip ext and comment
      pos += (CENHDR + nlen + elen + clen);
    }
    if (pos + ENDHDR != cen.length) {
      zerror("invalid CEN header (bad header size)");
    }
    return cen;
  }

  private void ensureOpen() throws IOException
  {
    if (!isOpen) {
      throw new ClosedFileSystemException();
    }
  }


  //////////////////// update & sync //////////////////////////////////////

  private IndexNode getInode(byte[] path)
  {
    if (path == null) {
      throw new NullPointerException("path");
    }
    IndexNode key = IndexNode.keyOf(path);
    IndexNode inode = inodes.get(key);
    if (inode == null && (path.length == 0 || path[path.length - 1] != '/')) {
      // if does not ends with a slash
      path = Arrays.copyOf(path, path.length + 1);
      path[path.length - 1] = '/';
      inode = inodes.get(key.as(path));
    }
    return inode;
  }

  private Entry getEntry0(byte[] path) throws IOException
  {
    IndexNode inode = getInode(path);
    if (inode instanceof Entry) {
      return (Entry) inode;
    }
    if (inode == null || inode.pos == -1) {
      return null;
    }
    return Entry.readCEN(this, inode.pos);
  }

  private InputStream getInputStream(Entry e) throws IOException
  {
    InputStream eis = null;

    if (e.type == Entry.NEW) {
      if (e.bytes != null) {
        eis = new ByteArrayInputStream(e.bytes);
      } else if (e.file != null) {
        eis = Files.newInputStream(e.file);
      } else {
        throw new ZipException("update entry data is missing");
      }
    } else if (e.type == Entry.FILECH) {
      // FILECH result is un-compressed.
      eis = Files.newInputStream(e.file);
      // TBD: wrap to hook close()
      // streams.add(eis);
      return eis;
    } else { // untouced CEN or COPY
      eis = new EntryInputStream(e, ch);
    }
    if (e.method == METHOD_STORED) {
      // TBD: wrap/ it does not seem necessary
    } else {
      throw new ZipException("invalid compression method");
    }
    streams.add(eis);
    return eis;
  }

  // Inner class implementing the input stream used to read
  // a (possibly compressed) zip file entry.
  private class EntryInputStream extends InputStream
  {
    private final SeekableByteChannel zfch; // local ref to zipfs's "ch".
                                            // zipfs.ch might
                                // point to a new channel after sync()
    private long pos;           // current position within entry data
    protected long rem;         // number of remaining bytes within entry

    EntryInputStream(Entry e, SeekableByteChannel zfch) throws IOException
    {
      this.zfch = zfch;
      rem = e.csize;
      pos = getDataPos(e);
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException
    {
      ensureOpen();
      if (rem == 0) {
        return -1;
      }
      if (len <= 0) {
        return 0;
      }
      if (len > rem) {
        len = (int) rem;
      }
      // readFullyAt()
      long n = 0;
      ByteBuffer bb = ByteBuffer.wrap(b);
      bb.position(off);
      bb.limit(off + len);
      synchronized (zfch) {
        n = zfch.position(pos).read(bb);
      }
      if (n > 0) {
        pos += n;
        rem -= n;
      }
      if (rem == 0) {
        close();
      }
      return (int) n;
    }

    @Override
    public int read() throws IOException
    {
      byte[] b = new byte[1];
      if (read(b, 0, 1) == 1) {
        return b[0] & 0xff;
      } else {
        return -1;
      }
    }

    @Override
    public long skip(long n) throws IOException
    {
      ensureOpen();
      if (n > rem)
        n = rem;
      pos += n;
      rem -= n;
      if (rem == 0) {
        close();
      }
      return n;
    }

    @Override
    public int available()
    {
      return rem > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) rem;
    }

    @Override
    public void close()
    {
      rem = 0;
      streams.remove(this);
    }
  }

  static void zerror(String msg)
  {
    throw new ZipError(msg);
  }

  // End of central directory record
  static class END
  {
    int disknum;
    int sdisknum;
    int endsub;   // endsub
    int centot;   // 4 bytes
    long cenlen;  // 4 bytes
    long cenoff;  // 4 bytes
    int comlen;   // comment length
    byte[] comment;

    /* members of Zip64 end of central directory locator */
    int diskNum;
    long endpos;
    int disktot;

    void write(OutputStream os, long offset) throws IOException
    {
      boolean hasZip64 = false;
      long xlen = cenlen;
      long xoff = cenoff;
      if (xlen >= ZIP64_MINVAL) {
        xlen = ZIP64_MINVAL;
        hasZip64 = true;
      }
      if (xoff >= ZIP64_MINVAL) {
        xoff = ZIP64_MINVAL;
        hasZip64 = true;
      }
      int count = centot;
      if (count >= ZIP64_MINVAL32) {
        count = ZIP64_MINVAL32;
        hasZip64 = true;
      }
      if (hasZip64) {
        long off64 = offset;
        // zip64 end of central directory record
        writeInt(os, ZIP64_ENDSIG); // zip64 END record signature
        writeLong(os, ZIP64_ENDHDR - 12); // size of zip64 end
        writeShort(os, 45); // version made by
        writeShort(os, 45); // version needed to extract
        writeInt(os, 0); // number of this disk
        writeInt(os, 0); // central directory start disk
        writeLong(os, centot); // number of directory entires on disk
        writeLong(os, centot); // number of directory entires
        writeLong(os, cenlen); // length of central directory
        writeLong(os, cenoff); // offset of central directory

        // zip64 end of central directory locator
        writeInt(os, ZIP64_LOCSIG); // zip64 END locator signature
        writeInt(os, 0); // zip64 END start disk
        writeLong(os, off64); // offset of zip64 END
        writeInt(os, 1); // total number of disks (?)
      }
      writeInt(os, ENDSIG); // END record signature
      writeShort(os, 0); // number of this disk
      writeShort(os, 0); // central directory start disk
      writeShort(os, count); // number of directory entries on disk
      writeShort(os, count); // total number of directory entries
      writeInt(os, xlen); // length of central directory
      writeInt(os, xoff); // offset of central directory
      if (comment != null) { // zip file comment
        writeShort(os, comment.length);
        writeBytes(os, comment);
      } else {
        writeShort(os, 0);
      }
    }
  }

  // Internal node that links a "name" to its pos in cen table.
  // The node itself can be used as a "key" to lookup itself in
  // the HashMap inodes.
  static class IndexNode
  {
    byte[] name;
    int hashcode; // node is hashable/hashed by its name
    int pos = -1; // position in cen table, -1 menas the
                  // entry does not exists in zip file

    IndexNode(byte[] name, int pos)
    {
      name(name);
      this.pos = pos;
    }

    final static IndexNode keyOf(byte[] name) // get a lookup key;
    {
      return new IndexNode(name, -1);
    }

    final void name(byte[] name)
    {
      this.name = name;
      this.hashcode = Arrays.hashCode(name);
    }

    final IndexNode as(byte[] name) // reuse the node, mostly
    {
      name(name); // as a lookup "key"
      return this;
    }

    boolean isDir()
    {
      return name != null && (name.length == 0 || name[name.length - 1] == '/');
    }

    public boolean equals(Object other)
    {
      if (!(other instanceof IndexNode)) {
        return false;
      }
      return Arrays.equals(name, ((IndexNode) other).name);
    }

    public int hashCode()
    {
      return hashcode;
    }

    IndexNode()
    {
    }

    IndexNode sibling;
    IndexNode child; // 1st child
  }

  static class Entry extends IndexNode
  {
    static final int CEN = 1;     // entry read from cen
    static final int NEW = 2;     // updated contents in bytes or file
    static final int FILECH = 3;  // fch update in "file"
    static final int COPY = 4;    // copy of a CEN entry

    byte[] bytes;                 // updated content bytes
    Path file;                    // use tmp file to store bytes;
    int type = CEN;               // default is the entry read from cen

    // entry attributes
    int version;
    int flag;
    int method = -1;    // compression method
    long mtime = -1;    // last modification time (in DOS time)
    long atime = -1;    // last access time
    long ctime = -1;    // create time
    long crc = -1;      // crc-32 of entry data
    long csize = -1;    // compressed size of entry data
    long size = -1;     // uncompressed size of entry data
    byte[] extra;

    // cen
    int versionMade;
    int disk;
    int attrs;
    long attrsEx;
    long locoff;
    byte[] comment;

    Entry()
    {
    }

    Entry(byte[] name)
    {
      name(name);
      this.mtime = this.ctime = this.atime = System.currentTimeMillis();
      this.crc = 0;
      this.size = 0;
      this.csize = 0;
      this.method = METHOD_STORED;
    }

    Entry(byte[] name, int type)
    {
      this(name);
      this.type = type;
    }

    Entry(Entry e, int type)
    {
      name(e.name);
      this.version = e.version;
      this.ctime = e.ctime;
      this.atime = e.atime;
      this.mtime = e.mtime;
      this.crc = e.crc;
      this.size = e.size;
      this.csize = e.csize;
      this.method = e.method;
      this.extra = e.extra;
      this.versionMade = e.versionMade;
      this.disk = e.disk;
      this.attrs = e.attrs;
      this.attrsEx = e.attrsEx;
      this.locoff = e.locoff;
      this.comment = e.comment;
      this.type = type;
    }

    Entry(byte[] name, Path file, int type)
    {
      this(name, type);
      this.file = file;
      this.method = METHOD_STORED;
    }

    int version() throws ZipException
    {
      if (method == METHOD_STORED) {
        return 10;
      }
      throw new ZipException("unsupported compression method");
    }

    ///////////////////// CEN //////////////////////
    static Entry readCEN(DlcFileSystem dlcfs, int pos) throws IOException
    {
      return new Entry().cen(dlcfs, pos);
    }

    private Entry cen(DlcFileSystem dlcfs, int pos) throws IOException
    {
      byte[] cen = dlcfs.cen;
      if (CENSIG(cen, pos) != CENSIG) {
        zerror("invalid CEN header (bad signature)");
      }
      versionMade = CENVEM(cen, pos);
      version = CENVER(cen, pos);
      flag = CENFLG(cen, pos);
      method = CENHOW(cen, pos);
      mtime = dosToJavaTime(CENTIM(cen, pos));
      crc = CENCRC(cen, pos);
      csize = CENSIZ(cen, pos);
      size = CENLEN(cen, pos);
      int nlen = CENNAM(cen, pos);
      int elen = CENEXT(cen, pos);
      int clen = CENCOM(cen, pos);
      disk = CENDSK(cen, pos);
      attrs = CENATT(cen, pos);
      attrsEx = CENATX(cen, pos);
      locoff = CENOFF(cen, pos);

      pos += CENHDR;
      name(Arrays.copyOfRange(cen, pos, pos + nlen));

      pos += nlen;
      if (elen > 0) {
        extra = Arrays.copyOfRange(cen, pos, pos + elen);
        pos += elen;
        readExtra(dlcfs);
      }
      if (clen > 0) {
        comment = Arrays.copyOfRange(cen, pos, pos + clen);
      }
      return this;
    }

    ///////////////////// LOC //////////////////////
    static Entry readLOC(DlcFileSystem dlcfs, long pos) throws IOException
    {
      return readLOC(dlcfs, pos, new byte[1024]);
    }

    static Entry readLOC(DlcFileSystem dlcfs, long pos, byte[] buf) throws IOException
    {
      return new Entry().loc(dlcfs, pos, buf);
    }

    Entry loc(DlcFileSystem dlcfs, long pos, byte[] buf) throws IOException
    {
      assert (buf.length >= LOCHDR);
      if (dlcfs.readFullyAt(buf, 0, LOCHDR, pos) != LOCHDR) {
        throw new ZipException("loc: reading failed");
      }
      if (LOCSIG(buf) != LOCSIG) {
        throw new ZipException("loc: wrong sig ->" + Long.toString(LOCSIG(buf), 16));
      }
      // startPos = pos;
      version = LOCVER(buf);
      flag = LOCFLG(buf);
      method = LOCHOW(buf);
      mtime = dosToJavaTime(LOCTIM(buf));
      crc = LOCCRC(buf);
      csize = LOCSIZ(buf);
      size = LOCLEN(buf);
      int nlen = LOCNAM(buf);
      int elen = LOCEXT(buf);

      name = new byte[nlen];
      if (dlcfs.readFullyAt(name, 0, nlen, pos + LOCHDR) != nlen) {
        throw new ZipException("loc: name reading failed");
      }
      if (elen > 0) {
        extra = new byte[elen];
        if (dlcfs.readFullyAt(extra, 0, elen, pos + LOCHDR + nlen) != elen) {
          throw new ZipException("loc: ext reading failed");
        }
      }
      pos += (LOCHDR + nlen + elen);
      if ((flag & FLAG_DATADESCR) != 0) {
        // Data Descriptor
        Entry e = dlcfs.getEntry0(name); // get the size/csize from cen
        if (e == null) {
          throw new ZipException("loc: name not found in cen");
        }
        size = e.size;
        csize = e.csize;
        pos += (method == METHOD_STORED ? size : csize);
        if (size >= ZIP64_MINVAL || csize >= ZIP64_MINVAL) {
          pos += 24;
        } else {
          pos += 16;
        }
      } else {
        if (extra != null && (size == ZIP64_MINVAL || csize == ZIP64_MINVAL)) {
          // zip64 ext: must include both size and csize
          int off = 0;
          while (off + 20 < elen) { // HeaderID+DataSize+Data
            int sz = SH(extra, off + 2);
            if (SH(extra, off) == EXTID_ZIP64 && sz == 16) {
              size = LL(extra, off + 4);
              csize = LL(extra, off + 12);
              break;
            }
            off += (sz + 4);
          }
        }
        pos += (method == METHOD_STORED ? size : csize);
      }
      return this;
    }

    // read NTFS, UNIX and ZIP64 data from cen.extra
    void readExtra(DlcFileSystem dlcfs) throws IOException
    {
      if (extra == null) {
        return;
      }
      int elen = extra.length;
      int off = 0;
      int newOff = 0;
      while (off + 4 < elen) {
        // extra spec: HeaderID+DataSize+Data
        int pos = off;
        int tag = SH(extra, pos);
        int sz = SH(extra, pos + 2);
        pos += 4;
        if (pos + sz > elen) {  // invalid data
          break;
        }
        switch (tag) {
          case EXTID_ZIP64:
            if (size == ZIP64_MINVAL) {
              if (pos + 8 > elen) { // invalid zip64 extra
                break; // fields, just skip
              }
              size = LL(extra, pos);
              pos += 8;
            }
            if (csize == ZIP64_MINVAL) {
              if (pos + 8 > elen) {
                break;
              }
              csize = LL(extra, pos);
              pos += 8;
            }
            if (locoff == ZIP64_MINVAL) {
              if (pos + 8 > elen) {
                break;
              }
              locoff = LL(extra, pos);
              pos += 8;
            }
            break;
          case EXTID_NTFS:
            if (sz < 32) {
              break;
            }
            pos += 4; // reserved 4 bytes
            if (SH(extra, pos) != 0x0001) {
              break;
            }
            if (SH(extra, pos + 2) != 24) {
              break;
            }
            // override the loc field, datatime here is
            // more "accurate"
            mtime = winToJavaTime(LL(extra, pos + 4));
            atime = winToJavaTime(LL(extra, pos + 12));
            ctime = winToJavaTime(LL(extra, pos + 20));
            break;
          case EXTID_EXTT:
            // spec says the Extened timestamp in cen only has mtime
            // need to read the loc to get the extra a/ctime
            byte[] buf = new byte[LOCHDR];
            if (dlcfs.readFullyAt(buf, 0, buf.length, locoff) != buf.length) {
              throw new ZipException("loc: reading failed");
            }
            if (LOCSIG(buf) != LOCSIG) {
              throw new ZipException("loc: wrong sig ->" + Long.toString(LOCSIG(buf), 16));
            }

            int locElen = LOCEXT(buf);
            if (locElen < 9) {  // EXTT is at lease 9 bytes
              break;
            }
            int locNlen = LOCNAM(buf);
            buf = new byte[locElen];
            if (dlcfs.readFullyAt(buf, 0, buf.length, locoff + LOCHDR + locNlen) != buf.length) {
              throw new ZipException("loc extra: reading failed");
            }
            int locPos = 0;
            while (locPos + 4 < buf.length) {
              int locTag = SH(buf, locPos);
              int locSZ = SH(buf, locPos + 2);
              locPos += 4;
              if (locTag != EXTID_EXTT) {
                locPos += locSZ;
                continue;
              }
              int flag = CH(buf, locPos++);
              if ((flag & 0x1) != 0) {
                mtime = unixToJavaTime(LG(buf, locPos));
                locPos += 4;
              }
              if ((flag & 0x2) != 0) {
                atime = unixToJavaTime(LG(buf, locPos));
                locPos += 4;
              }
              if ((flag & 0x4) != 0) {
                ctime = unixToJavaTime(LG(buf, locPos));
                locPos += 4;
              }
              break;
            }
            break;
          default: // unknown tag
            System.arraycopy(extra, off, extra, newOff, sz + 4);
            newOff += (sz + 4);
        }
        off += (sz + 4);
      }
      if (newOff != 0 && newOff != extra.length) {
        extra = Arrays.copyOf(extra, newOff);
      } else {
        extra = null;
      }
    }
  }
}
