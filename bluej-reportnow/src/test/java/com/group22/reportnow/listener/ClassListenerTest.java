package com.group22.reportnow.listener;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.group22.reportnow.model.ClassEvent;
import com.group22.reportnow.model.ProjectEvent;
import com.group22.reportnow.repository.TelemetryRepository;

public class ClassListenerTest {
    private TelemetryRepository repo;
    private ClassLifecycleListener listener;
    private File testFolder;

    @BeforeEach 
    void setup() throws Exception{
        repo = new TelemetryRepository();
        listener = new ClassLifecycleListener(repo);

        testFolder = Files.createTempDirectory("testProject").toFile();
        
    }

    @AfterEach 
    void deleteFiles() {
        File[] files = testFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                assertTrue(file.delete());
            }
        }

        if (testFolder != null && testFolder.exists()) {
            assertTrue(testFolder.delete());
        }
    }

    @Test 
    void classCreationIsRecorded() throws Exception {
        listener.trackProject(testFolder, "TestProject");

        File classFile = new File(testFolder, "Student.java");
        assertTrue(classFile.createNewFile());
        listener.checkFiles();

        List<ClassEvent> classEvents = repo.getClassEvents();
        
        assertEquals(1, classEvents.size());
        assertEquals("ClassCreated", classEvents.get(0).getAction());
        assertEquals("Student.java", classEvents.get(0).getClassName());
        assertEquals("TestProject", classEvents.get(0).getProjectName());
        
    }

    @Test 
    void classModificationIsRecorded() throws Exception {
        File classFile = new File(testFolder, "Student.java");
        Files.writeString(classFile.toPath(), "public class Student {}");

        listener.trackProject(testFolder, "TestProject");

        Files.writeString(classFile.toPath(),"public class Student { int age; }");

        Files.setLastModifiedTime(classFile.toPath(), FileTime.fromMillis(classFile.lastModified() + 5000));
        
        listener.checkFiles();

        List<ClassEvent> events = repo.getClassEvents();

        assertEquals(1, events.size());
        assertEquals("ClassModified", events.get(0).getAction());
        assertEquals("Student.java", events.get(0).getClassName());
        assertEquals("TestProject", events.get(0).getProjectName());
    }

    @Test 
    void classDeletionIsRecorded() throws Exception{
        File classFile = new File(testFolder, "Student.java");
        assertTrue(classFile.createNewFile());

        listener.trackProject(testFolder, "TestProject");

        assertTrue(classFile.delete());

        listener.checkFiles();

        List<ClassEvent> events = repo.getClassEvents();

        assertEquals(1, events.size());
        assertEquals("ClassDeleted", events.get(0).getAction());
        assertEquals("Student.java", events.get(0).getClassName());
        assertEquals("TestProject", events.get(0).getProjectName());
    }

    @Test 
    void existingClassIsNotRecordedAsNew() throws Exception{
        File classFile = new File(testFolder, "Student.java");
        assertTrue(classFile.createNewFile());

        listener.trackProject(testFolder, "TestProject");

        listener.checkFiles();

        List<ClassEvent> events = repo.getClassEvents();

        assertEquals(0, events.size());
    }

    @Test
    void classChangeAlsoRecordsProjectModification() throws Exception {

        listener.trackProject(testFolder, "TestProject");

        File classFile = new File(testFolder, "Student.java");
        assertTrue(classFile.createNewFile());

        listener.checkFiles();

        List<ProjectEvent> events = repo.getProjectEvents();

        assertEquals(1, events.size());
        assertEquals("ProjectModified", events.get(0).getAction());
        assertEquals("TestProject", events.get(0).getProjectName());
    }
}
