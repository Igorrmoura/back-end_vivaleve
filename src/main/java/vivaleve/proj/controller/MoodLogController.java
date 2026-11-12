package vivaleve.proj.controller;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vivaleve.proj.dto.MoodLogDto;
import vivaleve.proj.model.enums.MoodType;
import vivaleve.proj.service.MoodLogService;

import java.util.List;

@RestController
@RequestMapping("/api/mood-logs")
@RequiredArgsConstructor
public class MoodLogController {

        private final MoodLogService service;

        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<MoodLogDto.MoodLogResponse> create(
                @RequestParam Long userId,   // trocar pelo usuário autenticado (JWT)
                @RequestParam @NotNull MoodType mood,
                @RequestParam(required = false) @Size(max = 500) String note,
                @RequestParam(required = false) MultipartFile image) {

            var response = service.create(userId, new MoodLogDto.MoodLogRequest(mood, note), image);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @GetMapping("/today")
        public MoodLogDto.MoodLogResponse today(@RequestParam Long userId) {
            return service.getToday(userId);
        }

        @GetMapping
        public Page<MoodLogDto.MoodLogResponse> history(@RequestParam Long userId, Pageable pageable) {
            return service.history(userId, pageable);
        }

    @GetMapping("/all")
    public List<MoodLogDto.MoodLogResponse> listAll(@RequestParam Long userId) {
        return service.listAll(userId);
    }

        @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public MoodLogDto.MoodLogResponse update(@PathVariable Long id, @RequestParam Long userId,
                                                 @RequestParam @NotNull MoodType mood,
                                                 @RequestParam(required = false) String note,
                                                 @RequestParam(required = false) MultipartFile image) {
            return service.update(userId, id, new MoodLogDto.MoodLogRequest(mood, note), image);
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam Long userId) {
            service.delete(userId, id);
            return ResponseEntity.noContent().build();
        }
}

