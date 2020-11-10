/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

import * as React from "react";
import PackageView from "./component/packageview"
import Module from "./Module"

import {
  HashRouter as Router,
  Route
} from "react-router-dom";


class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      modules: null
    };

    this.loadScript("./data/doc_data.js", () => {
      console.log("Script loaded");
      this.setState({
        modules: window.docData.modules
      });
    });

    this.loadScript("./doc-search/search-data.js", () => {
      console.log("Search Data loaded");
    });
  }

  loadScript(url, callback) {
    console.log("loading..........");

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
      <section className="App">
        {this.state.modules != null &&
          <Router>
            <Route exact path="/" render={(props) => (<PackageView {...props} modules={this.state.modules} />)} />
            <Route path="/:moduleName" render={(props) => (<Module {...props} modules={this.state.modules} />)} />
          </Router>
        }
      </section>

    );
  }
}

export default App;
