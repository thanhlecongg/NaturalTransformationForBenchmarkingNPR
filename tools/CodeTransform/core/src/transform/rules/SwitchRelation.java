package transform.rules;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import transform.Utils;

import java.util.*;

public class SwitchRelation extends ASTVisitor {
    List<InfixExpression> relationStatementBin = new ArrayList<InfixExpression>();
    CompilationUnit cu;
    Document document;
    String outputDirPath;
    ArrayList targetLines;
    public SwitchRelation(CompilationUnit cu_, Document document_, String outputDirPath_, ArrayList targetLines) {
        this.cu = cu_;
        this.document = document_;
        this.outputDirPath = outputDirPath_;
        this.targetLines = targetLines;
    }

    public boolean visit(InfixExpression node) {
        if(node.getOperator().equals(InfixExpression.Operator.LESS)
                || node.getOperator().equals(InfixExpression.Operator.GREATER)
                || node.getOperator().equals(InfixExpression.Operator.LESS_EQUALS)
                || node.getOperator().equals(InfixExpression.Operator.GREATER_EQUALS)
                || node.getOperator().equals(InfixExpression.Operator.NOT_EQUALS)) {
            if(Utils.checkTargetLines(this.targetLines, this.cu, node)){
                relationStatementBin.add(node);
            }
        }
        return true;
    }


    public InfixExpression.Operator getOppositeOperator(InfixExpression.Operator op){
        if (op.equals(InfixExpression.Operator.GREATER)) {
            return InfixExpression.Operator.LESS;
        }
        if (op.equals(InfixExpression.Operator.LESS)) {
            return InfixExpression.Operator.GREATER;
        }
        if (op.equals(InfixExpression.Operator.LESS_EQUALS)) {
            return InfixExpression.Operator.GREATER_EQUALS;
        }
        if (op.equals(InfixExpression.Operator.GREATER_EQUALS)) {
            return InfixExpression.Operator.LESS_EQUALS;
        }
        if (op.equals(InfixExpression.Operator.NOT_EQUALS)) {
            return InfixExpression.Operator.NOT_EQUALS;
        }
        return null;

    }

    public void endVisit(CompilationUnit node) {
        AST ast = cu.getAST();
        ASTRewrite rewriter = ASTRewrite.create(ast);

        if (relationStatementBin.size() == 0) {
            return;
        }
        for(InfixExpression equ : relationStatementBin) {
            //Copy
            InfixExpression copyedOne = (InfixExpression) ASTNode.copySubtree(ast, equ);
            Expression le = (Expression) ASTNode.copySubtree(ast,equ.getLeftOperand());
            copyedOne.setRightOperand(le);
            Expression re = (Expression) ASTNode.copySubtree(ast,equ.getRightOperand());
            copyedOne.setLeftOperand(re);
            InfixExpression.Operator op = getOppositeOperator(equ.getOperator());
            assert op != null;
            copyedOne.setOperator(op);
            rewriter.replace(equ, copyedOne, null);
        }
        TextEdit edits = rewriter.rewriteAST(document, null);
        Utils.applyRewrite(edits, document,outputDirPath);

    }
}





