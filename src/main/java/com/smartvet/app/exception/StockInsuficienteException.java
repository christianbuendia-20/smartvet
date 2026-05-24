package com.smartvet.app.exception;

public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(String nombreProducto, int stockActual, int solicitado) {
        super(String.format("Stock insuficiente para '%s': disponible=%d, solicitado=%d",
                nombreProducto, stockActual, solicitado));
    }
}
