# Code Transformation Tool
This repository implement a tool that transforms Java code snippets for generating semantic-preserving programs. A part of this repository is built upon [SPAT](https://github.com/Santiago-Yu/SPAT).

## Environment and Setup

### IDE
This project is developed using Java 8 in [IntelliJ IDEA Ultimate](https://www.jetbrains.com/idea/business/). If you wish to experiment with the code, please ensure you're using the same Java version and IDE. I intend to refactor this code using Maven for enhanced convenience and reliability when my schedule permits. If you've already done this, a pull request would be greatly appreciated.

### Build & Dependencies

### For Java (module `core`)
To build the core module from scratch, please do following steps:

- Download dependencies from the [link](https://figshare.com/s/05c50e7e0bd021ed16b3)
- Import module dependencies following [IntelliJ guidelines](https://www.jetbrains.com/help/idea/working-with-module-dependencies.html)
- Build artifact following [IntelliJ guidelines](https://www.jetbrains.com/help/idea/compiling-applications.html#package_into_jar)

### For Python
Please use `enviroment.yml` for creating `conda` enviroment

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

## Note
Currently, this tool is not fully automated and might necessitate some manual efforts. Specifically, the `fill_mask` post-processing stage relies on signatures to pinpoint the modified faulty line. Regrettably, the core engine occasionally omits these signatures, resulting in errors. To mitigate this, I've manually added the missing signatures when conducting experiments as there are only not many cases. I plan to rectify this issue asap to achieve a fully automated solution once my schedule allows. 
