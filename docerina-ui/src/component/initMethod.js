/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

import React from "react";
import { getTypeLabel } from "./helper"
import Markdown from "./markdown"

const InitMethod = (props) => {
    return (
        <div>
            {props.initMethod.description != "" &&

                <section className="method-list">

                    <h2>Constructor</h2>
                    {props.initMethod.description != null &&
                        <Markdown text={props.initMethod.description} />
                    }
                    <pre className="method-signature">
                        <code className="break-spaces"><span className="token keyword">init</span> ({props.initMethod.parameters.length > 0 && props.initMethod.parameters.map(param => { return [getTypeLabel(param.type), " " + param.name]; }).reduce((prev, curr) => [prev, ', ', curr])})</code>
                    </pre>
                    <div className="data-wrapper">
                        {props.initMethod.parameters.map(item => (
                            <div key={item.name} className="params-listing">
                                <ul>
                                    <li> <b>{item.name}</b><span className="type"> {getTypeLabel(item.type)}</span> {item.defaultValue}</li>
                                    <li>
                                        <Markdown text={item.description} />
                                    </li>
                                </ul>
                            </div>
                        ))}
                    </div>
                </section>
            }
        </div>
    );
}

export default InitMethod;
