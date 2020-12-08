import * as React from "react";
import { getTypeLabel } from "./helper"

const Method = (props) => {
    return (
        <div className="method-content construct-page">
            <div className="main-method-title" id={props.method.name} title={props.method.name}>

                <h2 className={props.method.isDeprecated ? "strike" : ""}> {props.method.name} </h2>
            </div>
            <div>
                <pre className="method-signature">
                    <code className="break-spaces"><span className="token keyword">function</span> {props.method.name}(
            {props.method.parameters.length > 0 && props.method.parameters.map(param => { return [getTypeLabel(param.type), " " + param.name]; }).reduce((prev, curr) => [prev, ', ', curr])})
            {props.method.returnParameters.length > 0 && <span> <span className="token keyword">returns</span> {getTypeLabel(props.method.returnParameters[0].type)}</span>}
                    </code>
                </pre>
            </div>
            <div className="function-desc">
                {
                    props.method.isDeprecated == true &&
                    <div className="ui orange horizontal label">Deprecated</div>
                }
                {
                    props.method.isIsolated == true &&
                    <div className="ui horizontal label">Isolated Function</div>
                }
                <p><span dangerouslySetInnerHTML={{ __html: props.method.description }} /></p>
            </div>
            {props.method.parameters.length > 0 &&
                <div className="parameters">
                    <h3 className="param-title">Parameters</h3>
                    {props.method.parameters.map(item => (
                        <div key={item.name} className="params-listing">
                            <ul>
                                <li>
                                    <span className={item.isDeprecated ? "strike" : ""}>{item.name}</span>
                                    <span className="type">  {getTypeLabel(item.type)} </span>
                                    {item.defaultValue != "" && <span className="default"> (default {item.defaultValue})</span>}
                                </li>
                                {
                                    item.isDeprecated == true &&
                                    <div className="ui orange horizontal label">Deprecated</div>
                                }
                                <p>
                                    <span dangerouslySetInnerHTML={{ __html: item.description }} />
                                </p>
                            </ul>
                        </div>
                    ))}
                </div>
            }

            {props.method.returnParameters.length > 0 &&
                <div className="returns-listing">
                    <h3 className="type">Return Type</h3> (<span className="type">{getTypeLabel(props.method.returnParameters[0].type)}</span>)
            <span dangerouslySetInnerHTML={{ __html: props.method.returnParameters[0].description }} />
                </div>
            }

        </div>
    );



}

export default Method;
