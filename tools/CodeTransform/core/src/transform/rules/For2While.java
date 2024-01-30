package transform.rules;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import transform.Utils;

import java.util.ArrayList;
import java.util.List;

public class For2While extends ASTVisitor{
    CompilationUnit cu;
    Document document;
    String outputDirPath;
    ArrayList<ForStatement> forsBin = new ArrayList<ForStatement>();
    ArrayList<Integer> targetLines;
    public For2While(CompilationUnit cu_, Document document_, String outputDirPath_, ArrayList targetLines) {
        this.cu = cu_;
        this.document = document_;
        this.outputDirPath = outputDirPath_;
        this.targetLines = targetLines;
    }


    public boolean visit(ForStatement node) {
        //Visit all for-loop statement
        if(Utils.checkTargetLines(targetLines, cu, node)){
            forsBin.add(node);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public void endVisit(CompilationUnit node) {
        //If for-loop does not exist, skip
        if (forsBin.size() == 0) {
            return;
        }

        //get AST
        AST ast = cu.getAST();

        //create rewriter
        ASTRewrite rewriter = ASTRewrite.create(ast);

        //loop over for-loop statements
        for(ForStatement forer: forsBin){

            //Mask local variables for initializing loop index
            ChangeInitNames(forer);

            // Init new while-loop
            WhileStatement whiler = ast.newWhileStatement();

            Expression theexp = null;

            // get for-loop expression and move to while
            if (forer.getExpression() == null) {
                theexp = ast.newBooleanLiteral(true);
            }
            else {
                theexp = (Expression) ASTNode.copySubtree(ast, forer.getExpression());
            }
            whiler.setExpression(theexp);

            // get for-loop body and move to while
            Statement bodystatement = forer.getBody();
            Statement whilebody = (Statement) ASTNode.copySubtree(ast, bodystatement);
            if(whilebody.getNodeType() != ASTNode.BLOCK) {
                Block blocker = ast.newBlock();
                blocker.statements().add(whilebody);
                whilebody = blocker;
            }
            whiler.setBody(whilebody);

            // get for-loop update-index expression and move to while
            List<Expression> updexpressions = forer.updaters();
            for(Expression upd: updexpressions) {
                ExpressionStatement updsta = ast.newExpressionStatement((Expression) ASTNode.copySubtree(ast, upd));
                ((Block) whilebody).statements().add(updsta);
            }

            // get for-loop init-index expression and move to while
            List<Expression> initexpressions = forer.initializers();

            Block blockoutsideWhile;
            if(forer.getParent().getNodeType() != ASTNode.BLOCK) {//we have to create a new block
                blockoutsideWhile = ast.newBlock();
                for(Expression ini: initexpressions) {
                    ExpressionStatement inista = ast.newExpressionStatement((Expression) ASTNode.copySubtree(ast, ini));
                    blockoutsideWhile.statements().add(inista);
                }
                blockoutsideWhile.statements().add(whiler);
                rewriter.replace(forer, blockoutsideWhile, null);
            }
            else {
                blockoutsideWhile = (Block) forer.getParent();
                ListRewrite lrt2 = rewriter.getListRewrite(blockoutsideWhile, Block.STATEMENTS_PROPERTY);
                for(Expression ini: initexpressions) {//Be careful!!!, we have to rename the local variable
                    ExpressionStatement inista = ast.newExpressionStatement((Expression) ASTNode.copySubtree(ast, ini));
                    lrt2.insertBefore(inista, forer, null);
                }
                rewriter.replace(forer, whiler, null);
            }
        }

        //Rewrite
        TextEdit edits = rewriter.rewriteAST(document, null);
        Utils.applyRewrite(edits, document, outputDirPath);
    }

    // If any local variable is initialized for the loop-index, rename it
    private void ChangeInitNames(ForStatement forer) {
        List<Expression> initexpressions = forer.initializers();
        int count = 0;
        for(Expression ini: initexpressions) {//Be careful!!!, we have to rename the local variable
            if(IsNameDeclaration(ini)) {
                List<VariableDeclarationFragment> ini_frags = ((VariableDeclarationExpression) ini).fragments();
                for(VariableDeclarationFragment frag: ini_frags) {
                    ArrayList<SimpleName>tochangeNames = new ArrayList<>();
                    SimpleName decName = frag.getName();
                    tochangeNames.add(decName);
                    SearchChange(forer.getExpression(),frag.getName(), tochangeNames);
                    SearchChange(forer.getBody(),frag.getName(), tochangeNames);
                    List<Expression> updas = forer.updaters();
                    for(Expression a: updas) {
                        SearchChange(a,frag.getName(), tochangeNames);
                    }

                    //Mask variable name
                    String newName = "___MASKED_";
                    for(SimpleName var: tochangeNames) {
                        var.setIdentifier(newName + "tmp" + String.valueOf(count) + "___");
                    }
                    count ++;

                }
            }
        }
    }


    private void SearchChange(ASTNode astnode, SimpleName name, ArrayList<SimpleName> tochangeNames) {
        class searchFortheSameVar extends ASTVisitor{
            public ArrayList<SimpleName> records = new ArrayList<SimpleName>();
            SimpleName equaler;
            public searchFortheSameVar(SimpleName equaler) {
                this.equaler = equaler;
            }

            public boolean visit(SimpleName node) {
                if(node.resolveBinding() == equaler.resolveBinding()) {
                    records.add(node);
                }
                return true;
            }

        }
        searchFortheSameVar searcher = new searchFortheSameVar(name);
        astnode.accept(searcher);
        tochangeNames.addAll(searcher.records);
    }


    private boolean IsNameDeclaration(Expression inista) {
        if (inista.getNodeType() == ASTNode.VARIABLE_DECLARATION_EXPRESSION) {
            return true;
        }
        else{
            return false;
        }
    }
}
