from abc import ABC, abstractmethod
from transformers import RobertaTokenizer, RobertaForMaskedLM, pipeline
from utils.substitution_utils import is_suitable

class LLMProbing(ABC):
    def __init__(self, name, top_k):
        self.name = name
        self.top_k = top_k


    @abstractmethod
    def predict(self, code):
        pass


class CodeBERTProbing(LLMProbing):
    def __init__(self, name, top_k):
        super().__init__(name, top_k)
        self.model = RobertaForMaskedLM.from_pretrained("microsoft/codebert-base-mlm")
        self.tokenizer = RobertaTokenizer.from_pretrained("microsoft/codebert-base-mlm")
        self.fill_mask = pipeline('fill-mask', model=self.model, tokenizer=self.tokenizer, top_k=50)
    
    def predict(self, code, existing_var = []):
        outputs = self.fill_mask(code)

        try:
            candidates = [item['token_str'].strip() for item in outputs]
            filtered_candidates = []
            for cand in candidates:
                if cand not in filtered_candidates and is_suitable(cand, existing_var):
                    filtered_candidates.append(cand)
            return filtered_candidates[:5]
        except:
            occurences = {}
            
            for i in range(len(outputs)):
                for j in range(len(outputs[i])):
                    if outputs[i][j]["token_str"].strip() not in occurences:
                        occurences[outputs[i][j]["token_str"].strip()] = outputs[i][j]["score"] 
                    else:
                        occurences[outputs[i][j]["token_str"].strip()] += outputs[i][j]["score"] 

            sorted_occurences = dict(sorted(occurences.items(), key=lambda x: x[1], reverse=True))
            
            filtered_occurences = {}
            for cand in sorted_occurences:
                if is_suitable(cand, existing_var):
                    filtered_occurences[cand] = sorted_occurences[cand]
            
            return list(filtered_occurences.keys())[:self.top_k]



