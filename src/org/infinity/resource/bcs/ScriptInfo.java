// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.bcs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.infinity.resource.Profile;
import org.infinity.resource.bcs.Signatures.Function;

/**
 * Central hub for engine-specific information about script features and exceptions.
 */
public class ScriptInfo {
  private static final HashMap<Profile.Engine, ScriptInfo> MAP_INFO = new HashMap<>();

  static {
    // *** Profile.Engine.BG1 ***
    ScriptInfo si = new ScriptInfo(new String[] { "EA", "GENERAL", "RACE", "CLASS", "SPECIFIC", "GENDER", "ALIGN" },
        new String[] { "GLOBAL", "LOCALS", "MYAREA" });
    MAP_INFO.put(Profile.Engine.BG1, si);
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x0001, 'S', 0), "ITM");  // Acquired
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x0009, 'S', 0), "ITM");  // Unusable
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4061, 'S', 0), "ITM");  // HasItem
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4075, 'S', 0), "ITM");  // Contains
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4051, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // Dead
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4071, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // NumDead
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4072, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // NumDeadGT
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4073, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // NumDeadLT
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 161, 'S', 0), "2DA"); // IncrementChapter
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 199, 'S', 0), "2DA"); // TextList
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 170, 'S', 0), "ARE"); // RevealAreaOnMap
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 185, 'S', 0), "ARE"); // SetMasterArea
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 7, 'S', 0), "CRE");   // CreateCreature
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 194, 'S', 0), "CRE"); // ChangeAnimation
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 9, 'S', 0), "ITM");   // DropItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 11, 'S', 0), "ITM");  // EquipItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 14, 'S', 0), "ITM");  // GetItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 15, 'S', 0), "ITM");  // GiveItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 34, 'S', 0), "ITM");  // UseItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 82, 'S', 0), "ITM");  // CreateItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 140, 'S', 0), "ITM"); // GiveItemCreate
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 169, 'S', 0), "ITM"); // DestroyItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 167, 'S', 0), "MVE"); // StartMovie
    si.functionConcatMap.put(0x400F, 0x0001); // Global
    si.functionConcatMap.put(0x4034, 0x0001); // GlobalGT
    si.functionConcatMap.put(0x4035, 0x0001); // GlobalLT
    si.functionConcatMap.put(30, 0x0001); // SetGlobal
    si.functionConcatMap.put(109, 0x0001); // IncrementGlobal
    si.functionConcatMap.put(115, 0x0001); // SetGlobalTimer
    si.functionConcatMap.put(141, 0x0001); // GivePartyGoldGlobal
    si.functionConcatMap.put(165, 0x0001); // AddexperiencePartyGlobal
    si.functionParamCommentMap.put(151, 1);  // DisplayString
    si.functionParamCommentMap.put(197, 1);  // MoveGlobal

    // *** Profile.Engine.BG2 ***
    si = new ScriptInfo(MAP_INFO.get(Profile.Engine.BG1), null);
    MAP_INFO.put(Profile.Engine.BG2, si);
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x40C6, 'S', 0), "");  // G
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x40C7, 'S', 0), "");  // GGT
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x40C8, 'S', 0), "");  // GLT
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x407E, 'S', 0), "ARE");  // AreaCheck
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x40D4, 'S', 0), "ARE");  // AreaCheckObject
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4092, 'S', 0), "CRE");  // InLine
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4077, 'S', 0), "ITM");  // NumItems
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4078, 'S', 0), "ITM");  // NumItemsGT
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4079, 'S', 0), "ITM");  // NumItemsLT
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x407A, 'S', 0), "ITM");  // NumItemsParty
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x407B, 'S', 0), "ITM");  // NumItemsPartyGT
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x407C, 'S', 0), "ITM");  // NumItemsPartyLT
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x407F, 'S', 0), "ITM");  // HasItemEquiped
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x40A9, 'S', 0), "ITM");  // PartyHasItemIdentified
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x40C2, 'S', 0), "ITM");  // HasItemEquipedReal
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4104, 'S', 0), "ITM");  // CurrentAmmo
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x40A5, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // Name
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x40DF, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // BeenInParty
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 220, 'S', 0), "2DA"); // TakeItemListParty
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 226, 'S', 0), "2DA"); // TakeItemListPartyNum
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 251, 'S', 0), "ARE"); // HideAreaOnMap
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 264, 'S', 0), "ARE"); // CopyGroundPilesTo
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 277, 'S', 0), "ARE"); // EscapeAreaObjectMove
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 219, 'S', 0), "CRE"); // ChangeAnimationNoEffect
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 227, 'S', 0), "CRE"); // CreateCreatureObject
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 228, 'S', 0), "CRE"); // CreateCreatureImpassable
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 231, 'S', 0), "CRE"); // CreateCreatureDoor
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 232, 'S', 0), "CRE"); // CreateCreatureObjectDoor
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 233, 'S', 0), "CRE"); // CreateCreatureObjectOffScreen
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 238, 'S', 0), "CRE"); // CreateCreatureOffScreen
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 250, 'S', 0), "CRE"); // CreateCreatureObjectCopy
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 252, 'S', 0), "CRE"); // CreateCreatureObjectOffset
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 295, 'S', 0), "CRE"); // CreateCreatureCopyPoint
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 323, 'S', 0), "CRE"); // CreateCreatureImpassableAllowOverlap
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 246, 'S', 2), "CRE"); // CreateCreatureAtLocation
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 204, 'S', 0), "ITM"); // TakePartyItemNum
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 257, 'S', 0), "ITM"); // PickUpItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 256, 'S', 2), "ITM"); // CreateItemGlobal
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 279, 'S', 0), "SPL"); // AddSpecialAbility
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 272, 'S', 0), "VEF:VVC:BAM"); // CreateVisualEffect
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 273, 'S', 0), "VEF:VVC:BAM"); // CreateVisualEffectObject
    si.functionConcatMap.put(246, 0x0001); // CreateCreatureAtLocation
    si.functionConcatMap.put(256, 0x0001); // CreateItemGlobal
    si.functionConcatMap.put(268, 0x0001); // RealSetGlobalTimer
    si.functionConcatMap.put(297, 0x0001); // MoveToSavedLocation
    si.functionConcatMap.put(335, 0x0001); // SetTokenGlobal
    si.functionParamCommentMap.put(246, 2);  // CreateCreatureAtLocation
    si.functionParamCommentMap.put(256, 2);  // CreateItemGlobal
    si.functionParamCommentMap.put(262, 1);  // DisplayStringNoName
    si.functionParamCommentMap.put(269, 1);  // DisplayStringHead
    si.functionParamCommentMap.put(292, 1);  // DisplayStringHeadOwner
    si.functionParamCommentMap.put(311, 1);  // DisplayStringWait
    si.functionParamCommentMap.put(342, 1);  // DisplayStringHeadDead
    si.functionParamCommentMap.put(346, 1);  // DisplayStringNoNameHead
    si.addFunctionDefinition(Function.FunctionType.TRIGGER, "0x4100 TriggerOverride(O:Object*,T:Trigger*)");

    // *** Profile.Engine.EE ***
    si = new ScriptInfo(MAP_INFO.get(Profile.Engine.BG2), null);
    MAP_INFO.put(Profile.Engine.EE, si);
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 445, 'S', 0), "CRE"); // CreateCreatureAtFeet (PSTEE)
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 438, 'S', 1), "DLG"); // ChangeDialog (PSTEE)
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 431, 'S', 0), "ITM"); // DestroyPartyItem (PSTEE)
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 455, 'S', 0), "ITM"); // DestroyItemObject (PSTEE)
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 432, 'S', 0), "ITM"); // TransformPartyItem (PSTEE)
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 432, 'S', 1), "ITM"); // TransformPartyItem (PSTEE)
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 167, 'S', 0), "WBM:MVE"); // StartMovie
    si.functionConcatMap.put(0x4109, 0x0001); // StuffGlobalRandom (PSTEE)
    si.functionConcatMap.put(364, 0x0001); // SetGlobalRandom
    si.functionConcatMap.put(377, 0x0001); // SetGlobalTimerRandom
    si.functionConcatMap.put(446, 0x0011 | (5 << 16)); // IncrementGlobalOnce (PSTEE)
    si.functionParamCommentMap.put(362, 1);  // RemoveStoreItem
    si.functionParamCommentMap.put(363, 1);  // AddStoreItem
    si.functionParamCommentMap.put(376, 1);  // DisplayStringNoNameDlg
    si.functionParamCommentMap.put(388, 1);  // DisplayStringHeadNoLog
    si.removeFunctionDefinition(Function.FunctionType.TRIGGER, "0x4100 TriggerOverride(O:Object*,T:Trigger*)"); // leftover from BG2 profile
    si.addFunctionDefinition(Function.FunctionType.TRIGGER, "0x40e0 TriggerOverride(O:Object*,T:Trigger*)");

    // *** Profile.Engine.IWD ***
    si = new ScriptInfo(new String[] { "EA", "GENERAL", "RACE", "CLASS", "SPECIFIC", "GENDER", "ALIGN" },
        new String[] { "GLOBAL", "LOCALS", "MYAREA" });
    MAP_INFO.put(Profile.Engine.IWD, si);
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x0001, 'S', 0), "ITM");  // Acquired
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x0009, 'S', 0), "ITM");  // Unusable
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4061, 'S', 0), "ITM");  // HasItem
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4051, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // Dead
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4071, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // NumDead
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4072, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // NumDeadGT
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4073, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // NumDeadLT
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 161, 'S', 0), "2DA"); // IncrementChapter
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 199, 'S', 0), "2DA"); // TextScreen
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 170, 'S', 0), "ARE"); // RevealAreaOnMap
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 185, 'S', 0), "ARE"); // SetMasterArea
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 7, 'S', 0), "CRE");   // CreateCreature
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 194, 'S', 0), "CRE"); // ChangeAnimation
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 9, 'S', 0), "ITM");   // DropItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 11, 'S', 0), "ITM");  // EquipItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 14, 'S', 0), "ITM");  // GetItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 15, 'S', 0), "ITM");  // GiveItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 34, 'S', 0), "ITM");  // UseItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 82, 'S', 0), "ITM");  // CreateItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 140, 'S', 0), "ITM"); // GiveItemCreate
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 169, 'S', 0), "ITM"); // DestroyItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 167, 'S', 0), "MVE"); // StartMovie
    si.functionConcatMap.put(0x400F, 0x0001); // Global
    si.functionConcatMap.put(0x4034, 0x0001); // GlobalGT
    si.functionConcatMap.put(0x4035, 0x0001); // GlobalLT
    si.functionConcatMap.put(30, 0x0001); // SetGlobal
    si.functionConcatMap.put(109, 0x0001); // IncrementGlobal
    si.functionConcatMap.put(115, 0x0001); // SetGlobalTimer
    si.functionConcatMap.put(141, 0x0001); // GivePartyGoldGlobal
    si.functionConcatMap.put(165, 0x0001); // AddexperiencePartyGlobal
    si.functionConcatMap.put(243, 0x0011); // IncrementGlobalOnce
    si.functionConcatMap.put(0x40A5, 0x0101); // BitGlobal
    si.functionConcatMap.put(247, 0x0101); // BitGlobal
    si.functionConcatMap.put(0x40A6, 0x1111); // GlobalBitGlobal
    si.functionConcatMap.put(248, 0x1111); // GlobalBitGlobal
    si.functionParamCommentMap.put(151, 1);  // DisplayString
    si.functionParamCommentMap.put(197, 1);  // MoveGlobal

    // *** Profile.Engine.IWD2 ***
    si = new ScriptInfo(new String[] { "EA", "GENERAL", "RACE", "CLASS", "SPECIFIC", "GENDER", "ALIGNMNT", "SUBRACE",
        "AVCLASS", "CLASSMSK" }, new String[] { "GLOBAL", "LOCALS", "MYAREA" });
    MAP_INFO.put(Profile.Engine.IWD2, si);
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x40A5, 'S', 1), "ARE");  // BitGlobal
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x40A6, 'S', 1), "ARE");  // GlobalBitGlobal
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x40A6, 'S', 3), "ARE");  // GlobalBitGlobal
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x0001, 'S', 0), "ITM");  // Acquired
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x0009, 'S', 0), "ITM");  // Unusable
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4061, 'S', 0), "ITM");  // HasItem
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4075, 'S', 0), "ITM");  // Contains
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4077, 'S', 0), "ITM");  // NumItems
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4078, 'S', 0), "ITM");  // NumItemsGT
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4079, 'S', 0), "ITM");  // NumItemsLT
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x407A, 'S', 0), "ITM");  // NumItemsParty
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x407B, 'S', 0), "ITM");  // NumItemsPartyGT
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x407C, 'S', 0), "ITM");  // NumItemsPartyLT
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x40C1, 'S', 0), "ITM");  // TotalItemCntExclude
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x40C2, 'S', 0), "ITM");  // TotalItemCntExcludeGT
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x40C3, 'S', 0), "ITM");  // TotalItemCntExcludeLT
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x40C5, 'S', 0), "ITM");  // ItemIsIdentified
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x40C8, 'S', 0), "SPL");  // HasInnateAbility
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4071, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // NumDead
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4072, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // NumDeadGT
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4073, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // NumDeadLT
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 161, 'S', 0), "2DA"); // IncrementChapter
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 199, 'S', 0), "2DA"); // TextScreen
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 170, 'S', 0), "ARE"); // RevealAreaOnMap
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 185, 'S', 0), "ARE"); // SetMasterArea
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 247, 'S', 1), "ARE"); // BitGlobal
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 243, 'S', 1), "ARE"); // IncrementGlobalOnce
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 248, 'S', 1), "ARE"); // GlobalBitGlobal
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 243, 'S', 3), "ARE"); // IncrementGlobalOnce
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 248, 'S', 3), "ARE"); // GlobalBitGlobal
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 7, 'S', 0), "CRE");   // CreateCreature
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 194, 'S', 0), "CRE"); // ChangeAnimation
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 278, 'S', 0), "ITM"); // DropInventoryEXExclude
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 9, 'S', 0), "ITM");   // DropItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 11, 'S', 0), "ITM");  // EquipItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 14, 'S', 0), "ITM");  // GetItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 15, 'S', 0), "ITM");  // GiveItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 34, 'S', 0), "ITM");  // UseItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 82, 'S', 0), "ITM");  // CreateItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 140, 'S', 0), "ITM"); // GiveItemCreate
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 169, 'S', 0), "ITM"); // DestroyItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 167, 'S', 0), "MVE"); // StartMovie
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 321, 'S', 0), Function.Parameter.RESTYPE_SPELL_LIST); // MarkSpellAndObject
    si.functionConcatMap.put(0x400F, 0x0001); // Global
    si.functionConcatMap.put(0x4034, 0x0001); // GlobalGT
    si.functionConcatMap.put(0x4035, 0x0001); // GlobalLT
    si.functionConcatMap.put(30, 0x0001); // SetGlobal
    si.functionConcatMap.put(109, 0x0001); // IncrementGlobal
    si.functionConcatMap.put(115, 0x0001); // SetGlobalTimer
    si.functionConcatMap.put(141, 0x0001); // GivePartyGoldGlobal
    si.functionConcatMap.put(165, 0x0001); // AddexperiencePartyGlobal
    si.functionConcatMap.put(308, 0x0001); // SetGlobalTimerOnce
    si.functionConcatMap.put(243, 0x0011); // IncrementGlobalOnce
    si.functionConcatMap.put(0x40A5, 0x0101); // BitGlobal
    si.functionConcatMap.put(247, 0x0101); // BitGlobal
    si.functionConcatMap.put(306, 0x0101); // SetGlobalRandom
    si.functionConcatMap.put(307, 0x0101); // SetGlobalTimerRandom
    si.functionConcatMap.put(0x40A6, 0x1111); // GlobalBitGlobal
    si.functionConcatMap.put(289, 0x1010); // SpellCastEffect
    si.functionConcatMap.put(248, 0x1111); // GlobalBitGlobal
    si.functionParamCommentMap.put(151, 1);  // DisplayString
    si.functionParamCommentMap.put(197, 1);  // MoveGlobal

    // *** Profile.Engine.PST ***
    si = new ScriptInfo(
        new String[] { "EA", "FACTION", "TEAM", "GENERAL", "RACE", "CLASS", "SPECIFIC", "GENDER", "ALIGN" },
        new String[] { "GLOBAL", "LOCALS", "MYAREA", "KAPUTZ" });
    MAP_INFO.put(Profile.Engine.PST, si);
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x0001, 'S', 0), "ITM");  // Acquired
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x0009, 'S', 0), "ITM");  // Unusable
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4061, 'S', 0), "ITM");  // HasItem
    si.functionResTypeMap.put(key(Function.FunctionType.TRIGGER, 0x4051, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // Dead
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 161, 'S', 0), "2DA"); // IncrementChapter
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 199, 'S', 0), "2DA"); // TextList
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 170, 'S', 0), "ARE"); // RevealAreaOnMap
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 185, 'S', 0), "ARE"); // SetMasterArea
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 7, 'S', 0), "CRE");   // CreateCreature
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 295, 'S', 0), "CRE"); // CreateCreatureAtFeet
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 272, 'S', 1), "DLG"); // ChangeDialog
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 9, 'S', 0), "ITM");   // DropItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 11, 'S', 0), "ITM");  // EquipItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 14, 'S', 0), "ITM");  // GetItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 15, 'S', 0), "ITM");  // GiveItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 34, 'S', 0), "ITM");  // UseItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 82, 'S', 0), "ITM");  // CreateItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 140, 'S', 0), "ITM"); // GiveItemCreate
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 169, 'S', 0), "ITM"); // DestroyItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 222, 'S', 0), "ITM"); // DestroyPartyItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 223, 'S', 0), "ITM"); // TransformItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 224, 'S', 0), "ITM"); // TransformPartyItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 307, 'S', 0), "ITM"); // TransformItemAll
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 308, 'S', 0), "ITM"); // TransformPartyItemAll
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 224, 'S', 1), "ITM"); // TransformPartyItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 308, 'S', 1), "ITM"); // TransformPartyItemAll
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 223, 'S', 2), "ITM"); // TransformItem
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 307, 'S', 2), "ITM"); // TransformItemAll
    si.functionResTypeMap.put(key(Function.FunctionType.ACTION, 167, 'S', 0), "MVE"); // StartMovie
    si.functionConcatMap.put(0x400F, 0x0001); // Global
    si.functionConcatMap.put(0x4034, 0x0001); // GlobalGT
    si.functionConcatMap.put(0x4035, 0x0001); // GlobalLT
    si.functionConcatMap.put(0x407F, 0x0001); // BitCheck
    si.functionConcatMap.put(0x4080, 0x0001); // GlobalBAND
    si.functionConcatMap.put(0x4081, 0x0001); // BitCheckExact
    si.functionConcatMap.put(0x4095, 0x0001); // Xor
    si.functionConcatMap.put(0x409C, 0x0001); // StuffGlobalRandom
    si.functionConcatMap.put(30, 0x0001); // SetGlobal
    si.functionConcatMap.put(109, 0x0001); // IncrementGlobal
    si.functionConcatMap.put(115, 0x0001); // SetGlobalTimer
    si.functionConcatMap.put(141, 0x0001); // GivePartyGoldGlobal
    si.functionConcatMap.put(165, 0x0001); // AddexperiencePartyGlobal
    si.functionConcatMap.put(227, 0x0001); // GlobalBAND
    si.functionConcatMap.put(228, 0x0001); // GlobalBOR
    si.functionConcatMap.put(229, 0x0001); // GlobalSHR
    si.functionConcatMap.put(230, 0x0001); // GlobalSHL
    si.functionConcatMap.put(231, 0x0001); // GlobalMAX
    si.functionConcatMap.put(232, 0x0001); // GlobalMIN
    si.functionConcatMap.put(244, 0x0001); // BitSet
    si.functionConcatMap.put(245, 0x0001); // BitClear
    si.functionConcatMap.put(260, 0x0001); // GlobalXOR
    si.functionConcatMap.put(0x4082, 0x0011); // GlobalEqualsGlobal
    si.functionConcatMap.put(0x4083, 0x0011); // GlobalLTGlobal
    si.functionConcatMap.put(0x4084, 0x0011); // GlobalGTGlobal
    si.functionConcatMap.put(0x4085, 0x0011); // GlobalANDGlobal
    si.functionConcatMap.put(0x4086, 0x0011); // GlobalORGlobal
    si.functionConcatMap.put(0x4087, 0x0011); // GlobalBANDGlobal
    si.functionConcatMap.put(0x4088, 0x0011); // GlobalBANDGlobalExact
    si.functionConcatMap.put(202, 0x0011); // IncrementGlobalOnce
    si.functionConcatMap.put(233, 0x0011); // GlobalSetGlobal
    si.functionConcatMap.put(234, 0x0011); // GlobalAddGlobal
    si.functionConcatMap.put(235, 0x0011); // GlobalSubGlobal
    si.functionConcatMap.put(236, 0x0011); // GlobalANDGlobal
    si.functionConcatMap.put(237, 0x0011); // GlobalORGlobal
    si.functionConcatMap.put(238, 0x0011); // GlobalBANDGlobal
    si.functionConcatMap.put(239, 0x0011); // GlobalBORGlobal
    si.functionConcatMap.put(240, 0x0011); // GlobalSHRGlobal
    si.functionConcatMap.put(241, 0x0011); // GlobalSHLGlobal
    si.functionConcatMap.put(242, 0x0011); // GlobalMAXGlobal
    si.functionConcatMap.put(243, 0x0011); // GlobalMINGlobal
    si.functionConcatMap.put(261, 0x0011); // GlobalXORGlobal
    si.functionParamCommentMap.put(151, 1);  // DisplayString
    si.addFunctionDefinition(Function.FunctionType.TRIGGER, "0x4070 Clicked(O:Object*)");

    // *** Profile.Engine.Unknown ***
    si = new ScriptInfo(MAP_INFO.get(Profile.Engine.BG2), null);
    MAP_INFO.put(Profile.Engine.Unknown, si);
  }

  /**
   * Maps parameter information returned by the method {@code key(FunctionType ftype, int id, char type, int index)} to
   * one or more resource types.
   *
   * @param Key   A colon-separated list of function type, function id, parameter type (I, S, O or P) and parameter
   *              position.<br>
   *              <b>Example:</b> {@code "A:7:S:1"} points to the parameter {@code "S:Effect*"} of the action function
   *              {@code "7 CreateCreatureEffect(S:NewObject*,S:Effect*,P:Location*,I:Face*DIR)"}.
   * @param Value A colon-separated list of resource types (in upper case) or special keywords (in lower case)
   *              associated with the parameter.
   */
  public final Map<String, String> functionResTypeMap = new HashMap<>(128);

  /**
   * Returns number and type of concatenated strings of the specified trigger or action specified by code.
   *
   * @param Key   The trigger or action function code.
   * @param Value A numeric code: Bit 0: Whether first string parameter consists of two separate strings.<br>
   *              Bit 4: Whether second string parameter consists of two separate strings.<br>
   *              Bit 8: Whether first string parameter is separated by fixed-size string (0) or is colon-separated
   *              (1).<br>
   *              Bit 12: Whether second string parameter is separated by fixed-size string (0) or is colon-separated
   *              (1).<br>
   *              Bits 16..31: Optional number of expected parameter. This is useful to distinguish functions with
   *              identical code, but different signatures.
   */
  public final Map<Integer, Integer> functionConcatMap = new HashMap<>(64);

  /**
   * Lists of additional function signatures for triggers and actions that may not be available from the respective IDS
   * files.
   */
  public final Map<Function.FunctionType, List<String>> functionSignaturesMap = new HashMap<>();

  /**
   * Defines which parameter of a function should generate comments. Unlisted functions generate comments for the first
   * commentable parameter only.
   *
   * @param Key   The trigger or action function code.
   * @param Value The parameter index.
   */
  public final Map<Integer, Integer> functionParamCommentMap = new HashMap<>(64);

  /**
   * List of supported scope names aside from ARE resource names.
   */
  public final String[] SCOPES;

  /** List of IDS resource names for target object specifiers. */
  public final String[] OBJECT_SPECIFIER_IDS;
  /** Array index for {@code OBJECT_SPECIFIER_IDS} referencing EA target. */
  public int IDX_OBJECT_EA        = -1;
  /** Array index for {@code OBJECT_SPECIFIER_IDS} referencing FACTION target (PST-only). */
  public int IDX_OBJECT_FACTION   = -1;
  /** Array index for {@code OBJECT_SPECIFIER_IDS} referencing TEAM target (PST-only). */
  public int IDX_OBJECT_TEAM      = -1;
  /** Array index for {@code OBJECT_SPECIFIER_IDS} referencing GENERAL target. */
  public int IDX_OBJECT_GENERAL   = -1;
  /** Array index for {@code OBJECT_SPECIFIER_IDS} referencing RACE target. */
  public int IDX_OBJECT_RACE      = -1;
  /** Array index for {@code OBJECT_SPECIFIER_IDS} referencing CLASS target. */
  public int IDX_OBJECT_CLASS     = -1;
  /** Array index for {@code OBJECT_SPECIFIER_IDS} referencing SPECIFIC target. */
  public int IDX_OBJECT_SPECIFIC  = -1;
  /** Array index for {@code OBJECT_SPECIFIER_IDS} referencing GENDER target. */
  public int IDX_OBJECT_GENDER    = -1;
  /** Array index for {@code OBJECT_SPECIFIER_IDS} referencing ALIGN/ALIGNMNT target. */
  public int IDX_OBJECT_ALIGN     = -1;
  /** Array index for {@code OBJECT_SPECIFIER_IDS} referencing SUBRACE target (IWD2-only). */
  public int IDX_OBJECT_SUBRACE   = -1;
  /** Array index for {@code OBJECT_SPECIFIER_IDS} referencing AVCLASS target (IWD2-only). */
  public int IDX_OBJECT_AVCLASS   = -1;
  /** Array index for {@code OBJECT_SPECIFIER_IDS} referencing CLASSMSK target (IWD2-only). */
  public int IDX_OBJECT_CLASSMASK = -1;

  /**
   * Returns a {@code ScriptInfo} object for the current game engine.
   */
  public static ScriptInfo getInfo() {
    return getInfo(Profile.getEngine());
  }

  /**
   * Returns the {@code ScriptInfo} object associated with the specified game engine.
   */
  public static ScriptInfo getInfo(Profile.Engine engine) {
    return MAP_INFO.get(engine);
  }

  /**
   * Internally used: Create a correctly formatted string for use as key for {@code FUNCTION_RESTYPE}.
   */
  private static String key(Function.FunctionType ftype, int id, char type, int index) {
    StringBuilder sb = new StringBuilder();
    sb.append(ftype == Function.FunctionType.TRIGGER ? 'T' : 'A').append(':').append(id).append(':')
        .append(Character.toUpperCase(type)).append(':').append(index);
    return sb.toString();
  }

  /**
   * Internally used: Registers a new trigger or action function definition.
   *
   * @param type The function type (action or trigger).
   * @param def  Function definition consisting of function id and signature.
   * @return Whether definition has been added successfully.
   */
  private boolean addFunctionDefinition(Function.FunctionType type, String def) {
    if (def != null) {
      List<String> list = functionSignaturesMap.get(type);
      if (list != null) {
        list.add(def);
        return true;
      }
    }
    return false;
  }

  /**
   * Internally used: Removes the specified trigger or action function definition from the list.
   *
   * @param type The function type (action or trigger).
   * @param def  Exact function definition consisting of function id and signature.
   * @return Whether definition has been removed successfully.
   */
  private boolean removeFunctionDefinition(Function.FunctionType type, String def) {
    if (def != null) {
      List<String> list = functionSignaturesMap.get(type);
      if (list != null) {
        return list.remove(def);
      }
    }
    return false;
  }

  protected ScriptInfo(String[] objectSpecifierIds, String[] scopes) {
    functionSignaturesMap.put(Function.FunctionType.ACTION, new ArrayList<>());
    functionSignaturesMap.put(Function.FunctionType.TRIGGER, new ArrayList<>());
    SCOPES = scopes;
    OBJECT_SPECIFIER_IDS = objectSpecifierIds;
    for (int i = 0; i < OBJECT_SPECIFIER_IDS.length; i++) {
      if (OBJECT_SPECIFIER_IDS[i].equals("EA")) {
        IDX_OBJECT_EA = i;
      } else if (OBJECT_SPECIFIER_IDS[i].equals("FACTION")) {
        IDX_OBJECT_FACTION = i;
      } else if (OBJECT_SPECIFIER_IDS[i].equals("TEAM")) {
        IDX_OBJECT_TEAM = i;
      } else if (OBJECT_SPECIFIER_IDS[i].equals("GENERAL")) {
        IDX_OBJECT_GENERAL = i;
      } else if (OBJECT_SPECIFIER_IDS[i].equals("RACE")) {
        IDX_OBJECT_RACE = i;
      } else if (OBJECT_SPECIFIER_IDS[i].equals("CLASS")) {
        IDX_OBJECT_CLASS = i;
      } else if (OBJECT_SPECIFIER_IDS[i].equals("SPECIFIC")) {
        IDX_OBJECT_SPECIFIC = i;
      } else if (OBJECT_SPECIFIER_IDS[i].equals("GENDER")) {
        IDX_OBJECT_GENDER = i;
      } else if (OBJECT_SPECIFIER_IDS[i].equals("ALIGN") || OBJECT_SPECIFIER_IDS[i].equals("ALIGNMNT")) {
        IDX_OBJECT_ALIGN = i;
      } else if (OBJECT_SPECIFIER_IDS[i].equals("SUBRACE")) {
        IDX_OBJECT_SUBRACE = i;
      } else if (OBJECT_SPECIFIER_IDS[i].equals("AVCLASS")) {
        IDX_OBJECT_AVCLASS = i;
      } else if (OBJECT_SPECIFIER_IDS[i].equals("CLASSMSK")) {
        IDX_OBJECT_CLASSMASK = i;
      }
    }
  }

  protected ScriptInfo(ScriptInfo obj, String[] objectSpecifierIds) {
    this((objectSpecifierIds != null) ? objectSpecifierIds : obj.OBJECT_SPECIFIER_IDS, obj.SCOPES);
    this.functionResTypeMap.putAll(obj.functionResTypeMap);
    this.functionConcatMap.putAll(obj.functionConcatMap);
    for (final Map.Entry<Function.FunctionType, List<String>> entry : obj.functionSignaturesMap.entrySet()) {
      List<String> oldList = entry.getValue();
      if (oldList != null) {
        this.functionSignaturesMap.put(entry.getKey(), new ArrayList<>(oldList));
      }
    }
    this.functionParamCommentMap.putAll(obj.functionParamCommentMap);
  }

  /** Returns object specifier IDS resource names. */
  public String[] getObjectIdsList() {
    return OBJECT_SPECIFIER_IDS;
  }

  /**
   * Determines the resource type associated with the specified function parameter.
   *
   * @param ftype Trigger or Action?
   * @param id    Numeric function identifier from IDS.
   * @param param Required for name, type and ids reference.
   * @param index Position of parameter in parameter list of the function.
   * @return A string with colon-separated resource types (in upper case) or special keywords (in lower case). An empty
   *         string otherwise.
   */
  public String getResType(Function.FunctionType ftype, int id, Function.Parameter param, int index) {
    String retVal = functionResTypeMap.get(key(ftype, id, param.getType(), index));

    // check more generally by matching parameter names
    if (retVal == null) {
      String name = param.getName().toLowerCase(Locale.ENGLISH);
      if (param.getType() == Function.Parameter.TYPE_STRING) {
        if (name.endsWith("scriptname")) {
          retVal = Function.Parameter.RESTYPE_SCRIPT;
        } else if (name.startsWith("area") || name.endsWith("area") || name.equals("scope")) {
          retVal = "ARE";
        } else if (name.startsWith("spell") || name.equals("res")) {
          retVal = "SPL";
        } else if (name.startsWith("item") || name.endsWith("item") || name.equals("give") || name.equals("take")) {
          retVal = "ITM";
        } else if (name.startsWith("dialogfile")) {
          retVal = "DLG";
        } else if (name.equals("cutscene") || name.equals("scriptfile") || name.equals("script")) {
          retVal = "BCS";
        } else if (name.startsWith("sound") || name.startsWith("voice")) {
          retVal = "WAV";
        } else if (name.startsWith("effect")) {
          retVal = "VEF:VVC:BAM";
          // } else if (name.equals("parchment")) { // most likely incorrect
          // retVal = "MOS";
        } else if (name.equals("store")) {
          retVal = "STO";
        } else if (name.equals("bamresref")) {
          retVal = "BAM";
        } else if (name.equals("pool")) {
          retVal = "SRC";
        } else if (name.equals("palette")) {
          retVal = "BMP";
        } else if (name.equals("worldmap")) {
          retVal = "WMP";
        } else if (name.equals("resref")) {
          // generic fallback solution
          retVal = "CRE:ITM:ARE:2DA:BCS:WBM:MVE:SPL:DLG:VEF:VVC:BAM:BMP";
        }
      } else if (param.getType() == Function.Parameter.TYPE_INTEGER) {
        if (name.equals("strref") || name.equals("stringref") || name.equals("entry")) {
          retVal = "TLK";
        } else if (param.getIdsRef().equalsIgnoreCase("spell")) {
          retVal = "SPL";
        }
      }
    }

    if (retVal == null || retVal.isEmpty()) {
      return "";
    } else {
      return retVal;
    }
  }

  /**
   * Returns an unmodifyable list of function definitions for the specified type.
   *
   * @param type The function type (action or trigger).
   * @return An unmodifyable list of function definitions.
   */
  public List<String> getFunctionDefinitions(Function.FunctionType type) {
    return Collections.unmodifiableList(functionSignaturesMap.get(type));
  }

  /**
   * Returns whether the specified scope name (e.g. GLOBAL, LOCALS, MYAREA) is supported by the current game.
   */
  public boolean isGlobalScope(String scope) {
    boolean retVal = false;
    if (scope != null && scope.length() == 6) {
      scope = scope.toUpperCase(Locale.ENGLISH);
      for (final String s : SCOPES) {
        if (s.equals(scope)) {
          retVal = true;
          break;
        }
      }
    }
    return retVal;
  }

  /** Returns all available scope names for the current game. */
  public String[] getGlobalScopes() {
    return SCOPES;
  }

  /** Returns whether the parameter at the specified index may generate comments. */
  public boolean isCommentAllowed(int code, int paramIndex) {
    if (functionParamCommentMap.containsKey(Integer.valueOf(code))) {
      return (paramIndex >= functionParamCommentMap.get(Integer.valueOf(code)).intValue());
    } else {
      return true;
    }
  }

  /**
   * Returns whether the string argument at the specified position has to be splitted from a string parameter.
   *
   * @param code          Trigger or action function code.
   * @param position      Relative string argument position (in range: [0, 3]).
   * @param numParameters Number of expected parameters for this function. Specify 0 to ignore.
   * @return {@code true} if argument is part of a combined string parameter, {@code false} otherwise.
   */
  public boolean isCombinedString(int code, int position, int numParameters) {
    boolean retVal = false;
    Integer v = functionConcatMap.get(Integer.valueOf(code));
    if (v != null) {
      int numParams = (v >> 16) & 0xffff;
      if (numParams == 0 || numParameters == 0 || numParams == numParameters) {
        int mask = v & 0xff;
        int pos = 0;
        while (pos < position) {
          int ofs = ((mask & 1) != 0) ? 2 : 1;
          if (position < pos + ofs) {
            break;
          }
          pos += ofs;
          mask >>= 4;
        }
        retVal = (mask & 1) != 0;
      }
    }
    return retVal;
  }

  /**
   * Returns whether the string argument is included in a colon-separated string parameter.
   *
   * @param code          Trigger of action function code.
   * @param position      Relative string argument position (in range: [0, 3]).
   * @param numParameters Number of expected parameters for this function. Specify 0 to ignore.
   * @return {@code true} if argument is part of a colon-separated string parameter, {@code false} otherwise.
   */
  public boolean isColonSeparatedString(int code, int position, int numParameters) {
    boolean retVal = false;
    Integer v = functionConcatMap.get(Integer.valueOf(code));
    if (v != null) {
      int numParams = (v >> 16) & 0xffff;
      if (numParams == 0 || numParameters == 0 || numParams == numParameters) {
        int mask1 = v & 0xff;
        int mask2 = (v >> 8) & 0xff;
        int pos = 0;
        while (pos < position) {
          int ofs = ((mask1 & 1) != 0) ? 2 : 1;
          if (position < pos + ofs) {
            break;
          }
          pos += ofs;
          mask1 >>= 4;
          mask2 >>= 4;
        }
        retVal = (mask2 & 1) != 0;
      }
    }
    return retVal;
  }
}
