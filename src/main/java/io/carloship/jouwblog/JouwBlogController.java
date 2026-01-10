package io.carloship.jouwblog;

import io.micronaut.http.annotation.*;

@Controller("/jouwBlog")
public class JouwBlogController {

    @Get(uri="/", produces="text/plain")
    public String index() {
        return "Example Response";
    }
}