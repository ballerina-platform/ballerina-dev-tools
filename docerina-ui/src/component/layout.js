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
import SideBar from "./sidebar"
import { Head, rootPath, otherScripts, Link } from '../Router'
import { SearchList } from "./searchlist"
import { removeHtmlTags } from "./helper"

function toggleMenu() {
    $('#mobMenu').transition('slide down')
}

const Layout = (props) => {

    let hasChildPages;
    if (props.pageType == "functions" || props.pageType == "types" || props.pageType == "errors" || props.pageType == "annotations" || props.pageType == "constants") {
        hasChildPages = false;
    } else {
        hasChildPages = true;
    }

    useEffect(() => {
        $(document).ready(function () {
            $('.ui.accordion').accordion();
            $('.ui.dropdown').dropdown();
            $('#version-picker').dropdown('set selected', "swan-lake");
            $('#version-picker-mob').dropdown('set selected', "swan-lake");
        });
    })

    return (
        <div>
            <Head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <meta http-equiv="X-UA-Compatible" content="ie=edge" />
                <title>{props.title}</title>

                {props.module != null && <meta name="description" content={removeHtmlTags(props.module.summary)}></meta>}
                <meta name="keywords" content={props.module != null ? "ballerina,ballerina lang,api docs," + props.module.id : "ballerina,ballerina lang,api docs"} />

                {otherScripts}
            </Head>

            <div className="pusher">

                <div className="mobile-logo-div">
                    <div className="ui secondary menu">
                        <a href="/"><img className="mobile-logo" src={rootPath + "html-template-resources/images/ballerina-logo.png"}></img></a>
                        <div className="right item">
                            <button className="ui icon button" onClick={toggleMenu}>
                                <i className="large bars icon"></i>
                            </button>
                        </div>
                    </div>
                </div>

                <div className="toc">
                    <div className="ui visible left vertical sidebar menu">
                        <a href="/"><img className="logo" src={rootPath + "html-template-resources/images/ballerina-logo.png"}></img></a>

                        <SideBar {...props} type="desktop" />
                    </div>
                </div>

                <div className="content">
                    <div className="row nav-bar">
                        <div className="ui secondary menu">
                            <div className="search-bar">
                                <div className="ui category search item search-bar">
                                    <i className="search link icon"></i>
                                    <div className="ui transparent icon input">
                                        <input className="prompt" id="searchBox" type="text" placeholder="Search API Docs..." />
                                    </div>
                                </div>
                            </div>
                            <div className="right menu">
                                <a href="/learn" className="item active">Learn</a>
                                <a href="/learn/events" className="item">Events</a>
                                <a href="https://central.ballerina.io/" className="item">Central</a>
                                <a href="/community" className="item">Community</a>
                                <a href="https://blog.ballerina.io/" className="item">Blog</a>
                                <div className="ui dropdown item ballerina" id="version-picker">
                                    Version
                            <i className="dropdown icon"></i>
                                    <div className="menu">
                                        <a href={"https://ballerina.io/1.0/learn/api-docs/ballerina/" + (props.module != null ? props.module.id : "")} className="item" value="1.0">1.0</a>
                                        <a href={"https://ballerina.io/1.1/learn/api-docs/ballerina/" + (props.module != null ? props.module.id : "")} className="item" value="1.1">1.1</a>
                                        <a href={"https://ballerina.io/learn/api-docs/ballerina/" + (props.module != null ? props.module.id : "")} className="item" value="1.2">1.2</a>
                                        <a className="item active" value="swan-lake">Swan Lake</a>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div className="main-content-holder">
                        <div className="main-content">
                            <div id="mobMenu" className="mob-menu">
                                <div className="ui stackable secondary menu">
                                    <div className="item">
                                        <div className="search-bar">
                                            <div className="ui category search item search-bar">
                                                <i className="search link icon"></i>
                                                <div className="ui transparent icon input">
                                                    <input className="prompt" id="searchBoxMob" type="text" placeholder="Search API Docs..." />
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                    <SideBar {...props} type="mobile" />

                                    <div className="ui dropdown item" id="version-picker-mob">
                                        Version <i className="dropdown icon"></i>
                                        <div className="menu">
                                            <a href="https://ballerina.io/1.0/learn/api-docs/ballerina/" className="item" value="1.0">1.0</a>
                                            <a href="https://ballerina.io/1.1/learn/api-docs/ballerina/" className="item" value="1.1">1.1</a>
                                            <a href="https://ballerina.io/learn/api-docs/ballerina/" className="item" value="1.2">1.2</a>
                                            <a className="item active" value="swan-lake">Swan Lake</a>
                                        </div>
                                    </div>
                                    <div className="ui dropdown item ballerina">
                                        Ballerina <i className="dropdown icon"></i>
                                        <div className="menu">
                                            <a href="/learn" className="item active">Learn</a>
                                            <a href="/learn/events" className="item">Events</a>
                                            <a href="https://central.ballerina.io/" className="item">Central</a>
                                            <a href="/community" className="item">Community</a>
                                            <a href="https://blog.ballerina.io/" className="item">Blog</a>
                                        </div>
                                    </div>

                                </div>
                            </div>

                            <div id="search-impl">
                                <SearchList searchData={props.searchData} />
                            </div>
                            <div id="main">
                                {props.pageType != "packageIndex" && props.pageType != "404" &&
                                    <div className="ui breadcrumb">
                                        {props.match.params.orgName == "ballerina" &&
                                            <>
                                                <Link to="/" className="section">Packages</Link>
                                                <i className="right angle icon divider"></i>
                                            </>
                                        }
                                        {props.pageType != "package" && <Link to={"/" + props.match.params.orgName + "/" + props.match.params.packageName + "/" + props.match.params.version + "/"} className="section">{props.match.params.packageName} Package</Link>}
                                        {props.pageType == "package" && <p className="section active">{props.match.params.packageName} Package</p>}

                                        {props.pageType != "package" &&
                                            <>
                                                <i className="right angle icon divider"></i>
                                                <Link to={"/" + props.match.params.orgName + "/" + props.match.params.packageName + "/" + props.match.params.version + "#modules"} className="section">Modules</Link>
                                                <i className="right angle icon divider"></i>
                                                {props.pageType != "module" && <Link to={"/" + props.match.params.orgName + "/" + props.match.params.packageName + "/" + props.match.params.version + "/" + props.match.params.moduleName} className="section">{props.module.id} Module</Link>}
                                                {props.pageType == "module" && <p className="section active">{props.module.id} Module</p>}

                                                {props.pageType != "module" &&
                                                    <span>
                                                        <i className="right angle icon divider"></i>
                                                        {hasChildPages ?
                                                            (<span><Link to={"/" + props.match.params.orgName + "/" + props.match.params.packageName + "/" + props.match.params.version + "/" + props.match.params.moduleName + "#" + props.pageType} className="section capitalize">{props.pageType}</Link>
                                                            <i className="right angle icon divider"></i><p className="section capitalize active">{props.match.params.constructName}</p></span>)
                                                            :
                                                            (<p className="section capitalize active">{props.pageType}</p>)
                                                        }
                                                    </span>
                                                }
                                            </>
                                        }
                                    </div>
                                }

                                <div className="ui very padded segment module-data">
                                    {props.children}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

        </div>
    );
}

export default Layout;
