package org.example.quoteotd.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.example.quoteotd.dto.ViewerRequest;
import org.example.quoteotd.dto.ViewerResponse;
import org.example.quoteotd.service.ViewerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/viewers")
public class ViewerController {

    private final ViewerService viewerService;

    public ViewerController(ViewerService viewerService) {
        this.viewerService = viewerService;
    }

    @GetMapping
    public List<ViewerResponse> findAll() {
        return viewerService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ViewerResponse> findById(@PathVariable UUID id) {
        return viewerService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ViewerResponse> create(@RequestBody ViewerRequest request) {
        ViewerResponse created = viewerService.create(request);
        return ResponseEntity.created(URI.create("/api/viewers/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ViewerResponse> update(@PathVariable UUID id, @RequestBody ViewerRequest request) {
        return viewerService.update(id, request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        if (!viewerService.deleteById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
