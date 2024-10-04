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

# Read input values
[ -z "$1" ] && { echo "Name not provided"; exit 1; } || NAME="$1"
PARENT_DIR="$(dirname "${BASH_SOURCE[0]}")"

# Check if source file exists
SOURCE_FILE="$PARENT_DIR/source/$NAME.bal"
[ ! -f "$SOURCE_FILE" ] && touch "$SOURCE_FILE"

# Check if config file exists
cd "$PARENT_DIR/config" || exit 1
PREFIX_FILE="$PARENT_DIR/$NAME*"

# Create new config file
if ls $PREFIX_FILE 1> /dev/null 2>&1; then
    LAST_FILE=$(ls -v $PREFIX_FILE | grep "$NAME"'[0-9]*\.json' | sort -t't' -k2,2n | tail -n 1)
    NUMBER=$(echo "$LAST_FILE" | sed -n 's/.*'$NAME'\([0-9]*\)\.json/\1/p')
    PREV_FILE="${NAME}${NUMBER}.json"
    ((NUMBER++))
    cp "$PREV_FILE" "${NAME}${NUMBER}.json"
else
    echo '{
  "start": {
    "line": 1,
    "offset": 0
  },
  "end": {
    "line": 3,
    "offset": 1
  },
  "source": "'$NAME.bal'",
  "description": "Tests a simple diagram flow",
  "diagram": {}
}' > "${NAME}1.json"
fi
