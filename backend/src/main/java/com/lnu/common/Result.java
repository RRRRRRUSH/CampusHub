package com.lnu.common;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code; // 200 成功, 其他失败
    private String msg;   // 提示信息
    private T data;       // 数据载体

    // 成功时的快捷方法
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "操作成功";
        r.data = data;
        return r;
    }

    // 成功但无数据
    public static <T> Result<T> success() {
        return success(null);
    }

    // 失败时的快捷方法
    public static <T> Result<T> error(String msg) {
        Result<T> r = new Result<>();
        r.code = 500;
        r.msg = msg;
        return r;
    }
}