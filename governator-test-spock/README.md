Testing Governator With Spock
=============================

# Features
* Annotate your test with [@ModulesForTesting](https://github.com/Netflix/governator/blob/master/governator-test/src/main/java/com/netflix/governator/guice/test/ModulesForTesting.java) to specify which Guice modules should be included when building your Injector.
* @Inject dependencies directly into your Specification class.
* Annotate dependencies with [@ReplaceWithMock](https://github.com/Netflix/governator/blob/master/governator-test/src/main/java/com/netflix/governator/guice/test/ReplaceWithMock.java) to add an override binding for that dependency's type to a Spock Mock implementation. 
* Annotate dependencies with [@WrapWithSpy](https://github.com/Netflix/governator/blob/master/governator-test/src/main/java/com/netflix/governator/guice/test/WrapWithSpy.java) to wrap any binding with a Spock Spy.
* Annotate parent classes, test classes, and/or test methods with [@TestPropertyOverride](https://github.com/Netflix/archaius/blob/2.x/archaius2-test/src/main/java/com/netflix/archaius/test/TestPropertyOverride.java) to set [Archaius2](https://github.com/Netflix/archaius/tree/2.x) property overrides for your Injector **(Note: You must include [ArchaiusModule](https://github.com/Netflix/archaius/blob/2.x/archaius2-guice/src/main/java/com/netflix/archaius/guice/ArchaiusModule.java) yourself in @ModulesForTesting to use this feature.)**

# Dependency
Add the following to your build.gradle
```
testCompile 'com.netflix.governator:governator-test-spock:latest.release'
```
***Note: This framework requires at least Spock 1.1 to properly function!***

# Example
```java
@ModulesForTesting({ MyModule.class, ArchaiusModule.class })    //Specify any Modules you wish to include in your test
public class ExampleTest extends Specification {
    
    @Inject
    @ReplaceWithMock                                            //Indicate that you wish this dependency to be Mocked
    SomeDependency dependency  
    
    @Inject
    @WrapWithSpy                                                //Indicate that you wish to wrap this dependency with a Spy
    SomeRealDependency realDependency                      
    
    def setup() {
        dependency.getValue() >> "Test"                         //Specify desired behavior for your Mock
    }
    
    def "Test a basic Mock"() {
        expect:
        dependency.value == "Test"                              //Verify behavior of your Mock
    }
    
    def "Test Spying an object and checking what was called" {
        when:
        realDependency.getValue()
        
        then:
        1 * realDependency.getValue()                           //Verify that the getValue method was invoked once
    }
    
    @Inject
    Config config
    
    @TestPropertyOverride({"myProperty=test"})                  //Specify property values you wish to be set
    def "Test Config Overrides"() {
        expect:
        config.getString("myProperty") == "test"                //Verify that your property was set as expected
    }
    
    @TestPropertyOverride(propertyFiles={"testProps.properties"}) //Properties may also be loaded from a file
    public void testConfigFromFile() {
        expect:
        config.getString("myProperty") == "test"                //Verify that your property was set as expected
    }
}
```

# Controlling Injector Creation
You may choose whether the Injector will be created once per test class or once per test method by setting the injectorCreation attribute of [@ModulesForTesting](https://github.com/Netflix/governator/blob/master/governator-test/src/main/java/com/netflix/governator/guice/test/ModulesForTesting.java). **By default, the injector is created once per test class.** This should only be modified if the state of your injector bound objects are mutable in some way and that state has been modified by one of your tests.
```java
//Injector created once per test class
@ModulesForTesting(injectorCreation=InjectorCreationMode.BEFORE_TEST_CLASS)  
public class ExampleTest extends Specification {
...
}
```
```java
//Injector created once per test method
@ModulesForTesting(injectorCreation=InjectorCreationMode.BEFORE_EACH_TEST_METHOD)  
public class ExampleTest extends Specification {
...
}
```
