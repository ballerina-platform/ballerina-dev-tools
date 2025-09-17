import React, { Component } from 'react'; // we need this to make JSX compile
import { TestData, WorkspaceData } from './TestData';
import {ReactComponent as TotalIcon} from './images/test.svg';
import {ReactComponent as PassedIcon} from "./images/success.svg";
import {ReactComponent as FailedIcon} from "./images/failed.svg";
import {ReactComponent as SkippedIcon} from "./images/skipped.svg";

type ReportProps = {
    data: TestData,
}

export function ReportSummary ({ data }: ReportProps) {
    let isCoverageAvailable = data.moduleCoverage.length > 0
    let coverage = null
    if (isCoverageAvailable) {
        coverage = <div className="col-sm-3 card summary-card progress-card">
            <h6>Code Coverage: {data.coveragePercentage}%</h6>
            <div className="progress">
                <div className="progress-bar" style={{width: data.coveragePercentage+"%"}}>{data.coveragePercentage}%</div>
            </div>
        </div>
      }
    return <div className="row">
        <div className="col card summary-card total">
            <div className="row">
                <div className="col-sm-4"><TotalIcon className="icon total" /></div>
                <div className="col-sm-8"><h3>{data.totalTests}</h3><small>Total tests</small></div>
            </div>
        </div>
        <div className="col card summary-card passed white-font">
        <div className="row">
                <div className="col-sm-5"><PassedIcon className="icon" /><br/><small>Passed</small></div>
                <div className="col-sm-7"><h1>{data.passed}/<small>{data.totalTests}</small></h1></div>
            </div>
        </div>
        <div className="col card summary-card failed white-font">
        <div className="row">
                <div className="col-sm-5"><FailedIcon className="icon" /><br/><small>Failed</small></div>
                <div className="col-sm-7"><h1>{data.failed}/<small>{data.totalTests}</small></h1></div>
            </div>
        </div>
        <div className="col card summary-card skipped white-font">
        <div className="row">
                <div className="col-sm-5"><SkippedIcon className="icon" /><br/><small>Skipped</small></div>
                <div className="col-sm-7"><h1>{data.skipped}/<small>{data.totalTests}</small></h1></div>
            </div>
        </div>
        {coverage}
    </div>;
}

class ProjectReport extends Component<{data: TestData, projectIndex?: number, updateState: Function}> { 
  render() {
      let data = this.props.data
      let isCoverageAvailable = data.moduleCoverage.length > 0
        let coverageHeaders = null
        if (isCoverageAvailable) {
            coverageHeaders = [<th>Lines Covered</th>,<th>Lines Missed</th>,<th>Coverage Percentage</th>]
        }
      return(
        <div className="module-content">
            <div className="title row">
                <span className="back-arrow" style={{fontSize:30}} onClick={() => this.props.updateState("index", 0, 0)}>&#60;</span>
                <span className='project'><h5 id={data.projectName}>
                    {data.projectName}
                </h5></span>
            </div>
            <div className="row card">
                <table className="col-sm-12 table table-borderless">
                <thead>
                <tr>
                    <th>Module Name</th>
                    <th>Total Functions</th>
                    <th><PassedIcon className="icon passed-icon" /></th>
                    <th><FailedIcon className="icon failed-icon" /></th>
                    <th><SkippedIcon className="icon skipped-icon" /></th>
                    {coverageHeaders}
                </tr>
                </thead>
                <tbody>
                {data.moduleStatus.map((module, index) => {
                        let coverageData = null
                        if (isCoverageAvailable) {
                            const moduleCov = data.moduleCoverage[index]
                            coverageData = [<td>{moduleCov.coveredLines}</td>,
                            <td>{moduleCov.missedLines}</td>,
                            <td>
                                <div className="progress">
                                    <div className="progress-bar" style={{width: moduleCov.coveragePercentage+"%"}}>{moduleCov.coveragePercentage}%</div>
                                </div>
                            </td>]
                        }
                        return <tr id={module.name}>
                        <td><span className="link" onClick={() => this.props.updateState("module", index, 0, this.props.projectIndex)}>{module.name}</span></td>
                        <td>{module.totalTests}</td>
                        <td>{module.passed}</td>
                        <td>{module.failed}</td>
                        <td>{module.skipped}</td>
                        {coverageData}
                        </tr>;
                    })}
                </tbody>
            </table>
            </div>
        </div>
      );
  }
}

// Workspace-level components
type WorkspaceProps = {
    data: WorkspaceData,
}

export function WorkspaceSummary ({ data }: WorkspaceProps) {
    let isCoverageAvailable = data.packages.some(project => project.moduleCoverage.length > 0)
    let coverage = null
    if (isCoverageAvailable) {
        coverage = <div className="col-sm-3 card summary-card progress-card">
            <h6>Code Coverage: {data.coveragePercentage}%</h6>
            <div className="progress">
                <div className="progress-bar" style={{width: data.coveragePercentage+"%"}}>{data.coveragePercentage}%</div>
            </div>
        </div>
      }
    return <div className="row">
        <div className="col card summary-card total">
            <div className="row">
                <div className="col-sm-4"><TotalIcon className="icon total" /></div>
                <div className="col-sm-8"><h3>{data.totalTests}</h3><small>Total tests</small></div>
            </div>
        </div>
        <div className="col card summary-card passed white-font">
        <div className="row">
                <div className="col-sm-5"><PassedIcon className="icon" /><br/><small>Passed</small></div>
                <div className="col-sm-7"><h1>{data.passed}/<small>{data.totalTests}</small></h1></div>
            </div>
        </div>
        <div className="col card summary-card failed white-font">
        <div className="row">
                <div className="col-sm-5"><FailedIcon className="icon" /><br/><small>Failed</small></div>
                <div className="col-sm-7"><h1>{data.failed}/<small>{data.totalTests}</small></h1></div>
            </div>
        </div>
        <div className="col card summary-card skipped white-font">
        <div className="row">
                <div className="col-sm-5"><SkippedIcon className="icon" /><br/><small>Skipped</small></div>
                <div className="col-sm-7"><h1>{data.skipped}/<small>{data.totalTests}</small></h1></div>
            </div>
        </div>
        {coverage}
    </div>;
}

class WorkspaceReport extends Component<{data: WorkspaceData, updateState: Function}> { 
  render() {
      let data = this.props.data
      let isCoverageAvailable = data.packages.some(project => project.moduleCoverage.length > 0)
      let coverageHeader = null
      if (isCoverageAvailable) {
          coverageHeader = <th>Coverage %</th>
      }
      return(
        <div className="row card">
            <table className="col-sm-12 table table-borderless">
                <thead>
                <tr>
                    <th>Package Name</th>
                    <th>Total Tests</th>
                    <th><PassedIcon className="icon passed-icon" /></th>
                    <th><FailedIcon className="icon failed-icon" /></th>
                    <th><SkippedIcon className="icon skipped-icon" /></th>
                    {coverageHeader}
                </tr>
                </thead>
                <tbody>
                {data.packages.map((project, index) => {
                        let coverageData = null
                        if (isCoverageAvailable) {
                            coverageData = <td>
                                <div className="progress">
                                    <div className="progress-bar" style={{width: project.coveragePercentage+"%"}}>{project.coveragePercentage}%</div>
                                </div>
                            </td>
                        }
                        return <tr id={project.projectName}>
                        <td><span className="link" onClick={() => this.props.updateState("project", 0, 0, index)}>{project.projectName}</span></td>
                        <td>{project.totalTests}</td>
                        <td>{project.passed}</td>
                        <td>{project.failed}</td>
                        <td>{project.skipped}</td>
                        {coverageData}
                        </tr>;
                    })}
                </tbody>
            </table>
    </div>
      );
  }
}

export { WorkspaceReport };

export default ProjectReport;
