package transform.rules;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import transform.Utils;

import java.util.*;

public class MergeVarDecl extends ASTVisitor{
	ArrayList targetLines;
	CompilationUnit cu = null;
	Document document = null;
	String outputDirPath = null;
	Map<VariableDeclarationStatement, VariableDeclarationStatement> VarDecscanSwitch = 
			new HashMap<VariableDeclarationStatement, VariableDeclarationStatement>();
	Set<VariableDeclarationStatement> involvedStatements = new HashSet<VariableDeclarationStatement>();
	AST ast = null;
	ASTRewrite rewriter = null;
	
	public MergeVarDecl(CompilationUnit cu_, Document document_, String outputDirPath_, ArrayList targetLines) {
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
		for(int i = 0; i < lener - 1; i++) {
			VariableDeclarationStatement presta = null;
			VariableDeclarationStatement aftersta = null;
			try {
				presta = (VariableDeclarationStatement)stas.get(i);
				aftersta = (VariableDeclarationStatement) stas.get(i+1);
			}catch (Exception e) {
				continue;
			}
			if(involvedStatements.contains(presta) || involvedStatements.contains(aftersta)) {
				continue;
			}
			
			if(canSwitch(presta, aftersta)) {
				if (Utils.checkTargetLines(targetLines, cu, presta) || Utils.checkTargetLines(targetLines, cu, aftersta)){
					VarDecscanSwitch.put(presta,aftersta);
					involvedStatements.add(presta);
					involvedStatements.add(aftersta);
				}
			}
		}
		return true;
	}


	private Set<IBinding> namesInInitials(VariableDeclarationStatement decStatement){
		 Set<IBinding> res = new HashSet<IBinding>();
		 List<VariableDeclarationFragment> frags = decStatement.fragments();
		 for(VariableDeclarationFragment frag : frags) {
			 ASTNode tmp = frag.getInitializer();
			 if(tmp ==null) {
				 continue;
			 }
			 res.addAll(Utils.resolveAlluseddVarNames(tmp));
		 }
		 return res;
	}
	private Set<IBinding> namesInvars(VariableDeclarationStatement decStatement) {
		 Set<IBinding> res = new HashSet<IBinding>();
		 List<VariableDeclarationFragment> frags = decStatement.fragments();
		 for(VariableDeclarationFragment frag : frags) {
			 res.add(frag.getName().resolveBinding());
		 }
		 return res;
	}
	
	private boolean canSwitch(VariableDeclarationStatement presta, VariableDeclarationStatement aftersta) {
		
		
		//First, judge if the types are the same.
		if (presta.getType().resolveBinding() != aftersta.getType().resolveBinding()) {
			return false;
		}
		
		Set<IBinding> defBefore = namesInvars(presta);
		//Second, gather all the modified variables of the big brother.
		Set<IBinding> usedAfter = namesInInitials(aftersta);
		//Third, check if there is no intersection
		defBefore.retainAll(usedAfter);
		if(defBefore.size() > 0) {
			return false;
		}
		else return true;
	}
	
	

	public void endVisit(CompilationUnit node) {
		if (VarDecscanSwitch.size() == 0) {
			return;
		}
		Set<VariableDeclarationStatement> thePres = VarDecscanSwitch.keySet();
		for(VariableDeclarationStatement thePre : thePres) {
			VariableDeclarationStatement theAfter = VarDecscanSwitch.get(thePre);
//			System.out.println("Find One!");
//			System.out.println(thePre.toString());
//			System.out.println(theAfter.toString());
			//get the listrewriter of the prestate.
			List<VariableDeclarationFragment> prefrags = thePre.fragments();
			VariableDeclarationFragment latsoftheprefrags = prefrags.get(prefrags.size()-1);
			ListRewrite lrt = rewriter.getListRewrite(thePre, 
					(ChildListPropertyDescriptor) latsoftheprefrags.getLocationInParent());
		
			//insert the afterfrags into the prefrags.
			List<VariableDeclarationFragment> afterfrags = theAfter.fragments();
			for(VariableDeclarationFragment afterfrag:afterfrags) {
				lrt.insertLast(afterfrag, null);
			}
			//delet the afterdecStatement
			rewriter.remove(theAfter, null);

		}
		TextEdit edits = rewriter.rewriteAST(document, null);
		Utils.applyRewrite(edits, document,outputDirPath);
		
	}
}