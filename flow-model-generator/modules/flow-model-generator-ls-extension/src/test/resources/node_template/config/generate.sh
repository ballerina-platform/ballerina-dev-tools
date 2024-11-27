#!/bin/bash
# ---------------------------------------------------------------------------
#  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org)
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

PARENT_DIR="$(dirname "${BASH_SOURCE[0]}")"

check_file_exists() {
    if [ -f "$FILE_PATH" ]; then
        echo "File already exists: $FILE_PATH"
        exit 1
    fi
}

if [ $# -eq 1 ]; then
    NAME="$1"
    
    FILE_PATH="$PARENT_DIR/$NAME.json"
    check_file_exists

    echo '{
    "description": "Sample diagram node",
    "codedata": {
        "node": "'$(echo "$NAME" | tr '[:lower:]' '[:upper:]')'"
    },
    "output": {}
}' > "$FILE_PATH"

elif [ $# -eq 3 ]; then
    NAME="$1"
    LIBRARY="$2"
    CALL="$3"

    FILE_PATH="$PARENT_DIR/$NAME-$LIBRARY-$CALL.json"
    check_file_exists

    echo '{
    "description": "Sample diagram node",
    "codedata": {
        "node": "'$(echo "$NAME" | tr '[:lower:]' '[:upper:]')'",
        "library": "'${LIBRARY}'",
        "call": "'${CALL}'"
    },
    "output": {}
}' > "$FILE_PATH"

else
    echo "Invalid number of parameters"
    exit 1
fi
