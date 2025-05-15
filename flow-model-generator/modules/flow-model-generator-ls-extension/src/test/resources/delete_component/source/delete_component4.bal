import ballerina/sql;
import ballerinax/postgresql;
import ballerinax/postgresql.driver as _;

final postgresql:Client postgresqlClient = check new ();
