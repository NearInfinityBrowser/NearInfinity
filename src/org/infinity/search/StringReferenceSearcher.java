// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search;

import java.awt.Component;
import java.util.List;
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
import org.infinity.resource.dlg.AbstractCode;
import org.infinity.resource.dlg.Action;
import org.infinity.resource.dlg.DlgResource;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.sav.SavResource;
import org.infinity.resource.text.PlainTextResource;

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
      StructEntry o = dialog.getField(i);
      if (o instanceof StringRef && ((StringRef)o).getValue() == searchvalue)
        addHit(entry, entry.getSearchString(), o);
      else if (o instanceof AbstractCode) {
        AbstractCode sourceCode = (AbstractCode)o;
        try {
          Compiler compiler = new Compiler(sourceCode.toString(),
                                           (sourceCode instanceof Action) ? Compiler.ScriptType.ACTION :
                                                                            Compiler.ScriptType.TRIGGER);
          String code = compiler.getCode();
          if (compiler.getErrors().size() == 0) {
            Decompiler decompiler = new Decompiler(code, true);
            if (o instanceof Action) {
              decompiler.setScriptType(Decompiler.ScriptType.ACTION);
            } else {
              decompiler.setScriptType(Decompiler.ScriptType.TRIGGER);
            }
            decompiler.decompile();
            for (final Integer stringRef : decompiler.getStringRefsUsed()) {
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
    List<? extends ResourceEntry> entries = savfile.getFileHandler().getFileEntries();
    for (int i = 0; i < entries.size(); i++) {
      ResourceEntry saventry = (ResourceEntry)entries.get(i);
      Resource resource = ResourceFactory.getResource(saventry);
      if (resource instanceof AbstractStruct)
        searchSavStruct(entry, saventry, (AbstractStruct)resource);
    }
  }

  private void searchScript(ResourceEntry entry, BcsResource bcsfile)
  {
    Decompiler decompiler = new Decompiler(bcsfile.getCode(), true);
    decompiler.decompile();
    for (final Integer stringRef : decompiler.getStringRefsUsed()) {
      if (stringRef.intValue() == searchvalue)
        addHit(entry, null, null);
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
    Matcher m = NUMBERPATTERN.matcher(text.getText());
    while (m.find()) {
      long nr = Long.parseLong(text.getText().substring(m.start(), m.end()));
      if (nr == searchvalue)
        addHit(entry, null, null);
    }
  }
}

