// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import infinity.resource.key.FileResourceEntry;
import infinity.resource.key.ResourceEntry;

/**
 * Provides read access to 2DA table data.
 * @author argent77
 */
public class Table2da
{
  // Checked against in strict mode
  private static final String Signature = "2DA";
  private static final String Version = "V1.0";

  private final ResourceEntry entry;
  private final InputStream stream;
  private final boolean ignoreSignature;
  private final Charset charset;
  private final List<String> headers = new ArrayList<String>();
  private final List<TableEntry> data = new ArrayList<TableEntry>();

  private String signature, version;
  private String defaultValue;
  private int maxColumns;           // the max. number of columns per row (key value not included)

  public Table2da(String fileName)
  {
    this(fileName, false, Charset.defaultCharset());
  }

  public Table2da(String fileName, boolean ignoreSignature)
  {
    this(fileName, ignoreSignature, Charset.defaultCharset());
  }

  public Table2da(String fileName, boolean ignoreSignature, Charset charset)
  {
    this(new FileResourceEntry(new File(fileName)), ignoreSignature, Charset.defaultCharset());
  }

  public Table2da(ResourceEntry entry)
  {
    this(entry, false, Charset.defaultCharset());
  }

  public Table2da(ResourceEntry entry, boolean ignoreSignature)
  {
    this(entry, ignoreSignature, Charset.defaultCharset());
  }

  public Table2da(ResourceEntry entry, boolean ignoreSignature, Charset charset)
  {
    if (entry != null && entry.getResourceName() != null && !entry.getResourceName().isEmpty()) {
      this.entry = entry;
    } else {
      this.entry = null;
    }
    this.stream = null;
    this.ignoreSignature = ignoreSignature;
    this.charset = (charset != null) ? charset : Charset.defaultCharset();
    init();
  }

  public Table2da(InputStream stream)
  {
    this(stream, false, Charset.defaultCharset());
  }

  public Table2da(InputStream stream, boolean ignoreSignature)
  {
    this(stream, ignoreSignature, Charset.defaultCharset());
  }

  public Table2da(InputStream stream, boolean ignoreSignature, Charset charset)
  {
    this.entry = null;
    this.stream = stream;
    this.ignoreSignature = ignoreSignature;
    this.charset = charset;
    init();
  }



  /**
   * Returns the resource entry object of the 2DA resource.
   */
  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

  /**
   * Returns how strict the signature of the 2DA resource has been checked.
   */
  public boolean isSignatureIgnored()
  {
    return ignoreSignature;
  }

  /**
   * Returns the character set used during loading of the 2DA resource.
   */
  public String getCharset()
  {
    if (charset != null) {
      return charset.name();
    } else {
      return Charset.defaultCharset().name();
    }
  }

  /**
   * Returns the signature of the 2DA resource. This is always "2DA" in strict mode and can be anything
   * in loose mode.
   * @return Signature of the 2DA resource.
   */
  public String getSignature()
  {
    return signature;
  }

  /**
   * Returns the version of the 2DA resource. Always "V1.0" in strict mode, and usually empty in
   * loose mode.
   * @return Version string of the 2DA resource.
   */
  public String getVersion()
  {
    return version;
  }

  /**
   * Returns the default value as defined by the 2DA resource.
   */
  public String getDefaultValue()
  {
    return defaultValue;
  }

  /**
   * Returns the number of available column headings.
   */
  public int getHeadersCount()
  {
    return headers.size();
  }

  /**
   * Returns the heading at the specified index.
   */
  public String getHeader(int index)
  {
    if (index >= 0 && index < headers.size()) {
      return headers.get(index);
    } else {
      return "";
    }
  }

  /**
   * Returns the max. number of columns that contain valid data.
   * @return Max. number of columns containing valid data.
   */
  public int getTableColumns()
  {
    return maxColumns;
  }

  /**
   * Returns the number of rows containing data.
   */
  public int getTableRows()
  {
    return data.size();
  }

  /**
   * Returns the keys for each row of data in a string array.
   * @return A string array containing all available keys that identify table rows.
   *         (Note: Duplicate keys are allowed.)
   */
  public String[] getTableKeys()
  {

    String[] retVal = new String[data.size()];
    for (int i = 0; i < data.size(); i++) {
      retVal[i] = data.get(i).key;
    }
    return retVal;
  }

  /**
   * Returns the key name of the desired row
   * @param row The row to get the key name from.
   * @return The key name.
   */
  public String getTableKey(int row)
  {
    if (row >= 0 && row < data.size()) {
      return data.get(row).key;
    }
    return "";
  }

  /**
   * Returns an array of data values from the row specified by the 0-based index.
   * @param index A 0-based index the specifies the desired table row.
   * @return An array containing the table data of the desired row (key value not included).
   */
  public String[] getTableData(int index)
  {
    String[] retVal;
    if (index >= 0 && index < data.size()) {
      retVal = new String[maxColumns];
      List<String> values = data.get(index).values;
      for (int i = 0; i < values.size(); i++) {
        retVal[i] = data.get(index).values.get(i);
      }
      // filling remaining entries with default value
      for (int i = values.size(); i < retVal.length; i++) {
        retVal[i] = defaultValue;
      }
    } else {
      retVal = new String[0];
    }
    return retVal;
  }

  /**
   * Returns an array of data values from the row identified by the specified key.
   * @param key The key value to identify the correct row (value of first column).
   * @return An array containing the table data of the desired row (key value not included).
   */
  public String[] getTableData(String key)
  {
    if (key != null) {
      for (int i = 0; i < data.size(); i++) {
        if (key.equalsIgnoreCase(data.get(i).key)) {
          return getTableData(i);
        }
      }
    }
    return new String[0];
  }

  /**
   * Returns the data value at the location defined by <code>key</code> and column heading.
   * @param key The key that identified the correct table row.
   * @param heading The column heading that identifies the correct column of the data.
   * @return The data at the specified location, or an empty string on error.
   *         Note: Returns the first available data of the specified key, if key exists more than once.
   */
  public String getTableData(String key, String heading)
  {
    return getTableData(getKeyIndex(key), getHeaderIndex(heading));
  }

  /**
   * Returns the data value at the location defined by <code>key</code> and column heading.
   * @param key The key that identified the correct table row.
   * @param column The column of the data value.
   * @return The data at the specified location, or an empty string if not found.
   */
  public String getTableData(String key, int column)
  {
    return getTableData(getKeyIndex(key), column);
  }

  /**
   * Returns the data value at the location defined by <code>keyIndex</code> and <code>valueIndex</code>.
   * Note: Does NOT include the key entry at the beginning of each row.
   * @param row Indicates the table row.
   * @param column The column of the data value.
   * @return The data at the specified location, or an empty string on error.
   */
  public String getTableData(int row, int column)
  {
    if (row >= 0 && row < data.size()) {
      List<String> list = data.get(row).values;
      if (column >= 0) {
        if (column < list.size()) {
          return list.get(column);
        } else if (column < maxColumns) {
          return defaultValue;
        }
      }
    }
    return "";
  }

  /**
   * Clears all 2DA data from memory.
   */
  public void clear()
  {
    signature = "";
    version = "";
    defaultValue = "";

    if (headers != null) {
      headers.clear();
    }

    if (data != null) {
      for (int i = 0; i < data.size(); i++) {
        data.get(i).values.clear();
      }
      data.clear();
    }
  }

  /**
   * Reloads the 2DA resource specified in the constructor.
   */
  public void reload()
  {
    init();
  }

  @Override
  public String toString()
  {
    if (entry != null) {
      return String.format("%1$s [rows = %2$d]", entry.getResourceName(), getTableRows());
    } else {
      return String.format("2DA resource [rows = %1$d]", getTableRows());
    }
  }

  private void init()
  {
    clear();

    try {
      BufferedReader br;
      if (stream != null) {
        br = new BufferedReader(new InputStreamReader(stream, charset));
      } else if (entry != null) {
        br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(entry.getResourceData()), charset));
      } else {
        return;
      }

      try {
        String line;
        String[] tokens;
        // reading line 1: signature/version
        line = br.readLine();
        tokens = (line != null) ? line.trim().split("\\s+") : null;
        if (tokens == null) {
          throw new Exception("No signature/version found");
        }
        if (ignoreSignature) {
          signature = line;
        } else {
          if (tokens.length > 1) {
            signature = tokens[0];
            version = tokens[1];
          }
          if (!Signature.equalsIgnoreCase(signature) || !Version.equalsIgnoreCase(version)) {
            throw new Exception("Invalid signature/version found");
          }
        }

        // reading line 2: default value
        line = br.readLine();
        tokens = (line != null) ? line.trim().split("\\s+") : null;
        if (tokens == null) {
          throw new Exception("No default value found");
        }
        if (tokens.length > 0) {
          defaultValue = tokens[0];
        }
        if (defaultValue.isEmpty()) {
          throw new Exception("No default value found");
        }

        // reading line 3: header columns
        line = br.readLine();
        tokens = (line != null) ? line.trim().split("\\s+") : null;
        if (tokens == null) {
          throw new Exception("No header columns found");
        }
        for (int i = 0; i < tokens.length; i++) {
          headers.add(tokens[i]);
        }

        // reading remaining lines: table data
        while ((line = br.readLine()) != null) {
          tokens = line.trim().split("\\s+");
          if (tokens != null && tokens.length > 0) {
            String key = tokens[0];
            List<String> list = new ArrayList<String>();
            for (int i = 1; i < tokens.length; i++) {
              list.add(tokens[i]);
            }
            data.add(new TableEntry(key, list));
          }
        }

        // calculating the max. number of available columns
        maxColumns = headers.size();
        for (int i = 0; i < data.size(); i++) {
          int n = data.get(i).values.size();
          if (n > maxColumns) {
            maxColumns = n;
          }
        }
      } finally {
        br.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
      clear();
    }
  }

  // Returns the index of the specified key
  private int getKeyIndex(String key)
  {
    if (key != null) {
      for (int i = 0; i < data.size(); i++) {
        if (key.equalsIgnoreCase(data.get(i).key)) {
          return i;
        }
      }
    }
    return -1;
  }

  // Returns the index of the specified heading
  private int getHeaderIndex(String heading)
  {
    if (heading != null) {
      for (int i = 0; i < headers.size(); i++) {
        if (heading.equalsIgnoreCase(headers.get(i))) {
          return i;
        }
      }
    }
    return -1;
  }

//-------------------------- INNER CLASSES --------------------------

  private class TableEntry
  {
    public String key;
    public List<String> values;

    public TableEntry(String key, List<String> values)
    {
      this.key = (key != null) ? key : "";
      this.values = (values != null) ? values : new ArrayList<String>();
    }
  }
}
