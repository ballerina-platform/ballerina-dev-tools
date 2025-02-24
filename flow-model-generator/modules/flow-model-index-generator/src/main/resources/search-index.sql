-- Drop the tables if exists
DROP TABLE IF EXISTS Package;
DROP TABLE IF EXISTS Connector;
DROP TABLE IF EXISTS Function;
DROP TABLE IF EXISTS Type;

-- Create the tables
CREATE TABLE Package (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    org TEXT,
    name TEXT,
    version TEXT,
    pull_count INTEGER,
    keywords TEXT
);

CREATE TABLE Connector (
    name TEXT,
    description TEXT,
    category TEXT,
    package_id INTEGER,
    PRIMARY KEY (name, package_id),
    FOREIGN KEY (package_id) REFERENCES Package(id)
);

CREATE TABLE Function (
    name TEXT,
    description TEXT,
    package_id INTEGER,
    PRIMARY KEY (name, package_id),
    FOREIGN KEY (package_id) REFERENCES Package(id)
);

CREATE TABLE Type (
    name TEXT,
    description TEXT,
    kind TEXT,
    package_id INTEGER,
    PRIMARY KEY (name, package_id),
    FOREIGN KEY (package_id) REFERENCES Package(id)
);
