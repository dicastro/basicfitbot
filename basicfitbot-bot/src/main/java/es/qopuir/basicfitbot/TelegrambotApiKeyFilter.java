package es.qopuir.basicfitbot;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TelegrambotApiKeyFilter extends OncePerRequestFilter {
	private static final Logger LOG = LoggerFactory.getLogger(TelegrambotApiKeyFilter.class);
	
	private BotProperties botProperties;
	 
    @Autowired
    public void setEventHolderBean(BotProperties botProperties) {
        this.botProperties = botProperties;
    }
    
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (request.getMethod().equalsIgnoreCase(RequestMethod.POST.name())
				&& request.getRequestURI().endsWith(BotController.IDEALISTABOT_URL)) {
			if (!request.getRequestURI().startsWith("/" + botProperties.getApiKey())) {
				LOG.info("Rejected request '{}': api-key is missing", BotController.IDEALISTABOT_URL);
				return;
			} else {
				String newUrl = request.getRequestURI().substring(botProperties.getApiKey().length() + 1);

				LOG.debug("Redirecting request '{}' to {}", request.getRequestURI(), newUrl);

				RequestDispatcher requestDispatcher = request.getRequestDispatcher(newUrl);

				requestDispatcher.forward(request, response);
				return;
			}
		}

		filterChain.doFilter(request, response);
	}
}
