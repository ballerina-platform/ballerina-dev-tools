/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.indexgenerator;

import io.ballerina.compiler.api.symbols.PathParameterSymbol;
import io.ballerina.compiler.api.symbols.ResourceMethodSymbol;
import io.ballerina.compiler.api.symbols.resourcepath.PathSegmentList;
import io.ballerina.compiler.api.symbols.resourcepath.ResourcePath;
import io.ballerina.compiler.api.symbols.resourcepath.util.NamedPathSegment;
import io.ballerina.compiler.api.symbols.resourcepath.util.PathSegment;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a tree structure of the resource methods.
 *
 * @since 2.1.0
 */
public class ResourceMethodTree {

    private int idx;
    private NonTerminalNode root = null;

    public ResourceMethodTree() {
        this.idx = 0;
    }

    public void addResourceToTree(ResourceMethodSymbol resourceMethodSymbol, int functionId) {
        if (root == null) {
            root = new NonTerminalNode(idx, null, "/");
        }
        String accessor = resourceMethodSymbol.getName().orElse("");
        ResourcePath resourcePath = resourceMethodSymbol.resourcePath();
        NonTerminalNode parent = root;
        switch (resourcePath.kind()) {
            case PATH_SEGMENT_LIST -> {
                PathSegmentList pathSegmentList = (PathSegmentList) resourcePath;
                for (PathSegment pathSegment : pathSegmentList.list()) {
                    String path = pathSegment.signature();
                    NonTerminalNode grandParent = parent;
                    parent = locateNode(parent, path);
                    if (parent != null) {
                        continue;
                    }
                    parent = grandParent;
                    if (pathSegment instanceof NamedPathSegment namedPathSegment) {
                        NonTerminalNode nonTerminalNode = new NonTerminalNode(++idx, parent,
                                "/" + namedPathSegment.signature());
                        parent.addChild(nonTerminalNode);
                        parent = nonTerminalNode;
                        continue;
                    }
                    if (pathSegment instanceof PathParameterSymbol pathParameterSymbol) {
                        NonTerminalNode nonTerminalNode = new NonTerminalNode(++idx, parent,
                                "/[" + pathParameterSymbol.getName().orElse("^") + "]");
                        parent.addChild(nonTerminalNode);
                        parent = nonTerminalNode;
                    }
                }
                TerminalNode terminalNode = new TerminalNode(++idx, parent, accessor, functionId);
                parent.addChild(terminalNode);
            }
//                case PATH_REST_PARAM -> pathBuilder.append("/path/to/subdirectory");
            case DOT_RESOURCE_PATH -> {
                TerminalNode terminalNode = new TerminalNode(++idx, parent, accessor, functionId);
                root.addChild(terminalNode);
            }
        }
    }

    public NonTerminalNode getRoot() {
        return root;
    }

    public void insertTreeToDatabase(NonTerminalNode root) {
        // insert root node to database
        // get the id of the root node
        root.id = 1;

        // insert all the children of the root node to the database
        for (Node child : root.children) {
            if (child instanceof NonTerminalNode nonTerminalNode) {
                insertTreeToDatabase(nonTerminalNode);
            } else if (child instanceof TerminalNode terminalNode) {
                // insert the terminal node to the database
                // insert the terminal node to function id relationship to the database
            }
        }

    }

    private static NonTerminalNode locateNode(NonTerminalNode nonTerminalNode, String path) {
        if (nonTerminalNode == null) {
            return null;
        }
        if (nonTerminalNode.path.equals(path)) {
            return nonTerminalNode;
        }
        for (Node child : nonTerminalNode.children) {
            if (child instanceof NonTerminalNode childNode) {
                if (childNode.path.equals(path)) {
                    return childNode;
                }
            }
        }
        return null;
    }

    /**
     * Represents a node in the resource tree.
     */
    public abstract static class Node {
        int id;
        final Node parent;

        Node(int id, Node parent) {
            this.id = id;
            this.parent = parent;
        }
    }

    /**
     * Represents a non-terminal node in the resource tree.
     */
    public static class NonTerminalNode extends Node {
        final List<Node> children = new ArrayList<>();
        final String path;

        NonTerminalNode(int id, Node parent, String path) {
            super(id, parent);
            this.path = path;
        }

        void addChild(Node child) {
            children.add(child);
        }
    }

    /**
     * Represents a terminal node in the resource tree.
     */
    public static class TerminalNode extends Node {
        final String accessor;
        final int functionId;

        TerminalNode(int id, Node parent, String accessor, int functionId) {
            super(id, parent);
            this.accessor = accessor;
            this.functionId = functionId;
        }
    }
}
