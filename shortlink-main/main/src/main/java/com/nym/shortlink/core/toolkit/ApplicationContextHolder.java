package com.nym.shortlink.core.toolkit;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring ApplicationContext 持有者
 * 该类用于在整个应用程序中持有ApplicationContext实例，方便其他组件获取Spring容器中的Bean
 */
@Component
public class ApplicationContextHolder implements ApplicationContextAware {

    // 静态ApplicationContext实例，用于在整个应用程序中共享Spring容器上下文
    private static ApplicationContext CONTEXT;

    /**
     * 实现ApplicationContextAware接口的方法，在Spring容器初始化时调用
     * @param applicationContext Spring容器上下文
     * @throws BeansException 如果获取ApplicationContext失败
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 将传入的ApplicationContext实例赋值给静态变量CONTEXT
        CONTEXT = applicationContext;
    }

    /**
     * 获取指定类型的 Bean
     * @param clazz 要获取的Bean的Class对象
     * @param <T> Bean的类型
     * @return 指定类型的Bean实例
     */
    public static <T> T getBean(Class<T> clazz) {
        // 从Spring容器中获取指定类型的Bean实例
        return CONTEXT.getBean(clazz);
    }
}
