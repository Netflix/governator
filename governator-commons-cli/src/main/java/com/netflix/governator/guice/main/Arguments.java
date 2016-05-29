package com.netflix.governator.guice.main;

/**
 * Placeholder for command line arguments.  For now the placeholder only contains the 
 * command line arguments but may be extended to include additional application context.
 * 
 * @author elandau
 *
 */
public class Arguments {
    private String[] args;
    
    public Arguments(String[] args) {
        this.args = args;
    }
    
    public String[] getArguments() {
        return args;
    }
    
}
