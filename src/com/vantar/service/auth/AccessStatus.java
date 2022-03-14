package com.vantar.service.auth;

public enum AccessStatus {
    // customer is enabled after signup
    ENABLED,
    // shop is pending after signup
    PENDING,
    // admin can disable user
    DISABLED,
    // user can unsubscribe
    UNSUBSCRIBED,
}
