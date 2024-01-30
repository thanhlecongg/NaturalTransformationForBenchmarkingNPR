package transform.rules;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Assignment.Operator;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import transform.Utils;

import java.util.ArrayList;

public class If2CondExp extends ASTVisitor{
	CompilationUnit cu = null;
	Document document = null;
	String outputDirPath = null;
	ArrayList<IfStatement> singleIfBin = new ArrayList<IfStatement>();
	ArrayList targetLines;
	public If2CondExp(CompilationUnit cu_, Document document_, String outputDirPath_, ArrayList targetLines) {
		this.cu = cu_;
		this.document = document_;
		this.outputDirPath = outputDirPath_;
		this.targetLines = targetLines;
	}
	
	
	private boolean isSingleIf(IfStatement node) {
		System.out.println("=====");
		System.out.println(node);
		Statement thenstatement = node.getThenStatement();
		
		Statement elsestatement = node.getElseStatement();

		SimpleName theleftsideIF = null;
		SimpleName theleftsideELSE = null;
		if (thenstatement != null) {
			if(isSingleStatementBlock(thenstatement)) {
				thenstatement = (Statement) ((Block) thenstatement).statements().get(0);
			}
			
			int nodetype_then = thenstatement.getNodeType();
			if(nodetype_then != ASTNode.EXPRESSION_STATEMENT && nodetype_then != ASTNode.EMPTY_STATEMENT) {
				return false;
			}
			if(nodetype_then == ASTNode.EXPRESSION_STATEMENT) {
				Expression tmp = ((ExpressionStatement) thenstatement).getExpression();
				if (tmp.getNodeType() != ASTNode.ASSIGNMENT) {
					System.out.println(tmp.getNodeType());
					System.out.println("1");
					System.out.println("=====");
					return false;
				}
				Assignment theOne = (Assignment) tmp;
				if(theOne != null  && theOne.getNodeType() != ASTNode.ASSIGNMENT) {
					return false;
				}
				if(theOne != null) {
					try {
						theleftsideIF = (SimpleName) theOne.getLeftHandSide();
					}catch (Exception e){
						;
					}
				}
			}
			
		}
		
		if (elsestatement != null) {
			if(isSingleStatementBlock(elsestatement)) {
				elsestatement = (Statement) ((Block) elsestatement).statements().get(0);
			}
			
			int nodetype_else = elsestatement.getNodeType();
			if(nodetype_else != ASTNode.EXPRESSION_STATEMENT && nodetype_else != ASTNode.EMPTY_STATEMENT) {
				return false;
			}
			if(nodetype_else == ASTNode.EXPRESSION_STATEMENT) {
				Expression tmp = ((ExpressionStatement) elsestatement).getExpression();
				if (tmp.getNodeType() != ASTNode.ASSIGNMENT) {
					return false;
				}
				Assignment theOne = (Assignment) tmp;
				if(theOne != null  && theOne.getNodeType() != ASTNode.ASSIGNMENT) {
					return false;
				}
				
				if(theOne != null) {
					try {
						theleftsideELSE = (SimpleName) theOne.getLeftHandSide();
					}catch (Exception e){
						;
					}
				}
			}
			
		}
		if (theleftsideIF == theleftsideELSE && theleftsideELSE == null) {
			return false;
		}
		if (theleftsideIF == null) {
			return true;
		}
		if (theleftsideELSE == null) {
			return true;
		}
		if(theleftsideIF.resolveBinding() == theleftsideELSE.resolveBinding()) {
			return true;
		}
		return false;
	}
	
	
	
	private boolean isSingleStatementBlock(Statement statement) {
		if (statement.getNodeType() != ASTNode.BLOCK) {
			return false;
		}
		if (((Block) statement).statements().size() != 1) {
			return false;
		}
		return true;
	}


	public boolean visit(IfStatement node) {
		if (isSingleIf(node)) {
			if (Utils.checkTargetLinesAll(this.targetLines, this.cu, node)){
				singleIfBin.add(node);
			}
		}
		return true;
	}
	
	public void endVisit(CompilationUnit node) {
		
		if (singleIfBin.size() == 0) {
			return;
		}
		
		AST ast = cu.getAST();
		ASTRewrite rewriter = ASTRewrite.create(ast);
		
		//System.out.println("Find the one!");
		for(IfStatement singleIf : singleIfBin) {
			//System.out.println(singleIf.toString());
			
			Assignment newConditionalExp = ast.newAssignment();
			SimpleName sname = getSimpleNameFromSingleIf(singleIf);
			Expression theexp = singleIf.getExpression();
			ParenthesizedExpression tmp = ast.newParenthesizedExpression();
			tmp.setExpression((Expression) ASTNode.copySubtree(ast,theexp));
			Expression thenExp = getRightExpressiontFromSingleIfThenOrElseStatement(singleIf.getThenStatement());
			thenExp = (thenExp==null)? sname : thenExp;
			Expression elseExp = getRightExpressiontFromSingleIfThenOrElseStatement(singleIf.getElseStatement());
			elseExp = (elseExp==null)? sname : elseExp;
			ConditionalExpression ce = ast.newConditionalExpression();
			ce.setExpression(tmp);
			ce.setThenExpression((Expression) ASTNode.copySubtree(ast,thenExp));
			ce.setElseExpression((Expression) ASTNode.copySubtree(ast,elseExp));
			newConditionalExp.setLeftHandSide((Expression) ASTNode.copySubtree(ast,sname));
			newConditionalExp.setRightHandSide(ce);
			newConditionalExp.setOperator(Operator.toOperator("="));
			ExpressionStatement newES = ast.newExpressionStatement(newConditionalExp);
			//System.out.println("NewCE:\n" + newES.toString());
			rewriter.replace(singleIf, newES, null);	
		}
		TextEdit edits = rewriter.rewriteAST(document, null);
		Utils.applyRewrite(edits, document,outputDirPath);
		
	}

	
	
	private Expression getRightorLeftHandOfAssinmentFromExpressionStatementOrSingleBlock(int rightOrLeft, Statement estate) {
		if(isSingleStatementBlock(estate)) {
			estate = (Statement) ((Block) estate).statements().get(0);
		}
		Expression tmp = ((ExpressionStatement) estate).getExpression();
		if (rightOrLeft == 0) {// right
			Expression res = ((Assignment)tmp).getRightHandSide();
			return res;
		}
		else if(rightOrLeft == 1) {//left
			Expression res = ((Assignment)tmp).getLeftHandSide();
			return res;
		}
		return null;
	}
	
	private Expression getRightExpressiontFromSingleIfThenOrElseStatement(Statement statement) {
		Expression res = null;
		try {
			res = getRightorLeftHandOfAssinmentFromExpressionStatementOrSingleBlock(0, statement);
		}catch (Exception e){
			res = null;
		}
		return res;
	}


	private SimpleName getSimpleNameFromSingleIf(IfStatement singleIf) {
		SimpleName res= null;
		try {
			Statement thenstatement = singleIf.getThenStatement();
			res = (SimpleName) getRightorLeftHandOfAssinmentFromExpressionStatementOrSingleBlock(1, thenstatement);
		}catch (Exception e){
			res = null;
		}
		if(res != null) {
			return res;
		}
		try {
			Statement elseStatement = singleIf.getElseStatement();
			res = (SimpleName) getRightorLeftHandOfAssinmentFromExpressionStatementOrSingleBlock(1, elseStatement);
		}catch (Exception e){
			res = null;
		}
		if(res == null) {
			System.out.println("Warning!!!! no LeftSide is obatined From this IF, Shutting down...");
			System.exit(8);
		}
		return res;
		
	}
}
