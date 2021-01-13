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
        label.push(<span key="typeName"><code> <span>function(</span>{type.paramTypes.map(type1 => getTypeLabel(type1)).reduce((prev, curr) => [prev, ', ', curr])}<span>) </span><span>returns (</span>{getTypeLabel(type.returnType)} <span>)</span></code></span>);
    } else if (type.isLambda && type.returnType == null) {
        label.push(<span key="typeName"><code> <span>function(</span>{type.paramTypes.map(type1 => getTypeLabel(type1)).reduce((prev, curr) => [prev, ', ', curr])}<span>) </span><span>() </span></code></span>);
    } else if (type.isArrayType) {
        label.push(<span key="typeName" className="array-type">{getTypeLabel(type.elementType)}{getSuffixes(type)}</span>);
    } else if (type.isParenthesisedType) {
        label.push(<span key="typeName">({getTypeLabel(type.elementType)})</span>);
    } else if (type.isTypeDesc) {
        label.push(<span key="typeName">typedesc&lt;{getTypeLabel(type.elementType)}&gt;</span>);
    } else if (type.isRestParam) {
        label.push(<span key="typeName" className="array-type">{getTypeLabel(type.elementType)}{getSuffixes(type)}</span>);
    } else if (type.category == "map" && type.constraint != null) {
        label.push(<span key="typeName"><span className="builtin-type">{type.name}</span><span>&lt;{getTypeLabel(type.constraint)}&gt;</span></span>);
    } else if (type.category == "stream") {
        label.push(<span key="typeName" className="builtin-type">{type.name}&lt;{type.memberTypes.map(type1 => getTypeLabel(type1)).reduce((prev, curr) => [prev, ', ', curr])}&gt;</span>);
    } else if (type.category == "builtin" || type.moduleName == "lang.annotations" || !type.generateUserDefinedTypeLink || type.category == "UNKNOWN") {
        label.push(<span key="typeName" className="builtin-type">{type.name + getSuffixes(type)}</span>);
    } else {
        label.push(getLink(type));
    }
    if (defaultValue != null && defaultValue != "") {
        if ((type.category == "abstractobjects" || type.category == "classes") && defaultValue != "()") {
            label.push(<span key="defVal"><span className="default">(default</span> <span className="type">{getLink(type)}</span><span className="default">)</span></span>);
        } else {
            label.push(<span className="default">(default {defaultValue})</span>);
        }
    }

    return label;
}

export const getFirstLine = (lines) => {
    var newLine = lines.replace(/<pre>(.|\n)*?<\/pre>/g, " ");
    newLine = newLine.replace(/<table>(.|\n)*?<\/table>/g, " ");

    var splits = newLine.split(/\.\s/, 2);
    if (splits.length < 2) {
        return splits[0];
    } else {
        return splits[0] + ".";
    }
}

export const getLink = (type) => {
    var link = <Link key="typeName" className="item" to={"/" + type.orgName + "/" + getPackageName(type.moduleName) + "/" + type.version + "/" + type.moduleName + "/" + type.category + getConnector(type.category) + type.name} replace>{type.name + getSuffixes(type)}</Link>
    return link;
}

export const getPackageName = (moduleName) => {
    var packageName = moduleName != null ? moduleName.split(/\./, 2)[0] : "UNK";
    return packageName == "lang" ? moduleName : packageName;
}

export const getSuffixes = (type) => {
    var suffix = "";
    if (type.isArrayType) {
        suffix = "[ ]".repeat(type.arrayDimensions);
    } else if (type.isRestParam) {
        suffix = "...";
    }
    suffix += type.isNullable ? "?" : "";
    return suffix;
}

export const getConnector = (listType) => {
    if (listType == "records" || listType == "classes" || listType == "clients" || listType == "abstractObjects" || listType == "listeners" || listType == "enums") {
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
    elem.scrollIntoView();
    elem.classList.add('highlight');
    setTimeout(function () { elem.classList.remove('highlight'); }, 2000);
}
