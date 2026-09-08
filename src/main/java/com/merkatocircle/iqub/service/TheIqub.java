package com.merkatocircle.iqub.service;

import com.merkatocircle.iqub.domain.Iqub;
import com.merkatocircle.iqub.repository.IqubRepository;
import org.springframework.stereotype.Component;

/**
 * Tier 1 only ever has one Iqub group (spec §1) — this is the single place that assumption
 * lives, so Tier 2's multi-group support only has to change this one class, not every
 * controller that currently calls {@link #get()}.
 */
@Component
public class TheIqub {

    private final IqubRepository iqubRepository;

    public TheIqub(IqubRepository iqubRepository) {
        this.iqubRepository = iqubRepository;
    }

    public Iqub get() {
        return iqubRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No Iqub group has been created yet"));
    }
}
