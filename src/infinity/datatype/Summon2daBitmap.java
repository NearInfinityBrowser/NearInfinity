// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import java.util.StringTokenizer;

import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.text.PlainTextResource;
import infinity.util.LongIntegerHashMap;

/** Specialized HashBitmap type for parsing SMTABLES.2DA from IWDEE. */
public class Summon2daBitmap extends HashBitmap
{
  private static final String TableName = "SMTABLES.2DA";

  public Summon2daBitmap(byte[] buffer, int offset, int length, String name)
  {
    this(null, buffer, offset, length, name);
  }

  public Summon2daBitmap(StructEntry parent, byte[] buffer, int offset, int length, String name)
  {
    super(parent, buffer, offset, length, name, getSummonTable());
  }

  private static LongIntegerHashMap<String> getSummonTable()
  {
    LongIntegerHashMap<String> map = new LongIntegerHashMap<String>();

    if (ResourceFactory.getInstance().resourceExists(TableName)) {
      try {
        PlainTextResource smtables =
            new PlainTextResource(ResourceFactory.getInstance().getResourceEntry(TableName));
        StringTokenizer stLine = new StringTokenizer(smtables.getText(), "\r\n");

        // skipping header
        for (int i = 0; i < 3; i++) {
          if (stLine.hasMoreTokens()) {
            stLine.nextToken();
          }
        }

        // parsing table data
        while (stLine.hasMoreTokens()) {
          String line = stLine.nextToken();
          StringTokenizer stElement = new StringTokenizer(line);
          String label = stElement.hasMoreTokens() ? stElement.nextToken() : "";
          String ref = stElement.hasMoreTokens() ? stElement.nextToken() + ".2DA" : "";

          if (!label.isEmpty() && !ref.isEmpty()) {
            // Extracting number from label
            int idx = 0;
            for (; idx < label.length(); idx++) {
              if (!Character.isDigit(label.charAt(idx))) break;
            }

            // adding new table entry to map
            if (idx > 0) {
              try {
                long id = Long.parseLong(label.substring(0, idx));
                map.put(id, ref);
              } catch (NumberFormatException nfe) {
                System.out.println("Number not found in smtables");
              }
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return map;
  }
}
