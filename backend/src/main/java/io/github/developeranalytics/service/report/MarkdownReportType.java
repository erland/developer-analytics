package io.github.developeranalytics.service.report;

public enum MarkdownReportType {
    PUBLIC_OSS_REPORT("Public OSS report", "developer-analytics-public-oss-report.md"),
    FULL_DEVELOPER_REPORT("Full developer report", "developer-analytics-full-report.md"),
    TECHNOLOGY_PROFILE("Technology profile", "developer-analytics-technology-profile.md"),
    ACTIVITY_REPORT("Activity report", "developer-analytics-activity-report.md");

    private final String title;
    private final String filename;

    MarkdownReportType(String title, String filename) {
        this.title = title;
        this.filename = filename;
    }

    public String title() { return title; }
    public String filename() { return filename; }
}
