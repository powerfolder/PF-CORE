package de.dal33t.powerfolder.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import de.dal33t.powerfolder.util.FileUtils;

public class Delete {

    /**
     * to test call this method with an existing file in the current working
     * directory
     * 
     * @param args
     */
    public static void main(String[] args) {
        if (createAndRename()) {
            System.out.println("createAndRename succes");
        } else {
            System.out.println("createAndRename failed");
        }
        File file = new File(args[0]);
        if (moveToRecycleBin(file)) {
            System.out.println("moveToRecycleBin succes");
        } else {
            System.out.println("moveToRecycleBin failed");
        }
    }
    
    public static boolean createAndRename() {
        File file = new File("text.txt");
        try {
            file.createNewFile();
            FileWriter writer = new FileWriter(file);
            writer.write("test text");
            writer.close();
            File target = new File("recycled\\text_renamed.txt");
            new File (target.getParent()).mkdirs();
            return (file.renameTo(target));
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        }
        
        
    }

    /** @return true if succeded */
    public static boolean moveToRecycleBin(File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException(
                "moveToRecycleBin: file does not exists: " + file);
        }

        File recycleBinDir = new File(".recycle");
        if (!recycleBinDir.exists()) {
            if (!recycleBinDir.mkdir()) {
                System.out
                    .println("moveToRecycleBin: cannot create recycle bin: "
                        + recycleBinDir);
                return false;
            }
            // Make recycle bin system/hidden
            FileUtils.setAttributesOnWindows(recycleBinDir, true, true);
        }

        File target = new File(recycleBinDir, file.getName()); //getName should be the complete dir but for this test this is ok
        //File target = new File("newname.xxx");
        if (!new File(target.getParent()).mkdirs()) {
            System.out
                .println("moveToRecycleBin: cannot create recycle bin directorty structure for: "
                    + target);
            return false;
        }/*
        if (file.getAbsoluteFile().isAbsolute()) {
            System.out.println("abs: " + file);
        } else {
            System.out.println("not abs: " + file);
        }
        
        if (target.isAbsolute()) {
            System.out.println("abs: " + target);
        } else {
            System.out.println("not abs: " + target);
        }
        
        if (!file.getAbsoluteFile().renameTo(target.getAbsoluteFile())) {
            System.out
                .println("moveToRecycleBin: cannot rename file to recycle bin: "
                    + target);
            try {
                Util.copyFile(file, target);
            } catch (IOException ioe) {
                System.out
                    .println("moveToRecycleBin: cannot copy to recycle bin: "
                        + target + "\n" + ioe.getMessage());
                return false;
            }
            if (!file.delete()) {
                System.out
                    .println("moveToRecycleBin: cannot delete file after copy to recycle bin: "
                        + file);
                return false;
            }
        }
        // checks to validate code
        if (file.exists()) {
            System.out
                .println("moveToRecycleBin: source not deleted?: " + file);
            return false;
        }
        if (!target.exists()) {
            System.out.println("moveToRecycleBin: target not created?: "
                + target);
            return false;
        }
*/
        return true;
    }
    // RecycleDelete.delete(args[0],true, true);
}
