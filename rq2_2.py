import pandas as pd
import json
import matplotlib.pyplot as plt
import numpy as np

def load_naturalness_evaluation(type):
    path = "data/ids/{}_ids.txt".format(type)
    return open(path).read().split("\n")

naturalness_evaluation = {
    "unnatural": load_naturalness_evaluation("unnatural"),
    "natural": load_naturalness_evaluation("natural"),
    "likely_natural": load_naturalness_evaluation("likely_natural"),
    "likely_unnatural": load_naturalness_evaluation("likely_unnatural")
}

def parse_correctness_assessment(path):
    df = pd.read_excel(path)
    for col in df.columns:
        df = df.rename(columns={col: col.strip()})
    
    rs = {}
    diff = {}
    for idx in range(len(df["bug_id"])):
        bug_id = df["id"][idx]
        
        assert "Annotation" in df.columns
        assert df["Annotation"][idx] in ["yes", "no"]
        
        label = df["Annotation"][idx] == "yes"
        rs[bug_id] = label
        diff[bug_id] = df["generated_diff"][idx]
    
    return rs, diff

def parse_results(transform, meta_path, annotation_path):
    meta = json.load(meta_path)
    assessment, diff = parse_correctness_assessment(annotation_path)
    rs = {}
    for bug_info in meta:
        id = bug_info["id"] 
        if "{}_{}".format(transform, id) not in naturalness_evaluation["natural"]:
            continue
        rs[id] = {
            "id": bug_info["id"],
            "bug_id": bug_info["bug_id"],
            "status": 0, # 0: not plausible, 1: plausible, 2: correct
            "diff": diff[id] if id in diff else None
        }
        if id in assessment:
            rs[id]["status"] = 2 if assessment[id] else 1
    return rs

def parse_origin_results(meta_path, annotation_path):
    meta = json.load(meta_path)
    assessment, diff = parse_correctness_assessment(annotation_path)
    rs = {}
    
    for bug_info in meta:
        id = bug_info["id"] 
        if len(bug_info["line_numbers"]) == 0:
            continue
        rs[bug_info["bug_id"]] = {
            "status": 0, # 0: not plausible, 1: plausible, 2: correct
            "diff": diff[id] if id in diff else None
        }
        if id in assessment:
            rs[bug_info["bug_id"]]["status"] = 2 if assessment[id] else 1
    return rs

def evaluation_metrics(results, all_bugs):
    plausible_rate = 0
    correct_rate = 0
    
    for bug_id in results:
        if bug_id in all_bugs:
            if results[bug_id]["status"] >= 1:
                plausible_rate += 1
            if results[bug_id]["status"] == 2:
                correct_rate += 1
    total_bugs = len(list(all_bugs))
    print("Plausible Rate: {}/{} ({})".format(plausible_rate, total_bugs, round(plausible_rate*100/total_bugs, 1)))
    print("Correct Rate: {}/{} ({})".format(correct_rate, total_bugs, round(correct_rate*100/total_bugs, 1)))
    return plausible_rate, correct_rate
def calculate_metrics(plausible, correct, all_bugs):
    plausible_rate = 0
    for bug_id in all_bugs:
        if bug_id in plausible:
            plausible_rate += plausible[bug_id]/all_bugs[bug_id]
    
    correct_rate = 0
    for bug_id in all_bugs:
        if bug_id in correct:
            correct_rate += correct[bug_id]/all_bugs[bug_id]
        
    total_bugs = len(list(all_bugs.keys()))
    print("Plausible Rate: {}/{} ({})".format(plausible_rate, total_bugs, round(plausible_rate*100/total_bugs, 1)))
    print("Correct Rate: {}/{} ({})".format(correct_rate, total_bugs, round(correct_rate*100/total_bugs, 1)))
    return plausible_rate, correct_rate
def normalized_evaluation_metrics(results):
    plausible = {}
    correct = {}
    all_bugs = {}
    for idx in results:
        bug_id = results[idx]["bug_id"]
        all_bugs[bug_id] = all_bugs[bug_id] + 1 if bug_id in all_bugs else 1
        if results[idx]["status"] >= 1:
            plausible[bug_id] = plausible[bug_id] + 1 if bug_id in plausible else 1

        if results[idx]["status"] == 2:
            correct[bug_id] = correct[bug_id] + 1 if bug_id in correct else 1

    # calculate_metrics(plausible, correct, all_bugs)
   
    return plausible, correct, all_bugs

def unique_change_rate(positive_changes, negative_changes, all_bugs):
    unique_pos_change = len(list(positive_changes.keys()))
    unique_neg_change = len(list(negative_changes.keys()))
    unique_change = len(set(list(positive_changes.keys()) + list(negative_changes.keys())))
    total_bugs = len(list(all_bugs))
    print("Positve: {}/{} ({})".format(unique_pos_change, total_bugs, round(unique_pos_change*100/total_bugs, 1)))
    print("including: {}".format(set(positive_changes)))
    print("Negative: {}/{} ({})".format(unique_neg_change, total_bugs, round(unique_neg_change*100/total_bugs, 1)))
    print("including: {}".format(set(negative_changes)))
    print("Change Rate: {}/{} ({})".format(unique_change, total_bugs, round(unique_change*100/total_bugs, 1)))
    
def normalized_change_rate(positive_changes, negative_changes, all_bugs):
    positive_rate = 0
    for bug_id in all_bugs:
        if bug_id in positive_changes:
            positive_rate += positive_changes[bug_id]/all_bugs[bug_id]
    
    negative_rate = 0
    for bug_id in all_bugs:
        if bug_id in negative_changes:
            negative_rate += negative_changes[bug_id]/all_bugs[bug_id]
    total_bugs = len(list(all_bugs))
    print("Positive Rate: {}/{} ({})".format(positive_rate, total_bugs, round(positive_rate*100/total_bugs, 1)))
    print("Negative Rate: {}/{} ({})".format(negative_rate, total_bugs, round(negative_rate*100/total_bugs, 1)))
    print("Change Rate: {}/{} ({})".format(negative_rate + positive_rate, total_bugs, round((positive_rate + negative_rate)*100/total_bugs, 1)))

def calculate_changes(transform_results, original_results):
    positive_changes = {}
    negative_changes = {}
    all_bugs = {}
    transform_changes = []
    for idx in transform_results:
        status = transform_results[idx]["status"]
        bug_id = transform_results[idx]["bug_id"]
        all_bugs[bug_id] = all_bugs[bug_id] + 1 if bug_id in all_bugs else 1
        if status > original_results[bug_id]["status"]:
            positive_changes[bug_id] = positive_changes[bug_id] + 1 if bug_id in positive_changes else 1
            transform_changes.append({
                "id": transform_results[idx]["id"],
                "diff": transform_results[idx]["diff"],
                "original_diff": original_results[bug_id]["diff"]
            })
        if status < original_results[bug_id]["status"]:
            negative_changes[bug_id] = negative_changes[bug_id] + 1 if bug_id in negative_changes else 1
            transform_changes.append({
                "id": transform_results[idx]["id"],
                "diff": transform_results[idx]["diff"],
                "original_diff": original_results[bug_id]["diff"]
            })

    
    
    # print("== Unique Change Rate ==")
    # unique_change_rate(positive_changes, negative_changes, all_bugs)
    
    # print("== Normalized Change Rate ==")
    # normalized_change_rate(positive_changes, negative_changes, all_bugs)
    
    return positive_changes, negative_changes, all_bugs, transform_changes

def merge_dict(curr, new):
    for key in new:
        curr[key] = curr[key]  + new[key] if key in curr else  new[key]
    return curr

def evaluate(tool):
    print(f"============ Transformation: {tool} ============")
    f = open("data/changes/{}.txt".format(tool), "w")
    original_results = parse_origin_results(open(f"data/meta/original.json", 'r'), f"data/plausible_patches/original-{tool}.xlsx")
    all_plausible, all_correct, all_bugs = {}, {}, {}
    all_positive_changes, all_negative_changes = {}, {}
    all_changes = []
    for transform in ["naming", "expression", "statement"]:
        transform_results = parse_results(transform, open(f"data/meta/{transform}.json", 'r'), f"data/plausible_patches/{transform}-{tool}.xlsx")
        plausible, correct, bugs = normalized_evaluation_metrics(transform_results)
        positive_changes, negative_changes, _, transform_changes = calculate_changes(transform_results, original_results)
        all_plausible, all_correct, all_bugs = merge_dict(all_plausible, plausible), merge_dict(all_correct, correct), merge_dict(all_bugs, bugs)
        all_positive_changes, all_negative_changes = merge_dict(all_positive_changes, positive_changes), merge_dict(all_negative_changes, negative_changes)
        for change in transform_changes:
            f.write('==============================\n')
            f.write(f'{transform}-{change["id"]}\n')
            all_changes.append(f'{transform}_{change["id"]}')
            f.write('------------------------------\n')
            if change["diff"] is not None:
                f.write(change["diff"] + "\n")
            else:
                f.write("None\n")
            f.write('------------------------------\n')
            if change["original_diff"] is not None:
                f.write(change["original_diff"] + "\n")
            else:
                f.write("None\n")

    print("== Performance on Original Data ==")
    plausible_rate_b, correct_rate_b = evaluation_metrics(original_results, all_bugs)
    print("== Performance on Transformed Data ==")
    plausible_rate_a, correct_rate_a = calculate_metrics(all_plausible, all_correct, all_bugs)
    print(1 - plausible_rate_a/plausible_rate_b)
    print(1 - correct_rate_a/correct_rate_b)
    print("== Unique Change Rate ==")
    unique_change_rate(all_positive_changes, all_negative_changes, all_bugs)
    
    n_positive = sum(list(all_positive_changes.values()))
    n_negative = sum(list(all_negative_changes.values()))
    print("Total Positive Change: {}({}%)".format(n_positive, round(n_positive*100/len(naturalness_evaluation["natural"]), 1)))
    print("Total Negative Change: {}({}%)".format(n_negative, round(n_negative*100/len(naturalness_evaluation["natural"]), 1)))
    print("Total Change: {}({}%)".format(n_negative + n_positive, round((n_negative+n_positive)*100/len(naturalness_evaluation["natural"]), 1)))
    f.close()
    return all_changes

print("----------------------------------------------------------------------")
print("==> [RQ2.2 - Section 4.2.2] Impact of Unnatural Code Transformations on Robustness Testing")
changes = {
    "alpharepair": evaluate("alpharepair"),
    "recoder": evaluate("recoder"),
    "sequencer": evaluate("sequencer"),
    "selfapr": evaluate("selfapr"),
    "rewardrepair": evaluate("rewardrepair")
}

