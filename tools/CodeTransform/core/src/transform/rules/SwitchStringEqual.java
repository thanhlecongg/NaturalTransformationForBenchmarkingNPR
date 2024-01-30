package transform.rules;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import transform.Utils;

public class SwitchStringEqual extends ASTVisitor{
	CompilationUnit cu = null;
	Document document = null;
	String outputDirPath = null;
	List<MethodInvocation> stringEqualBin = new ArrayList<MethodInvocation>();
	AST ast = null;
	ASTRewrite rewriter = null;
	ArrayList targetLines;
	public SwitchStringEqual(CompilationUnit cu_, Document document_, String outputDirPath_, ArrayList targetLines) {
		this.cu = cu_;
		this.document = document_;
		this.outputDirPath = outputDirPath_;
		ast = cu.getAST();
		rewriter = ASTRewrite.create(ast);
		this.targetLines = targetLines;
	}
	public boolean visit(MethodInvocation stringE) {
		if(stringE.resolveMethodBinding() != null 
				&& stringE.resolveMethodBinding().getMethodDeclaration().getKey().toString().equals("Ljava/lang/String;.equals(Ljava/lang/Object;)Z")
				&& stringE.arguments().size() == 1) {
			if(Utils.checkTargetLines(this.targetLines, this.cu, stringE)) {
				stringEqualBin.add(stringE);
			}
		}
		return true;
	}	
	
	

	public void endVisit(CompilationUnit node) {
		if (stringEqualBin.size() == 0) {
			return;
		}
		for(MethodInvocation stringE : stringEqualBin) {
			//get the caller
			Expression LeftOne = (Expression) ASTNode.copySubtree(ast, stringE.getExpression());
			//get the augment
			Expression RightOne = (Expression) ASTNode.copySubtree(ast,(Expression) stringE.arguments().get(0));
			if (RightOne.getNodeType() == ASTNode.INFIX_EXPRESSION) {
				 ParenthesizedExpression tmp = ast.newParenthesizedExpression();
				 tmp.setExpression((Expression) ASTNode.copySubtree(ast,RightOne));
				 RightOne = tmp;
			}
			
			//create new StringEual
			MethodInvocation newOne = (MethodInvocation) ASTNode.copySubtree(ast, stringE);
			List<Expression> args = newOne.arguments();
			args.remove(0);
			args.add(LeftOne);
			newOne.setExpression(RightOne);
			if ((RightOne.getNodeType() == ASTNode.SIMPLE_NAME 
					|| RightOne.getNodeType() == ASTNode.METHOD_INVOCATION)) {
				if (RightOne.getNodeType() == ASTNode.METHOD_INVOCATION) {
					String tmper = "hhh";
				}
				InfixExpression IENE = ast.newInfixExpression();
				IENE.setOperator(InfixExpression.Operator.NOT_EQUALS);
				IENE.setLeftOperand((Expression) ASTNode.copySubtree(ast, RightOne));
				IENE.setRightOperand(ast.newNullLiteral());
				
				InfixExpression Wraper = ast.newInfixExpression();
				Wraper.setOperator(InfixExpression.Operator.CONDITIONAL_AND);
				Wraper.setLeftOperand(IENE);
				Wraper.setRightOperand(newOne);
				ParenthesizedExpression tmp = ast.newParenthesizedExpression();
				tmp.setExpression(Wraper);
				rewriter.replace(stringE, tmp, null);
			}
			else {
				rewriter.replace(stringE, newOne, null);
			}
			
		}
		TextEdit edits = rewriter.rewriteAST(document, null);
		Utils.applyRewrite(edits, document,outputDirPath);
		
	}
}