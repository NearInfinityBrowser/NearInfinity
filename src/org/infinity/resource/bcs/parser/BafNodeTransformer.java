// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.bcs.parser;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import org.infinity.resource.ResourceFactory;
import org.infinity.resource.bcs.ScriptInfo;
import org.infinity.resource.bcs.ScriptMessage;
import org.infinity.resource.bcs.Signatures;
import org.infinity.util.IdsMapCache;

/**
 * Transforms the raw parse tree into a node tree that more closely resembles the resulting
 * BCS bytecode structure.
 */
public class BafNodeTransformer
{
  private final Signatures triggers;
  private final Signatures actions;
  private final Set<ScriptMessage> errors;
  private final Set<ScriptMessage> warnings;

  private boolean verbose;      // whether to increase number if warnings
  private boolean exactMatch;   // indicates whether to enforce exact function name matches
  private boolean octSupported; // indicates whether octal notation is supported

  public BafNodeTransformer(Set<ScriptMessage> errors, Set<ScriptMessage> warnings, boolean verbose)
  {
    this.triggers = Signatures.getTriggers();
    this.actions = Signatures.getActions();
    this.errors = (errors != null) ? errors : new TreeSet<>();
    this.warnings = (warnings != null) ? warnings : new TreeSet<>();
    this.verbose = verbose;
  }

  /**
   * Transforms the node tree from the initial parse process into a node tree that much more
   * resembles the resulting BCS bytecode structure.
   * @param root The root node. Supported types: JJTSC, JJTSEQ_TR and JJTSEQ_AC
   * @return Root node of the transformed ScriptNode tree.
   * @throws ParseException if nodes don't conform to a valid script state.
   */
  public ScriptNode transformParseNodes(BafNode root) throws ParseException
  {
    if (root == null) {
      throw new NullPointerException();
    }

    ScriptNode scriptRoot = null;
    if (root.getId() == BafParser.JJTSC) {
      scriptRoot = transformSC(root);
    } else if (root.getId() == BafParser.JJTSEQ_TR) {
      scriptRoot = transformSEQ_TR(root);
    } else if (root.getId() == BafParser.JJTSEQ_AC) {
      scriptRoot = transformSEQ_AC(root);
    } else {
      throw new ParseException("Unsupported script type");
    }

    return scriptRoot;
  }

  // Transforms full BAF script
  private ScriptNode transformSC(BafNode baf) throws ParseException
  {
    if (baf.getId() != BafParser.JJTSC) {
      errors.add(new ScriptMessage(getUnexpectedNodeMessage(baf), baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return null;
    }

    setExactMatch(false);
    setOctSupported(true);
    ScriptNode retVal = new ScriptNode(null, "SC", baf.jjtGetFirstToken());
    for (int i = 0, cnt = baf.jjtGetNumChildren(); i < cnt; i++) {
      transformCR(retVal, (BafNode)baf.jjtGetChild(i));
    }
    return retVal;
  }

  // Transforms sequence of triggers
  private ScriptNode transformSEQ_TR(BafNode baf) throws ParseException
  {
    if (baf.getId() != BafParser.JJTSEQ_TR) {
      errors.add(new ScriptMessage(getUnexpectedNodeMessage(baf), baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return null;
    }

    setExactMatch(false);
    setOctSupported(false);
    ScriptNode retVal = new ScriptNode(null, "SEQ_TR", baf.jjtGetFirstToken());
    for (int i = 0, cnt = baf.jjtGetNumChildren(); i < cnt; i++) {
      transformTR(retVal, (BafNode)baf.jjtGetChild(i));
    }
    return retVal;
  }

  // Transforms sequence of actions
  private ScriptNode transformSEQ_AC(BafNode baf) throws ParseException
  {
    if (baf.getId() != BafParser.JJTSEQ_AC) {
      errors.add(new ScriptMessage(getUnexpectedNodeMessage(baf), baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return null;
    }

    setExactMatch(false);
    setOctSupported(false);
    ScriptNode retVal = new ScriptNode(null, "SEQ_AC", baf.jjtGetFirstToken());
    for (int i = 0, cnt = baf.jjtGetNumChildren(); i < cnt; i++) {
      transformAC(retVal, (BafNode)baf.jjtGetChild(i));
    }
    return retVal;
  }

  private void transformCR(ScriptNode parent, BafNode baf) throws ParseException
  {
    if (baf.getId() != BafParser.JJTCR) {
      errors.add(new ScriptMessage(getUnexpectedNodeMessage(baf), baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return;
    }

    ScriptNode nodeCR = new ScriptNode(parent, "CR", baf.jjtGetFirstToken());
    parent.addChild(nodeCR);
    for (int i = 0, cnt = baf.jjtGetNumChildren(); i < cnt; i++) {
      BafNode child = (BafNode)baf.jjtGetChild(i);
      if (child.getId() == BafParser.JJTCO) {
        transformCO(nodeCR, child);
      } else if (child.getId() == BafParser.JJTRS) {
        transformRS(nodeCR, child);
      }
    }
  }

  private void transformCO(ScriptNode parent, BafNode baf) throws ParseException
  {
    if (baf.getId() != BafParser.JJTCO) {
      errors.add(new ScriptMessage(getUnexpectedNodeMessage(baf), baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return;
    }

    ScriptNode nodeCO = new ScriptNode(parent, "CO", baf.jjtGetFirstToken());
    parent.addChild(nodeCO);
    for (int i = 0, cnt = baf.jjtGetNumChildren(); i < cnt; i++) {
      BafNode child = (BafNode)baf.jjtGetChild(i);
      if (child.getId() == BafParser.JJTTR) {
        transformTR(nodeCO, child);
      }
    }
  }

  private void transformRS(ScriptNode parent, BafNode baf) throws ParseException
  {
    if (baf.getId() != BafParser.JJTRS) {
      errors.add(new ScriptMessage(getUnexpectedNodeMessage(baf), baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return;
    }

    ScriptNode nodeRS = new ScriptNode(parent, "RS", baf.jjtGetFirstToken());
    parent.addChild(nodeRS);
    for (int i = 0, cnt = baf.jjtGetNumChildren(); i < cnt; i++) {
      BafNode child = (BafNode)baf.jjtGetChild(i);
      if (child.getId() == BafParser.JJTRE) {
        transformRE(nodeRS, child);
      }
    }
  }

  private void transformRE(ScriptNode parent, BafNode baf) throws ParseException
  {
    if (baf.getId() != BafParser.JJTRE) {
      errors.add(new ScriptMessage(getUnexpectedNodeMessage(baf), baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return;
    }

    ScriptNode nodeRE = new ScriptNode(parent, "RE", baf.jjtGetFirstToken());
    parent.addChild(nodeRE);
    for (int i = 0, cnt = baf.jjtGetNumChildren(); i < cnt; i++) {
      BafNode child = (BafNode)baf.jjtGetChild(i);
      if (child.getId() == BafParser.JJTUINT) {
        // response probability
        long value = 100;
        try {
          value = parseNumber(child.getTokenString());
        } catch (Exception e) {
          warnings.add(new ScriptMessage("Invalid response probability: " + child.getTokenString() + " (using defaults)",
                                         child.jjtGetFirstToken(), child.jjtGetLastToken()));
        }
        nodeRE.code = value;
      } else if (child.getId() == BafParser.JJTAC) {
        transformAC(nodeRE, child);
      }
    }
  }

  private void transformTR(ScriptNode parent, BafNode baf) throws ParseException
  {
    if (baf.getId() != BafParser.JJTTR) {
      errors.add(new ScriptMessage(getUnexpectedNodeMessage(baf), baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return;
    }

    ScriptNode nodeTR = new ScriptNode(parent, "TR", baf.jjtGetFirstToken());
    parent.addChild(nodeTR);
    Signatures.Function func = null;
    for (int i = 0, cnt = baf.jjtGetNumChildren(); i < cnt; i++) {
      BafNode child = (BafNode)baf.jjtGetChild(i);
      if (child.getId() == BafParser.JJTNEG) {
        // trigger is negated
        nodeTR.negated = true;
      } else if (child.getId() == BafParser.JJTNAME) {
        // determine trigger signature
        Token token = child.jjtGetFirstToken();
        func = getTriggerSignatures().getFunction(token.toString(), isExactMatch());
        if (func == null) {
          errors.add(new ScriptMessage(getSymbolNotFoundMessage(token.toString(), "TRIGGER.IDS"),
                                       child.jjtGetFirstToken(), child.jjtGetLastToken()));
          return;
        } else if (verbose && !token.toString().equals(func.getName())) {
          warnings.add(new ScriptMessage("No exact match: " + token.toString() + " vs. " + func.getName(),
                                         child.jjtGetFirstToken(), child.jjtGetLastToken()));
        }
        nodeTR.code = func.getId();
        nodeTR.function = func;
      } else if (child.getId() == BafParser.JJTPARAM) {
        // process trigger parameters
        nodeTR.index++;
        BafNode paramChild = (BafNode)child.jjtGetChild(0);
        if (nodeTR.index < func.getNumParameters() &&
            func.getParameter(nodeTR.index).getType() == Signatures.Function.Parameter.TYPE_TRIGGER) {
          // special: nested trigger is added as child node to current trigger node
          transformTR(nodeTR, paramChild);
        } else {
          // processing regular parameters
          processPARAM(nodeTR, func, child);
        }
      }
    }
  }

  private void transformAC(ScriptNode parent, BafNode baf) throws ParseException
  {
    if (baf.getId() != BafParser.JJTAC) {
      errors.add(new ScriptMessage(getUnexpectedNodeMessage(baf), baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return;
    }

    ScriptNode nodeAC = new ScriptNode(parent, "AC", baf.jjtGetFirstToken());
    parent.addChild(nodeAC);
    Signatures.Function func = null;
    for (int i = 0, cnt = baf.jjtGetNumChildren(); i < cnt; i++) {
      BafNode child = (BafNode)baf.jjtGetChild(i);
      if (child.getId() == BafParser.JJTNAME) {
        // determine trigger signature
        Token token = child.jjtGetFirstToken();
        func = getActionSignatures().getFunction(token.toString(), isExactMatch());
        if (func == null) {
          errors.add(new ScriptMessage(getSymbolNotFoundMessage(token.toString(), "ACTION.IDS"),
                                       child.jjtGetFirstToken(), child.jjtGetLastToken()));
          return;
        } else if (verbose && !token.toString().equals(func.getName())) {
          warnings.add(new ScriptMessage("No exact match: " + token.toString() + " vs. " + func.getName(),
                                         child.jjtGetFirstToken(), child.jjtGetLastToken()));
        }
        nodeAC.code = func.getId();
        nodeAC.function = func;
      } else if (child.getId() == BafParser.JJTPARAM) {
        // process action parameters
        nodeAC.index++;
        BafNode paramChild = (BafNode)child.jjtGetChild(0);
        if (nodeAC.index < func.getNumParameters() &&
            func.getParameter(nodeAC.index).getType() == Signatures.Function.Parameter.TYPE_ACTION) {
          // special: nested action is added as child node to current action node
          transformAC(nodeAC, paramChild);
        } else {
          // processing regular parameters
          processPARAM(nodeAC, func, child);
        }
      }
    }
  }

  private void processPARAM(ScriptNode funcNode, Signatures.Function function, BafNode baf) throws ParseException
  {
    if (baf.getId() != BafParser.JJTPARAM) {
      errors.add(new ScriptMessage(getUnexpectedNodeMessage(baf), baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return;
    }

    if (funcNode.index < function.getNumParameters()) {
      BafNode child = (BafNode)baf.jjtGetChild(0);
      Signatures.Function.Parameter param = function.getParameter(funcNode.index);
      if (param.getType() == Signatures.Function.Parameter.TYPE_STRING) {
        // String parameter expected
        processParamString(funcNode, param, child);
      } else if (param.getType() == Signatures.Function.Parameter.TYPE_INTEGER) {
        // Numeric or symbolic parameter expected
        processParamNumeric(funcNode, param, child);
      } else if (param.getType() == Signatures.Function.Parameter.TYPE_POINT) {
        // Point parameter expected
        processParamPoint(funcNode, param, child);
      } else if (param.getType() == Signatures.Function.Parameter.TYPE_OBJECT) {
        // Object parameter expected
        BafNode rect = getObjectRegion((child.getId() == BafParser.JJTOBJECT) ? baf : child);
        processParamObject(funcNode, param, child, rect);
      } else if (param.getType() == Signatures.Function.Parameter.TYPE_TRIGGER) {
        errors.add(new ScriptMessage("Too many nested triggers", baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
        return;
      } else if (param.getType() == Signatures.Function.Parameter.TYPE_ACTION) {
        errors.add(new ScriptMessage("Too many nested actions", baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
        return;
      }
    } else {
      warnings.add(new ScriptMessage("Too many arguments for [" + function.getName() + "] (allowed: " + function.getNumParameters() + ")",
                                     baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
    }
  }

  private void processParamString(ScriptNode funcNode, Signatures.Function.Parameter param, BafNode baf)
  {
    if (baf.getId() != BafParser.JJTPARAM_STR) {
      errors.add(new ScriptMessage("Invalid string: " + baf.jjtGetFirstToken(), baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return;
    }

    String s = processString((BafNode)baf.jjtGetChild(0));
    funcNode.strings.push(s);
  }

  private String processString(BafNode baf)
  {
    if (baf.getId() != BafParser.JJTSTRING) {
      errors.add(new ScriptMessage("Invalid string parameter: " + baf.jjtGetFirstToken(),
                                   baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return "";
    }

    String s = unquoteString(baf.getTokenString());
    if (s == null) {
      errors.add(new ScriptMessage("Invalid string parameter: " + baf.jjtGetFirstToken(),
                                   baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return "";
    }

    if (s.indexOf('"') >= 0) {
      errors.add(new ScriptMessage("Double quote character not allowed inside string parameter: " + s,
                                   baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return "";
    }

    if (verbose) {
      // significant number of characters is limited to 32
      if (s.length() > 32) {
        int excess = s.length() - 32;
        warnings.add(new ScriptMessage("String is too long by " + excess + " character(s): " + s,
                                       baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      }
      // special (umlaut) characters may cause issues
      int count = 0;
      for (int i = 0, len = s.length(); i < len; i++) {
        if (s.charAt(i) > 0x7f) {
          count++;
        }
      }
      if (count > 0) {
        warnings.add(new ScriptMessage("String contains " + count + " non-ascii character(s): " + s,
                                       baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      }
    }

    return s;
  }

  private void processParamNumeric(ScriptNode funcNode, Signatures.Function.Parameter param, BafNode baf)
  {
    if (baf.getId() != BafParser.JJTPARAM_NUM &&
        baf.getId() != BafParser.JJTPARAM_SYM &&
        baf.getId() != BafParser.JJTPARAM_STR) {
      errors.add(new ScriptMessage("Invalid number of symbol: " + baf.jjtGetFirstToken(),
                                   baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return;
    }

    BafNode child = (BafNode)baf.jjtGetChild(0);
    long value = processNumberExpression(param.getIdsRef(), child);

    // handling optional expressions
    for (int i = 1, cnt = baf.jjtGetNumChildren(); i < cnt; i++) {
      child = (BafNode)baf.jjtGetChild(i);
      if (child.getId() == BafParser.JJTOR_EXPR) {
        value |= processOrExpression(param, child);
      }
    }

    if (verbose && (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE)) {
      warnings.add(new ScriptMessage("Numeric value is out of range: " + param.toString(true) + " - " + value,
                                     baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
    }

    funcNode.numbers.push(Long.valueOf(value));
  }

  private long processNumberExpression(String ids, BafNode baf)
  {
    if (baf.getId() != BafParser.JJTSINT &&
        baf.getId() != BafParser.JJTUINT &&
        baf.getId() != BafParser.JJTNAME &&
        baf.getId() != BafParser.JJTSTRING) {
      errors.add(new ScriptMessage("Invalid numeric or symbolic value: " + baf.jjtGetFirstToken(),
                                   baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return 0;
    }

    String svalue = baf.getTokenString();
    int id = baf.getId();

    // string may be number or symbol
    if (baf.getId() == BafParser.JJTSTRING) {
      svalue = unquoteString(svalue);
      try {
        // check number first
        parseNumber(svalue);
        id = BafParser.JJTSINT;
      } catch (Exception e) {
        // may be symbol instead
        id = BafParser.JJTNAME;
      }
    }

    if (svalue == null) {
      errors.add(new ScriptMessage("Invalid numeric or symbolic value: " + baf.jjtGetFirstToken(),
                                   baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return 0;
    }

    long value = 0;
    if (id == BafParser.JJTSINT || id == BafParser.JJTUINT) {
      try {
        value = parseNumber(svalue);
      } catch (Exception e) {
        errors.add(new ScriptMessage("Invalid number: " + baf.jjtGetFirstToken(),
                                     baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
        return 0;
      }
    } else if (id == BafParser.JJTNAME) {
      String idsRef = ids.toUpperCase(Locale.ENGLISH);
      if (!idsRef.isEmpty() && idsRef.lastIndexOf('.') < 0) {
        idsRef += ".IDS";
      }

      String symbol = getNormalizedSymbol(svalue);
      boolean exact = isExactMatch() && IdsMapCache.isCaseSensitiveMatch(idsRef);
      Long number = IdsMapCache.getIdsValue(idsRef, symbol, exact, null);
      if (number != null) {
        value = (int)number.longValue();  // treat as signed 32-bit integer
      } else {
        if (idsRef.isEmpty()) {
          errors.add(new ScriptMessage("Cannot resolve symbol [" + symbol + ']',
                                       baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
          return 0;
        } else if (ResourceFactory.resourceExists(idsRef)) {
          errors.add(new ScriptMessage(getSymbolNotFoundMessage(symbol, idsRef),
                                       baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
          return 0;
        } else {
          errors.add(new ScriptMessage("Resource not found: " + idsRef,
                                       baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
          return 0;
        }
      }
    }

    return value;
  }

  // Processes numeric or symbolic OR expressions
  private long processOrExpression(Signatures.Function.Parameter param, BafNode baf)
  {
    if (baf.getId() != BafParser.JJTOR_EXPR) {
      errors.add(new ScriptMessage("Invalid numeric or symbolic expression: " + baf.jjtGetFirstToken(),
                                   baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return 0;
    }

    BafNode child = (BafNode)baf.jjtGetChild(0);
    if (child.getId() == BafParser.JJTNUMERIC) {
      return processNumberExpression(param.getIdsRef(), (BafNode)child.jjtGetChild(0));
    } else {
      errors.add(new ScriptMessage("Invalid numeric or symbolic expression: " + child.jjtGetFirstToken(),
                                   child.jjtGetFirstToken(), child.jjtGetLastToken()));
      return 0;
    }
  }

  private void processParamPoint(ScriptNode funcNode, Signatures.Function.Parameter param, BafNode baf)
  {
    if (baf.getId() != BafParser.JJTPARAM_TGT) {
      errors.add(new ScriptMessage("Invalid point: " + baf.jjtGetFirstToken(),
                                   baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return;
    }

    BafNode child = (BafNode)baf.jjtGetChild(0);
    if (child.getId() == BafParser.JJTTARGET && child.jjtGetNumChildren() == 2) {
      BafNode childNumeric = (BafNode)child.jjtGetChild(0);
      funcNode.point.x = (int)processNumberExpression(param.getIdsRef(), (BafNode)childNumeric.jjtGetChild(0));
      childNumeric = (BafNode)child.jjtGetChild(1);
      funcNode.point.y = (int)processNumberExpression(param.getIdsRef(), (BafNode)childNumeric.jjtGetChild(0));
    } else {
      errors.add(new ScriptMessage("Invalid point value: " + child.jjtGetFirstToken(),
                                   child.jjtGetFirstToken(), child.jjtGetLastToken()));
      return;
    }
  }

  private void processParamObject(ScriptNode funcNode, Signatures.Function.Parameter param, BafNode baf, BafNode rect) throws ParseException
  {
    if (baf.getId() != BafParser.JJTTR &&
        baf.getId() != BafParser.JJTAC &&
        baf.getId() != BafParser.JJTOBJECT &&
        baf.getId() != BafParser.JJTPARAM_STR &&
        baf.getId() != BafParser.JJTPARAM_SYM &&
        baf.getId() != BafParser.JJTPARAM_TGT) {
      errors.add(new ScriptMessage("Invalid object: " + baf.jjtGetFirstToken(),
                                   baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return;
    }

    ScriptNode nodeOB = new ScriptNode(funcNode, "OB", baf.jjtGetFirstToken());
    nodeOB.parameter = param;
    funcNode.addChild(nodeOB);

    processObject(nodeOB, baf);

    if (rect != null) {
      // processing optional region
      processRectangle(nodeOB, rect);
    }
  }

  private void processObject(ScriptNode objNode, BafNode baf) throws ParseException
  {
    if (baf.getId() != BafParser.JJTTR &&
        baf.getId() != BafParser.JJTAC &&
        baf.getId() != BafParser.JJTOBJECT &&
        baf.getId() != BafParser.JJTPARAM_STR &&
        baf.getId() != BafParser.JJTPARAM_SYM &&
        baf.getId() != BafParser.JJTPARAM_TGT) {
      errors.add(new ScriptMessage("Invalid object: " + baf.jjtGetFirstToken(),
                                   baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return;
    }

    BafNode child = (BafNode)baf.jjtGetChild(0);
    if (baf.getId() == BafParser.JJTPARAM_SYM) {
      // single target identifier
      String symbol = child.getTokenString();
      Long value = IdsMapCache.getIdsValue("OBJECT.IDS", symbol, null);
      if (value != null) {
        objNode.numbers2.push(value);
      } else {
        errors.add(new ScriptMessage(getSymbolNotFoundMessage(symbol, "OBJECT.IDS"),
                                     child.jjtGetFirstToken(), child.jjtGetLastToken()));
        return;
      }
    } else if (baf.getId() == BafParser.JJTPARAM_STR) {
      // script name
      String s = processString(child);
      objNode.strings.push(s);
    } else if (baf.getId() == BafParser.JJTPARAM_TGT) {
      // IDS target
      processIdsTarget(objNode, child);
    } else if (baf.getId() == BafParser.JJTOBJECT) {
      // target identifiers with arguments
      processObjectIdentifier(objNode, baf);
    } else {
      // target identifiers with arguments incorrectly detected as triggers or actions
      processObjectIdentifierTRAC(objNode, baf);
    }
  }

  private void processIdsTarget(ScriptNode objNode, BafNode baf)
  {
    if (baf.getId() != BafParser.JJTTARGET) {
      errors.add(new ScriptMessage("Invalid IDS target: " + baf.jjtGetFirstToken(),
                                   baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return;
    }

    String[] targetIds = ScriptInfo.getInfo().getObjectIdsList();
    for (int i = 0, cnt = Math.min(targetIds.length, baf.jjtGetNumChildren()); i < cnt; i++) {
      BafNode child = (BafNode)baf.jjtGetChild(i);
      if (child.getId() == BafParser.JJTNUMERIC) {
        long value = processNumberExpression(targetIds[i], (BafNode)child.jjtGetChild(0));
        boolean special = "SUBRACE".equalsIgnoreCase(targetIds[i]); // exclude SUBRACE from range check

        if (verbose && !special && (value > 255 || value < 0)) {
          warnings.add(new ScriptMessage("IDS target value is out of range: " + value,
                                         baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
        }

        objNode.numbers.push(value);
      } else {
        errors.add(new ScriptMessage("Invalid numeric value: " + child.jjtGetFirstToken(),
                                     child.jjtGetFirstToken(), child.jjtGetLastToken()));
        return;
      }
    }
    if (baf.jjtGetNumChildren() > targetIds.length) {
      // Too many IDS targets defined
      warnings.add(new ScriptMessage("Too many IDS targets (allowed: " + targetIds.length + ")",
                                     baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
    }
  }

  private void processObjectIdentifier(ScriptNode objNode, BafNode baf) throws ParseException
  {
    if (baf.getId() != BafParser.JJTOBJECT) {
      errors.add(new ScriptMessage("Invalid object identifier: " + baf.jjtGetFirstToken(),
                                   baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return;
    }

    for (int i = 0, cnt = baf.jjtGetNumChildren(); i < cnt; i++) {
      BafNode child = (BafNode)baf.jjtGetChild(i);
      if (child.getId() == BafParser.JJTNAME) {
        processObjectName(objNode, child);
      } else if (child.getId() == BafParser.JJTOBJECT) {
        processObjectIdentifier(objNode, child);
      } else if (child.getId() == BafParser.JJTSTRING) {
        String s = processString(child);
        objNode.strings.push(s);
      } else if (child.getId() == BafParser.JJTTARGET) {
        processIdsTarget(objNode, child);
      } else if (child.getId() == BafParser.JJTSYMBOL) {
        long value = processNumberExpression("OBJECT", (BafNode)child.jjtGetChild(0));
        objNode.numbers2.push(Long.valueOf(value));
      }
    }
  }

  private void processObjectIdentifierTRAC(ScriptNode objNode, BafNode baf) throws ParseException
  {
    if (baf.getId() != BafParser.JJTTR &&
        baf.getId() != BafParser.JJTAC) {
      errors.add(new ScriptMessage("Invalid object identifier: " + baf.jjtGetFirstToken(),
                                   baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return;
    }

    for (int i = 0, cnt = baf.jjtGetNumChildren(); i < cnt; i++) {
      BafNode child = (BafNode)baf.jjtGetChild(i);
      if (child.getId() == BafParser.JJTNAME) {
        processObjectName(objNode, child);
      } else if (child.jjtGetNumChildren() > 0) {
        child = (BafNode)child.jjtGetChild(0);
        processObject(objNode, child);
      }
    }
  }

  private void processObjectName(ScriptNode objNode, BafNode baf)
  {
    if (baf.getId() != BafParser.JJTNAME) {
      errors.add(new ScriptMessage("Invalid object name: " + baf.jjtGetFirstToken(),
                                   baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return;
    }

    String symbol = baf.getTokenString();
    Long value = IdsMapCache.getIdsValue("OBJECT.IDS", symbol, null);
    if (value != null) {
      objNode.numbers2.push(value);
    } else {
      errors.add(new ScriptMessage(getSymbolNotFoundMessage(symbol, "OBJECT.IDS"),
                                   baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return;
    }
  }

  private void processRectangle(ScriptNode objNode, BafNode baf)
  {
    if (baf.getId() != BafParser.JJTRECT) {
      errors.add(new ScriptMessage("Invalid region: " + baf.jjtGetFirstToken(),
                                   baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
      return;
    }

    for (int i = 0, cnt = baf.jjtGetNumChildren(); i < cnt; i++) {
      BafNode child  = (BafNode)baf.jjtGetChild(i);
      if (child.getId() == BafParser.JJTSINT) {
        int value = 0;
        try {
          value = (int)parseNumber(child.getTokenString());
        } catch (Exception e) {
          warnings.add(new ScriptMessage("Invalid region value: " + child.getTokenString() + " (using defaults)",
                                         child.jjtGetFirstToken(), child.jjtGetLastToken()));
        }
        switch (i) {
          case 0: objNode.region.x = value; break;
          case 1: objNode.region.y = value; break;
          case 2: objNode.region.width = value; break;
          case 3: objNode.region.height = value; break;
        }
      } else {
        errors.add(new ScriptMessage("Invalid numeric value: " + baf.jjtGetFirstToken(),
                                     baf.jjtGetFirstToken(), baf.jjtGetLastToken()));
        return;
      }
    }
  }

  private BafNode getObjectRegion(BafNode parent)
  {
    BafNode retVal = null;
    if (parent != null) {
      for (int i = 0, cnt = parent.jjtGetNumChildren(); i < cnt; i++) {
        BafNode child = (BafNode)parent.jjtGetChild(i);
        if (child.getId() == BafParser.JJTRECT) {
          retVal = child;
          break;
        }
      }
    }
    return retVal;
  }

  // Attempts to convert the specified string into a usable numeric value (dec/hex/oct supported)
  private long parseNumber(String number) throws Exception
  {
    if (number != null && number.length() > 0) {
      int signPos = "-+".indexOf(number.charAt(0));
      if (signPos >= 0) {
        number = number.substring(1);
      }

      int radix = 10;
      if (number.length() > 2 && number.substring(0, 2).equalsIgnoreCase("0x")) {
        radix = 16;
        number = number.substring(2);
      } else if (number.length() > 2 && number.substring(0, 2).equalsIgnoreCase("0b")) {
        radix = 2;
        number = number.substring(2);
      } else if (isOctSupported() && number.charAt(0) == '0') {
        radix = 8;
      }

      if (signPos == 0) {
        number = '-' + number;
      }

      return Long.parseLong(number, radix);
    } else {
      throw new Exception();
    }
  }

  // Handles special symbol expressions
  private String getNormalizedSymbol(String symbol)
  {
    if (symbol != null) {
      if (symbol.length() >= 10 && symbol.startsWith("\"\"\"\"\"") && symbol.endsWith("\"\"\"\"\"")) {
        symbol = symbol.substring(5, symbol.length() - 5);
      }
    }
    return symbol;
  }

  private String unquoteString(String s)
  {
    String retVal = null;

    if (s != null) {
      if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
        retVal = s.substring(1, s.length() - 1);
      } else if (s.length() >= 10 && s.startsWith("~~~~~") && s.endsWith("~~~~~")) {
        retVal = s.substring(5, s.length() - 5);
      } else if (s.length() >= 2 && s.charAt(0) == '~' && s.charAt(s.length() - 1) == '~') {
        retVal = s.substring(1, s.length() - 1);
      } else if (s.length() >= 2 && s.charAt(0) == '%' && s.charAt(s.length() - 1) == '%') {
        retVal = s.substring(1, s.length() - 1);
      } else if (s.length() >= 2 && s.charAt(0) == '#' && s.charAt(s.length() - 1) == '#') {
        retVal = s.substring(1, s.length() - 1);
      }
    }

    return retVal;
  }

  private String getUnexpectedNodeMessage(BafNode baf)
  {
    return "Unexpected node type: " + BafParser.getNodeName(baf.getId());
  }

  private String getSymbolNotFoundMessage(String symbol, String idsFile)
  {
    return '[' + symbol + "] not found in " + idsFile.toUpperCase(Locale.ENGLISH);
  }

  private Signatures getTriggerSignatures() { return this.triggers; }

  private Signatures getActionSignatures() { return this.actions; }

  private boolean isExactMatch() { return exactMatch; }

  private void setExactMatch(boolean set) { exactMatch = set; }

  private boolean isOctSupported() { return octSupported; }

  private void setOctSupported(boolean set) { octSupported = set; }

//-------------------------- INNER CLASSES --------------------------

  public static class ScriptNode
  {
    // default state for undefined numeric values
    public static final int NONE = -1;

    public final List<ScriptNode> children = new ArrayList<>();

    // stores numeric arguments for triggers and actions, target values for objects
    public final Stack<Long> numbers = new Stack<>();
    // stores identifiers for objects
    public final Stack<Long> numbers2 = new Stack<>();
    // stores string arguments for triggers and actions, script name for objects
    public final Stack<String> strings = new Stack<>();
    // stores point arguments for triggers and actions
    public final Point point = new Point(0, 0);
    // stores region prefix for objects
    public final Rectangle region = new Rectangle(-1, -1, -1, -1);
    // the node type (SC, CO, AC, TR, ...)
    public final String type;
    // First available token associated with this node (may be null)
    public final Token token;

    // parent of this node
    public ScriptNode parent;
    // numeric value trigger/action functions, probability for response block
    public long code;
    // a general-purpose index that can be used for tracking, set to -1 initially
    public int index;
    // negation state of triggers
    public boolean negated;
    // Function info for triggers and actions
    public Signatures.Function function;
    // Parameter info for objects
    public Signatures.Function.Parameter parameter;

    public ScriptNode(ScriptNode parent, String type, Token token)
    {
      this.parent = parent;
      this.type = type;
      this.token = token;
      this.code = NONE;
      this.index = NONE;
    }

    public ScriptNode getParent() { return parent; }

    public ScriptNode setParent(ScriptNode newParent)
    {
      ScriptNode retVal = this.parent;
      this.parent = newParent;
      return retVal;
    }

    public int getNumChildren() { return children.size(); }

    public ScriptNode getChild(int index) throws IndexOutOfBoundsException { return children.get(index); }

    public Iterator<ScriptNode> getChildren() { return children.iterator(); }

    public int addChild(ScriptNode child) { return addChild(child, getNumChildren()); }

    public int addChild(ScriptNode child, int index)
    {
      index = Math.max(0, Math.min(getNumChildren(), index));
      children.add(index, child);
      return index;
    }

    public String toString(String prefix)
    {
      String s = prefix + "Type: " + type;
      if (code != NONE) {
        s += ", code: " + code;
      }
      if (index != NONE) {
        s += ", params: " + (index + 1);
      }
      if ("TR".equals(type)) {
        s += ", negated: " + negated;
      }
      return s;
    }

    @Override
    public String toString()
    {
      return toString("");
    }

    public void dump(PrintStream output, String prefix)
    {
      dump(output, prefix, 0);
    }

    public void dump(PrintStream output, String prefix, int level)
    {
      String prefix2 = "";
      for (int i = 0; i < level; i++) { prefix2 += prefix; }
      System.out.println(toString(prefix2));
      Iterator<ScriptNode> iter = getChildren();
      while (iter.hasNext()) {
        iter.next().dump(output, prefix, level + 1);
      }
    }
  }
}
