package com.atci.quizhub.config;

import com.atci.quizhub.masterdata.*;
import com.atci.quizhub.mcq.*;
import com.atci.quizhub.user.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final TechStackRepository stacks;
    private final TopicRepository topics;
    private final UserRepository users;
    private final UserSkillRepository skills;
    private final McqRepository mcqs;
    private final PasswordEncoder encoder;

    public DataSeeder(TechStackRepository stacks, TopicRepository topics, UserRepository users,
                      UserSkillRepository skills, McqRepository mcqs, PasswordEncoder encoder) {
        this.stacks = stacks; this.topics = topics; this.users = users;
        this.skills = skills; this.mcqs = mcqs; this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (stacks.count() > 0) return; // idempotent

        // --- Tech stacks (slide 15) ---
        TechStack springCloud = stacks.save(new TechStack("Spring Cloud"));
        TechStack springBoot  = stacks.save(new TechStack("Spring Boot"));
        TechStack springCore  = stacks.save(new TechStack("Spring Core"));
        TechStack springMvc   = stacks.save(new TechStack("Spring MVC & REST"));
        TechStack springOrm   = stacks.save(new TechStack("Spring ORM & Data JPA"));
        TechStack coreJava    = stacks.save(new TechStack("Core Java"));

        // --- Topics for Spring Cloud (slide 15) ---
        topics.save(new Topic("Introduction to Spring Cloud", springCloud));
        topics.save(new Topic("Service Discovery design pattern – Eureka Server & Discovery Client", springCloud));
        topics.save(new Topic("Eureka Heartbeats & Self Preservation", springCloud));
        topics.save(new Topic("Spring Cloud Loadbalancer", springCloud));
        topics.save(new Topic("Spring Cloud OpenFeign", springCloud));
        topics.save(new Topic("Resilience4J- Circuit Breaker", springCloud));
        topics.save(new Topic("Spring Boot Actuator", springCloud));
        // a couple of topics for other stacks so dropdowns aren't empty
        topics.save(new Topic("Spring Boot Auto-configuration", springBoot));
        topics.save(new Topic("Spring Boot Starters", springBoot));
        topics.save(new Topic("Beans & Dependency Injection", springCore));
        topics.save(new Topic("REST Controllers", springMvc));
        topics.save(new Topic("JPA Repositories", springOrm));
        topics.save(new Topic("Collections", coreJava));

        // --- Users (enterprise IDs from slide 16). Password = "password" for all. ---
        String pw = encoder.encode("password");
        User gaurav   = users.save(new User("gaurav.a.bhola", "Gaurav Bhola", pw, Role.SME));
        User birendra = users.save(new User("birendra.kumar.singh", "Birendra Kumar Singh", pw, Role.ADMIN));
        User divya    = users.save(new User("divya.madhanasekar", "Divya Madhanasekar", pw, Role.SME));
        User swati    = users.save(new User("swati.avinash.nikam", "Swati Avinash Nikam", pw, Role.SME));
        User indugu   = users.save(new User("indugu.hari.prasad", "Indugu Hari Prasad", pw, Role.SME));

        // --- SME ↔ skill mapping (slide 16) ---
        skills.save(new UserSkill(gaurav, springCloud));
        skills.save(new UserSkill(birendra, springBoot));
        skills.save(new UserSkill(gaurav, springCore));
        skills.save(new UserSkill(divya, springMvc));
        skills.save(new UserSkill(divya, springCloud));
        skills.save(new UserSkill(swati, springBoot));
        skills.save(new UserSkill(indugu, springCloud));

        // --- Sample MCQs (slide 14), created by birendra ---
        Topic introCloud = topics.findByStackId(springCloud.getId()).get(0);
        Mcq m1 = new Mcq();
        m1.setQuestionStem("Alex is building a microservices-based system using Spring Boot. "
            + "Which is the primary purpose of Spring Cloud?");
        m1.setOptionA("To replace Spring Boot completely");
        m1.setOptionB("To provide tools for building distributed systems and microservices");
        m1.setOptionC("To manage only database transactions");
        m1.setOptionD("To handle only UI development");
        m1.setCorrectAnswer(AnswerOption.B);
        m1.setDifficulty(Difficulty.MEDIUM);
        m1.setStack(springCloud); m1.setTopic(introCloud); m1.setCreator(birendra);
        m1.setStatus(McqStatus.READY_FOR_REVIEW);
        mcqs.save(m1);

        Mcq m2 = new Mcq();
        m2.setQuestionStem("John has multiple instances of a service running dynamically in the cloud. "
            + "Which component is used for automatic registration and discovery?");
        m2.setOptionA("Spring MVC");
        m2.setOptionB("Eureka Server");
        m2.setOptionC("Hibernate");
        m2.setOptionD("Apache Tomcat");
        m2.setCorrectAnswer(AnswerOption.B);
        m2.setDifficulty(Difficulty.MEDIUM);
        m2.setStack(springCloud); m2.setTopic(introCloud); m2.setCreator(birendra);
        m2.setStatus(McqStatus.APPROVED);
        mcqs.save(m2);
    }
}
