package com.sohlman.vertx.site;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import io.vertx.core.Vertx;

public class Main {

	public static void main(String[] args) {
		
		if (args.length > 0) {
			File rootDir = new File(args[0]);

			if (rootDir.exists() && rootDir.isDirectory() && rootDir.canRead()) {
				Vertx.vertx().deployVerticle(new HttpServerVerticle(rootDir));				
			} else {
				new IllegalArgumentException(rootDir.getAbsolutePath() + " not found");
			}
				
		}
		else {
			System.out.println("Requires template root directory path:");
		}
	}
}
