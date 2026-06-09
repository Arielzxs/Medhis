package com.neusoft.his.service.dto;

import java.util.Set;

public record LoginResponse(String token, Long userId, String username, Set<String> roles) {
}
