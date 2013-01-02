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

package com.netflix.governator.autobind;

import com.google.inject.Inject;
import com.netflix.governator.annotations.AutoBind;

public class SimpleWithMultipleAutoBinds
{
    private final MockWithParameter arg1;
    private final MockWithParameter arg2;
    private final MockWithParameter arg3;
    private final MockWithParameter arg4;

    @Inject
    public SimpleWithMultipleAutoBinds
        (
            @AutoBind("one") MockWithParameter arg1,
            @AutoBind("two") MockWithParameter arg2,
            @AutoBind("three") MockWithParameter arg3,
            @AutoBind("four") MockWithParameter arg4
        )
    {
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.arg3 = arg3;
        this.arg4 = arg4;
    }

    public MockWithParameter getArg1()
    {
        return arg1;
    }

    public MockWithParameter getArg2()
    {
        return arg2;
    }

    public MockWithParameter getArg3()
    {
        return arg3;
    }

    public MockWithParameter getArg4()
    {
        return arg4;
    }
}
