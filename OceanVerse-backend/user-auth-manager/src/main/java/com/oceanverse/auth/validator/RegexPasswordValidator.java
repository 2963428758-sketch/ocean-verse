package com.oceanverse.auth.validator;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 基于正则 + 字典的密码强度校验
 */
@Component
public class RegexPasswordValidator implements PasswordStrengthValidator {

    // Top 100 常见弱密码
    private static final Set<String> WEAK_DICTIONARY = Set.of(
            "123456", "password", "123456789", "12345678", "12345",
            "1234567890", "1234567", "qwerty", "abc123", "111111",
            "123123", "admin", "password1", "iloveyou", "welcome",
            "monkey", "dragon", "master", "football", "baseball",
            "sunshine", "princess", "1234", "123", "000000",
            "666666", "888888", "qwerty123", "1q2w3e4r", "passw0rd"
    );

    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");
    private static final Pattern CONSECUTIVE_CHARS = Pattern.compile(".*(?:abcdef|fghijk|123456|654321).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern REPEATED_CHAR = Pattern.compile("(.)\\1{3,}");

    @Override
    public ValidationResult validate(String password, String username) {
        if (password == null || password.length() < 8) {
            return ValidationResult.weak("密码长度至少为8位");
        }

        // 纯数字
        if (DIGIT.matcher(password).matches()) {
            return ValidationResult.weak("密码不能为纯数字，请加入字母和特殊字符");
        }

        // 纯字母
        if (password.matches("^[a-zA-Z]+$")) {
            return ValidationResult.weak("密码不能为纯字母，请加入数字和特殊字符");
        }

        // 弱密码字典
        if (WEAK_DICTIONARY.contains(password.toLowerCase())) {
            return ValidationResult.weak("该密码为常见弱密码，请使用更复杂的组合");
        }

        // 含用户名
        if (username != null && password.toLowerCase().contains(username.toLowerCase())) {
            return ValidationResult.weak("密码不能包含用户名");
        }

        // 连续字符
        if (CONSECUTIVE_CHARS.matcher(password).find()) {
            return ValidationResult.weak("密码包含连续字符（如123456、abcdef），请避免使用");
        }

        // 相同字符重复
        if (REPEATED_CHAR.matcher(password).find()) {
            return ValidationResult.weak("密码包含过多重复字符（如aaaa），请避免使用");
        }

        // 计算字符类型数量
        int types = 0;
        if (UPPER.matcher(password).find()) types++;
        if (LOWER.matcher(password).find()) types++;
        if (DIGIT.matcher(password).find()) types++;
        if (SPECIAL.matcher(password).find()) types++;

        if (types < 3) {
            return ValidationResult.weak("密码需包含大写字母、小写字母、数字、特殊字符中至少3种");
        }

        if (password.length() >= 12 && types == 4) {
            return ValidationResult.strong();
        }
        return ValidationResult.medium();
    }
}
