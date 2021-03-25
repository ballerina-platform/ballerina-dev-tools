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

import React, { useState } from 'react'
import { Transition, Dropdown } from 'semantic-ui-react'
import SideBar from "./sidebar"
import { Head, rootPath, otherScripts, Link, appType } from '../Router'
import SearchList from "./searchlist"
import { removeHtmlTags } from "./helper"

const Layout = (props) => {

    const [visibility, setVisibility] = useState(false);
    const [searchText, setSearchText] = useState("");

    let hasChildPages;
    if (props.pageType == "functions" || props.pageType == "types" || props.pageType == "errors" || props.pageType == "annotations" || props.pageType == "constants") {
        hasChildPages = false;
    } else {
        hasChildPages = true;
    }

    let isLocal = location.hostname == null || location.hostname == "localhost" || location.hostname == "";
    let searchBoxText = props.pageType == "packageIndex" ? "Search in distribution..." : "Search in " + props.package.name + " package...";

    function toggleMenu() {
        setVisibility(!visibility);
    }

    function keyUpHandler(e) {
        const inputText = e.target.value;
        const mainDiv = document.getElementById("main");
        if (inputText !== "") {
            if (mainDiv != null) {
                mainDiv.classList.add('hidden');
            }
        } else {
            if (mainDiv != null) {
                mainDiv.classList.remove('hidden');
            }
        }
        setSearchText(inputText);
    }

    const resetSearch = () => {
        setSearchText("");
        const mainDiv = document.getElementById("main");
        if (mainDiv != null) {
            mainDiv.classList.remove('hidden');
        }
    }

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
                        <div className="mobile-logo"><a href="/"><img className="mobile-logo-img" src={rootPath + "images/ballerina-docs-logo.svg"}></img></a></div>
                        <div className="right item">
                            <button className="ui icon button" onClick={toggleMenu}>
                                <i className="large bars icon"></i>
                            </button>
                        </div>
                    </div>
                </div>

                <div className="toc">
                    <div className="ui visible left vertical sidebar menu">
                        <div className="logo"><Link to="/"><img className="logo-img" src={rootPath + "images/ballerina-docs-logo.svg"}></img></Link></div>
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
                                        <input className="prompt" id="searchBox" onInput={keyUpHandler} type="text" placeholder={searchBoxText} />
                                    </div>
                                </div>
                            </div>
                            {!isLocal && <div className="right menu">
                                <a href="http://ballerina.io/learn/" className="item">Learn</a>
                                <a href="https://swanlake.central.ballerina.io/" className="item">Central</a>
                                <a href="https://ballerina.io/events" className="item">Events</a>
                                <a href="https://ballerina.io/community/" className="item">Community</a>
                                <a href="https://blog.ballerina.io/" className="item">Blog</a>
                            </div>}
                        </div>
                    </div>
                    <div className="main-content-holder">
                        <div className="main-content">
                            <Transition visible={visibility} animation='slide down' duration={500}>
                                <div id="mobMenu" className="mob-menu">
                                    <div className="ui stackable secondary menu">
                                        <div className="item">
                                            <div className="search-bar">
                                                <div className="ui category search item search-bar">
                                                    <i className="search link icon"></i>
                                                    <div className="ui transparent icon input">
                                                        <input className="prompt" id="searchBoxMob" onInput={keyUpHandler} type="text" placeholder={searchBoxText} />
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                        <SideBar {...props} type="mobile" />
                                        {!isLocal && <Dropdown className="ballerina item" text="Ballerina">
                                            <Dropdown.Menu>
                                                <a href="http://ballerina.io/learn/" className="item">Learn</a>
                                                <a href="https://swanlake.central.ballerina.io/" className="item">Central</a>
                                                <a href="https://ballerina.io/events" className="item">Events</a>
                                                <a href="https://ballerina.io/community/" className="item">Community</a>
                                                <a href="https://blog.ballerina.io/" className="item">Blog</a>                                            </Dropdown.Menu>
                                        </Dropdown>}
                                    </div>
                                </div>
                            </Transition>

                            <div id="search-impl">
                                <SearchList searchText={searchText} resetFunc={resetSearch} searchData={props.searchData} />
                            </div>
                            <div id="main">
                                {props.pageType != "packageIndex" && props.pageType != "404" &&
                                    <div className="ui breadcrumb">
                                        {(props.match.params.orgName == "ballerina" || props.match.params.orgName == "ballerinax" || props.pageType != "builtin" || props.pageType != "keyword") &&
                                            <>
                                                <Link to="/" className="section">Packages</Link>
                                                <i className="right angle icon divider"></i>
                                            </>
                                        }
                                        {props.pageType == "builtin" &&
                                            <>
                                                <Link to="/#builtin">Builtin Types</Link>
                                                <i className="right angle icon divider"></i>
                                                <p className="section active">{props.match.params.type}</p>
                                            </>
                                        }
                                        {props.pageType == "keyword" &&
                                            <>
                                                <Link to="/#keywords">Keywords</Link>
                                                <i className="right angle icon divider"></i>
                                                <p className="section active">{props.match.params.type}</p>
                                            </>
                                        }
                                        {props.pageType != "builtin" && props.pageType != "keyword" &&
                                            <>
                                                {props.pageType != "module" && <Link to={"/" + props.match.params.orgName + "/" + props.match.params.packageName + "/" + props.match.params.version + "/"} className="section">{props.match.params.packageName}</Link>}
                                                {props.pageType == "module" && props.module.id != props.package.name && <Link to={"/" + props.match.params.orgName + "/" + props.match.params.packageName + "/" + props.match.params.version + "/"} className="section">{props.match.params.packageName}</Link>}
                                                {props.pageType == "module" && props.module.id == props.package.name && <p className="section active">{props.match.params.packageName}</p>}

                                                {props.module.id != props.package.name &&
                                                    <>
                                                        <i className="right angle icon divider"></i>
                                                        <Link to={"/" + props.match.params.orgName + "/" + props.match.params.packageName + "/" + props.match.params.version + "#modules"} className="section">Modules</Link>
                                                        <i className="right angle icon divider"></i>
                                                        {props.pageType != "module" && <Link to={"/" + props.match.params.orgName + "/" + props.match.params.packageName + "/" + props.match.params.version + "/" + props.match.params.moduleName} className="section">{props.module.id}</Link>}
                                                        {props.pageType == "module" && <p className="section active">{props.module.id}</p>}
                                                    </>
                                                }
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
