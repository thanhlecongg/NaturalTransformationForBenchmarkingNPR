package transform.rules;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import transform.Config;
import transform.Utils;

import java.util.*;

public class AddUnusedCode extends ASTVisitor {
    CompilationUnit cu;
    Document document;
    String outputDirPath;
    Set<ASTNode> stmtBin = new HashSet<>();
    ASTRewrite rewriter = null;
    AST ast = null;

    public AddUnusedCode(CompilationUnit cu_, Document document_, String outputDirPath_, ArrayList targetLines) {
        this.cu = cu_;
        this.document = document_;
        this.outputDirPath = outputDirPath_;
        this.ast = cu.getAST();
        this.rewriter = ASTRewrite.create(ast);

    }

    public boolean visit(MethodDeclaration method) {
        stmtBin.addAll(method.getBody().statements());
        return true;
    }

    public void endVisit(CompilationUnit node) {
        stmtBin = Utils.newShuffledSet(stmtBin);
        int cnt = 0;
        for(ASTNode stmt: stmtBin){
            ListRewrite lrt = rewriter.getListRewrite(stmt.getParent(), Block.STATEMENTS_PROPERTY);

            if (cnt > Config.maxTrans){
                continue;
            }
            MethodInvocation newPrint = Utils.createPrintStmt(this.ast);
            lrt.insertAfter(newPrint, stmt, null);
            cnt ++;
        }

        stmtBin = Utils.newShuffledSet(stmtBin);
        ASTNode first = stmtBin.iterator().next();

        ListRewrite lrt = rewriter.getListRewrite(first.getParent(), Block.STATEMENTS_PROPERTY);

        lrt.insertAfter(Utils.createDeadCode(ast), first, null);

        TextEdit edits = this.rewriter.rewriteAST(document, null);
        Utils.applyRewrite(edits, document,outputDirPath);
    }


}





