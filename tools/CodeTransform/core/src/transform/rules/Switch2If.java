package transform.rules;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import transform.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Switch2If extends ASTVisitor{
	ArrayList targetLines;
	CompilationUnit cu = null;
	Document document = null;
	String outputDirPath = null;
	ArrayList<SwitchStatement> switchBin = new ArrayList<SwitchStatement>();

	public Switch2If(CompilationUnit cu_, Document document_, String outputDirPath_, ArrayList targetLines) {
		this.cu = cu_;
		this.document = document_;
		this.outputDirPath = outputDirPath_;
		this.targetLines = targetLines;
	}
	
	
	public boolean visit(SwitchStatement node) {
		//visit switch-case statements
		boolean is_hit = false;

		if(Utils.checkTargetLines(targetLines, cu, node)){
			is_hit = true;
//
		}
		List<Statement> caseStatments = node.statements();
		for (Statement caseSt: caseStatments) {
			if (caseSt.getNodeType() == ASTNode.SWITCH_CASE) {
				if(Utils.checkTargetLines(targetLines, cu, caseSt)){
					is_hit = true;
				}
			}
		}
		if (is_hit){
			switchBin.add(node);
		}

		return true;
	}
	
	public void endVisit(CompilationUnit node) {

		//if switchBin is empty, let's skip
		if (switchBin.size() == 0) {
			return;
		}

		//Transform
		AST ast = cu.getAST();
		ASTRewrite rewriter = ASTRewrite.create(ast);
		for(SwitchStatement switchCase: switchBin){
			List<Statement> caseStatments = switchCase.statements();
			HashMap<Expression,List<Statement>> cond2Sts = new HashMap<Expression,List<Statement>>();
			List<Expression> wait2fill = new ArrayList<Expression>();
			for (Statement caseSt: caseStatments) {
				if (caseSt.getNodeType() == ASTNode.SWITCH_CASE) {
					Expression condexp = ((SwitchCase) caseSt).getExpression();
					wait2fill.add(condexp);
					assert(!cond2Sts.containsKey(condexp));
					cond2Sts.put(condexp, new ArrayList<Statement>());

				}
				else if (caseSt.getNodeType() == ASTNode.BREAK_STATEMENT) {
					wait2fill.clear();
				}
				else {
					assert(caseSt.getNodeType() == ASTNode.EXPRESSION_STATEMENT);
					for (Expression exp: wait2fill) {
						assert(cond2Sts.containsKey(exp));
						cond2Sts.get(exp).add(caseSt);
					}
				}
			}
			
			IfStatement thefirstIf = ast.newIfStatement();
			IfStatement thecurrentIf = thefirstIf;
			for (Expression condexp: cond2Sts.keySet()) {
				if (condexp==null) {
					continue;
				}
				Expression swtichExp = (Expression) ASTNode.copySubtree(ast,switchCase.getExpression());
				InfixExpression theExp = ast.newInfixExpression();
				theExp.setOperator(InfixExpression.Operator.EQUALS);
				theExp.setLeftOperand(swtichExp);
				theExp.setRightOperand((Expression) ASTNode.copySubtree(ast,condexp));
				thecurrentIf.setExpression(theExp);
				
				Block block =ast.newBlock();
				for (Statement casest: cond2Sts.get(condexp)) {
					block.statements().add((Statement) ASTNode.copySubtree(ast,casest));
				}
				thecurrentIf.setThenStatement(block);
				IfStatement theNextIf = ast.newIfStatement(); 
				thecurrentIf.setElseStatement(theNextIf);
				thecurrentIf = theNextIf;
			}
			
			if (cond2Sts.containsKey(null)) {
				IfStatement lastSecond = (IfStatement) thecurrentIf.getParent();
				Block block =ast.newBlock();
				for (Statement casest: cond2Sts.get(null)) {
					block.statements().add((Statement) ASTNode.copySubtree(ast,casest));
				}
				lastSecond.setElseStatement(block);
			}
			else {
				IfStatement lastSecond = (IfStatement) thecurrentIf.getParent();
				lastSecond.setElseStatement(null);
			}


			//Rewrite
			rewriter.replace(switchCase, thefirstIf, null);
		}
		TextEdit edits = rewriter.rewriteAST(document, null);
		Utils.applyRewrite(edits, document,outputDirPath);
	}
}
