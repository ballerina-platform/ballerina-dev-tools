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


def write_json(output_node, output_filename):
    # Create the JSON structure
    output_data = {
        "description": "Sample diagram node",
        "output": [],
        "diagram": output_node
    }

    # Write the JSON data to the output file
    output_file_path = os.path.join(current_path, output_filename)
    with open(output_file_path, 'w') as output_file:
        json.dump(output_data, output_file)


# Prompt the user for input
user_input = input("File name: ")

# Obtain the current path
current_path = os.path.dirname(os.path.abspath(__file__))

# Relative to this path, read the file
diagram_dir_name = os.path.join(
    current_path, "..", "diagram_generator", "config")
matching_files = [file for file in os.listdir(
    diagram_dir_name) if file.startswith(user_input)]

# Iterate over the matching files
for file_name in matching_files:
    # Construct the file path
    file_path = os.path.join(diagram_dir_name, file_name)

    # Read the JSON file
    with open(file_path, 'r') as json_file:
        data = json.load(json_file)

        # Get the nodes array from the diagram property
        nodes = data['diagram']['nodes']

        # Find the first node with the specified kind
        for node in nodes:
            if node['codedata']['node'] == user_input.upper():
                write_json(node, file_name)
                break

# Add a test case for the template file
template_file = os.path.join(
    current_path, "..", "node_template", f"{user_input}.json")

# Read the JSON template file
with open(template_file, 'r') as template_file:
    template_data = json.load(template_file)
    output_property = template_data['output']
    output_property["lineRange"] = {
        "fileName": "test.bal",
        "startLine": {"line": 0, "offset": 0},
        "endLine": {"line": 0, "offset": 0}
    }
    write_json(output_property, f"{user_input}0.json")
