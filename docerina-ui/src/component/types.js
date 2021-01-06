import React, { useEffect } from 'react'
import { getTypeLabel, scrollAndHighlight } from "./helper"
import Layout from "./layout"
import { appType } from '../Router'
import Markdown from "./markdown"

const Types = (props) => {

    useEffect(() => {
        if (appType == "react" && props.history.location.hash != "") {
            scrollAndHighlight(props.history.location.hash);
        } else if (appType == "next" && location.hash != "") {
            scrollAndHighlight(location.hash);
        } else {
            window.scrollTo(0, 0);
        }
        $('.ui.accordion.types').accordion('open', 0);
    });

    return (
        <Layout {...props} title={"API Docs Types "} pageType="types">
            <section className="construct-page">
                <h1 className="capitalize">Types</h1>
                {props.types != null &&
                    <div className="types">
                        <div className="data-wrapper">
                            {props.types.map(item => (
                                <div key={item.name} id={item.name} className="params-listing">
                                    <ul>
                                        <li>
                                            <b className={item.isDeprecated ? "strike" : ""}>{item.name} </b>
                                            <span className="type">{getTypeLabel(item)}</span>
                                        </li>
                                        {
                                            item.isDeprecated == true &&
                                            <div className="ui orange horizontal label">Deprecated</div>
                                        }
                                        <Markdown text={item.description} />
                                    </ul>
                                </div>

                            ))}
                        </div>
                    </div>
                }
            </section>
        </Layout>
    );

}

export default Types;
