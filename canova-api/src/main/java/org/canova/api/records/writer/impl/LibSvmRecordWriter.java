/*
 *
 *  *
 *  *  * Copyright 2015 Skymind,Inc.
 *  *  *
 *  *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *  *    you may not use this file except in compliance with the License.
 *  *  *    You may obtain a copy of the License at
 *  *  *
 *  *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *  *
 *  *  *    Unless required by applicable law or agreed to in writing, software
 *  *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  *    See the License for the specific language governing permissions and
 *  *  *    limitations under the License.
 *  *
 *
 */

package org.canova.api.records.writer.impl;

import org.canova.api.conf.Configuration;
import org.canova.api.records.reader.LibSvm;
import org.canova.api.writable.Writable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * Each line is in the format:
 * label i:value
 *
 * where is is the current index and value is a double
 * separated by space
 *
 * @author Adam Gibson
 */
public class LibSvmRecordWriter extends LineRecordWriter implements LibSvm {


    public LibSvmRecordWriter(File path) throws FileNotFoundException {
        super(path);
    }

    public LibSvmRecordWriter(File path, boolean append) throws FileNotFoundException {
        super(path, append);
    }

    public LibSvmRecordWriter(Configuration conf) throws FileNotFoundException {
        super(conf);
    }

    public LibSvmRecordWriter() {
    }

    @Override
    public void write(Collection<Writable> record) throws IOException {
        List<Writable> asList = record instanceof  List ? (List<Writable>)  record : new ArrayList<>(record);
        double response = Double.valueOf(asList.get(asList.size() - 1).toString());
        StringBuffer write = new StringBuffer();
        boolean classification = conf.getBoolean(CLASSIFICATION,true);
        if(classification) {
            write.append((int) response);
        }
        else
            write.append(response);
        write.append(" ");

        for(int i = 0; i < asList.size() - 1; i++) {
            //sparse format
            double val = Double.valueOf(asList.get(i).toString());
           
            if(val == 0.0)
                continue;
            write.append(i + 1).append(":");
            try {
                write.append(Integer.valueOf(asList.get(i).toString()));
            }
            catch(NumberFormatException e) {
                write.append(Double.valueOf(asList.get(i).toString()));
            }
            if(i < asList.size() - 1)
                write.append(" ");
        }

        out.write(write.toString().trim().getBytes());
        out.write(NEW_LINE.getBytes());

    }

}
