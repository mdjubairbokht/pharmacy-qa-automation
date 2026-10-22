package com.rxcare.pharmacy.prescription;

import com.rxcare.pharmacy.auth.AuthInterceptor;
import com.rxcare.pharmacy.auth.UserSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private final PrescriptionService service;

    public PrescriptionController(PrescriptionService service) {
        this.service = service;
    }

    @GetMapping
    public List<Prescription> list(@RequestParam(name = "status", required = false) String status,
                                   @RequestParam(name = "patientId", required = false) Integer patientId) {
        return service.list(status, patientId);
    }

    @GetMapping("/{id}")
    public Prescription get(@PathVariable("id") int id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Prescription create(@Valid @RequestBody PrescriptionRequest request,
                               @RequestAttribute(AuthInterceptor.CURRENT_USER) UserSession user) {
        return service.create(request, user);
    }

    @PostMapping("/{id}/verify")
    public Prescription verify(@PathVariable("id") int id,
                               @RequestAttribute(AuthInterceptor.CURRENT_USER) UserSession user) {
        return service.verify(id, user);
    }

    @PostMapping("/{id}/fill")
    public Prescription fill(@PathVariable("id") int id,
                             @RequestAttribute(AuthInterceptor.CURRENT_USER) UserSession user) {
        return service.fill(id, user);
    }

    @PostMapping("/{id}/pickup")
    public Prescription pickup(@PathVariable("id") int id,
                               @Valid @RequestBody PickupRequest request,
                               @RequestAttribute(AuthInterceptor.CURRENT_USER) UserSession user) {
        return service.pickup(id, request, user);
    }

    @PostMapping("/{id}/cancel")
    public Prescription cancel(@PathVariable("id") int id,
                               @Valid @RequestBody CancelRequest request,
                               @RequestAttribute(AuthInterceptor.CURRENT_USER) UserSession user) {
        return service.cancel(id, request, user);
    }

    @PostMapping("/{id}/refill")
    public Prescription refill(@PathVariable("id") int id,
                               @RequestAttribute(AuthInterceptor.CURRENT_USER) UserSession user) {
        return service.refill(id, user);
    }

    @GetMapping("/{id}/history")
    public List<StatusHistory> history(@PathVariable("id") int id) {
        return service.history(id);
    }

    @GetMapping("/{id}/transactions")
    public List<PharmacyTransaction> transactions(@PathVariable("id") int id) {
        return service.transactions(id);
    }
}
