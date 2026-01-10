package io.carloship.jouwblog.response.impl;

import io.carloship.jouwblog.response.Post;
import io.carloship.jouwblog.response.User;
import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@Serdeable
@AllArgsConstructor
public class CachedUser {

    private User user;
    private List<Post> posts;

}
