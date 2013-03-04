/*
 * Copyright 2010 Proofpoint, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.governator.configuration;

// copied from https://raw.github.com/proofpoint/platform/master/bootstrap/src/main/java/com/proofpoint/bootstrap/ColumnPrinter.java

import com.google.common.collect.Lists;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A utility for outputting columnar text
 */
class ColumnPrinter
{
    private final List<List<String>> data = Lists.newArrayList();
    private final List<String> columnNames = Lists.newArrayList();
    private int margin;

    private static final int DEFAULT_MARGIN = 2;

    ColumnPrinter()
    {
        margin = DEFAULT_MARGIN;
    }

    /**
     * Add a column
     *
     * @param columnName name of the column
     */
    void addColumn(String columnName)
    {
        data.add(new ArrayList<String>());
        columnNames.add(columnName);
    }

    /**
     * Add a value to the first column with the given name
     *
     * @param columnName name of the column to add to
     * @param value      value to add
     */
    void addValue(String columnName, String value)
    {
        addValue(columnNames.indexOf(columnName), value);
    }

    /**
     * Add a value to the nth column
     *
     * @param columnIndex n
     * @param value       value to add
     */
    void addValue(int columnIndex, String value)
    {
        if ( (columnIndex < 0) || (columnIndex >= data.size()) )
        {
            throw new IllegalArgumentException();
        }

        List<String> stringList = data.get(columnIndex);
        stringList.add(value);
    }

    /**
     * Change the margin from the default
     *
     * @param margin new margin between columns
     */
    void setMargin(int margin)
    {
        this.margin = margin;
    }

    /**
     * Output the columns/data
     *
     * @param out stream
     */
    void print(PrintWriter out)
    {
        for ( String s : generate() )
        {
            out.println(s);
        }
    }

    /**
     * Generate the output as a list of string lines
     *
     * @return lines
     */
    List<String> generate()
    {
        List<String> lines = Lists.newArrayList();
        StringBuilder workStr = new StringBuilder();

        List<AtomicInteger> columnWidths = getColumnWidths();
        List<Iterator<String>> dataIterators = getDataIterators();

        Iterator<AtomicInteger> columnWidthIterator = columnWidths.iterator();
        for ( String columnName : columnNames )
        {
            int thisWidth = columnWidthIterator.next().intValue();
            printValue(workStr, columnName, thisWidth);
        }
        pushLine(lines, workStr);

        boolean done = false;
        while ( !done )
        {
            boolean hadValue = false;
            Iterator<Iterator<String>> rowIterator = dataIterators.iterator();
            for ( AtomicInteger width : columnWidths )
            {
                Iterator<String> thisDataIterator = rowIterator.next();
                if ( thisDataIterator.hasNext() )
                {
                    hadValue = true;

                    String value = thisDataIterator.next();
                    printValue(workStr, value, width.intValue());
                }
                else
                {
                    printValue(workStr, "", width.intValue());
                }
            }
            pushLine(lines, workStr);

            if ( !hadValue )
            {
                done = true;
            }
        }

        return lines;
    }

    private void pushLine(List<String> lines, StringBuilder workStr)
    {
        lines.add(workStr.toString());
        workStr.setLength(0);
    }

    private void printValue(StringBuilder str, String value, int thisWidth)
    {
        str.append(String.format(widthSpec(thisWidth), value));
    }

    private String widthSpec(int thisWidth)
    {
        return "%-" + (thisWidth + margin) + "s";
    }

    private List<Iterator<String>> getDataIterators()
    {
        List<Iterator<String>> dataIterators = Lists.newArrayList();
        for ( List<String> valueList : data )
        {
            dataIterators.add(valueList.iterator());
        }
        return dataIterators;
    }

    private List<AtomicInteger> getColumnWidths()
    {
        List<AtomicInteger> columnWidths = Lists.newArrayList();
        for ( String columnName : columnNames )
        {
            columnWidths.add(new AtomicInteger(columnName.length()));
        }

        int columnIndex = 0;
        for ( List<String> valueList : data )
        {
            AtomicInteger width = columnWidths.get(columnIndex++);
            for ( String value : valueList )
            {
                width.set(Math.max(value.length(), width.intValue()));
            }
        }

        return columnWidths;
    }
}