package io.ballerina.flowmodelgenerator.core.db.model;

import io.ballerina.flowmodelgenerator.core.model.Item;

import java.util.List;

public record TreeNode(String display, String segmentPath, List<Item> children, int id) implements Item {
}
