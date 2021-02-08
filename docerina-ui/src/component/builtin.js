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
import { getFirstLine, scrollAndHighlight } from "./helper"
import { Link, appType } from '../Router'
import Layout from "./layout"
import Markdown from "./markdown"
import { languages } from 'prism-react-renderer/prism'

const Bulitin = (props) => {

    useEffect(() => {
        if (appType == "react" && props.history.location.hash != "") {
            scrollAndHighlight(props.history.location.hash);
        } else if (appType == "next" && location.hash != "") {
            scrollAndHighlight(location.hash);
        } else {
            window.scrollTo(0, 0);
        }
        $('.ui.accordion.functions').accordion('open', 0);
    });

    props.builtinTypesAndKeywords.forEach(element => {
        let module = props.searchData.modules.filter((item) => {
            return item.id.includes("lang." + element.name);
        })[0];
        element.module = module;
    });

    console.log(props.builtinTypesAndKeywords);

    return (
        <Layout {...props} title={"Ballerina Bultin types and keywords"} pageType="builtin">

            <section>
                <h1 className="capitalize">Bultin Types and Keywords</h1>
                <div className="constants">
                    <table className="ui very basic table">
                        <tbody>
                            {props.builtinTypesAndKeywords.map(item => (
                                <tr key={item.name}>
                                    <td title={item.name} width="30%" className="truncate">
                                        <Link className="functions" to={"/builtin#" + item.name}>{item.name}</Link>
                                    </td>
                                    <td width="70%">
                                        <div className="module-desc">
                                            <p>{getFirstLine(item.description)}</p>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                    <div>
                        {props.builtinTypesAndKeywords.map(item => (
                            <div key={item.name} className="method-content construct-page">
                                <div className="main-method-title" id={item.name} title={item.name}>
                                    <h2> {item.name} </h2>
                                </div>
                                <div className="function-desc">
                                    <Markdown text={item.description} />
                                    {item.module != null && <Link to={`/${item.module.orgName}/${item.module.id}/${item.module.version}/${item.module.id}/functions`}>View {item.name} functions.</Link>}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </section>
        </Layout>
    );
}

export default Bulitin;
