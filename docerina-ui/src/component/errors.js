import React, { useEffect } from 'react'
import { getTypeLabel, scrollAndHighlight } from "./helper"
import Layout from "./layout"

const Errors = (props) => {

    useEffect(() => {
        if (props.history.location.hash != "") {
            scrollAndHighlight(props.history.location.hash);
        } else {
            window.scrollTo(0, 0);
        }
        $('.ui.accordion.errors').accordion('open',0);

    });

    return (
        <Layout {...props} title={"API Docs Errors " } pageType="errors">

        <section>
            <h1 className="capitalize">Errors</h1>

            <div className="errors">
                <div className="fields-listing">
                    <ul>
                        {props.errors.map(item => (
                            <div key={item.name}>
                                <li id={item.name}>
                                    <b className={item.isDeprecated ? "strike" : ""}>{item.name} </b>
                                    <span className="type">{item.detailType != null && getTypeLabel(item.detailType)}</span>
                                </li>
                                {item.isDeprecated == true &&
                                    <div className="ui orange horizontal label" data-tooltip="Deprecated" data-position="top left">D</div>
                                }
                                <span dangerouslySetInnerHTML={{ __html: item.description }}></span>
                            </div>
                        ))}
                    </ul>
                </div>
            </div>

        </section>
        </Layout>
    );
}

export default Errors;