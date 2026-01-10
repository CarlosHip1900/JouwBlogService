package io.carloship.jouwblog.runtime;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlogThreads {

    public static final ExecutorService USER_EXECUTOR;
    public static final ExecutorService POST_EXECUTOR;
    public static final ExecutorService COMMENT_EXECUTOR;

    static {
        USER_EXECUTOR = Executors.newCachedThreadPool();
        POST_EXECUTOR = Executors.newCachedThreadPool();
        COMMENT_EXECUTOR = Executors.newCachedThreadPool();
    }

}
