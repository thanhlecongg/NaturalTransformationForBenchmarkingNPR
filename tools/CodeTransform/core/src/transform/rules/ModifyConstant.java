package transform.rules;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import transform.Config;
import transform.Utils;

import java.util.*;

public class ModifyConstant extends ASTVisitor {
    Map<IBinding, ArrayList<SimpleName>> bindings2names = new HashMap<>();
    CompilationUnit cu;
    Document document;
    String outputDirPath;
    ArrayList targetLines;

    ArrayList<Assignment> assignmentBin = new ArrayList<>();
    ArrayList<VariableDeclarationFragment> declarationBin = new ArrayList<>();
    public ModifyConstant(CompilationUnit cu_, Document document_, String outputDirPath_, ArrayList targetLines) {
        this.cu = cu_;
        this.document = document_;
        this.outputDirPath = outputDirPath_;
        this.targetLines = targetLines;
    }

    private static String[] generateValue(String typeName, String value) {
        Random random = new Random();
        double[] tmp = new double[2];
        String[] returnVal = new String[2];

        tmp[0] = random.nextInt(50);
        tmp[1] = tmp[0] + parseNumber(typeName, value);

        switch (typeName) {
            case "int":
                returnVal[0] = String.valueOf((int) tmp[0]);
                returnVal[1] = String.valueOf((int) tmp[1]);
                break;
            case "byte":
                returnVal[0] = String.valueOf((byte) tmp[0]);
                returnVal[1] = String.valueOf((byte) tmp[1]);
                break;
            case "short":
                returnVal[0] = String.valueOf((short) tmp[0]);
                returnVal[1] = String.valueOf((short) tmp[1]);
                break;
            case "double":
                returnVal[0] = String.valueOf(tmp[0]);
                returnVal[1] = String.valueOf(tmp[1]);
                break;
            case "float":
                returnVal[0] = String.valueOf((float) tmp[0]);
                returnVal[1] = String.valueOf((float) tmp[1]);
                break;
            case "long":
                returnVal[0] = String.valueOf((long) tmp[0]);
                returnVal[1] = String.valueOf((long) tmp[1]);
                break;
            default:
                return null;
        }

        return returnVal;
    }

    private static double parseNumber(String typeName, String value) {
        switch (typeName) {
            case "int":
                return Integer.parseInt(value);
            case "byte":
                return Byte.parseByte(value);
            case "short":
                return Short.parseShort(value);
            case "double":
                return Double.parseDouble(value);
            case "float":
                return Float.parseFloat(value);
            case "long":
                if (value.endsWith("L") || value.endsWith("l")) {
                    return Long.decode(value.substring(0, value.length() - 1));
                } else {
                    return Long.decode(value);
                }
            default:
                throw new IllegalArgumentException("Unsupported type: " + typeName);
        }
    }


    public boolean visit(Assignment assignment) {
        if (assignment.getLeftHandSide() instanceof SimpleName && assignment.getRightHandSide() instanceof NumberLiteral) {
            if(Utils.checkTargetLines(this.targetLines, this.cu, assignment)) {
                assignmentBin.add(assignment);
            }
        }
        return true;
    }

    public boolean visit(VariableDeclarationFragment stmt) {
        SimpleName variable = stmt.getName();
        Type variableType = null;
        ASTNode parent = stmt.getParent();

        if (parent instanceof VariableDeclarationStatement) {
            variableType = ((VariableDeclarationStatement) parent).getType();
        } else if (parent instanceof VariableDeclarationExpression) {
            variableType = ((VariableDeclarationExpression) parent).getType();
        }

        Expression initializer = stmt.getInitializer();

        if (variableType != null && (variableType.isPrimitiveType() || variableType.isSimpleType())) {
            if (initializer instanceof NumberLiteral){
                if(Utils.checkTargetLines(this.targetLines, this.cu, stmt)){
                    declarationBin.add(stmt);
                }
            }

        }

        return true;
    }

    public void endVisit(CompilationUnit node) {
        System.out.println("Whole file is parsed! begin rewriting");
        AST ast = cu.getAST();
        ASTRewrite rewriter = ASTRewrite.create(ast);
        boolean isChange = false;
        for(Assignment assignment: assignmentBin){
            SimpleName variable = (SimpleName) assignment.getLeftHandSide();
            Expression value = assignment.getRightHandSide();

            ITypeBinding typeBinding = variable.resolveTypeBinding();
            String variableType = typeBinding != null ? typeBinding.getName() : "Unknown";
            String[] newValues = generateValue(variableType, value.toString());
            if (newValues == null){
                continue;
            }
            isChange = true;
            NumberLiteral numberLiteral1 = ast.newNumberLiteral(newValues[0]);
            NumberLiteral numberLiteral2 = ast.newNumberLiteral(newValues[1]);

            // Create right-hand side expression: 3.5 - 2.5
            Assignment newAssignment = ast.newAssignment();
            newAssignment.setLeftHandSide(ast.newSimpleName(variable.getIdentifier()));
            InfixExpression rightHandSide = ast.newInfixExpression();
            rightHandSide.setOperator(InfixExpression.Operator.MINUS);
            rightHandSide.setLeftOperand(numberLiteral2);
            rightHandSide.setRightOperand(numberLiteral1);
            newAssignment.setRightHandSide(rightHandSide);
            rewriter.replace(assignment, newAssignment, null);
        }

        for (VariableDeclarationFragment vdStmt: declarationBin) {
            SimpleName variable = vdStmt.getName();
            String variableType = null;
            ASTNode parent = vdStmt.getParent();

            if (parent instanceof VariableDeclarationStatement) {
                variableType = ((VariableDeclarationStatement) parent).getType().toString();
            } else if (parent instanceof VariableDeclarationExpression) {
                variableType = ((VariableDeclarationExpression) parent).getType().toString();
            }
            Expression initializer = vdStmt.getInitializer();

            String[] newValues = generateValue(variableType, initializer.toString());
            VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
            NumberLiteral numberLiteral1 = ast.newNumberLiteral(newValues[0]);
            NumberLiteral numberLiteral2 = ast.newNumberLiteral(newValues[1]);
            InfixExpression newInitializer = ast.newInfixExpression();
            newInitializer.setOperator(InfixExpression.Operator.MINUS);
            newInitializer.setLeftOperand(numberLiteral2);
            newInitializer.setRightOperand(numberLiteral1);
            fragment.setName(ast.newSimpleName(variable.getIdentifier()));
            fragment.setInitializer(newInitializer);
        }
        if(isChange){
            TextEdit edits = rewriter.rewriteAST(document, null);
            Utils.applyRewrite(edits, document, outputDirPath);
        }

    }
}





