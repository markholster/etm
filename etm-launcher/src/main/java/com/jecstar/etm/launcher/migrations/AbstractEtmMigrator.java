package com.jecstar.etm.launcher.migrations;

/**
 * Abstract superclass for all <code>EtmMigrator</code> instances.
 */
public abstract class AbstractEtmMigrator implements EtmMigrator {

    /**
     * Print a progress bar on the console.
     *
     * @param lastPrint The last print percentage.
     * @param current   The current number of tasks executed.
     * @param total     The total number of tasks to be executed.
     * @return The last print percentage.
     */
    long printPercentageWhenChanged(long lastPrint, long current, long total) {
        long percentage = Math.round(current * 100.0 / total);
        if (percentage > lastPrint) {
            if (percentage == 100) {
                System.out.println("[##########] 100%");
            } else {
                System.out.print("[");
                for (int i = 0; i < 10; i++) {
                    if (percentage / 10 > i) {
                        System.out.print("#");
                    } else {
                        System.out.print(" ");
                    }
                }
                System.out.print("] " + percentage + "%\r");
                System.out.flush();
            }
        }
        return percentage;
    }
}
