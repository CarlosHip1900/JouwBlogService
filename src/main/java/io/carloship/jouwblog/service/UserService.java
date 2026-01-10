package io.carloship.jouwblog.service;

import io.carloship.jouwblog.repository.UserRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Optional;

@Singleton
public class UserService {

    @Inject
    protected UserRepository repository;

}
