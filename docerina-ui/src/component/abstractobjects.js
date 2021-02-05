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
import Fields from "./fields"
import MethodTable from "./methodTable"
import Method from "./method"
import Layout from "./layout"
import { scrollAndHighlight } from "./helper"
import { appType } from '../Router'
import Markdown from "./markdown"

const AbstractObject = (props) => {

    useEffect(() => {
        if (appType == "react" && props.history.location.hash != "") {
            scrollAndHighlight(props.history.location.hash);
        } else if (appType == "next" && location.hash != "") {
            scrollAndHighlight(location.hash);
        } else {
            window.scrollTo(0, 0);
        }
        $('.ui.accordion.abstractObjects').accordion('open', 0);

    });

    let abstractObject = props.abstractObject;

    return (
        <Layout {...props} title={"API Docs Abstract Object: " + abstractObject.name} >

            <section className="construct-page">
                {abstractObject != null &&
                    <section>
                        <h1>Abstract Object: <span className={abstractObject.isDeprecated ? "strike" : ""}>{abstractObject.name}</span></h1>
                        {
                            abstractObject.isDeprecated == true &&
                            <div className="ui orange horizontal label">Deprecated</div>
                        }
                        <Markdown text={abstractObject.description} />
                        <div className="constants">
                            <div className="method-sum">

                                {abstractObject.methods != null && abstractObject.methods.length > 0 &&
                                    <section className="method-list">
                                        <h2>Methods</h2>
                                        <div className="method-list">
                                            <MethodTable {...props} methods={abstractObject.methods} />
                                        </div>
                                    </section>
                                }
                                {abstractObject.fields != null && abstractObject.fields.length > 0 &&
                                    <section className="fields-list">
                                        <h2>Fields</h2>
                                        <div>
                                            <Fields fields={abstractObject.fields} />
                                        </div>
                                    </section>
                                }
                            </div>
                            {abstractObject.methods != null &&
                                abstractObject.methods.map(item => (
                                    <div key={item.name}><Method method={item} /></div>
                                ))
                            }
                        </div>
                    </section>
                }
            </section>
        </Layout>
    );
}

export default AbstractObject;
