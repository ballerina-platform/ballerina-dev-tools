import React, { useEffect } from 'react'
import { getTypeLabel } from "./helper"
import Layout from "./layout";


const Constants = (props) => {

    useEffect(() => {
        window.scrollTo(0, 0);
        $('.ui.accordion.constants').accordion('open',0);

    });

    return (
        <Layout title={"API Docs Constants: " } module={props.module}  pageType="constants">

        <section>
            <h1 className="capitalize">Constants</h1>

            <div className="constants">
                <div className="fields-listing">
                    <ul>
                        {props.constants.map(item => (
                            <div key={item.name}>
                                <li id={item.name}>
                                    <b className={item.isDeprecated ? "strike" : ""}>{item.name} </b>
                                    <span className="type">{item.type != null && getTypeLabel(item.type)}</span> {item.value}
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

export default Constants;