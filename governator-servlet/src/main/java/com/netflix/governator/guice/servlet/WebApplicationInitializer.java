package com.netflix.governator.guice.servlet;

import com.google.inject.Injector;

/***
 * Servlet 3.0+ compatible interface for bootstrapping a web application. Any
 * class (or classes) on the classpath implementing WebApplicationInitializer
 * will be initialized by the container.
 * 
 * public class MyWebApplication implements WebApplicationInitializer {
 * 
 *      @Override 
 *      protected Injector createInjector() throws Exception { 
 *          return InjectorBuilder.fromModules().createInjector(); 
 *      }  
 * 
 * }
 * 
 * @author twicksell
 */
public interface WebApplicationInitializer {

    Injector createInjector();

}
