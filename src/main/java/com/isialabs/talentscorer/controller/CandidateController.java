package com.isialabs.talentscorer.controller;

import com.isialabs.talentscorer.dto.ScoreResultDTO;
import com.isialabs.talentscorer.model.Candidate;
import com.isialabs.talentscorer.service.ScoringService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/candidatos")
public class CandidateController {

    private final ScoringService scoringService;

    // Inyección por constructor
    public CandidateController(ScoringService scoringService) {
        this.scoringService = scoringService;
    }

    @GetMapping("/score")
    public ResponseEntity<List<ScoreResultDTO>> getScoredCandidates() {
        try {
            List<ScoreResultDTO> scoredCandidates = scoringService.getScoredCandidates();
            return ResponseEntity.ok(scoredCandidates);
        } catch (Exception e) {
            // Manejo de excepciones simple
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/score")
    public ResponseEntity<List<ScoreResultDTO>> scoreCandidates(@RequestBody List<Candidate> candidates) {
        try {
            if (candidates == null || candidates.isEmpty()) {
                // Fallback: si el body está vacío, usa los del archivo
                return ResponseEntity.ok(scoringService.getScoredCandidates());
            }
            List<ScoreResultDTO> results = scoringService.scoreCandidates(candidates);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
