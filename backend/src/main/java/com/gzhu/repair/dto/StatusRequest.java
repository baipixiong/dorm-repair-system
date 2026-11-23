package com.gzhu.repair.dto;

import jakarta.validation.constraints.NotNull;

public class StatusRequest {

    @NotNull(message = "缺少状态值")
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
