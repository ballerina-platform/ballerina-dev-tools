-- Drop tables if they already exist to prevent conflicts
DROP TABLE IF EXISTS FunctionConnector;
DROP TABLE IF EXISTS Parameter;
DROP TABLE IF EXISTS Function;
DROP TABLE IF EXISTS Connector;
DROP TABLE IF EXISTS Package;
DROP TABLE IF EXISTS ParameterMemberType;

-- Create Package table
CREATE TABLE Package (
    package_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    org TEXT NOT NULL,
    version TEXT,
    keywords TEXT
);

-- Create Function table
CREATE TABLE Function (
    function_id INTEGER PRIMARY KEY AUTOINCREMENT,
    kind TEXT CHECK(kind IN ('FUNCTION', 'CONNECTOR', 'REMOTE', 'RESOURCE')),
    name TEXT NOT NULL,
    description TEXT,
    package_id INTEGER,
    return_type JSON, -- JSON type for return type information
    resource_path TEXT NOT NULL,
    return_error INTEGER CHECK(return_error IN (0, 1)),
    inferred_return_type INTEGER CHECK(return_error IN (0, 1)), -- Whether the return type is inferred 
    FOREIGN KEY (package_id) REFERENCES Package(package_id) ON DELETE CASCADE
);

-- Create FunctionConnector table to define actions for connectors
CREATE TABLE FunctionConnector (
    function_id INTEGER,
    connector_id INTEGER,
    PRIMARY KEY (function_id, connector_id),
    FOREIGN KEY (function_id) REFERENCES Function(function_id) ON DELETE CASCADE,
    FOREIGN KEY (connector_id) REFERENCES Function(function_id) ON DELETE CASCADE
);

-- Create Parameter table
CREATE TABLE Parameter (
    parameter_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    kind TEXT CHECK(kind IN ('REQUIRED', 'DEFAULTABLE', 'INCLUDED_RECORD', 'REST_PARAMETER',
    'INCLUDED_FIELD', 'INCLUDED_RECORD_REST', 'PARAM_FOR_TYPE_INFER', 'PATH_PARAM', 'PATH_REST_PARAM')),
    type JSON, -- JSON type for parameter type information
    default_value TEXT,
    optional INTEGER CHECK(optional IN (0, 1)),
    import_statements TEXT,
    function_id INTEGER,
    FOREIGN KEY (function_id) REFERENCES Function(function_id) ON DELETE CASCADE
);

-- Create Parameter Member Type table
CREATE TABLE ParameterMemberType (
    member_id INTEGER PRIMARY KEY AUTOINCREMENT,
    type JSON, -- JSON type for parameter type information
    kind TEXT,
    parameter_id INTEGER,
    package TEXT, -- format of the package is org:name:version
    FOREIGN KEY (parameter_id) REFERENCES Parameter(parameter_id) ON DELETE CASCADE
);
