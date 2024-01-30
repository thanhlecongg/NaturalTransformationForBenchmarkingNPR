package transform;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.Document;
import transform.rules.*;

import java.util.ArrayList;

public class RuleSelector {
    static final int RenameVariable = 1;
    static final int For2While = 2;
    static final int While2For = 3;
    static final int Do2While = 4;
    static final int IfElseIf2IfElse = 5;
    static final int IfElse2IfElseIf = 6;
    static final int Switch2If = 7;
    static final int Unary2Add = 8;
    static final int Add2Equal = 9;
    static final int DivideVarDecl = 10;
    static final int MergeVarDecl = 11;
    static final int SwapStatement = 12;

    static final int ModifyConstant = 13;

    static final int ReverseIf = 14;
    static final int If2CondExp = 15;
    static final int ConfExp2If = 16;
    static final int InfixDividing = 17;
    static final int DividePrePostFix = 18;
    static final int DividingComposedIf = 19;
    static final int LoopIfContinue2Else = 20;
    static final int SwitchEqualExp = 21;
    static final int SwitchStringEqual = 22;
    static final int SwitchRelation = 23;

    static ASTVisitor create(String ruleIdStr, CompilationUnit cu_, Document document_, String outputDirPath_, ArrayList targetLines) {
        int ruleID = Integer.parseInt(ruleIdStr);
        switch (ruleID) {
            //Rename variables and methods
            case RenameVariable:
                return new RenameVariable(cu_, document_, outputDirPath_, targetLines);
            //Transform for-loop to while-loop
            case For2While:
                return new For2While(cu_, document_, outputDirPath_, targetLines);
            //Transform while-loop to for-loop
            case While2For:
                return new While2For(cu_, document_, outputDirPath_, targetLines);
            case Do2While:
                return new Do2While(cu_, document_, outputDirPath_, targetLines);
            case IfElseIf2IfElse:
                return new IfElseIf2IfElse(cu_, document_, outputDirPath_, targetLines);
            case IfElse2IfElseIf:
                return new IfElse2IfElseIf(cu_, document_, outputDirPath_, targetLines);
            //Transform switch-case to if-elseif-else
            case Switch2If:
                return new Switch2If(cu_, document_, outputDirPath_, targetLines);
            //Transform unary statements (e.g., i++) to add assignments;
            case Unary2Add:
                return new Unary2Add(cu_, document_, outputDirPath_, targetLines);
            //Transform add assignments (e.g., a += b) to equal assignments (a = a + b);
            case Add2Equal:
                return new Add2Equal(cu_, document_, outputDirPath_, targetLines);
            //Divide variable declaration into two statements
            case DivideVarDecl:
                return new DivideVarDecl(cu_, document_, outputDirPath_, targetLines);
            case MergeVarDecl:
                return new MergeVarDecl(cu_, document_, outputDirPath_, targetLines);
            case SwapStatement:
                return new SwapStatement(cu_, document_, outputDirPath_, targetLines);
            case ModifyConstant:
                return new ModifyConstant(cu_, document_, outputDirPath_, targetLines);
            case ReverseIf:
                return new ReverseIf(cu_, document_, outputDirPath_, targetLines);
            case If2CondExp:
                return new If2CondExp(cu_, document_, outputDirPath_, targetLines);
            case ConfExp2If:
                return new CondExp2If(cu_, document_, outputDirPath_, targetLines);
            case InfixDividing:
                return new DivideInfix(cu_, document_, outputDirPath_, targetLines);
            case DividePrePostFix:
                return new PrePostFixExpDividing(cu_, document_, outputDirPath_, targetLines);
            case DividingComposedIf:
                return new DivideIf(cu_, document_, outputDirPath_, targetLines);
            case LoopIfContinue2Else:
                return new LoopIfContinue2Else(cu_, document_, outputDirPath_, targetLines);
            case SwitchEqualExp:
                return new SwitchEqualExp(cu_, document_, outputDirPath_, targetLines);
            case SwitchStringEqual:
                return new SwitchStringEqual(cu_, document_, outputDirPath_, targetLines);
            case SwitchRelation:
                return new SwitchRelation(cu_, document_, outputDirPath_, targetLines);
            default:
                System.out.println("ERROR:" + "No rule belongs to this id!");
                System.exit(5);
                return null;
        }
    }
}
