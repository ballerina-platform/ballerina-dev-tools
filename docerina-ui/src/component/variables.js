/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import React, { useEffect } from 'react'
import { getTypeLabel, scrollAndHighlight } from "./helper"
import Layout from "./layout"
import { appType } from '../Router'
import Markdown from "./markdown"

const ModuleVariables = (props) => {

    useEffect(() => {
        if (appType == "react" && props.history.location.hash != "") {
            scrollAndHighlight(props.history.location.hash);
        } else if (appType == "next" && location.hash != "") {
            scrollAndHighlight(location.hash);
        } else {
            window.scrollTo(0, 0);
        }
    });

    return (
        <Layout {...props} title={"API Docs Types "} pageType="variables">
            <section className="construct-page">
                <h1 className="capitalize">Variables</h1>
                {props.variables != null &&
                    <div className="types">
                        <div className="data-wrapper">
                            {props.variables.map(item => (
                                <div key={item.name} id={item.name} className="params-listing">
                                    <ul>
                                        <li>
                                            <b className={item.isDeprecated ? "strike" : ""}>{item.name} </b>
                                            <span className="type">{getTypeLabel(item.type, item.defaultValue)}</span>
                                        </li>
                                        {
                                            item.isDeprecated == true &&
                                            <div className="ui orange horizontal label">Deprecated</div>
                                        }
                                        <Markdown text={item.description} />
                                    </ul>
                                </div>
                            ))}
                        </div>
                    </div>
                }
            </section>
        </Layout>
    );
}

export default ModuleVariables;
