package com.netflix.governator.configuration;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

import javax.xml.bind.DatatypeConverter;

import com.google.common.base.Supplier;

/**
 * Special supplier that converts a string date to a Date
 * @author elandau
 *
 */
public class DateWithDefaultSupplier implements Supplier<Date> {
    private Date defaultValue;
    private final Supplier<String> supplier;
    private final DateFormat formatter;
    
    public DateWithDefaultSupplier(Supplier<String> supplier, Date defaultValue) {
        this.defaultValue = defaultValue;
        this.supplier     = supplier;
        
        formatter = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
        formatter.setLenient(false);
    }
    
    @Override
    public Date get() {
        String current = supplier.get();
        if (current != null) {
            Date newDate = parseDate(current);
            if (newDate != null)
                return newDate;
        }        
        return defaultValue;
    }

    private Date parseDate(String value) 
    {
        if (value == null)
            return null;
        
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
