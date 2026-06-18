package com.oceanverse.auth.validator;

public class ValidationResult {

    public static final String WEAK = "WEAK";
    public static final String MEDIUM = "MEDIUM";
    public static final String STRONG = "STRONG";

    private final boolean valid;
    private final String strength;
    private final String message;

    private ValidationResult(boolean valid, String strength, String message) {
        this.valid = valid;
        this.strength = strength;
        this.message = message;
    }

    public static ValidationResult weak(String message) {
        return new ValidationResult(false, WEAK, message);
    }

    public static ValidationResult medium() {
        return new ValidationResult(true, MEDIUM, "密码强度中等");
    }

    public static ValidationResult strong() {
        return new ValidationResult(true, STRONG, "密码强度符合要求");
    }

    public boolean isValid() { return valid; }
    public String getStrength() { return strength; }
    public String getMessage() { return message; }
}
