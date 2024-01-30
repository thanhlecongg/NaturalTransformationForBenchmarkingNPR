package transform;

import java.io.File;
import java.nio.file.Files;

public class Config {
    //Dir
    public static String inDir = "../data/defects4j/buggy_methods_signed/";
    public static String ruleID = "5";

    public static String outDir = "../data/defects4j/masked_methods/" + ruleID + "/";

    public static String infoDir = "../data/defects4j/buggy_method_info.json";

    //Parameter
    public static int maxTrans = 5;
    public static int randomSeed = 42;
}
