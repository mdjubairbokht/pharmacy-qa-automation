package com.rxcare.qa.listeners;

import com.rxcare.qa.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/** Re-runs a failed UI test up to retry.count times (0 locally, 1 in CI). */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger LOG = LoggerFactory.getLogger(RetryAnalyzer.class);
    private final int maxRetries = Config.getInt("retry.count", 0);
    private int attempts;

    @Override
    public boolean retry(ITestResult result) {
        if (attempts < maxRetries) {
            attempts++;
            LOG.warn("Retrying {} (attempt {}/{})", result.getMethod().getMethodName(), attempts, maxRetries);
            return true;
        }
        return false;
    }
}
