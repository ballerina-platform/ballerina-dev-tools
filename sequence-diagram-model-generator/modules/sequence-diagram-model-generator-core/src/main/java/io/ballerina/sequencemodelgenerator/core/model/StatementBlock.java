/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

/**
 * Represents the annotated comment statements of type //@sq-comment: text.
 * This special comment will be used to add comments to the sequence diagram,
 * which will explain a certain statement/statement block.
 *
 * @since 2201.8.5
 */
public class StatementBlock extends DElement {

    private String statementBlockText;

    public void setStatementBlockText(String statementBlockText) {
        this.statementBlockText = statementBlockText;
    }

    public String getStatementBlockText() {
        return statementBlockText;
    }

    public StatementBlock(LineRange location) {
        super("StatementBlock", false, location);
    }

}
