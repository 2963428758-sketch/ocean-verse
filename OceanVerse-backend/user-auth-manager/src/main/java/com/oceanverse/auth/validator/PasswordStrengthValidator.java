package com.oceanverse.auth.validator;

public interface PasswordStrengthValidator {

    ValidationResult validate(String password, String username);
}
