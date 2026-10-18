package com.smarttaxi.db;

import java.time.LocalDateTime;

/** One saved training run. Use id 0 for a run that has not been saved yet. */
public record TrainingRun(int id, LocalDateTime startedAt, String agentType, int episodes,
                          double alpha, double gamma, double epsilonStart, double epsilonEnd,
                          double avgReward, double successRate, double totalProfit,
                          String mapData, String notes) {

    public TrainingRun withNotes(String newNotes) {
        return new TrainingRun(id, startedAt, agentType, episodes, alpha, gamma, epsilonStart, epsilonEnd,
                avgReward, successRate, totalProfit, mapData, newNotes);
    }
}
