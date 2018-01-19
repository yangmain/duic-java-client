package io.zhudy.duic.config.spring;

import io.zhudy.duic.config.Config;
import io.zhudy.duic.config.ConfigUtils;
import io.zhudy.duic.config.DuicListener;
import io.zhudy.duic.config.ReloadPlot;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * DuiC Spring BeanFactoryPostProcessor.
 *
 * @author Kevin Zou (kevinz@weghst.com)
 */
public class DuicConfigBeanFactoryPostProcessor implements EnvironmentAware, BeanFactoryPostProcessor {

    private ConfigurableListableBeanFactory beanFactory;
    private ConfigurableEnvironment environment;

    private String baseUri;
    private String name;
    private String profile;
    private String configToken;
    private boolean failFast;
    private ReloadPlot reloadPlot;
    private List<DuicListener> listeners;

    private String oldState;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
        postProcess();
    }

    private void postProcess() {
        Config config = buildConfig();
        ConfigUtils.setDefaultConfig(config);

        // 注册 Spring 属性配置
        environment.getPropertySources().addFirst(new PropertySource<String>(ConfigUtils.class.getName()) {

            @Override
            public String getProperty(String name) {
                return ConfigUtils.getString(name, null);
            }
        });
    }

    private void reloadValueAnnotation() {
        AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
        bpp.setAutowiredAnnotationType(Value.class);
        bpp.setBeanFactory(beanFactory);
        for (String name : beanFactory.getBeanDefinitionNames()) {
            bpp.processInjection(beanFactory.getBean(name));
        }
    }

    private Config buildConfig() {
        Config.Builder builder = new Config.Builder()
                .baseUri(baseUri)
                .name(name)
                .profile(profile)
                .configToken(configToken)
                .failFast(failFast);
        if (reloadPlot != null) {
            builder.reloadPlot(reloadPlot);
        }
        if (listeners != null && !listeners.isEmpty()) {
            for (DuicListener listener : listeners) {
                builder.listener(listener);
            }
        }

        builder.listener(new DuicListener() {
            @Override
            public void handle(String state, Map<String, Object> properties) {
                if (oldState != null && !Objects.equals(oldState, state)) {
                    reloadValueAnnotation();
                }
                oldState = state;
            }
        });
        return builder.build();
    }

    public String getBaseUri() {
        return baseUri;
    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getConfigToken() {
        return configToken;
    }

    public void setConfigToken(String configToken) {
        this.configToken = configToken;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    public ReloadPlot getReloadPlot() {
        return reloadPlot;
    }

    public void setReloadPlot(ReloadPlot reloadPlot) {
        this.reloadPlot = reloadPlot;
    }

    public List<DuicListener> getListeners() {
        return listeners;
    }

    public void setListeners(List<DuicListener> listeners) {
        this.listeners = listeners;
    }
}
