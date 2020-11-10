import React, { useEffect } from 'react'
import Fields from "./fields"
import MethodTable from "./methodTable"
import Method from "./method"
import Layout from "./layout"

const AbstractObject = (props) => {
    
    useEffect(() => {
        window.scrollTo(0, 0);
        $('.ui.accordion.abstractObjects').accordion('open',0);

    });

    let abstractObject = props.abstractObject;

    return (
        <Layout title={"API Docs Abstract Object: " + abstractObject.name} module={props.module}  pageType="abstractObjects" name={abstractObject.name} >

            <section className="construct-page">
                {abstractObject != null &&
                    <section>
                        <h1>Abstract Object: <span className={abstractObject.isDeprecated ? "strike" : ""}>{abstractObject.name}</span></h1>
                        {
                            abstractObject.isDeprecated == true &&
                            <div className="ui orange horizontal label">Deprecated</div>
                        }
                        <p><span dangerouslySetInnerHTML={{ __html: abstractObject.description }} /></p>
                        <div className="constants">
                            <div className="method-sum">

                                {abstractObject.methods != null && abstractObject.methods.length > 0 &&
                                    <section className="method-list">
                                        <h2>Methods</h2>
                                        <div className="method-list">
                                            <MethodTable methods={abstractObject.methods} />
                                        </div>
                                    </section>
                                }
                                {abstractObject.fields != null && abstractObject.fields.length > 0 &&
                                    <section className="fields-list">
                                        <h2>Fields</h2>
                                        <div>
                                            <Fields fields={abstractObject.fields} />
                                        </div>
                                    </section>
                                }
                            </div>
                            {abstractObject.methods != null &&
                                abstractObject.methods.map(item => (
                                    <div key={item.name}><Method method={item} /></div>
                                ))
                            }
                        </div>

                    </section>
                }
            </section>
        </Layout>
    );

}

export default AbstractObject;