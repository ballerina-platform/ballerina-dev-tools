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

import React from "react";

import {
  HashRouter as Router,
  Route
} from "react-router-dom";
import PackageIndex from "./component/packageindex";
import Package from "./Package";


class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      packages: null,
      searchData: null,
      packageDescription: null
    };

    this.loadScript("./data/doc_data.js", () => {
      console.log("Doc data loaded");
      this.setState({
        packages: window.docData.packages,
        packageDescription: window.docData.description
      });
    });

    this.loadScript("./doc-search/search-data.js", () => {
      console.log("Search Data loaded");
      this.setState({
        searchData: window.searchData
      });
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
        {this.state.packages != null && this.state.searchData != null &&
          <Router>
            <Route exact path="/" render={(props) => (<PackageIndex {...props} packages={this.state.packages} searchData={this.state.searchData} packageDescription={this.state.packageDescription} />)} />
            <Route path="/:orgName/:packageName/:version" render={(props) => (<Package {...props} packages={this.state.packages} searchData={this.state.searchData} />)} />
          </Router>
        }
      </section>

    );
  }
}

export default App;
