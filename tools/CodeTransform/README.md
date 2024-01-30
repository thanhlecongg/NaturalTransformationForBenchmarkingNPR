# Code Transformation Tool
This repository implement a tool that transforms Java code snippets for generating semantic-preserving programs. A part of this repository is built upon [SPAT](https://github.com/Santiago-Yu/SPAT).

## Environment and Setup

### IDE
This project is develop using Java 8 in [IntelliJ IDEA Ultimate](https://www.jetbrains.com/idea/business/). If you want to play with the code, please use the same Java version and IDE. Otherwise, you can use our executable package. 

### Build & Dependencies

To build this repository from scratch, please do following steps:

- Download dependencies from the [link]()
- Import module dependencies following [IntelliJ guidelines](https://www.jetbrains.com/help/idea/working-with-module-dependencies.html)
- Build artifact following [IntelliJ guidelines](https://www.jetbrains.com/help/idea/compiling-applications.html#package_into_jar)

### Executable package
Our executable package with dependencies can be found in this [link]()

## Usage
To run our tool for Defects4J dataset, please do the following steps:

```
1. Preprocessing: This step happens on `preprocess` directory including the following sub-steps:
    
    1.1. python3 -m preprocess.defects4j_checkout // Download Defects4J projects
    1.2. python3 -m preprocess.extract_buggy_files // Extract buggy files
    1.3. Run `src/Main.java`  using IntelliJ // Extract buggy methods
    1.4. python3 -m preprocess.insert_signatures // Inserts signatures to denote buggy lines

2. Transform: This step happens on `core` directory
    2.1. Config neccessary info in `src/Config.java` //Config
    2.2. Run `src/Main.java` using IntelliJ //Transform original code to masked code

3. Postprocessing: This step happens on `postprocess` directory
    3.1. python3 -m postprocess.fill_mask.py // Fill masked code by recommendations from LLMs ==> See option within code
    3.2. python3 -m postprocess.apply_transform //Apply transformed methods to original files + update associating metadata
```

