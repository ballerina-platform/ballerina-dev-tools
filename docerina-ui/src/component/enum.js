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
import Layout from "./layout"
import { appType } from '../Router'
import Markdown from "./markdown"

const Enum = (props) => {

    useEffect(() => {
        if (appType == "react" && props.history.location.hash != "") {
            scrollAndHighlight(props.history.location.hash);
        } else if (appType == "next" && location.hash != "") {
            scrollAndHighlight(location.hash);
        } else {
            window.scrollTo(0, 0);
        }
        $('.ui.accordion.enums').accordion('open', 0);
    });
    let bEnum = props.bEnum;

    return (
        <Layout {...props} title={"API Docs - " + props.module.id + " Enum: " + bEnum.name}>
            <section className="construct-page">
                {bEnum != null &&
                    <section>
                        <h1>Enum: <span className={bEnum.isDeprecated ? "strike" : ""}>{bEnum.name}</span></h1>
                        {
                            bEnum.isDeprecated == true &&
                            <div className="ui orange horizontal label">Deprecated</div>
                        }
                        <Markdown text={bEnum.description} />
                        <br />
                        <div className="constants">
                            {bEnum.members.length != 0 &&
                                <section>
                                    <h2>Members</h2>
                                    <div className="fields-listing">
                                        <table className="ui very basic table">
                                            <tbody>
                                                {bEnum.members.map(item => (
                                                    <tr key={item.name}>
                                                        <td title={item.name} className="enum-name truncate">
                                                            <span className={item.isDeprecated ? "strike" : ""}>{item.name}</span>
                                                        </td>
                                                        <td><Markdown text={item.description} /></td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                </section>
                            }
                        </div>
                    </section>
                }
            </section>
        </Layout>
    );
}

export default Enum;
