import * as React from "react";
import { Link } from '../Router'
import Layout from "./layout";


const PackageView = (props) => {


        return (

            <section>
            <Layout title={"API Docs" }  pageType="package">

                <h1 className="capitalize">Ballerina Modules</h1>
                <table className="ui very basic table">
                    <tbody>
                        {props.modules.map((item) => (
                            <tr>
                                <td className="module-title modules"><Link to={"/"+item.id}>{item.id}</Link></td>
                                <td className="module-desc"><span dangerouslySetInnerHTML={{ __html: item.summary }} /></td>
                            </tr>
                        ))}
                    </tbody>
                </table>
                </Layout>
            </section>
        );
    
}

export default PackageView;