import re
from antlr.Java8Lexer import Java8Lexer
import random
import string
from spiral import ronin

def is_valid_variable_name(variable_name):
    # Regular expression pattern for valid Java variable names
    pattern = r'^[a-zA-Z_$][a-zA-Z_$0-9]*$'
    
    # Check if the variable name matches the pattern
    if re.match(pattern, variable_name) is not None:
        # Check if the variable name is not a Java keyword
        if variable_name not in ['true', 'false', 'null', 'this'] and variable_name not in Java8Lexer.literalNames:
            return True
    return False

def is_suitable(cand, existing_vars):
    return cand not in existing_vars and is_valid_variable_name(cand)


def gen_random_string(existing_var):
    # generate a random string of length 10 with letters and digits only
    random_string = ''.join(random.choices(string.ascii_letters + string.digits, k=10))

    # ensure the random string starts with a letter
    if random_string[0].isdigit():
        random_string = 'v' + random_string[1:]

    if not is_suitable(random_string, existing_var):
        return gen_random_string(existing_var)
    
    return random_string

def gen_first_all(cand, existing_var):
    sub_tokens = ronin.split(cand)
    if len(sub_tokens) == 1:
        return None
    
    new_var = ""
    for tok in sub_tokens:
        new_var += tok[0].lower()
        
    if is_suitable(new_var, existing_var):
        return new_var
    
    return None

