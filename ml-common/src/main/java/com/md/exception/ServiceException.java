package com.md.exception;

import com.md.result.ResultCode;
import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {

    private final ResultCode resultCode;

    public ServiceException(ResultCode resultCode, String coderMessage) {
        super(coderMessage);
        this.resultCode = resultCode;
    }
}