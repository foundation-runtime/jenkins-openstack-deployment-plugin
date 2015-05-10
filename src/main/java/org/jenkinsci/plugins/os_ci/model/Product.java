package org.jenkinsci.plugins.os_ci.model;

import com.google.common.base.Joiner;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.os_ci.ProductDeployer;
import org.jenkinsci.plugins.os_ci.exceptions.OsCiPluginException;
import org.jenkinsci.plugins.os_ci.exceptions.ProductDeployPluginException;
import org.jenkinsci.plugins.os_ci.model.Openstack.FloatingIP;
import org.jenkinsci.plugins.os_ci.model.Openstack.StackDetails;
import org.jenkinsci.plugins.os_ci.model.Openstack.StackStatus;
import org.jenkinsci.plugins.os_ci.model.maven.MavenPom;
import org.jenkinsci.plugins.os_ci.model.maven.MavenRPMPom;
import org.jenkinsci.plugins.os_ci.repohandlers.NexusClient;
import org.jenkinsci.plugins.os_ci.repohandlers.OpenStackClient;
import org.jenkinsci.plugins.os_ci.utils.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

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
public class Product {

    static final long CREATE_TIMEOUT = 40 * 60 * 1000;
    static final long DELETE_TIMEOUT = 30 * 60 * 1000;
    static final long SLEEP_TIME = 60 * 1000;

    private final ArtifactParameters artifact;
    private final AbstractBuild build;
    private final BuildListener listener;

    public Product(ArtifactParameters artifact, AbstractBuild build, BuildListener listener) {
        this.artifact = artifact;
        this.build = build;
        this.listener = listener;
    }

    public ArtifactParameters getArtifact() {
        return artifact;
    }

    public boolean deploy(NexusClient nexusClient, OpenStackClient openStackClient, YumRepoParameters yumRepoParameters, ProductDeployer.DescriptorImpl descriptor) throws Exception {
        final String targetFolder = Joiner.on(File.separator).join(build.getWorkspace().getRemote(), "archive");

        List<String> fileTypes = new ArrayList<String>();
        fileTypes.add("pom");
        fileTypes.add("tar.gz");
        fileTypes.add("rpm");
        fileTypes.add("rh6");
        nexusClient.downloadProductArtifacts(getArtifact(), Joiner.on(File.separator).join(targetFolder, artifact.getArtifactId()), fileTypes);
        //****************** get pom file dependencies ******************
        MavenPom mavenPom = new MavenPom(new File(Joiner.on(File.separator).join(targetFolder, artifact.getArtifactId(), "pom.xml")));
        List<ArtifactParameters> subProducts = mavenPom.getPomProductDependencies();

        for (ArtifactParameters m : subProducts) {
            Product p = new Product(m, build, listener);
            boolean return_ = p.deploy(nexusClient, openStackClient, yumRepoParameters, descriptor);
            if (!return_)
                throw new ProductDeployPluginException("Deploy dependent product " + m.getArtifactId() + " failed.");
        }

        LogUtils.logSection(listener, "Deploy Product " + artifact.getArtifactId());

        String buildId = Joiner.on("-").join(new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SS").format(build.getTime()), descriptor.getDeployCounter());


        if (!new File(Joiner.on(File.separator).join(targetFolder, artifact.getArtifactId(), "external_dependencies.tar.gz")).exists()) {
            // if there isn't an external_dependencies.tar.gz file
            // only a pom file => we're deploying a profile
            LogUtils.log(listener, "Finish Deploy: " + artifact.getArtifactId());
            return true;
        }

        CompressUtils.untarFile(new File(Joiner.on(File.separator).join(targetFolder, artifact.getArtifactId(), "external_dependencies.tar.gz")));
        LogUtils.log(listener, "Untar File: " + Joiner.on(File.separator).join(targetFolder, artifact.getArtifactId(), "external_dependencies.tar.gz"));

        //****************** move deployment-scripts ******************
        // Push scripts to YUM repo machine
        copyFolderToRepoMachine(
                build,
                listener,
                yumRepoParameters,
                Joiner.on(File.separator).join(targetFolder, artifact.getArtifactId(), "archive", "deploy-scripts"),
                Joiner.on("/").join("/var", "www", "html", "build", buildId));
        LogUtils.log(listener, "Copy deployment-scripts folder to Yum Repo machine.");

        // move external rpms from archive/product/archive/rpms  to /archive/repo folder

        if (new File(Joiner.on(File.separator).join(targetFolder, artifact.getArtifactId(), "archive", "repo")).exists()) {
            moveExternalRpmsToRepoDirectory(Joiner.on(File.separator).join(targetFolder, artifact.getArtifactId(), "archive", "repo"),
                    Joiner.on(File.separator).join(targetFolder, "repo"));
            LogUtils.log(listener, "Copied external RPMS to repo directory.");
        }

        File deploymentScriptsRPM = new File(Joiner.on(File.separator).join(targetFolder, artifact.getArtifactId(), artifact.getArtifactId() + "-" + artifact.getVersion() + ".rpm"));
        if (deploymentScriptsRPM.exists()) {
//            Rename RPM file according to rpm metadata
            if (!System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                ExecUtils.executeLocalCommand("/usr/local/bin/download_rpm.sh " + deploymentScriptsRPM.getPath().replaceAll(" ", "\\ "), deploymentScriptsRPM.getParentFile().getPath().replaceAll(" ", "\\ "));
                deploymentScriptsRPM.delete();
                deploymentScriptsRPM = new File(Joiner.on(File.separator).join(targetFolder, artifact.getArtifactId(), "nds_" + artifact.getArtifactId() + "_deployment-scripts" + "-" + artifact.getVersion() + "_1.noarch.rpm"));
            }

            LogUtils.log(listener, deploymentScriptsRPM.getParentFile().list().toString());
            FileUtils.moveFileToDirectory(deploymentScriptsRPM,
                    new File(Joiner.on(File.separator).join(targetFolder, "repo")), true);
            LogUtils.log(listener, "Copied deployment-scripts to repo directory");
        }
        MavenPom mp = new MavenPom(new File(Joiner.on(File.separator).join(targetFolder, artifact.getArtifactId(), "pom.xml")));
        List<ArtifactParameters> rpms = mp.getPomModuleDependencies();

        // download rpms to archive/repo
        LogUtils.logSection(listener, "Download dependent RPMS.");
        for (ArtifactParameters m : rpms) {
            new NexusClient(m, build, listener).downloadRPM(Joiner.on(File.separator).join(targetFolder, "repo"));
        }

        // create yum repo
        createAndMoveYumRepo(build, listener, yumRepoParameters, Joiner.on(File.separator).join(targetFolder, "repo"), String.valueOf(descriptor.getDeployCounter()));
        LogUtils.log(listener, "YUM repository have been created.");

        // deploy stack
        String stackName = artifact.getArtifactId().toLowerCase().replace("-product", "");

        openStackClient.createStack(
                stackName,
                descriptor.getOverridingParameters(),
                descriptor.getGlobalOutputs(),
                Joiner.on(File.separator).join(targetFolder, artifact.getArtifactId(), "archive", "heat", stackName));

        long startTime = System.currentTimeMillis();
        boolean createComplete = false;

        while (!createComplete && System.currentTimeMillis() - startTime < CREATE_TIMEOUT) {
            StackStatus stackStatus = openStackClient.getStackStatus(stackName);
            LogUtils.log(listener, "Waiting for stack creation for " + stackName + ". Status is: " + stackStatus);
            if (stackStatus == StackStatus.CREATE_COMPLETE) {
                createComplete = true;
                // update outputs map
                StackDetails stackOutputs = openStackClient.getStackOutputs(stackName);
                descriptor.setGlobalOutputsWithNewOutputs(stackOutputs.getOutputs());
            } else if (stackStatus == StackStatus.FAILED || stackStatus == StackStatus.CREATE_FAILED || stackStatus == StackStatus.UNDEFINED)
                throw new OsCiPluginException("Failed to Launch Stack " + stackName);
            else
                Thread.sleep(SLEEP_TIME);

        }
        // if stack is not complete after 40 minutes -  throw a timeout exception
        if (!createComplete)
            throw new TimeoutException("Create Stack- timeout exception");

        // clean files
        try {
            FileUtils.cleanDirectory(new File(Joiner.on(File.separator).join(targetFolder, "repo")));
        } catch (IOException e) { /*Swallow*/ }

        descriptor.increaseDeployCounter();
        LogUtils.log(listener, "Increased deployment counter.");

        return true;

    }


    public boolean cleanOpenstackBeforeDeployment(NexusClient nexusClient, OpenstackParameters openstackParameters, ProductDeployer.DescriptorImpl descriptor) throws Exception {
        final String targetFolder = Joiner.on(File.separator).join(build.getWorkspace().getRemote(), "archive");


        OpenStackClient openStackClient = new OpenStackClient(listener, openstackParameters.getOpenstackUser(), openstackParameters.getOpenstackPassword(), openstackParameters.getOpenstackTenant(), "/v2.0/tokens", openstackParameters.getOpenstackIP(), 5000);

//        String[] fileTypes = {"pom"};
        List<String> fileTypes = new ArrayList<String>();
        fileTypes.add("pom");
        nexusClient.downloadProductArtifacts(getArtifact(), Joiner.on(File.separator).join(targetFolder, artifact.getArtifactId()), fileTypes);

        // parse pom and get its product dependencies
        MavenPom mp = new MavenPom(new File(Joiner.on(File.separator).join(targetFolder, artifact.getArtifactId(), "pom.xml")));
        List<ArtifactParameters> dependentProducts = mp.getPomProductDependencies();

        List<ArtifactParameters> subsystems = new ArrayList<ArtifactParameters>();
        subsystems.addAll(dependentProducts);
        subsystems.add(artifact);

        LogUtils.logSection(listener, "Clean Openstack previous deployments:");
        // delete all stacks
        if (subsystems != null) {
            for (ArtifactParameters s : subsystems) {
                String productName = s.getArtifactId().toLowerCase().replace("-product", "");

                if (openStackClient.stackExists(productName)) {
                    LogUtils.log(listener, "Deleting stack " + productName);
                    openStackClient.deleteStack(productName);
                    LogUtils.log(listener, "Releasing IPs");
                    openStackClient.releaseStackFloatingIPs(productName);
                }
            }
        }
        descriptor.setGlobalOutputs(new HashMap<String, String>());

        // verify all deletions completed - run every minute for 30 minutes.
        if (subsystems != null) {
            for (ArtifactParameters s : subsystems) {
                String productName = s.getArtifactId().toLowerCase().replace("-product", "");

                long startTime = System.currentTimeMillis();
                boolean deleteComplete = false;

                while (!deleteComplete && (System.currentTimeMillis() - startTime < DELETE_TIMEOUT)) {
                    StackStatus stackStatus = openStackClient.getStackStatus(productName);
                    LogUtils.log(listener, "Waiting for deletion of " + productName + ". Status:" + stackStatus);
                    if (stackStatus == StackStatus.DELETE_COMPLETE || stackStatus == StackStatus.UNDEFINED) {
                        deleteComplete = true;
                    } else
                        Thread.sleep(SLEEP_TIME);
                }
                if (!deleteComplete)
                    throw new TimeoutException("Delete Stack -timeout exception");
            }
        }


        // Prepare parameters map which override the stack 'env'
        prepareStackOverrides(openStackClient,openstackParameters, descriptor);
        LogUtils.log(listener, "Get User parameters");

        descriptor.resetDeployCounter();
        return true;

    }


    private void setSymlinkOnYumRepo(YumRepoParameters yumRepoParameters, String sourcePath, String targetPath) throws IOException {
        FtpUtils.createRemoteSymLink(sourcePath, targetPath, yumRepoParameters.getYumRepoIP(), yumRepoParameters.getYumRepoPrivateKey());
    }

    private void createAndMoveYumRepo(AbstractBuild build, BuildListener listener, YumRepoParameters yumRepoParameters, String folder, String deployCounter) throws IOException, InterruptedException {

        String deploymentScriptsRPM = "nds_" + artifact.getArtifactId() + "_deployment-scripts-" + artifact.getVersion() + "_1.noarch.rpm";

        String basepath = Joiner.on("/").join("/var", "www", "html", "build", "latest");
        String packagespath = Joiner.on("/").join("/var", "www", "html", "ci-repo", "Packages",deploymentScriptsRPM);
        String buildId = Joiner.on("-").join(new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SS").format(build.getTime()), deployCounter);

        copyFolderToRepoMachine(
                build,
                listener,
                yumRepoParameters,
                folder,
                Joiner.on("/").join("/var", "www", "html", "build", buildId));

        // Update symlink on yum repo to point to latest build
        setSymlinkOnYumRepo(yumRepoParameters, Joiner.on("/").join("/var", "www", "html", "build", buildId), basepath);



        String cmds[] = {
                "rm -rf " + packagespath ,
                "createrepo -o " + basepath + "/repo " + basepath + "/repo",
                "pakrat --name ci-repo --repoversion " + buildId + " --baseurl http://" + yumRepoParameters.getYumRepoIP() + "/build/latest/repo/ --outdir /var/www/html/",
                "rm -fr " + basepath + "/repo/*.rpm"};
        ExecUtils.executeRemoteCommand(listener, cmds, yumRepoParameters.getYumRepoIP(), yumRepoParameters.getYumRepoPrivateKey());
    }

    private void copyFolderToRepoMachine(AbstractBuild build, BuildListener listener, YumRepoParameters yumRepoParameters, String sourceFolder, String targetFolder) {
        FileOutputStream fos = null;
        try {
            String rsapath = Joiner.on(File.separator).join(build.getWorkspace().getRemote(), "id_rsa");
            fos = new FileOutputStream(new File(rsapath));
            fos.write(yumRepoParameters.getYumRepoPrivateKey().getBytes());

            // Copy scripts and yum folders to yum repo machine
            FtpUtils.copyLocalFolderToRemote(sourceFolder,
                    targetFolder,
                    yumRepoParameters.getYumRepoIP(),
                    rsapath
            );
        } catch (IOException e) {
            throw new OsCiPluginException(e.getMessage());
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

    private void moveExternalRpmsToRepoDirectory(String rpmsSrcFolder, String repoTargetFolder) {

        File repoFolder = new File(repoTargetFolder);

        for (File f : new File(rpmsSrcFolder).listFiles())
            try {
                FileUtils.moveFileToDirectory(f, repoFolder, true);
            } catch (IOException e) {
            }
    }

    private void prepareStackOverrides(OpenStackClient openStackClient ,OpenstackParameters openstackParameters, ProductDeployer.DescriptorImpl descriptor) {
        Map<String, String> overrides = descriptor.getOverridingParameters();

        overrides.put("urn:com:cisco:vci:service:version", artifact.getVersion());

        // Openstack Parameters
        if (StringUtils.isNotEmpty(openstackParameters.getKeyPair())){
            overrides.put("key_name", openstackParameters.getKeyPair());
            overrides.put("urn:com:cisco:vci:heat:stack:keypairid", openstackParameters.getKeyPair());
        }
        if (StringUtils.isNotEmpty(openstackParameters.getPrivateNetworkID())){
            overrides.put("private_network_id", openstackParameters.getPrivateNetworkID());
            overrides.put("urn:com:cisco:vci:heat:stack:networkid", openstackParameters.getPrivateNetworkID());
        }
        if (StringUtils.isNotEmpty(openstackParameters.getPrivateSubnetID())){
            overrides.put("private_subnet_id", openstackParameters.getPrivateSubnetID());
            overrides.put("urn:com:cisco:vci:heat:stack:subnetid", openstackParameters.getPrivateSubnetID());
        }

        if (MapUtils.isNotEmpty(openstackParameters.getAdditionalParams())) {
            for (Map.Entry<String, String> entry : openstackParameters.getAdditionalParams().entrySet())
                overrides.put(entry.getKey(), entry.getValue());

            for (Map.Entry<String, String> entry : openstackParameters.getAdditionalParams().entrySet())
                overrides.put("urn:com:cisco:vci:service:parameter:" + entry.getKey(), entry.getValue());

            //    overrides.put("hiera_params", JsonParser.convertMapToJsonString(openstackParameters.getAdditionalParams()));
        }

        // Policy Parameters
        if (openstackParameters.getPolicyParameters().getNodeConfigurationsParameters() != null) {
            // Node Configuration
            for (NodeConfiguration nodeConfiguration : openstackParameters.getPolicyParameters().getNodeConfigurationsParameters()) {
                overrides.put(nodeConfiguration.getName() + ".imagename", nodeConfiguration.getImageName());
                overrides.put(nodeConfiguration.getName() + ".imageid", nodeConfiguration.getImageId());
                overrides.put(nodeConfiguration.getName() + ".flavorname", nodeConfiguration.getFlavorName());
                overrides.put(nodeConfiguration.getName() + ".flavorid", nodeConfiguration.getFlavorId());
                overrides.put(nodeConfiguration.getName() + ".quantity:min", nodeConfiguration.getQuantityMin());
                overrides.put(nodeConfiguration.getName() + ".quantity:max", nodeConfiguration.getQuantityMax());

                overrides.put("image", nodeConfiguration.getImageName());
                overrides.put("flavor", nodeConfiguration.getFlavorName());
                overrides.put("min_size", nodeConfiguration.getQuantityMin());
                overrides.put("max_size", nodeConfiguration.getQuantityMax());
            }
        }
        // Domains
        if (openstackParameters.getPolicyParameters().getDomains() != null) {

            String public_network_name = openStackClient.geListOfPublicNetworks().get(0).getName();
            LogUtils.logSection(listener, "DNS Registration:");

            for (Domain domain : openstackParameters.getPolicyParameters().getDomains()) {
                FloatingIP fip = openStackClient.createFloatingIP(public_network_name);
                // register to external DNS
                registerToExternalDNS(domain.getFqdn(), fip.getFloatingIpAddress());

                overrides.put(domain.getName() + ".fqdn", domain.getFqdn());
                overrides.put(domain.getName() + ".floatingipid", fip.getFloatingIpId());

                overrides.put("fqdn", domain.getFqdn());
                overrides.put("floating_ip_id", fip.getFloatingIpId());
            }
        }

        //AZs
        if (openstackParameters.getPolicyParameters().getAzs() != null) {
            for (AZ az : openstackParameters.getPolicyParameters().getAzs()) {
                overrides.put(az.getName() + ".azname", az.getazname());

                overrides.put("availability_zone", az.getazname());
            }
        }

        descriptor.setOverridingParameters(overrides);
    }

    public boolean downloadGitRepos(String tempTargetFolder, String targetFolder, String scriptsFolder, String scriptsGitUrl, String heatGitUrl, String puppetBaseGitUrl) {
        try {

            LogUtils.logSection(listener, "Downloading git repos");
            // Get Scripts from GIT repo
            if (!scriptsGitUrl.isEmpty()) {
                new GitClient(tempTargetFolder, scriptsGitUrl, "master", "deploy-scripts", listener);
                LogUtils.log(listener, "Downloaded repo " + scriptsGitUrl);
            }
            if (!heatGitUrl.isEmpty()) {
                // Get HEAT templates from GIT repo
                new GitClient(tempTargetFolder, heatGitUrl, "master", "heat", listener);
                LogUtils.log(listener, "Downloaded repo " + heatGitUrl);
            }
            // HEAT repository include templates for all product.
            // Select only the ones we need.
            saveProductResourcesOnly(tempTargetFolder, targetFolder);

            if (!puppetBaseGitUrl.isEmpty()) {
                new GitClient(scriptsFolder, puppetBaseGitUrl, "master", "puppets", listener);
                LogUtils.log(listener, "Downloaded repo " + puppetBaseGitUrl);
//                createTar("puppet-base.tar.gz", Joiner.on(File.separator).join(scriptsFolder, "puppet-base"));
                //TODO copy
            }

            LogUtils.log(listener, "Saved necessary files only");

        } catch (IOException e) {
            LogUtils.log(listener, "Got an error fetching git repository: " + e.getMessage());
            return false;
        }
        return true;
    }

    private void saveProductResourcesOnly(String srcDir, String tarDir) throws IOException {
        String plainId = artifact.getArtifactId().substring(0, artifact.getArtifactId().lastIndexOf('-'));
        FileUtils.forceMkdir(new File(Joiner.on(File.separator).join(tarDir, "heat")));
        FileUtils.forceMkdir(new File(Joiner.on(File.separator).join(tarDir, "deploy-scripts")));

        // save GLOBAL scripts
        File fgScripts = new File(Joiner.on(File.separator).join(srcDir, "deploy-scripts", "GLOBAL"));
        if (fgScripts.isDirectory()) {
            FileUtils.moveDirectory(fgScripts,
                    new File(Joiner.on(File.separator).join(tarDir, "deploy-scripts", "GLOBAL")));
        }

        // save product resources
        File fHeat = new File(Joiner.on(File.separator).join(srcDir, "heat", plainId.toLowerCase()));
        if (fHeat.isDirectory()) {
            FileUtils.moveDirectory(fHeat,
                    new File(Joiner.on(File.separator).join(tarDir, "heat", plainId.toLowerCase())));
        } else {
            throw new OsCiPluginException("HEAT template missing in repo for product" + plainId.toLowerCase());
        }

        File fScripts = new File(Joiner.on(File.separator).join(srcDir, "deploy-scripts", plainId.toUpperCase()));
        if (fScripts.isDirectory()) {
            FileUtils.moveDirectory(fScripts,
                    new File(Joiner.on(File.separator).join(tarDir, "deploy-scripts", plainId.toUpperCase())));
        }

        // delete temp_archive directory - failed to delete pack file
        //FileUtils.forceDelete(new File(srcDir));
    }

    public String createPom(String targetFolder, ArrayList<ArtifactParameters> productDependencies, ArrayList<ArtifactParameters> artifactDependencies) {
        String pomPath = null;
        try {
            LogUtils.logSection(listener, "Creating pom.xml");

            FilePath pomDir = new FilePath(new File(targetFolder)).getParent();
            pomPath = Joiner.on(File.separator).join(pomDir, "pom.xml");
            File f = new File(pomPath);
            new MavenPom.MavenPomBuilder()
                    .artifact(artifact)
                    .dependencies(productDependencies, artifactDependencies)
                    .build().save(f);
            LogUtils.log(listener, "pom.xml: " + pomPath);
        } catch (FileNotFoundException e) {
            LogUtils.log(listener, "could not save pom.xml " + e.getMessage());
        } catch (IOException e) {
            LogUtils.log(listener, "could not save pom.xml " + e.getMessage());
        }
        return pomPath;

    }

    public String createRPMPom(String targetFolder) {
        String pomPath = null;
        try {
            LogUtils.logSection(listener, "Creating pom.xml");

            FilePath pomDir = new FilePath(new File(targetFolder));
            pomPath = Joiner.on(File.separator).join(pomDir, "pom.xml");
            File f = new File(pomPath);
            new MavenRPMPom.MavenPomBuilder()
                    .artifact(artifact)
                    .build(targetFolder).save(f);
            LogUtils.log(listener, "pom.xml: " + pomPath);
        } catch (FileNotFoundException e) {
            LogUtils.log(listener, "could not save pom.xml " + e.getMessage());
        } catch (IOException e) {
            LogUtils.log(listener, "could not save pom.xml " + e.getMessage());
        }
        return pomPath;

    }

    public String createExternalDependenciesTar(String targetFolder) {
        return createTar("external_dependencies.tar.gz", targetFolder);
    }

    public String createTar(String filename, String targetFolder) {
        String tarPath;
        try {
            LogUtils.logSection(listener, "Compressing artifacts");
            FilePath tarDir = new FilePath(new File(targetFolder)).getParent();
            tarPath = Joiner.on(File.separator).join(tarDir, filename);
            CompressUtils.tarGzDirectory(targetFolder, tarPath);
            LogUtils.log(listener, "Created archive: " + tarPath);
        } catch (IOException e) {
            LogUtils.log(listener, "Got an error compressing directory: " + e.getMessage());
            return null;
        }
        return tarPath;
    }


    public void registerToExternalDNS(String fqdn, String ipaddress) {
        try{
            if (!System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                // re-register ip ot fqdn
                LogUtils.log(listener, "DNS registration: " + fqdn + " to " + ipaddress);

                if (fqdn.contains("_"))
                    throw new InterruptedException("FQDN string cannot contain '_'");
                ExecUtils.executeLocalCommand("/usr/local/bin/do_nsupdate -D -N " + fqdn + " -R A", "/usr/local/bin/");
                ExecUtils.executeLocalCommand("/usr/local/bin/do_nsupdate -I " + ipaddress + " -A -N " + fqdn + " -R A", "/usr/local/bin/");
            }
        }
        catch (InterruptedException e)
        {
            LogUtils.log(listener, "could not register IP Address to external DNS " + e.getMessage());
            
        }
    }

}
