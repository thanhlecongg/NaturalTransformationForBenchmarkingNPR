package transform.rules;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import transform.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DivideVarDecl extends ASTVisitor{
	ArrayList targetLines;
	CompilationUnit cu = null;
	Document document = null;
	String outputDirPath = null;
	Set<VariableDeclarationStatement> composedVDBin = new HashSet<VariableDeclarationStatement>();
	Set<VariableDeclarationStatement> initVDBin = new HashSet<VariableDeclarationStatement>();
	AST ast = null;
	ASTRewrite rewriter = null;
	
	public DivideVarDecl(CompilationUnit cu_, Document document_, String outputDirPath_, ArrayList targetLines) {
		this.cu = cu_;
		this.document = document_;
		this.outputDirPath = outputDirPath_;
		ast = cu.getAST();
		rewriter = ASTRewrite.create(ast);
		this.targetLines = targetLines;
	}
	
	public boolean visit(VariableDeclarationStatement vdstatment) {
		if (vdstatment.fragments().size() > 1 && vdstatment.getParent().getNodeType() == ASTNode.BLOCK) {
			if(Utils.checkTargetLines(targetLines, cu, vdstatment)) {
				composedVDBin.add(vdstatment);
			}
		} else {
			if ((vdstatment.fragments().size() == 1)) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment) vdstatment.fragments().get(0);
				if (fragment.getInitializer() != null){
					if(Utils.checkTargetLines(targetLines, cu, vdstatment)) {
						initVDBin.add(vdstatment);
					}
				}
			}

		}


		return false;
	}	
	
	

	public void endVisit(CompilationUnit node) {
		if (composedVDBin.size() == 0) {
			return;
		}
		for(VariableDeclarationStatement composedVDstatement : composedVDBin) {
			//get the type, get the modifiers
			Type typer = composedVDstatement.getType();
			List<Modifier> modis = composedVDstatement.modifiers();
			//iterate its fragments, create new ones.
			List<VariableDeclarationFragment> fraglist = composedVDstatement.fragments();
			ListRewrite lrt = rewriter.getListRewrite(composedVDstatement.getParent(), Block.STATEMENTS_PROPERTY);
			for (VariableDeclarationFragment frag : fraglist) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment) ASTNode.copySubtree(ast, frag);
				VariableDeclarationStatement newVDStatement = ast.newVariableDeclarationStatement(fragment);
				List<Modifier> tmpModis = newVDStatement.modifiers();
				for (Modifier x : modis) {
					tmpModis.add((Modifier) ASTNode.copySubtree(ast, x));
				}
				newVDStatement.setType((Type) ASTNode.copySubtree(ast, typer));
				lrt.insertAfter(newVDStatement, composedVDstatement, null);
			}
			rewriter.remove(composedVDstatement, null);
		}

		for(VariableDeclarationStatement initVDstatement : initVDBin) {

			ListRewrite lrt = rewriter.getListRewrite(initVDstatement.getParent(), Block.STATEMENTS_PROPERTY);

			//Get info
			Type typer = initVDstatement.getType();
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) initVDstatement.fragments().get(0);
			VariableDeclarationFragment newFragment = (VariableDeclarationFragment) ASTNode.copySubtree(ast, fragment);

			SimpleName name = newFragment.getName();
			Expression initializer = newFragment.getInitializer();

			VariableDeclarationStatement newVDStatement = ast.newVariableDeclarationStatement(newFragment);

			//change initializer in declaration to null
			newFragment.setInitializer(null);
			newVDStatement.setType((Type) ASTNode.copySubtree(ast, typer));
			lrt.insertAfter(newVDStatement, initVDstatement, null);
			rewriter.remove(initVDstatement, null);

			//assign value in a new assignment
			Assignment assignment = ast.newAssignment();
			assignment.setLeftHandSide(ast.newSimpleName(name.getIdentifier()));
			assignment.setRightHandSide((Expression) ASTNode.copySubtree(ast, initializer));

			lrt.insertAfter(assignment, newVDStatement, null);

		}
		TextEdit edits = rewriter.rewriteAST(document, null);
		Utils.applyRewrite(edits, document,outputDirPath);
		
	}
}