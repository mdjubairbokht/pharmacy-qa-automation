package com.rxcare.qa.tests.base;

import com.rxcare.qa.api.AuthApi;
import com.rxcare.qa.api.RxApi;
import org.testng.annotations.BeforeClass;

/** API tests share one cached session per role; each test creates its own patients/prescriptions. */
public abstract class BaseApiTest {

    protected final AuthApi auth = new AuthApi();
    protected RxApi pharmacist;
    protected RxApi technician;

    @BeforeClass(alwaysRun = true)
    public void createSessions() {
        pharmacist = RxApi.asPharmacist();
        technician = RxApi.asTechnician();
    }
}
