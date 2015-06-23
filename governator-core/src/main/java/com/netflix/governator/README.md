# Governator
## Overview
-----------
Governator is a library of extensions and utilities that enhance Google Guice with
lifecycle management.  The concept of lifecycle management has two aspects here.
1.  Injector lifecycle management via LifecycleListener
2.  Object lifecycle managmenet via @PostConstruct and @PreDestroy.

Governator also provides hooks through which additional features may be added to 
lifecycle management at the appropriate places. 

Note that this version of governator doesn't try to add any additional APIs (with the exception of the lightweight Governator.createInjector() API) or bastractions on top of Guice but rather leverage the use of Guice Modules and simple bindings to add features.

## Creating the Injector
---------------------
Governator provides a simple wrapper to Guice's Guice.createInjector through which lifecycle management is added.  Note that unlike previous versions of Governator no other features, such as classpath scanning and configuration bindings, are added here.  These are added using traditional modules.  Also, unlike previous versions only one Injector is created to avoid various idiosyncrasies introduced when using child injectors. 

```java
LifecycleInjector injector = Governator.createInjector(new MyApplicationModule());
```

Once created the container can interact with the LifecycleInjector's lifecycle 
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

## Using Governator with main()
----------------------------------
```java
public class MyApplication {
	public static void main(String[] args) throws Exception {
		Governator.createInjector(new ShutdownHookModule(), new MyApplicationModule())
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
        return Governator.createInjector(
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
