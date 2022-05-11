package ru.quest.dto;

import lombok.Data;

@Data
public class FileDTO {
    private boolean ok;
    private FileInfo result;

    @Data
    public static class FileInfo {
        private String file_id;
        private String file_unique_id;
        private long file_size;
        private String file_path;
    }

}
