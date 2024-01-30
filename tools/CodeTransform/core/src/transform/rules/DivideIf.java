package transform.rules;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import transform.Utils;

import java.util.ArrayList;

public class DivideIf extends ASTVisitor{
	ArrayList targetLines;
	CompilationUnit cu = null;
	Document document = null;
	String outputDirPath = null;
	ArrayList<IfStatement> ifStatementBin = new ArrayList<IfStatement>();

	public DivideIf(CompilationUnit cu_, Document document_, String outputDirPath_, ArrayList targetLines) {
		this.cu = cu_;
		this.document = document_;
		this.outputDirPath = outputDirPath_;
		this.targetLines = targetLines;
	}
	
	
	public boolean visit(IfStatement node) {
		Expression theexp = node.getExpression();
		try {
			if(theexp.getNodeType() == ASTNode.INFIX_EXPRESSION
					&& ((InfixExpression)theexp).getOperator() == InfixExpression.Operator.AND || ((InfixExpression)theexp).getOperator() == InfixExpression.Operator.CONDITIONAL_AND) {
				if(Utils.checkTargetLines(this.targetLines, this.cu, node)) {
					ifStatementBin.add(node);
				}
			}
		}catch (Exception e){
			return true;
		}

		return true;
	}
	
	public void endVisit(CompilationUnit node) {
		
		if (ifStatementBin.size() == 0) {
			return;
		}
		AST ast = cu.getAST();
		ASTRewrite rewriter = ASTRewrite.create(ast);
		for(IfStatement ifer: ifStatementBin){
			IfStatement newifer_outer = ast.newIfStatement();
			IfStatement newifer_inner = ast.newIfStatement();
			InfixExpression theexp = (InfixExpression) ASTNode.copySubtree(ast, ifer.getExpression());
			Expression exp_A = (Expression) ASTNode.copySubtree(ast, theexp.getLeftOperand());
			Expression exp_B = (Expression) ASTNode.copySubtree(ast, theexp.getRightOperand());
			
			newifer_outer.setExpression(exp_A);
			newifer_inner.setExpression(exp_B);
			
			newifer_inner.setThenStatement((Statement) ASTNode.copySubtree(ast,ifer.getThenStatement()));
			newifer_inner.setElseStatement((Statement) ASTNode.copySubtree(ast,ifer.getElseStatement()));
			
			Block blocker = ast.newBlock();
			blocker.statements().add(newifer_inner);
			newifer_outer.setThenStatement(blocker);
			newifer_outer.setElseStatement((Statement) ASTNode.copySubtree(ast,ifer.getElseStatement()));
			
			rewriter.replace(ifer, newifer_outer, null);
		}
		TextEdit edits = rewriter.rewriteAST(document, null);
		Utils.applyRewrite(edits, document,outputDirPath);
	}
}
