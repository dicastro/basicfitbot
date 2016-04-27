package es.qopuir.basicfitbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import es.qopuir.basicfitbot.back.PhantomjsDownloader;
import es.qopuir.basicfitbot.back.PhantomjsDownloader.Version;
import es.qopuir.basicfitbot.back.ProxyProperties;

public class ApplicationPreparedListener implements ApplicationListener<ContextRefreshedEvent> {
	private static final Logger LOG = LoggerFactory.getLogger(ApplicationPreparedListener.class);
	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent applicationPreparedEvent) {
		ProxyProperties proxyProperties = applicationPreparedEvent.getApplicationContext().getBean(ProxyProperties.class);
		
		LOG.info("Application prepared with proxy enabled: {}", proxyProperties.isEnabled());
		
		PhantomjsDownloader.download(Version.V_2_1_1, proxyProperties);
	}
}