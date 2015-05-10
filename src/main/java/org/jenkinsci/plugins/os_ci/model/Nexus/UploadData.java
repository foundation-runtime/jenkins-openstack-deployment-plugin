package org.jenkinsci.plugins.os_ci.model.Nexus;

/**
 * Created by agrosmar on 1/7/2015.
 */

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang.builder.ToStringBuilder;

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
        "stagedRepositoryId",
        "description"
})
public class UploadData {

    @JsonProperty("stagedRepositoryId")
    private String stagedRepositoryId;
    @JsonProperty("description")
    private String description;


    /**
     *
     * @return
     * The stagedRepositoryId
     */
    @JsonProperty("stagedRepositoryId")
    public String getStagedRepositoryId() {
        return stagedRepositoryId;
    }

    /**
     *
     * @param stagedRepositoryId
     * The stagedRepositoryId
     */
    @JsonProperty("stagedRepositoryId")
    public void setStagedRepositoryId(String stagedRepositoryId) {
        this.stagedRepositoryId = stagedRepositoryId;
    }

    public UploadData withStagedRepositoryId(String stagedRepositoryId) {
        this.stagedRepositoryId = stagedRepositoryId;
        return this;
    }

    /**
     *
     * @return
     * The description
     */
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    /**
     *
     * @param description
     * The description
     */
    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    public UploadData withDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }



}
