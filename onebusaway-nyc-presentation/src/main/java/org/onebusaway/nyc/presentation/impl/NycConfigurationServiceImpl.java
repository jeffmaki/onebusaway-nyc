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
package org.onebusaway.nyc.presentation.impl;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;
import org.onebusaway.nyc.transit_data.services.VehicleTrackingManagementService;
import org.onebusaway.nyc.presentation.service.ConfigurationBean;
import org.onebusaway.nyc.presentation.service.NycConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NycConfigurationServiceImpl implements NycConfigurationService {

  private VehicleTrackingManagementService _vehicleTrackingManagementService;

  private File _path;

  private ConfigurationBean _config = new ConfigurationBean();

  private volatile String _agencyId;

  public void setPath(File path) {
    _path = path;
  }

  @Autowired
  public void setVehicleTrackingManagementService(
      VehicleTrackingManagementService vehicleTrackingManagementService) {
    _vehicleTrackingManagementService = vehicleTrackingManagementService;
  }

  @PostConstruct
  public void setup() {
    _config = loadSettings();
  }

  /****
   * {@link NycConfigurationService} Interface
   ****/

  public String getDefaultAgencyId() {
    if (_agencyId == null) {
      synchronized (this) {
        if (_agencyId == null)
          _agencyId = _vehicleTrackingManagementService.getDefaultAgencyId();
      }
    }
    return _agencyId;
  }

  @Override
  public ConfigurationBean getConfiguration() {
    return new ConfigurationBean(_config);
  }

  @Override
  public void setConfiguration(ConfigurationBean configuration) {
    _config = new ConfigurationBean(configuration);
    notifySettings();
    saveSettings(_config);
  }

  /****
   * Private Methods
   ****/

  private void notifySettings() {
    _vehicleTrackingManagementService.setVehicleOffRouteDistanceThreshold(_config.getOffRouteDistance());
    _vehicleTrackingManagementService.setVehicleStalledTimeThreshold(_config.getNoProgressTimeout());
  }

  private ConfigurationBean loadSettings() {

    if (_path == null || !_path.exists())
      return new ConfigurationBean();

    try {

      Properties properties = new Properties();
      properties.load(new FileReader(_path));

      ConfigurationBean bean = new ConfigurationBean();
      BeanInfo beanInfo = Introspector.getBeanInfo(ConfigurationBean.class);

      for (PropertyDescriptor desc : beanInfo.getPropertyDescriptors()) {
        String name = desc.getName();
        Object value = properties.getProperty(name);
        if (value != null) {
          Converter converter = ConvertUtils.lookup(desc.getPropertyType());
          value = converter.convert(desc.getPropertyType(), value);
          Method m = desc.getWriteMethod();
          m.invoke(bean, value);
        }
      }

      return bean;
    } catch (Exception ex) {
      throw new IllegalStateException(
          "error loading configuration from properties file " + _path, ex);
    }
  }

  private void saveSettings(ConfigurationBean bean) {

    if (_path == null)
      return;

    try {

      Properties properties = new Properties();
      BeanInfo beanInfo = Introspector.getBeanInfo(ConfigurationBean.class);

      for (PropertyDescriptor desc : beanInfo.getPropertyDescriptors()) {

        String name = desc.getName();

        if (name.equals("class"))
          continue;

        Method m = desc.getReadMethod();
        Object value = m.invoke(bean);
        if (value != null) {
          properties.setProperty(name, value.toString());
        }
      }

      properties.store(new FileWriter(_path), "onebusaway-nyc configuration");
    } catch (Exception ex) {
      throw new IllegalStateException(
          "error saving configuration to properties file " + _path, ex);
    }
  }
}
