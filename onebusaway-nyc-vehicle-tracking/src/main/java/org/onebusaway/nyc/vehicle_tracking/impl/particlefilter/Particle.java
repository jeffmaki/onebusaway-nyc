package org.onebusaway.nyc.vehicle_tracking.impl.particlefilter;

import java.io.Serializable;

/**
 * Particle object has information about time, weight, the parent particle, and
 * any associated data.
 * 
 * @author bdferris
 * @see ParticleFilter
 */
public class Particle implements Serializable {

  private static final long serialVersionUID = 1L;

  private double _timestamp;

  private double _weight = 1.0;

  private Particle _parent;

  private Object _data;

  public Particle(double timestamp) {
    this(timestamp, null);
  }

  public Particle(double timestamp, Particle parent) {
    this(timestamp, parent, 1.0);
  }

  public Particle(double timestamp, Particle parent, double weight) {
    _timestamp = timestamp;
    _parent = parent;
    _weight = weight;
  }

  public Particle(Particle particle) {
    _timestamp = particle.getTimestamp();
    _parent = particle.getParent();
    _weight = particle.getWeight();
    _data = particle.getData();
  }

  public double getTimestamp() {
    return _timestamp;
  }

  public void setTimestamp(double timestamp) {
    _timestamp = timestamp;
  }

  public double getWeight() {
    return _weight;
  }

  public void setWeight(double weight) {
    _weight = weight;
  }

  public Particle getParent() {
    return _parent;
  }

  public void setParent(Particle parent) {
    _parent = parent;
  }

  @SuppressWarnings("unchecked")
  public <T> T getData() {
    return (T) _data;
  }

  public void setData(Object data) {
    _data = data;
  }

  public Particle cloneParticle() {
    return new Particle(this);
  }

  @Override
  public String toString() {
    return "Particle(time=" + _timestamp + " weight=" + _weight + " data="
        + _data + ")";
  }
}