/*
 * Copyright 2013 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.netflix.governator.lifecycle.warmup;

import com.netflix.governator.lifecycle.LifecycleManager;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Thrown by {@link LifecycleManager#start()} if there are errors in warm up methods
 */
public class WarmUpException extends Exception
{
    private final WarmUpErrors errors;

    WarmUpException(WarmUpErrors errors)
    {
        this.errors = errors;
    }

    /**
     * @return the errors
     */
    public WarmUpErrors getErrors()
    {
        return errors;
    }

    @Override
    public String getMessage()
    {
        StringBuilder       str = new StringBuilder();
        boolean     first = true;
        for ( WarmUpErrors.Error error : errors )
        {
            if ( first )
            {
                first = false;
            }
            else
            {
                str.append("; ");
            }
            str.append(error.getContext());
            //noinspection ThrowableResultOfMethodCallIgnored
            String message = error.getException().getMessage();
            if ( message != null )
            {
                str.append(": ").append(message);
            }
        }
        return str.toString();
    }

    @Override
    public void printStackTrace()
    {
        printStackTrace(System.err);
    }

    @Override
    public void printStackTrace(PrintStream s)
    {
        PrintWriter out = new PrintWriter(new OutputStreamWriter(s));
        printStackTrace(out);
        out.close();
    }

    @Override
    public void printStackTrace(PrintWriter out)
    {
        boolean     first = true;
        for ( WarmUpErrors.Error error : errors )
        {
            if ( first )
            {
                first = false;
            }
            else
            {
                out.println();
            }
            out.println(error.getContext());
            //noinspection ThrowableResultOfMethodCallIgnored
            error.getException().printStackTrace(out);
        }
    }
}
