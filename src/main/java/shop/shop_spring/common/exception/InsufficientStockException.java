package shop.shop_spring.common.exception;

public class InsufficientStockException extends RuntimeException{
    public InsufficientStockException(String message){super(message);}
}
