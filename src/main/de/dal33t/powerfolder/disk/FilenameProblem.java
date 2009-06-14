package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Translation;

public class FilenameProblem {

//    /**
//     * The fileinfo that has problems, immutable field
//     */
//    private final FileInfo fileInfo;
//
//    /**
//     * The FileInfo that hold the same name (but with differnt case), immutable
//     * field
//     */
//    private final FileInfo fileInfoDupe;
//
//    private final int filenameProblemType;
//
//    /**
//     * Creates a FileName Problem. only used internal use the getProblems method
//     */
//    public FilenameProblem(FileInfo fileInfo, int filenameProblemType) {
//        this.fileInfo = fileInfo;
//        this.filenameProblemType = filenameProblemType;
//        fileInfoDupe = null;
//    }
//
//    /**
//     * creates a FileName Problem with a duplicate (problemType =
//     * DUPPLICATE_FOUND)
//     */
//    public FilenameProblem(FileInfo fileInfo, FileInfo fileInfoDupe) {
//        this.fileInfo = fileInfo;
//        this.fileInfoDupe = fileInfoDupe;
//        filenameProblemType = FilenameProblemHelper.DUPLICATE_FOUND;
//    }
//
//    public FileInfo getFileInfo() {
//        return fileInfo;
//    }
//
//    public FileInfo getFileInfoDupe() {
//        return fileInfoDupe;
//    }
//
//    public int getFilenameProblemType() {
//        return filenameProblemType;
//    }
//
//    public String describeProblem() {
//        switch (filenameProblemType) {
//            case FilenameProblemHelper.CONTAINS_ILLEGAL_LINUX_CHARS:
//                return Translation
//                        .getTranslation("filename_problem.not_recommended_chars_linux.description");
//            case FilenameProblemHelper.CONTAINS_ILLEGAL_MACOSX_CHARS:
//                return Translation
//                        .getTranslation("filename_problem.not_recommended_chars_mac_osx.description");
//            case FilenameProblemHelper.CONTAINS_ILLEGAL_WINDOWS_CHARS:
//                return Translation
//                        .getTranslation("filename_problem.not_recommended_chars_windows.description");
//            case FilenameProblemHelper.ENDS_WITH_ILLEGAL_WINDOWS_CHARS:
//                return Translation
//                        .getTranslation("filename_problem.ends_with_illegal_char.description");
//            case FilenameProblemHelper.IS_RESERVED_WINDOWS_WORD:
//                return Translation
//                        .getTranslation("filename_problem.reserved_filename.description");
//            case FilenameProblemHelper.TOO_LONG:
//                return Translation
//                        .getTranslation("filename_problem.to_long.description");
//            case FilenameProblemHelper.DUPLICATE_FOUND:
//                return Translation.getTranslation(
//                        "filename_problem.duplicate.description", fileInfoDupe
//                                .getName());
//            default:
//                throw new IllegalStateException("invalid FilenameProblemType: " + filenameProblemType);
//        }
//    }
//
//    public String shortDescription() {
//        switch (filenameProblemType) {
//            case FilenameProblemHelper.CONTAINS_ILLEGAL_LINUX_CHARS: // fallthrough
//            case FilenameProblemHelper.CONTAINS_ILLEGAL_MACOSX_CHARS: // fallthrough
//            case FilenameProblemHelper.CONTAINS_ILLEGAL_WINDOWS_CHARS:
//                return Translation
//                        .getTranslation("filename_problem.not_recommended_chars");
//            case FilenameProblemHelper.ENDS_WITH_ILLEGAL_WINDOWS_CHARS:
//                return Translation
//                        .getTranslation("filename_problem.ends_with_illegal_char");
//            case FilenameProblemHelper.IS_RESERVED_WINDOWS_WORD:
//                return Translation
//                        .getTranslation("filename_problem.reserved_filename");
//            case FilenameProblemHelper.TOO_LONG:
//                return Translation.getTranslation("filename_problem.to_long");
//            case FilenameProblemHelper.DUPLICATE_FOUND:
//                return Translation.getTranslation("filename_problem.duplicate");
//            default:
//                throw new IllegalStateException("invalid FilenameProblemType: " + filenameProblemType);
//        }
//    }
//
//
}
