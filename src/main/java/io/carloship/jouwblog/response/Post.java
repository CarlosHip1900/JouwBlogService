package io.carloship.jouwblog.response;

import io.carloship.jouwblog.response.impl.HashResponse;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import java.util.Map;

@Data
@Serdeable
@MappedEntity
@NoArgsConstructor
@AllArgsConstructor
public class Post implements HashResponse {

    @BsonId
    @GeneratedValue
    private String postId;

    private String userId;

    private String title;
    private long postTimestamp;
    private long updateTimestamp;

    private String text;

    private int likes;
    private int replies;

    @Override
    @BsonIgnore
    public Post fromMap(Map<String, String> map) {
        return new Post(
                map.get("postId"),
                map.get("userId"),
                map.get("title"),
                Long.parseLong(map.get("postTimestamp")),
                Long.parseLong(map.get("updateTimestamp")),
                map.get("text"),
                Integer.parseInt(map.get("likes")),
                Integer.parseInt(map.get("replies"))
        );
    }

    @Override
    @BsonIgnore
    public Map<String, String> toMap() {
        return Map.of(
                "postId", getPostId(),
                "userId", getUserId(),
                "title", getTitle(),
                "postTimestamp", String.valueOf(getPostTimestamp()),
                "updateTimestamp", String.valueOf(getUpdateTimestamp()),
                "text", getText(),
                "likes", String.valueOf(getLikes()),
                "replies", String.valueOf(getReplies())
        );
    }
}
