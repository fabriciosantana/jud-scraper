package br.edu.idp.mcdia.dl.judscraper.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DatajudResponse(Hits hits) {

    public record Hits(
            Total total,
            @JsonProperty("max_score") Double maxScore,
            List<ProcessHit> hits) {
    }

    public record Total(long value, String relation) {
    }

    public record ProcessHit(
            @JsonProperty("_index") String index,
            @JsonProperty("_id") String id,
            @JsonProperty("_score") Double score,
            @JsonProperty("_source") Processo processo,
            @JsonProperty("sort") List<Object> sort) {
    }
}
