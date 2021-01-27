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

import * as React from "react";
import { getFirstLine, getTypeLabel } from "./helper"
import { Link } from '../Router'

const MethodTable = (props) => {

    return (
        <section>
            <table className="ui very basic table">
                <tbody>
                    {props.methods.map(item => (
                        <tr key={item.name}>
                            <td className={item.isDeprecated ? "module-title strike" : "module-title"} title={item.name}>
                                <Link to={"/" + props.module.orgName + "/" + props.package.name + "/" + props.module.version + "/" + props.module.id + "/" + props.pageType + "/" + props.match.params.constructName + "#" + item.name}>{item.name}</Link>
                            </td>
                            <td className="module-desc">
                                {
                                    item.isDeprecated == true &&
                                    <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                }
                                <p>{item.inclusionType == null && getFirstLine(item.description)}</p>
                                {item.inclusionType != null && <p>Method included from {getTypeLabel(item.inclusionType)}</p>}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </section>
    );
}

export default MethodTable;
