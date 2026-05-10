package com.smartstock.thread;

import com.smartstock.service.WeeklySummaryService;
import javafx.application.Platform;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class WeeklyReportTask implements Runnable {

    private static final long SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000;

    private final WeeklySummaryService summaryService;
    private volatile boolean running = true;

    public WeeklyReportTask() {
        this.summaryService = new WeeklySummaryService();
    }

    @Override
    public void run() {
        System.out.println("[WeeklyReportTask] Started. Checking last run time...");

        long initialDelayMs = calculateInitialDelay();

        while (running) {
            try {
                // Wait out the initial delay (0 if we should run immediately)
                if (initialDelayMs > 0) {
                    System.out.println("[WeeklyReportTask] Next run in ~"
                            + (initialDelayMs / 3_600_000) + " hour(s).");
                    Thread.sleep(initialDelayMs);
                    initialDelayMs = 0;
                }

                if (!running) break;

                generateAndReport();

                // After each successful run, always wait a full 7 days
                Thread.sleep(SEVEN_DAYS_MS);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[WeeklyReportTask] Error: " + e.getMessage());
                try {
                    Thread.sleep(60_000); // retry after 1 minute on error
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        System.out.println("[WeeklyReportTask] Stopped.");
    }

    /**
     * Calculates how long to wait before the first run.
     * Returns 0 if we should run immediately, otherwise the remaining ms until 7 days have passed.
     */
    private long calculateInitialDelay() {
        LocalDateTime lastRun = summaryService.getLastSummaryTimestamp();

        if (lastRun == null) {
            System.out.println("[WeeklyReportTask] No previous summary found. Running immediately.");
            return 0;
        }

        long millisSinceLastRun = ChronoUnit.MILLIS.between(lastRun, LocalDateTime.now());

        if (millisSinceLastRun >= SEVEN_DAYS_MS) {
            System.out.println("[WeeklyReportTask] Last summary was "
                    + (millisSinceLastRun / 3_600_000) + " hour(s) ago (>=7 days). Running immediately.");
            return 0;
        }

        long remaining = SEVEN_DAYS_MS - millisSinceLastRun;
        System.out.println("[WeeklyReportTask] Last summary was "
                + (millisSinceLastRun / 3_600_000) + " hour(s) ago. Next run in ~"
                + (remaining / 3_600_000) + " hour(s).");
        return remaining;
    }

    private void generateAndReport() {
        String summary = summaryService.generateAISummary();
        System.out.println("[WeeklyReportTask] Weekly summary generated successfully.");

        Platform.runLater(() ->
                System.out.println("[WeeklyReportTask] Alert sent to admin dashboard."));
    }

    public void stop() {
        this.running = false;
    }
}
