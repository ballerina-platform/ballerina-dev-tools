import * as React from "react";
import { getTypeLabel } from "./helper"

const Fields = (props) => {

    return (
        <div className="fields-listing">
            <ul>
                {props.fields.map(item => (
                    <section key={item.name}>
                        <li key={item.name}>
                            <span className={item.isDeprecated ? "strike" : ""}>{item.name} </span>
                            {getTypeLabel(item.type, item.defaultValue)}
                        </li>
                        <span dangerouslySetInnerHTML={{ __html: item.description }}></span>
                    </section>
                ))}
            </ul>
        </div>
    );

}

export default Fields;