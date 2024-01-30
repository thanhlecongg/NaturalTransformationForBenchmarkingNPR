from transformers import RobertaConfig, RobertaTokenizer, RobertaForMaskedLM, pipeline
from llm.llm import CodeBERTProbing

class LLMFactory(object):
    def create(llm_name, top_k):
        if llm_name == 'CodeBERT':
            return CodeBERTProbing(llm_name, top_k)
        else:
            raise ValueError('Invalid LLM name. Currently we only support [CodeBERT]')
