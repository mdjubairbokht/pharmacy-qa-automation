package com.rxcare.pharmacy.patient;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService service;

    public PatientController(PatientService service) {
        this.service = service;
    }

    @GetMapping
    public List<Patient> search(@RequestParam(name = "search", required = false) String search) {
        return service.search(search);
    }

    @GetMapping("/{id}")
    public Patient get(@PathVariable("id") int id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Patient create(@Valid @RequestBody PatientRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public Patient update(@PathVariable("id") int id, @Valid @RequestBody PatientRequest request) {
        return service.update(id, request);
    }
}
