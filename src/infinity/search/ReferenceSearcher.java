// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.search;

import infinity.datatype.ProRef;
import infinity.datatype.ResourceRef;
import infinity.resource.*;
import infinity.resource.bcs.*;
import infinity.resource.bcs.Compiler;
import infinity.resource.dlg.*;
import infinity.resource.key.ResourceEntry;
import infinity.resource.sav.SavResource;

import java.awt.*;
import java.util.List;

public final class ReferenceSearcher extends AbstractReferenceSearcher
{
  public ReferenceSearcher(ResourceEntry targetEntry, Component parent)
  {
    super(targetEntry, new String[]{"ARE", "BCS", "CHR", "CHU", "CRE", "DLG", "EFF", "GAM", "ITM",
                                    "PRO", "SAV", "SPL", "STO", "VVC", "WED", "WMP"}, parent);
  }

  protected void search(ResourceEntry entry, Resource resource)
  {
    if (resource instanceof DlgResource)
      searchDialog(entry, (AbstractStruct)resource);
    else if (resource instanceof SavResource)
      searchSave(entry, (SavResource)resource);
    else if (resource instanceof AbstractStruct)
      searchStruct(entry, (AbstractStruct)resource);
    else
      searchScript(entry, (BcsResource)resource);
  }

  private void searchDialog(ResourceEntry entry, AbstractStruct dialog)
  {
    for (int i = 0; i < dialog.getRowCount(); i++) {
      StructEntry o = dialog.getStructEntryAt(i);
      if (o instanceof ResourceRef &&
          ((ResourceRef)o).getResourceName().equalsIgnoreCase(targetEntry.toString()))
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
            for (final ResourceEntry resourceUsed : Decompiler.getResourcesUsed()) {
              if (targetEntry.toString().equalsIgnoreCase(resourceUsed.toString()))
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
      if (o instanceof ResourceRef &&
          ((ResourceRef)o).getResourceName().equalsIgnoreCase(targetEntry.toString()))
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
    for (final ResourceEntry resourceUsed : Decompiler.getResourcesUsed()) {
      if (resourceUsed == targetEntry)
        addHit(entry, null, null);
    }
  }

  private void searchStruct(ResourceEntry entry, AbstractStruct struct)
  {
    for (int i = 0; i < struct.getRowCount(); i++) {
      StructEntry o = struct.getStructEntryAt(i);
      if (o instanceof ResourceRef &&
          ((ResourceRef)o).getResourceName().equalsIgnoreCase(targetEntry.toString()))
        addHit(entry, entry.getSearchString(), o);
      else if (o instanceof ProRef && ((ProRef)o).getSelectedEntry() == targetEntry)
        addHit(entry, entry.getSearchString(), o);
      else if (o instanceof AbstractStruct)
        searchStruct(entry, (AbstractStruct)o);
    }
  }
}

