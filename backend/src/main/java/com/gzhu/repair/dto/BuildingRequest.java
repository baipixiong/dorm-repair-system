package com.gzhu.repair.dto;

import jakarta.validation.constraints.NotBlank;

public class BuildingRequest {

    @NotBlank(message = "请输入楼栋名称")
    private String name;

    private String campus;

    private String address;

    private String remark;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCampus() {
        return campus;
    }

    public void setCampus(String campus) {
        this.campus = campus;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
