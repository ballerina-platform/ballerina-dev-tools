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

const SearchList = (props) => {

    const onLinkClick = () => {
        console.log("on click");
        document.getElementById("searchBox").value = "";
        var resetSearch = props.resetFunc;
        resetSearch();
    }

    let searchTxt = props.searchText;

    // Variables to hold the filtered list
    let filteredModuleList = [];
    let filteredFunctionsList = [];
    let filteredClassesList = [];
    let filteredObjTypesList = [];
    let filteredRecordsList = [];
    let filteredConstantsList = [];
    let filteredErrorsList = [];
    let filteredTypesList = [];
    let filteredClientsList = [];
    let filteredListenersList = [];
    let filteredAnnotationsList = [];
    let filteredEnumsList = [];

    // Use .filter() to determine which items should be displayed
    // based on the search terms
    filteredModuleList = props.searchData.modules.filter((item) => {
        // change current item to lowercase
        const lc = item.id.toLowerCase();
        // change search term to lowercase
        const filter = searchTxt.toLowerCase();
        // check to see if the current list item includes the search term
        // If it does, it will be added to filteredList. Using lowercase eliminates
        // issues with capitalization in search terms and search content
        return lc.includes(filter);
    });

    filteredFunctionsList = props.searchData.functions.filter((item) => {
        const lc = item.id.toLowerCase();
        const filter = searchTxt.toLowerCase();
        return lc.includes(filter);
    });

    filteredClassesList = props.searchData.classes.filter((item) => {
        const lc = item.id.toLowerCase();
        const filter = searchTxt.toLowerCase();
        return lc.includes(filter);
    });

    filteredObjTypesList = props.searchData.objectTypes.filter((item) => {
        const lc = item.id.toLowerCase();
        const filter = searchTxt.toLowerCase();
        return lc.includes(filter);
    });

    filteredRecordsList = props.searchData.records.filter((item) => {
        const lc = item.id.toLowerCase();
        const filter = searchTxt.toLowerCase();
        return lc.includes(filter);
    });

    filteredConstantsList = props.searchData.constants.filter((item) => {
        const lc = item.id.toLowerCase();
        const filter = searchTxt.toLowerCase();
        return lc.includes(filter);
    });

    filteredErrorsList = props.searchData.errors.filter((item) => {
        const lc = item.id.toLowerCase();
        const filter = searchTxt.toLowerCase();
        return lc.includes(filter);
    });

    filteredTypesList = props.searchData.types.filter((item) => {
        const lc = item.id.toLowerCase();
        const filter = searchTxt.toLowerCase();
        return lc.includes(filter);
    });

    filteredClientsList = props.searchData.clients.filter((item) => {
        const lc = item.id.toLowerCase();
        const filter = searchTxt.toLowerCase();
        return lc.includes(filter);
    });

    filteredListenersList = props.searchData.listeners.filter((item) => {
        const lc = item.id.toLowerCase();
        const filter = searchTxt.toLowerCase();
        return lc.includes(filter);
    });

    filteredAnnotationsList = props.searchData.annotations.filter((item) => {
        const lc = item.id.toLowerCase();
        const filter = searchTxt.toLowerCase();
        return lc.includes(filter);
    });

    filteredEnumsList = props.searchData.enums.filter((item) => {
        const lc = item.id.toLowerCase();
        const filter = searchTxt.toLowerCase();
        return lc.includes(filter);
    });

    return (
        <div>
            {props.searchText &&
                <div className="search-list">
                    <h1>Search results for '{props.searchText}'</h1>
                    {filteredModuleList.length == 0 && filteredClassesList.length == 0 &&
                        filteredFunctionsList.length == 0 && filteredRecordsList.length == 0 &&
                        filteredConstantsList.length == 0 && filteredTypesList.length == 0 &&
                        filteredErrorsList.length == 0 && filteredClientsList.length == 0 &&
                        filteredListenersList.length == 0 && filteredAnnotationsList.length == 0
                        && filteredObjTypesList.length == 0 && filteredEnumsList.length == 0 && <p>No results found</p>
                    }
                    {filteredModuleList.length > 0 &&
                        <div className="ui segment">
                            <h3>Modules: {filteredModuleList.length}</h3>
                            <table className="ui very basic table">
                                <tbody>
                                    {filteredModuleList.map(item => (
                                        <tr>
                                            <td className="search-title" id={item.id} title={item.id}>
                                                <Link to={`/${item.orgName}/${item.id}/${item.version}`} className="objects">
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

                    {filteredClassesList.length > 0 &&
                        <div className="ui segment">
                            <h3>Classes: {filteredClassesList.length}</h3>
                            <table className="ui very basic table">
                                <tbody>
                                    {filteredClassesList.map(item => (
                                        <tr>
                                            <td className="search-title" id={item.id} title={item.id}>
                                                <Link to={`/${item.moduleOrgName}/${item.moduleId}/${item.moduleVersion}/classes/${item.id}`} onClick={onLinkClick}
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

                    {filteredObjTypesList.length > 0 &&
                        <div className="ui segment">
                            <h3>Object Types: {filteredObjTypesList.length}</h3>
                            <table className="ui very basic table">
                                <tbody>
                                    {filteredObjTypesList.map(item => (
                                        <tr>
                                            <td className="search-title" id={item.id} title={item.id}>
                                                <Link to={`/${item.moduleOrgName}/${item.moduleId}/${item.moduleVersion}/objectTypes/${item.id}`} onClick={onLinkClick}
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

                    {filteredClientsList.length > 0 &&
                        <div className="ui segment">
                            <h3>Clients: {filteredClientsList.length}</h3>
                            <table className="ui very basic table">
                                <tbody>
                                    {filteredClientsList.map(item => (
                                        <tr>
                                            <td className="search-title" id={item.id} title={item.id}>
                                                <Link to={`/${item.moduleOrgName}/${item.moduleId}/${item.moduleVersion}/clients/${item.id}`} onClick={onLinkClick}
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

                    {filteredListenersList.length > 0 &&
                        <div className="ui segment">
                            <h3>Listeners: {filteredListenersList.length}</h3>
                            <table className="ui very basic table">
                                <tbody>
                                    {filteredListenersList.map(item => (
                                        <tr>
                                            <td className="search-title" id={item.id} title={item.id}>
                                                <Link to={`/${item.moduleOrgName}/${item.moduleId}/${item.moduleVersion}/listeners/${item.id}`} onClick={onLinkClick}

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

                    {filteredFunctionsList.length > 0 &&
                        <div className="ui segment">
                            <h3>Functions: {filteredFunctionsList.length}</h3>
                            <table className="ui very basic table">
                                <tbody>
                                    {filteredFunctionsList.map(item => (
                                        <tr>
                                            <td className="search-title" id={item.id} title={item.id}>
                                                <Link to={`/${item.moduleOrgName}/${item.moduleId}/${item.moduleVersion}/functions#${item.id}`} onClick={onLinkClick}
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

                    {filteredRecordsList.length > 0 &&
                        <div className="ui segment">
                            <h3>Records: {filteredRecordsList.length}</h3>
                            <table className="ui very basic table">
                                <tbody>
                                    {filteredRecordsList.map(item => (
                                        <tr>
                                            <td className="search-title" id={item.id} title={item.id}>
                                                <Link to={`/${item.moduleOrgName}/${item.moduleId}/${item.moduleVersion}/records/${item.id}`} onClick={onLinkClick}
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

                    {filteredConstantsList.length > 0 &&
                        <div className="ui segment">
                            <h3>Constants: {filteredConstantsList.length}</h3>
                            <table className="ui very basic table">
                                <tbody>
                                    {filteredConstantsList.map(item => (
                                        <tr>
                                            <td className="search-title" id={item.id} title={item.id}>
                                                <Link to={`/${item.moduleOrgName}/${item.moduleId}/${item.moduleVersion}/constants#${item.id}`} onClick={onLinkClick}
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

                    {filteredEnumsList.length > 0 &&
                        <div className="ui segment">
                            <h3>Enums: {filteredEnumsList.length}</h3>
                            <table className="ui very basic table">
                                <tbody>
                                    {filteredEnumsList.map(item => (
                                        <tr>
                                            <td className="search-title" id={item.id} title={item.id}>
                                                <Link to={`/${item.moduleOrgName}/${item.moduleId}/${item.moduleVersion}/enums/${item.id}`} onClick={onLinkClick}
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

                    {filteredTypesList.length > 0 &&
                        <div className="ui segment">
                            <h3>Types: {filteredTypesList.length}</h3>
                            <table className="ui very basic table">
                                <tbody>
                                    {filteredTypesList.map(item => (
                                        <tr>
                                            <td className="search-title" id={item.id} title={item.id}>
                                                <Link to={`/${item.moduleOrgName}/${item.moduleId}/${item.moduleVersion}/types#${item.id}`} onClick={onLinkClick}
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

                    {filteredErrorsList.length > 0 &&
                        <div className="ui segment">
                            <h3>Errors: {filteredErrorsList.length}</h3>
                            <table className="ui very basic table">
                                <tbody>
                                    {filteredErrorsList.map(item => (
                                        <tr>
                                            <td className="search-title" id={item.id} title={item.id}>
                                                <Link to={`/${item.moduleOrgName}/${item.moduleId}/${item.moduleVersion}/errors#${item.id}`} onClick={onLinkClick}
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

                    {filteredAnnotationsList.length > 0 &&
                        <div className="ui segment">
                            <h3>Annotations: {filteredAnnotationsList.length}</h3>
                            <table className="ui very basic table">
                                <tbody>
                                    {filteredAnnotationsList.map(item => (
                                        <tr>
                                            <td className="search-title" id={item.id} title={item.id}>
                                                <Link to={`/${item.moduleOrgName}/${item.moduleId}/${item.moduleVersion}/annotations#${item.id}`} onClick={onLinkClick}
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

export default SearchList;
