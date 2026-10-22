package com.rxcare.qa.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/** Console logging of each test's lifecycle; makes CI logs readable without opening the report. */
public class TestListener implements ITestListener {

    private static final Logger LOG = LoggerFactory.getLogger(TestListener.class);

    @Override
    public void onTestStart(ITestResult result) {
        LOG.info("START  {}", name(result));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        LOG.info("PASS   {} ({} ms)", name(result), duration(result));
    }

    @Override
    public void onTestFailure(ITestResult result) {
        LOG.error("FAIL   {} ({} ms): {}", name(result), duration(result),
                result.getThrowable() == null ? "" : result.getThrowable().getMessage());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        LOG.warn("SKIP   {}{}", name(result), result.wasRetried() ? " (will be retried)" : "");
    }

    @Override
    public void onFinish(ITestContext context) {
        LOG.info("Finished <{}>: {} passed, {} failed, {} skipped", context.getName(),
                context.getPassedTests().size(), context.getFailedTests().size(), context.getSkippedTests().size());
    }

    private static String name(ITestResult result) {
        return result.getTestClass().getRealClass().getSimpleName() + "." + result.getMethod().getMethodName();
    }

    private static long duration(ITestResult result) {
        return result.getEndMillis() - result.getStartMillis();
    }
}
