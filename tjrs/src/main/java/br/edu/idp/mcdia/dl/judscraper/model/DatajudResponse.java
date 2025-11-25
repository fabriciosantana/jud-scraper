package br.edu.idp.mcdia.dl.judscraper.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class DatajudResponse {

    private Hits hits;

    public Hits getHits() {
        return hits;
    }

    public void setHits(Hits hits) {
        this.hits = hits;
    }

    public static class Hits {
        private Total total;

        @JsonProperty("max_score")
        private Double maxScore;

        private List<ProcessHit> hits;

        public Total getTotal() {
            return total;
        }

        public void setTotal(Total total) {
            this.total = total;
        }

        public Double getMaxScore() {
            return maxScore;
        }

        public void setMaxScore(Double maxScore) {
            this.maxScore = maxScore;
        }

        public List<ProcessHit> getHits() {
            return hits;
        }

        public void setHits(List<ProcessHit> hits) {
            this.hits = hits;
        }
    }

    public static class Total {
        private long value;
        private String relation;

        public long getValue() {
            return value;
        }

        public void setValue(long value) {
            this.value = value;
        }

        public String getRelation() {
            return relation;
        }

        public void setRelation(String relation) {
            this.relation = relation;
        }
    }

    public static class ProcessHit {
        @JsonProperty("_index")
        private String index;

        @JsonProperty("_id")
        private String id;

        @JsonProperty("_score")
        private Double score;

        @JsonProperty("_source")
        private Processo processo;

        @JsonProperty("sort")
        private List<Object> sort;

        public String getIndex() {
            return index;
        }

        public void setIndex(String index) {
            this.index = index;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }

        public Processo getProcesso() {
            return processo;
        }

        public void setProcesso(Processo processo) {
            this.processo = processo;
        }

        public List<Object> getSort() {
            return sort;
        }

        public void setSort(List<Object> sort) {
            this.sort = sort;
        }
    }
}
