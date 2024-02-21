## Evaluating Program Repair with Semantic-Preserving Transformations: A Naturalness Assessment

This repository contains the data and code for the paper "Evaluating Program Repair with Semantic-Preserving Transformations: A Naturalness Assessment" (submitted to ACM Transactions on Software Engineering and Methodology).
### Data
Our data is published using Figshare, please download data from [here](https://figshare.com/s/05c50e7e0bd021ed16b3) and put it into the folder data before running experiments.

### Replicating results in the Paper
To replicate results of our RQ1, please use the following command:
```
python3 rq1.py 
```
To replicate results of our RQ2, please use the following command:
```
python3 rq2_1.py 
python3 rq2_2.py 
```
To replicate results of our RQ3, please use the following command:
```
python3 rq3.py 
```

### Supplementary Materials
#### Human Study Data
Data collected from our human study is in the `human_study` folder. Particularly:
- Interview:
    - Transcripts: `data/human_study/interview/Transcript`
    - Themes with their associated main themes: `data/human_study/interview/Final_Themes.xlsx`
    - Card Sorting Discussion Resulst: `data/human_study/interview/A1(A2)_Categories.txt`
- Survey:
    - Raw Data: `data/human_study/survey/survey.json`
    - Example: 
    ```
    "naming": { // transformation levels
        "1": { // ID of transformation in this level
            "S9": { // ID of the participant
                "CR": 3, // Assessment for Code Readability
                "CC": 1, // Assessment for Code Convention
                "Time": 10.56 // Completion Time
            },
            ...
        },
        ...
    }
    ```
### List of Defects4J bugs used in this study:
In this work, we used the following 225 bugs from the Defects4J dataset:
```
    - Chart: 1, 3, 6, 8, 9, 10, 11, 12, 13, 17, 20, 24
    - Cli: 4, 5, 8, 11, 25, 32
    - Closure: 10, 11, 14, 18, 20, 35, 38, 46, 51, 52, 55, 57, 62, 65, 70, 71, 73, 77, 81, 83, 92, 97, 104, 109, 111, 113, 122, 123, 124, 125, 126, 130, 132, 133, 150, 152, 159, 168 
    - Codec: 2, 3, 7, 9, 10, 17, 18 
    - Compress: 5, 12, 13, 14, 19, 23, 26, 27, 31, 36, 37, 38, 45, 46
    - Csv: 1, 2, 3, 5, 6, 9, 11, 14, 15
    - Gson: 6, 10, 11, 12, 13, 15, 17 
    - JacksonCore: 3, 4, 5, 6, 8, 25, 26 
    - JacksonDatabind: 5, 12, 16, 17, 19, 27, 33, 34, 37, 39, 45, 46, 49, 51, 57, 58, 70, 71, 76, 82, 88, 93, 96, 97, 98, 99, 102 
    - JacksonXml: 4, 5
    - Jsoup: 1, 10, 13, 19, 26, 27, 32, 33, 34, 37, 40, 41, 43, 45, 46, 47, 49, 51, 54, 57, 61, 68, 75, 77, 84, 86
    - JxPath: 5, 8, 10, 12
    - Lang: 6, 9, 14, 16, 21, 22, 24, 26, 28, 29, 33, 37, 38, 39, 40, 43, 44, 49, 52, 54, 57, 58, 59, 61
    - Math: 9, 11, 17, 30, 32, 33, 41, 45, 50, 53, 56, 57, 58, 59, 63, 69, 70, 75, 80, 82, 85, 89, 91, 94, 96, 101
    - Mockito: 5, 12, 18, 22, 27, 28, 29, 33, 34, 38
    - Time: 4, 14, 15, 16, 19, 24
```

#### Repair Data
Data collected from our repair experiments is in the `data/plausible_patches` folder. Particularly:
- Naming Format: `{transformation_level}-{repair_tool}.xlsx`
- Columns in this data:
    - ID: ID of the transformation
    - Bug_id: ID of the original bug in Defects4J
    - "generated_diff": the generated patch by repair tool
    - "developer_diff": the patch written by developers extracted from Defects4J dataset
    - "Annotation": Correctness Assessment (yes is correct, no is plausible)
    - Any ID do not exists in this data means that repair tool do not provide any plausible patch, a.k.a, wrong patch quality.
- This results are obtained by running [Cerberus](https://github.com/nus-apr/cerberus) (SHA: baed4074cdc1b0ff6b6c99619dbe70f508ec4004, dev-branch) on repair dataset in `data/repair_dataset`. Please following instructions in Cerberus and using configurations presented in the paper to reproduce these results.

#### Transformations Data
Our transformation data is stored in `data/repair_dataset/naturaltransform`. This dataset is generated based on our tool, CodeTransform `tools/CodeTransform` which is extended based on [SPAT](https://github.com/Santiago-Yu/SPAT). Please following the instructions in `tools/CodeTransform/README.md` to reproduce this dataset. 

#### Naturalness Evaluation
Cross-Entropy values for original and transformed programs are stored in `data/entropy`. These results are generated using our tool CodeNaturalnessEvaluator `tools/CodeNaturalnessEvaluator`. Please following the instructions in `tools/CodeNaturalnessEvaluator/README.md` to reproduce these results.
