import os
from utils.logger import logger
from utils.file_utils import read_dict_file, write_dict_file, readlines, read_file
import config
import glob
from tqdm import tqdm
from utils.code_utils import get_methods

class TrainDataset():
    '''
    This class is used to process the training dataset of ngram models, which is collected from 
    the paper "Natural software revisited" of Rahman et al. (ICSE 2019). 
    '''
    def __init__(
        self, name, tokenizer, data_dir, processed_dir, force_process=False
    ):
        self.name = name
        self.data_dir = data_dir
        self.processed_dir = processed_dir
        os.makedirs(self.processed_dir, exist_ok=True)
        self.tokenizer = tokenizer
        self.txt_path = os.path.join(self.processed_dir, f"{tokenizer.name}.txt")
        self.train_methods = []
        self.process(force_process)        
        
    def process_txt_data(self):
        logger.info(f"==> Tokenize data using {self.tokenizer.name}")
        data_dir = self.data_dir
        path = "{}/*/*.java".format(data_dir)
        f = open(self.txt_path, "w")
        cnt = 0
        idx = 0
        for file_name in tqdm(glob.glob(path)):
            full_code = read_file(file_name)
            methods = get_methods(full_code)
            for code in methods:
                tokens = self.tokenizer.tokenize(code)
                self.train_methods.append(["</s>"] + tokens + ["EOS"])
                f.write(" ".join(tokens) + "\n")
                idx += 1
                
        self.data_len = idx
        logger.info(f"==> Total data len: {self.data_len}")
        f.close()

    def process(self, force_process):
        logger.info(f"Processing full txt dataset...")
        if not force_process and os.path.exists(self.txt_path):
            logger.info(f"==> Full txt dataset exists. Skipping ...")
            methods = readlines(self.txt_path)
            for m in methods:
                self.train_methods.append(["</s>"] + m.split(" ") + ["<EOS>"])
            self.data_len = len(self.train_methods)
        else:
            self.process_txt_data()
        logger.info(f"Processing full txt dataset... Done!")


        
class TestDataset():
    '''
    This class is used to process the evaluating dataset. 
    '''
    def __init__(
        self, name, tokenizer, data_dir, vocab=None, force_process=False
    ):
        self.name = name
        self.data_dir = data_dir
        self.processed_dir = os.path.join("data/processed", self.name)
        os.makedirs(self.processed_dir, exist_ok=True)
        self.tokenizer = tokenizer
        self.txt_path = os.path.join(self.processed_dir, f"{tokenizer.name}.txt")
        self.id2idx_path = os.path.join(self.processed_dir, f"{tokenizer.name}.id2idx")
        self.test_methods = []
        self.vocab = vocab  
        self.process(force_process) 
        
             
        
    def process_txt_data(self):
        logger.info(f"==> Tokenize data using {self.tokenizer.name}")
        data_dir = self.data_dir
        path = "{}/*.java".format(data_dir)
        f = open(self.txt_path, "w")
        g = open(self.id2idx_path, "w" )
        idx = 0
        for file_name in tqdm(glob.glob(path)):
            file_id = file_name.split("/")[-1].split(".")[0]
            code = " ".join(readlines(file_name)[1:-1])
            tokens = self.tokenizer.tokenize(code)
            
            # need look up unknown tokens for ngram model
            if self.vocab is not None:
                tokens = list(self.vocab.lookup(tokens))
                
            self.test_methods.append(tokens)  
            f.write(" ".join(tokens) + "\n")
            idx += 1
            g.write(f"{file_id}\t{idx}\n")
                
        self.data_len = idx
        logger.info(f"==> Total data len: {self.data_len}")
        f.close()
        g.close()

    def process(self, force_process):
        logger.info(f"Processing full txt dataset...")
        if not force_process and os.path.exists(self.txt_path) and os.path.exists(self.id2idx_path):
            logger.info(f"==> Full txt dataset exists. Skipping ...")
            methods = readlines(self.txt_path)
            for m in methods:
                self.test_methods.append(m.split())
            self.data_len = len(self.test_methods)
        else:
            self.process_txt_data()
        logger.info(f"Processing full txt dataset... Done!")


        