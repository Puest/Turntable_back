package com.turn.controller;

import com.turn.model.Track;
import com.turn.service.TrackService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
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
                .map(track -> {
                    Resource resource = trackService.getCoverResource(track.getCoverImage());
                    if (!resource.exists()) {
                        return new ResponseEntity<Resource>(HttpStatus.NOT_FOUND);
                    }
                    String filename = track.getCoverImage().toLowerCase();
                    String mime = filename.endsWith(".png") ? "image/png"
                               : filename.endsWith(".webp") ? "image/webp"
                               : "image/jpeg";
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.parseMediaType(mime));
                    headers.setCacheControl("public, max-age=86400");
                    return new ResponseEntity<>(resource, headers, HttpStatus.OK);
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/tracks/{id}/disc")
    public ResponseEntity<Resource> getDisc(@PathVariable Long id) {
        return trackService.getTrackById(id)
                .filter(track -> track.getDiscImage() != null)
                .map(track -> {
                    Resource resource = trackService.getCoverResource(track.getDiscImage());
                    if (!resource.exists()) {
                        return new ResponseEntity<Resource>(HttpStatus.NOT_FOUND);
                    }
                    String filename = track.getDiscImage().toLowerCase();
                    String mime = filename.endsWith(".png") ? "image/png"
                               : filename.endsWith(".webp") ? "image/webp"
                               : "image/jpeg";
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.parseMediaType(mime));
                    headers.setCacheControl("public, max-age=86400");
                    return new ResponseEntity<>(resource, headers, HttpStatus.OK);
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/tracks/{id}/stream")
    public ResponseEntity<Resource> streamTrack(
            @PathVariable Long id,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {

        return trackService.getTrackById(id).map(track -> {
            Resource resource = trackService.getTrackResource(track.getFilename());
            try {
                long fileLength = resource.contentLength();
                HttpHeaders headers = new HttpHeaders();
                String mime = track.getFilename().endsWith(".wav") ? "audio/wav"
                           : track.getFilename().endsWith(".ogg") ? "audio/ogg"
                           : track.getFilename().endsWith(".flac") ? "audio/flac"
                           : "audio/mpeg";
                headers.setContentType(MediaType.parseMediaType(mime));
                headers.set("Accept-Ranges", "bytes");

                if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                    String[] ranges = rangeHeader.substring(6).split("-");
                    long start = Long.parseLong(ranges[0]);
                    long end = ranges.length > 1 && !ranges[1].isEmpty()
                            ? Long.parseLong(ranges[1]) : fileLength - 1;
                    long contentLength = end - start + 1;

                    headers.set("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
                    headers.setContentLength(contentLength);

                    return new ResponseEntity<>(resource, headers, HttpStatus.PARTIAL_CONTENT);
                }

                headers.setContentLength(fileLength);
                return new ResponseEntity<>(resource, headers, HttpStatus.OK);
            } catch (IOException e) {
                return new ResponseEntity<Resource>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
