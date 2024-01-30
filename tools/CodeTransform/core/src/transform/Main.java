package transform;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.Document;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.cli.*;


public class Main {

    public static void parserFileInDir(String inDir, String outDir, String ruleID, List<Defects4jProject> projects){
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        List<Future<String>> futures = new ArrayList<>();
        // Submit tasks for each object in the list
        for (Defects4jProject project : projects) {
            ObjectProcessor task = new ObjectProcessor(project, inDir, outDir, ruleID);
            Future<String> future = executorService.submit(task);
            futures.add(future);
        }


        // Process the results
        for (Future<String> future : futures) {
            try {
                String result = future.get();
            } catch (InterruptedException | ExecutionException e) {

            }
        }

        // Shutdown the ExecutorService
        executorService.shutdown();
    }
    public static void main(String[] args) {
        String inDir = Config.inDir;
        String outDir = Config.outDir;
        String ruleID = Config.ruleID;
        String infoDir = Config.infoDir;
        try (Reader reader = new FileReader(infoDir)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Defects4jProject>>() {}.getType();
            List<Defects4jProject> projects = gson.fromJson(reader, listType);
            parserFileInDir(inDir, outDir, ruleID, projects);

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}

class ObjectProcessor implements Callable<String> {
    private final Defects4jProject myObject;
    private final String outDir;
    private final String inDir;
    private final String ruleID;
    public ObjectProcessor(Defects4jProject myObject, String inDir, String outDir, String ruleID) {
        this.myObject = myObject;
        this.inDir = inDir;
        this.outDir = outDir;
        this.ruleID = ruleID;
    }

    public Defects4jProject getMyObject() {
        return myObject;
    }

    //TODO: Add support for mulitple target lines
    public static void parse(String code, String dirPath, String outputdir, String ruleID, ArrayList targetLines) {
        //init a parser with JLS13 AST (Java 13)
        ASTParser parser = ASTParser.newParser(AST.JLS13);

        // Resolve binding
        parser.setResolveBindings(true);

        // Set kind if parser at compliation unit level
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        // allow binding recovery
        parser.setBindingsRecovery(true);

        // get default compiler options and set them as parser options
        Map<String, String> options = JavaCore.getOptions();
        parser.setCompilerOptions(options);

        String unitName = "Unit.java";// Just some random name.
        parser.setUnitName(unitName);

        //This code for setting environment. But we do not need for now
        String[] sources = {""};//Just the file itself.
        String[] classpath = {""};
        parser.setEnvironment(classpath, sources, new String[] { "UTF-8"}, true);

        parser.setSource(code.toCharArray());
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        //Create a document object of code
        Document document = new Document(code);
        cu.accept(RuleSelector.create(ruleID, cu, document, outputdir, targetLines));
        return;
    }
    @Override
    public String call() throws Exception {
        File buggyFile = new File(this.inDir + myObject.getId() + "_" + myObject.getSource_file() + "_" + myObject.getMethodName() + ".java");

        String filePath = buggyFile.getAbsolutePath();
//        System.out.println(filePath);

        //TODO: Add support for mulitple target lines
        ArrayList<Integer> targetLines = new ArrayList<>();
        for (Integer line: myObject.getLine_numbers()) {
            targetLines.add(line -  myObject.getMethodStartLine() + 3);
        }

        if(buggyFile.isFile()){
//            System.out.println("Current File is: " + filePath);
            try {
                // read code from cur file
                String codeOfCurFile = Utils.readFileToString(filePath);

                // output file
                String outputFile = Utils.sublizeOutput(filePath, inDir, outDir);

                // parser curfile
                parse(codeOfCurFile, inDir, outputFile, ruleID, targetLines);

            } catch (Exception e ) {
                System.out.println("YYYY");
                // TODO Auto-generated catch block
                e.printStackTrace();
                System.out.println("trans failed:	" + filePath);
            } catch (Error s) {
                // TODO Auto-generated catch block
                s.printStackTrace();
                System.out.println("trans failed:	" + s.toString());
            }
        }
        return "Processed: " + myObject.getId();
    }
}
class Defects4jProject {
    private int bug_id;
    private int id;
    private String subject;
    private String source_file;
    private List<Integer> line_numbers;
    private int methodStartLine;
    private int methodEndLine;
    private String methodName;

    public int getBug_id() {
        return bug_id;
    }

    public void setBug_id(int bug_id) {
        this.bug_id = bug_id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSource_file() {
        return source_file;
    }

    public void setSource_file(String source_file) {
        this.source_file = source_file;
    }

    public List<Integer> getLine_numbers() {
        return line_numbers;
    }

    public void setLine_numbers(List<Integer> line_numbers) {
        this.line_numbers = line_numbers;
    }

    public void setMethodStartLine(int methodStartLine) {
        this.methodStartLine = methodStartLine;
    }

    public void setMethodEndLine(int methodEndLine) {
        this.methodEndLine = methodEndLine;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public int getMethodStartLine() {
        return methodStartLine;
    }

    public int getMethodEndLine() {
        return methodEndLine;
    }

    public String getMethodName() {
        return methodName;
    }

    @Override
    public String toString() {
        return "{" +
                "bug_id: '" + this.bug_id + ", " +
                "id: '" + this.id + ", " +
                "subject: '" + this.subject + ", " +
                "source_file: '" + this.source_file + ", " +
                "line_number: '" + this.line_numbers.get(0) + ", " +
                "methodStartLine: '" + this.methodStartLine + ", " +
                "methodEndLine: '" + this.methodEndLine + ", " +
                ", methodName:" + methodName +
                '}';
    }
}

