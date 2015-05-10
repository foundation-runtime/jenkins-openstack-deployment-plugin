package org.jenkinsci.plugins.os_ci.model.maven;

import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jenkinsci.plugins.os_ci.NexusSettings;
import org.jenkinsci.plugins.os_ci.exceptions.OsCiPluginException;
import org.jenkinsci.plugins.os_ci.model.ArtifactParameters;

import java.io.*;
import java.util.ArrayList;
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
 */
public class MavenPom {
    private ArtifactParameters artifact;
    private List<ArtifactParameters> productsDependencies;
    private List<ArtifactParameters> dependentArtifacts;
    Model model = new Model();

    private MavenPom (MavenPomBuilder pomBuilder) {
        artifact = pomBuilder.artifact;
        productsDependencies = pomBuilder.productsDependencies;
        dependentArtifacts = pomBuilder.artifactDependencies;
    }

    public MavenPom(File pomFile) {
        try {
            FileInputStream fis = new FileInputStream(pomFile);
            model =  new MavenXpp3Reader().read(fis);
        } catch (IOException e) {
            // Swallow - no module dependencies
        } catch (XmlPullParserException e) {
            throw new OsCiPluginException(e.getMessage());
        }

    }

    public ArtifactParameters getArtifact() {
        return artifact;
    }

    public List<ArtifactParameters> getArtifactParameters() {
        return dependentArtifacts;
    }

    public List<ArtifactParameters> getProductsDependencies() {
        return productsDependencies;
    }


    public void save(File file) throws FileNotFoundException, IOException{
        model.setName("pom for " + artifact.getArtifactId());
        model.setModelVersion("4.0.0");
        model.setGroupId( artifact.getGroupId() );
        model.setArtifactId(artifact.getArtifactId());
        model.setVersion(artifact.getVersion());
        model.setPackaging("pom");
        String nexusUrlPrefix = NexusSettings.getNexusSettingsDescriptor().getNexusDocUrl();
        model.setUrl(nexusUrlPrefix + "/${project.groupId}/${project.artifactId}/${project.version}");
        Organization org = new Organization();
        org.setName("Cisco");
        org.setUrl("http://www.cisco.com");

        Properties properties = new Properties();
        properties.setProperty("project.build.sourceEncoding", "UTF-8");
        model.setProperties(properties);

        model.setOrganization(org);
        Parent parent = new Parent();
        parent.setVersion("3.0.6-1");
        parent.setGroupId("com.nds.cab.build");
        parent.setArtifactId("base");
        model.setParent(parent);

        Site site = new Site();
        site.setId("NDS Site");
        String nexusScpUrlPrefix = NexusSettings.getNexusSettingsDescriptor().getNexusDocUrl().replace("http://", "scp://");
        site.setUrl(nexusScpUrlPrefix + "/cifs/maven_sites/${project.groupId}/${project.artifactId}/${project.version}");
        DistributionManagement dm = new DistributionManagement();
        dm.setSite(site);
        model.setDistributionManagement(dm);

        if (productsDependencies != null) {
            for (ArtifactParameters d : productsDependencies) {
                Dependency dep = new Dependency();
                dep.setArtifactId(d.getArtifactId());
                dep.setGroupId(d.getGroupId());
                dep.setVersion(d.getVersion());
                dep.setClassifier("pom");
                model.addDependency(dep);
            }
        }

        if (dependentArtifacts != null) {
            for (ArtifactParameters d : dependentArtifacts) {
                Dependency dep = new Dependency();
                dep.setArtifactId(d.getArtifactId());
                dep.setGroupId(d.getGroupId());
                dep.setVersion(d.getVersion());
                dep.setClassifier(d.getOsVersion());
                model.addDependency(dep);
            }
        }
        FileOutputStream fos = new FileOutputStream(file);
        new MavenXpp3Writer().write(fos, model);

    }

    public List<ArtifactParameters> getPomProductDependencies() throws IOException {


        List<ArtifactParameters> ArtifactParameters = new ArrayList<ArtifactParameters>();
        try {
            for (Dependency dependency : model.getDependencies()) {
                if (dependency.getClassifier().equalsIgnoreCase("pom"))
                    ArtifactParameters.add(new ArtifactParameters(dependency.getGroupId(), dependency.getArtifactId(),dependency.getVersion(),""));
            }
        } catch (Exception e) {
            // Swallow
        }
        return ArtifactParameters;
    }

    public List<ArtifactParameters> getPomModuleDependencies() throws IOException  {
        List<ArtifactParameters> ArtifactParameters = new ArrayList<ArtifactParameters>();
        try {
            for (Dependency dependency: model.getDependencies()){
                if (dependency.getClassifier().equalsIgnoreCase("rpm")||dependency.getClassifier().equalsIgnoreCase("rh6"))
                    ArtifactParameters.add(new ArtifactParameters(dependency.getGroupId(),dependency.getArtifactId(), dependency.getVersion(),dependency.getClassifier()));
            }
        } catch (Exception e) {
            // Swallow - no module dependencies
        }


        return ArtifactParameters;
    }

    public static class MavenPomBuilder {
        private ArtifactParameters artifact;
        private List<ArtifactParameters> productsDependencies;
        private List<ArtifactParameters> artifactDependencies;

        public MavenPomBuilder artifact(ArtifactParameters artifact) {
            this.artifact = artifact;
            return this;
        }

        public MavenPomBuilder dependencies (List<ArtifactParameters> productsDependencies, List<ArtifactParameters> artifactDependencies) {
            this.artifactDependencies = artifactDependencies;
            this.productsDependencies = productsDependencies;
            return this;
        }
        public MavenPom build() {
            return new MavenPom(this);
        }
    }
}
