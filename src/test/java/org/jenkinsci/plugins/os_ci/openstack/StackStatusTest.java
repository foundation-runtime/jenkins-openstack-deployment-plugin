package org.jenkinsci.plugins.os_ci.openstack;

import org.junit.Test;
import org.w3c.dom.*;

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
 */public class StackStatusTest {

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

            System.out.println("wait");

        } catch (Exception e) {
            System.out.println("Exception");
        }

        System.out.println("Done");
    }
}

