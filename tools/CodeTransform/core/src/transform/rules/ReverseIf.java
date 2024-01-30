package transform.rules;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.PrefixExpression.Operator;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import transform.Utils;

import java.util.ArrayList;
import java.util.List;

public class ReverseIf extends ASTVisitor{
	ArrayList targetLines;
	CompilationUnit cu = null;
	Document document = null;
	String outputDirPath = null;
	ArrayList<IfStatement> ifStatementBin = new ArrayList<IfStatement>();

	public ReverseIf(CompilationUnit cu_, Document document_, String outputDirPath_, ArrayList targetLines) {
		this.cu = cu_;
		this.document = document_;
		this.outputDirPath = outputDirPath_;
		this.targetLines = targetLines;
	}
	
	
	public boolean visit(IfStatement node) {
		if(Utils.checkTargetLines(this.targetLines, this.cu, node)){
			ifStatementBin.add(node);
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
			IfStatement newifer = ast.newIfStatement();
			Expression theexp = (Expression) ASTNode.copySubtree(ast, ifer.getExpression());
			ParenthesizedExpression psizedexp = ast.newParenthesizedExpression();
			psizedexp.setExpression(theexp);
			PrefixExpression notexp = ast.newPrefixExpression();
			notexp.setOperator(Operator.toOperator("!"));
			notexp.setOperand(psizedexp);
			newifer.setExpression(notexp);
			Statement thenStatement = (Statement) ASTNode.copySubtree(ast,ifer.getThenStatement());
			Statement elseStatement = (Statement) ASTNode.copySubtree(ast,ifer.getElseStatement());
			if (elseStatement == null ) {
				elseStatement = ast.newEmptyStatement();
			}
			//Lets chek if this a nested else.
			else if(elseStatement.getNodeType() == ASTNode.IF_STATEMENT) {
				//System.out.println("Jusus! this is a nested if!");
				Block blocker = ast.newBlock();
				ListRewrite lrt = rewriter.getListRewrite(blocker, Block.STATEMENTS_PROPERTY);
				lrt.insertFirst(elseStatement, null);
				elseStatement = blocker;
			}
			newifer.setElseStatement(thenStatement);
			newifer.setThenStatement(elseStatement);
			
			rewriter.replace(ifer, newifer, null);
		}
		TextEdit edits = rewriter.rewriteAST(document, null);
		Utils.applyRewrite(edits, document,outputDirPath);
	}
}
