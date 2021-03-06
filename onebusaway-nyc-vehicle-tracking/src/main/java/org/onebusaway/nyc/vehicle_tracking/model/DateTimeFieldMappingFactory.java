/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
/**
 * 
 */
package org.onebusaway.nyc.vehicle_tracking.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.onebusaway.gtfs.csv.CsvEntityContext;
import org.onebusaway.gtfs.csv.exceptions.CsvEntityException;
import org.onebusaway.gtfs.csv.schema.AbstractFieldMapping;
import org.onebusaway.gtfs.csv.schema.BeanWrapper;
import org.onebusaway.gtfs.csv.schema.EntitySchemaFactory;
import org.onebusaway.gtfs.csv.schema.FieldMapping;
import org.onebusaway.gtfs.csv.schema.FieldMappingFactory;

public class DateTimeFieldMappingFactory implements FieldMappingFactory {

  private static SimpleDateFormat _format = new SimpleDateFormat(
      "yyyy-MM-dd' 'HH:mm:ss");
  
  static {
    _format.setTimeZone(TimeZone.getTimeZone("America/New_York"));
  }

  @Override
  public FieldMapping createFieldMapping(EntitySchemaFactory schemaFactory,
      Class<?> entityType, String csvFieldName, String objFieldName,
      Class<?> objFieldType, boolean required) {
    return new DateTimeFieldMapping(entityType, csvFieldName, objFieldName,
        required);
  }

  private class DateTimeFieldMapping extends AbstractFieldMapping {

    public DateTimeFieldMapping(Class<?> entityType, String csvFieldName,
        String objFieldName, boolean required) {
      super(entityType, csvFieldName, objFieldName, required);
    }

    @Override
    public void translateFromCSVToObject(CsvEntityContext context,
        Map<String, Object> csvValues, BeanWrapper object)
        throws CsvEntityException {

      if (isMissingAndOptional(csvValues))
        return;

      String valueAsString = (String) csvValues.get(_csvFieldName);

      try {

        Date valueAsDate = _format.parse(valueAsString);
        Class<?> type = object.getPropertyType(_objFieldName);

        if (type.equals(Long.class) || type.equals(Long.TYPE))
          object.setPropertyValue(_objFieldName, valueAsDate.getTime());
        else
          object.setPropertyValue(_objFieldName, valueAsDate);

      } catch (ParseException e) {
        throw new CsvDateFormatException(_entityType, "bad date-time string:"
            + valueAsString, e);
      }
    }

    @Override
    public void translateFromObjectToCSV(CsvEntityContext context,
        BeanWrapper object, Map<String, Object> csvValues)
        throws CsvEntityException {

      Object obj = object.getPropertyValue(_objFieldName);
      Date valueAsDate = obj instanceof Long ? new Date(
          ((Long) obj).longValue()) : (Date) obj;
      String valueAsString = _format.format(valueAsDate);
      csvValues.put(_csvFieldName, valueAsString);
    }
  }

  public static class CsvDateFormatException extends CsvEntityException {

    public CsvDateFormatException(Class<?> entityType, String message,
        Throwable cause) {
      super(entityType, message, cause);
    }

    private static final long serialVersionUID = 1L;

  }
}