package com.sohlman.vertx.site.freemarker;

import com.sohlman.vertx.site.Util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Map;

import freemarker.cache.TemplateLoader;
import io.vertx.core.Vertx;

public class FreeMarkerTemplateLoader implements TemplateLoader {

	private final Vertx vertx;
	private Map<String, String> templates;

	public FreeMarkerTemplateLoader(Vertx vertx, Map<String, String> templates) {
		this.vertx = vertx;
		this.templates = templates;
	}

	@Override
	public Object findTemplateSource(String name) throws IOException {
		String template = null;
		
		if ( "random".equalsIgnoreCase(name)) {
			String[] templateStr = templates.keySet().toArray(new String[] {});
			if ( templateStr.length>0) {
				int random = Util.randomNumber(0, templateStr.length - 1);
				name = templateStr[random];
			}
		}
		
		template = templates.get(name);
		
		if ( template==null ) {
			template = templates.get("_default.ftl");
		}
		if ( template==null ) {
			template = "<html><head>Template not found</head><body>Template not found</body></html>";
		}
		return new StringTemplateSource(name, template, System.currentTimeMillis());
	}

	@Override
	public long getLastModified(Object templateSource) {
		return ((StringTemplateSource) templateSource).lastModified;
	}

	@Override
	public Reader getReader(Object templateSource, String encoding) throws IOException {
		return new StringReader(((StringTemplateSource) templateSource).source);
	}

	@Override
	public void closeTemplateSource(Object templateSource) throws IOException {

	}

	private static class StringTemplateSource {
		private final String name;
		private final String source;
		private final long lastModified;

		StringTemplateSource(String name, String source, long lastModified) {
			if (name == null) {
				throw new IllegalArgumentException("name == null");
			}
			if (source == null) {
				throw new IllegalArgumentException("source == null");
			}
			if (lastModified < -1L) {
				throw new IllegalArgumentException("lastModified < -1L");
			}
			this.name = name;
			this.source = source;
			this.lastModified = lastModified;
		}

		public boolean equals(Object obj) {
			return obj instanceof StringTemplateSource && name.equals(((StringTemplateSource) obj).name);
		}

		public int hashCode() {
			return name.hashCode();
		}
	}
}
