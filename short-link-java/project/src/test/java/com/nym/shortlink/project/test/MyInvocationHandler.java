package com.nym.shortlink.project.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class MyInvocationHandler implements InvocationHandler {
    private Service service;
    public MyInvocationHandler(Service service) {
        this.service = service;
    }
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("===== 方法调用前 =====");

        // 执行目标方法
        Object result = method.invoke(service, args);

        // 后置增强
        System.out.println("===== 方法调用后 =====");
        return result;
    }
}
