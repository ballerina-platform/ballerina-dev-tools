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
import Layout from "./layout"
import { appType } from '../Router'
import Markdown from "./markdown"

const Record = (props) => {

    useEffect(() => {
        if (appType == "react" && props.history.location.hash != "") {
            scrollAndHighlight(props.history.location.hash);
        } else if (appType == "next" && location.hash != "") {
            scrollAndHighlight(location.hash);
        } else {
            window.scrollTo(0, 0);
        }
        $('.ui.accordion.records').accordion('open',0);
    });
    let record = props.record;
    
    return (
        <Layout {...props} title={"API Docs - " + props.module.id + " Record: " + record.name}>
        <section className="construct-page">
            {record != null &&
                <section>
                    <h1>Record: <span className={record.isDeprecated ? "strike" : ""}>{record.name}</span></h1>
                    {
                        record.isDeprecated == true &&
                        <div className="ui orange horizontal label">Deprecated</div>
                    }
                    <Markdown text={record.description} />
                    {
                        record.isClosed == true &&
                        <div className="ui horizontal label" data-tooltip="A record with a fixed set of fields" data-position="top left">Closed record</div>
                    }
                    <div className="constants">
                        {record.fields.length == 0 && <p>This record doesn't contain any fields.</p>}
                        {record.fields.length != 0 &&
                            <section>
                                <h2>Fields</h2>
                                <Fields fields={record.fields} />
                            </section>
                        }
                    </div>
                </section>
            }
        </section>
        </Layout>
    );
}

export default Record;
