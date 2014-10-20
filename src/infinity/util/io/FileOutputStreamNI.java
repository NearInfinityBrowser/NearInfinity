// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.util.io;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/** Overrides java.io.FileOutputStream to support case sensitive file systems. */
public class FileOutputStreamNI extends FileOutputStream
{
  public FileOutputStreamNI(String name) throws FileNotFoundException
  {
    super(FileLookup.getInstance().queryFilePath(name));
  }

  public FileOutputStreamNI(File file) throws FileNotFoundException
  {
    super(new File(FileLookup.getInstance().queryFilePath(file)));
  }

  public FileOutputStreamNI(FileDescriptor fdObj)
  {
    // No wrapper needed
    super(fdObj);
  }

  public FileOutputStreamNI(String name, boolean append) throws FileNotFoundException
  {
    super(FileLookup.getInstance().queryFilePath(name), append);
  }

  public FileOutputStreamNI(File file, boolean append) throws FileNotFoundException
  {
    super(FileLookup.getInstance().queryFilePath(file), append);
  }

}
