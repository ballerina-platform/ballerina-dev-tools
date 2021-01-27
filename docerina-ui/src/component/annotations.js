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
import { getTypeLabel, scrollAndHighlight } from "./helper"
import Layout from "./layout"
import { appType } from '../Router'
import Markdown from "./markdown"

const Annotations = (props) => {

    useEffect(() => {
        if (appType == "react" && props.history.location.hash != "") {
            scrollAndHighlight(props.history.location.hash);
        } else if (appType == "next" && location.hash != "") {
            scrollAndHighlight(location.hash);
        } else {
            window.scrollTo(0, 0);
        }
        $('.ui.accordion.annotations').accordion('open', 0);
    });

    return (
        <Layout {...props} title={"API Docs Annotations"} pageType="annotations">

            <section>
                <h1 className="capitalize">Annotations</h1>

                <div className="annotations">
                    <div className="fields-listing">
                        <ul>
                            {props.annotations.map(item => (
                                <div key={item.name}>
                                    <li id={item.name}>
                                        <b className={item.isDeprecated ? "strike" : ""}>{item.name} </b>
                                        <span className="type">{item.type != null && getTypeLabel(item.type)} </span><img className="attach-icon" src="/html-template-resources/images/attach.svg" />
                                        {item.attachmentPoints}
                                    </li>
                                    {item.isDeprecated == true &&
                                        <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                    }
                                    <Markdown text={item.description} />
                                </div>
                            ))}
                        </ul>
                    </div>
                </div>
            </section>
        </Layout>
    );
}

export default Annotations;
