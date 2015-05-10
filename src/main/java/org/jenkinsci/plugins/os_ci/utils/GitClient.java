package org.jenkinsci.plugins.os_ci.utils;


import com.google.common.base.Joiner;
import hudson.model.BuildListener;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.*;
import java.util.Iterator;

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
 */public class GitClient {

    private final BuildListener listener;
    private final Git git;
    private final String branch;
    private final String MASTER_BRANCH = "master";

    public GitClient(String gitFolderName, String puppetRepoUrl, String branch, String fullName, BuildListener listener) throws IOException {
        this.listener = listener;
        this.branch = branch;
        final File gitFolder = new File(Joiner.on(File.separator).join(gitFolderName, fullName));
        if (gitFolder == null)
            throw new IOException("gitFolder is null");
        try {
            gitFolder.mkdir();
            git = Git.cloneRepository().setURI(puppetRepoUrl).setDirectory(gitFolder).call();
            if (git == null)
                throw new IOException("git is null");
            if (branch == null || branch.isEmpty())
                throw new IOException("branch is not defined");
            if (!branch.equalsIgnoreCase(MASTER_BRANCH))
                git.checkout().setName("origin/" + branch).call();
        } catch (InvalidRemoteException e) {
            throw new IOException(e.getMessage());
        } catch (TransportException e) {
            throw new IOException(e.getMessage());
        } catch (GitAPIException e) {
            throw new IOException(e.getMessage());
        }
    }

    public String getCurrentRevisionCommit() {
        try {
            final LogCommand logCommand = git.log();
            if (logCommand == null) {
                LogUtils.log(listener, "DEBUG: logCommand is null, return null");
                return null;
            }
            final Iterable<RevCommit> commitIterable = logCommand.call();
            if (commitIterable == null) {
                LogUtils.log(listener, "DEBUG: commitIterable is null, return null");
                return null;
            }
            final Iterator<RevCommit> revCommitIterator = commitIterable.iterator();
            if (revCommitIterator == null || !revCommitIterator.hasNext()) {
                LogUtils.log(listener, "DEBUG: revCommitIterator is null or does not have next value, return null");
                return null;
            }
            return revCommitIterator.next().toString();
        } catch (GitAPIException e) {
            LogUtils.log(listener, "DEBUG: " + e.getMessage() + " ,return null");
            return null;
        }
    }

    public static void archive() throws GitAPIException {
         String localPath, remotePath;
         Repository localRepo;
         Git git;

        try {
            localPath = "C:\\Users\\agrosmar\\git\\gitTry";
//            remotePath = "git@github.com:me/mytestrepo.git";
            localRepo = new FileRepository(localPath + "/.git");
            git = new Git(localRepo);
//            "http://gitlab.cisco.com/control-plane-ci/deployment-scripts/blob/master/GLOBAL




            FileOutputStream out   = new FileOutputStream(new File("c://git.zip"));
            git.archive().setTree(localRepo.resolve("")).setFormat("zip").setOutputStream(out).call();

        } catch (Exception e) {
            e.printStackTrace();
        }
//        ObjectId tree = null;



    }
}
