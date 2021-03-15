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
import PackageIndex from "./component/packageindex";
import Builtin from "./component/builtin"
import Keyword from "./component/keyword"
import Package from "./Package";

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      packages: null,
      searchData: null,
      packageDescription: null,
      builtinTypes: null,
      keywords: null
    };
  }

  componentDidMount() {
    this.setState({
      packages: this.props.data.docsData.packages,
      searchData: this.props.data.searchData,
      builtinTypes: this.props.data.docsData.builtinTypes,
      keywords: this.props.data.docsData.keywords
    });
  }


  render() {
    return (
      <section className="App">
        {this.state.packages != null && this.state.searchData != null &&
          <Router>
            <Switch>
              <Route exact path="/" render={(props) => (<PackageIndex {...props} packages={this.state.packages} searchData={this.state.searchData} releaseDescription={this.props.data.docsData.description} builtinTypes={this.state.builtinTypes} keywords={this.state.keywords} releaseVersion={this.props.data.docsData.releaseVersion} ballerinaShortVersion={this.props.data.docsData.releaseShortVersion} />)} />
              <Route exact path="/builtin/:balVersion/:type" render={(props) => (<FindBuiltinType {...props} builtinTypes={this.state.builtinTypes} packages={this.state.packages} searchData={this.state.searchData} ballerinaShortVersion={this.props.data.docsData.releaseShortVersion} />)} />
              <Route exact path="/keywords/:balVersion/:type" render={(props) => (<FindKeyword {...props} keywords={this.props.data.docsData.keywords} packages={this.state.packages} searchData={this.state.searchData} />)} />
              <Route path="/:orgName/:packageName/:version" render={(props) => (<Package {...props} packages={this.state.packages} searchData={this.state.searchData} />)} />
            </Switch>

          </Router>
        }
      </section>

    );
  }
}

const FindBuiltinType = (props) => {
  let builtinType = props.builtinTypes.filter((item) => {
    return item.name == props.match.params.type;
  })[0]

  var langlib = null;

  if (builtinType.langlib != null) {
    langlib = props.packages.filter((item) => {
      return item.name == builtinType.langlib.name;
    })[0].modules[0]
  }

  return <Builtin {...props} builtinType={builtinType} langlib={langlib} ballerinaShortVersion={props.ballerinaShortVersion}  />

}

const FindKeyword = (props) => {
  let builtinType = props.keywords.filter((item) => {
    return item.name == props.match.params.type;
  })[0]

  return <Keyword {...props} builtinType={builtinType}/>

}

export default App;
