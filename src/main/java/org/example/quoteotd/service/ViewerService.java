package org.example.quoteotd.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.example.quoteotd.dto.ViewerRequest;
import org.example.quoteotd.dto.ViewerResponse;
import org.example.quoteotd.model.Viewer;
import org.example.quoteotd.repository.ViewerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;

@Service
@RequiredArgsConstructor
public class ViewerService {

    private final ViewerRepository viewerRepository;

    @Transactional(readOnly = true)
    public Optional<ViewerResponse> findById(UUID id) {
        return viewerRepository.findById(id).map(ViewerService::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ViewerResponse> findAll() {
        return viewerRepository.findAll().stream().map(ViewerService::toResponse).toList();
    }

    @Transactional
    public ViewerResponse create(ViewerRequest request) {
        return toResponse(viewerRepository.save(new Viewer(request.name())));
    }

    @Transactional
    public Optional<ViewerResponse> update(UUID id, ViewerRequest request) {
        return viewerRepository.findById(id)
                .map(viewer -> {
                    viewer.setName(request.name());
                    return viewerRepository.save(viewer);
                })
                .map(ViewerService::toResponse);
    }

    @Transactional
    public boolean deleteById(UUID id) {
        if (!viewerRepository.existsById(id)) {
            return false;
        }
        viewerRepository.deleteById(id);
        return true;
    }

    private static ViewerResponse toResponse(Viewer viewer) {
        return new ViewerResponse(viewer.getId(), viewer.getName());
    }
}
