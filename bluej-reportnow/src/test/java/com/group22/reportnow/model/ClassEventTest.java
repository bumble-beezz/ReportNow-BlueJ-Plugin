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

    @Test 
    void classEventStoresCorrectTimestamp(){
        ClassEvent event = new ClassEvent(4000L,"ClassCreated", "Test.java", "TestProject");
        assertEquals(4000L, event.getTimestampMs());
    }

    @Test 
    void classEventStoresCorrectNames(){
        ClassEvent event = new ClassEvent(5000L, "ClassCreated", "Test.java", "TestProject");
        assertEquals("Test.java", event.getClassName());
        assertEquals("TestProject", event.getProjectName());
    }
}
