package com.rxcare.qa.listeners;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Attaches {@link RetryAnalyzer} to every test in the "ui" group. API and DB tests are
 * deterministic and are never retried, so a real defect cannot hide behind a retry.
 */
public class RetryTransformer implements IAnnotationTransformer {

    @Override
    @SuppressWarnings("rawtypes")
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        if (Arrays.asList(annotation.getGroups()).contains("ui")) {
            annotation.setRetryAnalyzer(RetryAnalyzer.class);
        }
    }
}
