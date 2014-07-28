package com.netflix.governator.commons_cli.providers;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.google.inject.Inject;
import com.google.inject.spi.BindingTargetVisitor;
import com.google.inject.spi.ProviderInstanceBinding;
import com.google.inject.spi.ProviderWithExtensionVisitor;
import com.google.inject.spi.Toolable;

/**
 * Custom provider that bridges an Option with an injectable String for the option value
 * 
 * @author elandau
 *
 */
public class StringOptionProvider implements ProviderWithExtensionVisitor<String> {

    private CommandLine commandLine;
    private Option option;
    private String defaultValue;

    public StringOptionProvider(Option option, String defaultValue) {
        this.option = option;
    }
    
    @Override
    public String get() {
        return commandLine.getOptionValue(option.getOpt(), defaultValue);
    }

    /**
     * This is needed for 'initialize(injector)' below to be called so the provider
     * can get the injector after it is instantiated.
     */
    @Override
    public <B, V> V acceptExtensionVisitor(
            BindingTargetVisitor<B, V> visitor,
            ProviderInstanceBinding<? extends B> binding) {
        return visitor.visit(binding);
    }

    @Inject
    @Toolable
    void initialize(CommandLine commandLine) {
        this.commandLine = commandLine;
    }
}
