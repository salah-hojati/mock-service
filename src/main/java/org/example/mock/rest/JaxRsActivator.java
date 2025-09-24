package org.example.mock.rest;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

// The base path for all REST endpoints in this application
@ApplicationPath("/api")
public class JaxRsActivator extends Application {
}