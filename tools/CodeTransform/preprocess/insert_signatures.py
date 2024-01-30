import json
import os 

_METHOD_PATH = "data/defects4j/buggy_methods/{}_{}_{}.java"
with open("data/defects4j/buggy_method_info.json", "r") as json_file:
    buggy_method_info = json.load(json_file)

_NEW_METHOD_PATH = "data/defects4j/buggy_methods_signed/{}_{}_{}.java"
os.makedirs("data/defects4j/buggy_methods_signed/", exist_ok=True)

for method_info in buggy_method_info:
    if method_info is None or "methodName" not in method_info:
        continue
        
    path = _METHOD_PATH.format(method_info["id"], method_info["source_file"], method_info["methodName"])
    with open(path, 'r') as f:
        code_lines = f.readlines()
        fault_locations = [line - method_info["methodStartLine"] + 1 for line in method_info["line_numbers"]]
        indentation_count = len(code_lines[fault_locations[0]]) - len(code_lines[fault_locations[0]].lstrip())
        code_lines.insert(fault_locations[0], " " * indentation_count + "//kXrgPH_C0deTransform_Start\n") #kXrgPH_C0deTransform_Start just a random string
        code_lines.insert(fault_locations[-1] + 2, " " * indentation_count + "//kXrgPH_C0deTransform_End\n") #kXrgPH_C0deTransform_End just a random string
    
    with open(_NEW_METHOD_PATH.format(method_info["id"], method_info["source_file"], method_info["methodName"]), 'w') as f:
        f.write("".join(code_lines))

