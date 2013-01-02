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

public class SimpleWithMethodAutoBind
{
    private MockWithParameter       f1;

    private MockWithParameter       f2;

    public MockWithParameter getF1()
    {
        return f1;
    }

    @Inject
    public void setF1(@AutoBind("f1") MockWithParameter f1)
    {
        this.f1 = f1;
    }

    public MockWithParameter getF2()
    {
        return f2;
    }

    @Inject
    public void setF2(@AutoBind("f2") MockWithParameter f2)
    {
        this.f2 = f2;
    }
}
