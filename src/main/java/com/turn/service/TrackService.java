package com.turn.service;

import com.turn.model.Track;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TrackService {

    private final List<Track> tracks = new ArrayList<>();
    private final ResourcePatternResolver resolver;

    // 파일명 → Resource. classpath 기반이라 개발 실행/JAR 패키징 모두에서 동작한다.
    private final Map<String, Resource> musicResources = new LinkedHashMap<>();
    private final Map<String, Resource> coverResources = new LinkedHashMap<>();

    private static final String[] LABEL_COLORS = {
        "#cc3333", "#3366cc", "#339933", "#cc9933", "#9933cc",
        "#cc6633", "#3399cc", "#66cc33", "#cc3399", "#33cc99"
    };

    public TrackService(ResourceLoader resourceLoader) {
        this.resolver = new PathMatchingResourcePatternResolver(resourceLoader);
    }

    @PostConstruct
    public void init() {
        loadResources("classpath*:music/*", musicResources);
        loadResources("classpath*:covers/*", coverResources);
        scanMusicDirectory();
    }

    // 패턴에 매칭되는 리소스를 파일명 → Resource 맵으로 적재 (JAR 안에서도 jar 엔트리를 열거함)
    private void loadResources(String pattern, Map<String, Resource> target) {
        target.clear();
        try {
            for (Resource r : resolver.getResources(pattern)) {
                if (!r.isReadable()) continue;
                String name = r.getFilename();
                if (name == null || name.isEmpty()) continue;
                target.put(name, r);
            }
        } catch (IOException e) {
            System.err.println("[TrackService] Error loading " + pattern + ": " + e.getMessage());
        }
    }

    private void scanMusicDirectory() {
        tracks.clear();
        AtomicLong idCounter = new AtomicLong(1);

        musicResources.keySet().stream()
            .filter(name -> {
                String n = name.toLowerCase();
                return n.endsWith(".mp3") || n.endsWith(".wav")
                    || n.endsWith(".ogg") || n.endsWith(".flac");
            })
            // fireplace, chair-creak 등 효과음 제외
            .filter(name -> {
                String n = name.toLowerCase();
                return !n.contains("fireplace") && !n.contains("chair")
                    && !n.contains("creak") && !n.contains("sfx");
            })
            .sorted()
            .forEach(filename -> {
                long id = idCounter.getAndIncrement();
                String nameWithoutExt = filename.replaceAll("\\.[^.]+$", "");

                // 파일명 규칙: "Artist - Title" → 아티스트/제목 분리.
                // " - "(공백-하이픈-공백) 구분자가 없으면 아티스트 미지정(null).
                String artist = null;
                String titleRaw = nameWithoutExt;
                int sep = nameWithoutExt.indexOf(" - ");
                if (sep > 0) {
                    artist = nameWithoutExt.substring(0, sep).trim();
                    titleRaw = nameWithoutExt.substring(sep + 3).trim();
                }
                // 제목: 언더스코어 → 공백, 첫 글자 대문자
                String title = titleRaw.replace("_", " ").trim();
                if (!title.isEmpty()) {
                    title = title.substring(0, 1).toUpperCase() + title.substring(1);
                }

                int colorIdx = (int) ((id - 1) % LABEL_COLORS.length);
                String coverImage = findImage(nameWithoutExt, false);
                String discImage = findImage(nameWithoutExt, true);
                tracks.add(new Track(id, title, artist, "Vinyl Collection",
                    0, filename, LABEL_COLORS[colorIdx],
                    coverImage, versionOf(coverImage), discImage, versionOf(discImage)));
            });

        System.out.println("[TrackService] Loaded " + tracks.size() + " tracks"
            + " (music=" + musicResources.size() + ", covers=" + coverResources.size() + ")");
        tracks.forEach(t -> System.out.println("  - " + t.getId() + ": "
            + t.getTitle() + " / " + (t.getArtist() == null ? "(no artist)" : t.getArtist())));
    }

    public List<Track> getAllTracks() {
        return tracks;
    }

    public Optional<Track> getTrackById(Long id) {
        return tracks.stream().filter(t -> t.getId().equals(id)).findFirst();
    }

    public Resource getTrackResource(String filename) {
        return musicResources.get(filename);
    }

    public Resource getCoverResource(String coverImage) {
        return coverResources.get(coverImage);
    }

    // 이미지 파일의 mtime을 캐시 버스팅 버전으로 사용 (없으면 0)
    private long versionOf(String imageName) {
        if (imageName == null) return 0L;
        Resource r = coverResources.get(imageName);
        if (r == null) return 0L;
        try {
            return r.lastModified();
        } catch (IOException e) {
            return 0L;
        }
    }

    // 커버: <basename>.{ext}, 디스크: <basename>-disc/-disk.{ext} 를 covers 리소스에서 찾는다.
    private String findImage(String nameWithoutExt, boolean disc) {
        String[] extensions = {".jpg", ".jpeg", ".png", ".webp"};
        String[] bases = disc
            ? new String[]{ nameWithoutExt + "-disc", nameWithoutExt + "-disk" }
            : new String[]{ nameWithoutExt };
        for (String base : bases) {
            for (String ext : extensions) {
                String candidate = base + ext;
                if (coverResources.containsKey(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }
}
