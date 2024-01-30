package transform.rules;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import transform.Config;
import transform.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RenameVariable extends ASTVisitor {
    Map<IBinding, ArrayList<SimpleName>> bindings2names = new HashMap<>();
    CompilationUnit cu;
    Document document;
    String outputDirPath;
    ArrayList targetLines;
    public RenameVariable(CompilationUnit cu_, Document document_, String outputDirPath_, ArrayList targetLines) {
        this.cu = cu_;
        this.document = document_;
        this.outputDirPath = outputDirPath_;
        this.targetLines = targetLines;
    }

    public boolean visit(SimpleName node) {
//        System.out.println();
        // resolve the binding (i.e., the reference) to the declaration of a node
        IBinding ibd = node.resolveBinding();

        // If the binding cannot be resolved, return true.
        if (ibd == null) {
            return true;
        }

        // If the binding already exists, add the current node to the list of names for that binding.
        if (this.bindings2names.containsKey(ibd)) {
            this.bindings2names.get(ibd).add(node);
        }

        // If this is a new binding for a variable declaration or parameter, create a new list of names and add it to the map.
        else if (node.getParent().getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT || node.getParent().getNodeType() == ASTNode.SINGLE_VARIABLE_DECLARATION) {
            ArrayList<SimpleName> tmp = new ArrayList<SimpleName>();
            tmp.add(node);
            this.bindings2names.put(ibd, tmp);
        }
        // Return true to continue traversing the AST.
        return true;
    }

    public void endVisit(CompilationUnit node) {
        System.out.println("Whole file is parsed! begin rewriting");
        AST ast = cu.getAST();
        ASTRewrite rewriter = ASTRewrite.create(ast);
        Set<IBinding> variableBins = this.bindings2names.keySet();
        variableBins = Utils.newShuffledSet(variableBins);
        int counter = 0;
        for (IBinding varBin : variableBins) {
            if (counter >= Config.maxTrans) break;
            String newName = "___MASKED_";
            ArrayList<SimpleName> vars = this.bindings2names.get(varBin);
            System.out.println("==>" + vars.get(0) + " is replaced by " + newName + vars.get(0));
            boolean is_hit = false;
            for(SimpleName var: vars) {
                if(targetLines.contains(cu.getLineNumber(var.getStartPosition()))){
                    is_hit = true;
                }
            }
            if(is_hit){
                for(SimpleName var: vars) {
                    rewriter.set(var, SimpleName.IDENTIFIER_PROPERTY, newName + var + "___", null);
                }
                counter += 1;
            }
        }
        TextEdit edits = rewriter.rewriteAST(document, null);
        Utils.applyRewrite(edits, document, outputDirPath);
    }
}





