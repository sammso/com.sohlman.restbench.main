package com.sohlman.vertx.site;

import com.sohlman.vertx.site.freemarker.FreemarkerTemplateEngine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.handler.BodyHandler;

public class HttpServerVerticle extends AbstractVerticle {

	public HttpServerVerticle(File rootDir) {
		this.rootDir = rootDir;
	}
	
	public HttpServerVerticle(int portToListen, File rootDir) {
		this.rootDir = rootDir;
		this.portToListen = portToListen;
	}

	@Override
	public void start() {
		this.workerExecutor = vertx.createSharedWorkerExecutor("worker", 1);
		
		OpenAPI3RouterFactory.create(this.vertx, "slowsite.yaml", openAPI3RouterFactoryAsyncResult -> {
			
			if ( openAPI3RouterFactoryAsyncResult.succeeded()) {
				OpenAPI3RouterFactory routerFactory = openAPI3RouterFactoryAsyncResult.result();
				
				routerFactory.addHandlerByOperationId("get-minimum-request-time", this::getMinimumTimeOut);
				routerFactory.addHandlerByOperationId("set-minimum-request-time", this::setMinimumTimeout);
				routerFactory.addHandlerByOperationId("get-maximum-request-time", this::getMaximumTimeOut);
				routerFactory.addHandlerByOperationId("set-maximum-request-time", this::setMaximumTimeout);

				Router router = routerFactory.getRouter();

				router.route().handler(BodyHandler.create());

				router.get("/:page").handler(this::getPage);
				router.get("/").handler(this::getPage);

				this.freeMarkerTemplateEngine = new FreemarkerTemplateEngine(vertx, templates);

				handleUpdateTemplatesTimer(0); // Start right away
				
				vertx.createHttpServer().requestHandler(router).listen(this.portToListen);
				
			}
			else {
				Throwable t =  openAPI3RouterFactoryAsyncResult.cause();
			}
		});
	}
		
		
	private void handleUpdateTemplatesTimer(long id) {
		if ( !updatingTemplates ) {
			updatingTemplates=true;
			this.workerExecutor.executeBlocking(promise -> {
				File[] listOfFiles = rootDir.listFiles();
				
				Set<String> toBeRemoved = new TreeSet(templates.keySet());
				
				for (File file : listOfFiles) {
				    if (file.isFile()) {
				    	try {
				    		String templateName = file.getName();
							templates.put(templateName, new String(Files.readAllBytes(Paths.get(file.toURI()))));
							toBeRemoved.remove(templateName);
						} catch (IOException e) {
							// Ignore
						}
				    }
				}
				
				for (String templateName : toBeRemoved) {
					templates.remove(templateName);
				}
				updatingTemplates=false;
				vertx.setTimer(1000, this::handleUpdateTemplatesTimer);
			}, resultHandler -> {
			}); 
		}
	}
		
	private void getPage(RoutingContext routingContext) {
		long waitTime = Util.randomNumber(this.minTimeOut, this.maxTimeOut);
		vertx.setTimer(waitTime, id -> {
			String page = routingContext.request().getParam("page");
			
			if ( page==null ) {
				page="";
			}
			
			HttpServerResponse response = routingContext.response();
			
			JsonObject data = new JsonObject()
				.put("name", "Slow site")
				.put("waitTime", waitTime)
				.put("now", String.valueOf(new Date()))
				.put("path", routingContext.request().path());
						
			freeMarkerTemplateEngine.render(data, page, res -> {
				if (res.succeeded()) {
					routingContext.response().end(res.result());
				} else {
					routingContext.fail(res.cause());
				}
			});
		});
	}

	private void getMinimumTimeOut(RoutingContext routingContext) {
		routingContext.response().setStatusCode(200).putHeader(CONTENTTYPE, CONTENTTYPE_APPLICATION_JSON)
				.end(Json.encodePrettily(Long.valueOf(this.minTimeOut)));
	}

	private void setMinimumTimeout(RoutingContext routingContext) {
		try {
			int timeout = Integer.valueOf(routingContext.request().getParam("timeout"));
			this.minTimeOut = timeout;
		
			
			routingContext.response().setStatusCode(201).putHeader(CONTENTTYPE, CONTENTTYPE_APPLICATION_JSON)
					.end(Json.encodePrettily(this.minTimeOut));
		} catch (NumberFormatException nfe) {
			routingContext.response().setStatusCode(204).end("Error " + nfe.getMessage());
		}
	}
	
	private void getMaximumTimeOut(RoutingContext routingContext) {
		routingContext.response().setStatusCode(200).putHeader(CONTENTTYPE, CONTENTTYPE_APPLICATION_JSON)
				.end(Json.encodePrettily(Long.valueOf(this.maxTimeOut)));
	}

	private void setMaximumTimeout(RoutingContext routingContext) {
		try {
			int timeout = Integer.valueOf(routingContext.request().getParam("timeout"));
			this.maxTimeOut = timeout;
			routingContext.response().setStatusCode(201).putHeader(CONTENTTYPE, CONTENTTYPE_APPLICATION_JSON)
					.end(Json.encodePrettily(this.maxTimeOut));
		} catch (NumberFormatException nfe) {
			routingContext.response().setStatusCode(204).end("Error " + nfe.getMessage());
		}
	}
	
	public static final String CONTENTTYPE = "content-type";
	public static final String CONTENTTYPE_APPLICATION_JSON = "application/json; charset=utf-8";

	private TemplateEngine freeMarkerTemplateEngine;
	
	private int maxTimeOut = 1479;
	private int minTimeOut = 567;
	private int portToListen = 13579;
	private WorkerExecutor workerExecutor;
	private File rootDir;
	private Map<String, String> templates = new ConcurrentHashMap<>();
	private boolean updatingTemplates = false;
}