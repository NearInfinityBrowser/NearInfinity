// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.io;

import java.io.File;
import java.util.regex.Pattern;

import javax.swing.filechooser.FileFilter;

/**
 * An implementation of {@link FileFilter} that filters using a specified filename pattern.
 * <p>
 * The following example creates a {@code FileNameFilter} that will show all files starting with an underscore:
 * <pre>
 * FileFilter filter = new FileNameFilter("Internal files", "_.*");
 * JFileChooser fc = ...;
 * fc.addChoosableFileFilter(filter);
 * </pre>
 * </p>
 *
 * @see FileFilter
 * @see javax.swing.JFileChooser#setFileFilter
 * @see javax.swing.JFileChooser#addChoosableFileFilter
 * @see javax.swing.JFileChooser#getFileFilter
 */
public class FileNameFilter extends FileFilter {
  private final String description;
  private final Pattern pattern;

  /**
   * Creates a {@code FileNameFilter} with the specified description and name pattern string. The returned
   * {@code FileNameFilter} will accept all directories and any file with a matching filename.
   *
   * @param description textual description for the filter, may be {@code null}.
   * @param namePattern regular expression pattern as {@code String}.
   */
  public FileNameFilter(String description, String namePattern) {
    this(description, namePattern != null ? Pattern.compile(namePattern) : null);
  }

  /**
   * Creates a {@code FileNameFilter} with the specified description and name {@code Pattern}. The returned
   * {@code FileNameFilter} will accept all directories and any file with a matching filename.
   *
   * @param description textual description for the filter, may be {@code null}.
   * @param namePattern regular expression as {@link Pattern} object.
   */
  public FileNameFilter(String description, Pattern namePattern) {
    this.description = description;
    this.pattern = namePattern != null ? namePattern : Pattern.compile(".+");
  }

  @Override
  public boolean accept(File f) {
    if (f != null) {
      if (f.isDirectory()) {
        return true;
      }
      final String fileName = f.getName();
      return pattern.matcher(fileName).matches();
    }
    return false;
  }

  @Override
  public String getDescription() {
    return description;
  }

  /**
   * Returns the regular expression pattern files are tested against.
   *
   * @return {@link Pattern} instance of the regular expression pattern.
   */
  public Pattern getNamePattern() {
    return pattern;
  }

  @Override
  public String toString() {
    return super.toString() + "[description=" + getDescription() + " pattern=" + getNamePattern() + "]";
  }
}
