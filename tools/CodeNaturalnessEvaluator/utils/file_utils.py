import os
from utils.logger import logger
import json
import re
import pickle as pkl


def read_file(path, is_ignore=False):
    f =  open(path, 'r', encoding='utf-8', errors='ignore') if is_ignore else open(path, 'r', encoding='utf-8')
    content = f.read()
    f.close()
    return content

def read_file_without_nl(path, is_ignore=False):
    f =  open(path, 'r', encoding='utf-8', errors='ignore') if is_ignore else open(path, 'r', encoding='utf-8')
    content = f.read()
    content.replace("\n", " ")
    content = re.sub('\t| {4}', '', content)
    f.close()
    return content

def readlines(path, is_ignore=False):
    f =  open(path, 'r', encoding='utf-8', errors='ignore') if is_ignore else open(path, 'r', encoding='utf-8')
    content = f.readlines()
    f.close()
    return content

def write_txt_to_file(txt, path):
    with open(path, 'w') as f:
        f.write(txt)
        
def write_array_to_file(array, path):
    parent_dir = os.path.dirname(path)
    if not os.path.exists(parent_dir):
        os.makedirs(parent_dir)

    with open(path, 'w') as f:
        for item in array:
            f.write('%s\n' % item)
    
def write_dict_file(my_dict, path):
    with open(path, 'w') as f:
        json.dump(my_dict, f, indent=4)
        
def read_dict_file(path):
    with open(path, 'r') as f:
        return json.load(f)
        
def rm_file(file_path):
    if os.path.exists(file_path) and os.path.isfile(file_path):
        os.remove(file_path)
        logger.debug(f'rm file: {file_path}')
        
def write_array_to_pickle(path, data):
    with open(path, "wb") as f:
        pkl.dump(data, f)
        
def read_array_from_pickle(path):
    with open(path, "rb") as f:
        out = pkl.load(f)
    return out



