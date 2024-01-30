from llm.llm_factory import LLMFactory
from utils.logger import logger
import argparse
from utils.file_utils import write_file, read_file, read_lines
import os
from tqdm import tqdm
from antlr4 import *
from antlr.Java8Lexer import Java8Lexer
from utils.substitution_utils import is_suitable, gen_random_string, gen_first_all
import itertools

def antlr_tokenize(code):
    codeStream = InputStream(code)
    lexer = Java8Lexer(codeStream)
    tokens = lexer.getAllTokens()
    return [t.text for t in tokens]
 
def find_faulty_line(code_lines):
    start_idxs = []
    end_idxs = []
    for idx, line in enumerate(code_lines):
        if "//kXrgPH_C0deTransform_Start" in line:
            start_idxs.append(idx)
        if "//kXrgPH_C0deTransform_End" in line:
            end_idxs.append(idx)
            
    return start_idxs, end_idxs

def write_transformed_code(ori_code, var_set, curr_var, new_var, f_name, outDir):
    new_code = ori_code 
    for other_var in var_set:
        if other_var != curr_var:
            new_code = new_code.replace("___MASKED_" + other_var + "___", other_var)
    new_path = os.path.join(outDir, "{}_{}.java".format(f_name, curr_var))
    new_code = new_code.replace("___MASKED_" + curr_var + "___", new_var)  
    write_file(new_path, new_code)      
  
# TODO: Currently assume that only have one fault location ==> need to change to support multiple fault locations
def prepare_code(code_lines, ctx_size):
    
    start_idxs, end_idxs = find_faulty_line(code_lines)
    print(start_idxs)
    print(end_idxs)
    assert len(start_idxs) == 1 and len(end_idxs) == 1 and len(end_idxs) == len(start_idxs) #Currently assume that only have one fault location
    
    start_idx = start_idxs[0]
    end_idx = end_idxs[0]
    return code_lines[max(0, start_idx - ctx_size): start_idx] + code_lines[start_idx + 1: end_idx] + code_lines[end_idx + 2: min(end_idx + ctx_size + 1, len(code_lines))]

def probing(llm, inDir, outDir):
    top_k = 5
    outDirs = {}
    for i in range(top_k):
        outDirs[i] = os.path.join(outDir, "top_{}".format(i)) 
        os.makedirs(outDirs[i], exist_ok=True)

    model = LLMFactory.create(llm, top_k)
    files = [f for f in os.listdir(inDir) if f.endswith(".java")]
    cnt = 0
    for f in tqdm(files):
        f_name = f[:-5]    
        print("============================")
        print(f_name)
        path = os.path.join(inDir, f)
        logger.debug(path)
        new_path = os.path.join(outDir, f_name)
        ori_code_lines = read_lines(path)
        
        #Ignore the code do not have any masked variable
        if len(ori_code_lines) <= 3:
            cnt += 1
            continue
        
        ctx_code_lines = prepare_code(ori_code_lines, 3)
        ori_code = "".join(ori_code_lines)
        ctx_code = "".join(ctx_code_lines)
        print("-------------------------------------------")
        print(ctx_code)
        tokens = antlr_tokenize(ori_code)
    
        var_set = set()
        for _, t in enumerate(tokens):
            if t.startswith("___MASKED_"):
                var = t[10:-3]
                var_set.add(var)
                
        if len(var_set) == 0:
            continue
        
        logger.debug(var_set)
        substitute_candidate = {}
        existing_vars = set(tokens) | var_set

        for curr_var in var_set:
            print("CURRENT VAR: {}".format(curr_var))
            masked_code = ctx_code.replace("___MASKED_" + curr_var + "___", "<mask>")
            for other_var in var_set:
                if other_var != curr_var:
                    masked_code = masked_code.replace("___MASKED_" + other_var + "___", other_var)
            print("-------------------------------------------")
            print(masked_code)
            substitute_candidate[curr_var] = model.predict(masked_code, existing_vars)
            print("{}: {}".format(curr_var, substitute_candidate[curr_var]))
        
        for var, substutions in substitute_candidate.items():            
            for idx, sub in enumerate(substutions):
                write_transformed_code(ori_code, var_set, var, sub, f_name, outDirs[idx])

def generate_arrays(k, t):
    digits = range(t)  # Generate digits from 0 to j-1
    arrays = itertools.product(digits, repeat=k)  # Generate all possible combinations
    return list(arrays)

def generate_combinations(substitute_candidate):
    list_keys = list(substitute_candidate.keys())
    number_of_vars = len(list_keys)
    number_of_subs = len(list(substitute_candidate.values())[0])
    valid_combinations = []
    for combo in generate_arrays(number_of_vars, number_of_subs):
        substitute_combinations = {}
        existing_subs = []
        is_valid = True
        for idx, key in enumerate(list_keys):
            curr_sub = substitute_candidate[key][combo[idx]]
            if curr_sub in existing_subs:
                is_valid = False
                break
            existing_subs.append(curr_sub)
            substitute_combinations[key] = curr_sub
        if is_valid:
            valid_combinations.append([substitute_combinations, sum(combo)])
    
    sorted_combinations = sorted(valid_combinations, key=lambda x: x[1])
    return sorted_combinations
    
    
    
            
def probing_all(llm, inDir, outDir):
    top_k = 5
    outDirs = {}
    for i in range(top_k):
        outDirs[i] = os.path.join(outDir, "top_{}".format(i)) 
        os.makedirs(outDirs[i], exist_ok=True)

    model = LLMFactory.create(llm, top_k)
    files = [f for f in os.listdir(inDir) if f.endswith(".java")]
    cnt = 0

    for f in tqdm(files):
        f_name = f[:-5]    
        print("============================")
        print(f_name)
        path = os.path.join(inDir, f)
        logger.debug(path)
        new_path = os.path.join(outDir, f_name)
        ori_code_lines = read_lines(path)
        
        #Ignore the code do not have any masked variable
        if len(ori_code_lines) <= 3:
            cnt += 1
            continue
        
        ctx_code_lines = prepare_code(ori_code_lines, 3)
        ori_code = "".join(ori_code_lines)
        ctx_code = "".join(ctx_code_lines)
        print("-------------------------------------------")
        print(ctx_code)
        tokens = antlr_tokenize(ori_code)
    
        var_set = set()
        for _, t in enumerate(tokens):
            if t.startswith("___MASKED_"):
                var = t[10:-3]
                var_set.add(var)
                
        if len(var_set) == 0:
            continue
        
        logger.debug(var_set)
        substitute_candidate = {}
        existing_vars = set(tokens) | var_set

        for curr_var in var_set:
            print("CURRENT VAR: {}".format(curr_var))
            masked_code = ctx_code.replace("___MASKED_" + curr_var + "___", "<mask>")
            for other_var in var_set:
                if other_var != curr_var:
                    masked_code = masked_code.replace("___MASKED_" + other_var + "___", other_var)
            print("-------------------------------------------")
            print(masked_code)
            substitute_candidate[curr_var] = model.predict(masked_code, existing_vars)
            print("{}: {}".format(curr_var, substitute_candidate[curr_var]))
        
        combinations = generate_combinations(substitute_candidate)
        print(combinations)
        for idx in range(5):
            subs = combinations[idx][0]
            new_code = ori_code 
            for var in var_set:
                new_code = new_code.replace("___MASKED_" + var + "___", subs[var])
            new_path = os.path.join(outDirs[idx], "{}.java".format(f_name))
            write_file(new_path, new_code)    
    
def first_letter_first_fill(inDir, outDir):
    files = [f for f in os.listdir(inDir) if f.endswith(".java")]
    for f in tqdm(files):
        path = os.path.join(inDir, f)
        logger.debug(path)
        f_name = f[:-5]
                
        ori_code = read_file(path)
        tokens = antlr_tokenize(ori_code)
        
        var_set = set()
        for _, t in enumerate(tokens):
            if t.startswith("___MASKED_"):
                var = t[10:-3]
                var_set.add(var)

        logger.debug(var_set)
        
        existing_vars = set(tokens) | var_set
        
        for var in var_set:
            new_var = var[0].lower()
            if is_suitable(new_var, existing_vars):
                write_transformed_code(ori_code, var_set, var, new_var, f_name, outDir)

def first_letter_all_fill(inDir, outDir):
    files = [f for f in os.listdir(inDir) if f.endswith(".java")]
    for f in tqdm(files):
        path = os.path.join(inDir, f)
        logger.debug(path)
        f_name = f[:-5]           
        
        ori_code = read_file(path)
        tokens = antlr_tokenize(ori_code)
        
        var_set = set()
        for _, t in enumerate(tokens):
            if t.startswith("___MASKED_"):
                var = t[10:-3]
                var_set.add(var)

        logger.debug(var_set)
        
        existing_vars = set(tokens) | var_set
                
        for var in var_set: 
            new_var = gen_first_all(var, existing_vars)
            if new_var is not None:
                write_transformed_code(ori_code, var_set, var, new_var, f_name, outDir)
                
            
def lowercase_first_letter(string):
    return string[:1].lower() + string[1:]

def lowercase_fill(inDir, outDir):
    files = [f for f in os.listdir(inDir) if f.endswith(".java")]
    for f in tqdm(files):
        f_name = f[:-5]           
        path = os.path.join(inDir, f)
        logger.debug(path)
        new_path = os.path.join(outDir, f)
        if os.path.exists(new_path):
            continue
        
        ori_code = read_file(path)
        tokens = antlr_tokenize(ori_code)
        
        var_set = set()
        for _, t in enumerate(tokens):
            if t.startswith("___MASKED_"):
                var = t[10:-3]
                var_set.add(var)

        logger.debug(var_set)
        
        existing_vars = set(tokens) | var_set
        
        is_write = False
        for var in var_set: 
            new_var = lowercase_first_letter(var)
            if new_var != var and is_suitable(new_var, existing_vars):
                write_transformed_code(ori_code, var_set, var, new_var, f_name, outDir)
        
            
def random_fill(inDir, outDir):
    
    files = [f for f in os.listdir(inDir) if f.endswith(".java")]
    
    for f in tqdm(files):
        path = os.path.join(inDir, f)
        
        logger.debug(path)
        
        new_path = os.path.join(outDir, f)
        
        if os.path.exists(new_path):
            continue
        
        ori_code = read_file(path)

        tokens = antlr_tokenize(ori_code)
        
        var_set = set()
        for _, t in enumerate(tokens):
            if t.startswith("___MASKED_"):
                var = t[10:-3]
                var_set.add(var)

        logger.debug(var_set)
        
        existing_vars = set(tokens) | var_set
        
        new_code = ori_code
        for var in var_set: 
            new_var = gen_random_string(existing_vars)
            new_code = new_code.replace("___MASKED_" + var + "___", new_var)  

        write_file(new_path, new_code)
        
def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-i', '--inDir', default="data/defects4j/masked_methods/2", type=str)
    parser.add_argument('-o', '--outDir', default="data/defects4j/transformed_methods/2", type=str)
    parser.add_argument('-f', '--fillType', default="probing", type=str)
    parser.add_argument('-l', '--llm', default="CodeBERT", type=str)

    return parser.parse_args()
                        
def main():
    logger.info("Starting Fill Masks")
    args = parse_args()
    logger.info("==> Input Directory: " + args.inDir)
    outDir = os.path.join(args.outDir, args.fillType)
    os.makedirs(outDir, exist_ok=True)
    if args.fillType == "probing":
        logger.info("Fill Masks using Probing")
        if args.llm != None:
            probing(args.llm, args.inDir, outDir)
        else:
            logger.error("Missing llm argument. Please specify with -l or --llm with the name of the LLM. Currently, we supported: CodeBERT")
    elif args.fillType == "probing_all":
        logger.info("Fill Masks using Probing")
        if args.llm != None:
            probing_all(args.llm, args.inDir, outDir)
        else:
            logger.error("Missing llm argument. Please specify with -l or --llm with the name of the LLM. Currently, we supported: CodeBERT")
    elif args.fillType == "random":
        random_fill(args.inDir, outDir)
    elif args.fillType == "first":
        first_letter_first_fill(args.inDir, outDir)
    elif args.fillType == "first_all":
        first_letter_all_fill(args.inDir, outDir)
    elif args.fillType == "lower":
        lowercase_fill(args.inDir, outDir)

if __name__ == "__main__":
    main()
