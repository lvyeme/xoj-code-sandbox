package com.lvye.xojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import com.lvye.xojcodesandbox.model.ExecuteCodeRequest;
import com.lvye.xojcodesandbox.model.ExecuteCodeResponse;
import com.lvye.xojcodesandbox.model.JudgeInfo;
import com.lvye.xojcodesandbox.utils.ProcessUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StopWatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author LVye
 * @version 1.0
 */
public class JavaNativeCodeSandbox implements CodeSandbox {
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
    private static final String GLOBAL_TEST_CODE_DIR_NAME = "main.java";

    public static void main(String[] args) {
        JavaNativeCodeSandbox javaNativeCodeSandbox = new JavaNativeCodeSandbox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/main.java", StandardCharsets.UTF_8);
        //String code = ResourceUtil.readStr("testCode/simpleCompute/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setInputList(Arrays.asList("1 2", "3 4"));
        executeCodeRequest.setLanguage("java");
        executeCodeRequest.setCode(code);
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);

    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String language = executeCodeRequest.getLanguage();
        String code = executeCodeRequest.getCode();

        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        //判断全局代码目录是否存在,没有则新建
        if (!cn.hutool.core.io.FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        //把用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_TEST_CODE_DIR_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);

        //2.编译代码。 得到class 文件
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            System.out.println(executeMessage);

        } catch (IOException e) {

            return getErrorCodeResponse(e);
        }

        // 3.执行代码，得到输出结果
            List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            String runCmd = String.format("java -Dfile.encoding=UTF-8 -cp %s main %s", userCodeParentPath, inputArgs);
            try {

                Process runProcess = Runtime.getRuntime().exec(runCmd);
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                // ExecuteMessage executeMessage1 = ProcessUtils.runInteractProcessAndGetMessage(runProcess, inputArgs);
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage);
            } catch (IOException e) {
                return getErrorCodeResponse(e);
            }

            //4.收集整理输出结果
            ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
            List<String> outputList = new ArrayList<>();
            // 取用时最大值，便于判断是否超时
            long maxTime = 0;
            for (ExecuteMessage executeMessage : executeMessageList) {
                String errorMessage = executeMessage.getErrorMessage();
                if (StrUtil.isNotBlank(errorMessage)) {
                    executeCodeResponse.setMassage(errorMessage);
                    // 执行中存在错误
                    executeCodeResponse.setStatus(3);
                    break;
                }
                outputList.add(executeMessage.getMessage());
                Long time = executeMessage.getTime();
                if(time != maxTime){
                    maxTime = Math.max(maxTime,time);
                }
            }
            // 正常运行完成
            if (outputList.size() == executeMessageList.size()) {
                executeCodeResponse.setStatus(2);
            }
            executeCodeResponse.setOutputList(outputList);
            JudgeInfo judgeInfo = new JudgeInfo();
            judgeInfo.setTime(maxTime);
            // 要借用第三方库 来获取内存占用
//            judgeInfo.setMemory();

            executeCodeResponse.setJudgeInfo(judgeInfo);

            //5.文件清理
            if (userCodeFile.getParentFile() != null){
                boolean del = FileUtil.del(userCodeParentPath);
                System.out.println("文件清理成功:" + (del ? "成功" : "失败"));
            }
        }
        return null;
    }

    /**
     * 错误码 处理
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorCodeResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMassage(e.getMessage());
        // 代表代码沙箱错误
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;

    }
}
