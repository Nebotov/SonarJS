/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011 SonarSource and Eriks Nukis
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.javascript.unittest.jstestdriver;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jfree.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.javascript.JavaScriptPlugin;
import org.sonar.plugins.javascript.core.JavaScript;
import org.sonar.plugins.javascript.unittest.surefireparser.AbstractSurefireParser;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class JsTestDriverSensor implements Sensor {

  protected JavaScript javascript;

  public JsTestDriverSensor(JavaScript javascript) {
    this.javascript = javascript;
  }

  private static final Logger LOG = LoggerFactory.getLogger(JsTestDriverSensor.class);

  public boolean shouldExecuteOnProject(Project project) {
    return javascript.equals(project.getLanguage())
      && StringUtils.isNotBlank(javascript.getSettings().getString(JavaScriptPlugin.JSTESTDRIVER_REPORTS_PATH));
  }

  public void analyse(Project project, SensorContext context) {
    String jsTestDriverFolder = javascript.getSettings().getString(JavaScriptPlugin.JSTESTDRIVER_REPORTS_PATH);
    collect(project, context, project.getFileSystem().resolvePath(jsTestDriverFolder));
  }

  protected void collect(final Project project, final SensorContext context, File reportsDir) {
    LOG.debug("Parsing JsTestDriver run results in Surefile format from folder {}", reportsDir);

    new AbstractSurefireParser() {

      @Override
      protected Resource<?> getUnitTestResource(String classKey) {

        org.sonar.api.resources.File unitTestFileResource = getUnitTestFileResource(classKey);
        unitTestFileResource.setLanguage(javascript);
        unitTestFileResource.setQualifier(Qualifiers.UNIT_TEST_FILE);

        LOG.debug("Adding unittest resource: {}", unitTestFileResource.toString());

        List<File> testDirectories = project.getFileSystem().getTestDirs();

        File unitTestFile = getUnitTestFile(testDirectories, getUnitTestFileName(classKey));

        String source;
        try {
          source = FileUtils.readFileToString(unitTestFile, project.getFileSystem().getSourceCharset().name());
        } catch (IOException e) {
          source = "Could not find source for unit test: " + classKey + " in any of test directories";
          Log.debug(source, e);
        }

        context.saveSource(unitTestFileResource, source);

        return unitTestFileResource;
      }
    }.collect(project, context, reportsDir);

  }

  protected org.sonar.api.resources.File getUnitTestFileResource(String classKey) {
    // For JsTestDriver assume notation com.company.MyJsTest that maps to com/company/MyJsTest.js
    return new org.sonar.api.resources.File(classKey.replaceAll("\\.", "/") + ".js");
  }

  protected String getUnitTestFileName(String className) {
    String fileName = className.substring((className.indexOf('.') + 1));
    fileName = fileName.replace('.', '/');
    fileName = fileName + ".js";
    return fileName;
  }

  protected File getUnitTestFile(List<File> testDirectories, String name) {
    File unitTestFile = new File("");
    for (File dir : testDirectories) {
      unitTestFile = new File(dir, name);

      if (unitTestFile.exists()) {
        break;
      }
    }
    return unitTestFile;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
