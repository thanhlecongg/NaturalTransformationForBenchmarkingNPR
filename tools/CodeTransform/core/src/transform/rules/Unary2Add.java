package transform.rules;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import transform.Utils;

import java.util.ArrayList;

public class Unary2Add extends ASTVisitor{
	ArrayList targetLines;
	CompilationUnit cu = null;
	Document document = null;
	String outputDirPath = null;
	ArrayList<PostfixExpression> postUnaryBin = new ArrayList<PostfixExpression>();

	public Unary2Add(CompilationUnit cu_, Document document_, String outputDirPath_, ArrayList targetLines) {
		this.cu = cu_;
		this.document = document_;
		this.outputDirPath = outputDirPath_;
		this.targetLines = targetLines;
	}
	
	
	public boolean visit(PostfixExpression node) {
		//Visit all post unary statements;
		try {
			Statement staFather = (Statement) node.getParent();
			if(staFather.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
				if(Utils.checkTargetLines(targetLines, cu, node)) {
					postUnaryBin.add(node);
				}
			}
		}catch (Exception e) {
			return true;
		}
		return true;
	}
	
	public void endVisit(CompilationUnit node) {

		// if bin is emply, let's skip
		if (postUnaryBin.size() == 0) {
			return;
		}

		// transform
		AST ast = cu.getAST();
		ASTRewrite rewriter = ASTRewrite.create(ast);
		for(PostfixExpression poste: postUnaryBin){
			Assignment ass = ast.newAssignment(); 
			Expression thename = (Expression) ASTNode.copySubtree(ast, poste.getOperand());
			ass.setLeftHandSide(thename);
			ass.setRightHandSide(ast.newNumberLiteral("1"));
			if(poste.getOperator().toString() == "++") {
				ass.setOperator(Assignment.Operator.toOperator("+="));
			}
			else if (poste.getOperator().toString() == "--") {
				ass.setOperator(Assignment.Operator.toOperator("-="));
			}
			else {
				System.out.println("postExpression has an operator other than ++ and --");
				return;
			}
			rewriter.replace(poste, ass, null);
		}

		//rewrite
		TextEdit edits = rewriter.rewriteAST(document, null);
		Utils.applyRewrite(edits, document,outputDirPath);
	}
}
