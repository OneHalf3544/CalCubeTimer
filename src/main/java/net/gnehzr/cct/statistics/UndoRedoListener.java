package net.gnehzr.cct.statistics;

public interface UndoRedoListener {

	void undoRedoChange(int undoable, int redoable);

	void refresh();
}
