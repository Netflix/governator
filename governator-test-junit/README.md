# Testing Governator With JUnit
To simplify integration testing of components wired together using Guice, you may use the [GovernatorJunit4ClassRunner](https://github.com/Netflix/governator/blob/master/governator-test-junit/src/main/java/com/netflix/governator/guice/test/junit4/GovernatorJunit4ClassRunner.java) JUnit runner. 

# Features
* Annotate your test with [@ModulesForTesting](https://github.com/Netflix/governator/blob/master/governator-test-junit/src/main/java/com/netflix/governator/guice/test/ModulesForTesting.java) to specify which Guice modules should be included when building your Injector.
* @Inject dependencies directly into your JUnit test class.
* Annotate dependencies with [@ReplaceWithMock](https://github.com/Netflix/governator/blob/master/governator-test-junit/src/main/java/com/netflix/governator/guice/test/ReplaceWithMock.java) to add an override binding for that dependency's type to a Mockito implementation. 
* Annotate dependencies with [@WrapWithSpy](https://github.com/Netflix/governator/blob/master/governator-test-junit/src/main/java/com/netflix/governator/guice/test/WrapWithSpy.java) to wrap any binding with a Mockito spy.
* Annotate parent classes, test classes, and/or test methods with [@TestPropertyOverride](https://github.com/Netflix/archaius/blob/2.x/archaius2-test/src/main/java/com/netflix/archaius/test/TestPropertyOverride.java) to set [Archaius2](https://github.com/Netflix/archaius/tree/2.x) property overrides for your Injector **(Note: You must include [ArchaiusModule](https://github.com/Netflix/archaius/blob/2.x/archaius2-guice/src/main/java/com/netflix/archaius/guice/ArchaiusModule.java) yourself in @ModulesForTesting to use this feature.)**

# Dependency
Add the following to your build.gradle
```
testCompile 'com.netflix.governator:governator-test-junit:latest.release'
```

# Example
```java
@RunWith(GovernatorJunit4ClassRunner.class)                     //You must use the Runner for these features to work
@ModulesForTesting({ MyModule.class, ArchaiusModule.class })    //Specify any Modules you wish to include in your test
public class ExampleTest {
    
    @Inject
    @ReplaceWithMock                                            //Indicate that you wish this dependency to be Mocked
    SomeDependency dependency;  
    
    @Inject
    @WrapWithSpy                                                //Indicate that you wish to wrap this dependency with a Spy
    SomeRealDependency realDependency;                      
    
    @Before
    public void setup() {
        Mockito.when(dependency.getValue()).thenReturn("Test"); //Specify desired behavior for your Mock
    }
    
    @Test
    public void testMock() {
        assertEquals("Test", dependency.getValue());            //Verify behavior of your Mock
    }
    
    @Test
    public void testSpy() {
        realDependency.getValue();
        Mockito.verify(realDependency, Mockito.times(1)).getValue(); //Verify that this object was interacted with exactly once
    }
    
    @Inject
    Config config;
    
    @Test
    @TestPropertyOverride({"myProperty=test"})                    //Specify property values you wish to be set
    public void testConfig() {
        assertEquals("test", config.getString("myProperty"));    //Verify that your property was set as expected
    }
    
    @Test
    @TestPropertyOverride(propertyFiles={"testProps.properties"}) //Properties may also be loaded from a file
    public void testConfigFromFile() {
        assertEquals("test", config.getString("myProperty"));   //Verify that your property was set as expected
    }
}
```

# Controlling Injector Creation
You may choose whether the Injector will be created once per test class or once per test method by setting the injectorCreation attribute of [@ModulesForTesting](https://github.com/Netflix/governator/blob/master/governator-test-junit/src/main/java/com/netflix/governator/guice/test/ModulesForTesting.java). **By default, the injector is created once per test class.** This should only be modified if your Injector is in some way stateful and that state has been modified by one of your tests.
```java
//Injector created once per test class
@RunWith(GovernatorJunit4ClassRunner.class)
@ModulesForTesting(injectorCreation=InjectorCreationMode.BEFORE_TEST_CLASS)  
public class ExampleTest {
...
}
```
```java
//Injector created once per test method
@RunWith(GovernatorJunit4ClassRunner.class)
@ModulesForTesting(injectorCreation=InjectorCreationMode.BEFORE_EACH_TEST_METHOD)  
public class ExampleTest {
...
}
```
