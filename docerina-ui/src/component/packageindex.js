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
import Markdown from "./markdown"
import { getFirstLine, scrollAndHighlight } from "./helper"

const PackageIndex = (props) => {

    console.log(props);

    useEffect(() => {
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
            <Layout {...props} title={"API Docs"} pageType="packageIndex">
                <h1 className="capitalize">Release: {props.releaseVersion}</h1>
                <Markdown text={props.packageDescription} />
                <h2 className="capitalize">Packages</h2>
                <table className="ui very basic table" id="packages">
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
                <h2 className="capitalize">Builtin Types</h2>
                <table className="ui very basic table" id="builtin">
                    <tbody>
                        {props.builtinTypesAndKeywords.map(item => (
                            <tr key={item.name}>
                                <td title={item.name} width="30%" className="truncate">
                                    <Link className="functions" to={`/builtin/${props.ballerinaShortVersion}/${item.name}`}>{item.name}</Link>
                                </td>
                                <td width="70%">
                                    <div className="module-desc">
                                        <p>{getFirstLine(item.description)}</p>
                                    </div>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </Layout>
        </section>
    );
}

export default PackageIndex;
