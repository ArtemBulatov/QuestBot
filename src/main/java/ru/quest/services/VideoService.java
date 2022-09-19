package ru.quest.services;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.quest.dto.FileDTO;
import ru.quest.dto.VideoDTO;
import ru.quest.models.Video;
import ru.quest.repositories.VideoRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j
@Service
public class VideoService {
    private final VideoRepository videoRepository;
    private final Gson gson;

    public VideoService(VideoRepository videoRepository, Gson gson) {
        this.videoRepository = videoRepository;
        this.gson = gson;
    }

    public Video save(Video video) {
        return videoRepository.save(video);
    }

    public void delete(Video video) {
        videoRepository.delete(video);
    }

    public Video getSavedVideoFromDto(VideoDTO dto, String botToken) {
        Video video = new Video();

        try {
            URL obj = new URL("https://api.telegram.org/bot" + botToken + "/getFile?file_id=" + dto.getVideo().getFileId());
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            while (reader.ready()) {
                response.append(reader.readLine());
            }
            reader.close();

            FileDTO fileDTO = gson.fromJson(response.toString(), FileDTO.class);

            URL fileUrl = new URL("https://api.telegram.org/file/bot" + botToken + "/" + fileDTO.getResult().getFile_path());
            connection = (HttpURLConnection) fileUrl.openConnection();

            InputStream inputStream = connection.getInputStream();
            byte[] bytes = inputStream.readAllBytes();

            video.setName(fileDTO.getResult().getFile_path().split("/",2)[1]);
            video.setBytes(bytes);
            video = save(video);

            inputStream.close();
        } catch (IOException e) {
            log.error("Ошибка при загрузке видео: " + e.getMessage());
        }

        return video;
    }
}
