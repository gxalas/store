package com.example.pdfreader;

import jakarta.persistence.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "entries_files")
public class EntriesFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "filepath")
    private String filepath;

    @Column(name = "checksum")
    private String checksum;

    public EntriesFile(String filePath, String checksum) {
        this.filepath = filePath;
        this.checksum = checksum;
    }
    public EntriesFile() {
    }

    // Getters and setters

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntriesFile that = (EntriesFile) o;
        return Objects.equals(filepath, that.filepath) &&
                Objects.equals(checksum, that.checksum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filepath, checksum);
    }

    public static List<File> getTxtFilesInFolder(String folderPath) {
        List<File> txtFiles = new ArrayList<>();
        File folder = new File(folderPath);
        getTxtFilesRecursive(folder, txtFiles);

        return txtFiles;
    }

    private static void getTxtFilesRecursive(File folder, List<File> txtFiles) {
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase().endsWith(".txt")) {
                    txtFiles.add(file);
                } else if (file.isDirectory()) {
                    // Recursively search in subfolders
                    getTxtFilesRecursive(file, txtFiles);
                }
            }
        }
    }
}
