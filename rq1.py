import json
import argparse
import numpy as np
from statsmodels.stats.inter_rater import fleiss_kappa, aggregate_raters
import matplotlib.pyplot as plt
from krippendorff import alpha
import os
import pandas as pd
from scipy import stats

def agreement(data):
    print(data.shape)
    data = data.transpose()
    ratings_array, _ = aggregate_raters(data, n_cat=2)
    kappa = fleiss_kappa(ratings_array)
    return kappa

def MWW(A, B):
    # Ensure both arrays are NumPy arrays
    A = np.array(A)
    B = np.array(B)

    statistic, p_value = stats.mannwhitneyu(A, B, alternative='less')
    r = 1 - 2*statistic/(A.size*B.size)


    return statistic, p_value, r

def box_plot(data, labels, name):
    # Create a figure and axis
    fig, ax = plt.subplots()

    # Create a box plot for each array
    ax.boxplot(data, labels=labels)

    # Set labels and title
    ax.set_xlabel('Data Sets')
    ax.set_ylabel('Values')
    ax.set_title('Box Plot of Three Arrays')

    # Show the plot
    plt.savefig(name)
    
def write_ids_to_file(ids, file_name):
    with open(os.path.join("data/ids", file_name), "w") as f:
        f.write("\n".join(ids))
    
def calculate_naturalness(data):
    return data[data >=4], data[data == 3], data[data == 2], data[data <=1]

def main():
    # Specify the file name to read
    file_name = "data/human_study/survey/survey.json"

    # Read the JSON data from the file and load it into a dictionary
    with open(file_name, "r") as json_file:
        data = json.load(json_file)
    
    final = [[],[],[],[],[]]
    bug_info = []
    average_cc_score = []
    average_cr_score = []
    time = [[],[],[],[],[]]
    for level in data:
        for bug in data[level]:
            bug_info.append("{}_{}".format(level, bug))
            cc_score = 0
            cr_score = 0
            for participant in data[level][bug]:
                cur_final = 0
                cc_score += data[level][bug][participant]["CC"]
                cr_score += data[level][bug][participant]["CR"]
                if data[level][bug][participant]["CC"] >= 3:
                    cur_final = 1
                if data[level][bug][participant]["CR"] >= 3:
                    cur_final = 1
                ID = int(participant[1:]) - 1
                
                if ID >=5:
                    ID = ID - 5
                assert 0 <=ID and ID <=9
                final[ID].append(cur_final)
                time[ID].append(float(data[level][bug][participant]["Time"]))
            average_cc_score.append(cc_score/5)
            average_cr_score.append(cr_score/5)
    
    with(open("data/score/average_score.txt", "w")) as f:
        for idx, bug in enumerate(bug_info):
            f.write("{}\t{}\t{}\n".format(bug, average_cc_score[idx], average_cr_score[idx]))
    
    naming_range = np.array(range(906))
    expression_range = np.array(range(906, 1061))
    statement_range = np.array(range(1061, 1178))
    
    final = np.array(final)
    df = pd.DataFrame(final.transpose())
    df.to_csv("data/human_study/survey/survey.csv", index=False)
    results = final.sum(axis=0)
    time = np.array(time)
    bug_info = np.array(bug_info)
    
    # Some assertion
    for bug in bug_info[naming_range]:
        assert bug.startswith("naming")

    for bug in bug_info[expression_range]:
        assert bug.startswith("expression")
    
    for bug in bug_info[statement_range]:
        assert bug.startswith("statement")
        
    print("----------------------------------------------------------------------")
    print("==> [RQ1.1 - Section 4.1.2] Completion Time")
    _99th_percentile_time = np.percentile(time, 99)
    print("=> Overal")
    print("+ 99% Percentile: " + str(round(_99th_percentile_time, 2)))
    print("+ Average: " + str(np.mean(time[time < _99th_percentile_time])))
    print("+ STD: " +str(np.std(time[time < _99th_percentile_time])))
    
    
    naming_time = time[:, naming_range]
    naming_time = naming_time[naming_time < _99th_percentile_time]
    print("=> Table 1: Time taken by different transformation levels")
    print("+++ Naming-leve;")
    print("+ Average Naming-level Completion Time: " + str(np.mean(naming_time)))
    print("+ STD Naming-level Completion Time: " + str(np.std(naming_time)))
    print("++++++")
    expression_time = time[:, expression_range]
    expression_time = expression_time[expression_time < _99th_percentile_time]
    print("+++ Expression-level:")
    print("+ Average Expression-level Completion Time: " + str(np.mean(expression_time)))
    print("+ STD Expression-level Completion Time: " + str(np.std(expression_time)))
    print("++++++")
    statement_time = time[:, statement_range]
    statement_time = statement_time[statement_time < _99th_percentile_time]
    print("+++ Statement-level:")
    print("+ Average Statement-level Completion Time: " + str(np.mean(statement_time)))
    print("+ STD Statement-level Completion Time: " + str(np.std(statement_time)))
    print("++++++")

    print("+++ Statistical Tests:")
    _, p_value, effect_size = MWW(expression_time, statement_time)
    print("MWW Test: p-value: {}, effect size: {}".format(p_value, effect_size))

    _, p_value, effect_size = MWW(naming_time, statement_time)
    print("MWW Test: p-value: {}, effect size: {}".format(p_value, effect_size))

    _, p_value, effect_size = MWW(naming_time, expression_time)
    print("MWW Test: p-value: {}, effect size: {}".format(p_value, effect_size))
    print("++++++")

    print("=> Table 2: Time taken by high agreement vs high disgareement cases")
    natural_ids = np.where(results <=1)[0]
    write_ids_to_file(bug_info[natural_ids], "natural_ids.txt")
    unnatural_ids = np.where(results >=4)[0]
    write_ids_to_file(bug_info[unnatural_ids], "unnatural_ids.txt")
    likely_natural_ids = np.where(results == 2)[0]
    write_ids_to_file(bug_info[likely_natural_ids], "likely_natural_ids.txt")
    likely_unnatural_ids = np.where(results == 3)[0]
    write_ids_to_file(bug_info[likely_unnatural_ids], "likely_unnatural_ids.txt")
    agreement_ins = np.concatenate([natural_ids, unnatural_ids])
    write_ids_to_file(bug_info[agreement_ins], "agreement_ins.txt")
    disagrement_ids =  np.concatenate([likely_natural_ids, likely_unnatural_ids])
    write_ids_to_file(bug_info[disagrement_ids], "disagrement_ids.txt")

    agreement_time = time[:, agreement_ins]
    agreement_time = agreement_time[agreement_time < _99th_percentile_time]

    disagreement_time = time[:, disagrement_ids]
    disagreement_time = disagreement_time[disagreement_time < _99th_percentile_time]
    print("+++ Agreement:")
    print("Average time for agreement: " + str(np.mean(agreement_time)))
    print("STD time for agreement: " + str(np.std(agreement_time)))
    print("++++++")
    print("+++ Disagreement:")
    print("Average time for disagreement: " + str(np.mean(disagreement_time)))
    print("STD time for disagreement: " + str(np.std(disagreement_time)))
    print("++++++")
    print("+++ Statistical Tests:")
    _, p_value, effect_size = MWW(agreement_time, disagreement_time)
    print("MWW Test: p-value: {}, effect size: {}".format(p_value, effect_size))
    
    print("----------------------------------------------------------------------")
    print("==> [RQ1.2 - Section 4.1.3] Agreement")
    print("=> Overall Agreement")
    print("+ Data points that are considered as natural by a all annotators: {}({}%)".format(len(results[results == 0]), round(len(results[results == 0])/len(results) * 100, 2)))
    print("+ Data points that are considered as unnatural by a all annotators: {}({}%)".format(len(results[results == 5]), round(len(results[results == 5])/len(results) * 100, 2)))
    print("+ Data points that are considered as natural by a 4/5 annotators: {}({}%)".format(len(results[results <= 1]), round(len(results[results <= 1])/len(results) * 100, 2)))
    print("+ Data points that are considered as unnatural by a 4/5 annotators: {}({}%)".format(len(results[results >= 4]), round(len(results[results >= 4])/len(results) * 100, 2)))
    print("+ Data points that are considered as natural by a 3/5 annotators: {}({}%)".format(len(results[results == 2]), round(len(results[results == 2])/len(results) * 100, 2)))
    print("+ Data points that are considered as unnatural by a 3/5 annotators: {}({}%)".format(len(results[results == 3]), round(len(results[results == 3])/len(results) * 100, 2)))
    print("+ Data points that are considered as not natural by a majority of Annotators: {}%".format(round(len(results[results >= 3])/len(results) * 100, 2)))
    print("+ Data points that are considered as not natural by a at least one of Annotators: {}%".format(round(len(results[results >= 1])/len(results) * 100, 2)))
    
    print("=> Inter-rater Agreement Metrics")
    print("+ Fleiss's agreement: " + str(round(agreement(final), 2)))
    
    print("----------------------------------------------------------------------")
    print("==> [RQ1.3 - Section 4.1.4] Naturalness Ratio")
    naming_naturalness = calculate_naturalness(results[naming_range])
    expression_naturalness = calculate_naturalness(results[expression_range])
    statemet_naturalness = calculate_naturalness(results[statement_range])
    full_naturalness = calculate_naturalness(results)
    categories = ['Naming', 'Expression', 'Statement', 'Overall']

    naming_values = np.array([len(i) for i in naming_naturalness])
    expression_values =  np.array([len(i) for i in expression_naturalness])
    statement_values =  np.array([len(i) for i in statemet_naturalness])
    full_values =  np.array([len(i) for i in full_naturalness])

    print("=> Table 3: Naturalness Ratio")
    print("+ Naming-level: " + str(naming_values))
    print("+ Expression-level: " + str(expression_values))
    print("+ Statement-level: " + str(statement_values))
    print("+ Full-level: " + str(full_values))
    
    assert naming_values.sum() == 906
    assert expression_values.sum() == 155
    assert statement_values.sum() == 117
    assert full_values.sum() == 1178
    
    naming_values = np.array(naming_values) / 906 * 100
    expression_values = np.array(expression_values) / 155 * 100
    statement_values = np.array(statement_values) / 117 * 100
    full_values = np.array(full_values) / 1178 * 100

    values = np.array([naming_values, expression_values, statement_values, full_values])
    values = np.round(values, 1)

    fig, ax = plt.subplots(figsize=(5.5, 3))
        
    bars1 = ax.bar(categories, values[:, 0], label='Unnatural', edgecolor="black", color="tab:orange")
    ax.bar_label(bars1, label_type='center')

    bars2 = ax.bar(categories, values[:, 1] + values[:, 2], bottom=values[:, 0], label='Likely Natural/Unnatural', edgecolor="black", color="tab:gray")
    ax.bar_label(bars2, label_type='center')
    bars3 = ax.bar(categories, values[:, 3], bottom=values[:, 0] + values[:, 1] + values[:, 2], label='Natural', edgecolor="black", color="tab:green")
    ax.bar_label(bars3, label_type='center')
    ax.legend(title='Categories', bbox_to_anchor=(1.05, 1), loc='upper left')

    plt.ylabel('Proportion (%)', fontsize=12)
    plt.xticks(fontsize=12)
    plt.yticks(fontsize=12)
    plt.legend()
    plt.legend(loc='upper center', bbox_to_anchor=(0.5, 1.13),
          ncol=3, fancybox=True, shadow=True)
    print("=> Please see results in data/figures/rq1_naturalness.png")
    plt.savefig("data/figures/rq1_naturalness.png", dpi=300)
if __name__ == "__main__":
    main()


