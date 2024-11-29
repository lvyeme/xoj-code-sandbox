package com.lvye.xojcodesandbox;

import lombok.Data;

/**
 * @author LVye
 * @version 1.0
 */
@Data
public class ExecuteMessage {
    private Integer exitValue;
    private String message;

    private String errorMessage;

    private Long time;
}
