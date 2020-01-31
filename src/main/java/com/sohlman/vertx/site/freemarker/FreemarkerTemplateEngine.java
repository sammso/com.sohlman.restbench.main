package com.sohlman.vertx.site.freemarker;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

import freemarker.cache.NullCacheStorage;
import freemarker.template.Configuration;
import freemarker.template.Template;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import io.vertx.ext.web.templ.freemarker.impl.VertxWebObjectWrapper;

public class FreemarkerTemplateEngine implements TemplateEngine, FreeMarkerTemplateEngine {

	private final Configuration config;

	public FreemarkerTemplateEngine(Vertx vertx, Map<String, String> templates) {
	    config = new Configuration(Configuration.VERSION_2_3_28);
	    config.setLocalizedLookup(false);
	    config.setObjectWrapper(new VertxWebObjectWrapper(config.getIncompatibleImprovements()));
	    config.setTemplateLoader(new FreeMarkerTemplateLoader(vertx, templates));
	    config.setCacheStorage(new NullCacheStorage());
	  }

	@Override
	public FreeMarkerTemplateEngine setMaxCacheSize(int maxCacheSize) {
		return this;
	}

	@Override
	public void render(Map<String, Object> context, String templateFile, Handler<AsyncResult<Buffer>> handler) {
		try {
			Template template = config.getTemplate(templateFile);

			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				template.process(context, new OutputStreamWriter(baos));
				handler.handle(Future.succeededFuture(Buffer.buffer(baos.toByteArray())));
			}

		} catch (Exception ex) {
			handler.handle(Future.failedFuture(ex));
		}
	}

	@Override
	public FreeMarkerTemplateEngine setExtension(String extension) {
		return this;
	}

	@Override
	public boolean isCachingEnabled() {
		return false;
	}
}