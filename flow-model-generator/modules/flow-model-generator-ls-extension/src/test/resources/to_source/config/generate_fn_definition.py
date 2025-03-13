'''
Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)

WSO2 LLC. licenses this file to you under the Apache License,
Version 2.0 (the "License"); you may not use this file except
in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
'''

import os
import json
import glob
import re
import sys


def write_json_file(json_file, output_file_name):
    with open(json_file, "r") as f:
        # Read the entire JSON content into DIAGRAM
        diagram = json.load(f)

    output_data = {
        "source": "empty.bal",
        "description": "Sample diagram node",
        "output": {},
        "diagram": diagram['output']
    }

    output_file = os.path.join(current_dir, output_file_name)
    with open(output_file, "w") as out:
        json.dump(output_data, out, indent=4)


def sort_key(filepath):
    filename = os.path.basename(filepath)
    name, _ = os.path.splitext(filename)
    match = re.match(r"([^\d]+)(\d+)?", name)
    if match:
        prefix = match.group(1)
        number = int(match.group(2)) if match.group(2) else 0
        return (prefix, number)
    return (name, 0)


# Define the paths to the function definition configurations
current_dir = os.path.dirname(os.path.abspath(__file__))
fn_def_dir = os.path.normpath(os.path.join(
    current_dir, "..", "..", "function_definition", "config"))
node_template_dir = os.path.normpath(os.path.join(
    current_dir, "..", "..", "node_template", "config"))

# Capture user input for the function type
if len(sys.argv) < 2:
    raise ValueError("Need to provide the function form type as an argument")
user_input = sys.argv[1]
input_file = user_input + "_def"
output_file = user_input + "_definition"

# Generate the configs for the function definitions
json_files = glob.glob(os.path.join(fn_def_dir, f"{input_file}*.json"))
json_files.sort(key=sort_key)
print("\n".join(json_files))
for idx, json_file in enumerate(json_files, 1):
    write_json_file(json_file, f"{output_file}{idx}.json")

# Generate the config for the node template
node_template_file = f"{output_file}.json"
template_file = os.path.join(node_template_dir, node_template_file)
write_json_file(template_file, node_template_file)
