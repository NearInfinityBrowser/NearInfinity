// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/** Overrides java.io.PrintWriter to support case sensitive file systems. */
public class PrintWriterNI extends PrintWriter
{
  public PrintWriterNI(Writer out)
  {
    // No wrapper needed
    super(out);
  }

  public PrintWriterNI(OutputStream out)
  {
    // No wrapper needed
    super(out);
  }

  public PrintWriterNI(String fileName) throws FileNotFoundException
  {
    super(FileLookup.getInstance().queryFilePath(fileName));
  }

  public PrintWriterNI(File file) throws FileNotFoundException
  {
    super(FileLookup.getInstance().queryFile(file));
  }

  public PrintWriterNI(Writer out, boolean autoFlush)
  {
    // No wrapper needed
    super(out, autoFlush);
  }

  public PrintWriterNI(OutputStream out, boolean autoFlush)
  {
    // No wrapper needed
    super(out, autoFlush);
  }

  public PrintWriterNI(String fileName, String csn) throws FileNotFoundException, UnsupportedEncodingException
  {
    super(FileLookup.getInstance().queryFilePath(fileName), csn);
  }

  public PrintWriterNI(File file, String csn) throws FileNotFoundException, UnsupportedEncodingException
  {
    super(FileLookup.getInstance().queryFile(file), csn);
  }

}
