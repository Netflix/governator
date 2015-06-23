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

package com.netflix.governator.configuration;

import com.google.common.base.Supplier;
import javax.xml.bind.DatatypeConverter;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

/**
 * Special supplier that converts a string date to a Date
 *
 * @author elandau
 */
public class DateWithDefaultSupplier implements Supplier<Date>
{
    private final Date defaultValue;
    private final Supplier<String> supplier;
    private final DateFormat formatter;

    public DateWithDefaultSupplier(Supplier<String> supplier, Date defaultValue)
    {
        this.defaultValue = defaultValue;
        this.supplier = supplier;

        formatter = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
        formatter.setLenient(false);
    }

    @Override
    public Date get()
    {
        String current = supplier.get();
        if ( current != null )
        {
            Date newDate = parseDate(current);
            if ( newDate != null )
            {
                return newDate;
            }
        }
        return defaultValue;
    }

    private Date parseDate(String value)
    {
        if ( value == null )
        {
            return null;
        }

        try
        {
            return formatter.parse(value);
        }
        catch ( ParseException e )
        {
            // ignore as the fallback is the DatattypeConverter.
        }

        try
        {
            return DatatypeConverter.parseDateTime(value).getTime();
        }
        catch ( IllegalArgumentException e )
        {
            // ignore as the fallback is the DatattypeConverter.
        }

        return null;
    }
}
