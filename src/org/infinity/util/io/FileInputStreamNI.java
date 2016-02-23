// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.io;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/** Overrides java.io.FileInputStream to support case sensitive file systems. */
public class FileInputStreamNI extends FileInputStream
{
  public FileInputStreamNI(String name) throws FileNotFoundException
  {
    super(FileLookup.getInstance().queryFilePath(name));
  }

  public FileInputStreamNI(File file) throws FileNotFoundException
  {
    super(FileLookup.getInstance().queryFile(file));
  }

  public FileInputStreamNI(FileDescriptor fdObj)
  {
    // No wrapper needed
    super(fdObj);
  }

}
