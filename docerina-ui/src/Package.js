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
import {
    Route
} from "react-router-dom";
import Module from "./Module"
import NotFound from "./component/notfound"

const Package = (parentProps) => {

    let docPackage = parentProps.packages.filter((item) => {
        return item.name == (parentProps.match.params.packageName) && item.orgName == (parentProps.match.params.orgName);
    })[0];
    return (
        <>
            {docPackage != null &&
                <>
                    <Route exact path="/:orgName/:packageName/:version" render={(props) => (<Module {...props} package={docPackage} searchData={parentProps.searchData} />)} />
                    <Route path="/:orgName/:packageName/:version/:moduleName" render={(props) => (<Module {...props} package={docPackage} searchData={parentProps.searchData} />)} />
                </>
            }
            {docPackage == null && <NotFound {...parentProps} />}
        </>
    );
}

export default Package;
