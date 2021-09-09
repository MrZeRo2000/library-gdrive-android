package com.romanpulov.library.gdrive;

import java.io.File;

public class GDPathUtils {
    private static String removeLeadingDelimiters(String path) {
        //remove leading delimiters
        int si = 0;
        while ((path.substring(si).startsWith(File.separator)) && (si < (path.length() - 1))) {
            si += File.separator.length();
        }

        return path.substring(si);
    }

    public static String pathToFolder(String path) {
        String p1 = removeLeadingDelimiters(path);

        String p2;
        int li = p1.indexOf(File.separator);
        if (li > 0) {
            p2 = p1.substring(0, li);
        } else {
            p2 = p1;
        }

        return p2;
    }

    public static String[] pathToFolderNameAndFileName(String path) {
        String p1 = removeLeadingDelimiters(path);

        int li = p1.indexOf(File.separator);
        if (li > 0) {
            return new String[] {p1.substring(0, li), p1.substring(li + 1)};
        } else {
            return new String[] {p1, null};
        }
    }
}
