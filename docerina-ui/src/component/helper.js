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
import { Link, appType } from '../Router'

export const getLangLib = (name) => {
    const langLibs = ["array", "boolean", "decimal", "error", "float", "future", "int", "map", "object", "stream", "string", "value","xml"];
    if (langLibs.includes(name) && appType != "react") {
        return <Link className="builtin-type-link" to={`/ballerina/lang.${name}/latest`}>{name}</Link>
    } else {
        return <span key="typeName" className="builtin-type-other">{name}</span>
    }
}

export const getTypeLabel = (type, defaultValue) => {
    var label = [];
    if (type.isAnonymousUnionType) {
        label.push(type.memberTypes.map(type1 => getTypeLabel(type1)).reduce((prev, curr) => [prev, ' | ', curr]));
    } else if (type.isIntersectionType) {
        label.push(type.memberTypes.map(type1 => getTypeLabel(type1)).reduce((prev, curr) => [prev, ' & ', curr]));
    } else if (type.isTuple) {
        label.push(<span key="typeName"><span>[</span>{type.memberTypes.map(type1 => getTypeLabel(type1)).reduce((prev, curr) => [prev, ', ', curr])}<span>]</span></span>);
    } else if (type.isLambda && type.returnType != null) {
        label.push(<span key="typeName"><code> <span>function(</span>{type.paramTypes.length > 0 && type.paramTypes.map(type1 => getTypeLabel(type1)).reduce((prev, curr) => [prev, ', ', curr])}<span>)</span><span> returns (</span>{getTypeLabel(type.returnType)}<span>)</span></code></span>);
    } else if (type.isLambda && type.returnType == null) {
        label.push(<span key="typeName"><code> <span>function(</span>{type.paramTypes.length > 0 && type.paramTypes.map(type1 => getTypeLabel(type1)).reduce((prev, curr) => [prev, ', ', curr])}<span>) </span><span>() </span></code></span>);
    } else if (type.isArrayType) {
        label.push(<span key="typeName" className="array-type">{getTypeLabel(type.elementType)}</span>);
    } else if (type.isParenthesisedType) {
        label.push(<span key="typeName">({getTypeLabel(type.elementType)})</span>);
    } else if (type.isInclusion) {
        label.push(<span key="typeName">*{getLink(type)}</span>);
    } else if (type.isTypeDesc) {
        label.push(<span key="typeName">typedesc{type.elementType != null && <span>&lt;{getTypeLabel(type.elementType)}&gt;</span>}</span>);
    } else if (type.category == "future") {
        label.push(<span key="typeName">future{type.elementType != null && <span>&lt;{getTypeLabel(type.elementType)}&gt;</span>}</span>);
    } else if (type.category == "inline_closed_record") {
        label.push(<span key="typeName">record &#123;|{type.memberTypes.length > 0 && <span> {type.memberTypes.map(type1 => <span>{type1.name} {getTypeLabel(type1.elementType)}</span>).reduce((prev, curr) => [prev, ', ', curr])} </span>}|&#125;</span>);
    } else if (type.category == "inline_record") {
        label.push(<span key="typeName">record &#123;{type.memberTypes.length > 0 && <span> {type.memberTypes.map(type1 => <span>{type1.name} {getTypeLabel(type1.elementType)}</span>).reduce((prev, curr) => [prev, ', ', curr])} </span>}&#125;</span>);
    } else if (type.isRestParam) {
        label.push(<span key="typeName" className="array-type">{getTypeLabel(type.elementType)}</span>);
    } else if (type.category == "map" && type.constraint != null) {
        label.push(<span key="typeName">{getLangLib(type.name)}<span>&lt;{getTypeLabel(type.constraint)}&gt;</span></span>);
    } else if (type.category == "stream") {
        label.push(<span key="typeName">{getLangLib(type.name)}&lt;{type.memberTypes.map(type1 => getTypeLabel(type1)).reduce((prev, curr) => [prev, ', ', curr])}&gt;</span>);
    } else if (type.category == "builtin" || type.moduleName == "lang.annotations") {
        label.push(getLangLib(type.name));
    } else if (!type.generateUserDefinedTypeLink || type.category == "UNKNOWN") {
        label.push(<span key="typeName" className="builtin-type-other">{type.name}</span>);
    } else {
        label.push(getLink(type));
    }
    // Get suffixes
    label.push(getSuffixes(type));
    if (defaultValue != null && defaultValue != "") {
        if ((type.category == "objectTypes" || type.category == "classes") && defaultValue != "()") {
            label.push(<span key="defVal"><span className="default">(default</span> <span className="type">{getLink(type)}</span><span className="default">)</span></span>);
        } else {
            label.push(<span className="default">(default {defaultValue})</span>);
        }
    }

    return label;
}

export const getFirstLine = (lines) => {
    if (lines != null) {
        var newLine = lines.replace(/<pre>(.|\n)*?<\/pre>/g, " ");
        newLine = newLine.replace(/<table>(.|\n)*?<\/table>/g, " ");

        var splits = newLine.split(/\.\s/, 2);
        if (splits.length < 2) {
            return splits[0];
        } else {
            return splits[0] + ".";
        }
    } else {
        return "";
    }
}

export const getLink = (type) => {
    var link = <Link key="typeName" className="item" to={`/${type.orgName}/${type.moduleName}/${type.version}/${type.category + getConnector(type.category) + type.name}`}>{type.name}</Link>
    return link;
}


export const getSuffixes = (type) => {
    var suffix = [];
    if (type.isArrayType) {
        suffix.push(<span title="array">{"[ ]".repeat(type.arrayDimensions)}</span>);
    } else if (type.isRestParam) {
        suffix.push(<span title="rest parameter">...</span>);
    }
    if (type.isNullable) {
        suffix.push(<span title="optional">?</span>);
    }
    return suffix;
}

export const getConnector = (listType) => {
    if (listType == "records" || listType == "classes" || listType == "clients" || listType == "objectTypes" || listType == "listeners" || listType == "enums") {
        return "/";
    } else {
        return "#";
    }
}

export const removeHtmlTags = (str) => {
    return str != null ? str.replace(/<\/?[^>]*>/g, "") : "";
}

export const scrollAndHighlight = (elemId) => {
    const elem = document.getElementById(elemId.split("#")[1]);
    if (elem != null) {
        elem.scrollIntoView();
        elem.classList.add('highlight');
        setTimeout(function () { elem.classList.remove('highlight'); }, 2000);
    }
}
