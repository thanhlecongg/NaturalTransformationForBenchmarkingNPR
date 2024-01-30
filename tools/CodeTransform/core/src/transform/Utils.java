package transform;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import java.io.*;
import java.util.*;

public class Utils {
    public static String getRandomString(int length){
        String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";
        String head = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random=new Random();
        StringBuffer sb=new StringBuffer();
        int number=random.nextInt(52);
        sb.append(head.charAt(number));
        for(int i=1;i<length;i++){
            number=random.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    public static String sublizeOutput(String filePath, String dirPath, String outputdir) {
        // return outpur file

        int ender = dirPath.length();
        String [] parts = filePath.split("/");
        String newDirPath = outputdir + parts[parts.length-1];

        return newDirPath;
    }
    public static String readFileToString(String filePath) throws IOException {
        // read file to string

        StringBuilder fileData = new StringBuilder(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        char[] buf = new char[10];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            //System.out.println(numRead);
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }

        reader.close();

        return  fileData.toString();
    }

    public static void mkfatherdir(String path) {
        // Mkdir a directory with path
        File f;
        try {
            f = new File(path);
            f = f.getParentFile();
            if (!f.exists()) {
                boolean success = f.mkdirs();
                if (!success) {
                    System.out.println("Dir make failed!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static File[] folderMethod(String inputDir, String outputDir) {
        int fileNum = 0, folderNum = 0;

        // Init
        File file = new File(inputDir);
        LinkedList<File> list = new LinkedList<>();
        ArrayList<File> res = new ArrayList<File>();

        //Check if inputDir exists
        if (file.exists()) {

            // check if inputDir is empty
            if (null == file.listFiles()) {
                return res.toArray(new File[res.size()]);
            }

            // add all files in inputDIr to a list
            list.addAll(Arrays.asList(file.listFiles()));

            // loop until list is empty
            while (!list.isEmpty()) {

                //remove current file/folder from list
                File curr = list.removeFirst();
                File[] files = curr.listFiles();


                // if curr is file
                if (null == files) {//Maybe a file

                    //Check end if ".java"
                    if (curr.getName().split("\\.")[1].equals("java")) {
                        int ender = inputDir.length();

                        //output path
                        String newDirPath = outputDir + curr.getAbsolutePath().substring(ender);

                        //Create parent dir if not exists
                        mkfatherdir(newDirPath);

                        //add curr to res
                        res.add(curr);
                    }
                    fileNum++;
                    continue;
                }

                // Recursively loop files/folder if curr is folder
                for (File f : files) {
                    if (f.isDirectory()) {

                        // recursively add to list
                        list.add(f);
                        folderNum++;

                    } else {

                        //Check end if ".java"
                        if (f.getName().split("\\.")[1].equals("java")) {
                            int ender = inputDir.length();
                            String newDirPath = outputDir + f.getAbsolutePath().substring(ender);
                            mkfatherdir(newDirPath);

                            res.add(f);
                        }
                        fileNum++;
                    }
                }
            }
        } else {
            System.out.println("Root does not exist!");
        }
        System.out.println("Input Directory: " + inputDir);
        System.out.println("==> Total number of sub-folders:" + folderNum + "\n==> Total number of files:" + fileNum);
        return res.toArray(new File[res.size()]);
    }

    public static void applyRewrite(TextEdit edits, Document document, String outputDirPath) {
        try {
            edits.apply(document);
        } catch(MalformedTreeException e) {
            e.printStackTrace();
        } catch(BadLocationException e) {
            e.printStackTrace();
        }

        String code = document.get();

        IDocument formatteddoc = new Document(code);
        try {
            CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(null, ToolFactory.M_FORMAT_EXISTING);
            TextEdit textEdit = codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT, code, 0, code.length(), 0, "\n");
            textEdit.apply(formatteddoc);
        } catch (MalformedTreeException | BadLocationException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
				e.printStackTrace();
				System.out.println(code);
        }
        String newcode = formatteddoc.get();

        // TODO Auto-generated method stub

        try {
            //String classname = FromCompilationUnit2ClassName(node);
            String filepath = outputDirPath;

            FileWriter writer = new FileWriter(filepath);
            writer.write(newcode);
            writer.flush();
            writer.close();
            System.out.println(filepath + "	reWrited successfully!");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {

        }
    }
    public static Set<IBinding> resolveAlluseddVarNames(ASTNode node){
        //System.out.println(node.toString());
        class varNameRecording extends ASTVisitor {
            public Set<IBinding> res = new HashSet<IBinding> ();
            public boolean visit(SimpleName namer) {
                //if(namer.isVar()) {
                res.add(namer.resolveBinding());
                //}
                return true;
            }

        };
        varNameRecording varC = new varNameRecording();
        node.accept(varC);
        //System.out.println(varC.res.toString());
        return varC.res;
    }
    public static <T> Set<T> newShuffledSet(Collection<T> collection) {
        List<T> shuffleMe = new ArrayList<T>(collection);
        Collections.shuffle(shuffleMe, new Random(Config.randomSeed));
        return new LinkedHashSet<T>(shuffleMe);
    }

    public static Statement parent2AStatement(ASTNode node) {
        while(!(node.getParent() instanceof  Statement)) {
            return parent2AStatement(node.getParent());
        }
        return (Statement) node.getParent();

    }

    public static InfixExpression parent2AInfixExpression(Expression node) {
        if(!(node.getParent() instanceof  Expression)) {
            return null;
        }
        if(!(node.getParent() instanceof InfixExpression)) {
            return parent2AInfixExpression( (Expression) node.getParent());
        }
        return (InfixExpression) node.getParent();
    }

    public static Statement parent2AListRewriterForStatementInserting(ASTNode node, ASTRewrite rewriter) {
        if(node == null) {
            return null;
        }
        if(node.getNodeType() == ASTNode.FOR_STATEMENT || node.getNodeType() == ASTNode.WHILE_STATEMENT
                || node.getNodeType() == ASTNode.DO_STATEMENT || node.getNodeType() == ASTNode.ENHANCED_FOR_STATEMENT) {
            return (Statement) node;
        }

        if(!(node instanceof Statement)) {
            System.out.println(node.getClass());
            return parent2AListRewriterForStatementInserting(node.getParent(), rewriter);
        }
        try {
            @SuppressWarnings("unused")
            ListRewrite lrt = rewriter.getListRewrite(node.getParent(), (ChildListPropertyDescriptor) node.getLocationInParent());
            return (Statement) node;
        }catch (Exception e) {
            return parent2AListRewriterForStatementInserting(node.getParent(), rewriter);
        }
    }

    public static MethodInvocation createPrintStmt(AST ast){
        MethodInvocation printMethodInvocation = ast.newMethodInvocation();
        printMethodInvocation.setExpression(ast.newSimpleName("System"));
        printMethodInvocation.setName(ast.newSimpleName("out"));

        MethodInvocation printlnMethodInvocation = ast.newMethodInvocation();
        printlnMethodInvocation.setExpression(printMethodInvocation);
        printlnMethodInvocation.setName(ast.newSimpleName("println"));

        StringLiteral stringLiteral = ast.newStringLiteral();
        stringLiteral.setLiteralValue("MASKED");
        printlnMethodInvocation.arguments().add(stringLiteral);

        return printlnMethodInvocation;
    }

    public static IfStatement createDeadCode(AST ast){
        Expression condition = ast.newBooleanLiteral(false);
        IfStatement ifStatement = ast.newIfStatement();
        ifStatement.setExpression(condition);

        return ifStatement;
    }

    public static boolean checkTargetLines(ArrayList<Integer> targetLines, CompilationUnit cu, ASTNode node){
        return targetLines.contains(cu.getLineNumber(node.getStartPosition()));
    }

    public static boolean checkTargetLinesAll(ArrayList<Integer> targetLines, CompilationUnit cu, ASTNode node){
        boolean is_hit = false;
        for(Integer line_no: targetLines){
            if (cu.getLineNumber(node.getStartPosition()) < line_no && cu.getLineNumber(node.getStartPosition() + node.getLength() - 1) > line_no){
                is_hit = true;
            }
        }
        return is_hit;
    }




}

