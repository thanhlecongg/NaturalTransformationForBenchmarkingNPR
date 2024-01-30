
from utils.logger import logger
import subprocess
import time
import json

def write_file(file_name, data):
    with open(file_name, 'w') as f:
        f.write(data)
        

def read_file(file_name):
    with open(file_name, 'r') as f:
        return f.read()

def read_lines(file_name):
    with open(file_name, 'r') as f:
        return f.readlines()
    
def read_json_file(file_name):
    with open(file_name, 'r') as json_file:
        json_data = json_file.read()

    # Parse the JSON data
    data = json.loads(json_data)
    
    return data