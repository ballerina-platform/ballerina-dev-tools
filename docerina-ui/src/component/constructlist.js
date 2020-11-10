import * as React from "react";
import { Link } from '../Router'
import { getConnector } from "./helper"



const ConstructList = (props) => {

    if (props.type == "desktop") {
        return (
            <>
                <div class={"ui accordion item "+props.listType}>
                    <div class="title capitalize">
                    {props.listType}
                        <i class="dropdown icon"></i>
                    </div>
                    <div class="content">
                    {props.data.map(item => (
                            <Link key={item.name} className={props.name==item.name?"active item":"item"} to={"/" + props.moduleName + "/" + props.listType + getConnector(props.listType) + item.name}>{item.name}</Link>
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
                {props.data.map(item => (
                    <Link key={item.name} className="item" to={"/" + props.moduleName + "/" + props.listType + getConnector(props.listType) + item.name}>{item.name}</Link>
                ))}
            </div>
        )
    }

}

export default ConstructList;
