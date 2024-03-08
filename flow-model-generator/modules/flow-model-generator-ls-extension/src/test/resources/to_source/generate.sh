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

[ -z "$1" ] && { echo "Name not provided"; exit 1; } || NAME="${1}_node"
PARENT_DIR="$(dirname "${BASH_SOURCE[0]}")"

PREFIX_FILE="$PARENT_DIR/$NAME*"

if ls $PREFIX_FILE 1> /dev/null 2>&1; then
    LAST_FILE=$(ls -v $PREFIX_FILE | tail -n 1)
    NUMBER=$(echo "$LAST_FILE" | sed -n 's/.*'$NAME'\([0-9]*\)\.json/\1/p')
    ((NUMBER++))
else
    NUMBER=1
fi

echo '{
    "description": "Sample diagram node",
    "diagram": {},
    "output": []
}' > ""${NAME}${NUMBER}.json""
