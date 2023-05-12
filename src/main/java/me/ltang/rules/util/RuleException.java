package me.ltang.rules.util;

/**
 * 规则异常类
 *
 * @author tangliu
 */
public class RuleException extends Exception {
    private String code;
    private String msg;

    public RuleException(Exception err) {
        super(err);
        this.code = "-1";
        this.msg = err.getMessage();
    }

    public RuleException(String msg) {
        super(msg);
        this.code = "-1";
        this.msg = msg;
    }

    public RuleException(String message, Throwable cause) {
        super(message, cause);
        this.code = "-1";
        this.msg = msg;
    }

    public RuleException(String code, String msg) {
        super(code + ":" + msg);
        this.code = code;
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}