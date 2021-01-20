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
import { Route, Switch } from "react-router-dom";
import Record from "./component/record"
import BClass from "./component/classes"
import AbstractObject from "./component/abstractobjects"
import Client from "./component/clients"
import Listener from "./component/listeners"
import Functions from "./component/functions"
import Types from "./component/types"
import Errors from "./component/errors"
import Constants from "./component/constants"
import Annotations from "./component/annotations"
import ModuleView from "./component/moduleview"
import Enum from "./component/enum"
import NotFound from "./component/notfound"

const Module = (parentProps) => {

    let module = parentProps.package.modules.filter((item) => {
        return item.id == (parentProps.match.params.moduleName);
    })[0];

    return (
        <>
            {module != null &&
                <Switch>
                    <Route exact path="/:orgName/:packageName/:version/:moduleName" render={(props) => (<ModuleView {...props} module={module} searchData={parentProps.searchData} />)} />

                    <Route exact path="/:orgName/:packageName/:version/:moduleName/records/:constructName" render={(props) => (<FindConstruct {...props} module={module} pageType="records" searchData={parentProps.searchData} />)} />
                    <Route exact path="/:orgName/:packageName/:version/:moduleName/classes/:constructName" render={(props) => (<FindConstruct {...props} module={module} pageType="classes" searchData={parentProps.searchData} />)} />
                    <Route exact path="/:orgName/:packageName/:version/:moduleName/abstractObjects/:constructName" render={(props) => (<FindConstruct {...props} module={module} pageType="abstractObjects" searchData={parentProps.searchData} />)} />
                    <Route exact path="/:orgName/:packageName/:version/:moduleName/clients/:constructName" render={(props) => (<FindConstruct {...props} module={module} pageType="clients" searchData={parentProps.searchData} />)} />
                    <Route exact path="/:orgName/:packageName/:version/:moduleName/listeners/:constructName" render={(props) => (<FindConstruct {...props} module={module} pageType="listeners" searchData={parentProps.searchData} />)} />
                    <Route exact path="/:orgName/:packageName/:version/:moduleName/enums/:constructName" render={(props) => (<FindConstruct {...props} module={module} pageType="enums" searchData={parentProps.searchData} />)} />

                    <Route exact path="/:orgName/:packageName/:version/:moduleName/functions" render={(props) => (<Functions {...props} functions={module.functions} module={module} searchData={parentProps.searchData} />)} />
                    <Route exact path="/:orgName/:packageName/:version/:moduleName/types" render={(props) => (<Types {...props} types={module.types} module={module} searchData={parentProps.searchData} />)} />
                    <Route exact path="/:orgName/:packageName/:version/:moduleName/errors" render={(props) => (<Errors {...props} errors={module.errors} module={module} searchData={parentProps.searchData} />)} />
                    <Route exact path="/:orgName/:packageName/:version/:moduleName/constants" render={(props) => (<Constants {...props} constants={module.constants} module={module} searchData={parentProps.searchData} />)} />
                    <Route exact path="/:orgName/:packageName/:version/:moduleName/annotations" render={(props) => (<Annotations {...props} annotations={module.annotations} module={module} searchData={parentProps.searchData} />)} />
                    <Route render={(props) => (<NotFound {...parentProps} />)} />
                </Switch>
            }
            {module == null && <NotFound {...parentProps} />}
        </>
    );
}

const FindConstruct = (props) => {
    let construct = props.module[props.pageType].filter((item) => {
        return item.name == props.match.params.constructName;
    })[0];

    if (construct == null) {
        return <NotFound {...props} />
    }

    if (props.pageType == "records") {
        return <Record {...props} record={construct} />
    } else if (props.pageType == "classes") {
        return <BClass {...props} bClass={construct} />
    } else if (props.pageType == "listeners") {
        return <Listener {...props} listener={construct} />
    } else if (props.pageType == "clients") {
        return <Client {...props} client={construct} />
    } else if (props.pageType == "abstractObjects") {
        return <AbstractObject {...props} abstractObject={construct} />
    } else if (props.pageType == "enums") {
        return <Enum {...props} bEnum={construct} />
    }
}

export default Module;
