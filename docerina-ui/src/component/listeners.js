import React, { useEffect } from 'react'
import Fields from "./fields"
import InitMethod from "./initMethod"
import MethodTable from "./methodTable"
import Method from "./method"
import Layout from "./layout"

const Listener = (props) => {

    useEffect(() => {
        window.scrollTo(0, 0);
        $('.ui.accordion.listeners').accordion('open',0);

    });

    let listener = props.listener;

    return (

        <Layout title={"API Docs Listener: " + listener.name } module={props.module}  pageType="listeners" name={listener.name}>

        <section className="construct-page">
            {listener != null &&
                <section>
                    <h1>Listener: <span className={listener.isDeprecated ? "strike listeners" : "listeners"}>{listener.name}</span></h1>
                    {
                        listener.isDeprecated == true &&
                        <div className="ui orange horizontal label">Deprecated</div>
                    }
                    <p><span dangerouslySetInnerHTML={{ __html: listener.description }} /></p>
                    <div className="constants">
                        <div className="method-sum">

                            {listener.initMethod != null && <InitMethod initMethod={listener.initMethod} />}

                            {listener.lifeCycleMethods != null && listener.lifeCycleMethods.length>0 &&
                                <section className="method-list">
                                    <h2>LifeCycle Methods</h2>
                                    <div>
                                        <MethodTable methods={listener.lifeCycleMethods} />
                                    </div>
                                </section>
                            }
                            {listener.otherMethods != null && listener.otherMethods.length>0 &&
                                <section className="method-list">
                                    <h2>Methods</h2>
                                    <div>
                                        <MethodTable methods={listener.otherMethods} />
                                    </div>
                                </section>
                            }
                            {listener.fields.length > 0 && 
                                <section className="fields-list">
                                    <h2>Fields</h2>
                                    <div>
                                        <Fields fields={listener.fields} />
                                    </div>
                                </section>
                            }
                        </div>
                        {listener.lifeCycleMethods != null &&
                            listener.lifeCycleMethods.map(item => (
                                <div key={item.name}><Method method={item} /></div>
                            ))
                        }
                        {listener.otherMethods != null &&
                            listener.otherMethods.map(item => (
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

export default Listener;