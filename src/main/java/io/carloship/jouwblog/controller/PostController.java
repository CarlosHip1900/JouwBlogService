package io.carloship.jouwblog.controller;

import io.carloship.jouwblog.service.PostService;
import io.micronaut.http.annotation.Controller;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Inject;

@ExecuteOn(TaskExecutors.IO)
@Controller("/posts/")
public class PostController {

    @Inject
    protected PostService service;

}
