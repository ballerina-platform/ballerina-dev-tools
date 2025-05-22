-- Drop existing tables
DROP TABLE IF EXISTS Package;
DROP TABLE IF EXISTS Connector;
DROP TABLE IF EXISTS Function;
DROP TABLE IF EXISTS Type;
DROP TABLE IF EXISTS PackageFTS;
DROP TABLE IF EXISTS ConnectorFTS;
DROP TABLE IF EXISTS FunctionFTS;
DROP TABLE IF EXISTS TypeFTS;

-- Create the main tables
CREATE TABLE Package (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    org TEXT,
    name TEXT NOT NULL,
    package_name TEXT NOT NULL,
    version TEXT,
    pull_count INTEGER,
    keywords TEXT
);

CREATE TABLE Connector (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    category TEXT,
    package_id INTEGER NOT NULL,
    UNIQUE(name, package_id),
    FOREIGN KEY (package_id) REFERENCES Package(id)
);

CREATE TABLE Function (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    package_id INTEGER NOT NULL,
    UNIQUE(name, package_id),
    FOREIGN KEY (package_id) REFERENCES Package(id)
);

CREATE TABLE Type (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    kind TEXT,
    package_id INTEGER NOT NULL,
    UNIQUE(name, package_id),
    FOREIGN KEY (package_id) REFERENCES Package(id)
);

-- Create FTS tables for search
CREATE VIRTUAL TABLE ConnectorFTS USING fts5(
    name,
    description,
    package_name, 
    content='Connector',
    content_rowid='id'
);

CREATE VIRTUAL TABLE FunctionFTS USING fts5(
    name,
    description,
    package_name, 
    content='Function',
    content_rowid='id',
);

CREATE VIRTUAL TABLE TypeFTS USING fts5(
    name,
    description,
    package_name, 
    content='Type',
    content_rowid='id',
);

-- Create rank function for FTS tables
INSERT INTO ConnectorFTS(ConnectorFTS, rank) VALUES('rank', 'bm25(10.0, 2.0, 5.0)');
INSERT INTO FunctionFTS(FunctionFTS, rank) VALUES('rank', 'bm25(10.0, 2.0, 5.0)');
INSERT INTO TypeFTS(TypeFTS, rank) VALUES('rank', 'bm25(10.0, 2.0, 5.0)');

-- Create triggers to keep FTS tables in sync with main tables
CREATE TRIGGER connector_ai AFTER INSERT ON Connector BEGIN
    INSERT INTO ConnectorFTS(rowid, name, description, package_name) 
    VALUES (new.id, new.name, new.description, 
           (SELECT name FROM Package WHERE id = new.package_id));
END;

CREATE TRIGGER function_ai AFTER INSERT ON Function BEGIN
    INSERT INTO FunctionFTS(rowid, name, description, package_name) 
    VALUES (new.id, new.name, new.description,
           (SELECT name FROM Package WHERE id = new.package_id));
END;

CREATE TRIGGER type_ai AFTER INSERT ON Type BEGIN
    INSERT INTO TypeFTS(rowid, name, description, package_name) 
    VALUES (new.id, new.name, new.description,
           (SELECT name FROM Package WHERE id = new.package_id));
END;

CREATE TRIGGER connector_ad AFTER DELETE ON Connector BEGIN
    INSERT INTO ConnectorFTS(ConnectorFTS, rowid, name, description, package_name) 
    VALUES('delete', old.id, old.name, old.description, 
           (SELECT name FROM Package WHERE id = old.package_id));
END;

CREATE TRIGGER function_ad AFTER DELETE ON Function BEGIN
    INSERT INTO FunctionFTS(FunctionFTS, rowid, name, description, package_name) 
    VALUES('delete', old.id, old.name, old.description, 
           (SELECT name FROM Package WHERE id = old.package_id));
END;

CREATE TRIGGER type_ad AFTER DELETE ON Type BEGIN
    INSERT INTO TypeFTS(TypeFTS, rowid, name, description, package_name) 
    VALUES('delete', old.id, old.name, old.description, 
           (SELECT name FROM Package WHERE id = old.package_id));
END;
