package com.wanfadger.AdministrativeareaApi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.core.Ordered;

/**
 * <b>{@code order = HIGHEST_PRECEDENCE}</b> makes the cache interceptor sit outside the transaction
 * interceptor. Both default to {@code LOWEST_PRECEDENCE}, and when two advisors tie, their nesting is
 * decided arbitrarily — so a method carrying both {@code @Cacheable} and {@code @Transactional} may
 * well open a transaction (and check a connection out of the Hikari pool) only to then find the value
 * in the cache and commit an empty transaction. Under load the connection pool is what limits this
 * service, so spending a connection on a cache hit is exactly the wrong trade.
 *
 * <p>The read path does not rely on this: {@link com.wanfadger.AdministrativeareaApi.service.AdministrativeAreaCachingService}
 * is a separate bean in front of the transactional service, which makes the nesting structural rather
 * than a matter of configuration. This annotation is the safety net for anything added later that
 * puts both annotations on one method.
 */
@SpringBootApplication
@EnableCaching(order = Ordered.HIGHEST_PRECEDENCE)
public class AdministrativeareaApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(AdministrativeareaApiApplication.class, args);
	}

}
