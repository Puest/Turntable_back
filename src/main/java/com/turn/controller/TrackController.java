package com.turn.controller;

import com.turn.model.Track;
import com.turn.service.TrackService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TrackController {

    private final TrackService trackService;

    public TrackController(TrackService trackService) {
        this.trackService = trackService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/tracks")
    public ResponseEntity<List<Track>> getTracks() {
        return ResponseEntity.ok(trackService.getAllTracks());
    }

    @GetMapping("/tracks/{id}")
    public ResponseEntity<Track> getTrack(@PathVariable Long id) {
        return trackService.getTrackById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tracks/{id}/cover")
    public ResponseEntity<Resource> getCover(@PathVariable Long id) {
        return trackService.getTrackById(id)
                .filter(track -> track.getCoverImage() != null)
                .map(track -> serveImage(track.getCoverImage()))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/tracks/{id}/disc")
    public ResponseEntity<Resource> getDisc(@PathVariable Long id) {
        return trackService.getTrackById(id)
                .filter(track -> track.getDiscImage() != null)
                .map(track -> serveImage(track.getDiscImage()))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // 커버/디스크 이미지 공통 서빙.
    // no-cache + Last-Modified로, 파일을 같은 이름으로 교체해도(=mtime 변경) 브라우저가
    // 재검증해 새 이미지를 받고, 변경 없으면 304로 빠르게 응답한다.
    private ResponseEntity<Resource> serveImage(String imageName) {
        Resource resource = trackService.getCoverResource(imageName);
        if (resource == null || !resource.exists()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        String f = imageName.toLowerCase();
        String mime = f.endsWith(".png") ? "image/png"
                   : f.endsWith(".webp") ? "image/webp"
                   : "image/jpeg";
        long lastMod;
        try {
            lastMod = resource.lastModified();
        } catch (IOException e) {
            lastMod = -1;
        }
        return (lastMod > 0
                ? ResponseEntity.ok().cacheControl(CacheControl.noCache()).lastModified(lastMod)
                : ResponseEntity.ok().cacheControl(CacheControl.noCache()))
                .contentType(MediaType.parseMediaType(mime))
                .body(resource);
    }

    @GetMapping("/tracks/{id}/stream")
    public ResponseEntity<ResourceRegion> streamTrack(
            @PathVariable Long id,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {

        return trackService.getTrackById(id).map(track -> {
            Resource resource = trackService.getTrackResource(track.getFilename());
            if (resource == null || !resource.exists()) {
                return new ResponseEntity<ResourceRegion>(HttpStatus.NOT_FOUND);
            }
            try {
                long fileLength = resource.contentLength();
                String mime = track.getFilename().endsWith(".wav") ? "audio/wav"
                           : track.getFilename().endsWith(".ogg") ? "audio/ogg"
                           : track.getFilename().endsWith(".flac") ? "audio/flac"
                           : "audio/mpeg";

                ResourceRegion region;
                HttpStatus status;
                if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                    String[] ranges = rangeHeader.substring(6).split("-");
                    long start = Long.parseLong(ranges[0]);
                    long end = ranges.length > 1 && !ranges[1].isEmpty()
                            ? Long.parseLong(ranges[1]) : fileLength - 1;
                    if (end >= fileLength) end = fileLength - 1;
                    // ResourceRegion이 올바른 오프셋부터 바이트를 보내고
                    // Content-Range/Content-Length 헤더를 자동 설정한다 (seek 정상 동작).
                    region = new ResourceRegion(resource, start, end - start + 1);
                    status = HttpStatus.PARTIAL_CONTENT;
                } else {
                    region = new ResourceRegion(resource, 0, fileLength);
                    status = HttpStatus.OK;
                }

                return ResponseEntity.status(status)
                        .contentType(MediaType.parseMediaType(mime))
                        .header("Accept-Ranges", "bytes")
                        .body(region);
            } catch (IOException e) {
                return new ResponseEntity<ResourceRegion>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
