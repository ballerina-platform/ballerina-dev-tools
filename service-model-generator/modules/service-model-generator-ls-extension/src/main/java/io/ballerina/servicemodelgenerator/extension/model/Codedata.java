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

package io.ballerina.servicemodelgenerator.extension.model;

import io.ballerina.tools.text.LineRange;

public class Codedata {
    private LineRange lineRange;
    private boolean inListenerInit;
    private boolean isBasePath;
    private boolean inDisplayAnnotation;
    private String type;
    private String argType;
    private String originalName;
    private String orgName;
    private String moduleName;

    public Codedata() {
    }

    public Codedata(String type) {
        this.type = type;
    }

    public Codedata(String type, String argType) {
        this.type = type;
        this.argType = argType;
    }

    public Codedata(LineRange lineRange) {
        this(lineRange, false, false, false);
    }

    public Codedata(LineRange lineRange, boolean inListenerInit, boolean isBasePath, boolean inDisplayAnnotation) {
        this.lineRange = lineRange;
        this.inListenerInit = inListenerInit;
        this.isBasePath = isBasePath;
        this.inDisplayAnnotation = inDisplayAnnotation;
    }

    public LineRange getLineRange() {
        return lineRange;
    }

    public void setLineRange(LineRange lineRange) {
        this.lineRange = lineRange;
    }

    public boolean isInListenerInit() {
        return inListenerInit;
    }

    public void setInListenerInit(boolean inListenerInit) {
        this.inListenerInit = inListenerInit;
    }

    public boolean isBasePath() {
        return isBasePath;
    }

    public void setBasePath(boolean isBasePath) {
        this.isBasePath = isBasePath;
    }

    public boolean isInDisplayAnnotation() {
        return inDisplayAnnotation;
    }

    public void setInDisplayAnnotation(boolean inDisplayAnnotation) {
        this.inDisplayAnnotation = inDisplayAnnotation;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getArgType() {
        return argType;
    }

    public void setArgType(String argType) {
        this.argType = argType;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public static class Builder {
        private LineRange lineRange;
        private boolean inListenerInit = false;
        private boolean isBasePath = false;
        private boolean inDisplayAnnotation = false;
        private String type;
        private String argType;
        private String originalName;
        private String orgName;
        private String moduleName;

        public Builder() {
        }

        public Builder setLineRange(LineRange lineRange) {
            this.lineRange = lineRange;
            return this;
        }

        public Builder setInListenerInit(boolean inListenerInit) {
            this.inListenerInit = inListenerInit;
            return this;
        }

        public Builder setBasePath(boolean isBasePath) {
            this.isBasePath = isBasePath;
            return this;
        }

        public Builder setInDisplayAnnotation(boolean inDisplayAnnotation) {
            this.inDisplayAnnotation = inDisplayAnnotation;
            return this;
        }

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public Builder setArgType(String argType) {
            this.argType = argType;
            return this;
        }

        public Builder setOriginalName(String originalName) {
            this.originalName = originalName;
            return this;
        }

        public Builder setOrgName(String orgName) {
            this.orgName = orgName;
            return this;
        }

        public Builder setModuleName(String moduleName) {
            this.moduleName = moduleName;
            return this;
        }

        public Codedata build() {
            Codedata codedata = new Codedata();
            codedata.setLineRange(lineRange);
            codedata.setInListenerInit(inListenerInit);
            codedata.setBasePath(isBasePath);
            codedata.setInDisplayAnnotation(inDisplayAnnotation);
            codedata.setType(type);
            codedata.setArgType(argType);
            codedata.setOriginalName(originalName);
            codedata.setOrgName(orgName);
            codedata.setModuleName(moduleName);
            return codedata;
        }
    }
}
