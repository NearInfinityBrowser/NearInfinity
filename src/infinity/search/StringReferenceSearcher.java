// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.search;

import infinity.datatype.StringRef;
import infinity.resource.*;
import infinity.resource.bcs.*;
import infinity.resource.bcs.Compiler;
import infinity.resource.dlg.*;
import infinity.resource.key.ResourceEntry;
import infinity.resource.other.PlainTextResource;
import infinity.resource.sav.SavResource;

import java.awt.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringReferenceSearcher extends AbstractReferenceSearcher
{
  private static final Pattern NUMBERPATTERN = Pattern.compile("\\d+", Pattern.DOTALL);
  private final int searchvalue;

  public StringReferenceSearcher(int searchvalue, Component parent)
  {
    super(null, new String[]{"2DA", "ARE", "BCS", "CHR", "CHU", "CRE", "DLG", "EFF", "GAM", "INI",
                             "ITM", "SPL", "SRC", "STO", "TOH", "WMP"}, parent);
    this.searchvalue = searchvalue;
  }

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
    for (int i = 0; i < dialog.getRowCount(); i++) {
      StructEntry o = dialog.getStructEntryAt(i);
      if (o instanceof StringRef && ((StringRef)o).getValue() == searchvalue)
        addHit(entry, entry.getSearchString(), o);
      else if (o instanceof AbstractCode) {
        AbstractCode sourceCode = (AbstractCode)o;
        try {
          String code = Compiler.getInstance().compileDialogCode(sourceCode.toString(),
                                                                 sourceCode instanceof Action);
          if (Compiler.getInstance().getErrors().size() == 0) {
            if (o instanceof Action)
              Decompiler.decompileDialogAction(code, true);
            else
              Decompiler.decompileDialogTrigger(code, true);
            for (final Integer stringRef : Decompiler.getStringRefsUsed()) {
              if (stringRef.intValue() == searchvalue)
                addHit(entry, entry.getSearchString(), sourceCode);
            }
          }
        } catch (Exception e) {
          System.out.println("Exception in " + dialog.getName() + " - " + sourceCode.getName());
          e.printStackTrace();
        }
      }
      else if (o instanceof AbstractStruct)
        searchDialog(entry, (AbstractStruct)o);
    }
  }

  private void searchSavStruct(ResourceEntry entry, ResourceEntry saventry, AbstractStruct struct)
  {
    for (int i = 0; i < struct.getRowCount(); i++) {
      StructEntry o = struct.getStructEntryAt(i);
      if (o instanceof StringRef && ((StringRef)o).getValue() == searchvalue)
        addHit(entry, saventry.toString(), o);
      else if (o instanceof AbstractStruct)
        searchSavStruct(entry, saventry, (AbstractStruct)o);
    }
  }

  private void searchSave(ResourceEntry entry, SavResource savfile)
  {
    List entries = savfile.getFileHandler().getFileEntries();
    for (int i = 0; i < entries.size(); i++) {
      ResourceEntry saventry = (ResourceEntry)entries.get(i);
      Resource resource = ResourceFactory.getResource(saventry);
      if (resource instanceof AbstractStruct)
        searchSavStruct(entry, saventry, (AbstractStruct)resource);
    }
  }

  private void searchScript(ResourceEntry entry, BcsResource bcsfile)
  {
    Decompiler.decompile(bcsfile.getCode(), true);
    for (final Integer stringRef : Decompiler.getStringRefsUsed()) {
      if (stringRef.intValue() == searchvalue)
        addHit(entry, null, null);
    }
  }

  private void searchStruct(ResourceEntry entry, AbstractStruct struct)
  {
    for (int i = 0; i < struct.getRowCount(); i++) {
      StructEntry o = struct.getStructEntryAt(i);
      if (o instanceof StringRef && ((StringRef)o).getValue() == searchvalue)
        addHit(entry, entry.getSearchString(), o);
      else if (o instanceof AbstractStruct)
        searchStruct(entry, (AbstractStruct)o);
    }
  }

  private void searchText(ResourceEntry entry, PlainTextResource text)
  {
    Matcher m = NUMBERPATTERN.matcher(text.getText());
    while (m.find()) {
      long nr = Long.parseLong(text.getText().substring(m.start(), m.end()));
      if (nr == searchvalue)
        addHit(entry, null, null);
    }
  }
}

