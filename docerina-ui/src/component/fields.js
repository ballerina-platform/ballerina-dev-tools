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
import { getTypeLabel } from "./helper"
import { Link } from '../Router'
import Markdown from "./markdown"

const Fields = (props) => {
    return (
        <div className="fields-listing">
            <ul>
                {props.fields.map(item => (
                    <section key={item.name} id={item.name}>
                        <li>
                            {item.inclusionType == null &&
                                <>
                                    <span className={item.isDeprecated ? "strike" : ""}>{item.name} </span>
                                    {item.isReadOnly && <span>readonly </span>}
                                    {getTypeLabel(item.type, item.defaultValue)}
                                </>
                            }
                            {item.inclusionType != null &&
                                <>
                                    <span className={item.isDeprecated ? "strike" : ""}>
                                        <span>Fields Included from </span>
                                        <span data-tooltip="Type inclusion" data-position="top left">*</span>
                                        {getTypeLabel(item.type, item.defaultValue)}</span>
                                </>
                            }
                        </li>
                        {item.inclusionType == null && <Markdown text={item.description} />}
                        {item.inclusionType != null &&
                            <>
                                <ul>
                                    {item.type.memberTypes.map(type => {
                                        return(
                                            <li>{type.name} 
                                            {/* {getTypeLabel(type.elementType, type.defaultValue)} */}
                                            </li>
                                        );
                                    })}
                                </ul>
                            </>
                        }
                    </section>
                ))}
            </ul>
        </div>
    );
}

export default Fields;
