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
  }

  componentDidMount() {
    this.setState({
      packages: this.props.data.docsData.packages,
      searchData: this.props.data.searchData,
    });
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
