package com.rxcare.pharmacy.drug;

import com.rxcare.pharmacy.auth.AuthInterceptor;
import com.rxcare.pharmacy.auth.UserSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DrugController {

    private final DrugService service;

    public DrugController(DrugService service) {
        this.service = service;
    }

    @GetMapping("/api/drugs")
    public List<Drug> list() {
        return service.list();
    }

    @GetMapping("/api/drugs/{ndc}")
    public Drug get(@PathVariable("ndc") String ndc) {
        return service.get(ndc);
    }

    @GetMapping("/api/inventory/low-stock")
    public List<Drug> lowStock() {
        return service.lowStock();
    }

    @PostMapping("/api/inventory/{ndc}/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public Drug adjust(@PathVariable("ndc") String ndc,
                       @Valid @RequestBody InventoryAdjustmentRequest request,
                       @RequestAttribute(AuthInterceptor.CURRENT_USER) UserSession user) {
        return service.adjust(ndc, request, user.username());
    }

    @GetMapping("/api/inventory/{ndc}/transactions")
    public List<InventoryTransaction> transactions(@PathVariable("ndc") String ndc) {
        return service.transactions(ndc);
    }
}
