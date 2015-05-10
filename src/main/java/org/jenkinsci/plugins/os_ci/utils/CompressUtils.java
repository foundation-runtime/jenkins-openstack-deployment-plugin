package org.jenkinsci.plugins.os_ci.utils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import static org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_POSIX;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
 */public class CompressUtils {

    public static void tarGzDirectory(String baseDir, String targetFile) throws FileNotFoundException, IOException {
        FileOutputStream fOut = null;
        BufferedOutputStream bOut = null;
        TarArchiveOutputStream tOut = null;
        GzipCompressorOutputStream gzOut = null;
        try {
            System.out.println(new File(".").getAbsolutePath());
            fOut = new FileOutputStream(new File(targetFile));
            bOut = new BufferedOutputStream(fOut);
            gzOut = new GzipCompressorOutputStream(bOut);
            tOut = new TarArchiveOutputStream(gzOut);
            tOut.setLongFileMode(LONGFILE_POSIX);
            addFileToTarGz(tOut, baseDir, "");
        } finally {
            if (tOut != null) {
                tOut.finish();
                tOut.close();
            }
            if (gzOut != null) {
                gzOut.close();
            }
            if (bOut != null) {
                bOut.close();
            }
            if (fOut != null) {
                fOut.close();
            }
        }


    }

    private static void addFileToTarGz(TarArchiveOutputStream tOut, String path, String base) throws IOException {
        File f = new File(path);
        String entryName = base + f.getName();
        TarArchiveEntry tarEntry = new TarArchiveEntry(f, entryName);
        tOut.putArchiveEntry(tarEntry);

        if (f.isFile()) {
            IOUtils.copy(new FileInputStream(f), tOut);
            tOut.closeArchiveEntry();
        } else {
            tOut.closeArchiveEntry();
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) {
                    addFileToTarGz(tOut, child.getAbsolutePath(), entryName + "/");
                }
            }
        }
    }

    public static void unZip(String zipFile, String outputFolder) throws IOException {

        byte[] buffer = new byte[1024];

        //create output directory is not exists
        File folder = new File(outputFolder);
        if (!folder.exists()) {
            folder.mkdir();
        }

        //get the zip file content
        ZipInputStream zis =
                new ZipInputStream(new FileInputStream(zipFile));
        //get the zipped file list entry
        ZipEntry ze = zis.getNextEntry();

        while (ze != null) {

            String fileName = ze.getName();
            File newFile = new File(outputFolder + File.separator + fileName);

            //create all non exists folders
            //else you will hit FileNotFoundException for compressed folder
            if (ze.isDirectory())
                newFile.mkdirs();
            else {
                newFile.getParentFile().mkdirs();

                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
            }
            ze = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();

    }

    public static void untarFile(File file) throws IOException {
        FileInputStream fileInputStream = null;
        String currentDir = file.getParent();
        try {
            fileInputStream = new FileInputStream(file);
            GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);
            TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(gzipInputStream);

            TarArchiveEntry tarArchiveEntry;

            while (null != (tarArchiveEntry = tarArchiveInputStream.getNextTarEntry())) {
                if (tarArchiveEntry.isDirectory()) {
                    FileUtils.forceMkdir(new File(currentDir + File.separator + tarArchiveEntry.getName()));
                } else {
                    byte[] content = new byte[(int) tarArchiveEntry.getSize()];
                    int offset = 0;
                    tarArchiveInputStream.read(content, offset, content.length - offset);
                    FileOutputStream outputFile = new FileOutputStream(currentDir + File.separator + tarArchiveEntry.getName());
                    org.apache.commons.io.IOUtils.write(content, outputFile);
                    outputFile.close();
                }
            }
        } catch (FileNotFoundException e) {
            throw new IOException(e.getStackTrace().toString());
        }
    }
}