import React, { useEffect } from 'react'
import { getFirstLine } from "./helper"
import { HashLink as Link } from 'react-router-hash-link';
import Method from "./method"
import Layout from "./layout"


const Functions = (props) => {

    useEffect(() => {
        $('.ui.accordion.functions').accordion('open',0);
    });
    
        return (
            <Layout title={"API Docs Functions" } module={props.module}  pageType="functions">

            <section>
                <h1 className="capitalize">Functions</h1>
                <div className="constants">
                    <table className="ui very basic table">
                        <tbody>
                            {props.functions.map(item => (
                                <tr key={item.name}>
                                    <td width="30%">
                                        <Link className={item.isDeprecated ? "strike functions" : "functions"} to={"#" + item.name}>{item.name}</Link>
                                    </td>
                                    <td width="70%">
                                        <div className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            {
                                                item.isIsolated == true &&
                                                <div className="ui horizontal label" data-tooltip="Isolated Function" data-position="top left">I</div>
                                            }
                                            <span dangerouslySetInnerHTML={getFirstLine(item.description)} />
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                    <div>
                    {props.functions.map(item => (
                        <Method method={item}/>
                    ))}
                </div>
                </div>
            </section>
            </Layout>
        );
    
}

export default Functions;