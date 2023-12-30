package org.example;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class ConfigFileScanner {

    public static void main(String[] args) {
        Yaml yaml = new Yaml();
        try (InputStream in = ConfigFileScanner.class.getClassLoader().getResourceAsStream("config.yml")) {
            Map<String, Object> config = yaml.load(in);
            String directoryPath = (String) config.get("directoryPath");
            List<Map<String, String>> searchConfigs = (List<Map<String, String>>) config.get("searchConfigs");
            String outputPath = (String) config.get("outputPath");


            List<SearchConfig> searchConfigList = new ArrayList<>();
            for (Map<String, String> searchConfigMap : searchConfigs) {
                String file = searchConfigMap.get("file");
                String searchText = searchConfigMap.get("searchText");
                String expectResult = searchConfigMap.get("expectResult");
                searchConfigList.add(new SearchConfig(file, searchText, expectResult));
            }

            Map<File, List<String>> foundFiles = new HashMap<>();

            Files.walkFileTree(Paths.get(directoryPath), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    // 获取目录名称
                    String dirName = dir.getFileName().toString();
                    List<String> excludedDirectories = Arrays.asList(".idea", ".gradle", "build", "gradle", "out"); // 替换为实际要排除的目录名称
                    // 检查当前目录是否在排除列表中
                    if (excludedDirectories.contains(dirName)) {
                        return FileVisitResult.SKIP_SUBTREE; // 跳过整个子树
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!file.toString().endsWith(".java") && !file.toString().endsWith(".xml") && !file.toString().endsWith(".csv")) {
                        String fileName = file.getFileName().toString();

                        // 使用迭代器避免在遍历时修改列表
                        Iterator<SearchConfig> iterator = searchConfigList.iterator();
                        while (iterator.hasNext()) {
                            SearchConfig searchConfig = iterator.next();
                            if (searchConfig.getFile().equals(fileName)) {
                                List<String> lines = findLines(file.toFile(), searchConfig.getSearchText(), searchConfig.getExpectResult());
                                if (!lines.isEmpty()) {
                                    foundFiles.computeIfAbsent(file.toFile(), k -> new ArrayList<>()).addAll(lines);
                                }
                                iterator.remove(); // 移除已经处理的配置
                                break; // 匹配到一个文件后就跳出循环
                            }
                        }

                        if (searchConfigList.isEmpty()) {
                            return FileVisitResult.TERMINATE; // 所有配置都已处理，提前终止遍历
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            // 写入到TXT文件
            writeResultsToFile(foundFiles, outputPath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<String> findLines(File file, String searchText, String expectResult) throws IOException {
        List<String> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                if (expectResult.contains("<>")) {
                    expectResult = expectResult.substring(2, expectResult.length());
                    if (currentLine.contains(searchText)) {
                        String nextLine = reader.readLine();
                        result.add("Current line: " + currentLine);
                        if (nextLine != null) {
                            result.add("Next line: " + nextLine);
                            if (currentLine.contains(expectResult) || nextLine.contains(expectResult)) {
                                result.add("Wrong config!Should not be " + expectResult);
                            }
                        }
                        break; // 找到 searchText 后就跳出循环
                    }
                } else {
                    if (currentLine.contains(searchText)) {
                        String nextLine = reader.readLine();
                        result.add("Current line: " + currentLine);
                        if (nextLine != null) {
                            result.add("Next line: " + nextLine);
                            if (!currentLine.contains(expectResult) && !nextLine.contains(expectResult)) {
                                result.add("Wrong config!The right config should be " + expectResult);
                            } else {
                                result.add("Correct config!");
                            }
                        }
                        break; // 找到 searchText 后就跳出循环
                    }
                }

            }
        }
        return result;
    }

    private static void writeResultsToFile(Map<File, List<String>> foundFiles, String outputPath) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(outputPath))) {
            for (Map.Entry<File, List<String>> entry : foundFiles.entrySet()) {
                out.println("File: " + entry.getKey().getName());
                for (String line : entry.getValue()) {
                    out.println(line);
                }
                out.println("---"); // 添加分隔符以区分不同文件的输出
            }
        }
    }
}
