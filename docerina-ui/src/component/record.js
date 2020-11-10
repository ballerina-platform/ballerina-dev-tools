import React, { useEffect } from 'react'
import Fields from "./fields"
import Layout from "./layout"

const Record = (props) => {

    useEffect(() => {
        window.scrollTo(0, 0);
        $('.ui.accordion.records').accordion('open',0);
    });

    let record = props.record;
    
    return (
        <Layout title={"API Docs - " + props.module.id +" Record: " + record.name } module={props.module}  pageType="records" name={record.name}>
        <section className="construct-page">
            {record != null &&
                <section>
                    <h1>Record: <span className={record.isDeprecated ? "strike" : ""}>{record.name}</span></h1>
                    {
                        record.isDeprecated == true &&
                        <div className="ui orange horizontal label">Deprecated</div>
                    }
                    <p><span dangerouslySetInnerHTML={{ __html: record.description }} /></p>
                    {
                        record.isClosed == true &&
                        <div className="ui horizontal label" data-tooltip="A record with a fixed set of fields" data-position="top left">Closed record</div>
                    }
                    <div className="constants">
                        {record.fields.length == 0 && <p>This record doesn't contain any fields.</p>}
                        {record.fields.length != 0 &&
                            <section>
                                <h2>Fields</h2>

                                <Fields fields={record.fields} />

                            </section>
                        }
                    </div>

                </section>
            }
        </section>
        </Layout>
    );

}

export default Record;