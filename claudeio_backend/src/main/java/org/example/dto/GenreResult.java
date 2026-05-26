package org.example.dto;

/**
 * 风格识别结果
 */
public class GenreResult {
    private String genre;
    private String source;  // CACHE/DATABASE/AI/DEFAULT
    private int confidence;

    public GenreResult(String genre, String source, int confidence) {
        this.genre = genre;
        this.source = source;
        this.confidence = confidence;
    }

    public static GenreResult fromCache(String genre) {
        return new GenreResult(genre, "CACHE", 100);
    }

    public static GenreResult fromDatabase(String genre) {
        return new GenreResult(genre, "DATABASE", 95);
    }

    public static GenreResult fromAI(String genre) {
        return new GenreResult(genre, "AI", 85);
    }

    public static GenreResult fromDefault(String genre) {
        return new GenreResult(genre, "DEFAULT", 50);
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getConfidence() {
        return confidence;
    }

    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }
}
