
package com.group22.reportnow.listener;

import bluej.extensions2.event.PackageEvent;
import bluej.extensions2.event.PackageListener;

public class EditorPackageListener implements PackageListener {

    private final EditorInteractionManager manager;

    public EditorPackageListener(EditorInteractionManager manager) {
        this.manager = manager;
    }

    @Override
    public void packageOpened(PackageEvent event) {
        manager.scanOpenEditors();
    }

    @Override
    public void packageClosing(PackageEvent event) {
        // No action needed when a package closes.
    }
}
