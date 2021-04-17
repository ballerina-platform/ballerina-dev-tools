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
import { getConnector } from "./helper"
import { Accordion } from 'semantic-ui-react'

const ConstructList = (props) => {
    if (props.type == "desktop") {
        const panel = [
            {
                key: props.listType,
                title: props.listType,
                content: {
                    content: (
                        <div>
                            {props.module[props.listType].map(item => (
                                <Link title={item.name} key={item.name} className={props.match.params.constructName == item.name ? "active item" : "item"} to={`/${props.module.orgName}/${props.module.id}/${props.module.version}/${props.listType + getConnector(props.listType) + item.name}`}>{item.name}</Link>
                            ))}
                        </div>
                    ),
                },
            },
        ];
        return (
            <Accordion className="item" defaultActiveIndex={props.listType == props.pageType ? 0 : -1} panels={panel} />
        );
    } else {
        return (
            <>
                {props.module[props.listType].map(item => (
                    <Link key={item.name} className="item" to={`/${props.module.orgName}/${props.module.id}/${props.module.version}/${props.listType + getConnector(props.listType) + item.name}`}>{item.name}</Link>
                ))}
            </>
        )
    }
}

export default ConstructList;
