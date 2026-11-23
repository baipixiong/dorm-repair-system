package com.gzhu.repair.common;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Constants {

    private Constants() {
    }

    public static final String ROLE_STUDENT = "student";
    public static final String ROLE_TECHNICIAN = "technician";
    public static final String ROLE_ADMIN = "admin";

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_ASSIGNED = "assigned";
    public static final String STATUS_WORKING = "working";
    public static final String STATUS_REVIEW = "review";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_CANCELLED = "cancelled";

    public static final String PRIORITY_LOW = "low";
    public static final String PRIORITY_NORMAL = "normal";
    public static final String PRIORITY_HIGH = "high";
    public static final String PRIORITY_URGENT = "urgent";

    public static final Map<String, String> STATUS_LABELS = new LinkedHashMap<>();
    public static final Map<String, String> CATEGORY_LABELS = new LinkedHashMap<>();
    public static final Map<String, String> PRIORITY_LABELS = new LinkedHashMap<>();
    public static final Map<String, String> ROLE_LABELS = new LinkedHashMap<>();

    static {
        STATUS_LABELS.put(STATUS_PENDING, "待派单");
        STATUS_LABELS.put(STATUS_ASSIGNED, "已派单");
        STATUS_LABELS.put(STATUS_WORKING, "维修中");
        STATUS_LABELS.put(STATUS_REVIEW, "待验收");
        STATUS_LABELS.put(STATUS_COMPLETED, "已完成");
        STATUS_LABELS.put(STATUS_CANCELLED, "已取消");

        CATEGORY_LABELS.put("water", "水电维修");
        CATEGORY_LABELS.put("electric", "电路故障");
        CATEGORY_LABELS.put("door", "门窗维修");
        CATEGORY_LABELS.put("furniture", "家具维修");
        CATEGORY_LABELS.put("other", "其他问题");

        PRIORITY_LABELS.put(PRIORITY_LOW, "低");
        PRIORITY_LABELS.put(PRIORITY_NORMAL, "普通");
        PRIORITY_LABELS.put(PRIORITY_HIGH, "紧急");
        PRIORITY_LABELS.put(PRIORITY_URGENT, "特急");

        ROLE_LABELS.put(ROLE_STUDENT, "学生");
        ROLE_LABELS.put(ROLE_TECHNICIAN, "维修人员");
        ROLE_LABELS.put(ROLE_ADMIN, "管理员");
    }

    public static boolean isActiveStatus(String status) {
        return STATUS_PENDING.equals(status) || STATUS_ASSIGNED.equals(status)
                || STATUS_WORKING.equals(status) || STATUS_REVIEW.equals(status);
    }
}
