'''
Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com)

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


current_dir = os.path.dirname(os.path.abspath(__file__))
fn_def_dir = os.path.normpath(os.path.join(
    current_dir, "..", "..", "function_definition", "config"))
node_template_dir = os.path.normpath(os.path.join(
    current_dir, "..", "..", "node_template", "config"))

# Generate the configs for the function definitions
json_files = glob.glob(os.path.join(fn_def_dir, "*.json"))
for idx, json_file in enumerate(json_files, 1):
    write_json_file(json_file, f"function_definition{idx}.json")

# Generate the config for the node template
template_file = os.path.join(node_template_dir, "function_definition.json")
write_json_file(template_file, "function_definition.json")
