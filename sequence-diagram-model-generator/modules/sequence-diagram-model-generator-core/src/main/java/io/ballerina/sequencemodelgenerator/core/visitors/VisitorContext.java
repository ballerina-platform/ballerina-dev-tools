/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

package io.ballerina.sequencemodelgenerator.core.visitors;

import io.ballerina.sequencemodelgenerator.core.model.DElement;
import io.ballerina.sequencemodelgenerator.core.model.Participant;

import java.util.ArrayList;
import java.util.List;

/**
 * Context that is maintained throughout the model generation process.
 *
 * @since 2201.8.0
 */
public class VisitorContext {
    private Participant rootParticipant;
    private Participant currentParticipant;
    private final List<Participant> participants;
    // Represents constructs such as conditional statements with a block of statements.
    private DElement diagramElementWithChildren;
    private final List<String> visitedFunctionNames;

    public VisitorContext() {
        this.rootParticipant = null;
        this.currentParticipant = null;
        this.participants = new ArrayList<>();
        this.visitedFunctionNames = new ArrayList<>();
    }

    public VisitorContext(Participant rootParticipant, Participant participant, List<Participant> participants,
                          List<String> visitedFunctionNames) {
        this.rootParticipant = rootParticipant;
        this.currentParticipant = participant;
        this.participants = participants;
        this.visitedFunctionNames = visitedFunctionNames;
    }

    public VisitorContext(Participant rootParticipant, Participant currentParticipant, List<Participant> participants
            , DElement diagramElementWithChildren, List<String> visitedFunctionNames) {
        this.rootParticipant = rootParticipant;
        this.currentParticipant = currentParticipant;
        this.participants = participants;
        this.diagramElementWithChildren = diagramElementWithChildren;
        this.visitedFunctionNames = visitedFunctionNames;
    }

    public DElement getDiagramElementWithChildren() {
        return diagramElementWithChildren;
    }

    public Participant getCurrentParticipant() {
        return currentParticipant;
    }

    public Participant getRootParticipant() {
        return rootParticipant;
    }

    public void setCurrentParticipant(Participant currentParticipant) {
        this.currentParticipant = currentParticipant;
    }

    public void setRootParticipant(Participant rootParticipant) {
        this.rootParticipant = rootParticipant;
    }

    public List<Participant> getParticipants() {
        return participants;
    }

    public void addToParticipants(Participant participant) {
        this.participants.add(participant);
    }

    public List<String> getVisitedFunctionNames() {
        return visitedFunctionNames;
    }

    public void addToVisitedFunctionNames(String nameReferenceNode) {
        this.visitedFunctionNames.add(nameReferenceNode);
    }
}
