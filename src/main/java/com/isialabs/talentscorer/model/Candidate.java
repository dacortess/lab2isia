package com.isialabs.talentscorer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Candidate {
    private String name;
    private String role;
    private String profession;
    private List<String> stack;
    private List<String> skills;
}
