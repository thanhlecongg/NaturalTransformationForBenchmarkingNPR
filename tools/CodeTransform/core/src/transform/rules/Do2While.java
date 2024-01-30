package transform.rules;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import transform.Utils;

import java.util.ArrayList;
import java.util.List;

public class Do2While extends ASTVisitor{
    CompilationUnit cu;
    Document document;
    String outputDirPath;
    ArrayList<ForStatement> forsBin = new ArrayList<ForStatement>();
    ArrayList<Integer> targetLines;
    ArrayList<DoStatement> dosBin = new ArrayList<>();
    public Do2While(CompilationUnit cu_, Document document_, String outputDirPath_, ArrayList targetLines) {
        this.cu = cu_;
        this.document = document_;
        this.outputDirPath = outputDirPath_;
        this.targetLines = targetLines;
    }


    public boolean visit(DoStatement node) {
        if(Utils.checkTargetLines(targetLines, cu, node)){
            dosBin.add(node);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public void endVisit(CompilationUnit node) {
        // TODO: Implement transformation (Currently, there is no coverage so do not implement)
        if(dosBin.size() != 0){
            //get AST
            AST ast = cu.getAST();
            //create rewriter
            ASTRewrite rewriter = ASTRewrite.create(ast);
            //Rewrite
            TextEdit edits = rewriter.rewriteAST(document, null);
            Utils.applyRewrite(edits, document, outputDirPath);
        }
    }
}
