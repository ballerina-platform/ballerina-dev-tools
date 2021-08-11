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
import { Link, appType } from '../Router'
import { getFirstLine, scrollAndHighlight } from "./helper"
import Layout from "./layout"
import Markdown from "./markdown"
import { Dropdown } from 'semantic-ui-react'

const ModuleView = (props) => {
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
        <Layout {...props} title={"API Docs : " + props.module.id} pageType="module">

            <div>
                <h1>{props.module.orgName}/{props.module.id}
                    {(props.moduleVersions == null || props.moduleVersions.length == 1) && <span className="package-version"> : {props.module.version}</span>}
                    {props.moduleVersions != null && props.moduleVersions.length > 1 && 
                    <Dropdown className="module-version" text={props.module.version}>
                        <Dropdown.Menu>
                            {props.moduleVersions.map(item => (
                                <Link key={item} className="item" to={"/" + props.module.orgName + "/" + props.module.id + "/" + item}>{item}</Link>
                            ))}
                        </Dropdown.Menu>
                    </Dropdown>}
                </h1>

                <Markdown text={props.module.description} />

                {props.module.relatedModules.length > 1 &&
                    <section id="modules" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Modules</h2>
                            <p>[{props.module.relatedModules.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.module.relatedModules.map(item => (
                                    <tr key={item.id}>
                                        <td className="module-title truncate objectTypes" id={item.id} title={item.id}>
                                            <Link className={item.isDeprecated ? "strike records" : "records"} to={`/${item.orgName}/${item.id}/${item.version}`}>{item.id}</Link>
                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <Markdown text={item.summary} />
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }

                {props.module.listeners.length > 0 &&
                    <section id="listeners" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Listeners</h2>
                            <p>[{props.module.listeners.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.module.listeners.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate objectTypes" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike records" : "records"} to={`/${props.module.orgName}/${props.module.id}/${props.module.version}/listeners/${item.name}`}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <Markdown text={getFirstLine(item.description)} />
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }

                {props.module.clients.length > 0 &&
                    <section id="clients" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Clients</h2>
                            <p>[{props.module.clients.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.module.clients.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate clients" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike clients" : "clients"} to={`/${props.module.orgName}/${props.module.id}/${props.module.version}/clients/${item.name}`}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <Markdown text={getFirstLine(item.description)} />
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }

                {props.module.functions.length > 0 &&
                    <section id="functions" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Functions</h2>
                            <p>[{props.module.functions.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.module.functions.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate functions" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike functions" : "functions"} to={`/${props.module.orgName}/${props.module.id}/${props.module.version}/functions#${item.name}`}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <Markdown text={getFirstLine(item.description)} />
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }

                {props.module.classes.length > 0 &&
                    <section id="classes" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Classes</h2>
                            <p>[{props.module.classes.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.module.classes.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate classes" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike classes" : "classes"} to={`/${props.module.orgName}/${props.module.id}/${props.module.version}/classes/${item.name}`}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <Markdown text={getFirstLine(item.description)} />
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }

                {props.module.objectTypes != null && props.module.objectTypes.length > 0 &&
                    <section id="objectTypes" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Object Types </h2>
                            <p>[{props.module.objectTypes.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.module.objectTypes.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate objectTypes" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike objectTypes" : "objectTypes"} to={`/${props.module.orgName}/${props.module.id}/${props.module.version}/objectTypes/${item.name}`}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <Markdown text={getFirstLine(item.description)} />
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }

                {props.module.records.length > 0 &&
                    <section id="records" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Records </h2>
                            <p>[{props.module.records.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.module.records.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate records" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike records" : "records"} to={`/${props.module.orgName}/${props.module.id}/${props.module.version}/records/${item.name}`}>{item.name}</Link>
                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <Markdown text={getFirstLine(item.description)} />
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }

                {props.module.constants.length > 0 &&
                    <section id="constants" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Constants</h2>
                            <p>[{props.module.constants.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.module.constants.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate constants" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike constants" : "constants"} to={`/${props.module.orgName}/${props.module.id}/${props.module.version}/constants#${item.name}`}>{item.name}</Link>
                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <Markdown text={getFirstLine(item.description)} />
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }

                {props.module.enums.length > 0 &&
                    <section id="enums" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Enums</h2>
                            <p>[{props.module.enums.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.module.enums.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate constants" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike constants" : "constants"} to={`/${props.module.orgName}/${props.module.id}/${props.module.version}/enums/${item.name}`}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <Markdown text={getFirstLine(item.description)} />
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }

                {props.module.annotations.length > 0 &&
                    <section id="annotations" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Annotations</h2>
                            <p>[{props.module.annotations.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.module.annotations.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate annotations" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike annotations" : "annotations"} to={`/${props.module.orgName}/${props.module.id}/${props.module.version}/annotations#${item.name}`}>{item.name}</Link>
                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <Markdown text={getFirstLine(item.description)} />
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }

                {props.module.types.length > 0 &&
                    <section id="types" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Types</h2>
                            <p>[{props.module.types.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.module.types.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate types" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike types" : "types"} to={`/${props.module.orgName}/${props.module.id}/${props.module.version}/types#${item.name}`}>{item.name}</Link>
                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <Markdown text={getFirstLine(item.description)} />
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }

                {props.module.variables != null && props.module.variables.length > 0 &&
                    <section id="variables" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Variables</h2>
                            <p>[{props.module.variables.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.module.variables.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate constants" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike constants" : "constants"} to={`/${props.module.orgName}/${props.module.id}/${props.module.version}/variables#${item.name}`}>{item.name}</Link>
                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <Markdown text={getFirstLine(item.description)} />
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }
                {props.module.errors.length > 0 &&
                    <section id="errors" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Errors</h2>
                            <p>[{props.module.errors.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.module.errors.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate errors" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike errors" : "errors"} to={`/${props.module.orgName}/${props.module.id}/${props.module.version}/errors#${item.name}`}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <Markdown text={getFirstLine(item.description)} />
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }
            </div>
        </Layout>
    );
}

export default ModuleView;
