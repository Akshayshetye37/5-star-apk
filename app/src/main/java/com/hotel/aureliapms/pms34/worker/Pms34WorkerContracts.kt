package com.hotel.aureliapms.pms34.worker

/**
 * Scheduling contract for the automation/night-audit/housekeeping/reporting
 * areas. The host WorkManager implementation should call these jobs.
 */
enum class Pms34Job {
    HOUSEKEEPING_ALERTS,
    NIGHT_AUDIT,
    REVENUE_REFRESH,
    BACKUP,
    REPORT_REFRESH,
    AUTOMATION
}
