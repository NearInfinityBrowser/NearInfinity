// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
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
import java.nio.file.ClosedDirectoryStreamException;
import java.nio.file.DirectoryStream;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class DlcDirectoryStream implements DirectoryStream<Path> {
  private final DlcFileSystem dlcfs;
  private final byte[] path;
  private final DirectoryStream.Filter<? super Path> filter;
  private volatile boolean isClosed;
  private volatile Iterator<Path> itr;

  protected DlcDirectoryStream(DlcPath zipPath, DirectoryStream.Filter<? super java.nio.file.Path> filter) throws IOException {
    this.dlcfs = zipPath.getFileSystem();
    this.path = zipPath.getResolvedPath();
    this.filter = filter;
    // sanity check
    if (!dlcfs.isDirectory(path)) {
      throw new NotDirectoryException(zipPath.toString());
    }
  }

  @Override
  public void close() throws IOException {
    isClosed = true;
  }

  @Override
  public Iterator<Path> iterator() {
    if (isClosed) {
      throw new ClosedDirectoryStreamException();
    }
    if (itr != null) {
      throw new IllegalStateException("Iterator has already been returned");
    }

    try {
      itr = dlcfs.iteratorOf(path, filter);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    return new Iterator<Path>() {
      @Override
      public boolean hasNext() {
        if (isClosed) {
          return false;
        }
        return itr.hasNext();
      }

      @Override
      public synchronized Path next() {
        if (isClosed) {
          throw new NoSuchElementException();
        }
        return itr.next();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

}
