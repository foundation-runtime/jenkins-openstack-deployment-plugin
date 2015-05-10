package org.jenkinsci.plugins.os_ci.openstack;

import com.google.common.base.Joiner;
import org.jenkinsci.plugins.os_ci.exceptions.OsCiPluginException;
import org.jenkinsci.plugins.os_ci.model.StackTemplatesData;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
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
public class TemplateUtils {
    public static StackTemplatesData readStackTemplate(String templateName, String path) {
        try {
            StackTemplatesData std = new StackTemplatesData();
            // Read main file
            std.setName(templateName);
            std.setContent(readFile(new File(Joiner.on(File.separator).join(path, templateName + ".yaml"))));
            std.setEnvContent(readFile(new File(Joiner.on(File.separator).join(path, templateName + ".env.yaml"))));

            // loop on files in directory and read all files there
            Map<String, String> dependencies = new HashMap<String, String>();
            File depDir = new File(Joiner.on(File.separator).join(path, templateName + ".yaml.dependencies"));
            if (depDir.isDirectory()) {
                File[] files = depDir.listFiles();
                for (File file : files) {
                    // Ignore nested dirs
                    if (!file.isDirectory()){
                        dependencies.put(file.getName(), readFile(file));
                    }
                }
                std.setDependencies(dependencies);
            }
            return std;
        } catch (IOException e) {
            throw new OsCiPluginException("Couldn't read template file for stack " + templateName);
        }
    }

    private static String readFile(File file) throws IOException {
        RandomAccessFile f = new RandomAccessFile(file, "r");
        try {

            long longlength = f.length();
            int length = (int) longlength;
            if (length != longlength) {
                throw new IOException("File size >= 2 GB");
            }
            byte[] data = new byte[length];
            f.readFully(data);
            return new String(data);
        } finally {
            f.close();
        }
    }
}
