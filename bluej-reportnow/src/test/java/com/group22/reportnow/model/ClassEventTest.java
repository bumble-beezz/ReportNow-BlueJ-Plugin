package com.group22.reportnow.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ClassEventTest {
    @Test
    void classCreationEventStoresCorrectDetails() {

        ClassEvent event = new ClassEvent(
            1000L, "ClassCreated", "Student.java", "TestProject"
        );

        assertEquals("class", event.getType());
        assertEquals("ClassCreated", event.getAction());
        assertEquals("Student.java", event.getClassName());
        assertEquals("TestProject", event.getProjectName());
    }

    @Test
    void classModificationEventStoresCorrectDetails() {

        ClassEvent event = new ClassEvent(
            2000L, "ClassModified", "Student.java", "TestProject"
        );

        assertEquals("class", event.getType());
        assertEquals("ClassModified", event.getAction());
        assertEquals("Student.java", event.getClassName());
        assertEquals("TestProject", event.getProjectName());
    }

    @Test
    void classDeletionEventStoresCorrectDetails() {

        ClassEvent event = new ClassEvent(
            3000L, "ClassDeleted", "Student.java", "TestProject"
        );

        assertEquals("class", event.getType());
        assertEquals("ClassDeleted", event.getAction());
        assertEquals("Student.java", event.getClassName());
        assertEquals("TestProject", event.getProjectName());
    }

}
