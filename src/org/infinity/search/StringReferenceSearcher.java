// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search;

import java.awt.Component;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.infinity.datatype.StringRef;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.bcs.BcsResource;
import org.infinity.resource.bcs.Compiler;
import org.infinity.resource.bcs.Decompiler;
import org.infinity.resource.bcs.ScriptType;
import org.infinity.resource.dlg.AbstractCode;
import org.infinity.resource.dlg.Action;
import org.infinity.resource.dlg.DlgResource;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.sav.SavResource;
import org.infinity.resource.text.PlainTextResource;

/** Performs search of the specified string reference in other resources. */
public final class StringReferenceSearcher extends AbstractReferenceSearcher
{
  /**
   * Regular expression, used to the find string references in the
   * {@link PlainTextResource text resources}.
   * Expression consists from three parts:
   * <ul>
   * <li>lookbehind for a dot that filters fractional part of the floating-point value</li>
   * <li>integer part itself surrounded with word boundaries to filter things like 2 in {@code 2DA}</li>
   * <li>lookahead for a dot that filter integer part of the floating-point value</li>
   * </ul>
   */
  public static final Pattern NUMBER_PATTERN = Pattern.compile("(?<!\\.)" + "\\b\\d+\\b" + "(?!\\.)", Pattern.DOTALL);
  /** Array of resource extensions which can contains string references. */
  public static final String[] FILE_TYPES = {"2DA", "ARE", "BCS", "BS", "CHR", "CHU", "CRE", "DLG", "EFF",
                                            "GAM", "INI", "ITM", "SPL", "SRC", "STO", "TOH", "WMP"};
  /** Searched string reference value. */
  private final int searchvalue;

  /**
   * Creates finder that searches localizable string in the resources.
   *
   * @param stringRef Searched string reference value
   * @param parent GUI component that will be parent for results window
   */
  public StringReferenceSearcher(int stringRef, Component parent)
  {
    super(null, FILE_TYPES, parent);
    this.searchvalue = stringRef;
  }

  @Override
  protected void search(ResourceEntry entry, Resource resource)
  {
    if (resource instanceof BcsResource)
      searchScript(entry, (BcsResource)resource);
    else if (resource instanceof DlgResource)
      searchDialog(entry, (AbstractStruct)resource);
    else if (resource instanceof SavResource)
      searchSave(entry, (SavResource)resource);
    else if (resource instanceof PlainTextResource)
      searchText(entry, (PlainTextResource)resource);
    else
      searchStruct(entry, (AbstractStruct)resource);
  }

  private void searchDialog(ResourceEntry entry, AbstractStruct dialog)
  {
    for (int i = 0; i < dialog.getFieldCount(); i++) {
      final StructEntry o = dialog.getField(i);
      if (o instanceof StringRef && ((StringRef)o).getValue() == searchvalue) {
        addHit(entry, entry.getSearchString(), o);
      } else if (o instanceof AbstractCode) {
        final AbstractCode sourceCode = (AbstractCode)o;
        try {
          final ScriptType type = sourceCode instanceof Action ? ScriptType.ACTION : ScriptType.TRIGGER;
          final Compiler compiler = new Compiler(sourceCode.toString(), type);
          final String code = compiler.getCode();
          if (compiler.getErrors().isEmpty()) {
            final Decompiler decompiler = new Decompiler(code, true);
            decompiler.setGenerateComments(false);
            decompiler.setGenerateResourcesUsed(true);
            decompiler.setScriptType(type);
            decompiler.decompile();
            for (final Integer stringRef : decompiler.getStringRefsUsed()) {
              if (stringRef.intValue() == searchvalue) {
                addHit(entry, entry.getSearchString(), sourceCode);
              }
            }
          }
        } catch (Exception e) {
          System.out.println("Exception in " + dialog.getName() + " - " + sourceCode.getName());
          e.printStackTrace();
        }
      }
      else if (o instanceof AbstractStruct) {
        searchDialog(entry, (AbstractStruct)o);
      }
    }
  }

  private void searchSavStruct(ResourceEntry entry, ResourceEntry saventry, AbstractStruct struct)
  {
    for (int i = 0; i < struct.getFieldCount(); i++) {
      StructEntry o = struct.getField(i);
      if (o instanceof StringRef && ((StringRef)o).getValue() == searchvalue)
        addHit(entry, saventry.toString(), o);
      else if (o instanceof AbstractStruct)
        searchSavStruct(entry, saventry, (AbstractStruct)o);
    }
  }

  private void searchSave(ResourceEntry entry, SavResource savfile)
  {
    for (ResourceEntry saventry : savfile.getFileHandler().getFileEntries()) {
      Resource resource = ResourceFactory.getResource(saventry);
      if (resource instanceof AbstractStruct) {
        searchSavStruct(entry, saventry, (AbstractStruct)resource);
      }
    }
  }

  private void searchScript(ResourceEntry entry, BcsResource bcsfile)
  {
    Decompiler decompiler = new Decompiler(bcsfile.getCode(), true);
    decompiler.setGenerateComments(false);
    decompiler.setGenerateResourcesUsed(true);
    try {
      decompiler.decompile();
      for (final Integer stringRef : decompiler.getStringRefsUsed()) {
        if (stringRef.intValue() == searchvalue)
          addHit(entry, null, null);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void searchStruct(ResourceEntry entry, AbstractStruct struct)
  {
    for (int i = 0; i < struct.getFieldCount(); i++) {
      StructEntry o = struct.getField(i);
      if (o instanceof StringRef && ((StringRef)o).getValue() == searchvalue)
        addHit(entry, entry.getSearchString(), o);
      else if (o instanceof AbstractStruct)
        searchStruct(entry, (AbstractStruct)o);
    }
  }

  private void searchText(ResourceEntry entry, PlainTextResource text)
  {
    final Matcher m = NUMBER_PATTERN.matcher(text.getText());
    while (m.find()) {
      long nr = Long.parseLong(m.group());
      if (nr == searchvalue) {
        addHit(entry, null, null);
      }
    }
  }
}

