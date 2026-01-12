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
public class User implements HashResponse {

    @BsonId
    @GeneratedValue
    private String id;

    private String username;
    private String name;

    private String email;

    @Override
    @BsonIgnore
    public User fromMap(Map<String, String> result){
        return new User(
                result.get("id"),
                result.get("username"),
                result.get("name"),
                result.get("email")
        );
    }

    @Override
    @BsonIgnore
    public Map<String, String> toMap(){
        return Map.of(
                "id", getId(),
                "username", getUsername(),
                "name", getName(),
                "email", getEmail()
        );
    }

}
