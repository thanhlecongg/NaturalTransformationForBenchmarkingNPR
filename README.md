## Robustness meets Naturalness: An Empirical Study on Neural Program Repair

This repository contains the data and code for the paper "Robustness meets Naturalness: An Empirical Study on Neural Program Repair" (submitted to FSE2024).

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
Our transformation data is stored in `data/repair_dataset/naturaltransform`. This dataset is generated based on our tool, CodeTransform `tools/CodeTransform` which is built upon [SPAT](https://github.com/Santiago-Yu/SPAT). Please following the instructions in `tools/CodeTransform/README.md` to reproduce this dataset. 

#### Naturalness Evaluation
Cross-Entropy values for original and transformed programs are stored in `data/entropy`. These results are generated using our tool CodeNaturalnessEvaluator `tools/CodeNaturalnessEvaluator`. Please following the instructions in `tools/CodeNaturalnessEvaluator/README.md` to reproduce these results.
