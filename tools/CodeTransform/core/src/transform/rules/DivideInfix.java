package transform.rules;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import transform.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class DivideInfix extends ASTVisitor{
	ArrayList targetLines;
	CompilationUnit cu = null;
	Document document = null;
	String outputDirPath = null;
	ArrayList<InfixExpression> infixExpBin = new ArrayList<InfixExpression>();
	Set<Expression> ReplicationAvoiding = new HashSet<Expression>();
	
	public DivideInfix(CompilationUnit cu_, Document document_, String outputDirPath_, ArrayList targetLines) {
		this.cu = cu_;
		this.document = document_;
		this.outputDirPath = outputDirPath_;
		this.targetLines = targetLines;
	}

	public boolean visit(InfixExpression node) {
		try {
			InfixExpression parent = Utils.parent2AInfixExpression(node);
			if(parent !=null && PrimitiveType.toCode(node.resolveTypeBinding().toString()) != null) {
				if(!ReplicationAvoiding.contains(parent)) { // In case We write one Expression Twice.
					if(Utils.checkTargetLines(this.targetLines, this.cu, node)){
						infixExpBin.add(node);
					}
				}
				ReplicationAvoiding.add(node);
			}
		}catch (Exception e) {
			return true;
		}
		return true;
	}

	public void endVisit(CompilationUnit node) {
		
		if (infixExpBin.size() == 0) {
			return;
		}
		
		AST ast = cu.getAST();
		ASTRewrite rewriter = ASTRewrite.create(ast);
		int cnt = 0;
		boolean is_change = false;
		for(InfixExpression theInfixExp: infixExpBin){
			String theStringOftheType = theInfixExp.resolveTypeBinding().toString();
			VariableDeclarationFragment tmpDecFra = ast.newVariableDeclarationFragment();
			String newVarname = "___MASKED_tmp" + cnt + "___";
			SimpleName tmpName = ast.newSimpleName(newVarname);
			tmpDecFra.setName(tmpName);
			tmpDecFra.setInitializer((InfixExpression) ASTNode.copySubtree(ast, theInfixExp));
			
			VariableDeclarationStatement tmpDec = ast.newVariableDeclarationStatement(tmpDecFra);
			PrimitiveType pt = ast.newPrimitiveType(PrimitiveType.toCode(theStringOftheType));
			tmpDec.setType(pt);
			
			Statement insertbeforethisparent = Utils.parent2AListRewriterForStatementInserting(theInfixExp, rewriter);
			if(insertbeforethisparent == null) {
				continue;
			}
			is_change = true;
			ListRewrite lrt = rewriter.getListRewrite(insertbeforethisparent.getParent(), (ChildListPropertyDescriptor) insertbeforethisparent.getLocationInParent());
			lrt.insertBefore(tmpDec, insertbeforethisparent, null);
			rewriter.replace(theInfixExp, tmpName, null);
			cnt ++;

		}
		if (is_change) {
			TextEdit edits = rewriter.rewriteAST(document, null);
			Utils.applyRewrite(edits, document, outputDirPath);
		}
	}
}
