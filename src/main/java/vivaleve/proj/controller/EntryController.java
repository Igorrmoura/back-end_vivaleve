package vivaleve.proj.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vivaleve.proj.dto.EntryDtos.EntryRequest;
import vivaleve.proj.dto.EntryDtos.EntryResponse;
import vivaleve.proj.service.EntryService;

import java.util.List;

@RestController
@RequestMapping("/v1/entries")
@RequiredArgsConstructor
@Validated
public class EntryController {

    private final EntryService entryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EntryResponse criar(@Valid @RequestBody EntryRequest request) {
        return entryService.criar(request);
    }

    @GetMapping
    public List<EntryResponse> listar(@RequestParam Long userId) {
        return entryService.listarPorUsuario(userId);
    }

    @GetMapping("/{id}")
    public EntryResponse buscar(@PathVariable Long id) {
        return entryService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public EntryResponse atualizar(@PathVariable Long id, @Valid @RequestBody EntryRequest request) {
        return entryService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        entryService.deletar(id);
    }
}
