package org.example.dto;

/**
 * 音乐歌曲数据传输对象
 */
public class MusicSongDto {
    private Long id;
    private String name;
    private String artist;
    private String genre;        // 音乐风格
    private String genreSource;  // 风格来源: CACHE/DATABASE/AI/DEFAULT

    public MusicSongDto() {
    }

    public MusicSongDto(Long id, String name, String artist) {
        this.id = id;
        this.name = name;
        this.artist = artist;
    }

    public MusicSongDto(Long id, String name, String artist, String genre, String genreSource) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.genre = genre;
        this.genreSource = genreSource;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getGenreSource() {
        return genreSource;
    }

    public void setGenreSource(String genreSource) {
        this.genreSource = genreSource;
    }
}
