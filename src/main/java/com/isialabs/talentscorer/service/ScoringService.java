package com.isialabs.talentscorer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isialabs.talentscorer.dto.ScoreResultDTO;
import com.isialabs.talentscorer.model.Candidate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScoringService {

    private List<Candidate> candidates = new ArrayList<>();
    private final ObjectMapper objectMapper;

    // Mapas de puntuación: Rol -> (Item -> Puntaje)
    private final Map<String, Map<String, Integer>> professionScores = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, Map<String, Integer>> stackScores = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, Map<String, Integer>> skillScores = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, Integer> maxScoreByRole = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public ScoringService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        initializeScores();
    }

    private void initializeScores() {
        // --- PROFESIÓN ---
        Map<String, Integer> profArq = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        profArq.put("Ingeniero de Sistemas", 10);
        profArq.put("Ingeniero de Software", 10);
        profArq.put("Ciencias de la Computación", 9);
        profArq.put("Ingeniero Electrónico", 8);
        professionScores.put("Arquitecto de Software", profArq);

        Map<String, Integer> profSr = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        profSr.put("Ingeniero de Software", 10);
        profSr.put("Ingeniero de Sistemas", 10);
        profSr.put("Ciencias de la Computación", 9);
        profSr.put("Tecnólogo en Software", 7);
        professionScores.put("Desarrollador Senior", profSr);

        Map<String, Integer> profJr = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        profJr.put("Ingeniero de Sistemas", 9);
        profJr.put("Tecnólogo en Software", 8);
        profJr.put("Bootcamp desarrollo", 7);
        profJr.put("Técnico en Sistemas", 6);
        professionScores.put("Desarrollador Junior", profJr);

        Map<String, Integer> profQA = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        profQA.put("Ingeniero de Sistemas", 9);
        profQA.put("Ingeniero de Software", 9);
        profQA.put("Tecnólogo en Software", 8);
        profQA.put("Técnico en Sistemas", 7);
        professionScores.put("Analista QA", profQA);

        // --- STACK ---
        Map<String, Integer> stackArq = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        stackArq.put("Microservicios", 10);
        stackArq.put("AWS/Cloud", 10);
        stackArq.put("Kubernetes", 10);
        stackArq.put("Docker", 9);
        stackArq.put("Spring Boot", 9);
        stackScores.put("Arquitecto de Software", stackArq);

        Map<String, Integer> stackSr = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        stackSr.put("Java/.NET", 10);
        stackSr.put("Spring Boot", 9);
        stackSr.put("Node.js", 9);
        stackSr.put("SQL", 8);
        stackSr.put("Git", 8);
        stackScores.put("Desarrollador Senior", stackSr);

        Map<String, Integer> stackJr = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        stackJr.put("JavaScript", 8);
        stackJr.put("HTML/CSS", 7);
        stackJr.put("React/Angular", 7);
        stackJr.put("Git", 7);
        stackJr.put("APIs REST", 7);
        stackScores.put("Desarrollador Junior", stackJr);

        Map<String, Integer> stackQA = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        stackQA.put("Selenium", 10);
        stackQA.put("JUnit/TestNG", 9);
        stackQA.put("Postman", 9);
        stackQA.put("APIs REST", 8);
        stackQA.put("SQL", 7);
        stackScores.put("Analista QA", stackQA);

        // --- SKILLS ---
        Map<String, Integer> skillArq = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        skillArq.put("Arquitectura de software", 10);
        skillArq.put("Diseño de sistemas distribuidos", 10);
        skillArq.put("Patrones de diseño", 10);
        skillArq.put("Liderazgo técnico", 9);
        skillArq.put("Toma de decisiones", 9);
        skillScores.put("Arquitecto de Software", skillArq);

        Map<String, Integer> skillSr = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        skillSr.put("Programación avanzada", 10);
        skillSr.put("Clean Code", 10);
        skillSr.put("Patrones de diseño", 9);
        skillSr.put("Resolución de problemas", 9);
        skillSr.put("Testing", 8);
        skillScores.put("Desarrollador Senior", skillSr);

        Map<String, Integer> skillJr = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        skillJr.put("Lógica de programación", 9);
        skillJr.put("Programación básica", 8);
        skillJr.put("Consumo de APIs", 7);
        skillJr.put("Trabajo en equipo", 8);
        skillJr.put("Adaptabilidad", 9);
        skillScores.put("Desarrollador Junior", skillJr);

        Map<String, Integer> skillQA = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        skillQA.put("Testing manual", 10);
        skillQA.put("Testing automatizado", 9);
        skillQA.put("Atención al detalle", 10);
        skillQA.put("Pensamiento crítico", 9);
        skillQA.put("Documentación de pruebas", 8);
        skillScores.put("Analista QA", skillQA);

        // --- MAX SCORES ---
        maxScoreByRole.put("Arquitecto de Software", 106);
        maxScoreByRole.put("Desarrollador Senior", 110);
        maxScoreByRole.put("Desarrollador Junior", 93);
        maxScoreByRole.put("Analista QA", 107);
    }

    @PostConstruct
    public void loadCandidates() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/candidatos.json")) {
            if (inputStream == null) {
                // Si no se encuentra como recurso, intentamos cargarlo desde el sistema de
                // archivos relativo
                // En un proyecto Spring Boot estándar, src/main/resources termina en el
                // classpath.
                // Pero si estamos en desarrollo, a veces es más seguro buscarlo.
                // Sin embargo, getResourceAsStream("/candidatos.json") debería funcionar si
                // está en src/main/resources.
                return;
            }
            candidates = objectMapper.readValue(inputStream, new TypeReference<List<Candidate>>() {
            });
        }
    }

    public List<ScoreResultDTO> getScoredCandidates() {
        return candidates.stream()
                .map(this::scoreCandidate)
                .sorted(Comparator.comparingDouble(ScoreResultDTO::getScore).reversed())
                .collect(Collectors.toList());
    }

    private ScoreResultDTO scoreCandidate(Candidate candidate) {
        double totalScore = 0;
        String role = candidate.getRole();

        // Puntaje Profesión
        totalScore += getItemScore(professionScores, role, candidate.getProfession());

        // Puntaje Stack
        if (candidate.getStack() != null) {
            for (String tech : candidate.getStack()) {
                totalScore += getItemScore(stackScores, role, tech);
            }
        }

        // Puntaje Skills
        if (candidate.getSkills() != null) {
            for (String skill : candidate.getSkills()) {
                totalScore += getItemScore(skillScores, role, skill);
            }
        }

        Integer maxScore = maxScoreByRole.get(role);
        double finalScore = totalScore;
        if (maxScore != null && maxScore > 0) {
            double normalized = (totalScore / maxScore) * 100;
            finalScore = Math.round(normalized * 10.0) / 10.0;
        }

        return new ScoreResultDTO(candidate.getName(), finalScore);
    }

    private int getItemScore(Map<String, Map<String, Integer>> mainMap, String role, String item) {
        if (role == null || item == null)
            return 0;
        Map<String, Integer> roleMap = mainMap.get(role);
        if (roleMap != null) {
            return roleMap.getOrDefault(item, 0);
        }
        return 0;
    }
}
