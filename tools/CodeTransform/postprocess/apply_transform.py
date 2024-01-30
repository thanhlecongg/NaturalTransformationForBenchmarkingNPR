import json
import os
import glob
import argparse
from tqdm import tqdm
def find_faulty_line(code_lines):
    start_idxs = []
    end_idxs = []
    for idx, line in enumerate(code_lines):
        if "//kXrgPH_C0deTransform_Start" in line:
            start_idxs.append(idx)
        if "//kXrgPH_C0deTransform_End" in line:
            end_idxs.append(idx)
    assert len(start_idxs) == 1 
    assert len(end_idxs) == 1
    return start_idxs, end_idxs

def get_sub_transform(transform):
    if transform == "naming":
        return [
            "1/first",
            "1/first_all",
            "1/probing/top_0",
            "1/probing/top_1",
            "1/probing/top_2"
        ]
    elif transform == "statement":
        return [
            "3/default",
            "4/default",
            "5/default",
            "6/default",
            "2/probing_all/top_0",
            "2/probing_all/top_1",
            "2/probing_all/top_2",
            "7/default",
            "12/default",
            "13/default",
            "14/default",
            "15/default",
            "16/default",
            "18/default",
            "19/default",
            "20/default"
        ]
    elif transform == "expression":
        return [
            "23/default",
            "22/default",
            "21/default",
            "17/probing_all/top_0",
            "17/probing_all/top_1",
            "17/probing_all/top_2",
            "10/default",
            "11/default",
            "8/default",
            "9/default"
        ]
    else:
        raise("Wrong Transformation Category")
def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-tm', '--transMethodDir', default="data/defects4j/transformed_methods/", type=str)
    parser.add_argument('-t', '--transform', default="naming", type=str)
    parser.add_argument('-b', '--bugDir', default="data/defects4j/buggy_files", type=str)
    parser.add_argument('-o', '--outDir', default="data/defects4j/transforms/", type=str)
    parser.add_argument('-m', '--metaDir', default="data/defects4j/meta-data.json", type=str)
    parser.add_argument('-mm', '--methodMetaDir', default="data/defects4j/buggy_method_info.json", type=str)

    return parser.parse_args()

def create_missing_directories(path):
    directories = path.split('/')
    current_path = directories[0]  # Start with the root directory
    for directory in directories[1:]:
        current_path = os.path.join(current_path, directory)
        if not os.path.exists(current_path):
            os.mkdir(current_path)
            
def main():
    args = parse_args()
    newMetaDir = os.path.join(args.outDir, args.transform, "meta-data.json")
    
    with open(args.metaDir, "r") as json_file:
        meta_data = json.load(json_file)

    with open(args.methodMetaDir, "r") as json_file:
        buggy_method_info = json.load(json_file)

    new_meta_data = []
    list_bugs = []
    new_idx = 0
    
    sub_transforms = get_sub_transform(args.transform)
    
    count = {}
    for sub_transform in sub_transforms:
        for idx, method_info in tqdm(enumerate(buggy_method_info)):
            if method_info is None:
                continue
            meta = meta_data[method_info["id"] - 1]
            assert method_info["id"] == meta["id"]
            assert method_info["subject"] == meta["subject"]
            assert method_info["bug_id"] == meta["bug_id"]
            if "methodName" in method_info:
                transFileDir = os.path.join(args.outDir, args.transform, "files", sub_transform)
                create_missing_directories(transFileDir)
                transform_pattern = os.path.join(args.transMethodDir, sub_transform, "{}_{}_{}*.java".format(str(method_info["id"]), method_info["source_file"], method_info["methodName"]))
                for transform_path in glob.glob(transform_pattern):
                    new_meta = meta.copy()
                    list_bugs.append(str(method_info["id"]))
                    old_file = os.path.join(args.bugDir, str(method_info["id"]), method_info["source_file"] + ".java")
                    old_code = open(old_file, "r").readlines()
                    
                    transformed_methods = open(transform_path, "r").readlines()    
                    new_code = old_code[:method_info["methodStartLine"] - 1] + transformed_methods[1:-1] + old_code[method_info["methodEndLine"]:]  
                    faulty_lines = find_faulty_line(new_code)
                    start_idx, end_idx = faulty_lines[0][0], faulty_lines[1][0]

                    assert new_code.pop(start_idx).strip() == "//kXrgPH_C0deTransform_Start"
                    assert new_code.pop(end_idx - 1).strip() == "//kXrgPH_C0deTransform_End"
                    
                    if end_idx - start_idx >=3:
                        new_code_line = ""
                        is_change = False
                        for idx in range(start_idx, end_idx - 1):
                            if new_code[idx][:-1].strip().startswith("//"):
                                continue
                            new_code_line += " "
                            new_code_line += new_code[idx][:-1].strip()
                            is_change = True
                        
                        if not is_change:
                            continue    
                        
                        new_code[start_idx] =  new_code_line + "\n"
                        for idx in range(start_idx + 1, end_idx - 1):
                            new_code.pop(start_idx + 1)  
                        
                    if args.transform == "naming" and new_meta["id"] in count and count[new_meta["id"]] >= 5:
                        continue
                    
                    if new_meta["id"] not in count:
                        count[new_meta["id"]] = 1
                    else:
                        count[new_meta["id"]] += 1
                    new_idx += 1
                    file_name = "{}_{}_{}.java".format(new_idx, method_info["source_file"], method_info["methodName"])
                    new_file = os.path.join(transFileDir, file_name)
                    open(new_file, "w").write("".join(new_code))    
                    new_meta["ori_id"] = new_meta["id"]
                    new_meta["bug_id"] = "{}-{}".format(new_meta["subject"], new_meta["bug_id"])
                    new_meta["id"] = new_idx
                    new_meta["line_numbers"] = [start_idx + 1]
                    new_meta["transform_file"] = os.path.join(sub_transform, file_name)
                    new_meta_data.append(new_meta)
    
    with open(newMetaDir, "w") as json_file:
        json.dump(new_meta_data, json_file, indent=4)

if __name__ == "__main__":
    main()