import { Link } from '../Router'
import * as React from "react";



export const getTypeLabel = (type, defaultValue)=>{
    var label = [];
    if (type.isAnonymousUnionType) {
        label.push(type.memberTypes.map(type1 => getTypeLabel(type1)).reduce((prev, curr) => [prev, ' | ', curr]));
    } else if (type.isTuple) {
        label.push(<span key="typeName"><span>[</span>{type.memberTypes.map(type1 => getTypeLabel(type1)).reduce((prev, curr) => [prev, ', ', curr])}<span>]</span></span>);
    } else if (type.isLambda && type.returnType != null) {
        label.push(<span key="typeName"><code> <span>function(</span>{type.paramTypes.map(type1 => getTypeLabel(type1)).reduce((prev, curr) => [prev, ', ', curr])}<span>) </span><span>returns (</span>{getTypeLabel(type.returnType)} <span>)</span></code></span>);
    } else if (type.isLambda && type.returnType == null) {
        label.push(<span key="typeName"><code> <span>function(</span>{type.paramTypes.map(type1 => getTypeLabel(type1)).reduce((prev, curr) => [prev, ', ', curr])}<span>) </span><span>() </span></code></span>);
    } else if (type.isArrayType) {
        label.push(<span key="typeName" className="array-type">{getTypeLabel(type.elementType)}{getSuffixes(type)}</span>);
    } else if (type.isRestParam) {
        label.push(<span key="typeName" className="array-type">{getTypeLabel(type.elementType)}{getSuffixes(type)}</span>);
    } else if (type.category == "map" && type.constraint != null) {
        label.push(<span key="typeName"><span className="builtin-type">{type.name}</span><span>&lt;{getTypeLabel(type.constraint)}&gt;</span></span>);
    } else if (type.category == "stream") {
        label.push(<span key="typeName" className="builtin-type">{type.name}&lt;{type.memberTypes.map(type1 => getTypeLabel(type1)).reduce((prev, curr) => [prev, ', ', curr])}&gt;</span>);
    } else if (type.category == "builtin" || type.moduleName == "lang.annotations" || !type.generateUserDefinedTypeLink || type.category == "UNKNOWN") {
        label.push(<span key="typeName" className="builtin-type">{type.name+getSuffixes(type)}</span>);
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

export const getFirstLine = (lines)=>{
    var newLine = lines.replace(/<pre>(.|\n)*?<\/pre>/g, " ");
    newLine = newLine.replace(/<table>(.|\n)*?<\/table>/g, " ");

    var splits = newLine.split(/\.\s/, 2);
    if (splits.length < 2) {
        return {__html: splits[0]+"</p>"};
    } else {
        return {__html: splits[0]+".</p>"};
    }
}

export const getLink = (type)=>{
    var link = <Link key="typeName" className="item" to={"/" + type.moduleName + "/" + type.category + getConnector(type.category) + type.name} replace>{type.name}</Link>
    return link;
}

export const getSuffixes=(type)=>{
    var suffix = "";
    if (type.isArrayType) {
        suffix = "[ ]".repeat(type.arrayDimensions);
    } else if (type.isRestParam) {
        suffix = "...";
    }
    suffix += type.isNullable ? "?" : "";
    return suffix;
}


export const getConnector=(listType)=>{
    if (listType == "records" || listType == "classes" || listType == "clients" || listType == "abstractObjects" || listType == "listeners" ) {
        return "/";
    } else {
        return "#";
    }
}

export const removeHtmlTags=(str)=> {
    return str!=null?str.replace(/<\/?[^>]*>/g, ""):"";
}