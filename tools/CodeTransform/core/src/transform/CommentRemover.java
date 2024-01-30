package transform;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import transform.Utils;

public class CommentRemover {
    public static void main(String[] args) {
        // Path to your Java file
        String filePath = "../data/defects4j/transformed_methods/1_masked/828_org.joda.time.DateTimeZone_getOffsetFromLocal.java";

        try {
            // Initialize the ASTParser
            ASTParser parser = ASTParser.newParser(AST.JLS13);
            parser.setResolveBindings(true);
            parser.setKind(ASTParser.K_COMPILATION_UNIT);

            // Read the content of the Java file
            String source = Utils.readFileToString(filePath);
            // Set the source code for the parser
            parser.setSource(source.toCharArray());

            // Get the AST (Abstract Syntax Tree) for the Java file
            CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);

            // Create the ASTRewrite instance
            ASTRewrite rewrite = ASTRewrite.create(compilationUnit.getAST());

            // Visit the AST nodes to remove comments
            compilationUnit.accept(new ASTVisitor() {
//                @Override
//                public void preVisit(ASTNode node) {
//                    System.out.println(node.getClass());
//                    System.out.println(node);
//                    // Remove the comment by replacing it with an empty string
////                    rewrite.remove(node, null);
////                    return super.preVisit(node);
//                }
                @Override

                public boolean visit(BlockComment node) {
                    System.out.println(node);
                    // Remove the comment by replacing it with an empty string
                    rewrite.remove(node, null);
                    return super.visit(node);
                }

//                @Override
//                public boolean visit(Javadoc node) {
//                    System.out.println(node);
//                    // Remove the comment by replacing it with an empty string
//                    rewrite.remove(node, null);
//                    return super.visit(node);
//                }

                @Override
                public boolean visit(LineComment node) {
                    // Remove the comment by replacing it with an empty string
                    System.out.println(node);
                    rewrite.remove(node, null);
                    return super.visit(node);
                }
            });

            // Apply the changes to the AST
            TextEdit edits = rewrite.rewriteAST();

            // Update the source code with the modified AST
            Document document = new Document(source);
            edits.apply(document);
            // Save the modified source code back to the file

            System.out.println(document.get());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
