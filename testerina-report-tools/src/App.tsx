import React, { Component } from 'react';

import 'bootstrap-css-only/css/bootstrap.min.css'
import './App.css';

import ProjectReport, { ReportSummary, WorkspaceReport, WorkspaceSummary } from './StatusReport';
import ModuleStatusSummary, { ModuleSummaryView } from './ModuleReport';
import { TestData, WorkspaceData, ReportData, normalizeToWorkspaceData } from './TestData';
import { BallerinaLogo } from './BallerinaLogo';
import {ReactComponent as PassedIcon} from "./images/success.svg";
import {ReactComponent as FailedIcon} from "./images/failed.svg";
import {ReactComponent as SkippedIcon} from "./images/skipped.svg";

// app.ts

import ModuleCoverageSummary from './ModuleCoverage';
import FileCoverage from './FileCoverage';

let jsonContainer : HTMLElement = document.getElementById('testData') as HTMLInputElement;
let rawData: ReportData = JSON.parse(jsonContainer.innerHTML);
let workspaceData: WorkspaceData = normalizeToWorkspaceData(rawData);

interface IState {
  view: string;
  projectIndex: number;
  moduleIndex: number;
  fileIndex: number;
}

class App extends Component {
  state: IState;
  constructor(props: any) {
    super(props);
    this.state = { view: "index", projectIndex: 0, moduleIndex: 0 , fileIndex: 0}
    this.handleStateChange = this.handleStateChange.bind(this);
  }
 
  handleStateChange (value: string, moduleIndex: number, fileIndex: number, projectIndex?: number) {
      this.setState({ 
        view: value, 
        projectIndex: projectIndex !== undefined ? projectIndex : this.state.projectIndex,
        moduleIndex: moduleIndex, 
        fileIndex: fileIndex
      });
  }

  getModuleTableView(testData: TestData, moduleIndex: number, isCoverageAvailable: boolean) {
    let moduleCoverageHtml = null
    if (isCoverageAvailable) {
      moduleCoverageHtml = <ModuleCoverageSummary 
      module={testData.moduleCoverage[this.state.moduleIndex]} 
      modIndex={this.state.moduleIndex} 
      projectIndex={this.state.projectIndex}
      updateState={this.handleStateChange}
      />
      
    }
    return <div className="module-content">
      <div className="title row">
      <span className="back-arrow" style={{fontSize:30}} onClick={() => this.handleStateChange("project", 0, 0, this.state.projectIndex)}>&#60;</span>
        <span className='project'><h5 id={testData.moduleStatus[moduleIndex].name}>
          {testData.moduleStatus[moduleIndex].name}
        </h5></span>
    
      </div>
      <div className="row">
      <ModuleStatusSummary module={testData.moduleStatus[moduleIndex]}/>
      {moduleCoverageHtml}
    </div>
  </div>
  }
 
  render() {
    // Get current package from workspace
    const currentProject = workspaceData.packages[this.state.projectIndex];
    
    let isCoverageAvailable = currentProject.moduleCoverage.length > 0
    let statusIcon
    if (workspaceData.failed > 0) {
      statusIcon = <FailedIcon className="icon failed-icon" />
    } else if (workspaceData.passed > 0) {
      statusIcon = <PassedIcon className="icon passed-icon" />
    } else {
      statusIcon = <SkippedIcon className="icon skipped-icon" />
    }

    let tableView
    let summaryView
    let coveragetableView = null
    if (this.state.view.includes("index")) {
        // Show workspace summary with all packages
        tableView = <WorkspaceReport data={workspaceData} updateState={this.handleStateChange} />
        summaryView = <WorkspaceSummary data={workspaceData}/>
    } else if (this.state.view.includes("project")){
        // Show package details
        tableView = <ProjectReport data={currentProject} projectIndex={this.state.projectIndex} updateState={this.handleStateChange} />
        summaryView = <ReportSummary data={currentProject}/>
    } else if (this.state.view.includes("module")){
        console.log(currentProject.moduleStatus[this.state.moduleIndex])
        tableView = this.getModuleTableView(currentProject, this.state.moduleIndex, isCoverageAvailable)
        summaryView = <ModuleSummaryView data={currentProject} index={this.state.moduleIndex}/> 
      } else if (this.state.view.includes("coverage")){
        tableView = null
        summaryView = <FileCoverage 
        pkgIndex={this.state.projectIndex}
        moduleName={currentProject.moduleCoverage[this.state.moduleIndex].name}
        modIndex={this.state.moduleIndex}
        file={currentProject.moduleCoverage[this.state.moduleIndex].sourceFiles[this.state.fileIndex]} 
        updateState={this.handleStateChange}
        />
    }
    return (

<div className="App">
  <div className="header-section">
    <div className="container-fluid">
      <header className="App-header row justify-content-md-center">
        <div className="col-sm-6">
          <div className="row">
            <span className="logo pull-left"><BallerinaLogo /></span>
            <h4 className="header-title">Test Report</h4>
          </div>
        </div>
        <div className="title_projectname col-sm-2">
        <h4>{statusIcon} {workspaceData.workspaceName}</h4>
      </div>
      
      </header>
      </div>
  </div>
  <div className="container-fluid content-section">
      {summaryView}
  </div>
  <div className="container-fluid content-section">
      {tableView}
      {coveragetableView}
  </div>
</div>

    );
  }
}

export default App;
