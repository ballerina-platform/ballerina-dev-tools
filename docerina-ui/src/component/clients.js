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

const Client = (props) => {

    useEffect(() => {
        if (appType == "react" && props.history.location.hash != "") {
            scrollAndHighlight(props.history.location.hash);
        } else if (appType == "next" && location.hash != "") {
            scrollAndHighlight(location.hash);
        } else {
            window.scrollTo(0, 0);
        }
        $('.ui.accordion.clients').accordion('open', 0);
    });

    let client = props.client;

    return (

        <Layout {...props} title={"API Docs Client: " + client.name}>

            <section className="construct-page">
                {client != null &&
                    <section>
                        <h1>Client: <span className={client.isDeprecated ? "strike clients" : "clients"}>{client.name}</span></h1>
                        {
                            client.isDeprecated == true &&
                            <div className="ui orange horizontal label">Deprecated</div>
                        }
                        {
                            client.isIsolated == true &&
                            <div className="ui horizontal label">Isolated</div>
                        }
                        {
                            client.isReadOnly == true &&
                            <div className="ui horizontal label">Read Only</div>
                        }
                        <Markdown text={client.description} />
                        <div className="constants">
                            <div className="method-sum">

                                {client.initMethod != null && <InitMethod initMethod={client.initMethod} />}

                                {client.remoteMethods != null && client.remoteMethods.length > 0 &&
                                    <section className="method-list">
                                        <h2>Remote Methods</h2>
                                        <div>
                                            <MethodTable {...props} methods={client.remoteMethods} />
                                        </div>
                                    </section>
                                }
                                {client.otherMethods != null && client.otherMethods.length > 0 &&
                                    <section className="method-list">
                                        <h2>Methods</h2>
                                        <div className="method-list">
                                            <MethodTable {...props} methods={client.otherMethods} />
                                        </div>
                                    </section>
                                }
                                {client.fields != null && client.fields.length > 0 &&
                                    <section className="fields-list">
                                        <h2>Fields</h2>
                                        <div>
                                            <Fields fields={client.fields} />
                                        </div>
                                    </section>
                                }
                            </div>
                            {client.remoteMethods != null &&
                                client.remoteMethods.map(item => (
                                    <div key={item.name}><Method method={item} /></div>
                                ))
                            }
                            {client.otherMethods != null &&
                                client.otherMethods.map(item => (
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

export default Client;
