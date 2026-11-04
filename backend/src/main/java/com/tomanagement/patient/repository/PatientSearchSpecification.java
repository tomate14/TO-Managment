package com.tomanagement.patient.repository;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.tomanagement.patient.entity.Dni;
import com.tomanagement.patient.entity.Patient;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

/**
 * Specification pattern for H4.2: every word of the query must appear (partially, ignoring case) in the DNI, the
 * first name or the last name. "lucía fernández" and "30.123" both work. A blank query matches everyone.
 */
public final class PatientSearchSpecification implements Specification<Patient> {

    private static final String WILDCARD = "%";
    private static final String WORD_SEPARATOR = "\\s+";
    private static final String DIGITS = "\\d+";
    private static final char ESCAPE = '\\';

    private final List<String> words;

    private PatientSearchSpecification(List<String> words) {
        this.words = words;
    }

    public static PatientSearchSpecification matching(String query) {
        String text = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<String> words = text.isEmpty() ? List.of() : Arrays.asList(text.split(WORD_SEPARATOR));
        return new PatientSearchSpecification(words);
    }

    @Override
    public Predicate toPredicate(Root<Patient> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        Expression<String> dni = root.<String>get("dni");
        Expression<String> firstName = builder.lower(root.<String>get("firstName"));
        Expression<String> lastName = builder.lower(root.<String>get("lastName"));
        Predicate[] perWord = words.stream()
                .map(word -> {
                    String pattern = WILDCARD + escape(word) + WILDCARD;
                    String digits = Dni.normalize(word);
                    Predicate byDni = digits.matches(DIGITS)
                            ? builder.like(dni, WILDCARD + digits + WILDCARD)
                            : builder.disjunction();
                    return builder.or(
                            builder.like(firstName, pattern, ESCAPE),
                            builder.like(lastName, pattern, ESCAPE),
                            byDni);
                })
                .toArray(Predicate[]::new);
        return builder.and(perWord);
    }

    /** Escapes LIKE wildcards typed by the user so "%" and "_" are searched literally. */
    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
