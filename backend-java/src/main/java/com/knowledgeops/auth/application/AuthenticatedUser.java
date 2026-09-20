package com.knowledgeops.auth.application;

import java.util.*;

public record AuthenticatedUser(
    UUID id, String email, Set<String> roles, Set<String> permissions) {}
