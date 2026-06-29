package com.food.smart_food_system.DTO;

import java.util.Set;

public class UpdateUserRolesRequest {

    private Set<String> roles;

    public UpdateUserRolesRequest() {
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}