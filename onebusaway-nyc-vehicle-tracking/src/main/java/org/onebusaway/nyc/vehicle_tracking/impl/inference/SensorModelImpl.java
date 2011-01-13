package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.Collections;
import java.util.List;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Context;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelRule;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelSupportLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SensorModelImpl implements SensorModel<Observation> {

  private List<SensorModelRule> _rules = Collections.emptyList();

  private SensorModelSupportLibrary _sensorModelLibrary;

  @Autowired
  public void setSensorModelLibrary(SensorModelSupportLibrary sensorModelLibrary) {
    _sensorModelLibrary = sensorModelLibrary;
  }

  /****
   * {@link SensorModel} Interface
   ****/

  @Override
  public SensorModelResult likelihood(Particle particle, Observation observation) {

    VehicleState state = particle.getData();
    VehicleState parentState = null;
    Particle parent = particle.getParent();

    if (parent != null)
      parentState = parent.getData();

    SensorModelResult result = likelihood(parentState, state, observation);

    return result;
  }

  @Autowired
  public void setRules(List<SensorModelRule> rules) {
    _rules = rules;
  }

  /****
   * {@link SensorModelImpl} Interface
   ****/

  public SensorModelResult likelihood(VehicleState parentState,
      VehicleState state, Observation obs) {

    SensorModelResult result = new SensorModelResult("pTotal", 1.0);

    Context context = new Context(parentState, state, obs);

    for (SensorModelRule rule : _rules) {
      SensorModelResult r = rule.likelihood(_sensorModelLibrary, context);
      result.addResultAsAnd(r);
    }

    return result;
  }
}
