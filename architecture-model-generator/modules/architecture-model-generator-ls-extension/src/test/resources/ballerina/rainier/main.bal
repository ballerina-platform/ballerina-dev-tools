// Copyright (c) 2023 WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/io;
import ballerina/persist;
import foo/association.entities as rainier;

function checkBuilding(rainier:Client rainierClient) returns error? {
    rainier:Building building1 = {
        buildingCode: "building-1",
        city: "Colombo",
        state: "Western Province",
        country: "Sri Lanka",
        postalCode: "10370",
        'type: "rented"
    };

    rainier:BuildingInsert building2 = {
        buildingCode: "building-2",
        city: "Manhattan",
        state: "New York",
        country: "USA",
        postalCode: "10570",
        'type: "owned"
    };

    rainier:BuildingInsert building3 = {
        buildingCode: "building-3",
        city: "London",
        state: "London",
        country: "United Kingdom",
        postalCode: "39202",
        'type: "rented"
    };

    rainier:Building updatedBuilding1 = {
        buildingCode: "building-1",
        city: "Galle",
        state: "Southern Province",
        country: "Sri Lanka",
        postalCode: "10890",
        'type: "owned"
    };

    string[] buildingCodes = check rainierClient->/buildings.post([building1]);
    rainier:Building buildingRetrieved = check rainierClient->/buildings/[building1.buildingCode].get();
    buildingCodes = check rainierClient->/buildings.post([building2, building3]);
    buildingRetrieved = check rainierClient->/buildings/[building2.buildingCode].get();
    buildingRetrieved = check rainierClient->/buildings/[building3.buildingCode].get();
    buildingRetrieved = check rainierClient->/buildings/[building1.buildingCode].get();
    stream<rainier:Building, error?> buildingStream = rainierClient->/buildings.get();
    rainier:Building[] buildings = check from rainier:Building building_temp in buildingStream
        select building_temp;
    rainier:Building buildingUpdated = check rainierClient->/buildings/[building1.buildingCode].put({
        city: "Galle",
        state: "Southern Province",
        postalCode: "10890",
        'type: "owned"
    });
    buildingRetrieved = check rainierClient->/buildings/[building1.buildingCode].get();

    rainier:Building|error buildingRetrievedError = rainierClient->/buildings/["invalid-building-code"].put({
        city: "Galle",
        state: "Southern Province",
        postalCode: "10890"
    });
    if buildingRetrievedError !is persist:Error {
        panic error("Error expected");
    }
    stream<rainier:Building, error?> buildingStream2 = rainierClient->/buildings.get();
    rainier:Building[] buildingSet = check from rainier:Building building_temp2 in buildingStream2
        select building_temp2;
    rainier:Building buildingDeleted = check rainierClient->/buildings/[building1.buildingCode].delete();

    io:println("Building examples successfully executed!");

}

function checkWorkspace(rainier:Client rainierClient) returns error? {
    rainier:Workspace workspace1 = {
        workspaceId: "workspace-1",
        workspaceType: "small",
        locationBuildingCode: "building-2"
    };
    rainier:Workspace workspace2 = {
        workspaceId: "workspace-2",
        workspaceType: "medium",
        locationBuildingCode: "building-2"
    };
    rainier:Workspace workspace3 = {
        workspaceId: "workspace-3",
        workspaceType: "small",
        locationBuildingCode: "building-2"
    };

    rainier:Workspace updatedWorkspace1 = {
        workspaceId: "workspace-1",
        workspaceType: "large",
        locationBuildingCode: "building-2"
    };

    string[] workspaceIds = check rainierClient->/workspaces.post([workspace1]);
    rainier:Workspace workspaceRetrieved = check rainierClient->/workspaces/[workspace1.workspaceId].get();
    workspaceIds = check rainierClient->/workspaces.post([workspace2, workspace3]);
    workspaceRetrieved = check rainierClient->/workspaces/[workspace2.workspaceId].get();
    workspaceRetrieved = check rainierClient->/workspaces/[workspace3.workspaceId].get();
    workspaceRetrieved = check rainierClient->/workspaces/[workspace1.workspaceId].get();
    rainier:Workspace|error workspaceError = rainierClient->/workspaces/["invalid-workspace-id"].get();
    if workspaceError !is persist:Error {
        panic error("Error expected");
    }
    stream<rainier:Workspace, error?> workspaceStream = rainierClient->/workspaces.get();
    rainier:Workspace[] workspaces = check from rainier:Workspace workspace_temp in workspaceStream
        select workspace_temp;
    rainier:Workspace workspaceUpdated = check rainierClient->/workspaces/[workspace1.workspaceId].put({
        workspaceType: "large"
    });
    workspaceRetrieved = check rainierClient->/workspaces/[workspace1.workspaceId].get();

    workspaceError = rainierClient->/workspaces/["invalid-workspace-id"].put({
        workspaceType: "large"
    });

    stream<rainier:Workspace, error?> workspaceStream2 = rainierClient->/workspaces.get();
    workspaces = check from rainier:Workspace workspace_temp2 in workspaceStream2
        select workspace_temp2;
    rainier:Workspace workspaceDeleted = check rainierClient->/workspaces/[workspace1.workspaceId].delete();

    workspaceIds = check rainierClient->/workspaces.post([workspace1]);

    io:println("Workspace examples successfully executed!");

}

function checkDepartment(rainier:Client rainierClient) returns error? {
    rainier:Department department1 = {
        deptNo: "department-1",
        deptName: "Finance"
    };

    rainier:Department department2 = {
        deptNo: "department-2",
        deptName: "Marketing"
    };
    rainier:Department department3 = {
        deptNo: "department-3",
        deptName: "Engineering"
    };

    rainier:Department updatedDepartment1 = {
        deptNo: "department-1",
        deptName: "Finance & Legalities"
    };

    string[] deptNos = check rainierClient->/departments.post([department1]);
    rainier:Department departmentRetrieved = check rainierClient->/departments/[department1.deptNo].get();
    deptNos = check rainierClient->/departments.post([department2, department3]);
    departmentRetrieved = check rainierClient->/departments/[department2.deptNo].get();
    departmentRetrieved = check rainierClient->/departments/[department3.deptNo].get();
    departmentRetrieved = check rainierClient->/departments/[department1.deptNo].get();
    rainier:Department|error departmentError = rainierClient->/departments/["invalid-department-id"].get();
    if departmentError !is persist:Error {
        panic error("Error expected");
    }

    stream<rainier:Department, error?> departmentStream = rainierClient->/departments.get();
    rainier:Department[] departments = check from rainier:Department department_temp in departmentStream
        select department_temp;

    rainier:Department departmentUpdated = check rainierClient->/departments/[department1.deptNo].put({
        deptName: "Finance & Legalities"
    });

    departmentRetrieved = check rainierClient->/departments/[department1.deptNo].get();

    departmentError = rainierClient->/departments/["invalid-department-id"].put({
        deptName: "Human Resources"
    });
    if departmentError !is persist:Error {
        panic error("Error expected");
    }

    stream<rainier:Department, error?> departmentStream2 = rainierClient->/departments.get();
    departments = check from rainier:Department department_Temp2 in departmentStream2
        select department_Temp2;

    rainier:Department departmentDeleted = check rainierClient->/departments/[department1.deptNo].delete();


    io:println("Department examples successfully executed!");

}
function checkEmployee(rainier:Client rainierClient) returns error? {
    rainier:Employee employee1 = {
        empNo: "employee-1",
        firstName: "Tom",
        lastName: "Scott",
        birthDate: {year: 1992, month: 11, day: 13},
        gender: "M",
        hireDate: {year: 2022, month: 8, day: 1},
        departmentDeptNo: "department-2",
        workspaceWorkspaceId: "workspace-1"
    };
    rainier:Employee invalidEmployee = {
        empNo: "invalid-employee-no-extra-characters-to-force-failure",
        firstName: "Tom",
        lastName: "Scott",
        birthDate: {year: 1992, month: 11, day: 13},
        gender: "M",
        hireDate: {year: 2022, month: 8, day: 1},
        departmentDeptNo: "department-2",
        workspaceWorkspaceId: "workspace-2"
    };
    rainier:Employee employee2 = {
        empNo: "employee-2",
        firstName: "Jane",
        lastName: "Doe",
        birthDate: {year: 1996, month: 9, day: 15},
        gender: "F",
        hireDate: {year: 2022, month: 6, day: 1},
        departmentDeptNo: "department-2",
        workspaceWorkspaceId: "workspace-2"
    };
    rainier:Employee employee3 = {
        empNo: "employee-3",
        firstName: "Hugh",
        lastName: "Smith",
        birthDate: {year: 1986, month: 9, day: 15},
        gender: "F",
        hireDate: {year: 2021, month: 6, day: 1},
        departmentDeptNo: "department-3",
        workspaceWorkspaceId: "workspace-3"
    };

    rainier:Employee updatedEmployee1 = {
        empNo: "employee-1",
        firstName: "Tom",
        lastName: "Jones",
        birthDate: {year: 1994, month: 11, day: 13},
        gender: "M",
        hireDate: {year: 2022, month: 8, day: 1},
        departmentDeptNo: "department-3",
        workspaceWorkspaceId: "workspace-2"
    };

    string[] empNos = check rainierClient->/employees.post([employee1]);

    rainier:Employee employeeRetrieved = check rainierClient->/employees/[employee1.empNo].get();


    empNos = check rainierClient->/employees.post([employee2, employee3]);
    employeeRetrieved = check rainierClient->/employees/[employee2.empNo].get();

    employeeRetrieved = check rainierClient->/employees/[employee3.empNo].get();
    employeeRetrieved = check rainierClient->/employees/[employee1.empNo].get();
    rainier:Employee|error employeeError = rainierClient->/employees/["invalid-employee-id"].get();
    stream<rainier:Employee, error?> employeeStream = rainierClient->/employees.get();
    rainier:Employee[] employees = check from rainier:Employee employee in employeeStream
        select employee;

    rainier:Employee employeeUpdated = check rainierClient->/employees/[employee1.empNo].put({
        lastName: "Jones",
        departmentDeptNo: "department-3",
        birthDate: {year: 1994, month: 11, day: 13}
    });
    employeeRetrieved = check rainierClient->/employees/[employee1.empNo].get();
    employeeError = rainierClient->/employees/["invalid-employee-id"].put({
        lastName: "Jones"
    });

    employeeError = rainierClient->/employees/[employee1.empNo].put({
        workspaceWorkspaceId: "invalid-workspaceWorkspaceId"
    });

    stream<rainier:Employee, error?> employeeStream2 = rainierClient->/employees.get();
    employees = check from rainier:Employee employee_temp2 in employeeStream2
        select employee_temp2;

    rainier:Employee employeeDeleted = check rainierClient->/employees/[employee1.empNo].delete();
    io:println("Employee examples successfully executed!");
}

function checkOrderItem(rainier:Client rainierClient) returns error? {
    rainier:OrderItem orderItem1 = {
        orderId: "order-1",
        itemId: "item-1",
        quantity: 5,
        notes: "none"
    };
    rainier:OrderItem orderItem2 = {
        orderId: "order-2",
        itemId: "item-2",
        quantity: 10,
        notes: "more"
    };
    rainier:OrderItem orderItem2Updated = {
        orderId: "order-2",
        itemId: "item-2",
        quantity: 20,
        notes: "more than more"
    };

    [string, string][] ids = check rainierClient->/orderitems.post([orderItem1, orderItem2]);

    rainier:OrderItem orderItemRetrieved = check rainierClient->/orderitems/[orderItem1.orderId]/[orderItem1.itemId].get();

    orderItemRetrieved = check rainierClient->/orderitems/[orderItem2.orderId]/[orderItem2.itemId].get();

    [string, string][]|error idsError = rainierClient->/orderitems.post([orderItem1]);
    stream<rainier:OrderItem, error?> orderItemStream = rainierClient->/orderitems.get();
    rainier:OrderItem[] orderitems = check from rainier:OrderItem orderItemTemp in orderItemStream
        select orderItemTemp;
    rainier:OrderItem orderItem = check rainierClient->/orderitems/[orderItem1.orderId]/[orderItem1.itemId].get();
    rainier:OrderItem|error orderItemError = rainierClient->/orderitems/["invalid-order-id"]/[orderItem1.itemId].get();
    orderItemError = rainierClient->/orderitems/[orderItem1.itemId]/["invalid-order-id"].get();
    rainier:OrderItem orderItemUpdated = check rainierClient->/orderitems/[orderItem2.orderId]/[orderItem2.itemId].put({
        quantity: orderItem2Updated.quantity,
        notes: orderItem2Updated.notes
    });
    orderItem = check rainierClient->/orderitems/[orderItem2.orderId]/[orderItem2.itemId].get();
    orderItemError = rainierClient->/orderitems/[orderItem2.orderId]/[orderItem2.itemId].put({
        quantity: 239,
        notes: "updated notes"
    });
    rainier:OrderItem orderItemDeleted = check rainierClient->/orderitems/[orderItem2.orderId]/[orderItem2.itemId].delete();
    orderItemError = rainierClient->/orderitems/[orderItem2.orderId]/[orderItem2.itemId].get();
    if orderItemError !is persist:Error {
        panic error("Error expected");
    }
    orderItemError = rainierClient->/orderitems/["invalid-order-id"]/[orderItem2.itemId].delete();
    if orderItemError !is persist:Error {
        panic error("Error expected");
    }

    io:println("OrderItem examples successfully executed!");
}

public function main() returns error? {
    rainier:Client rainierClient = check new ();

    check checkBuilding(rainierClient);
    check checkDepartment(rainierClient);
    check checkWorkspace(rainierClient);
    check checkEmployee(rainierClient);
    check checkOrderItem(rainierClient);
    
    io:println("\n========== Building ==========");
    _ = check from rainier:Building building in rainierClient->/buildings.get(rainier:Building)
        do {
            io:println(building);
        };

    io:println("\n========== Workspace ==========");
    _ = check from rainier:Workspace workspace in rainierClient->/workspaces.get(rainier:Workspace)
        do {
            io:println(workspace);
        };
    io:println("\n========== Department ==========");
    _ = check from rainier:Department department in rainierClient->/departments.get(rainier:Department)
        do {
            io:println(department);
        };
    io:println("\n========== Employee ==========");
    _ = check from rainier:Employee employee in rainierClient->/employees.get(rainier:Employee)
        do {
            io:println(employee);
        };
    io:println("\n========== OrderItem ==========");
    _ = check from rainier:OrderItem orderIt in rainierClient->/orderitems.get(rainier:OrderItem)
        do {
            io:println(orderIt);
        };

}
