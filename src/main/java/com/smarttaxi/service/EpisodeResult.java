package com.smarttaxi.service;

import java.util.List;

import com.smarttaxi.model.Action;
import com.smarttaxi.model.Position;

/** Everything recorded about one finished episode. Accessors are named like the fields: totalReward(), steps(), ... */
public record EpisodeResult(
        int episodeNo,
        double totalReward,
        int steps,
        double epsilon,
        boolean success,
        double fare,
        double cost,
        double profit,
        Position startTaxi,
        Position pickup,
        Position destination,
        List<Action> actions) {
}
