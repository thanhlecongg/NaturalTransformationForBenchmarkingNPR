import json
import numpy as np

def transform_distribution(n_transform, meta_path):
    naming_info = json.load(open(meta_path, 'r'))
    for bug in naming_info:
        if bug["ori_id"] not in n_transform:
            n_transform[bug["ori_id"]] = 0
        n_transform[bug["ori_id"]] += 1
    return n_transform
n_transform = {}
n_transform = transform_distribution(n_transform, 'data/meta/naming.json')
n_transform = transform_distribution(n_transform, 'data/meta/expression.json')
n_transform = transform_distribution(n_transform, 'data/meta/statement.json')

n_transform = np.array(list(n_transform.values()))
n_transform = np.sort(n_transform)
for i in range(1, 13):
    print(f"Number of bugs with {i} transform: ", len(n_transform[(n_transform == i)]))
