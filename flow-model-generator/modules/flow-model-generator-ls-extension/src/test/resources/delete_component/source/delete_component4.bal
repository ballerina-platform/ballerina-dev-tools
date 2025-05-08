import ballerina/sql;
import ballerinax/mssql;
import ballerinax/mssql.driver as _;
import ballerinax/mysql;
import ballerinax/mysql.driver as _;
import ballerinax/oracledb;
import ballerinax/oracledb.driver as _;
import ballerinax/postgresql;
import ballerinax/postgresql.driver as _;

final postgresql:Client postgresqlClient = check new ();
final mysql:Client mysqlClient = check new ();
final oracledb:Client oracledbClient = check new ();
final mssql:Client mssqlClient = check new ();