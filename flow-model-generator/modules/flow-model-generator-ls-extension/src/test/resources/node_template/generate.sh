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

[ -z "$1" ] && { echo "Name not provided"; exit 1; } || NAME="${1}"
PARENT_DIR="$(dirname "${BASH_SOURCE[0]}")"

FILE_PATH="$PARENT_DIR/$NAME.json"
if [ -f "$FILE_PATH" ]; then
    echo "File already exists: $FILE_PATH"
    exit 1
fi

echo '{
    "description": "Sample diagram node",
    "kind": "'$(echo "$NAME" | tr '[:lower:]' '[:upper:]')'",
    "output": {}
}' > ""${NAME}.json""
