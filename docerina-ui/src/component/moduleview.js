import * as React from "react";
import { Link } from '../Router'

import { getFirstLine } from "./helper"
import Layout from "./layout"

const ModuleView = (props) => {

    return (
        <Layout title={"API Docs : " + props.moduleData.id} module={props.moduleData} pageType="module">

            <div>

                <h1>{props.moduleData.orgName}/{props.moduleData.id}:{props.moduleData.version}</h1>
                <span dangerouslySetInnerHTML={{ __html: props.moduleData.description }} />

                {props.moduleData.listeners.length > 0 &&
                    <section id="listeners" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Listeners</h2>
                            <p>[{props.moduleData.listeners.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.moduleData.listeners.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate abstractObjects" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike records" : "records"} to={props.moduleData.id + "/listeners/" + item.name}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <span dangerouslySetInnerHTML={getFirstLine(item.description)} /></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }

                {props.moduleData.clients.length > 0 &&
                    <section id="clients" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Clients</h2>
                            <p>[{props.moduleData.clients.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.moduleData.clients.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate clients" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike clients" : "clients"} to={props.moduleData.id + "/clients/" + item.name}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <span dangerouslySetInnerHTML={getFirstLine(item.description)} /></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }

                {props.moduleData.functions.length > 0 &&
                    <section id="functions" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Functions</h2>
                            <p>[{props.moduleData.functions.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.moduleData.functions.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate functions" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike functions" : "functions"} to={props.moduleData.id + "/functions#" + item.name}>{item.name}</Link>

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
                }

                {props.moduleData.classes.length > 0 &&
                    <section id="classes" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Classes</h2>
                            <p>[{props.moduleData.classes.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.moduleData.classes.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate classes" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike classes" : "classes"} to={props.moduleData.id + "/classes/" + item.name}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <span dangerouslySetInnerHTML={getFirstLine(item.description)} /></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }
                {props.moduleData.abstractObjects.length > 0 &&
                    <section id="abstractObjects" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Abstract Objects </h2>
                            <p>[{props.moduleData.abstractObjects.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.moduleData.abstractObjects.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate abstractObjects" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike abstractObjects" : "abstractObjects"} to={props.moduleData.id + "/abstractObjects/" + item.name}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <span dangerouslySetInnerHTML={getFirstLine(item.description)} /></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }

                {props.moduleData.records.length > 0 &&
                    <section id="records" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Records </h2>
                            <p>[{props.moduleData.records.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.moduleData.records.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate records" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike records" : "records"} to={props.moduleData.id + "/records/" + item.name}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <span dangerouslySetInnerHTML={getFirstLine(item.description)} /></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }

                {props.moduleData.constants.length > 0 &&
                    <section id="constants" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Constants</h2>
                            <p>[{props.moduleData.constants.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.moduleData.constants.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate constants" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike constants" : "constants"} to={props.moduleData.id + "/constants#" + item.name}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <span dangerouslySetInnerHTML={getFirstLine(item.description)} /></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }


                {props.moduleData.annotations.length > 0 &&
                    <section id="annotations" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Annotations</h2>
                            <p>[{props.moduleData.annotations.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.moduleData.annotations.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate annotations" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike annotations" : "annotations"} to={props.moduleData.id + "/annotations#" + item.name}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <span dangerouslySetInnerHTML={getFirstLine(item.description)} /></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }

                {props.moduleData.types.length > 0 &&
                    <section id="types" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Types</h2>
                            <p>[{props.moduleData.types.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.moduleData.types.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate types" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike types" : "types"} to={props.moduleData.id + "/types#" + item.name}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <span dangerouslySetInnerHTML={getFirstLine(item.description)} /></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }

                {props.moduleData.errors.length > 0 &&
                    <section id="errors" className="module-construct">
                        <div className="main-method-title here">
                            <h2>Errors</h2>
                            <p>[{props.moduleData.errors.length}]</p>
                        </div>
                        <div className="ui divider"></div>
                        <table className="ui very basic table">
                            <tbody>
                                {props.moduleData.errors.map(item => (
                                    <tr key={item.name}>
                                        <td className="module-title truncate errors" id={item.name} title={item.name}>
                                            <Link className={item.isDeprecated ? "strike errors" : "errors"} to={props.moduleData.id + "/errors#" + item.name}>{item.name}</Link>

                                        </td>
                                        <td className="module-desc">
                                            {
                                                item.isDeprecated == true &&
                                                <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                            }
                                            <span dangerouslySetInnerHTML={getFirstLine(item.description)} /></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </section>
                }
            </div>
        </Layout>
    );
}

export default ModuleView;