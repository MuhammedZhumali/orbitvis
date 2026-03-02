package com.orbitvis.orbit.model;

import lombok.Data;

@Data
public class OrbitPoint{
    private double time;
    private double lat;
    private double lon;
    private double alt;
}