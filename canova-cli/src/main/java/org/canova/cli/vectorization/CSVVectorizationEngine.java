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

package org.canova.cli.vectorization;

import au.com.bytecode.opencsv.CSVParser;
import org.canova.api.conf.Configuration;
import org.canova.api.exceptions.CanovaException;
import org.canova.api.formats.output.OutputFormat;
import org.canova.api.io.data.DoubleWritable;
import org.canova.api.io.data.Text;
import org.canova.api.records.writer.RecordWriter;
import org.canova.api.writable.Writable;
import org.canova.cli.csv.schema.CSVInputSchema;
import org.canova.cli.csv.schema.CSVSchemaColumn;
import org.canova.cli.shuffle.Shuffler;
import org.canova.cli.subcommands.Vectorize;
import org.canova.cli.vectorization.VectorizationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Vectorization Engine
 * - takes CSV input and converts it to a transformed vector output in a standard format
 * - uses the input CSV schema and the collected statistics from a pre-pass
 *
 * @author josh
 */
public class CSVVectorizationEngine extends VectorizationEngine {
  private CSVParser csvParser = new CSVParser();
  private static final Logger log = LoggerFactory.getLogger(CSVVectorizationEngine.class);


  private CSVInputSchema inputSchema = null;
  //private CSVVectorizationEngine vectorizer = null;


  // this picks up the input schema file from the properties file and loads it
  private void loadInputSchemaFile() throws Exception {
      String schemaFilePath = (String) this.configProps.get("canova.input.vector.schema");
      this.inputSchema = new CSVInputSchema();
      this.inputSchema.parseSchemaFile(schemaFilePath);
  //    this.vectorizer = new CSVVectorizationEngine();
  }

  /**
   *
   * This is where our custom vectorization engine does its thing
 * @throws CanovaException
 * @throws IOException
 * @throws InterruptedException
   *
   */
  public void execute() throws CanovaException, IOException, InterruptedException {



	  try {
		this.loadInputSchemaFile();
	} catch (Exception e1) {
		// TODO Auto-generated catch block
		//e1.printStackTrace();
		log.error("No schema loaded for CSV transforms.");
		return;
	}

	  int linenum=0;

      // 1. Do a pre-pass to collect dataset statistics
      while (reader.hasNext()) {
    	  linenum++;
          Collection<Writable> w = reader.next();

          // TODO: this will end up processing key-value pairs
          try {
              this.inputSchema.evaluateInputRecord(w.toArray()[0].toString());
          } catch (Exception e) {
              // TODO Auto-generated catch block
              log.error("Exception on line "+linenum);
              log.error("Exception line: "+w.toArray()[0].toString());
              e.printStackTrace();
          }

      }

      reader.close();

      // 2. computate the dataset statistics
      this.inputSchema.computeDatasetStatistics();

      // 2.a. debug dataset stats
/*      String schema_print_key = "canova.input.statistics.debug.print";
      if (null != this.configProps.get(schema_print_key)) {
          String printSchema = (String) this.configProps.get(schema_print_key);
          if ("true".equals(printSchema.trim().toLowerCase())) {
              //this.debugLoadedConfProperties();
              this.inputSchema.debugPringDatasetStatistics();
          }
      }
*/

      if (this.printStats) {
    	  this.inputSchema.debugPringDatasetStatistics();
      }




      // 1. make second pass to do transforms now that we have stats on the datasets

      // 1.a. reset the reader
		reader = inputFormat.createReader(split);


      Configuration conf = new Configuration();
      conf.set( OutputFormat.OUTPUT_PATH, this.outputFilename );

      if (shuffleOn) {

    	  Shuffler shuffle = new Shuffler();

	      RecordWriter writer = outputFormat.createWriter(conf); //new SVMLightRecordWriter(tmpOutSVMLightFile,true);

	      while (reader.hasNext()) {

	          Collection<Writable> w = reader.next();

	          String line = w.toArray()[0].toString();

	          // TODO: we need to be re-using objects here for heap churn purposes

	          if (!Strings.isNullOrEmpty(line)) {
	          //    writer.write(this.vectorizeToWritable("", line, this.inputSchema));
	        	  shuffle.addRecord( this.vectorizeToWritable("", line, this.inputSchema) );
	          }

	      }

			while (shuffle.hasNext()) {

				Collection<Writable> shuffledRecord = shuffle.next();
				writer.write( shuffledRecord );

			}


	      reader.close();
	      writer.close();


      } else {

	      RecordWriter writer = outputFormat.createWriter(conf); //new SVMLightRecordWriter(tmpOutSVMLightFile,true);
          linenum =0;
	      while (reader.hasNext()) {
              linenum++;
	          Collection<Writable> w = reader.next();

	          String line = w.toArray()[0].toString();
	          // TODO: this will end up processing key-value pairs

	          // TODO: this is where the transform system would live (example: execute the filter transforms, etc, here)

	          // this outputVector needs to be ND4J
	          // TODO: we need to be re-using objects here for heap churn purposes
	          //INDArray outputVector = this.vectorizer.vectorize( "", line, this.inputSchema );
	          if (!Strings.isNullOrEmpty(line)) {
                  try {
                      writer.write(this.vectorizeToWritable("", line, this.inputSchema));
                  } catch (Exception e) {
                      log.error("Error Writing Line:"+linenum);
                      throw  e;
                  }
	          }

	      }

	      reader.close();
	      writer.close();

      }




  }

  /**
   * Use statistics collected from a previous pass to vectorize (or drop) each column
   *
   * @return
   */
  public Collection<Writable> vectorize(String key, String value, CSVInputSchema schema) throws IOException {

    //INDArray
    Collection<Writable> ret = new ArrayList<>();

    // TODO: this needs to be different (needs to be real vector representation
    //String outputVector = "";
    //String[] columns = value.split(schema.delimiter);
      String[] columns = csvParser.parseLine(value);

    if (columns[0].trim().equals("")) {
      //	log.info("Skipping blank line");
      return null;
    }

    int srcColIndex = 0;
    int dstColIndex = 0;

    //log.info( "> Engine.vectorize() ----- ");

    double label = 0;

    // scan through the columns in the schema / input csv data
    for (Map.Entry<String, CSVSchemaColumn> entry : schema.getColumnSchemas().entrySet()) {

      String colKey = entry.getKey();
      CSVSchemaColumn colSchemaEntry = entry.getValue();

      // produce the per column transform based on stats

      switch (colSchemaEntry.transform) {
        case SKIP:
          // dont append this to the output vector, skipping
          break;
        case LABEL:
          //	log.info( " label value: " + columns[ srcColIndex ] );
          label = colSchemaEntry.transformColumnValue(columns[srcColIndex].trim());
          break;
        default:
          //	log.info( " column value: " + columns[ srcColIndex ] );
          double convertedColumn = colSchemaEntry.transformColumnValue(columns[srcColIndex].trim());
          // add this value to the output vector
          ret.add(new DoubleWritable(convertedColumn));
          dstColIndex++;
          break;
      }

      srcColIndex++;

    }
    ret.add(new DoubleWritable(label));
    //dstColIndex++;

    return ret;
  }

  /**
   * Use statistics collected from a previous pass to vectorize (or drop) each column
   *
   * @return
   */
  public Collection<Writable> vectorizeToWritable(String key, String value, CSVInputSchema schema) throws IOException {

    //INDArray
    //INDArray ret = this.createArray( schema.getTransformedVectorSize() );
    Collection<Writable> ret = new ArrayList<>();

    // TODO: this needs to be different (needs to be real vector representation
    //String outputVector = "";
   // String[] columns = value.split(schema.delimiter);
    String[] columns = csvParser.parseLine(value);

    if (columns[0].trim().equals("")) {
      //	log.info("Skipping blank line");
      return null;
    }

    int srcColIndex = 0;
    int dstColIndex = 0;

    //log.info( "> Engine.vectorize() ----- ");

    // scan through the columns in the schema / input csv data
    for (Map.Entry<String, CSVSchemaColumn> entry : schema.getColumnSchemas().entrySet()) {

      String colKey = entry.getKey();
      CSVSchemaColumn colSchemaEntry = entry.getValue();

      // produce the per column transform based on stats

      switch (colSchemaEntry.transform) {
        case SKIP:
          // dont append this to the output vector, skipping
          break;
        default:
          //	log.info( " column value: " + columns[ srcColIndex ] );
          double convertedColumn = colSchemaEntry.transformColumnValue(columns[srcColIndex].trim());
          // add this value to the output vector
          //ret.putScalar(dstColIndex, convertedColumn);
          ret.add(new Text(convertedColumn + ""));
          dstColIndex++;
          break;
      }

      srcColIndex++;
    }

    return ret;
  }

}
