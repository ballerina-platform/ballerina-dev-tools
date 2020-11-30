import * as React from "react";
import { getTypeLabel } from "./helper"


const InitMethod = (props) => {
    return (
        <div>
            {props.initMethod.description != "" &&

                <section className="method-list">

                    <h2>Constructor</h2>
                    {props.initMethod.description != null &&
                        <span dangerouslySetInnerHTML={{ __html: props.initMethod.description }} />
                    }
                    <pre className="method-signature">
                        <code className="break-spaces"><span class="token keyword">__init</span> ({props.initMethod.parameters.length > 0 && props.initMethod.parameters.map(param => { return [getTypeLabel(param.type), " " + param.name]; }).reduce((prev, curr) => [prev, ', ', curr])})</code>
                    </pre>
                    <div className="data-wrapper">
                        {props.initMethod.parameters.map(item => (
                            <div key={item.name} className="params-listing">
                                <ul>
                                    <li> <b>{item.name}</b><span className="type"> {getTypeLabel(item.type)}</span> {item.defaultValue}</li>
                                    <li>
                                        <span dangerouslySetInnerHTML={{ __html: item.description }} />
                                    </li>
                                </ul>
                            </div>
                        ))}
                    </div>
                </section>
            }
        </div>
    );
}

export default InitMethod;
