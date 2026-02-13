import React, { useState } from 'react';
import { ModuleStatus, Test, TestData, isEvaluationTest } from './TestData';

import { ReactComponent as TotalIcon } from './images/test.svg';
import { ReactComponent as PassedIcon } from "./images/success.svg";
import { ReactComponent as FailedIcon } from "./images/failed.svg";
import { ReactComponent as SkippedIcon } from "./images/skipped.svg";

type ModuleSummaryViewProps = {
    data: TestData,
    index: number
}

export function ModuleSummaryView({ data, index }: ModuleSummaryViewProps) {
    let isCoverageAvailable = data.moduleCoverage.length > 0
    let coverage = null
    if (isCoverageAvailable) {
        coverage = <div className="col-sm-3 card summary-card progress-card">
            <h6>Code Coverage: {data.moduleCoverage[index].coveragePercentage}%</h6>
            <div className="progress">
                <div className="progress-bar" style={{ width: data.moduleCoverage[index].coveragePercentage + "%" }}>{data.moduleCoverage[index].coveragePercentage}%</div>
            </div>
        </div>
    }
    return <div className="row">
        <div className="col card summary-card total">
            <div className="row">
                <div className="col-sm-4"><TotalIcon className="icon total" /></div>
                <div className="col-sm-8"><h3>{data.moduleStatus[index].totalTests}</h3><small>Total tests</small></div>
            </div>
        </div>
        <div className="col card summary-card passed white-font">
            <div className="row">
                <div className="col-sm-5"><PassedIcon className="icon" /><br /><small>Passed</small></div>
                <div className="col-sm-7"><h1>{data.moduleStatus[index].passed}/<small>{data.moduleStatus[index].totalTests}</small></h1></div>
            </div>
        </div>
        <div className="col card summary-card failed white-font">
            <div className="row">
                <div className="col-sm-5"><FailedIcon className="icon" /><br /><small>Failed</small></div>
                <div className="col-sm-7"><h1>{data.moduleStatus[index].failed}/<small>{data.moduleStatus[index].totalTests}</small></h1></div>
            </div>
        </div>
        <div className="col card summary-card skipped white-font">
            <div className="row">
                <div className="col-sm-5"><SkippedIcon className="icon" /><br /><small>Skipped</small></div>
                <div className="col-sm-7"><h1>{data.moduleStatus[index].skipped}/<small>{data.moduleStatus[index].totalTests}</small></h1></div>
            </div>
        </div>
        {coverage}
    </div>;

}

// Test row component with evaluation support
type TestRowProps = {
    test: Test;
    onEvalClick?: (test: Test) => void;
    isExpanded: boolean;
    onToggleExpand: () => void;
};

function TestRow({ test, onEvalClick, isExpanded, onToggleExpand }: TestRowProps) {
    const isEval = isEvaluationTest(test);
    const hasError = test.status === "FAILURE" && test.failureMessage;

    return (
        <>
            <tr className={test.status} id={test.name}>
                <td>
                    {isEval && onEvalClick ? (
                        <span
                            className="link eval-test-name"
                            onClick={() => onEvalClick(test)}
                        >
                            {test.name}
                            <span className="view-details-icon"> →</span>
                        </span>
                    ) : hasError ? (
                        <span
                            className="link test-error-toggle"
                            onClick={onToggleExpand}
                        >
                            {test.name}
                            <span className="expand-indicator"> {isExpanded ? '▼' : '▶'}</span>
                        </span>
                    ) : (
                        test.name
                    )}
                </td>
                <td>{test.status}</td>
            </tr>
            {/* Failure message row - only shown when expanded */}
            {hasError && isExpanded && (
                <tr className={test.status}>
                    <td className="small" colSpan={2}>{test.failureMessage}</td>
                </tr>
            )}
        </>
    );
}

type ModuleStatusSummaryProps = {
    module: ModuleStatus;
    onEvalClick?: (test: Test) => void;
}

function ModuleStatusSummary({ module, onEvalClick }: ModuleStatusSummaryProps) {
    // Track expanded state for each test with errors
    const [expandedTests, setExpandedTests] = useState<Set<string>>(new Set());

    // Get list of failed tests
    const failedTests = module.tests.filter(t => t.status === "FAILURE" && t.failureMessage);
    const hasFailedTests = failedTests.length > 0;
    const allExpanded = failedTests.every(t => expandedTests.has(t.name));

    const toggleTest = (testName: string) => {
        setExpandedTests(prev => {
            const newSet = new Set(prev);
            if (newSet.has(testName)) {
                newSet.delete(testName);
            } else {
                newSet.add(testName);
            }
            return newSet;
        });
    };

    const toggleAll = () => {
        if (allExpanded) {
            // Collapse all
            setExpandedTests(new Set());
        } else {
            // Expand all failed tests
            setExpandedTests(new Set(failedTests.map(t => t.name)));
        }
    };

    return (
        <div className="col card">
            {hasFailedTests && (
                <div className="test-error-controls">
                    <button className="test-expand-all-btn" onClick={toggleAll}>
                        {allExpanded ? 'Collapse All Errors' : 'Expand All Errors'}
                    </button>
                </div>
            )}
            <table className="table table-striped table-borderless">
                <thead>
                    <tr>
                        <th>Test Name</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
                    {module.tests.map((test, index) => (
                        <TestRow
                            key={test.name || index}
                            test={test}
                            onEvalClick={onEvalClick}
                            isExpanded={expandedTests.has(test.name)}
                            onToggleExpand={() => toggleTest(test.name)}
                        />
                    ))}
                </tbody>
            </table>
        </div>
    );
}

export default ModuleStatusSummary


