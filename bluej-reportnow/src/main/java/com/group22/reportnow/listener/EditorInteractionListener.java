
package com.group22.reportnow.listener;

import bluej.extensions2.editor.DocumentListener;
import bluej.extensions2.editor.JavaEditor;

public class EditorInteractionListener implements DocumentListener {

    private final EditorInteractionTracker tracker;
    private final JavaEditor editor;

    public EditorInteractionListener(
            JavaEditor editor,
            EditorInteractionTracker tracker) {

        this.editor = editor;
        this.tracker = tracker;

        editor.addDocumentListener(this);
    }

    @Override
    public void textReplaced(
            int offset,
            String oldText,
            String newText,
            int newCaretPos,
            int newSelectionLength) {

        tracker.recordEdit();
    }

    public void detach() {
        editor.removeDocumentListener(this);
    }
}
