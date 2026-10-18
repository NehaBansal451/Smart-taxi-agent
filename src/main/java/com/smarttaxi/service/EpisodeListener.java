package com.smarttaxi.service;

/** Receives each finished episode while training runs. */
public interface EpisodeListener {
    void onEpisode(EpisodeResult result);
}
