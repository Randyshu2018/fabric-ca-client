package com.blockchain.model;

/**
 * @author shurenwei
 */
public class Result {

    public static final String SUCC = "操作成功";
    public static final String FAIL = "操作失败";

    private String msg;
    private Integer code;
    private String data;

    private Result(String msg,Integer code,String data){
        this.msg = msg;
        this.code = code;
        this.data = data;
    }
    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public static Result getErrorInstance(String msg){
        return new Result(msg,500,null);
    }

    public static Result getSuccInstance(String data){
        return new Result(Result.SUCC,200,data);
    }
}
