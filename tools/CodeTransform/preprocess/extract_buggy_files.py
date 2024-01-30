import json
import os
import shutil

with open("data/defects4j/meta-data.json", "r") as json_file:
    meta_data = json.load(json_file)
    
for dat in meta_data:
    idx = str(dat["id"])
    srcfiles = dat["source_file"].split(";")
    srcdir = dat["source_directory"]
    for srcfile in srcfiles:
        buggy_path = os.path.join("data/defects4j/full", idx, srcdir, srcfile.replace(".", "/") + ".java")
        if not os.path.exists(buggy_path):
            print(" ====== " + dat["subject"] + "-" + str(dat["bug_id"]) + " ====== ")
            print(buggy_path)
        else:
            des_dir = os.path.join("data/defects4j/buggy_files", idx)
            os.makedirs(des_dir, exist_ok = True)
            des_path = os.path.join(des_dir, srcfile + ".java")
            shutil.copy(buggy_path, des_path)
    
