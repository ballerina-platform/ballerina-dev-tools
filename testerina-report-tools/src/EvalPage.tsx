import React from 'react';
import { Test } from './TestData';
import EvalRunsView from './EvalRunsView';

type EvalPageProps = {
    test: Test;
    moduleName: string;
    onBack: () => void;
}

export default function EvalPage({ test, moduleName, onBack }: EvalPageProps) {
    const evalSummary = test.evaluationSummary;
    const totalRuns = evalSummary?.evaluationRuns.length || 0;
    const hasDataProvider = evalSummary?.evaluationRuns.some(run => run.outcomes && run.outcomes.length > 0);

    return (
        <div className="eval-page">
            <div className="eval-page-header">
                <div className="eval-page-nav">
                    <span className="back-arrow" onClick={onBack}>&#60;</span>
                    <div className="eval-page-breadcrumb">
                        <span className="eval-breadcrumb-module">{moduleName}</span>
                        <span className="eval-breadcrumb-separator">/</span>
                        <span className="eval-breadcrumb-test">{test.name}</span>
                    </div>
                </div>
            </div>

            <div className="eval-page-summary">
                <div className="eval-summary-card">
                    <div className="eval-summary-title">Evaluation Test</div>
                    <h2 className="eval-summary-name">{test.name}</h2>
                    <div className={`eval-summary-status ${test.status}`}>
                        {test.status}
                    </div>
                </div>

                <div className="eval-stats-row">
                    <div className="eval-stat">
                        <div className="eval-stat-value">{totalRuns}</div>
                        <div className="eval-stat-label">Total Runs</div>
                    </div>

                    <div className="eval-stat">
                        <div className="eval-stat-value">{hasDataProvider ? 'Yes' : 'No'}</div>
                        <div className="eval-stat-label">Data Provider</div>
                    </div>
                    {evalSummary?.targetPassRate !== undefined && (
                        <div className="eval-stat">
                            <div className="eval-stat-value">{(evalSummary.targetPassRate * 100).toFixed(0)}%</div>
                            <div className="eval-stat-label">Target Pass Rate</div>
                        </div>
                    )}
                    {evalSummary?.observedPassRate !== undefined && (
                        <div className="eval-stat">
                            <div className="eval-stat-value">{(evalSummary.observedPassRate * 100).toFixed(0)}%</div>
                            <div className="eval-stat-label">Observed Pass Rate</div>
                        </div>
                    )}
                </div>
            </div>

            {test.failureMessage && (
                <div className="eval-page-section">
                    <h3 className="eval-section-title">Failure Summary</h3>
                    <div className="eval-failure-message">{test.failureMessage}</div>
                </div>
            )}

            <div className="eval-page-section">
                <h3 className="eval-section-title">Evaluation Runs</h3>
                {evalSummary && (
                    <EvalRunsView evalRuns={evalSummary.evaluationRuns} testName={test.name} />
                )}
            </div>
        </div>
    );
}
