package com.netflix.governator.test;

import java.io.PrintStream;

import com.google.common.base.Strings;
import com.google.inject.spi.ProvisionListener;

/**
 * Use TracingProvisionListener to debug issues with Guice Injector creation by tracing 
 * the object initialization path.  Various hooks are provided for the different stages 
 * of object instantiation: before, after and on error.
 * 
 * To enable add the following binding in any guice module
 * <code>
 *     bindListener(Matchers.any(), TracingProvisionListener.createDefault());
 * </code>
 *      
 */
public class TracingProvisionListener implements ProvisionListener {
    
    private int indent = 0;
    
    private final String prefix;
    private final int indentAmount;
    private final BindingFormatter beforeFormatter;
    private final BindingFormatter afterFormatter;
    private final PrintStream stream;

    private ErrorFormatter errorFormatter;
    
    private static final BindingFormatter EMPTY_BINDING_FORMATTER = new BindingFormatter() {
        @Override
        public <T> String format(ProvisionInvocation<T> provision) {
            return "";
        }
    };
    
    private static final BindingFormatter SIMPLE_BINDING_FORMATTER = new BindingFormatter() {
        @Override
        public <T> String format(ProvisionInvocation<T> provision) {
            return provision.getBinding().getKey().toString();
        }
    };
    
    private static final ErrorFormatter SIMPLE_ERROR_FORMATTER = new ErrorFormatter() {
        @Override
        public <T> String format(ProvisionInvocation<T> provision, Throwable t) {
            return String.format("Error creating '%s'. %s", provision.getBinding().getKey().toString(), t.getMessage());
        }
    };
    
    /**
     * Builder for customizing the tracer output.
     */
    public static class Builder {
        private String prefix = "";
        private int indentAmount = 2;
        private BindingFormatter beforeFormatter = SIMPLE_BINDING_FORMATTER;
        private BindingFormatter afterFormatter = EMPTY_BINDING_FORMATTER;
        private ErrorFormatter errorFormatter = SIMPLE_ERROR_FORMATTER;
        private PrintStream stream = System.out;
        
        /**
         * Provide a custom formatter for messages written before a type is provisioned
         * Returning null or empty stream will result in no message being written
         * 
         * @param formatter
         */
        public Builder formatBeforeWith(BindingFormatter formatter) {
            this.beforeFormatter = formatter;
            return this;
        }
        
        /**
         * Provide a custom formatter for messages written after a type is provisioned.
         * Returning null or empty stream will result in no message being written
         * 
         * @param formatter
         */
        public Builder formatAfterWith(BindingFormatter formatter) {
            this.afterFormatter = formatter;
            return this;
        }

        /**
         * Provide a custom formatter for messages written when type provision throws 
         * an exception
         * 
         * @param formatter
         */
        public Builder formatErrorsWith(ErrorFormatter formatter) {
            this.errorFormatter = formatter;
            return this;
        }
        
        /**
         * Indentation increment for each nested provisioning
         * @param amount - Number of spaces to indent
         */
        public Builder indentBy(int amount) {
            this.indentAmount = amount;
            return this;
        }

        /**
         * Customized the PrintStream to which messages are written.  By default
         * messages are written to System.err
         * 
         * @param stream
         */
        public Builder writeTo(PrintStream stream) {
            this.stream = stream;
            return this;
        }

        /**
         * String to prefix each row (prior to indentation).  Default is ""
         * 
         * @param prefix
         */
        public Builder prefixLinesWith(String prefix) {
            this.prefix = prefix;
            return this;
        }
        
        public TracingProvisionListener build() {
            return new TracingProvisionListener(this);
        }
    }
    
    public static interface BindingFormatter {
        <T> String format(ProvisionInvocation<T> provision);
    }
    
    public static interface ErrorFormatter {
        <T> String format(ProvisionInvocation<T> provision, Throwable error);
    }
    
    public static TracingProvisionListener createDefault() {
        return new TracingProvisionListener(newBuilder());
    }
    
    public static Builder newBuilder() {
        return new Builder();
    }
    
    private TracingProvisionListener(Builder builder) {
        this.indentAmount = builder.indentAmount;
        this.beforeFormatter = builder.beforeFormatter;
        this.afterFormatter = builder.afterFormatter;
        this.errorFormatter = builder.errorFormatter;
        this.prefix = builder.prefix;
        this.stream = builder.stream;
    }
    
    @Override
    public <T> void onProvision(ProvisionInvocation<T> provision) {
        writeString(beforeFormatter.format(provision));
        indent += indentAmount;
        try {
            provision.provision();
            writeString(afterFormatter.format(provision));
        }
        catch (Throwable t) {
            writeString(errorFormatter.format(provision, t));
            throw t;
        }
        finally {
            indent -= indentAmount;
        }
    }
    
    private void writeString(String str) {
        if (str != null && !str.isEmpty()) {
            stream.println(prefix + Strings.repeat(" ", indent) + str);
        }
    }

}
