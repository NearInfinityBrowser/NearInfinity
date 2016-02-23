// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

/** Overrides java.io.PrintStream to support case sensitive file systems. */
public class PrintStreamNI extends PrintStream
{
  public PrintStreamNI(OutputStream out)
  {
    super(out);
  }

  public PrintStreamNI(String fileName) throws FileNotFoundException
  {
    super(FileLookup.getInstance().queryFilePath(fileName));
  }

  public PrintStreamNI(File file) throws FileNotFoundException
  {
    super(FileLookup.getInstance().queryFile(file));
  }

  public PrintStreamNI(OutputStream out, boolean autoFlush)
  {
    // No wrapper needed
    super(out, autoFlush);
  }

  public PrintStreamNI(String fileName, String csn) throws FileNotFoundException, UnsupportedEncodingException
  {
    super(FileLookup.getInstance().queryFilePath(fileName), csn);
  }

  public PrintStreamNI(File file, String csn) throws FileNotFoundException, UnsupportedEncodingException
  {
    super(FileLookup.getInstance().queryFile(file), csn);
  }

  public PrintStreamNI(OutputStream out, boolean autoFlush, String encoding) throws UnsupportedEncodingException
  {
    // No wrapper needed
    super(out, autoFlush, encoding);
  }

}
