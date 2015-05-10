package org.jenkinsci.plugins.os_ci.model;

import com.fasterxml.jackson.annotation.*;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Copyright 2015 Cisco Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "disable_rollback",
        "stack_name",
        "template",
        "parameters",
        "files"
})
public class CreateStack {


    @JsonProperty("disable_rollback")
    private Boolean disableRollback =true;
    @JsonProperty("stack_name")
    private String stackName;
    @JsonProperty("template")
    private String template;
    @JsonProperty("parameters")
    private Map<String, Object> parameters = new HashMap<String, Object>();
    @JsonProperty("files")
    private Map<String, String> files = new HashMap<String, String>();

    /**
     *
     * @return
     * The disableRollback
     */
    @JsonProperty("disable_rollback")
    public Boolean getDisableRollback() {
        return disableRollback;
    }

    /**
     *
     * @param disableRollback
     * The disable_rollback
     */
    @JsonProperty("disable_rollback")
    public void setDisableRollback(Boolean disableRollback) {
        this.disableRollback = disableRollback;
    }


    /**
     *
     * @return
     * The stackName
     */
    @JsonProperty("stack_name")
    public String getStackName() {
        return stackName;
    }

    /**
     *
     * @param stackName
     * The stack_name
     */
    @JsonProperty("stack_name")
    public void setStackName(String stackName) {
        this.stackName = stackName;
    }


    /**
     *
     * @return
     * The template
     */
    @JsonProperty("template")
    public String getTemplate() {
        return template;
    }

    /**
     *
     * @param template
     * The template
     */
    @JsonProperty("template")
    public void setTemplate(String template) {
        this.template = template;
    }


    /**
     *
     * @return
     * The parameters
     */
    @JsonProperty("parameters")
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     *
     * @param parameters
     * The parameters
     */
    @JsonProperty("parameters")
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }


    /**
     *
     * @return
     * The files
     */
    @JsonProperty("files")
    public Map<String, String> getFiles() {
        return files;
    }

    /**
     *
//     * @param files
     * The files
     */
    @JsonProperty("files")
    public void setFiles(Map<String, String> files) {
        this.files = files;
    }


    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }


}