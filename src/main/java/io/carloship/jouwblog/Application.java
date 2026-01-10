package io.carloship.jouwblog;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.info.*;

import java.time.Duration;

@OpenAPIDefinition(
    info = @Info(
            title = "JouwBlog",
            version = "0.0"
    )
)
public class Application {

    public static final Duration DEFAULT_REDIS_TIME = Duration.ofMinutes(30);

    static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }

}
