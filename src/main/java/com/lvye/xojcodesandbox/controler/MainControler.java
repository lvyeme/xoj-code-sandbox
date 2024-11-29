package com.lvye.xojcodesandbox.controler;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author LVye
 * @version 1.0
 */
@RestController("/")
public class MainControler {
    @GetMapping("health")
    public String healthCheck(){
        return "ok";
    }
}
