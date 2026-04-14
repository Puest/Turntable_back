package com.turn.service;

import com.turn.model.Track;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TrackService {

    private final List<Track> tracks = new ArrayList<>();
    private final ResourceLoader resourceLoader;
    private Path musicDir;
    private Path coversDir;

    private static final String[] LABEL_COLORS = {
        "#cc3333", "#3366cc", "#339933", "#cc9933", "#9933cc",
        "#cc6633", "#3399cc", "#66cc33", "#cc3399", "#33cc99"
    };

    public TrackService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        // classpath의 music 폴더 경로를 찾는다
        try {
            Resource musicResource = resourceLoader.getResource("classpath:music/");
            musicDir = Paths.get(musicResource.getURI());
        } catch (IOException e) {
            // classpath에서 못 찾으면 상대 경로로 시도
            musicDir = Paths.get("src/main/resources/music");
        }

        // covers 폴더 경로
        try {
            Resource coversResource = resourceLoader.getResource("classpath:covers/");
            coversDir = Paths.get(coversResource.getURI());
        } catch (IOException e) {
            coversDir = Paths.get("src/main/resources/covers");
        }

        scanMusicDirectory();
    }

    private void scanMusicDirectory() {
        tracks.clear();
        AtomicLong idCounter = new AtomicLong(1);

        if (!Files.exists(musicDir)) {
            System.out.println("[TrackService] Music directory not found: " + musicDir);
            return;
        }

        try {
            Files.list(musicDir)
                .filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return name.endsWith(".mp3") || name.endsWith(".wav")
                        || name.endsWith(".ogg") || name.endsWith(".flac");
                })
                // fireplace, chair-creak 등 효과음 제외
                .filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return !name.contains("fireplace") && !name.contains("chair")
                        && !name.contains("creak") && !name.contains("sfx");
                })
                .sorted()
                .forEach(p -> {
                    long id = idCounter.getAndIncrement();
                    String filename = p.getFileName().toString();
                    String nameWithoutExt = filename.replaceAll("\\.[^.]+$", "");
                    // 파일명에서 제목 추출 (언더스코어/하이픈 → 공백)
                    String title = nameWithoutExt.replace("_", " ").replace("-", " ");
                    // 첫 글자 대문자
                    title = title.substring(0, 1).toUpperCase() + title.substring(1);

                    int colorIdx = (int) ((id - 1) % LABEL_COLORS.length);
                    String coverImage = findCoverImage(nameWithoutExt);
                    String discImage = findDiscImage(nameWithoutExt);
                    tracks.add(new Track(id, title, "Unknown Artist", "Vinyl Collection",
                        0, filename, LABEL_COLORS[colorIdx], coverImage, discImage));
                });

            System.out.println("[TrackService] Loaded " + tracks.size() + " tracks from " + musicDir);
            tracks.forEach(t -> System.out.println("  - " + t.getId() + ": " + t.getTitle() + " (" + t.getFilename() + ")"));

        } catch (IOException e) {
            System.err.println("[TrackService] Error scanning music directory: " + e.getMessage());
        }
    }

    public List<Track> getAllTracks() {
        return tracks;
    }

    public Optional<Track> getTrackById(Long id) {
        return tracks.stream().filter(t -> t.getId().equals(id)).findFirst();
    }

    public Resource getTrackResource(String filename) {
        Path filePath = musicDir.resolve(filename);
        return new FileSystemResource(filePath.toFile());
    }

    public Resource getCoverResource(String coverImage) {
        Path filePath = coversDir.resolve(coverImage);
        return new FileSystemResource(filePath.toFile());
    }

    private String findCoverImage(String nameWithoutExt) {
        if (!Files.exists(coversDir)) return null;
        String[] extensions = {".jpg", ".jpeg", ".png", ".webp"};
        for (String ext : extensions) {
            Path candidate = coversDir.resolve(nameWithoutExt + ext);
            if (Files.exists(candidate)) {
                return nameWithoutExt + ext;
            }
        }
        return null;
    }

    private String findDiscImage(String nameWithoutExt) {
        if (!Files.exists(coversDir)) return null;
        String[] suffixes = {"-disc", "-disk"};
        String[] extensions = {".jpg", ".jpeg", ".png", ".webp"};
        for (String suffix : suffixes) {
            for (String ext : extensions) {
                Path candidate = coversDir.resolve(nameWithoutExt + suffix + ext);
                if (Files.exists(candidate)) {
                    return nameWithoutExt + suffix + ext;
                }
            }
        }
        return null;
    }
}
