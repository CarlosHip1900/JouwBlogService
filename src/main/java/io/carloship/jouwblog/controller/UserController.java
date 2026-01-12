package io.carloship.jouwblog.controller;

import io.carloship.jouwblog.response.User;
import io.carloship.jouwblog.service.UserService;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.Async;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
@ExecuteOn(TaskExecutors.IO)
@Controller("/user/")
public class UserController {

    @Inject
    protected UserService service;

    @Async
    @Get("/{userId}")
    CompletableFuture<MutableHttpResponse<?>> findUser(@NonNull @NotNull @PathVariable String userId){
        if (userId.isBlank()) {
             return CompletableFuture.completedFuture(HttpResponse.badRequest("userId cannot be null"));
        }

        return service.findUser(userId).thenApply(result -> {
            if (result == null){
                return HttpResponse.notFound("User with id " + userId + " not found");
            }

            return HttpResponse.created(result);
        }).exceptionally(ex -> {
            log.error(ex.getMessage());
            return HttpResponse.serverError();
        });
    }

    @Async
    @Post
    CompletableFuture<User> postUser(@NonNull @NotNull @Valid User user){
        return service.saveUser(user);
    }

    @Async
    @Delete("/{userId}")
    CompletableFuture<MutableHttpResponse<?>> deleteUser(@NonNull @NotNull @PathVariable String userId){
        if (userId.isBlank()) {
            return CompletableFuture.completedFuture(HttpResponse.badRequest("userId cannot be null"));
        }

        return service.deleteUser(userId).thenApply(_ -> HttpResponse.ok());
    }


}
