import * as React from "react";
import { getFirstLine } from "./helper"
import { Link } from '../Router'


const MethodTable = (props) => {


    return (
        <section>

            <table className="ui very basic table">
                <tbody>
                    {props.methods.map(item => (
                        <tr key={item.name}>
                            <td className={item.isDeprecated ? "module-title strike" : "module-title"} title={item.name}>
                                <Link to={"#" + item.name}>{item.name}</Link>
                            </td>
                            <td className="module-desc">
                                {
                                    item.isDeprecated == true &&
                                    <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                }
                                <span dangerouslySetInnerHTML={getFirstLine(item.description)} />
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </section>
    );



}

export default MethodTable;
