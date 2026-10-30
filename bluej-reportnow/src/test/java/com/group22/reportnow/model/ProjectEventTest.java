package com.group22.reportnow.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ProjectEventTest {
    @Test
    void projectCreationEventStoresCorrectDetails() {

        ProjectEvent event = new ProjectEvent(
            1000L, "ProjectCreated", "TestProject"
        );

        assertEquals("project", event.getType());
        assertEquals("ProjectCreated", event.getAction());
        assertEquals("TestProject", event.getProjectName());
    }

    @Test
    void projectModificationEventStoresCorrectDetails() {

        ProjectEvent event = new ProjectEvent(
            2000L, "ProjectModified", "TestProject"
        );

        assertEquals("project", event.getType());
        assertEquals("ProjectModified", event.getAction());
        assertEquals("TestProject", event.getProjectName());
    }

    @Test
    void projectDeletionEventStoresCorrectDetails() {

        ProjectEvent event = new ProjectEvent(
            3000L, "ProjectDeleted", "TestProject"
        );

        assertEquals("project", event.getType());
        assertEquals("ProjectDeleted", event.getAction());
        assertEquals("TestProject", event.getProjectName());
    }

    @Test 
    void projectOpeningEventStoresCorrectDetails(){
        ProjectEvent event = new ProjectEvent(4000L, "ProjectOpened", "TestProject");

        assertEquals("project", event.getType());
        assertEquals("ProjectOpened", event.getAction());
        assertEquals("TestProject", event.getProjectName());
    }

    @Test 
    void projectEventStoresCorrectTimestamp(){
        ProjectEvent event = new ProjectEvent(5000L, "ProjectCreated", "TestProject");

        assertEquals(5000L, event.getTimestampMs());
    }
}
