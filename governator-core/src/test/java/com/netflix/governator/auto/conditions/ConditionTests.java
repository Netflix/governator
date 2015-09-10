package com.netflix.governator.auto.conditions;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.testng.annotations.Test;

import com.google.inject.Key;
import com.google.inject.spi.Element;
import com.netflix.governator.auto.AutoContext;
import com.netflix.governator.auto.annotations.ConditionalOnBinding;
import com.netflix.governator.auto.annotations.ConditionalOnClass;
import com.netflix.governator.auto.annotations.ConditionalOnMissingBinding;
import com.netflix.governator.auto.annotations.ConditionalOnMissingClass;

public class ConditionTests {
    public static class TestAutoContext implements AutoContext {
        @Override
        public boolean hasModule(String className) {
            return false;
        }

        @Override
        public boolean hasProfile(String profile) {
            return false;
        }

        @Override
        public boolean hasBinding(Key<?> key) {
            return false;
        }

        @Override
        public List<Element> getElements() {
            return Collections.emptyList();
        }
    }
    
    @Test
    public void testBindingCondition() {
        final Set<Key<?>> keys = new HashSet<>();
        keys.add(Key.get(String.class));
        keys.add(Key.get(Double.class));
        
        AutoContext context = new TestAutoContext() {
            @Override
            public boolean hasBinding(Key<?> key) {
                return keys.contains(key);
            }
        };
        
        OnBindingCondition condition = new OnBindingCondition(context);
        
        Assert.assertTrue(
            "Should have one existing binding", 
            condition.check(new ConditionalOnBinding() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return null;
                }
    
                @Override
                public String[] value() {
                    return new String[]{String.class.getName()};
                }
            }));
        
        Assert.assertTrue(
            "Should have two existing bindings",
            condition.check(new ConditionalOnBinding() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return null;
                }
    
                @Override
                public String[] value() {
                    return new String[]{String.class.getName(), Double.class.getName()};
                }
            }));

        Assert.assertFalse(
            "Should not have single binding", 
            condition.check(new ConditionalOnBinding() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return null;
                }
    
                @Override
                public String[] value() {
                    return new String[]{Integer.class.getName()};
                }
            }));

        Assert.assertFalse(
            "Should fail for one existing and one missing binding",
            condition.check(new ConditionalOnBinding() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return null;
                }
    
                @Override
                public String[] value() {
                    return new String[]{String.class.getName(), Integer.class.getName()};
                }
            }));
    }
    
    @Test
    public void testClassCondition() {
        OnClassCondition condition = new OnClassCondition();

        Assert.assertTrue(
                "Should succeed on existing class", 
                condition.check(new ConditionalOnClass() {
                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return null;
                    }
        
                    @Override
                    public String[] value() {
                        return new String[]{String.class.getName()};
                    }
                }));
            
            Assert.assertTrue(
                "Should succeed on two existing classes",
                condition.check(new ConditionalOnClass() {
                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return null;
                    }
        
                    @Override
                    public String[] value() {
                        return new String[]{String.class.getName(), Double.class.getName()};
                    }
                }));

            Assert.assertFalse(
                "Should fail on one missing class", 
                condition.check(new ConditionalOnClass() {
                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return null;
                    }
        
                    @Override
                    public String[] value() {
                        return new String[]{"nonexistingclass"};
                    }
                }));

            Assert.assertFalse(
                "Should fail for one existing and one missing class",
                condition.check(new ConditionalOnClass() {
                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return null;
                    }
        
                    @Override
                    public String[] value() {
                        return new String[]{String.class.getName(), "nonexistingclass"};
                    }
                }));
    }
    
    @Test
    public void testEnvironmentCondition() {
        OnEnvironmentCondition condition = new OnEnvironmentCondition();

//        Assert.assertTrue(
//                "Should succeed on existing class", 
//                condition.check(new ConditionalOnEnvironment() {
//                    @Override
//                    public Class<? extends Annotation> annotationType() {
//                        return null;
//                    }
//        
//                    @Override
//                    public String[] value() {
//                        return new String[]{String.class.getName()};
//                    }
//                }));
    }
    
    @Test
    public void testJUnitCondition() {
        
    }
    
    @Test
    public void testMacOSCondition() {
        
    }
    
    @Test
    public void testMissingBindingCondition() {
        final Set<Key<?>> keys = new HashSet<>();
        keys.add(Key.get(String.class));
        keys.add(Key.get(Double.class));
        
        AutoContext context = new TestAutoContext() {
            @Override
            public boolean hasBinding(Key<?> key) {
                return keys.contains(key);
            }
        };
        
        OnMissingBindingCondition condition = new OnMissingBindingCondition(context);
        
        Assert.assertFalse(
            "Should fail on one existing binding", 
            condition.check(new ConditionalOnMissingBinding() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return null;
                }
    
                @Override
                public String[] value() {
                    return new String[]{String.class.getName()};
                }
            }));
        
        Assert.assertFalse(
            "Should fail on two existing bindings",
            condition.check(new ConditionalOnMissingBinding() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return null;
                }
    
                @Override
                public String[] value() {
                    return new String[]{String.class.getName(), Double.class.getName()};
                }
            }));

        Assert.assertTrue(
            "Should successed on one missing binding", 
            condition.check(new ConditionalOnMissingBinding() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return null;
                }
    
                @Override
                public String[] value() {
                    return new String[]{Integer.class.getName()};
                }
            }));

        Assert.assertFalse(
            "Should fail for one existing and one missing binding",
            condition.check(new ConditionalOnMissingBinding() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return null;
                }
    
                @Override
                public String[] value() {
                    return new String[]{String.class.getName(), Integer.class.getName()};
                }
            }));
    }
    
    @Test
    public void testMissingClassCondition() {
        OnMissingClassCondition condition = new OnMissingClassCondition();

        Assert.assertFalse(
                "Should fail on existing class", 
                condition.check(new ConditionalOnMissingClass() {
                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return null;
                    }
        
                    @Override
                    public String[] value() {
                        return new String[]{String.class.getName()};
                    }
                }));
            
            Assert.assertFalse(
                "Should fail on two existing classes",
                condition.check(new ConditionalOnMissingClass() {
                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return null;
                    }
        
                    @Override
                    public String[] value() {
                        return new String[]{String.class.getName(), Double.class.getName()};
                    }
                }));

            Assert.assertTrue(
                "Should successed on one missing class", 
                condition.check(new ConditionalOnMissingClass() {
                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return null;
                    }
        
                    @Override
                    public String[] value() {
                        return new String[]{"nonexistingclass"};
                    }
                }));

            Assert.assertFalse(
                "Should fail for one existing and one missing class",
                condition.check(new ConditionalOnMissingClass() {
                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return null;
                    }
        
                    @Override
                    public String[] value() {
                        return new String[]{String.class.getName(), "nonexistingclass"};
                    }
                }));
    }
    
    @Test
    public void testMissingModuleCondition() {
        
    }
    
    @Test
    public void testModuleCondition() {
        
    }
    
    @Test
    public void testProfileCondition() {
        
    }
    
    @Test
    public void testPropertyCondition() {
        
    }
    
    @Test
    public void testSystemCondition() {
        
    }
    
    @Test
    public void testTestNGCondition() {
        
    }
}
