package transform.rules;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import transform.Config;
import transform.Utils;

import java.util.*;

public class SwapStatement extends ASTVisitor{
	ArrayList targetLines;
	CompilationUnit cu = null;
	Document document = null;
	String outputDirPath = null;
	Map<Statement, Statement > statementDependency = new HashMap<Statement, Statement >();
	Set<Statement> statementBin = new HashSet<Statement>();
	AST ast = null;
	ASTRewrite rewriter = null;
	
	public SwapStatement(CompilationUnit cu_, Document document_, String outputDirPath_, ArrayList targetLines) {
		this.cu = cu_;
		this.document = document_;
		this.outputDirPath = outputDirPath_;
		ast = cu.getAST();
		rewriter = ASTRewrite.create(ast);
		this.targetLines = targetLines;
	}
	
	public boolean visit(Block blocker) {
		@SuppressWarnings("unchecked")
		List<Statement> stas = blocker.statements();
		int lener = stas.size();
		for(int i = 0; i < lener-1; i++) {
			Statement presta = stas.get(i);
			Statement aftersta = stas.get(i+1);
			if(statementBin.contains(presta) || statementBin.contains(aftersta)) {
				continue;
			}
			
			if(canSwitch(presta, aftersta)) {
				if(Utils.checkTargetLines(this.targetLines, this.cu, presta) && Utils.checkTargetLines(this.targetLines, this.cu, aftersta)) {
					statementDependency.put(presta, aftersta);
					statementBin.add(presta);
					statementBin.add(aftersta);
				}
			}
		}
		return true;
	}


	private boolean isCalling(Statement s) {
		// check if a statement containing a call
		if (s.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
			 Expression es = ((ExpressionStatement)s).getExpression();
			 if(es.getNodeType() == ASTNode.METHOD_INVOCATION) {
				 return true;
			 }
		}
		return false;
	}
	
	
	private boolean canSwitch(Statement prestmt, Statement poststmt) {
		//Before anything, lets check the special statements
		int ForbiddenStatements[] = {ASTNode.CONTINUE_STATEMENT, ASTNode.BREAK_STATEMENT, ASTNode.RETURN_STATEMENT};

		Set<Integer> FSset = new HashSet<>();

		//List all forbidden statements
		for (int fs : ForbiddenStatements) {
			FSset.add(fs);
		}

		//If pre-statement or post-statement is forbidden, let's skip
		if(FSset.contains(prestmt.getNodeType()) || FSset.contains(poststmt.getNodeType())) {
			return false;
		}

		//If pre-statement or post-statement call to a method, let's skip
		if(isCalling(prestmt) || isCalling(poststmt)) {
			return false;
		}

		//First, gather all the required variables.
		Set<IBinding> usedPre = Utils.resolveAlluseddVarNames(prestmt);

		//Second, gather all the modified variables of the big brother.
		Set<IBinding> usedafter = Utils.resolveAlluseddVarNames(poststmt);

		//Third, check if there is no intersection
		usedPre.retainAll(usedafter);
		if(usedPre.size() > 0) {
			return false;
		}

		else return true;
	}
	
	public void endVisit(CompilationUnit node) {
		if (statementDependency.size() == 0) {
			return;
		}
		int counter = 0;
		Set<Statement> thePres = statementDependency.keySet();

		thePres = Utils.newShuffledSet(thePres);

		for(Statement thePre : thePres) {
			if (counter >= Config.maxTrans) break;
			Statement theAfter = statementDependency.get(thePre);
			Statement tmp = (Statement) ASTNode.copySubtree(ast, thePre);
			rewriter.replace(thePre, (Statement) ASTNode.copySubtree(ast, theAfter), null);
			rewriter.replace(theAfter, tmp, null);
			counter ++;

		}

		TextEdit edits = rewriter.rewriteAST(document, null);
		Utils.applyRewrite(edits, document,outputDirPath);
		
	}
	
}
