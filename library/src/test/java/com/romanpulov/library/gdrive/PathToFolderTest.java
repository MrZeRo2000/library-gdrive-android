package com.romanpulov.library.gdrive;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class PathToFolderTest {
    @Test
    public void simple_path() {
        String path = "SimpleFolderName";
        String result = GDPathUtils.pathToFolder(path);
        Assert.assertEquals(path, result);
    }

    @Test
    public void leading_path() {
        String path = File.separator + "Path";
        String result = GDPathUtils.pathToFolder(path);
        Assert.assertEquals("Path", result);
    }

    @Test
    public void leading_path_follow_separator() {
        String path = File.separator + "Path" + File.separator;
        String result = GDPathUtils.pathToFolder(path);
        Assert.assertEquals("Path", result);
    }

    @Test
    public void leading_path_follow_separator_and_file() {
        String path = File.separator + "Path" + File.separator + "name";
        String result = GDPathUtils.pathToFolder(path);
        Assert.assertEquals("Path", result);
    }


}
