/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.persistence.metrics;

import java.lang.reflect.Method;
import java.sql.ResultSet;
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
@Profile({"stats", "test"})
@Slf4j
/**
 * A data source proxy that can be configured to log slow and/or all generated queries at the level
 * of the SQL datasource. It can be used for simple logging or to query the exact number of
 * generated SQL queries at flush time in tests.
 *
 * <p>Based on the example in "Leonard, A. (2020): Spring Boot Persistence Best Practices. Apress
 * Media."
 */
public class DataSourceProxyBeanPostProcessor implements BeanPostProcessor {

  @Value("${veo.logging.datasource.slow_threshold_ms:300}")
  private long slowThresholdMs;

  @Value("${veo.logging.datasource.all_queries:false}")
  private boolean logAll;

  @Value("${veo.logging.datasource.row_count:false}")
  private boolean logResultSetRowCount;

  static int totalResultSetRowsRead = 0;

  public static int getTotalResultSetRowsRead() {
    return totalResultSetRowsRead;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) {
    if (bean instanceof DataSource datasource) {
      String jdbcUrl = "unknown";
      if (bean
          instanceof
          // https://github.com/pmd/pmd/issues/5042
          @SuppressWarnings("PMD.CloseResource") HikariDataSource hikariData) {
        jdbcUrl = hikariData.getJdbcUrl();
      }
      log.info(
          "DataSource has been found: {}. Logging queries and slow queries (> {}ms)",
          jdbcUrl,
          slowThresholdMs);
      final ProxyFactory proxyFactory = new ProxyFactory(bean);
      proxyFactory.setProxyTargetClass(true);
      proxyFactory.addAdvice(
          new ProxyDataSourceInterceptor(
              datasource, slowThresholdMs, logAll, logResultSetRowCount));
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

    public ProxyDataSourceInterceptor(
        DataSource dataSource, long slowThreshold, boolean logAll, boolean resultSetRowCount) {
      super();
      var dataSourceBuilder =
          ProxyDataSourceBuilder.create(dataSource)
              .countQuery()
              .name("DATA_SOURCE_PROXY")
              .logSlowQueryBySlf4j(slowThreshold, TimeUnit.MILLISECONDS)
              .multiline();

      if (logAll) {
        dataSourceBuilder.logQueryBySlf4j(SLF4JLogLevel.INFO);
      }

      if (resultSetRowCount) {
        dataSourceBuilder
            .repeatableReadResultSet()
            .proxyResultSet()
            .afterMethod(
                executionContext -> {
                  var method = executionContext.getMethod();
                  if (ResultSet.class.isAssignableFrom(executionContext.getTarget().getClass())
                      && Boolean.TRUE.equals(executionContext.getResult())
                      && method.getName().equals("next")) {
                    DataSourceProxyBeanPostProcessor.totalResultSetRowsRead++;
                    log.debug(
                        "Total ResultSet rows processed: {}",
                        DataSourceProxyBeanPostProcessor.totalResultSetRowsRead);
                  }
                });
      }
      this.dataSource = dataSourceBuilder.build();
    }

    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
      final Method proxyMethod =
          ReflectionUtils.findMethod(this.dataSource.getClass(), invocation.getMethod().getName());
      if (proxyMethod != null) {
        return proxyMethod.invoke(this.dataSource, invocation.getArguments());
      }
      return invocation.proceed();
    }
  }
}
