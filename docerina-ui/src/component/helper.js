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
    } else if (type.isTypeDesc) {
        label.push(<span key="typeName"><Link className="builtin-type-link" to={`/builtin/${type.version}/typedesc`}>typedesc</Link>&lt;{getTypeLabel(type.elementType)}&gt;</span>);
    } else if (type.isRestParam) {
        label.push(<span key="typeName" className="array-type">{getTypeLabel(type.elementType)}</span>);
    } else if (type.category == "map" && type.constraint != null) {
        label.push(<span key="typeName"><Link className="builtin-type-link" to={`/builtin/${type.version}/map`}>{type.name}</Link><span>&lt;{getTypeLabel(type.constraint)}&gt;</span></span>);
    } else if (type.category == "stream") {
        label.push(<span key="typeName"><Link className="builtin-type-link" to={`/builtin/${type.version}/stream`}>{type.name}</Link>&lt;{type.memberTypes.map(type1 => getTypeLabel(type1)).reduce((prev, curr) => [prev, ', ', curr])}&gt;</span>);
    } else if (type.category == "builtin" || type.moduleName == "lang.annotations") {
        label.push(<Link key="typeName" className="builtin-type-link" to={type.name.replace(/\s/,"") == "()" ? `/builtin/${type.version}/()` : `/builtin/${type.version}/${type.name}`}>{type.name}</Link>);
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
    var link = <Link key="typeName" className="item" to={"/" + type.orgName + "/" + getPackageName(type.moduleName) + "/" + type.version + "/" + type.moduleName + "/" + type.category + getConnector(type.category) + type.name}>{type.name}</Link>
    return link;
}

export const getPackageName = (moduleName) => {
    var packageName = moduleName != null ? moduleName.split(/\./, 2)[0] : "UNK";
    return packageName == "lang" ? moduleName : packageName;
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
