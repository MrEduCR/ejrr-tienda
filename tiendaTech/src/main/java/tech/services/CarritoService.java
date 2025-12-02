package tech.services;

import tech.domain.*;
import tech.repository.FacturaRepository;
import tech.repository.ProductoRepository;
import tech.repository.VentaRepository;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CarritoService {
    private static final String ATTRIBUTE_CARRITO = "carrito";

    private final ProductoRepository productoRepository;
    private final FacturaRepository facturaRepository;
    private final VentaRepository ventaRepository;

    public CarritoService(ProductoRepository productoRepository, FacturaRepository facturaRepository, VentaRepository ventaRepository) {
        this.productoRepository = productoRepository;
        this.facturaRepository = facturaRepository;
        this.ventaRepository = ventaRepository;
    }

    // --- 1. Gestión de Sesión ---
    public List<Item> obtenerCarrito(HttpSession session) {
        @SuppressWarnings("unchecked")
        List<Item> carrito = (List<Item>) session.getAttribute(ATTRIBUTE_CARRITO);
        if (carrito == null) {
            carrito = new ArrayList<>();
        }
        return carrito;
    }

    public void guardarCarrito(HttpSession session, List<Item> carrito) {
        session.setAttribute(ATTRIBUTE_CARRITO, carrito);
    }

    // --- 2. Agregar producto ---
    public void agregarProducto(List<Item> carrito, Long idProducto) {
        Producto producto = productoRepository.findById(idProducto)
            .orElseThrow(() -> new RuntimeException("Producto no encontrado."));

        Optional<Item> itemExistente = carrito.stream()
            .filter(i -> i.getProducto().getIdProducto().equals(idProducto))
            .findFirst();

        int cantidad = 1;

        if (itemExistente.isPresent()) {
            Item item = itemExistente.get();
            int nuevaCantidad = item.getCantidad() + cantidad;

            if (nuevaCantidad > producto.getExistencias()) {
                throw new RuntimeException("Stock insuficiente para agregar " + cantidad + " unidades.");
            }
            item.setCantidad(nuevaCantidad);

        } else {
            if (cantidad > producto.getExistencias()) {
                throw new RuntimeException("Stock insuficiente para agregar " + cantidad + " unidades.");
            }

            Item nuevoItem = new Item();
            nuevoItem.setProducto(producto);
            nuevoItem.setCantidad(cantidad);
            nuevoItem.setPrecioHistorico(producto.getPrecio());
            carrito.add(nuevoItem);
        }
    }

    // --- 3. Buscar item ---
    public Item buscarItem(List<Item> carrito, Long idProducto) {
        if (carrito == null) {
            return null;
        }

        return carrito.stream()
                .filter(item -> item.getProducto().getIdProducto().equals(idProducto))
                .findFirst()
                .orElse(null);
    }

    // --- 4. Eliminar item ---
    public void eliminarItem(List<Item> carrito, Long idProducto) {
        carrito.removeIf(item -> item.getProducto().getIdProducto().equals(idProducto));
    }

    // --- 5. Actualizar cantidad ---
    public void actualizarCantidad(List<Item> carrito, Long idProducto, int nuevaCantidad) {
        if (nuevaCantidad <= 0) {
            eliminarItem(carrito, idProducto);
            return;
        }

        Optional<Item> itemExistente = carrito.stream()
            .filter(i -> i.getProducto().getIdProducto().equals(idProducto))
            .findFirst();

        if (itemExistente.isPresent()) {
            Item item = itemExistente.get();
            Producto producto = item.getProducto();

            if (nuevaCantidad > producto.getExistencias()) {
                throw new RuntimeException("No hay suficiente stock disponible.");
            }

            item.setCantidad(nuevaCantidad);
        }
    }

    // --- 6. Contar unidades ---
    public int contarUnidades(List<Item> carrito) {
        if (carrito == null || carrito.isEmpty()) {
            return 0;
        }
        return carrito.stream()
                .mapToInt(Item::getCantidad)
                .sum();
    }

    // --- 7. Calcular total ---
    public BigDecimal calcularTotal(List<Item> carrito) {
        return carrito.stream()
            .map(Item::getSubTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // --- 8. Limpiar carrito ---
    public void limpiarCarrito(HttpSession session) {
        List<Item> carrito = obtenerCarrito(session);
        if (carrito != null) {
            carrito.clear();
        }
        guardarCarrito(session, carrito);
    }

    // --- 9. Procesar compra ---
    @Transactional
    public Factura procesarCompra(List<Item> carrito, Usuario usuario) {
        System.out.println("Se va a Procesar la Compra...");

        if (carrito == null || carrito.isEmpty()) {
            throw new RuntimeException("El carrito está vacío para procesar la compra.");
        }

        // 1. Crear Factura
        Factura factura = new Factura();
        factura.setUsuario(usuario);
        factura.setFecha(LocalDateTime.now());
        factura.setTotal(calcularTotal(carrito));
        factura.setEstado(EstadoFactura.Pagada);
        factura.setFechaCreacion(LocalDateTime.now());
        factura.setFechaModificacion(LocalDateTime.now());
        factura = facturaRepository.save(factura);

        // 2. Crear líneas de venta y actualizar stock
        for (Item item : carrito) {
            Producto producto = productoRepository.findById(item.getProducto().getIdProducto()).get();

            if (item.getCantidad() > producto.getExistencias()) {
                throw new RuntimeException("Fallo en la compra: El producto " 
                        + producto.getDescripcion() + " ya no tiene suficiente stock.");
            }

            Venta venta = new Venta();
            venta.setFactura(factura);
            venta.setProducto(item.getProducto());
            venta.setPrecioHistorico(item.getPrecioHistorico());
            venta.setCantidad(item.getCantidad());
            venta.setFechaCreacion(LocalDateTime.now());
            venta.setFechaModificacion(LocalDateTime.now());
            ventaRepository.save(venta);

            producto.setExistencias(producto.getExistencias() - item.getCantidad());
            productoRepository.save(producto);
        }

        return factura;
    }
}
