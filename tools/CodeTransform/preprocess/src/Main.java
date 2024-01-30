import com.google.gson.GsonBuilder;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {
    public static void main(String[] args) {
        try (Reader reader = new FileReader("../data/defects4j/meta-data.json")) {
            Gson gson = new Gson();

            Type listType = new TypeToken<List<Defects4jProject>>() {}.getType();

            List<Defects4jProject> projects = gson.fromJson(reader, listType);
            List<Defects4jProject> selectedProjects = new ArrayList<>();
            int cnt = 0;
            for (Defects4jProject prj : projects) {
                if(!prj.getLine_numbers().isEmpty()) {
                    prj = findMethod(prj);
                    selectedProjects.add(prj);
                    cnt += 1;
                }
            }
            System.out.println("Total " + String.valueOf(cnt) + " selected bugs");

            Gson write_gson = new GsonBuilder().setPrettyPrinting().create();
            String json = write_gson.toJson(selectedProjects);

            // Write the JSON to a file
            try (FileWriter writer = new FileWriter("../data/defects4j/buggy_method_info.json")) {
                writer.write(json);
                System.out.println("JSON file created successfully.");
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static String readFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        byte[] bytes = Files.readAllBytes(path);
        return new String(bytes);
    }
    private static List<String> readLines(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return Files.readAllLines(path);
    }
    //TODO: Add support for multiple lines. Currently, we only support single line.
    private static Defects4jProject findMethod(Defects4jProject project){
        try {
            //TODO: Add support for multiple lines. Currently, we only support single line as the data only contains single lines
            int targetLine = project.getLine_numbers().get(0);
            String buggyPath = "../data/defects4j/buggy_files/" + String.valueOf(project.getId()) + "/" + project.getSource_file() + ".java";
            System.out.println("Working on " + buggyPath);

            String javaCode = readFile(buggyPath);
            ASTParser parser = ASTParser.newParser(AST.JLS13);
            parser.setSource(javaCode.toCharArray());
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            Map<String, String> options = JavaCore.getOptions();
            parser.setCompilerOptions(options);
            CompilationUnit cu = (CompilationUnit) parser.createAST(null);
            MethodVisitor visitor = new MethodVisitor(targetLine);
            visitor.setCompilationUnit(cu);
            cu.accept(visitor);
            project.setMethodStartLine(visitor.startLine);
            project.setMethodEndLine(visitor.endLine);
            project.setMethodName(visitor.methodName);

            List<String> javaCodeLines = readLines(buggyPath);
            List<String> buggyMethodCodeLines = IntStream.rangeClosed(visitor.startLine - 1, visitor.endLine - 1)
                    .filter(i -> i >= 0 && i < javaCodeLines.size())
                    .mapToObj(javaCodeLines::get)
                    .collect(Collectors.toList());

            String buggyMethodCode = String.join(System.lineSeparator(), buggyMethodCodeLines);
            buggyMethodCode = "public class Main {\n" + buggyMethodCode + "\n}";
            String outputFilePath = "../data/defects4j/buggy_methods/" + String.valueOf(project.getId()) + "_" + project.getSource_file() + "_" + visitor.methodName + ".java";
            try {
                File file = new File(outputFilePath);
                file.getParentFile().mkdirs(); // Create the parent directory if it doesn't exist

                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                writer.write(buggyMethodCode);
                writer.close();

                System.out.println("String written to file successfully.");
            } catch (IOException e) {
                System.out.println("An error occurred while writing to file: " + e.getMessage());
            }

            return project;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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

class MethodVisitor extends ASTVisitor {
    private int targetLine;
    public int startLine;
    public int endLine;
    public String methodName;
    private CompilationUnit cu;

    public MethodVisitor(int targetLine) {
        this.targetLine = targetLine;
    }

    public void setCompilationUnit(CompilationUnit cu) {
        this.cu = cu;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        int startLine = cu.getLineNumber(node.getStartPosition());
        int endLine = cu.getLineNumber(node.getStartPosition() + node.getLength());

        if (targetLine >= startLine && targetLine <= endLine) {
            System.out.println("Found method: " + node.getName().getIdentifier() + "in position (" + startLine + ", " + endLine +")" + " for target line " + targetLine);
            this.startLine = startLine;
            this.endLine = endLine;
            this.methodName = node.getName().getIdentifier();
        }

        return super.visit(node);
    }
}
