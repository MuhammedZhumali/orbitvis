package com.orbitvis.orbit.model;

import lombok.Data;

@Data
public class OrbitPoint{
    private double time;
    private double lan;
    private double lon;
    private double alt;
}