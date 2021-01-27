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

const BClass = (props) => {

    useEffect(() => {
        if (appType == "react" && props.history.location.hash != "") {
            scrollAndHighlight(props.history.location.hash);
        } else if (appType == "next" && location.hash != "") {
            scrollAndHighlight(location.hash);
        } else {
            window.scrollTo(0, 0);
        }
        $('.ui.accordion.classes').accordion('open', 0);
    });

    let bClass = props.bClass;

    return (
        <Layout {...props} title={"API Docs Class: " + bClass.name}>

            <section className="construct-page">
                {bClass != null &&
                    <section>
                        <h1>Class: <span className={bClass.isDeprecated ? "strike" : ""}>{bClass.name}</span></h1>
                        {
                            bClass.isDeprecated == true &&
                            <div className="ui orange horizontal label">Deprecated</div>
                        }
                        {
                            bClass.isIsolated == true &&
                            <div className="ui horizontal label">Isolated</div>
                        }
                        {
                            bClass.isReadOnly == true &&
                            <div className="ui horizontal label">Read Only</div>
                        }
                        <Markdown text={bClass.description} />
                        <div className="constants">
                            <div className="method-sum">

                                {bClass.initMethod != null && <InitMethod initMethod={bClass.initMethod} />}
                                {bClass.otherMethods != null && bClass.otherMethods.length > 0 &&
                                    <section className="method-list">
                                        <h2>Methods</h2>
                                        <div className="method-list">
                                            <MethodTable {...props} methods={bClass.otherMethods} />
                                        </div>
                                    </section>
                                }
                                {bClass.fields != null && bClass.fields.length > 0 &&
                                    <section className="fields-list">
                                        <h2>Fields</h2>
                                        <div>
                                            <Fields fields={bClass.fields} />
                                        </div>
                                    </section>
                                }
                            </div>
                            <div>
                                {bClass.otherMethods != null && bClass.otherMethods.length > 0 &&
                                    bClass.otherMethods.map(item => (
                                        <div key={item.name}><Method method={item} /></div>
                                    ))
                                }
                            </div>
                        </div>
                    </section>
                }
            </section>
        </Layout>
    );
}

export default BClass;
