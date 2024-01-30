import json
import subprocess
import multiprocessing
import os
def checkout_project(meta_data):
    project_id = meta_data["subject"]
    version_id = str(meta_data["bug_id"]) + "b"
    if len(meta_data["line_numbers"]) > 0:
        output_dir = "data/defects4j/full/{}".format(meta_data["id"])
        if not (os.path.exists(output_dir) and len(os.listdir(output_dir)) > 0):
            # Defects4J checkout command
            checkout_cmd = f"defects4j checkout -p {project_id} -v {version_id} -w {output_dir}"

            # Execute the checkout command
            try:
                subprocess.run(checkout_cmd, shell=True, check=True)
            except subprocess.CalledProcessError:
                exit()
        
# Open the JSON file and load its contents
with open("data/defects4j/meta-data.json", "r") as json_file:
    meta_data = json.load(json_file)

ids = meta_data
pool = multiprocessing.Pool(processes=8)

pool.map(checkout_project, ids)

pool.close()
pool.join()

