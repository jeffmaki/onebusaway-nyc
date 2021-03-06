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
import java.util.List;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.MotionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Motion model implementation for vehicle location inference.
 * 
 * @author bdferris
 */
public class MotionModelImpl implements MotionModel<Observation> {

  private JourneyStateTransitionModel _journeyMotionModel;

  /**
   * Distance, in meters, that a bus has to travel to be considered "in motion"
   */
  private double _motionThreshold = 20;

  @Autowired
  public void setJourneyMotionModel(
      JourneyStateTransitionModel journeyMotionModel) {
    _journeyMotionModel = journeyMotionModel;
  }

  public void setMotionThreshold(double motionThreshold) {
    _motionThreshold = motionThreshold;
  }

  @Override
  public void move(Particle parent, double timestamp, double timeElapsed,
      Observation obs, List<Particle> results) {

    VehicleState parentState = parent.getData();

    /**
     * Snap the observation to an edge on the street network
     */
    EdgeState edgeState = determineEdgeState(obs, parentState);

    MotionState motionState = updateMotionState(parentState, obs);

    List<VehicleState> vehicleStates = new ArrayList<VehicleState>();
    _journeyMotionModel.move(parentState, edgeState, motionState, obs,
        vehicleStates);

    for (VehicleState vs : vehicleStates)
      results.add(new Particle(timestamp, parent, 1.0, vs));
  }

  private EdgeState determineEdgeState(Observation obs, VehicleState parentState) {

    EdgeState edgeState = parentState.getEdgeState();

    /**
     * We only let the street network edge change if the GPS has changed. This
     * helps us avoid oscilation between potential states when the bus isn't
     * moving.
     */
    if (gpsHasChanged(obs)) {

      /**
       * We can cache the edge CDF for an observation
       */
      /*
       * CDFMap<EdgeState> edges = _observationCache.getValueForObservation(obs,
       * EObservationCacheKey.STREET_NETWORK_EDGES);
       * 
       * if (edges == null) { edges =
       * _edgeStateLibrary.calculatePotentialEdgeStates(obs.getPoint());
       * _observationCache.putValueForObservation(obs,
       * EObservationCacheKey.STREET_NETWORK_EDGES, edges); }
       * 
       * edgeState = edges.sample();
       */
    }

    return edgeState;
  }

  public MotionState updateMotionState(Observation obs) {
    return updateMotionState(null, obs);
  }

  public MotionState updateMotionState(VehicleState parentState, Observation obs) {

    long lastInMotionTime = obs.getTime();
    CoordinatePoint lastInMotionLocation = obs.getLocation();

    if (parentState != null) {

      MotionState motionState = parentState.getMotionState();

      double d = SphericalGeometryLibrary.distance(
          motionState.getLastInMotionLocation(), obs.getLocation());

      if (d <= _motionThreshold) {
        lastInMotionTime = motionState.getLastInMotionTime();
        lastInMotionLocation = motionState.getLastInMotionLocation();
      }
    }

    return new MotionState(lastInMotionTime, lastInMotionLocation);
  }

  private boolean gpsHasChanged(Observation obs) {

    NycVehicleLocationRecord record = obs.getRecord();
    NycVehicleLocationRecord prevRecord = obs.getPreviousRecord();

    if (prevRecord == null)
      return true;

    return prevRecord.getLatitude() != record.getLatitude()
        || prevRecord.getLongitude() != record.getLongitude();
  }
}
