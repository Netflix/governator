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

import com.google.common.collect.Iterators;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Container for errors that occur in warm up methods
 */
public class WarmUpErrors implements Iterable<WarmUpErrors.Error>
{
    private final Queue<Error>      errors = new ConcurrentLinkedQueue<Error>();

    /**
     * Record of a single error
     */
    public static class Error
    {
        private final Throwable exception;
        private final String context;

        /**
         * @return The error
         */
        public Throwable getException()
        {
            return exception;
        }

        /**
         * @return object/context of the error
         */
        public String getContext()
        {
            return context;
        }

        private Error(Throwable exception, String context)
        {
            this.exception = exception;
            this.context = context;
        }
    }

    /**
     * Add an error and resolve to its cause if needed
     *
     * @param e error
     * @param context error context
     * @return resolved error
     */
    public Throwable     addError(Throwable e, String context)
    {
        while ( InvocationTargetException.class.isAssignableFrom(e.getClass()) )
        {
            if ( e.getCause() != null )
            {
                e = e.getCause();
            }
        }

        errors.add(new Error(e, context));

        return e;
    }

    /**
     * Throw a WarmUpException if there are errors
     *
     * @throws WarmUpException if there are errors
     */
    public void     throwIfErrors() throws WarmUpException
    {
        if ( errors.size() > 0 )
        {
            throw new WarmUpException(this);
        }
    }

    @Override
    public Iterator<Error> iterator()
    {
        return Iterators.unmodifiableIterator(errors.iterator());
    }
}
