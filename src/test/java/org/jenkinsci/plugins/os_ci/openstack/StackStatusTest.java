package org.jenkinsci.plugins.os_ci.openstack;

import com.ctc.wstx.util.StringUtil;
import hudson.util.Secret;
import hudson.util.VersionNumber;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jenkinsci.plugins.os_ci.NexusSettings;
import org.jenkinsci.plugins.os_ci.exceptions.OsCiPluginException;
import org.jenkinsci.plugins.os_ci.model.Openstack.FloatingIP;
import org.jenkinsci.plugins.os_ci.model.StackTemplatesData;
import org.jenkinsci.plugins.os_ci.repohandlers.NexusClient;
import org.jenkinsci.plugins.os_ci.repohandlers.OpenStackClient;
import org.junit.Test;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.yaml.snakeyaml.Yaml;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class StackStatusTest {

    public enum increase_options {
        MAJOR, MINOR, PACTH, REVISION;
    }

    private static String getCharacterDataFromElement(Element e) {
        Node child = e.getFirstChild();
        if (child instanceof CharacterData) {
            CharacterData cd = (CharacterData) child;
            return cd.getData();
        }
        return "";
    }

    @Test
    public void getStackStatusTest() {

        try {

            OpenStackClient openStackClient = new OpenStackClient(null, "jenkins","jenkins", "jenkins-tenant", "/v2.0/tokens", "10.56.165.71", 5000);
            System.out.println(openStackClient.getStackDetails("pps"));
            System.out.println(openStackClient.getStackOutputs("pps"));
            //System.out.println(openStackClient.getStackStatus("pps"));
            //System.out.println(openStackClient.stackExists("pps"));





            System.out.println("wait");

        } catch (Exception e) {
            System.out.println("Exception");
        }

        System.out.println("Done");
    }
}

