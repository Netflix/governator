1.2.3 - 1.2.8 - xxxxxxxxxxxxx
==============================
* Updated to use Guava 14

* Issue 82: DAG builder wasn't handling bindings through interfaces. Integrated
pull request that builds implicit dependencies. Also, as a side benefit, the
PR reduces the size of the DAG by removing non warmup deps. Thanks to user
wfhartford.

* Pull 90: removed System.exit(-1) that can cause weird "Java VM exited abnormally

* Pull 91: Improved abstraction for ConfigurationDocumentation so it be later
exposed via a REST endpoint

* Pull 94: bunch of useful binding annotations added

* Issue 95: Revert lookup() so governator is buildable with Java6

* Issue 96/Pull 97: DAGManager has memory leak. Thank you to user: romikk

* Added concept of an application runner

* Change LifecycleInjector to optionally use a simulated child 
injector instead of a real Guice child injector. Using Guice child injectors 
has unwanted side effects. It also makes some patterns (e.g. injecting the 
Injector) difficult.

* Support adding multiple Module classes

* Converting to muli-module project

1.2.2 - June 11, 2013
=====================
* Issue 59: Add support for multi binding via AutoBindSingleton.

* Previous fix for memory leak (Issue 60) is improved in 1.2.2. Only
record dependencies for objects that have @WarmUp methods. Also,
use a weak valued Guava cache internally.

1.2.1 - May 15, 2013
=========================
* Pull 57: In JBoss 7.X, url.getProtocol() returns "vfs" instead of "file".

* Issue 60: Once the LifecycleManager has been started, newly injected objects
could cause a memory leak due how the warmup code was handling post-start injections.

1.2.0 - March 12, 2013
=========================
* Pull 51: Support dynamic property configuration injection. The Configuration
APIs have changed to support this new functionality. Please see
https://github.com/Netflix/governator/wiki/Configuration-Mapping for details.

1.1.1 - February 26, 2013
=========================
* Some additional createInjector() variants in LifecycleInjector.

1.1.0 - February 21, 2013
=========================
* Enhanced LifecycleListener.objectInjected() to take correct type information. All implementations
of LifecycleListener will need to be updated.

1.0.5 - February 20, 2013
=========================
* Fixed a potential infinite loop with an internal reflective method. Though, I have not seen
it actually occur in production.

* Changed the log message "Could not find lifecycle-registered object for key..." to DEBUG.

* Issue 43: Added a version of createStandardClasspathScanner() that takes additional
annotations.

* Issue 45: Added support for @Resource and @Resources. The resources themselves are loaded
by a new interface: ResourceLocator. A default ResourceLocator is installed that simply calls
injector.getInstance(resource.type()). It will only be a few lines of code to add a JNDI version
of ResourceLocator. Bind ResourceLocator instances via BootstrapBinder.bindResourceLocator().

* LifecycleInjectorBuilder now has a way to add modules as opposed to always specifying the complete
set.

1.0.4 - January 8, 2013
=======================
* WarmUps weren't being run for objects with no dependencies.

* Issue 33: Warm up errors were merely being logged. They now generate true exceptions. Any
exceptions thrown by warm up methods are wrapped in a single WarmUpException that is throw by
the LifecycleManager.start() method.

* Issue 32: Objects added to the lifecycle after LifecycleManager.start() was called were getting
warmed up in the Guice thread. This could cause deadlocks and other problems. Further, each object
would get warmed up independently. I've reworked this code so that post-start warm ups occur in
a separate thread and there is a small padding period so that multiple objects added near the same
time can get warmed up together.

1.0.3 - January 3, 2013
=======================
* Added a new scope, FineGrainedLazySingleton. Guice's default Singleton scope synchronizes
all object creation on a single lock (InternalInjectorCreator.class). It does this to avoid
deadlocks with circular dependencies. FineGrainedLazySingleton instead locks on the key
so that multiple singletons can be created concurrently. Circular dependencies are rare
so FineGrainedLazySingleton risks deadlocks in those situations for the benefit of better
concurrency.

* Made LifecycleManager more concurrent by removing the coarse syncs on the add methods.

1.0.2 - December 5, 2012
========================
* Integrated hierarchy graphing. See the wiki for details:
https://github.com/Netflix/governator/wiki/Grapher-Integration

* There's an edge case that results in a ClassNotFoundException when Governator
reflects on a class to find annotated methods. Governator now catches these and logs
an error instead of stopping the app.

1.0.1 - November 28, 2012
=========================
* Issue 27: Add support for auto binding Guice modules. Modules can now be marked
@AutoBindSingleton and they will get automatically installed.

* By default, @AutoBindSingleton binds to the class that has the annotation. You can
now set the value to any base class/interface that you want to bind to. You can bind to
generic base classes/interfaces by specifying the raw type (i.e.
@AutoBindSingleton(List.class) for List<String>).

