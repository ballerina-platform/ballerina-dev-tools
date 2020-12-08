import * as React from "react";
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
        {module.abstractObjects != null && module.abstractObjects.length > 0 &&
            <ConstructList {...props} listType="abstractObjects" />
        }
        {module.records != null && module.records.length > 0 &&
            <ConstructList {...props} listType="records" />
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
        {module.errors != null && module.errors.length > 0 &&
            <ConstructList {...props} listType="errors" />
        }
        <div className="ui divider"></div>

    </div>);
}




const SideBar = (props) => {
    if (props.type == "desktop") {
        return (
            <section>
                {props.module != null &&
                    <section>
                    
                        <div className="header">
                            <Link className="capitalize" to={"/" + props.module.id}>{props.module.id}</Link>
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
            {hasChildPages &&
                <div className="capitalize ui dropdown item">
                    {props.pageType}<i className="dropdown icon"></i>
                    <ConstructList {...props} type="mobile" data={props.module[props.pageType]} listType={props.pageType} moduleName={props.module.id} />
                </div>
            }
            <div className="capitalize ui dropdown item">
                {props.module.id} Module <i className="dropdown icon"></i>
                {getModuleConstructTypes(props)}
            </div>

        </>);
    }

}


export default SideBar;