package org.jenkinsci.plugins.os_ci.utils;

import com.google.common.base.Joiner;
import com.jcraft.jsch.*;
import hudson.model.BuildListener;
import org.jenkinsci.plugins.os_ci.exceptions.OsCiPluginException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

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
 */public class ExecUtils {
    public static void executeRemoteCommand(BuildListener listener, String[] commands, String remoteHost, String privateKey ) throws IOException {

        JSch jsch = new JSch();
        Session session = null;
        ChannelExec execChannel = null;
        String cmds = Joiner.on(";").join(commands);
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
            Channel channel = session.openChannel("exec");
            execChannel = (ChannelExec) channel;

            // Execute
            LogUtils.log(listener, "Executing command: " + cmds);
            execChannel.setCommand(cmds);
            execChannel.setInputStream(null);
            execChannel.setErrStream(System.err);
            channel.connect();

            InputStream in = channel.getInputStream();
            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    String msg = new String(tmp, 0, i);
                    System.out.print(msg);
                    LogUtils.log(listener, msg);
                }
                if (channel.isClosed()) {
                    if (in.available() > 0) continue;
                    System.out.println("exit-status: " + channel.getExitStatus());
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                }
            }
            if (execChannel != null)
                execChannel.disconnect();

        } catch (JSchException e) {
            LogUtils.log(listener, "Error executing command" + e.getMessage() + "\n" + e.getStackTrace());
            throw new IOException(e.getMessage());
        } finally {
            if (execChannel != null)
                execChannel.disconnect();
            if (session != null)
                session.disconnect();
        }
    }

    public static int executeLocalCommand(String cmd, String folder)throws InterruptedException{
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd.split(" "));
            pb.directory(new File(folder));
            process = pb.start();
            return process.waitFor();
        } catch (IOException e) {
            throw new OsCiPluginException(e.getMessage() + " failed running: " + cmd + " on folder: " + folder);
        } finally {
            if (process != null)
                process.destroy();
        }
    }

}
