package org.jenkinsci.plugins.os_ci.utils;

import java.io.*;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
 */public class FtpUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(FtpUtils.class);


    private static byte[] readFile(File file) throws IOException {
        RandomAccessFile f = new RandomAccessFile(file, "r");
        try {

            long longlength = f.length();
            int length = (int) longlength;
            if (length != longlength) {
                throw new IOException("File size >= 2 GB");
            }
            byte[] data = new byte[length];
            f.readFully(data);
            return data;
        } finally {
            f.close();
        }
    }


    /**
     * Copy a folder and its sub folders into a remote path
     *
     * @param localFolderPathFrom the folder to copy
     * @param RemoteFolderPathTo   the path where the folder will be located
     * @param remoteHost          the host to connect to
     * @param privateKeyPath      the private key that should be used for connecting to the remote host
     * @throws IOException
     */
    public static void copyLocalFolderToRemote(String localFolderPathFrom, String RemoteFolderPathTo, String remoteHost, String privateKeyPath) throws IOException{
        copyLocalFolderToRemote(localFolderPathFrom, RemoteFolderPathTo, remoteHost, privateKeyPath, false);
    }


    /**
     * Copy a folder and its sub folders into a remote path
     *
     * @param localFolderPathFrom the folder to copy
     * @param RemoteFolderPathTo   the path where the folder will be located
     * @param remoteHost          the host to connect to
     * @param privateKeyPath      the private key that should be used for connecting to the remote host
     * @param copyDots            if it is true - folders that starts with "." will  be copied
     * @throws IOException
     */
    public static void copyLocalFolderToRemote(String localFolderPathFrom, String RemoteFolderPathTo, String remoteHost, String privateKeyPath, boolean copyDots) throws IOException {

        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp sftpChannel = null;
        LOGGER.trace("copying entire folder:" + localFolderPathFrom + " to: " + RemoteFolderPathTo + " on host:" + remoteHost);
        try {
            final byte[] emptyPassPhrase = new byte[0];
            jsch.addIdentity("",
                    readFile(new File(privateKeyPath)),
                    null,
                    emptyPassPhrase
            );
            session = jsch.getSession("root", remoteHost, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            Channel channel = session.openChannel("sftp");
            channel.connect();
            sftpChannel = (ChannelSftp) channel;
            createDirRecursive(sftpChannel, RemoteFolderPathTo, "/");
            copyFolder(new File(localFolderPathFrom), "/", RemoteFolderPathTo, remoteHost, sftpChannel, copyDots);

        } catch (JSchException e) {
            throw new IOException(e.getMessage());
        } catch (SftpException e) {
            throw new IOException(e.getMessage());
        } catch (FileNotFoundException e) {
            throw new IOException(e.getMessage());
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        } finally {
            if (sftpChannel != null)
                sftpChannel.exit();
            if (session != null)
                session.disconnect();
        }
    }


    private static void copyFolder(File folder, String relative, String RemoteFolderPathTo, String remoteHost, ChannelSftp sftpChannel, boolean copyDots) throws FileNotFoundException, SftpException {
        if (folder.isDirectory()) {
            if (!copyDots && folder.getName().startsWith(".")) {
                return;
            }
            String dir = RemoteFolderPathTo + relative + folder.getName();
            try {
                sftpChannel.mkdir(dir);
            } catch (SftpException e) {
                // swallow exception - legitimate if dir exists.
                LOGGER.debug("failed creating folder", e);
            }

            for (File file : folder.listFiles()) {
                if (file.isDirectory()) {
                    //recursively call to copy this folder
                    copyFolder(file, relative + folder.getName() + "/", RemoteFolderPathTo, remoteHost, sftpChannel, copyDots);
                } else {
                    if (copyDots || !file.getName().startsWith(".")) {
                        //copy the file:
                        sftpChannel.cd(RemoteFolderPathTo + relative + folder.getName());
                        sftpChannel.put(new FileInputStream(file), file.getName());
                    }
                }

            }

        }

    }

    private static void createDirRecursive(ChannelSftp sftpChannel, String dir, String sep) {

        String d[] = dir.split(sep);
        StringBuilder path = new StringBuilder();
        if (d.length == 0)
            return;
        path.append(d[0]);
        for (int i=1; i<d.length; i++) {
            path.append(sep).append(d[i]);
            try {
                sftpChannel.mkdir(path.toString());
            } catch (SftpException e) {
                // swallow exception - legitimate if dir exists.
                LOGGER.debug("failed creating folder", e);
            }
        }
    }

    /**
     * Copy a file to remote location
     *
     * @param localFile      the file to copy
     * @param remoteHost     the host to copy the file to
     * @param remotePath     the remote folder where the file will be entered
     * @param privateKeyPath the path for local file that is the private key for connecting to this machine
     * @throws IOException
     */
    public static void copyFileToRemote(String localFile, String remoteHost, String remotePath, String privateKeyPath) throws IOException {
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp sftpChannel = null;
        LOGGER.trace("copying file:" + localFile + " to: " + remotePath + " on host:" + remoteHost);
        try {
            final byte[] emptyPassPhrase = new byte[0];
            jsch.addIdentity("",
                    readFile(new File(privateKeyPath)),
                    null,
                    emptyPassPhrase
            );
            session = jsch.getSession("root", remoteHost, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            Channel channel = session.openChannel("sftp");
            channel.connect();
            sftpChannel = (ChannelSftp) channel;
            sftpChannel.cd(remotePath);
            File file = new File(localFile);
            sftpChannel.put(new FileInputStream(file), file.getName());

        } catch (JSchException e) {
            throw new IOException(e.getMessage());
        } catch (SftpException e) {
            throw new IOException(e.getMessage());
        } catch (FileNotFoundException e) {
            throw new IOException(e.getMessage());
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        } finally {
            sftpChannel.exit();
            session.disconnect();
        }

    }
    public static void createRemoteSymLink(String sourcePath, String targetPath, String remoteHost, String privateKey ) throws IOException {

        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp sftpChannel = null;
        try {
            final byte[] emptyPassPhrase = new byte[0];
            jsch.addIdentity("",
                    privateKey.getBytes(),
                    null,
                    emptyPassPhrase
            );
            session = jsch.getSession("root", remoteHost, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            Channel channel = session.openChannel("sftp");
            channel.connect();
            sftpChannel = (ChannelSftp) channel;
            try {
                sftpChannel.rm(targetPath);
            } catch (SftpException e) {
                // swallow - ok, if file doesn't exists.
            }
            sftpChannel.symlink(sourcePath, targetPath);

        } catch (JSchException e) {
            throw new IOException(e.getMessage());
        } catch (SftpException e) {
            throw new IOException(e.getMessage());
        } finally {
            if (sftpChannel != null)
                sftpChannel.exit();
            if (session != null)
                session.disconnect();
        }
    }



}