package com.group22.reportnow.listener;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.TimerTask;

import com.group22.reportnow.factory.TelemetryEventFactory;
import com.group22.reportnow.repository.TelemetryRepository;

import bluej.extensions2.event.ClassEvent;
import bluej.extensions2.event.ClassListener;

public class ClassLifecycleListener implements ClassListener {
	private final TelemetryRepository repository;
	private final Map<String, String> projects = new ConcurrentHashMap<>();
	private final Map<String, Long> classes = new ConcurrentHashMap<>();
	private Timer lifecycleTimer;

	public ClassLifecycleListener(TelemetryRepository repository){
		this.repository = repository;
	}
	
	public synchronized void trackProject(File folder, String projectName){
		String path = folder.getAbsolutePath();
	
		if(projects.putIfAbsent(path, projectName) == null){
			searchClasses(folder, projectName, false);
		}
	}

	private void searchClasses(File folder, String projectName, boolean recordChanges){		
        File[] files = folder.listFiles();
		
		if(files == null){
			return;
		}

		for(File file: files){
			if(file.isDirectory()) {
				searchClasses(file, projectName, recordChanges);
			}
			else if (file.getName().endsWith(".java")){
				String path = file.getAbsolutePath();
				long modified = file.lastModified();
				Long previous = classes.putIfAbsent(path, modified);
		
				if(previous == null){
                    if(recordChanges){
                        recordEvent("ClassCreated", file.getName(), projectName);
					}
				}
                else if (previous.longValue() != modified){
                    classes.put(path, modified);
                    if(recordChanges){
                        recordEvent("ClassModified", file.getName(), projectName);
                    }
                }
			}
		}
}

	private void recordEvent(String action, String className, String projectName){
		repository.addEvent(TelemetryEventFactory.createClassEvent(action, className, projectName));
        repository.addEvent(TelemetryEventFactory.createProjectEvent("ProjectModified", projectName));
    }   

    private String getFileName(String className){
        int position = className.lastIndexOf(".");
        if (position >= 0){
            className = className.substring(position + 1);
        }
        return className + ".java";
    }

    @Override
    public void classStateChanged(ClassEvent event){}

    @Override
    public synchronized void classNameChanged(ClassEvent event){
        try{
            File folder = event.getPackage().getProject().getDir();
            String projectName = projects.get(folder.getAbsolutePath());
            if(projectName == null){
                return;
            }

            File packageFolder = event.getPackage().getDir();
            String oldFileName = getFileName(event.getOldName());
            String newFileName = getFileName(event.getBClass().getName());

            File oldFile = new File(packageFolder, oldFileName);
            File newFile = new File(packageFolder, newFileName);

            Long previous = classes.remove(oldFile.getAbsolutePath());
            if(previous != null){
                if (newFile.exists()){
                    classes.put(newFile.getAbsolutePath(), newFile.lastModified());
                }
                recordEvent("ClassModified", newFileName, projectName);
            }
        } catch (Exception e){
            System.out.println("Error handling class name change: " + e.getMessage());
        }
    }

    @Override
    public synchronized void classRemoved(ClassEvent event){
        try{
            File folder = event.getPackage().getProject().getDir();
            String projectName = projects.get(folder.getAbsolutePath());
            if(projectName == null){
                return;
            }

            File packageFolder = event.getPackage().getDir();
            String className = getFileName(event.getBClass().getName());
            File classFile = new File(packageFolder, className);

            Long previous = classes.remove(classFile.getAbsolutePath());
            if(previous != null && !classFile.exists()){
                recordEvent("ClassDeleted", className, projectName);
            }
        } catch (Exception e){
            System.out.println("Error handling class removal: " + e.getMessage());
        }
    }

    public synchronized void checkFiles(){
        for(String path : projects.keySet()){
            File folder = new File(path);
            if(!folder.exists()){
                continue;
            }
            
            String projectName = projects.get(path);
            searchClasses(folder, projectName, true);
        }

        for(String path : new ArrayList<>(classes.keySet())){
            File file = new File(path);
            if(!file.exists()){
                for(String projectPath : projects.keySet()){
                    File projectFolder = new File(projectPath);
                    if(projectFolder.exists() && path.startsWith(projectPath + File.separator)){
                       Long previous = classes.remove(path);
                       if(previous != null){
                           recordEvent("ClassDeleted", file.getName(), projects.get(projectPath));
                       }
                       break;
                    }
                    
                }
            }
        }
    }
    

    public synchronized void startTimer(){
	    if(lifecycleTimer != null){
            return;
        }
        TimerTask task = new TimerTask(){

	    @Override
	    public void run(){
		    try{
                checkFiles();
            } catch (Exception e){
                System.out.println("Error checking files: " + e.getMessage());
            }
	    }
    };

        lifecycleTimer = new Timer("Class Lifecycle Timer", true);
        long delay = 1000L;
        long period = 1000L;

        lifecycleTimer.scheduleAtFixedRate(task,delay,period);
}

    
    public synchronized void stopTimer(){
	    if(lifecycleTimer != null){
		    lifecycleTimer.cancel();
            lifecycleTimer = null;
	    }
    }

}
