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

package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

import static io.ballerina.sequencemodelgenerator.core.model.Constants.PARTICIPANT;

/**
 * Represents the Participant which is the main component in sequence diagram model.
 * The sequence model will contain a list of Participant in which it will contain details about interactions.
 *
 * @since 2201.8.5
 */
public class Participant extends DElement {
    private final String id;
    private final String name;
    private final ParticipantKind participantKind;
    private final String packageName;
    private String type;
    private boolean hasInteractions;

    public Participant(String id, String name, ParticipantKind kind, String packageName, String type,
                       LineRange location, boolean hasInteractions) {
        super(PARTICIPANT, false, location);
        this.id = id;
        this.name = name;
        this.participantKind = kind;
        this.packageName = packageName;
        this.type = type;
        this.hasInteractions = hasInteractions;
    }

    public Participant(String id, String name, ParticipantKind kind, String packageName, LineRange location) {
        super(PARTICIPANT, false, location);
        this.id = id;
        this.name = name;
        this.participantKind = kind;
        this.packageName = packageName;
    }

    public void setHasInteractions(boolean hasInteractions) {
        this.hasInteractions = hasInteractions;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getId() {
        return id;
    }

    public ParticipantKind getParticipantKind() {
        return participantKind;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Participant{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", kind=" + participantKind +
                ", packageName='" + packageName + '\'' +
                ", type='" + type + '\'' +
                ", hasInteractions=" + hasInteractions +
                '}';
    }
}
