import argparse
from utils.logger import logger
import datetime
import logging
from model import *
from tokenizer import *
from dataset import *
import config
from tqdm import tqdm
import copy

class TokenizerFactory:
    def create_tokenizer(tokenizer_name):
        if tokenizer_name == 'antlr':
            return ANTLR_Tokenizer(tokenizer_name)
        elif tokenizer_name in config._MODEL_VERSION.keys():
            return BPETokenizer(tokenizer_name, config._MODEL_VERSION[tokenizer_name])
        else:
            raise ValueError('Invalid tokenizer name. Currently we only support ANTLR and BPETokenizer with the following models: {}'.format(config._MODEL_VERSION.keys()))
        
class ModelFactory:
    def create_model(model_name, tokenizer_name):
        if model_name == 'ngram':
            if tokenizer_name is None:
                tokenizer = TokenizerFactory.create_tokenizer('antlr')
            else:
                tokenizer = TokenizerFactory.create_tokenizer(tokenizer_name)
            train_data = TrainDataset('ngram_train', tokenizer, config.train_data_dir, config.processed_dir, force_process=False)
            model = NGram("{}_{}".format(model_name, tokenizer.name), train_data, config.ngram_order)
            return model, tokenizer
        elif model_name in config._MODEL_VERSION.keys():
            model_version = config._MODEL_VERSION[model_name]
            if tokenizer_name is None:
                tokenizer = TokenizerFactory.create_tokenizer(model_name)
            else:
                tokenizer = TokenizerFactory.create_tokenizer(tokenizer_name)   
                raise Warning("CodeGPT should use default tokenizer. Your are using {}".format(tokenizer_name)) 
            model = LLM("{}_{}".format(model_name, tokenizer.name), model_version, tokenizer.tokenizer)
            return model, tokenizer
        else:
            raise ValueError('Invalid model name. Currently we only support N-gram and the following LLMs: {}'.format(list(config._MODEL_VERSION.keys())))

def parse_args():
    parser = argparse.ArgumentParser(description="Curator: Code Naturalness Evaluator")
    parser.add_argument('-m', '--model', type=str, default="ngram", help='Model used to evaluate code naturalness', choices=['ngram', 'gptneo', 'codellama', 'bloom', 'codellama13', 'codellama34'])
    parser.add_argument('-t', '--test_dir', type=str, default="data/raw/methods/original", help='Path to test data')
    parser.add_argument('-n', '--test_name', type=str, default="original", help='Name of test data')
    parser.add_argument('-tk', '--tokenizer', type=str, default=None, help='Name of test data. Please leave it blank if you want to use default tokenizer', choices=['antlr', 'gptneo', 'codellama', 'bloom', 'codellama13'])
    parser.add_argument('-s', '--only_setup', action='store_true', help='If only setup')

    return parser.parse_args()

def main(args): 
    #Prepare logger
    logger.info("Curator is running ...")
    now = datetime.datetime.now()
    model_name = args.model
    tokenizer_name = args.tokenizer
    logfile_name = now.strftime("%Y-%m-%d")
    file_handler = logging.FileHandler(f'logs/{model_name}_{logfile_name}.log')
    logger.addHandler(file_handler)
    
    #Prepare model
    logger.info("Preparing model {} ...".format(model_name))
    model, tokenizer = ModelFactory.create_model(model_name, tokenizer_name)

    test_data = TestDataset(args.test_name, tokenizer, args.test_dir, None, force_process=False)
    
    logger.info("Preparing model {} ... Done!".format(model_name))  
    
    if args.only_setup:
        logger.info("Setup successfully")  
        exit()  
    
    if tokenizer_name is None:
        result_file_name = "results/{}_{}_{}.txt".format(model_name, args.test_name, "default")
    else:
        result_file_name = "results/{}_{}_{}.txt".format(model_name, args.test_name, tokenizer_name)

    f = open(result_file_name, "w")
    for index, method_tokens in enumerate(tqdm(test_data.test_methods)):
        try:
            ce = model.entropy(method_tokens)
        except Exception as e:
            logger.error("OOM at index {}: {}".format(index, e))
            ce = None
            exit()
        f.write("{}\t{}\n".format(index, ce))  # Write the index and ce value
    f.close()
    logger.info("Evaluation successfully. Result is saved at {}".format(result_file_name))
if __name__ == "__main__":
    args = parse_args()
    main(args)
