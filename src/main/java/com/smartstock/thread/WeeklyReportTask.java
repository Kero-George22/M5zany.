package com.smartstock.thread;

import com.smartstock.service.WeeklySummaryService;
import javafx.application.Platform;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class WeeklyReportTask implements Runnable {

    private static final long SEVEN_DAYS_MS  = 7L * 24 * 60 * 60 * 1000;
    private static final long POLL_INTERVAL_MS = 60 * 60 * 1000; // check every 1 hour

    private final WeeklySummaryService summaryService;
    private volatile boolean running = true;

    public WeeklyReportTask() {
        this.summaryService = new WeeklySummaryService();
    }

    @Override
    public void run() {
        System.out.println("[WeeklyReportTask] Started. Will check every hour if 7 days have passed since last summary.");

        while (running) {
            try {
                LocalDateTime lastRun = summaryService.getLastSummaryTimestamp();

                boolean shouldRun;
                if (lastRun == null) {
                    System.out.println("[WeeklyReportTask] No previous summary found. Generating now...");
                    shouldRun = true;
                } else {
                    long millisSince = ChronoUnit.MILLIS.between(lastRun, LocalDateTime.now());
                    shouldRun = millisSince >= SEVEN_DAYS_MS;
                    if (!shouldRun) {
                        long hoursLeft = (SEVEN_DAYS_MS - millisSince) / 3_600_000;
                        System.out.println("[WeeklyReportTask] Next auto-summary in ~" + hoursLeft + " hour(s).");
                    }
                }

                if (shouldRun) {
                    System.out.println("[WeeklyReportTask] 7 days passed — generating AI summary automatically...");
                    String summary = summaryService.generateAISummary();
                    System.out.println("[WeeklyReportTask] Summary generated successfully.");
                    Platform.runLater(() -> System.out.println("[WeeklyReportTask] Alert dispatched to admin."));
                }

                // Wait 1 hour before next check
                Thread.sleep(POLL_INTERVAL_MS);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[WeeklyReportTask] Error: " + e.getMessage());
                try { Thread.sleep(60_000); } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        System.out.println("[WeeklyReportTask] Stopped.");
    }

    public void stop() {
        this.running = false;
    }
}
