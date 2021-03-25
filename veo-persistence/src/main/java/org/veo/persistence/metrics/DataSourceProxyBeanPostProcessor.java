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
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

@Component
@Profile({ "stats", "test" })
@Slf4j
/**
 * A data source proxy that can be configured to log slow and/or all generated
 * queries at the level of the SQL datasource. It can be used for simple logging
 * or to query the exact number of generated SQL queries at flush time in tests.
 *
 * Based on the example in "Leonard, A. (2020): Spring Boot Persistence Best
 * Practices. Apress Media."
 */
public class DataSourceProxyBeanPostProcessor implements BeanPostProcessor {

    @Value("${veo.logging.datasource.slow_threshold_ms:1000}")
    private long SLOW_THRESHOLD_MS;

    @Value("${veo.logging.datasource.all_queries:false}")
    private boolean LOG_ALL;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof DataSource) {
            String jdbcUrl = "unknown";
            if (bean instanceof HikariDataSource) {
                HikariDataSource hikariData = (HikariDataSource) bean;
                jdbcUrl = hikariData.getJdbcUrl();
            }
            log.info("DataSource has been found: {}. Logging queries and slow queries (> {}ms)",
                     jdbcUrl, SLOW_THRESHOLD_MS);
            final ProxyFactory proxyFactory = new ProxyFactory(bean);
            proxyFactory.setProxyTargetClass(true);
            proxyFactory.addAdvice(new ProxyDataSourceInterceptor((DataSource) bean,
                    SLOW_THRESHOLD_MS, LOG_ALL));
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

        public ProxyDataSourceInterceptor(DataSource dataSource, long slowThreshold,
                boolean logAll) {
            super();
            var dataSourceBuilder = ProxyDataSourceBuilder.create(dataSource)
                                                          .countQuery()
                                                          .name("DATA_SOURCE_PROXY")
                                                          .logSlowQueryBySlf4j(slowThreshold,
                                                                               TimeUnit.MILLISECONDS)
                                                          .multiline();
            if (logAll) {
                dataSourceBuilder.logQueryBySlf4j(SLF4JLogLevel.INFO);
            }
            this.dataSource = dataSourceBuilder.build();
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
