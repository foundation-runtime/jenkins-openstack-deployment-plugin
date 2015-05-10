package org.jenkinsci.plugins.os_ci.repohandlers;

import com.google.common.base.Joiner;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.util.Secret;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jenkinsci.plugins.os_ci.NexusSettings;
import org.jenkinsci.plugins.os_ci.exceptions.ArtifactVersionNotFoundException;
import org.jenkinsci.plugins.os_ci.exceptions.OsCiPluginException;
import org.jenkinsci.plugins.os_ci.model.ArtifactParameters;
import org.jenkinsci.plugins.os_ci.model.Nexus.*;
import org.jenkinsci.plugins.os_ci.utils.*;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by nbamberg on 01/12/2014.
 */
public class NexusClient {

    private static String nexusUrlPrefix;
    private String nexusUser;
    private Secret nexusPassword;
    private final String EXTERNAL_DEPENDENCIES = "external_dependencies";
    private static NexusArtifact nexusArtifact = new NexusArtifact();
    protected AbstractBuild build;
    protected BuildListener listener;
    ArtifactParameters artifactParameters;
    String encoded;
    String repoId = null;
    boolean autoDrop;
    static CloseableHttpClient httpClient;

    public NexusClient(ArtifactParameters artifactParameters, AbstractBuild build, BuildListener listener) {
        try {
            this.artifactParameters = artifactParameters;
            this.build = build;
            this.listener = listener;
            this.nexusUrlPrefix = NexusSettings.getNexusSettingsDescriptor().getNexusUrl();
            this.nexusUser = NexusSettings.getNexusSettingsDescriptor().getNexusUser();
            this.nexusPassword = NexusSettings.getNexusSettingsDescriptor().getNexusPassword();
            encoded = DatatypeConverter.printBase64Binary((nexusUser + ":" + nexusPassword).getBytes("UTF-8"));
            httpClient = HttpClients.createDefault();
        } catch (UnsupportedEncodingException e) {
            LogUtils.log(listener, "UnsupportedEncodingException: " + e.getMessage());
        }
    }

    public NexusClient(ArtifactParameters artifactParameters, AbstractBuild build, BuildListener listener, String nexusUrlPrefix,
                       String nexusUser, String nexusPassword) {
        try {
            this.artifactParameters = artifactParameters;
            this.build = build;
            this.listener = listener;
            this.nexusUrlPrefix = nexusUrlPrefix;
            this.nexusUser = nexusUser;
            this.nexusPassword = Secret.fromString(nexusPassword);
            encoded = DatatypeConverter.printBase64Binary((nexusUser + ":" + nexusPassword).getBytes("UTF-8"));
            httpClient = HttpClients.createDefault();
        } catch (UnsupportedEncodingException e) {
            LogUtils.log(listener, "UnsupportedEncodingException: " + e.getMessage());
        }
    }

    public boolean testConnection(String _nexusUrlPrefix, String _nexusUser, Secret _nexusPassword) {
        try {
            this.nexusUrlPrefix = _nexusUrlPrefix;
            this.nexusUser = _nexusUser;
            this.nexusPassword = _nexusPassword;
            URI uri = new URIBuilder().setPath(nexusUrlPrefix + "/service/local/status").build();
            HttpGet httpGet = new HttpGet(uri);
            httpGet.addHeader("AUTHORIZATION", "Basic " + encoded);
            httpGet.addHeader("Accept", "application/json");
            CloseableHttpResponse response = httpClient.execute(httpGet);
            try {
                int status = response.getStatusLine().getStatusCode();
                if (status != 200) {
                    throw new OsCiPluginException(" Failed. Please check the configuration. HTTP Status: " + status);
                }
                return true;
            } finally {
                response.close();
            }
        } catch (IOException e) {
            throw new ArtifactVersionNotFoundException("IOException Invalid Nexus server response " + e.getMessage());
        } catch (URISyntaxException e) {
            throw new ArtifactVersionNotFoundException("URISyntaxException Invalid Nexus server response " + e.getMessage());
        } catch (Exception e) {
            throw new ArtifactVersionNotFoundException("IOException in Invalid Nexus server response " + e.getMessage());
        }
    }


    public List<String> downloadProductArtifacts(ArtifactParameters ap, String targetFolder, List<String> fileTypes) {
        try {
            String repoId = null;
            if (StringUtils.containsIgnoreCase(ap.getVersion(), "snapshot")) {
                repoId = "snapshots";
            } else {
                repoId = getRepositoryId(ap);
            }
            URI uri = new URIBuilder().setPath(nexusUrlPrefix + "/service/local/repositories/" + repoId +
                    "/content/" + ap.getGroupId().replaceAll("\\.", "/") + "/" + ap.getArtifactId() +
                    "/" + ap.getVersion()).build();
            HttpGet httpGet = new HttpGet(uri);
            httpGet.addHeader("Accept", "application/json");
            CloseableHttpResponse response = httpClient.execute(httpGet);
            List<String> resourceUriList = new ArrayList<String>();
            List<String> fileTypesCopy = new ArrayList<String>(fileTypes);
            try {
                int status = response.getStatusLine().getStatusCode();
                if (status == 200) {
                    for (String resourceUri : JsonParser.getResourceURI(response, listener)) {
                        if(fileTypesCopy.isEmpty()){return resourceUriList;}
                        String type =shouldDownloadType(resourceUri, fileTypesCopy);
                        if (!(type).isEmpty()) {
                            saveArtifact(resourceUri, targetFolder);
                            resourceUriList.add(resourceUri.substring(resourceUri.lastIndexOf("/") + 1, resourceUri.length()));
                            fileTypesCopy.remove(type);
                        }
                    }
                    return resourceUriList;
                }
                throw new OsCiPluginException("Failed to get artifacts resources");
            } finally {
                response.close();
            }
        } catch (IOException e) {
            throw new OsCiPluginException("IOException in getToken " + e.getMessage());
        } catch (URISyntaxException e) {
            throw new OsCiPluginException("URISyntaxException in getToken " + e.getMessage());
        }
    }

    private String shouldDownloadType(String resourceUri, List<String> fileTypes) {
        for (String type : fileTypes) {
            if (resourceUri.endsWith(type))
                return type;
        }
        return "";
    }

    public String downloadRPM() {
        LogUtils.log(listener, "downloading from Nexus : " + artifactParameters);
        if (!artifactParameters.getOsVersion().equalsIgnoreCase("rh6")) {
            return downloadArtifact("rpm", "", null);
        }
        return downloadArtifact("rpm", artifactParameters.getOsVersion(), null);
    }

    public String downloadRPM(String targetFolder) {
        LogUtils.log(listener, "downloading from Nexus : " + artifactParameters);
        if (!artifactParameters.getOsVersion().equalsIgnoreCase("rh6")) {
            return downloadArtifact("rpm", "", targetFolder);
        }
        return downloadArtifact("rpm", artifactParameters.getOsVersion(), targetFolder);
    }

    private String downloadArtifact(String packageType, String classifier, String targetFolder) {
        FileOutputStream fos = null;
        InputStream is = null;
        File file = null;
        URL nexusUrl = null;
        String version = "";
        try {
            nexusUrl = new URL(buildNexusRedirectUrl(artifactParameters, packageType, classifier));
            URLConnection con = nexusUrl.openConnection();
            is = con.getInputStream();
            File destFile = new File(con.getURL().getFile());
            version = destFile.getParentFile().getName();
            String filename = destFile.getName();
            if (targetFolder == null)
                targetFolder = Joiner.on(File.separator).join(build.getWorkspace().getRemote(), "archive", "repo");
            if (packageType.equals("rpm") || classifier.equals(EXTERNAL_DEPENDENCIES)) {
                String path = Joiner.on(File.separator).join(targetFolder, filename);
                file = new File(path);

            } else if (classifier.equals("puppet")) {
                String path = Joiner.on(File.separator).join(build.getWorkspace().getRemote(), "archive", "prodpuppet", "modules", filename);
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

            if (packageType.equals("tar.gz") && (classifier.equals("puppet") || classifier.equals(EXTERNAL_DEPENDENCIES))) {
                CompressUtils.untarFile(file);
                LogUtils.log(listener, "delete file : " + file.getParent() + File.separator + file.getName());
                FileUtils.deleteQuietly(file);
            }

            // Rename RPM file according to rpm metadata
            if (!System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                ExecUtils.executeLocalCommand("/usr/local/bin/download_rpm.sh " + file.getPath().replaceAll(" ", "\\ "), file.getParentFile().getPath().replaceAll(" ", "\\ "));
                file.delete();
            }
            return version;

        } catch (MalformedURLException e) {
            throw new OsCiPluginException(e.toString());
        } catch (FileNotFoundException e) {
            if (classifier.isEmpty())
                version = downloadArtifact("rpm", "rpm", null);
            else if (!classifier.equals(EXTERNAL_DEPENDENCIES))
                throw new OsCiPluginException(e.toString());
        } catch (IOException e) {
            throw new OsCiPluginException(e.toString());
        } catch (InterruptedException e) {
            throw new OsCiPluginException(e.toString());
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

    public void uploadPomWithExternalDependencies(String uploadDescription, String pathToPomFile, String pomDescription, String pathToTarFile, String tarDescription, String pathToRPMFile) {
        try {
            HttpPost httppost = new HttpPost(nexusUrlPrefix + "/service/local/staging/upload");
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addTextBody("hasPom", "true")
                    .addBinaryBody("file", new File(pathToPomFile),
                            ContentType.create("application/octet-stream"), pomDescription)
                    .addTextBody("c", "");

            if (pathToTarFile != null) {
                builder.addTextBody("e", "tar.gz")
                        .addBinaryBody("file", new File(pathToTarFile),
                                ContentType.create("application/octet-stream"), tarDescription);
            }
            if (pathToRPMFile != null) {
                File rpmFile = new File(pathToRPMFile);
                if (rpmFile.exists()) {
                    builder.addTextBody("e", "rpm")
                            .addBinaryBody("file", rpmFile,
                                    ContentType.create("application/octet-stream"), tarDescription);
                }
            }
            HttpEntity httpEntity = builder.addTextBody("desc", uploadDescription)
                    .build();
            UUID sessionId = UUID.randomUUID();
            httppost.addHeader("AUTHORIZATION", "Basic " + encoded);
            httppost.addHeader("Accept", "text/html ");
            httppost.setEntity(httpEntity);
            CloseableHttpResponse response = httpClient.execute(httppost);
            int status = response.getStatusLine().getStatusCode();
            try {
                if (status != 201) {
                    throw new OsCiPluginException("Invalid Nexus server response "
                            + status + " got " + response.getStatusLine().getReasonPhrase());
                }
                nexusArtifact = JsonParser.parseNexusPomUpload(response, listener);
                NexusUpload nexusUploadPom = new NexusUpload();
                UploadData data = new UploadData();
                data.setDescription("version :" + pomDescription + ":" + uploadDescription + " is now ready for testing");
                data.setStagedRepositoryId(nexusArtifact.getRepositoryId());
                nexusUploadPom.setData(data);
                String body = JsonBuilder.createJson(nexusUploadPom);
                httpClient = HttpClients.createDefault();
                HttpPost post = new HttpPost(nexusUrlPrefix + "/service/local/staging/profiles/" + nexusArtifact.getProfileId() + "/finish");
                final StringEntity entity = new StringEntity(body, ContentType.APPLICATION_JSON);
                post.setHeader("Cookie", "JSESSIONID=" + sessionId);
                post.addHeader("AUTHORIZATION", "Basic " + encoded);
                post.addHeader("Accept", "application/json");
                post.setEntity(entity);
                response = httpClient.execute(post);
                status = response.getStatusLine().getStatusCode();
                if (status != 201) {
                    performStagingAction(StagingAction.DROP);
                    throw new OsCiPluginException("Invalid Nexus server response "
                            + status + " got " + response.getStatusLine().getReasonPhrase());
                }
            } finally {
                response.close();
            }
        } catch (IOException e) {
            throw new ArtifactVersionNotFoundException("IOException Invalid Nexus server response " + e.getMessage());
        }
    }

    public void uploadExternalDependenciesWithPuppet(String uploadDescription, String pathToPomFile, String pomDescription, String pathToTarFile, String tarDescription, String pathToRPMFile) {
        try {
            HttpPost httppost = new HttpPost(nexusUrlPrefix + "/service/local/staging/upload");
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addTextBody("hasPom", "true")
                    .addBinaryBody("file", new File(pathToPomFile),
                            ContentType.create("application/octet-stream"), pomDescription)
                    .addTextBody("c", "");

            if (pathToTarFile != null) {
                builder.addTextBody("e", "puppet.tar.gz")
                        .addBinaryBody("file", new File(pathToTarFile),
                                ContentType.create("application/octet-stream"), tarDescription);
            }
            if (pathToRPMFile != null) {
                File rpmFile = new File(pathToRPMFile);
                if (rpmFile.exists()) {
                    builder.addTextBody("e", "rpm")
                            .addBinaryBody("file", rpmFile,
                                    ContentType.create("application/octet-stream"), tarDescription);
                }
            }
            HttpEntity httpEntity = builder.addTextBody("desc", uploadDescription)
                    .build();
            UUID sessionId = UUID.randomUUID();
            httppost.addHeader("AUTHORIZATION", "Basic " + encoded);
            httppost.addHeader("Accept", "text/html ");
            httppost.setEntity(httpEntity);
            CloseableHttpResponse response = httpClient.execute(httppost);
            int status = response.getStatusLine().getStatusCode();
            try {
                if (status != 201) {
                    throw new OsCiPluginException("Invalid Nexus server response "
                            + status + " got " + response.getStatusLine().getReasonPhrase());
                }
                nexusArtifact = JsonParser.parseNexusPomUpload(response, listener);
                NexusUpload nexusUploadPom = new NexusUpload();
                UploadData data = new UploadData();
                data.setDescription("version :" + pomDescription + ":" + uploadDescription + " is now ready for testing");
                data.setStagedRepositoryId(nexusArtifact.getRepositoryId());
                nexusUploadPom.setData(data);
                String body = JsonBuilder.createJson(nexusUploadPom);
                httpClient = HttpClients.createDefault();
                HttpPost post = new HttpPost(nexusUrlPrefix + "/service/local/staging/profiles/" + nexusArtifact.getProfileId() + "/finish");
                final StringEntity entity = new StringEntity(body, ContentType.APPLICATION_JSON);
                post.setHeader("Cookie", "JSESSIONID=" + sessionId);
                post.addHeader("AUTHORIZATION", "Basic " + encoded);
                post.addHeader("Accept", "application/json");
                post.setEntity(entity);
                response = httpClient.execute(post);
                status = response.getStatusLine().getStatusCode();
                if (status != 201) {
                    performStagingAction(StagingAction.DROP);
                    throw new OsCiPluginException("Invalid Nexus server response "
                            + status + " got " + response.getStatusLine().getReasonPhrase());
                }
            } finally {
                response.close();
            }
        } catch (IOException e) {
            throw new ArtifactVersionNotFoundException("IOException Invalid Nexus server response " + e.getMessage());
        }
    }

    public void verifyRepositoryId(ArtifactParameters artifactParameters, String expectedRepoId) {
        String artifactRepo = getRepositoryId(artifactParameters);
        if (!expectedRepoId.equalsIgnoreCase(artifactRepo)) {
            throw new OsCiPluginException("The artifact is not stored in the correct Nexus Repository expected: " + expectedRepoId + " but was in : " + artifactRepo);
        }
    }


    public String getRepositoryId(ArtifactParameters artifactParameters) {
        try {
            URIBuilder uri = new URIBuilder().setPath(nexusUrlPrefix + "/service/local/lucene/search");
            uri.setParameter("g", artifactParameters.getGroupId());
            uri.setParameter("a", artifactParameters.getArtifactId());
            uri.setParameter("v", artifactParameters.getVersion());
            HttpGet httpGet = new HttpGet(uri.build());
            httpGet.addHeader("AUTHORIZATION", "Basic " + encoded);
            httpGet.addHeader("Accept", "application/json");
            CloseableHttpResponse response = httpClient.execute(httpGet);
            try {
                int status = response.getStatusLine().getStatusCode();
                if (status != 200) {
                    throw new OsCiPluginException("Invalid Nexus server response " + status);
                }
                return JsonParser.getRepoId(response, listener);
            } finally {
                response.close();
            }
        } catch (IOException e) {
            throw new ArtifactVersionNotFoundException("IOException Invalid Nexus server response " + e.getMessage());
        } catch (URISyntaxException e) {
            throw new ArtifactVersionNotFoundException("URISyntaxException Invalid Nexus server response " + e.getMessage());
        } catch (Exception e) {
            throw new ArtifactVersionNotFoundException("IOException in Invalid Nexus server response " + e.getMessage());
        }
    }

    public void performStagingAction(StagingAction stagingAction) {
        try {
            NexusRelease nexusRelease = new NexusRelease();
            StagingActionData data = new StagingActionData();
            data.setDescription("version of artifact: " + artifactParameters.getArtifactId() + artifactParameters.getVersion() + " artifact is now in : " + StagingAction.values()[stagingAction.getValue()]);
            data.setStagedRepositoryIds(new String[]{getRepositoryId(artifactParameters)});
            if (stagingAction.getValue() == StagingAction.PROMOTE.getValue()) {
                data.setAutoDropAfterRelease(true);
            }
            nexusRelease.setData(data);
            String body = JsonBuilder.createJson(nexusRelease);
            HttpPost post = new HttpPost(nexusUrlPrefix + "/service/local/staging/bulk/" + StagingAction.values()[stagingAction.getValue()]);
            final StringEntity entity = new StringEntity(body, ContentType.APPLICATION_JSON);
            post.setHeader("Cookie", "JSESSIONID=" + "sessionId");
            post.addHeader("AUTHORIZATION", "Basic " + encoded);
            post.addHeader("Accept", "application/json");
            post.setEntity(entity);
            CloseableHttpResponse response = httpClient.execute(post);
            int status = status = response.getStatusLine().getStatusCode();
            try {
                if (status != 201) {
                    throw new OsCiPluginException("Invalid Nexus server response "
                            + status + " got " + response.getStatusLine().getReasonPhrase());
                }
            } finally {
                response.close();
            }
        } catch (IOException e) {
            throw new ArtifactVersionNotFoundException("IOException Invalid Nexus server response " + e.getMessage());
        }
    }

    public String getStagingRepositoryProfile() throws Exception {

        try {
            URIBuilder uri = new URIBuilder().setPath(nexusUrlPrefix + "/service/local/staging/profiles");
            HttpGet httpGet = new HttpGet(uri.build());
            httpGet.addHeader("AUTHORIZATION", "Basic " + encoded);
            httpGet.addHeader("Accept", "application/json");
            CloseableHttpResponse response = httpClient.execute(httpGet);
            try {
                int status = response.getStatusLine().getStatusCode();
                if (status != 200) {
                    throw new OsCiPluginException("Invalid Nexus server response " + status);
                }
                return JsonParser.getProfileId(response, listener);
            } finally {
                response.close();
            }
        } catch (IOException e) {
            throw new OsCiPluginException("IOException in getstagingRepositoryProfile " + e.getMessage());
        } catch (URISyntaxException e) {
            throw new OsCiPluginException("URISyntaxException in getstagingRepositoryProfile " + e.getMessage());
        }
    }

    private String buildNexusRedirectUrl(ArtifactParameters artifact, String packageType, String classifier) {

        URIBuilder uri = new URIBuilder().setPath(nexusUrlPrefix + "/service/local/artifact/maven/redirect");
        uri.setParameter("g", artifact.getGroupId());
        uri.setParameter("a", artifact.getArtifactId());
        uri.setParameter("v", artifact.getVersion());
        uri.setParameter("p", packageType);
        if (StringUtils.isNotBlank(classifier)) {
            uri.setParameter("c", classifier);
        }

        uri.setParameter("r", getRepositoryId(artifact));
        return uri.toString();
    }


    private boolean validateConfiguration() {
        if (nexusUrlPrefix == null || nexusUser == null || nexusPassword == null ||
                nexusUrlPrefix.isEmpty() || nexusUser.isEmpty() || nexusPassword.getPlainText().isEmpty()) {
            return false;
        }
        return true;
    }

    public String getLatestVersion(String repoId) {
        this.repoId = repoId;
        return getLatestVersion();
    }

    public String getLatestVersion() {
        try {
            URIBuilder uri = new URIBuilder().setPath(nexusUrlPrefix + "/service/local/lucene/search");
            uri.setParameter("g", artifactParameters.getGroupId());
            uri.setParameter("a", artifactParameters.getArtifactId());
            if (repoId != null) {
                uri.setParameter("r", repoId);
            }
            HttpGet httpGet = new HttpGet(uri.build());
            httpGet.addHeader("AUTHORIZATION", "Basic " + encoded);
            httpGet.addHeader("Accept", "application/json");
            CloseableHttpResponse response = httpClient.execute(httpGet);
            try {
                int status = response.getStatusLine().getStatusCode();
                if (status != 200) {
                    throw new OsCiPluginException("Invalid Nexus server response " + status);
                }
                String version = null;
                if (repoId != null) {
                    version = JsonParser.getLatestVersion(response, listener, repoId);

                } else {
                    version = JsonParser.getLatestVersion(response, listener, null);
                }

                LogUtils.log(listener, "Artifact" + artifactParameters.getGroupId() + ":" + artifactParameters.getArtifactId() + " Latest version is :" + version);
                return version;
            } finally {
                response.close();
            }
        } catch (IOException e) {
            throw new ArtifactVersionNotFoundException("IOException Invalid Nexus server response " + e.getMessage());
        } catch (URISyntaxException e) {
            throw new ArtifactVersionNotFoundException("URISyntaxException Invalid Nexus server response " + e.getMessage());
        }
    }


    private void saveArtifact(String requestUri, String targetFile) {
        try {

            URI uri = new URIBuilder().setPath(requestUri).build();
            HttpGet httpGet = new HttpGet(uri);

            CloseableHttpResponse response = httpClient.execute(httpGet);

            InputStream input_s = response.getEntity().getContent();
            FileOutputStream fos;

            FileUtils.forceMkdir(new File(targetFile));

            if (requestUri.endsWith("pom"))
                fos = new FileOutputStream(new File(Joiner.on(File.separator).join(targetFile, "pom.xml")));
            else if (requestUri.endsWith("tar.gz"))
                fos = new FileOutputStream(new File(Joiner.on(File.separator).join(targetFile, "external_dependencies.tar.gz")));
            else if (requestUri.endsWith("rpm"))
                fos = new FileOutputStream(new File(Joiner.on(File.separator).join(targetFile, requestUri.substring(requestUri.lastIndexOf("/") + 1, requestUri.length()))));
            else
                fos = new FileOutputStream(new File(Joiner.on(File.separator).join(targetFile, "repo")));
            int read = 0;
            byte[] buffer = new byte[32768];
            while ((read = input_s.read(buffer)) > 0) {
                fos.write(buffer, 0, read);
            }

            fos.close();
            input_s.close();
        } catch (IOException e) {
            throw new OsCiPluginException("IOException in getToken " + e.getMessage());
        } catch (URISyntaxException e) {
            throw new OsCiPluginException("URISyntaxException in getToken " + e.getMessage());
        }

    }

    public String increaseVersion(String increaseVersionOption) {
        String ver;
        try {
            LogUtils.logSection(listener, "Setting product version");
            ver = getLatestVersion();
            LogUtils.log(listener, "Latest product version: " + ver);

            ver = VersionUtils.increaseArtifactVersion(ver, Enum.valueOf(VersionUtils.increaseVersionOptions.class, increaseVersionOption));
            LogUtils.log(listener, "Version Change:" + increaseVersionOption + ". Setting version to: " + ver);

        } catch (ArtifactVersionNotFoundException e) {
            ver = "1.0.0-0";
            LogUtils.log(listener, "Product doesn't exist in Nexus yet.  Setting version to " + ver);
        }
        return ver;
    }

}
