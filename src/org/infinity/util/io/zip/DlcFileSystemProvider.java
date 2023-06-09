// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderMismatchException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * FileSystemProvider implementation for DLC archives in zip format.
 */
public class DlcFileSystemProvider extends FileSystemProvider {
  /** Returns the URI scheme that identifies this provider. */
  public static final String SCHEME = "dlc";

  private final Map<Path, DlcFileSystem> filesystems = new HashMap<>();

  public DlcFileSystemProvider() {
  }

  @Override
  public String getScheme() {
    return SCHEME;
  }

  @Override
  public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
    return newFileSystem(uriToPath(uri), env);
  }

  @Override
  public FileSystem newFileSystem(Path path, Map<String, ?> env) throws IOException {
    if (path.getFileSystem() != FileSystems.getDefault()) {
      throw new UnsupportedOperationException();
    }
    synchronized (filesystems) {
      Path realPath = null;
      if (ensureFile(path)) {
        // XXX: Using LinkOption.NOFOLLOW_LINKS to prevent issues with Windows junctions
        realPath = path.toRealPath(LinkOption.NOFOLLOW_LINKS);
        if (filesystems.containsKey(realPath)) {
          throw new FileSystemAlreadyExistsException();
        }
      }
      DlcFileSystem dlcfs = null;
      try {
        dlcfs = new DlcFileSystem(this, path, env);
      } catch (DlcError de) {
        String pname = path.toString();
        if (pname.endsWith(".zip") || pname.endsWith(".mod")) {
          throw de;
        }
        // assume NOT a zip file
        throw new UnsupportedOperationException();
      }
      filesystems.put(realPath, dlcfs);
      return dlcfs;
    }
  }

  @Override
  public FileSystem getFileSystem(URI uri) {
    synchronized (filesystems) {
      DlcFileSystem dlcfs = null;
      try {
        // XXX: Using LinkOption.NOFOLLOW_LINKS to prevent issues with Windows junctions
        dlcfs = filesystems.get(uriToPath(uri).toRealPath(LinkOption.NOFOLLOW_LINKS));
      } catch (IOException ioe) {
        // ignore the ioe from toRealPath(), return FSNFE
      }
      if (dlcfs == null) {
        throw new FileSystemNotFoundException();
      }
      return dlcfs;
    }
  }

  @Override
  public Path getPath(URI uri) {
    String spec = uri.getSchemeSpecificPart();
    int sep = spec.indexOf("!/");
    if (sep == -1) {
      throw new IllegalArgumentException("URI: " + uri + " does not contain path info ex. dlc:file:/c:/foo.zip!/BAR");
    }
    return getFileSystem(uri).getPath(spec.substring(sep + 1));
  }

  @Override
  public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
    return toDlcPath(path).newInputStream(options);
  }

  @Override
  public OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
    return toDlcPath(path).newOutputStream(options);
  }

  @Override
  public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
      throws IOException {
    return toDlcPath(path).newFileChannel(options, attrs);
  }

  @Override
  public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
      throws IOException {
    return toDlcPath(path).newByteChannel(options, attrs);
  }

  @Override
  public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
    return toDlcPath(dir).newDirectoryStream(filter);
  }

  @Override
  public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
    toDlcPath(dir).createDirectory(attrs);
  }

  @Override
  public void delete(Path path) throws IOException {
    toDlcPath(path).delete();
  }

  @Override
  public void copy(Path source, Path target, CopyOption... options) throws IOException {
    toDlcPath(source).copy(toDlcPath(target), options);
  }

  @Override
  public void move(Path source, Path target, CopyOption... options) throws IOException {
    toDlcPath(source).move(toDlcPath(target), options);
  }

  @Override
  public boolean isSameFile(Path path, Path path2) throws IOException {
    return toDlcPath(path).isSameFile(path2);
  }

  @Override
  public boolean isHidden(Path path) throws IOException {
    return toDlcPath(path).isHidden();
  }

  @Override
  public FileStore getFileStore(Path path) throws IOException {
    return toDlcPath(path).getFileStore();
  }

  @Override
  public void checkAccess(Path path, AccessMode... modes) throws IOException {
    toDlcPath(path).checkAccess(modes);
  }

  @Override
  public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
    return DlcFileAttributeView.get(toDlcPath(path), type);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
      throws IOException {
    if (type == BasicFileAttributes.class || type == DlcFileAttributes.class) {
      return (A) toDlcPath(path).getAttributes();
    }
    return null;
  }

  @Override
  public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
    return toDlcPath(path).readAttributes(attributes, options);
  }

  @Override
  public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
    toDlcPath(path).setAttribute(attribute, value, options);
  }

  protected Path uriToPath(URI uri) {
    String scheme = uri.getScheme();
    if ((scheme == null) || !scheme.equalsIgnoreCase(getScheme())) {
      throw new IllegalArgumentException("URI scheme is not '" + getScheme() + "'");
    }
    try {
      // only support legacy JAR URL syntax dlc:{uri}!/{entry} for now
      String spec = uri.getRawSchemeSpecificPart();
      int sep = spec.indexOf("!/");
      if (sep != -1) {
        spec = spec.substring(0, sep);
      }
      return Paths.get(new URI(spec)).toAbsolutePath();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }
  }

  private boolean ensureFile(Path path) {
    try {
      BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
      if (!attrs.isRegularFile()) {
        throw new UnsupportedOperationException();
      }
      return true;
    } catch (IOException ioe) {
      return false;
    }
  }

  // Checks that the given file is a UnixPath
  protected static final DlcPath toDlcPath(Path path) {
    if (path == null) {
      throw new NullPointerException();
    }
    if (!(path instanceof DlcPath)) {
      throw new ProviderMismatchException();
    }
    return (DlcPath) path;
  }

  //////////////////////////////////////////////////////////////
  protected void removeFileSystem(Path dfpath, DlcFileSystem dfs) throws IOException {
    synchronized (filesystems) {
      dfpath = dfpath.toRealPath();
      if (filesystems.get(dfpath) == dfs) {
        filesystems.remove(dfpath);
      }
    }
  }
}
