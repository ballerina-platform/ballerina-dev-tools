import * as React from "react";
import { Link } from '../Router'
import { getConnector } from "./helper"

const ConstructList = (props) => {
    let path;
    if (props.match.params.packageName != null) {
        path = "/" + props.match.params.packageName;
    } else {
        path = "";
    }
    if (props.type == "desktop") {
        return (
            <>
                <div className={"ui accordion item "+props.listType}>
                    <div className="title capitalize">
                    {props.listType}
                        <i className="dropdown icon"></i>
                    </div>
                    <div className="content">
                    {props.module[props.listType].map(item => (
                            <Link title={item.name} key={item.name} className={props.match.params.constructName==item.name?"active item":"item"} to={path + "/" + props.module.id + "/" + props.listType + getConnector(props.listType) + item.name}>{item.name}</Link>
                        ))}
                    </div>
                </div>
                {/* <div class="ui left pointing dropdown link item">
                    <span class="capitalize"></span>
                    <i class="dropdown icon"></i>
                    <div className="menu">

                    </div>
                </div> */}
            </>
        );
    } else {
        return (
            <div className="menu">
                {props.module[props.listType].map(item => (
                    <Link key={item.name} className="item" to={props.module.id + "/" + props.listType + getConnector(props.listType) + item.name}>{item.name}</Link>
                ))}
            </div>
        )
    }

}

export default ConstructList;
