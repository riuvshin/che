/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.selenium.editor.autocomplete;

import static org.eclipse.che.selenium.pageobject.CodenvyEditor.MarkerLocator.ERROR;
import static org.eclipse.che.selenium.pageobject.CodenvyEditor.MarkerLocator.TASK_OVERVIEW;
import static org.testng.Assert.fail;

import com.google.inject.Inject;
import java.net.URL;
import java.nio.file.Paths;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.selenium.core.client.TestProjectServiceClient;
import org.eclipse.che.selenium.core.project.ProjectTemplates;
import org.eclipse.che.selenium.core.workspace.TestWorkspace;
import org.eclipse.che.selenium.pageobject.CodenvyEditor;
import org.eclipse.che.selenium.pageobject.CodenvyEditor.MarkerLocator;
import org.eclipse.che.selenium.pageobject.Ide;
import org.eclipse.che.selenium.pageobject.Loader;
import org.eclipse.che.selenium.pageobject.MavenPluginStatusBar;
import org.eclipse.che.selenium.pageobject.ProjectExplorer;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** @author Musienko Maxim */
public class AutocompleteWithInheritTest {
  private static final String PROJECT_NAME =
      NameGenerator.generate(AutocompleteWithInheritTest.class.getSimpleName(), 4);
  private static final String BASE_CLASS = "AppController";
  private static final String EXTENDED_CLASS = "InheritClass";
  private static final Logger LOG = LoggerFactory.getLogger(AutocompleteWithInheritTest.class);

  private static final String contentAfterFix =
      "public class InheritClass extends AppController {\n"
          + "\n"
          + "    public InheritClass(int testInt, String testString) {\n"
          + "        super(testInt, testString);\n"
          + "        // TODO Auto-generated constructor stub\n"
          + "    }\n"
          + "\n"
          + "}\n";

  @Inject private TestWorkspace workspace;
  @Inject private Ide ide;
  @Inject private ProjectExplorer projectExplorer;
  @Inject private Loader loader;
  @Inject private CodenvyEditor editor;
  @Inject private MavenPluginStatusBar mavenPluginStatusBar;
  @Inject private TestProjectServiceClient testProjectServiceClient;

  @BeforeClass
  public void prepare() throws Exception {
    URL resource = getClass().getResource("/projects/default-dependency-test");
    testProjectServiceClient.importProject(
        workspace.getId(),
        Paths.get(resource.toURI()),
        PROJECT_NAME,
        ProjectTemplates.MAVEN_SPRING);

    ide.open(workspace);
  }

  @Test
  public void updateDependencyWithInheritTest() throws Exception {
    projectExplorer.waitProjectExplorer();
    projectExplorer.waitItem(PROJECT_NAME);
    mavenPluginStatusBar.waitClosingInfoPanel();
    projectExplorer.expandPathInProjectExplorerAndOpenFile(
        PROJECT_NAME + "/src/main/java/example", BASE_CLASS + ".java");
    editor.waitAllMarkersInvisibility(ERROR);
    projectExplorer.openItemByVisibleNameInExplorer(EXTENDED_CLASS + ".java");
    waitErrorMarkerInPosition();
    editor.setCursorToLine(13);
    editor.launchPropositionAssistPanel();
    editor.waitTextIntoFixErrorProposition("Add constructor 'InheritClass(int,String)'");
    editor.selectFirstItemIntoFixErrorPropByEnter();
    editor.waitTextIntoEditor(contentAfterFix);
    editor.waitMarkerInvisibility(ERROR, 13);
    editor.waitMarkerInPosition(TASK_OVERVIEW, 18);
    editor.waitTabFileWithSavedStatus(EXTENDED_CLASS);
    editor.selectTabByName(BASE_CLASS);
    loader.waitOnClosed();
    editor.selectLineAndDelete(24);
    editor.typeTextIntoEditor("int testString;");
    editor.typeTextIntoEditor(Keys.ARROW_DOWN.toString());
    editor.typeTextIntoEditor(Keys.ARROW_DOWN.toString());
    editor.selectLineAndDelete();
    editor.typeTextIntoEditor("public AppController(int testInt, int testString) {");
    editor.typeTextIntoEditor(Keys.DELETE.toString());
    editor.waitTabFileWithSavedStatus(BASE_CLASS);
    editor.selectTabByName(EXTENDED_CLASS);
    loader.waitOnClosed();
    editor.setCursorToLine(16);
    editor.typeTextIntoEditor(Keys.END.toString());
    editor.typeTextIntoEditor(Keys.HOME.toString());
    editor.typeTextIntoEditor(Keys.DELETE.toString());
    editor.typeTextIntoEditor("s");
    editor.launchPropositionAssistPanel();
    editor.waitTextIntoFixErrorProposition("Change type of 'testString' to 'int'");
    editor.selectFirstItemIntoFixErrorPropByDoubleClick();
    editor.waitAllMarkersInvisibility(ERROR);
  }

  private void waitErrorMarkerInPosition() throws Exception {
    try {
      editor.waitMarkerInPosition(MarkerLocator.ERROR, 13);
    } catch (TimeoutException ex) {
      logExternalLibraries();
      logProjectTypeChecking();
      logProjectLanguageChecking();

      // remove try-catch block after issue has been resolved
      fail("Known issue https://github.com/eclipse/che/issues/7161", ex);
    }
  }

  private void logExternalLibraries() throws Exception {
    testProjectServiceClient
        .getExternalLibraries(workspace.getId(), PROJECT_NAME)
        .forEach(library -> LOG.info("project external library:  {}", library));
  }

  private void logProjectTypeChecking() throws Exception {
    LOG.info(
        "Project type of the {} project is \"maven\" - {}",
        PROJECT_NAME,
        testProjectServiceClient.checkProjectType(workspace.getId(), PROJECT_NAME, "maven"));
  }

  private void logProjectLanguageChecking() throws Exception {
    LOG.info(
        "Project language of the {} project is \"java\" - {}",
        PROJECT_NAME,
        testProjectServiceClient.checkProjectLanguage(workspace.getId(), PROJECT_NAME, "java"));
  }
}
