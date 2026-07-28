package com.md.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** @author 油桃老师 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {

    /** 响应代码 */
    private Integer code;
    /** 响应描述 */
    private String message;
    /** 响应描述（开发人员） */
    private String coderMessage;
    /** 响应数据 */
    private T data;

    /**
     * 创建Result类，默认返回成功状态码和响应描述，手动传递响应数据
     *
     * @param data 响应数据
     */
    public Result(T data) {
        this(ResultCode.SUCCESS.getCODE(), 
             ResultCode.SUCCESS.getMESSAGE(), 
             ResultCode.SUCCESS.getMESSAGE(), 
             data);
    }

    /**
     * 创建Result类，手动传递响应代码，响应描述和响应数据
     *
     * @param resultCode   响应状态枚举实例
     * @param coderMessage 响应描述（开发人员）
     * @param data         响应数据
     */
    public Result(ResultCode resultCode, String coderMessage, T data) {
        this(resultCode.getCODE(), 
             resultCode.getMESSAGE(), 
             coderMessage, 
             data);
    }

    /**
     * 创建Result类，手动传递响应代码和响应描述，响应数据为null
     *
     * @param resultCode   响应状态枚举实例
     * @param coderMessage 响应描述（开发人员）
     */
    public Result(ResultCode resultCode, String coderMessage) {
        this(resultCode.getCODE(), 
             resultCode.getMESSAGE(), 
             coderMessage, 
             null);
    }
}