package com.oceanverse.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "状态更新请求")
public class UpdateStatusDTO {

    @Schema(description = "状态值：0=禁用, 1=正常, 2=锁定", example = "1")
    @NotNull(message = "status 不能为空")
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
