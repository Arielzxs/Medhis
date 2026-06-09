package com.neusoft.his.common.security;

import java.util.Set;

public record SecurityUser(Long userId, String username, Set<String> roles) {
}
