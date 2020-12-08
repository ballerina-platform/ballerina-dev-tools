import React, { useEffect } from 'react'
import { getTypeLabel, scrollAndHighlight } from "./helper"
import Layout from "./layout"

const Annotations = (props) => {

    useEffect(() => {
        if (props.history.location.hash != "") {
            scrollAndHighlight(props.history.location.hash);
        } else {
            window.scrollTo(0, 0);
        }
        $('.ui.accordion.annotations').accordion('open',0);
    });

    return (
        <Layout {...props} title={"API Docs Annotations" }  pageType="annotations">

        <section>
            <h1 className="capitalize">Annotations</h1>

            <div className="annotations">
                <div className="fields-listing">
                    <ul>
                        {props.annotations.map(item => (
                            <div key={item.name}>
                                <li id={item.name}>
                                    <b className={item.isDeprecated ? "strike" : ""}>{item.name} </b>
                                    <span className="type">{item.type != null && getTypeLabel(item.type)} </span><img className="attach-icon" src="./html-template-resources/images/attach.svg" />
{item.attachmentPoints}
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

export default Annotations;