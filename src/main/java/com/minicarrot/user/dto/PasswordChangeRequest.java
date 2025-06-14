package com.minicarrot.user.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordChangeRequest {
    
    @NotBlank(message = "현재 비밀번호는 필수입니다.")
    private String currentPassword;
    
    @NotBlank(message = "새 비밀번호는 필수입니다.")
    @Size(min = 6, max = 20, message = "새 비밀번호는 6자 이상 20자 이하여야 합니다.")
    private String newPassword;
} 