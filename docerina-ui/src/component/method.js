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
import Markdown from "./markdown"
import { Link } from '../Router'

const Method = (props) => {
    return (
        <div className="method-content construct-page">
            <div className="main-method-title" id={props.method.isResource ?
                props.method.accessor + '-' + props.method.resourcePath.replace(/[^\w\s]/gi, '-')
                : props.method.name} title={props.method.isResource ?
                props.method.accessor + '-' + props.method.resourcePath.replace(/[^\w\s]/gi, '-')
                : props.method.name}>

                <h2 className={props.method.isDeprecated ? "strike" : ""}> {props.method.isResource ?
                    <><i>{props.method.accessor}</i> {props.method.resourcePath}</>
                    : props.method.name} </h2>
            </div>
            <div>
                <pre className="method-signature">
                    <code className="break-spaces"><span className="token keyword">function</span> {props.method.isResource ?
                        <><i>{props.method.accessor}</i> {props.method.resourcePath}</>
                        : props.method.name}(
                        {props.method.parameters.length > 0 && props.method.parameters.map(param => { return [getTypeLabel(param.type), " " + param.name]; }).reduce((prev, curr) => [prev, ', ', curr])})
                        {props.method.returnParameters.length > 0 && <span> <span className="token keyword">returns</span> {getTypeLabel(props.method.returnParameters[0].type)}</span>}
                    </code>
                </pre>
            </div>
            <div className="function-desc">
                {
                    props.method.isDeprecated == true &&
                    <div className="ui orange horizontal label">Deprecated</div>
                }
                {
                    props.method.isIsolated == true &&
                    <div className="ui horizontal label">Isolated Function</div>
                }
                {
                    props.method.isRemote == true &&
                    <div className="ui horizontal label">Remote Function</div>
                }
                {
                    props.method.isResource == true &&
                    <div className="ui horizontal label">Resource Function</div>
                }
                <Markdown text={props.method.description} />
                {props.method.inclusionType != null && <p>Method included from <span data-tooltip="Type inclusion" data-position="top left">*</span>{getTypeLabel(props.method.inclusionType)}</p>}
            </div>
            {props.method.inclusionType == null &&
                <>
                    {props.method.parameters.length > 0 &&
                        <div className="parameters">
                            <h3 className="param-title">Parameters</h3>
                            {props.method.parameters.map(item => (
                                <div key={item.name} className="params-listing">
                                    <ul>
                                        <li>
                                            <span className={item.isDeprecated ? "strike" : ""}>{item.name}</span>
                                            <span className="type">  {getTypeLabel(item.type)} </span>
                                            {item.defaultValue != "" && <span className="default"> (default {item.defaultValue})</span>}
                                        </li>
                                        {
                                            item.isDeprecated == true &&
                                            <div className="ui orange horizontal label">Deprecated</div>
                                        }
                                        <Markdown text={item.description} />

                                    </ul>
                                </div>
                            ))}
                        </div>
                    }

                    {props.method.returnParameters.length > 0 &&
                        <div className="returns-listing">
                            <h3 className="type">Return Type</h3> (<span className="type">{getTypeLabel(props.method.returnParameters[0].type)}</span>)
                            <Markdown text={props.method.returnParameters[0].description} />
                        </div>
                    }
                </>
            }
        </div>
    );
}

export default Method;
