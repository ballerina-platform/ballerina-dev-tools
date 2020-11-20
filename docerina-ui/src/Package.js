import * as React from "react";
import {
    Route
} from "react-router-dom";
import Module from "./Module"
import PackageView from "./component/packageview"


export class Package extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            modules: null
        };
        this.loadScript("./data/" + this.props.match.params.packageName + ".js");
    }

    loadScript(url) {
        const callback = () => {
            console.log("Script loaded");
            this.setState({
                modules: window.docData.modules
            });
            console.log(this.state.modules);
        };
        // Adding the script tag to the head as suggested before
        var head = document.head;
        var script = document.createElement('script');
        script.type = 'text/javascript';
        script.src = url;

        // Then bind the event to the callback function.
        // There are several events for cross browser compatibility.
        script.onreadystatechange = callback;
        script.onload = callback;

        // Fire the loading
        head.appendChild(script);
    }

    render() {
        return (
            <section>

                {this.state.modules != null &&
                    <section>
                        <Route exact path="/:packageName" render={(props) => (<PackageView {...props} moduleData={this.state.modules} />)} />
                        <Route path="/:packageName/:moduleName" render={(props) => (<Module {...props} modules={this.state.modules} />)} />
                    </section>
                }

            </section>

        );
    }
}