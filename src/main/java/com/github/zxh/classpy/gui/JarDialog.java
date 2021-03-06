package com.github.zxh.classpy.gui;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 *
 * @author zxh
 */
public class JarDialog {
    
    public static URL showDialog(File jar) throws Exception {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        
        Button openButton = new Button("Open");
        Button cancelButton = new Button("Cancel");
        
        AtomicBoolean openButtonClicked = new AtomicBoolean(false);
        cancelButton.setOnAction(e -> stage.close());
        openButton.setOnAction(e -> {
            openButtonClicked.set(true);
            stage.close();
        });
        
        URI jarUri = new URI("jar", jar.toPath().toUri().toString(), null);  
        try (FileSystem zipFs = FileSystems.newFileSystem(jarUri, new HashMap<>())) {
            TreeView<Path> jarTree = createTreeView(zipFs.getPath("/"));
            BorderPane rootPane = createRootPane(jarTree, openButton, cancelButton);
            Scene scene = new Scene(rootPane, 500, 300);
            
            stage.setScene(scene);
            stage.setTitle("Jar");
            stage.showAndWait();
            
            if (openButtonClicked.get()) {
                return getSelectedClass(jar, jarTree);
            } else {
                return null;
            }
        }
    }
    
    private static TreeView<Path> createTreeView(Path rootPath) {
        JarTreeItem rootItem = new JarTreeItem(rootPath);
        rootItem.setExpanded(true);
        
        TreeView<Path> tree = new TreeView<>(rootItem);
        tree.setMinWidth(200);
        
        return tree;
    }
    
    private static BorderPane createRootPane(TreeView<Path> jarTree,
            Button openButton, Button cancelButton) {
        
        HBox buttonBox = new HBox();
        buttonBox.getChildren().add(openButton);
        buttonBox.getChildren().add(cancelButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(4, 4, 4, 4));
        buttonBox.setSpacing(4);
        
        BorderPane rootPane = new BorderPane();
        rootPane.setCenter(jarTree);
        rootPane.setBottom(buttonBox);
        return rootPane;
    }
    
    // jar:file:/absolute/location/of/yourJar.jar!/path/to/ClassName.class
    private static URL getSelectedClass(File jar, TreeView<Path> jarTree) throws MalformedURLException {
        TreeItem<Path> selectedItem = jarTree.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            Path selectedPath = selectedItem.getValue();
            if (selectedPath.toString().endsWith(".class")) {
                String jarPath = jar.getAbsolutePath();
                if (!jarPath.startsWith("/")) {
                    // windows
                    jarPath = "/" + jarPath;
                }
                String classPath = selectedPath.toAbsolutePath().toString();
                String classUrl = String.format("jar:file:%s!%s", jarPath, classPath);
                classUrl = classUrl.replace('\\', '/');
                System.out.println(classUrl);
                return new URL(classUrl);
            }
        }
        return null;
    }
    
}
