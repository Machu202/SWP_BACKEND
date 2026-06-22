package com.mangastudio.backend.controller;

import com.mangastudio.backend.dto.MangaSeriesRequest;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.MangaSeriesRepository;
import com.mangastudio.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/mangas")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@RequiredArgsConstructor
public class MangaSeriesController {

    private final MangaSeriesRepository mangaSeriesRepository;
    private final UserRepository userRepository;

    // ==========================================
    // 1. CREATE A NEW MANGA SERIES (FE-11)
    // ==========================================
    @PostMapping
    public ResponseEntity<?> createMangaSeries(@RequestBody MangaSeriesRequest request) {
        // Step 1: Verify if the user exists
        Optional<User> mangakaOpt = userRepository.findById(request.getMangakaId());
        
        if (mangakaOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mangaka ID not found!");
        }

        User mangaka = mangakaOpt.get();

        // Step 2: Ensure only users with the MANGAKA role can create a series
        if (!mangaka.getRole().getRoleName().equals("MANGAKA")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only users with MANGAKA role can create a series.");
        }

        // Step 3: Initialize the entity and set default business rules
        MangaSeries newSeries = new MangaSeries();
        newSeries.setTitle(request.getTitle());
        newSeries.setGenre(request.getGenre());
        newSeries.setSummary(request.getSummary());
        newSeries.setMangaka(mangaka);
        
        // Core Rule: All new creations must start at DRAFT status
        newSeries.setStatus("DRAFT"); 

        MangaSeries savedSeries = mangaSeriesRepository.save(newSeries);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedSeries);
    }

    // ==========================================
    // 2. GET MANGA LIST BY MANGAKA ID (FE-11)
    // ==========================================
    @GetMapping("/mangaka/{mangakaId}")
    public ResponseEntity<?> getMangasByMangaka(@PathVariable Long mangakaId) {
        
        // Ensure this method matches the one defined in your MangaSeriesRepository
        List<MangaSeries> seriesList = mangaSeriesRepository.findByMangaka_UserId(mangakaId);
        
        if (seriesList.isEmpty()) {
            return ResponseEntity.ok("No series found for this Mangaka.");
        }
        
        return ResponseEntity.ok(seriesList);
    }

    // ==========================================
    // 3. UPDATE SERIES STATUS (FE-13: State Machine transition)
    // ==========================================
    @PutMapping("/{seriesId}/status")
    public ResponseEntity<?> updateSeriesStatus(@PathVariable Long seriesId, @RequestParam String newStatus) {
        
        Optional<MangaSeries> seriesOpt = mangaSeriesRepository.findById(seriesId);
        
        if (seriesOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Manga Series not found!");
        }
        
        MangaSeries series = seriesOpt.get();
        
        // Convert status to uppercase to maintain data consistency (e.g., PENDING_EDITOR)
        series.setStatus(newStatus.toUpperCase()); 
        mangaSeriesRepository.save(series);
        
        return ResponseEntity.ok("Manga Series status successfully updated to: " + newStatus.toUpperCase());
    }
}