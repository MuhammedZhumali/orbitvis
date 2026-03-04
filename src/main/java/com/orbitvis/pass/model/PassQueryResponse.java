package com.orbitvis.pass.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassQueryResponse {

    private List<PassPrediction> passes;
}

