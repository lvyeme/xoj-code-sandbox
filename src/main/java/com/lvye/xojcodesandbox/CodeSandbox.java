package com.lvye.xojcodesandbox;


import com.lvye.xojcodesandbox.model.ExecuteCodeRequest;
import com.lvye.xojcodesandbox.model.ExecuteCodeResponse;

/**
 * @author LVye
 * @version 1.0
 */
public interface CodeSandbox {
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
