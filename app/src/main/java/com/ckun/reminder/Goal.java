package com.ckun.reminder;

public final class Goal {
    public long id, deadline;
    public String syncId = "";
    public long createdAt, updatedAt, deletedAt, syncVersion;
    public String title = "", dailyTitle = "";
    public int hour = 21, minute = 30;
    public boolean autoAdd = true;
}
