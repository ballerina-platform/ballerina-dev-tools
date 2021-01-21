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

import React, { useEffect } from "react";
import { Link, appType } from '../Router'
import Layout from "./layout";
import { getPackageName, scrollAndHighlight } from "./helper"
import Markdown from "./markdown"

const PackageView = (props) => {
    useEffect(() => {
        console.log("useeffect");
        if (appType == "react" && props.history.location.hash != "") {
            scrollAndHighlight(props.history.location.hash);
        } else if (appType == "next" && location.hash != "") {
            scrollAndHighlight(location.hash);
        } else {
            window.scrollTo(0, 0);
        }
    });

    return (
        <section>
            <Layout {...props} title={"API Docs"} pageType="package">
                <h1>Package: {props.package.orgName}/{props.package.name}</h1>
                <h1>Package Version: {props.package.version}</h1>
                <Markdown text={props.package.description} />

                <h1 className="capitalize">Modules</h1>
                <section id="modules">
                    <table id="modules" className="ui very basic table">
                        <tbody>
                            {props.package.modules.map((item) => (
                                <tr>
                                    <td className="module-title modules"><Link to={"/" + item.orgName + "/" + getPackageName(item.id) + "/" + item.version + "/" + item.id}>{item.id}</Link></td>
                                    <td className="module-desc"><Markdown text={item.summary} /></td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </section>
            </Layout>
        </section>
    );
}

export default PackageView;
