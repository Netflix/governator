package com.netflix.governator.guice;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.MembersInjector;
import com.google.inject.Module;
import com.google.inject.PrivateBinder;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.AnnotatedConstantBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.matcher.Matcher;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.spi.Message;
import com.google.inject.spi.TypeConverter;
import com.google.inject.spi.TypeListener;
import com.netflix.governator.assets.AssetLoader;
import com.netflix.governator.assets.AssetParametersView;
import com.netflix.governator.configuration.ConfigurationProvider;
import org.aopalliance.intercept.MethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

public class BootstrapBinder implements Binder
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Binder binder;
    private final List<ConfigurationProviderBinding> configurationProviderBindings = Lists.newArrayList();

    @Override
    public void bindInterceptor(Matcher<? super Class<?>> classMatcher, Matcher<? super Method> methodMatcher, MethodInterceptor... interceptors)
    {
        binder.bindInterceptor(classMatcher, methodMatcher, interceptors);
    }

    @Override
    public void bindScope(Class<? extends Annotation> annotationType, Scope scope)
    {
        binder.bindScope(annotationType, scope);
    }

    @Override
    public <T> LinkedBindingBuilder<T> bind(Key<T> key)
    {
        warnOnSpecialized(key.getTypeLiteral().getRawType());
        return binder.bind(key);
    }

    @Override
    public <T> AnnotatedBindingBuilder<T> bind(TypeLiteral<T> typeLiteral)
    {
        warnOnSpecialized(typeLiteral.getRawType());
        return binder.bind(typeLiteral);
    }

    @Override
    public <T> AnnotatedBindingBuilder<T> bind(Class<T> type)
    {
        warnOnSpecialized(type);
        return binder.bind(type);
    }

    @Override
    public AnnotatedConstantBindingBuilder bindConstant()
    {
        return binder.bindConstant();
    }

    @Override
    public <T> void requestInjection(TypeLiteral<T> type, T instance)
    {
        binder.requestInjection(type, instance);
    }

    @Override
    public void requestInjection(Object instance)
    {
        binder.requestInjection(instance);
    }

    @Override
    public void requestStaticInjection(Class<?>... types)
    {
        binder.requestStaticInjection(types);
    }

    @Override
    public void install(Module module)
    {
        binder.install(module);
    }

    @Override
    public Stage currentStage()
    {
        return binder.currentStage();
    }

    @Override
    public void addError(String message, Object... arguments)
    {
        binder.addError(message, arguments);
    }

    @Override
    public void addError(Throwable t)
    {
        binder.addError(t);
    }

    @Override
    public void addError(Message message)
    {
        binder.addError(message);
    }

    @Override
    public <T> Provider<T> getProvider(Key<T> key)
    {
        return binder.getProvider(key);
    }

    @Override
    public <T> Provider<T> getProvider(Class<T> type)
    {
        return binder.getProvider(type);
    }

    @Override
    public <T> MembersInjector<T> getMembersInjector(TypeLiteral<T> typeLiteral)
    {
        return binder.getMembersInjector(typeLiteral);
    }

    @Override
    public <T> MembersInjector<T> getMembersInjector(Class<T> type)
    {
        return binder.getMembersInjector(type);
    }

    @Override
    public void convertToTypes(Matcher<? super TypeLiteral<?>> typeMatcher, TypeConverter converter)
    {
        binder.convertToTypes(typeMatcher, converter);
    }

    @Override
    public void bindListener(Matcher<? super TypeLiteral<?>> typeMatcher, TypeListener listener)
    {
        binder.bindListener(typeMatcher, listener);
    }

    @Override
    public Binder withSource(Object source)
    {
        return binder.withSource(source);
    }

    @Override
    public Binder skipSources(Class... classesToSkip)
    {
        return binder.skipSources(classesToSkip);
    }

    @Override
    public PrivateBinder newPrivateBinder()
    {
        return binder.newPrivateBinder();
    }

    @Override
    public void requireExplicitBindings()
    {
        binder.requireExplicitBindings();
    }

    @Override
    public void disableCircularProxies()
    {
        binder.disableCircularProxies();
    }

    /**
     * Begin binding a required asset name/value to a loader
     *
     * @param requiredAssetValue asset name/value
     * @return binder
     */
    public LinkedBindingBuilder<AssetLoader> bindRequiredAsset(String requiredAssetValue)
    {
        requiredAssetValue = Preconditions.checkNotNull(requiredAssetValue, "requiredAssetValue cannot be null");
        MapBinder<String, AssetLoader>  mapBinder = MapBinder.newMapBinder(binder, String.class, AssetLoader.class);
        return mapBinder.addBinding(requiredAssetValue);
    }

    /**
     * Begin binding a required asset name/value to an asset parameter
     *
     * @param requiredAssetValue asset name/value
     * @return binder
     */
    public LinkedBindingBuilder<AssetParametersView> bindRequiredAssetParameters(String requiredAssetValue)
    {
        requiredAssetValue = Preconditions.checkNotNull(requiredAssetValue, "requiredAssetValue cannot be null");
        MapBinder<String, AssetParametersView>  mapBinder = MapBinder.newMapBinder(binder, String.class, AssetParametersView.class);
        return mapBinder.addBinding(requiredAssetValue);
    }

    /**
     * Use this to bind {@link ConfigurationProvider}s. Do NOT use standard Guice binding.
     *
     * @return configuration binding builder
     */
    public ConfigurationProviderBuilder bindConfigurationProvider()
    {
        return new ConfigurationProviderBuilder()
        {
            @Override
            public void to(Class<? extends ConfigurationProvider> implementation)
            {
                configurationProviderBindings.add(new ConfigurationProviderBinding(implementation, null, null));
            }

            @Override
            public void toInstance(ConfigurationProvider implementation)
            {
                configurationProviderBindings.add(new ConfigurationProviderBinding(null, implementation, null));
            }

            @Override
            public void toProvider(Provider<? extends ConfigurationProvider> implementation)
            {
                configurationProviderBindings.add(new ConfigurationProviderBinding(null, null, implementation));
            }
        };
    }

    BootstrapBinder(Binder binder)
    {
        this.binder = binder;
    }

    List<ConfigurationProviderBinding> getConfigurationProviderBindings()
    {
        return configurationProviderBindings;
    }

    private<T> void    warnOnSpecialized(Class<T> clazz)
    {
        if ( AssetLoader.class.isAssignableFrom(clazz) || AssetParametersView.class.isAssignableFrom(clazz) || ConfigurationProvider.class.isAssignableFrom(clazz) )
        {
            log.warn("You should use the specialized binding methods for AssetLoaders, AssetParameters and ConfigurationProviders");
        }
    }
}
