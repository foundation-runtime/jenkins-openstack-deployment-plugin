package org.jenkinsci.plugins.os_ci.utils;

import org.junit.Test;

import java.io.IOException;

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
public class ExecUtilsTest {
    @Test
    public void executeCommandTest() {
        String privkey = "-----BEGIN RSA PRIVATE KEY-----\n" +
                "MIIEoQIBAAKCAQEAo+9WVCYGd/UkPOb7ABNgellI0O+LnCz1e1EdI8J4U6HcBsez\n" +
                "Hdu7hgdGnCfXiqMhQmBIswOqSBdIuciEh1zn7h+JbjyGdxFDeX2IJY6ZRho+23fr\n" +
                "wfH9biEMzmoCGzUCVlo9SDNFJAeQPfuLVfqksGGlYI8AEc1gz0su1fqioPTiB/0T\n" +
                "L7XgO4SESWvAGrwJlonKCSu3wzwbH2mJpvlgBYUHvAJgq5qxBOxB+Q6Sh1pQ60vv\n" +
                "brFIy8H1DMcvAoOGt/N5vEdKIwpdrba2Mlx0WdZyhapmC01ek8FHfs7sqExzz3pU\n" +
                "IbS7csh8V/hX54Ag1GMG5Otd6Oi4EUarpBoP6wIBIwKCAQBY/kTOmEynhROsCFxf\n" +
                "IHYzyhGV/mG7LllgM1j2J72pvkQ+MeTkUrZBcan8/6+FxkVBSjYYCU3PXRjZ4eGL\n" +
                "T7Ea3euE/Ej+ztt8d1iJaqr6K4E1T7p/OjkehvhSy9VB+DR4BRnz/pM/cdH1wxEg\n" +
                "C7h9Ac7OBHxhb32yXAN7eW46HaqUMTR/01F3JgRkfu98HhHh0yyjEFj3cGWbGXE1\n" +
                "LJwrvei2G4HDkbBQbgJwByQR6ZpEQG6+LeyvwPm484UURnR9dooXLCi8hylBuxt9\n" +
                "WA8FY2LDJDCECgfZbosiBacZ2xdty9u5Bve2o8UUb/OamsUU4K1OScQyCMsXV+zA\n" +
                "zxSjAoGBANhkZB3zEo9QOPI0SmAgLsPILwbZcxy1fMc9cWKa+k7iItuO/28W1/Vt\n" +
                "Kke4Y28QWEHDqS5Xh45nOQn5jIfMCfSs+1hPcYG2m8x9VLa4Z+9sEncqU8ofla08\n" +
                "AYlnKCWWKnWU5Je/P5OQ49Z7lr5Fl7r0TyYANRcA7wIkRTdfw9j1AoGBAMHw7XmU\n" +
                "idXMcVQYo1X695Cnn9y/+M6/M6YwDKdZM6Cc6WSMtVNiZYRG91QT/EzPRkPSVHvT\n" +
                "x6gTTduxDkZHBUIxU6nI4sDATSo+CVOTDu/+yYjLckKMsrBbbV96qdafIp0s9clQ\n" +
                "/If9PbQVDKL+X4GRMqGlVtAFEe735jln5jlfAoGAEoxDGIKMj/Du8DBeJX8Z84Yv\n" +
                "6qTsnA+OWjh3btoVdHnIeTgkhd1i98eHR/ncox6oeqpe9Vf1rR7KX/DRh/uL0yTF\n" +
                "FjKx9SzoyGKDmqIXiYzro9BtlPtkmdHxgM5T5fbtsk6XQDT20iJcq/v2+l28jF4V\n" +
                "aag/EJmuFiBdtEoJeP8CgYBeMzGEMjRR5vU3ea5rlyfJ6wSBKgsil2I6xuGTHLKt\n" +
                "GQOl3fGsKHpzcu0o1oHcKilxZikI8dYBLfKd6EjDDI2GjPzALl+CMYSQ3E2sQB0y\n" +
                "vZUWjrPXLmVrmh8uYCabcd2czLHlcHqoc7BBhpEb9+U+7sDSK7xzqrDwaceYOceY\n" +
                "NQKBgQCXERsGl6FkUexHt06lZhcvPLYNsrkruozf+dHNc79/zksAd7Me/pRluIX3\n" +
                "q9LO8n5tRlPlQ/h/LU659DjaJYwcmm66/7MefdCUWdA7Tlwrm8OWyiHSkCp97cS2\n" +
                "Xv/VXcGIyiebMH57mywcW0ppDBqKK/r7dam9SCAxZ21Bd9uh1w==\n" +
                "-----END RSA PRIVATE KEY-----";
        String cwd = "/var/www/html/build/latest/repo";
        String commands[] = { "cd "+cwd, "createrepo .", "pakrat --name XXXXX", "rm -f *.rpm"};
        try {
            ExecUtils.executeRemoteCommand(null, commands, "10.56.167.156", privkey);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
