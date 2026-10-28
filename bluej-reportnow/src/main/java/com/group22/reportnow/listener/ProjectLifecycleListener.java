package com.group22.reportnow.listener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.group22.reportnow.factory.TelemetryEventFactory;
import com.group22.reportnow.repository.TelemetryRepository;

import bluej.extensions2.BlueJ;
import bluej.extensions2.BProject;
import bluej.extensions2.ProjectNotOpenException;
import bluej.extensions2.event.PackageEvent;
import bluej.extensions2.event.PackageListener;

public class ProjectLifecycleListener implements PackageListener {
    private final BlueJ blueJ;
    private final TelemetryRepository repository;
    private final ClassLifecycleListener classListener;
    private final Consumer<String> onProjectClosing; // NEW callback
    private Map<String, String> projects = new ConcurrentHashMap<>();
    private Map<String, Boolean> openProjects = new ConcurrentHashMap<>();
    private Timer lifecycleTimer;

    public ProjectLifecycleListener(BlueJ blueJ, TelemetryRepository repository, ClassLifecycleListener classListener, Consumer<String> onProjectClosing){
	    this.blueJ = blueJ;
	    this.repository = repository;
	    this.classListener = classListener;
        this.onProjectClosing = onProjectClosing;
    }

    private void recordEvent(String action, String projectName){
        repository.addEvent(TelemetryEventFactory.createProjectEvent(action, projectName));
    }

    @Override 
    public synchronized void packageOpened(PackageEvent event){
	    try{
		    BProject project = event.getPackage().getProject();
		    addProject(project);
        } catch (ProjectNotOpenException e){
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void packageClosing(PackageEvent event){
	    try {
            String projectName = event.getPackage().getProject().getName();
            // Trigger the export for this specific project
            if (onProjectClosing != null) {
                onProjectClosing.accept(projectName);
            }
        } catch (ProjectNotOpenException e) {
            System.out.println(e.getMessage());
        }
    }

    private synchronized void addProject(BProject project) throws ProjectNotOpenException {
        String projectName = project.getName();
        File projectFolder = project.getDir();
        String path = projectFolder.getAbsolutePath();

        if(projects.putIfAbsent(path, projectName) == null){
            if(isNewProject(projectFolder)){
                recordEvent("ProjectCreated", projectName);
            } 
        }
        if(openProjects.putIfAbsent(path, true) == null){
            recordEvent("ProjectOpened", projectName);
        }
        classListener.trackProject(projectFolder, projectName);
    }

    private boolean isNewProject(File projectFolder){
        try{
            BasicFileAttributes attributes = Files.readAttributes(projectFolder.toPath(), BasicFileAttributes.class);
            long creationTime = attributes.creationTime().toMillis();
            long currentTime = System.currentTimeMillis();
            return creationTime <= currentTime && currentTime - creationTime < 1000L;
        } catch (IOException e){
            return false;
        }
    }

    public void recordProjectModified(String projectName){
        repository.addEvent(TelemetryEventFactory.createProjectEvent("ProjectModified", projectName));
    }

    public synchronized void checkOpenProjects(){
        BProject[] currentProjects = blueJ.getOpenProjects();
        Map<String, Boolean> currentOpenProjects = new ConcurrentHashMap<>();

        for(BProject project : currentProjects){
            try{
                String path = project.getDir().getAbsolutePath();
                currentOpenProjects.put(path, true);
                addProject(project);
            } catch (ProjectNotOpenException e){
                System.out.println(e.getMessage());
            }
        }
        openProjects.keySet().retainAll(currentOpenProjects.keySet());
    }

    private synchronized void checkProjects(){
        for(String path : new ConcurrentHashMap<>(projects).keySet()){
            File projectFolder = new File(path);
            if(!projectFolder.exists()){
                String projectName = projects.remove(path);
                openProjects.remove(path);
                if(projectName != null){
                    recordEvent("ProjectDeleted", projectName);
                }
            }
        }
    }

    public synchronized void startTimer(){
        if(lifecycleTimer != null){
            return;
        }
        checkOpenProjects();
	    TimerTask task = new TimerTask(){
	    @Override
	    public void run(){
            try{
                checkOpenProjects();
                checkProjects();
            } catch (Exception e){
                System.out.println("Error checking projects: " + e.getMessage());
            }
	    }
    };
        lifecycleTimer = new Timer("Lifecycle Timer", true);
        lifecycleTimer.scheduleAtFixedRate(task,1000L,1000L);
    }

    public synchronized void stopTimer(){
	    if(lifecycleTimer != null){
		    lifecycleTimer.cancel();
            lifecycleTimer = null;
	    }
    }
}