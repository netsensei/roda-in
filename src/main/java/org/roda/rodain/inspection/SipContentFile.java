package org.roda.rodain.inspection;

import java.nio.file.Path;

import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * @author Andre Pereira apereira@keep.pt
 * @since 17-09-2015.
 */
public class SipContentFile extends TreeItem<Object> implements InspectionTreeItem {
    public static final Image fileImage = new Image(ClassLoader.getSystemResourceAsStream("icons/file.png"));
    private Path fullPath;
    private TreeItem parent;


    public SipContentFile(Path file, TreeItem parent) {
        super(file.toString());
        this.fullPath = file;
        this.parent = parent;
        this.setGraphic(new ImageView(fileImage));

        Path name = fullPath.getFileName();
        if (name != null) {
            this.setValue(name.toString());
        } else {
            this.setValue(fullPath.toString());
        }
    }

    /**
     * @return This item's parent.
     */
    @Override
    public TreeItem getParentDir() {
        return parent;
    }

    /**
     * @return The path of this item.
     */
    @Override
    public Path getPath() {
        return this.fullPath;
    }
}
