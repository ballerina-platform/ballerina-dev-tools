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

import React from "react";
import { Link } from '../Router'
import Layout from "./layout";
import Markdown from "./markdown"
import { getFirstLine } from "./helper"

const PackageIndex = (props) => {
    return (
        <section>
            <Layout {...props} title={"API Docs"} pageType="packageIndex">
                <Markdown text={props.packageDescription} />
                <h1 className="capitalize">Packages</h1>
                <table className="ui very basic table">
                    <tbody>
                        {props.packages.map((item) => (
                            <tr>
                                <td className="module-title modules"><Link to={"/" + item.orgName + "/" + item.name + "/" + item.version}>{item.name}</Link></td>
                                <td className="module-desc"><p>{item.version}</p></td>
                                <td className="module-desc"><Markdown text={getFirstLine(item.summary)} /></td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </Layout>
        </section>
    );
}

export default PackageIndex;
