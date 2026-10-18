package com.smarttaxi.db;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.smarttaxi.model.Action;
import com.smarttaxi.model.Position;
import com.smarttaxi.service.EpisodeResult;

/**
 * One stored trip. Positions are stored as "row,col" and the path as "SOUTH,EAST,PICKUP,...".
 * Use id 0 and runId 0 for a trip that has not been saved yet.
 */
public record Trip(int id, int runId, int episodeNo, double totalReward, int steps, boolean success,
                   double fare, double cost, double profit,
                   String startTaxi, String pickup, String destination, String actions) {

    public static Trip fromEpisode(int runId, EpisodeResult r) {
        String path = r.actions().stream().map(Action::name).collect(Collectors.joining(","));
        return new Trip(0, runId, r.episodeNo(), r.totalReward(), r.steps(), r.success(),
                r.fare(), r.cost(), r.profit(), r.startTaxi().toString(), r.pickup().toString(),
                r.destination().toString(), path);
    }

    public Trip withRunId(int newRunId) {
        return new Trip(id, newRunId, episodeNo, totalReward, steps, success, fare, cost, profit,
                startTaxi, pickup, destination, actions);
    }

    public static Position parsePosition(String text) {
        String[] parts = text.split(",");
        return new Position(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
    }

    public List<Action> actionList() {
        List<Action> list = new ArrayList<>();
        if (actions != null && !actions.isBlank()) {
            for (String name : actions.split(",")) {
                list.add(Action.valueOf(name.trim()));
            }
        }
        return list;
    }
}
