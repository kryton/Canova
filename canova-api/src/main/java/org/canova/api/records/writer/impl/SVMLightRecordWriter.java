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
import org.canova.api.writable.Writable;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 *
 * SVM Light Record Writer
 *
 * @author Adam Gibson
 *
 */
public class SVMLightRecordWriter extends FileRecordWriter {
    public SVMLightRecordWriter() {
    }

    public SVMLightRecordWriter(File path) throws FileNotFoundException {
        super(path);
    }
    public SVMLightRecordWriter(File path,boolean append) throws FileNotFoundException {
        super(path,append);
    }

    public SVMLightRecordWriter(Configuration conf) throws FileNotFoundException {
        super(conf);
    }

    @Override
    public void write(Collection<Writable> record) throws IOException {
        if(!record.isEmpty()) {
            List<Writable> recordList = record instanceof List ? (List<Writable>) record : new ArrayList<>(record);
            StringBuilder result = new StringBuilder();

            // get the label
            result.append(recordList.get(recordList.size() - 1).toString());

            // get only the non-zero entries
            Double value = 0.0;

            for (int i = 0; i < recordList.size() - 1; i++) {

                value = Double.valueOf(recordList.get(i).toString());

                // TODO make this sparse. the blocker here is ND4J not supporting sparse files.
                // Issue in tracked here - https://github.com/deeplearning4j/nd4j/issues/202
                //if ( value > 0.0 ) {
                	result.append(" ").append(i + 1).append(":").append(Double.valueOf(recordList.get(i).toString()));
                //}

            }

            out.write(result.toString().getBytes());
            out.write(NEW_LINE.getBytes());

        }

    }
}
