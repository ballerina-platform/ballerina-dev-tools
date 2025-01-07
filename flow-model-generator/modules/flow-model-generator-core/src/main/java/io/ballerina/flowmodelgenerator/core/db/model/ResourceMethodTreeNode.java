package io.ballerina.flowmodelgenerator.core.db.model;

public record ResourceMethodTreeNode(
        int treeNodeId,
        int parentId,
        int isLeaf,
        String path,
        int functionId,
        String description,
        String resourcePath
        ) {
}

