package com.steve.saasapp.service;

import com.steve.saasapp.dto.UserRegistrationRequest;
import com.steve.saasapp.model.User;

public interface UserService {
    User registerUser(UserRegistrationRequest request);
}

