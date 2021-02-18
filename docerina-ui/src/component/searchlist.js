/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

import React from "react";
import { Link } from '../Router'
import { getPackageName } from "./helper"

export class SearchList extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            filteredModules: [],
            filteredFunctions: [],
            filteredClasses: [],
            filteredObjTypes: [],
            filteredRecords: [],
            filteredConstants: [],
            filteredErrors: [],
            filteredTypes: [],
            filteredClients: [],
            filteredListeners: [],
            filteredAnnotations: [],
            filteredEnums: [],
            searchText: null
        };
        this.handleChange = this.handleChange.bind(this);
    }

    componentDidMount() {
        const searchBox = document.getElementById("searchBox");
        if (searchBox != null) {
            searchBox.addEventListener("keyup", this.handleChange);
        }
        const searchBoxMob = document.getElementById("searchBoxMob");
        if (searchBoxMob != null) {
            searchBoxMob.addEventListener("keyup", this.handleChange);
        }

        this.setState({
            filteredModules: this.props.searchData.modules,
            filteredFunctions: this.props.searchData.functions,
            filteredClasses: this.props.searchData.classes,
            filteredRecords: this.props.searchData.records,
            filteredConstants: this.props.searchData.constants,
            filteredObjTypes: this.props.searchData.objectTypes,
            filteredErrors: this.props.searchData.errors,
            filteredTypes: this.props.searchData.types,
            filteredClients: this.props.searchData.clients,
            filteredListeners: this.props.searchData.listeners,
            filteredAnnotations: this.props.searchData.annotations,
            filteredEnums: this.props.searchData.enums
        });
        this.handleChange();
    }

    componentWillReceiveProps(nextProps) {
        this.setState({
            filteredModules: this.props.searchData.modules,
            filteredFunctions: this.props.searchData.functions,
            filteredClasses: this.props.searchData.objects,
            filteredObjTypes: this.props.searchData.objectTypes,
            filteredRecords: this.props.searchData.records,
            filteredConstants: this.props.searchData.constants,
            filteredErrors: this.props.searchData.errors,
            filteredTypes: this.props.searchData.types,
            filteredClients: this.props.searchData.clients,
            filteredListeners: this.props.searchData.listeners,
            filteredAnnotations: this.props.searchData.annotations,
            filteredEnums: this.props.searchData.enums

        });
        this.handleChange();
    }

    onLinkClick() {
        document.getElementById("searchBox").value = "";
        document.getElementById("searchBoxMob").value = "";

    }

    handleChange() {
        const mainDiv = document.getElementById("main");
        if (mainDiv != null) {
            mainDiv.classList.add('hidden');
        }
        const searchTxtDesktop = document.getElementById("searchBox").value;
        const searchTxtMobile = document.getElementById("searchBoxMob").value;
        let searchTxt = "";
        if (searchTxtDesktop != "") {
            searchTxt = searchTxtDesktop;
        } else if (searchTxtMobile != "") {
            searchTxt = searchTxtMobile;

        }
        this.setState({
            searchText: searchTxt
        });
        // Variable to hold the original version of the list
        let currentModuleList = [];
        let currentFunctionsList = [];
        let currentClassesList = [];
        let currentObjTypesList = [];
        let currentRecordsList = [];
        let currentConstantsList = [];
        let currentErrorsList = [];
        let currentTypesList = [];
        let currentClientsList = [];
        let currentListenersList = [];
        let currentAnnotationsList = [];
        let currentEnumsList = [];
        // Variable to hold the filtered list before putting into state
        let newModuleList = [];
        let newFunctionsList = [];
        let newClassesList = [];
        let newObjTypesList = [];
        let newRecordsList = [];
        let newConstantsList = [];
        let newErrorsList = [];
        let newTypesList = [];
        let newClientsList = [];
        let newListenersList = [];
        let newAnnotationsList = [];
        let newEnumsList = [];
        // If the search bar isn't empty
        if (searchTxt !== "") {
            // Assign the original list to currentList
            currentModuleList = this.props.searchData.modules;
            currentFunctionsList = this.props.searchData.functions;
            currentClassesList = this.props.searchData.classes;
            currentObjTypesList = this.props.searchData.objectTypes;
            currentRecordsList = this.props.searchData.records;
            currentConstantsList = this.props.searchData.constants;
            currentErrorsList = this.props.searchData.errors;
            currentTypesList = this.props.searchData.types;
            currentClientsList = this.props.searchData.clients;
            currentListenersList = this.props.searchData.listeners;
            currentAnnotationsList = this.props.searchData.annotations;
            currentEnumsList = this.props.searchData.enums;
            // Use .filter() to determine which items should be displayed
            // based on the search terms
            newModuleList = currentModuleList.filter((item) => {
                // change current item to lowercase
                const lc = item.id.toLowerCase();
                // change search term to lowercase
                const filter = searchTxt.toLowerCase();
                // check to see if the current list item includes the search term
                // If it does, it will be added to newList. Using lowercase eliminates
                // issues with capitalization in search terms and search content
                return lc.includes(filter);
            });

            newFunctionsList = currentFunctionsList.filter((item) => {
                const lc = item.id.toLowerCase();
                const filter = searchTxt.toLowerCase();
                return lc.includes(filter);
            });

            newClassesList = currentClassesList.filter((item) => {
                const lc = item.id.toLowerCase();
                const filter = searchTxt.toLowerCase();
                return lc.includes(filter);
            });

            newObjTypesList = currentObjTypesList.filter((item) => {
                const lc = item.id.toLowerCase();
                const filter = searchTxt.toLowerCase();
                return lc.includes(filter);
            });

            newRecordsList = currentRecordsList.filter((item) => {
                const lc = item.id.toLowerCase();
                const filter = searchTxt.toLowerCase();
                return lc.includes(filter);
            });

            newConstantsList = currentConstantsList.filter((item) => {
                const lc = item.id.toLowerCase();
                const filter = searchTxt.toLowerCase();
                return lc.includes(filter);
            });

            newErrorsList = currentErrorsList.filter((item) => {
                const lc = item.id.toLowerCase();
                const filter = searchTxt.toLowerCase();
                return lc.includes(filter);
            });

            newTypesList = currentTypesList.filter((item) => {
                const lc = item.id.toLowerCase();
                const filter = searchTxt.toLowerCase();
                return lc.includes(filter);
            });

            newClientsList = currentClientsList.filter((item) => {
                const lc = item.id.toLowerCase();
                const filter = searchTxt.toLowerCase();
                return lc.includes(filter);
            });

            newListenersList = currentListenersList.filter((item) => {
                const lc = item.id.toLowerCase();
                const filter = searchTxt.toLowerCase();
                return lc.includes(filter);
            });

            newAnnotationsList = currentAnnotationsList.filter((item) => {
                const lc = item.id.toLowerCase();
                const filter = searchTxt.toLowerCase();
                return lc.includes(filter);
            });

            newEnumsList = currentEnumsList.filter((item) => {
                const lc = item.id.toLowerCase();
                const filter = searchTxt.toLowerCase();
                return lc.includes(filter);
            });

        } else {
            if (mainDiv != null) {
                mainDiv.classList.remove('hidden');
            }
        }
        // Set the filtered state based on what our rules added to newList
        this.setState({
            filteredModules: newModuleList,
            filteredFunctions: newFunctionsList,
            filteredClasses: newClassesList,
            filteredObjTypes: newObjTypesList,
            filteredRecords: newRecordsList,
            filteredConstants: newConstantsList,
            filteredErrors: newErrorsList,
            filteredTypes: newTypesList,
            filteredClients: newClientsList,
            filteredListeners: newListenersList,
            filteredAnnotations: newAnnotationsList,
            filteredEnums: newEnumsList
        });
    }

    render() {
        return (
            <div>
                {this.state.searchText &&
                    <div className="search-list">
                        <h1>Search results for '{this.state.searchText}'</h1>
                        {this.state.filteredModules.length == 0 && this.state.filteredClasses.length == 0 &&
                            this.state.filteredFunctions.length == 0 && this.state.filteredRecords.length == 0 &&
                            this.state.filteredConstants.length == 0 && this.state.filteredTypes.length == 0 &&
                            this.state.filteredErrors.length == 0 && this.state.filteredClients.length == 0 &&
                            this.state.filteredListeners.length == 0 && this.state.filteredAnnotations.length == 0
                            && this.state.filteredObjTypes.length == 0 && this.state.filteredEnums.length == 0 && <p>No results found</p>
                        }
                        {this.state.filteredModules.length > 0 &&
                            <div className="ui segment">
                                <h3>Modules: {this.state.filteredModules.length}</h3>
                                <table className="ui very basic table">
                                    <tbody>
                                        {this.state.filteredModules.map(item => (
                                            <tr>
                                                <td className="search-title" id={item.id} title={item.id}>
                                                    <Link to={"/" + item.orgName + "/" + getPackageName(item.id) + "/" + item.version + "/" + item.id} onClick={this.onLinkClick} className="objects">
                                                        {item.id}</Link></td>
                                                <td className="search-desc">
                                                    <p>{item.description}</p>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        }

                        {this.state.filteredClasses.length > 0 &&
                            <div className="ui segment">
                                <h3>Classes: {this.state.filteredClasses.length}</h3>
                                <table className="ui very basic table">
                                    <tbody>
                                        {this.state.filteredClasses.map(item => (
                                            <tr>
                                                <td className="search-title" id={item.id} title={item.id}>
                                                    <Link to={"/" + item.moduleOrgName + "/" + getPackageName(item.moduleId) + "/" + item.moduleVersion + "/" + item.moduleId + "/classes/" + item.id} onClick={this.onLinkClick}
                                                        className="objects">{item.moduleId + ": " + item.id}</Link></td>
                                                <td className="search-desc">
                                                    <p>{item.description}</p>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        }

                        {this.state.filteredObjTypes.length > 0 &&
                            <div className="ui segment">
                                <h3>Object Types: {this.state.filteredObjTypes.length}</h3>
                                <table className="ui very basic table">
                                    <tbody>
                                        {this.state.filteredObjTypes.map(item => (
                                            <tr>
                                                <td className="search-title" id={item.id} title={item.id}>
                                                    <Link to={"/" + item.moduleOrgName + "/" + getPackageName(item.moduleId) + "/" + item.moduleVersion + "/" + item.moduleId + "/objectTypes/" + item.id} onClick={this.onLinkClick}
                                                        className="objects">{item.moduleId + ": " + item.id}</Link></td>
                                                <td className="search-desc">
                                                    <p>{item.description}</p>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        }

                        {this.state.filteredClients.length > 0 &&
                            <div className="ui segment">
                                <h3>Clients: {this.state.filteredClients.length}</h3>
                                <table className="ui very basic table">
                                    <tbody>
                                        {this.state.filteredClients.map(item => (
                                            <tr>
                                                <td className="search-title" id={item.id} title={item.id}>
                                                    <Link to={"/" + item.moduleOrgName + "/" + getPackageName(item.moduleId) + "/" + item.moduleVersion + "/" + item.moduleId + "/clients/" + item.id} onClick={this.onLinkClick}
                                                        className="clients">{item.moduleId + ": " + item.id}</Link></td>
                                                <td className="search-desc">
                                                    <p>{item.description}</p>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        }

                        {this.state.filteredListeners.length > 0 &&
                            <div className="ui segment">
                                <h3>Listeners: {this.state.filteredListeners.length}</h3>
                                <table className="ui very basic table">
                                    <tbody>
                                        {this.state.filteredListeners.map(item => (
                                            <tr>
                                                <td className="search-title" id={item.id} title={item.id}>
                                                    <Link to={"/" + item.moduleOrgName + "/" + getPackageName(item.moduleId) + "/" + item.moduleVersion + "/" + item.moduleId + "/listeners/" + item.id} onClick={this.onLinkClick}

                                                        className="listeners">{item.moduleId + ": " + item.id}</Link></td>
                                                <td className="search-desc">
                                                    <p>{item.description}</p>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        }

                        {this.state.filteredFunctions.length > 0 &&
                            <div className="ui segment">
                                <h3>Functions: {this.state.filteredFunctions.length}</h3>
                                <table className="ui very basic table">
                                    <tbody>
                                        {this.state.filteredFunctions.map(item => (
                                            <tr>
                                                <td className="search-title" id={item.id} title={item.id}>
                                                    <Link to={"/" + item.moduleOrgName + "/" + getPackageName(item.moduleId) + "/" + item.moduleVersion + "/" + item.moduleId + "/functions#" + item.id} onClick={this.onLinkClick}
                                                        className="functions">{item.moduleId + ": " + item.id}</Link></td>
                                                <td className="search-desc">
                                                    <p>{item.description}</p>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        }

                        {this.state.filteredRecords.length > 0 &&
                            <div className="ui segment">
                                <h3>Records: {this.state.filteredRecords.length}</h3>
                                <table className="ui very basic table">
                                    <tbody>
                                        {this.state.filteredRecords.map(item => (
                                            <tr>
                                                <td className="search-title" id={item.id} title={item.id}>
                                                    <Link to={"/" + item.moduleOrgName + "/" + getPackageName(item.moduleId) + "/" + item.moduleVersion + "/" + item.moduleId + "/records/" + item.id} onClick={this.onLinkClick}
                                                        className="records">{item.moduleId + ": " + item.id}</Link></td>
                                                <td className="search-desc">
                                                    <p>{item.description}</p>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        }

                        {this.state.filteredConstants.length > 0 &&
                            <div className="ui segment">
                                <h3>Constants: {this.state.filteredConstants.length}</h3>
                                <table className="ui very basic table">
                                    <tbody>
                                        {this.state.filteredConstants.map(item => (
                                            <tr>
                                                <td className="search-title" id={item.id} title={item.id}>
                                                    <Link to={"/" + item.moduleOrgName + "/" + getPackageName(item.moduleId) + "/" + item.moduleVersion + "/" + item.moduleId + "/constants#" + item.id} onClick={this.onLinkClick}
                                                        className="constant">{item.moduleId + ": " + item.id}</Link></td>
                                                <td className="search-desc">
                                                    <p>{item.description}</p>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        }

                        {this.state.filteredEnums.length > 0 &&
                            <div className="ui segment">
                                <h3>Enums: {this.state.filteredEnums.length}</h3>
                                <table className="ui very basic table">
                                    <tbody>
                                        {this.state.filteredEnums.map(item => (
                                            <tr>
                                                <td className="search-title" id={item.id} title={item.id}>
                                                    <Link to={"/" + item.moduleOrgName + "/" + getPackageName(item.moduleId) + "/" + item.moduleVersion + "/" + item.moduleId + "/enums/" + item.id} onClick={this.onLinkClick}
                                                        className="constant">{item.moduleId + ": " + item.id}</Link></td>
                                                <td className="search-desc">
                                                    <p>{item.description}</p>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        }

                        {this.state.filteredTypes.length > 0 &&
                            <div className="ui segment">
                                <h3>Types: {this.state.filteredTypes.length}</h3>
                                <table className="ui very basic table">
                                    <tbody>
                                        {this.state.filteredTypes.map(item => (
                                            <tr>
                                                <td className="search-title" id={item.id} title={item.id}>
                                                    <Link to={"/" + item.moduleOrgName + "/" + getPackageName(item.moduleId) + "/" + item.moduleVersion + "/" + item.moduleId + "/types#" + item.id} onClick={this.onLinkClick}
                                                        className="types">{item.moduleId + ": " + item.id}</Link></td>
                                                <td className="search-desc">
                                                    <p>{item.description}</p>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        }

                        {this.state.filteredErrors.length > 0 &&
                            <div className="ui segment">
                                <h3>Errors: {this.state.filteredErrors.length}</h3>
                                <table className="ui very basic table">
                                    <tbody>
                                        {this.state.filteredErrors.map(item => (
                                            <tr>
                                                <td className="search-title" id={item.id} title={item.id}>
                                                    <Link to={"/" + item.moduleOrgName + "/" + getPackageName(item.moduleId) + "/" + item.moduleVersion + "/" + item.moduleId + "/errors#" + item.id} onClick={this.onLinkClick}
                                                        className="errors">{item.moduleId + ": " + item.id}</Link></td>
                                                <td className="search-desc">
                                                    <p>{item.description}</p>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        }

                        {this.state.filteredAnnotations.length > 0 &&
                            <div className="ui segment">
                                <h3>Annotations: {this.state.filteredAnnotations.length}</h3>
                                <table className="ui very basic table">
                                    <tbody>
                                        {this.state.filteredAnnotations.map(item => (
                                            <tr>
                                                <td className="search-title" id={item.id} title={item.id}>
                                                    <Link to={"/" + item.moduleOrgName + "/" + getPackageName(item.moduleId) + "/" + item.moduleVersion + "/" + item.moduleId + "/annotations#" + item.id} onClick={this.onLinkClick}
                                                        className="annotations">{item.moduleId + ": " + item.id}</Link></td>
                                                <td className="search-desc">
                                                    <p>{item.description}</p>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        }

                    </div>
                }
            </div>
        )
    }
}
