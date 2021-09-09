package com.romanpulov.library.gdrive;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class PathToFolderNameFileNameTest {
    @Test
    public void path_and_file() {
        String path = File.separator + "Path" + File.separator + "Name";
        String[] result = GDPathUtils.pathToFolderNameAndFileName(path);
        Assert.assertEquals("Path", result[0]);
        Assert.assertEquals("Name", result[1]);
    }

    @Test
    public void path_no_root_and_file() {
        String path = "Path" + File.separator + "Name";
        String[] result = GDPathUtils.pathToFolderNameAndFileName(path);
        Assert.assertEquals("Path", result[0]);
        Assert.assertEquals("Name", result[1]);
    }

    @Test
    public void path_no_file() {
        String path = File.separator + "Path";
        String[] result = GDPathUtils.pathToFolderNameAndFileName(path);
        Assert.assertEquals("Path", result[0]);
        Assert.assertNull(result[1]);
    }


}
