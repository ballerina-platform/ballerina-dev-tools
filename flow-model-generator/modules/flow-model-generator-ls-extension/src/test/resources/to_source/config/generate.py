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
import sys
from typing import OrderedDict


def remove_duplicates(output_nodes):
    # Use OrderedDict to preserve order and remove duplicates
    unique_output_nodes = list(OrderedDict((json.dumps(node, sort_keys=True), node) for node in output_nodes).values())
    return unique_output_nodes


def write_json(output_node, output_filename):
    # Create the JSON structure
    output_data = {
        "source": f"{output_filename.split('.json')[0]}/main.bal",
        "description": "Sample diagram node",
        "output": {},
        "diagram": output_node
    }

    # Write the JSON data to the output file
    output_file_path = os.path.join(current_path, output_filename)
    with open(output_file_path, 'w') as output_file:
        json.dump(output_data, output_file)


def process_file_code(file_code):
    # Split the user input by "-"
    input_parts = file_code.split("-")
    input_node_name = input_parts[0]

    # Relative to this path, read the file
    diagram_dir_name = os.path.join(
        current_path, "..", "..", "diagram_generator", "config")
    matching_files = sorted([file for file in os.listdir(
        diagram_dir_name) if file.startswith(file_code)])

    # Iterate over the matching files
    output_nodes = []
    for file_name in matching_files:
        # Construct the file path
        file_path = os.path.join(diagram_dir_name, file_name)

        # Read the JSON file
        with open(file_path, 'r') as json_file:
            data = json.load(json_file)
            codedata_type = input_node_name.upper()

            # Get the nodes array from the diagram property
            nodes = data['diagram']['nodes']

            # Find the nodes with the specified kind
            for node in nodes:
                if node['codedata']['node'] == codedata_type:
                    output_nodes.append(node)

            if codedata_type == "NEW_CONNECTION":
                connections = data['diagram']['connections']
                for connection in connections:
                    if connection['codedata']['node'] == codedata_type:
                        output_nodes.append(connection)

    # Write each node to a JSON file
    output_nodes_set = remove_duplicates(output_nodes)
    for index, node in enumerate(output_nodes_set):
        write_json(node, f"{file_code}{index+1}.json")

    # Add a test case for the template file
    template_file_dir = os.path.join(current_path, "..", "..", "node_template", "config")
    matching_template_files = sorted([file for file in os.listdir(
        template_file_dir) if file.startswith(file_code)])

    # Iterate over the matching template files
    for matching_template_file in matching_template_files:
        # Construct the file path
        template_file_name = os.path.join(
            template_file_dir, matching_template_file)

        # Read the JSON template file
        with open(template_file_name, 'r') as template_file:
            template_data = json.load(template_file)
            output_property = template_data['output']
            output_property['codedata']['lineRange'] = {
                "fileName": "test.bal",
                "startLine": {"line": 0, "offset": 0},
                "endLine": {"line": 0, "offset": 0}
            }
            write_json(output_property, matching_template_file)


# Get the user input from the command line argument
file_code = sys.argv[1]

# Exit the program if the user input is empty
if not file_code:
    print("Please enter the node kind")
    sys.exit(1)

# Obtain the current path
current_path = os.path.dirname(os.path.abspath(__file__))

# Check if the input is equal to "all"
if file_code == "all":
    # Read the files in the format "*.json" in the node_template directory
    template_files = sorted([file for file in os.listdir(
        os.path.join(current_path, "..", "..", "node_template", "config")) if file.endswith(".json")])
    for template_file in template_files:
        prefix = template_file.split(".")[0]
        print(f"Generating node: {template_file}...")
        process_file_code(prefix)
else:
    # Call the function with the file_code input
    process_file_code(file_code)