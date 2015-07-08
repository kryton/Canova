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

package org.canova.cli.csv.schema;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*

	purpose: to parse and represent the input schema + column transforms of CSV data to vectorize



TODO:

- produce the following metrics per column

		min
		max
		stddev
		median (percentiles 1, 25, 50, 75, 99)
		range
		outliers
		classify: {Normal, uniform, and skewed} distributions
		render: Histograms and box plots





*/
public class CSVSchemaColumn {
	private static final Logger log = LoggerFactory.getLogger(CSVSchemaColumn.class);

	public enum ColumnType { NUMERIC, DATE, NOMINAL, STRING, NUMERICDEFAULT }
	public enum TransformType { COPY, SKIP, BINARIZE, NORMALIZE, LABEL }

	public String name = ""; // the name of the attribute/column
	public ColumnType columnType = null;
	public TransformType transform = null;

	/*
	 * TODO:
	 * - how do we model statistics per column?
	 *
	 */
	public double minValue = Double.NaN;
	public double maxValue = Double.NaN;
	//public double stddev = 0;
	//public double median = 0;

	// used to track input values that do not match the schema data type
	public long invalidDataEntries = 0;



	// we want to track the label counts to understand the class balance
	// layout: { columnName, columnID, occurenceCount }
	public Map<String, Pair<Integer, Integer>> recordLabels = new LinkedHashMap<>();


	public CSVSchemaColumn(String colName, ColumnType colType, TransformType transformType) {

		this.name = colName;
		this.columnType = colType;
		this.transform = transformType;

	}

	/**
	 * This method collects dataset statistics about the column that we'll
	 * need later to
	 * 1. convert the column based on the requested transforms
	 * 2. report on column specfic statistics to give visibility into the properties of the input dataset
	 *
	 * @param value
	 * @throws Exception
	 */
	public void evaluateColumnValue(String value) throws Exception {

		/*
		 * Need to get stats for the following transforms here:
		 * 1. normalize
		 * 2. binarize
		 *
		 */

		if ( ColumnType.NUMERIC == this.columnType|| ColumnType.NUMERICDEFAULT== this.columnType) {
			// then we want to look at min/max values if the field isn't blank. (and allowed to be blank)

			if ((ColumnType.NUMERICDEFAULT != this.columnType) || !value.trim().isEmpty()) {

				double tmpVal;
				tmpVal = Double.parseDouble(value);

				// System.out.println( "converted: " + tmpVal );

				if (Double.isNaN(tmpVal)) {
					throw new Exception("The column was defined as Numeric yet could not be parsed as a Double");
				}

				if (Double.isNaN(this.minValue)) {

					this.minValue = tmpVal;

				} else if (tmpVal < this.minValue) {

					this.minValue = tmpVal;

				}

				if (Double.isNaN(this.maxValue)) {

					this.maxValue = tmpVal;

				} else if (tmpVal > this.maxValue) {

					this.maxValue = tmpVal;

				}
			}

		} else if ( ColumnType.NOMINAL == this.columnType || TransformType.LABEL == this.transform ) {

		//	System.out.println( "> label '" + value + "' " );

			String trimmedKey = value.trim();

			// then we want to track the record label
			if ( this.recordLabels.containsKey( trimmedKey ) ) {

				//System.out.println( " size: " + this.recordLabels.size() );

				Integer labelID = this.recordLabels.get( trimmedKey ).getFirst();
				Integer countInt = this.recordLabels.get( trimmedKey ).getSecond();
				countInt++;

				this.recordLabels.put( trimmedKey, new Pair<>( labelID, countInt ) );

			} else {

				Integer labelID = this.recordLabels.size();
				this.recordLabels.put( trimmedKey, new Pair<>( labelID, 1 ) );

			//	System.out.println( ">>> Adding Label: '" + trimmedKey + "' @ " + labelID );

			}

		} else {
			throw new Exception("Unknown ColumnType / Transform type combination "+this.columnType+"/"+ this.transform);
		}

	}

	public void computeStatistics() {

		if ( ColumnType.NUMERIC == this.columnType  || ColumnType.NUMERICDEFAULT == this.columnType) {

		//} else if ( Column == this.columnType ) {

		} else {


		}

	}

	public void debugPrintColumns() {

		for (Map.Entry<String, Pair<Integer,Integer>> entry : this.recordLabels.entrySet()) {

			String key = entry.getKey();
		    //Integer value = entry.getValue();
			Pair<Integer,Integer> value = entry.getValue();

		    System.out.println( "> " + key + ", " + value);

		    // now work with key and value...
		}

	}

	public Integer getLabelCount( String label ) {

		if ( this.recordLabels.containsKey(label) ) {

			return this.recordLabels.get( label ).getSecond();

		}

		return 0;

	}

	public Integer getLabelID( String label ) {

	//	System.out.println( "getLableID() => '" + label + "' " );

		if ( this.recordLabels.containsKey(label) ) {

			return this.recordLabels.get( label ).getFirst();

		}

		log.debug("Column:{}/{} .getLabelID() >> returning null for label {} with size:{} ",this.name, this.columnType, label, this.recordLabels.size());
		return null;

	}


	public double transformColumnValue(String inputColumnValue) {

		switch (this.transform) {
			case LABEL:
				return this.label(inputColumnValue);
			case BINARIZE:
				return this.binarize(inputColumnValue);
			case COPY:
				return this.copy(inputColumnValue);
			case NORMALIZE:
				return this.normalize(inputColumnValue);
			case SKIP:
				return 0.0; // but the vector engine has to remove this from output
		}
		log.error("TransformColumnValue called with invalid transform of:"+ this.transform + " and  inputColumnValue of:"+inputColumnValue);
		return -1.0; // not good

	}


	public double copy(String inputColumnValue) {

		double return_value = 0;

		if (this.columnType == ColumnType.NUMERIC) {

			return_value = Double.parseDouble( inputColumnValue );

		} else {

			// In the prep-pass all of the different strings are indexed
			// copies the label index over as-is as a floating point value (1.0, 2.0, 3.0, ... N)

			String key = inputColumnValue.trim();

			return_value = this.getLabelID( key );


		}

		return return_value;
	}

	/*
	 * Needed Statistics for binarize() - range of values (min, max) - similar
	 * to normalize, but we threshold on 0.5 after normalize
	 */
	public double binarize(String inputColumnValue) {

		if ( inputColumnValue.equalsIgnoreCase("Y")) {
			return 1.0;
		}

		if ( inputColumnValue.equalsIgnoreCase("N") || inputColumnValue.trim().isEmpty()) {
			return 0.0;
		}

		double val = Double.parseDouble(inputColumnValue);

		double range = this.maxValue - this.minValue;
		double midpoint = ( range / 2 ) + this.minValue;

		if (val < midpoint) {
			return 0.0;
		}

		return 1.0;

	}

	/*
	 * Needed Statistics for normalize() - range of values (min, max)
	 *
	 *
	 * normalize( x ) = ( x - min ) / range
	 *
	 */
	public double normalize(String inputColumnValue) {

		double return_value = 0;

		if (this.columnType == ColumnType.NUMERIC || this.columnType == ColumnType.NUMERICDEFAULT) {
			double range = this.maxValue - this.minValue;
			if (0.0 == range) {
				return_value = 0.0;
			} else {
				if (inputColumnValue.trim().isEmpty()) {
					double avgValue = (this.maxValue - this.minValue) / 2 + this.minValue;
					return_value = (avgValue - this.minValue) / range;
				} else {
					double val = Double.parseDouble(inputColumnValue);
					double normalizedOut = (val - this.minValue) / range;
					return_value = normalizedOut;
				}
			}

		} else {

			// we have a normalized list of labels

			String key = inputColumnValue.trim();

			double totalLabels = this.recordLabels.size();
			Integer ID = this.getLabelID(key);
			double labelIndex = this.getLabelID( key ) + 1.0;

			//System.out.println("Index Label: " + labelIndex);

			return_value = labelIndex / totalLabels;


		}

		return return_value;

	}

	/*
	 * Needed Statistics for label() - count of distinct labels - index of
	 * labels to IDs (hashtable?)
	 */
	public double label(String inputColumnValue) {

		double return_value = 0;

		if (this.columnType == ColumnType.NUMERIC || this.columnType == ColumnType.NUMERICDEFAULT) {
			// TODO: Discuss if this is what you want. shouldn't numerics in this case just be treated like strings?
			// In this case, same thing as !COPY --- uses input column numbers as the floating point label value
			return_value = Double.parseDouble(inputColumnValue);

		} else {

			// its a nominal value in the indexed list -> pull the index, return it as a double

			// TODO: how do get a numeric index from a list of labels?
			Integer ID = this.getLabelID( inputColumnValue.trim() );

			if (ID == null) {
				log.error("label:{} returned NULL -- inputColumnValue:{}", this.name, inputColumnValue);
			}

			return_value = ID;

		}


		return return_value;

	}



}

