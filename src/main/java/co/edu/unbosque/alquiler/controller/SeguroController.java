package co.edu.unbosque.alquiler.controller;

import co.edu.unbosque.alquiler.dto.SeguroResponse;
import co.edu.unbosque.alquiler.service.SeguroService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/seguros")
public class SeguroController {

    private final SeguroService seguroService;

    public SeguroController(SeguroService seguroService) {
        this.seguroService = seguroService;
    }

    @GetMapping
    public List<SeguroResponse> listar() {
        return seguroService.listar();
    }
}
