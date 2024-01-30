# Cde NatURAlness EvaluaTOR

This repository contains source code for Cde NatURAlness EvaluaTOR, a tool for evaluating the naturalness of source code.

## Environment Configuration
```
conda create --name [env_name] --file requirements.txt
```
## Usage

Please refer to the following command for usage:
```
python3 main.py -m  [LM model: ngram, gptneo, codellama, bloom, codellama13]
                -t  [path to test directory]
                -n  [name of experiment]
                -tk [tokenizer: gpt2, gptneo, codellama, bloom, codellama13]
                -s  [if you only want to setup]
```

The result of experiment will be stored at `./result/[model]_[name of experiment]_[tokenizer].txt`.