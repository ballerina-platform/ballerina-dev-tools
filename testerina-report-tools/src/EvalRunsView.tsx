import React, { useState } from 'react';
import { EvalRun } from './TestData';

type EvalRunsViewProps = {
    evalRuns: EvalRun[];
    testName: string;
}

// Check if eval runs have outcomes (data provider based)
function hasOutcomes(evalRuns: EvalRun[]): boolean {
    return evalRuns.some(run => run.outcomes && run.outcomes.length > 0);
}

// List view for simple eval runs (no data provider)
function SimpleEvalListView({ evalRuns, expandedRunIds, onToggleRun }: {
    evalRuns: EvalRun[],
    expandedRunIds: Set<number>,
    onToggleRun: (id: number) => void
}) {
    return (
        <div className="eval-list">
            {evalRuns.map(run => {
                const hasFailed = !!run.errorMessage;
                const isExpanded = expandedRunIds.has(run.id);

                return (
                    <div key={run.id} className={`eval-list-item ${hasFailed ? 'failed' : 'passed'}`}>
                        <div
                            className="eval-list-item-header"
                            onClick={() => hasFailed && onToggleRun(run.id)}
                        >
                            <span className={`eval-status-icon ${hasFailed ? 'failed' : 'passed'}`}>
                                {hasFailed ? '✗' : '✓'}
                            </span>
                            <span className="eval-list-item-title">Run {run.id}</span>
                            <span className={`eval-list-item-status ${hasFailed ? 'failed' : 'passed'}`}>
                                {hasFailed ? 'FAILED' : 'PASSED'}
                            </span>
                            {hasFailed && (
                                <span className="eval-expand-icon">{isExpanded ? '▼' : '▶'}</span>
                            )}
                        </div>
                        {isExpanded && run.errorMessage && (
                            <div className="eval-list-item-error">
                                <pre>{run.errorMessage}</pre>
                            </div>
                        )}
                    </div>
                );
            })}
        </div>
    );
}

// List view for eval runs with data provider
function DataProviderEvalListView({ evalRuns, expandedKeys, onToggleExpand }: {
    evalRuns: EvalRun[],
    expandedKeys: Set<string>,
    onToggleExpand: (key: string) => void
}) {
    return (
        <div className="eval-list">
            {evalRuns.map(run => {
                const passRateText = run.passRate !== undefined
                    ? `${(run.passRate * 100).toFixed(0)}%`
                    : '';

                return (
                    <div key={run.id} className="eval-run-group">
                        <div className="eval-run-label">
                            Run {run.id} {passRateText && `(${passRateText} pass rate)`}
                        </div>
                        {run.outcomes?.map(outcome => {
                            const hasFailed = !!outcome.errorMessage;
                            const key = `${run.id}-${outcome.id}`;
                            const isExpanded = expandedKeys.has(key);

                            return (
                                <div key={key} className={`eval-list-item ${hasFailed ? 'failed' : 'passed'}`}>
                                    <div
                                        className="eval-list-item-header"
                                        onClick={() => hasFailed && onToggleExpand(key)}
                                    >
                                        <span className={`eval-status-icon ${hasFailed ? 'failed' : 'passed'}`}>
                                            {hasFailed ? '✗' : '✓'}
                                        </span>
                                        <span className="eval-list-item-title">{outcome.id}</span>
                                        <span className={`eval-list-item-status ${hasFailed ? 'failed' : 'passed'}`}>
                                            {hasFailed ? 'FAILED' : 'PASSED'}
                                        </span>
                                        {hasFailed && (
                                            <span className="eval-expand-icon">{isExpanded ? '▼' : '▶'}</span>
                                        )}
                                    </div>
                                    {isExpanded && outcome.errorMessage && (
                                        <div className="eval-list-item-error">
                                            <pre>{outcome.errorMessage}</pre>
                                        </div>
                                    )}
                                </div>
                            );
                        })}
                    </div>
                );
            })}
        </div>
    );
}

// Main component
export default function EvalRunsView({ evalRuns, testName }: EvalRunsViewProps) {
    const [expandedRunIds, setExpandedRunIds] = useState<Set<number>>(new Set());
    const [expandedKeys, setExpandedKeys] = useState<Set<string>>(new Set());
    const isDataProvider = hasOutcomes(evalRuns);

    // Get all failed run IDs or keys
    const getFailedItems = () => {
        if (isDataProvider) {
            const keys = new Set<string>();
            evalRuns.forEach(run => {
                run.outcomes?.forEach(outcome => {
                    if (outcome.errorMessage) {
                        keys.add(`${run.id}-${outcome.id}`);
                    }
                });
            });
            return keys;
        } else {
            const ids = new Set<number>();
            evalRuns.forEach(run => {
                if (run.errorMessage) {
                    ids.add(run.id);
                }
            });
            return ids;
        }
    };

    const failedItems = getFailedItems();
    const hasFailedItems = failedItems.size > 0;
    const allExpanded = isDataProvider
        ? expandedKeys.size === failedItems.size && failedItems.size > 0
        : expandedRunIds.size === failedItems.size && failedItems.size > 0;

    const toggleRun = (id: number) => {
        const newSet = new Set(expandedRunIds);
        if (newSet.has(id)) {
            newSet.delete(id);
        } else {
            newSet.add(id);
        }
        setExpandedRunIds(newSet);
    };

    const toggleExpand = (key: string) => {
        const newSet = new Set(expandedKeys);
        if (newSet.has(key)) {
            newSet.delete(key);
        } else {
            newSet.add(key);
        }
        setExpandedKeys(newSet);
    };

    const toggleExpandAll = () => {
        if (allExpanded) {
            // Collapse all
            if (isDataProvider) {
                setExpandedKeys(new Set());
            } else {
                setExpandedRunIds(new Set());
            }
        } else {
            // Expand all failed items
            if (isDataProvider) {
                setExpandedKeys(failedItems as Set<string>);
            } else {
                setExpandedRunIds(failedItems as Set<number>);
            }
        }
    };

    return (
        <div className="eval-runs-view">
            {hasFailedItems && (
                <div className="eval-runs-controls">
                    <button
                        className="eval-expand-all-btn"
                        onClick={toggleExpandAll}
                    >
                        {allExpanded ? 'Collapse All' : 'Expand All'}
                    </button>
                </div>
            )}
            {isDataProvider ? (
                <DataProviderEvalListView
                    evalRuns={evalRuns}
                    expandedKeys={expandedKeys}
                    onToggleExpand={toggleExpand}
                />
            ) : (
                <SimpleEvalListView
                    evalRuns={evalRuns}
                    expandedRunIds={expandedRunIds}
                    onToggleRun={toggleRun}
                />
            )}
        </div>
    );
}
