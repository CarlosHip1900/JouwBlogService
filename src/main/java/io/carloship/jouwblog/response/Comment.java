package io.carloship.jouwblog.response;

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
public class Comment implements HashResponse {

    @BsonId
    @GeneratedValue
    private String commentId;

    private String postId;
    private String userId;

    private String commentText;

    private int likes;

    @Override
    @BsonIgnore
    public Comment fromMap(Map<String, String> map) {
        return new Comment(
                map.get("commentId"),
                map.get("postId"),
                map.get("userId"),
                map.get("commentText"),
                Integer.parseInt(map.get("likes"))
        );
    }

    @Override
    @BsonIgnore
    public Map<String, String> toMap() {
        return Map.of(
                "commentId", getCommentId(),
                "postId", getPostId(),
                "userId", getUserId(),
                "commentText", getCommentText(),
                "likes", String.valueOf(getLikes())
        );
    }
}
