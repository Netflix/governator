package com.netflix.governator.commons_cli.modules;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.Singleton;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.name.Names;
import com.netflix.governator.annotations.binding.Main;
import com.netflix.governator.commons_cli.providers.StringOptionProvider;

/**
 * Guicify Apache Commons CLI.  
 * 
 * Usages
 * 
 * <pre>
 * {code
 * 
 * // When creating Guice
 * 
 * install(new OptionsModule() {
 *    protected void configure() {
 *       option("f")
 *          .hasArg()
 *          .withLongOpt("filename")
 *          .annotatedWith(Filename.class);  // no need to call create()
 *          
 *    }
 * })
 * 
 * // Inject into any class
 * 
 * @Singleton 
 * public class MyService {
 *    @Inject
 *    public MyService(@Filename String filename) {
 *    }
 * }
 * 
 * // You can also inject CommandLine directly
 * 
 * @Singleton
 * public class MyService {
 *    @Inject
 *    public MyService(CommandLine commandLine) {
 *    }
 * }
 * 
 * }
 * </pre>
 * 
 * @author elandau
 *
 */
public abstract class OptionsModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(OptionsModule.class);
    
    private List<OptionBuilder> builders = Lists.newArrayList();
    private boolean parserIsBound = false;
    
    /**
     * Non-static version of commons CLI OptionBuilder
     * 
     * @author elandau
     */
    protected class OptionBuilder {
        private String longopt;
        private String description;
        private String argName;
        private boolean required;
        private int numberOfArgs = Option.UNINITIALIZED;
        private Object type;
        private boolean optionalArg;
        private char valuesep;
        private String shortopt;
        private String defaultValue;
        private Class<? extends Annotation> annot;

        public OptionBuilder annotatedWith(Class<? extends Annotation> annot) {
            this.annot = annot;
            return this;
        }
        
        public OptionBuilder withLongOpt(String longopt) {
            this.longopt = longopt;
            return this;
        }

        public OptionBuilder withShortOpt(char shortopt) {
            this.shortopt = Character.toString(shortopt);
            return this;
        }

        public OptionBuilder hasArg() {
            this.numberOfArgs = 1;
            return this;
        }

        public OptionBuilder hasArg(boolean hasArg) {
            this.numberOfArgs = hasArg ? 1 : Option.UNINITIALIZED;
            return this;
        }

        public OptionBuilder withArgName(String name) {
            this.argName = name;
            return this;
        }

        public OptionBuilder isRequired() {
            this.required = true;
            return this;
        }

        public OptionBuilder withValueSeparator(char sep) {
            this.valuesep = sep;
            return this;
        }

        public OptionBuilder withValueSeparator() {
            this.valuesep = '=';
            return this;
        }

        public OptionBuilder isRequired(boolean newRequired) {
            this.required = newRequired;
            return this;
        }

        public OptionBuilder hasArgs() {
            this.numberOfArgs = Option.UNLIMITED_VALUES;
            return this;
        }

        public OptionBuilder hasArgs(int num) {
            this.numberOfArgs = num;
            return this;
        }

        public OptionBuilder hasOptionalArg() {
            this.numberOfArgs = 1;
            this.optionalArg = true;
            return this;
        }

        public OptionBuilder hasOptionalArgs() {
            this.numberOfArgs = Option.UNLIMITED_VALUES;
            this.optionalArg = true;
            return this;
        }

        public OptionBuilder hasOptionalArgs(int numArgs) {
            this.numberOfArgs = numArgs;
            this.optionalArg = true;
            return this;
        }

        public OptionBuilder withType(Object newType) {
            this.type = newType;
            return this;
        }

        public OptionBuilder withDescription(String newDescription) {
            this.description = newDescription;
            return this;
        }
        
        public OptionBuilder withDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        Option create() throws IllegalArgumentException
        {
            Preconditions.checkNotNull(shortopt);
            
            Option option = null;
            // create the option
            option = new Option(shortopt, description);

            // set the option properties
            option.setLongOpt(longopt);
            option.setRequired(required);
            option.setOptionalArg(optionalArg);
            option.setArgs(numberOfArgs);
            option.setType(type);
            option.setValueSeparator(valuesep);
            option.setArgName(argName);

            // return the Option instance
            return option;
        }
    }
    
    /**
     * On injection of CommandLine execute the BasicParser
     * @author elandau
     */
    @Singleton
    public static class CommandLineProvider implements Provider<CommandLine> {
        private final Options options;
        private final String[] arguments;
        private final Parser parser;
        
        @Inject
        public CommandLineProvider(Options options, @Main String[] arguments, Parser parser) {
            this.options = options;
            this.arguments = arguments;
            this.parser = parser;
        }
        
        @Override
        public CommandLine get() {
            try {
                return parser.parse(options, arguments);
            } catch (ParseException e) {
                throw new ProvisionException("Error parsing command line arguments", e);
            }
        }
    }
    
    @Override
    protected final void configure() {
        configureOptions();
        
        Options options = new Options();
        for (OptionBuilder builder : builders) {
            Option option = builder.create();
            if (builder.annot != null) {
                bind(String.class)
                    .annotatedWith(builder.annot)
                    .toProvider(new StringOptionProvider(option, builder.defaultValue))
                    .asEagerSingleton();
                
                LOG.info("Binding option to annotation : " + builder.annot.getName());
            }
            else {
                bind(String.class)
                    .annotatedWith(Names.named(option.getOpt()))
                    .toProvider(new StringOptionProvider(option, builder.defaultValue))
                    .asEagerSingleton();
                LOG.info("Binding option to String : " + option.getOpt());
            }
            options.addOption(option);
        }
        
        bind(Options.class).toInstance(options);
        bind(CommandLine.class).toProvider(CommandLineProvider.class);
        
        if (!parserIsBound) {
            bindParser().to(BasicParser.class);
        }
    }
    
    protected abstract void configureOptions();

    /**
     * @param shortopt
     * @return Return a builder through which a single option may be configured
     */
    protected OptionBuilder option(char shortopt) {
        OptionBuilder builder = new OptionBuilder().withShortOpt(shortopt);
        builders.add(builder);
        return builder;
    }
    
    /**
     * Bind any parser.  BasicParser is used by default if no other parser is provided.
     */
    protected AnnotatedBindingBuilder<Parser> bindParser() {
        parserIsBound = true;
        return bind(Parser.class);
    }
}
