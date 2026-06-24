package com.neusoft.his.service.dto;

public record AuthRequest(String username, String password, String name, String role) {
}
