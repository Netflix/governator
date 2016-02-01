# Governator
## Overview
-----------
Governator is a library of extensions and utilities that enhance Google Guice with
lifecycle management.  The concept of lifecycle management has two aspects here.
1.  Injector lifecycle management via LifecycleListener
2.  Object lifecycle managmenet via @PostConstruct and @PreDestroy.

Governator also provides hooks through which additional features may be added to 
lifecycle management at the appropriate places. (see LifecycleFeature)

## Creating the Injector
------------------------
Governator provides a simple builder DSL on top of Guice's built in SPI to,
1.  provide a more readable fluent code
2.  Make injector creation policy driven
3.  constructing the list of modules provided to Guice.  

```java
LifecycleInjector injector = InjectorBuilder.fromModules(new MyApplicationModule());
```

Once created the container can interact with the LifecycleInjector's lifecycle methods
directly or asynchronously by registering a LifecycleListener in a Guice module.  The
injector's lifecycle may be terminated either by calling LifecycleInjector.shutdown()
or by injecting LifecycleShutdownSignal and calling LifecycleShutdownSignal.shutdown().

For example, to block until the injector's lifecycle completes,

```java
injector.awaitTermination();
```

Or to get asynchronous notification of termination
```java
injector.addListener(new DefaultLifecycleListener() {
        public void onShutdown() {
            // Do your shutdown handling here
        }
    });
```

To shutdown the injector from outside of Guice call,
```java
injector.shutdown();
```

Or to trigger shutdown from an Injector managed class,
```java
@Singleton
public class SomeServiceThatGetsAShutdownSignal {
    private final LifecycleShutdownSignal signal;

    @Inject
    public SomeServiceThatGetsAShutdownSignal(LifecycleShutdownSignal signal) {
    	this.signal = signal;
    }
    
    private void someShutdownInvokingCode() {
        this.signal.shutdown();
    }
}
```

You can also enable a JVM shutdown hook by adding ShutdownHookModule to Guice's list of modules 
```java
Governator.createInjector(new ShutdownHookModule(), new MyApplicationModule());
```

## Injector lifecycle
----------------------

When using DI all operations should be performed within the context DI such that initialization 
is done via dependency injection and eager singleton behavior. As long as eager singletons have 
been properly registered the entire application should be functional once the injector has been 
created.  Sometimes, however, it may be necessary to perform post injector creation operations, 
such as registering that the application is ready to serve traffic.  This can be done by implementing
the LifecycleInterface from any eager singleton.  

```java
@Singleton
public class MyApplication extends AbstractLifecycleListener {
    @Override
    public void onStarted() {
        applicationStatus.markAsUp();
    }
}
```

## Debugging Guice
----------------------

Governator includes several utilities to help debug problems and get insight into injector creation.

### TracingProvisionListener

Sometimes Guice fails to create the injector and provides little information about which class or object
creation path resulted in failure.  Enabling the TracingProvisionListener will result in class names
being emitted to standard output (can be customized to different outputs) and indented to give a clear
path to the path through which objects were provisioned.  

### ProvisionDebugModule

The ProvisionDebugModule tracks metrics about object instantiation and retains this information after
the injector has been created.  The information is later accessible via ProvisionMetrics.  This can 
be used to provide a UI that gives a visual representation of the application bootstrapping processes,
helping identify slow initialization paths.

## 

## Using Governator with main()
----------------------------------
```java
public class MyApplication {
	public static void main(String[] args) throws Exception {
		InjectorBuilder
		    .fromModules(new ShutdownHookModule(), new MyApplicationModule())
			.awaitTermination();
	}
}
```

## Using Governator with Tomcat
----------------------------
```java
public class StartServer extends GovernatorServletContextListener
{
    @Override
    protected Injector createInjector() {
        return InjectorBuilder.fromModules(
        	new ShutdownHookModule(),
            new JerseyServletModule() {
                @Override
                protected void configureServlets() {
                    serve("/REST/*").with(GuiceContainer.class);
                    bind(GuiceContainer.class).asEagerSingleton();
                    
                    bind(MyResource.class).asEagerSingleton();
                }
            }
        );
    }
}
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app version="2.4" xmlns="http://java.sun.com/xml/ns/j2ee" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd">

    <filter>
        <filter-name>guiceFilter</filter-name>
        <filter-class>com.google.inject.servlet.GuiceFilter</filter-class>
    </filter>

    <filter-mapping>
        <filter-name>guiceFilter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>

    <listener>
        <listener-class>StartServer</listener-class>
    </listener>
</web-app>
```

## Using Governator with Jetty
---------------------------
```java
public class MyApplication {
	public static void main(String[] args) throws Exception {
		Governator.createInjector(
		    new JettyModule(),
        	new ShutdownHookModule(),
            new JerseyServletModule() {
                @Override
                protected void configureServlets() {
                    serve("/REST/*").with(GuiceContainer.class);
                    bind(GuiceContainer.class).asEagerSingleton();
                    
                    bind(MyResource.class).asEagerSingleton();
                }
            }
        ).awaitTermination();
    }
}
```

# Using governator with JUnit
--------------------------------
TBD

# Building
-----------

Governator is built via Gradle (http://www.gradle.org). To build from the command line:
    ./gradlew build

# Artifacts
----------

Governator binaries are published to Maven Central. Please see the docs for details.

# Author
-----------

Eran Landau (mailto:elandau@netflix.com)

# License
-----------

Copyright 2015 Netflix, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
