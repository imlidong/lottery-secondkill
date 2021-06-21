package com.sankuai.mdp.lotterylidongservice.exception;

/**
 * @author lidong52
 * @version 1.0.0
 * @ClassName DuplicateIPException.java
 * @Description TODO
 * @createTime 2021年06月18日 22:10:00
 */
public class DuplicateIPException extends RuntimeException{
    public DuplicateIPException() {
    }

    public DuplicateIPException(String message) {
        super(message);
    }

    public DuplicateIPException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicateIPException(Throwable cause) {
        super(cause);
    }
}
