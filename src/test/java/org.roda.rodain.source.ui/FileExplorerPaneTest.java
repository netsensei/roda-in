package org.roda.rodain.source.ui;

import javafx.scene.control.TreeItem;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.roda.rodain.core.Footer;
import org.roda.rodain.source.ui.items.SourceTreeDirectory;
import org.roda.rodain.source.ui.items.SourceTreeFile;
import org.roda.rodain.source.ui.items.SourceTreeItemState;
import org.roda.rodain.utils.AsyncCallState;
import org.testfx.framework.junit.ApplicationTest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by adrapereira on 14-12-2015.
 */
public class FileExplorerPaneTest extends ApplicationTest {
  private static int LOAD_MORE_SIZE = 50;
  private Path testDir;
  private FileExplorerPane fileExplorer;

  @Override
  public void start(Stage stage) throws Exception {
    setUp();
    new Footer(); //footer needs to be initialized because of setStatus
    fileExplorer = new FileExplorerPane(stage);
    fileExplorer.setFileExplorerRoot(testDir);
  }

  @Before
  public void setUp() throws Exception {
    /*
      Create a directory structure to test the file explorer
     */
    String home = System.getProperty("user.home");
    Path homePath = Paths.get(home);

    testDir = homePath.resolve("RODA-In Test Dir");
    createDir(testDir);
    /*
      Dir 1
     */
    Path dir1 = testDir.resolve("dir1");
    createDir(dir1);
    for (int i = 0; i < 120; i++) {
      createFile(dir1.resolve("file" + i + ".txt"));
    }
    /*
      Dir 2
     */
    Path dir2 = testDir.resolve("dir2");
    createDir(dir2);
    for (int i = 0; i < 120; i++) {
      createDir(dir2.resolve("dir" + i));
    }
    /*
      Dir 3
     */
    Path dir3 = testDir.resolve("dir3");
    createDir(dir3);
    for (int i = 0; i < 70; i++) {
      createFile(dir3.resolve("file" + i + ".txt"));
      createDir(dir3.resolve("dir" + i));
    }
    /*
      Dir 4
     */
    Path dir4 = testDir.resolve("dir4");
    createDir(dir4);
    createFile(dir4.resolve("file1.txt"));
    createFile(dir4.resolve("file2.txt"));
    createFile(dir4.resolve("file3.txt"));

    //dir4/dirA
    Path dirA = dir4.resolve("dirA");
    createDir(dirA);

    // dir4/dirA/dirAA
    Path dirAA = dirA.resolve("dirAA");
    createDir(dirAA);
    Path dirAAA = dirAA.resolve("dirAAA");
    createDir(dirAAA);
    createFile(dirAAA.resolve("file1.txt"));
    Path dirAAB = dirAA.resolve("dirAAB");
    createDir(dirAAB);
    createFile(dirAAB.resolve("file1.txt"));
    createFile(dirAAB.resolve("file2.txt"));
    Path dirAAC = dirAA.resolve("dirAAC");
    createDir(dirAAC);
    for (int i = 0; i < 10; i++)
      createFile(dirAAC.resolve("file" + i + ".txt"));

    // dir4/dirA/dirAB
    Path dirAB = dirA.resolve("dirAB");
    createDir(dirAB);
    createFile(dirAB.resolve("file1.txt"));

    //dir4/dirB
    Path dirB = dir4.resolve("dirB");
    createDir(dirB);
    for (int i = 0; i < 13; i++)
      createFile(dirB.resolve("file" + i + ".txt"));
  }


  @Test
  public void root() {
    TreeItem<String> root = fileExplorer.getTreeView().getRoot();
    // Root exists, is a SourceTreeDirectory and its path is testDir
    assert root != null;
    assert root instanceof SourceTreeDirectory;
    assert ((SourceTreeDirectory) root).getPath().equals(testDir.toString());

    /* Tree is well structured */
    loadMore(root);
    assert root.getChildren().size() == 4;
  }

  @Test
  public void dir1() {
    TreeItem<String> root = fileExplorer.getTreeView().getRoot();
    TreeItem<String> dir1 = root.getChildren().get(0);
    assert dir1 != null;
    assert dir1.getValue().equals("dir1");

    loadMore(dir1);
    assert dir1.getChildren().size() == LOAD_MORE_SIZE + 1;
    assert dir1.getChildren().get(0) instanceof SourceTreeFile;

    loadMore(dir1);
    assert dir1.getChildren().size() == (LOAD_MORE_SIZE * 2) + 1;

    loadMore(dir1);
    assert dir1.getChildren().size() == 120;

    SourceTreeFile file = (SourceTreeFile) dir1.getChildren().get(0);
    assert file.getValue().equals("file0.txt");
    StringBuilder sb = new StringBuilder();
    sb.append(testDir).append(File.separator);
    sb.append("dir1").append(File.separator);
    sb.append("file0.txt");
    assert file.getPath().equals(sb.toString());
  }

  @Test
  public void dir2() {
    TreeItem<String> root = fileExplorer.getTreeView().getRoot();
    TreeItem<String> dir2 = root.getChildren().get(1);
    assert dir2 != null;
    assert dir2.getValue().equals("dir2");

    loadMore(dir2);
    assert dir2.getChildren().size() == LOAD_MORE_SIZE + 1;
    assert dir2.getChildren().get(0) instanceof SourceTreeDirectory;

    loadMore(dir2);
    assert dir2.getChildren().size() == (LOAD_MORE_SIZE * 2) + 1;

    loadMore(dir2);
    assert dir2.getChildren().size() == 120;
  }

  @Test
  public void dir3() {
    TreeItem<String> root = fileExplorer.getTreeView().getRoot();
    TreeItem<String> dir3 = root.getChildren().get(2);
    assert dir3 != null;
    assert dir3.getValue().equals("dir3");

    loadMore(dir3);
    assert dir3.getChildren().size() == LOAD_MORE_SIZE + 1;

    loadMore(dir3);
    assert dir3.getChildren().size() == (LOAD_MORE_SIZE * 2) + 1;

    loadMore(dir3);
    assert dir3.getChildren().size() == 140;

    List<Object> files = dir3.getChildren().stream().
        filter(p -> p instanceof SourceTreeFile).
        collect(Collectors.toList());
    List<Object> dirs = dir3.getChildren().stream().
        filter(p -> p instanceof SourceTreeDirectory).
        collect(Collectors.toList());

    assert files.size() == 70;
    assert dirs.size() == 70;
  }

  @Test
  public void dir4() {
    TreeItem<String> root = fileExplorer.getTreeView().getRoot();
    TreeItem<String> dir4 = root.getChildren().get(3);
    assert dir4 != null;
    assert dir4.getValue().equals("dir4");

    loadMore(dir4);
    assert dir4.getChildren().size() == 5;

    TreeItem<String> dirA = dir4.getChildren().get(0);
    assert dirA.getValue().equals("dirA");
    assert dirA instanceof SourceTreeDirectory;

    loadMore(dirA);
    assert dirA.getChildren().size() == 2;
    SourceTreeDirectory dirAA = (SourceTreeDirectory) dirA.getChildren().get(0);
    assert dirAA.getValue().equals("dirAA");

    loadMore(dirAA);
    assert dirAA.getChildren().size() == 3;
    SourceTreeDirectory dirAAC = (SourceTreeDirectory) dirAA.getChildren().get(2);
    assert dirAAC.getValue().equals("dirAAC");

    loadMore(dirAAC);
    assert dirAAC.getChildren().size() == 10;
    SourceTreeFile file = (SourceTreeFile) dirAAC.getChildren().get(0);
    assert file.getValue().equals("file0.txt");

  }

  private void loadMore(TreeItem dir) {
    if (dir instanceof SourceTreeDirectory) {
      AsyncCallState dirTask = ((SourceTreeDirectory) dir).loadMore();
      try {
        synchronized (dirTask) {
          dirTask.wait();
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  @After
  public void tearDown() throws Exception {
    FileUtils.deleteDirectory(testDir.toFile());
  }

  private void createDir(Path p) {
    File file = p.toFile();
    file.mkdir();
  }

  private void createFile(Path p) {
    try {
      PrintWriter writer = new PrintWriter(p.toString(), "UTF-8");
      writer.println(p.toString());
      writer.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

  }
}