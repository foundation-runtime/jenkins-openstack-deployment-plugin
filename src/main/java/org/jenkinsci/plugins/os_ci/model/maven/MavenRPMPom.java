package org.jenkinsci.plugins.os_ci.model.maven;

import com.google.common.base.Joiner;
import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jenkinsci.plugins.os_ci.NexusSettings;
import org.jenkinsci.plugins.os_ci.exceptions.OsCiPluginException;
import org.jenkinsci.plugins.os_ci.model.ArtifactParameters;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

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
 */public class MavenRPMPom {
    private ArtifactParameters artifact;
    private List<ArtifactParameters> productsDependencies;
    Model model = new Model();
    private static String workSpace = "";

    private MavenRPMPom(MavenPomBuilder pomBuilder, String workSpace) {
        artifact = pomBuilder.artifact;
        productsDependencies = pomBuilder.productsDependencies;
        this.workSpace = workSpace;
    }

    public MavenRPMPom(File pomFile) {
        try {
            FileInputStream fis = new FileInputStream(pomFile);
            model = new MavenXpp3Reader().read(fis);
        } catch (IOException e) {
            // Swallow - no module dependencies
        } catch (XmlPullParserException e) {
            throw new OsCiPluginException(e.getMessage());
        }

    }

    public ArtifactParameters getArtifact() {
        return artifact;
    }


    public List<ArtifactParameters> getProductsDependencies() {
        return productsDependencies;
    }


    public void save(File file) throws FileNotFoundException, IOException {
        model.setGroupId(artifact.getGroupId());
        model.setModelVersion("4.0.0");
        model.setArtifactId(artifact.getArtifactId());
        model.setVersion(artifact.getVersion());
        model.setPackaging("pom");
        String nexusUrlPrefix = NexusSettings.getNexusSettingsDescriptor().getNexusDocUrl();
        model.setUrl(nexusUrlPrefix + "/maven_sites/${project.groupId}/${project.artifactId}/${project.version}");
        Organization org = new Organization();
        org.setName("Cisco");
        org.setUrl("http://www.cisco.com");
        model.setOrganization(org);
        Properties properties = new Properties();
        properties.setProperty("project.build.sourceEncoding", "UTF-8");
        model.setProperties(properties);
        Build mvnRpmBuild = new Build();
        Plugin mvnRpmPlugin = new Plugin();
        mvnRpmPlugin.setArtifactId("rpm-maven-plugin");
        mvnRpmPlugin.setVersion("2.1.2");
        mvnRpmPlugin.setGroupId("org.codehaus.mojo");

        PluginExecution pluginExecution = new PluginExecution();
        List<String> goals = new ArrayList<String>();
        goals.add("attached-rpm");
        pluginExecution.setGoals(goals);
        pluginExecution.setId("makerpm");
        List<PluginExecution> plugexList = new ArrayList<PluginExecution>();
        plugexList.add(pluginExecution);
        mvnRpmPlugin.setExecutions(plugexList);
        mvnRpmBuild.addPlugin(mvnRpmPlugin);
        Site site = new Site();
        site.setId("NDS Site");
        String nexusScpUrlPrefix = NexusSettings.getNexusSettingsDescriptor().getNexusDocUrl().replace("http://", "scp://");
        site.setUrl(nexusScpUrlPrefix + "/cifs/maven_sites/${project.groupId}/${project.artifactId}/${project.version}");
        Xpp3Dom configuration = createPluginConfiguration();
        mvnRpmPlugin.setConfiguration(configuration);
        model.setBuild(mvnRpmBuild);

        FileOutputStream fos = new FileOutputStream(file);
        new MavenXpp3Writer().write(fos, model);

    }

    Xpp3Dom createPluginConfiguration() throws IOException {
        final Xpp3Dom configuration = new Xpp3Dom("configuration");
        final Xpp3Dom name = new Xpp3Dom("name");
        name.setValue( "nds_"+artifact.getArtifactId()+"_deployment-scripts");

        configuration.addChild(name);
        final Xpp3Dom autoRequires = new Xpp3Dom("autoRequires");
        autoRequires.setValue("true");
        configuration.addChild(autoRequires);
        final Xpp3Dom packager = new Xpp3Dom("packager");
        packager.setValue("CISCO Limited");
        configuration.addChild(packager);
        final Xpp3Dom group = new Xpp3Dom("group");
        group.setValue("Application/CISCO");
        configuration.addChild(group);
        //****************** mappings ******************
        final Xpp3Dom mappings = new Xpp3Dom("mappings");
        //****************** mappings ******************
        final Xpp3Dom mapping = new Xpp3Dom("mapping");
        Xpp3Dom username = new Xpp3Dom("username");
        username.setValue("${rpmUsername}");
        Xpp3Dom groupname = new Xpp3Dom("groupname");
        groupname.setValue("${rpmGroupname}");
        final Xpp3Dom source = new Xpp3Dom("source");
        Xpp3Dom filemode = new Xpp3Dom("filemode");
        filemode.setValue("0755");

        Xpp3Dom location = new Xpp3Dom("location");
        //****************** mappingList ******************
        Xpp3Dom sources = new Xpp3Dom("sources");
        Xpp3Dom directory = new Xpp3Dom("directory");
        directory.setValue("/etc/puppet/heatpuppet");
        mapping.addChild(directory);
        mapping.addChild(username);
        mapping.addChild(filemode);
        mapping.addChild(groupname);
        File dir = new File(Joiner.on(File.separator).join(workSpace, "puppets"));
        location.setValue(dir.getPath());
        source.addChild(location);
        Xpp3Dom excludes = new Xpp3Dom("excludes");
        Xpp3Dom exclude = new Xpp3Dom("exclude");
        exclude.setValue("modules/external_dependencies.tar.gz");
        excludes.addChild(exclude);
        source.addChild(excludes);
        List<Xpp3Dom> includesList = new ArrayList<Xpp3Dom>();
        Xpp3Dom includes = new Xpp3Dom("includes");
        includesList.add(new Xpp3Dom("include"));
        includesList.get(0).setValue("hieradata/**/*");
        includes.addChild(includesList.get(0));
        includesList.add(new Xpp3Dom("include"));
        includesList.get(1).setValue("modules/**/*");
        includes.addChild(includesList.get(1));
        includesList.add(new Xpp3Dom("include"));
        includesList.get(2).setValue("provisioning/**/*");
        includes.addChild(includesList.get(2));
        includesList.add(new Xpp3Dom("include"));
        includesList.get(3).setValue("forge/**/*");
        includes.addChild(includesList.get(3));
        source.addChild(includes);
        sources.addChild(source);
        mapping.addChild(sources);
        mappings.addChild(mapping);

        configuration.addChild(mappings);
        final Xpp3Dom defineStatements = new Xpp3Dom("defineStatements");
        defineStatements.setAttribute("combine.children", "append");
        List<Xpp3Dom> defineStatementsItem = new ArrayList<Xpp3Dom>();
        List<String> defineStatementsVal = Arrays.asList("_source_filedigest_algorithm 1", "javaVersionDir ${rpmJavaVersionDir}",
                "createJavaVersionLink ${rpmCreateJavaVersionLink}", "createInitdLink ${rpmCreateInitdLink}", "createLogLink ${rpmCreateLogLink}"
                , "createConfigLink ${rpmCreateConfigLink}", "createActiveVersionLink ${rpmCreateActiveVersionLink}", "software_version ${project.version}"
                , "software_group cab", "software_name ${rpmSoftwareName}", "_binary_filedigest_algorithm 1",
                "_binary_payload w9.gzdio", "__os_install_post %{nil}", "software_user_id ${rpmUsername}", "software_group_id ${rpmGroupname}");

        //****************** defineStatements ******************
        for (int i = 0; i < defineStatementsVal.size(); i++) {
            defineStatementsItem.add(new Xpp3Dom("defineStatements"));
            defineStatementsItem.get(i).setValue(defineStatementsVal.get(i));
            defineStatements.addChild(defineStatementsItem.get(i));
        }

        configuration.addChild(defineStatements);
        return configuration;
    }

    public List<ArtifactParameters> getPomProductDependencies() throws IOException {


        List<ArtifactParameters> ArtifactParameters = new ArrayList<ArtifactParameters>();
        try {
            for (Dependency dependency : model.getDependencies()) {
                if (dependency.getClassifier().equalsIgnoreCase("pom"))
                    ArtifactParameters.add(new ArtifactParameters(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(),""));
            }
        } catch (Exception e) {
            // Swallow
        }
        return ArtifactParameters;
    }

    public List<ArtifactParameters> getPomModuleDependencies() throws IOException {
        List<ArtifactParameters> ArtifactParameters = new ArrayList<ArtifactParameters>();
        try {
            for (Dependency dependency : model.getDependencies()) {
                if (dependency.getClassifier().equalsIgnoreCase("rpm"))
                    ArtifactParameters.add(new ArtifactParameters(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(),""));
            }
        } catch (Exception e) {
            // Swallow - no module dependencies
        }


        return ArtifactParameters;
    }

    public static class MavenPomBuilder {
        private ArtifactParameters artifact;
        private List<ArtifactParameters> productsDependencies;

        public MavenPomBuilder artifact(ArtifactParameters artifact) {
            this.artifact = artifact;
            return this;
        }

        public MavenPomBuilder dependencies(List<ArtifactParameters> productsDependencies) {
            this.productsDependencies = productsDependencies;
            return this;
        }

        public MavenRPMPom build(String workSpace ) {
            return new MavenRPMPom(this, workSpace);
        }
    }
}
