package transform.rules;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import transform.Utils;

import java.util.ArrayList;

public class IfElseIf2IfElse extends ASTVisitor {
    CompilationUnit cu;
    Document document;
    String outputDirPath;
    ArrayList<ForStatement> forsBin = new ArrayList<ForStatement>();
    ArrayList<Integer> targetLines;
    ArrayList<IfStatement> ifsBin = new ArrayList<>();

    public IfElseIf2IfElse(CompilationUnit cu_, Document document_, String outputDirPath_, ArrayList<Integer> targetLines) {
        this.cu = cu_;
        this.document = document_;
        this.outputDirPath = outputDirPath_;
        this.targetLines = targetLines;
    }

    public boolean visit(IfStatement node) {
        if (node.getElseStatement() != null) {
            if (node.getElseStatement() instanceof IfStatement) {
                if (Utils.checkTargetLines(targetLines, cu, node.getElseStatement())) {
                    ifsBin.add(node);
                }
            }
        }
        return true;
    }

    //TODO: Transform multiple else-if statement
    public void endVisit(CompilationUnit node) {
        if (ifsBin.size() != 0) {
            // get AST
            AST ast = cu.getAST();
            // create rewriter
            ASTRewrite rewriter = ASTRewrite.create(ast);
            for (IfStatement ifStmt : ifsBin) {
                // Do transformation here
                IfStatement elseIfStmt = (IfStatement) ASTNode.copySubtree(ast, ifStmt.getElseStatement());
                Block elseBlock = ast.newBlock();

                IfStatement newElseIfStmt = ast.newIfStatement();
                newElseIfStmt.setExpression((Expression) ASTNode.copySubtree(ast, elseIfStmt.getExpression()));
                newElseIfStmt.setThenStatement((Statement) ASTNode.copySubtree(ast, elseIfStmt.getThenStatement()));
                newElseIfStmt.setElseStatement((Statement) ASTNode.copySubtree(ast, elseIfStmt.getElseStatement()));
                elseBlock.statements().add(newElseIfStmt);

                IfStatement newIfStmt = ast.newIfStatement();
                newIfStmt.setExpression((Expression) ASTNode.copySubtree(ast, ifStmt.getExpression()));
                newIfStmt.setThenStatement((Statement) ASTNode.copySubtree(ast, ifStmt.getThenStatement()));
                newIfStmt.setElseStatement(elseBlock);

                rewriter.replace(ifStmt, newIfStmt, null);
            }
            // Rewrite
            TextEdit edits = rewriter.rewriteAST(document, null);
            Utils.applyRewrite(edits, document, outputDirPath);
        }
    }
}
