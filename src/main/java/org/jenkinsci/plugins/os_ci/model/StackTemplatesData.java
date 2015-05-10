package org.jenkinsci.plugins.os_ci.model;

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
public class StackTemplatesData {
    private String name;
    private String content;
    private String envContent;
    private Map<String, String> dependencies = new HashMap<String, String>();

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    public Map<String, String> getDependencies() {
        return dependencies;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setDependencies(Map<String, String> dependencies) {
        this.dependencies = dependencies;
    }

    public String getEnvContent() {
        return envContent;
    }

    public void setEnvContent(String envContent) {
        this.envContent = envContent;
    }

    @Override
    public String toString() {
        return "StackTemplatesData{" +
                "name='" + name + '\'' +
                ", content='" + content + '\'' +
                ", envContent='" + envContent + '\'' +
                ", dependencies=" + dependencies +
                '}';
    }
}
