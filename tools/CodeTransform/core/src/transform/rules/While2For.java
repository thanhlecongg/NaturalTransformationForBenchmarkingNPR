package transform.rules;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import transform.Utils;

import java.util.ArrayList;

public class While2For extends ASTVisitor{
	ArrayList targetLines;
	CompilationUnit cu = null;
	Document document = null;
	String outputDirPath = null;
	ArrayList<WhileStatement> whileBin = new ArrayList<WhileStatement>();

	public While2For(CompilationUnit cu_, Document document_, String outputDirPath_, ArrayList targetLines) {
		this.cu = cu_;
		this.document = document_;
		this.outputDirPath = outputDirPath_;
		this.targetLines = targetLines;
	}
	
	
	public boolean visit(WhileStatement node) {
		//visit while-loop statements
		if(Utils.checkTargetLines(targetLines, cu, node)){
			whileBin.add(node);
		}
		return true;
	}
	
	public void endVisit(CompilationUnit node) {

		// If whileBin is empty, let's skip
		if (whileBin.size() == 0) {
			return;
		}
		
		AST ast = cu.getAST();
		ASTRewrite rewriter = ASTRewrite.create(ast);

		//Loop over while-loop statements
		for(WhileStatement whiler: whileBin){

			//Convert while-loop to for-loop
			ForStatement forer = ast.newForStatement();
			Expression theexp = (Expression) ASTNode.copySubtree(ast, whiler.getExpression());
			forer.setExpression(theexp);
			Statement bodystatement = whiler.getBody();
			Statement forbody = (Statement) ASTNode.copySubtree(ast, bodystatement);
			forer.setBody(forbody);
			rewriter.replace(whiler, forer, null);
		}

		//Rewrite
		TextEdit edits = rewriter.rewriteAST(document, null);
		Utils.applyRewrite(edits, document, outputDirPath);
	}
}
