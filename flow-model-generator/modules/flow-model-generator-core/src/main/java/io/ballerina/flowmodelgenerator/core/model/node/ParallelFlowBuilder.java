/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 *  OF ANY KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.flowmodelgenerator.core.model.node;

import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.model.Branch;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents the properties of parallel flow node in the flow model.
 *
 * @since 2.0.0
 */
public class ParallelFlowBuilder extends NodeBuilder {

    public static final String LABEL = "Parallel Flow";
    public static final String DESCRIPTION = "Create parallel flows";

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata().node(NodeKind.PARALLEL_FLOW);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        generateWorkers(sourceBuilder);
        return sourceBuilder.textEdit(false).build();
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        Set<String> names = context.getAllVisibleSymbolNames();
        Branch firstBranch = Branch.getDefaultWorkerBranch(names);
        names.add(firstBranch.label());
        this.branches = List.of(firstBranch, Branch.getDefaultWorkerBranch(names));
        codedata().isNew();
    }

    protected List<String> generateWorkers(SourceBuilder sourceBuilder) {
        List<Branch> branches = sourceBuilder.flowNode.branches();
        List<String> workerNames = new ArrayList<>();
        for (Branch branch : branches) {
            // Write the worker name
            Optional<Property> variableProperty = branch.getProperty(Property.VARIABLE_KEY);
            if (variableProperty.isEmpty()) {
                continue;
            }
            sourceBuilder.token()
                    .keyword(SyntaxKind.WORKER_KEYWORD)
                    .name(variableProperty.get())
                    .whiteSpace();
            workerNames.add(variableProperty.get().value().toString());

            // Write the return type
            Optional<Property> typeProperty = branch.getProperty(Property.TYPE_KEY);
            if (typeProperty.isPresent() && !typeProperty.get().value().toString().isEmpty()) {
                sourceBuilder.token()
                        .keyword(SyntaxKind.RETURNS_KEYWORD)
                        .name(typeProperty.get());
            }

            // Write the body
            sourceBuilder.body(branch.children());
        }
        return workerNames;
    }
}
