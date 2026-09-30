package dev.temper.cerebro.auth.domain;

import java.util.Objects;
import dev.temper.cerebro.common.DomainChecks;

public record User(String id, String displayName, AvatarVariant avatarVariant) {
    public enum AvatarVariant { MALE, FEMALE }
    public User {
        id = DomainChecks.text(id, 80, "user id");
        displayName = DomainChecks.text(displayName, 80, "display name");
        Objects.requireNonNull(avatarVariant);
    }
}
