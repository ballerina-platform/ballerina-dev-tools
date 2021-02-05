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
import InitMethod from "./initMethod"
import MethodTable from "./methodTable"
import Method from "./method"
import Layout from "./layout"
import { scrollAndHighlight } from "./helper"
import { appType } from '../Router'
import Markdown from "./markdown"

const Listener = (props) => {

    useEffect(() => {
        if (appType == "react" && props.history.location.hash != "") {
            scrollAndHighlight(props.history.location.hash);
        } else if (appType == "next" && location.hash != "") {
            scrollAndHighlight(location.hash);
        } else {
            window.scrollTo(0, 0);
        }
        $('.ui.accordion.listeners').accordion('open', 0);

    });

    let listener = props.listener;

    return (

        <Layout {...props} title={"API Docs Listener: " + listener.name}>

            <section className="construct-page">
                {listener != null &&
                    <section>
                        <h1>Listener: <span className={listener.isDeprecated ? "strike listeners" : "listeners"}>{listener.name}</span></h1>
                        {
                            listener.isDeprecated == true &&
                            <div className="ui orange horizontal label">Deprecated</div>
                        }
                        {
                            listener.isIsolated == true &&
                            <div className="ui horizontal label">Isolated</div>
                        }
                        {
                            listener.isReadOnly == true &&
                            <div className="ui horizontal label">Read Only</div>
                        }
                        <Markdown text={listener.description} />
                        <div className="constants">
                            <div className="method-sum">

                                {listener.initMethod != null && <InitMethod initMethod={listener.initMethod} />}

                                {listener.lifeCycleMethods != null && listener.lifeCycleMethods.length > 0 &&
                                    <section className="method-list">
                                        <h2>LifeCycle Methods</h2>
                                        <div>
                                            <MethodTable {...props} methods={listener.lifeCycleMethods} />
                                        </div>
                                    </section>
                                }
                                {listener.otherMethods != null && listener.otherMethods.length > 0 &&
                                    <section className="method-list">
                                        <h2>Methods</h2>
                                        <div>
                                            <MethodTable {...props} methods={listener.otherMethods} />
                                        </div>
                                    </section>
                                }
                                {listener.fields.length > 0 &&
                                    <section className="fields-list">
                                        <h2>Fields</h2>
                                        <div>
                                            <Fields fields={listener.fields} />
                                        </div>
                                    </section>
                                }
                            </div>
                            {listener.lifeCycleMethods != null &&
                                listener.lifeCycleMethods.map(item => (
                                    <div key={item.name}><Method method={item} /></div>
                                ))
                            }
                            {listener.otherMethods != null &&
                                listener.otherMethods.map(item => (
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

export default Listener;
