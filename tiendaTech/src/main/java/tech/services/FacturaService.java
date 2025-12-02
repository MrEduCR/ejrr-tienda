package tech.services;

import tech.domain.*;
import tech.repository.FacturaRepository;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FacturaService {
   
    private final FacturaRepository facturaRepository;
    
    public FacturaService(FacturaRepository facturaRepository) {
        this.facturaRepository = facturaRepository;
    }

    @Transactional(readOnly = true)
    public Factura getFacturaConVentas(Long idFactura) {
    return facturaRepository.findByIdFacturaConDetalle(idFactura)
        .orElseThrow(() -> new NoSuchElementException(
            "Factura con ID " + idFactura + " no encontrada."));
    }

}
