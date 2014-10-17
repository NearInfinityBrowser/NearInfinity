// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.util.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

/** Overrides java.io.RandomAccessFile to support case sensitive file systems. */
public class RandomAccessFileNI extends RandomAccessFile
{
  public RandomAccessFileNI(String name, String mode) throws FileNotFoundException
  {
    super(FileLookup.getInstance().queryFilePath(name), mode);
  }

  public RandomAccessFileNI(File file, String mode) throws FileNotFoundException
  {
    super(new File(FileLookup.getInstance().queryFilePath(file)), mode);
  }

}
