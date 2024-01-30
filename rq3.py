import matplotlib.pyplot as plt
import json
import numpy as np
from sklearn import metrics
import os 
from scipy import stats
import matplotlib.patches as mpatches
import pandas as pd
import seaborn as sns

def MWW(A, B):
    # Ensure both arrays are NumPy arrays
    A = np.array(A)
    B = np.array(B)

    statistic, p_value = stats.mannwhitneyu(A, B, alternative='less')
    r = 1 - 2*statistic/(A.size*B.size)


    return statistic, p_value, r

def read_id2idx(file_name):
    id2idx = {}
    with open(file_name, "r") as f:
        for line in f:
            line = line.strip()
            idx, id = line.split("\t")
            id2idx[int(id) - 1] = int(idx)
    return id2idx

def read_entropy_data(file_name, id2idx):
    entropy = {}
    with open(file_name, "r") as f:
        for line in f:
            id, value = line.split("\t")
            entropy[id2idx[int(id)]] = float(value)
    return entropy

def read_data(model):
    if model == "ngram":
        tokenizer = "antlr"
    else:
        tokenizer = model
    id2idx = {
        "original": read_id2idx(f"data/entropy/id2idx/original/{tokenizer}.id2idx"),
        "naming": read_id2idx(f"data/entropy/id2idx/naming/{tokenizer}.id2idx"),
        "expression": read_id2idx(f"data/entropy/id2idx/expression/{tokenizer}.id2idx"),
        "statement": read_id2idx(f"data/entropy/id2idx/statement/{tokenizer}.id2idx"),
    }
    return {
        "original": read_entropy_data(f"data/entropy/{model}_original_default.txt", id2idx["original"]),
        "naming": read_entropy_data(f"data/entropy/{model}_naming_default.txt", id2idx["naming"]),
        "statement": read_entropy_data(f"data/entropy/{model}_statement_default.txt", id2idx["statement"]),
        "expression": read_entropy_data(f"data/entropy/{model}_expression_default.txt", id2idx["expression"])
    }

    
def sns_box_plot(df, metric):
    mapping = {
        "ce": "Cross Entropy",
        "rnc": "Relative Naturalness Changes",
        "nc": "Naturalness Changes"
    }
    # palette = ['dimgray', 'lightgray', 'darkgray', 'white']
    palette = ['tab:blue', 'aliceblue', 'dodgerblue', 'lightskyblue']
    ax = sns.boxplot(data=df, x="model", y=metric, hue="naturalness_category", orient="v", palette=palette)
    ax.set_ylabel(mapping[metric], fontsize=14)
    ax.set_xlabel("")
    if metric == "rnc":
        ax.set_ylim(-20, 30)
    plt.xticks(fontsize=14)
    plt.yticks(fontsize=14)
    plt.legend()
    plt.legend(loc='upper center', bbox_to_anchor=(0.5, 1.13),
        ncol=2, fancybox=True, shadow=True, fontsize="14")
    plt.savefig("data/figures/rq3_{}.png".format(metric), dpi=300)
    plt.clf()
    
def box_plot(data, labels, name):
    # Create a figure and axis
    fig, ax = plt.subplots(figsize=(4, 6))

    # Create a box plot for each array
    ax.boxplot(data, labels=labels)

    # Set labels and title
    # plt.ylabel('Cross Entropy', fontsize=14)
    plt.xticks(fontsize=16)
    plt.yticks(fontsize=16)
    # Show the plot
    plt.savefig(os.path.join("data/figures", name), dpi=300)
    plt.clf()

def read_meta_data(name):
    file_name = "data/meta/{}.json".format(name)
    with open(file_name, 'r') as json_file:
        json_data = json_file.read()

    # Parse the JSON data
    data = json.loads(json_data)
    
    return data
def read_meta_data_mappings():
    meta_data = {
        "naming": read_meta_data("naming"),
        "expression": read_meta_data("expression"),
        "statement": read_meta_data("statement"),
    }   
    
    return {
        "naming": {meta_data["naming"][i]["id"]: meta_data["naming"][i]["ori_id"] for i in range(len(meta_data["naming"]))},
        "expression": {meta_data["expression"][i]["id"]: meta_data["expression"][i]["ori_id"] for i in range(len(meta_data["expression"]))},
        "statement": {meta_data["statement"][i]["id"]: meta_data["statement"][i]["ori_id"] for i in range(len(meta_data["statement"]))},
    }
    
def evaluations_metrics(natural, unnatural):
    y = np.array([1] * len(natural) + [0] * len(unnatural))
    pred = np.concatenate([natural, unnatural])
    y_pred = np.array([1 if p >= 0.5 else 0 for p in pred])
    fpr, tpr, thresholds = metrics.roc_curve(y, pred, pos_label=1)
    tn, fp, fn, tp = metrics.confusion_matrix(y, y_pred).ravel()
    return y_pred, metrics.auc(fpr, tpr), metrics.f1_score(y, y_pred), metrics.precision_score(y, y_pred), metrics.recall_score(y, y_pred), 
    
    
def comparision_function(new_score, old_score, type="ratio"):
    if type == "ratio":
        return (new_score / old_score - 1) * 100
    elif type == "absolute":
        return new_score - old_score
    
    
def main():
    print("----------------------------------------------------------------------")
    print("==> [RQ3 - Section 4.3] Effectiveness of Language Models on Automated Naturalness Assessment of Code Transformations")
    entropy = {
        "N-gram": read_data("ngram"),
        "GPTNeo": read_data("gptneo"),
        "BLOOM": read_data("bloom"),
        "CodeLlama": read_data("codellama"),
    }
    
    ids = {
        "Natural": open("data/ids/natural_ids.txt", "r").read().split("\n"),
        "Unnatural": open("data/ids/unnatural_ids.txt", "r").read().split("\n"),
        "Likely Natural": open("data/ids/likely_natural_ids.txt", "r").read().split("\n"),
        "Likely Unnatural": open("data/ids/likely_unnatural_ids.txt", "r").read().split("\n"),
    }
    meta_data_mappings = read_meta_data_mappings()
    
    results = {
        "ce": [],
        "model": [],
        "rnc": [],
        "nc": [],
        "naturalness_category": []
    }
    probability = {}
    probability_ce = {}
    probability_nc = {}
    
    rnc_results = {}
    print("==> Statistical Tests")
    for model in entropy:
        all_new_score, all_old_score = [], []
        rnc_results[model] = {}
        probability[model] = {}
        probability_ce[model] = {}
        probability_nc[model] = {}
        all_values = []
        for naturalness_type in ids:
            rnc_results[model][naturalness_type] = []
            probability[model][naturalness_type] = []
            probability_ce[model][naturalness_type] = []
            probability_nc[model][naturalness_type] = []
            for idx in ids[naturalness_type]:
                level, bug = idx.split("_")
                bug = int(bug)
                new_score = entropy[model][level][bug]
                old_score = entropy[model]["original"][meta_data_mappings[level][bug]]
                rnc = comparision_function(new_score, old_score)
                nc = comparision_function(new_score, old_score, "absolute")
                results["ce"].append(new_score)
                results["rnc"].append(rnc)
                results["nc"].append(nc)
                results["model"].append(model)
                results["naturalness_category"].append(naturalness_type)
                rnc_results[model][naturalness_type].append(rnc)
                all_new_score.append(new_score)
                all_old_score.append(old_score)

                # probability[model][naturalness_type].append(1 - (rnc + 100)/200)
                probability[model][naturalness_type].append(rnc)
                probability_ce[model][naturalness_type].append(new_score)
                probability_nc[model][naturalness_type].append(nc)

        _, p_value, effect_size = MWW(rnc_results[model]["Natural"], rnc_results[model]["Unnatural"])
        print("MWW on model {}: p-value: {}, effect size: {}".format(model, p_value, effect_size))
        _, p_value, effect_size = MWW(rnc_results[model]["Likely Natural"], rnc_results[model]["Likely Unnatural"])
        print("MWW on model {}: p-value: {}, effect size: {}".format(model, p_value, effect_size))
        _, p_value, effect_size = MWW(rnc_results[model]["Likely Unnatural"], rnc_results[model]["Likely Natural"])
        print("MWW on model {}: p-value: {}, effect size: {}".format(model, p_value, effect_size))
        spearman_corr, _ = stats.spearmanr(all_new_score, all_old_score)
        print(f"Spearman Correlation on model {model}: {spearman_corr}")
    
    #Normalize RNC:
    for model in probability:
        all_values = [] 
        for naturalness_type in probability[model]:
            all_values.extend(probability[model][naturalness_type])
        min = np.min(all_values)
        max = np.max(all_values)
        for naturalness_type in probability[model]:
            probability[model][naturalness_type] = [1 - (v - min) / (max - min) for v in probability[model][naturalness_type]]
            
    #Normalize NC/CE:
    for model in probability_nc:
        all_values = [] 
        for naturalness_type in probability_nc[model]:
            all_values.extend(probability_nc[model][naturalness_type])
        min = np.min(all_values)
        max = np.max(all_values)
        for naturalness_type in probability_nc[model]:
            probability_nc[model][naturalness_type] = [1 - (v - min) / (max - min) for v in probability_nc[model][naturalness_type]]
    
    for model in probability_ce:
        all_values = [] 
        for naturalness_type in probability_ce[model]:
            all_values.extend(probability_ce[model][naturalness_type])
        min = np.min(all_values)
        max = np.max(all_values)
        for naturalness_type in probability_ce[model]:
            probability_ce[model][naturalness_type] = [1 - (v - min) / (max - min) for v in probability_ce[model][naturalness_type]]
    df = pd.DataFrame(results)
    
    print("==> Box-Plot of CE (Figures 3a): Please see results in data/figures/rq3_ce.png")
    sns_box_plot(df, "ce")
    print("==> Box-Plot of RNC (Figures 3b):Please see results in data/figures/rq3_rnc.png")
    sns_box_plot(df, "rnc")
    print("==> Box-Plot of NC (Figures 3b):Please see results in data/figures/rq3_rnc.png")
    sns_box_plot(df, "nc")
    
    print("==> Classification Results - Table 7")
    for model in rnc_results:
        assert len(rnc_results[model]["Natural"]) == 693
        assert len(rnc_results[model]["Unnatural"]) == 227
        assert len(rnc_results[model]["Likely Unnatural"]) == 240
        assert len(rnc_results[model]["Likely Natural"]) == 18
        _, auc, _, _, _ = evaluations_metrics(probability[model]["Natural"], probability[model]["Unnatural"])
        print(f"[RNC] AUC of {model}: {auc}")
        
    for model in rnc_results:
        _, auc, _, _, _ = evaluations_metrics(probability_nc[model]["Natural"], probability_nc[model]["Unnatural"])
        print(f"[NC] AUC of {model}: {auc}")
    
    for model in rnc_results:
        _, auc, _, _, _ = evaluations_metrics(probability_ce[model]["Natural"], probability_ce[model]["Unnatural"])
        print(f"[CE] AUC of {model}: {auc}")
    
if __name__ == "__main__":
    main()