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

import React, { useEffect } from 'react'
import { getFirstLine, scrollAndHighlight, getPackageName } from "./helper"
import { Link, appType } from '../Router'
import Method from "./method"
import Layout from "./layout"

const Functions = (props) => {

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
        <Layout {...props} title={"API Docs Functions"} pageType="functions">

            <section>
                <h1 className="capitalize">Functions</h1>
                <div className="constants">
                    <table className="ui very basic table">
                        <tbody>
                            {props.functions.map(item => (
                                <tr key={item.name}>
                                    <td title={item.name} width="30%" className="truncate">
                                        <Link className={item.isDeprecated ? "strike functions" : "functions"} to={"/" + props.module.orgName + "/" + getPackageName(props.module.id) + "/" + props.module.version + "/" + props.module.id + "/functions#" + item.name}>{item.name}</Link>
                                    </td>
                                    <td width="70%">
                                        <div className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            {
                                                item.isIsolated == true &&
                                                <div className="ui horizontal label" data-tooltip="Isolated Function" data-position="top left">I</div>
                                            }
                                            <p>{getFirstLine(item.description)}</p>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                    <div>
                        {props.functions.map(item => (
                            <Method key={item.name} method={item} />
                        ))}
                    </div>
                </div>
            </section>
        </Layout>
    );
}

export default Functions;
