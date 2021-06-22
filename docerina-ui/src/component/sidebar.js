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
import { Dropdown } from 'semantic-ui-react'
import { Link } from '../Router'
import ConstructList from "./constructlist"

const getModuleConstructTypes = (props) => {
    let module = props.module;
    return (<div className="menu">
        {module.listeners != null && module.listeners.length > 0 &&
            <ConstructList {...props} listType="listeners" />
        }
        {module.clients != null && module.clients.length > 0 &&
            <ConstructList {...props} listType="clients" />
        }
        {module.functions != null && module.functions.length > 0 &&
            <ConstructList {...props} listType="functions" />
        }
        {module.classes != null && module.classes.length > 0 &&
            <ConstructList {...props} listType="classes" />
        }
        {module.objectTypes != null && module.objectTypes.length > 0 &&
            <ConstructList {...props} listType="objectTypes" />
        }
        {module.records != null && module.records.length > 0 &&
            <ConstructList {...props} listType="records" />
        }
        {module.enums != null && module.enums.length > 0 &&
            <ConstructList {...props} listType="enums" />
        }
        {module.constants != null && module.constants.length > 0 &&
            <ConstructList {...props} listType="constants" />
        }
        {module.annotations != null && module.annotations.length > 0 &&
            <ConstructList {...props} listType="annotations" />
        }
        {module.types != null && module.types.length > 0 &&
            <ConstructList {...props} listType="types" />
        }
        {module.variables != null && module.variables.length > 0 &&
            <ConstructList {...props} listType="variables" />
        }
        {module.errors != null && module.errors.length > 0 &&
            <ConstructList {...props} listType="errors" />
        }
        <div className="ui divider"></div>

    </div>);
}

const SideBar = (props) => {
    if (props.pageType == "404") {
        return (<></>);
    } else if (props.pageType == "orgModules") {
        return (
            <section>
                <div className="header">
                    <span>Organization</span>
                </div>
                <div className="menu">
                    <span className="item no-hover capitalize">{props.orgName}</span>
                </div>
                <div className="ui divider"></div>
            </section>);
    } else if (props.pageType == "moduleIndex") {
        return (
            <section>
                <div className="header">
                    <span>Distribution</span>
                </div>
                <div className="menu">
                    <span className="item no-hover capitalize">{props.releaseVersion}</span>
                </div>
                <div className="ui divider"></div>
                <div className="menu">
                    <Link className="item" to={`/#langlibs`}>Language Library</Link>
                    <Link className="item" to={`/#stdlibs`}>Standard Library</Link>
                </div>
            </section>);
    }
    if (props.type == "desktop") {
        return (
            <section>
                <div className="header">
                    <Link to={`/${props.module.orgName}/${props.module.id}/${props.module.version}`}>Module {props.module.id}</Link>
                </div>
                <div className="menu">
                    <span className="item no-hover">Version {props.module.version}</span>
                </div>
                <div className="ui divider"></div>
                {props.module.relatedModules.length > 1 &&
                    <>
                        <div className="header">
                            <Link to={`/${props.module.orgName}/${props.module.id}/${props.module.version}#modules"`}>Related Modules</Link>
                        </div>
                        <div className="menu">
                            {props.module.relatedModules.map(item => (
                                <Link key={item.id} to={`/${item.orgName}/${item.id}/${item.version}`} className={props.module.id == item.id ? "active item" : "item"}>{item.id}</Link>
                            ))}
                        </div>
                        <div className="ui divider"></div>
                    </>
                }
                {props.module != null &&
                    <section>

                        <div className="header">
                            <Link to={`/${props.module.orgName}/${props.module.id}/${props.module.version}`}>Module {props.module.id}</Link>
                        </div>
                        {getModuleConstructTypes(props)}

                    </section>
                }
            </section>
        );
    } else {
        let hasChildPages;
        if (props.pageType == "types" || props.pageType == "errors" || props.pageType == "annotations" || props.pageType == "constants" || props.pageType == "module") {
            hasChildPages = false;
        } else {
            hasChildPages = true;
        }
        return (<>
            {props.module.relatedModules.length > 1 &&
                <Dropdown className="capitalize item" text='Related Modules'>
                    <Dropdown.Menu>
                        {props.module.relatedModules.map(item => (
                            <Link key={item.id} to={`/${props.module.orgName}/${item.id}/${props.module.version}`} className={props.module.id == item.id ? "active item" : "item"}>{item.id}</Link>
                        ))}
                    </Dropdown.Menu>
                </Dropdown>
            }
            {hasChildPages &&
                <Dropdown className="capitalize item" text={props.pageType}>
                    <Dropdown.Menu>
                        <ConstructList {...props} type="mobile" data={props.module[props.pageType]} listType={props.pageType} moduleName={props.module.id} />
                    </Dropdown.Menu>
                </Dropdown>
            }
            <Dropdown className="capitalize item" text={props.module.id}>
                <Dropdown.Menu>
                    {props.module.listeners != null && props.module.listeners.length > 0 &&
                        <Link className="item" to={`/${props.module.orgName}/${props.module.id}/${props.module.version}#listeners`}>Listeners</Link>
                    }
                    {props.module.clients != null && props.module.clients.length > 0 &&
                        <Link className="item" to={`/${props.module.orgName}/${props.module.id}/${props.module.version}#clients`}>Clients</Link>
                    }
                    {props.module.functions != null && props.module.functions.length > 0 &&
                        <Link className="item" to={`/${props.module.orgName}/${props.module.id}/${props.module.version}#functions`}>Functions</Link>
                    }
                    {props.module.classes != null && props.module.classes.length > 0 &&
                        <Link className="item" to={`/${props.module.orgName}/${props.module.id}/${props.module.version}#classes`}>Classes</Link>
                    }
                    {props.module.objectTypes != null && props.module.objectTypes.length > 0 &&
                        <Link className="item" to={`/${props.module.orgName}/${props.module.id}/${props.module.version}#objectTypes`}>Object Types</Link>
                    }
                    {props.module.records != null && props.module.records.length > 0 &&
                        <Link className="item" to={`/${props.module.orgName}/${props.module.id}/${props.module.version}#records`}>Records</Link>
                    }
                    {props.module.enums != null && props.module.enums.length > 0 &&
                        <Link className="item" to={`/${props.module.orgName}/${props.module.id}/${props.module.version}#enums`}>Enums</Link>
                    }
                    {props.module.constants != null && props.module.constants.length > 0 &&
                        <Link className="item" to={`/${props.module.orgName}/${props.module.id}/${props.module.version}#constants`}>Constants</Link>
                    }
                    {props.module.annotations != null && props.module.annotations.length > 0 &&
                        <Link className="item" to={`/${props.module.orgName}/${props.module.id}/${props.module.version}#annotations`}>Annotations</Link>
                    }
                    {props.module.types != null && props.module.types.length > 0 &&
                        <Link className="item" to={`/${props.module.orgName}/${props.module.id}/${props.module.version}#types`}>Types</Link>
                    }
                    {props.module.variables != null && props.module.variables.length > 0 &&
                        <Link className="item" to={`/${props.module.orgName}/${props.module.id}/${props.module.version}#variables`}>Variables</Link>
                    }
                    {props.module.errors != null && props.module.errors.length > 0 &&
                        <Link className="item" to={`/${props.module.orgName}/${props.module.id}/${props.module.version}#errors`}>Errors</Link>
                    }
                </Dropdown.Menu>
            </Dropdown>
        </>);
    }
}

export default SideBar;
