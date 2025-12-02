
package tech.repository;


import tech.domain.Factura;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacturaRepository extends JpaRepository<Factura, Long> {
    @Query("SELECT f FROM Factura f " +
           "LEFT JOIN FETCH f.usuario u " +
           "LEFT JOIN FETCH f.ventas v " +
           "LEFT JOIN FETCH v.producto p " +
           "WHERE f.idFactura = :idFactura")
    Optional<Factura> findByIdFacturaConDetalle(@Param("idFactura") Long idFactura);
}
