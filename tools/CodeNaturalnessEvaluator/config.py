import torch

#Model version:
_MODEL_VERSION = {
                    "gptneo": "EleutherAI/gpt-neo-2.7B",
                    "codellama": "codellama/CodeLlama-7b-hf",
                    "bloom": "bigscience/bloomz-7b1",
                    "codellama13": "codellama/CodeLlama-13b-hf",
                    "codellama34": "codellama/CodeLlama-34b-hf",
                  }

#General Configurations
UNK = "<UNK>"
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

model_dir = "data/models/"
#N-Gram Configurations
ngram_order = 4
processed_dir = "data/processed/train"
train_data_dir = "data/raw/rahman19/Projects"