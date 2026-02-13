export type TestData = {
    projectName: string;
    totalTests: number;
    passed: number;
    failed: number;
    skipped: number;
    coveredLines: number;
    missedLines: number;
    coveragePercentage: number;
    moduleStatus: ModuleStatus[];
    moduleCoverage: ModuleCoverage[];
}

// Workspace contains multiple projects
export type WorkspaceData = {
    workspaceName: string;
    totalTests: number;
    passed: number;
    failed: number;
    skipped: number;
    coveredLines: number;
    missedLines: number;
    coveragePercentage: number;
    packages: TestData[];
};

// Single project structure (same as TestData but without packages array)
export type SingleProjectData = TestData;

// Union type that can handle both structures
export type ReportData = WorkspaceData | SingleProjectData;

// Helper functions to determine and work with data structure
export function isWorkspaceData(data: ReportData): data is WorkspaceData {
    return 'workspaceName' in data && 'packages' in data;
}

export function isSingleProjectData(data: ReportData): data is SingleProjectData {
    return 'projectName' in data && !('packages' in data);
}

// Function to normalize data to workspace structure
export function normalizeToWorkspaceData(data: ReportData): WorkspaceData {
    if (isWorkspaceData(data)) {
        return data;
    } else {
        // Convert single project to workspace structure
        return {
            workspaceName: data.projectName,
            totalTests: data.totalTests,
            passed: data.passed,
            failed: data.failed,
            skipped: data.skipped,
            coveredLines: data.coveredLines,
            missedLines: data.missedLines,
            coveragePercentage: data.coveragePercentage,
            packages: [data]
        };
    }
}

// The report now expects a workspace containing multiple projects
export type TestDataArray = TestData[];

export type ModuleStatus = {
    name: string;
    totalTests: number;
    passed: number;
    failed: number;
    skipped: number;
    tests: Test[];
}

export type Test = {
    name: string;
    status: string;
    failureMessage?: string;
    evaluationSummary?: EvaluationSummary;
}

export type ModuleCoverage = {
    name: string;
    coveredLines: number;
    missedLines: number;
    coveragePercentage: number;
    sourceFiles: SourceFile[];
}

export type SourceFile = {
    name: string;
    coveredLines: number[];
    missedLines: number[];
    coveragePercentage: number;
    sourceCode: string;
}

// Outcome for data-provider based evaluation
export type EvalOutcome = {
    id: string;
    errorMessage?: string;
}

// Single evaluation run
export type EvalRun = {
    id: number;
    errorMessage?: string;        // For simple evals (no data provider)
    outcomes?: EvalOutcome[];     // For data-provider evals
    passRate?: number;
}

// Evaluation summary structure (as it appears in JSON)
export type EvaluationSummary = {
    evaluationRuns: EvalRun[];
    targetPassRate: number;
    observedPassRate: number;
}

// Helper function to check if a test is an evaluation test
export function isEvaluationTest(test: Test): boolean {
    return test.evaluationSummary !== undefined &&
        Array.isArray(test.evaluationSummary.evaluationRuns) &&
        test.evaluationSummary.evaluationRuns.length > 0;
}
