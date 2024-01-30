package transform.rules;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;

import transform.Utils;

public class SwitchEqualExp extends ASTVisitor{
	CompilationUnit cu = null;
	Document document = null;
	String outputDirPath = null;
	List<InfixExpression> equalStatementBin = new ArrayList<InfixExpression>();
	AST ast = null;
	ASTRewrite rewriter = null;
	ArrayList targetLines;
	public SwitchEqualExp(CompilationUnit cu_, Document document_, String outputDirPath_, ArrayList targetLines) {
		this.cu = cu_;
		this.document = document_;
		this.outputDirPath = outputDirPath_;
		ast = cu.getAST();
		rewriter = ASTRewrite.create(ast);
		this.targetLines = targetLines;
	}
	
	public boolean visit(InfixExpression equ) {
		if(equ.getOperator().toString() == InfixExpression.Operator.EQUALS.toString()) {
			if(Utils.checkTargetLines(this.targetLines, this.cu, equ)){
				equalStatementBin.add(equ);
			}
		}
		return true;
	}	
	
	

	public void endVisit(CompilationUnit node) {
		if (equalStatementBin.size() == 0) {
			return;
		}
		for(InfixExpression equ : equalStatementBin) {
			//Copy
			InfixExpression copyedOne = (InfixExpression) ASTNode.copySubtree(ast, equ);
			Expression le = (Expression) ASTNode.copySubtree(ast,equ.getLeftOperand());
			copyedOne.setRightOperand(le);
			Expression re = (Expression) ASTNode.copySubtree(ast,equ.getRightOperand());
			copyedOne.setLeftOperand(re);
			rewriter.replace(equ, copyedOne, null);
		}
		TextEdit edits = rewriter.rewriteAST(document, null);
		Utils.applyRewrite(edits, document,outputDirPath);
		
	}
}