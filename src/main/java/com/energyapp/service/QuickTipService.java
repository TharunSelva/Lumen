package com.energyapp.service;

import com.energyapp.db.QuickTipRepository;
import com.energyapp.model.QuickTip;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class QuickTipService {

    private static final QuickTipService INSTANCE = new QuickTipService(new QuickTipRepository(), new Random());

    private final QuickTipRepository repository;
    @SuppressWarnings("unused")
    private final Random random;
    private List<QuickTip> cachedTips = new ArrayList<>();

    QuickTipService(QuickTipRepository repository, Random random) {
        this.repository = repository;
        this.random = random;
    }

    public static QuickTipService getInstance() {
        return INSTANCE;
    }

    public synchronized List<QuickTip> getAllTips() {
        ensureTipsLoaded();
        return new ArrayList<>(cachedTips);
    }

    public synchronized String getContinuousMarqueeText() {
        ensureTipsLoaded();

        if (cachedTips.isEmpty()) {
            return "Quick tips unavailable right now.";
        }

        List<String> selections = new ArrayList<>();
        for (QuickTip tip : cachedTips) {
            selections.add(tip.toMarqueeText());
        }

        return String.join("   |   ", selections);
    }

    private void ensureTipsLoaded() {
        if (!cachedTips.isEmpty()) {
            return;
        }

        cachedTips = repository.fetchAllTips();
    }
}
