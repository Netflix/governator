package com.netflix.governator.guice.jetty;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;

/**
 * Implementations of this interface should return a connector which may be added to a Jetty server. The server is
 * passed as an argument to the getConnector() argument so that implementations can utilize subclasses of Jetty's
 * ServerConnector which requires the Server as a constructor argument.
 */
public interface JettyConnectorProvider {
    public Connector getConnector(Server server);
}
