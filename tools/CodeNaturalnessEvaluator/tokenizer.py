from antlr4 import *
from antlr.Java8Lexer import Java8Lexer
from abc import ABC, abstractmethod
from transformers import AutoTokenizer


        
class Tokenizer(ABC):
    def __init__(self, name):
        self.name = name
    
    @abstractmethod
    def tokenize(self, code):
        pass
    
class ANTLR_Tokenizer(Tokenizer):
    def __init__(self, name):
        super().__init__(name)
        
        
    def tokenize(self, code):
        codeStream = InputStream(code)
        lexer = Java8Lexer(codeStream)
        tokens = lexer.getAllTokens()
        return [t.text for t in tokens]
   
class BPETokenizer(Tokenizer):
    def __init__(self, name, model_version):
        self.name = name
        self.model_version = model_version
        self.tokenizer = AutoTokenizer.from_pretrained(self.model_version)

    def tokenize(self, code):
        tokens = self.tokenizer.tokenize(code)
        return tokens

