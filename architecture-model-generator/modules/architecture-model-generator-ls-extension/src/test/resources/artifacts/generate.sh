#!/bin/bash
# ---------------------------------------------------------------------------
#  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org)
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

# Check if a name parameter is provided
if [ -z "$1" ]; then
    echo "Error: Please provide a name parameter."
    echo "Usage: $0 <name>"
    exit 1
fi

# Assign the name parameter to a variable
name=$1

# Get the directory where this script is located
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Create the JSON file in the config directory
cat > "$script_dir/config/${name}.json" << EOF
{
    "source": "${name}.bal",
    "output": {}
}
EOF

# Create an empty .bal file in the source directory
touch "$script_dir/source/${name}.bal"

echo "Created files successfully:"
echo "- $script_dir/config/${name}.json"
echo "- $script_dir/source/${name}.bal"