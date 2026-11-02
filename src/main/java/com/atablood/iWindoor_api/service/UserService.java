package com.atablood.iWindoor_api.service;

import com.atablood.iWindoor_api.auth.RegisterRequest;
import com.atablood.iWindoor_api.dto.UserDTO;
import com.atablood.iWindoor_api.entity.User;
import java.util.List;

public interface UserService {
    List<UserDTO> getMyEmployees(User currentUser);
    void createEmployee(User currentUser, RegisterRequest request);
}