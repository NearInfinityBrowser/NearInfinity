// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
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
public class ScriptInfo
{
  private static final HashMap<Profile.Engine, ScriptInfo> mapInfo = new HashMap<>();
  static {
    // *** Profile.Engine.BG1 ***
    ScriptInfo si = new ScriptInfo(new String[]{"EA", "GENERAL", "RACE", "CLASS", "SPECIFIC", "GENDER", "ALIGN"},
                                   new String[]{"GLOBAL", "LOCALS", "MYAREA"});
    mapInfo.put(Profile.Engine.BG1, si);
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x0001, 'S', 0), "ITM");  // Acquired
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x0009, 'S', 0), "ITM");  // Unusable
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4061, 'S', 0), "ITM");  // HasItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4075, 'S', 0), "ITM");  // Contains
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4051, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // Dead
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4071, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // NumDead
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4072, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // NumDeadGT
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4073, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // NumDeadLT
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 161, 'S', 0), "2DA"); // IncrementChapter
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 199, 'S', 0), "2DA"); // TextList
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 170, 'S', 0), "ARE"); // RevealAreaOnMap
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 185, 'S', 0), "ARE"); // SetMasterArea
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 7, 'S', 0), "CRE");   // CreateCreature
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 194, 'S', 0), "CRE"); // ChangeAnimation
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 9, 'S', 0), "ITM");   // DropItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 11, 'S', 0), "ITM");  // EquipItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 14, 'S', 0), "ITM");  // GetItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 15, 'S', 0), "ITM");  // GiveItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 34, 'S', 0), "ITM");  // UseItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 82, 'S', 0), "ITM");  // CreateItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 140, 'S', 0), "ITM"); // GiveItemCreate
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 169, 'S', 0), "ITM"); // DestroyItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 167, 'S', 0), "MVE"); // StartMovie
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x400F), Integer.valueOf(0x0001)); // Global
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x4034), Integer.valueOf(0x0001)); // GlobalGT
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x4035), Integer.valueOf(0x0001)); // GlobalLT
    si.FUNCTION_CONCAT.put(Integer.valueOf(30), Integer.valueOf(0x0001)); // SetGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(109), Integer.valueOf(0x0001)); // IncrementGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(115), Integer.valueOf(0x0001)); // SetGlobalTimer
    si.FUNCTION_CONCAT.put(Integer.valueOf(141), Integer.valueOf(0x0001)); // GivePartyGoldGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(165), Integer.valueOf(0x0001)); // AddexperiencePartyGlobal
    si.FUNCTION_PARAM_COMMENT.put(Integer.valueOf(151), Integer.valueOf(1));  // DisplayString
    si.FUNCTION_PARAM_COMMENT.put(Integer.valueOf(197), Integer.valueOf(1));  // MoveGlobal

    // *** Profile.Engine.BG2 ***
    si = new ScriptInfo(mapInfo.get(Profile.Engine.BG1), null);
    mapInfo.put(Profile.Engine.BG2, si);
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x40C6, 'S', 0), "");  // G
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x40C7, 'S', 0), "");  // GGT
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x40C8, 'S', 0), "");  // GLT
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x407E, 'S', 0), "ARE");  // AreaCheck
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x40D4, 'S', 0), "ARE");  // AreaCheckObject
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4092, 'S', 0), "CRE");  // InLine
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4077, 'S', 0), "ITM");  // NumItems
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4078, 'S', 0), "ITM");  // NumItemsGT
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4079, 'S', 0), "ITM");  // NumItemsLT
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x407A, 'S', 0), "ITM");  // NumItemsParty
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x407B, 'S', 0), "ITM");  // NumItemsPartyGT
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x407C, 'S', 0), "ITM");  // NumItemsPartyLT
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x407F, 'S', 0), "ITM");  // HasItemEquiped
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x40A9, 'S', 0), "ITM");  // PartyHasItemIdentified
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x40C2, 'S', 0), "ITM");  // HasItemEquipedReal
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4104, 'S', 0), "ITM");  // CurrentAmmo
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x40A5, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // Name
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x40DF, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // BeenInParty
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 220, 'S', 0), "2DA"); // TakeItemListParty
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 226, 'S', 0), "2DA"); // TakeItemListPartyNum
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 251, 'S', 0), "ARE"); // HideAreaOnMap
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 264, 'S', 0), "ARE"); // CopyGroundPilesTo
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 277, 'S', 0), "ARE"); // EscapeAreaObjectMove
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 219, 'S', 0), "CRE"); // ChangeAnimationNoEffect
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 227, 'S', 0), "CRE"); // CreateCreatureObject
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 228, 'S', 0), "CRE"); // CreateCreatureImpassable
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 231, 'S', 0), "CRE"); // CreateCreatureDoor
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 232, 'S', 0), "CRE"); // CreateCreatureObjectDoor
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 233, 'S', 0), "CRE"); // CreateCreatureObjectOffScreen
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 238, 'S', 0), "CRE"); // CreateCreatureOffScreen
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 250, 'S', 0), "CRE"); // CreateCreatureObjectCopy
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 252, 'S', 0), "CRE"); // CreateCreatureObjectOffset
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 295, 'S', 0), "CRE"); // CreateCreatureCopyPoint
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 323, 'S', 0), "CRE"); // CreateCreatureImpassableAllowOverlap
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 246, 'S', 2), "CRE"); // CreateCreatureAtLocation
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 204, 'S', 0), "ITM"); // TakePartyItemNum
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 257, 'S', 0), "ITM"); // PickUpItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 256, 'S', 2), "ITM"); // CreateItemGlobal
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 279, 'S', 0), "SPL"); // AddSpecialAbility
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 272, 'S', 0), "VEF:VVC:BAM"); // CreateVisualEffect
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 273, 'S', 0), "VEF:VVC:BAM"); // CreateVisualEffectObject
    si.FUNCTION_CONCAT.put(Integer.valueOf(246), Integer.valueOf(0x0001)); // CreateCreatureAtLocation
    si.FUNCTION_CONCAT.put(Integer.valueOf(256), Integer.valueOf(0x0001)); // CreateItemGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(268), Integer.valueOf(0x0001)); // RealSetGlobalTimer
    si.FUNCTION_CONCAT.put(Integer.valueOf(297), Integer.valueOf(0x0001)); // MoveToSavedLocation
    si.FUNCTION_CONCAT.put(Integer.valueOf(335), Integer.valueOf(0x0001)); // SetTokenGlobal
    si.FUNCTION_PARAM_COMMENT.put(Integer.valueOf(246), Integer.valueOf(2));  // CreateCreatureAtLocation
    si.FUNCTION_PARAM_COMMENT.put(Integer.valueOf(256), Integer.valueOf(2));  // CreateItemGlobal
    si.FUNCTION_PARAM_COMMENT.put(Integer.valueOf(262), Integer.valueOf(1));  // DisplayStringNoName
    si.FUNCTION_PARAM_COMMENT.put(Integer.valueOf(269), Integer.valueOf(1));  // DisplayStringHead
    si.FUNCTION_PARAM_COMMENT.put(Integer.valueOf(292), Integer.valueOf(1));  // DisplayStringHeadOwner
    si.FUNCTION_PARAM_COMMENT.put(Integer.valueOf(311), Integer.valueOf(1));  // DisplayStringWait
    si.FUNCTION_PARAM_COMMENT.put(Integer.valueOf(342), Integer.valueOf(1));  // DisplayStringHeadDead
    si.FUNCTION_PARAM_COMMENT.put(Integer.valueOf(346), Integer.valueOf(1));  // DisplayStringNoNameHead
    si.addFunctionDefinition(Function.FunctionType.TRIGGER, "0x4100 TriggerOverride(O:Object*,T:Trigger*)");

    // *** Profile.Engine.EE ***
    si = new ScriptInfo(mapInfo.get(Profile.Engine.BG2), null);
    mapInfo.put(Profile.Engine.EE, si);
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 445, 'S', 0), "CRE"); // CreateCreatureAtFeet (PSTEE)
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 438, 'S', 1), "DLG"); // ChangeDialog (PSTEE)
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 431, 'S', 0), "ITM"); // DestroyPartyItem (PSTEE)
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 455, 'S', 0), "ITM"); // DestroyItemObject (PSTEE)
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 432, 'S', 0), "ITM"); // TransformPartyItem (PSTEE)
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 432, 'S', 1), "ITM"); // TransformPartyItem (PSTEE)
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 167, 'S', 0), "WBM:MVE"); // StartMovie
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x4109), Integer.valueOf(0x0001)); // StuffGlobalRandom (PSTEE)
    si.FUNCTION_CONCAT.put(Integer.valueOf(364), Integer.valueOf(0x0001)); // SetGlobalRandom
    si.FUNCTION_CONCAT.put(Integer.valueOf(377), Integer.valueOf(0x0001)); // SetGlobalTimerRandom
    si.FUNCTION_CONCAT.put(Integer.valueOf(446), Integer.valueOf(0x0011 | (5 << 16))); // IncrementGlobalOnce (PSTEE)
    si.FUNCTION_PARAM_COMMENT.put(Integer.valueOf(362), Integer.valueOf(1));  // RemoveStoreItem
    si.FUNCTION_PARAM_COMMENT.put(Integer.valueOf(363), Integer.valueOf(1));  // AddStoreItem
    si.FUNCTION_PARAM_COMMENT.put(Integer.valueOf(376), Integer.valueOf(1));  // DisplayStringNoNameDlg
    si.FUNCTION_PARAM_COMMENT.put(Integer.valueOf(388), Integer.valueOf(1));  // DisplayStringHeadNoLog
    si.removeFunctionDefinition(Function.FunctionType.TRIGGER, "0x4100 TriggerOverride(O:Object*,T:Trigger*)"); // leftover from BG2 profile
    si.addFunctionDefinition(Function.FunctionType.TRIGGER, "0x40e0 TriggerOverride(O:Object*,T:Trigger*)");

    // *** Profile.Engine.IWD ***
    si = new ScriptInfo(new String[]{"EA", "GENERAL", "RACE", "CLASS", "SPECIFIC", "GENDER", "ALIGN"},
                        new String[]{"GLOBAL", "LOCALS", "MYAREA"});
    mapInfo.put(Profile.Engine.IWD, si);
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x0001, 'S', 0), "ITM");  // Acquired
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x0009, 'S', 0), "ITM");  // Unusable
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4061, 'S', 0), "ITM");  // HasItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4051, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // Dead
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4071, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // NumDead
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4072, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // NumDeadGT
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4073, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // NumDeadLT
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 161, 'S', 0), "2DA"); // IncrementChapter
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 199, 'S', 0), "2DA"); // TextScreen
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 170, 'S', 0), "ARE"); // RevealAreaOnMap
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 185, 'S', 0), "ARE"); // SetMasterArea
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 7, 'S', 0), "CRE");   // CreateCreature
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 194, 'S', 0), "CRE"); // ChangeAnimation
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 9, 'S', 0), "ITM");   // DropItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 11, 'S', 0), "ITM");  // EquipItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 14, 'S', 0), "ITM");  // GetItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 15, 'S', 0), "ITM");  // GiveItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 34, 'S', 0), "ITM");  // UseItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 82, 'S', 0), "ITM");  // CreateItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 140, 'S', 0), "ITM"); // GiveItemCreate
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 169, 'S', 0), "ITM"); // DestroyItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 167, 'S', 0), "MVE"); // StartMovie
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x400F), Integer.valueOf(0x0001)); // Global
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x4034), Integer.valueOf(0x0001)); // GlobalGT
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x4035), Integer.valueOf(0x0001)); // GlobalLT
    si.FUNCTION_CONCAT.put(Integer.valueOf(30), Integer.valueOf(0x0001)); // SetGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(109), Integer.valueOf(0x0001)); // IncrementGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(115), Integer.valueOf(0x0001)); // SetGlobalTimer
    si.FUNCTION_CONCAT.put(Integer.valueOf(141), Integer.valueOf(0x0001)); // GivePartyGoldGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(165), Integer.valueOf(0x0001)); // AddexperiencePartyGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(243), Integer.valueOf(0x0011)); // IncrementGlobalOnce
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x40A5), Integer.valueOf(0x0101)); // BitGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(247), Integer.valueOf(0x0101)); // BitGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x40A6), Integer.valueOf(0x1111)); // GlobalBitGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(248), Integer.valueOf(0x1111)); // GlobalBitGlobal
    si.FUNCTION_PARAM_COMMENT.put(Integer.valueOf(151), Integer.valueOf(1));  // DisplayString
    si.FUNCTION_PARAM_COMMENT.put(Integer.valueOf(197), Integer.valueOf(1));  // MoveGlobal

    // *** Profile.Engine.IWD2 ***
    si = new ScriptInfo(new String[]{"EA", "GENERAL", "RACE", "CLASS", "SPECIFIC", "GENDER", "ALIGNMNT", "SUBRACE", "AVCLASS", "CLASSMSK"},
                        new String[]{"GLOBAL", "LOCALS", "MYAREA"});
    mapInfo.put(Profile.Engine.IWD2, si);
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x40A5, 'S', 1), "ARE");  // BitGlobal
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x40A6, 'S', 1), "ARE");  // GlobalBitGlobal
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x40A6, 'S', 3), "ARE");  // GlobalBitGlobal
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x0001, 'S', 0), "ITM");  // Acquired
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x0009, 'S', 0), "ITM");  // Unusable
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4061, 'S', 0), "ITM");  // HasItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4075, 'S', 0), "ITM");  // Contains
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4077, 'S', 0), "ITM");  // NumItems
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4078, 'S', 0), "ITM");  // NumItemsGT
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4079, 'S', 0), "ITM");  // NumItemsLT
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x407A, 'S', 0), "ITM");  // NumItemsParty
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x407B, 'S', 0), "ITM");  // NumItemsPartyGT
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x407C, 'S', 0), "ITM");  // NumItemsPartyLT
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x40C1, 'S', 0), "ITM");  // TotalItemCntExclude
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x40C2, 'S', 0), "ITM");  // TotalItemCntExcludeGT
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x40C3, 'S', 0), "ITM");  // TotalItemCntExcludeLT
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x40C5, 'S', 0), "ITM");  // ItemIsIdentified
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x40C8, 'S', 0), "SPL");  // HasInnateAbility
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4071, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // NumDead
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4072, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // NumDeadGT
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4073, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // NumDeadLT
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 161, 'S', 0), "2DA"); // IncrementChapter
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 199, 'S', 0), "2DA"); // TextScreen
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 170, 'S', 0), "ARE"); // RevealAreaOnMap
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 185, 'S', 0), "ARE"); // SetMasterArea
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 247, 'S', 1), "ARE"); // BitGlobal
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 243, 'S', 1), "ARE"); // IncrementGlobalOnce
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 248, 'S', 1), "ARE"); // GlobalBitGlobal
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 243, 'S', 3), "ARE"); // IncrementGlobalOnce
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 248, 'S', 3), "ARE"); // GlobalBitGlobal
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 7, 'S', 0), "CRE");   // CreateCreature
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 194, 'S', 0), "CRE"); // ChangeAnimation
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 278, 'S', 0), "ITM"); // DropInventoryEXExclude
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 9, 'S', 0), "ITM");   // DropItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 11, 'S', 0), "ITM");  // EquipItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 14, 'S', 0), "ITM");  // GetItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 15, 'S', 0), "ITM");  // GiveItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 34, 'S', 0), "ITM");  // UseItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 82, 'S', 0), "ITM");  // CreateItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 140, 'S', 0), "ITM"); // GiveItemCreate
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 169, 'S', 0), "ITM"); // DestroyItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 167, 'S', 0), "MVE"); // StartMovie
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 321, 'S', 0), Function.Parameter.RESTYPE_SPELL_LIST); // MarkSpellAndObject
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x400F), Integer.valueOf(0x0001)); // Global
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x4034), Integer.valueOf(0x0001)); // GlobalGT
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x4035), Integer.valueOf(0x0001)); // GlobalLT
    si.FUNCTION_CONCAT.put(Integer.valueOf(30), Integer.valueOf(0x0001)); // SetGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(109), Integer.valueOf(0x0001)); // IncrementGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(115), Integer.valueOf(0x0001)); // SetGlobalTimer
    si.FUNCTION_CONCAT.put(Integer.valueOf(141), Integer.valueOf(0x0001)); // GivePartyGoldGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(165), Integer.valueOf(0x0001)); // AddexperiencePartyGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(308), Integer.valueOf(0x0001)); // SetGlobalTimerOnce
    si.FUNCTION_CONCAT.put(Integer.valueOf(243), Integer.valueOf(0x0011)); // IncrementGlobalOnce
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x40A5), Integer.valueOf(0x0101)); // BitGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(247), Integer.valueOf(0x0101)); // BitGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(306), Integer.valueOf(0x0101)); // SetGlobalRandom
    si.FUNCTION_CONCAT.put(Integer.valueOf(307), Integer.valueOf(0x0101)); // SetGlobalTimerRandom
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x40A6), Integer.valueOf(0x1111)); // GlobalBitGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(289), Integer.valueOf(0x1010)); // SpellCastEffect
    si.FUNCTION_CONCAT.put(Integer.valueOf(248), Integer.valueOf(0x1111)); // GlobalBitGlobal
    si.FUNCTION_PARAM_COMMENT.put(Integer.valueOf(151), Integer.valueOf(1));  // DisplayString
    si.FUNCTION_PARAM_COMMENT.put(Integer.valueOf(197), Integer.valueOf(1));  // MoveGlobal

    // *** Profile.Engine.PST ***
    si = new ScriptInfo(new String[]{"EA", "FACTION", "TEAM", "GENERAL", "RACE", "CLASS", "SPECIFIC", "GENDER", "ALIGN"},
                        new String[]{"GLOBAL", "LOCALS", "MYAREA", "KAPUTZ"});
    mapInfo.put(Profile.Engine.PST, si);
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x0001, 'S', 0), "ITM");  // Acquired
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x0009, 'S', 0), "ITM");  // Unusable
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4061, 'S', 0), "ITM");  // HasItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.TRIGGER, 0x4051, 'S', 0), Function.Parameter.RESTYPE_SCRIPT);  // Dead
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 161, 'S', 0), "2DA"); // IncrementChapter
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 199, 'S', 0), "2DA"); // TextList
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 170, 'S', 0), "ARE"); // RevealAreaOnMap
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 185, 'S', 0), "ARE"); // SetMasterArea
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 7, 'S', 0), "CRE");   // CreateCreature
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 295, 'S', 0), "CRE"); // CreateCreatureAtFeet
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 272, 'S', 1), "DLG"); // ChangeDialog
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 9, 'S', 0), "ITM");   // DropItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 11, 'S', 0), "ITM");  // EquipItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 14, 'S', 0), "ITM");  // GetItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 15, 'S', 0), "ITM");  // GiveItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 34, 'S', 0), "ITM");  // UseItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 82, 'S', 0), "ITM");  // CreateItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 140, 'S', 0), "ITM"); // GiveItemCreate
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 169, 'S', 0), "ITM"); // DestroyItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 222, 'S', 0), "ITM"); // DestroyPartyItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 223, 'S', 0), "ITM"); // TransformItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 224, 'S', 0), "ITM"); // TransformPartyItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 307, 'S', 0), "ITM"); // TransformItemAll
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 308, 'S', 0), "ITM"); // TransformPartyItemAll
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 224, 'S', 1), "ITM"); // TransformPartyItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 308, 'S', 1), "ITM"); // TransformPartyItemAll
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 223, 'S', 2), "ITM"); // TransformItem
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 307, 'S', 2), "ITM"); // TransformItemAll
    si.FUNCTION_RESTYPE.put(key(Function.FunctionType.ACTION, 167, 'S', 0), "MVE"); // StartMovie
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x400F), Integer.valueOf(0x0001)); // Global
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x4034), Integer.valueOf(0x0001)); // GlobalGT
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x4035), Integer.valueOf(0x0001)); // GlobalLT
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x407F), Integer.valueOf(0x0001)); // BitCheck
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x4080), Integer.valueOf(0x0001)); // GlobalBAND
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x4081), Integer.valueOf(0x0001)); // BitCheckExact
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x4095), Integer.valueOf(0x0001)); // Xor
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x409C), Integer.valueOf(0x0001)); // StuffGlobalRandom
    si.FUNCTION_CONCAT.put(Integer.valueOf(30), Integer.valueOf(0x0001)); // SetGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(109), Integer.valueOf(0x0001)); // IncrementGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(115), Integer.valueOf(0x0001)); // SetGlobalTimer
    si.FUNCTION_CONCAT.put(Integer.valueOf(141), Integer.valueOf(0x0001)); // GivePartyGoldGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(165), Integer.valueOf(0x0001)); // AddexperiencePartyGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(227), Integer.valueOf(0x0001)); // GlobalBAND
    si.FUNCTION_CONCAT.put(Integer.valueOf(228), Integer.valueOf(0x0001)); // GlobalBOR
    si.FUNCTION_CONCAT.put(Integer.valueOf(229), Integer.valueOf(0x0001)); // GlobalSHR
    si.FUNCTION_CONCAT.put(Integer.valueOf(230), Integer.valueOf(0x0001)); // GlobalSHL
    si.FUNCTION_CONCAT.put(Integer.valueOf(231), Integer.valueOf(0x0001)); // GlobalMAX
    si.FUNCTION_CONCAT.put(Integer.valueOf(232), Integer.valueOf(0x0001)); // GlobalMIN
    si.FUNCTION_CONCAT.put(Integer.valueOf(244), Integer.valueOf(0x0001)); // BitSet
    si.FUNCTION_CONCAT.put(Integer.valueOf(245), Integer.valueOf(0x0001)); // BitClear
    si.FUNCTION_CONCAT.put(Integer.valueOf(260), Integer.valueOf(0x0001)); // GlobalXOR
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x4082), Integer.valueOf(0x0011)); // GlobalEqualsGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x4083), Integer.valueOf(0x0011)); // GlobalLTGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x4084), Integer.valueOf(0x0011)); // GlobalGTGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x4085), Integer.valueOf(0x0011)); // GlobalANDGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x4086), Integer.valueOf(0x0011)); // GlobalORGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x4087), Integer.valueOf(0x0011)); // GlobalBANDGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(0x4088), Integer.valueOf(0x0011)); // GlobalBANDGlobalExact
    si.FUNCTION_CONCAT.put(Integer.valueOf(202), Integer.valueOf(0x0011)); // IncrementGlobalOnce
    si.FUNCTION_CONCAT.put(Integer.valueOf(233), Integer.valueOf(0x0011)); // GlobalSetGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(234), Integer.valueOf(0x0011)); // GlobalAddGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(235), Integer.valueOf(0x0011)); // GlobalSubGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(236), Integer.valueOf(0x0011)); // GlobalANDGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(237), Integer.valueOf(0x0011)); // GlobalORGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(238), Integer.valueOf(0x0011)); // GlobalBANDGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(239), Integer.valueOf(0x0011)); // GlobalBORGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(240), Integer.valueOf(0x0011)); // GlobalSHRGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(241), Integer.valueOf(0x0011)); // GlobalSHLGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(242), Integer.valueOf(0x0011)); // GlobalMAXGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(243), Integer.valueOf(0x0011)); // GlobalMINGlobal
    si.FUNCTION_CONCAT.put(Integer.valueOf(261), Integer.valueOf(0x0011)); // GlobalXORGlobal
    si.FUNCTION_PARAM_COMMENT.put(Integer.valueOf(151), Integer.valueOf(1));  // DisplayString
    si.addFunctionDefinition(Function.FunctionType.TRIGGER, "0x4070 Clicked(O:Object*)");

    // *** Profile.Engine.Unknown ***
    si = new ScriptInfo(mapInfo.get(Profile.Engine.BG2), null);
    mapInfo.put(Profile.Engine.Unknown, si);
  }

  /**
   * Maps parameter information returned by the method {@code key(FunctionType ftype, int id, char type, int index)}
   * to one or more resource types.
   * @param Key A colon-separated list of function type, function id, parameter type (I, S, O or P)
   *            and parameter position.<br>
   *            <b>Example:</b> {@code "A:7:S:1"} points to the parameter {@code "S:Effect*"} of the
   *            action function {@code "7 CreateCreatureEffect(S:NewObject*,S:Effect*,P:Location*,I:Face*DIR)"}.
   * @param Value A colon-separated list of resource types (in upper case) or special keywords (in lower case)
   *              associated with the parameter.
   */
  public final Map<String, String> FUNCTION_RESTYPE = new HashMap<>(128);

  /**
   * Returns number and type of concatenated strings of the specified trigger or action
   * specified by code.
   * @param Key The trigger or action function code.
   * @param Value A numeric code:
   *              Bit 0: Whether first string parameter consists of two separate strings.<br>
   *              Bit 4: Whether second string parameter consists of two separate strings.<br>
   *              Bit 8: Whether first string parameter is separated by fixed-size string (0) or is colon-separated (1).<br>
   *              Bit 12: Whether second string parameter is separated by fixed-size string (0) or is colon-separated (1).<br>
   *              Bits 16..31: Optional number of expected parameter. This is useful to distinguish functions with
   *                           identical code, but different signatures.
   */
  public final Map<Integer, Integer> FUNCTION_CONCAT = new HashMap<>(64);

  /**
   * Lists of additional function signatures for triggers and actions that may not
   * be available from the respective IDS files.
   */
  public final Map<Function.FunctionType, List<String>> FUNCTION_SIGNATURES = new HashMap<>();

  /**
   * Defines which parameter of a function should generate comments.
   * Unlisted functions generate comments for the first commentable parameter only.
   * @param Key The trigger or action function code.
   * @param Value The parameter index.
   */
  public final Map<Integer, Integer> FUNCTION_PARAM_COMMENT = new HashMap<>(64);

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
  public static ScriptInfo getInfo()
  {
    return getInfo(Profile.getEngine());
  }

  /**
   * Returns the {@code ScriptInfo} object associated with the specified game engine.
   */
  public static ScriptInfo getInfo(Profile.Engine engine)
  {
    return mapInfo.get(engine);
  }

  /**
   * Internally used: Create a correctly formatted string for use as key for {@code FUNCTION_RESTYPE}.
   */
  private static String key(Function.FunctionType ftype, int id, char type, int index)
  {
    StringBuilder sb = new StringBuilder();
    sb.append(ftype == Function.FunctionType.TRIGGER ? 'T' : 'A').append(':')
      .append(id).append(':')
      .append(Character.toUpperCase(type)).append(':')
      .append(index);
    return sb.toString();
  }

  /**
   * Internally used: Registers a new trigger or action function definition.
   * @param type The function type (action or trigger).
   * @param def Function definition consisting of function id and signature.
   * @return Whether definition has been added successfully.
   */
  private boolean addFunctionDefinition(Function.FunctionType type, String def)
  {
    if (def != null) {
      List<String> list = FUNCTION_SIGNATURES.get(type);
      if (list != null) {
        list.add(def);
        return true;
      }
    }
    return false;
  }

  /**
   * Internally used: Removes the specified trigger or action function definition from the list.
   * @param type The function type (action or trigger).
   * @param def Exact function definition consisting of function id and signature.
   * @return Whether definition has been removed successfully.
   */
  private boolean removeFunctionDefinition(Function.FunctionType type, String def)
  {
    if (def != null) {
      List<String> list = FUNCTION_SIGNATURES.get(type);
      if (list != null) {
        return list.remove(def);
      }
    }
    return false;
  }

  protected ScriptInfo(String[] objectSpecifierIds, String[] scopes)
  {
    FUNCTION_SIGNATURES.put(Function.FunctionType.ACTION, new ArrayList<>());
    FUNCTION_SIGNATURES.put(Function.FunctionType.TRIGGER, new ArrayList<>());
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

  protected ScriptInfo(ScriptInfo obj, String[] objectSpecifierIds)
  {
    this((objectSpecifierIds != null) ? objectSpecifierIds : obj.OBJECT_SPECIFIER_IDS, obj.SCOPES);
    this.FUNCTION_RESTYPE.putAll(obj.FUNCTION_RESTYPE);
    this.FUNCTION_CONCAT.putAll(obj.FUNCTION_CONCAT);
    for (final Map.Entry<Function.FunctionType, List<String>> entry : obj.FUNCTION_SIGNATURES.entrySet()) {
      List<String> oldList = entry.getValue();
      if (oldList != null) {
        this.FUNCTION_SIGNATURES.put(entry.getKey(), new ArrayList<>(oldList));
      }
    }
    this.FUNCTION_PARAM_COMMENT.putAll(obj.FUNCTION_PARAM_COMMENT);
  }

  /** Returns object specifier IDS resource names. */
  public String[] getObjectIdsList()
  {
    return OBJECT_SPECIFIER_IDS;
  }

  /**
   * Determines the resource type associated with the specified function parameter.
   * @param ftype Trigger or Action?
   * @param id Numeric function identifier from IDS.
   * @param param Required for name, type and ids reference.
   * @param index Position of parameter in parameter list of the function.
   * @return A string with colon-separated resource types (in upper case) or
   *         special keywords (in lower case). An empty string otherwise.
   */
  public String getResType(Function.FunctionType ftype, int id, Function.Parameter param, int index)
  {
    String retVal = FUNCTION_RESTYPE.get(key(ftype, id, param.getType(), index));

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
        } else if (name.startsWith("item") || name.endsWith("item") ||
                   name.equals("give") || name.equals("take")) {
          retVal = "ITM";
        } else if (name.startsWith("dialogfile")) {
          retVal = "DLG";
        } else if (name.equals("cutscene") || name.equals("scriptfile") || name.equals("script")) {
          retVal = "BCS";
        } else if (name.startsWith("sound") || name.startsWith("voice")) {
          retVal = "WAV";
        } else if (name.startsWith("effect")) {
          retVal = "VEF:VVC:BAM";
//        } else if (name.equals("parchment")) {  // most likely incorrect
//          retVal = "MOS";
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
   * @param type The function type (action or trigger).
   * @return An unmodifyable list of function definitions.
   */
  public List<String> getFunctionDefinitions(Function.FunctionType type)
  {
    return Collections.unmodifiableList(FUNCTION_SIGNATURES.get(type));
  }

  /**
   * Returns whether the specified scope name (e.g. GLOBAL, LOCALS, MYAREA) is supported
   * by the current game.
   */
  public boolean isGlobalScope(String scope)
  {
    boolean retVal = false;
    if (scope != null && scope.length() == 6) {
      scope = scope.toUpperCase(Locale.ENGLISH);
      for (final String s: SCOPES) {
        if (s.equals(scope)) {
          retVal = true;
          break;
        }
      }
    }
    return retVal;
  }

  /** Returns all available scope names for the current game. */
  public String[] getGlobalScopes()
  {
    return SCOPES;
  }

  /** Returns whether the parameter at the specified index may generate comments. */
  public boolean isCommentAllowed(int code, int paramIndex)
  {
    if (FUNCTION_PARAM_COMMENT.containsKey(Integer.valueOf(code))) {
      return (paramIndex >= FUNCTION_PARAM_COMMENT.get(Integer.valueOf(code)).intValue());
    } else {
      return true;
    }
  }

  /**
   * Returns whether the string argument at the specified position has to be splitted
   * from a string parameter.
   * @param code Trigger or action function code.
   * @param position Relative string argument position (in range: [0, 3]).
   * @param numParameters Number of expected parameters for this function. Specify 0 to ignore.
   * @return {@code true} if argument is part of a combined string parameter, {@code false} otherwise.
   */
  public boolean isCombinedString(int code, int position, int numParameters)
  {
    boolean retVal = false;
    Integer v = FUNCTION_CONCAT.get(Integer.valueOf(code));
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
   * @param code Trigger of action function code.
   * @param position Relative string argument position (in range: [0, 3]).
   * @param numParameters Number of expected parameters for this function. Specify 0 to ignore.
   * @return {@code true} if argument is part of a colon-separated string parameter, {@code false} otherwise.
   */
  public boolean isColonSeparatedString(int code, int position, int numParameters)
  {
    boolean retVal = false;
    Integer v = FUNCTION_CONCAT.get(Integer.valueOf(code));
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
