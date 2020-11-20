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

import * as React from "react";
import { Link } from '../Router'


export class SearchList extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            filteredModules: [],
            filteredFunctions: [],
            filteredClasses: [],
            filteredAbsObjects: [],
            filteredRecords: [],
            filteredConstants: [],
            filteredErrors: [],
            filteredTypes: [],
            filteredClients: [],
            filteredListeners: [],
            filteredAnnotations: [],
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
            filteredModules: window.searchData.modules,
            filteredFunctions: window.searchData.functions,
            filteredClasses: window.searchData.classes,
            filteredRecords: window.searchData.records,
            filteredConstants: window.searchData.constants,
            filteredAbsObjects: window.searchData.abstractObjects,
            filteredErrors: window.searchData.errors,
            filteredTypes: window.searchData.types,
            filteredClients: window.searchData.clients,
            filteredListeners: window.searchData.listeners,
            filteredAnnotations: window.searchData.annotations
        });
        this.handleChange();
    }

    componentWillReceiveProps(nextProps) {
        this.setState({
            filteredModules: window.searchData.modules,
            filteredFunctions: window.searchData.functions,
            filteredClasses: window.searchData.objects,
            filteredAbsObjects: window.searchData.abstractObjects,
            filteredRecords: window.searchData.records,
            filteredConstants: window.searchData.constants,
            filteredErrors: window.searchData.errors,
            filteredTypes: window.searchData.types,
            filteredClients: window.searchData.clients,
            filteredListeners: window.searchData.listeners,
            filteredAnnotations: window.searchData.annotations
        });
        this.handleChange();
    }

    onLinkClick() {
        document.getElementById("searchBox").value = "";
    }

    handleChange() {
        const mainDiv = document.getElementById("main");
        if (mainDiv != null) {
            mainDiv.classList.add('hidden');
        }
        const searchTxt = document.getElementById("searchBox").value;
        this.setState({
            searchText: searchTxt
        });
        // Variable to hold the original version of the list
        let currentModuleList = [];
        let currentFunctionsList = [];
        let currentClassesList = [];
        let currentAbsObjectsList = [];
        let currentRecordsList = [];
        let currentConstantsList = [];
        let currentErrorsList = [];
        let currentTypesList = [];
        let currentClientsList = [];
        let currentListenersList = [];
        let currentAnnotationsList = [];
        // Variable to hold the filtered list before putting into state
        let newModuleList = [];
        let newFunctionsList = [];
        let newClassesList = [];
        let newAbsObjectsList = [];
        let newRecordsList = [];
        let newConstantsList = [];
        let newErrorsList = [];
        let newTypesList = [];
        let newClientsList = [];
        let newListenersList = [];
        let newAnnotationsList = [];
        // If the search bar isn't empty
        if (searchTxt !== "") {
            // Assign the original list to currentList
            currentModuleList = window.searchData.modules;
            currentFunctionsList = window.searchData.functions;
            currentClassesList = window.searchData.classes;
            currentAbsObjectsList = window.searchData.abstractObjects;
            currentRecordsList = window.searchData.records;
            currentConstantsList = window.searchData.constants;
            currentErrorsList = window.searchData.errors;
            currentTypesList = window.searchData.types;
            currentClientsList = window.searchData.clients;
            currentListenersList = window.searchData.listeners;
            currentAnnotationsList = window.searchData.annotations;
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

            newAbsObjectsList = currentAbsObjectsList.filter((item) => {
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
            filteredAbsObjects: newAbsObjectsList,
            filteredRecords: newRecordsList,
            filteredConstants: newConstantsList,
            filteredErrors: newErrorsList,
            filteredTypes: newTypesList,
            filteredClients: newClientsList,
            filteredListeners: newListenersList,
            filteredAnnotations: newAnnotationsList
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
                        && this.state.filteredAbsObjects.length == 0 && <p>No results found</p>
                        }
                        {this.state.filteredModules.length > 0 &&
                            <div className="ui segment">
                                <h3>Modules: {this.state.filteredModules.length}</h3>
                                <table className="ui very basic table">
                                    <tbody>
                                        {this.state.filteredModules.map(item => (
                                            <tr>
                                                <td className="search-title" id={item.id} title={item.id}>
                                                    <Link to={"/" + item.id} onClick={this.onLinkClick} className="objects">
                                                    {item.id}</Link></td>
                                                <td className="search-desc"><span
                                                dangerouslySetInnerHTML={{ __html: item.description }} /></td>
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
                                                    <Link to={"/" + item.moduleId + "/classes/" + item.id} onClick={this.onLinkClick}
                                                     className="objects">{item.moduleId + ": " + item.id}</Link></td>
                                                <td className="search-desc"><span
                                                dangerouslySetInnerHTML={{ __html: item.description }} /></td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        }

                        {this.state.filteredAbsObjects.length > 0 &&
                            <div className="ui segment">
                                <h3>Abstract Objects: {this.state.filteredAbsObjects.length}</h3>
                                <table className="ui very basic table">
                                    <tbody>
                                        {this.state.filteredAbsObjects.map(item => (
                                            <tr>
                                                <td className="search-title" id={item.id} title={item.id}>
                                                    <Link to={"/" + item.moduleId + "/abstractObjects/" + item.id } onClick={this.onLinkClick}
                                                     className="objects">{item.moduleId + ": " + item.id}</Link></td>
                                                <td className="search-desc"><span
                                                dangerouslySetInnerHTML={{ __html: item.description }} /></td>
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
                                                    <Link to={"/" + item.moduleId + "/clients/" + item.id } onClick={this.onLinkClick}
                                                     className="clients">{item.moduleId + ": " + item.id}</Link></td>
                                                <td className="search-desc"><span
                                                dangerouslySetInnerHTML={{ __html: item.description }} /></td>
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
                                                    <Link to={"/" + item.moduleId + "/listeners/" + item.id} onClick={this.onLinkClick}
                                                    
                                                     className="listeners">{item.moduleId + ": " + item.id}</Link></td>
                                                <td className="search-desc"><span
                                                dangerouslySetInnerHTML={{ __html: item.description }} /></td>
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
                                                    <Link to={"/" + item.moduleId + "/functions#" + item.id} onClick={this.onLinkClick}
                                                    className="functions">{item.moduleId + ": " + item.id}</Link></td>
                                                <td className="search-desc"><span
                                                dangerouslySetInnerHTML={{ __html: item.description }} /></td>
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
                                                    <Link to={"/" + item.moduleId + "/records/" + item.id } onClick={this.onLinkClick}
                                                     className="records">{item.moduleId + ": " + item.id}</Link></td>
                                                <td className="search-desc"><span
                                                dangerouslySetInnerHTML={{ __html: item.description }} /></td>
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
                                                    <Link to={"/" + item.moduleId + "/constants#" + item.id} onClick={this.onLinkClick}
                                                    className="constant">{item.moduleId + ": " + item.id}</Link></td>
                                                <td className="search-desc"><span
                                                dangerouslySetInnerHTML={{ __html: item.description }} /></td>
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
                                                    <Link to={"/" + item.moduleId + "/types#" + item.id} onClick={this.onLinkClick}
                                                    className="types">{item.moduleId + ": " + item.id}</Link></td>
                                                <td className="search-desc"><span
                                                dangerouslySetInnerHTML={{ __html: item.description }} /></td>
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
                                                    <Link to={"/" + item.moduleId + "/errors#" + item.id} onClick={this.onLinkClick}
                                                    className="errors">{item.moduleId + ": " + item.id}</Link></td>
                                                <td className="search-desc"><span
                                                dangerouslySetInnerHTML={{ __html: item.description }} /></td>
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
                                                    <Link to={"/" + item.moduleId + "/annotations#" + item.id} onClick={this.onLinkClick}
                                                    className="annotations">{item.moduleId + ": " + item.id}</Link></td>
                                                <td className="search-desc"><span
                                                dangerouslySetInnerHTML={{ __html: item.description }} /></td>
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
