import * as React from "react";
import { Route } from "react-router-dom";
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
import PackageView from "./component/packageview"
import ModuleView from "./component/moduleview"

const Module = (props) => {

    let module = props.modules.filter((item) => {
        return item.id == (props.match.params.moduleName);
    })[0];

    return (
        <section>

            {module != null &&
                <section>
                    <Route exact path="/:moduleName" render={(props) => (<ModuleView {...props} module={module} />)} />

                    <Route exact path="/:moduleName/records/:constructName" render={(props) => (<FindConstruct {...props} module={module} pageType="records" />)} />
                    <Route exact path="/:moduleName/classes/:constructName" render={(props) => (<FindConstruct {...props} module={module} pageType="classes" />)} />
                    <Route exact path="/:moduleName/abstractObjects/:constructName" render={(props) => (<FindConstruct {...props} module={module} pageType="abstractObjects" />)} />
                    <Route exact path="/:moduleName/clients/:constructName" render={(props) => (<FindConstruct {...props} module={module} pageType="clients" />)} />
                    <Route exact path="/:moduleName/listeners/:constructName" render={(props) => (<FindConstruct {...props} module={module} pageType="listeners" />)} />

                    <Route exact path="/:moduleName/functions" render={(props) => (<Functions {...props} functions={module.functions} module={module} />)} />
                    <Route exact path="/:moduleName/types" render={(props) => (<Types {...props} types={module.types} module={module} />)} />
                    <Route exact path="/:moduleName/errors" render={(props) => (<Errors {...props} errors={module.errors} module={module} />)} />
                    <Route exact path="/:moduleName/constants" render={(props) => (<Constants {...props} constants={module.constants} module={module} />)} />
                    <Route exact path="/:moduleName/annotations" render={(props) => (<Annotations {...props} annotations={module.annotations} module={module} />)} />
                </section>
            }

        </section>

    );

}

const FindConstruct = (props) => {
    let construct = props.module[props.pageType].filter((item) => {
        return item.name == props.match.params.constructName;
    })[0];

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
    }
}



export default Module;