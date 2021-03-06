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
package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.RecordLibrary;
import org.onebusaway.nyc.vehicle_tracking.model.VehicleLocationManagementRecord;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationDetails;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationInferenceService;
import org.onebusaway.realtime.api.VehicleLocationListener;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class VehicleLocationInferenceServiceImpl implements
    VehicleLocationInferenceService {

  private static Logger _log = LoggerFactory.getLogger(VehicleLocationInferenceServiceImpl.class);

  private ExecutorService _executorService;

  private VehicleLocationListener _vehicleLocationListener;

  private int _numberOfProcessingThreads = 10;

  private ConcurrentMap<AgencyAndId, VehicleInferenceInstance> _vehicleInstancesByVehicleId = new ConcurrentHashMap<AgencyAndId, VehicleInferenceInstance>();

  private ApplicationContext _applicationContext;

  @Autowired
  public void setVehicleLocationListener(
      VehicleLocationListener vehicleLocationListener) {
    _vehicleLocationListener = vehicleLocationListener;
  }

  /**
   * Usually, we shoudn't ever have a reference to ApplicationContext, but we
   * need it for the prototype
   * 
   * @param applicationContext
   */
  @Autowired
  public void setApplicationContext(ApplicationContext applicationContext) {
    _applicationContext = applicationContext;
  }

  public void setNumberOfProcessingThreads(int numberOfProcessingThreads) {
    _numberOfProcessingThreads = numberOfProcessingThreads;
  }

  @PostConstruct
  public void start() {
    if (_numberOfProcessingThreads <= 0)
      throw new IllegalArgumentException(
          "numberOfProcessingThreads must be positive");
    _executorService = Executors.newFixedThreadPool(_numberOfProcessingThreads);
  }

  @PreDestroy
  public void stop() {
    _executorService.shutdownNow();
  }

  @Override
  public void handleNycVehicleLocationRecord(NycVehicleLocationRecord record) {
    _executorService.execute(new ProcessingTask(record));
  }

  @Override
  public void handleVehicleLocationRecord(VehicleLocationRecord record) {
    _executorService.execute(new ProcessingTask(record));
  }

  @Override
  public void handleNycTestLocationRecord(AgencyAndId vehicleId,
      NycTestLocationRecord record) {
    _executorService.execute(new ProcessingTask(vehicleId, record));
  }

  @Override
  public void resetVehicleLocation(AgencyAndId vid) {
    _vehicleInstancesByVehicleId.remove(vid);
  }

  @Override
  public NycTestLocationRecord getVehicleLocationForVehicle(AgencyAndId vid) {
    VehicleInferenceInstance instance = _vehicleInstancesByVehicleId.get(vid);
    if (instance == null)
      return null;
    NycTestLocationRecord record = instance.getCurrentState();
    if( record != null)
      record.setVehicleId(vid.getId());
    return record;
  }

  @Override
  public List<NycTestLocationRecord> getLatestProcessedVehicleLocationRecords() {

    List<NycTestLocationRecord> records = new ArrayList<NycTestLocationRecord>();

    for (Map.Entry<AgencyAndId, VehicleInferenceInstance> entry : _vehicleInstancesByVehicleId.entrySet()) {
      AgencyAndId vehicleId = entry.getKey();
      VehicleInferenceInstance instance = entry.getValue();
      if (instance != null) {
        NycTestLocationRecord record = instance.getCurrentState();
        if (record != null) {
          record.setVehicleId(vehicleId.getId());
          records.add(record);
        }
      } else {
        _log.warn("No VehicleInferenceInstance found: vid=" + vehicleId);
      }
    }

    return records;
  }

  @Override
  public VehicleLocationManagementRecord getVehicleLocationManagementRecordForVehicle(
      AgencyAndId vid) {
    VehicleInferenceInstance instance = _vehicleInstancesByVehicleId.get(vid);
    if (instance == null)
      return null;
    return instance.getCurrentManagementState();
  }

  @Override
  public List<VehicleLocationManagementRecord> getVehicleLocationManagementRecords() {

    List<VehicleLocationManagementRecord> records = new ArrayList<VehicleLocationManagementRecord>();

    for (Map.Entry<AgencyAndId, VehicleInferenceInstance> entry : _vehicleInstancesByVehicleId.entrySet()) {
      AgencyAndId vehicleId = entry.getKey();
      VehicleInferenceInstance instance = entry.getValue();
      VehicleLocationManagementRecord record = instance.getCurrentManagementState();
      record.setVehicleId(vehicleId);
      records.add(record);
    }

    return records;
  }

  @Override
  public void setVehicleStatus(AgencyAndId vid, boolean enabled) {
    VehicleInferenceInstance instance = _vehicleInstancesByVehicleId.get(vid);
    if (instance == null)
      return;
    instance.setVehicleStatus(enabled);
  }

  @Override
  public List<Particle> getCurrentParticlesForVehicleId(AgencyAndId vehicleId) {
    VehicleInferenceInstance instance = _vehicleInstancesByVehicleId.get(vehicleId);
    if (instance == null)
      return null;
    return instance.getCurrentParticles();
  }

  @Override
  public List<Particle> getCurrentSampledParticlesForVehicleId(
      AgencyAndId vehicleId) {
    VehicleInferenceInstance instance = _vehicleInstancesByVehicleId.get(vehicleId);
    if (instance == null)
      return null;
    return instance.getCurrentSampledParticles();
  }

  @Override
  public List<JourneyPhaseSummary> getCurrentJourneySummariesForVehicleId(
      AgencyAndId vehicleId) {
    VehicleInferenceInstance instance = _vehicleInstancesByVehicleId.get(vehicleId);
    if (instance == null)
      return Collections.emptyList();
    return instance.getJourneySummaries();
  }

  @Override
  public VehicleLocationDetails getDetailsForVehicleId(AgencyAndId vehicleId) {
    VehicleInferenceInstance instance = _vehicleInstancesByVehicleId.get(vehicleId);
    if (instance == null)
      return null;
    VehicleLocationDetails details = instance.getDetails();
    details.setVehicleId(vehicleId.getId());
    return details;
  }
  

  @Override
  public VehicleLocationDetails getBadDetailsForVehicleId(AgencyAndId vehicleId) {
    VehicleInferenceInstance instance = _vehicleInstancesByVehicleId.get(vehicleId);
    if (instance == null)
      return null;
    VehicleLocationDetails details = instance.getBadParticleDetails();
    details.setVehicleId(vehicleId.getId());
    details.setParticleFilterFailureActive(true);
    return details;
  }


  /****
   * Private Methods
   ****/

  private VehicleInferenceInstance getInstanceForVehicle(AgencyAndId vehicleId) {

    VehicleInferenceInstance instance = _vehicleInstancesByVehicleId.get(vehicleId);

    if (instance == null) {
      VehicleInferenceInstance newInstance = _applicationContext.getBean(VehicleInferenceInstance.class);
      instance = _vehicleInstancesByVehicleId.putIfAbsent(vehicleId,
          newInstance);
      if (instance == null)
        instance = newInstance;
    }

    return instance;
  }

  private class ProcessingTask implements Runnable {

    private AgencyAndId _vehicleId;

    private NycVehicleLocationRecord _inferenceRecord;

    private NycTestLocationRecord _nycTestLocationRecord;

    private VehicleLocationRecord _vehicleLocationRecord;

    public ProcessingTask(NycVehicleLocationRecord record) {
      _vehicleId = record.getVehicleId();
      _inferenceRecord = record;
    }

    public ProcessingTask(AgencyAndId vehicleId, NycTestLocationRecord record) {
      _vehicleId = vehicleId;
      _nycTestLocationRecord = record;
    }

    public ProcessingTask(VehicleLocationRecord record) {
      _vehicleId = record.getVehicleId();
      _vehicleLocationRecord = record;
    }

    @Override
    public void run() {

      try {
        VehicleInferenceInstance existing = getInstanceForVehicle(_vehicleId);

        boolean passOnRecord = sendRecord(existing);

        if (passOnRecord) {
          NycTestLocationRecord record = existing.getCurrentState();
          VehicleLocationRecord vlr = RecordLibrary.getNycTestLocationRecordAsVehicleLocationRecord(record);
          vlr.setVehicleId(_vehicleId);
          _vehicleLocationListener.handleVehicleLocationRecord(vlr);
        }

      } catch (Throwable ex) {
        _log.error("error processing new location record for inference", ex);
      }
    }

    private boolean sendRecord(VehicleInferenceInstance existing) {
      if (_inferenceRecord != null) {
        return existing.handleUpdate(_inferenceRecord);
      } else if (_nycTestLocationRecord != null) {
        return existing.handleBypassUpdate(_nycTestLocationRecord);
      } else if (_vehicleLocationRecord != null) {
        return existing.handleBypassUpdate(_vehicleLocationRecord);
      }
      return false;
    }
  }
}
