package transform.rules;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import transform.Utils;

import java.util.ArrayList;

public class Add2Equal extends ASTVisitor{
	ArrayList targetLines;
	CompilationUnit cu = null;
	Document document = null;
	String outputDirPath = null;
	ArrayList<Assignment> addAssignBin = new ArrayList<Assignment>();

	public Add2Equal(CompilationUnit cu_, Document document_, String outputDirPath_, ArrayList targetLines) {
		this.cu = cu_;
		this.document = document_;
		this.outputDirPath = outputDirPath_;
		this.targetLines = targetLines;
	}
	
	
	public boolean visit(Assignment node) {
		try {
			String theOp = node.getOperator().toString();
			if(theOp == "+=" || theOp == "-=" || theOp == "*=" || theOp == "/=") {
				if(Utils.checkTargetLines(targetLines, cu, node)) {
					addAssignBin.add(node);
				}
			}
		}catch (Exception e) {
			return true;
		}
		return true;
	}
	
	public void endVisit(CompilationUnit node) {
		
		if (addAssignBin.size() == 0) {
			return;
		}
		
		AST ast = cu.getAST();
		ASTRewrite rewriter = ASTRewrite.create(ast);
		for(Assignment addAss: addAssignBin){
			Assignment newAss = ast.newAssignment();
			newAss.setOperator(Assignment.Operator.toOperator("="));
			newAss.setLeftHandSide((Expression) ASTNode.copySubtree(ast, addAss.getLeftHandSide()));
			
			Expression exp = (Expression) ASTNode.copySubtree(ast, addAss.getRightHandSide());

			// The code for ensuring that a += b ==> a = a + b instead of a = a + (b)
			// 42 is variable, 34 is number, 45 is string
			Expression theRightOftheRight;
			if (exp.getNodeType() == 42 || exp.getNodeType() == 34 || exp.getNodeType() == 45){
				theRightOftheRight = exp;
			} else {
				theRightOftheRight = ast.newParenthesizedExpression();
				((ParenthesizedExpression) theRightOftheRight).setExpression(exp);
			}
			
			Expression theLeftOftheRight = (Expression) ASTNode.copySubtree(ast, addAss.getLeftHandSide());
			
			InfixExpression theNewRight = ast.newInfixExpression();
			theNewRight.setLeftOperand(theLeftOftheRight);
			theNewRight.setRightOperand(theRightOftheRight);

			String theaddTyep = addAss.getOperator().toString();
			theNewRight.setOperator(InfixExpression.Operator.toOperator(theaddTyep.substring(0,1)));

			newAss.setRightHandSide(theNewRight);
			rewriter.replace(addAss, newAss, null);
		}
		TextEdit edits = rewriter.rewriteAST(document, null);
		Utils.applyRewrite(edits, document,outputDirPath);
	}
}
