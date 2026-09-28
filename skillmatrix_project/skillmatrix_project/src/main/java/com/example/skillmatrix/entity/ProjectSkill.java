package com.example.skillmatrix.entity;

import jakarta.persistence.*;

/** A skill a project needs, with the minimum proficiency required (1-5). */
@Entity
@Table(name = "project_skills",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "skill_id"}))
public class ProjectSkill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(name = "min_proficiency", nullable = false)
    private int minProficiency;

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }
    public int getMinProficiency() { return minProficiency; }
    public void setMinProficiency(int minProficiency) { this.minProficiency = minProficiency; }
}
