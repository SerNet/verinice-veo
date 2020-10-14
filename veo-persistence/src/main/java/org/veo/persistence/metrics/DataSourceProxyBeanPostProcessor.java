/*******************************************************************************
 * Copyright (c) 2020 Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.persistence.metrics;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

@Component
@Slf4j
/**
 * This class is based on the example in "Leonard, A. (2020): Spring Boot Persistence Best
 * Practices. Apress Media."
 */
public class DataSourceProxyBeanPostProcessor implements BeanPostProcessor {

    @Value("${veo.logging.datasource.slow_threshold_ms:1000}")
    private long SLOW_THRESHOLD_MS;



    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {

        if (bean instanceof DataSource) {

            log.info("DataSource has been found: " + bean + ". Logging queries and slow queries " +
                             "(> " + SLOW_THRESHOLD_MS + "ms)");

            final ProxyFactory proxyFactory = new ProxyFactory(bean);

            proxyFactory.setProxyTargetClass(true);
            proxyFactory.addAdvice(new ProxyDataSourceInterceptor((DataSource) bean, SLOW_THRESHOLD_MS));

            return proxyFactory.getProxy();
        }
        return bean;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    private static class ProxyDataSourceInterceptor implements MethodInterceptor {

        private final DataSource dataSource;

        public ProxyDataSourceInterceptor(DataSource dataSource, long slowThreshold) {
            super();
            this.dataSource = ProxyDataSourceBuilder.create(dataSource)
                                                    .name("DATA_SOURCE_PROXY")
                                                    .logQueryBySlf4j(SLF4JLogLevel.INFO)
                                                    .logSlowQueryBySlf4j(slowThreshold, TimeUnit.MILLISECONDS)
                                                    .multiline()
                                                    .build();
        }

        @Override
        public Object invoke(final MethodInvocation invocation) throws Throwable {

            final Method proxyMethod = ReflectionUtils.findMethod(this.dataSource.getClass(),
                                                                  invocation.getMethod()
                                                                            .getName());

            if (proxyMethod != null) {
                return proxyMethod.invoke(this.dataSource, invocation.getArguments());
            }

            return invocation.proceed();
        }
    }
}
