import React, { useEffect } from 'react'
import Fields from "./fields"
import InitMethod from "./initMethod"
import MethodTable from "./methodTable"
import Method from "./method"
import Layout from "./layout"

const BClass = (props) => {

    useEffect(() => {
        window.scrollTo(0, 0);
        $('.ui.accordion.classes').accordion('open',0);

    });
    
        let bClass= props.bClass;

        return (
            <Layout title={"API Docs Class: " + bClass.name } module={props.module}  pageType="classes" name={bClass.name} >

            <section className="construct-page">
                {bClass != null &&
                    <section>
                        <h1>Class: <span className={bClass.isDeprecated ? "strike" : ""}>{bClass.name}</span></h1>
                        {
                            bClass.isDeprecated == true &&
                            <div className="ui orange horizontal label">Deprecated</div>
                        }
                        <p><span dangerouslySetInnerHTML={{ __html: bClass.description }} /></p>
                        <div className="constants">
                        <div className="method-sum">

                            {bClass.initMethod != null && <InitMethod initMethod={bClass.initMethod} />}
                            {bClass.otherMethods != null && bClass.otherMethods.length>0 &&
                                <section className="method-list">
                                    <h2>Methods</h2>
                                    <div className="method-list">
                                        <MethodTable methods={bClass.otherMethods} />
                                    </div>
                                </section>
                            }
                            {bClass.fields != null && bClass.fields.length>0 &&
                                <section className="fields-list">
                                    <h2>Fields</h2>
                                    <div>
                                        <Fields fields={bClass.fields} />
                                    </div>
                                </section>
                            }
                            </div>
                            <div>
                                {bClass.otherMethods != null && bClass.otherMethods.length>0 &&
                                    bClass.otherMethods.map(item => (
                                        <div key={item.name}><Method method={item} /></div>
                                    ))
                                }
                            </div>
                        </div>

                    </section>
                }
            </section>
            </Layout>
        );
    
}

export default BClass;