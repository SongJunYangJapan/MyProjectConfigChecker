package org.example;

public class SearchConfig {
    private String file;
    private String searchText;
    private String expectResult;

    public SearchConfig(String file, String searchText, String expectResult) {
        this.file = file;
        this.searchText = searchText;
        this.expectResult = expectResult;
    }

    // Getters
    public String getFile() {
        return file;
    }

    public String getSearchText() {
        return searchText;
    }

    public String getExpectResult() {
        return expectResult;
    }
}