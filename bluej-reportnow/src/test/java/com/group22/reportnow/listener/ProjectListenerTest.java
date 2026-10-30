package com.group22.reportnow.listener;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Consumer;

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
    private Consumer<String> mockCallback;

    @BeforeEach
    void setup() throws Exception{
        repo = new TelemetryRepository();
        blueJ = mock(BlueJ.class);
        classListener = mock(ClassLifecycleListener.class);
        
        @SuppressWarnings("unchecked")
        Consumer<String> callback = mock(Consumer.class);
        this.mockCallback = callback;

        listener = new ProjectLifecycleListener(blueJ, repo, classListener, mockCallback);

        testFolder = Files.createTempDirectory("testProject").toFile();
    }

    @AfterEach 
    void deleteFiles() {
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

    private void closeProject() throws Exception {
        BProject project = mock(BProject.class);
        when(project.getName()).thenReturn("TestProject");

        BPackage bluejPackage = mock(BPackage.class);
        when(bluejPackage.getProject()).thenReturn(project);

        PackageEvent event = mock(PackageEvent.class);
        when(event.getPackage()).thenReturn(bluejPackage);

        listener.packageClosing(event);
    }


    @Test
    void projectCreationIsRecorded() throws Exception {
        openProject();

        List<ProjectEvent> events = repo.getProjectEvents();
        boolean found = false;

        for (ProjectEvent event : events) {
            if (event.getAction().equals("ProjectCreated")) {
                assertEquals("TestProject", event.getProjectName());
                found = true;
            }
        }

        assertTrue(found);
    }

    @Test 
    void projectOpeningIsRecorded() throws Exception{
        openProject();

        List<ProjectEvent> events = repo.getProjectEvents();
        boolean found = false;

        for(ProjectEvent event : events){
            if(event.getAction().equals("ProjectOpened")){
                assertEquals("TestProject", event.getProjectName());
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
    void projectDeletionIsRecorded() throws Exception{
        openProject();

        assertTrue(testFolder.delete());
        listener.checkProjects();

        List<ProjectEvent> events = repo.getProjectEvents();
        boolean found = false;

        for(ProjectEvent event : events){
            if(event.getAction().equals("ProjectDeleted")){
                assertEquals("TestProject", event.getProjectName());
                found = true;
            }
        }
        
        assertTrue(found);
    }
  
    @Test
    void projectClosingTriggersExportCallback() throws Exception {
        closeProject();
        
        verify(mockCallback, times(1)).accept("TestProject");
    }
}