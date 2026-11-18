package com.osc.backend.admin.dto;

public class AdminDashboardStatsDto {

    private long totalUsers;
    private long newUsersThisMonth;
    private long newUsersLast7Days;

    private long totalMessages;
    private long unreadMessages;

    private long totalIssueReports;
    private long pendingIssueReports;

    private long totalRepositoryRequests;
    private long pendingRepositoryRequests;

    public AdminDashboardStatsDto() {
    }

    public AdminDashboardStatsDto(long totalUsers, long newUsersThisMonth, long newUsersLast7Days,
                                 long totalMessages, long unreadMessages,
                                 long totalIssueReports, long pendingIssueReports,
                                 long totalRepositoryRequests, long pendingRepositoryRequests) {
        this.totalUsers = totalUsers;
        this.newUsersThisMonth = newUsersThisMonth;
        this.newUsersLast7Days = newUsersLast7Days;
        this.totalMessages = totalMessages;
        this.unreadMessages = unreadMessages;
        this.totalIssueReports = totalIssueReports;
        this.pendingIssueReports = pendingIssueReports;
        this.totalRepositoryRequests = totalRepositoryRequests;
        this.pendingRepositoryRequests = pendingRepositoryRequests;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getNewUsersThisMonth() {
        return newUsersThisMonth;
    }

    public void setNewUsersThisMonth(long newUsersThisMonth) {
        this.newUsersThisMonth = newUsersThisMonth;
    }

    public long getNewUsersLast7Days() {
        return newUsersLast7Days;
    }

    public void setNewUsersLast7Days(long newUsersLast7Days) {
        this.newUsersLast7Days = newUsersLast7Days;
    }

    public long getTotalMessages() {
        return totalMessages;
    }

    public void setTotalMessages(long totalMessages) {
        this.totalMessages = totalMessages;
    }

    public long getUnreadMessages() {
        return unreadMessages;
    }

    public void setUnreadMessages(long unreadMessages) {
        this.unreadMessages = unreadMessages;
    }

    public long getTotalIssueReports() {
        return totalIssueReports;
    }

    public void setTotalIssueReports(long totalIssueReports) {
        this.totalIssueReports = totalIssueReports;
    }

    public long getPendingIssueReports() {
        return pendingIssueReports;
    }

    public void setPendingIssueReports(long pendingIssueReports) {
        this.pendingIssueReports = pendingIssueReports;
    }

    public long getTotalRepositoryRequests() {
        return totalRepositoryRequests;
    }

    public void setTotalRepositoryRequests(long totalRepositoryRequests) {
        this.totalRepositoryRequests = totalRepositoryRequests;
    }

    public long getPendingRepositoryRequests() {
        return pendingRepositoryRequests;
    }

    public void setPendingRepositoryRequests(long pendingRepositoryRequests) {
        this.pendingRepositoryRequests = pendingRepositoryRequests;
    }
}
