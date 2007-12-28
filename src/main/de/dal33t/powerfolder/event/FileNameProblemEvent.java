package de.dal33t.powerfolder.event;

import java.util.EventObject;
import java.util.List;
import java.util.Map;

import de.dal33t.powerfolder.disk.FilenameProblem;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.ScanResult;
import de.dal33t.powerfolder.light.FileInfo;

public class FileNameProblemEvent extends EventObject {
    private ScanResult scanResult;
    private Map<FileInfo, List<FilenameProblem>> problems;

    public FileNameProblemEvent(Folder folder, ScanResult scanResult) {
        this(folder, scanResult.getProblemFiles());
        this.scanResult = scanResult;
    }

    public FileNameProblemEvent(Folder folder,
        Map<FileInfo, List<FilenameProblem>> problems)
    {
        super(folder);
        this.problems = problems;
    }

    public ScanResult getScanResult() {
        return scanResult;
    }

    public Map<FileInfo, List<FilenameProblem>> getProblems() {
        return problems;
    }

    public Folder getFolder() {
        return (Folder) getSource();
    }
}
