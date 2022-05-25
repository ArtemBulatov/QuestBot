package ru.quest.services;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import ru.quest.dto.FileDTO;
import ru.quest.models.Photo;
import ru.quest.repositories.PhotoRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Comparator;
import java.util.List;

@Service
public class PhotoService {

    private final PhotoRepository photoRepository;
    private final Gson gson;

    public PhotoService(PhotoRepository photoRepository, Gson gson) {
        this.photoRepository = photoRepository;
        this.gson = gson;
    }

    public Photo save(Photo photo) {
        return photoRepository.save(photo);
    }

    public void delete(Photo photo) {
        photoRepository.delete(photo);
    }

    public List<Photo> getAll() {
        return photoRepository.findAll();
    }

    public Photo getSavedPhotoFromDto(List<PhotoSize> photoSizeList, String botToken) {
        photoSizeList.sort(Comparator.comparing(PhotoSize::getFileSize));
        PhotoSize photoSize = photoSizeList.get(photoSizeList.size()-1);
        Photo photo = new Photo();

        try {
            URL obj = new URL("https://api.telegram.org/bot" + botToken + "/getFile?file_id=" + photoSize.getFileId());
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

            photo.setName(fileDTO.getResult().getFile_path().split("/",2)[1]);
            photo.setBytes(bytes);
            photo = save(photo);

            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return photo;
    }
}
