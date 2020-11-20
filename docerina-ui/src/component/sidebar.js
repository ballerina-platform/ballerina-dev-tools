import * as React from "react";
import { Link } from '../Router'

import ConstructList from "./constructlist"

const getModuleConstructTypes = (module, name) => {
    return (<div className="menu">
        {module.listeners != null && module.listeners.length > 0 &&
            <ConstructList type="desktop" data={module.listeners} listType="listeners" moduleName={module.id} name={name} />
        }
        {module.clients != null && module.clients.length > 0 &&
            <ConstructList type="desktop" data={module.clients} listType="clients" moduleName={module.id} name={name} />
        }
        {module.functions != null && module.functions.length > 0 &&
            <ConstructList type="desktop" data={module.functions} listType="functions" moduleName={module.id} />
        }
        {module.classes != null && module.classes.length > 0 &&
            <ConstructList type="desktop" data={module.classes} listType="classes" moduleName={module.id} name={name} />
        }
        {module.abstractObjects != null && module.abstractObjects.length > 0 &&
            <ConstructList type="desktop" data={module.abstractObjects} listType="abstractObjects" moduleName={module.id} name={name} />
        }
        {module.records != null && module.records.length > 0 &&
            <ConstructList type="desktop" data={module.records} listType="records" moduleName={module.id} name={name} />
        }
        {module.constants != null && module.constants.length > 0 &&
            <ConstructList type="desktop" data={module.constants} listType="constants" moduleName={module.id} />
        }
        {module.annotations != null && module.annotations.length > 0 &&
            <ConstructList type="desktop" data={module.annotations} listType="annotations" moduleName={module.id} />
        }
        {module.types != null && module.types.length > 0 &&
            <ConstructList type="desktop" data={module.types} listType="types" moduleName={module.id} />
        }
        {module.errors != null && module.errors.length > 0 &&
            <ConstructList type="desktop" data={module.errors} listType="errors" moduleName={module.id} />
        }
        <div className="ui divider"></div>

    </div>);
}




const SideBar = (props) => {
    let module = props.module;

    if (props.type == "desktop") {
        return (
            <section>
                {module != null &&
                    <section>
                    
                        <div className="header">
                            <Link className="capitalize" to={"/" + module.id}>{module.id}</Link>
                        </div>
                        {getModuleConstructTypes(module, props.name)}

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
            {hasChildPages &&
                <div className="capitalize ui dropdown item">
                    {props.pageType}<i className="dropdown icon"></i>
                    <ConstructList {...props} type="mobile" data={module[props.pageType]} listType={props.pageType} moduleName={module.id} />
                </div>
            }
            <div className="capitalize ui dropdown item">
                {module.id} Module <i className="dropdown icon"></i>
                {getModuleConstructTypes(module)}
            </div>


        </>);
    }

}


export default SideBar;