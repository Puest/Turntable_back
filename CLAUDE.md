# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

이 디렉토리는 모노레포(`turnTable/`)의 **backend** git submodule이다. 상위 `../CLAUDE.md`에 프론트엔드를 포함한 전체 그림이 있다.

## Overview

"Vinyl Listening Room" 턴테이블 웹앱의 음악 메타데이터/스트리밍 API. Spring Boot 3.2 + Java 17. **DB 없음** — 시작 시 리소스 디렉토리를 스캔해 트랙 목록을 인메모리로 보관한다.

## Commands

Maven wrapper(`mvnw`)는 이 저장소에 없다. 전역 `mvn`을 사용한다 (상위 CLAUDE.md의 `./mvnw` 표기는 부정확).

```bash
mvn spring-boot:run    # 개발 서버 (localhost:8080), devtools 자동 리로드
mvn package            # JAR 빌드 → target/turntable-0.0.1-SNAPSHOT.jar
mvn test               # 테스트 (현재 테스트 코드 없음 — src/test 미존재)
```

## Architecture

3-layer로 단순하다: `TrackController` (REST) → `TrackService` (스캔/조회) → `Track` (POJO).

### TrackService — 인메모리 트랙 목록의 핵심
- `@PostConstruct init()`에서 `classpath:music/`, `classpath:covers/`를 실제 파일시스템 경로로 해석한 뒤 `scanMusicDirectory()` 실행. classpath 해석 실패 시 `src/main/resources/...` 상대 경로로 폴백.
- **트랙 ID는 정렬된 파일명 순서로 1부터 부여되는 위치 기반 값**이다. 파일을 추가/삭제하면 기존 트랙의 ID가 바뀌므로 안정적 식별자가 아니다.
- 음악 파일 확장자: `.mp3 .wav .ogg .flac`. 이름에 `fireplace / chair / creak / sfx`가 포함된 효과음 파일은 트랙에서 제외한다.
- 메타데이터는 파일에서 추출하지 않고 **합성**한다: artist=`"Unknown Artist"`, album=`"Vinyl Collection"`, durationSeconds=`0`. title은 파일명(확장자 제거)에서 `_`/`-`를 공백으로 바꾸고 첫 글자만 대문자. coverColor는 `LABEL_COLORS` 팔레트를 순환 배정.

### 커버/디스크 이미지 규칙 (파일명 컨벤션 기반)
`covers/` 디렉토리에서 음악 파일의 basename으로 매칭한다:
- 커버: `<basename>.{jpg,jpeg,png,webp}`
- 디스크: `<basename>-disc.{...}` 또는 `<basename>-disk.{...}`
- 매칭 파일이 없으면 해당 필드는 `null`이고, `/cover`·`/disc` 엔드포인트는 404.

### REST API (`/api`, `TrackController`)
- `GET /health` → `{"status":"ok"}`
- `GET /tracks`, `GET /tracks/{id}`
- `GET /tracks/{id}/cover`, `GET /tracks/{id}/disc` — 이미지 바이너리, `Cache-Control: public, max-age=86400`
- `GET /tracks/{id}/stream` — **HTTP Range 요청 지원** (부분 콘텐츠 206). 확장자로 MIME 결정(wav/ogg/flac/mpeg). 프론트의 `<audio>` seek가 이 동작에 의존.

### CORS
`WebConfig`가 `/api/**`에 대해 `http://localhost:5173`만 허용. 프론트 포트가 바뀌면 여기를 수정.

## Gotchas

- **JAR 패키징 시 스캔 방식 한계**: `Files.list(musicDir)`는 디렉토리가 실제 파일시스템 경로일 때만 동작한다. `mvn package`로 만든 JAR 안에서는 `classpath:music/`가 파일시스템 경로로 해석되지 않아 스캔이 실패할 수 있다 — 현재 구조는 개발 실행(`spring-boot:run`) 전제.
- 리소스 디렉토리(`src/main/resources/music/`, `covers/`)에 파일을 추가하는 것이 곧 "트랙 등록"이다. 별도 등록 코드/DB 없음.
