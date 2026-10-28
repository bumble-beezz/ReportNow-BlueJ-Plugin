
package com.group22.reportnow.listener;

import bluej.extensions2.BlueJ;
import bluej.extensions2.BProject;
import bluej.extensions2.BPackage;
import bluej.extensions2.BClass;
import bluej.extensions2.ProjectNotOpenException;
import bluej.extensions2.editor.JavaEditor;
import bluej.extensions2.PackageNotFoundException;

import java.util.IdentityHashMap;
import java.util.Map;

public class EditorInteractionManager {

    private final BlueJ blueJ;
    private final EditorInteractionTracker tracker;

    private final Map<JavaEditor, EditorInteractionListener> attachedListeners =
            new IdentityHashMap<>();

    public EditorInteractionManager(
            BlueJ blueJ,
            EditorInteractionTracker tracker) {

        this.blueJ = blueJ;
        this.tracker = tracker;
    }

    public void scanOpenEditors() {
        for (BProject project : blueJ.getOpenProjects()) {
            try {
                for (BPackage currentPackage : project.getPackages()) {
                    for (BClass currentClass : currentPackage.getClasses()) {

                        JavaEditor editor =
                                currentClass.getJavaEditorIfOpen();

                        if (editor != null
                                && !attachedListeners.containsKey(editor)) {

                            EditorInteractionListener listener =
                                    new EditorInteractionListener(editor, tracker);

                            attachedListeners.put(editor, listener);
                        }
                    }
                }
            } catch (ProjectNotOpenException | PackageNotFoundException e) {
                System.err.println(
                    "[ReportNow] Could not scan a project or package: " + e.getMessage());
            }
        }
    }

    public void detachAllListeners() {
        for (EditorInteractionListener listener :
                attachedListeners.values()) {
            listener.detach();
        }

        attachedListeners.clear();
    }
}
