package com.group22.reportnow.listener;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import bluej.extensions2.BlueJ;
import bluej.extensions2.BProject;
import bluej.extensions2.BPackage;
import bluej.extensions2.event.PackageEvent;

import com.group22.reportnow.model.ProjectEvent;
import com.group22.reportnow.repository.TelemetryRepository;


public class ProjectListenerTest {
    private TelemetryRepository repo;
    private ProjectLifecycleListener listener;
    private ClassLifecycleListener classListener;
    private BlueJ blueJ;
    private File testFolder;

    @BeforeEach
    void setup() throws Exception{
        repo = new TelemetryRepository();
        blueJ = mock(BlueJ.class);
        classListener = mock(ClassLifecycleListener.class);
        listener = new ProjectLifecycleListener(blueJ, repo, classListener);

        testFolder = Files.createTempDirectory("testProject").toFile();
    }

    @AfterEach 
    void tearDown() {
        if (testFolder != null && testFolder.exists()) {
            assertTrue(testFolder.delete());
        }
    }

    private void openProject() throws Exception {
        BProject project = mock(BProject.class);
        when(project.getName()).thenReturn("TestProject");
        when(project.getDir()).thenReturn(testFolder);

        BPackage bluejPackage = mock(BPackage.class);
        when(bluejPackage.getProject()).thenReturn(project);

        PackageEvent event = mock(PackageEvent.class);
        when(event.getPackage()).thenReturn(bluejPackage);

        listener.packageOpened(event);
    }

    private void checkProjects() throws Exception{
        Method method = ProjectLifecycleListener.class.getDeclaredMethod("checkProjects");
        method.setAccessible(true);
        method.invoke(listener);
    }

    @Test
     void projectOpeningIsRecorded() throws Exception {
        openProject();

        List<ProjectEvent> events = repo.getProjectEvents();
        boolean found = false;

        for (ProjectEvent event : events) {
            if (event.getAction().equals("ProjectOpened")) {
                found = true;
            }
        }

        assertTrue(found);
    }

    @Test
     void projectModificationIsRecorded() {
        listener.recordProjectModified("TestProject");

        List<ProjectEvent> events = repo.getProjectEvents();

        assertEquals(1, events.size());
        assertEquals("ProjectModified", events.get(0).getAction());
        assertEquals("TestProject", events.get(0).getProjectName());
    }

    @Test 
    void missingProjectFolderRecordsDeletion() throws Exception {
        openProject();
        assertTrue(testFolder.delete());
        checkProjects();

        List<ProjectEvent> events = repo.getProjectEvents();
        boolean foundDeletion = false;

        for (ProjectEvent event : events) {
            if (event.getAction().equals("ProjectDeleted")) {
                foundDeletion = true;
            }
        }
        assertTrue(foundDeletion);
    }

    @Test 
    void projectOpeningIsNotRecordedTwice() throws Exception {
        openProject();
        openProject(); 

        List<ProjectEvent> events = repo.getProjectEvents();
        int count = 0;
        for (ProjectEvent event : events) {
            if (event.getAction().equals("ProjectOpened")) {
                count++;
            }
        }
        assertEquals(1, count); 
    }

    @Test 
    void projectNameIsRecordedCorrectly() throws Exception {
        openProject();

        List<ProjectEvent> events = repo.getProjectEvents();
        boolean found = false;

        for (ProjectEvent event : events) {
            if (event.getAction().equals("ProjectOpened")) {
                assertEquals("TestProject", event.getProjectName());
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test 
    void projectCreationIsRecorded() throws Exception {
        openProject();

        List<ProjectEvent> events = repo.getProjectEvents();
        boolean foundCreation = false;

        for (ProjectEvent event : events) {
            if (event.getAction().equals("ProjectCreated")) {
                foundCreation = true;
            }
        }
        assertTrue(foundCreation);
    }
}
