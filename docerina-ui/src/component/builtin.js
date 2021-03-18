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
import BuiltinMethod from "./builtinmethod"
import Layout from "./layout"
import Markdown from "./markdown"

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

    return (
        <Layout {...props} title={"Ballerina Bultin Type: " + props.builtinType.name} pageType="builtin">
            <section>
                <h1 className="capitalize">Bultin Types: {props.builtinType.name}</h1>
                <div className="function-desc">
                    <Markdown text={props.builtinType.description} />
                </div>
                {props.langlib != null && <section>
                <br></br>
                <h2 className="capitalize">Functions</h2>
                <div className="constants">
                    <table className="ui very basic table">
                        <tbody>
                            {props.langlib.functions.map(item => (
                                <tr key={item.name}>
                                    <td title={item.name} width="30%" className="truncate">
                                        <Link to={`/builtin/${props.ballerinaShortVersion}/${props.builtinType.name}#${item.name}`} className={item.isDeprecated ? "strike functions" : "functions"} >{item.name}</Link>
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
                        {props.langlib.functions.map(item => (
                            <BuiltinMethod key={item.name} method={item} />
                        ))}
                    </div>
                </div>
            </section>}
            </section>

        </Layout>
    );
}

export default Bulitin;
