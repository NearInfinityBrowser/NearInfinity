// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.cre;

import infinity.NearInfinity;
import infinity.datatype.Bitmap;
import infinity.datatype.ColorValue;
import infinity.datatype.Datatype;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.HashBitmap;
import infinity.datatype.HexNumber;
import infinity.datatype.IdsBitmap;
import infinity.datatype.IdsFlag;
import infinity.datatype.KitIdsBitmap;
import infinity.datatype.ResourceRef;
import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.datatype.StringRef;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.datatype.UnsignDecNumber;
import infinity.gui.ButtonPanel;
import infinity.gui.ButtonPopupMenu;
import infinity.gui.StructViewer;
import infinity.gui.hexview.BasicColorMap;
import infinity.gui.hexview.HexViewer;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.Effect;
import infinity.resource.Effect2;
import infinity.resource.HasAddRemovable;
import infinity.resource.HasViewerTabs;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.are.AreResource;
import infinity.resource.gam.GamResource;
import infinity.resource.key.ResourceEntry;
import infinity.search.SearchOptions;
import infinity.util.DynamicArray;
import infinity.util.IdsMapCache;
import infinity.util.IdsMapEntry;
import infinity.util.LongIntegerHashMap;
import infinity.util.io.FileNI;
import infinity.util.io.FileOutputStreamNI;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

public final class CreResource extends AbstractStruct
  implements Resource, HasAddRemovable, AddRemovable, HasViewerTabs, ItemListener
{
  private static final LongIntegerHashMap<String> m_magetype = new LongIntegerHashMap<String>();
  private static final LongIntegerHashMap<String> m_colorPlacement = new LongIntegerHashMap<String>();
  public static final String s_flag[] = {"No flags set", "Identified", "No corpse", "Permanent corpse",
                                         "Original class: Fighter",
                                         "Original class: Mage", "Original class: Cleric",
                                         "Original class: Thief", "Original class: Druid",
                                         "Original class: Ranger", "Fallen paladin", "Fallen ranger",
                                         "Export allowed", "Hide status", "Large creature", "Moving between areas", "Been in party",
                                         "Holding item", "Clear all flags", "", "", "", "", "", "", "Allegiance tracking",
                                         "General tracking", "Race tracking", "Class tracking",
                                         "Specifics tracking", "Gender tracking", "Alignment tracking",
                                         "Uninterruptible"};
  public static final String s_feats1[] = {
    "No feats selected", "Aegis of rime", "Ambidexterity", "Aqua mortis", "Armor proficiency", "Armored arcana",
    "Arterial strike", "Blind fight", "Bullheaded", "Cleave", "Combat casting", "Courteous magocracy", "Crippling strike",
    "Dash", "Deflect arrows", "Dirty fighting", "Discipline", "Dodge", "Envenom weapon", "Exotic bastard",
    "Expertise", "Extra rage", "Extra shapeshifting", "Extra smiting", "Extra turning", "Fiendslayer",
    "Forester", "Great fortitude", "Hamstring", "Heretic's bane", "Heroic inspiration", "Improved critical",
    "Improved evasion"};
  public static final String s_feats2[] = {
    "No feats selected", "Improved initiative", "Improved turning", "Iron will", "Lightning reflexes",
    "Lingering song", "Luck of heroes", "Martial axe", "Martial bow", "Martial flail", "Martial greatsword",
    "Martial hammer", "Martial large sword", "Martial polearm", "Maximized attacks", "Mercantile background",
    "Power attack", "Precise shot", "Rapid shot", "Resist poison", "Scion of storms", "Shield proficiency",
    "Simple crossbow", "Simple mace", "Simple missile", "Simple quarterstaff", "Simple small blade",
    "Slippery mind", "Snake blood", "Spell focus enchantment", "Spell focus evocation", "Spell focus necromancy",
    "Spell focus transmutation"};
  public static final String s_feats3[] = {
    "No feats selected", "Spell penetration", "Spirit of flame", "Strong back", "Stunning fist",
    "Subvocal casting",
    "Toughness", "Two-weapon fighting", "Weapon finesse", "Wild shape boar", "Wild shape panther",
    "Wild shape shambler"};
  public static final String s_attributes_pst[] = {
    "No flags set", "", "Transparent", "", "", "Increment death variable", "Increment kill count",
    "Script name only", "Increment faction kills", "Increment team kills", "Invulnerable",
    "Good increment on death", "Law increment on death", "Lady increment on death", "Murder increment on death",
    "Don't face speaker", "Call for help", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "Died"};
  public static final String s_attributes_iwd2[] = {"No flags set", "Mental fortitude", "Critical hit immunity",
                                                    "Cannot be paladin", "Cannot be monk"};
  public static final String s_attacks[] = {"0", "1", "2", "3", "4", "5", "1/2", "3/2", "5/2", "7/2", "9/2"};
  public static final String s_noyes[] = {"No", "Yes"};
  public static final String s_visible[] = {"Shown", "Hidden"};

  static
  {
    m_magetype.put((long)0x0000, "None");
    m_magetype.put((long)0x0040, "Abjurer");
    m_magetype.put((long)0x0080, "Conjurer");
    m_magetype.put((long)0x0100, "Diviner");
    m_magetype.put((long)0x0200, "Enchanter");
    m_magetype.put((long)0x0400, "Illusionist");
    m_magetype.put((long)0x0800, "Invoker");
    m_magetype.put((long)0x1000, "Necromancer");
    m_magetype.put((long)0x2000, "Transmuter");
    m_magetype.put((long)0x4000, "Generalist");

    m_colorPlacement.put((long)0x80, "Metal");
    m_colorPlacement.put((long)0x81, "Metal (hologram)");
    m_colorPlacement.put((long)0x82, "Metal (pulsate)");
    m_colorPlacement.put((long)0x83, "Metal (hologram/pulsate)");
    m_colorPlacement.put((long)0x90, "Minor cloth");
    m_colorPlacement.put((long)0x91, "Minor cloth (hologram)");
    m_colorPlacement.put((long)0x92, "Minor cloth (pulsate)");
    m_colorPlacement.put((long)0x93, "Minor cloth (hologram/pulsate)");
    m_colorPlacement.put((long)0xA0, "Main cloth");
    m_colorPlacement.put((long)0xA1, "Main cloth (hologram)");
    m_colorPlacement.put((long)0xA2, "Main cloth (pulsate)");
    m_colorPlacement.put((long)0xA3, "Main cloth (hologram/pulsate)");
    m_colorPlacement.put((long)0xB0, "Skin");
    m_colorPlacement.put((long)0xB1, "Skin (hologram)");
    m_colorPlacement.put((long)0xB2, "Skin (pulsate)");
    m_colorPlacement.put((long)0xB3, "Skin (hologram/pulsate)");
    m_colorPlacement.put((long)0xC0, "Leather");
    m_colorPlacement.put((long)0xC1, "Leather (hologram)");
    m_colorPlacement.put((long)0xC2, "Leather (pulsate)");
    m_colorPlacement.put((long)0xC3, "Leather (hologram/pulsate)");
    m_colorPlacement.put((long)0xD0, "Armor");
    m_colorPlacement.put((long)0xD1, "Armor (hologram)");
    m_colorPlacement.put((long)0xD2, "Armor (pulsate)");
    m_colorPlacement.put((long)0xD3, "Armor (hologram/pulsate)");
    m_colorPlacement.put((long)0xE0, "Hair");
    m_colorPlacement.put((long)0xE1, "Hair (hologram)");
    m_colorPlacement.put((long)0xE2, "Hair (pulsate)");
    m_colorPlacement.put((long)0xE3, "Hair (hologram/pulsate)");
    m_colorPlacement.put((long)0x00, "Not used");
  }

  private boolean isChr;
  private JMenuItem miExport, miConvert;
  private ButtonPopupMenu bExport;
  private HexViewer hexViewer;
  private Boolean hasRawTab;

  public static void addScriptName(Map<String, Set<ResourceEntry>> scriptNames,
                                   ResourceEntry entry)
  {
    try {
      byte[] buffer = entry.getResourceData();
      String signature = new String(buffer, 0, 4);
      String scriptName = "";
      if (signature.equalsIgnoreCase("CRE ")) {
        String version = new String(buffer, 4, 4);
        if (version.equalsIgnoreCase("V1.0"))
          scriptName = DynamicArray.getString(buffer, 640, 32);
        else if (version.equalsIgnoreCase("V1.1") || version.equalsIgnoreCase("V1.2"))
          scriptName = DynamicArray.getString(buffer, 804, 32);
        else if (version.equalsIgnoreCase("V2.2"))
          scriptName = DynamicArray.getString(buffer, 916, 32);
        else if (version.equalsIgnoreCase("V9.0"))
          scriptName = DynamicArray.getString(buffer, 744, 32);
        if (scriptName.equals("") || scriptName.equalsIgnoreCase("None"))
          return;
        // Apparently script name is the only thing that matters
  //        scriptName = entry.toString().substring(0, entry.toString().length() - 4);
        else {
          scriptName = scriptName.toLowerCase().replaceAll(" ", "");
          if (scriptNames.containsKey(scriptName)) {
            Set<ResourceEntry> entries = scriptNames.get(scriptName);
            entries.add(entry);
          }
          else {
            Set<ResourceEntry> entries = new HashSet<ResourceEntry>();
            entries.add(entry);
            scriptNames.put(scriptName, entries);
          }
        }
      }
    } catch (Exception e) {}
  }

  private static void adjustEntryOffsets(AbstractStruct struct, int amount)
  {
    for (int i = 0; i < struct.getFieldCount(); i++) {
      StructEntry structEntry = struct.getField(i);
      structEntry.setOffset(structEntry.getOffset() + amount);
      if (structEntry instanceof AbstractStruct)
        adjustEntryOffsets((AbstractStruct)structEntry, amount);
    }
  }

  public static void convertCHRtoCRE(ResourceEntry resourceEntry)
  {
    if (!resourceEntry.getExtension().equalsIgnoreCase("CHR"))
      return;
    String resourcename = resourceEntry.toString();
    resourcename = resourcename.substring(0, resourcename.lastIndexOf(".")) + ".CRE";
    JFileChooser chooser = new JFileChooser(ResourceFactory.getRootDir());
    chooser.setDialogTitle("Convert CHR to CRE");
    chooser.setSelectedFile(new FileNI(resourcename));
    if (chooser.showSaveDialog(NearInfinity.getInstance()) == JFileChooser.APPROVE_OPTION) {
      File output = chooser.getSelectedFile();
      if (output.exists()) {
        String options[] = {"Overwrite", "Cancel"};
        int result = JOptionPane.showOptionDialog(NearInfinity.getInstance(), output + " exists. Overwrite?",
                                                  "Save resource", JOptionPane.YES_NO_OPTION,
                                                  JOptionPane.WARNING_MESSAGE, null, options, options[0]);
        if (result != 0) return;
      }
      try {
        CreResource crefile = (CreResource)ResourceFactory.getResource(resourceEntry);
        while (!crefile.getField(0).toString().equals("CRE "))
          crefile.removeField(0);
        convertToSemiStandard(crefile);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStreamNI(output));
        crefile.write(bos);
        bos.close();
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "File saved to " + output,
                                      "Conversion complete", JOptionPane.INFORMATION_MESSAGE);
      } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Errors during conversion",
                                      "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private static void convertToSemiStandard(CreResource crefile)
  {
    if (!crefile.getField(1).toString().equals("V1.0")) {
      System.err.println("Conversion to semi-standard aborted: Unsupported CRE version");
      return;
    }

    // Order:
    //  KnownSpell
    //  SpellMemorizationInfo
    //  MemorizedSpell
    //  Effects
    //  Items
    //  ItemSlots

    // Adjust offsets first - Size of CHR header = 0x64
    adjustEntryOffsets(crefile, -0x64);

    SectionOffset knownspells_offset = (SectionOffset)crefile.getAttribute("Known spells offset");
    SectionOffset memspellinfo_offset = (SectionOffset)crefile.getAttribute("Memorization info offset");
    SectionOffset memspells_offset = (SectionOffset)crefile.getAttribute("Memorized spells offset");
    SectionOffset itemslots_offset = (SectionOffset)crefile.getAttribute("Item slots offset");
    SectionOffset items_offset = (SectionOffset)crefile.getAttribute("Items offset");
    SectionOffset effects_offset = (SectionOffset)crefile.getAttribute("Effects offset");

    int indexStructs = crefile.getIndexOf(effects_offset) + 3; // Start of non-permanent section
    List<StructEntry> newlist = new ArrayList<StructEntry>(crefile.getFieldCount());
    for (int i = 0; i < indexStructs; i++)
      newlist.add(crefile.getField(i));

    int offsetStructs = 0x2d4;
    knownspells_offset.setValue(offsetStructs);
    offsetStructs = copyStruct(crefile.getList(), newlist, indexStructs, offsetStructs, KnownSpells.class);

    memspellinfo_offset.setValue(offsetStructs);
    offsetStructs = copyStruct(crefile.getList(), newlist, indexStructs, offsetStructs, SpellMemorization.class);

    memspells_offset.setValue(offsetStructs);
    // XXX: mem spells are not directly stored in crefile.list
    // and added by addFlatList on the Spell Memorization entries
    // (but the offsets are wrong, so we need to realign them with copyStruct)
    List<StructEntry> trashlist = new ArrayList<StructEntry>();
    for (int i = indexStructs; i < crefile.getFieldCount(); i++) {
      StructEntry entry = crefile.getField(i);
      if (entry instanceof SpellMemorization) {
        offsetStructs = copyStruct(((SpellMemorization)entry).getList(), trashlist, 0, offsetStructs, MemorizedSpells.class);
      }
    }

    effects_offset.setValue(offsetStructs);
    offsetStructs =
    copyStruct(crefile.getList(), newlist, indexStructs, offsetStructs, effects_offset.getSection());

    items_offset.setValue(offsetStructs);
    offsetStructs = copyStruct(crefile.getList(), newlist, indexStructs, offsetStructs, Item.class);

    itemslots_offset.setValue(offsetStructs);
    offsetStructs = copyStruct(crefile.getList(), newlist, indexStructs, offsetStructs, DecNumber.class);
    copyStruct(crefile.getList(), newlist, indexStructs, offsetStructs, Unknown.class);

    crefile.setList(newlist);
  }

  private static int copyStruct(List<StructEntry> oldlist, List<StructEntry> newlist,
                                int indexStructs, int offsetStructs,
                                Class<? extends StructEntry> copyClass)
  {
    for (int i = indexStructs; i < oldlist.size(); i++) {
      StructEntry structEntry = oldlist.get(i);
      if (structEntry.getClass() == copyClass) {
        structEntry.setOffset(offsetStructs);
        if (structEntry instanceof AbstractStruct)
          ((AbstractStruct)structEntry).realignStructOffsets();
        offsetStructs += structEntry.getSize();
        newlist.add(structEntry);
      }
    }
    return offsetStructs;
  }

  public static String getSearchString(byte buffer[])
  {
    String signature = new String(buffer, 0, 4);
    if (signature.equalsIgnoreCase("CHR "))
      return new String(buffer, 8, 32);
    String name = new StringRef(buffer, 8, "").toString().trim();
    String shortname = new StringRef(buffer, 12, "").toString().trim();
    if (name.equals(shortname))
      return name;
    return name + " - " + shortname;
  }

  public CreResource(ResourceEntry entry) throws Exception
  {
    super(entry);
    isChr = entry.getExtension().equalsIgnoreCase("CHR");
  }

  public CreResource(AbstractStruct superStruct, String name, byte data[], int startoffset) throws Exception
  {
    super(superStruct, name, data, startoffset);
    isChr = new String(data, startoffset, 4).equalsIgnoreCase("CHR ");
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  @Override
  public AddRemovable[] getAddRemovables() throws Exception
  {
    DecNumber effectFlag = (DecNumber)getAttribute("Effect flag");
    if (ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2) {
      if (effectFlag.getValue() == 1)
        return new AddRemovable[]{new Item(), new Effect2()};
      else
        return new AddRemovable[]{new Item(), new Effect()};
    }
    else {
      if (effectFlag.getValue() == 1)
        return new AddRemovable[]{new Item(), new Effect2(), new KnownSpells(), new SpellMemorization()};
      else
        return new AddRemovable[]{new Item(), new Effect(), new KnownSpells(), new SpellMemorization()};
    }
  }

// --------------------- End Interface HasAddRemovable ---------------------


//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------


// --------------------- Begin Interface HasViewerTabs ---------------------

  @Override
  public int getViewerTabCount()
  {
    return showRawTab() ? 2 : 1;
  }

  @Override
  public String getViewerTabName(int index)
  {
    switch (index) {
      case 0:
        return StructViewer.TAB_VIEW;
      case 1:
        return showRawTab() ? StructViewer.TAB_RAW : null;
    }
    return null;
  }

  @Override
  public JComponent getViewerTab(int index)
  {
    switch (index) {
      case 0:
        return new Viewer(this);
      case 1:
        if (showRawTab() && hexViewer == null) {
          hexViewer = new HexViewer(this, new BasicColorMap(this, true));
        }
        return hexViewer;
    }
    return null;
  }

  @Override
  public boolean viewerTabAddedBefore(int index)
  {
    return (index == 0);
  }

  // Needed for embedded CRE resources
  private boolean showRawTab()
  {
    if (hasRawTab == null) {
      hasRawTab = !(Boolean.valueOf(this.isChildOf(GamResource.class)) ||
                    Boolean.valueOf(this.isChildOf(AreResource.class)));
    }
    return hasRawTab.booleanValue();
  }

// --------------------- End Interface HasViewerTabs ---------------------


// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    super.writeFlatList(os);
  }


// --------------------- End Interface Writeable ---------------------

  @Override
  protected void viewerInitialized(StructViewer viewer)
  {
    viewer.addTabChangeListener(hexViewer);
    if (isChr) {
      ButtonPanel panel = viewer.getButtonPanel();
      JButton b = (JButton)panel.getControlByType(ButtonPanel.Control.ExportButton);
      int idx = panel.getControlPosition(b);
      if (b != null && idx >= 0) {
        // replacing button with menu
        b.removeActionListener(viewer);
        panel.removeControl(idx);
        miExport = new JMenuItem("original");
        miExport.setToolTipText(b.getToolTipText());
        miConvert = new JMenuItem("as CRE");
        bExport = (ButtonPopupMenu)panel.addControl(idx, ButtonPanel.Control.ExportMenu);
        bExport.setMenuItems(new JMenuItem[]{miExport, miConvert});
        bExport.addItemListener(this);
      }
    }
  }

  @Override
  protected void datatypeAdded(AddRemovable datatype)
  {
    updateOffsets(datatype, datatype.getSize());
    if (datatype instanceof SpellMemorization)
      updateMemorizedSpells();
    hexViewer.dataModified();
  }

  @Override
  protected void datatypeAddedInChild(AbstractStruct child, AddRemovable datatype)
  {
    updateOffsets(datatype, datatype.getSize());
    if (datatype instanceof MemorizedSpells)
      updateMemorizedSpells();
    super.datatypeAddedInChild(child, datatype);
    hexViewer.dataModified();
  }

  @Override
  protected void datatypeRemoved(AddRemovable datatype)
  {
    updateOffsets(datatype, -datatype.getSize());
    if (datatype instanceof SpellMemorization)
      updateMemorizedSpells();
    hexViewer.dataModified();
  }

  @Override
  protected void datatypeRemovedInChild(AbstractStruct child, AddRemovable datatype)
  {
    updateOffsets(datatype, -datatype.getSize());
    if (datatype instanceof MemorizedSpells)
      updateMemorizedSpells();
    super.datatypeRemovedInChild(child, datatype);
    hexViewer.dataModified();
  }

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    setExtraOffset(getExtraOffset() + offset);
    TextString signature = new TextString(buffer, offset, 4, "Signature");
    addField(signature);
    TextString version = new TextString(buffer, offset + 4, 4, "Version");
    addField(version);
    if (signature.toString().equalsIgnoreCase("CHR ")) {
      addField(new TextString(buffer, offset + 8, 32, "Character name"));
      HexNumber structOffset = new HexNumber(buffer, offset + 40, 4, "CRE structure offset");
      addField(structOffset);
      addField(new HexNumber(buffer, offset + 44, 4, "CRE structure length"));
      if (version.toString().equalsIgnoreCase("V2.2")) {
        addField(new IdsBitmap(buffer, offset + 48, 2, "Quick weapon slot 1", "SLOTS.IDS"));
        addField(new IdsBitmap(buffer, offset + 50, 2, "Quick shield slot 1", "SLOTS.IDS"));
        addField(new IdsBitmap(buffer, offset + 52, 2, "Quick weapon slot 2", "SLOTS.IDS"));
        addField(new IdsBitmap(buffer, offset + 54, 2, "Quick shield slot 2", "SLOTS.IDS"));
        addField(new IdsBitmap(buffer, offset + 56, 2, "Quick weapon slot 3", "SLOTS.IDS"));
        addField(new IdsBitmap(buffer, offset + 58, 2, "Quick shield slot 3", "SLOTS.IDS"));
        addField(new IdsBitmap(buffer, offset + 60, 2, "Quick weapon slot 4", "SLOTS.IDS"));
        addField(new IdsBitmap(buffer, offset + 62, 2, "Quick shield slot 4", "SLOTS.IDS"));
        addField(new DecNumber(buffer, offset + 64, 2, "Quick weapon 1 ability"));
        addField(new DecNumber(buffer, offset + 66, 2, "Quick shield 1 ability"));
        addField(new DecNumber(buffer, offset + 68, 2, "Quick weapon 2 ability"));
        addField(new DecNumber(buffer, offset + 70, 2, "Quick shield 2 ability"));
        addField(new DecNumber(buffer, offset + 72, 2, "Quick weapon 3 ability"));
        addField(new DecNumber(buffer, offset + 74, 2, "Quick shield 3 ability"));
        addField(new DecNumber(buffer, offset + 76, 2, "Quick weapon 4 ability"));
        addField(new DecNumber(buffer, offset + 78, 2, "Quick shield 4 ability"));
        addField(new ResourceRef(buffer, offset + 80, "Quick spell 1", "SPL"));
        addField(new ResourceRef(buffer, offset + 88, "Quick spell 2", "SPL"));
        addField(new ResourceRef(buffer, offset + 96, "Quick spell 3", "SPL"));
        addField(new ResourceRef(buffer, offset + 104, "Quick spell 4", "SPL"));
        addField(new ResourceRef(buffer, offset + 112, "Quick spell 5", "SPL"));
        addField(new ResourceRef(buffer, offset + 120, "Quick spell 6", "SPL"));
        addField(new ResourceRef(buffer, offset + 128, "Quick spell 7", "SPL"));
        addField(new ResourceRef(buffer, offset + 136, "Quick spell 8", "SPL"));
        addField(new ResourceRef(buffer, offset + 144, "Quick spell 9", "SPL"));
        addField(new IdsBitmap(buffer, offset + 152, 1, "Quick spell 1 class", "CLASS.IDS"));
        addField(new IdsBitmap(buffer, offset + 153, 1, "Quick spell 2 class", "CLASS.IDS"));
        addField(new IdsBitmap(buffer, offset + 154, 1, "Quick spell 3 class", "CLASS.IDS"));
        addField(new IdsBitmap(buffer, offset + 155, 1, "Quick spell 4 class", "CLASS.IDS"));
        addField(new IdsBitmap(buffer, offset + 156, 1, "Quick spell 5 class", "CLASS.IDS"));
        addField(new IdsBitmap(buffer, offset + 157, 1, "Quick spell 6 class", "CLASS.IDS"));
        addField(new IdsBitmap(buffer, offset + 158, 1, "Quick spell 7 class", "CLASS.IDS"));
        addField(new IdsBitmap(buffer, offset + 159, 1, "Quick spell 8 class", "CLASS.IDS"));
        addField(new IdsBitmap(buffer, offset + 160, 1, "Quick spell 9 class", "CLASS.IDS"));
        addField(new Unknown(buffer, offset + 161, 1));
        addField(new IdsBitmap(buffer, offset + 162, 2, "Quick item slot 1", "SLOTS.IDS"));
        addField(new IdsBitmap(buffer, offset + 164, 2, "Quick item slot 2", "SLOTS.IDS"));
        addField(new IdsBitmap(buffer, offset + 166, 2, "Quick item slot 3", "SLOTS.IDS"));
        addField(new DecNumber(buffer, offset + 168, 2, "Quick item 1 ability"));
        addField(new DecNumber(buffer, offset + 170, 2, "Quick item 2 ability"));
        addField(new DecNumber(buffer, offset + 172, 2, "Quick item 3 ability"));
        addField(new ResourceRef(buffer, offset + 174, "Quick ability 1", "SPL"));
        addField(new ResourceRef(buffer, offset + 182, "Quick ability 2", "SPL"));
        addField(new ResourceRef(buffer, offset + 190, "Quick ability 3", "SPL"));
        addField(new ResourceRef(buffer, offset + 198, "Quick ability 4", "SPL"));
        addField(new ResourceRef(buffer, offset + 206, "Quick ability 5", "SPL"));
        addField(new ResourceRef(buffer, offset + 214, "Quick ability 6", "SPL"));
        addField(new ResourceRef(buffer, offset + 222, "Quick ability 7", "SPL"));
        addField(new ResourceRef(buffer, offset + 230, "Quick ability 8", "SPL"));
        addField(new ResourceRef(buffer, offset + 238, "Quick ability 9", "SPL"));
        addField(new ResourceRef(buffer, offset + 246, "Quick song 1", "SPL"));
        addField(new ResourceRef(buffer, offset + 254, "Quick song 2", "SPL"));
        addField(new ResourceRef(buffer, offset + 262, "Quick song 3", "SPL"));
        addField(new ResourceRef(buffer, offset + 270, "Quick song 4", "SPL"));
        addField(new ResourceRef(buffer, offset + 278, "Quick song 5", "SPL"));
        addField(new ResourceRef(buffer, offset + 286, "Quick song 6", "SPL"));
        addField(new ResourceRef(buffer, offset + 294, "Quick song 7", "SPL"));
        addField(new ResourceRef(buffer, offset + 302, "Quick song 8", "SPL"));
        addField(new ResourceRef(buffer, offset + 310, "Quick song 9", "SPL"));
        addField(new DecNumber(buffer, offset + 318, 4, "Quick button 1"));
        addField(new DecNumber(buffer, offset + 322, 4, "Quick button 2"));
        addField(new DecNumber(buffer, offset + 326, 4, "Quick button 3"));
        addField(new DecNumber(buffer, offset + 330, 4, "Quick button 4"));
        addField(new DecNumber(buffer, offset + 334, 4, "Quick button 5"));
        addField(new DecNumber(buffer, offset + 338, 4, "Quick button 6"));
        addField(new DecNumber(buffer, offset + 342, 4, "Quick button 7"));
        addField(new DecNumber(buffer, offset + 346, 4, "Quick button 8"));
        addField(new DecNumber(buffer, offset + 350, 4, "Quick button 9"));
        addField(new Unknown(buffer, offset + 354, 26));
        addField(new TextString(buffer, offset + 380, 8, "Voice set prefix"));
        addField(new TextString(buffer, offset + 388, 32, "Voice set"));
        addField(new Unknown(buffer, offset + 420, 128));
      }
      else if (version.toString().equalsIgnoreCase("V1.0") || version.toString().equalsIgnoreCase("V2.0")) {
        addField(new IdsBitmap(buffer, offset + 48, 2, "Quick weapon slot 1", "SLOTS.IDS"));
        addField(new IdsBitmap(buffer, offset + 50, 2, "Quick weapon slot 2", "SLOTS.IDS"));
        addField(new IdsBitmap(buffer, offset + 52, 2, "Quick weapon slot 3", "SLOTS.IDS"));
        addField(new IdsBitmap(buffer, offset + 54, 2, "Quick weapon slot 4", "SLOTS.IDS"));
        addField(new DecNumber(buffer, offset + 56, 2, "Quick weapon 1 ability"));
        addField(new DecNumber(buffer, offset + 58, 2, "Quick weapon 2 ability"));
        addField(new DecNumber(buffer, offset + 60, 2, "Quick weapon 3 ability"));
        addField(new DecNumber(buffer, offset + 62, 2, "Quick weapon 4 ability"));
        addField(new ResourceRef(buffer, offset + 64, "Quick spell 1", "SPL"));
        addField(new ResourceRef(buffer, offset + 72, "Quick spell 2", "SPL"));
        addField(new ResourceRef(buffer, offset + 80, "Quick spell 3", "SPL"));
        addField(new IdsBitmap(buffer, offset + 88, 2, "Quick item slot 1", "SLOTS.IDS"));
        addField(new IdsBitmap(buffer, offset + 90, 2, "Quick item slot 2", "SLOTS.IDS"));
        addField(new IdsBitmap(buffer, offset + 92, 2, "Quick item slot 3", "SLOTS.IDS"));
        addField(new DecNumber(buffer, offset + 94, 2, "Quick item 1 ability"));
        addField(new DecNumber(buffer, offset + 96, 2, "Quick item 2 ability"));
        addField(new DecNumber(buffer, offset + 98, 2, "Quick item 3 ability"));
      }
      else {
        addField(new Unknown(buffer, offset + 48, structOffset.getValue() - 48));
      }
      offset = structOffset.getValue();
      addField(new TextString(buffer, offset, 4, "Signature 2"));
      version = new TextString(buffer, offset + 4, 4, "Version 2");
      addField(version);
      setExtraOffset(getExtraOffset() + structOffset.getValue());
    }
    offset += 8;
    if (version.toString().equalsIgnoreCase("V2.2"))
      return readIWD2(buffer, offset);
    return readOther(version.toString(), buffer, offset);
  }

  ////////////////////////
  // Icewind Dale 2
  ////////////////////////

  private int readIWD2(byte buffer[], int offset) throws Exception
  {
    addField(new StringRef(buffer, offset, "Name"));
    addField(new StringRef(buffer, offset + 4, "Tooltip"));
    addField(new Flag(buffer, offset + 8, 4, "Flags", s_flag)); // ToDo: figure these out whenever
    addField(new DecNumber(buffer, offset + 12, 4, "XP value"));
    addField(new DecNumber(buffer, offset + 16, 4, "XP"));
    addField(new DecNumber(buffer, offset + 20, 4, "Gold"));
    addField(new IdsFlag(buffer, offset + 24, 4, "Status", "STATE.IDS"));
    addField(new DecNumber(buffer, offset + 28, 2, "Current HP"));
    addField(new DecNumber(buffer, offset + 30, 2, "Maximum HP"));
    addField(new IdsBitmap(buffer, offset + 32, 4, "Animation", "ANIMATE.IDS"));
//    addField(new Unknown(buffer, offset + 34, 2));
    addField(new ColorValue(buffer, offset + 36, 1, "Metal color"));
    addField(new ColorValue(buffer, offset + 37, 1, "Minor color"));
    addField(new ColorValue(buffer, offset + 38, 1, "Major color"));
    addField(new ColorValue(buffer, offset + 39, 1, "Skin color"));
    addField(new ColorValue(buffer, offset + 40, 1, "Leather color"));
    addField(new ColorValue(buffer, offset + 41, 1, "Armor color"));
    addField(new ColorValue(buffer, offset + 42, 1, "Hair color"));
    DecNumber effect_flag = (DecNumber)addField(new DecNumber(buffer, offset + 43, 1, "Effect flag"));
    addField(new ResourceRef(buffer, offset + 44, "Small portrait", "BMP"));
    addField(new ResourceRef(buffer, offset + 52, "Large portrait", "BMP"));
    addField(new DecNumber(buffer, offset + 60, 1, "Reputation"));
    addField(new Unknown(buffer, offset + 61, 1));
    addField(new DecNumber(buffer, offset + 62, 2, "Armor class"));
    addField(new DecNumber(buffer, offset + 64, 2, "Bludgeoning AC modifier"));
    addField(new DecNumber(buffer, offset + 66, 2, "Missile AC modifier"));
    addField(new DecNumber(buffer, offset + 68, 2, "Piercing AC modifier"));
    addField(new DecNumber(buffer, offset + 70, 2, "Slashing AC modifier"));
    addField(new DecNumber(buffer, offset + 72, 1, "Base attack bonus"));
    addField(new DecNumber(buffer, offset + 73, 1, "# attacks/round"));
    addField(new DecNumber(buffer, offset + 74, 1, "Fortitude save"));
    addField(new DecNumber(buffer, offset + 75, 1, "Reflex save"));
    addField(new DecNumber(buffer, offset + 76, 1, "Will save"));
    addField(new DecNumber(buffer, offset + 77, 1, "Fire resistance"));
    addField(new DecNumber(buffer, offset + 78, 1, "Cold resistance"));
    addField(new DecNumber(buffer, offset + 79, 1, "Electricity resistance"));
    addField(new DecNumber(buffer, offset + 80, 1, "Acid resistance"));
    addField(new DecNumber(buffer, offset + 81, 1, "Spell resistance"));
    addField(new DecNumber(buffer, offset + 82, 1, "Magic fire resistance"));
    addField(new DecNumber(buffer, offset + 83, 1, "Magic cold resistance"));
    addField(new DecNumber(buffer, offset + 84, 1, "Slashing resistance"));
    addField(new DecNumber(buffer, offset + 85, 1, "Bludgeoning resistance"));
    addField(new DecNumber(buffer, offset + 86, 1, "Piercing resistance"));
    addField(new DecNumber(buffer, offset + 87, 1, "Missile resistance"));
    addField(new DecNumber(buffer, offset + 88, 1, "Magic damage resistance"));

    addField(new Unknown(buffer, offset + 89, 4));
    addField(new DecNumber(buffer, offset + 93, 1, "Fatigue"));
    addField(new DecNumber(buffer, offset + 94, 1, "Intoxication"));
    addField(new DecNumber(buffer, offset + 95, 1, "Luck"));
    addField(new DecNumber(buffer, offset + 96, 1, "Turn undead level"));
    addField(new Unknown(buffer, offset + 97, 33));

    addField(new DecNumber(buffer, offset + 130, 1, "Total level"));
    addField(new DecNumber(buffer, offset + 131, 1, "Barbarian level"));
    addField(new DecNumber(buffer, offset + 132, 1, "Bard level"));
    addField(new DecNumber(buffer, offset + 133, 1, "Cleric level"));
    addField(new DecNumber(buffer, offset + 134, 1, "Druid level"));
    addField(new DecNumber(buffer, offset + 135, 1, "Fighter level"));
    addField(new DecNumber(buffer, offset + 136, 1, "Monk level"));
    addField(new DecNumber(buffer, offset + 137, 1, "Paladin level"));
    addField(new DecNumber(buffer, offset + 138, 1, "Ranger level"));
    addField(new DecNumber(buffer, offset + 139, 1, "Rogue level"));
    addField(new DecNumber(buffer, offset + 140, 1, "Sorcerer level"));
    addField(new DecNumber(buffer, offset + 141, 1, "Wizard level"));
    addField(new Unknown(buffer, offset + 142, 22));

    LongIntegerHashMap<IdsMapEntry> sndmap = null;
    if (ResourceFactory.getInstance().resourceExists("SOUNDOFF.IDS")) {
      sndmap = IdsMapCache.get("SOUNDOFF.IDS").getMap();
    }
    if (sndmap != null) {
      for (int i = 0; i < 64; i++) {
        if (sndmap.containsKey((long)i)) {
          addField(new StringRef(buffer, offset + 164 + 4 * i,
                                "Sound: " + ((IdsMapEntry)sndmap.get((long)i)).getString()));
        } else {
          addField(new StringRef(buffer, offset + 164 + 4 * i, "Sound: Unknown"));
        }
      }
    }
    else {
      for (int i = 0; i < 64; i++) {
        addField(new StringRef(buffer, offset + 164 + 4 * i, "Soundset string"));
      }
    }

    addField(new ResourceRef(buffer, offset + 420, "Team script", "BCS"));
    addField(new ResourceRef(buffer, offset + 428, "Special script 1", "BCS"));
    addField(new Unknown(buffer, offset + 436, 4));
    addField(new Flag(buffer, offset + 440, 4, "Feats (1/3)", s_feats1));
    addField(new Flag(buffer, offset + 444, 4, "Feats (2/3)", s_feats2));
    addField(new Flag(buffer, offset + 448, 4, "Feats (3/3)", s_feats3));
    addField(new Unknown(buffer, offset + 452, 12));
    addField(new DecNumber(buffer, offset + 464, 1, "MW: Bow"));
    addField(new DecNumber(buffer, offset + 465, 1, "SW: Crossbow"));
    addField(new DecNumber(buffer, offset + 466, 1, "SW: Missile"));
    addField(new DecNumber(buffer, offset + 467, 1, "MW: Axe"));
    addField(new DecNumber(buffer, offset + 468, 1, "SW: Mace"));
    addField(new DecNumber(buffer, offset + 469, 1, "MW: Flail"));
    addField(new DecNumber(buffer, offset + 470, 1, "MW: Polearm"));
    addField(new DecNumber(buffer, offset + 471, 1, "MW: Hammer"));
    addField(new DecNumber(buffer, offset + 472, 1, "SW: Quarterstaff"));
    addField(new DecNumber(buffer, offset + 473, 1, "MW: Greatsword"));
    addField(new DecNumber(buffer, offset + 474, 1, "MW: Large sword"));
    addField(new DecNumber(buffer, offset + 475, 1, "SW: Small blade"));
    addField(new DecNumber(buffer, offset + 476, 1, "Toughness"));
    addField(new DecNumber(buffer, offset + 477, 1, "Armored arcana"));
    addField(new DecNumber(buffer, offset + 478, 1, "Cleave"));
    addField(new DecNumber(buffer, offset + 479, 1, "Armor proficiency"));
    addField(new DecNumber(buffer, offset + 480, 1, "SF: Enchantment"));
    addField(new DecNumber(buffer, offset + 481, 1, "SF: Evocation"));
    addField(new DecNumber(buffer, offset + 482, 1, "SF: Necromancy"));
    addField(new DecNumber(buffer, offset + 483, 1, "SF: Transmutation"));
    addField(new DecNumber(buffer, offset + 484, 1, "Spell penetration"));
    addField(new DecNumber(buffer, offset + 485, 1, "Extra rage"));
    addField(new DecNumber(buffer, offset + 486, 1, "Extra wild shape"));
    addField(new DecNumber(buffer, offset + 487, 1, "Extra smiting"));
    addField(new DecNumber(buffer, offset + 488, 1, "Extra turning"));
    addField(new DecNumber(buffer, offset + 489, 1, "EW: Bastard sword"));
    addField(new Unknown(buffer, offset + 490, 38));
    addField(new DecNumber(buffer, offset + 528, 1, "Alchemy"));
    addField(new DecNumber(buffer, offset + 529, 1, "Animal empathy"));
    addField(new DecNumber(buffer, offset + 530, 1, "Bluff"));
    addField(new DecNumber(buffer, offset + 531, 1, "Concentration"));
    addField(new DecNumber(buffer, offset + 532, 1, "Diplomacy"));
    addField(new DecNumber(buffer, offset + 533, 1, "Disable device"));
    addField(new DecNumber(buffer, offset + 534, 1, "Hide"));
    addField(new DecNumber(buffer, offset + 535, 1, "Intimidate"));
    addField(new DecNumber(buffer, offset + 536, 1, "Knowledge (arcana)"));
    addField(new DecNumber(buffer, offset + 537, 1, "Move silently"));
    addField(new DecNumber(buffer, offset + 538, 1, "Open lock"));
    addField(new DecNumber(buffer, offset + 539, 1, "Pick pocket"));
    addField(new DecNumber(buffer, offset + 540, 1, "Search"));
    addField(new DecNumber(buffer, offset + 541, 1, "Spellcraft"));
    addField(new DecNumber(buffer, offset + 542, 1, "Use magic device"));
    addField(new DecNumber(buffer, offset + 543, 1, "Wilderness lore"));
    addField(new Unknown(buffer, offset + 544, 50));
    addField(new DecNumber(buffer, offset + 594, 1, "Challenge rating"));
    addField(new IdsBitmap(buffer, offset + 595, 1, "Favored enemy 1", "RACE.IDS"));
    addField(new IdsBitmap(buffer, offset + 596, 1, "Favored enemy 2", "RACE.IDS"));
    addField(new IdsBitmap(buffer, offset + 597, 1, "Favored enemy 3", "RACE.IDS"));
    addField(new IdsBitmap(buffer, offset + 598, 1, "Favored enemy 4", "RACE.IDS"));
    addField(new IdsBitmap(buffer, offset + 599, 1, "Favored enemy 5", "RACE.IDS"));
    addField(new IdsBitmap(buffer, offset + 600, 1, "Favored enemy 6", "RACE.IDS"));
    addField(new IdsBitmap(buffer, offset + 601, 1, "Favored enemy 7", "RACE.IDS"));
    addField(new IdsBitmap(buffer, offset + 602, 1, "Favored enemy 8", "RACE.IDS"));
    addField(new Bitmap(buffer, offset + 603, 1, "Subrace",
                        new String[]{"Pureblood", "Aamimar/Drow/Gold dwarf/Strongheart halfling/Deep gnome",
                                     "Tiefling/Wild elf/Gray dwarf/Ghostwise halfling"}));
    addField(new Unknown(buffer, offset + 604, 1));
    addField(new IdsBitmap(buffer, offset + 605, 1, "Sex", "GENDER.IDS"));
    addField(new DecNumber(buffer, offset + 606, 1, "Strength"));
    addField(new DecNumber(buffer, offset + 607, 1, "Intelligence"));
    addField(new DecNumber(buffer, offset + 608, 1, "Wisdom"));
    addField(new DecNumber(buffer, offset + 609, 1, "Dexterity"));
    addField(new DecNumber(buffer, offset + 610, 1, "Constitution"));
    addField(new DecNumber(buffer, offset + 611, 1, "Charisma"));
    addField(new DecNumber(buffer, offset + 612, 1, "Morale"));
    addField(new DecNumber(buffer, offset + 613, 1, "Morale break"));
    addField(new DecNumber(buffer, offset + 614, 2, "Morale recovery"));
    addField(new IdsBitmap(buffer, offset + 616, 4, "Kit", "KIT.IDS"));
    addField(new ResourceRef(buffer, offset + 620, "Override script", "BCS"));
    addField(new ResourceRef(buffer, offset + 628, "Special script 2", "BCS"));
    addField(new ResourceRef(buffer, offset + 636, "Combat script", "BCS"));
    addField(new ResourceRef(buffer, offset + 644, "Special script 3", "BCS"));
    addField(new ResourceRef(buffer, offset + 652, "Movement script", "BCS"));
    addField(new Bitmap(buffer, offset + 660, 1, "Default visibility", s_visible));
    addField(new Bitmap(buffer, offset + 661, 1, "Set extra death variable?", s_noyes));
    addField(new Bitmap(buffer, offset + 662, 1, "Increment kill count?", s_noyes));
    addField(new Unknown(buffer, offset + 663, 1));
    addField(new DecNumber(buffer, offset + 664, 2, "Internal 1"));
    addField(new DecNumber(buffer, offset + 666, 2, "Internal 2"));
    addField(new DecNumber(buffer, offset + 668, 2, "Internal 3"));
    addField(new DecNumber(buffer, offset + 670, 2, "Internal 4"));
    addField(new DecNumber(buffer, offset + 672, 2, "Internal 5"));
    addField(new TextString(buffer, offset + 674, 32, "Death variable (set)"));
    addField(new TextString(buffer, offset + 706, 32, "Death variable (increment)"));
    addField(new Bitmap(buffer, offset + 738, 2, "Location saved?", s_noyes));
    addField(new DecNumber(buffer, offset + 740, 2, "Saved location: X"));
    addField(new DecNumber(buffer, offset + 742, 2, "Saved location: Y"));
    addField(new DecNumber(buffer, offset + 744, 2, "Saved orientation"));
    addField(new Unknown(buffer, offset + 746, 15));
    addField(new DecNumber(buffer, offset + 761, 1, "Fade amount"));
    addField(new DecNumber(buffer, offset + 762, 1, "Fade speed"));
    addField(new Flag(buffer, offset + 763, 1, "Attributes", s_attributes_iwd2));
    addField(new DecNumber(buffer, offset + 764, 1, "Visibility"));
    addField(new Unknown(buffer, offset + 765, 2));
    addField(new DecNumber(buffer, offset + 767, 1, "Unused skill points"));
    addField(new Unknown(buffer, offset + 768, 124));
    addField(new IdsBitmap(buffer, offset + 892, 1, "Allegiance", "EA.IDS"));
    addField(new IdsBitmap(buffer, offset + 893, 1, "General", "GENERAL.IDS"));
    addField(new IdsBitmap(buffer, offset + 894, 1, "Race", "RACE.IDS"));
    addField(new IdsBitmap(buffer, offset + 895, 1, "Class", "CLASS.IDS"));
    addField(new IdsBitmap(buffer, offset + 896, 1, "Specifics", "SPECIFIC.IDS"));
    addField(new IdsBitmap(buffer, offset + 897, 1, "Gender", "GENDER.IDS"));
    addField(new IdsBitmap(buffer, offset + 898, 1, "Object spec 1", "OBJECT.IDS"));
    addField(new IdsBitmap(buffer, offset + 899, 1, "Object spec 2", "OBJECT.IDS"));
    addField(new IdsBitmap(buffer, offset + 900, 1, "Object spec 3", "OBJECT.IDS"));
    addField(new IdsBitmap(buffer, offset + 901, 1, "Object spec 4", "OBJECT.IDS"));
    addField(new IdsBitmap(buffer, offset + 902, 1, "Object spec 5", "OBJECT.IDS"));
    addField(new IdsBitmap(buffer, offset + 903, 1, "Alignment", "ALIGNMNT.IDS"));
    addField(new DecNumber(buffer, offset + 904, 2, "Global identifier"));
    addField(new DecNumber(buffer, offset + 906, 2, "Local identifier"));
    addField(new TextString(buffer, offset + 908, 32, "Script name"));
    addField(new IdsBitmap(buffer, offset + 940, 2, "Class 2", "CLASS.IDS"));
    addField(new IdsBitmap(buffer, offset + 942, 4, "Class mask", "CLASSMSK.IDS"));

    // Bard spells
    for (int i = 0; i < 9; i++) {
      SectionOffset s_off = new SectionOffset(buffer, offset + 946 + 4 * i,
                                              "Bard spells " + (i + 1) + " offset", null);
      DecNumber s_count = new DecNumber(buffer, offset + 1198 + 4 * i, 4,
                                        "Bard spells " + (i + 1) + " count");
      addField(s_off);
      addField(s_count);
      AbstractStruct s = new Iwd2Struct(this, buffer, getExtraOffset() + s_off.getValue(),
                                        s_count, "Bard spells " + (i + 1), Iwd2Struct.TYPE_SPELL);
      addField(s);
//      s_off.setStaticStruct(s);
    }

    // Cleric spells
    for (int i = 0; i < 9; i++) {
      SectionOffset s_off = new SectionOffset(buffer, offset + 982 + 4 * i,
                                              "Cleric spells " + (i + 1) + " offset", null);
      DecNumber s_count = new DecNumber(buffer, offset + 1234 + 4 * i, 4,
                                        "Cleric spells " + (i + 1) + " count");
      addField(s_off);
      addField(s_count);
      AbstractStruct s = new Iwd2Struct(this, buffer, getExtraOffset() + s_off.getValue(),
                                        s_count, "Cleric spells " + (i + 1), Iwd2Struct.TYPE_SPELL);
      addField(s);
//      s_off.setStaticStruct(s);
    }

    // Druid spells
    for (int i = 0; i < 9; i++) {
      SectionOffset s_off = new SectionOffset(buffer, offset + 1018 + 4 * i,
                                              "Druid spells " + (i + 1) + " offset", null);
      DecNumber s_count = new DecNumber(buffer, offset + 1270 + 4 * i, 4,
                                        "Druid spells " + (i + 1) + " count");
      addField(s_off);
      addField(s_count);
      AbstractStruct s = new Iwd2Struct(this, buffer, getExtraOffset() + s_off.getValue(),
                                        s_count, "Druid spells " + (i + 1), Iwd2Struct.TYPE_SPELL);
      addField(s);
//      s_off.setStaticStruct(s);
    }

    // Paladin spells
    for (int i = 0; i < 9; i++) {
      SectionOffset s_off = new SectionOffset(buffer, offset + 1054 + 4 * i,
                                              "Paladin spells " + (i + 1) + " offset", null);
      DecNumber s_count = new DecNumber(buffer, offset + 1306 + 4 * i, 4,
                                        "Paladin spells " + (i + 1) + " count");
      addField(s_off);
      addField(s_count);
      AbstractStruct s = new Iwd2Struct(this, buffer, getExtraOffset() + s_off.getValue(),
                                        s_count, "Paladin spells " + (i + 1), Iwd2Struct.TYPE_SPELL);
      addField(s);
//      s_off.setStaticStruct(s);
    }

    // Ranger spells
    for (int i = 0; i < 9; i++) {
      SectionOffset s_off = new SectionOffset(buffer, offset + 1090 + 4 * i,
                                              "Ranger spells " + (i + 1) + " offset", null);
      DecNumber s_count = new DecNumber(buffer, offset + 1342 + 4 * i, 4,
                                        "Ranger spells " + (i + 1) + " count");
      addField(s_off);
      addField(s_count);
      AbstractStruct s = new Iwd2Struct(this, buffer, getExtraOffset() + s_off.getValue(),
                                        s_count, "Ranger spells " + (i + 1), Iwd2Struct.TYPE_SPELL);
      addField(s);
//      s_off.setStaticStruct(s);
    }

    // Sorcerer spells
    for (int i = 0; i < 9; i++) {
      SectionOffset s_off = new SectionOffset(buffer, offset + 1126 + 4 * i,
                                              "Sorcerer spells " + (i + 1) + " offset", null);
      DecNumber s_count = new DecNumber(buffer, offset + 1378 + 4 * i, 4,
                                        "Sorcerer spells " + (i + 1) + " count");
      addField(s_off);
      addField(s_count);
      AbstractStruct s = new Iwd2Struct(this, buffer, getExtraOffset() + s_off.getValue(),
                                        s_count, "Sorcerer spells " + (i + 1), Iwd2Struct.TYPE_SPELL);
      addField(s);
//      s_off.setStaticStruct(s);
    }

    // Wizard spells
    for (int i = 0; i < 9; i++) {
      SectionOffset s_off = new SectionOffset(buffer, offset + 1162 + 4 * i,
                                              "Wizard spells " + (i + 1) + " offset", null);
      DecNumber s_count = new DecNumber(buffer, offset + 1414 + 4 * i, 4,
                                        "Wizard spells " + (i + 1) + " count");
      addField(s_off);
      addField(s_count);
      AbstractStruct s = new Iwd2Struct(this, buffer, getExtraOffset() + s_off.getValue(),
                                        s_count, "Wizard spells " + (i + 1), Iwd2Struct.TYPE_SPELL);
      addField(s);
//      s_off.setStaticStruct(s);
    }

    // Domain spells
    for (int i = 0; i < 9; i++) {
      SectionOffset s_off = new SectionOffset(buffer, offset + 1450 + 4 * i,
                                              "Domain spells " + (i + 1) + " offset", null);
      DecNumber s_count = new DecNumber(buffer, offset + 1486 + 4 * i, 4,
                                        "Domain spells " + (i + 1) + " count");
      addField(s_off);
      addField(s_count);
      AbstractStruct s = new Iwd2Struct(this, buffer, getExtraOffset() + s_off.getValue(),
                                        s_count, "Domain spells " + (i + 1), Iwd2Struct.TYPE_SPELL);
      addField(s);
//      s_off.setStaticStruct(s);
    }

    // Innate abilities
    SectionOffset inn_off = new SectionOffset(buffer, offset + 1522, "Abilities offset", null);
    DecNumber inn_num = new DecNumber(buffer, offset + 1526, 4, "Abilities count");
    addField(inn_off);
    addField(inn_num);
    AbstractStruct inn_str = new Iwd2Struct(this, buffer, getExtraOffset() + inn_off.getValue(),
                                            inn_num, "Abilities", Iwd2Struct.TYPE_ABILITY);
    addField(inn_str);
//    inn_off.setStaticStruct(inn_str);

    // Songs
    SectionOffset song_off = new SectionOffset(buffer, offset + 1530, "Songs offset", null);
    DecNumber song_num = new DecNumber(buffer, offset + 1534, 4, "Songs count");
    addField(song_off);
    addField(song_num);
    AbstractStruct song_str = new Iwd2Struct(this, buffer, getExtraOffset() + song_off.getValue(),
                                             song_num, "Songs", Iwd2Struct.TYPE_SONG);
    addField(song_str);
//    song_off.setStaticStruct(song_str);

    // Shapes
    SectionOffset shape_off = new SectionOffset(buffer, offset + 1538, "Shapes offset", null);
    DecNumber shape_num = new DecNumber(buffer, offset + 1542, 4, "Shapes count");
    addField(shape_off);
    addField(shape_num);
    AbstractStruct shape_str = new Iwd2Struct(this, buffer, getExtraOffset() + shape_off.getValue(),
                                              shape_num, "Shapes", Iwd2Struct.TYPE_SHAPE);
    addField(shape_str);
//    shape_off.setStaticStruct(shape_str);

    SectionOffset itemslots_offset = new SectionOffset(buffer, offset + 1546, "Item slots offset", null);
    addField(itemslots_offset);
    SectionOffset items_offset = new SectionOffset(buffer, offset + 1550, "Items offset",
                                                   Item.class);
    addField(items_offset);
    SectionCount items_count = new SectionCount(buffer, offset + 1554, 4, "# items",
                                                Item.class);
    addField(items_count);

    SectionOffset effects_offset;
    SectionCount effects_count;
    if (effect_flag.getValue() == 1) {
      effects_offset = new SectionOffset(buffer, offset + 1558, "Effects offset",
                                         Effect2.class);
      effects_count = new SectionCount(buffer, offset + 1562, 4, "# effects",
                                       Effect2.class);
    }
    else {
      effects_offset = new SectionOffset(buffer, offset + 1558, "Effects offset",
                                         Effect.class);
      effects_count = new SectionCount(buffer, offset + 1562, 4, "# effects",
                                       Effect.class);
    }
    addField(effects_offset);
    addField(effects_count);
    addField(new ResourceRef(buffer, offset + 1566, "Dialogue", "DLG"));

    offset = getExtraOffset() + effects_offset.getValue();
    if (effect_flag.getValue() == 1)
      for (int i = 0; i < effects_count.getValue(); i++) {
        Effect2 eff = new Effect2(this, buffer, offset, i);
        offset = eff.getEndOffset();
        addField(eff);
      }
    else
      for (int i = 0; i < effects_count.getValue(); i++) {
        Effect eff = new Effect(this, buffer, offset, i);
        offset = eff.getEndOffset();
        addField(eff);
      }

    offset = getExtraOffset() + items_offset.getValue();
    for (int i = 0; i < items_count.getValue(); i++) {
      Item item = new Item(this, buffer, offset, i);
      offset = item.getEndOffset();
      addField(item);
    }

    offset = getExtraOffset() + itemslots_offset.getValue();
    addField(new DecNumber(buffer, offset, 2, "Helmet"));
    addField(new DecNumber(buffer, offset + 2, 2, "Armor"));
    addField(new DecNumber(buffer, offset + 4, 2, "Shield"));
    addField(new DecNumber(buffer, offset + 6, 2, "Gauntlets"));
    addField(new DecNumber(buffer, offset + 8, 2, "Left ring"));
    addField(new DecNumber(buffer, offset + 10, 2, "Right ring"));
    addField(new DecNumber(buffer, offset + 12, 2, "Amulet"));
    addField(new DecNumber(buffer, offset + 14, 2, "Belt"));
    addField(new DecNumber(buffer, offset + 16, 2, "Boots"));
    addField(new DecNumber(buffer, offset + 18, 2, "Weapon 1"));
    addField(new DecNumber(buffer, offset + 20, 2, "Shield 1"));
    addField(new DecNumber(buffer, offset + 22, 2, "Weapon 2"));
    addField(new DecNumber(buffer, offset + 24, 2, "Shield 2"));
    addField(new DecNumber(buffer, offset + 26, 2, "Weapon 3"));
    addField(new DecNumber(buffer, offset + 28, 2, "Shield 3"));
    addField(new DecNumber(buffer, offset + 30, 2, "Weapon 4"));
    addField(new DecNumber(buffer, offset + 32, 2, "Shield 4"));
    addField(new DecNumber(buffer, offset + 34, 2, "Quiver 1"));
    addField(new DecNumber(buffer, offset + 36, 2, "Quiver 2"));
    addField(new DecNumber(buffer, offset + 38, 2, "Quiver 3"));
    addField(new DecNumber(buffer, offset + 40, 2, "Quiver 4"));
    addField(new DecNumber(buffer, offset + 42, 2, "Cloak"));
    addField(new DecNumber(buffer, offset + 44, 2, "Quick item 1"));
    addField(new DecNumber(buffer, offset + 46, 2, "Quick item 2"));
    addField(new DecNumber(buffer, offset + 48, 2, "Quick item 3"));
    addField(new DecNumber(buffer, offset + 50, 2, "Inventory 1"));
    addField(new DecNumber(buffer, offset + 52, 2, "Inventory 2"));
    addField(new DecNumber(buffer, offset + 54, 2, "Inventory 3"));
    addField(new DecNumber(buffer, offset + 56, 2, "Inventory 4"));
    addField(new DecNumber(buffer, offset + 58, 2, "Inventory 5"));
    addField(new DecNumber(buffer, offset + 60, 2, "Inventory 6"));
    addField(new DecNumber(buffer, offset + 62, 2, "Inventory 7"));
    addField(new DecNumber(buffer, offset + 64, 2, "Inventory 8"));
    addField(new DecNumber(buffer, offset + 66, 2, "Inventory 9"));
    addField(new DecNumber(buffer, offset + 68, 2, "Inventory 10"));
    addField(new DecNumber(buffer, offset + 70, 2, "Inventory 11"));
    addField(new DecNumber(buffer, offset + 72, 2, "Inventory 12"));
    addField(new DecNumber(buffer, offset + 74, 2, "Inventory 13"));
    addField(new DecNumber(buffer, offset + 76, 2, "Inventory 14"));
    addField(new DecNumber(buffer, offset + 78, 2, "Inventory 15"));
    addField(new DecNumber(buffer, offset + 80, 2, "Inventory 16"));
    addField(new DecNumber(buffer, offset + 82, 2, "Inventory 17"));
    addField(new DecNumber(buffer, offset + 84, 2, "Inventory 18"));
    addField(new DecNumber(buffer, offset + 86, 2, "Inventory 19"));
    addField(new DecNumber(buffer, offset + 88, 2, "Inventory 20"));
    addField(new DecNumber(buffer, offset + 90, 2, "Inventory 21"));
    addField(new DecNumber(buffer, offset + 92, 2, "Inventory 22"));
    addField(new DecNumber(buffer, offset + 94, 2, "Inventory 23"));
    addField(new DecNumber(buffer, offset + 96, 2, "Inventory 24"));
    addField(new DecNumber(buffer, offset + 98, 2, "Magically created weapon"));
    addField(new DecNumber(buffer, offset + 100, 2, "Weapon slot selected"));
    addField(new DecNumber(buffer, offset + 102, 2, "Weapon ability selected"));

    int endoffset = offset;
    for (int i = 0; i < getFieldCount(); i++) {
      StructEntry entry = getField(i);
      if (entry.getOffset() + entry.getSize() > endoffset) {
        endoffset = entry.getOffset() + entry.getSize();
      }
    }
    return endoffset;
  }

  ////////////////////////
  // Other IE games
  ////////////////////////

  private int readOther(String version, byte buffer[], int offset) throws Exception
  {
    addField(new StringRef(buffer, offset, "Name"));
    addField(new StringRef(buffer, offset + 4, "Tooltip"));
    addField(new Flag(buffer, offset + 8, 4, "Flags", s_flag));
    addField(new DecNumber(buffer, offset + 12, 4, "XP value"));
    addField(new DecNumber(buffer, offset + 16, 4, "XP"));
    addField(new DecNumber(buffer, offset + 20, 4, "Gold"));
    addField(new IdsFlag(buffer, offset + 24, 4, "Status", "STATE.IDS"));
    addField(new DecNumber(buffer, offset + 28, 2, "Current HP"));
    addField(new DecNumber(buffer, offset + 30, 2, "Maximum HP"));
    addField(new IdsBitmap(buffer, offset + 32, 4, "Animation", "ANIMATE.IDS"));
//    addField(new Unknown(buffer, offset + 34, 2));
//    if (version.equalsIgnoreCase("V1.2") || version.equalsIgnoreCase("V1.1"))
//      addField(new Unknown(buffer, offset + 36, 7));
//    else {
    addField(new ColorValue(buffer, offset + 36, 1, "Metal color"));
    addField(new ColorValue(buffer, offset + 37, 1, "Minor color"));
    addField(new ColorValue(buffer, offset + 38, 1, "Major color"));
    addField(new ColorValue(buffer, offset + 39, 1, "Skin color"));
    addField(new ColorValue(buffer, offset + 40, 1, "Leather color"));
    addField(new ColorValue(buffer, offset + 41, 1, "Armor color"));
    addField(new ColorValue(buffer, offset + 42, 1, "Hair color"));
//    }
    DecNumber effect_flag = new DecNumber(buffer, offset + 43, 1, "Effect flag");
    addField(effect_flag);
    addField(new ResourceRef(buffer, offset + 44, "Small portrait", "BMP"));
    if (version.equalsIgnoreCase("V1.2") || version.equalsIgnoreCase("V1.1")) {
      addField(new ResourceRef(buffer, offset + 52, "Large portrait", "BAM"));
    } else {
      addField(new ResourceRef(buffer, offset + 52, "Large portrait", "BMP"));
    }
    addField(new UnsignDecNumber(buffer, offset + 60, 1, "Reputation"));
    addField(new UnsignDecNumber(buffer, offset + 61, 1, "Hide in shadows"));
    addField(new DecNumber(buffer, offset + 62, 2, "Natural AC"));
    addField(new DecNumber(buffer, offset + 64, 2, "Effective AC"));
    addField(new DecNumber(buffer, offset + 66, 2, "Crushing AC modifier"));
    addField(new DecNumber(buffer, offset + 68, 2, "Missile AC modifier"));
    addField(new DecNumber(buffer, offset + 70, 2, "Piercing AC modifier"));
    addField(new DecNumber(buffer, offset + 72, 2, "Slashing AC modifier"));
    addField(new DecNumber(buffer, offset + 74, 1, "THAC0"));
//    if (version.equalsIgnoreCase("V1.2") || version.equalsIgnoreCase("V1.1"))
    addField(new Bitmap(buffer, offset + 75, 1, "# attacks", s_attacks));
//    else
//      addField(new DecNumber(buffer, offset + 75, 1, "# attacks"));
    addField(new DecNumber(buffer, offset + 76, 1, "Save vs. death"));
    addField(new DecNumber(buffer, offset + 77, 1, "Save vs. wand"));
    addField(new DecNumber(buffer, offset + 78, 1, "Save vs. polymorph"));
    addField(new DecNumber(buffer, offset + 79, 1, "Save vs. breath"));
    addField(new DecNumber(buffer, offset + 80, 1, "Save vs. spell"));
    addField(new DecNumber(buffer, offset + 81, 1, "Resist fire"));
    addField(new DecNumber(buffer, offset + 82, 1, "Resist cold"));
    addField(new DecNumber(buffer, offset + 83, 1, "Resist electricity"));
    addField(new DecNumber(buffer, offset + 84, 1, "Resist acid"));
    addField(new DecNumber(buffer, offset + 85, 1, "Resist magic"));
    addField(new DecNumber(buffer, offset + 86, 1, "Resist magic fire"));
    addField(new DecNumber(buffer, offset + 87, 1, "Resist magic cold"));
    addField(new DecNumber(buffer, offset + 88, 1, "Resist slashing"));
    addField(new DecNumber(buffer, offset + 89, 1, "Resist crushing"));
    addField(new DecNumber(buffer, offset + 90, 1, "Resist piercing"));
    addField(new DecNumber(buffer, offset + 91, 1, "Resist missile"));
    if (version.equalsIgnoreCase("V1.2") || version.equalsIgnoreCase("V1.1")) {
      addField(new DecNumber(buffer, offset + 92, 1, "Unspent proficiencies"));
//      addField(new Unknown(buffer, offset + 93, 1));
    }
    else {
      addField(new UnsignDecNumber(buffer, offset + 92, 1, "Detect illusions"));
    }
    addField(new UnsignDecNumber(buffer, offset + 93, 1, "Set traps"));
    addField(new DecNumber(buffer, offset + 94, 1, "Lore"));
    addField(new UnsignDecNumber(buffer, offset + 95, 1, "Open locks"));
    addField(new UnsignDecNumber(buffer, offset + 96, 1, "Move silently"));
    addField(new UnsignDecNumber(buffer, offset + 97, 1, "Find traps"));
    addField(new UnsignDecNumber(buffer, offset + 98, 1, "Pick pockets"));
    addField(new DecNumber(buffer, offset + 99, 1, "Fatigue"));
    addField(new DecNumber(buffer, offset + 100, 1, "Intoxication"));
    addField(new DecNumber(buffer, offset + 101, 1, "Luck"));
    if (version.equals("V1.0")) {
      addField(new DecNumber(buffer, offset + 102, 1, "Large sword proficiency"));
      addField(new DecNumber(buffer, offset + 103, 1, "Small sword proficiency"));
      addField(new DecNumber(buffer, offset + 104, 1, "Bow proficiency"));
      addField(new DecNumber(buffer, offset + 105, 1, "Spear proficiency"));
      addField(new DecNumber(buffer, offset + 106, 1, "Blunt proficiency"));
      addField(new DecNumber(buffer, offset + 107, 1, "Spiked proficiency"));
      addField(new DecNumber(buffer, offset + 108, 1, "Axe proficiency"));
      addField(new DecNumber(buffer, offset + 109, 1, "Missile proficiency"));
      if (ResourceFactory.isEnhancedEdition()) {
        if (ResourceFactory.getGameID() == ResourceFactory.ID_IWDEE ||
            ResourceFactory.getGameID() == ResourceFactory.ID_BG2EE) {
          addField(new Unknown(buffer, offset + 110, 7));
          addField(new Bitmap(buffer, offset + 117, 1, "Nightmare mode", s_noyes));
          addField(new UnsignDecNumber(buffer, offset + 118, 1, "Translucency"));
        } else {
          addField(new Unknown(buffer, offset + 110, 9));
        }
        addField(new DecNumber(buffer, offset + 119, 1, "Reputation gain/loss when killed"));
        addField(new DecNumber(buffer, offset + 120, 1, "Reputation gain/loss when joining party"));
        addField(new DecNumber(buffer, offset + 121, 1, "Reputation gain/loss when leaving party"));
      } else {
        addField(new Unknown(buffer, offset + 110, 12));
      }
    }
    else if (version.equalsIgnoreCase("V1.2") || version.equalsIgnoreCase("V1.1")) {
      addField(new DecNumber(buffer, offset + 102, 1, "Fist proficiency"));
      addField(new DecNumber(buffer, offset + 103, 1, "Edged-weapon proficiency"));
      addField(new DecNumber(buffer, offset + 104, 1, "Hammer proficiency"));
      addField(new DecNumber(buffer, offset + 105, 1, "Axe proficiency"));
      addField(new DecNumber(buffer, offset + 106, 1, "Club proficiency"));
      addField(new DecNumber(buffer, offset + 107, 1, "Bow proficiency"));
//      addField(new DecNumber(buffer, offset + 108, 1, "Extra proficiency 1"));
//      addField(new DecNumber(buffer, offset + 109, 1, "Extra proficiency 2"));
//      addField(new DecNumber(buffer, offset + 110, 1, "Extra proficiency 3"));
//      addField(new DecNumber(buffer, offset + 111, 1, "Extra proficiency 4"));
//      addField(new DecNumber(buffer, offset + 112, 1, "Extra proficiency 5"));
//      addField(new DecNumber(buffer, offset + 113, 1, "Extra proficiency 6"));
//      addField(new DecNumber(buffer, offset + 114, 1, "Extra proficiency 7"));
//      addField(new DecNumber(buffer, offset + 115, 1, "Extra proficiency 8"));
//      addField(new DecNumber(buffer, offset + 116, 1, "Extra proficiency 9"));
      addField(new Unknown(buffer, offset + 108, 14));
    }
    else if (version.equalsIgnoreCase("V9.0")) {
      addField(new DecNumber(buffer, offset + 102, 1, "Large sword proficiency"));
      addField(new DecNumber(buffer, offset + 103, 1, "Small sword proficiency"));
      addField(new DecNumber(buffer, offset + 104, 1, "Bow proficiency"));
      addField(new DecNumber(buffer, offset + 105, 1, "Spear proficiency"));
      addField(new DecNumber(buffer, offset + 106, 1, "Axe proficiency"));
      addField(new DecNumber(buffer, offset + 107, 1, "Missile proficiency"));
      addField(new DecNumber(buffer, offset + 108, 1, "Greatsword proficiency"));
      addField(new DecNumber(buffer, offset + 109, 1, "Dagger proficiency"));
      addField(new DecNumber(buffer, offset + 110, 1, "Halberd proficiency"));
      addField(new DecNumber(buffer, offset + 111, 1, "Mace proficiency"));
      addField(new DecNumber(buffer, offset + 112, 1, "Flail proficiency"));
      addField(new DecNumber(buffer, offset + 113, 1, "Hammer proficiency"));
      addField(new DecNumber(buffer, offset + 114, 1, "Club proficiency"));
      addField(new DecNumber(buffer, offset + 115, 1, "Quarterstaff proficiency"));
      addField(new DecNumber(buffer, offset + 116, 1, "Crossbow proficiency"));
      addField(new Unknown(buffer, offset + 117, 5));
    }
    else {
      clearFields();
      throw new Exception("Unsupported version: " + version);
    }
    addField(new DecNumber(buffer, offset + 122, 1, "Undead level"));
    addField(new DecNumber(buffer, offset + 123, 1, "Tracking"));
    addField(new TextString(buffer, offset + 124, 32, "Target"));
    LongIntegerHashMap<IdsMapEntry> sndmap = null;
    if (ResourceFactory.getInstance().resourceExists("SNDSLOT.IDS")) {
      sndmap = IdsMapCache.get("SNDSLOT.IDS").getMap();
    } else if (ResourceFactory.getInstance().resourceExists("SOUNDOFF.IDS")) {
      sndmap = IdsMapCache.get("SOUNDOFF.IDS").getMap();
    }
    if (sndmap != null) {
      for (int i = 0; i < 100; i++)
        if (sndmap.containsKey((long)i)) {
          addField(new StringRef(buffer, offset + 156 + i * 4,
                                "Sound: " + ((IdsMapEntry)sndmap.get((long)i)).getString()));
        } else {
          addField(new StringRef(buffer, offset + 156 + i * 4, "Sound: Unknown"));
        }
    }
    else {
      for (int i = 0; i < 100; i++) {
        addField(new StringRef(buffer, offset + 156 + i * 4, "Soundset string"));
      }
    }
    addField(new DecNumber(buffer, offset + 556, 1, "Level first class"));
    addField(new DecNumber(buffer, offset + 557, 1, "Level second class"));
    addField(new DecNumber(buffer, offset + 558, 1, "Level third class"));
    addField(new IdsBitmap(buffer, offset + 559, 1, "Sex", "GENDER.IDS"));
//            new Bitmap(buffer, offset + 559, 1, "Sex", new String[]{"", "Male", "Female", "Neither", "Both"}));
    addField(new DecNumber(buffer, offset + 560, 1, "Strength"));
    addField(new DecNumber(buffer, offset + 561, 1, "Strength bonus"));
    addField(new DecNumber(buffer, offset + 562, 1, "Intelligence"));
    addField(new DecNumber(buffer, offset + 563, 1, "Wisdom"));
    addField(new DecNumber(buffer, offset + 564, 1, "Dexterity"));
    addField(new DecNumber(buffer, offset + 565, 1, "Constitution"));
    addField(new DecNumber(buffer, offset + 566, 1, "Charisma"));
    addField(new DecNumber(buffer, offset + 567, 1, "Morale"));
    addField(new DecNumber(buffer, offset + 568, 1, "Morale break"));
    addField(new IdsBitmap(buffer, offset + 569, 1, "Racial enemy", "RACE.IDS"));
    addField(new DecNumber(buffer, offset + 570, 2, "Morale recovery"));
//    addField(new Unknown(buffer, offset + 571, 1));
    if (ResourceFactory.getInstance().resourceExists("KIT.IDS")) {
      addField(new KitIdsBitmap(buffer, offset + 572, "Kit"));
    }
    else {
      if (ResourceFactory.getInstance().resourceExists("DEITY.IDS")) {
        addField(new IdsBitmap(buffer, offset + 572, 2, "Deity", "DEITY.IDS"));
      } else if (ResourceFactory.getInstance().resourceExists("DIETY.IDS")) {
        addField(new IdsBitmap(buffer, offset + 572, 2, "Deity", "DIETY.IDS"));
      } else {
        addField(new Unknown(buffer, offset + 572, 2));
      }
      if (ResourceFactory.getInstance().resourceExists("MAGESPEC.IDS")) {
        addField(new IdsBitmap(buffer, offset + 574, 2, "Mage type", "MAGESPEC.IDS"));
      } else {
        addField(new HashBitmap(buffer, offset + 574, 2, "Mage type", m_magetype));
      }
    }
    addField(new ResourceRef(buffer, offset + 576, "Override script", "BCS"));
    addField(new ResourceRef(buffer, offset + 584, "Class script", "BCS"));
    addField(new ResourceRef(buffer, offset + 592, "Race script", "BCS"));
    addField(new ResourceRef(buffer, offset + 600, "General script", "BCS"));
    addField(new ResourceRef(buffer, offset + 608, "Default script", "BCS"));
    if (version.equalsIgnoreCase("V1.2") || version.equalsIgnoreCase("V1.1")) {
//      LongIntegerHashMap<String> m_zoom = new LongIntegerHashMap<String>();
//      m_zoom.put(0x0000L, "No");
//      m_zoom.put(0xffffL, "Yes");
      addField(new Unknown(buffer, offset + 616, 24));
      addField(new Unknown(buffer, offset + 640, 4));
      addField(new Unknown(buffer, offset + 644, 8));
      addField(new Unknown(buffer, offset + 652, 4, "Overlays offset"));
      addField(new Unknown(buffer, offset + 656, 4, "Overlays size"));
      addField(new DecNumber(buffer, offset + 660, 4, "XP second class"));
      addField(new DecNumber(buffer, offset + 664, 4, "XP third class"));
      LongIntegerHashMap<IdsMapEntry> intMap = IdsMapCache.get("INTERNAL.IDS").getMap();
      for (int i = 0; i < 10; i++) {
        if (intMap.containsKey((long)i)) {
          addField(new DecNumber(buffer, offset + 668 + i * 2, 2,
                                ((IdsMapEntry)intMap.get((long)i)).getString()));
        } else {
          addField(new DecNumber(buffer, offset + 668 + i * 2, 2, "Internal " + i));
        }
      }
      addField(new DecNumber(buffer, offset + 688, 1, "Good increment by"));
      addField(new DecNumber(buffer, offset + 689, 1, "Law increment by"));
      addField(new DecNumber(buffer, offset + 690, 1, "Lady increment by"));
      addField(new DecNumber(buffer, offset + 691, 1, "Murder increment by"));
      addField(new TextString(buffer, offset + 692, 32, "Character type"));
      addField(new DecNumber(buffer, offset + 724, 1, "Dialogue activation radius"));
      addField(new DecNumber(buffer, offset + 725, 1, "Collision radius")); // 0x2dd
      addField(new Unknown(buffer, offset + 726, 1));
      addField(new DecNumber(buffer, offset + 727, 1, "# colors"));
      addField(new Flag(buffer, offset + 728, 4, "Attributes", s_attributes_pst));
//      addField(new Flag(buffer, offset + 729, 1, "Attribute flags 2",
//                        new String[]{"No flags set", "", "Invulnerable"}));
//      addField(new Unknown(buffer, offset + 730, 2));
      addField(new IdsBitmap(buffer, offset + 732, 2, "Color 1", "CLOWNCLR.IDS"));
      addField(new IdsBitmap(buffer, offset + 734, 2, "Color 2", "CLOWNCLR.IDS"));
      addField(new IdsBitmap(buffer, offset + 736, 2, "Color 3", "CLOWNCLR.IDS"));
      addField(new IdsBitmap(buffer, offset + 738, 2, "Color 4", "CLOWNCLR.IDS"));
      addField(new IdsBitmap(buffer, offset + 740, 2, "Color 5", "CLOWNCLR.IDS"));
      addField(new IdsBitmap(buffer, offset + 742, 2, "Color 6", "CLOWNCLR.IDS"));
      addField(new IdsBitmap(buffer, offset + 744, 2, "Color 7", "CLOWNCLR.IDS"));
      addField(new Unknown(buffer, offset + 746, 3));
      addField(new HashBitmap(buffer, offset + 749, 1, "Color 1 placement", m_colorPlacement));
      addField(new HashBitmap(buffer, offset + 750, 1, "Color 2 placement", m_colorPlacement));
      addField(new HashBitmap(buffer, offset + 751, 1, "Color 3 placement", m_colorPlacement));
      addField(new HashBitmap(buffer, offset + 752, 1, "Color 4 placement", m_colorPlacement));
      addField(new HashBitmap(buffer, offset + 753, 1, "Color 5 placement", m_colorPlacement));
      addField(new HashBitmap(buffer, offset + 754, 1, "Color 6 placement", m_colorPlacement));
      addField(new HashBitmap(buffer, offset + 755, 1, "Color 7 placement", m_colorPlacement));
      addField(new Unknown(buffer, offset + 756, 21));
      addField(new IdsBitmap(buffer, offset + 777, 1, "Species", "RACE.IDS"));
      addField(new IdsBitmap(buffer, offset + 778, 1, "Team", "TEAM.IDS"));
      addField(new IdsBitmap(buffer, offset + 779, 1, "Faction", "FACTION.IDS"));
      offset += 164;
    }
    else if (version.equalsIgnoreCase("V9.0")) {
      addField(new Bitmap(buffer, offset + 616, 1, "Default visibility", s_visible));
      addField(new Bitmap(buffer, offset + 617, 1, "Set extra death variable?", s_noyes));
      addField(new Bitmap(buffer, offset + 618, 1, "Increment kill count?", s_noyes));
      addField(new Unknown(buffer, offset + 619, 1));
      addField(new DecNumber(buffer, offset + 620, 2, "Internal 1"));
      addField(new DecNumber(buffer, offset + 622, 2, "Internal 2"));
      addField(new DecNumber(buffer, offset + 624, 2, "Internal 3"));
      addField(new DecNumber(buffer, offset + 626, 2, "Internal 4"));
      addField(new DecNumber(buffer, offset + 628, 2, "Internal 5"));
      addField(new TextString(buffer, offset + 630, 32, "Death variable (set)"));
      addField(new TextString(buffer, offset + 662, 32, "Death variable (increment)"));
      addField(new Bitmap(buffer, offset + 694, 2, "Location saved?", s_noyes));
      addField(new DecNumber(buffer, offset + 696, 2, "Saved location: X"));
      addField(new DecNumber(buffer, offset + 698, 2, "Saved location: Y"));
      addField(new DecNumber(buffer, offset + 700, 2, "Saved orientation"));
      addField(new Unknown(buffer, offset + 702, 18));
      offset += 104;
    }
    addField(new IdsBitmap(buffer, offset + 616, 1, "Allegiance", "EA.IDS"));
    addField(new IdsBitmap(buffer, offset + 617, 1, "General", "GENERAL.IDS"));
    addField(new IdsBitmap(buffer, offset + 618, 1, "Race", "RACE.IDS"));
    addField(new IdsBitmap(buffer, offset + 619, 1, "Class", "CLASS.IDS"));
    addField(new IdsBitmap(buffer, offset + 620, 1, "Specifics", "SPECIFIC.IDS"));
    addField(new IdsBitmap(buffer, offset + 621, 1, "Gender", "GENDER.IDS"));
    addField(new IdsBitmap(buffer, offset + 622, 1, "Object spec 1", "OBJECT.IDS"));
    addField(new IdsBitmap(buffer, offset + 623, 1, "Object spec 2", "OBJECT.IDS"));
    addField(new IdsBitmap(buffer, offset + 624, 1, "Object spec 3", "OBJECT.IDS"));
    addField(new IdsBitmap(buffer, offset + 625, 1, "Object spec 4", "OBJECT.IDS"));
    addField(new IdsBitmap(buffer, offset + 626, 1, "Object spec 5", "OBJECT.IDS"));
//    if (ResourceFactory.getGameID() == ResourceFactory.ID_BG2 ||
//        ResourceFactory.getGameID() == ResourceFactory.ID_BG2TOB)
//      addField(new IdsBitmap(buffer, offset + 627, 1, "Alignment", "ALIGN.IDS"));
//    else
    addField(new IdsBitmap(buffer, offset + 627, 1, "Alignment", "ALIGNMEN.IDS"));
    addField(new DecNumber(buffer, offset + 628, 2, "Global identifier"));
    addField(new DecNumber(buffer, offset + 630, 2, "Local identifier"));
    addField(new TextString(buffer, offset + 632, 32, "Script name"));
//    addField(new Unknown(buffer, offset + 650, 14));

    SectionOffset offsetKnownSpells = new SectionOffset(buffer, offset + 664, "Known spells offset",
                                                        KnownSpells.class);
    addField(offsetKnownSpells);
    SectionCount countKnownSpells = new SectionCount(buffer, offset + 668, 4, "# known spells",
                                                     KnownSpells.class);
    addField(countKnownSpells);
    SectionOffset offsetMemSpellInfo = new SectionOffset(buffer, offset + 672, "Memorization info offset",
                                                         SpellMemorization.class);
    addField(offsetMemSpellInfo);
    SectionCount countMemSpellInfo = new SectionCount(buffer, offset + 676, 4, "# memorization info",
                                                      SpellMemorization.class);
    addField(countMemSpellInfo);
    SectionOffset offsetMemSpells = new SectionOffset(buffer, offset + 680, "Memorized spells offset",
                                                      MemorizedSpells.class);
    addField(offsetMemSpells);
    SectionCount countMemSpells = new SectionCount(buffer, offset + 684, 4, "# memorized spells",
                                                   MemorizedSpells.class);
    addField(countMemSpells);
    SectionOffset offsetItemslots = new SectionOffset(buffer, offset + 688, "Item slots offset", null);
    addField(offsetItemslots);
    SectionOffset offsetItems = new SectionOffset(buffer, offset + 692, "Items offset", Item.class);
    addField(offsetItems);
    SectionCount countItems = new SectionCount(buffer, offset + 696, 4, "# items", Item.class);
    addField(countItems);
    SectionOffset offsetEffects;
    SectionCount countEffects;
    if (effect_flag.getValue() == 1) {
      offsetEffects = new SectionOffset(buffer, offset + 700, "Effects offset", Effect2.class);
      countEffects = new SectionCount(buffer, offset + 704, 4, "# effects", Effect2.class);
    }
    else {
      offsetEffects = new SectionOffset(buffer, offset + 700, "Effects offset", Effect.class);
      countEffects = new SectionCount(buffer, offset + 704, 4, "# effects", Effect.class);
    }
    addField(offsetEffects);
    addField(countEffects);
    addField(new ResourceRef(buffer, offset + 708, "Dialogue", "DLG"));

    offset = getExtraOffset() + offsetKnownSpells.getValue();
    for (int i = 0; i < countKnownSpells.getValue(); i++) {
      KnownSpells known = new KnownSpells(this, buffer, offset, i);
      offset = known.getEndOffset();
      addField(known);
    }

    offset = getExtraOffset() + offsetMemSpellInfo.getValue();
    for (int i = 0; i < countMemSpellInfo.getValue(); i++) {
      SpellMemorization mem = new SpellMemorization(this, buffer, offset, i);
      offset = mem.getEndOffset();
      mem.readMemorizedSpells(buffer, offsetMemSpells.getValue() + getExtraOffset());
      addField(mem);
    }

    offset = getExtraOffset() + offsetEffects.getValue();
    if (effect_flag.getValue() == 1) {
      for (int i = 0; i < countEffects.getValue(); i++) {
        Effect2 eff = new Effect2(this, buffer, offset, i);
        offset = eff.getEndOffset();
        addField(eff);
      }
    } else {
      for (int i = 0; i < countEffects.getValue(); i++) {
        Effect eff = new Effect(this, buffer, offset, i);
        offset = eff.getEndOffset();
        addField(eff);
      }
    }

    offset = getExtraOffset() + offsetItems.getValue();
    for (int i = 0; i < countItems.getValue(); i++) {
      Item item = new Item(this, buffer, offset, i);
      offset = item.getEndOffset();
      addField(item);
    }

    offset = getExtraOffset() + offsetItemslots.getValue();
    if (version.equalsIgnoreCase("V1.2")) {
      addField(new DecNumber(buffer, offset, 2, "Right earring"));
      addField(new DecNumber(buffer, offset + 2, 2, "Chest"));
      addField(new DecNumber(buffer, offset + 4, 2, "Left tattoo"));
      addField(new DecNumber(buffer, offset + 6, 2, "Hand"));
      addField(new DecNumber(buffer, offset + 8, 2, "Left ring"));
      addField(new DecNumber(buffer, offset + 10, 2, "Right ring"));
      addField(new DecNumber(buffer, offset + 12, 2, "Left earring"));
      addField(new DecNumber(buffer, offset + 14, 2, "Right tattoo (lower)"));
      addField(new DecNumber(buffer, offset + 16, 2, "Wrist"));
      addField(new DecNumber(buffer, offset + 18, 2, "Weapon 1"));
      addField(new DecNumber(buffer, offset + 20, 2, "Weapon 2"));
      addField(new DecNumber(buffer, offset + 22, 2, "Weapon 3"));
      addField(new DecNumber(buffer, offset + 24, 2, "Weapon 4"));
      addField(new DecNumber(buffer, offset + 26, 2, "Quiver 1"));
      addField(new DecNumber(buffer, offset + 28, 2, "Quiver 2"));
      addField(new DecNumber(buffer, offset + 30, 2, "Quiver 3"));
      addField(new DecNumber(buffer, offset + 32, 2, "Quiver 4"));
      addField(new DecNumber(buffer, offset + 34, 2, "Quiver 5"));
      addField(new DecNumber(buffer, offset + 36, 2, "Quiver 6"));
      addField(new DecNumber(buffer, offset + 38, 2, "Right tattoo (upper)"));
      addField(new DecNumber(buffer, offset + 40, 2, "Quick item 1"));
      addField(new DecNumber(buffer, offset + 42, 2, "Quick item 2"));
      addField(new DecNumber(buffer, offset + 44, 2, "Quick item 3"));
      addField(new DecNumber(buffer, offset + 46, 2, "Quick item 4"));
      addField(new DecNumber(buffer, offset + 48, 2, "Quick item 5"));
      addField(new DecNumber(buffer, offset + 50, 2, "Inventory 1"));
      addField(new DecNumber(buffer, offset + 52, 2, "Inventory 2"));
      addField(new DecNumber(buffer, offset + 54, 2, "Inventory 3"));
      addField(new DecNumber(buffer, offset + 56, 2, "Inventory 4"));
      addField(new DecNumber(buffer, offset + 58, 2, "Inventory 5"));
      addField(new DecNumber(buffer, offset + 60, 2, "Inventory 6"));
      addField(new DecNumber(buffer, offset + 62, 2, "Inventory 7"));
      addField(new DecNumber(buffer, offset + 64, 2, "Inventory 8"));
      addField(new DecNumber(buffer, offset + 66, 2, "Inventory 9"));
      addField(new DecNumber(buffer, offset + 68, 2, "Inventory 10"));
      addField(new DecNumber(buffer, offset + 70, 2, "Inventory 11"));
      addField(new DecNumber(buffer, offset + 72, 2, "Inventory 12"));
      addField(new DecNumber(buffer, offset + 74, 2, "Inventory 13"));
      addField(new DecNumber(buffer, offset + 76, 2, "Inventory 14"));
      addField(new DecNumber(buffer, offset + 78, 2, "Inventory 15"));
      addField(new DecNumber(buffer, offset + 80, 2, "Inventory 16"));
      addField(new DecNumber(buffer, offset + 82, 2, "Inventory 17"));
      addField(new DecNumber(buffer, offset + 84, 2, "Inventory 18"));
      addField(new DecNumber(buffer, offset + 86, 2, "Inventory 19"));
      addField(new DecNumber(buffer, offset + 88, 2, "Inventory 20"));
      addField(new DecNumber(buffer, offset + 90, 2, "Magically created weapon"));
      addField(new DecNumber(buffer, offset + 92, 2, "Weapon slot selected"));
      addField(new DecNumber(buffer, offset + 94, 2, "Weapon ability selected"));
    }
    else {
      addField(new DecNumber(buffer, offset, 2, "Helmet"));
      addField(new DecNumber(buffer, offset + 2, 2, "Armor"));
      addField(new DecNumber(buffer, offset + 4, 2, "Shield"));
      addField(new DecNumber(buffer, offset + 6, 2, "Gloves"));
      addField(new DecNumber(buffer, offset + 8, 2, "Left ring"));
      addField(new DecNumber(buffer, offset + 10, 2, "Right ring"));
      addField(new DecNumber(buffer, offset + 12, 2, "Amulet"));
      addField(new DecNumber(buffer, offset + 14, 2, "Belt"));
      addField(new DecNumber(buffer, offset + 16, 2, "Boots"));
      addField(new DecNumber(buffer, offset + 18, 2, "Weapon 1"));
      addField(new DecNumber(buffer, offset + 20, 2, "Weapon 2"));
      addField(new DecNumber(buffer, offset + 22, 2, "Weapon 3"));
      addField(new DecNumber(buffer, offset + 24, 2, "Weapon 4"));
      addField(new DecNumber(buffer, offset + 26, 2, "Quiver 1"));
      addField(new DecNumber(buffer, offset + 28, 2, "Quiver 2"));
      addField(new DecNumber(buffer, offset + 30, 2, "Quiver 3"));
      addField(new DecNumber(buffer, offset + 32, 2, "Quiver 4"));
      addField(new DecNumber(buffer, offset + 34, 2, "Cloak"));
      addField(new DecNumber(buffer, offset + 36, 2, "Quick item 1"));
      addField(new DecNumber(buffer, offset + 38, 2, "Quick item 2"));
      addField(new DecNumber(buffer, offset + 40, 2, "Quick item 3"));
      addField(new DecNumber(buffer, offset + 42, 2, "Inventory 1"));
      addField(new DecNumber(buffer, offset + 44, 2, "Inventory 2"));
      addField(new DecNumber(buffer, offset + 46, 2, "Inventory 3"));
      addField(new DecNumber(buffer, offset + 48, 2, "Inventory 4"));
      addField(new DecNumber(buffer, offset + 50, 2, "Inventory 5"));
      addField(new DecNumber(buffer, offset + 52, 2, "Inventory 6"));
      addField(new DecNumber(buffer, offset + 54, 2, "Inventory 7"));
      addField(new DecNumber(buffer, offset + 56, 2, "Inventory 8"));
      addField(new DecNumber(buffer, offset + 58, 2, "Inventory 9"));
      addField(new DecNumber(buffer, offset + 60, 2, "Inventory 10"));
      addField(new DecNumber(buffer, offset + 62, 2, "Inventory 11"));
      addField(new DecNumber(buffer, offset + 64, 2, "Inventory 12"));
      addField(new DecNumber(buffer, offset + 66, 2, "Inventory 13"));
      addField(new DecNumber(buffer, offset + 68, 2, "Inventory 14"));
      addField(new DecNumber(buffer, offset + 70, 2, "Inventory 15"));
      addField(new DecNumber(buffer, offset + 72, 2, "Inventory 16"));
      addField(new DecNumber(buffer, offset + 74, 2, "Magically created weapon"));
      addField(new DecNumber(buffer, offset + 76, 2, "Weapon slot selected"));
      addField(new DecNumber(buffer, offset + 78, 2, "Weapon ability selected"));
    }
    int endoffset = offset;
    for (int i = 0; i < getFieldCount(); i++) {
      StructEntry entry = getField(i);
      if (entry.getOffset() + entry.getSize() > endoffset) {
        endoffset = entry.getOffset() + entry.getSize();
      }
    }
    return endoffset;
  }

  private void updateMemorizedSpells()
  {
    // Assumes memorized spells offset is correct
    int offset = ((HexNumber)getAttribute("Memorized spells offset")).getValue() + getExtraOffset();
    int count = 0;
    for (int i = 0; i < getFieldCount(); i++) {
      Object o = getField(i);
      if (o instanceof SpellMemorization) {
        SpellMemorization info = (SpellMemorization)o;
        int numSpells = info.updateSpells(offset, count);
        offset += 12 * numSpells;
        count += numSpells;
      }
    }
    ((DecNumber)getAttribute("# memorized spells")).setValue(count);
  }

  private void updateOffsets(AddRemovable datatype, int size)
  {
    if (getField(0).toString().equalsIgnoreCase("CHR "))
      ((HexNumber)getAttribute("CRE structure length")).incValue(size);
//    if (!(datatype instanceof MemorizedSpells)) {
//      HexNumber offsetMemSpells = (HexNumber)getAttribute("Memorized spells offset");
//      if (datatype.getOffset() < offsetMemSpells.getValue() + getExtraOffset() ||
//          datatype.getOffset() == offsetMemSpells.getValue() + getExtraOffset() && size > 0)
//        offsetMemSpells.incValue(size);
//    }
  }

  //--------------------- Begin Interface ItemListener ---------------------

  @Override
  public void itemStateChanged(ItemEvent event)
  {
    if (event.getSource() == bExport) {
      JMenuItem item = bExport.getSelectedItem();
      if (item == miExport) {
        ResourceFactory.getInstance().exportResource(getResourceEntry(), NearInfinity.getInstance());
      } else if (item == miConvert) {
        convertCHRtoCRE(getResourceEntry());
      }
    }
  }

//--------------------- End Interface ItemListener ---------------------


  // Called by "Extended Search"
  // Checks whether the specified resource entry matches all available search options.
  public static boolean matchSearchOptions(ResourceEntry entry, SearchOptions searchOptions)
  {
    if (entry != null && searchOptions != null) {
      try {
        CreResource cre = new CreResource(entry);
        AbstractStruct[] effects;
        AbstractStruct[] items;
        Datatype[] spells;
        boolean retVal = true;
        String key;
        Object o;

        // preparing substructures
        DecNumber ofs = (DecNumber)cre.getAttribute("Effects offset");
        DecNumber cnt = (DecNumber)cre.getAttribute("# effects");
        if (ofs != null && ofs.getValue() > 0 && cnt != null && cnt.getValue() > 0) {
          effects = new AbstractStruct[cnt.getValue()];
          for (int idx = 0; idx < cnt.getValue(); idx++) {
            String label = String.format(SearchOptions.getResourceName(SearchOptions.CRE_Effect), idx);
            effects[idx] = (AbstractStruct)cre.getAttribute(label);
          }
        } else {
          effects = new AbstractStruct[0];
        }

        ofs = (DecNumber)cre.getAttribute("Items offset");
        cnt = (DecNumber)cre.getAttribute("# items");
        if (ofs != null && ofs.getValue() > 0 && cnt != null && cnt.getValue() > 0) {
          items = new AbstractStruct[cnt.getValue()];
          for (int idx = 0; idx < cnt.getValue(); idx++) {
            String label = String.format(SearchOptions.getResourceName(SearchOptions.CRE_Item), idx);
            items[idx] = (AbstractStruct)cre.getAttribute(label);
          }
        } else {
          items = new AbstractStruct[0];
        }

        if (ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2) {
          final String[] spellTypes = new String[]{
              SearchOptions.getResourceName(SearchOptions.CRE_IWD2SpellBard),
              SearchOptions.getResourceName(SearchOptions.CRE_IWD2SpellCleric),
              SearchOptions.getResourceName(SearchOptions.CRE_IWD2SpellDruid),
              SearchOptions.getResourceName(SearchOptions.CRE_IWD2SpellPaladin),
              SearchOptions.getResourceName(SearchOptions.CRE_IWD2SpellRanger),
              SearchOptions.getResourceName(SearchOptions.CRE_IWD2SpellSorcerer),
              SearchOptions.getResourceName(SearchOptions.CRE_IWD2SpellWizard),
              SearchOptions.getResourceName(SearchOptions.CRE_IWD2SpellDomain)};
          final String spellTypesStruct = SearchOptions.getResourceName(SearchOptions.CRE_IWD2SpellBard_Spell);
          final String spellTypesRef = SearchOptions.getResourceName(SearchOptions.CRE_IWD2SpellBard_Spell_ResRef);
          List<Datatype> listSpells = new ArrayList<Datatype>(64);
          for (int i = 0; i < spellTypes.length; i++) {
            for (int j = 1; j < 10; j++) {
              String label = String.format(spellTypes[i], j);
              AbstractStruct struct1 = (AbstractStruct)cre.getAttribute(label);
              if (struct1 != null) {
                AbstractStruct struct2 = (AbstractStruct)struct1.getAttribute(spellTypesStruct);
                if (struct2 != null) {
                  Datatype struct3 = (Datatype)struct2.getAttribute(spellTypesRef);
                  if (struct3 != null) {
                    listSpells.add(struct3);
                  }
                }
              }
            }
          }
          spells = new Datatype[listSpells.size()];
          for (int i = 0; i < spells.length; i++) {
            spells[i] = listSpells.get(i);
          }
        } else {
          ofs = (DecNumber)cre.getAttribute("Known spells offset");
          cnt = (DecNumber)cre.getAttribute("# known spells");
          if (ofs != null && ofs.getValue() > 0 && cnt != null && cnt.getValue() > 0) {
            spells = new Datatype[cnt.getValue()];
            final String spellLabel = SearchOptions.getResourceName(SearchOptions.CRE_Spell_Spell1);
            for (int idx = 0; idx < cnt.getValue(); idx++) {
              String label = String.format(SearchOptions.getResourceName(SearchOptions.CRE_Spell), idx);
              AbstractStruct struct = (AbstractStruct)cre.getAttribute(label);
              spells[idx] = (Datatype)struct.getAttribute(spellLabel);
            }
          } else {
            spells = new Datatype[0];
          }
        }

        // checking options
        String[] keyList = new String[]{SearchOptions.CRE_Name, SearchOptions.CRE_ScriptName};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            StructEntry struct = cre.getAttribute(SearchOptions.getResourceName(key));
            retVal &= SearchOptions.Utils.matchString(struct, o, false, false);
          } else {
            break;
          }
        }

        keyList = new String[]{SearchOptions.CRE_Script1, SearchOptions.CRE_Script2};
        String[] scriptFields;
        if (ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2) {
          scriptFields = new String[]{"Team script", "Special script 1", "Override script",
                                      "Special script 2", "Combat script", "Special script 3",
                                      "Movement script"};
        } else {
          scriptFields = new String[]{"Override script", "Class script", "Race script",
                                      "General script", "Default script"};
        }
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            boolean found = false;
            key = keyList[idx];
            o = searchOptions.getOption(key);
            for (int idx2 = 0; idx2 < scriptFields.length; idx2++) {
              StructEntry struct = cre.getAttribute(scriptFields[idx2]);
              found |= SearchOptions.Utils.matchResourceRef(struct, o, false);
            }
            retVal &= found;
          }
        }

        keyList = new String[]{SearchOptions.CRE_Flags, SearchOptions.CRE_Feats1,
                               SearchOptions.CRE_Feats2, SearchOptions.CRE_Feats3,
                               SearchOptions.CRE_Attributes};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            StructEntry struct = cre.getAttribute(SearchOptions.getResourceName(key));
            retVal &= SearchOptions.Utils.matchFlags(struct, o);
          } else {
            break;
          }
        }

        keyList = new String[]{SearchOptions.CRE_Animation, SearchOptions.CRE_General,
                               SearchOptions.CRE_Class, SearchOptions.CRE_Specifics,
                               SearchOptions.CRE_Alignment, SearchOptions.CRE_Gender,
                               SearchOptions.CRE_Sex, SearchOptions.CRE_Race,
                               SearchOptions.CRE_Allegiance, SearchOptions.CRE_Kit,
                               SearchOptions.CRE_Level1, SearchOptions.CRE_Level2, SearchOptions.CRE_Level3,
                               SearchOptions.CRE_IWD2LevelTotal, SearchOptions.CRE_IWD2LevelBarbarian,
                               SearchOptions.CRE_IWD2LevelBard, SearchOptions.CRE_IWD2LevelCleric,
                               SearchOptions.CRE_IWD2LevelDruid, SearchOptions.CRE_IWD2LevelFighter,
                               SearchOptions.CRE_IWD2LevelMonk, SearchOptions.CRE_IWD2LevelPaladin,
                               SearchOptions.CRE_IWD2LevelRanger, SearchOptions.CRE_IWD2LevelRogue,
                               SearchOptions.CRE_IWD2LevelSorcerer, SearchOptions.CRE_IWD2LevelWizard};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            StructEntry struct = cre.getAttribute(SearchOptions.getResourceName(key));
            retVal &= SearchOptions.Utils.matchNumber(struct, o);
          } else {
            break;
          }
        }

        keyList = new String[]{SearchOptions.CRE_Effect_Type1, SearchOptions.CRE_Effect_Type2,
                               SearchOptions.CRE_Effect_Type3, SearchOptions.CRE_Effect_Type4};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            boolean found = false;
            key = keyList[idx];
            o = searchOptions.getOption(key);
            for (int idx2 = 0; idx2 < effects.length; idx2++) {
              if (!found) {
                if (effects[idx2] != null) {
                  StructEntry struct = effects[idx2].getAttribute(SearchOptions.getResourceName(key));
                  found |= SearchOptions.Utils.matchNumber(struct, o);
                }
              } else {
                break;
              }
            }
            retVal &= found || (o == null);
          } else {
            break;
          }
        }

        keyList = new String[]{SearchOptions.CRE_Item_Item1, SearchOptions.CRE_Item_Item2,
                               SearchOptions.CRE_Item_Item3, SearchOptions.CRE_Item_Item4};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            boolean found = false;
            key = keyList[idx];
            o = searchOptions.getOption(key);
            for (int idx2 = 0; idx2 < items.length; idx2++) {
              if (!found) {
                if (items[idx2] != null) {
                  StructEntry struct = items[idx2].getAttribute(SearchOptions.getResourceName(key));
                  found |= SearchOptions.Utils.matchResourceRef(struct, o, false);
                }
              } else {
                break;
              }
            }
            retVal &= found || (o == null);
          } else {
            break;
          }
        }

        keyList = new String[]{SearchOptions.CRE_Spell_Spell1, SearchOptions.CRE_Spell_Spell2,
                               SearchOptions.CRE_Spell_Spell3, SearchOptions.CRE_Spell_Spell4};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            boolean found = false;
            key = keyList[idx];
            o = searchOptions.getOption(key);
            for (int idx2 = 0; idx2 < spells.length; idx2++) {
              if (!found) {
                if (spells[idx2] != null) {
                  found |= SearchOptions.Utils.matchResourceRef(spells[idx2], o, false);
                }
              } else {
                break;
              }
            }
            retVal &= found || (o == null);
          } else {
            break;
          }
        }

        keyList = new String[]{SearchOptions.CRE_Custom1, SearchOptions.CRE_Custom2,
                               SearchOptions.CRE_Custom3, SearchOptions.CRE_Custom4};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            retVal &= SearchOptions.Utils.matchCustomFilter(cre, o);
          } else {
            break;
          }
        }

        return retVal;
      } catch (Exception e) {
      }
    }
    return false;
  }
}

