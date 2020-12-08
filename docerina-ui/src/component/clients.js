import React, { useEffect } from 'react'
import Fields from "./fields"
import InitMethod from "./initMethod"
import MethodTable from "./methodTable"
import Method from "./method"
import Layout from "./layout"
import {scrollAndHighlight} from "./helper"

const Client = (props) => {

    useEffect(() => {
        if (props.history.location.hash != "") {
            scrollAndHighlight(props.history.location.hash);
        } else {
            window.scrollTo(0, 0);
        }
        $('.ui.accordion.clients').accordion('open',0);
    });

    let client = props.client;

    return (

        <Layout {...props} title={"API Docs Client: " + client.name } name={client.name}>

        <section className="construct-page">
            {client != null &&
                <section>
                    <h1>Client: <span className={client.isDeprecated ? "strike clients" : "clients"}>{client.name}</span></h1>
                    {
                        client.isDeprecated == true &&
                        <div className="ui orange horizontal label">Deprecated</div>
                    }
                    <p><span dangerouslySetInnerHTML={{ __html: client.description }} /></p>
                    <div className="constants">
                        <div className="method-sum">

                            {client.initMethod != null && <InitMethod initMethod={client.initMethod} />}

                            {client.remoteMethods != null && client.remoteMethods.length>0 &&
                                <section className="method-list">
                                    <h2>Remote Methods</h2>
                                    <div>
                                        <MethodTable methods={client.remoteMethods} />
                                    </div>
                                </section>
                            }
                            {client.otherMethods != null && client.otherMethods.length>0 &&
                                <section className="method-list">
                                    <h2>Methods</h2>
                                    <div className="method-list">
                                        <MethodTable methods={client.otherMethods} />
                                    </div>
                                </section>
                            }
                            {client.fields != null && client.fields.length>0 &&
                                <section className="fields-list">
                                    <h2>Fields</h2>
                                    <div>
                                        <Fields fields={client.fields} />
                                    </div>
                                </section>
                            }
                        </div>
                        {client.remoteMethods != null &&
                            client.remoteMethods.map(item => (
                                <div key={item.name}><Method method={item} /></div>
                            ))
                        }
                        {client.otherMethods != null &&
                            client.otherMethods.map(item => (
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

export default Client;