package com.smarttaxi.agent;

/** Average results of an evaluation run. successRate is between 0.0 and 1.0. */
public record EvaluationSummary(String agentName, int episodes, double averageReward,
                                double averageSteps, double successRate) {
}
