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
  Route,
  Switch
} from "react-router-dom";
import ModuleIndex from "./component/moduleindex";
import Module from "./Module";

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      modules: null,
      searchData: null,
      packageDescription: null
    };
  }

  componentDidMount() {
    this.setState({
      modules: this.props.data.docsData.modules,
      langLibs: this.props.data.docsData.langLibs,
      searchData: this.props.data.searchData
    });
  }


  render() {
    return (
      <section className="App">
        {this.state.modules != null && this.state.searchData != null &&
          <Router>
            <Switch>
              <Route exact path="/" render={(props) => (<ModuleIndex {...props} modules={this.state.modules} langLibs={this.state.langLibs} searchData={this.state.searchData} releaseDescription={this.props.data.docsData.description} releaseVersion={this.props.data.docsData.releaseVersion} ballerinaShortVersion={this.props.data.docsData.releaseShortVersion} />)} />
              <Route path="/:orgName/:moduleName/:version" render={(props) => (<Module {...props} modules={this.state.modules} langLibs={this.state.langLibs} searchData={this.state.searchData} />)} />
            </Switch>

          </Router>
        }
      </section>

    );
  }
}

export default App;
