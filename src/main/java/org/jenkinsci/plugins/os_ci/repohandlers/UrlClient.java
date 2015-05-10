package org.jenkinsci.plugins.os_ci.repohandlers;

import com.google.common.base.Joiner;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import org.jenkinsci.plugins.os_ci.exceptions.OsCiPluginException;
import org.jenkinsci.plugins.os_ci.model.UrlParameters;
import org.jenkinsci.plugins.os_ci.utils.CompressUtils;
import org.jenkinsci.plugins.os_ci.utils.LogUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

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
 */public class UrlClient {

    UrlParameters externalDependency;
    AbstractBuild build;
    BuildListener listener;

    public UrlClient(UrlParameters externalDependency, AbstractBuild build, BuildListener listener) {
        this.externalDependency = externalDependency;
        this.build = build;
        this.listener = listener;
    }

    public String downloadRPM() {
        LogUtils.log(listener, "downloading from URL : " + externalDependency);
        return downloadArtifact("rpm");
    }

    private String downloadArtifact(String packageType) {
        FileOutputStream fos = null;
        InputStream is = null;
        File file = null;
        URL fileUrl = null;
        String version = "";
        try {
            fileUrl = new URL(externalDependency.getUrl());
            URLConnection con = fileUrl.openConnection();
            is = con.getInputStream();
            String filename = externalDependency.getDestinationFileName().isEmpty() ? con.getURL().getFile() : externalDependency.getDestinationFileName();
            File destFile = new File(filename);
            String path = "";
            filename = destFile.getName();
            if (packageType.equals("rpm")) {
                path = Joiner.on(File.separator).join(build.getWorkspace().getRemote(), "archive", "repo", filename);
                file = new File(path);
            } else if (packageType.equals("tar.gz")) {
                path = Joiner.on(File.separator).join(build.getWorkspace().getRemote(), "archive", "prodpuppet", "modules", filename);
                file = new File(path);
            }

            file.getParentFile().mkdirs();
            fos = new FileOutputStream(file);
            LogUtils.log(listener, "File location : " + filename);


            byte[] buffer = new byte[4096 * 10];              //declare 4KB buffer
            int len;

            //while we have available data, continue downloading and storing to local file
            while ((len = is.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            fos = null;

            if (filename.endsWith("tar.gz")) {
                CompressUtils.untarFile(file);
            }


            String pattern =  	"^[^-]+-([\\d\\-\\.]+\\d).*rpm$";
            version = destFile.getName().replaceAll(pattern, "$1");
            if (version.isEmpty()) {
                LogUtils.log(listener, "Version not found from URL for external dependency: " + externalDependency.getUrl());
            }

        } catch (MalformedURLException e) {
            throw new OsCiPluginException(e.toString());
        } catch (FileNotFoundException e) {
            throw new OsCiPluginException(e.toString());
        } catch (IOException e) {
            throw new OsCiPluginException(e.toString());
            //LogUtils.log(listener, "Got an error! : " + e.getMessage());
        } finally {
            try {

                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        LogUtils.log(listener, "Got an error!");
                    }
                }
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        LogUtils.log(listener, "Got an error!");
                    }
                }
            }
        }
        return version;
    }
}
